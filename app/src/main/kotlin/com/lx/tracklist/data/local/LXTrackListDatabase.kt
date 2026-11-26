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
    version = 22,
    exportSchema = true
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

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_entries ADD COLUMN userComments TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_entries ADD COLUMN broadcastTime TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE media_entries ADD COLUMN nextAiringEpoch INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_entries ADD COLUMN gamePlatforms TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE media_entries ADD COLUMN isGameFranchise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_entries ADD COLUMN gameSeriesEntriesJson TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE media_entries ADD COLUMN gameProgressPercent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_entries ADD COLUMN customUnitLabel TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE media_entries ADD COLUMN customUnitCurrent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_entries ADD COLUMN customUnitTotal INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_entries ADD COLUMN originalLanguage TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate media_entries with customUnitCurrent and customUnitTotal as REAL
                db.execSQL(
                    """
                    CREATE TABLE media_entries_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        customCategory TEXT,
                        subCategory TEXT,
                        title TEXT NOT NULL,
                        alternateTitle TEXT,
                        mediaType TEXT NOT NULL,
                        status TEXT NOT NULL,
                        score INTEGER,
                        externalScore REAL,
                        externalScoreSource TEXT,
                        startDate INTEGER,
                        finishDate INTEGER,
                        airedStartDate INTEGER,
                        airedEndDate INTEGER,
                        notes TEXT,
                        tags TEXT,
                        coverImageUri TEXT,
                        priority TEXT NOT NULL,
                        rewatchCount INTEGER NOT NULL,
                        isRewatching INTEGER NOT NULL,
                        isFavorite INTEGER NOT NULL,
                        isExternallyTracked INTEGER NOT NULL,
                        externalId TEXT,
                        externalSource TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        currentEpisodes INTEGER NOT NULL,
                        totalEpisodes INTEGER NOT NULL,
                        airedEpisodes INTEGER NOT NULL,
                        seasonNumber INTEGER NOT NULL,
                        currentSeasons INTEGER NOT NULL,
                        totalSeasons INTEGER NOT NULL,
                        seasonEpisodesJson TEXT,
                        episodeLengthMinutes INTEGER,
                        currentTimeMinutes INTEGER,
                        isAiring INTEGER NOT NULL,
                        broadcastTime TEXT,
                        nextAiringEpoch INTEGER,
                        currentVolumes INTEGER NOT NULL,
                        totalVolumes INTEGER NOT NULL,
                        currentChapters INTEGER NOT NULL,
                        totalChapters INTEGER NOT NULL,
                        airedChapters INTEGER NOT NULL,
                        currentPages INTEGER NOT NULL,
                        totalPages INTEGER NOT NULL,
                        trackingUnit TEXT NOT NULL,
                        movieSeriesNumber INTEGER,
                        movieSeriesName TEXT,
                        durationMinutes INTEGER,
                        relatedEntriesJson TEXT,
                        gamePlatforms TEXT,
                        isGameFranchise INTEGER NOT NULL,
                        gameSeriesEntriesJson TEXT,
                        gameProgressPercent INTEGER NOT NULL,
                        customUnitLabel TEXT,
                        customUnitCurrent REAL NOT NULL DEFAULT 0.0,
                        customUnitTotal REAL NOT NULL DEFAULT 0.0,
                        userComments TEXT,
                        originalLanguage TEXT
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO media_entries_new (
                        id, userId, customCategory, subCategory, title, alternateTitle, mediaType, status, score,
                        externalScore, externalScoreSource, startDate, finishDate, airedStartDate, airedEndDate,
                        notes, tags, coverImageUri, priority, rewatchCount, isRewatching, isFavorite,
                        isExternallyTracked, externalId, externalSource, createdAt, updatedAt, currentEpisodes,
                        totalEpisodes, airedEpisodes, seasonNumber, currentSeasons, totalSeasons,
                        seasonEpisodesJson, episodeLengthMinutes, currentTimeMinutes, isAiring, broadcastTime,
                        nextAiringEpoch, currentVolumes, totalVolumes, currentChapters, totalChapters,
                        airedChapters, currentPages, totalPages, trackingUnit, movieSeriesNumber,
                        movieSeriesName, durationMinutes, relatedEntriesJson, gamePlatforms, isGameFranchise,
                        gameSeriesEntriesJson, gameProgressPercent, customUnitLabel, customUnitCurrent,
                        customUnitTotal, userComments, originalLanguage
                    )
                    SELECT
                        id, userId, customCategory, subCategory, title, alternateTitle, mediaType, status, score,
                        externalScore, externalScoreSource, startDate, finishDate, airedStartDate, airedEndDate,
                        notes, tags, coverImageUri, priority, rewatchCount, isRewatching, isFavorite,
                        isExternallyTracked, externalId, externalSource, createdAt, updatedAt, currentEpisodes,
                        totalEpisodes, airedEpisodes, seasonNumber, currentSeasons, totalSeasons,
                        seasonEpisodesJson, episodeLengthMinutes, currentTimeMinutes, isAiring, broadcastTime,
                        nextAiringEpoch, currentVolumes, totalVolumes, currentChapters, totalChapters,
                        airedChapters, currentPages, totalPages, trackingUnit, movieSeriesNumber,
                        movieSeriesName, durationMinutes, relatedEntriesJson, gamePlatforms, isGameFranchise,
                        gameSeriesEntriesJson, gameProgressPercent, customUnitLabel, CAST(customUnitCurrent AS REAL),
                        CAST(customUnitTotal AS REAL), userComments, originalLanguage
                    FROM media_entries
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE media_entries")
                db.execSQL("ALTER TABLE media_entries_new RENAME TO media_entries")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_entries_mediaType ON media_entries(mediaType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_entries_status ON media_entries(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_entries_isFavorite ON media_entries(isFavorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_entries_isExternallyTracked ON media_entries(isExternallyTracked)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_entries_title ON media_entries(title)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_entries_userId ON media_entries(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_entries_customCategory ON media_entries(customCategory)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_entries_subCategory ON media_entries(subCategory)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_entries_externalId_externalSource ON media_entries(externalId, externalSource)")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_entries ADD COLUMN creator TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE media_entries ADD COLUMN musicAlbumId TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_entries_creator ON media_entries(creator)")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_entries ADD COLUMN lastMetadataSyncAt INTEGER DEFAULT NULL")

                // Normalize externalSource and externalScoreSource to canonical names (FND-06 / Decision D10)
                val sourceColumns = listOf("externalSource", "externalScoreSource")
                for (col in sourceColumns) {
                    db.execSQL("UPDATE media_entries SET $col = 'MAL'          WHERE UPPER(TRIM($col)) IN ('MAL','MYANIMELIST','JIKAN','MAL (ANILIST)') AND $col <> 'MAL'")
                    db.execSQL("UPDATE media_entries SET $col = 'AniList'      WHERE UPPER(TRIM($col)) = 'ANILIST' AND $col <> 'AniList'")
                    db.execSQL("UPDATE media_entries SET $col = 'Kitsu'        WHERE UPPER(TRIM($col)) = 'KITSU' AND $col <> 'Kitsu'")
                    db.execSQL("UPDATE media_entries SET $col = 'MangaDex'     WHERE UPPER(TRIM($col)) = 'MANGADEX' AND $col <> 'MangaDex'")
                    db.execSQL("UPDATE media_entries SET $col = 'TMDB'         WHERE UPPER(TRIM($col)) IN ('TMDB','THEMOVIEDB','THE MOVIE DATABASE') AND $col <> 'TMDB'")
                    db.execSQL("UPDATE media_entries SET $col = 'Google Books' WHERE UPPER(TRIM($col)) IN ('GOOGLE BOOKS','GOOGLEBOOKS','GOOGLE_BOOKS') AND $col <> 'Google Books'")
                    db.execSQL("UPDATE media_entries SET $col = 'Open Library' WHERE UPPER(TRIM($col)) IN ('OPEN LIBRARY','OPENLIBRARY','OPEN_LIBRARY') AND $col <> 'Open Library'")
                    db.execSQL("UPDATE media_entries SET $col = 'IGDB'         WHERE UPPER(TRIM($col)) = 'IGDB' AND $col <> 'IGDB'")
                    db.execSQL("UPDATE media_entries SET $col = 'RAWG'         WHERE UPPER(TRIM($col)) = 'RAWG' AND $col <> 'RAWG'")
                    db.execSQL("UPDATE media_entries SET $col = 'Apple Music'  WHERE UPPER(TRIM($col)) IN ('ITUNES','APPLE MUSIC','APPLE_MUSIC') AND $col <> 'Apple Music'")
                    db.execSQL("UPDATE media_entries SET $col = 'MusicBrainz'  WHERE UPPER(TRIM($col)) = 'MUSICBRAINZ' AND $col <> 'MusicBrainz'")
                    db.execSQL("UPDATE media_entries SET $col = 'Tachiyomi'    WHERE UPPER(TRIM($col)) IN ('TACHIYOMI','MIHON') AND $col <> 'Tachiyomi'")
                    db.execSQL("UPDATE media_entries SET $col = NULL           WHERE $col IS NOT NULL AND TRIM($col) = ''")
                }

                db.execSQL("UPDATE media_entries SET externalSource = NULL WHERE UPPER(TRIM(externalSource)) = 'CSV' AND (externalId IS NULL OR TRIM(externalId) = '')")
                db.execSQL("UPDATE media_entries SET gamePlatforms = REPLACE(gamePlatforms, 'XBox', 'Xbox') WHERE gamePlatforms LIKE '%XBox%'")
            }
        }

        fun getDatabase(context: Context): LXTrackListDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LXTrackListDatabase::class.java,
                    "lx_track_list.db"
                )
                    .addMigrations(
                        MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                        MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                        MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14,
                        MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18,
                        MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
