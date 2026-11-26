package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.RelatedMediaItem
import com.lx.tracklist.ui.search.AdultMode
import com.lx.tracklist.ui.search.BookSortOrder
import com.lx.tracklist.ui.search.BookTagRegistry
import com.lx.tracklist.ui.search.ContentDiscoveryMode
import com.lx.tracklist.ui.search.OriginalLanguageRegistry
import java.util.Calendar
import java.util.TimeZone

object BookMetadataManager {

    private const val TAG = "BookMetadataManager"

    fun discoverBooks(
        mode: ContentDiscoveryMode = ContentDiscoveryMode.BY_YEAR,
        startYear: Int? = null,
        endYear: Int? = null,
        sort: BookSortOrder = BookSortOrder.POPULARITY,
        search: String? = null,
        includedTags: Set<String> = emptySet(),
        excludedTags: Set<String> = emptySet(),
        adultMode: AdultMode = AdultMode.OFF,
        originalLanguages: Set<String> = emptySet(),
        originalLanguage: String? = null,
        page: Int = 1,
        limit: Int = 20,
        context: Context? = null
    ): Result<List<BookDetails>> {
        val effectiveLanguages = if (originalLanguages.isNotEmpty()) originalLanguages else originalLanguage?.let { setOf(it) } ?: emptySet()
        val primaryLang = if (effectiveLanguages.size == 1) effectiveLanguages.first() else null
        val subject = includedTags.mapNotNull { BookTagRegistry.getSubject(it) }.firstOrNull()
        val openLibSort = when (sort) {
            BookSortOrder.POPULARITY -> "editions"
            BookSortOrder.RATING -> "rating"
            BookSortOrder.NEWEST -> "new"
            BookSortOrder.TITLE -> "title"
        }

        val openLibRes = OpenLibraryApiClient.discoverBooks(
            startYear = if (mode == ContentDiscoveryMode.BY_YEAR) startYear else null,
            endYear = if (mode == ContentDiscoveryMode.BY_YEAR) endYear else null,
            sort = openLibSort,
            subject = subject,
            search = search,
            originalLanguage = primaryLang,
            originalLanguages = effectiveLanguages,
            page = page,
            limit = limit
        )

        val rawList = if (openLibRes.isSuccess && openLibRes.getOrDefault(emptyList()).isNotEmpty()) {
            openLibRes.getOrDefault(emptyList())
        } else {
            val googleOrderBy = if (sort == BookSortOrder.NEWEST) "newest" else "relevance"
            if (effectiveLanguages.size > 1) {
                effectiveLanguages.flatMap { lang ->
                    GoogleBooksApiClient.discoverBooks(
                        subject = subject,
                        startYear = if (mode == ContentDiscoveryMode.BY_YEAR) startYear else null,
                        endYear = if (mode == ContentDiscoveryMode.BY_YEAR) endYear else null,
                        search = search,
                        orderBy = googleOrderBy,
                        startIndex = (page - 1) * limit,
                        limit = limit,
                        originalLanguage = lang,
                        context = context
                    ).getOrDefault(emptyList())
                }.distinctBy { it.id }
            } else {
                val googleRes = GoogleBooksApiClient.discoverBooks(
                    subject = subject,
                    startYear = if (mode == ContentDiscoveryMode.BY_YEAR) startYear else null,
                    endYear = if (mode == ContentDiscoveryMode.BY_YEAR) endYear else null,
                    search = search,
                    orderBy = googleOrderBy,
                    startIndex = (page - 1) * limit,
                    limit = limit,
                    originalLanguage = primaryLang,
                    context = context
                )
                googleRes.getOrDefault(emptyList())
            }
        }

        val filtered = if (excludedTags.isNotEmpty()) {
            rawList.filterNot { book ->
                val bookGenres = (book.categories ?: emptyList()) + listOfNotNull(book.description)
                excludedTags.any { exc ->
                    bookGenres.any { g -> g.contains(exc, ignoreCase = true) }
                }
            }
        } else {
            rawList
        }

        val langFiltered = if (effectiveLanguages.isNotEmpty()) {
            filtered.filter { book ->
                com.lx.tracklist.ui.search.OriginalLanguageRegistry.matchesAny(effectiveLanguages, book.language) ||
                (book.language.isNullOrBlank() && primaryLang != null)
            }
        } else {
            filtered
        }

        return Result.success(langFiltered)
    }

    /**
     * Search standard books / novels using Google Books as primary, falling back to Open Library.
     */
    fun searchBooks(
        query: String,
        context: Context? = null,
        limit: Int = 20,
        page: Int = 1
    ): Result<List<BookDetails>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return Result.success(emptyList())

        val isIsbn = cleanQuery.replace("-", "").matches(Regex("^[0-9]{9}[0-9Xx]$|^[0-9]{13}$"))

        if (isIsbn) {
            val cleanIsbn = cleanQuery.replace("-", "")
            Log.d(TAG, "Executing ISBN search for '$cleanIsbn'")
            val googleRes = GoogleBooksApiClient.searchByIsbn(cleanIsbn, context)
            if (googleRes.isSuccess && googleRes.getOrNull() != null) {
                return Result.success(listOf(googleRes.getOrNull()!!))
            }
            Log.i(TAG, "Google Books ISBN not found, falling back to Open Library for '$cleanIsbn'")
            val openLibRes = OpenLibraryApiClient.searchByIsbn(cleanIsbn)
            if (openLibRes.isSuccess && openLibRes.getOrNull() != null) {
                return Result.success(listOf(openLibRes.getOrNull()!!))
            }
            return Result.success(emptyList())
        }

        // Title / author / general text search
        val startIndex = ((page - 1) * limit).coerceAtLeast(0)
        val googleRes = GoogleBooksApiClient.searchBooks(cleanQuery, context, limit, startIndex = startIndex)
        if (googleRes.isSuccess) {
            val list = googleRes.getOrDefault(emptyList())
            if (list.isNotEmpty()) {
                return Result.success(list)
            }
        }

        Log.i(TAG, "Google Books search empty or failed for '$cleanQuery', falling back to Open Library")
        val openLibRes = OpenLibraryApiClient.searchBooks(cleanQuery, limit)
        if (openLibRes.isSuccess) {
            val list = openLibRes.getOrDefault(emptyList())
            if (list.isNotEmpty()) {
                return Result.success(list)
            }
        }

        return googleRes
    }

    /**
     * Search Light Novels and Asian Web Novels using AniList (format: NOVEL) as primary,
     * falling back to Jikan v4 (type: lightnovel) as secondary.
     */
    fun searchLightNovels(
        query: String,
        limit: Int = 20,
        page: Int = 1
    ): Result<List<JikanMediaDetails>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return Result.success(emptyList())

        Log.d(TAG, "Searching Light Novels on AniList for '$cleanQuery'")
        val anilistRes = AniListNovelApiClient.searchLightNovels(cleanQuery, limit, page)
        if (anilistRes.isSuccess) {
            val items = anilistRes.getOrDefault(emptyList())
            if (items.isNotEmpty()) {
                return Result.success(items)
            }
        }

        Log.i(TAG, "AniList Light Novel search returned no results, falling back to Jikan for '$cleanQuery'")
        val jikanRes = JikanApiClient.searchJikanMultiple(
            queryTitle = cleanQuery,
            isManga = true,
            subType = "lightnovel",
            limit = limit,
            page = page
        )

        return if (jikanRes.isSuccess && jikanRes.getOrDefault(emptyList()).isNotEmpty()) {
            jikanRes
        } else {
            // Also try subType = "novel" on Jikan
            JikanApiClient.searchJikanMultiple(
                queryTitle = cleanQuery,
                isManga = true,
                subType = "novel",
                limit = limit,
                page = page
            )
        }
    }

    /**
     * Fetch related books (prequels, sequels, series franchise entries).
     * 1. Checks AniList/Jikan if categorized as a Light Novel.
     * 2. Checks Open Library Series API (canonical franchise sequencing).
     * 3. Falls back to Open Library search.
     * 4. Falls back to Google Books search.
     */
    fun fetchRelatedBooks(
        entry: MediaEntry,
        context: Context? = null
    ): Result<List<RelatedMediaItem>> {
        val title = entry.title.trim()
        if (title.isBlank()) return Result.success(emptyList())

        val isLightNovel = entry.isLightNovel()

        if (isLightNovel) {
            val lnRes = JikanApiClient.fetchRelatedEntries(
                malId = entry.externalId?.toLongOrNull(),
                isManga = true,
                title = title,
                externalSource = entry.externalSource,
                externalId = entry.externalId
            )
            if (lnRes.isSuccess && lnRes.getOrDefault(emptyList()).isNotEmpty()) {
                return lnRes
            }
        }

        val authorRegex = Regex("(?i)Author:\\s*([^\r\n]+)")
        val authorFromDesc = (entry.description ?: entry.notes)?.let { authorRegex.find(it)?.groupValues?.get(1)?.trim() }

        val isbnRegex = Regex("(?i)ISBN:\\s*([0-9Xx-]+)")
        val isbnFromDesc = (entry.description ?: entry.notes)?.let { isbnRegex.find(it)?.groupValues?.get(1)?.replace("-", "")?.trim() }

        val publishYear = entry.airedStartDate?.let {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = it }
            cal.get(Calendar.YEAR).toString()
        }

        // 1. Open Library Series API (canonical franchise sequencing)
        val openLibRes = OpenLibraryApiClient.fetchRelatedBooks(
            title = title,
            author = authorFromDesc,
            currentYear = publishYear,
            isbn = isbnFromDesc,
            externalId = entry.externalId.takeIf { entry.externalSource.equals("Open Library", ignoreCase = true) }
        )
        if (openLibRes.isSuccess && openLibRes.getOrDefault(emptyList()).isNotEmpty()) {
            return openLibRes
        }

        // 2. Google Books fallback
        val googleBooksRes = GoogleBooksApiClient.fetchRelatedBooks(
            title = title,
            author = authorFromDesc,
            currentYear = publishYear,
            context = context
        )
        if (googleBooksRes.isSuccess && googleBooksRes.getOrDefault(emptyList()).isNotEmpty()) {
            return googleBooksRes
        }

        return Result.success(emptyList())
    }
}

