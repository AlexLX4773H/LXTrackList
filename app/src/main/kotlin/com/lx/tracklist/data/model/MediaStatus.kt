package com.lx.tracklist.data.model

enum class MediaStatus(val defaultLabel: String) {
    WATCHING("Watching"),
    PLAN_TO_WATCH("Plan to Watch"),
    COMPLETED("Completed"),
    ON_HOLD("On Hold"),
    DROPPED("Dropped");

    fun getDisplayLabel(mediaType: MediaType): String {
        return when (this) {
            WATCHING -> mediaType.consumeVerbIng()
            PLAN_TO_WATCH -> "Plan to ${mediaType.consumeVerb()}"
            COMPLETED -> "Completed"
            ON_HOLD -> "On Hold"
            DROPPED -> "Dropped"
        }
    }

    /** Neutral status label for mixed-category views (e.g. All tab, mixed batch dialogs). */
    fun getNeutralLabel(): String = when (this) {
        WATCHING -> "In Progress"
        PLAN_TO_WATCH -> "Planned"
        COMPLETED -> "Completed"
        ON_HOLD -> "On Hold"
        DROPPED -> "Dropped"
    }

    /**
     * Returns the category-specific display label if [types] resolves to a single [MediaType],
     * or [getNeutralLabel] when [types] is empty or spans multiple media types.
     */
    fun labelFor(types: Collection<MediaType>): String {
        val distinct = types.distinct()
        return if (distinct.size == 1) getDisplayLabel(distinct.first()) else getNeutralLabel()
    }

    companion object {
        fun fromString(value: String): MediaStatus {
            val normalized = value.trim().uppercase().replace(" ", "_").replace("-", "_")
            return when {
                normalized.contains("PLAYING") || normalized.contains("WATCHING") || normalized.contains("READING") || normalized.contains("LISTENING") -> WATCHING
                normalized.contains("PLAN") -> PLAN_TO_WATCH
                normalized.contains("COMPLETE") -> COMPLETED
                normalized.contains("HOLD") -> ON_HOLD
                normalized.contains("DROP") -> DROPPED
                else -> WATCHING
            }
        }

        fun fromMalStatus(status: Int): MediaStatus {
            return when (status) {
                1, 7 -> WATCHING
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
