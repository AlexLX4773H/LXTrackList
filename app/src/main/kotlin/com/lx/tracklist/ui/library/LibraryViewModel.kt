package com.lx.tracklist.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val entries: List<MediaEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryFilter: MediaType? = null,
    val selectedCustomCategoryFilter: String? = null,
    val onlyFavorites: Boolean = false,
    val totalCount: Int = 0,
    val selectedIds: Set<Long> = emptySet(),
    val customCategories: List<CustomCategory> = emptyList()
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()
}

private data class LibraryFilterState(
    val query: String = "",
    val categoryFilter: MediaType? = null,
    val customCategoryFilter: String? = null,
    val onlyFavorites: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
)

class LibraryViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _filterState = MutableStateFlow(LibraryFilterState())

    val uiState: StateFlow<LibraryUiState> = combine(
        _filterState,
        repository.getAllEntries(),
        repository.customCategories
    ) { filter, allEntries, customCats ->
        var list = allEntries

        if (filter.customCategoryFilter != null) {
            list = list.filter { it.customCategory == filter.customCategoryFilter }
        } else if (filter.categoryFilter != null) {
            list = list.filter { it.mediaType == filter.categoryFilter && it.customCategory == null }
        }

        if (filter.onlyFavorites) {
            list = list.filter { it.isFavorite }
        }

        if (filter.query.isNotBlank()) {
            list = list.filter {
                it.title.contains(filter.query, ignoreCase = true) ||
                        (it.alternateTitle?.contains(filter.query, ignoreCase = true) == true) ||
                        (it.tags?.contains(filter.query, ignoreCase = true) == true)
            }
        }

        LibraryUiState(
            entries = list,
            searchQuery = filter.query,
            selectedCategoryFilter = filter.categoryFilter,
            selectedCustomCategoryFilter = filter.customCategoryFilter,
            onlyFavorites = filter.onlyFavorites,
            totalCount = list.size,
            selectedIds = filter.selectedIds,
            customCategories = customCats
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

    fun toggleFavoritesFilter() {
        _filterState.update { it.copy(onlyFavorites = !it.onlyFavorites) }
    }

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
