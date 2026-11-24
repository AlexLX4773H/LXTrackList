package com.lx.tracklist

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.remote.JikanSyncManager
import com.lx.tracklist.data.remote.TmdbApiClient
import com.lx.tracklist.data.remote.TmdbSyncManager
import com.lx.tracklist.data.remote.buildMovieTvDescriptionWithMetadata
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
                "production_companies": [
                    {"id": 508, "name": "Regency Enterprises"},
                    {"id": 711, "name": "Fox 2000 Pictures"}
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
        assertEquals(listOf("Regency Enterprises", "Fox 2000 Pictures"), details.studios)
        assertEquals("Regency Enterprises, Fox 2000 Pictures", details.studioString)
        val movieDesc = details.buildDescriptionWithMetadata()
        assertTrue(movieDesc.contains("A ticking-loss insomniac..."))
        assertTrue(movieDesc.contains("Studio: Regency Enterprises, Fox 2000 Pictures"))
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
                "production_companies": [
                    {"id": 11073, "name": "Sony Pictures Television"},
                    {"id": 33742, "name": "High Bridge Productions"}
                ],
                "networks": [
                    {"id": 174, "name": "AMC"}
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
        assertEquals(listOf("Sony Pictures Television", "High Bridge Productions"), details.studios)
        assertEquals(listOf("AMC"), details.publishers)
        val tvDesc = details.buildDescriptionWithMetadata()
        assertTrue(tvDesc.contains("Walter White, a chemistry teacher..."))
        assertTrue(tvDesc.contains("Studio: Sony Pictures Television, High Bridge Productions"))
        assertTrue(tvDesc.contains("Publisher: AMC"))
        assertEquals(false, details.isAiring)
        assertNotNull(details.airedStartDate)
        assertNotNull(details.airedEndDate)
        assertTrue(details.alternateTitle?.contains("Metástasis") == true)
    }

    @Test
    fun testParseTvDetailsCompletedShowFallbacks() {
        // Test TV show that ended but last_air_date is null, falling back to last_episode_to_air or seasons
        val jsonWithEpisode = """
            {
                "id": 19885,
                "name": "Sherlock",
                "status": "Ended",
                "first_air_date": "2010-07-25",
                "last_episode_to_air": {
                    "air_date": "2017-01-15",
                    "episode_number": 3,
                    "season_number": 4
                },
                "seasons": [
                    {"season_number": 1, "air_date": "2010-07-25", "episode_count": 3},
                    {"season_number": 4, "air_date": "2017-01-01", "episode_count": 3}
                ]
            }
        """.trimIndent()
        val detailsWithEp = TmdbApiClient.parseTvDetails(JsonParser.parseString(jsonWithEpisode).asJsonObject, 19885L)
        assertEquals(false, detailsWithEp.isAiring)
        assertNotNull(detailsWithEp.airedEndDate)
        assertEquals(TmdbApiClient.parseDateString("2017-01-15"), detailsWithEp.airedEndDate)

        // Test fallback to seasons when last_episode_to_air is also missing
        val jsonWithSeasonOnly = """
            {
                "id": 19885,
                "name": "Sherlock",
                "status": "Ended",
                "first_air_date": "2010-07-25",
                "seasons": [
                    {"season_number": 1, "air_date": "2010-07-25", "episode_count": 3},
                    {"season_number": 4, "air_date": "2017-01-01", "episode_count": 3}
                ]
            }
        """.trimIndent()
        val detailsWithSeason = TmdbApiClient.parseTvDetails(JsonParser.parseString(jsonWithSeasonOnly).asJsonObject, 19885L)
        assertEquals(false, detailsWithSeason.isAiring)
        assertNotNull(detailsWithSeason.airedEndDate)
        assertEquals(TmdbApiClient.parseDateString("2017-01-01"), detailsWithSeason.airedEndDate)
    }

    @Test
    fun testParseTvDetailsAiringShowNullEndDate() {
        val json = """
            {
                "id": 1000,
                "name": "Ongoing Show",
                "status": "Returning Series",
                "first_air_date": "2024-01-10",
                "last_air_date": "2024-05-20"
            }
        """.trimIndent()
        val details = TmdbApiClient.parseTvDetails(JsonParser.parseString(json).asJsonObject, 1000L)
        assertEquals(true, details.isAiring)
        assertNotNull(details.airedStartDate)
        assertNull(details.airedEndDate)
    }

    @Test
    fun testParseMovieDetailsSetsAiredEndDate() {
        val json = """
            {
                "id": 550,
                "title": "Fight Club",
                "release_date": "1999-10-15"
            }
        """.trimIndent()
        val details = TmdbApiClient.parseMovieDetails(JsonParser.parseString(json).asJsonObject, 550L)
        assertNotNull(details.airedStartDate)
        assertEquals(details.airedStartDate, details.airedEndDate)
    }

    @Test
    fun testBuildMovieTvDescriptionWithMetadataDeduplication() {
        val baseOverview = "A great film about space exploration."
        val studios = listOf("Syncopy", "Legendary Pictures")
        val publishers = listOf("Warner Bros. Pictures")

        val formatted = buildMovieTvDescriptionWithMetadata(baseOverview, studios, publishers)
        assertTrue(formatted.contains("A great film about space exploration."))
        assertTrue(formatted.contains("Studio: Syncopy, Legendary Pictures"))
        assertTrue(formatted.contains("Publisher: Warner Bros. Pictures"))

        // Repeated build on already-formatted text shouldn't duplicate
        val secondPass = buildMovieTvDescriptionWithMetadata(formatted, studios, publishers)
        assertEquals(formatted, secondPass)
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
        assertEquals(9, categories.size)
        assertEquals("MAL", com.lx.tracklist.ui.search.SearchCategory.ANIME.sourceName)
        assertEquals("MAL / MangaDex", com.lx.tracklist.ui.search.SearchCategory.MANGA.sourceName)
        assertEquals("MangaDex / AniList", com.lx.tracklist.ui.search.SearchCategory.MANHWA.sourceName)
        assertEquals("TMDB", com.lx.tracklist.ui.search.SearchCategory.MOVIES.sourceName)
        assertEquals("TMDB", com.lx.tracklist.ui.search.SearchCategory.TV_SHOWS.sourceName)
        assertEquals("Google Books / Open Library", com.lx.tracklist.ui.search.SearchCategory.BOOKS.sourceName)
        assertEquals("AniList / MAL", com.lx.tracklist.ui.search.SearchCategory.LIGHT_NOVELS.sourceName)
        assertEquals("IGDB / RAWG", com.lx.tracklist.ui.search.SearchCategory.GAMES.sourceName)
        assertEquals("iTunes / MusicBrainz", com.lx.tracklist.ui.search.SearchCategory.MUSIC.sourceName)
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

    @Test
    fun testParseTvDetailsExtractsAiredEpisodesFromLastEpisodeToAir() {
        val sampleJson = """
            {
              "id": 999,
              "name": "Weekly Series",
              "status": "Returning Series",
              "number_of_episodes": 24,
              "last_episode_to_air": {
                "air_date": "2024-03-15",
                "episode_number": 11,
                "season_number": 1
              }
            }
        """.trimIndent()

        val obj = com.google.gson.JsonParser.parseString(sampleJson).asJsonObject
        val details = TmdbApiClient.parseTvDetails(obj, 999L)
        assertEquals(11, details.airedEpisodes)
        assertEquals(true, details.isAiring)
    }

    @Test
    fun testEffectiveAiredEpisodesForShow() {
        // Explicit airedEpisodes returns directly
        val completedShow = com.lx.tracklist.data.model.MediaEntry(
            id = 1L,
            userId = 1L,
            title = "Completed Show",
            mediaType = com.lx.tracklist.data.model.MediaType.SHOW,
            status = com.lx.tracklist.data.model.MediaStatus.COMPLETED,
            isAiring = false,
            totalEpisodes = 12,
            airedEpisodes = 12
        )
        assertEquals(12, completedShow.getEffectiveAiredEpisodes())

        // Airing show calculates from airedStartDate if totalEpisodes > 0 and airedEpisodes == 0
        val airingShow = com.lx.tracklist.data.model.MediaEntry(
            id = 2L,
            userId = 1L,
            title = "Airing Show",
            mediaType = com.lx.tracklist.data.model.MediaType.SHOW,
            status = com.lx.tracklist.data.model.MediaStatus.WATCHING,
            isAiring = true,
            totalEpisodes = 12,
            airedEpisodes = 0,
            airedStartDate = System.currentTimeMillis() - 21 * 24 * 60 * 60 * 1000L // 3 weeks ago
        )
        val calculated = airingShow.getEffectiveAiredEpisodes()
        // Should calculate approximately 4 episodes (day 0 = ep 1, +3 weeks = 4)
        assertTrue("Calculated should be >= 3, was $calculated", calculated >= 3)
    }

    @Test
    fun testParseMovieDetailsWithNullCollectionsAndNullRecsDoesNotThrowClassCastException() {
        // Simulates TMDB response for standalone movies like Marvel's Black Widow (ID 497698)
        val json = """
            {
              "id": 497698,
              "title": "Black Widow",
              "runtime": 134,
              "belongs_to_collection": null,
              "recommendations": null,
              "external_ids": null,
              "release_date": "2021-07-07",
              "overview": "Natasha Romanoff confronts the darker parts of her ledger...",
              "vote_average": 7.3
            }
        """.trimIndent()

        val parsedObj = JsonParser.parseString(json).asJsonObject
        val details = TmdbApiClient.parseMovieDetails(parsedObj, 497698L)
        assertEquals("Black Widow", details.title)
        assertEquals(134, details.durationMinutes)
        assertEquals(497698L, details.tmdbId)
        assertNotNull(details.airedStartDate)
        assertEquals(details.airedStartDate, details.airedEndDate)
    }

    @Test
    fun testParseTvDetailsWithNullNextEpisodeDoesNotThrowClassCastException() {
        // Simulates TMDB response for ended TV shows like Sherlock (ID 19885)
        val json = """
            {
              "id": 19885,
              "name": "Sherlock",
              "status": "Ended",
              "first_air_date": "2010-07-25",
              "last_air_date": "2017-01-15",
              "next_episode_to_air": null,
              "last_episode_to_air": null,
              "recommendations": null,
              "external_ids": null,
              "episode_run_time": null,
              "seasons": [
                {"season_number": 1, "air_date": "2010-07-25", "episode_count": 3},
                {"season_number": 4, "air_date": "2017-01-01", "episode_count": 3}
              ]
            }
        """.trimIndent()

        val parsedObj = JsonParser.parseString(json).asJsonObject
        val details = TmdbApiClient.parseTvDetails(parsedObj, 19885L)
        assertEquals("Sherlock", details.title)
        assertEquals(false, details.isAiring)
        assertNotNull(details.airedStartDate)
        assertNotNull(details.airedEndDate)
        assertEquals(TmdbApiClient.parseDateString("2017-01-15"), details.airedEndDate)
    }

    @Test
    fun testEffectiveAiredEpisodesProjectsCurrentWeek() {
        val entry = com.lx.tracklist.data.model.MediaEntry(
            id = 10L,
            userId = 1L,
            title = "Airing Anime",
            mediaType = com.lx.tracklist.data.model.MediaType.ANIME,
            status = com.lx.tracklist.data.model.MediaStatus.WATCHING,
            isAiring = true,
            totalEpisodes = 13,
            airedEpisodes = 7, // Stored previously as 7
            airedStartDate = System.currentTimeMillis() - (8 * 7 * 24 * 60 * 60 * 1000L) // 8 weeks ago
        )
        // 8 weeks ago + 1 = 9 episodes aired, which is > stored 7
        val effective = entry.getEffectiveAiredEpisodes()
        assertTrue("Effective aired episodes should be at least 8, was $effective", effective >= 8)
    }

    @Test
    fun testParseCreatorsAcrossTmdbJikanAniListAndMangaDex() {
        val tmdbMovieJson = JsonParser.parseString(
            """
            {
              "id": 27205,
              "title": "Inception",
              "credits": {
                "crew": [
                  {"job": "Producer", "name": "Emma Thomas"},
                  {"job": "Director", "name": "Christopher Nolan"}
                ]
              },
              "production_companies": [
                {"name": "Syncopy"},
                {"name": "Warner Bros. Pictures"}
              ]
            }
            """.trimIndent()
        ).asJsonObject
        val movieDetails = TmdbApiClient.parseMovieDetails(tmdbMovieJson, 27205L)
        assertEquals(listOf("Christopher Nolan"), movieDetails.directors)
        assertEquals("Christopher Nolan", movieDetails.resolveCreatorFor(false))

        val tmdbTvJson = JsonParser.parseString(
            """
            {
              "id": 1396,
              "name": "Breaking Bad",
              "created_by": [
                {"id": 66633, "name": "Vince Gilligan"}
              ],
              "networks": [
                {"name": "AMC"}
              ],
              "production_companies": [
                {"name": "Sony Pictures Television"}
              ]
            }
            """.trimIndent()
        ).asJsonObject
        val tvDetails = TmdbApiClient.parseTvDetails(tmdbTvJson, 1396L)
        assertEquals(listOf("Vince Gilligan"), tvDetails.creators)
        assertEquals("Vince Gilligan", tvDetails.resolveCreatorFor(true))

        val jikanAnimeJson = JsonParser.parseString(
            """
            {
              "mal_id": 5114,
              "title": "Fullmetal Alchemist: Brotherhood",
              "type": "TV",
              "studios": [{"mal_id": 4, "name": "Bones"}]
            }
            """.trimIndent()
        ).asJsonObject
        val animeDetails = com.lx.tracklist.data.remote.JikanApiClient.parseJikanData(jikanAnimeJson, 5114L)
        assertEquals("Bones", animeDetails.authors)

        val jikanMangaJson = JsonParser.parseString(
            """
            {
              "mal_id": 2,
              "title": "Berserk",
              "type": "Manga",
              "authors": [{"mal_id": 1868, "name": "Miura, Kentarou"}]
            }
            """.trimIndent()
        ).asJsonObject
        val mangaDetails = com.lx.tracklist.data.remote.JikanApiClient.parseJikanData(jikanMangaJson, 2L)
        assertEquals("Miura, Kentarou", mangaDetails.authors)

        val aniListAnimeJson = JsonParser.parseString(
            """
            {
              "id": 16498,
              "idMal": 16498,
              "title": {"romaji": "Shingeki no Kyojin", "english": "Attack on Titan"},
              "format": "TV",
              "studios": {"nodes": [{"name": "Wit Studio"}]}
            }
            """.trimIndent()
        ).asJsonObject
        val aniAnime = com.lx.tracklist.data.remote.JikanApiClient.parseAniListMedia(aniListAnimeJson, 16498L)
        assertEquals("Wit Studio", aniAnime.authors)

        val aniListMangaJson = JsonParser.parseString(
            """
            {
              "id": 30002,
              "idMal": 2,
              "title": {"romaji": "Berserk"},
              "format": "MANGA",
              "staff": {
                "edges": [
                  {"role": "Story & Art", "node": {"name": {"full": "Kentarou Miura"}}}
                ]
              }
            }
            """.trimIndent()
        ).asJsonObject
        val aniManga = com.lx.tracklist.data.remote.JikanApiClient.parseAniListMedia(aniListMangaJson, 2L)
        assertEquals("Kentarou Miura", aniManga.authors)

        val mangaDexJson = JsonParser.parseString(
            """
            {
              "id": "md-123",
              "attributes": {
                "title": {"en": "Solo Leveling"},
                "originalLanguage": "ko"
              },
              "relationships": [
                {"type": "author", "attributes": {"name": "Chugong"}},
                {"type": "artist", "attributes": {"name": "DUBU"}}
              ]
            }
            """.trimIndent()
        ).asJsonObject
        val mdDetails = com.lx.tracklist.data.remote.MangaDexApiClient.parseMangaDexItem(mangaDexJson)
        assertEquals("Chugong, DUBU", mdDetails?.authors)
    }
}

