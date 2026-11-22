package com.lx.tracklist.util

import java.util.Locale
import kotlin.math.roundToInt

object DurationFormatter {

    /**
     * Formats duration in hours (e.g. 2547.9 hrs) into a human readable string.
     * When duration is small (< 24h), shows hours (e.g., "12.5 hrs" or "12h 30m").
     * When duration is large (>= 24h):
     * - >= 24h and < 7d: converted to days (e.g., "1.5 days", "5.2 days")
     * - >= 7d and < 30.5d: converted to weeks (e.g., "1.5 wks", "3.2 wks")
     * - >= 30.5d and < 365.25d: converted to months (e.g., "3.5 mos", "9.1 mos")
     * - >= 365.25d: converted to years (e.g., "1.2 yrs")
     */
    fun formatHours(hours: Double, compact: Boolean = false): String {
        if (hours <= 0.0) return if (compact) "0h" else "0.0 hrs"
        val days = hours / 24.0
        return when {
            hours < 24.0 -> {
                if (compact) {
                    val h = hours.toInt()
                    val m = ((hours - h) * 60).roundToInt()
                    if (m > 0 && h > 0) "${h}h ${m}m" else if (h > 0) "${h}h" else "${m}m"
                } else {
                    String.format(Locale.US, "%.1f hrs", hours)
                }
            }
            days < 7.0 -> {
                val unit = if (compact) "d" else "days"
                String.format(Locale.US, "%.1f %s", days, unit)
            }
            days < 30.4375 -> {
                val weeks = days / 7.0
                val unit = if (compact) "w" else "wks"
                String.format(Locale.US, "%.1f %s", weeks, unit)
            }
            days < 365.25 -> {
                val months = days / 30.4375
                val unit = if (compact) "mo" else "mos"
                String.format(Locale.US, "%.1f %s", months, unit)
            }
            else -> {
                val years = days / 365.25
                val unit = if (compact) "y" else "yrs"
                String.format(Locale.US, "%.1f %s", years, unit)
            }
        }
    }

    /**
     * Formats duration in minutes (e.g. from durationMinutes).
     * If duration is under 24 hours, shows standard "Xh Ym" or "Xh".
     * If duration is large (>= 24 hours), converts to days, weeks, months, or years.
     */
    fun formatMinutes(minutes: Int?, compact: Boolean = false): String {
        if (minutes == null || minutes <= 0) return if (compact) "0m" else "0 mins"
        val hours = minutes / 60.0
        if (hours < 24.0) {
            val h = minutes / 60
            val m = minutes % 60
            return if (h > 0 && m > 0) "${h}h ${m}m" else if (h > 0) "${h}h" else "${m}m"
        }
        return formatHours(hours, compact = compact)
    }
}
