package de.janmeckelholt.stravasummary.data.models

import de.janmeckelholt.stravasummary.data.database.models.ActivityDto

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
        val activityStart = activity.startDateUnix ?: 0L
        activityStart >= startOfWeek && activityStart < startOfWeek + 7 * 24 * 60 * 60
    }
    val numberOfRuns = weekActivities.count { it.type == "Run" }
    val numberOfOthers = weekActivities.size - numberOfRuns
    val totalDistance = weekActivities.sumOf { it.distance ?: 0.0 }
    val totalMovingTime = weekActivities.sumOf { it.movingTime ?: 0 }
    val totalElevationGain = weekActivities.sumOf { it.totalElevationGain ?: 0.0 }

    return WeekSummary(
        startOfWeek = startOfWeek,
        numberOfRuns = numberOfRuns,
        numberOfOthers = numberOfOthers,
        totalDistance = totalDistance,
        totalMovingTime = totalMovingTime,
        totalElevationGain = totalElevationGain
    )
}