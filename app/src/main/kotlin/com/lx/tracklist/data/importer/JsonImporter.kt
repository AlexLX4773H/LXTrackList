package com.lx.tracklist.data.importer

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.lx.tracklist.data.model.ExternalSources
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.Priority
import com.lx.tracklist.data.model.TrackingUnit
import java.io.InputStream
import java.io.InputStreamReader

data class BackupWrapper(
    val app: String? = null,
    val version: Int? = null,
    val exportedAt: Long? = null,
    val totalCount: Int? = null,
    val entries: List<MediaEntry>? = null
)

private data class BackupWrapperImportDto(
    val app: String? = null,
    val version: Int? = null,
    val exportedAt: Long? = null,
    val totalCount: Int? = null,
    val entries: JsonArray? = null
)

/**
 * Nullable mirror of [MediaEntry] for safe JSON deserialization so Gson never
 * bypasses Kotlin constructor defaults on omitted fields.
 */
data class MediaEntryImportDto(
    val id: Long? = null,
    val userId: Long? = null,
    val customCategory: String? = null,
    val subCategory: String? = null,
    val title: String? = null,
    val alternateTitle: String? = null,
    val mediaType: String? = null,
    val status: String? = null,
    val score: Int? = null,
    val externalScore: Float? = null,
    val externalScoreSource: String? = null,
    val startDate: Long? = null,
    val finishDate: Long? = null,
    val airedStartDate: Long? = null,
    val airedEndDate: Long? = null,
    @SerializedName(value = "description", alternate = ["notes"])
    val description: String? = null,
    val tags: String? = null,
    val coverImageUri: String? = null,
    val priority: String? = null,
    val rewatchCount: Int? = null,
    val isRewatching: Boolean? = null,
    val isFavorite: Boolean? = null,
    val isExternallyTracked: Boolean? = null,
    val externalId: String? = null,
    val externalSource: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val currentEpisodes: Int? = null,
    val totalEpisodes: Int? = null,
    val airedEpisodes: Int? = null,
    val seasonNumber: Int? = null,
    val currentSeasons: Int? = null,
    val totalSeasons: Int? = null,
    val seasonEpisodesJson: String? = null,
    val episodeLengthMinutes: Int? = null,
    val currentTimeMinutes: Int? = null,
    val isAiring: Boolean? = null,
    val broadcastTime: String? = null,
    val nextAiringEpoch: Long? = null,
    val currentVolumes: Int? = null,
    val totalVolumes: Int? = null,
    val currentChapters: Int? = null,
    val totalChapters: Int? = null,
    val airedChapters: Int? = null,
    val currentPages: Int? = null,
    val totalPages: Int? = null,
    val trackingUnit: String? = null,
    val movieSeriesNumber: Int? = null,
    val movieSeriesName: String? = null,
    val durationMinutes: Int? = null,
    val relatedEntriesJson: String? = null,
    val gamePlatforms: String? = null,
    val isGameFranchise: Boolean? = null,
    val gameSeriesEntriesJson: String? = null,
    val gameProgressPercent: Int? = null,
    val customUnitLabel: String? = null,
    val customUnitCurrent: Double? = null,
    val customUnitTotal: Double? = null,
    @SerializedName("userComments")
    val userComments: String? = null,
    val originalLanguage: String? = null,
    @SerializedName(value = "creator", alternate = ["artist", "author", "studio", "developer"])
    val creator: String? = null,
    @SerializedName(value = "musicAlbumId", alternate = ["albumId"])
    val musicAlbumId: String? = null,
    val lastMetadataSyncAt: Long? = null
) {
    fun toMediaEntry(now: Long = System.currentTimeMillis()): MediaEntry {
        val cleanTitle = title?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("Missing required 'title'")

        val rawMediaType = mediaType?.trim()?.takeIf { it.isNotEmpty() }
        val isLightNovelImport = MediaEntry.isLightNovelTypeToken(rawMediaType)
        val resolvedType = when {
            isLightNovelImport -> MediaType.BOOK
            rawMediaType != null -> MediaType.fromString(rawMediaType)
            else -> MediaType.ANIME
        }
        val resolvedSubCategory = subCategory?.trim()?.takeIf { it.isNotEmpty() }
            ?: if (isLightNovelImport) "Light Novel" else null

        val resolvedStatus = status?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { MediaStatus.fromString(it) }
            ?: MediaStatus.WATCHING

        val resolvedPriority = priority?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { Priority.fromString(it) }
            ?: Priority.MEDIUM

        val resolvedTrackingUnit = trackingUnit?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { TrackingUnit.fromString(it) }
            ?: TrackingUnit.CHAPTERS

        val cleanExternalId = externalId?.trim()?.takeIf { it.isNotEmpty() }
        val canonicalSource = ExternalSources.canonical(externalSource, cleanExternalId)

        return MediaEntry(
            id = id ?: 0L,
            userId = userId ?: 0L,
            customCategory = customCategory?.trim()?.takeIf { it.isNotEmpty() },
            subCategory = resolvedSubCategory,
            title = cleanTitle,
            alternateTitle = alternateTitle?.takeIf { it.isNotBlank() },
            mediaType = resolvedType,
            status = resolvedStatus,
            score = score?.takeIf { it in 0..10 },
            externalScore = externalScore,
            externalScoreSource = externalScoreSource?.takeIf { it.isNotBlank() },
            startDate = startDate,
            finishDate = finishDate,
            airedStartDate = airedStartDate,
            airedEndDate = airedEndDate,
            description = description?.takeIf { it.isNotBlank() },
            tags = tags?.takeIf { it.isNotBlank() },
            coverImageUri = coverImageUri?.trim()?.takeIf { it.isNotEmpty() },
            priority = resolvedPriority,
            rewatchCount = rewatchCount ?: 0,
            isRewatching = isRewatching ?: false,
            isFavorite = isFavorite ?: false,
            isExternallyTracked = isExternallyTracked ?: false,
            externalId = cleanExternalId,
            externalSource = canonicalSource,
            createdAt = createdAt?.takeIf { it > 0L } ?: now,
            updatedAt = updatedAt?.takeIf { it > 0L } ?: now,
            currentEpisodes = currentEpisodes ?: 0,
            totalEpisodes = totalEpisodes ?: 0,
            airedEpisodes = airedEpisodes ?: 0,
            seasonNumber = seasonNumber ?: 1,
            currentSeasons = currentSeasons ?: 1,
            totalSeasons = totalSeasons ?: 1,
            seasonEpisodesJson = seasonEpisodesJson,
            episodeLengthMinutes = episodeLengthMinutes,
            currentTimeMinutes = currentTimeMinutes,
            isAiring = isAiring ?: false,
            broadcastTime = broadcastTime,
            nextAiringEpoch = nextAiringEpoch,
            currentVolumes = currentVolumes ?: 0,
            totalVolumes = totalVolumes ?: 0,
            currentChapters = currentChapters ?: 0,
            totalChapters = totalChapters ?: 0,
            airedChapters = airedChapters ?: 0,
            currentPages = currentPages ?: 0,
            totalPages = totalPages ?: 0,
            trackingUnit = resolvedTrackingUnit,
            movieSeriesNumber = movieSeriesNumber,
            movieSeriesName = movieSeriesName,
            durationMinutes = durationMinutes,
            relatedEntriesJson = relatedEntriesJson,
            gamePlatforms = gamePlatforms,
            isGameFranchise = isGameFranchise ?: false,
            gameSeriesEntriesJson = gameSeriesEntriesJson,
            gameProgressPercent = gameProgressPercent ?: 0,
            customUnitLabel = customUnitLabel,
            customUnitCurrent = customUnitCurrent ?: 0.0,
            customUnitTotal = customUnitTotal ?: 0.0,
            userComments = userComments?.takeIf { it.isNotBlank() },
            originalLanguage = originalLanguage?.trim()?.takeIf { it.isNotEmpty() },
            creator = creator?.trim()?.takeIf { it.isNotEmpty() },
            musicAlbumId = musicAlbumId?.trim()?.takeIf { it.isNotEmpty() },
            lastMetadataSyncAt = lastMetadataSyncAt
        )
    }
}

data class JsonImportResult(
    val entries: List<MediaEntry> = emptyList(),
    val errors: List<String> = emptyList()
)

object JsonImporter {

    private val gson = Gson()

    fun parse(inputStream: InputStream): List<MediaEntry> = parseWithReport(inputStream).entries

    fun parseWithReport(
        inputStream: InputStream,
        now: Long = System.currentTimeMillis()
    ): JsonImportResult {
        val rawText = try {
            InputStreamReader(inputStream, Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            return JsonImportResult(errors = listOf("Failed to read JSON stream: ${e.message ?: "I/O error"}"))
        }

        val content = rawText.trimStart('\uFEFF').trim()
        if (content.isEmpty()) {
            return JsonImportResult(errors = listOf("Empty JSON input"))
        }

        val rootElement: JsonElement = try {
            JsonParser.parseString(content)
        } catch (e: Exception) {
            return JsonImportResult(errors = listOf("Invalid JSON: ${e.message ?: "Malformed syntax"}"))
        }

        val entriesArray: JsonArray = try {
            when {
                rootElement.isJsonObject -> {
                    val wrapper = gson.fromJson(rootElement, BackupWrapperImportDto::class.java)
                    wrapper?.entries ?: return JsonImportResult(errors = listOf("JSON object has no 'entries' array"))
                }
                rootElement.isJsonArray -> rootElement.asJsonArray
                else -> return JsonImportResult(errors = listOf("Expected a JSON object or array"))
            }
        } catch (e: Exception) {
            return JsonImportResult(errors = listOf("Invalid JSON structure: ${e.message ?: "Unexpected shape"}"))
        }

        val entries = mutableListOf<MediaEntry>()
        val errors = mutableListOf<String>()

        entriesArray.forEachIndexed { index, element ->
            try {
                if (!element.isJsonObject) {
                    errors.add("Row ${index + 1}: expected JSON object")
                } else {
                    val dto = gson.fromJson(element, MediaEntryImportDto::class.java)
                    entries.add(dto.toMediaEntry(now = now))
                }
            } catch (e: Exception) {
                errors.add("Row ${index + 1}: ${e.message ?: "invalid entry"}")
            }
        }

        return JsonImportResult(entries = entries, errors = errors)
    }
}
