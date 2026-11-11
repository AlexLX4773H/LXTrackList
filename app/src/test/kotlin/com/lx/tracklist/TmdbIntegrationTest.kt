package com.lx.tracklist

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.remote.JikanSyncManager
import com.lx.tracklist.data.remote.TmdbApiClient
import com.lx.tracklist.data.remote.TmdbSyncManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbIntegrationTest {

    @Test
    fun testBuildImageUrl() {
        val path = "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg"
        val url = TmdbApiClient.buildImageUrl(path, "w500")
        assertEquals("https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg", url)

        assertNull(TmdbApiClient.buildImageUrl(null))
        assertNull(TmdbApiClient.buildImageUrl(""))
    }

    @Test
    fun testCleanSearchTitle() {
        assertEquals("Inception", TmdbApiClient.cleanSearchTitle("Inception (2010)"))
        assertEquals("Breaking Bad", TmdbApiClient.cleanSearchTitle("[4K] Breaking Bad [HDR]"))
        assertEquals("Spider-Man", TmdbApiClient.cleanSearchTitle("Spider-Man"))
    }

    @Test
    fun testParseDateString() {
        val millis = TmdbApiClient.parseDateString("1999-10-15")
        assertNotNull(millis)
        assertTrue(millis!! > 0L)

        assertNull(TmdbApiClient.parseDateString(null))
        assertNull(TmdbApiClient.parseDateString(""))
        assertNull(TmdbApiClient.parseDateString("0000-00-00"))
    }

    @Test
    fun testParseMovieDetails() {
        val json = """
            {
                "id": 550,
                "title": "Fight Club",
                "original_title": "Fight Club Original",
                "overview": "A ticking-loss insomniac...",
                "poster_path": "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                "release_date": "1999-10-15",
                "runtime": 139,
                "vote_average": 8.433,
                "status": "Released",
                "imdb_id": "tt0137523",
                "genres": [
                    {"id": 18, "name": "Drama"},
                    {"id": 53, "name": "Thriller"}
                ],
                "alternative_titles": {
                    "titles": [
                        {"iso_3166_1": "US", "title": "Fight Club: Extended"}
                    ]
                }
            }
        """.trimIndent()

        val jsonObject = JsonParser.parseString(json).asJsonObject
        val details = TmdbApiClient.parseMovieDetails(jsonObject, 550L)

        assertEquals(550L, details.tmdbId)
        assertEquals("Fight Club", details.title)
        assertEquals(139, details.durationMinutes)
        assertEquals(8.433f, details.externalScore ?: 0f, 0.001f)
        assertEquals("TMDB", details.externalScoreSource)
        assertEquals("tt0137523", details.imdbId)
        assertEquals("Drama, Thriller", details.tags)
        assertEquals("https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg", details.posterUrl)
        assertEquals(false, details.isAiring)
        assertNotNull(details.airedStartDate)
        assertTrue(details.alternateTitle?.contains("Fight Club Original") == true)
        assertTrue(details.alternateTitle?.contains("Fight Club: Extended") == true)
    }

    @Test
    fun testParseTvDetails() {
        val json = """
            {
                "id": 1396,
                "name": "Breaking Bad",
                "original_name": "Breaking Bad Original",
                "overview": "Walter White, a chemistry teacher...",
                "poster_path": "/ggFHVNu6YYI5L9pCfOacjizRGt.jpg",
                "first_air_date": "2008-01-20",
                "last_air_date": "2013-09-29",
                "episode_run_time": [47],
                "number_of_episodes": 62,
                "number_of_seasons": 5,
                "vote_average": 8.9,
                "status": "Ended",
                "genres": [
                    {"id": 18, "name": "Drama"},
                    {"id": 80, "name": "Crime"}
                ],
                "alternative_titles": {
                    "results": [
                        {"iso_3166_1": "ES", "title": "Metástasis"}
                    ]
                }
            }
        """.trimIndent()

        val jsonObject = JsonParser.parseString(json).asJsonObject
        val details = TmdbApiClient.parseTvDetails(jsonObject, 1396L)

        assertEquals(1396L, details.tmdbId)
        assertEquals("Breaking Bad", details.title)
        assertEquals(47, details.durationMinutes)
        assertEquals(62, details.totalEpisodes)
        assertEquals(5, details.totalSeasons)
        assertEquals(8.9f, details.externalScore ?: 0f, 0.001f)
        assertEquals("Drama, Crime", details.tags)
        assertEquals(false, details.isAiring)
        assertNotNull(details.airedStartDate)
        assertNotNull(details.airedEndDate)
        assertTrue(details.alternateTitle?.contains("Metástasis") == true)
    }

    @Test
    fun testEligibilityAndSourceDetection() {
        // Eligibility
        assertTrue(TmdbSyncManager.isEligibleMediaForTmdb(MediaType.MOVIE))
        assertTrue(TmdbSyncManager.isEligibleMediaForTmdb(MediaType.SHOW))
        assertFalse(TmdbSyncManager.isEligibleMediaForTmdb(MediaType.ANIME))
        assertFalse(TmdbSyncManager.isEligibleMediaForTmdb(MediaType.MANGA))

        // Tmdb source check
        val movieEntry = MediaEntry(
            title = "Inception",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.COMPLETED
        )
        assertTrue(TmdbSyncManager.isTmdbSource(movieEntry))

        val tmdbExplicitEntry = movieEntry.copy(externalSource = "TMDB", externalId = "27205")
        assertTrue(TmdbSyncManager.isTmdbSource(tmdbExplicitEntry))

        val malMovieEntry = movieEntry.copy(externalSource = "MAL", externalId = "199")
        assertFalse(TmdbSyncManager.isTmdbSource(malMovieEntry))

        // Ensure JikanSyncManager does NOT claim TMDB-sourced entry
        assertFalse(JikanSyncManager.isMalSource(tmdbExplicitEntry))
    }

    @Test
    fun testExternalDbSyncPartitioning() {
        val anime = MediaEntry(id = 1L, title = "Naruto", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED)
        val manga = MediaEntry(id = 2L, title = "Berserk", mediaType = MediaType.MANGA, status = MediaStatus.WATCHING)
        val manhwa = MediaEntry(id = 3L, title = "Solo Leveling", mediaType = MediaType.MANHWA, status = MediaStatus.WATCHING)
        val showTmdb = MediaEntry(id = 4L, title = "Breaking Bad", mediaType = MediaType.SHOW, status = MediaStatus.COMPLETED, externalSource = "TMDB")
        val movieTmdb = MediaEntry(id = 5L, title = "Inception", mediaType = MediaType.MOVIE, status = MediaStatus.COMPLETED)
        val animeMovieMal = MediaEntry(id = 6L, title = "Your Name", mediaType = MediaType.MOVIE, status = MediaStatus.COMPLETED, externalSource = "MAL", externalId = "32281")

        val all = listOf(anime, manga, manhwa, showTmdb, movieTmdb, animeMovieMal)

        val jikanIds = mutableSetOf<Long>()
        val tmdbIds = mutableSetOf<Long>()

        for (entry in all) {
            val src = (entry.externalSource ?: entry.foreignDatabaseSource)?.trim() ?: ""
            if (src.equals("TMDB", ignoreCase = true)) {
                tmdbIds.add(entry.id)
            } else if (src.equals("MAL", ignoreCase = true) || src.equals("MyAnimeList", ignoreCase = true)) {
                jikanIds.add(entry.id)
            } else if (entry.mediaType == MediaType.MOVIE || entry.mediaType == MediaType.SHOW) {
                tmdbIds.add(entry.id)
            } else {
                jikanIds.add(entry.id)
            }
        }

        assertEquals(setOf(1L, 2L, 3L, 6L), jikanIds)
        assertEquals(setOf(4L, 5L), tmdbIds)
    }

    @Test
    fun testSearchCategoriesAndNavigation() {
        assertEquals("search", com.lx.tracklist.ui.navigation.Screen.Search.route)
        assertEquals("Search", com.lx.tracklist.ui.navigation.Screen.Search.title)

        assertEquals("External Sync Jobs", com.lx.tracklist.ui.settings.SettingsSubMenu.SYNC_JOBS.title)

        val categories = com.lx.tracklist.ui.search.SearchCategory.entries
        assertEquals(5, categories.size)
        assertEquals("MAL", com.lx.tracklist.ui.search.SearchCategory.ANIME.sourceName)
        assertEquals("MAL / MangaDex", com.lx.tracklist.ui.search.SearchCategory.MANGA.sourceName)
        assertEquals("MangaDex", com.lx.tracklist.ui.search.SearchCategory.MANHWA.sourceName)
        assertEquals("TMDB", com.lx.tracklist.ui.search.SearchCategory.MOVIES.sourceName)
        assertEquals("TMDB", com.lx.tracklist.ui.search.SearchCategory.TV_SHOWS.sourceName)
    }

    @Test
    fun testTmdbAttributionDisclaimer() {
        val disclaimer = "This product uses the TMDB API but is not endorsed or certified by TMDB."
        assertTrue(disclaimer.contains("TMDB API"))
        assertTrue(disclaimer.contains("not endorsed or certified by TMDB"))
    }

    @Test
    fun testExternalDbSourcesInAddEdit() {
        val malSource = com.lx.tracklist.ui.edit.ExternalDbSource.MAL
        val tmdbSource = com.lx.tracklist.ui.edit.ExternalDbSource.TMDB

        assertEquals("MyAnimeList (MAL)", malSource.displayName)
        assertEquals("MAL", malSource.shortName)
        assertEquals("The Movie Database (TMDB)", tmdbSource.displayName)
        assertEquals("TMDB", tmdbSource.shortName)
    }

    @Test
    fun testRelatedMediaItemProperties() {
        val sequel = com.lx.tracklist.data.model.RelatedMediaItem(
            title = "Ace Ventura: When Nature Calls",
            relationship = "Sequel",
            externalId = "9273",
            externalSource = "TMDB",
            mediaType = MediaType.MOVIE,
            posterUrl = "https://image.tmdb.org/t/p/w500/test.jpg",
            releaseYear = "1995",
            score = 6.4f,
            inLibraryEntryId = 42L,
            inLibraryStatus = MediaStatus.COMPLETED
        )

        assertEquals("Ace Ventura: When Nature Calls", sequel.title)
        assertEquals("Sequel", sequel.relationship)
        assertEquals("9273", sequel.externalId)
        assertEquals("TMDB", sequel.externalSource)
        assertEquals(MediaType.MOVIE, sequel.mediaType)
        assertEquals("1995", sequel.releaseYear)
        assertEquals(6.4f, sequel.score)
        assertEquals(42L, sequel.inLibraryEntryId)
        assertEquals(MediaStatus.COMPLETED, sequel.inLibraryStatus)
    }

    @Test
    fun testPrequelVsSequelClassification() {
        val baseDate = "2001-03-18"
        val earlierPartDate = "1999-05-20"
        val laterPartDate = "2003-07-25"

        val relEarlier = if (earlierPartDate.isNotBlank() && earlierPartDate < baseDate) "Prequel" else "Sequel"
        val relLater = if (laterPartDate.isNotBlank() && laterPartDate > baseDate) "Sequel" else "Prequel"

        assertEquals("Prequel", relEarlier)
        assertEquals("Sequel", relLater)
    }

    @Test
    fun testBaseUrlsAndStickyPreference() {
        assertEquals("https://api.themoviedb.org/3", TmdbApiClient.DEFAULT_BASE_URL)
        assertEquals("https://api.tmdb.org/3", TmdbApiClient.FALLBACK_BASE_URL)

        TmdbApiClient.resetPreferredBaseUrl()
        assertEquals(TmdbApiClient.DEFAULT_BASE_URL, TmdbApiClient.getPreferredBaseUrl())
    }

    @Test
    fun testParseTvDetailsExtractsPerSeasonEpisodesAndFixesSingleEpFallback() {
        val sampleJson = """
            {
              "id": 45,
              "name": "Top Gear",
              "original_name": "Top Gear",
              "number_of_seasons": 3,
              "number_of_episodes": 1,
              "seasons": [
                { "season_number": 0, "episode_count": 40, "name": "Specials" },
                { "season_number": 1, "episode_count": 8, "name": "Series 1" },
                { "season_number": 2, "episode_count": 10, "name": "Series 2" },
                { "season_number": 3, "episode_count": 6, "name": "Series 3" }
              ],
              "genres": [{ "id": 99, "name": "Documentary" }],
              "status": "Returning Series",
              "vote_average": 7.5
            }
        """.trimIndent()

        val obj = com.google.gson.JsonParser.parseString(sampleJson).asJsonObject
        val details = TmdbApiClient.parseTvDetails(obj, 45L)

        assertEquals(45L, details.tmdbId)
        assertEquals("Top Gear", details.title)
        assertEquals(3, details.totalSeasons)
        assertEquals(listOf(8, 10, 6), details.seasonEpisodes)
        assertEquals(24, details.totalEpisodes)
    }
}

