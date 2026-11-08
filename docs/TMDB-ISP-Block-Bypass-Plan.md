# Plan: TMDB ISP Block Bypass (App-Level)

## Problem

Some Indian ISPs (notably Jio Fiber/AirFiber and certain mobile carriers) block access to `api.themoviedb.org`. This prevents the app from performing any TMDB operations — search, entity details, entity edit metadata fetch, and background refresh sync jobs all fail with connection timeouts.

The error from the user's screenshot:

```
failed to connect to api.themoviedb.org/49.44.79.236 (port 443)
from /192.0.0.8 (port 41088) after 15000ms
```

The IP `49.44.79.236` is a Jio-poisoned DNS response (not a real TMDB IP), confirming this is a **DNS-level block**. The ISP's DNS resolver returns a bogus/sinkhole IP instead of the actual TMDB server address.

### Constraint

The solution must work **entirely at the app level** — no user action required such as changing device DNS settings, installing a VPN, or modifying router configuration.

---

## Current Architecture

All TMDB network calls flow through a single file:

- **[`TmdbApiClient.kt`](../app/src/main/kotlin/com/lx/tracklist/data/remote/TmdbApiClient.kt)** — `object TmdbApiClient`
  - Uses a single `OkHttpClient` instance (OkHttp `4.12.0`)
  - Base URL: `https://api.themoviedb.org/3` (`DEFAULT_BASE_URL` constant)
  - Image URL: `https://image.tmdb.org/t/p` (`IMAGE_BASE_URL` constant)
  - 15-second connect/read timeouts
  - No custom DNS resolver configured — uses system DNS (which is the ISP's DNS)
  - No retry logic on connection failures

### All Call Sites (Affected Features)

| Feature | Method | File |
|---------|--------|------|
| Movie search | `searchMoviesMultiple()`, `searchMovieByTitle()` | `TmdbApiClient.kt` |
| TV search | `searchTvMultiple()`, `searchTvByTitle()` | `TmdbApiClient.kt` |
| Movie details | `fetchMovieDetails()` | `TmdbApiClient.kt` |
| TV details | `fetchTvDetails()` | `TmdbApiClient.kt` |
| Combined details | `fetchMediaDetails()`, `searchMediaDetailsByTitle()` | `TmdbApiClient.kt` |
| Related entries | `fetchRelatedEntries()` | `TmdbApiClient.kt` |
| Background sync | Uses `TmdbApiClient` methods | `TmdbSyncManager.kt` |
| Library entity page | Uses `TmdbApiClient.fetchMovieDetails/fetchTvDetails` | `MediaRepository.kt` |
| Entity edit page | Uses `TmdbApiClient.searchMoviesMultiple/searchTvMultiple` | `SearchViewModel.kt` |
| Image loading | Coil loads from `image.tmdb.org` URLs | `LXApplication.kt` (ImageLoader), many UI composables |

**Key insight**: Since all API calls go through the single `okHttpClient` in `TmdbApiClient`, and all image loads go through Coil's single `ImageLoader` in `LXApplication`, fixing DNS resolution in these two HTTP clients covers 100% of TMDB traffic.

---

## Solution: Multi-Layer App-Level DNS Bypass

### Layer 1 — OkHttp DNS-over-HTTPS (DoH)

OkHttp has a first-party `okhttp-dnsoverhttps` module that routes DNS queries over HTTPS to a trusted public resolver (Cloudflare `1.1.1.1` or Google `dns.google`). Since the query travels over port 443 as standard HTTPS traffic, the ISP cannot intercept, tamper with, or even distinguish it from normal web traffic.

#### Changes Required

**1. Add dependency** in [`app/build.gradle.kts`](../app/build.gradle.kts):

```kotlin
// OkHttp DNS-over-HTTPS (bypass ISP DNS blocking)
implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")
```

**2. Create `TmdbDnsHelper.kt`** — new file alongside the existing client:

```kotlin
package com.lx.tracklist.data.remote

import android.content.Context
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.io.File
import java.net.InetAddress

object TmdbDnsHelper {

    /**
     * Builds a DnsOverHttps resolver that sends DNS queries to
     * Cloudflare's 1.1.1.1 over HTTPS. Falls back to Google DoH
     * if Cloudflare is unreachable.
     *
     * The bootstrap client uses hardcoded IPs (no DNS needed to
     * reach the DoH endpoint itself), breaking the chicken-and-egg
     * problem.
     */
    fun createDoHResolver(context: Context): DnsOverHttps {
        val cacheDir = File(context.cacheDir, "okhttp-dns-cache")
        val dnsCache = Cache(cacheDir, 5L * 1024 * 1024) // 5 MB

        val bootstrapClient = OkHttpClient.Builder()
            .cache(dnsCache)
            .build()

        return DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://1.1.1.1/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                // Hardcoded IPs — no system DNS needed to reach Cloudflare
                InetAddress.getByName("1.1.1.1"),
                InetAddress.getByName("1.0.0.1"),
                // Google DoH IPs as secondary bootstrap
                InetAddress.getByName("8.8.8.8"),
                InetAddress.getByName("8.8.4.4")
            )
            .build()
    }
}
```

**3. Modify `TmdbApiClient.kt`** — inject DoH into the shared OkHttpClient:

```kotlin
// BEFORE (current):
private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

// AFTER:
private var okHttpClient: OkHttpClient = buildDefaultClient()

private fun buildDefaultClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
}

/**
 * Initializes the OkHttpClient with DNS-over-HTTPS.
 * Call once from Application.onCreate() or lazily on first use.
 */
fun initWithDoH(context: Context) {
    val dohResolver = TmdbDnsHelper.createDoHResolver(context)
    okHttpClient = OkHttpClient.Builder()
        .dns(dohResolver)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
}
```

**4. Initialize in `LXApplication.onCreate()`:**

```kotlin
override fun onCreate() {
    super.onCreate()
    instance = this
    NotificationHelper.initNotificationChannels(this)
    TmdbApiClient.initWithDoH(this)  // <-- add this
}
```

**5. Inject DoH into Coil's ImageLoader** in `LXApplication.newImageLoader()`:

```kotlin
override fun newImageLoader(): ImageLoader {
    val dohClient = OkHttpClient.Builder()
        .dns(TmdbDnsHelper.createDoHResolver(this))
        .build()

    return ImageLoader.Builder(this)
        .okHttpClient(dohClient)   // <-- use DoH-enabled client for image fetches
        .memoryCache { ... }       // existing config
        .diskCache { ... }         // existing config
        .respectCacheHeaders(false)
        .crossfade(true)
        .build()
}
```

> [!NOTE]
> Coil uses OkHttp internally. By passing a custom `OkHttpClient` with our DoH resolver via `.okHttpClient(...)`, all image loads from `image.tmdb.org` will also bypass ISP DNS poisoning.

---

### Layer 2 — Alternative API Subdomain Fallback

TMDB provides an alternative API endpoint at `api.tmdb.org` (without "the movie" in the name). Both `api.themoviedb.org` and `api.tmdb.org` point to the same infrastructure and return identical responses. Many ISP block lists only target the `themoviedb.org` domain.

#### Changes Required

**1. Add the fallback URL** to `TmdbApiClient.kt`:

```kotlin
const val DEFAULT_BASE_URL = "https://api.themoviedb.org/3"
const val FALLBACK_BASE_URL = "https://api.tmdb.org/3"          // <-- add
const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p"
```

**2. Add connection-failure retry with fallback** — wrap each network call to try the alternate domain on `ConnectException`, `SocketTimeoutException`, or `UnknownHostException`:

```kotlin
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Executes a TMDB operation with automatic fallback to the alternate
 * API domain if the primary domain is unreachable (ISP block).
 */
private inline fun <T> withFallback(
    primaryBaseUrl: String = DEFAULT_BASE_URL,
    operation: (baseUrl: String) -> Result<T>
): Result<T> {
    val result = operation(primaryBaseUrl)
    if (result.isFailure) {
        val ex = result.exceptionOrNull()
        if (ex is ConnectException || ex is SocketTimeoutException || ex is UnknownHostException) {
            Log.w(TAG, "Primary TMDB URL failed (${ex.javaClass.simpleName}), retrying with fallback domain")
            return operation(FALLBACK_BASE_URL)
        }
    }
    return result
}
```

**3. Apply to all public methods** — each existing method already accepts `baseUrl` as a parameter, so wrap their call sites:

```kotlin
// Example for fetchMovieDetails:
fun fetchMovieDetails(
    tmdbId: Long,
    context: Context? = null,
    baseUrl: String = DEFAULT_BASE_URL
): Result<TmdbMediaDetails> = withFallback(baseUrl) { url ->
    fetchMovieDetailsInternal(tmdbId, context, url)
}

private fun fetchMovieDetailsInternal(
    tmdbId: Long,
    context: Context?,
    baseUrl: String
): Result<TmdbMediaDetails> {
    // ... existing implementation moves here unchanged ...
}
```

Repeat the same pattern for: `fetchTvDetails`, `searchMovieByTitle`, `searchTvByTitle`, `searchMoviesMultiple`, `searchTvMultiple`, `fetchRelatedEntries`.

> [!TIP]
> The `searchMediaDetailsByTitle` method already delegates to the individual search/fetch methods, so it will inherit the fallback behavior automatically.

---

### Layer 3 — Sticky Domain Preference (Optimization)

Once the app detects that the primary domain is blocked, avoid repeating the timeout penalty on every subsequent request by remembering which domain worked.

#### Changes Required

Add an in-memory preference to `TmdbApiClient`:

```kotlin
@Volatile
private var preferredBaseUrl: String = DEFAULT_BASE_URL

private inline fun <T> withFallback(
    operation: (baseUrl: String) -> Result<T>
): Result<T> {
    val result = operation(preferredBaseUrl)
    if (result.isFailure) {
        val ex = result.exceptionOrNull()
        if (ex is ConnectException || ex is SocketTimeoutException || ex is UnknownHostException) {
            val fallbackUrl = if (preferredBaseUrl == DEFAULT_BASE_URL) FALLBACK_BASE_URL else DEFAULT_BASE_URL
            Log.w(TAG, "TMDB domain $preferredBaseUrl unreachable, switching to $fallbackUrl")
            val fallbackResult = operation(fallbackUrl)
            if (fallbackResult.isSuccess) {
                preferredBaseUrl = fallbackUrl  // remember for future calls
            }
            return fallbackResult
        }
    }
    return result
}
```

> [!NOTE]
> This is an **in-memory** preference — it resets on app restart, ensuring the app periodically re-checks if the primary domain has been unblocked. This is intentional: ISP blocks can be temporary, and we don't want to permanently route through the fallback.

---

## Implementation Summary

### Files to Create

| File | Purpose |
|------|---------|
| `data/remote/TmdbDnsHelper.kt` | DNS-over-HTTPS resolver factory |

### Files to Modify

| File | Change |
|------|--------|
| `app/build.gradle.kts` | Add `okhttp-dnsoverhttps:4.12.0` dependency |
| `data/remote/TmdbApiClient.kt` | Add `FALLBACK_BASE_URL`, `initWithDoH()`, `withFallback()`, sticky domain preference, refactor methods to use fallback wrapper |
| `LXApplication.kt` | Call `TmdbApiClient.initWithDoH(this)` in `onCreate()`, pass DoH-enabled OkHttpClient to Coil |

### New Dependency

```
com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0
```

Same version as the existing OkHttp dependency — zero transitive dependency conflicts.

---

## How Each Layer Addresses the Block

```
ISP DNS Block (most common)
 └─→ Layer 1 (DoH) resolves the real IP via encrypted Cloudflare DNS ✅
 └─→ Layer 2 (api.tmdb.org) may not even be in the ISP blocklist    ✅

ISP blocks both domains by DNS
 └─→ Layer 1 (DoH) bypasses DNS entirely for both domains           ✅

DoH endpoint itself unreachable (rare, aggressive DPI)
 └─→ DoH falls back to Google DNS bootstrap IPs                     ✅
 └─→ Layer 2 alternate domain still attempted via system DNS         ✅

Block is transient / flaky
 └─→ Layer 3 (sticky preference) avoids repeated timeout penalty     ✅
 └─→ Resets on app restart to re-test primary domain                 ✅
```

---

## What This Does NOT Cover

- **SNI-based blocking**: If the ISP performs deep packet inspection and blocks based on the TLS Server Name Indication field, neither DoH nor an alternate domain will help (since the ISP sees `api.themoviedb.org` in the TLS ClientHello). This would require ECH (Encrypted Client Hello) which is not yet widely supported on Android. However, SNI blocking of TMDB in India has **not been reported** — all known cases are DNS-level.
- **Full IP-range blocking**: If the ISP blocks the entire IP range of TMDB's CDN (Fastly/Cloudflare), only a VPN or proxy would work. This is extremely unlikely for a metadata API.
- **Image CDN (`image.tmdb.org`)**: Layer 1 (DoH via Coil's OkHttpClient) covers this. Layer 2 (domain fallback) is not applicable for images since there is no alternate image domain. If `image.tmdb.org` is also DNS-blocked, DoH alone solves it.

---

## Testing Strategy

1. **Unit test**: Mock OkHttp DNS to simulate `UnknownHostException` on primary domain → verify fallback domain is tried and sticky preference updates.
2. **Integration test**: With a real device on a blocking ISP (e.g., Jio Fiber), verify:
   - Search works
   - Entity detail page loads metadata + poster image
   - Background sync completes
3. **Regression test**: On non-blocking networks, verify no latency increase from DoH (Cloudflare DoH adds ~2-5ms per DNS query, cached after first lookup).

