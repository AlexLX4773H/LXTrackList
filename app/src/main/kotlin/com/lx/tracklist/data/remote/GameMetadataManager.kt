package com.lx.tracklist.data.remote

import android.content.Context
import android.util.Log
import com.lx.tracklist.ui.search.AdultMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GameMetadataManager {
    private const val TAG = "GameMetadataManager"

    /**
     * Searches for games, prioritizing IGDB as primary and falling back to RAWG if unconfigured or no hits,
     * or routing directly if a specific source ("IGDB" or "RAWG") is specified.
     */
    suspend fun searchGames(context: Context, query: String, source: String? = null, limit: Int = 20): Result<List<GameSearchResult>> {
        val src = source?.trim()?.uppercase()
        if (src == "IGDB") {
            if (!GameApiConfig.isIgdbConfigured(context)) {
                return Result.failure(IllegalStateException("IGDB credentials are not configured. Please add your Twitch Client ID and Secret in Settings."))
            }
            return IgdbApiClient.searchGames(context, query, limit)
        }
        if (src == "RAWG") {
            if (!GameApiConfig.isRawgConfigured(context)) {
                return Result.failure(IllegalStateException("RAWG API key is not configured. Please add your RAWG API key in Settings."))
            }
            return RawgApiClient.searchGames(context, query, limit)
        }

        // 1. Try IGDB primary
        if (GameApiConfig.isIgdbConfigured(context)) {
            val igdbRes = IgdbApiClient.searchGames(context, query, limit)
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
            val rawgRes = RawgApiClient.searchGames(context, query, limit)
            if (rawgRes.isSuccess) {
                return rawgRes
            } else {
                Log.w(TAG, "RAWG search fallback failed: ${rawgRes.exceptionOrNull()?.message}")
            }
        }

        return Result.failure(
            IllegalStateException(
                if (!GameApiConfig.isIgdbConfigured(context) && !GameApiConfig.isRawgConfigured(context)) {
                    "Neither IGDB nor RAWG credentials are configured. Set your free API keys in Settings."
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
                    "Neither IGDB nor RAWG credentials are configured. Set your free API keys in Settings."
                } else {
                    "No discovery results found."
                }
            )
        )
    }

    /**
     * Fetches details for a specific game, routing to the source that provided the externalId.
     */
    suspend fun getGameDetails(context: Context, externalId: String, externalSource: String?): Result<GameDetails> {
        val src = externalSource?.trim().orEmpty()
        return if (src.equals("RAWG", ignoreCase = true)) {
            RawgApiClient.getGameDetails(context, externalId)
        } else {
            // Default to IGDB
            if (GameApiConfig.isIgdbConfigured(context)) {
                val igdbRes = IgdbApiClient.getGameDetails(context, externalId)
                if (igdbRes.isSuccess) {
                    val details = igdbRes.getOrThrow()
                    if (details.completionTimeMinutes == null && GameApiConfig.isRawgConfigured(context)) {
                        val rawgMatch = RawgApiClient.searchGames(context, details.title, limit = 1).getOrNull()?.firstOrNull()
                        if (rawgMatch != null) {
                            val rawgDetails = RawgApiClient.getGameDetails(context, rawgMatch.externalId).getOrNull()
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
                    Result.success(details)
                } else if (GameApiConfig.isRawgConfigured(context)) {
                    RawgApiClient.getGameDetails(context, externalId)
                } else {
                    igdbRes
                }
            } else if (GameApiConfig.isRawgConfigured(context)) {
                RawgApiClient.getGameDetails(context, externalId)
            } else {
                Result.failure(IllegalStateException("No game API configured. Please configure IGDB or RAWG in Settings."))
            }
        }
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
