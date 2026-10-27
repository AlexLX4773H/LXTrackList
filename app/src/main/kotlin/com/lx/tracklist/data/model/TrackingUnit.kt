package com.lx.tracklist.data.model

enum class TrackingUnit(val displayName: String) {
    CHAPTERS("Chapters"),
    VOLUMES("Volumes"),
    PAGES("Pages"),
    COMBINED("Combined");

    companion object {
        fun fromString(value: String): TrackingUnit {
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) 
            } ?: CHAPTERS
        }
    }
}
