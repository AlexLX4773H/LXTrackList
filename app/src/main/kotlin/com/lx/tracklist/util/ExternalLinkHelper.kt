package com.lx.tracklist.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import com.google.gson.JsonParser
import com.lx.tracklist.data.remote.ITunesApiClient
import com.lx.tracklist.data.remote.KitsuApiClient
import com.lx.tracklist.data.remote.MusicBrainzApiClient
import com.lx.tracklist.data.remote.MusicMetadataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

object ExternalLinkHelper {

    private data class SpotifyLookupHint(
        val title: String,
        val artist: String?,
        val album: String?,
        val externalId: String?,
        val externalSource: String?,
        val webUrl: String?
    )

    private val kitsuCache = ConcurrentHashMap<String, String>()
    private val spotifyHintCache = ConcurrentHashMap<String, SpotifyLookupHint>()
    private val spotifyTrackCache = ConcurrentHashMap<String, String>()

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

        // Intercept direct Spotify track links/URIs so Spotify app opens cleanly when installed
        val directSpotifyTrackId = if (
            url.contains("open.spotify.com", ignoreCase = true) ||
            url.startsWith("spotify:track:", ignoreCase = true)
        ) {
            extractSpotifyTrackId(url)
        } else null
        if (directSpotifyTrackId != null) {
            openSpotifyTrack(directSpotifyTrackId, uriHandler, context, clipboardManager)
            return
        }

        // Intercept Spotify search/resolve URLs: Spotify's mobile web SPA ignores /search/<query>,
        // so resolve to a direct track page first, then fall back to Spotify app search or web track search.
        if (url.startsWith("spotify-resolve://", ignoreCase = true) ||
            url.startsWith("https://open.spotify.com/search/", ignoreCase = true) ||
            url.startsWith("http://open.spotify.com/search/", ignoreCase = true)
        ) {
            resolveAndOpenSpotify(url, uriHandler, context, clipboardManager)
            return
        }

        openExternalUri(url, uriHandler, context, clipboardManager)
    }

    private fun openExternalUri(
        url: String,
        uriHandler: UriHandler,
        context: Context,
        clipboardManager: ClipboardManager?
    ) {
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

    private fun safeUrlEncode(text: String): String {
        return try {
            Uri.encode(text)
        } catch (_: Throwable) {
            java.net.URLEncoder.encode(text, "UTF-8")
        }
    }

    fun buildGameOpenOptions(
        title: String,
        externalId: String?,
        externalSource: String?,
        webUrl: String? = null
    ): List<ExternalOpenOption> {
        val list = mutableListOf<ExternalOpenOption>()
        val encodedTitle = safeUrlEncode(title.trim())

        // 1. IGDB
        val igdbUrl = when {
            externalSource.equals("IGDB", ignoreCase = true) && !externalId.isNullOrBlank() ->
                webUrl ?: "https://www.igdb.com/games/$externalId"
            webUrl != null && webUrl.contains("igdb.com", ignoreCase = true) -> webUrl
            else -> "https://www.igdb.com/search?utf8=✓&q=$encodedTitle"
        }
        list.add(
            ExternalOpenOption(
                title = "IGDB",
                subtitle = "igdb.com",
                url = igdbUrl,
                color = 0xFF9146FF
            )
        )

        // 2. RAWG
        val rawgUrl = when {
            externalSource.equals("RAWG", ignoreCase = true) && !externalId.isNullOrBlank() ->
                webUrl ?: "https://rawg.io/games/$externalId"
            webUrl != null && webUrl.contains("rawg.io", ignoreCase = true) -> webUrl
            else -> "https://rawg.io/search?query=$encodedTitle"
        }
        list.add(
            ExternalOpenOption(
                title = "RAWG",
                subtitle = "rawg.io",
                url = rawgUrl,
                color = 0xFF000000
            )
        )

        // 3. Steam Store
        list.add(
            ExternalOpenOption(
                title = "Steam Store",
                subtitle = "store.steampowered.com",
                url = "https://store.steampowered.com/search/?term=$encodedTitle",
                color = 0xFF171A21
            )
        )

        // 4. SteamGridDB
        list.add(
            ExternalOpenOption(
                title = "SteamGridDB",
                subtitle = "steamgriddb.com",
                url = "https://www.steamgriddb.com/search/grids?term=$encodedTitle",
                color = 0xFF1B2838
            )
        )

        return list
    }

    private const val SPOTIFY_PACKAGE = "com.spotify.music"
    private val SPOTIFY_TRACK_ID_REGEX = Regex(
        """(?:open\.spotify\.com/(?:intl-[^/]+/)?track/|spotify:track:)([A-Za-z0-9]{22})""",
        RegexOption.IGNORE_CASE
    )
    private val ITUNES_URL_TRACK_ID_REGEX = Regex("""[?&]i=(\d+)""")
    private val APPLE_SONG_PATH_ID_REGEX = Regex("""/song/(?:[^/]+/)?(\d+)""")

    internal fun extractSpotifyTrackId(urlOrUri: String?): String? {
        if (urlOrUri.isNullOrBlank()) return null
        val normalized = urlOrUri
            .replace("\\/", "/")
            .replace("%2f", "/", ignoreCase = true)
            .replace("%3a", ":", ignoreCase = true)
        return SPOTIFY_TRACK_ID_REGEX.find(normalized)?.groupValues?.getOrNull(1)
    }

    internal fun extractItunesTrackIdFromAppleUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return ITUNES_URL_TRACK_ID_REGEX.find(url)?.groupValues?.getOrNull(1)
            ?: APPLE_SONG_PATH_ID_REGEX.find(url)?.groupValues?.getOrNull(1)
    }

    internal fun parseSpotifyTrackIdFromOdesliJson(body: String): String? {
        return try {
            val root = JsonParser.parseString(body).asJsonObject
            val linksByPlatform = root.getAsJsonObject("linksByPlatform") ?: return null
            val spotifyObj = linksByPlatform.getAsJsonObject("spotify") ?: return null
            val spotifyUrl = spotifyObj.get("url")?.takeIf { !it.isJsonNull }?.asString
            val mobileUri = spotifyObj.get("nativeAppUriMobile")?.takeIf { !it.isJsonNull }?.asString
            val desktopUri = spotifyObj.get("nativeAppUriDesktop")?.takeIf { !it.isJsonNull }?.asString
            val entityId = spotifyObj.get("entityUniqueId")?.takeIf { !it.isJsonNull }?.asString
            extractSpotifyTrackId(spotifyUrl)
                ?: extractSpotifyTrackId(mobileUri)
                ?: extractSpotifyTrackId(desktopUri)
                ?: entityId
                    ?.takeIf { it.startsWith("SPOTIFY_SONG::") }
                    ?.substringAfter("SPOTIFY_SONG::")
                    ?.takeIf { it.length == 22 && it.all(Char::isLetterOrDigit) }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildSpotifyCacheKey(title: String, artist: String?): String {
        val a = artist?.trim()?.lowercase().orEmpty()
        val t = title.trim().lowercase()
        return "$a|$t"
    }

    private fun <K, V> ConcurrentHashMap<K, V>.putBounded(key: K, value: V, maxSize: Int = 200) {
        if (size >= maxSize && !containsKey(key)) {
            clear()
        }
        put(key, value)
    }

    private fun parseSpotifyResolveUrl(url: String): SpotifyLookupHint? {
        val queryPart = url.substringAfter('?', "")
        if (queryPart.isBlank()) return null
        val params = mutableMapOf<String, String>()
        queryPart.split('&').forEach { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = pair.substring(0, idx)
                val value = try {
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                } catch (_: Exception) {
                    pair.substring(idx + 1)
                }
                if (value.isNotBlank()) params[key] = value
            }
        }
        val title = params["title"] ?: return null
        return SpotifyLookupHint(
            title = title,
            artist = params["artist"],
            album = params["album"],
            externalId = params["externalId"],
            externalSource = params["externalSource"],
            webUrl = params["webUrl"]
        )
    }

    private fun resolveAndOpenSpotify(
        url: String,
        uriHandler: UriHandler,
        context: Context,
        clipboardManager: ClipboardManager?
    ) {
        val hintFromResolve = if (url.startsWith("spotify-resolve://", ignoreCase = true)) {
            parseSpotifyResolveUrl(url)
        } else null

        val queryFromSearchUrl = url.substringAfter("/search/", "")
            .substringBefore("?")
            .takeIf { !url.startsWith("spotify-resolve://", ignoreCase = true) && it.isNotBlank() }
            ?.let { raw ->
                try {
                    URLDecoder.decode(raw, "UTF-8")
                } catch (_: Exception) {
                    raw
                }
            }

        val hint = hintFromResolve
            ?: spotifyHintCache[url]
            ?: queryFromSearchUrl?.let { spotifyHintCache[it.trim().lowercase()] }

        val query = listOfNotNull(
            hint?.artist?.trim()?.takeIf { it.isNotBlank() },
            hint?.title?.trim()?.takeIf { it.isNotBlank() }
        ).joinToString(" ").ifBlank {
            queryFromSearchUrl?.trim().orEmpty()
        }

        if (query.isBlank() && hint == null) {
            openExternalUri(url, uriHandler, context, clipboardManager)
            return
        }

        val cacheKey = buildSpotifyCacheKey(hint?.title ?: query, hint?.artist)
        val cachedTrackId = spotifyTrackCache[cacheKey]
        if (cachedTrackId != null) {
            openSpotifyTrack(cachedTrackId, uriHandler, context, clipboardManager)
            return
        }

        Toast.makeText(context, "Finding Spotify track...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            val resolvedTrackId = resolveSpotifyTrackId(hint, query)
            if (resolvedTrackId != null) {
                spotifyTrackCache.putBounded(cacheKey, resolvedTrackId)
            }
            withContext(Dispatchers.Main) {
                if (resolvedTrackId != null) {
                    openSpotifyTrack(resolvedTrackId, uriHandler, context, clipboardManager)
                } else {
                    openSpotifySearchFallback(query, uriHandler, context, clipboardManager)
                }
            }
        }
    }

    private suspend fun resolveSpotifyTrackId(hint: SpotifyLookupHint?, query: String): String? {
        if (query.isBlank() && hint == null) return null
        return withTimeoutOrNull(7500L) {
            // 1. Direct Spotify URL if already stored in metadata
            extractSpotifyTrackId(hint?.webUrl)?.let { return@withTimeoutOrNull it }

            // 2. MusicBrainz URL relationships (url-rels)
            val mbId = hint?.externalId?.takeIf {
                hint.externalSource.equals("MusicBrainz", ignoreCase = true) && it.isNotBlank()
            }
            if (mbId != null) {
                val mbSpotifyUrl = try {
                    MusicBrainzApiClient.lookupSpotifyUrlForRecording(mbId)
                } catch (_: Exception) {
                    null
                }
                extractSpotifyTrackId(mbSpotifyUrl)?.let { return@withTimeoutOrNull it }
            }

            // 3. Odesli / Songlink cross-platform track matching
            val directAppleUrl = hint?.webUrl?.takeIf {
                it.contains("music.apple.com", ignoreCase = true) ||
                    it.contains("itunes.apple.com", ignoreCase = true)
            }
            var itunesTrackId: String? = when {
                (hint?.externalSource?.contains("iTunes", ignoreCase = true) == true ||
                    hint?.externalSource?.contains("Apple", ignoreCase = true) == true) &&
                    hint.externalId?.all { it.isDigit() } == true -> hint.externalId
                else -> extractItunesTrackIdFromAppleUrl(directAppleUrl)
            }
            var appleUrl = directAppleUrl

            if (itunesTrackId == null && appleUrl == null && query.isNotBlank()) {
                val candidates = ITunesApiClient.searchTracks(query = query, limit = 8).getOrNull().orEmpty()
                val best = MusicMetadataManager.pickBestMusicMatch(
                    targetTitle = hint?.title?.takeIf { it.isNotBlank() } ?: query,
                    targetArtist = hint?.artist,
                    targetAlbum = hint?.album,
                    candidates = candidates
                )
                if (best != null) {
                    itunesTrackId = best.externalId.takeIf { it.isNotBlank() && it.all(Char::isDigit) }
                    appleUrl = best.webUrl
                }
            }

            fetchSpotifyTrackIdFromOdesli(itunesTrackId, appleUrl)?.let { return@withTimeoutOrNull it }

            // 4. DuckDuckGo HTML fallback for site:open.spotify.com/track <query>
            if (query.isNotBlank()) {
                fetchSpotifyTrackIdFromDuckDuckGo(query)?.let { return@withTimeoutOrNull it }
            }

            null
        }
    }

    private fun fetchSpotifyTrackIdFromOdesli(itunesTrackId: String?, appleMusicUrl: String?): String? {
        if (!itunesTrackId.isNullOrBlank()) {
            val byIdUrl = "https://api.song.link/v1-alpha.1/links?platform=itunes&type=song&id=$itunesTrackId"
            httpGetString(byIdUrl, timeoutMs = 4500)
                ?.let { parseSpotifyTrackIdFromOdesliJson(it) }
                ?.let { return it }
        }
        if (!appleMusicUrl.isNullOrBlank()) {
            val encodedAppleUrl = safeUrlEncode(appleMusicUrl)
            val byUrl = "https://api.song.link/v1-alpha.1/links?url=$encodedAppleUrl"
            httpGetString(byUrl, timeoutMs = 4500)
                ?.let { parseSpotifyTrackIdFromOdesliJson(it) }
                ?.let { return it }
        }
        return null
    }

    private fun fetchSpotifyTrackIdFromDuckDuckGo(query: String): String? {
        val searchQuery = "site:open.spotify.com/track $query"
        val url = "https://html.duckduckgo.com/html/?q=${safeUrlEncode(searchQuery)}"
        val html = httpGetString(
            urlStr = url,
            timeoutMs = 4500,
            userAgent = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
            maxChars = 131072
        ) ?: return null
        return extractSpotifyTrackId(html)
    }

    private fun httpGetString(
        urlStr: String,
        timeoutMs: Int = 4500,
        userAgent: String = "LXTrackList/1.0",
        maxChars: Int = 262144
    ): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as? HttpURLConnection) ?: return null
            conn.requestMethod = "GET"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", userAgent)
            conn.setRequestProperty("Accept", "application/json,text/html,*/*")
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { reader ->
                val buffer = CharArray(8192)
                val sb = StringBuilder()
                var total = 0
                while (total < maxChars) {
                    val read = reader.read(buffer)
                    if (read <= 0) break
                    sb.append(buffer, 0, minOf(read, maxChars - total))
                    total += read
                }
                sb.toString()
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun openSpotifyTrack(
        trackId: String,
        uriHandler: UriHandler,
        context: Context,
        clipboardManager: ClipboardManager?
    ) {
        if (tryOpenSpotifyAppUri("spotify:track:$trackId", context)) return
        openExternalUri("https://open.spotify.com/track/$trackId", uriHandler, context, clipboardManager)
    }

    private fun openSpotifySearchFallback(
        query: String,
        uriHandler: UriHandler,
        context: Context,
        clipboardManager: ClipboardManager?
    ) {
        if (query.isNotBlank()) {
            val appSearchUri = "spotify:search:${encodePercentQuery(query)}"
            if (tryOpenSpotifyAppUri(appSearchUri, context)) return
        }
        val googleQuery = if (query.isNotBlank()) "site:open.spotify.com/track $query" else "site:open.spotify.com/track"
        openExternalUri("https://www.google.com/search?q=${safeUrlEncode(googleQuery)}", uriHandler, context, clipboardManager)
    }

    private fun tryOpenSpotifyAppUri(spotifyUri: String, context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUri)).apply {
                setPackage(SPOTIFY_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun encodePercentQuery(query: String): String {
        return try {
            Uri.encode(query)
        } catch (_: Exception) {
            URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        }
    }

    fun buildMusicOpenOptions(
        title: String,
        artist: String?,
        album: String?,
        externalId: String?,
        externalSource: String?,
        webUrl: String? = null
    ): List<ExternalOpenOption> {
        val list = mutableListOf<ExternalOpenOption>()
        val trimmedTitle = title.trim()
        val trimmedArtist = artist?.trim()?.takeIf { it.isNotBlank() }
        val query = listOfNotNull(trimmedArtist, trimmedTitle).filter { it.isNotBlank() }.joinToString(" ")
        val encodedQuery = safeUrlEncode(query)

        // 1. MusicBrainz
        val mbUrl = when {
            externalSource.equals("MusicBrainz", ignoreCase = true) && !externalId.isNullOrBlank() ->
                webUrl ?: "https://musicbrainz.org/recording/$externalId"
            webUrl != null && webUrl.contains("musicbrainz.org", ignoreCase = true) -> webUrl
            else -> "https://musicbrainz.org/search?query=$encodedQuery&type=recording"
        }
        list.add(
            ExternalOpenOption(
                title = "MusicBrainz 🎵",
                subtitle = "musicbrainz.org",
                url = mbUrl,
                color = 0xFFBA478F
            )
        )

        // 2. Apple Music
        val appleMusicUrl = when {
            (externalSource?.contains("Apple", ignoreCase = true) == true || externalSource?.contains("iTunes", ignoreCase = true) == true) && !webUrl.isNullOrBlank() -> webUrl
            else -> "https://music.apple.com/us/search?term=$encodedQuery"
        }
        list.add(
            ExternalOpenOption(
                title = "Apple Music 🍎",
                subtitle = "music.apple.com",
                url = appleMusicUrl,
                color = 0xFFFC3C44
            )
        )

        // 3. Spotify
        val cacheKey = buildSpotifyCacheKey(trimmedTitle, trimmedArtist)
        val knownTrackId = extractSpotifyTrackId(webUrl) ?: spotifyTrackCache[cacheKey]
        val spotifySearchUrl = "https://open.spotify.com/search/$encodedQuery"
        val spotifyUrl = if (knownTrackId != null) {
            "https://open.spotify.com/track/$knownTrackId"
        } else {
            val hint = SpotifyLookupHint(
                title = trimmedTitle,
                artist = trimmedArtist,
                album = album?.trim()?.takeIf { it.isNotBlank() },
                externalId = externalId?.trim()?.takeIf { it.isNotBlank() },
                externalSource = externalSource?.trim()?.takeIf { it.isNotBlank() },
                webUrl = webUrl?.trim()?.takeIf { it.isNotBlank() }
            )
            spotifyHintCache.putBounded(spotifySearchUrl, hint)
            if (query.isNotBlank()) {
                spotifyHintCache.putBounded(query.lowercase(), hint)
            }
            spotifySearchUrl
        }
        list.add(
            ExternalOpenOption(
                title = "Spotify 🎧",
                subtitle = if (knownTrackId != null) "open.spotify.com/track" else "open.spotify.com",
                url = spotifyUrl,
                color = 0xFF1DB954
            )
        )

        // 4. YouTube Music
        list.add(
            ExternalOpenOption(
                title = "YouTube Music ▶️",
                subtitle = "music.youtube.com",
                url = "https://music.youtube.com/search?q=$encodedQuery",
                color = 0xFFFF0000
            )
        )

        return list
    }
}
