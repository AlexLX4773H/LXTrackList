package com.lx.tracklist.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val mediaType: MediaType? = null,
    val isWatchType: Boolean = false,
    val trackingUnit: TrackingUnit = TrackingUnit.CHAPTERS,
    val colorHex: String = "#A7EF60",
    val createdAt: Long = System.currentTimeMillis()
)

