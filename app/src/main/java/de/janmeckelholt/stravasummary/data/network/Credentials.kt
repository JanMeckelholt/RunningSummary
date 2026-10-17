package de.janmeckelholt.stravasummary.data.network

import de.janmeckelholt.stravasummary.Constants
import de.janmeckelholt.stravasummary.PreferencesDataStore
import de.janmeckelholt.stravasummary.TimberApplication
import de.janmeckelholt.stravasummary.data.StravaRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber

object Credentials {
    var accessToken: String? = null
    private var refreshToken: String? = null
    var code: String? = null
    private var repo: StravaRepo? = null
    var athleteId: Long? = null


    fun initialize(stravaRepo: StravaRepo) {
        repo = stravaRepo
        runBlocking {
            withContext(Dispatchers.IO) {
                accessToken = PreferencesDataStore(TimberApplication.appContext).getString(Constants.STRAVA_ACCESS_TOKEN_KEY)
                refreshToken = PreferencesDataStore(TimberApplication.appContext).getString(Constants.STRAVA_REFRESH_TOKEN_KEY)
                code = PreferencesDataStore(TimberApplication.appContext).getString(Constants.STRAVA_CODE_KEY)
                athleteId = PreferencesDataStore(TimberApplication.appContext).getLong(Constants.STRAVA_ATHLETE_ID_KEY)
                Timber.i("___athleteId: $athleteId")
            }
        }
    }

    fun updateCode(newCode: String) {
        code = newCode
        GlobalScope.launch(Dispatchers.IO) {
            PreferencesDataStore(TimberApplication.appContext).saveString(Constants.STRAVA_CODE_KEY, newCode)
        }
    }

    fun updateAthleteId(newAthleteId: Long) {
        athleteId = newAthleteId
        GlobalScope.launch(Dispatchers.IO) {
            PreferencesDataStore(TimberApplication.appContext).saveLong(Constants.STRAVA_ATHLETE_ID_KEY, newAthleteId)
        }
    }

    suspend fun updateAccessToken(): String? {
        withContext(Dispatchers.IO) {
            if (accessToken.isNullOrEmpty()) refreshAccessToken()
        }
        return accessToken
    }

    suspend fun refreshAccessToken(reset: Boolean = false) {
        withContext(Dispatchers.IO) {
            Timber.i("Refreshing tmToken...")
            if (reset) {
                PreferencesDataStore(TimberApplication.appContext).saveString(Constants.STRAVA_ACCESS_TOKEN_KEY, "")
                accessToken = null
            }
            if (code.isNullOrEmpty()) code = PreferencesDataStore(TimberApplication.appContext).getString(Constants.STRAVA_CODE_KEY)
            if (refreshToken.isNullOrEmpty()) refreshToken = PreferencesDataStore(TimberApplication.appContext).getString(Constants.STRAVA_REFRESH_TOKEN_KEY)
            if (accessToken.isNullOrEmpty()) accessToken = PreferencesDataStore(TimberApplication.appContext).getString(Constants.STRAVA_ACCESS_TOKEN_KEY)
            if (accessToken.isNullOrEmpty()) {
                if (!refreshToken.isNullOrEmpty()) {
                    // get token with refresh token
                    Timber.i("using refresh token $refreshToken to get new token...")
                    try {
                        val tokenResponse = repo?.refreshToken(refreshToken = refreshToken!!)
                        accessToken = tokenResponse?.access_token
                        refreshToken = tokenResponse?.refresh_token
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to refresh token with refresh_token")
                        refreshToken = null
                        PreferencesDataStore(TimberApplication.appContext).saveString(Constants.STRAVA_REFRESH_TOKEN_KEY, "")
                        return@withContext
                    }
                } else {
                    // get token with code
                    if (code.isNullOrEmpty()) {
                        Timber.e("No code found to refresh tmToken!")
                        return@withContext
                    }
                    Timber.i("using code $code to get new token...(repo $repo)")

                    try {
                        val tokenResponse = repo?.getTokenWithCode(code = code!!)
                        if (tokenResponse == null) {
                            Timber.e("No token response for code: $code. Resetting code...")
                            PreferencesDataStore(TimberApplication.appContext).saveString(Constants.STRAVA_CODE_KEY, "")
                            code = null
                            return@withContext
                        }
                        Timber.i("___tokenResponse: $tokenResponse")
                        accessToken = tokenResponse.access_token
                        refreshToken = tokenResponse.refresh_token
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to get token with code. Resetting code...")
                        PreferencesDataStore(TimberApplication.appContext).saveString(Constants.STRAVA_CODE_KEY, "")
                        code = null
                        return@withContext
                    }
                }
            }
            PreferencesDataStore(TimberApplication.appContext).saveString(key = Constants.STRAVA_ACCESS_TOKEN_KEY, value = accessToken ?: "")
            PreferencesDataStore(TimberApplication.appContext).saveString(key = Constants.STRAVA_REFRESH_TOKEN_KEY, value = refreshToken ?: "")
        }
    }
}