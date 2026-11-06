package com.lx.tracklist.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.UserProfile
import com.lx.tracklist.ui.components.UserAvatarButton
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
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    activeUser: UserProfile? = null,
    onUserAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    var showScoreDistributionSheet by remember { mutableStateOf(false) }
    var scoreDistributionCategory by remember { mutableStateOf<String?>(null) }
    var showCategoryDistributionSheet by remember { mutableStateOf(false) }
    var selectedCategoryForSheet by remember { mutableStateOf("ALL") }
    var inlineMalCategoryKey by remember { mutableStateOf("ALL") }

    if (showScoreDistributionSheet) {
        ScoreDistributionSheet(
            entries = state.allEntries,
            customCategories = state.customCategories,
            initialCategory = scoreDistributionCategory,
            onDismiss = {
                showScoreDistributionSheet = false
                scoreDistributionCategory = null
            }
        )
    }

    if (showCategoryDistributionSheet) {
        CategoryDistributionSheet(
            entries = state.allEntries,
            customCategories = state.customCategories,
            initialCategoryKey = selectedCategoryForSheet,
            onDismiss = { showCategoryDistributionSheet = false },
            onOpenScoreDistribution = { catKey ->
                showCategoryDistributionSheet = false
                scoreDistributionCategory = catKey
                showScoreDistributionSheet = true
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LXBackgroundBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    UserAvatarButton(
                        activeUser = activeUser,
                        onClick = onUserAvatarClick
                    )
                },
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
                    .widthIn(max = if (isTablet) 960.dp else 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = if (isTablet) 24.dp else 16.dp,
                        vertical = if (isTablet) 16.dp else 12.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isTablet) {
                    // 4 in 1 row on tablet with matching uniform heights
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard("Total Entries", "${state.totalEntries}", Icons.Default.PlayCircleOutline, modifier = Modifier.weight(1f).fillMaxHeight())
                        StatCard("Completed", "${state.completedCount}", Icons.Default.CheckCircle, valueColor = LXPrimaryGreen, modifier = Modifier.weight(1f).fillMaxHeight())
                        StatCard(
                            title = "Average Score",
                            value = if (state.averageScore > 0) String.format(Locale.US, "%.1f", state.averageScore) else "-",
                            icon = Icons.Default.Star,
                            valueColor = LXScoreGold,
                            onClick = {
                                scoreDistributionCategory = null
                                showScoreDistributionSheet = true
                            },
                            subtitle = "Tap for graph",
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        StatCard("Favorites", "${state.totalFavorites}", Icons.Default.Favorite, valueColor = LXPrimaryGreen, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    // 2x2 on phone with uniform heights
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard("Total Entries", "${state.totalEntries}", Icons.Default.PlayCircleOutline, modifier = Modifier.weight(1f).fillMaxHeight())
                        StatCard("Completed", "${state.completedCount}", Icons.Default.CheckCircle, valueColor = LXPrimaryGreen, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Average Score",
                            value = if (state.averageScore > 0) String.format(Locale.US, "%.1f", state.averageScore) else "-",
                            icon = Icons.Default.Star,
                            valueColor = LXScoreGold,
                            onClick = {
                                scoreDistributionCategory = null
                                showScoreDistributionSheet = true
                            },
                            subtitle = "Tap for graph",
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        StatCard("Favorites", "${state.totalFavorites}", Icons.Default.Favorite, valueColor = LXPrimaryGreen, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }

                // Consumed Metrics
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                    border = BorderStroke(1.dp, LXBorder),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CategoryBreakdownCard(
                            state = state,
                            onCategoryClick = { catKey ->
                                selectedCategoryForSheet = catKey
                                showCategoryDistributionSheet = true
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        StatusBreakdownCard(
                            state = state,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                } else {
                    CategoryBreakdownCard(
                        state = state,
                        onCategoryClick = { catKey ->
                            selectedCategoryForSheet = catKey
                            showCategoryDistributionSheet = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    StatusBreakdownCard(state = state, modifier = Modifier.fillMaxWidth())
                }

                // MAL-Style Featured Category Distribution Card directly on Stats page
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MAL Stats & Distribution",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXPrimaryGreen
                        )
                        TextButton(
                            onClick = {
                                selectedCategoryForSheet = inlineMalCategoryKey
                                showCategoryDistributionSheet = true
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Full Breakdown →", color = LXPrimaryGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Category selection chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = inlineMalCategoryKey == "ALL",
                                onClick = { inlineMalCategoryKey = "ALL" },
                                label = { Text("All", fontSize = 12.sp) },
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
                                selected = inlineMalCategoryKey == cat.name,
                                onClick = { inlineMalCategoryKey = cat.name },
                                label = { Text(cat.displayName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LXPrimaryGreen,
                                    selectedLabelColor = LXBackgroundBlack,
                                    containerColor = LXSurfaceElevated,
                                    labelColor = LXTextSecondary
                                )
                            )
                        }
                        items(state.customCategories) { customCat ->
                            FilterChip(
                                selected = inlineMalCategoryKey.equals(customCat.name, ignoreCase = true),
                                onClick = { inlineMalCategoryKey = customCat.name },
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

                    val inlineSummary = remember(state.allEntries, inlineMalCategoryKey, state.customCategories) {
                        calculateCategoryStatSummary(state.allEntries, inlineMalCategoryKey, state.customCategories)
                    }

                    MalCategoryStatsCard(
                        summary = inlineSummary,
                        onDetailsClick = {
                            selectedCategoryForSheet = inlineMalCategoryKey
                            showCategoryDistributionSheet = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    state: StatsUiState,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCategoryClick("ALL") },
        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
        border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Category Distribution",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXPrimaryGreen
                        )
                        Text(
                            text = "Tap for MAL breakdown",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = LXPrimaryGreen.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "View Details",
                        tint = LXPrimaryGreen.copy(alpha = 0.85f),
                        modifier = Modifier.size(13.dp)
                    )
                }

                MediaType.entries.forEach { cat ->
                    val count = state.categoryCounts[cat] ?: 0
                    val fraction = if (state.totalEntries > 0) count.toFloat() / state.totalEntries else 0f

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onCategoryClick(cat.name) }
                            .padding(vertical = 2.dp)
                    ) {
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

                // If any custom categories exist, display them as well
                state.customCategories.forEach { customCat ->
                    val count = state.allEntries.count { it.customCategory.equals(customCat.name, ignoreCase = true) }
                    val fraction = if (state.totalEntries > 0) count.toFloat() / state.totalEntries else 0f

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onCategoryClick(customCat.name) }
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(customCat.name, color = LXScoreGold, style = MaterialTheme.typography.bodyMedium)
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
                                    .background(LXScoreGold)
                            )
                        }
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
        border = BorderStroke(1.dp, LXBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Status Overview",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXPrimaryGreen
                )

                MediaStatus.entries.forEach { st ->
                    val count = state.statusCounts[st] ?: 0
                    val fraction = if (state.totalEntries > 0) count.toFloat() / state.totalEntries else 0f
                    val statusColor = when (st) {
                        MediaStatus.WATCHING -> Color(0xFF2ECC71)
                        MediaStatus.COMPLETED -> Color(0xFF2980B9)
                        MediaStatus.ON_HOLD -> Color(0xFFF1C40F)
                        MediaStatus.DROPPED -> Color(0xFFE74C3C)
                        MediaStatus.PLAN_TO_WATCH -> Color(0xFF7F8C8D)
                    }

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
                                    .background(statusColor)
                            )
                        }
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
    valueColor: Color = LXTextPrimary,
    onClick: (() -> Unit)? = null,
    subtitle: String? = null,
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
        border = BorderStroke(1.dp, if (onClick != null) LXScoreGold.copy(alpha = 0.5f) else LXBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
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
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = LXScoreGold.copy(alpha = 0.85f)
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
fun MalCategoryStatsCard(
    summary: CategoryStatSummary,
    modifier: Modifier = Modifier,
    onDetailsClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
        border = BorderStroke(1.dp, LXBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: "$displayName Stats" and "All $displayName Stats" / link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${summary.displayName} Stats",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )
                if (onDetailsClick != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onDetailsClick)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All ${summary.displayName} Stats",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = LXPrimaryGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = LXPrimaryGreen,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            // Days & Mean Score Header Row (exactly like MAL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Days: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LXTextSecondary
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f", summary.daysSpent),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Mean Score: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LXTextSecondary
                    )
                    Text(
                        text = if (summary.meanScore > 0) String.format(Locale.US, "%.2f", summary.meanScore) else "0.00",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (summary.meanScore > 0) LXScoreGold else LXTextPrimary
                    )
                }
            }

            // Proportional Multi-color Stacked Bar (matching MAL Image 2)
            val watchingCount = summary.statusCounts[MediaStatus.WATCHING] ?: 0
            val completedCount = summary.statusCounts[MediaStatus.COMPLETED] ?: 0
            val onHoldCount = summary.statusCounts[MediaStatus.ON_HOLD] ?: 0
            val droppedCount = summary.statusCounts[MediaStatus.DROPPED] ?: 0
            val planToCount = summary.statusCounts[MediaStatus.PLAN_TO_WATCH] ?: 0
            val totalInStatus = watchingCount + completedCount + onHoldCount + droppedCount + planToCount

            val barHeight = 14.dp
            if (totalInStatus == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(RoundedCornerShape(7.dp))
                        .background(LXProgressTrack)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(RoundedCornerShape(7.dp))
                        .background(LXProgressTrack)
                ) {
                    if (watchingCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(watchingCount.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFF2ECC71)) // MAL Watching green
                        )
                    }
                    if (completedCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(completedCount.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFF2980B9)) // MAL Completed blue
                        )
                    }
                    if (onHoldCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(onHoldCount.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFFF1C40F)) // MAL On-Hold amber/yellow
                        )
                    }
                    if (droppedCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(droppedCount.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFFE74C3C)) // MAL Dropped red
                        )
                    }
                    if (planToCount > 0) {
                        Box(
                            modifier = Modifier
                                .weight(planToCount.toFloat())
                                .fillMaxHeight()
                                .background(Color(0xFF7F8C8D)) // MAL Plan to watch/read grey
                        )
                    }
                }
            }

            // 2-Column Breakdown (matching MAL Image 2)
            val isWatch = summary.isVideo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Column: Status counts with colored dots
                Column(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MalStatusRow(
                        color = Color(0xFF2ECC71),
                        label = if (isWatch) "Watching" else "Reading",
                        count = watchingCount
                    )
                    MalStatusRow(
                        color = Color(0xFF2980B9),
                        label = "Completed",
                        count = completedCount
                    )
                    MalStatusRow(
                        color = Color(0xFFF1C40F),
                        label = "On-Hold",
                        count = onHoldCount
                    )
                    MalStatusRow(
                        color = Color(0xFFE74C3C),
                        label = "Dropped",
                        count = droppedCount
                    )
                    MalStatusRow(
                        color = Color(0xFF7F8C8D),
                        label = if (isWatch) "Plan to Watch" else "Plan to Read",
                        count = planToCount
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right Column: Summary metrics (Total Entries, Rewatched/Reread, Episodes/Chapters/Volumes)
                Column(
                    modifier = Modifier.weight(0.9f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MalMetricRow(label = "Total Entries", value = summary.totalEntries.formatCommas())
                    MalMetricRow(
                        label = if (isWatch) "Rewatched" else "Reread",
                        value = summary.rewatchedCount.formatCommas()
                    )
                    if (isWatch) {
                        MalMetricRow(label = "Episodes", value = summary.episodesCount.formatCommas())
                    } else {
                        MalMetricRow(label = "Chapters", value = summary.chaptersCount.formatCommas())
                        if (summary.volumesCount > 0) {
                            MalMetricRow(label = "Volumes", value = summary.volumesCount.formatCommas())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MalStatusRow(color: Color, label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                color = LXTextPrimary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = count.formatCommas(),
            color = LXTextPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun MalMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = LXTextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = LXTextPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDistributionSheet(
    entries: List<MediaEntry>,
    customCategories: List<CustomCategory>,
    initialCategoryKey: String = "ALL",
    onDismiss: () -> Unit,
    onOpenScoreDistribution: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCategoryKey by remember { mutableStateOf(initialCategoryKey) }

    val activeSummary = remember(entries, selectedCategoryKey, customCategories) {
        calculateCategoryStatSummary(entries, selectedCategoryKey, customCategories)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LXSurfaceBlack,
        sheetMaxWidth = if (isTablet) 720.dp else Dp.Unspecified,
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
                .navigationBarsPadding()
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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Category Distribution Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                    Text(
                        text = "Detailed counts, status distribution & MAL metrics",
                        style = MaterialTheme.typography.bodySmall,
                        color = LXTextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = LXTextSecondary)
                }
            }

            HorizontalDivider(color = LXBorder, thickness = 1.dp)

            // Category Chips Row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Select Category",
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
                            onClick = { selectedCategoryKey = "ALL" },
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
                            selected = selectedCategoryKey == cat.name,
                            onClick = { selectedCategoryKey = cat.name },
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
                        FilterChip(
                            selected = selectedCategoryKey.equals(customCat.name, ignoreCase = true),
                            onClick = { selectedCategoryKey = customCat.name },
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

            // MAL Card for active selection
            MalCategoryStatsCard(summary = activeSummary)

            // Additional breakdown metrics
            if (activeSummary.totalEntries > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LXSurfaceElevated),
                    border = BorderStroke(1.dp, LXBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${activeSummary.displayName} Completion & Status Breakdown",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )

                        // Status Percentages with progress bars
                        MediaStatus.entries.forEach { status ->
                            val count = activeSummary.statusCounts[status] ?: 0
                            val pct = if (activeSummary.totalEntries > 0) (count * 100f / activeSummary.totalEntries) else 0f
                            val statusColor = when (status) {
                                MediaStatus.WATCHING -> Color(0xFF2ECC71)
                                MediaStatus.COMPLETED -> Color(0xFF2980B9)
                                MediaStatus.ON_HOLD -> Color(0xFFF1C40F)
                                MediaStatus.DROPPED -> Color(0xFFE74C3C)
                                MediaStatus.PLAN_TO_WATCH -> Color(0xFF7F8C8D)
                            }
                            val label = when (status) {
                                MediaStatus.WATCHING -> if (activeSummary.isVideo) "Watching" else "Reading"
                                MediaStatus.COMPLETED -> "Completed"
                                MediaStatus.ON_HOLD -> "On Hold"
                                MediaStatus.DROPPED -> "Dropped"
                                MediaStatus.PLAN_TO_WATCH -> if (activeSummary.isVideo) "Plan to Watch" else "Plan to Read"
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                                        Text(label, color = LXTextPrimary, fontSize = 12.sp)
                                    }
                                    Text(
                                        text = "$count (${String.format(Locale.US, "%.1f", pct)}%)",
                                        color = LXTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(LXProgressTrack)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(pct / 100f)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(statusColor)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = LXBorder.copy(alpha = 0.5f), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = LXPrimaryGreen, modifier = Modifier.size(16.dp))
                                Text("Favorites: ${activeSummary.favoritesCount}", color = LXTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = LXScoreGold, modifier = Modifier.size(16.dp))
                                Text("Scored: ${activeSummary.scoredCount}/${activeSummary.totalEntries}", color = LXTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }

                            TextButton(
                                onClick = {
                                    onOpenScoreDistribution(selectedCategoryKey)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Score Graph →", color = LXScoreGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // If ALL is selected, list other individual categories that have entries!
            if (selectedCategoryKey == "ALL") {
                val categoriesWithEntries = MediaType.entries.filter { cat ->
                    entries.any { it.mediaType == cat && it.customCategory == null }
                }
                val customCatsWithEntries = customCategories.filter { customCat ->
                    entries.any { it.customCategory.equals(customCat.name, ignoreCase = true) }
                }

                if (categoriesWithEntries.isNotEmpty() || customCatsWithEntries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Individual Categories Breakdown",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = LXPrimaryGreen
                    )

                    categoriesWithEntries.forEach { cat ->
                        val subSummary = calculateCategoryStatSummary(entries, cat.name, customCategories)
                        MalCategoryStatsCard(summary = subSummary)
                    }

                    customCatsWithEntries.forEach { customCat ->
                        val subSummary = calculateCategoryStatSummary(entries, customCat.name, customCategories)
                        MalCategoryStatsCard(summary = subSummary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun Int.formatCommas(): String = NumberFormat.getNumberInstance(Locale.US).format(this)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreDistributionSheet(
    entries: List<MediaEntry>,
    customCategories: List<CustomCategory>,
    initialCategory: String? = null,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Determine sensible initial category: passed category, else ANIME if entries exist, else ALL
    val initialCat = remember(entries, initialCategory) {
        initialCategory ?: if (entries.any { it.mediaType == MediaType.ANIME }) "ANIME" else "ALL"
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
        sheetMaxWidth = if (isTablet) 720.dp else Dp.Unspecified,
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
                .navigationBarsPadding()
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
