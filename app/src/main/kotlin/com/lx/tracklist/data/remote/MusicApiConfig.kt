package com.lx.tracklist.data.remote

import android.content.Context

object MusicApiConfig {
    private const val PREFS_NAME = "lx_music_api_config"

    const val MUSICBRAINZ_BASE_URL = "https://musicbrainz.org/ws/2"
    const val COVER_ART_ARCHIVE_BASE_URL = "https://coverartarchive.org"
    const val DEEZER_BASE_URL = "https://api.deezer.com"
    const val ITUNES_SEARCH_URL = "https://itunes.apple.com/search"

    // MusicBrainz asks for descriptive User-Agent with app name, version, and contact info
    const val MUSICBRAINZ_USER_AGENT = "LXTrackList/1.0 ( Android; contact@lxtracklist.app )"

    const val PREF_SPOTIFY_CLIENT_ID = "spotify_client_id"
    const val PREF_SPOTIFY_CLIENT_SECRET = "spotify_client_secret"

    fun getSpotifyClientId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_SPOTIFY_CLIENT_ID, "")?.trim().orEmpty()
    }

    fun getSpotifyClientSecret(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_SPOTIFY_CLIENT_SECRET, "")?.trim().orEmpty()
    }

    fun isSpotifyConfigured(context: Context): Boolean {
        return getSpotifyClientId(context).isNotBlank() && getSpotifyClientSecret(context).isNotBlank()
    }

    fun setSpotifyCredentials(context: Context, clientId: String, clientSecret: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(PREF_SPOTIFY_CLIENT_ID, clientId.trim())
            .putString(PREF_SPOTIFY_CLIENT_SECRET, clientSecret.trim())
            .apply()
    }

    fun clearSpotifyCredentials(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(PREF_SPOTIFY_CLIENT_ID)
            .remove(PREF_SPOTIFY_CLIENT_SECRET)
            .apply()
    }
}
