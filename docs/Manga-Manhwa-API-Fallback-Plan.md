# Manga / Manhwa API Fallback Plan

> **Context:** LX Track List uses **Jikan** (`https://api.jikan.moe/v4`) as the primary source for Manga/Manhwa metadata and cover images, since Jikan wraps MyAnimeList data for free with no API key. This document covers what to do when Jikan fails, is rate-limited, or doesn't have the title (especially for Manhwa, which MAL has poor coverage of).

---

## Why Jikan Fails for Manga / Manhwa

| Failure Reason | Description |
|---|---|
| **Rate limiting** | Jikan enforces 3 req/sec and 60 req/min; burst imports can hit this fast |
| **Manhwa gaps** | MAL's manhwa coverage is poor — many Korean webtoons simply aren't listed |
| **Missing covers** | Some entries have no image on MAL, especially older or niche titles |
| **Downtime** | Jikan is a community-run wrapper; occasional outages occur |
| **Incorrect data** | MAL metadata can be wrong, outdated, or missing chapter/volume counts |

---

## Fallback Source Options

### Comparison at a Glance

| Feature | **MangaDex** | **AniList** | **Kitsu** | **MangaUpdates** |
|---|---|---|---|---|
| **Cost** | 100% Free | 100% Free | 100% Free | Free (scrape/API) |
| **API Key Required** | ❌ No | ❌ No (GraphQL) | ❌ No | ⚠️ Partial |
| **Manga Coverage** | ✅ Massive | ✅ Good | ⚠️ Moderate | ✅ Excellent |
| **Manhwa Coverage** | ✅ Excellent | ✅ Good | ⚠️ Limited | ✅ Excellent |
| **Manhua Coverage** | ✅ Excellent | ⚠️ Limited | ❌ Limited | ✅ Good |
| **Cover Images** | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No |
| **Chapter Count** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Volume Count** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Rate Limit** | Reasonable | Generous | Generous | Low |
| **Closest to Jikan?** | ⭐ **Best match** | ⭐ **2nd best** | ✅ Good | ⚠️ Scrape-only |

---

## 1. MangaDex — ⭐ Primary Fallback (Recommended)

**Base URL:** `https://api.mangadex.org`

The **best free, no-key-required API** for Manga and Manhwa. It is already listed in the main project plan (Section 7.1) for cover images — it should be promoted to the full **metadata fallback** as well.

### Why MangaDex First

- **No API key needed** — just hit the REST endpoints directly (same as Jikan's UX)
- **Massive catalog** — Manga, Manhwa, Manhua, Doujinshi all in one place
- **Excellent Manhwa coverage** — far better than MAL/Jikan for Korean webtoons
- **Rich metadata** — titles (all languages), descriptions, genres, tags, chapter count, volume count, status, demographic, year, authors, artists
- **Cover art endpoint** — dedicated API for cover images in multiple sizes
- **Content ratings** — can filter by `safe`, `suggestive`, `erotica`, `pornographic`

### Useful Endpoints

```
# Search by title
GET https://api.mangadex.org/manga?title=Solo+Leveling&limit=5&contentRating[]=safe&contentRating[]=suggestive

# Get single manga by ID
GET https://api.mangadex.org/manga/{id}?includes[]=cover_art&includes[]=author&includes[]=artist

# Get cover image (returns cover filename, build URL as shown below)
GET https://api.mangadex.org/cover?manga[]={id}&limit=1
# → Cover URL: https://uploads.mangadex.org/covers/{mangaId}/{fileName}
# → Thumbnail:  https://uploads.mangadex.org/covers/{mangaId}/{fileName}.256.jpg
```

### Sample Response (Search)

```json
{
  "data": [
    {
      "id": "32d76d19-8a05-4db0-9fc2-e0b0648fe9d0",
      "type": "manga",
      "attributes": {
        "title": { "en": "Solo Leveling" },
        "altTitles": [{ "ko": "나 혼자만 레벨업" }],
        "description": { "en": "10 years ago..." },
        "status": "completed",
        "year": 2018,
        "lastVolume": "8",
        "lastChapter": "179",
        "publicationDemographic": "shounen",
        "contentRating": "safe",
        "tags": [{ "attributes": { "name": { "en": "Action" } } }]
      }
    }
  ]
}
```

### Field Mapping → `MediaEntry`

| MangaDex Field | → App Field |
|---|---|
| `id` | `externalId` (store as `"MANGADEX_{id}"`) |
| `attributes.title.en` (or `ko`, `ja`) | `title` |
| `attributes.altTitles[]` | `alternateTitle` |
| `attributes.description.en` | `notes` (pre-fill synopsis) |
| `attributes.lastChapter` | `totalChapters` |
| `attributes.lastVolume` | `totalVolumes` |
| `attributes.status` | `airingStatus` (map below) |
| `attributes.year` | part of `airingDates` |
| `attributes.tags[].name.en` | `tags` |
| Cover URL | `coverImageUri` |
| `"MangaDex"` | `externalSource` |

**Status mapping:** `ongoing` → `AIRING`, `completed` → `FINISHED`, `hiatus` → `ON_HOLD`, `cancelled` → `DROPPED`

### No Setup Required ✅

No account, no API key, no registration. Just make the HTTP call.

### Rate Limits

MangaDex asks for reasonable use — keep requests under ~5/sec and add a small delay between calls during bulk imports. No hard documented limit for read-only public endpoints.

---

## 2. AniList — ⭐ Second Best Fallback

**Base URL:** `https://graphql.anilist.co`

AniList is a community tracker (like MAL) with a **completely free GraphQL API** and no key required. It has solid Manga and decent Manhwa coverage, with high-quality metadata.

### Why AniList Second

- **No API key** — anonymous GraphQL queries work out of the box
- **Good Manhwa coverage** — better than MAL, tagged as `MANHWA` in the `countryOfOrigin` field
- **High-quality cover images** — `coverImage.large` and `coverImage.extraLarge`
- **Well-structured data** — chapters, volumes, start/end dates, genres, status are all first-class fields
- **Same GraphQL API used by AniList app** — well-tested and reliable

### Sample GraphQL Query

```graphql
query ($search: String, $type: MediaType) {
  Page(page: 1, perPage: 5) {
    media(search: $search, type: $type, format_in: [MANGA, ONE_SHOT]) {
      id
      title { romaji english native }
      description
      status
      chapters
      volumes
      startDate { year month day }
      endDate { year month day }
      coverImage { large extraLarge }
      genres
      countryOfOrigin
      isAdult
    }
  }
}
```

**Variables:**
```json
{ "search": "Solo Leveling", "type": "MANGA" }
```

**Endpoint:** `POST https://graphql.anilist.co` with `Content-Type: application/json`

### Field Mapping → `MediaEntry`

| AniList Field | → App Field |
|---|---|
| `id` | `externalId` (store as `"ANILIST_{id}"`) |
| `title.english` or `title.romaji` | `title` |
| `title.native` | `alternateTitle` |
| `description` | `notes` |
| `chapters` | `totalChapters` |
| `volumes` | `totalVolumes` |
| `status` | `airingStatus` (map below) |
| `startDate` / `endDate` | `airingDates` |
| `genres` joined | `tags` |
| `coverImage.large` | `coverImageUri` |
| `countryOfOrigin` | use to infer `MANHWA` (KR) vs `MANGA` (JP) vs `MANHUA` (CN) |
| `"AniList"` | `externalSource` |

**Status mapping:** `RELEASING` → `AIRING`, `FINISHED` → `FINISHED`, `NOT_YET_RELEASED` → `NOT_YET_AIRED`, `CANCELLED` → `DROPPED`, `HIATUS` → `ON_HOLD`

**Manhwa Detection:** If `countryOfOrigin == "KR"` and type is MANGA → override `mediaType` to `MANHWA`.

### No Setup Required ✅

Anonymous GraphQL queries require zero authentication.

### Rate Limits

90 requests per minute. Well within normal use.

---

## 3. Kitsu — Lightweight Option

**Base URL:** `https://kitsu.app/api/edge`

A JSON:API-compliant tracker with no key required. Covers Manga reasonably well.

### Quick Reference

```
# Search manga
GET https://kitsu.app/api/edge/manga?filter[text]=solo+leveling&page[limit]=5

# Get cover image
→ attributes.posterImage.large
```

### Pros
- No API key
- JSON:API standard, clean response format
- Has cover images

### Cons
- Manhwa/Manhua coverage is weaker than MangaDex or AniList
- Smaller database overall
- Less active development

**Use as:** 3rd fallback if MangaDex and AniList both miss the title.

---

## 4. MangaUpdates — Specialized Source

**URL:** `https://api.mangaupdates.com/v1`

The most comprehensive metadata database for Manga, Manhwa, and Manhua — especially for obscure titles. Used by many scanlation tools.

### Pros
- Best database for Manhwa/Manhua specifically
- Accurate chapter/volume counts
- Good for titles that MangaDex and AniList don't have

### Cons
- **No cover images** — text metadata only
- API is newer and less documented
- Not meant to be used for cover art retrieval

**Use as:** last-resort for metadata when all other sources fail, then fetch cover from MangaDex separately.

---

## Recommended Fallback Chain

```
Primary: Jikan (api.jikan.moe) — no key, wraps MAL
   ↓ (rate limited / not found / Manhwa)
Fallback 1: MangaDex (api.mangadex.org) — no key, best Manhwa coverage, has covers
   ↓ (not found)
Fallback 2: AniList (graphql.anilist.co) — no key, GraphQL, good metadata + covers
   ↓ (not found)
Fallback 3: Kitsu (kitsu.app/api/edge) — no key, lightweight
```

> **For Manhwa specifically:** Skip Jikan entirely and go straight to **MangaDex → AniList**.

---

## Implementation Plan

### Phase 1 — MangaDex Fallback (No Setup Needed)

This is the highest-value addition with zero user friction.

#### New files to create

```
data/remote/
├── MangaDexService.kt         ← Retrofit interface
├── MangaDexRepository.kt      ← Calls service, maps to MediaEntry
└── dto/
    ├── MangaDexSearchResponse.kt
    ├── MangaDexMangaDto.kt
    └── MangaDexCoverDto.kt
```

#### `MangaDexService.kt` (Retrofit)

```kotlin
interface MangaDexService {
    @GET("manga")
    suspend fun searchManga(
        @Query("title") title: String,
        @Query("limit") limit: Int = 5,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author"),
        @Query("contentRating[]") ratings: List<String> = listOf("safe", "suggestive")
    ): MangaDexSearchResponse

    @GET("manga/{id}")
    suspend fun getMangaById(
        @Path("id") id: String,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author")
    ): MangaDexMangaResponse
}
```

#### Cover URL Builder

```kotlin
fun buildCoverUrl(mangaId: String, fileName: String, size: String = "256"): String {
    return "https://uploads.mangadex.org/covers/$mangaId/$fileName.$size.jpg"
}
```

#### Media Type Detection

```kotlin
fun detectMediaType(attributes: MangaDexAttributes): MediaType {
    return when {
        attributes.originalLanguage == "ko" -> MediaType.MANHWA
        attributes.originalLanguage == "zh" || attributes.originalLanguage == "zh-hk" -> MediaType.MANGA // Manhua — treat as manga for now
        else -> MediaType.MANGA
    }
}
```

---

### Phase 2 — AniList Fallback

#### New files

```
data/remote/
├── AniListService.kt          ← Retrofit or manual OkHttp (GraphQL POST)
├── AniListRepository.kt
└── dto/
    └── AniListMangaDto.kt
```

#### GraphQL via OkHttp (simplest approach)

```kotlin
suspend fun searchManga(query: String): List<AniListMangaDto> {
    val body = """
        {"query":"query(${'$'}s:String){Page(perPage:5){media(search:${'$'}s,type:MANGA,format_in:[MANGA,ONE_SHOT]){id,title{romaji,english,native},chapters,volumes,status,coverImage{large},genres,countryOfOrigin}}}",
         "variables":{"s":"$query"}}
    """.trimIndent()
    // POST to https://graphql.anilist.co
}
```

---

### Phase 3 — Wire Up the Fallback Chain

#### `MangaMetadataRepository.kt`

```kotlin
class MangaMetadataRepository(
    private val jikan: JikanRepository,
    private val mangaDex: MangaDexRepository,
    private val aniList: AniListRepository
) {
    suspend fun search(query: String, mediaType: MediaType): List<MediaSearchResult> {
        // For Manhwa: skip Jikan, go straight to MangaDex
        if (mediaType == MediaType.MANHWA) {
            return mangaDex.search(query).ifEmpty { aniList.search(query) }
        }

        // For Manga: try Jikan first
        return try {
            jikan.searchManga(query).ifEmpty {
                mangaDex.search(query).ifEmpty { aniList.search(query) }
            }
        } catch (e: Exception) {
            // Jikan down or rate-limited
            mangaDex.search(query).ifEmpty { aniList.search(query) }
        }
    }
}
```

---

## What You Need to Do

### No Setup Required — Everything is Zero-Auth

All fallbacks require **zero account creation or API keys**. You can implement them immediately with no prerequisites.

---

## Summary

| Priority | Source | Setup Needed | Best For |
|---|---|---|---|
| ✅ Always ON | **Jikan** | None | Manga (MAL catalog) |
| ✅ Always ON | **MangaDex** | None | Manhwa, Manhua, any manga |
| ✅ Always ON | **AniList** | None | Manga + Manhwa with clean metadata |
| ⚙️ Optional | **Kitsu** | None | Lightweight fallback |

**Bottom line:** Implement MangaDex as your first fallback and AniList as your second. Both are free, require no API key, and together they cover virtually every Manga and Manhwa title that exists.

