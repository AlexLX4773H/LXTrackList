package com.lx.tracklist.data.local

import android.content.Context
import android.net.Uri
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.remote.TmdbDnsHelper
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

    @Volatile
    private var dohInitialized = false

    @Volatile
    internal var probeHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    internal var contentUriChecker: (Context?, String) -> Boolean = { ctx, uriString ->
        if (ctx == null) {
            true
        } else {
            try {
                ctx.contentResolver.openInputStream(Uri.parse(uriString))?.use { true } ?: false
            } catch (_: Exception) {
                false
            }
        }
    }

    fun initWithDoH(context: Context) {
        synchronized(this) {
            val appCtx = context.applicationContext ?: context
            probeHttpClient = OkHttpClient.Builder()
                .dns(TmdbDnsHelper.createDoHResolver(appCtx))
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
            dohInitialized = true
        }
    }

    internal fun resetForTest() {
        verifiedSuccessUrls.clear()
        knownFailedUrls.clear()
        failedEntryIdsFlow.value = emptySet()
    }

    private enum class CoverUriKind {
        HTTP_HTTPS,
        DATA_IMAGE,
        CONTENT,
        FILE_OR_PATH,
        INVALID
    }

    private fun classifyUri(url: String): CoverUriKind {
        val clean = url.trim()
        return when {
            clean.isBlank() -> CoverUriKind.INVALID
            clean.startsWith("http://", ignoreCase = true) ||
                clean.startsWith("https://", ignoreCase = true) -> CoverUriKind.HTTP_HTTPS
            clean.startsWith("data:image/", ignoreCase = true) -> {
                val payload = clean.substringAfter(',', missingDelimiterValue = "")
                if (payload.isNotBlank()) CoverUriKind.DATA_IMAGE else CoverUriKind.INVALID
            }
            clean.startsWith("content://", ignoreCase = true) -> CoverUriKind.CONTENT
            clean.startsWith("file:", ignoreCase = true) ||
                (!clean.contains("://") && (clean.startsWith("/") || clean.contains(":\\"))) -> CoverUriKind.FILE_OR_PATH
            else -> CoverUriKind.INVALID
        }
    }

    private fun checkLocalFileExists(clean: String): Boolean {
        val path = clean.removePrefix("file://").removePrefix("file:")
        return try { File(path).exists() } catch (_: Exception) { false }
    }

    internal fun isHttpCoverBroken(url: String, client: OkHttpClient = probeHttpClient): Boolean {
        val headReq = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .head()
            .build()
        val headOk = try {
            client.newCall(headReq).execute().use { it.isSuccessful || it.code == 304 }
        } catch (_: Exception) {
            false
        }
        if (headOk) return false

        // Non-2xx HEAD (including 403/405/etc.) or network failure on HEAD: retry with ranged GET.
        val getReq = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .header("Range", "bytes=0-1024")
            .get()
            .build()
        return try {
            val getOk = client.newCall(getReq).execute().use { it.isSuccessful || it.code == 304 }
            !getOk
        } catch (_: Exception) {
            true
        }
    }

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
        if (classifyUri(url) == CoverUriKind.FILE_OR_PATH && !checkLocalFileExists(url)) {
            return true
        }
        return false
    }

    suspend fun checkUrlBroken(url: String, context: Context? = null): Boolean {
        if (!dohInitialized && context != null) {
            try {
                initWithDoH(context)
            } catch (_: Exception) {}
        }
        val clean = url.trim()
        if (clean.isBlank()) return true
        if (knownFailedUrls.contains(clean)) return true

        when (classifyUri(clean)) {
            CoverUriKind.INVALID -> {
                knownFailedUrls.add(clean)
                return true
            }
            CoverUriKind.DATA_IMAGE -> {
                verifiedSuccessUrls.add(clean)
                return false
            }
            CoverUriKind.CONTENT -> {
                val ok = contentUriChecker(context, clean)
                if (ok) verifiedSuccessUrls.add(clean) else knownFailedUrls.add(clean)
                return !ok
            }
            CoverUriKind.FILE_OR_PATH -> {
                val exists = checkLocalFileExists(clean)
                if (exists) verifiedSuccessUrls.add(clean) else knownFailedUrls.add(clean)
                return !exists
            }
            CoverUriKind.HTTP_HTTPS -> {
                if (verifiedSuccessUrls.contains(clean)) return false
                if (isMalformedUrl(clean)) {
                    knownFailedUrls.add(clean)
                    return true
                }
                return withContext(Dispatchers.IO) {
                    val broken = isHttpCoverBroken(clean, probeHttpClient)
                    if (broken) {
                        knownFailedUrls.add(clean)
                    } else {
                        verifiedSuccessUrls.add(clean)
                    }
                    broken
                }
            }
        }
    }

    suspend fun probeBrokenCovers(
        entries: List<MediaEntry>,
        context: Context? = null
    ): Set<Long> = withContext(Dispatchers.IO) {
        if (!dohInitialized && context != null) {
            try {
                initWithDoH(context)
            } catch (_: Exception) {}
        }
        val entriesWithUrl = entries.filter { !it.coverImageUri.isNullOrBlank() }
        val brokenIds = ConcurrentHashMap.newKeySet<Long>()
        val unverifiedHttp = mutableListOf<Pair<Long, String>>()

        for (entry in entriesWithUrl) {
            val url = entry.coverImageUri!!.trim()
            when (classifyUri(url)) {
                CoverUriKind.INVALID -> {
                    markFailed(entry.id, url)
                    brokenIds.add(entry.id)
                }
                CoverUriKind.DATA_IMAGE -> {
                    markSuccess(entry.id, url)
                }
                CoverUriKind.CONTENT -> {
                    if (contentUriChecker(context, url)) {
                        markSuccess(entry.id, url)
                    } else {
                        markFailed(entry.id, url)
                        brokenIds.add(entry.id)
                    }
                }
                CoverUriKind.FILE_OR_PATH -> {
                    if (checkLocalFileExists(url)) {
                        markSuccess(entry.id, url)
                    } else {
                        markFailed(entry.id, url)
                        brokenIds.add(entry.id)
                    }
                }
                CoverUriKind.HTTP_HTTPS -> {
                    if (isMalformedUrl(url) || knownFailedUrls.contains(url)) {
                        markFailed(entry.id, url)
                        brokenIds.add(entry.id)
                    } else if (!verifiedSuccessUrls.contains(url)) {
                        unverifiedHttp.add(entry.id to url)
                    }
                }
            }
        }

        if (unverifiedHttp.isNotEmpty()) {
            val semaphore = Semaphore(10)
            coroutineScope {
                unverifiedHttp.map { (id, url) ->
                    async {
                        semaphore.withPermit {
                            val isBroken = isHttpCoverBroken(url, probeHttpClient)
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

