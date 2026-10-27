package com.lx.tracklist.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lx.tracklist.R
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val entry by viewModel.entry.collectAsState()
    val isDeleted by viewModel.isDeleted.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isDeleted) {
        if (isDeleted) {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LXBackgroundBlack,
        topBar = {
            TopAppBar(
                title = { Text("Details", color = LXTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LXPrimaryGreen
                        )
                    }
                },
                actions = {
                    entry?.let { e ->
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (e.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (e.isFavorite) LXPrimaryGreen else LXTextSecondary
                            )
                        }
                        IconButton(onClick = { onEdit(e.id) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = LXPrimaryGreen
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = LXErrorRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LXBackgroundBlack)
            )
        }
    ) { paddingValues ->
        val currentEntry = entry
        if (currentEntry == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", color = LXTextSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Large Banner / Cover Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(LXSurfaceBlack),
                    contentAlignment = Alignment.Center
                ) {
                    if (!currentEntry.coverImageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = currentEntry.coverImageUri,
                            contentDescription = currentEntry.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.lx_logo),
                            placeholder = painterResource(R.drawable.lx_logo)
                        )
                    } else {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.lx_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Bottom Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, LXBackgroundBlack),
                                    startY = 100f
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Titles
                    Text(
                        text = currentEntry.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )

                    if (!currentEntry.alternateTitle.isNullOrBlank()) {
                        Text(
                            text = currentEntry.alternateTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LXTextSecondary
                        )
                    }

                    // Metadata Chips (Status, Score, Priority, Airing)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category Chip
                        MetaChip(label = currentEntry.mediaType.displayName, color = LXPrimaryGreen)

                        // Status Chip
                        MetaChip(
                            label = currentEntry.status.getDisplayLabel(currentEntry.mediaType),
                            color = if (currentEntry.status == com.lx.tracklist.data.model.MediaStatus.COMPLETED) LXPrimaryGreen else LXTextSecondary
                        )

                        // Score Chip
                        if (currentEntry.score != null && currentEntry.score > 0) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LXScoreGold.copy(alpha = 0.15f))
                                    .border(1.dp, LXScoreGold.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = LXScoreGold, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${currentEntry.score}/10",
                                    color = LXScoreGold,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        if (currentEntry.isAiring) {
                            MetaChip(label = "AIRING", color = LXAiringBlue)
                        }
                    }

                    // Interactive Progress Tracker Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Current Progress",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LXTextPrimary
                                )

                                Text(
                                    text = currentEntry.displayProgress,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LXPrimaryGreen
                                )
                            }

                            // Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LXProgressTrack)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(currentEntry.progressFraction)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(LXPrimaryGreen)
                                )
                            }

                            // Progress Controls (- and +)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.decrement() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(LXSurfaceElevated)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrement", tint = LXTextPrimary)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                IconButton(
                                    onClick = { viewModel.increment() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(LXPrimaryGreen)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increment", tint = LXBackgroundBlack)
                                }
                            }
                        }
                    }

                    // Dates
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val startStr = currentEntry.startDate?.let { sdf.format(Date(it)) }
                    val finishStr = currentEntry.finishDate?.let { sdf.format(Date(it)) }

                    if (startStr != null || finishStr != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Started", color = LXTextSecondary, style = MaterialTheme.typography.bodySmall)
                                    Text(startStr ?: "-", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Finished", color = LXTextSecondary, style = MaterialTheme.typography.bodySmall)
                                    Text(finishStr ?: "-", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Notes
                    if (!currentEntry.notes.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Notes", color = LXPrimaryGreen, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(currentEntry.notes, color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // Tags
                    if (!currentEntry.tags.isNullOrBlank()) {
                        Column {
                            Text("Tags", color = LXTextSecondary, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                currentEntry.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(LXSurfaceElevated)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("#$tag", color = LXPrimaryGreen, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    // External ID / Source info
                    if (!currentEntry.externalSource.isNullOrBlank()) {
                        Text(
                            text = "Imported from ${currentEntry.externalSource}" + if (currentEntry.externalId != null) " (ID: ${currentEntry.externalId})" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = LXTextSecondary.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Entry?", color = LXTextPrimary) },
            text = { Text("Are you sure you want to delete this entry? This action cannot be undone.", color = LXTextSecondary) },
            containerColor = LXSurfaceBlack,
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteEntry()
                }) {
                    Text("Delete", color = LXErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = LXTextPrimary)
                }
            }
        )
    }
}

@Composable
private fun MetaChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}
