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
import com.lx.tracklist.ui.search.GamePlatformRegistry
import com.lx.tracklist.ui.search.GameTagRegistry
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
    private const val IGDB_TIME_TO_BEAT_URL = "https://api.igdb.com/v4/game_time_to_beats"
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
     * Executes an Apicalypse query against an IGDB endpoint with automatic token refresh & rate limiting.
     */
    private suspend fun executeApicalypseQuery(
        context: Context,
        query: String,
        endpointUrl: String = IGDB_API_URL
    ): Result<String> {
        var token = ensureValidToken(context).getOrElse { return Result.failure(it) }
        val clientId = GameApiConfig.getIgdbClientId(context)

        suspend fun doCall(authToken: String): okhttp3.Response {
            throttle()
            val requestBody = query.toRequestBody("text/plain".toMediaType())
            val request = Request.Builder()
                .url(endpointUrl)
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
     * Builds the Apicalypse query string for searching games on IGDB with optional platforms, tags, years, and adult mode.
     */
    fun buildSearchApicalypseQuery(
        query: String,
        limit: Int = 20,
        page: Int = 1,
        includedPlatforms: Set<String> = emptySet(),
        excludedPlatforms: Set<String> = emptySet(),
        includedTags: Set<String> = emptySet(),
        excludedTags: Set<String> = emptySet(),
        adultMode: AdultMode = AdultMode.OFF,
        year: Int? = null,
        startYear: Int? = null,
        endYear: Int? = null
    ): String {
        val cleanQuery = query.trim().replace("\"", "\\\"")
        val searchClause = if (cleanQuery.isNotEmpty()) "search \"$cleanQuery\";\n" else ""
        val whereConditions = mutableListOf("version_parent = null")

        val sYear = startYear ?: year
        val eYear = endYear ?: year
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
        }

        val includedGenreIds = includedTags.mapNotNull { tag ->
            tag.toIntOrNull() ?: GameTagRegistry.findGenre(tag)?.igdbId
        }.distinct()

        val excludedGenreIds = excludedTags.mapNotNull { tag ->
            tag.toIntOrNull() ?: GameTagRegistry.findGenre(tag)?.igdbId
        }.distinct()

        if (includedGenreIds.isNotEmpty()) {
            whereConditions.add("genres = [${includedGenreIds.joinToString(",")}]")
        }
        if (excludedGenreIds.isNotEmpty()) {
            whereConditions.add("genres != (${excludedGenreIds.joinToString(",")})")
        }

        val includedPlatformIds = includedPlatforms.flatMap { plat ->
            plat.toIntOrNull()?.let { listOf(it) } ?: GamePlatformRegistry.findOption(plat)?.igdbPlatformIds ?: emptyList()
        }.distinct()

        val excludedPlatformIds = excludedPlatforms.flatMap { plat ->
            plat.toIntOrNull()?.let { listOf(it) } ?: GamePlatformRegistry.findOption(plat)?.igdbPlatformIds ?: emptyList()
        }.distinct()

        if (includedPlatformIds.isNotEmpty()) {
            whereConditions.add("platforms = (${includedPlatformIds.joinToString(",")})")
        }
        if (excludedPlatformIds.isNotEmpty()) {
            whereConditions.add("platforms != (${excludedPlatformIds.joinToString(",")})")
        }

        when (adultMode) {
            AdultMode.ONLY -> whereConditions.add("themes = (42)")
            AdultMode.OFF -> whereConditions.add("themes != (42)")
            AdultMode.INCLUDE -> { /* No restriction */ }
        }

        val whereClause = if (whereConditions.isNotEmpty()) "where " + whereConditions.joinToString(" & ") + ";" else ""
        val offset = ((page - 1) * limit).coerceAtLeast(0)
        val offsetClause = if (offset > 0) "offset $offset;\n" else ""
        return """
            $searchClause
            fields name, alternative_names.name, first_release_date, cover.image_id, artworks.image_id, platforms.name, platforms.abbreviation, total_rating, summary, genres.name, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, collections.id, collections.name, franchises.id, franchises.name, game_type;
            $whereClause
            limit $limit;
            $offsetClause
        """.trimIndent().trim()
    }

    /**
     * Searches for games on IGDB with optional platform, tag, year, and adult filters.
     */
    suspend fun searchGames(
        context: Context,
        query: String,
        limit: Int = 20,
        page: Int = 1,
        includedPlatforms: Set<String> = emptySet(),
        excludedPlatforms: Set<String> = emptySet(),
        includedTags: Set<String> = emptySet(),
        excludedTags: Set<String> = emptySet(),
        adultMode: AdultMode = AdultMode.OFF,
        year: Int? = null,
        startYear: Int? = null,
        endYear: Int? = null
    ): Result<List<GameSearchResult>> {
        val apicalypse = buildSearchApicalypseQuery(
            query = query,
            limit = limit,
            page = page,
            includedPlatforms = includedPlatforms,
            excludedPlatforms = excludedPlatforms,
            includedTags = includedTags,
            excludedTags = excludedTags,
            adultMode = adultMode,
            year = year,
            startYear = startYear,
            endYear = endYear
        )

        val result = executeApicalypseQuery(context, apicalypse)
        return result.map { jsonStr ->
            var list = parseSearchResultsJson(jsonStr)
            if (includedTags.isNotEmpty()) {
                list = list.filter { item ->
                    includedTags.all { inc ->
                        item.genres.any { g -> inc.equals(g, ignoreCase = true) }
                    }
                }
            }
            if (excludedTags.isNotEmpty()) {
                list = list.filterNot { item ->
                    excludedTags.any { exc ->
                        item.genres.any { g -> exc.equals(g, ignoreCase = true) }
                    }
                }
            }
            if (includedPlatforms.isNotEmpty()) {
                list = list.filter { item ->
                    GamePlatformRegistry.matchesAnyPlatform(includedPlatforms, item.platforms)
                }
            }
            if (excludedPlatforms.isNotEmpty()) {
                list = list.filterNot { item ->
                    GamePlatformRegistry.isExcluded(excludedPlatforms, item.platforms)
                }
            }
            list
        }
    }

    /**
     * Builds the Apicalypse query string for discovering games on IGDB, translating included and excluded
     * genre tags into IGDB genre IDs for server-side filtering.
     */
    fun buildDiscoverApicalypseQuery(
        year: Int? = null,
        startYear: Int? = null,
        endYear: Int? = null,
        sort: String = "total_rating_count desc",
        limit: Int = 20,
        page: Int = 1,
        includedTags: Set<String> = emptySet(),
        excludedTags: Set<String> = emptySet(),
        adultMode: AdultMode = AdultMode.OFF,
        includedPlatforms: Set<String> = emptySet(),
        excludedPlatforms: Set<String> = emptySet()
    ): String {
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

        val includedGenreIds = includedTags.mapNotNull { tag ->
            tag.toIntOrNull() ?: GameTagRegistry.findGenre(tag)?.igdbId
        }.distinct()

        val excludedGenreIds = excludedTags.mapNotNull { tag ->
            tag.toIntOrNull() ?: GameTagRegistry.findGenre(tag)?.igdbId
        }.distinct()

        // Square brackets [id1, id2] enforce AND logic in Apicalypse (must contain ALL specified genres)
        if (includedGenreIds.isNotEmpty()) {
            whereConditions.add("genres = [${includedGenreIds.joinToString(",")}]")
        }
        // Parentheses (id1, id2) enforce NOT ANY in Apicalypse (must not contain ANY of the excluded genres)
        if (excludedGenreIds.isNotEmpty()) {
            whereConditions.add("genres != (${excludedGenreIds.joinToString(",")})")
        }

        val includedPlatformIds = includedPlatforms.flatMap { plat ->
            plat.toIntOrNull()?.let { listOf(it) } ?: GamePlatformRegistry.findOption(plat)?.igdbPlatformIds ?: emptyList()
        }.distinct()

        val excludedPlatformIds = excludedPlatforms.flatMap { plat ->
            plat.toIntOrNull()?.let { listOf(it) } ?: GamePlatformRegistry.findOption(plat)?.igdbPlatformIds ?: emptyList()
        }.distinct()

        // Parentheses (id1, id2) enforce OR/IN logic in Apicalypse (must contain ANY of specified platforms)
        if (includedPlatformIds.isNotEmpty()) {
            whereConditions.add("platforms = (${includedPlatformIds.joinToString(",")})")
        }
        // Parentheses (id1, id2) enforce NOT ANY in Apicalypse (must not contain ANY of the excluded platforms)
        if (excludedPlatformIds.isNotEmpty()) {
            whereConditions.add("platforms != (${excludedPlatformIds.joinToString(",")})")
        }

        // Adult mode filtering (Theme 42 is 'Erotic' in IGDB)
        when (adultMode) {
            AdultMode.ONLY -> whereConditions.add("themes = (42)")
            AdultMode.OFF -> whereConditions.add("themes != (42)")
            AdultMode.INCLUDE -> { /* No restriction */ }
        }

        val whereClause = "where " + whereConditions.joinToString(" & ") + ";"
        val offset = ((page - 1) * limit).coerceAtLeast(0)
        val offsetClause = if (offset > 0) "offset $offset;\n" else ""
        return """
            fields name, alternative_names.name, first_release_date, cover.image_id, artworks.image_id, platforms.name, platforms.abbreviation, total_rating, summary, genres.name, involved_companies.company.name, involved_companies.developer, involved_companies.publisher, collections.id, collections.name, franchises.id, franchises.name, game_type;
            $whereClause
            sort $sort;
            limit $limit;
            $offsetClause
        """.trimIndent().trim()
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
        page: Int = 1,
        includedTags: Set<String> = emptySet(),
        excludedTags: Set<String> = emptySet(),
        adultMode: AdultMode = AdultMode.OFF,
        includedPlatforms: Set<String> = emptySet(),
        excludedPlatforms: Set<String> = emptySet()
    ): Result<List<GameSearchResult>> {
        val apicalypse = buildDiscoverApicalypseQuery(
            year = year,
            startYear = startYear,
            endYear = endYear,
            sort = sort,
            limit = limit,
            page = page,
            includedTags = includedTags,
            excludedTags = excludedTags,
            adultMode = adultMode,
            includedPlatforms = includedPlatforms,
            excludedPlatforms = excludedPlatforms
        )

        val result = executeApicalypseQuery(context, apicalypse)
        return result.map { jsonStr ->
            var list = parseSearchResultsJson(jsonStr)
            if (includedTags.isNotEmpty()) {
                list = list.filter { item ->
                    includedTags.all { inc ->
                        item.genres.any { g -> inc.equals(g, ignoreCase = true) }
                    }
                }
            }
            if (excludedTags.isNotEmpty()) {
                list = list.filterNot { item ->
                    excludedTags.any { exc ->
                        item.genres.any { g -> exc.equals(g, ignoreCase = true) }
                    }
                }
            }
            if (includedPlatforms.isNotEmpty()) {
                list = list.filter { item ->
                    GamePlatformRegistry.matchesAnyPlatform(includedPlatforms, item.platforms)
                }
            }
            if (excludedPlatforms.isNotEmpty()) {
                list = list.filterNot { item ->
                    GamePlatformRegistry.isExcluded(excludedPlatforms, item.platforms)
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
            involved_companies.company.name, involved_companies.developer, involved_companies.publisher,
            collections.id, collections.name, collections.games.id, collections.games.name, collections.games.first_release_date, collections.games.cover.image_id, collections.games.game_type, collections.games.total_rating, collections.games.platforms.name, collections.games.platforms.abbreviation,
            franchises.id, franchises.name,
            parent_game.id, parent_game.name, parent_game.first_release_date, parent_game.cover.image_id, parent_game.platforms.name, parent_game.platforms.abbreviation,
            expansions.id, expansions.name, expansions.first_release_date, expansions.cover.image_id, expansions.platforms.name, expansions.platforms.abbreviation,
            dlcs.id, dlcs.name, dlcs.first_release_date, dlcs.cover.image_id, dlcs.platforms.name, dlcs.platforms.abbreviation,
            standalone_expansions.id, standalone_expansions.name, standalone_expansions.first_release_date, standalone_expansions.cover.image_id, standalone_expansions.platforms.name, standalone_expansions.platforms.abbreviation,
            remakes.id, remakes.name, remakes.first_release_date, remakes.cover.image_id, remakes.platforms.name, remakes.platforms.abbreviation,
            remasters.id, remasters.name, remasters.first_release_date, remasters.cover.image_id, remasters.platforms.name, remasters.platforms.abbreviation,
            external_games.category, external_games.uid;
            where id = $igdbId;
        """.trimIndent()

        val result = executeApicalypseQuery(context, apicalypse)
        return result.mapCatching { jsonStr ->
            val array = gson.fromJson(jsonStr, JsonArray::class.java)
            if (array == null || array.size() == 0) {
                throw NoSuchElementException("Game with IGDB ID $igdbId not found")
            }
            val timeToBeat = try {
                getTimeToBeat(context, igdbId).getOrNull()
            } catch (_: Exception) {
                null
            }
            parseGameDetailsJson(array.get(0).asJsonObject, igdbId, timeToBeat)
        }
    }

    /**
     * Fetches community time-to-beat metrics from IGDB (average 100% completion time, normal playthrough time).
     */
    suspend fun getTimeToBeat(context: Context, igdbId: String): Result<IgdbTimeToBeat?> {
        val cleanId = igdbId.trim()
        if (cleanId.isBlank() || !cleanId.all { it.isDigit() }) return Result.success(null)
        val apicalypse = "fields game_id, hastily, normally, completely, count; where game_id = $cleanId; limit 1;"
        val result = executeApicalypseQuery(context, apicalypse, endpointUrl = IGDB_TIME_TO_BEAT_URL)
        return result.map { jsonStr ->
            parseTimeToBeatJson(jsonStr)
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
            val (developers, publishers) = parseInvolvedCompanies(obj.getAsJsonArray("involved_companies"))

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
                    developers = developers,
                    publishers = publishers,
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

    fun parseGameDetailsJson(
        obj: JsonObject,
        targetId: String,
        timeToBeat: IgdbTimeToBeat? = null
    ): GameDetails {
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
        val (developers, publishers) = parseInvolvedCompanies(obj.getAsJsonArray("involved_companies"))

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
            developers = developers,
            publishers = publishers,
            rating = rating,
            ratingSource = "IGDB",
            collectionId = colId,
            collectionName = colName,
            collectionGames = collectionGames,
            franchiseName = franchiseName,
            relatedItems = relatedItems,
            steamAppId = steamAppId,
            completionTimeMinutes = timeToBeat?.averageCompletionMinutes,
            normalTimeMinutes = timeToBeat?.normalMinutes
        )
    }

    fun parseTimeToBeatJson(jsonStr: String): IgdbTimeToBeat? {
        val array = try {
            gson.fromJson(jsonStr, JsonArray::class.java) ?: return null
        } catch (_: Exception) {
            return null
        }
        if (array.size() == 0) return null
        val candidates = mutableListOf<IgdbTimeToBeat>()
        for (elem in array) {
            if (!elem.isJsonObject) continue
            val obj = elem.asJsonObject
            val gameId = obj.get("game_id")?.asLong ?: continue
            val completely = obj.get("completely")?.takeIf { !it.isJsonNull }?.asLong ?: 0L
            val normally = obj.get("normally")?.takeIf { !it.isJsonNull }?.asLong ?: 0L
            val hastily = (obj.get("hastily") ?: obj.get("hastly"))?.takeIf { !it.isJsonNull }?.asLong ?: 0L
            val count = obj.get("count")?.takeIf { !it.isJsonNull }?.asInt ?: 0
            candidates.add(
                IgdbTimeToBeat(
                    gameId = gameId,
                    completelySeconds = completely,
                    normallySeconds = normally,
                    hastilySeconds = hastily,
                    count = count
                )
            )
        }
        return candidates.maxByOrNull { it.count } ?: candidates.firstOrNull()
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
            data class MainlineCandidate(val id: String, val name: String, val releaseEpoch: Long, val coverId: String?, val score: Float?, val platforms: List<String> = emptyList())
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
                    val gPlatforms = parsePlatforms(gObj.getAsJsonArray("platforms"))
                    candidates.add(MainlineCandidate(gId, gName, gEpoch, gCover, gScore, gPlatforms))
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
                            externalSource = "IGDB",
                            platforms = c.platforms
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
                            externalSource = "IGDB",
                            platforms = c.platforms
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
            val platforms = parsePlatforms(obj.getAsJsonArray("platforms"))

            destination.add(
                RelatedMediaItem(
                    externalId = id,
                    title = name,
                    relationship = relationship,
                    mediaType = MediaType.GAME,
                    posterUrl = cov,
                    releaseYear = yr,
                    externalSource = "IGDB",
                    platforms = platforms
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

    fun parseInvolvedCompanies(array: JsonArray?): Pair<List<String>, List<String>> {
        if (array == null) return Pair(emptyList(), emptyList())
        val developers = mutableListOf<String>()
        val publishers = mutableListOf<String>()
        for (elem in array) {
            if (!elem.isJsonObject) continue
            val obj = elem.asJsonObject
            val companyObj = obj.getAsJsonObject("company")
            val companyName = companyObj?.get("name")?.asString?.trim()
                ?: obj.get("name")?.asString?.trim()
            if (companyName.isNullOrBlank()) continue
            val isDev = obj.get("developer")?.takeIf { !it.isJsonNull }?.asBoolean ?: false
            val isPub = obj.get("publisher")?.takeIf { !it.isJsonNull }?.asBoolean ?: false
            if (isDev && !developers.contains(companyName)) {
                developers.add(companyName)
            }
            if (isPub && !publishers.contains(companyName)) {
                publishers.add(companyName)
            }
        }
        return Pair(developers, publishers)
    }
}

data class IgdbTimeToBeat(
    val gameId: Long,
    val completelySeconds: Long = 0L,
    val normallySeconds: Long = 0L,
    val hastilySeconds: Long = 0L,
    val count: Int = 0
) {
    // Backward compatibility alias for any legacy callers
    val hastlySeconds: Long
        get() = hastilySeconds

    /**
     * Average time for 100% completion in minutes for average players (with fallbacks to normal and rush).
     */
    val averageCompletionMinutes: Int?
        get() = when {
            completelySeconds > 0 -> (completelySeconds / 60).toInt()
            normallySeconds > 0 -> (normallySeconds / 60).toInt()
            hastilySeconds > 0 -> (hastilySeconds / 60).toInt()
            else -> null
        }

    val normalMinutes: Int?
        get() = when {
            normallySeconds > 0 -> (normallySeconds / 60).toInt()
            hastilySeconds > 0 -> (hastilySeconds / 60).toInt()
            else -> null
        }
}
