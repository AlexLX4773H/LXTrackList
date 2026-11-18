# LX Track List — Entity, Property & Field-Source Reference Guide

> **Context:** This reference document provides an exhaustive inventory of how each entity is stored in **LX Track List**, every property it contains, and the exact source hierarchy for each data field across all media categories, detailing both the **Current Implementation** and the **New Optimized Plan**.

---

## 1. Entity Storage Architecture

LX Track List uses **Room (SQLite)** for local persistence, configured in `LXTrackListDatabase.kt`. 

### 1.1 Database Tables & Schemas

| Entity Class | Table Name | Primary Key | Key Indices | Description |
|---|---|---|---|---|
| `MediaEntry` | `media_entries` | `id: Long` (Auto-generate) | `mediaType`, `status`, `isFavorite`, `isExternallyTracked`, `title`, `userId`, `customCategory`, `subCategory`, `[externalId, externalSource]` | The central entity representing any trackable media item. |
| `CustomCategory` | `custom_categories` | `id: Long` (Auto-generate) | `[userId, name]` (Unique) | Top-level custom categories created by the user (e.g. "Donghua", "Web Novels"). |
| `SubCategory` | `sub_categories` | `id: Long` (Auto-generate) | `[userId, categoryKey, name]` (Unique) | Hierarchical sub-genres/tags linked to a category (e.g. "Isekai", "Murim"). |
| `UserProfile` | `user_profiles` | `id: Long` (Auto-generate) | None | Multi-user profiles with personal avatar, pin/passkey, and startup defaults. |

### 1.2 Type Converters (`Converters.kt`)

Room serializes enums and complex structures using `Converters.kt`:
- `MediaType` ↔ String (`"ANIME"`, `"MANGA"`, `"MANHWA"`, `"MOVIE"`, `"SHOW"`, `"BOOK"`)
- `MediaStatus` ↔ String (`"WATCHING"`, `"PLAN_TO_WATCH"`, `"COMPLETED"`, `"ON_HOLD"`, `"DROPPED"`)
- `Priority` ↔ String (`"LOW"`, `"MEDIUM"`, `"HIGH"`)
- `TrackingUnit` ↔ String (`"CHAPTERS"`, `"VOLUMES"`, `"PAGES"`, `"COMBINED"`)
- Complex JSON structures (`relatedEntriesJson`, `seasonEpisodesJson`) are serialized as standard JSON strings using Google `Gson`.

---

## 2. Complete Entity Property Dictionary

### 2.1 `MediaEntry` Properties (Full 52-Field Breakdown)

The table below lists every field of the core `MediaEntry` Room entity, its column name, type, default, and purpose:

| # | Kotlin Property | DB Column | Data Type | Default | Category Scope | Description |
|---|---|---|---|---|---|---|
| 1 | `id` | `id` | `Long` | `0L` | All | Room Auto-generated primary key |
| 2 | `userId` | `userId` | `Long` | `0L` | All | Foreign key linking entry to a `UserProfile` |
| 3 | `customCategory` | `customCategory` | `String?` | `null` | All | Optional custom category override (e.g. "Donghua") |
| 4 | `subCategory` | `subCategory` | `String?` | `null` | All | Sub-category tag (e.g. "Isekai", "Murim") |
| 5 | `title` | `title` | `String` | *(required)* | All | Primary display title |
| 6 | `alternateTitle` | `alternateTitle` | `String?` | `null` | All | Newline-separated alternate titles (Romaji, English, Native, Synonyms) |
| 7 | `mediaType` | `mediaType` | `MediaType` | *(required)* | All | Core media type enum (`ANIME`, `MANGA`, `MANHWA`, `MOVIE`, `SHOW`, `BOOK`) |
| 8 | `status` | `status` | `MediaStatus` | *(required)* | All | User consumption status (`WATCHING`, `PLAN_TO_WATCH`, `COMPLETED`, etc.) |
| 9 | `score` | `score` | `Int?` | `null` | All | User's personal rating (1 to 10) |
| 10 | `externalScore` | `externalScore` | `Float?` | `null` | All | Public rating from external database (e.g. 8.42 on MAL, 7.8 on TMDB) |
| 11 | `externalScoreSource` | `externalScoreSource` | `String?` | `null` | All | Provider of the external score (e.g. "MAL", "AniList", "TMDB", "Google Books") |
| 12 | `startDate` | `startDate` | `Long?` | `null` | All | User's personal consumption start date (epoch ms) |
| 13 | `finishDate` | `finishDate` | `Long?` | `null` | All | User's personal completion date (epoch ms) |
| 14 | `airedStartDate` | `airedStartDate` | `Long?` | `null` | All | Official release/broadcast premiere date (epoch ms) |
| 15 | `airedEndDate` | `airedEndDate` | `Long?` | `null` | All | Official finale/end publication date (epoch ms) |
| 16 | `description` | `notes` | `String?` | `null` | All | Synopsis / plot summary. (Serialized as `description`, legacy column `notes`) |
| 17 | `tags` | `tags` | `String?` | `null` | All | Comma-separated list of genres, demographics, and content tags |
| 18 | `coverImageUri` | `coverImageUri` | `String?` | `null` | All | Web URL or local file URI for the primary poster / book cover |
| 19 | `priority` | `priority` | `Priority` | `Priority.MEDIUM` | All | User priority flag (`LOW`, `MEDIUM`, `HIGH`) |
| 20 | `rewatchCount` | `rewatchCount` | `Int` | `0` | All | Number of times rewatched or reread |
| 21 | `isRewatching` | `isRewatching` | `Boolean` | `false` | All | Whether user is currently rewatching / rereading |
| 22 | `isFavorite` | `isFavorite` | `Boolean` | `false` | All | Favorite bookmark toggle |
| 23 | `isExternallyTracked`| `isExternallyTracked`| `Boolean` | `false` | All | Whether changes auto-push to MyAnimeList |
| 24 | `externalId` | `externalId` | `String?` | `null` | All | Primary ID in external DB (MAL ID, TMDB ID, MangaDex UUID, Google Books ID, etc.) |
| 25 | `externalSource` | `externalSource` | `String?` | `null` | All | Provider name ("MAL", "AniList", "TMDB", "MangaDex", "Google Books", etc.) |
| 26 | `createdAt` | `createdAt` | `Long` | `now` | All | Local creation timestamp in epoch ms |
| 27 | `updatedAt` | `updatedAt` | `Long` | `now` | All | Local modification timestamp in epoch ms |
| 28 | `currentEpisodes` | `currentEpisodes` | `Int` | `0` | Video | Episodes watched by user |
| 29 | `totalEpisodes` | `totalEpisodes` | `Int` | `0` | Video | Total official episode count |
| 30 | `airedEpisodes` | `airedEpisodes` | `Int` | `0` | Video | Currently released episodes for ongoing shows/anime |
| 31 | `seasonNumber` | `seasonNumber` | `Int` | `1` | Shows | Legacy season pointer |
| 32 | `currentSeasons` | `currentSeasons` | `Int` | `1` | Shows | Current season user is watching |
| 33 | `totalSeasons` | `totalSeasons` | `Int` | `1` | Shows | Total available seasons |
| 34 | `seasonEpisodesJson` | `seasonEpisodesJson`| `String?` | `null` | Shows | JSON array of episode counts per season (e.g. `"[12, 24, 12]"`) |
| 35 | `episodeLengthMinutes`| `episodeLengthMinutes`| `Int?` | `null` | Video | Average runtime per episode in minutes |
| 36 | `currentTimeMinutes` | `currentTimeMinutes` | `Int?` | `null` | Video | Intra-episode playback checkpoint (minutes elapsed) |
| 37 | `isAiring` | `isAiring` | `Boolean` | `false` | Video/Reading | Whether media is currently releasing / broadcasting / serializing |
| 38 | `broadcastTime` | `broadcastTime` | `String?` | `null` | Anime | Scheduled airing day/time (e.g. "Sundays at 00:00 (JST)") |
| 39 | `nextAiringEpoch` | `nextAiringEpoch` | `Long?` | `null` | Anime | Exact epoch ms timestamp when next episode will broadcast |
| 40 | `currentVolumes` | `currentVolumes` | `Int` | `0` | Reading | Volumes read by user |
| 41 | `totalVolumes` | `totalVolumes` | `Int` | `0` | Reading | Total published volume count |
| 42 | `currentChapters` | `currentChapters` | `Int` | `0` | Reading | Chapters read by user |
| 43 | `totalChapters` | `totalChapters` | `Int` | `0` | Reading | Total released chapter count |
| 44 | `airedChapters` | `airedChapters` | `Int` | `0` | Reading | Current published chapters for ongoing manga/manhwa |
| 45 | `currentPages` | `currentPages` | `Int` | `0` | Reading | Current page number read by user |
| 46 | `totalPages` | `totalPages` | `Int` | `0` | Reading | Total page count of book |
| 47 | `trackingUnit` | `trackingUnit` | `TrackingUnit` | `CHAPTERS` | Reading | Active progress unit (`CHAPTERS`, `VOLUMES`, `PAGES`, `COMBINED`) |
| 48 | `movieSeriesNumber` | `movieSeriesNumber` | `Int?` | `null` | Movies | Sequence order in a film series (e.g. 2 for "The Two Towers") |
| 49 | `movieSeriesName` | `movieSeriesName` | `String?` | `null` | Movies | Collection franchise name (e.g. "The Lord of the Rings Collection") |
| 50 | `durationMinutes` | `durationMinutes` | `Int?` | `null` | Movies | Total movie runtime in minutes |
| 51 | `relatedEntriesJson`| `relatedEntriesJson`| `String?` | `null` | All | JSON array of serialized `RelatedMediaItem` records |
| 52 | `userComments` | `userComments` | `String?` | `null` | All | User personal thoughts/notes kept separate from official synopsis |

---

### 2.2 Supporting Models & Data Transfer Objects (DTOs)

#### `RelatedMediaItem`
Stored as a serialized JSON array inside `MediaEntry.relatedEntriesJson`:
- `title: String` — Title of the related work
- `relationship: String` — Relationship type: `"Sequel"`, `"Prequel"`, `"Part of Series"`, `"Side Story"`, `"Alternative Version"`, `"Related"`
- `externalId: String` — External ID on source platform
- `externalSource: String` — `"MAL"`, `"AniList"`, or `"TMDB"`
- `mediaType: MediaType` — Target media type
- `posterUrl: String?` — Thumbnail / poster image URL
- `releaseYear: String?` — 4-digit release year (e.g. `"2024"`)
- `score: Float?` — External score (0.0 to 10.0)
- `inLibraryEntryId: Long?` — Correlated local entry ID (if already in user library)
- `inLibraryStatus: MediaStatus?` — Correlated local library status

#### `CustomCategory`
- `id: Long` (PK)
- `userId: Long` — User profile association
- `name: String` — Custom category name (e.g. "Donghua", "Web Novels")
- `mediaType: MediaType?` — Base tracking behavior
- `isWatchType: Boolean` — True for video tracking, false for reading
- `trackingUnit: TrackingUnit` — Default tracking unit
- `colorHex: String` — UI theme accent hex code
- `createdAt: Long` — Creation epoch ms

#### `SubCategory`
- `id: Long` (PK)
- `userId: Long` — User profile association
- `categoryKey: String` — Parent category key (e.g. `"MANGA"`, `"MANHWA"`, `"ANIME"`, `"Donghua"`)
- `name: String` — Sub-category name (e.g. "Isekai", "Murim", "Cyberpunk")
- `colorHex: String` — UI tag color hex code
- `createdAt: Long` — Creation epoch ms

#### `UserProfile`
- `id: Long` (PK)
- `name: String` — Profile name (e.g. "Alex")
- `avatarColorHex: String` — Profile badge color
- `avatarImageUri: String?` — Custom avatar image URI
- `isDefault: Boolean` — Default startup profile
- `defaultMediaType: MediaType?` — Default landing tab
- `defaultCustomCategory: String?` — Default custom category tab
- `defaultSubCategory: String?` — Default sub-category filter
- `defaultStatus: MediaStatus?` — Default status filter tab
- `passKey: String?` — Optional PIN / lock passkey

#### `BookDetails`
DTO used by `GoogleBooksApiClient` and `OpenLibraryApiClient`:
- `id: String`, `title: String`, `subtitle: String?`, `authors: List<String>`, `publisher: String?`, `publishedDate: String?`, `description: String?`, `pageCount: Int?`, `totalChapters: Int?`, `totalVolumes: Int?`, `isbn10: String?`, `isbn13: String?`, `coverUrl: String?`, `categories: List<String>`, `averageScore: Float?`, `sourceProvider: String`, `language: String?`, `infoUrl: String?`

---

## 3. External API Sources & Capabilities

| Source | Protocol | Auth Requirement | Rate Limit | Primary Data Provided |
|---|---|---|---|---|
| **AniList** | GraphQL | None | 90 req/min | Media details, Romaji/Native titles, airing schedule (`nextAiringEpisode`), staff, relations, genres, HD covers. |
| **Official MAL API v2** | REST | OAuth2 / Client-ID | Generous (user-token) | Official MyAnimeList user sync, anime/manga details, rankings, user list state. |
| **Jikan v4** | REST | None | 3 req/sec, 60 req/min | Unauthenticated MAL wrapper. Rich anime/manga details, broadcast schedules, character lists. |
| **TMDB v3** | REST | Bearer Token | 40 req/10 sec | Movies & TV shows details, season/episode counts, credits, recommendations, collection franchises. |
| **MangaDex v5** | REST | None | 5 req/sec | Manga, Manhwa, Manhua titles, latest chapters, volume counts, author/artist, original language, CDN covers. |
| **Kitsu** | JSON:API | None | Fair use (~30 req/min) | Anime & Manga details, canonical titles, poster images, episode/chapter counts. |
| **Google Books** | REST | API Key (Optional) | 1k/day (free), 25k/day (key) | Books, ISBN search, page counts, publishers, publication dates, descriptions. |
| **Open Library** | REST | None | 100 req/5 min | Open source book catalog, ISBN lookups, series ordering, author works. |

---

## 4. Field-by-Field Source Mapping Per Category

Below are the exact source mapping tables for every field of `MediaEntry` across all 7 categories. Each table shows the **Current Source & Hierarchy** alongside the **New Plan Source & Hierarchy**.

---

### 4.1 ANIME

| Field Name | Current Source & Hierarchy | New Plan Source & Hierarchy | Extraction & Transformation Logic |
|---|---|---|---|
| `title` | 1. Jikan `title`<br>2. AniList `title.romaji`<br>3. Kitsu `canonicalTitle` | **1. AniList `title.romaji` (or `english`)**<br>2. Official MAL v2 `node.title`<br>3. Jikan `data.title`<br>4. Kitsu `canonicalTitle` | Prefer English or Romaji title based on user settings. |
| `alternateTitle` | 1. Jikan `title_english` + `title_japanese` + `titles[]`<br>2. AniList `synonyms` + `native` | **1. AniList `title.english` + `native` + `synonyms[]`**<br>2. Jikan `titles[]`<br>3. MAL v2 `alternative_titles` | Formatted as newline-delimited unique list. |
| `coverImageUri` | 1. Jikan `images.webp.large_image_url`<br>2. AniList `coverImage.extraLarge`<br>3. Kitsu `posterImage.large` | **1. AniList `coverImage.extraLarge`**<br>2. Official MAL v2 `main_picture.large`<br>3. Jikan `images.webp.large_image_url`<br>4. Kitsu `posterImage.large` | AniList extraLarge provides 1000px+ WebP images. |
| `totalEpisodes` | 1. Jikan `episodes`<br>2. AniList `episodes`<br>3. Kitsu `episodeCount` | **1. AniList `episodes`**<br>2. Official MAL v2 `num_episodes`<br>3. Jikan `episodes`<br>4. Kitsu `episodeCount` | Null/0 if ongoing with unknown episode count. |
| `airedEpisodes` | 1. Calculated from `airedStartDate` + weeks<br>2. Jikan `episodes` (if completed) | **1. AniList `nextAiringEpisode.episode - 1`**<br>2. Calculated from `airedStartDate`<br>3. Total episodes (if finished) | AniList `nextAiringEpisode` gives exact current broadcast count! |
| `isAiring` | 1. Jikan `airing: Boolean`<br>2. AniList `status == RELEASING` | **1. AniList `status == "RELEASING"`**<br>2. Official MAL v2 `status == "currently_airing"`<br>3. Jikan `airing` | Exact boolean state. |
| `broadcastTime` | 1. Jikan `broadcast.string`<br>2. Null | **1. AniList calculated from `nextAiringEpisode.airingAt`**<br>2. Jikan `broadcast.string` | AniList epoch converts directly to localized time. |
| `nextAiringEpoch` | 1. Calculated from Jikan `broadcast.string` regex<br>2. Null | **1. AniList `nextAiringEpisode.airingAt * 1000`**<br>2. Calculated from Jikan `broadcast.string` | AniList provides native Unix epoch timestamp in seconds! |
| `episodeLengthMinutes` | 1. Jikan `duration` (parsed regex e.g. "24 min")<br>2. AniList `duration` | **1. AniList `duration` (native Int minutes)**<br>2. Jikan `duration` (regex parsed) | AniList returns integer directly (e.g. `24`). |
| `airedStartDate` | 1. Jikan `aired.from`<br>2. AniList `startDate`<br>3. Kitsu `startDate` | **1. AniList `startDate` (Y/M/D object)**<br>2. Official MAL v2 `start_date`<br>3. Jikan `aired.from`<br>4. Kitsu `startDate` | Converted to UTC epoch milliseconds. |
| `airedEndDate` | 1. Jikan `aired.to`<br>2. AniList `endDate`<br>3. Kitsu `endDate` | **1. AniList `endDate` (Y/M/D object)**<br>2. Official MAL v2 `end_date`<br>3. Jikan `aired.to`<br>4. Kitsu `endDate` | Converted to UTC epoch milliseconds. |
| `externalScore` | 1. Jikan `score`<br>2. AniList `averageScore / 10f`<br>3. Kitsu `averageRating / 10f` | **1. AniList `averageScore / 10f` (or MAL v2 `mean`)**<br>2. Official MAL v2 `mean`<br>3. Jikan `score`<br>4. Kitsu `averageRating / 10f` | Normalized to 0.0 – 10.0 scale. |
| `externalScoreSource` | 1. Jikan: `"MAL"`<br>2. AniList: `"AniList"` | **1. `"AniList"` (or `"MAL"` if user uses MAL sync)**<br>2. `"MAL"`<br>3. `"Kitsu"` | Stamped with source identity. |
| `externalId` | 1. Jikan `mal_id`<br>2. AniList `idMal` or `id` | **1. AniList `idMal` (MAL ID) or `id` (AniList ID)**<br>2. Official MAL v2 `node.id`<br>3. Jikan `mal_id` | Keep MAL ID if available to ensure sync compatibility. |
| `externalSource` | 1. `"MAL"` | **1. `"MAL"` (if `idMal` present) or `"AniList"`** | Authoritative source tag. |
| `tags` | 1. Jikan `genres[]` + `themes[]` + `demographics[]`<br>2. AniList `genres[]` | **1. AniList `genres[]` + top `tags[].name`**<br>2. Jikan `genres[]` + `themes[]`<br>3. MAL v2 `genres[]` | Deduplicated comma-separated string. |
| `description` | 1. Jikan `synopsis` (HTML cleaned)<br>2. AniList `description(asHtml: false)` | **1. AniList `description(asHtml: false)`**<br>2. Official MAL v2 `synopsis`<br>3. Jikan `synopsis` | Stripped of BBCode / HTML tags. |
| `relatedEntriesJson` | 1. Jikan `queryJikanRelations` (2nd HTTP call)<br>2. AniList `queryAniListRelations` | **1. AniList `relations.edges` (Embedded in 1st call!)**<br>2. Jikan `queryJikanRelations` | **Zero extra network calls** with AniList GraphQL! |

---

### 4.2 MANGA

| Field Name | Current Source & Hierarchy | New Plan Source & Hierarchy | Extraction & Transformation Logic |
|---|---|---|---|
| `title` | 1. Jikan `title`<br>2. MangaDex `attributes.title.en`<br>3. AniList `title.romaji`<br>4. Kitsu `canonicalTitle` | **1. AniList `title.romaji`** *(or MangaDex if UUID)*<br>2. MangaDex `title.en` (or `ja-ro`)<br>3. Jikan `data.title`<br>4. Kitsu `canonicalTitle` | Clean title without scanlation tags. |
| `alternateTitle` | 1. Jikan `titles[]`<br>2. MangaDex `altTitles[]`<br>3. AniList `synonyms` | **1. MangaDex `altTitles[]`** (Most comprehensive)<br>2. AniList `title.english` + `synonyms[]`<br>3. Jikan `titles[]` | Combines multi-language localized titles. |
| `coverImageUri` | 1. Jikan `images.webp.large_image_url`<br>2. MangaDex `covers CDN (512px)`<br>3. AniList `coverImage.extraLarge` | **1. MangaDex `uploads.mangadex.org (512px)`**<br>2. AniList `coverImage.extraLarge`<br>3. Jikan `images.webp.large_image_url` | High-res 512px cover thumbnail. |
| `totalChapters` | 1. Jikan `chapters`<br>2. MangaDex `lastChapter` (if completed)<br>3. AniList `chapters` | **1. AniList `chapters` (if completed)**<br>2. MangaDex `lastChapter`<br>3. Jikan `chapters` | Null if ongoing series. |
| `airedChapters` | 1. MangaDex `lastChapter` (if ongoing)<br>2. Null | **1. MangaDex `lastChapter` (if ongoing)**<br>2. AniList `chapters`<br>3. Null | Tracks latest published chapter for ongoing manga. |
| `totalVolumes` | 1. Jikan `volumes`<br>2. MangaDex `lastVolume`<br>3. AniList `volumes` | **1. AniList `volumes`**<br>2. MangaDex `lastVolume`<br>3. Jikan `volumes` | Standard tankobon volume count. |
| `isAiring` | 1. Jikan `publishing: Boolean`<br>2. MangaDex `status == "ongoing"`<br>3. AniList `status == "RELEASING"` | **1. MangaDex `status == "ongoing"`**<br>2. AniList `status == "RELEASING"`<br>3. Jikan `publishing` | Ongoing serialization flag. |
| `airedStartDate` | 1. Jikan `published.from`<br>2. AniList `startDate`<br>3. MangaDex `year` | **1. AniList `startDate`**<br>2. MangaDex parsed `createdAt` / `year`<br>3. Jikan `published.from` | UTC epoch ms. |
| `airedEndDate` | 1. Jikan `published.to`<br>2. AniList `endDate` | **1. AniList `endDate`**<br>2. Jikan `published.to` | UTC epoch ms (null if ongoing). |
| `externalScore` | 1. Jikan `score`<br>2. AniList `averageScore / 10f`<br>3. Kitsu `averageRating / 10f` | **1. AniList `averageScore / 10f`**<br>2. Jikan `score`<br>3. Kitsu `averageRating / 10f` | Normalized 0.0 – 10.0. |
| `externalScoreSource` | 1. `"MAL"` | **1. `"AniList"` (or `"MAL"` if Jikan used)** | Provider source. |
| `externalId` | 1. Jikan `mal_id`<br>2. MangaDex `id` | **1. AniList `idMal` or `id`**<br>2. MangaDex `id` (UUID)<br>3. Jikan `mal_id` | Stored as numeric ID or UUID. |
| `externalSource` | 1. `"MAL"` | **1. `"AniList"` / `"MangaDex"` / `"MAL"`** | Source tag. |
| `tags` | 1. Jikan `genres[]` + `demographics[]`<br>2. MangaDex `tags[].name`<br>3. AniList `genres[]` | **1. AniList `genres[]` + MangaDex `tags[]`**<br>2. Jikan `genres[]` | Filtered content & demographic tags. |
| `description` | 1. Jikan `synopsis`<br>2. MangaDex `description.en`<br>3. AniList `description` | **1. AniList `description(asHtml: false)`**<br>2. MangaDex `description.en`<br>3. Jikan `synopsis` | Markdown / cleaned text synopsis. |
| `relatedEntriesJson` | 1. Jikan `queryJikanRelations` (2nd call)<br>2. AniList relations | **1. AniList `relations.edges` (Single query)**<br>2. Jikan relations | Single round-trip franchise mapping. |

---

### 4.3 MANHWA (Korean Webtoons)

| Field Name | Current Source & Hierarchy | New Plan Source & Hierarchy | Extraction & Transformation Logic |
|---|---|---|---|
| `title` | 1. Jikan `title` *(fails >50%)*<br>2. MangaDex `title.en`<br>3. AniList `title.romaji` | **1. MangaDex `attributes.title.en` (or `ko`)**<br>2. AniList `title.romaji` / `english`<br>3. Kitsu `canonicalTitle`<br>4. Jikan *(last resort only)* | MangaDex has >94% Manhwa coverage. |
| `alternateTitle` | 1. Jikan `titles[]`<br>2. MangaDex `altTitles[]`<br>3. AniList `synonyms` | **1. MangaDex `altTitles[]`** (Includes Hangul & English aliases)<br>2. AniList `title.native` + `synonyms[]` | Hangul native title + English webtoon title. |
| `coverImageUri` | 1. Jikan `large_image_url`<br>2. MangaDex `covers CDN (512px)`<br>3. AniList `coverImage.extraLarge` | **1. MangaDex `uploads.mangadex.org (512px)`**<br>2. AniList `coverImage.extraLarge`<br>3. Kitsu `posterImage.large` | HD webtoon vertical cover. |
| `totalChapters` | 1. Jikan `chapters`<br>2. MangaDex `lastChapter` | **1. MangaDex `lastChapter` (if completed)**<br>2. AniList `chapters` | Finished webtoon total chapters. |
| `airedChapters` | 1. MangaDex `lastChapter`<br>2. Null | **1. MangaDex `lastChapter` (if ongoing)**<br>2. AniList `chapters` | Latest serialized webtoon chapter. |
| `totalVolumes` | 1. Jikan `volumes`<br>2. MangaDex `lastVolume` | **1. MangaDex `lastVolume`**<br>2. AniList `volumes` | Physical Korean manhwa print volume count. |
| `isAiring` | 1. Jikan `publishing`<br>2. MangaDex `status == "ongoing"` | **1. MangaDex `status == "ongoing"`**<br>2. AniList `status == "RELEASING"` | Ongoing webtoon release status. |
| `airedStartDate` | 1. Jikan `published.from`<br>2. AniList `startDate`<br>3. MangaDex `year` | **1. AniList `startDate`**<br>2. MangaDex `year` / `createdAt`<br>3. Kitsu `startDate` | Premiere date epoch ms. |
| `airedEndDate` | 1. Jikan `published.to`<br>2. AniList `endDate` | **1. AniList `endDate`**<br>2. MangaDex status completion date | Finale date epoch ms. |
| `externalScore` | 1. Jikan `score`<br>2. AniList `averageScore / 10f`<br>3. Kitsu `averageRating / 10f` | **1. AniList `averageScore / 10f`**<br>2. Kitsu `averageRating / 10f`<br>3. MangaDex statistics | Normalized 0.0 – 10.0. |
| `externalScoreSource` | 1. `"MAL"` | **1. `"AniList"` / `"Kitsu"` / `"MangaDex"`** | Source tag. |
| `externalId` | 1. Jikan `mal_id`<br>2. MangaDex `id` (UUID) | **1. MangaDex `id` (UUID)**<br>2. AniList `id`<br>3. Kitsu `id` | Standard UUID format for MangaDex. |
| `externalSource` | 1. `"MAL"` | **1. `"MangaDex"`** (or `"AniList"`) | Marked as `"MangaDex"`. |
| `tags` | 1. Jikan `genres[]`<br>2. MangaDex `tags[]` | **1. MangaDex `tags[]`** (Includes "Long Strip", "Web Comic", "Isekai")<br>2. AniList `genres[]` | Webtoon-specific tags preserved. |
| `description` | 1. Jikan `synopsis`<br>2. MangaDex `description.en` | **1. MangaDex `description.en`**<br>2. AniList `description(asHtml: false)` | Clean English plot synopsis. |
| `relatedEntriesJson` | 1. Jikan relations<br>2. AniList relations | **1. AniList `relations.edges`**<br>2. MangaDex related manga relationships | Prequels, sequels, spin-offs. |

---

### 4.4 MOVIES

| Field Name | Current Source & Hierarchy | New Plan Source & Hierarchy | Extraction & Transformation Logic |
|---|---|---|---|
| `title` | 1. TMDB `title` | **1. TMDB `title` (Single call)** | Primary movie title. |
| `alternateTitle` | 1. TMDB `alternative_titles.titles[].title` | **1. TMDB `alternative_titles.titles[].title`** | Newline-separated localized titles. |
| `coverImageUri` | 1. TMDB `poster_path` (`w500`) | **1. TMDB `poster_path` (`w500`)** | Full URL: `https://image.tmdb.org/t/p/w500$path`. |
| `durationMinutes` | 1. TMDB `runtime` | **1. TMDB `runtime` (Int)** | Movie runtime in minutes. |
| `airedStartDate` | 1. TMDB `release_date` | **1. TMDB `release_date`** | Parsed to UTC epoch ms. |
| `airedEndDate` | 1. TMDB `release_date` | **1. TMDB `release_date`** | Same as premiere date for movies. |
| `isAiring` | 1. `false` | **1. TMDB `status != "Released"`** | True if upcoming/in production. |
| `externalScore` | 1. TMDB `vote_average` | **1. TMDB `vote_average`** | TMDB score (0.0 – 10.0). |
| `externalScoreSource` | 1. `"TMDB"` | **1. `"TMDB"`** | Identifies source as TMDB. |
| `externalId` | 1. TMDB `id.toString()` | **1. TMDB `id.toString()`** | TMDB Movie ID. |
| `externalSource` | 1. `"TMDB"` | **1. `"TMDB"`** | Marked as TMDB. |
| `tags` | 1. TMDB `genres[].name` | **1. TMDB `genres[].name`** | Comma-separated genres. |
| `description` | 1. TMDB `overview` | **1. TMDB `overview`** | Clean movie plot synopsis. |
| `movieSeriesName` | 1. TMDB `belongs_to_collection.name` | **1. TMDB `belongs_to_collection.name`** | E.g. "The Dark Knight Trilogy". |
| `movieSeriesNumber` | 1. Parsed from collection parts / title | **1. TMDB collection sequence index** | 1, 2, 3... in chronological franchise order. |
| `relatedEntriesJson` | 1. TMDB `fetchRelatedEntries` (2nd call!) | **1. TMDB `recommendations.results` + `belongs_to_collection.parts` (Embedded in 1st call!)** | **Zero extra network calls!** Combined in 1 request. |

---

### 4.5 SHOWS (TV Shows)

| Field Name | Current Source & Hierarchy | New Plan Source & Hierarchy | Extraction & Transformation Logic |
|---|---|---|---|
| `title` | 1. TMDB `name` | **1. TMDB `name` (Single call)** | Primary show title. |
| `alternateTitle` | 1. TMDB `alternative_titles.results[].title` | **1. TMDB `alternative_titles.results[].title`** | Localized / original titles. |
| `coverImageUri` | 1. TMDB `poster_path` (`w500`) | **1. TMDB `poster_path` (`w500`)** | Full URL: `https://image.tmdb.org/t/p/w500$path`. |
| `totalSeasons` | 1. TMDB `number_of_seasons` | **1. TMDB `number_of_seasons`** | Official season count (excluding specials). |
| `totalEpisodes` | 1. TMDB `number_of_episodes` | **1. TMDB `number_of_episodes`** | Sum of all standard episodes. |
| `seasonEpisodesJson` | 1. TMDB `seasons[].episode_count` | **1. TMDB `seasons[].episode_count`** | JSON array of episode counts (e.g. `"[8, 8, 8]"`). |
| `episodeLengthMinutes` | 1. TMDB `episode_run_time[0]` | **1. TMDB `episode_run_time[0]`** | Average episode duration. |
| `isAiring` | 1. TMDB `status == "Returning Series"` | **1. TMDB `status in ("Returning Series", "In Production")`** | Ongoing television broadcast state. |
| `airedStartDate` | 1. TMDB `first_air_date` | **1. TMDB `first_air_date`** | Premiere date in UTC epoch ms. |
| `airedEndDate` | 1. TMDB `last_air_date` | **1. TMDB `last_air_date`** | Finale date in UTC epoch ms. |
| `externalScore` | 1. TMDB `vote_average` | **1. TMDB `vote_average`** | TMDB score (0.0 – 10.0). |
| `externalScoreSource` | 1. `"TMDB"` | **1. `"TMDB"`** | Stamped as TMDB. |
| `externalId` | 1. TMDB `id.toString()` | **1. TMDB `id.toString()`** | TMDB TV Show ID. |
| `externalSource` | 1. `"TMDB"` | **1. `"TMDB"`** | Marked as TMDB. |
| `tags` | 1. TMDB `genres[].name` | **1. TMDB `genres[].name`** | Comma-separated TV genres. |
| `description` | 1. TMDB `overview` | **1. TMDB `overview`** | Clean series overview. |
| `relatedEntriesJson` | 1. TMDB `fetchRelatedEntries` (2nd call!) | **1. TMDB `recommendations.results` (Embedded in 1st call!)** | **Zero extra network calls!** Combined in 1 request. |

---

### 4.6 BOOKS (Traditional Books & Western Literature)

| Field Name | Current Source & Hierarchy | New Plan Source & Hierarchy | Extraction & Transformation Logic |
|---|---|---|---|
| `title` | 1. Google Books `volumeInfo.title`<br>2. Open Library `docs[].title` | **1. Google Books `volumeInfo.title` (Projected)**<br>*(Parallel race with Open Library for ISBNs)*<br>2. Open Library `docs[].title` | Clean book title. |
| `alternateTitle` | 1. Google Books `volumeInfo.subtitle`<br>2. Open Library `subtitle` | **1. Google Books `volumeInfo.subtitle`**<br>2. Open Library `subtitle` | Book subtitle or translated edition title. |
| `coverImageUri` | 1. Google Books `imageLinks.thumbnail`<br>2. Open Library `covers.openlibrary.org` | **1. Google Books `imageLinks.thumbnail` (rewritten to `https://` & `zoom=1`)**<br>2. Open Library `covers.openlibrary.org/b/id/{id}-L.jpg` | High-res cover image URL. |
| `totalPages` | 1. Google Books `volumeInfo.pageCount`<br>2. Open Library `number_of_pages_median` | **1. Google Books `volumeInfo.pageCount`**<br>2. Open Library `number_of_pages_median` | Total printed page count. |
| `trackingUnit` | 1. `TrackingUnit.PAGES` | **1. `TrackingUnit.PAGES`** | Default tracking unit for books. |
| `airedStartDate` | 1. Google Books `publishedDate`<br>2. Open Library `first_publish_year` | **1. Google Books `publishedDate`**<br>2. Open Library `first_publish_year` | Converted to UTC epoch ms. |
| `airedEndDate` | 1. Same as `airedStartDate` | **1. Same as `airedStartDate`** | Books have matching start/end publish date. |
| `isAiring` | 1. `false` | **1. `false`** | Books are published units. |
| `externalScore` | 1. Google Books `averageRating`<br>2. Open Library `ratings_average` | **1. Google Books `averageRating`**<br>2. Open Library `ratings_average` | Normalized rating (0.0 – 5.0 scaled to 10.0). |
| `externalScoreSource` | 1. `"Google Books"` / `"Open Library"` | **1. `"Google Books"` / `"Open Library"`** | Provider identity. |
| `externalId` | 1. Google Books `id`<br>2. Open Library `key` | **1. Google Books `id`**<br>2. Open Library `key` (clean without `/works/`) | Volume or Work key. |
| `externalSource` | 1. `"Google Books"` / `"Open Library"` | **1. `"Google Books"` / `"Open Library"`** | Source tag. |
| `tags` | 1. Google Books `categories[]`<br>2. Open Library `subject[]` | **1. Google Books `categories[]` (Split by `/`)**<br>2. Open Library `subject[]` | Cleaned genres and subjects. |
| `description` | 1. Google Books `description` + Metadata block<br>2. Open Library `description` | **1. Google Books `description` + Metadata block (Author, ISBN, Publisher)**<br>2. Open Library `description` | HTML stripped, author/publisher appended. |
| `relatedEntriesJson` | 1. Open Library Series API<br>2. Google Books author search | **1. Open Library Series API (Canonical series)**<br>2. Google Books author search | Book series entries (e.g. *Mistborn #1, #2*). |

---

### 4.7 LIGHT NOVELS & ASIAN WEB NOVELS

| Field Name | Current Source & Hierarchy | New Plan Source & Hierarchy | Extraction & Transformation Logic |
|---|---|---|---|
| `title` | 1. AniList `title.romaji`<br>2. Jikan Light Novel `title`<br>3. Google Books `volumeInfo.title` | **1. AniList `title.romaji` (or `english`)**<br>2. Jikan `type=lightnovel` / `novel`<br>3. MangaDex title<br>4. Google Books | Primary Romaji or English novel series title. |
| `alternateTitle` | 1. AniList `title.english` + `native` + `synonyms[]`<br>2. Jikan `titles[]` | **1. AniList `title.english` + `native` + `synonyms[]`**<br>2. Jikan `titles[]`<br>3. Google Books `subtitle` | Full alternate naming list. |
| `coverImageUri` | 1. AniList `coverImage.extraLarge`<br>2. Jikan `large_image_url`<br>3. Google Books cover | **1. AniList `coverImage.extraLarge`**<br>2. Jikan `images.webp.large_image_url`<br>3. Google Books cover | Original novel series volume 1 / main cover. |
| `totalVolumes` | 1. AniList `volumes`<br>2. Jikan `volumes`<br>3. Null | **1. AniList `volumes`**<br>2. Jikan `volumes`<br>3. Null | Total published bunko volume count. |
| `totalChapters` | 1. AniList `chapters`<br>2. Jikan `chapters` | **1. AniList `chapters`**<br>2. Jikan `chapters`<br>3. MangaDex / Web Novel count | Total serialized chapter count. |
| `trackingUnit` | 1. `TrackingUnit.VOLUMES` or `CHAPTERS` | **1. `TrackingUnit.VOLUMES` (if volumes > 0) else `CHAPTERS`** | Auto-inferred based on novel format. |
| `isAiring` | 1. AniList `status == "RELEASING"`<br>2. Jikan `publishing` | **1. AniList `status == "RELEASING"`**<br>2. Jikan `publishing` | Ongoing novel publication flag. |
| `airedStartDate` | 1. AniList `startDate`<br>2. Jikan `published.from` | **1. AniList `startDate`**<br>2. Jikan `published.from` | UTC epoch ms. |
| `airedEndDate` | 1. AniList `endDate`<br>2. Jikan `published.to` | **1. AniList `endDate`**<br>2. Jikan `published.to` | UTC epoch ms. |
| `externalScore` | 1. AniList `averageScore / 10f`<br>2. Jikan `score` | **1. AniList `averageScore / 10f`**<br>2. Jikan `score` | Normalized 0.0 – 10.0. |
| `externalScoreSource` | 1. `"AniList"` | **1. `"AniList"`** | Provider tag. |
| `externalId` | 1. AniList `idMal` or `id` | **1. AniList `idMal` (MAL ID) or `id`**<br>2. Jikan `mal_id` | Stored as numeric ID. |
| `externalSource` | 1. `"AniList"` / `"MAL"` | **1. `"AniList"` / `"MAL"`** | Source tag. |
| `tags` | 1. AniList `genres[]` + `tags[]`<br>2. Jikan `genres[]` | **1. AniList `genres[]` + Light Novel tags**<br>2. Jikan `genres[]` | Preserves tags like "Isekai", "Male Protagonist". |
| `description` | 1. AniList `description` + Staff block (Author & Illustrator)<br>2. Jikan `synopsis` | **1. AniList `description` + Staff block (Story Author + Illustrator)**<br>2. Jikan `synopsis` | Appends author and illustrator credits to description. |
| `relatedEntriesJson` | 1. Jikan relations<br>2. AniList relations | **1. AniList `relations.edges` (Single query)**<br>2. Jikan relations | Anime adaptations, manga adaptations, side stories. |

---

## 5. Field Resolution Precedence & Conflict Rules

When updating or refreshing metadata for an existing library entry, the following conflict resolution rules apply:

1. **User Tracking Data is Sacred:**
   - `score`, `status`, `currentEpisodes`, `currentChapters`, `currentVolumes`, `currentPages`, `startDate`, `finishDate`, `userComments`, and `isFavorite` are **NEVER overwritten** by an external metadata refresh.
   - Only structural metadata fields (`totalEpisodes`, `totalChapters`, `totalVolumes`, `airedEpisodes`, `coverImageUri`, `tags`, `description`, `externalScore`, `relatedEntriesJson`, `broadcastTime`, `nextAiringEpoch`) are updated.
2. **Authoritative Identity Precedence:**
   - If an entry has a verified external ID (e.g. numeric `malId`, TMDB `tmdbId`, MangaDex UUID), direct ID lookup on that source takes top priority before falling back to title searches.
3. **Empty / Null Protection:**
   - A refresh operation never replaces an existing non-blank field with a `null` or empty string from a secondary API response.
4. **Force Refresh vs Gentle Sync:**
   - In standard background sync ("Fetch Missing"), fields that are already populated and valid (e.g. custom user cover image) are preserved.
   - In explicit "Force Refresh" from the detail screen, metadata fields are updated to the latest upstream values, but user tracking numbers remain untouched.
