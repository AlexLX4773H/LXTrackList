package com.lx.tracklist.data.remote

import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.TrackingUnit

data class MusicSearchResult(
    val externalId: String,
    val externalSource: String, // "MusicBrainz", "Deezer", "Spotify"
    val title: String,
    val artist: String,
    val alternateTitle: String? = null,
    val albumName: String? = null,
    val trackNumber: Int? = null,
    val durationSeconds: Int? = null,
    val releaseYear: String? = null,
    val releaseDateMillis: Long? = null,
    val coverUrl: String? = null,
    val genres: List<String> = emptyList(),
    val musicAlbumId: String? = null,
    val isrc: String? = null,
    val webUrl: String? = null,
    val score: Float? = null,
    val originalLanguage: String? = "en",
    val previewUrl: String? = null,
    val albumCoverUrl: String? = null,
    val relatedTracks: List<MusicSearchResult> = emptyList()
) {
    val durationFormatted: String
        get() = MediaEntry.formatMusicDuration(durationSeconds)

    fun toMediaEntry(
        userId: Long = 0L,
        status: MediaStatus = MediaStatus.PLAN_TO_WATCH,
        customCategory: String? = null,
        subCategory: String? = null
    ): MediaEntry {
        return MediaEntry(
            userId = userId,
            title = title,
            alternateTitle = alternateTitle,
            mediaType = MediaType.MUSIC,
            status = status,
            creator = artist,
            movieSeriesName = albumName,
            movieSeriesNumber = trackNumber,
            musicAlbumId = musicAlbumId,
            customCategory = customCategory,
            subCategory = subCategory,
            externalId = externalId,
            externalSource = externalSource,
            externalScore = score,
            externalScoreSource = externalSource,
            coverImageUri = coverUrl ?: albumCoverUrl,
            durationMinutes = durationSeconds, // Stores seconds for Music
            tags = genres.joinToString(", ").takeIf { it.isNotBlank() },
            description = null,
            airedStartDate = releaseDateMillis,
            trackingUnit = TrackingUnit.PERCENTAGE,
            originalLanguage = originalLanguage ?: "en"
        )
    }
}

data class MusicAlbumDetails(
    val albumId: String,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val releaseYear: String? = null,
    val releaseDateMillis: Long? = null,
    val genres: List<String> = emptyList(),
    val tracks: List<MusicSearchResult> = emptyList(),
    val externalSource: String = "Deezer",
    val webUrl: String? = null
)
