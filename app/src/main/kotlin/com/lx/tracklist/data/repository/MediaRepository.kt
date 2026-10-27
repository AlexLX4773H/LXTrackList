package com.lx.tracklist.data.repository

import com.lx.tracklist.data.local.MediaEntryDao
import com.lx.tracklist.data.local.StatusCount
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

enum class ConflictStrategy {
    SKIP,
    OVERWRITE,
    MERGE
}

class MediaRepository(private val dao: MediaEntryDao) {

    fun getAllEntries(): Flow<List<MediaEntry>> = dao.getAllEntries()

    fun getEntriesByCategory(mediaType: MediaType): Flow<List<MediaEntry>> =
        dao.getEntriesByCategory(mediaType)

    fun getEntriesByCategoryAndStatus(mediaType: MediaType, status: MediaStatus): Flow<List<MediaEntry>> =
        dao.getEntriesByCategoryAndStatus(mediaType, status)

    fun getFavorites(): Flow<List<MediaEntry>> = dao.getFavorites()

    fun getEntryById(id: Long): Flow<MediaEntry?> = dao.getEntryById(id)

    suspend fun getEntryByIdSync(id: Long): MediaEntry? = dao.getEntryByIdSync(id)

    fun searchEntries(query: String): Flow<List<MediaEntry>> = dao.searchEntries(query)

    fun getStatusCountsByCategory(mediaType: MediaType): Flow<List<StatusCount>> =
        dao.getStatusCountsByCategory(mediaType)

    fun getTotalCountByCategory(mediaType: MediaType): Flow<Int> =
        dao.getTotalCountByCategory(mediaType)

    fun getTotalCount(): Flow<Int> = dao.getTotalCount()

    suspend fun insert(entry: MediaEntry): Long = dao.insert(entry)

    suspend fun update(entry: MediaEntry) = dao.update(entry)

    suspend fun delete(entry: MediaEntry) = dao.delete(entry)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun toggleFavorite(entry: MediaEntry) {
        dao.update(entry.copy(isFavorite = !entry.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    suspend fun incrementProgress(entry: MediaEntry): MediaEntry {
        val updated = entry.incrementProgress()
        dao.update(updated)
        return updated
    }

    suspend fun importEntries(
        entries: List<MediaEntry>,
        strategy: ConflictStrategy
    ): Pair<Int, Int> { // Returns Pair(insertedCount, updatedCount)
        var inserted = 0
        var updated = 0

        for (entry in entries) {
            val existing = if (entry.externalId != null && entry.externalSource != null) {
                dao.findByExternalId(entry.externalId, entry.externalSource)
            } else if (entry.externalId != null) {
                dao.findByAnyExternalId(entry.externalId)
            } else {
                null
            }

            if (existing == null) {
                dao.insert(entry)
                inserted++
            } else {
                when (strategy) {
                    ConflictStrategy.SKIP -> {
                        // Do nothing
                    }
                    ConflictStrategy.OVERWRITE -> {
                        dao.update(entry.copy(id = existing.id, updatedAt = System.currentTimeMillis()))
                        updated++
                    }
                    ConflictStrategy.MERGE -> {
                        val merged = mergeEntries(existing, entry)
                        dao.update(merged)
                        updated++
                    }
                }
            }
        }

        return Pair(inserted, updated)
    }

    suspend fun getAllEntriesSync(): List<MediaEntry> = dao.getAllEntriesSync()

    private fun mergeEntries(existing: MediaEntry, imported: MediaEntry): MediaEntry {
        val maxEpisodes = max(existing.currentEpisodes, imported.currentEpisodes)
        val maxTotalEp = if (existing.totalEpisodes > 0) existing.totalEpisodes else imported.totalEpisodes
        val maxVol = max(existing.currentVolumes, imported.currentVolumes)
        val maxTotalVol = if (existing.totalVolumes > 0) existing.totalVolumes else imported.totalVolumes
        val maxCh = max(existing.currentChapters, imported.currentChapters)
        val maxTotalCh = if (existing.totalChapters > 0) existing.totalChapters else imported.totalChapters
        val maxPages = max(existing.currentPages, imported.currentPages)
        val maxTotalPages = if (existing.totalPages > 0) existing.totalPages else imported.totalPages

        // Merge tags
        val existingTags = existing.tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val importedTags = imported.tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val mergedTags = (existingTags + importedTags).distinct().joinToString(", ")

        return existing.copy(
            score = imported.score ?: existing.score,
            status = if (imported.status == MediaStatus.COMPLETED) MediaStatus.COMPLETED else existing.status,
            currentEpisodes = maxEpisodes,
            totalEpisodes = maxTotalEp,
            currentVolumes = maxVol,
            totalVolumes = maxTotalVol,
            currentChapters = maxCh,
            totalChapters = maxTotalCh,
            currentPages = maxPages,
            totalPages = maxTotalPages,
            tags = mergedTags.ifBlank { null },
            notes = if (!imported.notes.isNullOrBlank()) imported.notes else existing.notes,
            coverImageUri = imported.coverImageUri ?: existing.coverImageUri,
            updatedAt = System.currentTimeMillis()
        )
    }
}
