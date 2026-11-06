package com.lx.tracklist.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.RelatedMediaItem
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class JikanAnimeDetails(
    val malId: Long,
    val title: String?,
    val posterUrl: String?,
    val durationText: String?,
    val durationMinutes: Int?
)

data class JikanMediaDetails(
    val malId: Long?,
    val title: String?,
    val posterUrl: String?,
    val durationText: String? = null,
    val durationMinutes: Int? = null,
    val totalEpisodes: Int? = null,
    val totalChapters: Int? = null,
    val totalVolumes: Int? = null,
    val alternateTitle: String? = null,
    val airedStartDate: Long? = null,
    val airedEndDate: Long? = null,
    val tags: String? = null,
    val synopsis: String? = null,
    val isAiring: Boolean? = null,
    val externalScore: Float? = null,
    val externalScoreSource: String? = null,
    val type: String? = null,
    val sourceProvider: String = "Jikan"
)

object JikanApiClient {

    private const val TAG = "JikanApiClient"
    private val gson = Gson()
    const val DEFAULT_BASE_URL = "https://api.jikan.moe/v4"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 16; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36 LXTrackList/1.0"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun fetchAnimeDetails(
        malId: Long,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<JikanAnimeDetails> {
        return fetchMediaDetails(malId, isManga = false, baseUrl = baseUrl).map { media ->
            JikanAnimeDetails(
                malId = media.malId ?: malId,
                title = media.title,
                posterUrl = media.posterUrl,
                durationText = media.durationText,
                durationMinutes = media.durationMinutes
            )
        }
    }

    fun fetchMediaDetails(
        malId: Long,
        isManga: Boolean,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<JikanMediaDetails> {
        val primaryType = if (isManga) "manga" else "anime"

        // 1. Try Jikan API
        val jikanResult = queryJikan(baseUrl, primaryType, malId)
        if (jikanResult.isSuccess) {
            val details = jikanResult.getOrNull()
            if (details != null && !details.posterUrl.isNullOrBlank()) {
                Log.d(TAG, "Successfully fetched from Jikan for MAL ID $malId (${details.title})")
                return jikanResult
            }
        }

        val jikanErr = jikanResult.exceptionOrNull()
        Log.w(TAG, "Jikan lookup failed for MAL ID $malId: ${jikanErr?.message}. Falling back to AniList...")

        // 2. Fallback to AniList GraphQL API using idMal
        val anilistResult = queryAniList(malId, isManga)
        if (anilistResult.isSuccess) {
            val details = anilistResult.getOrNull()
            if (details != null && !details.posterUrl.isNullOrBlank()) {
                Log.d(TAG, "Successfully fetched from AniList for MAL ID $malId (${details.title})")
                return anilistResult
            }
        }

        // 3. Cross-fallback (try alternate media type on AniList in case media type is misclassified)
        val anilistCrossResult = queryAniList(malId, !isManga)
        if (anilistCrossResult.isSuccess) {
            val details = anilistCrossResult.getOrNull()
            if (details != null && !details.posterUrl.isNullOrBlank()) {
                Log.d(TAG, "Successfully fetched from AniList cross-type for MAL ID $malId (${details.title})")
                return anilistCrossResult
            }
        }

        // 4. Cross-fallback on Jikan
        val fallbackType = if (isManga) "anime" else "manga"
        val jikanCrossResult = queryJikan(baseUrl, fallbackType, malId)
        if (jikanCrossResult.isSuccess) {
            val details = jikanCrossResult.getOrNull()
            if (details != null && !details.posterUrl.isNullOrBlank()) {
                Log.d(TAG, "Successfully fetched from Jikan cross-type for MAL ID $malId (${details.title})")
                return jikanCrossResult
            }
        }

        val errorSummary = "Jikan: ${jikanErr?.message ?: "unknown"}, AniList: ${anilistResult.exceptionOrNull()?.message ?: "unknown"}"
        Log.e(TAG, "All metadata lookups failed for MAL ID $malId. $errorSummary")
        return Result.failure(Exception(errorSummary))
    }

    private fun queryJikan(
        baseUrl: String,
        type: String, // "anime" or "manga"
        malId: Long
    ): Result<JikanMediaDetails> {
        val endpoint = "${baseUrl.trimEnd('/')}/$type/$malId"
        return try {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            Log.d(TAG, "Jikan HTTP $code for $endpoint")

            if (code == 429) {
                return Result.failure(RateLimitException("Jikan API rate limit (429) for $type #$malId"))
            }
            if (code == 404) {
                return Result.failure(NotFoundException("Jikan entry #$malId not found in /$type/"))
            }
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP $code from Jikan: ${bodyString.take(200)}"))
            }

            val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
            val dataObj = jsonObject?.getAsJsonObject("data")
                ?: return Result.failure(Exception("Missing 'data' in Jikan response"))

            Result.success(parseJikanData(dataObj, fallbackMalId = malId))
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying Jikan $endpoint", e)
            Result.failure(e)
        }
    }

    fun queryAniList(malId: Long, isManga: Boolean): Result<JikanMediaDetails> {
        val type = if (isManga) "MANGA" else "ANIME"
        val query = """
            query (${'$'}idMal: Int, ${'$'}type: MediaType) {
              Media(idMal: ${'$'}idMal, type: ${'$'}type) {
                id
                idMal
                title { romaji english native }
                synonyms
                coverImage { extraLarge large }
                duration
                episodes
                chapters
                volumes
                format
                status
                averageScore
                meanScore
                description(asHtml: false)
                startDate { year month day }
                endDate { year month day }
                genres
                tags { name }
              }
            }
        """.trimIndent()

        val jsonBody = JsonObject().apply {
            addProperty("query", query)
            val vars = JsonObject().apply {
                addProperty("idMal", malId)
                addProperty("type", type)
            }
            add("variables", vars)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://graphql.anilist.co")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .post(requestBody)
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            Log.d(TAG, "AniList HTTP $code for MAL ID $malId ($type)")

            if (!response.isSuccessful) {
                return Result.failure(Exception("AniList HTTP $code: ${bodyString.take(200)}"))
            }

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
            val dataObj = rootObj?.getAsJsonObject("data")
            val mediaObj = dataObj?.getAsJsonObject("Media")
                ?: return Result.failure(NotFoundException("MAL ID #$malId not found on AniList"))

            Result.success(parseAniListMedia(mediaObj, fallbackMalId = malId))
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying AniList for MAL ID $malId", e)
            Result.failure(e)
        }
    }

    fun cleanSearchTitle(title: String): String {
        val withoutBrackets = title
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("\\(.*?\\)"), "")
            .trim()
        return withoutBrackets.ifBlank { title.trim() }
    }

    fun searchMediaDetailsByTitle(
        title: String,
        isManga: Boolean,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<JikanMediaDetails> {
        val rawTitle = title.trim()
        if (rawTitle.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be blank"))

        val cleanedTitle = cleanSearchTitle(rawTitle)
        val primaryType = if (isManga) "manga" else "anime"

        // 1. Try Jikan search with cleaned title
        var jikanResult = queryJikanByTitle(baseUrl, primaryType, cleanedTitle)
        if (jikanResult.isSuccess && !jikanResult.getOrNull()?.posterUrl.isNullOrBlank()) {
            val details = jikanResult.getOrNull()
            Log.d(TAG, "Successfully found by title on Jikan for '$cleanedTitle' (MAL ID ${details?.malId})")
            return jikanResult
        }

        // 2. If cleanedTitle differs from rawTitle, try raw title on Jikan
        if (cleanedTitle != rawTitle) {
            val rawJikanResult = queryJikanByTitle(baseUrl, primaryType, rawTitle)
            if (rawJikanResult.isSuccess && !rawJikanResult.getOrNull()?.posterUrl.isNullOrBlank()) {
                val details = rawJikanResult.getOrNull()
                Log.d(TAG, "Successfully found by raw title on Jikan for '$rawTitle' (MAL ID ${details?.malId})")
                return rawJikanResult
            }
            if (rawJikanResult.isFailure && rawJikanResult.exceptionOrNull() is RateLimitException) {
                jikanResult = rawJikanResult
            }
        }

        val jikanErr = jikanResult.exceptionOrNull()
        Log.w(TAG, "Jikan title search failed for '$rawTitle': ${jikanErr?.message}. Falling back to AniList...")

        // 3. Fallback to AniList GraphQL search by title
        var anilistResult = queryAniListByTitle(cleanedTitle, isManga)
        if (anilistResult.isSuccess && !anilistResult.getOrNull()?.posterUrl.isNullOrBlank()) {
            val details = anilistResult.getOrNull()
            Log.d(TAG, "Successfully found by title on AniList for '$cleanedTitle' (MAL ID ${details?.malId})")
            return anilistResult
        }

        if (cleanedTitle != rawTitle) {
            val rawAnilistResult = queryAniListByTitle(rawTitle, isManga)
            if (rawAnilistResult.isSuccess && !rawAnilistResult.getOrNull()?.posterUrl.isNullOrBlank()) {
                val details = rawAnilistResult.getOrNull()
                Log.d(TAG, "Successfully found by raw title on AniList for '$rawTitle' (MAL ID ${details?.malId})")
                return rawAnilistResult
            }
        }

        // 4. Cross-fallback on Jikan (e.g. manga if anime or anime if manga)
        val fallbackType = if (isManga) "anime" else "manga"
        val jikanCrossResult = queryJikanByTitle(baseUrl, fallbackType, cleanedTitle)
        if (jikanCrossResult.isSuccess && !jikanCrossResult.getOrNull()?.posterUrl.isNullOrBlank()) {
            val details = jikanCrossResult.getOrNull()
            Log.d(TAG, "Successfully found by cross-type title on Jikan for '$cleanedTitle' (MAL ID ${details?.malId})")
            return jikanCrossResult
        }

        // 5. Cross-fallback on AniList
        val anilistCrossResult = queryAniListByTitle(cleanedTitle, !isManga)
        if (anilistCrossResult.isSuccess && !anilistCrossResult.getOrNull()?.posterUrl.isNullOrBlank()) {
            val details = anilistCrossResult.getOrNull()
            Log.d(TAG, "Successfully found by cross-type title on AniList for '$cleanedTitle' (MAL ID ${details?.malId})")
            return anilistCrossResult
        }

        val errorSummary = "Jikan search: ${jikanErr?.message ?: "not found"}, AniList search: ${anilistResult.exceptionOrNull()?.message ?: "not found"}"
        Log.e(TAG, "All title searches failed for '$rawTitle'. $errorSummary")
        return Result.failure(NotFoundException(errorSummary))
    }

    fun searchJikanMultiple(
        queryTitle: String,
        isManga: Boolean,
        subType: String? = null,
        limit: Int = 20,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<List<JikanMediaDetails>> {
        val cleanTitle = queryTitle.trim()
        if (cleanTitle.isBlank()) return Result.success(emptyList())

        val type = if (isManga) "manga" else "anime"
        val encodedQuery = URLEncoder.encode(cleanTitle, "UTF-8")
        val subTypeParam = if (!subType.isNullOrBlank()) "&type=$subType" else ""
        val endpoint = "${baseUrl.trimEnd('/')}/$type?q=$encodedQuery&limit=$limit$subTypeParam"

        try {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
                val dataArray = jsonObject?.getAsJsonArray("data")
                if (dataArray != null && dataArray.size() > 0) {
                    val list = mutableListOf<JikanMediaDetails>()
                    for (i in 0 until dataArray.size()) {
                        val elem = dataArray.get(i)
                        if (elem.isJsonObject) {
                            val dataObj = elem.asJsonObject
                            val malId = dataObj.get("mal_id")?.takeIf { !it.isJsonNull }?.asLong
                            if (malId != null) {
                                try {
                                    list.add(parseJikanData(dataObj, fallbackMalId = malId))
                                } catch (e: Exception) {
                                    Log.w(TAG, "Error parsing Jikan item at index $i", e)
                                }
                            }
                        }
                    }
                    if (list.isNotEmpty()) return Result.success(list)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Jikan multiple search failed, trying fallback: ${e.message}")
        }

        // Fallback: try AniList query if Jikan failed or was empty
        val anilistFallback = queryAniListByTitle(cleanTitle, isManga)
        if (anilistFallback.isSuccess) {
            anilistFallback.getOrNull()?.let { return Result.success(listOf(it)) }
        }

        return Result.success(emptyList())
    }

    private fun queryJikanByTitle(
        baseUrl: String,
        type: String, // "anime" or "manga"
        queryTitle: String
    ): Result<JikanMediaDetails> {
        val encodedQuery = URLEncoder.encode(queryTitle, "UTF-8")
        val endpoint = "${baseUrl.trimEnd('/')}/$type?q=$encodedQuery&limit=1"
        return try {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            Log.d(TAG, "Jikan title search HTTP $code for $endpoint")

            if (code == 429) {
                return Result.failure(RateLimitException("Jikan API rate limit (429) for search '$queryTitle'"))
            }
            if (code == 404) {
                return Result.failure(NotFoundException("No Jikan results for '$queryTitle' in /$type/"))
            }
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP $code from Jikan: ${bodyString.take(200)}"))
            }

            val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
            val dataArray = jsonObject?.getAsJsonArray("data")
            if (dataArray == null || dataArray.size() == 0) {
                return Result.failure(NotFoundException("No search results found for '$queryTitle' on Jikan"))
            }

            val dataObj = dataArray.get(0).asJsonObject
            val malId = dataObj.get("mal_id")?.takeIf { !it.isJsonNull }?.asLong
                ?: return Result.failure(NotFoundException("Missing 'mal_id' in Jikan search result"))

            Result.success(parseJikanData(dataObj, fallbackMalId = malId))
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying Jikan title search $endpoint", e)
            Result.failure(e)
        }
    }

    fun queryAniListByTitle(searchTitle: String, isManga: Boolean): Result<JikanMediaDetails> {
        val type = if (isManga) "MANGA" else "ANIME"
        val query = """
            query (${'$'}search: String, ${'$'}type: MediaType) {
              Media(search: ${'$'}search, type: ${'$'}type) {
                id
                idMal
                title { romaji english native }
                synonyms
                coverImage { extraLarge large }
                duration
                episodes
                chapters
                volumes
                format
                status
                averageScore
                meanScore
                description(asHtml: false)
                startDate { year month day }
                endDate { year month day }
                genres
                tags { name }
              }
            }
        """.trimIndent()

        val jsonBody = JsonObject().apply {
            addProperty("query", query)
            val vars = JsonObject().apply {
                addProperty("search", searchTitle)
                addProperty("type", type)
            }
            add("variables", vars)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://graphql.anilist.co")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .post(requestBody)
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            Log.d(TAG, "AniList title search HTTP $code for '$searchTitle' ($type)")

            if (!response.isSuccessful) {
                return Result.failure(Exception("AniList HTTP $code: ${bodyString.take(200)}"))
            }

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
            val dataObj = rootObj?.getAsJsonObject("data")
            val mediaObj = dataObj?.getAsJsonObject("Media")
                ?: return Result.failure(NotFoundException("Title '$searchTitle' not found on AniList"))

            val idMal = mediaObj.get("idMal")?.takeIf { !it.isJsonNull }?.asLong

            Result.success(parseAniListMedia(mediaObj, fallbackMalId = idMal))
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying AniList title search for '$searchTitle'", e)
            Result.failure(e)
        }
    }

    fun parseJikanData(dataObj: JsonObject, fallbackMalId: Long?): JikanMediaDetails {
        val malId = dataObj.get("mal_id")?.takeIf { !it.isJsonNull }?.asLong ?: fallbackMalId
        val title = dataObj.get("title")?.takeIf { !it.isJsonNull }?.asString
        val mediaTypeStr = dataObj.get("type")?.takeIf { !it.isJsonNull }?.asString

        val imagesObj = dataObj.getAsJsonObject("images")
        val webpObj = imagesObj?.getAsJsonObject("webp")
        val jpgObj = imagesObj?.getAsJsonObject("jpg")

        val posterUrl = webpObj?.get("large_image_url")?.takeIf { !it.isJsonNull }?.asString
            ?: jpgObj?.get("large_image_url")?.takeIf { !it.isJsonNull }?.asString
            ?: webpObj?.get("image_url")?.takeIf { !it.isJsonNull }?.asString
            ?: jpgObj?.get("image_url")?.takeIf { !it.isJsonNull }?.asString

        val durationText = dataObj.get("duration")?.takeIf { !it.isJsonNull }?.asString
        val durationMinutes = parseDurationMinutes(durationText)

        val totalEpisodes = dataObj.get("episodes")?.takeIf { !it.isJsonNull }?.asInt
        val totalChapters = dataObj.get("chapters")?.takeIf { !it.isJsonNull }?.asInt
        val totalVolumes = dataObj.get("volumes")?.takeIf { !it.isJsonNull }?.asInt

        // Airing status
        val airing = dataObj.get("airing")?.takeIf { !it.isJsonNull }?.asBoolean
        val publishing = dataObj.get("publishing")?.takeIf { !it.isJsonNull }?.asBoolean
        val statusStr = dataObj.get("status")?.takeIf { !it.isJsonNull }?.asString
        val isAiring = airing ?: publishing ?: (statusStr?.equals("Currently Airing", ignoreCase = true) == true || statusStr?.equals("Publishing", ignoreCase = true) == true)

        // Broadcast / Published dates
        val dateContainer = dataObj.getAsJsonObject("aired") ?: dataObj.getAsJsonObject("published")
        val fromStr = dateContainer?.get("from")?.takeIf { !it.isJsonNull }?.asString
        val toStr = dateContainer?.get("to")?.takeIf { !it.isJsonNull }?.asString
        val propObj = dateContainer?.getAsJsonObject("prop")
        val airedStartDate = parseDateStringOrProp(fromStr, propObj?.getAsJsonObject("from"))
        val airedEndDate = parseDateStringOrProp(toStr, propObj?.getAsJsonObject("to"))

        // Alternative titles
        val altList = mutableListOf<String>()
        val engTitle = dataObj.get("title_english")?.takeIf { !it.isJsonNull }?.asString?.trim()
        val japTitle = dataObj.get("title_japanese")?.takeIf { !it.isJsonNull }?.asString?.trim()
        if (!engTitle.isNullOrBlank() && !engTitle.equals(title, ignoreCase = true)) {
            altList.add(engTitle)
        }
        if (!japTitle.isNullOrBlank() && !japTitle.equals(title, ignoreCase = true)) {
            altList.add(japTitle)
        }
        dataObj.getAsJsonArray("title_synonyms")?.forEach { synElem ->
            val syn = synElem?.takeIf { !it.isJsonNull }?.asString?.trim()
            if (!syn.isNullOrBlank() && !syn.equals(title, ignoreCase = true) && !altList.contains(syn)) {
                altList.add(syn)
            }
        }
        dataObj.getAsJsonArray("titles")?.forEach { tElem ->
            if (tElem.isJsonObject) {
                val tObj = tElem.asJsonObject
                val tStr = tObj.get("title")?.takeIf { !it.isJsonNull }?.asString?.trim()
                if (!tStr.isNullOrBlank() && !tStr.equals(title, ignoreCase = true) && !altList.contains(tStr)) {
                    altList.add(tStr)
                }
            }
        }
        val alternateTitle = altList.distinct().joinToString("\n").takeIf { it.isNotBlank() }

        // Tags (genres, themes, demographics, explicit_genres)
        val tagList = mutableListOf<String>()
        listOf("genres", "themes", "demographics", "explicit_genres").forEach { arrayKey ->
            dataObj.getAsJsonArray(arrayKey)?.forEach { item ->
                if (item.isJsonObject) {
                    val name = item.asJsonObject.get("name")?.takeIf { !it.isJsonNull }?.asString?.trim()
                    if (!name.isNullOrBlank() && !tagList.contains(name)) {
                        tagList.add(name)
                    }
                }
            }
        }
        val tags = tagList.joinToString(", ").takeIf { it.isNotBlank() }

        // Synopsis / Description
        val rawSynopsis = dataObj.get("synopsis")?.takeIf { !it.isJsonNull }?.asString
        val synopsis = cleanSynopsis(rawSynopsis)

        // External Score (MAL score e.g. 7.18)
        val rawScore = dataObj.get("score")?.takeIf { !it.isJsonNull }?.asFloat
        val externalScore = rawScore?.takeIf { it > 0f }
        val externalScoreSource = if (externalScore != null) "MAL" else null

        return JikanMediaDetails(
            malId = malId,
            title = title,
            posterUrl = posterUrl,
            durationText = durationText,
            durationMinutes = durationMinutes,
            totalEpisodes = totalEpisodes,
            totalChapters = totalChapters,
            totalVolumes = totalVolumes,
            alternateTitle = alternateTitle,
            airedStartDate = airedStartDate,
            airedEndDate = airedEndDate,
            tags = tags,
            synopsis = synopsis,
            isAiring = isAiring,
            externalScore = externalScore,
            externalScoreSource = externalScoreSource,
            type = mediaTypeStr,
            sourceProvider = "Jikan"
        )
    }

    fun parseAniListMedia(mediaObj: JsonObject, fallbackMalId: Long?): JikanMediaDetails {
        val idMal = mediaObj.get("idMal")?.takeIf { !it.isJsonNull }?.asLong ?: fallbackMalId

        val titleObj = mediaObj.getAsJsonObject("title")
        val engTitle = titleObj?.get("english")?.takeIf { !it.isJsonNull }?.asString?.trim()
        val romajiTitle = titleObj?.get("romaji")?.takeIf { !it.isJsonNull }?.asString?.trim()
        val nativeTitle = titleObj?.get("native")?.takeIf { !it.isJsonNull }?.asString?.trim()
        val title = engTitle ?: romajiTitle ?: nativeTitle

        val coverObj = mediaObj.getAsJsonObject("coverImage")
        val posterUrl = coverObj?.get("extraLarge")?.takeIf { !it.isJsonNull }?.asString
            ?: coverObj?.get("large")?.takeIf { !it.isJsonNull }?.asString

        val durationMinutes = mediaObj.get("duration")?.takeIf { !it.isJsonNull }?.asInt
        val totalEpisodes = mediaObj.get("episodes")?.takeIf { !it.isJsonNull }?.asInt
        val totalChapters = mediaObj.get("chapters")?.takeIf { !it.isJsonNull }?.asInt
        val totalVolumes = mediaObj.get("volumes")?.takeIf { !it.isJsonNull }?.asInt
        val format = mediaObj.get("format")?.takeIf { !it.isJsonNull }?.asString

        val statusStr = mediaObj.get("status")?.takeIf { !it.isJsonNull }?.asString
        val isAiring = statusStr?.equals("RELEASING", ignoreCase = true)

        val airedStartDate = parseAniListDate(mediaObj.getAsJsonObject("startDate"))
        val airedEndDate = parseAniListDate(mediaObj.getAsJsonObject("endDate"))

        // Alternative titles
        val altList = mutableListOf<String>()
        listOfNotNull(engTitle, nativeTitle, romajiTitle).forEach { t ->
            if (t.isNotBlank() && !t.equals(title, ignoreCase = true) && !altList.contains(t)) {
                altList.add(t)
            }
        }
        mediaObj.getAsJsonArray("synonyms")?.forEach { synElem ->
            val syn = synElem?.takeIf { !it.isJsonNull }?.asString?.trim()
            if (!syn.isNullOrBlank() && !syn.equals(title, ignoreCase = true) && !altList.contains(syn)) {
                altList.add(syn)
            }
        }
        val alternateTitle = altList.distinct().joinToString("\n").takeIf { it.isNotBlank() }

        // Tags (genres + tags)
        val tagList = mutableListOf<String>()
        mediaObj.getAsJsonArray("genres")?.forEach { g ->
            val name = g?.takeIf { !it.isJsonNull }?.asString?.trim()
            if (!name.isNullOrBlank() && !tagList.contains(name)) tagList.add(name)
        }
        mediaObj.getAsJsonArray("tags")?.forEach { t ->
            if (t.isJsonObject) {
                val name = t.asJsonObject.get("name")?.takeIf { !it.isJsonNull }?.asString?.trim()
                if (!name.isNullOrBlank() && !tagList.contains(name)) tagList.add(name)
            }
        }
        val tags = tagList.take(15).joinToString(", ").takeIf { it.isNotBlank() }

        // Synopsis / Description
        val rawDesc = mediaObj.get("description")?.takeIf { !it.isJsonNull }?.asString
        val synopsis = cleanSynopsis(rawDesc)

        // External Score (AniList averageScore is out of 100, convert to 10-point scale e.g. 84 -> 8.4)
        val aniScoreInt = mediaObj.get("averageScore")?.takeIf { !it.isJsonNull }?.asInt
            ?: mediaObj.get("meanScore")?.takeIf { !it.isJsonNull }?.asInt
        val externalScore = aniScoreInt?.takeIf { it > 0 }?.let { it / 10.0f }
        val externalScoreSource = if (externalScore != null) "AniList" else null

        return JikanMediaDetails(
            malId = idMal,
            title = title,
            posterUrl = posterUrl,
            durationText = durationMinutes?.let { "$it min" },
            durationMinutes = durationMinutes,
            totalEpisodes = totalEpisodes,
            totalChapters = totalChapters,
            totalVolumes = totalVolumes,
            alternateTitle = alternateTitle,
            airedStartDate = airedStartDate,
            airedEndDate = airedEndDate,
            tags = tags,
            synopsis = synopsis,
            isAiring = isAiring,
            externalScore = externalScore,
            externalScoreSource = externalScoreSource,
            type = format,
            sourceProvider = "AniList"
        )
    }

    fun parseDateStringOrProp(dateStr: String?, propObj: JsonObject?): Long? {
        if (!dateStr.isNullOrBlank() && !dateStr.startsWith("0000")) {
            val parsed = parseIsoOrSimpleDate(dateStr)
            if (parsed != null) return parsed
        }
        if (propObj != null) {
            val year = propObj.get("year")?.takeIf { !it.isJsonNull }?.asInt
            val month = propObj.get("month")?.takeIf { !it.isJsonNull }?.asInt ?: 1
            val day = propObj.get("day")?.takeIf { !it.isJsonNull }?.asInt ?: 1
            if (year != null && year > 0) {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(year, (month - 1).coerceIn(0, 11), day.coerceIn(1, 31))
                }
                return cal.timeInMillis
            }
        }
        return null
    }

    fun parseIsoOrSimpleDate(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank() || dateStr.startsWith("0000")) return null
        val clean = dateStr.trim()
        try {
            if (clean.contains("T")) {
                return OffsetDateTime.parse(clean).toInstant().toEpochMilli()
            }
        } catch (_: Exception) {}
        try {
            if (clean.length == 10 && clean.contains("-")) {
                return LocalDate.parse(clean).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }
        } catch (_: Exception) {}
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val d = sdf.parse(clean)
            if (d != null) return d.time
        } catch (_: Exception) {}
        return null
    }

    fun parseAniListDate(dateObj: JsonObject?): Long? {
        if (dateObj == null) return null
        val year = dateObj.get("year")?.takeIf { !it.isJsonNull }?.asInt ?: return null
        val month = dateObj.get("month")?.takeIf { !it.isJsonNull }?.asInt ?: 1
        val day = dateObj.get("day")?.takeIf { !it.isJsonNull }?.asInt ?: 1
        if (year <= 0) return null
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, (month - 1).coerceIn(0, 11), day.coerceIn(1, 31))
        }
        return cal.timeInMillis
    }

    fun cleanSynopsis(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val noHtml = text
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</?[a-zA-Z0-9]+[^>]*>"), "")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trim()
        return noHtml.ifBlank { null }
    }

    fun parseDurationMinutes(durationStr: String?): Int? {
        if (durationStr.isNullOrBlank()) return null
        val hoursMatch = Regex("(\\d+)\\s*(?:hr|hour)", RegexOption.IGNORE_CASE).find(durationStr)
        val minsMatch = Regex("(\\d+)\\s*(?:min|minute)", RegexOption.IGNORE_CASE).find(durationStr)
        val hours = hoursMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val mins = minsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val total = (hours * 60) + mins
        return if (total > 0) total else null
    }

    fun queryJikanRelations(
        baseUrl: String = DEFAULT_BASE_URL,
        isManga: Boolean,
        malId: Long
    ): Result<List<RelatedMediaItem>> {
        val type = if (isManga) "manga" else "anime"
        val endpoint = "${baseUrl.trimEnd('/')}/$type/$malId/relations"
        return try {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return Result.failure(Exception("Jikan HTTP ${response.code} for relations"))
            }

            val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
            val dataArray = jsonObject?.getAsJsonArray("data") ?: return Result.success(emptyList())

            val list = mutableListOf<RelatedMediaItem>()
            for (i in 0 until dataArray.size()) {
                val relObj = dataArray.get(i).asJsonObject
                val relationName = relObj.get("relation")?.takeIf { !it.isJsonNull }?.asString ?: "Related"
                val entriesArray = relObj.getAsJsonArray("entry") ?: continue

                for (j in 0 until entriesArray.size()) {
                    val entryObj = entriesArray.get(j).asJsonObject
                    val id = entryObj.get("mal_id")?.takeIf { !it.isJsonNull }?.asLong ?: continue
                    val name = entryObj.get("name")?.takeIf { !it.isJsonNull }?.asString ?: continue
                    val entryType = entryObj.get("type")?.takeIf { !it.isJsonNull }?.asString ?: type
                    val mType = if (entryType.equals("anime", ignoreCase = true)) MediaType.ANIME else MediaType.MANGA

                    list.add(
                        RelatedMediaItem(
                            title = name,
                            relationship = relationName,
                            externalId = id.toString(),
                            externalSource = "MAL",
                            mediaType = mType
                        )
                    )
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.w(TAG, "Exception fetching Jikan relations for $malId: ${e.message}")
            Result.failure(e)
        }
    }

    fun formatAniListRelationType(relType: String?): String {
        return when (relType?.uppercase()) {
            "SEQUEL" -> "Sequel"
            "PREQUEL" -> "Prequel"
            "SIDE_STORY" -> "Side Story"
            "SPIN_OFF" -> "Spin-off"
            "ALTERNATIVE" -> "Alternative Version"
            "PARENT" -> "Parent Story"
            "SUMMARY" -> "Summary"
            "ADAPTATION" -> "Adaptation"
            "CHARACTER" -> "Character"
            "OTHER" -> "Other"
            null -> "Related"
            else -> relType.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
    }

    fun parseAniListRelations(mediaObj: JsonObject?): List<RelatedMediaItem> {
        if (mediaObj == null) return emptyList()
        val relationsObj = mediaObj.getAsJsonObject("relations") ?: return emptyList()
        val edgesArray = relationsObj.getAsJsonArray("edges") ?: return emptyList()

        val list = mutableListOf<RelatedMediaItem>()
        for (i in 0 until edgesArray.size()) {
            val edgeElem = edgesArray.get(i)
            if (!edgeElem.isJsonObject) continue
            val edgeObj = edgeElem.asJsonObject
            val relTypeRaw = edgeObj.get("relationType")?.takeIf { !it.isJsonNull }?.asString
            val nodeObj = edgeObj.getAsJsonObject("node") ?: continue

            val id = nodeObj.get("id")?.takeIf { !it.isJsonNull }?.asLong ?: continue
            val idMal = nodeObj.get("idMal")?.takeIf { !it.isJsonNull }?.asLong
            val finalExternalId = (idMal ?: id).toString()

            val titleObj = nodeObj.getAsJsonObject("title")
            val english = titleObj?.get("english")?.takeIf { !it.isJsonNull }?.asString?.trim()
            val userPreferred = titleObj?.get("userPreferred")?.takeIf { !it.isJsonNull }?.asString?.trim()
            val romaji = titleObj?.get("romaji")?.takeIf { !it.isJsonNull }?.asString?.trim()
            val title = english?.takeIf { it.isNotBlank() }
                ?: userPreferred?.takeIf { it.isNotBlank() }
                ?: romaji?.takeIf { it.isNotBlank() }
                ?: continue

            val typeStr = nodeObj.get("type")?.takeIf { !it.isJsonNull }?.asString ?: "ANIME"
            val mediaType = if (typeStr.equals("MANGA", ignoreCase = true)) MediaType.MANGA else MediaType.ANIME

            val coverImageObj = nodeObj.getAsJsonObject("coverImage")
            val posterUrl = coverImageObj?.get("large")?.takeIf { !it.isJsonNull }?.asString
                ?: coverImageObj?.get("medium")?.takeIf { !it.isJsonNull }?.asString

            val startDateObj = nodeObj.getAsJsonObject("startDate")
            val year = startDateObj?.get("year")?.takeIf { !it.isJsonNull }?.asInt
            val releaseYear = year?.takeIf { it > 0 }?.toString()

            val avgScore = nodeObj.get("averageScore")?.takeIf { !it.isJsonNull }?.asFloat
            val score = if (avgScore != null && avgScore > 0f) avgScore / 10f else null

            val formattedRel = formatAniListRelationType(relTypeRaw)

            list.add(
                RelatedMediaItem(
                    title = title,
                    relationship = formattedRel,
                    externalId = finalExternalId,
                    externalSource = "MAL",
                    mediaType = mediaType,
                    posterUrl = posterUrl,
                    releaseYear = releaseYear,
                    score = score
                )
            )
        }
        return list
    }

    fun queryAniListRelations(
        malId: Long? = null,
        aniListId: Long? = null,
        isManga: Boolean
    ): Result<List<RelatedMediaItem>> {
        val type = if (isManga) "MANGA" else "ANIME"
        val query = """
            query (${'$'}idMal: Int, ${'$'}id: Int, ${'$'}type: MediaType) {
              Media(idMal: ${'$'}idMal, id: ${'$'}id, type: ${'$'}type) {
                id
                idMal
                relations {
                  edges {
                    relationType
                    node {
                      id
                      idMal
                      title {
                        english
                        romaji
                        userPreferred
                      }
                      type
                      format
                      coverImage {
                        large
                        medium
                      }
                      averageScore
                      startDate {
                        year
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val jsonBody = JsonObject().apply {
            addProperty("query", query)
            val vars = JsonObject().apply {
                if (malId != null && malId > 0) addProperty("idMal", malId)
                if (aniListId != null && aniListId > 0) addProperty("id", aniListId)
                addProperty("type", type)
            }
            add("variables", vars)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://graphql.anilist.co")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .post(requestBody)
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            Log.d(TAG, "AniList relations HTTP $code for MAL ID $malId / AniList ID $aniListId ($type)")

            if (!response.isSuccessful) {
                return Result.failure(Exception("AniList HTTP $code: ${bodyString.take(200)}"))
            }

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
            val dataObj = rootObj?.getAsJsonObject("data")
            val mediaObj = dataObj?.getAsJsonObject("Media")
                ?: return Result.failure(NotFoundException("Media ID $malId relations not found on AniList"))

            Result.success(parseAniListRelations(mediaObj))
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying AniList relations for MAL ID $malId", e)
            Result.failure(e)
        }
    }

    fun queryAniListRelationsByTitle(searchTitle: String, isManga: Boolean): Result<List<RelatedMediaItem>> {
        val type = if (isManga) "MANGA" else "ANIME"
        val query = """
            query (${'$'}search: String, ${'$'}type: MediaType) {
              Media(search: ${'$'}search, type: ${'$'}type) {
                id
                idMal
                relations {
                  edges {
                    relationType
                    node {
                      id
                      idMal
                      title {
                        english
                        romaji
                        userPreferred
                      }
                      type
                      format
                      coverImage {
                        large
                        medium
                      }
                      averageScore
                      startDate {
                        year
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val jsonBody = JsonObject().apply {
            addProperty("query", query)
            val vars = JsonObject().apply {
                addProperty("search", searchTitle)
                addProperty("type", type)
            }
            add("variables", vars)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://graphql.anilist.co")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .post(requestBody)
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            Log.d(TAG, "AniList relations by title HTTP $code for '$searchTitle' ($type)")

            if (!response.isSuccessful) {
                return Result.failure(Exception("AniList HTTP $code: ${bodyString.take(200)}"))
            }

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
            val dataObj = rootObj?.getAsJsonObject("data")
            val mediaObj = dataObj?.getAsJsonObject("Media")
                ?: return Result.failure(NotFoundException("Title '$searchTitle' relations not found on AniList"))

            Result.success(parseAniListRelations(mediaObj))
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying AniList relations for title '$searchTitle'", e)
            Result.failure(e)
        }
    }

    fun fetchRelatedEntries(
        malId: Long? = null,
        isManga: Boolean,
        title: String? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<List<RelatedMediaItem>> {
        var lastException: Throwable? = null

        // 1. If malId is provided, try Jikan relations endpoint
        if (malId != null && malId > 0) {
            val jikanRes = queryJikanRelations(baseUrl, isManga, malId)
            if (jikanRes.isSuccess) {
                val items = jikanRes.getOrDefault(emptyList())
                if (items.isNotEmpty()) {
                    return jikanRes
                }
            } else {
                lastException = jikanRes.exceptionOrNull()
                Log.w(TAG, "Jikan relations query failed for MAL ID $malId (${lastException?.message}). Falling back to AniList...")
            }

            // 2. Fallback to AniList by MAL ID
            val anilistRes = queryAniListRelations(malId = malId, isManga = isManga)
            if (anilistRes.isSuccess) {
                val items = anilistRes.getOrDefault(emptyList())
                if (items.isNotEmpty()) {
                    return anilistRes
                }
            } else {
                lastException = anilistRes.exceptionOrNull()
                Log.w(TAG, "AniList relations query failed for MAL ID $malId (${lastException?.message}).")
            }

            // 2b. Also try as AniList ID directly
            val anilistDirectRes = queryAniListRelations(aniListId = malId, isManga = isManga)
            if (anilistDirectRes.isSuccess) {
                val items = anilistDirectRes.getOrDefault(emptyList())
                if (items.isNotEmpty()) {
                    return anilistDirectRes
                }
            }
        }

        // 3. Fallback to AniList by exact title if provided
        if (!title.isNullOrBlank()) {
            val rawRes = queryAniListRelationsByTitle(title.trim(), isManga)
            if (rawRes.isSuccess) {
                val items = rawRes.getOrDefault(emptyList())
                if (items.isNotEmpty()) {
                    return rawRes
                }
            } else {
                lastException = rawRes.exceptionOrNull()
            }

            val cleaned = cleanSearchTitle(title)
            if (cleaned != title.trim()) {
                val anilistTitleRes = queryAniListRelationsByTitle(cleaned, isManga)
                if (anilistTitleRes.isSuccess) {
                    val items = anilistTitleRes.getOrDefault(emptyList())
                    if (items.isNotEmpty()) {
                        return anilistTitleRes
                    }
                } else {
                    lastException = anilistTitleRes.exceptionOrNull()
                }
            }
        }

        // If we experienced an error during all attempts and malId or title was provided:
        if (lastException != null && (malId != null || !title.isNullOrBlank())) {
            return Result.failure(lastException)
        }

        return Result.success(emptyList())
    }
}

class RateLimitException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
