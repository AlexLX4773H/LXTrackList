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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MusicSyncProgress(
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

object MusicSyncManager {
    private const val TAG = "MusicSync"
    private const val RATE_LIMIT_DELAY_MS = 350L
    private val gson = Gson()

    private val _syncState = MutableStateFlow(MusicSyncProgress())
    val syncState: StateFlow<MusicSyncProgress> = _syncState.asStateFlow()

    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun isEligibleMediaForMusic(mediaType: MediaType): Boolean {
        return mediaType == MediaType.MUSIC
    }

    fun isMusicSource(entry: MediaEntry): Boolean {
        if (!isEligibleMediaForMusic(entry.mediaType)) return false
        val src = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim().orEmpty()
        return src.isBlank() ||
                src.equals("iTunes", ignoreCase = true) ||
                src.equals("Apple Music", ignoreCase = true) ||
                src.equals("MusicBrainz", ignoreCase = true)
    }

    fun needsSync(entry: MediaEntry, forceRefresh: Boolean): Boolean {
        if (forceRefresh) return true

        val extId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()
        if (extId.isNullOrBlank()) return true

        val missingCover = entry.coverImageUri.isNullOrBlank() ||
                entry.coverImageUri.startsWith("file:") ||
                FailedCoverTracker.isEntryCoverBroken(entry)

        val missingArtist = entry.creator.isNullOrBlank()
        val missingAlbum = entry.movieSeriesName.isNullOrBlank()
        val missingAlbumId = entry.musicAlbumId.isNullOrBlank()
        val missingTrackNumber = entry.movieSeriesNumber == null || entry.movieSeriesNumber <= 0
        val missingDuration = entry.durationMinutes == null || entry.durationMinutes <= 0
        val missingDates = entry.airedStartDate == null
        val missingTags = entry.tags.isNullOrBlank()
        val missingLanguage = entry.originalLanguage.isNullOrBlank()
        val missingRelated = entry.relatedEntriesJson.isNullOrBlank()

        return missingCover ||
                missingArtist ||
                missingAlbum ||
                missingAlbumId ||
                missingTrackNumber ||
                missingDuration ||
                missingDates ||
                missingTags ||
                missingLanguage ||
                missingRelated
    }

    suspend fun syncSingleEntry(
        context: Context,
        entry: MediaEntry,
        repository: MediaRepository,
        forceRefresh: Boolean = false
    ): Result<MediaEntry> {
        val matchResult = MusicMetadataManager.resolveMusicMetadataForSync(
            entry = entry,
            skipIdLookup = false
        )
        val match = matchResult.getOrElse { return Result.failure(it) }

        val effectiveAlbumName = match.albumName?.takeIf { it.isNotBlank() } ?: entry.movieSeriesName
        val effectiveAlbumId = match.musicAlbumId?.takeIf { it.isNotBlank() } ?: entry.musicAlbumId
        val effectiveExtId = match.externalId.takeIf { it.isNotBlank() } ?: entry.externalId
        val effectiveExtSource = match.externalSource.takeIf { it.isNotBlank() } ?: entry.externalSource

        val relatedRes = MusicMetadataManager.fetchRelatedAlbumTracks(
            title = entry.title.ifBlank { match.title },
            artist = match.artist.takeIf { it.isNotBlank() } ?: entry.creator,
            albumName = effectiveAlbumName,
            musicAlbumId = effectiveAlbumId,
            externalId = effectiveExtId,
            externalSource = effectiveExtSource
        )

        val latest = repository.getEntryByIdSync(entry.id) ?: entry

        val relatedJson = if (relatedRes.isSuccess) {
            val relatedItems = relatedRes.getOrDefault(emptyList()).filter { item ->
                !latest.matchesExternal(
                    candidateExternalId = item.externalId,
                    candidateExternalSource = item.externalSource,
                    candidateTitle = item.title,
                    candidateMediaType = item.mediaType,
                    candidateReleaseYear = item.releaseYear
                )
            }
            if (relatedItems.isNotEmpty() || forceRefresh || latest.relatedEntriesJson.isNullOrBlank()) {
                gson.toJson(relatedItems)
            } else {
                latest.relatedEntriesJson
            }
        } else {
            latest.relatedEntriesJson
        }

        val remoteCover = (match.coverUrl ?: match.albumCoverUrl)?.takeIf { it.isNotBlank() }
        val isCoverBroken = FailedCoverTracker.isEntryCoverBroken(latest)
        val coverUri = when {
            forceRefresh && !remoteCover.isNullOrBlank() -> remoteCover
            !latest.coverImageUri.isNullOrBlank() && !latest.coverImageUri.startsWith("file:") && !isCoverBroken -> latest.coverImageUri
            !remoteCover.isNullOrBlank() -> remoteCover
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

        val candidateTitle = match.title.trim()
        val currentAlts = (latest.alternateTitle?.split("\n") ?: emptyList()).map { it.trim() }.filter { it.isNotBlank() }
        val mergedAlts = if (candidateTitle.isNotBlank() && !candidateTitle.equals(latest.title, ignoreCase = true) && !currentAlts.any { it.equals(candidateTitle, ignoreCase = true) }) {
            currentAlts + candidateTitle
        } else {
            currentAlts
        }

        val remoteTags = match.genres.joinToString(", ").takeIf { it.isNotBlank() }
        val newTags = if (latest.tags.isNullOrBlank() || forceRefresh) {
            remoteTags ?: latest.tags
        } else {
            latest.tags
        }

        val newArtist = if (latest.creator.isNullOrBlank() || forceRefresh) {
            match.artist.takeIf { it.isNotBlank() } ?: latest.creator
        } else {
            latest.creator
        }

        val newAlbum = if (latest.movieSeriesName.isNullOrBlank() || forceRefresh) {
            effectiveAlbumName
        } else {
            latest.movieSeriesName
        }

        val newAlbumId = if (latest.musicAlbumId.isNullOrBlank() || forceRefresh) {
            effectiveAlbumId
        } else {
            latest.musicAlbumId
        }

        val newTrackNumber = if (latest.movieSeriesNumber == null || latest.movieSeriesNumber <= 0 || forceRefresh) {
            match.trackNumber ?: latest.movieSeriesNumber
        } else {
            latest.movieSeriesNumber
        }

        val newDuration = if (latest.durationMinutes == null || latest.durationMinutes <= 0 || forceRefresh) {
            match.durationSeconds ?: latest.durationMinutes
        } else {
            latest.durationMinutes
        }

        val newAiredDate = if (latest.airedStartDate == null || forceRefresh) {
            match.releaseDateMillis ?: latest.airedStartDate
        } else {
            latest.airedStartDate
        }

        val newLanguage = if (latest.originalLanguage.isNullOrBlank() || forceRefresh) {
            match.originalLanguage ?: latest.originalLanguage
        } else {
            latest.originalLanguage
        }

        val newDescription = if (latest.description.isNullOrBlank() && !newAlbum.isNullOrBlank()) {
            "Album: $newAlbum"
        } else {
            latest.description
        }

        val updated = latest.copy(
            title = latest.title,
            externalId = effectiveExtId,
            externalSource = effectiveExtSource,
            creator = newArtist,
            movieSeriesName = newAlbum,
            movieSeriesNumber = newTrackNumber,
            musicAlbumId = newAlbumId,
            coverImageUri = coverUri,
            durationMinutes = newDuration,
            tags = newTags,
            description = newDescription,
            airedStartDate = newAiredDate,
            airedEndDate = newAiredDate ?: latest.airedEndDate,
            originalLanguage = newLanguage,
            alternateTitle = mergedAlts.joinToString("\n").takeIf { it.isNotBlank() } ?: latest.alternateTitle,
            relatedEntriesJson = relatedJson,
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
        val effectiveProfileId = profileId ?: repository.activeUser.value.id
        val effectiveProfileName = profileName ?: repository.activeUser.value.name

        if (_syncState.value.isRunning) {
            if (_syncState.value.profileId == effectiveProfileId) {
                return
            } else {
                cancelSync(context)
            }
        }

        _syncState.value = MusicSyncProgress(
            isRunning = true,
            profileId = effectiveProfileId,
            profileName = effectiveProfileName,
            statusMessage = if (!forceRefresh) "Checking music covers & metadata..." else "Preparing music sync..."
        )

        syncJob = scope.launch {
            SyncForegroundService.start(context)
            NotificationHelper.initNotificationChannels(context)
            try {
                val allEntries = repository.getAllEntriesForUserSync(effectiveProfileId)
                val pool = if (targetEntryIds != null) {
                    allEntries.filter { targetEntryIds.contains(it.id) }
                } else {
                    allEntries
                }

                val musicEntries = pool.filter { isEligibleMediaForMusic(it.mediaType) && it.title.isNotBlank() }

                if (!forceRefresh && musicEntries.isNotEmpty()) {
                    _syncState.update { it.copy(statusMessage = "Checking cover images...") }
                    FailedCoverTracker.probeBrokenCovers(musicEntries)
                }

                val targetEntries = musicEntries.filter { needsSync(it, forceRefresh) }
                if (targetEntries.isEmpty()) {
                    val msg = if (musicEntries.isEmpty()) {
                        "No music entries found to sync."
                    } else {
                        "All ${musicEntries.size} music entries already have metadata & related tracks."
                    }
                    _syncState.value = MusicSyncProgress(
                        isRunning = false,
                        profileId = effectiveProfileId,
                        profileName = effectiveProfileName,
                        isCompleted = true,
                        statusMessage = msg
                    )
                    NotificationHelper.showMusicSyncComplete(
                        context = context,
                        completed = 0,
                        total = 0,
                        updated = 0,
                        failed = 0,
                        lastError = if (musicEntries.isEmpty()) msg else null,
                        isCancelled = false,
                        profileName = effectiveProfileName
                    )
                    return@launch
                }

                _syncState.value = MusicSyncProgress(
                    isRunning = true,
                    profileId = effectiveProfileId,
                    profileName = effectiveProfileName,
                    totalCount = targetEntries.size,
                    pendingCount = targetEntries.size,
                    statusMessage = if (forceRefresh) {
                        "Re-fetching all ${targetEntries.size} music tracks..."
                    } else {
                        "Syncing ${targetEntries.size} music tracks..."
                    }
                )

                var completed = 0
                var updated = 0
                var failed = 0
                var lastError: String? = null

                for (entry in targetEntries) {
                    if (syncJob?.isCancelled == true) break

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
                        lastError = res.exceptionOrNull()?.message
                    }

                    completed++
                    _syncState.update {
                        it.copy(
                            completedCount = completed,
                            pendingCount = targetEntries.size - completed,
                            updatedCount = updated,
                            failedCount = failed,
                            lastErrorMessage = lastError
                        )
                    }
                    NotificationHelper.showMusicSyncProgress(
                        context = context,
                        completed = completed,
                        total = targetEntries.size,
                        currentTitle = entry.title,
                        profileName = effectiveProfileName
                    )

                    if (completed < targetEntries.size) {
                        delay(RATE_LIMIT_DELAY_MS)
                    }
                }

                val isCancelled = syncJob?.isCancelled == true
                _syncState.update {
                    it.copy(
                        isRunning = false,
                        isCompleted = true,
                        statusMessage = when {
                            isCancelled -> "Sync cancelled ($completed/${targetEntries.size} processed, $updated updated)"
                            failed == 0 -> "Synced $updated music tracks"
                            else -> "Synced $updated tracks ($failed failed)"
                        }
                    )
                }

                NotificationHelper.showMusicSyncComplete(
                    context = context,
                    completed = completed,
                    total = targetEntries.size,
                    updated = updated,
                    failed = failed,
                    lastError = lastError,
                    isCancelled = isCancelled,
                    profileName = effectiveProfileName
                )
            } catch (e: CancellationException) {
                val snap = _syncState.value
                _syncState.update { it.copy(isRunning = false, isCompleted = true, statusMessage = "Sync cancelled") }
                NotificationHelper.showMusicSyncComplete(
                    context = context,
                    completed = snap.completedCount,
                    total = snap.totalCount,
                    updated = snap.updatedCount,
                    failed = snap.failedCount,
                    lastError = "Sync cancelled",
                    isCancelled = true,
                    profileName = effectiveProfileName
                )
            } catch (e: Exception) {
                Log.e(TAG, "Library music sync error", e)
                val snap = _syncState.value
                _syncState.update { it.copy(isRunning = false, isCompleted = true, lastErrorMessage = e.message) }
                NotificationHelper.showMusicSyncComplete(
                    context = context,
                    completed = snap.completedCount,
                    total = snap.totalCount,
                    updated = snap.updatedCount,
                    failed = snap.failedCount,
                    lastError = e.message,
                    isCancelled = false,
                    profileName = effectiveProfileName
                )
            } finally {
                SyncForegroundService.checkAndStop(context)
            }
        }
    }

    fun cancelSync(context: Context? = null) {
        val job = syncJob
        syncJob = null
        val wasActive = job?.isActive == true
        job?.cancel()
        val snap = _syncState.value
        _syncState.update { it.copy(isRunning = false, isCompleted = true, statusMessage = "Sync cancelled by user") }
        val ctx = context ?: LXApplication.instance
        if (wasActive) {
            NotificationHelper.showMusicSyncComplete(
                context = ctx,
                completed = snap.completedCount,
                total = snap.totalCount,
                updated = snap.updatedCount,
                failed = snap.failedCount,
                lastError = "Cancelled by user",
                isCancelled = true,
                profileName = snap.profileName
            )
        }
        SyncForegroundService.checkAndStop(ctx)
    }
}
