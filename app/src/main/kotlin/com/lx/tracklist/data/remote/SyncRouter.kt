package com.lx.tracklist.data.remote

import com.lx.tracklist.data.model.ExternalSources
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaType

/** Canonical metadata sync job identifiers. */
enum class SyncJob {
    JIKAN,
    TMDB,
    BOOKS,
    GAMES,
    MUSIC,
    RESOLVE_IDS
}

/**
 * Single authoritative router assigning each [MediaEntry] to exactly one [SyncJob].
 */
object SyncRouter {

    /**
     * Returns the single [SyncJob] responsible for syncing [entry].
     */
    fun jobFor(entry: MediaEntry): SyncJob {
        val canonicalSource = ExternalSources.canonical(entry.externalSource, entry.externalId)
        if (canonicalSource == ExternalSources.TACHIYOMI || JikanSyncManager.isTachiyomiSource(entry)) {
            return SyncJob.RESOLVE_IDS
        }
        return when (entry.mediaType) {
            MediaType.ANIME -> if (canonicalSource == ExternalSources.TMDB) SyncJob.TMDB else SyncJob.JIKAN
            MediaType.MANGA, MediaType.MANHWA -> SyncJob.JIKAN
            MediaType.BOOK -> SyncJob.BOOKS
            MediaType.MOVIE, MediaType.SHOW -> {
                if (canonicalSource == ExternalSources.MAL || canonicalSource == ExternalSources.ANILIST) {
                    SyncJob.JIKAN
                } else {
                    SyncJob.TMDB
                }
            }
            MediaType.GAME -> SyncJob.GAMES
            MediaType.MUSIC -> SyncJob.MUSIC
        }
    }
}
