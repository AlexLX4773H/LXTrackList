package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log
import androidx.core.text.HtmlCompat
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object GoogleBooksApiClient {

    private const val TAG = "GoogleBooksApiClient"
    private const val BASE_URL = "https://www.googleapis.com/books/v1/volumes"
    private const val USER_AGENT = "LXTrackList/1.0 (Android; Books Tracker)"
    private val gson = Gson()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun searchBooks(
        query: String,
        context: Context? = null,
        limit: Int = 20
    ): Result<List<BookDetails>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return Result.success(emptyList())

        val isIsbn = cleanQuery.replace("-", "").matches(Regex("^[0-9]{9}[0-9Xx]$|^[0-9]{13}$"))
        val formattedQuery = if (isIsbn) "isbn:${cleanQuery.replace("-", "")}" else cleanQuery
        val encodedQuery = URLEncoder.encode(formattedQuery, "UTF-8")
        val clampedLimit = limit.coerceIn(1, 40)

        val apiKey = GoogleBooksConfig.getApiKey(context)
        val keyParam = if (apiKey.isNotBlank()) "&key=$apiKey" else ""
        val endpoint = "$BASE_URL?q=$encodedQuery&maxResults=$clampedLimit$keyParam"

        return try {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.w(TAG, "Google Books search failed HTTP $code: ${bodyString.take(150)}")
                return Result.failure(Exception("Google Books HTTP $code: ${bodyString.take(150)}"))
            }

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
            val itemsArray = rootObj?.getAsJsonArray("items")
            if (itemsArray == null || itemsArray.size() == 0) {
                return Result.success(emptyList())
            }

            val results = mutableListOf<BookDetails>()
            for (i in 0 until itemsArray.size()) {
                val itemElem = itemsArray.get(i)
                if (itemElem.isJsonObject) {
                    try {
                        parseVolumeItem(itemElem.asJsonObject)?.let { results.add(it) }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing Google Books item at $i", e)
                    }
                }
            }
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying Google Books for '$cleanQuery'", e)
            Result.failure(e)
        }
    }

    fun searchByIsbn(
        isbn: String,
        context: Context? = null
    ): Result<BookDetails?> {
        val clean = isbn.replace(Regex("[^0-9Xx]"), "")
        if (clean.isBlank()) return Result.success(null)

        val searchRes = searchBooks("isbn:$clean", context, limit = 1)
        if (searchRes.isFailure) return Result.failure(searchRes.exceptionOrNull()!!)
        return Result.success(searchRes.getOrNull()?.firstOrNull())
    }

    fun fetchVolumeDetails(
        volumeId: String,
        context: Context? = null
    ): Result<BookDetails> {
        val apiKey = GoogleBooksConfig.getApiKey(context)
        val keyParam = if (apiKey.isNotBlank()) "?key=$apiKey" else ""
        val endpoint = "$BASE_URL/${volumeId.trim()}$keyParam"

        return try {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return Result.failure(Exception("Google Books volume HTTP ${response.code}: ${bodyString.take(150)}"))
            }

            val obj = gson.fromJson(bodyString, JsonObject::class.java)
            val details = parseVolumeItem(obj)
                ?: return Result.failure(Exception("Failed to parse volume details for $volumeId"))
            Result.success(details)
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching volume $volumeId", e)
            Result.failure(e)
        }
    }

    private fun parseVolumeItem(itemObj: JsonObject): BookDetails? {
        val id = itemObj.get("id")?.takeIf { !it.isJsonNull }?.asString ?: return null
        val volumeInfo = itemObj.getAsJsonObject("volumeInfo") ?: return null

        val title = volumeInfo.get("title")?.takeIf { !it.isJsonNull }?.asString?.trim() ?: "Unknown"
        val subtitle = volumeInfo.get("subtitle")?.takeIf { !it.isJsonNull }?.asString?.trim()

        val authors = mutableListOf<String>()
        volumeInfo.getAsJsonArray("authors")?.forEach { authElem ->
            if (!authElem.isJsonNull) {
                authors.add(authElem.asString.trim())
            }
        }

        val publisher = volumeInfo.get("publisher")?.takeIf { !it.isJsonNull }?.asString?.trim()
        val publishedDate = volumeInfo.get("publishedDate")?.takeIf { !it.isJsonNull }?.asString?.trim()

        val rawDescription = volumeInfo.get("description")?.takeIf { !it.isJsonNull }?.asString
        val cleanDescription = rawDescription?.let {
            try {
                HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
            } catch (_: Exception) {
                it.replace(Regex("<[^>]*>"), "").trim()
            }
        }

        val pageCount = volumeInfo.get("pageCount")?.takeIf { !it.isJsonNull }?.asInt

        var isbn10: String? = null
        var isbn13: String? = null
        volumeInfo.getAsJsonArray("industryIdentifiers")?.forEach { idElem ->
            if (idElem.isJsonObject) {
                val type = idElem.asJsonObject.get("type")?.takeIf { !it.isJsonNull }?.asString
                val identifier = idElem.asJsonObject.get("identifier")?.takeIf { !it.isJsonNull }?.asString
                if (type == "ISBN_10") isbn10 = identifier
                if (type == "ISBN_13") isbn13 = identifier
            }
        }

        val categories = mutableListOf<String>()
        volumeInfo.getAsJsonArray("categories")?.forEach { catElem ->
            if (!catElem.isJsonNull) {
                categories.add(catElem.asString.trim())
            }
        }

        val averageRating = volumeInfo.get("averageRating")?.takeIf { !it.isJsonNull }?.asFloat
        // Google Books ratings are out of 5; scale to 10
        val scaledScore = averageRating?.let { it * 2f }

        val imageLinks = volumeInfo.getAsJsonObject("imageLinks")
        val coverUrl = resolveBestCoverUrl(imageLinks)

        val language = volumeInfo.get("language")?.takeIf { !it.isJsonNull }?.asString
        val infoUrl = volumeInfo.get("infoLink")?.takeIf { !it.isJsonNull }?.asString

        return BookDetails(
            id = id,
            title = title,
            subtitle = subtitle,
            authors = authors,
            publisher = publisher,
            publishedDate = publishedDate,
            description = cleanDescription,
            pageCount = pageCount,
            isbn10 = isbn10,
            isbn13 = isbn13,
            coverUrl = coverUrl,
            categories = categories,
            averageScore = scaledScore,
            sourceProvider = "Google Books",
            language = language,
            infoUrl = infoUrl
        )
    }

    private fun resolveBestCoverUrl(imageLinks: JsonObject?): String? {
        if (imageLinks == null) return null
        val raw = imageLinks.get("extraLarge")?.takeIf { !it.isJsonNull }?.asString
            ?: imageLinks.get("large")?.takeIf { !it.isJsonNull }?.asString
            ?: imageLinks.get("medium")?.takeIf { !it.isJsonNull }?.asString
            ?: imageLinks.get("small")?.takeIf { !it.isJsonNull }?.asString
            ?: imageLinks.get("thumbnail")?.takeIf { !it.isJsonNull }?.asString
            ?: imageLinks.get("smallThumbnail")?.takeIf { !it.isJsonNull }?.asString
            ?: return null

        return raw.replace("http://", "https://")
    }
}

