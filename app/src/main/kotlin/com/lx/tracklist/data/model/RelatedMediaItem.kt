package com.lx.tracklist.data.model

data class RelatedMediaItem(
    val title: String,
    val relationship: String, // "Sequel", "Prequel", "Part of Series", "Side Story", "Alternative Version", "Related"
    val externalId: String,
    val externalSource: String, // "MAL" or "TMDB"
    val mediaType: MediaType,
    val posterUrl: String? = null,
    val releaseYear: String? = null,
    val score: Float? = null,
    val inLibraryEntryId: Long? = null,
    val inLibraryStatus: MediaStatus? = null
)

