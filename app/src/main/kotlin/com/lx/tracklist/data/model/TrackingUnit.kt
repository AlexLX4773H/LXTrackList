package com.lx.tracklist.data.model

enum class TrackingUnit(val displayName: String) {
    CHAPTERS("Chapters"),
    VOLUMES("Volumes"),
    PAGES("Pages"),
    COMBINED("Combined"),
    PERCENTAGE("Percentage"),
    CUSTOM("Custom Unit");

    companion object {
        /** Tracking units supported for reading media types (Manga, Manhwa, Books/Light Novels). */
        val READING_UNITS: List<TrackingUnit> = listOf(CHAPTERS, VOLUMES, PAGES, COMBINED)

        /**
         * Coerces legacy unsupported reading units ([PERCENTAGE], [CUSTOM]) to [CHAPTERS]
         * when [mediaType] is a reading type.
         */
        fun sanitizeForMediaType(unit: TrackingUnit, mediaType: MediaType): TrackingUnit =
            if (mediaType.isReadType && unit !in READING_UNITS) CHAPTERS else unit

        fun fromString(value: String): TrackingUnit {
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) 
            } ?: CHAPTERS
        }
    }
}
