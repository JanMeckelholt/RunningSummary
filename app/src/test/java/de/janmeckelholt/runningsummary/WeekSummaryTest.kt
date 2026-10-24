package de.janmeckelholt.runningsummary

import de.janmeckelholt.runningsummary.data.database.models.ActivityDto
import de.janmeckelholt.runningsummary.data.models.toWeekSummary
import de.janmeckelholt.runningsummary.utils.formatPacePerKm
import de.janmeckelholt.runningsummary.utils.unixTimeStampToNextMondayMorning
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
                name = "Monday Run",
                distance = 10000.0,
                movingTime = 3600,
                elapsedTime = 3600,
                totalElevationGain = 100.0,
                type = "Run",
                startDateUnix = mondayEpoch + 8 * 3600,
            ),
            ActivityDto(
                id = "2",
                name = "Wednesday Walk",
                distance = 5000.0,
                movingTime = 1800,
                elapsedTime = 1800,
                totalElevationGain = 50.0,
                type = "Walk",
                startDateUnix = mondayEpoch + 2 * 86400 + 10 * 3600,
            ),
            ActivityDto(
                id = "3",
                name = "Sunday Run",
                distance = 12000.0,
                movingTime = 4200,
                elapsedTime = 4200,
                totalElevationGain = 150.0,
                type = "Run",
                startDateUnix = mondayEpoch + 6 * 86400 + 18 * 3600,
            ),
            ActivityDto(
                id = "4",
                name = "Next Week Run",
                distance = 15000.0,
                movingTime = 5000,
                elapsedTime = 5000,
                totalElevationGain = 200.0,
                type = "Run",
                startDateUnix = mondayEpoch + 7 * 86400 + 8 * 3600,
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
                startDateUnix = mondayEpoch + 8 * 3600,
            ),
            ActivityDto(
                id = "strava_run_1",
                name = "Morning Run Strava",
                distance = 5000.0,
                movingTime = 1795,
                elapsedTime = 1795,
                totalElevationGain = 48.0,
                type = "Run",
                startDateUnix = mondayEpoch + 8 * 3600 + 30, // 30s diff
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
                startDateUnix = mondayEpoch + 86400 + 7 * 3600,
            ),
            ActivityDto(
                id = "strava_run_2",
                name = "Tuesday Morning Run Strava",
                distance = 6000.0,
                movingTime = 2100,
                elapsedTime = 2100,
                totalElevationGain = 58.0,
                type = "Run",
                startDateUnix = mondayEpoch + 86400 + 7 * 3600 + 60, // 60s diff
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
                startDateUnix = mondayEpoch + 86400 + 16 * 3600,
            ),
            ActivityDto(
                id = "strava_run_3",
                name = "Tuesday Afternoon Run Strava",
                distance = 7000.0,
                movingTime = 2400,
                elapsedTime = 2400,
                totalElevationGain = 68.0,
                type = "Run",
                startDateUnix = mondayEpoch + 86400 + 16 * 3600 + 120, // 2m diff
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
                startDateUnix = mondayEpoch + 86400 + 18 * 3600,
            ),
            ActivityDto(
                id = "strava_strength",
                name = "Evening Workout Strava",
                distance = 0.0,
                movingTime = 2700,
                elapsedTime = 2700,
                totalElevationGain = 0.0,
                type = "Other",
                startDateUnix = mondayEpoch + 86400 + 18 * 3600 + 10,
            )
        )

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
    fun testRunAndStrengthAtSameTimeAreNotDeduplicated() {
        val mondayEpoch = 1747008000L

        // A warm-up run followed immediately or starting at the same time as a strength session
        val activities = listOf(
            ActivityDto(
                id = "run_warmup",
                name = "Warm up run",
                distance = 1500.0,
                movingTime = 480,
                elapsedTime = 480,
                totalElevationGain = 10.0,
                type = "Run",
                startDateUnix = mondayEpoch + 8 * 3600,
            ),
            ActivityDto(
                id = "gym_strength",
                name = "Gym workout",
                distance = 0.0,
                movingTime = 2700,
                elapsedTime = 2700,
                totalElevationGain = 0.0,
                type = "Strength",
                startDateUnix = mondayEpoch + 8 * 3600 + 60, // 1 min later
            )
        )

        val summary = activities.toWeekSummary(mondayEpoch)
        assertEquals(1, summary.numberOfRuns)
        assertEquals(1, summary.numberOfOthers)
        assertEquals(1500.0, summary.totalDistance, 0.01)
    }

    @Test
    fun testMondayAndSundayStartEndCalculation() {
        // Wednesday 2025-01-15 12:34:56 UTC
        val wednesdayEpoch = 1736944496L

        val mondayEpoch = unixTimeStampToPreviousMondayMorning(wednesdayEpoch)
        val sundayEpoch = unixTimeStampToNextSundayMidnight(wednesdayEpoch)
        val nextMondayEpoch = unixTimeStampToNextMondayMorning(wednesdayEpoch)

        val mondayLocal = Instant.ofEpochSecond(mondayEpoch).atZone(ZoneId.systemDefault())
        val sundayLocal = Instant.ofEpochSecond(sundayEpoch).atZone(ZoneId.systemDefault())
        val nextMondayLocal = Instant.ofEpochSecond(nextMondayEpoch).atZone(ZoneId.systemDefault())

        assertEquals(DayOfWeek.MONDAY, mondayLocal.dayOfWeek)
        assertEquals(0, mondayLocal.hour)
        assertEquals(0, mondayLocal.minute)
        assertEquals(0, mondayLocal.second)

        assertEquals(DayOfWeek.SUNDAY, sundayLocal.dayOfWeek)
        assertEquals(23, sundayLocal.hour)
        assertEquals(59, sundayLocal.minute)
        assertEquals(59, sundayLocal.second)

        assertEquals(DayOfWeek.MONDAY, nextMondayLocal.dayOfWeek)
        assertEquals(0, nextMondayLocal.hour)
        assertEquals(0, nextMondayLocal.minute)
        assertEquals(0, nextMondayLocal.second)
    }

    @Test
    fun testDstTransitionWeekIncludesSundayLateNightRun() {
        // October 2026 DST transition week in Europe: Monday 2026-10-19 to Sunday 2026-10-25 (clocks fall back at 03:00)
        // Week start: Monday 2026-10-19 00:00:00 local time
        val zone = ZoneId.systemDefault()
        val mondayEpoch = java.time.LocalDate.of(2026, 10, 19).atStartOfDay(zone).toEpochSecond()
        val nextMondayEpoch = unixTimeStampToNextMondayMorning(mondayEpoch)

        // Activity on Sunday 2026-10-25 at 23:30:00 local time
        val sundayLateRunEpoch =
            java.time.LocalDate.of(2026, 10, 25).atTime(23, 30).atZone(zone).toEpochSecond()

        val activities = listOf(
            ActivityDto(
                id = "sunday_late_run",
                name = "Late Night Sunday Run",
                distance = 8000.0,
                movingTime = 2400,
                elapsedTime = 2400,
                totalElevationGain = 40.0,
                type = "Run",
                startDateUnix = sundayLateRunEpoch,
            )
        )

        val summary = activities.toWeekSummary(mondayEpoch)
        assertEquals(1, summary.numberOfRuns)
        assertEquals(8000.0, summary.totalDistance, 0.01)
    }

    @Test
    fun testWeekSummaryRunsAreOrderedChronologically() {
        val mondayEpoch = 1747008000L

        val activities = listOf(
            ActivityDto(
                id = "friday_run",
                name = "Friday Run",
                distance = 10000.0,
                movingTime = 3000,
                elapsedTime = 3000,
                totalElevationGain = 80.0,
                type = "Run",
                startDateUnix = mondayEpoch + 4 * 86400 + 17 * 3600,
            ),
            ActivityDto(
                id = "wednesday_walk",
                name = "Wednesday Walk",
                distance = 3000.0,
                movingTime = 1800,
                elapsedTime = 1800,
                totalElevationGain = 10.0,
                type = "Walk",
                startDateUnix = mondayEpoch + 2 * 86400 + 10 * 3600,
            ),
            ActivityDto(
                id = "monday_run",
                name = "Monday Run",
                distance = 5000.0,
                movingTime = 1500,
                elapsedTime = 1500,
                totalElevationGain = 30.0,
                type = "Run",
                startDateUnix = mondayEpoch + 8 * 3600,
            )
        )

        val summary = activities.toWeekSummary(mondayEpoch)
        assertEquals(2, summary.runs.size)
        assertEquals("monday_run", summary.runs[0].id)
        assertEquals("friday_run", summary.runs[1].id)
    }

    @Test
    fun testFormatPacePerKm() {
        assertEquals("5:00 /km", formatPacePerKm(10000.0, 3000))
        assertEquals("5:30 /km", formatPacePerKm(5000.0, 1650))
        assertEquals("- /km", formatPacePerKm(0.0, 1000))
        assertEquals("- /km", formatPacePerKm(5000.0, 0))
    }
}
