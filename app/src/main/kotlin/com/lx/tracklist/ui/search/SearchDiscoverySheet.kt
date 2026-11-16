package com.lx.tracklist.ui.search

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.ui.library.TagSelectionState
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary

private data class TagDisplayItem(
    val name: String,
    val categoryLabel: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchDiscoverySheet(
    category: SearchCategory,
    activeFilter: DiscoveryFilter?,
    onDismiss: () -> Unit,
    onApplyAnime: (DiscoveryFilter.Anime) -> Unit,
    onApplyMovie: (DiscoveryFilter.Movie) -> Unit,
    onApplyTv: (DiscoveryFilter.Tv) -> Unit,
    onResetFilter: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentYear = AnimeSeason.currentYear()
    val maxYear = AnimeSeason.maxDiscoveryYear() // currentYear + 2

    // Anime state
    val animeFilter = activeFilter as? DiscoveryFilter.Anime
    var selectedAnimeMode by remember {
        mutableStateOf(animeFilter?.mode)
    }
    var selectedSeason by remember {
        mutableStateOf(animeFilter?.season ?: AnimeSeason.current())
    }
    var selectedAnimeYear by remember {
        mutableIntStateOf(animeFilter?.year ?: currentYear)
    }
    var selectedAnimeSort by remember {
        mutableStateOf(animeFilter?.sortOrder ?: AnimeSortOrder.POPULARITY)
    }
    var includedTags by remember {
        mutableStateOf(animeFilter?.includedTags ?: emptySet())
    }
    var excludedTags by remember {
        mutableStateOf(animeFilter?.excludedTags ?: emptySet())
    }

    // Movie state
    val movieFilter = activeFilter as? DiscoveryFilter.Movie
    var selectedMovieMode by remember {
        mutableStateOf(movieFilter?.mode)
    }
    var selectedMovieYear by remember {
        mutableIntStateOf(movieFilter?.year ?: currentYear)
    }
    var selectedMovieSort by remember {
        mutableStateOf(movieFilter?.sortOrder ?: TmdbMovieSort.POPULARITY)
    }
    var movieIncludedTags by remember {
        mutableStateOf(movieFilter?.includedTags ?: emptySet())
    }
    var movieExcludedTags by remember {
        mutableStateOf(movieFilter?.excludedTags ?: emptySet())
    }

    // TV state
    val tvFilter = activeFilter as? DiscoveryFilter.Tv
    var selectedTvMode by remember {
        mutableStateOf(tvFilter?.mode)
    }
    var selectedTvYear by remember {
        mutableIntStateOf(tvFilter?.year ?: currentYear)
    }
    var selectedTvSort by remember {
        mutableStateOf(tvFilter?.sortOrder ?: TmdbTvSort.POPULARITY)
    }
    var tvIncludedTags by remember {
        mutableStateOf(tvFilter?.includedTags ?: emptySet())
    }
    var tvExcludedTags by remember {
        mutableStateOf(tvFilter?.excludedTags ?: emptySet())
    }

    var adultMode by remember {
        mutableStateOf(activeFilter?.adultMode ?: AdultMode.OFF)
    }

    var yearDropdownExpanded by remember { mutableStateOf(false) }
    var tagSearchQuery by remember { mutableStateOf("") }
    var selectedTagCategory by remember { mutableStateOf<AnimeTagCategory?>(null) }
    var showTagsSubMenu by remember { mutableStateOf(false) }

    val activeIncludedTags: Set<String> = when (category) {
        SearchCategory.ANIME -> includedTags
        SearchCategory.MOVIES -> movieIncludedTags
        SearchCategory.TV_SHOWS -> tvIncludedTags
        else -> emptySet()
    }

    val activeExcludedTags: Set<String> = when (category) {
        SearchCategory.ANIME -> excludedTags
        SearchCategory.MOVIES -> movieExcludedTags
        SearchCategory.TV_SHOWS -> tvExcludedTags
        else -> emptySet()
    }

    BackHandler(enabled = showTagsSubMenu) {
        showTagsSubMenu = false
    }

    fun cycleTag(tagName: String) {
        val clean = tagName.trim()
        when (category) {
            SearchCategory.ANIME -> {
                val isInc = includedTags.any { it.equals(clean, ignoreCase = true) }
                val isExc = excludedTags.any { it.equals(clean, ignoreCase = true) }
                when {
                    !isInc && !isExc -> {
                        includedTags = includedTags + clean
                        excludedTags = excludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                    isInc -> {
                        includedTags = includedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        excludedTags = excludedTags + clean
                    }
                    else -> {
                        includedTags = includedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        excludedTags = excludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                }
            }
            SearchCategory.MOVIES -> {
                val isInc = movieIncludedTags.any { it.equals(clean, ignoreCase = true) }
                val isExc = movieExcludedTags.any { it.equals(clean, ignoreCase = true) }
                when {
                    !isInc && !isExc -> {
                        movieIncludedTags = movieIncludedTags + clean
                        movieExcludedTags = movieExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                    isInc -> {
                        movieIncludedTags = movieIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        movieExcludedTags = movieExcludedTags + clean
                    }
                    else -> {
                        movieIncludedTags = movieIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        movieExcludedTags = movieExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                }
            }
            SearchCategory.TV_SHOWS -> {
                val isInc = tvIncludedTags.any { it.equals(clean, ignoreCase = true) }
                val isExc = tvExcludedTags.any { it.equals(clean, ignoreCase = true) }
                when {
                    !isInc && !isExc -> {
                        tvIncludedTags = tvIncludedTags + clean
                        tvExcludedTags = tvExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                    isInc -> {
                        tvIncludedTags = tvIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        tvExcludedTags = tvExcludedTags + clean
                    }
                    else -> {
                        tvIncludedTags = tvIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        tvExcludedTags = tvExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                }
            }
            else -> {}
        }
    }

    fun getTagState(tagName: String): TagSelectionState {
        val clean = tagName.trim()
        return when {
            activeIncludedTags.any { it.equals(clean, ignoreCase = true) } -> TagSelectionState.INCLUDED
            activeExcludedTags.any { it.equals(clean, ignoreCase = true) } -> TagSelectionState.EXCLUDED
            else -> TagSelectionState.UNSELECTED
        }
    }

    fun clearAllCurrentTags() {
        when (category) {
            SearchCategory.ANIME -> {
                includedTags = emptySet()
                excludedTags = emptySet()
            }
            SearchCategory.MOVIES -> {
                movieIncludedTags = emptySet()
                movieExcludedTags = emptySet()
            }
            SearchCategory.TV_SHOWS -> {
                tvIncludedTags = emptySet()
                tvExcludedTags = emptySet()
            }
            else -> {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LXSurfaceBlack,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
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
            // ================= TAGS SEPARATE PAGE (Like Library Advanced Search) =================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
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
                            text = when (category) {
                                SearchCategory.ANIME -> "Tags & Genres"
                                SearchCategory.MOVIES -> "Movie Genres"
                                SearchCategory.TV_SHOWS -> "TV Show Genres"
                                else -> "Tags & Genres"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }

                    if (activeIncludedTags.isNotEmpty() || activeExcludedTags.isNotEmpty()) {
                        TextButton(onClick = { clearAllCurrentTags() }) {
                            Text("Clear All", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
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
                                .background(Color(0xFFEF4444), RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                        }
                        Text("2 taps = Exclude", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "${activeIncludedTags.size} inc • ${activeExcludedTags.size} exc",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (activeIncludedTags.isNotEmpty() || activeExcludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary
                    )
                }

                // Search Bar for Tags
                OutlinedTextField(
                    value = tagSearchQuery,
                    onValueChange = { tagSearchQuery = it },
                    placeholder = {
                        Text(
                            text = if (category == SearchCategory.ANIME) "Search tags, genres, themes..." else "Search genres...",
                            color = LXTextSecondary,
                            fontSize = 13.sp
                        )
                    },
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LXPrimaryGreen,
                        unfocusedBorderColor = LXBorder,
                        focusedContainerColor = LXSurfaceElevated,
                        unfocusedContainerColor = LXSurfaceElevated,
                        focusedTextColor = LXTextPrimary,
                        unfocusedTextColor = LXTextPrimary
                    )
                )

                // Category filter chips (All, Genres, Themes, Demographics) - Anime only
                if (category == SearchCategory.ANIME) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val categories = listOf<AnimeTagCategory?>(null, AnimeTagCategory.GENRE, AnimeTagCategory.THEME, AnimeTagCategory.DEMOGRAPHIC)
                        categories.forEach { cat ->
                            val label = cat?.displayName ?: "All"
                            val isCatSelected = selectedTagCategory == cat
                            FilterChip(
                                selected = isCatSelected,
                                onClick = { selectedTagCategory = cat },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = LXPrimaryGreen,
                                    containerColor = LXSurfaceElevated,
                                    labelColor = LXTextSecondary
                                ),
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }
                }

                // Tag list with tri-state selection (Matching Library Advanced Filter)
                val visibleItems = remember(category, tagSearchQuery, selectedTagCategory) {
                    when (category) {
                        SearchCategory.ANIME -> AnimeTagRegistry.filterTags(tagSearchQuery, selectedTagCategory).map {
                            TagDisplayItem(it.name, it.category.displayName)
                        }
                        SearchCategory.MOVIES -> TmdbTagRegistry.filterMovieGenres(tagSearchQuery).map {
                            TagDisplayItem(it.name, "Genre")
                        }
                        SearchCategory.TV_SHOWS -> TmdbTagRegistry.filterTvGenres(tagSearchQuery).map {
                            TagDisplayItem(it.name, "Genre")
                        }
                        else -> emptyList()
                    }
                }

                if (visibleItems.isEmpty()) {
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
                        visibleItems.forEach { tagDef ->
                            val tagState = getTagState(tagDef.name)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (tagState) {
                                            TagSelectionState.INCLUDED -> LXPrimaryGreen.copy(alpha = 0.12f)
                                            TagSelectionState.EXCLUDED -> Color(0xFFEF4444).copy(alpha = 0.12f)
                                            TagSelectionState.UNSELECTED -> LXSurfaceElevated
                                        }
                                    )
                                    .border(
                                        width = if (tagState != TagSelectionState.UNSELECTED) 1.dp else 0.5.dp,
                                        color = when (tagState) {
                                            TagSelectionState.INCLUDED -> LXPrimaryGreen.copy(alpha = 0.6f)
                                            TagSelectionState.EXCLUDED -> Color(0xFFEF4444).copy(alpha = 0.6f)
                                            TagSelectionState.UNSELECTED -> LXBorder
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { cycleTag(tagDef.name) }
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
                                        text = tagDef.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (tagState != TagSelectionState.UNSELECTED) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = when (tagState) {
                                            TagSelectionState.INCLUDED -> LXPrimaryGreen
                                            TagSelectionState.EXCLUDED -> Color(0xFFEF4444)
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
                                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Excluded",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    color = Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                        TagSelectionState.UNSELECTED -> {
                                            Text(
                                                text = tagDef.categoryLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = LXTextSecondary.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { showTagsSubMenu = false },
                    colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Done (${activeIncludedTags.size} included • ${activeExcludedTags.size} excluded)",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            // ================= MAIN DISCOVERY SHEET =================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (category) {
                                SearchCategory.ANIME -> when (selectedAnimeMode) {
                                    AnimeDiscoveryMode.SEASONAL -> Icons.Default.Explore
                                    AnimeDiscoveryMode.MOST_POPULAR -> Icons.Default.LocalFireDepartment
                                    null -> Icons.Default.Explore
                                }
                                SearchCategory.MOVIES -> when (selectedMovieMode) {
                                    TmdbDiscoveryMode.BY_YEAR -> Icons.Default.Movie
                                    TmdbDiscoveryMode.MOST_POPULAR -> Icons.Default.LocalFireDepartment
                                    null -> Icons.Default.Movie
                                }
                                SearchCategory.TV_SHOWS -> when (selectedTvMode) {
                                    TmdbDiscoveryMode.BY_YEAR -> Icons.Default.Tv
                                    TmdbDiscoveryMode.MOST_POPULAR -> Icons.Default.LocalFireDepartment
                                    null -> Icons.Default.Tv
                                }
                                else -> Icons.Default.Explore
                            },
                            contentDescription = null,
                            tint = LXPrimaryGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (category) {
                                SearchCategory.ANIME -> when (selectedAnimeMode) {
                                    AnimeDiscoveryMode.SEASONAL -> "Seasonal Anime"
                                    AnimeDiscoveryMode.MOST_POPULAR -> "Most Popular Anime"
                                    null -> "Discover Anime"
                                }
                                SearchCategory.MOVIES -> when (selectedMovieMode) {
                                    TmdbDiscoveryMode.BY_YEAR -> "Discover Movies"
                                    TmdbDiscoveryMode.MOST_POPULAR -> "Most Popular Movies"
                                    null -> "Discover Movies"
                                }
                                SearchCategory.TV_SHOWS -> when (selectedTvMode) {
                                    TmdbDiscoveryMode.BY_YEAR -> "Discover TV Shows"
                                    TmdbDiscoveryMode.MOST_POPULAR -> "Most Popular TV Shows"
                                    null -> "Discover TV Shows"
                                }
                                else -> "Discover"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }

                    if (activeFilter != null) {
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    onResetFilter()
                                    onDismiss()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (category) {
                    SearchCategory.ANIME -> {
                        // 1. Mode Selector: Small Icon Pills (Seasonal / Most Popular)
                        Text(
                            text = "Discovery Mode",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnimeDiscoveryMode.entries.forEach { mode ->
                                val isSelected = selectedAnimeMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSelected) LXPrimaryGreen.copy(alpha = 0.2f) else LXSurfaceElevated)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) LXPrimaryGreen else LXBorder,
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable { selectedAnimeMode = mode },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    ) {
                                        Text(mode.iconEmoji, fontSize = 13.sp)
                                        Text(
                                            text = mode.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Load rest of the controls ONLY once a Mode is clicked!
                        if (selectedAnimeMode == null) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LXSurfaceElevated)
                                    .padding(vertical = 18.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Select a mode above to customize discovery",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LXTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Seasonal Controls (only when Seasonal mode is active)
                            if (selectedAnimeMode == AnimeDiscoveryMode.SEASONAL) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val (nextSeason, nextYear) = AnimeSeason.nextSeason()
                                    OutlinedButton(
                                        onClick = {
                                            selectedSeason = AnimeSeason.current()
                                            selectedAnimeYear = currentYear
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                    ) {
                                        Text("Current Season", fontSize = 11.sp, maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            selectedSeason = nextSeason
                                            selectedAnimeYear = nextYear
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                    ) {
                                        Text("Upcoming", fontSize = 11.sp, maxLines = 1)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Season Selector (2x2 Grid)
                                Text(
                                    text = "Season",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LXTextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SeasonSelectorBox(
                                        season = AnimeSeason.WINTER,
                                        isSelected = selectedSeason == AnimeSeason.WINTER,
                                        modifier = Modifier.weight(1f)
                                    ) { selectedSeason = AnimeSeason.WINTER }

                                    SeasonSelectorBox(
                                        season = AnimeSeason.SPRING,
                                        isSelected = selectedSeason == AnimeSeason.SPRING,
                                        modifier = Modifier.weight(1f)
                                    ) { selectedSeason = AnimeSeason.SPRING }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SeasonSelectorBox(
                                        season = AnimeSeason.SUMMER,
                                        isSelected = selectedSeason == AnimeSeason.SUMMER,
                                        modifier = Modifier.weight(1f)
                                    ) { selectedSeason = AnimeSeason.SUMMER }

                                    SeasonSelectorBox(
                                        season = AnimeSeason.FALL,
                                        isSelected = selectedSeason == AnimeSeason.FALL,
                                        modifier = Modifier.weight(1f)
                                    ) { selectedSeason = AnimeSeason.FALL }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Year Selector
                                Text(
                                    text = "Year: $selectedAnimeYear",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LXTextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val quickAnimeYears = listOf(maxYear, maxYear - 1, maxYear - 2, maxYear - 3)
                                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        quickAnimeYears.forEach { yr ->
                                            val isSelected = selectedAnimeYear == yr
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { selectedAnimeYear = yr },
                                                label = { Text(yr.toString(), fontSize = 11.sp) },
                                                modifier = Modifier.height(30.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                                    selectedLabelColor = LXPrimaryGreen,
                                                    containerColor = LXSurfaceElevated,
                                                    labelColor = LXTextSecondary
                                                )
                                            )
                                        }

                                        // Dropdown for more years
                                        Box {
                                            FilterChip(
                                                selected = selectedAnimeYear !in quickAnimeYears,
                                                onClick = { yearDropdownExpanded = true },
                                                modifier = Modifier.height(30.dp),
                                                label = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            if (selectedAnimeYear !in quickAnimeYears) selectedAnimeYear.toString() else "More",
                                                            fontSize = 11.sp
                                                        )
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    }
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                                    selectedLabelColor = LXPrimaryGreen,
                                                    containerColor = LXSurfaceElevated,
                                                    labelColor = LXTextSecondary
                                                )
                                            )

                                            DropdownMenu(
                                                expanded = yearDropdownExpanded,
                                                onDismissRequest = { yearDropdownExpanded = false },
                                                modifier = Modifier.background(LXSurfaceElevated)
                                            ) {
                                                (maxYear downTo 1990).forEach { yr ->
                                                    DropdownMenuItem(
                                                        text = { Text(yr.toString(), color = LXTextPrimary) },
                                                        onClick = {
                                                            selectedAnimeYear = yr
                                                            yearDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Sort Order
                            Text(
                                text = "Sort By",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = LXTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    AnimeSortOrder.entries.forEach { sort ->
                                        val isSelected = selectedAnimeSort == sort
                                        val shortLabel = when (sort) {
                                            AnimeSortOrder.POPULARITY -> "Popular"
                                            AnimeSortOrder.SCORE -> "Top Rated"
                                            AnimeSortOrder.AIR_DATE -> "Release Date"
                                            AnimeSortOrder.TITLE -> "Title (A–Z)"
                                        }
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedAnimeSort = sort },
                                            label = { Text(shortLabel, fontSize = 11.sp) },
                                            modifier = Modifier.height(30.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                                selectedLabelColor = LXPrimaryGreen,
                                                containerColor = LXSurfaceElevated,
                                                labelColor = LXTextSecondary
                                            )
                                        )
                                    }
                                }
                            }

                            // Tags Section Button (Separate Page Navigation)
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LXSurfaceElevated)
                                    .border(1.dp, LXBorder, RoundedCornerShape(10.dp))
                                    .clickable { showTagsSubMenu = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(LXPrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tag,
                                                contentDescription = null,
                                                tint = LXPrimaryGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Select & Exclude Tags",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                                color = LXTextPrimary
                                            )
                                            Text(
                                                text = if (includedTags.isEmpty() && excludedTags.isEmpty())
                                                    "Filter by genres, themes, demographics"
                                                else
                                                    "${includedTags.size} included (✓) • ${excludedTags.size} excluded (✕)",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = if (includedTags.isNotEmpty() || excludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = "Open Tags Filter",
                                        tint = LXPrimaryGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Active Tag Chips inside Main Sheet with 1-tap delete
                                if (includedTags.isNotEmpty() || excludedTags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        includedTags.forEach { tag ->
                                            SearchTagBadge(
                                                label = "+$tag",
                                                isIncluded = true,
                                                onRemove = {
                                                    includedTags = includedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                                }
                                            )
                                        }
                                        excludedTags.forEach { tag ->
                                            SearchTagBadge(
                                                label = "-$tag",
                                                isIncluded = false,
                                                onRemove = {
                                                    excludedTags = excludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 18+ Subtle Checkbox Toggle
                            SubtleAdultFilterToggle(
                                adultMode = adultMode,
                                onModeChange = { adultMode = it }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Apply Button
                            Button(
                                onClick = {
                                    val filter = DiscoveryFilter.Anime(
                                        mode = selectedAnimeMode!!,
                                        season = selectedSeason,
                                        year = selectedAnimeYear,
                                        sortOrder = selectedAnimeSort,
                                        includedTags = includedTags,
                                        excludedTags = excludedTags,
                                        adultMode = adultMode
                                    )
                                    onApplyAnime(filter)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                val modeText = when (selectedAnimeMode!!) {
                                    AnimeDiscoveryMode.SEASONAL -> "Explore ${selectedSeason.displayName} $selectedAnimeYear"
                                    AnimeDiscoveryMode.MOST_POPULAR -> "Explore Most Popular Anime"
                                }
                                val tagsText = when {
                                    includedTags.isNotEmpty() || excludedTags.isNotEmpty() -> " (+${includedTags.size}/-${excludedTags.size})"
                                    else -> ""
                                }
                                val adultText = if (adultMode == AdultMode.ONLY) " (18+ Only)" else ""
                                Text(
                                    text = "$modeText$tagsText$adultText",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                SearchCategory.MOVIES -> {
                    // 1. Mode Selector: Small Icon Pills (By Year / Most Popular)
                    Text(
                        text = "Discovery Mode",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TmdbDiscoveryMode.entries.forEach { mode ->
                            val isSelected = selectedMovieMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedMovieMode = mode },
                                color = if (isSelected) LXPrimaryGreen.copy(alpha = 0.2f) else LXSurfaceElevated,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) LXPrimaryGreen else LXBorder
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = mode.iconEmoji,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Only load the rest once a mode is clicked!
                    if (selectedMovieMode != null) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Year Selector (only for BY_YEAR)
                        if (selectedMovieMode == TmdbDiscoveryMode.BY_YEAR) {
                            Text(
                                text = "Release Year: $selectedMovieYear",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = LXTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val quickMovieYears = listOf(maxYear, maxYear - 1, maxYear - 2, maxYear - 3)
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    quickMovieYears.forEach { yr ->
                                        val isSelected = selectedMovieYear == yr
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedMovieYear = yr },
                                            label = { Text(yr.toString(), fontSize = 11.sp) },
                                            modifier = Modifier.height(30.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                                selectedLabelColor = LXPrimaryGreen,
                                                containerColor = LXSurfaceElevated,
                                                labelColor = LXTextSecondary
                                            )
                                        )
                                    }

                                    // Dropdown for more movie years
                                    Box {
                                        FilterChip(
                                            selected = selectedMovieYear !in quickMovieYears,
                                            onClick = { yearDropdownExpanded = true },
                                            modifier = Modifier.height(30.dp),
                                            label = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        if (selectedMovieYear !in quickMovieYears) selectedMovieYear.toString() else "More",
                                                        fontSize = 11.sp
                                                    )
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                                }
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                                selectedLabelColor = LXPrimaryGreen,
                                                containerColor = LXSurfaceElevated,
                                                labelColor = LXTextSecondary
                                            )
                                        )

                                        DropdownMenu(
                                            expanded = yearDropdownExpanded,
                                            onDismissRequest = { yearDropdownExpanded = false },
                                            modifier = Modifier.background(LXSurfaceElevated)
                                        ) {
                                            (maxYear downTo 1970).forEach { yr ->
                                                DropdownMenuItem(
                                                    text = { Text(yr.toString(), color = LXTextPrimary) },
                                                    onClick = {
                                                        selectedMovieYear = yr
                                                        yearDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Movie Sort Order
                        Text(
                            text = "Sort By",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TmdbMovieSort.entries.forEach { sort ->
                                    val isSelected = selectedMovieSort == sort
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedMovieSort = sort },
                                        label = { Text(sort.displayName, fontSize = 11.sp) },
                                        modifier = Modifier.height(30.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = LXPrimaryGreen,
                                            containerColor = LXSurfaceElevated,
                                            labelColor = LXTextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        // Tags Section Button (Sub-menu Navigation)
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(LXSurfaceElevated)
                                .border(1.dp, LXBorder, RoundedCornerShape(10.dp))
                                .clickable { showTagsSubMenu = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(LXPrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tag,
                                            contentDescription = null,
                                            tint = LXPrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Select & Exclude Tags",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                            color = LXTextPrimary
                                        )
                                        Text(
                                            text = if (movieIncludedTags.isEmpty() && movieExcludedTags.isEmpty())
                                                "Filter by movie genres"
                                            else
                                                "${movieIncludedTags.size} included (✓) • ${movieExcludedTags.size} excluded (✕)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = if (movieIncludedTags.isNotEmpty() || movieExcludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "Open Tags Filter",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Active Tag Chips inside Main Sheet with 1-tap delete
                            if (movieIncludedTags.isNotEmpty() || movieExcludedTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    movieIncludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "+$tag",
                                            isIncluded = true,
                                            onRemove = {
                                                movieIncludedTags = movieIncludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                    movieExcludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "-$tag",
                                            isIncluded = false,
                                            onRemove = {
                                                movieExcludedTags = movieExcludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 18+ Subtle Checkbox Toggle
                        SubtleAdultFilterToggle(
                            adultMode = adultMode,
                            onModeChange = { adultMode = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val filter = DiscoveryFilter.Movie(
                                    mode = selectedMovieMode!!,
                                    year = if (selectedMovieMode == TmdbDiscoveryMode.BY_YEAR) selectedMovieYear else null,
                                    sortOrder = selectedMovieSort,
                                    includedTags = movieIncludedTags,
                                    excludedTags = movieExcludedTags,
                                    adultMode = adultMode
                                )
                                onApplyMovie(filter)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            val modeText = when (selectedMovieMode!!) {
                                TmdbDiscoveryMode.BY_YEAR -> "Discover $selectedMovieYear Movies"
                                TmdbDiscoveryMode.MOST_POPULAR -> "Discover Most Popular Movies"
                            }
                            val tagsText = when {
                                movieIncludedTags.isNotEmpty() || movieExcludedTags.isNotEmpty() -> " (+${movieIncludedTags.size}/-${movieExcludedTags.size})"
                                else -> ""
                            }
                            val adultText = if (adultMode == AdultMode.ONLY) " (18+ Only)" else ""
                            Text(
                                text = "$modeText$tagsText$adultText",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                SearchCategory.TV_SHOWS -> {
                    // 1. Mode Selector: Small Icon Pills (By Year / Most Popular)
                    Text(
                        text = "Discovery Mode",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TmdbDiscoveryMode.entries.forEach { mode ->
                            val isSelected = selectedTvMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedTvMode = mode },
                                color = if (isSelected) LXPrimaryGreen.copy(alpha = 0.2f) else LXSurfaceElevated,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) LXPrimaryGreen else LXBorder
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = mode.iconEmoji,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Only load the rest once a mode is clicked!
                    if (selectedTvMode != null) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // TV Air Year Selector (only for BY_YEAR)
                        if (selectedTvMode == TmdbDiscoveryMode.BY_YEAR) {
                            Text(
                                text = "First Air Year: $selectedTvYear",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = LXTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val quickTvYears = listOf(maxYear, maxYear - 1, maxYear - 2, maxYear - 3)
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    quickTvYears.forEach { yr ->
                                        val isSelected = selectedTvYear == yr
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedTvYear = yr },
                                            label = { Text(yr.toString(), fontSize = 11.sp) },
                                            modifier = Modifier.height(30.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                                selectedLabelColor = LXPrimaryGreen,
                                                containerColor = LXSurfaceElevated,
                                                labelColor = LXTextSecondary
                                            )
                                        )
                                    }

                                    // Dropdown for more TV years
                                    Box {
                                        FilterChip(
                                            selected = selectedTvYear !in quickTvYears,
                                            onClick = { yearDropdownExpanded = true },
                                            modifier = Modifier.height(30.dp),
                                            label = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        if (selectedTvYear !in quickTvYears) selectedTvYear.toString() else "More",
                                                        fontSize = 11.sp
                                                    )
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                                }
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                                selectedLabelColor = LXPrimaryGreen,
                                                containerColor = LXSurfaceElevated,
                                                labelColor = LXTextSecondary
                                            )
                                        )

                                        DropdownMenu(
                                            expanded = yearDropdownExpanded,
                                            onDismissRequest = { yearDropdownExpanded = false },
                                            modifier = Modifier.background(LXSurfaceElevated)
                                        ) {
                                            (maxYear downTo 1970).forEach { yr ->
                                                DropdownMenuItem(
                                                    text = { Text(yr.toString(), color = LXTextPrimary) },
                                                    onClick = {
                                                        selectedTvYear = yr
                                                        yearDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // TV Sort Order
                        Text(
                            text = "Sort By",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TmdbTvSort.entries.forEach { sort ->
                                    val isSelected = selectedTvSort == sort
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedTvSort = sort },
                                        label = { Text(sort.displayName, fontSize = 11.sp) },
                                        modifier = Modifier.height(30.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = LXPrimaryGreen,
                                            containerColor = LXSurfaceElevated,
                                            labelColor = LXTextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        // Tags Section Button (Sub-menu Navigation)
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(LXSurfaceElevated)
                                .border(1.dp, LXBorder, RoundedCornerShape(10.dp))
                                .clickable { showTagsSubMenu = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(LXPrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tag,
                                            contentDescription = null,
                                            tint = LXPrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Select & Exclude Tags",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                            color = LXTextPrimary
                                        )
                                        Text(
                                            text = if (tvIncludedTags.isEmpty() && tvExcludedTags.isEmpty())
                                                "Filter by TV show genres"
                                            else
                                                "${tvIncludedTags.size} included (✓) • ${tvExcludedTags.size} excluded (✕)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = if (tvIncludedTags.isNotEmpty() || tvExcludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "Open Tags Filter",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Active Tag Chips inside Main Sheet with 1-tap delete
                            if (tvIncludedTags.isNotEmpty() || tvExcludedTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    tvIncludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "+$tag",
                                            isIncluded = true,
                                            onRemove = {
                                                tvIncludedTags = tvIncludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                    tvExcludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "-$tag",
                                            isIncluded = false,
                                            onRemove = {
                                                tvExcludedTags = tvExcludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 18+ Subtle Checkbox Toggle
                        SubtleAdultFilterToggle(
                            adultMode = adultMode,
                            onModeChange = { adultMode = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val filter = DiscoveryFilter.Tv(
                                    mode = selectedTvMode!!,
                                    year = if (selectedTvMode == TmdbDiscoveryMode.BY_YEAR) selectedTvYear else null,
                                    sortOrder = selectedTvSort,
                                    includedTags = tvIncludedTags,
                                    excludedTags = tvExcludedTags,
                                    adultMode = adultMode
                                )
                                onApplyTv(filter)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            val modeText = when (selectedTvMode!!) {
                                TmdbDiscoveryMode.BY_YEAR -> "Discover $selectedTvYear TV Shows"
                                TmdbDiscoveryMode.MOST_POPULAR -> "Discover Most Popular TV Shows"
                            }
                            val tagsText = when {
                                tvIncludedTags.isNotEmpty() || tvExcludedTags.isNotEmpty() -> " (+${tvIncludedTags.size}/-${tvExcludedTags.size})"
                                else -> ""
                            }
                            val adultText = if (adultMode == AdultMode.ONLY) " (18+ Only)" else ""
                            Text(
                                text = "$modeText$tagsText$adultText",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        text = "Seasonal & discovery filters are available for Anime, Movies, and TV Shows.",
                        color = LXTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
}

@Composable
private fun SearchTagBadge(
    label: String,
    isIncluded: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isIncluded) LXPrimaryGreen.copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
    val borderColor = if (isIncluded) LXPrimaryGreen.copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f)
    val contentColor = if (isIncluded) LXPrimaryGreen else Color(0xFFEF4444)

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = contentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SeasonSelectorBox(
    season: AnimeSeason,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) LXPrimaryGreen else LXBorder.copy(alpha = 0.5f)
    val bgColor = if (isSelected) LXPrimaryGreen.copy(alpha = 0.18f) else LXSurfaceElevated

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier.height(42.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = season.emoji,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = season.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                )
                Text(
                    text = season.monthsText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (isSelected) LXPrimaryGreen.copy(alpha = 0.8f) else LXTextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubtleAdultFilterToggle(
    adultMode: AdultMode,
    onModeChange: (AdultMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onModeChange(adultMode.next()) }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Checkbox(
                checked = adultMode != AdultMode.OFF,
                onCheckedChange = { onModeChange(adultMode.next()) },
                colors = CheckboxDefaults.colors(
                    checkedColor = if (adultMode == AdultMode.ONLY) Color(0xFFEF4444) else LXPrimaryGreen,
                    uncheckedColor = LXTextSecondary.copy(alpha = 0.6f),
                    checkmarkColor = Color.Black
                ),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = when (adultMode) {
                AdultMode.OFF -> "18+"
                AdultMode.INCLUDE -> "18+"
                AdultMode.ONLY -> "18+ Only"
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (adultMode != AdultMode.OFF) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp
            ),
            color = when (adultMode) {
                AdultMode.OFF -> LXTextSecondary
                AdultMode.INCLUDE -> LXPrimaryGreen
                AdultMode.ONLY -> Color(0xFFEF4444)
            }
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
                    contentDescription = "Included",
                    tint = LXBackgroundBlack,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        TagSelectionState.EXCLUDED -> {
            Box(
                modifier = modifier
                    .size(boxSize)
                    .background(Color(0xFFEF4444), shape)
                    .clip(shape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Excluded",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
