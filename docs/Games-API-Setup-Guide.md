# Video Games Tracking — API Setup & Configuration Guide

This guide provides step-by-step instructions for obtaining and configuring all API credentials used by the Video Games tracking system in **LX Track List**.

---

## Quick Summary of Required & Optional APIs

All APIs listed below are **100% free** and **do not require a credit card**.

| Service | Role | Necessity | Free Tier Limits | Credentials Required |
| :--- | :--- | :--- | :--- | :--- |
| **[IGDB v4](#1-igdb-v4-twitch-developer--primary-metadata)** | Primary game metadata, official cover art, franchise series, DLCs/expansions & relations | **Recommended (Primary)** | Unlimited requests (4 req/sec rate limit) | `Client ID` + `Client Secret` |
| **[RAWG](#2-rawg-api--secondary--fallback-metadata)** | Fallback search & metadata when IGDB is unconfigured or finds no match | **Optional (Fallback)** | 20,000 requests / month | `API Key` |
| **[SteamGridDB](#3-steamgriddb-api-v2--custom-600900-posters)** | High-res 600×900 vertical box-art poster picker | **Optional (Poster Picker)** | Unlimited personal use | `API Key` |
| **Steam CDN** | Zero-key vertical posters for Steam games | **Automatic (Built-in)** | Unlimited (public CDN) | None |

> [!TIP]
> **You can start tracking games immediately without any API keys!**
> The app is built with zero-key fallbacks (manual entry, Steam CDN artwork, web image search). However, setting up **IGDB** will unlock automatic box art, franchise checklists, DLC relations, and discovery searches.

---

## 1. IGDB v4 (Twitch Developer) — Primary Metadata

IGDB (Internet Game Database) is owned by Twitch/Amazon and is the most comprehensive video game database available. It provides crisp 2:3 vertical cover art (`t_cover_big_2x`), franchise/series grouping, release dates, platforms, ratings, and DLC/expansion trees.

### Why IGDB?
- **No Monthly Request Cap:** Unlike RAWG (which caps free users at 20,000 requests/month), IGDB has no monthly cap.
- **Native Portrait Covers:** Covers match LX Track List's 2:3 aspect ratio perfectly without cropping or distortion.
- **Franchise Collections:** Automatically detects if a game belongs to a series (e.g., *The Witcher*, *Dark Souls*, *God of War*) to generate checklists.

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

## 2. RAWG API — Secondary / Fallback Metadata

RAWG is a fast, lightweight video games database with over 500,000 games. It uses a single API key with simple HTTP queries (no OAuth token exchanges).

### Why RAWG?
- **Zero-OAuth Fallback:** Uses a direct query parameter (`?key=...`).
- **Instant Setup:** Generates an API key in under 1 minute.
- **Broad Coverage:** Useful fallback when IGDB credentials have not yet been configured.

### Step-by-Step Setup Guide:
1. Go to [rawg.io](https://rawg.io/) and click **Sign Up** to create a free account.
2. Visit the API documentation page: [rawg.io/apidocs](https://rawg.io/apidocs).
3. Click **Get an API Key** (or go to **Profile → Settings → API**).
4. Fill in the brief request form:
   - **Project Name:** `LX Track List`
   - **Description:** `Personal media tracking Android app`
5. Click **Submit** and copy your **API Key**.

---

## 3. SteamGridDB API v2 — Custom 600×900 Posters

SteamGridDB is a community repository of high-resolution video game artwork, including vertical covers, custom posters, and textless variants.

### Why SteamGridDB?
- **High-Resolution Custom Art:** Vertical covers designed specifically at 600×900 resolution.
- **In-App Poster Picker:** Lets you browse alternate box arts and community posters directly from the **Detail Screen** or **Add/Edit Screen** with a single tap.

### Step-by-Step Setup Guide:
1. Go to [steamgriddb.com](https://www.steamgriddb.com/).
2. Click **Sign in through STEAM** in the top right corner.
3. Once logged in, go to your **Preferences**: [steamgriddb.com/profile/preferences/api](https://www.steamgriddb.com/profile/preferences/api).
4. Under the **API** section, click **Generate API Key**.
5. Read and accept the API terms, then copy your generated **API Key**.

---

## 4. How to Configure Keys in LX Track List

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
   - A green confirmation indicates successful authentication.
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

## 5. Official Attribution Notices

In compliance with provider API terms of service:
- **IGDB:** *"Game metadata and cover art provided by IGDB"* (displayed in Settings and Game detail footers).
- **RAWG:** *"Game data powered by RAWG"* (displayed when RAWG data is rendered).
