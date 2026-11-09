package com.lx.tracklist.data.repository

import android.content.Context
import com.lx.tracklist.data.local.CustomCategoryDao
import com.lx.tracklist.data.local.MediaEntryDao
import com.lx.tracklist.data.local.StatusCount
import com.lx.tracklist.data.local.SubCategoryDao
import com.lx.tracklist.data.local.UserProfileDao
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.SubCategory
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

enum class ConflictStrategy {
    SKIP,
    OVERWRITE,
    MERGE
}

class MediaRepository(
    private val dao: MediaEntryDao,
    private val userProfileDao: UserProfileDao? = null,
    private val customCategoryDao: CustomCategoryDao? = null,
    private val subCategoryDao: SubCategoryDao? = null,
    private val context: Context? = null
) {
    private val repoScope = CoroutineScope(Dispatchers.IO)
    private val prefs = context?.getSharedPreferences("lx_user_prefs", Context.MODE_PRIVATE)

    private val defaultUser = UserProfile(id = 1L, name = "Main Profile", avatarColorHex = "#A7EF60", isDefault = true)
    private val savedInitialUserId = prefs?.getLong("active_user_id", 1L) ?: 1L
    private val _activeUser = MutableStateFlow(
        if (savedInitialUserId != 1L) UserProfile(id = savedInitialUserId, name = "Profile $savedInitialUserId", avatarColorHex = "#A7EF60") else defaultUser
    )
    val activeUser: StateFlow<UserProfile> = _activeUser.asStateFlow()

    private val _isUserInitialized = MutableStateFlow(false)
    val isUserInitialized: StateFlow<Boolean> = _isUserInitialized.asStateFlow()

    private val _isSwipeToSwitchTabsEnabled = MutableStateFlow(
        prefs?.getBoolean("swipe_to_switch_tabs_$savedInitialUserId", prefs?.getBoolean("swipe_to_switch_tabs", true) ?: true) ?: true
    )
    val isSwipeToSwitchTabsEnabled: StateFlow<Boolean> = _isSwipeToSwitchTabsEnabled.asStateFlow()

    fun setSwipeToSwitchTabsEnabled(enabled: Boolean) {
        _isSwipeToSwitchTabsEnabled.value = enabled
        val userId = _activeUser.value.id
        prefs?.edit()?.putBoolean("swipe_to_switch_tabs_$userId", enabled)?.apply()
    }

    private val _isTachiyomiImportAsFavorites = MutableStateFlow(
        prefs?.getBoolean("tachiyomi_import_as_favorites_$savedInitialUserId", prefs?.getBoolean("tachiyomi_import_as_favorites", false) ?: false) ?: false
    )
    val isTachiyomiImportAsFavorites: StateFlow<Boolean> = _isTachiyomiImportAsFavorites.asStateFlow()

    fun setTachiyomiImportAsFavorites(enabled: Boolean) {
        _isTachiyomiImportAsFavorites.value = enabled
        val userId = _activeUser.value.id
        prefs?.edit()?.putBoolean("tachiyomi_import_as_favorites_$userId", enabled)?.apply()
    }

    fun getConflictStrategy(userId: Long? = null): ConflictStrategy {
        val targetUserId = userId ?: _activeUser.value.id
        val savedName = prefs?.getString("conflict_strategy_$targetUserId", null)
            ?: prefs?.getString("conflict_strategy", ConflictStrategy.MERGE.name)
            ?: ConflictStrategy.MERGE.name
        return try {
            ConflictStrategy.valueOf(savedName)
        } catch (_: Exception) {
            ConflictStrategy.MERGE
        }
    }

    fun setConflictStrategy(strategy: ConflictStrategy, userId: Long? = null) {
        val targetUserId = userId ?: _activeUser.value.id
        prefs?.edit()?.putString("conflict_strategy_$targetUserId", strategy.name)?.apply()
    }

    init {
        initUserProfile()
    }

    private fun initUserProfile() {
        if (userProfileDao != null) {
            repoScope.launch {
                try {
                    val count = userProfileDao.getCount()
                    if (count == 0) {
                        userProfileDao.insert(defaultUser)
                    }
                    val savedUserId = prefs?.getLong("active_user_id", 1L) ?: 1L
                    val profile = userProfileDao.getProfileById(savedUserId)
                        ?: userProfileDao.getAllProfilesSync().firstOrNull()
                        ?: defaultUser
                    _activeUser.value = profile
                    _isSwipeToSwitchTabsEnabled.value = prefs?.getBoolean("swipe_to_switch_tabs_${profile.id}", true) ?: true
                    _isTachiyomiImportAsFavorites.value = prefs?.getBoolean("tachiyomi_import_as_favorites_${profile.id}", false) ?: false
                    dao.sanitizeZeroUserIds(defaultUser.id)
                } catch (_: Exception) {
                } finally {
                    _isUserInitialized.value = true
                }
            }
        } else {
            _isUserInitialized.value = true
        }
    }

    val allUsers: Flow<List<UserProfile>>
        get() = userProfileDao?.getAllProfiles() ?: emptyFlow()

    suspend fun getAllUsersSync(): List<UserProfile> = userProfileDao?.getAllProfilesSync() ?: emptyList()

    suspend fun switchUser(profile: UserProfile) {
        // Cancel any running background sync for the previous profile so jobs don't cross over
        try {
            com.lx.tracklist.data.remote.JikanSyncManager.cancelSync(context)
            com.lx.tracklist.data.remote.TmdbSyncManager.cancelSync(context)
            com.lx.tracklist.data.remote.mal.MalSyncManager.cancelSync()
        } catch (_: Exception) {}

        _activeUser.value = profile
        prefs?.edit()?.putLong("active_user_id", profile.id)?.apply()
        _isSwipeToSwitchTabsEnabled.value = prefs?.getBoolean("swipe_to_switch_tabs_${profile.id}", true) ?: true
        _isTachiyomiImportAsFavorites.value = prefs?.getBoolean("tachiyomi_import_as_favorites_${profile.id}", false) ?: false
    }

    suspend fun createUser(
        name: String,
        avatarColorHex: String = "#A7EF60",
        avatarImageUri: String? = null,
        passKey: String? = null
    ): UserProfile {
        val newProfile = UserProfile(
            name = name.trim().ifBlank { "User" },
            avatarColorHex = avatarColorHex,
            avatarImageUri = avatarImageUri,
            isDefault = false,
            passKey = passKey?.trim()?.takeIf { it.length == 4 && it.all { ch -> ch.isDigit() } }
        )
        val id = userProfileDao?.insert(newProfile) ?: 1L
        val saved = newProfile.copy(id = id)
        switchUser(saved)
        return saved
    }

    suspend fun updatePassKey(profileId: Long, newPassKey: String?) {
        val profile = userProfileDao?.getProfileById(profileId) ?: return
        val sanitized = newPassKey?.trim()?.takeIf { it.length == 4 && it.all { ch -> ch.isDigit() } }
        val updated = profile.copy(passKey = sanitized)
        userProfileDao?.update(updated)
        if (_activeUser.value.id == profileId) {
            _activeUser.value = updated
        }
    }

    suspend fun deleteUser(profile: UserProfile) {
        if (userProfileDao == null) return
        val all = userProfileDao.getAllProfilesSync()
        if (all.size <= 1) return // Do not delete only user

        // Delete user's media entries and external tracking credentials
        dao.deleteAllByUser(profile.id)
        prefs?.edit()?.remove("conflict_strategy_${profile.id}")?.apply()
        try {
            context?.let { ctx ->
                com.lx.tracklist.data.remote.mal.MalTokenStore(ctx).clear(profile.id)
            }
        } catch (_: Exception) {}
        userProfileDao.delete(profile)

        val remaining = userProfileDao.getAllProfilesSync()
        if (_activeUser.value.id == profile.id && remaining.isNotEmpty()) {
            switchUser(remaining.first())
        }
    }

    suspend fun updateUser(profile: UserProfile) {
        userProfileDao?.update(profile)
        if (_activeUser.value.id == profile.id) {
            _activeUser.value = profile
        }
    }

    suspend fun updateProfileStartupDefaults(
        profileId: Long,
        defaultMediaType: MediaType?,
        defaultCustomCategory: String?,
        defaultSubCategory: String?,
        defaultStatus: MediaStatus?
    ) {
        val profile = userProfileDao?.getProfileById(profileId) ?: return
        val updated = profile.copy(
            defaultMediaType = defaultMediaType,
            defaultCustomCategory = defaultCustomCategory,
            defaultSubCategory = defaultSubCategory,
            defaultStatus = defaultStatus
        )
        updateUser(updated)
    }

    // Custom Categories
    val customCategories: Flow<List<CustomCategory>>
        get() = customCategoryDao?.getAllCategories() ?: emptyFlow()

    suspend fun addCustomCategory(
        name: String,
        mediaType: MediaType? = null,
        isWatchType: Boolean = false,
        trackingUnit: TrackingUnit = TrackingUnit.CHAPTERS,
        colorHex: String = "#A7EF60"
    ): Long {
        val cat = CustomCategory(
            name = name.trim(),
            mediaType = mediaType,
            isWatchType = isWatchType,
            trackingUnit = trackingUnit,
            colorHex = colorHex
        )
        return customCategoryDao?.insert(cat) ?: 0L
    }

    suspend fun updateCustomCategory(category: CustomCategory) {
        customCategoryDao?.update(category)
    }

    suspend fun deleteCustomCategory(category: CustomCategory) {
        customCategoryDao?.delete(category)
    }

    // Sub Categories (specific to a category)
    fun getSubCategories(categoryKey: String): Flow<List<SubCategory>> {
        return subCategoryDao?.getSubCategoriesByCategory(categoryKey) ?: emptyFlow()
    }

    val allSubCategories: Flow<List<SubCategory>>
        get() = subCategoryDao?.getAllSubCategories() ?: emptyFlow()

    suspend fun addSubCategory(categoryKey: String, name: String, colorHex: String = "#A7EF60") {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        subCategoryDao?.insert(
            SubCategory(
                categoryKey = categoryKey,
                name = trimmed,
                colorHex = colorHex
            )
        )
    }

    suspend fun deleteSubCategory(subCategory: SubCategory) {
        subCategoryDao?.delete(subCategory)
    }

    suspend fun deleteSubCategoryByName(categoryKey: String, name: String) {
        subCategoryDao?.deleteByName(categoryKey, name)
    }

    // Media Entries Queries - Reactive to activeUser
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllEntries(): Flow<List<MediaEntry>> {
        return if (userProfileDao != null) {
            _activeUser.flatMapLatest { user -> dao.getAllEntriesByUser(user.id) }
        } else {
            dao.getAllEntries()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getEntriesByCategory(mediaType: MediaType): Flow<List<MediaEntry>> {
        return if (userProfileDao != null) {
            _activeUser.flatMapLatest { user -> dao.getEntriesByUserAndCategory(user.id, mediaType) }
        } else {
            dao.getEntriesByCategory(mediaType)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getEntriesByCategoryAndStatus(mediaType: MediaType, status: MediaStatus): Flow<List<MediaEntry>> {
        return if (userProfileDao != null) {
            _activeUser.flatMapLatest { user -> dao.getEntriesByUserAndCategoryAndStatus(user.id, mediaType, status) }
        } else {
            dao.getEntriesByCategoryAndStatus(mediaType, status)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getFavorites(): Flow<List<MediaEntry>> {
        return if (userProfileDao != null) {
            _activeUser.flatMapLatest { user -> dao.getFavoritesByUser(user.id) }
        } else {
            dao.getFavorites()
        }
    }

    fun getEntryById(id: Long): Flow<MediaEntry?> = dao.getEntryById(id)

    suspend fun getEntryByIdSync(id: Long): MediaEntry? = dao.getEntryByIdSync(id)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun searchEntries(query: String): Flow<List<MediaEntry>> {
        return if (userProfileDao != null) {
            _activeUser.flatMapLatest { user -> dao.searchEntriesByUser(user.id, query) }
        } else {
            dao.searchEntries(query)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getStatusCountsByCategory(mediaType: MediaType): Flow<List<StatusCount>> {
        return if (userProfileDao != null) {
            _activeUser.flatMapLatest { user -> dao.getStatusCountsByUserAndCategory(user.id, mediaType) }
        } else {
            dao.getStatusCountsByCategory(mediaType)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTotalCountByCategory(mediaType: MediaType): Flow<Int> {
        return if (userProfileDao != null) {
            _activeUser.flatMapLatest { user -> dao.getTotalCountByUserAndCategory(user.id, mediaType) }
        } else {
            dao.getTotalCountByCategory(mediaType)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTotalCount(): Flow<Int> {
        return if (userProfileDao != null) {
            _activeUser.flatMapLatest { user -> dao.getTotalCountByUser(user.id) }
        } else {
            dao.getTotalCount()
        }
    }

    suspend fun insert(entry: MediaEntry): Long {
        val entryWithUser = if (entry.userId <= 0L) {
            entry.copy(userId = _activeUser.value.id)
        } else {
            entry
        }
        val id = dao.insert(entryWithUser)
        context?.let { ctx ->
            com.lx.tracklist.data.remote.mal.MalSyncManager.pushUpdateInBackground(ctx, entryWithUser.copy(id = id))
        }
        return id
    }

    suspend fun update(entry: MediaEntry) {
        val effectiveEntry = if (entry.userId <= 0L) {
            val existing = dao.getEntryByIdSync(entry.id)
            entry.copy(userId = existing?.userId ?: _activeUser.value.id)
        } else {
            entry
        }
        dao.update(effectiveEntry)
        context?.let { ctx ->
            com.lx.tracklist.data.remote.mal.MalSyncManager.pushUpdateInBackground(ctx, effectiveEntry)
        }
    }

    suspend fun delete(entry: MediaEntry) {
        val userId = if (entry.userId > 0L) entry.userId else _activeUser.value.id
        dao.deleteByIdAndUser(entry.id, userId)
    }

    suspend fun deleteById(id: Long) = dao.deleteByIdAndUser(id, _activeUser.value.id)

    suspend fun deleteEntries(ids: List<Long>) {
        if (ids.isEmpty()) return
        dao.deleteEntriesByIdsAndUser(_activeUser.value.id, ids)
    }

    suspend fun batchUpdateCategory(ids: List<Long>, mediaType: MediaType, customCategory: String?) {
        if (ids.isEmpty()) return
        dao.updateCategoryForIdsAndUser(_activeUser.value.id, ids, mediaType, customCategory, System.currentTimeMillis())
    }

    suspend fun batchUpdateSubCategory(ids: List<Long>, subCategory: String?) {
        if (ids.isEmpty()) return
        dao.updateSubCategoryForIdsAndUser(_activeUser.value.id, ids, subCategory, System.currentTimeMillis())
    }

    suspend fun batchUpdateStatus(ids: List<Long>, status: MediaStatus) {
        if (ids.isEmpty()) return
        dao.updateStatusForIdsAndUser(_activeUser.value.id, ids, status, System.currentTimeMillis())
    }

    suspend fun batchUpdateFavorite(ids: List<Long>, isFavorite: Boolean) {
        if (ids.isEmpty()) return
        dao.updateFavoriteForIdsAndUser(_activeUser.value.id, ids, isFavorite, System.currentTimeMillis())
    }

    suspend fun toggleFavorite(entry: MediaEntry) {
        dao.update(entry.copy(isFavorite = !entry.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    suspend fun batchUpdateExternalTracking(ids: List<Long>, isExternallyTracked: Boolean) {
        if (ids.isEmpty()) return
        val userId = _activeUser.value.id
        dao.updateExternalTrackingForIdsAndUser(userId, ids, isExternallyTracked, System.currentTimeMillis())
        if (isExternallyTracked) {
            context?.let { ctx ->
                ids.forEach { id ->
                    val entry = dao.getEntryByIdSync(id)
                    if (entry != null && entry.isExternallyTracked) {
                        com.lx.tracklist.data.remote.mal.MalSyncManager.pushUpdateInBackground(ctx, entry)
                    }
                }
            }
        }
    }

    suspend fun toggleExternalTracking(entry: MediaEntry): MediaEntry {
        val effectiveEntry = if (entry.userId <= 0L) {
            val existing = dao.getEntryByIdSync(entry.id)
            entry.copy(userId = existing?.userId ?: _activeUser.value.id)
        } else {
            entry
        }
        val newTracked = !effectiveEntry.isExternallyTracked
        val updated = effectiveEntry.copy(isExternallyTracked = newTracked, updatedAt = System.currentTimeMillis())
        dao.update(updated)
        if (newTracked) {
            context?.let { ctx ->
                com.lx.tracklist.data.remote.mal.MalSyncManager.pushUpdateInBackground(ctx, updated)
            }
        }
        return updated
    }

    suspend fun setExternalTracking(entry: MediaEntry, isExternallyTracked: Boolean): MediaEntry {
        val effectiveEntry = if (entry.userId <= 0L) {
            val existing = dao.getEntryByIdSync(entry.id)
            entry.copy(userId = existing?.userId ?: _activeUser.value.id)
        } else {
            entry
        }
        val updated = effectiveEntry.copy(isExternallyTracked = isExternallyTracked, updatedAt = System.currentTimeMillis())
        dao.update(updated)
        if (isExternallyTracked) {
            context?.let { ctx ->
                com.lx.tracklist.data.remote.mal.MalSyncManager.pushUpdateInBackground(ctx, updated)
            }
        }
        return updated
    }

    suspend fun incrementProgress(entry: MediaEntry): MediaEntry {
        val effectiveEntry = if (entry.userId <= 0L) {
            val existing = dao.getEntryByIdSync(entry.id)
            entry.copy(userId = existing?.userId ?: _activeUser.value.id)
        } else {
            entry
        }
        val updated = effectiveEntry.incrementProgress()
        dao.update(updated)
        context?.let { ctx ->
            com.lx.tracklist.data.remote.mal.MalSyncManager.pushUpdateInBackground(ctx, updated)
        }
        return updated
    }

    suspend fun importEntries(
        entries: List<MediaEntry>,
        strategy: ConflictStrategy,
        targetUserId: Long = _activeUser.value.id
    ): Pair<Int, Int> { // Returns Pair(insertedCount, updatedCount)
        var inserted = 0
        var updated = 0

        // Auto-create any missing custom categories
        if (customCategoryDao != null) {
            val uniqueCustomCats = entries.mapNotNull { it.customCategory?.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            for (catName in uniqueCustomCats) {
                val sample = entries.firstOrNull { it.customCategory?.trim().equals(catName, ignoreCase = true) }
                val targetType = sample?.mediaType ?: MediaType.MANGA
                val existing = customCategoryDao.getCategoryByName(catName)
                if (existing == null) {
                    val isWatch = sample?.let {
                        it.mediaType == MediaType.ANIME || it.mediaType == MediaType.SHOW || it.mediaType == MediaType.MOVIE
                    } ?: false
                    val unit = sample?.trackingUnit ?: TrackingUnit.CHAPTERS
                    customCategoryDao.insert(
                        CustomCategory(
                            name = catName,
                            mediaType = targetType,
                            isWatchType = isWatch,
                            trackingUnit = unit
                        )
                    )
                } else if (existing.mediaType == null) {
                    customCategoryDao.update(existing.copy(mediaType = targetType))
                }
            }
        }

        // Auto-create any missing sub categories
        if (subCategoryDao != null) {
            for (rawEntry in entries) {
                val subCat = rawEntry.subCategory?.trim()
                if (!subCat.isNullOrBlank()) {
                    val catKey = if (!rawEntry.customCategory.isNullOrBlank()) {
                        rawEntry.customCategory.trim()
                    } else {
                        rawEntry.mediaType.name
                    }
                    val existing = subCategoryDao.getSubCategory(catKey, subCat)
                    if (existing == null) {
                        subCategoryDao.insert(
                            SubCategory(
                                categoryKey = catKey,
                                name = subCat
                            )
                        )
                    }
                }
            }
        }

        for (rawEntry in entries) {
            val entry = rawEntry.copy(userId = targetUserId)
            val existing = if (entry.externalId != null && entry.externalSource != null) {
                dao.findByExternalId(targetUserId, entry.externalId, entry.externalSource)
            } else if (entry.externalId != null) {
                dao.findByAnyExternalId(targetUserId, entry.externalId)
            } else {
                dao.findByTitleAndType(targetUserId, entry.title, entry.mediaType)
            }

            if (existing == null) {
                dao.insert(entry.copy(id = 0L, userId = targetUserId))
                inserted++
            } else {
                when (strategy) {
                    ConflictStrategy.SKIP -> {
                        // Do nothing
                    }
                    ConflictStrategy.OVERWRITE -> {
                        dao.update(entry.copy(id = existing.id, userId = targetUserId, updatedAt = System.currentTimeMillis()))
                        updated++
                    }
                    ConflictStrategy.MERGE -> {
                        val merged = mergeEntries(existing, entry).copy(id = existing.id, userId = targetUserId)
                        dao.update(merged)
                        updated++
                    }
                }
            }
        }

        return Pair(inserted, updated)
    }

    suspend fun getAllEntriesSync(): List<MediaEntry> {
        return if (userProfileDao != null) {
            dao.getAllEntriesByUserSync(_activeUser.value.id)
        } else {
            dao.getAllEntriesSync()
        }
    }

    suspend fun getAllEntriesForUserSync(userId: Long): List<MediaEntry> {
        return if (userProfileDao != null) {
            dao.getAllEntriesByUserSync(userId)
        } else {
            dao.getAllEntriesSync()
        }
    }

    suspend fun getAllEntriesAllUsersSync(): List<MediaEntry> {
        return dao.getAllEntriesSync()
    }

    private fun mergeEntries(existing: MediaEntry, imported: MediaEntry): MediaEntry {
        val maxEpisodes = max(existing.currentEpisodes, imported.currentEpisodes)
        val maxTotalEp = if (existing.totalEpisodes > 0) existing.totalEpisodes else imported.totalEpisodes
        val maxVol = max(existing.currentVolumes, imported.currentVolumes)
        val maxTotalVol = if (existing.totalVolumes > 0) existing.totalVolumes else imported.totalVolumes
        val maxCh = max(existing.currentChapters, imported.currentChapters)
        val maxTotalCh = if (existing.totalChapters > 0) existing.totalChapters else imported.totalChapters
        val maxPages = max(existing.currentPages, imported.currentPages)
        val maxTotalPages = if (existing.totalPages > 0) existing.totalPages else imported.totalPages

        // Merge tags
        val existingTags = existing.tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val importedTags = imported.tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val mergedTags = (existingTags + importedTags).distinct().joinToString(", ")

        return existing.copy(
            score = imported.score ?: existing.score,
            externalScore = imported.externalScore ?: existing.externalScore,
            externalScoreSource = imported.externalScoreSource ?: existing.externalScoreSource,
            status = if (imported.status == MediaStatus.COMPLETED) MediaStatus.COMPLETED else existing.status,
            startDate = imported.startDate ?: existing.startDate,
            finishDate = imported.finishDate ?: existing.finishDate,
            airedStartDate = imported.airedStartDate ?: existing.airedStartDate,
            airedEndDate = imported.airedEndDate ?: existing.airedEndDate,
            externalId = imported.externalId ?: existing.externalId,
            externalSource = imported.externalSource ?: existing.externalSource,
            rewatchCount = if (imported.rewatchCount > 0) imported.rewatchCount else existing.rewatchCount,
            isRewatching = imported.isRewatching || existing.isRewatching,
            episodeLengthMinutes = imported.episodeLengthMinutes ?: existing.episodeLengthMinutes,
            durationMinutes = imported.durationMinutes ?: existing.durationMinutes,
            currentEpisodes = maxEpisodes,
            totalEpisodes = maxTotalEp,
            currentVolumes = maxVol,
            totalVolumes = maxTotalVol,
            currentChapters = maxCh,
            totalChapters = maxTotalCh,
            currentPages = maxPages,
            totalPages = maxTotalPages,
            tags = mergedTags.ifBlank { null },
            notes = if (!imported.notes.isNullOrBlank()) imported.notes else existing.notes,
            coverImageUri = imported.coverImageUri ?: existing.coverImageUri,
            customCategory = imported.customCategory ?: existing.customCategory,
            subCategory = imported.subCategory ?: existing.subCategory,
            isExternallyTracked = existing.isExternallyTracked || imported.isExternallyTracked,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun addRelatedItem(
        item: com.lx.tracklist.data.model.RelatedMediaItem,
        status: MediaStatus,
        context: Context? = null,
        targetUserId: Long? = null
    ): Long {
        val effectiveUserId = targetUserId ?: activeUser.value.id
        return withContext(Dispatchers.IO) {
            val entry = if (item.externalSource.equals("TMDB", ignoreCase = true)) {
                val tmdbId = item.externalId.toLongOrNull() ?: 0L
                if (item.mediaType == MediaType.MOVIE) {
                    val fullDetails = com.lx.tracklist.data.remote.TmdbApiClient.fetchMovieDetails(tmdbId, context).getOrNull()
                    MediaEntry(
                        userId = effectiveUserId,
                        title = fullDetails?.title ?: item.title,
                        alternateTitle = fullDetails?.alternateTitle,
                        mediaType = MediaType.MOVIE,
                        status = status,
                        externalId = item.externalId,
                        externalSource = "TMDB",
                        externalScore = fullDetails?.externalScore ?: item.score,
                        externalScoreSource = "TMDB",
                        coverImageUri = fullDetails?.posterUrl ?: item.posterUrl,
                        durationMinutes = fullDetails?.durationMinutes,
                        tags = fullDetails?.tags,
                        notes = fullDetails?.synopsis,
                        airedStartDate = fullDetails?.airedStartDate
                    )
                } else {
                    val fullDetails = com.lx.tracklist.data.remote.TmdbApiClient.fetchTvDetails(tmdbId, context).getOrNull()
                    MediaEntry(
                        userId = effectiveUserId,
                        title = fullDetails?.title ?: item.title,
                        alternateTitle = fullDetails?.alternateTitle,
                        mediaType = MediaType.SHOW,
                        status = status,
                        externalId = item.externalId,
                        externalSource = "TMDB",
                        externalScore = fullDetails?.externalScore ?: item.score,
                        externalScoreSource = "TMDB",
                        coverImageUri = fullDetails?.posterUrl ?: item.posterUrl,
                        totalEpisodes = fullDetails?.totalEpisodes ?: 0,
                        totalSeasons = fullDetails?.totalSeasons ?: 1,
                        episodeLengthMinutes = fullDetails?.durationMinutes,
                        tags = fullDetails?.tags,
                        notes = fullDetails?.synopsis,
                        airedStartDate = fullDetails?.airedStartDate,
                        isAiring = fullDetails?.isAiring ?: false
                    )
                }
            } else {
                val malId = item.externalId.toLongOrNull() ?: 0L
                val isManga = item.mediaType == MediaType.MANGA || item.mediaType == MediaType.MANHWA || item.mediaType == MediaType.BOOK
                val details = com.lx.tracklist.data.remote.JikanApiClient.fetchMediaDetails(malId, isManga).getOrNull()
                val isVideo = item.mediaType == MediaType.ANIME
                MediaEntry(
                    userId = effectiveUserId,
                    title = details?.title ?: item.title,
                    alternateTitle = details?.alternateTitle,
                    mediaType = item.mediaType,
                    status = status,
                    externalId = item.externalId,
                    externalSource = "MAL",
                    externalScore = details?.externalScore ?: item.score,
                    externalScoreSource = "MAL",
                    coverImageUri = details?.posterUrl ?: item.posterUrl,
                    totalEpisodes = if (isVideo) (details?.totalEpisodes ?: 0) else 0,
                    totalChapters = if (!isVideo) (details?.totalChapters ?: 0) else 0,
                    totalVolumes = if (!isVideo) (details?.totalVolumes ?: 0) else 0,
                    episodeLengthMinutes = details?.durationMinutes,
                    durationMinutes = if (item.mediaType == MediaType.MOVIE) details?.durationMinutes else null,
                    tags = details?.tags,
                    notes = details?.synopsis,
                    airedStartDate = details?.airedStartDate,
                    airedEndDate = details?.airedEndDate,
                    isAiring = details?.isAiring ?: false
                )
            }
            insert(entry)
        }
    }
}
