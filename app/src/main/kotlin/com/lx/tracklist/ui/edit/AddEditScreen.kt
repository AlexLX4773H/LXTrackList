package com.lx.tracklist.ui.edit

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
    val subCategories by viewModel.subCategories.collectAsState(initial = emptyList())
    var showCoverSearchDialog by remember { mutableStateOf(false) }
    var mainCategoryExpanded by remember { mutableStateOf(false) }
    var subCategoryExpanded by remember { mutableStateOf(false) }
    var pendingMainCategory by remember { mutableStateOf<MainCategoryItem?>(null) }
    var pendingSubCategoryChange by remember { mutableStateOf<PendingSubCategoryChange?>(null) }
    var isForeignDbUnlocked by remember { mutableStateOf(false) }
    var showForeignDbWarningDialog by remember { mutableStateOf(false) }
    var databaseSourceExpanded by remember { mutableStateOf(false) }
    var externalScoreSourceExpanded by remember { mutableStateOf(false) }

    BackHandler(onBack = onNavigateBack)

    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect {
            onNavigateBack()
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            viewModel.resetSavedState()
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

            // Title
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Title *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedColors()
            )

            // Alternate Titles (Support for multiple alternate titles)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alternate Titles (Optional)",
                        style = MaterialTheme.typography.titleSmall,
                        color = LXTextPrimary
                    )
                    TextButton(onClick = { viewModel.addAlternateTitle() }) {
                        Text(
                            text = "+ Add Alternate Title",
                            color = LXPrimaryGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (state.alternateTitles.isEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.addAlternateTitle() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LXPrimaryGreen),
                        border = BorderStroke(1.dp, LXBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = LXPrimaryGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("+ Add Alternate Title (e.g. Japanese, Korean)", color = LXTextPrimary)
                    }
                } else {
                    state.alternateTitles.forEachIndexed { index, altTitle ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = altTitle,
                                onValueChange = { viewModel.updateAlternateTitle(index, it) },
                                label = { Text("Alternate Title ${index + 1}") },
                                placeholder = { Text("e.g. Shingeki no Kyojin, 進撃の巨人...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = outlinedColors()
                            )
                            IconButton(onClick = { viewModel.removeAlternateTitle(index) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = LXErrorRed)
                            }
                        }
                    }
                }
            }

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
                                if (state.isAiring) {
                                    OutlinedTextField(
                                        value = if (state.airedEpisodes > 0) state.airedEpisodes.toString() else "",
                                        onValueChange = { viewModel.updateAiredEpisodes(it.toIntOrNull() ?: 0) },
                                        label = { Text("Aired Ep") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        colors = outlinedColors()
                                    )
                                }
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

                            OutlinedTextField(
                                value = if (state.episodeLengthMinutes != null && state.episodeLengthMinutes!! > 0) state.episodeLengthMinutes.toString() else "",
                                onValueChange = { viewModel.updateEpisodeLengthMinutes(it.toIntOrNull()?.takeIf { len -> len > 0 }) },
                                label = { Text("Episode Runtime (mins)") },
                                placeholder = { Text("e.g. 24") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = outlinedColors()
                            )

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
                                    if (state.isAiring) {
                                        OutlinedTextField(
                                            value = if (state.airedChapters > 0) state.airedChapters.toString() else "",
                                            onValueChange = { viewModel.updateAiredChapters(it.toIntOrNull() ?: 0) },
                                            label = { Text("Aired Ch") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            colors = outlinedColors()
                                        )
                                    }
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Currently Publishing / Ongoing?", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium)
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

                            OutlinedTextField(
                                value = if (state.durationMinutes != null && state.durationMinutes!! > 0) state.durationMinutes.toString() else "",
                                onValueChange = { viewModel.updateDurationMinutes(it.toIntOrNull()?.takeIf { len -> len > 0 }) },
                                label = { Text("Movie Runtime (mins)") },
                                placeholder = { Text("e.g. 110") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = outlinedColors()
                            )
                        }
                    }
                }
            }

            // Dates & Schedule Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                border = BorderStroke(1.dp, LXBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Dates & Schedule",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = LXPrimaryGreen
                    )

                    // Personal Watch / Reading Dates
                    val personalLabel = when (state.mediaType) {
                        MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK -> "My Reading Dates"
                        else -> "My Watch Dates"
                    }
                    Text(
                        text = personalLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = LXTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    AdaptiveDateRow(
                        startLabel = "Started",
                        startTimestamp = state.startDate,
                        onStartDateSelected = { viewModel.updateStartDate(it) },
                        endLabel = "Completed",
                        endTimestamp = state.finishDate,
                        onEndDateSelected = { viewModel.updateFinishDate(it) }
                    )

                    HorizontalDivider(color = LXBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Broadcast / Publishing Dates
                    val airedLabel = when (state.mediaType) {
                        MediaType.ANIME, MediaType.SHOW -> "Airing / Broadcast Dates"
                        MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK -> "Publishing Dates"
                        MediaType.MOVIE -> "Theatrical / Release Dates"
                    }
                    Text(
                        text = airedLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = LXTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    AdaptiveDateRow(
                        startLabel = "From",
                        startTimestamp = state.airedStartDate,
                        onStartDateSelected = { viewModel.updateAiredStartDate(it) },
                        endLabel = "To",
                        endTimestamp = state.airedEndDate,
                        onEndDateSelected = { viewModel.updateAiredEndDate(it) }
                    )
                }
            }

            // Rewatch Details Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                border = BorderStroke(1.dp, LXBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Rewatch Details",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = LXPrimaryGreen
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Currently Rewatching?", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium)
                            Text("Flag that you are re-experiencing this title", color = LXTextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = state.isRewatching,
                            onCheckedChange = { viewModel.updateIsRewatching(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LXBackgroundBlack,
                                checkedTrackColor = LXPrimaryGreen
                            )
                        )
                    }
                    OutlinedTextField(
                        value = if (state.rewatchCount > 0) state.rewatchCount.toString() else "",
                        onValueChange = { viewModel.updateRewatchCount(it.toIntOrNull() ?: 0) },
                        label = { Text("Times Rewatched / Re-read") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                }
            }

            // Foreign Database Information Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                border = BorderStroke(1.dp, LXBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Foreign Database Information",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = LXPrimaryGreen
                            )
                            Text(
                                text = "Source database and external ID (e.g. TMDB ID for Movies/Shows, MAL ID for Anime/Manga)",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextSecondary
                            )
                        }
                        if (!isForeignDbUnlocked) {
                            TextButton(
                                onClick = { showForeignDbWarningDialog = true }
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = LXScoreGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", color = LXScoreGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = databaseSourceExpanded && isForeignDbUnlocked,
                            onExpandedChange = {
                                if (!isForeignDbUnlocked) {
                                    showForeignDbWarningDialog = true
                                } else {
                                    databaseSourceExpanded = it
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = state.externalSource.ifBlank { "None" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Database Source") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = databaseSourceExpanded)
                                },
                                colors = outlinedColors(),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = databaseSourceExpanded && isForeignDbUnlocked,
                                onDismissRequest = { databaseSourceExpanded = false },
                                modifier = Modifier.background(LXSurfaceElevated)
                            ) {
                                val allSources = remember(state.externalSource) {
                                    val list = mutableListOf("MAL", "AniList", "Kitsu", "TMDB", "IMDb", "TVDb", "Tachiyomi")
                                    if (state.externalSource.isNotBlank() && !list.contains(state.externalSource)) {
                                        list.add(0, state.externalSource)
                                    }
                                    list
                                }

                                allSources.forEach { source ->
                                    val isSelected = state.externalSource.equals(source, ignoreCase = true)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = source,
                                                color = if (isSelected) LXPrimaryGreen else LXTextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.updateExternalSource(source)
                                            databaseSourceExpanded = false
                                        }
                                    )
                                }

                                HorizontalDivider(color = LXBorder.copy(alpha = 0.5f))

                                val isNoneSelected = state.externalSource.isBlank()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "None (No External DB)",
                                            color = if (isNoneSelected) LXPrimaryGreen else LXTextSecondary,
                                            fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateExternalSource("")
                                        databaseSourceExpanded = false
                                    }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = state.externalId,
                            onValueChange = {
                                if (isForeignDbUnlocked) viewModel.updateExternalId(it)
                                else showForeignDbWarningDialog = true
                            },
                            readOnly = !isForeignDbUnlocked,
                            label = { Text("Foreign Database ID") },
                            placeholder = { Text("e.g. 38101") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f),
                            colors = outlinedColors()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = externalScoreSourceExpanded && isForeignDbUnlocked,
                            onExpandedChange = {
                                if (!isForeignDbUnlocked) {
                                    showForeignDbWarningDialog = true
                                } else {
                                    externalScoreSourceExpanded = it
                                }
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = state.externalScoreSource.ifBlank { "None" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Score Source") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = externalScoreSourceExpanded)
                                },
                                colors = outlinedColors(),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = externalScoreSourceExpanded && isForeignDbUnlocked,
                                onDismissRequest = { externalScoreSourceExpanded = false },
                                modifier = Modifier.background(LXSurfaceElevated)
                            ) {
                                val allScoreSources = remember(state.externalScoreSource) {
                                    val list = mutableListOf("MAL", "IMDb", "Rotten Tomatoes", "AniList", "TMDB", "Metacritic")
                                    if (state.externalScoreSource.isNotBlank() && !list.contains(state.externalScoreSource)) {
                                        list.add(0, state.externalScoreSource)
                                    }
                                    list
                                }

                                allScoreSources.forEach { source ->
                                    val isSelected = state.externalScoreSource.equals(source, ignoreCase = true)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = source,
                                                color = if (isSelected) LXPrimaryGreen else LXTextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.updateExternalScoreSource(source)
                                            externalScoreSourceExpanded = false
                                        }
                                    )
                                }

                                HorizontalDivider(color = LXBorder.copy(alpha = 0.5f))

                                val isNoneSelected = state.externalScoreSource.isBlank()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "None (No Score Source)",
                                            color = if (isNoneSelected) LXPrimaryGreen else LXTextSecondary,
                                            fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateExternalScoreSource("")
                                        externalScoreSourceExpanded = false
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.externalScore?.let {
                                if (it % 1.0f == 0.0f) it.toInt().toString() else it.toString()
                            } ?: "",
                            onValueChange = { input ->
                                if (isForeignDbUnlocked) {
                                    viewModel.updateExternalScore(input.toFloatOrNull())
                                } else {
                                    showForeignDbWarningDialog = true
                                }
                            },
                            readOnly = !isForeignDbUnlocked,
                            label = { Text("External Score") },
                            placeholder = { Text("e.g. 8.5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1.5f),
                            colors = outlinedColors()
                        )
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

            var isCoverImageError by remember(state.coverImageUri) { mutableStateOf(false) }

            if (state.coverImageUri.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = if (isCoverImageError) LXErrorRed else LXBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                width = 1.dp,
                                color = if (isCoverImageError) LXErrorRed else LXBorder,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .background(Color(0xFF1E1E1E)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = state.coverImageUri,
                            contentDescription = "Cover Preview",
                            onError = { isCoverImageError = true },
                            onSuccess = { isCoverImageError = false },
                            onLoading = { isCoverImageError = false },
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        if (isCoverImageError) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = "Image Failed to Load",
                                tint = LXErrorRed,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isCoverImageError) "Cover preview (Failed to load)" else "Cover preview",
                            color = if (isCoverImageError) LXErrorRed else LXPrimaryGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.coverImageUri,
                            color = if (isCoverImageError) LXErrorRed.copy(alpha = 0.85f) else LXTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCoverImageError) {
                            Text(
                                text = "Image URL is not accessible or failed to load",
                                color = LXErrorRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.updateCoverUri("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = if (isCoverImageError) LXErrorRed else LXTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.coverImageUri,
                onValueChange = {
                    isCoverImageError = false
                    viewModel.updateCoverUri(it)
                },
                label = { Text("Cover Image URL (or search above)") },
                singleLine = true,
                isError = isCoverImageError,
                supportingText = if (isCoverImageError) {
                    {
                        Text(
                            text = "Image could not be loaded from this URL. Server may block hotlinking or URL is broken.",
                            color = LXErrorRed,
                            fontSize = 11.sp
                        )
                    }
                } else null,
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

            // Confirmation Dialog for Main Category
            if (pendingMainCategory != null) {
                val target = pendingMainCategory!!
                val currentCategoryDisplayName = state.customCategory ?: state.mediaType.displayName
                val message = if (state.subCategory.isNullOrBlank()) {
                    "Are you sure you want to change the category from \"$currentCategoryDisplayName\" to \"${target.displayName}\"?"
                } else {
                    "Are you sure you want to change the category from \"$currentCategoryDisplayName\" to \"${target.displayName}\"? This will reset the sub category."
                }
                AlertDialog(
                    onDismissRequest = { pendingMainCategory = null },
                    title = {
                        Text(
                            text = "Change Main Category?",
                            color = LXTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = message,
                            color = LXTextSecondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.selectCategory(target.mediaType, target.customCategory)
                                pendingMainCategory = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LXPrimaryGreen,
                                contentColor = LXBackgroundBlack
                            )
                        ) {
                            Text("Change", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingMainCategory = null }) {
                            Text("Cancel", color = LXTextSecondary)
                        }
                    },
                    containerColor = LXSurfaceElevated
                )
            }

            // Confirmation Dialog for Sub Category
            if (pendingSubCategoryChange != null) {
                val targetName = pendingSubCategoryChange!!.name
                val currentSubName = if (state.subCategory.isNullOrBlank()) "None" else state.subCategory!!
                val targetDisplay = targetName ?: "None"
                val dialogMessage = if (targetName == null) {
                    "Are you sure you want to remove the sub category \"$currentSubName\"?"
                } else if (state.subCategory.isNullOrBlank()) {
                    "Are you sure you want to set the sub category to \"$targetDisplay\"?"
                } else {
                    "Are you sure you want to change the sub category from \"$currentSubName\" to \"$targetDisplay\"?"
                }

                AlertDialog(
                    onDismissRequest = { pendingSubCategoryChange = null },
                    title = {
                        Text(
                            text = "Change Sub Category?",
                            color = LXTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = dialogMessage,
                            color = LXTextSecondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.updateSubCategory(targetName)
                                pendingSubCategoryChange = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LXPrimaryGreen,
                                contentColor = LXBackgroundBlack
                            )
                        ) {
                            Text("Change", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingSubCategoryChange = null }) {
                            Text("Cancel", color = LXTextSecondary)
                        }
                    },
                    containerColor = LXSurfaceElevated
                )
            }

            // Warning Dialog for Foreign Database ID / Source editing
            if (showForeignDbWarningDialog) {
                AlertDialog(
                    onDismissRequest = { showForeignDbWarningDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = LXScoreGold)
                            Text(
                                text = "Edit Foreign Database ID?",
                                color = LXTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Text(
                            text = "Modifying the foreign database ID or source may affect automated poster fetching and synchronization with external databases (such as TMDB or MyAnimeList). Are you sure you want to edit this?",
                            color = LXTextSecondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                isForeignDbUnlocked = true
                                showForeignDbWarningDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LXPrimaryGreen,
                                contentColor = LXBackgroundBlack
                            )
                        ) {
                            Text("Enable Editing", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showForeignDbWarningDialog = false }) {
                            Text("Cancel", color = LXTextSecondary)
                        }
                    },
                    containerColor = LXSurfaceElevated
                )
            }

            // Main Category Dropdown
            val allMainCategories = remember(customCategories, state.mediaType) {
                val list = mutableListOf<MainCategoryItem>()
                MediaType.entries.forEach { cat ->
                    list.add(MainCategoryItem(displayName = cat.displayName, mediaType = cat, customCategory = null))
                }
                customCategories.forEach { customCat ->
                    list.add(
                        MainCategoryItem(
                            displayName = customCat.name,
                            mediaType = customCat.mediaType ?: state.mediaType,
                            customCategory = customCat.name
                        )
                    )
                }
                list
            }
            val currentCategoryDisplayName = state.customCategory ?: state.mediaType.displayName

            ExposedDropdownMenuBox(
                expanded = mainCategoryExpanded,
                onExpandedChange = { mainCategoryExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentCategoryDisplayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Main Category") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = mainCategoryExpanded)
                    },
                    colors = outlinedColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = mainCategoryExpanded,
                    onDismissRequest = { mainCategoryExpanded = false },
                    modifier = Modifier.background(LXSurfaceElevated)
                ) {
                    allMainCategories.forEach { item ->
                        val isSelected = if (item.customCategory != null) {
                            state.customCategory.equals(item.customCategory, ignoreCase = true)
                        } else {
                            state.customCategory == null && state.mediaType == item.mediaType
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = item.displayName,
                                    color = if (isSelected) LXPrimaryGreen else LXTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                mainCategoryExpanded = false
                                if (!isSelected) {
                                    pendingMainCategory = item
                                }
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = LXPrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            // Sub Category (Optional) Dropdown
            val currentSubCategoryDisplayName = if (state.subCategory.isNullOrBlank()) "None" else state.subCategory!!

            ExposedDropdownMenuBox(
                expanded = subCategoryExpanded,
                onExpandedChange = { subCategoryExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentSubCategoryDisplayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sub Category (Optional)") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = subCategoryExpanded)
                    },
                    colors = outlinedColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = subCategoryExpanded,
                    onDismissRequest = { subCategoryExpanded = false },
                    modifier = Modifier.background(LXSurfaceElevated)
                ) {
                    // "None" option
                    val isNoneSelected = state.subCategory.isNullOrBlank()
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "None",
                                color = if (isNoneSelected) LXPrimaryGreen else LXTextPrimary,
                                fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            subCategoryExpanded = false
                            if (!isNoneSelected) {
                                pendingSubCategoryChange = PendingSubCategoryChange(null)
                            }
                        },
                        trailingIcon = if (isNoneSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null
                    )

                    // Sub categories from repository
                    subCategories.forEach { subCat ->
                        val isSelected = state.subCategory.equals(subCat.name, ignoreCase = true)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = subCat.name,
                                    color = if (isSelected) LXPrimaryGreen else LXTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                subCategoryExpanded = false
                                if (!isSelected) {
                                    pendingSubCategoryChange = PendingSubCategoryChange(subCat.name)
                                }
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = LXPrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null
                        )
                    }

                    // Existing sub category if currently set on the entry but not in repository subCategories list
                    if (!state.subCategory.isNullOrBlank() && subCategories.none { it.name.equals(state.subCategory, ignoreCase = true) }) {
                        val customSubName = state.subCategory!!
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = customSubName,
                                    color = LXPrimaryGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                subCategoryExpanded = false
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }

            if (subCategories.isEmpty() && state.subCategory.isNullOrBlank()) {
                Text(
                    text = "No sub categories available for this category. Sub categories can be created from the Home screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary
                )
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
    cursorColor = LXPrimaryGreen,
    focusedTrailingIconColor = LXPrimaryGreen,
    unfocusedTrailingIconColor = LXTextSecondary,
    errorBorderColor = LXErrorRed,
    errorLabelColor = LXErrorRed,
    errorTextColor = LXTextPrimary,
    errorCursorColor = LXErrorRed
)

private data class MainCategoryItem(
    val displayName: String,
    val mediaType: MediaType,
    val customCategory: String?
)

private data class PendingSubCategoryChange(
    val name: String?
)

@Composable
private fun AdaptiveDateRow(
    startLabel: String,
    startTimestamp: Long?,
    onStartDateSelected: (Long?) -> Unit,
    endLabel: String,
    endTimestamp: Long?,
    onEndDateSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 280.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DatePickerField(
                    label = startLabel,
                    timestamp = startTimestamp,
                    onDateSelected = onStartDateSelected,
                    modifier = Modifier.fillMaxWidth()
                )
                DatePickerField(
                    label = endLabel,
                    timestamp = endTimestamp,
                    onDateSelected = onEndDateSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DatePickerField(
                    label = startLabel,
                    timestamp = startTimestamp,
                    onDateSelected = onStartDateSelected,
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    label = endLabel,
                    timestamp = endTimestamp,
                    onDateSelected = onEndDateSelected,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DatePickerField(
    label: String,
    timestamp: Long?,
    onDateSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayValue = remember(timestamp) {
        timestamp?.let { sdf.format(Date(it)) } ?: ""
    }

    val calendar = remember(timestamp) {
        Calendar.getInstance().apply {
            if (timestamp != null && timestamp > 0L) {
                timeInMillis = timestamp
            }
        }
    }

    val showDialog = {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH)
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onDateSelected(selectedCal.timeInMillis)
            },
            y, m, d
        ).show()
    }

    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = showDialog),
        shape = RoundedCornerShape(8.dp),
        color = LXSurfaceElevated.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (timestamp != null) LXPrimaryGreen.copy(alpha = 0.6f) else LXBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = if (timestamp != null) LXPrimaryGreen else LXPrimaryGreen.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (timestamp != null) LXPrimaryGreen else LXTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = displayValue.ifEmpty { "Select date" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = if (timestamp != null) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (timestamp != null) LXTextPrimary else LXTextSecondary.copy(alpha = 0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (timestamp != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(LXBorder.copy(alpha = 0.6f))
                        .clickable(
                            role = androidx.compose.ui.semantics.Role.Button,
                            onClickLabel = "Clear Date",
                            onClick = { onDateSelected(null) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Date",
                        tint = LXTextPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
