package com.lx.tracklist.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Library : Screen("library", "Library", Icons.Default.LibraryBooks)
    data object Search : Screen("search", "Search", Icons.Default.Search)
    data object Stats : Screen("stats", "Stats", Icons.Default.PieChart)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
