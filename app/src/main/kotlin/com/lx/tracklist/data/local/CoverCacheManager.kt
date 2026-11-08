package com.lx.tracklist.data.local

import android.content.Context
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.lx.tracklist.data.model.MediaEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

data class ReindexResult(
    val total: Int,
    val succeeded: Int,
    val failed: Int
)

data class ClearCacheStats(
    val freedBytes: Long,
    val freedFiles: Int,
    val remainingBytes: Long,
    val remainingFiles: Int
)

object CoverCacheManager {

    const val COVER_CACHE_DIR_NAME = "cover_cache"

    fun sha256Hex(input: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format(Locale.US, "%02x", b))
        }
        return sb.toString()
    }

    fun getCacheDir(context: Context): File {
        val dir = File(context.filesDir, COVER_CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getCacheSizeBytes(context: Context): Long {
        val dir = File(context.filesDir, COVER_CACHE_DIR_NAME)
        if (!dir.exists()) return 0L
        return try {
            dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } catch (_: Exception) {
            0L
        }
    }

    fun getCachedFileCount(context: Context): Int {
        val dir = File(context.filesDir, COVER_CACHE_DIR_NAME)
        if (!dir.exists()) return 0
        return try {
            dir.walkTopDown().filter { it.isFile }.count()
        } catch (_: Exception) {
            0
        }
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 0L -> "0 B"
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun clearCache(context: Context) {
        try {
            val imageLoader = Coil.imageLoader(context)
            imageLoader.diskCache?.clear()
            imageLoader.memoryCache?.clear()
        } catch (_: Exception) {}

        try {
            val dir = File(context.filesDir, COVER_CACHE_DIR_NAME)
            if (dir.exists()) {
                dir.deleteRecursively()
                dir.mkdirs()
            }
        } catch (_: Exception) {}
    }

    fun clearUnlinkedCache(context: Context, activeUrls: Set<String>): ClearCacheStats {
        var freedBytes = 0L
        var freedFiles = 0

        val dir = File(context.filesDir, COVER_CACHE_DIR_NAME)
        if (!dir.exists()) {
            return ClearCacheStats(0L, 0, 0L, 0)
        }

        val imageLoader = Coil.imageLoader(context)
        val activeFileNames = mutableSetOf<String>()

        activeUrls.filter { it.isNotBlank() }.forEach { url ->
            val hash = sha256Hex(url)
            activeFileNames.add("$hash.0")
            activeFileNames.add("$hash.1")
            try {
                val snapshot = imageLoader.diskCache?.openSnapshot(url)
                if (snapshot != null) {
                    activeFileNames.add(snapshot.data.toFile().name)
                    activeFileNames.add(snapshot.metadata.toFile().name)
                    snapshot.close()
                }
            } catch (_: Exception) {}
        }

        try {
            imageLoader.memoryCache?.clear()
        } catch (_: Exception) {}

        try {
            dir.listFiles()?.forEach { file ->
                val name = file.name
                if (name != "journal" && name != "journal.tmp" && name != "journal.bkp") {
                    if (file.isFile && !activeFileNames.contains(name)) {
                        val len = file.length()
                        if (file.delete()) {
                            freedBytes += len
                            freedFiles++
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        val remainingBytes = getCacheSizeBytes(context)
        val remainingFiles = getCachedFileCount(context)

        return ClearCacheStats(
            freedBytes = freedBytes,
            freedFiles = freedFiles,
            remainingBytes = remainingBytes,
            remainingFiles = remainingFiles
        )
    }

    fun filterEligibleUrls(entries: List<MediaEntry>): List<String> {
        return entries
            .mapNotNull { it.coverImageUri?.trim() }
            .filter { uri ->
                uri.isNotBlank() && (
                    uri.startsWith("http://", ignoreCase = true) ||
                    uri.startsWith("https://", ignoreCase = true) ||
                    uri.startsWith("data:image/", ignoreCase = true)
                )
            }
            .distinct()
    }

    suspend fun reindexCovers(
        context: Context,
        entries: List<MediaEntry>,
        onProgress: (current: Int, total: Int, successCount: Int, failedCount: Int) -> Unit
    ): ReindexResult = withContext(Dispatchers.IO) {
        val targetUrls = filterEligibleUrls(entries)
        val total = targetUrls.size

        if (total == 0) {
            clearCache(context)
            return@withContext ReindexResult(total = 0, succeeded = 0, failed = 0)
        }

        // Clear existing cache before re-indexing
        clearCache(context)

        val imageLoader = Coil.imageLoader(context)
        val successCounter = AtomicInteger(0)
        val failedCounter = AtomicInteger(0)
        val processedCounter = AtomicInteger(0)

        // Process with concurrency limit (4) to prevent excessive memory/network strain
        val semaphore = Semaphore(4)

        val jobs = targetUrls.map { url ->
            async {
                semaphore.withPermit {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(url)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .networkCachePolicy(CachePolicy.ENABLED)
                            .build()

                        val result = imageLoader.execute(request)
                        if (result is SuccessResult) {
                            successCounter.incrementAndGet()
                            FailedCoverTracker.markSuccess(0L, url)
                        } else {
                            failedCounter.incrementAndGet()
                            FailedCoverTracker.markFailed(0L, url)
                        }
                    } catch (_: Exception) {
                        failedCounter.incrementAndGet()
                        FailedCoverTracker.markFailed(0L, url)
                    } finally {
                        val current = processedCounter.incrementAndGet()
                        onProgress(
                            current,
                            total,
                            successCounter.get(),
                            failedCounter.get()
                        )
                    }
                }
            }
        }

        jobs.awaitAll()

        ReindexResult(
            total = total,
            succeeded = successCounter.get(),
            failed = failedCounter.get()
        )
    }
}

