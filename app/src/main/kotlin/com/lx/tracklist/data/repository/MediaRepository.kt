package com.lx.tracklist.data.repository

import android.content.Context
import com.lx.tracklist.data.local.CustomCategoryDao
import com.lx.tracklist.data.local.MediaEntryDao
import com.lx.tracklist.data.local.StatusCount
import com.lx.tracklist.data.local.UserProfileDao
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
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
    private val context: Context? = null
) {
    private val repoScope = CoroutineScope(Dispatchers.IO)
    private val prefs = context?.getSharedPreferences("lx_user_prefs", Context.MODE_PRIVATE)

    private val defaultUser = UserProfile(id = 1L, name = "Main Profile", avatarColorHex = "#A7EF60", isDefault = true)
    private val _activeUser = MutableStateFlow(defaultUser)
    val activeUser: StateFlow<UserProfile> = _activeUser.asStateFlow()

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
                } catch (_: Exception) {}
            }
        }
    }

    val allUsers: Flow<List<UserProfile>>
        get() = userProfileDao?.getAllProfiles() ?: emptyFlow()

    suspend fun switchUser(profile: UserProfile) {
        _activeUser.value = profile
        prefs?.edit()?.putLong("active_user_id", profile.id)?.apply()
    }

    suspend fun createUser(
        name: String,
        avatarColorHex: String = "#A7EF60",
        avatarImageUri: String? = null
    ): UserProfile {
        val newProfile = UserProfile(
            name = name.trim().ifBlank { "User" },
            avatarColorHex = avatarColorHex,
            avatarImageUri = avatarImageUri,
            isDefault = false
        )
        val id = userProfileDao?.insert(newProfile) ?: 1L
        val saved = newProfile.copy(id = id)
        switchUser(saved)
        return saved
    }

    suspend fun deleteUser(profile: UserProfile) {
        if (userProfileDao == null) return
        val all = userProfileDao.getAllProfilesSync()
        if (all.size <= 1) return // Do not delete only user

        // Delete user's media entries
        dao.deleteAllByUser(profile.id)
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

    // Custom Categories
    val customCategories: Flow<List<CustomCategory>>
        get() = customCategoryDao?.getAllCategories() ?: emptyFlow()

    suspend fun addCustomCategory(
        name: String,
        isWatchType: Boolean = false,
        trackingUnit: TrackingUnit = TrackingUnit.CHAPTERS,
        colorHex: String = "#A7EF60"
    ): Long {
        val cat = CustomCategory(
            name = name.trim(),
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
        val entryWithUser = if (entry.userId <= 0L || entry.userId == 1L) {
            entry.copy(userId = _activeUser.value.id)
        } else {
            entry
        }
        return dao.insert(entryWithUser)
    }

    suspend fun update(entry: MediaEntry) = dao.update(entry)

    suspend fun delete(entry: MediaEntry) = dao.delete(entry)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteEntries(ids: List<Long>) = dao.deleteEntriesByIds(ids)

    suspend fun toggleFavorite(entry: MediaEntry) {
        dao.update(entry.copy(isFavorite = !entry.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    suspend fun incrementProgress(entry: MediaEntry): MediaEntry {
        val updated = entry.incrementProgress()
        dao.update(updated)
        return updated
    }

    suspend fun importEntries(
        entries: List<MediaEntry>,
        strategy: ConflictStrategy
    ): Pair<Int, Int> { // Returns Pair(insertedCount, updatedCount)
        var inserted = 0
        var updated = 0
        val targetUserId = _activeUser.value.id

        // Auto-create any missing custom categories
        if (customCategoryDao != null) {
            val uniqueCustomCats = entries.mapNotNull { it.customCategory?.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            for (catName in uniqueCustomCats) {
                val existing = customCategoryDao.getCategoryByName(catName)
                if (existing == null) {
                    val sample = entries.firstOrNull { it.customCategory?.trim().equals(catName, ignoreCase = true) }
                    val isWatch = sample?.let {
                        it.mediaType == MediaType.ANIME || it.mediaType == MediaType.SHOW || it.mediaType == MediaType.MOVIE
                    } ?: false
                    val unit = sample?.trackingUnit ?: TrackingUnit.CHAPTERS
                    customCategoryDao.insert(
                        CustomCategory(
                            name = catName,
                            isWatchType = isWatch,
                            trackingUnit = unit
                        )
                    )
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
                null
            }

            if (existing == null) {
                dao.insert(entry)
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
                        val merged = mergeEntries(existing, entry).copy(userId = targetUserId)
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
            status = if (imported.status == MediaStatus.COMPLETED) MediaStatus.COMPLETED else existing.status,
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
            updatedAt = System.currentTimeMillis()
        )
    }
}
