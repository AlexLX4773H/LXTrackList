package com.lx.tracklist.ui.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.RelatedMediaItem
import com.lx.tracklist.data.model.UserProfile
import com.lx.tracklist.ui.components.AddRelatedToLibraryDialog
import com.lx.tracklist.ui.components.MediaCard
import com.lx.tracklist.ui.components.UserAvatarButton
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXErrorRed
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXScoreGold
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXSurfaceVariant
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onEntryClick: (Long) -> Unit,
    onEditEntry: (Long) -> Unit,
    onAddNewEntry: ((mediaType: MediaType, customCategory: String?, subCategory: String?, status: MediaStatus) -> Unit)? = null,
    activeUser: UserProfile? = null,
    onUserAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val isTablet = configuration.screenWidthDp >= 600
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCategoryBatchDialog by remember { mutableStateOf(false) }
    var showSubCategoryBatchDialog by remember { mutableStateOf(false) }
    var showStatusBatchDialog by remember { mutableStateOf(false) }
    var showFavoriteBatchDialog by remember { mutableStateOf(false) }
    var showExternalTrackingBatchDialog by remember { mutableStateOf(false) }
    var showExternalDbBatchDialog by remember { mutableStateOf(false) }
    var showSelectionOverflowMenu by remember { mutableStateOf(false) }
    var relatedItemToAdd by remember { mutableStateOf<RelatedMediaItem?>(null) }
    var isAddingRelated by remember { mutableStateOf(false) }
    var entryToChangeStatusOnIncrement by remember { mutableStateOf<MediaEntry?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val ensureNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val visibleIds = remember(state.entries) { state.entries.map { it.id } }

    // Proactively validate image URLs across all entries when Broken Cover filter is engaged or filter sheet is opened
    LaunchedEffect(state.coverImageFilter, state.isFilterSheetOpen) {
        if (state.coverImageFilter == CoverImageFilterOption.BROKEN_COVER || state.isFilterSheetOpen) {
            viewModel.validateCovers(context)
        }
    }

    // Intercept back button when in selection mode or filter sheet is open
    BackHandler(enabled = state.isSelectionMode || state.isFilterSheetOpen) {
        if (state.isFilterSheetOpen) {
            viewModel.setFilterSheetOpen(false)
        } else if (state.isSelectionMode) {
            viewModel.clearSelection()
        }
    }

    // Bulk Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete ${state.selectedIds.size} ${if (state.selectedIds.size == 1) "entry" else "entries"}?",
                    color = LXTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently remove the selected entries from your library. This action cannot be undone.",
                    color = LXTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelected()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LXErrorRed)
                ) {
                    Text("Delete All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceElevated
        )
    }

    // Change Category Batch Dialog
    if (showCategoryBatchDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryBatchDialog = false },
            title = {
                Text(
                    text = "Change Category (${state.selectedIds.size} selected)",
                    color = LXTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Standard Categories", style = MaterialTheme.typography.labelMedium, color = LXTextSecondary)
                        MediaType.entries.forEach { mediaType ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LXSurfaceBlack)
                                    .clickable {
                                        viewModel.batchUpdateCategory(mediaType, null)
                                        showCategoryBatchDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(mediaType.displayName, color = LXTextPrimary, fontWeight = FontWeight.Medium)
                            }
                        }

                        if (state.customCategories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Custom Categories", style = MaterialTheme.typography.labelMedium, color = LXTextSecondary)
                            state.customCategories.forEach { customCat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(LXSurfaceBlack)
                                        .clickable {
                                            val targetType = customCat.mediaType ?: if (customCat.isWatchType) MediaType.ANIME else MediaType.MANGA
                                            viewModel.batchUpdateCategory(targetType, customCat.name)
                                            showCategoryBatchDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(customCat.name, color = LXPrimaryGreen, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCategoryBatchDialog = false }) {
                    Text("Cancel", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceElevated
        )
    }

    // Set Sub Category Batch Dialog
    if (showSubCategoryBatchDialog) {
        val distinctSubCats = remember(state.subCategories) {
            state.subCategories.distinctBy { it.name.lowercase() }
        }
        AlertDialog(
            onDismissRequest = { showSubCategoryBatchDialog = false },
            title = {
                Text(
                    text = "Set Sub Category (${state.selectedIds.size} selected)",
                    color = LXTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LXSurfaceBlack)
                                .clickable {
                                    viewModel.batchUpdateSubCategory(null)
                                    showSubCategoryBatchDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("None (Clear Sub Category)", color = LXErrorRed, fontWeight = FontWeight.Medium)
                        }

                        if (distinctSubCats.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No sub categories found. Sub categories can be created from the Home screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextSecondary
                            )
                        } else {
                            distinctSubCats.forEach { subCat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(LXSurfaceBlack)
                                        .clickable {
                                            viewModel.batchUpdateSubCategory(subCat.name)
                                            showSubCategoryBatchDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(subCat.name, color = LXTextPrimary, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSubCategoryBatchDialog = false }) {
                    Text("Cancel", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceElevated
        )
    }

    // Set Status Batch Dialog
    if (showStatusBatchDialog) {
        AlertDialog(
            onDismissRequest = { showStatusBatchDialog = false },
            title = {
                Text(
                    text = "Set Status (${state.selectedIds.size} selected)",
                    color = LXTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MediaStatus.entries.forEach { st ->
                        val statusLabel = when (st) {
                            MediaStatus.WATCHING -> "Watching / Reading"
                            MediaStatus.PLAN_TO_WATCH -> "Plan to Watch / Read"
                            MediaStatus.COMPLETED -> "Completed"
                            MediaStatus.ON_HOLD -> "On Hold"
                            MediaStatus.DROPPED -> "Dropped"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LXSurfaceBlack)
                                .clickable {
                                    viewModel.batchUpdateStatus(st)
                                    showStatusBatchDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(statusLabel, color = LXTextPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStatusBatchDialog = false }) {
                    Text("Cancel", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceElevated
        )
    }

    // Set Favorite Batch Dialog
    if (showFavoriteBatchDialog) {
        AlertDialog(
            onDismissRequest = { showFavoriteBatchDialog = false },
            title = {
                Text(
                    text = "Favorite (${state.selectedIds.size} selected)",
                    color = LXTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.batchUpdateFavorite(true)
                            showFavoriteBatchDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark as Favorite", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.batchUpdateFavorite(false)
                            showFavoriteBatchDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LXSurfaceVariant, contentColor = LXTextPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove from Favorites", fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFavoriteBatchDialog = false }) {
                    Text("Cancel", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceElevated
        )
    }

    // External Tracking Selected Entries Dialog
    if (showExternalTrackingBatchDialog) {
        AlertDialog(
            onDismissRequest = { showExternalTrackingBatchDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = LXPrimaryGreen)
                    Text(
                        text = "External Tracking (${state.selectedIds.size} selected)",
                        color = LXTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Enable or disable syncing to external trackers (e.g. MyAnimeList) for the selected ${state.selectedIds.size} ${if (state.selectedIds.size == 1) "entry" else "entries"}. Only entries with a valid MAL ID will push updates.",
                        color = LXTextSecondary,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = {
                            viewModel.batchUpdateExternalTracking(true)
                            showExternalTrackingBatchDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to External Tracking", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.batchUpdateExternalTracking(false)
                            showExternalTrackingBatchDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LXSurfaceVariant, contentColor = LXTextPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove from External Tracking", fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExternalTrackingBatchDialog = false }) {
                    Text("Cancel", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceElevated
        )
    }

    val targetIncrementEntry = entryToChangeStatusOnIncrement
    if (targetIncrementEntry != null) {
        val statusLabel = if (targetIncrementEntry.mediaType.isReadType) "Reading" else "Watching"
        val planLabel = if (targetIncrementEntry.mediaType.isReadType) "Plan to Read" else "Plan to Watch"

        AlertDialog(
            onDismissRequest = { entryToChangeStatusOnIncrement = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = LXPrimaryGreen)
                    Text(
                        text = "Change Status to $statusLabel?",
                        color = LXTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "\"${targetIncrementEntry.title}\" is currently in $planLabel. Would you like to change its status to $statusLabel?",
                        color = LXTextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Adding progress indicates you have started this title.",
                        color = LXTextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toUpdate = targetIncrementEntry
                        entryToChangeStatusOnIncrement = null
                        viewModel.incrementProgress(toUpdate, andChangeStatusTo = MediaStatus.WATCHING)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LXPrimaryGreen,
                        contentColor = LXBackgroundBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Change to $statusLabel", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { entryToChangeStatusOnIncrement = null }) {
                        Text("Cancel", color = LXTextSecondary)
                    }
                    TextButton(onClick = {
                        val toUpdate = targetIncrementEntry
                        entryToChangeStatusOnIncrement = null
                        viewModel.incrementProgress(toUpdate)
                    }) {
                        Text("Keep as $planLabel", color = LXTextPrimary)
                    }
                }
            },
            containerColor = LXSurfaceElevated,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // External DB Sync Selected Entries Dialog
    if (showExternalDbBatchDialog) {
        AlertDialog(
            onDismissRequest = { showExternalDbBatchDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = LXPrimaryGreen)
                    Text(
                        text = "External DB Sync (${state.selectedIds.size} selected)",
                        color = LXTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Search and update posters, runtimes, broadcast dates, season/episode counts, and metadata for the selected ${state.selectedIds.size} entries using MyAnimeList (Jikan) and The Movie Database (TMDB).",
                        color = LXTextSecondary
                    )
                    Text(
                        text = "Anime, Manga, and Manhwa entries are synced with Jikan (MAL). Movies and TV Shows are synced with TMDB (or MAL if previously linked to MAL).",
                        color = LXTextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Tap 'Resolve IDs' to find and link official IDs (MAL, MangaDex, AniList, Kitsu, TMDB) with confident exact title matching and audit reports.",
                        color = LXScoreGold.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Existing external IDs will be used directly. Otherwise, it searches by title.",
                        color = LXTextSecondary.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                ensureNotificationPermission()
                                viewModel.syncSelectedWithExternalDb(context, forceRefresh = false)
                                showExternalDbBatchDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LXPrimaryGreen,
                                contentColor = LXBackgroundBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fetch Missing", fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                ensureNotificationPermission()
                                viewModel.syncSelectedWithExternalDb(context, forceRefresh = true)
                                showExternalDbBatchDialog = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LXPrimaryGreen),
                            border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Re-fetch All", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    Button(
                        onClick = {
                            ensureNotificationPermission()
                            viewModel.resolveIdsSelected(context)
                            showExternalDbBatchDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LXScoreGold,
                            contentColor = LXBackgroundBlack
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resolve IDs", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = { showExternalDbBatchDialog = false }
                    ) {
                        Text("Cancel", color = LXTextSecondary)
                    }
                }
            },
            dismissButton = null,
            containerColor = LXSurfaceElevated
        )
    }

    // Add Related Media Dialog
    if (relatedItemToAdd != null) {
        relatedItemToAdd?.let { item ->
            AddRelatedToLibraryDialog(
                item = item,
                isAdding = isAddingRelated,
                confirmButtonText = "Add to Library",
                onConfirm = { status ->
                    if (!isAddingRelated) {
                        isAddingRelated = true
                        coroutineScope.launch {
                            try {
                                viewModel.addRelatedToLibrary(item, status, context)
                                relatedItemToAdd = null
                            } finally {
                                isAddingRelated = false
                            }
                        }
                    }
                },
                onDismiss = {
                    if (!isAddingRelated) {
                        relatedItemToAdd = null
                    }
                }
            )
        }
    }

    // Advanced Filter Modal Bottom Sheet
    if (state.isFilterSheetOpen) {
        AdvancedFilterSheet(
            state = state,
            viewModel = viewModel,
            onDismiss = { viewModel.setFilterSheetOpen(false) }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LXBackgroundBlack,
        contentWindowInsets = if (isTablet) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
        topBar = {
            if (state.isSelectionMode) {
                // Contextual Multi-Select Top Bar
                TopAppBar(
                    title = {
                        Text(
                            text = "${state.selectedIds.size} Selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXPrimaryGreen,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection", tint = LXTextPrimary)
                        }
                    },
                    actions = {
                        // Select All
                        IconButton(onClick = { viewModel.selectAll(visibleIds) }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = LXTextPrimary)
                        }

                        // External DB Sync Action (Single unified sync for MAL & TMDB)
                        IconButton(onClick = { showExternalDbBatchDialog = true }) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync Selected with External DB", tint = LXPrimaryGreen)
                        }

                        // Bulk Delete Action
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = LXErrorRed)
                        }

                        if (isTablet) {
                            // Range Select
                            TextButton(onClick = { viewModel.selectRange(visibleIds) }) {
                                Text("Range", color = LXPrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            // Invert Selection
                            TextButton(onClick = { viewModel.invertSelection(visibleIds) }) {
                                Text("Invert", color = LXTextPrimary, fontSize = 13.sp)
                            }
                        } else {
                            // Overflow menu on phone so the top bar remains clean and title is never crushed
                            Box {
                                IconButton(onClick = { showSelectionOverflowMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More selection actions", tint = LXTextPrimary)
                                }
                                DropdownMenu(
                                    expanded = showSelectionOverflowMenu,
                                    onDismissRequest = { showSelectionOverflowMenu = false },
                                    modifier = Modifier.background(LXSurfaceElevated)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Select Range", color = LXTextPrimary) },
                                        onClick = {
                                            viewModel.selectRange(visibleIds)
                                            showSelectionOverflowMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Invert Selection", color = LXTextPrimary) },
                                        onClick = {
                                            viewModel.invertSelection(visibleIds)
                                            showSelectionOverflowMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("External Tracking", color = LXTextPrimary) },
                                        onClick = {
                                            showExternalTrackingBatchDialog = true
                                            showSelectionOverflowMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = LXSurfaceElevated)
                )
            } else {
                // Standard Top Bar
                TopAppBar(
                    navigationIcon = {
                        UserAvatarButton(
                            activeUser = activeUser,
                            onClick = onUserAvatarClick
                        )
                    },
                    title = {
                        Text(
                            text = "Library (${state.totalCount})",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    },
                    actions = {
                        // Sort Button with Dropdown
                        Box {
                            IconButton(onClick = { viewModel.setSortMenuOpen(true) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort",
                                    tint = LXPrimaryGreen
                                )
                            }

                            DropdownMenu(
                                expanded = state.isSortMenuOpen,
                                onDismissRequest = { viewModel.setSortMenuOpen(false) }
                            ) {
                                LibrarySortOption.entries.forEach { option ->
                                    val isCurrent = state.selectedSort == option
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.displayName,
                                                color = if (isCurrent) LXPrimaryGreen else LXTextPrimary,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = { viewModel.setSortOption(option) },
                                        trailingIcon = {
                                            if (isCurrent) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = LXPrimaryGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Advanced Filter Button with Badge
                        IconButton(onClick = { viewModel.setFilterSheetOpen(true) }) {
                            if (state.activeFilterCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = LXPrimaryGreen,
                                            contentColor = LXBackgroundBlack
                                        ) {
                                            Text(state.activeFilterCount.toString(), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "All Filters",
                                        tint = LXPrimaryGreen
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "All Filters",
                                    tint = LXTextSecondary
                                )
                            }
                        }

                        // Multi-select toggle button
                        IconButton(onClick = {
                            if (visibleIds.isNotEmpty()) {
                                viewModel.toggleSelect(visibleIds.first())
                            }
                        }) {
                            Icon(Icons.Default.Checklist, contentDescription = "Select Mode", tint = LXTextSecondary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = LXBackgroundBlack)
                )
            }
        },
        bottomBar = {
            if (state.isSelectionMode) {
                Surface(
                    color = LXSurfaceElevated,
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, LXBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BatchActionButton(
                            icon = Icons.Default.Category,
                            label = "Category",
                            onClick = { showCategoryBatchDialog = true }
                        )
                        BatchActionButton(
                            icon = Icons.Default.Style,
                            label = "Sub Category",
                            onClick = { showSubCategoryBatchDialog = true }
                        )
                        BatchActionButton(
                            icon = Icons.Default.Bookmarks,
                            label = "Status",
                            onClick = { showStatusBatchDialog = true }
                        )
                        BatchActionButton(
                            icon = Icons.Default.Favorite,
                            label = "Favorite",
                            onClick = { showFavoriteBatchDialog = true }
                        )
                        BatchActionButton(
                            icon = Icons.Default.Sync,
                            label = "Tracking",
                            onClick = { showExternalTrackingBatchDialog = true }
                        )
                        BatchActionButton(
                            icon = Icons.Default.Tune,
                            label = "DB Sync",
                            onClick = { showExternalDbBatchDialog = true }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!state.isSelectionMode && onAddNewEntry != null) {
                FloatingActionButton(
                    onClick = {
                        onAddNewEntry(
                            state.selectedCategoryFilter ?: MediaType.ANIME,
                            state.selectedCustomCategoryFilter,
                            state.selectedSubCategoryFilter,
                            state.selectedStatusFilter ?: MediaStatus.WATCHING
                        )
                    },
                    containerColor = LXPrimaryGreen,
                    contentColor = LXBackgroundBlack,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Entry", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        "Search title, alternate title, tags...",
                        color = LXTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = LXTextPrimary),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = LXTextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = LXSurfaceElevated,
                    unfocusedContainerColor = LXSurfaceElevated,
                    focusedBorderColor = LXPrimaryGreen,
                    unfocusedBorderColor = LXBorder,
                    focusedTextColor = LXTextPrimary,
                    unfocusedTextColor = LXTextPrimary,
                    cursorColor = LXPrimaryGreen
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Quick Category Horizontal Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategoryFilter == null && state.selectedCustomCategoryFilter == null,
                        onClick = {
                            viewModel.setCategoryFilter(null)
                            viewModel.setCustomCategoryFilter(null)
                        },
                        label = { Text("All Categories", fontSize = 12.sp, fontWeight = if (state.selectedCategoryFilter == null && state.selectedCustomCategoryFilter == null) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.height(34.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen,
                            selectedLabelColor = LXBackgroundBlack,
                            containerColor = LXSurfaceElevated,
                            labelColor = LXTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = state.selectedCategoryFilter == null && state.selectedCustomCategoryFilter == null,
                            borderColor = if (state.selectedCategoryFilter == null && state.selectedCustomCategoryFilter == null) LXPrimaryGreen else LXBorder
                        )
                    )
                }

                items(MediaType.entries) { cat ->
                    val isSelected = state.selectedCategoryFilter == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCategoryFilter(if (isSelected) null else cat) },
                        label = { Text(cat.displayName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.height(34.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen,
                            selectedLabelColor = LXBackgroundBlack,
                            containerColor = LXSurfaceElevated,
                            labelColor = LXTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) LXPrimaryGreen else LXBorder
                        )
                    )
                }

                // Custom Categories Chips
                items(state.customCategories) { customCat ->
                    val isSelected = state.selectedCustomCategoryFilter.equals(customCat.name, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCustomCategoryFilter(if (isSelected) null else customCat.name) },
                        label = { Text(customCat.name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.height(34.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen,
                            selectedLabelColor = LXBackgroundBlack,
                            containerColor = LXSurfaceElevated,
                            labelColor = LXTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) LXPrimaryGreen else LXBorder
                        )
                    )
                }
            }

            // Active Filter Removable Badges (if filters active)
            val hasActiveFilters = state.selectedCategoryFilter != null ||
                    state.selectedCustomCategoryFilter != null ||
                    state.selectedSubCategoryFilter != null ||
                    state.selectedStatusFilter != null ||
                    state.minScore != null ||
                    state.onlyUnrated ||
                    state.includedTags.isNotEmpty() ||
                    state.excludedTags.isNotEmpty() ||
                    state.favoritesFilter != InclusionFilterOption.ALL ||
                    state.airingFilter != InclusionFilterOption.ALL ||
                    state.externalTrackingFilter != InclusionFilterOption.ALL ||
                    state.coverImageFilter != CoverImageFilterOption.ALL

            if (hasActiveFilters) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        RemovableFilterBadge(
                            label = "Reset All",
                            onRemove = { viewModel.resetAllFilters() }
                        )
                    }

                    state.selectedCategoryFilter?.let { cat ->
                        item {
                            RemovableFilterBadge(
                                label = "Category: ${cat.displayName}",
                                onRemove = { viewModel.setCategoryFilter(null) }
                            )
                        }
                    }

                    state.selectedCustomCategoryFilter?.let { customCat ->
                        item {
                            RemovableFilterBadge(
                                label = "Category: $customCat",
                                onRemove = { viewModel.setCustomCategoryFilter(null) }
                            )
                        }
                    }

                    state.selectedSubCategoryFilter?.let { subCat ->
                        item {
                            RemovableFilterBadge(
                                label = "Sub: $subCat",
                                onRemove = { viewModel.setSubCategoryFilter(null) }
                            )
                        }
                    }
                    state.selectedStatusFilter?.let { status ->
                        item {
                            RemovableFilterBadge(
                                label = "Status: ${status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                onRemove = { viewModel.setStatusFilter(null) }
                            )
                        }
                    }

                    if (state.minScore != null) {
                        item {
                            RemovableFilterBadge(
                                label = "Score: ${state.minScore}+",
                                onRemove = { viewModel.setMinScore(null) }
                            )
                        }
                    }

                    if (state.onlyUnrated) {
                        item {
                            RemovableFilterBadge(
                                label = "Unrated",
                                onRemove = { viewModel.setOnlyUnrated(false) }
                            )
                        }
                    }

                    state.includedTags.forEach { tag ->
                        item(key = "inc_$tag") {
                            RemovableFilterBadge(
                                label = "+Tag: $tag",
                                onRemove = { viewModel.removeTagFilter(tag) }
                            )
                        }
                    }

                    state.excludedTags.forEach { tag ->
                        item(key = "exc_$tag") {
                            RemovableFilterBadge(
                                label = "-Tag: $tag",
                                onRemove = { viewModel.removeTagFilter(tag) }
                            )
                        }
                    }

                    if (state.favoritesFilter == InclusionFilterOption.INCLUDE_ONLY) {
                        item {
                            RemovableFilterBadge(
                                label = "+Favorites",
                                onRemove = { viewModel.setFavoritesFilter(InclusionFilterOption.ALL) }
                            )
                        }
                    } else if (state.favoritesFilter == InclusionFilterOption.EXCLUDE) {
                        item {
                            RemovableFilterBadge(
                                label = "-Favorites",
                                onRemove = { viewModel.setFavoritesFilter(InclusionFilterOption.ALL) }
                            )
                        }
                    }

                    if (state.airingFilter == InclusionFilterOption.INCLUDE_ONLY) {
                        item {
                            RemovableFilterBadge(
                                label = "+Airing",
                                onRemove = { viewModel.setAiringFilter(InclusionFilterOption.ALL) }
                            )
                        }
                    } else if (state.airingFilter == InclusionFilterOption.EXCLUDE) {
                        item {
                            RemovableFilterBadge(
                                label = "-Airing",
                                onRemove = { viewModel.setAiringFilter(InclusionFilterOption.ALL) }
                            )
                        }
                    }

                    if (state.externalTrackingFilter == InclusionFilterOption.INCLUDE_ONLY) {
                        item {
                            RemovableFilterBadge(
                                label = "+Tracked Only",
                                onRemove = { viewModel.setExternalTrackingFilter(InclusionFilterOption.ALL) }
                            )
                        }
                    } else if (state.externalTrackingFilter == InclusionFilterOption.EXCLUDE) {
                        item {
                            RemovableFilterBadge(
                                label = "-Not Tracked",
                                onRemove = { viewModel.setExternalTrackingFilter(InclusionFilterOption.ALL) }
                            )
                        }
                    }

                    when (state.coverImageFilter) {
                        CoverImageFilterOption.ALL -> {}
                        CoverImageFilterOption.HAS_COVER -> {
                            item {
                                RemovableFilterBadge(
                                    label = "+Has Cover",
                                    onRemove = { viewModel.setCoverImageFilter(CoverImageFilterOption.ALL) }
                                )
                            }
                        }
                        CoverImageFilterOption.BROKEN_COVER -> {
                            item {
                                RemovableFilterBadge(
                                    label = "⚠️ Broken Cover URL",
                                    onRemove = { viewModel.setCoverImageFilter(CoverImageFilterOption.ALL) }
                                )
                            }
                        }
                        CoverImageFilterOption.NO_COVER -> {
                            item {
                                RemovableFilterBadge(
                                    label = "-No Cover",
                                    onRemove = { viewModel.setCoverImageFilter(CoverImageFilterOption.ALL) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Grid Content
            if (state.entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = LXTextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (state.searchQuery.isNotBlank() || state.activeFilterCount > 0) "No media matches the current filters" else "No library entries found",
                            style = MaterialTheme.typography.titleMedium,
                            color = LXTextSecondary
                        )
                        if (state.activeFilterCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.resetAllFilters() },
                                colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear Filters", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 360.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = state.entries,
                        key = { it.id }
                    ) { entry ->
                        MediaCard(
                            entry = entry,
                            onClick = { onEntryClick(entry.id) },
                            onIncrement = {
                                if (entry.status == MediaStatus.PLAN_TO_WATCH) {
                                    entryToChangeStatusOnIncrement = entry
                                } else {
                                    viewModel.incrementProgress(entry)
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(entry) },
                            onDelete = { viewModel.deleteEntry(entry) },
                            onEdit = { onEditEntry(entry.id) },
                            isSelectionMode = state.isSelectionMode,
                            isSelected = state.selectedIds.contains(entry.id),
                            onToggleSelect = { viewModel.toggleSelect(entry.id) },
                            onLongClick = { viewModel.toggleSelect(entry.id) },
                            allLibraryEntries = state.allUserEntries,
                            onRelatedEntryClick = { onEntryClick(it) },
                            onAddRelatedItem = { item -> relatedItemToAdd = item }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemovableFilterBadge(
    label: String,
    onRemove: () -> Unit
) {
    val isExclude = label.startsWith("-")
    val badgeColor = if (isExclude) LXErrorRed else LXPrimaryGreen
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor.copy(alpha = 0.15f))
            .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .clickable(onClick = onRemove)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove",
            tint = badgeColor,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun TriStateTagCheckbox(
    state: TagSelectionState,
    modifier: Modifier = Modifier
) {
    val boxSize = 20.dp
    val shape = RoundedCornerShape(4.dp)

    when (state) {
        TagSelectionState.UNSELECTED -> {
            Box(
                modifier = modifier
                    .size(boxSize)
                    .border(BorderStroke(1.5.dp, LXTextSecondary.copy(alpha = 0.6f)), shape)
                    .clip(shape)
            )
        }
        TagSelectionState.INCLUDED -> {
            Box(
                modifier = modifier
                    .size(boxSize)
                    .background(LXPrimaryGreen, shape)
                    .clip(shape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Included (✓)",
                    tint = LXBackgroundBlack,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        TagSelectionState.EXCLUDED -> {
            Box(
                modifier = modifier
                    .size(boxSize)
                    .background(LXErrorRed, shape)
                    .clip(shape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Excluded (✕)",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun TriStateFilterRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    state: InclusionFilterOption,
    onClick: () -> Unit
) {
    val activeColor = when (state) {
        InclusionFilterOption.INCLUDE_ONLY -> LXPrimaryGreen
        InclusionFilterOption.EXCLUDE -> LXErrorRed
        InclusionFilterOption.ALL -> LXTextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when (state) {
                    InclusionFilterOption.INCLUDE_ONLY -> LXPrimaryGreen.copy(alpha = 0.12f)
                    InclusionFilterOption.EXCLUDE -> LXErrorRed.copy(alpha = 0.12f)
                    InclusionFilterOption.ALL -> LXSurfaceElevated
                }
            )
            .border(
                width = if (state != InclusionFilterOption.ALL) 1.dp else 0.5.dp,
                color = when (state) {
                    InclusionFilterOption.INCLUDE_ONLY -> LXPrimaryGreen.copy(alpha = 0.6f)
                    InclusionFilterOption.EXCLUDE -> LXErrorRed.copy(alpha = 0.6f)
                    InclusionFilterOption.ALL -> LXBorder
                },
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when (state) {
                            InclusionFilterOption.INCLUDE_ONLY -> LXPrimaryGreen.copy(alpha = 0.2f)
                            InclusionFilterOption.EXCLUDE -> LXErrorRed.copy(alpha = 0.2f)
                            InclusionFilterOption.ALL -> LXSurfaceBlack
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = activeColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (state != InclusionFilterOption.ALL) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (state != InclusionFilterOption.ALL) activeColor else LXTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = if (state != InclusionFilterOption.ALL) activeColor.copy(alpha = 0.85f) else LXTextSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (state) {
                InclusionFilterOption.ALL -> {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .border(BorderStroke(1.5.dp, LXTextSecondary.copy(alpha = 0.5f)), RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
                InclusionFilterOption.INCLUDE_ONLY -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LXPrimaryGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Only",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = LXPrimaryGreen
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(LXPrimaryGreen, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Included",
                            tint = LXBackgroundBlack,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                InclusionFilterOption.EXCLUDE -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LXErrorRed.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Excluded",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = LXErrorRed
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(LXErrorRed, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Excluded",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverImageFilterRow(
    state: CoverImageFilterOption,
    onClick: () -> Unit
) {
    val activeColor = when (state) {
        CoverImageFilterOption.HAS_COVER -> LXPrimaryGreen
        CoverImageFilterOption.BROKEN_COVER -> LXErrorRed
        CoverImageFilterOption.NO_COVER -> Color(0xFFE67E22)
        CoverImageFilterOption.ALL -> LXTextSecondary
    }

    val icon = when (state) {
        CoverImageFilterOption.BROKEN_COVER -> Icons.Default.BrokenImage
        CoverImageFilterOption.NO_COVER -> Icons.Default.Close
        else -> Icons.Default.Image
    }

    val subtitle = when (state) {
        CoverImageFilterOption.ALL -> "Tap: Has Cover → Broken URL → No Cover → Off"
        CoverImageFilterOption.HAS_COVER -> "Showing Only Entries With Cover Image"
        CoverImageFilterOption.BROKEN_COVER -> "Showing Entries With Broken Cover URL"
        CoverImageFilterOption.NO_COVER -> "Excluding Entries With Cover (No Cover Only)"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when (state) {
                    CoverImageFilterOption.HAS_COVER -> LXPrimaryGreen.copy(alpha = 0.12f)
                    CoverImageFilterOption.BROKEN_COVER -> LXErrorRed.copy(alpha = 0.12f)
                    CoverImageFilterOption.NO_COVER -> Color(0xFFE67E22).copy(alpha = 0.12f)
                    CoverImageFilterOption.ALL -> LXSurfaceElevated
                }
            )
            .border(
                width = if (state != CoverImageFilterOption.ALL) 1.dp else 0.5.dp,
                color = when (state) {
                    CoverImageFilterOption.HAS_COVER -> LXPrimaryGreen.copy(alpha = 0.6f)
                    CoverImageFilterOption.BROKEN_COVER -> LXErrorRed.copy(alpha = 0.6f)
                    CoverImageFilterOption.NO_COVER -> Color(0xFFE67E22).copy(alpha = 0.6f)
                    CoverImageFilterOption.ALL -> LXBorder
                },
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when (state) {
                            CoverImageFilterOption.HAS_COVER -> LXPrimaryGreen.copy(alpha = 0.2f)
                            CoverImageFilterOption.BROKEN_COVER -> LXErrorRed.copy(alpha = 0.2f)
                            CoverImageFilterOption.NO_COVER -> Color(0xFFE67E22).copy(alpha = 0.2f)
                            CoverImageFilterOption.ALL -> LXSurfaceBlack
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = activeColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Cover Image",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (state != CoverImageFilterOption.ALL) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (state != CoverImageFilterOption.ALL) activeColor else LXTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = if (state != CoverImageFilterOption.ALL) activeColor.copy(alpha = 0.85f) else LXTextSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (state) {
                CoverImageFilterOption.ALL -> {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .border(BorderStroke(1.5.dp, LXTextSecondary.copy(alpha = 0.5f)), RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
                CoverImageFilterOption.HAS_COVER -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LXPrimaryGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Has Cover",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = LXPrimaryGreen
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(LXPrimaryGreen, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Has Cover",
                            tint = LXBackgroundBlack,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                CoverImageFilterOption.BROKEN_COVER -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LXErrorRed.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Broken URL",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = LXErrorRed
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(LXErrorRed, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = "Broken URL",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                CoverImageFilterOption.NO_COVER -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE67E22).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "No Cover",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = Color(0xFFE67E22)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color(0xFFE67E22), RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "No Cover",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = LXPrimaryGreen, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LXTextPrimary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AdvancedFilterSheet(
    state: LibraryUiState,
    viewModel: LibraryViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showTagsSubMenu by remember { mutableStateOf(false) }
    var tagSearchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = showTagsSubMenu) {
        showTagsSubMenu = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LXSurfaceBlack,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(LXBorder)
            )
        }
    ) {
        if (showTagsSubMenu) {
            // ================= TAGS SUB-MENU =================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showTagsSubMenu = false }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LXPrimaryGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tags Filter",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }

                    if (state.includedTags.isNotEmpty() || state.excludedTags.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearTagFilters() }) {
                            Text("Clear All", color = LXErrorRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = LXBorder, thickness = 1.dp)

                // Sub-menu Instructions & active count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(LXPrimaryGreen, RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = LXBackgroundBlack, modifier = Modifier.size(12.dp))
                        }
                        Text("1 tap = Include", fontSize = 11.sp, color = LXPrimaryGreen, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(LXErrorRed, RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                        }
                        Text("2 taps = Exclude", fontSize = 11.sp, color = LXErrorRed, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "${state.includedTags.size} inc • ${state.excludedTags.size} exc",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (state.includedTags.isNotEmpty() || state.excludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary
                    )
                }

                // Search Bar for Tags
                OutlinedTextField(
                    value = tagSearchQuery,
                    onValueChange = { tagSearchQuery = it },
                    placeholder = { Text("Search tags...", color = LXTextSecondary, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = LXTextSecondary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (tagSearchQuery.isNotBlank()) {
                            IconButton(onClick = { tagSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = LXTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LXPrimaryGreen,
                        unfocusedBorderColor = LXBorder,
                        focusedTextColor = LXTextPrimary,
                        unfocusedTextColor = LXTextPrimary,
                        focusedContainerColor = LXSurfaceElevated,
                        unfocusedContainerColor = LXSurfaceElevated
                    )
                )

                // Tag List with Tri-State Checkboxes
                val filteredTags = remember(state.availableTags, tagSearchQuery) {
                    if (tagSearchQuery.isBlank()) {
                        state.availableTags
                    } else {
                        state.availableTags.filter { it.contains(tagSearchQuery, ignoreCase = true) }
                    }
                }

                if (state.availableTags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LXSurfaceElevated)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tags found in library. Add tags to your media in the Edit screen to filter by them.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LXTextSecondary
                        )
                    }
                } else if (filteredTags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LXSurfaceElevated)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tags matching \"$tagSearchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LXTextSecondary
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredTags.forEach { tag ->
                            val tagState = state.getTagState(tag)
                            val count = state.tagCounts[tag] ?: 0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (tagState) {
                                            TagSelectionState.INCLUDED -> LXPrimaryGreen.copy(alpha = 0.12f)
                                            TagSelectionState.EXCLUDED -> LXErrorRed.copy(alpha = 0.12f)
                                            TagSelectionState.UNSELECTED -> LXSurfaceElevated
                                        }
                                    )
                                    .border(
                                        width = if (tagState != TagSelectionState.UNSELECTED) 1.dp else 0.5.dp,
                                        color = when (tagState) {
                                            TagSelectionState.INCLUDED -> LXPrimaryGreen.copy(alpha = 0.6f)
                                            TagSelectionState.EXCLUDED -> LXErrorRed.copy(alpha = 0.6f)
                                            TagSelectionState.UNSELECTED -> LXBorder
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.cycleTagState(tag) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    TriStateTagCheckbox(state = tagState)

                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (tagState != TagSelectionState.UNSELECTED) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = when (tagState) {
                                            TagSelectionState.INCLUDED -> LXPrimaryGreen
                                            TagSelectionState.EXCLUDED -> LXErrorRed
                                            TagSelectionState.UNSELECTED -> LXTextPrimary
                                        }
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    when (tagState) {
                                        TagSelectionState.INCLUDED -> {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(LXPrimaryGreen.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Included",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    color = LXPrimaryGreen
                                                )
                                            }
                                        }
                                        TagSelectionState.EXCLUDED -> {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(LXErrorRed.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Excluded",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    color = LXErrorRed
                                                )
                                            }
                                        }
                                        TagSelectionState.UNSELECTED -> {}
                                    }

                                    Text(
                                        text = "($count)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LXTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Done button in sub-menu
                Button(
                    onClick = { showTagsSubMenu = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Apply Tag Filters (${state.includedTags.size} Inc, ${state.excludedTags.size} Exc)",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            // ================= MAIN ADVANCED FILTERS =================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = LXPrimaryGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Advanced Filters",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }

                    if (state.activeFilterCount > 0) {
                        TextButton(onClick = { viewModel.resetAllFilters() }) {
                            Text("Reset All", color = LXErrorRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = LXBorder, thickness = 1.dp)

                // Status Filter Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedStatusFilter == null,
                            onClick = { viewModel.setStatusFilter(null) },
                            label = { Text("All Statuses") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LXPrimaryGreen,
                                selectedLabelColor = LXBackgroundBlack,
                                containerColor = LXSurfaceElevated,
                                labelColor = LXTextSecondary
                            )
                        )

                        MediaStatus.entries.forEach { st ->
                            val isSelected = state.selectedStatusFilter == st
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setStatusFilter(if (isSelected) null else st) },
                                label = { Text(st.defaultLabel) },
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

                // Sub Category Filter Section (Advanced Filter)
                val catFilter = state.selectedCategoryFilter
                val customCatFilter = state.selectedCustomCategoryFilter
                val relevantSubCats = remember(state.subCategories, catFilter, customCatFilter) {
                    if (catFilter != null) {
                        state.subCategories.filter { it.categoryKey.equals(catFilter.name, ignoreCase = true) }
                    } else if (customCatFilter != null) {
                        state.subCategories.filter { it.categoryKey.equals(customCatFilter, ignoreCase = true) }
                    } else {
                        state.subCategories.distinctBy { it.name.lowercase() }
                    }
                }

                if (relevantSubCats.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Style, contentDescription = null, tint = LXScoreGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sub Category",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = LXTextPrimary
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.selectedSubCategoryFilter == null,
                                onClick = { viewModel.setSubCategoryFilter(null) },
                                label = { Text("All Sub Categories") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LXPrimaryGreen,
                                    selectedLabelColor = LXBackgroundBlack,
                                    containerColor = LXSurfaceElevated,
                                    labelColor = LXTextSecondary
                                )
                            )

                            relevantSubCats.forEach { subCat ->
                                val isSelected = state.selectedSubCategoryFilter.equals(subCat.name, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setSubCategoryFilter(if (isSelected) null else subCat.name) },
                                    label = { Text(subCat.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LXScoreGold,
                                        selectedLabelColor = LXBackgroundBlack,
                                        containerColor = LXSurfaceElevated,
                                        labelColor = LXTextSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                // Score / Rating Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = LXScoreGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Score / Rating",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.minScore == null && !state.onlyUnrated,
                            onClick = {
                                viewModel.setMinScore(null)
                                viewModel.setOnlyUnrated(false)
                            },
                            label = { Text("Any Score") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LXPrimaryGreen,
                                selectedLabelColor = LXBackgroundBlack,
                                containerColor = LXSurfaceElevated,
                                labelColor = LXTextSecondary
                            )
                        )

                        listOf(9, 8, 7, 6, 5).forEach { threshold ->
                            val isSelected = state.minScore == threshold && !state.onlyUnrated
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setMinScore(if (isSelected) null else threshold) },
                                label = { Text("★ $threshold+") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LXScoreGold,
                                    selectedLabelColor = LXBackgroundBlack,
                                    containerColor = LXSurfaceElevated,
                                    labelColor = LXTextSecondary
                                )
                            )
                        }

                        FilterChip(
                            selected = state.onlyUnrated,
                            onClick = { viewModel.setOnlyUnrated(!state.onlyUnrated) },
                            label = { Text("Unrated") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LXPrimaryGreen,
                                selectedLabelColor = LXBackgroundBlack,
                                containerColor = LXSurfaceElevated,
                                labelColor = LXTextSecondary
                            )
                        )
                    }
                }

                // Tags Filter Section (Replaced Priority)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Style, contentDescription = null, tint = LXPrimaryGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tags Filter",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = LXTextPrimary
                            )
                        }

                        if (state.includedTags.isNotEmpty() || state.excludedTags.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearTagFilters() }) {
                                Text("Clear Tags", color = LXErrorRed, fontSize = 12.sp)
                            }
                        }
                    }

                    // Clickable Card to open Tags Sub-Menu
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showTagsSubMenu = true },
                        colors = CardDefaults.cardColors(containerColor = LXSurfaceElevated),
                        border = BorderStroke(
                            1.dp,
                            if (state.includedTags.isNotEmpty() || state.excludedTags.isNotEmpty()) LXPrimaryGreen.copy(alpha = 0.6f) else LXBorder
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LXPrimaryGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Checklist,
                                        contentDescription = null,
                                        tint = LXPrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Select & Exclude Tags",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = LXTextPrimary
                                    )
                                    Text(
                                        text = if (state.includedTags.isEmpty() && state.excludedTags.isEmpty())
                                            "1 tap: [✓] Include • 2 taps: [✕] Exclude • 3 taps: Clear"
                                        else
                                            "${state.includedTags.size} included (✓) • ${state.excludedTags.size} excluded (✕)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = LXTextSecondary
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Open Tags Filter",
                                tint = LXPrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Active Tag Chips in Main Sheet
                    if (state.includedTags.isNotEmpty() || state.excludedTags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            state.includedTags.forEach { tag ->
                                RemovableFilterBadge(
                                    label = "+$tag",
                                    onRemove = { viewModel.removeTagFilter(tag) }
                                )
                            }
                            state.excludedTags.forEach { tag ->
                                RemovableFilterBadge(
                                    label = "-$tag",
                                    onRemove = { viewModel.removeTagFilter(tag) }
                                )
                            }
                        }
                    }
                }

                // Quick Toggles Section (Extra Options with Tri-State Exclusion)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Extra Options",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )

                    // Ongoing / Airing Filter
                    TriStateFilterRow(
                        title = "Ongoing / Airing",
                        subtitle = when (state.airingFilter) {
                            InclusionFilterOption.ALL -> "Tap: Only Ongoing → Exclude Ongoing → Off"
                            InclusionFilterOption.INCLUDE_ONLY -> "Showing Only Ongoing / Airing"
                            InclusionFilterOption.EXCLUDE -> "Excluding / Hiding Ongoing (Finished only)"
                        },
                        icon = Icons.Default.Tv,
                        state = state.airingFilter,
                        onClick = { viewModel.cycleAiringFilter() }
                    )

                    // Favorites Filter
                    TriStateFilterRow(
                        title = "Favorites",
                        subtitle = when (state.favoritesFilter) {
                            InclusionFilterOption.ALL -> "Tap: Only Favorites → Exclude Favorites → Off"
                            InclusionFilterOption.INCLUDE_ONLY -> "Showing Only Favorites"
                            InclusionFilterOption.EXCLUDE -> "Excluding / Hiding Favorites"
                        },
                        icon = Icons.Default.Favorite,
                        state = state.favoritesFilter,
                        onClick = { viewModel.cycleFavoritesFilter() }
                    )

                    // External Tracking Filter
                    TriStateFilterRow(
                        title = "External Tracking",
                        subtitle = when (state.externalTrackingFilter) {
                            InclusionFilterOption.ALL -> "Tap: Tracked Only → Not Tracked → Off"
                            InclusionFilterOption.INCLUDE_ONLY -> "Showing Only Externally Tracked"
                            InclusionFilterOption.EXCLUDE -> "Excluding / Hiding Tracked (Not Tracked only)"
                        },
                        icon = Icons.Default.Sync,
                        state = state.externalTrackingFilter,
                        onClick = { viewModel.cycleExternalTrackingFilter() }
                    )

                    // Cover Image Filter (All / Has Cover / Broken URL / No Cover)
                    CoverImageFilterRow(
                        state = state.coverImageFilter,
                        onClick = { viewModel.cycleCoverImageFilter() }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Apply / Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Show ${state.totalCount} ${if (state.totalCount == 1) "Result" else "Results"}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
