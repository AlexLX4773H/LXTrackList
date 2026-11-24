package com.lx.tracklist.data.model

enum class MediaType(
    val displayName: String,
    val isWatchType: Boolean,
    val isReadType: Boolean,
    val isPlayType: Boolean = false,
    val isListenType: Boolean = false
) {
    ANIME("Anime", true, false),
    MANGA("Manga", false, true),
    MANHWA("Manhwa", false, true),
    MOVIE("Movies", true, false),
    SHOW("Shows", true, false),
    BOOK("Books", false, true),
    GAME("Games", false, false, true),
    MUSIC("Music", false, false, false, true);

    fun getCreatorRoleLabel(): String = when (this) {
        MUSIC -> "Artist"
        ANIME -> "Studio"
        MANGA, MANHWA, BOOK -> "Author"
        GAME -> "Developer"
        MOVIE -> "Director"
        SHOW -> "Creator / Network"
    }

    companion object {
        fun fromString(value: String): MediaType {
            val upper = value.trim().uppercase()
            if (upper == "GAME" || upper == "GAMES" || upper == "VIDEO GAME" || upper == "VIDEO GAMES") {
                return GAME
            }
            if (upper == "MUSIC" || upper == "SONG" || upper == "SONGS" || upper == "TRACK" || upper == "TRACKS") {
                return MUSIC
            }
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) 
            } ?: ANIME
        }
    }
}
