package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log

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
}

