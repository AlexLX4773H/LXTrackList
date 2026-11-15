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

sealed class DiscoveryFilter {
    abstract val label: String
    abstract val includeAdult: Boolean

    data class Anime(
        val season: AnimeSeason,
        val year: Int,
        val sortOrder: AnimeSortOrder,
        override val includeAdult: Boolean = false
    ) : DiscoveryFilter() {
        override val label: String
            get() {
                val adultTag = if (includeAdult) " • 18+" else ""
                return "${season.emoji} ${season.displayName} $year • ${sortOrder.displayName}$adultTag"
            }
    }

    data class Movie(
        val year: Int,
        val sortOrder: TmdbMovieSort,
        override val includeAdult: Boolean = false
    ) : DiscoveryFilter() {
        override val label: String
            get() {
                val adultTag = if (includeAdult) " • 18+" else ""
                return "🎬 Movies $year • ${sortOrder.displayName}$adultTag"
            }
    }

    data class Tv(
        val year: Int,
        val sortOrder: TmdbTvSort,
        override val includeAdult: Boolean = false
    ) : DiscoveryFilter() {
        override val label: String
            get() {
                val adultTag = if (includeAdult) " • 18+" else ""
                return "📺 TV Shows $year • ${sortOrder.displayName}$adultTag"
            }
    }
}
