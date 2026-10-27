package com.lx.tracklist.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import kotlinx.coroutines.flow.Flow

data class StatusCount(
    val status: MediaStatus,
    val count: Int
)

@Dao
interface MediaEntryDao {

    @Query("SELECT * FROM media_entries ORDER BY updatedAt DESC")
    fun getAllEntries(): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE mediaType = :mediaType ORDER BY updatedAt DESC")
    fun getEntriesByCategory(mediaType: MediaType): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE mediaType = :mediaType AND status = :status ORDER BY updatedAt DESC")
    fun getEntriesByCategoryAndStatus(mediaType: MediaType, status: MediaStatus): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavorites(): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE id = :id")
    fun getEntryById(id: Long): Flow<MediaEntry?>

    @Query("SELECT * FROM media_entries WHERE id = :id")
    suspend fun getEntryByIdSync(id: Long): MediaEntry?

    @Query("SELECT * FROM media_entries WHERE externalId = :externalId AND externalSource = :externalSource LIMIT 1")
    suspend fun findByExternalId(externalId: String, externalSource: String): MediaEntry?

    @Query("SELECT * FROM media_entries WHERE externalId = :externalId LIMIT 1")
    suspend fun findByAnyExternalId(externalId: String): MediaEntry?

    @Query("SELECT * FROM media_entries WHERE title LIKE '%' || :query || '%' OR alternateTitle LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchEntries(query: String): Flow<List<MediaEntry>>

    @Query("SELECT COUNT(*) FROM media_entries WHERE mediaType = :mediaType AND status = :status")
    fun getCountByCategoryAndStatus(mediaType: MediaType, status: MediaStatus): Flow<Int>

    @Query("SELECT status, COUNT(*) as count FROM media_entries WHERE mediaType = :mediaType GROUP BY status")
    fun getStatusCountsByCategory(mediaType: MediaType): Flow<List<StatusCount>>

    @Query("SELECT COUNT(*) FROM media_entries WHERE mediaType = :mediaType")
    fun getTotalCountByCategory(mediaType: MediaType): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_entries")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT * FROM media_entries")
    suspend fun getAllEntriesSync(): List<MediaEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MediaEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<MediaEntry>): List<Long>

    @Update
    suspend fun update(entry: MediaEntry)

    @Delete
    suspend fun delete(entry: MediaEntry)

    @Query("DELETE FROM media_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM media_entries")
    suspend fun deleteAll()
}
