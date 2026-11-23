package com.lx.tracklist.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.RelatedMediaItem
import com.lx.tracklist.ui.search.AdultMode
import com.lx.tracklist.ui.search.GamePlatformRegistry
import com.lx.tracklist.ui.search.GameTagRegistry
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object RawgApiClient {
    private const val TAG = "RawgApiClient"
    private const val BASE_URL = "https://api.rawg.io/api"

    private val gson = Gson()
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun buildSearchUrl(
        apiKey: String,
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
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val sYear = startYear ?: year
        val eYear = endYear ?: year
        val datesParam = if (sYear != null || eYear != null) {
            val fromYr = sYear ?: 1970
            val toYr = eYear ?: sYear ?: (Calendar.getInstance().get(Calendar.YEAR) + 1)
            "&dates=$fromYr-01-01,$toYr-12-31"
        } else ""

        val rawgSlugs = includedTags.mapNotNull { tag ->
            GameTagRegistry.findGenre(tag)?.rawgSlug ?: tag.trim().lowercase(Locale.ROOT).replace(" ", "-")
        }.distinct()

        val genresParam = if (rawgSlugs.isNotEmpty()) {
            "&genres=${rawgSlugs.joinToString(",")}"
        } else ""

        val rawgPlatformIds = includedPlatforms.flatMap { plat ->
            plat.toIntOrNull()?.let { listOf(it) } ?: GamePlatformRegistry.findOption(plat)?.rawgPlatformIds ?: emptyList()
        }.distinct()

        val platformsParam = if (rawgPlatformIds.isNotEmpty()) {
            "&platforms=${rawgPlatformIds.joinToString(",")}"
        } else ""

        val adultParam = when (adultMode) {
            AdultMode.ONLY -> "&tags=nsfw"
            AdultMode.OFF -> ""
            AdultMode.INCLUDE -> ""
        }

        val pageParam = if (page > 1) "&page=$page" else ""
        return "$BASE_URL/games?key=$apiKey&search=$encodedQuery$datesParam$genresParam$platformsParam$adultParam&page_size=$limit$pageParam"
    }

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
        val apiKey = GameApiConfig.getRawgApiKey(context)
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("RAWG API key is not configured."))
        }

        val url = buildSearchUrl(
            apiKey = apiKey,
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
        val request = Request.Builder().url(url).build()

        return try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return Result.failure(Exception("RAWG API error (HTTP ${response.code}): $body"))
            }
            var list = parseSearchResultsJson(body)
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
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching RAWG games", e)
            Result.failure(e)
        }
    }

    /**
     * Builds the RAWG discover games request URL with dates, genre, and adult parameters.
     */
    fun buildDiscoverUrl(
        apiKey: String,
        year: Int? = null,
        startYear: Int? = null,
        endYear: Int? = null,
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
        val datesParam = if (sYear != null || eYear != null) {
            val fromYr = sYear ?: 1970
            val toYr = eYear ?: sYear ?: (Calendar.getInstance().get(Calendar.YEAR) + 1)
            "&dates=$fromYr-01-01,$toYr-12-31"
        } else ""

        val rawgSlugs = includedTags.mapNotNull { tag ->
            GameTagRegistry.findGenre(tag)?.rawgSlug ?: tag.trim().lowercase(Locale.ROOT).replace(" ", "-")
        }.distinct()

        val genresParam = if (rawgSlugs.isNotEmpty()) {
            "&genres=${rawgSlugs.joinToString(",")}"
        } else ""

        val rawgPlatformIds = includedPlatforms.flatMap { plat ->
            plat.toIntOrNull()?.let { listOf(it) } ?: GamePlatformRegistry.findOption(plat)?.rawgPlatformIds ?: emptyList()
        }.distinct()

        val platformsParam = if (rawgPlatformIds.isNotEmpty()) {
            "&platforms=${rawgPlatformIds.joinToString(",")}"
        } else ""

        val adultParam = when (adultMode) {
            AdultMode.ONLY -> "&tags=nsfw"
            AdultMode.OFF -> ""
            AdultMode.INCLUDE -> ""
        }

        val pageParam = if (page > 1) "&page=$page" else ""
        return "$BASE_URL/games?key=$apiKey&ordering=-added$datesParam$genresParam$platformsParam$adultParam&page_size=$limit$pageParam"
    }

    suspend fun discoverGames(
        context: Context,
        year: Int? = null,
        startYear: Int? = null,
        endYear: Int? = null,
        limit: Int = 20,
        page: Int = 1,
        includedTags: Set<String> = emptySet(),
        excludedTags: Set<String> = emptySet(),
        adultMode: AdultMode = AdultMode.OFF,
        includedPlatforms: Set<String> = emptySet(),
        excludedPlatforms: Set<String> = emptySet()
    ): Result<List<GameSearchResult>> {
        val apiKey = GameApiConfig.getRawgApiKey(context)
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("RAWG API key is not configured."))
        }
        val url = buildDiscoverUrl(
            apiKey = apiKey,
            year = year,
            startYear = startYear,
            endYear = endYear,
            limit = limit,
            page = page,
            includedTags = includedTags,
            excludedTags = excludedTags,
            adultMode = adultMode,
            includedPlatforms = includedPlatforms,
            excludedPlatforms = excludedPlatforms
        )
        val request = Request.Builder().url(url).build()

        return try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return Result.failure(Exception("RAWG API error (HTTP ${response.code}): $body"))
            }
            var list = parseSearchResultsJson(body)
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
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering RAWG games", e)
            Result.failure(e)
        }
    }

    suspend fun getGameDetails(context: Context, rawgId: String): Result<GameDetails> {
        val apiKey = GameApiConfig.getRawgApiKey(context)
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("RAWG API key is not configured."))
        }

        val detailUrl = "$BASE_URL/games/$rawgId?key=$apiKey"
        val request = Request.Builder().url(detailUrl).build()

        return try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return Result.failure(Exception("RAWG detail error (HTTP ${response.code}): $body"))
            }

            val seriesBody = try {
                val seriesReq = Request.Builder().url("$BASE_URL/games/$rawgId/game-series?key=$apiKey").build()
                val seriesResp = httpClient.newCall(seriesReq).execute()
                if (seriesResp.isSuccessful) seriesResp.body?.string() else null
            } catch (_: Exception) { null }

            val additionsBody = try {
                val additionsReq = Request.Builder().url("$BASE_URL/games/$rawgId/additions?key=$apiKey").build()
                val additionsResp = httpClient.newCall(additionsReq).execute()
                if (additionsResp.isSuccessful) additionsResp.body?.string() else null
            } catch (_: Exception) { null }

            Result.success(parseGameDetailsJson(body, rawgId, seriesBody, additionsBody))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching RAWG game details", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // JSON Parsers (Unit-Testable)
    // =========================================================================

    fun parseSearchResultsJson(jsonStr: String): List<GameSearchResult> {
        val json = try {
            gson.fromJson(jsonStr, JsonObject::class.java) ?: return emptyList()
        } catch (_: Exception) {
            return emptyList()
        }
        val results = json.getAsJsonArray("results") ?: return emptyList()
        val list = mutableListOf<GameSearchResult>()

        for (elem in results) {
            if (!elem.isJsonObject) continue
            val obj = elem.asJsonObject
            val id = obj.get("id")?.asLong?.toString() ?: continue
            val name = obj.get("name")?.asString ?: continue
            val releasedStr = obj.get("released")?.asString
            val releaseEpoch = releasedStr?.let { parseDateToMillis(it) }
            val releaseYear = releasedStr?.take(4)?.takeIf { it.length == 4 && it.all { c -> c.isDigit() } }

            val coverUrl = obj.get("background_image")?.asString
            val score = obj.get("rating")?.asFloat // RAWG rating is 0..5, double it or keep as 0..10
                ?.let { it * 2f }

            val platforms = parsePlatforms(obj.getAsJsonArray("platforms"))
            val genres = parseStringArray(obj.getAsJsonArray("genres"), "name")
            val developers = parseStringArray(obj.getAsJsonArray("developers"), "name")
            val publishers = parseStringArray(obj.getAsJsonArray("publishers"), "name")
            val playtimeHours = obj.get("playtime")?.takeIf { !it.isJsonNull }?.asInt
            val completionMinutes = if (playtimeHours != null && playtimeHours > 0) playtimeHours * 60 else null

            list.add(
                GameSearchResult(
                    externalId = id,
                    title = name,
                    coverUrl = coverUrl,
                    releaseDateMillis = releaseEpoch,
                    releaseYear = releaseYear,
                    platforms = platforms,
                    genres = genres,
                    developers = developers,
                    publishers = publishers,
                    score = score,
                    externalSource = "RAWG",
                    completionTimeMinutes = completionMinutes
                )
            )
        }
        return list
    }

    fun parseGameDetailsJson(
        detailJsonStr: String,
        targetId: String,
        seriesJsonStr: String? = null,
        additionsJsonStr: String? = null
    ): GameDetails {
        val obj = gson.fromJson(detailJsonStr, JsonObject::class.java)
        val id = obj.get("id")?.asLong?.toString() ?: targetId
        val name = obj.get("name")?.asString.orEmpty()
        val summary = obj.get("description_raw")?.asString ?: obj.get("description")?.asString

        val coverUrl = obj.get("background_image")?.asString
        val artworkUrl = obj.get("background_image_additional")?.asString

        val releasedStr = obj.get("released")?.asString
        val releaseEpoch = releasedStr?.let { parseDateToMillis(it) }
        val releaseYear = releasedStr?.take(4)?.takeIf { it.length == 4 && it.all { c -> c.isDigit() } }

        val platforms = parsePlatforms(obj.getAsJsonArray("platforms"))
        val genres = parseStringArray(obj.getAsJsonArray("genres"), "name")
        val themes = parseStringArray(obj.getAsJsonArray("tags"), "name")
        val developers = parseStringArray(obj.getAsJsonArray("developers"), "name")
        val publishers = parseStringArray(obj.getAsJsonArray("publishers"), "name")

        val score = (obj.get("rating")?.asFloat)?.let { it * 2f }
        val playtimeHours = obj.get("playtime")?.takeIf { !it.isJsonNull }?.asInt
        val completionMinutes = if (playtimeHours != null && playtimeHours > 0) playtimeHours * 60 else null

        // Related items from series and additions
        val related = mutableListOf<RelatedMediaItem>()
        if (!seriesJsonStr.isNullOrBlank()) {
            val seriesObj = try { gson.fromJson(seriesJsonStr, JsonObject::class.java) } catch (_: Exception) { null }
            val seriesResults = seriesObj?.getAsJsonArray("results")
            if (seriesResults != null) {
                for (s in seriesResults) {
                    if (s.isJsonObject) {
                        val sObj = s.asJsonObject
                        val sId = sObj.get("id")?.asLong?.toString() ?: continue
                        if (sId == targetId) continue
                        val sName = sObj.get("name")?.asString ?: continue
                        val sCover = sObj.get("background_image")?.asString
                        val sYear = sObj.get("released")?.asString?.take(4)
                        val sRating = sObj.get("rating")?.asFloat?.let { it * 2f }
                        val sPlatforms = parsePlatforms(sObj.getAsJsonArray("platforms"))

                        related.add(
                            RelatedMediaItem(
                                externalId = sId,
                                title = sName,
                                relationship = "Part of Series",
                                mediaType = MediaType.GAME,
                                posterUrl = sCover,
                                releaseYear = sYear,
                                score = sRating,
                                externalSource = "RAWG",
                                platforms = sPlatforms
                            )
                        )
                    }
                }
            }
        }

        if (!additionsJsonStr.isNullOrBlank()) {
            val addObj = try { gson.fromJson(additionsJsonStr, JsonObject::class.java) } catch (_: Exception) { null }
            val addResults = addObj?.getAsJsonArray("results")
            if (addResults != null) {
                for (a in addResults) {
                    if (a.isJsonObject) {
                        val aObj = a.asJsonObject
                        val aId = aObj.get("id")?.asLong?.toString() ?: continue
                        if (aId == targetId) continue
                        val aName = aObj.get("name")?.asString ?: continue
                        val aCover = aObj.get("background_image")?.asString
                        val aYear = aObj.get("released")?.asString?.take(4)
                        val aPlatforms = parsePlatforms(aObj.getAsJsonArray("platforms"))

                        related.add(
                            RelatedMediaItem(
                                externalId = aId,
                                title = aName,
                                relationship = "DLC",
                                mediaType = MediaType.GAME,
                                posterUrl = aCover,
                                releaseYear = aYear,
                                externalSource = "RAWG",
                                platforms = aPlatforms
                            )
                        )
                    }
                }
            }
        }

        return GameDetails(
            externalId = id,
            externalSource = "RAWG",
            title = name,
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
            rating = score,
            ratingSource = "RAWG",
            relatedItems = related,
            completionTimeMinutes = completionMinutes,
            normalTimeMinutes = completionMinutes
        )
    }

    private fun parsePlatforms(array: JsonArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (elem in array) {
            if (elem.isJsonObject) {
                val pObj = elem.asJsonObject.getAsJsonObject("platform")
                val name = pObj?.get("name")?.asString?.trim()
                if (!name.isNullOrBlank() && !list.contains(name)) {
                    list.add(name)
                }
            }
        }
        return list
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

    private fun parseDateToMillis(dateStr: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(dateStr)?.time
        } catch (_: Exception) {
            null
        }
    }
}
