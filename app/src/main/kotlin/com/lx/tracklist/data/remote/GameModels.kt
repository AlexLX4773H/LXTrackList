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
    val developers: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
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

fun buildGameDescriptionWithMetadata(
    baseSummary: String?,
    developers: List<String> = emptyList(),
    publishers: List<String> = emptyList()
): String {
    val metaLines = mutableListOf<String>()
    val devList = developers.filter { it.isNotBlank() }.distinct()
    val pubList = publishers.filter { it.isNotBlank() }.distinct()

    if (devList.isNotEmpty()) {
        metaLines.add("Developer: ${devList.joinToString(", ")}")
    }
    if (pubList.isNotEmpty()) {
        metaLines.add("Publisher: ${pubList.joinToString(", ")}")
    }

    var cleanSummary = baseSummary?.trim().orEmpty()
    if (cleanSummary.isNotBlank()) {
        cleanSummary = cleanSummary.lines().filterNot { line ->
            val l = line.trim()
            l.startsWith("Developer:", ignoreCase = true) ||
            l.startsWith("Studio:", ignoreCase = true) ||
            l.startsWith("Studio / Developer:", ignoreCase = true) ||
            l.startsWith("Publisher:", ignoreCase = true)
        }.joinToString("\n").trim()
    }

    val metaBlock = metaLines.joinToString("\n")
    return when {
        cleanSummary.isNotBlank() && metaBlock.isNotBlank() -> "$cleanSummary\n\n$metaBlock"
        metaBlock.isNotBlank() -> metaBlock
        else -> cleanSummary
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
    val developers: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
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
) {
    val developerString: String
        get() = developers.joinToString(", ")

    val publisherString: String
        get() = publishers.joinToString(", ")

    fun buildDescriptionWithMetadata(): String {
        return buildGameDescriptionWithMetadata(summary, developers, publishers)
    }
}

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
