package com.lx.tracklist.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lx.tracklist.R
import com.lx.tracklist.ui.theme.ThemeManager

@Composable
fun ThemedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Add Entry"
) {
    val colors = ThemeManager.currentColors
    when (colors.fabBadgeShape) {
        "magic_orb" -> {
            // Beyond theme: glowing crimson magic orb with slowly rotating arcane rune ring
            val infiniteTransition = rememberInfiniteTransition(label = "RuneRotation")
            val runeRotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 24000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "RuneAngle"
            )

            Box(
                modifier = modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                // Rotating outer arcane rune ring
                Image(
                    painter = painterResource(R.drawable.theme_beyond_fab_bg_rune),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = runeRotation)
                )
                // Glowing crimson inner button orb with '+'
                Image(
                    painter = painterResource(R.drawable.theme_beyond_fab_inner_button),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        "slime" -> {
            // Slime theme: soft rounded cyan slime bubble
            Box(
                modifier = modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF38BDF8))
                    .border(2.dp, Color(0xFFFDE047), RoundedCornerShape(22.dp))
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = contentDescription,
                    tint = Color(0xFF0369A1),
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        "shield" -> {
            // Gate theme: crusader shield / armored crest
            Box(
                modifier = modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(Color(0xFF991B1B))
                    .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp))
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = contentDescription,
                    tint = Color(0xFFFDE047),
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        "reticle" -> {
            // Sky theme: cyber holographic HUD reticle
            Box(
                modifier = modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0891B2).copy(alpha = 0.25f))
                    .border(2.dp, Color(0xFF06B6D4), CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0891B2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = contentDescription,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
        else -> {
            FloatingActionButton(
                onClick = onClick,
                containerColor = colors.fabContainerColor,
                contentColor = colors.fabContentColor,
                shape = CircleShape,
                modifier = modifier
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
