package com.lx.tracklist.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lx.tracklist.data.model.CustomCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCategoryDao {

    @Query("SELECT * FROM custom_categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CustomCategory>>

    @Query("SELECT * FROM custom_categories ORDER BY name ASC")
    suspend fun getAllCategoriesSync(): List<CustomCategory>

    @Query("SELECT * FROM custom_categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CustomCategory?

    @Query("SELECT * FROM custom_categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CustomCategory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CustomCategory): Long

    @Update
    suspend fun update(category: CustomCategory)

    @Delete
    suspend fun delete(category: CustomCategory)

    @Query("DELETE FROM custom_categories WHERE name = :name")
    suspend fun deleteByName(name: String)
}

