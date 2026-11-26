package com.lx.tracklist.data.model

enum class MediaType(
    val displayName: String,
    val singularName: String,
    val isWatchType: Boolean,
    val isReadType: Boolean,
    val isPlayType: Boolean = false,
    val isListenType: Boolean = false
) {
    ANIME("Anime", "Anime", true, false),
    MANGA("Manga", "Manga", false, true),
    MANHWA("Manhwa", "Manhwa", false, true),
    MOVIE("Movies", "Movie", true, false),
    SHOW("TV Shows", "TV Show", true, false),
    BOOK("Books", "Book", false, true),
    GAME("Games", "Game", false, false, true),
    MUSIC("Music", "Track", false, false, false, true);

    fun getCreatorRoleLabel(): String = when (this) {
        MUSIC -> "Artist"
        ANIME -> "Studio"
        MANGA, MANHWA, BOOK -> "Author"
        GAME -> "Developer"
        MOVIE -> "Director"
        SHOW -> "Creator / Network"
    }

    /** Present-tense base verb for consuming this media type (Watch / Read / Play / Listen). */
    fun consumeVerb(): String = when (this) {
        ANIME, MOVIE, SHOW -> "Watch"
        MANGA, MANHWA, BOOK -> "Read"
        GAME -> "Play"
        MUSIC -> "Listen"
    }

    /** Past-tense verb for consuming this media type (Watched / Read / Played / Listened). */
    fun consumeVerbPast(): String = when (this) {
        ANIME, MOVIE, SHOW -> "Watched"
        MANGA, MANHWA, BOOK -> "Read"
        GAME -> "Played"
        MUSIC -> "Listened"
    }

    /** Progressive (-ing) verb for active consumption (Watching / Reading / Playing / Listening). */
    fun consumeVerbIng(): String = when (this) {
        ANIME, MOVIE, SHOW -> "Watching"
        MANGA, MANHWA, BOOK -> "Reading"
        GAME -> "Playing"
        MUSIC -> "Listening"
    }

    /** Base repeat verb (Rewatch / Reread / Replay / Relisten). */
    fun repeatVerb(): String = when (this) {
        ANIME, MOVIE, SHOW -> "Rewatch"
        MANGA, MANHWA, BOOK -> "Reread"
        GAME -> "Replay"
        MUSIC -> "Relisten"
    }

    /** Progressive repeat label (Rewatching / Rereading / Replaying / Relistening). */
    fun repeatingLabel(): String = when (this) {
        ANIME, MOVIE, SHOW -> "Rewatching"
        MANGA, MANHWA, BOOK -> "Rereading"
        GAME -> "Replaying"
        MUSIC -> "Relistening"
    }

    /** Past-tense repeat label (Rewatched / Reread / Replayed / Relistened). */
    fun repeatedLabel(): String = when (this) {
        ANIME, MOVIE, SHOW -> "Rewatched"
        MANGA, MANHWA, BOOK -> "Reread"
        GAME -> "Replayed"
        MUSIC -> "Relistened"
    }

    /** Repeat-count label (Times Rewatched / Times Reread / Times Replayed / Times Relistened). */
    fun timesRepeatedLabel(): String = when (this) {
        ANIME, MOVIE, SHOW -> "Times Rewatched"
        MANGA, MANHWA, BOOK -> "Times Reread"
        GAME -> "Times Replayed"
        MUSIC -> "Times Relistened"
    }

    /** Personal dates section label (My Watching Dates / My Reading Dates / ...). */
    fun personalDatesLabel(): String = when (this) {
        ANIME, MOVIE, SHOW -> "My Watching Dates"
        MANGA, MANHWA, BOOK -> "My Reading Dates"
        GAME -> "My Playing Dates"
        MUSIC -> "My Listening Dates"
    }

    /** Short uppercase badge for unreleased items (NYA / NYP / UNRELEASED). */
    fun unreleasedBadge(): String = when (this) {
        ANIME, SHOW, MOVIE -> "NYA"
        MANGA, MANHWA, BOOK -> "NYP"
        GAME, MUSIC -> "UNRELEASED"
    }

    /** Short uppercase badge for ongoing items (AIRING / PUBLISHING / null). */
    fun ongoingBadge(): String? = when (this) {
        ANIME, SHOW -> "AIRING"
        MANGA, MANHWA, BOOK -> "PUBLISHING"
        MOVIE, GAME, MUSIC -> null
    }

    /** Title-Case label for unreleased items in running text and filters. */
    fun unreleasedLabel(): String = when (this) {
        ANIME, SHOW, MOVIE -> "Not Yet Aired"
        MANGA, MANHWA, BOOK -> "Not Yet Published"
        GAME, MUSIC -> "Unreleased"
    }

    /** Title-Case label for ongoing items in running text and filters. */
    fun ongoingLabel(): String? = when (this) {
        ANIME, SHOW -> "Airing"
        MANGA, MANHWA, BOOK -> "Publishing"
        MOVIE, GAME, MUSIC -> null
    }

    companion object {
        fun fromString(value: String): MediaType {
            val trimmed = value.trim()
            val upper = trimmed.uppercase()
            if (upper == "GAME" || upper == "GAMES" || upper == "VIDEO GAME" || upper == "VIDEO GAMES") {
                return GAME
            }
            if (upper == "MUSIC" || upper == "SONG" || upper == "SONGS" || upper == "TRACK" || upper == "TRACKS") {
                return MUSIC
            }
            if (upper == "SHOW" || upper == "SHOWS" || upper == "TV" || upper == "TV SHOW" || upper == "TV SHOWS" || upper == "SERIES") {
                return SHOW
            }
            return entries.firstOrNull {
                it.name.equals(trimmed, ignoreCase = true) ||
                    it.displayName.equals(trimmed, ignoreCase = true) ||
                    it.singularName.equals(trimmed, ignoreCase = true)
            } ?: ANIME
        }
    }
}
