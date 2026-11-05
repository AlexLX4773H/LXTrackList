package com.lx.tracklist

import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.TrackingUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaEntryTest {

    @Test
    fun testAnimeProgressIncrement() {
        val entry = MediaEntry(
            title = "Test Anime",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            currentEpisodes = 5,
            totalEpisodes = 12
        )

        assertEquals("5/12 ep", entry.displayProgress)
        assertEquals(5f / 12f, entry.progressFraction, 0.001f)

        val updated = entry.incrementProgress()
        assertEquals(6, updated.currentEpisodes)
        assertEquals(MediaStatus.WATCHING, updated.status)

        // Complete when reaching total
        var current = updated
        repeat(6) { current = current.incrementProgress() }
        assertEquals(12, current.currentEpisodes)
        assertEquals(MediaStatus.COMPLETED, current.status)
    }

    @Test
    fun testMangaProgressCombined() {
        val entry = MediaEntry(
            title = "Test Manga",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            currentVolumes = 2,
            totalVolumes = 10,
            currentChapters = 15,
            totalChapters = 80,
            trackingUnit = TrackingUnit.COMBINED
        )

        assertEquals("Vol 2/10 • Ch 15/80", entry.displayProgress)
        assertEquals(15f / 80f, entry.progressFraction, 0.001f)

        val updated = entry.incrementProgress()
        assertEquals(16, updated.currentChapters)
    }

    @Test
    fun testMangaProgressVolumes() {
        val entry = MediaEntry(
            title = "Test Manga Vol",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            currentVolumes = 3,
            totalVolumes = 5,
            trackingUnit = TrackingUnit.VOLUMES
        )

        assertEquals("Vol 3/5", entry.displayProgress)
        val updated = entry.incrementProgress()
        assertEquals(4, updated.currentVolumes)
    }

    @Test
    fun testRewatchProgressCompletion() {
        val entry = MediaEntry(
            title = "Steins;Gate",
            mediaType = MediaType.ANIME,
            status = MediaStatus.COMPLETED,
            currentEpisodes = 23,
            totalEpisodes = 24,
            isRewatching = true,
            rewatchCount = 1
        )

        val updated = entry.incrementProgress()
        assertEquals(24, updated.currentEpisodes)
        assertEquals(MediaStatus.COMPLETED, updated.status)
        assertEquals(false, updated.isRewatching)
        assertEquals(2, updated.rewatchCount)
    }

    @Test
    fun testRewatchInProgressIncrement() {
        val entry = MediaEntry(
            title = "Steins;Gate",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            currentEpisodes = 5,
            totalEpisodes = 24,
            isRewatching = true,
            rewatchCount = 1
        )

        val updated = entry.incrementProgress()
        assertEquals(6, updated.currentEpisodes)
        assertEquals(MediaStatus.WATCHING, updated.status)
        assertEquals(true, updated.isRewatching)
        assertEquals(1, updated.rewatchCount)
    }

    @Test
    fun testForeignDatabaseIdAndSourceAliases() {
        val entry = MediaEntry(
            title = "Fullmetal Alchemist: Brotherhood",
            mediaType = MediaType.ANIME,
            status = MediaStatus.COMPLETED,
            externalId = "5114",
            externalSource = "MAL"
        )

        assertEquals("5114", entry.foreignDatabaseId)
        assertEquals("MAL", entry.foreignDatabaseSource)
    }
}
