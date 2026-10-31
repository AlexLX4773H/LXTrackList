package com.lx.tracklist.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.Priority
import com.lx.tracklist.data.model.SubCategory
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.repository.MediaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class AddEditUiState(
    val id: Long = 0L,
    val title: String = "",
    val alternateTitles: List<String> = emptyList(),
    val alternateTitle: String = "",
    val mediaType: MediaType = MediaType.ANIME,
    val customCategory: String? = null,
    val subCategory: String? = null,
    val status: MediaStatus = MediaStatus.WATCHING,
    val score: Int? = null,
    val priority: Priority = Priority.MEDIUM,
    val coverImageUri: String = "",
    val isFavorite: Boolean = false,
    val rewatchCount: Int = 0,
    val notes: String = "",
    val tags: String = "",
    val startDate: Long? = null,
    val finishDate: Long? = null,
    val airedStartDate: Long? = null,
    val airedEndDate: Long? = null,

    // Watch fields
    val currentEpisodes: Int = 0,
    val totalEpisodes: Int = 0,
    val airedEpisodes: Int = 0,
    val seasonNumber: Int = 1,
    val currentSeasons: Int = 1,
    val totalSeasons: Int = 1,
    val episodeLengthMinutes: Int? = null,
    val isAiring: Boolean = false,

    // Read fields
    val trackingUnit: TrackingUnit = TrackingUnit.CHAPTERS,
    val currentVolumes: Int = 0,
    val totalVolumes: Int = 0,
    val currentChapters: Int = 0,
    val totalChapters: Int = 0,
    val airedChapters: Int = 0,
    val currentPages: Int = 0,
    val totalPages: Int = 0,

    // Movie fields
    val movieSeriesName: String = "",
    val movieSeriesNumber: Int? = null,
    val durationMinutes: Int? = null,

    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class AddEditViewModel(
    private val repository: MediaRepository,
    private val entryId: Long?,
    private val initialMediaType: MediaType? = null,
    private val initialCustomCategory: String? = null,
    private val initialSubCategory: String? = null,
    private val initialStatus: MediaStatus? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        run {
            val status = initialStatus ?: MediaStatus.WATCHING
            AddEditUiState(
                mediaType = initialMediaType ?: MediaType.ANIME,
                customCategory = initialCustomCategory,
                subCategory = initialSubCategory,
                status = status,
                startDate = if (status == MediaStatus.WATCHING) todayStartTimestamp() else null
            )
        }
    )
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private val _savedEvent = Channel<Unit>(Channel.BUFFERED)
    val savedEvent = _savedEvent.receiveAsFlow()

    val customCategories = repository.customCategories

    val currentCategoryKey: String
        get() {
            val s = _uiState.value
            return s.customCategory ?: s.mediaType.name
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val subCategories: Flow<List<SubCategory>> = _uiState
        .map { it.customCategory ?: it.mediaType.name }
        .distinctUntilChanged()
        .flatMapLatest { key -> repository.getSubCategories(key) }

    init {
        loadEntry(entryId)
    }

    fun loadEntry(id: Long?) {
        if (id != null && id > 0) {
            viewModelScope.launch {
                val entry = repository.getEntryByIdSync(id)
                if (entry != null) {
                    val altTitles = entry.getAlternateTitles()
                    val effectiveStartDate = if (entry.status == MediaStatus.WATCHING && entry.startDate == null) {
                        todayStartTimestamp()
                    } else {
                        entry.startDate
                    }
                    _uiState.update {
                        it.copy(
                            id = entry.id,
                            title = entry.title,
                            alternateTitles = altTitles,
                            alternateTitle = entry.alternateTitle ?: "",
                            mediaType = entry.mediaType,
                            customCategory = entry.customCategory,
                            subCategory = entry.subCategory,
                            status = entry.status,
                            score = entry.score,
                            priority = entry.priority,
                            coverImageUri = entry.coverImageUri ?: "",
                            isFavorite = entry.isFavorite,
                            rewatchCount = entry.rewatchCount,
                            notes = entry.notes ?: "",
                            tags = entry.tags ?: "",
                            startDate = effectiveStartDate,
                            finishDate = entry.finishDate,
                            airedStartDate = entry.airedStartDate,
                            airedEndDate = entry.airedEndDate,
                            currentEpisodes = entry.currentEpisodes,
                            totalEpisodes = entry.totalEpisodes,
                            airedEpisodes = entry.airedEpisodes,
                            seasonNumber = entry.seasonNumber,
                            currentSeasons = entry.currentSeasons,
                            totalSeasons = entry.totalSeasons,
                            episodeLengthMinutes = entry.episodeLengthMinutes,
                            isAiring = entry.isAiring,
                            trackingUnit = entry.trackingUnit,
                            currentVolumes = entry.currentVolumes,
                            totalVolumes = entry.totalVolumes,
                            currentChapters = entry.currentChapters,
                            totalChapters = entry.totalChapters,
                            airedChapters = entry.airedChapters,
                            currentPages = entry.currentPages,
                            totalPages = entry.totalPages,
                            movieSeriesName = entry.movieSeriesName ?: "",
                            movieSeriesNumber = entry.movieSeriesNumber,
                            durationMinutes = entry.durationMinutes,
                            isSaved = false,
                            errorMessage = null
                        )
                    }
                }
            }
        } else {
            val status = initialStatus ?: MediaStatus.WATCHING
            _uiState.value = AddEditUiState(
                mediaType = initialMediaType ?: MediaType.ANIME,
                customCategory = initialCustomCategory,
                subCategory = initialSubCategory,
                status = status,
                startDate = if (status == MediaStatus.WATCHING) todayStartTimestamp() else null,
                isSaved = false,
                errorMessage = null
            )
        }
    }

    fun resetSavedState() {
        _uiState.update { it.copy(isSaved = false) }
    }

    fun updateTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun updateAlternateTitle(value: String) {
        val split = value.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        _uiState.update { it.copy(alternateTitle = value, alternateTitles = split) }
    }
    fun addAlternateTitle(value: String = "") {
        _uiState.update { it.copy(alternateTitles = it.alternateTitles + value) }
    }
    fun updateAlternateTitle(index: Int, value: String) {
        _uiState.update { state ->
            val list = state.alternateTitles.toMutableList()
            if (index in list.indices) {
                list[index] = value
                state.copy(alternateTitles = list, alternateTitle = list.filter { it.isNotBlank() }.joinToString("\n"))
            } else {
                state
            }
        }
    }
    fun removeAlternateTitle(index: Int) {
        _uiState.update { state ->
            val list = state.alternateTitles.toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
                state.copy(alternateTitles = list, alternateTitle = list.filter { it.isNotBlank() }.joinToString("\n"))
            } else {
                state
            }
        }
    }
    fun updateMediaType(value: MediaType) = _uiState.update { current ->
        val effectiveStartDate = if (current.status == MediaStatus.WATCHING && current.startDate == null) {
            todayStartTimestamp()
        } else {
            current.startDate
        }
        current.copy(
            mediaType = value,
            customCategory = null,
            subCategory = null,
            startDate = effectiveStartDate
        )
    }
    fun updateCustomCategory(name: String?) = _uiState.update { it.copy(customCategory = name, subCategory = null) }
    fun selectCategory(mediaType: MediaType, customCategory: String? = null) = _uiState.update { current ->
        val effectiveStartDate = if (current.status == MediaStatus.WATCHING && current.startDate == null) {
            todayStartTimestamp()
        } else {
            current.startDate
        }
        current.copy(
            mediaType = mediaType,
            customCategory = customCategory,
            subCategory = null,
            startDate = effectiveStartDate
        )
    }

    fun updateSubCategory(name: String?) = _uiState.update { it.copy(subCategory = name?.trim()?.ifBlank { null }) }

    fun addSubCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val key = currentCategoryKey
        viewModelScope.launch {
            repository.addSubCategory(key, trimmed)
            updateSubCategory(trimmed)
        }
    }
    fun updateStatus(value: MediaStatus) = _uiState.update { current ->
        if (value == MediaStatus.WATCHING && current.startDate == null) {
            current.copy(status = value, startDate = todayStartTimestamp())
        } else {
            current.copy(status = value)
        }
    }
    fun updateScore(value: Int?) = _uiState.update { it.copy(score = value) }
    fun updatePriority(value: Priority) = _uiState.update { it.copy(priority = value) }
    fun updateCoverUri(value: String) = _uiState.update { it.copy(coverImageUri = value) }
    fun updateIsFavorite(value: Boolean) = _uiState.update { it.copy(isFavorite = value) }
    fun updateNotes(value: String) = _uiState.update { it.copy(notes = value) }
    fun updateTags(value: String) = _uiState.update { it.copy(tags = value) }
    fun updateStartDate(value: Long?) = _uiState.update { it.copy(startDate = value) }
    fun updateFinishDate(value: Long?) = _uiState.update { it.copy(finishDate = value) }
    fun updateAiredStartDate(value: Long?) = _uiState.update { it.copy(airedStartDate = value) }
    fun updateAiredEndDate(value: Long?) = _uiState.update { it.copy(airedEndDate = value) }

    fun updateCurrentEpisodes(value: Int) = _uiState.update { it.copy(currentEpisodes = value) }
    fun updateTotalEpisodes(value: Int) = _uiState.update { it.copy(totalEpisodes = value) }
    fun updateAiredEpisodes(value: Int) = _uiState.update { it.copy(airedEpisodes = value) }
    fun updateCurrentSeasons(value: Int) = _uiState.update { it.copy(currentSeasons = value) }
    fun updateTotalSeasons(value: Int) = _uiState.update { it.copy(totalSeasons = value) }
    fun updateIsAiring(value: Boolean) = _uiState.update { it.copy(isAiring = value) }

    fun updateTrackingUnit(value: TrackingUnit) = _uiState.update { it.copy(trackingUnit = value) }
    fun updateCurrentVolumes(value: Int) = _uiState.update { it.copy(currentVolumes = value) }
    fun updateTotalVolumes(value: Int) = _uiState.update { it.copy(totalVolumes = value) }
    fun updateCurrentChapters(value: Int) = _uiState.update { it.copy(currentChapters = value) }
    fun updateTotalChapters(value: Int) = _uiState.update { it.copy(totalChapters = value) }
    fun updateAiredChapters(value: Int) = _uiState.update { it.copy(airedChapters = value) }
    fun updateCurrentPages(value: Int) = _uiState.update { it.copy(currentPages = value) }
    fun updateTotalPages(value: Int) = _uiState.update { it.copy(totalPages = value) }

    fun updateMovieSeriesName(value: String) = _uiState.update { it.copy(movieSeriesName = value) }
    fun updateMovieSeriesNumber(value: Int?) = _uiState.update { it.copy(movieSeriesNumber = value) }
    fun updateDurationMinutes(value: Int?) = _uiState.update { it.copy(durationMinutes = value) }

    fun saveEntry() {
        val s = _uiState.value
        if (s.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title cannot be empty") }
            return
        }

        viewModelScope.launch {
            val formattedAltTitle = MediaEntry.formatAlternateTitles(
                if (s.alternateTitles.isNotEmpty()) s.alternateTitles
                else if (s.alternateTitle.isNotBlank()) listOf(s.alternateTitle)
                else emptyList()
            )
            val effectiveStartDate = if (s.status == MediaStatus.WATCHING && s.startDate == null) {
                todayStartTimestamp()
            } else {
                s.startDate
            }
            val entry = MediaEntry(
                id = s.id,
                title = s.title.trim(),
                alternateTitle = formattedAltTitle,
                mediaType = s.mediaType,
                customCategory = s.customCategory?.trim()?.ifBlank { null },
                subCategory = s.subCategory?.trim()?.ifBlank { null },
                status = s.status,
                score = s.score,
                priority = s.priority,
                coverImageUri = s.coverImageUri.trim().ifBlank { null },
                isFavorite = s.isFavorite,
                rewatchCount = s.rewatchCount,
                notes = s.notes.trim().ifBlank { null },
                tags = s.tags.trim().ifBlank { null },
                startDate = effectiveStartDate,
                finishDate = s.finishDate,
                airedStartDate = s.airedStartDate,
                airedEndDate = s.airedEndDate,
                currentEpisodes = s.currentEpisodes,
                totalEpisodes = s.totalEpisodes,
                airedEpisodes = s.airedEpisodes,
                seasonNumber = s.seasonNumber,
                currentSeasons = s.currentSeasons,
                totalSeasons = s.totalSeasons,
                episodeLengthMinutes = s.episodeLengthMinutes,
                isAiring = s.isAiring,
                trackingUnit = s.trackingUnit,
                currentVolumes = s.currentVolumes,
                totalVolumes = s.totalVolumes,
                currentChapters = s.currentChapters,
                totalChapters = s.totalChapters,
                airedChapters = s.airedChapters,
                currentPages = s.currentPages,
                totalPages = s.totalPages,
                movieSeriesName = s.movieSeriesName.trim().ifBlank { null },
                movieSeriesNumber = s.movieSeriesNumber,
                durationMinutes = s.durationMinutes,
                updatedAt = System.currentTimeMillis()
            )

            if (entry.id == 0L) {
                repository.insert(entry)
            } else {
                repository.update(entry)
            }

            _uiState.update { it.copy(isSaved = true, startDate = effectiveStartDate) }
            _savedEvent.send(Unit)
        }
    }

    companion object {
        fun todayStartTimestamp(): Long {
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }
}

class AddEditViewModelFactory(
    private val repository: MediaRepository,
    private val entryId: Long?,
    private val initialMediaType: MediaType? = null,
    private val initialCustomCategory: String? = null,
    private val initialSubCategory: String? = null,
    private val initialStatus: MediaStatus? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEditViewModel(
                repository = repository,
                entryId = entryId,
                initialMediaType = initialMediaType,
                initialCustomCategory = initialCustomCategory,
                initialSubCategory = initialSubCategory,
                initialStatus = initialStatus
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
