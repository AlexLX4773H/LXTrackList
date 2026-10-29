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

    @Query("SELECT * FROM media_entries WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllEntriesByUser(userId: Long): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE mediaType = :mediaType ORDER BY updatedAt DESC")
    fun getEntriesByCategory(mediaType: MediaType): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE userId = :userId AND mediaType = :mediaType ORDER BY updatedAt DESC")
    fun getEntriesByUserAndCategory(userId: Long, mediaType: MediaType): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE userId = :userId AND mediaType = :mediaType AND subCategory = :subCategory ORDER BY updatedAt DESC")
    fun getEntriesByUserCategoryAndSubCategory(userId: Long, mediaType: MediaType, subCategory: String): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE userId = :userId AND customCategory = :customCategory ORDER BY updatedAt DESC")
    fun getEntriesByUserAndCustomCategory(userId: Long, customCategory: String): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE mediaType = :mediaType AND status = :status ORDER BY updatedAt DESC")
    fun getEntriesByCategoryAndStatus(mediaType: MediaType, status: MediaStatus): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE userId = :userId AND mediaType = :mediaType AND status = :status ORDER BY updatedAt DESC")
    fun getEntriesByUserAndCategoryAndStatus(userId: Long, mediaType: MediaType, status: MediaStatus): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE userId = :userId AND mediaType = :mediaType AND subCategory = :subCategory AND status = :status ORDER BY updatedAt DESC")
    fun getEntriesByUserCategorySubCategoryAndStatus(userId: Long, mediaType: MediaType, subCategory: String, status: MediaStatus): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE userId = :userId AND customCategory = :customCategory AND status = :status ORDER BY updatedAt DESC")
    fun getEntriesByUserAndCustomCategoryAndStatus(userId: Long, customCategory: String, status: MediaStatus): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavorites(): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE userId = :userId AND isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoritesByUser(userId: Long): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE id = :id")
    fun getEntryById(id: Long): Flow<MediaEntry?>

    @Query("SELECT * FROM media_entries WHERE id = :id")
    suspend fun getEntryByIdSync(id: Long): MediaEntry?

    @Query("SELECT * FROM media_entries WHERE userId = :userId AND externalId = :externalId AND externalSource = :externalSource LIMIT 1")
    suspend fun findByExternalId(userId: Long, externalId: String, externalSource: String): MediaEntry?

    @Query("SELECT * FROM media_entries WHERE externalId = :externalId AND externalSource = :externalSource LIMIT 1")
    suspend fun findByExternalId(externalId: String, externalSource: String): MediaEntry?

    @Query("SELECT * FROM media_entries WHERE userId = :userId AND externalId = :externalId LIMIT 1")
    suspend fun findByAnyExternalId(userId: Long, externalId: String): MediaEntry?

    @Query("SELECT * FROM media_entries WHERE externalId = :externalId LIMIT 1")
    suspend fun findByAnyExternalId(externalId: String): MediaEntry?

    @Query("SELECT * FROM media_entries WHERE (title LIKE '%' || :query || '%' OR alternateTitle LIKE '%' || :query || '%') ORDER BY title ASC")
    fun searchEntries(query: String): Flow<List<MediaEntry>>

    @Query("SELECT * FROM media_entries WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR alternateTitle LIKE '%' || :query || '%') ORDER BY title ASC")
    fun searchEntriesByUser(userId: Long, query: String): Flow<List<MediaEntry>>

    @Query("SELECT COUNT(*) FROM media_entries WHERE mediaType = :mediaType AND status = :status")
    fun getCountByCategoryAndStatus(mediaType: MediaType, status: MediaStatus): Flow<Int>

    @Query("SELECT status, COUNT(*) as count FROM media_entries WHERE mediaType = :mediaType GROUP BY status")
    fun getStatusCountsByCategory(mediaType: MediaType): Flow<List<StatusCount>>

    @Query("SELECT status, COUNT(*) as count FROM media_entries WHERE userId = :userId AND mediaType = :mediaType GROUP BY status")
    fun getStatusCountsByUserAndCategory(userId: Long, mediaType: MediaType): Flow<List<StatusCount>>

    @Query("SELECT status, COUNT(*) as count FROM media_entries WHERE userId = :userId AND mediaType = :mediaType AND subCategory = :subCategory GROUP BY status")
    fun getStatusCountsByUserCategoryAndSubCategory(userId: Long, mediaType: MediaType, subCategory: String): Flow<List<StatusCount>>

    @Query("SELECT status, COUNT(*) as count FROM media_entries WHERE userId = :userId AND customCategory = :customCategory GROUP BY status")
    fun getStatusCountsByUserAndCustomCategory(userId: Long, customCategory: String): Flow<List<StatusCount>>

    @Query("SELECT COUNT(*) FROM media_entries WHERE mediaType = :mediaType")
    fun getTotalCountByCategory(mediaType: MediaType): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_entries WHERE userId = :userId AND mediaType = :mediaType")
    fun getTotalCountByUserAndCategory(userId: Long, mediaType: MediaType): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_entries WHERE userId = :userId AND mediaType = :mediaType AND subCategory = :subCategory")
    fun getTotalCountByUserCategoryAndSubCategory(userId: Long, mediaType: MediaType, subCategory: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_entries WHERE userId = :userId AND customCategory = :customCategory")
    fun getTotalCountByUserAndCustomCategory(userId: Long, customCategory: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_entries")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_entries WHERE userId = :userId")
    fun getTotalCountByUser(userId: Long): Flow<Int>

    @Query("SELECT * FROM media_entries")
    suspend fun getAllEntriesSync(): List<MediaEntry>

    @Query("SELECT * FROM media_entries WHERE userId = :userId")
    suspend fun getAllEntriesByUserSync(userId: Long): List<MediaEntry>

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

    @Query("DELETE FROM media_entries WHERE id IN (:ids)")
    suspend fun deleteEntriesByIds(ids: List<Long>)

    @Query("DELETE FROM media_entries WHERE userId = :userId")
    suspend fun deleteAllByUser(userId: Long)

    @Query("DELETE FROM media_entries")
    suspend fun deleteAll()
}
