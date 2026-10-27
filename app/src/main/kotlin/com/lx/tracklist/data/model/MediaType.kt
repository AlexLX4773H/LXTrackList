package com.lx.tracklist.data.model

enum class MediaType(val displayName: String, val isWatchType: Boolean, val isReadType: Boolean) {
    ANIME("Anime", true, false),
    MANGA("Manga", false, true),
    MANHWA("Manhwa", false, true),
    MOVIE("Movies", true, false),
    SHOW("Shows", true, false),
    BOOK("Books", false, true);

    companion object {
        fun fromString(value: String): MediaType {
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) 
            } ?: ANIME
        }
    }
}
