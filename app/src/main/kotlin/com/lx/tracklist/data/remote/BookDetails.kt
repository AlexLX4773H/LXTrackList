package com.lx.tracklist.data.remote

import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.TrackingUnit

data class BookDetails(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val authors: List<String> = emptyList(),
    val publisher: String? = null,
    val publishedDate: String? = null,
    val description: String? = null,
    val pageCount: Int? = null,
    val totalChapters: Int? = null,
    val totalVolumes: Int? = null,
    val isbn10: String? = null,
    val isbn13: String? = null,
    val coverUrl: String? = null,
    val categories: List<String> = emptyList(),
    val averageScore: Float? = null,
    val sourceProvider: String = "Google Books",
    val language: String? = null,
    val infoUrl: String? = null
) {
    val authorString: String
        get() = authors.joinToString(", ")

    val primaryIsbn: String?
        get() = isbn13 ?: isbn10

    fun toJikanMediaDetails(): JikanMediaDetails {
        val isNovelOrLN = totalVolumes != null || totalChapters != null
        return JikanMediaDetails(
            malId = null,
            title = title,
            posterUrl = coverUrl,
            durationText = null,
            durationMinutes = null,
            totalEpisodes = null,
            totalChapters = totalChapters,
            totalVolumes = totalVolumes,
            airedChapters = null,
            alternateTitle = subtitle,
            airedStartDate = parsePublishDateEpoch(publishedDate),
            airedEndDate = null,
            tags = buildTagsString(),
            synopsis = description,
            isAiring = false,
            externalScore = averageScore,
            externalScoreSource = sourceProvider,
            type = if (isNovelOrLN) "Light Novel" else "Book",
            sourceProvider = sourceProvider,
            url = infoUrl,
            totalPages = pageCount,
            isbn = primaryIsbn,
            authors = authorString.takeIf { it.isNotBlank() }
        )
    }

    fun toMediaEntry(
        userId: Long,
        status: MediaStatus,
        customCategory: String? = null,
        subCategory: String? = null
    ): MediaEntry {
        val isNovelOrLN = totalVolumes != null || totalChapters != null
        val unit = when {
            pageCount != null && pageCount > 0 -> TrackingUnit.PAGES
            totalVolumes != null && totalVolumes > 0 -> TrackingUnit.VOLUMES
            totalChapters != null && totalChapters > 0 -> TrackingUnit.CHAPTERS
            else -> TrackingUnit.PAGES
        }

        return MediaEntry(
            userId = userId,
            title = title,
            alternateTitle = subtitle,
            mediaType = MediaType.BOOK,
            status = status,
            customCategory = customCategory,
            subCategory = subCategory,
            externalId = id,
            externalSource = sourceProvider,
            externalScore = averageScore,
            externalScoreSource = sourceProvider,
            coverImageUri = coverUrl,
            totalPages = pageCount ?: 0,
            totalVolumes = totalVolumes ?: 0,
            totalChapters = totalChapters ?: 0,
            trackingUnit = unit,
            tags = buildTagsString(),
            notes = description,
            airedStartDate = parsePublishDateEpoch(publishedDate),
            isAiring = false
        )
    }

    fun buildTagsString(): String {
        val list = mutableListOf<String>()
        if (authors.isNotEmpty()) list.add("Author: $authorString")
        if (!primaryIsbn.isNullOrBlank()) list.add("ISBN: $primaryIsbn")
        list.addAll(categories)
        return list.joinToString(", ")
    }

    private fun parsePublishDateEpoch(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            val parts = dateStr.split("-")
            val cal = java.util.Calendar.getInstance()
            cal.clear()
            val year = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val month = parts.getOrNull(1)?.toIntOrNull()?.minus(1) ?: 0
            val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
            cal.set(year, month, day)
            cal.timeInMillis
        } catch (_: Exception) {
            null
        }
    }
}

