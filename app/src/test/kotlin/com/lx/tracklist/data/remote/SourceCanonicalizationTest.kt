package com.lx.tracklist.data.remote

import com.google.gson.JsonParser
import com.lx.tracklist.data.model.ExternalSources
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.ui.edit.AddEditUiState
import com.lx.tracklist.ui.edit.applyEdits
import com.lx.tracklist.ui.search.resolveJikanScoreSource
import com.lx.tracklist.ui.search.resolveJikanSearchExternalRef
import com.lx.tracklist.util.ExternalLinkHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SourceCanonicalizationTest {

    @Test
    fun `kitsu preferredExternalRef prefers explicit numeric kitsuId over url slug or resolver path`() {
        val withExplicitAndResolverUrl = JikanMediaDetails(
            malId = null,
            title = "Solo Leveling",
            sourceProvider = "Kitsu",
            url = "kitsu-resolve://manga?title=Solo+Leveling&kitsuId=41370",
            kitsuId = "41370"
        )
        assertEquals(ExternalSources.KITSU to "41370", withExplicitAndResolverUrl.preferredExternalRef())

        val withExplicitAndSlugUrl = JikanMediaDetails(
            malId = null,
            title = "Solo Leveling",
            sourceProvider = "Kitsu",
            url = "https://kitsu.app/manga/solo-leveling",
            kitsuId = "41370"
        )
        assertEquals(ExternalSources.KITSU to "41370", withExplicitAndSlugUrl.preferredExternalRef())

        val withResolverQueryParamOnly = JikanMediaDetails(
            malId = null,
            title = "Solo Leveling",
            sourceProvider = "Kitsu",
            url = "kitsu-resolve://manga?title=Solo+Leveling&kitsuId=41370",
            kitsuId = null
        )
        assertEquals(ExternalSources.KITSU to "41370", withResolverQueryParamOnly.preferredExternalRef())
    }

    @Test
    fun `jikan and anilist parsers emit canonical sourceProvider without Jikan or MAL (AniList) drift`() {
        val jikanJson = JsonParser.parseString(
            """
            {
              "mal_id": 5114,
              "title": "Fullmetal Alchemist: Brotherhood",
              "url": "https://myanimelist.net/anime/5114/Fullmetal_Alchemist__Brotherhood",
              "score": 9.1,
              "images": { "jpg": { "large_image_url": "https://cdn.myanimelist.net/images/anime/1208/94745l.jpg" } }
            }
            """.trimIndent()
        ).asJsonObject
        val jikanDetails = JikanApiClient.parseJikanData(jikanJson, fallbackMalId = null)
        assertEquals(ExternalSources.MAL, jikanDetails.sourceProvider)
        assertEquals(ExternalSources.MAL, jikanDetails.externalScoreSource)
        assertEquals(ExternalSources.MAL to "5114", jikanDetails.preferredExternalRef())

        val aniJson = JsonParser.parseString(
            """
            {
              "id": 5114,
              "idMal": 5114,
              "title": { "english": "Fullmetal Alchemist: Brotherhood", "romaji": "Hagane no Renkinjutsushi" },
              "averageScore": 90,
              "siteUrl": "https://anilist.co/anime/5114",
              "format": "TV",
              "countryOfOrigin": "JP"
            }
            """.trimIndent()
        ).asJsonObject
        val aniDetails = JikanApiClient.parseAniListMedia(aniJson, fallbackMalId = null)
        assertEquals(ExternalSources.ANILIST, aniDetails.sourceProvider)
        assertEquals(ExternalSources.ANILIST, aniDetails.externalScoreSource)
        assertEquals(ExternalSources.MAL to "5114", aniDetails.preferredExternalRef())

        val lnDetails = AniListNovelApiClient.parseAniListNovelMedia(aniJson)
        assertEquals(ExternalSources.ANILIST, lnDetails.sourceProvider)
        assertEquals(ExternalSources.ANILIST, lnDetails.externalScoreSource)
        assertEquals(ExternalSources.MAL to "5114", lnDetails.preferredExternalRef())
    }

    @Test
    fun `search helpers keep primary ID source separate from AniList scoreSource and preserve Kitsu`() {
        val aniDiscoveryWithMal = JikanMediaDetails(
            malId = 5114L,
            aniListId = 5114L,
            title = "Fullmetal Alchemist: Brotherhood",
            sourceProvider = ExternalSources.ANILIST,
            externalScore = 9.0f,
            externalScoreSource = ExternalSources.ANILIST,
            url = "https://anilist.co/anime/5114"
        )
        val (src, id) = resolveJikanSearchExternalRef(aniDiscoveryWithMal, "anime", 0)
        assertEquals(ExternalSources.MAL, src)
        assertEquals("5114", id)
        assertEquals(ExternalSources.ANILIST, resolveJikanScoreSource(aniDiscoveryWithMal, src))

        val kitsuOnly = JikanMediaDetails(
            malId = null,
            title = "Kitsu Exclusive",
            sourceProvider = ExternalSources.KITSU,
            kitsuId = "41370",
            externalScore = 8.4f,
            externalScoreSource = ExternalSources.KITSU,
            url = "kitsu-resolve://anime?title=Kitsu+Exclusive&kitsuId=41370"
        )
        val (kitsuSrc, kitsuResolvedId) = resolveJikanSearchExternalRef(kitsuOnly, "anime", 1)
        assertEquals(ExternalSources.KITSU, kitsuSrc)
        assertEquals("41370", kitsuResolvedId)
        assertEquals(ExternalSources.KITSU, resolveJikanScoreSource(kitsuOnly, kitsuSrc))
    }

    @Test
    fun `addEdit applyEdits and ExternalLinkHelper canonicalize legacy source aliases`() {
        val savedMal = applyEdits(
            original = null,
            s = AddEditUiState(
                userId = 1L,
                title = "Monster",
                mediaType = MediaType.ANIME,
                externalId = "19",
                externalSource = "MyAnimeList",
                externalScore = 8.9f,
                externalScoreSource = "Jikan"
            )
        )
        assertEquals(ExternalSources.MAL, savedMal.externalSource)
        assertEquals(ExternalSources.MAL, savedMal.externalScoreSource)

        val savedMusic = applyEdits(
            original = null,
            s = AddEditUiState(
                userId = 1L,
                title = "Numb",
                mediaType = MediaType.MUSIC,
                externalId = "528490497",
                externalSource = "iTunes",
                externalScoreSource = "iTunes"
            )
        )
        assertEquals(ExternalSources.APPLE_MUSIC, savedMusic.externalSource)
        assertEquals(ExternalSources.APPLE_MUSIC, savedMusic.externalScoreSource)

        val malLinks = ExternalLinkHelper.buildAnimeOpenOptions(
            title = "Monster",
            externalId = "19",
            externalSource = "MyAnimeList"
        )
        val malOpt = malLinks.firstOrNull { it.title.contains("MyAnimeList") }
        assertNotNull(malOpt)
        assertEquals("https://myanimelist.net/anime/19", malOpt?.url)

        val aniLinks = ExternalLinkHelper.buildMangaOpenOptions(
            title = "Solo Leveling",
            externalId = "105398",
            externalSource = "anilist"
        )
        val aniOpt = aniLinks.firstOrNull { it.title == "AniList" }
        assertNotNull(aniOpt)
        assertEquals("https://anilist.co/manga/105398", aniOpt?.url)
    }

    @Test
    fun `sync managers normalize legacy book and game sources to canonical values`() {
        val bookEntry = MediaEntry(
            id = 10L,
            userId = 1L,
            title = "Dune",
            mediaType = MediaType.BOOK,
            status = MediaStatus.COMPLETED,
            externalId = "vol123",
            externalSource = "google books"
        )
        val (updatedBook, changed) = BookSyncManager.applyBookMetadataToFresh(
            latest = bookEntry,
            bookDetails = BookDetails(
                id = "vol123",
                title = "Dune",
                sourceProvider = "Google Books",
                scoreSource = "Google Books",
                averageScore = 8.8f
            ),
            forceRefresh = true,
            now = 1_000L
        )
        assertEquals(true, changed)
        assertEquals(ExternalSources.GOOGLE_BOOKS, updatedBook.externalSource)
        assertEquals(ExternalSources.GOOGLE_BOOKS, updatedBook.externalScoreSource)

        val gameEntry = MediaEntry(
            id = 11L,
            userId = 1L,
            title = "Hades",
            mediaType = MediaType.GAME,
            status = MediaStatus.COMPLETED,
            externalId = "113112",
            externalSource = "igdb"
        )
        val updatedGame = GameSyncManager.applySyncedGameDetails(
            entry = gameEntry,
            details = GameDetails(
                externalId = "113112",
                externalSource = "igdb",
                title = "Hades",
                rating = 9.3f,
                ratingSource = "igdb"
            ),
            now = 1_000L
        )
        assertEquals(ExternalSources.IGDB, updatedGame.externalSource)
        assertEquals(ExternalSources.IGDB, updatedGame.externalScoreSource)
    }
}
