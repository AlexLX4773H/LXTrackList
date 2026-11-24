package com.lx.tracklist.ui.search

import java.util.Locale

data class MusicGenreTag(
    val name: String,
    val category: String = "Genre"
)

object MusicTagRegistry {
    val PRESET_GENRES = listOf(
        MusicGenreTag("Pop"),
        MusicGenreTag("Rock"),
        MusicGenreTag("Hip-Hop / Rap"),
        MusicGenreTag("R&B / Soul"),
        MusicGenreTag("Electronic / EDM"),
        MusicGenreTag("Indie / Alternative"),
        MusicGenreTag("Classical"),
        MusicGenreTag("Jazz"),
        MusicGenreTag("Metal"),
        MusicGenreTag("Anime OST"),
        MusicGenreTag("Video Game OST"),
        MusicGenreTag("Soundtrack"),
        MusicGenreTag("J-Pop"),
        MusicGenreTag("K-Pop"),
        MusicGenreTag("Folk / Acoustic"),
        MusicGenreTag("Country"),
        MusicGenreTag("Blues"),
        MusicGenreTag("Reggae"),
        MusicGenreTag("Punk"),
        MusicGenreTag("Ambient / Lo-Fi")
    )

    fun filterMusicGenres(query: String): List<MusicGenreTag> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isBlank()) return PRESET_GENRES
        return PRESET_GENRES.filter { it.name.lowercase(Locale.ROOT).contains(q) }
    }
}
