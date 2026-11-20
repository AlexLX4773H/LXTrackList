package com.lx.tracklist

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
    fun testFranchiseProgressWithSkippedGames() {
        // 4 games in franchise:
        // Game 1: Skipped
        // Game 2: Completed (100%)
        // Game 3: Skipped
        // Game 4: Playing at 50%
        val entries = listOf(
            GameSeriesItem(
                id = "1",
                order = 1,
                title = "The Witcher",
                status = GameSeriesItemStatus.SKIPPED
            ),
            GameSeriesItem(
                id = "2",
                order = 2,
                title = "The Witcher 2: Assassins of Kings",
                status = GameSeriesItemStatus.COMPLETED,
                progressPercent = 100
            ),
            GameSeriesItem(
                id = "3",
                order = 3,
                title = "The Witcher Adventure Game",
                status = GameSeriesItemStatus.SKIPPED
            ),
            GameSeriesItem(
                id = "4",
                order = 4,
                title = "The Witcher 3: Wild Hunt",
                status = GameSeriesItemStatus.PLAYING,
                progressPercent = 50
            )
        )

        val franchise = MediaEntry(
            title = "The Witcher Series",
            mediaType = MediaType.GAME,
            status = MediaStatus.WATCHING,
            isGameFranchise = true,
            gameSeriesEntriesJson = MediaEntry.formatGameSeriesEntries(entries)
        )

        // Active non-skipped count = 2 (Game 2 and Game 4).
        // Completed count = 1.
        // Skipped count = 2.
        val (completed, active, skipped) = franchise.getFranchiseProgressSummary()
        assertEquals(1, completed)
        assertEquals(2, active)
        assertEquals(2, skipped)

        // Display string format: "1/2 games (2 skipped)"
        assertEquals("1/2 games (2 skipped)", franchise.displayProgress)

        // Progress fraction: (1.0 + 0.5) / 2 = 0.75f
        assertEquals(0.75f, franchise.progressFraction, 0.001f)
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
        assertEquals("PC (Steam)", platforms[0])
        assertEquals("PS5", platforms[1])
        assertEquals("Xbox Series X|S", platforms[2])

        val formatted = MediaEntry.formatGamePlatforms(listOf("PC (Steam)", "Nintendo Switch"))
        assertEquals("PC (Steam), Nintendo Switch", formatted)
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
}
