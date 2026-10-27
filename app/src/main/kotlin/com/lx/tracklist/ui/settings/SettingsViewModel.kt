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
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.repository.ConflictStrategy
import com.lx.tracklist.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val selectedConflictStrategy: ConflictStrategy = ConflictStrategy.MERGE,
    val isImporting: Boolean = false,
    val isExporting: Boolean = false,
    val statusMessage: String? = null,
    val isErrorMessage: Boolean = false
)

class SettingsViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setConflictStrategy(strategy: ConflictStrategy) {
        _uiState.update { it.copy(selectedConflictStrategy = strategy) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun importFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, statusMessage = null) }
            try {
                val entries = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Cannot open selected file")

                    val fileName = uri.lastPathSegment?.lowercase() ?: ""
                    stream.use { s ->
                        when {
                            fileName.endsWith(".xml") || fileName.endsWith(".xml.gz") || fileName.endsWith(".gz") -> {
                                MalXmlImporter.parse(s)
                            }
                            fileName.endsWith(".csv") -> {
                                CsvImporter.parse(s)
                            }
                            fileName.endsWith(".json") -> {
                                JsonImporter.parse(s)
                            }
                            else -> {
                                // Attempt auto-detection: try MAL XML first, then JSON, then CSV
                                try {
                                    MalXmlImporter.parse(s)
                                } catch (_: Exception) {
                                    JsonImporter.parse(s)
                                }
                            }
                        }
                    }
                }

                if (entries.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            statusMessage = "No valid media entries found in file",
                            isErrorMessage = true
                        )
                    }
                    return@launch
                }

                val (inserted, updated) = withContext(Dispatchers.IO) {
                    repository.importEntries(entries, _uiState.value.selectedConflictStrategy)
                }

                _uiState.update {
                    it.copy(
                        isImporting = false,
                        statusMessage = "Successfully imported ${entries.size} entries ($inserted added, $updated updated)",
                        isErrorMessage = false
                    )
                }
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

    fun exportToJson(context: Context, destinationUri: Uri) {
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
