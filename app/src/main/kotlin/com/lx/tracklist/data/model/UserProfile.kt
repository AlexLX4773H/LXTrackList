package com.lx.tracklist.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val avatarColorHex: String = "#A7EF60",
    val avatarImageUri: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val defaultMediaType: MediaType? = null,
    val defaultCustomCategory: String? = null,
    val defaultSubCategory: String? = null,
    val defaultStatus: MediaStatus? = null,
    val passKey: String? = null
)

