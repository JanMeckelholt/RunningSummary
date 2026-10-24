package de.janmeckelholt.runningsummary.utils

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
private val SHORT_DAY_DATE_FORMATTER =
    DateTimeFormatter.ofPattern("EEE, dd.MM.", Locale.getDefault())

fun formatMetersToKm(meters: Double): String {
    val km = meters / 1000.0
    return String.format(Locale.getDefault(), "%.2f km", km)
}

fun formatPacePerKm(meters: Double, totalSeconds: Int): String {
    if (meters <= 0.0 || totalSeconds <= 0) {
        return "- /km"
    }
    val secondsPerKm = kotlin.math.round(totalSeconds / (meters / 1000.0)).toInt()
    val minutes = secondsPerKm / 60
    val seconds = secondsPerKm % 60
    return String.format(Locale.getDefault(), "%d:%02d /km", minutes, seconds)
}

fun formatSecondsToHMS(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

fun unixTimeStampToDateString(epoch: Long): String {
    return Instant.ofEpochSecond(epoch)
        .atZone(ZoneId.systemDefault())
        .format(DATE_FORMATTER)
}

fun unixTimeStampToDateTimeString(epoch: Long): String {
    return Instant.ofEpochSecond(epoch)
        .atZone(ZoneId.systemDefault())
        .format(DATE_TIME_FORMATTER)
}

fun unixTimeStampToShortDayDateString(epoch: Long): String {
    return Instant.ofEpochSecond(epoch)
        .atZone(ZoneId.systemDefault())
        .format(SHORT_DAY_DATE_FORMATTER)
}

fun unixTimeStampToPreviousMondayMorning(epoch: Long): Long {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochSecond(epoch).atZone(zone)
    val mondayDateTime = dateTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val mondayStartOfDay = mondayDateTime.toLocalDate().atStartOfDay(zone)
    return mondayStartOfDay.toEpochSecond()
}

fun unixTimeStampToNextSundayMidnight(epoch: Long): Long {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochSecond(epoch).atZone(zone)
    val sundayDateTime = dateTime.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val sundayEndOfDay = sundayDateTime.toLocalDate().atTime(23, 59, 59).atZone(zone)
    return sundayEndOfDay.toEpochSecond()
}

fun unixTimeStampToNextMondayMorning(epoch: Long): Long {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochSecond(epoch).atZone(zone)
    val mondayDateTime = dateTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val nextMondayStartOfDay = mondayDateTime.toLocalDate().plusWeeks(1).atStartOfDay(zone)
    return nextMondayStartOfDay.toEpochSecond()
}
