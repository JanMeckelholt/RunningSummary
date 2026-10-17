package de.janmeckelholt.runningsummary.data

import androidx.health.connect.client.records.ExerciseSessionRecord
import de.janmeckelholt.runningsummary.data.database.RunningDao
import de.janmeckelholt.runningsummary.data.database.models.ActivityDto
import de.janmeckelholt.runningsummary.utils.unixTimeStampToNextSundayMidnight
import de.janmeckelholt.runningsummary.utils.unixTimeStampToPreviousMondayMorning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import timber.log.Timber

class HealthConnectRepo(
    private val healthConnectManager: HealthConnectManager,
    private val dao: RunningDao
) {
    fun getWeekActivitiesFlow(unixTimeStamp: Long): Flow<List<ActivityDto>?> {
        val before = unixTimeStampToNextSundayMidnight(unixTimeStamp)
        val after = unixTimeStampToPreviousMondayMorning(unixTimeStamp)
        Timber.i("___ getWeekActivitiesFlow querying Room DB for startDateUnix between $after and $before")
        return dao.getActivitiesLiveData(
            before = before,
            after = after
        )
    }

    suspend fun fetchAndSaveActivitiesForRange(
        startInstant: Instant,
        endInstant: Instant
    ): List<ActivityDto> {
        Timber.i("___ Fetching Health Connect sessions from $startInstant to $endInstant")
        val rawSessions = try {
            healthConnectManager.readExerciseSessions(startInstant, endInstant)
        } catch (e: Exception) {
            Timber.e(e, "___ Exception reading exercise sessions from Health Connect")
            emptyList()
        }
        Timber.i("___ Health Connect returned ${rawSessions.size} raw exercise sessions for range $startInstant to $endInstant")
        for (session in rawSessions) {
            Timber.i("___ Session found: id=${session.metadata.id}, title=${session.title}, exerciseType=${session.exerciseType}, start=${session.startTime}, end=${session.endTime}, origin=${session.metadata.dataOrigin.packageName}")
        }

        val sessions = rawSessions.deduplicate()
        Timber.i("___ After deduplication: ${sessions.size} sessions remain")

        val distanceRecords = try {
            healthConnectManager.readDistanceRecords(startInstant, endInstant)
        } catch (e: Exception) {
            Timber.e(e, "___ Exception reading distance records")
            emptyList()
        }
        val elevationRecords = try {
            healthConnectManager.readElevationGainedRecords(startInstant, endInstant)
        } catch (e: Exception) {
            Timber.e(e, "___ Exception reading elevation records")
            emptyList()
        }

        val activities = sessions.map { session ->
            // Match distance and elevation records overlapping session duration
            val sessionDistanceMeters = distanceRecords
                .filter { it.startTime <= session.endTime && it.endTime >= session.startTime }
                .sumOf { it.distance.inMeters }

            val sessionElevationMeters = elevationRecords
                .filter { it.startTime <= session.endTime && it.endTime >= session.startTime }
                .sumOf { it.elevation.inMeters }

            session.toActivityDto(
                distanceMeters = sessionDistanceMeters,
                elevationMeters = sessionElevationMeters
            )
        }

        if (activities.isNotEmpty()) {
            Timber.i("___ Saving ${activities.size} activities to Room DB")
            insertAllActivities(*activities.toTypedArray())
        } else {
            Timber.w("___ No activities to save to Room DB for range $startInstant to $endInstant")
        }

        return activities
    }

    suspend fun fetchWeekActivities(unixTimeStamp: Long): List<ActivityDto> {
        val before = unixTimeStampToNextSundayMidnight(unixTimeStamp)
        val after = unixTimeStampToPreviousMondayMorning(unixTimeStamp)

        // Pad start/end instants by 24 hours to ensure all sessions around timezone boundaries are fetched from Health Connect
        val startInstant = Instant.ofEpochSecond(after - 86400)
        val endInstant = Instant.ofEpochSecond(before + 86400)

        Timber.i("___ fetchWeekActivities epoch=$unixTimeStamp -> Monday=$after, Sunday=$before")
        return fetchAndSaveActivitiesForRange(startInstant, endInstant)
    }

    suspend fun insertAllActivities(vararg activities: ActivityDto) {
        withContext(Dispatchers.IO) {
            dao.insertAllActivities(activities = activities)
        }
    }

    fun clearActivitiesCache() {
        // No-op for now, cache is handled by DB replacement policy
    }

    private fun List<ExerciseSessionRecord>.deduplicate(): List<ExerciseSessionRecord> {
        if (isEmpty()) return emptyList()

        val sorted = this.sortedBy { it.startTime }
        val result = mutableListOf<ExerciseSessionRecord>()

        for (session in sorted) {
            val duplicateIndex = result.indexOfFirst { existing ->
                val samePackage = existing.metadata.dataOrigin.packageName == session.metadata.dataOrigin.packageName
                if (samePackage) {
                    // Same app logging multiple workouts - only duplicate if exact same ID
                    existing.metadata.id == session.metadata.id
                } else {
                    // Different apps logging the same physical workout
                    val startDiffSeconds = kotlin.math.abs(
                        java.time.Duration.between(existing.startTime, session.startTime).seconds
                    )
                    startDiffSeconds < 120 && existing.exerciseType == session.exerciseType
                }
            }

            if (duplicateIndex != -1) {
                val existing = result[duplicateIndex]
                val sessionIsGarmin =
                    session.metadata.dataOrigin.packageName.contains("garmin", ignoreCase = true)
                val existingIsGarmin =
                    existing.metadata.dataOrigin.packageName.contains("garmin", ignoreCase = true)

                if (sessionIsGarmin && !existingIsGarmin) {
                    result[duplicateIndex] = session
                } else if (!sessionIsGarmin && existingIsGarmin) {
                    // Keep existing Garmin session
                } else {
                    val sessionDuration =
                        java.time.Duration.between(session.startTime, session.endTime).seconds
                    val existingDuration =
                        java.time.Duration.between(existing.startTime, existing.endTime).seconds
                    if (sessionDuration > existingDuration) {
                        result[duplicateIndex] = session
                    }
                }
            } else {
                result.add(session)
            }
        }

        return result
    }

    private fun ExerciseSessionRecord.toActivityDto(
        distanceMeters: Double,
        elevationMeters: Double
    ): ActivityDto {
        val mappedType = when (exerciseType) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "Run"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Ride"
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walk"
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Hike"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "Swim"

            else -> title ?: "Other"
        }

        return ActivityDto(
            id = metadata.id,
            athleteId = 1L,
            name = title ?: mappedType,
            distance = distanceMeters,
            movingTime = java.time.Duration.between(startTime, endTime).seconds.toInt(),
            elapsedTime = java.time.Duration.between(startTime, endTime).seconds.toInt(),
            totalElevationGain = kotlin.math.floor(elevationMeters),
            type = mappedType,
            startDate = startTime.toString(),
            startDateLocal = startTime.toString(),
            startDateUnix = startTime.epochSecond,
            timezone = startZoneOffset?.id,
            utcOffset = 0.0,
            sportType = mappedType
        )
    }
}
