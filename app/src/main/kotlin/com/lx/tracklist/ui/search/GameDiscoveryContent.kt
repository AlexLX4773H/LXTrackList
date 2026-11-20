package com.lx.tracklist.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary

@Composable
fun GameDiscoveryContent(
    currentYear: Int,
    selectedGameMode: ContentDiscoveryMode?,
    onSelectGameMode: (ContentDiscoveryMode) -> Unit,
    selectedGameYear: Int,
    onSelectGameYear: (Int) -> Unit,
    selectedGameSort: GameSortOrder,
    onSelectGameSort: (GameSortOrder) -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Mode Selector
        Text(
            text = "Discovery Mode",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = LXTextSecondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ContentDiscoveryMode.entries.forEach { mode ->
                val isSelected = (selectedGameMode ?: ContentDiscoveryMode.MOST_POPULAR) == mode
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) LXPrimaryGreen.copy(alpha = 0.18f) else LXSurfaceElevated,
                    border = BorderStroke(1.dp, if (isSelected) LXPrimaryGreen else LXBorder.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectGameMode(mode) }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(mode.iconEmoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                        )
                    }
                }
            }
        }

        // 2. Year Selector
        if (selectedGameMode == ContentDiscoveryMode.BY_YEAR) {
            Text(
                text = "Release Year",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = LXTextSecondary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (currentYear downTo currentYear - 15).forEach { yr ->
                    val isSelected = selectedGameYear == yr
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) LXPrimaryGreen else LXSurfaceElevated,
                        border = BorderStroke(1.dp, if (isSelected) LXPrimaryGreen else LXBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { onSelectGameYear(yr) }
                    ) {
                        Text(
                            text = "$yr",
                            color = if (isSelected) LXBackgroundBlack else LXTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 3. Sort Order
        Text(
            text = "Sort By",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = LXTextSecondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GameSortOrder.entries.forEach { sort ->
                val isSelected = selectedGameSort == sort
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) LXPrimaryGreen.copy(alpha = 0.18f) else LXSurfaceElevated,
                    border = BorderStroke(1.dp, if (isSelected) LXPrimaryGreen else LXBorder.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectGameSort(sort) }
                ) {
                    Text(
                        text = sort.displayName,
                        color = if (isSelected) LXPrimaryGreen else LXTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Apply Button
        Button(
            onClick = onApply,
            colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            Text(
                text = if (selectedGameMode == ContentDiscoveryMode.BY_YEAR) "Discover Games $selectedGameYear" else "Discover Most Popular Games",
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}
