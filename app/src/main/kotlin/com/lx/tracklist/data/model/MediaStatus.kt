package com.lx.tracklist.data.model

enum class MediaStatus(val defaultLabel: String) {
    WATCHING("Watching"),
    COMPLETED("Completed"),
    ON_HOLD("On Hold"),
    DROPPED("Dropped"),
    PLAN_TO_WATCH("Plan to Watch");

    fun getDisplayLabel(mediaType: MediaType): String {
        return when (this) {
            WATCHING -> if (mediaType.isReadType) "Reading" else "Watching"
            PLAN_TO_WATCH -> if (mediaType.isReadType) "Plan to Read" else "Plan to Watch"
            COMPLETED -> "Completed"
            ON_HOLD -> "On Hold"
            DROPPED -> "Dropped"
        }
    }

    companion object {
        fun fromString(value: String): MediaStatus {
            val normalized = value.trim().uppercase().replace(" ", "_").replace("-", "_")
            return when {
                normalized.contains("WATCHING") || normalized.contains("READING") -> WATCHING
                normalized.contains("COMPLETE") -> COMPLETED
                normalized.contains("HOLD") -> ON_HOLD
                normalized.contains("DROP") -> DROPPED
                normalized.contains("PLAN") -> PLAN_TO_WATCH
                else -> WATCHING
            }
        }

        fun fromMalStatus(status: Int): MediaStatus {
            return when (status) {
                1 -> WATCHING
                2 -> COMPLETED
                3 -> ON_HOLD
                4 -> DROPPED
                6 -> PLAN_TO_WATCH
                else -> PLAN_TO_WATCH
            }
        }

        fun toMalStatus(status: MediaStatus): Int {
            return when (status) {
                WATCHING -> 1
                COMPLETED -> 2
                ON_HOLD -> 3
                DROPPED -> 4
                PLAN_TO_WATCH -> 6
            }
        }
    }
}
