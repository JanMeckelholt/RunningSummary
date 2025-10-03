package de.janmeckelholt.stravasummary.viewmodel

import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.janmeckelholt.stravasummary.AuthActivity
import de.janmeckelholt.stravasummary.MainActivity
import de.janmeckelholt.stravasummary.api.StravaRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import de.janmeckelholt.stravasummary.Constants
import de.janmeckelholt.stravasummary.PreferencesDataStore
import de.janmeckelholt.stravasummary.TimberApplication
import jakarta.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class MainViewModel @Inject constructor(private val repo: StravaRepo) : ViewModel() {
    val userIntent = Channel<MainIntent>(Channel.UNLIMITED)
    val state = mutableStateOf<MainState>(MainState.Idle)

    private var _stravaCode = MutableStateFlow<String?>(null)
    val stravaCode = _stravaCode.asStateFlow()
    fun setStravaCode(code: String?) {
        Timber.i("setting stravaCode: $code.")
        _stravaCode.value = code
    }

    private var _stravaToken = MutableStateFlow<String?>(null)
    val stravaToken = _stravaToken.asStateFlow()
    fun setStravaToken(token: String?) {
        Timber.i("setting stravaToken: $token.")
        _stravaToken.value = token
    }

    init {
        handleIntent()
    }

    private fun handleIntent() {
        viewModelScope.launch {
            userIntent.consumeAsFlow().collect { collector ->
                when (collector) {
                    is MainIntent.GetStravaCode -> getStravaCode(ctx = collector.ctx)
                    is MainIntent.FetchStravaToken -> fetchToken(_stravaCode.value)
                    is MainIntent.FetchAthlete -> fetchAthlete(_stravaToken.value)
                }
            }
        }
    }

    private fun getStravaCode(ctx: MainActivity) {
        viewModelScope.launch {
            val storedCode = PreferencesDataStore(TimberApplication.appContext).getString(key= Constants.STRAVA_CODE_KEY)
            if (!storedCode.isNullOrEmpty()) {
                _stravaCode.value = storedCode
                Timber.i("Using saved Strava code from DataStore: $storedCode")
                return@launch
            }
            try {
                val intent = Intent(ctx, AuthActivity::class.java)
                ctx.startActivity(intent)

            } catch (e: Exception) {
                state.value = MainState.Error(e.localizedMessage)
            }
        }
    }

//    fun handleActivityResult(resultCode: Int, data: Intent?) {
//        viewModelScope.launch {
//            state.value = MainState.Loading
//            state.value = try {
//                if (resultCode == RESULT_OK && data != null) {
//                    Timber.i("____Received data: $data")
//                    val code = data.getStringExtra("code") ?: throw Exception("No auth code found")
//                    Timber.i("____Received code: $code")
//                    MainState.SuccessGettingToken(token = repo.getToken(code).access_token)
//                } else {
//                    throw Exception("Authentication failed")
//                }
//            } catch (e: Exception) {
//                MainState.Error(e.localizedMessage)
//            }
//        }
//    }


    private fun fetchToken(code: String?) {
        viewModelScope.launch {
            state.value = MainState.Loading
            val storedToken = PreferencesDataStore(TimberApplication.appContext).getString(key= Constants.STRAVA_TOKEN_KEY)
            if (!storedToken.isNullOrEmpty()) {
                _stravaToken.value = storedToken
                Timber.i("Using saved Strava token from DataStore: $storedToken")
                state.value = MainState.SuccessGettingToken(storedToken)
                return@launch
            }
            state.value = try {
                if (code.isNullOrEmpty()) {
                    throw Exception("No auth code provided")
                }
                _stravaToken.value = repo.getToken(code).access_token
                if (_stravaToken.value.isNullOrEmpty()) {
                    throw Exception("Failed to retrieve Strava token")
                }
                PreferencesDataStore(TimberApplication.appContext).saveString(key= Constants.STRAVA_TOKEN_KEY, value=_stravaToken.value!!)
                MainState.SuccessGettingToken(_stravaToken.value!!)
            } catch (e: Exception) {
                MainState.Error(e.localizedMessage)
            }
        }
    }

    private fun fetchAthlete(token: String?) {
        if (token.isNullOrEmpty()) {
            state.value = MainState.Error("No Strava token provided")
            return
        }
        viewModelScope.launch {
            state.value = MainState.Loading
            state.value = try {
                MainState.SuccessGettingAthlete(firstName = repo.getAthlete(token).firstname ?: "No Athlete Found")
            } catch (e: Exception) {
                MainState.Error(e.localizedMessage)
            }
        }
    }
}