package com.lx.tracklist.ui.search

import java.util.Locale

data class LanguageOption(
    val code: String,          // ISO 639-1 code (e.g. "ja", "en", "ko", "th")
    val displayName: String,   // E.g. "Japanese", "English", "Thai"
    val nativeName: String,    // E.g. "日本語", "한국어", "ไทย"
    val flagEmoji: String,     // E.g. "🇯🇵", "🇺🇸", "🇹🇭"
    val iso6392Codes: List<String> = emptyList(), // ISO 639-2/3 (e.g. ["jpn"], ["eng"])
    val countryCodes: List<String> = emptyList()  // ISO 3166-1 alpha-2 (e.g. ["JP"], ["US"])
) {
    val displayWithFlag: String get() = "$flagEmoji $displayName"
}

object OriginalLanguageRegistry {

    val ALL_LANGUAGES: List<LanguageOption> = listOf(
        LanguageOption("ja", "Japanese", "日本語", "🇯🇵", listOf("jpn"), listOf("JP")),
        LanguageOption("en", "English", "English", "🇺🇸", listOf("eng"), listOf("US", "GB", "CA", "AU")),
        LanguageOption("ko", "Korean", "한국어", "🇰🇷", listOf("kor"), listOf("KR")),
        LanguageOption("zh", "Chinese", "中文", "🇨🇳", listOf("chi", "zho"), listOf("CN", "TW", "HK")),
        LanguageOption("th", "Thai", "ไทย", "🇹🇭", listOf("tha"), listOf("TH")),
        LanguageOption("fr", "French", "Français", "🇫🇷", listOf("fre", "fra"), listOf("FR")),
        LanguageOption("es", "Spanish", "Español", "🇪🇸", listOf("spa"), listOf("ES", "MX")),
        LanguageOption("de", "German", "Deutsch", "🇩🇪", listOf("ger", "deu"), listOf("DE", "AT", "CH")),
        LanguageOption("it", "Italian", "Italiano", "🇮🇹", listOf("ita"), listOf("IT")),
        LanguageOption("hi", "Hindi", "हिन्दी", "🇮🇳", listOf("hin"), listOf("IN")),
        LanguageOption("ml", "Malayalam", "മലയാളം", "🇮🇳", listOf("mal"), listOf("IN")),
        LanguageOption("ta", "Tamil", "தமிழ்", "🇮🇳", listOf("tam"), listOf("IN", "LK", "SG")),
        LanguageOption("te", "Telugu", "తెలుగు", "🇮🇳", listOf("tel"), listOf("IN")),
        LanguageOption("ru", "Russian", "Русский", "🇷🇺", listOf("rus"), listOf("RU")),
        LanguageOption("pt", "Portuguese", "Português", "🇵🇹", listOf("por"), listOf("PT", "BR")),
        LanguageOption("id", "Indonesian", "Bahasa Indonesia", "🇮🇩", listOf("ind"), listOf("ID")),
        LanguageOption("tl", "Tagalog", "Tagalog", "🇵🇭", listOf("tgl", "fil"), listOf("PH")),
        LanguageOption("vi", "Vietnamese", "Tiếng Việt", "🇻🇳", listOf("vie"), listOf("VN")),
        LanguageOption("tr", "Turkish", "Türkçe", "🇹🇷", listOf("tur"), listOf("TR")),
        LanguageOption("pl", "Polish", "Polski", "🇵🇱", listOf("pol"), listOf("PL")),
        LanguageOption("ar", "Arabic", "العربية", "🇸🇦", listOf("ara"), listOf("SA", "EG", "AE")),
        LanguageOption("sv", "Swedish", "Svenska", "🇸🇪", listOf("swe"), listOf("SE")),
        LanguageOption("da", "Danish", "Dansk", "🇩🇰", listOf("dan"), listOf("DK")),
        LanguageOption("no", "Norwegian", "Norsk", "🇳🇴", listOf("nor"), listOf("NO")),
        LanguageOption("nl", "Dutch", "Nederlands", "🇳🇱", listOf("nld", "dut"), listOf("NL", "BE")),
        LanguageOption("fi", "Finnish", "Suomi", "🇫🇮", listOf("fin"), listOf("FI")),
        LanguageOption("uk", "Ukrainian", "Українська", "🇺🇦", listOf("ukr"), listOf("UA")),
        LanguageOption("el", "Greek", "Ελληνικά", "🇬🇷", listOf("ell", "gre"), listOf("GR")),
        LanguageOption("he", "Hebrew", "עברית", "🇮🇱", listOf("heb"), listOf("IL")),
        LanguageOption("cs", "Czech", "Čeština", "🇨🇿", listOf("ces", "cze"), listOf("CZ")),
        LanguageOption("hu", "Hungarian", "Magyar", "🇭🇺", listOf("hun"), listOf("HU")),
        LanguageOption("ro", "Romanian", "Română", "🇷🇴", listOf("ron", "rum"), listOf("RO")),
        LanguageOption("ms", "Malay", "Bahasa Melayu", "🇲🇾", listOf("may", "msa"), listOf("MY"))
    )

    fun getTopLanguagesForCategory(category: SearchCategory): List<LanguageOption> {
        val codes = when (category) {
            SearchCategory.ANIME -> listOf("ja", "zh", "ko", "en")
            SearchCategory.MANGA -> listOf("ja", "ko", "zh", "en")
            SearchCategory.MANHWA -> listOf("ko", "ja", "zh", "en")
            SearchCategory.LIGHT_NOVELS -> listOf("ja", "zh", "ko", "en")
            SearchCategory.MOVIES -> listOf("en", "ja", "ko", "th", "zh", "fr", "es", "de", "hi", "ml", "ta", "te")
            SearchCategory.TV_SHOWS -> listOf("en", "th", "ja", "ko", "zh", "es", "fr", "de", "hi")
            SearchCategory.BOOKS -> listOf("en", "ja", "fr", "es", "de", "zh", "it", "ru")
            SearchCategory.GAMES -> listOf("en", "ja", "ko", "zh", "fr", "de", "es")
        }
        return codes.mapNotNull { findByCode(it) }
    }

    /**
     * Returns all available languages for the given search category.
     * Indian languages (Malayalam, Tamil, Telugu) are available for Movies, TV Shows, and Books,
     * but excluded for Anime, Manga, Manhwa, and Light Novels.
     */
    fun getAllLanguagesForCategory(category: SearchCategory): List<LanguageOption> {
        return when (category) {
            SearchCategory.ANIME,
            SearchCategory.MANGA,
            SearchCategory.MANHWA,
            SearchCategory.LIGHT_NOVELS -> {
                ALL_LANGUAGES.filterNot { it.code in setOf("ml", "ta", "te") }
            }
            SearchCategory.MOVIES,
            SearchCategory.TV_SHOWS,
            SearchCategory.BOOKS,
            SearchCategory.GAMES -> {
                ALL_LANGUAGES
            }
        }
    }

    /**
     * Returns other (non-top) languages available for the given category.
     */
    fun getOtherLanguagesForCategory(category: SearchCategory): List<LanguageOption> {
        val topCodes = getTopLanguagesForCategory(category).map { it.code }.toSet()
        return getAllLanguagesForCategory(category).filter { it.code !in topCodes }
    }

    fun findByCode(code: String?): LanguageOption? {
        if (code.isNullOrBlank()) return null
        val clean = code.trim().lowercase(Locale.ROOT)

        // 1. Direct code match (ISO 639-1)
        ALL_LANGUAGES.firstOrNull { it.code.equals(clean, ignoreCase = true) }?.let { return it }

        // 2. ISO 639-2/3 match (e.g. "jpn", "eng", "tha")
        ALL_LANGUAGES.firstOrNull { opt ->
            opt.iso6392Codes.any { it.equals(clean, ignoreCase = true) }
        }?.let { return it }

        // 3. Country code match (e.g. "JP", "KR", "TH", "CN")
        ALL_LANGUAGES.firstOrNull { opt ->
            opt.countryCodes.any { it.equals(clean, ignoreCase = true) }
        }?.let { return it }

        // 4. Display or native name match
        ALL_LANGUAGES.firstOrNull {
            it.displayName.equals(clean, ignoreCase = true) || it.nativeName.equals(clean, ignoreCase = true)
        }?.let { return it }

        // 5. Fallback for any other valid ISO locale
        return try {
            val loc = Locale(clean)
            val disp = loc.getDisplayLanguage(Locale.ENGLISH)
            if (disp.isNotBlank() && !disp.equals(clean, ignoreCase = true)) {
                LanguageOption(
                    code = clean,
                    displayName = disp,
                    nativeName = loc.getDisplayLanguage(loc),
                    flagEmoji = "🌐"
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun getDisplayName(code: String?): String? {
        return findByCode(code)?.displayName
    }

    fun getDisplayWithFlag(code: String?): String? {
        val opt = findByCode(code) ?: return null
        return opt.displayWithFlag
    }

    fun toAniListCountryCode(code: String?): String? {
        val opt = findByCode(code) ?: return null
        return opt.countryCodes.firstOrNull()
    }

    fun toIso6392(code: String?): String? {
        val opt = findByCode(code) ?: return null
        return opt.iso6392Codes.firstOrNull() ?: opt.code
    }

    fun isSameLanguage(codeA: String?, codeB: String?): Boolean {
        if (codeA.isNullOrBlank() || codeB.isNullOrBlank()) return false
        val optA = findByCode(codeA)
        val optB = findByCode(codeB)
        return if (optA != null && optB != null) {
            optA.code.equals(optB.code, ignoreCase = true)
        } else {
            codeA.trim().equals(codeB.trim(), ignoreCase = true)
        }
    }

    fun matches(filterLanguageCode: String?, candidateLanguageCode: String?): Boolean {
        if (filterLanguageCode.isNullOrBlank()) return true
        if (candidateLanguageCode.isNullOrBlank()) return false

        val filterOpt = findByCode(filterLanguageCode)
        val candidateOpt = findByCode(candidateLanguageCode)

        return if (filterOpt != null && candidateOpt != null) {
            filterOpt.code.equals(candidateOpt.code, ignoreCase = true)
        } else {
            filterLanguageCode.trim().equals(candidateLanguageCode.trim(), ignoreCase = true)
        }
    }

    fun matchesAny(filterLanguageCodes: Collection<String>?, candidateLanguageCode: String?): Boolean {
        if (filterLanguageCodes.isNullOrEmpty()) return true
        if (candidateLanguageCode.isNullOrBlank()) return false
        return filterLanguageCodes.any { matches(it, candidateLanguageCode) }
    }
}
