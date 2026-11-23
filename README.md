# <p align="center"><img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="LX Track List Logo" width="96" style="border-radius: 20%;" /><br>LX Track List</p>

<p align="center">
  <strong>A fast, sleek, offline-first personal media tracking Android application.</strong><br>
  Track Anime, Manga, Manhwa, Movies, TV Shows, Books, Games, and Custom Media in one unified place.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-0.6.16-blue?style=flat-square" alt="Version" />
  <img src="https://img.shields.io/badge/Platform-Android_8.0+_(API_26--36)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin_2.x-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Language" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_&_Material_3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Database-Room_(SQLite)-00599C?style=flat-square&logo=sqlite&logoColor=white" alt="Room" />
  <img src="https://img.shields.io/badge/Theme-OLED_Dark_&_Neon_Lime_/_Custom_Themes-A7EF60?style=flat-square" alt="Theme" />
  <img src="https://img.shields.io/badge/Privacy-100%25_Local_&_Offline_First-success?style=flat-square" alt="Privacy" />
</p>

---

## 🚀 What's New in v0.6.16

- **🔍 Persistent Search Query with Discovery Filters**:
  - In the **Search & Discover** tab, applying or switching Discover filters (e.g. `🔥 Most Popular Games`, `+PC`, seasonal anime, genres, tags) now preserves any active search query text (such as `"lord of the rings"` or `"Pok"`) instead of resetting it to empty.
  - Automatically triggers an immediate scoped search combining both the active text query and the selected discovery filter constraints across all media categories (Games, Movies, TV Shows, Anime, Manga, Manhwa, Light Novels, and Books).
  - Clearing discovery filters via the `✕` chip or reset action smoothly preserves the search text in the input bar and transitions back to an unconstrained search without wiping the results or input field.
  - Upgraded Anime discovery search dispatch in `executeSearch` to route through `fetchAnimeDiscovery`, ensuring complete support for all discovery modes, tags, demographic filters, original languages, and adult content modes when searching with active queries.

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
- **Bidirectional Scoped Title Search & Query Persistence**:
  - **Type First or Filter First**: Type a search query first and then apply Discovery filters, or pick Discovery filters and then type—the search query text is seamlessly preserved across filter additions and modifications.
  - **Active Filter Scoped Search**: When Discovery filters are active (e.g. seasonal anime, release year, platforms, genres/tags, adult mode), searches execute strictly within those constraints across all media categories (Games, Movies, TV Shows, Anime, Manga, Manhwa, Light Novels, and Books).
  - **Seamless Filter Dismissal**: Tapping the `✕` chip on an active filter smoothly transitions back to an unconstrained catalog search for the active query without clearing the search bar or blanking results.
  - **Instant Discovery Restoration**: Clearing the search query via the search bar `✕` button immediately restores the full discovery catalog for the active filter.
- **On-Demand "Load More" Pagination**:
  - Clean, dedicated load-more button at the bottom of discovery and search result lists to smoothly paginate and append deeper archives.
- **Game Platform Multi-Select & Exclusion (Discover Games)**:
  - Multi-select platform filtering powered by `GamePlatformRegistry` with inclusion (`✓` - `OR` condition) and exclusion (`✕`) support across IGDB and RAWG.
  - Platform inclusion/exclusion constraints are embedded directly into upstream server-side queries across IGDB (Apicalypse) and RAWG for accurate, comprehensive combined text and platform search results.
  - Clean, uncompressed instructional header (`[✓] 1 tap = Include (OR)` and `[✕] 2 taps = Exclude`) with live active counts.
  - Comprehensive platform taxonomy: **PC**, **PlayStation** (PS5, PS4, PS3, PS2, PS1, Vita, PSP), **Xbox** (All, Series X/S, One, 360, Original), **Nintendo** (Switch, Wii U, Wii, GameCube, N64, 3DS, DS, GBA, SNES, NES), **Mobile** (Android & iOS), **Other** (Linux, macOS, Stadia), and **Retro / Classics**.
  - Active platform filter chips appear directly beneath search bars with one-tap dismissal.
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
- **Rich Movie & TV Previews with Studios, Publishers & Genre Chips**:
  - TMDB Movie and TV previews in Search automatically display production studio and broadcasting network chips, clickable `#genre` tags, and metadata-formatted synopses in the bottom sheet.
  - TMDB TV show previews automatically fetch authoritative season counts and total episodes (e.g. `3 Seasons • 24 Episodes • 57m/ep`), eliminating "Episodes unknown".
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

### ⏳ Universal "NYA" (Not Yet Aired) & "NYP" (Not Yet Published) Tracking
Clear, accurate status tracking for upcoming and unreleased media across all entertainment categories:
- **Distinct Status Badges**:
  - Prevents unreleased media from being ambiguously labeled as "Airing" or "Publishing".
  - Displays **"NYA"** (Not Yet Aired) for Anime and TV Show episodes and upcoming Movies, **"UNRELEASED"** for upcoming Video Games, and **"NYP"** (Not Yet Published) for Manga, Manhwa, Light Novels, and Books.
- **Universal Release Countdowns & Schedule Banners**:
  - For unreleased titles across **TV Shows**, **Video Games**, **Movies**, **Light Novels**, **Manga**, and **Manhwa**, automatically calculates and displays the release/premiere/publication date and remaining countdown (e.g. `Nov 15, 2027 • In 358d`) directly on library cards and in detail views, matching the real-time countdown banner used for Anime broadcasts.
- **Advanced Airing Status Filter**:
  - Filter your library with dedicated 4-mode toggles in the Advanced Filter Sheet:
    1. **All**: Shows all entries.
    2. **Airing / Publishing**: Shows currently active, ongoing broadcasts and releases.
    3. **NYA / NYP**: Isolates upcoming titles that have not yet started broadcasting or publishing.
    4. **Finished**: Shows completed releases.

### 🎮 Video Games & Interactive Media Tracking
Comprehensive video game tracking with dedicated progress modes, platform icons, rich metadata, and advanced discovery:
- **Advanced Game Discovery & Platform Filtering (Search & Discover)**:
  - **Multi-Select Platform Filter & Exclusion**: Comprehensive sub-menu with inclusion (`✓` - `OR` condition) and exclusion (`✕`) across PC, PlayStation, Xbox, Nintendo, Mobile, Linux/macOS, and Retro platforms.
  - **Next Year & Range Selection**: Quick preset chips for next year (e.g. `2027`), current year (`2026`), and previous year (`2025`), alongside custom `From ▾` and `To ▾` year range pickers, matching Movies discovery.
  - **Game Genre Filtering**: Integrated **Select & Exclude Tags** sub-menu powered by `GameTagRegistry` supporting inclusion (✓) and exclusion (✕) across standard IGDB genres (Action, RPG, Strategy, Visual Novel, etc.).
  - **Original Language & 3-State 18+ Cycle**: Filter discoveries by language, and cycle through Off, Include 18+, and 18+ Only modes with compact, vertically optimized UI sizing.
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
  - **Compact Custom Units Mode**: Track game-specific milestones using progression metrics such as **Levels**, **Cities**, **Chapters**, **Missions**, **Hours**, and **Quests**, with full support for integer and decimal values (e.g. `10.5` or `170.5` Hours) and optional target caps. If no target cap is set for Hours, the app automatically adopts the community 100% average completion time as the display target and computes accurate estimated progress.
- **Streamlined Edit UI & Language Picker**:
  - Vertical-optimized, compact custom unit controls with quick preset chips and side-by-side progress and target inputs.
  - Dedicated Original Language Picker dialog with instant search and regional groupings.
- **Community Average 100% Completion Time & Smart Duration Formatter**:
  - Automatically queries IGDB's `game_time_to_beats` endpoint and RAWG `playtime` to retrieve typical 100% completion durations.
  - Serves as the automated target baseline for Hours tracking when no explicit target cap is specified (e.g., displaying `5 / 29.5 Hours` instead of `5 / ? Hours`).
  - Auto-heals existing entries on detail screen visits, provides an editable "Avg 100% Completion (Hours)" input in Add/Edit, and displays completion badges on detail pages.
  - Smart duration formatting automatically scales very large playtimes and averages into readable human units (days, weeks, months, or years) without compressing UI stepper buttons.
- **IGDB, RAWG & SteamGridDB Metadata Integration**:
  - Search, link, and synchronize game details, release dates, platforms, genres, IGDB/RAWG external critic ratings, community completion times, and SteamGridDB cover/banner artwork.
  - **Studio & Publisher Auto-Enrichment**: Embeds developer (`Studio: ...`) and publisher (`Publisher: ...`) directly into game descriptions, displays genre `#tags` and metadata cards in search preview sheets, and displays platform badges in related game preview sheets.
- **Dedicated External Games Sync Job**:
  - Full batch metadata synchronization in **Settings $\rightarrow$ External Sync Jobs** with status indicator, live progress bar, current game title, and cancel support.
- **Automated "Resolve IDs" for Video Games**:
  - Automatically searches and matches unlinked or unverified game entries to official IGDB or RAWG entries using confident exact title matching.

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
- **Video Games**: Track game completion percentage (0–100%) or custom progress milestones (Levels, Cities, Chapters, Missions, Hours, Quests) with full decimal precision support (e.g. `10.5` hours). Manage multi-platform libraries with distinct, themed vector icons (PC, PS, Xbox, Switch, Mobile, Retro), play dates, replay counts, and franchise series progression.
- **Custom Categories & Subcategories**: Create custom categories (e.g., *Donghua*, *Web Novels*, *Podcasts*, *Documentaries*) with custom tracking units (Episodes, Chapters, Volumes, Pages) and badge accent colors.

### 🌐 Multi-Source External DB & Cascading Sync
LX Track List integrates with major metadata providers to enrich your library with covers, runtimes, episode counts, scores, and synopses:
- **IGDB & RAWG (Video Games)**: Direct game search, platform identification, release dates, IGDB/RAWG critic scores, community average 100% completion times (`game_time_to_beats`), and high-resolution cover/grid artwork via SteamGridDB.
- **Dedicated External Games Sync Job**: Full batch sync job under **Settings $\rightarrow$ External Sync Jobs** with status badge (`SYNCING`, `CONNECTED`, `ONLINE`), live progress bar, current title, pending/failed counts, and cancellation. Supports both **Fetch Missing** and **Re-fetch All**.
- **MyAnimeList (Jikan API & AniList GraphQL)**: Fast, keyless search and metadata synchronization for Anime and Manga.
- **Direct ID Hydration & Robust Sync**: Background and batch synchronization jobs prioritize authoritative direct ID queries (`fetchAnimeDetailsById`, `fetchMangaDetailsById`) via Jikan API v4 and official MyAnimeList OAuth, preventing sync job failures when titles have pre-existing MAL IDs.
- **Official MyAnimeList (MAL) 2-Way Sync**: Built-in OAuth2 authentication allows opt-in, live bi-directional sync of progress, scores, and watch/read statuses directly to your MyAnimeList account.
- **Foreground Sync Service (`SyncForegroundService`)**: Runs intensive batch sync jobs (MAL, TMDB, MangaDex, Books, Games, and ID Resolution) as an Android Foreground Service with live progress notifications and battery optimization management.
- **The Movie Database (TMDB)**: Direct search, seasonal discovery, high-resolution poster retrieval, release dates, runtimes, and season/episode breakdowns for Movies and TV Shows.
  - **Studio & Publisher Metadata**: Automatically captures production companies (**Studio**) and networks (**Publisher**) into entry descriptions, formats search preview cards with metadata chips, and synchronizes across all update jobs.
  - **ISP Block Bypass**: Built-in DNS-over-HTTPS (DoH via Cloudflare & Google DNS) and automatic fallback routing (`api.themoviedb.org` $\leftrightarrow$ `api.tmdb.org`) ensures uninterrupted connectivity even on restricted networks.
- **MangaDex API**: Full native integration for Manga, Manhwa, and Manhua with direct UUID linking and alternate title matching.
- **Light Novel AniList & MAL Accurate Mapping**: Specialized `AniListNovelApiClient` queries AniList GraphQL strictly for Light Novels (`format: NOVEL`) and resolves the official MyAnimeList manga ID (`idMal`), guaranteeing that clicking "Open in MAL" navigates directly to the authentic Light Novel entry.
- **Intelligent Fallback Cascading**: For Manga & Manhwa, searches cascade seamlessly across **Jikan (MAL) $\rightarrow$ MangaDex $\rightarrow$ AniList $\rightarrow$ Kitsu** to discover and link obscure webtoons, manhwa, and scanlations that MyAnimeList does not index.

### 🔍 Automated "Resolve IDs" Engine & Audit Reports
Easily link unlinked entries, custom records, or Tachiyomi/Mihon imports to official databases:
- **Universal Resolution**: Identifies entries lacking official database IDs across Manga, Manhwa, Books, Movies, TV Shows, and Video Games (IGDB & RAWG).
- **High-Confidence Matching Engine**:
  - Punctuation-agnostic, accent-agnostic, and space-agnostic title matching (e.g. matching `"Never Die Extra"` with `"Neverdie Extra"`).
  - Automatically strips bracketed quality/source tags (e.g. `[Official]`, `[4K]`, `[HDR]`, `(2010)`, `(Webtoon)`).
  - Matches against candidate original titles (e.g. Japanese Kanji/Romaji, Korean Hangul) and alternative titles.
- **Video Games Resolution Flow**: Automatically queries IGDB and RAWG APIs, extracts community completion times, platforms, genres, ratings, and release dates, and fetches high-res cover art with Steam CDN library poster fallback.
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
- **Granular Advanced Filters**: Filter simultaneously by Media Type, Status, Priority (Low, Medium, High), Airing Status (All, Airing/Publishing, NYA/NYP, Finished), Score, Custom Categories, **Game Platforms** (multi-select include/exclude for Games), and **Original Language** (native scripts and flag emojis for all media).

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
Gain full visibility into your media consumption habits with precision calculations, proportional status distributions, and interactive score histograms:
- **Comprehensive Status Distribution**: Visual proportional stacked bar charts and breakdown grids across statuses (*Watching/Reading/Playing*, *Plan to Watch/Read/Play*, *Completed*, *On-Hold*, *Dropped*) styled with authentic MAL/AniList theme tokens.
- **Precision Time Tracking & Dynamic Scaling (`DurationFormatter`)**: Converts total consumed minutes into exact 24-hour days (`Days Spent = Total Minutes / 1440.0`), alongside estimated days remaining to finish your backlog (`To Complete`). When hours become very large, values dynamically format into human intervals (days, weeks, months, years) without UI clipping.
- **Unskewed Mean Scores & 10-Point Score Distribution**: Interactive 10-bar frequency histograms (Scores 1–10) with peak detection, percentage tooltips, and an arithmetic mean score that strictly excludes unrated entries (`score == 0` or unrated) to prevent skewing.
- **Multi-Category Metric Breakdown**: Dedicated metrics tailored to each medium (Episodes, Chapters, Volumes, Pages, or Playtime Hours).

#### 🧮 How Statistics, Time & Rewatches Are Calculated

| Category | Primary Progress Units | Standard Consumption Rate | Time Spent Formula (`Days = Minutes / 1440`) | Remaining Time Formula ("To Complete") | Rewatch / Reread Multiplier |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Anime** | Episodes | Episode length (defaults to `24 min` if unlisted) | `(Watched Ep + (Rewatch × Total Ep)) × Ep Length` | `(Total Ep - Watched Ep) × Ep Length / 1440` (uncompleted titles) | Full rewatch cycles multiply total series episodes and add to total time |
| **TV Shows** | Episodes & Seasons | Episode length (defaults to `24 min` if unlisted) | `(Watched Ep + (Rewatch × Total Ep)) × Ep Length` | `(Total Ep - Watched Ep) × Ep Length / 1440` (uncompleted titles) | Rewatches multiply full series episode count and append to watch time |
| **Movies** | Minutes / Runtime | Exact movie runtime (defaults to `100 min` if unlisted) | `((1 if Completed else 0) + Rewatch) × Duration` | `(Duration - Watched Minutes) / 1440` (uncompleted titles) | Each rewatch adds the full film duration in minutes |
| **Manga** | Chapters & Volumes | `5 min` per chapter (or `60 min` per volume) | `(Chapters + (Reread × Total Ch)) × 5 min` *(or Volumes × 60m)* | `(Total Ch - Read Ch) × 5 min / 1440` *(or Remaining Vol × 60m)* | Reread cycles multiply total chapter/volume count and accumulate reading time |
| **Manhwa** | Chapters & Volumes | `5 min` per chapter (or `60 min` per volume) | `(Chapters + (Reread × Total Ch)) × 5 min` *(or Volumes × 60m)* | `(Total Ch - Read Ch) × 5 min / 1440` *(or Remaining Vol × 60m)* | Reread cycles multiply total chapter/volume count and accumulate reading time |
| **Light Novels** | Pages, Chapters, Volumes | `1.5 min` per page (fallback: `10 min`/ch, `180 min`/vol) | `(Pages + (Reread × Total Pages)) × 1.5 min` | `(Total Pages - Read Pages) × 1.5 min / 1440` | Rereads multiply total pages (or chapters/volumes) into cumulative read time |
| **Books** | Pages, Chapters, Volumes | `1.5 min` per page (fallback: `10 min`/ch, `180 min`/vol) | `(Pages + (Reread × Total Pages)) × 1.5 min` | `(Total Pages - Read Pages) × 1.5 min / 1440` | Rereads multiply total pages (or chapters/volumes) into cumulative read time |
| **Video Games** | Custom Units & % | Average 100% completion time (IGDB/RAWG or manual hours) | `((Progress % / 100) + Replay) × Completion Time` *(or Hours × 60m)* | `((100 - Progress %) / 100) × Completion Time / 1440` (uncompleted titles) | Replay counter tracks total completed playthroughs ("Replayed") |
| **Custom Categories**| Custom Units | Inherits Watch or Read rate depending on category type | Computed using respective Video or Reading formulas | Remaining units converted based on category type | Rewatch/reread multiplier mirrors primary media archetype |

> **Key Analytics Rules**:
> - **Days Spent Conversion**: All time calculations internally aggregate exact minutes and divide by **`1440.0`** ($24\text{ hours} \times 60\text{ minutes}$) to yield pure 24-hour days of non-stop consumption.
> - **"To Complete" Remaining Time**: Computed strictly across active uncompleted entries (`Watching`, `Plan to Watch`, `On-Hold`). Entries marked `Completed` contribute 0 to remaining time.
> - **Rewatch / Reread Accounting**: Rewatching or rereading an entry does not overwrite your original progress. Instead, every completed cycle increment (`rewatchCount`) multiplies the total episode/chapter/page/runtime count and accumulates into your total time spent, ensuring lifetime stats are never lost.
> - **Fair Mean Score**: $\text{Mean Score} = \frac{\sum \text{Scores}}{\text{Count of Scored Entries}}$. Entries without a score or scored 0 are strictly excluded from the denominator so unrated backlog items never artificially depress your average.

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
│   ├── remote/            # Jikan, TMDB, MangaDex, AniList, Kitsu, Books, IGDB, RAWG, MAL OAuth clients
│   │   ├── mal/           # MyAnimeList OAuth2 token store & 2-way sync manager
│   │   ├── AniListNovelApiClient.kt # AniList Light Novel GraphQL & MAL ID mapping
│   │   ├── AppUpdateChecker.kt     # GitHub Releases API client
│   │   ├── AppUpdateDownloader.kt  # In-app APK downloader & installer
│   │   ├── BookMetadataManager.kt  # Unified Google Books & Open Library manager
│   │   ├── BookSyncManager.kt      # Books & Novels metadata batch sync manager
│   │   ├── GameMetadataManager.kt  # Unified IGDB, RAWG & SteamGridDB manager
│   │   ├── GameSyncManager.kt      # Video Game metadata & completion time sync manager
│   │   ├── GoogleBooksApiClient.kt # Google Books API client with multi-code language routing
│   │   ├── IgdbApiClient.kt        # IGDB Twitch OAuth API client (genres, platforms, time to beat)
│   │   ├── JikanApiClient.kt       # Jikan v4 & AniList GraphQL seasonal client
│   │   ├── KitsuApiClient.kt       # Kitsu JSON:API client & slug resolver
│   │   ├── MangaDexApiClient.kt
│   │   ├── OpenLibraryApiClient.kt # Open Library Covers & Works API client
│   │   ├── RawgApiClient.kt        # RAWG Video Games Database API client
│   │   ├── SteamGridDbApiClient.kt # SteamGridDB vertical box art client
│   │   ├── SyncForegroundService.kt # Foreground service for uninterrupted sync
│   │   ├── TmdbApiClient.kt        # TMDB API with DoH & ISP Block Bypass
│   │   ├── TmdbSyncManager.kt      # Movie & TV metadata batch sync manager
│   │   └── TachiyomiResolverManager.kt # Universal ID Resolution Engine (Anime, Books, Movies, Games)
│   └── repository/        # MediaRepository (Single source of truth & deduplication)
├── ui/
│   ├── components/        # Reusable Compose widgets (MediaCard, Drawers, Sheets, FilterChips)
│   ├── detail/            # Entry detail view, metadata refresh, and related media manager
│   ├── edit/              # Add and edit media screen with ExternalDbSyncDialog & Language Picker
│   │   └── OriginalLanguagePickerDialog.kt # ISO 639 searchable dialog with regional groupings
│   ├── home/              # Main dashboard with status tabs
│   ├── library/           # Searchable library with multi-sort, NYA/NYP, Platform & Language filters
│   ├── navigation/        # Compose Navigation graph and destinations
│   ├── search/            # Unified local & multi-database search & Seasonal Discovery sheet
│   │   ├── DiscoveryModels.kt          # Discovery filters, seasons, sort enums & 3-state adult modes
│   │   ├── GamePlatformRegistry.kt     # Comprehensive gaming platforms taxonomy & IGDB/RAWG mapping
│   │   ├── GamePlatformsSubMenu.kt     # Multi-select platform include (OR) / exclude submenu
│   │   ├── OriginalLanguageRegistry.kt # ISO 639 language registry, multi-select matching & category filtering
│   │   ├── SearchDiscoveryLanguageSubMenu.kt # Discovery language picker with native script & flag badges
│   │   ├── SearchDiscoverySheet.kt     # Modal discovery sheet (Anime 2x2, Movie/TV years, Games, 18+ toggle)
│   │   ├── SearchScreen.kt             # Search & Discover UI with scoped filters, rich previews & Load More
│   │   └── SearchViewModel.kt          # Paging, scoped seasonal search, and discovery state
│   ├── settings/          # Data import/export, profile management, conflict resolution & updater
│   ├── stats/             # Visualization charts, proportional status distribution & score histograms
│   └── theme/             # Material 3 colors, typography, theme presets & custom accent picker
└── util/                  # DurationFormatter, ExternalLinkHelper, notification helpers, system utilities
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

## 📂 Import & Export Data Samples

LX Track List supports flexible backup, restore, and migration workflows using standard JSON and CSV files. Below are small samples for loading/importing data into the app and the resulting output export.

### 1. Sample Input CSV (`.csv`) for Load / Import

The CSV importer supports standard headers (case-insensitive). Only `title` (or `name`) is required; all other fields are optional with smart defaults.

```csv
title,mediaType,status,score,currentEpisodes,totalEpisodes,currentChapters,totalChapters,notes,tags,subCategory
"Frieren: Beyond Journey's End",ANIME,COMPLETED,10,28,28,0,0,"Masterpiece fantasy journey","fantasy, adventure",
"Solo Leveling",MANHWA,WATCHING,9,0,0,150,200,"High action hunter story","action, fantasy","Webtoon"
"Dune: Part Two",MOVIE,COMPLETED,9,0,0,0,0,"Epic sci-fi cinema","sci-fi, cinema",
"Elden Ring",GAME,COMPLETED,10,0,0,0,0,"Completed 100% achievements","souls, rpg, goty",
```

> **Supported CSV Columns**:
> - `title` / `name` *(Required)*
> - `mediaType` / `category` (`ANIME`, `MANGA`, `MANHWA`, `MOVIE`, `SHOW`, `BOOK`, `GAME`)
> - `status` (`WATCHING`, `COMPLETED`, `PLAN_TO_WATCH`, `ON_HOLD`, `DROPPED`)
> - `score` / `rating` (0–10)
> - `currentEpisodes` / `progress`, `totalEpisodes`
> - `currentVolumes`, `totalVolumes`
> - `currentChapters`, `totalChapters`
> - `description` / `notes`, `user_comments`
> - `tags`, `subCategory`
> - `foreign_database_id`, `foreign_database_source` (e.g. `MAL`, `TMDB`)
> - `runtime` / `duration` (in minutes)
> - `rewatch_count`, `is_rewatching`

### 2. Sample Input JSON (`.json`) for Load / Import

The JSON importer accepts either a backup wrapper object or a raw JSON array of media items:

```json
{
  "app": "LX Track List",
  "version": 1,
  "entries": [
    {
      "title": "Steins;Gate",
      "alternateTitle": "シュタインズ・ゲート",
      "mediaType": "ANIME",
      "status": "COMPLETED",
      "score": 10,
      "currentEpisodes": 24,
      "totalEpisodes": 24,
      "isFavorite": true,
      "description": "A scientist invents time travel through a microwave.",
      "userComments": "One of my absolute favorite sci-fi series.",
      "tags": "sci-fi, thriller, time-travel"
    },
    {
      "title": "Berserk",
      "mediaType": "MANGA",
      "status": "WATCHING",
      "score": 10,
      "currentChapters": 375,
      "totalChapters": 0,
      "trackingUnit": "CHAPTERS",
      "isFavorite": true,
      "tags": "dark fantasy, seinen"
    },
    {
      "title": "Portal 2",
      "mediaType": "GAME",
      "status": "COMPLETED",
      "score": 10,
      "gamePlatforms": "PC, Xbox, PS",
      "gameProgressPercent": 100,
      "isFavorite": true,
      "tags": "puzzle, sci-fi, comedy"
    }
  ]
}
```

*(Note: Direct JSON arrays like `[ { "title": "...", "mediaType": "..." } ]` are also supported).*

### 3. Sample Output Export (`.json`)

When exporting your library from **Settings > Backup & Restore > Export**, the app outputs a structured, pretty-printed JSON file:

```json
{
  "app": "LX Track List",
  "version": 1,
  "exportedAt": 1732233600000,
  "totalCount": 3,
  "entries": [
    {
      "id": 1,
      "userId": 0,
      "title": "Frieren: Beyond Journey's End",
      "mediaType": "ANIME",
      "status": "COMPLETED",
      "score": 10,
      "currentEpisodes": 28,
      "totalEpisodes": 28,
      "episodeLengthMinutes": 24,
      "isFavorite": true,
      "isExternallyTracked": true,
      "externalId": "52991",
      "externalSource": "MAL",
      "description": "After the defeat of the Demon King, elf mage Frieren embarks on a journey.",
      "tags": "fantasy, drama, adventure",
      "priority": "HIGH",
      "rewatchCount": 1,
      "isRewatching": false,
      "createdAt": 1705300000000,
      "updatedAt": 1732233600000
    },
    {
      "id": 2,
      "userId": 0,
      "title": "Dune: Part Two",
      "mediaType": "MOVIE",
      "status": "COMPLETED",
      "score": 9,
      "durationMinutes": 166,
      "movieSeriesName": "Dune",
      "movieSeriesNumber": 2,
      "isFavorite": true,
      "isExternallyTracked": false,
      "externalId": "693134",
      "externalSource": "TMDB",
      "tags": "sci-fi, adventure",
      "priority": "MEDIUM",
      "createdAt": 1709500000000,
      "updatedAt": 1732233600000
    },
    {
      "id": 3,
      "userId": 0,
      "title": "Portal 2",
      "mediaType": "GAME",
      "status": "COMPLETED",
      "score": 10,
      "gamePlatforms": "PC, Xbox, PS",
      "gameTrackingUnit": "PERCENTAGE",
      "gameProgressPercent": 100,
      "replayCount": 2,
      "isReplaying": false,
      "isFavorite": true,
      "isExternallyTracked": true,
      "externalId": "72",
      "externalSource": "IGDB",
      "tags": "puzzle, sci-fi, platformer",
      "priority": "HIGH",
      "createdAt": 1710000000000,
      "updatedAt": 1732233600000
    }
  ]
}
```

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
