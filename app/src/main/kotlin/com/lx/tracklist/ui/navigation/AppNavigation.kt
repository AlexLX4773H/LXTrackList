package com.lx.tracklist.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lx.tracklist.LXApplication
import com.lx.tracklist.ui.detail.DetailScreen
import com.lx.tracklist.ui.detail.DetailViewModel
import com.lx.tracklist.ui.detail.DetailViewModelFactory
import com.lx.tracklist.ui.edit.AddEditScreen
import com.lx.tracklist.ui.edit.AddEditViewModel
import com.lx.tracklist.ui.edit.AddEditViewModelFactory
import com.lx.tracklist.ui.home.HomeScreen
import com.lx.tracklist.ui.home.HomeViewModel
import com.lx.tracklist.ui.home.HomeViewModelFactory
import com.lx.tracklist.ui.library.LibraryScreen
import com.lx.tracklist.ui.library.LibraryViewModel
import com.lx.tracklist.ui.library.LibraryViewModelFactory
import com.lx.tracklist.ui.settings.SettingsScreen
import com.lx.tracklist.ui.settings.SettingsViewModel
import com.lx.tracklist.ui.settings.SettingsViewModelFactory
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary

sealed class NavDestination {
    data object Home : NavDestination()
    data object Library : NavDestination()
    data object Stats : NavDestination()
    data object Settings : NavDestination()
    data class Detail(val entryId: Long) : NavDestination()
    data class AddEdit(val entryId: Long? = null) : NavDestination()
}

@Composable
fun AppNavigation() {
    val repository = LXApplication.instance.repository
    var currentDestination by remember { mutableStateOf<NavDestination>(NavDestination.Home) }

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(repository))
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModelFactory(repository))
    val statsViewModel: com.lx.tracklist.ui.stats.StatsViewModel = viewModel(
        factory = com.lx.tracklist.ui.stats.StatsViewModelFactory(repository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(repository))

    val isTopLevelScreen = currentDestination in listOf(
        NavDestination.Home,
        NavDestination.Library,
        NavDestination.Stats,
        NavDestination.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = LXBackgroundBlack,
        bottomBar = {
            if (isTopLevelScreen) {
                NavigationBar(
                    containerColor = LXSurfaceBlack,
                    contentColor = LXTextPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .border(1.dp, LXBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    val bottomScreens = listOf(
                        Screen.Home to NavDestination.Home,
                        Screen.Library to NavDestination.Library,
                        Screen.Stats to NavDestination.Stats,
                        Screen.Settings to NavDestination.Settings
                    )

                    bottomScreens.forEach { (screen, dest) ->
                        val isSelected = currentDestination == dest
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentDestination = dest },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LXBackgroundBlack,
                                selectedTextColor = LXPrimaryGreen,
                                indicatorColor = LXPrimaryGreen,
                                unselectedIconColor = LXTextSecondary,
                                unselectedTextColor = LXTextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val dest = currentDestination) {
                is NavDestination.Home -> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onEntryClick = { entryId -> currentDestination = NavDestination.Detail(entryId) },
                        onAddNewEntry = { currentDestination = NavDestination.AddEdit(null) },
                        onEditEntry = { entryId -> currentDestination = NavDestination.AddEdit(entryId) }
                    )
                }

                is NavDestination.Library -> {
                    LibraryScreen(
                        viewModel = libraryViewModel,
                        onEntryClick = { entryId -> currentDestination = NavDestination.Detail(entryId) },
                        onEditEntry = { entryId -> currentDestination = NavDestination.AddEdit(entryId) }
                    )
                }

                is NavDestination.Stats -> {
                    com.lx.tracklist.ui.stats.StatsScreen(
                        viewModel = statsViewModel
                    )
                }

                is NavDestination.Settings -> {
                    SettingsScreen(
                        viewModel = settingsViewModel
                    )
                }

                is NavDestination.Detail -> {
                    val detailViewModel: DetailViewModel = viewModel(
                        key = "detail_${dest.entryId}",
                        factory = DetailViewModelFactory(repository, dest.entryId)
                    )
                    DetailScreen(
                        viewModel = detailViewModel,
                        onNavigateBack = { currentDestination = NavDestination.Home },
                        onEdit = { entryId -> currentDestination = NavDestination.AddEdit(entryId) }
                    )
                }

                is NavDestination.AddEdit -> {
                    val addEditViewModel: AddEditViewModel = viewModel(
                        key = "add_edit_${dest.entryId ?: 0L}",
                        factory = AddEditViewModelFactory(repository, dest.entryId)
                    )
                    AddEditScreen(
                        viewModel = addEditViewModel,
                        onNavigateBack = {
                            currentDestination = if (dest.entryId != null && dest.entryId > 0) {
                                NavDestination.Detail(dest.entryId)
                            } else {
                                NavDestination.Home
                            }
                        }
                    )
                }
            }
        }
    }
}
