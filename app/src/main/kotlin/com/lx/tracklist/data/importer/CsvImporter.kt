package com.lx.tracklist.data.importer

import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.Priority
import com.lx.tracklist.data.model.TrackingUnit
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object CsvImporter {

    fun parse(inputStream: InputStream): List<MediaEntry> {
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        val lines = reader.readLines()
        if (lines.isEmpty()) return emptyList()

        val header = parseCsvLine(lines.first()).map { it.lowercase().trim() }
        val titleIdx = header.indexOfFirst { it == "title" || it == "name" }
        if (titleIdx == -1) return emptyList()

        val typeIdx = header.indexOfFirst { (it.contains("type") || it.contains("category")) && !it.contains("sub") }
        val subCategoryIdx = header.indexOfFirst { it.contains("sub_category") || it.contains("subcategory") || it.contains("sub category") || it.contains("sub-category") }
        val statusIdx = header.indexOfFirst { it.contains("status") }
        val scoreIdx = header.indexOfFirst { it.contains("score") || it.contains("rating") }
        val currentEpIdx = header.indexOfFirst { it.contains("currentepisode") || it.contains("progress") || it.contains("episodes") }
        val totalEpIdx = header.indexOfFirst { it.contains("totalepisode") || it.contains("totalep") }
        val currentVolIdx = header.indexOfFirst { it.contains("currentvolume") || it.contains("volumes") }
        val totalVolIdx = header.indexOfFirst { it.contains("totalvolume") }
        val currentChIdx = header.indexOfFirst { it.contains("currentchapter") || it.contains("chapters") }
        val totalChIdx = header.indexOfFirst { it.contains("totalchapter") }
        val notesIdx = header.indexOfFirst { it.contains("note") || it.contains("comment") }
        val tagsIdx = header.indexOfFirst { it.contains("tag") }

        val entries = mutableListOf<MediaEntry>()

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            val cols = parseCsvLine(line)
            if (cols.size <= titleIdx) continue

            val title = cols[titleIdx].trim()
            if (title.isBlank()) continue

            val mediaType = if (typeIdx in cols.indices) MediaType.fromString(cols[typeIdx]) else MediaType.ANIME
            val subCategory = if (subCategoryIdx in cols.indices) cols[subCategoryIdx].trim().takeIf { it.isNotBlank() } else null
            val status = if (statusIdx in cols.indices) MediaStatus.fromString(cols[statusIdx]) else MediaStatus.WATCHING
            val score = if (scoreIdx in cols.indices) cols[scoreIdx].toIntOrNull()?.takeIf { it in 0..10 } else null

            val currentEp = if (currentEpIdx in cols.indices) cols[currentEpIdx].toIntOrNull() ?: 0 else 0
            val totalEp = if (totalEpIdx in cols.indices) cols[totalEpIdx].toIntOrNull() ?: 0 else 0
            val currentVol = if (currentVolIdx in cols.indices) cols[currentVolIdx].toIntOrNull() ?: 0 else 0
            val totalVol = if (totalVolIdx in cols.indices) cols[totalVolIdx].toIntOrNull() ?: 0 else 0
            val currentCh = if (currentChIdx in cols.indices) cols[currentChIdx].toIntOrNull() ?: 0 else 0
            val totalCh = if (totalChIdx in cols.indices) cols[totalChIdx].toIntOrNull() ?: 0 else 0

            val notes = if (notesIdx in cols.indices) cols[notesIdx].takeIf { it.isNotBlank() } else null
            val tags = if (tagsIdx in cols.indices) cols[tagsIdx].takeIf { it.isNotBlank() } else null

            entries.add(
                MediaEntry(
                    title = title,
                    mediaType = mediaType,
                    customCategory = null,
                    subCategory = subCategory,
                    status = status,
                    score = score,
                    currentEpisodes = currentEp,
                    totalEpisodes = totalEp,
                    currentVolumes = currentVol,
                    totalVolumes = totalVol,
                    currentChapters = currentCh,
                    totalChapters = totalCh,
                    trackingUnit = if (currentVol > 0 && currentCh > 0) TrackingUnit.COMBINED else if (currentVol > 0) TrackingUnit.VOLUMES else TrackingUnit.CHAPTERS,
                    notes = notes,
                    tags = tags,
                    priority = Priority.MEDIUM,
                    externalSource = "CSV"
                )
            )
        }

        return entries
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var cur = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    cur.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(cur.toString().trim())
                cur = StringBuilder()
            } else {
                cur.append(c)
            }
            i++
        }
        result.add(cur.toString().trim())
        return result
    }
}
