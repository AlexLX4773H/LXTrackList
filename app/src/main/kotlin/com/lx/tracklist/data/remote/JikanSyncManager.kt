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

object JikanSyncManager {

    private const val TAG = "JikanSync"
    private const val RATE_LIMIT_DELAY_MS = 600L
    private val gson = Gson()

    private val _syncState = MutableStateFlow(JikanSyncProgress())
    val syncState: StateFlow<JikanSyncProgress> = _syncState.asStateFlow()

    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun isEligibleMediaForMal(mediaType: MediaType): Boolean {
        return mediaType == MediaType.ANIME ||
               mediaType == MediaType.MANGA ||
               mediaType == MediaType.MANHWA ||
               mediaType == MediaType.BOOK
    }

    fun isTachiyomiSource(entry: MediaEntry): Boolean {
        val src = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim() ?: ""
        val extId = (entry.externalId ?: entry.foreignDatabaseId)?.trim() ?: ""
        return src.equals("TACHIYOMI", ignoreCase = true) ||
               src.contains("tachiyomi", ignoreCase = true) ||
               extId.startsWith("/manga/", ignoreCase = true) ||
               extId.startsWith("manga/", ignoreCase = true)
    }

    fun hasValidExternalId(entry: MediaEntry): Boolean {
        if (isTachiyomiSource(entry)) return false
        val src = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim() ?: ""
        val id = (entry.externalId ?: entry.foreignDatabaseId)?.trim() ?: return false
        if (id.isBlank() || id.startsWith("/")) return false
        if (src.equals("MAL", ignoreCase = true) || src.equals("MyAnimeList", ignoreCase = true) || src.equals("AniList", ignoreCase = true)) {
            return id.toLongOrNull() != null
        }
        if (src.equals("MangaDex", ignoreCase = true)) {
            return id.length >= 32
        }
        if (src.equals("Kitsu", ignoreCase = true)) {
            return id.isNotBlank()
        }
        return id.toLongOrNull() != null
    }

    fun extractMangaDexUuid(urlOrId: String): String? {
        val uuidRegex = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        return uuidRegex.find(urlOrId)?.value
    }

    fun extractNumericMalId(urlOrId: String): Long? {
        val trimmed = urlOrId.trim()
        trimmed.toLongOrNull()?.let { return it }
        val malPattern = Regex("(?:myanimelist\\.net/(?:manga|anime)/|/manga/|/anime/)(\\d+)")
        return malPattern.find(trimmed)?.groupValues?.get(1)?.toLongOrNull()
    }

    fun isMalSource(entry: MediaEntry): Boolean {
        if (isTachiyomiSource(entry)) return false
        val src = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim()
        val isSupportedSource = src.isNullOrBlank() ||
                src.equals("MAL", ignoreCase = true) ||
                src.equals("MyAnimeList", ignoreCase = true) ||
                src.equals("MangaDex", ignoreCase = true) ||
                src.equals("AniList", ignoreCase = true) ||
                src.equals("Kitsu", ignoreCase = true)
        val hasValidId = hasValidExternalId(entry)
        return isSupportedSource && (hasValidId || isEligibleMediaForMal(entry.mediaType))
    }

    fun isMangaType(mediaType: MediaType): Boolean {
        return mediaType == MediaType.MANGA || mediaType == MediaType.MANHWA || mediaType == MediaType.BOOK
    }

    private fun needsSync(entry: MediaEntry, forceRefresh: Boolean): Boolean {
        if (forceRefresh) return true
        if (isTachiyomiSource(entry)) return false

        val malId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()?.toLongOrNull()
        // If it doesn't have a MAL ID yet, it needs sync to fetch the ID and details based on title!
        if (malId == null) return true

        // Missing cover, leftover "file:" path, or broken image URL
        val missingCover = entry.coverImageUri.isNullOrBlank() ||
                           entry.coverImageUri.startsWith("file:") ||
                           FailedCoverTracker.isEntryCoverBroken(entry)

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
        val missingRelated = entry.relatedEntriesJson.isNullOrBlank()

        return missingCover ||
               missingRuntime ||
               missingEpisodes ||
               missingChapters ||
               missingVolumes ||
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

        syncJob = scope.launch {
            NotificationHelper.initNotificationChannels(context)

            // Find eligible entries strictly for this profile
            val allEntries = repository.getAllEntriesForUserSync(profileId)
            val entriesPool = if (targetEntryIds != null) {
                allEntries.filter { targetEntryIds.contains(it.id) }
            } else {
                allEntries
            }

            val malEntries = entriesPool.filter { isMalSource(it) && it.title.isNotBlank() }

            // Probe covers for broken image URLs so "Fetch Missing" picks them up
            if (!forceRefresh) {
                _syncState.update {
                    it.copy(
                        profileId = profileId,
                        profileName = profileName,
                        statusMessage = "Checking cover images..."
                    )
                }
                FailedCoverTracker.probeBrokenCovers(malEntries)
            }

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
                        "No eligible anime, manga, or manhwa entries found to sync."
                    } else {
                        "All ${malEntries.size} entries already have MAL IDs, posters & details."
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
                NotificationHelper.showJikanSyncComplete(
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
                "Starting sync for $total selected entries..."
            } else {
                "Starting sync for $total MAL entries..."
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

                val extIdStr = (entry.externalId ?: entry.foreignDatabaseId)?.trim()
                val isManga = isMangaType(entry.mediaType)
                val isManhwa = entry.mediaType == MediaType.MANHWA

                val isMangaDexSource = entry.externalSource?.equals("MangaDex", ignoreCase = true) == true
                val isAniListSource = entry.externalSource?.equals("AniList", ignoreCase = true) == true
                val mangaDexIdToUse = if (isMangaDexSource) extIdStr else null
                val aniListId = if (isAniListSource) extIdStr?.toLongOrNull()?.takeIf { it > 0 } else null
                val malId = if (!isMangaDexSource && !isAniListSource) extIdStr?.toLongOrNull() else null

                _syncState.update {
                    it.copy(
                        profileId = profileId,
                        profileName = profileName,
                        currentTitle = entry.title,
                        statusMessage = when {
                            isMangaDexSource && !mangaDexIdToUse.isNullOrBlank() ->
                                "Fetching ${entry.title} from MangaDex..."
                            aniListId != null ->
                                "Fetching ${entry.title} (AniList #$aniListId)..."
                            malId != null && malId > 0 ->
                                "Fetching ${entry.title} (#$malId)..."
                            else ->
                                "Searching MAL for ${entry.title}..."
                        }
                    )
                }

                NotificationHelper.showJikanSyncProgress(context, completed, total, entry.title, profileName = profileName)

                // Rate limiting delay
                delay(RATE_LIMIT_DELAY_MS)

                suspend fun fetchPrimary(): Result<JikanMediaDetails> = when {
                    isMangaDexSource && !mangaDexIdToUse.isNullOrBlank() ->
                        MangaDexApiClient.fetchMangaDetails(mangaDexIdToUse)
                    aniListId != null ->
                        JikanApiClient.queryAniListById(aniListId = aniListId, isManga = isManga)
                    malId != null && malId > 0 ->
                        JikanApiClient.fetchMediaDetails(malId = malId, isManga = isManga, title = entry.title, isManhwa = isManhwa)
                    else ->
                        JikanApiClient.searchMediaDetailsByTitle(title = entry.title, isManga = isManga, isManhwa = isManhwa)
                }

                // Attempt fetch by ID if present, otherwise search by title with fallback chain
                var usedFallbackResolution = false
                var detailsResult: Result<JikanMediaDetails> = fetchPrimary()

                if (detailsResult.isFailure && detailsResult.exceptionOrNull() is RateLimitException) {
                    delay(3000L)
                    detailsResult = fetchPrimary()
                }

                // If direct ID or primary title lookup failed, try Notes AniList ID and Alternate Names
                if (detailsResult.isFailure) {
                    if (malId != null || aniListId != null || !mangaDexIdToUse.isNullOrBlank()) {
                        val titleRes = JikanApiClient.searchMediaDetailsByTitle(
                            title = entry.title,
                            isManga = isManga,
                            isManhwa = isManhwa
                        )
                        if (titleRes.isSuccess) {
                            detailsResult = titleRes
                            usedFallbackResolution = true
                        }
                    }

                    if (detailsResult.isFailure) {
                        val notesAniIds = TachiyomiResolverManager.extractAniListIdsFromNotes(entry.notes, preferManga = isManga)
                            .filter { it != aniListId }
                        for (notesId in notesAniIds) {
                            val res = JikanApiClient.queryAniListById(aniListId = notesId, isManga = isManga)
                            if (res.isSuccess) {
                                detailsResult = res
                                usedFallbackResolution = true
                                break
                            }
                        }
                    }

                    if (detailsResult.isFailure) {
                        val altTitles = TachiyomiResolverManager.getCombinedAlternateTitles(entry)
                        for (alt in altTitles.take(5)) {
                            delay(250L)
                            val res = JikanApiClient.searchMediaDetailsByTitle(
                                title = alt,
                                isManga = isManga,
                                isManhwa = isManhwa
                            )
                            if (res.isSuccess) {
                                detailsResult = res
                                usedFallbackResolution = true
                                break
                            }
                        }
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

                        // Populate external ID if missing/invalid, or if fallback resolution replaced a dead ID
                        val needsNewExternalId = finalExternalId.isNullOrBlank() || !hasValidExternalId(entry) || usedFallbackResolution
                        if (needsNewExternalId) {
                            if (details.sourceProvider == "MangaDex") {
                                val mdId = details.url?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                                if (!mdId.isNullOrBlank()) {
                                    finalExternalId = mdId
                                    finalExternalSource = "MangaDex"
                                    hasChanges = true
                                }
                            } else if (details.sourceProvider.contains("AniList")) {
                                val resolvedId = details.malId?.toString() ?: details.url?.substringAfterLast('/')
                                if (!resolvedId.isNullOrBlank()) {
                                    finalExternalId = resolvedId
                                    finalExternalSource = if (details.malId != null && details.malId > 0) "MAL" else "AniList"
                                    hasChanges = true
                                }
                            } else if (details.sourceProvider.equals("Kitsu", ignoreCase = true)) {
                                val kitsuId = details.malId?.toString() ?: details.url?.substringAfterLast('/')
                                if (!kitsuId.isNullOrBlank()) {
                                    finalExternalId = kitsuId
                                    finalExternalSource = "Kitsu"
                                    hasChanges = true
                                }
                            } else if (details.malId != null && details.malId > 0) {
                                finalExternalId = details.malId.toString()
                                finalExternalSource = "MAL"
                                hasChanges = true
                            }
                        }

                        // Set poster URL if missing, leftover file: URI, broken image URL, or force refreshed
                        val isCoverBroken = FailedCoverTracker.isEntryCoverBroken(entry)
                        val needsPoster = entry.coverImageUri.isNullOrBlank() ||
                                          entry.coverImageUri.startsWith("file:") ||
                                          isCoverBroken ||
                                          forceRefresh
                        if (!details.posterUrl.isNullOrBlank() && needsPoster) {
                            finalCoverUri = details.posterUrl
                            hasChanges = true

                            FailedCoverTracker.markSuccess(entry.id, details.posterUrl)

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

                        // Extract related entries (prequels, sequels, side stories, etc.)
                        val newRelatedJson = if ((entry.relatedEntriesJson.isNullOrBlank() || forceRefresh) && finalExternalId != null) {
                            val targetMalId = finalExternalId.toLongOrNull()
                            if (targetMalId != null) {
                                val relRes = JikanApiClient.fetchRelatedEntries(targetMalId, isMangaType(entry.mediaType), title = entry.title)
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
                                userId = profileId,
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
                                relatedEntriesJson = newRelatedJson,
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

            NotificationHelper.showJikanSyncComplete(
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
            NotificationHelper.showJikanSyncComplete(
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
