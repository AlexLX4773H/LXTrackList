package com.lx.tracklist.data.model

enum class GameSeriesItemStatus(val displayName: String) {
    PLANNED("Planned"),
    PLAYING("Playing"),
    COMPLETED("Completed"),
    SKIPPED("Skipped"),
    DROPPED("Dropped")
}

data class GameSeriesItem(
    val id: String, // e.g. UUID or externalId
    val order: Int,
    val title: String,
    val externalId: String? = null,
    val externalSource: String? = null,
    val releaseYear: String? = null,
    val coverUrl: String? = null,
    val status: GameSeriesItemStatus = GameSeriesItemStatus.PLANNED,
    val progressPercent: Int = 0,
    val customUnitCurrent: Double = 0.0,
    val customUnitTotal: Double = 0.0,
    val platforms: List<String> = emptyList()
)
