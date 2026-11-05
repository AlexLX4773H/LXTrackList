package com.lx.tracklist

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.lx.tracklist.data.local.CoverCacheManager
import com.lx.tracklist.data.local.LXTrackListDatabase
import com.lx.tracklist.data.repository.MediaRepository
import com.lx.tracklist.util.NotificationHelper

class LXApplication : Application(), ImageLoaderFactory {

    val database by lazy { LXTrackListDatabase.getDatabase(this) }
    val repository by lazy {
        MediaRepository(
            dao = database.mediaEntryDao(),
            userProfileDao = database.userProfileDao(),
            customCategoryDao = database.customCategoryDao(),
            subCategoryDao = database.subCategoryDao(),
            context = applicationContext
        )
    }

    companion object {
        lateinit var instance: LXApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.initNotificationChannels(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(CoverCacheManager.getCacheDir(this))
                    .maxSizeBytes(512L * 1024 * 1024) // 512 MB persistent cover cache
                    .build()
            }
            .respectCacheHeaders(false) // Keep cached images indefinitely regardless of HTTP Cache-Control headers
            .crossfade(true)
            .build()
    }
}
