package com.lx.tracklist

import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.TrackingUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaEntryTest {

    @Test
    fun testAnimeProgressIncrement() {
        val entry = MediaEntry(
            title = "Test Anime",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            currentEpisodes = 5,
            totalEpisodes = 12
        )

        assertEquals("5/12 ep", entry.displayProgress)
        assertEquals(5f / 12f, entry.progressFraction, 0.001f)

        val updated = entry.incrementProgress()
        assertEquals(6, updated.currentEpisodes)
        assertEquals(MediaStatus.WATCHING, updated.status)

        // Complete when reaching total
        var current = updated
        repeat(6) { current = current.incrementProgress() }
        assertEquals(12, current.currentEpisodes)
        assertEquals(MediaStatus.COMPLETED, current.status)
    }

    @Test
    fun testMangaProgressCombined() {
        val entry = MediaEntry(
            title = "Test Manga",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            currentVolumes = 2,
            totalVolumes = 10,
            currentChapters = 15,
            totalChapters = 80,
            trackingUnit = TrackingUnit.COMBINED
        )

        assertEquals("Vol 2/10 • Ch 15/80", entry.displayProgress)
        assertEquals(15f / 80f, entry.progressFraction, 0.001f)

        val updated = entry.incrementProgress()
        assertEquals(16, updated.currentChapters)
    }

    @Test
    fun testMangaProgressVolumes() {
        val entry = MediaEntry(
            title = "Test Manga Vol",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            currentVolumes = 3,
            totalVolumes = 5,
            trackingUnit = TrackingUnit.VOLUMES
        )

        assertEquals("Vol 3/5", entry.displayProgress)
        val updated = entry.incrementProgress()
        assertEquals(4, updated.currentVolumes)
    }

    @Test
    fun testRewatchProgressCompletion() {
        val entry = MediaEntry(
            title = "Steins;Gate",
            mediaType = MediaType.ANIME,
            status = MediaStatus.COMPLETED,
            currentEpisodes = 23,
            totalEpisodes = 24,
            isRewatching = true,
            rewatchCount = 1
        )

        val updated = entry.incrementProgress()
        assertEquals(24, updated.currentEpisodes)
        assertEquals(MediaStatus.COMPLETED, updated.status)
        assertEquals(false, updated.isRewatching)
        assertEquals(2, updated.rewatchCount)
    }

    @Test
    fun testRewatchInProgressIncrement() {
        val entry = MediaEntry(
            title = "Steins;Gate",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            currentEpisodes = 5,
            totalEpisodes = 24,
            isRewatching = true,
            rewatchCount = 1
        )

        val updated = entry.incrementProgress()
        assertEquals(6, updated.currentEpisodes)
        assertEquals(MediaStatus.WATCHING, updated.status)
        assertEquals(true, updated.isRewatching)
        assertEquals(1, updated.rewatchCount)
    }

    @Test
    fun testForeignDatabaseIdAndSourceAliases() {
        val entry = MediaEntry(
            title = "Fullmetal Alchemist: Brotherhood",
            mediaType = MediaType.ANIME,
            status = MediaStatus.COMPLETED,
            externalId = "5114",
            externalSource = "MAL"
        )

        assertEquals("5114", entry.foreignDatabaseId)
        assertEquals("MAL", entry.foreignDatabaseSource)
    }

    @Test
    fun testRelatedMediaItemsSerializationAndCorrelation() {
        val relatedList = listOf(
            com.lx.tracklist.data.model.RelatedMediaItem(
                title = "Attack on Titan Season 2",
                relationship = "Sequel",
                externalId = "25777",
                externalSource = "MAL",
                mediaType = MediaType.ANIME
            ),
            com.lx.tracklist.data.model.RelatedMediaItem(
                title = "Attack on Titan: No Regrets",
                relationship = "Side story",
                externalId = "25781",
                externalSource = "MAL",
                mediaType = MediaType.ANIME
            ),
            com.lx.tracklist.data.model.RelatedMediaItem(
                title = "Attack on Titan: Junior High",
                relationship = "Spin-off",
                externalId = "31374",
                externalSource = "MAL",
                mediaType = MediaType.ANIME
            )
        )
        val json = com.google.gson.Gson().toJson(relatedList)

        val mainEntry = MediaEntry(
            id = 1L,
            title = "Attack on Titan",
            mediaType = MediaType.ANIME,
            status = MediaStatus.COMPLETED,
            externalId = "16498",
            externalSource = "MAL",
            relatedEntriesJson = json
        )

        // 1. Verify deserialization
        val items = mainEntry.getRelatedMediaItems()
        assertEquals(3, items.size)
        assertEquals("Attack on Titan Season 2", items[0].title)
        assertEquals("Sequel", items[0].relationship)

        // 2. Correlate with library (User only has Season 2 in library, not No Regrets or Junior High)
        val library = listOf(
            mainEntry,
            MediaEntry(
                id = 10L,
                title = "Attack on Titan Season 2",
                mediaType = MediaType.ANIME,
                status = MediaStatus.WATCHING,
                externalId = "25777",
                externalSource = "MAL"
            )
        )

        val correlated = mainEntry.getCorrelatedRelatedItems(library)
        assertEquals(3, correlated.size)

        // Season 2 is being tracked
        assertEquals(10L, correlated[0].inLibraryEntryId)
        assertEquals(MediaStatus.WATCHING, correlated[0].inLibraryStatus)

        // No Regrets is untracked
        org.junit.Assert.assertNull(correlated[1].inLibraryEntryId)
        org.junit.Assert.assertNull(correlated[1].inLibraryStatus)

        // Junior High is untracked
        org.junit.Assert.assertNull(correlated[2].inLibraryEntryId)
        org.junit.Assert.assertNull(correlated[2].inLibraryStatus)
    }

    @Test
    fun testSameNameDifferentExternalIdDoesNotMatch() {
        // Friday the 13th (1980) TMDB ID 9728
        val friday1980Entry = MediaEntry(
            id = 1L,
            title = "Friday the 13th",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.PLAN_TO_WATCH,
            externalId = "9728",
            externalSource = "TMDB",
            airedStartDate = 326764800000L // 1980-05-09 UTC
        )

        // Friday the 13th (2009) TMDB ID 13207
        val friday2009CandidateId = "13207"
        val friday2009CandidateSource = "TMDB"
        val friday2009Title = "Friday the 13th"
        val friday2009Year = "2009"

        // 1980 entry should match 1980 candidate
        org.junit.Assert.assertTrue(
            friday1980Entry.matchesExternal(
                candidateExternalId = "9728",
                candidateExternalSource = "TMDB",
                candidateTitle = "Friday the 13th",
                candidateMediaType = MediaType.MOVIE,
                candidateReleaseYear = "1980"
            )
        )

        // 1980 entry MUST NOT match 2009 candidate even though titles are identical
        org.junit.Assert.assertFalse(
            friday1980Entry.matchesExternal(
                candidateExternalId = friday2009CandidateId,
                candidateExternalSource = friday2009CandidateSource,
                candidateTitle = friday2009Title,
                candidateMediaType = MediaType.MOVIE,
                candidateReleaseYear = friday2009Year
            )
        )

        // Friday the 13th (2009) entry should match 2009 candidate and NOT 1980 candidate
        val friday2009Entry = MediaEntry(
            id = 2L,
            title = "Friday the 13th",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.PLAN_TO_WATCH,
            externalId = "13207",
            externalSource = "TMDB",
            airedStartDate = 1234483200000L // 2009-02-13 UTC
        )

        org.junit.Assert.assertTrue(
            friday2009Entry.matchesExternal(
                candidateExternalId = "13207",
                candidateExternalSource = "TMDB",
                candidateTitle = "Friday the 13th",
                candidateMediaType = MediaType.MOVIE,
                candidateReleaseYear = "2009"
            )
        )

        org.junit.Assert.assertFalse(
            friday2009Entry.matchesExternal(
                candidateExternalId = "9728",
                candidateExternalSource = "TMDB",
                candidateTitle = "Friday the 13th",
                candidateMediaType = MediaType.MOVIE,
                candidateReleaseYear = "1980"
            )
        )
    }

    @Test
    fun testSameNameManualEntryDistinguishedByReleaseYear() {
        // Manual entry without external ID, but has 1980 release date
        val manual1980Entry = MediaEntry(
            id = 1L,
            title = "Friday the 13th",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.PLAN_TO_WATCH,
            externalId = null,
            externalSource = null,
            airedStartDate = 326764800000L // 1980-05-09 UTC
        )

        // Candidate 1980 matches
        org.junit.Assert.assertTrue(
            manual1980Entry.matchesExternal(
                candidateExternalId = "9728",
                candidateExternalSource = "TMDB",
                candidateTitle = "Friday the 13th",
                candidateMediaType = MediaType.MOVIE,
                candidateReleaseYear = "1980"
            )
        )

        // Candidate 2009 with different release year must NOT match
        org.junit.Assert.assertFalse(
            manual1980Entry.matchesExternal(
                candidateExternalId = "13207",
                candidateExternalSource = "TMDB",
                candidateTitle = "Friday the 13th",
                candidateMediaType = MediaType.MOVIE,
                candidateReleaseYear = "2009"
            )
        )
    }

    @Test
    fun testSourceNormalizationAndMediaTypeMismatch() {
        val malEntry = MediaEntry(
            id = 1L,
            title = "Dororo",
            mediaType = MediaType.ANIME,
            status = MediaStatus.COMPLETED,
            externalId = "5760",
            externalSource = "MyAnimeList"
        )

        // Same ID with normalized source "MAL" matches
        org.junit.Assert.assertTrue(
            malEntry.matchesExternal(
                candidateExternalId = "5760",
                candidateExternalSource = "MAL",
                candidateTitle = "Dororo",
                candidateMediaType = MediaType.ANIME
            )
        )

        // Different ID from remake (2019 has MAL ID 37520) must NOT match
        org.junit.Assert.assertFalse(
            malEntry.matchesExternal(
                candidateExternalId = "37520",
                candidateExternalSource = "MAL",
                candidateTitle = "Dororo",
                candidateMediaType = MediaType.ANIME
            )
        )

        // Different MediaType must NOT match
        org.junit.Assert.assertFalse(
            malEntry.matchesExternal(
                candidateExternalId = "5760",
                candidateExternalSource = "MAL",
                candidateTitle = "Dororo",
                candidateMediaType = MediaType.MOVIE
            )
        )
    }

    @Test
    fun testCurrentTimePlaybackAndDisplayProgress() {
        val animeEntry = MediaEntry(
            title = "Frieren",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            currentEpisodes = 10,
            totalEpisodes = 28,
            episodeLengthMinutes = 24,
            currentTimeMinutes = 12
        )

        assertEquals(24, animeEntry.getEffectiveRuntimeMinutes())
        assertEquals(50, animeEntry.getPlaybackProgressPercent())
        assertEquals("12m", animeEntry.formatPlaybackTime())
        assertEquals("10/28 ep (12m)", animeEntry.displayProgress)

        // Incrementing progress advances episode and clears intra-episode current time
        val next = animeEntry.incrementProgress()
        assertEquals(11, next.currentEpisodes)
        org.junit.Assert.assertNull(next.currentTimeMinutes)
        assertEquals("11/28 ep", next.displayProgress)
    }

    @Test
    fun testMovieCurrentTimeProgress() {
        val movie = MediaEntry(
            title = "Inception",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.WATCHING,
            durationMinutes = 148,
            currentTimeMinutes = 74
        )

        assertEquals(148, movie.getEffectiveRuntimeMinutes())
        assertEquals(50, movie.getPlaybackProgressPercent())
        assertEquals("1h 14m", movie.formatPlaybackTime())
        assertEquals("1h 14m / 2h 28m", movie.displayProgress)
        assertEquals(0.5f, movie.progressFraction, 0.01f)
    }

    @Test
    fun testShowSeasonEpisodeTrackingAndRollover() {
        val seasonJson = MediaEntry.formatSeasonEpisodes(listOf(8, 10, 6))
        var show = MediaEntry(
            title = "Top Gear Sample",
            mediaType = MediaType.SHOW,
            status = MediaStatus.WATCHING,
            currentSeasons = 1,
            totalSeasons = 3,
            currentEpisodes = 7,
            totalEpisodes = 24,
            seasonEpisodesJson = seasonJson
        )

        org.junit.Assert.assertTrue(show.hasSeasonEpisodeTracking())
        assertEquals(listOf(8, 10, 6), show.getSeasonEpisodes())
        assertEquals(8, show.getTotalEpisodesForSeason(1))
        assertEquals(10, show.getTotalEpisodesForSeason(2))
        assertEquals(24, show.getEffectiveTotalEpisodes())
        assertEquals("S1 • Ep 7/8", show.displayProgress)
        assertEquals(7, show.getTotalWatchedEpisodes())

        // Ep 7 -> Ep 8 (still Season 1, still Watching)
        show = show.incrementProgress()
        assertEquals(1, show.currentSeasons)
        assertEquals(8, show.currentEpisodes)
        assertEquals(MediaStatus.WATCHING, show.status)
        assertEquals("S1 • Ep 8/8", show.displayProgress)

        // Clicking +1 after the 8th episode of Season 1 increments Season to 2 and restarts Episode at 1
        show = show.incrementProgress()
        assertEquals(2, show.currentSeasons)
        assertEquals(1, show.currentEpisodes)
        assertEquals(MediaStatus.WATCHING, show.status)
        assertEquals("S2 • Ep 1/10", show.displayProgress)
        assertEquals(9, show.getTotalWatchedEpisodes())
        assertEquals(9f / 24f, show.progressFraction, 0.001f)

        // Decrementing from S2 Ep 1 steps back to last episode of Season 1 (S1 Ep 8)
        val steppedBack = show.decrementProgress()
        assertEquals(1, steppedBack.currentSeasons)
        assertEquals(8, steppedBack.currentEpisodes)

        // Completing the final episode of the final season marks COMPLETED
        val nearEnd = show.copy(currentSeasons = 3, currentEpisodes = 5)
        val finished = nearEnd.incrementProgress()
        assertEquals(3, finished.currentSeasons)
        assertEquals(6, finished.currentEpisodes)
        assertEquals(MediaStatus.COMPLETED, finished.status)
        assertEquals(24, finished.getTotalWatchedEpisodes())
        assertEquals(1.0f, finished.progressFraction, 0.001f)
    }

    @Test
    fun testSeasonTrackingAppliesOnlyToShows() {
        val seasonJson = MediaEntry.formatSeasonEpisodes(listOf(8, 10))
        val anime = MediaEntry(
            title = "Sample Anime",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            currentSeasons = 1,
            totalSeasons = 2,
            currentEpisodes = 8,
            totalEpisodes = 18,
            seasonEpisodesJson = seasonJson
        )

        org.junit.Assert.assertFalse(anime.hasSeasonEpisodeTracking())
        assertEquals(emptyList<Int>(), anime.getSeasonEpisodes())

        val nextAnime = anime.incrementProgress()
        assertEquals(1, nextAnime.currentSeasons)
        assertEquals(9, nextAnime.currentEpisodes)
        assertEquals("9/18 ep", nextAnime.displayProgress)
    }

    @Test
    fun testDescriptionAndUserCommentsFields() {
        val entry = MediaEntry(
            title = "Test Description and Comments",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            description = "Detailed official synopsis and overview.",
            userComments = "My personal impressions and rating thoughts."
        )

        assertEquals("Detailed official synopsis and overview.", entry.description)
        assertEquals("Detailed official synopsis and overview.", entry.notes)
        assertEquals("My personal impressions and rating thoughts.", entry.userComments)

        val updated = entry.copy(userComments = "Updated thoughts after episode 5.")
        assertEquals("Detailed official synopsis and overview.", updated.description)
        assertEquals("Detailed official synopsis and overview.", updated.notes)
        assertEquals("Updated thoughts after episode 5.", updated.userComments)
    }
}
