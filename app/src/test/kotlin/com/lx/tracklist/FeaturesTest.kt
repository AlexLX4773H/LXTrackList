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

    @Test
    fun testHomeFilterStateStatusSelection() {
        var status: MediaStatus? = null
        var subCat: String? = null

        // Select WATCHING
        status = MediaStatus.WATCHING
        subCat = null

        assertNotNull(status)
        assertEquals(MediaStatus.WATCHING, status)
        assertNull(subCat)

        // Select subcategory
        subCat = "Action"
        status = null

        assertNull(status)
        assertEquals("Action", subCat)

        // Reset to ALL
        status = null
        subCat = null

        assertNull(status)
        assertNull(subCat)
    }

    @Test
    fun testSwipeToSwitchTabsState() {
        val homeState = com.lx.tracklist.ui.home.HomeUiState(
            isSwipeToSwitchTabsEnabled = true
        )
        assertTrue(homeState.isSwipeToSwitchTabsEnabled)

        val settingsState = com.lx.tracklist.ui.settings.SettingsUiState(
            isSwipeToSwitchTabsEnabled = true
        )
        assertTrue(settingsState.isSwipeToSwitchTabsEnabled)
    }

    @Test
    fun testTachiyomiImportAsFavoritesState() {
        val defaultSettingsState = com.lx.tracklist.ui.settings.SettingsUiState()
        assertFalse(defaultSettingsState.isTachiyomiImportAsFavorites)

        val customSettingsState = com.lx.tracklist.ui.settings.SettingsUiState(
            isTachiyomiImportAsFavorites = true
        )
        assertTrue(customSettingsState.isTachiyomiImportAsFavorites)

        val defaultPreview = com.lx.tracklist.ui.settings.ImportPreviewState(
            sourceName = "backup.tachibk",
            allEntries = emptyList(),
            categoryGroups = emptyList(),
            isTachiyomiSource = true,
            importFavorites = false
        )
        assertTrue(defaultPreview.isTachiyomiSource)
        assertFalse(defaultPreview.importFavorites)
    }

    @Test
    fun testAddEditSubCategorySelectionOnly() {
        // Feature 1: In Add/Edit, only choosing existing subcategories is permitted (no in-screen creation)
        val availableSubCategories = listOf(
            com.lx.tracklist.data.model.SubCategory(categoryKey = "ANIME", name = "Shonen"),
            com.lx.tracklist.data.model.SubCategory(categoryKey = "ANIME", name = "Isekai")
        )

        val state = com.lx.tracklist.ui.edit.AddEditUiState(
            title = "Jujutsu Kaisen",
            mediaType = MediaType.ANIME,
            subCategory = availableSubCategories[0].name
        )
        assertEquals("Shonen", state.subCategory)

        // Selecting "None"
        val stateNone = state.copy(subCategory = null)
        assertNull(stateNone.subCategory)
    }

    @Test
    fun testBatchUpdateLogic() {
        // Feature 2: Multi-select batch operations in Library
        val entries = listOf(
            MediaEntry(id = 1L, title = "Entry 1", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, isFavorite = false, subCategory = "Shonen"),
            MediaEntry(id = 2L, title = "Entry 2", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, isFavorite = true, subCategory = "Seinen"),
            MediaEntry(id = 3L, title = "Entry 3", mediaType = MediaType.MANGA, status = MediaStatus.PLAN_TO_WATCH, isFavorite = false, subCategory = "Romance")
        )
        val selectedIds = setOf(1L, 2L)

        // a. Change Category: changes mediaType and resets subCategory to null
        val afterCategoryChange = entries.map { entry ->
            if (selectedIds.contains(entry.id)) {
                entry.copy(mediaType = MediaType.MANHWA, customCategory = null, subCategory = null)
            } else entry
        }
        assertEquals(MediaType.MANHWA, afterCategoryChange[0].mediaType)
        assertNull(afterCategoryChange[0].subCategory)
        assertEquals(MediaType.MANHWA, afterCategoryChange[1].mediaType)
        assertNull(afterCategoryChange[1].subCategory)
        assertEquals(MediaType.MANGA, afterCategoryChange[2].mediaType) // unaffected
        assertEquals("Romance", afterCategoryChange[2].subCategory)

        // b. Set Sub Category
        val afterSubCategoryChange = entries.map { entry ->
            if (selectedIds.contains(entry.id)) {
                entry.copy(subCategory = "Action")
            } else entry
        }
        assertEquals("Action", afterSubCategoryChange[0].subCategory)
        assertEquals("Action", afterSubCategoryChange[1].subCategory)
        assertEquals("Romance", afterSubCategoryChange[2].subCategory)

        // c. Set Status
        val afterStatusChange = entries.map { entry ->
            if (selectedIds.contains(entry.id)) {
                entry.copy(status = MediaStatus.COMPLETED)
            } else entry
        }
        assertEquals(MediaStatus.COMPLETED, afterStatusChange[0].status)
        assertEquals(MediaStatus.COMPLETED, afterStatusChange[1].status)
        assertEquals(MediaStatus.PLAN_TO_WATCH, afterStatusChange[2].status)

        // d. Set / Unset Favorite
        val afterFavoriteSet = entries.map { entry ->
            if (selectedIds.contains(entry.id)) {
                entry.copy(isFavorite = true)
            } else entry
        }
        assertTrue(afterFavoriteSet[0].isFavorite)
        assertTrue(afterFavoriteSet[1].isFavorite)
        assertFalse(afterFavoriteSet[2].isFavorite)

        val afterFavoriteUnset = entries.map { entry ->
            if (selectedIds.contains(entry.id)) {
                entry.copy(isFavorite = false)
            } else entry
        }
        assertFalse(afterFavoriteUnset[0].isFavorite)
        assertFalse(afterFavoriteUnset[1].isFavorite)
    }

    @Test
    fun testAdvancedSubCategoryFilter() {
        // Feature 3: Subcategory as an advanced filter
        val entries = listOf(
            MediaEntry(id = 1L, title = "Attack on Titan", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, subCategory = "Dark Fantasy"),
            MediaEntry(id = 2L, title = "Demon Slayer", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, subCategory = "Shonen"),
            MediaEntry(id = 3L, title = "Solo Leveling", mediaType = MediaType.MANHWA, status = MediaStatus.WATCHING, subCategory = "Power Fantasy")
        )

        // Filter by Sub Category = "Dark Fantasy"
        val filtered = entries.filter { it.subCategory.equals("Dark Fantasy", ignoreCase = true) }
        assertEquals(1, filtered.size)
        assertEquals("Attack on Titan", filtered[0].title)

        // Filter by Sub Category = "Shonen"
        val filteredShonen = entries.filter { it.subCategory.equals("Shonen", ignoreCase = true) }
        assertEquals(1, filteredShonen.size)
        assertEquals("Demon Slayer", filteredShonen[0].title)
    }

    @Test
    fun testMultipleAlternateTitlesAndSearch() {
        // Feature 4: Multiple alternate titles storage and multi-title search
        val titles = listOf("Attack on Titan", "Shingeki no Kyojin", "進撃の巨人")
        val formatted = MediaEntry.formatAlternateTitles(titles)
        assertEquals("Attack on Titan\nShingeki no Kyojin\n進撃の巨人", formatted)

        val entry = MediaEntry(
            id = 1L,
            title = "AOT",
            alternateTitle = formatted,
            mediaType = MediaType.ANIME,
            status = MediaStatus.COMPLETED
        )

        // getAlternateTitles helper
        val parsedTitles = entry.getAlternateTitles()
        assertEquals(3, parsedTitles.size)
        assertEquals("Attack on Titan", parsedTitles[0])
        assertEquals("Shingeki no Kyojin", parsedTitles[1])
        assertEquals("進撃の巨人", parsedTitles[2])

        // Search matcher helper: title or any alternate title
        fun matchesQuery(e: MediaEntry, q: String): Boolean {
            return e.title.contains(q, ignoreCase = true) ||
                    e.getAlternateTitles().any { it.contains(q, ignoreCase = true) }
        }

        // Search by main title
        assertTrue(matchesQuery(entry, "AOT"))
        assertTrue(matchesQuery(entry, "aot"))

        // Search by 1st alternate title
        assertTrue(matchesQuery(entry, "Attack on Titan"))
        assertTrue(matchesQuery(entry, "titan"))

        // Search by 2nd alternate title
        assertTrue(matchesQuery(entry, "Shingeki"))
        assertTrue(matchesQuery(entry, "kyojin"))

        // Search by 3rd alternate title (Japanese kanji)
        assertTrue(matchesQuery(entry, "進撃"))
        assertTrue(matchesQuery(entry, "巨人"))

        // Non-matching query
        assertFalse(matchesQuery(entry, "Naruto"))
    }

    @Test
    fun testAiringProgressBreakdown() {
        // Airing anime with watched 8, aired 12, total 24
        val airingAnime = MediaEntry(
            id = 10L,
            title = "Jujutsu Kaisen Season 2",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            currentEpisodes = 8,
            airedEpisodes = 12,
            totalEpisodes = 24,
            isAiring = true
        )
        assertEquals("8/12 (24) ep", airingAnime.displayProgress)

        // Airing anime with unknown total episodes
        val airingUnknownTotal = MediaEntry(
            id = 11L,
            title = "One Piece",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            currentEpisodes = 1100,
            airedEpisodes = 1120,
            totalEpisodes = 0,
            isAiring = true
        )
        assertEquals("1100/1120 (?) ep", airingUnknownTotal.displayProgress)

        // Non-airing completed or standard anime
        val completedAnime = MediaEntry(
            id = 12L,
            title = "Frieren",
            mediaType = MediaType.ANIME,
            status = MediaStatus.COMPLETED,
            currentEpisodes = 28,
            totalEpisodes = 28,
            isAiring = false
        )
        assertEquals("28/28 ep", completedAnime.displayProgress)

        // Airing TV Show with seasons
        val airingShow = MediaEntry(
            id = 13L,
            title = "House of the Dragon",
            mediaType = MediaType.SHOW,
            status = MediaStatus.WATCHING,
            currentSeasons = 2,
            totalSeasons = 2,
            currentEpisodes = 4,
            airedEpisodes = 8,
            totalEpisodes = 8,
            isAiring = true
        )
        assertEquals("S2 • Ep 4/8 (8)", airingShow.displayProgress)

        // Ongoing Manga with chapters
        val ongoingManga = MediaEntry(
            id = 14L,
            title = "Chainsaw Man",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            trackingUnit = TrackingUnit.CHAPTERS,
            currentChapters = 140,
            airedChapters = 165,
            totalChapters = 200,
            isAiring = true
        )
        assertEquals("Ch 140/165 (200)", ongoingManga.displayProgress)
    }

    @Test
    fun testAiredAndUserDateRanges() {
        val cal = java.util.Calendar.getInstance()
        cal.set(2023, java.util.Calendar.APRIL, 1, 0, 0, 0)
        val airedStart = cal.timeInMillis
        cal.set(2023, java.util.Calendar.SEPTEMBER, 30, 0, 0, 0)
        val airedEnd = cal.timeInMillis

        cal.set(2023, java.util.Calendar.MAY, 10, 0, 0, 0)
        val userStart = cal.timeInMillis
        cal.set(2023, java.util.Calendar.OCTOBER, 5, 0, 0, 0)
        val userFinish = cal.timeInMillis

        val entry = MediaEntry(
            id = 20L,
            title = "Demon Slayer: Swordsmith Village Arc",
            mediaType = MediaType.ANIME,
            status = MediaStatus.COMPLETED,
            airedStartDate = airedStart,
            airedEndDate = airedEnd,
            startDate = userStart,
            finishDate = userFinish,
            isAiring = false
        )

        val airedRange = entry.formatAiredDateRange()
        assertTrue(airedRange.contains("2023-04-01"))
        assertTrue(airedRange.contains("2023-09-30"))

        val userRange = entry.formatUserDateRange()
        assertTrue(userRange.contains("2023-05-10"))
        assertTrue(userRange.contains("2023-10-05"))

        // Airing entry with no end date yet -> "Present"
        val ongoingEntry = MediaEntry(
            id = 21L,
            title = "Currently Airing Show",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            airedStartDate = airedStart,
            airedEndDate = null,
            isAiring = true
        )
        val ongoingAired = ongoingEntry.formatAiredDateRange()
        assertTrue(ongoingAired.contains("2023-04-01"))
        assertTrue(ongoingAired.contains("Present"))

        // Entry with no dates
        val emptyDates = MediaEntry(
            id = 22L,
            title = "No Dates Entry",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.PLAN_TO_WATCH
        )
        assertEquals("", emptyDates.formatAiredDateRange())
        assertEquals("", emptyDates.formatUserDateRange())
    }

    @Test
    fun testAddEditInitialStatePreselection() {
        // Preselection: Manga & Completed
        val state1 = com.lx.tracklist.ui.edit.AddEditUiState(
            mediaType = MediaType.MANGA,
            status = MediaStatus.COMPLETED,
            subCategory = null
        )
        assertEquals(MediaType.MANGA, state1.mediaType)
        assertEquals(MediaStatus.COMPLETED, state1.status)
        assertNull(state1.subCategory)

        // Preselection: Manga with Sub Category "Shonen" and status "Reading"
        val state2 = com.lx.tracklist.ui.edit.AddEditUiState(
            mediaType = MediaType.MANGA,
            customCategory = null,
            subCategory = "Shonen",
            status = MediaStatus.WATCHING
        )
        assertEquals(MediaType.MANGA, state2.mediaType)
        assertEquals("Shonen", state2.subCategory)
        assertEquals(MediaStatus.WATCHING, state2.status)

        // Preselection: Custom Category "Donghua" with status "Plan to Watch"
        val state3 = com.lx.tracklist.ui.edit.AddEditUiState(
            mediaType = MediaType.ANIME,
            customCategory = "Donghua",
            subCategory = "Cultivation",
            status = MediaStatus.PLAN_TO_WATCH
        )
        assertEquals(MediaType.ANIME, state3.mediaType)
        assertEquals("Donghua", state3.customCategory)
        assertEquals("Cultivation", state3.subCategory)
        assertEquals(MediaStatus.PLAN_TO_WATCH, state3.status)
    }

    @Test
    fun testCoverCacheManagerFormatBytes() {
        assertEquals("0 B", com.lx.tracklist.data.local.CoverCacheManager.formatBytes(-5L))
        assertEquals("0 B", com.lx.tracklist.data.local.CoverCacheManager.formatBytes(0L))
        assertEquals("500 B", com.lx.tracklist.data.local.CoverCacheManager.formatBytes(500L))
        assertEquals("1.5 KB", com.lx.tracklist.data.local.CoverCacheManager.formatBytes(1536L))
        assertEquals("10.0 MB", com.lx.tracklist.data.local.CoverCacheManager.formatBytes(10L * 1024L * 1024L))
        assertEquals("1.50 GB", com.lx.tracklist.data.local.CoverCacheManager.formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testCoverCacheManagerFilterEligibleUrls() {
        val entry1 = MediaEntry(
            id = 1L,
            title = "Solo Leveling",
            mediaType = MediaType.MANHWA,
            status = MediaStatus.COMPLETED,
            coverImageUri = "https://example.com/solo.jpg"
        )
        val entry2 = MediaEntry(
            id = 2L,
            title = "Solo Leveling Alt",
            mediaType = MediaType.MANHWA,
            status = MediaStatus.COMPLETED,
            coverImageUri = "  https://example.com/solo.jpg  " // duplicate and whitespace
        )
        val entry3 = MediaEntry(
            id = 3L,
            title = "HTTP Image",
            mediaType = MediaType.ANIME,
            status = MediaStatus.PLAN_TO_WATCH,
            coverImageUri = "http://example.com/anime.png"
        )
        val entry4 = MediaEntry(
            id = 4L,
            title = "Data URI Image",
            mediaType = MediaType.MANGA,
            status = MediaStatus.READING,
            coverImageUri = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA"
        )
        val entry5 = MediaEntry(
            id = 5L,
            title = "No Cover",
            mediaType = MediaType.MANGA,
            status = MediaStatus.READING,
            coverImageUri = null
        )
        val entry6 = MediaEntry(
            id = 6L,
            title = "Blank Cover",
            mediaType = MediaType.MANGA,
            status = MediaStatus.READING,
            coverImageUri = "   "
        )
        val entry7 = MediaEntry(
            id = 7L,
            title = "Invalid Protocol",
            mediaType = MediaType.MANGA,
            status = MediaStatus.READING,
            coverImageUri = "ftp://example.com/manga.jpg"
        )

        val entries = listOf(entry1, entry2, entry3, entry4, entry5, entry6, entry7)
        val eligible = com.lx.tracklist.data.local.CoverCacheManager.filterEligibleUrls(entries)

        assertEquals(3, eligible.size)
        assertTrue(eligible.contains("https://example.com/solo.jpg"))
        assertTrue(eligible.contains("http://example.com/anime.png"))
        assertTrue(eligible.contains("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA"))
        assertFalse(eligible.contains("ftp://example.com/manga.jpg"))
    }

    @Test
    fun testSettingsUiStateCoverCacheDefaults() {
        val state = com.lx.tracklist.ui.settings.SettingsUiState()
        assertEquals("0 B", state.coverCacheSizeFormatted)
        assertEquals(0, state.coverCacheCount)
        assertFalse(state.isReindexingCovers)
        assertEquals(0f, state.reindexProgress, 0.001f)
        assertNull(state.reindexStatusText)
    }
}
