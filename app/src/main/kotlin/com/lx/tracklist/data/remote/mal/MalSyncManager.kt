package com.lx.tracklist.data.remote.mal

import android.content.Context
import android.util.Log
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.repository.ConflictStrategy
import com.lx.tracklist.data.repository.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class MalSyncProgress(
    val isRunning: Boolean = false,
    val currentTitle: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val statusMessage: String? = null,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
) {
    val progressFraction: Float
        get() = if (totalCount > 0) (completedCount.toFloat() / totalCount).coerceIn(0f, 1f) else 0f
}

object MalSyncManager {

    private const val TAG = "MalSyncManager"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var pullJob: Job? = null

    private val _syncState = MutableStateFlow(MalSyncProgress())
    val syncState: StateFlow<MalSyncProgress> = _syncState.asStateFlow()

    /**
     * Checks if a media entry is structurally eligible to be linked with MyAnimeList.
     */
    fun canBeExternallyTracked(entry: MediaEntry): Boolean {
        val src = entry.externalSource?.trim()?.uppercase(Locale.US)
        val hasMalSource = src == "MAL" || src == "MYANIMELIST"
        val hasValidId = !entry.externalId.isNullOrBlank() && entry.externalId.toLongOrNull() != null
        val isSupportedType = entry.mediaType == MediaType.ANIME ||
                              entry.mediaType == MediaType.MANGA ||
                              entry.mediaType == MediaType.MANHWA ||
                              (entry.mediaType == MediaType.BOOK && hasMalSource)
        return hasMalSource && hasValidId && isSupportedType
    }

    /**
     * Checks if a media entry is enabled and eligible to be pushed to MyAnimeList.
     * By default entries are not synced; they must be explicitly added to External Tracking.
     */
    fun isEligibleForMalSync(entry: MediaEntry): Boolean {
        return entry.isExternallyTracked && canBeExternallyTracked(entry)
    }

    /**
     * Pushes a local entry update (progress, score, status, rewatch) to MyAnimeList in the background.
     */
    fun pushUpdateInBackground(context: Context, entry: MediaEntry) {
        if (!isEligibleForMalSync(entry)) return

        scope.launch {
            try {
                pushUpdateSync(context, entry)
            } catch (e: Exception) {
                Log.w(TAG, "Background push to MAL failed for entry: ${entry.title}", e)
            }
        }
    }

    /**
     * Suspended push of a single entry to MyAnimeList.
     * When [force] is true, pushes even if automatic background sync is turned off in settings.
     */
    suspend fun pushUpdateSync(
        context: Context,
        entry: MediaEntry,
        force: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val tokenStore = MalTokenStore(context)
        val userId = if (entry.userId > 0L) entry.userId else null
        if (userId == null) {
            val msg = "Cannot push to MAL: entry '${entry.title}' has no valid profile userId assigned"
            Log.w(TAG, msg)
            return@withContext if (force) Result.failure(Exception(msg)) else Result.success(Unit)
        }

        if (!tokenStore.isLoggedIn(userId)) {
            val msg = "MyAnimeList account is not connected for this profile."
            return@withContext if (force) Result.failure(Exception(msg)) else Result.success(Unit)
        }

        if (!force && !tokenStore.isAutoSyncEnabled(userId)) {
            return@withContext Result.success(Unit) // Auto-sync disabled for background triggers
        }

        val malId = entry.externalId?.toLongOrNull()
        if (malId == null) {
            val msg = "Entry has no valid numeric MyAnimeList ID."
            return@withContext if (force) Result.failure(Exception(msg)) else Result.success(Unit)
        }

        when (entry.mediaType) {
            MediaType.ANIME -> {
                val malStatus = mapStatusToMal(entry.status, isAnime = true)
                val result = MalApiClient.updateAnimeStatus(
                    context = context,
                    userId = userId,
                    malAnimeId = malId,
                    status = malStatus,
                    numWatchedEpisodes = entry.currentEpisodes,
                    score = entry.score,
                    isRewatching = entry.isRewatching,
                    numTimesRewatched = entry.rewatchCount
                )
                if (result.isSuccess) {
                    Log.d(TAG, "Successfully synced anime '${entry.title}' (Ep ${entry.currentEpisodes}) to MAL")
                    tokenStore.setLastSyncTimestamp(userId, System.currentTimeMillis())
                    Result.success(Unit)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Unknown MAL anime update error"))
                }
            }
            MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK -> {
                val malStatus = mapStatusToMal(entry.status, isAnime = false)
                val result = MalApiClient.updateMangaStatus(
                    context = context,
                    userId = userId,
                    malMangaId = malId,
                    status = malStatus,
                    numVolumesRead = entry.currentVolumes,
                    numChaptersRead = entry.currentChapters,
                    score = entry.score,
                    isRereading = entry.isRewatching,
                    numTimesReread = entry.rewatchCount
                )
                if (result.isSuccess) {
                    Log.d(TAG, "Successfully synced manga '${entry.title}' (Ch ${entry.currentChapters}) to MAL")
                    tokenStore.setLastSyncTimestamp(userId, System.currentTimeMillis())
                    Result.success(Unit)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Unknown MAL manga update error"))
                }
            }
            else -> Result.success(Unit)
        }
    }

    /**
     * Map LX MediaStatus to MAL string status.
     */
    private fun mapStatusToMal(status: MediaStatus, isAnime: Boolean): String {
        return when (status) {
            MediaStatus.WATCHING -> if (isAnime) "watching" else "reading"
            MediaStatus.COMPLETED -> "completed"
            MediaStatus.ON_HOLD -> "on_hold"
            MediaStatus.DROPPED -> "dropped"
            MediaStatus.PLAN_TO_WATCH -> if (isAnime) "plan_to_watch" else "plan_to_read"
        }
    }

    /**
     * Map MAL string status to LX MediaStatus.
     */
    private fun mapMalStatusToLx(statusStr: String?): MediaStatus {
        return when (statusStr?.trim()?.lowercase(Locale.US)) {
            "watching", "reading" -> MediaStatus.WATCHING
            "completed" -> MediaStatus.COMPLETED
            "on_hold" -> MediaStatus.ON_HOLD
            "dropped" -> MediaStatus.DROPPED
            "plan_to_watch", "plan_to_read" -> MediaStatus.PLAN_TO_WATCH
            else -> MediaStatus.WATCHING
        }
    }

    /**
     * Pulls the user's full anime & manga list from MyAnimeList and imports/merges it into local DB.
     */
    fun startFullPull(
        context: Context,
        userId: Long,
        repository: MediaRepository,
        strategy: ConflictStrategy = repository.getConflictStrategy(userId)
    ) {
        if (_syncState.value.isRunning) return

        pullJob?.cancel()
        pullJob = scope.launch {
            _syncState.value = MalSyncProgress(
                isRunning = true,
                statusMessage = "Connecting to MyAnimeList..."
            )

            try {
                val pulledEntries = mutableListOf<MediaEntry>()

                // 1. Fetch Anime list
                _syncState.update { it.copy(statusMessage = "Fetching anime list from MyAnimeList...") }
                var offset = 0
                val limit = 100
                var hasMore = true

                while (hasMore) {
                    val animeResponse = MalApiClient.getUserAnimeList(
                        context = context,
                        userId = userId,
                        offset = offset,
                        limit = limit
                    ).getOrThrow()

                    val items = animeResponse.data ?: emptyList()
                    for (item in items) {
                        val node = item.node
                        val listStatus = item.listStatus
                        val entry = MediaEntry(
                            userId = userId,
                            title = node.title,
                            alternateTitle = node.alternativeTitles?.en ?: node.alternativeTitles?.ja,
                            mediaType = MediaType.ANIME,
                            status = mapMalStatusToLx(listStatus?.status),
                            score = listStatus?.score?.takeIf { it > 0 },
                            externalScore = node.mean,
                            externalScoreSource = "MAL",
                            externalId = node.id.toString(),
                            externalSource = "MAL",
                            isExternallyTracked = true,
                            coverImageUri = node.mainPicture?.large ?: node.mainPicture?.medium,
                            currentEpisodes = listStatus?.numEpisodesWatched ?: 0,
                            totalEpisodes = node.numEpisodes,
                            isRewatching = listStatus?.isRewatching ?: false,
                            rewatchCount = listStatus?.numTimesRewatched ?: 0,
                            notes = node.synopsis,
                            tags = node.genres?.joinToString(", ") { it.name },
                            airedStartDate = parseMalDateToMillis(node.startDate),
                            airedEndDate = parseMalDateToMillis(node.endDate)
                        )
                        pulledEntries.add(entry)
                    }

                    if (items.size < limit || animeResponse.paging?.next == null) {
                        hasMore = false
                    } else {
                        offset += limit
                    }
                }

                // 2. Fetch Manga list
                _syncState.update { it.copy(statusMessage = "Fetching manga list from MyAnimeList...") }
                offset = 0
                hasMore = true

                while (hasMore) {
                    val mangaResponse = MalApiClient.getUserMangaList(
                        context = context,
                        userId = userId,
                        offset = offset,
                        limit = limit
                    ).getOrThrow()

                    val items = mangaResponse.data ?: emptyList()
                    for (item in items) {
                        val node = item.node
                        val listStatus = item.listStatus
                        val entry = MediaEntry(
                            userId = userId,
                            title = node.title,
                            alternateTitle = node.alternativeTitles?.en ?: node.alternativeTitles?.ja,
                            mediaType = MediaType.MANGA,
                            status = mapMalStatusToLx(listStatus?.status),
                            score = listStatus?.score?.takeIf { it > 0 },
                            externalScore = node.mean,
                            externalScoreSource = "MAL",
                            externalId = node.id.toString(),
                            externalSource = "MAL",
                            isExternallyTracked = true,
                            coverImageUri = node.mainPicture?.large ?: node.mainPicture?.medium,
                            currentVolumes = listStatus?.numVolumesRead ?: 0,
                            totalVolumes = node.numVolumes,
                            currentChapters = listStatus?.numChaptersRead ?: 0,
                            totalChapters = node.numChapters,
                            trackingUnit = TrackingUnit.CHAPTERS,
                            isRewatching = listStatus?.isRereading ?: false,
                            rewatchCount = listStatus?.numTimesReread ?: 0,
                            notes = node.synopsis,
                            tags = node.genres?.joinToString(", ") { it.name },
                            airedStartDate = parseMalDateToMillis(node.startDate),
                            airedEndDate = parseMalDateToMillis(node.endDate)
                        )
                        pulledEntries.add(entry)
                    }

                    if (items.size < limit || mangaResponse.paging?.next == null) {
                        hasMore = false
                    } else {
                        offset += limit
                    }
                }

                // 3. Import / Merge into local Room DB
                _syncState.update {
                    it.copy(
                        totalCount = pulledEntries.size,
                        statusMessage = "Importing ${pulledEntries.size} entries into your library..."
                    )
                }

                val (inserted, updated) = repository.importEntries(pulledEntries, strategy, targetUserId = userId)

                MalTokenStore(context).setLastSyncTimestamp(userId, System.currentTimeMillis())

                _syncState.value = MalSyncProgress(
                    isRunning = false,
                    isCompleted = true,
                    completedCount = pulledEntries.size,
                    totalCount = pulledEntries.size,
                    statusMessage = "Synced ${pulledEntries.size} titles ($inserted added, $updated updated)"
                )
            } catch (e: Exception) {
                Log.e(TAG, "MAL full sync failed", e)
                _syncState.value = MalSyncProgress(
                    isRunning = false,
                    errorMessage = e.message ?: "Failed to pull data from MyAnimeList"
                )
            }
        }
    }

    fun cancelSync() {
        pullJob?.cancel()
        pullJob = null
        _syncState.value = MalSyncProgress(isRunning = false, statusMessage = "Sync canceled")
    }

    private fun parseMalDateToMillis(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        val trimmed = dateStr.trim()
        val patterns = listOf("yyyy-MM-dd", "yyyy-MM", "yyyy")
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(trimmed)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return null
    }
}

