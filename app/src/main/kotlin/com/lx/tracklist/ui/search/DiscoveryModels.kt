package com.lx.tracklist.ui.search

import java.util.Calendar

enum class AnimeSeason(
    val apiValue: String,
    val displayName: String,
    val emoji: String,
    val monthsText: String
) {
    WINTER("WINTER", "Winter", "❄️", "Jan – Mar"),
    SPRING("SPRING", "Spring", "🌸", "Apr – Jun"),
    SUMMER("SUMMER", "Summer", "☀️", "Jul – Sep"),
    FALL("FALL", "Fall", "🍂", "Oct – Dec");

    companion object {
        fun current(): AnimeSeason {
            val month = Calendar.getInstance().get(Calendar.MONTH) // 0-based: 0=Jan, 11=Dec
            return when (month) {
                0, 1, 2 -> WINTER
                3, 4, 5 -> SPRING
                6, 7, 8 -> SUMMER
                else -> FALL
            }
        }

        fun currentYear(): Int {
            return Calendar.getInstance().get(Calendar.YEAR)
        }

        fun maxDiscoveryYear(): Int {
            return currentYear() + 2
        }

        fun nextSeason(current: AnimeSeason = current()): Pair<AnimeSeason, Int> {
            val year = currentYear()
            return when (current) {
                WINTER -> Pair(SPRING, year)
                SPRING -> Pair(SUMMER, year)
                SUMMER -> Pair(FALL, year)
                FALL -> Pair(WINTER, year + 1)
            }
        }
    }
}

enum class AnimeDiscoveryMode(val displayName: String) {
    SEASONAL("Seasonal"),
    MOST_POPULAR("Most Popular");

    val iconEmoji: String
        get() = when (this) {
            SEASONAL -> AnimeSeason.current().emoji
            MOST_POPULAR -> "🔥"
        }
}

enum class AnimeSortOrder(val apiValue: String, val displayName: String) {
    POPULARITY("POPULARITY_DESC", "Most Popular"),
    SCORE("SCORE_DESC", "Highest Rated"),
    AIR_DATE("START_DATE_DESC", "Release Date"),
    TITLE("TITLE_ROMAJI", "Title (A–Z)")
}

enum class TmdbMovieSort(val apiValue: String, val displayName: String) {
    POPULARITY("popularity.desc", "Most Popular"),
    RATING("vote_average.desc", "Highest Rated"),
    RELEASE_DATE("primary_release_date.desc", "Release Date"),
    REVENUE("revenue.desc", "Box Office")
}

enum class TmdbTvSort(val apiValue: String, val displayName: String) {
    POPULARITY("popularity.desc", "Most Popular"),
    RATING("vote_average.desc", "Highest Rated"),
    RELEASE_DATE("first_air_date.desc", "Premiere Date")
}

enum class AdultMode(val displayName: String) {
    OFF("Off"),
    INCLUDE("18+"),
    ONLY("18+ Only");

    fun next(): AdultMode = when (this) {
        OFF -> INCLUDE
        INCLUDE -> ONLY
        ONLY -> OFF
    }
}

sealed class DiscoveryFilter {
    abstract val label: String
    abstract val adultMode: AdultMode
    val includeAdult: Boolean get() = adultMode != AdultMode.OFF

    data class Anime(
        val mode: AnimeDiscoveryMode = AnimeDiscoveryMode.SEASONAL,
        val season: AnimeSeason = AnimeSeason.current(),
        val year: Int = AnimeSeason.currentYear(),
        val sortOrder: AnimeSortOrder = AnimeSortOrder.POPULARITY,
        val includedTags: Set<String> = emptySet(),
        val excludedTags: Set<String> = emptySet(),
        override val adultMode: AdultMode = AdultMode.OFF
    ) : DiscoveryFilter() {
        constructor(
            season: AnimeSeason,
            year: Int,
            sortOrder: AnimeSortOrder,
            adultMode: AdultMode = AdultMode.OFF
        ) : this(
            mode = AnimeDiscoveryMode.SEASONAL,
            season = season,
            year = year,
            sortOrder = sortOrder,
            includedTags = emptySet(),
            excludedTags = emptySet(),
            adultMode = adultMode
        )

        constructor(
            season: AnimeSeason,
            year: Int,
            sortOrder: AnimeSortOrder,
            includeAdult: Boolean
        ) : this(
            mode = AnimeDiscoveryMode.SEASONAL,
            season = season,
            year = year,
            sortOrder = sortOrder,
            includedTags = emptySet(),
            excludedTags = emptySet(),
            adultMode = if (includeAdult) AdultMode.INCLUDE else AdultMode.OFF
        )

        override val label: String
            get() {
                val adultTag = when (adultMode) {
                    AdultMode.OFF -> ""
                    AdultMode.INCLUDE -> " • 18+"
                    AdultMode.ONLY -> " • 18+ Only"
                }
                val base = when (mode) {
                    AnimeDiscoveryMode.SEASONAL -> "${season.emoji} ${season.displayName} $year"
                    AnimeDiscoveryMode.MOST_POPULAR -> "🔥 Most Popular"
                }
                val sortText = " • ${sortOrder.displayName}"
                val tagSummary = when {
                    includedTags.isNotEmpty() && excludedTags.isNotEmpty() -> " • +${includedTags.size} / -${excludedTags.size} tags"
                    includedTags.isNotEmpty() -> " • +${includedTags.size} tags"
                    excludedTags.isNotEmpty() -> " • -${excludedTags.size} tags"
                    else -> ""
                }
                return "$base$sortText$tagSummary$adultTag"
            }
    }

    data class Movie(
        val year: Int,
        val sortOrder: TmdbMovieSort,
        override val adultMode: AdultMode = AdultMode.OFF
    ) : DiscoveryFilter() {
        constructor(
            year: Int,
            sortOrder: TmdbMovieSort,
            includeAdult: Boolean
        ) : this(
            year = year,
            sortOrder = sortOrder,
            adultMode = if (includeAdult) AdultMode.INCLUDE else AdultMode.OFF
        )

        override val label: String
            get() {
                val adultTag = when (adultMode) {
                    AdultMode.OFF -> ""
                    AdultMode.INCLUDE -> " • 18+"
                    AdultMode.ONLY -> " • 18+ Only"
                }
                return "🎬 Movies $year • ${sortOrder.displayName}$adultTag"
            }
    }

    data class Tv(
        val year: Int,
        val sortOrder: TmdbTvSort,
        override val adultMode: AdultMode = AdultMode.OFF
    ) : DiscoveryFilter() {
        constructor(
            year: Int,
            sortOrder: TmdbTvSort,
            includeAdult: Boolean
        ) : this(
            year = year,
            sortOrder = sortOrder,
            adultMode = if (includeAdult) AdultMode.INCLUDE else AdultMode.OFF
        )

        override val label: String
            get() {
                val adultTag = when (adultMode) {
                    AdultMode.OFF -> ""
                    AdultMode.INCLUDE -> " • 18+"
                    AdultMode.ONLY -> " • 18+ Only"
                }
                return "📺 TV Shows $year • ${sortOrder.displayName}$adultTag"
            }
    }
}
