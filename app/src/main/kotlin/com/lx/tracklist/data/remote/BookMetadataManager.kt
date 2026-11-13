package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.RelatedMediaItem
import java.util.Calendar
import java.util.TimeZone

object BookMetadataManager {

    private const val TAG = "BookMetadataManager"

    /**
     * Search standard books / novels using Google Books as primary, falling back to Open Library.
     */
    fun searchBooks(
        query: String,
        context: Context? = null,
        limit: Int = 20
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
        val googleRes = GoogleBooksApiClient.searchBooks(cleanQuery, context, limit)
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
        limit: Int = 20
    ): Result<List<JikanMediaDetails>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return Result.success(emptyList())

        Log.d(TAG, "Searching Light Novels on AniList for '$cleanQuery'")
        val anilistRes = AniListNovelApiClient.searchLightNovels(cleanQuery, limit)
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
            limit = limit
        )

        return if (jikanRes.isSuccess && jikanRes.getOrDefault(emptyList()).isNotEmpty()) {
            jikanRes
        } else {
            // Also try subType = "novel" on Jikan
            JikanApiClient.searchJikanMultiple(
                queryTitle = cleanQuery,
                isManga = true,
                subType = "novel",
                limit = limit
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

        val isLightNovel = entry.externalSource.equals("AniList", ignoreCase = true) ||
                entry.customCategory.equals("Light Novels", ignoreCase = true) ||
                entry.subCategory.equals("Light Novel", ignoreCase = true)

        if (isLightNovel) {
            val lnRes = JikanApiClient.fetchRelatedEntries(
                malId = entry.externalId?.toLongOrNull(),
                isManga = true,
                title = title
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

