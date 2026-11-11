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
    version = 15,
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

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_entries ADD COLUMN isExternallyTracked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_entries_isExternallyTracked ON media_entries(isExternallyTracked)")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add userId column to sub_categories and custom_categories
                db.execSQL("ALTER TABLE sub_categories ADD COLUMN userId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE custom_categories ADD COLUMN userId INTEGER NOT NULL DEFAULT 1")

                // 2. Associate existing sub_categories with the userId that actually uses them in media_entries
                db.execSQL(
                    """
                    UPDATE sub_categories 
                    SET userId = COALESCE(
                        (SELECT me.userId FROM media_entries me WHERE me.subCategory = sub_categories.name LIMIT 1),
                        1
                    )
                    """.trimIndent()
                )

                // 3. Associate existing custom_categories with the userId that actually uses them in media_entries
                db.execSQL(
                    """
                    UPDATE custom_categories 
                    SET userId = COALESCE(
                        (SELECT me.userId FROM media_entries me WHERE me.customCategory = custom_categories.name LIMIT 1),
                        1
                    )
                    """.trimIndent()
                )

                // 4. Populate missing per-profile sub_categories from media_entries
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO sub_categories (userId, categoryKey, name, colorHex, createdAt)
                    SELECT DISTINCT me.userId, COALESCE(me.customCategory, me.mediaType), me.subCategory, '#A7EF60', strftime('%s','now')*1000
                    FROM media_entries me
                    WHERE me.subCategory IS NOT NULL AND trim(me.subCategory) != ''
                    """.trimIndent()
                )

                // 5. Recreate unique indices per userId
                db.execSQL("DROP INDEX IF EXISTS index_sub_categories_categoryKey_name")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sub_categories_userId_categoryKey_name ON sub_categories(userId, categoryKey, name)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_custom_categories_userId_name ON custom_categories(userId, name)")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_entries ADD COLUMN currentTimeMinutes INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_entries ADD COLUMN seasonEpisodesJson TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): LXTrackListDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LXTrackListDatabase::class.java,
                    "lx_track_list.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
