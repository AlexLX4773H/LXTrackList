package com.lx.tracklist.ui.search

import com.lx.tracklist.data.model.MediaEntry
import java.util.Locale

data class GamePlatformOption(
    val id: String,                         // e.g. "PC", "Switch", "PS5", "PS4", "PS", "Xbox Series", "Xbox One", "XBox", "Android", "iOS", "Mobile", "Retro"
    val displayName: String,                // e.g. "PC", "Nintendo Switch", "PlayStation 5", "PlayStation 4", "PlayStation (All)", "Xbox Series X/S", "Xbox One", "Xbox (All)", "Android", "iOS", "Mobile (All)", "Retro / Classics"
    val category: String,                   // "Popular", "PlayStation", "Xbox", "Nintendo", "Mobile", "Retro / Other"
    val iconEmoji: String,                  // "💻", "🎮", "🕹️", "📱"
    val aliases: List<String> = emptyList(),// Search and candidate matching aliases
    val igdbPlatformIds: List<Int> = emptyList(), // IGDB platform IDs
    val rawgPlatformIds: List<Int> = emptyList()  // RAWG platform IDs
)

object GamePlatformRegistry {

    val ALL_PLATFORMS: List<GamePlatformOption> = listOf(
        // Popular / Major
        GamePlatformOption(
            id = "PC",
            displayName = "PC",
            category = "Popular",
            iconEmoji = "💻",
            aliases = listOf("pc", "windows", "microsoft windows", "steam", "mac", "macos", "linux", "dos", "gog", "epic"),
            igdbPlatformIds = listOf(6, 14, 3), // PC (Microsoft Windows), Mac, Linux
            rawgPlatformIds = listOf(4, 5, 6)   // PC, macOS, Linux
        ),
        GamePlatformOption(
            id = "Switch",
            displayName = "Nintendo Switch",
            category = "Popular",
            iconEmoji = "🎮",
            aliases = listOf("switch", "nintendo switch"),
            igdbPlatformIds = listOf(130),
            rawgPlatformIds = listOf(7)
        ),
        GamePlatformOption(
            id = "PS5",
            displayName = "PlayStation 5",
            category = "Popular",
            iconEmoji = "🎮",
            aliases = listOf("ps5", "playstation 5"),
            igdbPlatformIds = listOf(167),
            rawgPlatformIds = listOf(187)
        ),
        GamePlatformOption(
            id = "PS4",
            displayName = "PlayStation 4",
            category = "Popular",
            iconEmoji = "🎮",
            aliases = listOf("ps4", "playstation 4"),
            igdbPlatformIds = listOf(48),
            rawgPlatformIds = listOf(18)
        ),
        GamePlatformOption(
            id = "Xbox Series",
            displayName = "Xbox Series X/S",
            category = "Popular",
            iconEmoji = "🎮",
            aliases = listOf("xbox series", "series x", "series s", "xbsx", "xbox series x/s"),
            igdbPlatformIds = listOf(169),
            rawgPlatformIds = listOf(186)
        ),
        GamePlatformOption(
            id = "Xbox One",
            displayName = "Xbox One",
            category = "Popular",
            iconEmoji = "🎮",
            aliases = listOf("xbox one", "xone", "xbone"),
            igdbPlatformIds = listOf(49),
            rawgPlatformIds = listOf(1)
        ),
        GamePlatformOption(
            id = "Android",
            displayName = "Android",
            category = "Popular",
            iconEmoji = "📱",
            aliases = listOf("android", "google play"),
            igdbPlatformIds = listOf(34),
            rawgPlatformIds = listOf(21)
        ),
        GamePlatformOption(
            id = "iOS",
            displayName = "iOS",
            category = "Popular",
            iconEmoji = "📱",
            aliases = listOf("ios", "iphone", "ipad", "apple app store"),
            igdbPlatformIds = listOf(39),
            rawgPlatformIds = listOf(3)
        ),

        // PlayStation Family
        GamePlatformOption(
            id = "PS",
            displayName = "PlayStation (All)",
            category = "PlayStation",
            iconEmoji = "🎮",
            aliases = listOf("ps", "playstation", "ps5", "ps4", "ps3", "ps2", "ps1", "psx", "psp", "vita"),
            igdbPlatformIds = listOf(167, 48, 9, 8, 7, 46, 38),
            rawgPlatformIds = listOf(187, 18, 16, 15, 27, 19, 17)
        ),
        GamePlatformOption(
            id = "PS3",
            displayName = "PlayStation 3",
            category = "PlayStation",
            iconEmoji = "🎮",
            aliases = listOf("ps3", "playstation 3"),
            igdbPlatformIds = listOf(9),
            rawgPlatformIds = listOf(16)
        ),
        GamePlatformOption(
            id = "PS2",
            displayName = "PlayStation 2",
            category = "PlayStation",
            iconEmoji = "🎮",
            aliases = listOf("ps2", "playstation 2"),
            igdbPlatformIds = listOf(8),
            rawgPlatformIds = listOf(15)
        ),
        GamePlatformOption(
            id = "PS1",
            displayName = "PlayStation 1 (PSX)",
            category = "PlayStation",
            iconEmoji = "🎮",
            aliases = listOf("ps1", "psx", "playstation 1", "original playstation"),
            igdbPlatformIds = listOf(7),
            rawgPlatformIds = listOf(27)
        ),
        GamePlatformOption(
            id = "PS Vita",
            displayName = "PlayStation Vita",
            category = "PlayStation",
            iconEmoji = "🎮",
            aliases = listOf("vita", "ps vita", "playstation vita"),
            igdbPlatformIds = listOf(46),
            rawgPlatformIds = listOf(19)
        ),
        GamePlatformOption(
            id = "PSP",
            displayName = "PlayStation Portable",
            category = "PlayStation",
            iconEmoji = "🎮",
            aliases = listOf("psp", "playstation portable"),
            igdbPlatformIds = listOf(38),
            rawgPlatformIds = listOf(17)
        ),

        // Xbox Family
        GamePlatformOption(
            id = "XBox",
            displayName = "Xbox (All)",
            category = "Xbox",
            iconEmoji = "🎮",
            aliases = listOf("xbox", "xbox series", "xbox one", "xbox 360"),
            igdbPlatformIds = listOf(169, 49, 12, 11),
            rawgPlatformIds = listOf(186, 1, 14, 80)
        ),
        GamePlatformOption(
            id = "Xbox 360",
            displayName = "Xbox 360",
            category = "Xbox",
            iconEmoji = "🎮",
            aliases = listOf("xbox 360", "x360"),
            igdbPlatformIds = listOf(12),
            rawgPlatformIds = listOf(14)
        ),
        GamePlatformOption(
            id = "Original Xbox",
            displayName = "Original Xbox",
            category = "Xbox",
            iconEmoji = "🎮",
            aliases = listOf("original xbox", "xbox original"),
            igdbPlatformIds = listOf(11),
            rawgPlatformIds = listOf(80)
        ),

        // Nintendo Family
        GamePlatformOption(
            id = "3DS",
            displayName = "Nintendo 3DS",
            category = "Nintendo",
            iconEmoji = "🎮",
            aliases = listOf("3ds", "nintendo 3ds"),
            igdbPlatformIds = listOf(37),
            rawgPlatformIds = listOf(8)
        ),
        GamePlatformOption(
            id = "DS",
            displayName = "Nintendo DS",
            category = "Nintendo",
            iconEmoji = "🎮",
            aliases = listOf("ds", "nintendo ds", "nds"),
            igdbPlatformIds = listOf(20),
            rawgPlatformIds = listOf(9)
        ),
        GamePlatformOption(
            id = "Wii U",
            displayName = "Nintendo Wii U",
            category = "Nintendo",
            iconEmoji = "🎮",
            aliases = listOf("wii u", "nintendo wii u"),
            igdbPlatformIds = listOf(41),
            rawgPlatformIds = listOf(10)
        ),
        GamePlatformOption(
            id = "Wii",
            displayName = "Nintendo Wii",
            category = "Nintendo",
            iconEmoji = "🎮",
            aliases = listOf("wii", "nintendo wii"),
            igdbPlatformIds = listOf(5),
            rawgPlatformIds = listOf(11)
        ),
        GamePlatformOption(
            id = "GameCube",
            displayName = "Nintendo GameCube",
            category = "Nintendo",
            iconEmoji = "🎮",
            aliases = listOf("gamecube", "gcn", "gc"),
            igdbPlatformIds = listOf(21),
            rawgPlatformIds = listOf(105)
        ),
        GamePlatformOption(
            id = "N64",
            displayName = "Nintendo 64",
            category = "Nintendo",
            iconEmoji = "🎮",
            aliases = listOf("n64", "nintendo 64"),
            igdbPlatformIds = listOf(4),
            rawgPlatformIds = listOf(83)
        ),
        GamePlatformOption(
            id = "GBA",
            displayName = "Game Boy Advance",
            category = "Nintendo",
            iconEmoji = "🎮",
            aliases = listOf("gba", "game boy advance"),
            igdbPlatformIds = listOf(24),
            rawgPlatformIds = listOf(24)
        ),
        GamePlatformOption(
            id = "SNES",
            displayName = "Super Nintendo (SNES)",
            category = "Nintendo",
            iconEmoji = "🕹️",
            aliases = listOf("snes", "super nintendo", "super nes"),
            igdbPlatformIds = listOf(19),
            rawgPlatformIds = listOf(79)
        ),
        GamePlatformOption(
            id = "NES",
            displayName = "NES (Famicom)",
            category = "Nintendo",
            iconEmoji = "🕹️",
            aliases = listOf("nes", "famicom"),
            igdbPlatformIds = listOf(18),
            rawgPlatformIds = listOf(49)
        ),

        // Mobile Family
        GamePlatformOption(
            id = "Mobile",
            displayName = "Mobile (Android / iOS)",
            category = "Mobile",
            iconEmoji = "📱",
            aliases = listOf("mobile", "phone", "android", "ios", "iphone", "ipad"),
            igdbPlatformIds = listOf(34, 39),
            rawgPlatformIds = listOf(21, 3)
        ),

        // Other Platforms
        GamePlatformOption(
            id = "Linux",
            displayName = "Linux",
            category = "Other",
            iconEmoji = "🐧",
            aliases = listOf("linux", "steamdeck", "steam deck", "steamos", "steam os", "gnu/linux", "ubuntu", "proton"),
            igdbPlatformIds = listOf(3),
            rawgPlatformIds = listOf(6)
        ),

        // Retro / Other
        GamePlatformOption(
            id = "Retro",
            displayName = "Retro / Classics",
            category = "Retro",
            iconEmoji = "🕹️",
            aliases = listOf("retro", "arcade", "sega", "genesis", "mega drive", "atari", "dreamcast", "saturn", "commodore", "amiga", "emulat"),
            igdbPlatformIds = listOf(18, 19, 24, 4, 21, 5, 7, 8, 33, 52, 29, 30),
            rawgPlatformIds = listOf(26, 49, 79, 24, 83, 105, 27, 15)
        )
    )

    fun getTopPlatforms(): List<GamePlatformOption> =
        ALL_PLATFORMS.filter { it.category == "Popular" }

    fun getOtherPlatforms(): List<GamePlatformOption> =
        ALL_PLATFORMS.filter { it.category != "Popular" }

    fun filterPlatforms(query: String): List<GamePlatformOption> {
        val clean = query.trim().lowercase(Locale.ROOT)
        if (clean.isBlank()) return ALL_PLATFORMS
        return ALL_PLATFORMS.filter { opt ->
            opt.displayName.lowercase(Locale.ROOT).contains(clean) ||
            opt.id.lowercase(Locale.ROOT).contains(clean) ||
            opt.category.lowercase(Locale.ROOT).contains(clean) ||
            opt.aliases.any { it.contains(clean) }
        }
    }

    fun findOption(idOrName: String?): GamePlatformOption? {
        if (idOrName.isNullOrBlank()) return null
        val clean = idOrName.trim().lowercase(Locale.ROOT)
        return ALL_PLATFORMS.firstOrNull { it.id.equals(clean, ignoreCase = true) }
            ?: ALL_PLATFORMS.firstOrNull { it.displayName.equals(clean, ignoreCase = true) }
            ?: ALL_PLATFORMS.firstOrNull { opt -> opt.aliases.any { it.equals(clean, ignoreCase = true) } }
            ?: ALL_PLATFORMS.firstOrNull { opt -> opt.displayName.lowercase(Locale.ROOT).contains(clean) }
    }

    fun matchesPlatform(filterPlatform: String, candidatePlatform: String): Boolean {
        if (filterPlatform.isBlank() || candidatePlatform.isBlank()) return false
        val filterClean = filterPlatform.trim()
        val candClean = candidatePlatform.trim()

        // 1. Direct case-insensitive match
        if (filterClean.equals(candClean, ignoreCase = true)) return true

        // 2. Canonical matching using MediaEntry canonicalize
        val filterCanon = MediaEntry.canonicalizeGamePlatform(filterClean)
        val candCanon = MediaEntry.canonicalizeGamePlatform(candClean)
        if (filterCanon.equals(candCanon, ignoreCase = true)) {
            // If the filter specifically requested a canonical umbrella, match
            if (MediaEntry.CANONICAL_GAME_PLATFORMS.any { it.equals(filterClean, ignoreCase = true) }) {
                return true
            }
        }

        // 3. Option lookup
        val opt = findOption(filterClean)
        if (opt != null) {
            val candLower = candClean.lowercase(Locale.ROOT)
            if (opt.id.equals(candClean, ignoreCase = true)) return true
            if (opt.displayName.equals(candClean, ignoreCase = true)) return true
            if (candLower.contains(opt.id.lowercase(Locale.ROOT))) return true
            if (candLower.contains(opt.displayName.lowercase(Locale.ROOT))) return true
            if (opt.aliases.any { alias -> candLower.contains(alias) || alias.contains(candLower) }) {
                return true
            }
        }

        // 4. Substring fallback
        val candLower = candClean.lowercase(Locale.ROOT)
        val filterLower = filterClean.lowercase(Locale.ROOT)
        if (filterLower.length >= 3 && (candLower.contains(filterLower) || filterLower.contains(candLower))) {
            return true
        }

        return false
    }

    /**
     * Multi-select OR condition matching:
     * Returns true if candidate platforms match ANY of the filter platforms.
     */
    fun matchesAnyPlatform(filterPlatforms: Collection<String>?, candidatePlatforms: Collection<String>?): Boolean {
        if (filterPlatforms.isNullOrEmpty()) return true
        if (candidatePlatforms.isNullOrEmpty()) return false
        return filterPlatforms.any { filterPlat ->
            candidatePlatforms.any { candPlat ->
                matchesPlatform(filterPlat, candPlat)
            }
        }
    }

    /**
     * Returns true if candidate platforms match ANY of the excluded platforms.
     */
    fun isExcluded(excludedPlatforms: Collection<String>?, candidatePlatforms: Collection<String>?): Boolean {
        if (excludedPlatforms.isNullOrEmpty()) return false
        if (candidatePlatforms.isNullOrEmpty()) return false
        return excludedPlatforms.any { excPlat ->
            candidatePlatforms.any { candPlat ->
                matchesPlatform(excPlat, candPlat)
            }
        }
    }
}
