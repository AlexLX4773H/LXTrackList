package com.lx.tracklist.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object SteamGridDbApiClient {
    private const val TAG = "SteamGridDbApiClient"
    private const val BASE_URL = "https://www.steamgriddb.com/api/v2"

    private val gson = Gson()
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    data class SgdbSearchResult(
        val id: Long,
        val name: String,
        val releaseDate: Long? = null
    )

    /**
     * Searches for a game by title to get its SteamGridDB internal ID.
     */
    suspend fun searchByTitle(context: Context, title: String): Result<List<SgdbSearchResult>> {
        val apiKey = GameApiConfig.getSteamGridDbApiKey(context)
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("SteamGridDB API key is not configured."))
        }

        val encodedTitle = URLEncoder.encode(title.trim(), "UTF-8")
        val url = "$BASE_URL/search/autocomplete/$encodedTitle"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return Result.failure(Exception("SteamGridDB search error (HTTP ${response.code}): $body"))
            }
            Result.success(parseSearchResultsJson(body))
        } catch (e: Exception) {
            Log.e(TAG, "Error searching SteamGridDB by title", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches vertical portrait grids (600x900) for a given SteamGridDB game ID.
     */
    suspend fun getPortraitGrids(context: Context, sgdbGameId: Long): Result<List<SteamGridPosterItem>> {
        val apiKey = GameApiConfig.getSteamGridDbApiKey(context)
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("SteamGridDB API key is not configured."))
        }

        val url = "$BASE_URL/grids/game/$sgdbGameId?dimensions=600x900&mimes=image/png,image/jpeg&types=static&nsfw=false&humor=false"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        return try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return Result.failure(Exception("SteamGridDB grids error (HTTP ${response.code}): $body"))
            }
            Result.success(parseGridsJson(body))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching SteamGridDB grids", e)
            Result.failure(e)
        }
    }

    // =========================================================================
    // JSON Parsers (Unit-Testable)
    // =========================================================================

    fun parseSearchResultsJson(jsonStr: String): List<SgdbSearchResult> {
        val json = try {
            gson.fromJson(jsonStr, JsonObject::class.java) ?: return emptyList()
        } catch (_: Exception) {
            return emptyList()
        }
        val dataArray = json.getAsJsonArray("data") ?: return emptyList()
        val list = mutableListOf<SgdbSearchResult>()

        for (elem in dataArray) {
            if (!elem.isJsonObject) continue
            val obj = elem.asJsonObject
            val id = obj.get("id")?.asLong ?: continue
            val name = obj.get("name")?.asString ?: continue
            val releaseDate = obj.get("release_date")?.asLong

            list.add(SgdbSearchResult(id, name, releaseDate))
        }
        return list
    }

    fun parseGridsJson(jsonStr: String): List<SteamGridPosterItem> {
        val json = try {
            gson.fromJson(jsonStr, JsonObject::class.java) ?: return emptyList()
        } catch (_: Exception) {
            return emptyList()
        }
        val dataArray = json.getAsJsonArray("data") ?: return emptyList()
        val list = mutableListOf<SteamGridPosterItem>()

        for (elem in dataArray) {
            if (!elem.isJsonObject) continue
            val obj = elem.asJsonObject
            val id = obj.get("id")?.asLong ?: continue
            val url = obj.get("url")?.asString ?: continue
            val thumb = obj.get("thumb")?.asString ?: url
            val width = obj.get("width")?.asInt ?: 600
            val height = obj.get("height")?.asInt ?: 900
            val score = obj.get("score")?.asInt
            val style = obj.get("style")?.asString
            val author = obj.getAsJsonObject("author")?.get("name")?.asString

            list.add(
                SteamGridPosterItem(
                    id = id,
                    url = url,
                    thumbUrl = thumb,
                    width = width,
                    height = height,
                    score = score,
                    style = style,
                    author = author
                )
            )
        }
        return list
    }
}
