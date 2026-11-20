# Video Games Tracking — API Setup & Configuration Guide

This guide provides step-by-step instructions for obtaining and configuring all API credentials used by the Video Games tracking system in **LX Track List**, along with comprehensive details on **usage quotas, rate limits, and built-in throttling**.

---

## 1. Quick Summary & Usage Limits Comparison

All APIs listed below are **100% free** and **do not require a credit card**.

| Service | Role | Monthly Quota | Rate Limit (Throttling) | Burst Limit | Status on Exceeded | App Built-in Handling |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **[IGDB v4](#2-igdb-v4-twitch-developer--primary-metadata)** | Primary metadata, covers, series & DLCs | **Unlimited** | **4 req / sec** | 8 req / window | `HTTP 429` | Paced with 270ms loop delay (≤ 3.7 req/s) + token caching |
| **[RAWG](#3-rawg-api--secondary--fallback-metadata)** | Fallback search & secondary metadata | **20,000 req / mo** | **5 req / sec** | ~10 req / burst | `HTTP 429` | Zero-OAuth fallback only when IGDB has 0 hits or is empty |
| **[SteamGridDB](#4-steamgriddb-api-v2--custom-600900-posters)** | High-res 600×900 vertical poster picker | **Unlimited** (Personal) | **~100 req / min** | 30 req / 10s | `HTTP 429` | On-demand UI search only (never called in bulk background sync) |
| **Steam CDN** | Zero-key vertical posters for Steam games | **Unlimited** | None (Static CDN) | Unlimited | `HTTP 404` (if unlisted) | Local Coil disk & memory cache |

> [!TIP]
> **You can start tracking games immediately without any API keys!**
> The app is built with zero-key fallbacks (manual entry, Steam CDN artwork, web image search). However, setting up **IGDB** will unlock automatic box art, franchise checklists, DLC relations, and discovery searches.

---

## 2. IGDB v4 (Twitch Developer) — Primary Metadata

IGDB (Internet Game Database) is owned by Twitch/Amazon and is the most comprehensive video game database available. It provides crisp 2:3 vertical cover art (`t_cover_big_2x`), franchise/series grouping, release dates, platforms, ratings, and DLC/expansion trees.

### Usage & Throttling Limits
- **Rate Limit (Throttling):** Exactly **4 requests per second (4 req/sec)** per Client ID.
- **Burst Window:** Allows small bursts of up to **8 requests** within short intervals, but sustained bursts will immediately trigger `HTTP 429 Too Many Requests`.
- **Monthly Volume Limit:** **Unlimited**. There is no daily or monthly cap on the number of requests you can make.
- **Payload & Pagination Limit:** Maximum **500 records** per single query (`limit 500;`).
- **OAuth Token Lifetime:** App Access Tokens (`client_credentials`) generated via `https://id.twitch.tv/oauth2/token` remain valid for approximately **60 days (~5,184,000 seconds)**. The token endpoint itself is limited to 100 requests per minute.
- **How LX Track List Protects Your Quota:**
  - `GameSyncManager` paces all batch requests with a built-in rate limit delay (`RATE_LIMIT_DELAY_MS = 270L`), guaranteeing that background library syncs never exceed 3.7 requests per second.
  - Cached OAuth tokens and expiration timestamps are stored in private `SharedPreferences` so the app only hits the Twitch OAuth server once every ~60 days (or when force-refreshed).

### Step-by-Step Setup Guide:
1. **Twitch Account & 2FA:**
   - Log in or create a free account at [twitch.tv](https://www.twitch.tv/).
   - Ensure **Two-Factor Authentication (2FA)** is enabled in your Twitch Account Security settings (Twitch requires 2FA to access the developer console).
2. **Access Twitch Developer Console:**
   - Go to [dev.twitch.tv/console](https://dev.twitch.tv/console).
3. **Register an Application:**
   - Click on the **Applications** tab, then click **+ Register Your Application**.
   - **Name:** Enter `LX Track List - Personal` (must be unique on Twitch; if taken, append random numbers).
   - **OAuth Redirect URLs:** Enter `http://localhost` (not used by client credentials flow, but Twitch requires a valid URL).
   - **Category:** Select **Application Integration** or **Game Integration**.
   - **Client Type:** Select **Confidential**.
   - Complete the CAPTCHA and click **Create**.
4. **Retrieve Credentials:**
   - In the applications list, click **Manage** next to your registered application.
   - Copy the **Client ID** (a ~30-character alphanumeric string).
   - Click **New Secret** to generate a **Client Secret**, and copy it immediately (it will only be shown once).

---

## 3. RAWG API — Secondary / Fallback Metadata

RAWG is a fast, lightweight video games database with over 500,000 games. It uses a single API key with simple HTTP queries (no OAuth token exchanges).

### Usage & Throttling Limits
- **Rate Limit (Throttling):** **5 requests per second (5 req/sec)**. Exceeding this rate returns `HTTP 429 Too Many Requests`.
- **Monthly Volume Limit:** **20,000 requests per calendar month** on the free tier. The quota resets on the 1st of every month.
- **Page Size Limit:** Maximum **40 games** per single search query (`page_size=40`).
- **How LX Track List Protects Your Quota:**
  - RAWG is strictly utilized as a secondary fallback. The app only queries RAWG when IGDB credentials are empty or when an IGDB search returns zero hits.
  - Because of this tiered fallback design, personal tracking typically consumes less than 50–100 requests per month (less than 0.5% of your free allowance).

### Step-by-Step Setup Guide:
1. Go to [rawg.io](https://rawg.io/) and click **Sign Up** to create a free account.
2. Visit the API documentation page: [rawg.io/apidocs](https://rawg.io/apidocs).
3. Click **Get an API Key** (or go to **Profile → Settings → API**).
4. Fill in the brief request form:
   - **Project Name:** `LX Track List`
   - **Description:** `Personal media tracking Android app`
5. Click **Submit** and copy your **API Key**.

---

## 4. SteamGridDB API v2 — Custom 600×900 Posters

SteamGridDB is a community repository of high-resolution video game artwork, including vertical covers, custom posters, and textless variants.

### Usage & Throttling Limits
- **Rate Limit (Throttling):** Approximately **100 requests per minute (~1.6 req/sec)** on standard personal API keys.
- **Burst Window:** Allows up to **30 requests in a 10-second burst**, after which temporary `HTTP 429` responses are returned until the window cools down.
- **Monthly Volume Limit:** **Unlimited** for personal non-commercial use.
- **Dimensions Filter:** The app specifically queries `dimensions=600x900` with `types=static` to guarantee vertical portrait aspect ratios without wasting bandwidth on animated GIFs or horizontal banners.
- **How LX Track List Protects Your Quota:**
  - SteamGridDB is **never** called during automated background sync jobs.
  - Queries only fire on-demand when you explicitly tap **"SteamGridDB"** in the Add/Edit screen, tap the cover art in the Detail screen, or use the poster search sheet.

### Step-by-Step Setup Guide:
1. Go to [steamgriddb.com](https://www.steamgriddb.com/).
2. Click **Sign in through STEAM** in the top right corner.
3. Once logged in, go to your **Preferences**: [steamgriddb.com/profile/preferences/api](https://www.steamgriddb.com/profile/preferences/api).
4. Under the **API** section, click **Generate API Key**.
5. Read and accept the API terms, then copy your generated **API Key**.

---

## 5. Steam Cloudflare CDN — Zero-Key Poster Fallback

When a game in your library has a known Steam Application ID (`steamAppId`), LX Track List can automatically load its official vertical library cover directly from Steam's content delivery network:
```
https://cdn.cloudflare.steamstatic.com/steam/apps/{steamAppId}/library_600x900_2x.jpg
```

### Usage & Throttling Limits
- **Rate Limit (Throttling):** None (public global CDN).
- **Monthly Volume Limit:** **Unlimited**.
- **Caching:** Automatically cached on device storage via Coil image loader to prevent redundant network fetches.

---

## 6. How to Configure Keys in LX Track List

You have two convenient ways to configure your API keys:

### Option A: Directly in App Settings (Recommended for Sideloaded Builds)
No need to rebuild the application! You can paste and test keys directly on your device:
1. Launch **LX Track List**.
2. Tap the **Settings** tab.
3. Select **External Sync Jobs**.
4. Scroll to the **Video Games Metadata & Posters** card.
5. Paste your credentials:
   - `IGDB Client ID` & `IGDB Client Secret`
   - `RAWG API Key`
   - `SteamGridDB API Key`
6. Tap the **Test Connection** button under each service:
   - A green confirmation message indicates successful authentication.
   - Credentials are automatically saved to private encrypted preferences on your device.

### Option B: Compile-Time via `.env` (Recommended for Dev & Personal Builds)
If you build and run the app from Android Studio or command-line Gradle:
1. In the project root, copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
2. Open `.env` and fill in the game tracking fields:
   ```env
   # 7. IGDB API (Twitch Developer) — Video Games Primary Metadata
   IGDB_CLIENT_ID=your_twitch_client_id_here
   IGDB_CLIENT_SECRET=your_twitch_client_secret_here

   # 8. RAWG API — Video Games Secondary / Fallback Metadata
   RAWG_API_KEY=your_rawg_api_key_here

   # 9. SteamGridDB API v2 — Custom Vertical Posters & Hero Art Picker
   STEAMGRIDDB_API_KEY=your_steamgriddb_api_key_here
   ```
3. Run or build the app:
   ```bash
   ./gradlew assembleDebug
   ```
   *Gradle will automatically inject these into `BuildConfig` during compilation.*

---

## 7. Official Attribution Notices

In compliance with provider API terms of service:
- **IGDB:** *"Game metadata and cover art provided by IGDB"* (displayed in Settings and Game detail footers).
- **RAWG:** *"Game data powered by RAWG"* (displayed when RAWG data is rendered).
