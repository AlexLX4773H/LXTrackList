package com.lx.tracklist.ui.edit

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.Priority
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.ui.components.CoverImageSearchDialog
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXErrorRed
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXScoreGold
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditScreen(
    viewModel: AddEditViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState(initial = emptyList())
    var showCoverSearchDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onNavigateBack)

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LXBackgroundBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.id == 0L) "Add New Entry" else "Edit Entry",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                },
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
                    IconButton(onClick = { viewModel.saveEntry() }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = LXPrimaryGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LXBackgroundBlack)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage ?: "",
                    color = LXErrorRed,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Category Chips
            Text("Category", style = MaterialTheme.typography.titleSmall, color = LXTextPrimary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaType.entries.forEach { cat ->
                    val isSelected = state.customCategory == null && state.mediaType == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.updateMediaType(cat) },
                        label = { Text(cat.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen,
                            selectedLabelColor = LXBackgroundBlack,
                            containerColor = LXSurfaceElevated,
                            labelColor = LXTextSecondary
                        )
                    )
                }

                customCategories.forEach { customCat ->
                    val isSelected = state.customCategory == customCat.name
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.updateCustomCategory(customCat.name)
                            if (customCat.isWatchType) {
                                viewModel.updateMediaType(MediaType.SHOW)
                            } else {
                                viewModel.updateMediaType(MediaType.MANGA)
                            }
                            viewModel.updateTrackingUnit(customCat.trackingUnit)
                        },
                        label = { Text("✨ ${customCat.name}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen,
                            selectedLabelColor = LXBackgroundBlack,
                            containerColor = LXSurfaceElevated,
                            labelColor = LXTextSecondary
                        )
                    )
                }
            }

            // Title
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Title *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedColors()
            )

            // Alternate Title
            OutlinedTextField(
                value = state.alternateTitle,
                onValueChange = { viewModel.updateAlternateTitle(it) },
                label = { Text("Alternate Title (e.g. Japanese / Korean)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedColors()
            )

            // Status Chips
            Text("Status", style = MaterialTheme.typography.titleSmall, color = LXTextPrimary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaStatus.entries.forEach { st ->
                    FilterChip(
                        selected = state.status == st,
                        onClick = { viewModel.updateStatus(st) },
                        label = { Text(st.getDisplayLabel(state.mediaType)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen,
                            selectedLabelColor = LXBackgroundBlack,
                            containerColor = LXSurfaceElevated,
                            labelColor = LXTextSecondary
                        )
                    )
                }
            }

            // Score Picker (0-10)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Score", style = MaterialTheme.typography.titleSmall, color = LXTextPrimary)
                    Text(
                        text = if (state.score != null) "${state.score}/10" else "Unrated",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LXScoreGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (0..10).forEach { s ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (state.score == s) LXScoreGold else LXSurfaceElevated)
                                .clickable { viewModel.updateScore(if (state.score == s) null else s) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (s == 0) "-" else "$s",
                                color = if (state.score == s) LXBackgroundBlack else LXTextSecondary,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Type-Specific Tracking Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Progress Tracking",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = LXPrimaryGreen
                    )

                    when (state.mediaType) {
                        MediaType.ANIME, MediaType.SHOW -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = if (state.currentEpisodes > 0) state.currentEpisodes.toString() else "",
                                    onValueChange = { viewModel.updateCurrentEpisodes(it.toIntOrNull() ?: 0) },
                                    label = { Text("Watched Ep") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    colors = outlinedColors()
                                )
                                OutlinedTextField(
                                    value = if (state.totalEpisodes > 0) state.totalEpisodes.toString() else "",
                                    onValueChange = { viewModel.updateTotalEpisodes(it.toIntOrNull() ?: 0) },
                                    label = { Text("Total Ep") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    colors = outlinedColors()
                                )
                            }

                            if (state.mediaType == MediaType.SHOW) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = state.currentSeasons.toString(),
                                        onValueChange = { viewModel.updateCurrentSeasons(it.toIntOrNull() ?: 1) },
                                        label = { Text("Current Season") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        colors = outlinedColors()
                                    )
                                    OutlinedTextField(
                                        value = state.totalSeasons.toString(),
                                        onValueChange = { viewModel.updateTotalSeasons(it.toIntOrNull() ?: 1) },
                                        label = { Text("Total Seasons") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        colors = outlinedColors()
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Currently Airing?", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = state.isAiring,
                                    onCheckedChange = { viewModel.updateIsAiring(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = LXBackgroundBlack,
                                        checkedTrackColor = LXPrimaryGreen
                                    )
                                )
                            }
                        }

                        MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK -> {
                            Text("Tracking Unit", style = MaterialTheme.typography.bodyMedium, color = LXTextSecondary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TrackingUnit.entries.forEach { unit ->
                                    FilterChip(
                                        selected = state.trackingUnit == unit,
                                        onClick = { viewModel.updateTrackingUnit(unit) },
                                        label = { Text(unit.displayName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LXPrimaryGreen,
                                            selectedLabelColor = LXBackgroundBlack,
                                            containerColor = LXSurfaceElevated,
                                            labelColor = LXTextSecondary
                                        )
                                    )
                                }
                            }

                            if (state.trackingUnit == TrackingUnit.CHAPTERS || state.trackingUnit == TrackingUnit.COMBINED) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = if (state.currentChapters > 0) state.currentChapters.toString() else "",
                                        onValueChange = { viewModel.updateCurrentChapters(it.toIntOrNull() ?: 0) },
                                        label = { Text("Read Chapters") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        colors = outlinedColors()
                                    )
                                    OutlinedTextField(
                                        value = if (state.totalChapters > 0) state.totalChapters.toString() else "",
                                        onValueChange = { viewModel.updateTotalChapters(it.toIntOrNull() ?: 0) },
                                        label = { Text("Total Chapters") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        colors = outlinedColors()
                                    )
                                }
                            }

                            if (state.trackingUnit == TrackingUnit.VOLUMES || state.trackingUnit == TrackingUnit.COMBINED) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = if (state.currentVolumes > 0) state.currentVolumes.toString() else "",
                                        onValueChange = { viewModel.updateCurrentVolumes(it.toIntOrNull() ?: 0) },
                                        label = { Text("Read Volumes") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        colors = outlinedColors()
                                    )
                                    OutlinedTextField(
                                        value = if (state.totalVolumes > 0) state.totalVolumes.toString() else "",
                                        onValueChange = { viewModel.updateTotalVolumes(it.toIntOrNull() ?: 0) },
                                        label = { Text("Total Volumes") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        colors = outlinedColors()
                                    )
                                }
                            }

                            if (state.trackingUnit == TrackingUnit.PAGES || state.trackingUnit == TrackingUnit.COMBINED) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = if (state.currentPages > 0) state.currentPages.toString() else "",
                                        onValueChange = { viewModel.updateCurrentPages(it.toIntOrNull() ?: 0) },
                                        label = { Text("Read Pages") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        colors = outlinedColors()
                                    )
                                    OutlinedTextField(
                                        value = if (state.totalPages > 0) state.totalPages.toString() else "",
                                        onValueChange = { viewModel.updateTotalPages(it.toIntOrNull() ?: 0) },
                                        label = { Text("Total Pages") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        colors = outlinedColors()
                                    )
                                }
                            }
                        }

                        MediaType.MOVIE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = state.movieSeriesName,
                                    onValueChange = { viewModel.updateMovieSeriesName(it) },
                                    label = { Text("Series / Franchise Name") },
                                    modifier = Modifier.weight(1.5f),
                                    colors = outlinedColors()
                                )
                                OutlinedTextField(
                                    value = state.movieSeriesNumber?.toString() ?: "",
                                    onValueChange = { viewModel.updateMovieSeriesNumber(it.toIntOrNull()) },
                                    label = { Text("Movie #") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    colors = outlinedColors()
                                )
                            }
                        }
                    }
                }
            }

            // Cover Image Section with Web Search Button & Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cover Image", style = MaterialTheme.typography.titleSmall, color = LXTextPrimary)
                Button(
                    onClick = { showCoverSearchDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LXPrimaryGreen.copy(alpha = 0.15f),
                        contentColor = LXPrimaryGreen
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Search Web for Cover", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (state.coverImageUri.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = state.coverImageUri,
                        contentDescription = "Cover Preview",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, LXBorder, RoundedCornerShape(6.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cover preview", color = LXPrimaryGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(state.coverImageUri, color = LXTextSecondary, fontSize = 11.sp, maxLines = 1)
                    }
                    IconButton(onClick = { viewModel.updateCoverUri("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = LXTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            OutlinedTextField(
                value = state.coverImageUri,
                onValueChange = { viewModel.updateCoverUri(it) },
                label = { Text("Cover Image URL (or search above)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedColors()
            )

            if (showCoverSearchDialog) {
                val q = (state.title.ifBlank { "manga" }) + " cover"
                CoverImageSearchDialog(
                    initialQuery = q,
                    onImageSelected = { url ->
                        viewModel.updateCoverUri(url)
                        showCoverSearchDialog = false
                    },
                    onDismiss = { showCoverSearchDialog = false }
                )
            }

            // Tags
            OutlinedTextField(
                value = state.tags,
                onValueChange = { viewModel.updateTags(it) },
                label = { Text("Tags (comma-separated, e.g. action, isekai)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedColors()
            )

            // Notes
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("Notes / Personal Comments") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedColors()
            )

            // Favorite Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.updateIsFavorite(!state.isFavorite) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (state.isFavorite) LXPrimaryGreen else LXTextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mark as Favorite", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium)
            }

            // Save Button
            Button(
                onClick = { viewModel.saveEntry() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LXPrimaryGreen,
                    contentColor = LXBackgroundBlack
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (state.id == 0L) "Add to Tracker" else "Save Changes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
}

@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LXPrimaryGreen,
    unfocusedBorderColor = LXBorder,
    focusedLabelColor = LXPrimaryGreen,
    unfocusedLabelColor = LXTextSecondary,
    focusedTextColor = LXTextPrimary,
    unfocusedTextColor = LXTextPrimary,
    cursorColor = LXPrimaryGreen
)
