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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

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

    fun isUrlBroken(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val clean = url.trim()
        if (knownFailedUrls.contains(clean)) return true
        if (isMalformedUrl(clean)) return true
        return false
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

