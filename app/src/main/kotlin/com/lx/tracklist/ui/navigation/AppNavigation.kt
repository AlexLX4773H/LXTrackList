package com.lx.tracklist.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lx.tracklist.LXApplication
import com.lx.tracklist.R
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.ui.components.PasskeyVerificationDialog
import com.lx.tracklist.ui.components.UserAvatarButton
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
import com.lx.tracklist.ui.search.SearchScreen
import com.lx.tracklist.ui.search.SearchViewModel
import com.lx.tracklist.ui.search.SearchViewModelFactory
import com.lx.tracklist.ui.settings.SettingsScreen
import com.lx.tracklist.ui.settings.SettingsViewModel
import com.lx.tracklist.ui.settings.SettingsViewModelFactory
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import com.lx.tracklist.ui.theme.ThemeManager

sealed class NavDestination {
    data object Home : NavDestination()
    data object Library : NavDestination()
    data object Search : NavDestination()
    data object Stats : NavDestination()
    data object Settings : NavDestination()
    data class Detail(val entryId: Long, val sessionId: Long = System.nanoTime()) : NavDestination()
    data class AddEdit(
        val entryId: Long? = null,
        val initialMediaType: MediaType? = null,
        val initialCustomCategory: String? = null,
        val initialSubCategory: String? = null,
        val initialStatus: MediaStatus? = null,
        val sessionId: Long = System.nanoTime()
    ) : NavDestination()
}

@Composable
fun AppNavigation() {
    val repository = LXApplication.instance.repository
    var backStack by remember { mutableStateOf(listOf<NavDestination>(NavDestination.Home)) }
    val currentDestination = backStack.last()
    val saveableStateHolder = rememberSaveableStateHolder()

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(repository))
    val homeState by homeViewModel.uiState.collectAsState()

    val context = LocalContext.current
    val activity = context as? Activity

    androidx.compose.runtime.LaunchedEffect(homeState.activeUser.id, homeState.isUserInitialized) {
        if (homeState.isUserInitialized) {
            ThemeManager.loadForProfile(homeState.activeUser.id, context)
        }
    }

    var isAppUnlocked by rememberSaveable { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(true) }

    val activePasskey = homeState.activeUser.passKey
    val isProtected = !activePasskey.isNullOrBlank()
    val needsUnlock = homeState.isUserInitialized && isProtected && !isAppUnlocked

    if (!homeState.isUserInitialized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LXBackgroundBlack)
        )
        return
    }

    if (needsUnlock) {
        BackHandler {
            activity?.finish()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LXBackgroundBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(LXPrimaryGreen.copy(alpha = 0.12f))
                        .border(1.5.dp, LXPrimaryGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Text(
                    text = "LX Track List",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )

                Text(
                    text = "Profile \"${homeState.activeUser.name}\" is protected by passkey",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LXTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { showUnlockDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LXPrimaryGreen,
                        contentColor = LXBackgroundBlack
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enter Passkey", fontWeight = FontWeight.Bold)
                }

                if (homeState.allUsers.size > 1) {
                    TextButton(onClick = { homeViewModel.setUserSwitcherOpen(true) }) {
                        Text("Switch Profile", color = LXPrimaryGreen, fontWeight = FontWeight.SemiBold)
                    }
                }

                TextButton(onClick = { activity?.finish() }) {
                    Text("Exit App", color = LXTextSecondary)
                }
            }

            if (showUnlockDialog) {
                PasskeyVerificationDialog(
                    profileName = homeState.activeUser.name,
                    expectedPasskey = activePasskey ?: "",
                    title = "Unlock Profile",
                    subtitle = "Enter 4-digit passkey to access \"${homeState.activeUser.name}\"",
                    dismissButtonText = "Exit App",
                    onSecondaryAction = if (homeState.allUsers.size > 1) {
                        { homeViewModel.setUserSwitcherOpen(true) }
                    } else null,
                    secondaryActionText = if (homeState.allUsers.size > 1) "Switch Profile" else null,
                    onSuccess = {
                        isAppUnlocked = true
                        showUnlockDialog = false
                    },
                    onDismiss = {
                        activity?.finish()
                    }
                )
            }
        }

        if (homeState.isUserSwitcherOpen) {
            UserSwitcherSheet(
                activeUser = homeState.activeUser,
                allUsers = homeState.allUsers,
                onSelectUser = { targetUser ->
                    homeViewModel.switchUser(targetUser)
                    if (targetUser.passKey.isNullOrBlank()) {
                        isAppUnlocked = true
                        showUnlockDialog = false
                    } else {
                        isAppUnlocked = true
                        showUnlockDialog = false
                    }
                    homeViewModel.setUserSwitcherOpen(false)
                },
                onCreateUser = { name, color, avatarUri, passKey ->
                    homeViewModel.createUser(name, color, avatarUri, passKey)
                    isAppUnlocked = true
                    showUnlockDialog = false
                    homeViewModel.setUserSwitcherOpen(false)
                },
                onUpdateUser = { homeViewModel.updateUser(it) },
                onDeleteUser = { homeViewModel.deleteUser(it) },
                onDismiss = { homeViewModel.setUserSwitcherOpen(false) }
            )
        }

        return
    }

    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModelFactory(repository))
    val searchViewModel: SearchViewModel = viewModel(factory = SearchViewModelFactory(repository))
    val statsViewModel: com.lx.tracklist.ui.stats.StatsViewModel = viewModel(
        factory = com.lx.tracklist.ui.stats.StatsViewModelFactory(repository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(repository))

    fun navigateTo(destination: NavDestination) {
        if (destination == currentDestination) return
        if (destination is NavDestination.AddEdit && currentDestination is NavDestination.AddEdit && destination.entryId == currentDestination.entryId) return
        if (destination is NavDestination.Detail && currentDestination is NavDestination.Detail && destination.entryId == currentDestination.entryId) return

        val isTopLevel = destination in listOf(
            NavDestination.Home,
            NavDestination.Library,
            NavDestination.Search,
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
            val popped = backStack.last()
            val poppedKey = when (popped) {
                is NavDestination.Home -> "screen_home"
                is NavDestination.Library -> "screen_library"
                is NavDestination.Search -> "screen_search"
                is NavDestination.Stats -> "screen_stats"
                is NavDestination.Settings -> "screen_settings"
                is NavDestination.Detail -> "screen_detail_${popped.entryId}_${popped.sessionId}"
                is NavDestination.AddEdit -> "screen_add_edit_${popped.entryId ?: 0L}_${popped.sessionId}"
            }
            saveableStateHolder.removeState(poppedKey)
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
        NavDestination.Search,
        NavDestination.Stats,
        NavDestination.Settings
    )

    val libraryState by libraryViewModel.uiState.collectAsState()
    val isMultiSelectActive = currentDestination == NavDestination.Library && libraryState.isSelectionMode

    val navItems = listOf(
        Screen.Home to NavDestination.Home,
        Screen.Library to NavDestination.Library,
        Screen.Search to NavDestination.Search,
        Screen.Stats to NavDestination.Stats,
        Screen.Settings to NavDestination.Settings
    )

    @Composable
    fun ScreenContent() {
        val dest = currentDestination
        val destKey = when (dest) {
            is NavDestination.Home -> "screen_home"
            is NavDestination.Library -> "screen_library"
            is NavDestination.Search -> "screen_search"
            is NavDestination.Stats -> "screen_stats"
            is NavDestination.Settings -> "screen_settings"
            is NavDestination.Detail -> "screen_detail_${dest.entryId}_${dest.sessionId}"
            is NavDestination.AddEdit -> "screen_add_edit_${dest.entryId ?: 0L}_${dest.sessionId}"
        }

        saveableStateHolder.SaveableStateProvider(key = destKey) {
            when (dest) {
                is NavDestination.Home -> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onEntryClick = { entryId -> navigateTo(NavDestination.Detail(entryId)) },
                        onAddNewEntry = { mediaType, customCategory, subCategory, status ->
                            navigateTo(
                                NavDestination.AddEdit(
                                    entryId = null,
                                    initialMediaType = mediaType,
                                    initialCustomCategory = customCategory,
                                    initialSubCategory = subCategory,
                                    initialStatus = status
                                )
                            )
                        },
                        onEditEntry = { entryId -> navigateTo(NavDestination.AddEdit(entryId)) }
                    )
                }

                is NavDestination.Library -> {
                    LibraryScreen(
                        viewModel = libraryViewModel,
                        onEntryClick = { entryId -> navigateTo(NavDestination.Detail(entryId)) },
                        onEditEntry = { entryId -> navigateTo(NavDestination.AddEdit(entryId)) },
                        onAddNewEntry = { mediaType, customCategory, subCategory, status ->
                            navigateTo(
                                NavDestination.AddEdit(
                                    entryId = null,
                                    initialMediaType = mediaType,
                                    initialCustomCategory = customCategory,
                                    initialSubCategory = subCategory,
                                    initialStatus = status
                                )
                            )
                        },
                        activeUser = homeState.activeUser,
                        onUserAvatarClick = { homeViewModel.setUserSwitcherOpen(true) }
                    )
                }

                is NavDestination.Search -> {
                    SearchScreen(
                        viewModel = searchViewModel,
                        onNavigateToDetail = { entryId -> navigateTo(NavDestination.Detail(entryId)) },
                        activeUser = homeState.activeUser,
                        onUserAvatarClick = { homeViewModel.setUserSwitcherOpen(true) }
                    )
                }

                is NavDestination.Stats -> {
                    com.lx.tracklist.ui.stats.StatsScreen(
                        viewModel = statsViewModel,
                        activeUser = homeState.activeUser,
                        onUserAvatarClick = { homeViewModel.setUserSwitcherOpen(true) }
                    )
                }

                is NavDestination.Settings -> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onUserAvatarClick = { homeViewModel.setUserSwitcherOpen(true) }
                    )
                }

                is NavDestination.Detail -> {
                    val detailViewModel: DetailViewModel = viewModel(
                        key = "detail_${dest.entryId}_${dest.sessionId}",
                        factory = DetailViewModelFactory(repository, dest.entryId)
                    )
                    DetailScreen(
                        viewModel = detailViewModel,
                        onNavigateBack = { navigateBack() },
                        onEdit = { entryId -> navigateTo(NavDestination.AddEdit(entryId)) },
                        onNavigateToDetail = { targetEntryId -> navigateTo(NavDestination.Detail(targetEntryId)) },
                        activeUser = homeState.activeUser,
                        onUserAvatarClick = { homeViewModel.setUserSwitcherOpen(true) }
                    )
                }

                is NavDestination.AddEdit -> {
                    val addEditViewModel: AddEditViewModel = viewModel(
                        key = "add_edit_${dest.entryId ?: 0L}_${dest.sessionId}",
                        factory = AddEditViewModelFactory(
                            repository = repository,
                            entryId = dest.entryId,
                            initialMediaType = dest.initialMediaType,
                            initialCustomCategory = dest.initialCustomCategory,
                            initialSubCategory = dest.initialSubCategory,
                            initialStatus = dest.initialStatus
                        )
                    )
                    AddEditScreen(
                        viewModel = addEditViewModel,
                        onNavigateBack = { navigateBack() },
                        activeUser = homeState.activeUser,
                        onUserAvatarClick = { homeViewModel.setUserSwitcherOpen(true) }
                    )
                }
            }
        }
    }

    // Global User Switcher Bottom Sheet
    if (homeState.isUserSwitcherOpen) {
        UserSwitcherSheet(
            activeUser = homeState.activeUser,
            allUsers = homeState.allUsers,
            onSelectUser = {
                homeViewModel.switchUser(it)
                backStack = listOf(NavDestination.Home)
            },
            onCreateUser = { name, color, avatarUri, passKey ->
                homeViewModel.createUser(name, color, avatarUri, passKey)
                backStack = listOf(NavDestination.Home)
            },
            onUpdateUser = { homeViewModel.updateUser(it) },
            onDeleteUser = {
                homeViewModel.deleteUser(it)
                backStack = listOf(NavDestination.Home)
            },
            onDismiss = { homeViewModel.setUserSwitcherOpen(false) }
        )
    }

    if (isTablet && isTopLevelScreen) {
        // Tablet Adaptive Navigation with NavigationRail on left
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(if (ThemeManager.effectiveBackgroundImage != null) Color.Transparent else LXBackgroundBlack)
        ) {
            NavigationRail(
                containerColor = LXSurfaceBlack.copy(alpha = if (ThemeManager.effectiveBackgroundImage != null) 0.90f else 1f),
                contentColor = LXTextPrimary,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(88.dp)
                    .border(width = 1.dp, color = LXBorder),
                header = {
                    Spacer(modifier = Modifier.height(16.dp))
                    UserAvatarButton(
                        activeUser = homeState.activeUser,
                        onClick = { homeViewModel.setUserSwitcherOpen(true) },
                        size = 46.dp,
                        paddingStart = 0.dp,
                        paddingEnd = 0.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            ) {
                navItems.forEach { (screen, dest) ->
                    val isSelected = currentDestination == dest
                    val isBeyond = ThemeManager.isBeyondTheme
                    val beyondIconRes = when (screen) {
                        Screen.Home -> R.drawable.theme_beyond_nav_home
                        Screen.Library -> R.drawable.theme_beyond_nav_library
                        Screen.Search -> R.drawable.theme_beyond_nav_search
                        Screen.Stats -> R.drawable.theme_beyond_nav_stats
                        Screen.Settings -> R.drawable.theme_beyond_nav_settings
                    }
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = { navigateTo(dest) },
                        icon = {
                            if (isBeyond) {
                                Image(
                                    painter = painterResource(beyondIconRes),
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(26.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = { Text(screen.title) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = if (isBeyond) Color.White else LXBackgroundBlack,
                            selectedTextColor = if (isBeyond) Color(0xFFFC8181) else LXPrimaryGreen,
                            indicatorColor = if (isBeyond) Color(0xFFC53030) else LXPrimaryGreen,
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
            containerColor = if (ThemeManager.effectiveBackgroundImage != null) Color.Transparent else LXBackgroundBlack,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isTopLevelScreen && !isMultiSelectActive) {
                    val isBeyond = ThemeManager.isBeyondTheme
                    androidx.compose.material3.Surface(
                        color = if (isBeyond) LXSurfaceBlack.copy(alpha = 0.92f) else LXSurfaceBlack.copy(alpha = if (ThemeManager.effectiveBackgroundImage != null) 0.90f else 1f),
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .border(1.dp, LXBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (isBeyond) {
                                Image(
                                    painter = painterResource(R.drawable.theme_beyond_nav_bar_frame),
                                    contentDescription = null,
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .height(64.dp)
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                navItems.forEach { (screen, dest) ->
                                    val isSelected = currentDestination == dest
                                    val beyondIconRes = when (screen) {
                                        Screen.Home -> R.drawable.theme_beyond_nav_home
                                        Screen.Library -> R.drawable.theme_beyond_nav_library
                                        Screen.Search -> R.drawable.theme_beyond_nav_search
                                        Screen.Stats -> R.drawable.theme_beyond_nav_stats
                                        Screen.Settings -> R.drawable.theme_beyond_nav_settings
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { navigateTo(dest) }
                                            .padding(vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .height(28.dp)
                                                .width(48.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isSelected) {
                                                        if (isBeyond) Color(0xFFC53030) else LXPrimaryGreen
                                                    } else Color.Transparent
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isBeyond) {
                                                Image(
                                                    painter = painterResource(beyondIconRes),
                                                    contentDescription = screen.title,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = screen.icon,
                                                    contentDescription = screen.title,
                                                    tint = if (isSelected) LXBackgroundBlack else LXTextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = screen.title,
                                            style = androidx.compose.ui.text.TextStyle(
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) {
                                                if (isBeyond) Color(0xFFFC8181) else LXPrimaryGreen
                                            } else LXTextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
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
