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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import java.util.Locale

@Composable
internal fun SearchDiscoveryLanguageSubMenu(
    category: SearchCategory,
    currentSelectedLanguages: Set<String>,
    onToggleLanguage: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var languageSearchQuery by remember { mutableStateOf("") }
    val hasSelectedLanguages = currentSelectedLanguages.isNotEmpty()

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
                    imageVector = if (hasSelectedLanguages) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (hasSelectedLanguages) "Apply" else "Back",
                    tint = LXPrimaryGreen,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Original Language",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )
            }

            if (hasSelectedLanguages) {
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

        // Sub-menu Instructions & active status
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
                Text("Tap to select • Multi-select (OR)", fontSize = 11.sp, color = LXPrimaryGreen, fontWeight = FontWeight.Bold)
            }

            Text(
                text = when {
                    currentSelectedLanguages.isEmpty() -> "All languages"
                    currentSelectedLanguages.size == 1 -> OriginalLanguageRegistry.getDisplayWithFlag(currentSelectedLanguages.first()) ?: "1 selected"
                    else -> "${currentSelectedLanguages.size} selected (OR)"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (currentSelectedLanguages.isNotEmpty()) LXPrimaryGreen else LXTextSecondary
            )
        }

        // Search Bar for Languages
        OutlinedTextField(
            value = languageSearchQuery,
            onValueChange = { languageSearchQuery = it },
            placeholder = {
                Text(
                    text = "Search language by name, native name, or code...",
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
                if (languageSearchQuery.isNotBlank()) {
                    IconButton(onClick = { languageSearchQuery = "" }) {
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

        // Filtered Languages
        val filteredLanguages = remember(languageSearchQuery, category) {
            val query = languageSearchQuery.trim()
            val available = OriginalLanguageRegistry.getAllLanguagesForCategory(category)
            if (query.isEmpty()) {
                val top = OriginalLanguageRegistry.getTopLanguagesForCategory(category)
                val others = OriginalLanguageRegistry.getOtherLanguagesForCategory(category)
                top to others
            } else {
                val matches = available.filter { lang ->
                    lang.displayName.contains(query, ignoreCase = true) ||
                    lang.nativeName.contains(query, ignoreCase = true) ||
                    lang.code.equals(query, ignoreCase = true) ||
                    lang.iso6392Codes.any { it.contains(query, ignoreCase = true) } ||
                    lang.countryCodes.any { it.contains(query, ignoreCase = true) }
                }
                emptyList<LanguageOption>() to matches
            }
        }

        if (filteredPlatformsEmpty(filteredLanguages)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LXSurfaceElevated)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No languages matching \"$languageSearchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LXTextSecondary
                )
            }
        } else {
            // "All Languages" reset card at top if not searching
            if (languageSearchQuery.isBlank()) {
                val isAllSelected = currentSelectedLanguages.isEmpty()
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
                        Text(text = "🌐", fontSize = 22.sp)
                        Column {
                            Text(
                                text = "All Languages",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isAllSelected) LXPrimaryGreen else LXTextPrimary
                            )
                            Text(
                                text = "Any country or origin",
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

            // Top for this category
            if (filteredLanguages.first.isNotEmpty()) {
                Text(
                    text = "Top for ${category.displayName}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredLanguages.first.forEach { lang ->
                        LanguageSelectRow(
                            language = lang,
                            isSelected = currentSelectedLanguages.any { OriginalLanguageRegistry.isSameLanguage(it, lang.code) },
                            onSelect = { onToggleLanguage(lang.code) }
                        )
                    }
                }
            }

            // Other / All languages
            if (filteredLanguages.second.isNotEmpty()) {
                if (filteredLanguages.first.isNotEmpty()) {
                    Text(
                        text = "Other Languages",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredLanguages.second.forEach { lang ->
                        LanguageSelectRow(
                            language = lang,
                            isSelected = currentSelectedLanguages.any { OriginalLanguageRegistry.isSameLanguage(it, lang.code) },
                            onSelect = { onToggleLanguage(lang.code) }
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
                currentSelectedLanguages.isEmpty() -> "Done (All Languages)"
                currentSelectedLanguages.size == 1 -> "Done (${OriginalLanguageRegistry.getDisplayWithFlag(currentSelectedLanguages.first())})"
                else -> "Done (${currentSelectedLanguages.size} Languages - OR)"
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

private fun filteredPlatformsEmpty(pair: Pair<List<LanguageOption>, List<LanguageOption>>): Boolean =
    pair.first.isEmpty() && pair.second.isEmpty()

@Composable
internal fun LanguageSelectRow(
    language: LanguageOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) LXPrimaryGreen.copy(alpha = 0.12f)
                else LXSurfaceElevated
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) LXPrimaryGreen.copy(alpha = 0.5f) else Color.Transparent
                ),
                RoundedCornerShape(8.dp)
            )
            .clickable { onSelect() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
            , modifier = Modifier.weight(1f)
        ) {
            Text(text = language.flagEmoji, fontSize = 22.sp)
            Column {
                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                )
                Text(
                    text = "${language.nativeName} • ${language.code.uppercase(Locale.ROOT)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = LXTextSecondary
                )
            }
        }

        if (isSelected) {
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
