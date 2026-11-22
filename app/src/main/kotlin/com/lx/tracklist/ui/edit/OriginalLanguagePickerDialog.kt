package com.lx.tracklist.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.ui.search.LanguageOption
import com.lx.tracklist.ui.search.OriginalLanguageRegistry
import com.lx.tracklist.ui.search.SearchCategory
import com.lx.tracklist.ui.theme.*
import java.util.Locale

@Composable
fun OriginalLanguagePickerDialog(
    currentLanguageCode: String?,
    mediaType: MediaType,
    onSelectLanguage: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val category = remember(mediaType) {
        when (mediaType) {
            MediaType.ANIME -> SearchCategory.ANIME
            MediaType.MANGA -> SearchCategory.MANGA
            MediaType.MANHWA -> SearchCategory.MANHWA
            MediaType.BOOK -> SearchCategory.BOOKS
            MediaType.MOVIE -> SearchCategory.MOVIES
            MediaType.SHOW -> SearchCategory.TV_SHOWS
            MediaType.GAME -> SearchCategory.GAMES
        }
    }

    val topLanguages = remember(category) {
        OriginalLanguageRegistry.getTopLanguagesForCategory(category)
    }

    val allLanguages = remember(category) {
        OriginalLanguageRegistry.getAllLanguagesForCategory(category)
    }

    val filteredLanguages = remember(searchQuery, allLanguages) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        if (q.isBlank()) {
            allLanguages
        } else {
            allLanguages.filter { opt ->
                opt.displayName.lowercase(Locale.ROOT).contains(q) ||
                opt.code.lowercase(Locale.ROOT).contains(q) ||
                opt.nativeName.lowercase(Locale.ROOT).contains(q) ||
                opt.iso6392Codes.any { it.lowercase(Locale.ROOT).contains(q) } ||
                opt.countryCodes.any { it.lowercase(Locale.ROOT).contains(q) }
            }
        }
    }

    // Check if user entered a custom code/name that is not matched by any existing language
    val canAddCustom = remember(searchQuery, filteredLanguages) {
        val q = searchQuery.trim()
        q.isNotBlank() && filteredLanguages.none {
            it.code.equals(q, ignoreCase = true) || it.displayName.equals(q, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = LXPrimaryGreen
                    )
                    Text(
                        text = "Original Language",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = LXTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search or add input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search or type custom code...", color = LXTextSecondary, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = LXTextSecondary)
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = LXTextSecondary)
                            }
                        }
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LXPrimaryGreen,
                        unfocusedBorderColor = LXBorder,
                        focusedTextColor = LXTextPrimary,
                        unfocusedTextColor = LXTextPrimary,
                        cursorColor = LXPrimaryGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // "None / Unspecified" Option
                    item {
                        val isNone = currentLanguageCode.isNullOrBlank()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isNone) LXPrimaryGreen.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    onSelectLanguage(null)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🚫", fontSize = 16.sp)
                                Column {
                                    Text(
                                        text = "None (Unspecified)",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isNone) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isNone) LXPrimaryGreen else LXTextPrimary
                                    )
                                    Text(
                                        text = "Clear original language",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LXTextSecondary
                                    )
                                }
                            }
                            if (isNone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Add Custom Language option if user typed something not matching
                    if (canAddCustom) {
                        val customInput = searchQuery.trim()
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LXPrimaryGreen.copy(alpha = 0.1f))
                                    .clickable {
                                        onSelectLanguage(customInput)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = LXPrimaryGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Use \"$customInput\"",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = LXPrimaryGreen
                                        )
                                        Text(
                                            text = "Set as custom language code",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LXTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // If not searching, show Top Languages Header
                    if (searchQuery.isBlank() && topLanguages.isNotEmpty()) {
                        item {
                            Text(
                                text = "POPULAR FOR ${category.displayName.uppercase(Locale.ROOT)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = LXTextSecondary,
                                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(topLanguages, key = { "top_${it.code}" }) { opt ->
                            LanguageItemRow(
                                opt = opt,
                                isSelected = OriginalLanguageRegistry.isSameLanguage(currentLanguageCode, opt.code),
                                onSelect = {
                                    onSelectLanguage(opt.code)
                                    onDismiss()
                                }
                            )
                        }
                        item {
                            HorizontalDivider(
                                color = LXBorder,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            Text(
                                text = "ALL LANGUAGES",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = LXTextSecondary,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }
                    }

                    // All / Filtered list
                    items(filteredLanguages, key = { "all_${it.code}" }) { opt ->
                        LanguageItemRow(
                            opt = opt,
                            isSelected = OriginalLanguageRegistry.isSameLanguage(currentLanguageCode, opt.code),
                            onSelect = {
                                onSelectLanguage(opt.code)
                                onDismiss()
                            }
                        )
                    }

                    if (filteredLanguages.isEmpty() && !canAddCustom) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No matching languages",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LXTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LXTextSecondary)
            }
        },
        containerColor = LXSurfaceElevated
    )
}

@Composable
private fun LanguageItemRow(
    opt: LanguageOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) LXPrimaryGreen.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = opt.flagEmoji, fontSize = 20.sp)
            Column {
                Text(
                    text = opt.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                )
                Text(
                    text = "${opt.nativeName} • ${opt.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary
                )
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = LXPrimaryGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
