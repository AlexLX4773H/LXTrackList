package com.lx.tracklist.data.remote

import com.lx.tracklist.data.model.ExternalSources
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.ui.detail.DetailViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookSyncAndRefreshTest {

    @Test
    fun `refreshing a book tracked by chapters with chapter progress preserves CHAPTERS even when remote reports pages`() {
        val chapterTrackedBook = MediaEntry(
            title = "Mushoku Tensei",
            mediaType = MediaType.BOOK,
            status = MediaStatus.WATCHING,
            subCategory = "Light Novel",
            trackingUnit = TrackingUnit.CHAPTERS,
            currentChapters = 14,
            totalChapters = 25,
            externalSource = "Google Books",
            externalId = "vol-abc123"
        )
        val detailsWithPages = BookDetails(
            id = "vol-abc123",
            title = "Mushoku Tensei",
            pageCount = 320,
            totalChapters = null,
            sourceProvider = "Google Books"
        )

        val resolvedUnit = BookSyncManager.resolveUpdatedBookTrackingUnit(chapterTrackedBook, detailsWithPages)
        assertEquals(TrackingUnit.CHAPTERS, resolvedUnit)
        assertEquals(14, chapterTrackedBook.currentChapters)
    }

    @Test
    fun `explicit non-default unit is preserved at zero progress while default unit at zero progress can adopt remote unit`() {
        val volumeExplicitAtZero = MediaEntry(
            title = "Ascendance of a Bookworm",
            mediaType = MediaType.BOOK,
            status = MediaStatus.PLAN_TO_WATCH,
            trackingUnit = TrackingUnit.VOLUMES,
            currentVolumes = 0
        )
        val bookWithPages = BookDetails(id = "gb1", title = "Ascendance", pageCount = 350)
        assertEquals(TrackingUnit.VOLUMES, BookSyncManager.resolveUpdatedBookTrackingUnit(volumeExplicitAtZero, bookWithPages))

        val freshDefaultBook = MediaEntry(
            title = "Project Hail Mary",
            mediaType = MediaType.BOOK,
            status = MediaStatus.PLAN_TO_WATCH,
            trackingUnit = TrackingUnit.CHAPTERS,
            currentChapters = 0,
            currentVolumes = 0,
            currentPages = 0
        )
        assertEquals(TrackingUnit.PAGES, BookSyncManager.resolveUpdatedBookTrackingUnit(freshDefaultBook, bookWithPages))
    }

    @Test
    fun `books with valid external IDs skip automatic title fallback and keep original source`() {
        val malLightNovel = MediaEntry(
            title = "86",
            mediaType = MediaType.BOOK,
            status = MediaStatus.WATCHING,
            tags = "light novel",
            externalSource = "MAL",
            externalId = "104039"
        )
        assertTrue(malLightNovel.isLightNovel())
        assertFalse(BookSyncManager.shouldAttemptBookTitleFallback(malLightNovel, forceReLink = false))
        assertTrue(BookSyncManager.shouldAttemptBookTitleFallback(malLightNovel, forceReLink = true))

        val unlinkedBook = malLightNovel.copy(externalId = null, externalSource = null)
        assertTrue(BookSyncManager.shouldAttemptBookTitleFallback(unlinkedBook, forceReLink = false))

        val preserved = DetailViewModel.resolveRefAfterRefresh(
            existing = malLightNovel,
            candidateId = "google-vol-999",
            candidateSource = "Google Books",
            forceReLink = false
        )
        assertEquals("104039" to ExternalSources.MAL, preserved)
    }
}
