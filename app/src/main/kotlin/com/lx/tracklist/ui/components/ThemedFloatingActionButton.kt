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
            // Slime theme: cute cosmic slime orb with gold '+', rings, and bubbles
            val infiniteTransition = rememberInfiniteTransition(label = "SlimeBobbing")
            val bobbingOffset by infiniteTransition.animateFloat(
                initialValue = -2.5f,
                targetValue = 2.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "SlimeOffset"
            )

            Box(
                modifier = modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.theme_slime_fab_add_full),
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(translationY = bobbingOffset)
                )
            }
        }
        "shield" -> {
            // Gate theme: crusader shield / armored crest with gold wings and central cross
            val infiniteTransition = rememberInfiniteTransition(label = "GateFabPulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.98f,
                targetValue = 1.02f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2400, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "GateScale"
            )

            Box(
                modifier = modifier
                    .size(width = 68.dp, height = 62.dp)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.theme_gate_fab_add_full),
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                )
            }
        }
        "reticle" -> {
            // Sky theme: cyber holographic HUD reticle
            val infiniteTransition = rememberInfiniteTransition(label = "SkyReticlePulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.98f,
                targetValue = 1.02f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2400, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "SkyScale"
            )

            Box(
                modifier = modifier
                    .size(68.dp)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.theme_sky_fab_add_full),
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                )
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
