package com.keithstack.carlog.ui

import com.keithstack.carlog.data.Sighting
import com.keithstack.carlog.data.formatCoords
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val MIN_MS = 60_000L
private const val DAY_MS = 86_400_000L

fun formatHm(ts: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    return "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
}

/** "just now" / "N min ago" / "N h ago" / "today" / "yesterday" / "N days ago" */
fun formatAgo(now: Long, ts: Long): String {
    val minutes = Math.round((now - ts) / MIN_MS.toDouble())
    if (minutes < 1) return "just now"
    if (minutes < 60) return "$minutes min ago"
    if (minutes < 180) return "${Math.round(minutes / 60.0)} h ago"
    val startOfDay = { t: Long -> Calendar.getInstance().apply {
        timeInMillis = t
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis }
    val days = Math.round((startOfDay(now) - startOfDay(ts)) / DAY_MS.toDouble())
    return when (days) {
        0L -> "today"
        1L -> "yesterday"
        else -> "$days days ago"
    }
}

private val dayLabelFormat = SimpleDateFormat("EEE d MMM", Locale.UK)
private val fullDateFormat = SimpleDateFormat("EEE d MMM yyyy", Locale.UK)
private val shortDateFormat = SimpleDateFormat("d MMM yyyy", Locale.UK)

fun formatDayLabel(now: Long, ts: Long): String {
    val ago = formatAgo(now, ts)
    return when {
        ago == "today" || ago.contains("min ago") || ago.contains("h ago") || ago == "just now" -> "Today"
        ago == "yesterday" -> "Yesterday"
        else -> dayLabelFormat.format(ts)
    }
}

fun formatFullDate(ts: Long): String = fullDateFormat.format(ts)
fun formatShortDate(ts: Long): String = shortDateFormat.format(ts)

fun placeText(sighting: Sighting): String = when {
    sighting.lat == null || sighting.lon == null -> "No location"
    sighting.near != null -> "Near ${sighting.near}"
    else -> formatCoords(sighting.lat, sighting.lon)
}
