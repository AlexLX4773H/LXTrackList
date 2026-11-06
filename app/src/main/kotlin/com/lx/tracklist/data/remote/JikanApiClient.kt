package com.lx.tracklist.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
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
    val totalChapters: Int? = null,
    val totalVolumes: Int? = null,
    val type: String? = null,
    val sourceProvider: String = "Jikan"
)

object JikanApiClient {

    private const val TAG = "JikanApiClient"
    private val gson = Gson()
    const val DEFAULT_BASE_URL = "https://api.jikan.moe/v4"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 16; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36 LXTrackList/1.0"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
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

            val totalChapters = dataObj.get("chapters")?.takeIf { !it.isJsonNull }?.asInt
            val totalVolumes = dataObj.get("volumes")?.takeIf { !it.isJsonNull }?.asInt

            Result.success(
                JikanMediaDetails(
                    malId = malId,
                    title = title,
                    posterUrl = posterUrl,
                    durationText = durationText,
                    durationMinutes = durationMinutes,
                    totalChapters = totalChapters,
                    totalVolumes = totalVolumes,
                    type = mediaTypeStr,
                    sourceProvider = "Jikan"
                )
            )
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
                title { romaji english }
                coverImage { extraLarge large }
                duration
                chapters
                volumes
                format
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

            val titleObj = mediaObj.getAsJsonObject("title")
            val title = titleObj?.get("english")?.takeIf { !it.isJsonNull }?.asString
                ?: titleObj?.get("romaji")?.takeIf { !it.isJsonNull }?.asString

            val coverObj = mediaObj.getAsJsonObject("coverImage")
            val posterUrl = coverObj?.get("extraLarge")?.takeIf { !it.isJsonNull }?.asString
                ?: coverObj?.get("large")?.takeIf { !it.isJsonNull }?.asString

            val durationMinutes = mediaObj.get("duration")?.takeIf { !it.isJsonNull }?.asInt
            val totalChapters = mediaObj.get("chapters")?.takeIf { !it.isJsonNull }?.asInt
            val totalVolumes = mediaObj.get("volumes")?.takeIf { !it.isJsonNull }?.asInt
            val format = mediaObj.get("format")?.takeIf { !it.isJsonNull }?.asString

            Result.success(
                JikanMediaDetails(
                    malId = malId,
                    title = title,
                    posterUrl = posterUrl,
                    durationText = durationMinutes?.let { "$it min" },
                    durationMinutes = durationMinutes,
                    totalChapters = totalChapters,
                    totalVolumes = totalVolumes,
                    type = format,
                    sourceProvider = "AniList"
                )
            )
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

            val totalChapters = dataObj.get("chapters")?.takeIf { !it.isJsonNull }?.asInt
            val totalVolumes = dataObj.get("volumes")?.takeIf { !it.isJsonNull }?.asInt

            Result.success(
                JikanMediaDetails(
                    malId = malId,
                    title = title,
                    posterUrl = posterUrl,
                    durationText = durationText,
                    durationMinutes = durationMinutes,
                    totalChapters = totalChapters,
                    totalVolumes = totalVolumes,
                    type = mediaTypeStr,
                    sourceProvider = "Jikan"
                )
            )
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
                title { romaji english }
                coverImage { extraLarge large }
                duration
                chapters
                volumes
                format
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

            val titleObj = mediaObj.getAsJsonObject("title")
            val title = titleObj?.get("english")?.takeIf { !it.isJsonNull }?.asString
                ?: titleObj?.get("romaji")?.takeIf { !it.isJsonNull }?.asString

            val coverObj = mediaObj.getAsJsonObject("coverImage")
            val posterUrl = coverObj?.get("extraLarge")?.takeIf { !it.isJsonNull }?.asString
                ?: coverObj?.get("large")?.takeIf { !it.isJsonNull }?.asString

            val durationMinutes = mediaObj.get("duration")?.takeIf { !it.isJsonNull }?.asInt
            val totalChapters = mediaObj.get("chapters")?.takeIf { !it.isJsonNull }?.asInt
            val totalVolumes = mediaObj.get("volumes")?.takeIf { !it.isJsonNull }?.asInt
            val format = mediaObj.get("format")?.takeIf { !it.isJsonNull }?.asString

            Result.success(
                JikanMediaDetails(
                    malId = idMal,
                    title = title,
                    posterUrl = posterUrl,
                    durationText = durationMinutes?.let { "$it min" },
                    durationMinutes = durationMinutes,
                    totalChapters = totalChapters,
                    totalVolumes = totalVolumes,
                    type = format,
                    sourceProvider = "AniList"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying AniList title search for '$searchTitle'", e)
            Result.failure(e)
        }
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
}

class RateLimitException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
