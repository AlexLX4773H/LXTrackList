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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturesTest {

    @Test
    fun testUserProfileDefaults() {
        val user1 = UserProfile(id = 1L, name = "Alex", avatarColorHex = "#A7EF60", isDefault = true)
        val user2 = UserProfile(id = 2L, name = "Guest", avatarColorHex = "#4DA8DA")
        val user3 = UserProfile(id = 3L, name = "Otaku", avatarColorHex = "#BB86FC", avatarImageUri = "/storage/emulated/0/avatar.jpg")

        assertEquals("Alex", user1.name)
        assertTrue(user1.isDefault)
        assertEquals("#A7EF60", user1.avatarColorHex)
        org.junit.Assert.assertNull(user1.avatarImageUri)

        assertEquals("Guest", user2.name)
        assertFalse(user2.isDefault)
        assertEquals("#4DA8DA", user2.avatarColorHex)
        org.junit.Assert.assertNull(user2.avatarImageUri)

        assertEquals("Otaku", user3.name)
        assertEquals("/storage/emulated/0/avatar.jpg", user3.avatarImageUri)
    }

    @Test
    fun testCustomCategoryAttributes() {
        val cat = CustomCategory(
            id = 5L,
            name = "Donghua",
            isWatchType = true,
            trackingUnit = TrackingUnit.CHAPTERS,
            colorHex = "#FF7597",
            mediaType = MediaType.ANIME
        )

        assertEquals("Donghua", cat.name)
        assertTrue(cat.isWatchType)
        assertEquals("#FF7597", cat.colorHex)
        assertEquals(MediaType.ANIME, cat.mediaType)

        val novelCat = CustomCategory(
            name = "Web Novels",
            isWatchType = false,
            trackingUnit = TrackingUnit.CHAPTERS,
            mediaType = MediaType.MANGA
        )
        assertFalse(novelCat.isWatchType)
        assertEquals(TrackingUnit.CHAPTERS, novelCat.trackingUnit)
        assertEquals(MediaType.MANGA, novelCat.mediaType)
    }

    @Test
    fun testMediaEntryWithUserAndCustomCategory() {
        val entry = MediaEntry(
            id = 100L,
            userId = 2L,
            title = "Soul Land",
            mediaType = MediaType.SHOW,
            customCategory = "Donghua",
            subCategory = "Cultivation",
            status = MediaStatus.WATCHING,
            currentEpisodes = 120,
            totalEpisodes = 250
        )

        assertEquals(2L, entry.userId)
        assertEquals("Donghua", entry.customCategory)
        assertEquals("Cultivation", entry.subCategory)
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
        val entry1 = MediaEntry(id = 1L, title = "Bleach", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, score = 8, currentEpisodes = 10, totalEpisodes = 20)
        val entry2 = MediaEntry(id = 2L, title = "Attack on Titan", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED, score = 10, currentEpisodes = 20, totalEpisodes = 20)
        val entry3 = MediaEntry(id = 3L, title = "Chainsaw Man", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, score = 7, currentEpisodes = 1, totalEpisodes = 12)
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
            MediaEntry(id = 1L, title = "A", mediaType = MediaType.ANIME, score = 9, isAiring = true, status = MediaStatus.WATCHING),
            MediaEntry(id = 2L, title = "B", mediaType = MediaType.ANIME, score = 6, isAiring = false, status = MediaStatus.COMPLETED),
            MediaEntry(id = 3L, title = "C", mediaType = MediaType.ANIME, score = null, isAiring = true, status = MediaStatus.PLAN_TO_WATCH)
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

    @Test
    fun testAddEditResetSavedState() {
        val state = com.lx.tracklist.ui.edit.AddEditUiState(isSaved = true)
        assertTrue(state.isSaved)
        val reset = state.copy(isSaved = false)
        assertFalse(reset.isSaved)
    }

    @Test
    fun testNavDestinationAddEditUniqueSessions() {
        val dest1 = com.lx.tracklist.ui.navigation.NavDestination.AddEdit(entryId = 10L, sessionId = 100L)
        val dest2 = com.lx.tracklist.ui.navigation.NavDestination.AddEdit(entryId = 10L, sessionId = 200L)
        assertEquals(10L, dest1.entryId)
        assertEquals(10L, dest2.entryId)
        assertTrue(dest1.sessionId != dest2.sessionId)
    }

    @Test
    fun testSubCategoryEntityAndNonSharedKeys() {
        val mangaIsekai = com.lx.tracklist.data.model.SubCategory(
            categoryKey = "MANGA",
            name = "Isekai",
            colorHex = "#FF7597"
        )
        val manhwaMurim = com.lx.tracklist.data.model.SubCategory(
            categoryKey = "MANHWA",
            name = "Murim",
            colorHex = "#4DA8DA"
        )

        assertEquals("MANGA", mangaIsekai.categoryKey)
        assertEquals("Isekai", mangaIsekai.name)
        assertEquals("MANHWA", manhwaMurim.categoryKey)
        assertEquals("Murim", manhwaMurim.name)

        val allSubCategories = listOf(mangaIsekai, manhwaMurim)

        // Manga subcategories should only contain Isekai, NOT Murim
        val mangaSubs = allSubCategories.filter { it.categoryKey.equals(MediaType.MANGA.name, ignoreCase = true) }
        assertEquals(1, mangaSubs.size)
        assertEquals("Isekai", mangaSubs[0].name)

        // Manhwa subcategories should only contain Murim, NOT Isekai
        val manhwaSubs = allSubCategories.filter { it.categoryKey.equals(MediaType.MANHWA.name, ignoreCase = true) }
        assertEquals(1, manhwaSubs.size)
        assertEquals("Murim", manhwaSubs[0].name)
    }

    @Test
    fun testAddEditSubCategoryState() {
        val state = com.lx.tracklist.ui.edit.AddEditUiState(
            title = "Mushoku Tensei",
            mediaType = MediaType.MANGA,
            customCategory = null,
            subCategory = "Isekai"
        )
        assertEquals("Mushoku Tensei", state.title)
        assertEquals(MediaType.MANGA, state.mediaType)
        assertNull(state.customCategory)
        assertEquals("Isekai", state.subCategory)
    }
}
