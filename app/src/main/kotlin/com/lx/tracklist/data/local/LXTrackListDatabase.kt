package com.lx.tracklist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.UserProfile

@Database(
    entities = [MediaEntry::class, UserProfile::class, CustomCategory::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LXTrackListDatabase : RoomDatabase() {

    abstract fun mediaEntryDao(): MediaEntryDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun customCategoryDao(): CustomCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: LXTrackListDatabase? = null

        fun getDatabase(context: Context): LXTrackListDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LXTrackListDatabase::class.java,
                    "lx_track_list.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
