package com.lx.tracklist.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Shared UTC date-only formatters and parsers to keep calendar dates identical across time zones.
 */
object DateFormats {
    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")

    /** Formats an epoch timestamp as a UTC calendar date ("MMM d, yyyy"). */
    fun formatDateOnly(epochMillis: Long, locale: Locale = Locale.US): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", locale).apply {
            timeZone = UTC
        }
        return sdf.format(Date(epochMillis))
    }

    /** Parses an ISO date-only string ("yyyy-MM-dd") as UTC midnight epoch milliseconds, or null if invalid. */
    fun parseDateOnly(dateString: String?): Long? {
        val trimmed = dateString?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = UTC
            isLenient = false
        }
        return try {
            sdf.parse(trimmed)?.time
        } catch (_: Exception) {
            null
        }
    }
}
