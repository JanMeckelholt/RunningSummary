package de.janmeckelholt.stravasummary.utils

import java.time.DayOfWeek
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters


fun unixTimeStampToPreviousMondayMorning(timestamp: Long): Long {
    val zone = java.time.ZoneId.systemDefault()
    val dateTime = java.time.Instant.ofEpochSecond(timestamp).atZone(zone)
    val monday = dateTime
        .with(ChronoField.HOUR_OF_DAY, 0)
        .with(ChronoField.MINUTE_OF_HOUR, 0)
        .with(ChronoField.SECOND_OF_MINUTE, 0)
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return monday.toEpochSecond()
}

fun unixTimeStampToNextSundayMidnight(timestamp: Long): Long {
    val zone = java.time.ZoneId.systemDefault()
    val dateTime = java.time.Instant.ofEpochSecond(timestamp).atZone(zone)
    val nextSunday = dateTime
        .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
        .with(java.time.LocalTime.MIDNIGHT)
        .plusDays(1) // Move to 24:00 (i.e., Monday 00:00)
    return nextSunday.toEpochSecond()
}

