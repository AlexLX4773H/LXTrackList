package com.lx.tracklist.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXScoreGold
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextDisabled
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import com.lx.tracklist.ui.theme.ThemeManager
import com.lx.tracklist.ui.theme.ThemePreset

@Composable
fun ThemesContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentPreset = ThemeManager.currentPreset
    val customBgUri = ThemeManager.customBackgroundUri
    val dimAlpha = ThemeManager.backgroundDimAlpha

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            ThemeManager.setCustomBackground(uri.toString(), context)
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Active Theme Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Active Theme: ${currentPreset.displayName}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = LXTextPrimary
                            )
                            val badgeText = if (customBgUri != null) "CUSTOM BG" else if (currentPreset.category == "advanced") "ADVANCED" else "PRESET"
                            Surface(
                                color = LXPrimaryGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LXPrimaryGreen
                                )
                            }
                        }
                        Text(
                            text = currentPreset.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = LXTextSecondary
                        )
                    }

                    OutlinedButton(
                        onClick = { ThemeManager.resetTheme(context) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LXTextSecondary),
                        border = BorderStroke(1.dp, LXBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 12.sp)
                    }
                }

                // Color Swatches of active theme
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColorSwatchPill(label = "Primary", color = ThemeManager.currentColors.primary)
                    ColorSwatchPill(label = "Surface", color = ThemeManager.currentColors.surface)
                    ColorSwatchPill(label = "Background", color = ThemeManager.currentColors.background)
                    ColorSwatchPill(label = "Accent", color = ThemeManager.currentColors.accentSecondary)
                    ColorSwatchPill(label = "Score", color = ThemeManager.currentColors.scoreGold)
                }
            }
        }

        // Section 1: Default Themes
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = LXPrimaryGreen, modifier = Modifier.size(20.dp))
                Text(
                    text = "Default Themes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )
            }
            Text(
                text = "Clean, high-contrast palettes tailored for reading and tracking.",
                style = MaterialTheme.typography.bodySmall,
                color = LXTextSecondary
            )

            val defaultPresets = ThemePreset.entries.filter { it.category == "default" }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                defaultPresets.forEach { preset ->
                    val isSelected = currentPreset.id == preset.id && customBgUri == null
                    ThemePresetCard(
                        preset = preset,
                        isSelected = isSelected,
                        onClick = {
                            ThemeManager.setTheme(preset.id, context)
                        }
                    )
                }
            }
        }

        // Section 2: Advanced Themes (Anime & Gaming Themed Looks)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LXScoreGold, modifier = Modifier.size(20.dp))
                Text(
                    text = "Advanced Themed Concepts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )
            }
            Text(
                text = "Custom atmospheric wallpapers, tailored button badges, and styled UI accents.",
                style = MaterialTheme.typography.bodySmall,
                color = LXTextSecondary
            )

            val advancedPresets = ThemePreset.entries.filter { it.category == "advanced" }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                advancedPresets.forEach { preset ->
                    val isSelected = currentPreset.id == preset.id && customBgUri == null
                    AdvancedThemeCard(
                        preset = preset,
                        isSelected = isSelected,
                        onClick = {
                            ThemeManager.setTheme(preset.id, context)
                        }
                    )
                }
            }
        }

        // Section 3: Custom Background Image
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = LXPrimaryGreen, modifier = Modifier.size(20.dp))
                Text(
                    text = "Custom Background Wallpaper",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )
            }
            Text(
                text = "Personalize your app with any image from your device storage.",
                style = MaterialTheme.typography.bodySmall,
                color = LXTextSecondary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                border = BorderStroke(1.dp, if (customBgUri != null) LXPrimaryGreen.copy(alpha = 0.5f) else LXBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (customBgUri != null) {
                        // Display active custom background preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, LXBorder, RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(customBgUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Custom Background Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Image Applied",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LXTextPrimary
                                )
                                Text(
                                    text = "Active behind all app screens and cards.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LXTextSecondary
                                )
                            }
                        }

                        // Dimming Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Background Scrim / Dim",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LXTextSecondary
                                )
                                Text(
                                    text = "${(dimAlpha * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LXPrimaryGreen
                                )
                            }
                            Slider(
                                value = dimAlpha,
                                onValueChange = { ThemeManager.setBackgroundDim(it, context) },
                                valueRange = 0.10f..0.90f,
                                colors = SliderDefaults.colors(
                                    thumbColor = LXPrimaryGreen,
                                    activeTrackColor = LXPrimaryGreen,
                                    inactiveTrackColor = LXSurfaceElevated
                                )
                            )
                            Text(
                                text = "Adjust darkness overlay to keep text and cover cards crystal-clear.",
                                style = MaterialTheme.typography.labelSmall,
                                color = LXTextDisabled
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, LXBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Image", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { ThemeManager.setCustomBackground(null, context) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.2f), contentColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remove", fontSize = 12.sp)
                            }
                        }
                    } else {
                        // No custom background active
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No Custom Image Active",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LXTextPrimary
                                )
                                Text(
                                    text = "Select any photo or wallpaper to render behind all app content.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LXTextSecondary
                                )
                            }

                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose Image", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ColorSwatchPill(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(LXSurfaceElevated)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, LXBorder, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LXTextSecondary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ThemePresetCard(
    preset: ThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) LXSurfaceElevated else LXSurfaceBlack
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) LXPrimaryGreen else LXBorder
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = preset.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                    if (isSelected) {
                        Surface(
                            color = LXPrimaryGreen,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = LXBackgroundBlack
                            )
                        }
                    }
                }
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Radio check icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) LXPrimaryGreen else Color.Transparent)
                    .border(1.5.dp, if (isSelected) LXPrimaryGreen else LXBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = LXBackgroundBlack,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvancedThemeCard(
    preset: ThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) LXPrimaryGreen else LXBorder
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Theme Preview Image Banner
            if (!preset.previewAssetPath.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(preset.previewAssetPath)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${preset.displayName} Theme Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Scrim gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )

                    // Tag badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.70f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LXScoreGold, modifier = Modifier.size(12.dp))
                            Text(
                                text = "CONCEPT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = Color.White
                            )
                        }
                    }

                    if (isSelected) {
                        Surface(
                            color = LXPrimaryGreen,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LXBackgroundBlack, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "ACTIVE THEME",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = LXBackgroundBlack
                                )
                            }
                        }
                    }
                }
            }

            // Info & Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = preset.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                    Text(
                        text = preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = LXTextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) LXPrimaryGreen else LXSurfaceElevated,
                        contentColor = if (isSelected) LXBackgroundBlack else LXTextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isSelected) "Applied" else "Apply",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
