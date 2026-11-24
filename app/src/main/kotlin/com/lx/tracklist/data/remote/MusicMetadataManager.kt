package com.lx.tracklist.data.remote

import android.util.Log
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
            val deezerDeferred = async { DeezerApiClient.searchTracks(clean, limit) }
            val mbDeferred = async { MusicBrainzApiClient.searchRecordings(clean, limit) }

            val deezerRes = deezerDeferred.await()
            val mbRes = mbDeferred.await()

            val deezerList = deezerRes.getOrDefault(emptyList())
            val mbList = mbRes.getOrDefault(emptyList())

            val combined = mutableListOf<MusicSearchResult>()
            val seenKeys = mutableSetOf<String>()

            // 1. Add Deezer results first (instant direct high-res cover art and track numbers)
            for (track in deezerList) {
                val key = normalizeKey(track.title, track.artist)
                if (seenKeys.add(key)) {
                    combined.add(track)
                }
            }

            // 2. Add MusicBrainz results that were not found in Deezer
            for (track in mbList) {
                val key = normalizeKey(track.title, track.artist)
                if (seenKeys.add(key)) {
                    combined.add(track)
                }
            }

            if (combined.isEmpty() && deezerRes.isFailure && mbRes.isFailure) {
                val err = deezerRes.exceptionOrNull() ?: mbRes.exceptionOrNull() ?: Exception("Failed to search music")
                Result.failure(err)
            } else {
                Result.success(combined.take(limit))
            }
        }
    }

    suspend fun fetchAlbumTracks(albumId: String, source: String = "Deezer"): Result<MusicAlbumDetails> {
        return DeezerApiClient.fetchAlbumTracks(albumId)
    }

    suspend fun resolveCoverUrl(artist: String, title: String, existingCoverUrl: String?): String? {
        if (!existingCoverUrl.isNullOrBlank() && !existingCoverUrl.contains("coverartarchive.org")) {
            return existingCoverUrl
        }
        return ITunesCoverApiClient.findCoverUrl(artist, title) ?: existingCoverUrl
    }

    private fun normalizeKey(title: String, artist: String): String {
        val cleanTitle = title.lowercase(Locale.ROOT).replace(Regex("""[^a-z0-9]"""), "")
        val cleanArtist = artist.lowercase(Locale.ROOT).replace(Regex("""[^a-z0-9]"""), "")
        return "$cleanArtist:$cleanTitle"
    }
}
