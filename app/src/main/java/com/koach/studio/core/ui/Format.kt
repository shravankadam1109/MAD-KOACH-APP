package com.koach.studio.core.ui

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val DayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val DayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val Time = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

/**
 * ₹1,20,000 style: Indian digit grouping (last three digits, then pairs), no
 * decimals. Done by hand because en-IN locale data differs between the JVM and
 * Android releases.
 */
fun formatInr(amount: Long): String {
    val digits = kotlin.math.abs(amount).toString()
    val grouped = if (digits.length <= 3) {
        digits
    } else {
        val head = digits.dropLast(3)
        head.reversed().chunked(2).joinToString(",").reversed() + "," + digits.takeLast(3)
    }
    return (if (amount < 0) "-₹" else "₹") + grouped
}

fun formatDate(date: LocalDate, today: LocalDate = LocalDate.now()): String =
    if (date.year == today.year) date.format(DayMonth) else date.format(DayMonthYear)

fun formatTime(dateTime: LocalDateTime): String = dateTime.format(Time)

/** "Today", "Tomorrow", "In 5 days", "2 days overdue"… for due dates. */
fun formatDue(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days == 0L -> "Due today"
        days == 1L -> "Due tomorrow"
        days in 2..13 -> "Due in $days days"
        days < 0 -> if (days == -1L) "1 day overdue" else "${-days} days overdue"
        else -> "Due ${formatDate(date, today)}"
    }
}

/** Short relative stamp for feeds: "Just now", "3h", "Yesterday", "12 Oct". */
fun formatRelative(dateTime: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
    val minutes = ChronoUnit.MINUTES.between(dateTime, now)
    val days = ChronoUnit.DAYS.between(dateTime.toLocalDate(), now.toLocalDate())
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        days == 0L -> "${minutes / 60}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> formatDate(dateTime.toLocalDate(), now.toLocalDate())
    }
}

fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format(Locale.ENGLISH, "%.1f GB", bytes / 1e9)
    bytes >= 1_000_000 -> String.format(Locale.ENGLISH, "%.1f MB", bytes / 1e6)
    bytes >= 1_000 -> "${bytes / 1_000} KB"
    else -> "$bytes B"
}
