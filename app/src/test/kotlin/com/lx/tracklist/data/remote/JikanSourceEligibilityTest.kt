package com.lx.tracklist.data.remote

import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JikanSourceEligibilityTest {

    @Test
    fun `GAME and MUSIC are never claimed by JikanSyncManager regardless of externalId or source`() {
        val gameNumericUnlinked = MediaEntry(
            title = "Elden Ring",
            mediaType = MediaType.GAME,
            status = MediaStatus.COMPLETED,
            externalId = "123",
            externalSource = null
        )
        val gameIgdb = gameNumericUnlinked.copy(externalSource = "IGDB")
        val musicNumeric = MediaEntry(
            title = "Nights",
            mediaType = MediaType.MUSIC,
            status = MediaStatus.COMPLETED,
            externalId = "12345",
            externalSource = null
        )
        val musicApple = musicNumeric.copy(externalSource = "Apple Music")
        val musicMb = musicNumeric.copy(externalId = "mbid-abc", externalSource = "MusicBrainz")

        assertFalse(JikanSyncManager.isMalSource(gameNumericUnlinked))
        assertFalse(JikanSyncManager.isMalSource(gameIgdb))
        assertFalse(JikanSyncManager.isMalSource(musicNumeric))
        assertFalse(JikanSyncManager.isMalSource(musicApple))
        assertFalse(JikanSyncManager.isMalSource(musicMb))
    }

    @Test
    fun `MOVIE and SHOW are claimed by JikanSyncManager only when canonical source is MAL or AniList`() {
        val movieNumericNoSource = MediaEntry(
            title = "Inception",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.COMPLETED,
            externalId = "27205",
            externalSource = null
        )
        val showTmdb = MediaEntry(
            title = "Breaking Bad",
            mediaType = MediaType.SHOW,
            status = MediaStatus.COMPLETED,
            externalId = "1396",
            externalSource = "TMDB"
        )
        val movieMal = movieNumericNoSource.copy(externalId = "32281", externalSource = "MyAnimeList")
        val showAniList = showTmdb.copy(externalId = "999", externalSource = "AniList")

        assertFalse(JikanSyncManager.isMalSource(movieNumericNoSource))
        assertFalse(JikanSyncManager.isMalSource(showTmdb))
        assertTrue(JikanSyncManager.isMalSource(movieMal))
        assertTrue(JikanSyncManager.isMalSource(showAniList))
    }
}
