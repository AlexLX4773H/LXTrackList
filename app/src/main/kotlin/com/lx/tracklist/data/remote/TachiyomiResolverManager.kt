package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.lx.tracklist.data.local.FailedCoverTracker
import com.lx.tracklist.data.local.StorageHelper
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
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TachiyomiResolveProgress(
    val isRunning: Boolean = false,
    val profileId: Long? = null,
    val profileName: String? = null,
    val currentTitle: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val resolvedCount: Int = 0,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val lastErrorMessage: String? = null,
    val lastReportPath: String? = null,
    val statusMessage: String? = null,
    val isCompleted: Boolean = false
) {
    val progressFraction: Float
        get() = if (totalCount > 0) (completedCount.toFloat() / totalCount).coerceIn(0f, 1f) else 0f
}

data class ResolvedItemReport(
    val entryId: Long,
    val originalTitle: String,
    val resolvedTitle: String,
    val isTitleChanged: Boolean,
    val alternateTitles: List<String>,
    val originalSource: String?,
    val originalExternalId: String?,
    val resolvedSource: String,
    val resolvedExternalId: String,
    val webUrl: String?,
    val coverUpdated: Boolean,
    val newCoverUrl: String?,
    val totalChapters: Int?,
    val totalVolumes: Int?,
    val externalScore: Float?
)

data class SkippedItemReport(
    val entryId: Long,
    val title: String,
    val externalId: String?,
    val reason: String,
    val evaluatedCandidates: List<String> = emptyList()
)

data class FailedItemReport(
    val entryId: Long,
    val title: String,
    val externalId: String?,
    val errorMessage: String
)

object TachiyomiResolverManager {

    private const val TAG = "TachiyomiResolver"
    private const val RATE_LIMIT_DELAY_MS = 450L
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val _resolveState = MutableStateFlow(TachiyomiResolveProgress())
    val resolveState: StateFlow<TachiyomiResolveProgress> = _resolveState.asStateFlow()

    private var resolveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun isEligibleTachiyomiEntry(entry: MediaEntry): Boolean {
        if (!JikanSyncManager.isTachiyomiSource(entry)) return false
        val isReadingType = entry.mediaType == MediaType.MANGA ||
                entry.mediaType == MediaType.MANHWA ||
                entry.mediaType == MediaType.BOOK
        return isReadingType && entry.title.isNotBlank()
    }

    fun isEligibleForResolution(entry: MediaEntry): Boolean {
        if (entry.title.isBlank()) return false
        if (entry.mediaType == MediaType.BOOK) {
            val hasValidBookSource = entry.externalSource.equals("Google Books", ignoreCase = true) ||
                    entry.externalSource.equals("Open Library", ignoreCase = true) ||
                    entry.externalSource.equals("AniList", ignoreCase = true) ||
                    entry.externalSource.equals("MAL", ignoreCase = true) ||
                    entry.externalSource.equals("MyAnimeList", ignoreCase = true)
            return entry.externalId.isNullOrBlank() || !hasValidBookSource
        }
        val isReadingType = entry.mediaType == MediaType.MANGA ||
                entry.mediaType == MediaType.MANHWA
        if (isReadingType) {
            return JikanSyncManager.isTachiyomiSource(entry) ||
                    entry.externalId.isNullOrBlank() ||
                    entry.externalSource.isNullOrBlank() ||
                    entry.externalSource.equals("TACHIYOMI", ignoreCase = true)
        }
        val isWatchType = entry.mediaType == MediaType.MOVIE ||
                entry.mediaType == MediaType.SHOW
        if (isWatchType) {
            val hasValidWatchSource = entry.externalSource.equals("TMDB", ignoreCase = true) ||
                    entry.externalSource.equals("MAL", ignoreCase = true) ||
                    entry.externalSource.equals("MyAnimeList", ignoreCase = true) ||
                    entry.externalSource.equals("AniList", ignoreCase = true) ||
                    entry.externalSource.equals("Kitsu", ignoreCase = true)
            return entry.externalId.isNullOrBlank() || !hasValidWatchSource
        }
        if (entry.mediaType == MediaType.ANIME) {
            return entry.externalId.isNullOrBlank() || entry.externalSource.isNullOrBlank()
        }
        return false
    }

    fun isManhwaEntry(entry: MediaEntry): Boolean {
        if (entry.mediaType == MediaType.MANHWA) return true
        val tags = entry.tags?.lowercase(Locale.ROOT) ?: ""
        if (tags.contains("manhwa") || tags.contains("webtoon")) return true
        val subCat = entry.subCategory?.lowercase(Locale.ROOT) ?: ""
        if (subCat.contains("manhwa") || subCat.contains("webtoon")) return true
        val title = entry.title.lowercase(Locale.ROOT)
        if (title.contains("(manhwa)") || title.contains("[manhwa]") || title.contains("(webtoon)") || title.contains("[webtoon]")) return true
        return false
    }

    fun isLikelyAnimeMovie(entry: MediaEntry): Boolean {
        val tags = entry.tags?.lowercase(Locale.ROOT) ?: ""
        if (tags.contains("anime") || tags.contains("animation") || tags.contains("japanese")) return true
        val subCat = entry.subCategory?.lowercase(Locale.ROOT) ?: ""
        if (subCat.contains("anime") || subCat.contains("animation")) return true
        val src = entry.externalSource?.lowercase(Locale.ROOT) ?: ""
        if (src == "mal" || src == "myanimelist" || src == "anilist" || src == "kitsu") return true
        val desc = (entry.description ?: entry.notes)?.lowercase(Locale.ROOT) ?: ""
        if (desc.contains("anilist.co/anime") || desc.contains("myanimelist.net/anime")) return true
        return false
    }

    fun isLightNovelOrWebNovel(entry: MediaEntry): Boolean {
        val tags = entry.tags?.lowercase(Locale.ROOT) ?: ""
        if (tags.contains("light novel") || tags.contains("web novel") || tags.contains("webnovel") || tags.contains("ranobe")) return true
        val subCat = entry.subCategory?.lowercase(Locale.ROOT) ?: ""
        if (subCat.contains("light novel") || subCat.contains("web novel") || subCat.contains("webnovel") || subCat.contains("novel")) return true
        val title = entry.title.lowercase(Locale.ROOT)
        if (title.contains("(light novel)") || title.contains("[light novel]") || title.contains("(novel)") || title.contains("[novel]")) return true
        return false
    }

    /**
     * Normalizes a title for confident exact matching:
     * - Converts to lowercase
     * - Strips combining diacritics / accents
     * - Replaces dashes, underscores, slashes with spaces
     * - Strips all punctuation and special characters (quotes, apostrophes, commas, colons, etc.)
     * - Collapses whitespace into single spaces and trims
     */
    fun normalizeTitle(title: String?): String {
        if (title.isNullOrBlank()) return ""
        val decomposed = Normalizer.normalize(title.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        val withoutAccents = decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        val withSpaceSeparators = withoutAccents.replace(Regex("[-_/]"), " ")
        val onlyLettersDigitsAndSpace = withSpaceSeparators.replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
        return onlyLettersDigitsAndSpace.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Strips common brackets / suffixes / quality tags like (Manhwa), [Official], (Webtoon), [4K], [1080p], (Uncensored)
     */
    fun stripBracketedSuffixes(title: String): String {
        if (title.isBlank()) return ""
        val withoutAllBrackets = title
            .replace(Regex("[\\[\\(\\{（【［〔〈《].*?[\\]\\)\\}）】］〕〉》]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        val candidate = if (withoutAllBrackets.isNotBlank()) {
            withoutAllBrackets
        } else {
            title
                .replace(Regex("(?i)\\s*[\\[\\(\\{（【［〔〈《](?:official|manhwa|webtoon|manga|novel|comic|colored|color|uncensored|censored|digital|raw|hd|hq)[\\]\\)\\}）】］〕〉》]"), "")
                .replace(Regex("\\s*[\\[\\(\\{（【［〔〈《].*?[\\]\\)\\}）】］〕〉》]\\s*$"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        // Also remove standalone or delimited suffix variants like "- Uncensored" or "Uncensored"
        val withoutTrailingUncensored = candidate
            .replace(Regex("(?i)\\s*[-–—:]?\\s*uncensored\\s*$"), "")
            .trim()

        return withoutTrailingUncensored.ifBlank { candidate.ifBlank { title.trim() } }
    }

    /**
     * Extracts the canonical external source provider ("MAL", "MangaDex", "AniList", "Kitsu")
     * and external ID from candidate metadata. Handles cases where JikanApiClient's multiple search
     * fell back to MangaDex, AniList, or Kitsu.
     */
    fun extractCandidateSourceAndId(cand: JikanMediaDetails): Pair<String, String>? {
        val provider = cand.sourceProvider.trim()

        // 1. MangaDex
        if (provider.equals("MangaDex", ignoreCase = true)) {
            val uuid = cand.url?.let { JikanSyncManager.extractMangaDexUuid(it) }
                ?: cand.url?.substringAfterLast('/')?.trim()?.takeIf { it.isNotBlank() }
            if (!uuid.isNullOrBlank()) {
                return Pair("MangaDex", uuid)
            }
        }

        // 2. MAL / Jikan
        if (provider.equals("Jikan", ignoreCase = true) || provider.equals("MAL", ignoreCase = true) || provider.startsWith("MAL", ignoreCase = true)) {
            val malId = cand.malId?.takeIf { it > 0 }
            if (malId != null) {
                return Pair("MAL", malId.toString())
            }
        }

        // 3. AniList
        if (provider.contains("AniList", ignoreCase = true)) {
            if (provider.startsWith("MAL", ignoreCase = true) && cand.malId != null && cand.malId > 0) {
                return Pair("MAL", cand.malId.toString())
            }
            val id = cand.malId?.takeIf { it > 0 }?.toString()
                ?: cand.url?.substringAfterLast('/')?.trim()?.takeIf { it.isNotBlank() }
            if (!id.isNullOrBlank()) {
                return Pair("AniList", id)
            }
        }

        // 4. Kitsu
        if (provider.equals("Kitsu", ignoreCase = true)) {
            val id = cand.malId?.takeIf { it > 0 }?.toString()
                ?: cand.url?.substringAfterLast('/')?.trim()?.takeIf { it.isNotBlank() }
            if (!id.isNullOrBlank()) {
                return Pair("Kitsu", id)
            }
        }

        // Generic fallback if provider was not explicitly classified:
        if (cand.malId != null && cand.malId > 0) {
            val src = if (provider.isNotBlank()) provider else "MAL"
            return Pair(src, cand.malId.toString())
        }
        val urlId = cand.url?.let { JikanSyncManager.extractMangaDexUuid(it) }
            ?: cand.url?.substringAfterLast('/')?.trim()?.takeIf { it.isNotBlank() }
        if (!urlId.isNullOrBlank()) {
            val src = if (provider.isNotBlank()) provider else "MangaDex"
            return Pair(src, urlId)
        }

        return null
    }

    /**
     * Determines whether candidate title or candidate's alternate titles exactly match the query title.
     * Match is confident ONLY if the normalized query (or normalized clean query) matches:
     * - the candidate's primary title
     * - or any of the candidate's alternate titles / synonyms
     * Checks exact normalized equality as well as space-agnostic equality (e.g. "Neverdie Extra" vs "Never Die Extra").
     */
    fun isConfidentExactMatch(
        queryTitle: String,
        candidateTitle: String?,
        candidateAltTitles: String? = null,
        entryAltTitles: String? = null
    ): Boolean {
        if (candidateTitle.isNullOrBlank()) return false
        if (isSingleTitleConfidentMatch(queryTitle, candidateTitle, candidateAltTitles)) return true

        if (!entryAltTitles.isNullOrBlank()) {
            val entryAlts = entryAltTitles.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            for (alt in entryAlts) {
                if (isSingleTitleConfidentMatch(alt, candidateTitle, candidateAltTitles)) return true
            }
        }

        return false
    }

    fun isConfidentExactMatch(
        queryTitle: String,
        candidate: JikanMediaDetails,
        entryAltTitles: String? = null
    ): Boolean {
        return isConfidentExactMatch(queryTitle, candidate.title, candidate.alternateTitle, entryAltTitles)
    }

    fun isConfidentExactMatch(
        queryTitle: String,
        candidate: TmdbSearchResult,
        entryAltTitles: String? = null
    ): Boolean {
        val alt = candidate.originalTitle?.takeIf { it.isNotBlank() && it != candidate.title }
        return isConfidentExactMatch(queryTitle, candidate.title, alt, entryAltTitles)
    }

    fun isConfidentExactMatch(
        queryTitle: String,
        candidate: TmdbMediaDetails,
        entryAltTitles: String? = null
    ): Boolean {
        return isConfidentExactMatch(queryTitle, candidate.title, candidate.alternateTitle, entryAltTitles)
    }

    fun isConfidentExactMatch(
        queryTitle: String,
        candidate: BookDetails,
        entryAltTitles: String? = null
    ): Boolean {
        val alt = candidate.subtitle?.takeIf { it.isNotBlank() && it != candidate.title }
        return isConfidentExactMatch(queryTitle, candidate.title, alt, entryAltTitles)
    }

    private fun isSingleTitleConfidentMatch(
        queryTitle: String,
        candidateTitle: String,
        candidateAltTitles: String?
    ): Boolean {
        val normQuery = normalizeTitle(queryTitle)
        if (normQuery.isBlank()) return false

        val cleanQuery = stripBracketedSuffixes(queryTitle)
        val normCleanQuery = normalizeTitle(cleanQuery)

        val normCandidateTitle = normalizeTitle(candidateTitle)
        val cleanCandidateTitle = stripBracketedSuffixes(candidateTitle)
        val normCleanCandidate = normalizeTitle(cleanCandidateTitle)

        // 1. Primary title match
        if (isExactOrSpaceAgnosticMatch(normQuery, normCleanQuery, normCandidateTitle, normCleanCandidate)) {
            return true
        }

        // 2. Candidate alternate titles & synonyms
        val altTitles = candidateAltTitles?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        for (alt in altTitles) {
            val normAlt = normalizeTitle(alt)
            val cleanAlt = stripBracketedSuffixes(alt)
            val normCleanAlt = normalizeTitle(cleanAlt)
            if (isExactOrSpaceAgnosticMatch(normQuery, normCleanQuery, normAlt, normCleanAlt)) {
                return true
            }
        }

        return false
    }

    private fun isExactOrSpaceAgnosticMatch(
        normQuery: String,
        normCleanQuery: String,
        normTarget: String,
        normCleanTarget: String
    ): Boolean {
        if (normTarget.isBlank() && normCleanTarget.isBlank()) return false

        // Exact match
        if (normTarget == normQuery || (normCleanQuery.isNotBlank() && normTarget == normCleanQuery)) {
            return true
        }
        if (normCleanTarget.isNotBlank() && (normCleanTarget == normQuery || normCleanTarget == normCleanQuery)) {
            return true
        }

        // Space-agnostic exact match (e.g. "Neverdie Extra" vs "Never Die Extra", "Solo-Leveling" vs "SoloLeveling")
        val noSpaceQuery = normQuery.replace(" ", "")
        val noSpaceCleanQuery = normCleanQuery.replace(" ", "")
        val noSpaceTarget = normTarget.replace(" ", "")
        val noSpaceCleanTarget = normCleanTarget.replace(" ", "")

        if (noSpaceTarget.isNotBlank() && (noSpaceTarget == noSpaceQuery || (noSpaceCleanQuery.isNotBlank() && noSpaceTarget == noSpaceCleanQuery))) {
            return true
        }
        if (noSpaceCleanTarget.isNotBlank() && (noSpaceCleanTarget == noSpaceQuery || (noSpaceCleanQuery.isNotBlank() && noSpaceCleanTarget == noSpaceCleanQuery))) {
            return true
        }

        return false
    }

    fun startResolution(
        context: Context,
        repository: MediaRepository,
        targetEntryIds: Set<Long>? = null,
        targetProfileId: Long? = null,
        targetProfileName: String? = null
    ) {
        val profileId = targetProfileId ?: repository.activeUser.value.id
        val profileName = targetProfileName ?: repository.activeUser.value.name

        if (_resolveState.value.isRunning) {
            if (_resolveState.value.profileId == profileId) {
                return
            } else {
                cancelResolution(context)
            }
        }

        resolveJob = scope.launch {
            NotificationHelper.initNotificationChannels(context)

            val allEntries = repository.getAllEntriesForUserSync(profileId)
            val pool = if (targetEntryIds != null) {
                allEntries.filter { targetEntryIds.contains(it.id) }
            } else {
                allEntries
            }

            val eligible = if (targetEntryIds != null) {
                pool.filter { it.title.isNotBlank() }
            } else {
                allEntries.filter { isEligibleForResolution(it) }
            }
            val total = eligible.size

            val resolvedItems = mutableListOf<ResolvedItemReport>()
            val skippedItems = mutableListOf<SkippedItemReport>()
            val failedItems = mutableListOf<FailedItemReport>()

            _resolveState.value = TachiyomiResolveProgress(
                isRunning = true,
                profileId = profileId,
                profileName = profileName,
                totalCount = total,
                pendingCount = total,
                statusMessage = if (total > 0) "Starting ID resolution ($total items)..." else "No entries found."
            )

            if (total == 0) {
                _resolveState.update { it.copy(isRunning = false, isCompleted = true, statusMessage = "No entries needed ID resolution.") }
                NotificationHelper.showTachiyomiResolveComplete(
                    context = context,
                    completed = 0,
                    total = 0,
                    resolved = 0,
                    skipped = 0,
                    failed = 0,
                    reportPath = null,
                    isCancelled = false,
                    profileName = profileName
                )
                return@launch
            }

            var completed = 0
            var resolvedCount = 0
            var skippedCount = 0
            var failedCount = 0

            var currentBatchSize = 5
            val minBatchSize = 2
            val maxBatchSize = 6
            var interBatchDelayMs = 200L
            var consecutiveCleanBatches = 0
            val mutex = Mutex()

            var index = 0
            while (index < eligible.size && resolveJob?.isCancelled != true) {
                val batchSize = currentBatchSize.coerceIn(minBatchSize, maxBatchSize)
                val chunk = eligible.subList(index, minOf(index + batchSize, eligible.size))

                val chunkTitles = chunk.joinToString(", ") { it.title }
                _resolveState.update {
                    it.copy(
                        currentTitle = if (chunk.size == 1) chunk[0].title else "${chunk.size} in progress: ${chunkTitles.take(60)}...",
                        statusMessage = "Matching ${chunk.size} items in parallel across databases..."
                    )
                }

                var hadRateLimit = false
                val chunkResults = coroutineScope {
                    chunk.map { entry ->
                        async {
                            if (resolveJob?.isCancelled == true) return@async null
                            try {
                                val outcome = matchAndResolveEntry(entry, repository, context)
                                mutex.withLock {
                                    completed++
                                    when (outcome) {
                                        is ResolutionOutcome.Success -> resolvedCount++
                                        is ResolutionOutcome.Skipped -> skippedCount++
                                        is ResolutionOutcome.Failed -> {
                                            failedCount++
                                            val err = outcome.report.errorMessage
                                            if (err.contains("429", ignoreCase = true) || err.contains("rate limit", ignoreCase = true)) {
                                                hadRateLimit = true
                                            }
                                        }
                                    }
                                    _resolveState.update {
                                        it.copy(
                                            completedCount = completed,
                                            pendingCount = total - completed,
                                            resolvedCount = resolvedCount,
                                            skippedCount = skippedCount,
                                            failedCount = failedCount,
                                            currentTitle = entry.title
                                        )
                                    }
                                    NotificationHelper.showTachiyomiResolveProgress(context, completed, total, entry.title, profileName)
                                }
                                Pair(entry, outcome)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                val errorMsg = e.message ?: e.localizedMessage ?: "Unknown error"
                                val failedOutcome = ResolutionOutcome.Failed(
                                    FailedItemReport(
                                        entryId = entry.id,
                                        title = entry.title,
                                        externalId = entry.externalId,
                                        errorMessage = errorMsg
                                    )
                                )
                                mutex.withLock {
                                    completed++
                                    failedCount++
                                    if (errorMsg.contains("429", ignoreCase = true) || errorMsg.contains("rate limit", ignoreCase = true)) {
                                        hadRateLimit = true
                                    }
                                    _resolveState.update {
                                        it.copy(
                                            completedCount = completed,
                                            pendingCount = total - completed,
                                            failedCount = failedCount,
                                            currentTitle = entry.title
                                        )
                                    }
                                    NotificationHelper.showTachiyomiResolveProgress(context, completed, total, entry.title, profileName)
                                }
                                Pair(entry, failedOutcome)
                            }
                        }
                    }.awaitAll()
                }

                for (res in chunkResults) {
                    if (res == null) continue
                    when (val outcome = res.second) {
                        is ResolutionOutcome.Success -> resolvedItems.add(outcome.report)
                        is ResolutionOutcome.Skipped -> skippedItems.add(outcome.report)
                        is ResolutionOutcome.Failed -> failedItems.add(outcome.report)
                    }
                }

                index += chunk.size

                // Dynamic concurrency adjustment:
                if (hadRateLimit) {
                    consecutiveCleanBatches = 0
                    currentBatchSize = maxOf(minBatchSize, currentBatchSize - 1)
                    interBatchDelayMs = minOf(1500L, interBatchDelayMs + 300L)
                    Log.w(TAG, "Rate limit detected. Dynamically throttled batch size to $currentBatchSize, inter-batch delay to ${interBatchDelayMs}ms")
                    delay(interBatchDelayMs)
                } else {
                    consecutiveCleanBatches++
                    if (consecutiveCleanBatches >= 2 && currentBatchSize < maxBatchSize) {
                        currentBatchSize = minOf(maxBatchSize, currentBatchSize + 1)
                        interBatchDelayMs = maxOf(200L, interBatchDelayMs - 50L)
                    }
                    if (index < eligible.size) {
                        delay(interBatchDelayMs)
                    }
                }
            }

            val isCancelled = resolveJob?.isCancelled == true

            // Generate report in LXTrackList/data folder
            val reportPath = try {
                generateResolutionReport(
                    context = context,
                    profileName = profileName,
                    profileId = profileId,
                    totalProcessed = completed,
                    resolvedItems = resolvedItems,
                    skippedItems = skippedItems,
                    failedItems = failedItems
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate Tachiyomi resolution report", e)
                null
            }

            _resolveState.update {
                it.copy(
                    isRunning = false,
                    isCompleted = true,
                    lastReportPath = reportPath,
                    statusMessage = if (isCancelled) "Resolution cancelled." else "Resolution complete ($resolvedCount linked, $skippedCount skipped)."
                )
            }

            NotificationHelper.showTachiyomiResolveComplete(
                context = context,
                completed = completed,
                total = total,
                resolved = resolvedCount,
                skipped = skippedCount,
                failed = failedCount,
                reportPath = reportPath,
                isCancelled = isCancelled,
                profileName = profileName
            )
        }
    }

    fun cancelResolution(context: Context) {
        resolveJob?.cancel()
        resolveJob = null
        val cur = _resolveState.value
        _resolveState.update {
            it.copy(
                isRunning = false,
                statusMessage = "Cancelled"
            )
        }
        NotificationHelper.showTachiyomiResolveComplete(
            context = context,
            completed = cur.completedCount,
            total = cur.totalCount,
            resolved = cur.resolvedCount,
            skipped = cur.skippedCount,
            failed = cur.failedCount,
            reportPath = cur.lastReportPath,
            isCancelled = true,
            profileName = cur.profileName
        )
    }

    private data class ResolvedJikanMatch(
        val details: JikanMediaDetails,
        val source: String,
        val externalId: String
    )

    fun extractAniListIdsFromDescription(description: String?, preferManga: Boolean? = null): List<Long> {
        if (description.isNullOrBlank()) return emptyList()
        val ids = mutableListOf<Long>()

        val urlRegex = Regex("""anilist\.co/(manga|anime)/(\d+)""", RegexOption.IGNORE_CASE)
        urlRegex.findAll(description).forEach { match ->
            val kind = match.groupValues[1].lowercase(Locale.US)
            val id = match.groupValues[2].toLongOrNull() ?: return@forEach
            if (id <= 0) return@forEach
            val typeMatches = when (preferManga) {
                true -> kind == "manga"
                false -> kind == "anime"
                null -> true
            }
            if (typeMatches && !ids.contains(id)) {
                ids.add(id)
            }
        }

        val labelRegex = Regex("""(?:\[?\s*AniList(?:\s+ID)?\s*[:=]\s*(\d+)\s*\]?)""", RegexOption.IGNORE_CASE)
        labelRegex.findAll(description).forEach { match ->
            val id = match.groupValues[1].toLongOrNull()
            if (id != null && id > 0 && !ids.contains(id)) {
                ids.add(id)
            }
        }

        return ids
    }

    @Deprecated("Use extractAniListIdsFromDescription instead", ReplaceWith("extractAniListIdsFromDescription(notes, preferManga)"))
    fun extractAniListIdsFromNotes(notes: String?, preferManga: Boolean? = null): List<Long> =
        extractAniListIdsFromDescription(notes, preferManga)

    fun extractAlternateTitlesFromDescription(description: String?): List<String> {
        if (description.isNullOrBlank()) return emptyList()
        val results = mutableListOf<String>()
        val headerRegex = Regex(
            """^(?:[-*•]\s*)?(?:(?:alternative|alternate|alt)\s*(?:titles?|names?)|synonyms|other\s*names?|also\s*known\s*as|aka)$""",
            RegexOption.IGNORE_CASE
        )
        val headerPrefixRegex = Regex(
            """^\s*(?:[-*•]\s*)?(?:(?:alternative|alternate|alt)\s*(?:titles?|names?)|synonyms|other\s*names?|also\s*known\s*as|aka)\s*:\s*(.*)$""",
            RegexOption.IGNORE_CASE
        )
        val otherMetadataHeaderRegex = Regex(
            """^\s*(?:author|artist|status|genres?|tags?|category|rating|score|source|description|synopsis|publisher|type|links?)\s*:""",
            RegexOption.IGNORE_CASE
        )
        val bulletPrefixRegex = Regex("""^\s*[-*•◦▪]\s+""")

        val lines = description.lines()
        var inAltSection = false
        var sawBlankAfterHeader = false

        fun addIfUnique(candidate: String) {
            val cleaned = candidate.trim()
            if (cleaned.isNotBlank() &&
                !cleaned.startsWith("http://", ignoreCase = true) &&
                !cleaned.startsWith("https://", ignoreCase = true) &&
                results.none { it.equals(cleaned, ignoreCase = true) }
            ) {
                results.add(cleaned)
            }
        }

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            val headerMatch = headerPrefixRegex.matchEntire(trimmed)
            if (headerMatch != null) {
                inAltSection = true
                sawBlankAfterHeader = false
                val inlineRemainder = headerMatch.groupValues[1].trim()
                if (inlineRemainder.isNotBlank()) {
                    val splitParts = when {
                        inlineRemainder.contains(";") || inlineRemainder.contains("|") || inlineRemainder.contains(" / ") ->
                            inlineRemainder.split(Regex("[;|]|\\s+/\\s+"))
                        inlineRemainder.contains(",") ->
                            listOf(inlineRemainder) + inlineRemainder.split(",")
                        else -> listOf(inlineRemainder)
                    }
                    splitParts.forEach(::addIfUnique)
                }
                continue
            }

            if (inAltSection) {
                if (trimmed.isBlank()) {
                    if (results.isNotEmpty() || sawBlankAfterHeader) {
                        inAltSection = false
                    } else {
                        sawBlankAfterHeader = true
                    }
                    continue
                }

                val isBullet = bulletPrefixRegex.containsMatchIn(rawLine)
                val cleanItem = rawLine.replace(bulletPrefixRegex, "").trim()

                if (!isBullet && (
                        otherMetadataHeaderRegex.containsMatchIn(trimmed) ||
                            trimmed.startsWith("[") ||
                            trimmed.startsWith("(Source:", ignoreCase = true) ||
                            trimmed.startsWith("http://", ignoreCase = true) ||
                            trimmed.startsWith("https://", ignoreCase = true) ||
                            headerRegex.matches(trimmed.removeSuffix(":"))
                        )
                ) {
                    inAltSection = false
                    continue
                }

                addIfUnique(cleanItem)
            }
        }

        return results
    }

    @Deprecated("Use extractAlternateTitlesFromDescription instead", ReplaceWith("extractAlternateTitlesFromDescription(notes)"))
    fun extractAlternateTitlesFromNotes(notes: String?): List<String> =
        extractAlternateTitlesFromDescription(notes)

    fun getCombinedAlternateTitles(entry: MediaEntry): List<String> {
        val seenNorm = mutableSetOf<String>()
        val mainNorm = normalizeTitle(entry.title)
        val cleanMainNorm = normalizeTitle(stripBracketedSuffixes(entry.title))
        if (mainNorm.isNotBlank()) seenNorm.add(mainNorm)
        if (cleanMainNorm.isNotBlank()) seenNorm.add(cleanMainNorm)

        val combined = mutableListOf<String>()
        (entry.getAlternateTitles() + extractAlternateTitlesFromDescription(entry.description ?: entry.notes)).forEach { raw ->
            val clean = raw.trim()
            val norm = normalizeTitle(clean)
            if (clean.isNotBlank() && norm.isNotBlank() && seenNorm.add(norm)) {
                combined.add(clean)
            }
        }
        return combined
    }

    fun getCombinedAlternateTitlesString(entry: MediaEntry): String? {
        return getCombinedAlternateTitles(entry).joinToString("\n").takeIf { it.isNotBlank() }
    }

    private fun tryResolveFromAniListIds(
        aniListIds: List<Long>,
        isManga: Boolean,
        evaluatedCandidateNames: MutableList<String>
    ): ResolvedJikanMatch? {
        for (aniId in aniListIds) {
            val res = JikanApiClient.queryAniListById(aniListId = aniId, isManga = isManga)
            if (res.isSuccess) {
                val cand = res.getOrNull()
                if (cand != null && !cand.title.isNullOrBlank()) {
                    evaluatedCandidateNames.add("AniList(Description ID #$aniId): ${cand.title}")
                    val srcAndId = extractCandidateSourceAndId(cand)
                    if (srcAndId != null) {
                        return ResolvedJikanMatch(cand, srcAndId.first, srcAndId.second)
                    }
                }
            } else {
                evaluatedCandidateNames.add("AniList(Description ID #$aniId): invalid/not found")
            }
        }
        return null
    }

    private fun matchAndPickCandidate(
        candidates: List<JikanMediaDetails>,
        providerLabel: String?,
        searchTitle: String,
        matchTargetTitle: String,
        cleanMatchTitle: String,
        entryAltTitles: String?,
        evaluatedCandidateNames: MutableList<String>
    ): ResolvedJikanMatch? {
        for (cand in candidates) {
            val prefix = providerLabel ?: cand.sourceProvider
            evaluatedCandidateNames.add("$prefix: ${cand.title}")
            if (isConfidentExactMatch(searchTitle, cand, entryAltTitles) ||
                isConfidentExactMatch(matchTargetTitle, cand, entryAltTitles) ||
                isConfidentExactMatch(cleanMatchTitle, cand, entryAltTitles)
            ) {
                val srcAndId = extractCandidateSourceAndId(cand)
                if (srcAndId != null) {
                    return ResolvedJikanMatch(cand, srcAndId.first, srcAndId.second)
                }
            }
        }
        return null
    }

    private fun searchReadingByTitle(
        searchTitle: String,
        matchTargetTitle: String,
        cleanMatchTitle: String,
        entryAltTitles: String?,
        isManhwa: Boolean,
        evaluatedCandidateNames: MutableList<String>
    ): ResolvedJikanMatch? {
        val cleanSearch = stripBracketedSuffixes(searchTitle).takeIf { it.isNotBlank() } ?: searchTitle.trim()
        val rawSearch = searchTitle.trim()

        fun queryAniListSource(): ResolvedJikanMatch? {
            val aniPrimary = JikanApiClient.queryAniListMultiple(
                searchTitle = cleanSearch,
                isManga = true,
                subType = if (isManhwa) "MANHWA" else null,
                limit = 5
            ).getOrDefault(emptyList())
            val aniCandidates = if (aniPrimary.isNotEmpty() || cleanSearch.equals(rawSearch, ignoreCase = true)) {
                aniPrimary
            } else {
                JikanApiClient.queryAniListMultiple(
                    searchTitle = rawSearch,
                    isManga = true,
                    subType = if (isManhwa) "MANHWA" else null,
                    limit = 5
                ).getOrDefault(emptyList())
            }
            return matchAndPickCandidate(
                aniCandidates, "AniList", rawSearch, matchTargetTitle, cleanMatchTitle, entryAltTitles, evaluatedCandidateNames
            )
        }

        fun queryMangaDexSource(): ResolvedJikanMatch? {
            val mdPrimary = MangaDexApiClient.searchMangaMultiple(cleanSearch, isManhwa = isManhwa, limit = 5).getOrDefault(emptyList())
            val mdCandidates = if (mdPrimary.isNotEmpty() || cleanSearch.equals(rawSearch, ignoreCase = true)) {
                mdPrimary
            } else {
                MangaDexApiClient.searchMangaMultiple(rawSearch, isManhwa = isManhwa, limit = 5).getOrDefault(emptyList())
            }
            return matchAndPickCandidate(
                mdCandidates, "MangaDex", rawSearch, matchTargetTitle, cleanMatchTitle, entryAltTitles, evaluatedCandidateNames
            )
        }

        fun queryKitsuSource(): ResolvedJikanMatch? {
            val kitsuPrimary = KitsuApiClient.searchMangaMultiple(cleanSearch, limit = 5).getOrDefault(emptyList())
            val kitsuCandidates = if (kitsuPrimary.isNotEmpty() || cleanSearch.equals(rawSearch, ignoreCase = true)) {
                kitsuPrimary
            } else {
                KitsuApiClient.searchMangaMultiple(rawSearch, limit = 5).getOrDefault(emptyList())
            }
            return matchAndPickCandidate(
                kitsuCandidates, "Kitsu", rawSearch, matchTargetTitle, cleanMatchTitle, entryAltTitles, evaluatedCandidateNames
            )
        }

        fun queryJikanSource(): ResolvedJikanMatch? {
            val jikanPrimary = JikanApiClient.searchJikanMultiple(
                queryTitle = cleanSearch,
                isManga = true,
                subType = if (isManhwa) "manhwa" else null,
                limit = 5
            ).getOrDefault(emptyList())
            val jikanCandidates = if (jikanPrimary.isNotEmpty() || cleanSearch.equals(rawSearch, ignoreCase = true)) {
                jikanPrimary
            } else {
                JikanApiClient.searchJikanMultiple(
                    queryTitle = rawSearch,
                    isManga = true,
                    subType = if (isManhwa) "manhwa" else null,
                    limit = 5
                ).getOrDefault(emptyList())
            }
            return matchAndPickCandidate(
                jikanCandidates, null, rawSearch, matchTargetTitle, cleanMatchTitle, entryAltTitles, evaluatedCandidateNames
            )
        }

        if (isManhwa) {
            // MANHWA: AniList -> MangaDex -> Kitsu -> Jikan (MAL last to avoid rate limits and 429 delays)
            queryAniListSource()?.let { return it }
            queryMangaDexSource()?.let { return it }
            queryKitsuSource()?.let { return it }
            return queryJikanSource()
        } else {
            // MANGA / BOOKS: AniList -> MangaDex -> Jikan (MAL) -> Kitsu
            queryAniListSource()?.let { return it }
            queryMangaDexSource()?.let { return it }
            queryJikanSource()?.let { return it }
            return queryKitsuSource()
        }
    }

    private fun searchAnimeByTitle(
        searchTitle: String,
        matchTargetTitle: String,
        cleanMatchTitle: String,
        entryAltTitles: String?,
        evaluatedCandidateNames: MutableList<String>
    ): ResolvedJikanMatch? {
        val cleanSearch = stripBracketedSuffixes(searchTitle).takeIf { it.isNotBlank() } ?: searchTitle.trim()
        val rawSearch = searchTitle.trim()

        // 1. AniList (Fast GraphQL, returns idMal, avoids Jikan 429 rate limit delays)
        val aniPrimary = JikanApiClient.queryAniListMultiple(
            searchTitle = cleanSearch,
            isManga = false,
            limit = 5
        ).getOrDefault(emptyList())
        val aniCandidates = if (aniPrimary.isNotEmpty() || cleanSearch.equals(rawSearch, ignoreCase = true)) {
            aniPrimary
        } else {
            JikanApiClient.queryAniListMultiple(
                searchTitle = rawSearch,
                isManga = false,
                limit = 5
            ).getOrDefault(emptyList())
        }
        matchAndPickCandidate(
            aniCandidates, "AniList", rawSearch, matchTargetTitle, cleanMatchTitle, entryAltTitles, evaluatedCandidateNames
        )?.let { return it }

        // 2. Jikan (MAL direct fallback)
        val jikanPrimary = JikanApiClient.searchJikanMultiple(
            queryTitle = cleanSearch,
            isManga = false,
            limit = 5
        ).getOrDefault(emptyList())
        val jikanCandidates = if (jikanPrimary.isNotEmpty() || cleanSearch.equals(rawSearch, ignoreCase = true)) {
            jikanPrimary
        } else {
            JikanApiClient.searchJikanMultiple(
                queryTitle = rawSearch,
                isManga = false,
                limit = 5
            ).getOrDefault(emptyList())
        }
        return matchAndPickCandidate(
            jikanCandidates, null, rawSearch, matchTargetTitle, cleanMatchTitle, entryAltTitles, evaluatedCandidateNames
        )
    }

    private fun searchTmdbByTitle(
        searchTitle: String,
        matchTargetTitle: String,
        cleanMatchTitle: String,
        entryAltTitles: String?,
        isTv: Boolean,
        context: Context?,
        evaluatedCandidateNames: MutableList<String>
    ): TmdbMediaDetails? {
        val cleanSearch = TmdbApiClient.cleanSearchTitle(searchTitle).takeIf { it.isNotBlank() } ?: searchTitle.trim()
        val rawSearch = searchTitle.trim()

        fun pickFrom(results: List<TmdbSearchResult>, tag: String? = null): TmdbMediaDetails? {
            for (cand in results.take(6)) {
                val typeLabel = "${tag ?: ""}${if (cand.isMovie) "Movie" else "TV"}"
                evaluatedCandidateNames.add("TMDB($typeLabel): ${cand.title}")
                if (isConfidentExactMatch(rawSearch, cand, entryAltTitles) ||
                    isConfidentExactMatch(matchTargetTitle, cand, entryAltTitles) ||
                    isConfidentExactMatch(cleanMatchTitle, cand, entryAltTitles)
                ) {
                    val fullRes = TmdbApiClient.fetchMediaDetails(cand.tmdbId, isTv = !cand.isMovie, context = context)
                    if (fullRes.isSuccess) {
                        val details = fullRes.getOrNull()
                        if (details != null) return details
                    }
                }
            }
            return null
        }

        val primary = if (isTv) TmdbApiClient.searchTvMultiple(cleanSearch, context) else TmdbApiClient.searchMoviesMultiple(cleanSearch, context)
        pickFrom(primary.getOrDefault(emptyList()))?.let { return it }

        val fallbackType = if (isTv) TmdbApiClient.searchMoviesMultiple(cleanSearch, context) else TmdbApiClient.searchTvMultiple(cleanSearch, context)
        pickFrom(fallbackType.getOrDefault(emptyList()))?.let { return it }

        if (!cleanSearch.equals(rawSearch, ignoreCase = true) && rawSearch.isNotBlank()) {
            val rawResults = if (isTv) TmdbApiClient.searchTvMultiple(rawSearch, context) else TmdbApiClient.searchMoviesMultiple(rawSearch, context)
            pickFrom(rawResults.getOrDefault(emptyList()), tag = "raw ")?.let { return it }
        }
        return null
    }

    sealed class ResolutionOutcome {
        data class Success(val report: ResolvedItemReport) : ResolutionOutcome()
        data class Skipped(val report: SkippedItemReport) : ResolutionOutcome()
        data class Failed(val report: FailedItemReport) : ResolutionOutcome()
    }

    suspend fun matchAndResolveEntry(
        entry: MediaEntry,
        repository: MediaRepository,
        context: Context?,
        skipExistingIdCheck: Boolean = false
    ): ResolutionOutcome {
        if (entry.mediaType == MediaType.BOOK) {
            return matchAndResolveBook(entry, repository, context, skipExistingIdCheck)
        }
        if (entry.mediaType == MediaType.MOVIE) {
            val isExplicitAnime = isLikelyAnimeMovie(entry)
            if (isExplicitAnime) {
                // 1. Anime sources first (AniList -> Jikan) for known/tagged anime movies
                val animeOutcome = matchAndResolveAnime(entry, repository, context, skipExistingIdCheck)
                if (animeOutcome is ResolutionOutcome.Success) return animeOutcome
                // 2. Fallback to TMDB
                return matchAndResolveMovieOrShow(entry, repository, context, skipExistingIdCheck)
            } else {
                // 1. TMDB first for general movies
                val tmdbOutcome = matchAndResolveMovieOrShow(entry, repository, context, skipExistingIdCheck)
                if (tmdbOutcome is ResolutionOutcome.Success) return tmdbOutcome
                // 2. Fallback to Anime sources (for untagged anime movies in Movies category)
                val animeOutcome = matchAndResolveAnime(entry, repository, context, skipExistingIdCheck)
                if (animeOutcome is ResolutionOutcome.Success) return animeOutcome
                return tmdbOutcome
            }
        }
        if (entry.mediaType == MediaType.SHOW) {
            return matchAndResolveMovieOrShow(entry, repository, context, skipExistingIdCheck)
        }
        if (entry.mediaType == MediaType.ANIME) {
            val animeOutcome = matchAndResolveAnime(entry, repository, context, skipExistingIdCheck)
            if (animeOutcome is ResolutionOutcome.Success) return animeOutcome
            val tmdbOutcome = matchAndResolveMovieOrShow(entry, repository, context, skipExistingIdCheck)
            if (tmdbOutcome is ResolutionOutcome.Success) return tmdbOutcome
            return animeOutcome
        }
        return matchAndResolveReading(entry, repository, context, skipExistingIdCheck)
    }

    private suspend fun matchAndResolveBook(
        entry: MediaEntry,
        repository: MediaRepository,
        context: Context?,
        skipExistingIdCheck: Boolean = false
    ): ResolutionOutcome {
        val queryTitle = entry.title.trim()
        val cleanTitle = stripBracketedSuffixes(queryTitle).takeIf { it.isNotBlank() } ?: queryTitle
        val isLightNovel = isLightNovelOrWebNovel(entry)
        val evaluatedCandidateNames = mutableListOf<String>()
        val combinedAltTitlesList = getCombinedAlternateTitles(entry)
        val combinedAltTitlesStr = combinedAltTitlesList.joinToString("\n").takeIf { it.isNotBlank() }

        val extIdStr = (entry.externalId ?: entry.foreignDatabaseId)?.trim()
        val srcStr = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim()

        var matchedBookDetails: BookDetails? = null
        var matchedJikanDetails: ResolvedJikanMatch? = null

        // 1. Try existing external ID if present
        if (!skipExistingIdCheck && !extIdStr.isNullOrBlank()) {
            if (srcStr?.equals("Google Books", ignoreCase = true) == true) {
                val candidate = GoogleBooksApiClient.fetchVolumeDetails(extIdStr, context).getOrNull()
                if (candidate != null) {
                    evaluatedCandidateNames.add("Google Books(ID): ${candidate.title}")
                    if (isConfidentExactMatch(queryTitle, candidate, combinedAltTitlesStr) ||
                        isConfidentExactMatch(cleanTitle, candidate, combinedAltTitlesStr)
                    ) {
                        matchedBookDetails = candidate
                    }
                }
            } else if (srcStr?.equals("Open Library", ignoreCase = true) == true) {
                val candidate = OpenLibraryApiClient.fetchWorkDetails(extIdStr).getOrNull()
                if (candidate != null) {
                    evaluatedCandidateNames.add("Open Library(ID): ${candidate.title}")
                    if (isConfidentExactMatch(queryTitle, candidate, combinedAltTitlesStr) ||
                        isConfidentExactMatch(cleanTitle, candidate, combinedAltTitlesStr)
                    ) {
                        matchedBookDetails = candidate
                    }
                }
            } else if (srcStr?.equals("AniList", ignoreCase = true) == true) {
                val aniId = extIdStr.toLongOrNull()
                if (aniId != null && aniId > 0) {
                    val candidate = JikanApiClient.queryAniListById(aniListId = aniId, isManga = true).getOrNull()
                    if (candidate != null) {
                        evaluatedCandidateNames.add("AniList(ID): ${candidate.title}")
                        if (isConfidentExactMatch(queryTitle, candidate, combinedAltTitlesStr) ||
                            isConfidentExactMatch(cleanTitle, candidate, combinedAltTitlesStr)
                        ) {
                            extractCandidateSourceAndId(candidate)?.let { (src, id) ->
                                matchedJikanDetails = ResolvedJikanMatch(candidate, src, id)
                            }
                        }
                    }
                }
            }
        }

        // Helper to search BookDetails providers (Google Books & Open Library)
        fun searchStandardBooks(searchT: String): BookDetails? {
            val cSearch = stripBracketedSuffixes(searchT).takeIf { it.isNotBlank() } ?: searchT.trim()
            val rSearch = searchT.trim()

            // 1. Google Books
            val gbResults = GoogleBooksApiClient.searchBooks(cSearch, context, limit = 5).getOrDefault(emptyList())
            for (cand in gbResults) {
                evaluatedCandidateNames.add("Google Books: ${cand.title}")
                if (isConfidentExactMatch(rSearch, cand, combinedAltTitlesStr) ||
                    isConfidentExactMatch(queryTitle, cand, combinedAltTitlesStr) ||
                    isConfidentExactMatch(cleanTitle, cand, combinedAltTitlesStr)
                ) {
                    return cand
                }
            }

            // 2. Open Library
            val olResults = OpenLibraryApiClient.searchBooks(cSearch, limit = 5).getOrDefault(emptyList())
            for (cand in olResults) {
                evaluatedCandidateNames.add("Open Library: ${cand.title}")
                if (isConfidentExactMatch(rSearch, cand, combinedAltTitlesStr) ||
                    isConfidentExactMatch(queryTitle, cand, combinedAltTitlesStr) ||
                    isConfidentExactMatch(cleanTitle, cand, combinedAltTitlesStr)
                ) {
                    return cand
                }
            }
            return null
        }

        // Helper to search Light Novels (AniList format: NOVEL & Jikan subType: lightnovel)
        fun searchNovelSources(searchT: String): ResolvedJikanMatch? {
            val cSearch = stripBracketedSuffixes(searchT).takeIf { it.isNotBlank() } ?: searchT.trim()
            val rSearch = searchT.trim()

            // AniList Light Novel
            val aniResults = AniListNovelApiClient.searchLightNovels(cSearch, limit = 5).getOrDefault(emptyList())
            for (cand in aniResults) {
                evaluatedCandidateNames.add("AniList(Novel): ${cand.title}")
                if (isConfidentExactMatch(rSearch, cand, combinedAltTitlesStr) ||
                    isConfidentExactMatch(queryTitle, cand, combinedAltTitlesStr) ||
                    isConfidentExactMatch(cleanTitle, cand, combinedAltTitlesStr)
                ) {
                    extractCandidateSourceAndId(cand)?.let { (src, id) ->
                        return ResolvedJikanMatch(cand, src, id)
                    }
                }
            }

            // Jikan Light Novel fallback
            val jikanResults = JikanApiClient.searchJikanMultiple(
                queryTitle = cSearch,
                isManga = true,
                subType = "lightnovel",
                limit = 5
            ).getOrDefault(emptyList())
            for (cand in jikanResults) {
                evaluatedCandidateNames.add("MAL(Novel): ${cand.title}")
                if (isConfidentExactMatch(rSearch, cand, combinedAltTitlesStr) ||
                    isConfidentExactMatch(queryTitle, cand, combinedAltTitlesStr) ||
                    isConfidentExactMatch(cleanTitle, cand, combinedAltTitlesStr)
                ) {
                    extractCandidateSourceAndId(cand)?.let { (src, id) ->
                        return ResolvedJikanMatch(cand, src, id)
                    }
                }
            }
            return null
        }

        // 2. Title search based on book type:
        if (matchedBookDetails == null && matchedJikanDetails == null) {
            if (isLightNovel) {
                matchedJikanDetails = searchNovelSources(cleanTitle)
                if (matchedJikanDetails == null) {
                    matchedBookDetails = searchStandardBooks(cleanTitle)
                }
            } else {
                matchedBookDetails = searchStandardBooks(cleanTitle)
                if (matchedBookDetails == null) {
                    matchedJikanDetails = searchNovelSources(cleanTitle)
                }
            }
        }

        // 3. Fallback to Alternate Names (top 4 alternate names)
        if (matchedBookDetails == null && matchedJikanDetails == null && combinedAltTitlesList.isNotEmpty()) {
            for (altTitle in combinedAltTitlesList.take(4)) {
                delay(150L)
                if (isLightNovel) {
                    matchedJikanDetails = searchNovelSources(altTitle)
                    if (matchedJikanDetails != null) break
                    matchedBookDetails = searchStandardBooks(altTitle)
                    if (matchedBookDetails != null) break
                } else {
                    matchedBookDetails = searchStandardBooks(altTitle)
                    if (matchedBookDetails != null) break
                    matchedJikanDetails = searchNovelSources(altTitle)
                    if (matchedJikanDetails != null) break
                }
            }
        }

        // If matched via Jikan/AniList:
        if (matchedJikanDetails != null) {
            return applyJikanMatchToEntry(
                entry = entry,
                matchDetails = matchedJikanDetails!!.details,
                matchSource = matchedJikanDetails!!.source,
                matchExternalId = matchedJikanDetails!!.externalId,
                repository = repository,
                context = context
            )
        }

        // If matched via Google Books or Open Library:
        if (matchedBookDetails != null) {
            val book = matchedBookDetails!!
            val cleanedOriginalTitle = stripBracketedSuffixes(entry.title).takeIf { it.isNotBlank() } ?: entry.title

            val candidateSubtitle = book.subtitle?.takeIf { it.isNotBlank() }
            val currentAlts = entry.alternateTitle?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            val extraAlts = if (candidateSubtitle != null && !candidateSubtitle.equals(cleanedOriginalTitle, ignoreCase = true)) {
                listOf(candidateSubtitle)
            } else emptyList()
            val allAlts = (extraAlts + currentAlts).distinct().filter { !it.equals(cleanedOriginalTitle, ignoreCase = true) }

            val isCoverBroken = FailedCoverTracker.isEntryCoverBroken(entry)
            val needsPoster = entry.coverImageUri.isNullOrBlank() ||
                    entry.coverImageUri.startsWith("file:") ||
                    isCoverBroken
            val finalCover = if (needsPoster && !book.coverUrl.isNullOrBlank()) book.coverUrl else entry.coverImageUri
            val coverUpdated = finalCover != entry.coverImageUri

            if (coverUpdated && !book.coverUrl.isNullOrBlank()) {
                FailedCoverTracker.markSuccess(entry.id, book.coverUrl)
                if (context != null) {
                    try {
                        val loader = Coil.imageLoader(context)
                        val req = ImageRequest.Builder(context)
                            .data(book.coverUrl)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                        loader.enqueue(req)
                    } catch (_: Exception) {}
                }
            }

            val publishEpoch = book.publishedDate?.let { dateStr ->
                try {
                    when (dateStr.length) {
                        4 -> SimpleDateFormat("yyyy", Locale.US).parse(dateStr)?.time
                        7 -> SimpleDateFormat("yyyy-MM", Locale.US).parse(dateStr)?.time
                        else -> SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)?.time
                    }
                } catch (_: Exception) { null }
            }

            val targetUnit = when {
                (book.pageCount ?: 0) > 0 -> TrackingUnit.PAGES
                (book.totalVolumes ?: 0) > 0 -> TrackingUnit.VOLUMES
                (book.totalChapters ?: 0) > 0 -> TrackingUnit.CHAPTERS
                else -> entry.trackingUnit
            }

            val updatedEntry = entry.copy(
                title = cleanedOriginalTitle,
                alternateTitle = allAlts.joinToString("\n").takeIf { it.isNotBlank() } ?: entry.alternateTitle,
                externalSource = book.sourceProvider,
                externalId = book.id,
                coverImageUri = finalCover,
                totalPages = if (book.pageCount != null && book.pageCount > 0) book.pageCount else entry.totalPages,
                totalVolumes = if (book.totalVolumes != null && book.totalVolumes > 0) book.totalVolumes else entry.totalVolumes,
                totalChapters = if (book.totalChapters != null && book.totalChapters > 0) book.totalChapters else entry.totalChapters,
                trackingUnit = targetUnit,
                airedStartDate = publishEpoch ?: entry.airedStartDate,
                airedEndDate = publishEpoch ?: entry.airedEndDate ?: entry.airedStartDate,
                externalScore = book.averageScore ?: entry.externalScore,
                externalScoreSource = if (book.averageScore != null) book.sourceProvider else entry.externalScoreSource,
                description = if ((entry.description ?: entry.notes).isNullOrBlank() && !book.description.isNullOrBlank()) book.description else (entry.description ?: entry.notes),
                tags = if (entry.tags.isNullOrBlank() && book.categories.isNotEmpty()) book.categories.joinToString(", ") else entry.tags,
                updatedAt = System.currentTimeMillis()
            )

            repository.update(updatedEntry)

            val titleChanged = !cleanedOriginalTitle.equals(entry.title, ignoreCase = false)

            return ResolutionOutcome.Success(
                ResolvedItemReport(
                    entryId = entry.id,
                    originalTitle = entry.title,
                    resolvedTitle = cleanedOriginalTitle,
                    isTitleChanged = titleChanged,
                    alternateTitles = allAlts,
                    originalSource = entry.externalSource,
                    originalExternalId = entry.externalId,
                    resolvedSource = book.sourceProvider,
                    resolvedExternalId = book.id,
                    webUrl = book.infoUrl,
                    coverUpdated = coverUpdated,
                    newCoverUrl = if (coverUpdated) finalCover else null,
                    totalChapters = updatedEntry.totalChapters.takeIf { it > 0 },
                    totalVolumes = updatedEntry.totalVolumes.takeIf { it > 0 },
                    externalScore = updatedEntry.externalScore
                )
            )
        }

        return ResolutionOutcome.Skipped(
            SkippedItemReport(
                entryId = entry.id,
                title = entry.title,
                externalId = entry.externalId,
                reason = "No confident exact match found in Google Books, Open Library, or AniList Novels.",
                evaluatedCandidates = evaluatedCandidateNames
            )
        )
    }

    private suspend fun applyJikanMatchToEntry(
        entry: MediaEntry,
        matchDetails: JikanMediaDetails,
        matchSource: String,
        matchExternalId: String,
        repository: MediaRepository,
        context: Context?
    ): ResolutionOutcome.Success {
        val cleanedOriginalTitle = stripBracketedSuffixes(entry.title).takeIf { it.isNotBlank() } ?: entry.title
        val candidateTitle = matchDetails.title?.takeIf { it.isNotBlank() }

        val candidateAlts = matchDetails.alternateTitle?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val currentAlts = entry.alternateTitle?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

        val extraAlts = if (candidateTitle != null && !candidateTitle.equals(cleanedOriginalTitle, ignoreCase = true)) {
            listOf(candidateTitle)
        } else emptyList()

        val allAlts = (extraAlts + candidateAlts + currentAlts)
            .distinct()
            .filter { !it.equals(cleanedOriginalTitle, ignoreCase = true) }

        // Preserve original Tachiyomi ID in description so future backup reimports match seamlessly
        val originalTachiId = entry.externalId?.takeIf {
            !it.isNullOrBlank() && (entry.externalSource?.contains("TACHIYOMI", ignoreCase = true) == true || it.startsWith("/"))
        }
        val tachiNoteMarker = if (originalTachiId != null) "[Tachiyomi ID: $originalTachiId]" else null
        val curDesc = entry.description ?: entry.notes
        val baseDesc = if (curDesc.isNullOrBlank() && !matchDetails.synopsis.isNullOrBlank()) matchDetails.synopsis else curDesc
        val finalDesc = if (tachiNoteMarker != null && baseDesc?.contains(tachiNoteMarker) != true) {
            if (baseDesc.isNullOrBlank()) tachiNoteMarker else "$baseDesc\n\n$tachiNoteMarker"
        } else baseDesc

        val isCoverBroken = FailedCoverTracker.isEntryCoverBroken(entry)
        val needsPoster = entry.coverImageUri.isNullOrBlank() ||
                entry.coverImageUri.startsWith("file:") ||
                isCoverBroken
        val finalCover = if (needsPoster && !matchDetails.posterUrl.isNullOrBlank()) matchDetails.posterUrl else entry.coverImageUri
        val coverUpdated = finalCover != entry.coverImageUri

        if (coverUpdated && !matchDetails.posterUrl.isNullOrBlank()) {
            FailedCoverTracker.markSuccess(entry.id, matchDetails.posterUrl)
            if (context != null) {
                try {
                    val loader = Coil.imageLoader(context)
                    val req = ImageRequest.Builder(context)
                        .data(matchDetails.posterUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                    loader.enqueue(req)
                } catch (_: Exception) {}
            }
        }

        val updatedEntry = entry.copy(
            title = cleanedOriginalTitle,
            alternateTitle = allAlts.joinToString("\n").takeIf { it.isNotBlank() } ?: entry.alternateTitle,
            externalSource = matchSource,
            externalId = matchExternalId,
            coverImageUri = finalCover,
            totalChapters = if (matchDetails.totalChapters != null && matchDetails.totalChapters > 0 && entry.totalChapters == 0) matchDetails.totalChapters else entry.totalChapters,
            totalVolumes = if (matchDetails.totalVolumes != null && matchDetails.totalVolumes > 0 && entry.totalVolumes == 0) matchDetails.totalVolumes else entry.totalVolumes,
            airedStartDate = matchDetails.airedStartDate ?: entry.airedStartDate,
            airedEndDate = matchDetails.airedEndDate ?: entry.airedEndDate,
            externalScore = matchDetails.externalScore ?: entry.externalScore,
            externalScoreSource = matchDetails.externalScoreSource ?: matchSource,
            tags = if (entry.tags.isNullOrBlank() && !matchDetails.tags.isNullOrBlank()) matchDetails.tags else entry.tags,
            description = finalDesc,
            isAiring = matchDetails.isAiring ?: entry.isAiring,
            updatedAt = System.currentTimeMillis()
        )

        repository.update(updatedEntry)

        val titleChanged = !cleanedOriginalTitle.equals(entry.title, ignoreCase = false)

        return ResolutionOutcome.Success(
            ResolvedItemReport(
                entryId = entry.id,
                originalTitle = entry.title,
                resolvedTitle = cleanedOriginalTitle,
                isTitleChanged = titleChanged,
                alternateTitles = allAlts,
                originalSource = entry.externalSource,
                originalExternalId = entry.externalId,
                resolvedSource = matchSource,
                resolvedExternalId = matchExternalId,
                webUrl = matchDetails.webUrl ?: matchDetails.url,
                coverUpdated = coverUpdated,
                newCoverUrl = if (coverUpdated) finalCover else null,
                totalChapters = updatedEntry.totalChapters.takeIf { it > 0 },
                totalVolumes = updatedEntry.totalVolumes.takeIf { it > 0 },
                externalScore = updatedEntry.externalScore
            )
        )
    }

    private suspend fun matchAndResolveReading(
        entry: MediaEntry,
        repository: MediaRepository,
        context: Context?,
        skipExistingIdCheck: Boolean = false
    ): ResolutionOutcome {
        val queryTitle = entry.title.trim()
        val cleanTitle = stripBracketedSuffixes(queryTitle).takeIf { it.isNotBlank() } ?: queryTitle
        val isManhwa = isManhwaEntry(entry)
        val evaluatedCandidateNames = mutableListOf<String>()
        val combinedAltTitlesList = getCombinedAlternateTitles(entry)
        val combinedAltTitlesStr = combinedAltTitlesList.joinToString("\n").takeIf { it.isNotBlank() }

        val extIdStr = (entry.externalId ?: entry.foreignDatabaseId)?.trim()
        val srcStr = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim()
        val extractedUuid = if (!extIdStr.isNullOrBlank()) JikanSyncManager.extractMangaDexUuid(extIdStr) else null
        val existingAniListId = if (!extIdStr.isNullOrBlank() && srcStr?.equals("AniList", ignoreCase = true) == true) {
            extIdStr.toLongOrNull()?.takeIf { it > 0 }
        } else null
        val extractedMalId = if (!extIdStr.isNullOrBlank() && existingAniListId == null) {
            val isExplicitMal = srcStr?.equals("MAL", ignoreCase = true) == true || srcStr?.equals("MyAnimeList", ignoreCase = true) == true
            val hasMalUrl = extIdStr.contains("myanimelist.net", ignoreCase = true)
            if (isExplicitMal || hasMalUrl) JikanSyncManager.extractNumericMalId(extIdStr) else null
        } else null

        var resolved: ResolvedJikanMatch? = null

        // 1. Try existing external ID if present (UUID, AniList ID, or MAL ID)
        if (!skipExistingIdCheck && !extractedUuid.isNullOrBlank()) {
            val candidate = MangaDexApiClient.fetchMangaDetails(extractedUuid).getOrNull()
            if (candidate != null) {
                evaluatedCandidateNames.add("MangaDex(ID): ${candidate.title}")
                if (isConfidentExactMatch(queryTitle, candidate, combinedAltTitlesStr) ||
                    isConfidentExactMatch(cleanTitle, candidate, combinedAltTitlesStr)
                ) {
                    extractCandidateSourceAndId(candidate)?.let { (src, id) ->
                        resolved = ResolvedJikanMatch(candidate, src, id)
                    }
                }
            }
        } else if (!skipExistingIdCheck && existingAniListId != null) {
            val candidate = JikanApiClient.queryAniListById(aniListId = existingAniListId, isManga = true).getOrNull()
            if (candidate != null) {
                evaluatedCandidateNames.add("AniList(ID): ${candidate.title}")
                if (isConfidentExactMatch(queryTitle, candidate, combinedAltTitlesStr) ||
                    isConfidentExactMatch(cleanTitle, candidate, combinedAltTitlesStr)
                ) {
                    extractCandidateSourceAndId(candidate)?.let { (src, id) ->
                        resolved = ResolvedJikanMatch(candidate, src, id)
                    }
                }
            }
        } else if (!skipExistingIdCheck && extractedMalId != null && extractedMalId > 0) {
            val candidate = JikanApiClient.fetchMediaDetails(malId = extractedMalId, isManga = true, title = cleanTitle, isManhwa = isManhwa).getOrNull()
            if (candidate != null) {
                evaluatedCandidateNames.add("MAL(ID): ${candidate.title}")
                if (isConfidentExactMatch(queryTitle, candidate, combinedAltTitlesStr) ||
                    isConfidentExactMatch(cleanTitle, candidate, combinedAltTitlesStr)
                ) {
                    extractCandidateSourceAndId(candidate)?.let { (src, id) ->
                        resolved = ResolvedJikanMatch(candidate, src, id)
                    }
                }
            }
        }

        // 2. Try AniList ID extracted from description / notes (instant single-call resolve)
        if (resolved == null) {
            val notesAniListIds = extractAniListIdsFromDescription(entry.description ?: entry.notes, preferManga = true)
                .filter { it != existingAniListId }
            if (notesAniListIds.isNotEmpty()) {
                resolved = tryResolveFromAniListIds(notesAniListIds, isManga = true, evaluatedCandidateNames = evaluatedCandidateNames)
            }
        }

        // 3. Search APIs by primary title (prioritized by category: AniList -> MangaDex -> Kitsu -> Jikan for Manhwa)
        if (resolved == null) {
            resolved = searchReadingByTitle(
                searchTitle = queryTitle,
                matchTargetTitle = queryTitle,
                cleanMatchTitle = cleanTitle,
                entryAltTitles = combinedAltTitlesStr,
                isManhwa = isManhwa,
                evaluatedCandidateNames = evaluatedCandidateNames
            )
        }

        // 4. Fallback to Alternate Names (cap at top 4 to prevent slow stalls)
        if (resolved == null && combinedAltTitlesList.isNotEmpty()) {
            for (altTitle in combinedAltTitlesList.take(4)) {
                delay(150L)
                resolved = searchReadingByTitle(
                    searchTitle = altTitle,
                    matchTargetTitle = queryTitle,
                    cleanMatchTitle = cleanTitle,
                    entryAltTitles = combinedAltTitlesStr,
                    isManhwa = isManhwa,
                    evaluatedCandidateNames = evaluatedCandidateNames
                )
                if (resolved != null) break
            }
        }

        val matchDetails = resolved?.details
        val matchSource = resolved?.source
        val matchExternalId = resolved?.externalId

        // If no confident exact match was found, skip updating
        if (matchDetails == null || matchSource.isNullOrBlank() || matchExternalId.isNullOrBlank()) {
            return ResolutionOutcome.Skipped(
                SkippedItemReport(
                    entryId = entry.id,
                    title = entry.title,
                    externalId = entry.externalId,
                    reason = "No exact confident match found via name, Description AniList ID, or alternate titles in Jikan, MangaDex, AniList, or Kitsu.",
                    evaluatedCandidates = evaluatedCandidateNames
                )
            )
        }

        return applyJikanMatchToEntry(
            entry = entry,
            matchDetails = matchDetails,
            matchSource = matchSource,
            matchExternalId = matchExternalId,
            repository = repository,
            context = context
        )
    }

    private suspend fun matchAndResolveMovieOrShow(
        entry: MediaEntry,
        repository: MediaRepository,
        context: Context?,
        skipExistingIdCheck: Boolean = false
    ): ResolutionOutcome {
        val queryTitle = entry.title.trim()
        val cleanTitle = TmdbApiClient.cleanSearchTitle(queryTitle)
        val isTv = entry.mediaType == MediaType.SHOW
        val evaluatedCandidateNames = mutableListOf<String>()
        val combinedAltTitlesList = getCombinedAlternateTitles(entry)
        val combinedAltTitlesStr = combinedAltTitlesList.joinToString("\n").takeIf { it.isNotBlank() }

        var matchDetails: TmdbMediaDetails? = null

        // 1. Check existing numeric TMDB ID if present
        val extIdNum = if (!skipExistingIdCheck) (entry.externalId ?: entry.foreignDatabaseId)?.trim()?.toLongOrNull() else null
        if (extIdNum != null && extIdNum > 0) {
            val res = TmdbApiClient.fetchMediaDetails(extIdNum, isTv, context)
            if (res.isSuccess) {
                val candidate = res.getOrNull()
                if (candidate != null) {
                    evaluatedCandidateNames.add("TMDB(ID): ${candidate.title}")
                    if (isConfidentExactMatch(queryTitle, candidate, combinedAltTitlesStr) ||
                        isConfidentExactMatch(cleanTitle, candidate, combinedAltTitlesStr)
                    ) {
                        matchDetails = candidate
                    }
                }
            }
        }

        // 2. Search TMDB with primary title
        if (matchDetails == null) {
            matchDetails = searchTmdbByTitle(
                searchTitle = queryTitle,
                matchTargetTitle = queryTitle,
                cleanMatchTitle = cleanTitle,
                entryAltTitles = combinedAltTitlesStr,
                isTv = isTv,
                context = context,
                evaluatedCandidateNames = evaluatedCandidateNames
            )
        }

        // 3. Fallback to Alternate Names (from entry + Notes)
        if (matchDetails == null && combinedAltTitlesList.isNotEmpty()) {
            for (altTitle in combinedAltTitlesList.take(4)) {
                delay(150L)
                matchDetails = searchTmdbByTitle(
                    searchTitle = altTitle,
                    matchTargetTitle = queryTitle,
                    cleanMatchTitle = cleanTitle,
                    entryAltTitles = combinedAltTitlesStr,
                    isTv = isTv,
                    context = context,
                    evaluatedCandidateNames = evaluatedCandidateNames
                )
                if (matchDetails != null) break
            }
        }

        if (matchDetails == null) {
            return ResolutionOutcome.Skipped(
                SkippedItemReport(
                    entryId = entry.id,
                    title = entry.title,
                    externalId = entry.externalId,
                    reason = "No exact confident title match found in TMDB.",
                    evaluatedCandidates = evaluatedCandidateNames
                )
            )
        }

        // High confidence match confirmed!
        val cleanedOriginalTitle = stripBracketedSuffixes(entry.title).takeIf { it.isNotBlank() } ?: entry.title
        val candidateTitle = matchDetails.title?.takeIf { it.isNotBlank() }

        val candidateAlts = matchDetails.alternateTitle?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val currentAlts = entry.alternateTitle?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

        val extraAlts = if (candidateTitle != null && !candidateTitle.equals(cleanedOriginalTitle, ignoreCase = true)) {
            listOf(candidateTitle)
        } else emptyList()

        val allAlts = (extraAlts + candidateAlts + currentAlts)
            .distinct()
            .filter { !it.equals(cleanedOriginalTitle, ignoreCase = true) }

        val isCoverBroken = FailedCoverTracker.isEntryCoverBroken(entry)
        val needsPoster = entry.coverImageUri.isNullOrBlank() ||
                entry.coverImageUri.startsWith("file:") ||
                isCoverBroken
        val finalCover = if (needsPoster && !matchDetails.posterUrl.isNullOrBlank()) matchDetails.posterUrl else entry.coverImageUri
        val coverUpdated = finalCover != entry.coverImageUri

        if (coverUpdated && !matchDetails.posterUrl.isNullOrBlank()) {
            FailedCoverTracker.markSuccess(entry.id, matchDetails.posterUrl)
            if (context != null) {
                try {
                    val loader = Coil.imageLoader(context)
                    val req = ImageRequest.Builder(context)
                        .data(matchDetails.posterUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                    loader.enqueue(req)
                } catch (_: Exception) {}
            }
        }

        val isMovie = entry.mediaType == MediaType.MOVIE || matchDetails.type.equals("Movie", ignoreCase = true)
        val duration = if (isMovie && matchDetails.durationMinutes != null && matchDetails.durationMinutes > 0 && (entry.durationMinutes == null || entry.durationMinutes == 0)) {
            matchDetails.durationMinutes
        } else entry.durationMinutes

        val episodeLength = if (!isMovie && matchDetails.durationMinutes != null && matchDetails.durationMinutes > 0 && (entry.episodeLengthMinutes == null || entry.episodeLengthMinutes == 0)) {
            matchDetails.durationMinutes
        } else entry.episodeLengthMinutes

        val episodes = if (matchDetails.totalEpisodes != null && matchDetails.totalEpisodes > 0 &&
            (entry.totalEpisodes == 0 || (entry.totalSeasons > 1 && entry.totalEpisodes <= 1))
        ) matchDetails.totalEpisodes else entry.totalEpisodes
        val seasonsCount = if (entry.mediaType == MediaType.SHOW && matchDetails.totalSeasons != null && matchDetails.totalSeasons > 0 && entry.totalSeasons <= 1) {
            matchDetails.totalSeasons
        } else entry.totalSeasons
        val seasonEpsJson = if (entry.mediaType == MediaType.SHOW && matchDetails.seasonEpisodes != null && !entry.hasSeasonEpisodeTracking()) {
            MediaEntry.formatSeasonEpisodes(matchDetails.seasonEpisodes) ?: entry.seasonEpisodesJson
        } else entry.seasonEpisodesJson

        val updatedEntry = entry.copy(
            title = cleanedOriginalTitle,
            alternateTitle = allAlts.joinToString("\n").takeIf { it.isNotBlank() } ?: entry.alternateTitle,
            externalSource = "TMDB",
            externalId = matchDetails.tmdbId.toString(),
            coverImageUri = finalCover,
            durationMinutes = duration,
            episodeLengthMinutes = episodeLength,
            totalEpisodes = episodes,
            totalSeasons = seasonsCount,
            seasonEpisodesJson = seasonEpsJson,
            airedStartDate = matchDetails.airedStartDate ?: entry.airedStartDate,
            airedEndDate = matchDetails.airedEndDate ?: entry.airedEndDate,
            externalScore = matchDetails.externalScore ?: entry.externalScore,
            externalScoreSource = "TMDB",
            tags = if (entry.tags.isNullOrBlank() && !matchDetails.tags.isNullOrBlank()) matchDetails.tags else entry.tags,
            description = if ((entry.description ?: entry.notes).isNullOrBlank() && !matchDetails.synopsis.isNullOrBlank()) matchDetails.synopsis else (entry.description ?: entry.notes),
            isAiring = matchDetails.isAiring ?: entry.isAiring,
            updatedAt = System.currentTimeMillis()
        )

        repository.update(updatedEntry)

        val titleChanged = !cleanedOriginalTitle.equals(entry.title, ignoreCase = false)
        val webUrl = if (entry.mediaType == MediaType.SHOW || matchDetails.type == "TV") {
            "https://www.themoviedb.org/tv/${matchDetails.tmdbId}"
        } else {
            "https://www.themoviedb.org/movie/${matchDetails.tmdbId}"
        }

        return ResolutionOutcome.Success(
            ResolvedItemReport(
                entryId = entry.id,
                originalTitle = entry.title,
                resolvedTitle = cleanedOriginalTitle,
                isTitleChanged = titleChanged,
                alternateTitles = allAlts,
                originalSource = entry.externalSource,
                originalExternalId = entry.externalId,
                resolvedSource = "TMDB",
                resolvedExternalId = matchDetails.tmdbId.toString(),
                webUrl = webUrl,
                coverUpdated = coverUpdated,
                newCoverUrl = if (coverUpdated) finalCover else null,
                totalChapters = null,
                totalVolumes = null,
                externalScore = updatedEntry.externalScore
            )
        )
    }

    private suspend fun matchAndResolveAnime(
        entry: MediaEntry,
        repository: MediaRepository,
        context: Context?,
        skipExistingIdCheck: Boolean = false
    ): ResolutionOutcome {
        val queryTitle = entry.title.trim()
        val cleanTitle = stripBracketedSuffixes(queryTitle).takeIf { it.isNotBlank() } ?: queryTitle
        val evaluatedCandidateNames = mutableListOf<String>()
        val combinedAltTitlesList = getCombinedAlternateTitles(entry)
        val combinedAltTitlesStr = combinedAltTitlesList.joinToString("\n").takeIf { it.isNotBlank() }

        val extIdStr = (entry.externalId ?: entry.foreignDatabaseId)?.trim()
        val srcStr = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim()
        val existingAniListId = if (!extIdStr.isNullOrBlank() && srcStr?.equals("AniList", ignoreCase = true) == true) {
            extIdStr.toLongOrNull()?.takeIf { it > 0 }
        } else null
        val extractedMalId = if (!extIdStr.isNullOrBlank() && existingAniListId == null) {
            val isExplicitMal = srcStr?.equals("MAL", ignoreCase = true) == true || srcStr?.equals("MyAnimeList", ignoreCase = true) == true
            val hasMalUrl = extIdStr.contains("myanimelist.net", ignoreCase = true)
            if (isExplicitMal || hasMalUrl) JikanSyncManager.extractNumericMalId(extIdStr) else null
        } else null

        var resolved: ResolvedJikanMatch? = null

        // 1. Try existing ID (AniList or MAL)
        if (!skipExistingIdCheck && existingAniListId != null) {
            val candidate = JikanApiClient.queryAniListById(aniListId = existingAniListId, isManga = false).getOrNull()
            if (candidate != null) {
                evaluatedCandidateNames.add("AniList(ID): ${candidate.title}")
                if (isConfidentExactMatch(queryTitle, candidate, combinedAltTitlesStr) ||
                    isConfidentExactMatch(cleanTitle, candidate, combinedAltTitlesStr)
                ) {
                    extractCandidateSourceAndId(candidate)?.let { (src, id) ->
                        resolved = ResolvedJikanMatch(candidate, src, id)
                    }
                }
            }
        } else if (!skipExistingIdCheck && extractedMalId != null && extractedMalId > 0) {
            val candidate = JikanApiClient.fetchMediaDetails(malId = extractedMalId, isManga = false, title = cleanTitle).getOrNull()
            if (candidate != null) {
                evaluatedCandidateNames.add("MAL(ID): ${candidate.title}")
                if (isConfidentExactMatch(queryTitle, candidate, combinedAltTitlesStr) ||
                    isConfidentExactMatch(cleanTitle, candidate, combinedAltTitlesStr)
                ) {
                    extractCandidateSourceAndId(candidate)?.let { (src, id) ->
                        resolved = ResolvedJikanMatch(candidate, src, id)
                    }
                }
            }
        }

        // 2. Try AniList ID extracted from description (instant single-call resolve)
        if (resolved == null) {
            val notesAniListIds = extractAniListIdsFromDescription(entry.description ?: entry.notes, preferManga = false)
                .filter { it != existingAniListId }
            if (notesAniListIds.isNotEmpty()) {
                resolved = tryResolveFromAniListIds(notesAniListIds, isManga = false, evaluatedCandidateNames = evaluatedCandidateNames)
            }
        }

        // 3. Search APIs by primary title (AniList -> Jikan)
        if (resolved == null) {
            resolved = searchAnimeByTitle(
                searchTitle = queryTitle,
                matchTargetTitle = queryTitle,
                cleanMatchTitle = cleanTitle,
                entryAltTitles = combinedAltTitlesStr,
                evaluatedCandidateNames = evaluatedCandidateNames
            )
        }

        // 4. Fallback to Alternate Names (cap at top 4)
        if (resolved == null && combinedAltTitlesList.isNotEmpty()) {
            for (altTitle in combinedAltTitlesList.take(4)) {
                delay(150L)
                resolved = searchAnimeByTitle(
                    searchTitle = altTitle,
                    matchTargetTitle = queryTitle,
                    cleanMatchTitle = cleanTitle,
                    entryAltTitles = combinedAltTitlesStr,
                    evaluatedCandidateNames = evaluatedCandidateNames
                )
                if (resolved != null) break
            }
        }

        val matchDetails = resolved?.details
        val matchSource = resolved?.source
        val matchExternalId = resolved?.externalId

        if (matchDetails == null || matchSource.isNullOrBlank() || matchExternalId.isNullOrBlank()) {
            return ResolutionOutcome.Skipped(
                SkippedItemReport(
                    entryId = entry.id,
                    title = entry.title,
                    externalId = entry.externalId,
                    reason = "No exact confident match found via name, Description AniList ID, or alternate titles in Jikan or AniList for anime.",
                    evaluatedCandidates = evaluatedCandidateNames
                )
            )
        }

        val cleanedOriginalTitle = stripBracketedSuffixes(entry.title).takeIf { it.isNotBlank() } ?: entry.title
        val candidateTitle = matchDetails.title?.takeIf { it.isNotBlank() }

        val candidateAlts = matchDetails.alternateTitle?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val currentAlts = entry.alternateTitle?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

        val extraAlts = if (candidateTitle != null && !candidateTitle.equals(cleanedOriginalTitle, ignoreCase = true)) {
            listOf(candidateTitle)
        } else emptyList()

        val allAlts = (extraAlts + candidateAlts + currentAlts)
            .distinct()
            .filter { !it.equals(cleanedOriginalTitle, ignoreCase = true) }

        val isCoverBroken = FailedCoverTracker.isEntryCoverBroken(entry)
        val needsPoster = entry.coverImageUri.isNullOrBlank() ||
                entry.coverImageUri.startsWith("file:") ||
                isCoverBroken
        val finalCover = if (needsPoster && !matchDetails.posterUrl.isNullOrBlank()) matchDetails.posterUrl else entry.coverImageUri
        val coverUpdated = finalCover != entry.coverImageUri

        if (coverUpdated && !matchDetails.posterUrl.isNullOrBlank()) {
            FailedCoverTracker.markSuccess(entry.id, matchDetails.posterUrl)
            if (context != null) {
                try {
                    val loader = Coil.imageLoader(context)
                    val req = ImageRequest.Builder(context)
                        .data(matchDetails.posterUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                    loader.enqueue(req)
                } catch (_: Exception) {}
            }
        }

        val updatedEntry = entry.copy(
            title = cleanedOriginalTitle,
            alternateTitle = allAlts.joinToString("\n").takeIf { it.isNotBlank() } ?: entry.alternateTitle,
            externalSource = matchSource,
            externalId = matchExternalId,
            coverImageUri = finalCover,
            totalEpisodes = if (matchDetails.totalEpisodes != null && matchDetails.totalEpisodes > 0 && entry.totalEpisodes == 0) matchDetails.totalEpisodes else entry.totalEpisodes,
            durationMinutes = if (matchDetails.durationMinutes != null && matchDetails.durationMinutes > 0 && (entry.durationMinutes == null || entry.durationMinutes == 0)) matchDetails.durationMinutes else entry.durationMinutes,
            episodeLengthMinutes = if (matchDetails.durationMinutes != null && matchDetails.durationMinutes > 0 && (entry.episodeLengthMinutes == null || entry.episodeLengthMinutes == 0)) matchDetails.durationMinutes else entry.episodeLengthMinutes,
            airedStartDate = matchDetails.airedStartDate ?: entry.airedStartDate,
            airedEndDate = matchDetails.airedEndDate ?: entry.airedEndDate,
            externalScore = matchDetails.externalScore ?: entry.externalScore,
            externalScoreSource = matchDetails.externalScoreSource ?: matchSource,
            tags = if (entry.tags.isNullOrBlank() && !matchDetails.tags.isNullOrBlank()) matchDetails.tags else entry.tags,
            description = if ((entry.description ?: entry.notes).isNullOrBlank() && !matchDetails.synopsis.isNullOrBlank()) matchDetails.synopsis else (entry.description ?: entry.notes),
            isAiring = matchDetails.isAiring ?: entry.isAiring,
            updatedAt = System.currentTimeMillis()
        )

        repository.update(updatedEntry)

        val titleChanged = !cleanedOriginalTitle.equals(entry.title, ignoreCase = false)

        return ResolutionOutcome.Success(
            ResolvedItemReport(
                entryId = entry.id,
                originalTitle = entry.title,
                resolvedTitle = cleanedOriginalTitle,
                isTitleChanged = titleChanged,
                alternateTitles = allAlts,
                originalSource = entry.externalSource,
                originalExternalId = entry.externalId,
                resolvedSource = matchSource,
                resolvedExternalId = matchExternalId,
                webUrl = matchDetails.webUrl ?: matchDetails.url,
                coverUpdated = coverUpdated,
                newCoverUrl = if (coverUpdated) finalCover else null,
                totalChapters = null,
                totalVolumes = null,
                externalScore = updatedEntry.externalScore
            )
        )
    }

    private fun generateResolutionReport(
        context: Context,
        profileName: String,
        profileId: Long,
        totalProcessed: Int,
        resolvedItems: List<ResolvedItemReport>,
        skippedItems: List<SkippedItemReport>,
        failedItems: List<FailedItemReport>
    ): String {
        val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val fileTimeFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val now = Date()
        val timeStr = timestampFormat.format(now)
        val fileTimeStr = fileTimeFormat.format(now)

        val sb = StringBuilder()
        sb.appendLine("================================================================================")
        sb.appendLine("                 LX TRACK LIST - ID RESOLUTION REPORT")
        sb.appendLine("================================================================================")
        sb.appendLine("Generated: $timeStr")
        sb.appendLine("Profile:   $profileName (ID: $profileId)")
        sb.appendLine("Processed: $totalProcessed")
        sb.appendLine("• Successfully Resolved & Linked: ${resolvedItems.size}")
        sb.appendLine("• Skipped (No Exact Match):       ${skippedItems.size}")
        sb.appendLine("• Failures / Errors:             ${failedItems.size}")
        sb.appendLine("================================================================================")
        sb.appendLine()

        // 1. Resolved Section
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("1. RESOLVED ENTRIES (${resolvedItems.size})")
        sb.appendLine("--------------------------------------------------------------------------------")
        if (resolvedItems.isEmpty()) {
            sb.appendLine("(No entries were resolved)")
        } else {
            resolvedItems.forEachIndexed { idx, item ->
                sb.appendLine("[${idx + 1}] ${item.originalTitle}")
                sb.appendLine("    • Original Source/ID: ${item.originalSource ?: "none"} (${item.originalExternalId ?: "none"})")
                sb.appendLine("    • Linked Provider:    ${item.resolvedSource} (#${item.resolvedExternalId})")
                if (!item.webUrl.isNullOrBlank()) {
                    sb.appendLine("    • Database URL:       ${item.webUrl}")
                }
                if (item.isTitleChanged) {
                    sb.appendLine("    • Name Changed:       YES (\"${item.originalTitle}\" -> \"${item.resolvedTitle}\")")
                } else {
                    sb.appendLine("    • Name Changed:       NO (\"${item.resolvedTitle}\")")
                }
                if (item.alternateTitles.isNotEmpty()) {
                    sb.appendLine("    • Alternate Names:    ${item.alternateTitles.joinToString(", ")}")
                }
                if (item.coverUpdated) {
                    sb.appendLine("    • Cover Updated:      YES (${item.newCoverUrl})")
                } else {
                    sb.appendLine("    • Cover Updated:      NO")
                }
                if (item.totalChapters != null) {
                    sb.appendLine("    • Chapters:           ${item.totalChapters}")
                }
                if (item.externalScore != null) {
                    sb.appendLine("    • External Rating:    ★ ${item.externalScore} (${item.resolvedSource})")
                }
                sb.appendLine()
            }
        }

        // 2. Skipped Section
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("2. SKIPPED / UNMATCHED ENTRIES (${skippedItems.size})")
        sb.appendLine("--------------------------------------------------------------------------------")
        if (skippedItems.isEmpty()) {
            sb.appendLine("(No entries were skipped)")
        } else {
            skippedItems.forEachIndexed { idx, item ->
                sb.appendLine("[${idx + 1}] ${item.title}")
                sb.appendLine("    • External ID:  ${item.externalId ?: "none"}")
                sb.appendLine("    • Reason:       ${item.reason}")
                if (item.evaluatedCandidates.isNotEmpty()) {
                    sb.appendLine("    • Candidates:   ${item.evaluatedCandidates.take(6).joinToString("; ")}")
                }
                sb.appendLine()
            }
        }

        // 3. Failed Section
        if (failedItems.isNotEmpty()) {
            sb.appendLine("--------------------------------------------------------------------------------")
            sb.appendLine("3. ERRORS / FAILURES (${failedItems.size})")
            sb.appendLine("--------------------------------------------------------------------------------")
            failedItems.forEachIndexed { idx, item ->
                sb.appendLine("[${idx + 1}] ${item.title} (${item.externalId})")
                sb.appendLine("    • Error: ${item.errorMessage}")
                sb.appendLine()
            }
        }

        sb.appendLine("================================================================================")
        sb.appendLine("End of Report")
        sb.appendLine("================================================================================")

        val reportText = sb.toString()
        val textFileName = "ID_Resolution_Report_$fileTimeStr.txt"
        val jsonFileName = "ID_Resolution_Report_$fileTimeStr.json"
        val legacyTextFileName = "Tachiyomi_Resolution_Report_$fileTimeStr.txt"

        var savedPath = ""

        // Write to primary /storage/emulated/0/LXTrackList/data/
        try {
            val dataFolder = StorageHelper.getDataFolder()
            val textFile = File(dataFolder, textFileName)
            FileOutputStream(textFile).use { it.write(reportText.toByteArray(Charsets.UTF_8)) }

            // Also keep legacy name so existing file references remain valid
            val legacyFile = File(dataFolder, legacyTextFileName)
            FileOutputStream(legacyFile).use { it.write(reportText.toByteArray(Charsets.UTF_8)) }

            val jsonReport = mapOf(
                "timestamp" to timeStr,
                "profile" to profileName,
                "profileId" to profileId,
                "totalProcessed" to totalProcessed,
                "resolvedCount" to resolvedItems.size,
                "skippedCount" to skippedItems.size,
                "failedCount" to failedItems.size,
                "resolved" to resolvedItems,
                "skipped" to skippedItems,
                "failed" to failedItems
            )
            val jsonFile = File(dataFolder, jsonFileName)
            FileOutputStream(jsonFile).use { it.write(gson.toJson(jsonReport).toByteArray(Charsets.UTF_8)) }

            savedPath = textFile.absolutePath
            Log.i(TAG, "ID resolution report written to $savedPath")
        } catch (e: Exception) {
            Log.e(TAG, "Could not write to external data folder", e)
        }

        // Always also save backup in context.filesDir/data/
        try {
            val internalDataDir = File(context.filesDir, "data").apply { if (!exists()) mkdirs() }
            val internalTextFile = File(internalDataDir, textFileName)
            FileOutputStream(internalTextFile).use { it.write(reportText.toByteArray(Charsets.UTF_8)) }
            val internalLegacyFile = File(internalDataDir, legacyTextFileName)
            FileOutputStream(internalLegacyFile).use { it.write(reportText.toByteArray(Charsets.UTF_8)) }
            if (savedPath.isBlank()) {
                savedPath = internalTextFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not write to internal data folder", e)
        }

        return savedPath
    }
}
