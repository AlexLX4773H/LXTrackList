package com.lx.tracklist.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.SubCategory
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

enum class TagSelectionState {
    UNSELECTED,
    INCLUDED,
    EXCLUDED
}

data class LibraryUiState(
    val entries: List<MediaEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryFilter: MediaType? = null,
    val selectedCustomCategoryFilter: String? = null,
    val selectedSubCategoryFilter: String? = null,
    val selectedStatusFilter: MediaStatus? = null,
    val minScore: Int? = null,
    val onlyUnrated: Boolean = false,
    val includedTags: Set<String> = emptySet(),
    val excludedTags: Set<String> = emptySet(),
    val availableTags: List<String> = emptyList(),
    val tagCounts: Map<String, Int> = emptyMap(),
    val onlyFavorites: Boolean = false,
    val onlyAiring: Boolean = false,
    val selectedSort: LibrarySortOption = LibrarySortOption.RECENTLY_UPDATED,
    val totalCount: Int = 0,
    val selectedIds: Set<Long> = emptySet(),
    val customCategories: List<CustomCategory> = emptyList(),
    val subCategories: List<SubCategory> = emptyList(),
    val isFilterSheetOpen: Boolean = false,
    val isSortMenuOpen: Boolean = false,
    val activeFilterCount: Int = 0
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()

    fun getTagState(tag: String): TagSelectionState {
        val clean = tag.trim()
        return when {
            includedTags.any { it.equals(clean, ignoreCase = true) } -> TagSelectionState.INCLUDED
            excludedTags.any { it.equals(clean, ignoreCase = true) } -> TagSelectionState.EXCLUDED
            else -> TagSelectionState.UNSELECTED
        }
    }
}

private data class LibraryFilterState(
    val query: String = "",
    val categoryFilter: MediaType? = null,
    val customCategoryFilter: String? = null,
    val subCategoryFilter: String? = null,
    val statusFilter: MediaStatus? = null,
    val minScore: Int? = null,
    val onlyUnrated: Boolean = false,
    val includedTags: Set<String> = emptySet(),
    val excludedTags: Set<String> = emptySet(),
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
            if (categoryFilter != null) count++
            if (customCategoryFilter != null) count++
            if (subCategoryFilter != null) count++
            if (statusFilter != null) count++
            if (minScore != null || onlyUnrated) count++
            if (includedTags.isNotEmpty() || excludedTags.isNotEmpty()) count++
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
        repository.customCategories,
        repository.allSubCategories
    ) { filter, allEntries, customCats, subCats ->
        // Distinct available tags and tag counts across all user entries
        val allAvailableTags = allEntries
            .flatMap { it.getTagList() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)

        val tagCounts = allAvailableTags.associateWith { tag ->
            allEntries.count { entry ->
                entry.getTagList().any { it.equals(tag, ignoreCase = true) }
            }
        }

        var list = allEntries

        // 1. Category / Custom Category / Sub Category Filter
        if (filter.categoryFilter != null) {
            list = list.filter { it.mediaType == filter.categoryFilter }
        }
        if (filter.customCategoryFilter != null) {
            list = list.filter { it.customCategory.equals(filter.customCategoryFilter, ignoreCase = true) }
        }
        if (filter.subCategoryFilter != null) {
            list = list.filter { it.subCategory.equals(filter.subCategoryFilter, ignoreCase = true) }
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

        // 5. Tags Filter (Multi-select Include & Inverse Exclude)
        if (filter.includedTags.isNotEmpty()) {
            list = list.filter { entry ->
                val entryTags = entry.getTagList()
                filter.includedTags.all { inc ->
                    entryTags.any { it.equals(inc, ignoreCase = true) }
                }
            }
        }
        if (filter.excludedTags.isNotEmpty()) {
            list = list.filter { entry ->
                val entryTags = entry.getTagList()
                filter.excludedTags.none { exc ->
                    entryTags.any { it.equals(exc, ignoreCase = true) }
                }
            }
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
                        it.getAlternateTitles().any { alt -> alt.contains(filter.query, ignoreCase = true) } ||
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
            selectedSubCategoryFilter = filter.subCategoryFilter,
            selectedStatusFilter = filter.statusFilter,
            minScore = filter.minScore,
            onlyUnrated = filter.onlyUnrated,
            includedTags = filter.includedTags,
            excludedTags = filter.excludedTags,
            availableTags = allAvailableTags,
            tagCounts = tagCounts,
            onlyFavorites = filter.onlyFavorites,
            onlyAiring = filter.onlyAiring,
            selectedSort = filter.sortOption,
            totalCount = sortedList.size,
            selectedIds = filter.selectedIds,
            customCategories = customCats,
            subCategories = subCats,
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
        _filterState.update { it.copy(categoryFilter = category) }
    }

    fun setCustomCategoryFilter(customCat: String?) {
        _filterState.update { it.copy(customCategoryFilter = customCat) }
    }

    fun setSubCategoryFilter(subCat: String?) {
        _filterState.update { it.copy(subCategoryFilter = subCat) }
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

    fun cycleTagState(tag: String) {
        _filterState.update { state ->
            val cleanTag = tag.trim()
            val isIncluded = state.includedTags.any { it.equals(cleanTag, ignoreCase = true) }
            val isExcluded = state.excludedTags.any { it.equals(cleanTag, ignoreCase = true) }

            when {
                !isIncluded && !isExcluded -> {
                    // 1st press: UNSELECTED -> INCLUDED (tick)
                    state.copy(
                        includedTags = state.includedTags + cleanTag,
                        excludedTags = state.excludedTags.filter { !it.equals(cleanTag, ignoreCase = true) }.toSet()
                    )
                }
                isIncluded -> {
                    // 2nd press: INCLUDED -> EXCLUDED (x)
                    state.copy(
                        includedTags = state.includedTags.filter { !it.equals(cleanTag, ignoreCase = true) }.toSet(),
                        excludedTags = state.excludedTags + cleanTag
                    )
                }
                else -> {
                    // 3rd press: EXCLUDED -> UNSELECTED (no select)
                    state.copy(
                        includedTags = state.includedTags.filter { !it.equals(cleanTag, ignoreCase = true) }.toSet(),
                        excludedTags = state.excludedTags.filter { !it.equals(cleanTag, ignoreCase = true) }.toSet()
                    )
                }
            }
        }
    }

    fun toggleIncludedTag(tag: String) {
        _filterState.update { state ->
            val cleanTag = tag.trim()
            val newIncluded = if (state.includedTags.any { it.equals(cleanTag, ignoreCase = true) }) {
                state.includedTags.filter { !it.equals(cleanTag, ignoreCase = true) }.toSet()
            } else {
                state.includedTags + cleanTag
            }
            val newExcluded = state.excludedTags.filter { !it.equals(cleanTag, ignoreCase = true) }.toSet()
            state.copy(includedTags = newIncluded, excludedTags = newExcluded)
        }
    }

    fun toggleExcludedTag(tag: String) {
        _filterState.update { state ->
            val cleanTag = tag.trim()
            val newExcluded = if (state.excludedTags.any { it.equals(cleanTag, ignoreCase = true) }) {
                state.excludedTags.filter { !it.equals(cleanTag, ignoreCase = true) }.toSet()
            } else {
                state.excludedTags + cleanTag
            }
            val newIncluded = state.includedTags.filter { !it.equals(cleanTag, ignoreCase = true) }.toSet()
            state.copy(includedTags = newIncluded, excludedTags = newExcluded)
        }
    }

    fun removeTagFilter(tag: String) {
        _filterState.update { state ->
            val cleanTag = tag.trim()
            state.copy(
                includedTags = state.includedTags.filter { !it.equals(cleanTag, ignoreCase = true) }.toSet(),
                excludedTags = state.excludedTags.filter { !it.equals(cleanTag, ignoreCase = true) }.toSet()
            )
        }
    }

    fun clearTagFilters() {
        _filterState.update { it.copy(includedTags = emptySet(), excludedTags = emptySet()) }
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
                subCategoryFilter = null,
                statusFilter = null,
                minScore = null,
                onlyUnrated = false,
                includedTags = emptySet(),
                excludedTags = emptySet(),
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

    fun batchUpdateCategory(mediaType: MediaType, customCategory: String? = null) {
        val selected = _filterState.value.selectedIds.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            repository.batchUpdateCategory(selected, mediaType, customCategory)
            clearSelection()
        }
    }

    fun batchUpdateSubCategory(subCategory: String?) {
        val selected = _filterState.value.selectedIds.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            repository.batchUpdateSubCategory(selected, subCategory)
            clearSelection()
        }
    }

    fun batchUpdateStatus(status: MediaStatus) {
        val selected = _filterState.value.selectedIds.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            repository.batchUpdateStatus(selected, status)
            clearSelection()
        }
    }

    fun batchUpdateFavorite(isFavorite: Boolean) {
        val selected = _filterState.value.selectedIds.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            repository.batchUpdateFavorite(selected, isFavorite)
            clearSelection()
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
