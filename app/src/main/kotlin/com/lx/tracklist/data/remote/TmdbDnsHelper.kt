package com.lx.tracklist.data.remote

import android.content.Context
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.io.File
import java.net.InetAddress

object TmdbDnsHelper {

    @Volatile
    private var dohResolver: DnsOverHttps? = null

    /**
     * Builds or returns a singleton DnsOverHttps resolver that sends DNS queries to
     * Cloudflare's 1.1.1.1 over HTTPS.
     *
     * The bootstrap client uses hardcoded IPs (no DNS needed to
     * reach the DoH endpoint itself), breaking the chicken-and-egg
     * problem.
     *
     * Reusing the resolver avoids conflicting Cache directory locks on disk.
     */
    fun createDoHResolver(context: Context): DnsOverHttps {
        return dohResolver ?: synchronized(this) {
            dohResolver ?: buildResolver(context.applicationContext).also {
                dohResolver = it
            }
        }
    }

    private fun buildResolver(context: Context): DnsOverHttps {
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

