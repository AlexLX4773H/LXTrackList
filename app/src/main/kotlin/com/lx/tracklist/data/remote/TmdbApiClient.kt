package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.RelatedMediaItem
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

data class TmdbMediaDetails(
    val tmdbId: Long,
    val title: String?,
    val posterUrl: String?,
    val backdropUrl: String? = null,
    val durationMinutes: Int? = null,
    val totalEpisodes: Int? = null,
    val totalSeasons: Int? = null,
    val alternateTitle: String? = null,
    val airedStartDate: Long? = null,
    val airedEndDate: Long? = null,
    val tags: String? = null,
    val synopsis: String? = null,
    val isAiring: Boolean? = null,
    val externalScore: Float? = null,
    val externalScoreSource: String = "TMDB",
    val type: String? = null, // "Movie" or "TV"
    val imdbId: String? = null,
    val sourceProvider: String = "TMDB"
)

data class TmdbSearchResult(
    val tmdbId: Long,
    val title: String,
    val originalTitle: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val overview: String? = null,
    val releaseDate: Long? = null,
    val releaseYear: String? = null,
    val voteAverage: Float? = null,
    val isMovie: Boolean
) {
    val webUrl: String
        get() = if (isMovie) "https://www.themoviedb.org/movie/$tmdbId" else "https://www.themoviedb.org/tv/$tmdbId"
}

object TmdbApiClient {

    private const val TAG = "TmdbApiClient"
    private val gson = Gson()
    const val DEFAULT_BASE_URL = "https://api.themoviedb.org/3"
    const val FALLBACK_BASE_URL = "https://api.tmdb.org/3"
    const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p"
    const val POSTER_SIZE_MEDIUM = "w500"
    const val POSTER_SIZE_ORIGINAL = "original"
    const val BACKDROP_SIZE = "w780"

    @Volatile
    private var okHttpClient: OkHttpClient = buildDefaultClient()

    @Volatile
    private var preferredBaseUrl: String = DEFAULT_BASE_URL

    private fun buildDefaultClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Initializes the OkHttpClient with DNS-over-HTTPS.
     * Call once from Application.onCreate() or lazily on first use.
     */
    fun initWithDoH(context: Context) {
        try {
            val dohResolver = TmdbDnsHelper.createDoHResolver(context)
            okHttpClient = OkHttpClient.Builder()
                .dns(dohResolver)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
            Log.i(TAG, "Initialized TMDB OkHttpClient with DNS-over-HTTPS resolver")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize DNS-over-HTTPS resolver for TMDB client", e)
        }
    }

    fun getPreferredBaseUrl(): String = preferredBaseUrl

    fun resetPreferredBaseUrl() {
        preferredBaseUrl = DEFAULT_BASE_URL
    }

    private fun isConnectionFailure(ex: Throwable?): Boolean {
        var current: Throwable? = ex
        while (current != null) {
            if (current is ConnectException ||
                current is SocketTimeoutException ||
                current is UnknownHostException ||
                current is SocketException ||
                current is SSLException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private inline fun <T> withFallback(
        baseUrl: String = DEFAULT_BASE_URL,
        operation: (baseUrl: String) -> Result<T>
    ): Result<T> {
        if (baseUrl != DEFAULT_BASE_URL) {
            val result = operation(baseUrl)
            if (result.isFailure && isConnectionFailure(result.exceptionOrNull())) {
                val fallbackUrl = if (baseUrl == FALLBACK_BASE_URL) DEFAULT_BASE_URL else FALLBACK_BASE_URL
                Log.w(TAG, "TMDB explicit domain $baseUrl unreachable, retrying with fallback $fallbackUrl")
                return operation(fallbackUrl)
            }
            return result
        }

        val currentUrl = preferredBaseUrl
        val result = operation(currentUrl)
        if (result.isFailure && isConnectionFailure(result.exceptionOrNull())) {
            val fallbackUrl = if (currentUrl == DEFAULT_BASE_URL) FALLBACK_BASE_URL else DEFAULT_BASE_URL
            Log.w(TAG, "TMDB domain $currentUrl unreachable (${result.exceptionOrNull()?.javaClass?.simpleName}), switching to fallback $fallbackUrl")
            val fallbackResult = operation(fallbackUrl)
            if (fallbackResult.isSuccess) {
                preferredBaseUrl = fallbackUrl
                Log.i(TAG, "Sticky TMDB domain updated to $fallbackUrl")
            }
            return fallbackResult
        }
        return result
    }

    fun buildImageUrl(path: String?, size: String = POSTER_SIZE_MEDIUM): String? {
        if (path.isNullOrBlank()) return null
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "$IMAGE_BASE_URL/$size$cleanPath"
    }

    private fun buildRequest(url: String, token: String): Request {
        val builder = Request.Builder()
            .header("Accept", "application/json")

        // If the token is a v3 API key (shorter string without periods)
        val finalUrl = if (!token.contains(".") && token.length <= 40) {
            val separator = if (url.contains("?")) "&" else "?"
            "$url${separator}api_key=$token"
        } else {
            builder.header("Authorization", "Bearer $token")
            url
        }

        return builder.url(finalUrl).get().build()
    }

    fun fetchMovieDetails(
        tmdbId: Long,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<TmdbMediaDetails> = withFallback(baseUrl) { activeBaseUrl ->
        fetchMovieDetailsInternal(tmdbId, context, activeBaseUrl)
    }

    private fun fetchMovieDetailsInternal(
        tmdbId: Long,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<TmdbMediaDetails> {
        val token = TmdbConfig.getAccessToken(context)
        if (token.isBlank()) {
            return Result.failure(TmdbAuthException("TMDB Read Access Token is not configured."))
        }

        val endpoint = "${baseUrl.trimEnd('/')}/movie/$tmdbId?append_to_response=alternative_titles,credits"
        return try {
            val request = buildRequest(endpoint, token)
            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            Log.d(TAG, "TMDB movie details HTTP $code for $endpoint")

            if (code == 429) {
                return Result.failure(TmdbRateLimitException("TMDB rate limit (429) for movie #$tmdbId"))
            }
            if (code == 401) {
                return Result.failure(TmdbAuthException("Invalid TMDB API credentials (401)"))
            }
            if (code == 404) {
                return Result.failure(TmdbNotFoundException("TMDB movie #$tmdbId not found"))
            }
            if (!response.isSuccessful) {
                return Result.failure(Exception("TMDB HTTP $code: ${bodyString.take(200)}"))
            }

            val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
                ?: return Result.failure(Exception("Empty JSON response for movie #$tmdbId"))

            Result.success(parseMovieDetails(jsonObject, tmdbId))
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching TMDB movie #$tmdbId", e)
            Result.failure(e)
        }
    }

    fun fetchTvDetails(
        tmdbId: Long,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<TmdbMediaDetails> = withFallback(baseUrl) { activeBaseUrl ->
        fetchTvDetailsInternal(tmdbId, context, activeBaseUrl)
    }

    private fun fetchTvDetailsInternal(
        tmdbId: Long,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<TmdbMediaDetails> {
        val token = TmdbConfig.getAccessToken(context)
        if (token.isBlank()) {
            return Result.failure(TmdbAuthException("TMDB Read Access Token is not configured."))
        }

        val endpoint = "${baseUrl.trimEnd('/')}/tv/$tmdbId?append_to_response=alternative_titles,credits"
        return try {
            val request = buildRequest(endpoint, token)
            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            Log.d(TAG, "TMDB TV details HTTP $code for $endpoint")

            if (code == 429) {
                return Result.failure(TmdbRateLimitException("TMDB rate limit (429) for TV show #$tmdbId"))
            }
            if (code == 401) {
                return Result.failure(TmdbAuthException("Invalid TMDB API credentials (401)"))
            }
            if (code == 404) {
                return Result.failure(TmdbNotFoundException("TMDB TV show #$tmdbId not found"))
            }
            if (!response.isSuccessful) {
                return Result.failure(Exception("TMDB HTTP $code: ${bodyString.take(200)}"))
            }

            val jsonObject = gson.fromJson(bodyString, JsonObject::class.java)
                ?: return Result.failure(Exception("Empty JSON response for TV show #$tmdbId"))

            Result.success(parseTvDetails(jsonObject, tmdbId))
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching TMDB TV show #$tmdbId", e)
            Result.failure(e)
        }
    }

    fun fetchMediaDetails(
        tmdbId: Long,
        isTv: Boolean,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<TmdbMediaDetails> {
        return if (isTv) {
            val tvResult = fetchTvDetails(tmdbId, context, baseUrl)
            if (tvResult.isSuccess) tvResult else fetchMovieDetails(tmdbId, context, baseUrl)
        } else {
            val movieResult = fetchMovieDetails(tmdbId, context, baseUrl)
            if (movieResult.isSuccess) movieResult else fetchTvDetails(tmdbId, context, baseUrl)
        }
    }

    fun searchMovieByTitle(
        title: String,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<TmdbMediaDetails> = withFallback(baseUrl) { activeBaseUrl ->
        searchMovieByTitleInternal(title, context, activeBaseUrl)
    }

    private fun searchMovieByTitleInternal(
        title: String,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<TmdbMediaDetails> {
        val token = TmdbConfig.getAccessToken(context)
        if (token.isBlank()) {
            return Result.failure(TmdbAuthException("TMDB Read Access Token is not configured."))
        }

        val encodedQuery = URLEncoder.encode(title.trim(), "UTF-8")
        val endpoint = "${baseUrl.trimEnd('/')}/search/movie?query=$encodedQuery&page=1&include_adult=false"

        return try {
            val request = buildRequest(endpoint, token)
            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            Log.d(TAG, "TMDB movie search HTTP $code for '$title'")

            if (code == 429) return Result.failure(TmdbRateLimitException("TMDB rate limit for '$title'"))
            if (code == 401) return Result.failure(TmdbAuthException("Invalid TMDB API credentials"))
            if (!response.isSuccessful) return Result.failure(Exception("TMDB search HTTP $code: ${bodyString.take(200)}"))

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
            val results = rootObj?.getAsJsonArray("results")
            if (results == null || results.size() == 0) {
                return Result.failure(TmdbNotFoundException("No movie results for '$title' on TMDB"))
            }

            val firstMatch = results.get(0).asJsonObject
            val tmdbId = firstMatch.get("id")?.takeIf { !it.isJsonNull }?.asLong
                ?: return Result.failure(TmdbNotFoundException("Missing ID in TMDB movie search result"))

            // Fetch full movie details to get runtime, full genres, alt titles, imdb_id
            fetchMovieDetails(tmdbId, context, baseUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Exception searching TMDB movie for '$title'", e)
            Result.failure(e)
        }
    }

    fun searchTvByTitle(
        title: String,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<TmdbMediaDetails> = withFallback(baseUrl) { activeBaseUrl ->
        searchTvByTitleInternal(title, context, activeBaseUrl)
    }

    private fun searchTvByTitleInternal(
        title: String,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<TmdbMediaDetails> {
        val token = TmdbConfig.getAccessToken(context)
        if (token.isBlank()) {
            return Result.failure(TmdbAuthException("TMDB Read Access Token is not configured."))
        }

        val encodedQuery = URLEncoder.encode(title.trim(), "UTF-8")
        val endpoint = "${baseUrl.trimEnd('/')}/search/tv?query=$encodedQuery&page=1&include_adult=false"

        return try {
            val request = buildRequest(endpoint, token)
            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            Log.d(TAG, "TMDB TV search HTTP $code for '$title'")

            if (code == 429) return Result.failure(TmdbRateLimitException("TMDB rate limit for '$title'"))
            if (code == 401) return Result.failure(TmdbAuthException("Invalid TMDB API credentials"))
            if (!response.isSuccessful) return Result.failure(Exception("TMDB search HTTP $code: ${bodyString.take(200)}"))

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
            val results = rootObj?.getAsJsonArray("results")
            if (results == null || results.size() == 0) {
                return Result.failure(TmdbNotFoundException("No TV show results for '$title' on TMDB"))
            }

            val firstMatch = results.get(0).asJsonObject
            val tmdbId = firstMatch.get("id")?.takeIf { !it.isJsonNull }?.asLong
                ?: return Result.failure(TmdbNotFoundException("Missing ID in TMDB TV search result"))

            // Fetch full TV details to get seasons, episodes, runtime, full genres, alt titles
            fetchTvDetails(tmdbId, context, baseUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Exception searching TMDB TV for '$title'", e)
            Result.failure(e)
        }
    }

    fun searchMoviesMultiple(
        title: String,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<List<TmdbSearchResult>> = withFallback(baseUrl) { activeBaseUrl ->
        searchMoviesMultipleInternal(title, context, activeBaseUrl)
    }

    private fun searchMoviesMultipleInternal(
        title: String,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<List<TmdbSearchResult>> {
        val token = TmdbConfig.getAccessToken(context)
        if (token.isBlank()) {
            return Result.failure(TmdbAuthException("TMDB Read Access Token is not configured."))
        }

        val encodedQuery = URLEncoder.encode(title.trim(), "UTF-8")
        val endpoint = "${baseUrl.trimEnd('/')}/search/movie?query=$encodedQuery&page=1&include_adult=false"

        return try {
            val request = buildRequest(endpoint, token)
            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return Result.failure(Exception("TMDB search HTTP $code: ${bodyString.take(200)}"))
            }

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
            val results = rootObj?.getAsJsonArray("results") ?: return Result.success(emptyList())

            val list = mutableListOf<TmdbSearchResult>()
            for (i in 0 until results.size()) {
                val item = results.get(i).asJsonObject
                val id = item.get("id")?.takeIf { !it.isJsonNull }?.asLong ?: continue
                val itemTitle = item.get("title")?.takeIf { !it.isJsonNull }?.asString
                    ?: item.get("original_title")?.takeIf { !it.isJsonNull }?.asString ?: "Untitled"
                val origTitle = item.get("original_title")?.takeIf { !it.isJsonNull }?.asString
                val posterPath = item.get("poster_path")?.takeIf { !it.isJsonNull }?.asString
                val backdropPath = item.get("backdrop_path")?.takeIf { !it.isJsonNull }?.asString
                val posterUrl = buildImageUrl(posterPath) ?: buildImageUrl(backdropPath)
                val backdropUrl = buildImageUrl(backdropPath, BACKDROP_SIZE)
                val releaseDateStr = item.get("release_date")?.takeIf { !it.isJsonNull }?.asString
                val releaseDate = parseDateString(releaseDateStr)
                val releaseYear = releaseDateStr?.take(4)?.takeIf { it.length == 4 && it.all { c -> c.isDigit() } }
                val voteAvg = item.get("vote_average")?.takeIf { !it.isJsonNull }?.asFloat?.takeIf { it > 0f }
                val overview = item.get("overview")?.takeIf { !it.isJsonNull }?.asString?.trim()?.ifBlank { null }

                list.add(
                    TmdbSearchResult(
                        tmdbId = id,
                        title = itemTitle,
                        originalTitle = origTitle,
                        posterUrl = posterUrl,
                        backdropUrl = backdropUrl,
                        overview = overview,
                        releaseDate = releaseDate,
                        releaseYear = releaseYear,
                        voteAverage = voteAvg,
                        isMovie = true
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Exception searching TMDB movies for '$title'", e)
            Result.failure(e)
        }
    }

    fun searchTvMultiple(
        title: String,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<List<TmdbSearchResult>> = withFallback(baseUrl) { activeBaseUrl ->
        searchTvMultipleInternal(title, context, activeBaseUrl)
    }

    private fun searchTvMultipleInternal(
        title: String,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<List<TmdbSearchResult>> {
        val token = TmdbConfig.getAccessToken(context)
        if (token.isBlank()) {
            return Result.failure(TmdbAuthException("TMDB Read Access Token is not configured."))
        }

        val encodedQuery = URLEncoder.encode(title.trim(), "UTF-8")
        val endpoint = "${baseUrl.trimEnd('/')}/search/tv?query=$encodedQuery&page=1&include_adult=false"

        return try {
            val request = buildRequest(endpoint, token)
            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return Result.failure(Exception("TMDB search HTTP $code: ${bodyString.take(200)}"))
            }

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
            val results = rootObj?.getAsJsonArray("results") ?: return Result.success(emptyList())

            val list = mutableListOf<TmdbSearchResult>()
            for (i in 0 until results.size()) {
                val item = results.get(i).asJsonObject
                val id = item.get("id")?.takeIf { !it.isJsonNull }?.asLong ?: continue
                val itemTitle = item.get("name")?.takeIf { !it.isJsonNull }?.asString
                    ?: item.get("original_name")?.takeIf { !it.isJsonNull }?.asString ?: "Untitled"
                val origTitle = item.get("original_name")?.takeIf { !it.isJsonNull }?.asString
                val posterPath = item.get("poster_path")?.takeIf { !it.isJsonNull }?.asString
                val backdropPath = item.get("backdrop_path")?.takeIf { !it.isJsonNull }?.asString
                val posterUrl = buildImageUrl(posterPath) ?: buildImageUrl(backdropPath)
                val backdropUrl = buildImageUrl(backdropPath, BACKDROP_SIZE)
                val airDateStr = item.get("first_air_date")?.takeIf { !it.isJsonNull }?.asString
                val releaseDate = parseDateString(airDateStr)
                val releaseYear = airDateStr?.take(4)?.takeIf { it.length == 4 && it.all { c -> c.isDigit() } }
                val voteAvg = item.get("vote_average")?.takeIf { !it.isJsonNull }?.asFloat?.takeIf { it > 0f }
                val overview = item.get("overview")?.takeIf { !it.isJsonNull }?.asString?.trim()?.ifBlank { null }

                list.add(
                    TmdbSearchResult(
                        tmdbId = id,
                        title = itemTitle,
                        originalTitle = origTitle,
                        posterUrl = posterUrl,
                        backdropUrl = backdropUrl,
                        overview = overview,
                        releaseDate = releaseDate,
                        releaseYear = releaseYear,
                        voteAverage = voteAvg,
                        isMovie = false
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Exception searching TMDB TV for '$title'", e)
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
        isTv: Boolean,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<TmdbMediaDetails> {
        val rawTitle = title.trim()
        if (rawTitle.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be blank"))

        val cleanedTitle = cleanSearchTitle(rawTitle)

        // 1. Search primary media type with cleaned title
        val primaryResult = if (isTv) {
            searchTvByTitle(cleanedTitle, context, baseUrl)
        } else {
            searchMovieByTitle(cleanedTitle, context, baseUrl)
        }
        if (primaryResult.isSuccess && !primaryResult.getOrNull()?.posterUrl.isNullOrBlank()) {
            return primaryResult
        }

        // 2. Try raw title with primary type if cleaned title differed
        if (cleanedTitle != rawTitle) {
            val rawResult = if (isTv) {
                searchTvByTitle(rawTitle, context, baseUrl)
            } else {
                searchMovieByTitle(rawTitle, context, baseUrl)
            }
            if (rawResult.isSuccess && !rawResult.getOrNull()?.posterUrl.isNullOrBlank()) {
                return rawResult
            }
        }

        // 3. Cross-fallback: if movie search failed, try TV (or vice versa)
        val crossResult = if (isTv) {
            searchMovieByTitle(cleanedTitle, context, baseUrl)
        } else {
            searchTvByTitle(cleanedTitle, context, baseUrl)
        }
        if (crossResult.isSuccess && !crossResult.getOrNull()?.posterUrl.isNullOrBlank()) {
            return crossResult
        }

        // 4. Cross-fallback with raw title
        if (cleanedTitle != rawTitle) {
            val rawCrossResult = if (isTv) {
                searchMovieByTitle(rawTitle, context, baseUrl)
            } else {
                searchTvByTitle(rawTitle, context, baseUrl)
            }
            if (rawCrossResult.isSuccess && !rawCrossResult.getOrNull()?.posterUrl.isNullOrBlank()) {
                return rawCrossResult
            }
        }

        return primaryResult
    }

    fun parseMovieDetails(obj: JsonObject, fallbackId: Long): TmdbMediaDetails {
        val id = obj.get("id")?.takeIf { !it.isJsonNull }?.asLong ?: fallbackId
        val title = obj.get("title")?.takeIf { !it.isJsonNull }?.asString
            ?: obj.get("original_title")?.takeIf { !it.isJsonNull }?.asString

        val originalTitle = obj.get("original_title")?.takeIf { !it.isJsonNull }?.asString
        val posterPath = obj.get("poster_path")?.takeIf { !it.isJsonNull }?.asString
        val backdropPath = obj.get("backdrop_path")?.takeIf { !it.isJsonNull }?.asString
        val posterUrl = buildImageUrl(posterPath) ?: buildImageUrl(backdropPath)
        val backdropUrl = buildImageUrl(backdropPath, BACKDROP_SIZE)

        val runtime = obj.get("runtime")?.takeIf { !it.isJsonNull }?.asInt
        val durationMinutes = if (runtime != null && runtime > 0) runtime else null

        val releaseDateStr = obj.get("release_date")?.takeIf { !it.isJsonNull }?.asString
        val airedStartDate = parseDateString(releaseDateStr)

        val voteAvg = obj.get("vote_average")?.takeIf { !it.isJsonNull }?.asFloat
        val externalScore = voteAvg?.takeIf { it > 0f }

        val overview = obj.get("overview")?.takeIf { !it.isJsonNull }?.asString?.trim()?.ifBlank { null }
        val imdbId = obj.get("imdb_id")?.takeIf { !it.isJsonNull }?.asString?.trim()?.ifBlank { null }

        // Genres
        val tagList = mutableListOf<String>()
        obj.getAsJsonArray("genres")?.forEach { elem ->
            if (elem.isJsonObject) {
                val gName = elem.asJsonObject.get("name")?.takeIf { !it.isJsonNull }?.asString?.trim()
                if (!gName.isNullOrBlank() && !tagList.contains(gName)) tagList.add(gName)
            }
        }
        val tags = tagList.joinToString(", ").takeIf { it.isNotBlank() }

        // Alternative titles
        val altList = mutableListOf<String>()
        if (!originalTitle.isNullOrBlank() && !originalTitle.equals(title, ignoreCase = true)) {
            altList.add(originalTitle)
        }
        val altTitlesObj = obj.getAsJsonObject("alternative_titles")
        altTitlesObj?.getAsJsonArray("titles")?.forEach { elem ->
            if (elem.isJsonObject) {
                val t = elem.asJsonObject.get("title")?.takeIf { !it.isJsonNull }?.asString?.trim()
                if (!t.isNullOrBlank() && !t.equals(title, ignoreCase = true) && !altList.contains(t)) {
                    altList.add(t)
                }
            }
        }
        val alternateTitle = altList.distinct().joinToString("\n").takeIf { it.isNotBlank() }

        val statusStr = obj.get("status")?.takeIf { !it.isJsonNull }?.asString
        val isAiring = statusStr != null && (statusStr.equals("Post Production", ignoreCase = true) ||
                statusStr.equals("In Production", ignoreCase = true) ||
                statusStr.equals("Planned", ignoreCase = true))

        return TmdbMediaDetails(
            tmdbId = id,
            title = title,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            durationMinutes = durationMinutes,
            totalEpisodes = null,
            totalSeasons = null,
            alternateTitle = alternateTitle,
            airedStartDate = airedStartDate,
            airedEndDate = null,
            tags = tags,
            synopsis = overview,
            isAiring = isAiring,
            externalScore = externalScore,
            externalScoreSource = "TMDB",
            type = "Movie",
            imdbId = imdbId,
            sourceProvider = "TMDB"
        )
    }

    fun parseTvDetails(obj: JsonObject, fallbackId: Long): TmdbMediaDetails {
        val id = obj.get("id")?.takeIf { !it.isJsonNull }?.asLong ?: fallbackId
        val name = obj.get("name")?.takeIf { !it.isJsonNull }?.asString
            ?: obj.get("original_name")?.takeIf { !it.isJsonNull }?.asString

        val originalName = obj.get("original_name")?.takeIf { !it.isJsonNull }?.asString
        val posterPath = obj.get("poster_path")?.takeIf { !it.isJsonNull }?.asString
        val backdropPath = obj.get("backdrop_path")?.takeIf { !it.isJsonNull }?.asString
        val posterUrl = buildImageUrl(posterPath) ?: buildImageUrl(backdropPath)
        val backdropUrl = buildImageUrl(backdropPath, BACKDROP_SIZE)

        // Episode runtime (array of ints, take first or average)
        var durationMinutes: Int? = null
        val runtimes = obj.getAsJsonArray("episode_run_time")
        if (runtimes != null && runtimes.size() > 0) {
            val first = runtimes.get(0)?.takeIf { !it.isJsonNull }?.asInt
            if (first != null && first > 0) {
                durationMinutes = first
            }
        }

        val totalEpisodes = obj.get("number_of_episodes")?.takeIf { !it.isJsonNull }?.asInt
        val totalSeasons = obj.get("number_of_seasons")?.takeIf { !it.isJsonNull }?.asInt

        val firstAirDateStr = obj.get("first_air_date")?.takeIf { !it.isJsonNull }?.asString
        val lastAirDateStr = obj.get("last_air_date")?.takeIf { !it.isJsonNull }?.asString
        val airedStartDate = parseDateString(firstAirDateStr)
        val airedEndDate = parseDateString(lastAirDateStr)

        val voteAvg = obj.get("vote_average")?.takeIf { !it.isJsonNull }?.asFloat
        val externalScore = voteAvg?.takeIf { it > 0f }

        val overview = obj.get("overview")?.takeIf { !it.isJsonNull }?.asString?.trim()?.ifBlank { null }

        // Genres
        val tagList = mutableListOf<String>()
        obj.getAsJsonArray("genres")?.forEach { elem ->
            if (elem.isJsonObject) {
                val gName = elem.asJsonObject.get("name")?.takeIf { !it.isJsonNull }?.asString?.trim()
                if (!gName.isNullOrBlank() && !tagList.contains(gName)) tagList.add(gName)
            }
        }
        val tags = tagList.joinToString(", ").takeIf { it.isNotBlank() }

        // Alternative titles
        val altList = mutableListOf<String>()
        if (!originalName.isNullOrBlank() && !originalName.equals(name, ignoreCase = true)) {
            altList.add(originalName)
        }
        val altTitlesObj = obj.getAsJsonObject("alternative_titles")
        altTitlesObj?.getAsJsonArray("results")?.forEach { elem ->
            if (elem.isJsonObject) {
                val t = elem.asJsonObject.get("title")?.takeIf { !it.isJsonNull }?.asString?.trim()
                if (!t.isNullOrBlank() && !t.equals(name, ignoreCase = true) && !altList.contains(t)) {
                    altList.add(t)
                }
            }
        }
        val alternateTitle = altList.distinct().joinToString("\n").takeIf { it.isNotBlank() }

        val statusStr = obj.get("status")?.takeIf { !it.isJsonNull }?.asString
        val isAiring = statusStr != null && (
                statusStr.equals("Returning Series", ignoreCase = true) ||
                        statusStr.equals("In Production", ignoreCase = true) ||
                        statusStr.equals("Planned", ignoreCase = true)
                )

        return TmdbMediaDetails(
            tmdbId = id,
            title = name,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            durationMinutes = durationMinutes,
            totalEpisodes = totalEpisodes,
            totalSeasons = totalSeasons,
            alternateTitle = alternateTitle,
            airedStartDate = airedStartDate,
            airedEndDate = airedEndDate,
            tags = tags,
            synopsis = overview,
            isAiring = isAiring,
            externalScore = externalScore,
            externalScoreSource = "TMDB",
            type = "TV",
            imdbId = null,
            sourceProvider = "TMDB"
        )
    }

    fun parseDateString(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank() || dateStr.startsWith("0000")) return null
        val clean = dateStr.trim()
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

    fun fetchRelatedEntries(
        tmdbId: Long,
        isTv: Boolean,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<List<RelatedMediaItem>> = withFallback(baseUrl) { activeBaseUrl ->
        fetchRelatedEntriesInternal(tmdbId, isTv, context, activeBaseUrl)
    }

    private fun fetchRelatedEntriesInternal(
        tmdbId: Long,
        isTv: Boolean,
        context: Context? = null,
        baseUrl: String = DEFAULT_BASE_URL
    ): Result<List<RelatedMediaItem>> {
        val token = TmdbConfig.getAccessToken(context)
        if (token.isBlank()) {
            return Result.failure(TmdbAuthException("TMDB token not configured"))
        }

        val list = mutableListOf<RelatedMediaItem>()
        val seenIds = mutableSetOf<Long>()
        seenIds.add(tmdbId)

        return try {
            if (!isTv) {
                val movieEndpoint = "${baseUrl.trimEnd('/')}/movie/$tmdbId?append_to_response=recommendations"
                val request = buildRequest(movieEndpoint, token)
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val root = gson.fromJson(body, JsonObject::class.java)
                    val currentReleaseDate = root.get("release_date")?.takeIf { !it.isJsonNull }?.asString ?: ""

                    // Check collection (sequels / prequels in franchise)
                    val collectionObj = root.getAsJsonObject("belongs_to_collection")
                    val collectionId = collectionObj?.get("id")?.takeIf { !it.isJsonNull }?.asLong
                    if (collectionId != null) {
                        val collectionEndpoint = "${baseUrl.trimEnd('/')}/collection/$collectionId"
                        val colReq = buildRequest(collectionEndpoint, token)
                        val colResp = okHttpClient.newCall(colReq).execute()
                        val colBody = colResp.body?.string() ?: ""
                        if (colResp.isSuccessful) {
                            val colJson = gson.fromJson(colBody, JsonObject::class.java)
                            val parts = colJson.getAsJsonArray("parts")
                            if (parts != null) {
                                for (i in 0 until parts.size()) {
                                    val part = parts.get(i).asJsonObject
                                    val partId = part.get("id")?.takeIf { !it.isJsonNull }?.asLong ?: continue
                                    if (seenIds.contains(partId)) continue
                                    seenIds.add(partId)

                                    val partTitle = part.get("title")?.takeIf { !it.isJsonNull }?.asString ?: continue
                                    val partDate = part.get("release_date")?.takeIf { !it.isJsonNull }?.asString ?: ""
                                    val partPoster = buildImageUrl(part.get("poster_path")?.takeIf { !it.isJsonNull }?.asString)
                                    val partScore = part.get("vote_average")?.takeIf { !it.isJsonNull }?.asFloat
                                    val relYear = partDate.take(4).takeIf { it.length == 4 && it.all { c -> c.isDigit() } }

                                    val relationship = if (partDate.isNotBlank() && currentReleaseDate.isNotBlank()) {
                                        if (partDate > currentReleaseDate) "Sequel" else "Prequel"
                                    } else {
                                        "Part of Series"
                                    }

                                    list.add(
                                        RelatedMediaItem(
                                            title = partTitle,
                                            relationship = relationship,
                                            externalId = partId.toString(),
                                            externalSource = "TMDB",
                                            mediaType = MediaType.MOVIE,
                                            posterUrl = partPoster,
                                            releaseYear = relYear,
                                            score = partScore
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Also recommendations
                    val recResults = root.getAsJsonObject("recommendations")?.getAsJsonArray("results")
                    if (recResults != null) {
                        for (i in 0 until recResults.size().coerceAtMost(6)) {
                            val rec = recResults.get(i).asJsonObject
                            val recId = rec.get("id")?.takeIf { !it.isJsonNull }?.asLong ?: continue
                            if (seenIds.contains(recId)) continue
                            seenIds.add(recId)

                            val recTitle = rec.get("title")?.takeIf { !it.isJsonNull }?.asString ?: continue
                            val recDate = rec.get("release_date")?.takeIf { !it.isJsonNull }?.asString ?: ""
                            val recPoster = buildImageUrl(rec.get("poster_path")?.takeIf { !it.isJsonNull }?.asString)
                            val recScore = rec.get("vote_average")?.takeIf { !it.isJsonNull }?.asFloat
                            val relYear = recDate.take(4).takeIf { it.length == 4 && it.all { c -> c.isDigit() } }

                            list.add(
                                RelatedMediaItem(
                                    title = recTitle,
                                    relationship = "Related",
                                    externalId = recId.toString(),
                                    externalSource = "TMDB",
                                    mediaType = MediaType.MOVIE,
                                    posterUrl = recPoster,
                                    releaseYear = relYear,
                                    score = recScore
                                )
                            )
                        }
                    }
                }
            } else {
                // TV recommendations
                val tvEndpoint = "${baseUrl.trimEnd('/')}/tv/$tmdbId?append_to_response=recommendations"
                val request = buildRequest(tvEndpoint, token)
                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val root = gson.fromJson(body, JsonObject::class.java)
                    val recResults = root.getAsJsonObject("recommendations")?.getAsJsonArray("results")
                    if (recResults != null) {
                        for (i in 0 until recResults.size().coerceAtMost(6)) {
                            val rec = recResults.get(i).asJsonObject
                            val recId = rec.get("id")?.takeIf { !it.isJsonNull }?.asLong ?: continue
                            if (seenIds.contains(recId)) continue
                            seenIds.add(recId)

                            val recTitle = rec.get("name")?.takeIf { !it.isJsonNull }?.asString
                                ?: rec.get("original_name")?.takeIf { !it.isJsonNull }?.asString ?: continue
                            val recDate = rec.get("first_air_date")?.takeIf { !it.isJsonNull }?.asString ?: ""
                            val recPoster = buildImageUrl(rec.get("poster_path")?.takeIf { !it.isJsonNull }?.asString)
                            val recScore = rec.get("vote_average")?.takeIf { !it.isJsonNull }?.asFloat
                            val relYear = recDate.take(4).takeIf { it.length == 4 && it.all { c -> c.isDigit() } }

                            list.add(
                                RelatedMediaItem(
                                    title = recTitle,
                                    relationship = "Related",
                                    externalId = recId.toString(),
                                    externalSource = "TMDB",
                                    mediaType = MediaType.SHOW,
                                    posterUrl = recPoster,
                                    releaseYear = relYear,
                                    score = recScore
                                )
                            )
                        }
                    }
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.w(TAG, "Exception fetching TMDB related entries for #$tmdbId", e)
            Result.failure(e)
        }
    }
}

class TmdbRateLimitException(message: String) : Exception(message)
class TmdbNotFoundException(message: String) : Exception(message)
class TmdbAuthException(message: String) : Exception(message)

