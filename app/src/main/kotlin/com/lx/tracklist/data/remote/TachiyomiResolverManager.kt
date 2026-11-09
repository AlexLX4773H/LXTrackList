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
    private const val RATE_LIMIT_DELAY_MS = 650L
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
     * Strips common brackets / suffixes like (Manhwa), [Official], (Webtoon), [Color]
     */
    fun stripBracketedSuffixes(title: String): String {
        return title
            .replace(Regex("(?i)\\s*[\\[\\(](?:official|manhwa|webtoon|manga|novel|comic|colored|color|uncensored|censored|digital|raw|hd|hq)[\\]\\)]"), "")
            .replace(Regex("\\s*[\\[\\(].*?[\\]\\)]\\s*$"), "")
            .trim()
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
        candidate: JikanMediaDetails,
        entryAltTitles: String? = null
    ): Boolean {
        if (isSingleTitleConfidentMatch(queryTitle, candidate)) return true

        if (!entryAltTitles.isNullOrBlank()) {
            val entryAlts = entryAltTitles.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            for (alt in entryAlts) {
                if (isSingleTitleConfidentMatch(alt, candidate)) return true
            }
        }

        return false
    }

    private fun isSingleTitleConfidentMatch(queryTitle: String, candidate: JikanMediaDetails): Boolean {
        val normQuery = normalizeTitle(queryTitle)
        if (normQuery.isBlank()) return false

        val cleanQuery = stripBracketedSuffixes(queryTitle)
        val normCleanQuery = normalizeTitle(cleanQuery)

        val normCandidateTitle = normalizeTitle(candidate.title)
        val cleanCandidateTitle = stripBracketedSuffixes(candidate.title ?: "")
        val normCleanCandidate = normalizeTitle(cleanCandidateTitle)

        // 1. Primary title match
        if (isExactOrSpaceAgnosticMatch(normQuery, normCleanQuery, normCandidateTitle, normCleanCandidate)) {
            return true
        }

        // 2. Candidate alternate titles & synonyms
        val altTitles = candidate.alternateTitle?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
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

            val eligible = pool.filter { isEligibleTachiyomiEntry(it) }
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
                statusMessage = if (total > 0) "Starting Tachiyomi ID resolution ($total items)..." else "No Tachiyomi entries found."
            )

            if (total == 0) {
                _resolveState.update { it.copy(isRunning = false, isCompleted = true, statusMessage = "No Tachiyomi entries needed resolution.") }
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

            for (entry in eligible) {
                if (resolveJob?.isCancelled == true) break

                _resolveState.update {
                    it.copy(
                        currentTitle = entry.title,
                        statusMessage = "Matching \"${entry.title}\" across databases..."
                    )
                }

                NotificationHelper.showTachiyomiResolveProgress(context, completed, total, entry.title, profileName)
                delay(RATE_LIMIT_DELAY_MS)

                try {
                    val outcome = matchAndResolveEntry(entry, repository, context)
                    when (outcome) {
                        is ResolutionOutcome.Success -> {
                            resolvedCount++
                            resolvedItems.add(outcome.report)
                        }
                        is ResolutionOutcome.Skipped -> {
                            skippedCount++
                            skippedItems.add(outcome.report)
                        }
                        is ResolutionOutcome.Failed -> {
                            failedCount++
                            failedItems.add(outcome.report)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failedCount++
                    failedItems.add(
                        FailedItemReport(
                            entryId = entry.id,
                            title = entry.title,
                            externalId = entry.externalId,
                            errorMessage = e.message ?: e.localizedMessage ?: "Unknown error"
                        )
                    )
                }

                completed++
                _resolveState.update {
                    it.copy(
                        completedCount = completed,
                        pendingCount = total - completed,
                        resolvedCount = resolvedCount,
                        skippedCount = skippedCount,
                        failedCount = failedCount
                    )
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

    sealed class ResolutionOutcome {
        data class Success(val report: ResolvedItemReport) : ResolutionOutcome()
        data class Skipped(val report: SkippedItemReport) : ResolutionOutcome()
        data class Failed(val report: FailedItemReport) : ResolutionOutcome()
    }

    suspend fun matchAndResolveEntry(
        entry: MediaEntry,
        repository: MediaRepository,
        context: Context?
    ): ResolutionOutcome {
        val queryTitle = entry.title.trim()
        val isManhwa = entry.mediaType == MediaType.MANHWA
        val evaluatedCandidateNames = mutableListOf<String>()

        val extIdStr = (entry.externalId ?: entry.foreignDatabaseId)?.trim()
        val extractedUuid = if (!extIdStr.isNullOrBlank()) JikanSyncManager.extractMangaDexUuid(extIdStr) else null
        val extractedMalId = if (!extIdStr.isNullOrBlank()) {
            val src = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim()
            val isExplicitMal = src?.equals("MAL", ignoreCase = true) == true || src?.equals("MyAnimeList", ignoreCase = true) == true
            val hasMalUrl = extIdStr.contains("myanimelist.net", ignoreCase = true)
            if (isExplicitMal || hasMalUrl) JikanSyncManager.extractNumericMalId(extIdStr) else null
        } else null

        var matchDetails: JikanMediaDetails? = null
        var matchSource: String? = null
        var matchExternalId: String? = null

        // 1. Try extracted ID if present (UUID or MAL ID)
        if (!extractedUuid.isNullOrBlank()) {
            val res = MangaDexApiClient.fetchMangaDetails(extractedUuid)
            if (res.isSuccess) {
                val candidate = res.getOrNull()
                if (candidate != null) {
                    evaluatedCandidateNames.add("MangaDex(ID): ${candidate.title}")
                    if (isConfidentExactMatch(queryTitle, candidate, entry.alternateTitle)) {
                        val srcAndId = extractCandidateSourceAndId(candidate)
                        if (srcAndId != null) {
                            matchDetails = candidate
                            matchSource = srcAndId.first
                            matchExternalId = srcAndId.second
                        }
                    }
                }
            }
        } else if (extractedMalId != null && extractedMalId > 0) {
            val res = JikanApiClient.fetchMediaDetails(malId = extractedMalId, isManga = true, title = queryTitle, isManhwa = isManhwa)
            if (res.isSuccess) {
                val candidate = res.getOrNull()
                if (candidate != null) {
                    evaluatedCandidateNames.add("MAL(ID): ${candidate.title}")
                    if (isConfidentExactMatch(queryTitle, candidate, entry.alternateTitle)) {
                        val srcAndId = extractCandidateSourceAndId(candidate)
                        if (srcAndId != null) {
                            matchDetails = candidate
                            matchSource = srcAndId.first
                            matchExternalId = srcAndId.second
                        }
                    }
                }
            }
        }

        // 2. Query Jikan (MyAnimeList, cascades to MangaDex/AniList/Kitsu if needed)
        if (matchDetails == null) {
            val jikanRes = JikanApiClient.searchJikanMultiple(
                queryTitle = queryTitle,
                isManga = true,
                subType = if (isManhwa) "manhwa" else null,
                limit = 5
            )
            if (jikanRes.isSuccess) {
                val candidates = jikanRes.getOrDefault(emptyList())
                for (cand in candidates) {
                    evaluatedCandidateNames.add("${cand.sourceProvider}: ${cand.title}")
                    if (isConfidentExactMatch(queryTitle, cand, entry.alternateTitle)) {
                        val srcAndId = extractCandidateSourceAndId(cand)
                        if (srcAndId != null) {
                            matchDetails = cand
                            matchSource = srcAndId.first
                            matchExternalId = srcAndId.second
                            break
                        }
                    }
                }
            }
        }

        // 3. Query MangaDex
        if (matchDetails == null) {
            val mdRes = MangaDexApiClient.searchMangaMultiple(queryTitle, isManhwa = isManhwa, limit = 5)
            if (mdRes.isSuccess) {
                val candidates = mdRes.getOrDefault(emptyList())
                for (cand in candidates) {
                    evaluatedCandidateNames.add("MangaDex: ${cand.title}")
                    if (isConfidentExactMatch(queryTitle, cand, entry.alternateTitle)) {
                        val srcAndId = extractCandidateSourceAndId(cand)
                        if (srcAndId != null) {
                            matchDetails = cand
                            matchSource = srcAndId.first
                            matchExternalId = srcAndId.second
                            break
                        }
                    }
                }
            }
        }

        // 4. Query AniList
        if (matchDetails == null) {
            val aniRes = JikanApiClient.queryAniListMultiple(
                searchTitle = queryTitle,
                isManga = true,
                subType = if (isManhwa) "MANHWA" else null,
                limit = 5
            )
            if (aniRes.isSuccess) {
                val candidates = aniRes.getOrDefault(emptyList())
                for (cand in candidates) {
                    evaluatedCandidateNames.add("AniList: ${cand.title}")
                    if (isConfidentExactMatch(queryTitle, cand, entry.alternateTitle)) {
                        val srcAndId = extractCandidateSourceAndId(cand)
                        if (srcAndId != null) {
                            matchDetails = cand
                            matchSource = srcAndId.first
                            matchExternalId = srcAndId.second
                            break
                        }
                    }
                }
            }
        }

        // 5. Query Kitsu
        if (matchDetails == null) {
            val kitsuRes = KitsuApiClient.searchMangaMultiple(queryTitle, limit = 5)
            if (kitsuRes.isSuccess) {
                val candidates = kitsuRes.getOrDefault(emptyList())
                for (cand in candidates) {
                    evaluatedCandidateNames.add("Kitsu: ${cand.title}")
                    if (isConfidentExactMatch(queryTitle, cand, entry.alternateTitle)) {
                        val srcAndId = extractCandidateSourceAndId(cand)
                        if (srcAndId != null) {
                            matchDetails = cand
                            matchSource = srcAndId.first
                            matchExternalId = srcAndId.second
                            break
                        }
                    }
                }
            }
        }

        // If no confident exact match was found, skip updating
        if (matchDetails == null || matchSource.isNullOrBlank() || matchExternalId.isNullOrBlank()) {
            return ResolutionOutcome.Skipped(
                SkippedItemReport(
                    entryId = entry.id,
                    title = entry.title,
                    externalId = entry.externalId,
                    reason = "No exact confident title match found in Jikan, MangaDex, AniList, or Kitsu.",
                    evaluatedCandidates = evaluatedCandidateNames
                )
            )
        }

        // High confidence match confirmed!
        val resolvedTitle = matchDetails.title?.takeIf { it.isNotBlank() } ?: entry.title
        val titleChanged = !resolvedTitle.equals(entry.title, ignoreCase = false)

        val candidateAlts = matchDetails.alternateTitle?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val currentAlts = entry.alternateTitle?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val allAlts = (candidateAlts + currentAlts + if (titleChanged) listOf(entry.title) else emptyList())
            .distinct()
            .filter { !it.equals(resolvedTitle, ignoreCase = true) }

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
            title = resolvedTitle,
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
            notes = if (entry.notes.isNullOrBlank() && !matchDetails.synopsis.isNullOrBlank()) matchDetails.synopsis else entry.notes,
            isAiring = matchDetails.isAiring ?: entry.isAiring,
            updatedAt = System.currentTimeMillis()
        )

        repository.update(updatedEntry)

        return ResolutionOutcome.Success(
            ResolvedItemReport(
                entryId = entry.id,
                originalTitle = entry.title,
                resolvedTitle = resolvedTitle,
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
        sb.appendLine("           LX TRACK LIST - TACHIYOMI ID RESOLUTION REPORT")
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
                sb.appendLine("    • Original Source/ID: ${item.originalSource ?: "TACHIYOMI"} (${item.originalExternalId ?: "none"})")
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
                sb.appendLine("    • Tachiyomi ID: ${item.externalId ?: "unknown"}")
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
        val textFileName = "Tachiyomi_Resolution_Report_$fileTimeStr.txt"
        val jsonFileName = "Tachiyomi_Resolution_Report_$fileTimeStr.json"

        var savedPath = ""

        // Write to primary /storage/emulated/0/LXTrackList/data/
        try {
            val dataFolder = StorageHelper.getDataFolder()
            val textFile = File(dataFolder, textFileName)
            FileOutputStream(textFile).use { it.write(reportText.toByteArray(Charsets.UTF_8)) }

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
            Log.i(TAG, "Tachiyomi resolution report written to $savedPath")
        } catch (e: Exception) {
            Log.e(TAG, "Could not write to external data folder", e)
        }

        // Always also save backup in context.filesDir/data/
        try {
            val internalDataDir = File(context.filesDir, "data").apply { if (!exists()) mkdirs() }
            val internalTextFile = File(internalDataDir, textFileName)
            FileOutputStream(internalTextFile).use { it.write(reportText.toByteArray(Charsets.UTF_8)) }
            if (savedPath.isBlank()) {
                savedPath = internalTextFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not write to internal data folder", e)
        }

        return savedPath
    }
}
