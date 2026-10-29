package com.lx.tracklist.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_entries",
    indices = [
        Index("mediaType"),
        Index("status"),
        Index("isFavorite"),
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
    val userId: Long = 1L,

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
    val startDate: Long? = null,
    val finishDate: Long? = null,
    val notes: String? = null,
    val tags: String? = null,
    val coverImageUri: String? = null,
    val priority: Priority = Priority.MEDIUM,
    val rewatchCount: Int = 0,
    val isFavorite: Boolean = false,
    val externalId: String? = null,
    val externalSource: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Video / Watch fields (Anime, Show, Movie)
    val currentEpisodes: Int = 0,
    val totalEpisodes: Int = 0,
    val seasonNumber: Int = 1,
    val currentSeasons: Int = 1,
    val totalSeasons: Int = 1,
    val episodeLengthMinutes: Int? = null,
    val isAiring: Boolean = false,

    // Reading fields (Manga, Manhwa, Book)
    val currentVolumes: Int = 0,
    val totalVolumes: Int = 0,
    val currentChapters: Int = 0,
    val totalChapters: Int = 0,
    val currentPages: Int = 0,
    val totalPages: Int = 0,
    val trackingUnit: TrackingUnit = TrackingUnit.CHAPTERS,

    // Movie fields
    val movieSeriesNumber: Int? = null,
    val movieSeriesName: String? = null,
    val durationMinutes: Int? = null
) {
    val displayProgress: String
        get() = when (mediaType) {
            MediaType.ANIME, MediaType.SHOW -> {
                val totalStr = if (totalEpisodes > 0) "$totalEpisodes" else "?"
                if (mediaType == MediaType.SHOW && (totalSeasons > 1 || currentSeasons > 1)) {
                    "S$currentSeasons • Ep $currentEpisodes/$totalStr"
                } else {
                    "$currentEpisodes/$totalStr ep"
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
                        if (currentChapters > 0 || totalChapters > 0) {
                            parts.add("Ch $currentChapters" + if (totalChapters > 0) "/$totalChapters" else "")
                        }
                        if (currentPages > 0 || totalPages > 0) {
                            parts.add("$currentPages" + if (totalPages > 0) "/$totalPages" else "" + "p")
                        }
                        if (parts.isEmpty()) {
                            val totalStr = if (totalChapters > 0) "$totalChapters" else "?"
                            "Ch $currentChapters/$totalStr"
                        } else {
                            parts.joinToString(" • ")
                        }
                    }
                    TrackingUnit.CHAPTERS -> {
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
                listOf(seriesStr, durStr).filter { it.isNotBlank() }.joinToString(" • ")
                    .ifBlank { if (status == MediaStatus.COMPLETED) "Watched" else "Planned" }
            }
        }

    val progressFraction: Float
        get() = when (mediaType) {
            MediaType.ANIME, MediaType.SHOW -> {
                if (totalEpisodes > 0) (currentEpisodes.toFloat() / totalEpisodes).coerceIn(0f, 1f)
                else if (status == MediaStatus.COMPLETED) 1f else 0f
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
                if (status == MediaStatus.COMPLETED) 1f else 0f
            }
        }

    fun incrementProgress(): MediaEntry {
        val now = System.currentTimeMillis()
        return when (mediaType) {
            MediaType.ANIME, MediaType.SHOW -> {
                val nextEp = currentEpisodes + 1
                val newStatus = if (totalEpisodes > 0 && nextEp >= totalEpisodes) MediaStatus.COMPLETED else status
                copy(
                    currentEpisodes = nextEp,
                    status = newStatus,
                    updatedAt = now,
                    finishDate = if (newStatus == MediaStatus.COMPLETED && finishDate == null) now else finishDate
                )
            }
            MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK -> {
                when (trackingUnit) {
                    TrackingUnit.VOLUMES -> {
                        val nextVol = currentVolumes + 1
                        val newStatus = if (totalVolumes > 0 && nextVol >= totalVolumes) MediaStatus.COMPLETED else status
                        copy(
                            currentVolumes = nextVol,
                            status = newStatus,
                            updatedAt = now,
                            finishDate = if (newStatus == MediaStatus.COMPLETED && finishDate == null) now else finishDate
                        )
                    }
                    TrackingUnit.PAGES -> {
                        val nextPages = currentPages + 10
                        val newStatus = if (totalPages > 0 && nextPages >= totalPages) MediaStatus.COMPLETED else status
                        copy(
                            currentPages = nextPages,
                            status = newStatus,
                            updatedAt = now,
                            finishDate = if (newStatus == MediaStatus.COMPLETED && finishDate == null) now else finishDate
                        )
                    }
                    TrackingUnit.CHAPTERS, TrackingUnit.COMBINED -> {
                        val nextCh = currentChapters + 1
                        val newStatus = if (totalChapters > 0 && nextCh >= totalChapters) MediaStatus.COMPLETED else status
                        copy(
                            currentChapters = nextCh,
                            status = newStatus,
                            updatedAt = now,
                            finishDate = if (newStatus == MediaStatus.COMPLETED && finishDate == null) now else finishDate
                        )
                    }
                }
            }
            MediaType.MOVIE -> {
                copy(
                    status = if (status == MediaStatus.COMPLETED) MediaStatus.WATCHING else MediaStatus.COMPLETED,
                    updatedAt = now,
                    finishDate = if (status != MediaStatus.COMPLETED && finishDate == null) now else finishDate
                )
            }
        }
    }

    fun getAlternateTitles(): List<String> {
        return alternateTitle
            ?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    companion object {
        fun formatAlternateTitles(titles: List<String>): String? {
            val cleaned = titles.map { it.trim() }.filter { it.isNotBlank() }
            return if (cleaned.isEmpty()) null else cleaned.joinToString("\n")
        }
    }
}
