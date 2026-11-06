# Free Movie / TV Show API Options for LX Track List

> Your app already uses **Jikan** (free, no-key, wraps MyAnimeList) for anime/manga. The goal is to find an equivalent for Movies & TV Shows — free, open, rich data, minimal friction.

---

## Option Comparison At a Glance

| Feature | **TMDB** | **OMDb** | **TVmaze** | **Trakt** |
|---|---|---|---|---|
| **Cost** | Free (non-commercial) | Free tier (1k req/day) | 100% Free | Free tier |
| **API Key Required** | ✅ Yes | ✅ Yes | ❌ No key needed | ✅ Yes (OAuth) |
| **Movies** | ✅ Extensive | ✅ Yes | ❌ **TV only** | ✅ Yes |
| **TV Shows** | ✅ Extensive | ✅ Basic | ✅ Extensive | ✅ Yes |
| **Images (Posters/Backdrops)** | ✅ High-res | ⚠️ Limited (free tier) | ✅ Yes | ❌ No (uses TMDB) |
| **Rate Limit** | ~40 req/10s | 1,000 req/day | 20 req/10s | Fair use |
| **Ratings Data** | ✅ Own ratings | ✅ IMDb + RT + Metacritic | ✅ Own ratings | ✅ Community ratings |
| **Cast/Crew** | ✅ Full | ✅ Basic | ✅ Yes | ⚠️ Limited |
| **Closest to Jikan?** | ⭐ **Best match** | ⚠️ Lightweight | ⚠️ TV only | ⚠️ Social-focused |

---

## 1. TMDB (The Movie Database) — ⭐ Recommended

**URL:** `https://api.themoviedb.org/3/`

The gold standard. Community-driven, massive database, and the closest equivalent to Jikan for movies/shows.

### Pros
- **Huge database** — covers movies, TV shows, seasons, episodes, people, companies, keywords, collections
- **Rich metadata** — full cast/crew, trailers, reviews, similar titles, recommendations, watch providers
- **High-quality images** — multiple poster/backdrop sizes via their CDN (`image.tmdb.org`)
- **Generous rate limits** — ~40 requests per 10 seconds (per IP), far more generous than OMDb
- **Free for non-commercial use** — personal / hobby / open-source projects are explicitly allowed
- **Well-documented** — excellent API docs, Swagger/OpenAPI spec, many community wrappers
- **Active community** — data is constantly updated and curated
- **Supports multiple languages** — localized titles, overviews, posters
- **Has "Watch Providers"** built in — can show where to stream via JustWatch data

### Cons
- **Requires registration** — you need a free account + API key (or Bearer token)
- **Commercial use requires a paid license** — if you ever monetize the app, you'd need to contact `sales@themoviedb.org`
- **Attribution required** — must display the TMDB logo and a disclaimer
- **No IMDb/Rotten Tomatoes scores** — only has its own user ratings (unlike OMDb which aggregates)
- **Caching restrictions** — terms limit how long you can locally cache data

### Why it's the Jikan equivalent
Like Jikan wraps MAL data into a clean REST API, TMDB *is* the primary source itself — a community-maintained open database with a free API. It's the most common choice in apps like yours.

---

## 2. OMDb (Open Movie Database)

**URL:** `https://www.omdbapi.com/`

Lightweight, IMDb-centric lookups. Think of it as a quick-reference tool rather than a full database.

### Pros
- **Aggregated ratings** — returns IMDb, Rotten Tomatoes, and Metacritic scores in one response
- **Simple API** — search by title or IMDb ID, get a flat JSON response
- **Very easy to integrate** — minimal setup, single endpoint
- **Good for supplementary data** — pair with TMDB to get ratings TMDB doesn't have

### Cons
- **1,000 requests/day on free tier** — very restrictive for a sync-heavy app
- **Limited data** — no cast lists, no seasons/episodes breakdown, no recommendations, no trailers
- **No images on free tier** — poster URLs may be restricted to Patreon backers
- **Patreon-gated features** — higher limits and full poster access require a paid Patreon tier ($1–$5/month)
- **Not a full database** — can't browse, discover, or get trending/popular lists
- **Maintenance concerns** — single-maintainer project, less reliable long-term than TMDB

---

## 3. TVmaze

**URL:** `https://api.tvmaze.com/`

The most "Jikan-like" in terms of **zero friction** — no API key at all, just hit the endpoint.

### Pros
- **No API key required** — literally `GET https://api.tvmaze.com/shows/1` and you get data. Most like Jikan's zero-auth model
- **100% free** — no tiers, no Patreon, no hidden costs
- **Rich TV data** — full episode guides, schedules, networks, web channels, cast
- **Embedding** — `?embed=episodes` or `?embed=cast` fetches related data in one call
- **Show schedule** — can query what's airing today/this week by country

### Cons
- **⚠️ TV shows ONLY — no movies at all** — this is a dealbreaker if you need movies
- **Lower rate limit** — 20 requests per 10 seconds (still reasonable)
- **Smaller database** — less comprehensive than TMDB for international or obscure shows
- **No streaming availability data**
- **Less active community** than TMDB

---

## 4. Trakt

**URL:** `https://api.trakt.tv/`

Best for social/tracking features (what users are watching, watchlists, ratings).

### Pros
- **User watch tracking** — built for "I watched this" tracking, similar to MAL's list functionality
- **Rich social data** — community ratings, comments, lists, check-ins
- **Covers both movies and TV**
- **Trending / popular / anticipated lists** — great for discovery features
- **Free for individual developers**

### Cons
- **No images** — Trakt has zero poster/backdrop hosting; you'd need TMDB anyway for artwork
- **OAuth required** — more complex auth flow than a simple API key
- **Heavier integration** — designed around user accounts, not just data lookup
- **API complexity** — more endpoints and concepts to manage than TMDB
- **Overkill** if you just want metadata — its strength is the social/tracking layer

---

## 5. Watchmode (Honorable Mention)

**URL:** `https://api.watchmode.com/`

Specializes in **"where to watch"** — which streaming service has a given title.

### Pros
- Free tier available
- Covers streaming availability across 200+ services and many countries

### Cons
- Very limited metadata (not a replacement for TMDB)
- Better as a supplementary API, not a primary data source
- Rate limits on free tier

---

## Recommendation for LX Track List

**Use TMDB as your primary source.** It's the closest equivalent to Jikan:
- Free for non-commercial use ✅
- Comprehensive data (movies + TV) ✅
- Well-documented REST API ✅
- Active community-maintained database ✅
- Generous rate limits ✅
- High-quality images included ✅

### Suggested Architecture
1. **Primary:** TMDB API for all movie/show metadata, images, cast, trailers, recommendations
2. **Optional supplement:** OMDb for IMDb/Rotten Tomatoes/Metacritic ratings (if you want aggregated scores)
3. **Build a `TmdbSyncManager`** mirroring your existing `JikanSyncManager` pattern

### If you want the absolute simplest "just works" option
Use **TVmaze** for TV shows only (no key needed, like Jikan), but you'll still need TMDB for movies. So TMDB alone covers both and is the cleaner single-source solution.

