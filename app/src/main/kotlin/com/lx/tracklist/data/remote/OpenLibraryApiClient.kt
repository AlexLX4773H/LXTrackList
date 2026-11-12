package com.lx.tracklist.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.RelatedMediaItem
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale
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

    fun cleanSearchTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\s*-\\s*(Hufflepuff|Gryffindor|Slytherin|Ravenclaw|Special|Anniversary|Deluxe|Collector'?s)\\s+Edition.*"), "")
            .replace(Regex("\\s*[\\[\\(].*?[\\]\\)]"), "")
            .trim()
    }

    fun extractFranchisePrefix(title: String): String? {
        val clean = cleanSearchTitle(title)
        val andTheIdx = clean.indexOf(" and the ", ignoreCase = true)
        if (andTheIdx > 0) {
            val prefix = clean.substring(0, andTheIdx).trim()
            if (prefix.isNotBlank() && prefix.length >= 3) return prefix
        }
        if (clean.contains(":")) {
            val prefix = clean.substringBefore(":").trim()
            if (prefix.isNotBlank() && prefix.length >= 3) return prefix
        }
        if (clean.contains(" - ")) {
            val prefix = clean.substringBefore(" - ").trim()
            if (prefix.isNotBlank() && prefix.length >= 3) return prefix
        }
        return null
    }

    fun fetchWorkDetails(workKey: String): Result<BookDetails?> {
        val cleanKey = workKey.removePrefix("/works/").removePrefix("/").trim()
        if (cleanKey.isBlank()) return Result.success(null)

        val endpoint = "https://openlibrary.org/works/$cleanKey.json"
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

            if (code == 404) return Result.success(null)
            if (!response.isSuccessful) {
                return Result.failure(Exception("OpenLibrary work HTTP $code: ${bodyString.take(150)}"))
            }

            val workObj = gson.fromJson(bodyString, JsonObject::class.java) ?: return Result.success(null)
            val title = workObj.get("title")?.takeIf { !it.isJsonNull }?.asString?.trim() ?: "Unknown"

            val descObj = workObj.get("description")
            val description = when {
                descObj == null || descObj.isJsonNull -> null
                descObj.isJsonObject -> descObj.asJsonObject.get("value")?.takeIf { !it.isJsonNull }?.asString
                descObj.isJsonPrimitive -> descObj.asString
                else -> null
            }

            val coverId = workObj.getAsJsonArray("covers")?.firstOrNull()?.takeIf { !it.isJsonNull }?.asLong
            val coverUrl = coverId?.let { "$COVERS_CDN_URL/id/$it-L.jpg" }
            val publishDate = workObj.get("first_publish_date")?.takeIf { !it.isJsonNull }?.asString

            val categories = mutableListOf<String>()
            workObj.getAsJsonArray("subjects")?.take(6)?.forEach { s ->
                if (!s.isJsonNull && s.isJsonPrimitive) {
                    val str = s.asString.trim()
                    if (str.isNotBlank() && !str.startsWith("series:", ignoreCase = true)) {
                        categories.add(str)
                    }
                }
            }

            val authors = mutableListOf<String>()
            workObj.getAsJsonArray("authors")?.forEach { authElem ->
                if (authElem.isJsonObject) {
                    val authObj = authElem.asJsonObject.getAsJsonObject("author")
                    val authKey = authObj?.get("key")?.takeIf { !it.isJsonNull }?.asString
                    if (!authKey.isNullOrBlank()) {
                        fetchAuthorName(authKey)?.let { authors.add(it) }
                    }
                }
            }

            Result.success(
                BookDetails(
                    id = cleanKey,
                    title = title,
                    subtitle = null,
                    authors = authors,
                    description = description,
                    publishedDate = publishDate,
                    coverUrl = coverUrl,
                    categories = categories,
                    sourceProvider = "Open Library",
                    infoUrl = "https://openlibrary.org/works/$cleanKey"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying OpenLibrary work '$cleanKey'", e)
            Result.failure(e)
        }
    }

    private fun fetchAuthorName(authorKey: String): String? {
        val clean = authorKey.removePrefix("/authors/").removePrefix("/").trim()
        val endpoint = "https://openlibrary.org/authors/$clean.json"
        return try {
            val req = Request.Builder().url(endpoint).header("User-Agent", USER_AGENT).get().build()
            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = gson.fromJson(resp.body?.string(), JsonObject::class.java)
                json?.get("name")?.takeIf { !it.isJsonNull }?.asString?.trim()
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun fetchRelatedBooks(
        title: String,
        author: String? = null,
        currentYear: String? = null,
        isbn: String? = null,
        externalId: String? = null
    ): Result<List<RelatedMediaItem>> {
        val cleanTitle = cleanSearchTitle(title)
        if (cleanTitle.isBlank()) return Result.success(emptyList())

        return try {
            var workKey: String? = externalId?.takeIf { it.startsWith("OL", ignoreCase = true) || it.contains("/works/") }
            if (workKey != null && !workKey.startsWith("/works/")) {
                workKey = "/works/$workKey"
            }

            // 1. If we don't have workKey, find it via search
            if (workKey == null) {
                val searchUrl = when {
                    !isbn.isNullOrBlank() && isbn.matches(Regex("^[0-9]{9}[0-9Xx]$|^[0-9]{13}$")) ->
                        "$BASE_SEARCH_URL?isbn=$isbn&fields=key,title,author_name,first_publish_year,cover_i,series&limit=1"
                    !author.isNullOrBlank() ->
                        "$BASE_SEARCH_URL?title=${URLEncoder.encode(cleanTitle, "UTF-8")}&author=${URLEncoder.encode(author, "UTF-8")}&fields=key,title,author_name,first_publish_year,cover_i,series&limit=3"
                    else ->
                        "$BASE_SEARCH_URL?title=${URLEncoder.encode(cleanTitle, "UTF-8")}&fields=key,title,author_name,first_publish_year,cover_i,series&limit=3"
                }

                val req = Request.Builder().url(searchUrl).header("User-Agent", USER_AGENT).header("Accept", "application/json").get().build()
                val resp = okHttpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val root = gson.fromJson(resp.body?.string(), JsonObject::class.java)
                    val doc = root?.getAsJsonArray("docs")?.firstOrNull()?.asJsonObject
                    workKey = doc?.get("key")?.takeIf { !it.isJsonNull }?.asString
                }
            }

            if (workKey == null) {
                return Result.success(emptyList())
            }

            // 2. Fetch work JSON
            val cleanWorkKey = workKey.removePrefix("/works/").removePrefix("/").trim()
            val workReq = Request.Builder().url("https://openlibrary.org/works/$cleanWorkKey.json").header("User-Agent", USER_AGENT).get().build()
            val workResp = okHttpClient.newCall(workReq).execute()
            if (!workResp.isSuccessful) {
                return Result.success(emptyList())
            }
            val workObj = gson.fromJson(workResp.body?.string(), JsonObject::class.java) ?: return Result.success(emptyList())

            val seriesArray = workObj.getAsJsonArray("series")
            var seriesKey: String? = null
            var currentPos: Int? = null

            if (seriesArray != null && seriesArray.size() > 0) {
                val firstElem = seriesArray.get(0)
                if (firstElem.isJsonObject) {
                    val sObj = firstElem.asJsonObject
                    seriesKey = sObj.getAsJsonObject("series")?.get("key")?.takeIf { !it.isJsonNull }?.asString
                    currentPos = sObj.get("position")?.takeIf { !it.isJsonNull }?.asString?.toIntOrNull()
                } else if (firstElem.isJsonPrimitive) {
                    val sStr = firstElem.asString
                    if (sStr.startsWith("/series/") || sStr.startsWith("OL")) {
                        seriesKey = sStr
                    }
                }
            }

            val list = mutableListOf<RelatedMediaItem>()
            val seenWorkKeys = mutableSetOf<String>()
            seenWorkKeys.add(cleanWorkKey.lowercase(Locale.US))
            val cleanCurrentTitle = cleanTitle.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")

            // 3. If seriesKey is found, fetch series seeds
            if (!seriesKey.isNullOrBlank()) {
                val cleanSeriesKey = seriesKey.removePrefix("/series/").removePrefix("/").trim()
                val seedsReq = Request.Builder().url("https://openlibrary.org/series/$cleanSeriesKey/seeds.json").header("User-Agent", USER_AGENT).get().build()
                val seedsResp = okHttpClient.newCall(seedsReq).execute()
                if (seedsResp.isSuccessful) {
                    val seedsObj = gson.fromJson(seedsResp.body?.string(), JsonObject::class.java)
                    val entries = seedsObj?.getAsJsonArray("entries")
                    if (entries != null) {
                        for (i in 0 until entries.size()) {
                            val entryElem = entries.get(i)
                            if (!entryElem.isJsonObject) continue
                            val eObj = entryElem.asJsonObject
                            val eUrl = eObj.get("url")?.takeIf { !it.isJsonNull }?.asString ?: ""
                            val eKey = eUrl.removePrefix("/works/").removePrefix("/").trim()
                            val eTitle = eObj.get("title")?.takeIf { !it.isJsonNull }?.asString?.trim() ?: continue
                            val normETitle = eTitle.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")

                            if (eKey.lowercase(Locale.US) in seenWorkKeys || normETitle == cleanCurrentTitle) continue
                            seenWorkKeys.add(eKey.lowercase(Locale.US))

                            val seedIndex = i + 1
                            val relationship = when {
                                currentPos != null && seedIndex < currentPos -> "Prequel"
                                currentPos != null && seedIndex > currentPos -> "Sequel"
                                else -> "Part of Series"
                            }

                            val picUrlRaw = eObj.getAsJsonObject("picture")?.get("url")?.takeIf { !it.isJsonNull }?.asString
                            val posterUrl = picUrlRaw?.let { raw ->
                                val full = if (raw.startsWith("//")) "https:$raw" else raw
                                full.replace("-S.jpg", "-L.jpg").replace("-M.jpg", "-L.jpg")
                            }

                            list.add(
                                RelatedMediaItem(
                                    title = eTitle,
                                    relationship = relationship,
                                    externalId = eKey,
                                    externalSource = "Open Library",
                                    mediaType = MediaType.BOOK,
                                    posterUrl = posterUrl
                                )
                            )
                        }
                    }
                }
            }

            if (list.isNotEmpty()) {
                return Result.success(list)
            }

            // 4. Fallback: Search by franchise prefix or series name
            val franchisePrefix = extractFranchisePrefix(title)
            val searchParam = franchisePrefix ?: cleanTitle
            val authorParam = if (!author.isNullOrBlank()) "&author=" + URLEncoder.encode(author, "UTF-8") else ""
            val searchEndpoint = "$BASE_SEARCH_URL?q=${URLEncoder.encode(searchParam, "UTF-8")}$authorParam&fields=key,title,author_name,first_publish_year,cover_i&limit=15"

            val fallbackReq = Request.Builder().url(searchEndpoint).header("User-Agent", USER_AGENT).get().build()
            val fallbackResp = okHttpClient.newCall(fallbackReq).execute()
            if (fallbackResp.isSuccessful) {
                val fbRoot = gson.fromJson(fallbackResp.body?.string(), JsonObject::class.java)
                val docs = fbRoot?.getAsJsonArray("docs")
                val curYearInt = currentYear?.take(4)?.toIntOrNull()
                if (docs != null) {
                    for (i in 0 until docs.size()) {
                        val d = docs.get(i).asJsonObject
                        val dKey = d.get("key")?.takeIf { !it.isJsonNull }?.asString?.removePrefix("/works/")?.removePrefix("/")?.trim() ?: continue
                        val dTitle = d.get("title")?.takeIf { !it.isJsonNull }?.asString?.trim() ?: continue
                        val normDTitle = dTitle.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")

                        if (dKey.lowercase(Locale.US) in seenWorkKeys || normDTitle == cleanCurrentTitle) continue
                        seenWorkKeys.add(dKey.lowercase(Locale.US))

                        val pubYear = d.get("first_publish_year")?.takeIf { !it.isJsonNull }?.asInt
                        val relationship = when {
                            curYearInt != null && pubYear != null && pubYear < curYearInt -> "Prequel"
                            curYearInt != null && pubYear != null && pubYear > curYearInt -> "Sequel"
                            else -> "Part of Series"
                        }
                        val coverId = d.get("cover_i")?.takeIf { !it.isJsonNull }?.asLong
                        val posterUrl = coverId?.let { "$COVERS_CDN_URL/id/$it-L.jpg" }

                        list.add(
                            RelatedMediaItem(
                                title = dTitle,
                                relationship = relationship,
                                externalId = dKey,
                                externalSource = "Open Library",
                                mediaType = MediaType.BOOK,
                                posterUrl = posterUrl,
                                releaseYear = pubYear?.toString()
                            )
                        )
                    }
                }
            }

            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in OpenLibrary fetchRelatedBooks for '$title'", e)
            Result.failure(e)
        }
    }
}

