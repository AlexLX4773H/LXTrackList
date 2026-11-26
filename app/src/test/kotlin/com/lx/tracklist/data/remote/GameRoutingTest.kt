package com.lx.tracklist.data.remote

import com.lx.tracklist.data.model.ExternalSources
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class GameRoutingTest {

    private fun yearMillis(year: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, Calendar.MAY, 19, 0, 0, 0)
        }.timeInMillis

    @Test
    fun `IGDB ID is never sent to RAWG details when IGDB keys are removed or IGDB fails`() = runBlocking {
        var rawgDetailsCalledWithId: String? = null
        var rawgSearchedTitle: String? = null

        // Case 1: IGDB keys removed, no RAWG match found (different year) -> failure and rawgDetails never called with IGDB ID
        val resMismatch = GameMetadataManager.getGameDetailsInternal(
            externalId = "1942",
            externalSource = "IGDB",
            title = "Prey",
            releaseYear = "2017",
            igdbConfigured = false,
            rawgConfigured = true,
            fetchIgdbDetails = { error("Should not be called when IGDB unconfigured") },
            fetchRawgDetails = { id ->
                rawgDetailsCalledWithId = id
                Result.success(GameDetails(externalId = id, externalSource = "RAWG", title = "Wrong Game"))
            },
            searchRawgGames = { query, _ ->
                rawgSearchedTitle = query
                Result.success(
                    listOf(
                        GameSearchResult(externalId = "99999", title = "Prey", releaseYear = "2006", externalSource = "RAWG")
                    )
                )
            }
        )
        assertTrue(resMismatch.isFailure)
        assertNull("RAWG getGameDetails must NOT be called with IGDB ID or wrong-year match", rawgDetailsCalledWithId)
        assertEquals("Prey", rawgSearchedTitle)

        // Case 2: IGDB returns failure (e.g. 429) -> RAWG fallback uses confident title+year search result's RAWG ID, not IGDB ID
        val resFallback = GameMetadataManager.getGameDetailsInternal(
            externalId = "1942",
            externalSource = "IGDB",
            title = "Prey",
            releaseYear = "2017",
            igdbConfigured = true,
            rawgConfigured = true,
            fetchIgdbDetails = { Result.failure(IllegalStateException("HTTP 429")) },
            fetchRawgDetails = { id ->
                rawgDetailsCalledWithId = id
                Result.success(GameDetails(externalId = id, externalSource = "RAWG", title = "Prey", releaseYear = "2017"))
            },
            searchRawgGames = { _, _ ->
                Result.success(
                    listOf(
                        GameSearchResult(externalId = "99999", title = "Prey", releaseYear = "2006", externalSource = "RAWG"),
                        GameSearchResult(externalId = "10297", title = "Prey", releaseYear = "2017", externalSource = "RAWG")
                    )
                )
            }
        )
        assertTrue(resFallback.isSuccess)
        assertEquals("10297", rawgDetailsCalledWithId)
        assertFalse("1942" == rawgDetailsCalledWithId)
    }

    @Test
    fun `RAWG ID routes directly to RAWG details`() = runBlocking {
        var rawgDetailsCalledWithId: String? = null
        val res = GameMetadataManager.getGameDetailsInternal(
            externalId = "3328",
            externalSource = "rawg",
            igdbConfigured = true,
            rawgConfigured = true,
            fetchIgdbDetails = { error("IGDB must not be called for RAWG source") },
            fetchRawgDetails = { id ->
                rawgDetailsCalledWithId = id
                Result.success(GameDetails(externalId = id, externalSource = "RAWG", title = "The Witcher 3: Wild Hunt"))
            },
            searchRawgGames = { _, _ -> Result.success(emptyList()) }
        )
        assertTrue(res.isSuccess)
        assertEquals("3328", rawgDetailsCalledWithId)
    }

    @Test
    fun `syncing an IGDB-linked game with RAWG fallback never overwrites its IGDB externalId or externalSource`() {
        val igdbEntry = MediaEntry(
            id = 42L,
            title = "The Witcher 3: Wild Hunt",
            mediaType = MediaType.GAME,
            status = MediaStatus.COMPLETED,
            externalSource = "IGDB",
            externalId = "1942",
            airedStartDate = yearMillis(2015)
        )
        val rawgFallbackDetails = GameDetails(
            externalId = "3328",
            externalSource = "RAWG",
            title = "The Witcher 3: Wild Hunt",
            rating = 9.3f,
            ratingSource = "RAWG",
            completionTimeMinutes = 3000
        )

        val updated = GameSyncManager.applySyncedGameDetails(igdbEntry, rawgFallbackDetails)
        assertEquals("1942", updated.externalId)
        assertEquals(ExternalSources.IGDB, updated.externalSource)
        assertEquals(3000, updated.durationMinutes)
    }
}
