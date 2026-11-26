package com.lx.tracklist.data.repository

import com.lx.tracklist.data.local.MediaEntryDao
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.Calendar
import java.util.TimeZone

class DeduplicationTest {

    private fun dummyRepo(entries: MutableList<MediaEntry> = mutableListOf()): MediaRepository {
        val dao = Proxy.newProxyInstance(
            MediaEntryDao::class.java.classLoader,
            arrayOf(MediaEntryDao::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getAllEntriesByUserSync" -> entries.toList()
                "getEntryByIdSync" -> {
                    val id = args?.getOrNull(0) as? Long
                    entries.firstOrNull { it.id == id }
                }
                "update" -> {
                    val updated = args?.getOrNull(0) as? MediaEntry
                    if (updated != null) {
                        val idx = entries.indexOfFirst { it.id == updated.id }
                        if (idx >= 0) entries[idx] = updated
                    }
                    Unit
                }
                "delete" -> {
                    val removed = args?.getOrNull(0) as? MediaEntry
                    if (removed != null) entries.removeAll { it.id == removed.id }
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
        return MediaRepository(dao = dao, userProfileDao = null)
    }

    private fun yearMillis(year: Int): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, Calendar.JUNE, 15, 0, 0, 0)
        }.timeInMillis
    }

    @Test
    fun `different works with same title and different TMDB IDs are not duplicates`() {
        val repo = dummyRepo()
        val dune1984 = MediaEntry(
            id = 1L,
            title = "Dune",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.COMPLETED,
            externalSource = "tmdb",
            externalId = "841"
        )
        val dune2021 = MediaEntry(
            id = 2L,
            title = "Dune",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.COMPLETED,
            externalSource = "TMDB",
            externalId = "438631"
        )

        assertFalse(repo.areEntriesDuplicates(dune1984, dune2021))
    }

    @Test
    fun `same title across different media types are not duplicates`() {
        val repo = dummyRepo()
        val fargoMovie = MediaEntry(
            id = 1L,
            title = "Fargo",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.COMPLETED,
            externalSource = "TMDB",
            externalId = "275"
        )
        val fargoShow = MediaEntry(
            id = 2L,
            title = "Fargo",
            mediaType = MediaType.SHOW,
            status = MediaStatus.WATCHING,
            externalSource = "TMDB",
            externalId = "275"
        )

        assertFalse(repo.areEntriesDuplicates(fargoMovie, fargoShow))
        assertFalse(repo.areEntriesDuplicates(fargoMovie.copy(externalId = null, externalSource = null), fargoShow.copy(externalId = null, externalSource = null)))
    }

    @Test
    fun `same TMDB ID and media type are duplicates across case variants`() {
        val repo = dummyRepo()
        val entryA = MediaEntry(
            id = 1L,
            title = "Dune",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.COMPLETED,
            externalSource = "tmdb",
            externalId = "438631"
        )
        val entryB = MediaEntry(
            id = 2L,
            title = "Dune: Part One",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.PLAN_TO_WATCH,
            externalSource = "TMDB",
            externalId = "438631"
        )

        assertTrue(repo.areEntriesDuplicates(entryA, entryB))
    }

    @Test
    fun `unlinked entries with same title but different release years are not duplicates`() {
        val repo = dummyRepo()
        val dune1984 = MediaEntry(
            id = 1L,
            title = "Dune",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.COMPLETED,
            airedStartDate = yearMillis(1984)
        )
        val dune2021 = MediaEntry(
            id = 2L,
            title = "Dune",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.COMPLETED,
            airedStartDate = yearMillis(2021)
        )
        val dune2021Dup = MediaEntry(
            id = 3L,
            title = "Dune",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.PLAN_TO_WATCH,
            airedStartDate = yearMillis(2021)
        )

        assertFalse(repo.areEntriesDuplicates(dune1984, dune2021))
        assertTrue(repo.areEntriesDuplicates(dune2021, dune2021Dup))
    }

    @Test
    fun `findDuplicatePairs only returns true pairs and deduplicateLibrary merges them`() = runBlocking {
        val stored = mutableListOf(
            MediaEntry(id = 1L, title = "Dune", mediaType = MediaType.MOVIE, status = MediaStatus.COMPLETED, externalSource = "TMDB", externalId = "841"),
            MediaEntry(id = 2L, title = "Dune", mediaType = MediaType.MOVIE, status = MediaStatus.COMPLETED, externalSource = "TMDB", externalId = "438631"),
            MediaEntry(id = 3L, title = "Dune", mediaType = MediaType.MOVIE, status = MediaStatus.PLAN_TO_WATCH, externalSource = "tmdb", externalId = "438631"),
            MediaEntry(id = 4L, title = "Fargo", mediaType = MediaType.MOVIE, status = MediaStatus.COMPLETED),
            MediaEntry(id = 5L, title = "Fargo", mediaType = MediaType.SHOW, status = MediaStatus.WATCHING)
        )
        val repo = dummyRepo(stored)

        val pairs = repo.findDuplicatePairs(1L)
        assertEquals(1, pairs.size)
        assertEquals(setOf(2L, 3L), setOf(pairs[0].primary.id, pairs[0].duplicate.id))

        val mergedCount = repo.deduplicateLibrary(1L)
        assertEquals(1, mergedCount)
        assertEquals(4, stored.size)
    }
}
