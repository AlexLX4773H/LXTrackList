package com.lx.tracklist.ui.search

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.SubCategory
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.remote.BookDetails
import com.lx.tracklist.data.remote.BookMetadataManager
import com.lx.tracklist.data.remote.JikanApiClient
import com.lx.tracklist.data.remote.JikanMediaDetails
import com.lx.tracklist.data.remote.MangaDexApiClient
import com.lx.tracklist.data.remote.TmdbApiClient
import com.lx.tracklist.data.remote.TmdbSearchResult
import com.lx.tracklist.data.repository.MediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

enum class SearchCategory(val displayName: String, val sourceName: String) {
    ANIME("Anime", "MAL"),
    MANGA("Manga", "MAL / MangaDex"),
    MANHWA("Manhwa", "MangaDex / AniList"),
    MOVIES("Movies", "TMDB"),
    TV_SHOWS("TV Shows", "TMDB"),
    BOOKS("Books", "Google Books / Open Library"),
    LIGHT_NOVELS("Light Novels", "AniList / MAL")
}

data class SearchResultItem(
    val externalId: String,
    val externalSource: String, // "MAL", "MangaDex", "AniList", "TMDB", "Google Books", "Open Library"
    val mediaType: MediaType,
    val title: String,
    val alternateTitle: String? = null,
    val posterUrl: String? = null,
    val releaseYear: String? = null,
    val totalUnits: Int? = null,
    val totalVolumes: Int? = null,
    val totalSeasons: Int? = null,
    val durationMinutes: Int? = null,
    val score: Float? = null,
    val statusText: String? = null,
    val synopsis: String? = null,
    val isAlreadyInLibrary: Boolean = false,
    val existingEntryId: Long? = null,
    val existingStatus: MediaStatus? = null,
    val author: String? = null,
    val publisher: String? = null,
    val isbn: String? = null,
    val rawBookDetails: BookDetails? = null,
    val rawJikanDetails: JikanMediaDetails? = null,
    val rawTmdbResult: TmdbSearchResult? = null
)

data class SearchUiState(
    val query: String = "",
    val selectedCategory: SearchCategory = SearchCategory.ANIME,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val currentPage: Int = 1,
    val errorMessage: String? = null,
    val results: List<SearchResultItem> = emptyList(),
    val customCategories: List<CustomCategory> = emptyList(),
    val subCategories: List<SubCategory> = emptyList(),
    val userAddedMessage: String? = null,
    val activeDiscoveryFilter: DiscoveryFilter? = null
)

class SearchViewModel(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    private var searchJob: Job? = null
    private var activeSearchKey: String? = null

    val uiState: StateFlow<SearchUiState> = combine(
        _uiState,
        repository.getAllEntries(),
        repository.customCategories,
        repository.allSubCategories
    ) { state: SearchUiState, allEntries: List<MediaEntry>, customCats: List<CustomCategory>, subCats: List<SubCategory> ->
        val updatedResults = state.results.map { item ->
            val match = allEntries.firstOrNull { entry ->
                entry.matchesExternal(
                    candidateExternalId = item.externalId,
                    candidateExternalSource = item.externalSource,
                    candidateTitle = item.title,
                    candidateMediaType = item.mediaType,
                    candidateReleaseYear = item.releaseYear,
                    candidateAlternateTitle = item.alternateTitle
                )
            }
            if (match != null) {
                item.copy(
                    isAlreadyInLibrary = true,
                    existingEntryId = match.id,
                    existingStatus = match.status
                )
            } else {
                item.copy(
                    isAlreadyInLibrary = false,
                    existingEntryId = null,
                    existingStatus = null
                )
            }
        }
        state.copy(
            results = updatedResults,
            customCategories = customCats,
            subCategories = subCats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    fun onQueryChanged(newQuery: String, context: Context? = null) {
        val oldQuery = _uiState.value.query
        val activeFilter = _uiState.value.activeDiscoveryFilter
        _uiState.update { it.copy(query = newQuery) }
        val trimmed = newQuery.trim()
        if (trimmed.length < 2) {
            if (activeFilter != null) {
                // If a discovery filter was active and user cleared their query, reload the seasonal/year list
                if (oldQuery.trim().length >= 2) {
                    reloadDiscoveryFilter(activeFilter, context)
                }
            } else {
                searchJob?.cancel()
                activeSearchKey = null
                _uiState.update {
                    it.copy(
                        results = emptyList(),
                        isLoading = false,
                        canLoadMore = false,
                        errorMessage = null
                    )
                }
            }
            return
        }
        if (trimmed == oldQuery.trim() && searchJob?.isActive == true) {
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            executeSearch(trimmed, _uiState.value.selectedCategory, context)
        }
    }

    fun onCategorySelected(category: SearchCategory, context: Context? = null) {
        if (_uiState.value.selectedCategory == category) return
        searchJob?.cancel()
        activeSearchKey = null
        _uiState.update {
            it.copy(
                selectedCategory = category,
                activeDiscoveryFilter = null,
                results = emptyList(),
                currentPage = 1,
                canLoadMore = false,
                errorMessage = null
            )
        }
        val currentQuery = _uiState.value.query.trim()
        if (currentQuery.length >= 2) {
            searchJob = viewModelScope.launch {
                executeSearch(currentQuery, category, context)
            }
        }
    }

    private fun reloadDiscoveryFilter(filter: DiscoveryFilter, context: Context? = null) {
        when (filter) {
            is DiscoveryFilter.Anime -> applyAnimeDiscoveryFilter(filter)
            is DiscoveryFilter.Movie -> applyMovieDiscoveryFilter(filter, context)
            is DiscoveryFilter.Tv -> applyTvDiscoveryFilter(filter, context)
            is DiscoveryFilter.Manga -> applyMangaDiscoveryFilter(filter, context)
            is DiscoveryFilter.Manhwa -> applyManhwaDiscoveryFilter(filter, context)
            is DiscoveryFilter.LightNovel -> applyLightNovelDiscoveryFilter(filter, context)
            is DiscoveryFilter.Book -> applyBookDiscoveryFilter(filter, context)
        }
    }

    fun applyAnimeDiscoveryFilter(filter: DiscoveryFilter.Anime) {
        searchJob?.cancel()
        activeSearchKey = "DISCOVER:ANIME:${filter.mode}:${filter.season}:${filter.year}:${filter.sortOrder}:${filter.includedTags}:${filter.excludedTags}:${filter.adultMode}"
        _uiState.update {
            it.copy(
                selectedCategory = SearchCategory.ANIME,
                activeDiscoveryFilter = filter,
                query = "",
                isLoading = true,
                currentPage = 1,
                canLoadMore = false,
                errorMessage = null,
                results = emptyList()
            )
        }
        searchJob = viewModelScope.launch {
            try {
                val detailsList = withContext(Dispatchers.IO) {
                    JikanApiClient.fetchAnimeDiscovery(
                        mode = filter.mode,
                        year = if (filter.mode == AnimeDiscoveryMode.SEASONAL) filter.year else null,
                        season = if (filter.mode == AnimeDiscoveryMode.SEASONAL) filter.season.apiValue else null,
                        sort = filter.sortOrder.apiValue,
                        search = null,
                        includedTags = filter.includedTags,
                        excludedTags = filter.excludedTags,
                        adultMode = filter.adultMode,
                        page = 1
                    ).getOrThrow()
                }
                val mapped = detailsList.mapIndexed { idx, details ->
                    mapJikanToSearchResult(details, idx)
                }
                _uiState.update {
                    it.copy(
                        results = mapped,
                        isLoading = false,
                        currentPage = 1,
                        canLoadMore = mapped.size >= 20
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error discovering anime", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to discover anime") }
            }
        }
    }

    fun applyAnimeSeasonalFilter(
        season: AnimeSeason,
        year: Int,
        sort: AnimeSortOrder,
        adultMode: AdultMode = AdultMode.OFF
    ) {
        applyAnimeDiscoveryFilter(
            DiscoveryFilter.Anime(
                mode = AnimeDiscoveryMode.SEASONAL,
                season = season,
                year = year,
                sortOrder = sort,
                adultMode = adultMode
            )
        )
    }

    fun removeAnimeTagFilter(tag: String) {
        val currentFilter = _uiState.value.activeDiscoveryFilter as? DiscoveryFilter.Anime ?: return
        val newIncluded = currentFilter.includedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
        val newExcluded = currentFilter.excludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
        applyAnimeDiscoveryFilter(
            currentFilter.copy(
                includedTags = newIncluded,
                excludedTags = newExcluded
            )
        )
    }

    fun clearAnimeTagFilters() {
        val currentFilter = _uiState.value.activeDiscoveryFilter as? DiscoveryFilter.Anime ?: return
        applyAnimeDiscoveryFilter(
            currentFilter.copy(
                includedTags = emptySet(),
                excludedTags = emptySet()
            )
        )
    }

    fun removeMovieTagFilter(tag: String, context: Context? = null) {
        val currentFilter = _uiState.value.activeDiscoveryFilter as? DiscoveryFilter.Movie ?: return
        val newIncluded = currentFilter.includedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
        val newExcluded = currentFilter.excludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
        applyMovieDiscoveryFilter(
            currentFilter.copy(
                includedTags = newIncluded,
                excludedTags = newExcluded
            ),
            context = context
        )
    }

    fun removeTvTagFilter(tag: String, context: Context? = null) {
        val currentFilter = _uiState.value.activeDiscoveryFilter as? DiscoveryFilter.Tv ?: return
        val newIncluded = currentFilter.includedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
        val newExcluded = currentFilter.excludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
        applyTvDiscoveryFilter(
            currentFilter.copy(
                includedTags = newIncluded,
                excludedTags = newExcluded
            ),
            context = context
        )
    }

    fun removeMangaTagFilter(tag: String, context: Context? = null) {
        val currentFilter = _uiState.value.activeDiscoveryFilter as? DiscoveryFilter.Manga ?: return
        val newIncluded = currentFilter.includedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
        val newExcluded = currentFilter.excludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
        applyMangaDiscoveryFilter(
            currentFilter.copy(includedTags = newIncluded, excludedTags = newExcluded),
            context = context
        )
    }

    fun removeManhwaTagFilter(tag: String, context: Context? = null) {
        val currentFilter = _uiState.value.activeDiscoveryFilter as? DiscoveryFilter.Manhwa ?: return
        val newIncluded = currentFilter.includedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
        val newExcluded = currentFilter.excludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
        applyManhwaDiscoveryFilter(
            currentFilter.copy(includedTags = newIncluded, excludedTags = newExcluded),
            context = context
        )
    }

    fun removeLightNovelTagFilter(tag: String, context: Context? = null) {
        val currentFilter = _uiState.value.activeDiscoveryFilter as? DiscoveryFilter.LightNovel ?: return
        val newIncluded = currentFilter.includedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
        val newExcluded = currentFilter.excludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
        applyLightNovelDiscoveryFilter(
            currentFilter.copy(includedTags = newIncluded, excludedTags = newExcluded),
            context = context
        )
    }

    fun removeBookTagFilter(tag: String, context: Context? = null) {
        val currentFilter = _uiState.value.activeDiscoveryFilter as? DiscoveryFilter.Book ?: return
        val newIncluded = currentFilter.includedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
        val newExcluded = currentFilter.excludedTags.filter { !it.equals(tag, ignoreCase = true) }.toSet()
        applyBookDiscoveryFilter(
            currentFilter.copy(includedTags = newIncluded, excludedTags = newExcluded),
            context = context
        )
    }

    fun removeTagFilter(tag: String, context: Context? = null) {
        when (_uiState.value.activeDiscoveryFilter) {
            is DiscoveryFilter.Anime -> removeAnimeTagFilter(tag)
            is DiscoveryFilter.Movie -> removeMovieTagFilter(tag, context)
            is DiscoveryFilter.Tv -> removeTvTagFilter(tag, context)
            is DiscoveryFilter.Manga -> removeMangaTagFilter(tag, context)
            is DiscoveryFilter.Manhwa -> removeManhwaTagFilter(tag, context)
            is DiscoveryFilter.LightNovel -> removeLightNovelTagFilter(tag, context)
            is DiscoveryFilter.Book -> removeBookTagFilter(tag, context)
            null -> {}
        }
    }

    fun applyMangaDiscoveryFilter(filter: DiscoveryFilter.Manga, context: Context? = null) {
        searchJob?.cancel()
        activeSearchKey = "DISCOVER:MANGA:${filter.mode}:${filter.startYear}:${filter.endYear}:${filter.sortOrder}:${filter.adultMode}:${filter.includedTags}:${filter.excludedTags}"
        _uiState.update {
            it.copy(
                selectedCategory = SearchCategory.MANGA,
                activeDiscoveryFilter = filter,
                query = "",
                isLoading = true,
                currentPage = 1,
                canLoadMore = false,
                errorMessage = null,
                results = emptyList()
            )
        }
        searchJob = viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    JikanApiClient.fetchMangaDiscovery(
                        isManhwa = false,
                        isLightNovel = false,
                        mode = filter.mode,
                        startYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.startYear else null,
                        endYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.endYear else null,
                        sort = filter.sortOrder.apiValue,
                        includedTags = filter.includedTags,
                        excludedTags = filter.excludedTags,
                        adultMode = filter.adultMode,
                        page = 1
                    ).getOrThrow()
                }
                val mapped = list.mapIndexed { idx, d -> mapJikanToSearchResult(d, idx, MediaType.MANGA) }
                _uiState.update {
                    it.copy(
                        results = mapped,
                        isLoading = false,
                        currentPage = 1,
                        canLoadMore = mapped.size >= 15
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error discovering manga", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to discover manga") }
            }
        }
    }

    fun applyManhwaDiscoveryFilter(filter: DiscoveryFilter.Manhwa, context: Context? = null) {
        searchJob?.cancel()
        activeSearchKey = "DISCOVER:MANHWA:${filter.mode}:${filter.startYear}:${filter.endYear}:${filter.sortOrder}:${filter.adultMode}:${filter.includedTags}:${filter.excludedTags}"
        _uiState.update {
            it.copy(
                selectedCategory = SearchCategory.MANHWA,
                activeDiscoveryFilter = filter,
                query = "",
                isLoading = true,
                currentPage = 1,
                canLoadMore = false,
                errorMessage = null,
                results = emptyList()
            )
        }
        searchJob = viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    JikanApiClient.fetchMangaDiscovery(
                        isManhwa = true,
                        isLightNovel = false,
                        mode = filter.mode,
                        startYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.startYear else null,
                        endYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.endYear else null,
                        sort = filter.sortOrder.apiValue,
                        includedTags = filter.includedTags,
                        excludedTags = filter.excludedTags,
                        adultMode = filter.adultMode,
                        page = 1
                    ).getOrThrow()
                }
                val mapped = list.mapIndexed { idx, d -> mapJikanToSearchResult(d, idx, MediaType.MANHWA) }
                _uiState.update {
                    it.copy(
                        results = mapped,
                        isLoading = false,
                        currentPage = 1,
                        canLoadMore = mapped.size >= 15
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error discovering manhwa", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to discover manhwa") }
            }
        }
    }

    fun applyLightNovelDiscoveryFilter(filter: DiscoveryFilter.LightNovel, context: Context? = null) {
        searchJob?.cancel()
        activeSearchKey = "DISCOVER:LN:${filter.mode}:${filter.startYear}:${filter.endYear}:${filter.sortOrder}:${filter.adultMode}:${filter.includedTags}:${filter.excludedTags}"
        _uiState.update {
            it.copy(
                selectedCategory = SearchCategory.LIGHT_NOVELS,
                activeDiscoveryFilter = filter,
                query = "",
                isLoading = true,
                currentPage = 1,
                canLoadMore = false,
                errorMessage = null,
                results = emptyList()
            )
        }
        searchJob = viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    JikanApiClient.fetchMangaDiscovery(
                        isManhwa = false,
                        isLightNovel = true,
                        mode = filter.mode,
                        startYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.startYear else null,
                        endYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.endYear else null,
                        sort = filter.sortOrder.apiValue,
                        includedTags = filter.includedTags,
                        excludedTags = filter.excludedTags,
                        adultMode = filter.adultMode,
                        page = 1
                    ).getOrThrow()
                }
                val mapped = list.mapIndexed { idx, d -> mapJikanToSearchResult(d, idx, MediaType.BOOK) }
                _uiState.update {
                    it.copy(
                        results = mapped,
                        isLoading = false,
                        currentPage = 1,
                        canLoadMore = mapped.size >= 15
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error discovering light novels", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to discover light novels") }
            }
        }
    }

    fun applyBookDiscoveryFilter(filter: DiscoveryFilter.Book, context: Context? = null) {
        searchJob?.cancel()
        activeSearchKey = "DISCOVER:BOOK:${filter.mode}:${filter.startYear}:${filter.endYear}:${filter.sortOrder}:${filter.adultMode}:${filter.includedTags}:${filter.excludedTags}"
        _uiState.update {
            it.copy(
                selectedCategory = SearchCategory.BOOKS,
                activeDiscoveryFilter = filter,
                query = "",
                isLoading = true,
                currentPage = 1,
                canLoadMore = false,
                errorMessage = null,
                results = emptyList()
            )
        }
        searchJob = viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    BookMetadataManager.discoverBooks(
                        mode = filter.mode,
                        startYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.startYear else null,
                        endYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.endYear else null,
                        sort = filter.sortOrder,
                        includedTags = filter.includedTags,
                        excludedTags = filter.excludedTags,
                        adultMode = filter.adultMode,
                        page = 1,
                        context = context
                    ).getOrThrow()
                }
                val mapped = list.mapIndexed { idx, b -> mapBookToSearchResult(b, idx) }
                _uiState.update {
                    it.copy(
                        results = mapped,
                        isLoading = false,
                        currentPage = 1,
                        canLoadMore = mapped.size >= 15
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error discovering books", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to discover books") }
            }
        }
    }

    fun applyMovieDiscoveryFilter(
        filter: DiscoveryFilter.Movie,
        context: Context? = null
    ) {
        searchJob?.cancel()
        val yearPart = filter.year?.toString() ?: "ALL"
        activeSearchKey = "DISCOVER:MOVIE:${filter.mode}:$yearPart:${filter.sortOrder}:${filter.adultMode}:${filter.includedTags.sorted()}:${filter.excludedTags.sorted()}"
        _uiState.update {
            it.copy(
                selectedCategory = SearchCategory.MOVIES,
                activeDiscoveryFilter = filter,
                query = "",
                isLoading = true,
                currentPage = 1,
                canLoadMore = false,
                errorMessage = null,
                results = emptyList()
            )
        }
        searchJob = viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    TmdbApiClient.discoverMovies(
                        year = if (filter.mode == TmdbDiscoveryMode.BY_YEAR) filter.year else null,
                        startYear = if (filter.mode == TmdbDiscoveryMode.BY_YEAR) filter.startYear else null,
                        endYear = if (filter.mode == TmdbDiscoveryMode.BY_YEAR) filter.endYear else null,
                        sortBy = filter.sortOrder.apiValue,
                        page = 1,
                        adultMode = filter.adultMode,
                        includedTags = filter.includedTags,
                        excludedTags = filter.excludedTags,
                        context = context
                    ).getOrThrow()
                }
                val mapped = list.map { tmdb ->
                    mapTmdbToSearchResult(tmdb, isMovie = true)
                }
                _uiState.update {
                    it.copy(
                        results = mapped,
                        isLoading = false,
                        currentPage = 1,
                        canLoadMore = mapped.size >= 20
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error discovering movies", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to discover movies") }
            }
        }
    }

    fun applyMovieDiscoveryFilter(
        year: Int,
        sort: TmdbMovieSort,
        adultMode: AdultMode = AdultMode.OFF,
        context: Context? = null
    ) {
        applyMovieDiscoveryFilter(
            DiscoveryFilter.Movie(
                mode = TmdbDiscoveryMode.BY_YEAR,
                year = year,
                sortOrder = sort,
                adultMode = adultMode
            ),
            context = context
        )
    }

    fun applyTvDiscoveryFilter(
        filter: DiscoveryFilter.Tv,
        context: Context? = null
    ) {
        searchJob?.cancel()
        val yearPart = filter.year?.toString() ?: "ALL"
        activeSearchKey = "DISCOVER:TV:${filter.mode}:$yearPart:${filter.sortOrder}:${filter.adultMode}:${filter.includedTags.sorted()}:${filter.excludedTags.sorted()}"
        _uiState.update {
            it.copy(
                selectedCategory = SearchCategory.TV_SHOWS,
                activeDiscoveryFilter = filter,
                query = "",
                isLoading = true,
                currentPage = 1,
                canLoadMore = false,
                errorMessage = null,
                results = emptyList()
            )
        }
        searchJob = viewModelScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    TmdbApiClient.discoverTvShows(
                        year = if (filter.mode == TmdbDiscoveryMode.BY_YEAR) filter.year else null,
                        startYear = if (filter.mode == TmdbDiscoveryMode.BY_YEAR) filter.startYear else null,
                        endYear = if (filter.mode == TmdbDiscoveryMode.BY_YEAR) filter.endYear else null,
                        sortBy = filter.sortOrder.apiValue,
                        page = 1,
                        adultMode = filter.adultMode,
                        includedTags = filter.includedTags,
                        excludedTags = filter.excludedTags,
                        context = context
                    ).getOrThrow()
                }
                val mapped = list.map { tmdb ->
                    mapTmdbToSearchResult(tmdb, isMovie = false)
                }
                _uiState.update {
                    it.copy(
                        results = mapped,
                        isLoading = false,
                        currentPage = 1,
                        canLoadMore = mapped.size >= 20
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error discovering TV shows", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to discover TV shows") }
            }
        }
    }

    fun applyTvDiscoveryFilter(
        year: Int,
        sort: TmdbTvSort,
        adultMode: AdultMode = AdultMode.OFF,
        context: Context? = null
    ) {
        applyTvDiscoveryFilter(
            DiscoveryFilter.Tv(
                mode = TmdbDiscoveryMode.BY_YEAR,
                year = year,
                sortOrder = sort,
                adultMode = adultMode
            ),
            context = context
        )
    }

    fun loadMore(context: Context? = null) {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isLoadingMore || !currentState.canLoadMore) return

        val nextPage = currentState.currentPage + 1
        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            try {
                val newItems: List<SearchResultItem> = withContext(Dispatchers.IO) {
                    val filter = currentState.activeDiscoveryFilter
                    val query = currentState.query.trim()
                    when {
                        filter is DiscoveryFilter.Anime -> {
                            val detailsList = JikanApiClient.fetchAnimeDiscovery(
                                mode = filter.mode,
                                year = if (filter.mode == AnimeDiscoveryMode.SEASONAL) filter.year else null,
                                season = if (filter.mode == AnimeDiscoveryMode.SEASONAL) filter.season.apiValue else null,
                                sort = filter.sortOrder.apiValue,
                                search = if (query.length >= 2) query else null,
                                includedTags = filter.includedTags,
                                excludedTags = filter.excludedTags,
                                adultMode = filter.adultMode,
                                page = nextPage
                            ).getOrThrow()
                            detailsList.mapIndexed { idx, details ->
                                mapJikanToSearchResult(details, currentState.results.size + idx)
                            }
                        }
                        filter is DiscoveryFilter.Movie -> {
                            val list = if (query.length >= 2) {
                                TmdbApiClient.searchMoviesMultiple(
                                    title = query,
                                    year = if (filter.mode == TmdbDiscoveryMode.BY_YEAR) filter.year else null,
                                    adultMode = filter.adultMode,
                                    page = nextPage,
                                    context = context
                                ).getOrThrow()
                            } else {
                                TmdbApiClient.discoverMovies(
                                    year = if (filter.mode == TmdbDiscoveryMode.BY_YEAR) filter.year else null,
                                    startYear = if (filter.mode == TmdbDiscoveryMode.BY_YEAR) filter.startYear else null,
                                    endYear = if (filter.mode == TmdbDiscoveryMode.BY_YEAR) filter.endYear else null,
                                    sortBy = filter.sortOrder.apiValue,
                                    page = nextPage,
                                    adultMode = filter.adultMode,
                                    includedTags = filter.includedTags,
                                    excludedTags = filter.excludedTags,
                                    context = context
                                ).getOrThrow()
                            }
                            list.map { mapTmdbToSearchResult(it, isMovie = true) }
                        }
                        filter is DiscoveryFilter.Tv -> {
                            val list = if (query.length >= 2) {
                                TmdbApiClient.searchTvMultiple(
                                    title = query,
                                    year = if (filter.mode == TmdbDiscoveryMode.BY_YEAR) filter.year else null,
                                    adultMode = filter.adultMode,
                                    page = nextPage,
                                    context = context
                                ).getOrThrow()
                            } else {
                                TmdbApiClient.discoverTvShows(
                                    year = if (filter.mode == TmdbDiscoveryMode.BY_YEAR) filter.year else null,
                                    startYear = if (filter.mode == TmdbDiscoveryMode.BY_YEAR) filter.startYear else null,
                                    endYear = if (filter.mode == TmdbDiscoveryMode.BY_YEAR) filter.endYear else null,
                                    sortBy = filter.sortOrder.apiValue,
                                    page = nextPage,
                                    adultMode = filter.adultMode,
                                    includedTags = filter.includedTags,
                                    excludedTags = filter.excludedTags,
                                    context = context
                                ).getOrThrow()
                            }
                            list.map { mapTmdbToSearchResult(it, isMovie = false) }
                        }
                        filter is DiscoveryFilter.Manga -> {
                            val list = JikanApiClient.fetchMangaDiscovery(
                                isManhwa = false,
                                isLightNovel = false,
                                mode = filter.mode,
                                startYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.startYear else null,
                                endYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.endYear else null,
                                sort = filter.sortOrder.apiValue,
                                search = if (query.length >= 2) query else null,
                                includedTags = filter.includedTags,
                                excludedTags = filter.excludedTags,
                                adultMode = filter.adultMode,
                                page = nextPage
                            ).getOrThrow()
                            list.mapIndexed { idx, d -> mapJikanToSearchResult(d, currentState.results.size + idx, MediaType.MANGA) }
                        }
                        filter is DiscoveryFilter.Manhwa -> {
                            val list = JikanApiClient.fetchMangaDiscovery(
                                isManhwa = true,
                                isLightNovel = false,
                                mode = filter.mode,
                                startYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.startYear else null,
                                endYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.endYear else null,
                                sort = filter.sortOrder.apiValue,
                                search = if (query.length >= 2) query else null,
                                includedTags = filter.includedTags,
                                excludedTags = filter.excludedTags,
                                adultMode = filter.adultMode,
                                page = nextPage
                            ).getOrThrow()
                            list.mapIndexed { idx, d -> mapJikanToSearchResult(d, currentState.results.size + idx, MediaType.MANHWA) }
                        }
                        filter is DiscoveryFilter.LightNovel -> {
                            val list = JikanApiClient.fetchMangaDiscovery(
                                isManhwa = false,
                                isLightNovel = true,
                                mode = filter.mode,
                                startYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.startYear else null,
                                endYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.endYear else null,
                                sort = filter.sortOrder.apiValue,
                                search = if (query.length >= 2) query else null,
                                includedTags = filter.includedTags,
                                excludedTags = filter.excludedTags,
                                adultMode = filter.adultMode,
                                page = nextPage
                            ).getOrThrow()
                            list.mapIndexed { idx, d -> mapJikanToSearchResult(d, currentState.results.size + idx, MediaType.BOOK) }
                        }
                        filter is DiscoveryFilter.Book -> {
                            val list = BookMetadataManager.discoverBooks(
                                mode = filter.mode,
                                startYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.startYear else null,
                                endYear = if (filter.mode == ContentDiscoveryMode.BY_YEAR) filter.endYear else null,
                                sort = filter.sortOrder,
                                search = if (query.length >= 2) query else null,
                                includedTags = filter.includedTags,
                                excludedTags = filter.excludedTags,
                                adultMode = filter.adultMode,
                                page = nextPage,
                                context = context
                            ).getOrThrow()
                            list.mapIndexed { idx, b -> mapBookToSearchResult(b, currentState.results.size + idx) }
                        }
                        currentState.selectedCategory == SearchCategory.MOVIES && query.length >= 2 -> {
                            val list = TmdbApiClient.searchMoviesMultiple(
                                title = query,
                                page = nextPage,
                                context = context
                            ).getOrThrow()
                            list.map { mapTmdbToSearchResult(it, isMovie = true) }
                        }
                        currentState.selectedCategory == SearchCategory.TV_SHOWS && query.length >= 2 -> {
                            val list = TmdbApiClient.searchTvMultiple(
                                title = query,
                                page = nextPage,
                                context = context
                            ).getOrThrow()
                            list.map { mapTmdbToSearchResult(it, isMovie = false) }
                        }
                        else -> emptyList()
                    }
                }

                _uiState.update { state ->
                    val combinedResults = (state.results + newItems).distinctBy { "${it.externalSource}_${it.externalId}" }
                    state.copy(
                        results = combinedResults,
                        currentPage = nextPage,
                        canLoadMore = newItems.isNotEmpty() && newItems.size >= 15,
                        isLoadingMore = false
                    )
                }
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error loading more results", e)
                _uiState.update { it.copy(isLoadingMore = false, canLoadMore = false) }
            }
        }
    }

    fun clearDiscoveryFilter() {
        searchJob?.cancel()
        activeSearchKey = null
        _uiState.update {
            it.copy(
                activeDiscoveryFilter = null,
                results = emptyList(),
                currentPage = 1,
                canLoadMore = false,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    private fun mapJikanToSearchResult(
        details: JikanMediaDetails,
        idx: Int,
        mediaType: MediaType = MediaType.ANIME
    ): SearchResultItem {
        val extSource = when (details.sourceProvider) {
            "MangaDex" -> "MangaDex"
            "Kitsu" -> "Kitsu"
            "AniList" -> "AniList"
            else -> if (details.sourceProvider.startsWith("AniList")) "AniList" else "MAL"
        }
        val extId = if (details.sourceProvider == "MangaDex") {
            details.url?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                ?: "md_${details.title?.hashCode() ?: idx}_$idx"
        } else {
            details.malId?.takeIf { it > 0 }?.toString() ?: "ani_${details.title?.hashCode() ?: idx}_$idx"
        }
        val status = when (mediaType) {
            MediaType.ANIME -> if (details.isAiring == true) "Airing" else "Finished"
            MediaType.MANGA, MediaType.MANHWA -> if (details.isAiring == true) "Publishing" else "Finished"
            MediaType.BOOK -> "Light Novel"
            else -> if (details.isAiring == true) "Ongoing" else "Finished"
        }
        return SearchResultItem(
            externalId = extId,
            externalSource = extSource,
            mediaType = mediaType,
            title = details.title ?: "Unknown",
            alternateTitle = details.alternateTitle,
            posterUrl = details.posterUrl,
            releaseYear = details.airedStartDate?.let { extractYear(it) },
            totalUnits = if (mediaType == MediaType.ANIME) details.totalEpisodes else details.totalChapters,
            totalVolumes = details.totalVolumes,
            durationMinutes = details.durationMinutes,
            score = details.externalScore,
            statusText = status,
            synopsis = details.synopsis,
            author = details.authors,
            rawJikanDetails = details
        )
    }

    private fun mapBookToSearchResult(book: BookDetails, idx: Int): SearchResultItem {
        val jikanDetails = book.toJikanMediaDetails()
        val extId = if (book.id.isNotBlank()) book.id else "book_${book.title.hashCode()}_$idx"
        val synopsisText = book.description?.takeIf { it.isNotBlank() } ?: book.buildDescriptionWithMetadata()
        return SearchResultItem(
            externalId = extId,
            externalSource = book.sourceProvider,
            mediaType = MediaType.BOOK,
            title = book.title,
            alternateTitle = book.subtitle,
            posterUrl = book.coverUrl,
            releaseYear = book.publishedDate?.take(4),
            totalUnits = book.pageCount,
            score = book.averageScore,
            statusText = "Book",
            synopsis = synopsisText,
            author = book.authorString.takeIf { it.isNotBlank() },
            publisher = book.publisher,
            isbn = book.primaryIsbn,
            rawBookDetails = book,
            rawJikanDetails = jikanDetails
        )
    }

    private fun mapTmdbToSearchResult(tmdb: TmdbSearchResult, isMovie: Boolean): SearchResultItem {
        return SearchResultItem(
            externalId = tmdb.tmdbId.toString(),
            externalSource = "TMDB",
            mediaType = if (isMovie) MediaType.MOVIE else MediaType.SHOW,
            title = tmdb.title,
            alternateTitle = tmdb.originalTitle,
            posterUrl = tmdb.posterUrl,
            releaseYear = tmdb.releaseYear,
            score = tmdb.voteAverage,
            statusText = if (isMovie) "Movie" else "TV Show",
            synopsis = tmdb.overview,
            rawTmdbResult = tmdb
        )
    }

    fun submitSearch(context: Context? = null) {
        val currentQuery = _uiState.value.query.trim()
        val currentCategory = _uiState.value.selectedCategory
        if (currentQuery.length >= 2) {
            val searchKey = "$currentCategory:$currentQuery"
            // If the exact same search is already actively executing, let it finish instead of cancelling & restarting
            if (searchJob?.isActive == true && activeSearchKey == searchKey && _uiState.value.isLoading) {
                return
            }
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                executeSearch(currentQuery, currentCategory, context)
            }
        }
    }

    private suspend fun executeSearch(
        query: String,
        category: SearchCategory,
        context: Context?
    ) {
        val searchKey = "$category:$query"
        activeSearchKey = searchKey
        _uiState.update { it.copy(isLoading = true, errorMessage = null, currentPage = 1, canLoadMore = false) }

        val activeFilter = _uiState.value.activeDiscoveryFilter

        try {
            val results = withContext(Dispatchers.IO) {
                // If a discovery filter is active, perform scoped search on that specific season or year!
                if (activeFilter != null) {
                    when (activeFilter) {
                        is DiscoveryFilter.Anime -> {
                            val list = JikanApiClient.fetchSeasonalAnime(
                                year = activeFilter.year,
                                season = activeFilter.season.apiValue,
                                sort = activeFilter.sortOrder.apiValue,
                                search = query,
                                adultMode = activeFilter.adultMode,
                                page = 1
                            ).getOrThrow()
                            return@withContext list.mapIndexed { idx, d -> mapJikanToSearchResult(d, idx) }
                        }
                        is DiscoveryFilter.Movie -> {
                            val list = TmdbApiClient.searchMoviesMultiple(
                                title = query,
                                year = if (activeFilter.mode == TmdbDiscoveryMode.BY_YEAR) activeFilter.year else null,
                                adultMode = activeFilter.adultMode,
                                page = 1,
                                context = context
                            ).getOrThrow()
                            return@withContext list.map { mapTmdbToSearchResult(it, isMovie = true) }
                        }
                        is DiscoveryFilter.Tv -> {
                            val list = TmdbApiClient.searchTvMultiple(
                                title = query,
                                year = if (activeFilter.mode == TmdbDiscoveryMode.BY_YEAR) activeFilter.year else null,
                                adultMode = activeFilter.adultMode,
                                page = 1,
                                context = context
                            ).getOrThrow()
                            return@withContext list.map { mapTmdbToSearchResult(it, isMovie = false) }
                        }
                        is DiscoveryFilter.Manga -> {
                            val list = JikanApiClient.fetchMangaDiscovery(
                                isManhwa = false,
                                isLightNovel = false,
                                mode = activeFilter.mode,
                                startYear = if (activeFilter.mode == ContentDiscoveryMode.BY_YEAR) activeFilter.startYear else null,
                                endYear = if (activeFilter.mode == ContentDiscoveryMode.BY_YEAR) activeFilter.endYear else null,
                                sort = activeFilter.sortOrder.apiValue,
                                search = query,
                                includedTags = activeFilter.includedTags,
                                excludedTags = activeFilter.excludedTags,
                                adultMode = activeFilter.adultMode,
                                page = 1
                            ).getOrThrow()
                            return@withContext list.mapIndexed { idx, d -> mapJikanToSearchResult(d, idx, MediaType.MANGA) }
                        }
                        is DiscoveryFilter.Manhwa -> {
                            val list = JikanApiClient.fetchMangaDiscovery(
                                isManhwa = true,
                                isLightNovel = false,
                                mode = activeFilter.mode,
                                startYear = if (activeFilter.mode == ContentDiscoveryMode.BY_YEAR) activeFilter.startYear else null,
                                endYear = if (activeFilter.mode == ContentDiscoveryMode.BY_YEAR) activeFilter.endYear else null,
                                sort = activeFilter.sortOrder.apiValue,
                                search = query,
                                includedTags = activeFilter.includedTags,
                                excludedTags = activeFilter.excludedTags,
                                adultMode = activeFilter.adultMode,
                                page = 1
                            ).getOrThrow()
                            return@withContext list.mapIndexed { idx, d -> mapJikanToSearchResult(d, idx, MediaType.MANHWA) }
                        }
                        is DiscoveryFilter.LightNovel -> {
                            val list = JikanApiClient.fetchMangaDiscovery(
                                isManhwa = false,
                                isLightNovel = true,
                                mode = activeFilter.mode,
                                startYear = if (activeFilter.mode == ContentDiscoveryMode.BY_YEAR) activeFilter.startYear else null,
                                endYear = if (activeFilter.mode == ContentDiscoveryMode.BY_YEAR) activeFilter.endYear else null,
                                sort = activeFilter.sortOrder.apiValue,
                                search = query,
                                includedTags = activeFilter.includedTags,
                                excludedTags = activeFilter.excludedTags,
                                adultMode = activeFilter.adultMode,
                                page = 1
                            ).getOrThrow()
                            return@withContext list.mapIndexed { idx, d -> mapJikanToSearchResult(d, idx, MediaType.BOOK) }
                        }
                        is DiscoveryFilter.Book -> {
                            val list = BookMetadataManager.discoverBooks(
                                mode = activeFilter.mode,
                                startYear = if (activeFilter.mode == ContentDiscoveryMode.BY_YEAR) activeFilter.startYear else null,
                                endYear = if (activeFilter.mode == ContentDiscoveryMode.BY_YEAR) activeFilter.endYear else null,
                                sort = activeFilter.sortOrder,
                                search = query,
                                includedTags = activeFilter.includedTags,
                                excludedTags = activeFilter.excludedTags,
                                adultMode = activeFilter.adultMode,
                                page = 1,
                                context = context
                            ).getOrThrow()
                            return@withContext list.mapIndexed { idx, b -> mapBookToSearchResult(b, idx) }
                        }
                    }
                }

                when (category) {
                    SearchCategory.ANIME -> {
                        supervisorScope {
                            val jikanDeferred = async {
                                try {
                                    JikanApiClient.searchJikanDirect(query, isManga = false, limit = 20)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Result.failure(e)
                                }
                            }
                            val anilistDeferred = async {
                                try {
                                    JikanApiClient.queryAniListMultiple(query, isManga = false, limit = 20)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Result.failure(e)
                                }
                            }

                            // Wait up to 4.5s for Jikan; if Jikan hangs/slows down, fall back to AniList immediately
                            val jikanRes = try {
                                withTimeoutOrNull(4500L) {
                                    jikanDeferred.await()
                                } ?: run {
                                    Log.w("SearchViewModel", "Jikan anime search for '$query' exceeded 4.5s; falling back to AniList")
                                    jikanDeferred.cancel()
                                    Result.failure(Exception("Jikan search timed out"))
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Result.failure(e)
                            }

                            val anilistRes = try {
                                anilistDeferred.await()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Result.failure(e)
                            }

                            val jikanList = jikanRes.getOrDefault(emptyList())
                            val anilistList = anilistRes.getOrDefault(emptyList())

                            val seenKeys = mutableSetOf<String>()
                            val combined = mutableListOf<JikanMediaDetails>()

                            for (item in jikanList) {
                                val key = item.title?.trim()?.lowercase(Locale.ROOT) ?: continue
                                if (seenKeys.add(key)) combined.add(item)
                            }
                            for (item in anilistList) {
                                val key = item.title?.trim()?.lowercase(Locale.ROOT) ?: continue
                                if (seenKeys.add(key)) combined.add(item)
                            }

                            if (combined.isEmpty() && jikanRes.isFailure && anilistRes.isFailure) {
                                throw jikanRes.exceptionOrNull() ?: anilistRes.exceptionOrNull() ?: Exception("Failed to fetch anime")
                            }

                            combined.mapIndexed { idx, details ->
                                val extId = details.malId?.takeIf { it > 0 }?.toString() ?: "ani_${details.title?.hashCode() ?: idx}_$idx"
                                val extSource = if (details.sourceProvider == "AniList" && (details.malId == null || details.malId == 0L)) "AniList" else "MAL"
                                SearchResultItem(
                                    externalId = extId,
                                    externalSource = extSource,
                                    mediaType = MediaType.ANIME,
                                    title = details.title ?: "Unknown",
                                    alternateTitle = details.alternateTitle,
                                    posterUrl = details.posterUrl,
                                    releaseYear = details.airedStartDate?.let { extractYear(it) },
                                    totalUnits = details.totalEpisodes,
                                    durationMinutes = details.durationMinutes,
                                    score = details.externalScore,
                                    statusText = if (details.isAiring == true) "Airing" else "Finished",
                                    synopsis = details.synopsis,
                                    rawJikanDetails = details
                                )
                            }
                        }
                    }
                    SearchCategory.MANGA -> {
                        supervisorScope {
                            val mangaDexDeferred = async {
                                try {
                                    MangaDexApiClient.searchMangaMultiple(query, isManhwa = false, limit = 20)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Result.failure(e)
                                }
                            }
                            val jikanDeferred = async {
                                try {
                                    JikanApiClient.searchJikanDirect(query, isManga = true, limit = 20)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Result.failure(e)
                                }
                            }
                            val anilistDeferred = async {
                                try {
                                    JikanApiClient.queryAniListMultiple(query, isManga = true, limit = 20)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Result.failure(e)
                                }
                            }

                            val mangaDexRes = try {
                                mangaDexDeferred.await()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Result.failure(e)
                            }

                            val jikanRes = try {
                                withTimeoutOrNull(4500L) {
                                    jikanDeferred.await()
                                } ?: run {
                                    jikanDeferred.cancel()
                                    Result.failure(Exception("Jikan search timed out"))
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Result.failure(e)
                            }

                            val anilistRes = try {
                                anilistDeferred.await()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Result.failure(e)
                            }

                            val mangaDexList = mangaDexRes.getOrDefault(emptyList())
                            val jikanList = jikanRes.getOrDefault(emptyList())
                            val anilistList = anilistRes.getOrDefault(emptyList())

                            val seenKeys = mutableSetOf<String>()
                            val combined = mutableListOf<JikanMediaDetails>()

                            // Add MAL results first if available
                            for (item in jikanList) {
                                val key = item.title?.trim()?.lowercase(Locale.ROOT) ?: continue
                                if (seenKeys.add(key)) combined.add(item)
                            }
                            // Then add MangaDex results
                            for (item in mangaDexList) {
                                val key = item.title?.trim()?.lowercase(Locale.ROOT) ?: continue
                                if (seenKeys.add(key)) combined.add(item)
                            }
                            // Then add AniList results if list is short
                            if (combined.size < 10) {
                                for (item in anilistList) {
                                    val key = item.title?.trim()?.lowercase(Locale.ROOT) ?: continue
                                    if (seenKeys.add(key)) combined.add(item)
                                }
                            }

                            if (combined.isEmpty() && jikanRes.isFailure && mangaDexRes.isFailure) {
                                throw jikanRes.exceptionOrNull() ?: mangaDexRes.exceptionOrNull() ?: Exception("Failed to fetch manga")
                            }

                            combined.mapIndexed { idx, details ->
                                val extSource = when (details.sourceProvider) {
                                    "MangaDex" -> "MangaDex"
                                    "Kitsu" -> "Kitsu"
                                    "AniList" -> "AniList"
                                    else -> if (details.sourceProvider.startsWith("AniList")) "AniList" else "MAL"
                                }
                                val extId = if (details.sourceProvider == "MangaDex") {
                                    details.url?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                                        ?: "md_${details.title?.hashCode() ?: idx}_$idx"
                                } else {
                                    details.malId?.takeIf { it > 0 }?.toString() ?: "ani_${details.title?.hashCode() ?: idx}_$idx"
                                }
                                SearchResultItem(
                                    externalId = extId,
                                    externalSource = extSource,
                                    mediaType = MediaType.MANGA,
                                    title = details.title ?: "Unknown",
                                    alternateTitle = details.alternateTitle,
                                    posterUrl = details.posterUrl,
                                    releaseYear = details.airedStartDate?.let { extractYear(it) },
                                    totalUnits = details.totalChapters,
                                    totalVolumes = details.totalVolumes,
                                    score = details.externalScore,
                                    statusText = if (details.isAiring == true) "Publishing" else "Finished",
                                    synopsis = details.synopsis,
                                    author = details.authors,
                                    rawJikanDetails = details
                                )
                            }
                        }
                    }
                    SearchCategory.MANHWA -> {
                        supervisorScope {
                            val mangaDexDeferred = async {
                                try {
                                    MangaDexApiClient.searchMangaMultiple(query, isManhwa = true, limit = 20)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Result.failure(e)
                                }
                            }
                            val anilistDeferred = async {
                                try {
                                    JikanApiClient.queryAniListMultiple(query, isManga = true, subType = "manhwa", limit = 20)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Result.failure(e)
                                }
                            }

                            val mangaDexRes = try {
                                mangaDexDeferred.await()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Result.failure(e)
                            }

                            val anilistRes = try {
                                anilistDeferred.await()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Result.failure(e)
                            }

                            val mangaDexList = mangaDexRes.getOrDefault(emptyList())
                            val anilistList = anilistRes.getOrDefault(emptyList())

                            val seenKeys = mutableSetOf<String>()
                            val combined = mutableListOf<JikanMediaDetails>()

                            for (item in mangaDexList) {
                                val key = item.title?.trim()?.lowercase(Locale.ROOT) ?: continue
                                if (seenKeys.add(key)) combined.add(item)
                            }
                            for (item in anilistList) {
                                val key = item.title?.trim()?.lowercase(Locale.ROOT) ?: continue
                                if (seenKeys.add(key)) combined.add(item)
                            }

                            if (combined.isEmpty()) {
                                val jikanRes = try {
                                    withTimeoutOrNull(4500L) {
                                        JikanApiClient.searchJikanDirect(query, isManga = true, subType = "manhwa", limit = 20)
                                    } ?: Result.failure(Exception("Jikan search timed out"))
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Result.failure(e)
                                }
                                combined.addAll(jikanRes.getOrDefault(emptyList()))
                            }

                            if (combined.isEmpty() && mangaDexRes.isFailure && anilistRes.isFailure) {
                                throw mangaDexRes.exceptionOrNull() ?: anilistRes.exceptionOrNull() ?: Exception("Failed to fetch manhwa")
                            }

                            combined.mapIndexed { idx, details ->
                                val extSource = when (details.sourceProvider) {
                                    "MangaDex" -> "MangaDex"
                                    "Kitsu" -> "Kitsu"
                                    "AniList" -> "AniList"
                                    else -> if (details.sourceProvider.startsWith("AniList")) "AniList" else "MAL"
                                }
                                val extId = if (details.sourceProvider == "MangaDex") {
                                    details.url?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                                        ?: "md_${details.title?.hashCode() ?: idx}_$idx"
                                } else {
                                    details.malId?.takeIf { it > 0 }?.toString() ?: "ani_${details.title?.hashCode() ?: idx}_$idx"
                                }
                                SearchResultItem(
                                    externalId = extId,
                                    externalSource = extSource,
                                    mediaType = MediaType.MANHWA,
                                    title = details.title ?: "Unknown",
                                    alternateTitle = details.alternateTitle,
                                    posterUrl = details.posterUrl,
                                    releaseYear = details.airedStartDate?.let { extractYear(it) },
                                    totalUnits = details.totalChapters,
                                    totalVolumes = details.totalVolumes,
                                    score = details.externalScore,
                                    statusText = if (details.isAiring == true) "Publishing" else "Finished",
                                    synopsis = details.synopsis,
                                    author = details.authors,
                                    rawJikanDetails = details
                                )
                            }
                        }
                    }
                    SearchCategory.MOVIES -> {
                        val res = TmdbApiClient.searchMoviesMultiple(query, context)
                        if (res.isFailure) throw res.exceptionOrNull() ?: Exception("Failed to search TMDB movies")
                        res.getOrDefault(emptyList()).map { tmdb ->
                            SearchResultItem(
                                externalId = tmdb.tmdbId.toString(),
                                externalSource = "TMDB",
                                mediaType = MediaType.MOVIE,
                                title = tmdb.title,
                                alternateTitle = tmdb.originalTitle,
                                posterUrl = tmdb.posterUrl,
                                releaseYear = tmdb.releaseYear,
                                score = tmdb.voteAverage,
                                statusText = "Movie",
                                synopsis = tmdb.overview,
                                rawTmdbResult = tmdb
                            )
                        }
                    }
                    SearchCategory.TV_SHOWS -> {
                        val res = TmdbApiClient.searchTvMultiple(query, context)
                        if (res.isFailure) throw res.exceptionOrNull() ?: Exception("Failed to search TMDB TV")
                        res.getOrDefault(emptyList()).map { tmdb ->
                            SearchResultItem(
                                externalId = tmdb.tmdbId.toString(),
                                externalSource = "TMDB",
                                mediaType = MediaType.SHOW,
                                title = tmdb.title,
                                alternateTitle = tmdb.originalTitle,
                                posterUrl = tmdb.posterUrl,
                                releaseYear = tmdb.releaseYear,
                                score = tmdb.voteAverage,
                                statusText = "TV Show",
                                synopsis = tmdb.overview,
                                rawTmdbResult = tmdb
                            )
                        }
                    }
                    SearchCategory.BOOKS -> {
                        val res = BookMetadataManager.searchBooks(query, context, limit = 20)
                        if (res.isFailure) throw res.exceptionOrNull() ?: Exception("Failed to fetch books")
                        res.getOrDefault(emptyList()).mapIndexed { idx, book ->
                            val jikanDetails = book.toJikanMediaDetails()
                            val extId = if (book.id.isNotBlank()) book.id else "book_${book.title.hashCode()}_$idx"
                            val synopsisText = book.description?.takeIf { it.isNotBlank() } ?: book.buildDescriptionWithMetadata()
                            SearchResultItem(
                                externalId = extId,
                                externalSource = book.sourceProvider,
                                mediaType = MediaType.BOOK,
                                title = book.title,
                                alternateTitle = book.subtitle,
                                posterUrl = book.coverUrl,
                                releaseYear = book.publishedDate?.take(4),
                                totalUnits = book.pageCount,
                                score = book.averageScore,
                                statusText = "Book",
                                synopsis = synopsisText,
                                author = book.authorString.takeIf { it.isNotBlank() },
                                publisher = book.publisher,
                                isbn = book.primaryIsbn,
                                rawBookDetails = book,
                                rawJikanDetails = jikanDetails
                            )
                        }
                    }
                    SearchCategory.LIGHT_NOVELS -> {
                        val res = BookMetadataManager.searchLightNovels(query, limit = 20)
                        if (res.isFailure) throw res.exceptionOrNull() ?: Exception("Failed to fetch light novels")
                        res.getOrDefault(emptyList()).mapIndexed { idx, details ->
                            val extSource = details.sourceProvider
                            val extId = details.malId?.takeIf { it > 0 }?.toString() ?: "ln_${details.title?.hashCode() ?: idx}_$idx"
                            SearchResultItem(
                                externalId = extId,
                                externalSource = extSource,
                                mediaType = MediaType.BOOK,
                                title = details.title ?: "Unknown",
                                alternateTitle = details.alternateTitle,
                                posterUrl = details.posterUrl,
                                releaseYear = details.airedStartDate?.let { extractYear(it) },
                                totalUnits = details.totalChapters,
                                totalVolumes = details.totalVolumes,
                                score = details.externalScore,
                                statusText = "Light Novel",
                                synopsis = details.synopsis,
                                author = details.authors,
                                rawJikanDetails = details
                            )
                        }
                    }
                }
            }

            // Guard against stale results: only update UI if query and category still match current state
            if (_uiState.value.query.trim() == query && _uiState.value.selectedCategory == category) {
                _uiState.update {
                    it.copy(
                        results = results,
                        isLoading = false,
                        currentPage = 1,
                        canLoadMore = results.size >= 15,
                        errorMessage = if (results.isEmpty()) "No results found for '$query'" else null
                    )
                }
            }
        } catch (e: CancellationException) {
            // Rethrow so coroutine cancellation behaves properly.
            // Do NOT overwrite UI state with cancellation errors or set isLoading = false.
            throw e
        } catch (e: Exception) {
            if (_uiState.value.query.trim() == query && _uiState.value.selectedCategory == category) {
                val cleanMsg = e.message?.takeIf { msg ->
                    msg.isNotBlank() && !msg.contains("cancel", ignoreCase = true)
                } ?: "An error occurred while searching."
                _uiState.update {
                    it.copy(
                        results = emptyList(),
                        isLoading = false,
                        errorMessage = cleanMsg
                    )
                }
            }
        } finally {
            if (activeSearchKey == searchKey) {
                activeSearchKey = null
            }
        }
    }

    fun addToLibrary(
        item: SearchResultItem,
        status: MediaStatus,
        customCategory: String? = null,
        subCategory: String? = null,
        context: Context? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val targetUserId = repository.activeUser.value.id
                val entry: MediaEntry = if (item.externalSource == "TMDB") {
                    val tmdbId = item.externalId.toLongOrNull() ?: 0L
                    if (item.mediaType == MediaType.MOVIE) {
                        val fullDetails = TmdbApiClient.fetchMovieDetails(tmdbId, context).getOrNull()
                        MediaEntry(
                            userId = targetUserId,
                            title = fullDetails?.title ?: item.title,
                            alternateTitle = fullDetails?.alternateTitle ?: item.alternateTitle,
                            mediaType = MediaType.MOVIE,
                            status = status,
                            customCategory = customCategory,
                            subCategory = subCategory,
                            externalId = item.externalId,
                            externalSource = "TMDB",
                            externalScore = fullDetails?.externalScore ?: item.score,
                            externalScoreSource = "TMDB",
                            coverImageUri = fullDetails?.posterUrl ?: item.posterUrl,
                            durationMinutes = fullDetails?.durationMinutes ?: item.durationMinutes,
                            tags = fullDetails?.tags,
                            description = fullDetails?.synopsis ?: item.synopsis,
                            airedStartDate = fullDetails?.airedStartDate ?: item.rawTmdbResult?.releaseDate
                        )
                    } else {
                        val fullDetails = TmdbApiClient.fetchTvDetails(tmdbId, context).getOrNull()
                        MediaEntry(
                            userId = targetUserId,
                            title = fullDetails?.title ?: item.title,
                            alternateTitle = fullDetails?.alternateTitle ?: item.alternateTitle,
                            mediaType = MediaType.SHOW,
                            status = status,
                            customCategory = customCategory,
                            subCategory = subCategory,
                            externalId = item.externalId,
                            externalSource = "TMDB",
                            externalScore = fullDetails?.externalScore ?: item.score,
                            externalScoreSource = "TMDB",
                            coverImageUri = fullDetails?.posterUrl ?: item.posterUrl,
                            totalEpisodes = fullDetails?.totalEpisodes ?: 0,
                            totalSeasons = fullDetails?.totalSeasons ?: 1,
                            seasonEpisodesJson = fullDetails?.seasonEpisodes?.let { MediaEntry.formatSeasonEpisodes(it) },
                            episodeLengthMinutes = fullDetails?.durationMinutes,
                            tags = fullDetails?.tags,
                            description = fullDetails?.synopsis ?: item.synopsis,
                            airedStartDate = fullDetails?.airedStartDate ?: item.rawTmdbResult?.releaseDate,
                            isAiring = fullDetails?.isAiring ?: false
                        )
                    }
                } else {
                    // MAL / Jikan / Book / Light Novel item
                    val details = item.rawJikanDetails
                    val isVideo = item.mediaType == MediaType.ANIME
                    val isBook = item.mediaType == MediaType.BOOK
                    val unit = when {
                        (details?.totalPages ?: 0) > 0 -> TrackingUnit.PAGES
                        (details?.totalVolumes ?: 0) > 0 -> TrackingUnit.VOLUMES
                        (details?.totalChapters ?: 0) > 0 -> TrackingUnit.CHAPTERS
                        isBook -> TrackingUnit.PAGES
                        else -> TrackingUnit.CHAPTERS
                    }
                    val startEpoch = details?.airedStartDate
                    val endEpoch = details?.airedEndDate ?: if (isBook) startEpoch else null
                    MediaEntry(
                        userId = targetUserId,
                        title = details?.title ?: item.title,
                        alternateTitle = details?.alternateTitle ?: item.alternateTitle,
                        mediaType = item.mediaType,
                        status = status,
                        customCategory = customCategory,
                        subCategory = subCategory,
                        externalId = item.externalId,
                        externalSource = item.externalSource,
                        externalScore = details?.externalScore ?: item.score,
                        externalScoreSource = details?.externalScoreSource ?: item.externalSource,
                        coverImageUri = details?.posterUrl ?: item.posterUrl,
                        totalEpisodes = if (isVideo) (details?.totalEpisodes ?: item.totalUnits ?: 0) else 0,
                        airedEpisodes = if (isVideo) (details?.airedEpisodes ?: 0) else 0,
                        broadcastTime = if (isVideo) details?.broadcastTime else null,
                        nextAiringEpoch = if (isVideo) details?.nextAiringEpoch else null,
                        totalChapters = if (!isVideo && !isBook) (details?.totalChapters ?: item.totalUnits ?: 0) else (details?.totalChapters ?: 0),
                        totalVolumes = if (!isVideo) (details?.totalVolumes ?: item.totalVolumes ?: 0) else 0,
                        totalPages = if (isBook) (details?.totalPages ?: item.totalUnits ?: 0) else 0,
                        trackingUnit = unit,
                        durationMinutes = details?.durationMinutes,
                        episodeLengthMinutes = details?.durationMinutes,
                        tags = details?.tags,
                        description = details?.synopsis ?: item.synopsis,
                        airedStartDate = startEpoch,
                        airedEndDate = endEpoch,
                        isAiring = if (isBook) false else (details?.isAiring ?: false)
                    )
                }

                repository.insert(entry)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(userAddedMessage = "Added '${entry.title}' to library!") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Failed to add '${item.title}' to library", e)
            }
        }
    }

    fun clearAddedMessage() {
        _uiState.update { it.copy(userAddedMessage = null) }
    }

    private fun extractYear(timestamp: Long): String {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(java.util.Calendar.YEAR).toString()
    }
}

class SearchViewModelFactory(
    private val repository: MediaRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
