package de.janmeckelholt.stravasummary.data

import de.janmeckelholt.stravasummary.BuildConfig
import de.janmeckelholt.stravasummary.Constants
import de.janmeckelholt.stravasummary.data.database.StravaDao
import de.janmeckelholt.stravasummary.data.database.models.ActivityDto
import de.janmeckelholt.stravasummary.data.database.models.AthleteDto
import de.janmeckelholt.stravasummary.data.network.StravaApi
import de.janmeckelholt.stravasummary.data.network.StravaResult
import de.janmeckelholt.stravasummary.data.network.models.StravaRefreshTokenRequestBody
import de.janmeckelholt.stravasummary.data.network.models.StravaTokenRequestWithCodeBody
import de.janmeckelholt.stravasummary.data.network.models.StravaTokenResponse
import de.janmeckelholt.stravasummary.data.network.safeApiCall
import de.janmeckelholt.stravasummary.utils.unixTimeStampToNextSundayMidnight
import de.janmeckelholt.stravasummary.utils.unixTimeStampToPreviousMondayMorning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class StravaRepo(private val api: StravaApi, private val dao: StravaDao) {

    // In-memory cache for activities - stores per day
    private val cacheMutex = Mutex()
    private data class CachedDay(
        val activities: List<ActivityDto>, // Empty list if no activities for that day
        val timestamp: Long
    )
    // Map of day (start of day in unix timestamp) to cached activities for that day
    private val activitiesCache: MutableMap<Long, CachedDay> = mutableMapOf()
    private val cacheValidityDuration = 60 * 60 * 1000L // 1 hour in milliseconds

    /**
     * Gets the start of day (midnight) for a given unix timestamp using timezone.
     */
    private fun getStartOfDay(unixTimestamp: Long): Long {
        val zone = java.time.ZoneId.of("Europe/Berlin")
        val dateTime = java.time.Instant.ofEpochSecond(unixTimestamp).atZone(zone)
        return dateTime
            .with(java.time.LocalTime.MIDNIGHT)
            .toEpochSecond()
    }

    /**
     * Gets all days (as start of day timestamps) between after and before
     */
    private fun getDaysBetween(after: Long, before: Long): List<Long> {
        val days = mutableListOf<Long>()
        var currentDay = getStartOfDay(after)
        val endDay = getStartOfDay(before)

        while (currentDay <= endDay) {
            days.add(currentDay)
            currentDay += 86400 // Add one day
        }
        return days
    }
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
    suspend fun fetchWeekActivities(unixTimeStamp: Long): List<ActivityDto>? {
        val before = unixTimeStampToNextSundayMidnight(unixTimeStamp)
        val after = unixTimeStampToPreviousMondayMorning(unixTimeStamp)

        val requiredDays = getDaysBetween(after, before)
        val currentTime = System.currentTimeMillis()

        // Check if all days in the range are cached and valid
        cacheMutex.withLock {
            val allDaysCached = requiredDays.all { day ->
                val cached = activitiesCache[day]
                cached != null && (currentTime - cached.timestamp) < cacheValidityDuration
            }

            if (allDaysCached) {
                Timber.d("Returning cached activities for range (all ${requiredDays.size} days cached)")
                // Collect all activities from cached days
                return requiredDays.flatMap { day ->
                    activitiesCache[day]?.activities ?: emptyList()
                }
            }
        }

        // Cache miss or expired - fetch from API
        Timber.d("Fetching activities from API (cache incomplete or expired for range)")
        val res = safeApiCall {
            api.getActivities(before = before, after = after)
        }

        return when (res) {
            is StravaResult.Success -> {
                val activities = res.data.map { it.toActivityDto() }

                // Update cache - store activities per day
                cacheMutex.withLock {
                    // Group activities by day
                    val activitiesByDay = activities.groupBy { activity ->
                        getStartOfDay(activity.startDateUnix ?: 0L)
                    }

                    // Cache each day in the requested range
                    requiredDays.forEach { day ->
                        val dayActivities = activitiesByDay[day] ?: emptyList()
                        activitiesCache[day] = CachedDay(
                            activities = dayActivities,
                            timestamp = currentTime
                        )
                        Timber.d("Cached ${dayActivities.size} activities for day $day")
                    }
                }

                activities
            }
            is StravaResult.Error -> {
                null
            }
        }
    }

    suspend fun fetchBackfillActivities(before: Long): StravaResult<List<ActivityDto>> {
        Timber.d("Backfilling activities before $before")
        val res = safeApiCall {
            val response = api.getActivitiesBefore(before = before, perPage = 200)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                throw retrofit2.HttpException(response)
            }
        }

        return when (res) {
            is StravaResult.Success -> {
                val dtos = res.data.map { it.toActivityDto() }
                if (dtos.isNotEmpty()) {
                    insertAllActivities(*dtos.toTypedArray())
                }
                StravaResult.Success(dtos)
            }
            is StravaResult.Error -> res
        }
    }

    /**
     * Clears the in-memory cache for activities
     */
    suspend fun clearActivitiesCache() {
        cacheMutex.withLock {
            activitiesCache.clear()
            Timber.d("Activities cache cleared")
        }
    }


    suspend fun getAthlete(id: Long): AthleteDto? {
        return withContext(Dispatchers.IO) {
            dao.getAthleteById(id = id)
        }
    }

    fun getActivities(before: Long, after: Long): Flow<List<ActivityDto>?> = dao.getActivitiesLiveData(before = before, after = after)
    fun getWeekActivitiesFlow(unixTimeStamp: Long): Flow<List<ActivityDto>?> = dao.getActivitiesLiveData(before = unixTimeStampToNextSundayMidnight(unixTimeStamp), after = unixTimeStampToPreviousMondayMorning(unixTimeStamp))
    suspend fun getWeekActivities(unixTimeStamp: Long): List<ActivityDto>? {
        return withContext(Dispatchers.IO) {
            dao.getActivities(before = unixTimeStampToNextSundayMidnight(unixTimeStamp), after = unixTimeStampToPreviousMondayMorning(unixTimeStamp))
        }
    }

    suspend fun insertAllActivities(vararg activities: ActivityDto) {
        withContext(Dispatchers.IO) {
            dao.insertAllActivities(activities = activities)
        }
    }

    suspend fun insertAllAthletes(vararg athletes: AthleteDto) {
        withContext(Dispatchers.IO) {
            dao.insertAllAthletes(athletes = athletes)
        }
    }

}