package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.lx.tracklist.LXApplication
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.repository.MediaRepository
import com.lx.tracklist.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class JikanSyncProgress(
    val isRunning: Boolean = false,
    val currentTitle: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val updatedCount: Int = 0,
    val failedCount: Int = 0,
    val lastErrorMessage: String? = null,
    val statusMessage: String? = null,
    val isCompleted: Boolean = false
) {
    val progressFraction: Float
        get() = if (totalCount > 0) (completedCount.toFloat() / totalCount).coerceIn(0f, 1f) else 0f
}

object JikanSyncManager {

    private const val TAG = "JikanSync"
    private const val RATE_LIMIT_DELAY_MS = 600L

    private val _syncState = MutableStateFlow(JikanSyncProgress())
    val syncState: StateFlow<JikanSyncProgress> = _syncState.asStateFlow()

    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun isMalSource(entry: MediaEntry): Boolean {
        val malId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()
        if (malId.isNullOrBlank() || malId.toLongOrNull() == null) return false
        val src = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim()
        return src.isNullOrBlank() || src.equals("MAL", ignoreCase = true) || src.equals("MyAnimeList", ignoreCase = true)
    }

    fun isMangaType(mediaType: MediaType): Boolean {
        return mediaType == MediaType.MANGA || mediaType == MediaType.MANHWA || mediaType == MediaType.BOOK
    }

    private fun needsSync(entry: MediaEntry, forceRefresh: Boolean): Boolean {
        if (forceRefresh) return true

        // Missing cover, or starts with leftover "file:" path
        val missingCover = entry.coverImageUri.isNullOrBlank() || entry.coverImageUri.startsWith("file:")

        val isVideo = entry.mediaType == MediaType.ANIME || entry.mediaType == MediaType.MOVIE || entry.mediaType == MediaType.SHOW
        val missingRuntime = if (isVideo) {
            if (entry.mediaType == MediaType.MOVIE) {
                entry.durationMinutes == null || entry.durationMinutes == 0
            } else {
                entry.episodeLengthMinutes == null || entry.episodeLengthMinutes == 0
            }
        } else {
            false
        }

        val missingChapters = !isVideo && entry.totalChapters == 0

        return missingCover || missingRuntime || missingChapters
    }

    fun startSync(
        context: Context,
        repository: MediaRepository,
        forceRefresh: Boolean = false
    ) {
        if (_syncState.value.isRunning) return

        syncJob = scope.launch {
            NotificationHelper.initNotificationChannels(context)

            // Find eligible entries across all media types (anime, movies, manga, manhwa, etc.)
            val allEntries = repository.getAllEntriesSync()
            val malEntries = allEntries.filter { isMalSource(it) }
            val eligible = malEntries.filter { needsSync(it, forceRefresh) }

            val total = eligible.size
            if (total == 0) {
                val msg = if (malEntries.isEmpty()) {
                    "No entries found with MyAnimeList (MAL) as foreign database source."
                } else {
                    "All ${malEntries.size} MAL entries already have posters & details."
                }
                _syncState.update {
                    it.copy(
                        isRunning = false,
                        totalCount = 0,
                        completedCount = 0,
                        pendingCount = 0,
                        updatedCount = 0,
                        failedCount = 0,
                        currentTitle = "",
                        statusMessage = null,
                        isCompleted = true
                    )
                }
                NotificationHelper.showJikanSyncComplete(
                    context = context,
                    completed = 0,
                    total = 0,
                    updated = 0,
                    failed = 0,
                    lastError = msg,
                    isCancelled = false
                )
                return@launch
            }

            _syncState.update {
                it.copy(
                    isRunning = true,
                    totalCount = total,
                    completedCount = 0,
                    pendingCount = total,
                    updatedCount = 0,
                    failedCount = 0,
                    lastErrorMessage = null,
                    currentTitle = "",
                    statusMessage = "Starting sync for $total MAL entries...",
                    isCompleted = false
                )
            }

            var completed = 0
            var updated = 0
            var failed = 0
            var lastError: String? = null

            for (entry in eligible) {
                if (syncJob?.isCancelled == true) break

                val malId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()?.toLongOrNull() ?: continue
                val isManga = isMangaType(entry.mediaType)

                _syncState.update {
                    it.copy(
                        currentTitle = entry.title,
                        statusMessage = "Fetching ${entry.title} (#$malId)..."
                    )
                }

                NotificationHelper.showJikanSyncProgress(context, completed, total, entry.title)

                // Rate limiting delay
                delay(RATE_LIMIT_DELAY_MS)

                // Attempt fetch (Jikan with automatic AniList fallback)
                var detailsResult = JikanApiClient.fetchMediaDetails(malId = malId, isManga = isManga)
                if (detailsResult.isFailure && detailsResult.exceptionOrNull() is RateLimitException) {
                    delay(3000L)
                    detailsResult = JikanApiClient.fetchMediaDetails(malId = malId, isManga = isManga)
                }

                if (detailsResult.isSuccess) {
                    val details = detailsResult.getOrNull()
                    Log.d(TAG, "Successfully fetched #${malId} (${entry.title}): poster=${details?.posterUrl}, duration=${details?.durationMinutes}, provider=${details?.sourceProvider}")
                    if (details != null) {
                        var hasChanges = false
                        var finalCoverUri: String? = entry.coverImageUri

                        // Set poster URL if missing, leftover file: URI, or force refreshed
                        val needsPoster = entry.coverImageUri.isNullOrBlank() || entry.coverImageUri.startsWith("file:") || forceRefresh
                        if (!details.posterUrl.isNullOrBlank() && needsPoster) {
                            finalCoverUri = details.posterUrl
                            hasChanges = true

                            // Pre-warm Coil cache directly with the official CDN URL
                            try {
                                val imageLoader = Coil.imageLoader(context)
                                val request = ImageRequest.Builder(context)
                                    .data(details.posterUrl)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build()
                                imageLoader.enqueue(request)
                            } catch (_: Exception) {}
                        }

                        // Determine runtime for anime/movies/shows
                        val newEpisodeLength = if (entry.mediaType != MediaType.MOVIE && !isManga) {
                            if (details.durationMinutes != null && details.durationMinutes != entry.episodeLengthMinutes) {
                                hasChanges = true
                                details.durationMinutes
                            } else {
                                entry.episodeLengthMinutes
                            }
                        } else {
                            entry.episodeLengthMinutes
                        }

                        val newDurationMinutes = if (entry.mediaType == MediaType.MOVIE) {
                            if (details.durationMinutes != null && details.durationMinutes != entry.durationMinutes) {
                                hasChanges = true
                                details.durationMinutes
                            } else {
                                entry.durationMinutes
                            }
                        } else {
                            entry.durationMinutes
                        }

                        // Determine chapters & volumes for manga/manhwa
                        val newTotalChapters = if (isManga && entry.totalChapters == 0 && details.totalChapters != null && details.totalChapters > 0) {
                            hasChanges = true
                            details.totalChapters
                        } else {
                            entry.totalChapters
                        }

                        val newTotalVolumes = if (isManga && entry.totalVolumes == 0 && details.totalVolumes != null && details.totalVolumes > 0) {
                            hasChanges = true
                            details.totalVolumes
                        } else {
                            entry.totalVolumes
                        }

                        if (hasChanges) {
                            val updatedEntry = entry.copy(
                                coverImageUri = finalCoverUri ?: entry.coverImageUri,
                                episodeLengthMinutes = newEpisodeLength,
                                durationMinutes = newDurationMinutes,
                                totalChapters = newTotalChapters,
                                totalVolumes = newTotalVolumes,
                                externalSource = entry.externalSource ?: "MAL",
                                updatedAt = System.currentTimeMillis()
                            )
                            repository.update(updatedEntry)
                            updated++
                        }
                    }
                } else {
                    val err = detailsResult.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "Failed to sync entry ${entry.title} (#$malId): $err", detailsResult.exceptionOrNull())
                    lastError = err
                    failed++
                }

                completed++
                val pending = total - completed
                _syncState.update {
                    it.copy(
                        completedCount = completed,
                        pendingCount = pending,
                        updatedCount = updated,
                        failedCount = failed,
                        lastErrorMessage = lastError,
                        statusMessage = "Synced $completed / $total ($updated updated${if (failed > 0) ", $failed failed" else ""})"
                    )
                }
            }

            _syncState.update {
                it.copy(
                    isRunning = false,
                    currentTitle = "",
                    statusMessage = null,
                    isCompleted = true
                )
            }

            NotificationHelper.showJikanSyncComplete(
                context = context,
                completed = completed,
                total = total,
                updated = updated,
                failed = failed,
                lastError = lastError,
                isCancelled = (syncJob?.isCancelled == true)
            )
        }
    }

    fun cancelSync(context: Context? = null) {
        val wasRunning = _syncState.value.isRunning
        val completed = _syncState.value.completedCount
        val total = _syncState.value.totalCount
        val updated = _syncState.value.updatedCount
        val failed = _syncState.value.failedCount
        syncJob?.cancel()
        syncJob = null
        _syncState.update {
            it.copy(
                isRunning = false,
                currentTitle = "",
                statusMessage = null,
                isCompleted = false
            )
        }
        if (wasRunning) {
            val ctx = context ?: LXApplication.instance
            NotificationHelper.showJikanSyncComplete(
                context = ctx,
                completed = completed,
                total = total,
                updated = updated,
                failed = failed,
                lastError = null,
                isCancelled = true
            )
        }
    }
}
