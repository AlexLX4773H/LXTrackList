package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.repository.MediaRepository
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
        if (!isEligibleMediaForGame(entry.mediaType)) return false
        val src = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim().orEmpty()
        return src.equals("IGDB", ignoreCase = true) || src.equals("RAWG", ignoreCase = true) || src.isBlank()
    }

    suspend fun syncSingleEntry(
        context: Context,
        entry: MediaEntry,
        repository: MediaRepository
    ): Result<MediaEntry> {
        val extId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()
        val extSource = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim() ?: "IGDB"

        val detailsResult = if (!extId.isNullOrBlank()) {
            GameMetadataManager.getGameDetails(context, extId, extSource)
        } else {
            // Search by title
            val searchRes = GameMetadataManager.searchGames(context, entry.title, limit = 1)
            val match = searchRes.getOrNull()?.firstOrNull()
            if (match != null) {
                GameMetadataManager.getGameDetails(context, match.externalId, match.externalSource)
            } else {
                Result.failure(NoSuchElementException("No metadata match found for '${entry.title}'"))
            }
        }

        val details = detailsResult.getOrElse { return Result.failure(it) }

        val relatedJson = if (details.relatedItems.isNotEmpty()) {
            gson.toJson(details.relatedItems)
        } else {
            entry.relatedEntriesJson
        }

        val coverUri = when {
            !entry.coverImageUri.isNullOrBlank() && !entry.coverImageUri.startsWith("file:") -> entry.coverImageUri
            !details.coverUrl.isNullOrBlank() -> details.coverUrl
            !details.steamAppId.isNullOrBlank() -> GameMetadataManager.getSteamCdnPosterUrl(details.steamAppId)
            else -> entry.coverImageUri
        }

        val platformsStr = if (details.platforms.isNotEmpty() && entry.gamePlatforms.isNullOrBlank()) {
            details.platforms.joinToString(", ")
        } else {
            entry.gamePlatforms
        }

        val updated = entry.copy(
            externalId = details.externalId,
            externalSource = details.externalSource,
            externalScore = details.rating ?: entry.externalScore,
            externalScoreSource = details.ratingSource,
            coverImageUri = coverUri,
            gamePlatforms = platformsStr,
            relatedEntriesJson = relatedJson,
            durationMinutes = if (entry.durationMinutes == null || entry.durationMinutes == 0) {
                details.completionTimeMinutes ?: entry.durationMinutes
            } else entry.durationMinutes,
            airedStartDate = details.releaseDateMillis ?: entry.airedStartDate,
            description = if (entry.description.isNullOrBlank()) details.summary else entry.description,
            updatedAt = System.currentTimeMillis()
        )

        repository.update(updated)
        return Result.success(updated)
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

        syncJob = scope.launch {
            SyncForegroundService.start(context)
            try {
                val allEntries = if (profileId != null) {
                    repository.getAllEntriesForUserSync(profileId)
                } else {
                    repository.getAllEntriesSync()
                }

                val targetEntries = allEntries.filter {
                    isEligibleMediaForGame(it.mediaType) && (targetEntryIds == null || targetEntryIds.contains(it.id))
                }
                if (targetEntries.isEmpty()) {
                    _syncState.value = GameSyncProgress(isCompleted = true, statusMessage = "No game entries to sync.")
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

                    val res = syncSingleEntry(context, entry, repository)
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
            } catch (e: CancellationException) {
                _syncState.update { it.copy(isRunning = false, statusMessage = "Sync canceled") }
            } catch (e: Exception) {
                Log.e(TAG, "Library game sync error", e)
                _syncState.update { it.copy(isRunning = false, lastErrorMessage = e.message) }
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
