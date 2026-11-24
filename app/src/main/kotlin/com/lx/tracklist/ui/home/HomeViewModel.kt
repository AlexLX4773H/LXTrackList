package com.lx.tracklist.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.SubCategory
import com.lx.tracklist.data.model.UserProfile
import com.lx.tracklist.data.remote.BookSyncManager
import com.lx.tracklist.data.remote.GameSyncManager
import com.lx.tracklist.data.remote.JikanSyncManager
import com.lx.tracklist.data.remote.SyncForegroundService
import com.lx.tracklist.data.remote.TachiyomiResolverManager
import com.lx.tracklist.data.remote.TmdbSyncManager
import com.lx.tracklist.data.repository.MediaRepository
import com.lx.tracklist.ui.components.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomeRefreshScope {
    CURRENT_VIEW,
    AIRING_ONLY,
    CURRENT_CATEGORY,
    ALL_CATEGORIES
}

data class HomeUiState(
    val selectedCategory: MediaType = MediaType.ANIME,
    val selectedCustomCategory: String? = null,
    val selectedSubCategory: String? = null,
    val selectedStatus: MediaStatus? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedSort: SortOption = SortOption.RECENTLY_UPDATED,
    val isCategorySheetOpen: Boolean = false,
    val isUserSwitcherOpen: Boolean = false,
    val isAddSubCategoryDialogOpen: Boolean = false,
    val isRefreshDialogOpen: Boolean = false,
    val isSyncRunning: Boolean = false,
    val syncStatusMessage: String? = null,
    val syncCompletedCount: Int = 0,
    val syncTotalCount: Int = 0,
    val entries: List<MediaEntry> = emptyList(),
    val allUserEntries: List<MediaEntry> = emptyList(),
    val categoryEntries: List<MediaEntry> = emptyList(),
    val statusCounts: Map<MediaStatus, Int> = emptyMap(),
    val totalCount: Int = 0,
    val categoryCounts: Map<MediaType, Int> = emptyMap(),
    val customCategories: List<CustomCategory> = emptyList(),
    val customCategoryCounts: Map<String, Int> = emptyMap(),
    val subCategories: List<SubCategory> = emptyList(),
    val subCategoryCounts: Map<String, Int> = emptyMap(),
    val activeUser: UserProfile = UserProfile(id = 1L, name = "Main Profile", isDefault = true),
    val allUsers: List<UserProfile> = emptyList(),
    val isSwipeToSwitchTabsEnabled: Boolean = true,
    val isUserInitialized: Boolean = false
)

private data class FilterState(
    val category: MediaType = MediaType.ANIME,
    val customCategory: String? = null,
    val subCategory: String? = null,
    val status: MediaStatus? = null,
    val query: String = "",
    val isSearching: Boolean = false,
    val sort: SortOption = SortOption.RECENTLY_UPDATED,
    val isSheetOpen: Boolean = false,
    val isUserSwitcherOpen: Boolean = false,
    val isAddSubCategoryDialogOpen: Boolean = false,
    val isRefreshDialogOpen: Boolean = false
)

private data class SyncProgressInfo(
    val isRunning: Boolean = false,
    val statusMessage: String? = null,
    val completedCount: Int = 0,
    val totalCount: Int = 0
)

private data class UserAndSettingsContext(
    val activeUser: UserProfile,
    val allUsers: List<UserProfile>,
    val isSwipeEnabled: Boolean,
    val isUserInitialized: Boolean,
    val syncInfo: SyncProgressInfo
)

class HomeViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())
    private var lastAppliedProfile: UserProfile? = null

    init {
        viewModelScope.launch {
            repository.activeUser.collect { user ->
                val isFirst = (lastAppliedProfile == null)
                val isUserSwitch = (lastAppliedProfile != null && lastAppliedProfile?.id != user.id)
                val isDefaultsUpdatedForActive = (lastAppliedProfile != null &&
                        (lastAppliedProfile?.defaultMediaType != user.defaultMediaType ||
                                lastAppliedProfile?.defaultCustomCategory != user.defaultCustomCategory ||
                                lastAppliedProfile?.defaultSubCategory != user.defaultSubCategory ||
                                lastAppliedProfile?.defaultStatus != user.defaultStatus))

                if (isFirst || isUserSwitch || isDefaultsUpdatedForActive) {
                    lastAppliedProfile = user
                    applyProfileDefaults(user)
                }
            }
        }
    }

    private fun applyProfileDefaults(profile: UserProfile) {
        val targetCustomCat = profile.defaultCustomCategory?.trim()?.ifBlank { null }
        val targetMediaType = if (!targetCustomCat.isNullOrBlank()) {
            profile.defaultMediaType ?: MediaType.MANGA
        } else {
            profile.defaultMediaType ?: MediaType.ANIME
        }
        val targetSubCat = profile.defaultSubCategory?.trim()?.ifBlank { null }
        val targetStatus = if (targetSubCat != null) null else profile.defaultStatus

        _filterState.update { current ->
            current.copy(
                category = targetMediaType,
                customCategory = targetCustomCat,
                subCategory = targetSubCat,
                status = targetStatus
            )
        }
    }

    private val syncProgressFlow = combine(
        JikanSyncManager.syncState,
        TmdbSyncManager.syncState,
        BookSyncManager.syncState,
        GameSyncManager.syncState,
        TachiyomiResolverManager.resolveState
    ) { j, t, b, g, r ->
        val running = j.isRunning || t.isRunning || b.isRunning || g.isRunning || r.isRunning
        val status = when {
            r.isRunning -> r.statusMessage ?: "Resolving Tachiyomi entries..."
            j.isRunning -> j.statusMessage ?: "Syncing Anime / Manga..."
            t.isRunning -> t.statusMessage ?: "Syncing TV Shows / Movies..."
            b.isRunning -> b.statusMessage ?: "Syncing Books..."
            g.isRunning -> g.statusMessage ?: "Syncing Games..."
            else -> null
        }
        val completed = (if (j.isRunning) j.completedCount else 0) +
                (if (t.isRunning) t.completedCount else 0) +
                (if (b.isRunning) b.completedCount else 0) +
                (if (g.isRunning) g.completedCount else 0) +
                (if (r.isRunning) r.completedCount else 0)
        val total = (if (j.isRunning) j.totalCount else 0) +
                (if (t.isRunning) t.totalCount else 0) +
                (if (b.isRunning) b.totalCount else 0) +
                (if (g.isRunning) g.totalCount else 0) +
                (if (r.isRunning) r.totalCount else 0)
        SyncProgressInfo(running, status, completed, total)
    }

    private val userAndSettingsContextFlow = combine(
        repository.activeUser,
        repository.allUsers,
        repository.isSwipeToSwitchTabsEnabled,
        repository.isUserInitialized,
        syncProgressFlow
    ) { active, all, swipe, initialized, syncInfo ->
        UserAndSettingsContext(active, all, swipe, initialized, syncInfo)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _filterState,
        repository.getAllEntries(),
        repository.customCategories,
        repository.allSubCategories,
        userAndSettingsContextFlow
    ) { filter, allEntries, customCats, allSubCats, ctx ->
        val activeKey = filter.customCategory ?: filter.category.name

        // 1. Drawer category counts
        val catCounts = MediaType.entries.associateWith { cat ->
            allEntries.count { it.mediaType == cat && it.customCategory == null }
        }
        val customCatCounts = customCats.associate { customCat ->
            customCat.name to allEntries.count { it.customCategory.equals(customCat.name, ignoreCase = true) }
        }

        // 2. Entries in active category
        val categoryEntries = if (!filter.customCategory.isNullOrBlank()) {
            allEntries.filter { it.customCategory.equals(filter.customCategory, ignoreCase = true) }
        } else {
            allEntries.filter { it.mediaType == filter.category && it.customCategory == null }
        }

        // 3. Subcategories for the active category (NOT shared)
        val subCatsForActive = allSubCats
            .filter { it.categoryKey.equals(activeKey, ignoreCase = true) }
            .toMutableList()
        val existingNames = subCatsForActive.map { it.name.lowercase().trim() }.toSet()

        categoryEntries.mapNotNull { it.subCategory?.trim() }
            .filter { it.isNotBlank() && !existingNames.contains(it.lowercase()) }
            .distinct()
            .forEach { newName ->
                subCatsForActive.add(
                    SubCategory(
                        userId = ctx.activeUser.id,
                        categoryKey = activeKey,
                        name = newName
                    )
                )
            }

        val subCatCounts = subCatsForActive.associate { subCat ->
            subCat.name to categoryEntries.count { it.subCategory.equals(subCat.name, ignoreCase = true) }
        }

        // 4. Status counts for the active category
        val stCounts = MediaStatus.entries.associateWith { st ->
            categoryEntries.count { it.status == st }
        }

        // 5. Query filtering on category entries
        val queryFiltered = if (filter.query.isNotBlank()) {
            categoryEntries.filter {
                it.title.contains(filter.query, ignoreCase = true) ||
                        it.getAlternateTitles().any { alt -> alt.contains(filter.query, ignoreCase = true) } ||
                        (it.tags?.contains(filter.query, ignoreCase = true) == true) ||
                        (it.subCategory?.contains(filter.query, ignoreCase = true) == true)
            }
        } else {
            categoryEntries
        }

        // 6. Sort all category entries
        val sortedCategoryEntries = when (filter.sort) {
            SortOption.RECENTLY_UPDATED -> queryFiltered.sortedByDescending { it.updatedAt }
            SortOption.TITLE_ASC -> queryFiltered.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> queryFiltered.sortedByDescending { it.title.lowercase() }
            SortOption.SCORE_DESC -> queryFiltered.sortedByDescending { it.score ?: -1 }
            SortOption.PROGRESS_DESC -> queryFiltered.sortedByDescending { it.progressFraction }
            SortOption.TIME_SPENT_DESC -> queryFiltered.sortedWith(
                compareByDescending<MediaEntry> { it.calculateSpentTimeMinutes() }.thenByDescending { it.updatedAt }
            )
            SortOption.TIME_REMAINING_DESC -> queryFiltered.sortedWith(
                compareByDescending<MediaEntry> { it.calculateRemainingTimeMinutes() }.thenByDescending { it.updatedAt }
            )
        }

        // 7. Filter entries by active status or subcategory tab
        val sortedActiveFiltered = when {
            filter.status != null -> sortedCategoryEntries.filter { it.status == filter.status }
            !filter.subCategory.isNullOrBlank() -> sortedCategoryEntries.filter { it.subCategory.equals(filter.subCategory, ignoreCase = true) }
            else -> sortedCategoryEntries
        }

        HomeUiState(
            selectedCategory = filter.category,
            selectedCustomCategory = filter.customCategory,
            selectedSubCategory = filter.subCategory,
            selectedStatus = filter.status,
            searchQuery = filter.query,
            isSearchActive = filter.isSearching,
            selectedSort = filter.sort,
            isCategorySheetOpen = filter.isSheetOpen,
            isUserSwitcherOpen = filter.isUserSwitcherOpen,
            isAddSubCategoryDialogOpen = filter.isAddSubCategoryDialogOpen,
            isRefreshDialogOpen = filter.isRefreshDialogOpen,
            isSyncRunning = ctx.syncInfo.isRunning,
            syncStatusMessage = ctx.syncInfo.statusMessage,
            syncCompletedCount = ctx.syncInfo.completedCount,
            syncTotalCount = ctx.syncInfo.totalCount,
            entries = sortedActiveFiltered,
            allUserEntries = allEntries,
            categoryEntries = sortedCategoryEntries,
            statusCounts = stCounts,
            totalCount = categoryEntries.size,
            categoryCounts = catCounts,
            customCategories = customCats,
            customCategoryCounts = customCatCounts,
            subCategories = subCatsForActive,
            subCategoryCounts = subCatCounts,
            activeUser = ctx.activeUser,
            allUsers = ctx.allUsers,
            isSwipeToSwitchTabsEnabled = ctx.isSwipeEnabled,
            isUserInitialized = ctx.isUserInitialized
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun selectCategory(category: MediaType) {
        _filterState.update { it.copy(category = category, customCategory = null, subCategory = null, status = null) }
    }

    fun selectCustomCategory(customCategoryName: String?) {
        _filterState.update { it.copy(customCategory = customCategoryName, subCategory = null, status = null) }
    }

    fun selectSubCategory(subCategoryName: String?) {
        _filterState.update { it.copy(subCategory = subCategoryName, status = null) }
    }

    fun selectStatus(status: MediaStatus?) {
        _filterState.update { it.copy(status = status, subCategory = null) }
    }

    fun setAddSubCategoryDialogOpen(open: Boolean) {
        _filterState.update { it.copy(isAddSubCategoryDialogOpen = open) }
    }

    fun addSubCategory(name: String, colorHex: String = "#A7EF60") {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val activeKey = _filterState.value.customCategory ?: _filterState.value.category.name
        viewModelScope.launch {
            repository.addSubCategory(
                categoryKey = activeKey,
                name = trimmed,
                colorHex = colorHex
            )
            selectSubCategory(trimmed)
            setAddSubCategoryDialogOpen(false)
        }
    }

    fun deleteSubCategory(subCategory: SubCategory) {
        viewModelScope.launch {
            repository.deleteSubCategory(subCategory)
            if (_filterState.value.subCategory.equals(subCategory.name, ignoreCase = true)) {
                selectSubCategory(null)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(query = query) }
    }

    fun toggleSearch() {
        _filterState.update {
            val next = !it.isSearching
            it.copy(isSearching = next, query = if (!next) "" else it.query)
        }
    }

    fun selectSort(sort: SortOption) {
        _filterState.update { it.copy(sort = sort) }
    }

    fun setCategorySheetOpen(open: Boolean) {
        _filterState.update { it.copy(isSheetOpen = open) }
    }

    fun setUserSwitcherOpen(open: Boolean) {
        _filterState.update { it.copy(isUserSwitcherOpen = open) }
    }

    fun switchUser(profile: UserProfile) {
        val targetCat = profile.defaultMediaType ?: MediaType.ANIME
        val targetCustomCat = profile.defaultCustomCategory?.trim()?.ifBlank { null }
        val targetSubCat = profile.defaultSubCategory?.trim()?.ifBlank { null }
        val targetStatus = profile.defaultStatus
        _filterState.update {
            it.copy(
                category = targetCat,
                customCategory = targetCustomCat,
                subCategory = targetSubCat,
                status = if (targetSubCat != null) null else targetStatus,
                query = "",
                isSearching = false,
                isUserSwitcherOpen = false
            )
        }
        viewModelScope.launch {
            repository.switchUser(profile)
        }
    }

    fun createUser(name: String, colorHex: String, avatarImageUri: String? = null, passKey: String? = null) {
        viewModelScope.launch {
            repository.createUser(name, colorHex, avatarImageUri, passKey)
        }
    }

    fun updateUser(profile: UserProfile) {
        viewModelScope.launch {
            repository.updateUser(profile)
        }
    }

    fun deleteUser(profile: UserProfile) {
        viewModelScope.launch {
            repository.deleteUser(profile)
        }
    }

    fun incrementProgress(entry: MediaEntry) {
        viewModelScope.launch {
            repository.incrementProgress(entry)
        }
    }

    fun toggleFavorite(entry: MediaEntry) {
        viewModelScope.launch {
            repository.toggleFavorite(entry)
        }
    }

    fun deleteEntry(entry: MediaEntry) {
        viewModelScope.launch {
            repository.delete(entry)
        }
    }

    fun addRelatedToLibrary(item: com.lx.tracklist.data.model.RelatedMediaItem, status: MediaStatus, context: android.content.Context? = null) {
        viewModelScope.launch {
            repository.addRelatedItem(item, status, context, targetUserId = repository.activeUser.value.id)
        }
    }

    fun setRefreshDialogOpen(open: Boolean) {
        _filterState.update { it.copy(isRefreshDialogOpen = open) }
    }

    fun cancelSync(context: Context) {
        SyncForegroundService.cancelAllSyncs(context)
    }

    fun startRefresh(context: Context, scope: HomeRefreshScope, forceRefresh: Boolean) {
        val currentState = uiState.value
        val targetEntries: List<MediaEntry> = when (scope) {
            HomeRefreshScope.CURRENT_VIEW -> currentState.entries
            HomeRefreshScope.AIRING_ONLY -> currentState.categoryEntries.filter { it.isAiring }
            HomeRefreshScope.CURRENT_CATEGORY -> currentState.categoryEntries
            HomeRefreshScope.ALL_CATEGORIES -> currentState.allUserEntries
        }

        if (targetEntries.isEmpty()) return

        val jikanIds = mutableSetOf<Long>()
        val tmdbIds = mutableSetOf<Long>()
        val bookIds = mutableSetOf<Long>()
        val gameIds = mutableSetOf<Long>()
        val tachiyomiIds = mutableSetOf<Long>()

        for (entry in targetEntries) {
            val src = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim() ?: ""
            if (JikanSyncManager.isTachiyomiSource(entry) && !JikanSyncManager.hasValidExternalId(entry)) {
                tachiyomiIds.add(entry.id)
            } else if (src.equals("IGDB", ignoreCase = true) || src.equals("RAWG", ignoreCase = true) || entry.mediaType == MediaType.GAME) {
                gameIds.add(entry.id)
            } else if (src.equals("TMDB", ignoreCase = true)) {
                tmdbIds.add(entry.id)
            } else if (src.equals("Google Books", ignoreCase = true) || src.equals("Open Library", ignoreCase = true)) {
                bookIds.add(entry.id)
            } else if (src.equals("MAL", ignoreCase = true) || src.equals("MyAnimeList", ignoreCase = true)) {
                jikanIds.add(entry.id)
            } else if (entry.mediaType == MediaType.MOVIE || entry.mediaType == MediaType.SHOW) {
                tmdbIds.add(entry.id)
            } else if (entry.mediaType == MediaType.BOOK) {
                if (src.equals("AniList", ignoreCase = true) || src.equals("MAL", ignoreCase = true) || src.equals("MyAnimeList", ignoreCase = true)) {
                    jikanIds.add(entry.id)
                } else {
                    bookIds.add(entry.id)
                }
            } else {
                jikanIds.add(entry.id)
            }
        }

        val user = repository.activeUser.value
        if (tachiyomiIds.isNotEmpty()) {
            TachiyomiResolverManager.startResolution(
                context = context,
                repository = repository,
                targetEntryIds = tachiyomiIds,
                targetProfileId = user.id,
                targetProfileName = user.name
            )
        }
        if (jikanIds.isNotEmpty()) {
            JikanSyncManager.startSync(
                context = context,
                repository = repository,
                forceRefresh = forceRefresh,
                targetEntryIds = jikanIds,
                targetProfileId = user.id,
                targetProfileName = user.name
            )
        }
        if (tmdbIds.isNotEmpty()) {
            TmdbSyncManager.startSync(
                context = context,
                repository = repository,
                forceRefresh = forceRefresh,
                targetEntryIds = tmdbIds,
                targetProfileId = user.id,
                targetProfileName = user.name
            )
        }
        if (bookIds.isNotEmpty()) {
            BookSyncManager.startSync(
                context = context,
                repository = repository,
                forceRefresh = forceRefresh,
                targetEntryIds = bookIds,
                targetProfileId = user.id,
                targetProfileName = user.name
            )
        }
        if (gameIds.isNotEmpty()) {
            GameSyncManager.syncLibrary(
                context = context,
                repository = repository,
                profileId = user.id,
                profileName = user.name,
                forceRefresh = forceRefresh,
                targetEntryIds = gameIds
            )
        }
    }
}

class HomeViewModelFactory(private val repository: MediaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
