package com.lx.tracklist.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.ui.components.MediaCard
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXErrorRed
import com.lx.tracklist.ui.theme.LXPrimaryGreen
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
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val visibleIds = remember(state.entries) { state.entries.map { it.id } }

    // Intercept back button when in selection mode
    BackHandler(enabled = state.isSelectionMode) {
        viewModel.clearSelection()
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LXBackgroundBlack,
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
                placeholder = { Text("Search all media across all categories...", color = LXTextSecondary) },
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

            // Category, Custom Category, and Favorite Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.onlyFavorites,
                        onClick = { viewModel.toggleFavoritesFilter() },
                        label = { Text("Favorites") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if (state.onlyFavorites) LXBackgroundBlack else LXPrimaryGreen
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
                        label = { Text("All Categories") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen,
                            selectedLabelColor = LXBackgroundBlack,
                            containerColor = LXSurfaceElevated,
                            labelColor = LXTextSecondary
                        )
                    )
                }

                items(MediaType.entries) { cat ->
                    FilterChip(
                        selected = state.selectedCategoryFilter == cat && state.selectedCustomCategoryFilter == null,
                        onClick = { viewModel.setCategoryFilter(cat) },
                        label = { Text(cat.displayName) },
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
                    FilterChip(
                        selected = state.selectedCustomCategoryFilter == customCat.name,
                        onClick = { viewModel.setCustomCategoryFilter(customCat.name) },
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

            Spacer(modifier = Modifier.height(4.dp))

            // Responsive Adaptive Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 360.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = state.entries,
                    key = { it.id }
                ) { entry ->
                    val isSelected = state.selectedIds.contains(entry.id)
                    MediaCard(
                        entry = entry,
                        onClick = {
                            if (state.isSelectionMode) {
                                viewModel.toggleSelect(entry.id)
                            } else {
                                onEntryClick(entry.id)
                            }
                        },
                        onLongClick = {
                            viewModel.toggleSelect(entry.id)
                        },
                        isSelectionMode = state.isSelectionMode,
                        isSelected = isSelected,
                        onToggleSelect = {
                            viewModel.toggleSelect(entry.id)
                        },
                        onIncrement = { viewModel.incrementProgress(entry) },
                        onToggleFavorite = { viewModel.toggleFavorite(entry) },
                        onDelete = { viewModel.deleteEntry(entry) },
                        onEdit = { onEditEntry(entry.id) }
                    )
                }
            }
        }
    }
}
