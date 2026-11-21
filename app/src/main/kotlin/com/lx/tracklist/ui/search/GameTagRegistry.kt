package com.lx.tracklist.ui.search

data class GameGenreDefinition(
    val name: String,
    val igdbId: Int? = null,
    val rawgSlug: String? = null
) {
    constructor(name: String, id: Int) : this(name, id, null)

    val id: Int get() = igdbId ?: 0
}

object GameTagRegistry {

    val gameGenres: List<GameGenreDefinition> = listOf(
        GameGenreDefinition("Action", igdbId = null, rawgSlug = "action"),
        GameGenreDefinition("Adventure", igdbId = 31, rawgSlug = "adventure"),
        GameGenreDefinition("Arcade", igdbId = 33, rawgSlug = "arcade"),
        GameGenreDefinition("Card & Board Game", igdbId = 35, rawgSlug = "board-games"),
        GameGenreDefinition("Fighting", igdbId = 4, rawgSlug = "fighting"),
        GameGenreDefinition("Hack and slash/Beat 'em up", igdbId = 25, rawgSlug = "action"),
        GameGenreDefinition("Indie", igdbId = 32, rawgSlug = "indie"),
        GameGenreDefinition("MOBA", igdbId = 36, rawgSlug = "massively-multiplayer"),
        GameGenreDefinition("Music", igdbId = 7, rawgSlug = "casual"),
        GameGenreDefinition("Pinball", igdbId = 30, rawgSlug = "arcade"),
        GameGenreDefinition("Platform", igdbId = 8, rawgSlug = "platformer"),
        GameGenreDefinition("Point-and-click", igdbId = 2, rawgSlug = "adventure"),
        GameGenreDefinition("Puzzle", igdbId = 9, rawgSlug = "puzzle"),
        GameGenreDefinition("Racing", igdbId = 10, rawgSlug = "racing"),
        GameGenreDefinition("Real Time Strategy (RTS)", igdbId = 11, rawgSlug = "strategy"),
        GameGenreDefinition("Role-playing (RPG)", igdbId = 12, rawgSlug = "role-playing-games-rpg"),
        GameGenreDefinition("Shooter", igdbId = 5, rawgSlug = "shooter"),
        GameGenreDefinition("Simulator", igdbId = 13, rawgSlug = "simulation"),
        GameGenreDefinition("Sport", igdbId = 14, rawgSlug = "sports"),
        GameGenreDefinition("Strategy", igdbId = 15, rawgSlug = "strategy"),
        GameGenreDefinition("Tactical", igdbId = 24, rawgSlug = "strategy"),
        GameGenreDefinition("Turn-based strategy (TBS)", igdbId = 16, rawgSlug = "strategy"),
        GameGenreDefinition("Visual Novel", igdbId = 34, rawgSlug = "adventure")
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
