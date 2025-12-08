package de.janmeckelholt.stravasummary.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.janmeckelholt.stravasummary.AuthActivity
import de.janmeckelholt.stravasummary.data.StravaRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import de.janmeckelholt.stravasummary.Steps
import de.janmeckelholt.stravasummary.data.database.models.ActivityDto
import de.janmeckelholt.stravasummary.data.database.models.AthleteDto
import de.janmeckelholt.stravasummary.data.models.WeekSummary
import de.janmeckelholt.stravasummary.data.models.toWeekSummary
import de.janmeckelholt.stravasummary.data.network.Credentials
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
class MainViewModel @Inject constructor(private val repo: StravaRepo) : ViewModel() {
    private var _displayedDate = MutableStateFlow<Long>(unixTimeStampToPreviousMondayMorning(System.currentTimeMillis() / 1000))
    val displayedDate: StateFlow<Long> = _displayedDate.asStateFlow()

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
            val token = Credentials.updateAccessToken()
            if (token.isNullOrEmpty()) {
                Timber.e("No Strava token provided")
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

    init {
        viewModelScope.launch {
            Credentials.initialize(stravaRepo = repo)
            _step.value = if (!Credentials.code.isNullOrEmpty()) {
                Timber.i("Using saved Strava code from DataStore: ${Credentials.code}")
                if (!Credentials.accessToken.isNullOrEmpty()) {
                    Timber.i("Using saved Strava accessToken from DataStore: ${Credentials.accessToken}")

                    val storedAthlete = repo.getAthlete(Credentials.athleteId ?: -1)
                    if (storedAthlete != null) {
                        Timber.i("Using saved Strava athlete from DataStore: ${storedAthlete.firstName} ${storedAthlete.lastName}")
                        _athleteDto.value = storedAthlete
                        _weekActivities.value = repo.getWeekActivities(displayedDate.value) ?: emptyList()
                        Steps.SHOW_WEEK_SUMMARY
                    } else Steps.GET_ATHLETE
                } else Steps.GET_TOKEN
            } else {
                Timber.i("No Strava code saved. Getting new one...")
                Steps.GET_CODE
            }
        }
    }

    fun getStravaCode(ctx: Context) {
        _step.value = Steps.GET_CODE
        viewModelScope.launch {
            val storedCode = Credentials.code
            if (!storedCode.isNullOrEmpty()) {
                Timber.i("Using saved Strava code from DataStore: $storedCode")
                return@launch
            }
            try {
                val intent = Intent(ctx, AuthActivity::class.java)
                ctx.startActivity(intent)

            } catch (e: Exception) {
                Timber.e(e.localizedMessage)
            }
        }
    }

    fun getAccessToken() {
        _step.value = Steps.GET_TOKEN
        viewModelScope.launch {
            val t = Credentials.updateAccessToken()
            Timber.i("Access token: $t")
            _step.value = Steps.GET_ATHLETE
        }
    }

    fun fetchAthlete() {
        _step.value = Steps.GET_ATHLETE
        val token = Credentials.updateAccessToken()
        if (token.isNullOrEmpty()) {
            Timber.e("No Strava token provided")
            return
        }
        viewModelScope.launch {
            val athleteDto = repo.fetchAthlete()
            _athleteDto.value = athleteDto
            Credentials.updateAthleteId(athleteDto.id)
            repo.insertAllAthletes(athleteDto)
            Credentials
            _step.value = Steps.GET_ACTIVITIES
        }
    }
}