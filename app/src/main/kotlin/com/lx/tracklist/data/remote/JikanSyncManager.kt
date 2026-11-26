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
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private data class JikanSyncItemResult(
    val hasChanges: Boolean,
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val isRateLimit: Boolean = false
)

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
               mediaType == MediaType.MANHWA
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
        val rawId = (entry.externalId ?: entry.foreignDatabaseId)?.trim() ?: return false
        val isBook = src.equals("Google Books", ignoreCase = true) ||
                     src.equals("Open Library", ignoreCase = true) ||
                     entry.mediaType == MediaType.BOOK
        val id = if (src.equals("Open Library", ignoreCase = true) || isBook) {
            rawId.removePrefix("/works/").removePrefix("/").trim()
        } else {
            rawId
        }
        if (id.isBlank() || id.startsWith("/")) return false
        val canonSrc = ExternalSources.canonical(src, id)
        if (canonSrc == ExternalSources.MAL || canonSrc == ExternalSources.ANILIST) {
            return id.toLongOrNull() != null
        }
        if (canonSrc == ExternalSources.MANGADEX) {
            return id.length >= 32
        }
        if (canonSrc == ExternalSources.KITSU) {
            return id.isNotBlank()
        }
        if (canonSrc == ExternalSources.MUSICBRAINZ ||
            canonSrc == ExternalSources.APPLE_MUSIC ||
            entry.mediaType == MediaType.MUSIC
        ) {
            return id.isNotBlank()
        }
        if (isBook) {
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
        if (SyncRouter.jobFor(entry) != SyncJob.JIKAN) return false
        if (!isEligibleMediaForMal(entry.mediaType) && entry.mediaType != MediaType.MOVIE && entry.mediaType != MediaType.SHOW) {
            return false
        }

        val rawSrc = entry.externalSource ?: entry.foreignDatabaseSource
        val rawId = entry.externalId ?: entry.foreignDatabaseId
        val canonicalSrc = ExternalSources.canonical(rawSrc, rawId)

        if (entry.mediaType == MediaType.MOVIE || entry.mediaType == MediaType.SHOW) {
            return canonicalSrc == ExternalSources.MAL || canonicalSrc == ExternalSources.ANILIST
        }

        return canonicalSrc == null ||
            canonicalSrc == ExternalSources.MAL ||
            canonicalSrc == ExternalSources.MANGADEX ||
            canonicalSrc == ExternalSources.ANILIST ||
            canonicalSrc == ExternalSources.KITSU
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
        val missingDescription = (entry.description ?: entry.notes).isNullOrBlank()
        val missingExternalScore = entry.externalScore == null
        val missingRelated = entry.relatedEntriesJson.isNullOrBlank()
        val missingCreator = entry.creator.isNullOrBlank()
        val isAiring = entry.isAiring

        return missingCover ||
               missingRuntime ||
               missingEpisodes ||
               missingChapters ||
               missingVolumes ||
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
                FailedCoverTracker.probeBrokenCovers(malEntries, context)
            }

            val eligible = if (targetEntryIds != null) malEntries else malEntries.filter { needsSync(it, forceRefresh) }

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
                SyncForegroundService.checkAndStop(context)
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

            var currentBatchSize = 2
            val minBatchSize = 1
            val maxBatchSize = 3
            var interBatchDelayMs = 350L
            var consecutiveCleanBatches = 0
            val mutex = Mutex()
            val activeTitles = ConcurrentHashMap.newKeySet<String>()

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
                        statusMessage = "Syncing ${chunk.size} items in parallel..."
                    )
                }

                var hadRateLimit = false

                coroutineScope {
                    chunk.mapIndexed { chunkIdx, entry ->
                        async {
                            if (syncJob?.isCancelled == true) return@async
                            if (chunkIdx > 0) delay(chunkIdx * 250L)
                            activeTitles.add(entry.title)
                            try {
                                val outcome = syncSingleEntry(
                                    context = context,
                                    repository = repository,
                                    entry = entry,
                                    profileId = profileId,
                                    forceRefresh = forceRefresh
                                )
                                activeTitles.remove(entry.title)
                                val currentFetching = activeTitles.firstOrNull() ?: entry.title
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
                                            currentTitle = currentFetching,
                                            statusMessage = "Synced $completed / $total ($updated updated${if (failed > 0) ", $failed failed" else ""})"
                                        )
                                    }
                                    NotificationHelper.showJikanSyncProgress(context, completed, total, currentFetching, profileName = profileName)
                                }
                            } catch (e: CancellationException) {
                                activeTitles.remove(entry.title)
                                throw e
                            } catch (e: Exception) {
                                activeTitles.remove(entry.title)
                                val currentFetching = activeTitles.firstOrNull() ?: entry.title
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
                                            currentTitle = currentFetching
                                        )
                                    }
                                    NotificationHelper.showJikanSyncProgress(context, completed, total, currentFetching, profileName = profileName)
                                }
                            }
                        }
                    }.awaitAll()
                }

                index += chunk.size

                if (hadRateLimit) {
                    currentBatchSize = (currentBatchSize - 1).coerceAtLeast(minBatchSize)
                    interBatchDelayMs = (interBatchDelayMs + 300L).coerceAtMost(1500L)
                    consecutiveCleanBatches = 0
                    Log.w(TAG, "Rate limit hit during batch. Reduced batch size to $currentBatchSize, delay to ${interBatchDelayMs}ms")
                    delay(1500L)
                } else {
                    consecutiveCleanBatches++
                    if (consecutiveCleanBatches >= 2 && currentBatchSize < maxBatchSize) {
                        currentBatchSize++
                        interBatchDelayMs = (interBatchDelayMs - 50L).coerceAtLeast(100L)
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
            SyncForegroundService.checkAndStop(context)
        }
    }

    internal enum class JikanLookupKind {
        MANGADEX_ID,
        ANILIST_ID,
        KITSU_ID,
        MAL_ID,
        TITLE_SEARCH
    }

    internal data class JikanLookupPlan(
        val kind: JikanLookupKind,
        val idOrTitle: String
    )

    /**
     * Resolves the authoritative primary lookup strategy from the entry's stored external ID/source
     * so linked Anime/Manga/Manhwa always query by their stored ID before any title fallback.
     */
    internal fun resolveStoredLookupPlan(entry: MediaEntry): JikanLookupPlan {
        val extIdStr = (entry.externalId ?: entry.foreignDatabaseId)?.trim().orEmpty()
        val extSrcRaw = entry.externalSource ?: entry.foreignDatabaseSource
        val canonicalSrc = ExternalSources.canonical(extSrcRaw, extIdStr)
        val detectedMdUuid = extIdStr.takeIf { it.isNotBlank() }?.let { extractMangaDexUuid(it) }

        if ((canonicalSrc == ExternalSources.MANGADEX && extIdStr.isNotBlank()) || detectedMdUuid != null) {
            return JikanLookupPlan(JikanLookupKind.MANGADEX_ID, detectedMdUuid ?: extIdStr)
        }
        if (canonicalSrc == ExternalSources.ANILIST) {
            val aniId = extIdStr.toLongOrNull()?.takeIf { it > 0L }
            if (aniId != null) {
                return JikanLookupPlan(JikanLookupKind.ANILIST_ID, aniId.toString())
            }
        }
        if (canonicalSrc == ExternalSources.KITSU && extIdStr.isNotBlank()) {
            return JikanLookupPlan(JikanLookupKind.KITSU_ID, extIdStr)
        }
        if (canonicalSrc == null || canonicalSrc == ExternalSources.MAL) {
            val malId = extIdStr.toLongOrNull()?.takeIf { it > 0L }
            if (malId != null) {
                return JikanLookupPlan(JikanLookupKind.MAL_ID, malId.toString())
            }
        }
        return JikanLookupPlan(JikanLookupKind.TITLE_SEARCH, entry.title)
    }

    private suspend fun syncSingleEntry(
        context: Context,
        repository: MediaRepository,
        entry: MediaEntry,
        profileId: Long,
        forceRefresh: Boolean
    ): JikanSyncItemResult {
        val isManga = isMangaType(entry.mediaType)
        val isManhwa = entry.mediaType == MediaType.MANHWA
        val plan = resolveStoredLookupPlan(entry)
        val aniListId = if (plan.kind == JikanLookupKind.ANILIST_ID) plan.idOrTitle.toLongOrNull() else null
        val malId = if (plan.kind == JikanLookupKind.MAL_ID) plan.idOrTitle.toLongOrNull() else null

        suspend fun fetchPrimary(): Result<JikanMediaDetails> = when (plan.kind) {
            JikanLookupKind.MANGADEX_ID ->
                MangaDexApiClient.fetchMangaDetails(plan.idOrTitle)
            JikanLookupKind.ANILIST_ID ->
                JikanApiClient.queryAniListById(aniListId = plan.idOrTitle.toLong(), isManga = isManga)
            JikanLookupKind.KITSU_ID ->
                KitsuApiClient.fetchDetailsById(plan.idOrTitle, isAnime = !isManga)
            JikanLookupKind.MAL_ID -> {
                val id = plan.idOrTitle.toLong()
                if (entry.isAiring && !isManga) {
                    val aniRes = JikanApiClient.queryAniList(malId = id, isManga = false)
                    if (aniRes.isSuccess) aniRes else JikanApiClient.fetchMediaDetails(malId = id, isManga = false, title = entry.title, isManhwa = false)
                } else {
                    JikanApiClient.fetchMediaDetails(malId = id, isManga = isManga, title = entry.title, isManhwa = isManhwa)
                }
            }
            JikanLookupKind.TITLE_SEARCH ->
                JikanApiClient.searchMediaDetailsByTitle(title = entry.title, isManga = isManga, isManhwa = isManhwa)
        }

        var usedFallbackResolution = false
        var detailsResult: Result<JikanMediaDetails> = fetchPrimary()

        if (detailsResult.isFailure && (detailsResult.exceptionOrNull() is RateLimitException || detailsResult.exceptionOrNull()?.message?.contains("429") == true)) {
            delay(1500L)
            detailsResult = fetchPrimary()
        }

        if (detailsResult.isFailure && plan.kind != JikanLookupKind.TITLE_SEARCH) {
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
            val descAniIds = TachiyomiResolverManager.extractAniListIdsFromDescription(entry.description ?: entry.notes, preferManga = isManga)
                .filter { it != aniListId }
            for (descId in descAniIds) {
                val res = JikanApiClient.queryAniListById(aniListId = descId, isManga = isManga)
                if (res.isSuccess) {
                    detailsResult = res
                    usedFallbackResolution = true
                    break
                }
            }
        }

        if (detailsResult.isFailure) {
            val altTitles = TachiyomiResolverManager.getCombinedAlternateTitles(entry)
            for (alt in altTitles.take(3)) {
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

        if (detailsResult.isSuccess) {
            val details = detailsResult.getOrNull()
            try {
                Log.d(TAG, "Successfully resolved entry '${entry.title}' (malId=${details?.malId}): poster=${details?.posterUrl}, duration=${details?.durationMinutes}, provider=${details?.sourceProvider}")
            } catch (_: RuntimeException) {}
            if (details != null) {
                var candidateExtId: String? = entry.externalId
                var candidateExtSource: String? = entry.externalSource
                val needsInitialExternalId = candidateExtId.isNullOrBlank() || !hasValidExternalId(entry)
                if (needsInitialExternalId) {
                    val ref = details.preferredExternalRef()
                    if (ref != null) {
                        candidateExtSource = ref.first
                        candidateExtId = ref.second
                    } else if (ExternalSources.canonical(details.sourceProvider) == ExternalSources.MANGADEX) {
                        val mdId = details.url?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                        if (!mdId.isNullOrBlank()) {
                            candidateExtId = mdId
                            candidateExtSource = ExternalSources.MANGADEX
                        }
                    }
                }

                val fetchedRelatedJson = if ((entry.relatedEntriesJson.isNullOrBlank() || forceRefresh) && candidateExtId != null) {
                    if (!details.relatedMedia.isNullOrEmpty()) {
                        gson.toJson(details.relatedMedia)
                    } else {
                        val targetId = candidateExtId.toLongOrNull()
                        if (targetId != null || !candidateExtSource.isNullOrBlank()) {
                            val relRes = JikanApiClient.fetchRelatedEntries(
                                malId = targetId,
                                isManga = isMangaType(entry.mediaType),
                                title = entry.title,
                                externalSource = candidateExtSource,
                                externalId = candidateExtId
                            )
                            val relList = relRes.getOrNull()
                            if (!relList.isNullOrEmpty()) gson.toJson(relList) else null
                        } else null
                    }
                } else {
                    null
                }

                // BUG-17: Re-read latest row right before merging so concurrent user edits survive sync.
                val latest = repository.getEntryByIdSync(entry.id) ?: entry
                val isCoverBroken = FailedCoverTracker.isEntryCoverBroken(latest)
                val (updatedEntry, hasChanges) = applyJikanMetadataToFresh(
                    latest = latest,
                    details = details,
                    usedFallbackResolution = usedFallbackResolution,
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
                return JikanSyncItemResult(hasChanges = hasChanges, isSuccess = true)
            }
        }

        val err = detailsResult.exceptionOrNull()?.message ?: "Unknown error"
        try {
            Log.e(TAG, "Failed to sync entry ${entry.title} (malId=$malId): $err", detailsResult.exceptionOrNull())
        } catch (_: RuntimeException) {}
        val isRateLimit = detailsResult.exceptionOrNull() is RateLimitException || err.contains("429", ignoreCase = true)
        return JikanSyncItemResult(hasChanges = false, isSuccess = false, errorMessage = err, isRateLimit = isRateLimit)
    }

    internal fun applyJikanMetadataToFresh(
        latest: MediaEntry,
        details: JikanMediaDetails,
        usedFallbackResolution: Boolean = false,
        isCoverBroken: Boolean = false,
        fetchedRelatedJson: String? = null,
        forceRefresh: Boolean = false,
        now: Long = System.currentTimeMillis()
    ): Pair<MediaEntry, Boolean> {
        var hasChanges = false
        var finalCoverUri: String? = latest.coverImageUri
        var finalExternalId: String? = latest.externalId
        var finalExternalSource: String? = latest.externalSource
        val isManga = isMangaType(latest.mediaType)

        // JOB-13: Never overwrite a valid stored external ID/source even when fallback details are used.
        val needsNewExternalId = finalExternalId.isNullOrBlank() || !hasValidExternalId(latest)
        if (needsNewExternalId) {
            val ref = details.preferredExternalRef()
            if (ref != null) {
                if (ref.first != latest.externalSource || ref.second != latest.externalId) {
                    finalExternalSource = ref.first
                    finalExternalId = ref.second
                    hasChanges = true
                }
            } else if (ExternalSources.canonical(details.sourceProvider) == ExternalSources.MANGADEX) {
                val mdId = details.url?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                if (!mdId.isNullOrBlank() && (mdId != latest.externalId || ExternalSources.MANGADEX != latest.externalSource)) {
                    finalExternalId = mdId
                    finalExternalSource = ExternalSources.MANGADEX
                    hasChanges = true
                }
            }
        } else {
            val canonicalLatestSource = ExternalSources.canonical(latest.externalSource, finalExternalId)
            if (canonicalLatestSource != null && canonicalLatestSource != latest.externalSource) {
                finalExternalSource = canonicalLatestSource
                hasChanges = true
            }
        }

        val needsPoster = latest.coverImageUri.isNullOrBlank() ||
            latest.coverImageUri.startsWith("file:") ||
            isCoverBroken ||
            forceRefresh
        if (!details.posterUrl.isNullOrBlank() && needsPoster && details.posterUrl != latest.coverImageUri) {
            finalCoverUri = details.posterUrl
            hasChanges = true
        }

        val newEpisodeLength = if (latest.mediaType != MediaType.MOVIE && !isManga) {
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

        val newDurationMinutes = if (latest.mediaType == MediaType.MOVIE) {
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

        val newTotalEpisodes = if (!isManga && latest.mediaType != MediaType.MOVIE) {
            if (details.totalEpisodes != null && details.totalEpisodes > 0 &&
                (latest.totalEpisodes == 0 || forceRefresh) &&
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

        val isEntryAiring = details.isAiring ?: latest.isAiring

        val newTotalChapters = if (isManga) {
            if (details.totalChapters != null && details.totalChapters > 0 &&
                (latest.totalChapters == 0 || forceRefresh || isEntryAiring) &&
                details.totalChapters != latest.totalChapters
            ) {
                hasChanges = true
                details.totalChapters
            } else {
                latest.totalChapters
            }
        } else {
            latest.totalChapters
        }

        val newAiredChapters = if (isManga) {
            val cand = details.airedChapters ?: if (isEntryAiring && details.totalChapters != null && details.totalChapters > 0) details.totalChapters else null
            if (cand != null && cand > 0 && cand != latest.airedChapters) {
                hasChanges = true
                cand
            } else {
                latest.airedChapters
            }
        } else {
            latest.airedChapters
        }

        val newTotalVolumes = if (isManga) {
            if (details.totalVolumes != null && details.totalVolumes > 0 &&
                (latest.totalVolumes == 0 || forceRefresh) &&
                details.totalVolumes != latest.totalVolumes
            ) {
                hasChanges = true
                details.totalVolumes
            } else {
                latest.totalVolumes
            }
        } else {
            latest.totalVolumes
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
            (latest.alternateTitle.isNullOrBlank() || forceRefresh) &&
            details.alternateTitle != latest.alternateTitle
        ) {
            hasChanges = true
            details.alternateTitle
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
        val newDescription = if (!details.synopsis.isNullOrBlank() &&
            (curDesc.isNullOrBlank() || forceRefresh) &&
            details.synopsis != curDesc
        ) {
            hasChanges = true
            details.synopsis
        } else {
            curDesc
        }

        val newIsAiring = if (details.isAiring != null && details.isAiring != latest.isAiring) {
            hasChanges = true
            details.isAiring
        } else {
            latest.isAiring
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

        val canonicalCandidateScoreSource = details.externalScoreSource?.let { ExternalSources.canonical(it) ?: it }
            ?: ExternalSources.MAL
        val newExternalScoreSource = if (details.externalScore != null &&
            (latest.externalScore == null || forceRefresh) &&
            canonicalCandidateScoreSource != latest.externalScoreSource
        ) {
            hasChanges = true
            canonicalCandidateScoreSource
        } else {
            latest.externalScoreSource
        }

        val candidateRelatedJson = when {
            !details.relatedMedia.isNullOrEmpty() -> gson.toJson(details.relatedMedia)
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

        val newAiredEpisodes = if (details.airedEpisodes != null && details.airedEpisodes > 0) {
            if (details.airedEpisodes != latest.airedEpisodes) hasChanges = true
            details.airedEpisodes
        } else if (latest.mediaType == MediaType.ANIME && (details.isAiring ?: latest.isAiring)) {
            val effective = latest.copy(
                airedStartDate = newAiredStartDate,
                totalEpisodes = newTotalEpisodes,
                isAiring = newIsAiring
            ).getEffectiveAiredEpisodes()
            if (effective > 0 && effective != latest.airedEpisodes) {
                hasChanges = true
                effective
            } else latest.airedEpisodes
        } else latest.airedEpisodes

        val newBroadcastTime = if (!details.broadcastTime.isNullOrBlank()) {
            if (details.broadcastTime != latest.broadcastTime) hasChanges = true
            details.broadcastTime
        } else latest.broadcastTime

        val newNextAiringEpoch = if (details.nextAiringEpoch != null) {
            if (details.nextAiringEpoch != latest.nextAiringEpoch) hasChanges = true
            details.nextAiringEpoch
        } else latest.nextAiringEpoch

        val newOriginalLanguage = if (latest.originalLanguage.isNullOrBlank() && !details.originalLanguage.isNullOrBlank()) {
            hasChanges = true
            details.originalLanguage
        } else latest.originalLanguage

        val newCreator = if (!details.authors.isNullOrBlank() && (latest.creator.isNullOrBlank() || forceRefresh) && details.authors != latest.creator) {
            hasChanges = true
            details.authors
        } else latest.creator

        val updatedEntry = latest.copy(
            creator = newCreator,
            coverImageUri = finalCoverUri ?: latest.coverImageUri,
            episodeLengthMinutes = newEpisodeLength,
            durationMinutes = newDurationMinutes,
            totalEpisodes = newTotalEpisodes,
            airedEpisodes = newAiredEpisodes,
            airedChapters = newAiredChapters,
            broadcastTime = newBroadcastTime,
            nextAiringEpoch = newNextAiringEpoch,
            totalChapters = newTotalChapters,
            totalVolumes = newTotalVolumes,
            airedStartDate = newAiredStartDate,
            airedEndDate = newAiredEndDate,
            alternateTitle = newAlternateTitle,
            tags = newTags,
            description = newDescription,
            isAiring = newIsAiring,
            externalScore = newExternalScore,
            externalScoreSource = newExternalScoreSource?.let { ExternalSources.canonical(it) ?: it },
            externalId = finalExternalId,
            externalSource = (finalExternalSource ?: latest.externalSource)?.let { ExternalSources.canonical(it, finalExternalId) ?: it },
            relatedEntriesJson = newRelatedJson,
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
            SyncForegroundService.checkAndStop(ctx)
        }
    }
}
