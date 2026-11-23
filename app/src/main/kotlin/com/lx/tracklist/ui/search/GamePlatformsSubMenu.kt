package com.lx.tracklist.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.lx.tracklist.ui.library.TagSelectionState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary

@Composable
fun GamePlatformsSubMenu(
    includedPlatforms: Set<String>,
    excludedPlatforms: Set<String>,
    onCyclePlatform: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val hasPlatforms = includedPlatforms.isNotEmpty() || excludedPlatforms.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDismiss() }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = if (hasPlatforms) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (hasPlatforms) "Apply" else "Back",
                    tint = LXPrimaryGreen,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Platforms",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )
            }

            if (hasPlatforms) {
                TextButton(
                    onClick = onClearAll,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Clear All", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(color = LXBorder, thickness = 1.dp)

        // Sub-menu Instructions & active count
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(LXSurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(LXPrimaryGreen, RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = LXBackgroundBlack, modifier = Modifier.size(12.dp))
                    }
                    Text("1 tap = Include (OR)", fontSize = 11.sp, color = LXPrimaryGreen, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(0xFFEF4444), RoundedCornerShape(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                    }
                    Text("2 taps = Exclude", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }

            HorizontalDivider(color = LXBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Filter",
                    style = MaterialTheme.typography.labelSmall,
                    color = LXTextSecondary
                )
                Text(
                    text = "${includedPlatforms.size} inc (OR) • ${excludedPlatforms.size} exc",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (hasPlatforms) LXPrimaryGreen else LXTextSecondary,
                    maxLines = 1
                )
            }
        }

        // Search Bar for Platforms
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Search platforms (e.g. PC, Switch, PS5, Xbox)...",
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
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = LXTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LXPrimaryGreen,
                unfocusedBorderColor = LXBorder,
                focusedTextColor = LXTextPrimary,
                unfocusedTextColor = LXTextPrimary,
                cursorColor = LXPrimaryGreen
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        // Filtered Platforms
        val filteredPlatforms = remember(searchQuery) {
            val query = searchQuery.trim()
            if (query.isEmpty()) {
                val top = GamePlatformRegistry.getTopPlatforms()
                val others = GamePlatformRegistry.getOtherPlatforms()
                top to others
            } else {
                val matches = GamePlatformRegistry.filterPlatforms(query)
                emptyList<GamePlatformOption>() to matches
            }
        }

        if (filteredPlatforms.first.isEmpty() && filteredPlatforms.second.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LXSurfaceElevated)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No platforms matching \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LXTextSecondary
                )
            }
        } else {
            // "All Platforms" reset card at top if not searching
            if (searchQuery.isBlank()) {
                val isAllSelected = !hasPlatforms
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isAllSelected) LXPrimaryGreen.copy(alpha = 0.12f)
                            else LXSurfaceElevated
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isAllSelected) LXPrimaryGreen.copy(alpha = 0.5f) else Color.Transparent
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onClearAll() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "🎮", fontSize = 22.sp)
                        Column {
                            Text(
                                text = "All Platforms",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isAllSelected) LXPrimaryGreen else LXTextPrimary
                            )
                            Text(
                                text = "Any platform (no platform restrictions)",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = LXTextSecondary
                            )
                        }
                    }

                    if (isAllSelected) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(LXPrimaryGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = LXBackgroundBlack,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .border(BorderStroke(1.5.dp, LXTextSecondary.copy(alpha = 0.4f)), CircleShape)
                        )
                    }
                }
            }

            // Top Platforms
            if (filteredPlatforms.first.isNotEmpty()) {
                Text(
                    text = "Popular Platforms",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredPlatforms.first.forEach { plat ->
                        val platState = when {
                            includedPlatforms.any { it.equals(plat.displayName, ignoreCase = true) || it.equals(plat.id, ignoreCase = true) } -> TagSelectionState.INCLUDED
                            excludedPlatforms.any { it.equals(plat.displayName, ignoreCase = true) || it.equals(plat.id, ignoreCase = true) } -> TagSelectionState.EXCLUDED
                            else -> TagSelectionState.UNSELECTED
                        }
                        PlatformSelectRow(
                            platform = plat,
                            state = platState,
                            onCycle = { onCyclePlatform(plat.displayName) }
                        )
                    }
                }
            }

            // Other Platforms
            if (filteredPlatforms.second.isNotEmpty()) {
                if (filteredPlatforms.first.isNotEmpty()) {
                    Text(
                        text = "Other Platforms",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredPlatforms.second.forEach { plat ->
                        val platState = when {
                            includedPlatforms.any { it.equals(plat.displayName, ignoreCase = true) || it.equals(plat.id, ignoreCase = true) } -> TagSelectionState.INCLUDED
                            excludedPlatforms.any { it.equals(plat.displayName, ignoreCase = true) || it.equals(plat.id, ignoreCase = true) } -> TagSelectionState.EXCLUDED
                            else -> TagSelectionState.UNSELECTED
                        }
                        PlatformSelectRow(
                            platform = plat,
                            state = platState,
                            onCycle = { onCyclePlatform(plat.displayName) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val doneLabel = when {
                !hasPlatforms -> "Done (All Platforms)"
                includedPlatforms.isNotEmpty() && excludedPlatforms.isNotEmpty() ->
                    "Done (${includedPlatforms.size} inc (OR) • ${excludedPlatforms.size} exc)"
                includedPlatforms.isNotEmpty() ->
                    "Done (${includedPlatforms.size} Platforms - OR)"
                else ->
                    "Done (${excludedPlatforms.size} Excluded)"
            }
            Text(
                text = doneLabel,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
internal fun PlatformSelectRow(
    platform: GamePlatformOption,
    state: TagSelectionState,
    onCycle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when (state) {
                    TagSelectionState.INCLUDED -> LXPrimaryGreen.copy(alpha = 0.12f)
                    TagSelectionState.EXCLUDED -> Color(0xFFEF4444).copy(alpha = 0.12f)
                    TagSelectionState.UNSELECTED -> LXSurfaceElevated
                }
            )
            .border(
                width = if (state != TagSelectionState.UNSELECTED) 1.dp else 0.5.dp,
                color = when (state) {
                    TagSelectionState.INCLUDED -> LXPrimaryGreen.copy(alpha = 0.6f)
                    TagSelectionState.EXCLUDED -> Color(0xFFEF4444).copy(alpha = 0.6f)
                    TagSelectionState.UNSELECTED -> LXBorder
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onCycle() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            TriStateTagCheckbox(state = state)

            Text(
                text = platform.iconEmoji,
                fontSize = 16.sp
            )

            Text(
                text = platform.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (state != TagSelectionState.UNSELECTED) FontWeight.Bold else FontWeight.Normal
                ),
                color = when (state) {
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
            when (state) {
                TagSelectionState.INCLUDED -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LXPrimaryGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Included (OR)",
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
                        text = platform.category,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = LXTextSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
