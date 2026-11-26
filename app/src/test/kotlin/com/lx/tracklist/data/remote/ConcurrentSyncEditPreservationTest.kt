package com.lx.tracklist.data.remote

import com.lx.tracklist.data.model.ExternalSources
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.Priority
import com.lx.tracklist.data.model.TrackingUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConcurrentSyncEditPreservationTest {

    @Test
    fun `jikan sync preserves concurrent user edits and non-blank language while updating metadata`() {
        val syncNow = 1_750_000_000_000L
        val latest = MediaEntry(
            id = 42L,
            userId = 7L,
            title = "Frieren",
            mediaType = MediaType.ANIME,
            status = MediaStatus.COMPLETED,
            score = 10,
            currentEpisodes = 28,
            totalEpisodes = 0,
            userComments = "Watched with friends - Masterpiece",
            startDate = 1_700_000_000_000L,
            finishDate = 1_710_000_000_000L,
            isFavorite = true,
            rewatchCount = 2,
            priority = Priority.HIGH,
            externalId = "52991",
            externalSource = ExternalSources.MAL,
            originalLanguage = "ja-custom"
        )
        val details = JikanMediaDetails(
            malId = 52991L,
            title = "Frieren",
            posterUrl = "https://cdn.example/frieren.jpg",
            totalEpisodes = 28,
            durationMinutes = 24,
            synopsis = "After the party defeated the Demon King...",
            tags = "Adventure, Drama, Fantasy",
            externalScore = 9.3f,
            externalScoreSource = "MAL",
            originalLanguage = "ja",
            authors = "Madhouse"
        )

        val (updated, hasChanges) = JikanSyncManager.applyJikanMetadataToFresh(
            latest = latest,
            details = details,
            forceRefresh = true,
            now = syncNow
        )

        assertTrue(hasChanges)
        // Metadata updated
        assertEquals("https://cdn.example/frieren.jpg", updated.coverImageUri)
        assertEquals(28, updated.totalEpisodes)
        assertEquals(24, updated.episodeLengthMinutes)
        assertEquals("After the party defeated the Demon King...", updated.description)
        assertEquals("Adventure, Drama, Fantasy", updated.tags)
        assertEquals(9.3f, updated.externalScore)
        assertEquals("Madhouse", updated.creator)
        assertEquals(syncNow, updated.lastMetadataSyncAt)

        // Concurrent user edits & non-blank language preserved
        assertEquals(MediaStatus.COMPLETED, updated.status)
        assertEquals(10, updated.score)
        assertEquals(28, updated.currentEpisodes)
        assertEquals("Watched with friends - Masterpiece", updated.userComments)
        assertEquals(1_700_000_000_000L, updated.startDate)
        assertEquals(1_710_000_000_000L, updated.finishDate)
        assertEquals(true, updated.isFavorite)
        assertEquals(2, updated.rewatchCount)
        assertEquals(Priority.HIGH, updated.priority)
        assertEquals("ja-custom", updated.originalLanguage)
    }

    @Test
    fun `jikan sync fills blank originalLanguage`() {
        val syncNow = 1_750_000_000_000L
        val latest = MediaEntry(
            id = 43L,
            title = "Bocchi the Rock",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            externalId = "47917",
            externalSource = ExternalSources.MAL,
            originalLanguage = null
        )
        val details = JikanMediaDetails(
            malId = 47917L,
            title = "Bocchi the Rock",
            originalLanguage = "ja"
        )

        val (updated, hasChanges) = JikanSyncManager.applyJikanMetadataToFresh(
            latest = latest,
            details = details,
            now = syncNow
        )

        assertTrue(hasChanges)
        assertEquals("ja", updated.originalLanguage)
        assertEquals(syncNow, updated.lastMetadataSyncAt)
    }

    @Test
    fun `tmdb sync preserves concurrent user edits while refreshing show metadata`() {
        val syncNow = 1_750_000_100_000L
        val latest = MediaEntry(
            id = 101L,
            title = "Severance",
            mediaType = MediaType.SHOW,
            status = MediaStatus.WATCHING,
            score = 9,
            currentEpisodes = 14,
            userComments = "Wild S2 finale - Keep watching weekly",
            isFavorite = true,
            rewatchCount = 1,
            priority = Priority.HIGH,
            externalId = "95396",
            externalSource = ExternalSources.TMDB,
            originalLanguage = "en-US"
        )
        val details = TmdbMediaDetails(
            tmdbId = 95396L,
            title = "Severance",
            posterUrl = "https://image.tmdb.org/severance.jpg",
            durationMinutes = 50,
            totalEpisodes = 19,
            totalSeasons = 2,
            seasonEpisodes = listOf(9, 10),
            tags = "Drama, Mystery, Sci-Fi",
            synopsis = "Lumon Industries.",
            externalScore = 8.4f,
            originalLanguage = "en",
            creators = listOf("Dan Erickson")
        )

        val (updated, hasChanges) = TmdbSyncManager.applyTmdbMetadataToFresh(
            latest = latest,
            details = details,
            forceRefresh = true,
            now = syncNow
        )

        assertTrue(hasChanges)
        assertEquals("https://image.tmdb.org/severance.jpg", updated.coverImageUri)
        assertEquals(50, updated.episodeLengthMinutes)
        assertEquals(19, updated.totalEpisodes)
        assertEquals(2, updated.totalSeasons)
        assertEquals(8.4f, updated.externalScore)
        assertEquals("Dan Erickson", updated.creator)
        assertEquals(syncNow, updated.lastMetadataSyncAt)

        // User edits & non-blank language preserved
        assertEquals(MediaStatus.WATCHING, updated.status)
        assertEquals(9, updated.score)
        assertEquals(14, updated.currentEpisodes)
        assertEquals("Wild S2 finale - Keep watching weekly", updated.userComments)
        assertEquals(true, updated.isFavorite)
        assertEquals(1, updated.rewatchCount)
        assertEquals(Priority.HIGH, updated.priority)
        assertEquals("en-US", updated.originalLanguage)
    }

    @Test
    fun `book sync preserves concurrent user edits trackingUnit and originalLanguage`() {
        val syncNow = 1_750_000_200_000L
        val latest = MediaEntry(
            id = 202L,
            title = "Mistborn",
            mediaType = MediaType.BOOK,
            status = MediaStatus.WATCHING,
            score = 8,
            currentPages = 240,
            trackingUnit = TrackingUnit.PAGES,
            userComments = "Great magic system - Bookmark p.240",
            isFavorite = true,
            priority = Priority.HIGH,
            externalId = "book-1",
            externalSource = ExternalSources.GOOGLE_BOOKS,
            originalLanguage = "en-GB"
        )
        val details = BookDetails(
            id = "book-1",
            sourceProvider = ExternalSources.GOOGLE_BOOKS,
            title = "Mistborn",
            authors = listOf("Brandon Sanderson"),
            coverUrl = "https://books.example/mistborn.jpg",
            pageCount = 672,
            averageScore = 8.9f,
            language = "en",
            description = "The Final Empire"
        )

        val (updated, hasChanges) = BookSyncManager.applyBookMetadataToFresh(
            latest = latest,
            bookDetails = details,
            forceRefresh = true,
            now = syncNow
        )

        assertTrue(hasChanges)
        assertEquals("https://books.example/mistborn.jpg", updated.coverImageUri)
        assertEquals(672, updated.totalPages)
        assertEquals(8.9f, updated.externalScore)
        assertEquals(syncNow, updated.lastMetadataSyncAt)

        // Concurrent user edits & trackingUnit & non-blank originalLanguage preserved
        assertEquals(MediaStatus.WATCHING, updated.status)
        assertEquals(8, updated.score)
        assertEquals(240, updated.currentPages)
        assertEquals(TrackingUnit.PAGES, updated.trackingUnit)
        assertEquals("Great magic system - Bookmark p.240", updated.userComments)
        assertEquals(true, updated.isFavorite)
        assertEquals(Priority.HIGH, updated.priority)
        assertEquals("en-GB", updated.originalLanguage)
    }

    @Test
    fun `game sync preserves concurrent user edits while updating metadata and lastMetadataSyncAt`() {
        val syncNow = 1_750_000_300_000L
        val latest = MediaEntry(
            id = 303L,
            title = "Hollow Knight",
            mediaType = MediaType.GAME,
            status = MediaStatus.COMPLETED,
            score = 10,
            userComments = "Radiant Hallownest - 112% run done",
            isFavorite = true,
            rewatchCount = 3,
            priority = Priority.HIGH,
            externalId = "igdb-14593",
            externalSource = ExternalSources.IGDB
        )
        val details = GameDetails(
            externalId = "rawg-9767",
            externalSource = ExternalSources.RAWG,
            title = "Hollow Knight",
            coverUrl = "https://games.example/hk.jpg",
            rating = 9.1f,
            ratingSource = "RAWG",
            genres = listOf("Metroidvania", "Indie"),
            platforms = listOf("PC", "Switch"),
            completionTimeMinutes = 1800,
            summary = "Explore Hallownest."
        )

        val updated = GameSyncManager.applySyncedGameDetails(
            entry = latest,
            details = details,
            coverUri = details.coverUrl,
            platformsStr = "PC, Switch",
            forceRefresh = true,
            now = syncNow
        )

        // IGDB ref preserved, metadata & timestamp updated
        assertEquals("igdb-14593", updated.externalId)
        assertEquals(ExternalSources.IGDB, updated.externalSource)
        assertEquals("https://games.example/hk.jpg", updated.coverImageUri)
        assertEquals("PC, Switch", updated.gamePlatforms)
        assertEquals(1800, updated.durationMinutes)
        assertEquals(9.1f, updated.externalScore)
        assertEquals(syncNow, updated.lastMetadataSyncAt)

        // User edits preserved
        assertEquals(MediaStatus.COMPLETED, updated.status)
        assertEquals(10, updated.score)
        assertEquals("Radiant Hallownest - 112% run done", updated.userComments)
        assertEquals(true, updated.isFavorite)
        assertEquals(3, updated.rewatchCount)
        assertEquals(Priority.HIGH, updated.priority)
    }
}
