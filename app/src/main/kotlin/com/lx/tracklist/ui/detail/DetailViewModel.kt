package com.lx.tracklist.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.repository.MediaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
