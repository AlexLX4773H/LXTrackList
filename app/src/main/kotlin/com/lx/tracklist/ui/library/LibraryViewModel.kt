package com.lx.tracklist.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    val onlyFavorites: Boolean = false,
    val totalCount: Int = 0
)

class LibraryViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _categoryFilter = MutableStateFlow<MediaType?>(null)
    private val _onlyFavorites = MutableStateFlow(false)

    val uiState: StateFlow<LibraryUiState> = combine(
        _searchQuery,
        _categoryFilter,
        _onlyFavorites,
        repository.getAllEntries()
    ) { query, catFilter, favsOnly, allEntries ->
        var list = allEntries

        if (catFilter != null) {
            list = list.filter { it.mediaType == catFilter }
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
            onlyFavorites = favsOnly,
            totalCount = list.size
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
    }

    fun toggleFavoritesFilter() {
        _onlyFavorites.value = !_onlyFavorites.value
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
