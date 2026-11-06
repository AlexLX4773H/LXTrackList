package com.lx.tracklist

import com.lx.tracklist.data.exporter.JsonExporter
import com.lx.tracklist.data.importer.CsvImporter
import com.lx.tracklist.data.importer.JsonImporter
import com.lx.tracklist.data.importer.MalXmlImporter
import com.lx.tracklist.data.importer.TachiyomiImporter
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.remote.JikanApiClient
import com.lx.tracklist.data.remote.JikanSyncManager
import com.lx.tracklist.ui.settings.ExportDestination
import com.lx.tracklist.ui.settings.ExportPreviewState
import com.lx.tracklist.ui.settings.ImportPreviewState
import com.lx.tracklist.ui.settings.buildCategoryGroups
import com.lx.tracklist.ui.settings.getCategoryKeyForEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class ImporterExporterTest {

    @Test
    fun testCsvImporter() {
        val csvData = """
            title,mediaType,status,score,currentEpisodes,totalEpisodes,notes,tags
            "Frieren: Beyond Journey's End",ANIME,COMPLETED,10,28,28,"Masterpiece","fantasy, adventure"
            "Solo Leveling",MANHWA,WATCHING,9,150,200,"Hype","action"
        """.trimIndent()

        val stream = ByteArrayInputStream(csvData.toByteArray(Charsets.UTF_8))
        val entries = CsvImporter.parse(stream)

        assertEquals(2, entries.size)

        val frieren = entries[0]
        assertEquals("Frieren: Beyond Journey's End", frieren.title)
        assertEquals(MediaType.ANIME, frieren.mediaType)
        assertEquals(MediaStatus.COMPLETED, frieren.status)
        assertEquals(10, frieren.score)
        assertEquals(28, frieren.currentEpisodes)
        assertEquals(28, frieren.totalEpisodes)
        assertEquals("Masterpiece", frieren.notes)
        assertEquals("fantasy, adventure", frieren.tags)

        val solo = entries[1]
        assertEquals("Solo Leveling", solo.title)
        assertEquals(MediaType.MANHWA, solo.mediaType)
    }

    @Test
    fun testCsvImporterWithSubCategory() {
        val csvData = """
            title,mediaType,status,subCategory
            "Nano Machine",MANHWA,WATCHING,"Murim"
            "Chainsaw Man",MANGA,WATCHING,"Favorites"
        """.trimIndent()

        val stream = ByteArrayInputStream(csvData.toByteArray(Charsets.UTF_8))
        val entries = CsvImporter.parse(stream)

        assertEquals(2, entries.size)
        assertEquals("Nano Machine", entries[0].title)
        assertEquals(MediaType.MANHWA, entries[0].mediaType)
        assertNull(entries[0].customCategory)
        assertEquals("Murim", entries[0].subCategory)

        assertEquals("Chainsaw Man", entries[1].title)
        assertEquals(MediaType.MANGA, entries[1].mediaType)
        assertNull(entries[1].customCategory)
        assertEquals("Favorites", entries[1].subCategory)
    }

    @Test
    fun testJsonExportAndImportRoundTrip() {
        val originalEntries = listOf(
            MediaEntry(
                title = "Steins;Gate",
                alternateTitle = "シュタインズ・ゲート",
                mediaType = MediaType.ANIME,
                status = MediaStatus.COMPLETED,
                score = 10,
                currentEpisodes = 24,
                totalEpisodes = 24,
                isFavorite = true,
                tags = "sci-fi, thriller",
                notes = "El Psy Kongroo"
            ),
            MediaEntry(
                title = "Berserk",
                mediaType = MediaType.MANGA,
                status = MediaStatus.WATCHING,
                currentChapters = 375,
                totalChapters = 0,
                trackingUnit = TrackingUnit.CHAPTERS,
                score = 10,
                isFavorite = true
            )
        )

        val output = ByteArrayOutputStream()
        JsonExporter.writeToStream(originalEntries, output)
        val jsonString = output.toString("UTF-8")

        assertNotNull(jsonString)
        val inputStream = ByteArrayInputStream(jsonString.toByteArray(Charsets.UTF_8))
        val imported = JsonImporter.parse(inputStream)

        assertEquals(2, imported.size)
        assertEquals("Steins;Gate", imported[0].title)
        assertEquals(10, imported[0].score)
        assertEquals(true, imported[0].isFavorite)
        assertEquals("Berserk", imported[1].title)
        assertEquals(375, imported[1].currentChapters)
    }

    @Test
    fun testTachiyomiImporterProtobufMangaAndManhwa() {
        val rootOut = ByteArrayOutputStream()

        // 1. Categories: 0 -> Reading, 1 -> Manhwa
        val cat0Out = ByteArrayOutputStream()
        writeStringField(cat0Out, 1, "Reading")
        writeVarintField(cat0Out, 2, 0L)
        writeMessageField(rootOut, 2, cat0Out.toByteArray())

        val cat1Out = ByteArrayOutputStream()
        writeStringField(cat1Out, 1, "Manhwa")
        writeVarintField(cat1Out, 2, 1L)
        writeMessageField(rootOut, 2, cat1Out.toByteArray())

        // 2. Sources: 1001 -> Asura Scans, 1002 -> MangaDex
        val src1Out = ByteArrayOutputStream()
        writeVarintField(src1Out, 1, 1001L)
        writeStringField(src1Out, 2, "Asura Scans")
        writeMessageField(rootOut, 101, src1Out.toByteArray())

        val src2Out = ByteArrayOutputStream()
        writeVarintField(src2Out, 1, 1002L)
        writeStringField(src2Out, 2, "MangaDex")
        writeMessageField(rootOut, 101, src2Out.toByteArray())

        // 3. Manga 1: Solo Leveling (Manhwa)
        val manga1Out = ByteArrayOutputStream()
        writeVarintField(manga1Out, 1, 1001L) // source = Asura Scans
        writeStringField(manga1Out, 2, "/manga/solo-leveling")
        writeStringField(manga1Out, 3, "Solo Leveling")
        writeStringField(manga1Out, 4, "DUBU")
        writeStringField(manga1Out, 5, "Chugong")
        writeStringField(manga1Out, 6, "In a world where hunters must battle deadly monsters...")
        writeStringField(manga1Out, 7, "Action")
        writeStringField(manga1Out, 7, "Fantasy")
        writeStringField(manga1Out, 7, "Manhwa")
        writeStringField(manga1Out, 7, "Webtoon")
        writeVarintField(manga1Out, 8, 1L) // Status: Completed
        writeStringField(manga1Out, 9, "https://example.com/covers/solo_leveling.jpg")
        writeVarintField(manga1Out, 13, 1600000000000L) // dateAdded
        writeVarintField(manga1Out, 17, 1L) // category: Manhwa

        // Manga 1 Chapters: 179 read
        for (ch in 1..5) {
            val chOut = ByteArrayOutputStream()
            writeStringField(chOut, 1, "/chapter/$ch")
            writeStringField(chOut, 2, "Chapter $ch")
            writeVarintField(chOut, 4, 1L) // read = true
            writeFloatField(chOut, 9, ch.toFloat())
            writeMessageField(manga1Out, 16, chOut.toByteArray())
        }

        // Manga 1 Tracking: MAL tracking
        val track1Out = ByteArrayOutputStream()
        writeVarintField(track1Out, 1, 1L) // syncId = 1 (MAL)
        writeVarintField(track1Out, 2, 121496L) // libraryId
        writeVarintField(track1Out, 3, 121496L) // mediaIdInt
        writeFloatField(track1Out, 6, 179f) // lastChapterRead = 179
        writeVarintField(track1Out, 7, 179L) // totalChapters = 179
        writeFloatField(track1Out, 8, 9.5f) // score = 9.5
        writeVarintField(track1Out, 9, 2L) // status = Completed in MAL
        writeMessageField(manga1Out, 18, track1Out.toByteArray())

        writeMessageField(rootOut, 1, manga1Out.toByteArray())

        // 4. Manga 2: Berserk (Manga)
        val manga2Out = ByteArrayOutputStream()
        writeVarintField(manga2Out, 1, 1002L) // source = MangaDex
        writeStringField(manga2Out, 2, "/manga/berserk")
        writeStringField(manga2Out, 3, "Berserk")
        writeStringField(manga2Out, 4, "Kentaro Miura")
        writeStringField(manga2Out, 5, "Kentaro Miura")
        writeStringField(manga2Out, 6, "Guts, a former mercenary known as the Black Swordsman...")
        writeStringField(manga2Out, 7, "Dark Fantasy")
        writeStringField(manga2Out, 7, "Seinen")
        writeStringField(manga2Out, 7, "Action")
        writeVarintField(manga2Out, 8, 2L) // Status: Ongoing
        writeStringField(manga2Out, 9, "https://example.com/covers/berserk.jpg")
        writeVarintField(manga2Out, 13, 1650000000000L)
        writeVarintField(manga2Out, 17, 0L) // category: Reading

        // Manga 2 Chapters: ch 1 read, ch 2 read, ch 3 unread
        val bch1 = ByteArrayOutputStream()
        writeStringField(bch1, 1, "/ch/1")
        writeStringField(bch1, 2, "Chapter 1")
        writeVarintField(bch1, 4, 1L) // read = true
        writeFloatField(bch1, 9, 1f)
        writeMessageField(manga2Out, 16, bch1.toByteArray())

        val bch2 = ByteArrayOutputStream()
        writeStringField(bch2, 1, "/ch/2")
        writeStringField(bch2, 2, "Chapter 2")
        writeVarintField(bch2, 4, 1L) // read = true
        writeFloatField(bch2, 9, 2f)
        writeMessageField(manga2Out, 16, bch2.toByteArray())

        val bch3 = ByteArrayOutputStream()
        writeStringField(bch3, 1, "/ch/3")
        writeStringField(bch3, 2, "Chapter 3")
        writeVarintField(bch3, 4, 0L) // read = false
        writeFloatField(bch3, 9, 3f)
        writeMessageField(manga2Out, 16, bch3.toByteArray())

        // Manga 2 Tracking: AniList tracking with 375 chapters read
        val track2Out = ByteArrayOutputStream()
        writeVarintField(track2Out, 1, 2L) // syncId = 2 (AniList)
        writeVarintField(track2Out, 2, 30002L)
        writeVarintField(track2Out, 3, 30002L)
        writeFloatField(track2Out, 6, 375f) // lastChapterRead
        writeVarintField(track2Out, 7, 0L) // ongoing total = 0
        writeFloatField(track2Out, 8, 100f) // AniList 100 scale -> score 10
        writeVarintField(track2Out, 9, 1L) // status = 1 (Current / Reading)
        writeMessageField(manga2Out, 18, track2Out.toByteArray())

        writeMessageField(rootOut, 1, manga2Out.toByteArray())

        // Gzip compress the protobuf stream to simulate a real .tachibk file
        val tachibkBytes = gzipBytes(rootOut.toByteArray())
        val entries = TachiyomiImporter.parse(ByteArrayInputStream(tachibkBytes))

        assertEquals(2, entries.size)

        // Validate Solo Leveling (Manhwa)
        val solo = entries[0]
        assertEquals("Solo Leveling", solo.title)
        assertEquals(MediaType.MANHWA, solo.mediaType)
        assertEquals(MediaStatus.COMPLETED, solo.status)
        assertEquals(10, solo.score) // 9.5 rounded
        assertEquals(179, solo.currentChapters)
        assertEquals(179, solo.totalChapters)
        assertEquals(TrackingUnit.CHAPTERS, solo.trackingUnit)
        assertEquals("MAL", solo.externalSource)
        assertEquals("121496", solo.externalId)
        assertEquals("https://example.com/covers/solo_leveling.jpg", solo.coverImageUri)
        assertTrue(solo.tags?.contains("Manhwa") == true)
        assertTrue(solo.tags?.contains("Webtoon") == true)
        assertTrue(solo.notes?.contains("Author: Chugong • Artist: DUBU") == true)

        // Validate Berserk (Manga)
        val berserk = entries[1]
        assertEquals("Berserk", berserk.title)
        assertEquals(MediaType.MANGA, berserk.mediaType)
        assertEquals(MediaStatus.WATCHING, berserk.status) // Watching = Reading
        assertEquals(10, berserk.score) // 100 / 10 = 10
        assertEquals(375, berserk.currentChapters)
        assertEquals(0, berserk.totalChapters)
        assertEquals(TrackingUnit.CHAPTERS, berserk.trackingUnit)
        assertEquals("ANILIST", berserk.externalSource)
        assertEquals("30002", berserk.externalId)
        assertTrue(berserk.tags?.contains("Dark Fantasy") == true)
        assertTrue(berserk.notes?.contains("Author: Kentaro Miura") == true)
    }

    @Test
    fun testTachiyomiImporterFavoritesOption() {
        val rootOut = ByteArrayOutputStream()
        val mangaOut = ByteArrayOutputStream()
        writeStringField(mangaOut, 3, "Test Manga")
        writeVarintField(mangaOut, 100, 1L) // favorite = true
        writeVarintField(mangaOut, 13, 1600000000000L) // dateAdded > 0
        writeMessageField(rootOut, 1, mangaOut.toByteArray())

        val tachibkBytes = gzipBytes(rootOut.toByteArray())

        // Default: importAsFavorites = false -> isFavorite should be false
        val defaultEntries = TachiyomiImporter.parse(ByteArrayInputStream(tachibkBytes))
        assertEquals(1, defaultEntries.size)
        assertFalse(defaultEntries[0].isFavorite)

        // Explicit: importAsFavorites = false -> isFavorite should be false
        val normalEntries = TachiyomiImporter.parse(ByteArrayInputStream(tachibkBytes), importAsFavorites = false)
        assertEquals(1, normalEntries.size)
        assertFalse(normalEntries[0].isFavorite)

        // Explicit: importAsFavorites = true -> isFavorite should be true
        val favoriteEntries = TachiyomiImporter.parse(ByteArrayInputStream(tachibkBytes), importAsFavorites = true)
        assertEquals(1, favoriteEntries.size)
        assertTrue(favoriteEntries[0].isFavorite)
    }

    @Test
    fun testTachiyomiImporterJsonFallback() {
        val jsonData = """
            {
              "backupCategories": [
                { "name": "Plan to Read", "order": 0 },
                { "name": "Manhwa", "order": 1 }
              ],
              "backupManga": [
                {
                  "title": "Tower of God",
                  "genre": ["Action", "Fantasy", "Manhwa", "Webtoon"],
                  "status": 2,
                  "categories": [1],
                  "chapters": [
                    { "chapterNumber": 1.0, "read": true },
                    { "chapterNumber": 2.0, "read": true },
                    { "chapterNumber": 3.0, "read": false }
                  ]
                },
                {
                  "title": "Vagabond",
                  "genre": ["Action", "Historical", "Samurai"],
                  "status": 3,
                  "categories": [0],
                  "chapters": [
                    { "chapterNumber": 1.0, "read": false }
                  ]
                }
              ]
            }
        """.trimIndent()

        val gzipped = gzipBytes(jsonData.toByteArray(Charsets.UTF_8))
        val entries = TachiyomiImporter.parse(ByteArrayInputStream(gzipped))

        assertEquals(2, entries.size)

        val tog = entries[0]
        assertEquals("Tower of God", tog.title)
        assertEquals(MediaType.MANHWA, tog.mediaType)
        assertEquals(MediaStatus.WATCHING, tog.status)
        assertEquals(2, tog.currentChapters)

        val vagabond = entries[1]
        assertEquals("Vagabond", vagabond.title)
        assertEquals(MediaType.MANGA, vagabond.mediaType)
        assertEquals(MediaStatus.PLAN_TO_WATCH, vagabond.status)
        assertEquals(0, vagabond.currentChapters)
    }

    @Test
    fun testTachiyomiImporterEmptyAndCorrupted() {
        assertEquals(0, TachiyomiImporter.parse(ByteArrayInputStream(ByteArray(0))).size)
        assertEquals(0, TachiyomiImporter.parse(ByteArrayInputStream(byteArrayOf(0x01, 0x02, 0x03))).size)
    }

    @Test
    fun testTachiyomiCustomCategoriesInProtobuf() {
        val rootOut = ByteArrayOutputStream()

        // Categories:
        // 0 -> Reading (status)
        val cat0Out = ByteArrayOutputStream()
        writeStringField(cat0Out, 1, "Reading")
        writeVarintField(cat0Out, 2, 0L)
        writeMessageField(rootOut, 2, cat0Out.toByteArray())

        // 1 -> Manhwa (built-in)
        val cat1Out = ByteArrayOutputStream()
        writeStringField(cat1Out, 1, "Manhwa")
        writeVarintField(cat1Out, 2, 1L)
        writeMessageField(rootOut, 2, cat1Out.toByteArray())

        // 2 -> Web Novels (custom category)
        val cat2Out = ByteArrayOutputStream()
        writeStringField(cat2Out, 1, "Web Novels")
        writeVarintField(cat2Out, 2, 2L)
        writeMessageField(rootOut, 2, cat2Out.toByteArray())

        // 3 -> Donghua (custom category)
        val cat3Out = ByteArrayOutputStream()
        writeStringField(cat3Out, 1, "Donghua")
        writeVarintField(cat3Out, 2, 3L)
        writeMessageField(rootOut, 2, cat3Out.toByteArray())

        // Manga 1: Solo Leveling (Manhwa built-in)
        val m1Out = ByteArrayOutputStream()
        writeStringField(m1Out, 3, "Solo Leveling")
        writeVarintField(m1Out, 17, 1L) // category 1 (Manhwa)
        writeMessageField(rootOut, 1, m1Out.toByteArray())

        // Manga 2: Lord of the Mysteries (Web Novels custom)
        val m2Out = ByteArrayOutputStream()
        writeStringField(m2Out, 3, "Lord of the Mysteries")
        writeVarintField(m2Out, 17, 2L) // category 2 (Web Novels)
        writeMessageField(rootOut, 1, m2Out.toByteArray())

        // Manga 3: Soul Land (Donghua custom)
        val m3Out = ByteArrayOutputStream()
        writeStringField(m3Out, 3, "Soul Land")
        writeVarintField(m3Out, 17, 3L) // category 3 (Donghua)
        writeMessageField(rootOut, 1, m3Out.toByteArray())

        val gzipped = gzipBytes(rootOut.toByteArray())
        val entries = TachiyomiImporter.parse(ByteArrayInputStream(gzipped))

        assertEquals(3, entries.size)

        val solo = entries[0]
        assertEquals("Solo Leveling", solo.title)
        assertEquals(MediaType.MANHWA, solo.mediaType)
        assertNull(solo.customCategory)

        val lotm = entries[1]
        assertEquals("Lord of the Mysteries", lotm.title)
        assertNull(lotm.customCategory)
        assertEquals("Web Novels", lotm.subCategory)
        assertEquals(MediaType.MANGA, lotm.mediaType)

        val soulLand = entries[2]
        assertEquals("Soul Land", soulLand.title)
        assertNull(soulLand.customCategory)
        assertEquals("Donghua", soulLand.subCategory)
        assertEquals(MediaType.MANGA, soulLand.mediaType)
    }

    @Test
    fun testCategoryGroupingAndSelection() {
        val entries = listOf(
            MediaEntry(title = "Attack on Titan", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED),
            MediaEntry(title = "Bleach", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING),
            MediaEntry(title = "Berserk", mediaType = MediaType.MANGA, status = MediaStatus.WATCHING),
            MediaEntry(title = "Kingdom", mediaType = MediaType.MANGA, status = MediaStatus.WATCHING),
            MediaEntry(title = "One Piece", mediaType = MediaType.MANGA, status = MediaStatus.WATCHING),
            MediaEntry(title = "Battle Through The Heavens", mediaType = MediaType.ANIME, customCategory = "Donghua", status = MediaStatus.WATCHING),
            MediaEntry(title = "Shadow Slave", mediaType = MediaType.BOOK, customCategory = "Web Novels", status = MediaStatus.WATCHING),
            MediaEntry(title = "Reverend Insanity", mediaType = MediaType.BOOK, customCategory = "Web Novels", status = MediaStatus.WATCHING)
        )

        val groups = buildCategoryGroups(entries)
        assertEquals(4, groups.size)

        val animeGroup = groups.first { it.key == "TYPE:ANIME" }
        assertEquals("Anime", animeGroup.displayName)
        assertEquals(false, animeGroup.isCustomCategory)
        assertEquals(2, animeGroup.count)

        val mangaGroup = groups.first { it.key == "TYPE:MANGA" }
        assertEquals("Manga", mangaGroup.displayName)
        assertEquals(false, mangaGroup.isCustomCategory)
        assertEquals(3, mangaGroup.count)

        val donghuaGroup = groups.first { it.key == "CUSTOM:Donghua" }
        assertEquals("Donghua", donghuaGroup.displayName)
        assertEquals(true, donghuaGroup.isCustomCategory)
        assertEquals(1, donghuaGroup.count)

        val novelGroup = groups.first { it.key == "CUSTOM:Web Novels" }
        assertEquals("Web Novels", novelGroup.displayName)
        assertEquals(true, novelGroup.isCustomCategory)
        assertEquals(2, novelGroup.count)

        // Test Preview State with partial selection
        val allKeys = groups.map { it.key }.toSet()
        val preview = ImportPreviewState(
            sourceName = "test.tachibk",
            allEntries = entries,
            categoryGroups = groups,
            selectedCategoryKeys = setOf("TYPE:MANGA", "CUSTOM:Donghua")
        )

        assertEquals(8, preview.totalCount)
        assertEquals(4, preview.selectedCount) // 3 Manga + 1 Donghua

        val filteredToImport = preview.allEntries.filter {
            preview.selectedCategoryKeys.contains(getCategoryKeyForEntry(it))
        }
        assertEquals(4, filteredToImport.size)
        assertTrue(filteredToImport.any { it.title == "Berserk" })
        assertTrue(filteredToImport.any { it.title == "Kingdom" })
        assertTrue(filteredToImport.any { it.title == "One Piece" })
        assertTrue(filteredToImport.any { it.title == "Battle Through The Heavens" })
        assertTrue(filteredToImport.none { it.title == "Attack on Titan" })
        assertTrue(filteredToImport.none { it.title == "Shadow Slave" })
    }

    @Test
    fun testExportPreviewGroupingAndFiltering() {
        val libraryEntries = listOf(
            MediaEntry(title = "Demon Slayer", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED),
            MediaEntry(title = "Jujutsu Kaisen", mediaType = MediaType.ANIME, status = MediaStatus.WATCHING),
            MediaEntry(title = "Tower of God", mediaType = MediaType.MANHWA, status = MediaStatus.WATCHING),
            MediaEntry(title = "Omniscient Reader", mediaType = MediaType.MANHWA, status = MediaStatus.WATCHING),
            MediaEntry(title = "Martial Peak", mediaType = MediaType.MANGA, customCategory = "Manhua", status = MediaStatus.WATCHING),
            MediaEntry(title = "Apotheosis", mediaType = MediaType.MANGA, customCategory = "Manhua", status = MediaStatus.WATCHING),
            MediaEntry(title = "Tales of Demons and Gods", mediaType = MediaType.MANGA, customCategory = "Manhua", status = MediaStatus.WATCHING)
        )

        val groups = buildCategoryGroups(libraryEntries)
        assertEquals(3, groups.size)

        val animeGrp = groups.first { it.key == "TYPE:ANIME" }
        assertEquals(2, animeGrp.count)

        val manhwaGrp = groups.first { it.key == "TYPE:MANHWA" }
        assertEquals(2, manhwaGrp.count)

        val manhuaGrp = groups.first { it.key == "CUSTOM:Manhua" }
        assertEquals(3, manhuaGrp.count)
        assertTrue(manhuaGrp.isCustomCategory)

        // Select only Manhua custom category for export
        val exportPreview = ExportPreviewState(
            destination = ExportDestination.CUSTOM_FILE,
            allEntries = libraryEntries,
            categoryGroups = groups,
            selectedCategoryKeys = setOf("CUSTOM:Manhua")
        )

        assertEquals(7, exportPreview.totalCount)
        assertEquals(3, exportPreview.selectedCount)

        val filtered = exportPreview.getFilteredEntries()
        assertEquals(3, filtered.size)
        assertTrue(filtered.all { it.customCategory == "Manhua" })
        assertTrue(filtered.none { it.mediaType == MediaType.ANIME })
        assertTrue(filtered.none { it.mediaType == MediaType.MANHWA })
    }

    @Test
    fun testTachiyomiCategoriesImportedAsSubCategories() {
        val rootOut = ByteArrayOutputStream()

        // 1. Categories in .tachibk:
        // 0 -> Reading
        val cat0Out = ByteArrayOutputStream()
        writeStringField(cat0Out, 1, "Reading")
        writeVarintField(cat0Out, 2, 0L)
        writeMessageField(rootOut, 2, cat0Out.toByteArray())

        // 1 -> Completed
        val cat1Out = ByteArrayOutputStream()
        writeStringField(cat1Out, 1, "Completed")
        writeVarintField(cat1Out, 2, 1L)
        writeMessageField(rootOut, 2, cat1Out.toByteArray())

        // 2 -> Favorites
        val cat2Out = ByteArrayOutputStream()
        writeStringField(cat2Out, 1, "Favorites")
        writeVarintField(cat2Out, 2, 2L)
        writeMessageField(rootOut, 2, cat2Out.toByteArray())

        // Manga 1: Berserk (Reading category)
        val m1 = ByteArrayOutputStream()
        writeStringField(m1, 3, "Berserk")
        writeVarintField(m1, 17, 0L) // category 0 = Reading
        writeMessageField(rootOut, 1, m1.toByteArray())

        // Manga 2: Attack on Titan (Completed category)
        val m2 = ByteArrayOutputStream()
        writeStringField(m2, 3, "Attack on Titan")
        writeVarintField(m2, 17, 1L) // category 1 = Completed
        writeMessageField(rootOut, 1, m2.toByteArray())

        // Manga 3: Solo Leveling (Favorites category, manhwa)
        val m3 = ByteArrayOutputStream()
        writeStringField(m3, 3, "Solo Leveling (Manhwa)")
        writeVarintField(m3, 17, 2L) // category 2 = Favorites
        writeMessageField(rootOut, 1, m3.toByteArray())

        val tachibkBytes = gzipBytes(rootOut.toByteArray())
        val entries = TachiyomiImporter.parse(ByteArrayInputStream(tachibkBytes))

        assertEquals(3, entries.size)

        // Manga 1 should have subCategory = "Reading"
        assertEquals("Berserk", entries[0].title)
        assertEquals("Reading", entries[0].subCategory)
        assertEquals(MediaType.MANGA, entries[0].mediaType)

        // Manga 2 should have subCategory = "Completed"
        assertEquals("Attack on Titan", entries[1].title)
        assertEquals("Completed", entries[1].subCategory)
        assertEquals(MediaType.MANGA, entries[1].mediaType)

        // Manga 3 should have subCategory = "Favorites"
        assertEquals("Solo Leveling (Manhwa)", entries[2].title)
        assertEquals("Favorites", entries[2].subCategory)
        assertEquals(MediaType.MANHWA, entries[2].mediaType)
    }

    @Test
    fun testImportPreviewGroupsWithSubCategories() {
        val entries = listOf(
            MediaEntry(title = "Berserk", mediaType = MediaType.MANGA, subCategory = "Reading", status = MediaStatus.WATCHING),
            MediaEntry(title = "Kingdom", mediaType = MediaType.MANGA, subCategory = "Reading", status = MediaStatus.WATCHING),
            MediaEntry(title = "Attack on Titan", mediaType = MediaType.MANGA, subCategory = "Completed", status = MediaStatus.COMPLETED),
            MediaEntry(title = "Monster", mediaType = MediaType.MANGA, subCategory = null, status = MediaStatus.COMPLETED),
            MediaEntry(title = "Solo Leveling", mediaType = MediaType.MANHWA, subCategory = "Favorites", status = MediaStatus.COMPLETED)
        )

        val groups = buildCategoryGroups(entries)
        assertEquals(4, groups.size)

        val readingGroup = groups.first { it.key == "TYPE:MANGA:SUB:Reading" }
        assertEquals("Reading", readingGroup.displayName)
        assertEquals("Manga", readingGroup.parentCategoryName)
        assertTrue(readingGroup.isSubCategory)
        assertEquals(2, readingGroup.count)

        val completedGroup = groups.first { it.key == "TYPE:MANGA:SUB:Completed" }
        assertEquals("Completed", completedGroup.displayName)
        assertEquals("Manga", completedGroup.parentCategoryName)
        assertTrue(completedGroup.isSubCategory)
        assertEquals(1, completedGroup.count)

        val uncategorizedManga = groups.first { it.key == "TYPE:MANGA" }
        assertEquals("Manga (Uncategorized)", uncategorizedManga.displayName)
        org.junit.Assert.assertFalse(uncategorizedManga.isSubCategory)
        assertEquals(1, uncategorizedManga.count)

        val manhwaFavorites = groups.first { it.key == "TYPE:MANHWA:SUB:Favorites" }
        assertEquals("Favorites", manhwaFavorites.displayName)
        assertEquals("Manhwa", manhwaFavorites.parentCategoryName)
        assertTrue(manhwaFavorites.isSubCategory)
        assertEquals(1, manhwaFavorites.count)

        // Test filtering by selecting only "Reading" subcategory
        val preview = ImportPreviewState(
            sourceName = "tachiyomi.tachibk",
            allEntries = entries,
            categoryGroups = groups,
            selectedCategoryKeys = setOf("TYPE:MANGA:SUB:Reading")
        )

        assertEquals(5, preview.totalCount)
        assertEquals(2, preview.selectedCount)

        val filtered = preview.allEntries.filter {
            preview.selectedCategoryKeys.contains(getCategoryKeyForEntry(it))
        }
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.subCategory == "Reading" })
    }

    @Test
    fun testMalXmlImporterDatesAndRewatch() {
        val xmlData = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <myanimelist>
                <myinfo>
                    <user_id>12345</user_id>
                    <user_name>Alex</user_name>
                    <user_export_type>1</user_export_type>
                </myinfo>
                <anime>
                    <series_animedb_id>5114</series_animedb_id>
                    <series_title><![CDATA[Fullmetal Alchemist: Brotherhood]]></series_title>
                    <series_type>TV</series_type>
                    <series_episodes>64</series_episodes>
                    <my_watched_episodes>64</my_watched_episodes>
                    <my_start_date>2023-01-15</my_start_date>
                    <my_finish_date>2023-04-20</my_finish_date>
                    <my_score>10</my_score>
                    <my_status>2</my_status>
                    <my_rewatching>1</my_rewatching>
                    <my_times_watched>2</my_times_watched>
                    <my_tags><![CDATA[shounen, military]]></my_tags>
                </anime>
                <anime>
                    <series_animedb_id>16498</series_animedb_id>
                    <series_title>Attack on Titan</series_title>
                    <series_type>TV</series_type>
                    <series_episodes>25</series_episodes>
                    <my_watched_episodes>10</my_watched_episodes>
                    <my_start_date>0000-00-00</my_start_date>
                    <my_finish_date />
                    <my_score>9</my_score>
                    <my_status>1</my_status>
                    <my_rewatching>0</my_rewatching>
                    <my_times_watched>0</my_times_watched>
                </anime>
                <manga>
                    <manga_mangadb_id>11</manga_mangadb_id>
                    <manga_title>Naruto</manga_title>
                    <manga_volumes>72</manga_volumes>
                    <manga_chapters>700</manga_chapters>
                    <my_read_volumes>72</my_read_volumes>
                    <my_read_chapters>700</my_read_chapters>
                    <my_start_date>2021-05-10</my_start_date>
                    <my_finish_date>2022-02-14</my_finish_date>
                    <my_score>8</my_score>
                    <my_status>2</my_status>
                    <my_rereadingg>0</my_rereadingg>
                    <my_times_read>3</my_times_read>
                </manga>
            </myanimelist>
        """.trimIndent()

        val stream = ByteArrayInputStream(xmlData.toByteArray(Charsets.UTF_8))
        val entries = MalXmlImporter.parse(stream)

        assertEquals(3, entries.size)

        // 1. FMAB
        val fmab = entries[0]
        assertEquals("Fullmetal Alchemist: Brotherhood", fmab.title)
        assertEquals("5114", fmab.externalId)
        assertEquals("5114", fmab.foreignDatabaseId)
        assertEquals("MAL", fmab.externalSource)
        assertEquals("MAL", fmab.foreignDatabaseSource)
        assertEquals(MediaType.ANIME, fmab.mediaType)
        assertEquals(MediaStatus.COMPLETED, fmab.status)
        assertEquals(10, fmab.score)
        assertEquals(64, fmab.currentEpisodes)
        assertEquals(64, fmab.totalEpisodes)
        assertEquals(true, fmab.isRewatching)
        assertEquals(2, fmab.rewatchCount)
        assertNotNull(fmab.startDate)
        assertNotNull(fmab.finishDate)
        assertTrue(fmab.finishDate!! > fmab.startDate!!)

        // 2. AOT (Zero/Empty dates)
        val aot = entries[1]
        assertEquals("Attack on Titan", aot.title)
        assertEquals("16498", aot.externalId)
        assertEquals("MAL", aot.externalSource)
        assertEquals(MediaType.ANIME, aot.mediaType)
        assertEquals(MediaStatus.WATCHING, aot.status)
        assertEquals(9, aot.score)
        assertEquals(10, aot.currentEpisodes)
        assertEquals(25, aot.totalEpisodes)
        assertEquals(false, aot.isRewatching)
        assertEquals(0, aot.rewatchCount)
        assertNull(aot.startDate)
        assertNull(aot.finishDate)

        // 3. Naruto (Manga)
        val naruto = entries[2]
        assertEquals("Naruto", naruto.title)
        assertEquals("11", naruto.externalId)
        assertEquals("MAL", naruto.externalSource)
        assertEquals(MediaType.MANGA, naruto.mediaType)
        assertEquals(MediaStatus.COMPLETED, naruto.status)
        assertEquals(8, naruto.score)
        assertEquals(700, naruto.currentChapters)
        assertEquals(700, naruto.totalChapters)
        assertEquals(false, naruto.isRewatching)
        assertEquals(3, naruto.rewatchCount)
        assertNotNull(naruto.startDate)
        assertNotNull(naruto.finishDate)
    }

    @Test
    fun testCsvImporterWithForeignDbAndRewatch() {
        val csvData = """
            title,mediaType,status,score,foreign_database_id,foreign_database_source,rewatch_count,is_rewatching,runtime
            "Death Note",ANIME,COMPLETED,9,"1535","MAL",2,true,23
            "Spirited Away",MOVIE,COMPLETED,10,"199","MAL",4,false,125
        """.trimIndent()

        val stream = ByteArrayInputStream(csvData.toByteArray(Charsets.UTF_8))
        val entries = CsvImporter.parse(stream)

        assertEquals(2, entries.size)

        val deathNote = entries[0]
        assertEquals("Death Note", deathNote.title)
        assertEquals(MediaType.ANIME, deathNote.mediaType)
        assertEquals(MediaStatus.COMPLETED, deathNote.status)
        assertEquals("1535", deathNote.foreignDatabaseId)
        assertEquals("MAL", deathNote.foreignDatabaseSource)
        assertEquals(2, deathNote.rewatchCount)
        assertEquals(true, deathNote.isRewatching)
        assertEquals(23, deathNote.episodeLengthMinutes)
        assertNull(deathNote.durationMinutes)

        val spirited = entries[1]
        assertEquals("Spirited Away", spirited.title)
        assertEquals(MediaType.MOVIE, spirited.mediaType)
        assertEquals("199", spirited.foreignDatabaseId)
        assertEquals("MAL", spirited.foreignDatabaseSource)
        assertEquals(4, spirited.rewatchCount)
        assertEquals(false, spirited.isRewatching)
        assertEquals(125, spirited.durationMinutes)
        assertNull(spirited.episodeLengthMinutes)
    }

    @Test
    fun testJikanDurationMinutesParsing() {
        assertEquals(24, JikanApiClient.parseDurationMinutes("24 min per ep"))
        assertEquals(110, JikanApiClient.parseDurationMinutes("1 hr 50 min"))
        assertEquals(60, JikanApiClient.parseDurationMinutes("1 hr"))
        assertEquals(120, JikanApiClient.parseDurationMinutes("2 hours"))
        assertEquals(45, JikanApiClient.parseDurationMinutes("45 min"))
        assertEquals(83, JikanApiClient.parseDurationMinutes("1 hour 23 mins"))
        assertNull(JikanApiClient.parseDurationMinutes("Unknown"))
        assertNull(JikanApiClient.parseDurationMinutes(""))
        assertNull(JikanApiClient.parseDurationMinutes(null))

        assertEquals("Attack on Titan", JikanApiClient.cleanSearchTitle("Attack on Titan [1080p]"))
        assertEquals("One Piece", JikanApiClient.cleanSearchTitle("One Piece (1999)"))
        assertEquals("Naruto", JikanApiClient.cleanSearchTitle("Naruto [Dub] (TV)"))
        assertEquals("Bleach", JikanApiClient.cleanSearchTitle("Bleach"))
        assertEquals("[Official]", JikanApiClient.cleanSearchTitle("[Official]"))
    }

    @Test
    fun testJikanSyncManagerEligibility() {
        val animeMal = MediaEntry(title = "Anime", mediaType = MediaType.ANIME, status = MediaStatus.COMPLETED, externalId = "5114", externalSource = "MAL")
        val mangaMal = MediaEntry(title = "Manga", mediaType = MediaType.MANGA, status = MediaStatus.WATCHING, externalId = "11", externalSource = "MAL")
        val manhwaMal = MediaEntry(title = "Manhwa", mediaType = MediaType.MANHWA, status = MediaStatus.WATCHING, externalId = "121496", externalSource = "MyAnimeList")
        val movieMal = MediaEntry(title = "Movie", mediaType = MediaType.MOVIE, status = MediaStatus.COMPLETED, externalId = "199", externalSource = "mal")
        val bookMal = MediaEntry(title = "Novel", mediaType = MediaType.BOOK, status = MediaStatus.WATCHING, externalId = "3000", externalSource = null)
        val anilistEntry = MediaEntry(title = "Anilist", mediaType = MediaType.MANGA, status = MediaStatus.WATCHING, externalId = "30002", externalSource = "ANILIST")

        // Title-only entries without an ID yet
        val titleOnlyAnime = MediaEntry(title = "Frieren", mediaType = MediaType.ANIME, status = MediaStatus.PLAN_TO_WATCH, externalId = null, externalSource = null)
        val titleOnlyManga = MediaEntry(title = "Berserk", mediaType = MediaType.MANGA, status = MediaStatus.WATCHING, externalId = null, externalSource = null)
        val titleOnlyCustomSource = MediaEntry(title = "Custom", mediaType = MediaType.ANIME, status = MediaStatus.PLAN_TO_WATCH, externalId = null, externalSource = "CUSTOM_DB")

        assertTrue(JikanSyncManager.isMalSource(animeMal))
        assertTrue(JikanSyncManager.isMalSource(mangaMal))
        assertTrue(JikanSyncManager.isMalSource(manhwaMal))
        assertTrue(JikanSyncManager.isMalSource(movieMal))
        assertTrue(JikanSyncManager.isMalSource(bookMal))
        assertFalse(JikanSyncManager.isMalSource(anilistEntry))

        // Title search eligibility
        assertTrue(JikanSyncManager.isMalSource(titleOnlyAnime))
        assertTrue(JikanSyncManager.isMalSource(titleOnlyManga))
        assertFalse(JikanSyncManager.isMalSource(titleOnlyCustomSource))

        assertTrue(JikanSyncManager.isMangaType(MediaType.MANGA))
        assertTrue(JikanSyncManager.isMangaType(MediaType.MANHWA))
        assertTrue(JikanSyncManager.isMangaType(MediaType.BOOK))
        assertFalse(JikanSyncManager.isMangaType(MediaType.ANIME))
        assertFalse(JikanSyncManager.isMangaType(MediaType.MOVIE))
        assertFalse(JikanSyncManager.isMangaType(MediaType.SHOW))
    }

    private fun writeVarint(out: ByteArrayOutputStream, value: Long) {
        var v = value
        while ((v and 0x7FL.inv()) != 0L) {
            out.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        out.write((v and 0x7F).toInt())
    }

    private fun writeStringField(out: ByteArrayOutputStream, fieldNumber: Int, value: String) {
        val b = value.toByteArray(Charsets.UTF_8)
        writeVarint(out, ((fieldNumber shl 3) or 2).toLong())
        writeVarint(out, b.size.toLong())
        out.write(b)
    }

    private fun writeVarintField(out: ByteArrayOutputStream, fieldNumber: Int, value: Long) {
        writeVarint(out, ((fieldNumber shl 3) or 0).toLong())
        writeVarint(out, value)
    }

    private fun writeFloatField(out: ByteArrayOutputStream, fieldNumber: Int, value: Float) {
        writeVarint(out, ((fieldNumber shl 3) or 5).toLong())
        val bits = java.lang.Float.floatToIntBits(value)
        out.write(bits and 0xFF)
        out.write((bits ushr 8) and 0xFF)
        out.write((bits ushr 16) and 0xFF)
        out.write((bits ushr 24) and 0xFF)
    }

    private fun writeMessageField(out: ByteArrayOutputStream, fieldNumber: Int, messageBytes: ByteArray) {
        writeVarint(out, ((fieldNumber shl 3) or 2).toLong())
        writeVarint(out, messageBytes.size.toLong())
        out.write(messageBytes)
    }

    private fun gzipBytes(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }
}
