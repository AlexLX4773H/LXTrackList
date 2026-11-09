package com.lx.tracklist.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sub_categories",
    indices = [
        Index(value = ["userId", "categoryKey", "name"], unique = true)
    ]
)
data class SubCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: Long = 0L,
    val categoryKey: String, // e.g. "MANGA", "MANHWA", "ANIME", "SHOW", "MOVIE", "BOOK", or a custom category name like "Donghua"
    val name: String,
    val colorHex: String = "#A7EF60",
    val createdAt: Long = System.currentTimeMillis()
)

