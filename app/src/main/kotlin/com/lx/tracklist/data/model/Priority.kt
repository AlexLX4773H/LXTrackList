package com.lx.tracklist.data.model

enum class Priority(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    companion object {
        fun fromString(value: String): Priority {
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) 
            } ?: MEDIUM
        }
    }
}
