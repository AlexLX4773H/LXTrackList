package com.lx.tracklist.ui.edit

import android.app.DatePickerDialog
import android.widget.Toast
import kotlin.math.roundToInt
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lx.tracklist.R
import com.lx.tracklist.data.remote.JikanMediaDetails
import com.lx.tracklist.data.remote.TmdbSearchResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.Priority
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.model.UserProfile
import com.lx.tracklist.ui.components.CoverImageSearchDialog
import com.lx.tracklist.ui.components.UserAvatarButton
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
    activeUser: UserProfile? = null,
    onUserAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExternalSyncDialog by remember { mutableStateOf(false) }
    var selectedExternalSource by remember { mutableStateOf(ExternalDbSource.MAL) }
    val context = LocalContext.current
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

    LaunchedEffect(syncState.successMessage) {
        val msg = syncState.successMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSyncMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LXBackgroundBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = LXPrimaryGreen
                            )
                        }
                        UserAvatarButton(
                            activeUser = activeUser,
                            onClick = onUserAvatarClick,
                            size = 32.dp,
                            paddingStart = 0.dp,
                            paddingEnd = 4.dp
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

            // Sync from External DB Option (shown once title is added)
            if (state.title.isNotBlank()) {
                var showSyncDropdown by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showSyncDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = LXSurfaceElevated.copy(alpha = 0.5f),
                            contentColor = LXPrimaryGreen
                        ),
                        border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = LXPrimaryGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sync from External DB",
                            color = LXPrimaryGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = LXPrimaryGreen
                        )
                    }

                    DropdownMenu(
                        expanded = showSyncDropdown,
                        onDismissRequest = { showSyncDropdown = false },
                        modifier = Modifier
                            .background(LXSurfaceElevated)
                            .border(1.dp, LXBorder, RoundedCornerShape(8.dp))
                    ) {
                        val showMal = true
                        val showMangaDex = state.mediaType.isReadType || (!state.mediaType.isWatchType)
                        val showTmdb = state.mediaType.isWatchType || (!state.mediaType.isReadType)

                        if (showMal) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("🎌", fontSize = 16.sp)
                                        Column {
                                            Text("MyAnimeList (MAL)", fontWeight = FontWeight.Bold, color = LXTextPrimary)
                                            Text("Anime, Manga, Manhwa, Light Novels", fontSize = 11.sp, color = LXTextSecondary)
                                        }
                                    }
                                },
                                onClick = {
                                    showSyncDropdown = false
                                    selectedExternalSource = ExternalDbSource.MAL
                                    viewModel.searchExternalDb(state.title, ExternalDbSource.MAL, context)
                                    showExternalSyncDialog = true
                                }
                            )
                        }

                        if (showMangaDex) {
                            if (showMal) {
                                HorizontalDivider(color = LXBorder.copy(alpha = 0.5f))
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("📖", fontSize = 16.sp)
                                        Column {
                                            Text("MangaDex", fontWeight = FontWeight.Bold, color = LXTextPrimary)
                                            Text("Manga, Manhwa, Manhua", fontSize = 11.sp, color = LXTextSecondary)
                                        }
                                    }
                                },
                                onClick = {
                                    showSyncDropdown = false
                                    selectedExternalSource = ExternalDbSource.MANGADEX
                                    viewModel.searchExternalDb(state.title, ExternalDbSource.MANGADEX, context)
                                    showExternalSyncDialog = true
                                }
                            )
                        }

                        if (showTmdb) {
                            if (showMal || showMangaDex) {
                                HorizontalDivider(color = LXBorder.copy(alpha = 0.5f))
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_tmdb_logo),
                                            contentDescription = "TMDB",
                                            modifier = Modifier.height(14.dp).width(20.dp)
                                        )
                                        Column {
                                            Text("The Movie Database (TMDB)", fontWeight = FontWeight.Bold, color = LXTextPrimary)
                                            Text("Movies & TV Shows", fontSize = 11.sp, color = LXTextSecondary)
                                        }
                                    }
                                },
                                onClick = {
                                    showSyncDropdown = false
                                    selectedExternalSource = ExternalDbSource.TMDB
                                    viewModel.searchExternalDb(state.title, ExternalDbSource.TMDB, context)
                                    showExternalSyncDialog = true
                                }
                            )
                        }
                    }
                }
            }

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
                            val hasSeasonTracking = state.mediaType == MediaType.SHOW && state.seasonEpisodes.any { it > 0 }
                            val curSeasonEps = if (hasSeasonTracking) {
                                state.seasonEpisodes.getOrElse(state.currentSeasons - 1) { 0 }
                            } else 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = if (state.currentEpisodes > 0) state.currentEpisodes.toString() else "",
                                    onValueChange = { viewModel.updateCurrentEpisodes(it.toIntOrNull() ?: 0) },
                                    label = { Text(if (hasSeasonTracking) "Season Ep" else "Watched Ep") },
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

                                var showSeasonEditor by remember { mutableStateOf(false) }
                                val displaySeasonCount = state.totalSeasons.coerceIn(1, 200)
                                val trackedSeasonEpsSum = (0 until displaySeasonCount).sumOf { state.seasonEpisodes.getOrElse(it) { 0 } }
                                val seasonSummaryText = if (hasSeasonTracking) {
                                    val curStr = if (curSeasonEps > 0) "S${state.currentSeasons}: $curSeasonEps eps • " else ""
                                    "${curStr}${displaySeasonCount} seasons ($trackedSeasonEpsSum eps total)"
                                } else {
                                    "Tap to set episodes per season (auto-advances season)"
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = LXSurfaceElevated,
                                    border = BorderStroke(1.dp, LXBorder.copy(alpha = 0.7f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateContentSize()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showSeasonEditor = !showSeasonEditor },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Season-by-Season Episodes",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                                    color = if (hasSeasonTracking) LXPrimaryGreen else LXTextPrimary
                                                )
                                                Text(
                                                    text = seasonSummaryText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = LXTextSecondary
                                                )
                                            }
                                            Icon(
                                                imageVector = if (showSeasonEditor) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = if (showSeasonEditor) "Collapse seasons" else "Expand seasons",
                                                tint = LXTextSecondary
                                            )
                                        }

                                        if (showSeasonEditor) {
                                            HorizontalDivider(color = LXBorder.copy(alpha = 0.5f))
                                            Text(
                                                text = "When you finish a season's last episode and tap +1, it advances to the next season at Ep 1.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LXTextSecondary
                                            )
                                            val seasonIndices = (0 until displaySeasonCount).toList()
                                            seasonIndices.chunked(3).forEach { rowIndices ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    rowIndices.forEach { sIdx ->
                                                        val epVal = state.seasonEpisodes.getOrElse(sIdx) { 0 }
                                                        val isCurSeason = (sIdx + 1) == state.currentSeasons
                                                        OutlinedTextField(
                                                            value = if (epVal > 0) epVal.toString() else "",
                                                            onValueChange = { txt ->
                                                                viewModel.updateSeasonEpisodeCount(sIdx, txt.toIntOrNull() ?: 0)
                                                            },
                                                            label = {
                                                                Text(
                                                                    text = "S${sIdx + 1} Eps",
                                                                    color = if (isCurSeason) LXPrimaryGreen else LXTextSecondary
                                                                )
                                                            },
                                                            placeholder = { Text("?") },
                                                            singleLine = true,
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            modifier = Modifier.weight(1f),
                                                            colors = outlinedColors()
                                                        )
                                                    }
                                                    repeat(3 - rowIndices.size) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                            if (state.totalSeasons > 200) {
                                                Text(
                                                    text = "Showing first 200 seasons.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = LXTextSecondary
                                                )
                                            }
                                        }
                                    }
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

                            CurrentPlaybackTimeSection(
                                currentTimeMinutes = state.currentTimeMinutes,
                                runtimeMinutes = state.episodeLengthMinutes,
                                onUpdateTime = { viewModel.updateCurrentTimeMinutes(it) }
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

                            CurrentPlaybackTimeSection(
                                currentTimeMinutes = state.currentTimeMinutes,
                                runtimeMinutes = state.durationMinutes,
                                onUpdateTime = { viewModel.updateCurrentTimeMinutes(it) }
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
                                    val list = mutableListOf("MAL", "MangaDex", "AniList", "Kitsu", "TMDB", "IMDb", "TVDb", "Tachiyomi")
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
                                    val list = mutableListOf("MAL", "MangaDex", "AniList", "Kitsu", "TMDB", "IMDb", "Rotten Tomatoes", "Metacritic")
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
                            value = state.externalScore?.let { formatScore(it) } ?: "",
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

            if (showExternalSyncDialog) {
                ExternalDbSyncDialog(
                    isOpen = showExternalSyncDialog,
                    onDismiss = { showExternalSyncDialog = false },
                    syncState = syncState,
                    initialQuery = state.title,
                    mediaType = state.mediaType,
                    onSearch = { query, source -> viewModel.searchExternalDb(query, source, context) },
                    onSelectMal = { item ->
                        viewModel.applySelectedMalResult(item)
                        showExternalSyncDialog = false
                    },
                    onSelectTmdb = { item ->
                        viewModel.applySelectedTmdbResult(item, context)
                        showExternalSyncDialog = false
                    }
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

            // External Tracking Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.updateIsExternallyTracked(!state.isExternallyTracked) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = if (state.isExternallyTracked) LXPrimaryGreen else LXTextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("External Tracking", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (state.isExternallyTracked) "Live sync enabled for linked MyAnimeList ID" else "Sync disabled (local only)",
                        color = LXTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExternalDbSyncDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    syncState: ExternalSyncUiState,
    initialQuery: String,
    mediaType: MediaType = MediaType.ANIME,
    onSearch: (String, ExternalDbSource) -> Unit,
    onSelectMal: (JikanMediaDetails) -> Unit,
    onSelectTmdb: (TmdbSearchResult) -> Unit
) {
    if (!isOpen) return

    var searchInput by remember(initialQuery) { mutableStateOf(initialQuery) }
    var expandedItemKey by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    fun openExternalUrl(url: String?) {
        if (url.isNullOrBlank()) return
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
        }
    }

    val availableSources = remember(mediaType, syncState.selectedSource) {
        val base = when {
            mediaType.isReadType -> listOf(ExternalDbSource.MAL, ExternalDbSource.MANGADEX)
            mediaType == MediaType.MOVIE || mediaType == MediaType.SHOW -> listOf(ExternalDbSource.TMDB, ExternalDbSource.MAL)
            mediaType == MediaType.ANIME -> listOf(ExternalDbSource.MAL, ExternalDbSource.TMDB)
            else -> listOf(ExternalDbSource.MAL, ExternalDbSource.MANGADEX, ExternalDbSource.TMDB)
        }
        if (syncState.selectedSource !in base) {
            base + syncState.selectedSource
        } else {
            base
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = LXPrimaryGreen)
                    Text(
                        text = "Sync from External DB",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = LXTextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search input
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    label = { Text("Search Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        expandedItemKey = null
                        onSearch(searchInput, syncState.selectedSource)
                    }),
                    trailingIcon = {
                        IconButton(onClick = {
                            expandedItemKey = null
                            onSearch(searchInput, syncState.selectedSource)
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = LXPrimaryGreen)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LXPrimaryGreen,
                        unfocusedBorderColor = LXBorder,
                        focusedLabelColor = LXPrimaryGreen,
                        unfocusedLabelColor = LXTextSecondary,
                        focusedTextColor = LXTextPrimary,
                        unfocusedTextColor = LXTextPrimary,
                        cursorColor = LXPrimaryGreen
                    )
                )

                // Source Choice Chips: Filtered by mediaType, horizontally scrollable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    availableSources.forEach { source ->
                        when (source) {
                            ExternalDbSource.MAL -> {
                                FilterChip(
                                    selected = syncState.selectedSource == ExternalDbSource.MAL,
                                    onClick = {
                                        if (syncState.selectedSource != ExternalDbSource.MAL) {
                                            expandedItemKey = null
                                            onSearch(searchInput, ExternalDbSource.MAL)
                                        }
                                    },
                                    label = { Text("🎌 MAL", fontSize = 12.sp, maxLines = 1, softWrap = false) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LXPrimaryGreen,
                                        selectedLabelColor = LXBackgroundBlack,
                                        containerColor = LXSurfaceElevated,
                                        labelColor = LXTextSecondary
                                    )
                                )
                            }
                            ExternalDbSource.MANGADEX -> {
                                FilterChip(
                                    selected = syncState.selectedSource == ExternalDbSource.MANGADEX,
                                    onClick = {
                                        if (syncState.selectedSource != ExternalDbSource.MANGADEX) {
                                            expandedItemKey = null
                                            onSearch(searchInput, ExternalDbSource.MANGADEX)
                                        }
                                    },
                                    label = { Text("📖 MangaDex", fontSize = 12.sp, maxLines = 1, softWrap = false) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LXPrimaryGreen,
                                        selectedLabelColor = LXBackgroundBlack,
                                        containerColor = LXSurfaceElevated,
                                        labelColor = LXTextSecondary
                                    )
                                )
                            }
                            ExternalDbSource.TMDB -> {
                                FilterChip(
                                    selected = syncState.selectedSource == ExternalDbSource.TMDB,
                                    onClick = {
                                        if (syncState.selectedSource != ExternalDbSource.TMDB) {
                                            expandedItemKey = null
                                            onSearch(searchInput, ExternalDbSource.TMDB)
                                        }
                                    },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_tmdb_logo),
                                                contentDescription = "TMDB",
                                                modifier = Modifier.height(12.dp).width(18.dp)
                                            )
                                            Text("TMDB", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LXPrimaryGreen,
                                        selectedLabelColor = LXBackgroundBlack,
                                        containerColor = LXSurfaceElevated,
                                        labelColor = LXTextSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                // Results Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    if (syncState.isSearching) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = LXPrimaryGreen, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Searching ${syncState.selectedSource.shortName} for '$searchInput'...",
                                color = LXTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    } else if (syncState.errorMessage != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = syncState.errorMessage,
                                color = LXTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    } else if (syncState.selectedSource == ExternalDbSource.MAL || syncState.selectedSource == ExternalDbSource.MANGADEX) {
                        if (syncState.malResults.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Enter a title and tap search.", color = LXTextSecondary, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(syncState.malResults, key = { "${it.sourceProvider}_${it.malId ?: it.url}_${it.title.hashCode()}" }) { item ->
                                    val itemKey = "${item.sourceProvider}_${item.malId ?: item.url}_${item.title}"
                                    val isExpanded = expandedItemKey == itemKey
                                    val webUrl = item.webUrl

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateContentSize(),
                                        colors = CardDefaults.cardColors(containerColor = LXSurfaceElevated),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (isExpanded) LXPrimaryGreen.copy(alpha = 0.6f) else LXBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Header row: Clickable to expand/collapse
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { expandedItemKey = if (isExpanded) null else itemKey },
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (!item.posterUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = item.posterUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(width = 46.dp, height = 64.dp)
                                                            .clip(RoundedCornerShape(4.dp)),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(width = 46.dp, height = 64.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(LXBackgroundBlack),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Image, contentDescription = null, tint = LXTextSecondary.copy(alpha = 0.5f))
                                                    }
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title ?: "Unknown",
                                                        fontWeight = FontWeight.Bold,
                                                        color = LXTextPrimary,
                                                        fontSize = 13.sp,
                                                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (!item.alternateTitle.isNullOrBlank()) {
                                                        Text(
                                                            text = item.alternateTitle.replace("\n", " • "),
                                                            color = LXTextSecondary,
                                                            fontSize = 11.sp,
                                                            maxLines = if (isExpanded) 3 else 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    FlowRow(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        val formattedScore = formatScore(item.externalScore)
                                                        if (formattedScore != null) {
                                                            Text(
                                                                text = "★ $formattedScore",
                                                                color = LXScoreGold,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        }
                                                        if (item.totalEpisodes != null && item.totalEpisodes > 0) {
                                                            Text(
                                                                text = "${item.totalEpisodes} eps",
                                                                color = LXTextSecondary,
                                                                fontSize = 11.sp,
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        } else if (item.totalChapters != null && item.totalChapters > 0) {
                                                            Text(
                                                                text = "${item.totalChapters} ch",
                                                                color = LXTextSecondary,
                                                                fontSize = 11.sp,
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        }
                                                        if (!item.type.isNullOrBlank()) {
                                                            Surface(
                                                                color = LXPrimaryGreen.copy(alpha = 0.15f),
                                                                shape = RoundedCornerShape(4.dp)
                                                            ) {
                                                                Text(
                                                                    text = item.type,
                                                                    color = LXPrimaryGreen,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    maxLines = 1,
                                                                    softWrap = false,
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                                )
                                                            }
                                                        }
                                                        val provLabel = item.sourceProvider
                                                        Text(
                                                            text = "($provLabel)",
                                                            color = LXTextSecondary.copy(alpha = 0.6f),
                                                            fontSize = 10.sp,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }

                                                Column(
                                                    horizontalAlignment = Alignment.End,
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Button(
                                                        onClick = { onSelectMal(item) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = LXPrimaryGreen,
                                                            contentColor = LXBackgroundBlack
                                                        ),
                                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        if (webUrl != null) {
                                                            IconButton(
                                                                onClick = { openExternalUrl(webUrl) },
                                                                modifier = Modifier.size(26.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                                    contentDescription = "Open in MyAnimeList",
                                                                    tint = LXTextSecondary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = { expandedItemKey = if (isExpanded) null else itemKey },
                                                            modifier = Modifier.size(26.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                                contentDescription = if (isExpanded) "Collapse" else "Expand details",
                                                                tint = LXPrimaryGreen,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Expanded details
                                            if (isExpanded) {
                                                HorizontalDivider(color = LXBorder.copy(alpha = 0.6f))

                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (!item.alternateTitle.isNullOrBlank()) {
                                                        Text(
                                                            text = "Alternative Titles:",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = LXTextSecondary
                                                        )
                                                        Text(
                                                            text = item.alternateTitle.lines().filter { it.isNotBlank() }.joinToString("\n• ", prefix = "• "),
                                                            fontSize = 11.sp,
                                                            color = LXTextPrimary.copy(alpha = 0.9f)
                                                        )
                                                    }

                                                    val dateRange = formatDateRange(item.airedStartDate, item.airedEndDate)
                                                    if (dateRange != null) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Text("Aired:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LXTextSecondary)
                                                            Text(dateRange, fontSize = 11.sp, color = LXTextPrimary)
                                                        }
                                                    }

                                                    if (item.isAiring != null) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Text("Status:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LXTextSecondary)
                                                            val isWatch = mediaType.isWatchType
                                                            val ongoingText = if (isWatch) "Currently Airing" else "Publishing"
                                                            Text(
                                                                text = if (item.isAiring) ongoingText else "Finished",
                                                                fontSize = 11.sp,
                                                                color = if (item.isAiring) LXPrimaryGreen else LXTextPrimary
                                                            )
                                                        }
                                                    }

                                                    if (item.totalVolumes != null && item.totalVolumes > 0) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Text("Volumes:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LXTextSecondary)
                                                            Text("${item.totalVolumes} vol", fontSize = 11.sp, color = LXTextPrimary)
                                                        }
                                                    }

                                                    if (item.durationMinutes != null && item.durationMinutes > 0) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Text("Duration:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LXTextSecondary)
                                                            Text("${item.durationMinutes} min/ep", fontSize = 11.sp, color = LXTextPrimary)
                                                        }
                                                    }

                                                    if (!item.tags.isNullOrBlank()) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Text("Genres:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LXTextSecondary)
                                                            Text(item.tags, fontSize = 11.sp, color = LXTextPrimary)
                                                        }
                                                    }

                                                    if (!item.synopsis.isNullOrBlank()) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(LXBackgroundBlack, RoundedCornerShape(6.dp))
                                                                .padding(8.dp)
                        ) {
                            Text(
                                                                text = "Synopsis",
                                                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LXPrimaryGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                                                text = item.synopsis,
                                                                fontSize = 11.sp,
                                                                color = LXTextPrimary.copy(alpha = 0.9f),
                                                                lineHeight = 16.sp
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(2.dp))

                                                    // Bottom expanded actions
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (webUrl != null) {
                                                            OutlinedButton(
                                                                onClick = { openExternalUrl(webUrl) },
                                                                modifier = Modifier
                                                                    .weight(1.35f)
                                                                    .heightIn(min = 34.dp),
                                                                shape = RoundedCornerShape(6.dp),
                                                                border = BorderStroke(1.dp, LXBorder),
                                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LXPrimaryGreen),
                                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                                            ) {
                                                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                val openLabel = if (item.sourceProvider == "MangaDex") "Open MangaDex" else if (item.sourceProvider.startsWith("AniList")) "Open AniList" else if (item.sourceProvider == "Kitsu") "Open Kitsu" else "Open MAL"
                                                                Text(
                                                                    text = openLabel,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    maxLines = 1,
                                                                    softWrap = false,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }

                                                        Button(
                                                            onClick = { onSelectMal(item) },
                                                            modifier = Modifier
                                                                .weight(if (webUrl != null) 0.9f else 1f)
                                                                .heightIn(min = 34.dp),
                                                            shape = RoundedCornerShape(6.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = LXPrimaryGreen,
                                                                contentColor = LXBackgroundBlack
                                                            ),
                                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                                        ) {
                                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // TMDB results
                        if (syncState.tmdbResults.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Enter a title and tap search.", color = LXTextSecondary, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(syncState.tmdbResults) { item ->
                                    val itemKey = "tmdb_${item.tmdbId}_${item.title}"
                                    val isExpanded = expandedItemKey == itemKey
                                    val webUrl = item.webUrl

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateContentSize(),
                                        colors = CardDefaults.cardColors(containerColor = LXSurfaceElevated),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (isExpanded) LXPrimaryGreen.copy(alpha = 0.6f) else LXBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Header row: Clickable to expand/collapse
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { expandedItemKey = if (isExpanded) null else itemKey },
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (!item.posterUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = item.posterUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(width = 46.dp, height = 64.dp)
                                                            .clip(RoundedCornerShape(4.dp)),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(width = 46.dp, height = 64.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(LXBackgroundBlack),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Image, contentDescription = null, tint = LXTextSecondary.copy(alpha = 0.5f))
                                                    }
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        fontWeight = FontWeight.Bold,
                                                        color = LXTextPrimary,
                                                        fontSize = 13.sp,
                                                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (!item.originalTitle.isNullOrBlank() && item.originalTitle != item.title) {
                                                        Text(
                                                            text = item.originalTitle,
                                                            color = LXTextSecondary,
                                                            fontSize = 11.sp,
                                                            maxLines = if (isExpanded) 3 else 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    FlowRow(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        val formattedScore = formatScore(item.voteAverage?.toFloat())
                                                        if (formattedScore != null) {
                                                            Text(
                                                                text = "★ $formattedScore",
                                                                color = LXScoreGold,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        }
                                                        if (!item.releaseYear.isNullOrBlank()) {
                                                            Text(
                                                                text = item.releaseYear,
                                                                color = LXTextSecondary,
                                                                fontSize = 11.sp,
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        }
                                                        Surface(
                                                            color = LXPrimaryGreen.copy(alpha = 0.15f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = if (item.isMovie) "Movie" else "TV Show",
                                                                color = LXPrimaryGreen,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Column(
                                                    horizontalAlignment = Alignment.End,
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Button(
                                                        onClick = { onSelectTmdb(item) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = LXPrimaryGreen,
                                                            contentColor = LXBackgroundBlack
                                                        ),
                                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        IconButton(
                                                            onClick = { openExternalUrl(webUrl) },
                                                            modifier = Modifier.size(26.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                                contentDescription = "Open on TMDB",
                                                                tint = LXTextSecondary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = { expandedItemKey = if (isExpanded) null else itemKey },
                                                            modifier = Modifier.size(26.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                                contentDescription = if (isExpanded) "Collapse" else "Expand details",
                                                                tint = LXPrimaryGreen,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Expanded details
                                            if (isExpanded) {
                                                HorizontalDivider(color = LXBorder.copy(alpha = 0.6f))

                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (!item.originalTitle.isNullOrBlank() && item.originalTitle != item.title) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Text("Original Title:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LXTextSecondary)
                                                            Text(item.originalTitle, fontSize = 11.sp, color = LXTextPrimary)
                                                        }
                                                    }

                                                    if (!item.releaseYear.isNullOrBlank()) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Text("Release Year:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LXTextSecondary)
                                                            Text(item.releaseYear, fontSize = 11.sp, color = LXTextPrimary)
                                                        }
                                                    }

                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text("Type:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LXTextSecondary)
                                                        Text(if (item.isMovie) "Movie" else "TV Show", fontSize = 11.sp, color = LXPrimaryGreen)
                                                    }

                                                    if (!item.overview.isNullOrBlank()) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(LXBackgroundBlack, RoundedCornerShape(6.dp))
                                                                .padding(8.dp)
                                                        ) {
                                                            Text(
                                                                text = "Overview",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = LXPrimaryGreen
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = item.overview,
                                                                fontSize = 11.sp,
                                                                color = LXTextPrimary.copy(alpha = 0.9f),
                                                                lineHeight = 16.sp
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(2.dp))

                                                    // Bottom expanded actions
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        OutlinedButton(
                                                            onClick = { openExternalUrl(webUrl) },
                                                            modifier = Modifier
                                                                .weight(1.35f)
                                                                .heightIn(min = 34.dp),
                                                            shape = RoundedCornerShape(6.dp),
                                                            border = BorderStroke(1.dp, LXBorder),
                                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LXPrimaryGreen),
                                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                                        ) {
                                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Open TMDB",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }

                                                        Button(
                                                            onClick = { onSelectTmdb(item) },
                                                            modifier = Modifier
                                                                .weight(0.9f)
                                                                .heightIn(min = 34.dp),
                                                            shape = RoundedCornerShape(6.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = LXPrimaryGreen,
                                                                contentColor = LXBackgroundBlack
                                                            ),
                                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                                        ) {
                                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
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
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LXTextSecondary)
            }
        },
        containerColor = LXSurfaceBlack,
        shape = RoundedCornerShape(12.dp)
    )
}

private fun formatDateRange(startDate: Long?, endDate: Long?): String? {
    if (startDate == null && endDate == null) return null
    val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    return when {
        startDate != null && endDate != null -> "${sdf.format(Date(startDate))} - ${sdf.format(Date(endDate))}"
        startDate != null -> "Started ${sdf.format(Date(startDate))}"
        endDate != null -> "Ended ${sdf.format(Date(endDate))}"
        else -> null
    }
}

private fun formatScore(score: Float?): String? {
    if (score == null || score <= 0f) return null
    return if (score % 1.0f == 0.0f) {
        score.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", score).trimEnd('0').trimEnd('.')
    }
}

@Composable
private fun CurrentPlaybackTimeSection(
    currentTimeMinutes: Int?,
    runtimeMinutes: Int?,
    onUpdateTime: (Int?) -> Unit
) {
    var rawInput by remember(currentTimeMinutes) {
        mutableStateOf(if (currentTimeMinutes != null && currentTimeMinutes > 0) currentTimeMinutes.toString() else "")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LXSurfaceElevated)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Current Watch Time (Optional)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = LXTextPrimary
            )
            if (currentTimeMinutes != null && currentTimeMinutes > 0) {
                val h = currentTimeMinutes / 60
                val m = currentTimeMinutes % 60
                val formatted = if (h > 0) "${h}h ${m}m" else "${m}m"
                val pctStr = if (runtimeMinutes != null && runtimeMinutes > 0) {
                    val pct = ((currentTimeMinutes.toDouble() / runtimeMinutes.toDouble()) * 100).toInt().coerceIn(0, 100)
                    " • $pct%"
                } else ""
                Text(
                    text = "$formatted$pctStr",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = LXPrimaryGreen
                )
            }
        }

        Text(
            text = "Track how much of the current episode or movie you have seen (e.g. 1/4, 1/2, or specific minutes).",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = LXTextSecondary
        )

        OutlinedTextField(
            value = rawInput,
            onValueChange = { input ->
                rawInput = input
                val parsedMinutes = parseTimeToMinutes(input)
                onUpdateTime(parsedMinutes)
            },
            label = { Text("Watch Progress (minutes or mm:ss)") },
            placeholder = { Text("e.g. 12 or 12:30 or 1h 15m") },
            modifier = Modifier.fillMaxWidth(),
            colors = outlinedColors(),
            singleLine = true
        )

        // Quick fractional presets if runtime is known
        if (runtimeMinutes != null && runtimeMinutes > 0) {
            val q1 = (runtimeMinutes * 0.25).roundToInt().coerceAtLeast(1)
            val q2 = (runtimeMinutes * 0.50).roundToInt().coerceAtLeast(1)
            val q3 = (runtimeMinutes * 0.75).roundToInt().coerceAtLeast(1)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "1/4 (${q1}m)" to q1,
                    "1/2 (${q2}m)" to q2,
                    "3/4 (${q3}m)" to q3
                ).forEach { (label, minVal) ->
                    val isSelected = currentTimeMinutes == minVal
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                rawInput = ""
                                onUpdateTime(null)
                            } else {
                                rawInput = minVal.toString()
                                onUpdateTime(minVal)
                            }
                        },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen,
                            selectedLabelColor = LXBackgroundBlack,
                            containerColor = LXSurfaceBlack,
                            labelColor = LXTextSecondary
                        )
                    )
                }

                if (currentTimeMinutes != null && currentTimeMinutes > 0) {
                    TextButton(
                        onClick = {
                            rawInput = ""
                            onUpdateTime(null)
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("Clear", color = LXErrorRed, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun parseTimeToMinutes(input: String): Int? {
    val clean = input.trim().lowercase()
    if (clean.isBlank()) return null

    val hrMatch = Regex("(\\d+)\\s*(?:h|hr|hour)").find(clean)
    val minMatch = Regex("(\\d+)\\s*(?:m|min|minute)").find(clean)
    if (hrMatch != null || minMatch != null) {
        val hrs = hrMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val mins = minMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val total = hrs * 60 + mins
        return if (total > 0) total else null
    }

    if (clean.contains(":")) {
        val parts = clean.split(":")
        if (parts.size == 2) {
            val m = parts[0].trim().toIntOrNull() ?: 0
            val s = parts[1].trim().toIntOrNull() ?: 0
            val total = m + if (s >= 30) 1 else 0
            return if (total > 0) total else null
        } else if (parts.size == 3) {
            val h = parts[0].trim().toIntOrNull() ?: 0
            val m = parts[1].trim().toIntOrNull() ?: 0
            val s = parts[2].trim().toIntOrNull() ?: 0
            val total = h * 60 + m + if (s >= 30) 1 else 0
            return if (total > 0) total else null
        }
    }

    val direct = clean.filter { it.isDigit() }.toIntOrNull()
    return if (direct != null && direct > 0) direct else null
}
