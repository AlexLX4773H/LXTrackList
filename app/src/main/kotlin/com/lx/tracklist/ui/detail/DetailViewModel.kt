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
import com.lx.tracklist.data.remote.JikanApiClient
import com.lx.tracklist.data.remote.JikanSyncManager
import com.lx.tracklist.data.remote.TmdbApiClient
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
import java.util.Calendar
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

    val relatedEntries: StateFlow<List<RelatedMediaItem>> = combine(
        _rawRelatedEntries,
        repository.getAllEntries()
    ) { related, allEntries ->
        related.map { item ->
            val match = allEntries.firstOrNull { entry ->
                val sameExt = !entry.externalId.isNullOrBlank() &&
                        entry.externalId.equals(item.externalId, ignoreCase = true) &&
                        entry.externalSource?.equals(item.externalSource, ignoreCase = true) == true
                val sameTitle = entry.title.equals(item.title, ignoreCase = true) && entry.mediaType == item.mediaType
                sameExt || sameTitle
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
        if (localRelated.isNotEmpty()) {
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
                val isManga = cur.mediaType == MediaType.MANGA ||
                        cur.mediaType == MediaType.MANHWA ||
                        cur.mediaType == MediaType.BOOK
                val src = cur.externalSource ?: ""
                var extId = cur.externalId?.toLongOrNull()

                if (extId == null && cur.title.isNotBlank()) {
                    if (!isAnime && (src.equals("TMDB", ignoreCase = true) || cur.mediaType == MediaType.MOVIE || cur.mediaType == MediaType.SHOW)) {
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

                val fetchResult: Result<List<RelatedMediaItem>> = if (isAnime || src.equals("MAL", ignoreCase = true) || src.equals("AniList", ignoreCase = true)) {
                    JikanApiClient.fetchRelatedEntries(malId = extId, isManga = isManga, title = cur.title)
                } else if ((src.equals("TMDB", ignoreCase = true) || cur.mediaType == MediaType.MOVIE || cur.mediaType == MediaType.SHOW) && extId != null) {
                    val tmdbRes = TmdbApiClient.fetchRelatedEntries(extId, isTv = cur.mediaType == MediaType.SHOW, context)
                    if (tmdbRes.isFailure) {
                        JikanApiClient.fetchRelatedEntries(malId = null, isManga = false, title = cur.title)
                    } else {
                        tmdbRes
                    }
                } else {
                    JikanApiClient.fetchRelatedEntries(malId = extId, isManga = isManga, title = cur.title)
                }

                if (fetchResult.isSuccess) {
                    val results = fetchResult.getOrDefault(emptyList())
                    val filtered = results.filter { item ->
                        val isSameExt = !cur.externalId.isNullOrBlank() && item.externalId.equals(cur.externalId, ignoreCase = true)
                        val isSameTitle = item.title.equals(cur.title, ignoreCase = true) && item.mediaType == cur.mediaType
                        !isSameExt && !isSameTitle
                    }
                    _rawRelatedEntries.value = filtered
                    _relatedErrorMessage.value = null

                    if (filtered.isNotEmpty()) {
                        val json = com.google.gson.Gson().toJson(filtered)
                        if (json != cur.relatedEntriesJson) {
                            repository.update(cur.copy(relatedEntriesJson = json, updatedAt = System.currentTimeMillis()))
                        }
                    }
                } else {
                    val err = fetchResult.exceptionOrNull()?.localizedMessage ?: "Failed to load related entries"
                    _relatedErrorMessage.value = "Unable to load related entries. Tap to retry."
                }
            } catch (e: Exception) {
                _relatedErrorMessage.value = "Failed to load related entries (${e.localizedMessage ?: "network error"}). Tap to retry."
            } finally {
                _isLoadingRelated.value = false
            }
        }
    }

    fun retryLoadRelatedEntries(context: Context? = null) {
        val cur = entry.value ?: return
        loadedForEntryId = null
        _relatedErrorMessage.value = null
        loadRelatedEntries(cur, context)
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

    fun increment() {
        val cur = entry.value ?: return
        viewModelScope.launch {
            repository.incrementProgress(cur)
        }
    }

    fun decrement() {
        val cur = entry.value ?: return
        viewModelScope.launch {
            val updated = when (cur.mediaType) {
                com.lx.tracklist.data.model.MediaType.ANIME, com.lx.tracklist.data.model.MediaType.SHOW -> {
                    cur.copy(currentEpisodes = max(0, cur.currentEpisodes - 1), updatedAt = System.currentTimeMillis())
                }
                com.lx.tracklist.data.model.MediaType.MANGA, com.lx.tracklist.data.model.MediaType.MANHWA, com.lx.tracklist.data.model.MediaType.BOOK -> {
                    when (cur.trackingUnit) {
                        TrackingUnit.VOLUMES -> cur.copy(currentVolumes = max(0, cur.currentVolumes - 1), updatedAt = System.currentTimeMillis())
                        TrackingUnit.PAGES -> cur.copy(currentPages = max(0, cur.currentPages - 10), updatedAt = System.currentTimeMillis())
                        TrackingUnit.CHAPTERS, TrackingUnit.COMBINED -> cur.copy(currentChapters = max(0, cur.currentChapters - 1), updatedAt = System.currentTimeMillis())
                    }
                }
                com.lx.tracklist.data.model.MediaType.MOVIE -> cur
            }
            repository.update(updated)
        }
    }

    fun setProgress(newProgress: Int) {
        val cur = entry.value ?: return
        val clamped = max(0, newProgress)
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val updated = when (cur.mediaType) {
                com.lx.tracklist.data.model.MediaType.ANIME, com.lx.tracklist.data.model.MediaType.SHOW -> {
                    val isDone = cur.totalEpisodes > 0 && clamped >= cur.totalEpisodes
                    val newStatus = if (isDone) com.lx.tracklist.data.model.MediaStatus.COMPLETED else cur.status
                    cur.copy(
                        currentEpisodes = clamped,
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

    fun toggleFavorite() {
        val cur = entry.value ?: return
        viewModelScope.launch {
            repository.toggleFavorite(cur)
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
            val updated = cur.copy(
                isRewatching = false,
                rewatchCount = cur.rewatchCount + 1,
                status = MediaStatus.COMPLETED,
                currentEpisodes = if (cur.totalEpisodes > 0) cur.totalEpisodes else cur.currentEpisodes,
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

        viewModelScope.launch {
            _isRefreshingMetadata.value = true
            try {
                val isMal = JikanSyncManager.isMalSource(cur) || isAnimeOrManga(cur)
                val extId = cur.externalId?.trim()?.toLongOrNull()

                if (isMal) {
                    val isManga = JikanSyncManager.isMangaType(cur.mediaType)
                    val result = if (extId != null) {
                        JikanApiClient.fetchMediaDetails(malId = extId, isManga = isManga)
                    } else {
                        JikanApiClient.searchMediaDetailsByTitle(title = cur.title, isManga = isManga)
                    }

                    if (result.isSuccess) {
                        val details = result.getOrNull()
                        if (details != null) {
                            val now = System.currentTimeMillis()
                            val newExtId = (details.malId ?: extId)?.toString() ?: cur.externalId
                            val updated = cur.copy(
                                externalId = newExtId,
                                externalSource = "MAL",
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
                                externalScoreSource = details.externalScoreSource ?: "MAL",
                                tags = if (!details.tags.isNullOrBlank()) details.tags else cur.tags,
                                notes = if (!details.synopsis.isNullOrBlank()) details.synopsis else cur.notes,
                                isAiring = details.isAiring ?: cur.isAiring,
                                updatedAt = now
                            )
                            repository.update(updated)
                            _metadataRefreshMessage.value = "Metadata refreshed from MyAnimeList (MAL #${newExtId})"
                        } else {
                            _metadataRefreshMessage.value = "No metadata returned from MyAnimeList"
                        }
                    } else {
                        _metadataRefreshMessage.value = "Failed to refresh from MAL: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}"
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
                            _metadataRefreshMessage.value = "No metadata returned from The Movie Database"
                        }
                    } else {
                        _metadataRefreshMessage.value = "Failed to refresh from TMDB: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}"
                    }
                }
            } catch (e: Exception) {
                _metadataRefreshMessage.value = "Error refreshing metadata: ${e.localizedMessage ?: "Unknown error"}"
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
