package com.lx.tracklist.ui

import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.ui.library.resolveLibraryStatusLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusLabelsWordingTest {

    @Test
    fun `WRD-03 resolveLibraryStatusLabel returns category-aware and neutral labels without raw enum names`() {
        assertEquals(
            "Plan to Read",
            resolveLibraryStatusLabel(
                status = MediaStatus.PLAN_TO_WATCH,
                selectedCategory = MediaType.MANGA,
                selectedCustomCategory = null,
                customCategories = emptyList()
            )
        )
        assertEquals(
            "Plan to Listen",
            resolveLibraryStatusLabel(
                status = MediaStatus.PLAN_TO_WATCH,
                selectedCategory = MediaType.MUSIC,
                selectedCustomCategory = null,
                customCategories = emptyList()
            )
        )
        assertEquals(
            "Playing",
            resolveLibraryStatusLabel(
                status = MediaStatus.WATCHING,
                selectedCategory = MediaType.GAME,
                selectedCustomCategory = null,
                customCategories = emptyList()
            )
        )
        assertEquals(
            "Planned",
            resolveLibraryStatusLabel(
                status = MediaStatus.PLAN_TO_WATCH,
                selectedCategory = null,
                selectedCustomCategory = null,
                customCategories = emptyList()
            )
        )
        assertEquals(
            "On Hold",
            resolveLibraryStatusLabel(
                status = MediaStatus.ON_HOLD,
                selectedCategory = MediaType.ANIME,
                selectedCustomCategory = null,
                customCategories = emptyList()
            )
        )

        val customMusic = listOf(
            CustomCategory(name = "Soundtracks", isWatchType = false, mediaType = MediaType.MUSIC)
        )
        assertEquals(
            "Listening",
            resolveLibraryStatusLabel(
                status = MediaStatus.WATCHING,
                selectedCategory = null,
                selectedCustomCategory = "Soundtracks",
                customCategories = customMusic
            )
        )
    }

    @Test
    fun `WRD-03 MediaStatus labelFor adapts per single vs mixed category selection`() {
        assertEquals("Reading", MediaStatus.WATCHING.labelFor(setOf(MediaType.BOOK)))
        assertEquals("Plan to Listen", MediaStatus.PLAN_TO_WATCH.labelFor(setOf(MediaType.MUSIC)))
        assertEquals("In Progress", MediaStatus.WATCHING.labelFor(setOf(MediaType.ANIME, MediaType.GAME)))
        assertEquals("Planned", MediaStatus.PLAN_TO_WATCH.labelFor(emptySet()))
    }
}
