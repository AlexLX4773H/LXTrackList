package com.lx.tracklist.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.repository.MediaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ScoreDistributionData(
    val categoryName: String,
    val scoredCount: Int,
    val meanScore: Double,
    val distribution: Map<Int, Int>, // score (1..10) -> count
    val maxFrequency: Int,
    val totalCategoryEntries: Int
)

fun calculateScoreDistribution(
    entries: List<MediaEntry>,
    categoryKey: String? = null,
    completedOnly: Boolean = false
): ScoreDistributionData {
    val isStandardMediaType = MediaType.entries.any { it.name.equals(categoryKey, ignoreCase = true) }
    val categoryFiltered = if (categoryKey == null || categoryKey.equals("ALL", ignoreCase = true)) {
        entries
    } else if (isStandardMediaType) {
        entries.filter { entry ->
            entry.mediaType.name.equals(categoryKey, ignoreCase = true) && entry.customCategory == null
        }
    } else {
        entries.filter { entry ->
            entry.customCategory != null && entry.customCategory.equals(categoryKey, ignoreCase = true)
        }
    }

    val totalInCat = categoryFiltered.size

    val targetEntries = if (completedOnly) {
        categoryFiltered.filter { it.status == MediaStatus.COMPLETED }
    } else {
        categoryFiltered
    }

    val scored = targetEntries.filter { it.score != null && it.score in 1..10 }
    val scoredCount = scored.size
    val mean = if (scoredCount > 0) scored.map { it.score!! }.average() else 0.0

    val dist = (1..10).associateWith { 0 }.toMutableMap()
    scored.forEach { entry ->
        val s = entry.score!!
        dist[s] = (dist[s] ?: 0) + 1
    }

    val maxFreq = dist.values.maxOrNull() ?: 0

    return ScoreDistributionData(
        categoryName = categoryKey ?: "All",
        scoredCount = scoredCount,
        meanScore = mean,
        distribution = dist,
        maxFrequency = maxFreq,
        totalCategoryEntries = totalInCat
    )
}

fun getScoreLabel(score: Int): String = when (score) {
    10 -> "Masterpiece"
    9 -> "Great"
    8 -> "Very Good"
    7 -> "Good"
    6 -> "Fine"
    5 -> "Average"
    4 -> "Bad"
    3 -> "Very Bad"
    2 -> "Horrible"
    1 -> "Appalling"
    else -> ""
}

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
    val totalVolumesRead: Int = 0,
    val allEntries: List<MediaEntry> = emptyList(),
    val customCategories: List<CustomCategory> = emptyList()
)

class StatsViewModel(private val repository: MediaRepository) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        repository.getAllEntries(),
        repository.customCategories
    ) { entries, customCats ->
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
            totalVolumesRead = totalVol,
            allEntries = entries,
            customCategories = customCats
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
