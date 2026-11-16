package com.lx.tracklist.ui.search

data class BookGenreDefinition(
    val name: String,
    val subjectQuery: String
)

object BookTagRegistry {

    val bookGenres: List<BookGenreDefinition> = listOf(
        BookGenreDefinition("Action & Adventure", "adventure"),
        BookGenreDefinition("Biography & Memoir", "biography"),
        BookGenreDefinition("Business & Economics", "business"),
        BookGenreDefinition("Children & Middle Grade", "children"),
        BookGenreDefinition("Classics", "classics"),
        BookGenreDefinition("Comics & Graphic Novels", "graphic_novels"),
        BookGenreDefinition("Crime & Mystery", "mystery"),
        BookGenreDefinition("Drama & Plays", "drama"),
        BookGenreDefinition("Fantasy", "fantasy"),
        BookGenreDefinition("Fiction", "fiction"),
        BookGenreDefinition("Historical Fiction", "historical_fiction"),
        BookGenreDefinition("History", "history"),
        BookGenreDefinition("Horror", "horror"),
        BookGenreDefinition("Non-Fiction", "nonfiction"),
        BookGenreDefinition("Philosophy", "philosophy"),
        BookGenreDefinition("Poetry", "poetry"),
        BookGenreDefinition("Psychology & Self-Help", "self-help"),
        BookGenreDefinition("Romance", "romance"),
        BookGenreDefinition("Science & Nature", "science"),
        BookGenreDefinition("Science Fiction", "science_fiction"),
        BookGenreDefinition("Thriller & Suspense", "thriller"),
        BookGenreDefinition("True Crime", "true_crime"),
        BookGenreDefinition("Young Adult", "young_adult")
    ).sortedBy { it.name.lowercase() }

    fun filterBookGenres(query: String): List<BookGenreDefinition> {
        val clean = query.trim().lowercase()
        return bookGenres
            .filter { clean.isEmpty() || it.name.lowercase().contains(clean) }
            .sortedBy { it.name.lowercase() }
    }

    fun findGenre(name: String): BookGenreDefinition? =
        bookGenres.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    fun getSubject(name: String): String? =
        findGenre(name)?.subjectQuery
}
