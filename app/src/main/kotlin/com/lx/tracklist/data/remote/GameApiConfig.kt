package com.lx.tracklist.data.remote

import android.content.Context
import androidx.core.content.edit
import com.lx.tracklist.BuildConfig

object GameApiConfig {
    private const val PREFS_NAME = "lx_game_api_config"

    private const val KEY_CUSTOM_IGDB_CLIENT_ID = "custom_igdb_client_id"
    private const val KEY_CUSTOM_IGDB_CLIENT_SECRET = "custom_igdb_client_secret"
    private const val KEY_CUSTOM_RAWG_API_KEY = "custom_rawg_api_key"
    private const val KEY_CUSTOM_STEAMGRIDDB_API_KEY = "custom_steamgriddb_api_key"

    private const val KEY_CACHED_TWITCH_TOKEN = "cached_twitch_token"
    private const val KEY_CACHED_TWITCH_TOKEN_EXPIRY = "cached_twitch_token_expiry"

    const val RAWG_ATTRIBUTION_TEXT = "Game data powered by RAWG"
    const val IGDB_ATTRIBUTION_TEXT = "Game metadata and cover art provided by IGDB"

    fun getIgdbClientId(context: Context? = null): String {
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val custom = prefs.getString(KEY_CUSTOM_IGDB_CLIENT_ID, null)?.trim()
            if (!custom.isNullOrBlank()) return custom
        }
        return try {
            BuildConfig.IGDB_CLIENT_ID.trim()
        } catch (_: Throwable) {
            ""
        }
    }

    fun getIgdbClientSecret(context: Context? = null): String {
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val custom = prefs.getString(KEY_CUSTOM_IGDB_CLIENT_SECRET, null)?.trim()
            if (!custom.isNullOrBlank()) return custom
        }
        return try {
            BuildConfig.IGDB_CLIENT_SECRET.trim()
        } catch (_: Throwable) {
            ""
        }
    }

    fun getRawgApiKey(context: Context? = null): String {
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val custom = prefs.getString(KEY_CUSTOM_RAWG_API_KEY, null)?.trim()
            if (!custom.isNullOrBlank()) return custom
        }
        return try {
            BuildConfig.RAWG_API_KEY.trim()
        } catch (_: Throwable) {
            ""
        }
    }

    fun getSteamGridDbApiKey(context: Context? = null): String {
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val custom = prefs.getString(KEY_CUSTOM_STEAMGRIDDB_API_KEY, null)?.trim()
            if (!custom.isNullOrBlank()) return custom
        }
        return try {
            BuildConfig.STEAMGRIDDB_API_KEY.trim()
        } catch (_: Throwable) {
            ""
        }
    }

    fun isIgdbConfigured(context: Context? = null): Boolean {
        return getIgdbClientId(context).isNotBlank() && getIgdbClientSecret(context).isNotBlank()
    }

    fun isRawgConfigured(context: Context? = null): Boolean {
        return getRawgApiKey(context).isNotBlank()
    }

    fun isSteamGridDbConfigured(context: Context? = null): Boolean {
        return getSteamGridDbApiKey(context).isNotBlank()
    }

    fun setCustomIgdbCredentials(context: Context, clientId: String?, clientSecret: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            if (clientId.isNullOrBlank()) remove(KEY_CUSTOM_IGDB_CLIENT_ID)
            else putString(KEY_CUSTOM_IGDB_CLIENT_ID, clientId.trim())

            if (clientSecret.isNullOrBlank()) remove(KEY_CUSTOM_IGDB_CLIENT_SECRET)
            else putString(KEY_CUSTOM_IGDB_CLIENT_SECRET, clientSecret.trim())

            // Invalidate cached token when credentials change
            remove(KEY_CACHED_TWITCH_TOKEN)
            remove(KEY_CACHED_TWITCH_TOKEN_EXPIRY)
        }
    }

    fun setCustomRawgApiKey(context: Context, key: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            if (key.isNullOrBlank()) remove(KEY_CUSTOM_RAWG_API_KEY)
            else putString(KEY_CUSTOM_RAWG_API_KEY, key.trim())
        }
    }

    fun setCustomSteamGridDbApiKey(context: Context, key: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            if (key.isNullOrBlank()) remove(KEY_CUSTOM_STEAMGRIDDB_API_KEY)
            else putString(KEY_CUSTOM_STEAMGRIDDB_API_KEY, key.trim())
        }
    }

    fun getCachedTwitchToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(KEY_CACHED_TWITCH_TOKEN, null)
        val expiry = prefs.getLong(KEY_CACHED_TWITCH_TOKEN_EXPIRY, 0L)
        // Check if token has at least 5 minutes remaining
        if (!token.isNullOrBlank() && expiry > System.currentTimeMillis() + (5 * 60 * 1000L)) {
            return token
        }
        return null
    }

    fun setCachedTwitchToken(context: Context, token: String, expiresInSeconds: Long) {
        val expiryMillis = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_CACHED_TWITCH_TOKEN, token)
            putLong(KEY_CACHED_TWITCH_TOKEN_EXPIRY, expiryMillis)
        }
    }

    fun clearCachedTwitchToken(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(KEY_CACHED_TWITCH_TOKEN)
            remove(KEY_CACHED_TWITCH_TOKEN_EXPIRY)
        }
    }
}
