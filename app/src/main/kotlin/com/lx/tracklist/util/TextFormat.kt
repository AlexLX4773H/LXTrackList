package com.lx.tracklist.util

import java.text.NumberFormat
import java.util.Locale

/** Formats a count with singular [one] when [count] == 1, otherwise [many]. */
fun plural(count: Int, one: String, many: String = "${one}s"): String =
    "$count ${if (count == 1) one else many}"

/** Formats an integer using locale-aware grouping separators. */
fun formatCount(n: Int, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getIntegerInstance(locale).format(n)

/**
 * Formats included/excluded filter counts consistently as "+N / −M", "+N", "−M", or "".
 */
fun formatIncludeExclude(included: Int, excluded: Int): String = when {
    included > 0 && excluded > 0 -> "+$included / \u2212$excluded"
    included > 0 -> "+$included"
    excluded > 0 -> "\u2212$excluded"
    else -> ""
}
