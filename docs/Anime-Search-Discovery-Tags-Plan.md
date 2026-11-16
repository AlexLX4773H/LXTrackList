# Anime Search & Discovery with Multi-State Tag Filtering Plan

> **File:** `docs/Anime-Search-Discovery-Tags-Plan.md`  
> **Status:** Draft / Planned  
> **Target Screen:** Search Tab → Anime Category → Hamburger Menu (`☰`)  

---

## 1. Executive Summary & Objectives

Currently, when the **Anime** category is selected in the **Search Tab**, pressing the **Hamburger icon (`☰`)** opens a modal bottom sheet (`SearchDiscoverySheet.kt`) that only supports **Seasonal Anime** exploration (choosing between Winter/Spring/Summer/Fall and a specific Year).

This plan outlines the architecture, feasibility, data models, and UI/UX design to expand the Anime discovery sheet into a full-featured discovery and filtering center:
1. **Primary Discovery Modes on Hamburger Press**:
   - 🌸 **Seasonal Anime** (Browse by specific Season and Year).
   - 🔥 **Most Popular** (Browse all-time or currently trending anime ranked by members/popularity).
   - 📺 **Top Airing** (Browse anime currently broadcasting, ranked by score or popularity).
2. **Tag-Based Filtering Section (Genres, Themes, Demographics)**:
   - Filter remote anime from **MyAnimeList (MAL / Jikan v4)** and **AniList GraphQL** using standardized anime tags.
   - Categorized into **Genres** (Action, Romance, Sci-Fi, etc.), **Themes** (Isekai, School, Mecha, etc.), and **Demographics** (Shounen, Seinen, Shoujo, etc.).
3. **Multi-Select & Multi-Exclude Tag Bar (Tri-State)**:
   - Mirror the tri-state selection interaction already implemented in the local library (`LibraryScreen.kt`):
     - **Tap 1**: `UNSELECTED` → `INCLUDED` (Green `✓` / `+Tag`)
     - **Tap 2**: `INCLUDED` → `EXCLUDED` (Red `✕` / `-Tag`)
     - **Tap 3**: `EXCLUDED` → `UNSELECTED` (Neutral state)
   - Real-time active chip bar displaying current inclusions and exclusions with 1-tap quick dismissal.

---

## 2. Feasibility Report

| Feature | Feasibility | Technical Backing & Constraints |
| :--- | :--- | :--- |
| **1. Hamburger Menu Options (Seasonal / Most Popular / Top Airing)** | **High (100% Feasible)** | - **AniList GraphQL**: Supports `season`/`seasonYear`, `sort: [POPULARITY_DESC]`, and `status: RELEASING, sort: [SCORE_DESC]` natively with instant response and high rate limits.<br>- **Jikan API v4**: Has dedicated endpoints `/seasons/{year}/{season}`, `/top/anime?filter=bypopularity`, and `/top/anime?filter=airing` (or `/anime?status=airing&order_by=score`).<br>- Both providers already exist and are integrated in `JikanApiClient.kt`. |
| **2. Remote Tag Filtering (Genres, Themes, Demographics)** | **High (100% Feasible)** | - **AniList GraphQL**: Accepts `genre_in`, `genre_not_in`, `tag_in`, `tag_not_in` as string arrays directly.<br>- **Jikan API v4**: Accepts `genres` (comma-separated MAL genre IDs to include) and `genres_exclude` (comma-separated MAL genre IDs to exclude) on `/anime`.<br>- A static lookup dictionary maps tag names to MAL IDs cleanly and reliably without requiring extra network overhead. |
| **3. Multi-Select & Multi-Exclude (Tri-State) Tag UI** | **High (100% Feasible)** | - Reuses the exact same tri-state state machine (`TagSelectionState`: `UNSELECTED`, `INCLUDED`, `EXCLUDED`) and chip style tested in `LibraryViewModel` / `LibraryScreen`.<br>- Fast in-memory state in Compose before applying.<br>- Fully compatible with search pagination (`page = 1, 2, 3...`). |

### Key Considerations & Edge Cases:
1. **Zero-Result Filter Combinations**: Users could select conflicting tags (e.g., `+Kids` AND `+Gore` or 10+ niche tags). The UI will display a clear empty state: *"No anime found matching all selected tag filters. Try removing some filters."* with a 1-tap **"Reset Tags"** button.
2. **Rate Limiting**: Jikan v4 has a 3 req/sec throttle. By using AniList GraphQL as the primary provider with Jikan as automatic fallback (already established in the codebase), requests will be swift, responsive, and avoid 429 rate limit errors.
3. **Tag Name Discrepancies**: MAL categorizes tags into Genres, Themes, Demographics, and Explicit Genres using integer IDs. AniList uses string tags and genres. A unified Kotlin tag definition model will bridge both cleanly.

---

## 3. Architecture & Data Models

### 3.1 Discovery Mode Enum
```kotlin
enum class AnimeDiscoveryMode(val displayName: String, val iconEmoji: String) {
    SEASONAL("Seasonal", "🌸"),
    MOST_POPULAR("Most Popular", "🔥"),
    TOP_AIRING("Top Airing", "📺")
}
```

### 3.2 Tag Model & Categorization
```kotlin
enum class AnimeTagCategory(val displayName: String) {
    GENRE("Genres"),
    THEME("Themes"),
    DEMOGRAPHIC("Demographics")
}

data class AnimeTagDefinition(
    val name: String,
    val category: AnimeTagCategory,
    val malId: Int,
    val isExplicit: Boolean = false
)
```

#### Curated Tag Registry (MAL ID & AniList Name Mapping):
- **Genres**:
  - `Action` (ID 1), `Adventure` (ID 2), `Avant Garde` (ID 5), `Award Winning` (ID 46), `Comedy` (ID 4), `Drama` (ID 8), `Fantasy` (ID 10), `Gourmet` (ID 47), `Horror` (ID 14), `Mystery` (ID 7), `Romance` (ID 22), `Sci-Fi` (ID 24), `Slice of Life` (ID 36), `Sports` (ID 30), `Supernatural` (ID 37), `Suspense` (ID 41), `Ecchi` (ID 9).
- **Themes**:
  - `Isekai` (ID 61), `School` (ID 23), `Mecha` (ID 18), `Military` (ID 38), `Music` (ID 19), `Historical` (ID 13), `Psychological` (ID 40), `Super Power` (ID 31), `Space` (ID 29), `Harem` (ID 35), `Survival` (ID 77), `Martial Arts` (ID 17), `Time Travel` (ID 79), `Vampire` (ID 32), `Video Game` (ID 80), `Mythology` (ID 6), `Detective` (ID 39), `Gag Humor` (ID 57), `Gore` (ID 58), `Workplace` (ID 48), `Otaku Culture` (ID 68).
- **Demographics**:
  - `Shounen` (ID 27), `Seinen` (ID 42), `Shoujo` (ID 25), `Josei` (ID 43), `Kids` (ID 15).

### 3.3 Enhanced DiscoveryFilter Model
Update `DiscoveryFilter.Anime` in `DiscoveryModels.kt`:
```kotlin
data class Anime(
    val mode: AnimeDiscoveryMode = AnimeDiscoveryMode.SEASONAL,
    val season: AnimeSeason = AnimeSeason.current(),
    val year: Int = AnimeSeason.currentYear(),
    val sortOrder: AnimeSortOrder = AnimeSortOrder.POPULARITY,
    val includedTags: Set<String> = emptySet(),
    val excludedTags: Set<String> = emptySet(),
    override val adultMode: AdultMode = AdultMode.OFF
) : DiscoveryFilter() {
    override val label: String
        get() {
            val base = when (mode) {
                AnimeDiscoveryMode.SEASONAL -> "${season.emoji} ${season.displayName} $year"
                AnimeDiscoveryMode.MOST_POPULAR -> "🔥 Most Popular"
                AnimeDiscoveryMode.TOP_AIRING -> "📺 Top Airing"
            }
            val tagSummary = when {
                includedTags.isNotEmpty() && excludedTags.isNotEmpty() -> " • +${includedTags.size} / -${excludedTags.size} tags"
                includedTags.isNotEmpty() -> " • +${includedTags.size} tags"
                excludedTags.isNotEmpty() -> " • -${excludedTags.size} tags"
                else -> ""
            }
            val sortText = if (mode != AnimeDiscoveryMode.SEASONAL) " • ${sortOrder.displayName}" else ""
            val adultText = when (adultMode) {
                AdultMode.OFF -> ""
                AdultMode.INCLUDE -> " • 18+"
                AdultMode.ONLY -> " • 18+ Only"
            }
            return "$base$sortText$tagSummary$adultText"
        }
}
```

---

## 4. Remote API Query Specifications

### 4.1 AniList GraphQL Query (Primary)
```graphql
query (
  $page: Int,
  $perPage: Int,
  $season: MediaSeason,
  $seasonYear: Int,
  $status: MediaStatus,
  $sort: [MediaSort],
  $genreIn: [String],
  $genreNotIn: [String],
  $tagIn: [String],
  $tagNotIn: [String],
  $isAdult: Boolean
) {
  Page(page: $page, perPage: $perPage) {
    pageInfo {
      hasNextPage
      total
    }
    media(
      type: ANIME,
      season: $season,
      seasonYear: $seasonYear,
      status: $status,
      sort: $sort,
      genre_in: $genreIn,
      genre_not_in: $genreNotIn,
      tag_in: $tagIn,
      tag_not_in: $tagNotIn,
      isAdult: $isAdult
    ) {
      id
      idMal
      title { romaji english native }
      synonyms
      coverImage { extraLarge large }
      duration
      episodes
      format
      status
      averageScore
      description(asHtml: false)
      startDate { year month day }
      endDate { year month day }
      genres
      tags { name }
      nextAiringEpisode { id airingAt timeUntilAiring episode }
    }
  }
}
```

#### Mapping by Mode:
- **Seasonal**: `$season = WINTER|SPRING|SUMMER|FALL`, `$seasonYear = year`, `$sort = [POPULARITY_DESC]`.
- **Most Popular**: `$sort = [POPULARITY_DESC]`, `$season = null`, `$seasonYear = null`.
- **Top Airing**: `$status = RELEASING`, `$sort = [SCORE_DESC]` (or `[POPULARITY_DESC]`).
- **Tags**: Included genres passed to `$genreIn`, excluded to `$genreNotIn`; themes/demographics passed to `$tagIn` / `$tagNotIn`.

### 4.2 Jikan API v4 Query (Secondary / Fallback)
Construct dynamic query URL on `/anime` or `/top/anime`:
- **Seasonal**:
  - If no custom tags: `/seasons/{year}/{season}?page={page}&limit=25`
  - If custom tags present: `/anime?start_date={year-season-start}&end_date={year-season-end}&genres={includedIds}&genres_exclude={excludedIds}&order_by=members&sort=desc`
- **Most Popular**:
  - `/anime?order_by=members&sort=desc&genres={includedIds}&genres_exclude={excludedIds}&page={page}`
- **Top Airing**:
  - `/anime?status=airing&order_by=score&sort=desc&genres={includedIds}&genres_exclude={excludedIds}&page={page}`

---

## 5. UI / UX Design in Compose

### 5.1 Redesigned `SearchDiscoverySheet.kt`

```
┌────────────────────────────────────────────────────────┐
│  🧭 Anime Discovery & Filters                 [Reset]  │
├────────────────────────────────────────────────────────┤
│  Mode Selector (Single-Select Segmented Row):          │
│  [ 🌸 Seasonal ]  [ 🔥 Most Popular ]  [ 📺 Top Airing ]│
├────────────────────────────────────────────────────────┤
│  (If 'Seasonal' selected):                             │
│    Quick Shortcuts: [ Current Season ]  [ Upcoming ]   │
│    Season (2x2 Grid): [Winter] [Spring] [Summer] [Fall]│
│    Year: [2026] [2025] [2024] [2023] [More ▾]          │
│                                                        │
│  (If 'Most Popular' or 'Top Airing' selected):         │
│    Sort By: [Highest Rated] [Most Popular] [Newest]    │
├────────────────────────────────────────────────────────┤
│  🏷️ Tags & Genres                                      │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 🔍 Search tags (e.g. Action, Isekai, Shounen)... │  │
│  └──────────────────────────────────────────────────┘  │
│  Category Filter: [All (45)] [Genres] [Themes] [Demo]  │
│                                                        │
│  Tag Chips Flow (Tri-State Tap to Cycle):              │
│  [ ✓ Action ]  [ ✕ Ecchi ]  [ Romance ]  [ ✓ Isekai ] │
│  [ Shounen ]   [ ✕ Harem ]  [ Comedy ]   [ Sci-Fi ]   │
│  *(Tap once to include ✓, twice to exclude ✕, 3rd clears)*│
├────────────────────────────────────────────────────────┤
│  [✓] 18+ Content Toggle (Off / Include / Only)         │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │    Apply Filters (e.g. Explore Fall 2026 Anime)  │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### 5.2 Tri-State Tag Component Details
- **Unselected**:
  - Border: `1.dp` subtle grey border (`LXBorder` / `LXTextSecondary.copy(alpha = 0.4f)`).
  - Background: `LXSurfaceElevated`.
  - Icon: None.
  - Text: `LXTextSecondary`.
- **Included (`+Tag`)**:
  - Border: `1.dp` solid green border (`LXPrimaryGreen`).
  - Background: `LXPrimaryGreen.copy(alpha = 0.15f)`.
  - Icon: `Icons.Default.Check` (Green).
  - Text: `LXPrimaryGreen` (Bold).
- **Excluded (`-Tag`)**:
  - Border: `1.dp` solid red border (`LXErrorRed`).
  - Background: `LXErrorRed.copy(alpha = 0.15f)`.
  - Icon: `Icons.Default.Close` (Red).
  - Text: `LXErrorRed` (Bold).

### 5.3 Active Tag Pills on `SearchScreen.kt`
When discovery filters are applied, active chips appear under the search bar / category row:
```
[ 🌸 Fall 2026 ✕ ]  [ +Action ✕ ]  [ +Isekai ✕ ]  [ -Ecchi ✕ ]  [ Clear All ]
```
- Tapping any chip immediately removes that tag and re-fetches the results seamlessly.

---

## 6. Implementation Roadmap

### Phase 1: Models & Tag Registry
1. Add `AnimeDiscoveryMode` enum to `DiscoveryModels.kt`.
2. Expand `DiscoveryFilter.Anime` to carry `mode`, `includedTags`, and `excludedTags`.
3. Create `AnimeTagRegistry.kt` defining standardized genres, themes, and demographics with MAL ID bindings and helper search methods.

### Phase 2: Remote API Client Enhancements
1. Update `JikanApiClient.kt`:
   - Enhance `queryAniListSeasonal` to support `mode` (`POPULAR`, `TOP_AIRING`, `SEASONAL`), `genreIn`, `genreNotIn`, `tagIn`, `tagNotIn`.
   - Enhance `queryJikanSeasonal` / add `queryJikanDiscovery` to support MAL genre inclusion/exclusion IDs and status filters.
   - Combine both in `fetchAnimeDiscovery(...)` with graceful fallback from AniList to Jikan.

### Phase 3: ViewModel & State Logic
1. Update `SearchViewModel.kt`:
   - Add `applyAnimeDiscoveryFilter(...)` accepting the full `DiscoveryFilter.Anime` configuration.
   - Update `loadMore()` to paginate discovery results for all three modes with tag filters.
   - Provide helper methods for cycling tag states and removing individual tag filters.

### Phase 4: UI Composable Integration
1. Update `SearchDiscoverySheet.kt`:
   - Add Mode Selector tabs (`Seasonal`, `Most Popular`, `Top Airing`).
   - Implement the Tags & Genres filter section with live search and category tabs.
   - Implement tri-state chip cycling using `TagSelectionState`.
2. Update `SearchScreen.kt`:
   - Display active tag chips below the search header when active discovery filter contains included/excluded tags.
   - Wire up 1-tap tag removal.

### Phase 5: Verification & Testing
1. Unit tests in `SearchViewModelTest` and `JikanApiClientTest` for:
   - Tag cycling logic (neutral → included → excluded → neutral).
   - GraphQL query generation with `genre_in`, `genre_not_in`, `tag_in`, `tag_not_in`.
   - Jikan URL generation with `genres` and `genres_exclude`.
   - Discovery mode mapping.
2. Build and verify end-to-end with `./gradlew testDebugUnitTest`.

---

## 7. Verification Checklist

- [ ] Selecting **Seasonal Anime** retains season and year selection.
- [ ] Selecting **Most Popular** queries all-time/trending anime without season restriction.
- [ ] Selecting **Top Airing** queries currently broadcasting anime (`status = RELEASING`).
- [ ] Tapping a tag once marks it as **Included (+)** with a green tick.
- [ ] Tapping the same tag again marks it as **Excluded (-)** with a red cross.
- [ ] Tapping the tag a third time resets it to **Unselected**.
- [ ] Tag search field filters tag chips by name smoothly.
- [ ] Category tabs (`All`, `Genres`, `Themes`, `Demographics`) filter the displayed tag chips.
- [ ] Applying filters returns relevant results from AniList/MAL.
- [ ] Infinite scrolling / pagination loads subsequent pages with the active tags maintained.
- [ ] Active tag chips appear on the search screen and can be dismissed individually.
- [ ] All unit tests compile and pass.
