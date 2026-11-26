package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.gson.Gson
import com.lx.tracklist.LXApplication
import com.lx.tracklist.data.local.FailedCoverTracker
import com.lx.tracklist.data.model.ExternalSources
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
        return SyncRouter.jobFor(entry) == SyncJob.TMDB
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
        val missingCreator = entry.creator.isNullOrBlank()
        val isAiring = entry.isAiring

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
                missingRelated ||
                missingCreator ||
                isAiring
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
                    lastErrorMessage = "TMDB API credentials are missing. Add an API key in Settings → External Sync Jobs → API Keys.",
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
                lastError = "TMDB Read Access Token is not configured. Add an API key in Settings → External Sync Jobs → API Keys.",
                isCancelled = false,
                profileName = profileName
            )
            return
        }

        _syncState.update {
            it.copy(
                isRunning = true,
                profileId = profileId,
                profileName = profileName,
                totalCount = 0,
                completedCount = 0,
                pendingCount = 0,
                updatedCount = 0,
                failedCount = 0,
                lastErrorMessage = null,
                currentTitle = "",
                statusMessage = if (!forceRefresh) "Checking cover images..." else "Preparing sync...",
                isCompleted = false
            )
        }

        syncJob = scope.launch {
            SyncForegroundService.start(context)
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
                FailedCoverTracker.probeBrokenCovers(tmdbEntries, context)
            }

            val eligible = if (targetEntryIds != null) tmdbEntries else tmdbEntries.filter { needsSync(it, forceRefresh) }

            val total = eligible.size
            if (total == 0) {
                val msg = if (targetEntryIds != null) {
                    if (entriesPool.isEmpty()) {
                        "Selected entries not found."
                    } else if (tmdbEntries.isEmpty()) {
                        "Selected entries are not eligible for TMDB sync (Movies, TV Shows, or TMDB-linked Anime)."
                    } else {
                        "Selected entries already have posters & details from TMDB."
                    }
                } else {
                    if (tmdbEntries.isEmpty()) {
                        "No eligible movie, TV show, or TMDB-linked anime entries found to sync."
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
                SyncForegroundService.checkAndStop(context)
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
            SyncForegroundService.checkAndStop(context)
        }
    }

    private suspend fun syncSingleEntry(
        context: Context,
        repository: MediaRepository,
        entry: MediaEntry,
        profileId: Long,
        forceRefresh: Boolean
    ): TmdbSyncItemResult {
        val isTvContaminatedMovie = entry.mediaType == MediaType.MOVIE && (
            entry.alternateTitle?.contains("Dr. Hudson", ignoreCase = true) == true ||
            (entry.title.equals("Black Widow", ignoreCase = true) && entry.externalId == "19345")
        )
        val tmdbId = if (isTvContaminatedMovie) null else (entry.externalId ?: entry.foreignDatabaseId)?.trim()?.toLongOrNull()
        val inferredTvForAnime = entry.totalSeasons > 1 ||
            entry.totalEpisodes > 1 ||
            entry.episodeLengthMinutes != null ||
            (entry.durationMinutes ?: 0) == 0
        var isTv = when (entry.mediaType) {
            MediaType.SHOW -> true
            MediaType.MOVIE -> false
            MediaType.ANIME -> inferredTvForAnime
            else -> false
        }

        var detailsResult = if (tmdbId != null) {
            TmdbApiClient.fetchMediaDetails(tmdbId = tmdbId, isTv = isTv, context = context)
        } else {
            TmdbApiClient.searchMediaDetailsByTitle(title = entry.title, isTv = isTv, context = context)
        }

        if (entry.mediaType == MediaType.ANIME && tmdbId != null &&
            detailsResult.isFailure && detailsResult.exceptionOrNull() is TmdbNotFoundException
        ) {
            isTv = !isTv
            detailsResult = TmdbApiClient.fetchMediaDetails(tmdbId = tmdbId, isTv = isTv, context = context)
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
                val candidateExternalId = if ((tmdbId == null || isTvContaminatedMovie) && details.tmdbId > 0) {
                    details.tmdbId.toString()
                } else {
                    entry.externalId
                }

                val fetchedRelatedJson = if (entry.relatedEntriesJson.isNullOrBlank() || forceRefresh) {
                    if (!details.relatedItems.isNullOrEmpty()) {
                        gson.toJson(details.relatedItems)
                    } else {
                        val targetTmdbId = candidateExternalId?.toLongOrNull()
                        if (targetTmdbId != null) {
                            val relRes = TmdbApiClient.fetchRelatedEntries(targetTmdbId, isTv = isTv, context = context)
                            val relList = relRes.getOrNull()
                            if (!relList.isNullOrEmpty()) gson.toJson(relList) else null
                        } else null
                    }
                } else {
                    null
                }

                // BUG-17: Re-read latest row right before merging so concurrent user edits survive sync.
                val latest = repository.getEntryByIdSync(entry.id) ?: entry
                val isLatestTvContaminatedMovie = latest.mediaType == MediaType.MOVIE && (
                    latest.alternateTitle?.contains("Dr. Hudson", ignoreCase = true) == true ||
                    (latest.title.equals("Black Widow", ignoreCase = true) && latest.externalId == "19345")
                )
                val latestTmdbIdMissing = (if (isLatestTvContaminatedMovie) null else (latest.externalId ?: latest.foreignDatabaseId)?.trim()?.toLongOrNull()) == null
                val isCoverBroken = FailedCoverTracker.isEntryCoverBroken(latest)
                val (updatedEntry, hasChanges) = applyTmdbMetadataToFresh(
                    latest = latest,
                    details = details,
                    isTvContaminatedMovie = isLatestTvContaminatedMovie,
                    tmdbIdWasMissing = latestTmdbIdMissing,
                    isCoverBroken = isCoverBroken,
                    fetchedRelatedJson = fetchedRelatedJson,
                    forceRefresh = forceRefresh
                )

                if (hasChanges) {
                    if (updatedEntry.coverImageUri != latest.coverImageUri && !updatedEntry.coverImageUri.isNullOrBlank()) {
                        FailedCoverTracker.markSuccess(latest.id, updatedEntry.coverImageUri)
                        try {
                            val imageLoader = Coil.imageLoader(context)
                            val request = ImageRequest.Builder(context)
                                .data(updatedEntry.coverImageUri)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build()
                            imageLoader.enqueue(request)
                        } catch (_: Exception) {}
                    }
                    val effectiveUserId = if (updatedEntry.userId == 0L) profileId else updatedEntry.userId
                    repository.updateMetadataOnly(updatedEntry.copy(userId = effectiveUserId))
                }
                return TmdbSyncItemResult(hasChanges = hasChanges, isSuccess = true)
            }
        }

        val err = detailsResult.exceptionOrNull()?.message ?: "Unknown error"
        Log.e(TAG, "Failed to sync entry ${entry.title} (tmdbId=$tmdbId): $err", detailsResult.exceptionOrNull())
        val isRateLimit = detailsResult.exceptionOrNull() is TmdbRateLimitException || err.contains("429", ignoreCase = true)
        return TmdbSyncItemResult(hasChanges = false, isSuccess = false, errorMessage = err, isRateLimit = isRateLimit)
    }

    internal fun applyTmdbMetadataToFresh(
        latest: MediaEntry,
        details: TmdbMediaDetails,
        isTvContaminatedMovie: Boolean = false,
        tmdbIdWasMissing: Boolean = (latest.externalId ?: latest.foreignDatabaseId).isNullOrBlank(),
        isCoverBroken: Boolean = false,
        fetchedRelatedJson: String? = null,
        forceRefresh: Boolean = false,
        now: Long = System.currentTimeMillis()
    ): Pair<MediaEntry, Boolean> {
        var hasChanges = false
        var finalCoverUri: String? = latest.coverImageUri
        var finalExternalId: String? = latest.externalId
        var finalExternalSource: String? = latest.externalSource
        val isTv = when (latest.mediaType) {
            MediaType.SHOW -> true
            MediaType.MOVIE -> false
            MediaType.ANIME -> details.type.equals("TV", ignoreCase = true) ||
                (!details.type.equals("Movie", ignoreCase = true) && (
                    latest.totalSeasons > 1 ||
                    latest.totalEpisodes > 1 ||
                    latest.episodeLengthMinutes != null ||
                    (latest.durationMinutes ?: 0) == 0
                ))
            else -> false
        }
        val isMovieLike = !isTv && (latest.mediaType == MediaType.MOVIE || latest.mediaType == MediaType.ANIME)
        val isShowLike = isTv && (latest.mediaType == MediaType.SHOW || latest.mediaType == MediaType.ANIME)

        if ((tmdbIdWasMissing || isTvContaminatedMovie) && details.tmdbId > 0) {
            finalExternalId = details.tmdbId.toString()
            finalExternalSource = ExternalSources.TMDB
            hasChanges = true
        }

        val needsPoster = latest.coverImageUri.isNullOrBlank() ||
            latest.coverImageUri.startsWith("file:") ||
            isCoverBroken ||
            isTvContaminatedMovie ||
            forceRefresh
        if (!details.posterUrl.isNullOrBlank() && needsPoster && details.posterUrl != latest.coverImageUri) {
            finalCoverUri = details.posterUrl
            hasChanges = true
        }

        val newDurationMinutes = if (isMovieLike) {
            if (details.durationMinutes != null && details.durationMinutes > 0 &&
                (latest.durationMinutes == null || latest.durationMinutes == 0 || forceRefresh) &&
                details.durationMinutes != latest.durationMinutes
            ) {
                hasChanges = true
                details.durationMinutes
            } else {
                latest.durationMinutes
            }
        } else {
            latest.durationMinutes
        }

        val newEpisodeLength = if (isShowLike) {
            if (details.durationMinutes != null && details.durationMinutes > 0 &&
                (latest.episodeLengthMinutes == null || latest.episodeLengthMinutes == 0 || forceRefresh) &&
                details.durationMinutes != latest.episodeLengthMinutes
            ) {
                hasChanges = true
                details.durationMinutes
            } else {
                latest.episodeLengthMinutes
            }
        } else {
            latest.episodeLengthMinutes
        }

        val newTotalEpisodes = if (isShowLike) {
            if (details.totalEpisodes != null && details.totalEpisodes > 0 &&
                (latest.totalEpisodes == 0 || (latest.totalSeasons > 1 && latest.totalEpisodes <= 1) || forceRefresh) &&
                details.totalEpisodes != latest.totalEpisodes
            ) {
                hasChanges = true
                details.totalEpisodes
            } else {
                latest.totalEpisodes
            }
        } else {
            latest.totalEpisodes
        }

        val newTotalSeasons = if (isShowLike) {
            if (details.totalSeasons != null && details.totalSeasons > 0 &&
                (latest.totalSeasons <= 1 || forceRefresh) &&
                details.totalSeasons != latest.totalSeasons
            ) {
                hasChanges = true
                details.totalSeasons
            } else {
                latest.totalSeasons
            }
        } else {
            latest.totalSeasons
        }

        val newSeasonEpisodesJson = if (isShowLike && details.seasonEpisodes != null) {
            val formatted = MediaEntry.formatSeasonEpisodes(details.seasonEpisodes)
            if (formatted != null && (!latest.hasSeasonEpisodeTracking() || forceRefresh) && formatted != latest.seasonEpisodesJson) {
                hasChanges = true
                formatted
            } else {
                latest.seasonEpisodesJson
            }
        } else {
            latest.seasonEpisodesJson
        }

        val newAiredStartDate = if (details.airedStartDate != null &&
            (latest.airedStartDate == null || forceRefresh) &&
            details.airedStartDate != latest.airedStartDate
        ) {
            hasChanges = true
            details.airedStartDate
        } else {
            latest.airedStartDate
        }

        val newAiredEndDate = if (details.airedEndDate != null &&
            (latest.airedEndDate == null || forceRefresh) &&
            details.airedEndDate != latest.airedEndDate
        ) {
            hasChanges = true
            details.airedEndDate
        } else {
            latest.airedEndDate
        }

        val newAlternateTitle = if (!details.alternateTitle.isNullOrBlank() &&
            (latest.alternateTitle.isNullOrBlank() || forceRefresh || isTvContaminatedMovie) &&
            details.alternateTitle != latest.alternateTitle
        ) {
            hasChanges = true
            details.alternateTitle
        } else if (isTvContaminatedMovie || (forceRefresh && details.alternateTitle.isNullOrBlank() && latest.alternateTitle?.contains("Dr. Hudson", ignoreCase = true) == true)) {
            hasChanges = true
            null
        } else {
            latest.alternateTitle
        }

        val newTags = if (!details.tags.isNullOrBlank() &&
            (latest.tags.isNullOrBlank() || forceRefresh) &&
            details.tags != latest.tags
        ) {
            hasChanges = true
            details.tags
        } else {
            latest.tags
        }

        val curDesc = latest.description ?: latest.notes
        val formattedDesc = details.buildDescriptionWithMetadata().takeIf { it.isNotBlank() } ?: details.synopsis
        val newDescription = if (!formattedDesc.isNullOrBlank() &&
            (curDesc.isNullOrBlank() || forceRefresh) &&
            formattedDesc != curDesc
        ) {
            hasChanges = true
            formattedDesc
        } else {
            curDesc
        }

        val newIsAiring = if (details.isAiring != null && details.isAiring != latest.isAiring) {
            hasChanges = true
            details.isAiring
        } else {
            latest.isAiring
        }

        val newAiredEpisodes = if (isShowLike) {
            if (details.airedEpisodes != null && details.airedEpisodes > 0 &&
                details.airedEpisodes != latest.airedEpisodes
            ) {
                hasChanges = true
                details.airedEpisodes
            } else if (newIsAiring) {
                val effective = latest.copy(
                    airedStartDate = newAiredStartDate,
                    totalEpisodes = newTotalEpisodes,
                    isAiring = newIsAiring
                ).getEffectiveAiredEpisodes()
                if (effective > 0 && effective != latest.airedEpisodes) {
                    hasChanges = true
                    effective
                } else {
                    latest.airedEpisodes
                }
            } else {
                latest.airedEpisodes
            }
        } else {
            latest.airedEpisodes
        }

        val newExternalScore = if (details.externalScore != null &&
            (latest.externalScore == null || forceRefresh) &&
            details.externalScore != latest.externalScore
        ) {
            hasChanges = true
            details.externalScore
        } else {
            latest.externalScore
        }

        val newExternalScoreSource = if (details.externalScore != null &&
            (latest.externalScore == null || forceRefresh) &&
            details.externalScoreSource != latest.externalScoreSource
        ) {
            hasChanges = true
            details.externalScoreSource
        } else {
            latest.externalScoreSource
        }

        val candidateRelatedJson = when {
            !details.relatedItems.isNullOrEmpty() -> gson.toJson(details.relatedItems)
            !fetchedRelatedJson.isNullOrBlank() -> fetchedRelatedJson
            else -> null
        }
        val newRelatedJson = if ((latest.relatedEntriesJson.isNullOrBlank() || forceRefresh) &&
            !candidateRelatedJson.isNullOrBlank() &&
            candidateRelatedJson != latest.relatedEntriesJson
        ) {
            hasChanges = true
            candidateRelatedJson
        } else {
            latest.relatedEntriesJson
        }

        val newOriginalLanguage = if (latest.originalLanguage.isNullOrBlank() && !details.originalLanguage.isNullOrBlank()) {
            hasChanges = true
            details.originalLanguage
        } else {
            latest.originalLanguage
        }

        val resolvedCreator = details.resolveCreatorFor(isTv)
        val newCreator = if (!resolvedCreator.isNullOrBlank() && (latest.creator.isNullOrBlank() || forceRefresh) && resolvedCreator != latest.creator) {
            hasChanges = true
            resolvedCreator
        } else {
            latest.creator
        }

        val newMovieSeriesName = if (isMovieLike &&
            !details.movieSeriesName.isNullOrBlank() &&
            (latest.movieSeriesName.isNullOrBlank() || forceRefresh) &&
            details.movieSeriesName != latest.movieSeriesName
        ) {
            hasChanges = true
            details.movieSeriesName
        } else {
            latest.movieSeriesName
        }

        val updatedEntry = latest.copy(
            coverImageUri = finalCoverUri ?: latest.coverImageUri,
            creator = newCreator,
            durationMinutes = newDurationMinutes,
            episodeLengthMinutes = newEpisodeLength,
            totalEpisodes = newTotalEpisodes,
            airedEpisodes = newAiredEpisodes,
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
            externalSource = finalExternalSource ?: latest.externalSource,
            relatedEntriesJson = newRelatedJson,
            movieSeriesName = newMovieSeriesName,
            originalLanguage = newOriginalLanguage,
            lastMetadataSyncAt = now,
            updatedAt = now
        )
        return updatedEntry to hasChanges
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
            SyncForegroundService.checkAndStop(ctx)
        }
    }
}

