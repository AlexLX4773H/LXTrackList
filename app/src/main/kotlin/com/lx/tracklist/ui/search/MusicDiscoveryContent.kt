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
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.text.style.TextOverflow
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
fun MusicDiscoveryContent(
    currentYear: Int,
    selectedMusicMode: MusicDiscoveryMode,
    onSelectMusicMode: (MusicDiscoveryMode) -> Unit,
    selectedGenre: String?,
    onSelectGenre: (String?) -> Unit,
    selectedMusicStartYear: Int?,
    selectedMusicEndYear: Int?,
    onRangeChange: (Int?, Int?) -> Unit,
    selectedMusicSort: MusicSortOrder,
    onSelectMusicSort: (MusicSortOrder) -> Unit,
    musicIncludedTags: Set<String>,
    musicExcludedTags: Set<String>,
    onOpenTags: () -> Unit,
    onRemoveIncludedTag: (String) -> Unit,
    onRemoveExcludedTag: (String) -> Unit,
    selectedLanguages: Set<String> = emptySet(),
    onOpenLanguages: () -> Unit = {},
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
            MusicDiscoveryMode.entries.forEach { mode ->
                val isSelected = selectedMusicMode == mode
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectMusicMode(mode) },
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

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Year Range Selector (for BY_YEAR)
        if (selectedMusicMode == MusicDiscoveryMode.BY_YEAR) {
            YearRangeSelector(
                startYear = selectedMusicStartYear,
                endYear = selectedMusicEndYear,
                onRangeChange = onRangeChange
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. Quick Genre Selection Chips
        Text(
            text = if (selectedMusicMode == MusicDiscoveryMode.BY_GENRE) "Select Genre" else "Filter by Genre (Optional)",
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
                val popularGenres = listOf(
                    "All", "Pop", "Rock", "Hip-Hop", "Anime OST", "J-Pop", "K-Pop",
                    "Dance", "Electronic", "R&B", "Metal", "Alternative", "Classical", "Country"
                )
                popularGenres.forEach { genre ->
                    val isAll = genre == "All"
                    val isSelected = if (isAll) selectedGenre == null else selectedGenre.equals(genre, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isAll) {
                                onSelectGenre(null)
                            } else {
                                onSelectGenre(if (isSelected) null else genre)
                            }
                        },
                        label = { Text(genre, fontSize = 11.sp) },
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

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Music Sort Order (if in BY_YEAR or BY_GENRE)
        if (selectedMusicMode != MusicDiscoveryMode.TOP_CHARTS) {
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
                    MusicSortOrder.entries.forEach { sort ->
                        val isSelected = selectedMusicSort == sort
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectMusicSort(sort) },
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
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 5. Full Tags Section Button
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
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = LXPrimaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Music Tags / Subgenres",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = LXTextPrimary
                        )
                        val subtitle = when {
                            musicIncludedTags.isEmpty() && musicExcludedTags.isEmpty() -> "Tap to filter by specific music subgenres"
                            musicIncludedTags.isNotEmpty() && musicExcludedTags.isEmpty() -> "${musicIncludedTags.size} tags included"
                            musicIncludedTags.isEmpty() && musicExcludedTags.isNotEmpty() -> "${musicExcludedTags.size} tags excluded"
                            else -> "+${musicIncludedTags.size} / -${musicExcludedTags.size} tags"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = if (musicIncludedTags.isNotEmpty() || musicExcludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Open Tags",
                    tint = if (musicIncludedTags.isNotEmpty() || musicExcludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }

            if (musicIncludedTags.isNotEmpty() || musicExcludedTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        musicIncludedTags.forEach { tag ->
                            SearchTagBadge(
                                label = tag,
                                isIncluded = true,
                                onRemove = { onRemoveIncludedTag(tag) }
                            )
                        }
                        musicExcludedTags.forEach { tag ->
                            SearchTagBadge(
                                label = tag,
                                isIncluded = false,
                                onRemove = { onRemoveExcludedTag(tag) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 6. Original Language Filter
        LanguageFilterEntryCard(
            selectedLanguages = selectedLanguages,
            onClick = onOpenLanguages,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 7. Apply Action Button
        val yearStr = when {
            selectedMusicStartYear != null && selectedMusicEndYear != null && selectedMusicStartYear == selectedMusicEndYear -> "$selectedMusicStartYear"
            selectedMusicStartYear != null && selectedMusicEndYear != null -> "$selectedMusicStartYear–$selectedMusicEndYear"
            selectedMusicStartYear != null -> "From $selectedMusicStartYear"
            selectedMusicEndYear != null -> "Up to $selectedMusicEndYear"
            else -> "$currentYear"
        }
        val modeText = when (selectedMusicMode) {
            MusicDiscoveryMode.TOP_CHARTS -> if (!selectedGenre.isNullOrBlank()) "Discover Top $selectedGenre Tracks" else "Discover Top Music Charts"
            MusicDiscoveryMode.BY_GENRE -> if (!selectedGenre.isNullOrBlank()) "Discover $selectedGenre Tracks" else "Discover Music by Genre"
            MusicDiscoveryMode.BY_YEAR -> "Discover Music $yearStr".trim()
        }
        val tagsText = when {
            musicIncludedTags.isNotEmpty() || musicExcludedTags.isNotEmpty() -> " (+${musicIncludedTags.size}/-${musicExcludedTags.size})"
            else -> ""
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
                text = "$modeText$tagsText",
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}
