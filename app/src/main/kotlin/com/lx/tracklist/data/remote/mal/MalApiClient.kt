package com.lx.tracklist.data.remote.mal

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

object MalApiClient {

    private const val TAG = "MalApiClient"
    private const val BASE_URL = "https://api.myanimelist.net/v2"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Executes an authenticated request with automatic 401 token refresh and retry.
     */
    private suspend fun executeWithAuth(
        context: Context,
        userId: Long,
        requestBuilder: (token: String) -> Request
    ): Result<Response> = withContext(Dispatchers.IO) {
        val initialToken = MalAuthManager.getValidAccessToken(context, userId)
            ?: return@withContext Result.failure(IllegalStateException("User $userId is not connected to MAL"))

        var request = requestBuilder(initialToken)
        try {
            var response = httpClient.newCall(request).execute()

            // If unauthorized (401), try refreshing token once and retry
            if (response.code == 401) {
                response.close()
                Log.d(TAG, "401 received from MAL. Attempting proactive token refresh...")
                val refreshResult = MalAuthManager.refreshAccessToken(context, userId)
                if (refreshResult.isSuccess) {
                    val newToken = refreshResult.getOrThrow()
                    request = requestBuilder(newToken)
                    response = httpClient.newCall(request).execute()
                } else {
                    return@withContext Result.failure(
                        Exception("MAL session expired. Please re-authenticate in Settings.")
                    )
                }
            }

            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Network exception during MAL request", e)
            Result.failure(e)
        }
    }

    /**
     * Update Anime status, episodes watched, score, and rewatch count.
     */
    suspend fun updateAnimeStatus(
        context: Context,
        userId: Long,
        malAnimeId: Long,
        status: String?,
        numWatchedEpisodes: Int?,
        score: Int?,
        isRewatching: Boolean? = null,
        numTimesRewatched: Int? = null
    ): Result<MalAnimeListStatus> = withContext(Dispatchers.IO) {
        val formBuilder = FormBody.Builder()
        status?.let { formBuilder.add("status", it) }
        numWatchedEpisodes?.let { formBuilder.add("num_watched_episodes", it.toString()) }
        score?.let { formBuilder.add("score", it.toString()) }
        isRewatching?.let { formBuilder.add("is_rewatching", it.toString()) }
        numTimesRewatched?.let { formBuilder.add("num_times_rewatched", it.toString()) }

        val formBody = formBuilder.build()

        val result = executeWithAuth(context, userId) { token ->
            Request.Builder()
                .url("$BASE_URL/anime/$malAnimeId/my_list_status")
                .header("Authorization", "Bearer $token")
                .patch(formBody)
                .build()
        }

        result.mapCatching { response ->
            val body = response.body?.string()
            if (!response.isSuccessful || body.isNullOrBlank()) {
                throw IOException("MAL anime status update failed: HTTP ${response.code} ($body)")
            }
            gson.fromJson(body, MalAnimeListStatus::class.java)
        }
    }

    /**
     * Update Manga status, chapters/volumes read, score, and reread count.
     */
    suspend fun updateMangaStatus(
        context: Context,
        userId: Long,
        malMangaId: Long,
        status: String?,
        numVolumesRead: Int?,
        numChaptersRead: Int?,
        score: Int?,
        isRereading: Boolean? = null,
        numTimesReread: Int? = null
    ): Result<MalMangaListStatus> = withContext(Dispatchers.IO) {
        val formBuilder = FormBody.Builder()
        status?.let { formBuilder.add("status", it) }
        numVolumesRead?.let { formBuilder.add("num_volumes_read", it.toString()) }
        numChaptersRead?.let { formBuilder.add("num_chapters_read", it.toString()) }
        score?.let { formBuilder.add("score", it.toString()) }
        isRereading?.let { formBuilder.add("is_rereading", it.toString()) }
        numTimesReread?.let { formBuilder.add("num_times_reread", it.toString()) }

        val formBody = formBuilder.build()

        val result = executeWithAuth(context, userId) { token ->
            Request.Builder()
                .url("$BASE_URL/manga/$malMangaId/my_list_status")
                .header("Authorization", "Bearer $token")
                .patch(formBody)
                .build()
        }

        result.mapCatching { response ->
            val body = response.body?.string()
            if (!response.isSuccessful || body.isNullOrBlank()) {
                throw IOException("MAL manga status update failed: HTTP ${response.code} ($body)")
            }
            gson.fromJson(body, MalMangaListStatus::class.java)
        }
    }

    /**
     * Delete an anime entry from the user's MAL list.
     */
    suspend fun deleteAnimeStatus(
        context: Context,
        userId: Long,
        malAnimeId: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val result = executeWithAuth(context, userId) { token ->
            Request.Builder()
                .url("$BASE_URL/anime/$malAnimeId/my_list_status")
                .header("Authorization", "Bearer $token")
                .delete()
                .build()
        }

        result.mapCatching { response ->
            if (!response.isSuccessful && response.code != 404) {
                throw IOException("Failed to delete anime from MAL: HTTP ${response.code}")
            }
            Unit
        }
    }

    /**
     * Delete a manga entry from the user's MAL list.
     */
    suspend fun deleteMangaStatus(
        context: Context,
        userId: Long,
        malMangaId: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val result = executeWithAuth(context, userId) { token ->
            Request.Builder()
                .url("$BASE_URL/manga/$malMangaId/my_list_status")
                .header("Authorization", "Bearer $token")
                .delete()
                .build()
        }

        result.mapCatching { response ->
            if (!response.isSuccessful && response.code != 404) {
                throw IOException("Failed to delete manga from MAL: HTTP ${response.code}")
            }
            Unit
        }
    }

    /**
     * Fetch user's anime list with paging.
     */
    suspend fun getUserAnimeList(
        context: Context,
        userId: Long,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 100
    ): Result<MalAnimeListResponse> = withContext(Dispatchers.IO) {
        val fields = "list_status{num_episodes_watched,score,status,is_rewatching,num_times_rewatched,updated_at}," +
                     "num_episodes,start_date,end_date,alternative_titles,genres,synopsis,mean"

        var url = "$BASE_URL/users/@me/animelist?fields=$fields&limit=$limit&offset=$offset"
        if (!status.isNullOrBlank()) {
            url += "&status=$status"
        }

        val result = executeWithAuth(context, userId) { token ->
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
        }

        result.mapCatching { response ->
            val body = response.body?.string()
            if (!response.isSuccessful || body.isNullOrBlank()) {
                throw IOException("Failed to fetch MAL anime list: HTTP ${response.code}")
            }
            gson.fromJson(body, MalAnimeListResponse::class.java)
        }
    }

    /**
     * Fetch user's manga list with paging.
     */
    suspend fun getUserMangaList(
        context: Context,
        userId: Long,
        status: String? = null,
        offset: Int = 0,
        limit: Int = 100
    ): Result<MalMangaListResponse> = withContext(Dispatchers.IO) {
        val fields = "list_status{num_volumes_read,num_chapters_read,score,status,is_rereading,num_times_reread,updated_at}," +
                     "num_volumes,num_chapters,start_date,end_date,alternative_titles,genres,synopsis,mean"

        var url = "$BASE_URL/users/@me/mangalist?fields=$fields&limit=$limit&offset=$offset"
        if (!status.isNullOrBlank()) {
            url += "&status=$status"
        }

        val result = executeWithAuth(context, userId) { token ->
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
        }

        result.mapCatching { response ->
            val body = response.body?.string()
            if (!response.isSuccessful || body.isNullOrBlank()) {
                throw IOException("Failed to fetch MAL manga list: HTTP ${response.code}")
            }
            gson.fromJson(body, MalMangaListResponse::class.java)
        }
    }
}

