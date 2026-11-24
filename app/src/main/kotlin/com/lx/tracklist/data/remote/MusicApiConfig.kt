package com.lx.tracklist.data.remote

object MusicApiConfig {
    const val MUSICBRAINZ_BASE_URL = "https://musicbrainz.org/ws/2"
    const val COVER_ART_ARCHIVE_BASE_URL = "https://coverartarchive.org"
    const val ITUNES_SEARCH_URL = "https://itunes.apple.com/search"

    // MusicBrainz asks for descriptive User-Agent with app name, version, and contact info
    const val MUSICBRAINZ_USER_AGENT = "LXTrackList/1.0 ( Android; contact@lxtracklist.app )"
}
