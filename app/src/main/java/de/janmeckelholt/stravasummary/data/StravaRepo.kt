package de.janmeckelholt.stravasummary.data

import de.janmeckelholt.stravasummary.BuildConfig
import de.janmeckelholt.stravasummary.Constants
import de.janmeckelholt.stravasummary.data.database.StravaDao
import de.janmeckelholt.stravasummary.data.database.models.ActivityDto
import de.janmeckelholt.stravasummary.data.database.models.AthleteDto
import de.janmeckelholt.stravasummary.data.network.StravaApi
import de.janmeckelholt.stravasummary.data.network.models.StravaRefreshTokenRequestBody
import de.janmeckelholt.stravasummary.data.network.models.StravaTokenRequestWithCodeBody
import de.janmeckelholt.stravasummary.data.network.models.StravaTokenResponse
import de.janmeckelholt.stravasummary.utils.unixTimeStampToNextSundayMidnight
import de.janmeckelholt.stravasummary.utils.unixTimeStampToPreviousMondayMorning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber

class StravaRepo(private val api: StravaApi, private val dao: StravaDao) {
    suspend fun getTokenWithCode(code: String): StravaTokenResponse {
        Timber.i("___StravaRepo: Getting token with code... $code")
        val ci = BuildConfig.STRAVA_CLIENT_ID
        Timber.i("___StravaRepo: Client ID: $ci")
        return api.getTokenWithCode(
            body = StravaTokenRequestWithCodeBody(
                code = code,
                client_id = BuildConfig.STRAVA_CLIENT_ID,
                client_secret = BuildConfig.STRAVA_CLIENT_SECRET,
                grant_type = Constants.STRAVA_GRANT_TYPE_CODE
            )
        )
    }

    suspend fun refreshToken(refreshToken: String): StravaTokenResponse = api.refreshToken(
        body = StravaRefreshTokenRequestBody(
            refresh_token = refreshToken,
            client_id = BuildConfig.STRAVA_CLIENT_ID,
            client_secret = BuildConfig.STRAVA_CLIENT_SECRET,
            grant_type = Constants.STRAVA_GRANT_TYPE_REFRESH_TOKEN
        )
    )

    suspend fun fetchAthlete(): AthleteDto = api.getAthlete().toAthleteDto()
    suspend fun fetchWeekActivities(unixTimeStamp: Long): List<ActivityDto>? = api.getActivities(before = unixTimeStampToNextSundayMidnight(unixTimeStamp), after = unixTimeStampToPreviousMondayMorning(unixTimeStamp)).map { it.toActivityDto() }
    fun getAthlete(id: Long): AthleteDto? = dao.getAthleteById(id = id)
    fun getActivities(before: Long, after: Long): Flow<List<ActivityDto>?> = dao.getActivitiesLiveData(before = before, after = after)
    fun getWeekActivities(unixTimeStamp: Long): Flow<List<ActivityDto>?> = dao.getActivitiesLiveData(before = unixTimeStampToNextSundayMidnight(unixTimeStamp), after = unixTimeStampToPreviousMondayMorning(unixTimeStamp))
    suspend fun insertAllActivities(vararg activities: ActivityDto) {
        withContext(Dispatchers.IO){
            dao.insertAllActivities(activities = activities)
        }
    }

    suspend fun insertAllAthletes(vararg athletes: AthleteDto) {
        withContext(Dispatchers.IO){
            dao.insertAllAthletes(athletes = athletes)
        }
    }

}