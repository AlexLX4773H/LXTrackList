package com.lx.tracklist.data.importer

import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.Priority
import com.lx.tracklist.data.model.TrackingUnit
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.BufferedInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream
import javax.xml.parsers.DocumentBuilderFactory

object MalXmlImporter {

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
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(stream)

        val animeNodes = doc.getElementsByTagName("anime")
        for (i in 0 until animeNodes.length) {
            val node = animeNodes.item(i)
            if (node is Element) {
                parseAnime(node)?.let { entries.add(it) }
            }
        }

        val mangaNodes = doc.getElementsByTagName("manga")
        for (i in 0 until mangaNodes.length) {
            val node = mangaNodes.item(i)
            if (node is Element) {
                parseManga(node)?.let { entries.add(it) }
            }
        }

        return entries
    }

    private fun parseAnime(element: Element): MediaEntry? {
        val map = getChildMap(element)
        val title = map["series_title"]?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val externalId = map["series_animedb_id"]?.trim()?.takeIf { it.isNotBlank() }
        val seriesType = map["series_type"]?.trim()
        val totalEpisodes = map["series_episodes"]?.toIntOrNull() ?: 0
        val watchedEpisodes = map["my_watched_episodes"]?.toIntOrNull() ?: 0
        val score = map["my_score"]?.toIntOrNull()?.takeIf { it in 1..10 }

        val rawStatus = map["my_status"]?.trim() ?: ""
        val status = rawStatus.toIntOrNull()?.let { MediaStatus.fromMalStatus(it) }
            ?: MediaStatus.fromString(rawStatus)

        val startDate = parseDate(map["my_start_date"])
        val finishDate = parseDate(map["my_finish_date"])
        val airedStartDate = parseDate(map["series_start"])
        val airedEndDate = parseDate(map["series_end"])

        val rawSeriesStatus = map["series_status"]?.trim() ?: ""
        val isAiring = rawSeriesStatus == "1" || rawSeriesStatus.contains("Airing", ignoreCase = true)

        val rewatchCount = map["my_times_watched"]?.toIntOrNull() ?: 0
        val isRewatching = map["my_rewatching"]?.trim() == "1"
        val comments = map["my_comments"]?.trim()?.takeIf { it.isNotBlank() }
        val tags = map["my_tags"]?.trim()?.takeIf { it.isNotBlank() }

        val mediaType = if (seriesType.equals("Movie", ignoreCase = true)) {
            MediaType.MOVIE
        } else {
            MediaType.ANIME
        }

        return MediaEntry(
            title = title,
            mediaType = mediaType,
            status = status,
            score = score,
            currentEpisodes = watchedEpisodes,
            totalEpisodes = totalEpisodes,
            notes = comments,
            tags = tags,
            priority = Priority.MEDIUM,
            startDate = startDate,
            finishDate = finishDate,
            airedStartDate = airedStartDate,
            airedEndDate = airedEndDate,
            isAiring = isAiring,
            rewatchCount = rewatchCount,
            isRewatching = isRewatching,
            externalId = externalId,
            externalSource = "MAL"
        )
    }

    private fun parseManga(element: Element): MediaEntry? {
        val map = getChildMap(element)
        val title = map["manga_title"]?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val externalId = map["manga_mangadb_id"]?.trim()?.takeIf { it.isNotBlank() }
        val totalVolumes = map["manga_volumes"]?.toIntOrNull() ?: 0
        val totalChapters = map["manga_chapters"]?.toIntOrNull() ?: 0
        val readVolumes = map["my_read_volumes"]?.toIntOrNull() ?: 0
        val readChapters = map["my_read_chapters"]?.toIntOrNull() ?: 0
        val score = map["my_score"]?.toIntOrNull()?.takeIf { it in 1..10 }

        val rawStatus = map["my_status"]?.trim() ?: ""
        val status = rawStatus.toIntOrNull()?.let { MediaStatus.fromMalStatus(it) }
            ?: MediaStatus.fromString(rawStatus)

        val startDate = parseDate(map["my_start_date"])
        val finishDate = parseDate(map["my_finish_date"])
        val airedStartDate = parseDate(map["manga_start"])
        val airedEndDate = parseDate(map["manga_end"])

        val rawSeriesStatus = map["manga_status"]?.trim() ?: ""
        val isPublishing = rawSeriesStatus == "1" || rawSeriesStatus.contains("Publishing", ignoreCase = true)

        val rewatchCount = map["my_times_read"]?.toIntOrNull() ?: 0
        val isRewatching = map["my_rereadingg"]?.trim() == "1"
        val comments = map["my_comments"]?.trim()?.takeIf { it.isNotBlank() }
        val tags = map["my_tags"]?.trim()?.takeIf { it.isNotBlank() }

        val trackingUnit = if (totalVolumes > 0 && totalChapters > 0) {
            TrackingUnit.COMBINED
        } else if (totalVolumes > 0) {
            TrackingUnit.VOLUMES
        } else {
            TrackingUnit.CHAPTERS
        }

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
            notes = comments,
            tags = tags,
            priority = Priority.MEDIUM,
            startDate = startDate,
            finishDate = finishDate,
            airedStartDate = airedStartDate,
            airedEndDate = airedEndDate,
            isAiring = isPublishing,
            rewatchCount = rewatchCount,
            isRewatching = isRewatching,
            externalId = externalId,
            externalSource = "MAL"
        )
    }

    private fun getChildMap(element: Element): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val children = element.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType == Node.ELEMENT_NODE) {
                map[child.nodeName] = child.textContent ?: ""
            }
        }
        return map
    }

    private fun parseDate(str: String?): Long? {
        if (str.isNullOrBlank() || str.startsWith("0000")) return null
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(str.trim())?.time
        } catch (_: Exception) {
            null
        }
    }
}
