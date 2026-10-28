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

class LibraryViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _categoryFilter = MutableStateFlow<MediaType?>(null)
    private val _customCategoryFilter = MutableStateFlow<String?>(null)
    private val _onlyFavorites = MutableStateFlow(false)
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<LibraryUiState> = combine(
        _searchQuery,
        _categoryFilter,
        _customCategoryFilter,
        _onlyFavorites,
        _selectedIds,
        repository.getAllEntries(),
        repository.customCategories
    ) { query, catFilter, customCatFilter, favsOnly, selectedIds, allEntries, customCats ->
        var list = allEntries

        if (customCatFilter != null) {
            list = list.filter { it.customCategory == customCatFilter }
        } else if (catFilter != null) {
            list = list.filter { it.mediaType == catFilter && it.customCategory == null }
        }

        if (favsOnly) {
            list = list.filter { it.isFavorite }
        }

        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                        (it.alternateTitle?.contains(query, ignoreCase = true) == true) ||
                        (it.tags?.contains(query, ignoreCase = true) == true)
            }
        }

        LibraryUiState(
            entries = list,
            searchQuery = query,
            selectedCategoryFilter = catFilter,
            selectedCustomCategoryFilter = customCatFilter,
            onlyFavorites = favsOnly,
            totalCount = list.size,
            selectedIds = selectedIds,
            customCategories = customCats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: MediaType?) {
        _categoryFilter.value = category
        _customCategoryFilter.value = null
    }

    fun setCustomCategoryFilter(customCat: String?) {
        _customCategoryFilter.value = customCat
        _categoryFilter.value = null
    }

    fun toggleFavoritesFilter() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun toggleSelect(id: Long) {
        val current = _selectedIds.value
        _selectedIds.value = if (current.contains(id)) {
            current - id
        } else {
            current + id
        }
    }

    fun selectAll(visibleIds: List<Long>) {
        _selectedIds.value = visibleIds.toSet()
    }

    fun selectRange(visibleIds: List<Long>) {
        val current = _selectedIds.value
        if (current.isEmpty()) return
        val indices = visibleIds.mapIndexedNotNull { index, id ->
            if (current.contains(id)) index else null
        }
        if (indices.isEmpty()) return
        val minIdx = indices.minOrNull() ?: 0
        val maxIdx = indices.maxOrNull() ?: (visibleIds.size - 1)
        val rangeIds = visibleIds.subList(minIdx, (maxIdx + 1).coerceAtMost(visibleIds.size)).toSet()
        _selectedIds.value = current + rangeIds
    }

    fun invertSelection(visibleIds: List<Long>) {
        val current = _selectedIds.value
        _selectedIds.value = visibleIds.filter { !current.contains(it) }.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val toDelete = _selectedIds.value.toList()
            if (toDelete.isNotEmpty()) {
                repository.deleteEntries(toDelete)
                _selectedIds.value = emptySet()
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
