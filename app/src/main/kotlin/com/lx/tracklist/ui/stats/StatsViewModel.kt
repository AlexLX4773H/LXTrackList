package com.lx.tracklist.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.repository.MediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val totalEntries: Int = 0,
    val completedCount: Int = 0,
    val inProgressCount: Int = 0,
    val planToCount: Int = 0,
    val averageScore: Double = 0.0,
    val totalFavorites: Int = 0,
    val categoryCounts: Map<MediaType, Int> = emptyMap(),
    val statusCounts: Map<MediaStatus, Int> = emptyMap(),
    val totalEpisodesWatched: Int = 0,
    val totalChaptersRead: Int = 0,
    val totalVolumesRead: Int = 0
)

class StatsViewModel(private val repository: MediaRepository) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = repository.getAllEntries().map { entries ->
        val total = entries.size
        val completed = entries.count { it.status == MediaStatus.COMPLETED }
        val inProgress = entries.count { it.status == MediaStatus.WATCHING }
        val planTo = entries.count { it.status == MediaStatus.PLAN_TO_WATCH }
        val favs = entries.count { it.isFavorite }

        val scoredEntries = entries.filter { it.score != null && it.score > 0 }
        val avgScore = if (scoredEntries.isNotEmpty()) {
            scoredEntries.map { it.score!! }.average()
        } else 0.0

        val catCounts = MediaType.entries.associateWith { cat ->
            entries.count { it.mediaType == cat }
        }

        val stCounts = MediaStatus.entries.associateWith { st ->
            entries.count { it.status == st }
        }

        val totalEp = entries.sumOf { it.currentEpisodes }
        val totalCh = entries.sumOf { it.currentChapters }
        val totalVol = entries.sumOf { it.currentVolumes }

        StatsUiState(
            totalEntries = total,
            completedCount = completed,
            inProgressCount = inProgress,
            planToCount = planTo,
            averageScore = avgScore,
            totalFavorites = favs,
            categoryCounts = catCounts,
            statusCounts = stCounts,
            totalEpisodesWatched = totalEp,
            totalChaptersRead = totalCh,
            totalVolumesRead = totalVol
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )
}

class StatsViewModelFactory(private val repository: MediaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
