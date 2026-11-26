package com.lx.tracklist.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStatusTest {

    @Test
    fun `getNeutralLabel returns neutral labels for all statuses`() {
        assertEquals("In Progress", MediaStatus.WATCHING.getNeutralLabel())
        assertEquals("Planned", MediaStatus.PLAN_TO_WATCH.getNeutralLabel())
        assertEquals("Completed", MediaStatus.COMPLETED.getNeutralLabel())
        assertEquals("On Hold", MediaStatus.ON_HOLD.getNeutralLabel())
        assertEquals("Dropped", MediaStatus.DROPPED.getNeutralLabel())
    }

    @Test
    fun `labelFor handles single type, duplicate single type, mixed types, and empty collection`() {
        // Single type -> category-aware label
        assertEquals("Reading", MediaStatus.WATCHING.labelFor(listOf(MediaType.MANGA)))
        assertEquals("Plan to Play", MediaStatus.PLAN_TO_WATCH.labelFor(listOf(MediaType.GAME)))
        assertEquals("Listening", MediaStatus.WATCHING.labelFor(listOf(MediaType.MUSIC, MediaType.MUSIC)))

        // Mixed types -> neutral label
        assertEquals("In Progress", MediaStatus.WATCHING.labelFor(listOf(MediaType.ANIME, MediaType.MANGA)))
        assertEquals("Planned", MediaStatus.PLAN_TO_WATCH.labelFor(listOf(MediaType.GAME, MediaType.MUSIC)))
        assertEquals("On Hold", MediaStatus.ON_HOLD.labelFor(listOf(MediaType.MOVIE, MediaType.BOOK)))

        // Empty list -> neutral label
        assertEquals("In Progress", MediaStatus.WATCHING.labelFor(emptyList()))
        assertEquals("Planned", MediaStatus.PLAN_TO_WATCH.labelFor(emptyList()))
        assertEquals("Completed", MediaStatus.COMPLETED.labelFor(emptyList()))
        assertEquals("Dropped", MediaStatus.DROPPED.labelFor(emptyList()))
    }

    @Test
    fun `MediaEntry statusLabel delegates to status getDisplayLabel for entry mediaType`() {
        val book = MediaEntry(title = "Dune", mediaType = MediaType.BOOK, status = MediaStatus.PLAN_TO_WATCH)
        val game = MediaEntry(title = "Hades", mediaType = MediaType.GAME, status = MediaStatus.WATCHING)
        val music = MediaEntry(title = "Track", mediaType = MediaType.MUSIC, status = MediaStatus.WATCHING)

        assertEquals("Plan to Read", book.statusLabel())
        assertEquals("Playing", game.statusLabel())
        assertEquals("Listening", music.statusLabel())
    }
}
