package de.janmeckelholt.stravasummary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.janmeckelholt.stravasummary.Steps
import de.janmeckelholt.stravasummary.data.HealthConnectManager
import de.janmeckelholt.stravasummary.data.HealthConnectRepo
import de.janmeckelholt.stravasummary.data.database.models.ActivityDto
import de.janmeckelholt.stravasummary.data.database.models.AthleteDto
import de.janmeckelholt.stravasummary.data.models.WeekSummary
import de.janmeckelholt.stravasummary.data.models.toWeekSummary
import de.janmeckelholt.stravasummary.utils.unixTimeStampToDateString
import de.janmeckelholt.stravasummary.utils.unixTimeStampToPreviousMondayMorning
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: HealthConnectRepo,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {
    private var _displayedDate = MutableStateFlow<Long>(unixTimeStampToPreviousMondayMorning(System.currentTimeMillis() / 1000))
    val displayedDate: StateFlow<Long> = _displayedDate.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setDisplayedDate(epoch: Long) {
        _displayedDate.value = unixTimeStampToPreviousMondayMorning(epoch)
        fetchActivities()
        observeActivities()
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
        _displayedDate.value = unixTimeStampToPreviousMondayMorning(epoch)
    }

    private var _weekActivities = MutableStateFlow<List<ActivityDto>>(emptyList())
    val weekActivities: StateFlow<List<ActivityDto>> = _weekActivities.asStateFlow()

    val weekSummary: StateFlow<WeekSummary> = _weekActivities
        .map { it.toWeekSummary(_displayedDate.value) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = _weekActivities.value.toWeekSummary(_displayedDate.value)
        )

    fun observeActivities() {
        viewModelScope.launch {
            repo.getWeekActivitiesFlow(_displayedDate.value).collectLatest { list ->
                _weekActivities.value = list ?: emptyList()
            }
        }
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

    fun fetchActivities() {
        viewModelScope.launch {
            try {
                if (healthConnectManager.hasAllPermissions()) {
                    val activities = repo.fetchWeekActivities(unixTimeStamp = _displayedDate.value)
                    repo.insertAllActivities(*activities.toTypedArray())
                    _step.value = Steps.SHOW_WEEK_SUMMARY
                    observeActivities()
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

    fun refreshActivities() {
        viewModelScope.launch {
            repo.clearActivitiesCache()
            fetchActivities()
        }
    }

    fun checkPermissionsAndFetch() {
        viewModelScope.launch {
            if (healthConnectManager.hasAllPermissions()) {
                fetchActivities()
            } else {
                _step.value = Steps.GET_CODE // We use GET_CODE for GET_PERMISSIONS on UI side
            }
        }
    }
}
