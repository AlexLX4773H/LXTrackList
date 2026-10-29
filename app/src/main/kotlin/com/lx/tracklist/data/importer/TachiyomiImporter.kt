package com.lx.tracklist.data.importer

import com.google.gson.JsonParser
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.Priority
import com.lx.tracklist.data.model.TrackingUnit
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt

data class TachiyomiChapter(
    val url: String = "",
    val name: String = "",
    val scanlator: String? = null,
    val read: Boolean = false,
    val bookmark: Boolean = false,
    val lastPageRead: Long = 0L,
    val dateFetch: Long = 0L,
    val dateUpload: Long = 0L,
    val chapterNumber: Float = 0f,
    val sourceOrder: Long = 0L
)

data class TachiyomiTracking(
    val syncId: Int = 0,
    val libraryId: Long = 0L,
    val mediaIdInt: Int = 0,
    val trackingUrl: String? = null,
    val title: String? = null,
    val lastChapterRead: Float = 0f,
    val totalChapters: Int = 0,
    val score: Float = 0f,
    val status: Int = 0,
    val startedReadingDate: Long = 0L,
    val finishedReadingDate: Long = 0L
)

data class TachiyomiHistory(
    val url: String = "",
    val lastRead: Long = 0L
)

data class TachiyomiCategory(
    val name: String = "",
    val order: Long = 0L,
    val id: Long = 0L
)

data class TachiyomiSource(
    val id: Long = 0L,
    val name: String = ""
)

data class TachiyomiManga(
    val source: Long = 0L,
    val url: String = "",
    val title: String = "",
    val artist: String? = null,
    val author: String? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val status: Int = 0,
    val thumbnailUrl: String? = null,
    val dateAdded: Long = 0L,
    val viewer: Int = 0,
    val chapters: List<TachiyomiChapter> = emptyList(),
    val categories: List<Long> = emptyList(),
    val tracking: List<TachiyomiTracking> = emptyList(),
    val history: List<TachiyomiHistory> = emptyList(),
    val favorite: Boolean = false
)

internal class ProtoReader(
    private val bytes: ByteArray,
    private var offset: Int = 0,
    private val limit: Int = bytes.size
) {
    fun hasMore(): Boolean = offset < limit

    fun readTag(): Int {
        if (offset >= limit) return 0
        return readVarint32()
    }

    fun readVarint64(): Long {
        var result = 0L
        var shift = 0
        while (shift < 64) {
            if (offset >= limit) throw EOFException("Unexpected EOF while reading varint")
            val b = bytes[offset++].toInt()
            result = result or ((b.toLong() and 0x7FL) shl shift)
            if ((b and 0x80) == 0) return result
            shift += 7
        }
        throw IllegalArgumentException("Malformed varint exceeds 64 bits")
    }

    fun readVarint32(): Int = readVarint64().toInt()

    fun readBoolean(): Boolean = readVarint64() != 0L

    fun readFixed32(): Int {
        if (offset + 4 > limit) throw EOFException("Unexpected EOF while reading fixed32")
        val b0 = bytes[offset++].toInt() and 0xFF
        val b1 = bytes[offset++].toInt() and 0xFF
        val b2 = bytes[offset++].toInt() and 0xFF
        val b3 = bytes[offset++].toInt() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    fun readFloat(): Float = Float.fromBits(readFixed32())

    fun readFixed64(): Long {
        if (offset + 8 > limit) throw EOFException("Unexpected EOF while reading fixed64")
        var result = 0L
        for (i in 0 until 8) {
            val b = bytes[offset++].toLong() and 0xFFL
            result = result or (b shl (i * 8))
        }
        return result
    }

    fun readDouble(): Double = Double.fromBits(readFixed64())

    fun readString(): String {
        val length = readVarint32()
        if (length < 0 || offset + length > limit) {
            throw EOFException("Invalid string length: $length (offset=$offset, limit=$limit)")
        }
        val str = String(bytes, offset, length, Charsets.UTF_8)
        offset += length
        return str
    }

    fun readBytes(): ByteArray {
        val length = readVarint32()
        if (length < 0 || offset + length > limit) {
            throw EOFException("Invalid byte length: $length")
        }
        val sub = bytes.copyOfRange(offset, offset + length)
        offset += length
        return sub
    }

    fun subReader(): ProtoReader {
        val length = readVarint32()
        if (length < 0 || offset + length > limit) {
            throw EOFException("Invalid sub-message length: $length")
        }
        val sub = ProtoReader(bytes, offset, offset + length)
        offset += length
        return sub
    }

    fun skipField(wireType: Int) {
        when (wireType) {
            0 -> readVarint64()
            1 -> {
                if (offset + 8 > limit) throw EOFException("Unexpected EOF")
                offset += 8
            }
            2 -> {
                val len = readVarint32()
                if (len < 0 || offset + len > limit) throw EOFException("Unexpected EOF")
                offset += len
            }
            5 -> {
                if (offset + 4 > limit) throw EOFException("Unexpected EOF")
                offset += 4
            }
            3 -> {
                while (hasMore()) {
                    val tag = readTag()
                    if (tag == 0 || (tag and 0x07) == 4) break
                    skipField(tag and 0x07)
                }
            }
            4 -> {}
            else -> throw IllegalArgumentException("Unknown wire type: $wireType")
        }
    }
}

object TachiyomiImporter {

    fun parse(inputStream: InputStream): List<MediaEntry> {
        val rawBytes = try {
            inputStream.readBytes()
        } catch (_: Exception) {
            return emptyList()
        }

        if (rawBytes.isEmpty()) return emptyList()

        // 1. Decompress if GZIP compressed
        val decompressedBytes = if (rawBytes.size >= 2 && rawBytes[0] == 0x1F.toByte() && rawBytes[1] == 0x8B.toByte()) {
            try {
                GZIPInputStream(ByteArrayInputStream(rawBytes)).use { it.readBytes() }
            } catch (_: Exception) {
                rawBytes
            }
        } else {
            rawBytes
        }

        // 2. Check if JSON (legacy or converted backup)
        val trimmedFirstChar = decompressedBytes.asSequence()
            .map { it.toInt().toChar() }
            .firstOrNull { !it.isWhitespace() }

        if (trimmedFirstChar == '{' || trimmedFirstChar == '[') {
            try {
                val jsonString = String(decompressedBytes, Charsets.UTF_8).trim()
                val jsonEntries = parseJson(jsonString)
                if (jsonEntries.isNotEmpty()) return jsonEntries
            } catch (_: Exception) {}
        }

        // 3. Parse Protobuf
        return try {
            parseProtobuf(decompressedBytes)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseProtobuf(bytes: ByteArray): List<MediaEntry> {
        val reader = ProtoReader(bytes)
        val mangaList = mutableListOf<TachiyomiManga>()
        val categoryMap = mutableMapOf<Long, String>()
        val sourceMap = mutableMapOf<Long, String>()
        var categoryIndex = 0L

        while (reader.hasMore()) {
            val tag = reader.readTag()
            if (tag == 0) break
            val field = tag ushr 3
            val wire = tag and 0x07

            when (field) {
                1 -> { // repeated BackupManga backupManga = 1;
                    if (wire == 2) {
                        mangaList.add(parseManga(reader.subReader()))
                    } else {
                        reader.skipField(wire)
                    }
                }
                2 -> { // repeated BackupCategory backupCategories = 2;
                    if (wire == 2) {
                        val cat = parseCategory(reader.subReader())
                        categoryMap[cat.order] = cat.name
                        categoryMap[categoryIndex] = cat.name
                        if (cat.id > 0L) categoryMap[cat.id] = cat.name
                        categoryIndex++
                    } else {
                        reader.skipField(wire)
                    }
                }
                3, 4, 101 -> { // backupSources
                    if (wire == 2) {
                        val src = parseSource(reader.subReader())
                        if (src.id != 0L && src.name.isNotBlank()) {
                            sourceMap[src.id] = src.name
                        }
                    } else {
                        reader.skipField(wire)
                    }
                }
                else -> reader.skipField(wire)
            }
        }

        return mangaList.mapNotNull { manga ->
            mangaToMediaEntry(manga, categoryMap, sourceMap)
        }
    }

    private fun parseManga(reader: ProtoReader): TachiyomiManga {
        var source = 0L
        var url = ""
        var title = ""
        var artist: String? = null
        var author: String? = null
        var description: String? = null
        val genres = mutableListOf<String>()
        var status = 0
        var thumbnailUrl: String? = null
        var dateAdded = 0L
        var viewer = 0
        val chapters = mutableListOf<TachiyomiChapter>()
        val categories = mutableListOf<Long>()
        val tracking = mutableListOf<TachiyomiTracking>()
        val history = mutableListOf<TachiyomiHistory>()
        var favorite = false

        while (reader.hasMore()) {
            val tag = reader.readTag()
            if (tag == 0) break
            val field = tag ushr 3
            val wire = tag and 0x07

            when (field) {
                1 -> source = reader.readVarint64()
                2 -> url = reader.readString()
                3 -> title = reader.readString()
                4 -> artist = reader.readString()
                5 -> author = reader.readString()
                6 -> description = reader.readString()
                7 -> genres.add(reader.readString())
                8 -> status = reader.readVarint32()
                9 -> thumbnailUrl = reader.readString()
                13 -> dateAdded = reader.readVarint64()
                14 -> viewer = reader.readVarint32()
                16 -> chapters.add(parseChapter(reader.subReader()))
                17 -> {
                    when (wire) {
                        0 -> categories.add(reader.readVarint64())
                        2 -> {
                            val sub = reader.subReader()
                            while (sub.hasMore()) {
                                categories.add(sub.readVarint64())
                            }
                        }
                        else -> reader.skipField(wire)
                    }
                }
                18 -> tracking.add(parseTracking(reader.subReader()))
                19 -> history.add(parseHistory(reader.subReader()))
                100 -> favorite = reader.readBoolean()
                else -> reader.skipField(wire)
            }
        }

        return TachiyomiManga(
            source = source,
            url = url,
            title = title,
            artist = artist,
            author = author,
            description = description,
            genres = genres,
            status = status,
            thumbnailUrl = thumbnailUrl,
            dateAdded = dateAdded,
            viewer = viewer,
            chapters = chapters,
            categories = categories,
            tracking = tracking,
            history = history,
            favorite = favorite
        )
    }

    private fun parseChapter(reader: ProtoReader): TachiyomiChapter {
        var url = ""
        var name = ""
        var scanlator: String? = null
        var read = false
        var bookmark = false
        var lastPageRead = 0L
        var dateFetch = 0L
        var dateUpload = 0L
        var chapterNumber = 0f
        var sourceOrder = 0L

        while (reader.hasMore()) {
            val tag = reader.readTag()
            if (tag == 0) break
            val field = tag ushr 3
            val wire = tag and 0x07

            when (field) {
                1 -> url = reader.readString()
                2 -> name = reader.readString()
                3 -> scanlator = reader.readString()
                4 -> read = reader.readBoolean()
                5 -> bookmark = reader.readBoolean()
                6 -> lastPageRead = reader.readVarint64()
                7 -> dateFetch = reader.readVarint64()
                8 -> dateUpload = reader.readVarint64()
                9 -> chapterNumber = if (wire == 5) reader.readFloat() else reader.readVarint64().toFloat()
                10 -> sourceOrder = reader.readVarint64()
                else -> reader.skipField(wire)
            }
        }

        return TachiyomiChapter(
            url = url,
            name = name,
            scanlator = scanlator,
            read = read,
            bookmark = bookmark,
            lastPageRead = lastPageRead,
            dateFetch = dateFetch,
            dateUpload = dateUpload,
            chapterNumber = chapterNumber,
            sourceOrder = sourceOrder
        )
    }

    private fun parseTracking(reader: ProtoReader): TachiyomiTracking {
        var syncId = 0
        var libraryId = 0L
        var mediaIdInt = 0
        var trackingUrl: String? = null
        var title: String? = null
        var lastChapterRead = 0f
        var totalChapters = 0
        var score = 0f
        var status = 0
        var startedReadingDate = 0L
        var finishedReadingDate = 0L

        while (reader.hasMore()) {
            val tag = reader.readTag()
            if (tag == 0) break
            val field = tag ushr 3
            val wire = tag and 0x07

            when (field) {
                1 -> syncId = reader.readVarint32()
                2 -> libraryId = reader.readVarint64()
                3 -> mediaIdInt = reader.readVarint32()
                4 -> trackingUrl = reader.readString()
                5 -> title = reader.readString()
                6 -> lastChapterRead = if (wire == 5) reader.readFloat() else reader.readVarint64().toFloat()
                7 -> totalChapters = reader.readVarint32()
                8 -> score = if (wire == 5) reader.readFloat() else reader.readVarint64().toFloat()
                9 -> status = reader.readVarint32()
                10 -> startedReadingDate = reader.readVarint64()
                11 -> finishedReadingDate = reader.readVarint64()
                else -> reader.skipField(wire)
            }
        }

        return TachiyomiTracking(
            syncId = syncId,
            libraryId = libraryId,
            mediaIdInt = mediaIdInt,
            trackingUrl = trackingUrl,
            title = title,
            lastChapterRead = lastChapterRead,
            totalChapters = totalChapters,
            score = score,
            status = status,
            startedReadingDate = startedReadingDate,
            finishedReadingDate = finishedReadingDate
        )
    }

    private fun parseHistory(reader: ProtoReader): TachiyomiHistory {
        var url = ""
        var lastRead = 0L

        while (reader.hasMore()) {
            val tag = reader.readTag()
            if (tag == 0) break
            val field = tag ushr 3
            val wire = tag and 0x07

            when (field) {
                1 -> url = reader.readString()
                2 -> lastRead = reader.readVarint64()
                else -> reader.skipField(wire)
            }
        }

        return TachiyomiHistory(url = url, lastRead = lastRead)
    }

    private fun parseCategory(reader: ProtoReader): TachiyomiCategory {
        var name = ""
        var order = 0L
        var id = 0L

        while (reader.hasMore()) {
            val tag = reader.readTag()
            if (tag == 0) break
            val field = tag ushr 3
            val wire = tag and 0x07

            when (field) {
                1 -> name = reader.readString()
                2 -> order = reader.readVarint64()
                4 -> id = reader.readVarint64()
                else -> reader.skipField(wire)
            }
        }

        return TachiyomiCategory(name = name, order = order, id = id)
    }

    private fun parseSource(reader: ProtoReader): TachiyomiSource {
        var id = 0L
        var name = ""

        while (reader.hasMore()) {
            val tag = reader.readTag()
            if (tag == 0) break
            val field = tag ushr 3
            val wire = tag and 0x07

            when (field) {
                1 -> {
                    if (wire == 0) id = reader.readVarint64()
                    else if (wire == 2) name = reader.readString()
                    else reader.skipField(wire)
                }
                2 -> {
                    if (wire == 2) name = reader.readString()
                    else if (wire == 0) id = reader.readVarint64()
                    else reader.skipField(wire)
                }
                else -> reader.skipField(wire)
            }
        }

        return TachiyomiSource(id = id, name = name)
    }

    fun isStatusCategory(name: String): Boolean {
        val lower = name.lowercase().trim()
        return lower.contains("reading") || lower.contains("completed") ||
                lower.contains("plan to read") || lower.contains("to read") ||
                lower.contains("backlog") || lower.contains("planned") ||
                lower.contains("on hold") || lower.contains("hold") ||
                lower.contains("paused") || lower.contains("dropped") ||
                lower.contains("finished") || lower.contains("done")
    }

    fun isBuiltInTypeCategory(name: String): Boolean {
        val lower = name.lowercase().trim()
        return lower == "manga" || lower == "japanese manga" ||
                lower == "manhwa" || lower == "webtoon" || lower == "webtoons" || lower == "korean" ||
                lower == "anime" || lower == "show" || lower == "tv show" || lower == "movie" || lower == "book" || lower == "books"
    }

    fun isGenericCategory(name: String): Boolean {
        val lower = name.lowercase().trim()
        return lower == "default" || lower == "default category" || lower == "all" || lower == "uncategorized" || lower.isBlank()
    }

    private fun mangaToMediaEntry(
        manga: TachiyomiManga,
        categoryMap: Map<Long, String>,
        sourceMap: Map<Long, String>
    ): MediaEntry? {
        val title = manga.title.trim()
        if (title.isBlank()) return null

        val resolvedCategories = manga.categories.mapNotNull { categoryMap[it] }.distinct()
        val sourceName = sourceMap[manga.source]

        // Sub category identification from Tachiyomi user categories
        val nonStatusCategories = resolvedCategories.filter { !isStatusCategory(it) }
        val customCategory = nonStatusCategories.firstOrNull {
            val lower = it.lowercase().trim()
            !isGenericCategory(lower) && lower != "manga" && lower != "manhwa" && lower != "japanese manga"
        }?.trim()

        // 1. Determine MediaType (MANGA vs MANHWA)
        // In .tachibk, entries are manga/manhwa; Tachiyomi categories are sub-categories under Manga or Manhwa
        val mediaType = if (isManhwa(manga, resolvedCategories, sourceName)) {
            MediaType.MANHWA
        } else {
            MediaType.MANGA
        }

        // 2. Determine Progress (Current & Total Chapters/Episodes)
        val trackedTotal = manga.tracking.map { it.totalChapters }.firstOrNull { it > 0 } ?: 0
        val maxChapterFromList = manga.chapters.maxOfOrNull { it.chapterNumber }?.toInt() ?: 0
        val chapterCount = manga.chapters.size

        val trackedRead = manga.tracking.map { it.lastChapterRead.toInt() }.firstOrNull { it > 0 } ?: 0
        val readCount = manga.chapters.count { it.read }
        val maxReadChapter = manga.chapters.filter { it.read }.maxOfOrNull { it.chapterNumber }?.toInt() ?: 0
        val chapterReadProgress = if (maxReadChapter > 0) maxReadChapter else readCount

        val currentChapters = maxOf(trackedRead, chapterReadProgress)

        var totalChapters = if (trackedTotal > 0) {
            trackedTotal
        } else if (manga.status == 1) { // 1 = COMPLETED in Tachiyomi SManga
            maxOf(maxChapterFromList, chapterCount)
        } else {
            0
        }
        if (totalChapters in 1 until currentChapters) {
            totalChapters = currentChapters
        }

        // 3. Determine Status
        val status = determineStatus(manga, resolvedCategories, currentChapters, totalChapters)

        // 4. Score
        val rawScore = manga.tracking.map { it.score }.firstOrNull { it > 0f }
        val score = rawScore?.let { s ->
            when {
                s in 1f..10f -> s.roundToInt()
                s in 11f..100f -> (s / 10f).roundToInt()
                else -> null
            }
        }

        // 5. Tags & Notes
        val allTags = (manga.genres + nonStatusCategories).map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val tags = allTags.joinToString(", ").takeIf { it.isNotBlank() }

        val noteParts = mutableListOf<String>()
        val author = manga.author?.trim()
        val artist = manga.artist?.trim()
        if (!author.isNullOrBlank() && !artist.isNullOrBlank() && author != artist) {
            noteParts.add("Author: $author • Artist: $artist")
        } else if (!author.isNullOrBlank()) {
            noteParts.add("Author: $author")
        } else if (!artist.isNullOrBlank()) {
            noteParts.add("Artist: $artist")
        }
        if (!manga.description.isNullOrBlank()) {
            noteParts.add(manga.description.trim())
        }
        val notes = noteParts.joinToString("\n\n").takeIf { it.isNotBlank() }

        // 6. Cover image URL
        val coverImageUri = manga.thumbnailUrl?.takeIf { it.isNotBlank() }

        // 7. External Tracker mapping
        val malTrack = manga.tracking.firstOrNull { it.syncId == 1 }
        val aniTrack = manga.tracking.firstOrNull { it.syncId == 2 }
        val (externalSource, externalId) = when {
            malTrack != null -> {
                val id = if (malTrack.mediaIdInt > 0) malTrack.mediaIdInt.toString() else malTrack.libraryId.toString()
                Pair("MAL", id)
            }
            aniTrack != null -> {
                val id = if (aniTrack.mediaIdInt > 0) aniTrack.mediaIdInt.toString() else aniTrack.libraryId.toString()
                Pair("ANILIST", id)
            }
            else -> {
                val id = if (manga.url.isNotBlank()) manga.url else manga.title
                Pair("TACHIYOMI", id)
            }
        }

        // 8. Timestamps
        val startDate = manga.tracking.mapNotNull { normalizeDate(it.startedReadingDate) }.firstOrNull()
        val finishDate = manga.tracking.mapNotNull { normalizeDate(it.finishedReadingDate) }.firstOrNull()
        val createdAt = normalizeDate(manga.dateAdded) ?: System.currentTimeMillis()
        val lastRead = manga.history.mapNotNull { normalizeDate(it.lastRead) }.maxOrNull()
        val updatedAt = lastRead ?: createdAt

        val isFavorite = manga.favorite || manga.dateAdded > 0L

        val trackingUnit = when {
            customCategory != null && (customCategory.contains("volume", ignoreCase = true) || customCategory.contains("tankobon", ignoreCase = true)) -> TrackingUnit.VOLUMES
            else -> TrackingUnit.CHAPTERS
        }

        return MediaEntry(
            title = title,
            mediaType = mediaType,
            customCategory = customCategory,
            status = status,
            score = score,
            startDate = startDate,
            finishDate = finishDate,
            notes = notes,
            tags = tags,
            coverImageUri = coverImageUri,
            priority = Priority.MEDIUM,
            isFavorite = isFavorite,
            externalId = externalId,
            externalSource = externalSource,
            createdAt = createdAt,
            updatedAt = updatedAt,
            currentEpisodes = 0,
            totalEpisodes = 0,
            currentChapters = currentChapters,
            totalChapters = totalChapters,
            trackingUnit = trackingUnit
        )
    }

    private fun isManhwa(
        manga: TachiyomiManga,
        categoryNames: List<String>,
        sourceName: String?
    ): Boolean {
        val manhwaKeywords = listOf(
            "manhwa",
            "webtoon",
            "webtoons",
            "korean",
            "long strip",
            "vertical strip"
        )

        // 1. Genres
        for (genre in manga.genres) {
            val lower = genre.lowercase().trim()
            if (manhwaKeywords.any { lower.contains(it) }) return true
        }

        // 2. User categories
        for (cat in categoryNames) {
            val lower = cat.lowercase().trim()
            if (manhwaKeywords.any { lower.contains(it) }) return true
        }

        // 3. Source name
        if (!sourceName.isNullOrBlank()) {
            val sLower = sourceName.lowercase().trim()
            val manhwaSources = listOf(
                "asura",
                "reaper",
                "flame",
                "webtoon",
                "manhwa",
                "naver",
                "kakao",
                "tappytoon",
                "tapas"
            )
            if (manhwaSources.any { sLower.contains(it) }) return true
        }

        // 4. Title
        val tLower = manga.title.lowercase()
        if (tLower.contains("manhwa") || tLower.contains("webtoon")) return true

        return false
    }

    private fun determineStatus(
        manga: TachiyomiManga,
        categoryNames: List<String>,
        currentChapters: Int,
        totalChapters: Int
    ): MediaStatus {
        // 1. Category check
        for (cat in categoryNames) {
            val lower = cat.lowercase()
            when {
                lower.contains("plan to read") || lower.contains("to read") || lower.contains("backlog") || lower.contains("planned") -> return MediaStatus.PLAN_TO_WATCH
                lower.contains("completed") || lower.contains("finished") || lower.contains("done") -> return MediaStatus.COMPLETED
                lower.contains("on hold") || lower.contains("hold") || lower.contains("paused") -> return MediaStatus.ON_HOLD
                lower.contains("dropped") -> return MediaStatus.DROPPED
                lower.contains("reading") || lower.contains("currently reading") -> return MediaStatus.WATCHING
            }
        }

        // 2. Tracking check
        for (track in manga.tracking) {
            if (track.status > 0) {
                if (track.syncId == 1) { // MAL
                    return MediaStatus.fromMalStatus(track.status)
                } else { // AniList or others
                    return when (track.status) {
                        1 -> MediaStatus.WATCHING
                        2 -> MediaStatus.PLAN_TO_WATCH
                        3 -> MediaStatus.COMPLETED
                        4 -> MediaStatus.DROPPED
                        5 -> MediaStatus.ON_HOLD
                        else -> MediaStatus.WATCHING
                    }
                }
            }
        }

        // 3. Chapter progress fallback
        return if (currentChapters > 0) {
            if (totalChapters > 0 && currentChapters >= totalChapters) {
                MediaStatus.COMPLETED
            } else {
                MediaStatus.WATCHING
            }
        } else {
            MediaStatus.PLAN_TO_WATCH
        }
    }

    private fun normalizeDate(timestamp: Long): Long? {
        if (timestamp <= 0L) return null
        return if (timestamp < 100_000_000_000L) timestamp * 1000L else timestamp
    }

    private fun parseJson(content: String): List<MediaEntry> {
        val element = JsonParser.parseString(content)
        if (!element.isJsonObject) return emptyList()
        val obj = element.asJsonObject

        val mangaArray = when {
            obj.has("backupManga") -> obj.getAsJsonArray("backupManga")
            obj.has("mangas") -> obj.getAsJsonArray("mangas")
            else -> null
        } ?: return emptyList()

        val categoriesArray = when {
            obj.has("backupCategories") -> obj.getAsJsonArray("backupCategories")
            obj.has("categories") -> obj.getAsJsonArray("categories")
            else -> null
        }

        val categoryMap = mutableMapOf<Long, String>()
        categoriesArray?.forEachIndexed { index, catEl ->
            if (catEl.isJsonObject) {
                val cObj = catEl.asJsonObject
                val name = cObj.get("name")?.asString ?: ""
                val order = cObj.get("order")?.asLong ?: index.toLong()
                val id = cObj.get("id")?.asLong ?: 0L
                categoryMap[order] = name
                categoryMap[index.toLong()] = name
                if (id > 0L) categoryMap[id] = name
            }
        }

        val entries = mutableListOf<MediaEntry>()
        for (mEl in mangaArray) {
            if (!mEl.isJsonObject) continue
            val rootMangaObj = mEl.asJsonObject
            val mObj = if (rootMangaObj.has("manga")) rootMangaObj.getAsJsonObject("manga") else rootMangaObj

            val title = mObj.get("title")?.asString ?: continue
            if (title.isBlank()) continue

            val genres = mObj.get("genre")?.let { gEl ->
                if (gEl.isJsonArray) gEl.asJsonArray.mapNotNull { it.asString }
                else gEl.asString.split(",").map { it.trim() }
            } ?: emptyList()

            val statusInt = mObj.get("status")?.asInt ?: 0
            val thumb = mObj.get("thumbnailUrl")?.asString
            val author = mObj.get("author")?.asString
            val artist = mObj.get("artist")?.asString
            val desc = mObj.get("description")?.asString
            val url = mObj.get("url")?.asString ?: ""

            val catList = mutableListOf<Long>()
            val catsEl = if (rootMangaObj.has("categories")) rootMangaObj.get("categories") else mObj.get("categories")
            if (catsEl != null && catsEl.isJsonArray) {
                catsEl.asJsonArray.forEach { c ->
                    if (c.isJsonPrimitive && c.asJsonPrimitive.isNumber) {
                        catList.add(c.asLong)
                    }
                }
            }

            val chList = mutableListOf<TachiyomiChapter>()
            val chaptersEl = if (rootMangaObj.has("chapters")) rootMangaObj.get("chapters") else mObj.get("chapters")
            if (chaptersEl != null && chaptersEl.isJsonArray) {
                chaptersEl.asJsonArray.forEach { chEl ->
                    if (chEl.isJsonObject) {
                        val chObj = chEl.asJsonObject
                        val chNum = chObj.get("chapterNumber")?.asFloat ?: 0f
                        val chRead = chObj.get("read")?.asBoolean ?: false
                        chList.add(TachiyomiChapter(chapterNumber = chNum, read = chRead))
                    }
                }
            }

            val tManga = TachiyomiManga(
                title = title,
                url = url,
                author = author,
                artist = artist,
                description = desc,
                genres = genres,
                status = statusInt,
                thumbnailUrl = thumb,
                categories = catList,
                chapters = chList
            )

            mangaToMediaEntry(tManga, categoryMap, emptyMap())?.let { entries.add(it) }
        }

        return entries
    }
}

