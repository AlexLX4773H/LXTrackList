package com.lx.tracklist.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object OpenLibraryApiClient {

    private const val TAG = "OpenLibraryApiClient"
    private const val BASE_SEARCH_URL = "https://openlibrary.org/search.json"
    private const val BASE_ISBN_URL = "https://openlibrary.org/isbn"
    private const val COVERS_CDN_URL = "https://covers.openlibrary.org/b"
    private const val USER_AGENT = "LXTrackList/1.0 (Android; https://github.com/RyanYuuki/AnymeX)"
    private val gson = Gson()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun searchBooks(
        query: String,
        limit: Int = 20
    ): Result<List<BookDetails>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return Result.success(emptyList())

        val isIsbn = cleanQuery.replace("-", "").matches(Regex("^[0-9]{9}[0-9Xx]$|^[0-9]{13}$"))
        val paramName = if (isIsbn) "isbn" else "q"
        val encodedVal = URLEncoder.encode(cleanQuery.replace("-", ""), "UTF-8")
        val clampedLimit = limit.coerceIn(1, 40)
        val endpoint = "$BASE_SEARCH_URL?$paramName=$encodedVal&limit=$clampedLimit"

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
                Log.w(TAG, "OpenLibrary search failed HTTP $code: ${bodyString.take(150)}")
                return Result.failure(Exception("OpenLibrary HTTP $code: ${bodyString.take(150)}"))
            }

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
            val docsArray = rootObj?.getAsJsonArray("docs")
            if (docsArray == null || docsArray.size() == 0) {
                return Result.success(emptyList())
            }

            val results = mutableListOf<BookDetails>()
            for (i in 0 until docsArray.size()) {
                val docElem = docsArray.get(i)
                if (docElem.isJsonObject) {
                    try {
                        parseSearchDoc(docElem.asJsonObject)?.let { results.add(it) }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing OpenLibrary doc at $i", e)
                    }
                }
            }
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying OpenLibrary for '$cleanQuery'", e)
            Result.failure(e)
        }
    }

    fun searchByIsbn(isbn: String): Result<BookDetails?> {
        val clean = isbn.replace(Regex("[^0-9Xx]"), "")
        if (clean.isBlank()) return Result.success(null)

        val endpoint = "$BASE_ISBN_URL/$clean.json"
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

            if (code == 404) {
                // Try fallback to search.json?isbn=...
                return searchBooks(clean, limit = 1).map { it.firstOrNull() }
            }

            if (!response.isSuccessful) {
                return Result.failure(Exception("OpenLibrary ISBN HTTP $code: ${bodyString.take(150)}"))
            }

            val obj = gson.fromJson(bodyString, JsonObject::class.java) ?: return Result.success(null)
            val title = obj.get("title")?.takeIf { !it.isJsonNull }?.asString?.trim() ?: "Unknown"
            val pageCount = obj.get("number_of_pages")?.takeIf { !it.isJsonNull }?.asInt
            val publishDate = obj.get("publish_date")?.takeIf { !it.isJsonNull }?.asString

            val isbn10 = obj.getAsJsonArray("isbn_10")?.firstOrNull()?.asString
            val isbn13 = obj.getAsJsonArray("isbn_13")?.firstOrNull()?.asString ?: clean

            val coverId = obj.getAsJsonArray("covers")?.firstOrNull()?.takeIf { !it.isJsonNull }?.asLong
            val coverUrl = coverId?.let { "$COVERS_CDN_URL/id/$it-L.jpg" } ?: "$COVERS_CDN_URL/isbn/$clean-L.jpg"

            val details = BookDetails(
                id = clean,
                title = title,
                subtitle = null,
                authors = emptyList(),
                publishedDate = publishDate,
                pageCount = pageCount,
                isbn10 = isbn10,
                isbn13 = isbn13,
                coverUrl = coverUrl,
                sourceProvider = "Open Library"
            )
            Result.success(details)
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying OpenLibrary ISBN '$clean'", e)
            Result.failure(e)
        }
    }

    private fun parseSearchDoc(docObj: JsonObject): BookDetails? {
        val key = docObj.get("key")?.takeIf { !it.isJsonNull }?.asString ?: return null
        val id = key.removePrefix("/works/").removePrefix("/")

        val title = docObj.get("title")?.takeIf { !it.isJsonNull }?.asString?.trim() ?: "Unknown"
        val subtitle = docObj.get("subtitle")?.takeIf { !it.isJsonNull }?.asString?.trim()

        val authors = mutableListOf<String>()
        docObj.getAsJsonArray("author_name")?.forEach { authElem ->
            if (!authElem.isJsonNull) {
                authors.add(authElem.asString.trim())
            }
        }

        val publisher = docObj.getAsJsonArray("publisher")?.firstOrNull()?.takeIf { !it.isJsonNull }?.asString?.trim()
        val firstPublishYear = docObj.get("first_publish_year")?.takeIf { !it.isJsonNull }?.asInt?.toString()
        val pageCount = docObj.get("number_of_pages_median")?.takeIf { !it.isJsonNull }?.asInt

        var isbn10: String? = null
        var isbn13: String? = null
        docObj.getAsJsonArray("isbn")?.forEach { elem ->
            val str = elem.asString.trim()
            if (str.length == 10 && isbn10 == null) isbn10 = str
            if (str.length == 13 && isbn13 == null) isbn13 = str
        }

        val coverId = docObj.get("cover_i")?.takeIf { !it.isJsonNull }?.asLong
        val coverUrl = when {
            coverId != null && coverId > 0 -> "$COVERS_CDN_URL/id/$coverId-L.jpg"
            !isbn13.isNullOrBlank() -> "$COVERS_CDN_URL/isbn/$isbn13-L.jpg"
            !isbn10.isNullOrBlank() -> "$COVERS_CDN_URL/isbn/$isbn10-L.jpg"
            else -> null
        }

        val categories = mutableListOf<String>()
        docObj.getAsJsonArray("subject")?.take(5)?.forEach { subElem ->
            if (!subElem.isJsonNull) {
                categories.add(subElem.asString.trim())
            }
        }

        val ratingAvg = docObj.get("ratings_average")?.takeIf { !it.isJsonNull }?.asFloat
        val scaledScore = ratingAvg?.let { it * 2f }

        return BookDetails(
            id = id,
            title = title,
            subtitle = subtitle,
            authors = authors,
            publisher = publisher,
            publishedDate = firstPublishYear,
            pageCount = pageCount,
            isbn10 = isbn10,
            isbn13 = isbn13,
            coverUrl = coverUrl,
            categories = categories,
            averageScore = scaledScore,
            sourceProvider = "Open Library",
            infoUrl = "https://openlibrary.org$key"
        )
    }
}

