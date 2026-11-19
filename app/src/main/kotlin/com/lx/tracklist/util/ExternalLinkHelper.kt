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
}
