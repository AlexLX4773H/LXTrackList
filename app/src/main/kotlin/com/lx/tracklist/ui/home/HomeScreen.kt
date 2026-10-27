package com.lx.tracklist.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.ui.components.CategorySelectorSheet
import com.lx.tracklist.ui.components.MediaCard
import com.lx.tracklist.ui.components.StatusFilterTabs
import com.lx.tracklist.ui.components.TopNavBar
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXTextSecondary

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onEntryClick: (Long) -> Unit,
    onAddNewEntry: () -> Unit,
    onEditEntry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LXBackgroundBlack,
        topBar = {
            Column {
                TopNavBar(
                    selectedCategory = state.selectedCategory,
                    onCategoryClick = { viewModel.setCategorySheetOpen(true) },
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    isSearchActive = state.isSearchActive,
                    onToggleSearch = { viewModel.toggleSearch() },
                    onRefresh = { /* UI flows are automatically reactive */ },
                    selectedSort = state.selectedSort,
                    onSortSelected = { viewModel.selectSort(it) }
                )

                StatusFilterTabs(
                    selectedCategory = state.selectedCategory,
                    selectedStatus = state.selectedStatus,
                    statusCounts = state.statusCounts,
                    totalCount = state.totalCount,
                    onStatusSelected = { viewModel.selectStatus(it) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewEntry,
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
            if (state.entries.isEmpty()) {
                EmptyState(
                    categoryName = state.selectedCategory.displayName,
                    searchQuery = state.searchQuery
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
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
                            onEdit = { onEditEntry(entry.id) }
                        )
                    }
                }
            }

            if (state.isCategorySheetOpen) {
                CategorySelectorSheet(
                    selectedCategory = state.selectedCategory,
                    categoryCounts = state.categoryCounts,
                    onSelectCategory = { viewModel.selectCategory(it) },
                    onDismiss = { viewModel.setCategorySheetOpen(false) }
                )
            }
        }
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
            text = "Tap the + button to add one or import from Settings",
            style = MaterialTheme.typography.bodySmall,
            color = LXTextSecondary.copy(alpha = 0.7f)
        )
    }
}
