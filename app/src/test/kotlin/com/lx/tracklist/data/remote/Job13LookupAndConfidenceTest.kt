package com.lx.tracklist.data.remote

import com.lx.tracklist.data.model.ExternalSources
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class Job13LookupAndConfidenceTest {

    private lateinit var origJikanClient: OkHttpClient
    private lateinit var origMangaDexClient: OkHttpClient
    private lateinit var origKitsuClient: OkHttpClient

    @Before
    fun setUp() {
        origJikanClient = JikanApiClient.searchOkHttpClient
        origMangaDexClient = MangaDexApiClient.okHttpClient
        origKitsuClient = KitsuApiClient.okHttpClient
    }

    @After
    fun tearDown() {
        JikanApiClient.searchOkHttpClient = origJikanClient
        MangaDexApiClient.okHttpClient = origMangaDexClient
        KitsuApiClient.okHttpClient = origKitsuClient
    }

    @Test
    fun `stored ID lookup runs first regardless of manhwa type`() {
        val malManhwa = MediaEntry(
            title = "Solo Leveling",
            mediaType = MediaType.MANHWA,
            status = MediaStatus.WATCHING,
            externalSource = "MAL",
            externalId = "121496"
        )
        val aniManhwa = malManhwa.copy(externalSource = "AniList", externalId = "105398")
        val kitsuManga = malManhwa.copy(mediaType = MediaType.MANGA, externalSource = "Kitsu", externalId = "41370")
        val mdManhwa = malManhwa.copy(externalSource = "MangaDex", externalId = "32d76d19-8a05-4db0-9fc2-e0b0648fe9d0")
        val unlinked = malManhwa.copy(externalSource = null, externalId = null)

        assertEquals(
            JikanSyncManager.JikanLookupPlan(JikanSyncManager.JikanLookupKind.MAL_ID, "121496"),
            JikanSyncManager.resolveStoredLookupPlan(malManhwa)
        )
        assertEquals(
            JikanSyncManager.JikanLookupPlan(JikanSyncManager.JikanLookupKind.ANILIST_ID, "105398"),
            JikanSyncManager.resolveStoredLookupPlan(aniManhwa)
        )
        assertEquals(
            JikanSyncManager.JikanLookupPlan(JikanSyncManager.JikanLookupKind.KITSU_ID, "41370"),
            JikanSyncManager.resolveStoredLookupPlan(kitsuManga)
        )
        assertEquals(
            JikanSyncManager.JikanLookupPlan(JikanSyncManager.JikanLookupKind.MANGADEX_ID, "32d76d19-8a05-4db0-9fc2-e0b0648fe9d0"),
            JikanSyncManager.resolveStoredLookupPlan(mdManhwa)
        )
        assertEquals(
            JikanSyncManager.JikanLookupPlan(JikanSyncManager.JikanLookupKind.TITLE_SEARCH, "Solo Leveling"),
            JikanSyncManager.resolveStoredLookupPlan(unlinked)
        )
    }

    @Test
    fun `mal-linked manhwa queries by MAL ID before MangaDex title search and preserves MAL ID even on fallback`() {
        val requestedUrls = CopyOnWriteArrayList<String>()
        val anilistJson = """
            {
              "data": {
                "Media": {
                  "id": 105398,
                  "idMal": 121496,
                  "title": { "romaji": "Solo Leveling", "english": "Solo Leveling", "native": "나 혼자만 레벨업" },
                  "synonyms": [],
                  "coverImage": { "extraLarge": "https://cdn.example/solo.jpg" },
                  "chapters": 200,
                  "volumes": 14,
                  "status": "FINISHED",
                  "averageScore": 87,
                  "description": "E-rank hunter",
                  "genres": ["Action"],
                  "countryOfOrigin": "KR",
                  "staff": { "edges": [] }
                }
              }
            }
        """.trimIndent()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.toString()
                requestedUrls.add(url)
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(anilistJson.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        JikanApiClient.searchOkHttpClient = mockClient
        MangaDexApiClient.okHttpClient = mockClient

        val res = JikanApiClient.fetchMediaDetails(
            malId = 121496L,
            isManga = true,
            title = "Solo Leveling",
            isManhwa = true
        )

        assertTrue(res.isSuccess)
        assertEquals(121496L, res.getOrThrow().malId)
        assertTrue("AniList ID lookup must run first", requestedUrls.any { it.contains("graphql.anilist.co") })
        assertFalse("MangaDex title search must not preempt MAL ID lookup", requestedUrls.any { it.contains("api.mangadex.org") })

        // Even if metadata came from fallback MangaDex UUID, existing valid MAL ID is preserved
        val malManhwa = MediaEntry(
            id = 5L,
            title = "Solo Leveling",
            mediaType = MediaType.MANHWA,
            status = MediaStatus.WATCHING,
            externalSource = ExternalSources.MAL,
            externalId = "121496"
        )
        val mdFallbackDetails = JikanMediaDetails(
            malId = null,
            title = "Solo Leveling",
            posterUrl = "https://cdn.example/md_solo.jpg",
            sourceProvider = "MangaDex",
            url = "https://mangadex.org/title/32d76d19-8a05-4db0-9fc2-e0b0648fe9d0"
        )
        val (updated, hasChanges) = JikanSyncManager.applyJikanMetadataToFresh(
            latest = malManhwa,
            details = mdFallbackDetails,
            usedFallbackResolution = true
        )
        assertTrue(hasChanges)
        assertEquals(ExternalSources.MAL, updated.externalSource)
        assertEquals("121496", updated.externalId)
    }

    @Test
    fun `mangadex kitsu anilist and jikan title searches reject non-confident first hits`() {
        val mdNonMatchJson = """
            {
              "data": [
                {
                  "id": "11111111-1111-1111-1111-111111111111",
                  "attributes": {
                    "title": { "en": "Completely Unrelated Parody" },
                    "altTitles": [],
                    "description": { "en": "Wrong manga" },
                    "tags": []
                  }
                }
              ]
            }
        """.trimIndent()
        val kitsuNonMatchJson = """
            {
              "data": [
                {
                  "id": "999",
                  "attributes": {
                    "slug": "unrelated-series",
                    "canonicalTitle": "Unrelated Anthology",
                    "titles": { "en": "Unrelated Anthology" },
                    "synopsis": "Wrong"
                  }
                }
              ]
            }
        """.trimIndent()
        val jikanNonMatchJson = """
            {
              "data": [
                {
                  "mal_id": 77777,
                  "title": "Different Manga Spinoff",
                  "titles": [{ "type": "Default", "title": "Different Manga Spinoff" }],
                  "images": { "jpg": { "large_image_url": "https://cdn.example/wrong.jpg" } }
                }
              ]
            }
        """.trimIndent()
        val anilistNonMatchJson = """
            {
              "data": {
                "Media": {
                  "id": 88888,
                  "idMal": 88888,
                  "title": { "romaji": "Totally Different Work", "english": "Totally Different Work" },
                  "synonyms": [],
                  "coverImage": { "large": "https://cdn.example/wrong2.jpg" }
                }
              }
            }
        """.trimIndent()

        fun clientReturning(body: String) = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        MangaDexApiClient.okHttpClient = clientReturning(mdNonMatchJson)
        val mdRes = MangaDexApiClient.searchMangaDetailsByTitle("Specific Target Title")
        assertTrue("MangaDex must reject non-matching first result", mdRes.isFailure)

        KitsuApiClient.okHttpClient = clientReturning(kitsuNonMatchJson)
        val kitsuMangaRes = KitsuApiClient.searchMangaDetailsByTitle("Specific Target Title")
        val kitsuAnimeRes = KitsuApiClient.searchAnimeDetailsByTitle("Specific Target Title")
        assertTrue("Kitsu manga must reject non-matching first result", kitsuMangaRes.isFailure)
        assertTrue("Kitsu anime must reject non-matching first result", kitsuAnimeRes.isFailure)

        JikanApiClient.searchOkHttpClient = clientReturning(anilistNonMatchJson)
        val aniRes = JikanApiClient.queryAniListByTitle("Specific Target Title", isManga = true)
        assertTrue("AniList title search must reject non-matching hit", aniRes.isFailure)

        JikanApiClient.searchOkHttpClient = clientReturning(jikanNonMatchJson)
        val jikanRes = JikanApiClient.searchMediaDetailsByTitle("Specific Target Title", isManga = true)
        assertTrue("Jikan title search cascade must fail when no candidate matches confidently", jikanRes.isFailure)
    }
}
