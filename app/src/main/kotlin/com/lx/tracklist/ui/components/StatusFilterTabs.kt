package com.lx.tracklist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary

@Composable
fun StatusFilterTabs(
    selectedCategory: MediaType,
    selectedStatus: MediaStatus?,
    statusCounts: Map<MediaStatus, Int>,
    totalCount: Int,
    onStatusSelected: (MediaStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    val statuses = listOf(
        MediaStatus.WATCHING,
        MediaStatus.PLAN_TO_WATCH,
        MediaStatus.COMPLETED,
        MediaStatus.ON_HOLD,
        MediaStatus.DROPPED
    )

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            // "ALL" Tab
            item {
                StatusTabItem(
                    label = "ALL",
                    count = totalCount,
                    isSelected = selectedStatus == null,
                    onClick = { onStatusSelected(null) }
                )
            }

            // Status items
            items(statuses) { status ->
                val label = status.getDisplayLabel(selectedCategory).uppercase()
                val count = statusCounts[status] ?: 0
                StatusTabItem(
                    label = label,
                    count = count,
                    isSelected = selectedStatus == status,
                    onClick = { onStatusSelected(status) }
                )
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

@Composable
private fun StatusTabItem(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
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
                color = if (isSelected) LXPrimaryGreen else LXTextSecondary
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "($count)",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = if (isSelected) LXPrimaryGreen else LXTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Neon Green active indicator underline
        Box(
            modifier = Modifier
                .height(3.dp)
                .width(48.dp)
                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                .background(if (isSelected) LXPrimaryGreen else androidx.compose.ui.graphics.Color.Transparent)
        )
    }
}
