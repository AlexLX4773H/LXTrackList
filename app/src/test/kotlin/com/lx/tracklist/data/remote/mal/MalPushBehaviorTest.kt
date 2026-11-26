package com.lx.tracklist.data.remote.mal

import com.lx.tracklist.data.local.MediaEntryDao
import com.lx.tracklist.data.model.ExternalSources
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.repository.MediaRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger

class MalPushBehaviorTest {

    @After
    fun tearDown() {
        MalSyncManager.pushDelayMs = 1_000L
        MalSyncManager.customPushDispatcher = null
    }

    private fun fakeDao(initial: MediaEntry): Pair<MediaEntryDao, () -> MediaEntry> {
        var stored = initial
        val dao = Proxy.newProxyInstance(
            MediaEntryDao::class.java.classLoader,
            arrayOf(MediaEntryDao::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getEntryByIdSync" -> stored
                "update" -> {
                    stored = args[0] as MediaEntry
                    Unit
                }
                else -> Unit
            }
        } as MediaEntryDao
        return dao to { stored }
    }

    @Test
    fun `updateMetadataOnly never dispatches MAL push even if entry is tracked`() = runBlocking {
        val base = MediaEntry(
            id = 10L,
            userId = 1L,
            title = "Steins;Gate",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            currentEpisodes = 12,
            totalEpisodes = 24,
            externalId = "9253",
            externalSource = ExternalSources.MAL,
            isExternallyTracked = true
        )
        val (dao, getStored) = fakeDao(base)
        val repo = MediaRepository(dao)
        var pushCount = 0
        repo.dispatchMalPush = { pushCount++ }

        val metadataUpdated = base.copy(
            coverImageUri = "https://cdn.example/sg.jpg",
            description = "El Psy Kongroo",
            externalScore = 9.07f,
            lastMetadataSyncAt = 1_750_000_000_000L
        )
        repo.updateMetadataOnly(metadataUpdated)

        assertEquals(0, pushCount)
        assertEquals("El Psy Kongroo", getStored().description)
    }

    @Test
    fun `update suppresses MAL push for metadata-only changes and pushes on user-tracked changes`() = runBlocking {
        val base = MediaEntry(
            id = 11L,
            userId = 1L,
            title = "Monster",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            score = 8,
            currentEpisodes = 10,
            totalEpisodes = 74,
            externalId = "19",
            externalSource = ExternalSources.MAL,
            isExternallyTracked = true
        )
        val (dao, _) = fakeDao(base)
        val repo = MediaRepository(dao)
        val pushed = mutableListOf<MediaEntry>()
        repo.dispatchMalPush = { pushed.add(it) }

        // Metadata / non-MAL-tracked field update -> 0 pushes
        repo.update(base.copy(description = "Dr. Tenma", tags = "Thriller", userComments = "Peak"))
        assertEquals(0, pushed.size)

        // Progress change -> triggers push
        val ep11 = base.copy(description = "Dr. Tenma", currentEpisodes = 11)
        repo.update(ep11)
        assertEquals(1, pushed.size)

        // Score change -> triggers push
        val scored = ep11.copy(score = 9)
        repo.update(scored)
        assertEquals(2, pushed.size)

        // Status / rewatch / personal dates checks
        assertTrue(MediaRepository.hasTrackedUserFieldChanges(scored, scored.copy(status = MediaStatus.COMPLETED)))
        assertTrue(MediaRepository.hasTrackedUserFieldChanges(scored, scored.copy(isRewatching = true)))
        assertTrue(MediaRepository.hasTrackedUserFieldChanges(scored, scored.copy(rewatchCount = 1)))
        assertTrue(MediaRepository.hasTrackedUserFieldChanges(scored, scored.copy(startDate = 1000L)))
        assertTrue(MediaRepository.hasTrackedUserFieldChanges(scored, scored.copy(finishDate = 2000L)))
        assertFalse(MediaRepository.hasTrackedUserFieldChanges(scored, scored.copy(coverImageUri = "https://new", relatedEntriesJson = "[]")))
    }

    @Test
    fun `mal push queue serializes concurrent pushes with spacing delay`() = runBlocking {
        MalSyncManager.pushDelayMs = 120L
        val inFlight = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)
        val timestamps = mutableListOf<Long>()
        val dispatchedTitles = mutableListOf<String>()
        val done = CompletableDeferred<Unit>()

        MalSyncManager.customPushDispatcher = { _, entry ->
            val current = inFlight.incrementAndGet()
            maxConcurrent.updateAndGet { prev -> maxOf(prev, current) }
            delay(30L)
            synchronized(timestamps) {
                timestamps.add(System.currentTimeMillis())
                dispatchedTitles.add(entry.title)
                if (dispatchedTitles.size == 2) {
                    done.complete(Unit)
                }
            }
            inFlight.decrementAndGet()
            Result.success(Unit)
        }

        val first = MediaEntry(
            id = 21L,
            userId = 1L,
            title = "First Entry",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            externalId = "1001",
            externalSource = ExternalSources.MAL,
            isExternallyTracked = true
        )
        val second = first.copy(id = 22L, title = "Second Entry", externalId = "1002")

        MalSyncManager.enqueuePush(null, first)
        MalSyncManager.enqueuePush(null, second)

        done.await()

        assertEquals(1, maxConcurrent.get())
        assertEquals(listOf("First Entry", "Second Entry"), dispatchedTitles)
        assertTrue("Second push must respect spacing delay", timestamps[1] - timestamps[0] >= 100L)
    }
}
