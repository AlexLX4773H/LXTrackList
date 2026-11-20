# LX Track List — Video Games Tracking Feature Implementation Plan

> **Target Codebase:** `AlexLX4773H/LXTrackList` (Kotlin, Jetpack Compose, Room DB v17 → v18, OkHttp, Gson, Coil)  
> **Target Implementer:** Antigravity (Gemini 3.8 Flash)  
> **Scope:** Add a built-in **Games** category after Books/LN with standalone & franchise (skippable series) tracking, multi-platform support, flexible progress tracking (percentage or custom user-defined units), release date & related games (prequel/sequel/DLC/remake) in the Entity detail view, and free/free-key metadata & poster providers.

---

## 1. Feature Requirements & UX Design

### 1.1 Category Placement & Status Labels
- **Built-in Category:** Add `MediaType.GAME` (`displayName = "Games"`) to `MediaType.kt` after `BOOK`.
- **Search Category:** Add `SearchCategory.GAMES("Games", "IGDB / RAWG")` to `SearchViewModel.kt` directly after `SearchCategory.LIGHT_NOVELS`.
- **Library & Drawer Ordering:** Place **Games** immediately after **Books** (and after the user's `Light Novels` / `LN` custom category when shown in the category bar/drawer).
- **Status Labels (`MediaStatus.kt`):**
  - `WATCHING` → **"Playing"**
  - `PLAN_TO_WATCH` → **"Plan to Play"**
  - `COMPLETED` → **"Completed"**
  - `ON_HOLD` → **"On Hold"**
  - `DROPPED` → **"Dropped"**
  - Update `MediaStatus.fromString()` to recognize `"PLAYING"` and `"PLAN_TO_PLAY"` for CSV/JSON imports.
- **Category Icon:** `Icons.Default.SportsEsports` (already available via `material-icons-extended`).

### 1.2 Standalone vs. Franchise (Series) Mode with Skippable Entries
Games behave like a hybrid of **Movies** and **TV Shows**:
- **Standalone Game (`isGameFranchise = false`):** Single entry with its own release date, platforms, and flexible progress (percentage or custom unit).
- **Franchise / Series (`isGameFranchise = true`):** Groups multiple games in a series (e.g., *The Witcher*, *Final Fantasy*, *Assassin's Creed*) like seasons in a TV show, **with per-game skip support**:
  - Each item in `gameSeriesEntriesJson` has an explicit status: `PLANNED`, `PLAYING`, `COMPLETED`, `SKIPPED`, or `DROPPED`.
  - **Skipping Behavior:** If a franchise has 4 games and the user marks Game 1 and Game 3 as `SKIPPED`, overall franchise completion is computed **only over non-skipped games** (e.g., Game 2 Completed + Game 4 at 50% = `75%`, displayed as `1/2 played • 2 skipped`).
  - **Optional Granularity:** Each series entry can also store its own `progressPercent` (or `customCurrent`/`customTotal`), `platforms`, `releaseYear`, `externalId`, and `coverUrl`. Users can either track the entire series inside one Franchise entry **or** tap any related entry in the Entity detail view to add it as its own standalone library item.

### 1.3 Multi-Platform Support
- Store platforms on `MediaEntry.gamePlatforms` (comma-separated string, matching the existing `tags` pattern, with helper `getGamePlatformList()`).
- Provide a **multi-select chip picker** in `AddEditScreen` and quick-edit sheet in `DetailScreen` with presets:
  - `PC (Steam)`, `PC (Epic)`, `PC (GOG)`, `PS5`, `PS4`, `Xbox Series X|S`, `Xbox One`, `Nintendo Switch`, `Switch 2`, `Steam Deck`, `Android`, `iOS`, `Retro / Emulated`
  - Plus a `+ Add Custom Platform` free-text input for niche consoles (e.g., `PS2`, `3DS`, `PSP`).
- Auto-populate candidate platforms from IGDB/RAWG on search import, while allowing the user to pick only the platform(s) they actually play on.
- Render compact platform pills on `MediaCard` (up to 2 pills + `+N`) and full platform chips in `DetailScreen`.

### 1.4 Flexible Progress Tracking (Percentage or Custom Units)
Give users per-game flexibility to switch what is tracked at any time:
1. **Percentage Mode (`TrackingUnit.PERCENTAGE`):**
   - Tracks `gameProgressPercent` (`0..100`).
   - Quick `+` / `-` buttons on `MediaCard` adjust by `5%` (configurable or tap progress bar for slider/exact number).
   - Auto-marks `COMPLETED` when reaching `100%`.
2. **Custom Unit Mode (`TrackingUnit.CUSTOM`):**
   - Tracks `customUnitLabel` (e.g., `"Levels"`, `"Cities"`, `"Chapters"`, `"Missions"`, `"Bosses"`, `"Quests"`, `"Hours"`, `"Achievements"`), `customUnitCurrent`, and `customUnitTotal` (optional; `0` means open-ended like `"42 Hours"`).
   - Quick-pick preset chips for common labels + free-text field so users can type any unit name.
   - Quick `+` / `-` buttons on `MediaCard` increment/decrement `customUnitCurrent` by `1`.
   - `displayProgress` examples: `"65%"`, `"12/20 Levels"`, `"4/9 Cities"`, `"38 Hours"`, `"2/4 played (1 skipped)"`.

### 1.5 Entity Detail Screen (Release Date & Related Games)
In `DetailScreen.kt` (the Entity detail view):
- **Release Date:** Reuse `airedStartDate` with label `"Release Date"` for `MediaType.GAME` (and show `"Unreleased"` / `"NYR"` badge if `airedStartDate > now`).
- **Related Games (`DetailRelatedCard`):** Reuse `relatedEntriesJson` (`List<RelatedMediaItem>`) to show:
  - **Prequel** and **Sequel** (inferred chronologically from IGDB `collections` main-line games)
  - **Part of Series** (other main-line games in the same `collection` or `franchise`)
  - **Expansion / DLC**, **Standalone Expansion**, **Remake**, **Remaster**, **Parent Game**
  - Each row displays poster thumbnail, title, release year, relationship badge, external score, and one-tap **Add to Library** (via existing `AddRelatedToLibraryDialog`).

---

## 2. Metadata & Asset Providers: Free Options & Feasibility

### 2.1 Provider Comparison Matrix

| Provider | Cost & License | Auth Method | Rate Limits | Metadata Depth | Franchise / Prequel / Sequel | Vertical Posters (2:3) | Feasibility Verdict |
|---|---|---|---|---|---|---|---|
| **[IGDB API](https://www.igdb.com/api)** (Twitch) | Free for non-commercial use ([IGDB Docs](https://api-docs.igdb.com/)) | Free Twitch Dev account (2FA) → Client ID + Secret → OAuth2 Bearer token (~60d TTL) | **4 req/sec**, 8 concurrent, **no monthly cap** ([IGDB Rate Limits](https://api-docs.igdb.com/)) | Excellent (387k+ games, platforms, release dates, genres, studios, scores, time-to-beat) | **Best:** `collections`, `franchises`, `parent_game`, `expansions`, `dlcs`, `remakes`, `remasters` ([IGDB Docs](https://api-docs.igdb.com/)) | **Yes:** `t_cover_big_2x` (528×748) & `t_1080p` artworks | **Primary Metadata + Default Cover Provider** |
| **[SteamGridDB API v2](https://github.com/SteamGridDB/node-steamgriddb)** | Free | Free account → static Bearer API Key | Fair-use rate limiting (HTTP 429 on bursts) | Artwork only (no synopsis/dates/relations) | None | **Best:** Community 600×900 portrait grids, 1920×620 heroes, transparent logos & icons ([SGDB Guide](https://blog.csdn.net/weixin_33883104/article/details/169869991)) | **Primary Custom Poster / Asset Picker** |
| **[RAWG API](https://rawg.io/apidocs)** | Free for non-commercial use; requires backlink attribution ([RAWG API](https://rawg.io/apidocs)) | Free static API Key (`?key=...`) | **20,000 req/month** ([RAWG API](https://rawg.io/apidocs)) | Very Good (500k+ games, platforms, release dates, Metacritic) | Good (`game-series`, `additions`, `parent-games`) | **Weak:** Primarily landscape `background_image` screenshots | **Secondary / Zero-OAuth Fallback Metadata** |
| **[Giant Bomb API](https://apis.io/providers/giantbomb/)** | Free for non-commercial use | Free static API Key | 200 req/hr per resource, 1 req/sec burst ([APIs.io](https://apis.io/providers/giantbomb/)) | Good (franchises, platforms, releases) | Moderate (franchises) | Mixed aspect ratios | **Not Recommended** (too slow for bulk sync) |
| **Steam Storefront & CDN** ([Steam Rate Limits](https://apis.io/rate-limits/steam/steam-rate-limits/)) | Free (unofficial storefront endpoint; CDN direct URLs) | None | Aggressive 429 on storefront API; CDN image URLs are fast | PC/Steam titles only | None | **Yes:** `library_600x900_2x.jpg` via Steam App ID | **Zero-Key PC Poster Fallback** |
| **Built-in `CoverImageSearchDialog`** | Free (existing WebView) | None | None | Manual image picker | None | User-selected | **Already Built — Manual Fallback** |

### 2.2 Recommended Hybrid Architecture
1. **Primary Metadata & Relations: [IGDB v4](https://api-docs.igdb.com/)**
   - **Why:** No monthly request cap (unlike RAWG's 20k/month), native vertical box-art covers (`t_cover_big_2x`), structured `collections`/`franchises`/`expansions`/`dlcs`/`remakes`/`remasters`/`parent_game` fields, and `first_release_date` Unix timestamps.
   - **Usage Limits & Throttling:**
     - **Monthly Quota:** Unlimited (no monthly cap).
     - **Rate Limit (Throttling):** Strictly **4 requests per second** (HTTP 429 returned if exceeded).
     - **Burst Limit:** Up to 8 requests in a short burst window.
     - **Payload Limit:** Maximum 500 items per query (`limit 500;`).
     - **Built-in Handling:** `GameSyncManager` paces all loop iterations with `RATE_LIMIT_DELAY_MS = 270L` (≤ 3.7 req/s), safely preventing 429 rate limit errors. OAuth tokens are cached in `SharedPreferences` for their ~60-day lifespan.
   - **Auth Handling on Android:** Follow the exact pattern of `TmdbConfig.kt` and `GoogleBooksConfig.kt`:
     - Inject `IGDB_CLIENT_ID` and `IGDB_CLIENT_SECRET` via `.env` / GitHub Secrets into `BuildConfig` (empty in public git repo), with user override fields in **Settings → API Keys**.
     - Implement `IgdbTokenManager` to `POST https://id.twitch.tv/oauth2/token?client_id=...&client_secret=...&grant_type=client_credentials`, cache `access_token` + `expires_at` in `SharedPreferences` (or `EncryptedSharedPreferences`, already a dependency in `app/build.gradle.kts`), and auto-refresh on `401 Unauthorized` or when `< 1 hour` remains.
   - **Querying Trick (Clean Search Results):** Use IGDB's Apicalypse syntax with `where version_parent = null;` to filter out duplicate Collector's/GOTY editions during search ([IGDB Docs](https://api-docs.igdb.com/)).
2. **Fallback Metadata: [RAWG](https://rawg.io/apidocs)**
   - **Usage Limits & Throttling:**
     - **Monthly Quota:** **20,000 requests per calendar month** on free tier (resets on 1st of month).
     - **Rate Limit (Throttling):** **5 requests per second** (HTTP 429 returned if exceeded).
     - **Built-in Handling:** RAWG is used strictly as a zero-OAuth secondary fallback when IGDB is unconfigured or returns zero hits. Typical personal tracking consumes < 1% of the monthly allowance.
   - Optional single API key (`RAWG_API_KEY`) in `.env` / Settings. Used automatically if IGDB credentials are unconfigured or return 0 hits. Add required attribution text (`"Game data powered by RAWG"`) in Settings/Detail footer when RAWG is used.
3. **Poster & Asset Pipeline (How to Get Posters & Other Assets)**
   - **Tier 1 (Automatic on Add/Sync):** IGDB `cover.image_id` → `https://images.igdb.com/igdb/image/upload/t_cover_big_2x/{image_id}.jpg` (528×748, crisp 2:3 portrait that matches `MediaCard` without stretching). Optional detail-screen backdrop from `artworks.image_id` → `t_1080p`.
   - **Tier 2 (High-Res Custom Posters / Alternate Art):** **[SteamGridDB v2](https://github.com/SteamGridDB/node-steamgriddb)** (`STEAMGRIDDB_API_KEY` in `.env` / Settings). Add a **"Search SteamGridDB Posters"** button inside `AddEditScreen` and `DetailScreen`:
     - **Usage Limits & Throttling:**
       - **Monthly Quota:** Unlimited for personal use.
       - **Rate Limit (Throttling):** ~100 requests per minute (~1.6 req/s), with a burst allowance of 30 req / 10s.
       - **Built-in Handling:** Only queried on-demand when the user opens the poster picker sheet (never called during bulk background syncs).
     1. `GET https://www.steamgriddb.com/api/v2/search/autocomplete/{urlEncodedTitle}` (use the English canonical title from IGDB, and cross-check release year if multiple results match).
     2. `GET https://www.steamgriddb.com/api/v2/grids/game/{sgdbId}?dimensions=600x900&mimes=image/png,image/jpeg&types=static&nsfw=false&humor=false` for vertical posters, and `/heroes/game/{sgdbId}?dimensions=1920x620` if you want wide banners.
     3. Present a clean grid of candidate posters for one-tap selection.
   - **Tier 3 (Zero-Key PC Poster Fallback):** If IGDB `external_games` contains a Steam App ID (`external_game_source = 1`), fallback URL: `https://cdn.cloudflare.steamstatic.com/steam/apps/{steamAppId}/library_600x900_2x.jpg` (Unlimited public Cloudflare CDN).
   - **Tier 4 (Manual Fallback):** Existing `CoverImageSearchDialog.kt` (Google / DuckDuckGo / Bing WebView) pre-filled with `"$title game cover art"`.
   - **Local Caching:** All selected poster URLs automatically flow through existing `CoverCacheManager.kt` + Coil disk/memory cache.

---

## 3. Data Model & Database Migration Plan (Room v17 → v18)

### 3.1 Enum Updates
1. **`app/src/main/kotlin/com/lx/tracklist/data/model/MediaType.kt`**
   - Add `val isPlayType: Boolean = false` parameter and `GAME("Games", isWatchType = false, isReadType = false, isPlayType = true)` after `BOOK`.
   - Update `fromString()` to also accept `"GAME"`, `"GAMES"`, `"VIDEO GAME"`, `"VIDEO GAMES"`.
2. **`app/src/main/kotlin/com/lx/tracklist/data/model/TrackingUnit.kt`**
   - Add `PERCENTAGE("Percentage")` and `CUSTOM("Custom Unit")`.
3. **`app/src/main/kotlin/com/lx/tracklist/data/model/MediaStatus.kt`**
   - Update `getDisplayLabel(mediaType: MediaType)`:
     - `WATCHING` → `if (mediaType.isPlayType) "Playing" else if (mediaType.isReadType) "Reading" else "Watching"`
     - `PLAN_TO_WATCH` → `if (mediaType.isPlayType) "Plan to Play" else if (mediaType.isReadType) "Plan to Read" else "Plan to Watch"`
   - Update `fromString()` to match `"PLAYING"` → `WATCHING` and `"PLAN_TO_PLAY"` → `PLAN_TO_WATCH`.

### 3.2 New Supporting Model: `GameSeriesItem.kt`
Create `app/src/main/kotlin/com/lx/tracklist/data/model/GameSeriesItem.kt`:
```kotlin
package com.lx.tracklist.data.model

enum class GameSeriesItemStatus(val displayName: String) {
    PLANNED("Planned"),
    PLAYING("Playing"),
    COMPLETED("Completed"),
    SKIPPED("Skipped"),
    DROPPED("Dropped")
}

data class GameSeriesItem(
    val id: String, // e.g. UUID or externalId
    val order: Int,
    val title: String,
    val externalId: String? = null,
    val externalSource: String? = null,
    val releaseYear: String? = null,
    val coverUrl: String? = null,
    val status: GameSeriesItemStatus = GameSeriesItemStatus.PLANNED,
    val progressPercent: Int = 0,
    val customUnitCurrent: Int = 0,
    val customUnitTotal: Int = 0,
    val platforms: List<String> = emptyList()
)
```

### 3.3 `MediaEntry.kt` New Columns & Helpers
Add 7 nullable/defaulted columns to `MediaEntry` (safe for Gson JSON backup compatibility):
- `val gamePlatforms: String? = null` — Comma-separated platforms (e.g., `"PC (Steam), PS5"`).
- `val isGameFranchise: Boolean = false` — `false` = Standalone, `true` = Franchise/Series.
- `val gameSeriesEntriesJson: String? = null` — JSON array of `GameSeriesItem`.
- `val gameProgressPercent: Int = 0` — `0..100` for `TrackingUnit.PERCENTAGE`.
- `val customUnitLabel: String? = null` — e.g., `"Levels"`, `"Cities"`, `"Chapters"`, `"Hours"`.
- `val customUnitCurrent: Int = 0` — Current count in custom unit mode.
- `val customUnitTotal: Int = 0` — Target total in custom unit mode (`0` = open-ended).

Add helper methods on `MediaEntry`:
- `getGamePlatformList(): List<String>`
- `getGameSeriesEntries(): List<GameSeriesItem>` + companion `formatGameSeriesEntries(items: List<GameSeriesItem>): String?`
- `getFranchiseProgressSummary(): Triple<Int, Int, Int>` → `(completedCount, activeNonSkippedCount, skippedCount)`
- Update `displayProgress`, `progressFraction`, `incrementProgress()`, `decrementProgress()`, and `isNotYetAiredOrPublished` (`when (mediaType)` branches for `MediaType.GAME`):
  - **Franchise (`isGameFranchise == true`):**
    - `active = items.filter { it.status != GameSeriesItemStatus.SKIPPED }`
    - `progressFraction`: average of per-item progress (`COMPLETED = 1.0`, `PLAYING = progressPercent / 100f`, `PLANNED/DROPPED = 0.0`) across `active`; fallback to `1f` if `status == COMPLETED` else `0f`.
    - `displayProgress`: `"$completed/${active.size} games" + if (skipped > 0) " ($skipped skipped)" else ""`.
  - **Standalone + `TrackingUnit.PERCENTAGE`:**
    - `progressFraction = (gameProgressPercent / 100f).coerceIn(0f, 1f)`
    - `displayProgress = "$gameProgressPercent%"`
    - `incrementProgress()`: `+5%` (clamped to `100`, auto-sets `COMPLETED` & `finishDate` at `100`).
    - `decrementProgress()`: `-5%` (clamped to `0`).
  - **Standalone + `TrackingUnit.CUSTOM` (or fallback):**
    - `label = customUnitLabel?.takeIf { it.isNotBlank() } ?: "Units"`
    - `progressFraction`: `if (customUnitTotal > 0) (customUnitCurrent.toFloat() / customUnitTotal).coerceIn(0f, 1f) else if (status == COMPLETED) 1f else 0f`
    - `displayProgress`: `if (customUnitTotal > 0) "$customUnitCurrent/$customUnitTotal $label" else "$customUnitCurrent $label"`
    - `incrementProgress()` / `decrementProgress()`: `+1` / `-1` on `customUnitCurrent` (auto-completes when `customUnitTotal > 0 && next >= customUnitTotal`).

### 3.4 Room Migration (`LXTrackListDatabase.kt`)
Bump `@Database(..., version = 18)` and add `MIGRATION_17_18` to `.addMigrations(...)`:
```kotlin
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE media_entries ADD COLUMN gamePlatforms TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE media_entries ADD COLUMN isGameFranchise INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE media_entries ADD COLUMN gameSeriesEntriesJson TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE media_entries ADD COLUMN gameProgressPercent INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE media_entries ADD COLUMN customUnitLabel TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE media_entries ADD COLUMN customUnitCurrent INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE media_entries ADD COLUMN customUnitTotal INTEGER NOT NULL DEFAULT 0")
    }
}
```

---

## 4. Networking, API Clients & Sync Architecture

Mirror the existing `TmdbConfig.kt` / `TmdbApiClient.kt` / `TmdbSyncManager.kt` and `BookMetadataManager.kt` patterns using the project's existing **OkHttp + Gson** stack (no new Gradle dependencies needed).

### 4.1 New Files in `data/remote/`
1. **`GameApiConfig.kt`**
   - Stores optional `IGDB_CLIENT_ID`, `IGDB_CLIENT_SECRET`, `RAWG_API_KEY`, and `STEAMGRIDDB_API_KEY` from `BuildConfig` (loaded from `.env` in `app/build.gradle.kts`) with runtime overrides in `SharedPreferences` (matching `TmdbConfig.kt`).
   - Also caches Twitch OAuth2 `access_token` + `expires_at` epoch ms.
2. **`GameModels.kt`**
   - `GameSearchResult`, `GameDetails` (title, alternateTitles, summary, coverUrl, artworkUrl, releaseDateMillis, releaseYear, platforms, genres, themes, rating, ratingSource, collectionName, collectionGames, franchiseName, relatedItems, externalId, externalSource, steamAppId).
   - `SteamGridPosterItem(id, url, thumbUrl, width, height, style)`.
3. **`IgdbApiClient.kt`**
   - `ensureValidToken(context)`: exchanges Client ID + Secret at `https://id.twitch.tv/oauth2/token` when missing/expired.
   - `searchGames(context, query, limit = 20)`: `POST https://api.igdb.com/v4/games` with headers `Client-ID` and `Authorization: Bearer <token>` and Apicalypse body:
     ```text
     search "{query}";
     fields name, alternative_names.name, first_release_date, cover.image_id, platforms.name, platforms.abbreviation, total_rating, summary, genres.name, collections.id, collections.name, franchises.id, franchises.name, game_type;
     where version_parent = null;
     limit 20;
     ```
   - `getGameDetails(context, igdbId)`: fetches full details including `collections.games.(id,name,first_release_date,cover.image_id,game_type,total_rating)`, `parent_game.(...)`, `expansions.(...)`, `dlcs.(...)`, `standalone_expansions.(...)`, `remakes.(...)`, `remasters.(...)`, `external_games.(external_game_source,uid)`, and `artworks.image_id`.
   - **Prequel/Sequel Resolution Logic:**
     - Take `collections.games`, filter main-line entries (`game_type == 0` or null), sort ascending by `first_release_date`.
     - Locate current game's index `i`:
       - `i - 1` → `"Prequel"`
       - `i + 1` → `"Sequel"`
       - All other main entries → `"Part of Series"`
     - Append `expansions` / `standalone_expansions` as `"Expansion"`, `dlcs` as `"DLC"`, `remakes` as `"Remake"`, `remasters` as `"Remaster"`, and `parent_game` as `"Parent Game"`.
     - Map each into `RelatedMediaItem` (`externalSource = "IGDB"`, `mediaType = MediaType.GAME`).
   - **Rate-Limit Guard:** Enforce `>= 260ms` spacing between calls (stays safely under 4 req/sec) with 1 retry on HTTP 429 / 401 (refreshing token on 401).
4. **`RawgApiClient.kt`** (Fallback)
   - `GET https://api.rawg.io/api/games?key={key}&search={query}&page_size=20`
   - `GET https://api.rawg.io/api/games/{id}?key={key}`
   - `GET https://api.rawg.io/api/games/{id}/game-series?key={key}` & `/additions?key={key}` for related games.
5. **`SteamGridDbApiClient.kt`** (Poster/Asset Picker)
   - `searchByTitle(context, title)` → `GET https://www.steamgriddb.com/api/v2/search/autocomplete/{encodedTitle}`
   - `getPortraitGrids(context, sgdbGameId)` → `GET https://www.steamgriddb.com/api/v2/grids/game/{id}?dimensions=600x900&mimes=image/png,image/jpeg&types=static&nsfw=false&humor=false`
6. **`GameMetadataManager.kt` & `GameSyncManager.kt`**
   - `GameMetadataManager`: orchestrates IGDB primary → RAWG fallback, and SteamGridDB poster search.
   - `GameSyncManager`: mirrors `TmdbSyncManager.kt` for bulk background sync of `MediaType.GAME` entries; register it in `SyncForegroundService.kt` (`isAnySyncRunning()`, `cancelAllSyncs()`, notification observer).
7. **`app/build.gradle.kts`**
   - Read `IGDB_CLIENT_ID`, `IGDB_CLIENT_SECRET`, `RAWG_API_KEY`, `STEAMGRIDDB_API_KEY` from `.env` / `System.getenv(...)` and expose via `buildConfigField(...)`.

---

## 5. UI & Exhaustive Integration Checklist

Every file below has existing `MediaType` branches or category lists that must be updated so `MediaType.GAME` works across the entire app:

| Layer / File | Required Changes |
|---|---|
| `ui/components/CategorySelectorDrawer.kt` | Add `MediaType.GAME -> Icons.Default.SportsEsports` in `getCategoryIcon()`; ensure Games appears cleanly in built-in category list. |
| `ui/components/MediaCard.kt` | Handle `MediaType.GAME` badges (`"NYR"` for unreleased instead of `"NYA"`/`"NYP"`), display platform pills (`PC • PS5`), display `displayProgress` (`65%`, `12/20 Levels`, or `2/4 played • 1 skipped`), wire `+`/`-` buttons. |
| `ui/detail/DetailScreen.kt` & `DetailViewModel.kt` | 1. Show **"Release Date"** label for `airedStartDate` on `GAME`.<br>2. Show **Platforms** chip row with quick-edit.<br>3. Show **Standalone Progress Card** (mode toggle: `%` slider vs. Custom Unit stepper + editable unit label) **OR** **Franchise Checklist Card** (like TV seasons, with per-game status dropdown: `Planned` / `Playing` / `Completed` / `Skipped` / `Dropped`, per-game progress, and `+ Add Game to Series` button; plus `"Auto-fill from IGDB Series"` button).<br>4. Wire `loadRelatedEntries()` and `refreshMetadata()` in `DetailViewModel` to `GameMetadataManager` so `DetailRelatedCard` shows Prequel, Sequel, DLC, Remake, and Series entries.<br>5. Add `"Find Poster (SteamGridDB)"` option alongside existing cover editor. |
| `ui/edit/AddEditScreen.kt` & `AddEditViewModel.kt` | 1. Add `ExternalDbSource.IGDB` and `ExternalDbSource.RAWG` to the metadata lookup picker when `mediaType == MediaType.GAME`.<br>2. Add **Mode Switch** (`Standalone` vs. `Franchise / Series`).<br>3. Add **Multi-Platform Picker** (preset chips + custom input).<br>4. Add **Progress Tracking Selector** (`Percentage` vs. `Custom Unit` with preset chips: `Levels`, `Cities`, `Chapters`, `Missions`, `Bosses`, `Hours`, `Achievements` + custom text field).<br>5. Add **Franchise Entries Editor** (add/reorder/delete/set `SKIPPED` status per game). |
| `ui/search/SearchViewModel.kt`, `SearchScreen.kt`, `DiscoveryModels.kt`, `SearchDiscoverySheet.kt` | Add `SearchCategory.GAMES` right after `LIGHT_NOVELS`. Route search & discovery (Most Popular Games, Released by Year, Genre/Platform filters) to `GameMetadataManager`. When adding a game from Search, offer a quick option to add as **Standalone** or **Franchise** (if IGDB reports a `collection` with `> 1` main game). |
| `ui/library/LibraryScreen.kt`, `HomeScreen.kt`, `HomeViewModel.kt` | Verify `MediaType.entries` chips, status filter labels (`"Playing"`, `"Plan to Play"`), and Sync Library dialog text mention Games (IGDB/RAWG). |
| `ui/stats/StatsScreen.kt` & `StatsViewModel.kt` | Include `MediaType.GAME` in category breakdown, completion counts, and platform breakdown stat card (optional polish). |
| `ui/settings/SettingsScreen.kt` | Add **Games Metadata & Posters** section under API Configuration for `IGDB Client ID`, `IGDB Client Secret`, `RAWG API Key` (optional fallback), and `SteamGridDB API Key` (optional posters), plus **"Sync Games Metadata"** action button and RAWG/IGDB attribution note. |
| `util/ExternalLinkHelper.kt` | Add external URL builders for `"IGDB"` (`https://www.igdb.com/games/{slugOrId}` or search fallback) and `"RAWG"` (`https://rawg.io/games/{id}`). |
| `data/repository/MediaRepository.kt` | Update `addRelatedToLibrary()` to handle `MediaType.GAME` (`IGDB`/`RAWG`), and ensure `areCompatibleTypes()` keeps `GAME` isolated from watch/read types. |
| `data/exporter/JsonExporter.kt` & `data/importer/JsonImporter.kt` | Verify new `MediaEntry` fields serialize/deserialize cleanly with default values so older JSON backups remain 100% backward compatible. |

---

## 6. Phased Implementation Roadmap for Antigravity (Gemini 3.8 Flash)

> **Working with Antigravity (Gemini 3.8 Flash):** Execute **one phase per prompt** to keep context focused and prevent half-finished refactors. After each phase, run `./gradlew testDebugUnitTest` (and `./gradlew assembleDebug` from Phase 2 onward) before moving to the next phase.

### Phase 1 — Core Data Models & Room Migration (`v17 → v18`)
- **Files:** `MediaType.kt`, `TrackingUnit.kt`, `MediaStatus.kt`, `GameSeriesItem.kt` (new), `MediaEntry.kt`, `LXTrackListDatabase.kt`, `MediaRepository.kt`.
- **Goal:** Add `MediaType.GAME`, `TrackingUnit.PERCENTAGE`, `TrackingUnit.CUSTOM`, `GameSeriesItem`, the 7 new `MediaEntry` columns, progress/skip math, and `MIGRATION_17_18`. Fix all exhaustive `when (mediaType)` compiler errors with safe placeholders where UI comes later.
- **Verification:** Add `GameEntryTest.kt` unit tests covering:
  - Standalone percentage increment/decrement & auto-complete at 100%.
  - Standalone custom unit (`Cities`, `Levels`, open-ended `Hours`).
  - Franchise progress with skipped games (e.g., 4 games, 2 skipped, 1 completed, 1 at 50% → `0.75f` and `"1/2 games (2 skipped)"`).
  - Run `./gradlew testDebugUnitTest`.

### Phase 2 — Config, IGDB OAuth2 Client, RAWG Fallback & SteamGridDB Client
- **Files:** `app/build.gradle.kts`, `GameApiConfig.kt`, `GameModels.kt`, `IgdbApiClient.kt`, `RawgApiClient.kt`, `SteamGridDbApiClient.kt`, `GameMetadataManager.kt`, `ExternalLinkHelper.kt`.
- **Goal:** Implement Twitch Client-Credentials token caching/refresh, IGDB Apicalypse search/detail/collection queries, chronological Prequel/Sequel derivation, RAWG fallback, and SteamGridDB 600×900 portrait grid search.
- **Verification:** Unit tests mocking JSON responses for IGDB collection → Prequel/Sequel/DLC mapping and SteamGridDB filtering (`600x900`, `static`, `nsfw=false`). Run `./gradlew testDebugUnitTest`.

### Phase 3 — Add/Edit Screen & Multi-Platform / Flexible Progress / Franchise Editor
- **Files:** `AddEditViewModel.kt`, `AddEditScreen.kt`, `CategorySelectorDrawer.kt`.
- **Goal:** Support creating and editing `MediaType.GAME` entries: IGDB/RAWG search autofill, Standalone vs. Franchise toggle, multi-platform chip picker, Percentage vs. Custom Unit selector (with presets + custom label), and Franchise series list editor with `SKIPPED` toggle.
- **Verification:** `./gradlew assembleDebug`; manually test adding a standalone game with `"Cities"` unit, a standalone game with `%`, and a 4-game franchise skipping #1 and #3.

### Phase 4 — Entity Detail Screen (Release Date, Related Games, Interactive Progress & Franchise Checklist)
- **Files:** `DetailViewModel.kt`, `DetailScreen.kt`, `MediaCard.kt`.
- **Goal:** Render Game-specific UI on `DetailScreen` and `MediaCard`:
  - Release Date + Platform pills.
  - Inline Percentage slider or Custom Unit stepper with quick unit-switcher.
  - Interactive Franchise checklist (toggle `Planned`/`Playing`/`Completed`/`Skipped`/`Dropped` per game, plus `"Populate Series from IGDB"`).
  - Related Games section (`DetailRelatedCard`) displaying Prequel, Sequel, Series, Expansion/DLC, Remake/Remaster with one-tap Add to Library.
- **Verification:** `./gradlew assembleDebug`; open a game entry, verify release date, skip toggling updates progress bar immediately, and related items correlate with library entries.

### Phase 5 — Search & Discovery Tab Integration
- **Files:** `SearchViewModel.kt`, `SearchScreen.kt`, `DiscoveryModels.kt`, `SearchDiscoverySheet.kt`.
- **Goal:** Add `SearchCategory.GAMES` right after `LIGHT_NOVELS`, wire live search and discovery filters (Popular, By Year, Genre, Platform), and support adding results directly to Library (with Standalone vs. Franchise prompt when part of a collection).
- **Verification:** `./gradlew assembleDebug`; search a franchise game (e.g., *The Witcher 3*) and a standalone game (e.g., *Hollow Knight*) from Search tab.

### Phase 6 — SteamGridDB Poster Picker, Settings API Keys & Background Sync
- **Files:** `SettingsScreen.kt`, `GameSyncManager.kt`, `SyncForegroundService.kt`, `LibraryScreen.kt`, `StatsScreen.kt`, `StatsViewModel.kt`.
- **Goal:**
  - Add API key configuration UI in Settings (`IGDB Client ID/Secret`, `RAWG API Key`, `SteamGridDB API Key`) with connection test buttons.
  - Add SteamGridDB 600×900 poster picker dialog accessible from `AddEditScreen` and `DetailScreen` (falling back to existing `CoverImageSearchDialog`).
  - Register `GameSyncManager` in `SyncForegroundService` and Library Sync dialog.
  - Update `StatsScreen` to include Games.
- **Verification:** `./gradlew testDebugUnitTest assembleDebug`; verify JSON export → import round-trip preserves `gamePlatforms`, `isGameFranchise`, `gameSeriesEntriesJson`, `customUnitLabel`, and `gameProgressPercent`.

---

## 7. Prompting Tips for Antigravity (Gemini 3.8 Flash)
1. **Feed One Phase at a Time:** Reference this plan file in Antigravity and say: *"Implement Phase 1 of `Games-Tracking-Feature-Plan.md`. Do not modify UI screens yet beyond fixing exhaustive `when (mediaType)` compile errors. Run `./gradlew testDebugUnitTest` when done."*
2. **Protect Existing Backups & DB:** Remind Antigravity to keep `fallbackToDestructiveMigration()` as a last resort only — `MIGRATION_17_18` must use `ALTER TABLE ... ADD COLUMN ... DEFAULT ...` so existing user data on your sideloaded device is preserved.
3. **Keep API Secrets Out of Git:** Add `IGDB_CLIENT_ID`, `IGDB_CLIENT_SECRET`, `RAWG_API_KEY`, and `STEAMGRIDDB_API_KEY` to your local `.env` (already gitignored) and to GitHub Actions repository secrets so CI release APKs get the keys automatically.
4. **Prevent Play Protect Regressions:** Do not add any new `AndroidManifest.xml` permissions — Games tracking uses only the existing `INTERNET` permission.
