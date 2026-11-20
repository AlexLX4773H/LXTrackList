package com.lx.tracklist.ui.search

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.platform.LocalUriHandler
import com.lx.tracklist.util.ExternalLinkHelper
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lx.tracklist.R
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.UserProfile
import com.lx.tracklist.data.remote.GoogleBooksApiClient
import com.lx.tracklist.data.remote.OpenLibraryApiClient
import com.lx.tracklist.data.remote.TmdbApiClient
import com.lx.tracklist.data.remote.TmdbConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lx.tracklist.ui.components.UserAvatarButton
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXScoreGold
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import com.lx.tracklist.ui.theme.ThemeManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToDetail: (Long) -> Unit,
    activeUser: UserProfile? = null,
    onUserAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    var itemToAdd by remember { mutableStateOf<SearchResultItem?>(null) }
    var previewItem by remember { mutableStateOf<SearchResultItem?>(null) }
    var showDiscoverySheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.userAddedMessage) {
        state.userAddedMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearAddedMessage()
        }
    }

    if (showDiscoverySheet) {
        SearchDiscoverySheet(
            category = state.selectedCategory,
            activeFilter = state.activeDiscoveryFilter,
            onDismiss = { showDiscoverySheet = false },
            onApplyAnime = { animeFilter ->
                viewModel.applyAnimeDiscoveryFilter(animeFilter)
            },
            onApplyMovie = { movieFilter ->
                viewModel.applyMovieDiscoveryFilter(movieFilter, context)
            },
            onApplyTv = { tvFilter ->
                viewModel.applyTvDiscoveryFilter(tvFilter, context)
            },
            onApplyManga = { mangaFilter ->
                viewModel.applyMangaDiscoveryFilter(mangaFilter, context)
            },
            onApplyManhwa = { manhwaFilter ->
                viewModel.applyManhwaDiscoveryFilter(manhwaFilter, context)
            },
            onApplyLightNovel = { lightNovelFilter ->
                viewModel.applyLightNovelDiscoveryFilter(lightNovelFilter, context)
            },
            onApplyBook = { bookFilter ->
                viewModel.applyBookDiscoveryFilter(bookFilter, context)
            },
            onApplyGame = { gameFilter ->
                viewModel.applyGameDiscoveryFilter(gameFilter, context)
            },
            onResetFilter = {
                viewModel.clearDiscoveryFilter()
            }
        )
    }

    if (itemToAdd != null) {
        AddToLibraryDialog(
            item = itemToAdd!!,
            customCategories = state.customCategories.filter { it.mediaType == itemToAdd!!.mediaType }.map { it.name },
            subCategories = state.subCategories.map { it.name },
            onConfirm = { status, customCat, subCat, asFranchise ->
                viewModel.addToLibrary(
                    item = itemToAdd!!,
                    status = status,
                    customCategory = customCat,
                    subCategory = subCat,
                    context = context,
                    asFranchise = asFranchise
                )
                itemToAdd = null
            },
            onDismiss = { itemToAdd = null }
        )
    }

    if (previewItem != null) {
        SearchResultDetailSheet(
            item = previewItem!!,
            onDismiss = { previewItem = null },
            onAddClick = {
                itemToAdd = previewItem
                previewItem = null
            },
            onViewInLibrary = { entryId ->
                previewItem = null
                onNavigateToDetail(entryId)
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (ThemeManager.effectiveBackgroundImage != null) Color.Transparent else LXBackgroundBlack,
        contentWindowInsets = if (isTablet) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    UserAvatarButton(
                        activeUser = activeUser,
                        onClick = onUserAvatarClick
                    )
                },
                title = {
                    Text(
                        text = "Search & Discover",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                },
                actions = {
                    val isDiscoverSupported = true
                    if (isDiscoverSupported) {
                        IconButton(
                            onClick = { showDiscoverySheet = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Discovery Options",
                                    tint = if (state.activeDiscoveryFilter != null) LXPrimaryGreen else LXTextPrimary
                                )
                                if (state.activeDiscoveryFilter != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .align(Alignment.TopEnd)
                                            .background(LXPrimaryGreen, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (ThemeManager.effectiveBackgroundImage != null) Color.Transparent else LXBackgroundBlack)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isBeyond = ThemeManager.isBeyondTheme
            val isGate = ThemeManager.isGateTheme
            val isSky = ThemeManager.isSkyTheme
            val isSlime = ThemeManager.isSlimeTheme

            // Search Input Field
            val searchPlaceholder = when (state.selectedCategory) {
                SearchCategory.ANIME -> "Search anime titles on MyAnimeList..."
                SearchCategory.MANGA -> "Search manga titles on MAL / MangaDex..."
                SearchCategory.MANHWA -> "Search manhwa titles on MangaDex..."
                SearchCategory.MOVIES -> "Search movies on TMDB..."
                SearchCategory.TV_SHOWS -> "Search TV shows on TMDB..."
                SearchCategory.BOOKS -> "Search books by title, author, or ISBN..."
                SearchCategory.LIGHT_NOVELS -> "Search light novels & web novels..."
                SearchCategory.GAMES -> "Search games on IGDB / RAWG..."
            }

            if (isSlime) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(60.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.theme_slime_search_bar),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.matchParentSize()
                    )

                    BasicTextField(
                        value = state.query,
                        onValueChange = { viewModel.onQueryChanged(it, context) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        cursorBrush = SolidColor(Color(0xFF38BDF8)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.submitSearch(context) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (isTablet) 90.dp else 58.dp,
                                end = if (state.isLoading || state.query.isNotEmpty()) 42.dp else 20.dp,
                                top = if (isTablet) 12.dp else 8.dp,
                                bottom = if (isTablet) 14.dp else 18.dp
                            )
                            .align(Alignment.CenterStart),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (state.query.isEmpty()) {
                                    Text(
                                        text = searchPlaceholder,
                                        color = Color.White.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp, bottom = 8.dp)
                                .size(18.dp),
                            color = Color(0xFF38BDF8),
                            strokeWidth = 2.dp
                        )
                    } else if (state.query.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onQueryChanged("", context) },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 6.dp, bottom = 8.dp)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    if (isBeyond) {
                        Image(
                            painter = painterResource(R.drawable.theme_beyond_search_bar),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.matchParentSize()
                        )
                    } else if (isGate) {
                        Image(
                            painter = painterResource(R.drawable.theme_gate_search_bar),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.matchParentSize()
                        )
                    } else if (isSky) {
                        Image(
                            painter = painterResource(R.drawable.theme_sky_search_bar),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.matchParentSize()
                        )
                    }

                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { viewModel.onQueryChanged(it, context) },
                        placeholder = {
                            Text(
                                text = searchPlaceholder,
                                color = if (isGate || isSky) Color(0xFF9E9E96) else LXTextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = LXTextPrimary),
                        leadingIcon = if (isGate || isBeyond || isSky) {
                            // theme_gate_search_bar, theme_beyond_search_bar, and theme_sky_search_bar already have the icon baked into the left side.
                            // We provide a spacer so the text starts after the baked-in icon without stacking a duplicate icon.
                            { Spacer(modifier = Modifier.width(if (isTablet) 68.dp else 36.dp)) }
                        } else {
                            {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = if (isGate) Color(0xFFFBBF24) else if (isBeyond) Color(0xFFFC8181) else if (isSky) Color(0xFFFB923C) else LXPrimaryGreen,
                                    strokeWidth = 2.dp
                                )
                            } else if (state.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChanged("", context) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = if (isGate) Color(0xFFFBBF24) else if (isBeyond) Color(0xFFFC8181) else if (isSky) Color(0xFFFB923C) else LXTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isGate || isSky || isBeyond) Color.Transparent else LXSurfaceElevated,
                            unfocusedContainerColor = if (isGate || isSky || isBeyond) Color.Transparent else LXSurfaceElevated,
                            focusedBorderColor = if (isGate || isSky || isBeyond) Color.Transparent else LXPrimaryGreen,
                            unfocusedBorderColor = if (isGate || isSky || isBeyond) Color.Transparent else LXBorder,
                            focusedTextColor = LXTextPrimary,
                            unfocusedTextColor = LXTextPrimary,
                            cursorColor = if (isSky) Color(0xFF06B6D4) else if (isGate) Color(0xFFFBBF24) else if (isBeyond) Color(0xFFFC8181) else LXPrimaryGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.submitSearch(context) }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(SearchCategory.entries) { category ->
                    val isSelected = state.selectedCategory == category
                    if (isBeyond) {
                        com.lx.tracklist.ui.library.BeyondCategoryChip(
                            label = "${category.displayName} (${category.sourceName})",
                            isSelected = isSelected,
                            onClick = { viewModel.onCategorySelected(category, context) }
                        )
                    } else if (isSlime) {
                        com.lx.tracklist.ui.library.SlimeCategoryChip(
                            label = "${category.displayName} (${category.sourceName})",
                            isSelected = isSelected,
                            onClick = { viewModel.onCategorySelected(category, context) }
                        )
                    } else if (isGate) {
                        com.lx.tracklist.ui.library.GateCategoryChip(
                            label = "${category.displayName} (${category.sourceName})",
                            isSelected = isSelected,
                            onClick = { viewModel.onCategorySelected(category, context) }
                        )
                    } else if (isSky) {
                        com.lx.tracklist.ui.library.SkyCategoryChip(
                            label = "${category.displayName} (${category.sourceName})",
                            isSelected = isSelected,
                            onClick = { viewModel.onCategorySelected(category, context) }
                        )
                    } else {
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onCategorySelected(category, context) },
                            label = {
                                Text(
                                    text = "${category.displayName} (${category.sourceName})",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.height(34.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LXPrimaryGreen,
                                selectedLabelColor = LXBackgroundBlack,
                                containerColor = LXSurfaceElevated,
                                labelColor = LXTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) LXPrimaryGreen else LXBorder
                            )
                        )
                    }
                }
            }

            // TMDB / Jikan Attribution Strip (hidden when discovery results are active)
            if (state.activeDiscoveryFilter == null) {
                if (state.selectedCategory == SearchCategory.MOVIES || state.selectedCategory == SearchCategory.TV_SHOWS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(LXSurfaceBlack)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_tmdb_logo),
                            contentDescription = "TMDB",
                            modifier = Modifier.height(16.dp).width(24.dp)
                        )
                        Text(
                            text = TmdbConfig.ATTRIBUTION_TEXT,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LXTextSecondary.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    val hasMangaDex = state.results.any { it.externalSource == "MangaDex" }
                    val isAniListBackup = state.results.isNotEmpty() && state.results.all {
                        it.rawJikanDetails?.sourceProvider?.contains("AniList", ignoreCase = true) == true
                    }
                    val attributionText = when {
                        state.selectedCategory == SearchCategory.GAMES -> "Results from IGDB (RAWG fallback)"
                        state.selectedCategory == SearchCategory.BOOKS -> "Results from Google Books (Open Library fallback)"
                        state.selectedCategory == SearchCategory.LIGHT_NOVELS -> "Results from AniList (Jikan/MAL fallback)"
                        hasMangaDex -> "Results from MangaDex (with MAL/AniList backup)"
                        isAniListBackup -> "Results fetched from AniList (MAL backup)"
                        state.selectedCategory == SearchCategory.MANHWA -> "Results fetched from MangaDex / AniList"
                        else -> "Results fetched from MyAnimeList"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(LXSurfaceBlack)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = LXPrimaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = attributionText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LXTextSecondary.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // Active Discovery Filter Banner
            state.activeDiscoveryFilter?.let { filter ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LXPrimaryGreen.copy(alpha = 0.15f))
                            .border(1.dp, LXPrimaryGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = when (filter) {
                                    is DiscoveryFilter.Anime -> when (filter.mode) {
                                        AnimeDiscoveryMode.SEASONAL -> Icons.Default.Explore
                                        AnimeDiscoveryMode.MOST_POPULAR -> Icons.Default.LocalFireDepartment
                                    }
                                    is DiscoveryFilter.Movie -> when (filter.mode) {
                                        ContentDiscoveryMode.BY_YEAR -> Icons.Default.Movie
                                        ContentDiscoveryMode.MOST_POPULAR -> Icons.Default.LocalFireDepartment
                                    }
                                    is DiscoveryFilter.Tv -> when (filter.mode) {
                                        ContentDiscoveryMode.BY_YEAR -> Icons.Default.Tv
                                        ContentDiscoveryMode.MOST_POPULAR -> Icons.Default.LocalFireDepartment
                                    }
                                    is DiscoveryFilter.Game -> when (filter.mode) {
                                        ContentDiscoveryMode.BY_YEAR -> Icons.Default.SportsEsports
                                        ContentDiscoveryMode.MOST_POPULAR -> Icons.Default.LocalFireDepartment
                                    }
                                    is DiscoveryFilter.Manga -> when (filter.mode) {
                                        ContentDiscoveryMode.BY_YEAR -> Icons.AutoMirrored.Filled.MenuBook
                                        ContentDiscoveryMode.MOST_POPULAR -> Icons.Default.LocalFireDepartment
                                    }
                                    is DiscoveryFilter.Manhwa -> when (filter.mode) {
                                        ContentDiscoveryMode.BY_YEAR -> Icons.AutoMirrored.Filled.MenuBook
                                        ContentDiscoveryMode.MOST_POPULAR -> Icons.Default.LocalFireDepartment
                                    }
                                    is DiscoveryFilter.LightNovel -> when (filter.mode) {
                                        ContentDiscoveryMode.BY_YEAR -> Icons.Default.Book
                                        ContentDiscoveryMode.MOST_POPULAR -> Icons.Default.LocalFireDepartment
                                    }
                                    is DiscoveryFilter.Book -> when (filter.mode) {
                                        ContentDiscoveryMode.BY_YEAR -> Icons.Default.Book
                                        ContentDiscoveryMode.MOST_POPULAR -> Icons.Default.LocalFireDepartment
                                    }
                                },
                                contentDescription = null,
                                tint = LXPrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = LXPrimaryGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearDiscoveryFilter() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Filter",
                                tint = LXPrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    val incTags = filter.includedTags
                    val excTags = filter.excludedTags
                    if (incTags.isNotEmpty() || excTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            incTags.forEach { tag ->
                                item(key = "inc_$tag") {
                                    SearchFilterChipPill(
                                        label = "+$tag",
                                        isIncluded = true,
                                        onRemove = { viewModel.removeTagFilter(tag, context) }
                                    )
                                }
                            }
                            excTags.forEach { tag ->
                                item(key = "exc_$tag") {
                                    SearchFilterChipPill(
                                        label = "-$tag",
                                        isIncluded = false,
                                        onRemove = { viewModel.removeTagFilter(tag, context) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Results List / Empty States
            val activeDiscovery = state.activeDiscoveryFilter
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading && state.results.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = LXPrimaryGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (activeDiscovery != null) {
                                "Discovering ${activeDiscovery.label}..."
                            } else {
                                "Searching ${state.selectedCategory.displayName} on ${state.selectedCategory.sourceName}..."
                            },
                            color = LXTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (state.results.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (state.selectedCategory) {
                                SearchCategory.MOVIES, SearchCategory.TV_SHOWS -> Icons.Default.Movie
                                SearchCategory.BOOKS, SearchCategory.LIGHT_NOVELS -> Icons.Default.Book
                                SearchCategory.GAMES -> Icons.Default.SportsEsports
                                else -> Icons.Default.Search
                            },
                            contentDescription = null,
                            tint = LXTextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val cleanErrorMessage = state.errorMessage?.takeIf { !it.contains("cancel", ignoreCase = true) }
                        Text(
                            text = if (activeDiscovery != null) {
                                cleanErrorMessage ?: "No titles found for ${activeDiscovery.label}"
                            } else if (state.query.isBlank()) {
                                "Search ${state.selectedCategory.displayName} on ${state.selectedCategory.sourceName}"
                            } else {
                                cleanErrorMessage ?: "No results found for '${state.query}'"
                            },
                            color = LXTextSecondary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (activeDiscovery != null) {
                                "Try selecting another season, year, or sort order from the menu (☰)."
                            } else if (state.query.isBlank()) {
                                val seasonalHint = if (state.selectedCategory in listOf(SearchCategory.ANIME, SearchCategory.MOVIES, SearchCategory.TV_SHOWS, SearchCategory.GAMES)) {
                                    " Tap ☰ at the top left to explore seasonal & discovery lists."
                                } else ""
                                "Type at least 2 characters to discover and add entries directly to your library.$seasonalHint"
                            } else {
                                "Try searching by alternate title, romaji, or English name."
                            },
                            color = LXTextSecondary.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.results, key = { "${it.externalSource}_${it.externalId}_${it.title.hashCode()}" }) { item ->
                            SearchResultCard(
                                item = item,
                                onAddClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    itemToAdd = item
                                },
                                onViewClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    item.existingEntryId?.let(onNavigateToDetail)
                                },
                                onItemClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    previewItem = item
                                }
                            )
                        }

                        if (state.canLoadMore) {
                            item(key = "load_more_footer") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.isLoadingMore) {
                                        CircularProgressIndicator(
                                            color = LXPrimaryGreen,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        OutlinedButton(
                                            onClick = { viewModel.loadMore(context) },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.5f)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LXPrimaryGreen),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Load More Results", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: SearchResultItem,
    onAddClick: () -> Unit,
    onViewClick: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
        border = BorderStroke(1.dp, if (item.isAlreadyInLibrary) LXPrimaryGreen.copy(alpha = 0.4f) else LXBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Poster thumbnail
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LXSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (!item.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = when (item.mediaType) {
                            MediaType.MOVIE -> Icons.Default.Movie
                            MediaType.SHOW -> Icons.Default.Tv
                            MediaType.BOOK -> Icons.Default.Book
                            MediaType.GAME -> Icons.Default.SportsEsports
                            else -> Icons.Default.Search
                        },
                        contentDescription = null,
                        tint = LXTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Score pill overlay
                if (item.score != null && item.score > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = LXScoreGold,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format(Locale.US, "%.2f", item.score).trimEnd('0').trimEnd('.'),
                                color = LXScoreGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Info & Actions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 110.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!item.alternateTitle.isNullOrBlank() && !item.alternateTitle.equals(item.title, ignoreCase = true)) {
                        Text(
                            text = item.alternateTitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LXTextSecondary.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Metadata row: Year • Status • Units
                    val metaParts = mutableListOf<String>()
                    item.releaseYear?.let { metaParts.add(it) }
                    if (item.mediaType == MediaType.BOOK && !item.author.isNullOrBlank()) {
                        metaParts.add(item.author)
                    } else {
                        item.statusText?.let { metaParts.add(it) }
                    }
                    if (item.mediaType == MediaType.GAME && item.gamePlatforms.isNotEmpty()) {
                        val platSummary = if (item.gamePlatforms.size <= 2) {
                            item.gamePlatforms.joinToString(" • ")
                        } else {
                            "${item.gamePlatforms.take(2).joinToString(" • ")} +${item.gamePlatforms.size - 2}"
                        }
                        metaParts.add(platSummary)
                    }
                    if (item.mediaType == MediaType.GAME && item.isGameFranchise) {
                        metaParts.add("Series")
                    }
                    item.totalUnits?.takeIf { it > 0 }?.let {
                        val unitLabel = when (item.mediaType) {
                            MediaType.ANIME -> if (it == 1) "1 Ep" else "$it Eps"
                            MediaType.MANGA, MediaType.MANHWA -> if (it == 1) "1 Ch" else "$it Chs"
                            MediaType.SHOW -> if (it == 1) "1 Ep" else "$it Eps"
                            MediaType.BOOK -> if (it == 1) "1 page" else "$it pages"
                            MediaType.MOVIE -> null
                            MediaType.GAME -> null
                        }
                        if (unitLabel != null) metaParts.add(unitLabel)
                    }
                    item.durationMinutes?.takeIf { it > 0 }?.let {
                        metaParts.add("${it}m")
                    }

                    if (metaParts.isNotEmpty()) {
                        Text(
                            text = metaParts.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LXTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Original language on its own row below metadata
                    item.originalLanguageWithFlag?.let { langText ->
                        Text(
                            text = langText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LXTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.webUrl != null) {
                        val isTmdb = item.externalSource.equals("TMDB", ignoreCase = true)
                        var showCardMenu by remember { mutableStateOf(false) }
                        val context = LocalContext.current
                        val uriHandler = LocalUriHandler.current

                        Box {
                            IconButton(
                                onClick = {
                                    if (isTmdb) {
                                        showCardMenu = true
                                    } else {
                                        ExternalLinkHelper.openUrlSafely(item.webUrl, uriHandler, context)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = item.openWebLabel,
                                    tint = LXTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (isTmdb) {
                                DropdownMenu(
                                    expanded = showCardMenu,
                                    onDismissRequest = { showCardMenu = false },
                                    modifier = Modifier.background(LXSurfaceElevated)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("Open on TMDB", fontWeight = FontWeight.Bold, color = LXTextPrimary, fontSize = 12.sp)
                                                Text("themoviedb.org", fontSize = 10.sp, color = LXTextSecondary)
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = LXPrimaryGreen, modifier = Modifier.size(14.dp))
                                        },
                                        onClick = {
                                            showCardMenu = false
                                            ExternalLinkHelper.openUrlSafely(item.webUrl, uriHandler, context)
                                        }
                                    )
                                    HorizontalDivider(color = LXBorder)
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("Open on IMDb ⭐", fontWeight = FontWeight.Bold, color = Color(0xFFF5C518), fontSize = 12.sp)
                                                Text("Search on IMDb", fontSize = 10.sp, color = LXTextSecondary)
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color(0xFFF5C518), modifier = Modifier.size(14.dp))
                                        },
                                        onClick = {
                                            showCardMenu = false
                                            ExternalLinkHelper.openUrlSafely(ExternalLinkHelper.buildImdbUrl(item.title, item.imdbId), uriHandler, context)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("Open on Rotten Tomatoes 🍅", fontWeight = FontWeight.Bold, color = Color(0xFFFF523B), fontSize = 12.sp)
                                                Text("Search on Rotten Tomatoes", fontSize = 10.sp, color = LXTextSecondary)
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color(0xFFFF523B), modifier = Modifier.size(14.dp))
                                        },
                                        onClick = {
                                            showCardMenu = false
                                            ExternalLinkHelper.openUrlSafely(ExternalLinkHelper.buildRottenTomatoesUrl(item.title), uriHandler, context)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (item.isAlreadyInLibrary) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LXPrimaryGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable { onViewClick() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "In Library (${item.existingStatus?.defaultLabel ?: "Saved"})",
                                    color = LXPrimaryGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onAddClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LXPrimaryGreen,
                                contentColor = LXBackgroundBlack
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add to Library",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToLibraryDialog(
    item: SearchResultItem,
    customCategories: List<String>,
    subCategories: List<String>,
    onConfirm: (MediaStatus, String?, String?, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStatus by remember { mutableStateOf(MediaStatus.PLAN_TO_WATCH) }
    var selectedCustomCategory by remember { mutableStateOf<String?>(null) }
    var selectedSubCategory by remember { mutableStateOf<String?>(null) }
    var isFranchiseMode by remember(item.externalId) { mutableStateOf(item.isGameFranchise) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add to Library",
                color = LXTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Item Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!item.posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp, 64.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.mediaType.displayName} • ${item.externalSource}",
                            style = MaterialTheme.typography.bodySmall,
                            color = LXPrimaryGreen
                        )
                    }
                }

                // Game Tracking Mode Selection
                if (item.mediaType == MediaType.GAME) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Track As",
                            style = MaterialTheme.typography.labelMedium,
                            color = LXTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (!isFranchiseMode) LXPrimaryGreen else LXSurfaceElevated,
                                border = BorderStroke(1.dp, if (!isFranchiseMode) LXPrimaryGreen else LXBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isFranchiseMode = false }
                            ) {
                                Text(
                                    text = "Standalone Game",
                                    color = if (!isFranchiseMode) LXBackgroundBlack else LXTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (!isFranchiseMode) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isFranchiseMode) Color(0xFF8B5CF6) else LXSurfaceElevated,
                                border = BorderStroke(1.dp, if (isFranchiseMode) Color(0xFF8B5CF6) else LXBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isFranchiseMode = true }
                            ) {
                                Text(
                                    text = "Franchise / Series",
                                    color = if (isFranchiseMode) Color.White else LXTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isFranchiseMode) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Status Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = LXTextSecondary
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MediaStatus.entries.forEach { status ->
                            val isSelected = selectedStatus == status
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) LXPrimaryGreen else LXSurfaceElevated,
                                border = BorderStroke(1.dp, if (isSelected) LXPrimaryGreen else LXBorder),
                                modifier = Modifier.clickable { selectedStatus = status }
                            ) {
                                Text(
                                    text = status.getDisplayLabel(item.mediaType),
                                    color = if (isSelected) LXBackgroundBlack else LXTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Custom Category (if user has any)
                if (customCategories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Custom Category (Optional)",
                            style = MaterialTheme.typography.labelMedium,
                            color = LXTextSecondary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedCustomCategory == null) LXPrimaryGreen else LXSurfaceElevated,
                                border = BorderStroke(1.dp, if (selectedCustomCategory == null) LXPrimaryGreen else LXBorder),
                                modifier = Modifier.clickable { selectedCustomCategory = null }
                            ) {
                                Text(
                                    text = "Standard (${item.mediaType.displayName})",
                                    color = if (selectedCustomCategory == null) LXBackgroundBlack else LXTextPrimary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            customCategories.forEach { cat ->
                                val isSelected = selectedCustomCategory == cat
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) LXPrimaryGreen else LXSurfaceElevated,
                                    border = BorderStroke(1.dp, if (isSelected) LXPrimaryGreen else LXBorder),
                                    modifier = Modifier.clickable { selectedCustomCategory = cat }
                                ) {
                                    Text(
                                        text = cat,
                                        color = if (isSelected) LXBackgroundBlack else LXTextPrimary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedStatus, selectedCustomCategory, selectedSubCategory, isFranchiseMode)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LXPrimaryGreen,
                    contentColor = LXBackgroundBlack
                )
            ) {
                Text("Add to Library", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LXTextSecondary)
            }
        },
        containerColor = LXSurfaceElevated,
        shape = RoundedCornerShape(14.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun SearchResultDetailSheet(
    item: SearchResultItem,
    onDismiss: () -> Unit,
    onAddClick: () -> Unit,
    onViewInLibrary: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    var detailedSynopsis by remember(item.externalId) { mutableStateOf(item.synopsis) }
    var detailedAuthor by remember(item.externalId) { mutableStateOf(item.author) }
    var detailedPublisher by remember(item.externalId) { mutableStateOf(item.publisher) }
    var detailedIsbn by remember(item.externalId) { mutableStateOf(item.isbn) }
    var detailedSeasons by remember(item.externalId) { mutableStateOf(item.totalVolumes) }
    var detailedEpisodes by remember(item.externalId) { mutableStateOf(item.totalUnits) }
    var detailedDuration by remember(item.externalId) { mutableStateOf(item.durationMinutes) }
    var detailedImdbId by remember(item.externalId) { mutableStateOf<String?>(item.imdbId) }
    var isLoadingDetails by remember(item.externalId) { mutableStateOf(false) }

    LaunchedEffect(item.externalId) {
        if (item.mediaType == MediaType.SHOW && item.externalSource.equals("TMDB", ignoreCase = true) && (detailedEpisodes == null || detailedSeasons == null || detailedImdbId == null)) {
            val tmdbId = item.externalId.toLongOrNull()
            if (tmdbId != null) {
                isLoadingDetails = true
                withContext(Dispatchers.IO) {
                    try {
                        val tvDetails = TmdbApiClient.fetchTvDetails(tmdbId, context).getOrNull()
                        if (tvDetails != null) {
                            if (tvDetails.totalSeasons != null && tvDetails.totalSeasons > 0) {
                                detailedSeasons = tvDetails.totalSeasons
                            }
                            if (tvDetails.totalEpisodes != null && tvDetails.totalEpisodes > 0) {
                                detailedEpisodes = tvDetails.totalEpisodes
                            }
                            if (tvDetails.durationMinutes != null && detailedDuration == null) {
                                detailedDuration = tvDetails.durationMinutes
                            }
                            if (detailedSynopsis.isNullOrBlank() && !tvDetails.synopsis.isNullOrBlank()) {
                                detailedSynopsis = tvDetails.synopsis
                            }
                            if (!tvDetails.imdbId.isNullOrBlank()) {
                                detailedImdbId = tvDetails.imdbId
                            }
                        }
                    } catch (_: Exception) {}
                    isLoadingDetails = false
                }
            }
        } else if (item.mediaType == MediaType.MOVIE && item.externalSource.equals("TMDB", ignoreCase = true) && (detailedDuration == null || detailedImdbId == null)) {
            val tmdbId = item.externalId.toLongOrNull()
            if (tmdbId != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val movieDetails = TmdbApiClient.fetchMovieDetails(tmdbId, context).getOrNull()
                        if (movieDetails != null) {
                            if (movieDetails.durationMinutes != null) {
                                detailedDuration = movieDetails.durationMinutes
                            }
                            if (detailedSynopsis.isNullOrBlank() && !movieDetails.synopsis.isNullOrBlank()) {
                                detailedSynopsis = movieDetails.synopsis
                            }
                            if (!movieDetails.imdbId.isNullOrBlank()) {
                                detailedImdbId = movieDetails.imdbId
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        } else if (item.mediaType == MediaType.BOOK && (detailedSynopsis.isNullOrBlank() || detailedSynopsis == item.title || detailedSynopsis!!.startsWith("Author:"))) {
            isLoadingDetails = true
            withContext(Dispatchers.IO) {
                try {
                    if (item.externalSource.equals("Google Books", ignoreCase = true) && !item.externalId.startsWith("book_")) {
                        val fullBook = GoogleBooksApiClient.fetchVolumeDetails(item.externalId, context).getOrNull()
                        if (fullBook != null) {
                            if (!fullBook.description.isNullOrBlank()) {
                                detailedSynopsis = fullBook.description
                            }
                            if (detailedAuthor.isNullOrBlank() && fullBook.authors.isNotEmpty()) {
                                detailedAuthor = fullBook.authorString
                            }
                            if (detailedPublisher.isNullOrBlank() && !fullBook.publisher.isNullOrBlank()) {
                                detailedPublisher = fullBook.publisher
                            }
                            if (detailedIsbn.isNullOrBlank() && !fullBook.primaryIsbn.isNullOrBlank()) {
                                detailedIsbn = fullBook.primaryIsbn
                            }
                        }
                    } else if (item.externalSource.equals("Open Library", ignoreCase = true) && !item.externalId.startsWith("book_")) {
                        val cleanKey = item.externalId.removePrefix("/works/").removePrefix("/").trim()
                        val fullBook = OpenLibraryApiClient.fetchWorkDetails(cleanKey).getOrNull()
                        if (fullBook != null) {
                            if (!fullBook.description.isNullOrBlank()) {
                                detailedSynopsis = fullBook.description
                            }
                            if (detailedAuthor.isNullOrBlank() && fullBook.authors.isNotEmpty()) {
                                detailedAuthor = fullBook.authorString
                            }
                            if (detailedPublisher.isNullOrBlank() && !fullBook.publisher.isNullOrBlank()) {
                                detailedPublisher = fullBook.publisher
                            }
                            if (detailedIsbn.isNullOrBlank() && !fullBook.primaryIsbn.isNullOrBlank()) {
                                detailedIsbn = fullBook.primaryIsbn
                            }
                        }
                    }
                } catch (_: Exception) {}
                isLoadingDetails = false
            }
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                val isFlingingUp = available.y < 0
                val isExpanded = sheetState.targetValue == SheetValue.Expanded
                return if (isFlingingUp && isExpanded) {
                    available
                } else {
                    Velocity.Zero
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LXSurfaceBlack,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(LXBorder)
            )
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(nestedScrollConnection)
        ) {
            val maxSheetHeight = maxHeight * 0.90f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // Header: Poster + Title + Metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Poster
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(148.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.posterUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = when (item.mediaType) {
                                MediaType.MOVIE -> Icons.Default.Movie
                                MediaType.SHOW -> Icons.Default.Tv
                                MediaType.BOOK -> Icons.Default.Book
                                MediaType.GAME -> Icons.Default.SportsEsports
                                else -> Icons.Default.Search
                            },
                            contentDescription = null,
                            tint = LXTextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Info Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    clipboardManager.setText(AnnotatedString(item.title))
                                    Toast.makeText(context, "Copied title to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                    )

                    if (!item.alternateTitle.isNullOrBlank() && !item.alternateTitle.equals(item.title, ignoreCase = true)) {
                        Text(
                            text = item.alternateTitle.replace("\n", " • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = LXTextSecondary.copy(alpha = 0.8f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        clipboardManager.setText(AnnotatedString(item.alternateTitle))
                                        Toast.makeText(context, "Copied title to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                )
                        )
                    }

                    // Chips row (Format, Year, Score)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = LXPrimaryGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = item.statusText ?: item.mediaType.displayName,
                                color = LXPrimaryGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (!item.releaseYear.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = LXSurfaceElevated
                            ) {
                                Text(
                                    text = item.releaseYear,
                                    color = LXTextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (item.score != null && item.score > 0f) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = LXScoreGold.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = LXScoreGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", item.score).trimEnd('0').trimEnd('.')} (${item.externalSource})",
                                        color = LXScoreGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Original Language on the next row
                    item.originalLanguageWithFlag?.let { langText ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = LXSurfaceElevated
                        ) {
                            Text(
                                text = langText,
                                color = LXTextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    val seasons = detailedSeasons ?: item.totalVolumes
                    val episodes = detailedEpisodes ?: item.totalUnits
                    val duration = detailedDuration ?: item.durationMinutes

                    // Units/Episodes/Runtime
                    val unitsInfo = when (item.mediaType) {
                        MediaType.SHOW -> {
                            val seasonText = seasons?.takeIf { it > 0 }?.let { if (it == 1) "1 Season" else "$it Seasons" }
                            val epText = episodes?.takeIf { it > 0 }?.let { if (it == 1) "1 Episode" else "$it Episodes" }
                            when {
                                seasonText != null && epText != null -> "$seasonText • $epText"
                                seasonText != null -> seasonText
                                epText != null -> epText
                                isLoadingDetails -> "Loading details..."
                                else -> null
                            }
                        }
                        MediaType.ANIME -> {
                            val eps = episodes?.takeIf { it > 0 }?.let { "$it Episodes" } ?: "Episodes unknown"
                            val dur = duration?.takeIf { it > 0 }?.let { " • ${it}m/ep" } ?: ""
                            eps + dur
                        }
                        MediaType.MOVIE -> {
                            duration?.takeIf { it > 0 }?.let { "${it}m runtime" }
                        }
                        MediaType.MANGA, MediaType.MANHWA -> {
                            val chs = item.totalUnits?.takeIf { it > 0 }?.let { "$it Chapters" } ?: "Ongoing"
                            val vols = item.totalVolumes?.takeIf { it > 0 }?.let { " • $it Volumes" } ?: ""
                            chs + vols
                        }
                        MediaType.BOOK -> {
                            val pages = item.totalUnits?.takeIf { it > 0 }?.let { if (it == 1) "1 Page" else "$it Pages" }
                            val vols = item.totalVolumes?.takeIf { it > 0 }?.let { if (it == 1) "1 Volume" else "$it Volumes" }
                            val chs = item.rawBookDetails?.totalChapters?.takeIf { it > 0 }?.let { if (it == 1) "1 Chapter" else "$it Chapters" }
                            listOfNotNull(pages, vols, chs).joinToString(" • ").ifBlank { "Pages unknown" }
                        }
                        MediaType.GAME -> {
                            val plats = if (item.gamePlatforms.isNotEmpty()) item.gamePlatforms.joinToString(" • ") else null
                            val franchiseText = if (item.isGameFranchise) "Franchise / Series" else null
                            listOfNotNull(plats, franchiseText).joinToString(" • ").ifBlank { "Game" }
                        }
                    }
                    if (!unitsInfo.isNullOrBlank()) {
                        Text(
                            text = unitsInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = LXTextSecondary
                        )
                    }
                }
            }

            // Book Metadata Card (Authors, Publisher, ISBN)
            val displayAuthor = detailedAuthor ?: item.author ?: item.rawBookDetails?.authorString ?: item.rawJikanDetails?.authors
            val displayPublisher = detailedPublisher ?: item.publisher ?: item.rawBookDetails?.publisher
            val displayIsbn = detailedIsbn ?: item.isbn ?: item.rawBookDetails?.primaryIsbn ?: item.rawJikanDetails?.isbn

            if (item.mediaType == MediaType.BOOK && (!displayAuthor.isNullOrBlank() || !displayPublisher.isNullOrBlank() || !displayIsbn.isNullOrBlank())) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LXSurfaceElevated),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, LXBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!displayAuthor.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "Author:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = LXPrimaryGreen
                                )
                                Text(
                                    text = displayAuthor,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LXTextPrimary
                                )
                            }
                        }
                        if (!displayPublisher.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "Publisher:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = LXTextSecondary
                                )
                                Text(
                                    text = displayPublisher,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LXTextPrimary
                                )
                            }
                        }
                        if (!displayIsbn.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ISBN:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = LXTextSecondary
                                )
                                Text(
                                    text = displayIsbn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LXTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Tags / Genres
            val rawTags = item.rawJikanDetails?.tags
            if (!rawTags.isNullOrBlank()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rawTags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = LXSurfaceElevated,
                            border = BorderStroke(0.5.dp, LXBorder)
                        ) {
                            Text(
                                text = "#$tag",
                                color = LXTextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Synopsis Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LXSurfaceElevated),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LXBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Synopsis / Overview",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXPrimaryGreen
                    )
                    if (isLoadingDetails) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = LXPrimaryGreen
                            )
                            Text(
                                text = "Loading overview...",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextSecondary
                            )
                        }
                    } else {
                        val textToDisplay = detailedSynopsis?.takeIf { it.isNotBlank() }
                            ?: item.synopsis?.takeIf { it.isNotBlank() }
                            ?: "No synopsis or overview available for this title."
                        Text(
                            text = textToDisplay,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = LXTextPrimary
                        )
                    }
                }
            }

            // Attribution Notice
            if (item.externalSource.equals("TMDB", ignoreCase = true)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_tmdb_logo),
                        contentDescription = "TMDB",
                        modifier = Modifier.height(14.dp).width(20.dp)
                    )
                    Text(
                        text = "This product uses the TMDB API but is not endorsed or certified by TMDB.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = LXTextSecondary.copy(alpha = 0.6f)
                    )
                }
            } else if (item.externalSource.equals("Google Books", ignoreCase = true)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Google Books",
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Book metadata and covers provided by Google Books.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = LXTextSecondary.copy(alpha = 0.7f)
                    )
                }
            } else if (item.externalSource.equals("Open Library", ignoreCase = true)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Open Library",
                        tint = LXScoreGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Book metadata and covers provided by Open Library (Internet Archive).",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = LXTextSecondary.copy(alpha = 0.7f)
                    )
                }
            }

            // Bottom Actions
            val isMovieOrShow = item.mediaType == MediaType.MOVIE || item.mediaType == MediaType.SHOW || item.externalSource.equals("TMDB", ignoreCase = true)
            val isLightNovel = item.mediaType == MediaType.BOOK && (
                item.statusText?.contains("Light Novel", ignoreCase = true) == true ||
                item.statusText?.contains("Novel", ignoreCase = true) == true ||
                item.externalSource.startsWith("AniList", ignoreCase = true)
            )
            val isBook = item.mediaType == MediaType.BOOK && !isLightNovel
            val isAnime = item.mediaType == MediaType.ANIME
            val isMangaOrManhwa = item.mediaType == MediaType.MANGA || item.mediaType == MediaType.MANHWA

            val openOptions = remember(item, detailedImdbId) {
                when {
                    isMovieOrShow -> ExternalLinkHelper.buildTmdbOpenOptions(
                        title = item.title,
                        tmdbId = item.externalId,
                        isMovie = item.mediaType == MediaType.MOVIE,
                        imdbId = detailedImdbId ?: item.imdbId,
                        webUrl = item.webUrl
                    )
                    isAnime -> ExternalLinkHelper.buildAnimeOpenOptions(
                        title = item.title,
                        externalId = item.externalId,
                        externalSource = item.externalSource,
                        directMalId = item.directMalId ?: item.rawJikanDetails?.malId,
                        directAniListId = item.directAniListId ?: item.rawJikanDetails?.aniListId,
                        directSiteUrl = item.rawJikanDetails?.url ?: item.rawJikanDetails?.webUrl,
                        webUrl = item.webUrl
                    )
                    isLightNovel -> ExternalLinkHelper.buildLightNovelOpenOptions(
                        title = item.title,
                        externalId = item.externalId,
                        externalSource = item.externalSource,
                        directMalId = item.directMalId ?: item.rawJikanDetails?.malId,
                        directAniListId = item.directAniListId ?: item.rawJikanDetails?.aniListId,
                        directSiteUrl = item.rawJikanDetails?.url ?: item.rawJikanDetails?.webUrl,
                        webUrl = item.webUrl
                    )
                    isBook -> ExternalLinkHelper.buildBookOpenOptions(
                        title = item.title,
                        externalId = item.externalId,
                        externalSource = item.externalSource,
                        webUrl = item.webUrl
                    )
                    isMangaOrManhwa -> ExternalLinkHelper.buildMangaOpenOptions(
                        title = item.title,
                        externalId = item.externalId,
                        externalSource = item.externalSource,
                        directMalId = item.directMalId ?: item.rawJikanDetails?.malId,
                        directAniListId = item.directAniListId ?: item.rawJikanDetails?.aniListId,
                        directSiteUrl = item.rawJikanDetails?.url ?: item.rawJikanDetails?.webUrl,
                        webUrl = item.webUrl
                    )
                    else -> item.webUrl?.let {
                        listOf(ExternalLinkHelper.ExternalOpenOption("Open in Browser", it, it))
                    } ?: emptyList()
                }
            }

            var showOpenMenu by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (openOptions.isNotEmpty()) {
                    Box(modifier = Modifier.weight(1.15f)) {
                        OutlinedButton(
                            onClick = {
                                if (openOptions.size > 1) {
                                    showOpenMenu = true
                                } else {
                                    ExternalLinkHelper.openUrlSafely(
                                        openOptions.first().url,
                                        uriHandler,
                                        context,
                                        clipboardManager
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, LXBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LXPrimaryGreen),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (openOptions.size > 1) "Open ▾" else "Open",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (openOptions.size > 1) {
                            DropdownMenu(
                                expanded = showOpenMenu,
                                onDismissRequest = { showOpenMenu = false },
                                modifier = Modifier.background(LXSurfaceElevated)
                            ) {
                                openOptions.forEachIndexed { index, option ->
                                    if (index > 0) {
                                        HorizontalDivider(color = LXBorder)
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = option.title,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(option.color),
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = option.subtitle,
                                                    fontSize = 10.sp,
                                                    color = LXTextSecondary
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.OpenInNew,
                                                contentDescription = null,
                                                tint = Color(option.color),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        onClick = {
                                            showOpenMenu = false
                                            ExternalLinkHelper.openUrlSafely(
                                                option.url,
                                                uriHandler,
                                                context,
                                                clipboardManager
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Add to Library or In Library button
                val actionWeight = if (item.webUrl != null) 1.25f else 1f
                if (item.isAlreadyInLibrary && item.existingEntryId != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onViewInLibrary(item.existingEntryId)
                        },
                        modifier = Modifier
                            .weight(actionWeight)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LXPrimaryGreen,
                            contentColor = LXBackgroundBlack
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "In Library (${item.existingStatus?.defaultLabel ?: "Saved"})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Button(
                        onClick = onAddClick,
                        modifier = Modifier
                            .weight(actionWeight)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LXPrimaryGreen,
                            contentColor = LXBackgroundBlack
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add to Library",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
}

@Composable
private fun SearchFilterChipPill(
    label: String,
    isIncluded: Boolean,
    onRemove: () -> Unit
) {
    val bgColor = if (isIncluded) LXPrimaryGreen.copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
    val borderColor = if (isIncluded) LXPrimaryGreen.copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f)
    val textColor = if (isIncluded) LXPrimaryGreen else Color(0xFFEF4444)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove $label",
            tint = textColor,
            modifier = Modifier
                .size(12.dp)
                .clickable(onClick = onRemove)
        )
    }
}
