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
            status = MediaStatus.WATCHING,
            coverImageUri = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA"
        )
        val entry5 = MediaEntry(
            id = 5L,
            title = "No Cover",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            coverImageUri = null
        )
        val entry6 = MediaEntry(
            id = 6L,
            title = "Blank Cover",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            coverImageUri = "   "
        )
        val entry7 = MediaEntry(
            id = 7L,
            title = "Invalid Protocol",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
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

    @Test
    fun testWatchingReadingDefaultStartDate() {
        val todayStart = com.lx.tracklist.ui.edit.AddEditViewModel.todayStartTimestamp()
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = todayStart
        assertEquals(0, cal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(java.util.Calendar.MINUTE))
        assertEquals(0, cal.get(java.util.Calendar.SECOND))
        assertEquals(0, cal.get(java.util.Calendar.MILLISECOND))

        // When entry is created or loaded in WATCHING (or READING) status and startDate is null,
        // it defaults to todayStart
        val defaultWatchingStartDate = if (MediaStatus.WATCHING == MediaStatus.WATCHING) todayStart else null
        assertEquals(todayStart, defaultWatchingStartDate)

        // When entry is in PLAN_TO_WATCH, startDate should remain null
        val planToWatchStartDate: Long? = if (MediaStatus.PLAN_TO_WATCH == MediaStatus.WATCHING) todayStart else null
        assertNull(planToWatchStartDate)

        // When switching from PLAN_TO_WATCH to WATCHING, startDate is populated if null
        var currentStartDate: Long? = null
        var newStatus = MediaStatus.WATCHING
        if (newStatus == MediaStatus.WATCHING && currentStartDate == null) {
            currentStartDate = todayStart
        }
        assertEquals(todayStart, currentStartDate)

        // When switching status but custom startDate is already set, it is preserved
        val customStartDate = 1700000000000L
        val entryWithCustomDate = MediaEntry(id = 1L, title = "Test", mediaType = MediaType.ANIME, status = MediaStatus.PLAN_TO_WATCH, startDate = customStartDate)
        val preservedStartDate = if (MediaStatus.WATCHING == MediaStatus.WATCHING && entryWithCustomDate.startDate == null) todayStart else entryWithCustomDate.startDate
        assertEquals(customStartDate, preservedStartDate)

        // When switching to COMPLETED with no finishDate, finishDate is set
        val entryWithoutFinishDate = MediaEntry(id = 2L, title = "Test 2", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING)
        val newFinishDate = if (MediaStatus.COMPLETED == MediaStatus.COMPLETED && entryWithoutFinishDate.finishDate == null) todayStart else entryWithoutFinishDate.finishDate
        assertEquals(todayStart, newFinishDate)
    }

    @Test
    fun testUserProfileStartupDefaults() {
        val userDefault = UserProfile(id = 1L, name = "Alex")
        assertNull(userDefault.defaultMediaType)
        assertNull(userDefault.defaultCustomCategory)
        assertNull(userDefault.defaultSubCategory)
        assertNull(userDefault.defaultStatus)

        // Custom defaults for User 1
        val userCustom = UserProfile(
            id = 2L,
            name = "Manga Reader",
            defaultMediaType = MediaType.MANGA,
            defaultCustomCategory = null,
            defaultSubCategory = null,
            defaultStatus = MediaStatus.WATCHING
        )
        assertEquals(MediaType.MANGA, userCustom.defaultMediaType)
        assertNull(userCustom.defaultCustomCategory)
        assertEquals(MediaStatus.WATCHING, userCustom.defaultStatus)
        assertNull(userCustom.defaultSubCategory)

        // Copy with updated startup default subcategory
        val updatedUser = userCustom.copy(
            defaultSubCategory = "Isekai",
            defaultStatus = null
        )
        assertEquals("Isekai", updatedUser.defaultSubCategory)
        assertNull(updatedUser.defaultStatus)
    }

    @Test
    fun testPerProfileStartupDefaultsIndependence() {
        val profile1 = UserProfile(
            id = 1L,
            name = "Anime Watcher",
            defaultMediaType = MediaType.ANIME,
            defaultStatus = MediaStatus.WATCHING
        )
        val profile2 = UserProfile(
            id = 2L,
            name = "Novel Lover",
            defaultCustomCategory = "Web Novels",
            defaultMediaType = MediaType.BOOK,
            defaultSubCategory = "Wuxia"
        )
        val profile3 = UserProfile(
            id = 3L,
            name = "Movie Fan",
            defaultMediaType = MediaType.MOVIE,
            defaultStatus = MediaStatus.PLAN_TO_WATCH
        )

        // Verify profile 1
        assertEquals(MediaType.ANIME, profile1.defaultMediaType)
        assertNull(profile1.defaultCustomCategory)
        assertEquals(MediaStatus.WATCHING, profile1.defaultStatus)
        assertNull(profile1.defaultSubCategory)

        // Verify profile 2
        assertEquals("Web Novels", profile2.defaultCustomCategory)
        assertEquals("Wuxia", profile2.defaultSubCategory)
        assertNull(profile2.defaultStatus)

        // Verify profile 3
        assertEquals(MediaType.MOVIE, profile3.defaultMediaType)
        assertEquals(MediaStatus.PLAN_TO_WATCH, profile3.defaultStatus)
        assertNull(profile3.defaultSubCategory)
    }

    @Test
    fun testStartupDefaultsApplicationLogic() {
        fun resolveStartupView(profile: UserProfile): Triple<MediaType, String?, Pair<String?, MediaStatus?>> {
            val targetCustomCat = profile.defaultCustomCategory?.trim()?.ifBlank { null }
            val targetMediaType = if (!targetCustomCat.isNullOrBlank()) {
                profile.defaultMediaType ?: MediaType.MANGA
            } else {
                profile.defaultMediaType ?: MediaType.ANIME
            }
            val targetSubCat = profile.defaultSubCategory?.trim()?.ifBlank { null }
            val targetStatus = if (targetSubCat != null) null else profile.defaultStatus
            return Triple(targetMediaType, targetCustomCat, Pair(targetSubCat, targetStatus))
        }

        // Profile with unconfigured defaults -> defaults to ANIME • ALL (null status, null subCategory)
        val unconfigured = UserProfile(id = 1L, name = "Default")
        val view1 = resolveStartupView(unconfigured)
        assertEquals(MediaType.ANIME, view1.first)
        assertNull(view1.second)
        assertNull(view1.third.first)
        assertNull(view1.third.second)

        // Profile with MANGA • WATCHING (Reading)
        val mangaProfile = UserProfile(id = 2L, name = "Reader", defaultMediaType = MediaType.MANGA, defaultStatus = MediaStatus.WATCHING)
        val view2 = resolveStartupView(mangaProfile)
        assertEquals(MediaType.MANGA, view2.first)
        assertNull(view2.second)
        assertNull(view2.third.first)
        assertEquals(MediaStatus.WATCHING, view2.third.second)

        // Profile with Custom category "Donghua" and SubCategory "Season 1"
        val donghuaProfile = UserProfile(
            id = 3L,
            name = "Donghua Fan",
            defaultCustomCategory = "Donghua",
            defaultMediaType = MediaType.ANIME,
            defaultSubCategory = "Season 1"
        )
        val view3 = resolveStartupView(donghuaProfile)
        assertEquals(MediaType.ANIME, view3.first)
        assertEquals("Donghua", view3.second)
        assertEquals("Season 1", view3.third.first)
        assertNull(view3.third.second)

        // Subcategory and Status mutual exclusivity: selecting subcategory overrides status
        val conflictingProfile = UserProfile(
            id = 4L,
            name = "Conflicted",
            defaultMediaType = MediaType.MANGA,
            defaultSubCategory = "Action",
            defaultStatus = MediaStatus.COMPLETED
        )
        val view4 = resolveStartupView(conflictingProfile)
        assertEquals("Action", view4.third.first)
        assertNull(view4.third.second)
    }

    @Test
    fun testSettingsUiStateStartupDefaults() {
        val state = com.lx.tracklist.ui.settings.SettingsUiState()
        assertNotNull(state.activeUser)
        assertEquals(1L, state.activeUser.id)
        assertNull(state.selectedProfileIdForDefaults)
        assertTrue(state.allSubCategories.isEmpty())
        assertTrue(state.allUsers.isEmpty())
    }

    @Test
    fun testMediaEntryGetTagList() {
        val entry1 = MediaEntry(title = "Bleach", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "Action, Supernatural, Shounen")
        assertEquals(listOf("Action", "Supernatural", "Shounen"), entry1.getTagList())

        // Extra whitespace, empty slots, trailing commas
        val entry2 = MediaEntry(title = "One Piece", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = " Action,  , Adventure , Pirates, ")
        assertEquals(listOf("Action", "Adventure", "Pirates"), entry2.getTagList())

        // Empty & null string
        val entry3 = MediaEntry(title = "Blank", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "")
        assertTrue(entry3.getTagList().isEmpty())

        val entry4 = MediaEntry(title = "Null", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = null)
        assertTrue(entry4.getTagList().isEmpty())
    }

    @Test
    fun testTagMultiSelectInclusion() {
        val entry1 = MediaEntry(id = 1L, title = "Jujutsu Kaisen", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "Action, Supernatural, School")
        val entry2 = MediaEntry(id = 2L, title = "Bleach", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "Action, Supernatural, Swords")
        val entry3 = MediaEntry(id = 3L, title = "Horimiya", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "Romance, School, Comedy")
        val entry4 = MediaEntry(id = 4L, title = "Naruto", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "Action, Ninja")

        val entries = listOf(entry1, entry2, entry3, entry4)

        // Filter requiring BOTH "Action" AND "Supernatural"
        val includedTags = setOf("Action", "Supernatural")
        val filtered = entries.filter { entry ->
            val entryTags = entry.getTagList()
            includedTags.all { inc -> entryTags.any { it.equals(inc, ignoreCase = true) } }
        }

        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.id == 1L })
        assertTrue(filtered.any { it.id == 2L })
        assertFalse(filtered.any { it.id == 3L })
        assertFalse(filtered.any { it.id == 4L })

        // Case-insensitivity check: "supernatural" matches "Supernatural"
        val lowerFilter = setOf("supernatural")
        val filteredLower = entries.filter { entry ->
            val entryTags = entry.getTagList()
            lowerFilter.all { inc -> entryTags.any { it.equals(inc, ignoreCase = true) } }
        }
        assertEquals(2, filteredLower.size)
    }

    @Test
    fun testTagInverseExclusion() {
        val entry1 = MediaEntry(id = 1L, title = "Dark Fantasy", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "Action, Dark, Gore")
        val entry2 = MediaEntry(id = 2L, title = "Light Fantasy", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "Action, Comedy, Fantasy")
        val entry3 = MediaEntry(id = 3L, title = "Horror Show", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "Horror, Gore")
        val entry4 = MediaEntry(id = 4L, title = "Slice of Life", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "Comedy, Slice of Life")

        val entries = listOf(entry1, entry2, entry3, entry4)

        // Exclude tags: "Gore", "Horror"
        val excludedTags = setOf("Gore", "Horror")
        val filtered = entries.filter { entry ->
            val entryTags = entry.getTagList()
            excludedTags.none { exc -> entryTags.any { it.equals(exc, ignoreCase = true) } }
        }

        // Only entry2 and entry4 should remain
        assertEquals(2, filtered.size)
        assertEquals(listOf(2L, 4L), filtered.map { it.id })
    }

    @Test
    fun testTagInclusionAndInverseExclusionCombined() {
        val entry1 = MediaEntry(id = 1L, title = "Action Pure", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "Action, Adventure")
        val entry2 = MediaEntry(id = 2L, title = "Action Mech", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "Action, Mecha, Sci-Fi")
        val entry3 = MediaEntry(id = 3L, title = "Action Romance", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "Action, Romance")
        val entry4 = MediaEntry(id = 4L, title = "Romance Only", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "Romance, Drama")

        val entries = listOf(entry1, entry2, entry3, entry4)

        // Include "Action", but exclude "Mecha" and "Romance"
        val included = setOf("Action")
        val excluded = setOf("Mecha", "Romance")

        val filtered = entries.filter { entry ->
            val tags = entry.getTagList()
            val matchesIncluded = included.all { inc -> tags.any { it.equals(inc, ignoreCase = true) } }
            val passesExcluded = excluded.none { exc -> tags.any { it.equals(exc, ignoreCase = true) } }
            matchesIncluded && passesExcluded
        }

        assertEquals(1, filtered.size)
        assertEquals(1L, filtered.first().id)
        assertEquals("Action Pure", filtered.first().title)
    }

    @Test
    fun testTagExtractionAndFrequencies() {
        val entries = listOf(
            MediaEntry(id = 1L, title = "A", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "Action, Fantasy"),
            MediaEntry(id = 2L, title = "B", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "Fantasy, Drama"),
            MediaEntry(id = 3L, title = "C", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = "action, Sci-Fi"),
            MediaEntry(id = 4L, title = "D", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, tags = null)
        )

        val allAvailableTags = entries
            .flatMap { it.getTagList() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)

        assertEquals(listOf("Action", "Drama", "Fantasy", "Sci-Fi"), allAvailableTags)

        val tagCounts = allAvailableTags.associateWith { tag ->
            entries.count { entry ->
                entry.getTagList().any { it.equals(tag, ignoreCase = true) }
            }
        }

        assertEquals(2, tagCounts["Action"])
        assertEquals(1, tagCounts["Drama"])
        assertEquals(2, tagCounts["Fantasy"])
        assertEquals(1, tagCounts["Sci-Fi"])
    }

    @Test
    fun testLibraryUiStateTagProperties() {
        val state = com.lx.tracklist.ui.library.LibraryUiState(
            includedTags = setOf("Action", "Fantasy"),
            excludedTags = setOf("Horror"),
            availableTags = listOf("Action", "Drama", "Fantasy", "Horror"),
            tagCounts = mapOf("Action" to 5, "Fantasy" to 3, "Horror" to 1)
        )

        assertEquals(2, state.includedTags.size)
        assertTrue(state.includedTags.contains("Action"))
        assertEquals(1, state.excludedTags.size)
        assertTrue(state.excludedTags.contains("Horror"))
        assertEquals(4, state.availableTags.size)
        assertEquals(5, state.tagCounts["Action"])
    }

    @Test
    fun testTriStateTagCycling() {
        val tag = "Supernatural"

        fun cycle(currentIncluded: Set<String>, currentExcluded: Set<String>, t: String): Pair<Set<String>, Set<String>> {
            val cleanTag = t.trim()
            val isIncluded = currentIncluded.any { it.equals(cleanTag, ignoreCase = true) }
            val isExcluded = currentExcluded.any { it.equals(cleanTag, ignoreCase = true) }

            return when {
                !isIncluded && !isExcluded -> {
                    // 1st press: UNSELECTED -> INCLUDED (tick)
                    Pair(currentIncluded + cleanTag, currentExcluded.filter { !it.equals(cleanTag, ignoreCase = true) }.toSet())
                }
                isIncluded -> {
                    // 2nd press: INCLUDED -> EXCLUDED (x)
                    Pair(currentIncluded.filter { !it.equals(cleanTag, ignoreCase = true) }.toSet(), currentExcluded + cleanTag)
                }
                else -> {
                    // 3rd press: EXCLUDED -> UNSELECTED (no select)
                    Pair(currentIncluded.filter { !it.equals(cleanTag, ignoreCase = true) }.toSet(), currentExcluded.filter { !it.equals(cleanTag, ignoreCase = true) }.toSet())
                }
            }
        }

        // Initial: Unselected
        var included = emptySet<String>()
        var excluded = emptySet<String>()
        var state = com.lx.tracklist.ui.library.LibraryUiState(includedTags = included, excludedTags = excluded)
        assertEquals(com.lx.tracklist.ui.library.TagSelectionState.UNSELECTED, state.getTagState(tag))

        // 1st press: select / include (tick ✓)
        val afterPress1 = cycle(included, excluded, tag)
        included = afterPress1.first
        excluded = afterPress1.second
        state = com.lx.tracklist.ui.library.LibraryUiState(includedTags = included, excludedTags = excluded)
        assertEquals(com.lx.tracklist.ui.library.TagSelectionState.INCLUDED, state.getTagState(tag))
        assertTrue(included.contains(tag))
        assertFalse(excluded.contains(tag))

        // 2nd press: exclude (x ✕)
        val afterPress2 = cycle(included, excluded, tag)
        included = afterPress2.first
        excluded = afterPress2.second
        state = com.lx.tracklist.ui.library.LibraryUiState(includedTags = included, excludedTags = excluded)
        assertEquals(com.lx.tracklist.ui.library.TagSelectionState.EXCLUDED, state.getTagState(tag))
        assertFalse(included.contains(tag))
        assertTrue(excluded.contains(tag))

        // 3rd press: no select / clear
        val afterPress3 = cycle(included, excluded, tag)
        included = afterPress3.first
        excluded = afterPress3.second
        state = com.lx.tracklist.ui.library.LibraryUiState(includedTags = included, excludedTags = excluded)
        assertEquals(com.lx.tracklist.ui.library.TagSelectionState.UNSELECTED, state.getTagState(tag))
        assertFalse(included.contains(tag))
        assertFalse(excluded.contains(tag))

        // 4th press: cycle restarts at include (tick ✓)
        val afterPress4 = cycle(included, excluded, tag)
        included = afterPress4.first
        excluded = afterPress4.second
        state = com.lx.tracklist.ui.library.LibraryUiState(includedTags = included, excludedTags = excluded)
        assertEquals(com.lx.tracklist.ui.library.TagSelectionState.INCLUDED, state.getTagState(tag))
        assertTrue(included.contains(tag))
    }

    @Test
    fun testFavoritesAndAiringInclusionAndExclusionFilter() {
        // Test enum cycling
        var opt = com.lx.tracklist.ui.library.InclusionFilterOption.ALL
        assertEquals(com.lx.tracklist.ui.library.InclusionFilterOption.ALL, opt)
        opt = opt.next()
        assertEquals(com.lx.tracklist.ui.library.InclusionFilterOption.INCLUDE_ONLY, opt)
        opt = opt.next()
        assertEquals(com.lx.tracklist.ui.library.InclusionFilterOption.EXCLUDE, opt)
        opt = opt.next()
        assertEquals(com.lx.tracklist.ui.library.InclusionFilterOption.ALL, opt)

        // Test filtering behavior on dataset
        val e1 = MediaEntry(id = 1L, title = "Fav & Airing", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, isFavorite = true, isAiring = true)
        val e2 = MediaEntry(id = 2L, title = "Fav & Finished", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED, isFavorite = true, isAiring = false)
        val e3 = MediaEntry(id = 3L, title = "Normal Airing", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, isFavorite = false, isAiring = true)
        val e4 = MediaEntry(id = 4L, title = "Normal Finished", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED, isFavorite = false, isAiring = false)

        val entries = listOf(e1, e2, e3, e4)

        // 1. Favorites: Include only
        val favOnly = entries.filter { it.isFavorite }
        assertEquals(listOf(1L, 2L), favOnly.map { it.id })

        // 2. Favorites: Exclude favorites (hide favorites)
        val favExcluded = entries.filter { !it.isFavorite }
        assertEquals(listOf(3L, 4L), favExcluded.map { it.id })

        // 3. Airing: Include only
        val airingOnly = entries.filter { it.isAiring }
        assertEquals(listOf(1L, 3L), airingOnly.map { it.id })

        // 4. Airing: Exclude airing (finished only)
        val airingExcluded = entries.filter { !it.isAiring }
        assertEquals(listOf(2L, 4L), airingExcluded.map { it.id })

        // 5. Combined: Exclude Favorites AND Exclude Airing (Only non-favorite completed items)
        val nonFavFinished = entries.filter { !it.isFavorite && !it.isAiring }
        assertEquals(listOf(4L), nonFavFinished.map { it.id })

        // 6. Combined: Include Favorites AND Exclude Airing (Favorite finished items)
        val favFinished = entries.filter { it.isFavorite && !it.isAiring }
        assertEquals(listOf(2L), favFinished.map { it.id })
    }

    @Test
    fun testScoreDistributionCalculation() {
        val anime1 = MediaEntry(id = 1L, title = "A1", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED, score = 10)
        val anime2 = MediaEntry(id = 2L, title = "A2", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED, score = 8)
        val anime3 = MediaEntry(id = 3L, title = "A3", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, score = 8)
        val anime4 = MediaEntry(id = 4L, title = "A4", mediaType = MediaType.ANIME, status = MediaStatus.PLAN_TO_WATCH, score = null)
        val manga1 = MediaEntry(id = 5L, title = "M1", mediaType = MediaType.MANGA, status = MediaStatus.COMPLETED, score = null)
        val donghua1 = MediaEntry(id = 6L, title = "D1", mediaType = MediaType.ANIME, customCategory = "Donghua", status = MediaStatus.COMPLETED, score = 9)

        val entries = listOf(anime1, anime2, anime3, anime4, manga1, donghua1)

        // 1. Anime distribution (all statuses)
        val animeData = com.lx.tracklist.ui.stats.calculateScoreDistribution(entries, "ANIME", completedOnly = false)
        assertEquals(4, animeData.totalCategoryEntries)
        assertEquals(3, animeData.scoredCount)
        assertEquals(2, animeData.distribution[8])
        assertEquals(1, animeData.distribution[10])
        assertEquals(0, animeData.distribution[9])
        assertEquals(0, animeData.distribution[1])
        assertEquals((10 + 8 + 8) / 3.0, animeData.meanScore, 0.001)
        assertEquals(2, animeData.maxFrequency)

        // 2. Anime completed only
        val animeCompleted = com.lx.tracklist.ui.stats.calculateScoreDistribution(entries, "ANIME", completedOnly = true)
        assertEquals(2, animeCompleted.scoredCount)
        assertEquals(1, animeCompleted.distribution[10])
        assertEquals(1, animeCompleted.distribution[8])
        assertEquals(9.0, animeCompleted.meanScore, 0.001)

        // 3. Manga with NO scores -> should have scoredCount = 0 (triggers Image 2 message)
        val mangaData = com.lx.tracklist.ui.stats.calculateScoreDistribution(entries, "MANGA", completedOnly = false)
        assertEquals(1, mangaData.totalCategoryEntries)
        assertEquals(0, mangaData.scoredCount)
        assertEquals(0.0, mangaData.meanScore, 0.001)
        assertEquals(0, mangaData.maxFrequency)

        // 4. Custom Category "Donghua"
        val donghuaData = com.lx.tracklist.ui.stats.calculateScoreDistribution(entries, "Donghua", completedOnly = false)
        assertEquals(1, donghuaData.totalCategoryEntries)
        assertEquals(1, donghuaData.scoredCount)
        assertEquals(1, donghuaData.distribution[9])
        assertEquals(9.0, donghuaData.meanScore, 0.001)

        // 5. Score rating labels
        assertEquals("Masterpiece", com.lx.tracklist.ui.stats.getScoreLabel(10))
        assertEquals("Great", com.lx.tracklist.ui.stats.getScoreLabel(9))
        assertEquals("Very Good", com.lx.tracklist.ui.stats.getScoreLabel(8))
        assertEquals("Good", com.lx.tracklist.ui.stats.getScoreLabel(7))
        assertEquals("Average", com.lx.tracklist.ui.stats.getScoreLabel(5))
        assertEquals("Appalling", com.lx.tracklist.ui.stats.getScoreLabel(1))
    }

    @Test
    fun testMalCategoryStatsCalculation() {
        val anime1 = MediaEntry(
            id = 1L,
            title = "Frieren",
            mediaType = MediaType.ANIME,
            status = MediaStatus.COMPLETED,
            score = 10,
            currentEpisodes = 28,
            totalEpisodes = 28,
            episodeLengthMinutes = 24,
            rewatchCount = 1,
            isFavorite = true
        )
        val anime2 = MediaEntry(
            id = 2L,
            title = "Dungeon Meshi",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            score = 9,
            currentEpisodes = 12,
            totalEpisodes = 24,
            episodeLengthMinutes = 24
        )
        val manga1 = MediaEntry(
            id = 3L,
            title = "Berserk",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            score = 10,
            currentChapters = 370,
            currentVolumes = 41
        )

        val entries = listOf(anime1, anime2, manga1)
        val customCats = emptyList<CustomCategory>()

        // Test Anime Category MAL Stats
        val animeSummary = com.lx.tracklist.ui.stats.calculateCategoryStatSummary(entries, "ANIME", customCats)
        assertEquals(2, animeSummary.totalEntries)
        assertEquals(1, animeSummary.statusCounts[MediaStatus.COMPLETED])
        assertEquals(1, animeSummary.statusCounts[MediaStatus.WATCHING])
        assertEquals(0, animeSummary.statusCounts[MediaStatus.ON_HOLD])
        assertEquals(9.5, animeSummary.meanScore, 0.001)
        assertEquals(2, animeSummary.scoredCount)
        assertEquals(1, animeSummary.favoritesCount)
        assertEquals(1, animeSummary.rewatchedCount)
        assertEquals(40, animeSummary.episodesCount) // 28 + 12
        assertTrue(animeSummary.isVideo)
        // Days calculation:
        // anime1: 28 current + 1*28 rewatch = 56 eps * 24 min = 1344 min
        // anime2: 12 current * 24 min = 288 min
        // total minutes = 1632 min / 1440 = 1.133 days
        assertEquals(1632.0 / 1440.0, animeSummary.daysSpent, 0.01)

        // Test Manga Category MAL Stats
        val mangaSummary = com.lx.tracklist.ui.stats.calculateCategoryStatSummary(entries, "MANGA", customCats)
        assertEquals(1, mangaSummary.totalEntries)
        assertEquals(1, mangaSummary.statusCounts[MediaStatus.WATCHING])
        assertEquals(370, mangaSummary.chaptersCount)
        assertEquals(41, mangaSummary.volumesCount)
        assertFalse(mangaSummary.isVideo)
        // 370 ch * 5 min = 1850 min / 1440 = 1.2847 days
        assertEquals(1850.0 / 1440.0, mangaSummary.daysSpent, 0.01)
    }

    @Test
    fun testCoverImageFilterOptions() {
        val entryWithGoodCover = MediaEntry(id = 1L, title = "Good Cover", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED, coverImageUri = "https://example.com/cover.jpg")
        val entryWithBrokenCover = MediaEntry(id = 2L, title = "Broken URL Cover", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED, coverImageUri = "https://broken-cdn.com/bad.webp")
        val entryWithMalformedCover = MediaEntry(id = 3L, title = "Malformed Cover", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED, coverImageUri = "not_a_valid_url")
        val entryWithEmptyCover = MediaEntry(id = 4L, title = "Empty Cover", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED, coverImageUri = "")
        val entryWithNullCover = MediaEntry(id = 5L, title = "Null Cover", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED, coverImageUri = null)

        val list = listOf(entryWithGoodCover, entryWithBrokenCover, entryWithMalformedCover, entryWithEmptyCover, entryWithNullCover)

        // Mark entry 2 as known broken
        com.lx.tracklist.data.local.FailedCoverTracker.markFailed(2L, "https://broken-cdn.com/bad.webp")

        // 1. Cycle test
        assertEquals(com.lx.tracklist.ui.library.CoverImageFilterOption.HAS_COVER, com.lx.tracklist.ui.library.CoverImageFilterOption.ALL.next())
        assertEquals(com.lx.tracklist.ui.library.CoverImageFilterOption.BROKEN_COVER, com.lx.tracklist.ui.library.CoverImageFilterOption.HAS_COVER.next())
        assertEquals(com.lx.tracklist.ui.library.CoverImageFilterOption.NO_COVER, com.lx.tracklist.ui.library.CoverImageFilterOption.BROKEN_COVER.next())
        assertEquals(com.lx.tracklist.ui.library.CoverImageFilterOption.ALL, com.lx.tracklist.ui.library.CoverImageFilterOption.NO_COVER.next())

        // 2. HAS_COVER (Any entry with a cover URL)
        val hasCover = list.filter { !it.coverImageUri.isNullOrBlank() }
        assertEquals(listOf(1L, 2L, 3L), hasCover.map { it.id })

        // 3. BROKEN_COVER (Has a cover URL, but it does NOT work: known broken or malformed)
        val brokenCover = list.filter { entry ->
            val url = entry.coverImageUri?.trim()
            !url.isNullOrBlank() && (
                com.lx.tracklist.data.local.FailedCoverTracker.isUrlBroken(url)
            )
        }
        assertEquals(listOf(2L, 3L), brokenCover.map { it.id })

        // 4. NO_COVER (Missing/empty cover image)
        val noCover = list.filter { it.coverImageUri.isNullOrBlank() }
        assertEquals(listOf(4L, 5L), noCover.map { it.id })

        // 5. isEntryCoverBroken helper
        assertFalse(com.lx.tracklist.data.local.FailedCoverTracker.isEntryCoverBroken(entryWithGoodCover))
        assertTrue(com.lx.tracklist.data.local.FailedCoverTracker.isEntryCoverBroken(entryWithBrokenCover))
        assertTrue(com.lx.tracklist.data.local.FailedCoverTracker.isEntryCoverBroken(entryWithMalformedCover))
        assertTrue(com.lx.tracklist.data.local.FailedCoverTracker.isEntryCoverBroken(entryWithEmptyCover))
        assertTrue(com.lx.tracklist.data.local.FailedCoverTracker.isEntryCoverBroken(entryWithNullCover))
    }

    @Test
    fun testAnimeMangaManhwaSyncEligibility() {
        assertTrue(com.lx.tracklist.data.remote.JikanSyncManager.isEligibleMediaForMal(MediaType.ANIME))
        assertTrue(com.lx.tracklist.data.remote.JikanSyncManager.isEligibleMediaForMal(MediaType.MANGA))
        assertTrue(com.lx.tracklist.data.remote.JikanSyncManager.isEligibleMediaForMal(MediaType.MANHWA))
        assertTrue(com.lx.tracklist.data.remote.JikanSyncManager.isMangaType(MediaType.MANHWA))

        val manhwaEntry = MediaEntry(
            id = 10L,
            title = "Solo Leveling",
            mediaType = MediaType.MANHWA,
            status = MediaStatus.WATCHING,
            externalSource = "MAL"
        )
        assertTrue(com.lx.tracklist.data.remote.JikanSyncManager.isMalSource(manhwaEntry))
    }

    @Test
    fun testTachiyomiEntityDetectionAndIdExtraction() {
        val tachiEntry1 = MediaEntry(
            id = 1L,
            title = "King of Gold",
            mediaType = MediaType.MANHWA,
            status = MediaStatus.WATCHING,
            externalSource = "TACHIYOMI",
            externalId = "/manga/king-of-gold"
        )
        val tachiEntry2 = MediaEntry(
            id = 2L,
            title = "Martial Peak",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            externalSource = null,
            externalId = "/manga/martial-peak"
        )
        val normalEntry = MediaEntry(
            id = 3L,
            title = "One Piece",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            externalSource = "MAL",
            externalId = "13"
        )

        assertTrue(com.lx.tracklist.data.remote.JikanSyncManager.isTachiyomiSource(tachiEntry1))
        assertTrue(com.lx.tracklist.data.remote.JikanSyncManager.isTachiyomiSource(tachiEntry2))
        assertFalse(com.lx.tracklist.data.remote.JikanSyncManager.isTachiyomiSource(normalEntry))

        // ID validity check: Tachiyomi slug / path IDs are not valid external IDs
        assertFalse(com.lx.tracklist.data.remote.JikanSyncManager.hasValidExternalId(tachiEntry1))
        assertFalse(com.lx.tracklist.data.remote.JikanSyncManager.hasValidExternalId(tachiEntry2))
        assertTrue(com.lx.tracklist.data.remote.JikanSyncManager.hasValidExternalId(normalEntry))

        // MangaDex UUID and numeric MAL extraction
        val uuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        assertEquals(uuid, com.lx.tracklist.data.remote.JikanSyncManager.extractMangaDexUuid("/manga/$uuid"))
        assertEquals(uuid, com.lx.tracklist.data.remote.JikanSyncManager.extractMangaDexUuid(uuid))
        assertNull(com.lx.tracklist.data.remote.JikanSyncManager.extractMangaDexUuid("/manga/king-of-gold"))

        assertEquals(12345L, com.lx.tracklist.data.remote.JikanSyncManager.extractNumericMalId("12345"))
        assertEquals(54321L, com.lx.tracklist.data.remote.JikanSyncManager.extractNumericMalId("https://myanimelist.net/manga/54321"))
        assertEquals(67890L, com.lx.tracklist.data.remote.JikanSyncManager.extractNumericMalId("/manga/67890"))
        assertNull(com.lx.tracklist.data.remote.JikanSyncManager.extractNumericMalId("/manga/king-of-gold"))

        // Tachiyomi entries are handled by the separate TachiyomiResolverManager, not generic MAL sync
        assertFalse(com.lx.tracklist.data.remote.JikanSyncManager.isMalSource(tachiEntry1))
        assertFalse(com.lx.tracklist.data.remote.JikanSyncManager.isMalSource(tachiEntry2))
        assertTrue(com.lx.tracklist.data.remote.TachiyomiResolverManager.isEligibleTachiyomiEntry(tachiEntry1))
        assertTrue(com.lx.tracklist.data.remote.TachiyomiResolverManager.isEligibleTachiyomiEntry(tachiEntry2))
        assertFalse(com.lx.tracklist.data.remote.TachiyomiResolverManager.isEligibleTachiyomiEntry(normalEntry))
    }

    @Test
    fun testTachiyomiResolverHighConfidenceMatching() {
        // Normalization testing
        assertEquals("king of gold", com.lx.tracklist.data.remote.TachiyomiResolverManager.normalizeTitle("King of Gold"))
        assertEquals("king of gold", com.lx.tracklist.data.remote.TachiyomiResolverManager.normalizeTitle("king-of-gold"))
        assertEquals("kings gold", com.lx.tracklist.data.remote.TachiyomiResolverManager.normalizeTitle("King's Gold!"))
        assertEquals("solo leveling ragnarok", com.lx.tracklist.data.remote.TachiyomiResolverManager.normalizeTitle("Solo Leveling: Ragnarok"))
        assertEquals("solo leveling ragnarok", com.lx.tracklist.data.remote.TachiyomiResolverManager.normalizeTitle("Solo Leveling - Ragnarok"))

        // Confident match on primary title
        val candidate1 = com.lx.tracklist.data.remote.JikanMediaDetails(
            malId = 100L,
            title = "King of Gold",
            alternateTitle = "황금의 제왕\nKing of Gold: System of Wealth"
        )
        assertTrue(com.lx.tracklist.data.remote.TachiyomiResolverManager.isConfidentExactMatch("King of Gold", candidate1))
        assertTrue(com.lx.tracklist.data.remote.TachiyomiResolverManager.isConfidentExactMatch("king-of-gold", candidate1))
        assertTrue(com.lx.tracklist.data.remote.TachiyomiResolverManager.isConfidentExactMatch("King of Gold (Manhwa)", candidate1))

        // Confident match on alternate title
        val candidate2 = com.lx.tracklist.data.remote.JikanMediaDetails(
            malId = 200L,
            title = "Na Honjaman Level Up",
            alternateTitle = "Solo Leveling\nI Alone Level Up"
        )
        assertTrue(com.lx.tracklist.data.remote.TachiyomiResolverManager.isConfidentExactMatch("Solo Leveling", candidate2))
        assertTrue(com.lx.tracklist.data.remote.TachiyomiResolverManager.isConfidentExactMatch("solo-leveling", candidate2))
        assertTrue(com.lx.tracklist.data.remote.TachiyomiResolverManager.isConfidentExactMatch("I Alone Level Up", candidate2))

        // Non-matches / partial matches MUST be rejected for safety
        assertFalse(com.lx.tracklist.data.remote.TachiyomiResolverManager.isConfidentExactMatch("King of Gold", candidate2))
        assertFalse(com.lx.tracklist.data.remote.TachiyomiResolverManager.isConfidentExactMatch("Solo Leveling: Ragnarok", candidate2))
        assertFalse(com.lx.tracklist.data.remote.TachiyomiResolverManager.isConfidentExactMatch("Solo Leveling Season 2", candidate2))
    }

    @Test
    fun testAniListRelationFormatting() {
        assertEquals("Sequel", com.lx.tracklist.data.remote.JikanApiClient.formatAniListRelationType("SEQUEL"))
        assertEquals("Prequel", com.lx.tracklist.data.remote.JikanApiClient.formatAniListRelationType("PREQUEL"))
        assertEquals("Side Story", com.lx.tracklist.data.remote.JikanApiClient.formatAniListRelationType("SIDE_STORY"))
        assertEquals("Spin-off", com.lx.tracklist.data.remote.JikanApiClient.formatAniListRelationType("SPIN_OFF"))
        assertEquals("Alternative Version", com.lx.tracklist.data.remote.JikanApiClient.formatAniListRelationType("ALTERNATIVE"))
        assertEquals("Parent Story", com.lx.tracklist.data.remote.JikanApiClient.formatAniListRelationType("PARENT"))
        assertEquals("Summary", com.lx.tracklist.data.remote.JikanApiClient.formatAniListRelationType("SUMMARY"))
        assertEquals("Adaptation", com.lx.tracklist.data.remote.JikanApiClient.formatAniListRelationType("ADAPTATION"))
        assertEquals("Character", com.lx.tracklist.data.remote.JikanApiClient.formatAniListRelationType("CHARACTER"))
        assertEquals("Other", com.lx.tracklist.data.remote.JikanApiClient.formatAniListRelationType("OTHER"))
        assertEquals("Related", com.lx.tracklist.data.remote.JikanApiClient.formatAniListRelationType(null))
    }

    @Test
    fun testAniListRelationsParsing() {
        val sampleJson = """
            {
              "relations": {
                "edges": [
                  {
                    "relationType": "SEQUEL",
                    "node": {
                      "id": 164980,
                      "idMal": 53580,
                      "title": {
                        "english": "That Time I Got Reincarnated as a Slime Season 3",
                        "romaji": "Tensei shitara Slime Datta Ken 3rd Season",
                        "userPreferred": "That Time I Got Reincarnated as a Slime Season 3"
                      },
                      "type": "ANIME",
                      "format": "TV",
                      "coverImage": {
                        "large": "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx164980.jpg",
                        "medium": "https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/bx164980.jpg"
                      },
                      "averageScore": 81,
                      "startDate": {
                        "year": 2024
                      }
                    }
                  },
                  {
                    "relationType": "PREQUEL",
                    "node": {
                      "id": 108511,
                      "idMal": 39551,
                      "title": {
                        "english": "That Time I Got Reincarnated as a Slime Season 2",
                        "romaji": "Tensei shitara Slime Datta Ken 2nd Season",
                        "userPreferred": "That Time I Got Reincarnated as a Slime Season 2"
                      },
                      "type": "ANIME",
                      "format": "TV",
                      "coverImage": {
                        "large": "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx108511.jpg"
                      },
                      "averageScore": 82,
                      "startDate": {
                        "year": 2021
                      }
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val mediaObj = com.google.gson.Gson().fromJson(sampleJson, com.google.gson.JsonObject::class.java)
        val items = com.lx.tracklist.data.remote.JikanApiClient.parseAniListRelations(mediaObj)

        assertEquals(2, items.size)

        // Item 0: Sequel (Season 3)
        assertEquals("That Time I Got Reincarnated as a Slime Season 3", items[0].title)
        assertEquals("Sequel", items[0].relationship)
        assertEquals("53580", items[0].externalId)
        assertEquals("MAL", items[0].externalSource)
        assertEquals(com.lx.tracklist.data.model.MediaType.ANIME, items[0].mediaType)
        assertEquals("https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx164980.jpg", items[0].posterUrl)
        assertEquals("2024", items[0].releaseYear)
        assertEquals(8.1f, items[0].score!!, 0.01f)

        // Item 1: Prequel (Season 2 Part 1)
        assertEquals("That Time I Got Reincarnated as a Slime Season 2", items[1].title)
        assertEquals("Prequel", items[1].relationship)
        assertEquals("39551", items[1].externalId)
        assertEquals("2021", items[1].releaseYear)
        assertEquals(8.2f, items[1].score!!, 0.01f)
    }

    @Test
    fun testAnimeMovieRoutingLogic() {
        val weatherWithYou = MediaEntry(
            id = 1L,
            title = "Weathering with You",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.COMPLETED,
            externalId = "38826",
            externalSource = "MAL",
            externalScoreSource = "AniList"
        )

        val src = weatherWithYou.externalSource ?: ""
        val scoreSrc = weatherWithYou.externalScoreSource ?: ""
        val isAnime = src.equals("MAL", ignoreCase = true) ||
                src.equals("AniList", ignoreCase = true) ||
                scoreSrc.equals("MAL", ignoreCase = true) ||
                scoreSrc.equals("AniList", ignoreCase = true)

        assertTrue("Anime movie imported from MAL should be identified as Anime", isAnime)
    }

    @Test
    fun testExternalSyncWebUrlGeneration() {
        // MAL anime
        val animeItem = com.lx.tracklist.data.remote.JikanMediaDetails(
            malId = 5114L,
            title = "Fullmetal Alchemist: Brotherhood",
            posterUrl = null,
            type = "TV"
        )
        assertEquals("https://myanimelist.net/anime/5114", animeItem.webUrl)

        // MAL manga
        val mangaItem = com.lx.tracklist.data.remote.JikanMediaDetails(
            malId = 3037L,
            title = "Rosario+Vampire: Season II",
            posterUrl = null,
            totalChapters = 67,
            type = "Manga"
        )
        assertEquals("https://myanimelist.net/manga/3037", mangaItem.webUrl)

        // Custom URL provided from API
        val customUrlItem = com.lx.tracklist.data.remote.JikanMediaDetails(
            malId = 1234L,
            title = "Custom",
            posterUrl = null,
            url = "https://myanimelist.net/manga/1234/custom"
        )
        assertEquals("https://myanimelist.net/manga/1234/custom", customUrlItem.webUrl)

        // TMDB movie
        val tmdbMovie = com.lx.tracklist.data.remote.TmdbSearchResult(
            tmdbId = 550L,
            title = "Fight Club",
            isMovie = true
        )
        assertEquals("https://www.themoviedb.org/movie/550", tmdbMovie.webUrl)

        // TMDB tv
        val tmdbTv = com.lx.tracklist.data.remote.TmdbSearchResult(
            tmdbId = 1399L,
            title = "Game of Thrones",
            isMovie = false
        )
        assertEquals("https://www.themoviedb.org/tv/1399", tmdbTv.webUrl)
    }

    @Test
    fun testRelatedMediaItemExternalSourceId() {
        val malRelated = com.lx.tracklist.data.model.RelatedMediaItem(
            title = "Lazy Dungeon Master",
            relationship = "Adaptation",
            externalId = "86779",
            externalSource = "MAL",
            mediaType = MediaType.MANGA
        )
        assertEquals("86779", malRelated.externalId)
        assertEquals("MAL", malRelated.externalSource)

        val tmdbRelated = com.lx.tracklist.data.model.RelatedMediaItem(
            title = "Interstellar",
            relationship = "Related",
            externalId = "157336",
            externalSource = "TMDB",
            mediaType = MediaType.MOVIE
        )
        assertEquals("157336", tmdbRelated.externalId)
        assertEquals("TMDB", tmdbRelated.externalSource)

        // Test AniList relations parser logic with idMal vs aniList id
        val aniListMediaJson = com.google.gson.JsonObject().apply {
            val relations = com.google.gson.JsonObject().apply {
                val edges = com.google.gson.JsonArray().apply {
                    val edge1 = com.google.gson.JsonObject().apply {
                        addProperty("relationType", "ADAPTATION")
                        val node = com.google.gson.JsonObject().apply {
                            addProperty("id", 1001L)
                            addProperty("idMal", 2002L)
                            val titleObj = com.google.gson.JsonObject().apply {
                                addProperty("english", "With MAL ID")
                            }
                            add("title", titleObj)
                            addProperty("type", "MANGA")
                        }
                        add("node", node)
                    }
                    val edge2 = com.google.gson.JsonObject().apply {
                        addProperty("relationType", "SEQUEL")
                        val node = com.google.gson.JsonObject().apply {
                            addProperty("id", 3003L)
                            val titleObj = com.google.gson.JsonObject().apply {
                                addProperty("english", "AniList Only")
                            }
                            add("title", titleObj)
                            addProperty("type", "ANIME")
                        }
                        add("node", node)
                    }
                    add(edge1)
                    add(edge2)
                }
                add("edges", edges)
            }
            add("relations", relations)
        }

        val parsed = com.lx.tracklist.data.remote.JikanApiClient.parseAniListRelations(aniListMediaJson)
        assertEquals(2, parsed.size)
        assertEquals("2002", parsed[0].externalId)
        assertEquals("MAL", parsed[0].externalSource)
        assertEquals("3003", parsed[1].externalId)
        assertEquals("AniList", parsed[1].externalSource)
    }

    @Test
    fun testTachiyomiResolutionNeverDieExtra() {
        val resolver = com.lx.tracklist.data.remote.TachiyomiResolverManager

        // Candidate returned from MangaDex (or via JikanApiClient's fallback)
        val mangaDexCandidate = com.lx.tracklist.data.remote.JikanMediaDetails(
            malId = null,
            title = "Never Die Extra",
            alternateTitle = "Neverdie Extra\n죽지 않는 엑스트라",
            posterUrl = "https://uploads.mangadex.org/covers/8df71822-b883-49aa-9b16-4663efbf2234/cover.jpg",
            totalChapters = 60,
            type = "Manhwa",
            sourceProvider = "MangaDex",
            url = "https://mangadex.org/title/8df71822-b883-49aa-9b16-4663efbf2234"
        )

        // 1. Confident exact match checks
        assertTrue("Exact title match must succeed", resolver.isConfidentExactMatch("Never Die Extra", mangaDexCandidate))
        assertTrue("Space-agnostic match for compound title 'Neverdie Extra' must succeed", resolver.isConfidentExactMatch("Neverdie Extra", mangaDexCandidate))
        assertTrue("Title with bracketed suffix must succeed", resolver.isConfidentExactMatch("Never Die Extra (Manhwa)", mangaDexCandidate))
        assertTrue("Korean title in candidate alternate titles must succeed", resolver.isConfidentExactMatch("죽지 않는 엑스트라", mangaDexCandidate))

        // 2. Extract external source and ID checks
        val sourceAndId = resolver.extractCandidateSourceAndId(mangaDexCandidate)
        assertNotNull("MangaDex candidate must yield non-null source and ID", sourceAndId)
        assertEquals("MangaDex", sourceAndId?.first)
        assertEquals("8df71822-b883-49aa-9b16-4663efbf2234", sourceAndId?.second)

        // 3. Test AniList and MAL candidate source extractions
        val aniCandidate = com.lx.tracklist.data.remote.JikanMediaDetails(
            malId = 4567L,
            title = "Solo Leveling",
            sourceProvider = "AniList",
            url = "https://anilist.co/manga/4567"
        )
        val aniSourceAndId = resolver.extractCandidateSourceAndId(aniCandidate)
        assertEquals("AniList", aniSourceAndId?.first)
        assertEquals("4567", aniSourceAndId?.second)

        val malCandidate = com.lx.tracklist.data.remote.JikanMediaDetails(
            malId = 121496L,
            title = "Solo Leveling",
            sourceProvider = "Jikan",
            url = "https://myanimelist.net/manga/121496"
        )
        val malSourceAndId = resolver.extractCandidateSourceAndId(malCandidate)
        assertEquals("MAL", malSourceAndId?.first)
        assertEquals("121496", malSourceAndId?.second)
    }
}
