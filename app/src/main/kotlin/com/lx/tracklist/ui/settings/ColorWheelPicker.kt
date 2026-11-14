package com.lx.tracklist.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextDisabled
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import com.lx.tracklist.ui.theme.parseThemeHexColor
import com.lx.tracklist.ui.theme.toHexRgb
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Interactive color picker combining:
 * 1. An HSV Color Wheel for intuitive 360° chromaticity & saturation selection.
 * 2. A Brightness / Value slider for shades ranging from pitch black to vibrant to white.
 * 3. An RGB Hex text field for direct hexadecimal typing (#RRGGBB).
 * 4. Quick-select palette chips for immediate one-tap popular colors.
 */
@Composable
fun ColorWheelPicker(
    selectedHex: String,
    onColorChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null
) {
    val initialColor = remember(selectedHex) { parseThemeHexColor(selectedHex, Color(0xFFA7EF60)) }
    val initialHsv = remember(selectedHex) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        hsv
    }

    var hue by remember(selectedHex) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(selectedHex) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember(selectedHex) { mutableFloatStateOf(initialHsv[2]) }
    var hexInput by remember(selectedHex) { mutableStateOf(selectedHex.removePrefix("#").uppercase()) }
    val focusManager = LocalFocusManager.current

    // Keep hexInput synchronized if selectedHex changes externally
    LaunchedEffect(selectedHex) {
        val clean = selectedHex.removePrefix("#").uppercase()
        if (clean != hexInput) {
            hexInput = clean
        }
    }

    fun notifyColorUpdate(h: Float, s: Float, v: Float) {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
        val newColor = Color(argb)
        val newHex = newColor.toHexRgb().removePrefix("#")
        hexInput = newHex
        onColorChanged("#$newHex")
    }

    val currentColor = remember(hue, saturation, brightness) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!title.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )
                Text(
                    text = "#$hexInput",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = LXPrimaryGreen
                )
            }
        }

        // Color Wheel Canvas
        Box(
            modifier = Modifier
                .size(170.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(170.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            val dist = sqrt(dx * dx + dy * dy)
                            val radius = size.width / 2f
                            val sat = (dist / radius).coerceIn(0f, 1f)
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0f) angle += 360f
                            hue = angle
                            saturation = sat
                            notifyColorUpdate(hue, saturation, brightness)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val dx = change.position.x - centerX
                            val dy = change.position.y - centerY
                            val dist = sqrt(dx * dx + dy * dy)
                            val radius = size.width / 2f
                            val sat = (dist / radius).coerceIn(0f, 1f)
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0f) angle += 360f
                            hue = angle
                            saturation = sat
                            notifyColorUpdate(hue, saturation, brightness)
                        }
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // 1. Rainbow sweep gradient for 360-degree hues
                val sweepBrush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFF0000), // Red
                        Color(0xFFFFFF00), // Yellow
                        Color(0xFF00FF00), // Green
                        Color(0xFF00FFFF), // Cyan
                        Color(0xFF0000FF), // Blue
                        Color(0xFFFF00FF), // Magenta
                        Color(0xFFFF0000)  // Red
                    ),
                    center = center
                )
                drawCircle(brush = sweepBrush, radius = radius, center = center)

                // 2. Radial gradient from White (saturation = 0) to Transparent (saturation = 1)
                val radialBrush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = center,
                    radius = radius
                )
                drawCircle(brush = radialBrush, radius = radius, center = center)

                // 3. Thumb indicator on the color wheel
                val angleRad = Math.toRadians(hue.toDouble())
                val thumbDistance = saturation * radius
                val thumbX = center.x + (thumbDistance * cos(angleRad)).toFloat()
                val thumbY = center.y + (thumbDistance * sin(angleRad)).toFloat()
                val thumbCenter = Offset(thumbX, thumbY)

                val fullBrightColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))

                // Contrast shadow ring
                drawCircle(color = Color.Black.copy(alpha = 0.55f), radius = 10.dp.toPx(), center = thumbCenter)
                // Crisp white ring
                drawCircle(color = Color.White, radius = 8.5.dp.toPx(), center = thumbCenter)
                // Thumb center filled with selected color
                drawCircle(color = fullBrightColor, radius = 5.5.dp.toPx(), center = thumbCenter)
            }
        }

        // Brightness / Value Slider
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Brightness / Shade",
                    style = MaterialTheme.typography.labelSmall,
                    color = LXTextSecondary
                )
                Text(
                    text = "${(brightness * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )
            }
            Slider(
                value = brightness,
                onValueChange = {
                    brightness = it
                    notifyColorUpdate(hue, saturation, brightness)
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = currentColor,
                    activeTrackColor = currentColor,
                    inactiveTrackColor = LXSurfaceElevated
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // RGB Hex Input Row + Swatch Preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live Color Swatch
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(currentColor)
                    .border(1.5.dp, LXBorder, RoundedCornerShape(8.dp))
            )

            // Hex Code TextField
            OutlinedTextField(
                value = hexInput,
                onValueChange = { input ->
                    val filtered = input.filter { it in "0123456789ABCDEFabcdef" }.take(6).uppercase()
                    hexInput = filtered
                    if (filtered.length == 6) {
                        try {
                            val parsed = android.graphics.Color.parseColor("#$filtered")
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(parsed, hsv)
                            hue = hsv[0]
                            saturation = hsv[1]
                            brightness = hsv[2]
                            onColorChanged("#$filtered")
                        } catch (_: Exception) {}
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text("RGB Hex Code", fontSize = 11.sp) },
                prefix = {
                    Text(
                        text = "#",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = LXPrimaryGreen
                    )
                },
                placeholder = { Text("A7EF60") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LXPrimaryGreen,
                    unfocusedBorderColor = LXBorder,
                    focusedTextColor = LXTextPrimary,
                    unfocusedTextColor = LXTextPrimary,
                    focusedContainerColor = LXBackgroundBlack,
                    unfocusedContainerColor = LXBackgroundBlack
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }

        // Quick Swatches Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.labelSmall,
                color = LXTextDisabled
            )
            val quickColors = listOf(
                "#000000" to "Black",
                "#121212" to "Dark",
                "#1E1E1E" to "Charcoal",
                "#252525" to "Elevated",
                "#FFFFFF" to "White",
                "#A7EF60" to "LX Lime",
                "#10B981" to "Emerald",
                "#38BDF8" to "Sky Blue",
                "#3B82F6" to "Royal Blue",
                "#8B5CF6" to "Violet",
                "#EF4444" to "Crimson",
                "#F59E0B" to "Amber",
                "#FFD166" to "Gold",
                "#F43F5E" to "Rose",
                "#64748B" to "Slate"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickColors.forEach { (hex, _) ->
                    val color = parseThemeHexColor(hex, Color.Black)
                    val isSelected = hexInput.equals(hex.removePrefix("#"), ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) LXPrimaryGreen else LXBorder,
                                shape = CircleShape
                            )
                            .clickable {
                                hexInput = hex.removePrefix("#").uppercase()
                                val parsed = android.graphics.Color.parseColor(hex)
                                val hsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(parsed, hsv)
                                hue = hsv[0]
                                saturation = hsv[1]
                                brightness = hsv[2]
                                onColorChanged(hex.uppercase())
                            }
                    )
                }
            }
        }
    }
}
