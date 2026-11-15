package com.lx.tracklist.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Movie
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchDiscoverySheet(
    category: SearchCategory,
    activeFilter: DiscoveryFilter?,
    onDismiss: () -> Unit,
    onApplyAnime: (AnimeSeason, Int, AnimeSortOrder, AdultMode) -> Unit,
    onApplyMovie: (Int, TmdbMovieSort, AdultMode) -> Unit,
    onApplyTv: (Int, TmdbTvSort, AdultMode) -> Unit,
    onResetFilter: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentYear = AnimeSeason.currentYear()
    val maxYear = AnimeSeason.maxDiscoveryYear() // currentYear + 2

    // Anime state
    var selectedSeason by remember {
        mutableStateOf(
            (activeFilter as? DiscoveryFilter.Anime)?.season ?: AnimeSeason.current()
        )
    }
    var selectedAnimeYear by remember {
        mutableIntStateOf(
            (activeFilter as? DiscoveryFilter.Anime)?.year ?: currentYear
        )
    }
    var selectedAnimeSort by remember {
        mutableStateOf(
            (activeFilter as? DiscoveryFilter.Anime)?.sortOrder ?: AnimeSortOrder.POPULARITY
        )
    }

    // Movie state
    var selectedMovieYear by remember {
        mutableIntStateOf(
            (activeFilter as? DiscoveryFilter.Movie)?.year ?: currentYear
        )
    }
    var selectedMovieSort by remember {
        mutableStateOf(
            (activeFilter as? DiscoveryFilter.Movie)?.sortOrder ?: TmdbMovieSort.POPULARITY
        )
    }

    // TV state
    var selectedTvYear by remember {
        mutableIntStateOf(
            (activeFilter as? DiscoveryFilter.Tv)?.year ?: currentYear
        )
    }
    var selectedTvSort by remember {
        mutableStateOf(
            (activeFilter as? DiscoveryFilter.Tv)?.sortOrder ?: TmdbTvSort.POPULARITY
        )
    }

    var adultMode by remember {
        mutableStateOf(activeFilter?.adultMode ?: AdultMode.OFF)
    }

    var yearDropdownExpanded by remember { mutableStateOf(false) }

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
                            SearchCategory.ANIME -> Icons.Default.Explore
                            SearchCategory.MOVIES -> Icons.Default.Movie
                            SearchCategory.TV_SHOWS -> Icons.Default.Tv
                            else -> Icons.Default.Explore
                        },
                        contentDescription = null,
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when (category) {
                            SearchCategory.ANIME -> "Seasonal Anime"
                            SearchCategory.MOVIES -> "Discover Movies"
                            SearchCategory.TV_SHOWS -> "Discover TV Shows"
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

            Spacer(modifier = Modifier.height(18.dp))

            when (category) {
                SearchCategory.ANIME -> {
                    // Quick seasonal shortcuts
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Current Season", fontSize = 12.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                selectedSeason = nextSeason
                                selectedAnimeYear = nextYear
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Upcoming", fontSize = 12.sp, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Season Selector (2x2 Grid with comfortable size and clear text)
                    Text(
                        text = "Season",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                    Spacer(modifier = Modifier.height(18.dp))

                    // Year Selector (Up to currentYear + 2)
                    Text(
                        text = "Year: $selectedAnimeYear",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val quickAnimeYears = listOf(maxYear, maxYear - 1, maxYear - 2, maxYear - 3, maxYear - 4)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        quickAnimeYears.forEach { yr ->
                            val isSelected = selectedAnimeYear == yr
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedAnimeYear = yr },
                                label = { Text(yr.toString(), fontSize = 12.sp) },
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
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            if (selectedAnimeYear !in quickAnimeYears) selectedAnimeYear.toString() else "More",
                                            fontSize = 12.sp
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
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

                    Spacer(modifier = Modifier.height(18.dp))

                    // Sort Order
                    Text(
                        text = "Sort By",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AnimeSortOrder.entries.forEach { sort ->
                            val isSelected = selectedAnimeSort == sort
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedAnimeSort = sort },
                                label = { Text(sort.displayName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = LXPrimaryGreen,
                                    containerColor = LXSurfaceElevated,
                                    labelColor = LXTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 18+ Subtle Checkbox Toggle
                    SubtleAdultFilterToggle(
                        adultMode = adultMode,
                        onModeChange = { adultMode = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onApplyAnime(selectedSeason, selectedAnimeYear, selectedAnimeSort, adultMode)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (adultMode == AdultMode.ONLY) {
                                "Explore ${selectedSeason.displayName} $selectedAnimeYear (18+ Only)"
                            } else {
                                "Explore ${selectedSeason.displayName} $selectedAnimeYear Anime"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                SearchCategory.MOVIES -> {
                    // Movie Year Selector (Up to currentYear + 2)
                    Text(
                        text = "Release Year: $selectedMovieYear",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val quickMovieYears = listOf(maxYear, maxYear - 1, maxYear - 2, maxYear - 3, maxYear - 4)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        quickMovieYears.forEach { yr ->
                            val isSelected = selectedMovieYear == yr
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMovieYear = yr },
                                label = { Text(yr.toString(), fontSize = 12.sp) },
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
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            if (selectedMovieYear !in quickMovieYears) selectedMovieYear.toString() else "More",
                                            fontSize = 12.sp
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
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

                    Spacer(modifier = Modifier.height(18.dp))

                    // Movie Sort Order
                    Text(
                        text = "Sort By",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TmdbMovieSort.entries.forEach { sort ->
                            val isSelected = selectedMovieSort == sort
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMovieSort = sort },
                                label = { Text(sort.displayName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = LXPrimaryGreen,
                                    containerColor = LXSurfaceElevated,
                                    labelColor = LXTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 18+ Subtle Checkbox Toggle
                    SubtleAdultFilterToggle(
                        adultMode = adultMode,
                        onModeChange = { adultMode = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onApplyMovie(selectedMovieYear, selectedMovieSort, adultMode)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (adultMode == AdultMode.ONLY) {
                                "Discover $selectedMovieYear Movies (18+ Only)"
                            } else {
                                "Discover $selectedMovieYear Movies"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                SearchCategory.TV_SHOWS -> {
                    // TV Air Year Selector (Up to currentYear + 2)
                    Text(
                        text = "First Air Year: $selectedTvYear",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val quickTvYears = listOf(maxYear, maxYear - 1, maxYear - 2, maxYear - 3, maxYear - 4)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        quickTvYears.forEach { yr ->
                            val isSelected = selectedTvYear == yr
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTvYear = yr },
                                label = { Text(yr.toString(), fontSize = 12.sp) },
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
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            if (selectedTvYear !in quickTvYears) selectedTvYear.toString() else "More",
                                            fontSize = 12.sp
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
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

                    Spacer(modifier = Modifier.height(18.dp))

                    // TV Sort Order
                    Text(
                        text = "Sort By",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TmdbTvSort.entries.forEach { sort ->
                            val isSelected = selectedTvSort == sort
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTvSort = sort },
                                label = { Text(sort.displayName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = LXPrimaryGreen,
                                    containerColor = LXSurfaceElevated,
                                    labelColor = LXTextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 18+ Subtle Checkbox Toggle
                    SubtleAdultFilterToggle(
                        adultMode = adultMode,
                        onModeChange = { adultMode = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onApplyTv(selectedTvYear, selectedTvSort, adultMode)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (adultMode == AdultMode.ONLY) {
                                "Discover $selectedTvYear TV Shows (18+ Only)"
                            } else {
                                "Discover $selectedTvYear TV Shows"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
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

            Spacer(modifier = Modifier.height(30.dp))
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
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier.height(52.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = season.emoji,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = season.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                )
                Text(
                    text = season.monthsText,
                    style = MaterialTheme.typography.labelSmall,
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
