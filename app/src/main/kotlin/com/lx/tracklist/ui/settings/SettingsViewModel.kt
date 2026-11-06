package com.lx.tracklist.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lx.tracklist.LXApplication
import com.lx.tracklist.util.NotificationHelper
import com.lx.tracklist.data.exporter.JsonExporter
import com.lx.tracklist.data.importer.CsvImporter
import com.lx.tracklist.data.importer.JsonImporter
import com.lx.tracklist.data.importer.MalXmlImporter
import com.lx.tracklist.data.importer.TachiyomiImporter
import com.lx.tracklist.data.local.CoverCacheManager
import com.lx.tracklist.data.local.StorageFileInfo
import com.lx.tracklist.data.local.StorageHelper
import com.lx.tracklist.data.remote.JikanSyncManager
import com.lx.tracklist.data.remote.TmdbSyncManager
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.SubCategory
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.model.UserProfile
import com.lx.tracklist.data.repository.ConflictStrategy
import com.lx.tracklist.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

data class ImportCategoryGroup(
    val key: String,
    val displayName: String,
    val parentCategoryName: String? = null,
    val isSubCategory: Boolean = false,
    val isCustomCategory: Boolean = false,
    val mediaType: MediaType? = null,
    val customCategoryName: String? = null,
    val subCategoryName: String? = null,
    val count: Int
)

data class ImportPreviewState(
    val sourceName: String,
    val allEntries: List<MediaEntry>,
    val categoryGroups: List<ImportCategoryGroup>,
    val selectedCategoryKeys: Set<String> = emptySet(),
    val isImporting: Boolean = false,
    val isTachiyomiSource: Boolean = false,
    val importFavorites: Boolean = false
) {
    val totalCount: Int get() = allEntries.size
    val selectedCount: Int get() = allEntries.count { entry ->
        val key = getCategoryKeyForEntry(entry)
        selectedCategoryKeys.contains(key)
    }
}

fun getCategoryKeyForEntry(entry: MediaEntry): String {
    val baseKey = if (!entry.customCategory.isNullOrBlank()) {
        "CUSTOM:${entry.customCategory.trim()}"
    } else {
        "TYPE:${entry.mediaType.name}"
    }
    return if (!entry.subCategory.isNullOrBlank()) {
        "$baseKey:SUB:${entry.subCategory.trim()}"
    } else {
        baseKey
    }
}

fun buildCategoryGroups(entries: List<MediaEntry>): List<ImportCategoryGroup> {
    val groups = mutableListOf<ImportCategoryGroup>()

    // 1. Built-in types
    for (type in MediaType.entries) {
        val typeEntries = entries.filter { it.customCategory.isNullOrBlank() && it.mediaType == type }
        if (typeEntries.isEmpty()) continue

        val subCatNames = typeEntries.mapNotNull { it.subCategory?.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)

        if (subCatNames.isEmpty()) {
            groups.add(
                ImportCategoryGroup(
                    key = "TYPE:${type.name}",
                    displayName = type.displayName,
                    isCustomCategory = false,
                    isSubCategory = false,
                    mediaType = type,
                    customCategoryName = null,
                    count = typeEntries.size
                )
            )
        } else {
            for (subName in subCatNames) {
                val count = typeEntries.count { it.subCategory?.trim().equals(subName, ignoreCase = true) }
                if (count > 0) {
                    groups.add(
                        ImportCategoryGroup(
                            key = "TYPE:${type.name}:SUB:$subName",
                            displayName = subName,
                            parentCategoryName = type.displayName,
                            isCustomCategory = false,
                            isSubCategory = true,
                            mediaType = type,
                            subCategoryName = subName,
                            count = count
                        )
                    )
                }
            }
            val uncategorizedCount = typeEntries.count { it.subCategory.isNullOrBlank() }
            if (uncategorizedCount > 0) {
                groups.add(
                    ImportCategoryGroup(
                        key = "TYPE:${type.name}",
                        displayName = "${type.displayName} (Uncategorized)",
                        parentCategoryName = type.displayName,
                        isCustomCategory = false,
                        isSubCategory = false,
                        mediaType = type,
                        count = uncategorizedCount
                    )
                )
            }
        }
    }

    // 2. Custom categories
    val customCatNames = entries.mapNotNull { it.customCategory?.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sortedWith(String.CASE_INSENSITIVE_ORDER)

    for (catName in customCatNames) {
        val catEntries = entries.filter { it.customCategory?.trim().equals(catName, ignoreCase = true) }
        if (catEntries.isEmpty()) continue

        val subCatNames = catEntries.mapNotNull { it.subCategory?.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)

        if (subCatNames.isEmpty()) {
            groups.add(
                ImportCategoryGroup(
                    key = "CUSTOM:$catName",
                    displayName = catName,
                    isCustomCategory = true,
                    isSubCategory = false,
                    mediaType = null,
                    customCategoryName = catName,
                    count = catEntries.size
                )
            )
        } else {
            for (subName in subCatNames) {
                val count = catEntries.count { it.subCategory?.trim().equals(subName, ignoreCase = true) }
                if (count > 0) {
                    groups.add(
                        ImportCategoryGroup(
                            key = "CUSTOM:$catName:SUB:$subName",
                            displayName = subName,
                            parentCategoryName = catName,
                            isCustomCategory = true,
                            isSubCategory = true,
                            customCategoryName = catName,
                            subCategoryName = subName,
                            count = count
                        )
                    )
                }
            }
            val uncategorizedCount = catEntries.count { it.subCategory.isNullOrBlank() }
            if (uncategorizedCount > 0) {
                groups.add(
                    ImportCategoryGroup(
                        key = "CUSTOM:$catName",
                        displayName = "$catName (Uncategorized)",
                        parentCategoryName = catName,
                        isCustomCategory = true,
                        isSubCategory = false,
                        customCategoryName = catName,
                        count = uncategorizedCount
                    )
                )
            }
        }
    }

    return groups
}

enum class ExportDestination {
    HOME_OUTPUT_FOLDER,
    CUSTOM_FILE
}

data class ExportPreviewState(
    val destination: ExportDestination = ExportDestination.HOME_OUTPUT_FOLDER,
    val allEntries: List<MediaEntry>,
    val categoryGroups: List<ImportCategoryGroup>,
    val selectedCategoryKeys: Set<String> = emptySet(),
    val isExporting: Boolean = false
) {
    val totalCount: Int get() = allEntries.size
    val selectedCount: Int get() = allEntries.count { entry ->
        val key = getCategoryKeyForEntry(entry)
        selectedCategoryKeys.contains(key)
    }
    fun getFilteredEntries(): List<MediaEntry> = allEntries.filter { entry ->
        selectedCategoryKeys.contains(getCategoryKeyForEntry(entry))
    }
}

data class SettingsUiState(
    val selectedConflictStrategy: ConflictStrategy = ConflictStrategy.MERGE,
    val isImporting: Boolean = false,
    val isExporting: Boolean = false,
    val isPreparingImport: Boolean = false,
    val isPreparingExport: Boolean = false,
    val importPreview: ImportPreviewState? = null,
    val exportPreview: ExportPreviewState? = null,
    val statusMessage: String? = null,
    val isErrorMessage: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val inputFiles: List<StorageFileInfo> = emptyList(),
    val homeFolderPath: String = "",
    val customCategories: List<CustomCategory> = emptyList(),
    val allSubCategories: List<SubCategory> = emptyList(),
    val activeUser: UserProfile = UserProfile(id = 1L, name = "Main Profile", isDefault = true),
    val allUsers: List<UserProfile> = emptyList(),
    val selectedProfileIdForDefaults: Long? = null,
    val isSwipeToSwitchTabsEnabled: Boolean = true,
    val isTachiyomiImportAsFavorites: Boolean = false,
    val coverCacheSizeFormatted: String = "0 B",
    val coverCacheCount: Int = 0,
    val isReindexingCovers: Boolean = false,
    val reindexProgress: Float = 0f,
    val reindexStatusText: String? = null
)

class SettingsViewModel(private val repository: MediaRepository) : ViewModel() {

    val jikanSyncState = JikanSyncManager.syncState
    val tmdbSyncState = TmdbSyncManager.syncState

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.customCategories.collect { cats ->
                _uiState.update { it.copy(customCategories = cats) }
            }
        }
        viewModelScope.launch {
            repository.allSubCategories.collect { subCats ->
                _uiState.update { it.copy(allSubCategories = subCats) }
            }
        }
        viewModelScope.launch {
            repository.activeUser.collect { user ->
                _uiState.update { state ->
                    state.copy(
                        activeUser = user,
                        selectedProfileIdForDefaults = state.selectedProfileIdForDefaults ?: user.id
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.allUsers.collect { users ->
                _uiState.update { state ->
                    state.copy(
                        allUsers = users,
                        selectedProfileIdForDefaults = state.selectedProfileIdForDefaults ?: users.firstOrNull()?.id
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.isSwipeToSwitchTabsEnabled.collect { enabled ->
                _uiState.update { it.copy(isSwipeToSwitchTabsEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            repository.isTachiyomiImportAsFavorites.collect { enabled ->
                _uiState.update { it.copy(isTachiyomiImportAsFavorites = enabled) }
            }
        }
    }

    fun selectProfileForDefaults(profileId: Long) {
        _uiState.update { it.copy(selectedProfileIdForDefaults = profileId) }
    }

    fun startJikanSync(context: Context, forceRefresh: Boolean = false) {
        JikanSyncManager.startSync(context, repository, forceRefresh)
    }

    fun cancelJikanSync(context: Context? = null) {
        JikanSyncManager.cancelSync(context)
    }

    fun startTmdbSync(context: Context, forceRefresh: Boolean = false) {
        TmdbSyncManager.startSync(context, repository, forceRefresh)
    }

    fun cancelTmdbSync(context: Context? = null) {
        TmdbSyncManager.cancelSync(context)
    }

    fun updateStartupDefaults(
        profileId: Long,
        defaultMediaType: MediaType?,
        defaultCustomCategory: String?,
        defaultSubCategory: String?,
        defaultStatus: MediaStatus?
    ) {
        viewModelScope.launch {
            repository.updateProfileStartupDefaults(
                profileId = profileId,
                defaultMediaType = defaultMediaType,
                defaultCustomCategory = defaultCustomCategory,
                defaultSubCategory = defaultSubCategory,
                defaultStatus = defaultStatus
            )
            val profileName = _uiState.value.allUsers.firstOrNull { it.id == profileId }?.name ?: "Profile"
            val categoryLabel = defaultCustomCategory ?: (defaultMediaType ?: MediaType.ANIME).displayName
            val tabLabel = defaultSubCategory ?: (defaultStatus?.getDisplayLabel(defaultMediaType ?: MediaType.ANIME) ?: "All")
            _uiState.update {
                it.copy(
                    statusMessage = "Startup defaults updated for \"$profileName\" ($categoryLabel • $tabLabel)",
                    isErrorMessage = false
                )
            }
        }
    }

    fun resetStartupDefaults(profileId: Long) {
        updateStartupDefaults(
            profileId = profileId,
            defaultMediaType = MediaType.ANIME,
            defaultCustomCategory = null,
            defaultSubCategory = null,
            defaultStatus = null
        )
    }

    fun setSwipeToSwitchTabsEnabled(enabled: Boolean) {
        repository.setSwipeToSwitchTabsEnabled(enabled)
    }

    fun setTachiyomiImportAsFavorites(enabled: Boolean) {
        repository.setTachiyomiImportAsFavorites(enabled)
    }

    fun setImportFavorites(enabled: Boolean) {
        repository.setTachiyomiImportAsFavorites(enabled)
        _uiState.update { state ->
            val preview = state.importPreview ?: return@update state
            state.copy(
                isTachiyomiImportAsFavorites = enabled,
                importPreview = preview.copy(importFavorites = enabled)
            )
        }
    }

    fun addCustomCategory(
        name: String,
        isWatchType: Boolean,
        trackingUnit: TrackingUnit = TrackingUnit.CHAPTERS,
        colorHex: String = "#A7EF60",
        mediaType: MediaType? = null
    ) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.addCustomCategory(
                name = trimmed,
                isWatchType = isWatchType,
                trackingUnit = trackingUnit,
                colorHex = colorHex,
                mediaType = mediaType
            )
            _uiState.update {
                it.copy(
                    statusMessage = "Created custom category \"$trimmed\"",
                    isErrorMessage = false
                )
            }
        }
    }

    fun deleteCustomCategory(category: CustomCategory) {
        viewModelScope.launch {
            repository.deleteCustomCategory(category)
            val affected = _uiState.value.allUsers.filter { it.defaultCustomCategory.equals(category.name, ignoreCase = true) }
            for (p in affected) {
                repository.updateUser(p.copy(defaultCustomCategory = null, defaultMediaType = MediaType.ANIME, defaultSubCategory = null))
            }
            _uiState.update {
                it.copy(
                    statusMessage = "Deleted category \"${category.name}\"",
                    isErrorMessage = false
                )
            }
        }
    }

    fun checkStorageStatus(context: Context) {
        val hasPerm = StorageHelper.hasStoragePermission(context)
        val files = if (hasPerm) StorageHelper.listInputFiles() else emptyList()
        val path = if (hasPerm) StorageHelper.getBaseDir().absolutePath else "/storage/emulated/0/LXTrackList"

        _uiState.update {
            it.copy(
                hasStoragePermission = hasPerm,
                inputFiles = files,
                homeFolderPath = path
            )
        }
        refreshCoverCacheStats(context)
    }

    fun refreshCoverCacheStats(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val sizeBytes = CoverCacheManager.getCacheSizeBytes(context)
            val count = CoverCacheManager.getCachedFileCount(context)
            val formatted = CoverCacheManager.formatBytes(sizeBytes)
            _uiState.update {
                it.copy(
                    coverCacheSizeFormatted = formatted,
                    coverCacheCount = count
                )
            }
        }
    }

    fun clearCoverCache(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            CoverCacheManager.clearCache(context)
            val sizeBytes = CoverCacheManager.getCacheSizeBytes(context)
            val count = CoverCacheManager.getCachedFileCount(context)
            val formatted = CoverCacheManager.formatBytes(sizeBytes)
            _uiState.update {
                it.copy(
                    coverCacheSizeFormatted = formatted,
                    coverCacheCount = count,
                    statusMessage = "Cover image cache cleared",
                    isErrorMessage = false
                )
            }
        }
    }

    fun reindexCoverImages(context: Context) {
        if (_uiState.value.isReindexingCovers) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isReindexingCovers = true,
                    reindexProgress = 0f,
                    reindexStatusText = "Scanning entries...",
                    statusMessage = null
                )
            }

            try {
                val allEntries = withContext(Dispatchers.IO) {
                    repository.getAllEntriesAllUsersSync()
                }

                val result = CoverCacheManager.reindexCovers(context, allEntries) { current, total, success, failed ->
                    val progress = if (total > 0) current.toFloat() / total.toFloat() else 1f
                    _uiState.update {
                        it.copy(
                            reindexProgress = progress,
                            reindexStatusText = "Reindexing: $current / $total ($success cached${if (failed > 0) ", $failed failed" else ""})"
                        )
                    }
                }

                val sizeBytes = withContext(Dispatchers.IO) { CoverCacheManager.getCacheSizeBytes(context) }
                val count = withContext(Dispatchers.IO) { CoverCacheManager.getCachedFileCount(context) }
                val formatted = CoverCacheManager.formatBytes(sizeBytes)

                _uiState.update {
                    it.copy(
                        isReindexingCovers = false,
                        reindexProgress = 1f,
                        reindexStatusText = null,
                        coverCacheSizeFormatted = formatted,
                        coverCacheCount = count,
                        statusMessage = if (result.total == 0) {
                            "No entries with web cover URLs found to reindex"
                        } else {
                            "Reindexed covers: ${result.succeeded} cached successfully${if (result.failed > 0) ", ${result.failed} failed/unreachable" else ""}"
                        },
                        isErrorMessage = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isReindexingCovers = false,
                        reindexProgress = 0f,
                        reindexStatusText = null,
                        statusMessage = "Reindex failed: ${e.localizedMessage ?: "Unknown error"}",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun requestStoragePermission(context: Context) {
        StorageHelper.requestStoragePermission(context)
    }

    fun setConflictStrategy(strategy: ConflictStrategy) {
        _uiState.update { it.copy(selectedConflictStrategy = strategy) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun importLocalFile(context: Context, fileInfo: StorageFileInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingImport = true, statusMessage = null) }
            try {
                val entries = withContext(Dispatchers.IO) {
                    val stream = StorageHelper.openFileInputStream(fileInfo.file)
                    parseFromStream(stream, fileInfo.name)
                }

                if (entries.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isPreparingImport = false,
                            statusMessage = "No valid media entries found in ${fileInfo.name}",
                            isErrorMessage = true
                        )
                    }
                    return@launch
                }

                val groups = buildCategoryGroups(entries)
                val allKeys = groups.map { it.key }.toSet()
                val isTachiyomi = fileInfo.name.endsWith(".tachibk", ignoreCase = true) ||
                        fileInfo.name.endsWith(".proto.gz", ignoreCase = true) ||
                        fileInfo.fileType.contains("Tachiyomi", ignoreCase = true) ||
                        entries.any { it.externalSource == "TACHIYOMI" }

                _uiState.update {
                    it.copy(
                        isPreparingImport = false,
                        importPreview = ImportPreviewState(
                            sourceName = fileInfo.name,
                            allEntries = entries,
                            categoryGroups = groups,
                            selectedCategoryKeys = allKeys,
                            isTachiyomiSource = isTachiyomi,
                            importFavorites = _uiState.value.isTachiyomiImportAsFavorites
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPreparingImport = false,
                        statusMessage = "Import failed: ${e.localizedMessage ?: "Unknown error"}",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun importAllLocalFiles(context: Context) {
        val files = _uiState.value.inputFiles
        if (files.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingImport = true, statusMessage = null) }
            val allParsedEntries = mutableListOf<MediaEntry>()

            withContext(Dispatchers.IO) {
                for (fileInfo in files) {
                    try {
                        val stream = StorageHelper.openFileInputStream(fileInfo.file)
                        val entries = parseFromStream(stream, fileInfo.name)
                        allParsedEntries.addAll(entries)
                    } catch (_: Exception) {}
                }
            }

            if (allParsedEntries.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isPreparingImport = false,
                        statusMessage = "No valid entries found across ${files.size} input files",
                        isErrorMessage = true
                    )
                }
                return@launch
            }

            val groups = buildCategoryGroups(allParsedEntries)
            val allKeys = groups.map { it.key }.toSet()
            val isTachiyomi = files.any {
                it.name.endsWith(".tachibk", ignoreCase = true) ||
                it.name.endsWith(".proto.gz", ignoreCase = true) ||
                it.fileType.contains("Tachiyomi", ignoreCase = true)
            } || allParsedEntries.any { it.externalSource == "TACHIYOMI" }

            _uiState.update {
                it.copy(
                    isPreparingImport = false,
                    importPreview = ImportPreviewState(
                        sourceName = "All ${files.size} input files",
                        allEntries = allParsedEntries,
                        categoryGroups = groups,
                        selectedCategoryKeys = allKeys,
                        isTachiyomiSource = isTachiyomi,
                        importFavorites = _uiState.value.isTachiyomiImportAsFavorites
                    )
                )
            }
        }
    }

    fun toggleCategorySelection(categoryKey: String) {
        _uiState.update { state ->
            val preview = state.importPreview ?: return@update state
            val currentKeys = preview.selectedCategoryKeys
            val newKeys = if (currentKeys.contains(categoryKey)) {
                currentKeys - categoryKey
            } else {
                currentKeys + categoryKey
            }
            state.copy(importPreview = preview.copy(selectedCategoryKeys = newKeys))
        }
    }

    fun selectAllCategories() {
        _uiState.update { state ->
            val preview = state.importPreview ?: return@update state
            val allKeys = preview.categoryGroups.map { it.key }.toSet()
            state.copy(importPreview = preview.copy(selectedCategoryKeys = allKeys))
        }
    }

    fun deselectAllCategories() {
        _uiState.update { state ->
            val preview = state.importPreview ?: return@update state
            state.copy(importPreview = preview.copy(selectedCategoryKeys = emptySet()))
        }
    }

    fun cancelImportPreview() {
        _uiState.update { it.copy(importPreview = null) }
    }

    fun confirmImport() {
        val preview = _uiState.value.importPreview ?: return
        val selectedKeys = preview.selectedCategoryKeys
        val filteredEntries = preview.allEntries
            .filter { entry -> selectedKeys.contains(getCategoryKeyForEntry(entry)) }
            .map { entry ->
                if (preview.isTachiyomiSource) {
                    entry.copy(isFavorite = preview.importFavorites)
                } else {
                    entry
                }
            }

        if (filteredEntries.isEmpty()) {
            _uiState.update {
                it.copy(
                    importPreview = null,
                    statusMessage = "No categories were selected for import",
                    isErrorMessage = true
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isImporting = true,
                    importPreview = preview.copy(isImporting = true)
                )
            }

            val appContext = LXApplication.instance.applicationContext
            NotificationHelper.showImportProgress(appContext, preview.sourceName)

            try {
                val (inserted, updated) = withContext(Dispatchers.IO) {
                    repository.importEntries(filteredEntries, _uiState.value.selectedConflictStrategy)
                }

                NotificationHelper.showImportComplete(
                    context = appContext,
                    sourceName = preview.sourceName,
                    totalCount = filteredEntries.size,
                    addedCount = inserted,
                    updatedCount = updated
                )

                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importPreview = null,
                        statusMessage = null,
                        isErrorMessage = false
                    )
                }
            } catch (e: Exception) {
                NotificationHelper.showImportFailed(
                    context = appContext,
                    sourceName = preview.sourceName,
                    errorMessage = e.localizedMessage ?: "Unknown error"
                )

                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importPreview = null,
                        statusMessage = null,
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    private var pendingCustomExportEntries: List<MediaEntry>? = null

    fun prepareExport(destination: ExportDestination) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingExport = true, statusMessage = null) }
            try {
                val allEntries = withContext(Dispatchers.IO) {
                    repository.getAllEntriesSync()
                }

                if (allEntries.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isPreparingExport = false,
                            statusMessage = "No media entries found in library to export",
                            isErrorMessage = true
                        )
                    }
                    return@launch
                }

                val groups = buildCategoryGroups(allEntries)
                val allKeys = groups.map { it.key }.toSet()
                _uiState.update {
                    it.copy(
                        isPreparingExport = false,
                        exportPreview = ExportPreviewState(
                            destination = destination,
                            allEntries = allEntries,
                            categoryGroups = groups,
                            selectedCategoryKeys = allKeys
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPreparingExport = false,
                        statusMessage = "Failed to load entries for export: ${e.localizedMessage ?: "Unknown error"}",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun setExportDestination(destination: ExportDestination) {
        _uiState.update { state ->
            val preview = state.exportPreview ?: return@update state
            state.copy(exportPreview = preview.copy(destination = destination))
        }
    }

    fun toggleExportCategorySelection(categoryKey: String) {
        _uiState.update { state ->
            val preview = state.exportPreview ?: return@update state
            val currentKeys = preview.selectedCategoryKeys
            val newKeys = if (currentKeys.contains(categoryKey)) {
                currentKeys - categoryKey
            } else {
                currentKeys + categoryKey
            }
            state.copy(exportPreview = preview.copy(selectedCategoryKeys = newKeys))
        }
    }

    fun selectAllExportCategories() {
        _uiState.update { state ->
            val preview = state.exportPreview ?: return@update state
            val allKeys = preview.categoryGroups.map { it.key }.toSet()
            state.copy(exportPreview = preview.copy(selectedCategoryKeys = allKeys))
        }
    }

    fun deselectAllExportCategories() {
        _uiState.update { state ->
            val preview = state.exportPreview ?: return@update state
            state.copy(exportPreview = preview.copy(selectedCategoryKeys = emptySet()))
        }
    }

    fun cancelExportPreview() {
        _uiState.update { it.copy(exportPreview = null) }
    }

    fun confirmExportToHomeOutput(context: Context) {
        val preview = _uiState.value.exportPreview ?: return
        val filteredEntries = preview.getFilteredEntries()

        if (filteredEntries.isEmpty()) {
            _uiState.update {
                it.copy(
                    exportPreview = null,
                    statusMessage = "No categories were selected for export",
                    isErrorMessage = true
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isExporting = true,
                    exportPreview = preview.copy(isExporting = true)
                )
            }
            try {
                val json = JsonExporter.exportToJson(filteredEntries)
                val targetFile = withContext(Dispatchers.IO) {
                    StorageHelper.exportToJsonFile(json)
                }

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportPreview = null,
                        statusMessage = "Saved ${filteredEntries.size} entries to:\n${targetFile.absolutePath}",
                        isErrorMessage = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportPreview = null,
                        statusMessage = "Export failed: ${e.localizedMessage ?: "Unknown error"}",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun startCustomExport(onLaunchPicker: () -> Unit) {
        val preview = _uiState.value.exportPreview ?: return
        val filteredEntries = preview.getFilteredEntries()
        if (filteredEntries.isEmpty()) {
            _uiState.update {
                it.copy(
                    exportPreview = null,
                    statusMessage = "No categories were selected for export",
                    isErrorMessage = true
                )
            }
            return
        }
        pendingCustomExportEntries = filteredEntries
        _uiState.update { it.copy(exportPreview = null) }
        onLaunchPicker()
    }

    fun exportPendingToCustomUri(context: Context, destinationUri: Uri) {
        val entries = pendingCustomExportEntries
        if (entries == null) {
            exportToCustomUri(context, destinationUri)
            return
        }
        pendingCustomExportEntries = null

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, statusMessage = null) }
            try {
                withContext(Dispatchers.IO) {
                    val outputStream = context.contentResolver.openOutputStream(destinationUri)
                        ?: throw IllegalStateException("Cannot write to destination file")
                    outputStream.use { out ->
                        JsonExporter.writeToStream(entries, out)
                    }
                }

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        statusMessage = "Successfully exported ${entries.size} entries to JSON backup",
                        isErrorMessage = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        statusMessage = "Export failed: ${e.localizedMessage ?: "Unknown error"}",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun exportToHomeOutput(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, statusMessage = null) }
            try {
                val allEntries = withContext(Dispatchers.IO) {
                    repository.getAllEntriesSync()
                }

                val json = JsonExporter.exportToJson(allEntries)
                val targetFile = withContext(Dispatchers.IO) {
                    StorageHelper.exportToJsonFile(json)
                }

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        statusMessage = "Saved ${allEntries.size} entries to:\n${targetFile.absolutePath}",
                        isErrorMessage = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        statusMessage = "Export failed: ${e.localizedMessage ?: "Unknown error"}",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun importFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingImport = true, statusMessage = null) }
            try {
                val fileName = uri.lastPathSegment ?: "Selected File"
                val entries = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Cannot open selected file")
                    parseFromStream(stream, fileName)
                }

                if (entries.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isPreparingImport = false,
                            statusMessage = "No valid media entries found in $fileName",
                            isErrorMessage = true
                        )
                    }
                    return@launch
                }

                val groups = buildCategoryGroups(entries)
                val allKeys = groups.map { it.key }.toSet()
                val isTachiyomi = fileName.endsWith(".tachibk", ignoreCase = true) ||
                        fileName.endsWith(".proto.gz", ignoreCase = true) ||
                        entries.any { it.externalSource == "TACHIYOMI" }

                _uiState.update {
                    it.copy(
                        isPreparingImport = false,
                        importPreview = ImportPreviewState(
                            sourceName = fileName,
                            allEntries = entries,
                            categoryGroups = groups,
                            selectedCategoryKeys = allKeys,
                            isTachiyomiSource = isTachiyomi,
                            importFavorites = _uiState.value.isTachiyomiImportAsFavorites
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPreparingImport = false,
                        statusMessage = "Import failed: ${e.localizedMessage ?: "Unknown error"}",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    fun exportToCustomUri(context: Context, destinationUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, statusMessage = null) }
            try {
                val allEntries = withContext(Dispatchers.IO) {
                    repository.getAllEntriesSync()
                }

                withContext(Dispatchers.IO) {
                    val outputStream = context.contentResolver.openOutputStream(destinationUri)
                        ?: throw IllegalStateException("Cannot write to destination file")
                    outputStream.use { out ->
                        JsonExporter.writeToStream(allEntries, out)
                    }
                }

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        statusMessage = "Successfully exported ${allEntries.size} entries to JSON backup",
                        isErrorMessage = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        statusMessage = "Export failed: ${e.localizedMessage ?: "Unknown error"}",
                        isErrorMessage = true
                    )
                }
            }
        }
    }

    private fun parseFromStream(stream: InputStream, name: String): List<MediaEntry> {
        val fileName = name.lowercase()
        return stream.use { s ->
            when {
                fileName.endsWith(".tachibk") || fileName.endsWith(".proto.gz") -> {
                    TachiyomiImporter.parse(s, importAsFavorites = _uiState.value.isTachiyomiImportAsFavorites)
                }
                fileName.endsWith(".xml") || fileName.endsWith(".xml.gz") -> {
                    MalXmlImporter.parse(s)
                }
                fileName.endsWith(".csv") -> {
                    CsvImporter.parse(s)
                }
                fileName.endsWith(".json") -> {
                    JsonImporter.parse(s)
                }
                fileName.endsWith(".gz") -> {
                    val bytes = s.readBytes()
                    val malResult = try {
                        MalXmlImporter.parse(java.io.ByteArrayInputStream(bytes))
                    } catch (_: Exception) {
                        emptyList()
                    }
                    if (malResult.isNotEmpty()) malResult else TachiyomiImporter.parse(java.io.ByteArrayInputStream(bytes), importAsFavorites = _uiState.value.isTachiyomiImportAsFavorites)
                }
                else -> {
                    val bytes = s.readBytes()
                    try {
                        val tachibkResult = TachiyomiImporter.parse(java.io.ByteArrayInputStream(bytes), importAsFavorites = _uiState.value.isTachiyomiImportAsFavorites)
                        if (tachibkResult.isNotEmpty()) return@use tachibkResult
                    } catch (_: Exception) {}

                    try {
                        val malResult = MalXmlImporter.parse(java.io.ByteArrayInputStream(bytes))
                        if (malResult.isNotEmpty()) return@use malResult
                    } catch (_: Exception) {}

                    try {
                        val jsonResult = JsonImporter.parse(java.io.ByteArrayInputStream(bytes))
                        if (jsonResult.isNotEmpty()) return@use jsonResult
                    } catch (_: Exception) {}

                    CsvImporter.parse(java.io.ByteArrayInputStream(bytes))
                }
            }
        }
    }

    private suspend fun saveImportedEntries(entries: List<MediaEntry>, sourceName: String) {
        val appContext = LXApplication.instance.applicationContext
        if (entries.isEmpty()) {
            NotificationHelper.showImportFailed(appContext, sourceName, "No valid media entries found")
            _uiState.update {
                it.copy(
                    isImporting = false,
                    statusMessage = null,
                    isErrorMessage = true
                )
            }
            return
        }

        NotificationHelper.showImportProgress(appContext, sourceName)
        try {
            val (inserted, updated) = withContext(Dispatchers.IO) {
                repository.importEntries(entries, _uiState.value.selectedConflictStrategy)
            }

            NotificationHelper.showImportComplete(
                context = appContext,
                sourceName = sourceName,
                totalCount = entries.size,
                addedCount = inserted,
                updatedCount = updated
            )

            _uiState.update {
                it.copy(
                    isImporting = false,
                    statusMessage = null,
                    isErrorMessage = false
                )
            }
        } catch (e: Exception) {
            NotificationHelper.showImportFailed(
                context = appContext,
                sourceName = sourceName,
                errorMessage = e.localizedMessage ?: "Unknown error"
            )
            _uiState.update {
                it.copy(
                    isImporting = false,
                    statusMessage = null,
                    isErrorMessage = true
                )
            }
        }
    }
}

class SettingsViewModelFactory(private val repository: MediaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
