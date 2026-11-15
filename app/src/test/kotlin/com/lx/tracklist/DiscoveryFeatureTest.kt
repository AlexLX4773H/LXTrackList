package com.lx.tracklist

import com.lx.tracklist.ui.search.AdultMode
import com.lx.tracklist.ui.search.AnimeSeason
import com.lx.tracklist.ui.search.AnimeSortOrder
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
