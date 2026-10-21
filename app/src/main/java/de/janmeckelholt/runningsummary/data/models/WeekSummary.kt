package de.janmeckelholt.runningsummary.data.models

import de.janmeckelholt.runningsummary.data.database.models.ActivityDto
import de.janmeckelholt.runningsummary.data.database.models.deduplicateActivities

data class WeekSummary(
    val startOfWeek: Long,
    val numberOfRuns: Int,
    val numberOfOthers: Int,
    val totalDistance: Double,
    val totalMovingTime: Int,
    val totalElevationGain: Double,
)

fun List<ActivityDto>.toWeekSummary(startOfWeek: Long): WeekSummary {
    val deduplicated = this.deduplicateActivities()
    val weekActivities = deduplicated.filter { activity ->
        activity.startDateUnix >= startOfWeek && activity.startDateUnix < startOfWeek + 7 * 24 * 60 * 60
    }
    val runs = weekActivities.filter { it.type.equals("Run", ignoreCase = true) }
    val numberOfRuns = runs.size
    val numberOfOthers = weekActivities.size - numberOfRuns
    val totalDistance = runs.sumOf { it.distance }
    val totalMovingTime = runs.sumOf { it.movingTime }
    val totalElevationGain = runs.sumOf { it.totalElevationGain }

    return WeekSummary(
        startOfWeek = startOfWeek,
        numberOfRuns = numberOfRuns,
        numberOfOthers = numberOfOthers,
        totalDistance = totalDistance,
        totalMovingTime = totalMovingTime,
        totalElevationGain = totalElevationGain
    )
}
