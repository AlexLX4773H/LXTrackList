package com.lx.tracklist.ui.edit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.Priority
import com.lx.tracklist.data.model.SubCategory
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.remote.AniListNovelApiClient
import com.lx.tracklist.data.remote.GoogleBooksApiClient
import com.lx.tracklist.data.remote.JikanApiClient
import com.lx.tracklist.data.remote.JikanMediaDetails
import com.lx.tracklist.data.remote.MangaDexApiClient
import com.lx.tracklist.data.remote.OpenLibraryApiClient
import com.lx.tracklist.data.remote.TachiyomiResolverManager
import com.lx.tracklist.data.remote.TmdbApiClient
import com.lx.tracklist.data.remote.TmdbConfig
import com.lx.tracklist.data.remote.TmdbMediaDetails
import com.lx.tracklist.data.remote.TmdbSearchResult
import com.lx.tracklist.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

enum class ExternalDbSource(val displayName: String, val shortName: String) {
    MAL("MyAnimeList (MAL)", "MAL"),
    MANGADEX("MangaDex", "MangaDex"),
    TMDB("The Movie Database (TMDB)", "TMDB"),
    GOOGLE_BOOKS("Google Books", "Google Books"),
    OPEN_LIBRARY("Open Library", "Open Library"),
    ANILIST_NOVEL("AniList (Light Novels)", "AniList")
}

data class ExternalSyncUiState(
    val isSearching: Boolean = false,
    val query: String = "",
    val selectedSource: ExternalDbSource = ExternalDbSource.MAL,
    val malResults: List<JikanMediaDetails> = emptyList(),
    val tmdbResults: List<TmdbSearchResult> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class AddEditUiState(
    val id: Long = 0L,
    val userId: Long = 0L,
    val title: String = "",
    val alternateTitles: List<String> = emptyList(),
    val alternateTitle: String = "",
    val mediaType: MediaType = MediaType.ANIME,
    val customCategory: String? = null,
    val subCategory: String? = null,
    val status: MediaStatus = MediaStatus.WATCHING,
    val score: Int? = null,
    val externalScore: Float? = null,
    val externalScoreSource: String = "",
    val priority: Priority = Priority.MEDIUM,
    val coverImageUri: String = "",
    val isFavorite: Boolean = false,
    val isExternallyTracked: Boolean = false,
    val rewatchCount: Int = 0,
    val isRewatching: Boolean = false,
    val externalId: String = "",
    val externalSource: String = "",
    val description: String = "",
    val userComments: String = "",
    val tags: String = "",
    val startDate: Long? = null,
    val finishDate: Long? = null,
    val airedStartDate: Long? = null,
    val airedEndDate: Long? = null,

    // Watch fields
    val currentEpisodes: Int = 0,
    val totalEpisodes: Int = 0,
    val airedEpisodes: Int = 0,
    val seasonNumber: Int = 1,
    val currentSeasons: Int = 1,
    val totalSeasons: Int = 1,
    val seasonEpisodes: List<Int> = emptyList(),
    val episodeLengthMinutes: Int? = null,
    val currentTimeMinutes: Int? = null,
    val isAiring: Boolean = false,

    // Read fields
    val trackingUnit: TrackingUnit = TrackingUnit.CHAPTERS,
    val currentVolumes: Int = 0,
    val totalVolumes: Int = 0,
    val currentChapters: Int = 0,
    val totalChapters: Int = 0,
    val airedChapters: Int = 0,
    val currentPages: Int = 0,
    val totalPages: Int = 0,

    // Movie fields
    val movieSeriesName: String = "",
    val movieSeriesNumber: Int? = null,
    val durationMinutes: Int? = null,

    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    val notes: String get() = description
}

class AddEditViewModel(
    private val repository: MediaRepository,
    private val entryId: Long?,
    private val initialMediaType: MediaType? = null,
    private val initialCustomCategory: String? = null,
    private val initialSubCategory: String? = null,
    private val initialStatus: MediaStatus? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        run {
            val status = initialStatus ?: MediaStatus.WATCHING
            AddEditUiState(
                userId = repository.activeUser.value.id,
                mediaType = initialMediaType ?: MediaType.ANIME,
                customCategory = initialCustomCategory,
                subCategory = initialSubCategory,
                status = status,
                startDate = if (status == MediaStatus.WATCHING) todayStartTimestamp() else null
            )
        }
    )
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private val _syncState = MutableStateFlow(
        ExternalSyncUiState(
            selectedSource = getDefaultSourceForType(initialMediaType ?: MediaType.ANIME)
        )
    )
    val syncState: StateFlow<ExternalSyncUiState> = _syncState.asStateFlow()

    private val _savedEvent = Channel<Unit>(Channel.BUFFERED)
    val savedEvent = _savedEvent.receiveAsFlow()

    val customCategories = repository.customCategories

    val currentCategoryKey: String
        get() {
            val s = _uiState.value
            return s.customCategory ?: s.mediaType.name
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val subCategories: Flow<List<SubCategory>> = _uiState
        .map { it.customCategory ?: it.mediaType.name }
        .distinctUntilChanged()
        .flatMapLatest { key -> repository.getSubCategories(key) }

    init {
        loadEntry(entryId)
    }

    fun loadEntry(id: Long?) {
        if (id != null && id > 0) {
            viewModelScope.launch {
                val entry = repository.getEntryByIdSync(id)
                if (entry != null) {
                    val altTitles = entry.getAlternateTitles()
                    val effectiveStartDate = if (entry.status == MediaStatus.WATCHING && entry.startDate == null) {
                        todayStartTimestamp()
                    } else {
                        entry.startDate
                    }
                    _uiState.update {
                        it.copy(
                            id = entry.id,
                            userId = entry.userId,
                            title = entry.title,
                            alternateTitles = altTitles,
                            alternateTitle = entry.alternateTitle ?: "",
                            mediaType = entry.mediaType,
                            customCategory = entry.customCategory,
                            subCategory = entry.subCategory,
                            status = entry.status,
                            score = entry.score,
                            externalScore = entry.externalScore,
                            externalScoreSource = entry.externalScoreSource ?: "",
                            priority = entry.priority,
                            coverImageUri = entry.coverImageUri ?: "",
                            isFavorite = entry.isFavorite,
                            isExternallyTracked = entry.isExternallyTracked,
                            rewatchCount = entry.rewatchCount,
                            isRewatching = entry.isRewatching,
                            externalId = entry.externalId ?: "",
                            externalSource = entry.externalSource ?: "",
                            description = entry.description ?: entry.notes ?: "",
                            userComments = entry.userComments ?: "",
                            tags = entry.tags ?: "",
                            startDate = effectiveStartDate,
                            finishDate = entry.finishDate,
                            airedStartDate = entry.airedStartDate,
                            airedEndDate = entry.airedEndDate,
                            currentEpisodes = entry.currentEpisodes,
                            totalEpisodes = entry.totalEpisodes,
                            airedEpisodes = entry.airedEpisodes,
                            seasonNumber = entry.seasonNumber,
                            currentSeasons = entry.currentSeasons,
                            totalSeasons = entry.totalSeasons,
                            seasonEpisodes = if (entry.mediaType == MediaType.SHOW) entry.getSeasonEpisodes() else emptyList(),
                            episodeLengthMinutes = entry.episodeLengthMinutes,
                            currentTimeMinutes = entry.currentTimeMinutes,
                            isAiring = entry.isAiring,
                            trackingUnit = entry.trackingUnit,
                            currentVolumes = entry.currentVolumes,
                            totalVolumes = entry.totalVolumes,
                            currentChapters = entry.currentChapters,
                            totalChapters = entry.totalChapters,
                            airedChapters = entry.airedChapters,
                            currentPages = entry.currentPages,
                            totalPages = entry.totalPages,
                            movieSeriesName = entry.movieSeriesName ?: "",
                            movieSeriesNumber = entry.movieSeriesNumber,
                            durationMinutes = entry.durationMinutes,
                            isSaved = false,
                            errorMessage = null
                        )
                    }
                    val defaultSource = getDefaultSourceForType(entry.mediaType)
                    _syncState.update { it.copy(selectedSource = defaultSource) }
                }
            }
        } else {
            val status = initialStatus ?: MediaStatus.WATCHING
            _uiState.value = AddEditUiState(
                userId = repository.activeUser.value.id,
                mediaType = initialMediaType ?: MediaType.ANIME,
                customCategory = initialCustomCategory,
                subCategory = initialSubCategory,
                status = status,
                startDate = if (status == MediaStatus.WATCHING) todayStartTimestamp() else null,
                isSaved = false,
                errorMessage = null
            )
        }
    }

    fun resetSavedState() {
        _uiState.update { it.copy(isSaved = false) }
    }

    fun updateTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun updateAlternateTitle(value: String) {
        val split = value.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        _uiState.update { it.copy(alternateTitle = value, alternateTitles = split) }
    }
    fun addAlternateTitle(value: String = "") {
        _uiState.update { it.copy(alternateTitles = it.alternateTitles + value) }
    }
    fun updateAlternateTitle(index: Int, value: String) {
        _uiState.update { state ->
            val list = state.alternateTitles.toMutableList()
            if (index in list.indices) {
                list[index] = value
                state.copy(alternateTitles = list, alternateTitle = list.filter { it.isNotBlank() }.joinToString("\n"))
            } else {
                state
            }
        }
    }
    fun removeAlternateTitle(index: Int) {
        _uiState.update { state ->
            val list = state.alternateTitles.toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
                state.copy(alternateTitles = list, alternateTitle = list.filter { it.isNotBlank() }.joinToString("\n"))
            } else {
                state
            }
        }
    }
    fun updateMediaType(value: MediaType) {
        val defaultSource = getDefaultSourceForType(value)
        _syncState.update { it.copy(selectedSource = defaultSource) }
        _uiState.update { current ->
            val effectiveStartDate = if (current.status == MediaStatus.WATCHING && current.startDate == null) {
                todayStartTimestamp()
            } else {
                current.startDate
            }
            current.copy(
                mediaType = value,
                customCategory = null,
                subCategory = null,
                startDate = effectiveStartDate
            )
        }
    }
    fun updateCustomCategory(name: String?) = _uiState.update { it.copy(customCategory = name, subCategory = null) }
    fun selectCategory(mediaType: MediaType, customCategory: String? = null) {
        val defaultSource = getDefaultSourceForType(mediaType)
        _syncState.update { it.copy(selectedSource = defaultSource) }
        _uiState.update { current ->
            val effectiveStartDate = if (current.status == MediaStatus.WATCHING && current.startDate == null) {
                todayStartTimestamp()
            } else {
                current.startDate
            }
            current.copy(
                mediaType = mediaType,
                customCategory = customCategory,
                subCategory = null,
                startDate = effectiveStartDate
            )
        }
    }

    fun updateSubCategory(name: String?) = _uiState.update { it.copy(subCategory = name?.trim()?.ifBlank { null }) }

    fun addSubCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val key = currentCategoryKey
        viewModelScope.launch {
            repository.addSubCategory(key, trimmed)
            updateSubCategory(trimmed)
        }
    }
    fun updateStatus(value: MediaStatus) = _uiState.update { current ->
        if (value == MediaStatus.WATCHING && current.startDate == null) {
            current.copy(status = value, startDate = todayStartTimestamp())
        } else {
            current.copy(status = value)
        }
    }
    fun updateScore(value: Int?) = _uiState.update { it.copy(score = value) }
    fun updateExternalScore(value: Float?) = _uiState.update { it.copy(externalScore = value) }
    fun updateExternalScoreSource(value: String) = _uiState.update { it.copy(externalScoreSource = value) }
    fun updatePriority(value: Priority) = _uiState.update { it.copy(priority = value) }
    fun updateCoverUri(value: String) = _uiState.update { it.copy(coverImageUri = value) }
    fun updateIsFavorite(value: Boolean) = _uiState.update { it.copy(isFavorite = value) }
    fun updateIsExternallyTracked(value: Boolean) = _uiState.update { it.copy(isExternallyTracked = value) }
    fun updateRewatchCount(value: Int) = _uiState.update { it.copy(rewatchCount = value.coerceAtLeast(0)) }
    fun updateIsRewatching(value: Boolean) = _uiState.update { it.copy(isRewatching = value) }
    fun updateExternalId(value: String) = _uiState.update { it.copy(externalId = value) }
    fun updateExternalSource(value: String) = _uiState.update { it.copy(externalSource = value) }
    fun updateDescription(value: String) = _uiState.update { it.copy(description = value) }
    fun updateUserComments(value: String) = _uiState.update { it.copy(userComments = value) }
    fun updateNotes(value: String) = updateDescription(value)
    fun updateTags(value: String) = _uiState.update { it.copy(tags = value) }
    fun updateStartDate(value: Long?) = _uiState.update { it.copy(startDate = value) }
    fun updateFinishDate(value: Long?) = _uiState.update { it.copy(finishDate = value) }
    fun updateAiredStartDate(value: Long?) = _uiState.update { it.copy(airedStartDate = value) }
    fun updateAiredEndDate(value: Long?) = _uiState.update { it.copy(airedEndDate = value) }

    fun updateCurrentEpisodes(value: Int) = _uiState.update { it.copy(currentEpisodes = value) }
    fun updateTotalEpisodes(value: Int) = _uiState.update { it.copy(totalEpisodes = value) }
    fun updateAiredEpisodes(value: Int) = _uiState.update { it.copy(airedEpisodes = value) }
    fun updateCurrentSeasons(value: Int) = _uiState.update { it.copy(currentSeasons = value.coerceAtLeast(1)) }
    fun updateTotalSeasons(value: Int) = _uiState.update { it.copy(totalSeasons = value.coerceAtLeast(1)) }
    fun updateSeasonEpisodeCount(seasonIndex: Int, episodes: Int) = _uiState.update { state ->
        if (seasonIndex < 0) return@update state
        val targetSize = maxOf(state.totalSeasons.coerceAtLeast(1), state.seasonEpisodes.size, seasonIndex + 1)
        val updated = MutableList(targetSize) { idx -> state.seasonEpisodes.getOrElse(idx) { 0 } }
        updated[seasonIndex] = episodes.coerceAtLeast(0)
        val newSum = updated.sum()
        state.copy(
            seasonEpisodes = updated,
            totalEpisodes = if (newSum > 0) newSum else state.totalEpisodes
        )
    }
    fun updateEpisodeLengthMinutes(value: Int?) = _uiState.update { it.copy(episodeLengthMinutes = value) }
    fun updateCurrentTimeMinutes(value: Int?) = _uiState.update { it.copy(currentTimeMinutes = value) }
    fun updateIsAiring(value: Boolean) = _uiState.update { it.copy(isAiring = value) }

    fun updateTrackingUnit(value: TrackingUnit) = _uiState.update { it.copy(trackingUnit = value) }
    fun updateCurrentVolumes(value: Int) = _uiState.update { it.copy(currentVolumes = value) }
    fun updateTotalVolumes(value: Int) = _uiState.update { it.copy(totalVolumes = value) }
    fun updateCurrentChapters(value: Int) = _uiState.update { it.copy(currentChapters = value) }
    fun updateTotalChapters(value: Int) = _uiState.update { it.copy(totalChapters = value) }
    fun updateAiredChapters(value: Int) = _uiState.update { it.copy(airedChapters = value) }
    fun updateCurrentPages(value: Int) = _uiState.update { it.copy(currentPages = value) }
    fun updateTotalPages(value: Int) = _uiState.update { it.copy(totalPages = value) }

    fun updateMovieSeriesName(value: String) = _uiState.update { it.copy(movieSeriesName = value) }
    fun updateMovieSeriesNumber(value: Int?) = _uiState.update { it.copy(movieSeriesNumber = value) }
    fun updateDurationMinutes(value: Int?) = _uiState.update { it.copy(durationMinutes = value) }

    fun saveEntry() {
        val s = _uiState.value
        if (s.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title cannot be empty") }
            return
        }

        viewModelScope.launch {
            val formattedAltTitle = MediaEntry.formatAlternateTitles(
                if (s.alternateTitles.isNotEmpty()) s.alternateTitles
                else if (s.alternateTitle.isNotBlank()) listOf(s.alternateTitle)
                else emptyList()
            )
            val effectiveStartDate = if (s.status == MediaStatus.WATCHING && s.startDate == null) {
                todayStartTimestamp()
            } else {
                s.startDate
            }
            val effectiveUserId = if (s.userId > 0L) s.userId else repository.activeUser.value.id
            val effectiveSeasonEpisodesJson = if (s.mediaType == MediaType.SHOW) {
                val sliced = s.seasonEpisodes.take(s.totalSeasons.coerceAtLeast(1))
                MediaEntry.formatSeasonEpisodes(sliced)
            } else null
            val entry = MediaEntry(
                id = s.id,
                userId = effectiveUserId,
                title = s.title.trim(),
                alternateTitle = formattedAltTitle,
                mediaType = s.mediaType,
                customCategory = s.customCategory?.trim()?.ifBlank { null },
                subCategory = s.subCategory?.trim()?.ifBlank { null },
                status = s.status,
                score = s.score,
                externalScore = s.externalScore,
                externalScoreSource = s.externalScoreSource.trim().ifBlank { null },
                priority = s.priority,
                coverImageUri = s.coverImageUri.trim().ifBlank { null },
                isFavorite = s.isFavorite,
                isExternallyTracked = s.isExternallyTracked,
                rewatchCount = s.rewatchCount,
                isRewatching = s.isRewatching,
                externalId = s.externalId.trim().ifBlank { null },
                externalSource = s.externalSource.trim().ifBlank { null },
                description = s.description.trim().ifBlank { null },
                userComments = s.userComments.trim().ifBlank { null },
                tags = s.tags.trim().ifBlank { null },
                startDate = effectiveStartDate,
                finishDate = s.finishDate,
                airedStartDate = s.airedStartDate,
                airedEndDate = s.airedEndDate,
                currentEpisodes = s.currentEpisodes,
                totalEpisodes = s.totalEpisodes,
                airedEpisodes = s.airedEpisodes,
                seasonNumber = s.seasonNumber,
                currentSeasons = s.currentSeasons,
                totalSeasons = s.totalSeasons,
                seasonEpisodesJson = effectiveSeasonEpisodesJson,
                episodeLengthMinutes = s.episodeLengthMinutes,
                currentTimeMinutes = if (s.mediaType == MediaType.ANIME || s.mediaType == MediaType.SHOW || s.mediaType == MediaType.MOVIE) s.currentTimeMinutes else null,
                isAiring = s.isAiring,
                trackingUnit = s.trackingUnit,
                currentVolumes = s.currentVolumes,
                totalVolumes = s.totalVolumes,
                currentChapters = s.currentChapters,
                totalChapters = s.totalChapters,
                airedChapters = s.airedChapters,
                currentPages = s.currentPages,
                totalPages = s.totalPages,
                movieSeriesName = s.movieSeriesName.trim().ifBlank { null },
                movieSeriesNumber = s.movieSeriesNumber,
                durationMinutes = s.durationMinutes,
                updatedAt = System.currentTimeMillis()
            )

            if (entry.id == 0L) {
                repository.insert(entry)
            } else {
                repository.update(entry)
            }

            _uiState.update { it.copy(isSaved = true, startDate = effectiveStartDate) }
            _savedEvent.send(Unit)
        }
    }

    fun searchExternalDb(query: String, source: ExternalDbSource, context: Context? = null) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            _syncState.update {
                it.copy(
                    query = "",
                    malResults = emptyList(),
                    tmdbResults = emptyList(),
                    errorMessage = "Please enter a title to search."
                )
            }
            return
        }

        _syncState.update {
            it.copy(
                isSearching = true,
                query = cleanQuery,
                selectedSource = source,
                errorMessage = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (source) {
                    ExternalDbSource.MAL -> {
                        val isManga = _uiState.value.mediaType == MediaType.MANGA ||
                                _uiState.value.mediaType == MediaType.MANHWA ||
                                _uiState.value.mediaType == MediaType.BOOK
                        val subType = if (_uiState.value.mediaType == MediaType.MANHWA) "manhwa" else null
                        val res = JikanApiClient.searchJikanMultiple(cleanQuery, isManga = isManga, subType = subType, limit = 20)
                        var items = res.getOrDefault(emptyList())
                        if (items.isEmpty() && subType != null) {
                            val fallback = JikanApiClient.searchJikanMultiple(cleanQuery, isManga = true, subType = null, limit = 20)
                            items = fallback.getOrDefault(emptyList())
                        }
                        _syncState.update {
                            it.copy(
                                isSearching = false,
                                malResults = items,
                                tmdbResults = emptyList(),
                                errorMessage = if (items.isEmpty()) "No matches found on MyAnimeList for '$cleanQuery'" else null
                            )
                        }
                    }
                    ExternalDbSource.MANGADEX -> {
                        val isManhwa = _uiState.value.mediaType == MediaType.MANHWA
                        coroutineScope {
                            val mdDeferred = async { MangaDexApiClient.searchMangaMultiple(cleanQuery, isManhwa = isManhwa, limit = 20).getOrDefault(emptyList()) }
                            val aniDeferred = if (isManhwa) {
                                async { JikanApiClient.queryAniListMultiple(cleanQuery, isManga = true, subType = "manhwa", limit = 15).getOrDefault(emptyList()) }
                            } else {
                                async { emptyList<JikanMediaDetails>() }
                            }
                            val mdList = mdDeferred.await()
                            val aniList = aniDeferred.await()

                            val seen = mutableSetOf<String>()
                            val combined = mutableListOf<JikanMediaDetails>()
                            for (item in mdList) {
                                val key = item.title?.trim()?.lowercase(Locale.ROOT) ?: continue
                                if (seen.add(key)) combined.add(item)
                            }
                            for (item in aniList) {
                                val key = item.title?.trim()?.lowercase(Locale.ROOT) ?: continue
                                if (seen.add(key)) combined.add(item)
                            }

                            _syncState.update {
                                it.copy(
                                    isSearching = false,
                                    malResults = combined,
                                    tmdbResults = emptyList(),
                                    errorMessage = if (combined.isEmpty()) "No matches found on MangaDex / AniList for '$cleanQuery'" else null
                                )
                            }
                        }
                    }
                    ExternalDbSource.TMDB -> {
                        if (!TmdbConfig.isConfigured(context)) {
                            _syncState.update {
                                it.copy(
                                    isSearching = false,
                                    tmdbResults = emptyList(),
                                    malResults = emptyList(),
                                    errorMessage = "TMDB Read Access Token is not configured. Please add your token in Settings > TMDB Sync."
                                )
                            }
                            return@launch
                        }

                        val isTv = _uiState.value.mediaType == MediaType.SHOW

                        coroutineScope {
                            val tvDeferred = async { TmdbApiClient.searchTvMultiple(cleanQuery, context).getOrDefault(emptyList()) }
                            val moviesDeferred = async { TmdbApiClient.searchMoviesMultiple(cleanQuery, context).getOrDefault(emptyList()) }

                            val tvResults = tvDeferred.await()
                            val movieResults = moviesDeferred.await()

                            val finalResults = if (isTv) {
                                tvResults + movieResults
                            } else {
                                movieResults + tvResults
                            }

                            _syncState.update {
                                it.copy(
                                    isSearching = false,
                                    tmdbResults = finalResults,
                                    malResults = emptyList(),
                                    errorMessage = if (finalResults.isEmpty()) "No matches found on TMDB for '$cleanQuery'" else null
                                )
                            }
                        }
                    }
                    ExternalDbSource.GOOGLE_BOOKS -> {
                        coroutineScope {
                            val gbDeferred = async { GoogleBooksApiClient.searchBooks(cleanQuery, context, limit = 20).getOrDefault(emptyList()).map { it.toJikanMediaDetails() } }
                            val olDeferred = async { OpenLibraryApiClient.searchBooks(cleanQuery, limit = 20).getOrDefault(emptyList()).map { it.toJikanMediaDetails() } }

                            val gbResults = gbDeferred.await()
                            val olResults = olDeferred.await()

                            val seen = mutableSetOf<String>()
                            val combined = mutableListOf<JikanMediaDetails>()
                            for (item in gbResults) {
                                val key = item.title?.trim()?.lowercase(Locale.ROOT) ?: continue
                                if (seen.add(key)) combined.add(item)
                            }
                            for (item in olResults) {
                                val key = item.title?.trim()?.lowercase(Locale.ROOT) ?: continue
                                if (seen.add(key)) combined.add(item)
                            }

                            _syncState.update {
                                it.copy(
                                    isSearching = false,
                                    malResults = combined,
                                    tmdbResults = emptyList(),
                                    errorMessage = if (combined.isEmpty()) "No matches found on Google Books / Open Library for '$cleanQuery'" else null
                                )
                            }
                        }
                    }
                    ExternalDbSource.OPEN_LIBRARY -> {
                        val res = OpenLibraryApiClient.searchBooks(cleanQuery, limit = 20)
                        val items = res.getOrDefault(emptyList()).map { it.toJikanMediaDetails() }
                        _syncState.update {
                            it.copy(
                                isSearching = false,
                                malResults = items,
                                tmdbResults = emptyList(),
                                errorMessage = if (items.isEmpty()) "No matches found on Open Library for '$cleanQuery'" else null
                            )
                        }
                    }
                    ExternalDbSource.ANILIST_NOVEL -> {
                        val res = AniListNovelApiClient.searchLightNovels(cleanQuery, limit = 20)
                        val items = res.getOrDefault(emptyList())
                        _syncState.update {
                            it.copy(
                                isSearching = false,
                                malResults = items,
                                tmdbResults = emptyList(),
                                errorMessage = if (items.isEmpty()) "No matches found on AniList for '$cleanQuery'" else null
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _syncState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = e.message ?: "An error occurred while searching."
                    )
                }
            }
        }
    }

    fun applySelectedMalResult(item: JikanMediaDetails) {
        _uiState.update { current ->
            val isVideo = current.mediaType == MediaType.ANIME || current.mediaType == MediaType.MOVIE || current.mediaType == MediaType.SHOW
            val queryTitle = current.title.trim()
            val cleanedOriginalTitle = TachiyomiResolverManager.stripBracketedSuffixes(queryTitle).takeIf { it.isNotBlank() } ?: queryTitle
            val candidateTitle = item.title?.trim()?.takeIf { it.isNotBlank() }

            val candidateAlts = item.alternateTitle?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            val currentAlts = (current.alternateTitle?.split("\n") ?: emptyList()).map { it.trim() }.filter { it.isNotBlank() } + current.alternateTitles

            val extraAlts = if (candidateTitle != null && !candidateTitle.equals(cleanedOriginalTitle, ignoreCase = true)) {
                listOf(candidateTitle)
            } else emptyList()

            val allAlts = (extraAlts + candidateAlts + currentAlts)
                .distinct()
                .filter { !it.equals(cleanedOriginalTitle, ignoreCase = true) }

            // Preserve original Tachiyomi ID in description so future backup reimports match seamlessly
            val originalTachiId = current.externalId?.takeIf {
                !it.isNullOrBlank() && (current.externalSource?.contains("TACHIYOMI", ignoreCase = true) == true || it.startsWith("/"))
            }
            val tachiNoteMarker = if (originalTachiId != null) "[Tachiyomi ID: $originalTachiId]" else null
            val baseDesc = if (current.description.isBlank() && !item.synopsis.isNullOrBlank()) (item.synopsis ?: "") else current.description
            val finalDesc = if (tachiNoteMarker != null && !baseDesc.contains(tachiNoteMarker)) {
                if (baseDesc.isBlank()) tachiNoteMarker else "$baseDesc\n\n$tachiNoteMarker"
            } else baseDesc

            // Align media type if current is generic or incompatible
            val targetMediaType = if (!isVideo && (item.type.equals("TV", ignoreCase = true) || item.type.equals("Movie", ignoreCase = true))) {
                if (item.type.equals("Movie", ignoreCase = true)) MediaType.MOVIE else MediaType.ANIME
            } else if (item.type.equals("Manhwa", ignoreCase = true)) {
                MediaType.MANHWA
            } else if (item.type.equals("Book", ignoreCase = true)) {
                MediaType.BOOK
            } else {
                current.mediaType
            }

            val externalSrc = when (item.sourceProvider) {
                "MangaDex" -> "MangaDex"
                "Kitsu" -> "Kitsu"
                "AniList" -> "AniList"
                "Google Books" -> "Google Books"
                "Open Library" -> "Open Library"
                else -> if (item.sourceProvider.startsWith("AniList")) "AniList" else "MAL"
            }
            val scoreSrc = item.externalScoreSource ?: externalSrc

            val resolvedExtId = when (item.sourceProvider) {
                "MangaDex" -> item.url?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: current.externalId
                "Google Books", "Open Library" -> {
                    val u = item.url
                    val fromUrl = when {
                        u != null && u.contains("id=") -> Regex("[?&]id=([^&]+)").find(u)?.groupValues?.get(1)
                        u != null -> u.substringAfterLast('/').removePrefix("/works/").removePrefix("/").takeIf { it.isNotBlank() }
                        else -> null
                    }
                    fromUrl ?: (item.isbn ?: current.externalId)
                }
                else -> (item.malId ?: 0L).takeIf { it > 0 }?.toString() ?: current.externalId
            }

            val isBookSource = targetMediaType == MediaType.BOOK ||
                    item.type.equals("Book", ignoreCase = true) ||
                    item.sourceProvider in listOf("Google Books", "Open Library")

            val hasPages = item.totalPages != null && item.totalPages > 0
            val hasVolumes = item.totalVolumes != null && item.totalVolumes > 0
            val hasChapters = item.totalChapters != null && item.totalChapters > 0

            val resolvedUnit = when {
                hasPages -> TrackingUnit.PAGES
                hasVolumes -> TrackingUnit.VOLUMES
                hasChapters -> TrackingUnit.CHAPTERS
                isBookSource -> TrackingUnit.PAGES
                else -> current.trackingUnit
            }

            val newTotalPages = if (hasPages) (item.totalPages ?: current.totalPages) else current.totalPages
            val newTotalVolumes = if (!isVideo && hasVolumes) (item.totalVolumes ?: current.totalVolumes) else current.totalVolumes
            val newTotalChapters = if (!isVideo && hasChapters) (item.totalChapters ?: current.totalChapters) else current.totalChapters

            val resolvedStartDate = item.airedStartDate ?: current.airedStartDate
            val resolvedEndDate = item.airedEndDate
                ?: if (isBookSource || item.isAiring == false) resolvedStartDate
                else current.airedEndDate

            current.copy(
                title = cleanedOriginalTitle,
                alternateTitle = allAlts.joinToString("\n").takeIf { it.isNotBlank() } ?: current.alternateTitle,
                alternateTitles = allAlts,
                mediaType = targetMediaType,
                coverImageUri = item.posterUrl ?: current.coverImageUri,
                externalId = resolvedExtId,
                externalSource = externalSrc,
                externalScore = item.externalScore ?: current.externalScore,
                externalScoreSource = scoreSrc,
                description = finalDesc,
                tags = (if (current.tags.isBlank() && !item.tags.isNullOrBlank()) item.tags else current.tags) ?: "",
                airedStartDate = resolvedStartDate,
                airedEndDate = resolvedEndDate,
                isAiring = if (isBookSource) false else (item.isAiring ?: current.isAiring),
                totalEpisodes = if (isVideo && item.totalEpisodes != null && item.totalEpisodes > 0 && current.totalEpisodes == 0) item.totalEpisodes else current.totalEpisodes,
                episodeLengthMinutes = if (isVideo && item.durationMinutes != null && item.durationMinutes > 0 && (current.episodeLengthMinutes == null || current.episodeLengthMinutes == 0)) item.durationMinutes else current.episodeLengthMinutes,
                durationMinutes = if (current.mediaType == MediaType.MOVIE && item.durationMinutes != null && item.durationMinutes > 0 && (current.durationMinutes == null || current.durationMinutes == 0)) item.durationMinutes else current.durationMinutes,
                totalChapters = newTotalChapters,
                airedChapters = if (!isVideo && item.airedChapters != null && item.airedChapters > 0 && current.airedChapters == 0) item.airedChapters else current.airedChapters,
                totalVolumes = newTotalVolumes,
                totalPages = newTotalPages,
                trackingUnit = resolvedUnit
            )
        }
        val sourceLabel = item.sourceProvider
        _syncState.update { it.copy(successMessage = "Applied '${item.title}' from $sourceLabel!") }
    }

    fun applySelectedTmdbResult(item: TmdbSearchResult, context: Context? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val tmdbId = item.tmdbId
            val fullDetails = if (item.isMovie) {
                TmdbApiClient.fetchMovieDetails(tmdbId, context).getOrNull()
            } else {
                TmdbApiClient.fetchTvDetails(tmdbId, context).getOrNull()
            }

            withContext(Dispatchers.Main) {
                _uiState.update { current ->
                    val isMovie = item.isMovie
                    val queryTitle = current.title.trim()
                    val cleanedOriginalTitle = TachiyomiResolverManager.stripBracketedSuffixes(queryTitle).takeIf { it.isNotBlank() } ?: queryTitle
                    val candidateTitle = (fullDetails?.title ?: item.title).trim().takeIf { it.isNotBlank() }

                    val candidateAlts = mutableListOf<String>()
                    fullDetails?.alternateTitle?.split("\n")?.forEach { line ->
                        val clean = line.trim()
                        if (clean.isNotBlank() && !clean.equals(cleanedOriginalTitle, ignoreCase = true)) {
                            candidateAlts.add(clean)
                        }
                    }
                    if (!item.originalTitle.isNullOrBlank() && !item.originalTitle.equals(cleanedOriginalTitle, ignoreCase = true)) {
                        candidateAlts.add(item.originalTitle.trim())
                    }

                    val currentAlts = (current.alternateTitle?.split("\n") ?: emptyList()).map { it.trim() }.filter { it.isNotBlank() } + current.alternateTitles
                    val extraAlts = if (candidateTitle != null && !candidateTitle.equals(cleanedOriginalTitle, ignoreCase = true)) {
                        listOf(candidateTitle)
                    } else emptyList()

                    val allAlts = (extraAlts + candidateAlts + currentAlts)
                        .distinct()
                        .filter { !it.equals(cleanedOriginalTitle, ignoreCase = true) }

                    val targetMediaType = if (isMovie) MediaType.MOVIE else MediaType.SHOW
                    val baseDesc = if (current.description.isBlank() && !(fullDetails?.synopsis ?: item.overview).isNullOrBlank()) {
                        (fullDetails?.synopsis ?: item.overview) ?: ""
                    } else current.description

                    val duration = if (isMovie) {
                        if (fullDetails?.durationMinutes != null && fullDetails.durationMinutes > 0) fullDetails.durationMinutes else current.durationMinutes
                    } else current.durationMinutes

                    val epLength = if (!isMovie) {
                        if (fullDetails?.durationMinutes != null && fullDetails.durationMinutes > 0) fullDetails.durationMinutes else current.episodeLengthMinutes
                    } else current.episodeLengthMinutes

                    current.copy(
                        title = cleanedOriginalTitle,
                        alternateTitle = allAlts.joinToString("\n").takeIf { it.isNotBlank() } ?: current.alternateTitle,
                        alternateTitles = allAlts,
                        mediaType = targetMediaType,
                        coverImageUri = fullDetails?.posterUrl ?: item.posterUrl ?: item.backdropUrl ?: current.coverImageUri,
                        externalId = item.tmdbId.toString(),
                        externalSource = "TMDB",
                        externalScore = fullDetails?.externalScore ?: item.voteAverage ?: current.externalScore,
                        externalScoreSource = "TMDB",
                        description = baseDesc,
                        tags = (if (current.tags.isBlank() && !fullDetails?.tags.isNullOrBlank()) fullDetails?.tags else current.tags) ?: "",
                        airedStartDate = fullDetails?.airedStartDate ?: item.releaseDate ?: current.airedStartDate,
                        airedEndDate = fullDetails?.airedEndDate ?: current.airedEndDate,
                        isAiring = fullDetails?.isAiring ?: current.isAiring,
                        durationMinutes = duration,
                        episodeLengthMinutes = epLength,
                        totalEpisodes = if (!isMovie && fullDetails?.totalEpisodes != null && fullDetails.totalEpisodes > 0 && (current.totalEpisodes == 0 || ((fullDetails.totalSeasons ?: 0) > 1 && current.totalEpisodes <= 1))) fullDetails.totalEpisodes else current.totalEpisodes,
                        totalSeasons = if (!isMovie && fullDetails?.totalSeasons != null && fullDetails.totalSeasons > 0 && current.totalSeasons <= 1) fullDetails.totalSeasons else current.totalSeasons,
                        seasonEpisodes = if (!isMovie && !fullDetails?.seasonEpisodes.isNullOrEmpty() && current.seasonEpisodes.all { it <= 0 }) {
                            fullDetails.seasonEpisodes.orEmpty()
                        } else current.seasonEpisodes
                    )
                }
                _syncState.update { it.copy(successMessage = "Applied '${item.title}' from TMDB!") }
            }
        }
    }

    fun clearSyncMessage() {
        _syncState.update { it.copy(successMessage = null) }
    }

    companion object {
        fun getDefaultSourceForType(mediaType: MediaType): ExternalDbSource = when {
            mediaType == MediaType.MANHWA -> ExternalDbSource.MANGADEX
            mediaType == MediaType.BOOK -> ExternalDbSource.GOOGLE_BOOKS
            mediaType == MediaType.MOVIE || mediaType == MediaType.SHOW -> ExternalDbSource.TMDB
            else -> ExternalDbSource.MAL
        }

        fun todayStartTimestamp(): Long {
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }
}

class AddEditViewModelFactory(
    private val repository: MediaRepository,
    private val entryId: Long?,
    private val initialMediaType: MediaType? = null,
    private val initialCustomCategory: String? = null,
    private val initialSubCategory: String? = null,
    private val initialStatus: MediaStatus? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEditViewModel(
                repository = repository,
                entryId = entryId,
                initialMediaType = initialMediaType,
                initialCustomCategory = initialCustomCategory,
                initialSubCategory = initialSubCategory,
                initialStatus = initialStatus
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
