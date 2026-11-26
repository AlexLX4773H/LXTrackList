package com.lx.tracklist.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.effectiveMediaType
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

data class CategoryStatSummary(
    val categoryKey: String,
    val displayName: String,
    val isVideo: Boolean,
    val isGame: Boolean = false,
    val isMusic: Boolean = false,
    val daysSpent: Double,
    val daysRemaining: Double = 0.0,
    val totalDays: Double = daysSpent + daysRemaining,
    val meanScore: Double,
    val scoredCount: Int,
    val totalEntries: Int,
    val rewatchedCount: Int,
    val statusCounts: Map<MediaStatus, Int>,
    val episodesCount: Int,
    val chaptersCount: Int,
    val volumesCount: Int,
    val favoritesCount: Int
) {
    fun getStatusLabel(status: MediaStatus): String {
        val representativeType = when {
            isMusic -> MediaType.MUSIC
            isGame -> MediaType.GAME
            isVideo -> MediaType.ANIME
            else -> MediaType.MANGA
        }
        return status.getDisplayLabel(representativeType)
    }
}

const val STATS_CUSTOM_KEY_PREFIX = "custom:"

/** Builds a non-colliding Stats chip key for a custom category. */
fun customStatsKey(name: String): String = "$STATS_CUSTOM_KEY_PREFIX$name"

private fun parseCustomStatsName(categoryKey: String?): String? =
    categoryKey?.takeIf { it.startsWith(STATS_CUSTOM_KEY_PREFIX) }
        ?.removePrefix(STATS_CUSTOM_KEY_PREFIX)

private fun findStandardMediaType(categoryKey: String?): MediaType? {
    if (categoryKey.isNullOrBlank() || categoryKey.startsWith(STATS_CUSTOM_KEY_PREFIX)) return null
    return MediaType.entries.firstOrNull { it.name.equals(categoryKey, ignoreCase = true) }
}

fun belongsToBuiltInCategoryBucket(
    entry: MediaEntry,
    targetType: MediaType,
    activeCustomNamesLower: Set<String>
): Boolean {
    if (entry.mediaType != targetType) return false
    val customName = entry.customCategory?.trim()?.takeIf { it.isNotEmpty() } ?: return true
    return customName.lowercase() !in activeCustomNamesLower
}

fun calculateBuiltInCategoryCounts(
    entries: List<MediaEntry>,
    customCategories: List<CustomCategory>
): Map<MediaType, Int> {
    val activeCustomNamesLower = customCategories.map { it.name.trim().lowercase() }.toSet()
    return MediaType.entries.associateWith { cat ->
        entries.count { belongsToBuiltInCategoryBucket(it, cat, activeCustomNamesLower) }
    }
}

fun countEntriesForCustomCategory(
    entries: List<MediaEntry>,
    customCategoryName: String
): Int {
    val cleanTarget = customCategoryName.trim()
    return entries.count {
        it.customCategory?.trim()?.equals(cleanTarget, ignoreCase = true) == true
    }
}

fun calculateCategoryStatSummary(
    entries: List<MediaEntry>,
    categoryKey: String,
    customCategories: List<CustomCategory>
): CategoryStatSummary {
    val explicitCustomName = parseCustomStatsName(categoryKey)
    val standardType = findStandardMediaType(categoryKey)
    val isAll = categoryKey.equals("ALL", ignoreCase = true)
    val activeCustomNamesLower = customCategories.map { it.name.trim().lowercase() }.toSet()

    val filteredEntries = when {
        isAll -> entries
        explicitCustomName != null -> {
            val cleanCustom = explicitCustomName.trim()
            entries.filter { it.customCategory?.trim()?.equals(cleanCustom, ignoreCase = true) == true }
        }
        standardType != null -> {
            entries.filter { belongsToBuiltInCategoryBucket(it, standardType, activeCustomNamesLower) }
        }
        else -> {
            val cleanKey = categoryKey.trim()
            entries.filter { it.customCategory?.trim()?.equals(cleanKey, ignoreCase = true) == true }
        }
    }

    val total = filteredEntries.size
    val statusMap = MediaStatus.entries.associateWith { st ->
        filteredEntries.count { it.status == st }
    }

    val scored = filteredEntries.filter { it.score != null && it.score > 0 }
    val mean = if (scored.isNotEmpty()) scored.map { it.score!! }.average() else 0.0

    val rewatch = filteredEntries.sumOf { it.rewatchCount }
    val episodes = filteredEntries.sumOf { it.getTotalWatchedEpisodes() }
    val chapters = filteredEntries.sumOf { it.currentChapters }
    val volumes = filteredEntries.sumOf { it.currentVolumes }
    val favs = filteredEntries.count { it.isFavorite }

    val customNameLookup = explicitCustomName ?: if (standardType == null && !isAll) categoryKey else null
    val customCategory = customNameLookup?.let { name ->
        customCategories.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
    val customEffectiveType = customCategory?.effectiveMediaType()

    val isVideo = when {
        isAll -> episodes >= chapters
        customNameLookup != null -> {
            if (customEffectiveType != null) {
                customEffectiveType == MediaType.ANIME || customEffectiveType == MediaType.SHOW || customEffectiveType == MediaType.MOVIE
            } else {
                customCategory?.isWatchType ?: true
            }
        }
        standardType != null -> standardType == MediaType.ANIME || standardType == MediaType.SHOW || standardType == MediaType.MOVIE
        else -> true
    }

    val isGame = when {
        isAll -> false
        customNameLookup != null -> {
            customEffectiveType == MediaType.GAME ||
                (customEffectiveType == null && (customNameLookup.equals(MediaType.GAME.name, ignoreCase = true) || customNameLookup.equals("Games", ignoreCase = true)))
        }
        standardType != null -> standardType == MediaType.GAME
        else -> false
    }

    val isMusic = when {
        isAll -> false
        customNameLookup != null -> {
            customEffectiveType == MediaType.MUSIC ||
                (customEffectiveType == null && customNameLookup.equals(MediaType.MUSIC.name, ignoreCase = true))
        }
        standardType != null -> standardType == MediaType.MUSIC
        else -> false
    }

    var totalMinutes = 0.0
    var remainingMinutes = 0.0

    filteredEntries.forEach { entry ->
        totalMinutes += entry.calculateSpentTimeMinutes()
        remainingMinutes += entry.calculateRemainingTimeMinutes()
    }
    val days = totalMinutes / 1440.0
    val daysRem = remainingMinutes / 1440.0

    val displayName = when {
        isAll -> "All Categories"
        customNameLookup != null -> customCategory?.name ?: customNameLookup
        standardType != null -> standardType.displayName
        else -> categoryKey
    }

    return CategoryStatSummary(
        categoryKey = categoryKey,
        displayName = displayName,
        isVideo = isVideo,
        isGame = isGame,
        isMusic = isMusic,
        daysSpent = days,
        daysRemaining = daysRem,
        totalDays = days + daysRem,
        meanScore = mean,
        scoredCount = scored.size,
        totalEntries = total,
        rewatchedCount = rewatch,
        statusCounts = statusMap,
        episodesCount = episodes,
        chaptersCount = chapters,
        volumesCount = volumes,
        favoritesCount = favs
    )
}

fun calculateScoreDistribution(
    entries: List<MediaEntry>,
    categoryKey: String? = null,
    completedOnly: Boolean = false
): ScoreDistributionData {
    val explicitCustomName = parseCustomStatsName(categoryKey)
    val standardType = findStandardMediaType(categoryKey)
    val isAll = categoryKey == null || categoryKey.equals("ALL", ignoreCase = true)

    val categoryFiltered = when {
        isAll -> entries
        explicitCustomName != null -> {
            entries.filter { entry ->
                entry.customCategory != null && entry.customCategory.equals(explicitCustomName, ignoreCase = true)
            }
        }
        standardType != null -> {
            entries.filter { entry ->
                entry.mediaType == standardType && entry.customCategory == null
            }
        }
        else -> {
            entries.filter { entry ->
                entry.customCategory != null && entry.customCategory.equals(categoryKey, ignoreCase = true)
            }
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
        categoryName = explicitCustomName ?: categoryKey ?: "All",
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

        val catCounts = calculateBuiltInCategoryCounts(entries, customCats)

        val stCounts = MediaStatus.entries.associateWith { st ->
            entries.count { it.status == st }
        }

        val totalEp = entries.sumOf { it.getTotalWatchedEpisodes() }
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
