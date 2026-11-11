package com.erengokturk.booklate.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private val shortDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("tr-TR"))
    private val dateTime = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("tr-TR"))
    private val zone: ZoneId = ZoneId.systemDefault()

    fun date(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(zone).format(shortDate)

    fun dateTime(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(zone).format(dateTime)

    fun relative(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val diff = (now - timestamp).coerceAtLeast(0L)
        val minutes = diff / 60_000L
        val hours = diff / 3_600_000L
        val days = diff / 86_400_000L
        return when {
            minutes < 1 -> "az önce"
            minutes < 60 -> "$minutes dk önce"
            hours < 24 -> "$hours saat önce"
            days == 1L -> "dün"
            days < 7 -> "$days gün önce"
            else -> date(timestamp)
        }
    }

    fun bytes(value: Long): String {
        if (value <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = value.toDouble()
        var unit = 0
        while (size >= 1024 && unit < units.lastIndex) {
            size /= 1024
            unit++
        }
        return if (unit == 0) "${size.toInt()} ${units[unit]}"
        else String.format(Locale.US, "%.1f %s", size, units[unit])
    }

    fun speed(value: Float): String = String.format(Locale.US, "%.2f×", value).replace(",", ".")
}
