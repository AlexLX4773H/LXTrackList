package com.lx.tracklist.data.model

import com.lx.tracklist.ui.components.SortOption
import com.lx.tracklist.ui.home.resolveHomeActiveCategory
import com.lx.tracklist.ui.library.LibrarySortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CustomCategoryTest {

    @Test
    fun `effectiveMediaType returns explicit mediaType when set`() {
        val donghua = CustomCategory(name = "Donghua", mediaType = MediaType.ANIME, isWatchType = true)
        val indieGames = CustomCategory(name = "Indie", mediaType = MediaType.GAME, isWatchType = false)
        val lightNovels = CustomCategory(name = "Light Novels", mediaType = MediaType.BOOK, isWatchType = false)
        val soundtrack = CustomCategory(name = "OST", mediaType = MediaType.MUSIC, isWatchType = true)

        assertEquals(MediaType.ANIME, donghua.effectiveMediaType())
        assertEquals(MediaType.GAME, indieGames.effectiveMediaType())
        assertEquals(MediaType.BOOK, lightNovels.effectiveMediaType())
        assertEquals(MediaType.MUSIC, soundtrack.effectiveMediaType())
    }

    @Test
    fun `effectiveMediaType falls back based on isWatchType when mediaType is null`() {
        val watchCategory = CustomCategory(name = "Cartoons", mediaType = null, isWatchType = true)
        val readCategory = CustomCategory(name = "Web Novels", mediaType = null, isWatchType = false)

        assertEquals(MediaType.ANIME, watchCategory.effectiveMediaType())
        assertEquals(MediaType.MANGA, readCategory.effectiveMediaType())
    }

    @Test
    fun `effectiveMediaTypeFor resolves case-insensitively and handles missing or null names`() {
        val categories = listOf(
            CustomCategory(name = "Donghua", mediaType = null, isWatchType = true),
            CustomCategory(name = "Web Novels", mediaType = MediaType.BOOK, isWatchType = false),
            CustomCategory(name = "Comics", mediaType = null, isWatchType = false)
        )

        assertEquals(MediaType.ANIME, categories.effectiveMediaTypeFor("donghua"))
        assertEquals(MediaType.BOOK, categories.effectiveMediaTypeFor("WEB NOVELS"))
        assertEquals(MediaType.MANGA, categories.effectiveMediaTypeFor("Comics"))
        assertNull(categories.effectiveMediaTypeFor("Unknown"))
        assertNull(categories.effectiveMediaTypeFor(null))
    }

    @Test
    fun `validateCustomCategoryName rejects colliding built-in, singular, All, and duplicate names`() {
        val existing = listOf("Donghua", "Web Novels")

        assertNotNull(validateCustomCategoryName("manhwa", existing))
        assertNotNull(validateCustomCategoryName("ANIME", existing))
        assertNotNull(validateCustomCategoryName("TV Shows", existing))
        assertNotNull(validateCustomCategoryName("TV Show", existing))
        assertNotNull(validateCustomCategoryName("Track", existing))
        assertNotNull(validateCustomCategoryName("All", existing))
        assertNotNull(validateCustomCategoryName("  all  ", existing))
        assertNotNull(validateCustomCategoryName("donghua", existing))
        assertNotNull(validateCustomCategoryName("   ", existing))

        assertNull(validateCustomCategoryName("Audiobooks", existing))
        assertNull(validateCustomCategoryName("Visual Novels", existing))
    }

    @Test
    fun `summarySubtitle formats base type and tracking unit for all category families`() {
        val donghua = CustomCategory(name = "Donghua", mediaType = MediaType.ANIME, isWatchType = true)
        val doc = CustomCategory(name = "Documentaries", mediaType = MediaType.MOVIE, isWatchType = true)
        val drama = CustomCategory(name = "K-Drama", mediaType = MediaType.SHOW, isWatchType = true)
        val webNovel = CustomCategory(
            name = "Web Novels",
            mediaType = MediaType.BOOK,
            isWatchType = false,
            trackingUnit = TrackingUnit.PAGES
        )
        val visualNovel = CustomCategory(
            name = "Visual Novels",
            mediaType = MediaType.GAME,
            isWatchType = false
        )
        val ost = CustomCategory(
            name = "Soundtracks",
            mediaType = MediaType.MUSIC,
            isWatchType = false
        )

        assertEquals("Anime • Episodes", donghua.summarySubtitle())
        assertEquals("Movies • Feature", doc.summarySubtitle())
        assertEquals("TV Shows • Episodes", drama.summarySubtitle())
        assertEquals("Books • Pages", webNovel.summarySubtitle())
        assertEquals("Games • Progress", visualNovel.summarySubtitle())
        assertEquals("Music • Tracks", ost.summarySubtitle())

        val legacyReadPercent = CustomCategory(
            name = "Legacy Webtoons",
            mediaType = null,
            isWatchType = false,
            trackingUnit = TrackingUnit.PERCENTAGE
        )
        assertEquals("Manga • Chapters", legacyReadPercent.summarySubtitle())
    }

    @Test
    fun `resolveHomeActiveCategory uses custom category base type regardless of fallback`() {
        val ost = CustomCategory(name = "Soundtracks", mediaType = MediaType.MUSIC, isWatchType = false)
        val vn = CustomCategory(name = "Visual Novels", mediaType = MediaType.GAME, isWatchType = false)
        val categories = listOf(ost, vn)

        assertEquals(MediaType.MUSIC, resolveHomeActiveCategory(categories, "soundtracks", MediaType.ANIME))
        assertEquals(MediaType.GAME, resolveHomeActiveCategory(categories, "Visual Novels", MediaType.MANGA))
        assertEquals(MediaType.ANIME, resolveHomeActiveCategory(categories, null, MediaType.ANIME))
        assertEquals(MediaType.BOOK, resolveHomeActiveCategory(categories, "Missing", MediaType.BOOK))
    }

    @Test
    fun `sort options and status labels honor resolved custom category base type`() {
        val ost = CustomCategory(name = "Soundtracks", mediaType = MediaType.MUSIC, isWatchType = false)
        val vn = CustomCategory(name = "Visual Novels", mediaType = MediaType.GAME, isWatchType = false)
        val webNovel = CustomCategory(name = "Web Novels", mediaType = MediaType.BOOK, isWatchType = false)
        val cats = listOf(ost, vn, webNovel)

        assertEquals("Most Time Listened", SortOption.TIME_SPENT_DESC.getDisplayLabel(null, "Soundtracks", cats))
        assertEquals("Most Time To Listen", SortOption.TIME_REMAINING_DESC.getDisplayLabel(null, "Soundtracks", cats))
        assertEquals("Most Time Played", SortOption.TIME_SPENT_DESC.getDisplayLabel(null, "Visual Novels", cats))
        assertEquals("Most Time To Play", SortOption.TIME_REMAINING_DESC.getDisplayLabel(null, "Visual Novels", cats))
        assertEquals("Most Relistened", LibrarySortOption.REWATCH_DESC.getDisplayLabel(null, "Soundtracks", cats))
        assertEquals("Most Replayed", LibrarySortOption.REWATCH_DESC.getDisplayLabel(null, "Visual Novels", cats))
        assertEquals("Most Time Read", LibrarySortOption.TIME_SPENT_DESC.getDisplayLabel(null, "Web Novels", cats))
        assertEquals("Listening", MediaStatus.WATCHING.getDisplayLabel(resolveHomeActiveCategory(cats, "Soundtracks", MediaType.ANIME)))
        assertEquals("Playing", MediaStatus.WATCHING.getDisplayLabel(resolveHomeActiveCategory(cats, "Visual Novels", MediaType.ANIME)))
    }
}
