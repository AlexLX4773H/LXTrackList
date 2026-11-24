package com.lx.tracklist.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicAndCreatorEntryTest {

    @Test
    fun testMediaTypeMusicProperties() {
        val music = MediaType.MUSIC
        assertEquals("Music", music.displayName)
        assertTrue(music.isListenType)
        assertFalse(music.isWatchType)
        assertFalse(music.isReadType)
        assertFalse(music.isPlayType)

        assertEquals(MediaType.MUSIC, MediaType.fromString("MUSIC"))
        assertEquals(MediaType.MUSIC, MediaType.fromString("Song"))
        assertEquals(MediaType.MUSIC, MediaType.fromString("track"))
        assertEquals(MediaType.MUSIC, MediaType.fromString("tracks"))
    }

    @Test
    fun testMediaStatusDisplayLabelForMusic() {
        val music = MediaType.MUSIC
        assertEquals("Listening", MediaStatus.WATCHING.getDisplayLabel(music))
        assertEquals("Plan to Listen", MediaStatus.PLAN_TO_WATCH.getDisplayLabel(music))
        assertEquals("Completed", MediaStatus.COMPLETED.getDisplayLabel(music))
        assertEquals("On Hold", MediaStatus.ON_HOLD.getDisplayLabel(music))
        assertEquals("Dropped", MediaStatus.DROPPED.getDisplayLabel(music))

        // Parsing "LISTENING"
        assertEquals(MediaStatus.WATCHING, MediaStatus.fromString("LISTENING"))
        assertEquals(MediaStatus.WATCHING, MediaStatus.fromString("Listening"))
    }

    @Test
    fun testCreatorRoleLabels() {
        assertEquals("Artist", MediaType.MUSIC.getCreatorRoleLabel())
        assertEquals("Studio", MediaType.ANIME.getCreatorRoleLabel())
        assertEquals("Author", MediaType.MANGA.getCreatorRoleLabel())
        assertEquals("Author", MediaType.MANHWA.getCreatorRoleLabel())
        assertEquals("Author", MediaType.BOOK.getCreatorRoleLabel())
        assertEquals("Developer", MediaType.GAME.getCreatorRoleLabel())
        assertEquals("Director", MediaType.MOVIE.getCreatorRoleLabel())
        assertEquals("Creator / Network", MediaType.SHOW.getCreatorRoleLabel())
    }

    @Test
    fun testFormatMusicDuration() {
        assertEquals("0:00", MediaEntry.formatMusicDuration(0))
        assertEquals("0:05", MediaEntry.formatMusicDuration(5))
        assertEquals("0:45", MediaEntry.formatMusicDuration(45))
        assertEquals("1:00", MediaEntry.formatMusicDuration(60))
        assertEquals("1:05", MediaEntry.formatMusicDuration(65))
        assertEquals("3:35", MediaEntry.formatMusicDuration(215))
        assertEquals("10:00", MediaEntry.formatMusicDuration(600))
        assertEquals("61:01", MediaEntry.formatMusicDuration(3661))
        assertEquals("0:00", MediaEntry.formatMusicDuration(null))
        assertEquals("0:00", MediaEntry.formatMusicDuration(-10))
    }

    @Test
    fun testMusicProgressToggleAndDisplay() {
        var entry = MediaEntry(
            title = "Bohemian Rhapsody",
            mediaType = MediaType.MUSIC,
            creator = "Queen",
            durationMinutes = 354, // 5m 54s in seconds
            movieSeriesName = "A Night at the Opera",
            movieSeriesNumber = 11,
            status = MediaStatus.PLAN_TO_WATCH
        )

        assertEquals("Queen", entry.artist)
        assertEquals("Artist", entry.getCreatorRoleLabel())
        assertEquals(0f, entry.progressFraction, 0.001f)
        assertEquals("5:54 • Track 11 • A Night at the Opera", entry.displayProgress)

        // Increment progress (toggles to COMPLETED)
        entry = entry.incrementProgress()
        assertEquals(MediaStatus.COMPLETED, entry.status)
        assertEquals(1f, entry.progressFraction, 0.001f)
        assertEquals("Completed", entry.status.getDisplayLabel(entry.mediaType))

        // Increment progress again (toggles back to WATCHING / listening)
        entry = entry.incrementProgress()
        assertEquals(MediaStatus.WATCHING, entry.status)
        assertEquals("Listening", entry.status.getDisplayLabel(entry.mediaType))
        assertEquals(0f, entry.progressFraction, 0.001f)
    }

    @Test
    fun testMusicSpentAndRemainingTime() {
        val entry = MediaEntry(
            title = "Clair de Lune",
            mediaType = MediaType.MUSIC,
            creator = "Claude Debussy",
            durationMinutes = 300, // 5 minutes (300 seconds)
            status = MediaStatus.COMPLETED
        )

        // 300 seconds = 5.0 minutes spent
        assertEquals(5.0, entry.calculateSpentTimeMinutes(), 0.001)
        assertEquals(0.0, entry.calculateRemainingTimeMinutes(), 0.001)

        val unplayed = entry.copy(status = MediaStatus.PLAN_TO_WATCH)
        assertEquals(0.0, unplayed.calculateSpentTimeMinutes(), 0.001)
        assertEquals(5.0, unplayed.calculateRemainingTimeMinutes(), 0.001)
    }

    @Test
    fun testCreatorSemanticGetters() {
        val animeEntry = MediaEntry(title = "Attack on Titan", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, creator = "WIT Studio")
        assertEquals("WIT Studio", animeEntry.studio)

        val bookEntry = MediaEntry(title = "1984", mediaType = MediaType.BOOK, status = MediaStatus.WATCHING, creator = "George Orwell")
        assertEquals("George Orwell", bookEntry.author)

        val gameEntry = MediaEntry(title = "Elden Ring", mediaType = MediaType.GAME, status = MediaStatus.WATCHING, creator = "FromSoftware")
        assertEquals("FromSoftware", gameEntry.developer)

        val musicEntry = MediaEntry(title = "Stairway to Heaven", mediaType = MediaType.MUSIC, status = MediaStatus.WATCHING, creator = "Led Zeppelin")
        assertEquals("Led Zeppelin", musicEntry.artist)
    }
}
