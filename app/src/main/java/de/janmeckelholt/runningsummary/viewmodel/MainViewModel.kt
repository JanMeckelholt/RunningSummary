package de.janmeckelholt.runningsummary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.janmeckelholt.runningsummary.Steps
import de.janmeckelholt.runningsummary.data.HealthConnectManager
import de.janmeckelholt.runningsummary.data.HealthConnectRepo
import de.janmeckelholt.runningsummary.data.database.models.ActivityDto
import de.janmeckelholt.runningsummary.data.database.models.AthleteDto
import de.janmeckelholt.runningsummary.data.models.WeekSummary
import de.janmeckelholt.runningsummary.data.models.toWeekSummary
import de.janmeckelholt.runningsummary.utils.unixTimeStampToDateString
import de.janmeckelholt.runningsummary.utils.unixTimeStampToPreviousMondayMorning
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: HealthConnectRepo,
    private val healthConnectManager: HealthConnectManager
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
        _step.value = Steps.SHOW_WEEK_SUMMARY
        setDisplayedDate(epoch)
    }

    private var _athleteDto = MutableStateFlow<AthleteDto?>(
        AthleteDto(
            id = 1L,
            username = "local",
            firstName = "Local",
            lastName = "User"
        )
    )
    val athleteDto: StateFlow<AthleteDto?> = _athleteDto.asStateFlow()

    fun fetchActivitiesForDisplayedWeek() {
        viewModelScope.launch {
            try {
                if (healthConnectManager.hasBasicPermissions()) {
                    _step.value = Steps.SHOW_WEEK_SUMMARY
                    repo.fetchWeekActivities(_displayedDate.value)
                } else {
                    _errorMessage.value = "Health Connect Permissions missing"
                    _step.value = Steps.GET_CODE // Repurpose GET_CODE step for permissions
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
                    _step.value = Steps.SHOW_WEEK_SUMMARY
                    repo.fetchWeekActivities(_displayedDate.value)
                    backfillHistoricalData()
                } else {
                    _errorMessage.value = "Health Connect Permissions missing"
                    _step.value = Steps.GET_CODE // Repurpose GET_CODE step for permissions
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching activities")
                _errorMessage.value = "Failed to read Health Connect"
            }
        }
    }

    fun backfillHistoricalData() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                Timber.i("Starting historical backfill from year 2000 to present...")
                val startInstant = java.time.Instant.parse("2000-01-01T00:00:00Z")
                val endInstant = java.time.Instant.now()
                val backfilled = repo.fetchAndSaveActivitiesForRange(startInstant, endInstant)
                Timber.i("Historical backfill completed. Saved ${backfilled.size} activities.")
            } catch (e: Exception) {
                Timber.e(e, "Error during historical backfill")
            }
        }
    }

    fun refreshActivities() {
        viewModelScope.launch {
            repo.clearActivitiesCache()
            fetchActivitiesForDisplayedWeek()
        }
    }

    fun checkPermissionsAndFetch() {
        viewModelScope.launch {
            if (healthConnectManager.hasBasicPermissions()) {
                fetchActivities()
            } else {
                _step.value = Steps.GET_CODE // We use GET_CODE for GET_PERMISSIONS on UI side
            }
        }
    }
}
