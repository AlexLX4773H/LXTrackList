package com.lx.tracklist.ui.search

import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.ui.library.TagSelectionState
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary

private data class TagDisplayItem(
    val name: String,
    val categoryLabel: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchDiscoverySheet(
    category: SearchCategory,
    activeFilter: DiscoveryFilter?,
    onDismiss: () -> Unit,
    onApplyAnime: (DiscoveryFilter.Anime) -> Unit,
    onApplyMovie: (DiscoveryFilter.Movie) -> Unit,
    onApplyTv: (DiscoveryFilter.Tv) -> Unit,
    onApplyManga: (DiscoveryFilter.Manga) -> Unit,
    onApplyManhwa: (DiscoveryFilter.Manhwa) -> Unit,
    onApplyLightNovel: (DiscoveryFilter.LightNovel) -> Unit,
    onApplyBook: (DiscoveryFilter.Book) -> Unit,
    onApplyGame: (DiscoveryFilter.Game) -> Unit = {},
    onResetFilter: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentYear = AnimeSeason.currentYear()
    val maxYear = AnimeSeason.maxDiscoveryYear() // currentYear + 2

    // Game state
    val gameFilter = activeFilter as? DiscoveryFilter.Game
    var selectedGameMode by remember {
        mutableStateOf(gameFilter?.mode ?: ContentDiscoveryMode.MOST_POPULAR)
    }
    var selectedGameYear by remember {
        mutableIntStateOf(gameFilter?.year ?: currentYear)
    }
    var selectedGameStartYear by remember {
        mutableStateOf<Int?>(gameFilter?.startYear ?: gameFilter?.year ?: currentYear)
    }
    var selectedGameEndYear by remember {
        mutableStateOf<Int?>(gameFilter?.endYear ?: gameFilter?.year ?: currentYear)
    }
    var selectedGameSort by remember {
        mutableStateOf(gameFilter?.sortOrder ?: GameSortOrder.POPULARITY)
    }
    var selectedGameLanguages by remember {
        mutableStateOf(gameFilter?.originalLanguages?.takeIf { it.isNotEmpty() } ?: gameFilter?.originalLanguage?.let { setOf(it) } ?: emptySet())
    }
    var gameIncludedTags by remember {
        mutableStateOf(gameFilter?.includedTags ?: emptySet())
    }
    var gameExcludedTags by remember {
        mutableStateOf(gameFilter?.excludedTags ?: emptySet())
    }
    var gameIncludedPlatforms by remember {
        mutableStateOf(gameFilter?.includedPlatforms ?: emptySet())
    }
    var gameExcludedPlatforms by remember {
        mutableStateOf(gameFilter?.excludedPlatforms ?: emptySet())
    }

    // Anime state
    val animeFilter = activeFilter as? DiscoveryFilter.Anime
    var selectedAnimeMode by remember {
        mutableStateOf(animeFilter?.mode ?: AnimeDiscoveryMode.SEASONAL)
    }
    var selectedSeason by remember {
        mutableStateOf(animeFilter?.season ?: AnimeSeason.current())
    }
    var selectedAnimeYear by remember {
        mutableIntStateOf(animeFilter?.year ?: currentYear)
    }
    var selectedAnimeSort by remember {
        mutableStateOf(animeFilter?.sortOrder ?: AnimeSortOrder.POPULARITY)
    }
    var selectedAnimeLanguages by remember {
        mutableStateOf(animeFilter?.originalLanguages?.takeIf { it.isNotEmpty() } ?: animeFilter?.originalLanguage?.let { setOf(it) } ?: emptySet())
    }
    var includedTags by remember {
        mutableStateOf(animeFilter?.includedTags ?: emptySet())
    }
    var excludedTags by remember {
        mutableStateOf(animeFilter?.excludedTags ?: emptySet())
    }

    // Movie state
    val movieFilter = activeFilter as? DiscoveryFilter.Movie
    var selectedMovieMode by remember {
        mutableStateOf(movieFilter?.mode ?: TmdbDiscoveryMode.MOST_POPULAR)
    }
    var selectedMovieYear by remember {
        mutableIntStateOf(movieFilter?.year ?: currentYear)
    }
    var selectedMovieStartYear by remember {
        mutableStateOf<Int?>(movieFilter?.startYear ?: movieFilter?.year ?: currentYear)
    }
    var selectedMovieEndYear by remember {
        mutableStateOf<Int?>(movieFilter?.endYear ?: movieFilter?.year ?: currentYear)
    }
    var selectedMovieSort by remember {
        mutableStateOf(movieFilter?.sortOrder ?: TmdbMovieSort.POPULARITY)
    }
    var selectedMovieLanguages by remember {
        mutableStateOf(movieFilter?.originalLanguages?.takeIf { it.isNotEmpty() } ?: movieFilter?.originalLanguage?.let { setOf(it) } ?: emptySet())
    }
    var movieIncludedTags by remember {
        mutableStateOf(movieFilter?.includedTags ?: emptySet())
    }
    var movieExcludedTags by remember {
        mutableStateOf(movieFilter?.excludedTags ?: emptySet())
    }

    // TV state
    val tvFilter = activeFilter as? DiscoveryFilter.Tv
    var selectedTvMode by remember {
        mutableStateOf(tvFilter?.mode ?: TmdbDiscoveryMode.MOST_POPULAR)
    }
    var selectedTvYear by remember {
        mutableIntStateOf(tvFilter?.year ?: currentYear)
    }
    var selectedTvStartYear by remember {
        mutableStateOf<Int?>(tvFilter?.startYear ?: tvFilter?.year ?: currentYear)
    }
    var selectedTvEndYear by remember {
        mutableStateOf<Int?>(tvFilter?.endYear ?: tvFilter?.year ?: currentYear)
    }
    var selectedTvSort by remember {
        mutableStateOf(tvFilter?.sortOrder ?: TmdbTvSort.POPULARITY)
    }
    var selectedTvLanguages by remember {
        mutableStateOf(tvFilter?.originalLanguages?.takeIf { it.isNotEmpty() } ?: tvFilter?.originalLanguage?.let { setOf(it) } ?: emptySet())
    }
    var tvIncludedTags by remember {
        mutableStateOf(tvFilter?.includedTags ?: emptySet())
    }
    var tvExcludedTags by remember {
        mutableStateOf(tvFilter?.excludedTags ?: emptySet())
    }

    // Manga state
    val mangaFilter = activeFilter as? DiscoveryFilter.Manga
    var selectedMangaMode by remember {
        mutableStateOf(mangaFilter?.mode ?: ContentDiscoveryMode.MOST_POPULAR)
    }
    var selectedMangaStartYear by remember {
        mutableStateOf<Int?>(mangaFilter?.startYear ?: currentYear)
    }
    var selectedMangaEndYear by remember {
        mutableStateOf<Int?>(mangaFilter?.endYear ?: currentYear)
    }
    var selectedMangaSort by remember {
        mutableStateOf(mangaFilter?.sortOrder ?: MangaSortOrder.POPULARITY)
    }
    var selectedMangaLanguages by remember {
        mutableStateOf(mangaFilter?.originalLanguages?.takeIf { it.isNotEmpty() } ?: mangaFilter?.originalLanguage?.let { setOf(it) } ?: emptySet())
    }
    var mangaIncludedTags by remember {
        mutableStateOf(mangaFilter?.includedTags ?: emptySet())
    }
    var mangaExcludedTags by remember {
        mutableStateOf(mangaFilter?.excludedTags ?: emptySet())
    }

    // Manhwa state
    val manhwaFilter = activeFilter as? DiscoveryFilter.Manhwa
    var selectedManhwaMode by remember {
        mutableStateOf(manhwaFilter?.mode ?: ContentDiscoveryMode.MOST_POPULAR)
    }
    var selectedManhwaStartYear by remember {
        mutableStateOf<Int?>(manhwaFilter?.startYear ?: currentYear)
    }
    var selectedManhwaEndYear by remember {
        mutableStateOf<Int?>(manhwaFilter?.endYear ?: currentYear)
    }
    var selectedManhwaSort by remember {
        mutableStateOf(manhwaFilter?.sortOrder ?: MangaSortOrder.POPULARITY)
    }
    var selectedManhwaLanguages by remember {
        mutableStateOf(manhwaFilter?.originalLanguages?.takeIf { it.isNotEmpty() } ?: manhwaFilter?.originalLanguage?.let { setOf(it) } ?: emptySet())
    }
    var manhwaIncludedTags by remember {
        mutableStateOf(manhwaFilter?.includedTags ?: emptySet())
    }
    var manhwaExcludedTags by remember {
        mutableStateOf(manhwaFilter?.excludedTags ?: emptySet())
    }

    // Light Novel state
    val lightNovelFilter = activeFilter as? DiscoveryFilter.LightNovel
    var selectedLightNovelMode by remember {
        mutableStateOf(lightNovelFilter?.mode ?: ContentDiscoveryMode.MOST_POPULAR)
    }
    var selectedLightNovelStartYear by remember {
        mutableStateOf<Int?>(lightNovelFilter?.startYear ?: currentYear)
    }
    var selectedLightNovelEndYear by remember {
        mutableStateOf<Int?>(lightNovelFilter?.endYear ?: currentYear)
    }
    var selectedLightNovelSort by remember {
        mutableStateOf(lightNovelFilter?.sortOrder ?: MangaSortOrder.POPULARITY)
    }
    var selectedLightNovelLanguages by remember {
        mutableStateOf(lightNovelFilter?.originalLanguages?.takeIf { it.isNotEmpty() } ?: lightNovelFilter?.originalLanguage?.let { setOf(it) } ?: emptySet())
    }
    var lightNovelIncludedTags by remember {
        mutableStateOf(lightNovelFilter?.includedTags ?: emptySet())
    }
    var lightNovelExcludedTags by remember {
        mutableStateOf(lightNovelFilter?.excludedTags ?: emptySet())
    }

    // Book state
    val bookFilter = activeFilter as? DiscoveryFilter.Book
    var selectedBookMode by remember {
        mutableStateOf(bookFilter?.mode ?: ContentDiscoveryMode.MOST_POPULAR)
    }
    var selectedBookStartYear by remember {
        mutableStateOf<Int?>(bookFilter?.startYear ?: currentYear)
    }
    var selectedBookEndYear by remember {
        mutableStateOf<Int?>(bookFilter?.endYear ?: currentYear)
    }
    var selectedBookSort by remember {
        mutableStateOf(bookFilter?.sortOrder ?: BookSortOrder.POPULARITY)
    }
    var selectedBookLanguages by remember {
        mutableStateOf(bookFilter?.originalLanguages?.takeIf { it.isNotEmpty() } ?: bookFilter?.originalLanguage?.let { setOf(it) } ?: emptySet())
    }
    var bookIncludedTags by remember {
        mutableStateOf(bookFilter?.includedTags ?: emptySet())
    }
    var bookExcludedTags by remember {
        mutableStateOf(bookFilter?.excludedTags ?: emptySet())
    }

    // Music state
    var selectedMusicLanguages by remember { mutableStateOf(emptySet<String>()) }
    var musicIncludedTags by remember { mutableStateOf(emptySet<String>()) }
    var musicExcludedTags by remember { mutableStateOf(emptySet<String>()) }

    var adultMode by remember {
        mutableStateOf(activeFilter?.adultMode ?: AdultMode.OFF)
    }

    var yearDropdownExpanded by remember { mutableStateOf(false) }
    var tagSearchQuery by remember { mutableStateOf("") }
    var selectedTagCategory by remember { mutableStateOf<AnimeTagCategory?>(null) }
    var showTagsSubMenu by remember { mutableStateOf(false) }
    var showLanguageSubMenu by remember { mutableStateOf(false) }
    var showPlatformsSubMenu by remember { mutableStateOf(false) }

    val currentSelectedLanguages: Set<String> = when (category) {
        SearchCategory.ANIME -> selectedAnimeLanguages
        SearchCategory.MOVIES -> selectedMovieLanguages
        SearchCategory.TV_SHOWS -> selectedTvLanguages
        SearchCategory.MANGA -> selectedMangaLanguages
        SearchCategory.MANHWA -> selectedManhwaLanguages
        SearchCategory.LIGHT_NOVELS -> selectedLightNovelLanguages
        SearchCategory.BOOKS -> selectedBookLanguages
        SearchCategory.GAMES -> selectedGameLanguages
        SearchCategory.MUSIC -> selectedMusicLanguages
    }

    fun setCategoryLanguages(languages: Set<String>) {
        when (category) {
            SearchCategory.ANIME -> selectedAnimeLanguages = languages
            SearchCategory.MOVIES -> selectedMovieLanguages = languages
            SearchCategory.TV_SHOWS -> selectedTvLanguages = languages
            SearchCategory.MANGA -> selectedMangaLanguages = languages
            SearchCategory.MANHWA -> selectedManhwaLanguages = languages
            SearchCategory.LIGHT_NOVELS -> selectedLightNovelLanguages = languages
            SearchCategory.BOOKS -> selectedBookLanguages = languages
            SearchCategory.GAMES -> selectedGameLanguages = languages
            SearchCategory.MUSIC -> selectedMusicLanguages = languages
        }
    }

    fun toggleCategoryLanguage(langCode: String) {
        val existing = currentSelectedLanguages.firstOrNull { OriginalLanguageRegistry.isSameLanguage(it, langCode) }
        val newSet = if (existing != null) {
            currentSelectedLanguages.filterNot { OriginalLanguageRegistry.isSameLanguage(it, langCode) }.toSet()
        } else {
            currentSelectedLanguages + langCode
        }
        setCategoryLanguages(newSet)
    }

    fun clearCategoryLanguages() {
        setCategoryLanguages(emptySet())
    }

    val activeIncludedTags: Set<String> = when (category) {
        SearchCategory.ANIME -> includedTags
        SearchCategory.MOVIES -> movieIncludedTags
        SearchCategory.TV_SHOWS -> tvIncludedTags
        SearchCategory.MANGA -> mangaIncludedTags
        SearchCategory.MANHWA -> manhwaIncludedTags
        SearchCategory.LIGHT_NOVELS -> lightNovelIncludedTags
        SearchCategory.BOOKS -> bookIncludedTags
        SearchCategory.GAMES -> gameIncludedTags
        SearchCategory.MUSIC -> musicIncludedTags
    }

    val activeExcludedTags: Set<String> = when (category) {
        SearchCategory.ANIME -> excludedTags
        SearchCategory.MOVIES -> movieExcludedTags
        SearchCategory.TV_SHOWS -> tvExcludedTags
        SearchCategory.MANGA -> mangaExcludedTags
        SearchCategory.MANHWA -> manhwaExcludedTags
        SearchCategory.LIGHT_NOVELS -> lightNovelExcludedTags
        SearchCategory.BOOKS -> bookExcludedTags
        SearchCategory.GAMES -> gameExcludedTags
        SearchCategory.MUSIC -> musicExcludedTags
    }

    val backDispatcherOwner = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current
    if (backDispatcherOwner != null) {
        BackHandler(enabled = showTagsSubMenu || showLanguageSubMenu || showPlatformsSubMenu) {
            when {
                showPlatformsSubMenu -> showPlatformsSubMenu = false
                showLanguageSubMenu -> showLanguageSubMenu = false
                showTagsSubMenu -> showTagsSubMenu = false
            }
        }
    }

    fun cyclePlatform(platformName: String) {
        val clean = platformName.trim()
        val isInc = gameIncludedPlatforms.any { it.equals(clean, ignoreCase = true) }
        val isExc = gameExcludedPlatforms.any { it.equals(clean, ignoreCase = true) }
        when {
            !isInc && !isExc -> {
                gameIncludedPlatforms = gameIncludedPlatforms + clean
                gameExcludedPlatforms = gameExcludedPlatforms.filter { !it.equals(clean, ignoreCase = true) }.toSet()
            }
            isInc -> {
                gameIncludedPlatforms = gameIncludedPlatforms.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                gameExcludedPlatforms = gameExcludedPlatforms + clean
            }
            else -> {
                gameIncludedPlatforms = gameIncludedPlatforms.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                gameExcludedPlatforms = gameExcludedPlatforms.filter { !it.equals(clean, ignoreCase = true) }.toSet()
            }
        }
    }

    fun clearGamePlatforms() {
        gameIncludedPlatforms = emptySet()
        gameExcludedPlatforms = emptySet()
    }

    fun cycleTag(tagName: String) {
        val clean = tagName.trim()
        when (category) {
            SearchCategory.ANIME -> {
                val isInc = includedTags.any { it.equals(clean, ignoreCase = true) }
                val isExc = excludedTags.any { it.equals(clean, ignoreCase = true) }
                when {
                    !isInc && !isExc -> {
                        includedTags = includedTags + clean
                        excludedTags = excludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                    isInc -> {
                        includedTags = includedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        excludedTags = excludedTags + clean
                    }
                    else -> {
                        includedTags = includedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        excludedTags = excludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                }
            }
            SearchCategory.MOVIES -> {
                val isInc = movieIncludedTags.any { it.equals(clean, ignoreCase = true) }
                val isExc = movieExcludedTags.any { it.equals(clean, ignoreCase = true) }
                when {
                    !isInc && !isExc -> {
                        movieIncludedTags = movieIncludedTags + clean
                        movieExcludedTags = movieExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                    isInc -> {
                        movieIncludedTags = movieIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        movieExcludedTags = movieExcludedTags + clean
                    }
                    else -> {
                        movieIncludedTags = movieIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        movieExcludedTags = movieExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                }
            }
            SearchCategory.TV_SHOWS -> {
                val isInc = tvIncludedTags.any { it.equals(clean, ignoreCase = true) }
                val isExc = tvExcludedTags.any { it.equals(clean, ignoreCase = true) }
                when {
                    !isInc && !isExc -> {
                        tvIncludedTags = tvIncludedTags + clean
                        tvExcludedTags = tvExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                    isInc -> {
                        tvIncludedTags = tvIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        tvExcludedTags = tvExcludedTags + clean
                    }
                    else -> {
                        tvIncludedTags = tvIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        tvExcludedTags = tvExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                }
            }
            SearchCategory.MANGA -> {
                val isInc = mangaIncludedTags.any { it.equals(clean, ignoreCase = true) }
                val isExc = mangaExcludedTags.any { it.equals(clean, ignoreCase = true) }
                when {
                    !isInc && !isExc -> {
                        mangaIncludedTags = mangaIncludedTags + clean
                        mangaExcludedTags = mangaExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                    isInc -> {
                        mangaIncludedTags = mangaIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        mangaExcludedTags = mangaExcludedTags + clean
                    }
                    else -> {
                        mangaIncludedTags = mangaIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        mangaExcludedTags = mangaExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                }
            }
            SearchCategory.MANHWA -> {
                val isInc = manhwaIncludedTags.any { it.equals(clean, ignoreCase = true) }
                val isExc = manhwaExcludedTags.any { it.equals(clean, ignoreCase = true) }
                when {
                    !isInc && !isExc -> {
                        manhwaIncludedTags = manhwaIncludedTags + clean
                        manhwaExcludedTags = manhwaExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                    isInc -> {
                        manhwaIncludedTags = manhwaIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        manhwaExcludedTags = manhwaExcludedTags + clean
                    }
                    else -> {
                        manhwaIncludedTags = manhwaIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        manhwaExcludedTags = manhwaExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                }
            }
            SearchCategory.LIGHT_NOVELS -> {
                val isInc = lightNovelIncludedTags.any { it.equals(clean, ignoreCase = true) }
                val isExc = lightNovelExcludedTags.any { it.equals(clean, ignoreCase = true) }
                when {
                    !isInc && !isExc -> {
                        lightNovelIncludedTags = lightNovelIncludedTags + clean
                        lightNovelExcludedTags = lightNovelExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                    isInc -> {
                        lightNovelIncludedTags = lightNovelIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        lightNovelExcludedTags = lightNovelExcludedTags + clean
                    }
                    else -> {
                        lightNovelIncludedTags = lightNovelIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        lightNovelExcludedTags = lightNovelExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                }
            }
            SearchCategory.BOOKS -> {
                val isInc = bookIncludedTags.any { it.equals(clean, ignoreCase = true) }
                val isExc = bookExcludedTags.any { it.equals(clean, ignoreCase = true) }
                when {
                    !isInc && !isExc -> {
                        bookIncludedTags = bookIncludedTags + clean
                        bookExcludedTags = bookExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                    isInc -> {
                        bookIncludedTags = bookIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        bookExcludedTags = bookExcludedTags + clean
                    }
                    else -> {
                        bookIncludedTags = bookIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        bookExcludedTags = bookExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                }
            }
            SearchCategory.GAMES -> {
                val isInc = gameIncludedTags.any { it.equals(clean, ignoreCase = true) }
                val isExc = gameExcludedTags.any { it.equals(clean, ignoreCase = true) }
                when {
                    !isInc && !isExc -> {
                        gameIncludedTags = gameIncludedTags + clean
                        gameExcludedTags = gameExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                    isInc -> {
                        gameIncludedTags = gameIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        gameExcludedTags = gameExcludedTags + clean
                    }
                    else -> {
                        gameIncludedTags = gameIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        gameExcludedTags = gameExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                }
            }
            SearchCategory.MUSIC -> {
                val isInc = musicIncludedTags.any { it.equals(clean, ignoreCase = true) }
                val isExc = musicExcludedTags.any { it.equals(clean, ignoreCase = true) }
                when {
                    !isInc && !isExc -> {
                        musicIncludedTags = musicIncludedTags + clean
                        musicExcludedTags = musicExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                    isInc -> {
                        musicIncludedTags = musicIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        musicExcludedTags = musicExcludedTags + clean
                    }
                    else -> {
                        musicIncludedTags = musicIncludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                        musicExcludedTags = musicExcludedTags.filter { !it.equals(clean, ignoreCase = true) }.toSet()
                    }
                }
            }
        }
    }

    fun getTagState(tagName: String): TagSelectionState {
        val clean = tagName.trim()
        return when {
            activeIncludedTags.any { it.equals(clean, ignoreCase = true) } -> TagSelectionState.INCLUDED
            activeExcludedTags.any { it.equals(clean, ignoreCase = true) } -> TagSelectionState.EXCLUDED
            else -> TagSelectionState.UNSELECTED
        }
    }

    fun clearAllCurrentTags() {
        when (category) {
            SearchCategory.ANIME -> {
                includedTags = emptySet()
                excludedTags = emptySet()
            }
            SearchCategory.MOVIES -> {
                movieIncludedTags = emptySet()
                movieExcludedTags = emptySet()
            }
            SearchCategory.TV_SHOWS -> {
                tvIncludedTags = emptySet()
                tvExcludedTags = emptySet()
            }
            SearchCategory.MANGA -> {
                mangaIncludedTags = emptySet()
                mangaExcludedTags = emptySet()
            }
            SearchCategory.MANHWA -> {
                manhwaIncludedTags = emptySet()
                manhwaExcludedTags = emptySet()
            }
            SearchCategory.LIGHT_NOVELS -> {
                lightNovelIncludedTags = emptySet()
                lightNovelExcludedTags = emptySet()
            }
            SearchCategory.BOOKS -> {
                bookIncludedTags = emptySet()
                bookExcludedTags = emptySet()
            }
            SearchCategory.GAMES -> {
                gameIncludedTags = emptySet()
                gameExcludedTags = emptySet()
            }
            SearchCategory.MUSIC -> {
                musicIncludedTags = emptySet()
                musicExcludedTags = emptySet()
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
        if (showTagsSubMenu) {
            // ================= TAGS SEPARATE PAGE (Like Library Advanced Search) =================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    val hasSelectedTags = activeIncludedTags.isNotEmpty() || activeExcludedTags.isNotEmpty()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showTagsSubMenu = false }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (hasSelectedTags) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (hasSelectedTags) "Apply" else "Back",
                            tint = LXPrimaryGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (category) {
                                SearchCategory.ANIME -> "Tags & Genres"
                                SearchCategory.MOVIES -> "Movie Genres"
                                SearchCategory.TV_SHOWS -> "TV Show Genres"
                                SearchCategory.MANGA -> "Manga Tags & Genres"
                                SearchCategory.MANHWA -> "Manhwa Tags & Genres"
                                SearchCategory.LIGHT_NOVELS -> "Light Novel Tags & Genres"
                                SearchCategory.BOOKS -> "Book Genres & Subjects"
                                SearchCategory.GAMES -> "Game Genres"
                                SearchCategory.MUSIC -> "Music Genres"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }

                    if (hasSelectedTags) {
                        TextButton(
                            onClick = { clearAllCurrentTags() },
                            modifier = Modifier.align(Alignment.CenterEnd),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Clear All", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = LXBorder, thickness = 1.dp)

                // Sub-menu Instructions & active count
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(LXPrimaryGreen, RoundedCornerShape(3.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = LXBackgroundBlack, modifier = Modifier.size(12.dp))
                            }
                            Text("1 tap = Include", fontSize = 11.sp, color = LXPrimaryGreen, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color(0xFFEF4444), RoundedCornerShape(3.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                            }
                            Text("2 taps = Exclude", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    HorizontalDivider(color = LXBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Filter",
                            style = MaterialTheme.typography.labelSmall,
                            color = LXTextSecondary
                        )
                        Text(
                            text = "${activeIncludedTags.size} inc • ${activeExcludedTags.size} exc",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (activeIncludedTags.isNotEmpty() || activeExcludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary,
                            maxLines = 1
                        )
                    }
                }

                // Search Bar for Tags
                OutlinedTextField(
                    value = tagSearchQuery,
                    onValueChange = { tagSearchQuery = it },
                    placeholder = {
                        Text(
                            text = when (category) {
                                SearchCategory.ANIME, SearchCategory.MANGA, SearchCategory.MANHWA, SearchCategory.LIGHT_NOVELS -> "Search tags, genres, themes..."
                                SearchCategory.BOOKS -> "Search genres, subjects..."
                                else -> "Search genres..."
                            },
                            color = LXTextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = LXTextSecondary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (tagSearchQuery.isNotBlank()) {
                            IconButton(onClick = { tagSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = LXTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LXPrimaryGreen,
                        unfocusedBorderColor = LXBorder,
                        focusedContainerColor = LXSurfaceElevated,
                        unfocusedContainerColor = LXSurfaceElevated,
                        focusedTextColor = LXTextPrimary,
                        unfocusedTextColor = LXTextPrimary
                    )
                )

                // Category filter chips (All, Genres, Themes, Demographics)
                if (category in listOf(SearchCategory.ANIME, SearchCategory.MANGA, SearchCategory.MANHWA, SearchCategory.LIGHT_NOVELS)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val categories = listOf<AnimeTagCategory?>(null, AnimeTagCategory.GENRE, AnimeTagCategory.THEME, AnimeTagCategory.DEMOGRAPHIC)
                        categories.forEach { cat ->
                            val label = cat?.displayName ?: "All"
                            val isCatSelected = selectedTagCategory == cat
                            FilterChip(
                                selected = isCatSelected,
                                onClick = { selectedTagCategory = cat },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = LXPrimaryGreen,
                                    containerColor = LXSurfaceElevated,
                                    labelColor = LXTextSecondary
                                ),
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }
                }

                // Tag list with tri-state selection (Matching Library Advanced Filter)
                val visibleItems = remember(category, tagSearchQuery, selectedTagCategory) {
                    when (category) {
                        SearchCategory.ANIME, SearchCategory.MANGA, SearchCategory.MANHWA, SearchCategory.LIGHT_NOVELS ->
                            AnimeTagRegistry.filterTags(tagSearchQuery, selectedTagCategory).map {
                                TagDisplayItem(it.name, it.category.displayName)
                            }
                        SearchCategory.MOVIES -> TmdbTagRegistry.filterMovieGenres(tagSearchQuery).map {
                            TagDisplayItem(it.name, "Genre")
                        }
                        SearchCategory.TV_SHOWS -> TmdbTagRegistry.filterTvGenres(tagSearchQuery).map {
                            TagDisplayItem(it.name, "Genre")
                        }
                        SearchCategory.BOOKS -> BookTagRegistry.filterBookGenres(tagSearchQuery).map {
                            TagDisplayItem(it.name, "Genre")
                        }
                        SearchCategory.GAMES -> GameTagRegistry.filterGameGenres(tagSearchQuery).map {
                            TagDisplayItem(it.name, "Genre")
                        }
                        SearchCategory.MUSIC -> MusicTagRegistry.filterMusicGenres(tagSearchQuery).map {
                            TagDisplayItem(it.name, "Genre")
                        }
                    }
                }

                if (visibleItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LXSurfaceElevated)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tags matching \"$tagSearchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LXTextSecondary
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        visibleItems.forEach { tagDef ->
                            val tagState = getTagState(tagDef.name)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (tagState) {
                                            TagSelectionState.INCLUDED -> LXPrimaryGreen.copy(alpha = 0.12f)
                                            TagSelectionState.EXCLUDED -> Color(0xFFEF4444).copy(alpha = 0.12f)
                                            TagSelectionState.UNSELECTED -> LXSurfaceElevated
                                        }
                                    )
                                    .border(
                                        width = if (tagState != TagSelectionState.UNSELECTED) 1.dp else 0.5.dp,
                                        color = when (tagState) {
                                            TagSelectionState.INCLUDED -> LXPrimaryGreen.copy(alpha = 0.6f)
                                            TagSelectionState.EXCLUDED -> Color(0xFFEF4444).copy(alpha = 0.6f)
                                            TagSelectionState.UNSELECTED -> LXBorder
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { cycleTag(tagDef.name) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    TriStateTagCheckbox(state = tagState)

                                    Text(
                                        text = tagDef.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (tagState != TagSelectionState.UNSELECTED) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = when (tagState) {
                                            TagSelectionState.INCLUDED -> LXPrimaryGreen
                                            TagSelectionState.EXCLUDED -> Color(0xFFEF4444)
                                            TagSelectionState.UNSELECTED -> LXTextPrimary
                                        }
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    when (tagState) {
                                        TagSelectionState.INCLUDED -> {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(LXPrimaryGreen.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Included",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    color = LXPrimaryGreen
                                                )
                                            }
                                        }
                                        TagSelectionState.EXCLUDED -> {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Excluded",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    color = Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                        TagSelectionState.UNSELECTED -> {
                                            Text(
                                                text = tagDef.categoryLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = LXTextSecondary.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { showTagsSubMenu = false },
                    colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Done (${activeIncludedTags.size} included • ${activeExcludedTags.size} excluded)",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        } else if (showLanguageSubMenu) {
            SearchDiscoveryLanguageSubMenu(
                category = category,
                currentSelectedLanguages = currentSelectedLanguages,
                onToggleLanguage = { toggleCategoryLanguage(it) },
                onClearAll = { clearCategoryLanguages() },
                onDismiss = { showLanguageSubMenu = false }
            )
        } else if (showPlatformsSubMenu) {
            GamePlatformsSubMenu(
                includedPlatforms = gameIncludedPlatforms,
                excludedPlatforms = gameExcludedPlatforms,
                onCyclePlatform = { cyclePlatform(it) },
                onClearAll = { clearGamePlatforms() },
                onDismiss = { showPlatformsSubMenu = false }
            )
        } else {
            // ================= MAIN DISCOVERY SHEET =================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (category) {
                                SearchCategory.ANIME -> when (selectedAnimeMode) {
                                    AnimeDiscoveryMode.SEASONAL -> Icons.Default.Explore
                                    AnimeDiscoveryMode.MOST_POPULAR -> Icons.Default.LocalFireDepartment
                                    null -> Icons.Default.Explore
                                }
                                SearchCategory.MOVIES -> when (selectedMovieMode) {
                                    TmdbDiscoveryMode.BY_YEAR -> Icons.Default.Movie
                                    TmdbDiscoveryMode.MOST_POPULAR -> Icons.Default.LocalFireDepartment
                                    null -> Icons.Default.Movie
                                }
                                SearchCategory.TV_SHOWS -> when (selectedTvMode) {
                                    TmdbDiscoveryMode.BY_YEAR -> Icons.Default.Tv
                                    TmdbDiscoveryMode.MOST_POPULAR -> Icons.Default.LocalFireDepartment
                                    null -> Icons.Default.Tv
                                }
                                SearchCategory.GAMES -> Icons.Default.SportsEsports
                                SearchCategory.MUSIC -> Icons.Default.MusicNote
                                SearchCategory.MANGA, SearchCategory.MANHWA, SearchCategory.LIGHT_NOVELS, SearchCategory.BOOKS -> Icons.Default.Explore
                            },
                            contentDescription = null,
                            tint = LXPrimaryGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (category) {
                                SearchCategory.ANIME -> when (selectedAnimeMode) {
                                    AnimeDiscoveryMode.SEASONAL -> "Seasonal Anime"
                                    AnimeDiscoveryMode.MOST_POPULAR -> "Most Popular Anime"
                                    null -> "Discover Anime"
                                }
                                SearchCategory.MOVIES -> when (selectedMovieMode) {
                                    TmdbDiscoveryMode.BY_YEAR -> "Discover Movies"
                                    TmdbDiscoveryMode.MOST_POPULAR -> "Most Popular Movies"
                                    null -> "Discover Movies"
                                }
                                SearchCategory.TV_SHOWS -> when (selectedTvMode) {
                                    TmdbDiscoveryMode.BY_YEAR -> "Discover TV Shows"
                                    TmdbDiscoveryMode.MOST_POPULAR -> "Most Popular TV Shows"
                                    null -> "Discover TV Shows"
                                }
                                SearchCategory.GAMES -> "Discover Games"
                                SearchCategory.MANGA -> "Discover Manga"
                                SearchCategory.MANHWA -> "Discover Manhwa"
                                SearchCategory.LIGHT_NOVELS -> "Discover Light Novels"
                                SearchCategory.BOOKS -> "Discover Books"
                                SearchCategory.MUSIC -> "Discover Music"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }

                    if (activeFilter != null) {
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    onResetFilter()
                                    onDismiss()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (category) {
                    SearchCategory.ANIME -> {
                        // 1. Mode Selector: Small Icon Pills (Seasonal / Most Popular)
                        Text(
                            text = "Discovery Mode",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnimeDiscoveryMode.entries.forEach { mode ->
                                val isSelected = selectedAnimeMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSelected) LXPrimaryGreen.copy(alpha = 0.2f) else LXSurfaceElevated)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) LXPrimaryGreen else LXBorder,
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .clickable { selectedAnimeMode = mode },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    ) {
                                        Text(mode.iconEmoji, fontSize = 13.sp)
                                        Text(
                                            text = mode.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Load rest of the controls ONLY once a Mode is clicked!
                        if (selectedAnimeMode == null) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LXSurfaceElevated)
                                    .padding(vertical = 18.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Select a mode above to customize discovery",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LXTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Seasonal Controls (only when Seasonal mode is active)
                            if (selectedAnimeMode == AnimeDiscoveryMode.SEASONAL) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val (nextSeason, nextYear) = AnimeSeason.nextSeason()
                                    OutlinedButton(
                                        onClick = {
                                            selectedSeason = AnimeSeason.current()
                                            selectedAnimeYear = currentYear
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                    ) {
                                        Text("Current Season", fontSize = 11.sp, maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            selectedSeason = nextSeason
                                            selectedAnimeYear = nextYear
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                    ) {
                                        Text("Upcoming", fontSize = 11.sp, maxLines = 1)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Season Selector (2x2 Grid)
                                Text(
                                    text = "Season",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LXTextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SeasonSelectorBox(
                                        season = AnimeSeason.WINTER,
                                        isSelected = selectedSeason == AnimeSeason.WINTER,
                                        modifier = Modifier.weight(1f)
                                    ) { selectedSeason = AnimeSeason.WINTER }

                                    SeasonSelectorBox(
                                        season = AnimeSeason.SPRING,
                                        isSelected = selectedSeason == AnimeSeason.SPRING,
                                        modifier = Modifier.weight(1f)
                                    ) { selectedSeason = AnimeSeason.SPRING }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SeasonSelectorBox(
                                        season = AnimeSeason.SUMMER,
                                        isSelected = selectedSeason == AnimeSeason.SUMMER,
                                        modifier = Modifier.weight(1f)
                                    ) { selectedSeason = AnimeSeason.SUMMER }

                                    SeasonSelectorBox(
                                        season = AnimeSeason.FALL,
                                        isSelected = selectedSeason == AnimeSeason.FALL,
                                        modifier = Modifier.weight(1f)
                                    ) { selectedSeason = AnimeSeason.FALL }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Year Selector
                                Text(
                                    text = "Year: $selectedAnimeYear",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LXTextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val quickAnimeYears = listOf(maxYear, maxYear - 1, maxYear - 2, maxYear - 3)
                                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        quickAnimeYears.forEach { yr ->
                                            val isSelected = selectedAnimeYear == yr
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { selectedAnimeYear = yr },
                                                label = { Text(yr.toString(), fontSize = 11.sp) },
                                                modifier = Modifier.height(30.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                                    selectedLabelColor = LXPrimaryGreen,
                                                    containerColor = LXSurfaceElevated,
                                                    labelColor = LXTextSecondary
                                                )
                                            )
                                        }

                                        // Dropdown for more years
                                        Box {
                                            FilterChip(
                                                selected = selectedAnimeYear !in quickAnimeYears,
                                                onClick = { yearDropdownExpanded = true },
                                                modifier = Modifier.height(30.dp),
                                                label = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            if (selectedAnimeYear !in quickAnimeYears) selectedAnimeYear.toString() else "More",
                                                            fontSize = 11.sp
                                                        )
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    }
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                                    selectedLabelColor = LXPrimaryGreen,
                                                    containerColor = LXSurfaceElevated,
                                                    labelColor = LXTextSecondary
                                                )
                                            )

                                            DropdownMenu(
                                                expanded = yearDropdownExpanded,
                                                onDismissRequest = { yearDropdownExpanded = false },
                                                modifier = Modifier.background(LXSurfaceElevated)
                                            ) {
                                                (maxYear downTo 1990).forEach { yr ->
                                                    DropdownMenuItem(
                                                        text = { Text(yr.toString(), color = LXTextPrimary) },
                                                        onClick = {
                                                            selectedAnimeYear = yr
                                                            yearDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Sort Order
                            Text(
                                text = "Sort By",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = LXTextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    AnimeSortOrder.entries.forEach { sort ->
                                        val isSelected = selectedAnimeSort == sort
                                        val shortLabel = when (sort) {
                                            AnimeSortOrder.POPULARITY -> "Popular"
                                            AnimeSortOrder.SCORE -> "Top Rated"
                                            AnimeSortOrder.AIR_DATE -> "Release Date"
                                            AnimeSortOrder.TITLE -> "Title (A–Z)"
                                        }
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedAnimeSort = sort },
                                            label = { Text(shortLabel, fontSize = 11.sp) },
                                            modifier = Modifier.height(30.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                                selectedLabelColor = LXPrimaryGreen,
                                                containerColor = LXSurfaceElevated,
                                                labelColor = LXTextSecondary
                                            )
                                        )
                                    }
                                }
                            }

                            // Tags Section Button (Separate Page Navigation)
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LXSurfaceElevated)
                                    .border(1.dp, LXBorder, RoundedCornerShape(10.dp))
                                    .clickable { showTagsSubMenu = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(LXPrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tag,
                                                contentDescription = null,
                                                tint = LXPrimaryGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Select & Exclude Tags",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                                color = LXTextPrimary
                                            )
                                            Text(
                                                text = if (includedTags.isEmpty() && excludedTags.isEmpty())
                                                    "Filter by genres, themes, demographics"
                                                else
                                                    "${includedTags.size} included (✓) • ${excludedTags.size} excluded (✕)",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = if (includedTags.isNotEmpty() || excludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = "Open Tags Filter",
                                        tint = LXPrimaryGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Active Tag Chips inside Main Sheet with 1-tap delete
                                if (includedTags.isNotEmpty() || excludedTags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        includedTags.forEach { tag ->
                                            SearchTagBadge(
                                                label = "+$tag",
                                                isIncluded = true,
                                                onRemove = {
                                                    includedTags = includedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                                }
                                            )
                                        }
                                        excludedTags.forEach { tag ->
                                            SearchTagBadge(
                                                label = "-$tag",
                                                isIncluded = false,
                                                onRemove = {
                                                    excludedTags = excludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Original Language Filter
                            LanguageFilterEntryCard(
                                selectedLanguages = selectedAnimeLanguages,
                                onClick = { showLanguageSubMenu = true }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 18+ Subtle Checkbox Toggle
                            SubtleAdultFilterToggle(
                                adultMode = adultMode,
                                onModeChange = { adultMode = it }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Apply Button
                            Button(
                                onClick = {
                                    val filter = DiscoveryFilter.Anime(
                                        mode = selectedAnimeMode ?: AnimeDiscoveryMode.SEASONAL,
                                        season = selectedSeason,
                                        year = selectedAnimeYear,
                                        sortOrder = selectedAnimeSort,
                                        includedTags = includedTags,
                                        excludedTags = excludedTags,
                                        adultMode = adultMode,
                                        originalLanguages = selectedAnimeLanguages
                                    )
                                    onApplyAnime(filter)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                val modeText = when (selectedAnimeMode ?: AnimeDiscoveryMode.SEASONAL) {
                                    AnimeDiscoveryMode.SEASONAL -> "Explore ${selectedSeason.displayName} $selectedAnimeYear"
                                    AnimeDiscoveryMode.MOST_POPULAR -> "Explore Most Popular Anime"
                                }
                                val tagsText = when {
                                    includedTags.isNotEmpty() || excludedTags.isNotEmpty() -> " (+${includedTags.size}/-${excludedTags.size})"
                                    else -> ""
                                }
                                val adultText = if (adultMode == AdultMode.ONLY) " (18+ Only)" else ""
                                Text(
                                    text = "$modeText$tagsText$adultText",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                SearchCategory.MOVIES -> {
                    // 1. Mode Selector: Small Icon Pills (By Year / Most Popular)
                    Text(
                        text = "Discovery Mode",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TmdbDiscoveryMode.entries.forEach { mode ->
                            val isSelected = selectedMovieMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedMovieMode = mode },
                                color = if (isSelected) LXPrimaryGreen.copy(alpha = 0.2f) else LXSurfaceElevated,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) LXPrimaryGreen else LXBorder
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = mode.iconEmoji,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Only load the rest once a mode is clicked!
                    if (selectedMovieMode != null) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Year Range Selector (only for BY_YEAR)
                        if (selectedMovieMode == TmdbDiscoveryMode.BY_YEAR) {
                            YearRangeSelector(
                                startYear = selectedMovieStartYear,
                                endYear = selectedMovieEndYear,
                                onRangeChange = { s, e ->
                                    selectedMovieStartYear = s
                                    selectedMovieEndYear = e
                                    selectedMovieYear = e ?: (s ?: currentYear)
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Movie Sort Order
                        Text(
                            text = "Sort By",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TmdbMovieSort.entries.forEach { sort ->
                                    val isSelected = selectedMovieSort == sort
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedMovieSort = sort },
                                        label = { Text(sort.displayName, fontSize = 11.sp) },
                                        modifier = Modifier.height(30.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = LXPrimaryGreen,
                                            containerColor = LXSurfaceElevated,
                                            labelColor = LXTextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        // Tags Section Button (Sub-menu Navigation)
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(LXSurfaceElevated)
                                .border(1.dp, LXBorder, RoundedCornerShape(10.dp))
                                .clickable { showTagsSubMenu = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(LXPrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tag,
                                            contentDescription = null,
                                            tint = LXPrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Select & Exclude Tags",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                            color = LXTextPrimary
                                        )
                                        Text(
                                            text = if (movieIncludedTags.isEmpty() && movieExcludedTags.isEmpty())
                                                "Filter by movie genres"
                                            else
                                                "${movieIncludedTags.size} included (✓) • ${movieExcludedTags.size} excluded (✕)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = if (movieIncludedTags.isNotEmpty() || movieExcludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "Open Tags Filter",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Active Tag Chips inside Main Sheet with 1-tap delete
                            if (movieIncludedTags.isNotEmpty() || movieExcludedTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    movieIncludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "+$tag",
                                            isIncluded = true,
                                            onRemove = {
                                                movieIncludedTags = movieIncludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                    movieExcludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "-$tag",
                                            isIncluded = false,
                                            onRemove = {
                                                movieExcludedTags = movieExcludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Original Language Filter
                        LanguageFilterEntryCard(
                            selectedLanguages = selectedMovieLanguages,
                            onClick = { showLanguageSubMenu = true }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 18+ Subtle Checkbox Toggle
                        SubtleAdultFilterToggle(
                            adultMode = adultMode,
                            onModeChange = { adultMode = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val filter = DiscoveryFilter.Movie(
                                    mode = selectedMovieMode ?: TmdbDiscoveryMode.MOST_POPULAR,
                                    year = if (selectedMovieMode == TmdbDiscoveryMode.BY_YEAR) selectedMovieYear else null,
                                    startYear = if (selectedMovieMode == TmdbDiscoveryMode.BY_YEAR) selectedMovieStartYear else null,
                                    endYear = if (selectedMovieMode == TmdbDiscoveryMode.BY_YEAR) selectedMovieEndYear else null,
                                    sortOrder = selectedMovieSort,
                                    includedTags = movieIncludedTags,
                                    excludedTags = movieExcludedTags,
                                    adultMode = adultMode,
                                    originalLanguages = selectedMovieLanguages
                                )
                                onApplyMovie(filter)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            val yearStr = when {
                                selectedMovieStartYear != null && selectedMovieEndYear != null && selectedMovieStartYear == selectedMovieEndYear -> "$selectedMovieStartYear"
                                selectedMovieStartYear != null && selectedMovieEndYear != null -> "$selectedMovieStartYear–$selectedMovieEndYear"
                                selectedMovieStartYear != null -> "From $selectedMovieStartYear"
                                selectedMovieEndYear != null -> "Up to $selectedMovieEndYear"
                                else -> "$selectedMovieYear"
                            }
                            val modeText = when (selectedMovieMode ?: TmdbDiscoveryMode.MOST_POPULAR) {
                                TmdbDiscoveryMode.BY_YEAR -> "Discover Movies $yearStr".trim()
                                TmdbDiscoveryMode.MOST_POPULAR -> "Discover Most Popular Movies"
                            }
                            val tagsText = when {
                                movieIncludedTags.isNotEmpty() || movieExcludedTags.isNotEmpty() -> " (+${movieIncludedTags.size}/-${movieExcludedTags.size})"
                                else -> ""
                            }
                            val adultText = if (adultMode == AdultMode.ONLY) " (18+ Only)" else ""
                            Text(
                                text = "$modeText$tagsText$adultText",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                SearchCategory.TV_SHOWS -> {
                    // 1. Mode Selector: Small Icon Pills (By Year / Most Popular)
                    Text(
                        text = "Discovery Mode",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TmdbDiscoveryMode.entries.forEach { mode ->
                            val isSelected = selectedTvMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedTvMode = mode },
                                color = if (isSelected) LXPrimaryGreen.copy(alpha = 0.2f) else LXSurfaceElevated,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) LXPrimaryGreen else LXBorder
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = mode.iconEmoji,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Only load the rest once a mode is clicked!
                    if (selectedTvMode != null) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // TV Air Year Selector (only for BY_YEAR)
                        if (selectedTvMode == TmdbDiscoveryMode.BY_YEAR) {
                            YearRangeSelector(
                                startYear = selectedTvStartYear,
                                endYear = selectedTvEndYear,
                                onRangeChange = { s, e ->
                                    selectedTvStartYear = s
                                    selectedTvEndYear = e
                                    selectedTvYear = e ?: (s ?: currentYear)
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // TV Sort Order
                        Text(
                            text = "Sort By",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TmdbTvSort.entries.forEach { sort ->
                                    val isSelected = selectedTvSort == sort
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedTvSort = sort },
                                        label = { Text(sort.displayName, fontSize = 11.sp) },
                                        modifier = Modifier.height(30.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = LXPrimaryGreen,
                                            containerColor = LXSurfaceElevated,
                                            labelColor = LXTextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        // Tags Section Button (Sub-menu Navigation)
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(LXSurfaceElevated)
                                .border(1.dp, LXBorder, RoundedCornerShape(10.dp))
                                .clickable { showTagsSubMenu = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(LXPrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tag,
                                            contentDescription = null,
                                            tint = LXPrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Select & Exclude Tags",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                            color = LXTextPrimary
                                        )
                                        Text(
                                            text = if (tvIncludedTags.isEmpty() && tvExcludedTags.isEmpty())
                                                "Filter by TV show genres"
                                            else
                                                "${tvIncludedTags.size} included (✓) • ${tvExcludedTags.size} excluded (✕)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = if (tvIncludedTags.isNotEmpty() || tvExcludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "Open Tags Filter",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Active Tag Chips inside Main Sheet with 1-tap delete
                            if (tvIncludedTags.isNotEmpty() || tvExcludedTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    tvIncludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "+$tag",
                                            isIncluded = true,
                                            onRemove = {
                                                tvIncludedTags = tvIncludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                    tvExcludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "-$tag",
                                            isIncluded = false,
                                            onRemove = {
                                                tvExcludedTags = tvExcludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Original Language Filter
                        LanguageFilterEntryCard(
                            selectedLanguages = selectedTvLanguages,
                            onClick = { showLanguageSubMenu = true }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 18+ Subtle Checkbox Toggle
                        SubtleAdultFilterToggle(
                            adultMode = adultMode,
                            onModeChange = { adultMode = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val filter = DiscoveryFilter.Tv(
                                    mode = selectedTvMode ?: TmdbDiscoveryMode.MOST_POPULAR,
                                    year = if (selectedTvMode == TmdbDiscoveryMode.BY_YEAR) selectedTvYear else null,
                                    startYear = if (selectedTvMode == TmdbDiscoveryMode.BY_YEAR) selectedTvStartYear else null,
                                    endYear = if (selectedTvMode == TmdbDiscoveryMode.BY_YEAR) selectedTvEndYear else null,
                                    sortOrder = selectedTvSort,
                                    includedTags = tvIncludedTags,
                                    excludedTags = tvExcludedTags,
                                    adultMode = adultMode,
                                    originalLanguages = selectedTvLanguages
                                )
                                onApplyTv(filter)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            val yearStr = when {
                                selectedTvStartYear != null && selectedTvEndYear != null && selectedTvStartYear == selectedTvEndYear -> "$selectedTvStartYear"
                                selectedTvStartYear != null && selectedTvEndYear != null -> "$selectedTvStartYear–$selectedTvEndYear"
                                selectedTvStartYear != null -> "From $selectedTvStartYear"
                                selectedTvEndYear != null -> "Up to $selectedTvEndYear"
                                else -> "$selectedTvYear"
                            }
                            val modeText = when (selectedTvMode ?: TmdbDiscoveryMode.MOST_POPULAR) {
                                TmdbDiscoveryMode.BY_YEAR -> "Discover TV Shows $yearStr".trim()
                                TmdbDiscoveryMode.MOST_POPULAR -> "Discover Most Popular TV Shows"
                            }
                            val tagsText = when {
                                tvIncludedTags.isNotEmpty() || tvExcludedTags.isNotEmpty() -> " (+${tvIncludedTags.size}/-${tvExcludedTags.size})"
                                else -> ""
                            }
                            val adultText = if (adultMode == AdultMode.ONLY) " (18+ Only)" else ""
                            Text(
                                text = "$modeText$tagsText$adultText",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                SearchCategory.MANGA -> {
                    Text(
                        text = "Discovery Mode",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ContentDiscoveryMode.entries.forEach { mode ->
                            val isSelected = selectedMangaMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedMangaMode = mode },
                                color = if (isSelected) LXPrimaryGreen.copy(alpha = 0.2f) else LXSurfaceElevated,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) LXPrimaryGreen else LXBorder
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(text = mode.iconEmoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (selectedMangaMode != null) {
                        Spacer(modifier = Modifier.height(10.dp))

                        if (selectedMangaMode == ContentDiscoveryMode.BY_YEAR) {
                            YearRangeSelector(
                                startYear = selectedMangaStartYear,
                                endYear = selectedMangaEndYear,
                                onRangeChange = { s, e ->
                                    selectedMangaStartYear = s
                                    selectedMangaEndYear = e
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Text(
                            text = "Sort By",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MangaSortOrder.entries.forEach { sort ->
                                    val isSelected = selectedMangaSort == sort
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedMangaSort = sort },
                                        label = { Text(sort.displayName, fontSize = 11.sp) },
                                        modifier = Modifier.height(30.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = LXPrimaryGreen,
                                            containerColor = LXSurfaceElevated,
                                            labelColor = LXTextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(LXSurfaceElevated)
                                .border(1.dp, LXBorder, RoundedCornerShape(10.dp))
                                .clickable { showTagsSubMenu = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(LXPrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tag,
                                            contentDescription = null,
                                            tint = LXPrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Select & Exclude Tags",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                            color = LXTextPrimary
                                        )
                                        Text(
                                            text = if (mangaIncludedTags.isEmpty() && mangaExcludedTags.isEmpty())
                                                "Filter by manga genres & themes"
                                            else
                                                "${mangaIncludedTags.size} included (✓) • ${mangaExcludedTags.size} excluded (✕)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = if (mangaIncludedTags.isNotEmpty() || mangaExcludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "Open Tags Filter",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            if (mangaIncludedTags.isNotEmpty() || mangaExcludedTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    mangaIncludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "+$tag",
                                            isIncluded = true,
                                            onRemove = {
                                                mangaIncludedTags = mangaIncludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                    mangaExcludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "-$tag",
                                            isIncluded = false,
                                            onRemove = {
                                                mangaExcludedTags = mangaExcludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Original Language Filter
                        LanguageFilterEntryCard(
                            selectedLanguages = selectedMangaLanguages,
                            onClick = { showLanguageSubMenu = true }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SubtleAdultFilterToggle(
                            adultMode = adultMode,
                            onModeChange = { adultMode = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val filter = DiscoveryFilter.Manga(
                                    mode = selectedMangaMode ?: ContentDiscoveryMode.MOST_POPULAR,
                                    startYear = if (selectedMangaMode == ContentDiscoveryMode.BY_YEAR) selectedMangaStartYear else null,
                                    endYear = if (selectedMangaMode == ContentDiscoveryMode.BY_YEAR) selectedMangaEndYear else null,
                                    sortOrder = selectedMangaSort,
                                    includedTags = mangaIncludedTags,
                                    excludedTags = mangaExcludedTags,
                                    adultMode = adultMode,
                                    originalLanguages = selectedMangaLanguages
                                )
                                onApplyManga(filter)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            val yearStr = when {
                                selectedMangaStartYear != null && selectedMangaEndYear != null && selectedMangaStartYear == selectedMangaEndYear -> "$selectedMangaStartYear"
                                selectedMangaStartYear != null && selectedMangaEndYear != null -> "$selectedMangaStartYear–$selectedMangaEndYear"
                                selectedMangaStartYear != null -> "From $selectedMangaStartYear"
                                selectedMangaEndYear != null -> "Up to $selectedMangaEndYear"
                                else -> ""
                            }
                            val modeText = when (selectedMangaMode ?: ContentDiscoveryMode.MOST_POPULAR) {
                                ContentDiscoveryMode.BY_YEAR -> "Discover Manga $yearStr".trim()
                                ContentDiscoveryMode.MOST_POPULAR -> "Discover Most Popular Manga"
                            }
                            val tagsText = when {
                                mangaIncludedTags.isNotEmpty() || mangaExcludedTags.isNotEmpty() -> " (+${mangaIncludedTags.size}/-${mangaExcludedTags.size})"
                                else -> ""
                            }
                            val adultText = if (adultMode == AdultMode.ONLY) " (18+ Only)" else ""
                            Text(
                                text = "$modeText$tagsText$adultText",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                SearchCategory.MANHWA -> {
                    Text(
                        text = "Discovery Mode",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ContentDiscoveryMode.entries.forEach { mode ->
                            val isSelected = selectedManhwaMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedManhwaMode = mode },
                                color = if (isSelected) LXPrimaryGreen.copy(alpha = 0.2f) else LXSurfaceElevated,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) LXPrimaryGreen else LXBorder
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(text = mode.iconEmoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (selectedManhwaMode != null) {
                        Spacer(modifier = Modifier.height(10.dp))

                        if (selectedManhwaMode == ContentDiscoveryMode.BY_YEAR) {
                            YearRangeSelector(
                                startYear = selectedManhwaStartYear,
                                endYear = selectedManhwaEndYear,
                                onRangeChange = { s, e ->
                                    selectedManhwaStartYear = s
                                    selectedManhwaEndYear = e
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Text(
                            text = "Sort By",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MangaSortOrder.entries.forEach { sort ->
                                    val isSelected = selectedManhwaSort == sort
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedManhwaSort = sort },
                                        label = { Text(sort.displayName, fontSize = 11.sp) },
                                        modifier = Modifier.height(30.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = LXPrimaryGreen,
                                            containerColor = LXSurfaceElevated,
                                            labelColor = LXTextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(LXSurfaceElevated)
                                .border(1.dp, LXBorder, RoundedCornerShape(10.dp))
                                .clickable { showTagsSubMenu = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(LXPrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tag,
                                            contentDescription = null,
                                            tint = LXPrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Select & Exclude Tags",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                            color = LXTextPrimary
                                        )
                                        Text(
                                            text = if (manhwaIncludedTags.isEmpty() && manhwaExcludedTags.isEmpty())
                                                "Filter by manhwa genres & themes"
                                            else
                                                "${manhwaIncludedTags.size} included (✓) • ${manhwaExcludedTags.size} excluded (✕)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = if (manhwaIncludedTags.isNotEmpty() || manhwaExcludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "Open Tags Filter",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            if (manhwaIncludedTags.isNotEmpty() || manhwaExcludedTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    manhwaIncludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "+$tag",
                                            isIncluded = true,
                                            onRemove = {
                                                manhwaIncludedTags = manhwaIncludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                    manhwaExcludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "-$tag",
                                            isIncluded = false,
                                            onRemove = {
                                                manhwaExcludedTags = manhwaExcludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Original Language Filter
                        LanguageFilterEntryCard(
                            selectedLanguages = selectedManhwaLanguages,
                            onClick = { showLanguageSubMenu = true }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SubtleAdultFilterToggle(
                            adultMode = adultMode,
                            onModeChange = { adultMode = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val filter = DiscoveryFilter.Manhwa(
                                    mode = selectedManhwaMode ?: ContentDiscoveryMode.MOST_POPULAR,
                                    startYear = if (selectedManhwaMode == ContentDiscoveryMode.BY_YEAR) selectedManhwaStartYear else null,
                                    endYear = if (selectedManhwaMode == ContentDiscoveryMode.BY_YEAR) selectedManhwaEndYear else null,
                                    sortOrder = selectedManhwaSort,
                                    includedTags = manhwaIncludedTags,
                                    excludedTags = manhwaExcludedTags,
                                    adultMode = adultMode,
                                    originalLanguages = selectedManhwaLanguages
                                )
                                onApplyManhwa(filter)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            val yearStr = when {
                                selectedManhwaStartYear != null && selectedManhwaEndYear != null && selectedManhwaStartYear == selectedManhwaEndYear -> "$selectedManhwaStartYear"
                                selectedManhwaStartYear != null && selectedManhwaEndYear != null -> "$selectedManhwaStartYear–$selectedManhwaEndYear"
                                selectedManhwaStartYear != null -> "From $selectedManhwaStartYear"
                                selectedManhwaEndYear != null -> "Up to $selectedManhwaEndYear"
                                else -> ""
                            }
                            val modeText = when (selectedManhwaMode ?: ContentDiscoveryMode.MOST_POPULAR) {
                                ContentDiscoveryMode.BY_YEAR -> "Discover Manhwa $yearStr".trim()
                                ContentDiscoveryMode.MOST_POPULAR -> "Discover Most Popular Manhwa"
                            }
                            val tagsText = when {
                                manhwaIncludedTags.isNotEmpty() || manhwaExcludedTags.isNotEmpty() -> " (+${manhwaIncludedTags.size}/-${manhwaExcludedTags.size})"
                                else -> ""
                            }
                            val adultText = if (adultMode == AdultMode.ONLY) " (18+ Only)" else ""
                            Text(
                                text = "$modeText$tagsText$adultText",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                SearchCategory.LIGHT_NOVELS -> {
                    Text(
                        text = "Discovery Mode",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ContentDiscoveryMode.entries.forEach { mode ->
                            val isSelected = selectedLightNovelMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedLightNovelMode = mode },
                                color = if (isSelected) LXPrimaryGreen.copy(alpha = 0.2f) else LXSurfaceElevated,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) LXPrimaryGreen else LXBorder
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(text = mode.iconEmoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (selectedLightNovelMode != null) {
                        Spacer(modifier = Modifier.height(10.dp))

                        if (selectedLightNovelMode == ContentDiscoveryMode.BY_YEAR) {
                            YearRangeSelector(
                                startYear = selectedLightNovelStartYear,
                                endYear = selectedLightNovelEndYear,
                                onRangeChange = { s, e ->
                                    selectedLightNovelStartYear = s
                                    selectedLightNovelEndYear = e
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Text(
                            text = "Sort By",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MangaSortOrder.entries.forEach { sort ->
                                    val isSelected = selectedLightNovelSort == sort
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedLightNovelSort = sort },
                                        label = { Text(sort.displayName, fontSize = 11.sp) },
                                        modifier = Modifier.height(30.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = LXPrimaryGreen,
                                            containerColor = LXSurfaceElevated,
                                            labelColor = LXTextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(LXSurfaceElevated)
                                .border(1.dp, LXBorder, RoundedCornerShape(10.dp))
                                .clickable { showTagsSubMenu = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(LXPrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tag,
                                            contentDescription = null,
                                            tint = LXPrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Select & Exclude Tags",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                            color = LXTextPrimary
                                        )
                                        Text(
                                            text = if (lightNovelIncludedTags.isEmpty() && lightNovelExcludedTags.isEmpty())
                                                "Filter by light novel genres & themes"
                                            else
                                                "${lightNovelIncludedTags.size} included (✓) • ${lightNovelExcludedTags.size} excluded (✕)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = if (lightNovelIncludedTags.isNotEmpty() || lightNovelExcludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "Open Tags Filter",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            if (lightNovelIncludedTags.isNotEmpty() || lightNovelExcludedTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    lightNovelIncludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "+$tag",
                                            isIncluded = true,
                                            onRemove = {
                                                lightNovelIncludedTags = lightNovelIncludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                    lightNovelExcludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "-$tag",
                                            isIncluded = false,
                                            onRemove = {
                                                lightNovelExcludedTags = lightNovelExcludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Original Language Filter
                        LanguageFilterEntryCard(
                            selectedLanguages = selectedLightNovelLanguages,
                            onClick = { showLanguageSubMenu = true }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SubtleAdultFilterToggle(
                            adultMode = adultMode,
                            onModeChange = { adultMode = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val filter = DiscoveryFilter.LightNovel(
                                    mode = selectedLightNovelMode ?: ContentDiscoveryMode.MOST_POPULAR,
                                    startYear = if (selectedLightNovelMode == ContentDiscoveryMode.BY_YEAR) selectedLightNovelStartYear else null,
                                    endYear = if (selectedLightNovelMode == ContentDiscoveryMode.BY_YEAR) selectedLightNovelEndYear else null,
                                    sortOrder = selectedLightNovelSort,
                                    includedTags = lightNovelIncludedTags,
                                    excludedTags = lightNovelExcludedTags,
                                    adultMode = adultMode,
                                    originalLanguages = selectedLightNovelLanguages
                                )
                                onApplyLightNovel(filter)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            val yearStr = when {
                                selectedLightNovelStartYear != null && selectedLightNovelEndYear != null && selectedLightNovelStartYear == selectedLightNovelEndYear -> "$selectedLightNovelStartYear"
                                selectedLightNovelStartYear != null && selectedLightNovelEndYear != null -> "$selectedLightNovelStartYear–$selectedLightNovelEndYear"
                                selectedLightNovelStartYear != null -> "From $selectedLightNovelStartYear"
                                selectedLightNovelEndYear != null -> "Up to $selectedLightNovelEndYear"
                                else -> ""
                            }
                            val modeText = when (selectedLightNovelMode ?: ContentDiscoveryMode.MOST_POPULAR) {
                                ContentDiscoveryMode.BY_YEAR -> "Discover Light Novels $yearStr".trim()
                                ContentDiscoveryMode.MOST_POPULAR -> "Discover Most Popular Light Novels"
                            }
                            val tagsText = when {
                                lightNovelIncludedTags.isNotEmpty() || lightNovelExcludedTags.isNotEmpty() -> " (+${lightNovelIncludedTags.size}/-${lightNovelExcludedTags.size})"
                                else -> ""
                            }
                            val adultText = if (adultMode == AdultMode.ONLY) " (18+ Only)" else ""
                            Text(
                                text = "$modeText$tagsText$adultText",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                SearchCategory.BOOKS -> {
                    Text(
                        text = "Discovery Mode",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ContentDiscoveryMode.entries.forEach { mode ->
                            val isSelected = selectedBookMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedBookMode = mode },
                                color = if (isSelected) LXPrimaryGreen.copy(alpha = 0.2f) else LXSurfaceElevated,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) LXPrimaryGreen else LXBorder
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(text = mode.iconEmoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (selectedBookMode != null) {
                        Spacer(modifier = Modifier.height(10.dp))

                        if (selectedBookMode == ContentDiscoveryMode.BY_YEAR) {
                            YearRangeSelector(
                                startYear = selectedBookStartYear,
                                endYear = selectedBookEndYear,
                                onRangeChange = { s, e ->
                                    selectedBookStartYear = s
                                    selectedBookEndYear = e
                                },
                                minYear = 1900
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Text(
                            text = "Sort By",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                BookSortOrder.entries.forEach { sort ->
                                    val isSelected = selectedBookSort == sort
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedBookSort = sort },
                                        label = { Text(sort.displayName, fontSize = 11.sp) },
                                        modifier = Modifier.height(30.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = LXPrimaryGreen,
                                            containerColor = LXSurfaceElevated,
                                            labelColor = LXTextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(LXSurfaceElevated)
                                .border(1.dp, LXBorder, RoundedCornerShape(10.dp))
                                .clickable { showTagsSubMenu = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(LXPrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tag,
                                            contentDescription = null,
                                            tint = LXPrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Select & Exclude Tags",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                            color = LXTextPrimary
                                        )
                                        Text(
                                            text = if (bookIncludedTags.isEmpty() && bookExcludedTags.isEmpty())
                                                "Filter by book genres & subjects"
                                            else
                                                "${bookIncludedTags.size} included (✓) • ${bookExcludedTags.size} excluded (✕)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = if (bookIncludedTags.isNotEmpty() || bookExcludedTags.isNotEmpty()) LXPrimaryGreen else LXTextSecondary
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "Open Tags Filter",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            if (bookIncludedTags.isNotEmpty() || bookExcludedTags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    bookIncludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "+$tag",
                                            isIncluded = true,
                                            onRemove = {
                                                bookIncludedTags = bookIncludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                    bookExcludedTags.forEach { tag ->
                                        SearchTagBadge(
                                            label = "-$tag",
                                            isIncluded = false,
                                            onRemove = {
                                                bookExcludedTags = bookExcludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Original Language Filter
                        LanguageFilterEntryCard(
                            selectedLanguages = selectedBookLanguages,
                            onClick = { showLanguageSubMenu = true }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SubtleAdultFilterToggle(
                            adultMode = adultMode,
                            onModeChange = { adultMode = it }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val filter = DiscoveryFilter.Book(
                                    mode = selectedBookMode ?: ContentDiscoveryMode.MOST_POPULAR,
                                    startYear = if (selectedBookMode == ContentDiscoveryMode.BY_YEAR) selectedBookStartYear else null,
                                    endYear = if (selectedBookMode == ContentDiscoveryMode.BY_YEAR) selectedBookEndYear else null,
                                    sortOrder = selectedBookSort,
                                    includedTags = bookIncludedTags,
                                    excludedTags = bookExcludedTags,
                                    adultMode = adultMode,
                                    originalLanguages = selectedBookLanguages
                                )
                                onApplyBook(filter)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            val yearStr = when {
                                selectedBookStartYear != null && selectedBookEndYear != null && selectedBookStartYear == selectedBookEndYear -> "$selectedBookStartYear"
                                selectedBookStartYear != null && selectedBookEndYear != null -> "$selectedBookStartYear–$selectedBookEndYear"
                                selectedBookStartYear != null -> "From $selectedBookStartYear"
                                selectedBookEndYear != null -> "Up to $selectedBookEndYear"
                                else -> ""
                            }
                            val modeText = when (selectedBookMode ?: ContentDiscoveryMode.MOST_POPULAR) {
                                ContentDiscoveryMode.BY_YEAR -> "Discover Books $yearStr".trim()
                                ContentDiscoveryMode.MOST_POPULAR -> "Discover Most Popular Books"
                            }
                            val tagsText = when {
                                bookIncludedTags.isNotEmpty() || bookExcludedTags.isNotEmpty() -> " (+${bookIncludedTags.size}/-${bookExcludedTags.size})"
                                else -> ""
                            }
                            val adultText = if (adultMode == AdultMode.ONLY) " (18+ Only)" else ""
                            Text(
                                text = "$modeText$tagsText$adultText",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                SearchCategory.GAMES -> {
                    GameDiscoveryContent(
                        currentYear = currentYear,
                        selectedGameMode = selectedGameMode,
                        onSelectGameMode = { selectedGameMode = it },
                        selectedGameStartYear = selectedGameStartYear,
                        selectedGameEndYear = selectedGameEndYear,
                        onRangeChange = { s, e ->
                            selectedGameStartYear = s
                            selectedGameEndYear = e
                            selectedGameYear = e ?: (s ?: currentYear)
                        },
                        selectedGameSort = selectedGameSort,
                        onSelectGameSort = { selectedGameSort = it },
                        gameIncludedTags = gameIncludedTags,
                        gameExcludedTags = gameExcludedTags,
                        onOpenTags = { showTagsSubMenu = true },
                        onRemoveIncludedTag = { tag ->
                            gameIncludedTags = gameIncludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                        },
                        onRemoveExcludedTag = { tag ->
                            gameExcludedTags = gameExcludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
                        },
                        gameIncludedPlatforms = gameIncludedPlatforms,
                        gameExcludedPlatforms = gameExcludedPlatforms,
                        onOpenPlatforms = { showPlatformsSubMenu = true },
                        onRemoveIncludedPlatform = { platform ->
                            gameIncludedPlatforms = gameIncludedPlatforms.filter { !it.equals(platform, ignoreCase = true) }.toSet()
                        },
                        onRemoveExcludedPlatform = { platform ->
                            gameExcludedPlatforms = gameExcludedPlatforms.filter { !it.equals(platform, ignoreCase = true) }.toSet()
                        },
                        selectedLanguages = selectedGameLanguages,
                        onOpenLanguages = { showLanguageSubMenu = true },
                        adultMode = adultMode,
                        onAdultModeChange = { adultMode = it },
                        onApply = {
                            val filter = DiscoveryFilter.Game(
                                mode = selectedGameMode ?: ContentDiscoveryMode.MOST_POPULAR,
                                year = if (selectedGameMode == ContentDiscoveryMode.BY_YEAR) (selectedGameEndYear ?: selectedGameStartYear ?: currentYear) else null,
                                startYear = if (selectedGameMode == ContentDiscoveryMode.BY_YEAR) selectedGameStartYear else null,
                                endYear = if (selectedGameMode == ContentDiscoveryMode.BY_YEAR) selectedGameEndYear else null,
                                sortOrder = selectedGameSort,
                                includedTags = gameIncludedTags,
                                excludedTags = gameExcludedTags,
                                adultMode = adultMode,
                                originalLanguages = selectedGameLanguages,
                                includedPlatforms = gameIncludedPlatforms,
                                excludedPlatforms = gameExcludedPlatforms
                            )
                            onApplyGame(filter)
                            onDismiss()
                        }
                    )
                }

                SearchCategory.MUSIC -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Music discovery and charts coming soon! Use the search bar above to look up songs, albums, and artists.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LXTextSecondary
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
internal fun SearchTagBadge(
    label: String,
    isIncluded: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isIncluded) LXPrimaryGreen.copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
    val borderColor = if (isIncluded) LXPrimaryGreen.copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f)
    val contentColor = if (isIncluded) LXPrimaryGreen else Color(0xFFEF4444)

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = contentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SeasonSelectorBox(
    season: AnimeSeason,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) LXPrimaryGreen else LXBorder.copy(alpha = 0.5f)
    val bgColor = if (isSelected) LXPrimaryGreen.copy(alpha = 0.18f) else LXSurfaceElevated

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier.height(42.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = season.emoji,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = season.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                )
                Text(
                    text = season.monthsText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (isSelected) LXPrimaryGreen.copy(alpha = 0.8f) else LXTextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubtleAdultFilterToggle(
    adultMode: AdultMode,
    onModeChange: (AdultMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onModeChange(adultMode.next()) }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Checkbox(
                checked = adultMode != AdultMode.OFF,
                onCheckedChange = { onModeChange(adultMode.next()) },
                colors = CheckboxDefaults.colors(
                    checkedColor = if (adultMode == AdultMode.ONLY) Color(0xFFEF4444) else LXPrimaryGreen,
                    uncheckedColor = LXTextSecondary.copy(alpha = 0.6f),
                    checkmarkColor = Color.Black
                ),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = when (adultMode) {
                AdultMode.OFF -> "18+"
                AdultMode.INCLUDE -> "18+"
                AdultMode.ONLY -> "18+ Only"
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (adultMode != AdultMode.OFF) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp
            ),
            color = when (adultMode) {
                AdultMode.OFF -> LXTextSecondary
                AdultMode.INCLUDE -> LXPrimaryGreen
                AdultMode.ONLY -> Color(0xFFEF4444)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun YearRangeSelector(
    startYear: Int?,
    endYear: Int?,
    onRangeChange: (Int?, Int?) -> Unit,
    minYear: Int = 1970,
    maxYear: Int = AnimeSeason.maxDiscoveryYear(),
    modifier: Modifier = Modifier
) {
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    val currentYr = AnimeSeason.currentYear()

    val labelText = when {
        startYear == null && endYear == null -> "Year: All"
        startYear != null && endYear != null && startYear == endYear -> "Year: $startYear"
        startYear != null && endYear != null -> "Year Range: $startYear – $endYear"
        startYear != null -> "Year: $startYear onwards"
        endYear != null -> "Year: Up to $endYear"
        else -> "Year Range"
    }

    Text(
        text = labelText,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = LXTextSecondary
    )
    Spacer(modifier = Modifier.height(4.dp))

    // Quick presets: Next year on left, Current year at center, Previous year on right
    val presets = listOf(
        "${currentYr + 1}" to (currentYr + 1 to currentYr + 1),
        "$currentYr" to (currentYr to currentYr),
        "${currentYr - 1}" to (currentYr - 1 to currentYr - 1)
    )

    val isPresetSelected = presets.any { it.second.first == startYear && it.second.second == endYear }
    val isCustomRange = !isPresetSelected && (startYear != null || endYear != null)

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Next (left), Current (center), Previous (right) years
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { (presetLabel, range) ->
                    val isSelected = startYear == range.first && endYear == range.second
                    FilterChip(
                        selected = isSelected,
                        onClick = { onRangeChange(range.first, range.second) },
                        label = {
                            Text(
                                text = presetLabel,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = LXPrimaryGreen,
                            containerColor = LXSurfaceElevated,
                            labelColor = LXTextSecondary
                        )
                    )
                }
            }

            // Row 2: Custom "From" and "To" years
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Custom "From: ..." dropdown chip
                Box(modifier = Modifier.weight(1f)) {
                    FilterChip(
                        selected = isCustomRange,
                        onClick = { fromExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "From: ${startYear ?: minYear}",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = LXPrimaryGreen,
                            containerColor = LXSurfaceElevated,
                            labelColor = LXTextPrimary
                        )
                    )

                    DropdownMenu(
                        expanded = fromExpanded,
                        onDismissRequest = { fromExpanded = false },
                        modifier = Modifier.background(LXSurfaceElevated)
                    ) {
                        (maxYear downTo minYear).forEach { yr ->
                            DropdownMenuItem(
                                text = { Text(yr.toString(), color = LXTextPrimary) },
                                onClick = {
                                    val currentEnd = endYear ?: yr
                                    val newEnd = if (currentEnd < yr) yr else currentEnd
                                    onRangeChange(yr, newEnd)
                                    fromExpanded = false
                                }
                            )
                        }
                    }
                }

                // Custom "To: ..." dropdown chip
                Box(modifier = Modifier.weight(1f)) {
                    FilterChip(
                        selected = isCustomRange,
                        onClick = { toExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "To: ${endYear ?: maxYear}",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = LXPrimaryGreen,
                            containerColor = LXSurfaceElevated,
                            labelColor = LXTextPrimary
                        )
                    )

                    DropdownMenu(
                        expanded = toExpanded,
                        onDismissRequest = { toExpanded = false },
                        modifier = Modifier.background(LXSurfaceElevated)
                    ) {
                        (maxYear downTo minYear).forEach { yr ->
                            DropdownMenuItem(
                                text = { Text(yr.toString(), color = LXTextPrimary) },
                                onClick = {
                                    val currentStart = startYear ?: yr
                                    val newStart = if (currentStart > yr) yr else currentStart
                                    onRangeChange(newStart, yr)
                                    toExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TriStateTagCheckbox(
    state: TagSelectionState,
    modifier: Modifier = Modifier
) {
    val boxSize = 20.dp
    val shape = RoundedCornerShape(4.dp)

    when (state) {
        TagSelectionState.UNSELECTED -> {
            Box(
                modifier = modifier
                    .size(boxSize)
                    .border(BorderStroke(1.5.dp, LXTextSecondary.copy(alpha = 0.6f)), shape)
                    .clip(shape)
            )
        }
        TagSelectionState.INCLUDED -> {
            Box(
                modifier = modifier
                    .size(boxSize)
                    .background(LXPrimaryGreen, shape)
                    .clip(shape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Included",
                    tint = LXBackgroundBlack,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        TagSelectionState.EXCLUDED -> {
            Box(
                modifier = modifier
                    .size(boxSize)
                    .background(Color(0xFFEF4444), shape)
                    .clip(shape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Excluded",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
internal fun LanguageFilterEntryCard(
    selectedLanguages: Set<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSelection = selectedLanguages.isNotEmpty()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LXSurfaceElevated)
            .border(
                1.dp,
                if (hasSelection) LXPrimaryGreen.copy(alpha = 0.5f) else LXBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(LXPrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val firstFlag = selectedLanguages.firstOrNull()?.let { OriginalLanguageRegistry.findByCode(it)?.flagEmoji }
                    if (firstFlag != null) {
                        Text(text = firstFlag, fontSize = 16.sp)
                    } else {
                        Text(text = "🌐", fontSize = 15.sp)
                    }
                }
                Column {
                    Text(
                        text = "Original Language",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        color = LXTextPrimary
                    )
                    val subtitle = when {
                        selectedLanguages.isEmpty() -> "Filter by original language"
                        selectedLanguages.size == 1 -> OriginalLanguageRegistry.getDisplayWithFlag(selectedLanguages.first()) ?: selectedLanguages.first()
                        selectedLanguages.size in 2..3 -> selectedLanguages.mapNotNull { OriginalLanguageRegistry.getDisplayWithFlag(it) ?: it }.joinToString(", ")
                        else -> "${selectedLanguages.take(2).mapNotNull { OriginalLanguageRegistry.getDisplayWithFlag(it) ?: it }.joinToString(", ")} +${selectedLanguages.size - 2}"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (hasSelection) LXPrimaryGreen else LXTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (hasSelection) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = LXPrimaryGreen.copy(alpha = 0.15f)
                    ) {
                        val badgeText = if (selectedLanguages.size == 1) {
                            OriginalLanguageRegistry.findByCode(selectedLanguages.first())?.displayName ?: selectedLanguages.first()
                        } else {
                            "${selectedLanguages.size} (OR)"
                        }
                        Text(
                            text = badgeText,
                            color = LXPrimaryGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Open Language Filter",
                    tint = if (hasSelection) LXPrimaryGreen else LXTextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

