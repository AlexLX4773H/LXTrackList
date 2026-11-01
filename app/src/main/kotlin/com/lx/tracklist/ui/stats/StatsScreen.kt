package com.lx.tracklist.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
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
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXProgressTrack
import com.lx.tracklist.ui.theme.LXScoreGold
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextDisabled
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    var showScoreDistributionSheet by remember { mutableStateOf(false) }

    if (showScoreDistributionSheet) {
        ScoreDistributionSheet(
            entries = state.allEntries,
            customCategories = state.customCategories,
            onDismiss = { showScoreDistributionSheet = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LXBackgroundBlack,
        contentWindowInsets = if (isTablet) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Statistics & Analytics",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
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
                    .widthIn(max = 900.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isTablet) {
                    // 4 in 1 row on tablet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard("Total Entries", "${state.totalEntries}", Icons.Default.PlayCircleOutline, modifier = Modifier.weight(1f))
                        StatCard("Completed", "${state.completedCount}", Icons.Default.CheckCircle, valueColor = LXPrimaryGreen, modifier = Modifier.weight(1f))
                        StatCard(
                            title = "Average Score",
                            value = if (state.averageScore > 0) String.format(Locale.US, "%.1f", state.averageScore) else "-",
                            icon = Icons.Default.Star,
                            valueColor = LXScoreGold,
                            onClick = { showScoreDistributionSheet = true },
                            modifier = Modifier.weight(1f)
                        )
                        StatCard("Favorites", "${state.totalFavorites}", Icons.Default.Favorite, valueColor = LXPrimaryGreen, modifier = Modifier.weight(1f))
                    }
                } else {
                    // 2x2 on phone
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard("Total Entries", "${state.totalEntries}", Icons.Default.PlayCircleOutline, modifier = Modifier.weight(1f))
                        StatCard("Completed", "${state.completedCount}", Icons.Default.CheckCircle, valueColor = LXPrimaryGreen, modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Average Score",
                            value = if (state.averageScore > 0) String.format(Locale.US, "%.1f", state.averageScore) else "-",
                            icon = Icons.Default.Star,
                            valueColor = LXScoreGold,
                            onClick = { showScoreDistributionSheet = true },
                            modifier = Modifier.weight(1f)
                        )
                        StatCard("Favorites", "${state.totalFavorites}", Icons.Default.Favorite, valueColor = LXPrimaryGreen, modifier = Modifier.weight(1f))
                    }
                }

                // Consumed Metrics
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Total Media Consumed",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXPrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            MetricColumn(label = "Episodes", value = "${state.totalEpisodesWatched}")
                            MetricColumn(label = "Chapters", value = "${state.totalChaptersRead}")
                            MetricColumn(label = "Volumes", value = "${state.totalVolumesRead}")
                        }
                    }
                }

                // Category & Status Side-by-side on tablet or stacked on phone
                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CategoryBreakdownCard(state = state, modifier = Modifier.weight(1f))
                        StatusBreakdownCard(state = state, modifier = Modifier.weight(1f))
                    }
                } else {
                    CategoryBreakdownCard(state = state, modifier = Modifier.fillMaxWidth())
                    StatusBreakdownCard(state = state, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(state: StatsUiState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
        border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Category Distribution",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = LXPrimaryGreen
            )

            MediaType.entries.forEach { cat ->
                val count = state.categoryCounts[cat] ?: 0
                val fraction = if (state.totalEntries > 0) count.toFloat() / state.totalEntries else 0f

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(cat.displayName, color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Text("$count", color = LXTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(LXProgressTrack)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(RoundedCornerShape(3.dp))
                                .background(LXPrimaryGreen)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBreakdownCard(state: StatsUiState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
        border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Status Overview",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = LXPrimaryGreen
            )

            MediaStatus.entries.forEach { st ->
                val count = state.statusCounts[st] ?: 0
                val fraction = if (state.totalEntries > 0) count.toFloat() / state.totalEntries else 0f

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(st.defaultLabel, color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Text("$count", color = LXTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(LXProgressTrack)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(RoundedCornerShape(3.dp))
                                .background(LXPrimaryGreen)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    valueColor: androidx.compose.ui.graphics.Color = LXTextPrimary,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onClick)
                } else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
        border = BorderStroke(1.dp, if (onClick != null) LXScoreGold.copy(alpha = 0.45f) else LXBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = LXTextSecondary, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(icon, contentDescription = null, tint = valueColor, modifier = Modifier.size(18.dp))
                    if (onClick != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "View Details",
                            tint = LXScoreGold.copy(alpha = 0.8f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
            Text(
                text = value,
                color = valueColor,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)
            )
            if (onClick != null) {
                Text(
                    text = "Tap for distribution",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = LXScoreGold.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreDistributionSheet(
    entries: List<MediaEntry>,
    customCategories: List<CustomCategory>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Determine sensible initial category: ANIME if entries exist, else ALL
    val initialCat = remember(entries) {
        if (entries.any { it.mediaType == MediaType.ANIME }) "ANIME" else "ALL"
    }
    var selectedCategoryKey by remember { mutableStateOf(initialCat) }
    var completedOnly by remember { mutableStateOf(false) }
    var inspectedScore by remember { mutableStateOf<Int?>(null) }

    val data = remember(entries, selectedCategoryKey, completedOnly) {
        calculateScoreDistribution(entries, selectedCategoryKey, completedOnly)
    }

    val selectedCategoryDisplayName = remember(selectedCategoryKey, customCategories) {
        when {
            selectedCategoryKey == "ALL" -> "All"
            MediaType.entries.any { it.name.equals(selectedCategoryKey, ignoreCase = true) } -> {
                MediaType.valueOf(selectedCategoryKey).displayName
            }
            else -> customCategories.firstOrNull { it.name.equals(selectedCategoryKey, ignoreCase = true) }?.name ?: selectedCategoryKey
        }
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "$selectedCategoryDisplayName Statistics: Overview",
                        style = MaterialTheme.typography.bodySmall,
                        color = LXTextSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = LXScoreGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (completedOnly) "Completed Score Distribution" else "Score Distribution",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                        // Colored category badge (Image 1 Anime = red, Image 2 Manga = gold/orange)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when (selectedCategoryKey) {
                                        "ANIME" -> Color(0xFFE54D4D)
                                        "MANGA" -> Color(0xFFF0932B)
                                        "MANHWA" -> Color(0xFF6C5CE7)
                                        "MOVIE" -> Color(0xFF00CEC9)
                                        "SHOW" -> Color(0xFF0984E3)
                                        "BOOK" -> Color(0xFFE84393)
                                        else -> LXPrimaryGreen.copy(alpha = 0.85f)
                                    }
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = selectedCategoryDisplayName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = Color.White
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = LXTextSecondary)
                }
            }

            HorizontalDivider(color = LXBorder, thickness = 1.dp)

            // Category Chips Row (Horizontal Scroll)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextSecondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryKey == "ALL",
                            onClick = {
                                selectedCategoryKey = "ALL"
                                inspectedScore = null
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
                        val isSelected = selectedCategoryKey == cat.name
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryKey = cat.name
                                inspectedScore = null
                            },
                            label = { Text(cat.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LXPrimaryGreen,
                                selectedLabelColor = LXBackgroundBlack,
                                containerColor = LXSurfaceElevated,
                                labelColor = LXTextSecondary
                            )
                        )
                    }

                    items(customCategories) { customCat ->
                        val isSelected = selectedCategoryKey.equals(customCat.name, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryKey = customCat.name
                                inspectedScore = null
                            },
                            label = { Text(customCat.name) },
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

            // Status Filter Toggle: All Statuses vs Completed Only
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LXSurfaceElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!completedOnly) LXPrimaryGreen.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = if (!completedOnly) 1.dp else 0.dp,
                            color = if (!completedOnly) LXPrimaryGreen else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            completedOnly = false
                            inspectedScore = null
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "All Statuses",
                        fontSize = 12.sp,
                        fontWeight = if (!completedOnly) FontWeight.Bold else FontWeight.Normal,
                        color = if (!completedOnly) LXPrimaryGreen else LXTextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (completedOnly) LXScoreGold.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = if (completedOnly) 1.dp else 0.dp,
                            color = if (completedOnly) LXScoreGold else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            completedOnly = true
                            inspectedScore = null
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Completed Only",
                        fontSize = 12.sp,
                        fontWeight = if (completedOnly) FontWeight.Bold else FontWeight.Normal,
                        color = if (completedOnly) LXScoreGold else LXTextSecondary
                    )
                }
            }

            // Empty State (Image 2) or Score Distribution Chart (Image 1)
            if (data.scoredCount == 0) {
                // "and if there is no scores for the catgory put up a messgae." (Image 2)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LXSurfaceElevated),
                    border = BorderStroke(1.dp, LXBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = LXTextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "There is not enough list data to display this graph.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = LXTextSecondary
                        )
                        if (data.totalCategoryEntries > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Found ${data.totalCategoryEntries} entries, but none have been scored 1-10 yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextDisabled
                            )
                        }
                    }
                }
            } else {
                // Chart Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LXSurfaceElevated),
                    border = BorderStroke(1.dp, LXBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Vertical bar chart with horizontal score numbers (1..10) along X axis
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                (1..10).forEach { score ->
                                    val count = data.distribution[score] ?: 0
                                    val isCurrentInspected = (inspectedScore == score)
                                    val heightFraction = if (data.maxFrequency > 0) {
                                        count.toFloat() / data.maxFrequency
                                    } else 0f

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                inspectedScore = if (inspectedScore == score) null else score
                                            }
                                            .padding(horizontal = 1.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom
                                    ) {
                                        // Count text above bar
                                        if (count > 0) {
                                            Text(
                                                text = "$count",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = if (count > 99) 8.sp else 9.sp,
                                                    fontWeight = if (isCurrentInspected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isCurrentInspected) LXScoreGold else LXTextSecondary
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Bar Container
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f, fill = false),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            // Empty track slot background
                                            Box(
                                                modifier = Modifier
                                                    .width(18.dp)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                    .background(LXProgressTrack.copy(alpha = 0.35f))
                                            )

                                            // Filled bar
                                            if (count > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(18.dp)
                                                        .fillMaxHeight(fraction = heightFraction.coerceIn(0.04f, 1f))
                                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                        .background(
                                                            if (isCurrentInspected) LXScoreGold
                                                            else Color(0xFF4C6EF5) // Sleek indigo/blue matching sample
                                                        )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Score Number (Horizontal row at bottom)
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isCurrentInspected) LXScoreGold else Color.Transparent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$score",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isCurrentInspected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 11.sp
                                                ),
                                                color = if (isCurrentInspected) LXBackgroundBlack else LXTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Inspected Score Tooltip / Card (or peak score by default)
                        val activeInspect = inspectedScore ?: data.distribution.maxByOrNull { it.value }?.key ?: 10
                        val inspectCount = data.distribution[activeInspect] ?: 0
                        val inspectPercent = if (data.scoredCount > 0) (inspectCount * 100f / data.scoredCount) else 0f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LXSurfaceBlack)
                                .border(1.dp, LXBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Score: $activeInspect — ${getScoreLabel(activeInspect)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LXScoreGold
                                )
                                Text(
                                    text = "$inspectCount entries (${String.format(Locale.US, "%.1f", inspectPercent)}% of scored)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = LXTextSecondary
                                )
                            }
                            Text(
                                text = "★ $activeInspect/10",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = LXScoreGold
                            )
                        }
                    }
                }

                // Summary Stats Footer (Matching Image 1: Days/Completed, Scored Anime, Mean Score)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Scored $selectedCategoryDisplayName",
                            style = MaterialTheme.typography.labelSmall,
                            color = LXTextSecondary
                        )
                        Text(
                            text = "${data.scoredCount}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXPrimaryGreen
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Mean Score",
                            style = MaterialTheme.typography.labelSmall,
                            color = LXTextSecondary
                        )
                        Text(
                            text = if (data.meanScore > 0) String.format(Locale.US, "%.2f", data.meanScore) else "-",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXScoreGold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = LXTextSecondary
                        )
                        Text(
                            text = "${data.totalCategoryEntries}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = LXPrimaryGreen,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = label,
            color = LXTextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
