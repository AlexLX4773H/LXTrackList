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

data class SettingsUiState(
    val selectedConflictStrategy: ConflictStrategy = ConflictStrategy.MERGE,
    val isImporting: Boolean = false,
    val isExporting: Boolean = false,
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
        colorHex: String = "#A7EF60"
    ) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.addCustomCategory(
                name = trimmed,
                isWatchType = isWatchType,
                trackingUnit = trackingUnit,
                colorHex = colorHex
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
            _uiState.update { it.copy(isImporting = true, statusMessage = null) }
            try {
                val entries = withContext(Dispatchers.IO) {
                    val stream = StorageHelper.openFileInputStream(fileInfo.file)
                    parseFromStream(stream, fileInfo.name)
                }

                saveImportedEntries(entries, fileInfo.name)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
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
            _uiState.update { it.copy(isImporting = true, statusMessage = null) }
            var totalImported = 0
            var totalUpdated = 0
            var successFiles = 0

            withContext(Dispatchers.IO) {
                for (fileInfo in files) {
                    try {
                        val stream = StorageHelper.openFileInputStream(fileInfo.file)
                        val entries = parseFromStream(stream, fileInfo.name)
                        if (entries.isNotEmpty()) {
                            val (ins, upd) = repository.importEntries(entries, _uiState.value.selectedConflictStrategy)
                            totalImported += ins
                            totalUpdated += upd
                            successFiles++
                        }
                    } catch (_: Exception) {}
                }
            }

            _uiState.update {
                it.copy(
                    isImporting = false,
                    statusMessage = "Processed $successFiles files: $totalImported added, $totalUpdated updated",
                    isErrorMessage = false
                )
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
            _uiState.update { it.copy(isImporting = true, statusMessage = null) }
            try {
                val entries = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Cannot open selected file")
                    val fileName = uri.lastPathSegment?.lowercase() ?: ""
                    parseFromStream(stream, fileName)
                }

                saveImportedEntries(entries, uri.lastPathSegment ?: "file")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
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
