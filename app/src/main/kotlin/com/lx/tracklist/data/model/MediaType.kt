package com.lx.tracklist.data.model

enum class MediaType(
    val displayName: String,
    val isWatchType: Boolean,
    val isReadType: Boolean,
    val isPlayType: Boolean = false
) {
    ANIME("Anime", true, false),
    MANGA("Manga", false, true),
    MANHWA("Manhwa", false, true),
    MOVIE("Movies", true, false),
    SHOW("Shows", true, false),
    BOOK("Books", false, true),
    GAME("Games", false, false, true);

    companion object {
        fun fromString(value: String): MediaType {
            val upper = value.trim().uppercase()
            if (upper == "GAME" || upper == "GAMES" || upper == "VIDEO GAME" || upper == "VIDEO GAMES") {
                return GAME
            }
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) 
            } ?: ANIME
        }
    }
}
