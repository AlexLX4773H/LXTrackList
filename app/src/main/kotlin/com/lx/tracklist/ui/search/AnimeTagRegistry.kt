package com.lx.tracklist.ui.search

enum class AnimeTagCategory(val displayName: String) {
    GENRE("Genres"),
    THEME("Themes"),
    DEMOGRAPHIC("Demographics")
}

data class AnimeTagDefinition(
    val name: String,
    val category: AnimeTagCategory,
    val malId: Int,
    val isExplicit: Boolean = false
)

object AnimeTagRegistry {

    val allTags: List<AnimeTagDefinition> = listOf(
        // === GENRES (Explicit Genres & Major Categories in MAL / AniList) ===
        AnimeTagDefinition("Action", AnimeTagCategory.GENRE, 1),
        AnimeTagDefinition("Adventure", AnimeTagCategory.GENRE, 2),
        AnimeTagDefinition("Avant Garde", AnimeTagCategory.GENRE, 5),
        AnimeTagDefinition("Award Winning", AnimeTagCategory.GENRE, 46),
        AnimeTagDefinition("Comedy", AnimeTagCategory.GENRE, 4),
        AnimeTagDefinition("Drama", AnimeTagCategory.GENRE, 8),
        AnimeTagDefinition("Fantasy", AnimeTagCategory.GENRE, 10),
        AnimeTagDefinition("Gourmet", AnimeTagCategory.GENRE, 47),
        AnimeTagDefinition("Horror", AnimeTagCategory.GENRE, 14),
        AnimeTagDefinition("Mystery", AnimeTagCategory.GENRE, 7),
        AnimeTagDefinition("Romance", AnimeTagCategory.GENRE, 22),
        AnimeTagDefinition("Sci-Fi", AnimeTagCategory.GENRE, 24),
        AnimeTagDefinition("Slice of Life", AnimeTagCategory.GENRE, 36),
        AnimeTagDefinition("Sports", AnimeTagCategory.GENRE, 30),
        AnimeTagDefinition("Supernatural", AnimeTagCategory.GENRE, 37),
        AnimeTagDefinition("Suspense", AnimeTagCategory.GENRE, 41),
        AnimeTagDefinition("Ecchi", AnimeTagCategory.GENRE, 9, isExplicit = true),

        // === THEMES ===
        AnimeTagDefinition("Isekai", AnimeTagCategory.THEME, 61),
        AnimeTagDefinition("School", AnimeTagCategory.THEME, 23),
        AnimeTagDefinition("Mecha", AnimeTagCategory.THEME, 18),
        AnimeTagDefinition("Military", AnimeTagCategory.THEME, 38),
        AnimeTagDefinition("Music", AnimeTagCategory.THEME, 19),
        AnimeTagDefinition("Historical", AnimeTagCategory.THEME, 13),
        AnimeTagDefinition("Psychological", AnimeTagCategory.THEME, 40),
        AnimeTagDefinition("Super Power", AnimeTagCategory.THEME, 31),
        AnimeTagDefinition("Space", AnimeTagCategory.THEME, 29),
        AnimeTagDefinition("Harem", AnimeTagCategory.THEME, 35),
        AnimeTagDefinition("Survival", AnimeTagCategory.THEME, 77),
        AnimeTagDefinition("Martial Arts", AnimeTagCategory.THEME, 17),
        AnimeTagDefinition("Time Travel", AnimeTagCategory.THEME, 79),
        AnimeTagDefinition("Vampire", AnimeTagCategory.THEME, 32),
        AnimeTagDefinition("Video Game", AnimeTagCategory.THEME, 80),
        AnimeTagDefinition("Mythology", AnimeTagCategory.THEME, 6),
        AnimeTagDefinition("Detective", AnimeTagCategory.THEME, 39),
        AnimeTagDefinition("Gag Humor", AnimeTagCategory.THEME, 57),
        AnimeTagDefinition("Gore", AnimeTagCategory.THEME, 58, isExplicit = true),
        AnimeTagDefinition("Workplace", AnimeTagCategory.THEME, 48),
        AnimeTagDefinition("Otaku Culture", AnimeTagCategory.THEME, 68),
        AnimeTagDefinition("Idol", AnimeTagCategory.THEME, 60),
        AnimeTagDefinition("CGDCT", AnimeTagCategory.THEME, 52),
        AnimeTagDefinition("Parody", AnimeTagCategory.THEME, 20),
        AnimeTagDefinition("Adult Cast", AnimeTagCategory.THEME, 50),
        AnimeTagDefinition("Reincarnation", AnimeTagCategory.THEME, 73),
        AnimeTagDefinition("Samurai", AnimeTagCategory.THEME, 21),
        AnimeTagDefinition("Combat Sports", AnimeTagCategory.THEME, 54),
        AnimeTagDefinition("Strategy Game", AnimeTagCategory.THEME, 11),

        // === DEMOGRAPHICS ===
        AnimeTagDefinition("Shounen", AnimeTagCategory.DEMOGRAPHIC, 27),
        AnimeTagDefinition("Seinen", AnimeTagCategory.DEMOGRAPHIC, 42),
        AnimeTagDefinition("Shoujo", AnimeTagCategory.DEMOGRAPHIC, 25),
        AnimeTagDefinition("Josei", AnimeTagCategory.DEMOGRAPHIC, 43),
        AnimeTagDefinition("Kids", AnimeTagCategory.DEMOGRAPHIC, 15)
    ).sortedBy { it.name.lowercase() }

    private val malIdByName: Map<String, Int> = allTags.associate { it.name.lowercase() to it.malId }

    private val tagByName: Map<String, AnimeTagDefinition> = allTags.associateBy { it.name.lowercase() }

    fun findTag(name: String): AnimeTagDefinition? = tagByName[name.trim().lowercase()]

    fun getMalId(name: String): Int? = malIdByName[name.trim().lowercase()]

    fun getCategory(name: String): AnimeTagCategory? = findTag(name)?.category

    fun filterTags(query: String, category: AnimeTagCategory? = null): List<AnimeTagDefinition> {
        val cleanQuery = query.trim().lowercase()
        return allTags
            .filter { tag ->
                val matchesCat = category == null || tag.category == category
                val matchesQuery = cleanQuery.isEmpty() || tag.name.lowercase().contains(cleanQuery)
                matchesCat && matchesQuery
            }
            .sortedBy { it.name.lowercase() }
    }
}
