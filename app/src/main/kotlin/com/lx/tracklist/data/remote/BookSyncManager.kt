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
import com.lx.tracklist.data.model.TrackingUnit
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
import java.text.SimpleDateFormat
import java.util.Locale

private data class BookSyncItemResult(
    val hasChanges: Boolean,
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val isRateLimit: Boolean = false
)

data class BookSyncProgress(
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

object BookSyncManager {

    private const val TAG = "BookSync"
    private const val RATE_LIMIT_DELAY_MS = 250L
    private val gson = Gson()

    private val _syncState = MutableStateFlow(BookSyncProgress())
    val syncState: StateFlow<BookSyncProgress> = _syncState.asStateFlow()

    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun isEligibleMediaForBook(mediaType: MediaType): Boolean {
        return mediaType == MediaType.BOOK
    }

    fun isBookSource(entry: MediaEntry): Boolean {
        return SyncRouter.jobFor(entry) == SyncJob.BOOKS
    }

    private fun needsSync(entry: MediaEntry, forceRefresh: Boolean): Boolean {
        if (forceRefresh) return true

        val extId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()
        if (extId.isNullOrBlank()) return true

        val missingCover = entry.coverImageUri.isNullOrBlank() ||
                           entry.coverImageUri.startsWith("file:") ||
                           FailedCoverTracker.isEntryCoverBroken(entry)

        val missingPages = entry.totalPages == 0 && entry.totalVolumes == 0 && entry.totalChapters == 0
        val missingDates = entry.airedStartDate == null
        val missingAltTitles = entry.alternateTitle.isNullOrBlank()
        val missingTags = entry.tags.isNullOrBlank()
        val missingDescription = (entry.description ?: entry.notes).isNullOrBlank()
        val missingExternalScore = entry.externalScore == null
        val missingRelated = entry.relatedEntriesJson.isNullOrBlank()

        return missingCover ||
               missingPages ||
               missingDates ||
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

            val bookEntries = entriesPool.filter { isBookSource(it) && it.title.isNotBlank() }

            if (!forceRefresh) {
                _syncState.update {
                    it.copy(
                        profileId = profileId,
                        profileName = profileName,
                        statusMessage = "Checking cover images..."
                    )
                }
                FailedCoverTracker.probeBrokenCovers(bookEntries, context)
            }

            val eligible = if (targetEntryIds != null) bookEntries else bookEntries.filter { needsSync(it, forceRefresh) }
            val total = eligible.size

            if (total == 0) {
                val msg = if (targetEntryIds != null) {
                    if (entriesPool.isEmpty()) {
                        "Selected entries not found."
                    } else if (bookEntries.isEmpty()) {
                        "Selected entries are not books or novels."
                    } else {
                        "Selected book entries already have covers & details."
                    }
                } else {
                    if (bookEntries.isEmpty()) {
                        "No eligible book or novel entries found to sync."
                    } else {
                        "All ${bookEntries.size} book entries already have covers & details."
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
                        lastErrorMessage = if (bookEntries.isEmpty()) msg else null,
                        isCompleted = true
                    )
                }

                NotificationHelper.showBookSyncComplete(
                    context = context,
                    completed = 0,
                    total = 0,
                    updated = 0,
                    failed = 0,
                    lastError = if (bookEntries.isEmpty()) msg else null,
                    isCancelled = false,
                    profileName = profileName
                )
                SyncForegroundService.checkAndStop(context)
                return@launch
            }

            val startMsg = if (forceRefresh) {
                "Re-fetching all $total books & novels from Google Books / Open Library..."
            } else {
                "Starting sync for $total books & novels..."
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
                        statusMessage = "Syncing ${chunk.size} books/novels in parallel..."
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
                                    val remaining = total - completed
                                    _syncState.update {
                                        it.copy(
                                            completedCount = completed,
                                            pendingCount = remaining,
                                            updatedCount = updated,
                                            failedCount = failed,
                                            lastErrorMessage = lastError,
                                            currentTitle = entry.title,
                                            statusMessage = "Synced $completed / $total ($updated updated${if (failed > 0) ", $failed failed" else ""})"
                                        )
                                    }
                                    NotificationHelper.showBookSyncProgress(context, completed, total, entry.title, profileName = profileName)
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
                                    val remaining = total - completed
                                    _syncState.update {
                                        it.copy(
                                            completedCount = completed,
                                            pendingCount = remaining,
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
                    Log.w(TAG, "Rate limit hit. Reduced batch size to $currentBatchSize, delay to ${interBatchDelayMs}ms")
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



            val isCancelled = syncJob?.isCancelled == true
            _syncState.update {
                it.copy(
                    isRunning = false,
                    isCompleted = true,
                    statusMessage = when {
                        isCancelled -> "Sync cancelled ($completed/$total processed, $updated updated)"
                        failed == 0 -> "Sync complete! Updated $updated book entries."
                        else -> "Sync complete with $failed skipped/failed ($updated updated)."
                    }
                )
            }

            NotificationHelper.showBookSyncComplete(
                context = context,
                completed = completed,
                total = total,
                updated = updated,
                failed = failed,
                lastError = lastError,
                isCancelled = isCancelled,
                profileName = profileName
            )
            SyncForegroundService.checkAndStop(context)
        }
    }

    internal fun isAniListTitleMismatch(entry: MediaEntry, aniById: JikanMediaDetails?): Boolean {
        if (aniById == null) return true
        return !TachiyomiResolverManager.isConfidentExactMatch(
            queryTitle = entry.title,
            candidateTitle = aniById.title,
            candidateAltTitles = aniById.alternateTitle,
            entryAltTitles = entry.alternateTitle
        )
    }

    internal fun selectConfidentLightNovelMatch(
        entry: MediaEntry,
        candidates: List<JikanMediaDetails>
    ): JikanMediaDetails? {
        return candidates.firstOrNull { cand ->
            TachiyomiResolverManager.isConfidentExactMatch(
                queryTitle = entry.title,
                candidateTitle = cand.title,
                candidateAltTitles = cand.alternateTitle,
                entryAltTitles = entry.alternateTitle
            )
        }
    }

    /**
     * Updates tracking unit only when the entry has zero recorded progress across chapters, volumes,
     * and pages AND is still using the default unit ([TrackingUnit.CHAPTERS]).
     */
    internal fun resolveUpdatedBookTrackingUnit(entry: MediaEntry, bookDetails: BookDetails): TrackingUnit {
        val hasAnyProgress = entry.currentChapters > 0 || entry.currentVolumes > 0 || entry.currentPages > 0
        val isDefaultUnit = entry.trackingUnit == TrackingUnit.CHAPTERS
        if (hasAnyProgress || !isDefaultUnit) {
            return entry.trackingUnit
        }
        return when {
            entry.isLightNovel() && (bookDetails.totalChapters ?: 0) > 0 -> TrackingUnit.CHAPTERS
            (bookDetails.pageCount ?: 0) > 0 -> TrackingUnit.PAGES
            (bookDetails.totalVolumes ?: 0) > 0 -> TrackingUnit.VOLUMES
            (bookDetails.totalChapters ?: 0) > 0 -> TrackingUnit.CHAPTERS
            else -> entry.trackingUnit
        }
    }

    internal fun shouldAttemptBookTitleFallback(entry: MediaEntry, forceReLink: Boolean = false): Boolean {
        if (forceReLink) return true
        val extId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()
        if (extId.isNullOrBlank()) return true
        return !JikanSyncManager.hasValidExternalId(entry)
    }

    private suspend fun syncSingleEntry(
        context: Context,
        repository: MediaRepository,
        entry: MediaEntry,
        profileId: Long,
        forceRefresh: Boolean
    ): BookSyncItemResult {
        val extId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()
        val canonSrc = ExternalSources.canonical(entry.externalSource ?: entry.foreignDatabaseSource, extId)
        val isLightNovel = entry.isLightNovel()

        var bookDetails: BookDetails? = null
        var needsExternalRefRepair = false

        if (isLightNovel) {
            val existingAniNum = if (canonSrc == ExternalSources.ANILIST) extId?.toLongOrNull()?.takeIf { it > 0 } else null
            val existingMalNum = if (canonSrc == ExternalSources.MAL) extId?.toLongOrNull()?.takeIf { it > 0 } else null

            var directLnItem: JikanMediaDetails? = null
            if (existingMalNum != null) {
                directLnItem = JikanApiClient.fetchMediaDetails(malId = existingMalNum, isManga = true, title = entry.title).getOrNull()
            } else if (existingAniNum != null) {
                val aniCheck = JikanApiClient.queryAniListById(aniListId = existingAniNum, isManga = true)
                if (aniCheck.isSuccess) {
                    val aniById = aniCheck.getOrNull()
                    if (isAniListTitleMismatch(entry, aniById)) {
                        needsExternalRefRepair = true
                    } else {
                        directLnItem = aniById
                    }
                } else {
                    val msg = aniCheck.exceptionOrNull()?.message.orEmpty()
                    if (msg.contains("404") || msg.contains("Not Found", ignoreCase = true)) {
                        needsExternalRefRepair = true
                    }
                }
            }

            val lnItem = directLnItem ?: if (needsExternalRefRepair || shouldAttemptBookTitleFallback(entry)) {
                val lnResult = BookMetadataManager.searchLightNovels(entry.title, limit = 5)
                val candidates = lnResult.getOrNull().orEmpty()
                val confidentLn = selectConfidentLightNovelMatch(entry, candidates)
                if (needsExternalRefRepair) confidentLn else (confidentLn ?: candidates.firstOrNull())
            } else {
                null
            }

            if (lnItem != null) {
                val ref = lnItem.preferredExternalRef()
                bookDetails = BookDetails(
                    id = ref?.second ?: extId.orEmpty(),
                    title = lnItem.title ?: entry.title,
                    subtitle = lnItem.alternateTitle,
                    authors = lnItem.authors?.split(",")?.map { it.trim() } ?: emptyList(),
                    publishedDate = lnItem.airedStartDate?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(it))
                    },
                    description = lnItem.synopsis,
                    pageCount = lnItem.totalPages,
                    totalChapters = lnItem.totalChapters,
                    totalVolumes = lnItem.totalVolumes,
                    coverUrl = lnItem.posterUrl,
                    categories = lnItem.tags?.split(",")?.map { it.trim() } ?: emptyList(),
                    averageScore = lnItem.externalScore,
                    sourceProvider = ref?.first ?: (ExternalSources.canonical(lnItem.sourceProvider) ?: ExternalSources.ANILIST),
                    scoreSource = lnItem.externalScoreSource ?: ExternalSources.ANILIST
                )
            }
        }

        if (bookDetails == null && !isLightNovel) {
            val cleanExtId = extId?.removePrefix("/works/")?.removePrefix("/")?.trim()

            // 1. Direct ID lookup strictly scoped to the canonical source
            if (canonSrc == ExternalSources.GOOGLE_BOOKS && !cleanExtId.isNullOrBlank()) {
                bookDetails = GoogleBooksApiClient.fetchVolumeDetails(cleanExtId, context).getOrNull()
            } else if (canonSrc == ExternalSources.OPEN_LIBRARY && !cleanExtId.isNullOrBlank()) {
                bookDetails = OpenLibraryApiClient.fetchWorkDetails(cleanExtId).getOrNull()
            }

            // 2. Direct ID fallback only if source was unassigned
            if (bookDetails == null && canonSrc == null && !cleanExtId.isNullOrBlank()) {
                bookDetails = GoogleBooksApiClient.fetchVolumeDetails(cleanExtId, context).getOrNull()
                    ?: OpenLibraryApiClient.fetchWorkDetails(cleanExtId).getOrNull()
            }

            // 3. Fallback to ISBN / title ONLY when entry has no valid ID
            if (bookDetails == null && shouldAttemptBookTitleFallback(entry)) {
                val isbnRegex = Regex("(?i)ISBN:\\s*([0-9Xx-]+)")
                val isbn = (entry.description ?: entry.notes)?.let {
                    isbnRegex.find(it)?.groupValues?.get(1)?.replace("-", "")?.trim()
                }
                if (!isbn.isNullOrBlank()) {
                    coroutineScope {
                        val googleAsync = async { GoogleBooksApiClient.searchByIsbn(isbn, context).getOrNull() }
                        val openLibAsync = async { OpenLibraryApiClient.searchByIsbn(isbn).getOrNull() }
                        bookDetails = googleAsync.await() ?: openLibAsync.await()
                    }
                }

                if (bookDetails == null && entry.title.isNotBlank()) {
                    bookDetails = BookMetadataManager.searchBooks(entry.title, context, limit = 1).getOrNull()?.firstOrNull()
                }
            }
        }

        if (bookDetails != null) {
            val fetchedRelatedJson = if (entry.relatedEntriesJson.isNullOrBlank() || forceRefresh) {
                try {
                    val relatedRes = BookMetadataManager.fetchRelatedBooks(entry, context)
                    val relatedList = relatedRes.getOrNull()
                    if (!relatedList.isNullOrEmpty()) gson.toJson(relatedList) else null
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }

            // BUG-17: Re-read latest row right before merging so concurrent user edits survive sync.
            val latest = repository.getEntryByIdSync(entry.id) ?: entry
            val isCoverBroken = FailedCoverTracker.isEntryCoverBroken(latest)
            val (updatedEntry, hasChanges) = applyBookMetadataToFresh(
                latest = latest,
                bookDetails = bookDetails,
                needsExternalRefRepair = needsExternalRefRepair,
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
                repository.updateMetadataOnly(updatedEntry)
            }
            return BookSyncItemResult(hasChanges = hasChanges, isSuccess = true)
        }

        return BookSyncItemResult(
            hasChanges = false,
            isSuccess = false,
            errorMessage = "Could not find book metadata for '${entry.title}'"
        )
    }

    internal fun applyBookMetadataToFresh(
        latest: MediaEntry,
        bookDetails: BookDetails,
        needsExternalRefRepair: Boolean = false,
        isCoverBroken: Boolean = false,
        fetchedRelatedJson: String? = null,
        forceRefresh: Boolean = false,
        now: Long = System.currentTimeMillis()
    ): Pair<MediaEntry, Boolean> {
        var hasChanges = false
        var finalCoverUri: String? = latest.coverImageUri
        var finalExternalId: String? = latest.externalId
        val latestCanonicalSource = ExternalSources.canonical(latest.externalSource, latest.externalId)
        var finalExternalSource: String? = latestCanonicalSource ?: latest.externalSource
        if (latestCanonicalSource != null && latestCanonicalSource != latest.externalSource && !latest.externalId.isNullOrBlank()) {
            hasChanges = true
        }

        val preferredRef = bookDetails.preferredExternalRef()
        if ((latest.externalId.isNullOrBlank() || needsExternalRefRepair) && preferredRef != null) {
            if (preferredRef.first != latest.externalSource || preferredRef.second != latest.externalId) {
                finalExternalSource = preferredRef.first
                finalExternalId = preferredRef.second
                hasChanges = true
            }
        }

        val needsCover = latest.coverImageUri.isNullOrBlank() ||
            latest.coverImageUri.startsWith("file:") ||
            isCoverBroken ||
            forceRefresh
        if (!bookDetails.coverUrl.isNullOrBlank() && needsCover && bookDetails.coverUrl != latest.coverImageUri) {
            finalCoverUri = bookDetails.coverUrl
            hasChanges = true
        }

        val publishEpoch = bookDetails.publishedDate?.let { dateStr ->
            try {
                when (dateStr.length) {
                    4 -> SimpleDateFormat("yyyy", Locale.US).parse(dateStr)?.time
                    7 -> SimpleDateFormat("yyyy-MM", Locale.US).parse(dateStr)?.time
                    else -> SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)?.time
                }
            } catch (_: Exception) { null }
        }

        val newAiredStart = if ((latest.airedStartDate == null || forceRefresh) && publishEpoch != null && publishEpoch != latest.airedStartDate) {
            hasChanges = true
            publishEpoch
        } else {
            latest.airedStartDate
        }

        val newAiredEnd = if ((latest.airedEndDate == null || forceRefresh) && publishEpoch != null && publishEpoch != latest.airedEndDate) {
            hasChanges = true
            publishEpoch
        } else {
            latest.airedEndDate
        }

        val newAltTitle = if ((latest.alternateTitle.isNullOrBlank() || forceRefresh) && !bookDetails.subtitle.isNullOrBlank() && bookDetails.subtitle != latest.alternateTitle) {
            hasChanges = true
            bookDetails.subtitle
        } else {
            latest.alternateTitle
        }

        val tagsStr = bookDetails.buildTagsString()
        val newTags = if ((latest.tags.isNullOrBlank() || forceRefresh) && tagsStr.isNotBlank() && tagsStr != latest.tags) {
            hasChanges = true
            tagsStr
        } else {
            latest.tags
        }

        val curDesc = latest.description ?: latest.notes
        val descWithMeta = bookDetails.buildDescriptionWithMetadata()
        val newDescription = if ((curDesc.isNullOrBlank() || forceRefresh) && descWithMeta.isNotBlank() && descWithMeta != curDesc) {
            hasChanges = true
            descWithMeta
        } else {
            curDesc
        }

        val newScore = if ((latest.externalScore == null || forceRefresh) && bookDetails.averageScore != null && bookDetails.averageScore != latest.externalScore) {
            hasChanges = true
            bookDetails.averageScore
        } else {
            latest.externalScore
        }

        val resolvedScoreSource = (bookDetails.scoreSource ?: bookDetails.sourceProvider)
            .takeIf { it.isNotBlank() }
            ?.let { ExternalSources.canonical(it) ?: it }
        val newScoreSource = if (bookDetails.averageScore != null && resolvedScoreSource != null) {
            if (resolvedScoreSource != latest.externalScoreSource && newScore != null) hasChanges = true
            resolvedScoreSource
        } else {
            latest.externalScoreSource
        }

        val targetUnit = resolveUpdatedBookTrackingUnit(latest, bookDetails)
        if (targetUnit != latest.trackingUnit) {
            hasChanges = true
        }

        val newTotalPages = if ((latest.totalPages == 0 || forceRefresh) && (bookDetails.pageCount ?: 0) > 0 && (bookDetails.pageCount ?: 0) != latest.totalPages) {
            hasChanges = true
            bookDetails.pageCount ?: 0
        } else {
            latest.totalPages
        }

        val newTotalVolumes = if ((latest.totalVolumes == 0 || forceRefresh) && (bookDetails.totalVolumes ?: 0) > 0 && (bookDetails.totalVolumes ?: 0) != latest.totalVolumes) {
            hasChanges = true
            bookDetails.totalVolumes ?: 0
        } else {
            latest.totalVolumes
        }

        val newTotalChapters = if ((latest.totalChapters == 0 || forceRefresh) && (bookDetails.totalChapters ?: 0) > 0 && (bookDetails.totalChapters ?: 0) != latest.totalChapters) {
            hasChanges = true
            bookDetails.totalChapters ?: 0
        } else {
            latest.totalChapters
        }

        val relatedJson = if ((latest.relatedEntriesJson.isNullOrBlank() || forceRefresh) && !fetchedRelatedJson.isNullOrBlank() && fetchedRelatedJson != latest.relatedEntriesJson) {
            hasChanges = true
            fetchedRelatedJson
        } else {
            latest.relatedEntriesJson
        }

        val newOriginalLanguage = if (latest.originalLanguage.isNullOrBlank() && !bookDetails.language.isNullOrBlank()) {
            hasChanges = true
            bookDetails.language
        } else {
            latest.originalLanguage
        }

        val updatedEntry = latest.copy(
            coverImageUri = finalCoverUri,
            externalId = finalExternalId,
            externalSource = finalExternalSource,
            alternateTitle = newAltTitle,
            tags = newTags,
            description = newDescription,
            externalScore = newScore,
            externalScoreSource = newScoreSource,
            airedStartDate = newAiredStart,
            airedEndDate = newAiredEnd,
            totalPages = newTotalPages,
            totalVolumes = newTotalVolumes,
            totalChapters = newTotalChapters,
            trackingUnit = targetUnit,
            relatedEntriesJson = relatedJson,
            originalLanguage = newOriginalLanguage,
            lastMetadataSyncAt = now,
            updatedAt = now
        )
        return updatedEntry to hasChanges
    }

    fun cancelSync(context: Context? = null) {
        val job = syncJob ?: return
        if (job.isActive) {
            job.cancel()
            _syncState.update {
                it.copy(
                    isRunning = false,
                    isCompleted = true,
                    statusMessage = "Sync cancelled by user"
                )
            }
            val ctx = context ?: LXApplication.instance
            val state = _syncState.value
            NotificationHelper.showBookSyncComplete(
                context = ctx,
                completed = state.completedCount,
                total = state.totalCount,
                updated = state.updatedCount,
                failed = state.failedCount,
                lastError = "Cancelled by user",
                isCancelled = true,
                profileName = state.profileName
            )
            SyncForegroundService.checkAndStop(ctx)
        }
    }
}

