package com.lx.tracklist.data.remote

object ITunesCoverApiClient {
    suspend fun findCoverUrl(artist: String, title: String): String? {
        return ITunesApiClient.findCoverUrl(artist, title)
    }
}
