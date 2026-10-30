package com.lx.tracklist.ui.library

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.Priority
import com.lx.tracklist.ui.components.MediaCard
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onEntryClick: (Long) -> Unit,
    onEditEntry: (Long) -> Unit,
    onAddNewEntry: ((mediaType: MediaType, customCategory: String?, subCategory: String?, status: MediaStatus) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCategoryBatchDialog by remember { mutableStateOf(false) }
    var showSubCategoryBatchDialog by remember { mutableStateOf(false) }
    var showStatusBatchDialog by remember { mutableStateOf(false) }
    var showFavoriteBatchDialog by remember { mutableStateOf(false) }

    val visibleIds = remember(state.entries) { state.entries.map { it.id } }

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
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = LXPrimaryGreen
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

                        // Range Select
                        TextButton(onClick = { viewModel.selectRange(visibleIds) }) {
                            Text("Range", color = LXPrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Invert Selection
                        TextButton(onClick = { viewModel.invertSelection(visibleIds) }) {
                            Text("Invert", color = LXTextPrimary, fontSize = 13.sp)
                        }

                        // Bulk Delete Action
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = LXErrorRed)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = LXSurfaceElevated)
                )
            } else {
                // Standard Top Bar
                TopAppBar(
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
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Filters",
                                        tint = LXPrimaryGreen
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filters",
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
            TextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search title, alternate title, tags...", color = LXTextSecondary) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = LXPrimaryGreen)
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = LXTextSecondary)
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = LXSurfaceVariant,
                    unfocusedContainerColor = LXSurfaceVariant,
                    focusedTextColor = LXTextPrimary,
                    unfocusedTextColor = LXTextPrimary,
                    cursorColor = LXPrimaryGreen,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            // Filter & Sort Control Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter Sheet Trigger Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (state.activeFilterCount > 0) LXPrimaryGreen.copy(alpha = 0.18f) else LXSurfaceElevated)
                            .border(
                                width = 1.dp,
                                color = if (state.activeFilterCount > 0) LXPrimaryGreen else LXBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setFilterSheetOpen(true) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (state.activeFilterCount > 0) LXPrimaryGreen else LXTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.activeFilterCount > 0) "Filters (${state.activeFilterCount})" else "All Filters",
                                fontSize = 12.sp,
                                fontWeight = if (state.activeFilterCount > 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (state.activeFilterCount > 0) LXPrimaryGreen else LXTextPrimary
                            )
                        }
                    }

                    // Sort Selector Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(LXSurfaceElevated)
                            .border(1.dp, LXBorder, RoundedCornerShape(8.dp))
                            .clickable { viewModel.setSortMenuOpen(true) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = null,
                                tint = LXTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = state.selectedSort.displayName,
                                fontSize = 12.sp,
                                color = LXTextPrimary
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = LXTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Reset Filters Button
                if (state.activeFilterCount > 0) {
                    TextButton(
                        onClick = { viewModel.resetAllFilters() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Reset", color = LXErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Category & Favorites Horizontal Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.onlyFavorites,
                        onClick = { viewModel.toggleFavoritesFilter() },
                        label = { Text("Favorites", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if (state.onlyFavorites) LXBackgroundBlack else LXPrimaryGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen,
                            selectedLabelColor = LXBackgroundBlack,
                            containerColor = LXSurfaceElevated,
                            labelColor = LXTextSecondary
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = state.selectedCategoryFilter == null && state.selectedCustomCategoryFilter == null,
                        onClick = {
                            viewModel.setCategoryFilter(null)
                            viewModel.setCustomCategoryFilter(null)
                        },
                        label = { Text("All Categories", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen,
                            selectedLabelColor = LXBackgroundBlack,
                            containerColor = LXSurfaceElevated,
                            labelColor = LXTextSecondary
                        )
                    )
                }

                items(MediaType.entries) { cat ->
                    val isSelected = state.selectedCategoryFilter == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCategoryFilter(if (isSelected) null else cat) },
                        label = { Text(cat.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen,
                            selectedLabelColor = LXBackgroundBlack,
                            containerColor = LXSurfaceElevated,
                            labelColor = LXTextSecondary
                        )
                    )
                }

                // Custom Categories Chips
                items(state.customCategories) { customCat ->
                    val isSelected = state.selectedCustomCategoryFilter.equals(customCat.name, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCustomCategoryFilter(if (isSelected) null else customCat.name) },
                        label = { Text(customCat.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen,
                            selectedLabelColor = LXBackgroundBlack,
                            containerColor = LXSurfaceElevated,
                            labelColor = LXTextSecondary
                        )
                    )
                }
            }

            // Active Filter Removable Badges (if filters active)
            if (state.selectedCategoryFilter != null || state.selectedCustomCategoryFilter != null || state.selectedSubCategoryFilter != null || state.selectedStatusFilter != null || state.minScore != null || state.onlyUnrated || state.selectedPriorityFilter != null || state.onlyAiring) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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

                    state.selectedPriorityFilter?.let { priority ->
                        item {
                            RemovableFilterBadge(
                                label = "Priority: ${priority.name}",
                                onRemove = { viewModel.setPriorityFilter(null) }
                            )
                        }
                    }

                    if (state.onlyAiring) {
                        item {
                            RemovableFilterBadge(
                                label = "Airing Only",
                                onRemove = { viewModel.toggleAiringFilter() }
                            )
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
                            onIncrement = { viewModel.incrementProgress(entry) },
                            onToggleFavorite = { viewModel.toggleFavorite(entry) },
                            onDelete = { viewModel.deleteEntry(entry) },
                            onEdit = { onEditEntry(entry.id) },
                            isSelectionMode = state.isSelectionMode,
                            isSelected = state.selectedIds.contains(entry.id),
                            onToggleSelect = { viewModel.toggleSelect(entry.id) },
                            onLongClick = { viewModel.toggleSelect(entry.id) }
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
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(LXPrimaryGreen.copy(alpha = 0.15f))
            .border(1.dp, LXPrimaryGreen.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .clickable(onClick = onRemove)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = LXPrimaryGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove",
            tint = LXPrimaryGreen,
            modifier = Modifier.size(12.dp)
        )
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = LXPrimaryGreen, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = LXTextPrimary, fontSize = 11.sp)
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
                            label = { Text(st.name.lowercase().replaceFirstChar { it.uppercase() }) },
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

            // Priority Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.selectedPriorityFilter == null,
                        onClick = { viewModel.setPriorityFilter(null) },
                        label = { Text("Any Priority") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen,
                            selectedLabelColor = LXBackgroundBlack,
                            containerColor = LXSurfaceElevated,
                            labelColor = LXTextSecondary
                        )
                    )

                    Priority.entries.forEach { p ->
                        val isSelected = state.selectedPriorityFilter == p
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setPriorityFilter(if (isSelected) null else p) },
                            label = { Text(p.name) },
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

            // Quick Toggles Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Extra Options",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )

                // Airing Only Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Only Ongoing / Airing", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = state.onlyAiring,
                        onCheckedChange = { viewModel.toggleAiringFilter() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LXBackgroundBlack,
                            checkedTrackColor = LXPrimaryGreen
                        )
                    )
                }

                // Favorites Only Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Favorites Only", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = state.onlyFavorites,
                        onCheckedChange = { viewModel.toggleFavoritesFilter() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LXBackgroundBlack,
                            checkedTrackColor = LXPrimaryGreen
                        )
                    )
                }
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
