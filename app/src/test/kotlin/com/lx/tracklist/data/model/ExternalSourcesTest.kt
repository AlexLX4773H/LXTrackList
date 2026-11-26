package com.lx.tracklist.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalSourcesTest {

    @Test
    fun `canonical maps every documented alias row accurately`() {
        val expectedMappings = listOf(
            "MAL" to ExternalSources.MAL,
            "mal" to ExternalSources.MAL,
            "MyAnimeList" to ExternalSources.MAL,
            "MYANIMELIST" to ExternalSources.MAL,
            "Jikan" to ExternalSources.MAL,
            "MAL (AniList)" to ExternalSources.MAL,
            "ANILIST" to ExternalSources.ANILIST,
            "anilist" to ExternalSources.ANILIST,
            "KITSU" to ExternalSources.KITSU,
            "kitsu" to ExternalSources.KITSU,
            "MANGADEX" to ExternalSources.MANGADEX,
            "mangadex" to ExternalSources.MANGADEX,
            "TMDB" to ExternalSources.TMDB,
            "TheMovieDB" to ExternalSources.TMDB,
            "The Movie Database" to ExternalSources.TMDB,
            "GOOGLE BOOKS" to ExternalSources.GOOGLE_BOOKS,
            "GoogleBooks" to ExternalSources.GOOGLE_BOOKS,
            "google_books" to ExternalSources.GOOGLE_BOOKS,
            "OPEN LIBRARY" to ExternalSources.OPEN_LIBRARY,
            "OpenLibrary" to ExternalSources.OPEN_LIBRARY,
            "open_library" to ExternalSources.OPEN_LIBRARY,
            "igdb" to ExternalSources.IGDB,
            "IGDB" to ExternalSources.IGDB,
            "rawg" to ExternalSources.RAWG,
            "RAWG" to ExternalSources.RAWG,
            "iTunes" to ExternalSources.APPLE_MUSIC,
            "ITUNES" to ExternalSources.APPLE_MUSIC,
            "apple music" to ExternalSources.APPLE_MUSIC,
            "APPLE_MUSIC" to ExternalSources.APPLE_MUSIC,
            "musicbrainz" to ExternalSources.MUSICBRAINZ,
            "MUSICBRAINZ" to ExternalSources.MUSICBRAINZ,
            "TACHIYOMI" to ExternalSources.TACHIYOMI,
            "Mihon" to ExternalSources.TACHIYOMI
        )

        for ((raw, canonical) in expectedMappings) {
            assertEquals("Failed for '$raw'", canonical, ExternalSources.canonical("  $raw  "))
        }
    }

    @Test
    fun `canonical handles blank, CSV, and unknown strings`() {
        assertNull(ExternalSources.canonical(null))
        assertNull(ExternalSources.canonical(""))
        assertNull(ExternalSources.canonical("   "))
        assertNull(ExternalSources.canonical("CSV"))
        assertNull(ExternalSources.canonical("csv", externalId = ""))
        assertNull(ExternalSources.canonical("CSV", externalId = "   "))

        assertEquals("CSV", ExternalSources.canonical(" CSV ", externalId = "12345"))
        assertEquals("CustomSource", ExternalSources.canonical("  CustomSource  "))
    }
}
