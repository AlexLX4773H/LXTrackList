package com.lx.tracklist.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.lx.tracklist.R
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.RelatedMediaItem
import com.lx.tracklist.ui.theme.LXAiringBlue
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXErrorRed
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXProgressTrack
import com.lx.tracklist.ui.theme.LXScoreGold
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaCard(
    entry: MediaEntry,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    allLibraryEntries: List<MediaEntry> = emptyList(),
    onRelatedEntryClick: ((Long) -> Unit)? = null,
    onAddRelatedItem: ((RelatedMediaItem) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val progressAnimated by animateFloatAsState(targetValue = entry.progressFraction, label = "progress")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) LXPrimaryGreen else LXBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect?.invoke()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    if (isSelectionMode) {
                        onToggleSelect?.invoke()
                    } else {
                        onLongClick?.invoke()
                    }
                }
            ),
        color = if (isSelected) LXPrimaryGreen.copy(alpha = 0.08f) else LXSurfaceBlack
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Selection Checkbox (Visible in Selection Mode)
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) LXPrimaryGreen else Color.Transparent)
                        .border(2.dp, if (isSelected) LXPrimaryGreen else LXBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = LXBackgroundBlack,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            // Cover Image Thumbnail
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center
            ) {
                if (!entry.coverImageUri.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = entry.coverImageUri,
                        contentDescription = entry.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        success = {
                            com.lx.tracklist.data.local.FailedCoverTracker.markSuccess(entry.id, entry.coverImageUri)
                            SubcomposeAsyncImageContent()
                        },
                        loading = {
                            SmallLogoPlaceholder(size = 48.dp, isError = false)
                        },
                        error = {
                            com.lx.tracklist.data.local.FailedCoverTracker.markFailed(entry.id, entry.coverImageUri)
                            SmallLogoPlaceholder(size = 48.dp, isError = true)
                        }
                    )
                } else {
                    SmallLogoPlaceholder(size = 48.dp, isError = false)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title and Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (entry.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (entry.isFavorite) LXPrimaryGreen else LXTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = LXTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit Entry") },
                                    onClick = {
                                        showMenu = false
                                        onEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = LXErrorRed) },
                                    onClick = {
                                        showMenu = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }

                // Subtitle / Date / Alternate title
                val dateInfo = when {
                    entry.formatAiredDateRange().isNotBlank() -> entry.formatAiredDateRange()
                    entry.formatUserDateRange().isNotBlank() -> entry.formatUserDateRange()
                    else -> formatDateRange(entry.startDate, entry.finishDate)
                }
                val subtitleText = when {
                    !entry.alternateTitle.isNullOrBlank() -> entry.getAlternateTitles().joinToString(" • ")
                    dateInfo.isNotBlank() -> dateInfo
                    else -> entry.mediaType.displayName
                }
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Progress info & Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.displayProgress,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                        color = LXPrimaryGreen
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!entry.customCategory.isNullOrBlank()) {
                            Text(
                                text = entry.customCategory,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = LXPrimaryGreen,
                                modifier = Modifier
                                    .background(LXPrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }

                        if (entry.isAiring) {
                            Text(
                                text = "AIRING",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = LXAiringBlue,
                                modifier = Modifier
                                    .background(LXAiringBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }

                        if (entry.isRewatching) {
                            Text(
                                text = "REWATCHING",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = LXPrimaryGreen,
                                modifier = Modifier
                                    .background(LXPrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }

                        if (entry.score != null && entry.score > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(LXScoreGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = LXScoreGold,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${entry.score}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = LXScoreGold
                                )
                            }
                        } else if (entry.externalScore != null && entry.externalScore > 0f) {
                            val scoreStr = if (entry.externalScore % 1.0f == 0.0f) {
                                entry.externalScore.toInt().toString()
                            } else {
                                String.format(Locale.US, "%.1f", entry.externalScore)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(LXScoreGold.copy(alpha = 0.10f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = LXScoreGold.copy(alpha = 0.7f),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = scoreStr,
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp, fontWeight = FontWeight.Normal),
                                    color = LXScoreGold.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress bar + Quick Increment button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom Neon Progress Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(LXProgressTrack)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressAnimated)
                                .clip(RoundedCornerShape(3.dp))
                                .background(LXPrimaryGreen)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // +1 Quick Increment Button
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(LXPrimaryGreen.copy(alpha = 0.15f))
                            .border(1.dp, LXPrimaryGreen.copy(alpha = 0.4f), CircleShape)
                            .clickable(onClick = onIncrement),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "+1 Progress",
                            tint = LXPrimaryGreen,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // Related Franchise Section (Extracted during Sync)
            val correlatedRelated = remember(entry.relatedEntriesJson, allLibraryEntries) {
                entry.getCorrelatedRelatedItems(allLibraryEntries)
            }

            if (correlatedRelated.isNotEmpty()) {
                var isRelatedExpanded by remember { mutableStateOf(false) }
                val trackingCount = remember(correlatedRelated) { correlatedRelated.count { it.inLibraryEntryId != null } }
                val untrackedCount = correlatedRelated.size - trackingCount

                HorizontalDivider(
                    color = LXBorder.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .clickable { isRelatedExpanded = !isRelatedExpanded }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔗 ${correlatedRelated.size} Related:",
                            color = LXTextSecondary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            fontSize = 10.5.sp
                        )

                        // Tracking pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LXPrimaryGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "$trackingCount Tracking",
                                color = LXPrimaryGreen,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                fontSize = 9.5.sp
                            )
                        }

                        // Untracked pill
                        if (untrackedCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = "$untrackedCount Untracked",
                                    color = Color(0xFFF59E0B),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    fontSize = 9.5.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isRelatedExpanded) "Hide ▲" else "View ▼",
                        color = LXPrimaryGreen,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        fontSize = 10.sp
                    )
                }

                if (isRelatedExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141414))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        correlatedRelated.forEach { relItem ->
                            val isItemTracking = relItem.inLibraryEntryId != null
                            val relColor = when {
                                relItem.relationship.contains("Sequel", ignoreCase = true) -> LXPrimaryGreen
                                relItem.relationship.contains("Prequel", ignoreCase = true) -> Color(0xFF38BDF8)
                                relItem.relationship.contains("Collection", ignoreCase = true) || relItem.relationship.contains("Part", ignoreCase = true) -> LXScoreGold
                                relItem.relationship.contains("Spin", ignoreCase = true) || relItem.relationship.contains("Side", ignoreCase = true) -> Color(0xFFA78BFA)
                                else -> LXTextSecondary
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LXSurfaceBlack)
                                    .border(BorderStroke(0.5.dp, LXBorder.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (isItemTracking && relItem.inLibraryEntryId != null) {
                                            onRelatedEntryClick?.invoke(relItem.inLibraryEntryId)
                                        } else if (!isItemTracking) {
                                            onAddRelatedItem?.invoke(relItem)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(relColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = relItem.relationship.uppercase(),
                                                color = relColor,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                fontSize = 8.sp
                                            )
                                        }
                                        if (!relItem.releaseYear.isNullOrBlank()) {
                                            Text(
                                                text = relItem.releaseYear,
                                                color = LXTextSecondary,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 8.5.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = relItem.title,
                                        color = LXTextPrimary,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                if (isItemTracking) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(LXPrimaryGreen.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = LXPrimaryGreen,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = relItem.inLibraryStatus?.getDisplayLabel(relItem.mediaType) ?: "Tracking",
                                            color = LXPrimaryGreen,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            fontSize = 9.sp
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(LXSurfaceElevated)
                                            .border(0.5.dp, LXBorder, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = LXTextPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "Track",
                                            color = LXTextPrimary,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDateRange(start: Long?, finish: Long?): String {
    if (start == null && finish == null) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val startStr = start?.let { sdf.format(Date(it)) } ?: "???"
    val finishStr = finish?.let { sdf.format(Date(it)) } ?: if (start != null) "???" else ""
    return if (finishStr.isNotBlank()) "$startStr - $finishStr" else startStr
}

@Composable
private fun SmallLogoPlaceholder(size: Dp, isError: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.lx_logo),
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Fit
        )
        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Image Failed to Load",
                    tint = LXErrorRed,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
