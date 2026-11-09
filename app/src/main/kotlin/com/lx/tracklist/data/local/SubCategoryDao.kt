package com.lx.tracklist.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lx.tracklist.data.model.SubCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface SubCategoryDao {

    @Query("SELECT * FROM sub_categories WHERE userId = :userId AND categoryKey = :categoryKey ORDER BY name ASC")
    fun getSubCategoriesByCategory(userId: Long, categoryKey: String): Flow<List<SubCategory>>

    @Query("SELECT * FROM sub_categories WHERE userId = :userId ORDER BY name ASC")
    fun getAllSubCategories(userId: Long): Flow<List<SubCategory>>

    @Query("SELECT * FROM sub_categories WHERE userId = :userId AND categoryKey = :categoryKey AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getSubCategory(userId: Long, categoryKey: String, name: String): SubCategory?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(subCategory: SubCategory): Long

    @Delete
    suspend fun delete(subCategory: SubCategory)

    @Query("DELETE FROM sub_categories WHERE userId = :userId AND categoryKey = :categoryKey AND LOWER(name) = LOWER(:name)")
    suspend fun deleteByName(userId: Long, categoryKey: String, name: String)
}

