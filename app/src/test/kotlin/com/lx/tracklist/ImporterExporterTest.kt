package com.lx.tracklist

import com.lx.tracklist.data.exporter.JsonExporter
import com.lx.tracklist.data.importer.CsvImporter
import com.lx.tracklist.data.importer.JsonImporter
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.TrackingUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

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
}
