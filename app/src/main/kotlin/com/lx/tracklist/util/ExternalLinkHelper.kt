package com.lx.tracklist.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import com.lx.tracklist.data.remote.KitsuApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object ExternalLinkHelper {

    private val kitsuCache = ConcurrentHashMap<String, String>()

    fun openUrlSafely(
        url: String?,
        uriHandler: UriHandler,
        context: Context,
        clipboardManager: ClipboardManager? = null
    ) {
        if (url.isNullOrBlank()) return

        // Intercept kitsu-resolve scheme
        if (url.startsWith("kitsu-resolve://", ignoreCase = true)) {
            resolveAndOpenKitsu(url, uriHandler, context, clipboardManager)
            return
        }

        // Intercept numeric Kitsu web URLs which are known to 404 on Kitsu's frontend SPA
        if (url.startsWith("https://kitsu.app/anime/", ignoreCase = true) ||
            url.startsWith("https://kitsu.app/manga/", ignoreCase = true) ||
            url.startsWith("https://kitsu.io/anime/", ignoreCase = true) ||
            url.startsWith("https://kitsu.io/manga/", ignoreCase = true)
        ) {
            val cleanUrl = url.replace("kitsu.io", "kitsu.app").substringBefore("?").substringBefore("#")
            val isAnime = cleanUrl.contains("/anime/")
            val target = cleanUrl.substringAfterLast('/')
            if (target.all { it.isDigit() } && target.isNotBlank()) {
                val type = if (isAnime) "anime" else "manga"
                resolveAndOpenKitsu("kitsu-resolve://$type?kitsuId=$target", uriHandler, context, clipboardManager)
                return
            }
        }

        try {
            uriHandler.openUri(url)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                if (clipboardManager != null) {
                    clipboardManager.setText(AnnotatedString(url))
                    Toast.makeText(context, "Link copied to clipboard (no browser available)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun resolveAndOpenKitsu(
        uriString: String,
        uriHandler: UriHandler,
        context: Context,
        clipboardManager: ClipboardManager?
    ) {
        val parsedUri = Uri.parse(uriString)
        val isAnime = parsedUri.host.equals("anime", ignoreCase = true) || uriString.contains("/anime")
        val mediaType = if (isAnime) "anime" else "manga"
        val title = parsedUri.getQueryParameter("title").orEmpty()
        val malId = parsedUri.getQueryParameter("malId")?.toLongOrNull() ?: 0L
        val kitsuId = parsedUri.getQueryParameter("kitsuId")?.toLongOrNull() ?: 0L

        val cacheKey = "$mediaType:$malId:$title:$kitsuId"
        val cachedUrl = kitsuCache[cacheKey]
        if (!cachedUrl.isNullOrBlank()) {
            openResolvedUri(cachedUrl, uriHandler, context, clipboardManager)
            return
        }

        Toast.makeText(context, "Opening on Kitsu...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            var resolvedSlug: String? = null

            // 1. If numeric kitsuId provided, lookup directly from Kitsu API
            if (kitsuId > 0) {
                resolvedSlug = KitsuApiClient.lookupSlugById(kitsuId, isAnime)
            }

            // 2. If MAL ID is provided, lookup via official mappings endpoint
            if (resolvedSlug.isNullOrBlank() && malId > 0) {
                resolvedSlug = KitsuApiClient.lookupSlugByMalId(malId, isAnime)
            }

            // 3. If still not resolved, query Kitsu search API by title
            if (resolvedSlug.isNullOrBlank() && title.isNotBlank()) {
                val cleanTitle = title
                    .replace(Regex("\\((TV|ONA|OVA|Movie|Special|Dub|Sub)\\)", RegexOption.IGNORE_CASE), "")
                    .trim()
                resolvedSlug = KitsuApiClient.lookupSlugByTitle(cleanTitle, isAnime)
            }

            // 4. Construct final URL:
            val finalUrl = if (!resolvedSlug.isNullOrBlank()) {
                "https://kitsu.app/$mediaType/$resolvedSlug"
            } else {
                val cleanTitle = title
                    .replace(Regex("\\((TV|ONA|OVA|Movie|Special|Dub|Sub)\\)", RegexOption.IGNORE_CASE), "")
                    .trim()
                "https://www.google.com/search?q=site:kitsu.app/$mediaType+${Uri.encode(cleanTitle)}"
            }

            kitsuCache[cacheKey] = finalUrl

            withContext(Dispatchers.Main) {
                openResolvedUri(finalUrl, uriHandler, context, clipboardManager)
            }
        }
    }

    private fun openResolvedUri(
        finalUrl: String,
        uriHandler: UriHandler,
        context: Context,
        clipboardManager: ClipboardManager?
    ) {
        try {
            uriHandler.openUri(finalUrl)
        } catch (_: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                clipboardManager?.setText(AnnotatedString(finalUrl))
                Toast.makeText(context, "Copied link to clipboard", Toast.LENGTH_SHORT).show()
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

    fun toKitsuSlug(title: String): String {
        val cleanTitle = title
            .replace(Regex("\\((TV|ONA|OVA|Movie|Special|Dub|Sub)\\)", RegexOption.IGNORE_CASE), "")
            .trim()
        return cleanTitle
            .lowercase()
            .replace(Regex("['’]"), "")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
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
        directAniListId: Long? = null,
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
            directAniListId != null && directAniListId > 0 -> "https://anilist.co/anime/$directAniListId"
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
            !directSiteUrl.isNullOrBlank() &&
                (directSiteUrl.contains("kitsu.io/anime/") || directSiteUrl.contains("kitsu.app/anime/")) &&
                !directSiteUrl.substringAfterLast('/').all { it.isDigit() } ->
                    directSiteUrl.replace("kitsu.io", "kitsu.app")
            externalSource.equals("Kitsu", ignoreCase = true) && !externalId.isNullOrBlank() && !externalId.all { it.isDigit() } ->
                "https://kitsu.app/anime/$externalId"
            else -> {
                val malIdParam = directMalId ?: if (externalSource.equals("MAL", ignoreCase = true)) externalId?.toLongOrNull() else null
                val kitsuIdParam = if (externalSource.equals("Kitsu", ignoreCase = true)) externalId?.toLongOrNull() else null
                "kitsu-resolve://anime?title=$encodedTitle&malId=${malIdParam ?: 0}&kitsuId=${kitsuIdParam ?: 0}"
            }
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
        directAniListId: Long? = null,
        directSiteUrl: String? = null,
        webUrl: String? = null
    ): List<ExternalOpenOption> {
        val list = mutableListOf<ExternalOpenOption>()
        val encodedTitle = Uri.encode(title.trim())

        // 1. MangaDex
        val mdUrl = when {
            !directSiteUrl.isNullOrBlank() && directSiteUrl.contains("mangadex.org", ignoreCase = true) -> directSiteUrl
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
            directAniListId != null && directAniListId > 0 -> "https://anilist.co/manga/$directAniListId"
            !directSiteUrl.isNullOrBlank() && directSiteUrl.contains("anilist.co", ignoreCase = true) -> directSiteUrl
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
            !directSiteUrl.isNullOrBlank() &&
                (directSiteUrl.contains("kitsu.io/manga/") || directSiteUrl.contains("kitsu.app/manga/")) &&
                !directSiteUrl.substringAfterLast('/').all { it.isDigit() } ->
                    directSiteUrl.replace("kitsu.io", "kitsu.app")
            externalSource.equals("Kitsu", ignoreCase = true) && !externalId.isNullOrBlank() && !externalId.all { it.isDigit() } ->
                "https://kitsu.app/manga/$externalId"
            else -> {
                val malIdParam = directMalId ?: if (externalSource.equals("MAL", ignoreCase = true)) externalId?.toLongOrNull() else null
                val kitsuIdParam = if (externalSource.equals("Kitsu", ignoreCase = true)) externalId?.toLongOrNull() else null
                "kitsu-resolve://manga?title=$encodedTitle&malId=${malIdParam ?: 0}&kitsuId=${kitsuIdParam ?: 0}"
            }
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
        directAniListId: Long? = null,
        directSiteUrl: String? = null,
        webUrl: String? = null
    ): List<ExternalOpenOption> {
        val list = mutableListOf<ExternalOpenOption>()
        val encodedTitle = Uri.encode(title.trim())

        // 1. AniList
        val aniUrl = when {
            directAniListId != null && directAniListId > 0 -> "https://anilist.co/manga/$directAniListId"
            !directSiteUrl.isNullOrBlank() && directSiteUrl.contains("anilist.co", ignoreCase = true) -> directSiteUrl
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
            !directSiteUrl.isNullOrBlank() && directSiteUrl.contains("mangadex.org", ignoreCase = true) -> directSiteUrl
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
