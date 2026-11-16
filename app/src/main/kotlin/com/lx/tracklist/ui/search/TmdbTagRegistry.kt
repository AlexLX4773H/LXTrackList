package com.lx.tracklist.ui.search

data class TmdbGenreDefinition(
    val name: String,
    val id: Int
)

object TmdbTagRegistry {

    val movieGenres: List<TmdbGenreDefinition> = listOf(
        TmdbGenreDefinition("Action", 28),
        TmdbGenreDefinition("Adventure", 12),
        TmdbGenreDefinition("Animation", 16),
        TmdbGenreDefinition("Comedy", 35),
        TmdbGenreDefinition("Crime", 80),
        TmdbGenreDefinition("Documentary", 99),
        TmdbGenreDefinition("Drama", 18),
        TmdbGenreDefinition("Family", 10751),
        TmdbGenreDefinition("Fantasy", 14),
        TmdbGenreDefinition("History", 36),
        TmdbGenreDefinition("Horror", 27),
        TmdbGenreDefinition("Music", 10402),
        TmdbGenreDefinition("Mystery", 9648),
        TmdbGenreDefinition("Romance", 10749),
        TmdbGenreDefinition("Science Fiction", 878),
        TmdbGenreDefinition("TV Movie", 10770),
        TmdbGenreDefinition("Thriller", 53),
        TmdbGenreDefinition("War", 10752),
        TmdbGenreDefinition("Western", 37)
    ).sortedBy { it.name.lowercase() }

    val tvGenres: List<TmdbGenreDefinition> = listOf(
        TmdbGenreDefinition("Action & Adventure", 10759),
        TmdbGenreDefinition("Animation", 16),
        TmdbGenreDefinition("Comedy", 35),
        TmdbGenreDefinition("Crime", 80),
        TmdbGenreDefinition("Documentary", 99),
        TmdbGenreDefinition("Drama", 18),
        TmdbGenreDefinition("Family", 10751),
        TmdbGenreDefinition("Kids", 10762),
        TmdbGenreDefinition("Mystery", 9648),
        TmdbGenreDefinition("News", 10763),
        TmdbGenreDefinition("Reality", 10764),
        TmdbGenreDefinition("Sci-Fi & Fantasy", 10765),
        TmdbGenreDefinition("Soap", 10766),
        TmdbGenreDefinition("Talk", 10767),
        TmdbGenreDefinition("War & Politics", 10768),
        TmdbGenreDefinition("Western", 37)
    ).sortedBy { it.name.lowercase() }

    fun filterMovieGenres(query: String): List<TmdbGenreDefinition> {
        val clean = query.trim().lowercase()
        return movieGenres
            .filter { clean.isEmpty() || it.name.lowercase().contains(clean) }
            .sortedBy { it.name.lowercase() }
    }

    fun filterTvGenres(query: String): List<TmdbGenreDefinition> {
        val clean = query.trim().lowercase()
        return tvGenres
            .filter { clean.isEmpty() || it.name.lowercase().contains(clean) }
            .sortedBy { it.name.lowercase() }
    }

    fun getMovieGenreId(name: String): Int? =
        movieGenres.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }?.id

    fun getTvGenreId(name: String): Int? =
        tvGenres.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }?.id
}
