package com.lx.tracklist.ui.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.RelatedMediaItem
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.model.UserProfile
import com.lx.tracklist.data.remote.BookMetadataManager
import com.lx.tracklist.data.remote.GoogleBooksApiClient
import com.lx.tracklist.data.remote.JikanApiClient
import com.lx.tracklist.data.remote.JikanSyncManager
import com.lx.tracklist.data.remote.MangaDexApiClient
import com.lx.tracklist.data.remote.OpenLibraryApiClient
import com.lx.tracklist.data.remote.TachiyomiResolverManager
import com.lx.tracklist.data.remote.TmdbApiClient
import com.lx.tracklist.data.remote.mal.MalSyncManager
import com.lx.tracklist.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

class DetailViewModel(
    private val repository: MediaRepository,
    private val entryId: Long
) : ViewModel() {

    val entry: StateFlow<MediaEntry?> = repository.getEntryById(entryId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val activeUser: StateFlow<UserProfile> = repository.activeUser

    private val _rawRelatedEntries = MutableStateFlow<List<RelatedMediaItem>>(emptyList())
    private val _isLoadingRelated = MutableStateFlow(false)
    val isLoadingRelated: StateFlow<Boolean> = _isLoadingRelated.asStateFlow()

    private val _relatedErrorMessage = MutableStateFlow<String?>(null)
    val relatedErrorMessage: StateFlow<String?> = _relatedErrorMessage.asStateFlow()

    private val _isRefreshingMetadata = MutableStateFlow(false)
    val isRefreshingMetadata: StateFlow<Boolean> = _isRefreshingMetadata.asStateFlow()

    private val _metadataRefreshMessage = MutableStateFlow<String?>(null)
    val metadataRefreshMessage: StateFlow<String?> = _metadataRefreshMessage.asStateFlow()

    fun clearRefreshMessage() {
        _metadataRefreshMessage.value = null
    }

    private val _isPushingToExternalTracker = MutableStateFlow(false)
    val isPushingToExternalTracker: StateFlow<Boolean> = _isPushingToExternalTracker.asStateFlow()

    private val _externalSyncMessage = MutableStateFlow<String?>(null)
    val externalSyncMessage: StateFlow<String?> = _externalSyncMessage.asStateFlow()

    fun clearExternalSyncMessage() {
        _externalSyncMessage.value = null
    }

    fun pushToExternalTracker(context: Context) {
        val cur = entry.value ?: return
        if (_isPushingToExternalTracker.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isPushingToExternalTracker.value = true
            try {
                if (!cur.isExternallyTracked) {
                    _externalSyncMessage.value = "External tracking is off for this entry. Enable it first."
                    return@launch
                }
                if (!MalSyncManager.canBeExternallyTracked(cur)) {
                    _externalSyncMessage.value = "Missing valid MyAnimeList ID."
                    return@launch
                }
                val result = MalSyncManager.pushUpdateSync(context, cur, force = true)
                if (result.isSuccess) {
                    _externalSyncMessage.value = "Successfully pushed to MyAnimeList!"
                } else {
                    val ex = result.exceptionOrNull()
                    _externalSyncMessage.value = "MAL sync failed: ${ex?.message ?: "Unknown error"}"
                }
            } catch (e: Exception) {
                _externalSyncMessage.value = "MAL sync error: ${e.message}"
            } finally {
                _isPushingToExternalTracker.value = false
            }
        }
    }

    val relatedEntries: StateFlow<List<RelatedMediaItem>> = combine(
        _rawRelatedEntries,
        repository.getAllEntries()
    ) { related, allEntries ->
        related.map { item ->
            val match = allEntries.firstOrNull { entry ->
                entry.matchesExternal(
                    candidateExternalId = item.externalId,
                    candidateExternalSource = item.externalSource,
                    candidateTitle = item.title,
                    candidateMediaType = item.mediaType,
                    candidateReleaseYear = item.releaseYear
                )
            }
            item.copy(
                inLibraryEntryId = match?.id,
                inLibraryStatus = match?.status
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var loadedForEntryId: Long? = null

    init {
        viewModelScope.launch {
            entry.collect { currentEntry ->
                if (currentEntry != null) {
                    val localRelated = currentEntry.getRelatedMediaItems()
                    if (localRelated.isNotEmpty() && _rawRelatedEntries.value.isEmpty()) {
                        _rawRelatedEntries.value = localRelated
                    }
                    loadRelatedEntries(currentEntry)
                }
            }
        }
    }

    private fun isAnimeOrManga(cur: MediaEntry): Boolean {
        val src = cur.externalSource ?: ""
        val scoreSrc = cur.externalScoreSource ?: ""
        val cat = cur.customCategory ?: ""
        val subCat = cur.subCategory ?: ""

        if (src.equals("MAL", ignoreCase = true) || src.equals("AniList", ignoreCase = true)) return true
        if (scoreSrc.equals("MAL", ignoreCase = true) || scoreSrc.equals("AniList", ignoreCase = true)) return true
        if (cur.mediaType == MediaType.ANIME || cur.mediaType == MediaType.MANGA ||
            cur.mediaType == MediaType.MANHWA || cur.mediaType == MediaType.BOOK) return true
        if (cat.contains("Anime", ignoreCase = true) || cat.contains("Manga", ignoreCase = true) || cat.contains("Donghua", ignoreCase = true)) return true
        if (subCat.contains("Anime", ignoreCase = true) || subCat.contains("Manga", ignoreCase = true) || subCat.contains("Donghua", ignoreCase = true)) return true

        return false
    }

    fun loadRelatedEntries(cur: MediaEntry, context: Context? = null) {
        if (loadedForEntryId == cur.id && _rawRelatedEntries.value.isNotEmpty()) return
        loadedForEntryId = cur.id

        val localRelated = cur.getRelatedMediaItems()
        if (localRelated.isNotEmpty() || (cur.relatedEntriesJson == "[]" && cur.mediaType != MediaType.BOOK)) {
            _rawRelatedEntries.value = localRelated
            _isLoadingRelated.value = false
            _relatedErrorMessage.value = null
            return
        }

        _isLoadingRelated.value = true
        _relatedErrorMessage.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isAnime = isAnimeOrManga(cur)
                val isBook = cur.mediaType == MediaType.BOOK
                val isManga = (cur.mediaType == MediaType.MANGA || cur.mediaType == MediaType.MANHWA) && !isBook
                val isVideo = cur.mediaType == MediaType.MOVIE || cur.mediaType == MediaType.SHOW
                val src = cur.externalSource ?: ""
                var extId = cur.externalId?.toLongOrNull()

                if (!isBook && extId == null && cur.title.isNotBlank()) {
                    if (!isAnime && (src.equals("TMDB", ignoreCase = true) || isVideo)) {
                        val tmdbRes = if (cur.mediaType == MediaType.MOVIE) {
                            TmdbApiClient.searchMovieByTitle(cur.title, context).getOrNull()
                        } else {
                            TmdbApiClient.searchTvByTitle(cur.title, context).getOrNull()
                        }
                        extId = tmdbRes?.tmdbId
                    } else {
                        val jikanRes = JikanApiClient.searchJikanMultiple(cur.title, isManga, limit = 1).getOrNull()
                        extId = jikanRes?.firstOrNull()?.malId
                    }
                }

                val fetchResult: Result<List<RelatedMediaItem>> = when {
                    isBook -> {
                        BookMetadataManager.fetchRelatedBooks(cur, context)
                    }
                    isAnime || src.equals("MAL", ignoreCase = true) || src.equals("AniList", ignoreCase = true) -> {
                        JikanApiClient.fetchRelatedEntries(malId = extId, isManga = isManga, title = cur.title)
                    }
                    src.equals("TMDB", ignoreCase = true) || isVideo -> {
                        if (extId != null) {
                            TmdbApiClient.fetchRelatedEntries(extId, isTv = cur.mediaType == MediaType.SHOW, context)
                        } else {
                            Result.success(emptyList())
                        }
                    }
                    else -> {
                        JikanApiClient.fetchRelatedEntries(malId = extId, isManga = isManga, title = cur.title)
                    }
                }

                if (fetchResult.isSuccess) {
                    val results = fetchResult.getOrDefault(emptyList())
                    val filtered = results.filter { item ->
                        val isSelf = cur.matchesExternal(
                            candidateExternalId = item.externalId,
                            candidateExternalSource = item.externalSource,
                            candidateTitle = item.title,
                            candidateMediaType = item.mediaType,
                            candidateReleaseYear = item.releaseYear
                        )
                        !isSelf
                    }
                    _rawRelatedEntries.value = filtered
                    _relatedErrorMessage.value = null

                    val json = com.google.gson.Gson().toJson(filtered)
                    if (json != cur.relatedEntriesJson) {
                        repository.update(cur.copy(relatedEntriesJson = json, updatedAt = System.currentTimeMillis()))
                    }
                } else {
                    val ex = fetchResult.exceptionOrNull()
                    val isNotFound = ex is com.lx.tracklist.data.remote.NotFoundException ||
                            ex is com.lx.tracklist.data.remote.TmdbNotFoundException ||
                            ex?.message?.contains("not found", ignoreCase = true) == true ||
                            ex?.message?.contains("404", ignoreCase = true) == true

                    if (isNotFound) {
                        _rawRelatedEntries.value = emptyList()
                        _relatedErrorMessage.value = null
                        if (cur.relatedEntriesJson != "[]") {
                            repository.update(cur.copy(relatedEntriesJson = "[]", updatedAt = System.currentTimeMillis()))
                        }
                    } else {
                        _relatedErrorMessage.value = "Unable to load related entries. Tap to retry."
                    }
                }
            } catch (e: Exception) {
                val isNotFound = e is com.lx.tracklist.data.remote.NotFoundException ||
                        e is com.lx.tracklist.data.remote.TmdbNotFoundException ||
                        e.message?.contains("not found", ignoreCase = true) == true ||
                        e.message?.contains("404", ignoreCase = true) == true

                if (isNotFound) {
                    _rawRelatedEntries.value = emptyList()
                    _relatedErrorMessage.value = null
                    if (cur.relatedEntriesJson != "[]") {
                        repository.update(cur.copy(relatedEntriesJson = "[]", updatedAt = System.currentTimeMillis()))
                    }
                } else {
                    _relatedErrorMessage.value = "Failed to load related entries (${e.localizedMessage ?: "network error"}). Tap to retry."
                }
            } finally {
                _isLoadingRelated.value = false
            }
        }
    }

    fun retryLoadRelatedEntries(context: Context? = null) {
        val cur = entry.value ?: return
        loadedForEntryId = null
        _relatedErrorMessage.value = null
        loadRelatedEntries(cur.copy(relatedEntriesJson = null), context)
    }

    suspend fun addRelatedToLibrary(item: RelatedMediaItem, status: MediaStatus, context: Context? = null): Long {
        val targetUserId = entry.value?.userId ?: repository.activeUser.value.id
        return repository.addRelatedItem(item, status, context, targetUserId = targetUserId)
    }

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted

    private val _deletedEvent = Channel<Unit>(Channel.BUFFERED)
    val deletedEvent = _deletedEvent.receiveAsFlow()

    fun resetDeletedState() {
        _isDeleted.value = false
    }

    fun increment(andChangeStatusTo: MediaStatus? = null) {
        val cur = entry.value ?: return
        viewModelScope.launch {
            if (andChangeStatusTo != null) {
                val now = System.currentTimeMillis()
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val withStatus = cur.copy(
                    status = andChangeStatusTo,
                    startDate = if (andChangeStatusTo == MediaStatus.WATCHING && cur.startDate == null) todayStart else cur.startDate,
                    updatedAt = now
                )
                repository.incrementProgress(withStatus)
            } else {
                repository.incrementProgress(cur)
            }
        }
    }

    fun decrement() {
        val cur = entry.value ?: return
        viewModelScope.launch {
            repository.update(cur.decrementProgress())
        }
    }

    fun setProgress(newProgress: Int) {
        val cur = entry.value ?: return
        val clamped = max(0, newProgress)
        if (cur.hasSeasonEpisodeTracking()) {
            setShowProgress(cur.currentSeasons, clamped)
            return
        }
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val updated = when (cur.mediaType) {
                com.lx.tracklist.data.model.MediaType.ANIME, com.lx.tracklist.data.model.MediaType.SHOW -> {
                    val isDone = cur.totalEpisodes > 0 && clamped >= cur.totalEpisodes
                    val newStatus = if (isDone) com.lx.tracklist.data.model.MediaStatus.COMPLETED else cur.status
                    cur.copy(
                        currentEpisodes = clamped,
                        currentTimeMinutes = null,
                        status = newStatus,
                        finishDate = if (newStatus == com.lx.tracklist.data.model.MediaStatus.COMPLETED && cur.finishDate == null) now else cur.finishDate,
                        updatedAt = now
                    )
                }
                com.lx.tracklist.data.model.MediaType.MANGA, com.lx.tracklist.data.model.MediaType.MANHWA, com.lx.tracklist.data.model.MediaType.BOOK -> {
                    when (cur.trackingUnit) {
                        TrackingUnit.VOLUMES -> {
                            val isDone = cur.totalVolumes > 0 && clamped >= cur.totalVolumes
                            val newStatus = if (isDone) com.lx.tracklist.data.model.MediaStatus.COMPLETED else cur.status
                            cur.copy(
                                currentVolumes = clamped,
                                status = newStatus,
                                finishDate = if (newStatus == com.lx.tracklist.data.model.MediaStatus.COMPLETED && cur.finishDate == null) now else cur.finishDate,
                                updatedAt = now
                            )
                        }
                        TrackingUnit.PAGES -> {
                            val isDone = cur.totalPages > 0 && clamped >= cur.totalPages
                            val newStatus = if (isDone) com.lx.tracklist.data.model.MediaStatus.COMPLETED else cur.status
                            cur.copy(
                                currentPages = clamped,
                                status = newStatus,
                                finishDate = if (newStatus == com.lx.tracklist.data.model.MediaStatus.COMPLETED && cur.finishDate == null) now else cur.finishDate,
                                updatedAt = now
                            )
                        }
                        TrackingUnit.CHAPTERS, TrackingUnit.COMBINED -> {
                            val isDone = cur.totalChapters > 0 && clamped >= cur.totalChapters
                            val newStatus = if (isDone) com.lx.tracklist.data.model.MediaStatus.COMPLETED else cur.status
                            cur.copy(
                                currentChapters = clamped,
                                status = newStatus,
                                finishDate = if (newStatus == com.lx.tracklist.data.model.MediaStatus.COMPLETED && cur.finishDate == null) now else cur.finishDate,
                                updatedAt = now
                            )
                        }
                    }
                }
                com.lx.tracklist.data.model.MediaType.MOVIE -> {
                    val isDone = clamped > 0
                    val newStatus = if (isDone) com.lx.tracklist.data.model.MediaStatus.COMPLETED else cur.status
                    cur.copy(
                        status = newStatus,
                        finishDate = if (newStatus == com.lx.tracklist.data.model.MediaStatus.COMPLETED && cur.finishDate == null) now else cur.finishDate,
                        updatedAt = now
                    )
                }
            }
            repository.update(updated)
        }
    }

    fun setShowProgress(season: Int, episode: Int) {
        val cur = entry.value ?: return
        if (cur.mediaType != MediaType.SHOW) {
            setProgress(episode)
            return
        }
        val effectiveSeasons = cur.getEffectiveTotalSeasons()
        val targetSeason = season.coerceIn(1, effectiveSeasons)
        val targetEp = max(0, episode)
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val isDone = if (cur.hasSeasonEpisodeTracking()) {
                val seasonTotal = cur.getTotalEpisodesForSeason(targetSeason)
                targetSeason >= effectiveSeasons && seasonTotal > 0 && targetEp >= seasonTotal
            } else {
                cur.totalEpisodes > 0 && targetEp >= cur.totalEpisodes && targetSeason >= cur.totalSeasons
            }
            val newStatus = if (isDone) MediaStatus.COMPLETED else cur.status
            val updated = cur.copy(
                currentSeasons = targetSeason,
                currentEpisodes = targetEp,
                currentTimeMinutes = null,
                status = newStatus,
                finishDate = if (newStatus == MediaStatus.COMPLETED && cur.finishDate == null) now else cur.finishDate,
                updatedAt = now
            )
            repository.update(updated)
        }
    }

    fun toggleFavorite() {
        val cur = entry.value ?: return
        viewModelScope.launch {
            repository.toggleFavorite(cur)
        }
    }

    fun toggleExternalTracking() {
        val cur = entry.value ?: return
        viewModelScope.launch {
            repository.toggleExternalTracking(cur)
        }
    }

    fun setExternalTracking(isExternallyTracked: Boolean) {
        val cur = entry.value ?: return
        viewModelScope.launch {
            repository.setExternalTracking(cur, isExternallyTracked)
        }
    }

    fun updateStatus(newStatus: MediaStatus) {
        val cur = entry.value ?: return
        if (cur.status == newStatus) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val newStartDate = if (newStatus == MediaStatus.WATCHING && cur.startDate == null) {
                todayStart
            } else {
                cur.startDate
            }

            val newFinishDate = if (newStatus == MediaStatus.COMPLETED && cur.finishDate == null) {
                todayStart
            } else {
                cur.finishDate
            }

            val updated = cur.copy(
                status = newStatus,
                startDate = newStartDate,
                finishDate = newFinishDate,
                updatedAt = now
            )
            repository.update(updated)
        }
    }

    fun updateScore(newScore: Int?) {
        val cur = entry.value ?: return
        viewModelScope.launch {
            val updated = cur.copy(score = newScore, updatedAt = System.currentTimeMillis())
            repository.update(updated)
        }
    }

    fun startRewatch() {
        val cur = entry.value ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val updated = cur.copy(
                isRewatching = true,
                status = MediaStatus.WATCHING,
                currentSeasons = 1,
                currentEpisodes = 0,
                currentChapters = 0,
                currentVolumes = 0,
                currentPages = 0,
                startDate = now,
                finishDate = null,
                updatedAt = now
            )
            repository.update(updated)
        }
    }

    fun finishRewatch() {
        val cur = entry.value ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val targetSeason = if (cur.mediaType == MediaType.SHOW) cur.getEffectiveTotalSeasons() else cur.currentSeasons
            val targetEpisodes = if (cur.hasSeasonEpisodeTracking()) {
                val lastSeasonEps = cur.getTotalEpisodesForSeason(targetSeason)
                if (lastSeasonEps > 0) lastSeasonEps else cur.currentEpisodes
            } else {
                if (cur.totalEpisodes > 0) cur.totalEpisodes else cur.currentEpisodes
            }
            val updated = cur.copy(
                isRewatching = false,
                rewatchCount = cur.rewatchCount + 1,
                status = MediaStatus.COMPLETED,
                currentSeasons = targetSeason,
                currentEpisodes = targetEpisodes,
                currentChapters = if (cur.totalChapters > 0) cur.totalChapters else cur.currentChapters,
                currentVolumes = if (cur.totalVolumes > 0) cur.totalVolumes else cur.currentVolumes,
                finishDate = now,
                updatedAt = now
            )
            repository.update(updated)
        }
    }

    fun cancelRewatch() {
        val cur = entry.value ?: return
        viewModelScope.launch {
            val updated = cur.copy(
                isRewatching = false,
                updatedAt = System.currentTimeMillis()
            )
            repository.update(updated)
        }
    }

    fun deleteEntry() {
        val cur = entry.value ?: return
        viewModelScope.launch {
            repository.delete(cur)
            _isDeleted.value = true
            _deletedEvent.send(Unit)
        }
    }

    fun refreshMetadataFromExternalSource(context: Context) {
        val cur = entry.value ?: return
        if (_isRefreshingMetadata.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingMetadata.value = true
            try {
                if (cur.mediaType == MediaType.BOOK) {
                    val isbnFromNotes = cur.notes?.let { Regex("(?i)ISBN:\\s*([0-9Xx-]+)").find(it)?.groupValues?.get(1)?.replace("-", "")?.trim() }
                    val bookDetails: com.lx.tracklist.data.remote.BookDetails? = when {
                        cur.externalSource.equals("Google Books", ignoreCase = true) && !cur.externalId.isNullOrBlank() ->
                            GoogleBooksApiClient.fetchVolumeDetails(cur.externalId, context).getOrNull()
                        cur.externalSource.equals("Open Library", ignoreCase = true) && !cur.externalId.isNullOrBlank() ->
                            OpenLibraryApiClient.fetchWorkDetails(cur.externalId).getOrNull()
                        !isbnFromNotes.isNullOrBlank() ->
                            BookMetadataManager.searchBooks(isbnFromNotes, context, limit = 1).getOrNull()?.firstOrNull()
                        else ->
                            BookMetadataManager.searchBooks(cur.title, context, limit = 1).getOrNull()?.firstOrNull()
                    }

                    if (bookDetails != null) {
                        val now = System.currentTimeMillis()
                        val publishEpoch = bookDetails.publishedDate?.let { dateStr ->
                            try {
                                when (dateStr.length) {
                                    4 -> SimpleDateFormat("yyyy", Locale.US).parse(dateStr)?.time
                                    7 -> SimpleDateFormat("yyyy-MM", Locale.US).parse(dateStr)?.time
                                    else -> SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)?.time
                                }
                            } catch (_: Exception) { null }
                        }
                        val targetUnit = when {
                            (bookDetails.pageCount ?: 0) > 0 -> TrackingUnit.PAGES
                            (bookDetails.totalVolumes ?: 0) > 0 -> TrackingUnit.VOLUMES
                            (bookDetails.totalChapters ?: 0) > 0 -> TrackingUnit.CHAPTERS
                            else -> TrackingUnit.PAGES
                        }
                        val updated = cur.copy(
                            coverImageUri = if (!bookDetails.coverUrl.isNullOrBlank()) bookDetails.coverUrl else cur.coverImageUri,
                            alternateTitle = if (!bookDetails.subtitle.isNullOrBlank()) bookDetails.subtitle else cur.alternateTitle,
                            totalPages = if (bookDetails.pageCount != null && bookDetails.pageCount > 0) bookDetails.pageCount else cur.totalPages,
                            totalVolumes = if (bookDetails.totalVolumes != null && bookDetails.totalVolumes > 0) bookDetails.totalVolumes else cur.totalVolumes,
                            totalChapters = if (bookDetails.totalChapters != null && bookDetails.totalChapters > 0) bookDetails.totalChapters else cur.totalChapters,
                            trackingUnit = targetUnit,
                            airedStartDate = publishEpoch ?: cur.airedStartDate,
                            airedEndDate = publishEpoch ?: cur.airedEndDate ?: cur.airedStartDate,
                            externalScore = bookDetails.averageScore ?: cur.externalScore,
                            externalScoreSource = bookDetails.sourceProvider,
                            tags = bookDetails.buildTagsString().takeIf { it.isNotBlank() } ?: cur.tags,
                            notes = bookDetails.buildNotesWithMetadata().takeIf { it.isNotBlank() } ?: cur.notes,
                            externalId = bookDetails.id.takeIf { it.isNotBlank() } ?: cur.externalId,
                            externalSource = bookDetails.sourceProvider,
                            relatedEntriesJson = null,
                            updatedAt = now
                        )
                        repository.update(updated)
                        _metadataRefreshMessage.value = "Metadata refreshed from ${bookDetails.sourceProvider} (${bookDetails.title})"
                        loadRelatedEntries(updated, context)
                        return@launch
                    } else {
                        _metadataRefreshMessage.value = "No metadata returned from external book databases."
                        return@launch
                    }
                }

                val isTachiyomi = JikanSyncManager.isTachiyomiSource(cur)
                val hasValidId = JikanSyncManager.hasValidExternalId(cur)
                val needsResolution = isTachiyomi || !hasValidId || cur.externalId.isNullOrBlank() || TachiyomiResolverManager.isEligibleForResolution(cur)
                if (needsResolution) {
                    val outcome = TachiyomiResolverManager.matchAndResolveEntry(cur, repository, context)
                    when (outcome) {
                        is TachiyomiResolverManager.ResolutionOutcome.Success -> {
                            val r = outcome.report
                            _metadataRefreshMessage.value = "Resolved & linked to ${r.resolvedSource} (#${r.resolvedExternalId}): ${r.resolvedTitle}"
                        }
                        is TachiyomiResolverManager.ResolutionOutcome.Skipped -> {
                            _metadataRefreshMessage.value = "No confident exact match found for \"${cur.title}\""
                        }
                        is TachiyomiResolverManager.ResolutionOutcome.Failed -> {
                            _metadataRefreshMessage.value = "Failed to resolve: ${outcome.report.errorMessage}"
                        }
                    }
                    return@launch
                }

                suspend fun attemptFallbackResolution(failurePrefix: String) {
                    val fallbackOutcome = TachiyomiResolverManager.matchAndResolveEntry(
                        entry = cur,
                        repository = repository,
                        context = context,
                        skipExistingIdCheck = true
                    )
                    when (fallbackOutcome) {
                        is TachiyomiResolverManager.ResolutionOutcome.Success -> {
                            val r = fallbackOutcome.report
                            _metadataRefreshMessage.value = "Resolved via fallback to ${r.resolvedSource} (#${r.resolvedExternalId}): ${r.resolvedTitle}"
                        }
                        is TachiyomiResolverManager.ResolutionOutcome.Skipped -> {
                            _metadataRefreshMessage.value = failurePrefix
                        }
                        is TachiyomiResolverManager.ResolutionOutcome.Failed -> {
                            _metadataRefreshMessage.value = "$failurePrefix (${fallbackOutcome.report.errorMessage})"
                        }
                    }
                }

                val isMal = JikanSyncManager.isMalSource(cur) || isAnimeOrManga(cur)
                val extIdStr = cur.externalId.trim()
                val isManga = JikanSyncManager.isMangaType(cur.mediaType)
                val extId = extIdStr.toLongOrNull()
                val isMangaDex = cur.externalSource?.equals("MangaDex", ignoreCase = true) == true
                val isAniList = cur.externalSource?.equals("AniList", ignoreCase = true) == true
                val mangaDexIdToUse = if (isMangaDex) extIdStr else null

                if (isMangaDex && !mangaDexIdToUse.isNullOrBlank()) {
                    val result = MangaDexApiClient.fetchMangaDetails(mangaDexIdToUse)
                    if (result.isSuccess) {
                        val details = result.getOrNull()
                        if (details != null) {
                            val now = System.currentTimeMillis()
                            val mdId = details.url?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: mangaDexIdToUse
                            val updated = cur.copy(
                                externalId = mdId,
                                externalSource = "MangaDex",
                                coverImageUri = if (!details.posterUrl.isNullOrBlank()) details.posterUrl else cur.coverImageUri,
                                alternateTitle = if (!details.alternateTitle.isNullOrBlank()) details.alternateTitle else cur.alternateTitle,
                                totalChapters = if (details.totalChapters != null && details.totalChapters > 0) details.totalChapters else cur.totalChapters,
                                totalVolumes = if (details.totalVolumes != null && details.totalVolumes > 0) details.totalVolumes else cur.totalVolumes,
                                airedStartDate = details.airedStartDate ?: cur.airedStartDate,
                                airedEndDate = details.airedEndDate ?: cur.airedEndDate,
                                externalScore = details.externalScore ?: cur.externalScore,
                                externalScoreSource = details.externalScoreSource ?: "MangaDex",
                                tags = if (!details.tags.isNullOrBlank()) details.tags else cur.tags,
                                notes = if (!details.synopsis.isNullOrBlank()) details.synopsis else cur.notes,
                                isAiring = details.isAiring ?: cur.isAiring,
                                updatedAt = now
                            )
                            repository.update(updated)
                            _metadataRefreshMessage.value = "Metadata refreshed from MangaDex (${details.title})"
                        } else {
                            attemptFallbackResolution("No metadata returned from MangaDex")
                        }
                    } else {
                        val err = result.exceptionOrNull()
                        attemptFallbackResolution("Failed to refresh from MangaDex: ${err?.message ?: err?.localizedMessage ?: "Unknown error"}")
                    }
                } else if (isMal) {
                    val result = when {
                        isAniList && extId != null && extId > 0 ->
                            JikanApiClient.queryAniListById(aniListId = extId, isManga = isManga)
                        extId != null && extId > 0 ->
                            JikanApiClient.fetchMediaDetails(malId = extId, isManga = isManga, title = cur.title)
                        else ->
                            JikanApiClient.searchMediaDetailsByTitle(title = cur.title, isManga = isManga)
                    }

                    if (result.isSuccess) {
                        val details = result.getOrNull()
                        if (details != null) {
                            val now = System.currentTimeMillis()
                            val resolvedSource = when {
                                details.sourceProvider == "MangaDex" -> "MangaDex"
                                details.sourceProvider.startsWith("MAL") -> "MAL"
                                details.sourceProvider.contains("AniList") -> "AniList"
                                details.sourceProvider.equals("Kitsu", ignoreCase = true) -> "Kitsu"
                                else -> "MAL"
                            }
                            val newExtId = when {
                                details.sourceProvider == "MangaDex" -> {
                                    details.url?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: cur.externalId
                                }
                                details.sourceProvider.contains("AniList") -> {
                                    (details.malId?.takeIf { it > 0 } ?: details.url?.substringAfterLast('/')?.toLongOrNull())?.toString()
                                        ?: cur.externalId
                                }
                                details.sourceProvider.equals("Kitsu", ignoreCase = true) -> {
                                    details.malId?.toString() ?: details.url?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                                        ?: cur.externalId
                                }
                                else -> {
                                    (details.malId ?: extId)?.toString() ?: cur.externalId
                                }
                            }
                            val updated = cur.copy(
                                externalId = newExtId,
                                externalSource = resolvedSource,
                                coverImageUri = if (!details.posterUrl.isNullOrBlank()) details.posterUrl else cur.coverImageUri,
                                alternateTitle = if (!details.alternateTitle.isNullOrBlank()) details.alternateTitle else cur.alternateTitle,
                                totalEpisodes = if (details.totalEpisodes != null && details.totalEpisodes > 0) details.totalEpisodes else cur.totalEpisodes,
                                totalChapters = if (details.totalChapters != null && details.totalChapters > 0) details.totalChapters else cur.totalChapters,
                                totalVolumes = if (details.totalVolumes != null && details.totalVolumes > 0) details.totalVolumes else cur.totalVolumes,
                                episodeLengthMinutes = if (details.durationMinutes != null && details.durationMinutes > 0) details.durationMinutes else cur.episodeLengthMinutes,
                                durationMinutes = if (details.durationMinutes != null && details.durationMinutes > 0) details.durationMinutes else cur.durationMinutes,
                                airedStartDate = details.airedStartDate ?: cur.airedStartDate,
                                airedEndDate = details.airedEndDate ?: cur.airedEndDate,
                                externalScore = details.externalScore ?: cur.externalScore,
                                externalScoreSource = details.externalScoreSource ?: resolvedSource,
                                tags = if (!details.tags.isNullOrBlank()) details.tags else cur.tags,
                                notes = if (!details.synopsis.isNullOrBlank()) details.synopsis else cur.notes,
                                isAiring = details.isAiring ?: cur.isAiring,
                                updatedAt = now
                            )
                            repository.update(updated)
                            _metadataRefreshMessage.value = "Metadata refreshed from $resolvedSource (#$newExtId)"
                        } else {
                            attemptFallbackResolution("No metadata returned from external source")
                        }
                    } else {
                        val err = result.exceptionOrNull()
                        attemptFallbackResolution("Failed to refresh: ${err?.message ?: err?.localizedMessage ?: "Unknown error"}")
                    }
                } else {
                    // TMDB for Movie / Show
                    val isTv = cur.mediaType == MediaType.SHOW
                    val result = if (extId != null) {
                        TmdbApiClient.fetchMediaDetails(tmdbId = extId, isTv = isTv, context = context)
                    } else {
                        TmdbApiClient.searchMediaDetailsByTitle(title = cur.title, isTv = isTv, context = context)
                    }

                    if (result.isSuccess) {
                        val details = result.getOrNull()
                        if (details != null) {
                            val now = System.currentTimeMillis()
                            val updated = cur.copy(
                                externalId = details.tmdbId.toString(),
                                externalSource = "TMDB",
                                coverImageUri = if (!details.posterUrl.isNullOrBlank()) details.posterUrl else cur.coverImageUri,
                                alternateTitle = if (!details.alternateTitle.isNullOrBlank()) details.alternateTitle else cur.alternateTitle,
                                totalEpisodes = if (details.totalEpisodes != null && details.totalEpisodes > 0) details.totalEpisodes else cur.totalEpisodes,
                                totalSeasons = if (isTv && details.totalSeasons != null && details.totalSeasons > 0) details.totalSeasons else cur.totalSeasons,
                                seasonEpisodesJson = if (cur.mediaType == MediaType.SHOW && details.seasonEpisodes != null) {
                                    MediaEntry.formatSeasonEpisodes(details.seasonEpisodes) ?: cur.seasonEpisodesJson
                                } else {
                                    cur.seasonEpisodesJson
                                },
                                durationMinutes = if (details.durationMinutes != null && details.durationMinutes > 0) details.durationMinutes else cur.durationMinutes,
                                episodeLengthMinutes = if (details.durationMinutes != null && details.durationMinutes > 0) details.durationMinutes else cur.episodeLengthMinutes,
                                airedStartDate = details.airedStartDate ?: cur.airedStartDate,
                                airedEndDate = details.airedEndDate ?: cur.airedEndDate,
                                externalScore = details.externalScore ?: cur.externalScore,
                                externalScoreSource = "TMDB",
                                tags = if (!details.tags.isNullOrBlank()) details.tags else cur.tags,
                                notes = if (!details.synopsis.isNullOrBlank()) details.synopsis else cur.notes,
                                isAiring = details.isAiring ?: cur.isAiring,
                                updatedAt = now
                            )
                            repository.update(updated)
                            _metadataRefreshMessage.value = "Metadata refreshed from TMDB (TMDB #${details.tmdbId})"
                        } else {
                            attemptFallbackResolution("No metadata returned from The Movie Database")
                        }
                    } else {
                        val err = result.exceptionOrNull()
                        attemptFallbackResolution("Failed to refresh from TMDB: ${err?.message ?: err?.localizedMessage ?: "Unknown error"}")
                    }
                }
            } catch (e: Exception) {
                _metadataRefreshMessage.value = "Error refreshing metadata: ${e.message ?: e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isRefreshingMetadata.value = false
            }
        }
    }
}

class DetailViewModelFactory(
    private val repository: MediaRepository,
    private val entryId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(repository, entryId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
