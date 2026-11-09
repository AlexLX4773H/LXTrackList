# <p align="center"><img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="LX Track List Logo" width="96" style="border-radius: 20%;" /><br>LX Track List</p>

<p align="center">
  <strong>A fast, sleek, offline-first personal media tracking Android application.</strong><br>
  Track Anime, Manga, Manhwa, Movies, TV Shows, Books, and Custom Media in one unified place.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin_17-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_&_Material_3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Database-Room_(SQLite)-00599C?style=flat-square&logo=sqlite&logoColor=white" alt="Room" />
  <img src="https://img.shields.io/badge/Theme-OLED_Dark_&_Neon_Lime-A7EF60?style=flat-square" alt="Theme" />
  <img src="https://img.shields.io/badge/Privacy-100%25_Local_&_Offline_First-success?style=flat-square" alt="Privacy" />
</p>

---

## 📖 Overview

**LX Track List** is an open-source, all-in-one personal media tracking app designed for Android. Instead of fragmenting your watchlist across separate platforms (MyAnimeList, AniList, Letterboxd, TV Time, MangaDex, Tachiyomi, Goodreads), LX Track List consolidates all your entertainment into a single, cohesive, and private database.

Built from the ground up with **Kotlin**, **Jetpack Compose (Material 3)**, and **Room (SQLite)**, the app emphasizes lightning-fast performance, elegant aesthetics (OLED pure black paired with `#A7EF60` electric neon accents), and total data sovereignty.

---

## ✨ Features at a Glance

### 🎬 Comprehensive Media Support
Track each medium with metrics tailored to its natural progression:
- **Anime**: Total and watched episodes, seasons, airing status, broadcast seasons, and series types (TV, Movie, OVA, ONA, Special).
- **Manga & Manhwa**: Read chapters, volumes, or total pages. Flexible progress tracking adapts based on which metrics you choose to populate.
- **Movies & Live-Action TV Shows**: Track standalone movies or full cinematic franchises, runtime in minutes, TV seasons, and current episode progress.
- **Books & Light Novels**: Track pages read, total chapters, volume progress, ISBN, and authors.
- **Custom Categories & Subcategories**: Create custom categories (e.g., *Donghua*, *Web Novels*, *Podcasts*, *Documentaries*) with custom tracking units (Episodes, Chapters, Volumes, Pages) and badge accent colors.

### 🌐 Multi-Source External DB & Cascading Sync
LX Track List integrates with 5 major metadata providers to enrich your library with covers, runtimes, episode counts, scores, and synopses:
- **MyAnimeList (Jikan API)**: Fast, keyless search and metadata synchronization for Anime and Manga.
- **Official MyAnimeList (MAL) 2-Way Sync**: Built-in OAuth2 authentication allows opt-in, live bi-directional sync of progress, scores, and watch/read statuses directly to your MyAnimeList account.
- **The Movie Database (TMDB)**: Direct search, high-resolution poster retrieval, release dates, runtimes, and season/episode breakdowns for Movies and TV Shows.
  - **ISP Block Bypass**: Built-in DNS-over-HTTPS (DoH via Cloudflare & Google DNS) and automatic fallback routing (`api.themoviedb.org` $\leftrightarrow$ `api.tmdb.org`) ensures uninterrupted connectivity even on restricted networks.
- **MangaDex API**: Full native integration for Manga, Manhwa, and Manhua with direct UUID linking and alternate title matching.
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
- Individual libraries, progress, scores, external tracking credentials, and statistics remain completely isolated per profile.

### 📑 Power Library & Batch Management
- **Multi-Select Batch Actions**: Select multiple entries to perform bulk operations in seconds:
  - **Set Status**: Bulk update to *Watching/Reading*, *Completed*, *On-Hold*, *Dropped*, or *Plan to Watch/Read*.
  - **Set Category / Subcategory**: Bulk categorize your items.
  - **Batch DB Sync**: Bulk **Fetch Missing**, **Re-fetch All**, or **Resolve IDs** with background notifications.
  - **Delete**: Bulk removal with confirmation.
- **8 Dynamic Sort Options**: Sort by Title (A-Z, Z-A), Score, Progress, Recently Updated, Date Added, Start Date, or Finish Date.
- **Granular Filters**: Filter simultaneously by Media Type, Status, Priority (Low, Medium, High), Airing Status, Score, and Custom Categories.

### ✏️ Context-Aware External DB Sync Dialog
- When manually editing an entry, the **Sync from External DB** dialog automatically adapts its source chips to the item's media type:
  - **Manga / Manhwa / Books**: Offers `MAL` and `MangaDex`.
  - **Movies / TV Shows**: Offers `TMDB` and `MAL`.
  - **Anime**: Offers `MAL` and `TMDB`.
- Horizontally scrollable source pills with single-line enforcement prevent text wrapping.
- Quick web links (`Open MangaDex`, `Open TMDB`, `Open MAL`) and expand/collapse cards with full synopsis, release dates, ratings, and genre tags.

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
- **Electric Neon Accent (`#A7EF60`)**: Vibrant visual feedback for active tabs, progress bars, and action buttons.
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
│   │   ├── JikanApiClient.kt
│   │   ├── MangaDexApiClient.kt
│   │   ├── TmdbApiClient.kt (with DoH & ISP Block Bypass)
│   │   └── TachiyomiResolverManager.kt (Universal ID Resolution Engine)
│   └── repository/        # MediaRepository (Single source of truth & deduplication)
├── ui/
│   ├── components/        # Reusable Compose widgets (MediaCard, Drawers, Sheets, FilterChips)
│   ├── detail/            # Entry detail view, metadata refresh, and related media manager
│   ├── edit/              # Add and edit media screen with ExternalDbSyncDialog
│   ├── home/              # Main dashboard with status tabs
│   ├── library/           # Searchable library with multi-sort, filters, and batch operations
│   ├── navigation/        # Compose Navigation graph and destinations
│   ├── search/            # Unified local & multi-database search interface
│   ├── settings/          # Data import/export, profile management, conflict resolution & preferences
│   ├── stats/             # Visualization charts and metrics
│   └── theme/             # Material 3 colors, typography, and dark theme
└── util/                  # Notification helpers and system utilities
```

### Key Libraries
- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/)
- **Database**: [Room](https://developer.android.com/training/data-storage/room) with SQLite
- **Image Loading**: [Coil Compose](https://coil-kt.github.io/coil/compose/) with persistent disk cache
- **Networking**: [OkHttp3](https://square.github.io/okhttp/) with DNS-over-HTTPS (DoH) & [Gson](https://github.com/google/gson)
- **CI/CD**: GitHub Actions workflow for automated builds and APK artifact publishing.

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
   *(Note: Jikan, MangaDex, AniList, and Kitsu work out-of-the-box without requiring any API keys).*

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
  - `INTERNET`: Required solely for fetching metadata and cover art from Jikan, TMDB, MangaDex, AniList, and Kitsu, or syncing with your official MAL account.
  - `POST_NOTIFICATIONS`: Optional for release/airing notifications and background sync progress on Android 13+.
  - `MANAGE_EXTERNAL_STORAGE`: Optional for accessing `/LXTrackList` input, output, and data directories directly.

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
