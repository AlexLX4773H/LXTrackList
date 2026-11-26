package com.lx.tracklist.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaTypeTest {

    @Test
    fun `category verbs match expected table across all 8 media types`() {
        val watchTypes = listOf(MediaType.ANIME, MediaType.MOVIE, MediaType.SHOW)
        for (type in watchTypes) {
            assertEquals("Watch", type.consumeVerb())
            assertEquals("Watched", type.consumeVerbPast())
            assertEquals("Watching", type.consumeVerbIng())
            assertEquals("Rewatch", type.repeatVerb())
            assertEquals("Rewatching", type.repeatingLabel())
            assertEquals("Rewatched", type.repeatedLabel())
            assertEquals("Times Rewatched", type.timesRepeatedLabel())
            assertEquals("My Watching Dates", type.personalDatesLabel())
        }

        val readTypes = listOf(MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK)
        for (type in readTypes) {
            assertEquals("Read", type.consumeVerb())
            assertEquals("Read", type.consumeVerbPast())
            assertEquals("Reading", type.consumeVerbIng())
            assertEquals("Reread", type.repeatVerb())
            assertEquals("Rereading", type.repeatingLabel())
            assertEquals("Reread", type.repeatedLabel())
            assertEquals("Times Reread", type.timesRepeatedLabel())
            assertEquals("My Reading Dates", type.personalDatesLabel())
        }

        assertEquals("Play", MediaType.GAME.consumeVerb())
        assertEquals("Played", MediaType.GAME.consumeVerbPast())
        assertEquals("Playing", MediaType.GAME.consumeVerbIng())
        assertEquals("Replay", MediaType.GAME.repeatVerb())
        assertEquals("Replaying", MediaType.GAME.repeatingLabel())
        assertEquals("Replayed", MediaType.GAME.repeatedLabel())
        assertEquals("Times Replayed", MediaType.GAME.timesRepeatedLabel())
        assertEquals("My Playing Dates", MediaType.GAME.personalDatesLabel())

        assertEquals("Listen", MediaType.MUSIC.consumeVerb())
        assertEquals("Listened", MediaType.MUSIC.consumeVerbPast())
        assertEquals("Listening", MediaType.MUSIC.consumeVerbIng())
        assertEquals("Relisten", MediaType.MUSIC.repeatVerb())
        assertEquals("Relistening", MediaType.MUSIC.repeatingLabel())
        assertEquals("Relistened", MediaType.MUSIC.repeatedLabel())
        assertEquals("Times Relistened", MediaType.MUSIC.timesRepeatedLabel())
        assertEquals("My Listening Dates", MediaType.MUSIC.personalDatesLabel())
    }

    @Test
    fun `getDisplayLabel matches all 8 types by 5 statuses`() {
        val expectedByType = mapOf(
            MediaType.ANIME to listOf("Watching", "Plan to Watch", "Completed", "On Hold", "Dropped"),
            MediaType.MOVIE to listOf("Watching", "Plan to Watch", "Completed", "On Hold", "Dropped"),
            MediaType.SHOW to listOf("Watching", "Plan to Watch", "Completed", "On Hold", "Dropped"),
            MediaType.MANGA to listOf("Reading", "Plan to Read", "Completed", "On Hold", "Dropped"),
            MediaType.MANHWA to listOf("Reading", "Plan to Read", "Completed", "On Hold", "Dropped"),
            MediaType.BOOK to listOf("Reading", "Plan to Read", "Completed", "On Hold", "Dropped"),
            MediaType.GAME to listOf("Playing", "Plan to Play", "Completed", "On Hold", "Dropped"),
            MediaType.MUSIC to listOf("Listening", "Plan to Listen", "Completed", "On Hold", "Dropped")
        )
        val statuses = listOf(
            MediaStatus.WATCHING,
            MediaStatus.PLAN_TO_WATCH,
            MediaStatus.COMPLETED,
            MediaStatus.ON_HOLD,
            MediaStatus.DROPPED
        )

        for (type in MediaType.entries) {
            val expectedLabels = expectedByType.getValue(type)
            statuses.forEachIndexed { index, status ->
                assertEquals(
                    "Mismatch for $type / $status",
                    expectedLabels[index],
                    status.getDisplayLabel(type)
                )
            }
        }
    }

    @Test
    fun `release-state badges and labels cover all 8 media types`() {
        val expectedUnreleasedBadges = mapOf(
            MediaType.ANIME to "NYA",
            MediaType.SHOW to "NYA",
            MediaType.MOVIE to "NYA",
            MediaType.MANGA to "NYP",
            MediaType.MANHWA to "NYP",
            MediaType.BOOK to "NYP",
            MediaType.GAME to "UNRELEASED",
            MediaType.MUSIC to "UNRELEASED"
        )
        val expectedUnreleasedLabels = mapOf(
            MediaType.ANIME to "Not Yet Aired",
            MediaType.SHOW to "Not Yet Aired",
            MediaType.MOVIE to "Not Yet Aired",
            MediaType.MANGA to "Not Yet Published",
            MediaType.MANHWA to "Not Yet Published",
            MediaType.BOOK to "Not Yet Published",
            MediaType.GAME to "Unreleased",
            MediaType.MUSIC to "Unreleased"
        )
        val expectedOngoingBadges = mapOf(
            MediaType.ANIME to "AIRING",
            MediaType.SHOW to "AIRING",
            MediaType.MANGA to "PUBLISHING",
            MediaType.MANHWA to "PUBLISHING",
            MediaType.BOOK to "PUBLISHING",
            MediaType.MOVIE to null,
            MediaType.GAME to null,
            MediaType.MUSIC to null
        )
        val expectedOngoingLabels = mapOf(
            MediaType.ANIME to "Airing",
            MediaType.SHOW to "Airing",
            MediaType.MANGA to "Publishing",
            MediaType.MANHWA to "Publishing",
            MediaType.BOOK to "Publishing",
            MediaType.MOVIE to null,
            MediaType.GAME to null,
            MediaType.MUSIC to null
        )

        for (type in MediaType.entries) {
            assertEquals(expectedUnreleasedBadges.getValue(type), type.unreleasedBadge())
            assertEquals(expectedUnreleasedLabels.getValue(type), type.unreleasedLabel())
            assertEquals(expectedOngoingBadges.getValue(type), type.ongoingBadge())
            assertEquals(expectedOngoingLabels.getValue(type), type.ongoingLabel())
        }
    }

    @Test
    fun `MediaEntry releaseBadge handles future, airing, and finished states`() {
        val future = System.currentTimeMillis() + 86_400_000L * 7
        val past = System.currentTimeMillis() - 86_400_000L * 30

        assertEquals(
            "NYA",
            MediaEntry(title = "Future Movie", mediaType = MediaType.MOVIE, status = MediaStatus.PLAN_TO_WATCH, airedStartDate = future).releaseBadge()
        )
        assertEquals(
            "NYP",
            MediaEntry(title = "Future Book", mediaType = MediaType.BOOK, status = MediaStatus.PLAN_TO_WATCH, airedStartDate = future).releaseBadge()
        )
        assertEquals(
            "UNRELEASED",
            MediaEntry(title = "Future Track", mediaType = MediaType.MUSIC, status = MediaStatus.PLAN_TO_WATCH, airedStartDate = future).releaseBadge()
        )

        assertEquals(
            "AIRING",
            MediaEntry(title = "Airing Show", mediaType = MediaType.SHOW, status = MediaStatus.WATCHING, airedStartDate = past, isAiring = true).releaseBadge()
        )
        assertEquals(
            "PUBLISHING",
            MediaEntry(title = "Ongoing Manga", mediaType = MediaType.MANGA, status = MediaStatus.WATCHING, airedStartDate = past, isAiring = true).releaseBadge()
        )
        assertNull(
            MediaEntry(title = "Airing Flagged Game", mediaType = MediaType.GAME, status = MediaStatus.WATCHING, airedStartDate = past, isAiring = true).releaseBadge()
        )
        assertNull(
            MediaEntry(title = "Airing Flagged Music", mediaType = MediaType.MUSIC, status = MediaStatus.WATCHING, airedStartDate = past, isAiring = true).releaseBadge()
        )
        assertNull(
            MediaEntry(title = "Finished Anime", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED, airedStartDate = past, isAiring = false).releaseBadge()
        )
    }

    @Test
    fun `displayName, singularName, and fromString aliases match Decision D12`() {
        assertEquals("TV Shows", MediaType.SHOW.displayName)

        val expectedSingular = mapOf(
            MediaType.ANIME to "Anime",
            MediaType.MANGA to "Manga",
            MediaType.MANHWA to "Manhwa",
            MediaType.MOVIE to "Movie",
            MediaType.SHOW to "TV Show",
            MediaType.BOOK to "Book",
            MediaType.GAME to "Game",
            MediaType.MUSIC to "Track"
        )
        for (type in MediaType.entries) {
            assertEquals(expectedSingular.getValue(type), type.singularName)
        }

        // Legacy & alias parsing for SHOW and singular forms
        assertEquals(MediaType.SHOW, MediaType.fromString("Shows"))
        assertEquals(MediaType.SHOW, MediaType.fromString("Show"))
        assertEquals(MediaType.SHOW, MediaType.fromString("TV"))
        assertEquals(MediaType.SHOW, MediaType.fromString("TV Show"))
        assertEquals(MediaType.SHOW, MediaType.fromString("TV Shows"))
        assertEquals(MediaType.SHOW, MediaType.fromString("Series"))
        assertEquals(MediaType.MOVIE, MediaType.fromString("Movie"))
        assertEquals(MediaType.BOOK, MediaType.fromString("Book"))
        assertEquals(MediaType.MUSIC, MediaType.fromString("Track"))
    }
}
