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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GameDiscoveryContent(
    currentYear: Int,
    selectedGameMode: ContentDiscoveryMode?,
    onSelectGameMode: (ContentDiscoveryMode) -> Unit,
    selectedGameStartYear: Int?,
    selectedGameEndYear: Int?,
    onRangeChange: (Int?, Int?) -> Unit,
    selectedGameSort: GameSortOrder,
    onSelectGameSort: (GameSortOrder) -> Unit,
    gameIncludedTags: Set<String>,
    gameExcludedTags: Set<String>,
    onOpenTags: () -> Unit,
    onRemoveIncludedTag: (String) -> Unit,
    onRemoveExcludedTag: (String) -> Unit,
    gameIncludedPlatforms: Set<String> = emptySet(),
    gameExcludedPlatforms: Set<String> = emptySet(),
    onOpenPlatforms: () -> Unit = {},
    onRemoveIncludedPlatform: (String) -> Unit = {},
    onRemoveExcludedPlatform: (String) -> Unit = {},
    selectedLanguages: Set<String> = emptySet(),
    onOpenLanguages: () -> Unit = {},
    adultMode: AdultMode,
    onAdultModeChange: (AdultMode) -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 1. Discovery Mode Selector
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
            ContentDiscoveryMode.entries.forEach { mode ->
                val isSelected = selectedGameMode == mode
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectGameMode(mode) },
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

        // Only load the rest once a mode is selected
        if (selectedGameMode != null) {
            Spacer(modifier = Modifier.height(10.dp))

            // 2. Year Range Selector (for BY_YEAR)
            if (selectedGameMode == ContentDiscoveryMode.BY_YEAR) {
                YearRangeSelector(
                    startYear = selectedGameStartYear,
                    endYear = selectedGameEndYear,
                    onRangeChange = onRangeChange
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Game Sort Order
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
                    GameSortOrder.entries.forEach { sort ->
                        val isSelected = selectedGameSort == sort
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectGameSort(sort) },
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

            // 4. Tags Section Button (Game Genres Sub-menu Navigation)
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(LXSurfaceElevated)
                    .border(1.dp, LXBorder, RoundedCornerShape(10.dp))
                    .clickable { onOpenTags() }
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
                                text = if (gameIncludedTags.isEmpty() && gameExcludedTags.isEmpty())
                                    "Filter by game genres"
                                else
                                    "${gameIncludedTags.size} included (✓) • ${gameExcludedTags.size} excluded (✕)",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (gameIncludedTags.isNotEmpty() || gameExcludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary
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
                if (gameIncludedTags.isNotEmpty() || gameExcludedTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        gameIncludedTags.forEach { tag ->
                            SearchTagBadge(
                                label = "+$tag",
                                isIncluded = true,
                                onRemove = { onRemoveIncludedTag(tag) }
                            )
                        }
                        gameExcludedTags.forEach { tag ->
                            SearchTagBadge(
                                label = "-$tag",
                                isIncluded = false,
                                onRemove = { onRemoveExcludedTag(tag) }
                            )
                        }
                    }
                }
            }

            // 5. Platforms Section Button (Game Platforms Sub-menu Navigation)
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(LXSurfaceElevated)
                    .border(
                        1.dp,
                        if (gameIncludedPlatforms.isNotEmpty() || gameExcludedPlatforms.isNotEmpty()) LXPrimaryGreen.copy(alpha = 0.5f) else LXBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onOpenPlatforms() }
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
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = LXPrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Platforms",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = LXTextPrimary
                            )
                            val subtitle = when {
                                gameIncludedPlatforms.isEmpty() && gameExcludedPlatforms.isEmpty() ->
                                    "Filter by platforms (PC, Switch, PS...)"
                                gameIncludedPlatforms.isNotEmpty() && gameExcludedPlatforms.isNotEmpty() ->
                                    "${gameIncludedPlatforms.size} included (OR) • ${gameExcludedPlatforms.size} excluded"
                                gameIncludedPlatforms.isNotEmpty() ->
                                    "${gameIncludedPlatforms.size} included (OR condition)"
                                else ->
                                    "${gameExcludedPlatforms.size} excluded"
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (gameIncludedPlatforms.isNotEmpty() || gameExcludedPlatforms.isNotEmpty()) LXPrimaryGreen else LXTextSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open Platforms Filter",
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Active Platform Chips inside Main Sheet with 1-tap delete
                if (gameIncludedPlatforms.isNotEmpty() || gameExcludedPlatforms.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        gameIncludedPlatforms.forEach { platform ->
                            SearchTagBadge(
                                label = "+$platform",
                                isIncluded = true,
                                onRemove = { onRemoveIncludedPlatform(platform) }
                            )
                        }
                        gameExcludedPlatforms.forEach { platform ->
                            SearchTagBadge(
                                label = "-$platform",
                                isIncluded = false,
                                onRemove = { onRemoveExcludedPlatform(platform) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6. 18+ Subtle Checkbox Toggle
            SubtleAdultFilterToggle(
                adultMode = adultMode,
                onModeChange = onAdultModeChange
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 7. Apply Action Button
            val yearStr = when {
                selectedGameStartYear != null && selectedGameEndYear != null && selectedGameStartYear == selectedGameEndYear -> "$selectedGameStartYear"
                selectedGameStartYear != null && selectedGameEndYear != null -> "$selectedGameStartYear–$selectedGameEndYear"
                selectedGameStartYear != null -> "From $selectedGameStartYear"
                selectedGameEndYear != null -> "Up to $selectedGameEndYear"
                else -> "$currentYear"
            }
            val modeText = when (selectedGameMode) {
                ContentDiscoveryMode.BY_YEAR -> "Discover Games $yearStr".trim()
                ContentDiscoveryMode.MOST_POPULAR -> "Discover Most Popular Games"
            }
            val tagsText = when {
                gameIncludedTags.isNotEmpty() || gameExcludedTags.isNotEmpty() -> " (+${gameIncludedTags.size}/-${gameExcludedTags.size})"
                else -> ""
            }
            val platformsText = when {
                gameIncludedPlatforms.isNotEmpty() || gameExcludedPlatforms.isNotEmpty() -> " (+${gameIncludedPlatforms.size}/-${gameExcludedPlatforms.size} plat)"
                else -> ""
            }
            val adultText = when (adultMode) {
                AdultMode.OFF -> ""
                AdultMode.INCLUDE -> " • Incl 18+"
                AdultMode.ONLY -> " • 18+ Only"
            }

            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Text(
                    text = "$modeText$tagsText$platformsText$adultText",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}
