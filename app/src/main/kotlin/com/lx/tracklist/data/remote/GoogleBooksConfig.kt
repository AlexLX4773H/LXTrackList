package com.lx.tracklist.data.remote

import android.content.Context
import androidx.core.content.edit
import com.lx.tracklist.BuildConfig

object GoogleBooksConfig {
    private const val PREFS_NAME = "lx_google_books_config"
    private const val KEY_CUSTOM_API_KEY = "custom_google_books_api_key"

    fun getCustomApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_API_KEY, null)?.trim().orEmpty()
    }

    fun getBuiltInApiKey(): String {
        return try {
            BuildConfig.GOOGLE_BOOKS_API_KEY.trim()
        } catch (_: Throwable) {
            ""
        }
    }

    fun getApiKey(context: Context? = null): String {
        if (context != null) {
            val custom = getCustomApiKey(context)
            if (custom.isNotBlank()) {
                return custom
            }
        }
        return getBuiltInApiKey()
    }

    fun getApiKeyState(context: Context? = null): ApiKeyState {
        val custom = context?.let { getCustomApiKey(it) }
        return resolveApiKeyState(custom, getBuiltInApiKey())
    }

    fun isConfigured(context: Context? = null): Boolean {
        return getApiKey(context).isNotBlank()
    }

    fun setCustomApiKey(context: Context, key: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            if (key.isNullOrBlank()) {
                remove(KEY_CUSTOM_API_KEY)
            } else {
                putString(KEY_CUSTOM_API_KEY, key.trim())
            }
        }
    }
}

