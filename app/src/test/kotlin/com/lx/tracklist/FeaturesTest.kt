package com.lx.tracklist

import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.Priority
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.model.UserProfile
import com.lx.tracklist.ui.library.LibrarySortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturesTest {

    @Test
    fun testUserProfileDefaults() {
        val user1 = UserProfile(id = 1L, name = "Alex", avatarColorHex = "#A7EF60", isDefault = true)
        val user2 = UserProfile(id = 2L, name = "Guest", avatarColorHex = "#4DA8DA")

        assertEquals("Alex", user1.name)
        assertTrue(user1.isDefault)
        assertEquals("#A7EF60", user1.avatarColorHex)

        assertEquals("Guest", user2.name)
        assertFalse(user2.isDefault)
        assertEquals("#4DA8DA", user2.avatarColorHex)
    }

    @Test
    fun testCustomCategoryAttributes() {
        val cat = CustomCategory(
            id = 5L,
            name = "Donghua",
            isWatchType = true,
            trackingUnit = TrackingUnit.CHAPTERS,
            colorHex = "#FF7597"
        )

        assertEquals("Donghua", cat.name)
        assertTrue(cat.isWatchType)
        assertEquals("#FF7597", cat.colorHex)

        val novelCat = CustomCategory(
            name = "Web Novels",
            isWatchType = false,
            trackingUnit = TrackingUnit.CHAPTERS
        )
        assertFalse(novelCat.isWatchType)
        assertEquals(TrackingUnit.CHAPTERS, novelCat.trackingUnit)
    }

    @Test
    fun testMediaEntryWithUserAndCustomCategory() {
        val entry = MediaEntry(
            id = 100L,
            userId = 2L,
            title = "Soul Land",
            mediaType = MediaType.SHOW,
            customCategory = "Donghua",
            status = MediaStatus.WATCHING,
            currentEpisodes = 120,
            totalEpisodes = 250
        )

        assertEquals(2L, entry.userId)
        assertEquals("Donghua", entry.customCategory)
        assertEquals("120/250 ep", entry.displayProgress)
        assertEquals(120f / 250f, entry.progressFraction, 0.001f)
    }

    @Test
    fun testRangeSelectionLogic() {
        val visibleIds = listOf(10L, 20L, 30L, 40L, 50L)
        val selected = setOf(20L, 40L)

        val indices = visibleIds.mapIndexedNotNull { index, id ->
            if (selected.contains(id)) index else null
        }
        val minIdx = indices.minOrNull() ?: 0
        val maxIdx = indices.maxOrNull() ?: 0

        assertEquals(1, minIdx) // 20L is at index 1
        assertEquals(3, maxIdx) // 40L is at index 3

        val range = visibleIds.subList(minIdx, maxIdx + 1).toSet()
        assertEquals(setOf(20L, 30L, 40L), range)
    }

    @Test
    fun testInvertSelectionLogic() {
        val visibleIds = listOf(10L, 20L, 30L, 40L, 50L)
        val current = setOf(10L, 30L)
        val inverted = visibleIds.filter { !current.contains(it) }.toSet()

        assertEquals(setOf(20L, 40L, 50L), inverted)
    }

    @Test
    fun testLibrarySortingLogic() {
        val entry1 = MediaEntry(id = 1L, title = "Bleach", score = 8, currentEpisodes = 10, totalEpisodes = 20)
        val entry2 = MediaEntry(id = 2L, title = "Attack on Titan", score = 10, currentEpisodes = 20, totalEpisodes = 20)
        val entry3 = MediaEntry(id = 3L, title = "Chainsaw Man", score = 7, currentEpisodes = 1, totalEpisodes = 12)
        val list = listOf(entry1, entry2, entry3)

        // Title ASC
        val byTitleAsc = list.sortedBy { it.title.lowercase() }
        assertEquals(listOf("Attack on Titan", "Bleach", "Chainsaw Man"), byTitleAsc.map { it.title })

        // Title DESC
        val byTitleDesc = list.sortedByDescending { it.title.lowercase() }
        assertEquals(listOf("Chainsaw Man", "Bleach", "Attack on Titan"), byTitleDesc.map { it.title })

        // Score DESC
        val byScoreDesc = list.sortedWith(compareByDescending<MediaEntry> { it.score ?: -1 }.thenByDescending { it.updatedAt })
        assertEquals(listOf(10, 8, 7), byScoreDesc.map { it.score })

        // Progress DESC
        val byProgressDesc = list.sortedByDescending { it.progressFraction }
        assertEquals(listOf(2L, 1L, 3L), byProgressDesc.map { it.id })
    }

    @Test
    fun testLibraryFilterScoreAndAiring() {
        val entries = listOf(
            MediaEntry(id = 1L, title = "A", score = 9, isAiring = true, status = MediaStatus.WATCHING),
            MediaEntry(id = 2L, title = "B", score = 6, isAiring = false, status = MediaStatus.COMPLETED),
            MediaEntry(id = 3L, title = "C", score = null, isAiring = true, status = MediaStatus.PLAN_TO_WATCH)
        )

        val score8Plus = entries.filter { (it.score ?: 0) >= 8 }
        assertEquals(1, score8Plus.size)
        assertEquals("A", score8Plus.first().title)

        val unrated = entries.filter { it.score == null || it.score == 0 }
        assertEquals(1, unrated.size)
        assertEquals("C", unrated.first().title)

        val airingOnly = entries.filter { it.isAiring }
        assertEquals(2, airingOnly.size)
    }
}
