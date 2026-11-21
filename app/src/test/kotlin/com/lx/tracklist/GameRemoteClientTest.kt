package com.lx.tracklist

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.remote.GameMetadataManager
import com.lx.tracklist.data.remote.IgdbApiClient
import com.lx.tracklist.data.remote.RawgApiClient
import com.lx.tracklist.data.remote.SteamGridDbApiClient
import com.lx.tracklist.util.ExternalLinkHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRemoteClientTest {

    private val gson = Gson()

    @Test
    fun testIgdbSearchResultsParsing() {
        val json = """
            [
              {
                "id": 1942,
                "name": "The Witcher 3: Wild Hunt",
                "first_release_date": 1431993600,
                "cover": { "id": 1001, "image_id": "co1wyy" },
                "artworks": [ { "id": 2001, "image_id": "ar1234" } ],
                "platforms": [
                  { "id": 6, "name": "PC (Microsoft Windows)", "abbreviation": "PC" },
                  { "id": 48, "name": "PlayStation 4", "abbreviation": "PS4" }
                ],
                "genres": [ { "id": 12, "name": "Role-playing (RPG)" } ],
                "total_rating": 92.4,
                "summary": "Geralt of Rivia searches for Ciri.",
                "collections": [ { "id": 72, "name": "The Witcher" } ]
              }
            ]
        """.trimIndent()

        val results = IgdbApiClient.parseSearchResultsJson(json)
        assertEquals(1, results.size)
        val item = results[0]
        assertEquals("1942", item.externalId)
        assertEquals("The Witcher 3: Wild Hunt", item.title)
        assertEquals("https://images.igdb.com/igdb/image/upload/t_cover_big_2x/co1wyy.jpg", item.coverUrl)
        assertEquals("https://images.igdb.com/igdb/image/upload/t_1080p/ar1234.jpg", item.artworkUrl)
        assertEquals("2015", item.releaseYear)
        assertEquals(9.24f, item.score ?: 0f, 0.01f)
        assertTrue(item.isCollectionFranchise)
        assertEquals(72L, item.collectionId)
        assertEquals("The Witcher", item.collectionName)
        assertTrue(item.platforms.contains("PC (Microsoft Windows)"))
    }

    @Test
    fun testIgdbDetailsAndPrequelSequelDerivation() {
        // Detailed response with a collection containing 3 main-line games:
        // Witcher 1 (epoch 1193356800 = 2007)
        // Witcher 2 (epoch 1305590400 = 2011) - current target
        // Witcher 3 (epoch 1431993600 = 2015)
        // Plus 1 DLC and 1 Remake
        val detailsJson = """
            {
              "id": 1941,
              "name": "The Witcher 2: Assassins of Kings",
              "first_release_date": 1305590400,
              "cover": { "id": 501, "image_id": "cov_w2" },
              "summary": "Geralt continues his adventure.",
              "total_rating": 88.0,
              "collections": [
                {
                  "id": 72,
                  "name": "The Witcher",
                  "games": [
                    { "id": 1940, "name": "The Witcher", "first_release_date": 1193356800, "game_type": 0, "cover": { "image_id": "cov_w1" } },
                    { "id": 1941, "name": "The Witcher 2: Assassins of Kings", "first_release_date": 1305590400, "game_type": 0, "cover": { "image_id": "cov_w2" } },
                    { "id": 1942, "name": "The Witcher 3: Wild Hunt", "first_release_date": 1431993600, "game_type": 0, "cover": { "image_id": "cov_w3" } }
                  ]
                }
              ],
              "dlcs": [
                { "id": 9901, "name": "Enhanced Edition Bonus Pack", "first_release_date": 1334620800 }
              ],
              "remakes": [
                { "id": 9902, "name": "The Witcher Remake", "first_release_date": 1800000000 }
              ],
              "external_games": [
                { "id": 888, "category": 1, "uid": "20920" }
              ]
            }
        """.trimIndent()

        val obj = gson.fromJson(detailsJson, JsonObject::class.java)
        val details = IgdbApiClient.parseGameDetailsJson(obj, "1941")

        assertEquals("1941", details.externalId)
        assertEquals("The Witcher 2: Assassins of Kings", details.title)
        assertEquals("2011", details.releaseYear)
        assertEquals("20920", details.steamAppId)
        assertEquals(8.8f, details.rating ?: 0f, 0.01f)

        // Verify related items:
        // Witcher 1 must be "Prequel"
        // Witcher 3 must be "Sequel"
        // Bonus Pack must be "DLC"
        // Remake must be "Remake"
        val related = details.relatedItems
        assertEquals(4, related.size)

        val prequel = related.firstOrNull { it.relationship == "Prequel" }
        assertNotNull(prequel)
        assertEquals("1940", prequel?.externalId)
        assertEquals("The Witcher", prequel?.title)
        assertEquals("2007", prequel?.releaseYear)
        assertEquals(MediaType.GAME, prequel?.mediaType)

        val sequel = related.firstOrNull { it.relationship == "Sequel" }
        assertNotNull(sequel)
        assertEquals("1942", sequel?.externalId)
        assertEquals("The Witcher 3: Wild Hunt", sequel?.title)
        assertEquals("2015", sequel?.releaseYear)

        val dlc = related.firstOrNull { it.relationship == "DLC" }
        assertNotNull(dlc)
        assertEquals("9901", dlc?.externalId)

        val remake = related.firstOrNull { it.relationship == "Remake" }
        assertNotNull(remake)
        assertEquals("9902", remake?.externalId)
    }

    @Test
    fun testSteamGridDbParsing() {
        val searchJson = """
            {
              "success": true,
              "data": [
                { "id": 34125, "name": "Hollow Knight", "release_date": 1487894400 }
              ]
            }
        """.trimIndent()

        val searchList = SteamGridDbApiClient.parseSearchResultsJson(searchJson)
        assertEquals(1, searchList.size)
        assertEquals(34125L, searchList[0].id)
        assertEquals("Hollow Knight", searchList[0].name)

        val gridsJson = """
            {
              "success": true,
              "data": [
                {
                  "id": 9012,
                  "score": 42,
                  "style": "alternate",
                  "width": 600,
                  "height": 900,
                  "url": "https://cdn2.steamgriddb.com/grid/abc.png",
                  "thumb": "https://cdn2.steamgriddb.com/thumb/abc.jpg"
                }
              ]
            }
        """.trimIndent()

        val grids = SteamGridDbApiClient.parseGridsJson(gridsJson)
        assertEquals(1, grids.size)
        val g = grids[0]
        assertEquals(9012L, g.id)
        assertEquals(600, g.width)
        assertEquals(900, g.height)
        assertEquals("https://cdn2.steamgriddb.com/grid/abc.png", g.url)
        assertEquals("https://cdn2.steamgriddb.com/thumb/abc.jpg", g.thumbUrl)
        assertEquals(42, g.score)
    }

    @Test
    fun testRawgParsing() {
        val searchJson = """
            {
              "count": 1,
              "results": [
                {
                  "id": 3498,
                  "name": "Grand Theft Auto V",
                  "released": "2013-09-17",
                  "background_image": "https://media.rawg.io/media/games/456/456dea5e1c7e3cd0702502577319ea0a.jpg",
                  "rating": 4.47,
                  "platforms": [
                    { "platform": { "id": 4, "name": "PC" } },
                    { "platform": { "id": 187, "name": "PlayStation 5" } }
                  ],
                  "genres": [ { "name": "Action" } ]
                }
              ]
            }
        """.trimIndent()

        val results = RawgApiClient.parseSearchResultsJson(searchJson)
        assertEquals(1, results.size)
        val item = results[0]
        assertEquals("3498", item.externalId)
        assertEquals("Grand Theft Auto V", item.title)
        assertEquals("2013", item.releaseYear)
        assertEquals(8.94f, item.score ?: 0f, 0.05f) // 4.47 * 2
        assertEquals("RAWG", item.externalSource)
        assertTrue(item.platforms.contains("PC"))
        assertTrue(item.platforms.contains("PlayStation 5"))
    }

    @Test
    fun testSteamCdnPosterFallback() {
        val url = GameMetadataManager.getSteamCdnPosterUrl("20920")
        assertEquals("https://cdn.cloudflare.steamstatic.com/steam/apps/20920/library_600x900_2x.jpg", url)

        val invalid = GameMetadataManager.getSteamCdnPosterUrl("not_a_number")
        assertEquals(null, invalid)
    }

    @Test
    fun testExternalGameOpenOptions() {
        val options = ExternalLinkHelper.buildGameOpenOptions(
            title = "Cyberpunk 2077",
            externalId = "1877",
            externalSource = "IGDB"
        )

        assertEquals(4, options.size)
        assertEquals("IGDB", options[0].title)
        assertEquals("https://www.igdb.com/games/1877", options[0].url)

        assertEquals("RAWG", options[1].title)
        assertTrue(options[1].url.contains("rawg.io"))

        assertEquals("Steam Store", options[2].title)
        assertTrue(options[2].url.contains("store.steampowered.com"))

        assertEquals("SteamGridDB", options[3].title)
        assertTrue(options[3].url.contains("steamgriddb.com"))
    }

    @Test
    fun testGameTagRegistryGenreLookup() {
        val fighting = com.lx.tracklist.ui.search.GameTagRegistry.findGenre("Fighting")
        assertNotNull(fighting)
        assertEquals(4, fighting?.igdbId)
        assertEquals("fighting", fighting?.rawgSlug)

        val rpg = com.lx.tracklist.ui.search.GameTagRegistry.findGenre("Role-playing (RPG)")
        assertNotNull(rpg)
        assertEquals(12, rpg?.igdbId)
        assertEquals("role-playing-games-rpg", rpg?.rawgSlug)

        val rts = com.lx.tracklist.ui.search.GameTagRegistry.findGenre("Real Time Strategy (RTS)")
        assertNotNull(rts)
        assertEquals(11, rts?.igdbId)
        assertEquals("strategy", rts?.rawgSlug)
    }

    @Test
    fun testIgdbDiscoverApicalypseQueryWithGenres() {
        val query = IgdbApiClient.buildDiscoverApicalypseQuery(
            includedTags = setOf("Fighting", "Shooter"),
            excludedTags = setOf("Racing"),
            adultMode = com.lx.tracklist.ui.search.AdultMode.OFF
        )
        // Square brackets [] indicate AND condition (must contain both genres 4 and 5)
        assertTrue("Query should include 'genres = [4,5]' for Fighting AND Shooter", query.contains("genres = [4,5]") || query.contains("genres = [5,4]"))
        // Parentheses () indicate NOT ANY (must not contain genre 10)
        assertTrue("Query should include 'genres != (10)' for Racing", query.contains("genres != (10)"))
        assertTrue("Query should exclude erotic theme for AdultMode.OFF", query.contains("themes != (42)"))

        val adultOnlyQuery = IgdbApiClient.buildDiscoverApicalypseQuery(
            adultMode = com.lx.tracklist.ui.search.AdultMode.ONLY
        )
        assertTrue("Query should require erotic theme for AdultMode.ONLY", adultOnlyQuery.contains("themes = (42)"))
    }

    @Test
    fun testRawgDiscoverUrlWithGenres() {
        val url = RawgApiClient.buildDiscoverUrl(
            apiKey = "test_key",
            includedTags = setOf("Fighting"),
            adultMode = com.lx.tracklist.ui.search.AdultMode.ONLY
        )
        assertTrue("URL should include '&genres=fighting'", url.contains("&genres=fighting"))
        assertTrue("URL should include '&tags=nsfw' for AdultMode.ONLY", url.contains("&tags=nsfw"))
    }
}
