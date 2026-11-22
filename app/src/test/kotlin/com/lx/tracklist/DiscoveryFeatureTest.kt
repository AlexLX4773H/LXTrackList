package com.lx.tracklist

import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.remote.IgdbApiClient
import com.lx.tracklist.data.remote.RawgApiClient
import com.lx.tracklist.data.remote.TmdbApiClient
import com.google.gson.JsonObject
import com.lx.tracklist.util.DurationFormatter
import com.lx.tracklist.ui.search.AdultMode
import com.lx.tracklist.ui.search.AnimeDiscoveryMode
import com.lx.tracklist.ui.search.AnimeSeason
import com.lx.tracklist.ui.search.AnimeSortOrder
import com.lx.tracklist.ui.search.AnimeTagCategory
import com.lx.tracklist.ui.search.AnimeTagRegistry
import com.lx.tracklist.ui.search.BookSortOrder
import com.lx.tracklist.ui.search.BookTagRegistry
import com.lx.tracklist.ui.search.ContentDiscoveryMode
import com.lx.tracklist.ui.search.DiscoveryFilter
import com.lx.tracklist.ui.search.GamePlatformOption
import com.lx.tracklist.ui.search.GamePlatformRegistry
import com.lx.tracklist.ui.search.GameSortOrder
import com.lx.tracklist.ui.search.MangaSortOrder
import com.lx.tracklist.ui.search.OriginalLanguageRegistry
import com.lx.tracklist.ui.search.SearchCategory
import com.lx.tracklist.ui.search.TmdbDiscoveryMode
import com.lx.tracklist.ui.search.TmdbMovieSort
import com.lx.tracklist.ui.search.TmdbTagRegistry
import com.lx.tracklist.ui.search.TmdbTvSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryFeatureTest {

    @Test
    fun testAnimeDiscoveryModesAndTags() {
        assertEquals("Seasonal", AnimeDiscoveryMode.SEASONAL.displayName)
        assertEquals("Most Popular", AnimeDiscoveryMode.MOST_POPULAR.displayName)
        assertEquals(AnimeSeason.current().emoji, AnimeDiscoveryMode.SEASONAL.iconEmoji)
        assertEquals("🔥", AnimeDiscoveryMode.MOST_POPULAR.iconEmoji)

        // Tag registry lookup
        val actionTag = AnimeTagRegistry.findTag("Action")
        assertNotNull(actionTag)
        assertEquals(1, actionTag?.malId)
        assertEquals(AnimeTagCategory.GENRE, actionTag?.category)

        val isekaiTag = AnimeTagRegistry.findTag("Isekai")
        assertNotNull(isekaiTag)
        assertEquals(61, isekaiTag?.malId)
        assertEquals(AnimeTagCategory.THEME, isekaiTag?.category)

        val shounenTag = AnimeTagRegistry.findTag("Shounen")
        assertNotNull(shounenTag)
        assertEquals(27, shounenTag?.malId)
        assertEquals(AnimeTagCategory.DEMOGRAPHIC, shounenTag?.category)

        // Tag filtering by category
        val genres = AnimeTagRegistry.filterTags("", AnimeTagCategory.GENRE)
        assertTrue(genres.any { it.name == "Action" })
        assertTrue(genres.none { it.name == "Isekai" })

        // Tag search query
        val searchResults = AnimeTagRegistry.filterTags("ise")
        assertTrue(searchResults.any { it.name == "Isekai" })

        // Alphabetical A to Z ordering
        val allTagsList = AnimeTagRegistry.filterTags("")
        val tagNames = allTagsList.map { it.name.lowercase() }
        assertEquals(tagNames.sorted(), tagNames)
    }

    @Test
    fun testAnimeDiscoveryFilterLabels() {
        // 1. Most Popular mode
        val popFilter = DiscoveryFilter.Anime(
            mode = AnimeDiscoveryMode.MOST_POPULAR,
            sortOrder = AnimeSortOrder.POPULARITY
        )
        assertEquals("🔥 Most Popular • Most Popular", popFilter.label)

        // 2. Mode with tags (+Action, -Ecchi)
        val tagFilter = DiscoveryFilter.Anime(
            mode = AnimeDiscoveryMode.SEASONAL,
            season = AnimeSeason.FALL,
            year = 2026,
            sortOrder = AnimeSortOrder.POPULARITY,
            includedTags = setOf("Action", "Isekai"),
            excludedTags = setOf("Ecchi")
        )
        assertEquals("🍂 Fall 2026 • Most Popular • +2 / -1 tags", tagFilter.label)
    }

    @Test
    fun testAnimeSeasonLogic() {
        assertNotNull(AnimeSeason.current())
        assertTrue(AnimeSeason.currentYear() >= 2024)
        assertEquals(AnimeSeason.currentYear() + 2, AnimeSeason.maxDiscoveryYear())

        val (nextSeason, nextYear) = AnimeSeason.nextSeason(AnimeSeason.FALL)
        assertEquals(AnimeSeason.WINTER, nextSeason)
        assertEquals(AnimeSeason.currentYear() + 1, nextYear)

        val (springSeason, springYear) = AnimeSeason.nextSeason(AnimeSeason.WINTER)
        assertEquals(AnimeSeason.SPRING, springSeason)
        assertEquals(AnimeSeason.currentYear(), springYear)
    }

    @Test
    fun testAdultModeCycle() {
        assertEquals(AdultMode.INCLUDE, AdultMode.OFF.next())
        assertEquals(AdultMode.ONLY, AdultMode.INCLUDE.next())
        assertEquals(AdultMode.OFF, AdultMode.ONLY.next())
    }

    @Test
    fun testAnimeFilterLabel() {
        val filterOff = DiscoveryFilter.Anime(
            season = AnimeSeason.FALL,
            year = 2024,
            sortOrder = AnimeSortOrder.POPULARITY,
            adultMode = AdultMode.OFF
        )
        assertEquals("🍂 Fall 2024 • Most Popular", filterOff.label)
        assertFalse(filterOff.includeAdult)

        val filterInclude = DiscoveryFilter.Anime(
            season = AnimeSeason.SPRING,
            year = 2025,
            sortOrder = AnimeSortOrder.SCORE,
            adultMode = AdultMode.INCLUDE
        )
        assertEquals("🌸 Spring 2025 • Highest Rated • 18+", filterInclude.label)
        assertTrue(filterInclude.includeAdult)

        val filterOnly = DiscoveryFilter.Anime(
            season = AnimeSeason.WINTER,
            year = 2026,
            sortOrder = AnimeSortOrder.POPULARITY,
            adultMode = AdultMode.ONLY
        )
        assertEquals("❄️ Winter 2026 • Most Popular • 18+ Only", filterOnly.label)
        assertTrue(filterOnly.includeAdult)
    }

    @Test
    fun testMovieFilterLabel() {
        val filter = DiscoveryFilter.Movie(
            year = 2027,
            sortOrder = TmdbMovieSort.POPULARITY,
            adultMode = AdultMode.ONLY
        )
        assertEquals("🎬 Movies 2027 • Most Popular • 18+ Only", filter.label)
        assertTrue(filter.includeAdult)
    }

    @Test
    fun testTvFilterLabel() {
        val filter = DiscoveryFilter.Tv(
            year = 2025,
            sortOrder = TmdbTvSort.RATING,
            adultMode = AdultMode.INCLUDE
        )
        assertEquals("📺 TV Shows 2025 • Highest Rated • 18+", filter.label)
        assertTrue(filter.includeAdult)
    }

    @Test
    fun testSortOrderApiValues() {
        assertEquals("POPULARITY_DESC", AnimeSortOrder.POPULARITY.apiValue)
        assertEquals("SCORE_DESC", AnimeSortOrder.SCORE.apiValue)
        assertEquals("START_DATE_DESC", AnimeSortOrder.AIR_DATE.apiValue)
        assertEquals("TITLE_ROMAJI", AnimeSortOrder.TITLE.apiValue)

        assertEquals("popularity.desc", TmdbMovieSort.POPULARITY.apiValue)
        assertEquals("vote_average.desc", TmdbMovieSort.RATING.apiValue)
        assertEquals("primary_release_date.desc", TmdbMovieSort.RELEASE_DATE.apiValue)
        assertEquals("revenue.desc", TmdbMovieSort.REVENUE.apiValue)

        assertEquals("popularity.desc", TmdbTvSort.POPULARITY.apiValue)
        assertEquals("vote_average.desc", TmdbTvSort.RATING.apiValue)
        assertEquals("first_air_date.desc", TmdbTvSort.RELEASE_DATE.apiValue)
    }

    @Test
    fun testTmdbDiscoveryModesAndRegistry() {
        assertEquals("By Year", TmdbDiscoveryMode.BY_YEAR.displayName)
        assertEquals("Most Popular", TmdbDiscoveryMode.MOST_POPULAR.displayName)
        assertEquals("🗓️", TmdbDiscoveryMode.BY_YEAR.iconEmoji)
        assertEquals("🔥", TmdbDiscoveryMode.MOST_POPULAR.iconEmoji)

        // Movie genres
        assertEquals(28, TmdbTagRegistry.getMovieGenreId("Action"))
        assertEquals(35, TmdbTagRegistry.getMovieGenreId("Comedy"))
        assertEquals(878, TmdbTagRegistry.getMovieGenreId("Science Fiction"))
        assertEquals(27, TmdbTagRegistry.getMovieGenreId("Horror"))

        // TV genres
        assertEquals(10759, TmdbTagRegistry.getTvGenreId("Action & Adventure"))
        assertEquals(16, TmdbTagRegistry.getTvGenreId("Animation"))
        assertEquals(10765, TmdbTagRegistry.getTvGenreId("Sci-Fi & Fantasy"))

        // Alphabetical A to Z ordering
        val movieGenreNames = TmdbTagRegistry.movieGenres.map { it.name.lowercase() }
        assertEquals(movieGenreNames.sorted(), movieGenreNames)

        val tvGenreNames = TmdbTagRegistry.tvGenres.map { it.name.lowercase() }
        assertEquals(tvGenreNames.sorted(), tvGenreNames)

        // Filter search
        val filtered = TmdbTagRegistry.filterMovieGenres("sci")
        assertTrue(filtered.any { it.name == "Science Fiction" })
    }

    @Test
    fun testTmdbDiscoveryFilterLabels() {
        // Most Popular Movie
        val popMovie = DiscoveryFilter.Movie(
            mode = TmdbDiscoveryMode.MOST_POPULAR,
            sortOrder = TmdbMovieSort.POPULARITY
        )
        assertEquals("🔥 Most Popular Movies • Most Popular", popMovie.label)

        // By Year Movie with tags (+Action, -Horror)
        val tagMovie = DiscoveryFilter.Movie(
            mode = TmdbDiscoveryMode.BY_YEAR,
            year = 2026,
            sortOrder = TmdbMovieSort.POPULARITY,
            includedTags = setOf("Action"),
            excludedTags = setOf("Horror")
        )
        assertEquals("🎬 Movies 2026 • Most Popular • +1 / -1 tags", tagMovie.label)

        // Most Popular TV with tags (+Drama)
        val popTv = DiscoveryFilter.Tv(
            mode = TmdbDiscoveryMode.MOST_POPULAR,
            sortOrder = TmdbTvSort.POPULARITY,
            includedTags = setOf("Drama")
        )
        assertEquals("🔥 Most Popular TV • Most Popular • +1 tags", popTv.label)
    }

    @Test
    fun testBookTagRegistry() {
        val allGenres = BookTagRegistry.bookGenres
        assertTrue(allGenres.isNotEmpty())

        // Strictly sorted A to Z
        val names = allGenres.map { it.name.lowercase() }
        assertEquals(names.sorted(), names)

        // Find by name
        val fantasy = BookTagRegistry.findGenre("Fantasy")
        assertNotNull(fantasy)
        assertEquals("fantasy", fantasy?.subjectQuery)

        val scifi = BookTagRegistry.findGenre("Science Fiction")
        assertNotNull(scifi)
        assertEquals("science_fiction", scifi?.subjectQuery)

        // Filter search
        val filtered = BookTagRegistry.filterBookGenres("mys")
        assertTrue(filtered.any { it.name == "Crime & Mystery" })
    }

    @Test
    fun testMangaAndBookSortOrders() {
        assertEquals("POPULARITY_DESC", MangaSortOrder.POPULARITY.apiValue)
        assertEquals("SCORE_DESC", MangaSortOrder.SCORE.apiValue)
        assertEquals("START_DATE_DESC", MangaSortOrder.START_DATE.apiValue)
        assertEquals("TITLE_ROMAJI", MangaSortOrder.TITLE.apiValue)

        assertEquals("editions", BookSortOrder.POPULARITY.apiValue)
        assertEquals("rating", BookSortOrder.RATING.apiValue)
        assertEquals("new", BookSortOrder.NEWEST.apiValue)
        assertEquals("title", BookSortOrder.TITLE.apiValue)
    }

    @Test
    fun testMangaDiscoveryFilterLabelsAndYearRange() {
        // Year range
        val rangeManga = DiscoveryFilter.Manga(
            mode = ContentDiscoveryMode.BY_YEAR,
            startYear = 2020,
            endYear = 2024,
            sortOrder = MangaSortOrder.POPULARITY,
            includedTags = setOf("Action", "Fantasy"),
            excludedTags = setOf("Romance")
        )
        assertEquals("2020–2024", rangeManga.yearText)
        assertEquals("📖 Manga 2020–2024 • Most Popular • +2 / -1 tags", rangeManga.label)

        // Single year
        val singleYearManga = DiscoveryFilter.Manga(
            mode = ContentDiscoveryMode.BY_YEAR,
            startYear = 2025,
            endYear = 2025,
            sortOrder = MangaSortOrder.SCORE,
            adultMode = AdultMode.INCLUDE
        )
        assertEquals("2025", singleYearManga.yearText)
        assertEquals("📖 Manga 2025 • Highest Rated • 18+", singleYearManga.label)

        // Most Popular
        val popManga = DiscoveryFilter.Manga(
            mode = ContentDiscoveryMode.MOST_POPULAR,
            sortOrder = MangaSortOrder.POPULARITY
        )
        assertEquals("🔥 Most Popular Manga • Most Popular", popManga.label)
    }

    @Test
    fun testManhwaDiscoveryFilterLabelsAndYearRange() {
        val rangeManhwa = DiscoveryFilter.Manhwa(
            mode = ContentDiscoveryMode.BY_YEAR,
            startYear = 2021,
            endYear = 2023,
            sortOrder = MangaSortOrder.POPULARITY,
            includedTags = setOf("Reincarnation")
        )
        assertEquals("2021–2023", rangeManhwa.yearText)
        assertEquals("🇰🇷 Manhwa 2021–2023 • Most Popular • +1 tags", rangeManhwa.label)

        val popManhwa = DiscoveryFilter.Manhwa(
            mode = ContentDiscoveryMode.MOST_POPULAR,
            sortOrder = MangaSortOrder.SCORE
        )
        assertEquals("🔥 Most Popular Manhwa • Highest Rated", popManhwa.label)
    }

    @Test
    fun testLightNovelDiscoveryFilterLabelsAndYearRange() {
        val rangeLN = DiscoveryFilter.LightNovel(
            mode = ContentDiscoveryMode.BY_YEAR,
            startYear = 2018,
            endYear = 2022,
            sortOrder = MangaSortOrder.POPULARITY,
            includedTags = setOf("Isekai", "Magic")
        )
        assertEquals("2018–2022", rangeLN.yearText)
        assertEquals("📜 Light Novels 2018–2022 • Most Popular • +2 tags", rangeLN.label)

        val popLN = DiscoveryFilter.LightNovel(
            mode = ContentDiscoveryMode.MOST_POPULAR,
            sortOrder = MangaSortOrder.POPULARITY
        )
        assertEquals("🔥 Most Popular Light Novels • Most Popular", popLN.label)
    }

    @Test
    fun testBookDiscoveryFilterLabelsAndYearRange() {
        val rangeBook = DiscoveryFilter.Book(
            mode = ContentDiscoveryMode.BY_YEAR,
            startYear = 2015,
            endYear = 2020,
            sortOrder = BookSortOrder.RATING,
            includedTags = setOf("Science Fiction"),
            excludedTags = setOf("Horror")
        )
        assertEquals("2015–2020", rangeBook.yearText)
        assertEquals("📚 Books 2015–2020 • Highest Rated • +1 / -1 tags", rangeBook.label)

        val popBook = DiscoveryFilter.Book(
            mode = ContentDiscoveryMode.MOST_POPULAR,
            sortOrder = BookSortOrder.POPULARITY
        )
        assertEquals("🔥 Most Popular Books • Most Popular", popBook.label)
    }

    @Test
    fun testMovieAndTvYearRangeLabels() {
        val rangeMovie = DiscoveryFilter.Movie(
            mode = ContentDiscoveryMode.BY_YEAR,
            startYear = 2010,
            endYear = 2019,
            sortOrder = TmdbMovieSort.POPULARITY
        )
        assertEquals("2010–2019", rangeMovie.yearText)
        assertEquals("🎬 Movies 2010–2019 • Most Popular", rangeMovie.label)

        val rangeTv = DiscoveryFilter.Tv(
            mode = ContentDiscoveryMode.BY_YEAR,
            startYear = 2020,
            endYear = 2024,
            sortOrder = TmdbTvSort.RATING
        )
        assertEquals("2020–2024", rangeTv.yearText)
        assertEquals("📺 TV Shows 2020–2024 • Highest Rated", rangeTv.label)
    }

    @Test
    fun testYearRangePresetsAndLabels() {
        val currentYr = AnimeSeason.currentYear()
        val nextYr = currentYr + 1
        val prevYr = currentYr - 1

        // Current Year single year filter
        val currentMovie = DiscoveryFilter.Movie(
            mode = ContentDiscoveryMode.BY_YEAR,
            startYear = currentYr,
            endYear = currentYr,
            sortOrder = TmdbMovieSort.POPULARITY
        )
        assertEquals("$currentYr", currentMovie.yearText)
        assertEquals("🎬 Movies $currentYr • Most Popular", currentMovie.label)

        // Next Year single year filter
        val nextMovie = DiscoveryFilter.Movie(
            mode = ContentDiscoveryMode.BY_YEAR,
            startYear = nextYr,
            endYear = nextYr,
            sortOrder = TmdbMovieSort.POPULARITY
        )
        assertEquals("$nextYr", nextMovie.yearText)
        assertEquals("🎬 Movies $nextYr • Most Popular", nextMovie.label)

        // Previous Year single year filter
        val prevMovie = DiscoveryFilter.Movie(
            mode = ContentDiscoveryMode.BY_YEAR,
            startYear = prevYr,
            endYear = prevYr,
            sortOrder = TmdbMovieSort.POPULARITY
        )
        assertEquals("$prevYr", prevMovie.yearText)
        assertEquals("🎬 Movies $prevYr • Most Popular", prevMovie.label)

        // Custom range
        val customRange = DiscoveryFilter.Movie(
            mode = ContentDiscoveryMode.BY_YEAR,
            startYear = 1995,
            endYear = 2010,
            sortOrder = TmdbMovieSort.POPULARITY
        )
        assertEquals("1995–2010", customRange.yearText)
        assertEquals("🎬 Movies 1995–2010 • Most Popular", customRange.label)
    }

    @Test
    fun testOriginalLanguageRegistryLookupAndMatching() {
        // 1. Direct ISO 639-1
        val thai = OriginalLanguageRegistry.findByCode("th")
        assertNotNull(thai)
        assertEquals("Thai", thai?.displayName)
        assertEquals("🇹🇭", thai?.flagEmoji)
        assertEquals("🇹🇭 Thai", thai?.displayWithFlag)

        val japanese = OriginalLanguageRegistry.findByCode("ja")
        assertNotNull(japanese)
        assertEquals("Japanese", japanese?.displayName)
        assertEquals("🇯🇵", japanese?.flagEmoji)

        // 2. ISO 639-2 (OpenLibrary / ISO-3)
        val jpn = OriginalLanguageRegistry.findByCode("jpn")
        assertNotNull(jpn)
        assertEquals("ja", jpn?.code)

        val tha = OriginalLanguageRegistry.findByCode("tha")
        assertNotNull(tha)
        assertEquals("th", tha?.code)

        // 3. Country Codes (AniList)
        val kr = OriginalLanguageRegistry.findByCode("KR")
        assertNotNull(kr)
        assertEquals("ko", kr?.code)

        val jp = OriginalLanguageRegistry.findByCode("JP")
        assertNotNull(jp)
        assertEquals("ja", jp?.code)

        // 4. Cross-format Matching
        assertTrue(OriginalLanguageRegistry.matches(null, "ja"))
        assertTrue(OriginalLanguageRegistry.matches("", "ja"))
        assertTrue(OriginalLanguageRegistry.matches("th", "tha"))
        assertTrue(OriginalLanguageRegistry.matches("th", "TH"))
        assertTrue(OriginalLanguageRegistry.matches("ja", "jpn"))
        assertTrue(OriginalLanguageRegistry.matches("ja", "JP"))
        assertTrue(OriginalLanguageRegistry.matches("ko", "kor"))
        assertTrue(OriginalLanguageRegistry.matches("ko", "KR"))
        assertFalse(OriginalLanguageRegistry.matches("th", "ja"))
        assertFalse(OriginalLanguageRegistry.matches("th", null))

        // 5. Code conversion helpers
        assertEquals("TH", OriginalLanguageRegistry.toAniListCountryCode("th"))
        assertEquals("JP", OriginalLanguageRegistry.toAniListCountryCode("ja"))
        assertEquals("KR", OriginalLanguageRegistry.toAniListCountryCode("ko"))
        assertEquals("tha", OriginalLanguageRegistry.toIso6392("th"))
        assertEquals("jpn", OriginalLanguageRegistry.toIso6392("ja"))
        assertEquals("kor", OriginalLanguageRegistry.toIso6392("ko"))
        assertEquals("mal", OriginalLanguageRegistry.toIso6392("ml"))
        assertEquals("tam", OriginalLanguageRegistry.toIso6392("ta"))
        assertEquals("tel", OriginalLanguageRegistry.toIso6392("te"))

        // Indian languages lookup (Malayalam, Tamil, Telugu)
        val malayalam = OriginalLanguageRegistry.findByCode("ml")
        assertNotNull(malayalam)
        assertEquals("Malayalam", malayalam?.displayName)
        assertEquals("മലയാളം", malayalam?.nativeName)
        assertEquals("🇮🇳", malayalam?.flagEmoji)

        val tamil = OriginalLanguageRegistry.findByCode("ta")
        assertNotNull(tamil)
        assertEquals("Tamil", tamil?.displayName)
        assertEquals("தமிழ்", tamil?.nativeName)
        assertEquals("🇮🇳", tamil?.flagEmoji)

        val telugu = OriginalLanguageRegistry.findByCode("te")
        assertNotNull(telugu)
        assertEquals("Telugu", telugu?.displayName)
        assertEquals("తెలుగు", telugu?.nativeName)
        assertEquals("🇮🇳", telugu?.flagEmoji)

        // 6. Category top languages
        val animeLangs = OriginalLanguageRegistry.getTopLanguagesForCategory(SearchCategory.ANIME)
        assertTrue(animeLangs.any { it.code == "ja" })
        assertTrue(animeLangs.any { it.code == "ko" })

        val tvLangs = OriginalLanguageRegistry.getTopLanguagesForCategory(SearchCategory.TV_SHOWS)
        assertTrue(tvLangs.any { it.code == "th" })
        assertTrue(tvLangs.any { it.code == "en" })
        assertFalse(tvLangs.any { it.code in setOf("ml", "ta", "te") })

        val movieLangs = OriginalLanguageRegistry.getTopLanguagesForCategory(SearchCategory.MOVIES)
        assertTrue(movieLangs.any { it.code == "ml" })
        assertTrue(movieLangs.any { it.code == "ta" })
        assertTrue(movieLangs.any { it.code == "te" })

        val bookTopLangs = OriginalLanguageRegistry.getTopLanguagesForCategory(SearchCategory.BOOKS)
        assertFalse(bookTopLangs.any { it.code in setOf("ml", "ta", "te") })

        // 7. Other languages per category
        val movieOtherLangs = OriginalLanguageRegistry.getOtherLanguagesForCategory(SearchCategory.MOVIES)
        assertFalse(movieOtherLangs.any { it.code in setOf("ml", "ta", "te") })

        val tvOtherLangs = OriginalLanguageRegistry.getOtherLanguagesForCategory(SearchCategory.TV_SHOWS)
        assertTrue(tvOtherLangs.any { it.code == "ml" })
        assertTrue(tvOtherLangs.any { it.code == "ta" })
        assertTrue(tvOtherLangs.any { it.code == "te" })

        val bookOtherLangs = OriginalLanguageRegistry.getOtherLanguagesForCategory(SearchCategory.BOOKS)
        assertTrue(bookOtherLangs.any { it.code == "ml" })
        assertTrue(bookOtherLangs.any { it.code == "ta" })
        assertTrue(bookOtherLangs.any { it.code == "te" })

        // 8. Excluded from Anime, Manga, Manhwa, Light Novel
        listOf(
            SearchCategory.ANIME,
            SearchCategory.MANGA,
            SearchCategory.MANHWA,
            SearchCategory.LIGHT_NOVELS
        ).forEach { cat ->
            val allForCat = OriginalLanguageRegistry.getAllLanguagesForCategory(cat)
            val topForCat = OriginalLanguageRegistry.getTopLanguagesForCategory(cat)
            val otherForCat = OriginalLanguageRegistry.getOtherLanguagesForCategory(cat)

            assertFalse(allForCat.any { it.code in setOf("ml", "ta", "te") })
            assertFalse(topForCat.any { it.code in setOf("ml", "ta", "te") })
            assertFalse(otherForCat.any { it.code in setOf("ml", "ta", "te") })
        }
    }

    @Test
    fun testOriginalLanguageInDiscoveryFilterLabels() {
        val tvFilter = DiscoveryFilter.Tv(
            mode = TmdbDiscoveryMode.MOST_POPULAR,
            sortOrder = TmdbTvSort.POPULARITY,
            originalLanguage = "th"
        )
        assertEquals("🔥 Most Popular TV • Most Popular • 🇹🇭 Thai", tvFilter.label)

        val animeFilter = DiscoveryFilter.Anime(
            mode = AnimeDiscoveryMode.MOST_POPULAR,
            sortOrder = AnimeSortOrder.POPULARITY,
            originalLanguage = "ja"
        )
        assertEquals("🔥 Most Popular • Most Popular • 🇯🇵 Japanese", animeFilter.label)

        val movieFilter = DiscoveryFilter.Movie(
            mode = TmdbDiscoveryMode.MOST_POPULAR,
            sortOrder = TmdbMovieSort.POPULARITY,
            originalLanguage = "fr"
        )
        assertEquals("🔥 Most Popular Movies • Most Popular • 🇫🇷 French", movieFilter.label)

        val mangaFilter = DiscoveryFilter.Manga(
            mode = ContentDiscoveryMode.MOST_POPULAR,
            sortOrder = MangaSortOrder.POPULARITY,
            originalLanguage = "zh"
        )
        assertEquals("🔥 Most Popular Manga • Most Popular • 🇨🇳 Chinese", mangaFilter.label)

        val manhwaFilter = DiscoveryFilter.Manhwa(
            mode = ContentDiscoveryMode.MOST_POPULAR,
            sortOrder = MangaSortOrder.POPULARITY,
            originalLanguage = "ko"
        )
        assertEquals("🔥 Most Popular Manhwa • Most Popular • 🇰🇷 Korean", manhwaFilter.label)

        val lnFilter = DiscoveryFilter.LightNovel(
            mode = ContentDiscoveryMode.MOST_POPULAR,
            sortOrder = MangaSortOrder.POPULARITY,
            originalLanguage = "ja"
        )
        assertEquals("🔥 Most Popular Light Novels • Most Popular • 🇯🇵 Japanese", lnFilter.label)

        val bookFilter = DiscoveryFilter.Book(
            mode = ContentDiscoveryMode.MOST_POPULAR,
            sortOrder = BookSortOrder.POPULARITY,
            originalLanguage = "en"
        )
        assertEquals("🔥 Most Popular Books • Most Popular • 🇺🇸 English", bookFilter.label)

        val movieMalayalam = DiscoveryFilter.Movie(
            mode = TmdbDiscoveryMode.MOST_POPULAR,
            sortOrder = TmdbMovieSort.POPULARITY,
            originalLanguage = "ml"
        )
        assertEquals("🔥 Most Popular Movies • Most Popular • 🇮🇳 Malayalam", movieMalayalam.label)

        val tvTamil = DiscoveryFilter.Tv(
            mode = TmdbDiscoveryMode.MOST_POPULAR,
            sortOrder = TmdbTvSort.POPULARITY,
            originalLanguage = "ta"
        )
        assertEquals("🔥 Most Popular TV • Most Popular • 🇮🇳 Tamil", tvTamil.label)

        val bookTelugu = DiscoveryFilter.Book(
            mode = ContentDiscoveryMode.MOST_POPULAR,
            sortOrder = BookSortOrder.POPULARITY,
            originalLanguage = "te"
        )
        assertEquals("🔥 Most Popular Books • Most Popular • 🇮🇳 Telugu", bookTelugu.label)
    }

    @Test
    fun testOriginalLanguageIdentityAndMatching() {
        // Strict equality for UI selection state (fixes all-checked bug)
        assertTrue(OriginalLanguageRegistry.isSameLanguage("en", "en"))
        assertTrue(OriginalLanguageRegistry.isSameLanguage("en", "ENG"))
        assertTrue(OriginalLanguageRegistry.isSameLanguage("ja", "jpn"))
        assertTrue(OriginalLanguageRegistry.isSameLanguage("ko", "KR"))
        assertFalse(OriginalLanguageRegistry.isSameLanguage("en", "ja"))
        assertFalse(OriginalLanguageRegistry.isSameLanguage(null, "en"))
        assertFalse(OriginalLanguageRegistry.isSameLanguage("en", null))
        assertFalse(OriginalLanguageRegistry.isSameLanguage(null, null))
        assertFalse(OriginalLanguageRegistry.isSameLanguage("", "en"))

        // Multi-select OR condition matching
        assertTrue(OriginalLanguageRegistry.matchesAny(emptySet(), "en"))
        assertTrue(OriginalLanguageRegistry.matchesAny(null, "en"))
        assertTrue(OriginalLanguageRegistry.matchesAny(setOf("th", "ko"), "th"))
        assertTrue(OriginalLanguageRegistry.matchesAny(setOf("th", "ko"), "ko"))
        assertTrue(OriginalLanguageRegistry.matchesAny(setOf("th", "ko"), "tha"))
        assertTrue(OriginalLanguageRegistry.matchesAny(setOf("th", "ko"), "kor"))
        assertFalse(OriginalLanguageRegistry.matchesAny(setOf("th", "ko"), "ja"))
        assertFalse(OriginalLanguageRegistry.matchesAny(setOf("th", "ko"), "en"))
        assertFalse(OriginalLanguageRegistry.matchesAny(setOf("th", "ko"), null))
    }

    @Test
    fun testMultiSelectOriginalLanguageFilterLabels() {
        val multiFilter = DiscoveryFilter.Movie(
            mode = TmdbDiscoveryMode.MOST_POPULAR,
            sortOrder = TmdbMovieSort.POPULARITY,
            originalLanguages = setOf("th", "ko")
        )
        assertEquals("🔥 Most Popular Movies • Most Popular • 🇹🇭 Thai, 🇰🇷 Korean", multiFilter.label)
        assertEquals("th", multiFilter.originalLanguage) // Backwards compatible first element getter
        assertEquals(setOf("th", "ko"), multiFilter.originalLanguages)
    }

    @Test
    fun testGamePlatformRegistryOptionsAndLookup() {
        // PC lookup
        val pc = GamePlatformRegistry.findOption("PC")
        assertNotNull(pc)
        assertEquals("PC", pc?.id)
        assertEquals("PC", pc?.displayName)
        assertEquals("Popular", pc?.category)
        assertTrue(pc?.igdbPlatformIds?.contains(6) == true)
        assertTrue(pc?.rawgPlatformIds?.contains(4) == true)

        // Switch lookup
        val switchOpt = GamePlatformRegistry.findOption("Switch")
        assertNotNull(switchOpt)
        assertEquals("Switch", switchOpt?.id)
        assertEquals("Nintendo Switch", switchOpt?.displayName)
        assertEquals("Popular", switchOpt?.category)
        assertTrue(switchOpt?.igdbPlatformIds?.contains(130) == true)
        assertTrue(switchOpt?.rawgPlatformIds?.contains(7) == true)

        // PS5 lookup
        val ps5 = GamePlatformRegistry.findOption("PS5")
        assertNotNull(ps5)
        assertEquals("PlayStation 5", ps5?.displayName)

        // Filter / Search platforms
        val searchSwitch = GamePlatformRegistry.filterPlatforms("switch")
        assertEquals(1, searchSwitch.size)
        assertEquals("Nintendo Switch", searchSwitch.first().displayName)

        val searchPlaystation = GamePlatformRegistry.filterPlatforms("playstation")
        assertTrue(searchPlaystation.size >= 5)

        val topPlatforms = GamePlatformRegistry.getTopPlatforms()
        assertTrue(topPlatforms.any { it.id == "PC" })
        assertTrue(topPlatforms.any { it.id == "Switch" })
        assertTrue(topPlatforms.any { it.id == "PS5" })
        assertTrue(topPlatforms.any { it.id == "Xbox Series" })
        assertTrue(topPlatforms.any { it.id == "Android" })
        assertTrue(topPlatforms.any { it.id == "iOS" })

        val otherPlatforms = GamePlatformRegistry.getOtherPlatforms()
        assertTrue(otherPlatforms.any { it.id == "PS3" })
        assertTrue(otherPlatforms.any { it.id == "3DS" })
        assertTrue(otherPlatforms.any { it.id == "Retro" })
        assertTrue(otherPlatforms.any { it.id == "Linux" })

        // Linux platform in Other Platforms
        val linux = GamePlatformRegistry.findOption("Linux")
        assertNotNull(linux)
        assertEquals("Other", linux?.category)
        assertEquals("🐧", linux?.iconEmoji)
        assertEquals(listOf(3), linux?.igdbPlatformIds)
        assertEquals(listOf(6), linux?.rawgPlatformIds)
        assertTrue(GamePlatformRegistry.matchesPlatform("Linux", "Linux"))
        assertTrue(GamePlatformRegistry.matchesPlatform("Linux", "SteamOS"))
        assertTrue(GamePlatformRegistry.matchesAnyPlatform(setOf("Linux"), listOf("PlayStation 3", "Linux", "PC (Microsoft Windows)")))
        assertFalse(GamePlatformRegistry.matchesPlatform("Linux", "PC (Microsoft Windows)"))
    }

    @Test
    fun testGamePlatformMatchingAndOrCondition() {
        // 1. Single platform matching
        assertTrue(GamePlatformRegistry.matchesPlatform("PC", "PC (Microsoft Windows)"))
        assertTrue(GamePlatformRegistry.matchesPlatform("PC", "PC"))
        assertTrue(GamePlatformRegistry.matchesPlatform("PC", "Windows"))
        assertTrue(GamePlatformRegistry.matchesPlatform("Switch", "Nintendo Switch"))
        assertTrue(GamePlatformRegistry.matchesPlatform("Switch", "Switch"))
        assertTrue(GamePlatformRegistry.matchesPlatform("PS5", "PlayStation 5"))
        assertTrue(GamePlatformRegistry.matchesPlatform("PS4", "PlayStation 4"))
        assertTrue(GamePlatformRegistry.matchesPlatform("Xbox Series", "Xbox Series X/S"))
        assertTrue(GamePlatformRegistry.matchesPlatform("Android", "Android"))
        assertTrue(GamePlatformRegistry.matchesPlatform("iOS", "iOS"))
        assertTrue(GamePlatformRegistry.matchesPlatform("Mobile", "Android"))
        assertTrue(GamePlatformRegistry.matchesPlatform("Mobile", "iOS"))
        assertTrue(GamePlatformRegistry.matchesPlatform("PS", "PlayStation 5"))
        assertTrue(GamePlatformRegistry.matchesPlatform("PS", "PlayStation 4"))
        assertTrue(GamePlatformRegistry.matchesPlatform("XBox", "Xbox Series X/S"))
        assertTrue(GamePlatformRegistry.matchesPlatform("XBox", "Xbox One"))

        assertFalse(GamePlatformRegistry.matchesPlatform("PC", "PlayStation 5"))
        assertFalse(GamePlatformRegistry.matchesPlatform("Switch", "Xbox One"))
        assertFalse(GamePlatformRegistry.matchesPlatform("PS5", "Nintendo Switch"))

        // 2. Multi-select OR condition
        val orPlatforms = setOf("PC", "Switch")
        // PC game -> matches (OR condition)
        assertTrue(GamePlatformRegistry.matchesAnyPlatform(orPlatforms, listOf("PC (Microsoft Windows)")))
        // Switch game -> matches (OR condition)
        assertTrue(GamePlatformRegistry.matchesAnyPlatform(orPlatforms, listOf("Nintendo Switch")))
        // Multi-platform game on PC & PS5 -> matches because it is on PC!
        assertTrue(GamePlatformRegistry.matchesAnyPlatform(orPlatforms, listOf("PC", "PlayStation 5")))
        // PS5 exclusive -> does NOT match PC OR Switch
        assertFalse(GamePlatformRegistry.matchesAnyPlatform(orPlatforms, listOf("PlayStation 5")))
        // Xbox exclusive -> does NOT match PC OR Switch
        assertFalse(GamePlatformRegistry.matchesAnyPlatform(orPlatforms, listOf("Xbox Series X/S")))

        // Empty filter matches any platforms
        assertTrue(GamePlatformRegistry.matchesAnyPlatform(emptySet(), listOf("PlayStation 5")))
        assertTrue(GamePlatformRegistry.matchesAnyPlatform(null, listOf("Xbox One")))
    }

    @Test
    fun testGamePlatformExclusionCondition() {
        val excluded = setOf("Mobile")
        // Mobile-only game -> excluded
        assertTrue(GamePlatformRegistry.isExcluded(excluded, listOf("Android")))
        assertTrue(GamePlatformRegistry.isExcluded(excluded, listOf("iOS")))

        // Multiplatform game including mobile -> excluded
        assertTrue(GamePlatformRegistry.isExcluded(excluded, listOf("PC", "Android")))

        // PC-only game -> NOT excluded
        assertFalse(GamePlatformRegistry.isExcluded(excluded, listOf("PC (Microsoft Windows)")))
        // Console game -> NOT excluded
        assertFalse(GamePlatformRegistry.isExcluded(excluded, listOf("Nintendo Switch", "PlayStation 5")))

        // Multiple exclusions
        val multiExcluded = setOf("Mobile", "Retro")
        assertTrue(GamePlatformRegistry.isExcluded(multiExcluded, listOf("Android")))
        assertTrue(GamePlatformRegistry.isExcluded(multiExcluded, listOf("SNES")))
        assertFalse(GamePlatformRegistry.isExcluded(multiExcluded, listOf("PC", "PlayStation 5")))
    }

    @Test
    fun testGameDiscoveryFilterWithPlatformsAndLabels() {
        val gameFilter = DiscoveryFilter.Game(
            mode = ContentDiscoveryMode.MOST_POPULAR,
            sortOrder = GameSortOrder.POPULARITY,
            includedPlatforms = setOf("PC", "Switch"),
            excludedPlatforms = setOf("Mobile")
        )
        assertEquals(setOf("PC", "Switch"), gameFilter.includedPlatforms)
        assertEquals(setOf("Mobile"), gameFilter.excludedPlatforms)
        assertEquals("🔥 Most Popular Games • Most Popular • PC, Switch", gameFilter.label)

        val singlePlatformFilter = DiscoveryFilter.Game(
            mode = ContentDiscoveryMode.BY_YEAR,
            year = 2024,
            sortOrder = GameSortOrder.RATING,
            includedPlatforms = setOf("Switch")
        )
        assertEquals("🎮 Games 2024 • Highest Rated • Switch", singlePlatformFilter.label)
    }

    @Test
    fun testIgdbAndRawgApiQueryWithPlatforms() {
        // IGDB Apicalypse query with platforms OR condition and exclusion
        val apicalypse = IgdbApiClient.buildDiscoverApicalypseQuery(
            year = 2024,
            sort = "total_rating_count desc",
            includedPlatforms = setOf("PC", "Switch"),
            excludedPlatforms = setOf("Mobile")
        )
        // Should contain platforms = (6, 14, 3, 130) or similar
        assertTrue(apicalypse.contains("platforms = ("))
        assertTrue(apicalypse.contains("6") && apicalypse.contains("130"))
        // Should contain platforms != (34, 39)
        assertTrue(apicalypse.contains("platforms != ("))
        assertTrue(apicalypse.contains("34") && apicalypse.contains("39"))

        // RAWG discover URL with platforms
        val rawgUrl = RawgApiClient.buildDiscoverUrl(
            apiKey = "test_key",
            year = 2024,
            includedPlatforms = setOf("PC", "Switch")
        )
        assertTrue(rawgUrl.contains("&platforms="))
        assertTrue(rawgUrl.contains("4") && rawgUrl.contains("7"))
    }

    @Test
    fun testDurationFormatterPlaytimeAndAverageTime() {
        // Less than 24h
        assertEquals("0.0 hrs", DurationFormatter.formatHours(0.0))
        assertEquals("12.5 hrs", DurationFormatter.formatHours(12.5))
        assertEquals("23.0 hrs", DurationFormatter.formatHours(23.0))

        // Days (24h to 7d)
        assertEquals("1.0 days", DurationFormatter.formatHours(24.0))
        assertEquals("2.5 days", DurationFormatter.formatHours(60.0))
        assertEquals("6.9 days", DurationFormatter.formatHours(165.6))

        // Weeks (7d to ~30.4d)
        assertEquals("1.0 wks", DurationFormatter.formatHours(168.0)) // 7 * 24
        assertEquals("2.5 wks", DurationFormatter.formatHours(420.0))

        // Months (~30.4d to 365.25d)
        assertEquals("1.0 mos", DurationFormatter.formatHours(30.4375 * 24.0))
        assertEquals("3.5 mos", DurationFormatter.formatHours(3.5 * 30.4375 * 24.0))

        // Years (>= 365.25d)
        assertEquals("1.0 yrs", DurationFormatter.formatHours(365.25 * 24.0))
        assertEquals("2.5 yrs", DurationFormatter.formatHours(2.5 * 365.25 * 24.0))

        // Minutes formatter (as used in game detail page for Baldur's Gate 3 ~5649h 38m)
        val bg3Minutes = 5649 * 60 + 38
        val bg3Formatted = DurationFormatter.formatMinutes(bg3Minutes)
        assertTrue(bg3Formatted.endsWith("mos") || bg3Formatted.endsWith("yrs"))

        // Small minutes (< 24h)
        assertEquals("45m", DurationFormatter.formatMinutes(45))
        assertEquals("2h 30m", DurationFormatter.formatMinutes(150))
    }

    @Test
    fun testMediaEntryEffectiveLanguageAndRegistry() {
        val entryWithExplicit = MediaEntry(
            title = "Test Manga",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            originalLanguage = "ja"
        )
        assertEquals("ja", entryWithExplicit.getEffectiveLanguage())

        val entryWithTag = MediaEntry(
            title = "Korean Webtoon",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            tags = "Action, Korean, Webtoon"
        )
        assertEquals("ko", entryWithTag.getEffectiveLanguage())

        val entryWithManhwaCategory = MediaEntry(
            title = "Solo Leveling",
            mediaType = MediaType.MANGA,
            status = MediaStatus.WATCHING,
            customCategory = "Manhwa"
        )
        assertEquals("ko", entryWithManhwaCategory.getEffectiveLanguage())

        val defaultAnime = MediaEntry(
            title = "Anime Show",
            mediaType = MediaType.ANIME,
            status = MediaStatus.WATCHING
        )
        assertEquals("ja", defaultAnime.getEffectiveLanguage())

        val defaultMovie = MediaEntry(
            title = "Hollywood Movie",
            mediaType = MediaType.MOVIE,
            status = MediaStatus.WATCHING
        )
        assertEquals("en", defaultMovie.getEffectiveLanguage())
    }

    @Test
    fun testPlatformAndLanguageMatchingRules() {
        // Platform matching OR condition
        val pcSwitchFilter = setOf("PC", "Nintendo Switch")
        assertTrue(GamePlatformRegistry.matchesAnyPlatform(pcSwitchFilter, listOf("PC (Microsoft Windows)")))
        assertTrue(GamePlatformRegistry.matchesAnyPlatform(pcSwitchFilter, listOf("Nintendo Switch")))
        assertFalse(GamePlatformRegistry.matchesAnyPlatform(pcSwitchFilter, listOf("PlayStation 5")))

        // Platform exclusion
        val mobileExc = setOf("Mobile (Android / iOS)")
        assertTrue(GamePlatformRegistry.isExcluded(mobileExc, listOf("Android", "PC")))
        assertFalse(GamePlatformRegistry.isExcluded(mobileExc, listOf("PC", "Nintendo Switch")))

        // Language matching
        val langFilter = setOf("ja", "ko")
        assertTrue(OriginalLanguageRegistry.matchesAny(langFilter, "ja"))
        assertTrue(OriginalLanguageRegistry.matchesAny(langFilter, "Japanese"))
        assertTrue(OriginalLanguageRegistry.matchesAny(langFilter, "ko"))
        assertFalse(OriginalLanguageRegistry.matchesAny(langFilter, "en"))
    }

    @Test
    fun testOriginalLanguagePillAndRegistry() {
        val zhOpt = OriginalLanguageRegistry.findByCode("zh")
        assertNotNull(zhOpt)
        assertEquals("Chinese", zhOpt?.displayName)
        assertEquals("🇨🇳", zhOpt?.flagEmoji)
        val zhLabel = if (zhOpt != null) "${zhOpt.flagEmoji} ${zhOpt.displayName}" else "🌐 zh"
        assertEquals("🇨🇳 Chinese", zhLabel)

        val jaOpt = OriginalLanguageRegistry.findByCode("ja")
        assertNotNull(jaOpt)
        assertEquals("Japanese", jaOpt?.displayName)
        assertEquals("🇯🇵", jaOpt?.flagEmoji)
        val jaLabel = if (jaOpt != null) "${jaOpt.flagEmoji} ${jaOpt.displayName}" else "🌐 ja"
        assertEquals("🇯🇵 Japanese", jaLabel)

        val koOpt = OriginalLanguageRegistry.findByCode("ko")
        assertNotNull(koOpt)
        assertEquals("Korean", koOpt?.displayName)
        assertEquals("🇰🇷", koOpt?.flagEmoji)

        // Custom / fallback language
        val customOpt = OriginalLanguageRegistry.findByCode("custom-lang")
        val customLabel = if (customOpt != null) "${customOpt.flagEmoji} ${customOpt.displayName}" else "🌐 custom-lang"
        assertEquals("🌐 custom-lang", customLabel)
    }

    @Test
    fun testOriginalLanguageSaveForMediaTypes() {
        // Media entry for Manhwa with originalLanguage
        val manhwaEntry = MediaEntry(
            title = "I'm The Great Immortal",
            mediaType = MediaType.MANHWA,
            status = MediaStatus.PLAN_TO_WATCH,
            originalLanguage = "zh"
        )
        assertEquals("zh", manhwaEntry.originalLanguage)

        // Simulating save logic: games do not have originalLanguage persisted
        val gameMediaType = MediaType.GAME
        val originalLangInput = "ja"
        val persistedGameLang = if (gameMediaType != MediaType.GAME) originalLangInput.trim().ifBlank { null } else null
        assertEquals(null, persistedGameLang)

        val animeMediaType = MediaType.ANIME
        val persistedAnimeLang = if (animeMediaType != MediaType.GAME) originalLangInput.trim().ifBlank { null } else null
        assertEquals("ja", persistedAnimeLang)
    }

    @Test
    fun testTmdbDetailsOriginalLanguageExtraction() {
        val movieJson = JsonObject().apply {
            addProperty("id", 9659L)
            addProperty("title", "Mad Max")
            addProperty("original_language", "en")
            addProperty("release_date", "1979-04-12")
            addProperty("vote_average", 6.7f)
            addProperty("runtime", 91)
        }
        val movieDetails = TmdbApiClient.parseMovieDetails(movieJson, 9659L)
        assertEquals("en", movieDetails.originalLanguage)
        assertEquals("Mad Max", movieDetails.title)

        val tvJson = JsonObject().apply {
            addProperty("id", 1399L)
            addProperty("name", "Game of Thrones")
            addProperty("original_language", "en")
            addProperty("first_air_date", "2011-04-17")
            addProperty("vote_average", 8.4f)
        }
        val tvDetails = TmdbApiClient.parseTvDetails(tvJson, 1399L)
        assertEquals("en", tvDetails.originalLanguage)
        assertEquals("Game of Thrones", tvDetails.title)
    }
}
