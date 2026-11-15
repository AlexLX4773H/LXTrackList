package com.lx.tracklist

import com.lx.tracklist.ui.search.AnimeSeason
import com.lx.tracklist.ui.search.AnimeSortOrder
import com.lx.tracklist.ui.search.DiscoveryFilter
import com.lx.tracklist.ui.search.TmdbMovieSort
import com.lx.tracklist.ui.search.TmdbTvSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryFeatureTest {

    @Test
    fun testAnimeSeasonLogic() {
        assertNotNull(AnimeSeason.current())
        assertTrue(AnimeSeason.currentYear() >= 2024)

        val (nextSeason, nextYear) = AnimeSeason.nextSeason(AnimeSeason.FALL)
        assertEquals(AnimeSeason.WINTER, nextSeason)
        assertEquals(AnimeSeason.currentYear() + 1, nextYear)

        val (springSeason, springYear) = AnimeSeason.nextSeason(AnimeSeason.WINTER)
        assertEquals(AnimeSeason.SPRING, springSeason)
        assertEquals(AnimeSeason.currentYear(), springYear)
    }

    @Test
    fun testAnimeFilterLabel() {
        val filter = DiscoveryFilter.Anime(
            season = AnimeSeason.FALL,
            year = 2024,
            sortOrder = AnimeSortOrder.POPULARITY
        )
        assertEquals("🍂 Fall 2024 • Most Popular", filter.label)
    }

    @Test
    fun testMovieFilterLabel() {
        val filter = DiscoveryFilter.Movie(
            year = 2024,
            sortOrder = TmdbMovieSort.POPULARITY
        )
        assertEquals("🎬 Movies 2024 • Most Popular", filter.label)
    }

    @Test
    fun testTvFilterLabel() {
        val filter = DiscoveryFilter.Tv(
            year = 2024,
            sortOrder = TmdbTvSort.RATING
        )
        assertEquals("📺 TV Shows 2024 • Highest Rated", filter.label)
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
