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
    val selectedSubCategory: String? = null,
    val selectedStatus: MediaStatus? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedSort: SortOption = SortOption.RECENTLY_UPDATED,
    val isCategorySheetOpen: Boolean = false,
    val isUserSwitcherOpen: Boolean = false,
    val isAddSubCategoryDialogOpen: Boolean = false,
    val entries: List<MediaEntry> = emptyList(),
    val statusCounts: Map<MediaStatus, Int> = emptyMap(),
    val totalCount: Int = 0,
    val categoryCounts: Map<MediaType, Int> = emptyMap(),
    val subCategories: List<CustomCategory> = emptyList(),
    val subCategoryCounts: Map<String, Int> = emptyMap(),
    val activeUser: UserProfile = UserProfile(id = 1L, name = "Main Profile", isDefault = true),
    val allUsers: List<UserProfile> = emptyList()
) {
    val selectedCustomCategory: String? get() = selectedSubCategory
    val customCategories: List<CustomCategory> get() = subCategories
    val customCategoryCounts: Map<String, Int> get() = subCategoryCounts
}

private data class FilterState(
    val category: MediaType = MediaType.ANIME,
    val subCategory: String? = null,
    val status: MediaStatus? = null,
    val query: String = "",
    val isSearching: Boolean = false,
    val sort: SortOption = SortOption.RECENTLY_UPDATED,
    val isSheetOpen: Boolean = false,
    val isUserSwitcherOpen: Boolean = false,
    val isAddSubCategoryDialogOpen: Boolean = false
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

        // 1. Main Category counts (All entries for each media type, including those with sub-categories)
        val catCounts = MediaType.entries.associateWith { cat ->
            allEntries.count { it.mediaType == cat }
        }

        // 2. All entries for the active main category (e.g. Manga, Manhwa, etc.)
        val mainCategoryEntries = allEntries.filter { it.mediaType == filter.category }

        // 3. Sub categories applicable to the active main category
        val subCatsForType = customCats
            .filter { it.mediaType == null || it.mediaType == filter.category }
            .toMutableList()
        val existingNames = subCatsForType.map { it.name.lowercase().trim() }.toSet()

        // Also discover any subcategories present in the imported/stored entries
        mainCategoryEntries.mapNotNull { it.customCategory?.trim() }
            .filter { it.isNotBlank() && !existingNames.contains(it.lowercase()) }
            .distinct()
            .forEach { newName ->
                subCatsForType.add(
                    CustomCategory(
                        name = newName,
                        mediaType = filter.category,
                        isWatchType = filter.category == MediaType.ANIME || filter.category == MediaType.SHOW || filter.category == MediaType.MOVIE
                    )
                )
            }

        // Subcategory counts for the current main category
        val subCatCounts = subCatsForType.associate { subCat ->
            subCat.name to mainCategoryEntries.count { it.customCategory.equals(subCat.name, ignoreCase = true) }
        }

        // 4. Filter entries by subcategory if one is selected
        val entriesInSubCategory = if (!filter.subCategory.isNullOrBlank()) {
            mainCategoryEntries.filter { it.customCategory.equals(filter.subCategory, ignoreCase = true) }
        } else {
            mainCategoryEntries
        }

        // 5. Status counts for the current selection
        val stCounts = MediaStatus.entries.associateWith { st ->
            entriesInSubCategory.count { it.status == st }
        }

        // 6. Filter by status if selected
        var filtered = if (filter.status != null) {
            entriesInSubCategory.filter { it.status == filter.status }
        } else {
            entriesInSubCategory
        }

        // 7. Filter by search query if active
        if (filter.query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(filter.query, ignoreCase = true) ||
                        (it.alternateTitle?.contains(filter.query, ignoreCase = true) == true) ||
                        (it.tags?.contains(filter.query, ignoreCase = true) == true) ||
                        (it.customCategory?.contains(filter.query, ignoreCase = true) == true)
            }
        }

        // 8. Sort entries
        val sorted = when (filter.sort) {
            SortOption.RECENTLY_UPDATED -> filtered.sortedByDescending { it.updatedAt }
            SortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            SortOption.SCORE_DESC -> filtered.sortedByDescending { it.score ?: -1 }
            SortOption.PROGRESS_DESC -> filtered.sortedByDescending { it.progressFraction }
        }

        HomeUiState(
            selectedCategory = filter.category,
            selectedSubCategory = filter.subCategory,
            selectedStatus = filter.status,
            searchQuery = filter.query,
            isSearchActive = filter.isSearching,
            selectedSort = filter.sort,
            isCategorySheetOpen = filter.isSheetOpen,
            isUserSwitcherOpen = filter.isUserSwitcherOpen,
            isAddSubCategoryDialogOpen = filter.isAddSubCategoryDialogOpen,
            entries = sorted,
            statusCounts = stCounts,
            totalCount = entriesInSubCategory.size,
            categoryCounts = catCounts,
            subCategories = subCatsForType,
            subCategoryCounts = subCatCounts,
            activeUser = activeUser,
            allUsers = allUsers
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun selectCategory(category: MediaType) {
        _filterState.update { it.copy(category = category, subCategory = null, status = null) }
    }

    fun selectSubCategory(subCategoryName: String?) {
        _filterState.update { it.copy(subCategory = subCategoryName, status = null) }
    }

    fun selectCustomCategory(customCategoryName: String?) {
        selectSubCategory(customCategoryName)
    }

    fun setAddSubCategoryDialogOpen(open: Boolean) {
        _filterState.update { it.copy(isAddSubCategoryDialogOpen = open) }
    }

    fun addSubCategory(name: String, colorHex: String = "#A7EF60") {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val currentCategory = _filterState.value.category
        viewModelScope.launch {
            repository.addCustomCategory(
                name = trimmed,
                mediaType = currentCategory,
                isWatchType = currentCategory == MediaType.ANIME || currentCategory == MediaType.SHOW || currentCategory == MediaType.MOVIE,
                colorHex = colorHex
            )
            selectSubCategory(trimmed)
            setAddSubCategoryDialogOpen(false)
        }
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

    fun createUser(name: String, colorHex: String, avatarImageUri: String? = null) {
        viewModelScope.launch {
            repository.createUser(name, colorHex, avatarImageUri)
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
