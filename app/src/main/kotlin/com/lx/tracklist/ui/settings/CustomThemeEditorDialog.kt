package com.lx.tracklist.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lx.tracklist.ui.theme.CustomThemeData
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextDisabled
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import com.lx.tracklist.ui.theme.parseThemeHexColor

enum class CustomColorSlot(val label: String, val subtitle: String) {
    PRIMARY("Primary Accent", "Buttons, active indicators, highlights"),
    BACKGROUND("Background", "Main app screen background"),
    SURFACE("Surface", "Top app bar, bottom navigation bar"),
    CARD("Card & Container", "Media cards, dialogs, elevated surfaces"),
    BORDER("Border Outline", "Card borders, dividers, outlines"),
    ACCENT("Secondary Accent", "Chips, tags, active filters"),
    TEXT_PRIMARY("Primary Text", "Main headlines and item titles"),
    TEXT_SECONDARY("Secondary Text", "Subtitles, details, captions"),
    SCORE("Score / Star", "Ratings, stars, score badges")
}

@Composable
fun CustomThemeEditorDialog(
    themeToEdit: CustomThemeData?,
    onDismiss: () -> Unit,
    onSave: (CustomThemeData) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(themeToEdit?.name ?: "My Custom Theme") }
    var isLight by remember { mutableStateOf(themeToEdit?.isLight ?: false) }
    var primaryHex by remember { mutableStateOf(themeToEdit?.primaryHex ?: "#A7EF60") }
    var backgroundHex by remember { mutableStateOf(themeToEdit?.backgroundHex ?: "#000000") }
    var surfaceHex by remember { mutableStateOf(themeToEdit?.surfaceHex ?: "#121212") }
    var surfaceElevatedHex by remember { mutableStateOf(themeToEdit?.surfaceElevatedHex ?: "#252525") }
    var borderHex by remember { mutableStateOf(themeToEdit?.borderHex ?: "#2C2C2C") }
    var accentSecondaryHex by remember { mutableStateOf(themeToEdit?.accentSecondaryHex ?: "#C4FF8E") }
    var textPrimaryHex by remember { mutableStateOf(themeToEdit?.textPrimaryHex ?: "#FFFFFF") }
    var textSecondaryHex by remember { mutableStateOf(themeToEdit?.textSecondaryHex ?: "#B0B0B0") }
    var scoreHex by remember { mutableStateOf(themeToEdit?.scoreHex ?: "#FFD166") }

    var backgroundUri by remember { mutableStateOf(themeToEdit?.backgroundUri) }
    var backgroundDimAlpha by remember { mutableFloatStateOf(themeToEdit?.backgroundDimAlpha ?: 0.60f) }

    var selectedSlot by remember { mutableStateOf(CustomColorSlot.PRIMARY) }

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
            backgroundUri = uri.toString()
        }
    }

    // Resolvers for live colors
    val curPrimary = parseThemeHexColor(primaryHex, Color(0xFFA7EF60))
    val curBg = parseThemeHexColor(backgroundHex, Color(0xFF000000))
    val curSurface = parseThemeHexColor(surfaceHex, Color(0xFF121212))
    val curCard = parseThemeHexColor(surfaceElevatedHex, Color(0xFF252525))
    val curBorder = parseThemeHexColor(borderHex, Color(0xFF2C2C2C))
    val curAccent = parseThemeHexColor(accentSecondaryHex, Color(0xFFC4FF8E))
    val curTextP = parseThemeHexColor(textPrimaryHex, Color(0xFFFFFFFF))
    val curTextS = parseThemeHexColor(textSecondaryHex, Color(0xFFB0B0B0))
    val curScore = parseThemeHexColor(scoreHex, Color(0xFFFFD166))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(16.dp),
            color = LXSurfaceBlack,
            border = BorderStroke(1.dp, LXBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = LXPrimaryGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (themeToEdit != null) "Edit Custom Theme" else "Create Custom Theme",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = LXTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Theme Name Input
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Theme Name") },
                        placeholder = { Text("e.g. Cyber Neon, Deep Slate, Ruby Night") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LXPrimaryGreen,
                            unfocusedBorderColor = LXBorder,
                            focusedTextColor = LXTextPrimary,
                            unfocusedTextColor = LXTextPrimary,
                            focusedContainerColor = LXSurfaceElevated,
                            unfocusedContainerColor = LXSurfaceElevated
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Starter Templates
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Start from Template",
                            style = MaterialTheme.typography.labelSmall,
                            color = LXTextSecondary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TemplateChip(label = "Default Dark") {
                                primaryHex = "#A7EF60"
                                backgroundHex = "#000000"
                                surfaceHex = "#121212"
                                surfaceElevatedHex = "#252525"
                                borderHex = "#2C2C2C"
                                accentSecondaryHex = "#C4FF8E"
                                textPrimaryHex = "#FFFFFF"
                                textSecondaryHex = "#B0B0B0"
                                scoreHex = "#FFD166"
                                isLight = false
                            }
                            TemplateChip(label = "Clean Light") {
                                primaryHex = "#10B981"
                                backgroundHex = "#F8FAFC"
                                surfaceHex = "#FFFFFF"
                                surfaceElevatedHex = "#E2E8F0"
                                borderHex = "#CBD5E1"
                                accentSecondaryHex = "#3B82F6"
                                textPrimaryHex = "#0F172A"
                                textSecondaryHex = "#64748B"
                                scoreHex = "#D97706"
                                isLight = true
                            }
                            TemplateChip(label = "Crimson Dark") {
                                primaryHex = "#EF4444"
                                backgroundHex = "#0B0B0E"
                                surfaceHex = "#18181F"
                                surfaceElevatedHex = "#2F2E3E"
                                borderHex = "#3B282F"
                                accentSecondaryHex = "#F87171"
                                textPrimaryHex = "#FFFFFF"
                                textSecondaryHex = "#D1A5A5"
                                scoreHex = "#FBBF24"
                                isLight = false
                            }
                            TemplateChip(label = "Slate Blue") {
                                primaryHex = "#38BDF8"
                                backgroundHex = "#0B111E"
                                surfaceHex = "#151E2E"
                                surfaceElevatedHex = "#283548"
                                borderHex = "#2E3C52"
                                accentSecondaryHex = "#818CF8"
                                textPrimaryHex = "#F1F5F9"
                                textSecondaryHex = "#94A3B8"
                                scoreHex = "#FBBF24"
                                isLight = false
                            }
                            TemplateChip(label = "Midnight Gold") {
                                primaryHex = "#F59E0B"
                                backgroundHex = "#0E0E12"
                                surfaceHex = "#1C1C24"
                                surfaceElevatedHex = "#2C2C38"
                                borderHex = "#4A3B22"
                                accentSecondaryHex = "#FBBF24"
                                textPrimaryHex = "#FAF5FF"
                                textSecondaryHex = "#D4D4D8"
                                scoreHex = "#F59E0B"
                                isLight = false
                            }
                        }
                    }

                    // Live Theme Mini Preview Card
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Live Preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = LXTextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(curBg)
                                .border(1.dp, curBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            if (!backgroundUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(backgroundUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Color.Black.copy(alpha = backgroundDimAlpha))
                                )
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = curCard),
                                border = BorderStroke(1.dp, curBorder),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (name.isNotBlank()) name else "Theme Preview",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = curTextP
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = curScore,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "9.4",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = curScore
                                            )
                                        }
                                    }

                                    Text(
                                        text = "High-contrast reading, smooth cards & personalized accents.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = curTextS
                                    )

                                    // Mini Progress bar preview showcasing primary accent
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(curPrimary.copy(alpha = 0.20f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(0.68f)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(curPrimary)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = curAccent.copy(alpha = 0.20f),
                                            border = BorderStroke(0.5.dp, curAccent),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "COMPLETED",
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                ),
                                                color = curAccent
                                            )
                                        }

                                        val actionTextColor = if ((0.299f * curPrimary.red + 0.587f * curPrimary.green + 0.114f * curPrimary.blue) > 0.5f) {
                                            Color(0xFF101410)
                                        } else {
                                            Color.White
                                        }

                                        Surface(
                                            color = curPrimary,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = actionTextColor
                                                )
                                                Text(
                                                    text = "Action",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.5.sp
                                                    ),
                                                    color = actionTextColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Theme Base Mode Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(LXSurfaceElevated)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isLight) "Clean Light Mode" else "Dark Canvas Mode",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = LXTextPrimary
                            )
                            Text(
                                text = if (isLight) "Light surfaces with dark text" else "Deep dark surfaces with luminous text",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextSecondary
                            )
                        }
                        Switch(
                            checked = isLight,
                            onCheckedChange = { isLight = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LXPrimaryGreen,
                                checkedTrackColor = LXPrimaryGreen.copy(alpha = 0.35f),
                                uncheckedThumbColor = LXTextSecondary,
                                uncheckedTrackColor = LXBorder
                            )
                        )
                    }

                    // Color Slot Selector Horizontal Carousel
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Select Color Option to Customize",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CustomColorSlot.entries.forEach { slot ->
                                val slotColorHex = when (slot) {
                                    CustomColorSlot.PRIMARY -> primaryHex
                                    CustomColorSlot.BACKGROUND -> backgroundHex
                                    CustomColorSlot.SURFACE -> surfaceHex
                                    CustomColorSlot.CARD -> surfaceElevatedHex
                                    CustomColorSlot.BORDER -> borderHex
                                    CustomColorSlot.ACCENT -> accentSecondaryHex
                                    CustomColorSlot.TEXT_PRIMARY -> textPrimaryHex
                                    CustomColorSlot.TEXT_SECONDARY -> textSecondaryHex
                                    CustomColorSlot.SCORE -> scoreHex
                                }
                                val isSelected = selectedSlot == slot
                                val slotColor = parseThemeHexColor(slotColorHex, Color.White)

                                Surface(
                                    color = if (isSelected) LXPrimaryGreen.copy(alpha = 0.18f) else LXSurfaceElevated,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) LXPrimaryGreen else LXBorder
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { selectedSlot = slot }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(slotColor)
                                                .border(1.dp, LXBorder, CircleShape)
                                        )
                                        Text(
                                            text = slot.label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Active Color Picker (Wheel + Slider + Hex)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LXSurfaceElevated),
                        border = BorderStroke(1.dp, LXBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${selectedSlot.label}: ${selectedSlot.subtitle}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextSecondary
                            )

                            val activeHex = when (selectedSlot) {
                                CustomColorSlot.PRIMARY -> primaryHex
                                CustomColorSlot.BACKGROUND -> backgroundHex
                                CustomColorSlot.SURFACE -> surfaceHex
                                CustomColorSlot.CARD -> surfaceElevatedHex
                                CustomColorSlot.BORDER -> borderHex
                                CustomColorSlot.ACCENT -> accentSecondaryHex
                                CustomColorSlot.TEXT_PRIMARY -> textPrimaryHex
                                CustomColorSlot.TEXT_SECONDARY -> textSecondaryHex
                                CustomColorSlot.SCORE -> scoreHex
                            }

                            ColorWheelPicker(
                                selectedHex = activeHex,
                                onColorChanged = { newHex ->
                                    when (selectedSlot) {
                                        CustomColorSlot.PRIMARY -> primaryHex = newHex
                                        CustomColorSlot.BACKGROUND -> backgroundHex = newHex
                                        CustomColorSlot.SURFACE -> surfaceHex = newHex
                                        CustomColorSlot.CARD -> surfaceElevatedHex = newHex
                                        CustomColorSlot.BORDER -> borderHex = newHex
                                        CustomColorSlot.ACCENT -> accentSecondaryHex = newHex
                                        CustomColorSlot.TEXT_PRIMARY -> textPrimaryHex = newHex
                                        CustomColorSlot.TEXT_SECONDARY -> textSecondaryHex = newHex
                                        CustomColorSlot.SCORE -> scoreHex = newHex
                                    }
                                }
                            )
                        }
                    }

                    // Custom Background Wallpaper (Saved specifically with this theme)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LXSurfaceElevated),
                        border = BorderStroke(1.dp, if (backgroundUri != null) LXPrimaryGreen.copy(alpha = 0.4f) else LXBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = LXPrimaryGreen, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Theme Wallpaper (Saved with Theme)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LXTextPrimary
                                )
                            }
                            Text(
                                text = "This wallpaper is bundled exclusively with this custom theme, separate from your manual background wallpaper.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextSecondary
                            )

                            if (backgroundUri != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, LXBorder, RoundedCornerShape(8.dp))
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(backgroundUri)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Theme Wallpaper",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Wallpaper Attached",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = LXTextPrimary
                                        )
                                        Text(
                                            text = "Loads automatically whenever this theme is applied.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LXTextSecondary
                                        )
                                    }
                                }

                                // Wallpaper Dim Slider
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Wallpaper Dim Overlay",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LXTextSecondary
                                        )
                                        Text(
                                            text = "${(backgroundDimAlpha * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = LXPrimaryGreen
                                        )
                                    }
                                    Slider(
                                        value = backgroundDimAlpha,
                                        onValueChange = { backgroundDimAlpha = it.coerceIn(0.10f, 0.90f) },
                                        valueRange = 0.10f..0.90f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = LXPrimaryGreen,
                                            activeTrackColor = LXPrimaryGreen,
                                            inactiveTrackColor = LXBackgroundBlack
                                        )
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
                                        Text("Change", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { backgroundUri = null },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                            contentColor = Color(0xFFEF4444)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Remove", fontSize = 12.sp)
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, LXBorder),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Wallpaper for this Theme", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, LXBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = LXTextSecondary)
                    }

                    Button(
                        onClick = {
                            val finalName = if (name.isNotBlank()) name.trim() else "My Theme"
                            val result = CustomThemeData(
                                id = themeToEdit?.id ?: ("custom_" + System.currentTimeMillis()),
                                name = finalName,
                                primaryHex = primaryHex.uppercase(),
                                backgroundHex = backgroundHex.uppercase(),
                                surfaceHex = surfaceHex.uppercase(),
                                surfaceElevatedHex = surfaceElevatedHex.uppercase(),
                                borderHex = borderHex.uppercase(),
                                accentSecondaryHex = accentSecondaryHex.uppercase(),
                                textPrimaryHex = textPrimaryHex.uppercase(),
                                textSecondaryHex = textSecondaryHex.uppercase(),
                                scoreHex = scoreHex.uppercase(),
                                isLight = isLight,
                                backgroundUri = backgroundUri,
                                backgroundDimAlpha = backgroundDimAlpha,
                                createdAt = themeToEdit?.createdAt ?: System.currentTimeMillis()
                            )
                            onSave(result)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LXPrimaryGreen,
                            contentColor = LXBackgroundBlack
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Theme", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateChip(label: String, onClick: () -> Unit) {
    Surface(
        color = LXSurfaceElevated,
        border = BorderStroke(1.dp, LXBorder),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = LXTextPrimary
        )
    }
}
