package com.lx.tracklist.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@Entity(
    tableName = "media_entries",
    indices = [
        Index("mediaType"),
        Index("status"),
        Index("isFavorite"),
        Index("isExternallyTracked"),
        Index("title"),
        Index("userId"),
        Index("customCategory"),
        Index("subCategory"),
        Index("creator"),
        Index(value = ["externalId", "externalSource"])
    ]
)
data class MediaEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // User profile association
    val userId: Long = 0L,

    // Custom Category override (Top-level custom category, e.g. "Donghua", "Web Novels")
    val customCategory: String? = null,

    // Sub-category (e.g. "Isekai", "Murim", specific to this category)
    val subCategory: String? = null,

    // Common fields
    val title: String,
    val alternateTitle: String? = null,
    val mediaType: MediaType,
    val status: MediaStatus,
    val score: Int? = null,
    val externalScore: Float? = null,
    val externalScoreSource: String? = null,
    // User personal viewing / reading dates
    val startDate: Long? = null,
    val finishDate: Long? = null,

    // Official media release / broadcast dates (when the show / manga aired / published)
    val airedStartDate: Long? = null,
    val airedEndDate: Long? = null,

    @ColumnInfo(name = "notes")
    @SerializedName(value = "description", alternate = ["notes"])
    val description: String? = null,
    val tags: String? = null,
    val coverImageUri: String? = null,
    val priority: Priority = Priority.MEDIUM,
    val rewatchCount: Int = 0,
    val isRewatching: Boolean = false,
    val isFavorite: Boolean = false,
    val isExternallyTracked: Boolean = false,
    val externalId: String? = null,
    val externalSource: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Video / Watch fields (Anime, Show, Movie)
    val currentEpisodes: Int = 0,
    val totalEpisodes: Int = 0,
    val airedEpisodes: Int = 0,
    val seasonNumber: Int = 1,
    val currentSeasons: Int = 1,
    val totalSeasons: Int = 1,
    val seasonEpisodesJson: String? = null,
    val episodeLengthMinutes: Int? = null,
    val currentTimeMinutes: Int? = null,
    val isAiring: Boolean = false,
    val broadcastTime: String? = null,
    val nextAiringEpoch: Long? = null,

    // Reading fields (Manga, Manhwa, Book)
    val currentVolumes: Int = 0,
    val totalVolumes: Int = 0,
    val currentChapters: Int = 0,
    val totalChapters: Int = 0,
    val airedChapters: Int = 0,
    val currentPages: Int = 0,
    val totalPages: Int = 0,
    val trackingUnit: TrackingUnit = TrackingUnit.CHAPTERS,

    // Movie fields
    val movieSeriesNumber: Int? = null,
    val movieSeriesName: String? = null,
    val durationMinutes: Int? = null,
    val relatedEntriesJson: String? = null,

    // Game fields
    val gamePlatforms: String? = null,
    val isGameFranchise: Boolean = false,
    val gameSeriesEntriesJson: String? = null,
    val gameProgressPercent: Int = 0,
    val customUnitLabel: String? = null,
    val customUnitCurrent: Double = 0.0,
    val customUnitTotal: Double = 0.0,

    // User personal thoughts / comments about the entity
    @SerializedName("userComments")
    val userComments: String? = null,

    // Original language code (e.g. "ja", "ko", "zh", "en", "fr")
    val originalLanguage: String? = null,

    // Universal creator field (Music Artist, Book Author, Game Developer, Anime Studio, Movie Director)
    @SerializedName(value = "creator", alternate = ["artist", "author", "studio", "developer"])
    val creator: String? = null,

    // Music album external ID (MusicBrainz Release Group MBID / Apple Music Album ID)
    @SerializedName(value = "musicAlbumId", alternate = ["albumId"])
    val musicAlbumId: String? = null
) {
    // Backward compatibility reference for legacy callers
    @get:Ignore
    val notes: String?
        get() = description

    fun getCreatorRoleLabel(): String = when (mediaType) {
        MediaType.MUSIC -> "Artist"
        MediaType.ANIME -> "Studio"
        MediaType.MANGA, MediaType.MANHWA -> "Author"
        MediaType.BOOK -> "Author"
        MediaType.GAME -> "Developer"
        MediaType.MOVIE -> "Director"
        MediaType.SHOW -> "Creator / Network"
    }

    val artist: String? get() = creator
    val author: String? get() = creator
    val studio: String? get() = creator
    val developer: String? get() = creator

    val isNotYetAiredOrPublished: Boolean
        get() {
            val now = System.currentTimeMillis()
            // 1. Release / broadcast start date is strictly in the future
            if (airedStartDate != null && airedStartDate > now) {
                return true
            }
            // 2. Next airing epoch is strictly in the future and 0 episodes/chapters have aired
            if (isAiring && mediaType.isWatchType && airedEpisodes == 0 && nextAiringEpoch != null && nextAiringEpoch > now) {
                return true
            }
            if (isAiring && !mediaType.isWatchType && airedChapters == 0 && airedStartDate != null && airedStartDate > now) {
                return true
            }
            return false
        }
    fun getRelatedMediaItems(): List<RelatedMediaItem> {
        if (relatedEntriesJson.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<RelatedMediaItem>>() {}.type
            val list = Gson().fromJson<List<RelatedMediaItem>>(relatedEntriesJson, type) ?: emptyList()
            list.map { item ->
                if (item.platforms == null) item.copy(platforms = emptyList()) else item
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getEffectiveRuntimeMinutes(): Int? {
        return if (mediaType == MediaType.MOVIE) {
            durationMinutes?.takeIf { it > 0 } ?: episodeLengthMinutes?.takeIf { it > 0 }
        } else {
            episodeLengthMinutes?.takeIf { it > 0 } ?: durationMinutes?.takeIf { it > 0 }
        }
    }

    fun getPlaybackProgressPercent(): Int? {
        val current = currentTimeMinutes?.takeIf { it > 0 } ?: return null
        val total = getEffectiveRuntimeMinutes() ?: return null
        return ((current.toDouble() / total.toDouble()) * 100).toInt().coerceIn(0, 100)
    }

    fun formatPlaybackTime(): String? {
        val current = currentTimeMinutes?.takeIf { it > 0 } ?: return null
        val hours = current / 60
        val mins = current % 60
        return if (hours > 0) {
            if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
        } else {
            "${mins}m"
        }
    }

    /**
     * Calculates the time already consumed/spent for this entry in minutes.
     * Incorporates watched episodes/runtime, read pages/chapters/volumes, and played hours/percentages,
     * including rewatch/re-read/replay multipliers.
     */
    fun calculateSpentTimeMinutes(): Double {
        val isCompleted = status == MediaStatus.COMPLETED
        return when (mediaType) {
            MediaType.ANIME, MediaType.SHOW -> {
                val epLen = (episodeLengthMinutes ?: (if (mediaType == MediaType.SHOW) 45 else 24)).toDouble().coerceAtLeast(1.0)
                val watchedEp = getTotalWatchedEpisodes().toDouble()
                val effectiveTotalEp = getEffectiveTotalEpisodes().toDouble()
                val totalEpConsumed = watchedEp + (rewatchCount * (if (effectiveTotalEp > 0) effectiveTotalEp else watchedEp))
                totalEpConsumed * epLen
            }
            MediaType.MOVIE -> {
                val dur = (durationMinutes ?: 100).toDouble().coerceAtLeast(1.0)
                val totalViews = (if (isCompleted) 1 else 0) + rewatchCount
                val watchedMin = if (isCompleted) 0.0 else (currentTimeMinutes ?: 0).toDouble().coerceIn(0.0, dur)
                (totalViews * dur) + watchedMin
            }
            MediaType.MANGA, MediaType.MANHWA -> {
                val chRead = (currentChapters + (rewatchCount * (if (totalChapters > 0) totalChapters else currentChapters))).toDouble()
                val volRead = (currentVolumes + (rewatchCount * (if (totalVolumes > 0) totalVolumes else currentVolumes))).toDouble()
                if (chRead > 0) {
                    chRead * 5.0
                } else if (volRead > 0) {
                    volRead * 60.0
                } else {
                    0.0
                }
            }
            MediaType.BOOK -> {
                val pagesRead = (currentPages + (rewatchCount * (if (totalPages > 0) totalPages else currentPages))).toDouble()
                val chRead = (currentChapters + (rewatchCount * (if (totalChapters > 0) totalChapters else currentChapters))).toDouble()
                val volRead = (currentVolumes + (rewatchCount * (if (totalVolumes > 0) totalVolumes else currentVolumes))).toDouble()
                if (pagesRead > 0) {
                    pagesRead * 1.5
                } else if (chRead > 0) {
                    chRead * 10.0
                } else if (volRead > 0) {
                    volRead * 180.0
                } else {
                    0.0
                }
            }
            MediaType.GAME -> {
                val userHours = if (customUnitLabel?.trim()?.equals("Hours", ignoreCase = true) == true) {
                    customUnitCurrent
                } else null

                val avgCompletionMin = (durationMinutes ?: 0).toDouble()

                if (userHours != null && userHours > 0) {
                    userHours * 60.0
                } else if (avgCompletionMin > 0) {
                    val isDone = isCompleted || gameProgressPercent >= 100
                    val totalViews = (if (isDone) 1 else 0) + rewatchCount
                    val fraction = when {
                        isDone -> 1.0
                        customUnitTotal > 0.0 && customUnitCurrent > 0.0 -> (customUnitCurrent / customUnitTotal).coerceIn(0.0, 1.0)
                        gameProgressPercent > 0 -> (gameProgressPercent.coerceIn(0, 100) / 100.0)
                        else -> 0.0
                    }
                    if (isDone) {
                        totalViews * avgCompletionMin
                    } else {
                        val replayedMinutes = rewatchCount * avgCompletionMin
                        val currentRunMinutes = fraction * avgCompletionMin
                        replayedMinutes + currentRunMinutes
                    }
                } else {
                    0.0
                }
            }
            MediaType.MUSIC -> {
                val durSec = (durationMinutes ?: 0).toDouble()
                val totalListens = (if (isCompleted) 1 else 0) + rewatchCount
                (totalListens * durSec) / 60.0
            }
        }
    }

    /**
     * Calculates the time remaining to complete this entry in minutes.
     * Returns 0.0 if already completed.
     */
    fun calculateRemainingTimeMinutes(): Double {
        if (status == MediaStatus.COMPLETED) return 0.0

        return when (mediaType) {
            MediaType.ANIME, MediaType.SHOW -> {
                val epLen = (episodeLengthMinutes ?: (if (mediaType == MediaType.SHOW) 45 else 24)).toDouble().coerceAtLeast(1.0)
                val watchedEp = getTotalWatchedEpisodes()
                val effectiveTotalEp = getEffectiveTotalEpisodes()
                if (effectiveTotalEp > 0 && effectiveTotalEp > watchedEp) {
                    (effectiveTotalEp - watchedEp) * epLen
                } else if (totalEpisodes > 0 && totalEpisodes > watchedEp) {
                    (totalEpisodes - watchedEp) * epLen
                } else {
                    0.0
                }
            }
            MediaType.MOVIE -> {
                val dur = (durationMinutes ?: 100).toDouble().coerceAtLeast(1.0)
                val watchedMin = (currentTimeMinutes ?: 0).toDouble().coerceIn(0.0, dur)
                (dur - watchedMin).coerceAtLeast(0.0)
            }
            MediaType.MANGA, MediaType.MANHWA -> {
                if (totalChapters > 0 && totalChapters > currentChapters) {
                    (totalChapters - currentChapters) * 5.0
                } else if (totalVolumes > 0 && totalVolumes > currentVolumes) {
                    (totalVolumes - currentVolumes) * 60.0
                } else {
                    0.0
                }
            }
            MediaType.BOOK -> {
                if (totalPages > 0 && totalPages > currentPages) {
                    (totalPages - currentPages) * 1.5
                } else if (totalChapters > 0 && totalChapters > currentChapters) {
                    (totalChapters - currentChapters) * 10.0
                } else if (totalVolumes > 0 && totalVolumes > currentVolumes) {
                    (totalVolumes - currentVolumes) * 180.0
                } else {
                    0.0
                }
            }
            MediaType.GAME -> {
                if (gameProgressPercent >= 100) return 0.0

                val userHours = if (customUnitLabel?.trim()?.equals("Hours", ignoreCase = true) == true) {
                    customUnitCurrent
                } else null

                val avgCompletionMin = (durationMinutes ?: 0).toDouble()

                if (userHours != null && userHours > 0) {
                    val effTotalHours = getEffectiveCustomUnitTotal()
                    if (effTotalHours > 0.0 && effTotalHours > userHours) {
                        (effTotalHours - userHours) * 60.0
                    } else {
                        0.0
                    }
                } else if (avgCompletionMin > 0) {
                    val fraction = when {
                        customUnitTotal > 0.0 && customUnitCurrent > 0.0 -> (customUnitCurrent / customUnitTotal).coerceIn(0.0, 1.0)
                        gameProgressPercent > 0 -> (gameProgressPercent.coerceIn(0, 100) / 100.0)
                        else -> 0.0
                    }
                    (1.0 - fraction).coerceAtLeast(0.0) * avgCompletionMin
                } else {
                    0.0
                }
            }
            MediaType.MUSIC -> {
                if (status == MediaStatus.COMPLETED) 0.0
                else (durationMinutes ?: 0).toDouble() / 60.0
            }
        }
    }

    fun matchesExternal(
        candidateExternalId: String?,
        candidateExternalSource: String?,
        candidateTitle: String,
        candidateMediaType: MediaType,
        candidateReleaseYear: String? = null,
        candidateAlternateTitle: String? = null
    ): Boolean {
        val entrySrc = normalizeSource(externalSource)
        val candidateSrc = normalizeSource(candidateExternalSource)
        val compatibleSource = (entrySrc == null || candidateSrc == null || entrySrc == candidateSrc)

        // 1. If both have external IDs and sources are compatible, ID equality is authoritative.
        if (!externalId.isNullOrBlank() && !candidateExternalId.isNullOrBlank()) {
            if (compatibleSource && mediaType == candidateMediaType) {
                return externalId.equals(candidateExternalId, ignoreCase = true)
            }
        }

        // 2. If both have external IDs from compatible sources and they didn't match,
        // they are definitively different items in that catalog. Never fall back to title matching.
        if (!externalId.isNullOrBlank() && !candidateExternalId.isNullOrBlank() && compatibleSource && mediaType == candidateMediaType) {
            return false
        }

        // 3. Fallback matching: media type must match.
        if (mediaType != candidateMediaType) {
            return false
        }

        // 4. Title or alternate title must match.
        val titleMatches = title.equals(candidateTitle, ignoreCase = true) ||
                (!alternateTitle.isNullOrBlank() && alternateTitle.equals(candidateTitle, ignoreCase = true)) ||
                (!candidateAlternateTitle.isNullOrBlank() && (title.equals(candidateAlternateTitle, ignoreCase = true) || (!alternateTitle.isNullOrBlank() && alternateTitle.equals(candidateAlternateTitle, ignoreCase = true))))

        if (!titleMatches) {
            return false
        }

        // 5. If titles match, check release year if available on both sides to prevent
        // false-matching remakes / reboots with identical names.
        val entryYear = airedStartDate?.let { extractYearFromMillis(it) }
        val candidateYear = candidateReleaseYear?.trim()?.take(4)?.takeIf { it.length == 4 && it.all { c -> c.isDigit() } }

        if (entryYear != null && candidateYear != null) {
            return entryYear == candidateYear
        }

        return true
    }

    fun getCorrelatedRelatedItems(allEntries: List<MediaEntry>): List<RelatedMediaItem> {
        val items = getRelatedMediaItems()
        if (items.isEmpty()) return emptyList()
        return items.map { item ->
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
    }

    val foreignDatabaseId: String?
        get() = externalId

    val foreignDatabaseSource: String?
        get() = externalSource

    fun getSeasonEpisodes(): List<Int> {
        if (mediaType != MediaType.SHOW) return emptyList()
        return parseSeasonEpisodes(seasonEpisodesJson)
    }

    fun hasSeasonEpisodeTracking(): Boolean {
        return mediaType == MediaType.SHOW && getSeasonEpisodes().any { it > 0 }
    }

    fun getEffectiveTotalSeasons(): Int {
        if (mediaType != MediaType.SHOW) return totalSeasons
        val listSize = getSeasonEpisodes().size
        return maxOf(totalSeasons, listSize, 1)
    }

    fun getTotalEpisodesForSeason(season: Int = currentSeasons): Int {
        if (!hasSeasonEpisodeTracking()) return totalEpisodes
        val idx = season - 1
        if (idx < 0) return 0
        return getSeasonEpisodes().getOrNull(idx)?.coerceAtLeast(0) ?: 0
    }

    fun getEffectiveTotalEpisodes(): Int {
        if (!hasSeasonEpisodeTracking()) return totalEpisodes
        val sum = getSeasonEpisodes().sum()
        return if (sum > 0) sum else totalEpisodes
    }

    fun getTotalWatchedEpisodes(): Int {
        if (!hasSeasonEpisodeTracking()) return currentEpisodes.coerceAtLeast(0)
        val counts = getSeasonEpisodes()
        val curSeason = currentSeasons.coerceIn(1, maxOf(counts.size, totalSeasons, 1))
        val priorEpisodes = (0 until (curSeason - 1).coerceAtMost(counts.size))
            .sumOf { counts[it].coerceAtLeast(0) }
        return priorEpisodes + currentEpisodes.coerceAtLeast(0)
    }

    val displayProgress: String
        get() = when (mediaType) {
            MediaType.ANIME, MediaType.SHOW -> {
                val timeStr = if (currentTimeMinutes != null && currentTimeMinutes > 0) " (${formatPlaybackTime() ?: "${currentTimeMinutes}m"})" else ""
                if (hasSeasonEpisodeTracking()) {
                    val seasonTotal = getTotalEpisodesForSeason(currentSeasons)
                    val seasonTotalStr = if (seasonTotal > 0) "$seasonTotal" else "?"
                    "S$currentSeasons • Ep $currentEpisodes/$seasonTotalStr$timeStr"
                } else {
                    val totalStr = if (totalEpisodes > 0) "$totalEpisodes" else "?"
                    if (mediaType == MediaType.SHOW && (totalSeasons > 1 || currentSeasons > 1)) {
                        if (isAiring && airedEpisodes > 0) {
                            "S$currentSeasons • Ep $currentEpisodes/$airedEpisodes ($totalStr)$timeStr"
                        } else {
                            "S$currentSeasons • Ep $currentEpisodes/$totalStr$timeStr"
                        }
                    } else {
                        if (isAiring && airedEpisodes > 0) {
                            "$currentEpisodes/$airedEpisodes ($totalStr) ep$timeStr"
                        } else {
                            "$currentEpisodes/$totalStr ep$timeStr"
                        }
                    }
                }
            }
            MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK -> {
                when (trackingUnit) {
                    TrackingUnit.VOLUMES -> {
                        val totalStr = if (totalVolumes > 0) "$totalVolumes" else "?"
                        "Vol $currentVolumes/$totalStr"
                    }
                    TrackingUnit.PAGES -> {
                        val totalStr = if (totalPages > 0) "$totalPages" else "?"
                        "$currentPages/$totalStr p"
                    }
                    TrackingUnit.COMBINED -> {
                        val parts = mutableListOf<String>()
                        if (currentVolumes > 0 || totalVolumes > 0) {
                            parts.add("Vol $currentVolumes" + if (totalVolumes > 0) "/$totalVolumes" else "")
                        }
                        if (currentChapters > 0 || totalChapters > 0 || airedChapters > 0) {
                            val chStr = if (isAiring && airedChapters > 0) {
                                "Ch $currentChapters/$airedChapters" + if (totalChapters > 0) " ($totalChapters)" else ""
                            } else {
                                "Ch $currentChapters" + if (totalChapters > 0) "/$totalChapters" else ""
                            }
                            parts.add(chStr)
                        }
                        if (currentPages > 0 || totalPages > 0) {
                            parts.add("$currentPages" + if (totalPages > 0) "/$totalPages" else "" + "p")
                        }
                        if (parts.isEmpty()) {
                            val totalStr = if (totalChapters > 0) "$totalChapters" else "?"
                            if (isAiring && airedChapters > 0) {
                                "Ch $currentChapters/$airedChapters ($totalStr)"
                            } else {
                                "Ch $currentChapters/$totalStr"
                            }
                        } else {
                            parts.joinToString(" • ")
                        }
                    }
                    TrackingUnit.CHAPTERS -> {
                        val totalStr = if (totalChapters > 0) "$totalChapters" else "?"
                        if (isAiring && airedChapters > 0) {
                            "Ch $currentChapters/$airedChapters ($totalStr)"
                        } else {
                            "Ch $currentChapters/$totalStr"
                        }
                    }
                    else -> {
                        val totalStr = if (totalChapters > 0) "$totalChapters" else "?"
                        "Ch $currentChapters/$totalStr"
                    }
                }
            }
            MediaType.MOVIE -> {
                val seriesStr = if (movieSeriesNumber != null) "#$movieSeriesNumber" else ""
                val durStr = if (durationMinutes != null && durationMinutes > 0) {
                    val h = durationMinutes / 60
                    val m = durationMinutes % 60
                    if (h > 0) "${h}h ${m}m" else "${m}m"
                } else ""
                val timeStr = if (currentTimeMinutes != null && currentTimeMinutes > 0) {
                    formatPlaybackTime() ?: "${currentTimeMinutes}m"
                } else ""
                val progressText = if (timeStr.isNotBlank() && durStr.isNotBlank()) {
                    "$timeStr / $durStr"
                } else if (timeStr.isNotBlank()) {
                    timeStr
                } else durStr

                listOf(seriesStr, progressText).filter { it.isNotBlank() }.joinToString(" • ")
                    .ifBlank { if (status == MediaStatus.COMPLETED) "Watched" else "Planned" }
            }
            MediaType.GAME -> {
                if (trackingUnit == TrackingUnit.PERCENTAGE) {
                    "$gameProgressPercent%"
                } else {
                    val label = customUnitLabel?.takeIf { it.isNotBlank() } ?: "Units"
                    val curStr = formatCustomUnitValue(customUnitCurrent)
                    val effTotal = getEffectiveCustomUnitTotal()
                    if (effTotal > 0.0) {
                        val totStr = formatCustomUnitValue(effTotal)
                        "$curStr/$totStr $label"
                    } else "$curStr $label"
                }
            }
            MediaType.MUSIC -> {
                val durSec = durationMinutes ?: 0
                val durStr = if (durSec > 0) formatMusicDuration(durSec) else ""
                val albumStr = movieSeriesName?.let { alb ->
                    val trackStr = if (movieSeriesNumber != null) "Track $movieSeriesNumber" else ""
                    listOf(trackStr, alb).filter { it.isNotBlank() }.joinToString(" • ")
                } ?: ""
                listOf(durStr, albumStr).filter { it.isNotBlank() }.joinToString(" • ")
                    .ifBlank { if (status == MediaStatus.COMPLETED) "Listened" else "Planned" }
            }
        }

    fun formatMusicDuration(seconds: Int? = durationMinutes): String = Companion.formatMusicDuration(seconds)

    val progressFraction: Float
        get() = when (mediaType) {
            MediaType.ANIME, MediaType.SHOW -> {
                val overallTotal = getEffectiveTotalEpisodes()
                if (overallTotal > 0) {
                    val epLen = episodeLengthMinutes ?: 24
                    val intraEp = if (currentTimeMinutes != null && currentTimeMinutes > 0 && epLen > 0) {
                        (currentTimeMinutes.toFloat() / epLen).coerceIn(0f, 0.99f)
                    } else 0f
                    val watched = getTotalWatchedEpisodes()
                    ((watched.toFloat() + intraEp) / overallTotal).coerceIn(0f, 1f)
                } else if (status == MediaStatus.COMPLETED) 1f else 0f
            }
            MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK -> {
                when (trackingUnit) {
                    TrackingUnit.VOLUMES -> if (totalVolumes > 0) (currentVolumes.toFloat() / totalVolumes).coerceIn(0f, 1f) else 0f
                    TrackingUnit.PAGES -> if (totalPages > 0) (currentPages.toFloat() / totalPages).coerceIn(0f, 1f) else 0f
                    TrackingUnit.CHAPTERS, TrackingUnit.COMBINED -> {
                        if (totalChapters > 0) (currentChapters.toFloat() / totalChapters).coerceIn(0f, 1f)
                        else if (totalVolumes > 0) (currentVolumes.toFloat() / totalVolumes).coerceIn(0f, 1f)
                        else if (status == MediaStatus.COMPLETED) 1f else 0f
                    }
                    else -> {
                        if (totalChapters > 0) (currentChapters.toFloat() / totalChapters).coerceIn(0f, 1f)
                        else if (status == MediaStatus.COMPLETED) 1f else 0f
                    }
                }
            }
            MediaType.MOVIE -> {
                if (status == MediaStatus.COMPLETED) 1f
                else if (durationMinutes != null && durationMinutes > 0 && currentTimeMinutes != null && currentTimeMinutes > 0) {
                    (currentTimeMinutes.toFloat() / durationMinutes.toFloat()).coerceIn(0f, 1f)
                } else 0f
            }
            MediaType.GAME -> {
                if (trackingUnit == TrackingUnit.PERCENTAGE) {
                    (gameProgressPercent / 100f).coerceIn(0f, 1f)
                } else {
                    val effTotal = getEffectiveCustomUnitTotal()
                    if (effTotal > 0.0) (customUnitCurrent / effTotal).toFloat().coerceIn(0f, 1f)
                    else if (status == MediaStatus.COMPLETED) 1f else 0f
                }
            }
            MediaType.MUSIC -> {
                if (status == MediaStatus.COMPLETED) 1f else 0f
            }
        }

    fun incrementProgress(): MediaEntry {
        val now = System.currentTimeMillis()
        return when (mediaType) {
            MediaType.ANIME, MediaType.SHOW -> {
                if (hasSeasonEpisodeTracking()) {
                    val effectiveSeasons = getEffectiveTotalSeasons()
                    val curSeason = currentSeasons.coerceIn(1, effectiveSeasons)
                    val curSeasonTotal = getTotalEpisodesForSeason(curSeason)
                    var nextSeason = curSeason
                    var nextEp = currentEpisodes + 1

                    if (curSeasonTotal > 0 && currentEpisodes >= curSeasonTotal) {
                        if (curSeason < effectiveSeasons) {
                            nextSeason = curSeason + 1
                            nextEp = 1
                        } else {
                            nextEp = curSeasonTotal
                        }
                    }

                    val lastSeasonTotal = getTotalEpisodesForSeason(effectiveSeasons)
                    val isDone = nextSeason >= effectiveSeasons && lastSeasonTotal > 0 && nextEp >= lastSeasonTotal
                    val newStatus = if (isDone) MediaStatus.COMPLETED else status
                    val newRewatchCount = if (isDone && isRewatching) rewatchCount + 1 else rewatchCount
                    val newIsRewatching = if (isDone) false else isRewatching
                    copy(
                        currentSeasons = nextSeason,
                        currentEpisodes = nextEp,
                        currentTimeMinutes = null,
                        status = newStatus,
                        rewatchCount = newRewatchCount,
                        isRewatching = newIsRewatching,
                        updatedAt = now,
                        finishDate = if (newStatus == MediaStatus.COMPLETED && finishDate == null) now else finishDate
                    )
                } else {
                    val nextEp = currentEpisodes + 1
                    val isDone = totalEpisodes > 0 && nextEp >= totalEpisodes
                    val newStatus = if (isDone) MediaStatus.COMPLETED else status
                    val newRewatchCount = if (isDone && isRewatching) rewatchCount + 1 else rewatchCount
                    val newIsRewatching = if (isDone) false else isRewatching
                    copy(
                        currentEpisodes = nextEp,
                        currentTimeMinutes = null,
                        status = newStatus,
                        rewatchCount = newRewatchCount,
                        isRewatching = newIsRewatching,
                        updatedAt = now,
                        finishDate = if (newStatus == MediaStatus.COMPLETED && finishDate == null) now else finishDate
                    )
                }
            }
            MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK -> {
                when (trackingUnit) {
                    TrackingUnit.VOLUMES -> {
                        val nextVol = currentVolumes + 1
                        val isDone = totalVolumes > 0 && nextVol >= totalVolumes
                        val newStatus = if (isDone) MediaStatus.COMPLETED else status
                        val newRewatchCount = if (isDone && isRewatching) rewatchCount + 1 else rewatchCount
                        val newIsRewatching = if (isDone) false else isRewatching
                        copy(
                            currentVolumes = nextVol,
                            status = newStatus,
                            rewatchCount = newRewatchCount,
                            isRewatching = newIsRewatching,
                            updatedAt = now,
                            finishDate = if (newStatus == MediaStatus.COMPLETED && finishDate == null) now else finishDate
                        )
                    }
                    TrackingUnit.PAGES -> {
                        val nextPages = currentPages + 10
                        val isDone = totalPages > 0 && nextPages >= totalPages
                        val newStatus = if (isDone) MediaStatus.COMPLETED else status
                        val newRewatchCount = if (isDone && isRewatching) rewatchCount + 1 else rewatchCount
                        val newIsRewatching = if (isDone) false else isRewatching
                        copy(
                            currentPages = nextPages,
                            status = newStatus,
                            rewatchCount = newRewatchCount,
                            isRewatching = newIsRewatching,
                            updatedAt = now,
                            finishDate = if (newStatus == MediaStatus.COMPLETED && finishDate == null) now else finishDate
                        )
                    }
                    TrackingUnit.CHAPTERS, TrackingUnit.COMBINED -> {
                        val nextCh = currentChapters + 1
                        val isDone = totalChapters > 0 && nextCh >= totalChapters
                        val newStatus = if (isDone) MediaStatus.COMPLETED else status
                        val newRewatchCount = if (isDone && isRewatching) rewatchCount + 1 else rewatchCount
                        val newIsRewatching = if (isDone) false else isRewatching
                        copy(
                            currentChapters = nextCh,
                            status = newStatus,
                            rewatchCount = newRewatchCount,
                            isRewatching = newIsRewatching,
                            updatedAt = now,
                            finishDate = if (newStatus == MediaStatus.COMPLETED && finishDate == null) now else finishDate
                        )
                    }
                    else -> {
                        val nextCh = currentChapters + 1
                        val isDone = totalChapters > 0 && nextCh >= totalChapters
                        val newStatus = if (isDone) MediaStatus.COMPLETED else status
                        val newRewatchCount = if (isDone && isRewatching) rewatchCount + 1 else rewatchCount
                        val newIsRewatching = if (isDone) false else isRewatching
                        copy(
                            currentChapters = nextCh,
                            status = newStatus,
                            rewatchCount = newRewatchCount,
                            isRewatching = newIsRewatching,
                            updatedAt = now,
                            finishDate = if (newStatus == MediaStatus.COMPLETED && finishDate == null) now else finishDate
                        )
                    }
                }
            }
            MediaType.MOVIE -> {
                val newStatus = if (status == MediaStatus.COMPLETED) MediaStatus.WATCHING else MediaStatus.COMPLETED
                val isDone = newStatus == MediaStatus.COMPLETED
                val newRewatchCount = if (isDone && isRewatching) rewatchCount + 1 else rewatchCount
                val newIsRewatching = if (isDone) false else isRewatching
                copy(
                    status = newStatus,
                    rewatchCount = newRewatchCount,
                    isRewatching = newIsRewatching,
                    updatedAt = now,
                    finishDate = if (status != MediaStatus.COMPLETED && finishDate == null) now else finishDate
                )
            }
            MediaType.GAME -> {
                if (trackingUnit == TrackingUnit.PERCENTAGE) {
                    val nextPercent = (gameProgressPercent + 5).coerceAtMost(100)
                    val isDone = nextPercent >= 100
                    val newStatus = if (isDone) MediaStatus.COMPLETED else status
                    val newRewatchCount = if (isDone && isRewatching) rewatchCount + 1 else rewatchCount
                    val newIsRewatching = if (isDone) false else isRewatching
                    copy(
                        gameProgressPercent = nextPercent,
                        status = newStatus,
                        rewatchCount = newRewatchCount,
                        isRewatching = newIsRewatching,
                        updatedAt = now,
                        finishDate = if (newStatus == MediaStatus.COMPLETED && finishDate == null) now else finishDate
                    )
                } else {
                    val nextVal = customUnitCurrent + 1.0
                    val isDone = customUnitTotal > 0.0 && nextVal >= customUnitTotal
                    val newStatus = if (isDone) MediaStatus.COMPLETED else status
                    val newRewatchCount = if (isDone && isRewatching) rewatchCount + 1 else rewatchCount
                    val newIsRewatching = if (isDone) false else isRewatching
                    copy(
                        customUnitCurrent = nextVal,
                        status = newStatus,
                        rewatchCount = newRewatchCount,
                        isRewatching = newIsRewatching,
                        updatedAt = now,
                        finishDate = if (newStatus == MediaStatus.COMPLETED && finishDate == null) now else finishDate
                    )
                }
            }
            MediaType.MUSIC -> {
                val newStatus = if (status == MediaStatus.COMPLETED) MediaStatus.WATCHING else MediaStatus.COMPLETED
                val isDone = newStatus == MediaStatus.COMPLETED
                val newRewatchCount = if (isDone && isRewatching) rewatchCount + 1 else rewatchCount
                val newIsRewatching = if (isDone) false else isRewatching
                copy(
                    status = newStatus,
                    rewatchCount = newRewatchCount,
                    isRewatching = newIsRewatching,
                    updatedAt = now,
                    finishDate = if (status != MediaStatus.COMPLETED && finishDate == null) now else finishDate
                )
            }
        }
    }

    fun decrementProgress(): MediaEntry {
        val now = System.currentTimeMillis()
        return when (mediaType) {
            MediaType.ANIME, MediaType.SHOW -> {
                if (hasSeasonEpisodeTracking()) {
                    val effectiveSeasons = getEffectiveTotalSeasons()
                    val curSeason = currentSeasons.coerceIn(1, effectiveSeasons)
                    val (prevSeason, prevEp) = if (currentEpisodes <= 1 && curSeason > 1) {
                        val targetSeason = curSeason - 1
                        val targetSeasonTotal = getTotalEpisodesForSeason(targetSeason)
                        targetSeason to if (targetSeasonTotal > 0) targetSeasonTotal else 1
                    } else {
                        curSeason to (currentEpisodes - 1).coerceAtLeast(0)
                    }
                    copy(
                        currentSeasons = prevSeason,
                        currentEpisodes = prevEp,
                        currentTimeMinutes = null,
                        updatedAt = now
                    )
                } else {
                    copy(
                        currentEpisodes = (currentEpisodes - 1).coerceAtLeast(0),
                        currentTimeMinutes = null,
                        updatedAt = now
                    )
                }
            }
            MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK -> {
                when (trackingUnit) {
                    TrackingUnit.VOLUMES -> copy(
                        currentVolumes = (currentVolumes - 1).coerceAtLeast(0),
                        updatedAt = now
                    )
                    TrackingUnit.PAGES -> copy(
                        currentPages = (currentPages - 10).coerceAtLeast(0),
                        updatedAt = now
                    )
                    TrackingUnit.CHAPTERS, TrackingUnit.COMBINED -> copy(
                        currentChapters = (currentChapters - 1).coerceAtLeast(0),
                        updatedAt = now
                    )
                    else -> copy(
                        currentChapters = (currentChapters - 1).coerceAtLeast(0),
                        updatedAt = now
                    )
                }
            }
            MediaType.MOVIE -> copy(
                status = if (status == MediaStatus.COMPLETED) MediaStatus.WATCHING else status,
                updatedAt = now
            )
            MediaType.GAME -> {
                if (trackingUnit == TrackingUnit.PERCENTAGE) {
                    val nextPercent = (gameProgressPercent - 5).coerceAtLeast(0)
                    copy(
                        gameProgressPercent = nextPercent,
                        status = if (status == MediaStatus.COMPLETED && nextPercent < 100) MediaStatus.WATCHING else status,
                        updatedAt = now
                    )
                } else {
                    val nextVal = (customUnitCurrent - 1.0).coerceAtLeast(0.0)
                    copy(
                        customUnitCurrent = nextVal,
                        status = if (status == MediaStatus.COMPLETED && customUnitTotal > 0.0 && nextVal < customUnitTotal) MediaStatus.WATCHING else status,
                        updatedAt = now
                    )
                }
            }
            MediaType.MUSIC -> copy(updatedAt = now)
        }
    }

    fun getAlternateTitles(): List<String> {
        return alternateTitle
            ?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    fun getTagList(): List<String> {
        return tags
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    fun getGamePlatformList(): List<String> {
        return parseGamePlatforms(gamePlatforms)
    }

    fun getEffectiveCustomUnitTotal(): Double {
        if (customUnitTotal > 0.0) return customUnitTotal
        if (customUnitLabel?.trim()?.equals("Hours", ignoreCase = true) == true && durationMinutes != null && durationMinutes > 0) {
            return durationMinutes / 60.0
        }
        return 0.0
    }

    fun getEffectiveLanguage(): String {
        val lang = originalLanguage?.trim()?.lowercase(Locale.ROOT)
        if (!lang.isNullOrBlank()) return lang

        // 1. Tags scan for language names or codes
        val allTags = getTagList()
        for (tag in allTags) {
            val opt = com.lx.tracklist.ui.search.OriginalLanguageRegistry.findByCode(tag)
            if (opt != null) return opt.code
        }

        // 2. Custom Category / Sub Category heuristics
        val catText = (customCategory.orEmpty() + " " + subCategory.orEmpty()).lowercase(Locale.ROOT)
        when {
            catText.contains("donghua") || catText.contains("manhua") || catText.contains("cdrama") || catText.contains("c-drama") || catText.contains("chinese") -> return "zh"
            catText.contains("manhwa") || catText.contains("aeni") || catText.contains("kdrama") || catText.contains("k-drama") || catText.contains("korean") || catText.contains("webtoon") -> return "ko"
            catText.contains("jdrama") || catText.contains("j-drama") || catText.contains("anime") || catText.contains("manga") || catText.contains("japanese") -> return "ja"
        }

        // 3. Fallback by MediaType
        return when (mediaType) {
            MediaType.ANIME -> "ja"
            MediaType.MANGA -> "ja"
            MediaType.MANHWA -> "ko"
            MediaType.BOOK -> "en"
            MediaType.MOVIE, MediaType.SHOW -> "en"
            MediaType.GAME, MediaType.MUSIC -> "en"
        }
    }

    fun getGameSeriesEntries(): List<GameSeriesItem> {
        if (gameSeriesEntriesJson.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<GameSeriesItem>>() {}.type
            Gson().fromJson<List<GameSeriesItem>>(gameSeriesEntriesJson, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getFranchiseProgressSummary(): Triple<Int, Int, Int> {
        val items = getGameSeriesEntries()
        val completedCount = items.count { it.status == GameSeriesItemStatus.COMPLETED }
        val activeNonSkippedCount = items.count { it.status != GameSeriesItemStatus.SKIPPED }
        val skippedCount = items.count { it.status == GameSeriesItemStatus.SKIPPED }
        return Triple(completedCount, activeNonSkippedCount, skippedCount)
    }

    fun formatExternalScore(): String? {
        val sc = externalScore ?: return null
        val src = externalScoreSource?.takeIf { it.isNotBlank() } ?: "External"
        val formatted = if (sc % 1.0f == 0.0f) {
            sc.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", sc).trimEnd('0').trimEnd('.')
        }
        return "$formatted ($src)"
    }

    fun formatAiredDateRange(): String {
        if (airedStartDate == null && airedEndDate == null) return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startStr = airedStartDate?.let { sdf.format(Date(it)) } ?: "?"
        val endStr = if (isAiring && !isNotYetAiredOrPublished && airedEndDate == null) "Present" else (airedEndDate?.let { sdf.format(Date(it)) } ?: "")
        return if (endStr.isNotBlank()) "$startStr - $endStr" else startStr
    }

    fun formatUserDateRange(): String {
        if (startDate == null && finishDate == null) return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startStr = startDate?.let { sdf.format(Date(it)) } ?: "?"
        val finishStr = finishDate?.let { sdf.format(Date(it)) } ?: ""
        return if (finishStr.isNotBlank()) "$startStr - $finishStr" else startStr
    }

    fun getEffectiveAiredEpisodes(): Int {
        if (isAiring && (mediaType == MediaType.ANIME || mediaType == MediaType.SHOW) && airedStartDate != null && airedStartDate > 0L) {
            val now = System.currentTimeMillis()
            if (now >= airedStartDate) {
                val weeks = ((now - airedStartDate) / (7L * 24 * 60 * 60 * 1000L)).toInt() + 1
                val calculated = if (totalEpisodes > 0) minOf(weeks, totalEpisodes) else weeks
                return maxOf(airedEpisodes, calculated)
            }
        }
        return airedEpisodes
    }

    fun getComputedNextAiringEpoch(): Long? {
        val now = System.currentTimeMillis()

        // 1. Any entry that is not yet aired / published / released
        if (isNotYetAiredOrPublished) {
            val epoch = nextAiringEpoch?.takeIf { it > now }
                ?: airedStartDate?.takeIf { it > now }
            if (epoch != null) return epoch
        }

        // 2. Airing episodic media (Anime or TV Show)
        if (isAiring && (mediaType == MediaType.ANIME || mediaType == MediaType.SHOW)) {
            // Explicit next airing epoch (roll forward weekly if elapsed)
            if (nextAiringEpoch != null && nextAiringEpoch > 0L) {
                var epoch = nextAiringEpoch
                while (epoch <= now) {
                    epoch += 7L * 24 * 60 * 60 * 1000L
                }
                return epoch
            }

            // Broadcast time string (e.g. "Sundays at 00:00 (JST)")
            if (!broadcastTime.isNullOrBlank()) {
                val parsed = parseBroadcastToNextEpoch(broadcastTime, now)
                if (parsed != null) return parsed
            }

            // Fallback: project weekly from airedStartDate
            if (airedStartDate != null && airedStartDate > 0L) {
                var epoch = airedStartDate
                while (epoch <= now) {
                    epoch += 7L * 24 * 60 * 60 * 1000L
                }
                return epoch
            }
        }

        return null
    }

    fun formatAiringSchedule(
        short: Boolean = false,
        timeZone: TimeZone = TimeZone.getDefault(),
        includeTimeZone: Boolean = !short
    ): String? {
        val nextEpoch = getComputedNextAiringEpoch() ?: return null

        val isCalendarDate = isNotYetAiredOrPublished || (mediaType != MediaType.ANIME && mediaType != MediaType.SHOW)
        if (isCalendarDate) {
            val pattern = if (short) "MMM d, yyyy" else "MMMM d, yyyy"
            val effectiveTz = if (timeZone == TimeZone.getDefault()) TimeZone.getTimeZone("UTC") else timeZone
            val sdf = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                this.timeZone = effectiveTz
            }
            return sdf.format(Date(nextEpoch))
        }

        val pattern = when {
            short && includeTimeZone -> "EEE HH:mm z"
            short && !includeTimeZone -> "EEE HH:mm"
            !short && includeTimeZone -> "EEEE 'at' HH:mm z"
            else -> "EEEE 'at' HH:mm"
        }
        val sdf = SimpleDateFormat(pattern, Locale.getDefault()).apply {
            this.timeZone = timeZone
        }
        return sdf.format(Date(nextEpoch))
    }

    fun getTimeUntilNextEpisode(prefixIn: Boolean = true): String? {
        val nextEpoch = getComputedNextAiringEpoch() ?: return null
        val now = System.currentTimeMillis()
        val diff = nextEpoch - now
        if (diff <= 0) {
            return when (mediaType) {
                MediaType.MOVIE, MediaType.GAME, MediaType.MUSIC -> "Releasing today"
                MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK -> "Publishing today"
                MediaType.SHOW, MediaType.ANIME -> if (isNotYetAiredOrPublished) "Premieres today" else "Airing now"
            }
        }

        val totalHours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = totalHours / 24
        val hours = totalHours % 24
        val timeStr = when {
            days > 30 -> "${days}d"
            days > 0 && hours > 0 -> "${days}d ${hours}h"
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            else -> {
                val mins = TimeUnit.MILLISECONDS.toMinutes(diff).coerceAtLeast(1)
                "${mins}m"
            }
        }
        return if (prefixIn) "In $timeStr" else timeStr
    }

    fun formatAiringCardText(): String? {
        val schedule = formatAiringSchedule(short = true, includeTimeZone = false)
        val countdown = getTimeUntilNextEpisode(prefixIn = true)
        return when {
            !schedule.isNullOrBlank() && !countdown.isNullOrBlank() -> "$schedule • $countdown"
            !countdown.isNullOrBlank() -> countdown
            !schedule.isNullOrBlank() -> schedule
            else -> null
        }
    }

    companion object {
        fun formatMusicDuration(seconds: Int?): String {
            val s = (seconds ?: 0).coerceAtLeast(0)
            val m = s / 60
            val remSec = s % 60
            return "$m:${remSec.toString().padStart(2, '0')}"
        }

        fun parseBroadcastToNextEpoch(broadcastStr: String, now: Long = System.currentTimeMillis()): Long? {
            if (broadcastStr.isBlank() || broadcastStr.equals("Unknown", ignoreCase = true)) return null
            val regex = Regex("""([A-Za-z]+)\s+at\s+(\d{1,2}):(\d{2})""", RegexOption.IGNORE_CASE)
            val match = regex.find(broadcastStr) ?: return null
            val dayStr = match.groupValues[1].lowercase(Locale.ROOT)
            val hour = match.groupValues[2].toIntOrNull() ?: return null
            val min = match.groupValues[3].toIntOrNull() ?: return null

            val dayOfWeek = when {
                dayStr.startsWith("sun") -> Calendar.SUNDAY
                dayStr.startsWith("mon") -> Calendar.MONDAY
                dayStr.startsWith("tue") -> Calendar.TUESDAY
                dayStr.startsWith("wed") -> Calendar.WEDNESDAY
                dayStr.startsWith("thu") -> Calendar.THURSDAY
                dayStr.startsWith("fri") -> Calendar.FRIDAY
                dayStr.startsWith("sat") -> Calendar.SATURDAY
                else -> return null
            }

            val tz = if (broadcastStr.contains("JST", ignoreCase = true) || broadcastStr.contains("Tokyo", ignoreCase = true)) {
                TimeZone.getTimeZone("Asia/Tokyo")
            } else {
                TimeZone.getTimeZone("Asia/Tokyo")
            }

            val cal = Calendar.getInstance(tz)
            cal.timeInMillis = now
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, min)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            var attempts = 0
            while ((cal.get(Calendar.DAY_OF_WEEK) != dayOfWeek || cal.timeInMillis <= now) && attempts < 14) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                attempts++
            }
            return cal.timeInMillis
        }
        fun formatAlternateTitles(titles: List<String>): String? {
            val cleaned = titles.map { it.trim() }.filter { it.isNotBlank() }
            return if (cleaned.isEmpty()) null else cleaned.joinToString("\n")
        }

        fun extractYearFromMillis(timestamp: Long): String {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.timeInMillis = timestamp
            return cal.get(Calendar.YEAR).toString()
        }

        fun normalizeSource(source: String?): String? {
            if (source.isNullOrBlank()) return null
            val upper = source.trim().uppercase(Locale.US)
            return if (upper == "MYANIMELIST") "MAL" else upper
        }

        fun formatSeasonEpisodes(counts: List<Int>): String? {
            if (counts.isEmpty() || counts.all { it <= 0 }) return null
            return Gson().toJson(counts.map { it.coerceAtLeast(0) })
        }

        fun parseSeasonEpisodes(json: String?): List<Int> {
            if (json.isNullOrBlank()) return emptyList()
            return try {
                val type = object : TypeToken<List<Int>>() {}.type
                Gson().fromJson<List<Int>>(json, type)?.map { it.coerceAtLeast(0) } ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun formatGameSeriesEntries(items: List<GameSeriesItem>): String? {
            if (items.isEmpty()) return null
            return Gson().toJson(items)
        }

        val CANONICAL_GAME_PLATFORMS = listOf("PC", "PS", "XBox", "Switch", "Mobile", "Retro")

        fun canonicalizeGamePlatform(raw: String): String {
            val lower = raw.trim().lowercase(Locale.US)
            return when {
                lower == "pc" || lower.contains("windows") || lower.contains("steam") || lower.contains("epic") || lower.contains("gog") || lower.contains("mac") || lower.contains("linux") || lower.contains("dos") -> "PC"
                lower == "ps" || lower.contains("playstation") || lower.contains("ps5") || lower.contains("ps4") || lower.contains("ps3") || lower.contains("ps2") || lower.contains("ps1") || lower.contains("psx") || lower.contains("psp") || lower.contains("vita") -> "PS"
                lower == "xbox" || lower.contains("xbox") -> "XBox"
                lower == "switch" || lower.contains("switch") -> "Switch"
                lower == "mobile" || lower.contains("android") || lower.contains("ios") || lower.contains("iphone") || lower.contains("ipad") || lower.contains("phone") -> "Mobile"
                lower == "retro" || lower.contains("retro") || lower.contains("emulat") || lower.contains("arcade") || lower.contains("nes") || lower.contains("snes") || lower.contains("game boy") || lower.contains("gba") || lower.contains("gbc") || lower.contains("sega") || lower.contains("atari") || lower.contains("amiga") || lower.contains("commodore") || lower.contains("dreamcast") || lower.contains("gamecube") || lower.contains("nintendo 64") || lower.contains("n64") || lower.contains("wii") || lower.contains("ds") -> "Retro"
                else -> {
                    CANONICAL_GAME_PLATFORMS.firstOrNull { it.equals(raw.trim(), ignoreCase = true) } ?: raw.trim()
                }
            }
        }

        fun formatGamePlatforms(platforms: List<String>): String? {
            val canonicalList = platforms
                .map { canonicalizeGamePlatform(it) }
                .filter { it.isNotBlank() }
                .distinct()
            if (canonicalList.isEmpty()) return null
            val sorted = canonicalList.sortedWith(Comparator { a, b ->
                val idxA = CANONICAL_GAME_PLATFORMS.indexOf(a).let { if (it == -1) Int.MAX_VALUE else it }
                val idxB = CANONICAL_GAME_PLATFORMS.indexOf(b).let { if (it == -1) Int.MAX_VALUE else it }
                if (idxA != idxB) idxA.compareTo(idxB) else a.compareTo(b)
            })
            return sorted.joinToString(", ")
        }

        fun parseGamePlatforms(platformsString: String?): List<String> {
            return platformsString
                ?.split(",")
                ?.map { canonicalizeGamePlatform(it.trim()) }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?: emptyList()
        }

        fun formatCustomUnitValue(value: Double): String {
            return if (value % 1.0 == 0.0) {
                value.toLong().toString()
            } else {
                String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
            }
        }
    }
}
