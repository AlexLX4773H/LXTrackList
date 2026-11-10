package com.lx.tracklist.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object MangaDexApiClient {

    private const val TAG = "MangaDexApiClient"
    private val gson = Gson()
    const val BASE_URL = "https://api.mangadex.org"
    const val COVERS_CDN_URL = "https://uploads.mangadex.org/covers"
    private const val USER_AGENT = "LXTrackList/1.0 (Android; https://github.com/RyanYuuki/AnymeX)"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun buildCoverUrl(mangaId: String, fileName: String, size: String = "512"): String {
        return "$COVERS_CDN_URL/$mangaId/$fileName.$size.jpg"
    }

    fun buildOriginalCoverUrl(mangaId: String, fileName: String): String {
        return "$COVERS_CDN_URL/$mangaId/$fileName"
    }

    fun searchMangaMultiple(
        query: String,
        isManhwa: Boolean = false,
        limit: Int = 20
    ): Result<List<JikanMediaDetails>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return Result.success(emptyList())

        val encodedTitle = URLEncoder.encode(cleanQuery, "UTF-8")
        val limitClamped = limit.coerceIn(1, 50)

        val languageFilter = if (isManhwa) {
            "&originalLanguage[]=ko"
        } else {
            ""
        }

        val endpoint = "$BASE_URL/manga?title=$encodedTitle&limit=$limitClamped" +
                "&includes[]=cover_art&includes[]=author&includes[]=artist" +
                "&contentRating[]=safe&contentRating[]=suggestive&contentRating[]=erotica&contentRating[]=pornographic$languageFilter"

        return try {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .get()
                .build()

            var response = okHttpClient.newCall(request).execute()
            var code = response.code

            // Rate limiting check (MangaDex has 5 req/sec limit)
            if (code == 429) {
                Log.w(TAG, "MangaDex rate limited (429) for '$cleanQuery', waiting 1.2s before retry...")
                try {
                    Thread.sleep(1200)
                } catch (_: InterruptedException) {}
                response = okHttpClient.newCall(request.newBuilder().build()).execute()
                code = response.code
            }

            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.w(TAG, "MangaDex search failed with HTTP $code: ${bodyString.take(150)}")
                return Result.failure(Exception("MangaDex HTTP $code: ${bodyString.take(150)}"))
            }

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
            val dataArray = rootObj?.getAsJsonArray("data")
            if (dataArray == null || dataArray.size() == 0) {
                // If Manhwa language filter returned nothing, retry once without language filter
                if (isManhwa) {
                    return searchMangaMultiple(cleanQuery, isManhwa = false, limit = limit)
                }
                return Result.success(emptyList())
            }

            val results = mutableListOf<JikanMediaDetails>()
            for (i in 0 until dataArray.size()) {
                val item = dataArray.get(i)
                if (item.isJsonObject) {
                    try {
                        val parsed = parseMangaDexItem(item.asJsonObject)
                        if (parsed != null) {
                            results.add(parsed)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing MangaDex item at index $i: ${e.message}")
                    }
                }
            }

            Log.d(TAG, "MangaDex search for '$cleanQuery' returned ${results.size} results (isManhwa=$isManhwa)")
            Result.success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during MangaDex search for '$cleanQuery'", e)
            Result.failure(e)
        }
    }

    fun fetchMangaDetails(mangaId: String): Result<JikanMediaDetails> {
        val cleanId = mangaId.trim()
        if (cleanId.isBlank()) return Result.failure(IllegalArgumentException("MangaDex ID cannot be blank"))

        val endpoint = "$BASE_URL/manga/$cleanId?includes[]=cover_art&includes[]=author&includes[]=artist"

        return try {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (code == 404) {
                return Result.failure(NotFoundException("MangaDex ID '$cleanId' not found"))
            }
            if (!response.isSuccessful) {
                return Result.failure(Exception("MangaDex HTTP $code: ${bodyString.take(150)}"))
            }

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
            val dataObj = rootObj?.getAsJsonObject("data")
                ?: return Result.failure(NotFoundException("Missing data object in MangaDex response"))

            val details = parseMangaDexItem(dataObj)
                ?: return Result.failure(Exception("Failed to parse MangaDex item $cleanId"))

            Result.success(details)
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching MangaDex ID $cleanId", e)
            Result.failure(e)
        }
    }

    fun searchMangaDetailsByTitle(
        title: String,
        isManhwa: Boolean = false
    ): Result<JikanMediaDetails> {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be blank"))

        val searchResult = searchMangaMultiple(cleanTitle, isManhwa = isManhwa, limit = 5)
        if (searchResult.isFailure) return Result.failure(searchResult.exceptionOrNull() ?: Exception("MangaDex search failed"))

        val list = searchResult.getOrDefault(emptyList())
        if (list.isEmpty()) {
            return Result.failure(NotFoundException("No MangaDex results found for '$cleanTitle'"))
        }

        // Find exact or closest title match, otherwise take the first
        val exactMatch = list.firstOrNull {
            it.title.equals(cleanTitle, ignoreCase = true) ||
                    it.alternateTitle?.split("\n")?.any { alt -> alt.equals(cleanTitle, ignoreCase = true) } == true
        }

        return Result.success(exactMatch ?: list.first())
    }

    fun parseMangaDexItem(mangaObj: JsonObject): JikanMediaDetails? {
        val id = mangaObj.get("id")?.takeIf { !it.isJsonNull }?.asString ?: return null
        val attributes = mangaObj.getAsJsonObject("attributes") ?: return null

        // Title resolution: en -> ko -> ja -> ja-ro -> first entry
        val titleObj = attributes.getAsJsonObject("title")
        val title = titleObj?.get("en")?.takeIf { !it.isJsonNull }?.asString?.trim()
            ?: titleObj?.get("ko")?.takeIf { !it.isJsonNull }?.asString?.trim()
            ?: titleObj?.get("ja")?.takeIf { !it.isJsonNull }?.asString?.trim()
            ?: titleObj?.get("ja-ro")?.takeIf { !it.isJsonNull }?.asString?.trim()
            ?: titleObj?.entrySet()?.firstOrNull()?.value?.takeIf { !it.isJsonNull }?.asString?.trim()
            ?: "Unknown Manga"

        // Alternate titles
        val altTitlesList = mutableListOf<String>()
        val altArray = attributes.getAsJsonArray("altTitles")
        altArray?.forEach { elem ->
            if (elem.isJsonObject) {
                elem.asJsonObject.entrySet().forEach { entry ->
                    val altVal = entry.value?.takeIf { !it.isJsonNull }?.asString?.trim()
                    if (!altVal.isNullOrBlank() && !altVal.equals(title, ignoreCase = true) && !altTitlesList.contains(altVal)) {
                        altTitlesList.add(altVal)
                    }
                }
            }
        }
        val alternateTitle = altTitlesList.distinct().joinToString("\n").takeIf { it.isNotBlank() }

        // Description / Synopsis: en -> first available
        val descObj = attributes.getAsJsonObject("description")
        val rawDesc = descObj?.get("en")?.takeIf { !it.isJsonNull }?.asString?.trim()
            ?: descObj?.entrySet()?.firstOrNull()?.value?.takeIf { !it.isJsonNull }?.asString?.trim()
        val synopsis = JikanApiClient.cleanSynopsis(rawDesc)

        // Status
        val statusStr = attributes.get("status")?.takeIf { !it.isJsonNull }?.asString
        val isAiring = statusStr?.equals("ongoing", ignoreCase = true)

        // Chapters & Volumes
        val lastChapterStr = attributes.get("lastChapter")?.takeIf { !it.isJsonNull }?.asString
        val parsedLastChapter = lastChapterStr?.toIntOrNull()
            ?: lastChapterStr?.toDoubleOrNull()?.toInt()
        val totalChapters = if (isAiring == true) null else parsedLastChapter
        val airedChapters = if (isAiring == true) parsedLastChapter else null

        val lastVolumeStr = attributes.get("lastVolume")?.takeIf { !it.isJsonNull }?.asString
        val totalVolumes = lastVolumeStr?.toIntOrNull()
            ?: lastVolumeStr?.toDoubleOrNull()?.toInt()

        // Original language & Media Type inference
        val originalLanguage = attributes.get("originalLanguage")?.takeIf { !it.isJsonNull }?.asString?.lowercase(Locale.ROOT)
        val inferredType = when (originalLanguage) {
            "ko" -> "Manhwa"
            "zh", "zh-hk" -> "Manhua"
            else -> "Manga"
        }

        // Release year / airedStartDate
        val year = attributes.get("year")?.takeIf { !it.isJsonNull }?.asInt
        val airedStartDate = if (year != null && year > 1900) {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(year, Calendar.JANUARY, 1)
            }
            cal.timeInMillis
        } else {
            val createdAtStr = attributes.get("createdAt")?.takeIf { !it.isJsonNull }?.asString
            JikanApiClient.parseIsoOrSimpleDate(createdAtStr)
        }

        // Tags
        val tagList = mutableListOf<String>()
        val tagsArray = attributes.getAsJsonArray("tags")
        tagsArray?.forEach { tagElem ->
            if (tagElem.isJsonObject) {
                val tagAttr = tagElem.asJsonObject.getAsJsonObject("attributes")
                val nameObj = tagAttr?.getAsJsonObject("name")
                val name = nameObj?.get("en")?.takeIf { !it.isJsonNull }?.asString?.trim()
                if (!name.isNullOrBlank() && !tagList.contains(name)) {
                    tagList.add(name)
                }
            }
        }
        val tags = tagList.take(15).joinToString(", ").takeIf { it.isNotBlank() }

        // Relationships (cover_art, author, artist)
        var coverFileName: String? = null
        val relArray = mangaObj.getAsJsonArray("relationships")
        relArray?.forEach { relElem ->
            if (relElem.isJsonObject) {
                val relObj = relElem.asJsonObject
                val relType = relObj.get("type")?.takeIf { !it.isJsonNull }?.asString
                if (relType == "cover_art") {
                    val relAttr = relObj.getAsJsonObject("attributes")
                    val fileName = relAttr?.get("fileName")?.takeIf { !it.isJsonNull }?.asString
                    if (!fileName.isNullOrBlank()) {
                        coverFileName = fileName
                    }
                }
            }
        }

        val posterUrl = if (!coverFileName.isNullOrBlank()) {
            buildCoverUrl(id, coverFileName)
        } else null

        // Web URL
        val webUrl = "https://mangadex.org/title/$id"

        return JikanMediaDetails(
            malId = null,
            title = title,
            posterUrl = posterUrl,
            durationText = null,
            durationMinutes = null,
            totalEpisodes = null,
            totalChapters = totalChapters,
            totalVolumes = totalVolumes,
            airedChapters = airedChapters,
            alternateTitle = alternateTitle,
            airedStartDate = airedStartDate,
            airedEndDate = null,
            tags = tags,
            synopsis = synopsis,
            isAiring = isAiring,
            externalScore = null,
            externalScoreSource = null,
            type = inferredType,
            sourceProvider = "MangaDex",
            url = webUrl
        )
    }
}

