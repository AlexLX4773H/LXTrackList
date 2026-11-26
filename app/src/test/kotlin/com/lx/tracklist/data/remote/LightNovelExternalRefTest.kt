package com.lx.tracklist.data.remote

import com.lx.tracklist.data.importer.CsvImporter
import com.lx.tracklist.data.importer.JsonImporter
import com.lx.tracklist.data.importer.MalXmlImporter
import com.lx.tracklist.data.model.ExternalSources
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.remote.mal.MalMangaListItem
import com.lx.tracklist.data.remote.mal.MalMangaListStatus
import com.lx.tracklist.data.remote.mal.MalMangaNode
import com.lx.tracklist.data.remote.mal.MalSyncManager
import com.lx.tracklist.ui.search.SearchResultItem
import com.lx.tracklist.ui.search.buildJikanOrBookLibraryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LightNovelExternalRefTest {

    @Test
    fun `light novel with idMal from AniList prefers MAL ref and preserves AniList fallback when unlinked to MAL`() {
        val lnWithMal = JikanMediaDetails(
            malId = 55161L,
            aniListId = 85161L,
            title = "Mushoku Tensei: Jobless Reincarnation",
            type = "Light Novel",
            sourceProvider = "AniList",
            url = "https://anilist.co/manga/85161",
            externalScore = 8.4f,
            externalScoreSource = "AniList"
        )
        assertEquals(ExternalSources.MAL to "55161", lnWithMal.preferredExternalRef())
        assertEquals(ExternalSources.MAL to "55161", TachiyomiResolverManager.extractCandidateSourceAndId(lnWithMal))

        val lnOnlyAniList = JikanMediaDetails(
            malId = null,
            aniListId = 109988L,
            title = "Original AniList Web Novel",
            type = "Light Novel",
            sourceProvider = "AniList",
            url = "https://anilist.co/manga/109988"
        )
        assertEquals(ExternalSources.ANILIST to "109988", lnOnlyAniList.preferredExternalRef())
        assertEquals(ExternalSources.ANILIST to "109988", TachiyomiResolverManager.extractCandidateSourceAndId(lnOnlyAniList))
    }

    @Test
    fun `BookDetails preferredExternalRef canonicalizes provider`() {
        val lnBookDetails = BookDetails(
            id = "55161",
            title = "Mushoku Tensei",
            sourceProvider = "mal",
            scoreSource = "AniList"
        )
        assertEquals(ExternalSources.MAL to "55161", lnBookDetails.preferredExternalRef())
        assertEquals("AniList", lnBookDetails.scoreSource)
    }

    @Test
    fun `BookSyncManager detects mismatched AniList title and picks confident LN match only`() {
        val corruptedEntry = MediaEntry(
            title = "Mushoku Tensei: Jobless Reincarnation",
            mediaType = MediaType.BOOK,
            status = MediaStatus.WATCHING,
            subCategory = "Light Novel",
            externalSource = "AniList",
            externalId = "55161" // actually MAL ID, resolves on AniList to some unrelated manga
        )
        val unrelatedAniListById = JikanMediaDetails(
            malId = null,
            aniListId = 55161L,
            title = "Some Totally Unrelated Manga",
            sourceProvider = "AniList"
        )
        assertTrue(BookSyncManager.isAniListTitleMismatch(corruptedEntry, unrelatedAniListById))
        assertTrue(BookSyncManager.isAniListTitleMismatch(corruptedEntry, null))

        val validAniListById = JikanMediaDetails(
            malId = 55161L,
            aniListId = 85161L,
            title = "Mushoku Tensei: Jobless Reincarnation",
            sourceProvider = "AniList"
        )
        assertFalse(BookSyncManager.isAniListTitleMismatch(corruptedEntry, validAniListById))

        val candidates = listOf(
            JikanMediaDetails(malId = 999L, aniListId = 888L, title = "Mushoku Tensei Spinoff", sourceProvider = "MAL (AniList)"),
            validAniListById
        )
        val confident = BookSyncManager.selectConfidentLightNovelMatch(corruptedEntry, candidates)
        assertEquals(ExternalSources.MAL to "55161", confident?.preferredExternalRef())

        val noConfident = BookSyncManager.selectConfidentLightNovelMatch(
            corruptedEntry,
            listOf(candidates[0])
        )
        assertNull(noConfident)
    }

    @Test
    fun `adding light novel from Search preserves Light Novel identity and chapter counts`() {
        val lnDetails = JikanMediaDetails(
            malId = 55161L,
            aniListId = 85161L,
            title = "Mushoku Tensei: Jobless Reincarnation",
            type = "Light Novel",
            totalChapters = 285,
            totalVolumes = 26,
            isAiring = false,
            sourceProvider = "AniList",
            externalScore = 8.4f,
            externalScoreSource = "AniList"
        )
        val searchItem = SearchResultItem(
            externalId = "55161",
            externalSource = ExternalSources.MAL,
            mediaType = MediaType.BOOK,
            title = "Mushoku Tensei: Jobless Reincarnation",
            totalUnits = 285,
            totalVolumes = 26,
            statusText = "NYP",
            rawJikanDetails = lnDetails,
            isLightNovel = true
        )
        assertTrue(searchItem.isLightNovelResult())

        val addedEntry = buildJikanOrBookLibraryEntry(
            item = searchItem,
            targetUserId = 1L,
            status = MediaStatus.PLAN_TO_WATCH
        )
        assertEquals(MediaType.BOOK, addedEntry.mediaType)
        assertEquals("Light Novel", addedEntry.subCategory)
        assertEquals(285, addedEntry.totalChapters)
        assertEquals(26, addedEntry.totalVolumes)
        assertEquals(0, addedEntry.totalPages)
        assertEquals(TrackingUnit.CHAPTERS, addedEntry.trackingUnit)
        assertTrue(addedEntry.isLightNovel())
        assertTrue(MediaEntry.isLightNovelCategory(MediaType.BOOK, null, addedEntry.subCategory))

        val regularBook = SearchResultItem(
            externalId = "OL123W",
            externalSource = ExternalSources.OPEN_LIBRARY,
            mediaType = MediaType.BOOK,
            title = "Dune",
            totalUnits = 412,
            statusText = "Book",
            isLightNovel = false
        )
        assertFalse(regularBook.isLightNovelResult())
        val addedBook = buildJikanOrBookLibraryEntry(
            item = regularBook,
            targetUserId = 1L,
            status = MediaStatus.PLAN_TO_WATCH
        )
        assertEquals(MediaType.BOOK, addedBook.mediaType)
        assertNull(addedBook.subCategory)
        assertEquals(412, addedBook.totalPages)
        assertEquals(0, addedBook.totalChapters)
        assertEquals(TrackingUnit.PAGES, addedBook.trackingUnit)
        assertFalse(addedBook.isLightNovel())
    }

    @Test
    fun `MAL pull and CSV JSON XML imports identify Light Novels consistently`() {
        val malNovel = MalMangaListItem(
            node = MalMangaNode(
                id = 55161L,
                title = "Mushoku Tensei",
                numVolumes = 26,
                numChapters = 285,
                mediaType = "light_novel"
            ),
            listStatus = MalMangaListStatus(status = "reading", numChaptersRead = 50, numVolumesRead = 4)
        )
        val mappedNovel = MalSyncManager.mapMalMangaItemToEntry(malNovel, userId = 1L)
        assertEquals(MediaType.BOOK, mappedNovel.mediaType)
        assertEquals("Light Novel", mappedNovel.subCategory)
        assertEquals(285, mappedNovel.totalChapters)
        assertEquals(26, mappedNovel.totalVolumes)
        assertEquals(TrackingUnit.CHAPTERS, mappedNovel.trackingUnit)
        assertTrue(mappedNovel.isLightNovel())

        val malManga = MalMangaListItem(
            node = MalMangaNode(id = 13L, title = "One Piece", numVolumes = 109, numChapters = 1120, mediaType = "manga"),
            listStatus = MalMangaListStatus(status = "reading", numChaptersRead = 1000)
        )
        val mappedManga = MalSyncManager.mapMalMangaItemToEntry(malManga, userId = 1L)
        assertEquals(MediaType.MANGA, mappedManga.mediaType)
        assertNull(mappedManga.subCategory)

        val csv = "Title,Type,Status,Total Chapters,External ID,External Source\nAscendance of a Bookworm,Light Novel,Watching,677,86119,MAL"
        val csvEntry = CsvImporter.parse(csv.byteInputStream()).single()
        assertEquals(MediaType.BOOK, csvEntry.mediaType)
        assertEquals("Light Novel", csvEntry.subCategory)
        assertTrue(csvEntry.isLightNovel())

        val json = """[{"title":"Spice and Wolf","mediaType":"Light Novel","totalChapters":140,"externalId":"9115","externalSource":"MAL"}]"""
        val jsonEntry = JsonImporter.parse(json.byteInputStream()).single()
        assertEquals(MediaType.BOOK, jsonEntry.mediaType)
        assertEquals("Light Novel", jsonEntry.subCategory)
        assertTrue(jsonEntry.isLightNovel())

        val xml = """
            <myanimelist>
              <manga>
                <manga_mangadb_id>55161</manga_mangadb_id>
                <manga_title>Mushoku Tensei</manga_title>
                <manga_type>Light Novel</manga_type>
                <manga_volumes>26</manga_volumes>
                <manga_chapters>285</manga_chapters>
                <my_status>Reading</my_status>
              </manga>
            </myanimelist>
        """.trimIndent()
        val xmlEntry = MalXmlImporter.parse(xml.byteInputStream()).single()
        assertEquals(MediaType.BOOK, xmlEntry.mediaType)
        assertEquals("Light Novel", xmlEntry.subCategory)
        assertEquals(TrackingUnit.CHAPTERS, xmlEntry.trackingUnit)
        assertTrue(xmlEntry.isLightNovel())
    }
}
