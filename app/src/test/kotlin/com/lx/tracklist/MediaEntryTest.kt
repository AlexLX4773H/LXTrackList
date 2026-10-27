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
}
