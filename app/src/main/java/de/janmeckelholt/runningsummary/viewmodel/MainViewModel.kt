package de.janmeckelholt.runningsummary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.janmeckelholt.runningsummary.Constants
import de.janmeckelholt.runningsummary.PreferencesDataStore
import de.janmeckelholt.runningsummary.Steps
import de.janmeckelholt.runningsummary.data.HealthConnectManager
import de.janmeckelholt.runningsummary.data.HealthConnectRepo
import de.janmeckelholt.runningsummary.data.database.models.ActivityDto
import de.janmeckelholt.runningsummary.data.models.WeekSummary
import de.janmeckelholt.runningsummary.data.models.toWeekSummary
import de.janmeckelholt.runningsummary.utils.unixTimeStampToPreviousMondayMorning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: HealthConnectRepo,
    private val healthConnectManager: HealthConnectManager,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {
    private val _displayedDate =
        MutableStateFlow(unixTimeStampToPreviousMondayMorning(System.currentTimeMillis() / 1000))
    val displayedDate: StateFlow<Long> = _displayedDate.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    private val _step = MutableStateFlow(Steps.INITIAL)
    val step: StateFlow<Steps> = _step.asStateFlow()

    fun setStep(newStep: Steps) {
        _step.value = newStep
    }

    private val _selectedRunId = MutableStateFlow<String?>(null)
    val selectedRunId: StateFlow<String?> = _selectedRunId.asStateFlow()

    fun selectRun(runId: String?) {
        _selectedRunId.value = runId
    }

    fun openRunDetails(runId: String? = null) {
        _selectedRunId.value = runId ?: weekSummary.value.runs.lastOrNull()?.id
        _step.value = Steps.SHOWING_RUN_DETAILS
    }

    fun closeRunDetails() {
        _step.value = Steps.SHOWING_SUMMARY
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val weekSummary: StateFlow<WeekSummary> = _displayedDate
        .flatMapLatest { epoch ->
            repo.getWeekActivitiesFlow(epoch).map { activities ->
                activities.toWeekSummary(epoch)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList<ActivityDto>().toWeekSummary(_displayedDate.value)
        )

    fun setDisplayedDate(epoch: Long) {
        val newMonday = unixTimeStampToPreviousMondayMorning(epoch)
        _displayedDate.value = newMonday
        _selectedRunId.value = null
        fetchActivitiesForDisplayedWeek()
    }

    fun fetchActivitiesForDisplayedWeek() {
        viewModelScope.launch {
            try {
                if (healthConnectManager.hasBasicPermissions()) {
                    _errorMessage.value = null
                    if (_step.value != Steps.SHOWING_RUN_DETAILS) {
                        _step.value = Steps.SHOWING_SUMMARY
                    }
                    repo.fetchWeekActivities(_displayedDate.value)
                } else {
                    _errorMessage.value = "Health Connect permissions missing"
                    _step.value = Steps.REQUESTING_PERMISSIONS
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching activities for week")
                _errorMessage.value =
                    "Failed to read Health Connect: ${e.localizedMessage ?: e.message}"
            }
        }
    }

    fun fetchActivities() {
        viewModelScope.launch {
            try {
                if (healthConnectManager.hasBasicPermissions()) {
                    _errorMessage.value = null
                    if (_step.value != Steps.SHOWING_SUMMARY && _step.value != Steps.SHOWING_RUN_DETAILS) {
                        _step.value = Steps.LOADING
                    }
                    repo.fetchWeekActivities(_displayedDate.value)
                    if (_step.value != Steps.SHOWING_RUN_DETAILS) {
                        _step.value = Steps.SHOWING_SUMMARY
                    }
                    syncHistoricalData()
                } else {
                    _errorMessage.value = "Health Connect permissions missing"
                    _step.value = Steps.REQUESTING_PERMISSIONS
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching activities")
                _errorMessage.value =
                    "Failed to read Health Connect: ${e.localizedMessage ?: e.message}"
                if (_step.value == Steps.LOADING) {
                    _step.value = Steps.SHOWING_SUMMARY
                }
            }
        }
    }

    private suspend fun syncHistoricalData() {
        withContext(Dispatchers.IO) {
            try {
                val lastSyncEpoch = preferencesDataStore
                    .getLong(Constants.LAST_SYNC_EPOCH_KEY, 0L)
                    .first()

                val now = Instant.now()
                val hasHistory = healthConnectManager.hasHistoryPermission()
                val maxLookbackSeconds = if (hasHistory) {
                    Constants.DEFAULT_BACKFILL_SECONDS
                } else {
                    // Capped to 29 days if READ_HEALTH_DATA_HISTORY is not granted
                    29L * 24 * 60 * 60
                }

                val startInstant = if (lastSyncEpoch == 0L) {
                    Timber.i("First sync — backfilling up to ${maxLookbackSeconds / (24 * 3600)} days of history")
                    now.minusSeconds(maxLookbackSeconds)
                } else {
                    val checkpoint = Instant.ofEpochSecond(lastSyncEpoch).minusSeconds(3600)
                    val earliestAllowed = now.minusSeconds(maxLookbackSeconds)
                    if (checkpoint.isBefore(earliestAllowed)) earliestAllowed else checkpoint
                }

                val synced = repo.fetchAndSaveActivitiesForRange(startInstant, now)
                Timber.i("Sync completed. Saved ${synced.size} activities.")

                preferencesDataStore.saveLong(Constants.LAST_SYNC_EPOCH_KEY, now.epochSecond)
            } catch (e: Exception) {
                Timber.e(e, "Error during historical sync")
            }
        }
    }

    fun refreshActivities() {
        fetchActivities()
    }
}
