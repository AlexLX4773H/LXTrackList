package com.lx.tracklist.util

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString

object ExternalLinkHelper {

    fun openUrlSafely(
        url: String?,
        uriHandler: UriHandler,
        context: Context,
        clipboardManager: ClipboardManager? = null
    ) {
        if (url.isNullOrBlank()) return
        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {
            if (clipboardManager != null) {
                clipboardManager.setText(AnnotatedString(url))
                Toast.makeText(context, "Link copied to clipboard (no browser available)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun buildImdbUrl(title: String, imdbId: String? = null): String {
        return if (!imdbId.isNullOrBlank()) {
            val cleanId = imdbId.trim()
            if (cleanId.startsWith("tt", ignoreCase = true)) {
                "https://www.imdb.com/title/$cleanId/"
            } else {
                "https://www.imdb.com/title/tt$cleanId/"
            }
        } else {
            val encodedTitle = Uri.encode(title.trim())
            "https://www.imdb.com/find/?q=$encodedTitle"
        }
    }

    fun buildRottenTomatoesUrl(title: String): String {
        val encodedTitle = Uri.encode(title.trim())
        return "https://www.rottentomatoes.com/search?search=$encodedTitle"
    }

    fun buildTmdbUrl(tmdbId: String, isMovie: Boolean): String {
        val type = if (isMovie) "movie" else "tv"
        return "https://www.themoviedb.org/$type/$tmdbId"
    }

    data class ExternalOpenOption(
        val title: String,
        val subtitle: String,
        val url: String,
        val color: Long = 0xFF4ADE80
    )

    fun buildTmdbOpenOptions(
        title: String,
        tmdbId: String?,
        isMovie: Boolean,
        imdbId: String? = null,
        webUrl: String? = null
    ): List<ExternalOpenOption> {
        val list = mutableListOf<ExternalOpenOption>()
        val directTmdbUrl = webUrl ?: if (!tmdbId.isNullOrBlank() && tmdbId != "0") buildTmdbUrl(tmdbId, isMovie) else null
        if (directTmdbUrl != null) {
            list.add(
                ExternalOpenOption(
                    title = "The Movie Database (TMDB)",
                    subtitle = "themoviedb.org",
                    url = directTmdbUrl,
                    color = 0xFF01B4E4
                )
            )
        }
        list.add(
            ExternalOpenOption(
                title = "IMDb ⭐",
                subtitle = if (!imdbId.isNullOrBlank()) "Direct title ($imdbId)" else "Search title on IMDb",
                url = buildImdbUrl(title, imdbId),
                color = 0xFFF5C518
            )
        )
        list.add(
            ExternalOpenOption(
                title = "Rotten Tomatoes 🍅",
                subtitle = "Search on Rotten Tomatoes",
                url = buildRottenTomatoesUrl(title),
                color = 0xFFFF523B
            )
        )
        return list
    }

    fun buildAnimeOpenOptions(
        title: String,
        externalId: String?,
        externalSource: String?,
        directMalId: Long? = null,
        directSiteUrl: String? = null,
        webUrl: String? = null
    ): List<ExternalOpenOption> {
        val list = mutableListOf<ExternalOpenOption>()
        val encodedTitle = Uri.encode(title.trim())

        // 1. MyAnimeList
        val malUrl = when {
            directMalId != null && directMalId > 0 -> "https://myanimelist.net/anime/$directMalId"
            externalSource.equals("MAL", ignoreCase = true) && !externalId.isNullOrBlank() && externalId != "0" && externalId.toLongOrNull() != null ->
                "https://myanimelist.net/anime/$externalId"
            webUrl != null && webUrl.contains("myanimelist.net", ignoreCase = true) -> webUrl
            else -> "https://myanimelist.net/anime.php?q=$encodedTitle"
        }
        list.add(
            ExternalOpenOption(
                title = "MyAnimeList (MAL)",
                subtitle = "myanimelist.net",
                url = malUrl,
                color = 0xFF2E51A2
            )
        )

        // 2. AniList
        val aniUrl = when {
            !directSiteUrl.isNullOrBlank() && directSiteUrl.contains("anilist.co", ignoreCase = true) -> directSiteUrl
            externalSource?.startsWith("AniList", ignoreCase = true) == true && !externalId.isNullOrBlank() && externalId.toLongOrNull() != null ->
                "https://anilist.co/anime/$externalId"
            webUrl != null && webUrl.contains("anilist.co", ignoreCase = true) -> webUrl
            else -> "https://anilist.co/search/anime?search=$encodedTitle"
        }
        list.add(
            ExternalOpenOption(
                title = "AniList",
                subtitle = "anilist.co",
                url = aniUrl,
                color = 0xFF02A9FF
            )
        )

        // 3. Kitsu
        val kitsuUrl = when {
            externalSource.equals("Kitsu", ignoreCase = true) && !externalId.isNullOrBlank() ->
                "https://kitsu.app/anime/$externalId"
            webUrl != null && webUrl.contains("kitsu.app", ignoreCase = true) -> webUrl
            else -> "https://kitsu.app/anime?text=$encodedTitle"
        }
        list.add(
            ExternalOpenOption(
                title = "Kitsu",
                subtitle = "kitsu.app",
                url = kitsuUrl,
                color = 0xFFFF4081
            )
        )

        return list
    }

    fun buildMangaOpenOptions(
        title: String,
        externalId: String?,
        externalSource: String?,
        directMalId: Long? = null,
        webUrl: String? = null
    ): List<ExternalOpenOption> {
        val list = mutableListOf<ExternalOpenOption>()
        val encodedTitle = Uri.encode(title.trim())

        // 1. MangaDex
        val mdUrl = when {
            externalSource.equals("MangaDex", ignoreCase = true) && !externalId.isNullOrBlank() && externalId != "0" ->
                "https://mangadex.org/title/$externalId"
            webUrl != null && webUrl.contains("mangadex.org", ignoreCase = true) -> webUrl
            else -> "https://mangadex.org/titles?q=$encodedTitle"
        }
        list.add(
            ExternalOpenOption(
                title = "MangaDex",
                subtitle = "mangadex.org",
                url = mdUrl,
                color = 0xFFFF6740
            )
        )

        // 2. MyAnimeList
        val malUrl = when {
            directMalId != null && directMalId > 0 -> "https://myanimelist.net/manga/$directMalId"
            externalSource.equals("MAL", ignoreCase = true) && !externalId.isNullOrBlank() && externalId != "0" && externalId.toLongOrNull() != null ->
                "https://myanimelist.net/manga/$externalId"
            webUrl != null && webUrl.contains("myanimelist.net", ignoreCase = true) -> webUrl
            else -> "https://myanimelist.net/manga.php?q=$encodedTitle"
        }
        list.add(
            ExternalOpenOption(
                title = "MyAnimeList (MAL)",
                subtitle = "myanimelist.net",
                url = malUrl,
                color = 0xFF2E51A2
            )
        )

        // 3. AniList
        val aniUrl = when {
            externalSource?.startsWith("AniList", ignoreCase = true) == true && !externalId.isNullOrBlank() && externalId.toLongOrNull() != null ->
                "https://anilist.co/manga/$externalId"
            webUrl != null && webUrl.contains("anilist.co", ignoreCase = true) -> webUrl
            else -> "https://anilist.co/search/manga?search=$encodedTitle"
        }
        list.add(
            ExternalOpenOption(
                title = "AniList",
                subtitle = "anilist.co",
                url = aniUrl,
                color = 0xFF02A9FF
            )
        )

        // 4. Kitsu
        val kitsuUrl = when {
            externalSource.equals("Kitsu", ignoreCase = true) && !externalId.isNullOrBlank() ->
                "https://kitsu.app/manga/$externalId"
            webUrl != null && webUrl.contains("kitsu.app", ignoreCase = true) -> webUrl
            else -> "https://kitsu.app/manga?text=$encodedTitle"
        }
        list.add(
            ExternalOpenOption(
                title = "Kitsu",
                subtitle = "kitsu.app",
                url = kitsuUrl,
                color = 0xFFFF4081
            )
        )

        return list
    }

    fun buildBookOpenOptions(
        title: String,
        externalId: String?,
        externalSource: String?,
        webUrl: String? = null
    ): List<ExternalOpenOption> {
        val list = mutableListOf<ExternalOpenOption>()
        val encodedTitle = Uri.encode(title.trim())

        // 1. Google Books
        val gbUrl = when {
            externalSource.equals("Google Books", ignoreCase = true) && !externalId.isNullOrBlank() ->
                webUrl ?: "https://books.google.com/books?id=$externalId"
            webUrl != null && webUrl.contains("books.google.com", ignoreCase = true) -> webUrl
            else -> "https://www.google.com/search?tbm=bks&q=$encodedTitle"
        }
        list.add(
            ExternalOpenOption(
                title = "Google Books",
                subtitle = "books.google.com",
                url = gbUrl,
                color = 0xFF4285F4
            )
        )

        // 2. Open Library
        val olUrl = when {
            externalSource.equals("Open Library", ignoreCase = true) && !externalId.isNullOrBlank() -> {
                val cleanKey = externalId.removePrefix("/works/").removePrefix("/").trim()
                "https://openlibrary.org/works/$cleanKey"
            }
            webUrl != null && webUrl.contains("openlibrary.org", ignoreCase = true) -> webUrl
            else -> "https://openlibrary.org/search?q=$encodedTitle"
        }
        list.add(
            ExternalOpenOption(
                title = "Open Library",
                subtitle = "openlibrary.org",
                url = olUrl,
                color = 0xFFE38836
            )
        )

        // 3. Goodreads
        list.add(
            ExternalOpenOption(
                title = "Goodreads 📚",
                subtitle = "goodreads.com",
                url = "https://www.goodreads.com/search?q=$encodedTitle",
                color = 0xFFD6C097
            )
        )

        return list
    }

    fun buildLightNovelOpenOptions(
        title: String,
        externalId: String?,
        externalSource: String?,
        directMalId: Long? = null,
        webUrl: String? = null
    ): List<ExternalOpenOption> {
        val list = mutableListOf<ExternalOpenOption>()
        val encodedTitle = Uri.encode(title.trim())

        // 1. AniList
        val aniUrl = when {
            externalSource?.startsWith("AniList", ignoreCase = true) == true && !externalId.isNullOrBlank() && externalId.toLongOrNull() != null ->
                "https://anilist.co/manga/$externalId"
            webUrl != null && webUrl.contains("anilist.co", ignoreCase = true) -> webUrl
            else -> "https://anilist.co/search/manga?search=$encodedTitle"
        }
        list.add(
            ExternalOpenOption(
                title = "AniList",
                subtitle = "anilist.co",
                url = aniUrl,
                color = 0xFF02A9FF
            )
        )

        // 2. MyAnimeList
        val malUrl = when {
            directMalId != null && directMalId > 0 -> "https://myanimelist.net/manga/$directMalId"
            externalSource.equals("MAL", ignoreCase = true) && !externalId.isNullOrBlank() && externalId != "0" && externalId.toLongOrNull() != null ->
                "https://myanimelist.net/manga/$externalId"
            webUrl != null && webUrl.contains("myanimelist.net", ignoreCase = true) -> webUrl
            else -> "https://myanimelist.net/manga.php?q=$encodedTitle"
        }
        list.add(
            ExternalOpenOption(
                title = "MyAnimeList (MAL)",
                subtitle = "myanimelist.net",
                url = malUrl,
                color = 0xFF2E51A2
            )
        )

        // 3. BookWalker
        list.add(
            ExternalOpenOption(
                title = "BookWalker",
                subtitle = "global.bookwalker.jp",
                url = "https://global.bookwalker.jp/search/?word=$encodedTitle",
                color = 0xFF00A0E9
            )
        )

        // 4. MangaDex
        val mdUrl = when {
            externalSource.equals("MangaDex", ignoreCase = true) && !externalId.isNullOrBlank() ->
                "https://mangadex.org/title/$externalId"
            else -> "https://mangadex.org/titles?q=$encodedTitle"
        }
        list.add(
            ExternalOpenOption(
                title = "MangaDex",
                subtitle = "mangadex.org",
                url = mdUrl,
                color = 0xFFFF6740
            )
        )

        return list
    }
}
