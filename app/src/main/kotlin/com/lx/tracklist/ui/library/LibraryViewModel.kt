package com.lx.tracklist.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.Priority
import com.lx.tracklist.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibrarySortOption(val displayName: String) {
    RECENTLY_UPDATED("Recently Updated"),
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    SCORE_DESC("Highest Score"),
    SCORE_ASC("Lowest Score"),
    PROGRESS_DESC("Highest Progress"),
    PROGRESS_ASC("Lowest Progress"),
    REWATCH_DESC("Most Re-watched/Re-read")
}

data class LibraryUiState(
    val entries: List<MediaEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryFilter: MediaType? = null,
    val selectedCustomCategoryFilter: String? = null,
    val selectedStatusFilter: MediaStatus? = null,
    val minScore: Int? = null,
    val onlyUnrated: Boolean = false,
    val selectedPriorityFilter: Priority? = null,
    val onlyFavorites: Boolean = false,
    val onlyAiring: Boolean = false,
    val selectedSort: LibrarySortOption = LibrarySortOption.RECENTLY_UPDATED,
    val totalCount: Int = 0,
    val selectedIds: Set<Long> = emptySet(),
    val customCategories: List<CustomCategory> = emptyList(),
    val isFilterSheetOpen: Boolean = false,
    val isSortMenuOpen: Boolean = false,
    val activeFilterCount: Int = 0
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()
}

private data class LibraryFilterState(
    val query: String = "",
    val categoryFilter: MediaType? = null,
    val customCategoryFilter: String? = null,
    val statusFilter: MediaStatus? = null,
    val minScore: Int? = null,
    val onlyUnrated: Boolean = false,
    val priorityFilter: Priority? = null,
    val onlyFavorites: Boolean = false,
    val onlyAiring: Boolean = false,
    val sortOption: LibrarySortOption = LibrarySortOption.RECENTLY_UPDATED,
    val selectedIds: Set<Long> = emptySet(),
    val isFilterSheetOpen: Boolean = false,
    val isSortMenuOpen: Boolean = false
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (categoryFilter != null || customCategoryFilter != null) count++
            if (statusFilter != null) count++
            if (minScore != null || onlyUnrated) count++
            if (priorityFilter != null) count++
            if (onlyFavorites) count++
            if (onlyAiring) count++
            return count
        }
}

class LibraryViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _filterState = MutableStateFlow(LibraryFilterState())

    val uiState: StateFlow<LibraryUiState> = combine(
        _filterState,
        repository.getAllEntries(),
        repository.customCategories
    ) { filter, allEntries, customCats ->
        var list = allEntries

        // 1. Category / Custom Category Filter
        if (filter.customCategoryFilter != null) {
            list = list.filter { it.customCategory == filter.customCategoryFilter }
        } else if (filter.categoryFilter != null) {
            list = list.filter { it.mediaType == filter.categoryFilter && it.customCategory == null }
        }

        // 2. Status Filter
        if (filter.statusFilter != null) {
            list = list.filter { it.status == filter.statusFilter }
        }

        // 3. Favorites Filter
        if (filter.onlyFavorites) {
            list = list.filter { it.isFavorite }
        }

        // 4. Airing Filter
        if (filter.onlyAiring) {
            list = list.filter { it.isAiring }
        }

        // 5. Priority Filter
        if (filter.priorityFilter != null) {
            list = list.filter { it.priority == filter.priorityFilter }
        }

        // 6. Score Filter
        if (filter.onlyUnrated) {
            list = list.filter { it.score == null || it.score == 0 }
        } else if (filter.minScore != null) {
            list = list.filter { (it.score ?: 0) >= filter.minScore }
        }

        // 7. Search Query Filter
        if (filter.query.isNotBlank()) {
            list = list.filter {
                it.title.contains(filter.query, ignoreCase = true) ||
                        (it.alternateTitle?.contains(filter.query, ignoreCase = true) == true) ||
                        (it.tags?.contains(filter.query, ignoreCase = true) == true)
            }
        }

        // 8. Sorting
        val sortedList = when (filter.sortOption) {
            LibrarySortOption.RECENTLY_UPDATED -> list.sortedByDescending { it.updatedAt }
            LibrarySortOption.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
            LibrarySortOption.TITLE_DESC -> list.sortedByDescending { it.title.lowercase() }
            LibrarySortOption.SCORE_DESC -> list.sortedWith(
                compareByDescending<MediaEntry> { it.score ?: -1 }.thenByDescending { it.updatedAt }
            )
            LibrarySortOption.SCORE_ASC -> list.sortedWith(
                compareBy<MediaEntry> { it.score ?: 99 }.thenByDescending { it.updatedAt }
            )
            LibrarySortOption.PROGRESS_DESC -> list.sortedByDescending { it.progressFraction }
            LibrarySortOption.PROGRESS_ASC -> list.sortedBy { it.progressFraction }
            LibrarySortOption.REWATCH_DESC -> list.sortedByDescending { it.rewatchCount }
        }

        LibraryUiState(
            entries = sortedList,
            searchQuery = filter.query,
            selectedCategoryFilter = filter.categoryFilter,
            selectedCustomCategoryFilter = filter.customCategoryFilter,
            selectedStatusFilter = filter.statusFilter,
            minScore = filter.minScore,
            onlyUnrated = filter.onlyUnrated,
            selectedPriorityFilter = filter.priorityFilter,
            onlyFavorites = filter.onlyFavorites,
            onlyAiring = filter.onlyAiring,
            selectedSort = filter.sortOption,
            totalCount = sortedList.size,
            selectedIds = filter.selectedIds,
            customCategories = customCats,
            isFilterSheetOpen = filter.isFilterSheetOpen,
            isSortMenuOpen = filter.isSortMenuOpen,
            activeFilterCount = filter.activeFilterCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(query = query) }
    }

    fun setCategoryFilter(category: MediaType?) {
        _filterState.update { it.copy(categoryFilter = category, customCategoryFilter = null) }
    }

    fun setCustomCategoryFilter(customCat: String?) {
        _filterState.update { it.copy(customCategoryFilter = customCat, categoryFilter = null) }
    }

    fun setStatusFilter(status: MediaStatus?) {
        _filterState.update { it.copy(statusFilter = status) }
    }

    fun setMinScore(score: Int?) {
        _filterState.update { it.copy(minScore = score, onlyUnrated = false) }
    }

    fun setOnlyUnrated(unrated: Boolean) {
        _filterState.update { it.copy(onlyUnrated = unrated, minScore = if (unrated) null else it.minScore) }
    }

    fun setPriorityFilter(priority: Priority?) {
        _filterState.update { it.copy(priorityFilter = priority) }
    }

    fun toggleFavoritesFilter() {
        _filterState.update { it.copy(onlyFavorites = !it.onlyFavorites) }
    }

    fun toggleAiringFilter() {
        _filterState.update { it.copy(onlyAiring = !it.onlyAiring) }
    }

    fun setSortOption(sort: LibrarySortOption) {
        _filterState.update { it.copy(sortOption = sort, isSortMenuOpen = false) }
    }

    fun setFilterSheetOpen(open: Boolean) {
        _filterState.update { it.copy(isFilterSheetOpen = open) }
    }

    fun setSortMenuOpen(open: Boolean) {
        _filterState.update { it.copy(isSortMenuOpen = open) }
    }

    fun resetAllFilters() {
        _filterState.update {
            it.copy(
                categoryFilter = null,
                customCategoryFilter = null,
                statusFilter = null,
                minScore = null,
                onlyUnrated = false,
                priorityFilter = null,
                onlyFavorites = false,
                onlyAiring = false
            )
        }
    }

    // Selection management
    fun toggleSelect(id: Long) {
        _filterState.update { state ->
            val current = state.selectedIds
            val updated = if (current.contains(id)) current - id else current + id
            state.copy(selectedIds = updated)
        }
    }

    fun selectAll(visibleIds: List<Long>) {
        _filterState.update { it.copy(selectedIds = visibleIds.toSet()) }
    }

    fun selectRange(visibleIds: List<Long>) {
        _filterState.update { state ->
            val current = state.selectedIds
            if (current.isEmpty()) return@update state
            val indices = visibleIds.mapIndexedNotNull { index, id ->
                if (current.contains(id)) index else null
            }
            if (indices.isEmpty()) return@update state
            val minIdx = indices.minOrNull() ?: 0
            val maxIdx = indices.maxOrNull() ?: (visibleIds.size - 1)
            val rangeIds = visibleIds.subList(minIdx, (maxIdx + 1).coerceAtMost(visibleIds.size)).toSet()
            state.copy(selectedIds = current + rangeIds)
        }
    }

    fun invertSelection(visibleIds: List<Long>) {
        _filterState.update { state ->
            val current = state.selectedIds
            state.copy(selectedIds = visibleIds.filter { !current.contains(it) }.toSet())
        }
    }

    fun clearSelection() {
        _filterState.update { it.copy(selectedIds = emptySet()) }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val toDelete = _filterState.value.selectedIds.toList()
            if (toDelete.isNotEmpty()) {
                repository.deleteEntries(toDelete)
                clearSelection()
            }
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
}

class LibraryViewModelFactory(private val repository: MediaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibraryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
