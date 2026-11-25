package com.lx.tracklist.data.remote

import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.ui.search.SearchResultItem
import com.lx.tracklist.util.ExternalLinkHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSyncAndLinksTest {

    @Test
    fun `music sync source and eligibility checks cover music providers`() {
        assertTrue(MusicSyncManager.isEligibleMediaForMusic(MediaType.MUSIC))
        assertFalse(MusicSyncManager.isEligibleMediaForMusic(MediaType.ANIME))
        assertFalse(MusicSyncManager.isEligibleMediaForMusic(MediaType.MOVIE))

        assertTrue(MusicSyncManager.isMusicSource(MediaEntry(title = "Papercut", mediaType = MediaType.MUSIC, status = MediaStatus.PLAN_TO_WATCH, externalSource = "Apple Music")))
        assertTrue(MusicSyncManager.isMusicSource(MediaEntry(title = "Papercut", mediaType = MediaType.MUSIC, status = MediaStatus.PLAN_TO_WATCH, externalSource = "iTunes")))
        assertTrue(MusicSyncManager.isMusicSource(MediaEntry(title = "Papercut", mediaType = MediaType.MUSIC, status = MediaStatus.PLAN_TO_WATCH, externalSource = "MusicBrainz")))
        assertTrue(MusicSyncManager.isMusicSource(MediaEntry(title = "Papercut", mediaType = MediaType.MUSIC, status = MediaStatus.PLAN_TO_WATCH, externalSource = "")))
        assertFalse(MusicSyncManager.isMusicSource(MediaEntry(title = "Papercut", mediaType = MediaType.MUSIC, status = MediaStatus.PLAN_TO_WATCH, externalSource = "TMDB")))
        assertFalse(MusicSyncManager.isMusicSource(MediaEntry(title = "Inception", mediaType = MediaType.MOVIE, status = MediaStatus.PLAN_TO_WATCH, externalSource = "Apple Music")))
    }

    @Test
    fun `media entry source normalization treats Apple Music and iTunes consistently`() {
        assertEquals("ITUNES", MediaEntry.normalizeSource("Apple Music"))
        assertEquals("ITUNES", MediaEntry.normalizeSource("iTunes"))
    }

    @Test
    fun `music sync needsSync detects missing fields and force refresh`() {
        val completeEntry = MediaEntry(
            title = "Papercut",
            mediaType = MediaType.MUSIC,
            status = MediaStatus.PLAN_TO_WATCH,
            externalId = "528436019",
            externalSource = "Apple Music",
            coverImageUri = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/cover/600x600bb.jpg",
            creator = "LINKIN PARK",
            movieSeriesName = "Hybrid Theory",
            musicAlbumId = "528436018",
            movieSeriesNumber = 1,
            durationMinutes = 184,
            airedStartDate = 972370800000L,
            tags = "Rock",
            originalLanguage = "English",
            relatedEntriesJson = "[{\"externalId\":\"528436020\",\"title\":\"One Step Closer\"}]"
        )

        assertFalse(MusicSyncManager.needsSync(completeEntry, forceRefresh = false))
        assertTrue(MusicSyncManager.needsSync(completeEntry, forceRefresh = true))
        assertTrue(MusicSyncManager.needsSync(completeEntry.copy(relatedEntriesJson = null), forceRefresh = false))
        assertTrue(MusicSyncManager.needsSync(completeEntry.copy(musicAlbumId = null), forceRefresh = false))
        assertTrue(MusicSyncManager.needsSync(completeEntry.copy(airedStartDate = null), forceRefresh = false))
        assertTrue(MusicSyncManager.needsSync(completeEntry.copy(tags = ""), forceRefresh = false))
    }

    @Test
    fun `pickBestMusicMatch prefers matching title, artist, and album`() {
        val wrongCover = MusicSearchResult(
            externalId = "111",
            externalSource = "iTunes",
            title = "Papercut (Live Cover)",
            artist = "Tribute Band",
            albumName = "Covers"
        )
        val exactMatch = MusicSearchResult(
            externalId = "528436019",
            externalSource = "iTunes",
            title = "Papercut",
            artist = "LINKIN PARK",
            albumName = "Hybrid Theory (Deluxe Edition)"
        )
        val picked = MusicMetadataManager.pickBestMusicMatch(
            targetTitle = "Papercut",
            targetArtist = "LINKIN PARK",
            targetAlbum = "Hybrid Theory",
            candidates = listOf(wrongCover, exactMatch)
        )
        assertEquals("528436019", picked?.externalId)
    }

    @Test
    fun `iTunes release date parser handles ISO timestamps safely`() {
        val millis = ITunesApiClient.parseReleaseDateMillis("2000-10-24T07:00:00Z")
        assertNotNull(millis)
        assertTrue((millis ?: 0L) > 900_000_000_000L)
        assertNull(ITunesApiClient.parseReleaseDateMillis(""))
        assertNull(ITunesApiClient.parseReleaseDateMillis("not-a-date"))
    }

    @Test
    fun `SearchResultItem webUrl for Music resolves music providers instead of MAL fallback`() {
        val itunesItem = SearchResultItem(
            externalId = "528436019",
            externalSource = "iTunes",
            mediaType = MediaType.MUSIC,
            title = "Papercut",
            rawMusicResult = MusicSearchResult(
                externalId = "528436019",
                externalSource = "iTunes",
                title = "Papercut",
                artist = "LINKIN PARK",
                webUrl = "https://music.apple.com/us/album/papercut/528436018?i=528436019"
            )
        )
        assertTrue(itunesItem.webUrl?.contains("music.apple.com") == true)
        assertFalse(itunesItem.webUrl?.contains("myanimelist.net") == true)

        val mbItem = SearchResultItem(
            externalId = "mbid-1234",
            externalSource = "MusicBrainz",
            mediaType = MediaType.MUSIC,
            title = "Papercut"
        )
        assertEquals("https://musicbrainz.org/recording/mbid-1234", mbItem.webUrl)

        val itunesNoRawUrl = SearchResultItem(
            externalId = "528436019",
            externalSource = "Apple Music",
            mediaType = MediaType.MUSIC,
            title = "Papercut",
            musicArtist = "LINKIN PARK"
        )
        assertFalse(itunesNoRawUrl.webUrl.orEmpty().contains("myanimelist.net"))
        assertTrue(itunesNoRawUrl.webUrl.orEmpty().contains("music.apple.com"))
    }

    @Test
    fun `extractSpotifyTrackId supports open links, app URIs, and encoded URLs`() {
        assertEquals(
            "60a0Rd6pjrkxjPbaKzXjfq",
            ExternalLinkHelper.extractSpotifyTrackId("https://open.spotify.com/track/60a0Rd6pjrkxjPbaKzXjfq")
        )
        assertEquals(
            "60a0Rd6pjrkxjPbaKzXjfq",
            ExternalLinkHelper.extractSpotifyTrackId("https://open.spotify.com/intl-en/track/60a0Rd6pjrkxjPbaKzXjfq?si=xyz")
        )
        assertEquals(
            "60a0Rd6pjrkxjPbaKzXjfq",
            ExternalLinkHelper.extractSpotifyTrackId("spotify:track:60a0Rd6pjrkxjPbaKzXjfq")
        )
        assertEquals(
            "60a0Rd6pjrkxjPbaKzXjfq",
            ExternalLinkHelper.extractSpotifyTrackId("https%3A%2F%2Fopen.spotify.com%2Ftrack%2F60a0Rd6pjrkxjPbaKzXjfq")
        )
        assertNull(ExternalLinkHelper.extractSpotifyTrackId("https://open.spotify.com/album/6PFPXS3p4hOA3562229384"))
        assertNull(ExternalLinkHelper.extractSpotifyTrackId(null))
    }

    @Test
    fun `extractItunesTrackIdFromAppleUrl parses query and song path IDs`() {
        assertEquals(
            "528436019",
            ExternalLinkHelper.extractItunesTrackIdFromAppleUrl("https://music.apple.com/us/album/papercut/528436018?i=528436019")
        )
        assertEquals(
            "528436019",
            ExternalLinkHelper.extractItunesTrackIdFromAppleUrl("https://music.apple.com/us/song/papercut/528436019")
        )
        assertNull(ExternalLinkHelper.extractItunesTrackIdFromAppleUrl("https://music.apple.com/us/album/hybrid-theory/528436018"))
    }

    @Test
    fun `parseSpotifyTrackIdFromOdesliJson extracts URL, native URI, and entityUniqueId`() {
        val urlJson = """
            {
              "linksByPlatform": {
                "spotify": {
                  "url": "https://open.spotify.com/track/60a0Rd6pjrkxjPbaKzXjfq",
                  "entityUniqueId": "SPOTIFY_SONG::60a0Rd6pjrkxjPbaKzXjfq"
                }
              }
            }
        """.trimIndent()
        assertEquals("60a0Rd6pjrkxjPbaKzXjfq", ExternalLinkHelper.parseSpotifyTrackIdFromOdesliJson(urlJson))

        val mobileUriJson = """
            {
              "linksByPlatform": {
                "spotify": {
                  "nativeAppUriMobile": "spotify:track:2nLtzopw4rPReszdYBJU6h"
                }
              }
            }
        """.trimIndent()
        assertEquals("2nLtzopw4rPReszdYBJU6h", ExternalLinkHelper.parseSpotifyTrackIdFromOdesliJson(mobileUriJson))

        val entityOnlyJson = """
            {
              "linksByPlatform": {
                "spotify": {
                  "entityUniqueId": "SPOTIFY_SONG::1BxfuPKGuaTgP7aM0Bbdwr"
                }
              }
            }
        """.trimIndent()
        assertEquals("1BxfuPKGuaTgP7aM0Bbdwr", ExternalLinkHelper.parseSpotifyTrackIdFromOdesliJson(entityOnlyJson))

        assertNull(ExternalLinkHelper.parseSpotifyTrackIdFromOdesliJson("""{"linksByPlatform":{}}"""))
        assertNull(ExternalLinkHelper.parseSpotifyTrackIdFromOdesliJson("invalid-json"))
    }

    @Test
    fun `buildMusicOpenOptions prefers direct Spotify track URL when known`() {
        val directOptions = ExternalLinkHelper.buildMusicOpenOptions(
            title = "Papercut",
            artist = "LINKIN PARK",
            album = "Hybrid Theory",
            externalId = "528436019",
            externalSource = "Apple Music",
            webUrl = "https://open.spotify.com/track/2nLtzopw4rPReszdYBJU6h"
        )
        val spotifyOption = directOptions.find { it.title.contains("Spotify") }
        assertNotNull(spotifyOption)
        assertEquals("https://open.spotify.com/track/2nLtzopw4rPReszdYBJU6h", spotifyOption?.url)
    }
}
