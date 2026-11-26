package com.lx.tracklist.util

import com.lx.tracklist.data.model.MediaEntry
import java.util.Locale
import kotlin.math.roundToInt

object DurationFormatter {

    /**
     * Formats runtime in minutes using compact runtime style: "45m", "1h 30m", "2h" (never "2h 0m").
     */
    fun formatRuntime(minutes: Int?): String {
        val total = (minutes ?: 0).coerceAtLeast(0)
        val h = total / 60
        val m = total % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    /** Formats per-episode length consistently as "24 min/ep". */
    fun formatPerEpisode(minutes: Int): String = "${minutes.coerceAtLeast(0)} min/ep"

    /** Formats track length in seconds as "m:ss" (e.g. "3:05"). */
    fun formatTrack(seconds: Int?): String = MediaEntry.formatMusicDuration(seconds)

    private fun formatTrimmedOneDecimal(value: Double): String =
        String.format(Locale.US, "%.1f", value).removeSuffix(".0")

    private fun unitFor(formattedNumber: String, singular: String, plural: String): String =
        if (formattedNumber == "1") singular else plural

    /**
     * Formats duration in hours into a human-readable string:
     * - `<= 0` -> "0 min" (compact: "0h")
     * - `< 1h` -> minutes ("45 min" / compact "45m")
     * - `>= 1h` and `< 24h` -> hours ("12.5 hrs", "23 hrs" / compact "12h 30m")
     * - `>= 24h` and `< 7d` -> days ("1 day", "2.5 days")
     * - `>= 7d` and `< 30.4375d` -> weeks ("1 wk", "2.5 wks")
     * - `>= 30.4375d` and `< 365.25d` -> months ("1 mo", "3.5 mos")
     * - `>= 365.25d` -> years ("1 yr", "2.5 yrs")
     */
    fun formatHours(hours: Double, compact: Boolean = false): String {
        if (hours <= 0.0) return if (compact) "0h" else "0 min"
        val totalMinutes = (hours * 60.0).roundToInt()
        if (totalMinutes < 60) {
            return if (compact) "${totalMinutes}m" else "$totalMinutes min"
        }
        val effectiveHours = if (hours < 1.0) totalMinutes / 60.0 else hours
        val days = effectiveHours / 24.0
        return when {
            effectiveHours < 24.0 -> {
                if (compact) {
                    if (totalMinutes >= 24 * 60) {
                        "1d"
                    } else {
                        val h = totalMinutes / 60
                        val m = totalMinutes % 60
                        if (h > 0 && m > 0) "${h}h ${m}m" else "${h}h"
                    }
                } else {
                    val num = formatTrimmedOneDecimal(effectiveHours)
                    "$num ${unitFor(num, "hr", "hrs")}"
                }
            }
            days < 7.0 -> {
                val num = formatTrimmedOneDecimal(days)
                if (compact) "${num}d" else "$num ${unitFor(num, "day", "days")}"
            }
            days < 30.4375 -> {
                val weeks = days / 7.0
                val num = formatTrimmedOneDecimal(weeks)
                if (compact) "${num}w" else "$num ${unitFor(num, "wk", "wks")}"
            }
            days < 365.25 -> {
                val months = days / 30.4375
                val num = formatTrimmedOneDecimal(months)
                if (compact) "${num}mo" else "$num ${unitFor(num, "mo", "mos")}"
            }
            else -> {
                val years = days / 365.25
                val num = formatTrimmedOneDecimal(years)
                if (compact) "${num}y" else "$num ${unitFor(num, "yr", "yrs")}"
            }
        }
    }

    /**
     * Formats duration in minutes (e.g. from durationMinutes).
     * If duration is under 24 hours, shows standard "Xh Ym", "Xh", or "Ym".
     * If duration is large (>= 24 hours), converts to days, weeks, months, or years.
     */
    fun formatMinutes(minutes: Int?, compact: Boolean = false): String {
        if (minutes == null || minutes <= 0) return if (compact) "0m" else "0 min"
        val hours = minutes / 60.0
        if (hours < 24.0) {
            return formatRuntime(minutes)
        }
        return formatHours(hours, compact = compact)
    }
}
