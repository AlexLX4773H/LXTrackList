package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.gson.Gson
import com.lx.tracklist.LXApplication
import com.lx.tracklist.data.local.FailedCoverTracker
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.repository.MediaRepository
import com.lx.tracklist.util.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private data class TmdbSyncItemResult(
    val hasChanges: Boolean,
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val isRateLimit: Boolean = false
)

data class TmdbSyncProgress(
    val isRunning: Boolean = false,
    val profileId: Long? = null,
    val profileName: String? = null,
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

object TmdbSyncManager {

    private const val TAG = "TmdbSync"
    private const val RATE_LIMIT_DELAY_MS = 250L
    private val gson = Gson()

    private val _syncState = MutableStateFlow(TmdbSyncProgress())
    val syncState: StateFlow<TmdbSyncProgress> = _syncState.asStateFlow()

    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun isEligibleMediaForTmdb(mediaType: MediaType): Boolean {
        return mediaType == MediaType.MOVIE || mediaType == MediaType.SHOW
    }

    fun isTmdbSource(entry: MediaEntry): Boolean {
        if (!isEligibleMediaForTmdb(entry.mediaType)) return false
        val src = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim()
        if (src.equals("MAL", ignoreCase = true) || src.equals("MyAnimeList", ignoreCase = true)) {
            return false
        }
        val hasTmdbSource = src.isNullOrBlank() || src.equals("TMDB", ignoreCase = true)
        val hasTmdbId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()?.toLongOrNull() != null
        return hasTmdbSource || hasTmdbId
    }

    private fun needsSync(entry: MediaEntry, forceRefresh: Boolean): Boolean {
        if (forceRefresh) return true

        val tmdbId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()?.toLongOrNull()
        if (tmdbId == null) return true

        val missingCover = entry.coverImageUri.isNullOrBlank() ||
                           entry.coverImageUri.startsWith("file:") ||
                           FailedCoverTracker.isEntryCoverBroken(entry)
        val isMovie = entry.mediaType == MediaType.MOVIE
        val missingRuntime = if (isMovie) {
            entry.durationMinutes == null || entry.durationMinutes == 0
        } else {
            entry.episodeLengthMinutes == null || entry.episodeLengthMinutes == 0
        }

        val missingEpisodes = !isMovie && (entry.totalEpisodes == 0 || (entry.mediaType == MediaType.SHOW && entry.totalSeasons > 1 && entry.totalEpisodes <= 1))
        val missingSeasons = !isMovie && entry.totalSeasons <= 1
        val missingSeasonEpisodes = entry.mediaType == MediaType.SHOW && !entry.hasSeasonEpisodeTracking()
        val missingAiredDates = entry.airedStartDate == null
        val missingAltTitles = entry.alternateTitle.isNullOrBlank()
        val missingTags = entry.tags.isNullOrBlank()
        val missingDescription = (entry.description ?: entry.notes).isNullOrBlank()
        val missingExternalScore = entry.externalScore == null
        val missingRelated = entry.relatedEntriesJson.isNullOrBlank()

        return missingCover ||
                missingRuntime ||
                missingEpisodes ||
                missingSeasons ||
                missingSeasonEpisodes ||
                missingAiredDates ||
                missingAltTitles ||
                missingTags ||
                missingDescription ||
                missingExternalScore ||
                missingRelated
    }

    fun startSync(
        context: Context,
        repository: MediaRepository,
        forceRefresh: Boolean = false,
        targetEntryIds: Set<Long>? = null,
        targetProfileId: Long? = null,
        targetProfileName: String? = null
    ) {
        val profileId = targetProfileId ?: repository.activeUser.value.id
        val profileName = targetProfileName ?: repository.activeUser.value.name

        if (_syncState.value.isRunning) {
            if (_syncState.value.profileId == profileId) {
                return
            } else {
                cancelSync(context)
            }
        }

        if (!TmdbConfig.isConfigured(context)) {
            _syncState.update {
                it.copy(
                    isRunning = false,
                    profileId = profileId,
                    profileName = profileName,
                    lastErrorMessage = "TMDB API credentials are missing. Please configure your Read Access Token.",
                    statusMessage = "TMDB API token missing",
                    isCompleted = true
                )
            }
            NotificationHelper.showTmdbSyncComplete(
                context = context,
                completed = 0,
                total = 0,
                updated = 0,
                failed = 0,
                lastError = "TMDB Read Access Token is not configured.",
                isCancelled = false,
                profileName = profileName
            )
            return
        }

        syncJob = scope.launch {
            NotificationHelper.initNotificationChannels(context)

            val allEntries = repository.getAllEntriesForUserSync(profileId)
            val entriesPool = if (targetEntryIds != null) {
                allEntries.filter { targetEntryIds.contains(it.id) }
            } else {
                allEntries
            }

            val tmdbEntries = entriesPool.filter { isTmdbSource(it) && it.title.isNotBlank() }

            // Probe covers for broken image URLs so "Fetch Missing" picks them up
            if (!forceRefresh) {
                _syncState.update {
                    it.copy(
                        profileId = profileId,
                        profileName = profileName,
                        statusMessage = "Checking cover images..."
                    )
                }
                FailedCoverTracker.probeBrokenCovers(tmdbEntries)
            }

            val eligible = tmdbEntries.filter { needsSync(it, forceRefresh) }

            val total = eligible.size
            if (total == 0) {
                val msg = if (targetEntryIds != null) {
                    if (entriesPool.isEmpty()) {
                        "Selected entries not found."
                    } else if (tmdbEntries.isEmpty()) {
                        "Selected entries are not eligible for TMDB sync (Movies/Shows only)."
                    } else {
                        "Selected entries already have posters & details from TMDB."
                    }
                } else {
                    if (tmdbEntries.isEmpty()) {
                        "No eligible movie or TV show entries found to sync."
                    } else {
                        "All ${tmdbEntries.size} movie & TV entries already have TMDB posters and details."
                    }
                }
                _syncState.update {
                    it.copy(
                        isRunning = false,
                        profileId = profileId,
                        profileName = profileName,
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
                NotificationHelper.showTmdbSyncComplete(
                    context = context,
                    completed = 0,
                    total = 0,
                    updated = 0,
                    failed = 0,
                    lastError = msg,
                    isCancelled = false,
                    profileName = profileName
                )
                return@launch
            }

            val startMsg = if (targetEntryIds != null) {
                "Starting TMDB sync for $total selected entries..."
            } else {
                "Starting TMDB sync for $total entries..."
            }
            _syncState.update {
                it.copy(
                    isRunning = true,
                    profileId = profileId,
                    profileName = profileName,
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

            var currentBatchSize = 4
            val minBatchSize = 1
            val maxBatchSize = 6
            var interBatchDelayMs = 150L
            var consecutiveCleanBatches = 0
            val mutex = Mutex()

            var index = 0
            while (index < eligible.size && syncJob?.isCancelled != true) {
                val batchSize = currentBatchSize.coerceIn(minBatchSize, maxBatchSize)
                val chunk = eligible.subList(index, minOf(index + batchSize, eligible.size))

                val chunkTitles = chunk.joinToString(", ") { it.title }
                _syncState.update {
                    it.copy(
                        profileId = profileId,
                        profileName = profileName,
                        currentTitle = if (chunk.size == 1) chunk[0].title else "${chunk.size} syncing: ${chunkTitles.take(50)}...",
                        statusMessage = "Syncing ${chunk.size} movies/shows in parallel..."
                    )
                }

                var hadRateLimit = false

                coroutineScope {
                    chunk.map { entry ->
                        async {
                            if (syncJob?.isCancelled == true) return@async
                            try {
                                val outcome = syncSingleEntry(
                                    context = context,
                                    repository = repository,
                                    entry = entry,
                                    profileId = profileId,
                                    forceRefresh = forceRefresh
                                )
                                mutex.withLock {
                                    completed++
                                    if (outcome.isSuccess) {
                                        if (outcome.hasChanges) updated++
                                    } else {
                                        failed++
                                        lastError = outcome.errorMessage
                                        if (outcome.isRateLimit) hadRateLimit = true
                                    }
                                    val pending = total - completed
                                    _syncState.update {
                                        it.copy(
                                            completedCount = completed,
                                            pendingCount = pending,
                                            updatedCount = updated,
                                            failedCount = failed,
                                            lastErrorMessage = lastError,
                                            currentTitle = entry.title,
                                            statusMessage = "Synced $completed / $total ($updated updated${if (failed > 0) ", $failed failed" else ""})"
                                        )
                                    }
                                    NotificationHelper.showTmdbSyncProgress(context, completed, total, entry.title, profileName = profileName)
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                val err = e.message ?: "Unknown error"
                                mutex.withLock {
                                    completed++
                                    failed++
                                    lastError = err
                                    if (err.contains("429", ignoreCase = true)) hadRateLimit = true
                                    val pending = total - completed
                                    _syncState.update {
                                        it.copy(
                                            completedCount = completed,
                                            pendingCount = pending,
                                            updatedCount = updated,
                                            failedCount = failed,
                                            lastErrorMessage = lastError,
                                            currentTitle = entry.title
                                        )
                                    }
                                }
                            }
                        }
                    }.awaitAll()
                }

                index += chunk.size

                if (hadRateLimit) {
                    currentBatchSize = (currentBatchSize - 1).coerceAtLeast(minBatchSize)
                    interBatchDelayMs = (interBatchDelayMs + 200L).coerceAtMost(1000L)
                    consecutiveCleanBatches = 0
                    Log.w(TAG, "TMDB rate limit hit. Reduced batch size to $currentBatchSize, delay to ${interBatchDelayMs}ms")
                    delay(1200L)
                } else {
                    consecutiveCleanBatches++
                    if (consecutiveCleanBatches >= 2 && currentBatchSize < maxBatchSize) {
                        currentBatchSize++
                        interBatchDelayMs = (interBatchDelayMs - 30L).coerceAtLeast(80L)
                        consecutiveCleanBatches = 0
                    }
                    if (interBatchDelayMs > 0 && index < eligible.size) {
                        delay(interBatchDelayMs)
                    }
                }
            }



            _syncState.update {
                it.copy(
                    isRunning = false,
                    profileId = profileId,
                    profileName = profileName,
                    currentTitle = "",
                    statusMessage = null,
                    isCompleted = true
                )
            }

            NotificationHelper.showTmdbSyncComplete(
                context = context,
                completed = completed,
                total = total,
                updated = updated,
                failed = failed,
                lastError = lastError,
                isCancelled = (syncJob?.isCancelled == true),
                profileName = profileName
            )
        }
    }

    private suspend fun syncSingleEntry(
        context: Context,
        repository: MediaRepository,
        entry: MediaEntry,
        profileId: Long,
        forceRefresh: Boolean
    ): TmdbSyncItemResult {
        val tmdbId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()?.toLongOrNull()
        val isTv = entry.mediaType == MediaType.SHOW

        var detailsResult = if (tmdbId != null) {
            TmdbApiClient.fetchMediaDetails(tmdbId = tmdbId, isTv = isTv, context = context)
        } else {
            TmdbApiClient.searchMediaDetailsByTitle(title = entry.title, isTv = isTv, context = context)
        }

        if (detailsResult.isFailure && detailsResult.exceptionOrNull() is TmdbRateLimitException) {
            delay(1500L)
            detailsResult = if (tmdbId != null) {
                TmdbApiClient.fetchMediaDetails(tmdbId = tmdbId, isTv = isTv, context = context)
            } else {
                TmdbApiClient.searchMediaDetailsByTitle(title = entry.title, isTv = isTv, context = context)
            }
        }

        if (detailsResult.isSuccess) {
            val details = detailsResult.getOrNull()
            Log.d(TAG, "Successfully resolved entry '${entry.title}' (tmdbId=${details?.tmdbId}): poster=${details?.posterUrl}, duration=${details?.durationMinutes}")
            if (details != null) {
                var hasChanges = false
                var finalCoverUri: String? = entry.coverImageUri
                var finalExternalId: String? = entry.externalId
                var finalExternalSource: String? = entry.externalSource

                if (tmdbId == null && details.tmdbId > 0) {
                    finalExternalId = details.tmdbId.toString()
                    finalExternalSource = "TMDB"
                    hasChanges = true
                }

                val isCoverBroken = FailedCoverTracker.isEntryCoverBroken(entry)
                val needsPoster = entry.coverImageUri.isNullOrBlank() ||
                                  entry.coverImageUri.startsWith("file:") ||
                                  isCoverBroken ||
                                  forceRefresh
                if (!details.posterUrl.isNullOrBlank() && needsPoster) {
                    finalCoverUri = details.posterUrl
                    hasChanges = true

                    FailedCoverTracker.markSuccess(entry.id, details.posterUrl)

                    // Pre-warm Coil cache directly
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

                // Determine runtime for movie vs show
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

                val newEpisodeLength = if (entry.mediaType == MediaType.SHOW) {
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

                // Total episodes for show
                val newTotalEpisodes = if (entry.mediaType == MediaType.SHOW) {
                    if (details.totalEpisodes != null && details.totalEpisodes > 0 &&
                        (entry.totalEpisodes == 0 || (entry.totalSeasons > 1 && entry.totalEpisodes <= 1) || forceRefresh) &&
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

                // Total seasons for show
                val newTotalSeasons = if (entry.mediaType == MediaType.SHOW) {
                    if (details.totalSeasons != null && details.totalSeasons > 0 &&
                        (entry.totalSeasons <= 1 || forceRefresh) &&
                        details.totalSeasons != entry.totalSeasons
                    ) {
                        hasChanges = true
                        details.totalSeasons
                    } else {
                        entry.totalSeasons
                    }
                } else {
                    entry.totalSeasons
                }

                // Season-by-season episodes for show
                val newSeasonEpisodesJson = if (entry.mediaType == MediaType.SHOW && details.seasonEpisodes != null) {
                    val formatted = MediaEntry.formatSeasonEpisodes(details.seasonEpisodes)
                    if (formatted != null && (!entry.hasSeasonEpisodeTracking() || forceRefresh) && formatted != entry.seasonEpisodesJson) {
                        hasChanges = true
                        formatted
                    } else {
                        entry.seasonEpisodesJson
                    }
                } else {
                    entry.seasonEpisodesJson
                }

                // Release / Broadcast dates
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

                // Alternate titles
                val newAlternateTitle = if (!details.alternateTitle.isNullOrBlank() &&
                    (entry.alternateTitle.isNullOrBlank() || forceRefresh) &&
                    details.alternateTitle != entry.alternateTitle
                ) {
                    hasChanges = true
                    details.alternateTitle
                } else {
                    entry.alternateTitle
                }

                // Tags (genres)
                val newTags = if (!details.tags.isNullOrBlank() &&
                    (entry.tags.isNullOrBlank() || forceRefresh) &&
                    details.tags != entry.tags
                ) {
                    hasChanges = true
                    details.tags
                } else {
                    entry.tags
                }

                // Description (synopsis)
                val curDesc = entry.description ?: entry.notes
                val newDescription = if (!details.synopsis.isNullOrBlank() &&
                    (curDesc.isNullOrBlank() || forceRefresh) &&
                    details.synopsis != curDesc
                ) {
                    hasChanges = true
                    details.synopsis
                } else {
                    curDesc
                }

                // Airing status
                val newIsAiring = if (details.isAiring != null && details.isAiring != entry.isAiring) {
                    hasChanges = true
                    details.isAiring
                } else {
                    entry.isAiring
                }

                // External Score
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
                    (details.externalScoreSource) != entry.externalScoreSource
                ) {
                    hasChanges = true
                    details.externalScoreSource
                } else {
                    entry.externalScoreSource
                }

                // Extract related entries (collection parts, sequels, prequels, recommendations)
                val newRelatedJson = if ((entry.relatedEntriesJson.isNullOrBlank() || forceRefresh) && finalExternalId != null) {
                    val targetTmdbId = finalExternalId.toLongOrNull()
                    if (targetTmdbId != null) {
                        val relRes = TmdbApiClient.fetchRelatedEntries(targetTmdbId, isTv = isTv, context = context)
                        val relList = relRes.getOrNull()
                        if (!relList.isNullOrEmpty()) {
                            val json = gson.toJson(relList)
                            if (json != entry.relatedEntriesJson) {
                                hasChanges = true
                                json
                            } else {
                                entry.relatedEntriesJson
                            }
                        } else {
                            entry.relatedEntriesJson
                        }
                    } else {
                        entry.relatedEntriesJson
                    }
                } else {
                    entry.relatedEntriesJson
                }

                if (hasChanges) {
                    val updatedEntry = entry.copy(
                        coverImageUri = finalCoverUri ?: entry.coverImageUri,
                        durationMinutes = newDurationMinutes,
                        episodeLengthMinutes = newEpisodeLength,
                        totalEpisodes = newTotalEpisodes,
                        totalSeasons = newTotalSeasons,
                        seasonEpisodesJson = newSeasonEpisodesJson,
                        airedStartDate = newAiredStartDate,
                        airedEndDate = newAiredEndDate,
                        alternateTitle = newAlternateTitle,
                        tags = newTags,
                        description = newDescription,
                        isAiring = newIsAiring,
                        externalScore = newExternalScore,
                        externalScoreSource = newExternalScoreSource,
                        externalId = finalExternalId,
                        externalSource = finalExternalSource ?: entry.externalSource,
                        relatedEntriesJson = newRelatedJson,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.update(updatedEntry.copy(userId = profileId))
                }
                return TmdbSyncItemResult(hasChanges = hasChanges, isSuccess = true)
            }
        }

        val err = detailsResult.exceptionOrNull()?.message ?: "Unknown error"
        Log.e(TAG, "Failed to sync entry ${entry.title} (tmdbId=$tmdbId): $err", detailsResult.exceptionOrNull())
        val isRateLimit = detailsResult.exceptionOrNull() is TmdbRateLimitException || err.contains("429", ignoreCase = true)
        return TmdbSyncItemResult(hasChanges = false, isSuccess = false, errorMessage = err, isRateLimit = isRateLimit)
    }

    fun cancelSync(context: Context? = null) {
        val wasRunning = _syncState.value.isRunning
        val profileName = _syncState.value.profileName
        val completed = _syncState.value.completedCount
        val total = _syncState.value.totalCount
        val updated = _syncState.value.updatedCount
        val failed = _syncState.value.failedCount
        syncJob?.cancel()
        syncJob = null
        _syncState.update {
            it.copy(
                isRunning = false,
                profileId = null,
                profileName = null,
                currentTitle = "",
                statusMessage = null,
                isCompleted = false
            )
        }
        if (wasRunning) {
            val ctx = context ?: LXApplication.instance
            NotificationHelper.showTmdbSyncComplete(
                context = ctx,
                completed = completed,
                total = total,
                updated = updated,
                failed = failed,
                lastError = null,
                isCancelled = true,
                profileName = profileName
            )
        }
    }
}

