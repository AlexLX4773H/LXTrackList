# LX Track List — Music Tracking & Universal Creator Field Feature Implementation Plan

> **Target Codebase:** `AlexLX4773H/LXTrackList` (Kotlin, Jetpack Compose, Room DB v20 → v21, OkHttp, Gson, Coil)  
> **Target Implementer:** Antigravity (Gemini 3.8 Flash)  
> **Scope:**  
> 1. Add a built-in **Music** category after Games, enabling users to track individual songs with album attribution, artist info, genre tags, flexible "listen" progress, and poster/cover art from free music metadata APIs.  
> 2. Introduce a dedicated **universal `creator` column** on `MediaEntry` (used for Artist in Music, Studio in Anime, Author/Writer in Books & Manga, Developer in Games, Director in Movies, Network/Creator in Shows) instead of embedding these details into the `description` field. Clean up `description` to be pure synopsis/notes across all media types.

---

## 1. Feature Requirements & UX Design

### 1.1 Category Placement & Status Labels
- **Built-in Category:** Add `MediaType.MUSIC` (`displayName = "Music"`) to `MediaType.kt` after `GAME`.
- **New Enum Flag:** Add `val isListenType: Boolean = false` parameter to `MediaType`, set `true` only for `MUSIC`.
- **Search Category:** Add `SearchCategory.MUSIC("Music", "MusicBrainz / Deezer / Spotify")` to `SearchViewModel.kt` directly after `SearchCategory.GAMES`.
- **Library & Drawer Ordering:** Place **Music** immediately after **Games** in the category bar/drawer and all `MediaType.entries` chip rows.
- **Status Labels (`MediaStatus.kt`):**
  - `WATCHING` → **"Listening"**
  - `PLAN_TO_WATCH` → **"Plan to Listen"**
  - `COMPLETED` → **"Completed"**
  - `ON_HOLD` → **"On Hold"**
  - `DROPPED` → **"Dropped"**
  - Update `MediaStatus.fromString()` to recognize `"LISTENING"` and `"PLAN_TO_LISTEN"` for CSV/JSON imports.
- **Category Icon:** `Icons.Default.MusicNote` (already available via `material-icons-extended`).
- **Rewatch/Replay Wording:** When `mediaType.isListenType`, use "Replay" instead of "Rewatch" or "Re-read" (e.g., `rewatchCount` displays as "Replay Count").

---

### 1.2 The Universal `creator` Field (Replacing Embedded Description Metadata)

#### The Problem It Solves
Previously in LX Track List:
- For **Games**, `Developer: ...` and `Publisher: ...` were appended as plain text into `MediaEntry.description` via `buildGameDescriptionWithMetadata()`.
- For **Books**, `Author: ...`, `ISBN: ...`, and `Publisher: ...` were appended as plain text into `MediaEntry.description` via `buildDescriptionWithMetadata()`.
- For **Anime/Manga**, studio or author names were either mixed into synopsis or missing from dedicated fields.
- Storing metadata inside the description makes it impossible to cleanly query, sort, display in sub-headers, edit in dedicated fields, or style independently.

#### The Solution: Dedicated `creator` Column
Add `val creator: String? = null` directly to `MediaEntry`.
This single column serves a polymorphic purpose based on `MediaType`:

| `MediaType` | Role / Label in UI | Example Data | Sources |
|---|---|---|---|
| **`MUSIC`** | **Artist** | `"Daft Punk"`, `"Queen"`, `"Yoasobi"` | MusicBrainz `artist-credit`, Deezer `artist.name`, Spotify `artists` |
| **`ANIME`** | **Studio** | `"Ufotable"`, `"MAPPA"`, `"Bones"` | AniList `studios`, Jikan `studios` |
| **`MANGA` / `MANHWA`** | **Author / Artist** | `"Eiichiro Oda"`, `"Chugong, DUBU"` | MangaDex `author/artist`, AniList `staff` |
| **`BOOK` / `LIGHT_NOVEL`** | **Author** | `"Brandon Sanderson"`, `"Tappei Nagatsuki"` | Google Books `authors`, Open Library `author_name` |
| **`GAME`** | **Developer** | `"FromSoftware"`, `"CD Projekt Red"` | IGDB `involved_companies (developer)`, RAWG `developers` |
| **`MOVIE`** | **Director / Studio** | `"Christopher Nolan"`, `"Studio Ghibli"` | TMDB `credits.crew (Director)` / `production_companies` |
| **`SHOW`** | **Creator / Network** | `"Vince Gilligan"`, `"HBO"` | TMDB `created_by` / `networks` |

#### Helper Methods on `MediaEntry`:
```kotlin
fun getCreatorRoleLabel(): String = when (mediaType) {
    MediaType.MUSIC -> "Artist"
    MediaType.ANIME -> "Studio"
    MediaType.MANGA, MediaType.MANHWA -> "Author"
    MediaType.BOOK -> "Author"
    MediaType.GAME -> "Developer"
    MediaType.MOVIE -> "Director"
    MediaType.SHOW -> "Creator / Network"
}

// Convenient semantic accessors
val artist: String? get() = creator
val author: String? get() = creator
val studio: String? get() = creator
val developer: String? get() = creator
```

#### Cleaning Up `description`
With `creator` as a first-class field:
1. `description` stores **only pure synopsis / plot summary / lyrics / notes**.
2. Deprecate and phase out `buildGameDescriptionWithMetadata()` and `buildDescriptionWithMetadata()` appending logic.
3. During Room migration (`MIGRATION_20_21`), automatically parse existing legacy descriptions with `"Author: "`, `"Developer: "`, or `"Studio: "` to backfill `creator` and strip those lines from `description`.

---

### 1.3 Music as a Hybrid of Movies & Games

Music tracking is closest to **Movies** in structure (single finite item with a duration) combined with **Games**-style status wording:

| Aspect | Movies Pattern | Music Adaptation |
|---|---|---|
| **Entity Type** | Single finite item | Single song / single |
| **Creator** | Director / Studio | **Artist** (stored in new `creator` field) |
| **Duration** | `durationMinutes` (runtime) | `durationMinutes` (song length in seconds, stored as seconds for precision; display as `m:ss`) |
| **Progress Toggle** | Watched ↔ Not Watched | Listened ↔ Not Listened (toggle like Movies) |
| **Series/Collection** | `movieSeriesName` (franchise) | Reuse as **Album Name** |
| **Series Number** | `movieSeriesNumber` (position in franchise) | Reuse as **Track Number** in album |
| **Related Items** | `relatedEntriesJson` (sequels, prequels) | Reuse for **Other tracks from same album** / **Other songs by same artist** |
| **Status Labels** | Watching / Plan to Watch | Listening / Plan to Listen (via `isListenType`) |
| **Quick Action** | Toggle Completed ↔ Watching | Toggle Completed ↔ Listening (identical `incrementProgress()` toggle behavior) |

---

### 1.4 Music-Specific & Reused Field Mapping

#### Reused Existing `MediaEntry` Fields
| Existing Field | Music Usage | Notes |
|---|---|---|
| `title` | Song title | Primary display |
| `alternateTitle` | Alternative song names / romanized titles | Same as other categories |
| `durationMinutes` | **Song duration in seconds** | Repurpose: store seconds (not minutes) for Music only. Display as `3:45`. Max realistic value ~3600 (1 hour). Movies continue using minutes. |
| `movieSeriesName` | **Album name** | The album this song belongs to |
| `movieSeriesNumber` | **Track number** in album | Position in track listing |
| `tags` | Genre tags (comma-separated) | e.g., "Rock, Alternative, Indie" |
| `description` | Song description / lyrics excerpt / notes | **Pure synopsis / notes** (no artist/album text pollution) |
| `coverImageUri` | Album art / single cover | Poster image |
| `externalScore` | External rating (if available) | From MusicBrainz/Deezer |
| `airedStartDate` | **Release date** | Song/single release date |
| `relatedEntriesJson` | Other tracks from same album, other songs by artist | Reuse `RelatedMediaItem` |
| `externalId` | MusicBrainz MBID / Deezer Track ID / Spotify ID | External catalog ID |
| `externalSource` | `"MusicBrainz"` / `"Deezer"` / `"Spotify"` | Provider tag |

#### New `MediaEntry` Columns (2 new fields total)
| New Field | Type | Default | Description |
|---|---|---|---|
| `creator` | `String?` | `null` | **Universal creator column** (Artist for Music; Studio/Author/Developer for other categories) |
| `musicAlbumId` | `String?` | `null` | External album ID (MusicBrainz Release Group MBID / Deezer Album ID) for fetching album art and linking related tracks |

---

### 1.5 Progress Tracking

Music progress behaves identically to Movies — a simple **toggle**:

- **`incrementProgress()`:** Toggles between `COMPLETED` ↔ `WATCHING` (displayed as "Listening").
- **`progressFraction`:** `1f` if `COMPLETED`, else `0f`.
- **`displayProgress`:** Shows duration string (e.g., `"3:45"`) + album/track info. If completed: `"Listened"`. If not yet started: `"Planned"`.
- **No partial playback tracking** (unlike movies' `currentTimeMinutes`). Songs are short enough that partial tracking adds friction without value. Users who want it can still use `currentTimeMinutes`.

---

### 1.6 Entity Detail & MediaCard UI Design

#### On `MediaCard.kt` (All Categories)
- When `!entry.creator.isNullOrBlank()`:
  - Display `creator` as the prominent subtitle under the title (e.g., for Music: `"Daft Punk"`, for Books: `"Brandon Sanderson"`, for Games: `"FromSoftware"`, for Anime: `"Ufotable"`).
  - Falls back to `alternateTitle` or release date if `creator` is empty.

#### On `DetailScreen.kt` (All Categories)
- **Prominent Creator Header:** Display the creator below the title using `entry.getCreatorRoleLabel()`:
  - Music: `"Artist: Daft Punk"`
  - Anime: `"Studio: Ufotable"`
  - Books: `"Author: Brandon Sanderson"`
  - Games: `"Developer: FromSoftware"`
- **Album / Series Pill (Music):** Chip showing album name + track number (e.g. `"Track 5 • Discovery"`).
- **Release Date:** Reuse `airedStartDate` with label `"Release Date"`.
- **Duration:** Formatted as `m:ss` for Music.
- **Genre Tags:** Standard tag chips from `tags`.
- **Clean Description Card:** Displays pure synopsis or user notes without the redundant "Author: ...", "Developer: ..." text at the bottom.
- **Related Tracks Section:**
  - **Same Album** — Other tracks from the same album
  - **Same Artist** — Popular tracks by the same artist
  - Each row: album art thumbnail, song title, artist, duration, one-tap **Add to Library**
- **Cover Art:** Full album art display, with existing `CoverImageSearchDialog` for manual override.

#### On `AddEditScreen.kt` (All Categories)
- **Dedicated "Creator" Input Field:**
  - Label dynamically reflects the media type: `"Artist"` for Music, `"Author"` for Books/Manga, `"Studio"` for Anime, `"Developer"` for Games, `"Director"` for Movies.
  - Allows direct editing of the creator independent of `description`.
- **Album & Track Number Inputs** when `mediaType == MediaType.MUSIC`.
- **Duration Input (`m:ss` or seconds)** when `mediaType == MediaType.MUSIC`.

---

## 2. Metadata & Asset Providers: Free Options & Feasibility

### 2.1 Provider Comparison Matrix

| Provider | Cost & License | Auth Method | Rate Limits | Metadata Depth | Album Art | Songs/Tracks | Feasibility Verdict |
|---|---|---|---|---|---|---|---|
| **[MusicBrainz](https://musicbrainz.org/doc/MusicBrainz_API)** | 100% Free & Open | **No API key** — only requires descriptive `User-Agent` header | **1 req/sec** (HTTP 503 if exceeded) | Excellent (artists, releases, recordings, relationships, ISRCs, 65M+ recordings) | Via **Cover Art Archive** (separate free API, also no key) | Yes — recordings, releases, release groups | **Primary Metadata Provider** |
| **[Cover Art Archive](https://coverartarchive.org/)** (CAA) | 100% Free & Open | **No API key** — only requires `User-Agent` | ~1-3 req/sec (polite use) | Cover art only (front, back, booklet) | **Yes:** `front-500` (500px) and `front-1200` (1200px) square covers | N/A | **Primary Cover Art Provider** |
| **[Deezer API](https://developers.deezer.com/api)** | Free for public catalog | **No API key needed** for search & catalog | ~50 req/5sec | Good (tracks, albums, artists, genres, 90M+ tracks) | **Yes:** `cover_big` (500×500) and `cover_xl` (1000×1000) | Yes — full track catalog with previews | **Secondary / Zero-Auth Fallback** |
| **[Spotify Web API](https://developer.spotify.com/documentation/web-api)** | Free (Dev Mode) | OAuth2 Client Credentials (free app registration) | Rolling 30s window (unpublished exact limit); 5 test users in Dev Mode | Very Good (tracks, albums, artists, genres, popularity, 100M+ tracks) | **Yes:** `album.images[]` up to 640×640 | Yes — full catalog | **Optional Enhancement (Requires OAuth)** |
| **[Last.fm API](https://www.last.fm/api)** | ~~Free~~ → **Now requires Last.fm Pro subscription** for API keys | API Key (paid) | ~5 req/sec | Good (artist info, album info, tags, similar artists) | Yes (medium-res) | Yes | **Not Recommended** (no longer free) |
| **[iTunes Search API](https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/)** | Free | **No API key** | ~20 req/min | Basic (track, album, artist, genre, preview URL) | **Yes:** `artworkUrl100` (100×100, replaceable to 600×600) | Yes — Apple Music catalog | **Supplementary Zero-Key Fallback** |
| **Built-in `CoverImageSearchDialog`** | Free (existing WebView) | None | None | Manual image picker | User-selected | None | **Already Built — Manual Fallback** |

### 2.2 Recommended Hybrid Architecture

#### 1. **Primary Metadata: [MusicBrainz API](https://musicbrainz.org/doc/MusicBrainz_API)**
- **Why:** 100% free, no API key needed, richest open music database (65M+ recordings), structured relationships (artist → releases → recordings), and community-curated data.
- **Usage Limits & Throttling:**
  - **Monthly Quota:** Unlimited (no monthly cap).
  - **Rate Limit:** Strictly **1 request per second** (HTTP 503 on burst).
  - **Built-in Handling:** `MusicSyncManager` paces all iterations with `RATE_LIMIT_DELAY_MS = 1100L` (safely under 1 req/s). **No token to manage.**
  - **Mandatory:** Must send `User-Agent: LXTrackList/<version> (contact@email.com)` header on every request.
- **Endpoints Used:**
  - `GET /ws/2/recording/?query="{title}" AND artist:"{artist}"&fmt=json` — Search songs
  - `GET /ws/2/release-group/{mbid}?inc=releases+artists&fmt=json` — Album details
  - `GET /ws/2/artist/{mbid}?inc=release-groups&fmt=json` — Artist discography
  - `GET /ws/2/release/{mbid}?inc=recordings+artist-credits&fmt=json` — Track listing for album

#### 2. **Primary Cover Art: [Cover Art Archive](https://coverartarchive.org/)**
- **Why:** Directly tied to MusicBrainz release IDs, completely free, no key, high-res square covers (500px and 1200px).
- **Usage:**
  - `GET https://coverartarchive.org/release/{MBID}/front-500` — 500px front cover
  - `GET https://coverartarchive.org/release-group/{RELEASE_GROUP_MBID}/front` — Release group cover (fallback)
- **Rate Limit:** Polite use (~1-3 req/s), `User-Agent` required.

#### 3. **Secondary / Zero-Auth Fallback: [Deezer API](https://developers.deezer.com/api)**
- **Why:** No API key at all (not even a User-Agent requirement), 90M+ track catalog, includes high-res square album art (up to 1000×1000), and returns song duration in seconds natively.
- **Usage Limits & Throttling:**
  - **Rate Limit:** ~50 requests per 5 seconds (~10 req/s).
  - **Built-in Handling:** Only used when MusicBrainz returns zero hits or the user explicitly selects Deezer as source.
- **Endpoints Used:**
  - `GET https://api.deezer.com/search/track?q={query}` — Search tracks
  - `GET https://api.deezer.com/album/{id}` — Album details with cover art
  - `GET https://api.deezer.com/artist/{id}/top` — Top tracks by artist
  - `GET https://api.deezer.com/album/{id}/tracks` — Track listing for related tracks

#### 4. **Optional Enhancement: [Spotify Web API](https://developer.spotify.com/documentation/web-api)**
- **Why:** Largest catalog (100M+ tracks), excellent search quality, genre/popularity data. OAuth is straightforward (Client Credentials flow, same as IGDB's Twitch OAuth pattern already built).
- **Usage Limits & Throttling:**
  - **Rate Limit:** Dynamic rolling 30-second window (HTTP 429 + `Retry-After`).
  - **Dev Mode:** 5 authorized test users (fine for personal use).
  - **Built-in Handling:** Reuse the same `SharedPreferences` token caching pattern from `GameApiConfig.kt` / `IgdbApiClient.kt`. Token refresh via `POST https://accounts.spotify.com/api/token` with `client_credentials` grant.
- **Config:** Optional `SPOTIFY_CLIENT_ID` + `SPOTIFY_CLIENT_SECRET` in `.env` / Settings (like IGDB).

#### 5. **Zero-Key Supplementary: [iTunes Search API](https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/)**
- **Why:** Completely free, no API key, provides artwork URLs that can be upscaled to 600×600 by string replacement (`100x100` → `600x600`).
- **Usage:** Fallback cover art source when MusicBrainz/CAA returns no artwork.
  - `GET https://itunes.apple.com/search?term={query}&media=music&entity=song&limit=10`

### 2.3 Cover Art / Poster Pipeline (How to Get Album Art)

| Tier | Source | Resolution | Auth | When Used |
|---|---|---|---|---|
| **Tier 1** | Cover Art Archive via MusicBrainz MBID | 500px / 1200px square | None (User-Agent only) | Automatic on Add/Sync when MusicBrainz is primary source |
| **Tier 2** | Deezer `cover_xl` | 1000×1000 square | None | Automatic when Deezer is source, or CAA has no art |
| **Tier 3** | Spotify `album.images[0].url` | Up to 640×640 | OAuth2 (optional) | When Spotify is configured and selected |
| **Tier 4** | iTunes Search API `artworkUrl100` → replace `100x100` with `600x600` | 600×600 | None | Zero-key fallback when all above fail |
| **Tier 5** | Existing `CoverImageSearchDialog.kt` (Google/DuckDuckGo WebView) | User-selected | None | Manual fallback, pre-filled with `"$title $artist album cover"` |
| **Caching** | `CoverCacheManager.kt` + Coil disk/memory cache | N/A | N/A | All selected URLs automatically flow through existing cache |

---

## 3. Data Model & Database Migration Plan (Room v20 → v21)

### 3.1 Enum Updates

#### `MediaType.kt`
Add `val isListenType: Boolean = false` parameter:
```kotlin
enum class MediaType(
    val displayName: String,
    val isWatchType: Boolean,
    val isReadType: Boolean,
    val isPlayType: Boolean = false,
    val isListenType: Boolean = false
) {
    ANIME("Anime", true, false),
    MANGA("Manga", false, true),
    MANHWA("Manhwa", false, true),
    MOVIE("Movies", true, false),
    SHOW("Shows", true, false),
    BOOK("Books", false, true),
    GAME("Games", false, false, true),
    MUSIC("Music", false, false, false, true);

    companion object {
        fun fromString(value: String): MediaType {
            val upper = value.trim().uppercase()
            if (upper == "GAME" || upper == "GAMES" || upper == "VIDEO GAME" || upper == "VIDEO GAMES") return GAME
            if (upper == "MUSIC" || upper == "SONG" || upper == "SONGS" || upper == "TRACK" || upper == "TRACKS") return MUSIC
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) 
            } ?: ANIME
        }
    }
}
```

#### `MediaStatus.kt`
Update `getDisplayLabel()`:
```kotlin
WATCHING -> when {
    mediaType.isListenType -> "Listening"
    mediaType.isPlayType -> "Playing"
    mediaType.isReadType -> "Reading"
    else -> "Watching"
}
PLAN_TO_WATCH -> when {
    mediaType.isListenType -> "Plan to Listen"
    mediaType.isPlayType -> "Plan to Play"
    mediaType.isReadType -> "Plan to Read"
    else -> "Plan to Watch"
}
```

Update `fromString()` to match `"LISTENING"` → `WATCHING` and `"PLAN_TO_LISTEN"` → `PLAN_TO_WATCH`.

---

### 3.2 `MediaEntry.kt` New Columns

Add 2 nullable columns:
```kotlin
// Universal creator column (Artist for Music; Studio, Author, Developer for others)
val creator: String? = null,

// Music album linking ID
val musicAlbumId: String? = null
```

Add index to `MediaEntry`:
```kotlin
indices = [
    ...
    Index("creator")
]
```

---

### 3.3 `MediaEntry.kt` Helpers & Branch Updates

```kotlin
fun getCreatorRoleLabel(): String = when (mediaType) {
    MediaType.MUSIC -> "Artist"
    MediaType.ANIME -> "Studio"
    MediaType.MANGA, MediaType.MANHWA -> "Author"
    MediaType.BOOK -> "Author"
    MediaType.GAME -> "Developer"
    MediaType.MOVIE -> "Director"
    MediaType.SHOW -> "Creator / Network"
}

val artist: String? get() = creator
val author: String? get() = creator
val studio: String? get() = creator
val developer: String? get() = creator
```

#### `calculateSpentTimeMinutes()` — New `MUSIC` branch:
```kotlin
MediaType.MUSIC -> {
    // durationMinutes stores seconds for music
    val durationSec = (durationMinutes ?: 0).toDouble()
    val totalListens = (if (status == MediaStatus.COMPLETED) 1 else 0) + rewatchCount
    (totalListens * durationSec) / 60.0
}
```

#### `calculateRemainingTimeMinutes()` — New `MUSIC` branch:
```kotlin
MediaType.MUSIC -> {
    if (status == MediaStatus.COMPLETED) 0.0
    else (durationMinutes ?: 0).toDouble() / 60.0
}
```

#### `displayProgress` — New `MUSIC` branch:
```kotlin
MediaType.MUSIC -> {
    val durSec = durationMinutes ?: 0
    val durStr = if (durSec > 0) {
        val m = durSec / 60; val s = durSec % 60
        "${m}:${s.toString().padStart(2, '0')}"
    } else ""
    val albumStr = movieSeriesName?.let { alb ->
        val trackStr = if (movieSeriesNumber != null) "Track $movieSeriesNumber" else ""
        listOf(trackStr, alb).filter { it.isNotBlank() }.joinToString(" • ")
    } ?: ""
    listOf(durStr, albumStr).filter { it.isNotBlank() }.joinToString(" • ")
        .ifBlank { if (status == MediaStatus.COMPLETED) "Listened" else "Planned" }
}
```

#### `progressFraction` — New `MUSIC` branch:
```kotlin
MediaType.MUSIC -> {
    if (status == MediaStatus.COMPLETED) 1f else 0f
}
```

#### `incrementProgress()` — New `MUSIC` branch:
```kotlin
MediaType.MUSIC -> {
    // Toggle between Completed and Listening (same as Movies)
    val newStatus = if (status == MediaStatus.COMPLETED) MediaStatus.WATCHING else MediaStatus.COMPLETED
    val isDone = newStatus == MediaStatus.COMPLETED
    val newRewatchCount = if (isDone && isRewatching) rewatchCount + 1 else rewatchCount
    val newIsRewatching = if (isDone) false else isRewatching
    copy(
        status = newStatus,
        rewatchCount = newRewatchCount,
        isRewatching = newIsRewatching,
        updatedAt = now,
        finishDate = if (status != MediaStatus.COMPLETED && finishDate == null) now else finishDate
    )
}
```

#### `decrementProgress()` — New `MUSIC` branch:
```kotlin
MediaType.MUSIC -> copy(updatedAt = now) // No-op for songs (toggle only via increment)
```

---

### 3.4 Room Migration (`LXTrackListDatabase.kt`)
Bump `@Database(..., version = 21)` and add `MIGRATION_20_21`:
```kotlin
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add universal creator and musicAlbumId columns
        db.execSQL("ALTER TABLE media_entries ADD COLUMN creator TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE media_entries ADD COLUMN musicAlbumId TEXT DEFAULT NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_media_entries_creator ON media_entries(creator)")

        // 2. Data backfill: populate creator from existing descriptions containing "Author: " or "Developer: "
        // and clean the metadata lines out of the description
        try {
            // Backfill books: "Author: <name>"
            db.execSQL("""
                UPDATE media_entries 
                SET creator = trim(substr(notes, instr(notes, 'Author: ') + 8, 
                    CASE 
                        WHEN instr(substr(notes, instr(notes, 'Author: ') + 8), char(10)) > 0 
                        THEN instr(substr(notes, instr(notes, 'Author: ') + 8), char(10)) - 1
                        ELSE length(notes)
                    END
                ))
                WHERE creator IS NULL AND notes LIKE '%Author: %'
            """.trimIndent())

            // Backfill games: "Developer: <name>"
            db.execSQL("""
                UPDATE media_entries 
                SET creator = trim(substr(notes, instr(notes, 'Developer: ') + 11, 
                    CASE 
                        WHEN instr(substr(notes, instr(notes, 'Developer: ') + 11), char(10)) > 0 
                        THEN instr(substr(notes, instr(notes, 'Developer: ') + 11), char(10)) - 1
                        ELSE length(notes)
                    END
                ))
                WHERE creator IS NULL AND notes LIKE '%Developer: %'
            """.trimIndent())
        } catch (_: Throwable) {
            // Best effort backfill; ignore syntax nuances across SQLite versions
        }
    }
}
```
Register in `.addMigrations(... MIGRATION_19_20, MIGRATION_20_21)`.

---

## 4. Networking, API Clients & Sync Architecture

Mirror the existing `GameApiConfig.kt` / `GameMetadataManager.kt` / `GameSyncManager.kt` pattern using the project's existing **OkHttp + Gson** stack (no new Gradle dependencies).

### 4.1 New Files in `data/remote/`

1. **`MusicApiConfig.kt`**
   - Stores optional `SPOTIFY_CLIENT_ID` and `SPOTIFY_CLIENT_SECRET` from `BuildConfig` with runtime overrides in `SharedPreferences` (matching `GameApiConfig.kt`).
   - MusicBrainz and Deezer need **no stored keys** — only the `User-Agent` header (which uses `BuildConfig.VERSION_NAME` + a contact email).
   - Caches Spotify OAuth2 `access_token` + `expires_at` epoch ms (identical pattern to Twitch token in `GameApiConfig.kt`).
   - Helper: `isMusicBrainzAvailable() = true` (always available), `isDeezerAvailable() = true` (always available), `isSpotifyConfigured(context)`.

2. **`MusicModels.kt`**
   - `MusicSearchResult(externalId, externalSource, title, artist, albumName, albumId, albumCoverUrl, durationSeconds, releaseYear, releaseDateMillis, genres, score, trackNumber, totalTracks, webUrl)`
   - `MusicAlbumDetails(externalId, externalSource, title, artist, coverUrl, releaseYear, releaseDateMillis, totalTracks, tracks: List<MusicSearchResult>, genres)`
   - `MusicArtistInfo(externalId, externalSource, name, genres, topTracks: List<MusicSearchResult>)`

3. **`MusicBrainzApiClient.kt`**
   - **No auth token.** All requests include `User-Agent: LXTrackList/{version} (lx-tracklist@example.com)` and `Accept: application/json`.
   - `searchRecordings(query, artist, limit = 20)`: `GET https://musicbrainz.org/ws/2/recording/?query="${query}" AND artist:"${artist}"&fmt=json&limit=20`
   - `searchReleaseGroups(query, limit = 20)`: `GET https://musicbrainz.org/ws/2/release-group/?query="${query}"&fmt=json&limit=20` — for album search
   - `getReleaseGroupDetails(mbid)`: `GET https://musicbrainz.org/ws/2/release-group/{mbid}?inc=releases+artists&fmt=json`
   - `getReleaseDetails(mbid)`: `GET https://musicbrainz.org/ws/2/release/{mbid}?inc=recordings+artist-credits&fmt=json` — for track listing
   - `getArtistReleases(artistMbid)`: `GET https://musicbrainz.org/ws/2/release-group/?artist={mbid}&type=album|single&fmt=json`
   - `getCoverArtUrl(releaseMbid)`: Construct `https://coverartarchive.org/release/{mbid}/front-500` (direct URL, no API call needed — Coil handles redirect/404 gracefully).
   - `getCoverArtUrlFallback(releaseGroupMbid)`: `https://coverartarchive.org/release-group/{mbid}/front`
   - **Rate-Limit Guard:** Enforce `>= 1100ms` spacing between calls (safely under 1 req/sec). 1 retry on HTTP 503.

4. **`DeezerApiClient.kt`**
   - **No auth required.** Plain GET requests.
   - `searchTracks(query, limit = 20)`: `GET https://api.deezer.com/search/track?q={query}&limit=20`
   - `getAlbumDetails(albumId)`: `GET https://api.deezer.com/album/{id}` — returns `cover_xl` (1000×1000) and track list
   - `getArtistTopTracks(artistId)`: `GET https://api.deezer.com/artist/{id}/top?limit=20`
   - `getAlbumTracks(albumId)`: `GET https://api.deezer.com/album/{id}/tracks`
   - **Rate-Limit Guard:** 200ms minimum spacing (~5 req/s, well under Deezer's limits).

5. **`SpotifyApiClient.kt`** (Optional)
   - `ensureValidToken(context)`: `POST https://accounts.spotify.com/api/token` with `client_credentials` grant type (identical pattern to `IgdbApiClient.ensureValidToken()`).
   - `searchTracks(context, query, limit = 20)`: `GET https://api.spotify.com/v1/search?q={query}&type=track&limit=20`
   - `getAlbumDetails(context, albumId)`: `GET https://api.spotify.com/v1/albums/{id}`
   - `getArtistTopTracks(context, artistId)`: `GET https://api.spotify.com/v1/artists/{id}/top-tracks`
   - **Rate-Limit Guard:** Respect `Retry-After` header on HTTP 429. Minimum 100ms spacing.

6. **`MusicMetadataManager.kt`**
   - Orchestrates MusicBrainz primary → Deezer fallback → Spotify optional.
   - `searchMusic(context, query, source?, limit)` — Routes to configured providers.
   - `getMusicDetails(context, externalId, externalSource)` — Fetches full details from the originating source.
   - `getAlbumTracks(context, albumId, externalSource)` — Fetches tracklist for related entries.
   - `searchAlbumCoverArt(context, title, artist)` — Multi-tier cover art resolution (CAA → Deezer → iTunes → manual).

7. **`MusicSyncManager.kt`**
   - Mirrors `GameSyncManager.kt` for bulk background metadata sync of `MediaType.MUSIC` entries.
   - Register in `SyncForegroundService.kt` (`isAnySyncRunning()`, `cancelAllSyncs()`, notification observer).
   - Throttles MusicBrainz calls at 1100ms intervals; Deezer at 200ms.

8. **`app/build.gradle.kts`**
   - Read `SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET` from `.env` / `System.getenv(...)` and expose via `buildConfigField(...)`.
   - **MusicBrainz and Deezer require zero build config changes.**

---

## 5. UI & Exhaustive Integration Checklist

Every file below has existing `MediaType` branches or category lists that must be updated so `MediaType.MUSIC` and the universal `creator` field work across the entire app:

| Layer / File | Required Changes |
|---|---|
| **`data/model/MediaType.kt`** | Add `MUSIC("Music", false, false, false, true)` with new `isListenType` flag; update `fromString()` to accept `"MUSIC"`, `"SONG"`, `"SONGS"`, `"TRACK"`, `"TRACKS"`. |
| **`data/model/MediaStatus.kt`** | Add `isListenType` branch to `getDisplayLabel()`: `"Listening"`, `"Plan to Listen"`. Update `fromString()` to match `"LISTENING"` and `"PLAN_TO_LISTEN"`. |
| **`data/model/MediaEntry.kt`** | 1. Add `creator: String? = null` and `musicAlbumId: String? = null`.<br>2. Add `getCreatorRoleLabel()` helper + semantic getters (`artist`, `author`, `studio`, `developer`).<br>3. Add `MUSIC` branches to `calculateSpentTimeMinutes()`, `calculateRemainingTimeMinutes()`, `displayProgress`, `progressFraction`, `incrementProgress()`, `decrementProgress()`.<br>4. Add `formatMusicDuration()` helper (seconds → `m:ss`). |
| **`data/local/LXTrackListDatabase.kt`** | Bump to v21, add `MIGRATION_20_21` with `ALTER TABLE media_entries ADD COLUMN creator TEXT DEFAULT NULL` and `musicAlbumId TEXT DEFAULT NULL`, plus index and data backfill. |
| **`data/local/Converters.kt`** | Verify `MediaType.MUSIC` serializes cleanly as `"MUSIC"` string. |
| **`ui/components/CategorySelectorDrawer.kt`** | Add `MediaType.MUSIC -> Icons.Default.MusicNote` in `getCategoryIcon()`; ensure Music appears after Games in built-in category list. |
| **`ui/components/MediaCard.kt`** | 1. Display `entry.creator` as the primary subtitle under `entry.title` across **all categories** (Music Artist, Book Author, Game Developer, Anime Studio).<br>2. Handle `MediaType.MUSIC`: album badge, duration as `m:ss`, `displayProgress`, wire toggle `+` button (Completed ↔ Listening). |
| **`ui/detail/DetailScreen.kt` & `DetailViewModel.kt`** | 1. Show **Creator** row/chip (`"${entry.getCreatorRoleLabel()}: ${entry.creator}"`) below title for all categories.<br>2. Show **Album** info chip with track number for Music.<br>3. Show **"Release Date"** label for `airedStartDate`.<br>4. Show **Duration** as `m:ss` for Music.<br>5. Show **Genre** tag chips.<br>6. In `DetailDescriptionCard`: display pure synopsis without the appended metadata lines.<br>7. Show **Related Tracks** section (same album, same artist). |
| **`ui/edit/AddEditScreen.kt` & `AddEditViewModel.kt`** | 1. Add dedicated **"Creator"** text input field with dynamic label (`"Artist"`, `"Author"`, `"Studio"`, `"Developer"`, `"Director"`).<br>2. Stop appending creator/publisher text into `description` — write directly to `creator`.<br>3. Add `ExternalDbSource.MUSICBRAINZ`, `ExternalDbSource.DEEZER`, optional `ExternalDbSource.SPOTIFY` when `mediaType == MUSIC`.<br>4. Add **Album** text field + **Track Number** field when `mediaType == MUSIC`.<br>5. Add **Duration** field (input as `m:ss` or total seconds) when `mediaType == MUSIC`.<br>6. Add **Genre** tag picker. |
| **`ui/search/SearchViewModel.kt`** | 1. Add `SearchCategory.MUSIC` after `GAMES`.<br>2. Map `MusicSearchResult` → `SearchResultItem(creator = it.artist, ...)`.<br>3. When converting search items to `MediaEntry` across all categories, set `creator = item.creator ?: item.author ?: item.gameDevelopers.firstOrNull()` and keep `description = item.synopsis` clean (remove `buildGameDescriptionWithMetadata` / `buildDescriptionWithMetadata` concatenation). |
| **`ui/search/SearchScreen.kt` & `SearchDiscoverySheet.kt`** | Add Music tab chip. Add music discovery filters (Popular, By Year, Genre). |
| **`ui/search/DiscoveryModels.kt`** | Add `MusicSortOrder` enum (`POPULARITY`, `RATING`, `RELEASE_DATE`). Add `DiscoveryFilter.Music` data class with `mode`, `year`/`startYear`/`endYear`, `sortOrder`, `includedTags`, `excludedTags`. |
| **`ui/search/MusicTagRegistry.kt`** *(new)* | Create genre registry with presets: `Pop`, `Rock`, `Hip-Hop`, `R&B`, `Electronic`, `Jazz`, `Classical`, `Country`, `Metal`, `Indie`, `Alternative`, `K-Pop`, `J-Pop`, `Latin`, `Reggae`, `Soul`, `Funk`, `Blues`, `Folk`, `Punk`, `Dance`, `Ambient`, `Soundtrack`. |
| **`ui/library/LibraryScreen.kt` & `HomeScreen.kt`** | Verify `MediaType.entries` chips include Music, status filter labels show `"Listening"`, `"Plan to Listen"`, and Sync Library dialog text mentions Music (MusicBrainz/Deezer). |
| **`ui/stats/StatsScreen.kt` & `StatsViewModel.kt`** | Include `MediaType.MUSIC` in category breakdown, completion counts, artist breakdown stat card, and listening time stats. |
| **`ui/settings/SettingsScreen.kt`** | Add **Music Metadata** section: Spotify Client ID/Secret (optional), attribution notes for MusicBrainz and Deezer, **"Sync Music Metadata"** action button. |
| **`ui/settings/MusicSettingsCard.kt`** *(new)* | Create settings card mirroring `GameSettingsCard.kt`: Spotify credentials input (optional), MusicBrainz/Deezer status (always available), test connection buttons, sync controls. |
| **`util/ExternalLinkHelper.kt`** | Add external URL builders for `"MusicBrainz"` (`https://musicbrainz.org/recording/{mbid}`), `"Deezer"` (`https://www.deezer.com/track/{id}`), and `"Spotify"` (`https://open.spotify.com/track/{id}`). |
| **`data/repository/MediaRepository.kt`** | Update `addRelatedToLibrary()` to handle `MediaType.MUSIC`, ensure `areCompatibleTypes()` keeps `MUSIC` isolated from watch/read/play types. |
| **`data/exporter/JsonExporter.kt` & `data/importer/JsonImporter.kt`** | Verify `creator` and `musicAlbumId` serialize/deserialize cleanly with default values so older JSON backups remain 100% backward compatible. |
| **`data/importer/CsvImporter.kt`** | Support parsing `"Music"` / `"Song"` as `MediaType.MUSIC`, map `"Listening"` → `WATCHING` status, parse `"Artist"` / `"Creator"` column into `creator`. |

---

## 6. Universal `creator` Field Extraction Hierarchy Across Categories

| Category | Primary API Source | Fallback API Source | Target Field | Previous Handling | New Handling |
|---|---|---|---|---|---|
| **Music** | MusicBrainz `recording.artist-credit[0].name` | Deezer `track.artist.name` / Spotify `artists[0].name` | `MediaEntry.creator` | N/A (New category) | Populates `creator` as Artist |
| **Anime** | AniList `studios.nodes[0].name` | Jikan / MAL `data.studios[0].name` | `MediaEntry.creator` | Appended to description or omitted | Populates `creator` as Studio |
| **Manga / Manhwa** | MangaDex `relationships[author].attributes.name` | AniList `staff.edges[role=Story].node.name` | `MediaEntry.creator` | Appended to description or omitted | Populates `creator` as Author/Artist |
| **Books / Light Novels** | Google Books `volumeInfo.authors[0]` | Open Library `docs[].author_name[0]` | `MediaEntry.creator` | Appended `Author: ...` to `description` | Populates `creator` as Author; `description` is pure synopsis |
| **Games** | IGDB `involved_companies[developer].company.name` | RAWG `developers[0].name` | `MediaEntry.creator` | Appended `Developer: ...` to `description` | Populates `creator` as Developer; `description` is pure summary |
| **Movies** | TMDB `credits.crew[Director].name` | TMDB `production_companies[0].name` | `MediaEntry.creator` | Omitted from dedicated field | Populates `creator` as Director |
| **Shows** | TMDB `created_by[0].name` | TMDB `networks[0].name` | `MediaEntry.creator` | Omitted from dedicated field | Populates `creator` as Creator / Network |

---

## 7. Phased Implementation Roadmap for Antigravity (Gemini 3.8 Flash)

> **Working with Antigravity (Gemini 3.8 Flash):** Execute **one phase per prompt** to keep context focused and prevent half-finished refactors. After each phase, run `./gradlew testDebugUnitTest` (and `./gradlew assembleDebug` from Phase 2 onward) before moving to the next phase.

### Phase 1 — Core Data Models, Universal `creator` Field & Room Migration (`v20 → v21`)
- **Files:** `MediaType.kt`, `MediaStatus.kt`, `MediaEntry.kt`, `LXTrackListDatabase.kt`, `MediaRepository.kt`.
- **Goal:**
  1. Add `MediaType.MUSIC` with `isListenType`.
  2. Add `val creator: String? = null` and `val musicAlbumId: String? = null` to `MediaEntry`.
  3. Add `getCreatorRoleLabel()` and semantic getters (`artist`, `author`, `studio`, `developer`) to `MediaEntry`.
  4. Implement `MUSIC` branches for `displayProgress`, `calculateSpentTimeMinutes()`, `calculateRemainingTimeMinutes()`, and toggle `incrementProgress()`.
  5. Implement `MIGRATION_20_21` in `LXTrackListDatabase.kt` with column creation, index, and legacy description data backfill.
  6. Fix all exhaustive `when (mediaType)` compiler errors with safe placeholders.
- **Verification:** Add `MusicAndCreatorEntryTest.kt` unit tests covering:
  - `getCreatorRoleLabel()` returns correct string for all 8 `MediaType` variants.
  - Toggle increment/decrement for Music (Completed ↔ Listening).
  - `displayProgress` with duration + album info.
  - `calculateSpentTimeMinutes()` with replay count.
  - `formatMusicDuration()` edge cases (0, 59, 180, 3661).
  - Run `./gradlew testDebugUnitTest`.

### Phase 2 — MusicBrainz Client, Deezer Fallback, Cover Art Pipeline & Search Model Updates
- **Files:** `MusicApiConfig.kt`, `MusicModels.kt`, `MusicBrainzApiClient.kt`, `DeezerApiClient.kt`, `MusicMetadataManager.kt`, `ExternalLinkHelper.kt`, `SearchResultItem.kt`, `BookDetails.kt`, `GameModels.kt`.
- **Goal:**
  1. Implement MusicBrainz search/detail queries (with 1-req/s throttle), Deezer zero-auth fallback, Cover Art Archive URL construction, and iTunes cover art fallback URL generation.
  2. Add `val creator: String? = null` to `SearchResultItem`.
  3. Update `BookDetails.toMediaEntry()` and `GameModels` to populate `creator` directly and stop appending metadata lines into `description`.
- **Verification:** Unit tests mocking JSON responses for MusicBrainz recording search → album cover resolution and Deezer track search → cover_xl extraction. Run `./gradlew testDebugUnitTest`.

### Phase 3 — Add/Edit Screen: Dedicated Creator Input Field & Music Form
- **Files:** `AddEditViewModel.kt`, `AddEditScreen.kt`, `CategorySelectorDrawer.kt`, `MusicTagRegistry.kt` (new).
- **Goal:**
  1. Add a dedicated "Creator" input field to `AddEditScreen` that dynamically renders the correct label based on `mediaType` ("Artist" for Music, "Author" for Books, "Studio" for Anime, "Developer" for Games, "Director" for Movies).
  2. Add Album, Track Number, and Duration inputs when `mediaType == MediaType.MUSIC`.
  3. Support MusicBrainz/Deezer search autofill with clean `creator` and pure `description`.
  4. Add Genre tag picker with music presets (`MusicTagRegistry`).
- **Verification:** `./gradlew assembleDebug`; test adding a song, a book, and a game, confirming the creator field works cleanly and descriptions stay uncluttered.

### Phase 4 — Entity Detail Screen & MediaCard: Displaying Creators Universally
- **Files:** `DetailViewModel.kt`, `DetailScreen.kt`, `MediaCard.kt`.
- **Goal:**
  1. On `MediaCard.kt`: render `entry.creator` as the prominent subtitle under the title for all categories when present.
  2. On `DetailScreen.kt`:
     - Render a dedicated Creator chip/row (e.g. `"Artist: Daft Punk"`, `"Author: Brandon Sanderson"`, `"Developer: FromSoftware"`, `"Studio: Ufotable"`).
     - Clean `DetailDescriptionCard`: display only the clean synopsis without redundant metadata text.
     - For Music: show Album info chip, duration as `m:ss`, and Related Tracks section (same album, same artist) with one-tap Add to Library.
- **Verification:** `./gradlew assembleDebug`; open existing library entries and new music entries; verify creator displays prominently across cards and detail screens.

### Phase 5 — Search & Discovery Tab Integration
- **Files:** `SearchViewModel.kt`, `SearchScreen.kt`, `DiscoveryModels.kt`, `SearchDiscoverySheet.kt`.
- **Goal:**
  1. Add `SearchCategory.MUSIC` after `GAMES`.
  2. Wire live search and discovery filters (Popular, By Year, Genre) to `MusicMetadataManager`.
  3. Ensure all Search categories map `creator` directly to `MediaEntry.creator` when user taps "Add to Library".
- **Verification:** `./gradlew assembleDebug`; search for a song, book, or game from Search tab, verify creator is saved in the dedicated column.

### Phase 6 — Settings, Spotify OAuth (Optional), Background Sync & Stats
- **Files:** `SettingsScreen.kt`, `MusicSettingsCard.kt` (new), `SpotifyApiClient.kt` (optional), `MusicSyncManager.kt`, `SyncForegroundService.kt`, `StatsScreen.kt`, `StatsViewModel.kt`, `LibraryScreen.kt`.
- **Goal:**
  1. Add Music Metadata section in Settings (MusicBrainz always-on status, optional Spotify Client ID/Secret with connection test).
  2. Optionally implement `SpotifyApiClient.kt` with Client Credentials OAuth (identical pattern to IGDB Twitch OAuth).
  3. Register `MusicSyncManager` in `SyncForegroundService` and Library Sync dialog.
  4. Update `StatsScreen` to include Music (listening time, top artists) and Creator breakdown.
  5. Verify JSON export/import and CSV import preserve `creator` and `musicAlbumId`.
- **Verification:** `./gradlew testDebugUnitTest assembleDebug`; verify JSON export → import round-trip preserves `creator`, `musicAlbumId`, and all reused fields.

---

## 8. Key Design Decisions & Rationale

### Why a universal `creator` column instead of `musicArtist`?
1. **Clean relational design:** Every media item has a primary creator (music artist, book author, game developer, anime studio, film director). Putting them all in a shared `creator` column avoids adding 6 separate sparse columns to SQLite (`artist`, `studio`, `author`, `developer`, `director`, `network`).
2. **Eliminates description pollution:** Appending "Author: ...", "Publisher: ...", "Developer: ..." into `description` was a workaround that cluttered the synopsis, broke text wrapping, and prevented clean UI layout. A dedicated column fixes this across the entire application.
3. **Consistent UI:** Cards and detail views can now consistently show the creator under the title for every media category in the app without special-case string parsing.

### Why track Songs (not Albums)?
Songs are the atomic unit of music consumption. Users listen to individual songs, skip some, replay others. Tracking at the song level gives maximum flexibility:
- A song belongs to an album (via `movieSeriesName` + `movieSeriesNumber`)
- Related tracks from the same album are shown as `relatedEntriesJson`
- Users can track a full album by adding its tracks (one-tap from album detail)

### Why reuse `movieSeriesName` / `movieSeriesNumber`?
These fields already exist in the database, are nullable, and are only used by `MediaType.MOVIE`. For Music, "album" is conceptually identical to "movie franchise" (a named collection with ordered entries). This keeps the database schema lean.

### Why `durationMinutes` stores seconds for Music?
Songs are typically 2-5 minutes. Storing in minutes loses precision. The existing `durationMinutes` field is `Int?` and nullable — for Music we store seconds (max ~3600 for a 1-hour track). The display logic already has per-`mediaType` branches, so formatting `m:ss` for Music is trivial. Movies continue storing minutes.

### Why MusicBrainz as primary over Spotify/Deezer?
- **Zero auth friction:** No API key, no OAuth, no account needed. Just a `User-Agent` header.
- **Open data:** Community-curated, Wikipedia-equivalent for music. No terms-of-service restrictions on caching.
- **Structured relationships:** Artist → Release Groups → Releases → Recordings hierarchy maps perfectly to our Album → Track model.
- **Cover Art Archive integration:** Direct MBID-based cover art URLs.
- **Drawback:** 1 req/sec rate limit — mitigated by using Deezer (10 req/s, also no key) as fallback.

---

## 9. Prompting Tips for Antigravity (Gemini 3.8 Flash)

1. **Feed One Phase at a Time:** Reference this plan file and say: *"Implement Phase 1 of `music_tracking_feature_plan.md`. Do not modify UI screens yet beyond fixing exhaustive `when (mediaType)` compile errors. Run `./gradlew testDebugUnitTest` when done."*
2. **Protect Existing Data:** Remind Antigravity to keep `MIGRATION_20_21` using `ALTER TABLE ... ADD COLUMN ... DEFAULT ...` so existing user data is preserved.
3. **Stop Description Pollution:** Ensure Antigravity does not re-add `Author: ` or `Developer: ` lines to `description`. Creator belongs strictly in the `creator` column.
4. **Keep API Secrets Out of Git:** Add `SPOTIFY_CLIENT_ID` and `SPOTIFY_CLIENT_SECRET` to your local `.env` (already gitignored) and to GitHub Actions repository secrets. MusicBrainz and Deezer need no secrets.
5. **Prevent Play Protect Regressions:** Do not add any new `AndroidManifest.xml` permissions — Music tracking uses only the existing `INTERNET` permission.
6. **Respect MusicBrainz Rate Limits:** Always enforce `>= 1100ms` delay between MusicBrainz API calls. The 503 rate-limit response has no retry-after header — back off exponentially.
7. **Test Duration Formatting:** Music's `durationMinutes` stores seconds. Ensure all display paths format correctly as `m:ss` for `MUSIC` and `Xh Ym` for other types.
