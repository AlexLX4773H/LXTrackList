package com.lx.tracklist.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.UserProfile
import com.lx.tracklist.data.repository.MediaRepository
import com.lx.tracklist.ui.components.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val selectedCategory: MediaType = MediaType.ANIME,
    val selectedCustomCategory: String? = null,
    val selectedStatus: MediaStatus? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedSort: SortOption = SortOption.RECENTLY_UPDATED,
    val isCategorySheetOpen: Boolean = false,
    val isUserSwitcherOpen: Boolean = false,
    val entries: List<MediaEntry> = emptyList(),
    val statusCounts: Map<MediaStatus, Int> = emptyMap(),
    val totalCount: Int = 0,
    val categoryCounts: Map<MediaType, Int> = emptyMap(),
    val customCategories: List<CustomCategory> = emptyList(),
    val customCategoryCounts: Map<String, Int> = emptyMap(),
    val activeUser: UserProfile = UserProfile(id = 1L, name = "Main Profile", isDefault = true),
    val allUsers: List<UserProfile> = emptyList()
)

private data class FilterState(
    val category: MediaType = MediaType.ANIME,
    val customCategory: String? = null,
    val status: MediaStatus? = null,
    val query: String = "",
    val isSearching: Boolean = false,
    val sort: SortOption = SortOption.RECENTLY_UPDATED,
    val isSheetOpen: Boolean = false,
    val isUserSwitcherOpen: Boolean = false
)

class HomeViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())

    val uiState: StateFlow<HomeUiState> = combine(
        _filterState,
        repository.getAllEntries(),
        repository.customCategories,
        repository.activeUser,
        repository.allUsers
    ) { filter, allEntries, customCats, activeUser, allUsers ->

        // Default Category counts
        val catCounts = MediaType.entries.associateWith { cat ->
            allEntries.count { it.mediaType == cat && it.customCategory == null }
        }

        // Custom Category counts
        val customCatCounts = customCats.associate { cat ->
            cat.name to allEntries.count { it.customCategory == cat.name }
        }

        // Entries for selected category / custom category
        val categoryEntries = if (filter.customCategory != null) {
            allEntries.filter { it.customCategory == filter.customCategory }
        } else {
            allEntries.filter { it.mediaType == filter.category && it.customCategory == null }
        }

        // Status counts for selected category
        val stCounts = MediaStatus.entries.associateWith { st ->
            categoryEntries.count { it.status == st }
        }

        // Filter by status if selected
        var filtered = if (filter.status != null) {
            categoryEntries.filter { it.status == filter.status }
        } else {
            categoryEntries
        }

        // Filter by search query if active
        if (filter.query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(filter.query, ignoreCase = true) ||
                        (it.alternateTitle?.contains(filter.query, ignoreCase = true) == true) ||
                        (it.tags?.contains(filter.query, ignoreCase = true) == true)
            }
        }

        // Sort entries
        val sorted = when (filter.sort) {
            SortOption.RECENTLY_UPDATED -> filtered.sortedByDescending { it.updatedAt }
            SortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            SortOption.SCORE_DESC -> filtered.sortedByDescending { it.score ?: -1 }
            SortOption.PROGRESS_DESC -> filtered.sortedByDescending { it.progressFraction }
        }

        HomeUiState(
            selectedCategory = filter.category,
            selectedCustomCategory = filter.customCategory,
            selectedStatus = filter.status,
            searchQuery = filter.query,
            isSearchActive = filter.isSearching,
            selectedSort = filter.sort,
            isCategorySheetOpen = filter.isSheetOpen,
            isUserSwitcherOpen = filter.isUserSwitcherOpen,
            entries = sorted,
            statusCounts = stCounts,
            totalCount = categoryEntries.size,
            categoryCounts = catCounts,
            customCategories = customCats,
            customCategoryCounts = customCatCounts,
            activeUser = activeUser,
            allUsers = allUsers
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun selectCategory(category: MediaType) {
        _filterState.update { it.copy(category = category, customCategory = null, status = null) }
    }

    fun selectCustomCategory(customCategoryName: String) {
        _filterState.update { it.copy(customCategory = customCategoryName, status = null) }
    }

    fun selectStatus(status: MediaStatus?) {
        _filterState.update { it.copy(status = status) }
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
        viewModelScope.launch {
            repository.switchUser(profile)
        }
    }

    fun createUser(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.createUser(name, colorHex)
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
