package com.lx.tracklist.ui.home

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
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
        containerColor = LXBackgroundBlack,
        contentWindowInsets = if (isTablet) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopNavBar(
                    selectedCategory = state.selectedCategory,
                    customCategoryTitle = state.selectedCustomCategory,
                    onCategoryClick = { viewModel.setCategorySheetOpen(true) },
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    isSearchActive = state.isSearchActive,
                    onToggleSearch = { viewModel.toggleSearch() },
                    onRefresh = { /* Flows auto-update */ },
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
            FloatingActionButton(
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
                containerColor = LXPrimaryGreen,
                contentColor = LXBackgroundBlack,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry", modifier = Modifier.size(28.dp))
            }
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
                        columns = GridCells.Adaptive(minSize = 360.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
