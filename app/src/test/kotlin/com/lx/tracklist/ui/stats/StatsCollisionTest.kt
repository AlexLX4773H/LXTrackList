package com.lx.tracklist.ui.stats

import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsCollisionTest {

    @Test
    fun `colliding custom category name does not crash and stays isolated from built-in type`() {
        val customManhwa = CustomCategory(name = "Manhwa", mediaType = MediaType.MANHWA, isWatchType = false)
        val builtInManhwa = MediaEntry(
            id = 1L,
            title = "Solo Leveling",
            mediaType = MediaType.MANHWA,
            status = MediaStatus.COMPLETED,
            score = 9
        )
        val customEntry = MediaEntry(
            id = 2L,
            title = "Custom Manhwa Entry",
            mediaType = MediaType.MANHWA,
            customCategory = "Manhwa",
            status = MediaStatus.WATCHING,
            score = 7
        )
        val entries = listOf(builtInManhwa, customEntry)
        val customCats = listOf(customManhwa)

        val customSummary = calculateCategoryStatSummary(entries, customStatsKey("Manhwa"), customCats)
        assertEquals("Manhwa", customSummary.displayName)
        assertEquals(1, customSummary.totalEntries)
        assertEquals(7.0, customSummary.meanScore, 0.001)

        // Mixed-case built-in key must not throw IllegalArgumentException
        val builtInSummary = calculateCategoryStatSummary(entries, "Manhwa", customCats)
        assertEquals("Manhwa", builtInSummary.displayName)
        assertEquals(1, builtInSummary.totalEntries)
        assertEquals(9.0, builtInSummary.meanScore, 0.001)

        val customScoreDist = calculateScoreDistribution(entries, customStatsKey("Manhwa"))
        assertEquals(1, customScoreDist.totalCategoryEntries)
        assertEquals(1, customScoreDist.scoredCount)
        assertEquals(7.0, customScoreDist.meanScore, 0.001)

        val builtInScoreDist = calculateScoreDistribution(entries, "Manhwa")
        assertEquals(1, builtInScoreDist.totalCategoryEntries)
        assertEquals(9.0, builtInScoreDist.meanScore, 0.001)
    }

    @Test
    fun `STA-01 distribution rows do not double-count custom categories and fold orphaned entries into media type`() {
        val customDonghua = CustomCategory(name = "Donghua", mediaType = MediaType.ANIME, isWatchType = true)
        val customLightNovel = CustomCategory(name = "Light Novel", mediaType = MediaType.BOOK, isWatchType = false)
        val activeCustoms = listOf(customDonghua, customLightNovel)

        val entries = listOf(
            MediaEntry(id = 1L, title = "Frieren", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED),
            MediaEntry(id = 2L, title = "Link Click", mediaType = MediaType.ANIME, customCategory = "Donghua", status = MediaStatus.WATCHING),
            MediaEntry(id = 3L, title = "Orphaned Legacy", mediaType = MediaType.ANIME, customCategory = "DeletedCategory", status = MediaStatus.PLAN_TO_WATCH),
            MediaEntry(id = 4L, title = "Bookworm", mediaType = MediaType.BOOK, customCategory = "Light Novel", status = MediaStatus.WATCHING),
            MediaEntry(id = 5L, title = "Dune", mediaType = MediaType.BOOK, status = MediaStatus.COMPLETED)
        )

        val builtInCounts = calculateBuiltInCategoryCounts(entries, activeCustoms)
        val customCounts = activeCustoms.sumOf { countEntriesForCustomCategory(entries, it.name) }

        // Frieren (1) + Orphaned Legacy (1) = 2 in Anime; Dune (1) in Book
        assertEquals(2, builtInCounts[MediaType.ANIME])
        assertEquals(1, builtInCounts[MediaType.BOOK])
        assertEquals(2, customCounts)
        // Sum of all distribution rows must strictly equal total entries (no double-counting)
        assertEquals(entries.size, builtInCounts.values.sum() + customCounts)

        // Category breakdown summary matches built-in row total
        val animeSummary = calculateCategoryStatSummary(entries, MediaType.ANIME.name, activeCustoms)
        assertEquals(2, animeSummary.totalEntries)
    }

    @Test
    fun `STA-02 dropped entries contribute zero remaining time in Stats summary`() {
        val droppedLongAnime = MediaEntry(
            id = 1L,
            title = "Dropped 500-ep Anime",
            mediaType = MediaType.ANIME,
            status = MediaStatus.DROPPED,
            currentEpisodes = 10,
            totalEpisodes = 500,
            episodeLengthMinutes = 24
        )
        val activeAnime = MediaEntry(
            id = 2L,
            title = "Active Anime",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            currentEpisodes = 0,
            totalEpisodes = 60,
            episodeLengthMinutes = 24
        )

        assertEquals(0.0, droppedLongAnime.calculateRemainingTimeMinutes(), 0.001)
        val summary = calculateCategoryStatSummary(listOf(droppedLongAnime, activeAnime), "ALL", emptyList())
        // Only activeAnime's 60 * 24 = 1440 min (1.0 day) counts toward daysRemaining
        assertEquals(1.0, summary.daysRemaining, 0.001)
    }
}
