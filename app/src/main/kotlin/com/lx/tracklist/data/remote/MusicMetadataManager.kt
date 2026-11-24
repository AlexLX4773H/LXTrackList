package com.lx.tracklist.data.remote

import android.util.Log
import com.lx.tracklist.ui.search.MusicDiscoveryMode
import com.lx.tracklist.ui.search.MusicSortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.util.Locale

object MusicMetadataManager {
    private const val TAG = "MusicMetadataManager"

    suspend fun searchMusic(query: String, limit: Int = 20): Result<List<MusicSearchResult>> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext Result.success(emptyList())

        supervisorScope {
            // Concurrently query iTunes (worldwide, robust, instant high-res covers) and MusicBrainz.
            val iTunesDeferred = async { ITunesApiClient.searchTracks(clean, limit) }
            val mbDeferred = async { MusicBrainzApiClient.searchRecordings(clean, limit) }

            val iTunesRes = iTunesDeferred.await()
            val mbRes = mbDeferred.await()

            val iTunesList = iTunesRes.getOrDefault(emptyList())
            val mbList = mbRes.getOrDefault(emptyList())

            val combined = mutableListOf<MusicSearchResult>()
            val seenKeys = mutableSetOf<String>()

            // 1. Add iTunes results (worldwide availability, 600x600 covers, audio previews)
            for (track in iTunesList) {
                val key = normalizeKey(track.title, track.artist)
                if (seenKeys.add(key)) {
                    combined.add(track)
                }
            }

            // 2. Add MusicBrainz results that were not found in iTunes
            for (track in mbList) {
                val key = normalizeKey(track.title, track.artist)
                if (seenKeys.add(key)) {
                    combined.add(track)
                }
            }

            if (combined.isEmpty() && iTunesRes.isFailure && mbRes.isFailure) {
                val err = iTunesRes.exceptionOrNull()
                    ?: mbRes.exceptionOrNull()
                    ?: Exception("Failed to search music")
                Result.failure(err)
            } else {
                Result.success(combined.take(limit))
            }
        }
    }

    suspend fun discoverMusic(
        mode: MusicDiscoveryMode = MusicDiscoveryMode.TOP_CHARTS,
        genre: String? = null,
        year: Int? = null,
        startYear: Int? = null,
        endYear: Int? = null,
        sortOrder: MusicSortOrder = MusicSortOrder.POPULARITY,
        limit: Int = 30
    ): Result<List<MusicSearchResult>> = withContext(Dispatchers.IO) {
        try {
            when (mode) {
                MusicDiscoveryMode.TOP_CHARTS -> {
                    ITunesApiClient.fetchTopSongs(genreId = null, limit = limit)
                }
                MusicDiscoveryMode.BY_GENRE -> {
                    val genreId = genre?.let { ITunesApiClient.GENRE_MAP[it] }
                    if (genreId != null) {
                        ITunesApiClient.fetchTopSongs(genreId = genreId, limit = limit)
                    } else {
                        val query = genre ?: "Top Hits"
                        ITunesApiClient.searchTracks(query, limit = limit)
                    }
                }
                MusicDiscoveryMode.BY_YEAR -> {
                    val targetYear = endYear ?: year ?: startYear ?: 2024
                    val query = if (!genre.isNullOrBlank()) "$genre $targetYear" else "$targetYear Hits"
                    ITunesApiClient.searchTracks(query, limit = limit)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "discoverMusic error", e)
            Result.failure(e)
        }
    }

    suspend fun fetchAlbumTracks(albumId: String, source: String = "Apple Music"): Result<MusicAlbumDetails> {
        return ITunesApiClient.fetchAlbumTracks(albumId)
    }

    suspend fun resolveCoverUrl(artist: String, title: String, existingCoverUrl: String?): String? {
        if (!existingCoverUrl.isNullOrBlank() && !existingCoverUrl.contains("coverartarchive.org")) {
            return existingCoverUrl
        }
        return ITunesApiClient.findCoverUrl(artist, title) ?: existingCoverUrl
    }

    private fun normalizeKey(title: String, artist: String): String {
        val cleanTitle = title.lowercase(Locale.ROOT).replace(Regex("""[^a-z0-9]"""), "")
        val cleanArtist = artist.lowercase(Locale.ROOT).replace(Regex("""[^a-z0-9]"""), "")
        return "$cleanArtist:$cleanTitle"
    }
}
