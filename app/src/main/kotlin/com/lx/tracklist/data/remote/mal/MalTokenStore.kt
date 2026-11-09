package com.lx.tracklist.data.remote.mal

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class MalTokenStore(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "lx_mal_auth_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w(TAG, "EncryptedSharedPreferences unavailable, falling back to standard prefs", e)
        context.getSharedPreferences("lx_mal_auth_prefs", Context.MODE_PRIVATE)
    }

    // Temporary storage for OAuth state & PKCE code_verifier while authentication is in progress
    var pendingCodeVerifier: String?
        get() = prefs.getString("pending_code_verifier", null)
        set(value) = prefs.edit().putString("pending_code_verifier", value).apply()

    var pendingAuthUserId: Long
        get() = prefs.getLong("pending_auth_user_id", 1L)
        set(value) = prefs.edit().putLong("pending_auth_user_id", value).apply()

    fun getAccessToken(userId: Long): String? =
        prefs.getString("access_token_$userId", null)

    fun setAccessToken(userId: Long, token: String?) =
        prefs.edit().putString("access_token_$userId", token).apply()

    fun getRefreshToken(userId: Long): String? =
        prefs.getString("refresh_token_$userId", null)

    fun setRefreshToken(userId: Long, token: String?) =
        prefs.edit().putString("refresh_token_$userId", token).apply()

    fun getExpiresAt(userId: Long): Long =
        prefs.getLong("expires_at_$userId", 0L)

    fun setExpiresAt(userId: Long, expiresAt: Long) =
        prefs.edit().putLong("expires_at_$userId", expiresAt).apply()

    fun getMalUsername(userId: Long): String? =
        prefs.getString("mal_username_$userId", null)

    fun setMalUsername(userId: Long, username: String?) =
        prefs.edit().putString("mal_username_$userId", username).apply()

    fun getMalPictureUrl(userId: Long): String? =
        prefs.getString("mal_picture_url_$userId", null)

    fun setMalPictureUrl(userId: Long, url: String?) =
        prefs.edit().putString("mal_picture_url_$userId", url).apply()

    fun isAutoSyncEnabled(userId: Long): Boolean =
        prefs.getBoolean("auto_sync_enabled_$userId", true)

    fun setAutoSyncEnabled(userId: Long, enabled: Boolean) =
        prefs.edit().putBoolean("auto_sync_enabled_$userId", enabled).apply()

    fun getLastSyncTimestamp(userId: Long): Long =
        prefs.getLong("last_sync_timestamp_$userId", 0L)

    fun setLastSyncTimestamp(userId: Long, timestamp: Long) =
        prefs.edit().putLong("last_sync_timestamp_$userId", timestamp).apply()

    fun isLoggedIn(userId: Long): Boolean =
        !getAccessToken(userId).isNullOrBlank()

    fun clear(userId: Long) {
        prefs.edit()
            .remove("access_token_$userId")
            .remove("refresh_token_$userId")
            .remove("expires_at_$userId")
            .remove("mal_username_$userId")
            .remove("mal_picture_url_$userId")
            .remove("auto_sync_enabled_$userId")
            .remove("last_sync_timestamp_$userId")
            .apply()
    }

    companion object {
        private const val TAG = "MalTokenStore"
    }
}

