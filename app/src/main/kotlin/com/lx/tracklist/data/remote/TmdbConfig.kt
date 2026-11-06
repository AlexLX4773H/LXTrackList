package com.lx.tracklist.data.remote

import android.content.Context
import androidx.core.content.edit
import com.lx.tracklist.BuildConfig

object TmdbConfig {
    private const val PREFS_NAME = "lx_tmdb_config"
    private const val KEY_CUSTOM_TOKEN = "custom_tmdb_token"

    // Fallback embedded token in case BuildConfig is empty or not yet generated
    private const val FALLBACK_READ_ACCESS_TOKEN =
        "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIxZTQ4OGE2MGQ0MDYyMzBhNjM5NWRiOTUzZDZhODU0NSIsIm5iZiI6MTc5Mzk1MzM4NS44NDUsInN1YiI6IjZhZWQ4ZTY5M2JhMGY1NThkMTNkMGI5YSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.qidQGBJhbJ-5DyHQqp4HMV_F4hcPs4LUs-A85cQz9sI"

    const val ATTRIBUTION_TEXT = "This product uses the TMDB API but is not endorsed or certified by TMDB."

    fun getAccessToken(context: Context? = null): String {
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val custom = prefs.getString(KEY_CUSTOM_TOKEN, null)?.trim()
            if (!custom.isNullOrBlank()) {
                return custom
            }
        }
        val buildConfigToken = try {
            BuildConfig.TMDB_READ_ACCESS_TOKEN.trim()
        } catch (_: Throwable) {
            ""
        }
        return if (buildConfigToken.isNotBlank()) buildConfigToken else FALLBACK_READ_ACCESS_TOKEN
    }

    fun isConfigured(context: Context? = null): Boolean {
        return getAccessToken(context).isNotBlank()
    }

    fun setCustomAccessToken(context: Context, token: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            if (token.isNullOrBlank()) {
                remove(KEY_CUSTOM_TOKEN)
            } else {
                putString(KEY_CUSTOM_TOKEN, token.trim())
            }
        }
    }
}

