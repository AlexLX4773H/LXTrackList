package com.lx.tracklist.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

object ITunesApiClient {
    private const val TAG = "ITunesApiClient"
    private val gson = Gson()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Map common music genre names to iTunes genre IDs for top charts.
     */
    val GENRE_MAP = mapOf(
        "Rock" to "21",
        "Pop" to "14",
        "Hip-Hop" to "18",
        "Hip-Hop/Rap" to "18",
        "Hip-Hop / Rap" to "18",
        "Rap" to "18",
        "Dance" to "17",
        "Electronic" to "17",
        "Electronic / EDM" to "17",
        "R&B" to "15",
        "R&B/Soul" to "15",
        "R&B / Soul" to "15",
        "Alternative" to "20",
        "Indie / Alternative" to "20",
        "Country" to "6",
        "Classical" to "5",
        "Jazz" to "11",
        "Blues" to "2",
        "Reggae" to "24",
        "Soundtrack" to "16",
        "Video Game OST" to "16",
        "Folk / Acoustic" to "1289",
        "J-Pop" to "27",
        "K-Pop" to "51",
        "Anime" to "29",
        "Anime OST" to "29",
        "Metal" to "21",
        "Punk" to "21",
        "Ambient / Lo-Fi" to "17"
    )

    private val COUNTRY_TO_DEFAULT_LANG = mapOf(
        "us" to "en", "gb" to "en", "ca" to "en", "au" to "en",
        "jp" to "ja", "kr" to "ko",
        "cn" to "zh", "tw" to "zh", "hk" to "zh",
        "es" to "es", "mx" to "es",
        "fr" to "fr",
        "de" to "de", "at" to "de", "ch" to "de",
        "it" to "it",
        "pt" to "pt", "br" to "pt",
        "ru" to "ru",
        "tr" to "tr",
        "pl" to "pl",
        "id" to "id",
        "th" to "th",
        "vn" to "vi",
        "ph" to "tl",
        "sa" to "ar", "eg" to "ar", "ae" to "ar",
        "se" to "sv",
        "dk" to "da",
        "no" to "no",
        "nl" to "nl", "be" to "nl",
        "fi" to "fi",
        "ua" to "uk",
        "gr" to "el",
        "il" to "he",
        "cz" to "cs",
        "hu" to "hu",
        "ro" to "ro",
        "my" to "ms"
    )

    fun resolveGenreId(genre: String?): String? {
        val clean = genre?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return GENRE_MAP[clean]
            ?: GENRE_MAP.entries.firstOrNull { it.key.equals(clean, ignoreCase = true) }?.value
    }

    internal fun inferLanguageFromCountry(countryCode: String?): String? {
        val clean = countryCode?.trim()?.lowercase(Locale.ROOT) ?: return null
        return COUNTRY_TO_DEFAULT_LANG[clean]
    }

    internal fun detectLanguageFromText(
        text: String,
        hintCountryCode: String? = null,
        explicitLanguage: String? = null
    ): String? {
        var hasKana = false
        var hasHangul = false
        var hasCjk = false
        var hasCyrillic = false
        var hasArabic = false
        var hasDevanagari = false
        var hasTamil = false
        var hasTelugu = false
        var hasMalayalam = false
        var hasThai = false
        var hasGreek = false
        var hasHebrew = false

        for (ch in text) {
            when (ch) {
                in '\u3040'..'\u30FF' -> hasKana = true
                in '\uAC00'..'\uD7AF', in '\u1100'..'\u11FF' -> hasHangul = true
                in '\u4E00'..'\u9FFF' -> hasCjk = true
                in '\u0400'..'\u04FF' -> hasCyrillic = true
                in '\u0600'..'\u06FF' -> hasArabic = true
                in '\u0900'..'\u097F' -> hasDevanagari = true
                in '\u0B80'..'\u0BFF' -> hasTamil = true
                in '\u0C00'..'\u0C7F' -> hasTelugu = true
                in '\u0D00'..'\u0D7F' -> hasMalayalam = true
                in '\u0E00'..'\u0E7F' -> hasThai = true
                in '\u0370'..'\u03FF' -> hasGreek = true
                in '\u0590'..'\u05FF' -> hasHebrew = true
            }
        }

        val hintCountry = hintCountryCode?.trim()?.lowercase(Locale.ROOT)
        val hintLang = explicitLanguage?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() }
        val scriptLang = when {
            hasKana -> "ja"
            hasHangul -> "ko"
            hasCjk -> if (hintLang == "ja" || hintCountry == "jp") "ja" else "zh"
            hasCyrillic -> if (hintLang == "uk" || hintCountry == "ua") "uk" else "ru"
            hasArabic -> "ar"
            hasDevanagari -> "hi"
            hasTamil -> "ta"
            hasTelugu -> "te"
            hasMalayalam -> "ml"
            hasThai -> "th"
            hasGreek -> "el"
            hasHebrew -> "he"
            else -> null
        }

        return scriptLang ?: hintLang ?: inferLanguageFromCountry(hintCountry)
    }

    suspend fun searchTracks(
        query: String,
        limit: Int = 25,
        countryCode: String? = null,
        originalLanguage: String? = null
    ): Result<List<MusicSearchResult>> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext Result.success(emptyList())

        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val countryParam = countryCode?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotBlank() }?.let { "&country=$it" } ?: ""
            val url = "${MusicApiConfig.ITUNES_SEARCH_URL}?term=$encoded&media=music&entity=song&limit=$limit$countryParam"

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("iTunes search returned HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.success(emptyList())
            val root = gson.fromJson(body, JsonObject::class.java)
            val resultsArray = root.getAsJsonArray("results") ?: return@withContext Result.success(emptyList())

            val list = mutableListOf<MusicSearchResult>()
            for (i in 0 until resultsArray.size()) {
                val item = resultsArray.get(i).asJsonObject
                val trackId = item.get("trackId")?.asString ?: continue
                val title = item.get("trackName")?.asString ?: "Unknown Track"
                val artist = item.get("artistName")?.asString ?: "Unknown Artist"
                val album = item.get("collectionName")?.asString
                val albumId = item.get("collectionId")?.asString
                val trackNum = item.get("trackNumber")?.asInt
                val durationMs = item.get("trackTimeMillis")?.asLong
                val durationSec = durationMs?.let { (it / 1000).toInt() }
                val rawArtwork = item.get("artworkUrl100")?.asString
                val coverUrl = rawArtwork?.replace("100x100bb", "600x600bb")
                val releaseDateStr = item.get("releaseDate")?.asString
                val releaseYear = releaseDateStr?.take(4)?.toIntOrNull()
                val genre = item.get("primaryGenreName")?.asString
                val webUrl = item.get("trackViewUrl")?.asString ?: "https://music.apple.com"
                val previewUrl = item.get("previewUrl")?.asString
                val detectedLang = detectLanguageFromText("$title $artist ${album.orEmpty()}", countryCode, originalLanguage)

                list.add(
                    MusicSearchResult(
                        externalId = trackId,
                        externalSource = "iTunes",
                        title = title,
                        artist = artist,
                        albumName = album,
                        trackNumber = trackNum,
                        durationSeconds = durationSec,
                        coverUrl = coverUrl,
                        albumCoverUrl = coverUrl,
                        releaseYear = releaseYear?.toString(),
                        genres = listOfNotNull(genre),
                        musicAlbumId = albumId,
                        webUrl = webUrl,
                        originalLanguage = detectedLang,
                        previewUrl = previewUrl
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "iTunes search error for query: $query", e)
            Result.failure(e)
        }
    }

    suspend fun fetchTopSongs(
        genreId: String? = null,
        limit: Int = 50,
        countryCode: String = "us",
        originalLanguage: String? = inferLanguageFromCountry(countryCode)
    ): Result<List<MusicSearchResult>> = withContext(Dispatchers.IO) {
        try {
            val cleanCountry = countryCode.trim().lowercase(Locale.ROOT).ifBlank { "us" }
            val genrePart = if (!genreId.isNullOrBlank()) "/genre=$genreId" else ""
            val url = "https://itunes.apple.com/$cleanCountry/rss/topsongs/limit=$limit$genrePart/json"

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("iTunes RSS returned HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.success(emptyList())
            val root = gson.fromJson(body, JsonObject::class.java)
            val feed = root.getAsJsonObject("feed") ?: return@withContext Result.success(emptyList())
            val entries = feed.getAsJsonArray("entry") ?: return@withContext Result.success(emptyList())

            val list = mutableListOf<MusicSearchResult>()
            for (i in 0 until entries.size()) {
                val entry = entries.get(i).asJsonObject

                val titleObj = entry.getAsJsonObject("im:name")
                val title = titleObj?.get("label")?.asString ?: continue

                val artistObj = entry.getAsJsonObject("im:artist")
                val artist = artistObj?.get("label")?.asString ?: "Unknown Artist"

                val collectionObj = entry.getAsJsonObject("im:collection")
                val album = collectionObj?.getAsJsonObject("im:name")?.get("label")?.asString

                val idObj = entry.getAsJsonObject("id")
                val idAttr = idObj?.getAsJsonObject("attributes")
                val externalId = idAttr?.get("im:id")?.asString ?: i.toString()

                // Artwork
                val imageArray = entry.getAsJsonArray("im:image")
                var coverUrl: String? = null
                if (imageArray != null && imageArray.size() > 0) {
                    val lastImg = imageArray.get(imageArray.size() - 1).asJsonObject
                    val rawUrl = lastImg.get("label")?.asString
                    coverUrl = rawUrl?.replace(Regex("""/\d+x\d+bb\."""), "/600x600bb.")
                }

                // Genre
                val categoryObj = entry.getAsJsonObject("category")
                val genre = categoryObj?.getAsJsonObject("attributes")?.get("label")?.asString
                    ?: categoryObj?.getAsJsonObject("attributes")?.get("term")?.asString

                // Release Date
                val relDateObj = entry.getAsJsonObject("im:releaseDate")
                val releaseDateStr = relDateObj?.get("label")?.asString
                val releaseYear = releaseDateStr?.take(4)?.toIntOrNull()

                // Links
                var webUrl = "https://music.apple.com"
                var previewUrl: String? = null
                val linkElem = entry.get("link")
                if (linkElem is JsonArray) {
                    for (l in 0 until linkElem.size()) {
                        val lObj = linkElem.get(l).asJsonObject
                        val attrs = lObj.getAsJsonObject("attributes")
                        val rel = attrs?.get("rel")?.asString
                        val href = attrs?.get("href")?.asString
                        if (rel == "alternate" && href != null) {
                            webUrl = href
                        } else if (rel == "enclosure" && href != null) {
                            previewUrl = href
                        }
                    }
                } else if (linkElem is JsonObject) {
                    val attrs = linkElem.getAsJsonObject("attributes")
                    webUrl = attrs?.get("href")?.asString ?: webUrl
                }

                val detectedLang = detectLanguageFromText("$title $artist ${album.orEmpty()}", cleanCountry, originalLanguage)

                list.add(
                    MusicSearchResult(
                        externalId = externalId,
                        externalSource = "Apple Music",
                        title = title,
                        artist = artist,
                        albumName = album,
                        coverUrl = coverUrl,
                        albumCoverUrl = coverUrl,
                        releaseYear = releaseYear?.toString(),
                        genres = listOfNotNull(genre),
                        webUrl = webUrl,
                        originalLanguage = detectedLang,
                        previewUrl = previewUrl
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "iTunes fetchTopSongs error", e)
            Result.failure(e)
        }
    }

    suspend fun findCoverUrl(artist: String, title: String): String? {
        val query = "$artist $title".trim()
        if (query.isBlank()) return null
        return searchTracks(query, limit = 1).getOrNull()?.firstOrNull()?.coverUrl
    }

    suspend fun lookupTrack(trackId: String): Result<MusicSearchResult?> = withContext(Dispatchers.IO) {
        val cleanId = trackId.trim()
        if (cleanId.isBlank() || !cleanId.all { it.isDigit() }) {
            return@withContext Result.success(null)
        }
        try {
            val url = "https://itunes.apple.com/lookup?id=$cleanId&entity=song"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("iTunes lookup returned HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.success(null)
            val root = gson.fromJson(body, JsonObject::class.java)
            val resultsArray = root.getAsJsonArray("results") ?: return@withContext Result.success(null)

            for (i in 0 until resultsArray.size()) {
                val item = resultsArray.get(i).asJsonObject
                val wrapper = item.get("wrapperType")?.asString
                if (wrapper != null && wrapper != "track") continue
                val id = item.get("trackId")?.asString ?: continue
                val title = item.get("trackName")?.asString ?: "Unknown Track"
                val artist = item.get("artistName")?.asString ?: "Unknown Artist"
                val album = item.get("collectionName")?.asString
                val albumId = item.get("collectionId")?.asString
                val trackNum = item.get("trackNumber")?.asInt
                val durationMs = item.get("trackTimeMillis")?.asLong
                val durationSec = durationMs?.let { (it / 1000).toInt() }
                val rawArtwork = item.get("artworkUrl100")?.asString
                val coverUrl = rawArtwork?.replace("100x100bb", "600x600bb")
                val releaseDateStr = item.get("releaseDate")?.asString
                val releaseYear = releaseDateStr?.take(4)?.toIntOrNull()
                val genre = item.get("primaryGenreName")?.asString
                val webUrl = item.get("trackViewUrl")?.asString ?: "https://music.apple.com"
                val previewUrl = item.get("previewUrl")?.asString
                val country = item.get("country")?.asString
                val detectedLang = detectLanguageFromText("$title $artist ${album.orEmpty()}", country, null)

                return@withContext Result.success(
                    MusicSearchResult(
                        externalId = id,
                        externalSource = "Apple Music",
                        title = title,
                        artist = artist,
                        albumName = album,
                        trackNumber = trackNum,
                        durationSeconds = durationSec,
                        coverUrl = coverUrl,
                        albumCoverUrl = coverUrl,
                        releaseYear = releaseYear?.toString(),
                        genres = listOfNotNull(genre),
                        musicAlbumId = albumId,
                        webUrl = webUrl,
                        originalLanguage = detectedLang,
                        previewUrl = previewUrl
                    )
                )
            }
            Result.success(null)
        } catch (e: Exception) {
            Log.e(TAG, "iTunes lookupTrack error for id=$trackId", e)
            Result.failure(e)
        }
    }

    suspend fun fetchAlbumTracks(albumId: String): Result<MusicAlbumDetails> = withContext(Dispatchers.IO) {
        try {
            val url = "https://itunes.apple.com/lookup?id=$albumId&entity=song"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("iTunes album lookup returned HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty iTunes response"))
            val root = gson.fromJson(body, JsonObject::class.java)
            val results = root.getAsJsonArray("results") ?: return@withContext Result.failure(Exception("No results in album"))

            var albumTitle = "Unknown Album"
            var artistName = "Unknown Artist"
            var coverUrl: String? = null
            var releaseYear: String? = null
            val tracks = mutableListOf<MusicSearchResult>()

            for (i in 0 until results.size()) {
                val item = results.get(i).asJsonObject
                val wrapperType = item.get("wrapperType")?.asString

                if (wrapperType == "collection") {
                    albumTitle = item.get("collectionName")?.asString ?: albumTitle
                    artistName = item.get("artistName")?.asString ?: artistName
                    coverUrl = item.get("artworkUrl100")?.asString?.replace("100x100bb", "600x600bb")
                    releaseYear = item.get("releaseDate")?.asString?.take(4)
                } else if (wrapperType == "track") {
                    val trackId = item.get("trackId")?.asString ?: continue
                    val trackTitle = item.get("trackName")?.asString ?: "Unknown Track"
                    val trackArtist = item.get("artistName")?.asString ?: artistName
                    val trackNum = item.get("trackNumber")?.asInt
                    val durationMs = item.get("trackTimeMillis")?.asLong
                    val durationSec = durationMs?.let { (it / 1000).toInt() }
                    val trackCover = item.get("artworkUrl100")?.asString?.replace("100x100bb", "600x600bb") ?: coverUrl
                    val webUrl = item.get("trackViewUrl")?.asString ?: "https://music.apple.com"
                    val preview = item.get("previewUrl")?.asString

                    tracks.add(
                        MusicSearchResult(
                            externalId = trackId,
                            externalSource = "Apple Music",
                            title = trackTitle,
                            artist = trackArtist,
                            albumName = albumTitle,
                            trackNumber = trackNum,
                            durationSeconds = durationSec,
                            coverUrl = trackCover,
                            albumCoverUrl = coverUrl,
                            releaseYear = releaseYear,
                            musicAlbumId = albumId,
                            webUrl = webUrl,
                            previewUrl = preview
                        )
                    )
                }
            }

            Result.success(
                MusicAlbumDetails(
                    albumId = albumId,
                    title = albumTitle,
                    artist = artistName,
                    coverUrl = coverUrl,
                    releaseYear = releaseYear,
                    totalTracks = tracks.size,
                    tracks = tracks,
                    externalSource = "Apple Music",
                    webUrl = "https://music.apple.com"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "iTunes fetchAlbumTracks error", e)
            Result.failure(e)
        }
    }
}
