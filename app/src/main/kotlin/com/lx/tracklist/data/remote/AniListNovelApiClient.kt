package com.lx.tracklist.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object AniListNovelApiClient {

    private const val TAG = "AniListNovelApiClient"
    private const val GRAPHQL_ENDPOINT = "https://graphql.anilist.co"
    private const val USER_AGENT = "LXTrackList/1.0 (Android; Novels Tracker)"
    private val gson = Gson()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun searchLightNovels(
        query: String,
        limit: Int = 20
    ): Result<List<JikanMediaDetails>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return Result.success(emptyList())

        val graphQlQuery = """
            query (${'$'}search: String, ${'$'}perPage: Int) {
              Page(page: 1, perPage: ${'$'}perPage) {
                media(search: ${'$'}search, type: MANGA, format: NOVEL) {
                  id
                  idMal
                  siteUrl
                  title {
                    romaji
                    english
                    native
                  }
                  synonyms
                  description(asHtml: false)
                  status
                  volumes
                  chapters
                  coverImage {
                    extraLarge
                    large
                  }
                  bannerImage
                  genres
                  tags {
                    name
                  }
                  staff {
                    edges {
                      role
                      node {
                        name {
                          full
                        }
                      }
                    }
                  }
                  averageScore
                  startDate {
                    year
                    month
                    day
                  }
                  endDate {
                    year
                    month
                    day
                  }
                }
              }
            }
        """.trimIndent()

        val jsonBody = JsonObject().apply {
            addProperty("query", graphQlQuery)
            val variables = JsonObject().apply {
                addProperty("search", cleanQuery)
                addProperty("perPage", limit.coerceIn(1, 50))
            }
            add("variables", variables)
        }

        val request = Request.Builder()
            .url(GRAPHQL_ENDPOINT)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.w(TAG, "AniList Light Novel search HTTP $code: ${bodyString.take(150)}")
                return Result.failure(Exception("AniList HTTP $code: ${bodyString.take(150)}"))
            }

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
            val mediaArray = rootObj?.getAsJsonObject("data")
                ?.getAsJsonObject("Page")
                ?.getAsJsonArray("media")
                ?: return Result.success(emptyList())

            val list = mutableListOf<JikanMediaDetails>()
            for (i in 0 until mediaArray.size()) {
                val elem = mediaArray.get(i)
                if (elem.isJsonObject) {
                    try {
                        list.add(parseAniListNovelMedia(elem.asJsonObject))
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing AniList novel at $i", e)
                    }
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying AniList novels for '$cleanQuery'", e)
            Result.failure(e)
        }
    }

    private fun parseAniListNovelMedia(mediaObj: JsonObject): JikanMediaDetails {
        val idMal = mediaObj.get("idMal")?.takeIf { !it.isJsonNull }?.asLong
        val aniId = mediaObj.get("id")?.takeIf { !it.isJsonNull }?.asLong
        val resolvedId = idMal ?: aniId

        val titleObj = mediaObj.getAsJsonObject("title")
        val engTitle = titleObj?.get("english")?.takeIf { !it.isJsonNull }?.asString?.trim()
        val romajiTitle = titleObj?.get("romaji")?.takeIf { !it.isJsonNull }?.asString?.trim()
        val nativeTitle = titleObj?.get("native")?.takeIf { !it.isJsonNull }?.asString?.trim()
        val primaryTitle = engTitle ?: romajiTitle ?: nativeTitle ?: "Unknown"

        val coverObj = mediaObj.getAsJsonObject("coverImage")
        val posterUrl = coverObj?.get("extraLarge")?.takeIf { !it.isJsonNull }?.asString
            ?: coverObj?.get("large")?.takeIf { !it.isJsonNull }?.asString

        val totalChapters = mediaObj.get("chapters")?.takeIf { !it.isJsonNull }?.asInt
        val totalVolumes = mediaObj.get("volumes")?.takeIf { !it.isJsonNull }?.asInt

        val statusStr = mediaObj.get("status")?.takeIf { !it.isJsonNull }?.asString
        val isAiring = statusStr?.equals("RELEASING", ignoreCase = true)

        val description = mediaObj.get("description")?.takeIf { !it.isJsonNull }?.asString

        val avgScore = mediaObj.get("averageScore")?.takeIf { !it.isJsonNull }?.asFloat?.div(10f)

        // Parse Authors / Staff
        val authors = mutableListOf<String>()
        val staffObj = mediaObj.getAsJsonObject("staff")
        staffObj?.getAsJsonArray("edges")?.forEach { edgeElem ->
            if (edgeElem.isJsonObject) {
                val edge = edgeElem.asJsonObject
                val role = edge.get("role")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
                val name = edge.getAsJsonObject("node")?.get("name")?.asJsonObject?.get("full")?.asString
                if (!name.isNullOrBlank()) {
                    if (role.contains("Story", ignoreCase = true) || role.contains("Original", ignoreCase = true) || role.contains("Author", ignoreCase = true)) {
                        authors.add("$name (Story)")
                    } else if (role.contains("Art", ignoreCase = true) || role.contains("Illustration", ignoreCase = true)) {
                        authors.add("$name (Art)")
                    }
                }
            }
        }

        // Tags / Genres
        val tagList = mutableListOf<String>()
        if (authors.isNotEmpty()) {
            tagList.add("Staff: ${authors.joinToString(", ")}")
        }
        mediaObj.getAsJsonArray("genres")?.forEach { g ->
            if (!g.isJsonNull) tagList.add(g.asString)
        }

        val altList = mutableListOf<String>()
        listOfNotNull(engTitle, nativeTitle, romajiTitle).forEach { t ->
            if (t.isNotBlank() && !t.equals(primaryTitle, ignoreCase = true) && !altList.contains(t)) {
                altList.add(t)
            }
        }
        mediaObj.getAsJsonArray("synonyms")?.forEach { syn ->
            val s = syn?.takeIf { !it.isJsonNull }?.asString?.trim()
            if (!s.isNullOrBlank() && !altList.contains(s)) altList.add(s)
        }

        val siteUrl = mediaObj.get("siteUrl")?.takeIf { !it.isJsonNull }?.asString

        return JikanMediaDetails(
            malId = resolvedId,
            title = primaryTitle,
            posterUrl = posterUrl,
            durationText = null,
            durationMinutes = null,
            totalEpisodes = null,
            totalChapters = totalChapters,
            totalVolumes = totalVolumes,
            airedChapters = null,
            alternateTitle = altList.firstOrNull(),
            airedStartDate = parseAniListDate(mediaObj.getAsJsonObject("startDate")),
            airedEndDate = parseAniListDate(mediaObj.getAsJsonObject("endDate")),
            tags = tagList.joinToString(", "),
            synopsis = description,
            isAiring = isAiring,
            externalScore = avgScore,
            externalScoreSource = "AniList",
            type = "Light Novel",
            sourceProvider = "AniList",
            url = siteUrl,
            totalPages = null,
            isbn = null,
            authors = authors.joinToString(", ").takeIf { it.isNotBlank() }
        )
    }

    private fun parseAniListDate(dateObj: JsonObject?): Long? {
        if (dateObj == null) return null
        val y = dateObj.get("year")?.takeIf { !it.isJsonNull }?.asInt ?: return null
        val m = dateObj.get("month")?.takeIf { !it.isJsonNull }?.asInt?.minus(1) ?: 0
        val d = dateObj.get("day")?.takeIf { !it.isJsonNull }?.asInt ?: 1
        return try {
            val cal = java.util.Calendar.getInstance()
            cal.clear()
            cal.set(y, m, d)
            cal.timeInMillis
        } catch (_: Exception) {
            null
        }
    }
}

