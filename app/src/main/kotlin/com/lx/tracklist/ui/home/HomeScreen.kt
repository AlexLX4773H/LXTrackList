package com.lx.tracklist.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.lx.tracklist.ui.components.ThemedFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.RelatedMediaItem
import com.lx.tracklist.data.model.SubCategory
import com.lx.tracklist.ui.components.AddRelatedToLibraryDialog
import com.lx.tracklist.ui.components.CategorySelectorSheet
import com.lx.tracklist.ui.components.MediaCard
import com.lx.tracklist.ui.components.StatusFilterTabs
import com.lx.tracklist.ui.components.TopNavBar
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXErrorRed
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import com.lx.tracklist.ui.theme.ThemeManager
import kotlinx.coroutines.launch

private sealed interface HomeTab {
    data object All : HomeTab
    data class Status(val status: MediaStatus) : HomeTab
    data class SubCat(val subCategory: SubCategory) : HomeTab
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onEntryClick: (Long) -> Unit,
    onAddNewEntry: (mediaType: MediaType, customCategory: String?, subCategory: String?, status: MediaStatus) -> Unit,
    onEditEntry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    var subCategoryToDelete by remember { mutableStateOf<SubCategory?>(null) }
    var relatedItemToAdd by remember { mutableStateOf<RelatedMediaItem?>(null) }
    var isAddingRelated by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val statuses = remember {
        listOf(
            MediaStatus.WATCHING,
            MediaStatus.PLAN_TO_WATCH,
            MediaStatus.COMPLETED,
            MediaStatus.ON_HOLD,
            MediaStatus.DROPPED
        )
    }

    val homeTabs = remember(state.subCategories) {
        buildList<HomeTab> {
            add(HomeTab.All)
            statuses.forEach { add(HomeTab.Status(it)) }
            state.subCategories.forEach { add(HomeTab.SubCat(it)) }
        }
    }

    val currentTabIndex = remember(state.selectedStatus, state.selectedSubCategory, homeTabs) {
        when {
            state.selectedSubCategory != null -> {
                val idx = homeTabs.indexOfFirst {
                    it is HomeTab.SubCat && it.subCategory.name.equals(state.selectedSubCategory, ignoreCase = true)
                }
                if (idx >= 0) idx else 0
            }
            state.selectedStatus != null -> {
                val idx = homeTabs.indexOfFirst {
                    it is HomeTab.Status && it.status == state.selectedStatus
                }
                if (idx >= 0) idx else 0
            }
            else -> 0
        }
    }

    val pagerState = key(state.activeUser?.id ?: 0L) {
        rememberPagerState(
            initialPage = currentTabIndex.coerceIn(0, (homeTabs.size - 1).coerceAtLeast(0)),
            pageCount = { homeTabs.size }
        )
    }

    // Sync pager swipe to ViewModel ONLY when pager settles
    LaunchedEffect(pagerState, homeTabs) {
        snapshotFlow { pagerState.settledPage }.collect { settledIndex ->
            val targetTab = homeTabs.getOrNull(settledIndex) ?: HomeTab.All
            when (targetTab) {
                is HomeTab.All -> {
                    if (state.selectedStatus != null || state.selectedSubCategory != null) {
                        viewModel.selectStatus(null)
                    }
                }
                is HomeTab.Status -> {
                    if (state.selectedStatus != targetTab.status || state.selectedSubCategory != null) {
                        viewModel.selectStatus(targetTab.status)
                    }
                }
                is HomeTab.SubCat -> {
                    if (!state.selectedSubCategory.equals(targetTab.subCategory.name, ignoreCase = true)) {
                        viewModel.selectSubCategory(targetTab.subCategory.name)
                    }
                }
            }
        }
    }

    // Sync tab selection from ViewModel / outside to pager
    LaunchedEffect(currentTabIndex) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != currentTabIndex && currentTabIndex in 0 until homeTabs.size) {
            pagerState.animateScrollToPage(currentTabIndex)
        }
    }

    // Handle back button for search and category sheet
    BackHandler(enabled = state.isSearchActive || state.isCategorySheetOpen) {
        if (state.isCategorySheetOpen) {
            viewModel.setCategorySheetOpen(false)
        } else if (state.isSearchActive) {
            viewModel.toggleSearch()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (ThemeManager.effectiveBackgroundImage != null) Color.Transparent else LXBackgroundBlack,
        contentWindowInsets = if (isTablet) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopNavBar(
                    selectedCategory = state.selectedCategory,
                    customCategoryTitle = state.selectedCustomCategory,
                    customCategories = state.customCategories,
                    onCategoryClick = { viewModel.setCategorySheetOpen(true) },
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    isSearchActive = state.isSearchActive,
                    onToggleSearch = { viewModel.toggleSearch() },
                    isSyncRunning = state.isSyncRunning,
                    onRefresh = { viewModel.setRefreshDialogOpen(true) },
                    selectedSort = state.selectedSort,
                    onSortSelected = { viewModel.selectSort(it) },
                    activeUser = state.activeUser,
                    onLogoClick = { viewModel.setUserSwitcherOpen(true) }
                )

                StatusFilterTabs(
                    selectedCategory = state.selectedCategory,
                    selectedStatus = state.selectedStatus,
                    selectedSubCategory = state.selectedSubCategory,
                    statusCounts = state.statusCounts,
                    subCategories = state.subCategories,
                    subCategoryCounts = state.subCategoryCounts,
                    totalCount = state.totalCount,
                    onStatusSelected = { status ->
                        viewModel.selectStatus(status)
                        val targetIdx = if (status == null) 0 else homeTabs.indexOfFirst { it is HomeTab.Status && it.status == status }
                        if (targetIdx in 0 until homeTabs.size) {
                            coroutineScope.launch { pagerState.animateScrollToPage(targetIdx) }
                        }
                    },
                    onSubCategorySelected = { subCatName ->
                        viewModel.selectSubCategory(subCatName)
                        val targetIdx = if (subCatName == null) 0 else homeTabs.indexOfFirst {
                            it is HomeTab.SubCat && it.subCategory.name.equals(subCatName, ignoreCase = true)
                        }
                        if (targetIdx in 0 until homeTabs.size) {
                            coroutineScope.launch { pagerState.animateScrollToPage(targetIdx) }
                        }
                    },
                    onAddSubCategoryClick = { viewModel.setAddSubCategoryDialogOpen(true) },
                    onDeleteSubCategory = { subCategoryToDelete = it }
                )
            }
        },
        floatingActionButton = {
            ThemedFloatingActionButton(
                onClick = {
                    val currentTab = homeTabs.getOrNull(pagerState.currentPage) ?: HomeTab.All
                    val initialStatus = when (currentTab) {
                        is HomeTab.Status -> currentTab.status
                        else -> state.selectedStatus ?: MediaStatus.WATCHING
                    }
                    val initialSubCategory = when (currentTab) {
                        is HomeTab.SubCat -> currentTab.subCategory.name
                        else -> state.selectedSubCategory
                    }
                    onAddNewEntry(
                        state.selectedCategory,
                        state.selectedCustomCategory,
                        initialSubCategory,
                        initialStatus
                    )
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                key = { page -> homeTabs.getOrNull(page)?.hashCode() ?: page },
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = state.isSwipeToSwitchTabsEnabled
            ) { pageIndex ->
                val tab = homeTabs.getOrNull(pageIndex) ?: HomeTab.All
                val pageEntries = remember(tab, state.categoryEntries) {
                    when (tab) {
                        is HomeTab.All -> state.categoryEntries
                        is HomeTab.Status -> state.categoryEntries.filter { it.status == tab.status }
                        is HomeTab.SubCat -> state.categoryEntries.filter {
                            it.subCategory.equals(tab.subCategory.name, ignoreCase = true)
                        }
                    }
                }

                val tabKey = when (tab) {
                    is HomeTab.All -> "all"
                    is HomeTab.Status -> "status_${tab.status.name}"
                    is HomeTab.SubCat -> "subcat_${tab.subCategory.id}"
                }
                val gridState = rememberSaveable(
                    state.selectedCategory,
                    state.selectedCustomCategory,
                    tabKey,
                    saver = LazyGridState.Saver
                ) {
                    LazyGridState()
                }

                if (pageEntries.isEmpty()) {
                    val emptyLabel = when (tab) {
                        is HomeTab.All -> state.selectedCustomCategory ?: state.selectedCategory.displayName
                        is HomeTab.Status -> "${tab.status.getDisplayLabel(state.selectedCategory)} ${state.selectedCustomCategory ?: state.selectedCategory.displayName}"
                        is HomeTab.SubCat -> tab.subCategory.name
                    }
                    EmptyState(
                        categoryName = emptyLabel,
                        searchQuery = state.searchQuery
                    )
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 340.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = pageEntries,
                            key = { it.id }
                        ) { entry ->
                            MediaCard(
                                entry = entry,
                                onClick = { onEntryClick(entry.id) },
                                onIncrement = { viewModel.incrementProgress(entry) },
                                onToggleFavorite = { viewModel.toggleFavorite(entry) },
                                onDelete = { viewModel.deleteEntry(entry) },
                                onEdit = { onEditEntry(entry.id) },
                                allLibraryEntries = state.allUserEntries,
                                onRelatedEntryClick = { onEntryClick(it) },
                                onAddRelatedItem = { item -> relatedItemToAdd = item }
                            )
                        }
                    }
                }
            }

            if (relatedItemToAdd != null) {
                val context = LocalContext.current
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

            if (state.isCategorySheetOpen) {
                CategorySelectorSheet(
                    selectedCategory = state.selectedCategory,
                    selectedCustomCategory = state.selectedCustomCategory,
                    categoryCounts = state.categoryCounts,
                    customCategories = state.customCategories,
                    customCategoryCounts = state.customCategoryCounts,
                    onSelectCategory = { viewModel.selectCategory(it) },
                    onSelectCustomCategory = { viewModel.selectCustomCategory(it) },
                    onDismiss = { viewModel.setCategorySheetOpen(false) }
                )
            }

            if (state.isAddSubCategoryDialogOpen) {
                AddSubCategoryDialog(
                    categoryName = state.selectedCustomCategory ?: state.selectedCategory.displayName,
                    onConfirm = { name, color -> viewModel.addSubCategory(name, color) },
                    onDismiss = { viewModel.setAddSubCategoryDialogOpen(false) }
                )
            }

            if (state.isRefreshDialogOpen) {
                val context = LocalContext.current
                HomeRefreshDialog(
                    isSyncRunning = state.isSyncRunning,
                    syncStatusMessage = state.syncStatusMessage,
                    syncCompletedCount = state.syncCompletedCount,
                    syncTotalCount = state.syncTotalCount,
                    categoryName = state.selectedCustomCategory ?: state.selectedCategory.displayName,
                    selectedSubCategory = state.selectedSubCategory,
                    selectedStatus = state.selectedStatus,
                    currentTabCount = state.entries.size,
                    airingCount = state.categoryEntries.count { it.isAiring },
                    categoryTotalCount = state.categoryEntries.size,
                    libraryTotalCount = state.allUserEntries.size,
                    onStartRefresh = { scope, force -> viewModel.startRefresh(context, scope, force) },
                    onCancelSync = { viewModel.cancelSync(context) },
                    onDismiss = { viewModel.setRefreshDialogOpen(false) }
                )
            }

            subCategoryToDelete?.let { subCat ->
                AlertDialog(
                    onDismissRequest = { subCategoryToDelete = null },
                    title = {
                        Text("Delete Sub Category", color = LXTextPrimary, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text(
                            text = "Delete \"${subCat.name}\" from ${state.selectedCustomCategory ?: state.selectedCategory.displayName}?",
                            color = LXTextSecondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteSubCategory(subCat)
                                subCategoryToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LXErrorRed, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { subCategoryToDelete = null }) {
                            Text("Cancel", color = LXTextSecondary)
                        }
                    },
                    containerColor = LXSurfaceElevated
                )
            }
        }
    }
}

@Composable
fun AddSubCategoryDialog(
    categoryName: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val colorOptions = listOf("#A7EF60", "#4DA8DA", "#BB86FC", "#FF7597", "#FFD700", "#00E5FF", "#FF8A65")
    var selectedColor by remember { mutableStateOf(colorOptions.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Sub Category for $categoryName",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = LXTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Sub Category Name",
                    style = MaterialTheme.typography.labelMedium,
                    color = LXTextSecondary
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Favorites, Shounen, Isekai...", color = LXTextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LXPrimaryGreen,
                        unfocusedBorderColor = LXBorder,
                        focusedTextColor = LXTextPrimary,
                        unfocusedTextColor = LXTextPrimary,
                        cursorColor = LXPrimaryGreen
                    )
                )

                Text(
                    text = "Badge Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = LXTextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorOptions) { hex ->
                        val color = parseHexColor(hex)
                        val isChosen = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isChosen) 3.dp else 1.dp,
                                    color = if (isChosen) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChosen) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedColor)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Add Sub Category", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LXTextSecondary)
            }
        },
        containerColor = LXSurfaceElevated
    )
}

private fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        LXPrimaryGreen
    }
}

@Composable
private fun EmptyState(
    categoryName: String,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
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
            text = if (searchQuery.isNotBlank()) "No matches found for \"$searchQuery\"" else "No $categoryName entries yet",
            style = MaterialTheme.typography.titleMedium,
            color = LXTextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap + to create an entry or import from Settings",
            style = MaterialTheme.typography.bodySmall,
            color = LXTextSecondary.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun HomeRefreshDialog(
    isSyncRunning: Boolean,
    syncStatusMessage: String?,
    syncCompletedCount: Int,
    syncTotalCount: Int,
    categoryName: String,
    selectedSubCategory: String?,
    selectedStatus: MediaStatus?,
    currentTabCount: Int,
    airingCount: Int,
    categoryTotalCount: Int,
    libraryTotalCount: Int,
    onStartRefresh: (HomeRefreshScope, Boolean) -> Unit,
    onCancelSync: () -> Unit,
    onDismiss: () -> Unit
) {
    if (isSyncRunning) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                val infiniteTransition = rememberInfiniteTransition(label = "dialogSyncSpin")
                val spinAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "spin"
                )
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = LXPrimaryGreen,
                    modifier = Modifier.size(32.dp).rotate(spinAngle)
                )
            },
            title = {
                Text(
                    text = "Sync in Progress",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (syncTotalCount > 0) {
                        val progressFraction = (syncCompletedCount.toFloat() / syncTotalCount.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = LXPrimaryGreen,
                            trackColor = LXBorder
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$syncCompletedCount / $syncTotalCount processed",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextSecondary
                            )
                            Text(
                                text = "${(progressFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = LXPrimaryGreen
                            )
                        }
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = LXPrimaryGreen,
                            trackColor = LXBorder
                        )
                    }
                    Text(
                        text = syncStatusMessage ?: "Updating entries...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LXTextPrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelSync()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LXErrorRed.copy(alpha = 0.2f), contentColor = LXErrorRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel Sync", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
        return
    }

    var selectedScope by remember {
        mutableStateOf(
            if (currentTabCount > 0) HomeRefreshScope.CURRENT_VIEW
            else if (airingCount > 0) HomeRefreshScope.AIRING_ONLY
            else HomeRefreshScope.CURRENT_CATEGORY
        )
    }
    var forceRefresh by remember { mutableStateOf(false) }

    val tabTitle = when {
        selectedSubCategory != null -> "Subcategory: $selectedSubCategory"
        selectedStatus != null -> "Tab: ${selectedStatus.defaultLabel}"
        else -> "Current Tab"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = LXPrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Refresh Aired Episodes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select what to update for $categoryName:",
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary
                )

                // Option 1: Current View (Tab / Subcategory)
                RefreshOptionCard(
                    title = "$tabTitle ($currentTabCount)",
                    subtitle = "Refresh aired episodes & status for items in this view",
                    selected = selectedScope == HomeRefreshScope.CURRENT_VIEW,
                    enabled = currentTabCount > 0,
                    onClick = { selectedScope = HomeRefreshScope.CURRENT_VIEW }
                )

                // Option 2: Airing / Ongoing only in category
                RefreshOptionCard(
                    title = "Airing / Ongoing $categoryName ($airingCount)",
                    subtitle = "Fast update for newly released episodes & schedule",
                    selected = selectedScope == HomeRefreshScope.AIRING_ONLY,
                    enabled = airingCount > 0,
                    onClick = { selectedScope = HomeRefreshScope.AIRING_ONLY }
                )

                // Option 3: Entire category
                RefreshOptionCard(
                    title = "All $categoryName ($categoryTotalCount)",
                    subtitle = "Refresh all items in this category",
                    selected = selectedScope == HomeRefreshScope.CURRENT_CATEGORY,
                    enabled = categoryTotalCount > 0,
                    onClick = { selectedScope = HomeRefreshScope.CURRENT_CATEGORY }
                )

                // Option 4: Entire library
                RefreshOptionCard(
                    title = "Entire Library ($libraryTotalCount)",
                    subtitle = "Refresh all media across all categories",
                    selected = selectedScope == HomeRefreshScope.ALL_CATEGORIES,
                    enabled = libraryTotalCount > 0,
                    onClick = { selectedScope = HomeRefreshScope.ALL_CATEGORIES }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Force full metadata refresh checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { forceRefresh = !forceRefresh }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = forceRefresh,
                        onCheckedChange = { forceRefresh = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = LXPrimaryGreen,
                            uncheckedColor = LXTextSecondary,
                            checkmarkColor = LXBackgroundBlack
                        )
                    )
                    Column {
                        Text(
                            text = "Force full metadata refresh",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = LXTextPrimary
                        )
                        Text(
                            text = "Re-download synopsis, tags, score & details",
                            style = MaterialTheme.typography.bodySmall,
                            color = LXTextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            val count = when (selectedScope) {
                HomeRefreshScope.CURRENT_VIEW -> currentTabCount
                HomeRefreshScope.AIRING_ONLY -> airingCount
                HomeRefreshScope.CURRENT_CATEGORY -> categoryTotalCount
                HomeRefreshScope.ALL_CATEGORIES -> libraryTotalCount
            }
            Button(
                onClick = {
                    onStartRefresh(selectedScope, forceRefresh)
                    onDismiss()
                },
                enabled = count > 0,
                colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (count > 0) "Refresh ($count)" else "Refresh",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LXTextSecondary)
            }
        },
        containerColor = LXSurfaceElevated,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun RefreshOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val borderColor = if (selected) LXPrimaryGreen else LXBorder
    val bgColor = if (selected) LXPrimaryGreen.copy(alpha = 0.1f) else LXSurfaceBlack
    val alpha = if (enabled) 1f else 0.4f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = if (enabled) onClick else null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = LXPrimaryGreen,
                unselectedColor = LXTextSecondary
            )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) LXPrimaryGreen else LXTextPrimary.copy(alpha = alpha)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LXTextSecondary.copy(alpha = alpha)
            )
        }
    }
}
