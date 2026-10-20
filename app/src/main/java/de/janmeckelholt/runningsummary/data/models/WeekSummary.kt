package de.janmeckelholt.runningsummary.data.models

import de.janmeckelholt.runningsummary.data.database.models.ActivityDto

data class WeekSummary(
    val startOfWeek: Long,
    val numberOfRuns: Int,
    val numberOfOthers: Int,
    val totalDistance: Double,
    val totalMovingTime: Int,
    val totalElevationGain: Double,
)

fun List<ActivityDto>.toWeekSummary(startOfWeek: Long): WeekSummary {
    val weekActivities = this.filter { activity ->
        activity.startDateUnix >= startOfWeek && activity.startDateUnix < startOfWeek + 7 * 24 * 60 * 60
    }
    val numberOfRuns = weekActivities.count { it.type.equals("Run", ignoreCase = true) }
    val numberOfOthers = weekActivities.size - numberOfRuns
    val totalDistance = weekActivities.sumOf { it.distance }
    val totalMovingTime = weekActivities.sumOf { it.movingTime }
    val totalElevationGain = weekActivities.sumOf { it.totalElevationGain }

    return WeekSummary(
        startOfWeek = startOfWeek,
        numberOfRuns = numberOfRuns,
        numberOfOthers = numberOfOthers,
        totalDistance = totalDistance,
        totalMovingTime = totalMovingTime,
        totalElevationGain = totalElevationGain
    )
}
