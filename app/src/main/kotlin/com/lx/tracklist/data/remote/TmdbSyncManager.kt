package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.gson.Gson
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

        val missingCover = entry.coverImageUri.isNullOrBlank() || entry.coverImageUri.startsWith("file:")
        val isMovie = entry.mediaType == MediaType.MOVIE
        val missingRuntime = if (isMovie) {
            entry.durationMinutes == null || entry.durationMinutes == 0
        } else {
            entry.episodeLengthMinutes == null || entry.episodeLengthMinutes == 0
        }

        val missingEpisodes = !isMovie && entry.totalEpisodes == 0
        val missingSeasons = !isMovie && entry.totalSeasons <= 1
        val missingAiredDates = entry.airedStartDate == null
        val missingAltTitles = entry.alternateTitle.isNullOrBlank()
        val missingTags = entry.tags.isNullOrBlank()
        val missingNotes = entry.notes.isNullOrBlank()
        val missingExternalScore = entry.externalScore == null
        val missingRelated = entry.relatedEntriesJson.isNullOrBlank()

        return missingCover ||
                missingRuntime ||
                missingEpisodes ||
                missingSeasons ||
                missingAiredDates ||
                missingAltTitles ||
                missingTags ||
                missingNotes ||
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

            for (entry in eligible) {
                if (syncJob?.isCancelled == true) break

                val tmdbId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()?.toLongOrNull()
                val isTv = entry.mediaType == MediaType.SHOW

                _syncState.update {
                    it.copy(
                        profileId = profileId,
                        profileName = profileName,
                        currentTitle = entry.title,
                        statusMessage = if (tmdbId != null) {
                            "Fetching ${entry.title} (TMDB #$tmdbId)..."
                        } else {
                            "Searching TMDB for ${entry.title}..."
                        }
                    )
                }

                NotificationHelper.showTmdbSyncProgress(context, completed, total, entry.title, profileName = profileName)

                // Rate limiting delay
                delay(RATE_LIMIT_DELAY_MS)

                var detailsResult = if (tmdbId != null) {
                    TmdbApiClient.fetchMediaDetails(tmdbId = tmdbId, isTv = isTv, context = context)
                } else {
                    TmdbApiClient.searchMediaDetailsByTitle(title = entry.title, isTv = isTv, context = context)
                }

                if (detailsResult.isFailure && detailsResult.exceptionOrNull() is TmdbRateLimitException) {
                    delay(2000L)
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

                        val needsPoster = entry.coverImageUri.isNullOrBlank() || entry.coverImageUri.startsWith("file:") || forceRefresh
                        if (!details.posterUrl.isNullOrBlank() && needsPoster) {
                            finalCoverUri = details.posterUrl
                            hasChanges = true

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

                        // Notes (synopsis)
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
                                relatedEntriesJson = newRelatedJson,
                                updatedAt = System.currentTimeMillis()
                            )
                            repository.update(updatedEntry.copy(userId = profileId))
                            updated++
                        }
                    }
                } else {
                    val err = detailsResult.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "Failed to sync entry ${entry.title} (tmdbId=$tmdbId): $err", detailsResult.exceptionOrNull())
                    lastError = err
                    failed++
                }

                completed++
                val pending = total - completed
                _syncState.update {
                    it.copy(
                        profileId = profileId,
                        profileName = profileName,
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

