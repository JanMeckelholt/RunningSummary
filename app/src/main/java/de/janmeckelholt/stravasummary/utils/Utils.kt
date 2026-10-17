package de.janmeckelholt.stravasummary.utils

import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale

fun Int.ordinal() = "$this" + when {
    (this % 100 in 11..13) -> "th"
    (this % 10) == 1 -> "st"
    (this % 10) == 2 -> "nd"
    (this % 10) == 3 -> "rd"
    else -> "th"
}

fun unixTimeStampToPreviousMondayMorning(timestamp: Long): Long {
    val zone = java.time.ZoneId.of("Europe/Berlin")
    val dateTime = java.time.Instant.ofEpochSecond(timestamp).atZone(zone)
    val monday = dateTime
        .with(ChronoField.HOUR_OF_DAY, 0)
        .with(ChronoField.MINUTE_OF_HOUR, 0)
        .with(ChronoField.SECOND_OF_MINUTE, 0)
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return monday.toEpochSecond()
}

fun unixTimeStampToNextSundayMidnight(timestamp: Long): Long {
    val zone = java.time.ZoneId.of("Europe/Berlin")
    val dateTime = java.time.Instant.ofEpochSecond(timestamp).atZone(zone)
    val nextSunday = dateTime
        .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        .with(java.time.LocalTime.MIDNIGHT)
        .plusDays(1) // Move to 24:00 (i.e., Monday 00:00)
    return nextSunday.toEpochSecond()
}


fun unixTimeStampToDateString(timestamp: Long): String {
    val zone = java.time.ZoneId.of("Europe/Berlin")
    val dateTime = java.time.Instant.ofEpochSecond(timestamp).atZone(zone)
    return dateTime.toLocalDate().toString()
}

fun formatSecondsToHMS(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

fun formatMetersToKm(meters: Double): String {
    return String.format("%.2f km", meters / 1000.0)
}

fun convertMillisToDateStr(millis: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}

