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

    // User personal thoughts / comments about the entity
    @SerializedName("userComments")
    val userComments: String? = null
) {
    // Backward compatibility reference for legacy callers
    @get:Ignore
    val notes: String?
        get() = description
    fun getRelatedMediaItems(): List<RelatedMediaItem> {
        if (relatedEntriesJson.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<RelatedMediaItem>>() {}.type
            Gson().fromJson<List<RelatedMediaItem>>(relatedEntriesJson, type) ?: emptyList()
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
        }

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
                }
            }
            MediaType.MOVIE -> {
                if (status == MediaStatus.COMPLETED) 1f
                else if (durationMinutes != null && durationMinutes > 0 && currentTimeMinutes != null && currentTimeMinutes > 0) {
                    (currentTimeMinutes.toFloat() / durationMinutes.toFloat()).coerceIn(0f, 1f)
                } else 0f
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
                }
            }
            MediaType.MOVIE -> copy(
                status = if (status == MediaStatus.COMPLETED) MediaStatus.WATCHING else status,
                updatedAt = now
            )
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
        val endStr = if (isAiring && airedEndDate == null) "Present" else (airedEndDate?.let { sdf.format(Date(it)) } ?: "")
        return if (endStr.isNotBlank()) "$startStr - $endStr" else startStr
    }

    fun formatUserDateRange(): String {
        if (startDate == null && finishDate == null) return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startStr = startDate?.let { sdf.format(Date(it)) } ?: "?"
        val finishStr = finishDate?.let { sdf.format(Date(it)) } ?: ""
        return if (finishStr.isNotBlank()) "$startStr - $finishStr" else startStr
    }

    companion object {
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
    }
}
