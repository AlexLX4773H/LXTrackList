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
                    loadRelatedEntries(currentEntry)
                }
            }
        }
    }

    fun loadRelatedEntries(cur: MediaEntry, context: Context? = null) {
        if (loadedForEntryId == cur.id && _rawRelatedEntries.value.isNotEmpty()) return
        loadedForEntryId = cur.id
        _isLoadingRelated.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val src = cur.externalSource ?: ""
                var extId = cur.externalId?.toLongOrNull()

                if (extId == null && cur.title.isNotBlank()) {
                    if (src.equals("TMDB", ignoreCase = true) || cur.mediaType == MediaType.MOVIE || cur.mediaType == MediaType.SHOW) {
                        val tmdbRes = if (cur.mediaType == MediaType.MOVIE) {
                            TmdbApiClient.searchMovieByTitle(cur.title, context).getOrNull()
                        } else {
                            TmdbApiClient.searchTvByTitle(cur.title, context).getOrNull()
                        }
                        extId = tmdbRes?.tmdbId
                    } else {
                        val isManga = cur.mediaType == MediaType.MANGA ||
                                cur.mediaType == MediaType.MANHWA ||
                                cur.mediaType == MediaType.BOOK
                        val jikanRes = JikanApiClient.searchJikanMultiple(cur.title, isManga, limit = 1).getOrNull()
                        extId = jikanRes?.firstOrNull()?.malId
                    }
                }

                val results = if ((src.equals("TMDB", ignoreCase = true) || cur.mediaType == MediaType.MOVIE || cur.mediaType == MediaType.SHOW) && extId != null) {
                    TmdbApiClient.fetchRelatedEntries(extId, isTv = cur.mediaType == MediaType.SHOW, context).getOrDefault(emptyList())
                } else if (extId != null) {
                    val isManga = cur.mediaType == MediaType.MANGA ||
                            cur.mediaType == MediaType.MANHWA ||
                            cur.mediaType == MediaType.BOOK
                    JikanApiClient.fetchRelatedEntries(extId, isManga).getOrDefault(emptyList())
                } else {
                    emptyList()
                }

                _rawRelatedEntries.value = results.filter { item ->
                    val isSameExt = !cur.externalId.isNullOrBlank() && item.externalId.equals(cur.externalId, ignoreCase = true)
                    val isSameTitle = item.title.equals(cur.title, ignoreCase = true) && item.mediaType == cur.mediaType
                    !isSameExt && !isSameTitle
                }
            } catch (_: Exception) {
                _rawRelatedEntries.value = emptyList()
            } finally {
                _isLoadingRelated.value = false
            }
        }
    }

    suspend fun addRelatedToLibrary(item: RelatedMediaItem, status: MediaStatus, context: Context? = null): Long {
        return withContext(Dispatchers.IO) {
            val entry = if (item.externalSource == "TMDB") {
                val tmdbId = item.externalId.toLongOrNull() ?: 0L
                if (item.mediaType == MediaType.MOVIE) {
                    val fullDetails = TmdbApiClient.fetchMovieDetails(tmdbId, context).getOrNull()
                    MediaEntry(
                        title = fullDetails?.title ?: item.title,
                        alternateTitle = fullDetails?.alternateTitle,
                        mediaType = MediaType.MOVIE,
                        status = status,
                        externalId = item.externalId,
                        externalSource = "TMDB",
                        externalScore = fullDetails?.externalScore ?: item.score,
                        externalScoreSource = "TMDB",
                        coverImageUri = fullDetails?.posterUrl ?: item.posterUrl,
                        durationMinutes = fullDetails?.durationMinutes,
                        tags = fullDetails?.tags,
                        notes = fullDetails?.synopsis,
                        airedStartDate = fullDetails?.airedStartDate
                    )
                } else {
                    val fullDetails = TmdbApiClient.fetchTvDetails(tmdbId, context).getOrNull()
                    MediaEntry(
                        title = fullDetails?.title ?: item.title,
                        alternateTitle = fullDetails?.alternateTitle,
                        mediaType = MediaType.SHOW,
                        status = status,
                        externalId = item.externalId,
                        externalSource = "TMDB",
                        externalScore = fullDetails?.externalScore ?: item.score,
                        externalScoreSource = "TMDB",
                        coverImageUri = fullDetails?.posterUrl ?: item.posterUrl,
                        totalEpisodes = fullDetails?.totalEpisodes ?: 0,
                        totalSeasons = fullDetails?.totalSeasons ?: 1,
                        episodeLengthMinutes = fullDetails?.durationMinutes,
                        tags = fullDetails?.tags,
                        notes = fullDetails?.synopsis,
                        airedStartDate = fullDetails?.airedStartDate,
                        isAiring = fullDetails?.isAiring ?: false
                    )
                }
            } else {
                val malId = item.externalId.toLongOrNull() ?: 0L
                val isManga = item.mediaType == MediaType.MANGA || item.mediaType == MediaType.MANHWA || item.mediaType == MediaType.BOOK
                val details = JikanApiClient.fetchMediaDetails(malId, isManga).getOrNull()
                val isVideo = item.mediaType == MediaType.ANIME
                MediaEntry(
                    title = details?.title ?: item.title,
                    alternateTitle = details?.alternateTitle,
                    mediaType = item.mediaType,
                    status = status,
                    externalId = item.externalId,
                    externalSource = "MAL",
                    externalScore = details?.externalScore ?: item.score,
                    externalScoreSource = "MAL",
                    coverImageUri = details?.posterUrl ?: item.posterUrl,
                    totalEpisodes = if (isVideo) (details?.totalEpisodes ?: 0) else 0,
                    totalChapters = if (!isVideo) (details?.totalChapters ?: 0) else 0,
                    totalVolumes = if (!isVideo) (details?.totalVolumes ?: 0) else 0,
                    trackingUnit = TrackingUnit.CHAPTERS,
                    durationMinutes = details?.durationMinutes,
                    episodeLengthMinutes = details?.durationMinutes,
                    tags = details?.tags,
                    notes = details?.synopsis,
                    airedStartDate = details?.airedStartDate,
                    airedEndDate = details?.airedEndDate,
                    isAiring = details?.isAiring ?: false
                )
            }
            repository.insert(entry)
        }
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
