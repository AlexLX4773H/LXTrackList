package com.lx.tracklist.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object ITunesCoverApiClient {
    private const val TAG = "ITunesCoverApiClient"
    private val gson = Gson()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun findCoverUrl(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        val query = "$artist $title".trim()
        if (query.isBlank()) return@withContext null

        try {
            val encodedTerm = URLEncoder.encode(query, "UTF-8")
            val url = "${MusicApiConfig.ITUNES_SEARCH_URL}?term=$encodedTerm&media=music&entity=song&limit=3"

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val root = gson.fromJson(body, JsonObject::class.java)
            val results = root.getAsJsonArray("results") ?: return@withContext null

            if (results.size() > 0) {
                val first = results.get(0).asJsonObject
                val rawArtwork = first.get("artworkUrl100")?.asString
                return@withContext rawArtwork?.replace("100x100bb", "600x600bb")
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve iTunes cover for $query", e)
            null
        }
    }
}
