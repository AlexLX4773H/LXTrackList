package com.lx.tracklist.data.remote

import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncRouterTest {

    @Test
    fun `jobFor routes every rule deterministically`() {
        val tachiyomi = MediaEntry(
            title = "Solo Leveling",
            mediaType = MediaType.MANHWA,
            status = MediaStatus.WATCHING,
            externalSource = "Tachiyomi",
            externalId = "/manga/solo-leveling"
        )
        assertEquals(SyncJob.RESOLVE_IDS, SyncRouter.jobFor(tachiyomi))

        val animeDefault = MediaEntry(title = "Frieren", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING)
        val animeTmdb = MediaEntry(
            title = "Castlevania",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            externalSource = "TMDB",
            externalId = "71024"
        )
        assertEquals(SyncJob.JIKAN, SyncRouter.jobFor(animeDefault))
        assertEquals(SyncJob.TMDB, SyncRouter.jobFor(animeTmdb))

        val manga = MediaEntry(title = "Berserk", mediaType = MediaType.MANGA, status = MediaStatus.WATCHING)
        val manhwa = MediaEntry(title = "Tower of God", mediaType = MediaType.MANHWA, status = MediaStatus.WATCHING)
        assertEquals(SyncJob.JIKAN, SyncRouter.jobFor(manga))
        assertEquals(SyncJob.JIKAN, SyncRouter.jobFor(manhwa))

        val book = MediaEntry(title = "Dune", mediaType = MediaType.BOOK, status = MediaStatus.PLAN_TO_WATCH)
        val lightNovel = MediaEntry(
            title = "86",
            mediaType = MediaType.BOOK,
            status = MediaStatus.WATCHING,
            subCategory = "Light Novel",
            externalSource = "AniList",
            externalId = "98610"
        )
        assertEquals(SyncJob.BOOKS, SyncRouter.jobFor(book))
        assertEquals(SyncJob.BOOKS, SyncRouter.jobFor(lightNovel))

        val movieDefault = MediaEntry(title = "Inception", mediaType = MediaType.MOVIE, status = MediaStatus.COMPLETED)
        val movieMal = MediaEntry(
            title = "Your Name",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.COMPLETED,
            externalSource = "MyAnimeList",
            externalId = "32281"
        )
        val showDefault = MediaEntry(title = "Severance", mediaType = MediaType.SHOW, status = MediaStatus.WATCHING)
        val showAniList = MediaEntry(
            title = "Arcane",
            mediaType = MediaType.SHOW,
            status = MediaStatus.COMPLETED,
            externalSource = "AniList",
            externalId = "12345"
        )
        assertEquals(SyncJob.TMDB, SyncRouter.jobFor(movieDefault))
        assertEquals(SyncJob.JIKAN, SyncRouter.jobFor(movieMal))
        assertEquals(SyncJob.TMDB, SyncRouter.jobFor(showDefault))
        assertEquals(SyncJob.JIKAN, SyncRouter.jobFor(showAniList))

        val game = MediaEntry(title = "Hades", mediaType = MediaType.GAME, status = MediaStatus.WATCHING)
        val music = MediaEntry(title = "Papercut", mediaType = MediaType.MUSIC, status = MediaStatus.COMPLETED)
        assertEquals(SyncJob.GAMES, SyncRouter.jobFor(game))
        assertEquals(SyncJob.MUSIC, SyncRouter.jobFor(music))
    }

    @Test
    fun `mixed library assigns each entry to strictly one job manager and never books to jikan`() {
        val mixed = listOf(
            MediaEntry(id = 1L, title = "Solo Leveling", mediaType = MediaType.MANHWA, status = MediaStatus.WATCHING, externalSource = "Tachiyomi", externalId = "/manga/solo"),
            MediaEntry(id = 2L, title = "Frieren", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING),
            MediaEntry(id = 3L, title = "Castlevania", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, externalSource = "TMDB", externalId = "71024"),
            MediaEntry(id = 4L, title = "Berserk", mediaType = MediaType.MANGA, status = MediaStatus.WATCHING, externalSource = "MAL", externalId = "2"),
            MediaEntry(id = 5L, title = "Tower of God", mediaType = MediaType.MANHWA, status = MediaStatus.WATCHING),
            MediaEntry(id = 6L, title = "Dune", mediaType = MediaType.BOOK, status = MediaStatus.PLAN_TO_WATCH),
            MediaEntry(id = 7L, title = "86 Light Novel", mediaType = MediaType.BOOK, status = MediaStatus.WATCHING, subCategory = "Light Novel", externalSource = "AniList", externalId = "98610"),
            MediaEntry(id = 8L, title = "Overlord LN", mediaType = MediaType.BOOK, status = MediaStatus.WATCHING, subCategory = "Light Novel", externalSource = "MAL", externalId = "81669"),
            MediaEntry(id = 9L, title = "Inception", mediaType = MediaType.MOVIE, status = MediaStatus.COMPLETED),
            MediaEntry(id = 10L, title = "Your Name", mediaType = MediaType.MOVIE, status = MediaStatus.COMPLETED, externalSource = "MyAnimeList", externalId = "32281"),
            MediaEntry(id = 11L, title = "Severance", mediaType = MediaType.SHOW, status = MediaStatus.WATCHING),
            MediaEntry(id = 12L, title = "Arcane", mediaType = MediaType.SHOW, status = MediaStatus.COMPLETED, externalSource = "AniList", externalId = "999999"),
            MediaEntry(id = 13L, title = "Hades", mediaType = MediaType.GAME, status = MediaStatus.WATCHING),
            MediaEntry(id = 14L, title = "Papercut", mediaType = MediaType.MUSIC, status = MediaStatus.COMPLETED)
        )

        val predicates = listOf<(MediaEntry) -> Boolean>(
            JikanSyncManager::isMalSource,
            TmdbSyncManager::isTmdbSource,
            BookSyncManager::isBookSource,
            GameSyncManager::isGameSource,
            MusicSyncManager::isMusicSource,
            JikanSyncManager::isTachiyomiSource
        )

        mixed.forEach { entry ->
            val owners = predicates.count { check -> check(entry) }
            assertEquals("Expected strictly 1 owner for ${entry.title} (${entry.mediaType}, src=${entry.externalSource})", 1, owners)
            if (entry.mediaType == MediaType.BOOK) {
                org.junit.Assert.assertFalse("No Book should be processed by JikanSyncManager (${entry.title})", JikanSyncManager.isMalSource(entry))
            }
        }
        // TMDB-linked anime routes to TMDB, not Jikan
        org.junit.Assert.assertTrue(TmdbSyncManager.isTmdbSource(mixed[2]))
        org.junit.Assert.assertFalse(JikanSyncManager.isMalSource(mixed[2]))
    }
}
