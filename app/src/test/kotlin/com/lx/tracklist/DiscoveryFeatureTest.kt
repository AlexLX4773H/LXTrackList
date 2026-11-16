package com.lx.tracklist

import com.lx.tracklist.ui.search.AdultMode
import com.lx.tracklist.ui.search.AnimeDiscoveryMode
import com.lx.tracklist.ui.search.AnimeSeason
import com.lx.tracklist.ui.search.AnimeSortOrder
import com.lx.tracklist.ui.search.AnimeTagCategory
import com.lx.tracklist.ui.search.AnimeTagRegistry
import com.lx.tracklist.ui.search.DiscoveryFilter
import com.lx.tracklist.ui.search.TmdbMovieSort
import com.lx.tracklist.ui.search.TmdbTvSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryFeatureTest {

    @Test
    fun testAnimeDiscoveryModesAndTags() {
        assertEquals("Seasonal", AnimeDiscoveryMode.SEASONAL.displayName)
        assertEquals("Most Popular", AnimeDiscoveryMode.MOST_POPULAR.displayName)
        assertEquals(AnimeSeason.current().emoji, AnimeDiscoveryMode.SEASONAL.iconEmoji)
        assertEquals("🔥", AnimeDiscoveryMode.MOST_POPULAR.iconEmoji)

        // Tag registry lookup
        val actionTag = AnimeTagRegistry.findTag("Action")
        assertNotNull(actionTag)
        assertEquals(1, actionTag?.malId)
        assertEquals(AnimeTagCategory.GENRE, actionTag?.category)

        val isekaiTag = AnimeTagRegistry.findTag("Isekai")
        assertNotNull(isekaiTag)
        assertEquals(61, isekaiTag?.malId)
        assertEquals(AnimeTagCategory.THEME, isekaiTag?.category)

        val shounenTag = AnimeTagRegistry.findTag("Shounen")
        assertNotNull(shounenTag)
        assertEquals(27, shounenTag?.malId)
        assertEquals(AnimeTagCategory.DEMOGRAPHIC, shounenTag?.category)

        // Tag filtering by category
        val genres = AnimeTagRegistry.filterTags("", AnimeTagCategory.GENRE)
        assertTrue(genres.any { it.name == "Action" })
        assertTrue(genres.none { it.name == "Isekai" })

        // Tag search query
        val searchResults = AnimeTagRegistry.filterTags("ise")
        assertTrue(searchResults.any { it.name == "Isekai" })
    }

    @Test
    fun testAnimeDiscoveryFilterLabels() {
        // 1. Most Popular mode
        val popFilter = DiscoveryFilter.Anime(
            mode = AnimeDiscoveryMode.MOST_POPULAR,
            sortOrder = AnimeSortOrder.POPULARITY
        )
        assertEquals("🔥 Most Popular • Most Popular", popFilter.label)

        // 2. Mode with tags (+Action, -Ecchi)
        val tagFilter = DiscoveryFilter.Anime(
            mode = AnimeDiscoveryMode.SEASONAL,
            season = AnimeSeason.FALL,
            year = 2026,
            sortOrder = AnimeSortOrder.POPULARITY,
            includedTags = setOf("Action", "Isekai"),
            excludedTags = setOf("Ecchi")
        )
        assertEquals("🍂 Fall 2026 • Most Popular • +2 / -1 tags", tagFilter.label)
    }

    @Test
    fun testAnimeSeasonLogic() {
        assertNotNull(AnimeSeason.current())
        assertTrue(AnimeSeason.currentYear() >= 2024)
        assertEquals(AnimeSeason.currentYear() + 2, AnimeSeason.maxDiscoveryYear())

        val (nextSeason, nextYear) = AnimeSeason.nextSeason(AnimeSeason.FALL)
        assertEquals(AnimeSeason.WINTER, nextSeason)
        assertEquals(AnimeSeason.currentYear() + 1, nextYear)

        val (springSeason, springYear) = AnimeSeason.nextSeason(AnimeSeason.WINTER)
        assertEquals(AnimeSeason.SPRING, springSeason)
        assertEquals(AnimeSeason.currentYear(), springYear)
    }

    @Test
    fun testAdultModeCycle() {
        assertEquals(AdultMode.INCLUDE, AdultMode.OFF.next())
        assertEquals(AdultMode.ONLY, AdultMode.INCLUDE.next())
        assertEquals(AdultMode.OFF, AdultMode.ONLY.next())
    }

    @Test
    fun testAnimeFilterLabel() {
        val filterOff = DiscoveryFilter.Anime(
            season = AnimeSeason.FALL,
            year = 2024,
            sortOrder = AnimeSortOrder.POPULARITY,
            adultMode = AdultMode.OFF
        )
        assertEquals("🍂 Fall 2024 • Most Popular", filterOff.label)
        assertFalse(filterOff.includeAdult)

        val filterInclude = DiscoveryFilter.Anime(
            season = AnimeSeason.SPRING,
            year = 2025,
            sortOrder = AnimeSortOrder.SCORE,
            adultMode = AdultMode.INCLUDE
        )
        assertEquals("🌸 Spring 2025 • Highest Rated • 18+", filterInclude.label)
        assertTrue(filterInclude.includeAdult)

        val filterOnly = DiscoveryFilter.Anime(
            season = AnimeSeason.WINTER,
            year = 2026,
            sortOrder = AnimeSortOrder.POPULARITY,
            adultMode = AdultMode.ONLY
        )
        assertEquals("❄️ Winter 2026 • Most Popular • 18+ Only", filterOnly.label)
        assertTrue(filterOnly.includeAdult)
    }

    @Test
    fun testMovieFilterLabel() {
        val filter = DiscoveryFilter.Movie(
            year = 2027,
            sortOrder = TmdbMovieSort.POPULARITY,
            adultMode = AdultMode.ONLY
        )
        assertEquals("🎬 Movies 2027 • Most Popular • 18+ Only", filter.label)
        assertTrue(filter.includeAdult)
    }

    @Test
    fun testTvFilterLabel() {
        val filter = DiscoveryFilter.Tv(
            year = 2025,
            sortOrder = TmdbTvSort.RATING,
            adultMode = AdultMode.INCLUDE
        )
        assertEquals("📺 TV Shows 2025 • Highest Rated • 18+", filter.label)
        assertTrue(filter.includeAdult)
    }

    @Test
    fun testSortOrderApiValues() {
        assertEquals("POPULARITY_DESC", AnimeSortOrder.POPULARITY.apiValue)
        assertEquals("SCORE_DESC", AnimeSortOrder.SCORE.apiValue)
        assertEquals("START_DATE_DESC", AnimeSortOrder.AIR_DATE.apiValue)
        assertEquals("TITLE_ROMAJI", AnimeSortOrder.TITLE.apiValue)

        assertEquals("popularity.desc", TmdbMovieSort.POPULARITY.apiValue)
        assertEquals("vote_average.desc", TmdbMovieSort.RATING.apiValue)
        assertEquals("primary_release_date.desc", TmdbMovieSort.RELEASE_DATE.apiValue)
        assertEquals("revenue.desc", TmdbMovieSort.REVENUE.apiValue)

        assertEquals("popularity.desc", TmdbTvSort.POPULARITY.apiValue)
        assertEquals("vote_average.desc", TmdbTvSort.RATING.apiValue)
        assertEquals("first_air_date.desc", TmdbTvSort.RELEASE_DATE.apiValue)
    }
}
