package com.lx.tracklist.data.local

import android.content.Context
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.lx.tracklist.data.model.MediaEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object FailedCoverTracker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val verifiedSuccessUrls = ConcurrentHashMap.newKeySet<String>()
    private val knownFailedUrls = ConcurrentHashMap.newKeySet<String>()
    private val failedEntryIdsFlow = MutableStateFlow<Set<Long>>(emptySet())

    val failedEntryIds: StateFlow<Set<Long>> = failedEntryIdsFlow.asStateFlow()

    fun markFailed(entryId: Long, url: String?) {
        if (!url.isNullOrBlank()) {
            val clean = url.trim()
            knownFailedUrls.add(clean)
            verifiedSuccessUrls.remove(clean)
        }
        if (entryId > 0) {
            failedEntryIdsFlow.update { it + entryId }
        }
    }

    fun markSuccess(entryId: Long, url: String?) {
        if (!url.isNullOrBlank()) {
            val clean = url.trim()
            verifiedSuccessUrls.add(clean)
            knownFailedUrls.remove(clean)
        }
        if (entryId > 0) {
            failedEntryIdsFlow.update { it - entryId }
        }
    }

    private val probeHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun isUrlBroken(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val clean = url.trim()
        if (knownFailedUrls.contains(clean)) return true
        if (isMalformedUrl(clean)) return true
        return false
    }

    fun isEntryCoverBroken(entry: MediaEntry): Boolean {
        val url = entry.coverImageUri?.trim()
        if (url.isNullOrBlank()) return true
        if (failedEntryIdsFlow.value.contains(entry.id)) return true
        if (isUrlBroken(url)) return true
        if (url.startsWith("file:", ignoreCase = true) || (!url.contains("://") && (url.startsWith("/") || url.contains(":\\")))) {
            val path = url.removePrefix("file://").removePrefix("file:")
            val exists = try { File(path).exists() } catch (_: Exception) { false }
            if (!exists) return true
        }
        return false
    }

    suspend fun checkUrlBroken(url: String): Boolean {
        val clean = url.trim()
        if (clean.isBlank()) return true
        if (knownFailedUrls.contains(clean)) return true
        if (verifiedSuccessUrls.contains(clean)) return false
        if (isMalformedUrl(clean)) {
            knownFailedUrls.add(clean)
            return true
        }
        if (clean.startsWith("file:", ignoreCase = true) || (!clean.contains("://") && (clean.startsWith("/") || clean.contains(":\\")))) {
            val path = clean.removePrefix("file://").removePrefix("file:")
            val exists = try { File(path).exists() } catch (_: Exception) { false }
            if (!exists) {
                knownFailedUrls.add(clean)
                return true
            } else {
                verifiedSuccessUrls.add(clean)
                return false
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                val headReq = Request.Builder()
                    .url(clean)
                    .header("User-Agent", "Mozilla/5.0")
                    .head()
                    .build()
                val response = try {
                    probeHttpClient.newCall(headReq).execute()
                } catch (_: Exception) {
                    val getReq = Request.Builder()
                        .url(clean)
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Range", "bytes=0-1024")
                        .get()
                        .build()
                    probeHttpClient.newCall(getReq).execute()
                }
                val isSuccess = response.use { it.isSuccessful || it.code == 304 }
                if (isSuccess) {
                    verifiedSuccessUrls.add(clean)
                    false
                } else {
                    knownFailedUrls.add(clean)
                    true
                }
            } catch (_: Exception) {
                knownFailedUrls.add(clean)
                true
            }
        }
    }

    suspend fun probeBrokenCovers(entries: List<MediaEntry>): Set<Long> = withContext(Dispatchers.IO) {
        val entriesWithUrl = entries.filter { !it.coverImageUri.isNullOrBlank() }
        val brokenIds = ConcurrentHashMap.newKeySet<Long>()
        val unverified = mutableListOf<Pair<Long, String>>()

        for (entry in entriesWithUrl) {
            val url = entry.coverImageUri!!.trim()
            if (isMalformedUrl(url) || knownFailedUrls.contains(url)) {
                brokenIds.add(entry.id)
                knownFailedUrls.add(url)
            } else if (url.startsWith("file:", ignoreCase = true) || (!url.contains("://") && (url.startsWith("/") || url.contains(":\\")))) {
                val path = url.removePrefix("file://").removePrefix("file:")
                val exists = try { File(path).exists() } catch (_: Exception) { false }
                if (!exists) {
                    brokenIds.add(entry.id)
                    knownFailedUrls.add(url)
                } else {
                    verifiedSuccessUrls.add(url)
                }
            } else if (!verifiedSuccessUrls.contains(url)) {
                unverified.add(entry.id to url)
            }
        }

        if (unverified.isNotEmpty()) {
            val semaphore = Semaphore(10)
            coroutineScope {
                unverified.map { (id, url) ->
                    async {
                        semaphore.withPermit {
                            val isBroken = try {
                                val headReq = Request.Builder()
                                    .url(url)
                                    .header("User-Agent", "Mozilla/5.0")
                                    .head()
                                    .build()
                                val response = try {
                                    probeHttpClient.newCall(headReq).execute()
                                } catch (_: Exception) {
                                    val getReq = Request.Builder()
                                        .url(url)
                                        .header("User-Agent", "Mozilla/5.0")
                                        .header("Range", "bytes=0-1024")
                                        .get()
                                        .build()
                                    probeHttpClient.newCall(getReq).execute()
                                }
                                val ok = response.use { it.isSuccessful || it.code == 304 }
                                !ok
                            } catch (_: Exception) {
                                true
                            }

                            if (isBroken) {
                                markFailed(id, url)
                                brokenIds.add(id)
                            } else {
                                markSuccess(id, url)
                            }
                        }
                    }
                }.awaitAll()
            }
        }

        if (brokenIds.isNotEmpty()) {
            failedEntryIdsFlow.update { it + brokenIds }
        }

        brokenIds.toSet()
    }

    fun isMalformedUrl(url: String): Boolean {
        val clean = url.trim()
        if (!clean.startsWith("http://", ignoreCase = true) &&
            !clean.startsWith("https://", ignoreCase = true) &&
            !clean.startsWith("content://", ignoreCase = true) &&
            !clean.startsWith("file://", ignoreCase = true) &&
            !clean.startsWith("data:image/", ignoreCase = true)
        ) {
            return true
        }
        return try {
            URI(clean)
            false
        } catch (_: Exception) {
            true
        }
    }

    fun validateEntries(context: Context, entries: List<MediaEntry>) {
        val entriesWithUrl = entries.filter { !it.coverImageUri.isNullOrBlank() }
        val brokenImmediately = mutableSetOf<Long>()
        val toProbe = mutableListOf<MediaEntry>()

        for (entry in entriesWithUrl) {
            val url = entry.coverImageUri!!.trim()
            if (isMalformedUrl(url) || knownFailedUrls.contains(url)) {
                brokenImmediately.add(entry.id)
            } else if (!verifiedSuccessUrls.contains(url)) {
                toProbe.add(entry)
            }
        }

        if (brokenImmediately.isNotEmpty()) {
            failedEntryIdsFlow.update { it + brokenImmediately }
        }

        if (toProbe.isEmpty()) return

        scope.launch {
            val imageLoader = Coil.imageLoader(context)
            val semaphore = Semaphore(4)
            for (entry in toProbe) {
                val url = entry.coverImageUri!!.trim()
                if (verifiedSuccessUrls.contains(url)) continue

                semaphore.withPermit {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(url)
                            .size(100, 100)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .networkCachePolicy(CachePolicy.ENABLED)
                            .build()

                        val result = imageLoader.execute(request)
                        if (result is SuccessResult) {
                            markSuccess(entry.id, url)
                        } else {
                            markFailed(entry.id, url)
                        }
                    } catch (_: Exception) {
                        markFailed(entry.id, url)
                    }
                }
            }
        }
    }
}

