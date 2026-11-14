package com.lx.tracklist.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.R
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.SubCategory
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXTextSecondary
import com.lx.tracklist.ui.theme.ThemeManager

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusFilterTabs(
    selectedCategory: MediaType,
    selectedStatus: MediaStatus?,
    selectedSubCategory: String? = null,
    statusCounts: Map<MediaStatus, Int>,
    subCategories: List<SubCategory> = emptyList(),
    subCategoryCounts: Map<String, Int> = emptyMap(),
    totalCount: Int,
    onStatusSelected: (MediaStatus?) -> Unit,
    onSubCategorySelected: (String?) -> Unit,
    onAddSubCategoryClick: () -> Unit,
    onDeleteSubCategory: ((SubCategory) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statuses = listOf(
        MediaStatus.WATCHING,
        MediaStatus.PLAN_TO_WATCH,
        MediaStatus.COMPLETED,
        MediaStatus.ON_HOLD,
        MediaStatus.DROPPED
    )

    val listState = rememberLazyListState()

    LaunchedEffect(selectedStatus, selectedSubCategory, subCategories) {
        val targetIdx = when {
            selectedSubCategory != null -> {
                val subIdx = subCategories.indexOfFirst { it.name.equals(selectedSubCategory, ignoreCase = true) }
                if (subIdx >= 0) 1 + statuses.size + 1 + subIdx else 0
            }
            selectedStatus != null -> {
                val stIdx = statuses.indexOf(selectedStatus)
                if (stIdx >= 0) 1 + stIdx else 0
            }
            else -> 0
        }
        if (targetIdx >= 0) {
            listState.animateScrollToItem(targetIdx)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "ALL" Tab
            item {
                StatusTabItem(
                    label = "ALL",
                    count = totalCount,
                    isSelected = selectedStatus == null && selectedSubCategory == null,
                    onClick = {
                        onStatusSelected(null)
                    }
                )
            }

            // Status items
            items(statuses) { status ->
                val label = status.getDisplayLabel(selectedCategory).uppercase()
                val count = statusCounts[status] ?: 0
                StatusTabItem(
                    label = label,
                    count = count,
                    isSelected = selectedStatus == status && selectedSubCategory == null,
                    onClick = {
                        onStatusSelected(status)
                    }
                )
            }

            // Sub Categories (specific to this category)
            if (subCategories.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .height(20.dp)
                            .width(1.dp)
                            .background(LXBorder)
                    )
                }

                items(subCategories, key = { "${it.categoryKey}_${it.id}_${it.name}" }) { subCat ->
                    val isSelected = selectedSubCategory.equals(subCat.name, ignoreCase = true)
                    val count = subCategoryCounts[subCat.name] ?: 0
                    val accentColor = parseHexColor(subCat.colorHex)

                    StatusTabItem(
                        label = subCat.name.uppercase(),
                        count = count,
                        isSelected = isSelected,
                        activeColor = accentColor,
                        onClick = {
                            if (isSelected) {
                                onSubCategorySelected(null)
                            } else {
                                onSubCategorySelected(subCat.name)
                            }
                        },
                        onLongClick = {
                            onDeleteSubCategory?.invoke(subCat)
                        }
                    )
                }
            }

            // "+" Add Subcategory Button Tab
            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onAddSubCategoryClick)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Sub Category",
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "ADD",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = LXPrimaryGreen
                    )
                }
            }
        }

        // Bottom border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LXBorder)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StatusTabItem(
    label: String,
    count: Int,
    isSelected: Boolean,
    activeColor: Color = LXPrimaryGreen,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = if (isSelected) activeColor else LXTextSecondary
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "($count)",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = if (isSelected) activeColor else LXTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Active indicator underline
        if (ThemeManager.isBeyondTheme && isSelected) {
            Image(
                painter = painterResource(R.drawable.theme_beyond_progress_bar_fill),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .height(8.dp)
                    .width(52.dp)
            )
        } else if (ThemeManager.isSlimeTheme && isSelected) {
            Image(
                painter = painterResource(R.drawable.theme_slime_progress_bar_fill),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .height(5.dp)
                    .width(52.dp)
            )
        } else if (ThemeManager.isGateTheme && isSelected) {
            Image(
                painter = painterResource(R.drawable.theme_gate_progress_bar_fill),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .height(4.dp)
                    .width(52.dp)
            )
        } else if (ThemeManager.isSkyTheme && isSelected) {
            Image(
                painter = painterResource(R.drawable.theme_sky_progress_bar_fill),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .height(4.dp)
                    .width(52.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .width(48.dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(if (isSelected) activeColor else Color.Transparent)
            )
        }
    }
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
