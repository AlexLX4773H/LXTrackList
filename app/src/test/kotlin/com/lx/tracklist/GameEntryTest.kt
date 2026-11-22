package com.lx.tracklist

import com.lx.tracklist.data.exporter.JsonExporter
import com.lx.tracklist.data.importer.JsonImporter
import com.lx.tracklist.data.model.GameSeriesItem
import com.lx.tracklist.data.model.GameSeriesItemStatus
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.TrackingUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEntryTest {

    @Test
    fun testMediaTypeAndStatusEnums() {
        assertEquals(MediaType.GAME, MediaType.fromString("GAME"))
        assertEquals(MediaType.GAME, MediaType.fromString("games"))
        assertEquals(MediaType.GAME, MediaType.fromString("Video Game"))
        assertEquals(MediaType.GAME, MediaType.fromString("video games"))
        assertTrue(MediaType.GAME.isPlayType)
        assertFalse(MediaType.GAME.isWatchType)
        assertFalse(MediaType.GAME.isReadType)

        assertEquals("Playing", MediaStatus.WATCHING.getDisplayLabel(MediaType.GAME))
        assertEquals("Plan to Play", MediaStatus.PLAN_TO_WATCH.getDisplayLabel(MediaType.GAME))
        assertEquals("Completed", MediaStatus.COMPLETED.getDisplayLabel(MediaType.GAME))
        assertEquals("On Hold", MediaStatus.ON_HOLD.getDisplayLabel(MediaType.GAME))
        assertEquals("Dropped", MediaStatus.DROPPED.getDisplayLabel(MediaType.GAME))

        assertEquals(MediaStatus.WATCHING, MediaStatus.fromString("PLAYING"))
        assertEquals(MediaStatus.PLAN_TO_WATCH, MediaStatus.fromString("PLAN_TO_PLAY"))
        assertEquals(MediaStatus.PLAN_TO_WATCH, MediaStatus.fromString("plan to play"))

        assertEquals(TrackingUnit.PERCENTAGE, TrackingUnit.fromString("PERCENTAGE"))
        assertEquals(TrackingUnit.CUSTOM, TrackingUnit.fromString("CUSTOM"))
    }

    @Test
    fun testStandalonePercentageIncrementDecrementAndAutoComplete() {
        val entry = MediaEntry(
            title = "Hollow Knight",
            mediaType = MediaType.GAME,
            status = MediaStatus.WATCHING,
            trackingUnit = TrackingUnit.PERCENTAGE,
            gameProgressPercent = 85
        )

        assertEquals("85%", entry.displayProgress)
        assertEquals(0.85f, entry.progressFraction, 0.001f)

        // Increment by 5%
        val step1 = entry.incrementProgress()
        assertEquals(90, step1.gameProgressPercent)
        assertEquals("90%", step1.displayProgress)
        assertEquals(MediaStatus.WATCHING, step1.status)

        // Increment to 95%
        val step2 = step1.incrementProgress()
        assertEquals(95, step2.gameProgressPercent)
        assertEquals(MediaStatus.WATCHING, step2.status)

        // Increment to 100% -> auto-completes
        val step3 = step2.incrementProgress()
        assertEquals(100, step3.gameProgressPercent)
        assertEquals("100%", step3.displayProgress)
        assertEquals(MediaStatus.COMPLETED, step3.status)
        assertNotNull(step3.finishDate)

        // Clamps at 100%
        val step4 = step3.incrementProgress()
        assertEquals(100, step4.gameProgressPercent)
        assertEquals(MediaStatus.COMPLETED, step4.status)

        // Decrement by 5% -> sets status back from COMPLETED to WATCHING (Playing)
        val dec1 = step4.decrementProgress()
        assertEquals(95, dec1.gameProgressPercent)
        assertEquals(MediaStatus.WATCHING, dec1.status)

        // Clamps at 0%
        val lowEntry = entry.copy(gameProgressPercent = 3)
        val decZero = lowEntry.decrementProgress()
        assertEquals(0, decZero.gameProgressPercent)
    }

    @Test
    fun testStandaloneCustomUnits() {
        // 1. Levels mode with total target (e.g., 12/20 Levels)
        val levelGame = MediaEntry(
            title = "Super Mario Odyssey",
            mediaType = MediaType.GAME,
            status = MediaStatus.WATCHING,
            trackingUnit = TrackingUnit.CUSTOM,
            customUnitLabel = "Levels",
            customUnitCurrent = 12,
            customUnitTotal = 20
        )
        assertEquals("12/20 Levels", levelGame.displayProgress)
        assertEquals(12f / 20f, levelGame.progressFraction, 0.001f)

        val nextLevel = levelGame.incrementProgress()
        assertEquals(13, nextLevel.customUnitCurrent)
        assertEquals("13/20 Levels", nextLevel.displayProgress)
        assertEquals(MediaStatus.WATCHING, nextLevel.status)

        // Increment until complete
        var completing = levelGame.copy(customUnitCurrent = 19)
        completing = completing.incrementProgress()
        assertEquals(20, completing.customUnitCurrent)
        assertEquals(MediaStatus.COMPLETED, completing.status)
        assertNotNull(completing.finishDate)

        // Decrement sets back to WATCHING
        val decLevel = completing.decrementProgress()
        assertEquals(19, decLevel.customUnitCurrent)
        assertEquals(MediaStatus.WATCHING, decLevel.status)

        // 2. Cities mode
        val civGame = MediaEntry(
            title = "Civilization VI",
            mediaType = MediaType.GAME,
            status = MediaStatus.WATCHING,
            trackingUnit = TrackingUnit.CUSTOM,
            customUnitLabel = "Cities",
            customUnitCurrent = 4,
            customUnitTotal = 9
        )
        assertEquals("4/9 Cities", civGame.displayProgress)
        assertEquals(4f / 9f, civGame.progressFraction, 0.001f)

        // 3. Open-ended Hours mode (customUnitTotal = 0)
        val hoursGame = MediaEntry(
            title = "Elden Ring",
            mediaType = MediaType.GAME,
            status = MediaStatus.WATCHING,
            trackingUnit = TrackingUnit.CUSTOM,
            customUnitLabel = "Hours",
            customUnitCurrent = 42,
            customUnitTotal = 0
        )
        assertEquals("42 Hours", hoursGame.displayProgress)
        assertEquals(0f, hoursGame.progressFraction, 0.001f)

        val nextHour = hoursGame.incrementProgress()
        assertEquals(43, nextHour.customUnitCurrent)
        assertEquals("43 Hours", nextHour.displayProgress)
        assertEquals(MediaStatus.WATCHING, nextHour.status)

        val prevHour = hoursGame.decrementProgress()
        assertEquals(41, prevHour.customUnitCurrent)
        assertEquals("41 Hours", prevHour.displayProgress)
    }

    @Test
    fun testStandaloneGameAlwaysStandalone() {
        val entry = MediaEntry(
            title = "The Witcher 3: Wild Hunt",
            mediaType = MediaType.GAME,
            status = MediaStatus.WATCHING,
            trackingUnit = TrackingUnit.PERCENTAGE,
            gameProgressPercent = 50,
            gamePlatforms = "PC (Steam), PS5, Nintendo Switch"
        )

        assertFalse(entry.isGameFranchise)
        assertEquals("50%", entry.displayProgress)
        assertEquals(0.5f, entry.progressFraction, 0.001f)
    }

    @Test
    fun testPlatformsHelper() {
        val entry = MediaEntry(
            title = "Cyberpunk 2077",
            mediaType = MediaType.GAME,
            status = MediaStatus.WATCHING,
            gamePlatforms = "PC (Steam), PS5, Xbox Series X|S"
        )

        val platforms = entry.getGamePlatformList()
        assertEquals(3, platforms.size)
        assertEquals("PC", platforms[0])
        assertEquals("PS", platforms[1])
        assertEquals("XBox", platforms[2])

        val formatted = MediaEntry.formatGamePlatforms(listOf("PC (Steam)", "Nintendo Switch"))
        assertEquals("PC, Switch", formatted)
    }

    @Test
    fun testUnreleasedGameBadge() {
        val futureTime = System.currentTimeMillis() + 100_000_000L
        val unreleased = MediaEntry(
            title = "Grand Theft Auto VI",
            mediaType = MediaType.GAME,
            status = MediaStatus.PLAN_TO_WATCH,
            airedStartDate = futureTime
        )

        assertTrue(unreleased.isNotYetAiredOrPublished)

        val pastTime = System.currentTimeMillis() - 100_000_000L
        val released = MediaEntry(
            title = "Grand Theft Auto V",
            mediaType = MediaType.GAME,
            status = MediaStatus.COMPLETED,
            airedStartDate = pastTime
        )

        assertFalse(released.isNotYetAiredOrPublished)
    }

    @Test
    fun testJsonExportImportRoundTripForGames() {
        val seriesItems = listOf(
            GameSeriesItem(id = "101", order = 1, title = "Dark Souls", status = GameSeriesItemStatus.COMPLETED, releaseYear = "2011"),
            GameSeriesItem(id = "102", order = 2, title = "Dark Souls II", status = GameSeriesItemStatus.SKIPPED, releaseYear = "2014"),
            GameSeriesItem(id = "103", order = 3, title = "Dark Souls III", status = GameSeriesItemStatus.PLAYING, releaseYear = "2016")
        )
        val seriesJson = MediaEntry.formatGameSeriesEntries(seriesItems)

        val franchiseEntry = MediaEntry(
            id = 1L,
            title = "Dark Souls Series",
            mediaType = MediaType.GAME,
            status = MediaStatus.WATCHING,
            gamePlatforms = "PC (Steam), PlayStation 4, Xbox One",
            isGameFranchise = true,
            gameSeriesEntriesJson = seriesJson,
            trackingUnit = TrackingUnit.PERCENTAGE,
            gameProgressPercent = 33
        )

        val customUnitEntry = MediaEntry(
            id = 2L,
            title = "Elden Ring",
            mediaType = MediaType.GAME,
            status = MediaStatus.WATCHING,
            gamePlatforms = "PC (Steam)",
            isGameFranchise = false,
            trackingUnit = TrackingUnit.CUSTOM,
            customUnitLabel = "Hours",
            customUnitCurrent = 120,
            customUnitTotal = 150
        )

        val originalEntries = listOf(franchiseEntry, customUnitEntry)

        // 1. Export to JSON
        val exportedJson = JsonExporter.exportToJson(originalEntries)
        assertTrue(exportedJson.contains("Dark Souls Series"))
        assertTrue(exportedJson.contains("Elden Ring"))
        assertTrue(exportedJson.contains("PC (Steam)"))
        assertTrue(exportedJson.contains("gamePlatforms"))
        assertTrue(exportedJson.contains("isGameFranchise"))

        // 2. Import from JSON stream
        val importedEntries = JsonImporter.parse(exportedJson.byteInputStream(Charsets.UTF_8))
        assertEquals(2, importedEntries.size)

        // Verify franchise entry
        val importedFranchise = importedEntries.first { it.id == 1L }
        assertEquals("Dark Souls Series", importedFranchise.title)
        assertEquals(MediaType.GAME, importedFranchise.mediaType)
        assertEquals(MediaStatus.WATCHING, importedFranchise.status)
        assertTrue(importedFranchise.isGameFranchise)
        assertEquals("PC (Steam), PlayStation 4, Xbox One", importedFranchise.gamePlatforms)
        assertEquals(33, importedFranchise.gameProgressPercent)
        val parsedSeries = importedFranchise.getGameSeriesEntries()
        assertEquals(3, parsedSeries.size)
        assertEquals(GameSeriesItemStatus.COMPLETED, parsedSeries[0].status)
        assertEquals(GameSeriesItemStatus.SKIPPED, parsedSeries[1].status)
        assertEquals(GameSeriesItemStatus.PLAYING, parsedSeries[2].status)

        // Verify custom units entry
        val importedCustom = importedEntries.first { it.id == 2L }
        assertEquals("Elden Ring", importedCustom.title)
        assertEquals(MediaType.GAME, importedCustom.mediaType)
        assertFalse(importedCustom.isGameFranchise)
        assertEquals("PC (Steam)", importedCustom.gamePlatforms)
        assertEquals(TrackingUnit.CUSTOM, importedCustom.trackingUnit)
        assertEquals("Hours", importedCustom.customUnitLabel)
        assertEquals(120, importedCustom.customUnitCurrent)
        assertEquals(150, importedCustom.customUnitTotal)
    }

    @Test
    fun testBackwardCompatibleJsonImportWithoutGameFields() {
        // Legacy JSON without any of the new Room v18 game fields
        val legacyJson = """
            {
                "app": "LX Track List",
                "version": 1,
                "exportedAt": 1700000000000,
                "totalCount": 1,
                "entries": [
                    {
                        "id": 99,
                        "title": "Legacy Anime",
                        "mediaType": "ANIME",
                        "status": "COMPLETED",
                        "currentEpisodes": 12,
                        "totalEpisodes": 12
                    }
                ]
            }
        """.trimIndent()

        val imported = JsonImporter.parse(legacyJson.byteInputStream(Charsets.UTF_8))
        assertEquals(1, imported.size)
        val entry = imported[0]
        assertEquals("Legacy Anime", entry.title)
        assertEquals(MediaType.ANIME, entry.mediaType)
        assertEquals(12, entry.currentEpisodes)
        // Verify default values for new game fields
        assertEquals(null, entry.gamePlatforms)
        assertFalse(entry.isGameFranchise)
        assertEquals(null, entry.gameSeriesEntriesJson)
        assertEquals(0, entry.gameProgressPercent)
        assertEquals(null, entry.customUnitLabel)
        assertEquals(0, entry.customUnitCurrent)
        assertEquals(0, entry.customUnitTotal)
    }

    @Test
    fun testGameTagRegistry() {
        val genres = com.lx.tracklist.ui.search.GameTagRegistry.gameGenres
        assertTrue(genres.isNotEmpty())
        assertTrue(genres.any { it.name.equals("Role-playing (RPG)", ignoreCase = true) })
        assertTrue(genres.any { it.name.equals("Action", ignoreCase = true) })

        val filtered = com.lx.tracklist.ui.search.GameTagRegistry.filterGameGenres("role")
        assertEquals(1, filtered.size)
        assertEquals("Role-playing (RPG)", filtered[0].name)

        val nonExistent = com.lx.tracklist.ui.search.GameTagRegistry.filterGameGenres("xyznonexistent")
        assertTrue(nonExistent.isEmpty())
    }

    @Test
    fun testGameCompletionStatsCalculation() {
        // 1. 100% Completed Game with average completion time of 40 hours (2400 minutes)
        val completedGame = MediaEntry(
            title = "Elden Ring",
            mediaType = MediaType.GAME,
            status = MediaStatus.COMPLETED,
            gameProgressPercent = 100,
            durationMinutes = 2400 // 40 hours
        )

        val summaryCompleted = com.lx.tracklist.ui.stats.calculateCategoryStatSummary(
            entries = listOf(completedGame),
            categoryKey = "GAME",
            customCategories = emptyList()
        )

        // 2400 min / 1440 min/day = 1.6666... days (40 hours)
        assertEquals(1.667, summaryCompleted.daysSpent, 0.01)
        assertEquals(0.0, summaryCompleted.daysRemaining, 0.01)
        assertTrue(summaryCompleted.isGame)

        // 2. In-Progress Game at 50% progress with 40-hour average
        val inProgressGame = MediaEntry(
            title = "Cyberpunk 2077",
            mediaType = MediaType.GAME,
            status = MediaStatus.WATCHING,
            gameProgressPercent = 50,
            durationMinutes = 2400
        )

        val summaryInProgress = com.lx.tracklist.ui.stats.calculateCategoryStatSummary(
            entries = listOf(inProgressGame),
            categoryKey = "GAME",
            customCategories = emptyList()
        )

        // 50% of 2400 = 1200 min spent (0.833 days / 20 hours), 1200 min remaining
        assertEquals(0.833, summaryInProgress.daysSpent, 0.01)
        assertEquals(0.833, summaryInProgress.daysRemaining, 0.01)

        // 3. Replayed Game: Completed + 1 replay = 2 full playthroughs (80 hours = 4800 minutes)
        val replayedGame = completedGame.copy(rewatchCount = 1)
        val summaryReplayed = com.lx.tracklist.ui.stats.calculateCategoryStatSummary(
            entries = listOf(replayedGame),
            categoryKey = "GAME",
            customCategories = emptyList()
        )
        // 4800 / 1440 = 3.333 days (80 hours)
        assertEquals(3.333, summaryReplayed.daysSpent, 0.01)

        // 4. User manual "Hours" override: user explicitly logged 60 hours
        val manualHoursGame = completedGame.copy(
            customUnitLabel = "Hours",
            customUnitCurrent = 60
        )
        val summaryManual = com.lx.tracklist.ui.stats.calculateCategoryStatSummary(
            entries = listOf(manualHoursGame),
            categoryKey = "GAME",
            customCategories = emptyList()
        )
        // 60 hours * 60 min = 3600 min -> 3600 / 1440 = 2.5 days
        assertEquals(2.5, summaryManual.daysSpent, 0.01)
    }
}
