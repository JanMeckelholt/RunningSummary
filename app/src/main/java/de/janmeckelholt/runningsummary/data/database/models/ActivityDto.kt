package de.janmeckelholt.runningsummary.data.database.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.janmeckelholt.runningsummary.Constants

@Entity(
    tableName = Constants.ACTIVITY_TABLE_NAME,
    indices = [Index(value = ["startDateUnix"])]
)
data class ActivityDto(
    @PrimaryKey
    val id: String,
    val athleteId: Long = 1L,
    val name: String,
    val distance: Double,
    val movingTime: Int,
    val elapsedTime: Int,
    val totalElevationGain: Double,
    val type: String,
    val startDate: String = "",
    val startDateLocal: String = "",
    val startDateUnix: Long,
    val timezone: String? = null,
    val utcOffset: Double = 0.0,
    val sportType: String = type,
)

fun List<ActivityDto>.deduplicateActivities(): List<ActivityDto> {
    if (isEmpty()) return emptyList()

    val sorted = this.sortedBy { it.startDateUnix }
    val result = mutableListOf<ActivityDto>()

    for (activity in sorted) {
        val duplicateIndex = result.indexOfFirst { existing ->
            val existingIsRun = existing.type.equals("Run", ignoreCase = true)
            val activityIsRun = activity.type.equals("Run", ignoreCase = true)

            // A Run and a non-Run are never duplicates of each other
            if (existingIsRun != activityIsRun) return@indexOfFirst false

            val startDiff = kotlin.math.abs(existing.startDateUnix - activity.startDateUnix)

            val existingEnd = existing.startDateUnix + existing.elapsedTime
            val activityEnd = activity.startDateUnix + activity.elapsedTime
            val latestStart = kotlin.math.max(existing.startDateUnix, activity.startDateUnix)
            val earliestEnd = kotlin.math.min(existingEnd, activityEnd)
            val overlap = kotlin.math.max(0L, earliestEnd - latestStart)

            val minDuration = kotlin.math.min(existing.elapsedTime, activity.elapsedTime)
            val timeMatches = startDiff < 300 || (minDuration > 0 && overlap > minDuration / 2)

            if (!timeMatches) return@indexOfFirst false

            if (existingIsRun && activityIsRun) return@indexOfFirst true

            // For non-runs, prefer matching if either is generic or both are non-runs with overlapping time
            true
        }

        if (duplicateIndex != -1) {
            val existing = result[duplicateIndex]
            result[duplicateIndex] = selectBetterActivity(existing, activity)
        } else {
            result.add(activity)
        }
    }

    return result
}

private fun selectBetterActivity(a: ActivityDto, b: ActivityDto): ActivityDto {
    val aIsRun = a.type.equals("Run", ignoreCase = true)
    val bIsRun = b.type.equals("Run", ignoreCase = true)
    if (aIsRun && !bIsRun) return a
    if (!aIsRun && bIsRun) return b

    val aIsOther = a.type.equals("Other", ignoreCase = true) || a.type.isBlank()
    val bIsOther = b.type.equals("Other", ignoreCase = true) || b.type.isBlank()
    if (!aIsOther && bIsOther) return a
    if (aIsOther && !bIsOther) return b

    if (a.distance > 0.0 && b.distance == 0.0) return a
    if (b.distance > 0.0 && a.distance == 0.0) return b

    if (a.totalElevationGain > 0.0 && b.totalElevationGain == 0.0) return a
    if (b.totalElevationGain > 0.0 && a.totalElevationGain == 0.0) return b

    return if (a.elapsedTime >= b.elapsedTime) a else b
}
