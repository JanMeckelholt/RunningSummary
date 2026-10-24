package de.janmeckelholt.runningsummary.data.models

import de.janmeckelholt.runningsummary.data.database.models.ActivityDto
import de.janmeckelholt.runningsummary.data.database.models.deduplicateActivities
import de.janmeckelholt.runningsummary.utils.unixTimeStampToNextMondayMorning

data class WeekSummary(
    val startOfWeek: Long,
    val numberOfRuns: Int,
    val numberOfOthers: Int,
    val totalDistance: Double,
    val totalMovingTime: Int,
    val totalElevationGain: Double,
    val runs: List<ActivityDto> = emptyList(),
)

fun List<ActivityDto>.toWeekSummary(startOfWeek: Long): WeekSummary {
    val deduplicated = this.deduplicateActivities()
    val endOfWeek = unixTimeStampToNextMondayMorning(startOfWeek)
    val weekActivities = deduplicated.filter { activity ->
        activity.startDateUnix in startOfWeek until endOfWeek
    }
    val runs = weekActivities
        .filter { it.type.equals("Run", ignoreCase = true) }
        .sortedBy { it.startDateUnix }
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
        totalElevationGain = totalElevationGain,
        runs = runs,
    )
}
