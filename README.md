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

**LX Track List** is an open-source, all-in-one personal media tracking app designed for Android. Instead of fragmenting your watchlist across separate platforms (MyAnimeList, AniList, Letterboxd, TV Time, Goodreads), LX Track List consolidates all your entertainment into a single, cohesive, and private database.

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

### 🌐 Rich Metadata & Automatic Sync
- **Jikan (MyAnimeList) Integration**: Fast, keyless search and metadata synchronization for Anime and Manga (synopsis, airing dates, cover art, studios, genres).
- **The Movie Database (TMDB) Integration**: Direct search, poster retrieval, release dates, runtime, and season breakdowns for Movies and TV Shows.
- **Related Media Exploration**: View sequels, prequels, spin-offs, side stories, and parent series. Inspect details and add related media directly to your watchlist with a single tap.
- **Cover Image Search & Persistent Cache**: Dedicated 512 MB offline image cache managed via Coil and Room to ensure your covers remain available offline.

### 👥 Multi-User Profiles with Profile Isolation
- Switch seamlessly between different user profiles by tapping the avatar in the top navigation bar.
- Individual libraries, progress, scores, and statistics remain completely isolated per profile.

### 📑 Power Library & Advanced Filtering
- **8 Dynamic Sort Options**: Sort by Title (A-Z, Z-A), Score, Progress, Recently Updated, Date Added, Start Date, or Finish Date.
- **Granular Filters**: Filter simultaneously by Media Type, Watch/Read Status, Priority (Low, Medium, High), Airing Status, Score, and Custom Categories.
- **Batch Management**: Multi-select mode for fast bulk status updates or deletions.

### 📊 Deep Statistics & Visual Analytics
- Comprehensive distribution breakdown across statuses (*Watching/Reading*, *Completed*, *On-Hold*, *Dropped*, *Plan to Watch/Read*).
- Category distributions, rating score charts, and cumulative counts of consumed episodes, chapters, and pages.

### 🔄 Open Import & Export (Data Sovereignty)
Your data belongs to you. You can export or import your collection at any time using Android's Storage Access Framework:
- **Import Formats**:
  - **MyAnimeList XML** (Supports both uncompressed `.xml` and compressed `.xml.gz` exports for anime and manga).
  - **Tachiyomi / Mihon Backups** (`.tachibk` / `.proto.gz`).
  - **Generic CSV** (Standardized column structure).
  - **JSON** (Native full backup format).
- **Export Formats**: Full database backup in human-readable JSON.

---

## 🎨 Design & UI Philosophy

LX Track List features a modern, distraction-free user interface optimized for OLED screens:
- **True Black Background (`#000000`)**: Maximizes battery efficiency and contrast.
- **Electric Neon Accent (`#A7EF60`)**: Vibrant visual feedback for active tabs, progress bars, and floating action buttons.
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
│   ├── local/             # Room Database, DAOs, TypeConverters, Cover Cache
│   ├── model/             # MediaEntry, UserProfile, CustomCategory, Enums
│   ├── remote/            # Jikan & TMDB API clients and sync engines
│   └── repository/        # MediaRepository (Single source of truth)
├── ui/
│   ├── components/        # Reusable Compose widgets (MediaCard, Drawers, Sheets)
│   ├── detail/            # Entry detail view and related media manager
│   ├── edit/              # Add and edit media screen
│   ├── home/              # Main dashboard with status tabs
│   ├── library/           # Searchable library with multi-sort & filters
│   ├── navigation/        # Compose Navigation3 graph and destinations
│   ├── search/            # Unified local & web search interface
│   ├── settings/          # Data import/export, profile management, and preferences
│   ├── stats/             # Visualization charts and metrics
│   └── theme/             # Material 3 colors, typography, and dark theme
└── util/                  # Notification helpers and system utilities
```

### Key Libraries
- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/)
- **Database**: [Room](https://developer.android.com/training/data-storage/room) with SQLite
- **Image Loading**: [Coil Compose](https://coil-kt.github.io/coil/compose/)
- **Networking**: [OkHttp3](https://square.github.io/okhttp/) & [Gson](https://github.com/google/gson)
- **CI/CD**: GitHub Actions workflow for automated release builds and APK artifact publishing.

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
   Add your personal API credentials if you wish to use TMDB for Movie/TV metadata:
   ```properties
   TMDB_API_KEY=your_tmdb_api_key_here
   TMDB_READ_ACCESS_TOKEN=your_tmdb_read_access_token_here
   ```
   *(Note: Jikan works out-of-the-box without requiring any API keys).*

3. **Build the APK**:
   ```bash
   # Build Debug APK
   ./gradlew assembleDebug

   # Build Release APK
   ./gradlew assembleRelease
   ```
   The compiled APK will be located in `app/build/outputs/apk/`.

---

## 🔒 Privacy & Permissions

- **Zero Telemetry**: No trackers, no ad networks, no analytics SDKs, and no external tracking servers.
- **Local Storage**: All media records, notes, scores, and preferences reside solely on your device in local SQLite tables.
- **Minimal Permissions**:
  - `INTERNET`: Required solely for fetching metadata and cover art from Jikan and TMDB.
  - `POST_NOTIFICATIONS`: Optional for release and airing reminders on Android 13+.

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

This project is open-source and available under the terms of the **MIT License** (or specified repository license).

---

<p align="center">
  Crafted with care for media enthusiasts and collectors everywhere. 🎬📖🍿
</p>

