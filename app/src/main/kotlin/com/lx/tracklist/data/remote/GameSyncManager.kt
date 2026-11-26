package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.gson.Gson
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameSyncProgress(
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

object GameSyncManager {
    private const val TAG = "GameSync"
    private const val RATE_LIMIT_DELAY_MS = 270L
    private val gson = Gson()

    private val _syncState = MutableStateFlow(GameSyncProgress())
    val syncState: StateFlow<GameSyncProgress> = _syncState.asStateFlow()

    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun isEligibleMediaForGame(mediaType: MediaType): Boolean {
        return mediaType == MediaType.GAME
    }

    fun isGameSource(entry: MediaEntry): Boolean {
        if (SyncRouter.jobFor(entry) != SyncJob.GAMES) return false
        val rawSrc = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim()
        val canonSrc = ExternalSources.canonical(rawSrc, entry.externalId ?: entry.foreignDatabaseId)
        return canonSrc == null || canonSrc == ExternalSources.IGDB || canonSrc == ExternalSources.RAWG
    }

    fun needsSync(entry: MediaEntry, forceRefresh: Boolean): Boolean {
        if (forceRefresh) return true

        val extId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()
        if (extId.isNullOrBlank()) return true

        val missingCover = entry.coverImageUri.isNullOrBlank() ||
                entry.coverImageUri.startsWith("file:") ||
                FailedCoverTracker.isEntryCoverBroken(entry)

        val missingPlatforms = entry.gamePlatforms.isNullOrBlank()
        val missingScore = entry.externalScore == null
        val missingDuration = entry.durationMinutes == null || entry.durationMinutes == 0
        val missingDates = entry.airedStartDate == null
        val missingDescription = (entry.description ?: entry.notes).isNullOrBlank()
        val missingRelated = entry.relatedEntriesJson.isNullOrBlank()

        return missingCover ||
                missingPlatforms ||
                missingScore ||
                missingDuration ||
                missingDates ||
                missingDescription ||
                missingRelated
    }

    suspend fun syncSingleEntry(
        context: Context,
        entry: MediaEntry,
        repository: MediaRepository,
        forceRefresh: Boolean = false
    ): Result<MediaEntry> {
        val extId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()
        val extSource = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { ExternalSources.canonical(it, extId) ?: it }
            ?: ExternalSources.IGDB
        val releaseYear = entry.airedStartDate?.let { MediaEntry.extractYearFromMillis(it) }

        val detailsResult = if (!extId.isNullOrBlank()) {
            GameMetadataManager.getGameDetails(
                context = context,
                externalId = extId,
                externalSource = extSource,
                title = entry.title,
                alternateTitle = entry.alternateTitle,
                releaseYear = releaseYear
            )
        } else {
            // Search by title
            val searchRes = GameMetadataManager.searchGames(context, entry.title, limit = 5)
            val candidates = searchRes.getOrNull().orEmpty()
            val match = candidates.firstOrNull {
                GameMetadataManager.isConfidentGameMatch(entry.title, it, entry.alternateTitle, releaseYear)
            } ?: candidates.firstOrNull()
            if (match != null) {
                GameMetadataManager.getGameDetails(
                    context = context,
                    externalId = match.externalId,
                    externalSource = match.externalSource,
                    title = match.title,
                    alternateTitle = match.alternateTitle,
                    releaseYear = match.releaseYear
                )
            } else {
                Result.failure(NoSuchElementException("No metadata match found for '${entry.title}'"))
            }
        }

        val details = detailsResult.getOrElse { return Result.failure(it) }

        val latest = repository.getEntryByIdSync(entry.id) ?: entry

        val relatedJson = if (details.relatedItems.isNotEmpty()) {
            gson.toJson(details.relatedItems)
        } else {
            latest.relatedEntriesJson
        }

        val isCoverBroken = FailedCoverTracker.isEntryCoverBroken(latest)
        val coverUri = when {
            forceRefresh && !details.coverUrl.isNullOrBlank() -> details.coverUrl
            forceRefresh && !details.steamAppId.isNullOrBlank() -> GameMetadataManager.getSteamCdnPosterUrl(details.steamAppId)
            !latest.coverImageUri.isNullOrBlank() && !latest.coverImageUri.startsWith("file:") && !isCoverBroken -> latest.coverImageUri
            !details.coverUrl.isNullOrBlank() -> details.coverUrl
            !details.steamAppId.isNullOrBlank() -> GameMetadataManager.getSteamCdnPosterUrl(details.steamAppId)
            else -> latest.coverImageUri
        }

        if (coverUri != latest.coverImageUri && !coverUri.isNullOrBlank()) {
            FailedCoverTracker.markSuccess(latest.id, coverUri)
            try {
                val loader = Coil.imageLoader(context)
                val req = ImageRequest.Builder(context)
                    .data(coverUri)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                loader.enqueue(req)
            } catch (_: Exception) {}
        }

        val platformsStr = if (details.platforms.isNotEmpty() && (latest.gamePlatforms.isNullOrBlank() || forceRefresh)) {
            details.platforms.joinToString(", ")
        } else {
            latest.gamePlatforms
        }

        val updated = applySyncedGameDetails(
            entry = latest,
            details = details,
            coverUri = coverUri,
            platformsStr = platformsStr,
            relatedJson = relatedJson,
            forceRefresh = forceRefresh
        )

        repository.updateMetadataOnly(updated)
        return Result.success(updated)
    }

    internal fun applySyncedGameDetails(
        entry: MediaEntry,
        details: GameDetails,
        coverUri: String? = details.coverUrl ?: entry.coverImageUri,
        platformsStr: String? = if (details.platforms.isNotEmpty() && (entry.gamePlatforms.isNullOrBlank())) details.platforms.joinToString(", ") else entry.gamePlatforms,
        relatedJson: String? = entry.relatedEntriesJson,
        forceRefresh: Boolean = false,
        now: Long = System.currentTimeMillis()
    ): MediaEntry {
        val existingId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()?.takeIf { it.isNotBlank() }
        val existingCanonical = ExternalSources.canonical(entry.externalSource, existingId)
        val detailsCanonical = ExternalSources.canonical(details.externalSource) ?: details.externalSource
        val keepExistingRef = existingId != null &&
            existingCanonical != null &&
            existingCanonical != detailsCanonical
        val resolvedExtId = if (keepExistingRef) existingId else details.externalId
        val resolvedExtSource = if (keepExistingRef) existingCanonical else detailsCanonical

        val canonicalRatingSource = details.ratingSource.takeIf { it.isNotBlank() }
            ?.let { ExternalSources.canonical(it) ?: it }

        return entry.copy(
            externalId = resolvedExtId,
            externalSource = resolvedExtSource,
            externalScore = details.rating ?: entry.externalScore,
            externalScoreSource = if (details.rating != null) canonicalRatingSource else entry.externalScoreSource,
            coverImageUri = coverUri,
            gamePlatforms = platformsStr,
            relatedEntriesJson = relatedJson,
            durationMinutes = if (entry.durationMinutes == null || entry.durationMinutes == 0 || forceRefresh) {
                details.completionTimeMinutes ?: entry.durationMinutes
            } else entry.durationMinutes,
            airedStartDate = if (forceRefresh && details.releaseDateMillis != null) details.releaseDateMillis else (entry.airedStartDate ?: details.releaseDateMillis),
            description = if (entry.description.isNullOrBlank() || forceRefresh) {
                details.buildDescriptionWithMetadata().takeIf { it.isNotBlank() } ?: entry.description
            } else entry.description,
            tags = if (entry.tags.isNullOrBlank() && details.genres.isNotEmpty()) details.genres.joinToString(", ") else entry.tags,
            lastMetadataSyncAt = now,
            updatedAt = now
        )
    }

    fun syncLibrary(
        context: Context,
        repository: MediaRepository,
        profileId: Long? = null,
        profileName: String? = null,
        forceRefresh: Boolean = false,
        targetEntryIds: Set<Long>? = null
    ) {
        if (_syncState.value.isRunning) return

        if (!GameApiConfig.isIgdbConfigured(context) && !GameApiConfig.isRawgConfigured(context)) {
            val missingMsg = "Neither IGDB nor RAWG credentials are configured. Add an API key in Settings → External Sync Jobs → API Keys."
            _syncState.value = GameSyncProgress(
                isCompleted = true,
                statusMessage = "Game API keys missing",
                lastErrorMessage = missingMsg
            )
            NotificationHelper.showGameSyncComplete(
                context = context,
                completed = 0,
                total = 0,
                updated = 0,
                failed = 0,
                lastError = missingMsg,
                isCancelled = false,
                profileName = profileName
            )
            return
        }

        syncJob = scope.launch {
            SyncForegroundService.start(context)
            try {
                val allEntries = if (profileId != null) {
                    repository.getAllEntriesForUserSync(profileId)
                } else {
                    repository.getAllEntriesSync()
                }

                val targetEntries = allEntries.filter {
                    isGameSource(it) && needsSync(it, forceRefresh) && (targetEntryIds == null || targetEntryIds.contains(it.id))
                }
                if (targetEntries.isEmpty()) {
                    val msg = "No game entries need syncing."
                    _syncState.value = GameSyncProgress(isCompleted = true, statusMessage = msg)
                    NotificationHelper.showGameSyncComplete(
                        context = context,
                        completed = 0,
                        total = 0,
                        updated = 0,
                        failed = 0,
                        lastError = null,
                        isCancelled = false,
                        profileName = profileName
                    )
                    return@launch
                }

                _syncState.value = GameSyncProgress(
                    isRunning = true,
                    profileId = profileId,
                    profileName = profileName,
                    totalCount = targetEntries.size,
                    pendingCount = targetEntries.size
                )

                var completed = 0
                var updated = 0
                var failed = 0

                for (entry in targetEntries) {
                    _syncState.update {
                        it.copy(
                            currentTitle = entry.title,
                            completedCount = completed,
                            pendingCount = targetEntries.size - completed
                        )
                    }

                    val res = syncSingleEntry(context, entry, repository, forceRefresh)
                    if (res.isSuccess) {
                        updated++
                    } else {
                        failed++
                    }

                    completed++
                    _syncState.update {
                        it.copy(
                            completedCount = completed,
                            updatedCount = updated,
                            failedCount = failed
                        )
                    }

                    delay(RATE_LIMIT_DELAY_MS)
                }

                _syncState.update {
                    it.copy(
                        isRunning = false,
                        isCompleted = true,
                        statusMessage = "Synced $updated games ($failed failed)"
                    )
                }

                NotificationHelper.showGameSyncComplete(
                    context = context,
                    completed = completed,
                    total = targetEntries.size,
                    updated = updated,
                    failed = failed,
                    lastError = null,
                    isCancelled = false,
                    profileName = profileName
                )
            } catch (e: CancellationException) {
                _syncState.update { it.copy(isRunning = false, statusMessage = "Sync canceled") }
                NotificationHelper.showGameSyncComplete(
                    context = context,
                    completed = _syncState.value.completedCount,
                    total = _syncState.value.totalCount,
                    updated = _syncState.value.updatedCount,
                    failed = _syncState.value.failedCount,
                    lastError = "Sync canceled",
                    isCancelled = true,
                    profileName = profileName
                )
            } catch (e: Exception) {
                Log.e(TAG, "Library game sync error", e)
                _syncState.update { it.copy(isRunning = false, lastErrorMessage = e.message) }
                NotificationHelper.showGameSyncComplete(
                    context = context,
                    completed = _syncState.value.completedCount,
                    total = _syncState.value.totalCount,
                    updated = _syncState.value.updatedCount,
                    failed = _syncState.value.failedCount,
                    lastError = e.message,
                    isCancelled = false,
                    profileName = profileName
                )
            } finally {
                SyncForegroundService.checkAndStop(context)
            }
        }
    }

    fun cancelSync(context: Context? = null) {
        syncJob?.cancel()
        syncJob = null
        _syncState.update { it.copy(isRunning = false, statusMessage = "Sync canceled") }
        if (context != null) {
            SyncForegroundService.checkAndStop(context)
        }
    }
}
