package com.lx.tracklist

import android.app.Application
import com.lx.tracklist.data.local.LXTrackListDatabase
import com.lx.tracklist.data.repository.MediaRepository

class LXApplication : Application() {

    val database by lazy { LXTrackListDatabase.getDatabase(this) }
    val repository by lazy {
        MediaRepository(
            dao = database.mediaEntryDao(),
            userProfileDao = database.userProfileDao(),
            customCategoryDao = database.customCategoryDao(),
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
    }
}
