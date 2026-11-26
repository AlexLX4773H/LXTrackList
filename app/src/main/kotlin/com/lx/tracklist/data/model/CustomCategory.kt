package com.lx.tracklist.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_categories",
    indices = [
        androidx.room.Index(value = ["userId", "name"], unique = true)
    ]
)
data class CustomCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val userId: Long = 0L,
    val name: String,
    val mediaType: MediaType? = null,
    val isWatchType: Boolean = false,
    val trackingUnit: TrackingUnit = TrackingUnit.CHAPTERS,
    val colorHex: String = "#A7EF60",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Resolves the effective [MediaType] for this custom category, falling back to
 * [MediaType.ANIME] for watch-type categories or [MediaType.MANGA] for read-type categories
 * when [CustomCategory.mediaType] is null.
 */
fun CustomCategory.effectiveMediaType(): MediaType =
    mediaType ?: if (isWatchType) MediaType.ANIME else MediaType.MANGA

/**
 * Returns a concise human-readable subtitle summarizing this category's resolved base
 * [MediaType] and tracking mode/unit (e.g. `"Anime • Episodes"`, `"Books • Pages"`,
 * `"Games • Progress"`, `"Music • Tracks"`).
 */
fun CustomCategory.summarySubtitle(): String {
    val base = effectiveMediaType()
    return when (base) {
        MediaType.ANIME, MediaType.SHOW -> "${base.displayName} • Episodes"
        MediaType.MOVIE -> "${base.displayName} • Feature"
        MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK ->
            "${base.displayName} • ${TrackingUnit.sanitizeForMediaType(trackingUnit, base).displayName}"
        MediaType.GAME -> "${base.displayName} • Progress"
        MediaType.MUSIC -> "${base.displayName} • Tracks"
    }
}

/**
 * Finds the custom category matching [name] (case-insensitive) and returns its
 * [effectiveMediaType], or null if [name] is null or no matching category exists.
 */
fun List<CustomCategory>.effectiveMediaTypeFor(name: String?): MediaType? =
    name?.let { n -> firstOrNull { it.name.equals(n, ignoreCase = true) }?.effectiveMediaType() }

/**
 * Validates a custom category name against built-in [MediaType] names, singular/plural
 * display names, `"All"`, and optional [existingNames]. Returns an error message or `null` if valid.
 */
fun validateCustomCategoryName(
    rawName: String,
    existingNames: Collection<String> = emptyList()
): String? {
    val trimmed = rawName.trim()
    if (trimmed.isEmpty()) return "Category name cannot be empty."
    val isReserved = trimmed.equals("All", ignoreCase = true) ||
        trimmed.equals("All Categories", ignoreCase = true) ||
        MediaType.entries.any { type ->
            type.name.equals(trimmed, ignoreCase = true) ||
                type.displayName.equals(trimmed, ignoreCase = true) ||
                type.singularName.equals(trimmed, ignoreCase = true)
        }
    if (isReserved) {
        return "\"$trimmed\" is a reserved category name."
    }
    if (existingNames.any { it.trim().equals(trimmed, ignoreCase = true) }) {
        return "Category \"$trimmed\" already exists."
    }
    return null
}

