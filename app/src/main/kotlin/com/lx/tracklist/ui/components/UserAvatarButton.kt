package com.lx.tracklist.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lx.tracklist.R
import com.lx.tracklist.data.model.UserProfile
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.ThemeManager

fun parseUserAvatarHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        LXPrimaryGreen
    }
}

@Composable
fun UserAvatarButton(
    activeUser: UserProfile?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    paddingStart: Dp = 12.dp,
    paddingEnd: Dp = 4.dp
) {
    val borderColor = if (activeUser != null) parseUserAvatarHexColor(activeUser.avatarColorHex) else LXPrimaryGreen
    val badgeSize = (size * 0.28f).coerceAtLeast(8.dp)

    Box(
        modifier = modifier
            .padding(start = paddingStart, end = paddingEnd)
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!activeUser?.avatarImageUri.isNullOrBlank()) {
            AsyncImage(
                model = activeUser.avatarImageUri,
                contentDescription = "Switch User (${activeUser.name})",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(1.5.dp, borderColor, CircleShape)
            )
        } else {
            val logoPainter = if (ThemeManager.isSlimeTheme) {
                painterResource(R.drawable.theme_slime_logo_slime)
            } else if (ThemeManager.isGateTheme) {
                painterResource(R.drawable.theme_gate_logo_lx)
            } else if (ThemeManager.isSkyTheme) {
                painterResource(R.drawable.theme_sky_logo_lx)
            } else {
                painterResource(R.drawable.lx_logo)
            }
            Image(
                painter = logoPainter,
                contentDescription = "Switch User (${activeUser?.name ?: "User"})",
                modifier = Modifier.fillMaxSize()
            )
            if (activeUser != null) {
                Box(
                    modifier = Modifier
                        .size(badgeSize)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(borderColor)
                        .border(1.5.dp, LXBackgroundBlack, CircleShape)
                )
            }
        }
    }
}

