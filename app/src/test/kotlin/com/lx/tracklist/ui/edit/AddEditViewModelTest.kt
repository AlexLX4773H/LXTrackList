package com.lx.tracklist.ui.edit

import com.lx.tracklist.data.model.ExternalSources
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.remote.SyncJob
import com.lx.tracklist.data.remote.SyncRouter
import com.lx.tracklist.data.remote.TmdbSearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddEditViewModelTest {

    @Test
    fun `applyEdits preserves non-form fields when saving an existing entry`() {
        val original = MediaEntry(
            id = 42L,
            userId = 2L,
            title = "Old Title",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_100_000L,
            broadcastTime = "Sundays at 17:30 (JST)",
            nextAiringEpoch = 1_800_000_000_000L,
            relatedEntriesJson = "[{\"title\":\"Sequel\",\"externalId\":\"999\",\"externalSource\":\"MAL\"}]",
            isGameFranchise = true,
            gameSeriesEntriesJson = "[{\"title\":\"DLC\",\"status\":\"COMPLETED\"}]",
            lastMetadataSyncAt = 1_750_000_000_000L,
            startDate = 1_700_000_000_000L
        )

        val stateWithNewTitle = AddEditUiState(
            id = 42L,
            userId = 2L,
            title = "New Title",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            startDate = original.startDate
        )

        val edited = applyEdits(original, stateWithNewTitle, activeUserId = 2L, now = 1_760_000_000_000L)

        assertEquals(42L, edited.id)
        assertEquals("New Title", edited.title)
        assertEquals(1_760_000_000_000L, edited.updatedAt)

        // Preserved metadata that is not editable on the Add/Edit form
        assertEquals(original.createdAt, edited.createdAt)
        assertEquals(original.broadcastTime, edited.broadcastTime)
        assertEquals(original.nextAiringEpoch, edited.nextAiringEpoch)
        assertEquals(original.relatedEntriesJson, edited.relatedEntriesJson)
        assertEquals(original.isGameFranchise, edited.isGameFranchise)
        assertEquals(original.gameSeriesEntriesJson, edited.gameSeriesEntriesJson)
        assertEquals(original.lastMetadataSyncAt, edited.lastMetadataSyncAt)
    }

    @Test
    fun `applyEdits builds a new entry when original is null`() {
        val state = AddEditUiState(
            id = 0L,
            userId = 1L,
            title = "New Game",
            mediaType = MediaType.GAME,
            status = MediaStatus.PLAN_TO_WATCH,
            gamePlatforms = "PC, Steam"
        )

        val created = applyEdits(original = null, s = state, activeUserId = 1L, now = 1_760_000_000_000L)

        assertEquals(0L, created.id)
        assertEquals("New Game", created.title)
        assertEquals(MediaType.GAME, created.mediaType)
        assertEquals(false, created.isGameFranchise)
        assertEquals(null, created.gameSeriesEntriesJson)
    }

    @Test
    fun `applyTmdbSelection keeps ANIME as ANIME with TMDB source and only re-types MOVIE and SHOW`() {
        val tmdbTvResult = TmdbSearchResult(
            tmdbId = 209867L,
            title = "Frieren: Beyond Journey's End",
            isMovie = false
        )
        val tmdbMovieResult = TmdbSearchResult(
            tmdbId = 129L,
            title = "Spirited Away",
            isMovie = true
        )

        val animeState = AddEditUiState(title = "Frieren", mediaType = MediaType.ANIME)
        val updatedAnimeTv = applyTmdbSelection(animeState, tmdbTvResult, fullDetails = null)
        assertEquals(MediaType.ANIME, updatedAnimeTv.mediaType)
        assertEquals(ExternalSources.TMDB, updatedAnimeTv.externalSource)
        assertEquals("209867", updatedAnimeTv.externalId)
        val savedAnime = applyEdits(null, updatedAnimeTv, activeUserId = 1L, now = 1L)
        assertEquals(SyncJob.TMDB, SyncRouter.jobFor(savedAnime))

        val animeMovieState = AddEditUiState(title = "Spirited Away", mediaType = MediaType.ANIME)
        val updatedAnimeMovie = applyTmdbSelection(animeMovieState, tmdbMovieResult, fullDetails = null)
        assertEquals(MediaType.ANIME, updatedAnimeMovie.mediaType)
        assertEquals(ExternalSources.TMDB, updatedAnimeMovie.externalSource)

        val movieTurnedShow = applyTmdbSelection(
            AddEditUiState(title = "Fargo", mediaType = MediaType.MOVIE),
            tmdbTvResult,
            fullDetails = null
        )
        assertEquals(MediaType.SHOW, movieTurnedShow.mediaType)
    }

    @Test
    fun `BUG-16 music duration parser accepts m ss and seconds and rejects seconds outside 0-59`() {
        val ms = AddEditInputParsers.parseMusicDurationSeconds("3:05")
        assertTrue(ms.isValid)
        assertEquals(185, ms.value)
        assertEquals("3:05", AddEditInputParsers.formatMusicDurationSeconds(185))

        val plainSecs = AddEditInputParsers.parseMusicDurationSeconds("225")
        assertTrue(plainSecs.isValid)
        assertEquals(225, plainSecs.value)

        val invalidSecs = AddEditInputParsers.parseMusicDurationSeconds("3:75")
        assertFalse(invalidSecs.isValid)
        assertNotNull(invalidSecs.error)

        val cleared = AddEditInputParsers.parseMusicDurationSeconds("")
        assertTrue(cleared.isValid)
        assertNull(cleared.value)
    }

    @Test
    fun `BUG-16 playback parser handles plain minutes mm ss and 1h 15m accurately`() {
        val hoursAndMins = AddEditInputParsers.parsePlaybackTimeMinutes("1h 15m")
        assertTrue(hoursAndMins.isValid)
        assertEquals(75, hoursAndMins.value)

        val mmSs = AddEditInputParsers.parsePlaybackTimeMinutes("12:30")
        assertTrue(mmSs.isValid)
        assertEquals(13, mmSs.value)

        val hMmSs = AddEditInputParsers.parsePlaybackTimeMinutes("1:15:10")
        assertTrue(hMmSs.isValid)
        assertEquals(75, hMmSs.value)

        val invalidSec = AddEditInputParsers.parsePlaybackTimeMinutes("12:75")
        assertFalse(invalidSec.isValid)
        assertNotNull(invalidSec.error)

        val garbage = AddEditInputParsers.parsePlaybackTimeMinutes("1h abc")
        assertFalse(garbage.isValid)
        assertNotNull(garbage.error)
    }

    @Test
    fun `BUG-16 game hours parser handles decimals without truncation and formats cleanly`() {
        val tenAndHalf = AddEditInputParsers.parseGameHoursToMinutes("10.5")
        assertTrue(tenAndHalf.isValid)
        assertEquals(630, tenAndHalf.value)
        assertEquals("10.5", AddEditInputParsers.formatMinutesAsHours(630))

        // Avoids floating point truncation like 1.2 * 60 = 71.9999 -> 71
        val onePointTwo = AddEditInputParsers.parseGameHoursToMinutes("1.2")
        assertTrue(onePointTwo.isValid)
        assertEquals(72, onePointTwo.value)
        assertEquals("1.2", AddEditInputParsers.formatMinutesAsHours(72))

        val zero = AddEditInputParsers.parseGameHoursToMinutes("0")
        assertTrue(zero.isValid)
        assertNull(zero.value)

        val negative = AddEditInputParsers.parseGameHoursToMinutes("-2")
        assertFalse(negative.isValid)
        assertNotNull(negative.error)
    }

    @Test
    fun `BUG-16 external score parser respects source scales preserves 0 and precision`() {
        val malPrecision = AddEditInputParsers.parseExternalScore("8.25", "MAL")
        assertTrue(malPrecision.isValid)
        assertEquals(8.25f, malPrecision.value)
        assertEquals("8.25", AddEditInputParsers.formatEditableScore(malPrecision.value))

        val zeroScore = AddEditInputParsers.parseExternalScore("0", "TMDB")
        assertTrue(zeroScore.isValid)
        assertEquals(0f, zeroScore.value)
        assertEquals("0", AddEditInputParsers.formatEditableScore(0f))

        val outOfMalRange = AddEditInputParsers.parseExternalScore("85", "MAL")
        assertFalse(outOfMalRange.isValid)
        assertNotNull(outOfMalRange.error)

        val validMetacritic = AddEditInputParsers.parseExternalScore("85", "Metacritic")
        assertTrue(validMetacritic.isValid)
        assertEquals(85f, validMetacritic.value)

        val overHundred = AddEditInputParsers.parseExternalScore("105", "Rotten Tomatoes")
        assertFalse(overHundred.isValid)
        assertNotNull(overHundred.error)
    }

    @Test
    fun `reading types restrict units and coerce legacy Percentage or Custom to Chapters on save`() {
        assertEquals(
            listOf(TrackingUnit.CHAPTERS, TrackingUnit.VOLUMES, TrackingUnit.PAGES, TrackingUnit.COMBINED),
            TrackingUnit.READING_UNITS
        )
        assertEquals(TrackingUnit.CHAPTERS, TrackingUnit.sanitizeForMediaType(TrackingUnit.PERCENTAGE, MediaType.MANGA))
        assertEquals(TrackingUnit.CHAPTERS, TrackingUnit.sanitizeForMediaType(TrackingUnit.CUSTOM, MediaType.MANHWA))
        assertEquals(TrackingUnit.CHAPTERS, TrackingUnit.sanitizeForMediaType(TrackingUnit.PERCENTAGE, MediaType.BOOK))
        assertEquals(TrackingUnit.PAGES, TrackingUnit.sanitizeForMediaType(TrackingUnit.PAGES, MediaType.BOOK))
        assertEquals(TrackingUnit.PERCENTAGE, TrackingUnit.sanitizeForMediaType(TrackingUnit.PERCENTAGE, MediaType.GAME))
        assertEquals(TrackingUnit.CUSTOM, TrackingUnit.sanitizeForMediaType(TrackingUnit.CUSTOM, MediaType.GAME))

        val original = MediaEntry(
            id = 88L,
            userId = 1L,
            title = "Legacy Manga",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            trackingUnit = TrackingUnit.PERCENTAGE
        )
        val state = AddEditUiState(
            id = 88L,
            title = "Legacy Manga",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            trackingUnit = TrackingUnit.PERCENTAGE
        )
        val savedExisting = applyEdits(original, state, activeUserId = 1L, now = 2_000L)
        assertEquals(TrackingUnit.CHAPTERS, savedExisting.trackingUnit)

        val newBookState = AddEditUiState(
            title = "Legacy Custom Book",
            mediaType = MediaType.BOOK,
            status = MediaStatus.WATCHING,
            trackingUnit = TrackingUnit.CUSTOM
        )
        val savedNew = applyEdits(null, newBookState, activeUserId = 1L, now = 2_000L)
        assertEquals(TrackingUnit.CHAPTERS, savedNew.trackingUnit)
    }
}
