package com.lx.tracklist.ui.detail

import com.lx.tracklist.data.model.ExternalSources
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.util.ExternalLinkHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailRefreshRoutingTest {

    @Test
    fun `TMDB movie in Anime Movies custom category routes to TMDB not MAL`() {
        val tmdbAnimeMovie = MediaEntry(
            title = "Spirited Away",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.COMPLETED,
            customCategory = "Anime Movies",
            externalSource = "tmdb",
            externalId = "129",
            externalScoreSource = "MAL"
        )
        val plan = DetailViewModel.resolveDetailRefreshPlan(tmdbAnimeMovie)
        assertEquals(DetailRefreshProvider.TMDB, plan.provider)
        assertEquals("The Movie Database (TMDB)", plan.sourceDisplayName)
        assertFalse(plan.isFindingAndLinking)
    }

    @Test
    fun `MAL-linked Manhwa routes to MAL and preserves MAL source after refresh`() {
        val malManhwa = MediaEntry(
            title = "Solo Leveling",
            mediaType = MediaType.MANHWA,
            status = MediaStatus.WATCHING,
            externalSource = "MyAnimeList",
            externalId = "121496"
        )
        val plan = DetailViewModel.resolveDetailRefreshPlan(malManhwa)
        assertEquals(DetailRefreshProvider.MAL, plan.provider)
        assertEquals("MyAnimeList (MAL)", plan.sourceDisplayName)
        assertTrue(plan.hasValidId)

        val preserved = DetailViewModel.resolveRefAfterRefresh(
            existing = malManhwa,
            candidateId = "abc-mangadex-uuid",
            candidateSource = "MangaDex",
            forceReLink = false
        )
        assertEquals("121496" to ExternalSources.MAL, preserved)

        val relinked = DetailViewModel.resolveRefAfterRefresh(
            existing = malManhwa,
            candidateId = "abc-mangadex-uuid",
            candidateSource = "MangaDex",
            forceReLink = true
        )
        assertEquals("abc-mangadex-uuid" to ExternalSources.MANGADEX, relinked)
    }

    @Test
    fun `AniList MangaDex Kitsu and unlinked entries route deterministically`() {
        val aniManga = MediaEntry(
            title = "Vagabond",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            externalSource = "AniList",
            externalId = "30656"
        )
        assertEquals(DetailRefreshProvider.ANILIST, DetailViewModel.resolveDetailRefreshPlan(aniManga).provider)

        val mdManhwa = aniManga.copy(
            mediaType = MediaType.MANHWA,
            externalSource = "MangaDex",
            externalId = "32d76d19-8a05-4db0-9fc2-e0b0648fe9d0"
        )
        assertEquals(DetailRefreshProvider.MANGADEX, DetailViewModel.resolveDetailRefreshPlan(mdManhwa).provider)

        val kitsuAnime = MediaEntry(
            title = "Frieren",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            externalSource = "Kitsu",
            externalId = "46474"
        )
        assertEquals(DetailRefreshProvider.KITSU, DetailViewModel.resolveDetailRefreshPlan(kitsuAnime).provider)

        val unlinkedMovieInAnimeCat = MediaEntry(
            title = "Your Name",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.COMPLETED,
            customCategory = "Anime Movies"
        )
        val moviePlan = DetailViewModel.resolveDetailRefreshPlan(unlinkedMovieInAnimeCat)
        assertEquals(DetailRefreshProvider.TMDB, moviePlan.provider)
        assertTrue(moviePlan.isFindingAndLinking)

        val tachiyomiManhwa = MediaEntry(
            title = "TBATE",
            mediaType = MediaType.MANHWA,
            status = MediaStatus.WATCHING,
            externalSource = "TACHIYOMI",
            externalId = "/manga/tbate"
        )
        val tachiPlan = DetailViewModel.resolveDetailRefreshPlan(tachiyomiManhwa)
        assertEquals(DetailRefreshProvider.RESOLVE_IDS, tachiPlan.provider)
        assertTrue(tachiPlan.isFindingAndLinking)
    }

    @Test
    fun `buildOpenOptionsForEntry returns category-appropriate providers for every category`() {
        val anime = ExternalLinkHelper.buildOpenOptionsForEntry(
            MediaEntry(title = "Frieren", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, externalId = "52991", externalSource = "MAL")
        )
        assertTrue(anime.any { it.title.contains("MyAnimeList") })
        assertTrue(anime.any { it.title.contains("AniList") })
        assertTrue(anime.any { it.title.contains("Kitsu") })

        val manga = ExternalLinkHelper.buildOpenOptionsForEntry(
            MediaEntry(title = "Vagabond", mediaType = MediaType.MANGA, status = MediaStatus.WATCHING)
        )
        assertTrue(manga.any { it.title.contains("MangaDex") })
        assertTrue(manga.any { it.title.contains("MyAnimeList") })
        assertTrue(manga.any { it.title.contains("AniList") })
        assertTrue(manga.any { it.title.contains("Kitsu") })

        val manhwa = ExternalLinkHelper.buildOpenOptionsForEntry(
            MediaEntry(title = "Solo Leveling", mediaType = MediaType.MANHWA, status = MediaStatus.WATCHING)
        )
        assertEquals(manga.map { it.title }, manhwa.map { it.title })

        val book = ExternalLinkHelper.buildOpenOptionsForEntry(
            MediaEntry(title = "Dune", mediaType = MediaType.BOOK, status = MediaStatus.PLAN_TO_WATCH, externalSource = "Open Library", externalId = "OL1W")
        )
        assertTrue(book.any { it.title.contains("Google Books") })
        assertTrue(book.any { it.title.contains("Open Library") })
        assertTrue(book.any { it.title.contains("Goodreads") })

        val lightNovel = ExternalLinkHelper.buildOpenOptionsForEntry(
            MediaEntry(title = "Mushoku Tensei", mediaType = MediaType.BOOK, status = MediaStatus.WATCHING, subCategory = "Light Novel", externalId = "55161", externalSource = "MAL")
        )
        assertTrue(lightNovel.any { it.title.contains("AniList") })
        assertTrue(lightNovel.any { it.title.contains("MyAnimeList") })
        assertTrue(lightNovel.any { it.title.contains("BookWalker") })
        assertTrue(lightNovel.any { it.title.contains("MangaDex") })

        val movie = ExternalLinkHelper.buildOpenOptionsForEntry(
            MediaEntry(title = "Inception", mediaType = MediaType.MOVIE, status = MediaStatus.COMPLETED, externalId = "27205", externalSource = "TMDB")
        )
        assertTrue(movie.any { it.title.contains("TMDB") })
        assertTrue(movie.any { it.title.contains("IMDb") })
        assertTrue(movie.any { it.title.contains("Rotten Tomatoes") })

        val show = ExternalLinkHelper.buildOpenOptionsForEntry(
            MediaEntry(title = "Severance", mediaType = MediaType.SHOW, status = MediaStatus.WATCHING, externalId = "95396", externalSource = "TMDB")
        )
        assertEquals(movie.map { it.title }, show.map { it.title })

        val game = ExternalLinkHelper.buildOpenOptionsForEntry(
            MediaEntry(title = "Hollow Knight", mediaType = MediaType.GAME, status = MediaStatus.COMPLETED, externalId = "hollow-knight", externalSource = "IGDB")
        )
        assertTrue(game.any { it.title.contains("IGDB") })
        assertTrue(game.any { it.title.contains("RAWG") })

        val music = ExternalLinkHelper.buildOpenOptionsForEntry(
            MediaEntry(title = "Numb", creator = "Linkin Park", mediaType = MediaType.MUSIC, status = MediaStatus.COMPLETED)
        )
        assertTrue(music.any { it.title.contains("MusicBrainz") })
        assertTrue(music.any { it.title.contains("Apple Music") })
        assertTrue(music.any { it.title.contains("Spotify") })
        assertTrue(music.any { it.title.contains("YouTube Music") })
    }

    @Test
    fun `unreleased Music Game and Movie entries produce countdown schedule and release badges`() {
        val futureEpoch = System.currentTimeMillis() + 5L * 24 * 60 * 60 * 1000L

        val music = MediaEntry(
            title = "Upcoming Single",
            mediaType = MediaType.MUSIC,
            status = MediaStatus.PLAN_TO_WATCH,
            airedStartDate = futureEpoch
        )
        assertTrue(music.isNotYetAiredOrPublished)
        assertEquals("UNRELEASED", music.releaseBadge())
        assertTrue(music.getTimeUntilNextEpisode(prefixIn = true)?.startsWith("In ") == true)
        assertNotNull(music.formatAiringSchedule(short = false))

        val game = music.copy(mediaType = MediaType.GAME, title = "Upcoming Game")
        assertTrue(game.isNotYetAiredOrPublished)
        assertEquals("UNRELEASED", game.releaseBadge())
        assertTrue(game.getTimeUntilNextEpisode(prefixIn = true)?.startsWith("In ") == true)

        val movie = music.copy(mediaType = MediaType.MOVIE, title = "Upcoming Movie")
        assertTrue(movie.isNotYetAiredOrPublished)
        assertEquals("NYA", movie.releaseBadge())
        assertTrue(movie.getTimeUntilNextEpisode(prefixIn = true)?.startsWith("In ") == true)
    }
}
