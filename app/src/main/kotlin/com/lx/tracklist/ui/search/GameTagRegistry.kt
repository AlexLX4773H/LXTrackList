package com.lx.tracklist.ui.search

data class GameGenreDefinition(
    val name: String,
    val id: Int
)

object GameTagRegistry {

    val gameGenres: List<GameGenreDefinition> = listOf(
        GameGenreDefinition("Action", 4),
        GameGenreDefinition("Adventure", 31),
        GameGenreDefinition("Arcade", 33),
        GameGenreDefinition("Card & Board Game", 35),
        GameGenreDefinition("Fighting", 11),
        GameGenreDefinition("Hack and slash/Beat 'em up", 25),
        GameGenreDefinition("Indie", 32),
        GameGenreDefinition("MOBA", 36),
        GameGenreDefinition("Music", 7),
        GameGenreDefinition("Pinball", 30),
        GameGenreDefinition("Platform", 8),
        GameGenreDefinition("Point-and-click", 2),
        GameGenreDefinition("Puzzle", 9),
        GameGenreDefinition("Racing", 10),
        GameGenreDefinition("Real Time Strategy (RTS)", 11),
        GameGenreDefinition("Role-playing (RPG)", 12),
        GameGenreDefinition("Shooter", 5),
        GameGenreDefinition("Simulator", 13),
        GameGenreDefinition("Sport", 14),
        GameGenreDefinition("Strategy", 15),
        GameGenreDefinition("Tactical", 24),
        GameGenreDefinition("Turn-based strategy (TBS)", 16),
        GameGenreDefinition("Visual Novel", 34)
    ).sortedBy { it.name.lowercase() }

    fun filterGameGenres(query: String): List<GameGenreDefinition> {
        val clean = query.trim().lowercase()
        return gameGenres
            .filter { clean.isEmpty() || it.name.lowercase().contains(clean) }
            .sortedBy { it.name.lowercase() }
    }

    fun findGenre(name: String): GameGenreDefinition? =
        gameGenres.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
}
