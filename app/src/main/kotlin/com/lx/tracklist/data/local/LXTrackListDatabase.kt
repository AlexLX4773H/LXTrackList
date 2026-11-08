package com.lx.tracklist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.SubCategory
import com.lx.tracklist.data.model.UserProfile

@Database(
    entities = [MediaEntry::class, UserProfile::class, CustomCategory::class, SubCategory::class],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LXTrackListDatabase : RoomDatabase() {

    abstract fun mediaEntryDao(): MediaEntryDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun customCategoryDao(): CustomCategoryDao
    abstract fun subCategoryDao(): SubCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: LXTrackListDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN avatarImageUri TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE custom_categories ADD COLUMN mediaType TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_entries ADD COLUMN subCategory TEXT DEFAULT NULL")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sub_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoryKey TEXT NOT NULL,
                        name TEXT NOT NULL,
                        colorHex TEXT NOT NULL DEFAULT '#A7EF60',
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sub_categories_categoryKey_name ON sub_categories(categoryKey, name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_entries_subCategory ON media_entries(subCategory)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_entries ADD COLUMN airedStartDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE media_entries ADD COLUMN airedEndDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE media_entries ADD COLUMN airedEpisodes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_entries ADD COLUMN airedChapters INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN defaultMediaType TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN defaultCustomCategory TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN defaultSubCategory TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN defaultStatus TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_entries ADD COLUMN isRewatching INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_entries ADD COLUMN externalScore REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE media_entries ADD COLUMN externalScoreSource TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_entries ADD COLUMN relatedEntriesJson TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profiles ADD COLUMN passKey TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): LXTrackListDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LXTrackListDatabase::class.java,
                    "lx_track_list.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
