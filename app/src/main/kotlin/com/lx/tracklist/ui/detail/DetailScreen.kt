package com.lx.tracklist.ui.detail

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.lx.tracklist.R
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
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

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 720

    LaunchedEffect(Unit) {
        viewModel.deletedEvent.collect {
            onNavigateBack()
        }
    }

    LaunchedEffect(isDeleted) {
        if (isDeleted) {
            viewModel.resetDeletedState()
            onNavigateBack()
        }
    }

    BackHandler(onBack = onNavigateBack)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LXBackgroundBlack,
        topBar = {
            TopAppBar(
                title = { Text(if (isTablet) (entry?.title ?: "Details") else "Details", color = LXTextPrimary, maxLines = 1) },
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
            if (isTablet) {
                // Tablet Two-Pane Layout (e.g. OnePlus Pad)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Column (Cover art & Overview)
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(LXSurfaceBlack)
                                .border(1.dp, LXBorder, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentEntry.coverImageUri.isNullOrBlank()) {
                                SubcomposeAsyncImage(
                                    model = currentEntry.coverImageUri,
                                    contentDescription = currentEntry.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    success = {
                                        SubcomposeAsyncImageContent()
                                    },
                                    loading = {
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(R.drawable.lx_logo),
                                            contentDescription = null,
                                            modifier = Modifier.size(110.dp).clip(CircleShape),
                                            contentScale = ContentScale.Fit
                                        )
                                    },
                                    error = {
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(R.drawable.lx_logo),
                                            contentDescription = null,
                                            modifier = Modifier.size(110.dp).clip(CircleShape),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                )
                            } else {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(R.drawable.lx_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(110.dp).clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        Text(
                            text = currentEntry.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )

                        val altTitles = currentEntry.getAlternateTitles()
                        if (altTitles.isNotEmpty()) {
                            altTitles.forEach { alt ->
                                Text(
                                    text = alt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LXTextSecondary
                                )
                            }
                        }

                        // Badges
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetaChip(label = currentEntry.mediaType.displayName, color = LXPrimaryGreen)
                            if (!currentEntry.customCategory.isNullOrBlank()) {
                                MetaChip(label = currentEntry.customCategory, color = LXScoreGold)
                            }
                            if (!currentEntry.subCategory.isNullOrBlank()) {
                                MetaChip(label = currentEntry.subCategory, color = LXScoreGold)
                            }
                            StatusDropdownChip(
                                status = currentEntry.status,
                                mediaType = currentEntry.mediaType,
                                onStatusSelected = { viewModel.updateStatus(it) }
                            )
                            ScoreDropdownChip(
                                score = currentEntry.score,
                                onScoreSelected = { viewModel.updateScore(it) }
                            )
                            if (currentEntry.mediaType == MediaType.MOVIE && currentEntry.durationMinutes != null && currentEntry.durationMinutes > 0) {
                                val h = currentEntry.durationMinutes / 60
                                val m = currentEntry.durationMinutes % 60
                                val durStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                                MetaChip(label = "⏱️ $durStr", color = LXPrimaryGreen)
                            } else if (currentEntry.episodeLengthMinutes != null && currentEntry.episodeLengthMinutes > 0) {
                                MetaChip(label = "⏱️ ${currentEntry.episodeLengthMinutes} min/ep", color = LXPrimaryGreen)
                            }
                            if (currentEntry.isRewatching) {
                                MetaChip(label = "REWATCHING", color = LXPrimaryGreen)
                            }
                            if (currentEntry.rewatchCount > 0) {
                                MetaChip(label = "Rewatched ${currentEntry.rewatchCount}x", color = LXScoreGold)
                            }
                        }
                    }

                    // Right Column (Progress, Dates, Notes, Tags)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DetailProgressCard(
                            entry = currentEntry,
                            onIncrement = { viewModel.increment() },
                            onDecrement = { viewModel.decrement() },
                            onStartRewatch = { viewModel.startRewatch() },
                            onFinishRewatch = { viewModel.finishRewatch() },
                            onCancelRewatch = { viewModel.cancelRewatch() }
                        )
                        DetailDatesCard(currentEntry)
                        DetailNotesCard(currentEntry)
                        DetailTagsCard(currentEntry)
                        DetailSourceText(currentEntry)
                    }
                }
            } else {
                // Phone Single-Column Layout (e.g. OnePlus 12R)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(LXSurfaceBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentEntry.coverImageUri.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = currentEntry.coverImageUri,
                                contentDescription = currentEntry.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                success = {
                                    SubcomposeAsyncImageContent()
                                },
                                loading = {
                                    androidx.compose.foundation.Image(
                                        painter = painterResource(R.drawable.lx_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(110.dp).clip(CircleShape),
                                        contentScale = ContentScale.Fit
                                    )
                                },
                                error = {
                                    androidx.compose.foundation.Image(
                                        painter = painterResource(R.drawable.lx_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(110.dp).clip(CircleShape),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            )
                        } else {
                            androidx.compose.foundation.Image(
                                painter = painterResource(R.drawable.lx_logo),
                                contentDescription = null,
                                modifier = Modifier.size(110.dp).clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                        }

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
                        Text(
                            text = currentEntry.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )

                        val altTitles = currentEntry.getAlternateTitles()
                        if (altTitles.isNotEmpty()) {
                            altTitles.forEach { alt ->
                                Text(
                                    text = alt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LXTextSecondary
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetaChip(label = currentEntry.mediaType.displayName, color = LXPrimaryGreen)
                            if (!currentEntry.customCategory.isNullOrBlank()) {
                                MetaChip(label = currentEntry.customCategory, color = LXScoreGold)
                            }
                            if (!currentEntry.subCategory.isNullOrBlank()) {
                                MetaChip(label = currentEntry.subCategory, color = LXScoreGold)
                            }
                            StatusDropdownChip(
                                status = currentEntry.status,
                                mediaType = currentEntry.mediaType,
                                onStatusSelected = { viewModel.updateStatus(it) }
                            )
                            ScoreDropdownChip(
                                score = currentEntry.score,
                                onScoreSelected = { viewModel.updateScore(it) }
                            )
                            if (currentEntry.mediaType == MediaType.MOVIE && currentEntry.durationMinutes != null && currentEntry.durationMinutes > 0) {
                                val h = currentEntry.durationMinutes / 60
                                val m = currentEntry.durationMinutes % 60
                                val durStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                                MetaChip(label = "⏱️ $durStr", color = LXPrimaryGreen)
                            } else if (currentEntry.episodeLengthMinutes != null && currentEntry.episodeLengthMinutes > 0) {
                                MetaChip(label = "⏱️ ${currentEntry.episodeLengthMinutes} min/ep", color = LXPrimaryGreen)
                            }
                            if (currentEntry.isRewatching) {
                                MetaChip(label = "REWATCHING", color = LXPrimaryGreen)
                            }
                            if (currentEntry.rewatchCount > 0) {
                                MetaChip(label = "Rewatched ${currentEntry.rewatchCount}x", color = LXScoreGold)
                            }
                            if (currentEntry.isAiring) {
                                MetaChip(label = "AIRING", color = LXAiringBlue)
                            }
                        }

                        DetailProgressCard(
                            entry = currentEntry,
                            onIncrement = { viewModel.increment() },
                            onDecrement = { viewModel.decrement() },
                            onStartRewatch = { viewModel.startRewatch() },
                            onFinishRewatch = { viewModel.finishRewatch() },
                            onCancelRewatch = { viewModel.cancelRewatch() }
                        )
                        DetailDatesCard(currentEntry)
                        DetailNotesCard(currentEntry)
                        DetailTagsCard(currentEntry)
                        DetailSourceText(currentEntry)

                        Spacer(modifier = Modifier.height(24.dp))
                    }
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
private fun DetailProgressCard(
    entry: MediaEntry,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onStartRewatch: () -> Unit,
    onFinishRewatch: () -> Unit,
    onCancelRewatch: () -> Unit
) {
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
                    text = entry.displayProgress,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXPrimaryGreen
                )
            }

            // Rewatching banner & controls
            if (entry.isRewatching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXPrimaryGreen.copy(alpha = 0.12f))
                        .border(1.dp, LXPrimaryGreen.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = LXPrimaryGreen, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Rewatching${if (entry.rewatchCount > 0) " (#${entry.rewatchCount + 1})" else ""}",
                            color = LXPrimaryGreen,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onCancelRewatch,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Cancel", color = LXTextSecondary, fontSize = 11.sp)
                        }
                        Button(
                            onClick = onFinishRewatch,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LXPrimaryGreen,
                                contentColor = LXBackgroundBlack
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Finish Rewatch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // If it is an airing show/anime, display user's episodes watched, aired episodes and total episodes
            if (entry.isAiring && (entry.mediaType == MediaType.ANIME || entry.mediaType == MediaType.SHOW)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Watched", color = LXTextSecondary, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${entry.currentEpisodes}",
                            color = LXPrimaryGreen,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Aired", color = LXTextSecondary, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (entry.airedEpisodes > 0) "${entry.airedEpisodes}" else "-",
                            color = LXAiringBlue,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total", color = LXTextSecondary, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (entry.totalEpisodes > 0) "${entry.totalEpisodes}" else "?",
                            color = LXTextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (entry.isAiring && (entry.mediaType == MediaType.MANGA || entry.mediaType == MediaType.MANHWA)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Read", color = LXTextSecondary, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${entry.currentChapters}",
                            color = LXPrimaryGreen,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Released", color = LXTextSecondary, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (entry.airedChapters > 0) "${entry.airedChapters}" else "-",
                            color = LXAiringBlue,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total", color = LXTextSecondary, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (entry.totalChapters > 0) "${entry.totalChapters}" else "?",
                            color = LXTextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

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
                        .fillMaxWidth(entry.progressFraction)
                        .clip(RoundedCornerShape(4.dp))
                        .background(LXPrimaryGreen)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (entry.status == MediaStatus.COMPLETED && !entry.isRewatching) {
                    OutlinedButton(
                        onClick = onStartRewatch,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LXPrimaryGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = LXPrimaryGreen, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Start Rewatch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LXSurfaceElevated)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrement", tint = LXTextPrimary)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = onIncrement,
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
    }
}

@Composable
private fun DetailDatesCard(entry: MediaEntry) {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val userStartStr = entry.startDate?.let { sdf.format(Date(it)) }
    val userFinishStr = entry.finishDate?.let { sdf.format(Date(it)) }

    val airedStartStr = entry.airedStartDate?.let { sdf.format(Date(it)) }
    val airedEndStr = if (entry.isAiring && entry.airedEndDate == null) "Present" else entry.airedEndDate?.let { sdf.format(Date(it)) }

    val hasUserData = userStartStr != null || userFinishStr != null
    val hasAiredData = airedStartStr != null || airedEndStr != null

    if (!hasUserData && !hasAiredData) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
        border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasAiredData) {
                val airedTitle = when (entry.mediaType) {
                    MediaType.ANIME, MediaType.SHOW -> "Aired / Broadcast Dates"
                    MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK -> "Publishing Period"
                    MediaType.MOVIE -> "Release Date"
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(airedTitle, color = LXPrimaryGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("From", color = LXTextSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(airedStartStr ?: "-", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("To", color = LXTextSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(airedEndStr ?: "-", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (hasUserData && hasAiredData) {
                HorizontalDivider(color = LXBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
            }

            if (hasUserData) {
                val userTitle = when (entry.mediaType) {
                    MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK -> "My Reading Dates"
                    else -> "My Watch Dates"
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(userTitle, color = LXPrimaryGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Started", color = LXTextSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(userStartStr ?: "-", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Completed", color = LXTextSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(userFinishStr ?: "-", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailNotesCard(entry: MediaEntry) {
    if (!entry.notes.isNullOrBlank()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Notes", color = LXPrimaryGreen, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                Text(entry.notes, color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailTagsCard(entry: MediaEntry) {
    if (!entry.tags.isNullOrBlank()) {
        Column {
            Text("Tags", color = LXTextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                entry.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
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
}

@Composable
private fun DetailSourceText(entry: MediaEntry) {
    if (!entry.externalSource.isNullOrBlank()) {
        Text(
            text = "Imported from ${entry.externalSource}" + if (entry.externalId != null) " (ID: ${entry.externalId})" else "",
            style = MaterialTheme.typography.bodySmall,
            color = LXTextSecondary.copy(alpha = 0.6f)
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

@Composable
private fun StatusDropdownChip(
    status: MediaStatus,
    mediaType: MediaType,
    onStatusSelected: (MediaStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (status) {
        MediaStatus.COMPLETED -> LXPrimaryGreen
        MediaStatus.WATCHING -> LXPrimaryGreen
        MediaStatus.ON_HOLD -> LXScoreGold
        MediaStatus.DROPPED -> LXErrorRed.copy(alpha = 0.85f)
        MediaStatus.PLAN_TO_WATCH -> LXTextSecondary
    }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(statusColor.copy(alpha = 0.15f))
                .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                .clickable { expanded = true }
                .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = status.getDisplayLabel(mediaType),
                color = statusColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Change Status",
                tint = statusColor,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(LXSurfaceElevated)
        ) {
            MediaStatus.entries.forEach { item ->
                val isSelected = item == status
                val itemLabel = item.getDisplayLabel(mediaType)
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = itemLabel,
                                color = if (isSelected) LXPrimaryGreen else LXTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onStatusSelected(item)
                    }
                )
            }
        }
    }
}

@Composable
private fun ScoreDropdownChip(
    score: Int?,
    onScoreSelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val hasScore = score != null && score > 0

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (hasScore) LXScoreGold.copy(alpha = 0.15f) else LXSurfaceElevated)
                .border(
                    width = 1.dp,
                    color = if (hasScore) LXScoreGold.copy(alpha = 0.45f) else LXBorder,
                    shape = RoundedCornerShape(6.dp)
                )
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (hasScore) LXScoreGold else LXTextSecondary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (hasScore) "$score/10" else "Rate",
                color = if (hasScore) LXScoreGold else LXTextSecondary,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Score",
                tint = if (hasScore) LXScoreGold else LXTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(LXSurfaceElevated)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "— Clear / Unrated",
                        color = if (score == null) LXPrimaryGreen else LXTextSecondary,
                        fontWeight = if (score == null) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = {
                    expanded = false
                    onScoreSelected(null)
                }
            )
            (10 downTo 1).forEach { s ->
                val isSelected = score == s
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isSelected) LXScoreGold else LXTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "$s / 10",
                                color = if (isSelected) LXScoreGold else LXTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onScoreSelected(s)
                    }
                )
            }
        }
    }
}
