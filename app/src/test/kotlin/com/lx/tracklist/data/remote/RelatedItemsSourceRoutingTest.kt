package com.lx.tracklist.data.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lx.tracklist.data.local.MediaEntryDao
import com.lx.tracklist.data.model.ExternalSources
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.RelatedMediaItem
import com.lx.tracklist.data.repository.MediaRepository
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class RelatedItemsSourceRoutingTest {

    private fun dummyRepo(): MediaRepository {
        val dao = Proxy.newProxyInstance(
            MediaEntryDao::class.java.classLoader,
            arrayOf(MediaEntryDao::class.java)
        ) { _, method, _ ->
            when (method.returnType) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                List::class.java -> emptyList<Any>()
                kotlinx.coroutines.flow.Flow::class.java -> emptyFlow<Any>()
                else -> null
            }
        } as MediaEntryDao
        return MediaRepository(dao = dao, userProfileDao = null)
    }

    @Test
    fun `parseAniListRelations assigns AniList source for AniList-only items and MAL source when idMal exists`() {
        val json = """
            {
              "relations": {
                "edges": [
                  {
                    "relationType": "SEQUEL",
                    "node": {
                      "id": 109988,
                      "idMal": null,
                      "type": "MANGA",
                      "title": { "english": "AniList Only Sequel" },
                      "averageScore": 82
                    }
                  },
                  {
                    "relationType": "SIDE_STORY",
                    "node": {
                      "id": 99999,
                      "idMal": 55555,
                      "type": "ANIME",
                      "title": { "english": "MAL Linked Side Story" },
                      "averageScore": 79
                    }
                  }
                ]
              }
            }
        """.trimIndent()
        val obj = Gson().fromJson(json, JsonObject::class.java)
        val parsed = JikanApiClient.parseAniListRelations(obj)
        assertEquals(2, parsed.size)

        assertEquals(ExternalSources.ANILIST, parsed[0].externalSource)
        assertEquals("109988", parsed[0].externalId)

        assertEquals(ExternalSources.MAL, parsed[1].externalSource)
        assertEquals("55555", parsed[1].externalId)
    }

    @Test
    fun `fetchRelatedEntries branches by canonical source without cross-contaminating IDs`() {
        // AniList source must query AniList ID directly and NEVER Jikan/MAL ID endpoints
        var queriedJikanMalId: Long? = null
        var queriedAniByMalId: Long? = null
        var queriedAniDirectId: Long? = null
        val aniItem = RelatedMediaItem(
            title = "AniList Item",
            relationship = "Sequel",
            externalId = "109988",
            externalSource = "anilist",
            mediaType = MediaType.MANGA
        )

        val aniRes = JikanApiClient.fetchRelatedEntriesInternal(
            malId = 109988L,
            isManga = true,
            title = "Test Work",
            externalSource = "AniList",
            externalId = "109988",
            fetchJikanByMalId = { queriedJikanMalId = it; Result.success(emptyList()) },
            fetchAniListByMalId = { queriedAniByMalId = it; Result.success(emptyList()) },
            fetchAniListByAniId = { queriedAniDirectId = it; Result.success(listOf(aniItem)) },
            fetchAniListByTitle = { Result.success(emptyList()) }
        )
        assertTrue(aniRes.isSuccess)
        assertEquals(109988L, queriedAniDirectId)
        assertNull("AniList ID must never be passed to Jikan", queriedJikanMalId)
        assertNull("AniList ID must never be passed as idMal", queriedAniByMalId)

        // MAL source must never fall back to sending MAL ID as AniList ID
        queriedJikanMalId = null
        queriedAniByMalId = null
        queriedAniDirectId = null
        var queriedTitle: String? = null
        val malRes = JikanApiClient.fetchRelatedEntriesInternal(
            malId = 5114L,
            isManga = false,
            title = "Fullmetal Alchemist",
            externalSource = "MAL",
            externalId = "5114",
            fetchJikanByMalId = { queriedJikanMalId = it; Result.failure(Exception("Jikan down")) },
            fetchAniListByMalId = { queriedAniByMalId = it; Result.success(emptyList()) },
            fetchAniListByAniId = { queriedAniDirectId = it; Result.success(emptyList()) },
            fetchAniListByTitle = { queriedTitle = it; Result.success(emptyList()) }
        )
        assertTrue(malRes.isSuccess)
        assertEquals(5114L, queriedJikanMalId)
        assertEquals(5114L, queriedAniByMalId)
        assertNull("MAL ID must never be retried as AniList ID", queriedAniDirectId)
        assertEquals("Fullmetal Alchemist", queriedTitle)
    }

    @Test
    fun `adding an AniList-only related item preserves AniList source and ID instead of overwriting with MAL`() {
        val repo = dummyRepo()
        val aniRelated = RelatedMediaItem(
            title = "AniList Only Novel",
            relationship = "Sequel",
            externalId = "109988",
            externalSource = "anilist",
            mediaType = MediaType.MANGA,
            score = 8.2f
        )
        val aniDetails = JikanMediaDetails(
            malId = null,
            aniListId = 109988L,
            title = "AniList Only Novel",
            externalScore = 8.2f,
            externalScoreSource = "AniList",
            sourceProvider = "AniList",
            url = "https://anilist.co/manga/109988"
        )

        val savedEntry = repo.buildAnimeMangaRelatedMediaEntry(
            userId = 1L,
            item = aniRelated,
            details = aniDetails,
            status = MediaStatus.PLAN_TO_WATCH
        )
        assertEquals(ExternalSources.ANILIST, savedEntry.externalSource)
        assertEquals("109988", savedEntry.externalId)
        assertEquals(ExternalSources.ANILIST, savedEntry.externalScoreSource)
        assertFalse(savedEntry.externalSource == ExternalSources.MAL)
    }
}
