package com.lx.tracklist.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lx.tracklist.R
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.remote.TmdbConfig
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXScoreGold
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var itemToAdd by remember { mutableStateOf<SearchResultItem?>(null) }

    LaunchedEffect(state.userAddedMessage) {
        state.userAddedMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearAddedMessage()
        }
    }

    if (itemToAdd != null) {
        AddToLibraryDialog(
            item = itemToAdd!!,
            customCategories = state.customCategories.filter { it.mediaType == itemToAdd!!.mediaType }.map { it.name },
            subCategories = state.subCategories.map { it.name },
            onConfirm = { status, customCat, subCat ->
                viewModel.addToLibrary(
                    item = itemToAdd!!,
                    status = status,
                    customCategory = customCat,
                    subCategory = subCat,
                    context = context
                )
                itemToAdd = null
            },
            onDismiss = { itemToAdd = null }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LXBackgroundBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Search & Discover",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LXSurfaceElevated)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Input Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.onQueryChanged(it, context) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = when (state.selectedCategory) {
                                SearchCategory.ANIME -> "Search anime titles on MyAnimeList..."
                                SearchCategory.MANGA -> "Search manga titles on MyAnimeList..."
                                SearchCategory.MANHWA -> "Search manhwa titles on MyAnimeList..."
                                SearchCategory.MOVIES -> "Search movies on TMDB..."
                                SearchCategory.TV_SHOWS -> "Search TV shows on TMDB..."
                            },
                            color = LXTextSecondary.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = LXPrimaryGreen
                        )
                    },
                    trailingIcon = {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = LXPrimaryGreen,
                                strokeWidth = 2.dp
                            )
                        } else if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChanged("", context) }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = LXTextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LXSurfaceBlack,
                        unfocusedContainerColor = LXSurfaceBlack,
                        focusedBorderColor = LXPrimaryGreen,
                        unfocusedBorderColor = LXBorder,
                        focusedTextColor = LXTextPrimary,
                        unfocusedTextColor = LXTextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.submitSearch(context) })
                )
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchCategory.entries.forEach { category ->
                    val isSelected = state.selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onCategorySelected(category, context) },
                        label = {
                            Text(
                                text = "${category.displayName} (${category.sourceName})",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen,
                            selectedLabelColor = LXBackgroundBlack,
                            containerColor = LXSurfaceBlack,
                            labelColor = LXTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) LXPrimaryGreen else LXBorder
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // TMDB / Jikan Attribution Strip
            if (state.selectedCategory == SearchCategory.MOVIES || state.selectedCategory == SearchCategory.TV_SHOWS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceBlack)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_tmdb_logo),
                        contentDescription = "TMDB",
                        modifier = Modifier.height(16.dp).width(24.dp)
                    )
                    Text(
                        text = TmdbConfig.ATTRIBUTION_TEXT,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = LXTextSecondary.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceBlack)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Results fetched from MyAnimeList via Jikan API",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = LXTextSecondary.copy(alpha = 0.75f)
                    )
                }
            }

            // Results List / Empty States
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading && state.results.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = LXPrimaryGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Searching ${state.selectedCategory.displayName} on ${state.selectedCategory.sourceName}...",
                            color = LXTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (state.results.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (state.selectedCategory) {
                                SearchCategory.MOVIES, SearchCategory.TV_SHOWS -> Icons.Default.Movie
                                else -> Icons.Default.Search
                            },
                            contentDescription = null,
                            tint = LXTextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (state.query.isBlank()) {
                                "Search ${state.selectedCategory.displayName} on ${state.selectedCategory.sourceName}"
                            } else {
                                state.errorMessage ?: "No results found for '${state.query}'"
                            },
                            color = LXTextSecondary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (state.query.isBlank()) {
                                "Type at least 2 characters to discover and add entries directly to your library."
                            } else {
                                "Try searching by alternate title, romaji, or English name."
                            },
                            color = LXTextSecondary.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.results, key = { "${it.externalSource}_${it.externalId}" }) { item ->
                            SearchResultCard(
                                item = item,
                                onAddClick = { itemToAdd = item },
                                onViewClick = { item.existingEntryId?.let(onNavigateToDetail) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: SearchResultItem,
    onAddClick: () -> Unit,
    onViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
        border = BorderStroke(1.dp, if (item.isAlreadyInLibrary) LXPrimaryGreen.copy(alpha = 0.4f) else LXBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Poster thumbnail
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LXSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (!item.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (item.mediaType == MediaType.MOVIE) Icons.Default.Movie else Icons.Default.Tv,
                        contentDescription = null,
                        tint = LXTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Score pill overlay
                if (item.score != null && item.score > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = LXScoreGold,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", item.score),
                                color = LXScoreGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Info & Actions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!item.alternateTitle.isNullOrBlank() && !item.alternateTitle.equals(item.title, ignoreCase = true)) {
                        Text(
                            text = item.alternateTitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LXTextSecondary.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Metadata row: Year • Status • Units
                    val metaParts = mutableListOf<String>()
                    item.releaseYear?.let { metaParts.add(it) }
                    item.statusText?.let { metaParts.add(it) }
                    item.totalUnits?.takeIf { it > 0 }?.let {
                        val unitLabel = when (item.mediaType) {
                            MediaType.ANIME -> if (it == 1) "1 Ep" else "$it Eps"
                            MediaType.MANGA, MediaType.MANHWA -> if (it == 1) "1 Ch" else "$it Chs"
                            MediaType.SHOW -> if (it == 1) "1 Ep" else "$it Eps"
                            else -> null
                        }
                        if (unitLabel != null) metaParts.add(unitLabel)
                    }
                    item.durationMinutes?.takeIf { it > 0 }?.let {
                        metaParts.add("${it}m")
                    }

                    if (metaParts.isNotEmpty()) {
                        Text(
                            text = metaParts.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LXTextSecondary
                        )
                    }
                }

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.isAlreadyInLibrary) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LXPrimaryGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable { onViewClick() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "In Library (${item.existingStatus?.defaultLabel ?: "Saved"})",
                                    color = LXPrimaryGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onAddClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LXPrimaryGreen,
                                contentColor = LXBackgroundBlack
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add to Library",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToLibraryDialog(
    item: SearchResultItem,
    customCategories: List<String>,
    subCategories: List<String>,
    onConfirm: (MediaStatus, String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStatus by remember { mutableStateOf(MediaStatus.PLAN_TO_WATCH) }
    var selectedCustomCategory by remember { mutableStateOf<String?>(null) }
    var selectedSubCategory by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add to Library",
                color = LXTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Item Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!item.posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp, 64.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.mediaType.displayName} • ${item.externalSource}",
                            style = MaterialTheme.typography.bodySmall,
                            color = LXPrimaryGreen
                        )
                    }
                }

                // Status Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = LXTextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MediaStatus.entries.forEach { status ->
                            val isSelected = selectedStatus == status
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) LXPrimaryGreen else LXSurfaceElevated,
                                border = BorderStroke(1.dp, if (isSelected) LXPrimaryGreen else LXBorder),
                                modifier = Modifier.clickable { selectedStatus = status }
                            ) {
                                Text(
                                    text = status.defaultLabel,
                                    color = if (isSelected) LXBackgroundBlack else LXTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Custom Category (if user has any)
                if (customCategories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Custom Category (Optional)",
                            style = MaterialTheme.typography.labelMedium,
                            color = LXTextSecondary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedCustomCategory == null) LXPrimaryGreen else LXSurfaceElevated,
                                border = BorderStroke(1.dp, if (selectedCustomCategory == null) LXPrimaryGreen else LXBorder),
                                modifier = Modifier.clickable { selectedCustomCategory = null }
                            ) {
                                Text(
                                    text = "Standard (${item.mediaType.displayName})",
                                    color = if (selectedCustomCategory == null) LXBackgroundBlack else LXTextPrimary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            customCategories.forEach { cat ->
                                val isSelected = selectedCustomCategory == cat
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) LXPrimaryGreen else LXSurfaceElevated,
                                    border = BorderStroke(1.dp, if (isSelected) LXPrimaryGreen else LXBorder),
                                    modifier = Modifier.clickable { selectedCustomCategory = cat }
                                ) {
                                    Text(
                                        text = cat,
                                        color = if (isSelected) LXBackgroundBlack else LXTextPrimary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedStatus, selectedCustomCategory, selectedSubCategory)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LXPrimaryGreen,
                    contentColor = LXBackgroundBlack
                )
            ) {
                Text("Add to Library", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LXTextSecondary)
            }
        },
        containerColor = LXSurfaceElevated,
        shape = RoundedCornerShape(14.dp)
    )
}
