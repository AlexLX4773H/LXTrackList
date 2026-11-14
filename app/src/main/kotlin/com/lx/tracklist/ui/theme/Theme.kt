package com.lx.tracklist.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun LXTrackListTheme(content: @Composable () -> Unit) {
    val currentColors = ThemeManager.currentColors
    val isLight = currentColors.isLight

    val colorScheme = if (isLight) {
        lightColorScheme(
            primary = currentColors.primary,
            onPrimary = Color.White,
            primaryContainer = currentColors.surfaceVariant,
            onPrimaryContainer = currentColors.textPrimary,
            secondary = currentColors.primaryLight,
            onSecondary = Color.White,
            background = currentColors.background,
            onBackground = currentColors.textPrimary,
            surface = currentColors.surface,
            onSurface = currentColors.textPrimary,
            surfaceVariant = currentColors.surfaceVariant,
            onSurfaceVariant = currentColors.textSecondary,
            outline = currentColors.border,
            outlineVariant = currentColors.surfaceElevated,
            error = currentColors.error,
            onError = Color.White
        )
    } else {
        darkColorScheme(
            primary = currentColors.primary,
            onPrimary = currentColors.background,
            primaryContainer = currentColors.surfaceVariant,
            onPrimaryContainer = currentColors.primary,
            secondary = currentColors.primaryLight,
            onSecondary = currentColors.background,
            background = currentColors.background,
            onBackground = currentColors.textPrimary,
            surface = currentColors.surface,
            onSurface = currentColors.textPrimary,
            surfaceVariant = currentColors.surfaceVariant,
            onSurfaceVariant = currentColors.textSecondary,
            outline = currentColors.border,
            outlineVariant = currentColors.surfaceElevated,
            error = currentColors.error,
            onError = currentColors.textPrimary
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = currentColors.background.toArgb()
                window.navigationBarColor = currentColors.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLight
            }
        }
    }

    CompositionLocalProvider(
        LocalLXColors provides currentColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Renders the global app background: handles custom local wallpaper image or advanced theme wallpaper
 * with adjustable scrim/dim overlay.
 */
@Composable
fun AppThemeBackgroundContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bgImage = ThemeManager.effectiveBackgroundImage
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize().background(ThemeManager.currentColors.background)) {
        if (!bgImage.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(bgImage)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Scrim overlay to maintain high readability across all cards and typography
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = ThemeManager.backgroundDimAlpha))
            )
        }
        content()
    }
}
