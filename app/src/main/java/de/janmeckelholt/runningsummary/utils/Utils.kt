package de.janmeckelholt.runningsummary.utils

import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale

fun formatMetersToKm(meters: Double): String {
    val km = meters / 1000.0
    return String.format(Locale.getDefault(), "%.2f km", km)
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
    val date = Date(epoch * 1000)
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return sdf.format(date)
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

fun convertMillisToDateStr(millis: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}
