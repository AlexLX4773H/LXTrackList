package com.lx.tracklist.data.remote

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class ApiKeyConfigTest {

    private fun fakePrefsContext(): Context {
        val stores = mutableMapOf<String, MutableMap<String, Any?>>()

        fun prefsFor(name: String): SharedPreferences {
            val map = stores.getOrPut(name) { mutableMapOf() }
            lateinit var prefsProxy: SharedPreferences
            val editorProxy = Proxy.newProxyInstance(
                SharedPreferences.Editor::class.java.classLoader,
                arrayOf(SharedPreferences.Editor::class.java)
            ) { proxy, method, args ->
                when (method.name) {
                    "putString" -> {
                        val k = args[0] as String
                        val v = args[1] as? String
                        if (v == null) map.remove(k) else map[k] = v
                        proxy
                    }
                    "putLong" -> {
                        map[args[0] as String] = args[1] as Long
                        proxy
                    }
                    "remove" -> {
                        map.remove(args[0] as String)
                        proxy
                    }
                    "clear" -> {
                        map.clear()
                        proxy
                    }
                    "commit" -> true
                    "apply" -> Unit
                    else -> null
                }
            } as SharedPreferences.Editor

            prefsProxy = Proxy.newProxyInstance(
                SharedPreferences::class.java.classLoader,
                arrayOf(SharedPreferences::class.java)
            ) { _, method, args ->
                when (method.name) {
                    "getString" -> (map[args[0] as String] as? String) ?: (args[1] as? String)
                    "getLong" -> (map[args[0] as String] as? Long) ?: (args[1] as Long)
                    "contains" -> map.containsKey(args[0] as String)
                    "edit" -> editorProxy
                    else -> null
                }
            } as SharedPreferences

            return prefsProxy
        }

        return object : ContextWrapper(null) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
                return prefsFor(name.orEmpty())
            }
        }
    }

    @Test
    fun `resolveApiKeyState prioritizes Custom over Built-in and Missing`() {
        assertEquals(ApiKeyState.CUSTOM, resolveApiKeyState("user-key", "built-in-key"))
        assertEquals(ApiKeyState.CUSTOM, resolveApiKeyState("  user-key  ", ""))
        assertEquals(ApiKeyState.BUILT_IN, resolveApiKeyState(null, "built-in-key"))
        assertEquals(ApiKeyState.BUILT_IN, resolveApiKeyState("   ", "built-in-key"))
        assertEquals(ApiKeyState.MISSING, resolveApiKeyState(null, null))
        assertEquals(ApiKeyState.MISSING, resolveApiKeyState("", "  "))
        assertTrue(ApiKeyState.CUSTOM.isConfigured)
        assertTrue(ApiKeyState.BUILT_IN.isConfigured)
        assertFalse(ApiKeyState.MISSING.isConfigured)
    }

    @Test
    fun `TmdbConfig custom token override updates configured status and clears back to baseline`() {
        val context = fakePrefsContext()
        val baselineConfigured = TmdbConfig.getBuiltInAccessToken().isNotBlank()
        val baselineState = if (baselineConfigured) ApiKeyState.BUILT_IN else ApiKeyState.MISSING

        assertEquals(baselineState, TmdbConfig.getAccessTokenState(context))
        assertEquals(baselineConfigured, TmdbConfig.isConfigured(context))

        TmdbConfig.setCustomAccessToken(context, "  tmdb_custom_123  ")
        assertEquals("tmdb_custom_123", TmdbConfig.getCustomAccessToken(context))
        assertEquals("tmdb_custom_123", TmdbConfig.getAccessToken(context))
        assertEquals(ApiKeyState.CUSTOM, TmdbConfig.getAccessTokenState(context))
        assertTrue(TmdbConfig.isConfigured(context))

        TmdbConfig.setCustomAccessToken(context, null)
        assertEquals("", TmdbConfig.getCustomAccessToken(context))
        assertEquals(baselineState, TmdbConfig.getAccessTokenState(context))
    }

    @Test
    fun `GoogleBooksConfig custom key override updates state and clears cleanly`() {
        val context = fakePrefsContext()
        val baselineConfigured = GoogleBooksConfig.getBuiltInApiKey().isNotBlank()
        val baselineState = if (baselineConfigured) ApiKeyState.BUILT_IN else ApiKeyState.MISSING

        assertEquals(baselineState, GoogleBooksConfig.getApiKeyState(context))

        GoogleBooksConfig.setCustomApiKey(context, "gbooks_custom_key")
        assertEquals("gbooks_custom_key", GoogleBooksConfig.getCustomApiKey(context))
        assertEquals("gbooks_custom_key", GoogleBooksConfig.getApiKey(context))
        assertEquals(ApiKeyState.CUSTOM, GoogleBooksConfig.getApiKeyState(context))
        assertTrue(GoogleBooksConfig.isConfigured(context))

        GoogleBooksConfig.setCustomApiKey(context, "   ")
        assertEquals("", GoogleBooksConfig.getCustomApiKey(context))
        assertEquals(baselineState, GoogleBooksConfig.getApiKeyState(context))
    }

    @Test
    fun `GameApiConfig custom credentials set configured state and invalidate cached Twitch token`() {
        val context = fakePrefsContext()
        GameApiConfig.setCachedTwitchToken(context, "cached-tok", 3600L)
        assertEquals("cached-tok", GameApiConfig.getCachedTwitchToken(context))

        GameApiConfig.setCustomIgdbCredentials(context, "igdb-id", "igdb-secret")
        assertNull(GameApiConfig.getCachedTwitchToken(context))
        assertEquals(ApiKeyState.CUSTOM, GameApiConfig.getIgdbClientIdState(context))
        assertEquals(ApiKeyState.CUSTOM, GameApiConfig.getIgdbClientSecretState(context))
        assertEquals("igdb-id", GameApiConfig.getIgdbClientId(context))
        assertEquals("igdb-secret", GameApiConfig.getIgdbClientSecret(context))
        assertTrue(GameApiConfig.isIgdbConfigured(context))

        GameApiConfig.setCustomRawgApiKey(context, "rawg-123")
        assertEquals(ApiKeyState.CUSTOM, GameApiConfig.getRawgApiKeyState(context))
        assertEquals("rawg-123", GameApiConfig.getRawgApiKey(context))
        assertTrue(GameApiConfig.isRawgConfigured(context))

        GameApiConfig.setCustomSteamGridDbApiKey(context, "sgdb-456")
        assertEquals(ApiKeyState.CUSTOM, GameApiConfig.getSteamGridDbApiKeyState(context))
        assertEquals("sgdb-456", GameApiConfig.getSteamGridDbApiKey(context))
        assertTrue(GameApiConfig.isSteamGridDbConfigured(context))

        GameApiConfig.setCustomIgdbCredentials(context, null, null)
        GameApiConfig.setCustomRawgApiKey(context, null)
        GameApiConfig.setCustomSteamGridDbApiKey(context, null)
        assertEquals("", GameApiConfig.getCustomIgdbClientId(context))
        assertEquals("", GameApiConfig.getCustomIgdbClientSecret(context))
        assertEquals("", GameApiConfig.getCustomRawgApiKey(context))
        assertEquals("", GameApiConfig.getCustomSteamGridDbApiKey(context))
    }
}
