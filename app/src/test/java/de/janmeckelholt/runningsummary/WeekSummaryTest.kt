package de.janmeckelholt.runningsummary

import de.janmeckelholt.runningsummary.data.database.models.ActivityDto
import de.janmeckelholt.runningsummary.data.models.toWeekSummary
import de.janmeckelholt.runningsummary.utils.unixTimeStampToNextSundayMidnight
import de.janmeckelholt.runningsummary.utils.unixTimeStampToPreviousMondayMorning
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

class WeekSummaryTest {

    @Test
    fun testWeekSummaryCalculation() {
        // Monday 2025-05-12 00:00:00 UTC
        val mondayEpoch = 1747008000L

        val activities = listOf(
            ActivityDto(
                id = "1",
                athleteId = 1L,
                name = "Monday Run",
                distance = 10000.0,
                movingTime = 3600,
                elapsedTime = 3600,
                totalElevationGain = 100.0,
                type = "Run",
                startDate = "2025-05-12T08:00:00Z",
                startDateLocal = "2025-05-12T08:00:00Z",
                startDateUnix = mondayEpoch + 8 * 3600,
                timezone = "UTC",
                utcOffset = 0.0,
                sportType = "Run"
            ),
            ActivityDto(
                id = "2",
                athleteId = 1L,
                name = "Wednesday Walk",
                distance = 5000.0,
                movingTime = 1800,
                elapsedTime = 1800,
                totalElevationGain = 50.0,
                type = "Walk",
                startDate = "2025-05-14T10:00:00Z",
                startDateLocal = "2025-05-14T10:00:00Z",
                startDateUnix = mondayEpoch + 2 * 86400 + 10 * 3600,
                timezone = "UTC",
                utcOffset = 0.0,
                sportType = "Walk"
            ),
            ActivityDto(
                id = "3",
                athleteId = 1L,
                name = "Sunday Run",
                distance = 12000.0,
                movingTime = 4200,
                elapsedTime = 4200,
                totalElevationGain = 150.0,
                type = "Run",
                startDate = "2025-05-18T18:00:00Z",
                startDateLocal = "2025-05-18T18:00:00Z",
                startDateUnix = mondayEpoch + 6 * 86400 + 18 * 3600,
                timezone = "UTC",
                utcOffset = 0.0,
                sportType = "Run"
            ),
            ActivityDto(
                id = "4",
                athleteId = 1L,
                name = "Next Week Run",
                distance = 15000.0,
                movingTime = 5000,
                elapsedTime = 5000,
                totalElevationGain = 200.0,
                type = "Run",
                startDate = "2025-05-19T08:00:00Z",
                startDateLocal = "2025-05-19T08:00:00Z",
                startDateUnix = mondayEpoch + 7 * 86400 + 8 * 3600,
                timezone = "UTC",
                utcOffset = 0.0,
                sportType = "Run"
            )
        )

        val summary = activities.toWeekSummary(mondayEpoch)

        assertEquals(2, summary.numberOfRuns)
        assertEquals(1, summary.numberOfOthers)
        assertEquals(22000.0, summary.totalDistance, 0.01)
        assertEquals(7800, summary.totalMovingTime)
        assertEquals(250.0, summary.totalElevationGain, 0.01)
    }

    @Test
    fun testDeduplicationOfGarminAndStrava() {
        // Monday 2025-05-12 00:00:00 UTC
        val mondayEpoch = 1747008000L

        val activities = listOf(
            // Run 1: Monday Morning (Garmin + Strava)
            ActivityDto(
                id = "garmin_run_1",
                name = "Morning Run Garmin",
                distance = 5000.0,
                movingTime = 1800,
                elapsedTime = 1800,
                totalElevationGain = 50.0,
                type = "Run",
                startDate = "2025-05-12T08:00:00Z",
                startDateLocal = "2025-05-12T08:00:00Z",
                startDateUnix = mondayEpoch + 8 * 3600,
                sportType = "Run"
            ),
            ActivityDto(
                id = "strava_run_1",
                name = "Morning Run Strava",
                distance = 5000.0,
                movingTime = 1795,
                elapsedTime = 1795,
                totalElevationGain = 48.0,
                type = "Run",
                startDate = "2025-05-12T08:00:30Z",
                startDateLocal = "2025-05-12T08:00:30Z",
                startDateUnix = mondayEpoch + 8 * 3600 + 30, // 30s diff
                sportType = "Run"
            ),
            // Run 2: Tuesday Morning (Garmin + Strava)
            ActivityDto(
                id = "garmin_run_2",
                name = "Tuesday Morning Run Garmin",
                distance = 6000.0,
                movingTime = 2100,
                elapsedTime = 2100,
                totalElevationGain = 60.0,
                type = "Run",
                startDate = "2025-05-13T07:00:00Z",
                startDateLocal = "2025-05-13T07:00:00Z",
                startDateUnix = mondayEpoch + 86400 + 7 * 3600,
                sportType = "Run"
            ),
            ActivityDto(
                id = "strava_run_2",
                name = "Tuesday Morning Run Strava",
                distance = 6000.0,
                movingTime = 2100,
                elapsedTime = 2100,
                totalElevationGain = 58.0,
                type = "Run",
                startDate = "2025-05-13T07:01:00Z",
                startDateLocal = "2025-05-13T07:01:00Z",
                startDateUnix = mondayEpoch + 86400 + 7 * 3600 + 60, // 60s diff
                sportType = "Run"
            ),
            // Run 3: Tuesday Afternoon (Garmin + Strava)
            ActivityDto(
                id = "garmin_run_3",
                name = "Tuesday Afternoon Run Garmin",
                distance = 7000.0,
                movingTime = 2400,
                elapsedTime = 2400,
                totalElevationGain = 70.0,
                type = "Run",
                startDate = "2025-05-13T16:00:00Z",
                startDateLocal = "2025-05-13T16:00:00Z",
                startDateUnix = mondayEpoch + 86400 + 16 * 3600,
                sportType = "Run"
            ),
            ActivityDto(
                id = "strava_run_3",
                name = "Tuesday Afternoon Run Strava",
                distance = 7000.0,
                movingTime = 2400,
                elapsedTime = 2400,
                totalElevationGain = 68.0,
                type = "Run",
                startDate = "2025-05-13T16:02:00Z",
                startDateLocal = "2025-05-13T16:02:00Z",
                startDateUnix = mondayEpoch + 86400 + 16 * 3600 + 120, // 2m diff
                sportType = "Run"
            ),
            // Strength: Tuesday Evening (Garmin Strength + Strava "Other")
            ActivityDto(
                id = "garmin_strength",
                name = "Strength Training Garmin",
                distance = 0.0,
                movingTime = 2700,
                elapsedTime = 2700,
                totalElevationGain = 0.0,
                type = "Strength",
                startDate = "2025-05-13T18:00:00Z",
                startDateLocal = "2025-05-13T18:00:00Z",
                startDateUnix = mondayEpoch + 86400 + 18 * 3600,
                sportType = "Strength"
            ),
            ActivityDto(
                id = "strava_strength",
                name = "Evening Workout Strava",
                distance = 0.0,
                movingTime = 2700,
                elapsedTime = 2700,
                totalElevationGain = 0.0,
                type = "Other",
                startDate = "2025-05-13T18:00:10Z",
                startDateLocal = "2025-05-13T18:00:10Z",
                startDateUnix = mondayEpoch + 86400 + 18 * 3600 + 10,
                sportType = "Other"
            )
        )

        // 8 raw activities (duplicates from both Garmin and Strava)
        assertEquals(8, activities.size)

        val summary = activities.toWeekSummary(mondayEpoch)

        // Must resolve to exactly 3 runs and 1 other (strength)
        assertEquals(3, summary.numberOfRuns)
        assertEquals(1, summary.numberOfOthers)
        assertEquals(18000.0, summary.totalDistance, 0.01) // 5k + 6k + 7k
        assertEquals(6300, summary.totalMovingTime) // 1800 + 2100 + 2400
        assertEquals(180.0, summary.totalElevationGain, 0.01) // 50 + 60 + 70
    }

    @Test
    fun testMondayAndSundayStartEndCalculation() {
        // Wednesday 2025-01-15 12:34:56 UTC
        val WednesdayEpoch = 1736944496L

        val mondayEpoch = unixTimeStampToPreviousMondayMorning(WednesdayEpoch)
        val sundayEpoch = unixTimeStampToNextSundayMidnight(WednesdayEpoch)

        val mondayLocal = Instant.ofEpochSecond(mondayEpoch).atZone(ZoneId.systemDefault())
        val sundayLocal = Instant.ofEpochSecond(sundayEpoch).atZone(ZoneId.systemDefault())

        assertEquals(DayOfWeek.MONDAY, mondayLocal.dayOfWeek)
        assertEquals(0, mondayLocal.hour)
        assertEquals(0, mondayLocal.minute)
        assertEquals(0, mondayLocal.second)

        assertEquals(DayOfWeek.SUNDAY, sundayLocal.dayOfWeek)
        assertEquals(23, sundayLocal.hour)
        assertEquals(59, sundayLocal.minute)
        assertEquals(59, sundayLocal.second)
    }
}
