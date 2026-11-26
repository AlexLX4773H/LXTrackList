package com.lx.tracklist.ui.theme

import androidx.compose.ui.graphics.Color

val LXPrimaryGreen: Color get() = ThemeManager.currentColors.primary
val LXPrimaryGreenDark: Color get() = ThemeManager.currentColors.primaryDark
val LXPrimaryGreenLight: Color get() = ThemeManager.currentColors.primaryLight

val LXBackgroundBlack: Color get() = ThemeManager.currentColors.background
val LXSurfaceBlack: Color get() = ThemeManager.currentColors.surface
val LXSurfaceVariant: Color get() = ThemeManager.currentColors.surfaceVariant
val LXSurfaceElevated: Color get() = ThemeManager.currentColors.surfaceElevated

val LXTextPrimary: Color get() = ThemeManager.currentColors.textPrimary
val LXTextSecondary: Color get() = ThemeManager.currentColors.textSecondary
val LXTextDisabled: Color get() = ThemeManager.currentColors.textDisabled

val LXBorder: Color get() = ThemeManager.currentColors.border
val LXProgressTrack: Color get() = ThemeManager.currentColors.progressTrack
val LXErrorRed: Color get() = ThemeManager.currentColors.error

val LXScoreGold: Color get() = ThemeManager.currentColors.scoreGold
val LXAiringBlue: Color get() = ThemeManager.currentColors.airingBlue
val LXSuccessGreen: Color get() = ThemeManager.currentColors.success
val LXWarningAmber: Color get() = ThemeManager.currentColors.warning
val LXInfoBlue: Color get() = ThemeManager.currentColors.info
val LXOnAccent: Color get() = ThemeManager.currentColors.onAccent
val LXAccent: Color get() = ThemeManager.currentColors.accent

/**
 * Parses a 6- or 8-digit `#RRGGBB` / `#AARRGGBB` hex string into a Compose [Color],
 * returning [fallback] if parsing fails.
 */
fun parseHexColor(hex: String, fallback: Color = LXPrimaryGreen): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        fallback
    }
}
