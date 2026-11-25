package com.lx.tracklist.data.remote

import android.util.Log
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.RelatedMediaItem
import com.lx.tracklist.ui.search.MusicDiscoveryMode
import com.lx.tracklist.ui.search.MusicSortOrder
import com.lx.tracklist.ui.search.OriginalLanguageRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object MusicMetadataManager {
    private const val TAG = "MusicMetadataManager"

    suspend fun searchMusic(
        query: String,
        limit: Int = 20,
        originalLanguages: Set<String> = emptySet(),
        excludedTags: Set<String> = emptySet()
    ): Result<List<MusicSearchResult>> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext Result.success(emptyList())

        supervisorScope {
            if (originalLanguages.isNotEmpty()) {
                val perLangLimit = (limit / originalLanguages.size.coerceAtLeast(1)).coerceAtLeast(15)
                val langBatches = originalLanguages.map { lang ->
                    async {
                        val langOpt = OriginalLanguageRegistry.findByCode(lang)
                        val country = langOpt?.countryCodes?.firstOrNull()?.lowercase(Locale.ROOT)
                        val localizedQuery = if (!lang.equals("en", ignoreCase = true) && langOpt != null) {
                            "$clean ${langOpt.displayName}".trim()
                        } else clean
                        val itRes = ITunesApiClient.searchTracks(
                            query = clean,
                            limit = perLangLimit,
                            countryCode = country,
                            originalLanguage = lang
                        )
                        val direct = itRes.getOrDefault(emptyList())
                        if (direct.isNotEmpty() || localizedQuery == clean) {
                            direct
                        } else {
                            ITunesApiClient.searchTracks(
                                query = localizedQuery,
                                limit = perLangLimit,
                                countryCode = country,
                                originalLanguage = lang
                            ).getOrDefault(emptyList())
                        }
                    }
                }.awaitAll()

                val mbRes = MusicBrainzApiClient.searchRecordings(clean, limit).getOrDefault(emptyList())
                    .filter { OriginalLanguageRegistry.matchesAny(originalLanguages, it.originalLanguage) }

                val interleaved = interleaveLists(langBatches) + mbRes
                val filtered = applyTagAndLanguageFilters(
                    tracks = interleaved,
                    originalLanguages = originalLanguages,
                    excludedTags = excludedTags
                )
                return@supervisorScope Result.success(filtered.take(limit))
            }

            // Concurrently query iTunes (worldwide, robust, instant high-res covers) and MusicBrainz.
            val iTunesDeferred = async { ITunesApiClient.searchTracks(clean, limit) }
            val mbDeferred = async { MusicBrainzApiClient.searchRecordings(clean, limit) }

            val iTunesRes = iTunesDeferred.await()
            val mbRes = mbDeferred.await()

            val iTunesList = iTunesRes.getOrDefault(emptyList())
            val mbList = mbRes.getOrDefault(emptyList())

            val combined = deduplicateTracks(iTunesList + mbList)
            val filtered = applyTagAndLanguageFilters(
                tracks = combined,
                originalLanguages = emptySet(),
                excludedTags = excludedTags
            )

            if (filtered.isEmpty() && iTunesRes.isFailure && mbRes.isFailure) {
                val err = iTunesRes.exceptionOrNull()
                    ?: mbRes.exceptionOrNull()
                    ?: Exception("Failed to search music")
                Result.failure(err)
            } else {
                Result.success(filtered.take(limit))
            }
        }
    }

    suspend fun discoverMusic(
        mode: MusicDiscoveryMode = MusicDiscoveryMode.TOP_CHARTS,
        genre: String? = null,
        year: Int? = null,
        startYear: Int? = null,
        endYear: Int? = null,
        sortOrder: MusicSortOrder = MusicSortOrder.POPULARITY,
        limit: Int = 30,
        originalLanguages: Set<String> = emptySet(),
        includedTags: Set<String> = emptySet(),
        excludedTags: Set<String> = emptySet()
    ): Result<List<MusicSearchResult>> = withContext(Dispatchers.IO) {
        try {
            val effectiveGenre = genre?.takeIf { it.isNotBlank() } ?: includedTags.firstOrNull()
            val targetLangs = originalLanguages.ifEmpty { emptySet() }

            if (targetLangs.isNotEmpty()) {
                val perLangLimit = (limit / targetLangs.size.coerceAtLeast(1)).coerceAtLeast(20)
                val batches = supervisorScope {
                    targetLangs.map { lang ->
                        async {
                            fetchForLanguage(
                                mode = mode,
                                genre = effectiveGenre,
                                year = year,
                                startYear = startYear,
                                endYear = endYear,
                                limit = perLangLimit,
                                languageCode = lang
                            )
                        }
                    }.awaitAll()
                }
                val merged = interleaveLists(batches)
                val filtered = applyTagAndLanguageFilters(
                    tracks = merged,
                    originalLanguages = targetLangs,
                    excludedTags = excludedTags
                )
                return@withContext Result.success(sortTracks(filtered, sortOrder).take(limit))
            }

            val rawRes = fetchForSingleStorefront(
                mode = mode,
                genre = effectiveGenre,
                year = year,
                startYear = startYear,
                endYear = endYear,
                limit = limit,
                countryCode = "us",
                explicitLanguage = null
            )
            if (rawRes.isFailure) return@withContext rawRes
            val filtered = applyTagAndLanguageFilters(
                tracks = deduplicateTracks(rawRes.getOrDefault(emptyList())),
                originalLanguages = emptySet(),
                excludedTags = excludedTags
            )
            Result.success(sortTracks(filtered, sortOrder).take(limit))
        } catch (e: Exception) {
            Log.e(TAG, "discoverMusic error", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchForLanguage(
        mode: MusicDiscoveryMode,
        genre: String?,
        year: Int?,
        startYear: Int?,
        endYear: Int?,
        limit: Int,
        languageCode: String
    ): List<MusicSearchResult> {
        val langOpt = OriginalLanguageRegistry.findByCode(languageCode)
        val candidateCountries = langOpt?.countryCodes
            ?.map { it.lowercase(Locale.ROOT) }
            ?.ifEmpty { null }
            ?: listOf("us")

        for (country in candidateCountries) {
            val res = fetchForSingleStorefront(
                mode = mode,
                genre = genre,
                year = year,
                startYear = startYear,
                endYear = endYear,
                limit = limit,
                countryCode = country,
                explicitLanguage = languageCode,
                languageDisplayName = langOpt?.displayName
            )
            val list = res.getOrDefault(emptyList())
            if (list.isNotEmpty()) return list
        }

        // Last resort: search tracks by language name + optional genre/year
        val langTerm = langOpt?.displayName ?: languageCode
        val fallbackQuery = listOfNotNull(
            genre?.takeIf { it.isNotBlank() },
            langTerm,
            if (mode == MusicDiscoveryMode.BY_YEAR) (endYear ?: year ?: startYear)?.toString() else null
        ).joinToString(" ")
        return ITunesApiClient.searchTracks(
            query = fallbackQuery,
            limit = limit,
            countryCode = candidateCountries.firstOrNull(),
            originalLanguage = languageCode
        ).getOrDefault(emptyList())
    }

    private suspend fun fetchForSingleStorefront(
        mode: MusicDiscoveryMode,
        genre: String?,
        year: Int?,
        startYear: Int?,
        endYear: Int?,
        limit: Int,
        countryCode: String,
        explicitLanguage: String?,
        languageDisplayName: String? = null
    ): Result<List<MusicSearchResult>> {
        return when (mode) {
            MusicDiscoveryMode.TOP_CHARTS -> {
                val genreId = ITunesApiClient.resolveGenreId(genre)
                val chartRes = ITunesApiClient.fetchTopSongs(
                    genreId = genreId,
                    limit = limit,
                    countryCode = countryCode,
                    originalLanguage = explicitLanguage ?: ITunesApiClient.inferLanguageFromCountry(countryCode)
                )
                if (chartRes.isSuccess && !chartRes.getOrNull().isNullOrEmpty()) {
                    chartRes
                } else if (!genre.isNullOrBlank()) {
                    val searchQuery = listOfNotNull(genre, languageDisplayName?.takeIf { !explicitLanguage.equals("en", true) })
                        .joinToString(" ")
                    ITunesApiClient.searchTracks(
                        query = searchQuery,
                        limit = limit,
                        countryCode = countryCode,
                        originalLanguage = explicitLanguage
                    )
                } else {
                    chartRes
                }
            }
            MusicDiscoveryMode.BY_GENRE -> {
                val genreId = ITunesApiClient.resolveGenreId(genre)
                if (genreId != null) {
                    val res = ITunesApiClient.fetchTopSongs(
                        genreId = genreId,
                        limit = limit,
                        countryCode = countryCode,
                        originalLanguage = explicitLanguage ?: ITunesApiClient.inferLanguageFromCountry(countryCode)
                    )
                    if (res.isSuccess && !res.getOrNull().isNullOrEmpty()) return res
                }
                val searchQuery = listOfNotNull(
                    genre ?: "Top Hits",
                    languageDisplayName?.takeIf { !explicitLanguage.equals("en", true) }
                ).joinToString(" ")
                ITunesApiClient.searchTracks(
                    query = searchQuery,
                    limit = limit,
                    countryCode = countryCode,
                    originalLanguage = explicitLanguage
                )
            }
            MusicDiscoveryMode.BY_YEAR -> {
                val targetYear = endYear ?: year ?: startYear ?: 2024
                val langHint = languageDisplayName?.takeIf { !explicitLanguage.equals("en", true) }
                val query = listOfNotNull(
                    genre?.takeIf { it.isNotBlank() },
                    targetYear.toString(),
                    langHint ?: if (genre.isNullOrBlank()) "Hits" else null
                ).joinToString(" ")
                ITunesApiClient.searchTracks(
                    query = query,
                    limit = limit,
                    countryCode = if (!countryCode.equals("us", true) || explicitLanguage != null) countryCode else null,
                    originalLanguage = explicitLanguage
                )
            }
        }
    }

    private fun applyTagAndLanguageFilters(
        tracks: List<MusicSearchResult>,
        originalLanguages: Set<String>,
        excludedTags: Set<String>
    ): List<MusicSearchResult> {
        return tracks.filter { track ->
            val langOk = if (originalLanguages.isEmpty()) true
            else OriginalLanguageRegistry.matchesAny(originalLanguages, track.originalLanguage)
            val excludedOk = if (excludedTags.isEmpty()) true
            else track.genres.none { g -> excludedTags.any { ex -> g.contains(ex, ignoreCase = true) || ex.contains(g, ignoreCase = true) } }
            langOk && excludedOk
        }
    }

    private fun sortTracks(tracks: List<MusicSearchResult>, sortOrder: MusicSortOrder): List<MusicSearchResult> {
        return when (sortOrder) {
            MusicSortOrder.POPULARITY -> tracks
            MusicSortOrder.RECENT -> tracks.sortedByDescending { it.releaseYear?.toIntOrNull() ?: 0 }
            MusicSortOrder.TITLE -> tracks.sortedBy { it.title.lowercase(Locale.ROOT) }
        }
    }

    private fun deduplicateTracks(tracks: List<MusicSearchResult>): List<MusicSearchResult> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<MusicSearchResult>()
        for (track in tracks) {
            if (seen.add(normalizeKey(track.title, track.artist))) {
                result.add(track)
            }
        }
        return result
    }

    private fun interleaveLists(batches: List<List<MusicSearchResult>>): List<MusicSearchResult> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<MusicSearchResult>()
        val maxSize = batches.maxOfOrNull { it.size } ?: 0
        for (idx in 0 until maxSize) {
            for (batch in batches) {
                val track = batch.getOrNull(idx) ?: continue
                if (seen.add(normalizeKey(track.title, track.artist))) {
                    result.add(track)
                }
            }
        }
        return result
    }

    private val albumTracksCache = ConcurrentHashMap<String, MusicAlbumDetails>()

    suspend fun fetchAlbumTracks(albumId: String, source: String = "Apple Music"): Result<MusicAlbumDetails> {
        val cleanId = albumId.trim()
        if (cleanId.isBlank()) return Result.failure(IllegalArgumentException("Blank album ID"))
        val cacheKey = "${source.lowercase(Locale.ROOT)}:$cleanId"
        albumTracksCache[cacheKey]?.let { return Result.success(it) }

        val res = if (source.equals("MusicBrainz", ignoreCase = true) || !cleanId.all { it.isDigit() }) {
            if (cleanId.all { it.isDigit() }) {
                ITunesApiClient.fetchAlbumTracks(cleanId)
            } else {
                MusicBrainzApiClient.fetchAlbumTracks(cleanId)
            }
        } else {
            ITunesApiClient.fetchAlbumTracks(cleanId)
        }
        res.getOrNull()?.let { details ->
            if (details.tracks.isNotEmpty()) {
                if (albumTracksCache.size > 100) albumTracksCache.clear()
                albumTracksCache[cacheKey] = details
            }
        }
        return res
    }

    suspend fun resolveMusicMetadataForSync(
        entry: MediaEntry,
        skipIdLookup: Boolean = false
    ): Result<MusicSearchResult> = withContext(Dispatchers.IO) {
        val extId = entry.externalId?.trim()
        val lookupMatch = if (!skipIdLookup && !extId.isNullOrBlank() && extId.all { it.isDigit() }) {
            ITunesApiClient.lookupTrack(extId).getOrNull()
        } else null

        if (lookupMatch != null) {
            return@withContext Result.success(lookupMatch)
        }

        val queryWithArtist = listOfNotNull(
            entry.title.trim().takeIf { it.isNotBlank() },
            entry.creator?.trim()?.takeIf { it.isNotBlank() }
        ).joinToString(" ")

        val candidates = if (queryWithArtist.isNotBlank()) {
            searchMusic(queryWithArtist, limit = 10).getOrDefault(emptyList())
        } else emptyList()

        val fallbackCandidates = if (candidates.isEmpty() && entry.title.isNotBlank() && queryWithArtist != entry.title.trim()) {
            searchMusic(entry.title.trim(), limit = 10).getOrDefault(emptyList())
        } else emptyList()

        val allCandidates = candidates.ifEmpty { fallbackCandidates }
        val best = pickBestMusicMatch(
            targetTitle = entry.title,
            targetArtist = entry.creator,
            targetAlbum = entry.movieSeriesName,
            candidates = allCandidates
        )

        if (best != null) {
            return@withContext Result.success(best)
        }
        Result.failure(NoSuchElementException("No matching music track found for '${entry.title}'"))
    }

    suspend fun fetchRelatedAlbumTracks(entry: MediaEntry): Result<List<RelatedMediaItem>> {
        return fetchRelatedAlbumTracks(
            title = entry.title,
            artist = entry.creator,
            albumName = entry.movieSeriesName,
            musicAlbumId = entry.musicAlbumId,
            externalId = entry.externalId,
            externalSource = entry.externalSource
        )
    }

    suspend fun fetchRelatedAlbumTracks(
        title: String,
        artist: String? = null,
        albumName: String? = null,
        musicAlbumId: String? = null,
        externalId: String? = null,
        externalSource: String? = null
    ): Result<List<RelatedMediaItem>> = withContext(Dispatchers.IO) {
        try {
            var resolvedAlbumId = musicAlbumId?.trim()?.takeIf { it.isNotBlank() }
            var resolvedSource = externalSource?.trim().orEmpty()
            var lastError: Throwable? = null

            // 1. If we don't have a numeric iTunes album ID yet, try looking up the numeric track ID
            if ((resolvedAlbumId == null || !resolvedAlbumId.all { it.isDigit() }) &&
                !externalId.isNullOrBlank() && externalId.trim().all { it.isDigit() }
            ) {
                val trackRes = ITunesApiClient.lookupTrack(externalId.trim())
                if (trackRes.isFailure) lastError = trackRes.exceptionOrNull()
                val trackAlbumId = trackRes.getOrNull()?.musicAlbumId?.trim()?.takeIf { it.isNotBlank() }
                if (trackAlbumId != null) {
                    resolvedAlbumId = trackAlbumId
                    resolvedSource = "Apple Music"
                }
            }

            // 2. If still no iTunes album ID, search iTunes tracks & albums by title / artist / album
            if (resolvedAlbumId == null || !resolvedAlbumId.all { it.isDigit() }) {
                val trackQuery = listOfNotNull(
                    title.trim().takeIf { it.isNotBlank() },
                    artist?.trim()?.takeIf { it.isNotBlank() }
                ).joinToString(" ")

                if (trackQuery.isNotBlank()) {
                    val searchRes = ITunesApiClient.searchTracks(trackQuery, limit = 10)
                    if (searchRes.isFailure) lastError = searchRes.exceptionOrNull()
                    val bestTrack = pickBestMusicMatch(
                        targetTitle = title,
                        targetArtist = artist,
                        targetAlbum = albumName,
                        candidates = searchRes.getOrDefault(emptyList())
                    )
                    val matchedAlbumId = bestTrack?.musicAlbumId?.trim()?.takeIf { it.isNotBlank() }
                    if (matchedAlbumId != null) {
                        resolvedAlbumId = matchedAlbumId
                        resolvedSource = "Apple Music"
                    }
                }

                if ((resolvedAlbumId == null || !resolvedAlbumId.all { it.isDigit() }) && !albumName.isNullOrBlank()) {
                    val albumQuery = listOfNotNull(
                        albumName.trim().takeIf { it.isNotBlank() },
                        artist?.trim()?.takeIf { it.isNotBlank() }
                    ).joinToString(" ")
                    val albumSearchRes = ITunesApiClient.searchAlbums(albumQuery, limit = 6)
                    if (albumSearchRes.isFailure) lastError = albumSearchRes.exceptionOrNull()
                    val albums = albumSearchRes.getOrDefault(emptyList())
                    val matchedAlbum = pickBestAlbumMatch(albumName, artist, albums)
                    if (matchedAlbum != null) {
                        resolvedAlbumId = matchedAlbum.albumId
                        resolvedSource = "Apple Music"
                    }
                }
            }

            // 3. Fetch album tracks from iTunes (or MusicBrainz if UUID)
            val effectiveAlbumId = resolvedAlbumId ?: musicAlbumId?.trim()?.takeIf { it.isNotBlank() }
            if (effectiveAlbumId.isNullOrBlank()) {
                return@withContext if (lastError != null) {
                    Result.failure(lastError)
                } else {
                    Result.success(emptyList())
                }
            }

            val albumRes = fetchAlbumTracks(
                albumId = effectiveAlbumId,
                source = if (effectiveAlbumId.all { it.isDigit() }) "Apple Music" else resolvedSource.ifBlank { "MusicBrainz" }
            )

            // If iTunes album lookup failed or returned empty and we had a MusicBrainz album ID, try MusicBrainz
            val finalAlbumRes = if ((albumRes.isFailure || albumRes.getOrNull()?.tracks.isNullOrEmpty()) &&
                !musicAlbumId.isNullOrBlank() && !musicAlbumId.trim().all { it.isDigit() }
            ) {
                fetchAlbumTracks(musicAlbumId.trim(), source = "MusicBrainz")
            } else {
                albumRes
            }

            if (finalAlbumRes.isFailure) {
                return@withContext Result.failure(finalAlbumRes.exceptionOrNull() ?: Exception("Failed to fetch album tracks"))
            }

            val details = finalAlbumRes.getOrNull() ?: return@withContext Result.success(emptyList())
            val items = details.tracks.map { track ->
                val relLabel = if (track.trackNumber != null && track.trackNumber > 0) {
                    "Track ${track.trackNumber}"
                } else {
                    "Album Track"
                }
                RelatedMediaItem(
                    title = track.title,
                    relationship = relLabel,
                    externalId = track.externalId,
                    externalSource = track.externalSource,
                    mediaType = MediaType.MUSIC,
                    posterUrl = track.coverUrl ?: track.albumCoverUrl ?: details.coverUrl,
                    releaseYear = track.releaseYear ?: details.releaseYear,
                    score = track.score
                )
            }
            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "fetchRelatedAlbumTracks error for $title", e)
            Result.failure(e)
        }
    }

    internal fun pickBestMusicMatch(
        targetTitle: String,
        targetArtist: String?,
        targetAlbum: String?,
        candidates: List<MusicSearchResult>
    ): MusicSearchResult? {
        if (candidates.isEmpty()) return null
        val normTitle = normalizeLoose(targetTitle)
        val normArtist = targetArtist?.let { normalizeLoose(it) }.orEmpty()
        val normAlbum = targetAlbum?.let { normalizeLoose(it) }.orEmpty()

        return candidates.maxByOrNull { cand ->
            var score = 0
            val cTitle = normalizeLoose(cand.title)
            val cArtist = normalizeLoose(cand.artist)
            val cAlbum = cand.albumName?.let { normalizeLoose(it) }.orEmpty()

            if (cTitle == normTitle) score += 100
            else if (normTitle.isNotBlank() && (cTitle.contains(normTitle) || normTitle.contains(cTitle))) score += 40

            if (normArtist.isNotBlank()) {
                if (cArtist == normArtist) score += 60
                else if (cArtist.contains(normArtist) || normArtist.contains(cArtist)) score += 30
            }
            if (normAlbum.isNotBlank() && cAlbum.isNotBlank()) {
                if (cAlbum == normAlbum) score += 50
                else if (cAlbum.contains(normAlbum) || normAlbum.contains(cAlbum)) score += 25
            }
            if (!cand.musicAlbumId.isNullOrBlank() && cand.musicAlbumId.all { it.isDigit() }) score += 20
            if (cand.externalSource.equals("iTunes", ignoreCase = true) || cand.externalSource.equals("Apple Music", ignoreCase = true)) score += 10
            if (!cand.coverUrl.isNullOrBlank()) score += 5
            score
        }
    }

    private fun pickBestAlbumMatch(
        targetAlbum: String,
        targetArtist: String?,
        albums: List<MusicAlbumDetails>
    ): MusicAlbumDetails? {
        if (albums.isEmpty()) return null
        val normAlbum = normalizeLoose(targetAlbum)
        val normArtist = targetArtist?.let { normalizeLoose(it) }.orEmpty()

        return albums.maxByOrNull { alb ->
            var score = 0
            val cAlbum = normalizeLoose(alb.title)
            val cArtist = normalizeLoose(alb.artist)
            if (cAlbum == normAlbum) score += 100
            else if (cAlbum.contains(normAlbum) || normAlbum.contains(cAlbum)) score += 40
            if (normArtist.isNotBlank()) {
                if (cArtist == normArtist) score += 60
                else if (cArtist.contains(normArtist) || normArtist.contains(cArtist)) score += 30
            }
            score
        }
    }

    private fun normalizeLoose(text: String): String {
        return text.lowercase(Locale.ROOT).replace(Regex("""[^a-z0-9]"""), "")
    }

    suspend fun resolveCoverUrl(artist: String, title: String, existingCoverUrl: String?): String? {
        if (!existingCoverUrl.isNullOrBlank() && !existingCoverUrl.contains("coverartarchive.org")) {
            return existingCoverUrl
        }
        return ITunesApiClient.findCoverUrl(artist, title) ?: existingCoverUrl
    }

    private fun normalizeKey(title: String, artist: String): String {
        val cleanTitle = title.lowercase(Locale.ROOT).replace(Regex("""[^a-z0-9]"""), "")
        val cleanArtist = artist.lowercase(Locale.ROOT).replace(Regex("""[^a-z0-9]"""), "")
        return "$cleanArtist:$cleanTitle"
    }
}
