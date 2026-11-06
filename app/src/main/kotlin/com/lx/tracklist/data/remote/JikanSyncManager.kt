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

    fun isEligibleMediaForMal(mediaType: MediaType): Boolean {
        return mediaType == MediaType.ANIME ||
               mediaType == MediaType.MANGA ||
               mediaType == MediaType.MANHWA ||
               mediaType == MediaType.MOVIE ||
               mediaType == MediaType.SHOW ||
               mediaType == MediaType.BOOK
    }

    fun isMalSource(entry: MediaEntry): Boolean {
        val src = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim()
        val hasMalSource = src.isNullOrBlank() || src.equals("MAL", ignoreCase = true) || src.equals("MyAnimeList", ignoreCase = true)
        val hasMalId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()?.toLongOrNull() != null
        return hasMalSource && (hasMalId || isEligibleMediaForMal(entry.mediaType))
    }

    fun isMangaType(mediaType: MediaType): Boolean {
        return mediaType == MediaType.MANGA || mediaType == MediaType.MANHWA || mediaType == MediaType.BOOK
    }

    private fun needsSync(entry: MediaEntry, forceRefresh: Boolean): Boolean {
        if (forceRefresh) return true

        val malId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()?.toLongOrNull()
        // If it doesn't have a MAL ID yet, it needs sync to fetch the ID and details based on title!
        if (malId == null) return true

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

        val missingEpisodes = isVideo && entry.mediaType != MediaType.MOVIE && entry.totalEpisodes == 0
        val missingChapters = !isVideo && entry.totalChapters == 0
        val missingVolumes = !isVideo && entry.totalVolumes == 0
        val missingAiredDates = entry.airedStartDate == null
        val missingAltTitles = entry.alternateTitle.isNullOrBlank()
        val missingTags = entry.tags.isNullOrBlank()
        val missingNotes = entry.notes.isNullOrBlank()
        val missingExternalScore = entry.externalScore == null

        return missingCover ||
               missingRuntime ||
               missingEpisodes ||
               missingChapters ||
               missingVolumes ||
               missingAiredDates ||
               missingAltTitles ||
               missingTags ||
               missingNotes ||
               missingExternalScore
    }

    fun startSync(
        context: Context,
        repository: MediaRepository,
        forceRefresh: Boolean = false,
        targetEntryIds: Set<Long>? = null
    ) {
        if (_syncState.value.isRunning) return

        syncJob = scope.launch {
            NotificationHelper.initNotificationChannels(context)

            // Find eligible entries across all media types (anime, movies, manga, manhwa, etc.)
            val allEntries = repository.getAllEntriesSync()
            val entriesPool = if (targetEntryIds != null) {
                allEntries.filter { targetEntryIds.contains(it.id) }
            } else {
                allEntries
            }

            val malEntries = entriesPool.filter { isMalSource(it) && it.title.isNotBlank() }
            val eligible = malEntries.filter { needsSync(it, forceRefresh) }

            val total = eligible.size
            if (total == 0) {
                val msg = if (targetEntryIds != null) {
                    if (entriesPool.isEmpty()) {
                        "Selected entries not found."
                    } else if (malEntries.isEmpty()) {
                        "Selected entries are not eligible for MAL sync."
                    } else {
                        "Selected entries already have posters & details."
                    }
                } else {
                    if (malEntries.isEmpty()) {
                        "No eligible anime or manga entries found to sync."
                    } else {
                        "All ${malEntries.size} entries already have MAL IDs, posters & details."
                    }
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

            val startMsg = if (targetEntryIds != null) {
                "Starting sync for $total selected entries..."
            } else {
                "Starting sync for $total MAL entries..."
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
                    statusMessage = startMsg,
                    isCompleted = false
                )
            }

            var completed = 0
            var updated = 0
            var failed = 0
            var lastError: String? = null

            for (entry in eligible) {
                if (syncJob?.isCancelled == true) break

                val malId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()?.toLongOrNull()
                val isManga = isMangaType(entry.mediaType)

                _syncState.update {
                    it.copy(
                        currentTitle = entry.title,
                        statusMessage = if (malId != null) {
                            "Fetching ${entry.title} (#$malId)..."
                        } else {
                            "Searching MAL for ${entry.title}..."
                        }
                    )
                }

                NotificationHelper.showJikanSyncProgress(context, completed, total, entry.title)

                // Rate limiting delay
                delay(RATE_LIMIT_DELAY_MS)

                // Attempt fetch by ID if present, otherwise search by title
                var detailsResult = if (malId != null) {
                    JikanApiClient.fetchMediaDetails(malId = malId, isManga = isManga)
                } else {
                    JikanApiClient.searchMediaDetailsByTitle(title = entry.title, isManga = isManga)
                }

                if (detailsResult.isFailure && detailsResult.exceptionOrNull() is RateLimitException) {
                    delay(3000L)
                    detailsResult = if (malId != null) {
                        JikanApiClient.fetchMediaDetails(malId = malId, isManga = isManga)
                    } else {
                        JikanApiClient.searchMediaDetailsByTitle(title = entry.title, isManga = isManga)
                    }
                }

                if (detailsResult.isSuccess) {
                    val details = detailsResult.getOrNull()
                    Log.d(TAG, "Successfully resolved entry '${entry.title}' (malId=${details?.malId}): poster=${details?.posterUrl}, duration=${details?.durationMinutes}, provider=${details?.sourceProvider}")
                    if (details != null) {
                        var hasChanges = false
                        var finalCoverUri: String? = entry.coverImageUri
                        var finalExternalId: String? = entry.externalId
                        var finalExternalSource: String? = entry.externalSource

                        // If it had no MAL ID, populate it now from search result!
                        if (malId == null && details.malId != null) {
                            val idStr = details.malId.toString()
                            finalExternalId = idStr
                            finalExternalSource = "MAL"
                            hasChanges = true
                        }

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
                            if (details.durationMinutes != null && details.durationMinutes > 0 &&
                                (entry.episodeLengthMinutes == null || entry.episodeLengthMinutes == 0 || forceRefresh) &&
                                details.durationMinutes != entry.episodeLengthMinutes
                            ) {
                                hasChanges = true
                                details.durationMinutes
                            } else {
                                entry.episodeLengthMinutes
                            }
                        } else {
                            entry.episodeLengthMinutes
                        }

                        val newDurationMinutes = if (entry.mediaType == MediaType.MOVIE) {
                            if (details.durationMinutes != null && details.durationMinutes > 0 &&
                                (entry.durationMinutes == null || entry.durationMinutes == 0 || forceRefresh) &&
                                details.durationMinutes != entry.durationMinutes
                            ) {
                                hasChanges = true
                                details.durationMinutes
                            } else {
                                entry.durationMinutes
                            }
                        } else {
                            entry.durationMinutes
                        }

                        // Determine total episodes for anime / shows
                        val newTotalEpisodes = if (!isManga && entry.mediaType != MediaType.MOVIE) {
                            if (details.totalEpisodes != null && details.totalEpisodes > 0 &&
                                (entry.totalEpisodes == 0 || forceRefresh) &&
                                details.totalEpisodes != entry.totalEpisodes
                            ) {
                                hasChanges = true
                                details.totalEpisodes
                            } else {
                                entry.totalEpisodes
                            }
                        } else {
                            entry.totalEpisodes
                        }

                        // Determine chapters & volumes for manga/manhwa/book
                        val newTotalChapters = if (isManga) {
                            if (details.totalChapters != null && details.totalChapters > 0 &&
                                (entry.totalChapters == 0 || forceRefresh) &&
                                details.totalChapters != entry.totalChapters
                            ) {
                                hasChanges = true
                                details.totalChapters
                            } else {
                                entry.totalChapters
                            }
                        } else {
                            entry.totalChapters
                        }

                        val newTotalVolumes = if (isManga) {
                            if (details.totalVolumes != null && details.totalVolumes > 0 &&
                                (entry.totalVolumes == 0 || forceRefresh) &&
                                details.totalVolumes != entry.totalVolumes
                            ) {
                                hasChanges = true
                                details.totalVolumes
                            } else {
                                entry.totalVolumes
                            }
                        } else {
                            entry.totalVolumes
                        }

                        // Broadcast / Published start and end dates
                        val newAiredStartDate = if (details.airedStartDate != null &&
                            (entry.airedStartDate == null || forceRefresh) &&
                            details.airedStartDate != entry.airedStartDate
                        ) {
                            hasChanges = true
                            details.airedStartDate
                        } else {
                            entry.airedStartDate
                        }

                        val newAiredEndDate = if (details.airedEndDate != null &&
                            (entry.airedEndDate == null || forceRefresh) &&
                            details.airedEndDate != entry.airedEndDate
                        ) {
                            hasChanges = true
                            details.airedEndDate
                        } else {
                            entry.airedEndDate
                        }

                        // Alternate titles (English, Japanese, Synonyms)
                        val newAlternateTitle = if (!details.alternateTitle.isNullOrBlank() &&
                            (entry.alternateTitle.isNullOrBlank() || forceRefresh) &&
                            details.alternateTitle != entry.alternateTitle
                        ) {
                            hasChanges = true
                            details.alternateTitle
                        } else {
                            entry.alternateTitle
                        }

                        // Tags (genres, themes, demographics)
                        val newTags = if (!details.tags.isNullOrBlank() &&
                            (entry.tags.isNullOrBlank() || forceRefresh) &&
                            details.tags != entry.tags
                        ) {
                            hasChanges = true
                            details.tags
                        } else {
                            entry.tags
                        }

                        // Notes (synopsis / description)
                        val newNotes = if (!details.synopsis.isNullOrBlank() &&
                            (entry.notes.isNullOrBlank() || forceRefresh) &&
                            details.synopsis != entry.notes
                        ) {
                            hasChanges = true
                            details.synopsis
                        } else {
                            entry.notes
                        }

                        // Airing status
                        val newIsAiring = if (details.isAiring != null && details.isAiring != entry.isAiring) {
                            hasChanges = true
                            details.isAiring
                        } else {
                            entry.isAiring
                        }

                        // External Score (e.g. MAL / AniList score) - DO NOT update user personal score!
                        val newExternalScore = if (details.externalScore != null &&
                            (entry.externalScore == null || forceRefresh) &&
                            details.externalScore != entry.externalScore
                        ) {
                            hasChanges = true
                            details.externalScore
                        } else {
                            entry.externalScore
                        }

                        val newExternalScoreSource = if (details.externalScore != null &&
                            (entry.externalScore == null || forceRefresh) &&
                            (details.externalScoreSource ?: "MAL") != entry.externalScoreSource
                        ) {
                            hasChanges = true
                            details.externalScoreSource ?: "MAL"
                        } else {
                            entry.externalScoreSource
                        }

                        if (hasChanges) {
                            val updatedEntry = entry.copy(
                                coverImageUri = finalCoverUri ?: entry.coverImageUri,
                                episodeLengthMinutes = newEpisodeLength,
                                durationMinutes = newDurationMinutes,
                                totalEpisodes = newTotalEpisodes,
                                totalChapters = newTotalChapters,
                                totalVolumes = newTotalVolumes,
                                airedStartDate = newAiredStartDate,
                                airedEndDate = newAiredEndDate,
                                alternateTitle = newAlternateTitle,
                                tags = newTags,
                                notes = newNotes,
                                isAiring = newIsAiring,
                                externalScore = newExternalScore,
                                externalScoreSource = newExternalScoreSource,
                                externalId = finalExternalId,
                                externalSource = finalExternalSource ?: entry.externalSource,
                                updatedAt = System.currentTimeMillis()
                            )
                            repository.update(updatedEntry)
                            updated++
                        }
                    }
                } else {
                    val err = detailsResult.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "Failed to sync entry ${entry.title} (malId=$malId): $err", detailsResult.exceptionOrNull())
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
