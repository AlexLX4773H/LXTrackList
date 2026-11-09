package com.lx.tracklist.data.remote.mal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.Gson
import com.lx.tracklist.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

object MalAuthManager {

    private const val TAG = "MalAuthManager"
    const val REDIRECT_URI = "lxtracklist://mal-oauth"
    private const val AUTH_ENDPOINT = "https://myanimelist.net/v1/oauth2/authorize"
    private const val TOKEN_ENDPOINT = "https://myanimelist.net/v1/oauth2/token"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun getClientId(): String = BuildConfig.MAL_CLIENT_ID.trim()

    fun isConfigured(): Boolean = getClientId().isNotBlank()

    /**
     * Generates a random PKCE code verifier between 43 and 128 chars.
     */
    private fun generateCodeVerifier(): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        val random = SecureRandom()
        return (1..128).map { allowedChars[random.nextInt(allowedChars.length)] }.joinToString("")
    }

    /**
     * Starts the OAuth2 flow by opening Chrome Custom Tabs.
     */
    fun startOAuth(context: Context, userId: Long): Result<Unit> {
        val clientId = getClientId()
        if (clientId.isBlank()) {
            return Result.failure(IllegalStateException("MAL_CLIENT_ID is not configured in .env"))
        }

        val codeVerifier = generateCodeVerifier()
        val tokenStore = MalTokenStore(context)
        tokenStore.pendingCodeVerifier = codeVerifier
        tokenStore.pendingAuthUserId = userId

        val authUri = Uri.parse(AUTH_ENDPOINT).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("code_challenge", codeVerifier)
            .appendQueryParameter("code_challenge_method", "plain") // MAL requires plain
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .build()

        return try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            customTabsIntent.launchUrl(context, authUri)
            Result.success(Unit)
        } catch (e: Exception) {
            // Fallback to standard browser if Custom Tabs fails
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, authUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                Result.success(Unit)
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Failed to launch browser for MAL OAuth", fallbackEx)
                Result.failure(fallbackEx)
            }
        }
    }

    /**
     * Exchanges the authorization code received from redirect for access & refresh tokens.
     * Returns Pair(username, userId)
     */
    suspend fun handleAuthCallback(context: Context, authorizationCode: String): Result<Pair<String, Long>> =
        withContext(Dispatchers.IO) {
            val clientId = getClientId()
            val tokenStore = MalTokenStore(context)
            val codeVerifier = tokenStore.pendingCodeVerifier
            val userId = tokenStore.pendingAuthUserId

            if (codeVerifier.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("No pending code verifier found"))
            }

            val formBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("grant_type", "authorization_code")
                .add("code", authorizationCode.trim())
                .add("code_verifier", codeVerifier)
                .add("redirect_uri", REDIRECT_URI)
                .build()

            val request = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(formBody)
                .build()

            try {
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                    Log.e(TAG, "Token exchange failed [${response.code}]: $responseBody")
                    return@withContext Result.failure(
                        Exception("MAL token exchange failed: HTTP ${response.code}")
                    )
                }

                val tokenResponse = gson.fromJson(responseBody, MalTokenResponse::class.java)
                val expiresAt = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)

                tokenStore.setAccessToken(userId, tokenResponse.accessToken)
                tokenStore.setRefreshToken(userId, tokenResponse.refreshToken)
                tokenStore.setExpiresAt(userId, expiresAt)
                tokenStore.pendingCodeVerifier = null

                // Fetch username and profile picture
                fetchAndStoreUserInfo(context, userId, tokenResponse.accessToken)

                val username = tokenStore.getMalUsername(userId) ?: "MAL User"
                Result.success(Pair(username, userId))
            } catch (e: Exception) {
                Log.e(TAG, "Exception during token exchange", e)
                Result.failure(e)
            }
        }

    /**
     * Refreshes the access token using the stored refresh token.
     */
    suspend fun refreshAccessToken(context: Context, userId: Long): Result<String> =
        withContext(Dispatchers.IO) {
            val clientId = getClientId()
            val tokenStore = MalTokenStore(context)
            val refreshToken = tokenStore.getRefreshToken(userId)

            if (refreshToken.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("No refresh token available for user $userId"))
            }

            val formBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build()

            val request = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(formBody)
                .build()

            try {
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                    Log.e(TAG, "Token refresh failed [${response.code}]: $responseBody")
                    if (response.code == 400 || response.code == 401) {
                        // Refresh token expired / revoked
                        tokenStore.clear(userId)
                    }
                    return@withContext Result.failure(
                        Exception("Token refresh failed: HTTP ${response.code}")
                    )
                }

                val tokenResponse = gson.fromJson(responseBody, MalTokenResponse::class.java)
                val expiresAt = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)

                tokenStore.setAccessToken(userId, tokenResponse.accessToken)
                tokenStore.setRefreshToken(userId, tokenResponse.refreshToken)
                tokenStore.setExpiresAt(userId, expiresAt)

                Result.success(tokenResponse.accessToken)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during token refresh", e)
                Result.failure(e)
            }
        }

    /**
     * Gets a valid access token, auto-refreshing if it is about to expire in the next 5 minutes.
     */
    suspend fun getValidAccessToken(context: Context, userId: Long): String? {
        val tokenStore = MalTokenStore(context)
        val token = tokenStore.getAccessToken(userId) ?: return null
        val expiresAt = tokenStore.getExpiresAt(userId)

        // If expires in less than 5 minutes, refresh proactively
        if (expiresAt > 0 && expiresAt - System.currentTimeMillis() < 5 * 60 * 1000) {
            val refreshResult = refreshAccessToken(context, userId)
            return refreshResult.getOrNull() ?: token
        }

        return token
    }

    private suspend fun fetchAndStoreUserInfo(context: Context, userId: Long, accessToken: String) {
        try {
            val request = Request.Builder()
                .url("https://api.myanimelist.net/v2/users/@me")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && !body.isNullOrBlank()) {
                val profile = gson.fromJson(body, MalUserProfile::class.java)
                val store = MalTokenStore(context)
                store.setMalUsername(userId, profile.name)
                store.setMalPictureUrl(userId, profile.picture)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch MAL user info", e)
        }
    }
}

