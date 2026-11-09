package com.lx.tracklist.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lx.tracklist.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val releaseName: String?,
    val releaseUrl: String,
    val releaseNotes: String? = null,
    val publishedAt: String? = null,
    val isUpdateAvailable: Boolean,
    val apkDownloadUrl: String? = null
)

sealed interface AppUpdateStatus {
    object Idle : AppUpdateStatus
    object Checking : AppUpdateStatus
    data class UpdateAvailable(val updateInfo: AppUpdateInfo) : AppUpdateStatus
    data class UpToDate(val updateInfo: AppUpdateInfo) : AppUpdateStatus
    data class Error(val message: String, val fallbackUrl: String) : AppUpdateStatus
}

object AppUpdateChecker {

    private const val TAG = "AppUpdateChecker"
    const val GITHUB_OWNER = "AlexLX4773H"
    const val GITHUB_REPO = "LXTrackList"

    const val RELEASES_PAGE_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
    const val LATEST_RELEASE_PAGE_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private const val API_LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private val gson = Gson()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun checkForUpdate(
        currentVersion: String = BuildConfig.VERSION_NAME
    ): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(API_LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "LXTrackList-Android/$currentVersion")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (code == 404) {
                // No releases found yet
                return@withContext Result.success(
                    AppUpdateInfo(
                        latestVersion = currentVersion,
                        currentVersion = currentVersion,
                        releaseName = "No releases found",
                        releaseUrl = RELEASES_PAGE_URL,
                        isUpdateAvailable = false
                    )
                )
            }

            if (!response.isSuccessful) {
                val errorMsg = if (code == 403) {
                    "GitHub API rate limit exceeded. Please try again later."
                } else {
                    "GitHub error HTTP $code"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val rootObj = gson.fromJson(bodyString, JsonObject::class.java)
                ?: return@withContext Result.failure(Exception("Invalid response from GitHub"))

            val rawTagName = rootObj.get("tag_name")?.asString?.trim() ?: ""
            val releaseName = rootObj.get("name")?.asString?.trim()
            val htmlUrl = rootObj.get("html_url")?.asString?.trim()?.ifBlank { null } ?: LATEST_RELEASE_PAGE_URL
            val body = rootObj.get("body")?.asString
            val publishedAt = rootObj.get("published_at")?.asString

            var apkUrl: String? = null
            val assets = rootObj.getAsJsonArray("assets")
            if (assets != null) {
                for (elem in assets) {
                    val assetObj = elem.asJsonObject
                    val name = assetObj.get("name")?.asString ?: ""
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = assetObj.get("browser_download_url")?.asString
                        break
                    }
                }
            }

            val latestVersion = cleanVersion(rawTagName)
            val isAvailable = isVersionNewer(latest = latestVersion, current = currentVersion)

            val updateInfo = AppUpdateInfo(
                latestVersion = if (rawTagName.isNotBlank()) rawTagName else "v$latestVersion",
                currentVersion = "v${cleanVersion(currentVersion)}",
                releaseName = releaseName,
                releaseUrl = htmlUrl,
                releaseNotes = body,
                publishedAt = publishedAt,
                isUpdateAvailable = isAvailable,
                apkDownloadUrl = apkUrl
            )

            Result.success(updateInfo)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check for updates: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun cleanVersion(v: String): String {
        return v.trim().removePrefix("v").removePrefix("V").trim()
    }

    fun isVersionNewer(latest: String, current: String): Boolean {
        val cleanL = cleanVersion(latest)
        val cleanC = cleanVersion(current)

        if (cleanL.isBlank()) return false
        if (cleanC.isBlank()) return true

        val lParts = cleanL.substringBefore("-").substringBefore("+").split(".")
        val cParts = cleanC.substringBefore("-").substringBefore("+").split(".")

        val maxLen = maxOf(lParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val lNum = lParts.getOrNull(i)?.toIntOrNull() ?: 0
            val cNum = cParts.getOrNull(i)?.toIntOrNull() ?: 0
            if (lNum > cNum) return true
            if (lNum < cNum) return false
        }
        return false
    }
}

