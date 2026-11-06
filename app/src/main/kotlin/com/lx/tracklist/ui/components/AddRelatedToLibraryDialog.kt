package com.lx.tracklist.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.RelatedMediaItem
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary

@Composable
fun AddRelatedToLibraryDialog(
    item: RelatedMediaItem,
    isAdding: Boolean,
    onConfirm: (MediaStatus) -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = "Add & Open"
) {
    var selectedStatus by remember {
        mutableStateOf(MediaStatus.PLAN_TO_WATCH)
    }

    val availableStatuses = listOf(
        MediaStatus.PLAN_TO_WATCH,
        MediaStatus.WATCHING,
        MediaStatus.COMPLETED,
        MediaStatus.ON_HOLD,
        MediaStatus.DROPPED
    )

    AlertDialog(
        onDismissRequest = { if (!isAdding) onDismiss() },
        title = {
            Text("Add to Library", color = LXTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!item.posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp, 64.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.relationship} • ${item.mediaType.displayName}" + if (!item.releaseYear.isNullOrBlank()) " (${item.releaseYear})" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = LXTextSecondary
                        )
                    }
                }

                HorizontalDivider(color = LXBorder.copy(alpha = 0.5f))

                Text(
                    text = "Initial Status",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = LXTextSecondary
                )

                var statusExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { if (!isAdding) statusExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = LXSurfaceElevated,
                            contentColor = LXTextPrimary
                        ),
                        border = BorderStroke(1.dp, LXBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedStatus.getDisplayLabel(item.mediaType), color = LXTextPrimary)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = LXTextSecondary)
                        }
                    }

                    DropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false },
                        modifier = Modifier.background(LXSurfaceBlack)
                    ) {
                        availableStatuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.getDisplayLabel(item.mediaType), color = LXTextPrimary) },
                                onClick = {
                                    selectedStatus = status
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        containerColor = LXSurfaceBlack,
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isAdding
            ) {
                Text("Cancel", color = LXTextSecondary)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedStatus) },
                enabled = !isAdding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LXPrimaryGreen,
                    contentColor = LXBackgroundBlack
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = LXBackgroundBlack
                    )
                } else {
                    Text(confirmButtonText, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

