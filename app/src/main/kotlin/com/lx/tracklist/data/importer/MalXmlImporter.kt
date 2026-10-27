package com.lx.tracklist.data.importer

import android.util.Xml
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.Priority
import com.lx.tracklist.data.model.TrackingUnit
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.GZIPInputStream

object MalXmlImporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun parse(inputStream: InputStream): List<MediaEntry> {
        val buffered = BufferedInputStream(inputStream)
        buffered.mark(2)
        val magic = ByteArray(2)
        val read = buffered.read(magic)
        buffered.reset()

        val stream = if (read == 2 && magic[0] == 0x1f.toByte() && magic[1] == 0x8b.toByte()) {
            GZIPInputStream(buffered)
        } else {
            buffered
        }

        val entries = mutableListOf<MediaEntry>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "anime" -> {
                        parseAnime(parser)?.let { entries.add(it) }
                    }
                    "manga" -> {
                        parseManga(parser)?.let { entries.add(it) }
                    }
                }
            }
            eventType = parser.next()
        }

        return entries
    }

    private fun parseAnime(parser: XmlPullParser): MediaEntry? {
        var externalId: String? = null
        var title: String? = null
        var seriesType: String? = null
        var totalEpisodes = 0
        var watchedEpisodes = 0
        var score: Int? = null
        var status = MediaStatus.WATCHING
        var startDate: Long? = null
        var finishDate: Long? = null
        var comments: String? = null
        var tags: String? = null

        while (parser.next() != XmlPullParser.END_TAG || parser.name != "anime") {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "series_animedb_id" -> externalId = readText(parser)
                "series_title" -> title = readText(parser)
                "series_type" -> seriesType = readText(parser)
                "series_episodes" -> totalEpisodes = readText(parser)?.toIntOrNull() ?: 0
                "my_watched_episodes" -> watchedEpisodes = readText(parser)?.toIntOrNull() ?: 0
                "my_score" -> score = readText(parser)?.toIntOrNull()?.takeIf { it in 1..10 }
                "my_status" -> {
                    val raw = readText(parser) ?: ""
                    status = raw.toIntOrNull()?.let { MediaStatus.fromMalStatus(it) }
                        ?: MediaStatus.fromString(raw)
                }
                "my_start_date" -> startDate = parseDate(readText(parser))
                "my_finish_date" -> finishDate = parseDate(readText(parser))
                "my_comments" -> comments = readText(parser)
                "my_tags" -> tags = readText(parser)
                else -> skip(parser)
            }
        }

        if (title.isNullOrBlank()) return null

        val mediaType = if (seriesType.equals("Movie", ignoreCase = true)) MediaType.MOVIE else MediaType.ANIME

        return MediaEntry(
            title = title,
            mediaType = mediaType,
            status = status,
            score = score,
            currentEpisodes = watchedEpisodes,
            totalEpisodes = totalEpisodes,
            startDate = startDate,
            finishDate = finishDate,
            notes = comments?.takeIf { it.isNotBlank() },
            tags = tags?.takeIf { it.isNotBlank() },
            externalId = externalId,
            externalSource = "MAL",
            priority = Priority.MEDIUM
        )
    }

    private fun parseManga(parser: XmlPullParser): MediaEntry? {
        var externalId: String? = null
        var title: String? = null
        var totalVolumes = 0
        var totalChapters = 0
        var readVolumes = 0
        var readChapters = 0
        var score: Int? = null
        var status = MediaStatus.WATCHING
        var startDate: Long? = null
        var finishDate: Long? = null
        var comments: String? = null
        var tags: String? = null

        while (parser.next() != XmlPullParser.END_TAG || parser.name != "manga") {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            when (parser.name) {
                "manga_mangadb_id" -> externalId = readText(parser)
                "manga_title" -> title = readText(parser)
                "manga_volumes" -> totalVolumes = readText(parser)?.toIntOrNull() ?: 0
                "manga_chapters" -> totalChapters = readText(parser)?.toIntOrNull() ?: 0
                "my_read_volumes" -> readVolumes = readText(parser)?.toIntOrNull() ?: 0
                "my_read_chapters" -> readChapters = readText(parser)?.toIntOrNull() ?: 0
                "my_score" -> score = readText(parser)?.toIntOrNull()?.takeIf { it in 1..10 }
                "my_status" -> {
                    val raw = readText(parser) ?: ""
                    status = raw.toIntOrNull()?.let { MediaStatus.fromMalStatus(it) }
                        ?: MediaStatus.fromString(raw)
                }
                "my_start_date" -> startDate = parseDate(readText(parser))
                "my_finish_date" -> finishDate = parseDate(readText(parser))
                "my_comments" -> comments = readText(parser)
                "my_tags" -> tags = readText(parser)
                else -> skip(parser)
            }
        }

        if (title.isNullOrBlank()) return null

        val trackingUnit = if (readVolumes > 0 && readChapters > 0) TrackingUnit.COMBINED
        else if (readVolumes > 0) TrackingUnit.VOLUMES
        else TrackingUnit.CHAPTERS

        return MediaEntry(
            title = title,
            mediaType = MediaType.MANGA,
            status = status,
            score = score,
            currentVolumes = readVolumes,
            totalVolumes = totalVolumes,
            currentChapters = readChapters,
            totalChapters = totalChapters,
            trackingUnit = trackingUnit,
            startDate = startDate,
            finishDate = finishDate,
            notes = comments?.takeIf { it.isNotBlank() },
            tags = tags?.takeIf { it.isNotBlank() },
            externalId = externalId,
            externalSource = "MAL",
            priority = Priority.MEDIUM
        )
    }

    private fun readText(parser: XmlPullParser): String? {
        var result: String? = null
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text?.trim()
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    private fun parseDate(str: String?): Long? {
        if (str.isNullOrBlank() || str.startsWith("0000")) return null
        return try {
            dateFormat.parse(str)?.time
        } catch (_: Exception) {
            null
        }
    }
}
