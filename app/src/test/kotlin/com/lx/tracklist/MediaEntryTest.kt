package com.lx.tracklist

import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.ui.components.SortOption
import com.lx.tracklist.ui.library.LibrarySortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun testLegacyRelatedEntriesJsonWithoutPlatformsFieldDoesNotCrash() {
        // Legacy JSON generated before 'platforms' field was introduced (no "platforms" key)
        val legacyJson = """
            [
              {
                "title": "Attack on Titan Season 2",
                "relationship": "Sequel",
                "externalId": "25777",
                "externalSource": "MAL",
                "mediaType": "ANIME"
              }
            ]
        """.trimIndent()

        val mainEntry = MediaEntry(
            id = 1L,
            title = "Attack on Titan",
            mediaType = MediaType.ANIME,
            status = MediaStatus.COMPLETED,
            externalId = "16498",
            externalSource = "MAL",
            relatedEntriesJson = legacyJson
        )

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

        // Must not crash with NullPointerException on copy() or deserialization
        val items = mainEntry.getRelatedMediaItems()
        assertEquals(1, items.size)
        assertEquals(emptyList<String>(), items[0].platforms)

        val correlated = mainEntry.getCorrelatedRelatedItems(library)
        assertEquals(1, correlated.size)
        assertEquals(10L, correlated[0].inLibraryEntryId)
        assertEquals(MediaStatus.WATCHING, correlated[0].inLibraryStatus)
        assertEquals(emptyList<String>(), correlated[0].platforms)

        // Verify copy() doesn't throw NPE
        val copied = items[0].copy(title = "Updated Title")
        assertEquals("Updated Title", copied.title)
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

    @Test
    fun testEffectiveAiredEpisodes() {
        val now = System.currentTimeMillis()
        val threeWeeksAgo = now - (21L * 24 * 60 * 60 * 1000L)

        // 1. Airing anime with explicit airedEpisodes > 0 returns that number
        val animeWithCount = MediaEntry(
            title = "Airing Show",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            isAiring = true,
            airedEpisodes = 6,
            totalEpisodes = 12
        )
        assertEquals(6, animeWithCount.getEffectiveAiredEpisodes())

        // 2. Airing anime with airedEpisodes == 0 projects from airedStartDate
        val animeProjected = MediaEntry(
            title = "Projected Show",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            isAiring = true,
            airedEpisodes = 0,
            airedStartDate = threeWeeksAgo,
            totalEpisodes = 12
        )
        assertEquals(4, animeProjected.getEffectiveAiredEpisodes())

        // 3. Non-airing anime does not project
        val animeNotAiring = MediaEntry(
            title = "Finished Show",
            mediaType = MediaType.ANIME,
            status = MediaStatus.PLAN_TO_WATCH,
            isAiring = false,
            airedEpisodes = 0,
            airedStartDate = threeWeeksAgo,
            totalEpisodes = 12
        )
        assertEquals(0, animeNotAiring.getEffectiveAiredEpisodes())
    }

    @Test
    fun testParseBroadcastToNextEpoch() {
        val now = System.currentTimeMillis()
        val epochJst = MediaEntry.parseBroadcastToNextEpoch("Sundays at 00:00 (JST)", now)
        org.junit.Assert.assertNotNull(epochJst)
        org.junit.Assert.assertTrue(epochJst!! >= now)

        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tokyo"))
        cal.timeInMillis = epochJst
        assertEquals(java.util.Calendar.SUNDAY, cal.get(java.util.Calendar.DAY_OF_WEEK))
        assertEquals(0, cal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(java.util.Calendar.MINUTE))

        // Unknown or blank returns null
        org.junit.Assert.assertNull(MediaEntry.parseBroadcastToNextEpoch("Unknown", now))
        org.junit.Assert.assertNull(MediaEntry.parseBroadcastToNextEpoch("", now))
    }

    @Test
    fun testFormatAiringScheduleInLocalTimezone() {
        // Construct calendar for Sunday at 17:30 JST (which is Sunday 14:00 in Asia/Kolkata / IST)
        val calJst = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tokyo"))
        calJst.set(2026, java.util.Calendar.OCTOBER, 4, 17, 30, 0)
        calJst.set(java.util.Calendar.MILLISECOND, 0)
        val testEpoch = calJst.timeInMillis

        val entry = MediaEntry(
            title = "Magical Explorer",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            isAiring = true,
            broadcastTime = "Sundays at 17:30 (JST)",
            nextAiringEpoch = testEpoch
        )

        val istTz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        val localScheduleFull = entry.formatAiringSchedule(short = false, timeZone = istTz)
        val localScheduleShort = entry.formatAiringSchedule(short = true, timeZone = istTz)

        // In IST, 17:30 JST is exactly 14:00 IST on Sunday
        org.junit.Assert.assertTrue(localScheduleFull!!.startsWith("Sunday at 14:00"))
        // Short format for card display omits timezone offset (understood to be local time)
        org.junit.Assert.assertEquals("Sun 14:00", localScheduleShort)
        org.junit.Assert.assertFalse(localScheduleShort!!.contains("GMT"))
        org.junit.Assert.assertFalse(localScheduleShort.contains("IST"))

        // When includeTimeZone is explicitly true for short format
        val localScheduleShortWithTz = entry.formatAiringSchedule(short = true, timeZone = istTz, includeTimeZone = true)
        org.junit.Assert.assertTrue(localScheduleShortWithTz!!.startsWith("Sun 14:00"))
        org.junit.Assert.assertTrue(localScheduleShortWithTz.contains("GMT") || localScheduleShortWithTz.contains("IST"))

        // JST timezone check
        val jstTz = java.util.TimeZone.getTimeZone("Asia/Tokyo")
        val jstSchedule = entry.formatAiringSchedule(short = false, timeZone = jstTz)
        org.junit.Assert.assertTrue(jstSchedule!!.startsWith("Sunday at 17:30"))

        // Card text formatting omits timezone
        val cardText = entry.formatAiringCardText()
        org.junit.Assert.assertNotNull(cardText)
        org.junit.Assert.assertFalse(cardText!!.contains("GMT"))
    }

    @Test
    fun testAiringApplicableOnlyToAiringAnime() {
        val manga = MediaEntry(
            title = "Sample Manga",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            isAiring = true,
            broadcastTime = "Sundays at 00:00 (JST)"
        )
        org.junit.Assert.assertNull(manga.getComputedNextAiringEpoch())
        org.junit.Assert.assertNull(manga.formatAiringSchedule())
        org.junit.Assert.assertNull(manga.getTimeUntilNextEpisode())
        org.junit.Assert.assertNull(manga.formatAiringCardText())

        val finishedAnime = MediaEntry(
            title = "Finished Anime",
            mediaType = MediaType.ANIME,
            status = MediaStatus.COMPLETED,
            isAiring = false,
            broadcastTime = "Sundays at 00:00 (JST)"
        )
        org.junit.Assert.assertNull(finishedAnime.getComputedNextAiringEpoch())
        org.junit.Assert.assertNull(finishedAnime.formatAiringSchedule())
        org.junit.Assert.assertNull(finishedAnime.getTimeUntilNextEpisode())
        org.junit.Assert.assertNull(finishedAnime.formatAiringCardText())

        val releasedMovie = MediaEntry(
            title = "Avengers: Endgame",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.COMPLETED,
            airedStartDate = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000L
        )
        org.junit.Assert.assertFalse(releasedMovie.isNotYetAiredOrPublished)
        org.junit.Assert.assertNull(releasedMovie.getComputedNextAiringEpoch())
        org.junit.Assert.assertNull(releasedMovie.formatAiringSchedule())
        org.junit.Assert.assertNull(releasedMovie.getTimeUntilNextEpisode())
        org.junit.Assert.assertNull(releasedMovie.formatAiringCardText())
    }

    @Test
    fun testMovieNyaCountdownAndSchedule() {
        val now = System.currentTimeMillis()
        val futureReleaseDate = now + 100L * 24 * 60 * 60 * 1000L + 60_000L

        val nyaMovie = MediaEntry(
            title = "Avengers: Doomsday",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.PLAN_TO_WATCH,
            airedStartDate = futureReleaseDate
        )

        org.junit.Assert.assertTrue(nyaMovie.isNotYetAiredOrPublished)
        assertEquals(futureReleaseDate, nyaMovie.getComputedNextAiringEpoch())

        val shortSchedule = nyaMovie.formatAiringSchedule(short = true)
        org.junit.Assert.assertNotNull(shortSchedule)
        org.junit.Assert.assertTrue(shortSchedule!!.isNotBlank())

        val countdown = nyaMovie.getTimeUntilNextEpisode()
        org.junit.Assert.assertNotNull(countdown)
        assertEquals("In 100d", countdown)

        val cardText = nyaMovie.formatAiringCardText()
        org.junit.Assert.assertNotNull(cardText)
        assertEquals("$shortSchedule • In 100d", cardText)

        // Close release countdown (e.g. 5 days 10 hours)
        val closeReleaseDate = now + (5L * 24 * 60 * 60 * 1000L) + (10L * 60 * 60 * 1000L) + 60_000L
        val closeMovie = MediaEntry(
            title = "Soon Movie",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.PLAN_TO_WATCH,
            airedStartDate = closeReleaseDate
        )
        assertEquals("In 5d 10h", closeMovie.getTimeUntilNextEpisode())
    }

    @Test
    fun testAllMediaTypesNyaAndNypCountdownAndSchedule() {
        val now = System.currentTimeMillis()
        val futureDate = now + 90L * 24 * 60 * 60 * 1000L + 60_000L

        // 1. Unreleased Video Game
        val unreleasedGame = MediaEntry(
            title = "Grand Theft Auto VI",
            mediaType = MediaType.GAME,
            status = MediaStatus.PLAN_TO_WATCH,
            airedStartDate = futureDate
        )
        org.junit.Assert.assertTrue(unreleasedGame.isNotYetAiredOrPublished)
        assertEquals(futureDate, unreleasedGame.getComputedNextAiringEpoch())
        assertEquals("In 90d", unreleasedGame.getTimeUntilNextEpisode())
        org.junit.Assert.assertTrue(unreleasedGame.formatAiringCardText()!!.endsWith("• In 90d"))

        // 2. Unreleased TV Show
        val unreleasedShow = MediaEntry(
            title = "House of the Dragon Season 3",
            mediaType = MediaType.SHOW,
            status = MediaStatus.PLAN_TO_WATCH,
            airedStartDate = futureDate
        )
        org.junit.Assert.assertTrue(unreleasedShow.isNotYetAiredOrPublished)
        assertEquals(futureDate, unreleasedShow.getComputedNextAiringEpoch())
        assertEquals("In 90d", unreleasedShow.getTimeUntilNextEpisode())
        org.junit.Assert.assertTrue(unreleasedShow.formatAiringCardText()!!.endsWith("• In 90d"))

        // 3. Unreleased Manga
        val unreleasedManga = MediaEntry(
            title = "Future Manga",
            mediaType = MediaType.MANGA,
            status = MediaStatus.PLAN_TO_WATCH,
            airedStartDate = futureDate
        )
        org.junit.Assert.assertTrue(unreleasedManga.isNotYetAiredOrPublished)
        assertEquals(futureDate, unreleasedManga.getComputedNextAiringEpoch())
        assertEquals("In 90d", unreleasedManga.getTimeUntilNextEpisode())
        org.junit.Assert.assertTrue(unreleasedManga.formatAiringCardText()!!.endsWith("• In 90d"))

        // 4. Unreleased Manhwa
        val unreleasedManhwa = MediaEntry(
            title = "Future Manhwa",
            mediaType = MediaType.MANHWA,
            status = MediaStatus.PLAN_TO_WATCH,
            airedStartDate = futureDate
        )
        org.junit.Assert.assertTrue(unreleasedManhwa.isNotYetAiredOrPublished)
        assertEquals(futureDate, unreleasedManhwa.getComputedNextAiringEpoch())
        assertEquals("In 90d", unreleasedManhwa.getTimeUntilNextEpisode())
        org.junit.Assert.assertTrue(unreleasedManhwa.formatAiringCardText()!!.endsWith("• In 90d"))

        // 5. Unreleased Book / Light Novel
        val unreleasedBook = MediaEntry(
            title = "Future Light Novel",
            mediaType = MediaType.BOOK,
            status = MediaStatus.PLAN_TO_WATCH,
            airedStartDate = futureDate
        )
        org.junit.Assert.assertTrue(unreleasedBook.isNotYetAiredOrPublished)
        assertEquals(futureDate, unreleasedBook.getComputedNextAiringEpoch())
        assertEquals("In 90d", unreleasedBook.getTimeUntilNextEpisode())
        org.junit.Assert.assertTrue(unreleasedBook.formatAiringCardText()!!.endsWith("• In 90d"))
    }

    @Test
    fun testCalculateSpentAndRemainingTime() {
        // 1. Anime: 10 of 24 ep watched (24 min each), 1 rewatch
        val anime = MediaEntry(
            title = "Test Anime",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            currentEpisodes = 10,
            totalEpisodes = 24,
            episodeLengthMinutes = 24,
            rewatchCount = 1
        )
        // Spent: (10 + 1 * 24) * 24 = 34 * 24 = 816 min
        assertEquals(816.0, anime.calculateSpentTimeMinutes(), 0.01)
        // Remaining: (24 - 10) * 24 = 14 * 24 = 336 min
        assertEquals(336.0, anime.calculateRemainingTimeMinutes(), 0.01)

        // Completed Anime: remaining is 0.0
        val animeCompleted = anime.copy(status = MediaStatus.COMPLETED, currentEpisodes = 24)
        assertEquals(0.0, animeCompleted.calculateRemainingTimeMinutes(), 0.01)

        // Dropped 500-episode Anime: remaining is 0.0 (STA-02)
        val dropped500EpAnime = MediaEntry(
            title = "Dropped Long Series",
            mediaType = MediaType.ANIME,
            status = MediaStatus.DROPPED,
            currentEpisodes = 12,
            totalEpisodes = 500,
            episodeLengthMinutes = 24
        )
        assertEquals(0.0, dropped500EpAnime.calculateRemainingTimeMinutes(), 0.01)

        // 2. Movie: 120 min duration, watched 40 min, 0 rewatch
        val movie = MediaEntry(
            title = "Test Movie",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.WATCHING,
            durationMinutes = 120,
            currentTimeMinutes = 40,
            rewatchCount = 0
        )
        // Spent: 40 min
        assertEquals(40.0, movie.calculateSpentTimeMinutes(), 0.01)
        // Remaining: 120 - 40 = 80 min
        assertEquals(80.0, movie.calculateRemainingTimeMinutes(), 0.01)

        // Movie completed with 2 rewatches: spent = (1 + 2) * 120 = 360 min, remaining = 0
        val movieDone = movie.copy(status = MediaStatus.COMPLETED, rewatchCount = 2)
        assertEquals(360.0, movieDone.calculateSpentTimeMinutes(), 0.01)
        assertEquals(0.0, movieDone.calculateRemainingTimeMinutes(), 0.01)

        // 3. Manga: 50 chapters read out of 100
        val manga = MediaEntry(
            title = "Test Manga",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            currentChapters = 50,
            totalChapters = 100,
            rewatchCount = 0
        )
        // Spent: 50 * 5.0 = 250 min
        assertEquals(250.0, manga.calculateSpentTimeMinutes(), 0.01)
        // Remaining: (100 - 50) * 5.0 = 250 min
        assertEquals(250.0, manga.calculateRemainingTimeMinutes(), 0.01)

        // 4. Book: 300 pages read out of 400
        val book = MediaEntry(
            title = "Test Book",
            mediaType = MediaType.BOOK,
            status = MediaStatus.WATCHING,
            currentPages = 300,
            totalPages = 400,
            rewatchCount = 0
        )
        // Spent: 300 * 1.5 = 450 min
        assertEquals(450.0, book.calculateSpentTimeMinutes(), 0.01)
        // Remaining: (400 - 300) * 1.5 = 150 min
        assertEquals(150.0, book.calculateRemainingTimeMinutes(), 0.01)

        // 5. Game with Hours unit: current 25.5 hrs, target 50 hrs
        val gameHours = MediaEntry(
            title = "Test Game Hours",
            mediaType = MediaType.GAME,
            status = MediaStatus.WATCHING,
            customUnitLabel = "Hours",
            customUnitCurrent = 25.5,
            customUnitTotal = 50.0
        )
        // Spent: 25.5 * 60 = 1530 min
        assertEquals(1530.0, gameHours.calculateSpentTimeMinutes(), 0.01)
        // Remaining: (50 - 25.5) * 60 = 24.5 * 60 = 1470 min
        assertEquals(1470.0, gameHours.calculateRemainingTimeMinutes(), 0.01)

        // 6. Game with percentage and community avg completion time: 50% of 1200 min (20 hrs)
        val gamePercent = MediaEntry(
            title = "Test Game Percent",
            mediaType = MediaType.GAME,
            status = MediaStatus.WATCHING,
            gameProgressPercent = 50,
            durationMinutes = 1200
        )
        // Spent: 0.5 * 1200 = 600 min
        assertEquals(600.0, gamePercent.calculateSpentTimeMinutes(), 0.01)
        // Remaining: (1.0 - 0.5) * 1200 = 600 min
        assertEquals(600.0, gamePercent.calculateRemainingTimeMinutes(), 0.01)
    }

    @Test
    fun testDynamicSortDisplayLabels() {
        val customAnime = CustomCategory(name = "Donghua", isWatchType = true)
        val customManga = CustomCategory(name = "Webtoons", isWatchType = false)
        val customCats = listOf(customAnime, customManga)

        // TopNavBar SortOption
        assertEquals("Most Time Watched", SortOption.TIME_SPENT_DESC.getDisplayLabel(MediaType.ANIME))
        assertEquals("Most Time Watched", SortOption.TIME_SPENT_DESC.getDisplayLabel(MediaType.SHOW))
        assertEquals("Most Time Watched", SortOption.TIME_SPENT_DESC.getDisplayLabel(MediaType.MOVIE))
        assertEquals("Most Time Played", SortOption.TIME_SPENT_DESC.getDisplayLabel(MediaType.GAME))
        assertEquals("Most Time Read", SortOption.TIME_SPENT_DESC.getDisplayLabel(MediaType.MANGA))
        assertEquals("Most Time Read", SortOption.TIME_SPENT_DESC.getDisplayLabel(MediaType.BOOK))
        assertEquals("Most Time Watched / Played / Read", SortOption.TIME_SPENT_DESC.getDisplayLabel(null))

        assertEquals("Most Time To Watch", SortOption.TIME_REMAINING_DESC.getDisplayLabel(MediaType.ANIME))
        assertEquals("Most Time To Play", SortOption.TIME_REMAINING_DESC.getDisplayLabel(MediaType.GAME))
        assertEquals("Most Time To Read", SortOption.TIME_REMAINING_DESC.getDisplayLabel(MediaType.MANGA))
        assertEquals("Most Time To Watch / Play / Read", SortOption.TIME_REMAINING_DESC.getDisplayLabel(null))

        // Custom category resolution
        assertEquals("Most Time Watched", SortOption.TIME_SPENT_DESC.getDisplayLabel(null, "Donghua", customCats))
        assertEquals("Most Time Read", SortOption.TIME_SPENT_DESC.getDisplayLabel(null, "Webtoons", customCats))
        assertEquals("Most Time To Watch", SortOption.TIME_REMAINING_DESC.getDisplayLabel(null, "Donghua", customCats))
        assertEquals("Most Time To Read", SortOption.TIME_REMAINING_DESC.getDisplayLabel(null, "Webtoons", customCats))

        // LibrarySortOption
        assertEquals("Most Time Watched", LibrarySortOption.TIME_SPENT_DESC.getDisplayLabel(MediaType.ANIME))
        assertEquals("Most Time Played", LibrarySortOption.TIME_SPENT_DESC.getDisplayLabel(MediaType.GAME))
        assertEquals("Most Time Read", LibrarySortOption.TIME_SPENT_DESC.getDisplayLabel(MediaType.MANGA))
        assertEquals("Most Time To Watch", LibrarySortOption.TIME_REMAINING_DESC.getDisplayLabel(MediaType.ANIME))
        assertEquals("Most Time To Play", LibrarySortOption.TIME_REMAINING_DESC.getDisplayLabel(MediaType.GAME))
        assertEquals("Most Time To Read", LibrarySortOption.TIME_REMAINING_DESC.getDisplayLabel(MediaType.MANGA))
        assertEquals("Most Time Watched", LibrarySortOption.TIME_SPENT_DESC.getDisplayLabel(null, "Donghua", customCats))
        assertEquals("Most Time Read", LibrarySortOption.TIME_SPENT_DESC.getDisplayLabel(null, "Webtoons", customCats))
    }

    @Test
    fun `isLightNovel follows canonical book marker and source rules`() {
        assertTrue(
            MediaEntry(
                title = "Spice and Wolf",
                mediaType = MediaType.BOOK,
                status = MediaStatus.WATCHING,
                subCategory = " Light Novel "
            ).isLightNovel()
        )
        assertTrue(
            MediaEntry(
                title = "Overlord",
                mediaType = MediaType.BOOK,
                status = MediaStatus.PLAN_TO_WATCH,
                customCategory = "LN"
            ).isLightNovel()
        )
        assertTrue(
            MediaEntry(
                title = "86",
                mediaType = MediaType.BOOK,
                status = MediaStatus.WATCHING,
                externalSource = "AniList"
            ).isLightNovel()
        )
        assertTrue(
            MediaEntry(
                title = "Monogatari",
                mediaType = MediaType.BOOK,
                status = MediaStatus.WATCHING,
                externalSource = "MAL"
            ).isLightNovel()
        )
        assertFalse(
            MediaEntry(
                title = "Dune",
                mediaType = MediaType.BOOK,
                status = MediaStatus.COMPLETED,
                externalSource = "Google Books"
            ).isLightNovel()
        )
        assertFalse(
            MediaEntry(
                title = "Omniscient Reader",
                mediaType = MediaType.BOOK,
                status = MediaStatus.WATCHING,
                subCategory = "Web Novel"
            ).isLightNovel()
        )
        assertFalse(
            MediaEntry(
                title = "Not a Book",
                mediaType = MediaType.MANGA,
                status = MediaStatus.WATCHING,
                subCategory = "Light Novel"
            ).isLightNovel()
        )
    }

    @Test
    fun `typed accessors for overloaded columns resolve per media type and do not serialize`() {
        val movie = MediaEntry(
            title = "Inception",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.COMPLETED,
            durationMinutes = 148,
            movieSeriesName = "Standalone",
            movieSeriesNumber = 1
        )
        assertEquals(148, movie.runtimeMinutes)
        org.junit.Assert.assertNull(movie.trackDurationSeconds)
        org.junit.Assert.assertNull(movie.avgCompletionMinutes)
        org.junit.Assert.assertNull(movie.albumName)
        org.junit.Assert.assertNull(movie.trackNumber)

        val music = MediaEntry(
            title = "Papercut",
            mediaType = MediaType.MUSIC,
            status = MediaStatus.COMPLETED,
            durationMinutes = 185,
            movieSeriesName = "Hybrid Theory",
            movieSeriesNumber = 1
        )
        org.junit.Assert.assertNull(music.runtimeMinutes)
        assertEquals(185, music.trackDurationSeconds)
        org.junit.Assert.assertNull(music.avgCompletionMinutes)
        assertEquals("Hybrid Theory", music.albumName)
        assertEquals(1, music.trackNumber)

        val game = MediaEntry(
            title = "Hades",
            mediaType = MediaType.GAME,
            status = MediaStatus.WATCHING,
            durationMinutes = 3000
        )
        org.junit.Assert.assertNull(game.runtimeMinutes)
        org.junit.Assert.assertNull(game.trackDurationSeconds)
        assertEquals(3000, game.avgCompletionMinutes)

        // Computed properties must not leak into JSON export
        val json = com.lx.tracklist.data.exporter.JsonExporter.exportToJson(listOf(movie, music, game))
        assertFalse(json.contains("\"runtimeMinutes\""))
        assertFalse(json.contains("\"trackDurationSeconds\""))
        assertFalse(json.contains("\"avgCompletionMinutes\""))
        assertFalse(json.contains("\"albumName\""))
        assertFalse(json.contains("\"trackNumber\""))
    }
}
