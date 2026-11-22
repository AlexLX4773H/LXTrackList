package com.lx.tracklist.data.remote

import com.lx.tracklist.data.model.RelatedMediaItem

data class GameSearchResult(
    val externalId: String,
    val title: String,
    val alternateTitle: String? = null,
    val coverUrl: String? = null,
    val artworkUrl: String? = null,
    val releaseDateMillis: Long? = null,
    val releaseYear: String? = null,
    val platforms: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val score: Float? = null,
    val externalSource: String = "IGDB",
    val summary: String? = null,
    val isCollectionFranchise: Boolean = false,
    val collectionId: Long? = null,
    val collectionName: String? = null,
    val completionTimeMinutes: Int? = null
) {
    val webUrl: String
        get() = if (externalSource.equals("RAWG", ignoreCase = true)) {
            "https://rawg.io/games/$externalId"
        } else {
            "https://www.igdb.com/games/$externalId"
        }
}

data class GameDetails(
    val externalId: String,
    val externalSource: String = "IGDB",
    val title: String,
    val alternateTitles: List<String> = emptyList(),
    val summary: String? = null,
    val coverUrl: String? = null,
    val artworkUrl: String? = null,
    val releaseDateMillis: Long? = null,
    val releaseYear: String? = null,
    val platforms: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val themes: List<String> = emptyList(),
    val rating: Float? = null,
    val ratingSource: String = "IGDB",
    val collectionId: Long? = null,
    val collectionName: String? = null,
    val collectionGames: List<GameSearchResult> = emptyList(),
    val franchiseName: String? = null,
    val relatedItems: List<RelatedMediaItem> = emptyList(),
    val steamAppId: String? = null,
    val completionTimeMinutes: Int? = null,
    val normalTimeMinutes: Int? = null
)

data class SteamGridPosterItem(
    val id: Long,
    val url: String,
    val thumbUrl: String,
    val width: Int,
    val height: Int,
    val score: Int? = null,
    val style: String? = null,
    val author: String? = null
)
