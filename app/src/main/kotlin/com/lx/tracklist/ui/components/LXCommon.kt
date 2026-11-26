package com.lx.tracklist.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lx.tracklist.ui.theme.LXTrackListTheme
import com.lx.tracklist.ui.theme.LocalLXColors
import com.lx.tracklist.ui.theme.badge
import com.lx.tracklist.ui.theme.caption
import com.lx.tracklist.ui.theme.chip
import com.lx.tracklist.util.ExternalLinkHelper

/**
 * Compact status/state badge with 15% tinted background, 4dp corners, and 10sp bold text.
 */
@Composable
fun LXBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.badge,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Removable filter or tag pill with 8dp corners and 12sp chip typography.
 * When [onRemove] is provided, the close icon renders at 14dp inside a 48dp touch target.
 */
@Composable
fun LXPill(
    text: String,
    selected: Boolean = true,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    accentColor: Color? = null
) {
    val colors = LocalLXColors.current
    val pillColor = accentColor ?: colors.primary
    val shape = RoundedCornerShape(8.dp)
    val bgColor = if (selected) pillColor.copy(alpha = 0.15f) else colors.surfaceVariant
    val borderColor = if (selected) pillColor.copy(alpha = 0.35f) else colors.border
    val contentColor = if (selected) pillColor else colors.textSecondary

    val pillContent: @Composable (Modifier) -> Unit = { pillModifier ->
        Row(
            modifier = pillModifier
                .clip(shape)
                .background(bgColor)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.chip,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (onRemove != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove $text",
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }

    if (onRemove != null) {
        Box(
            modifier = modifier
                .minimumInteractiveComponentSize()
                .clip(shape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            pillContent(Modifier)
        }
    } else {
        pillContent(modifier)
    }
}

/**
 * Standard selectable chip wrapping Material 3 [FilterChip] with consistent height, shape, and typography.
 */
@Composable
fun LXChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    dense: Boolean = false
) {
    val colors = LocalLXColors.current
    val chipHeight = if (dense) 28.dp else 32.dp
    val labelStyle = if (dense) MaterialTheme.typography.labelMedium else MaterialTheme.typography.chip
    val shape = RoundedCornerShape(8.dp)

    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.height(chipHeight),
        shape = shape,
        leadingIcon = leadingIcon,
        label = {
            Text(
                text = label,
                style = labelStyle,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surfaceVariant,
            labelColor = colors.textSecondary,
            iconColor = colors.textSecondary,
            selectedContainerColor = colors.primary.copy(alpha = 0.16f),
            selectedLabelColor = colors.primary,
            selectedLeadingIconColor = colors.primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.border,
            selectedBorderColor = colors.primary.copy(alpha = 0.5f),
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp
        )
    )
}

/**
 * Shared confirmation dialog using 16dp corners, elevated surface, scrollable message,
 * Cancel text button on the left, and primary/destructive action button on the right.
 */
@Composable
fun LXConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    val colors = LocalLXColors.current
    val confirmContainer = if (destructive) colors.error else colors.primary
    val confirmContent = if (destructive) Color.White else colors.onAccent

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = colors.surfaceElevated,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmContainer,
                    contentColor = confirmContent
                )
            ) {
                Text(
                    text = confirmText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    )
}

/**
 * Shared "Open" dropdown menu rendering each provider with an 8dp brand-color dot,
 * primary-colored title, and secondary-colored subtitle for high contrast across all themes.
 */
@Composable
fun LXOpenMenu(
    options: List<ExternalLinkHelper.ExternalOpenOption>,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpen: (ExternalLinkHelper.ExternalOpenOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalLXColors.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier.background(colors.surfaceElevated)
    ) {
        options.forEach { option ->
            DropdownMenuItem(
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(option.color))
                            .border(0.5.dp, colors.textSecondary.copy(alpha = 0.45f), CircleShape)
                    )
                },
                text = {
                    Column {
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (option.subtitle.isNotBlank()) {
                            Text(
                                text = option.subtitle,
                                style = MaterialTheme.typography.caption,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                onClick = {
                    onDismiss()
                    onOpen(option)
                }
            )
        }
    }
}

/**
 * Compact 44dp search input with 10dp corners, matching placeholder/input typography, and primary cursor.
 */
@Composable
fun LXSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null
) {
    val colors = LocalLXColors.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val shape = RoundedCornerShape(10.dp)
    val textStyle = MaterialTheme.typography.bodyMedium

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(colors.surfaceVariant)
            .border(1.dp, colors.border, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = textStyle,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = textStyle.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        onSearch?.invoke()
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear search",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Shared themed card reading corner radius, border width, and background color from the active theme.
 */
@Composable
fun LXCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalLXColors.current
    val shape = RoundedCornerShape(colors.cardCornerRadius)
    val border = if (colors.cardBorderWidth > 0.dp) {
        BorderStroke(colors.cardBorderWidth, colors.border)
    } else {
        null
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = colors.cardBackgroundColor,
            contentColor = colors.textPrimary,
            border = border
        ) {
            Column(content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = colors.cardBackgroundColor,
            contentColor = colors.textPrimary,
            border = border
        ) {
            Column(content = content)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LXCommonPreview() {
    LXTrackListTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LXBadge(text = "AIRING", color = LocalLXColors.current.airingBlue)
                LXBadge(text = "NYA", color = LocalLXColors.current.scoreGold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LXPill(text = "Fantasy", selected = true, onRemove = {})
                LXPill(text = "Sequel", selected = false)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LXChip(
                    selected = true,
                    label = "Anime",
                    onClick = {},
                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                LXChip(selected = false, label = "Dense", onClick = {}, dense = true)
            }
            LXSearchField(value = "", onValueChange = {}, placeholder = "Search library...")
            LXCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Themed Card Preview",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LXConfirmDialogPreview() {
    LXTrackListTheme {
        LXConfirmDialog(
            title = "Delete Entry?",
            message = "This removes the entry from your library.",
            confirmText = "Delete",
            onConfirm = {},
            onDismiss = {},
            destructive = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LXOpenMenuPreview() {
    LXTrackListTheme {
        LXOpenMenu(
            options = listOf(
                ExternalLinkHelper.ExternalOpenOption(
                    title = "MyAnimeList",
                    subtitle = "myanimelist.net",
                    url = "https://myanimelist.net",
                    color = 0xFF2E51A2
                ),
                ExternalLinkHelper.ExternalOpenOption(
                    title = "Steam",
                    subtitle = "store.steampowered.com",
                    url = "https://store.steampowered.com",
                    color = 0xFF171A21
                )
            ),
            expanded = true,
            onDismiss = {},
            onOpen = {}
        )
    }
}
