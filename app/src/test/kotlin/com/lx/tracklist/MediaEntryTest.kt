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

    @Test
    fun testRelatedMediaItemsSerializationAndCorrelation() {
        val relatedList = listOf(
            com.lx.tracklist.data.model.RelatedMediaItem(
                title = "Attack on Titan Season 2",
                relationship = "Sequel",
                externalId = "25777",
                externalSource = "MAL",
                mediaType = MediaType.ANIME
            ),
            com.lx.tracklist.data.model.RelatedMediaItem(
                title = "Attack on Titan: No Regrets",
                relationship = "Side story",
                externalId = "25781",
                externalSource = "MAL",
                mediaType = MediaType.ANIME
            ),
            com.lx.tracklist.data.model.RelatedMediaItem(
                title = "Attack on Titan: Junior High",
                relationship = "Spin-off",
                externalId = "31374",
                externalSource = "MAL",
                mediaType = MediaType.ANIME
            )
        )
        val json = com.google.gson.Gson().toJson(relatedList)

        val mainEntry = MediaEntry(
            id = 1L,
            title = "Attack on Titan",
            mediaType = MediaType.ANIME,
            status = MediaStatus.COMPLETED,
            externalId = "16498",
            externalSource = "MAL",
            relatedEntriesJson = json
        )

        // 1. Verify deserialization
        val items = mainEntry.getRelatedMediaItems()
        assertEquals(3, items.size)
        assertEquals("Attack on Titan Season 2", items[0].title)
        assertEquals("Sequel", items[0].relationship)

        // 2. Correlate with library (User only has Season 2 in library, not No Regrets or Junior High)
        val library = listOf(
            mainEntry,
            MediaEntry(
                id = 10L,
                title = "Attack on Titan Season 2",
                mediaType = MediaType.ANIME,
                status = MediaStatus.WATCHING,
                externalId = "25777",
                externalSource = "MAL"
            )
        )

        val correlated = mainEntry.getCorrelatedRelatedItems(library)
        assertEquals(3, correlated.size)

        // Season 2 is being tracked
        assertEquals(10L, correlated[0].inLibraryEntryId)
        assertEquals(MediaStatus.WATCHING, correlated[0].inLibraryStatus)

        // No Regrets is untracked
        org.junit.Assert.assertNull(correlated[1].inLibraryEntryId)
        org.junit.Assert.assertNull(correlated[1].inLibraryStatus)

        // Junior High is untracked
        org.junit.Assert.assertNull(correlated[2].inLibraryEntryId)
        org.junit.Assert.assertNull(correlated[2].inLibraryStatus)
    }
}
