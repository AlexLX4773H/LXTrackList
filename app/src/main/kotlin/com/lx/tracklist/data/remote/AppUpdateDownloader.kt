package com.lx.tracklist.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

object AppUpdateDownloader {

    private const val TAG = "AppUpdateDownloader"

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun requestPackageInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch unknown app sources settings", e)
            }
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        version: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "LXTrackList-Android/$version")
                .header("Accept", "application/octet-stream, application/vnd.android.package-archive, */*")
                .get()
                .build()

            val response = downloadClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code} downloading update: ${response.message}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty download response body"))
            val contentLength = body.contentLength()

            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            // Clean up old update files
            try {
                updatesDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".apk") || file.name.endsWith(".tmp")) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed cleaning old update files: ${e.message}")
            }

            val cleanVer = AppUpdateChecker.cleanVersion(version)
            val targetFile = File(updatesDir, "LXTrackList-$cleanVer.apk")
            val tmpFile = File(updatesDir, "LXTrackList-$cleanVer.tmp")

            if (tmpFile.exists()) tmpFile.delete()
            if (targetFile.exists()) targetFile.delete()

            body.byteStream().use { inputStream ->
                FileOutputStream(tmpFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastReportedTime = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (!coroutineContext.isActive) {
                            tmpFile.delete()
                            throw CancellationException("Download cancelled by user")
                        }
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastReportedTime >= 80L || totalRead == contentLength) {
                            lastReportedTime = now
                            onProgress(totalRead, contentLength)
                        }
                    }
                    outputStream.flush()
                }
            }

            if (!tmpFile.renameTo(targetFile)) {
                tmpFile.copyTo(targetFile, overwrite = true)
                tmpFile.delete()
            }

            Log.d(TAG, "Downloaded update successfully to ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            Result.success(targetFile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading update APK: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun installApk(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists() || apkFile.length() <= 0L) {
                return Result.failure(IllegalStateException("Update APK file not found or empty"))
            }

            if (!canRequestPackageInstalls(context)) {
                requestPackageInstallPermission(context)
                return Result.failure(SecurityException("Please allow 'Install unknown apps' permission in Android Settings, then tap Install Now."))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start package installer", e)
            Result.failure(e)
        }
    }

    fun openUpdateInBrowser(context: Context, downloadUrl: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open update in browser", e)
            false
        }
    }
}
