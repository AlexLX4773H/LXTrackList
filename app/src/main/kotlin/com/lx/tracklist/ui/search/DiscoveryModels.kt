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

enum class ContentDiscoveryMode(val displayName: String, val iconEmoji: String) {
    BY_YEAR("By Year", "🗓️"),
    MOST_POPULAR("Most Popular", "🔥")
}

typealias TmdbDiscoveryMode = ContentDiscoveryMode

enum class MangaSortOrder(val apiValue: String, val displayName: String) {
    POPULARITY("POPULARITY_DESC", "Most Popular"),
    SCORE("SCORE_DESC", "Highest Rated"),
    START_DATE("START_DATE_DESC", "Release Date"),
    TITLE("TITLE_ROMAJI", "Title (A–Z)")
}

enum class BookSortOrder(val apiValue: String, val displayName: String) {
    POPULARITY("editions", "Most Popular"),
    RATING("rating", "Highest Rated"),
    NEWEST("new", "Release Date"),
    TITLE("title", "Title (A–Z)")
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
    abstract val includedTags: Set<String>
    abstract val excludedTags: Set<String>
    abstract val originalLanguage: String?
    val includeAdult: Boolean get() = adultMode != AdultMode.OFF

    data class Anime(
        val mode: AnimeDiscoveryMode = AnimeDiscoveryMode.SEASONAL,
        val season: AnimeSeason = AnimeSeason.current(),
        val year: Int = AnimeSeason.currentYear(),
        val sortOrder: AnimeSortOrder = AnimeSortOrder.POPULARITY,
        override val includedTags: Set<String> = emptySet(),
        override val excludedTags: Set<String> = emptySet(),
        override val adultMode: AdultMode = AdultMode.OFF,
        override val originalLanguage: String? = null
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
            adultMode = adultMode,
            originalLanguage = null
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
            adultMode = if (includeAdult) AdultMode.INCLUDE else AdultMode.OFF,
            originalLanguage = null
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
                val langTag = originalLanguage?.let { " • ${OriginalLanguageRegistry.getDisplayWithFlag(it) ?: it}" } ?: ""
                return "$base$sortText$tagSummary$langTag$adultTag"
            }
    }

    data class Movie(
        val mode: ContentDiscoveryMode = ContentDiscoveryMode.BY_YEAR,
        val year: Int? = AnimeSeason.currentYear(),
        val startYear: Int? = null,
        val endYear: Int? = year,
        val sortOrder: TmdbMovieSort = TmdbMovieSort.POPULARITY,
        override val includedTags: Set<String> = emptySet(),
        override val excludedTags: Set<String> = emptySet(),
        override val adultMode: AdultMode = AdultMode.OFF,
        override val originalLanguage: String? = null
    ) : DiscoveryFilter() {
        constructor(
            year: Int,
            sortOrder: TmdbMovieSort,
            adultMode: AdultMode = AdultMode.OFF
        ) : this(
            mode = ContentDiscoveryMode.BY_YEAR,
            year = year,
            startYear = null,
            endYear = year,
            sortOrder = sortOrder,
            includedTags = emptySet(),
            excludedTags = emptySet(),
            adultMode = adultMode,
            originalLanguage = null
        )

        constructor(
            year: Int,
            sortOrder: TmdbMovieSort,
            includeAdult: Boolean
        ) : this(
            mode = ContentDiscoveryMode.BY_YEAR,
            year = year,
            startYear = null,
            endYear = year,
            sortOrder = sortOrder,
            includedTags = emptySet(),
            excludedTags = emptySet(),
            adultMode = if (includeAdult) AdultMode.INCLUDE else AdultMode.OFF,
            originalLanguage = null
        )

        val yearText: String
            get() = when {
                startYear != null && endYear != null && startYear != endYear -> "$startYear–$endYear"
                endYear != null -> "$endYear"
                year != null -> "$year"
                startYear != null -> "$startYear"
                else -> ""
            }

        override val label: String
            get() {
                val adultTag = when (adultMode) {
                    AdultMode.OFF -> ""
                    AdultMode.INCLUDE -> " • 18+"
                    AdultMode.ONLY -> " • 18+ Only"
                }
                val base = when (mode) {
                    ContentDiscoveryMode.BY_YEAR -> "🎬 Movies $yearText".trim()
                    ContentDiscoveryMode.MOST_POPULAR -> "🔥 Most Popular Movies"
                }
                val sortText = " • ${sortOrder.displayName}"
                val tagSummary = when {
                    includedTags.isNotEmpty() && excludedTags.isNotEmpty() -> " • +${includedTags.size} / -${excludedTags.size} tags"
                    includedTags.isNotEmpty() -> " • +${includedTags.size} tags"
                    excludedTags.isNotEmpty() -> " • -${excludedTags.size} tags"
                    else -> ""
                }
                val langTag = originalLanguage?.let { " • ${OriginalLanguageRegistry.getDisplayWithFlag(it) ?: it}" } ?: ""
                return "$base$sortText$tagSummary$langTag$adultTag"
            }
    }

    data class Tv(
        val mode: ContentDiscoveryMode = ContentDiscoveryMode.BY_YEAR,
        val year: Int? = AnimeSeason.currentYear(),
        val startYear: Int? = null,
        val endYear: Int? = year,
        val sortOrder: TmdbTvSort = TmdbTvSort.POPULARITY,
        override val includedTags: Set<String> = emptySet(),
        override val excludedTags: Set<String> = emptySet(),
        override val adultMode: AdultMode = AdultMode.OFF,
        override val originalLanguage: String? = null
    ) : DiscoveryFilter() {
        constructor(
            year: Int,
            sortOrder: TmdbTvSort,
            adultMode: AdultMode = AdultMode.OFF
        ) : this(
            mode = ContentDiscoveryMode.BY_YEAR,
            year = year,
            startYear = null,
            endYear = year,
            sortOrder = sortOrder,
            includedTags = emptySet(),
            excludedTags = emptySet(),
            adultMode = adultMode,
            originalLanguage = null
        )

        constructor(
            year: Int,
            sortOrder: TmdbTvSort,
            includeAdult: Boolean
        ) : this(
            mode = ContentDiscoveryMode.BY_YEAR,
            year = year,
            startYear = null,
            endYear = year,
            sortOrder = sortOrder,
            includedTags = emptySet(),
            excludedTags = emptySet(),
            adultMode = if (includeAdult) AdultMode.INCLUDE else AdultMode.OFF,
            originalLanguage = null
        )

        val yearText: String
            get() = when {
                startYear != null && endYear != null && startYear != endYear -> "$startYear–$endYear"
                endYear != null -> "$endYear"
                year != null -> "$year"
                startYear != null -> "$startYear"
                else -> ""
            }

        override val label: String
            get() {
                val adultTag = when (adultMode) {
                    AdultMode.OFF -> ""
                    AdultMode.INCLUDE -> " • 18+"
                    AdultMode.ONLY -> " • 18+ Only"
                }
                val base = when (mode) {
                    ContentDiscoveryMode.BY_YEAR -> "📺 TV Shows $yearText".trim()
                    ContentDiscoveryMode.MOST_POPULAR -> "🔥 Most Popular TV"
                }
                val sortText = " • ${sortOrder.displayName}"
                val tagSummary = when {
                    includedTags.isNotEmpty() && excludedTags.isNotEmpty() -> " • +${includedTags.size} / -${excludedTags.size} tags"
                    includedTags.isNotEmpty() -> " • +${includedTags.size} tags"
                    excludedTags.isNotEmpty() -> " • -${excludedTags.size} tags"
                    else -> ""
                }
                val langTag = originalLanguage?.let { " • ${OriginalLanguageRegistry.getDisplayWithFlag(it) ?: it}" } ?: ""
                return "$base$sortText$tagSummary$langTag$adultTag"
            }
    }

    data class Manga(
        val mode: ContentDiscoveryMode = ContentDiscoveryMode.BY_YEAR,
        val startYear: Int? = AnimeSeason.currentYear() - 2,
        val endYear: Int? = AnimeSeason.currentYear(),
        val sortOrder: MangaSortOrder = MangaSortOrder.POPULARITY,
        override val includedTags: Set<String> = emptySet(),
        override val excludedTags: Set<String> = emptySet(),
        override val adultMode: AdultMode = AdultMode.OFF,
        override val originalLanguage: String? = null
    ) : DiscoveryFilter() {
        val yearText: String
            get() = when {
                startYear == null && endYear == null -> ""
                startYear == null -> "${endYear ?: ""}"
                endYear == null -> "${startYear}"
                startYear == endYear -> "$startYear"
                else -> "$startYear–$endYear"
            }

        override val label: String
            get() {
                val adultTag = when (adultMode) {
                    AdultMode.OFF -> ""
                    AdultMode.INCLUDE -> " • 18+"
                    AdultMode.ONLY -> " • 18+ Only"
                }
                val base = when (mode) {
                    ContentDiscoveryMode.BY_YEAR -> "📖 Manga $yearText".trim()
                    ContentDiscoveryMode.MOST_POPULAR -> "🔥 Most Popular Manga"
                }
                val sortText = " • ${sortOrder.displayName}"
                val tagSummary = when {
                    includedTags.isNotEmpty() && excludedTags.isNotEmpty() -> " • +${includedTags.size} / -${excludedTags.size} tags"
                    includedTags.isNotEmpty() -> " • +${includedTags.size} tags"
                    excludedTags.isNotEmpty() -> " • -${excludedTags.size} tags"
                    else -> ""
                }
                val langTag = originalLanguage?.let { " • ${OriginalLanguageRegistry.getDisplayWithFlag(it) ?: it}" } ?: ""
                return "$base$sortText$tagSummary$langTag$adultTag"
            }
    }

    data class Manhwa(
        val mode: ContentDiscoveryMode = ContentDiscoveryMode.BY_YEAR,
        val startYear: Int? = AnimeSeason.currentYear() - 2,
        val endYear: Int? = AnimeSeason.currentYear(),
        val sortOrder: MangaSortOrder = MangaSortOrder.POPULARITY,
        override val includedTags: Set<String> = emptySet(),
        override val excludedTags: Set<String> = emptySet(),
        override val adultMode: AdultMode = AdultMode.OFF,
        override val originalLanguage: String? = null
    ) : DiscoveryFilter() {
        val yearText: String
            get() = when {
                startYear == null && endYear == null -> ""
                startYear == null -> "${endYear ?: ""}"
                endYear == null -> "${startYear}"
                startYear == endYear -> "$startYear"
                else -> "$startYear–$endYear"
            }

        override val label: String
            get() {
                val adultTag = when (adultMode) {
                    AdultMode.OFF -> ""
                    AdultMode.INCLUDE -> " • 18+"
                    AdultMode.ONLY -> " • 18+ Only"
                }
                val base = when (mode) {
                    ContentDiscoveryMode.BY_YEAR -> "🇰🇷 Manhwa $yearText".trim()
                    ContentDiscoveryMode.MOST_POPULAR -> "🔥 Most Popular Manhwa"
                }
                val sortText = " • ${sortOrder.displayName}"
                val tagSummary = when {
                    includedTags.isNotEmpty() && excludedTags.isNotEmpty() -> " • +${includedTags.size} / -${excludedTags.size} tags"
                    includedTags.isNotEmpty() -> " • +${includedTags.size} tags"
                    excludedTags.isNotEmpty() -> " • -${excludedTags.size} tags"
                    else -> ""
                }
                val langTag = originalLanguage?.let { " • ${OriginalLanguageRegistry.getDisplayWithFlag(it) ?: it}" } ?: ""
                return "$base$sortText$tagSummary$langTag$adultTag"
            }
    }

    data class LightNovel(
        val mode: ContentDiscoveryMode = ContentDiscoveryMode.BY_YEAR,
        val startYear: Int? = AnimeSeason.currentYear() - 2,
        val endYear: Int? = AnimeSeason.currentYear(),
        val sortOrder: MangaSortOrder = MangaSortOrder.POPULARITY,
        override val includedTags: Set<String> = emptySet(),
        override val excludedTags: Set<String> = emptySet(),
        override val adultMode: AdultMode = AdultMode.OFF,
        override val originalLanguage: String? = null
    ) : DiscoveryFilter() {
        val yearText: String
            get() = when {
                startYear == null && endYear == null -> ""
                startYear == null -> "${endYear ?: ""}"
                endYear == null -> "${startYear}"
                startYear == endYear -> "$startYear"
                else -> "$startYear–$endYear"
            }

        override val label: String
            get() {
                val adultTag = when (adultMode) {
                    AdultMode.OFF -> ""
                    AdultMode.INCLUDE -> " • 18+"
                    AdultMode.ONLY -> " • 18+ Only"
                }
                val base = when (mode) {
                    ContentDiscoveryMode.BY_YEAR -> "📜 Light Novels $yearText".trim()
                    ContentDiscoveryMode.MOST_POPULAR -> "🔥 Most Popular Light Novels"
                }
                val sortText = " • ${sortOrder.displayName}"
                val tagSummary = when {
                    includedTags.isNotEmpty() && excludedTags.isNotEmpty() -> " • +${includedTags.size} / -${excludedTags.size} tags"
                    includedTags.isNotEmpty() -> " • +${includedTags.size} tags"
                    excludedTags.isNotEmpty() -> " • -${excludedTags.size} tags"
                    else -> ""
                }
                val langTag = originalLanguage?.let { " • ${OriginalLanguageRegistry.getDisplayWithFlag(it) ?: it}" } ?: ""
                return "$base$sortText$tagSummary$langTag$adultTag"
            }
    }

    data class Book(
        val mode: ContentDiscoveryMode = ContentDiscoveryMode.BY_YEAR,
        val startYear: Int? = AnimeSeason.currentYear() - 5,
        val endYear: Int? = AnimeSeason.currentYear(),
        val sortOrder: BookSortOrder = BookSortOrder.POPULARITY,
        override val includedTags: Set<String> = emptySet(),
        override val excludedTags: Set<String> = emptySet(),
        override val adultMode: AdultMode = AdultMode.OFF,
        override val originalLanguage: String? = null
    ) : DiscoveryFilter() {
        val yearText: String
            get() = when {
                startYear == null && endYear == null -> ""
                startYear == null -> "${endYear ?: ""}"
                endYear == null -> "${startYear}"
                startYear == endYear -> "$startYear"
                else -> "$startYear–$endYear"
            }

        override val label: String
            get() {
                val adultTag = when (adultMode) {
                    AdultMode.OFF -> ""
                    AdultMode.INCLUDE -> " • 18+"
                    AdultMode.ONLY -> " • 18+ Only"
                }
                val base = when (mode) {
                    ContentDiscoveryMode.BY_YEAR -> "📚 Books $yearText".trim()
                    ContentDiscoveryMode.MOST_POPULAR -> "🔥 Most Popular Books"
                }
                val sortText = " • ${sortOrder.displayName}"
                val tagSummary = when {
                    includedTags.isNotEmpty() && excludedTags.isNotEmpty() -> " • +${includedTags.size} / -${excludedTags.size} tags"
                    includedTags.isNotEmpty() -> " • +${includedTags.size} tags"
                    excludedTags.isNotEmpty() -> " • -${excludedTags.size} tags"
                    else -> ""
                }
                val langTag = originalLanguage?.let { " • ${OriginalLanguageRegistry.getDisplayWithFlag(it) ?: it}" } ?: ""
                return "$base$sortText$tagSummary$langTag$adultTag"
            }
    }
}
