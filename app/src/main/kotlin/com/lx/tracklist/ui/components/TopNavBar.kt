package com.lx.tracklist.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lx.tracklist.R
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.UserProfile
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceVariant
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import com.lx.tracklist.ui.theme.ThemeManager
import kotlinx.coroutines.delay

enum class SortOption(val displayName: String) {
    RECENTLY_UPDATED("Recently Updated"),
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    SCORE_DESC("Highest Score"),
    PROGRESS_DESC("Highest Progress")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavBar(
    selectedCategory: MediaType,
    onCategoryClick: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchActive: Boolean,
    onToggleSearch: () -> Unit,
    onRefresh: () -> Unit,
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    activeUser: UserProfile? = null,
    onLogoClick: () -> Unit = {},
    customCategoryTitle: String? = null,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (ThemeManager.effectiveBackgroundImage != null) Color.Transparent else LXBackgroundBlack,
            titleContentColor = LXTextPrimary
        ),
        navigationIcon = {
            if (isSearchActive) {
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close Search",
                        tint = LXTextPrimary
                    )
                }
            } else {
                UserAvatarButton(
                    activeUser = activeUser,
                    onClick = onLogoClick
                )
            }
        },
        title = {
            if (isSearchActive) {
                val focusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current
                val accentColor = if (ThemeManager.isBeyondTheme) Color(0xFFFC8181)
                    else if (ThemeManager.isSlimeTheme) Color(0xFF38BDF8)
                    else if (ThemeManager.isGateTheme) Color(0xFFFBBF24)
                    else if (ThemeManager.isSkyTheme) Color(0xFF38BDF8)
                    else LXPrimaryGreen

                LaunchedEffect(Unit) {
                    delay(100L)
                    try {
                        focusRequester.requestFocus()
                    } catch (_: Exception) {}
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(LXSurfaceVariant, RoundedCornerShape(21.dp))
                        .border(1.dp, LXBorder.copy(alpha = 0.5f), RoundedCornerShape(21.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = accentColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search ${customCategoryTitle ?: selectedCategory.displayName}...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LXTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = LXTextPrimary),
                                cursorBrush = SolidColor(accentColor),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = { onSearchQueryChange("") }),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = LXTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCategoryClick)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = customCategoryTitle ?: selectedCategory.displayName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Category",
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        actions = {
            if (!isSearchActive) {
                IconButton(onClick = onToggleSearch) {
                    if (ThemeManager.isBeyondTheme) {
                        Image(
                            painter = painterResource(R.drawable.theme_beyond_search_icon),
                            contentDescription = "Search",
                            modifier = Modifier.size(22.dp)
                        )
                    } else if (ThemeManager.isSlimeTheme) {
                        Image(
                            painter = painterResource(R.drawable.theme_slime_search_icon),
                            contentDescription = "Search",
                            modifier = Modifier.size(22.dp)
                        )
                    } else if (ThemeManager.isGateTheme) {
                        Image(
                            painter = painterResource(R.drawable.theme_gate_search_icon),
                            contentDescription = "Search",
                            modifier = Modifier.size(22.dp)
                        )
                    } else if (ThemeManager.isSkyTheme) {
                        Image(
                            painter = painterResource(R.drawable.theme_sky_header_search_btn),
                            contentDescription = "Search",
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = LXTextSecondary
                        )
                    }
                }

                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = LXTextSecondary
                    )
                }
            }

            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    if (ThemeManager.isBeyondTheme) {
                        Image(
                            painter = painterResource(R.drawable.theme_beyond_icon_filters),
                            contentDescription = "Sort",
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (ThemeManager.isSlimeTheme) {
                        Image(
                            painter = painterResource(R.drawable.theme_slime_icon_filters),
                            contentDescription = "Sort",
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (ThemeManager.isGateTheme) {
                        Image(
                            painter = painterResource(R.drawable.theme_gate_header_portal_arch),
                            contentDescription = "Sort",
                            modifier = Modifier.size(22.dp)
                        )
                    } else if (ThemeManager.isSkyTheme) {
                        Image(
                            painter = painterResource(R.drawable.theme_sky_header_sort_btn),
                            contentDescription = "Sort",
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = LXTextSecondary
                        )
                    }
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.displayName,
                                    color = if (option == selectedSort) (if (ThemeManager.isBeyondTheme) Color(0xFFFC8181) else LXPrimaryGreen) else LXTextPrimary,
                                    fontWeight = if (option == selectedSort) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onSortSelected(option)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }
    )
}

private fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        LXPrimaryGreen
    }
}
