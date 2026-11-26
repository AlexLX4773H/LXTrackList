package com.lx.tracklist.data.remote

import com.lx.tracklist.data.local.MediaEntryDao
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.repository.MediaRepository
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class ResolverEligibilityTest {

    private fun noUpdateDao(updated: MutableList<MediaEntry>): MediaEntryDao {
        return Proxy.newProxyInstance(
            MediaEntryDao::class.java.classLoader,
            arrayOf(MediaEntryDao::class.java)
        ) { _, method, args ->
            when (method.name) {
                "update" -> {
                    (args?.getOrNull(0) as? MediaEntry)?.let { updated.add(it) }
                    Unit
                }
                else -> when (method.returnType) {
                    Boolean::class.javaPrimitiveType -> false
                    Int::class.javaPrimitiveType -> 0
                    Long::class.javaPrimitiveType -> 0L
                    List::class.java -> emptyList<Any>()
                    kotlinx.coroutines.flow.Flow::class.java -> emptyFlow<Any>()
                    else -> null
                }
            }
        } as MediaEntryDao
    }

    @Test
    fun `Music is excluded from resolver eligibility and skipped without modifying DB`() = runBlocking {
        val updated = mutableListOf<MediaEntry>()
        val repo = MediaRepository(dao = noUpdateDao(updated), userProfileDao = null)

        val track = MediaEntry(
            id = 77L,
            title = "Bohemian Rhapsody",
            mediaType = MediaType.MUSIC,
            status = MediaStatus.COMPLETED,
            externalSource = "Apple Music",
            externalId = "123456"
        )
        val unlinkedTrack = track.copy(id = 78L, externalSource = null, externalId = null)
        val manga = MediaEntry(
            id = 79L,
            title = "Vagabond",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING
        )

        assertFalse(TachiyomiResolverManager.isEligibleForResolution(track))
        assertFalse(TachiyomiResolverManager.isEligibleForResolution(unlinkedTrack))

        val (eligible, skipped) = TachiyomiResolverManager.partitionResolutionEntries(
            pool = listOf(track, unlinkedTrack, manga),
            hasTargetSelection = true
        )
        assertEquals(listOf(manga), eligible)
        assertEquals(2, skipped.size)
        assertTrue(skipped.all { it.reason.contains("skipped", ignoreCase = true) && it.reason.contains("not supported", ignoreCase = true) })

        val outcome = TachiyomiResolverManager.matchAndResolveEntry(track, repo, context = null)
        assertTrue(outcome is TachiyomiResolverManager.ResolutionOutcome.Skipped)
        val skippedOutcome = outcome as TachiyomiResolverManager.ResolutionOutcome.Skipped
        assertTrue(skippedOutcome.report.reason.contains("not supported", ignoreCase = true))
        assertTrue("Music track must remain unmodified", updated.isEmpty())

        val reportText = TachiyomiResolverManager.buildResolutionReportText(
            profileName = "Main Profile",
            profileId = 1L,
            totalProcessed = 3,
            resolvedItems = emptyList(),
            skippedItems = skipped,
            failedItems = emptyList(),
            timeStr = "2026-11-26 00:00:00"
        )
        assertTrue(reportText.contains("Skipped (Unmatched / Unsupported): 2"))
        assertTrue(reportText.contains("Bohemian Rhapsody"))
        assertTrue(reportText.contains("not supported"))
    }
}
