package com.lx.tracklist.data.model

import java.util.Locale

/**
 * Canonical external provider names and normalization helpers.
 * All external source writes and comparisons should pass through [canonical].
 */
object ExternalSources {
    const val MAL = "MAL"
    const val ANILIST = "AniList"
    const val KITSU = "Kitsu"
    const val MANGADEX = "MangaDex"
    const val TMDB = "TMDB"
    const val GOOGLE_BOOKS = "Google Books"
    const val OPEN_LIBRARY = "Open Library"
    const val IGDB = "IGDB"
    const val RAWG = "RAWG"
    const val APPLE_MUSIC = "Apple Music"
    const val MUSICBRAINZ = "MusicBrainz"
    const val TACHIYOMI = "Tachiyomi"

    /**
     * Normalizes [raw] provider strings to canonical names without an entry external ID context.
     * Blank strings and `"CSV"` map to `null`.
     */
    fun canonical(raw: String?): String? = canonical(raw, externalId = null)

    /**
     * Normalizes [raw] provider strings to canonical names.
     * Returns `null` for blank values or `"CSV"` when [externalId] is null/blank,
     * and the trimmed original string for unrecognized non-blank sources.
     */
    fun canonical(raw: String?, externalId: String?): String? {
        val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return when (trimmed.uppercase(Locale.US)) {
            "MAL", "MYANIMELIST", "JIKAN", "MAL (ANILIST)" -> MAL
            "ANILIST" -> ANILIST
            "KITSU" -> KITSU
            "MANGADEX" -> MANGADEX
            "TMDB", "THEMOVIEDB", "THE MOVIE DATABASE" -> TMDB
            "GOOGLE BOOKS", "GOOGLEBOOKS", "GOOGLE_BOOKS" -> GOOGLE_BOOKS
            "OPEN LIBRARY", "OPENLIBRARY", "OPEN_LIBRARY" -> OPEN_LIBRARY
            "IGDB" -> IGDB
            "RAWG" -> RAWG
            "ITUNES", "APPLE MUSIC", "APPLE_MUSIC" -> APPLE_MUSIC
            "MUSICBRAINZ" -> MUSICBRAINZ
            "TACHIYOMI", "MIHON" -> TACHIYOMI
            "CSV" -> if (externalId.isNullOrBlank()) null else trimmed
            else -> trimmed
        }
    }
}
