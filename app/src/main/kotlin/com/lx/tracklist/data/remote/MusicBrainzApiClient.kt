package com.lx.tracklist.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

object MusicBrainzApiClient {
    private const val TAG = "MusicBrainzApiClient"
    private val gson = Gson()
    private val rateLimitMutex = Mutex()
    private var lastRequestTime = 0L

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private suspend fun throttle() {
        rateLimitMutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < 1100L) {
                delay(1100L - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()
        }
    }

    suspend fun searchRecordings(query: String, limit: Int = 20): Result<List<MusicSearchResult>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext Result.success(emptyList())

        try {
            throttle()
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val url = "${MusicApiConfig.MUSICBRAINZ_BASE_URL}/recording?query=$encodedQuery&fmt=json&limit=$limit"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", MusicApiConfig.MUSICBRAINZ_USER_AGENT)
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("MusicBrainz returned HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.success(emptyList())
            val root = gson.fromJson(body, JsonObject::class.java)
            val recordingsArray = root.getAsJsonArray("recordings") ?: return@withContext Result.success(emptyList())

            val results = mutableListOf<MusicSearchResult>()
            for (i in 0 until recordingsArray.size()) {
                val rec = recordingsArray.get(i).asJsonObject
                val mbid = rec.get("id")?.asString ?: continue
                val title = rec.get("title")?.asString ?: "Unknown Track"
                val lengthMs = rec.get("length")?.asLong
                val durationSec = lengthMs?.let { (it / 1000).toInt() }

                val artistCredits = rec.getAsJsonArray("artist-credit")
                val artistBuilder = StringBuilder()
                if (artistCredits != null) {
                    for (a in 0 until artistCredits.size()) {
                        val acElem = artistCredits.get(a)
                        if (acElem.isJsonObject) {
                            val acObj = acElem.asJsonObject
                            val name = acObj.get("name")?.asString.orEmpty()
                            val joinPhrase = acObj.get("joinphrase")?.asString.orEmpty()
                            artistBuilder.append(name).append(joinPhrase)
                        }
                    }
                }
                val artistName = artistBuilder.toString().ifBlank { "Unknown Artist" }

                val releases = rec.getAsJsonArray("releases")
                var albumName: String? = null
                var albumMbid: String? = null
                var releaseMbid: String? = null
                var releaseDate: String? = null
                var trackNum: Int? = null

                if (releases != null && releases.size() > 0) {
                    val firstRelease = releases.get(0).asJsonObject
                    releaseMbid = firstRelease.get("id")?.asString
                    albumName = firstRelease.get("title")?.asString
                    releaseDate = firstRelease.get("date")?.asString
                    albumMbid = firstRelease.getAsJsonObject("release-group")?.get("id")?.asString

                    val mediaArray = firstRelease.getAsJsonArray("media")
                    if (mediaArray != null && mediaArray.size() > 0) {
                        val trackArray = mediaArray.get(0).asJsonObject.getAsJsonArray("track")
                        if (trackArray != null && trackArray.size() > 0) {
                            trackNum = trackArray.get(0).asJsonObject.get("number")?.asString?.toIntOrNull()
                        }
                    }
                }

                val coverUrl = releaseMbid?.let { "${MusicApiConfig.COVER_ART_ARCHIVE_BASE_URL}/release/$it/front-500" }
                    ?: albumMbid?.let { "${MusicApiConfig.COVER_ART_ARCHIVE_BASE_URL}/release-group/$it/front-500" }

                val releaseYear = releaseDate?.take(4)
                val releaseDateMillis = releaseDate?.let { parseDateToMillis(it) }

                val tags = mutableListOf<String>()
                rec.getAsJsonArray("tags")?.forEach { t ->
                    if (t.isJsonObject) {
                        t.asJsonObject.get("name")?.asString?.let { tags.add(it) }
                    }
                }

                val detectedLang = ITunesApiClient.detectLanguageFromText("$title $artistName ${albumName.orEmpty()}")

                results.add(
                    MusicSearchResult(
                        externalId = mbid,
                        externalSource = "MusicBrainz",
                        title = title,
                        artist = artistName,
                        albumName = albumName,
                        trackNumber = trackNum,
                        durationSeconds = durationSec,
                        releaseYear = releaseYear,
                        releaseDateMillis = releaseDateMillis,
                        coverUrl = coverUrl,
                        albumCoverUrl = coverUrl,
                        genres = tags,
                        musicAlbumId = albumMbid ?: releaseMbid,
                        webUrl = "https://musicbrainz.org/recording/$mbid",
                        originalLanguage = detectedLang
                    )
                )
            }
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during MusicBrainz search", e)
            Result.failure(e)
        }
    }

    private fun parseDateToMillis(dateStr: String): Long? {
        return try {
            val parts = dateStr.split("-")
            val cal = java.util.Calendar.getInstance()
            cal.clear()
            val year = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
            val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
            cal.set(year, month, day)
            cal.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    suspend fun lookupSpotifyUrlForRecording(mbid: String): String? = withContext(Dispatchers.IO) {
        val clean = mbid.trim()
        if (clean.isBlank()) return@withContext null
        try {
            throttle()
            val url = "${MusicApiConfig.MUSICBRAINZ_BASE_URL}/recording/$clean?fmt=json&inc=url-rels"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", MusicApiConfig.MUSICBRAINZ_USER_AGENT)
                .header("Accept", "application/json")
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val root = gson.fromJson(body, JsonObject::class.java)
            val relations = root.getAsJsonArray("relations") ?: return@withContext null
            for (i in 0 until relations.size()) {
                val rel = relations.get(i).asJsonObject
                val resUrl = rel.getAsJsonObject("url")?.get("resource")?.asString ?: continue
                if (resUrl.contains("open.spotify.com/track/", ignoreCase = true)) {
                    return@withContext resUrl
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "MusicBrainz url-rels lookup failed for $mbid: ${e.message}")
            null
        }
    }

    suspend fun fetchAlbumTracks(albumOrReleaseMbid: String): Result<MusicAlbumDetails> = withContext(Dispatchers.IO) {
        val cleanMbid = albumOrReleaseMbid.trim()
        if (cleanMbid.isBlank()) return@withContext Result.failure(IllegalArgumentException("Empty MBID"))

        try {
            // Try treating ID as a release-group MBID first to locate an official release
            var releaseId: String? = null
            throttle()
            val rgUrl = "${MusicApiConfig.MUSICBRAINZ_BASE_URL}/release-group/$cleanMbid?inc=releases&fmt=json"
            val rgReq = Request.Builder()
                .url(rgUrl)
                .header("User-Agent", MusicApiConfig.MUSICBRAINZ_USER_AGENT)
                .header("Accept", "application/json")
                .build()
            val rgRes = httpClient.newCall(rgReq).execute()
            if (rgRes.isSuccessful) {
                val rgBody = rgRes.body?.string()
                if (!rgBody.isNullOrBlank()) {
                    val rgRoot = gson.fromJson(rgBody, JsonObject::class.java)
                    val releases = rgRoot.getAsJsonArray("releases")
                    if (releases != null && releases.size() > 0) {
                        var fallbackId: String? = null
                        for (i in 0 until releases.size()) {
                            val rel = releases.get(i).asJsonObject
                            val id = rel.get("id")?.asString ?: continue
                            if (fallbackId == null) fallbackId = id
                            if (rel.get("status")?.asString.equals("Official", ignoreCase = true)) {
                                releaseId = id
                                break
                            }
                        }
                        if (releaseId == null) releaseId = fallbackId
                    }
                }
            }

            val targetReleaseId = releaseId ?: cleanMbid
            throttle()
            val relUrl = "${MusicApiConfig.MUSICBRAINZ_BASE_URL}/release/$targetReleaseId?inc=recordings+artist-credits&fmt=json"
            val relReq = Request.Builder()
                .url(relUrl)
                .header("User-Agent", MusicApiConfig.MUSICBRAINZ_USER_AGENT)
                .header("Accept", "application/json")
                .build()
            val relRes = httpClient.newCall(relReq).execute()
            if (!relRes.isSuccessful) {
                return@withContext Result.failure(Exception("MusicBrainz release lookup returned HTTP ${relRes.code}"))
            }
            val relBody = relRes.body?.string() ?: return@withContext Result.failure(Exception("Empty MusicBrainz release body"))
            val relRoot = gson.fromJson(relBody, JsonObject::class.java)

            val albumTitle = relRoot.get("title")?.asString ?: "Unknown Album"
            val releaseDateStr = relRoot.get("date")?.asString
            val releaseYear = releaseDateStr?.take(4)
            val releaseDateMillis = releaseDateStr?.let { parseDateToMillis(it) }
            val coverUrl = "${MusicApiConfig.COVER_ART_ARCHIVE_BASE_URL}/release/$targetReleaseId/front-500"

            val albumArtist = buildArtistCredit(relRoot.getAsJsonArray("artist-credit")).ifBlank { "Unknown Artist" }
            val tracks = mutableListOf<MusicSearchResult>()
            val mediaArray = relRoot.getAsJsonArray("media")
            if (mediaArray != null) {
                for (mIdx in 0 until mediaArray.size()) {
                    val medium = mediaArray.get(mIdx).asJsonObject
                    val discPos = medium.get("position")?.asInt ?: (mIdx + 1)
                    val trackArr = medium.getAsJsonArray("tracks") ?: continue
                    for (tIdx in 0 until trackArr.size()) {
                        val tr = trackArr.get(tIdx).asJsonObject
                        val rec = tr.getAsJsonObject("recording")
                        val recId = rec?.get("id")?.asString ?: tr.get("id")?.asString ?: continue
                        val trTitle = tr.get("title")?.asString ?: rec?.get("title")?.asString ?: "Unknown Track"
                        val trArtist = buildArtistCredit(tr.getAsJsonArray("artist-credit") ?: rec?.getAsJsonArray("artist-credit"))
                            .ifBlank { albumArtist }
                        val trNum = tr.get("position")?.asInt ?: tr.get("number")?.asString?.toIntOrNull() ?: (tIdx + 1)
                        val lenMs = tr.get("length")?.asLong ?: rec?.get("length")?.asLong
                        val durSec = lenMs?.let { (it / 1000).toInt() }
                        val detectedLang = ITunesApiClient.detectLanguageFromText("$trTitle $trArtist $albumTitle")
                        tracks.add(
                            MusicSearchResult(
                                externalId = recId,
                                externalSource = "MusicBrainz",
                                title = trTitle,
                                artist = trArtist,
                                albumName = albumTitle,
                                trackNumber = trNum,
                                discNumber = discPos,
                                durationSeconds = durSec,
                                releaseYear = releaseYear,
                                releaseDateMillis = releaseDateMillis,
                                coverUrl = coverUrl,
                                albumCoverUrl = coverUrl,
                                musicAlbumId = cleanMbid,
                                webUrl = "https://musicbrainz.org/recording/$recId",
                                originalLanguage = detectedLang
                            )
                        )
                    }
                }
            }

            Result.success(
                MusicAlbumDetails(
                    albumId = cleanMbid,
                    title = albumTitle,
                    artist = albumArtist,
                    coverUrl = coverUrl,
                    releaseYear = releaseYear,
                    releaseDateMillis = releaseDateMillis,
                    totalTracks = tracks.size,
                    tracks = tracks,
                    externalSource = "MusicBrainz",
                    webUrl = "https://musicbrainz.org/release/$targetReleaseId"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching MusicBrainz album tracks for $albumOrReleaseMbid", e)
            Result.failure(e)
        }
    }

    private fun buildArtistCredit(artistCredits: com.google.gson.JsonArray?): String {
        if (artistCredits == null) return ""
        val sb = StringBuilder()
        for (a in 0 until artistCredits.size()) {
            val acElem = artistCredits.get(a)
            if (acElem.isJsonObject) {
                val acObj = acElem.asJsonObject
                val name = acObj.get("name")?.asString.orEmpty()
                val joinPhrase = acObj.get("joinphrase")?.asString.orEmpty()
                sb.append(name).append(joinPhrase)
            }
        }
        return sb.toString()
    }
}
