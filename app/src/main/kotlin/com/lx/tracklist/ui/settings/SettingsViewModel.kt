package com.lx.tracklist.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lx.tracklist.data.exporter.JsonExporter
import com.lx.tracklist.data.importer.CsvImporter
import com.lx.tracklist.data.importer.JsonImporter
import com.lx.tracklist.data.importer.MalXmlImporter
import com.lx.tracklist.data.importer.TachiyomiImporter
import com.lx.tracklist.data.local.StorageFileInfo
import com.lx.tracklist.data.local.StorageHelper
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.TrackingUnit
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
    val isCustomCategory: Boolean,
    val mediaType: MediaType? = null,
    val customCategoryName: String? = null,
    val count: Int
)

data class ImportPreviewState(
    val sourceName: String,
    val allEntries: List<MediaEntry>,
    val categoryGroups: List<ImportCategoryGroup>,
    val selectedCategoryKeys: Set<String> = emptySet(),
    val isImporting: Boolean = false
) {
    val totalCount: Int get() = allEntries.size
    val selectedCount: Int get() = allEntries.count { entry ->
        val key = getCategoryKeyForEntry(entry)
        selectedCategoryKeys.contains(key)
    }
}

fun getCategoryKeyForEntry(entry: MediaEntry): String {
    return if (!entry.customCategory.isNullOrBlank()) {
        "CUSTOM:${entry.customCategory.trim()}"
    } else {
        "TYPE:${entry.mediaType.name}"
    }
}

fun buildCategoryGroups(entries: List<MediaEntry>): List<ImportCategoryGroup> {
    val groups = mutableListOf<ImportCategoryGroup>()

    // 1. Built-in types
    for (type in MediaType.entries) {
        val count = entries.count { it.customCategory.isNullOrBlank() && it.mediaType == type }
        if (count > 0) {
            groups.add(
                ImportCategoryGroup(
                    key = "TYPE:${type.name}",
                    displayName = type.displayName,
                    isCustomCategory = false,
                    mediaType = type,
                    customCategoryName = null,
                    count = count
                )
            )
        }
    }

    // 2. Custom categories
    val customCatNames = entries.mapNotNull { it.customCategory?.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sortedWith(String.CASE_INSENSITIVE_ORDER)

    for (catName in customCatNames) {
        val count = entries.count { it.customCategory?.trim().equals(catName, ignoreCase = true) }
        if (count > 0) {
            groups.add(
                ImportCategoryGroup(
                    key = "CUSTOM:$catName",
                    displayName = catName,
                    isCustomCategory = true,
                    mediaType = null,
                    customCategoryName = catName,
                    count = count
                )
            )
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
    val customCategories: List<CustomCategory> = emptyList()
)

class SettingsViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.customCategories.collect { cats ->
                _uiState.update { it.copy(customCategories = cats) }
            }
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
                _uiState.update {
                    it.copy(
                        isPreparingImport = false,
                        importPreview = ImportPreviewState(
                            sourceName = fileInfo.name,
                            allEntries = entries,
                            categoryGroups = groups,
                            selectedCategoryKeys = allKeys
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
            _uiState.update {
                it.copy(
                    isPreparingImport = false,
                    importPreview = ImportPreviewState(
                        sourceName = "All ${files.size} input files",
                        allEntries = allParsedEntries,
                        categoryGroups = groups,
                        selectedCategoryKeys = allKeys
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
        val filteredEntries = preview.allEntries.filter { entry ->
            selectedKeys.contains(getCategoryKeyForEntry(entry))
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

            try {
                val (inserted, updated) = withContext(Dispatchers.IO) {
                    repository.importEntries(filteredEntries, _uiState.value.selectedConflictStrategy)
                }

                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importPreview = null,
                        statusMessage = "Imported ${filteredEntries.size} entries from ${preview.sourceName} ($inserted added, $updated updated)",
                        isErrorMessage = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importPreview = null,
                        statusMessage = "Import failed: ${e.localizedMessage ?: "Unknown error"}",
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
                _uiState.update {
                    it.copy(
                        isPreparingImport = false,
                        importPreview = ImportPreviewState(
                            sourceName = fileName,
                            allEntries = entries,
                            categoryGroups = groups,
                            selectedCategoryKeys = allKeys
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
                    TachiyomiImporter.parse(s)
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
                    if (malResult.isNotEmpty()) malResult else TachiyomiImporter.parse(java.io.ByteArrayInputStream(bytes))
                }
                else -> {
                    val bytes = s.readBytes()
                    try {
                        val tachibkResult = TachiyomiImporter.parse(java.io.ByteArrayInputStream(bytes))
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
        if (entries.isEmpty()) {
            _uiState.update {
                it.copy(
                    isImporting = false,
                    statusMessage = "No valid media entries found in $sourceName",
                    isErrorMessage = true
                )
            }
            return
        }

        val (inserted, updated) = withContext(Dispatchers.IO) {
            repository.importEntries(entries, _uiState.value.selectedConflictStrategy)
        }

        _uiState.update {
            it.copy(
                isImporting = false,
                statusMessage = "Imported ${entries.size} entries from $sourceName ($inserted added, $updated updated)",
                isErrorMessage = false
            )
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
