package com.lx.tracklist.ui.library

import com.lx.tracklist.data.model.ExternalSources
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.ui.search.GamePlatformRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFilterTest {

    @Test
    fun `BUG-21 platform filter applies only to GAME entries so switching to ANIME keeps anime visible`() {
        val pcGame = MediaEntry(
            id = 1L,
            title = "Hollow Knight",
            mediaType = MediaType.GAME,
            status = MediaStatus.COMPLETED,
            gamePlatforms = "PC, Nintendo Switch"
        )
        val psGame = MediaEntry(
            id = 2L,
            title = "Bloodborne",
            mediaType = MediaType.GAME,
            status = MediaStatus.WATCHING,
            gamePlatforms = "PlayStation 4"
        )
        val anime = MediaEntry(
            id = 3L,
            title = "Frieren",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING,
            originalLanguage = "ja"
        )

        val allWithPcFilter = LibraryFilterState(
            categoryFilter = null,
            includedPlatforms = setOf("PC")
        )
        val allResult = filterLibraryEntries(listOf(pcGame, psGame, anime), allWithPcFilter)
        assertEquals(listOf(1L, 3L), allResult.map { it.id })

        val animeWithPcFilter = allWithPcFilter.copy(categoryFilter = MediaType.ANIME)
        val animeResult = filterLibraryEntries(listOf(pcGame, psGame, anime), animeWithPcFilter)
        assertEquals(listOf(3L), animeResult.map { it.id })

        val excludePcFilter = LibraryFilterState(excludedPlatforms = setOf("PC"))
        val excludedResult = filterLibraryEntries(listOf(pcGame, psGame, anime), excludePcFilter)
        assertEquals(listOf(2L, 3L), excludedResult.map { it.id })
    }

    @Test
    fun `BUG-21 language filter applies across all types including GAME entries`() {
        val jpGame = MediaEntry(
            id = 1L,
            title = "Elden Ring",
            mediaType = MediaType.GAME,
            status = MediaStatus.WATCHING,
            originalLanguage = "ja"
        )
        val krManhwa = MediaEntry(
            id = 2L,
            title = "Solo Leveling",
            mediaType = MediaType.MANHWA,
            status = MediaStatus.COMPLETED,
            originalLanguage = "ko"
        )
        val enGame = MediaEntry(
            id = 3L,
            title = "Baldur's Gate 3",
            mediaType = MediaType.GAME,
            status = MediaStatus.PLAN_TO_WATCH,
            originalLanguage = "en"
        )

        val jaFilter = LibraryFilterState(includedLanguages = setOf("ja", "ko"))
        val filtered = filterLibraryEntries(listOf(jpGame, krManhwa, enGame), jaFilter)
        assertEquals(listOf(1L, 2L), filtered.map { it.id })
    }

    @Test
    fun `BUG-21 activeFilterCount and badge helpers cover platforms languages and valid external ID`() {
        val filter = LibraryFilterState(
            includedPlatforms = setOf("PC", "Steam"),
            excludedPlatforms = setOf("Mobile"),
            includedLanguages = setOf("ja", "ko"),
            validExternalIdFilter = InclusionFilterOption.INCLUDE_ONLY
        )
        assertEquals(3, filter.activeFilterCount)
        assertEquals("Platforms +2 / −1", formatPlatformsBadgeLabel(filter.includedPlatforms.size, filter.excludedPlatforms.size))
        assertEquals("Platforms +1", formatPlatformsBadgeLabel(1, 0))
        assertEquals("Platforms −1", formatPlatformsBadgeLabel(0, 1))
        assertEquals("Language: ja, ko", formatLanguagesBadgeLabel(filter.includedLanguages))

        val linked = MediaEntry(id = 1L, title = "A", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, externalId = "5114", externalSource = ExternalSources.MAL)
        val unlinked = MediaEntry(id = 2L, title = "B", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING, externalId = null, externalSource = null)
        val validOnlyResult = filterLibraryEntries(listOf(linked, unlinked), LibraryFilterState(validExternalIdFilter = InclusionFilterOption.INCLUDE_ONLY))
        assertTrue(validOnlyResult.map { it.id } == listOf(1L))
    }

    @Test
    fun `PAR-10 Library platform filter exposes the six umbrella labels and matches stored canonical platforms`() {
        assertEquals(
            listOf("PC", "PlayStation", "Xbox", "Switch", "Mobile", "Retro"),
            GamePlatformRegistry.LIBRARY_UMBRELLA_PLATFORMS.map { it.displayName }
        )
        assertEquals(
            MediaEntry.CANONICAL_GAME_PLATFORMS,
            GamePlatformRegistry.LIBRARY_UMBRELLA_PLATFORMS.map { it.id }
        )

        val entries = listOf(
            MediaEntry(id = 1L, title = "PC Game", mediaType = MediaType.GAME, status = MediaStatus.WATCHING, gamePlatforms = "PC"),
            MediaEntry(id = 2L, title = "PS Game", mediaType = MediaType.GAME, status = MediaStatus.WATCHING, gamePlatforms = "PS"),
            MediaEntry(id = 3L, title = "Xbox Game", mediaType = MediaType.GAME, status = MediaStatus.WATCHING, gamePlatforms = "XBox"),
            MediaEntry(id = 4L, title = "Switch Game", mediaType = MediaType.GAME, status = MediaStatus.WATCHING, gamePlatforms = "Switch"),
            MediaEntry(id = 5L, title = "Mobile Game", mediaType = MediaType.GAME, status = MediaStatus.WATCHING, gamePlatforms = "Mobile"),
            MediaEntry(id = 6L, title = "Retro Game", mediaType = MediaType.GAME, status = MediaStatus.WATCHING, gamePlatforms = "Retro")
        )

        assertEquals(listOf(1L), filterLibraryEntries(entries, LibraryFilterState(includedPlatforms = setOf("PC"))).map { it.id })
        assertEquals(listOf(2L), filterLibraryEntries(entries, LibraryFilterState(includedPlatforms = setOf("PlayStation"))).map { it.id })
        assertEquals(listOf(3L), filterLibraryEntries(entries, LibraryFilterState(includedPlatforms = setOf("Xbox"))).map { it.id })
        assertEquals(listOf(4L), filterLibraryEntries(entries, LibraryFilterState(includedPlatforms = setOf("Switch"))).map { it.id })
        assertEquals(listOf(5L), filterLibraryEntries(entries, LibraryFilterState(includedPlatforms = setOf("Mobile"))).map { it.id })
        assertEquals(listOf(6L), filterLibraryEntries(entries, LibraryFilterState(includedPlatforms = setOf("Retro"))).map { it.id })

        // Excluding one umbrella leaves all other umbrella entries intact
        assertEquals(
            listOf(1L, 3L, 4L, 5L, 6L),
            filterLibraryEntries(entries, LibraryFilterState(excludedPlatforms = setOf("PlayStation"))).map { it.id }
        )

        // Granular non-umbrella names are never offered/matched in Library filtering
        assertEquals(emptyList<Long>(), filterLibraryEntries(entries, LibraryFilterState(includedPlatforms = setOf("PlayStation 5"))).map { it.id })
    }
}
