# <p align="center"><img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="LX Track List Logo" width="96" style="border-radius: 20%;" /><br>LX Track List</p>

<p align="center">
  <strong>A fast, sleek, offline-first personal media tracking Android application.</strong><br>
  Track Anime, Manga, Manhwa, Movies, TV Shows, Books, Games, and Custom Media in one unified place.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-0.6.5-blue?style=flat-square" alt="Version" />
  <img src="https://img.shields.io/badge/Platform-Android_8.0+_(API_26--36)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin_2.x-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_&_Material_3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Database-Room_(SQLite)-00599C?style=flat-square&logo=sqlite&logoColor=white" alt="Room" />
  <img src="https://img.shields.io/badge/Theme-OLED_Dark_&_Neon_Lime_/_Custom_Themes-A7EF60?style=flat-square" alt="Theme" />
  <img src="https://img.shields.io/badge/Privacy-100%25_Local_&_Offline_First-success?style=flat-square" alt="Privacy" />
</p>

---

## 📖 Overview

**LX Track List** is an open-source, all-in-one personal media tracking app designed for Android. Instead of fragmenting your watchlist across separate platforms (MyAnimeList, AniList, Letterboxd, TV Time, MangaDex, Tachiyomi, Goodreads), LX Track List consolidates all your entertainment into a single, cohesive, and private database.

Built from the ground up with **Kotlin**, **Jetpack Compose (Material 3)**, and **Room (SQLite)**, the app emphasizes lightning-fast performance, elegant aesthetics (OLED pure black paired with `#A7EF60` electric neon accents, plus custom theme packs and user-defined accent colors), and total data sovereignty.

---

## ✨ Features at a Glance

### 🌟 Seasonal Anime & Media Discovery Engine
Discover what's airing, trending, and premiering directly inside the **Search & Discover** tab via the top-right discovery menu:
- **Seasonal Anime Browser**:
  - Browse past, current, and upcoming anime seasons (**Winter**, **Spring**, **Summer**, **Fall**) with an intuitive 2×2 card selector, season emojis, and month ranges (e.g., *Jan – Mar*, *Apr – Jun*).
  - High-performance **AniList GraphQL** query pipeline with automatic **Jikan API v4 (MAL)** fallback for instant, robust results.
  - Sort by **Most Popular**, **Highest Rated**, **Release Date**, or **Title (A–Z)**.
- **TMDB Movie & TV Show Discovery**:
  - Filter movies and TV shows by release / first-air year.
  - Supports extended year horizons up to **Current Year + 2** (e.g. 2027) with quick-access year chips and a complete dropdown (1960 to Current Year + 2).
  - Sort by **Most Popular**, **Highest Rated**, **Release Date**, and **Box Office** (Movies) or **Premiere Date** (TV).
- **Scoped Title Search**:
  - When a seasonal or year filter is active, typing in the search bar scopes results strictly to that season or year (e.g. searching *"Frieren"* while filtered to *Fall 2023* searches within that season).
  - Clearing the search query instantly restores the full seasonal/year catalog.
- **On-Demand "Load More" Pagination**:
  - Clean, dedicated load-more button at the bottom of discovery and search result lists to paginate through deep archives.
- **Subtle 3-State 18+ Content Cycle**:
  - Compact, unobtrusive checkbox with a 3-state click cycle:
    1. **Off (Default)**: Strict family-safe content only (18+ excluded).
    2. **Include 18+**: Returns standard and adult content together.
    3. **18+ Only**: Strictly isolates and returns **only** 18+ entities (*Hentai* for Anime via AniList `$isAdult: true` / Jikan `Rx`, and verified adult entities for TMDB Movies/TV).
    4. Next click cycles back to **Off**.
- **Original Language Multi-Select & Discovery**:
  - Filter discoveries by original language with multi-select `OR` logic across Movies, TV Shows, and Books.
  - Complete ISO 639-1 / 639-2 code mapping, localized native script names, and flag emojis (e.g., 🇯🇵 Japanese, 🇰🇷 Korean, 🇨🇳 Chinese, 🇪🇸 Spanish, 🇫🇷 French, 🇩🇪 German).
  - Full support for Indian languages: **Malayalam** (`മലയാളം` • `ml`), **Tamil** (`தமிழ்` • `ta`), **Telugu** (`తెలుగు` • `te`), and **Hindi** (`हिन्दी` • `hi`).
  - Category-aware placement: placed in **Top for Movies** for films; placed in **Other Languages** for TV Shows and Books; omitted from Anime, Manga, Manhwa, and Light Novels to keep catalogs authentic.
- **Multilingual Book Discovery (Google Books & Open Library)**:
  - Deep international book discovery across Google Books and Open Library without English category lockouts.
  - Uses language-native query resolution, Solr `*:*` wildcard base queries, multi-code terminology matching (`(language:chi OR language:zho)`), and explicit field hydration.
- **Dynamic Apply Action (`✓`) in Submenus**:
  - Submenu headers in **Library (Tags Filter)** and **Search & Discover (Tags & Genres, Original Language)** dynamically morph the back arrow (`←`) into a prominent **Apply checkmark** (`✓`) whenever filters are selected.
- **Rich TV Previews & Title Clipboard Copy**:
  - TMDB TV show previews in Search automatically fetch authoritative season counts and total episodes (e.g. `3 Seasons • 24 Episodes • 57m/ep`), eliminating "Episodes unknown".
  - Long-pressing the primary or alternate title in the expanded search bottom sheet copies it straight to the clipboard with instant toast confirmation.
- **Clean Discovery UI**:
  - Automatically hides external provider attribution notices during discovery browsing to keep the catalog clean and immersive.
- **Accelerated Multi-Source Search**:
  - Optimized, parallel query execution across Jikan API v4, AniList GraphQL, MangaDex, and Kitsu.
  - Dramatically speeds up Anime, Manga, and Manhwa search queries while respecting upstream rate limits.

### 🌐 Unified Multi-Platform "Open ▾" Launcher & Kitsu Resolver
Seamlessly open external media pages on authoritative platforms without screen clutter or broken links:
- **Centralized "Open ▾" Dropdown**:
  - Replaces messy separate platform buttons and ISP workaround rows with a sleek, consolidated dropdown menu across **Search & Discover**, **Edit Entity (Sync from External DB)**, and **Media Details**.
  - Automatically curates relevant platforms based on media category:
    - **Video Games**: *IGDB*, *RAWG*, and *SteamGridDB*.
    - **Movies & TV Shows**: *The Movie Database (TMDB)*, *IMDb ⭐*, and *Rotten Tomatoes 🍅*.
    - **Anime**: *MyAnimeList (MAL)*, *AniList*, and *Kitsu*.
    - **Manga & Manhwa**: *MangaDex*, *MyAnimeList (MAL)*, *AniList*, and *Kitsu*.
    - **Light Novels**: *AniList*, *MyAnimeList (MAL)*, *BookWalker 🇯🇵*, and *MangaDex*.
    - **Books**: *Google Books*, *Open Library*, and *Goodreads 📚*.
- **Dynamic Kitsu Exact Slug Resolution (Zero 404s)**:
  - Kitsu's web frontend (SPA) requires exact database slugs (typically Romaji, e.g. `shingeki-no-kyojin`) rather than generic English titles or raw numeric IDs, which previously led to blank `kitsu.app/404` errors.
  - LX Track List dynamically resolves the exact slug via Kitsu's official mappings endpoint (`/mappings?filter[externalSite]=myanimelist&include=item`) using the MAL ID, or queries Kitsu's search API.
  - Features an in-memory resolution cache for instant subsequent launches and a guaranteed safe Google site search fallback (`site:kitsu.app/anime ...`) if an entry is unlisted, completely eliminating 404 dead ends.

### ⏳ "NYA" (Not Yet Aired) & "NYP" (Not Yet Published) Tracking
Clear, accurate status tracking for upcoming and unreleased media:
- **Distinct Status Badges**:
  - Prevents unreleased media from being ambiguously labeled as "Airing" or "Publishing".
  - Shows **"NYA"** (Not Yet Aired) for Anime episodes and **"NYP"** (Not Yet Published) for Manga/Manhwa chapters and volumes when release dates are in the future or no episodes/chapters have dropped yet.
- **Advanced Airing Status Filter**:
  - Filter your library with dedicated 4-mode toggles in the Advanced Filter Sheet:
    1. **All**: Shows all entries.
    2. **Airing / Publishing**: Shows currently active, ongoing broadcasts and releases.
    3. **NYA / NYP**: Isolates upcoming titles that have not yet started broadcasting or publishing.
    4. **Finished**: Shows completed releases.

### 🎮 Video Games & Interactive Media Tracking
Comprehensive video game tracking with dedicated progress modes, platform icons, and rich metadata:
- **Platform Badges with Dedicated Vector Icons**:
  - Entity details showcase distinct, crisp vector icons tailored to each platform:
    - **PC**: Desktop monitor display icon
    - **PlayStation (PS)**: Authentic PlayStation logo icon
    - **Xbox**: Official Xbox logo icon
    - **Mobile**: Modern smartphone device icon
    - **Nintendo Switch**: Iconic Switch Joy-Con icon
    - **Retro**: Classic arcade joystick icon
    - **Custom / Other**: Gamepad controller icon
  - Color-tinted to match the active theme, replacing generic emojis.
- **Dual Progress Tracking Modes**:
  - **Percentage Mode (0–100%)**: Smooth slider with fine-grained `-5%`, `-1%`, `+1%`, and `+5%` stepper buttons, and automatic "Completed" milestone tag.
  - **Compact Custom Units Mode**: Track game-specific milestones using progression metrics such as **Levels**, **Cities**, **Chapters**, **Missions**, **Hours**, and **Quests**, with optional target caps.
- **Streamlined Edit UI**:
  - Vertical-optimized, compact custom unit controls with quick preset chips and side-by-side progress and target inputs.
- **IGDB, RAWG & SteamGridDB Metadata Integration**:
  - Search, link, and synchronize game details, release dates, platforms, genres, IGDB/RAWG external critic ratings, and SteamGridDB cover/banner artwork.

### 🚀 In-App GitHub Releases & APK Auto-Updater
Keep your app up to date effortlessly without opening a web browser:
- **One-Tap Release Check**: Checks GitHub Releases API for new versions directly in **Settings**.
- **Changelog & Notes Viewer**: Read release titles, notes, and published dates before updating.
- **Built-in APK Downloader**: Downloads the latest release APK in the background with real-time download progress bar, downloaded bytes counter, and system notification.
- **Direct Installer Prompt**: Automatically triggers Android's package installer via secure `FileProvider` upon download completion.

### 🎨 Custom Themes & User Accent Color Picker
Tailor the app's visual identity to your liking:
- **Built-In Anime & Fantasy Theme Packs**: Preset aesthetic themes (*Beyond*, *Gate*, *Sky*, *Slime*) featuring custom header art, themed sprites, and stylized UI flourishes.
- **User Custom Accent Color Picker**: Choose any accent color via hex code input or preset palette with instant live preview across tabs, progress bars, and buttons.
- **OLED Pure Black Base**: Built on true `#000000` blacks for maximum contrast and battery savings on AMOLED/OLED displays.

### 🔐 Profile Security & App Lock / Passkey
- **Per-Profile PIN / Passkey Lock**: Set an optional passkey for any user profile.
- Protect your personal libraries, reading/watching history, and adult content filters from unauthorized access.
- Seamless unlock dialog with persistent local hashing.

### 🔗 Related Media & Franchise Graph
- When viewing entry details, LX Track List automatically catalogs and visualizes related media:
  - **Sequels**, **Prequels**, **Parent Story**, **Side Stories**, **Spin-offs**, and **Adaptations**.
- One-tap addition of related franchise entries directly to your library, or quick jump to existing entries.

### 🎬 Comprehensive Media Support
Track each medium with metrics tailored to its natural progression:
- **Anime**: Total and watched episodes, seasons, airing status (including NYA for upcoming titles), broadcast schedule, countdowns, and series types (TV, Movie, OVA, ONA, Special).
- **Manga & Manhwa**: Read chapters, volumes, or total pages. Flexible progress tracking adapts based on which metrics you choose to populate, with NYP status for future releases.
- **Light Novels**: First-class support with dedicated AniList `NOVEL` GraphQL integration. Automatically maps to authoritative MyAnimeList manga IDs (`idMal`) to prevent mismatching with doujinshis or unrelated manga adaptations.
- **Movies & Live-Action TV Shows**: Track standalone movies or full cinematic franchises, runtime in minutes, TV seasons, and current episode progress.
- **Books**: Track pages read, total chapters, volume progress, ISBN, and authors via Google Books and Open Library.
- **Video Games**: Track game completion percentage (0–100%) or custom progress milestones (Levels, Cities, Chapters, Missions, Hours, Quests). Manage multi-platform libraries with distinct, themed vector icons (PC, PS, Xbox, Switch, Mobile, Retro), play dates, replay counts, and franchise series progression.
- **Custom Categories & Subcategories**: Create custom categories (e.g., *Donghua*, *Web Novels*, *Podcasts*, *Documentaries*) with custom tracking units (Episodes, Chapters, Volumes, Pages) and badge accent colors.

### 🌐 Multi-Source External DB & Cascading Sync
LX Track List integrates with major metadata providers to enrich your library with covers, runtimes, episode counts, scores, and synopses:
- **IGDB & RAWG (Video Games)**: Direct game search, platform identification, release dates, IGDB/RAWG critic scores, and high-resolution cover/grid artwork via SteamGridDB.
- **MyAnimeList (Jikan API & AniList GraphQL)**: Fast, keyless search and metadata synchronization for Anime and Manga.
- **Direct ID Hydration & Robust Sync**: Background and batch synchronization jobs prioritize authoritative direct ID queries (`fetchAnimeDetailsById`, `fetchMangaDetailsById`) via Jikan API v4 and official MyAnimeList OAuth, preventing sync job failures when titles have pre-existing MAL IDs.
- **Official MyAnimeList (MAL) 2-Way Sync**: Built-in OAuth2 authentication allows opt-in, live bi-directional sync of progress, scores, and watch/read statuses directly to your MyAnimeList account.
- **Foreground Sync Service (`SyncForegroundService`)**: Runs intensive batch sync jobs (MAL, TMDB, MangaDex, Books, and ID Resolution) as an Android Foreground Service with live progress notifications and battery optimization management.
- **The Movie Database (TMDB)**: Direct search, seasonal discovery, high-resolution poster retrieval, release dates, runtimes, and season/episode breakdowns for Movies and TV Shows.
  - **ISP Block Bypass**: Built-in DNS-over-HTTPS (DoH via Cloudflare & Google DNS) and automatic fallback routing (`api.themoviedb.org` $\leftrightarrow$ `api.tmdb.org`) ensures uninterrupted connectivity even on restricted networks.
- **MangaDex API**: Full native integration for Manga, Manhwa, and Manhua with direct UUID linking and alternate title matching.
- **Light Novel AniList & MAL Accurate Mapping**: Specialized `AniListNovelApiClient` queries AniList GraphQL strictly for Light Novels (`format: NOVEL`) and resolves the official MyAnimeList manga ID (`idMal`), guaranteeing that clicking "Open in MAL" navigates directly to the authentic Light Novel entry.
- **Intelligent Fallback Cascading**: For Manga & Manhwa, searches cascade seamlessly across **Jikan (MAL) $\rightarrow$ MangaDex $\rightarrow$ AniList $\rightarrow$ Kitsu** to discover and link obscure webtoons, manhwa, and scanlations that MyAnimeList does not index.

### 🔍 Automated "Resolve IDs" Engine & Audit Reports
Easily link unlinked entries, custom records, or Tachiyomi/Mihon imports to official databases:
- **Universal Resolution**: Identifies entries lacking official database IDs across Manga, Manhwa, Books, Movies, and TV Shows.
- **High-Confidence Matching Engine**:
  - Punctuation-agnostic, accent-agnostic, and space-agnostic title matching (e.g. matching `"Never Die Extra"` with `"Neverdie Extra"`).
  - Automatically strips bracketed quality/source tags (e.g. `[Official]`, `[4K]`, `[HDR]`, `(2010)`, `(Webtoon)`).
  - Matches against candidate original titles (e.g. Japanese Kanji/Romaji, Korean Hangul) and alternative titles.
- **Title Preservation**: Preserves the user's custom or recognizable title, while cataloging official canonical names and synonyms into `alternateTitle`.
- **Persistent Audit Reports**: Generates comprehensive `.txt` and `.json` audit reports in `/LXTrackList/data/` detailing resolved, skipped, and unmatched entries.
- **One-Tap Access**: Accessible directly from **Library Multi-Select** (`[ 🔍 Resolve IDs ]`) and **Settings**.

### 🛡️ Smart Backup Re-Import Deduplication & Library Merge
Never worry about duplicate entries when restoring backups after names or IDs change:
- **Multi-Tier Matching**: Re-importing backups automatically identifies existing entries through:
  1. Authoritative external source and ID.
  2. Preserved Tachiyomi extension IDs stored in notes (`[Tachiyomi ID: ...]`).
  3. Alternate titles and synonyms (e.g. `"Wife is School Goddess"` $\leftrightarrow$ `"Picking up a School Beauty to Be My Wife"`).
  4. Normalized space- and punctuation-agnostic titles.
- **Intelligent Field Merging**: Automatically preserves higher reading/watching progress, keeps linked external IDs, merges alternate names, and retains custom notes.
- **Scan & Merge Duplicates Tool**: A dedicated tool in **Settings $\rightarrow$ Conflict Resolution** that scans your library for duplicate entries and merges them safely.

### 👥 Multi-User Profiles with Full Isolation
- Switch seamlessly between different user profiles by tapping the avatar in the top navigation bar.
- Individual libraries, progress, scores, external tracking credentials, theme preferences, and statistics remain completely isolated per profile.

### 📑 Power Library & Batch Management
- **Multi-Select Batch Actions**: Select multiple entries to perform bulk operations in seconds:
  - **Set Status**: Bulk update to *Watching/Reading*, *Completed*, *On-Hold*, *Dropped*, or *Plan to Watch/Read*.
  - **Set Category / Subcategory**: Bulk categorize your items.
  - **Batch DB Sync**: Bulk **Fetch Missing**, **Re-fetch All**, or **Resolve IDs** with background notifications.
  - **Delete**: Bulk removal with confirmation.
- **8 Dynamic Sort Options**: Sort by Title (A-Z, Z-A), Score, Progress, Recently Updated, Date Added, Start Date, or Finish Date.
- **Granular Filters**: Filter simultaneously by Media Type, Status, Priority (Low, Medium, High), Airing Status (All, Airing/Publishing, NYA/NYP, Finished), Score, and Custom Categories.

### ✏️ Context-Aware External DB Sync Dialog
- When manually editing an entry, the **Sync from External DB** dialog automatically adapts its source chips to the item's media type:
  - **Video Games**: Offers `IGDB` and `RAWG` (with SteamGridDB custom artwork).
  - **Manga / Manhwa**: Offers `MAL`, `MangaDex`, and `AniList`.
  - **Movies / TV Shows**: Offers `TMDB`, `IMDb`, and `Rotten Tomatoes`.
  - **Anime**: Offers `MAL`, `AniList`, and `TMDB`.
  - **Books**: Offers `MAL` (for Light Novels), `MangaDex`, `Google Books`, and `Open Library`.
- Horizontally scrollable source pills with single-line enforcement prevent text wrapping.
- Unified **"Open ▾"** button provides clean, one-tap access to IGDB, RAWG, SteamGridDB, TMDB, IMDb, Rotten Tomatoes, MAL, AniList, and Kitsu without cluttering the cards.
- Expandable result cards display high-res cover art, full synopsis, broadcast schedule, release year, ratings, and genre tags.

### 📊 Deep Statistics & Visual Analytics
- Comprehensive distribution breakdown across statuses (*Watching/Reading*, *Completed*, *On-Hold*, *Dropped*, *Plan to Watch/Read*).
- Category distributions, rating score charts, and cumulative counts of consumed episodes, chapters, volumes, and pages.

### 🔄 Open Import & Export (Data Sovereignty)
Your data belongs to you. You can export or import your collection at any time:
- **Standard Storage Structure** (`/storage/emulated/0/LXTrackList/`):
  - `input/`: Drop backup files here for instant one-tap scanning and import.
  - `output/`: Full database JSON exports.
  - `data/`: Automated ID resolution audit reports (`ID_Resolution_Report_*.txt/json`).
  - `backup/`: Automatic database snapshots.
- **Supported Import Formats**:
  - **MyAnimeList XML** (Supports both uncompressed `.xml` and compressed `.xml.gz` exports for anime and manga).
  - **Tachiyomi / Mihon Backups** (`.tachibk` / `.proto.gz`).
  - **Generic CSV** (Standardized column structure).
  - **JSON** (Native full backup format).
- **Export Formats**: Full database backup in human-readable JSON.

---

## 🎨 Design & UI Philosophy

LX Track List features a modern, distraction-free user interface optimized for OLED screens:
- **True Black Background (`#000000`)**: Maximizes battery efficiency and contrast.
- **Electric Neon Accent (`#A7EF60`)**: Vibrant visual feedback for active tabs, progress bars, and action buttons (customizable to any color).
- **Card-Based Entry Layout**: Clean cards presenting cover art, titles, date ranges, progress bars, and status badges.
- **Edge-to-Edge Experience**: Full immersive Android edge-to-edge support with dynamic navigation bars.

---

## 🛠️ Architecture & Tech Stack

The project follows modern Android architecture best practices (MVVM + Repository pattern):

```
app/src/main/kotlin/com/lx/tracklist/
├── data/
│   ├── exporter/          # JSON data exporter
│   ├── importer/          # MAL XML, Tachiyomi, CSV, and JSON parsers
│   ├── local/             # Room Database, DAOs, TypeConverters, Cover Cache, StorageHelper
│   ├── model/             # MediaEntry, UserProfile, CustomCategory, Enums
│   ├── remote/            # Jikan, TMDB, MangaDex, AniList, Kitsu, and MAL OAuth clients
│   │   ├── mal/           # MyAnimeList OAuth2 token store & 2-way sync manager
│   │   ├── AniListNovelApiClient.kt # AniList Light Novel GraphQL & MAL ID mapping
│   │   ├── AppUpdateChecker.kt     # GitHub Releases API client
│   │   ├── AppUpdateDownloader.kt  # In-app APK downloader & installer
│   │   ├── JikanApiClient.kt       # Jikan v4 & AniList GraphQL seasonal client
│   │   ├── KitsuApiClient.kt       # Kitsu JSON:API client & slug resolver
│   │   ├── MangaDexApiClient.kt
│   │   ├── SyncForegroundService.kt # Foreground service for uninterrupted sync
│   │   ├── TmdbApiClient.kt        # TMDB API with DoH & ISP Block Bypass
│   │   └── TachiyomiResolverManager.kt # Universal ID Resolution Engine
│   └── repository/        # MediaRepository (Single source of truth & deduplication)
├── ui/
│   ├── components/        # Reusable Compose widgets (MediaCard, Drawers, Sheets, FilterChips)
│   ├── detail/            # Entry detail view, metadata refresh, and related media manager
│   ├── edit/              # Add and edit media screen with ExternalDbSyncDialog
│   ├── home/              # Main dashboard with status tabs
│   ├── library/           # Searchable library with multi-sort, NYA/NYP filters, and batch operations
│   ├── navigation/        # Compose Navigation graph and destinations
│   ├── search/            # Unified local & multi-database search & Seasonal Discovery sheet
│   │   ├── DiscoveryModels.kt          # Discovery filters, seasons, sort enums & 3-state adult modes
│   │   ├── OriginalLanguageRegistry.kt # ISO 639 language registry, multi-select matching & category filtering
│   │   ├── SearchDiscoverySheet.kt     # Modal discovery sheet (Anime 2x2 grid, Movie/TV years, 18+ toggle, language filter)
│   │   ├── SearchScreen.kt             # Search & Discover UI with scoped filters, rich TV previews & Load More footer
│   │   └── SearchViewModel.kt          # Paging, scoped seasonal search, and discovery state
│   ├── settings/          # Data import/export, profile management, conflict resolution & updater
│   ├── stats/             # Visualization charts and metrics
│   └── theme/             # Material 3 colors, typography, and dark theme
└── util/                  # ExternalLinkHelper, notification helpers, and system utilities
```

### Key Libraries
- **Language**: [Kotlin](https://kotlinlang.org/) 2.x
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/)
- **Database**: [Room](https://developer.android.com/training/data-storage/room) 2.8+ with SQLite
- **Image Loading**: [Coil Compose](https://coil-kt.github.io/coil/compose/) with persistent disk cache
- **Networking**: [OkHttp3](https://square.github.io/okhttp/) with DNS-over-HTTPS (DoH) & [Gson](https://github.com/google/gson)
- **CI/CD**: GitHub Actions workflow for automated builds, testing, and APK artifact publishing.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- Android SDK 36 (Minimum SDK: 26 / Android 8.0 Oreo)
- JDK 17

### Building from Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/AlexLX4773H/LXTrackList.git
   cd LXTrackList
   ```

2. **Configure Environment Keys (Optional)**:
   Copy the example `.env` file:
   ```bash
   cp .env.example .env
   ```
   Add your personal API credentials if you wish to use TMDB or official MAL OAuth:
   ```properties
   TMDB_API_KEY=your_tmdb_api_key_here
   TMDB_READ_ACCESS_TOKEN=your_tmdb_read_access_token_here
   MAL_CLIENT_ID=your_mal_client_id_here
   ```
   *(Note: Jikan, AniList, MangaDex, and Kitsu work out-of-the-box without requiring any API keys).*

3. **Build the APK**:
   ```bash
   # Build Debug APK
   ./gradlew assembleDebug

   # Run Unit Tests
   ./gradlew testDebugUnitTest

   # Build Release APK
   ./gradlew assembleRelease
   ```
   The compiled APK will be located in `app/build/outputs/apk/`.

---

## 🔒 Privacy & Permissions

- **Zero Telemetry**: No trackers, no ad networks, no analytics SDKs, and no external tracking servers.
- **Local Storage**: All media records, notes, scores, and preferences reside solely on your device in local SQLite tables.
- **Minimal Permissions**:
  - `INTERNET`: Required solely for fetching metadata and cover art from Jikan, AniList, TMDB, MangaDex, and Kitsu, checking GitHub releases, or syncing with your official MAL account.
  - `POST_NOTIFICATIONS`: Optional for release/airing notifications, download progress, and background sync progress on Android 13+.
  - `MANAGE_EXTERNAL_STORAGE`: Optional for accessing `/LXTrackList` input, output, and data directories directly.
  - `REQUEST_INSTALL_PACKAGES`: Required solely for initiating direct in-app updates when a new APK is downloaded via the built-in update checker.

---

## 🤝 Contributing

Contributions, bug reports, and feature requests are welcome!
Feel free to open an issue or submit a pull request.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is open-source and available under the terms of the **MIT License**.

---

<p align="center">
  Crafted with care for media enthusiasts and collectors everywhere. 🎬📖🍿
</p>
