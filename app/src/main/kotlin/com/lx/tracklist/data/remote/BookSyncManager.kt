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
        if (isEligibleMediaForBook(entry.mediaType)) return true
        val src = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim() ?: ""
        return src.equals("Google Books", ignoreCase = true) ||
               src.equals("Open Library", ignoreCase = true)
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
                FailedCoverTracker.probeBrokenCovers(bookEntries)
            }

            val eligible = bookEntries.filter { needsSync(it, forceRefresh) }
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
        }
    }

    private suspend fun syncSingleEntry(
        context: Context,
        repository: MediaRepository,
        entry: MediaEntry,
        profileId: Long,
        forceRefresh: Boolean
    ): BookSyncItemResult {
        val extId = (entry.externalId ?: entry.foreignDatabaseId)?.trim()
        val isLightNovel = entry.externalSource.equals("AniList", ignoreCase = true) ||
                entry.customCategory.equals("Light Novels", ignoreCase = true) ||
                entry.subCategory.equals("Light Novel", ignoreCase = true)

        var bookDetails: BookDetails? = null

        if (isLightNovel) {
            val lnResult = BookMetadataManager.searchLightNovels(entry.title, limit = 5)
            val lnItem = lnResult.getOrNull()?.firstOrNull()
            if (lnItem != null) {
                bookDetails = BookDetails(
                    id = lnItem.malId?.toString() ?: extId.orEmpty(),
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
                    sourceProvider = lnItem.sourceProvider ?: "AniList"
                )
            }
        }

        if (bookDetails == null) {
            val cleanExtId = extId?.removePrefix("/works/")?.removePrefix("/")?.trim()

            // 1. Direct ID lookup
            if (entry.externalSource.equals("Google Books", ignoreCase = true) && !cleanExtId.isNullOrBlank()) {
                bookDetails = GoogleBooksApiClient.fetchVolumeDetails(cleanExtId, context).getOrNull()
            } else if (entry.externalSource.equals("Open Library", ignoreCase = true) && !cleanExtId.isNullOrBlank()) {
                bookDetails = OpenLibraryApiClient.fetchWorkDetails(cleanExtId).getOrNull()
            }

            // 2. Direct ID fallback if source was unassigned
            if (bookDetails == null && !cleanExtId.isNullOrBlank()) {
                bookDetails = GoogleBooksApiClient.fetchVolumeDetails(cleanExtId, context).getOrNull()
                    ?: OpenLibraryApiClient.fetchWorkDetails(cleanExtId).getOrNull()
            }

            // 3. Fallback to ISBN
            if (bookDetails == null) {
                val isbnRegex = Regex("(?i)ISBN:\\s*([0-9Xx-]+)")
                val isbn = (entry.description ?: entry.notes)?.let {
                    isbnRegex.find(it)?.groupValues?.get(1)?.replace("-", "")?.trim()
                }
                if (!isbn.isNullOrBlank()) {
                    bookDetails = BookMetadataManager.searchBooks(isbn, context, limit = 1).getOrNull()?.firstOrNull()
                }
            }

            // 4. Fallback to title search
            if (bookDetails == null && entry.title.isNotBlank()) {
                bookDetails = BookMetadataManager.searchBooks(entry.title, context, limit = 1).getOrNull()?.firstOrNull()
            }
        }

        if (bookDetails != null) {
            var hasChanges = false
            var finalCoverUri: String? = entry.coverImageUri
            var finalExternalId: String? = entry.externalId
            var finalExternalSource: String? = entry.externalSource

            if (entry.externalId.isNullOrBlank() && bookDetails.id.isNotBlank()) {
                finalExternalId = bookDetails.id
                finalExternalSource = bookDetails.sourceProvider
                hasChanges = true
            }

            val isCoverBroken = FailedCoverTracker.isEntryCoverBroken(entry)
            val needsCover = entry.coverImageUri.isNullOrBlank() ||
                             entry.coverImageUri.startsWith("file:") ||
                             isCoverBroken ||
                             forceRefresh

            if (!bookDetails.coverUrl.isNullOrBlank() && needsCover) {
                finalCoverUri = bookDetails.coverUrl
                hasChanges = true

                FailedCoverTracker.markSuccess(entry.id, bookDetails.coverUrl)

                try {
                    val imageLoader = Coil.imageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(bookDetails.coverUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                    imageLoader.enqueue(request)
                } catch (_: Exception) {}
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

            val newAiredStart = if ((entry.airedStartDate == null || forceRefresh) && publishEpoch != null) {
                hasChanges = true
                publishEpoch
            } else {
                entry.airedStartDate
            }

            val newAiredEnd = if ((entry.airedEndDate == null || forceRefresh) && publishEpoch != null) {
                hasChanges = true
                publishEpoch
            } else {
                entry.airedEndDate
            }

            val newAltTitle = if ((entry.alternateTitle.isNullOrBlank() || forceRefresh) && !bookDetails.subtitle.isNullOrBlank()) {
                hasChanges = true
                bookDetails.subtitle
            } else {
                entry.alternateTitle
            }

            val tagsStr = bookDetails.buildTagsString()
            val newTags = if ((entry.tags.isNullOrBlank() || forceRefresh) && tagsStr.isNotBlank()) {
                hasChanges = true
                tagsStr
            } else {
                entry.tags
            }

            val descWithMeta = bookDetails.buildDescriptionWithMetadata()
            val newDescription = if (((entry.description ?: entry.notes).isNullOrBlank() || forceRefresh) && descWithMeta.isNotBlank()) {
                hasChanges = true
                descWithMeta
            } else {
                entry.description ?: entry.notes
            }

            val newScore = if ((entry.externalScore == null || forceRefresh) && bookDetails.averageScore != null) {
                hasChanges = true
                bookDetails.averageScore
            } else {
                entry.externalScore
            }

            val newScoreSource = if (bookDetails.averageScore != null && bookDetails.sourceProvider.isNotBlank()) {
                bookDetails.sourceProvider
            } else {
                entry.externalScoreSource
            }

            val targetUnit = when {
                (bookDetails.pageCount ?: 0) > 0 -> TrackingUnit.PAGES
                (bookDetails.totalVolumes ?: 0) > 0 -> TrackingUnit.VOLUMES
                (bookDetails.totalChapters ?: 0) > 0 -> TrackingUnit.CHAPTERS
                else -> entry.trackingUnit
            }

            val newTotalPages = if ((entry.totalPages == 0 || forceRefresh) && (bookDetails.pageCount ?: 0) > 0) {
                hasChanges = true
                bookDetails.pageCount ?: 0
            } else {
                entry.totalPages
            }

            val newTotalVolumes = if ((entry.totalVolumes == 0 || forceRefresh) && (bookDetails.totalVolumes ?: 0) > 0) {
                hasChanges = true
                bookDetails.totalVolumes ?: 0
            } else {
                entry.totalVolumes
            }

            val newTotalChapters = if ((entry.totalChapters == 0 || forceRefresh) && (bookDetails.totalChapters ?: 0) > 0) {
                hasChanges = true
                bookDetails.totalChapters ?: 0
            } else {
                entry.totalChapters
            }

            var relatedJson: String? = entry.relatedEntriesJson
            if (relatedJson.isNullOrBlank() || forceRefresh) {
                try {
                    val relatedRes = BookMetadataManager.fetchRelatedBooks(entry, context)
                    val relatedList = relatedRes.getOrNull()
                    if (!relatedList.isNullOrEmpty()) {
                        relatedJson = gson.toJson(relatedList)
                        hasChanges = true
                    }
                } catch (_: Exception) {}
            }

            if (hasChanges) {
                val updatedEntry = entry.copy(
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
                    updatedAt = System.currentTimeMillis()
                )
                repository.update(updatedEntry)
            }
            return BookSyncItemResult(hasChanges = hasChanges, isSuccess = true)
        }

        return BookSyncItemResult(
            hasChanges = false,
            isSuccess = false,
            errorMessage = "Could not find book metadata for '${entry.title}'"
        )
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
            context?.let { ctx ->
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
            }
        }
    }
}

