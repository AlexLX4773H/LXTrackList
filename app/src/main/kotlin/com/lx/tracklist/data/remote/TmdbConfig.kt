package com.lx.tracklist.data.remote

import android.content.Context
import androidx.core.content.edit
import com.lx.tracklist.BuildConfig

object TmdbConfig {
    private const val PREFS_NAME = "lx_tmdb_config"
    private const val KEY_CUSTOM_TOKEN = "custom_tmdb_token"

    // Fallback embedded token - keep empty in public repository; inject via .env or GitHub Secrets
    private const val FALLBACK_READ_ACCESS_TOKEN = ""

    const val ATTRIBUTION_TEXT = "This product uses the TMDB API but is not endorsed or certified by TMDB."

    fun getCustomAccessToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_TOKEN, null)?.trim().orEmpty()
    }

    fun getBuiltInAccessToken(): String {
        val buildConfigToken = try {
            BuildConfig.TMDB_READ_ACCESS_TOKEN.trim()
        } catch (_: Throwable) {
            ""
        }
        return if (buildConfigToken.isNotBlank()) buildConfigToken else FALLBACK_READ_ACCESS_TOKEN
    }

    fun getAccessToken(context: Context? = null): String {
        if (context != null) {
            val custom = getCustomAccessToken(context)
            if (custom.isNotBlank()) {
                return custom
            }
        }
        return getBuiltInAccessToken()
    }

    fun getAccessTokenState(context: Context? = null): ApiKeyState {
        val custom = context?.let { getCustomAccessToken(it) }
        return resolveApiKeyState(custom, getBuiltInAccessToken())
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

