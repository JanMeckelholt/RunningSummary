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
        assertEquals(27000.0, summary.totalDistance, 0.01)
        assertEquals(9600, summary.totalMovingTime)
        assertEquals(300.0, summary.totalElevationGain, 0.01)
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
