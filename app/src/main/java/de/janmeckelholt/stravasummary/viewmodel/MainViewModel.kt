package de.janmeckelholt.stravasummary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.janmeckelholt.stravasummary.Steps
import de.janmeckelholt.stravasummary.data.StravaRepo
import de.janmeckelholt.stravasummary.data.database.models.ActivityDto
import de.janmeckelholt.stravasummary.data.database.models.AthleteDto
import de.janmeckelholt.stravasummary.data.models.WeekSummary
import de.janmeckelholt.stravasummary.data.models.toWeekSummary
import de.janmeckelholt.stravasummary.data.network.AuthManager
import de.janmeckelholt.stravasummary.data.work.BackfillWorker
import de.janmeckelholt.stravasummary.utils.unixTimeStampToDateString
import de.janmeckelholt.stravasummary.utils.unixTimeStampToPreviousMondayMorning
import jakarta.inject.Inject
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: StravaRepo,
    private val authManager: AuthManager,
    private val workManager: WorkManager
) : ViewModel() {
    private var _displayedDate = MutableStateFlow<Long>(unixTimeStampToPreviousMondayMorning(System.currentTimeMillis() / 1000))
    val displayedDate: StateFlow<Long> = _displayedDate.asStateFlow()

    private val _navigateToAuth = MutableSharedFlow<Unit>()
    val navigateToAuth = _navigateToAuth.asSharedFlow()

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

    private var _step = MutableStateFlow<Steps>(Steps.GET_CODE)
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

    private var _athleteDto = MutableStateFlow<AthleteDto?>(null)
    val athleteDto: StateFlow<AthleteDto?> = _athleteDto.asStateFlow()

    fun fetchActivities() {
        viewModelScope.launch {
            val token = authManager.getAccessToken()
            if (token.isNullOrEmpty()) {
                Timber.e("No Strava token provided")
                _step.value = Steps.GET_CODE
                return@launch
            }
            val activities = repo.fetchWeekActivities(unixTimeStamp = _displayedDate.value)
            activities?.let { acts ->
                repo.insertAllActivities(*acts.toTypedArray())
            }
            _step.value = Steps.SHOW_WEEK_SUMMARY
            observeActivities()
        }
    }

    fun refreshActivities() {
        viewModelScope.launch {
            // Clear the cache to force a fresh fetch from remote
            repo.clearActivitiesCache()
            Timber.i("Cache cleared, fetching fresh activities from remote")
            fetchActivities()
        }
    }

    fun saveCodeAndResume(code: String) {
        viewModelScope.launch {
            authManager.updateCode(code)
            resumeAuthFlow()
        }
    }

    init {
        resumeAuthFlow()
    }

    fun resumeAuthFlow() {
        _step.value = Steps.INITIAL
        viewModelScope.launch {
            authManager.initialize()
            val token = authManager.getAccessToken()
            
            if (!token.isNullOrEmpty()) {
                Timber.i("Have valid access token. Proceeding...")
                
                // Enqueue backfill worker in the background
                val backfillWorkRequest = OneTimeWorkRequestBuilder<BackfillWorker>().build()
                workManager.enqueueUniqueWork(
                    "strava_backfill",
                    ExistingWorkPolicy.KEEP,
                    backfillWorkRequest
                )

                val storedAthlete = repo.getAthlete(authManager.athleteId ?: -1)
                if (storedAthlete != null) {
                    Timber.i("Using saved Strava athlete from DataStore: ${storedAthlete.firstName} ${storedAthlete.lastName}")
                    _athleteDto.value = storedAthlete
                    fetchActivities()
                } else {
                    Timber.i("Fetching athlete...")
                    fetchAthlete()
                }
            } else {
                Timber.i("No Strava token available. Need to get code...")
                _step.value = Steps.GET_CODE
            }
        }
    }

    fun getStravaCode() {
        _step.value = Steps.GET_CODE
        viewModelScope.launch {
            val storedCode = authManager.code
            if (!storedCode.isNullOrEmpty()) {
                Timber.i("Using saved Strava code from DataStore: $storedCode")
                // Code is already there, let's try getting token
                resumeAuthFlow()
                return@launch
            }
            _navigateToAuth.emit(Unit)
        }
    }

    fun fetchAthlete() {
        _step.value = Steps.GET_ATHLETE
        viewModelScope.launch {
            val token = authManager.getAccessToken()
            if (token.isNullOrEmpty()) {
                Timber.e("No Strava token provided")
                _step.value = Steps.GET_CODE
                return@launch
            }
            
            try {
                val athleteDto = repo.fetchAthlete()
                _athleteDto.value = athleteDto
                authManager.updateAthleteId(athleteDto.id)
                repo.insertAllAthletes(athleteDto)
                fetchActivities()
            } catch (e: Exception) {
                Timber.e(e, "Error fetching athlete")
                _step.value = Steps.GET_CODE
            }
        }
    }
}