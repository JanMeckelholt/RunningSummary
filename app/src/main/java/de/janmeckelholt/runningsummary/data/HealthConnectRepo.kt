package de.janmeckelholt.runningsummary.data

import androidx.health.connect.client.records.ExerciseSessionRecord
import de.janmeckelholt.runningsummary.data.database.RunningDao
import de.janmeckelholt.runningsummary.data.database.models.ActivityDto
import de.janmeckelholt.runningsummary.data.database.models.deduplicateActivities
import de.janmeckelholt.runningsummary.utils.unixTimeStampToNextSundayMidnight
import de.janmeckelholt.runningsummary.utils.unixTimeStampToPreviousMondayMorning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectRepo @Inject constructor(
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
        ).map { activities ->
            activities?.deduplicateActivities()
        }
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
            val sessionOrigin = session.metadata.dataOrigin
            val sessionStart = session.startTime.minusSeconds(60)
            val sessionEnd = session.endTime.plusSeconds(60)

            val sameOriginDistance = distanceRecords.filter {
                it.metadata.dataOrigin == sessionOrigin &&
                        it.startTime >= sessionStart && it.endTime <= sessionEnd
            }
            val sessionDistanceMeters = if (sameOriginDistance.isNotEmpty()) {
                sameOriginDistance.sumOf { it.distance.inMeters }
            } else {
                val contained = distanceRecords.filter {
                    it.startTime >= sessionStart && it.endTime <= sessionEnd
                }
                val bestOrigin = contained.firstOrNull()?.metadata?.dataOrigin
                contained.filter { it.metadata.dataOrigin == bestOrigin }
                    .sumOf { it.distance.inMeters }
            }

            val sameOriginElevation = elevationRecords.filter {
                it.metadata.dataOrigin == sessionOrigin &&
                        it.startTime >= sessionStart && it.endTime <= sessionEnd
            }
            val sessionElevationMeters = if (sameOriginElevation.isNotEmpty()) {
                sameOriginElevation.sumOf { it.elevation.inMeters }
            } else {
                val contained = elevationRecords.filter {
                    it.startTime >= sessionStart && it.endTime <= sessionEnd
                }
                val bestOrigin = contained.firstOrNull()?.metadata?.dataOrigin
                contained.filter { it.metadata.dataOrigin == bestOrigin }
                    .sumOf { it.elevation.inMeters }
            }

            Timber.i("___ Matched session '${session.title ?: session.metadata.id}': distance=$sessionDistanceMeters m, elevation=$sessionElevationMeters m (origin=${sessionOrigin.packageName})")

            session.toActivityDto(
                distanceMeters = sessionDistanceMeters,
                elevationMeters = sessionElevationMeters
            )
        }

        if (activities.isNotEmpty()) {
            Timber.i("___ Saving ${activities.size} activities to Room DB (replacing range $startInstant to $endInstant)")
            withContext(Dispatchers.IO) {
                dao.deleteActivities(
                    before = endInstant.epochSecond,
                    after = startInstant.epochSecond
                )
                dao.insertAllActivities(*activities.toTypedArray())
            }
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

    private fun List<ExerciseSessionRecord>.deduplicate(): List<ExerciseSessionRecord> {
        if (isEmpty()) return emptyList()

        val sorted = this.sortedBy { it.startTime }
        val result = mutableListOf<ExerciseSessionRecord>()

        for (session in sorted) {
            val duplicateIndex = result.indexOfFirst { existing ->
                areSessionDuplicates(existing, session)
            }

            if (duplicateIndex != -1) {
                val existing = result[duplicateIndex]
                result[duplicateIndex] = selectPreferredSession(existing, session)
            } else {
                result.add(session)
            }
        }

        return result
    }

    private fun areSessionDuplicates(
        existing: ExerciseSessionRecord,
        session: ExerciseSessionRecord
    ): Boolean {
        val samePackage =
            existing.metadata.dataOrigin.packageName == session.metadata.dataOrigin.packageName
        if (samePackage) {
            // Same app logging multiple workouts - only duplicate if exact same ID
            return existing.metadata.id == session.metadata.id
        }

        // Different apps (e.g. Garmin and Strava) logging the same physical workout
        val startDiffSeconds = kotlin.math.abs(
            java.time.Duration.between(existing.startTime, session.startTime).seconds
        )

        val latestStart =
            if (existing.startTime.isAfter(session.startTime)) existing.startTime else session.startTime
        val earliestEnd =
            if (existing.endTime.isBefore(session.endTime)) existing.endTime else session.endTime
        val overlapSeconds = if (latestStart.isBefore(earliestEnd)) {
            java.time.Duration.between(latestStart, earliestEnd).seconds
        } else {
            0L
        }

        val existingDuration =
            java.time.Duration.between(existing.startTime, existing.endTime).seconds
        val sessionDuration = java.time.Duration.between(session.startTime, session.endTime).seconds
        val minDuration = kotlin.math.min(existingDuration, sessionDuration)

        val timeMatches =
            startDiffSeconds < 300 || (minDuration > 0 && overlapSeconds > minDuration / 2)
        if (!timeMatches) return false

        // Exercise type compatibility
        val existingIsRun = isRunType(existing.exerciseType)
        val sessionIsRun = isRunType(session.exerciseType)
        if (existingIsRun && sessionIsRun) return true

        val existingIsGeneric = isGenericWorkoutType(existing.exerciseType)
        val sessionIsGeneric = isGenericWorkoutType(session.exerciseType)
        if (existingIsGeneric || sessionIsGeneric) return true

        // If neither is a run (e.g. Strength vs Other/Workout), overlapping time means same workout
        if (!existingIsRun && !sessionIsRun) return true

        return existing.exerciseType == session.exerciseType
    }

    private fun isRunType(exerciseType: Int): Boolean {
        return exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING ||
                exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL
    }

    private fun isGenericWorkoutType(exerciseType: Int): Boolean {
        return exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT ||
                exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS ||
                exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP ||
                exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS ||
                exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
    }

    private fun selectPreferredSession(
        existing: ExerciseSessionRecord,
        session: ExerciseSessionRecord
    ): ExerciseSessionRecord {
        val sessionIsGarmin =
            session.metadata.dataOrigin.packageName.contains("garmin", ignoreCase = true)
        val existingIsGarmin =
            existing.metadata.dataOrigin.packageName.contains("garmin", ignoreCase = true)

        if (sessionIsGarmin && !existingIsGarmin) {
            return session
        } else if (!sessionIsGarmin && existingIsGarmin) {
            return existing
        }

        // Prefer specific exercise type over generic OTHER_WORKOUT
        val existingIsGeneric =
            existing.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        val sessionIsGeneric =
            session.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        if (!sessionIsGeneric && existingIsGeneric) return session
        if (!existingIsGeneric && sessionIsGeneric) return existing

        val sessionDuration = java.time.Duration.between(session.startTime, session.endTime).seconds
        val existingDuration =
            java.time.Duration.between(existing.startTime, existing.endTime).seconds
        return if (sessionDuration > existingDuration) session else existing
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
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
            ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> "Strength"

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
