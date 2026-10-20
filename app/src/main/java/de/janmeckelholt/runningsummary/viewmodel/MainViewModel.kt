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
import de.janmeckelholt.runningsummary.utils.unixTimeStampToDateString
import de.janmeckelholt.runningsummary.utils.unixTimeStampToPreviousMondayMorning
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: HealthConnectRepo,
    private val healthConnectManager: HealthConnectManager,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {
    private val _displayedDate =
        MutableStateFlow<Long>(unixTimeStampToPreviousMondayMorning(System.currentTimeMillis() / 1000))
    val displayedDate: StateFlow<Long> = _displayedDate.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val weekActivities: StateFlow<List<ActivityDto>> = _displayedDate
        .flatMapLatest { epoch ->
            repo.getWeekActivitiesFlow(epoch)
        }
        .map { it ?: emptyList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val weekSummary: StateFlow<WeekSummary> =
        combine(_displayedDate, weekActivities) { epoch, activities ->
            activities.toWeekSummary(epoch)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList<ActivityDto>().toWeekSummary(_displayedDate.value)
        )

    fun setDisplayedDate(epoch: Long) {
        val newMonday = unixTimeStampToPreviousMondayMorning(epoch)
        _displayedDate.value = newMonday
        fetchActivitiesForDisplayedWeek()
    }

    val formattedDisplayedDate: StateFlow<String> = _displayedDate
        .map { epoch ->
            unixTimeStampToDateString(epoch)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = java.time.Instant.ofEpochSecond(_displayedDate.value)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .toString()
        )

    private var _step = MutableStateFlow<Steps>(Steps.INITIAL)
    val step: StateFlow<Steps> = _step.asStateFlow()
    fun setStep(newStep: Steps) {
        _step.value = newStep
    }

    fun showWeekOf(epoch: Long) {
        _step.value = Steps.SHOWING_SUMMARY
        setDisplayedDate(epoch)
    }

    fun fetchActivitiesForDisplayedWeek() {
        viewModelScope.launch {
            try {
                if (healthConnectManager.hasBasicPermissions()) {
                    _step.value = Steps.SHOWING_SUMMARY
                    repo.fetchWeekActivities(_displayedDate.value)
                } else {
                    _errorMessage.value = "Health Connect Permissions missing"
                    _step.value = Steps.REQUESTING_PERMISSIONS
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching activities for week")
                _errorMessage.value = "Failed to read Health Connect"
            }
        }
    }

    fun fetchActivities() {
        viewModelScope.launch {
            try {
                if (healthConnectManager.hasBasicPermissions()) {
                    _step.value = Steps.SHOWING_SUMMARY
                    repo.fetchWeekActivities(_displayedDate.value)
                    syncHistoricalData()
                } else {
                    _errorMessage.value = "Health Connect Permissions missing"
                    _step.value = Steps.REQUESTING_PERMISSIONS
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching activities")
                _errorMessage.value = "Failed to read Health Connect"
            }
        }
    }

    /**
     * Incremental sync: fetches only from the last sync timestamp forward.
     * On first launch (no saved timestamp), looks back [Constants.DEFAULT_BACKFILL_SECONDS] (1 year).
     * After a successful sync, persists the current time as the last sync point.
     */
    private fun syncHistoricalData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lastSyncEpoch = preferencesDataStore
                    .getLong(Constants.LAST_SYNC_EPOCH_KEY, 0L)
                    .first()

                val now = java.time.Instant.now()
                val startInstant = if (lastSyncEpoch == 0L) {
                    // First sync: look back 1 year
                    Timber.i("First sync — backfilling ${Constants.DEFAULT_BACKFILL_SECONDS / (24*3600)} days of history")
                    now.minusSeconds(Constants.DEFAULT_BACKFILL_SECONDS)
                } else {
                    Timber.i("Incremental sync from last checkpoint ${java.time.Instant.ofEpochSecond(lastSyncEpoch)}")
                    java.time.Instant.ofEpochSecond(lastSyncEpoch)
                }

                val synced = repo.fetchAndSaveActivitiesForRange(startInstant, now)
                Timber.i("Sync completed. Saved ${synced.size} activities.")

                // Persist the sync checkpoint
                preferencesDataStore.saveLong(Constants.LAST_SYNC_EPOCH_KEY, now.epochSecond)
            } catch (e: Exception) {
                Timber.e(e, "Error during historical sync")
            }
        }
    }

    fun refreshActivities() {
        viewModelScope.launch {
            fetchActivitiesForDisplayedWeek()
            syncHistoricalData()
        }
    }

    fun checkPermissionsAndFetch() {
        viewModelScope.launch {
            if (healthConnectManager.hasBasicPermissions()) {
                fetchActivities()
            } else {
                _step.value = Steps.REQUESTING_PERMISSIONS
            }
        }
    }
}
