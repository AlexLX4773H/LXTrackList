package com.lx.tracklist.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LXDarkColorScheme = darkColorScheme(
    primary = LXPrimaryGreen,
    onPrimary = LXBackgroundBlack,
    primaryContainer = LXSurfaceVariant,
    onPrimaryContainer = LXPrimaryGreen,
    secondary = LXPrimaryGreenLight,
    onSecondary = LXBackgroundBlack,
    background = LXBackgroundBlack,
    onBackground = LXTextPrimary,
    surface = LXSurfaceBlack,
    onSurface = LXTextPrimary,
    surfaceVariant = LXSurfaceVariant,
    onSurfaceVariant = LXTextSecondary,
    outline = LXBorder,
    outlineVariant = LXSurfaceElevated,
    error = LXErrorRed,
    onError = LXTextPrimary
)

@Composable
fun LXTrackListTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = LXBackgroundBlack.toArgb()
                window.navigationBarColor = LXBackgroundBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = LXDarkColorScheme,
        typography = Typography,
        content = content
    )
}
