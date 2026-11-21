package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.RelatedMediaItem
import com.lx.tracklist.ui.search.AdultMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object IgdbApiClient {
    private const val TAG = "IgdbApiClient"
    private const val TWITCH_TOKEN_URL = "https://id.twitch.tv/oauth2/token"
    private const val IGDB_API_URL = "https://api.igdb.com/v4/games"
    private const val COVER_IMAGE_BASE = "https://images.igdb.com/igdb/image/upload/t_cover_big_2x"
    private const val ARTWORK_IMAGE_BASE = "https://images.igdb.com/igdb/image/upload/t_1080p"

    private val gson = Gson()
    private val rateLimitMutex = Mutex()
    private var lastRequestTime = 0L

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private suspend fun throttle() {
        rateLimitMutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < 260L) {
                delay(260L - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()
        }
    }

    /**
     * Obtains or refreshes the Twitch OAuth2 Bearer token using Client Credentials grant.
     */
    suspend fun ensureValidToken(context: Context, forceRefresh: Boolean = false): Result<String> {
        if (!forceRefresh) {
            val cached = GameApiConfig.getCachedTwitchToken(context)
            if (!cached.isNullOrBlank()) {
                return Result.success(cached)
            }
        }

        val clientId = GameApiConfig.getIgdbClientId(context)
        val clientSecret = GameApiConfig.getIgdbClientSecret(context)

        if (clientId.isBlank() || clientSecret.isBlank()) {
            return Result.failure(IllegalStateException("IGDB Client ID or Secret is not configured."))
        }

        throttle()

        val url = "$TWITCH_TOKEN_URL?client_id=$clientId&client_secret=$clientSecret&grant_type=client_credentials"
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody(null))
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Twitch OAuth failed (HTTP ${response.code}): $body"))
            }

            val json = gson.fromJson(body, JsonObject::class.java)
            val accessToken = json.get("access_token")?.asString
            val expiresIn = json.get("expires_in")?.asLong ?: 3600L

            if (!accessToken.isNullOrBlank()) {
                GameApiConfig.setCachedTwitchToken(context, accessToken, expiresIn)
                Result.success(accessToken)
            } else {
                Result.failure(Exception("Twitch OAuth returned empty access token."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining Twitch OAuth token", e)
            Result.failure(e)
        }
    }

    /**
     * Executes an Apicalypse query against the IGDB games endpoint with automatic token refresh & rate limiting.
     */
    private suspend fun executeApicalypseQuery(context: Context, query: String): Result<String> {
        var token = ensureValidToken(context).getOrElse { return Result.failure(it) }
        val clientId = GameApiConfig.getIgdbClientId(context)

        suspend fun doCall(authToken: String): okhttp3.Response {
            throttle()
            val requestBody = query.toRequestBody("text/plain".toMediaType())
            val request = Request.Builder()
                .url(IGDB_API_URL)
                .addHeader("Client-ID", clientId)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Accept", "application/json")
                .post(requestBody)
                .build()
            return httpClient.newCall(request).execute()
        }

        return try {
            var response = doCall(token)

            // Auto-refresh on 401 Unauthorized
            if (response.code == 401) {
                GameApiConfig.clearCachedTwitchToken(context)
                token = ensureValidToken(context, forceRefresh = true).getOrElse { return Result.failure(it) }
                response = doCall(token)
            }

            // Retry once on 429 Too Many Requests after backoff
            if (response.code == 429) {
                delay(1000L)
                response = doCall(token)
            }

            val body = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                Result.success(body)
            } else {
                Result.failure(Exception("IGDB API error (HTTP ${response.code}): $body"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing IGDB query", e)
            Result.failure(e)
        }
    }

    /**
     * Searches for games on IGDB.
     */
    suspend fun searchGames(context: Context, query: String, limit: Int = 20): Result<List<GameSearchResult>> {
        val cleanQuery = query.trim().replace("\"", "\\\"")
        val apicalypse = """
            search "$cleanQuery";
            fields name, alternative_names.name, first_release_date, cover.image_id, artworks.image_id, platforms.name, platforms.abbreviation, total_rating, summary, genres.name, collections.id, collections.name, franchises.id, franchises.name, game_type;
            where version_parent = null;
            limit $limit;
        """.trimIndent()

        val result = executeApicalypseQuery(context, apicalypse)
        return result.map { jsonStr ->
            parseSearchResultsJson(jsonStr)
        }
    }

    /**
     * Discovers games on IGDB with optional year, year range, genres/tags, and sorting.
     */
    suspend fun discoverGames(
        context: Context,
        year: Int? = null,
        startYear: Int? = null,
        endYear: Int? = null,
        sort: String = "total_rating_count desc",
        limit: Int = 20,
        includedTags: Set<String> = emptySet(),
        excludedTags: Set<String> = emptySet(),
        adultMode: AdultMode = AdultMode.OFF
    ): Result<List<GameSearchResult>> {
        val sYear = startYear ?: year
        val eYear = endYear ?: year
        val whereConditions = mutableListOf("version_parent = null")

        if (sYear != null || eYear != null) {
            val fromYr = sYear ?: 1970
            val toYr = eYear ?: sYear ?: (Calendar.getInstance().get(Calendar.YEAR) + 1)
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(fromYr, Calendar.JANUARY, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startSec = cal.timeInMillis / 1000L
            cal.set(toYr, Calendar.DECEMBER, 31, 23, 59, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endSec = cal.timeInMillis / 1000L
            whereConditions.add("first_release_date >= $startSec & first_release_date <= $endSec")
        } else {
            whereConditions.add("total_rating_count != null")
        }

        val whereClause = "where " + whereConditions.joinToString(" & ") + ";"
        val apicalypse = """
            fields name, alternative_names.name, first_release_date, cover.image_id, artworks.image_id, platforms.name, platforms.abbreviation, total_rating, summary, genres.name, collections.id, collections.name, franchises.id, franchises.name, game_type;
            $whereClause
            sort $sort;
            limit $limit;
        """.trimIndent()

        val result = executeApicalypseQuery(context, apicalypse)
        return result.map { jsonStr ->
            var list = parseSearchResultsJson(jsonStr)
            if (includedTags.isNotEmpty()) {
                list = list.filter { item ->
                    item.genres.any { g -> includedTags.any { inc -> inc.equals(g, ignoreCase = true) } }
                }
            }
            if (excludedTags.isNotEmpty()) {
                list = list.filterNot { item ->
                    item.genres.any { g -> excludedTags.any { exc -> exc.equals(g, ignoreCase = true) } }
                }
            }
            list
        }
    }

    /**
     * Fetches detailed information for a single game by its IGDB ID.
     */
    suspend fun getGameDetails(context: Context, igdbId: String): Result<GameDetails> {
        val apicalypse = """
            fields name, alternative_names.name, first_release_date, cover.image_id, artworks.image_id, platforms.name, platforms.abbreviation, total_rating, summary, genres.name, themes.name,
            collections.id, collections.name, collections.games.id, collections.games.name, collections.games.first_release_date, collections.games.cover.image_id, collections.games.game_type, collections.games.total_rating,
            franchises.id, franchises.name,
            parent_game.id, parent_game.name, parent_game.first_release_date, parent_game.cover.image_id,
            expansions.id, expansions.name, expansions.first_release_date, expansions.cover.image_id,
            dlcs.id, dlcs.name, dlcs.first_release_date, dlcs.cover.image_id,
            standalone_expansions.id, standalone_expansions.name, standalone_expansions.first_release_date, standalone_expansions.cover.image_id,
            remakes.id, remakes.name, remakes.first_release_date, remakes.cover.image_id,
            remasters.id, remasters.name, remasters.first_release_date, remasters.cover.image_id,
            external_games.category, external_games.uid;
            where id = $igdbId;
        """.trimIndent()

        val result = executeApicalypseQuery(context, apicalypse)
        return result.mapCatching { jsonStr ->
            val array = gson.fromJson(jsonStr, JsonArray::class.java)
            if (array == null || array.size() == 0) {
                throw NoSuchElementException("Game with IGDB ID $igdbId not found")
            }
            parseGameDetailsJson(array.get(0).asJsonObject, igdbId)
        }
    }

    // =========================================================================
    // JSON Parsing & Chronological Prequel / Sequel Derivation (Unit-Testable)
    // =========================================================================

    fun parseSearchResultsJson(jsonStr: String): List<GameSearchResult> {
        val array = try {
            gson.fromJson(jsonStr, JsonArray::class.java) ?: return emptyList()
        } catch (_: Exception) {
            return emptyList()
        }
        val list = mutableListOf<GameSearchResult>()
        for (element in array) {
            if (!element.isJsonObject) continue
            val obj = element.asJsonObject
            val id = obj.get("id")?.asLong?.toString() ?: continue
            val name = obj.get("name")?.asString ?: continue

            val altNames = parseStringArray(obj.getAsJsonArray("alternative_names"), "name")
            val altTitle = altNames.firstOrNull { !it.equals(name, ignoreCase = true) }

            val coverId = obj.getAsJsonObject("cover")?.get("image_id")?.asString
            val coverUrl = coverId?.let { "$COVER_IMAGE_BASE/$it.jpg" }

            val artworkId = obj.getAsJsonArray("artworks")?.firstOrNull()?.asJsonObject?.get("image_id")?.asString
            val artworkUrl = artworkId?.let { "$ARTWORK_IMAGE_BASE/$it.jpg" }

            val releaseEpoch = obj.get("first_release_date")?.asLong?.times(1000L)
            val releaseYear = releaseEpoch?.let { extractYear(it) }

            val platforms = parsePlatforms(obj.getAsJsonArray("platforms"))
            val genres = parseStringArray(obj.getAsJsonArray("genres"), "name")

            val score = obj.get("total_rating")?.asFloat?.let { it / 10f } // Normalize 0..100 to 0..10
            val summary = obj.get("summary")?.asString

            val collectionObj = obj.getAsJsonArray("collections")?.firstOrNull()?.asJsonObject
                ?: obj.getAsJsonObject("collections")
            val colId = collectionObj?.get("id")?.asLong
            val colName = collectionObj?.get("name")?.asString

            list.add(
                GameSearchResult(
                    externalId = id,
                    title = name,
                    alternateTitle = altTitle,
                    coverUrl = coverUrl,
                    artworkUrl = artworkUrl,
                    releaseDateMillis = releaseEpoch,
                    releaseYear = releaseYear,
                    platforms = platforms,
                    genres = genres,
                    score = score,
                    externalSource = "IGDB",
                    summary = summary,
                    isCollectionFranchise = colId != null,
                    collectionId = colId,
                    collectionName = colName
                )
            )
        }
        return list
    }

    fun parseGameDetailsJson(obj: JsonObject, targetId: String): GameDetails {
        val id = obj.get("id")?.asLong?.toString() ?: targetId
        val name = obj.get("name")?.asString.orEmpty()

        val altTitles = parseStringArray(obj.getAsJsonArray("alternative_names"), "name")
        val summary = obj.get("summary")?.asString

        val coverId = obj.getAsJsonObject("cover")?.get("image_id")?.asString
        val coverUrl = coverId?.let { "$COVER_IMAGE_BASE/$it.jpg" }

        val artworkId = obj.getAsJsonArray("artworks")?.firstOrNull()?.asJsonObject?.get("image_id")?.asString
        val artworkUrl = artworkId?.let { "$ARTWORK_IMAGE_BASE/$it.jpg" }

        val releaseEpoch = obj.get("first_release_date")?.asLong?.times(1000L)
        val releaseYear = releaseEpoch?.let { extractYear(it) }

        val platforms = parsePlatforms(obj.getAsJsonArray("platforms"))
        val genres = parseStringArray(obj.getAsJsonArray("genres"), "name")
        val themes = parseStringArray(obj.getAsJsonArray("themes"), "name")

        val rating = obj.get("total_rating")?.asFloat?.let { it / 10f }

        val collectionObj = obj.getAsJsonArray("collections")?.firstOrNull()?.asJsonObject
            ?: obj.getAsJsonObject("collections")
        val colId = collectionObj?.get("id")?.asLong
        val colName = collectionObj?.get("name")?.asString

        val franchiseObj = obj.getAsJsonArray("franchises")?.firstOrNull()?.asJsonObject
            ?: obj.getAsJsonObject("franchises")
        val franchiseName = franchiseObj?.get("name")?.asString

        // Steam App ID from external_games (category 1 = Steam)
        var steamAppId: String? = null
        val extGames = obj.getAsJsonArray("external_games")
        if (extGames != null) {
            for (ext in extGames) {
                if (ext.isJsonObject) {
                    val cat = ext.asJsonObject.get("category")?.asInt
                    if (cat == 1) { // 1 = Steam
                        steamAppId = ext.asJsonObject.get("uid")?.asString
                        break
                    }
                }
            }
        }

        // Parse collections.games
        val collectionGames = mutableListOf<GameSearchResult>()
        val colGamesArray = collectionObj?.getAsJsonArray("games")
        if (colGamesArray != null) {
            for (g in colGamesArray) {
                if (g.isJsonObject) {
                    val gObj = g.asJsonObject
                    val gId = gObj.get("id")?.asLong?.toString() ?: continue
                    val gName = gObj.get("name")?.asString ?: continue
                    val gEpoch = gObj.get("first_release_date")?.asLong?.times(1000L)
                    val gYear = gEpoch?.let { extractYear(it) }
                    val gCoverId = gObj.getAsJsonObject("cover")?.get("image_id")?.asString
                    val gCoverUrl = gCoverId?.let { "$COVER_IMAGE_BASE/$it.jpg" }
                    val gScore = gObj.get("total_rating")?.asFloat?.let { it / 10f }

                    collectionGames.add(
                        GameSearchResult(
                            externalId = gId,
                            title = gName,
                            coverUrl = gCoverUrl,
                            releaseDateMillis = gEpoch,
                            releaseYear = gYear,
                            score = gScore,
                            externalSource = "IGDB",
                            collectionId = colId,
                            collectionName = colName
                        )
                    )
                }
            }
        }

        // Derive related games (Prequel, Sequel, Part of Series, DLC, Remake, etc.)
        val relatedItems = deriveRelatedItems(obj, id, colGamesArray)

        return GameDetails(
            externalId = id,
            externalSource = "IGDB",
            title = name,
            alternateTitles = altTitles,
            summary = summary,
            coverUrl = coverUrl,
            artworkUrl = artworkUrl,
            releaseDateMillis = releaseEpoch,
            releaseYear = releaseYear,
            platforms = platforms,
            genres = genres,
            themes = themes,
            rating = rating,
            ratingSource = "IGDB",
            collectionId = colId,
            collectionName = colName,
            collectionGames = collectionGames,
            franchiseName = franchiseName,
            relatedItems = relatedItems,
            steamAppId = steamAppId
        )
    }

    /**
     * Resolves chronological Prequel/Sequel from collection main games, plus DLCs, Remakes, and Expansions.
     */
    fun deriveRelatedItems(
        obj: JsonObject,
        currentId: String,
        colGamesArray: JsonArray?
    ): List<RelatedMediaItem> {
        val result = mutableListOf<RelatedMediaItem>()

        // 1. Main-line series chronology from collection
        if (colGamesArray != null) {
            data class MainlineCandidate(val id: String, val name: String, val releaseEpoch: Long, val coverId: String?, val score: Float?)
            val candidates = mutableListOf<MainlineCandidate>()

            for (g in colGamesArray) {
                if (!g.isJsonObject) continue
                val gObj = g.asJsonObject
                val gType = gObj.get("game_type")?.asInt ?: 0
                // Mainline games typically have game_type 0 (main_game)
                if (gType == 0) {
                    val gId = gObj.get("id")?.asLong?.toString() ?: continue
                    val gName = gObj.get("name")?.asString ?: continue
                    val gEpoch = gObj.get("first_release_date")?.asLong?.times(1000L) ?: Long.MAX_VALUE
                    val gCover = gObj.getAsJsonObject("cover")?.get("image_id")?.asString
                    val gScore = gObj.get("total_rating")?.asFloat?.let { it / 10f }
                    candidates.add(MainlineCandidate(gId, gName, gEpoch, gCover, gScore))
                }
            }

            // Sort chronologically ascending
            candidates.sortBy { it.releaseEpoch }

            val currentIndex = candidates.indexOfFirst { it.id == currentId }
            if (currentIndex != -1) {
                for (i in candidates.indices) {
                    if (i == currentIndex) continue
                    val c = candidates[i]
                    val rel = when (i) {
                        currentIndex - 1 -> "Prequel"
                        currentIndex + 1 -> "Sequel"
                        else -> "Part of Series"
                    }
                    val yr = if (c.releaseEpoch < Long.MAX_VALUE) extractYear(c.releaseEpoch) else null
                    val cov = c.coverId?.let { "$COVER_IMAGE_BASE/$it.jpg" }
                    result.add(
                        RelatedMediaItem(
                            externalId = c.id,
                            title = c.name,
                            relationship = rel,
                            mediaType = MediaType.GAME,
                            posterUrl = cov,
                            releaseYear = yr,
                            score = c.score,
                            externalSource = "IGDB"
                        )
                    )
                }
            } else {
                // If current game wasn't located in the mainline list, add all as Part of Series
                for (c in candidates) {
                    if (c.id == currentId) continue
                    val yr = if (c.releaseEpoch < Long.MAX_VALUE) extractYear(c.releaseEpoch) else null
                    val cov = c.coverId?.let { "$COVER_IMAGE_BASE/$it.jpg" }
                    result.add(
                        RelatedMediaItem(
                            externalId = c.id,
                            title = c.name,
                            relationship = "Part of Series",
                            mediaType = MediaType.GAME,
                            posterUrl = cov,
                            releaseYear = yr,
                            score = c.score,
                            externalSource = "IGDB"
                        )
                    )
                }
            }
        }

        // 2. Parent Game
        parseRelatedArray(obj.getAsJsonObject("parent_game")?.let { JsonArray().apply { add(it) } }, "Parent Game", result)

        // 3. Expansions & Standalone Expansions
        parseRelatedArray(obj.getAsJsonArray("expansions"), "Expansion", result)
        parseRelatedArray(obj.getAsJsonArray("standalone_expansions"), "Expansion", result)

        // 4. DLCs
        parseRelatedArray(obj.getAsJsonArray("dlcs"), "DLC", result)

        // 5. Remakes & Remasters
        parseRelatedArray(obj.getAsJsonArray("remakes"), "Remake", result)
        parseRelatedArray(obj.getAsJsonArray("remasters"), "Remaster", result)

        return result
    }

    private fun parseRelatedArray(array: JsonArray?, relationship: String, destination: MutableList<RelatedMediaItem>) {
        if (array == null) return
        for (item in array) {
            if (!item.isJsonObject) continue
            val obj = item.asJsonObject
            val id = obj.get("id")?.asLong?.toString() ?: continue
            val name = obj.get("name")?.asString ?: continue
            val epoch = obj.get("first_release_date")?.asLong?.times(1000L)
            val yr = epoch?.let { extractYear(it) }
            val coverId = obj.getAsJsonObject("cover")?.get("image_id")?.asString
            val cov = coverId?.let { "$COVER_IMAGE_BASE/$it.jpg" }

            destination.add(
                RelatedMediaItem(
                    externalId = id,
                    title = name,
                    relationship = relationship,
                    mediaType = MediaType.GAME,
                    posterUrl = cov,
                    releaseYear = yr,
                    externalSource = "IGDB"
                )
            )
        }
    }

    private fun parseStringArray(array: JsonArray?, key: String): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (elem in array) {
            if (elem.isJsonObject) {
                val str = elem.asJsonObject.get(key)?.asString?.trim()
                if (!str.isNullOrBlank()) list.add(str)
            }
        }
        return list
    }

    private fun parsePlatforms(array: JsonArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (elem in array) {
            if (elem.isJsonObject) {
                val obj = elem.asJsonObject
                val name = obj.get("name")?.asString?.trim()
                val abbrev = obj.get("abbreviation")?.asString?.trim()
                val clean = when {
                    !name.isNullOrBlank() -> name
                    !abbrev.isNullOrBlank() -> abbrev
                    else -> null
                }
                if (!clean.isNullOrBlank() && !list.contains(clean)) {
                    list.add(clean)
                }
            }
        }
        return list
    }

    private fun extractYear(millis: Long): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = millis
        return cal.get(Calendar.YEAR).toString()
    }
}
