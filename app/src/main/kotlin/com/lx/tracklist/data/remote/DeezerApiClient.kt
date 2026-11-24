package com.lx.tracklist.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object DeezerApiClient {
    private const val TAG = "DeezerApiClient"
    private val gson = Gson()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun searchTracks(query: String, limit: Int = 20): Result<List<MusicSearchResult>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext Result.success(emptyList())

        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val url = "${MusicApiConfig.DEEZER_BASE_URL}/search/track?q=$encodedQuery&limit=$limit"

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Deezer search failed with HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.success(emptyList())
            val root = gson.fromJson(body, JsonObject::class.java)
            val dataArray = root.getAsJsonArray("data") ?: return@withContext Result.success(emptyList())

            val results = mutableListOf<MusicSearchResult>()
            for (i in 0 until dataArray.size()) {
                val item = dataArray.get(i).asJsonObject
                val trackId = item.get("id")?.asString ?: continue
                val title = item.get("title")?.asString ?: "Unknown Track"
                val durationSec = item.get("duration")?.asInt
                val previewUrl = item.get("preview")?.asString
                val link = item.get("link")?.asString

                val artistObj = item.getAsJsonObject("artist")
                val artistName = artistObj?.get("name")?.asString ?: "Unknown Artist"

                val albumObj = item.getAsJsonObject("album")
                val albumId = albumObj?.get("id")?.asString
                val albumTitle = albumObj?.get("title")?.asString
                val coverXl = albumObj?.get("cover_xl")?.asString
                    ?: albumObj?.get("cover_big")?.asString
                    ?: albumObj?.get("cover_medium")?.asString

                val trackPos = item.get("track_position")?.asInt

                results.add(
                    MusicSearchResult(
                        externalId = trackId,
                        externalSource = "Deezer",
                        title = title,
                        artist = artistName,
                        albumName = albumTitle,
                        trackNumber = trackPos,
                        durationSeconds = durationSec,
                        coverUrl = coverXl,
                        albumCoverUrl = coverXl,
                        musicAlbumId = albumId,
                        webUrl = link ?: "https://www.deezer.com/track/$trackId",
                        previewUrl = previewUrl
                    )
                )
            }
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Deezer track search", e)
            Result.failure(e)
        }
    }

    suspend fun fetchAlbumTracks(albumId: String): Result<MusicAlbumDetails> = withContext(Dispatchers.IO) {
        try {
            val url = "${MusicApiConfig.DEEZER_BASE_URL}/album/$albumId"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Deezer fetch album failed with HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty Deezer body"))
            val root = gson.fromJson(body, JsonObject::class.java)

            val title = root.get("title")?.asString ?: "Unknown Album"
            val coverUrl = root.get("cover_xl")?.asString ?: root.get("cover_big")?.asString
            val releaseDate = root.get("release_date")?.asString
            val releaseYear = releaseDate?.take(4)
            val releaseMillis = releaseDate?.let { parseDateToMillis(it) }

            val artistName = root.getAsJsonObject("artist")?.get("name")?.asString ?: "Unknown Artist"

            val genres = mutableListOf<String>()
            root.getAsJsonObject("genres")?.getAsJsonArray("data")?.forEach {
                if (it.isJsonObject) {
                    it.asJsonObject.get("name")?.asString?.let { g -> genres.add(g) }
                }
            }

            val tracks = mutableListOf<MusicSearchResult>()
            root.getAsJsonObject("tracks")?.getAsJsonArray("data")?.forEachIndexed { idx, elem ->
                if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    val trackId = obj.get("id")?.asString ?: idx.toString()
                    val trackTitle = obj.get("title")?.asString ?: "Track ${idx + 1}"
                    val dur = obj.get("duration")?.asInt
                    val trackPos = obj.get("track_position")?.asInt ?: (idx + 1)
                    val trackArtist = obj.getAsJsonObject("artist")?.get("name")?.asString ?: artistName
                    val preview = obj.get("preview")?.asString
                    val link = obj.get("link")?.asString

                    tracks.add(
                        MusicSearchResult(
                            externalId = trackId,
                            externalSource = "Deezer",
                            title = trackTitle,
                            artist = trackArtist,
                            albumName = title,
                            trackNumber = trackPos,
                            durationSeconds = dur,
                            releaseYear = releaseYear,
                            releaseDateMillis = releaseMillis,
                            coverUrl = coverUrl,
                            albumCoverUrl = coverUrl,
                            genres = genres,
                            musicAlbumId = albumId,
                            webUrl = link ?: "https://www.deezer.com/track/$trackId",
                            previewUrl = preview
                        )
                    )
                }
            }

            Result.success(
                MusicAlbumDetails(
                    albumId = albumId,
                    title = title,
                    artist = artistName,
                    coverUrl = coverUrl,
                    releaseYear = releaseYear,
                    releaseDateMillis = releaseMillis,
                    genres = genres,
                    tracks = tracks,
                    externalSource = "Deezer",
                    webUrl = "https://www.deezer.com/album/$albumId"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching Deezer album $albumId", e)
            Result.failure(e)
        }
    }

    private fun parseDateToMillis(dateStr: String): Long? {
        return try {
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            df.parse(dateStr)?.time
        } catch (_: Exception) {
            null
        }
    }
}
