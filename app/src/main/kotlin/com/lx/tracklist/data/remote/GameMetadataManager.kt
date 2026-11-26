package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log
import com.lx.tracklist.data.model.ExternalSources
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.ui.search.AdultMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GameMetadataManager {
    private const val TAG = "GameMetadataManager"

    /**
     * Searches for games, prioritizing IGDB as primary and falling back to RAWG if unconfigured or no hits,
     * or routing directly if a specific source ("IGDB" or "RAWG") is specified.
     */
    suspend fun searchGames(
        context: Context,
        query: String,
        source: String? = null,
        limit: Int = 20,
        page: Int = 1,
        includedPlatforms: Set<String> = emptySet(),
        excludedPlatforms: Set<String> = emptySet(),
        includedTags: Set<String> = emptySet(),
        excludedTags: Set<String> = emptySet(),
        adultMode: AdultMode = AdultMode.OFF,
        year: Int? = null,
        startYear: Int? = null,
        endYear: Int? = null
    ): Result<List<GameSearchResult>> {
        val src = source?.trim()?.uppercase()
        if (src == "IGDB") {
            if (!GameApiConfig.isIgdbConfigured(context)) {
                return Result.failure(IllegalStateException("IGDB credentials are not configured. Add an API key in Settings → External Sync Jobs → API Keys."))
            }
            return IgdbApiClient.searchGames(
                context = context,
                query = query,
                limit = limit,
                page = page,
                includedPlatforms = includedPlatforms,
                excludedPlatforms = excludedPlatforms,
                includedTags = includedTags,
                excludedTags = excludedTags,
                adultMode = adultMode,
                year = year,
                startYear = startYear,
                endYear = endYear
            )
        }
        if (src == "RAWG") {
            if (!GameApiConfig.isRawgConfigured(context)) {
                return Result.failure(IllegalStateException("RAWG API key is not configured. Add an API key in Settings → External Sync Jobs → API Keys."))
            }
            return RawgApiClient.searchGames(
                context = context,
                query = query,
                limit = limit,
                page = page,
                includedPlatforms = includedPlatforms,
                excludedPlatforms = excludedPlatforms,
                includedTags = includedTags,
                excludedTags = excludedTags,
                adultMode = adultMode,
                year = year,
                startYear = startYear,
                endYear = endYear
            )
        }

        // 1. Try IGDB primary
        if (GameApiConfig.isIgdbConfigured(context)) {
            val igdbRes = IgdbApiClient.searchGames(
                context = context,
                query = query,
                limit = limit,
                page = page,
                includedPlatforms = includedPlatforms,
                excludedPlatforms = excludedPlatforms,
                includedTags = includedTags,
                excludedTags = excludedTags,
                adultMode = adultMode,
                year = year,
                startYear = startYear,
                endYear = endYear
            )
            if (igdbRes.isSuccess) {
                val list = igdbRes.getOrDefault(emptyList())
                if (list.isNotEmpty()) {
                    return Result.success(list)
                }
            } else {
                Log.w(TAG, "IGDB search failed: ${igdbRes.exceptionOrNull()?.message}")
            }
        }

        // 2. Fallback to RAWG
        if (GameApiConfig.isRawgConfigured(context)) {
            val rawgRes = RawgApiClient.searchGames(
                context = context,
                query = query,
                limit = limit,
                page = page,
                includedPlatforms = includedPlatforms,
                excludedPlatforms = excludedPlatforms,
                includedTags = includedTags,
                excludedTags = excludedTags,
                adultMode = adultMode,
                year = year,
                startYear = startYear,
                endYear = endYear
            )
            if (rawgRes.isSuccess) {
                return rawgRes
            } else {
                Log.w(TAG, "RAWG search fallback failed: ${rawgRes.exceptionOrNull()?.message}")
            }
        }

        return Result.failure(
            IllegalStateException(
                if (!GameApiConfig.isIgdbConfigured(context) && !GameApiConfig.isRawgConfigured(context)) {
                    "Neither IGDB nor RAWG credentials are configured. Add an API key in Settings → External Sync Jobs → API Keys."
                } else {
                    "No game search results found."
                }
            )
        )
    }

    /**
     * Discovers popular or year-based games, prioritizing IGDB and falling back to RAWG.
     */
    suspend fun discoverGames(
        context: Context,
        year: Int? = null,
        startYear: Int? = null,
        endYear: Int? = null,
        sort: String = "total_rating_count desc",
        limit: Int = 20,
        page: Int = 1,
        includedTags: Set<String> = emptySet(),
        excludedTags: Set<String> = emptySet(),
        adultMode: AdultMode = AdultMode.OFF,
        includedPlatforms: Set<String> = emptySet(),
        excludedPlatforms: Set<String> = emptySet()
    ): Result<List<GameSearchResult>> {
        val sYear = startYear ?: year
        val eYear = endYear ?: year

        if (GameApiConfig.isIgdbConfigured(context)) {
            val res = IgdbApiClient.discoverGames(
                context = context,
                startYear = sYear,
                endYear = eYear,
                sort = sort,
                limit = limit,
                page = page,
                includedTags = includedTags,
                excludedTags = excludedTags,
                adultMode = adultMode,
                includedPlatforms = includedPlatforms,
                excludedPlatforms = excludedPlatforms
            )
            if (res.isSuccess && res.getOrDefault(emptyList()).isNotEmpty()) {
                return res
            }
        }
        if (GameApiConfig.isRawgConfigured(context)) {
            val rawgRes = RawgApiClient.discoverGames(
                context = context,
                startYear = sYear,
                endYear = eYear,
                limit = limit,
                page = page,
                includedTags = includedTags,
                excludedTags = excludedTags,
                adultMode = adultMode,
                includedPlatforms = includedPlatforms,
                excludedPlatforms = excludedPlatforms
            )
            if (rawgRes.isSuccess && rawgRes.getOrDefault(emptyList()).isNotEmpty()) {
                return rawgRes
            }
        }
        return Result.failure(
            IllegalStateException(
                if (!GameApiConfig.isIgdbConfigured(context) && !GameApiConfig.isRawgConfigured(context)) {
                    "Neither IGDB nor RAWG credentials are configured. Add an API key in Settings → External Sync Jobs → API Keys."
                } else {
                    "No discovery results found."
                }
            )
        )
    }

    /**
     * Fetches details for a specific game, routing strictly to the source that owns [externalId].
     * Never sends an IGDB (or non-RAWG) ID to [RawgApiClient.getGameDetails]; falls back to
     * [RawgApiClient.searchGames] + confident title/year match when [title] is provided.
     */
    suspend fun getGameDetails(
        context: Context,
        externalId: String,
        externalSource: String?,
        title: String? = null,
        alternateTitle: String? = null,
        releaseYear: String? = null
    ): Result<GameDetails> = getGameDetailsInternal(
        externalId = externalId,
        externalSource = externalSource,
        title = title,
        alternateTitle = alternateTitle,
        releaseYear = releaseYear,
        igdbConfigured = GameApiConfig.isIgdbConfigured(context),
        rawgConfigured = GameApiConfig.isRawgConfigured(context),
        fetchIgdbDetails = { id -> IgdbApiClient.getGameDetails(context, id) },
        fetchRawgDetails = { id -> RawgApiClient.getGameDetails(context, id) },
        searchRawgGames = { query, limit -> RawgApiClient.searchGames(context, query, limit = limit) }
    )

    internal fun isConfidentGameMatch(
        queryTitle: String,
        candidate: GameSearchResult,
        alternateTitle: String? = null,
        releaseYear: String? = null
    ): Boolean {
        val titleMatched = TachiyomiResolverManager.isConfidentExactMatch(
            queryTitle = queryTitle,
            candidateTitle = candidate.title,
            candidateAltTitles = candidate.alternateTitle,
            entryAltTitles = alternateTitle
        )
        if (!titleMatched) return false

        val expectedYear = releaseYear?.trim()?.take(4)?.takeIf { it.length == 4 && it.all(Char::isDigit) }
        val candYear = candidate.releaseYear?.trim()?.take(4)?.takeIf { it.length == 4 && it.all(Char::isDigit) }
            ?: candidate.releaseDateMillis?.let { MediaEntry.extractYearFromMillis(it) }
        if (expectedYear != null && candYear != null && expectedYear != candYear) {
            return false
        }
        return true
    }

    internal suspend fun getGameDetailsInternal(
        externalId: String,
        externalSource: String?,
        title: String? = null,
        alternateTitle: String? = null,
        releaseYear: String? = null,
        igdbConfigured: Boolean,
        rawgConfigured: Boolean,
        fetchIgdbDetails: suspend (String) -> Result<GameDetails>,
        fetchRawgDetails: suspend (String) -> Result<GameDetails>,
        searchRawgGames: suspend (String, Int) -> Result<List<GameSearchResult>>
    ): Result<GameDetails> {
        val canonicalSrc = ExternalSources.canonical(externalSource, externalId)
        if (canonicalSrc == ExternalSources.RAWG) {
            return fetchRawgDetails(externalId)
        }

        var igdbFailure: Result<GameDetails>? = null
        if (igdbConfigured) {
            val igdbRes = fetchIgdbDetails(externalId)
            if (igdbRes.isSuccess) {
                val details = igdbRes.getOrThrow()
                if (details.completionTimeMinutes == null && rawgConfigured) {
                    val rawgMatch = searchRawgGames(details.title, 5).getOrNull().orEmpty().firstOrNull {
                        isConfidentGameMatch(
                            queryTitle = details.title,
                            candidate = it,
                            alternateTitle = details.alternateTitles.joinToString("\n").takeIf { alts -> alts.isNotBlank() },
                            releaseYear = details.releaseYear
                        )
                    }
                    if (rawgMatch != null) {
                        val rawgDetails = fetchRawgDetails(rawgMatch.externalId).getOrNull()
                        if (rawgDetails?.completionTimeMinutes != null) {
                            return Result.success(
                                details.copy(
                                    completionTimeMinutes = rawgDetails.completionTimeMinutes,
                                    normalTimeMinutes = rawgDetails.normalTimeMinutes
                                )
                            )
                        }
                    }
                }
                return Result.success(details)
            }
            igdbFailure = igdbRes
        }

        // Non-RAWG IDs must NEVER be queried against RawgApiClient.getGameDetails.
        // Fall back only via confident RAWG title (+ release year) search when a title is known.
        val cleanTitle = title?.trim()?.takeIf { it.isNotBlank() }
        if (rawgConfigured && cleanTitle != null) {
            val rawgList = searchRawgGames(cleanTitle, 5).getOrNull().orEmpty()
            val confidentMatch = rawgList.firstOrNull {
                isConfidentGameMatch(
                    queryTitle = cleanTitle,
                    candidate = it,
                    alternateTitle = alternateTitle,
                    releaseYear = releaseYear
                )
            }
            if (confidentMatch != null) {
                return fetchRawgDetails(confidentMatch.externalId)
            }
            return Result.failure(NoSuchElementException("No confident RAWG match found for '$cleanTitle'"))
        }

        return igdbFailure ?: Result.failure(
            IllegalStateException(
                if (!igdbConfigured && !rawgConfigured) {
                    "No game API configured. Please configure IGDB or RAWG in Settings."
                } else {
                    "No IGDB credentials configured and no title match available for '$externalId'."
                }
            )
        )
    }

    /**
     * Searches SteamGridDB for vertical 600x900 custom box art posters.
     */
    suspend fun searchSteamGridPosters(context: Context, title: String): Result<List<SteamGridPosterItem>> = withContext(Dispatchers.IO) {
        if (!GameApiConfig.isSteamGridDbConfigured(context)) {
            return@withContext Result.failure(IllegalStateException("SteamGridDB API key is not configured."))
        }

        val searchRes = SteamGridDbApiClient.searchByTitle(context, title)
        val candidates = searchRes.getOrElse { return@withContext Result.failure(it) }
        val bestMatch = candidates.firstOrNull { it.name.equals(title, ignoreCase = true) }
            ?: candidates.firstOrNull()
            ?: return@withContext Result.failure(NoSuchElementException("No SteamGridDB game entry found for '$title'"))

        SteamGridDbApiClient.getPortraitGrids(context, bestMatch.id)
    }

    /**
     * Generates a zero-key Steam CDN library portrait (600x900) poster URL given a Steam App ID.
     */
    fun getSteamCdnPosterUrl(steamAppId: String?): String? {
        val cleanId = steamAppId?.trim()
        if (cleanId.isNullOrBlank() || !cleanId.all { it.isDigit() }) return null
        return "https://cdn.cloudflare.steamstatic.com/steam/apps/$cleanId/library_600x900_2x.jpg"
    }
}
