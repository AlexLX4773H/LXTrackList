package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object KitsuApiClient {

    private const val TAG = "KitsuApiClient"
    private val gson = Gson()
    const val BASE_URL = "https://kitsu.app/api/edge"
    private const val USER_AGENT = "LXTrackList/1.0 (Android; https://github.com/RyanYuuki/AnymeX)"

    private var okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun initWithDoH(context: Context) {
        try {
            val dohResolver = TmdbDnsHelper.createDoHResolver(context)
            okHttpClient = okHttpClient.newBuilder()
                .dns(dohResolver)
                .build()
            Log.i(TAG, "Initialized Kitsu OkHttpClient with DNS-over-HTTPS resolver")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize DNS-over-HTTPS resolver for Kitsu client", e)
        }
    }

    fun searchMangaMultiple(
        query: String,
        limit: Int = 20
    ): Result<List<JikanMediaDetails>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return Result.success(emptyList())

        val encodedTitle = URLEncoder.encode(cleanQuery, "UTF-8")
        val limitClamped = limit.coerceIn(1, 20)
        val endpoint = "$BASE_URL/manga?filter[text]=$encodedTitle&page[limit]=$limitClamped"

        return try {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/vnd.api+json")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.w(TAG, "Kitsu search failed with HTTP $code: ${bodyString.take(150)}")
                return Result.failure(Exception("Kitsu HTTP $code: ${bodyString.take(150)}"))
            }

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
            val dataArray = rootObj?.getAsJsonArray("data")
            if (dataArray == null || dataArray.size() == 0) {
                return Result.success(emptyList())
            }

            val results = mutableListOf<JikanMediaDetails>()
            for (i in 0 until dataArray.size()) {
                val item = dataArray.get(i)
                if (item.isJsonObject) {
                    try {
                        val parsed = parseKitsuItem(item.asJsonObject, isAnime = false)
                        if (parsed != null) {
                            results.add(parsed)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing Kitsu item at index $i: ${e.message}")
                    }
                }
            }

            Log.d(TAG, "Kitsu search for '$cleanQuery' returned ${results.size} results")
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Kitsu search for '$cleanQuery'", e)
            Result.failure(e)
        }
    }

    fun searchAnimeMultiple(
        query: String,
        limit: Int = 20
    ): Result<List<JikanMediaDetails>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return Result.success(emptyList())

        val encodedTitle = URLEncoder.encode(cleanQuery, "UTF-8")
        val limitClamped = limit.coerceIn(1, 20)
        val endpoint = "$BASE_URL/anime?filter[text]=$encodedTitle&page[limit]=$limitClamped"

        return try {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/vnd.api+json")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.w(TAG, "Kitsu anime search failed with HTTP $code: ${bodyString.take(150)}")
                return Result.failure(Exception("Kitsu HTTP $code: ${bodyString.take(150)}"))
            }

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
            val dataArray = rootObj?.getAsJsonArray("data")
            if (dataArray == null || dataArray.size() == 0) {
                return Result.success(emptyList())
            }

            val results = mutableListOf<JikanMediaDetails>()
            for (i in 0 until dataArray.size()) {
                val item = dataArray.get(i)
                if (item.isJsonObject) {
                    try {
                        val parsed = parseKitsuItem(item.asJsonObject, isAnime = true)
                        if (parsed != null) {
                            results.add(parsed)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing Kitsu anime item at index $i: ${e.message}")
                    }
                }
            }

            Log.d(TAG, "Kitsu anime search for '$cleanQuery' returned ${results.size} results")
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Kitsu anime search for '$cleanQuery'", e)
            Result.failure(e)
        }
    }

    fun searchAnimeDetailsByTitle(title: String): Result<JikanMediaDetails> {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be blank"))

        val searchResult = searchAnimeMultiple(cleanTitle, limit = 5)
        if (searchResult.isFailure) return Result.failure(searchResult.exceptionOrNull() ?: Exception("Kitsu anime search failed"))

        val list = searchResult.getOrDefault(emptyList())
        if (list.isEmpty()) {
            return Result.failure(NotFoundException("No Kitsu anime results found for '$cleanTitle'"))
        }

        val exactMatch = list.firstOrNull {
            it.title.equals(cleanTitle, ignoreCase = true) ||
                    it.alternateTitle?.split("\n")?.any { alt -> alt.equals(cleanTitle, ignoreCase = true) } == true
        }

        return Result.success(exactMatch ?: list.first())
    }

    fun searchMangaDetailsByTitle(title: String): Result<JikanMediaDetails> {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be blank"))

        val searchResult = searchMangaMultiple(cleanTitle, limit = 5)
        if (searchResult.isFailure) return Result.failure(searchResult.exceptionOrNull() ?: Exception("Kitsu search failed"))

        val list = searchResult.getOrDefault(emptyList())
        if (list.isEmpty()) {
            return Result.failure(NotFoundException("No Kitsu results found for '$cleanTitle'"))
        }

        val exactMatch = list.firstOrNull {
            it.title.equals(cleanTitle, ignoreCase = true) ||
                    it.alternateTitle?.split("\n")?.any { alt -> alt.equals(cleanTitle, ignoreCase = true) } == true
        }

        return Result.success(exactMatch ?: list.first())
    }

    fun parseKitsuItem(itemObj: JsonObject, isAnime: Boolean = false): JikanMediaDetails? {
        val id = itemObj.get("id")?.takeIf { !it.isJsonNull }?.asString ?: return null
        val attributes = itemObj.getAsJsonObject("attributes") ?: return null
        val slug = attributes.get("slug")?.takeIf { !it.isJsonNull }?.asString?.trim()

        val canonicalTitle = attributes.get("canonicalTitle")?.takeIf { !it.isJsonNull }?.asString?.trim()
        val titlesObj = attributes.getAsJsonObject("titles")
        val enTitle = titlesObj?.get("en")?.takeIf { !it.isJsonNull }?.asString?.trim()
        val enJpTitle = titlesObj?.get("en_jp")?.takeIf { !it.isJsonNull }?.asString?.trim()
        val jaJpTitle = titlesObj?.get("ja_jp")?.takeIf { !it.isJsonNull }?.asString?.trim()

        val title = canonicalTitle ?: enTitle ?: enJpTitle ?: jaJpTitle ?: if (isAnime) "Unknown Anime" else "Unknown Manga"

        // Alternate titles
        val altList = mutableListOf<String>()
        listOfNotNull(enTitle, enJpTitle, jaJpTitle).forEach { t ->
            if (t.isNotBlank() && !t.equals(title, ignoreCase = true) && !altList.contains(t)) {
                altList.add(t)
            }
        }
        val alternateTitle = altList.distinct().joinToString("\n").takeIf { it.isNotBlank() }

        // Synopsis
        val rawDesc = attributes.get("synopsis")?.takeIf { !it.isJsonNull }?.asString
        val synopsis = JikanApiClient.cleanSynopsis(rawDesc)

        // Poster image
        val posterObj = attributes.getAsJsonObject("posterImage")
        val posterUrl = posterObj?.get("large")?.takeIf { !it.isJsonNull }?.asString
            ?: posterObj?.get("original")?.takeIf { !it.isJsonNull }?.asString
            ?: posterObj?.get("medium")?.takeIf { !it.isJsonNull }?.asString

        // Units
        val totalEpisodes = if (isAnime) attributes.get("episodeCount")?.takeIf { !it.isJsonNull }?.asInt else null
        val durationMinutes = if (isAnime) attributes.get("episodeLength")?.takeIf { !it.isJsonNull }?.asInt else null
        val totalChapters = if (!isAnime) attributes.get("chapterCount")?.takeIf { !it.isJsonNull }?.asInt else null
        val totalVolumes = if (!isAnime) attributes.get("volumeCount")?.takeIf { !it.isJsonNull }?.asInt else null

        // Dates
        val startDateStr = attributes.get("startDate")?.takeIf { !it.isJsonNull }?.asString
        val endDateStr = attributes.get("endDate")?.takeIf { !it.isJsonNull }?.asString
        val airedStartDate = JikanApiClient.parseIsoOrSimpleDate(startDateStr)
        val airedEndDate = JikanApiClient.parseIsoOrSimpleDate(endDateStr)

        // Status
        val statusStr = attributes.get("status")?.takeIf { !it.isJsonNull }?.asString
        val isAiring = statusStr?.equals("current", ignoreCase = true)

        // Score (Kitsu averageRating is 0-100, e.g. "84.21" -> 8.4)
        val ratingStr = attributes.get("averageRating")?.takeIf { !it.isJsonNull }?.asString
        val score = ratingStr?.toFloatOrNull()?.let { it / 10f }

        val subtype = attributes.get("subtype")?.takeIf { !it.isJsonNull }?.asString?.replaceFirstChar { it.uppercase() } ?: if (isAnime) "Anime" else "Manga"
        val resolvedTarget = slug?.takeIf { it.isNotBlank() } ?: id

        return JikanMediaDetails(
            malId = null,
            title = title,
            posterUrl = posterUrl,
            durationText = durationMinutes?.let { "${it}m" },
            durationMinutes = durationMinutes,
            totalEpisodes = totalEpisodes,
            totalChapters = totalChapters,
            totalVolumes = totalVolumes,
            alternateTitle = alternateTitle,
            airedStartDate = airedStartDate,
            airedEndDate = airedEndDate,
            tags = subtype,
            synopsis = synopsis,
            isAiring = isAiring,
            externalScore = score,
            externalScoreSource = if (score != null) "Kitsu" else null,
            type = subtype,
            sourceProvider = "Kitsu",
            url = if (slug?.isNotBlank() == true) {
                if (isAnime) "https://kitsu.app/anime/$slug" else "https://kitsu.app/manga/$slug"
            } else {
                val type = if (isAnime) "anime" else "manga"
                val encTitle = URLEncoder.encode(title, "UTF-8")
                "kitsu-resolve://$type?title=$encTitle&kitsuId=$id"
            }
        )
    }

    /**
     * Look up exact Kitsu slug directly from the media ID (GET /anime/{id} or /manga/{id}).
     */
    fun lookupSlugById(id: Long, isAnime: Boolean): String? {
        if (id <= 0) return null
        val type = if (isAnime) "anime" else "manga"
        val endpoint = "$BASE_URL/$type/$id"

        return try {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/vnd.api+json")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val bodyString = response.body?.string() ?: return null
            val root = gson.fromJson(bodyString, JsonObject::class.java) ?: return null
            val data = root.getAsJsonObject("data")
            val slug = data?.getAsJsonObject("attributes")?.get("slug")?.takeIf { !it.isJsonNull }?.asString?.trim()
            if (!slug.isNullOrBlank()) {
                Log.d(TAG, "Resolved Kitsu slug by ID ($id -> $slug)")
                return slug
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error looking up Kitsu slug by ID $id: ${e.message}")
            null
        }
    }

    /**
     * Look up exact Kitsu slug from an external MyAnimeList ID using Kitsu's mapping endpoint.
     */
    fun lookupSlugByMalId(malId: Long, isAnime: Boolean): String? {
        if (malId <= 0) return null
        val extSite = if (isAnime) "myanimelist/anime" else "myanimelist/manga"
        val endpoint = "$BASE_URL/mappings?filter[externalSite]=$extSite&filter[externalId]=$malId&include=item"

        return try {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/vnd.api+json")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val bodyString = response.body?.string() ?: return null
            val root = gson.fromJson(bodyString, JsonObject::class.java) ?: return null

            // 1. Check included array for the item object
            val included = root.getAsJsonArray("included")
            if (included != null && included.size() > 0) {
                for (i in 0 until included.size()) {
                    val elem = included.get(i)?.asJsonObject ?: continue
                    val slug = elem.getAsJsonObject("attributes")?.get("slug")?.takeIf { !it.isJsonNull }?.asString?.trim()
                    if (!slug.isNullOrBlank()) {
                        Log.d(TAG, "Resolved Kitsu slug via MAL mapping ($malId -> $slug)")
                        return slug
                    }
                }
            }

            // 2. Check relationship item id if included didn't have slug directly
            val dataArr = root.getAsJsonArray("data")
            if (dataArr != null && dataArr.size() > 0) {
                for (i in 0 until dataArr.size()) {
                    val mappingObj = dataArr.get(i)?.asJsonObject ?: continue
                    val itemRel = mappingObj.getAsJsonObject("relationships")?.getAsJsonObject("item")?.getAsJsonObject("data")
                    val itemIdStr = itemRel?.get("id")?.takeIf { !it.isJsonNull }?.asString
                    val itemId = itemIdStr?.toLongOrNull()
                    if (itemId != null && itemId > 0) {
                        val slugFromId = lookupSlugById(itemId, isAnime)
                        if (!slugFromId.isNullOrBlank()) {
                            return slugFromId
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error looking up Kitsu slug by MAL ID $malId: ${e.message}")
            null
        }
    }

    /**
     * Look up exact Kitsu slug by searching title text.
     */
    fun lookupSlugByTitle(title: String, isAnime: Boolean): String? {
        val clean = title.trim()
        if (clean.isBlank()) return null
        val encodedTitle = URLEncoder.encode(clean, "UTF-8")
        val endpoint = if (isAnime) {
            "$BASE_URL/anime?filter[text]=$encodedTitle&page[limit]=1"
        } else {
            "$BASE_URL/manga?filter[text]=$encodedTitle&page[limit]=1"
        }

        return try {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/vnd.api+json")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val bodyString = response.body?.string() ?: return null
            val root = gson.fromJson(bodyString, JsonObject::class.java) ?: return null
            val data = root.getAsJsonArray("data")
            if (data != null && data.size() > 0) {
                val item = data.get(0)?.asJsonObject
                val slug = item?.getAsJsonObject("attributes")?.get("slug")?.takeIf { !it.isJsonNull }?.asString?.trim()
                if (!slug.isNullOrBlank()) {
                    Log.d(TAG, "Resolved Kitsu slug via search for '$clean': $slug")
                    return slug
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error looking up Kitsu slug by title '$title': ${e.message}")
            null
        }
    }
}

