package de.janmeckelholt.runningsummary.data

import androidx.health.connect.client.records.ExerciseSessionRecord
import de.janmeckelholt.runningsummary.data.database.RunningDao
import de.janmeckelholt.runningsummary.data.database.models.ActivityDto
import de.janmeckelholt.runningsummary.utils.unixTimeStampToNextSundayMidnight
import de.janmeckelholt.runningsummary.utils.unixTimeStampToPreviousMondayMorning
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectRepo @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val dao: RunningDao
) {
    fun getWeekActivitiesFlow(unixTimeStamp: Long): Flow<List<ActivityDto>> {
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
        val effectiveStart = if (healthConnectManager.hasHistoryPermission()) {
            startInstant
        } else {
            val earliestAllowed = Instant.now().minusSeconds(29L * 24 * 60 * 60)
            if (startInstant.isBefore(earliestAllowed)) earliestAllowed else startInstant
        }

        if (!effectiveStart.isBefore(endInstant)) {
            Timber.w("___ Requested range $startInstant..$endInstant is older than allowed history window without READ_HEALTH_DATA_HISTORY")
            return emptyList()
        }

        Timber.i("___ Fetching Health Connect sessions from $effectiveStart to $endInstant")
        val rawSessions = healthConnectManager.readExerciseSessions(effectiveStart, endInstant)
        Timber.i("___ Health Connect returned ${rawSessions.size} raw exercise sessions for range $effectiveStart to $endInstant")

        if (rawSessions.isEmpty()) {
            Timber.i("___ No activities found, clearing range in Room DB")
            dao.replaceActivitiesInRange(
                before = endInstant.epochSecond,
                after = effectiveStart.epochSecond,
                activities = emptyList()
            )
            return emptyList()
        }

        val sessions = rawSessions.deduplicate()
        Timber.i("___ After deduplication: ${sessions.size} sessions remain")

        val minStart = sessions.minOf { it.startTime }.minusSeconds(60)
        val maxEnd = sessions.maxOf { it.endTime }.plusSeconds(60)

        val (distanceRecords, elevationRecords) = coroutineScope {
            val distDeferred = async { healthConnectManager.readDistanceRecords(minStart, maxEnd) }
            val elevDeferred =
                async { healthConnectManager.readElevationGainedRecords(minStart, maxEnd) }
            Pair(distDeferred.await(), elevDeferred.await())
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
                contained.groupBy { it.metadata.dataOrigin }
                    .values
                    .maxOfOrNull { list -> list.sumOf { it.distance.inMeters } } ?: 0.0
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
                contained.groupBy { it.metadata.dataOrigin }
                    .values
                    .maxOfOrNull { list -> list.sumOf { it.elevation.inMeters } } ?: 0.0
            }

            Timber.i("___ Matched session '${session.title ?: session.metadata.id}': distance=$sessionDistanceMeters m, elevation=$sessionElevationMeters m (origin=${sessionOrigin.packageName})")

            session.toActivityDto(
                distanceMeters = sessionDistanceMeters,
                elevationMeters = sessionElevationMeters
            )
        }

        Timber.i("___ Atomically saving ${activities.size} activities to Room DB (replacing range $effectiveStart to $endInstant)")
        dao.replaceActivitiesInRange(
            before = endInstant.epochSecond,
            after = effectiveStart.epochSecond,
            activities = activities
        )

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
            return existing.metadata.id == session.metadata.id
        }

        val existingIsRun = isRunType(existing.exerciseType)
        val sessionIsRun = isRunType(session.exerciseType)

        // A run and a non-run workout are not duplicates
        if (existingIsRun != sessionIsRun) return false

        val startDiffSeconds = kotlin.math.abs(
            Duration.between(existing.startTime, session.startTime).seconds
        )

        val latestStart =
            if (existing.startTime.isAfter(session.startTime)) existing.startTime else session.startTime
        val earliestEnd =
            if (existing.endTime.isBefore(session.endTime)) existing.endTime else session.endTime
        val overlapSeconds = if (latestStart.isBefore(earliestEnd)) {
            Duration.between(latestStart, earliestEnd).seconds
        } else {
            0L
        }

        val existingDuration = Duration.between(existing.startTime, existing.endTime).seconds
        val sessionDuration = Duration.between(session.startTime, session.endTime).seconds
        val minDuration = kotlin.math.min(existingDuration, sessionDuration)

        val timeMatches =
            startDiffSeconds < 300 || (minDuration > 0 && overlapSeconds > minDuration / 2)
        if (!timeMatches) return false

        if (existingIsRun && sessionIsRun) return true

        // Both are non-runs with matching time
        return true
    }

    private fun isRunType(exerciseType: Int): Boolean {
        return exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING ||
                exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL
    }

    private fun selectPreferredSession(
        existing: ExerciseSessionRecord,
        session: ExerciseSessionRecord
    ): ExerciseSessionRecord {
        val existingIsRun = isRunType(existing.exerciseType)
        val sessionIsRun = isRunType(session.exerciseType)
        if (existingIsRun && !sessionIsRun) return existing
        if (!existingIsRun && sessionIsRun) return session

        val sessionIsGarmin =
            session.metadata.dataOrigin.packageName.contains("garmin", ignoreCase = true)
        val existingIsGarmin =
            existing.metadata.dataOrigin.packageName.contains("garmin", ignoreCase = true)

        if (sessionIsGarmin && !existingIsGarmin) {
            return session
        } else if (!sessionIsGarmin && existingIsGarmin) {
            return existing
        }

        val existingIsGeneric =
            existing.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        val sessionIsGeneric =
            session.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        if (!sessionIsGeneric && existingIsGeneric) return session
        if (!existingIsGeneric && sessionIsGeneric) return existing

        val sessionDuration = Duration.between(session.startTime, session.endTime).seconds
        val existingDuration = Duration.between(existing.startTime, existing.endTime).seconds
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
            else -> "Other"
        }

        val duration = Duration.between(startTime, endTime).seconds.toInt()

        return ActivityDto(
            id = metadata.id,
            athleteId = 1L,
            name = title ?: mappedType,
            distance = distanceMeters,
            movingTime = duration,
            elapsedTime = duration,
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
