package com.lx.tracklist.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lx.tracklist.LXApplication
import com.lx.tracklist.R
import com.lx.tracklist.ui.components.UserSwitcherSheet
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
    var backStack by remember { mutableStateOf(listOf<NavDestination>(NavDestination.Home)) }
    val currentDestination = backStack.last()

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(repository))
    val homeState by homeViewModel.uiState.collectAsState()

    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModelFactory(repository))
    val statsViewModel: com.lx.tracklist.ui.stats.StatsViewModel = viewModel(
        factory = com.lx.tracklist.ui.stats.StatsViewModelFactory(repository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(repository))

    fun navigateTo(destination: NavDestination) {
        if (destination == currentDestination) return
        val isTopLevel = destination in listOf(
            NavDestination.Home,
            NavDestination.Library,
            NavDestination.Stats,
            NavDestination.Settings
        )
        backStack = if (isTopLevel) {
            if (destination == NavDestination.Home) {
                listOf(NavDestination.Home)
            } else {
                listOf(NavDestination.Home, destination)
            }
        } else {
            backStack + destination
        }
    }

    fun navigateBack(): Boolean {
        if (backStack.size > 1) {
            backStack = backStack.dropLast(1)
            return true
        }
        return false
    }

    // Root back handler to navigate back down the stack until reaching Home
    BackHandler(enabled = backStack.size > 1) {
        navigateBack()
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val isTopLevelScreen = currentDestination in listOf(
        NavDestination.Home,
        NavDestination.Library,
        NavDestination.Stats,
        NavDestination.Settings
    )

    val navItems = listOf(
        Screen.Home to NavDestination.Home,
        Screen.Library to NavDestination.Library,
        Screen.Stats to NavDestination.Stats,
        Screen.Settings to NavDestination.Settings
    )

    @Composable
    fun ScreenContent() {
        when (val dest = currentDestination) {
            is NavDestination.Home -> {
                HomeScreen(
                    viewModel = homeViewModel,
                    onEntryClick = { entryId -> navigateTo(NavDestination.Detail(entryId)) },
                    onAddNewEntry = { navigateTo(NavDestination.AddEdit(null)) },
                    onEditEntry = { entryId -> navigateTo(NavDestination.AddEdit(entryId)) }
                )
            }

            is NavDestination.Library -> {
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onEntryClick = { entryId -> navigateTo(NavDestination.Detail(entryId)) },
                    onEditEntry = { entryId -> navigateTo(NavDestination.AddEdit(entryId)) }
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
                    onNavigateBack = { navigateBack() },
                    onEdit = { entryId -> navigateTo(NavDestination.AddEdit(entryId)) }
                )
            }

            is NavDestination.AddEdit -> {
                val addEditViewModel: AddEditViewModel = viewModel(
                    key = "add_edit_${dest.entryId ?: 0L}",
                    factory = AddEditViewModelFactory(repository, dest.entryId)
                )
                AddEditScreen(
                    viewModel = addEditViewModel,
                    onNavigateBack = { navigateBack() }
                )
            }
        }
    }

    // Global User Switcher Bottom Sheet
    if (homeState.isUserSwitcherOpen) {
        UserSwitcherSheet(
            activeUser = homeState.activeUser,
            allUsers = homeState.allUsers,
            onSelectUser = { homeViewModel.switchUser(it) },
            onCreateUser = { name, color -> homeViewModel.createUser(name, color) },
            onDeleteUser = { homeViewModel.deleteUser(it) },
            onDismiss = { homeViewModel.setUserSwitcherOpen(false) }
        )
    }

    if (isTablet && isTopLevelScreen) {
        // Tablet Adaptive Navigation with NavigationRail on left
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(LXBackgroundBlack)
        ) {
            NavigationRail(
                containerColor = LXSurfaceBlack,
                contentColor = LXTextPrimary,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(88.dp)
                    .border(width = 1.dp, color = LXBorder),
                header = {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable { homeViewModel.setUserSwitcherOpen(true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.lx_logo),
                            contentDescription = "Switch User (LX Logo)",
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(parseHexColor(homeState.activeUser.avatarColorHex))
                                .border(1.5.dp, LXBackgroundBlack, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            ) {
                navItems.forEach { (screen, dest) ->
                    val isSelected = currentDestination == dest
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = { navigateTo(dest) },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(screen.title) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = LXBackgroundBlack,
                            selectedTextColor = LXPrimaryGreen,
                            indicatorColor = LXPrimaryGreen,
                            unselectedIconColor = LXTextSecondary,
                            unselectedTextColor = LXTextSecondary
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                ScreenContent()
            }
        }
    } else {
        // Phone Layout with Bottom NavigationBar
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
                        navItems.forEach { (screen, dest) ->
                            val isSelected = currentDestination == dest
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { navigateTo(dest) },
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
                ScreenContent()
            }
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
