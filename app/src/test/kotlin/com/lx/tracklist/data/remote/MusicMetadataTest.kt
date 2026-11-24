package com.lx.tracklist.data.remote

import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.ui.search.MusicTagRegistry
import com.lx.tracklist.util.ExternalLinkHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicMetadataTest {

    @Test
    fun testMusicSearchResultToMediaEntry() {
        val result = MusicSearchResult(
            externalId = "rec_12345",
            externalSource = "MusicBrainz",
            title = "Starboy",
            artist = "The Weeknd ft. Daft Punk",
            albumName = "Starboy",
            trackNumber = 1,
            durationSeconds = 230,
            releaseYear = "2016",
            releaseDateMillis = 1479772800000L,
            coverUrl = "https://coverartarchive.org/release/123/front-500",
            genres = listOf("Pop", "R&B"),
            musicAlbumId = "rg_67890",
            webUrl = "https://musicbrainz.org/recording/rec_12345"
        )

        val entry = result.toMediaEntry(userId = 42L, status = MediaStatus.PLAN_TO_WATCH)

        assertEquals(42L, entry.userId)
        assertEquals("Starboy", entry.title)
        assertEquals(MediaType.MUSIC, entry.mediaType)
        assertEquals(MediaStatus.PLAN_TO_WATCH, entry.status)
        assertEquals("The Weeknd ft. Daft Punk", entry.creator)
        assertEquals("The Weeknd ft. Daft Punk", entry.artist)
        assertEquals("Starboy", entry.movieSeriesName)
        assertEquals(1, entry.movieSeriesNumber)
        assertEquals("rg_67890", entry.musicAlbumId)
        assertEquals("rec_12345", entry.externalId)
        assertEquals("MusicBrainz", entry.externalSource)
        assertEquals(230, entry.durationMinutes)
        assertEquals("https://coverartarchive.org/release/123/front-500", entry.coverImageUri)
        assertEquals(TrackingUnit.PERCENTAGE, entry.trackingUnit)
        assertEquals("3:50", entry.formatMusicDuration())
    }

    @Test
    fun testMusicTagRegistry() {
        val allGenres = MusicTagRegistry.PRESET_GENRES
        assertTrue(allGenres.isNotEmpty())
        assertTrue(allGenres.any { it.name.equals("Rock", ignoreCase = true) })
        assertTrue(allGenres.any { it.name.contains("Anime OST", ignoreCase = true) })

        val filtered = MusicTagRegistry.filterMusicGenres("pop")
        assertTrue(filtered.any { it.name == "Pop" })
        assertTrue(filtered.any { it.name == "J-Pop" })
        assertTrue(filtered.any { it.name == "K-Pop" })
    }

    @Test
    fun testExternalLinkHelperMusicOpenOptions() {
        val options = ExternalLinkHelper.buildMusicOpenOptions(
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            externalId = "mb_999",
            externalSource = "MusicBrainz"
        )

        assertTrue(options.size >= 4)
        val mbOption = options.find { it.title.contains("MusicBrainz") }
        assertNotNull(mbOption)
        assertEquals("https://musicbrainz.org/recording/mb_999", mbOption?.url)

        val appleMusicOption = options.find { it.title.contains("Apple Music") }
        assertNotNull(appleMusicOption)
        assertTrue(appleMusicOption?.url?.contains("music.apple.com") == true)

        val spotifyOption = options.find { it.title.contains("Spotify") }
        assertNotNull(spotifyOption)
        assertTrue(spotifyOption?.url?.contains("open.spotify.com") == true)
    }

    @Test
    fun testITunesGenreMap() {
        assertEquals("21", ITunesApiClient.GENRE_MAP["Rock"])
        assertEquals("14", ITunesApiClient.GENRE_MAP["Pop"])
        assertEquals("18", ITunesApiClient.GENRE_MAP["Hip-Hop"])
        assertEquals("27", ITunesApiClient.GENRE_MAP["J-Pop"])
        assertEquals("51", ITunesApiClient.GENRE_MAP["K-Pop"])
        assertEquals("29", ITunesApiClient.GENRE_MAP["Anime"])
        assertEquals("18", ITunesApiClient.resolveGenreId("hip-hop / rap"))
        assertEquals("17", ITunesApiClient.resolveGenreId("Dance"))
        assertEquals("29", ITunesApiClient.resolveGenreId("Anime OST"))
    }

    @Test
    fun testMusicLanguageDetection() {
        assertEquals("ja", ITunesApiClient.detectLanguageFromText("アイドル YOASOBI"))
        assertEquals("ko", ITunesApiClient.detectLanguageFromText("봄날 BTS"))
        assertEquals("zh", ITunesApiClient.detectLanguageFromText("告白氣球 周杰倫"))
        assertEquals("es", ITunesApiClient.detectLanguageFromText("Tití Me Preguntó Bad Bunny", "es", "es"))
        assertEquals("es", ITunesApiClient.inferLanguageFromCountry("ES"))
        assertEquals("ja", ITunesApiClient.inferLanguageFromCountry("jp"))
    }

    @Test
    fun testMusicDiscoveryFilter() {
        val topFilter = com.lx.tracklist.ui.search.DiscoveryFilter.Music(
            mode = com.lx.tracklist.ui.search.MusicDiscoveryMode.TOP_CHARTS
        )
        assertTrue(topFilter.label.contains("Top Music Charts"))

        val genreFilter = com.lx.tracklist.ui.search.DiscoveryFilter.Music(
            mode = com.lx.tracklist.ui.search.MusicDiscoveryMode.BY_GENRE,
            selectedGenre = "Rock"
        )
        assertTrue(genreFilter.label.contains("Rock Music"))

        val tagGenreFilter = com.lx.tracklist.ui.search.DiscoveryFilter.Music(
            mode = com.lx.tracklist.ui.search.MusicDiscoveryMode.BY_GENRE,
            includedTags = setOf("J-Pop"),
            originalLanguages = setOf("ja")
        )
        assertTrue(tagGenreFilter.label.contains("J-Pop Music"))
        assertTrue(tagGenreFilter.label.contains("Japanese"))

        val yearFilter = com.lx.tracklist.ui.search.DiscoveryFilter.Music(
            mode = com.lx.tracklist.ui.search.MusicDiscoveryMode.BY_YEAR,
            year = 2024
        )
        assertTrue(yearFilter.label.contains("Music 2024"))
    }
}
