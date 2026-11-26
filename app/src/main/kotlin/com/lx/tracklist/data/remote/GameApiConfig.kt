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

    private fun readPrefString(context: Context, key: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(key, null)?.trim().orEmpty()
    }

    private inline fun readBuiltInString(provider: () -> String): String {
        return try {
            provider().trim()
        } catch (_: Throwable) {
            ""
        }
    }

    private inline fun resolveEffectiveKey(
        context: Context?,
        prefKey: String,
        builtInProvider: () -> String
    ): String {
        if (context != null) {
            val custom = readPrefString(context, prefKey)
            if (custom.isNotBlank()) return custom
        }
        return readBuiltInString(builtInProvider)
    }

    private inline fun resolveSlotState(
        context: Context?,
        prefKey: String,
        builtInProvider: () -> String
    ): ApiKeyState {
        val custom = context?.let { readPrefString(it, prefKey) }
        return resolveApiKeyState(custom, readBuiltInString(builtInProvider))
    }

    fun getCustomIgdbClientId(context: Context): String =
        readPrefString(context, KEY_CUSTOM_IGDB_CLIENT_ID)

    fun getCustomIgdbClientSecret(context: Context): String =
        readPrefString(context, KEY_CUSTOM_IGDB_CLIENT_SECRET)

    fun getCustomRawgApiKey(context: Context): String =
        readPrefString(context, KEY_CUSTOM_RAWG_API_KEY)

    fun getCustomSteamGridDbApiKey(context: Context): String =
        readPrefString(context, KEY_CUSTOM_STEAMGRIDDB_API_KEY)

    fun getIgdbClientId(context: Context? = null): String =
        resolveEffectiveKey(context, KEY_CUSTOM_IGDB_CLIENT_ID) { BuildConfig.IGDB_CLIENT_ID }

    fun getIgdbClientSecret(context: Context? = null): String =
        resolveEffectiveKey(context, KEY_CUSTOM_IGDB_CLIENT_SECRET) { BuildConfig.IGDB_CLIENT_SECRET }

    fun getRawgApiKey(context: Context? = null): String =
        resolveEffectiveKey(context, KEY_CUSTOM_RAWG_API_KEY) { BuildConfig.RAWG_API_KEY }

    fun getSteamGridDbApiKey(context: Context? = null): String =
        resolveEffectiveKey(context, KEY_CUSTOM_STEAMGRIDDB_API_KEY) { BuildConfig.STEAMGRIDDB_API_KEY }

    fun getIgdbClientIdState(context: Context? = null): ApiKeyState =
        resolveSlotState(context, KEY_CUSTOM_IGDB_CLIENT_ID) { BuildConfig.IGDB_CLIENT_ID }

    fun getIgdbClientSecretState(context: Context? = null): ApiKeyState =
        resolveSlotState(context, KEY_CUSTOM_IGDB_CLIENT_SECRET) { BuildConfig.IGDB_CLIENT_SECRET }

    fun getRawgApiKeyState(context: Context? = null): ApiKeyState =
        resolveSlotState(context, KEY_CUSTOM_RAWG_API_KEY) { BuildConfig.RAWG_API_KEY }

    fun getSteamGridDbApiKeyState(context: Context? = null): ApiKeyState =
        resolveSlotState(context, KEY_CUSTOM_STEAMGRIDDB_API_KEY) { BuildConfig.STEAMGRIDDB_API_KEY }

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
