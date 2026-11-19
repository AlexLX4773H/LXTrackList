package com.lx.tracklist.data.remote

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.lx.tracklist.data.remote.mal.MalSyncManager

/**
 * Manages background CPU execution (via WakeLock) to keep sync operations
 * (Jikan/MAL, TMDB, Books, Tachiyomi resolution, MAL account) active when
 * the app is backgrounded or screen is turned off, without requiring dangerous
 * foreground service permissions that trigger Google Play Protect.
 */
object SyncForegroundService {

    private const val TAG = "SyncWakeLock"
    private var wakeLock: PowerManager.WakeLock? = null

    const val ACTION_START = "com.lx.tracklist.action.START_SYNC_SERVICE"
    const val ACTION_STOP = "com.lx.tracklist.action.STOP_SYNC_SERVICE"
    const val ACTION_CANCEL_ALL = "com.lx.tracklist.action.CANCEL_ALL_SYNCS"

    /**
     * Returns true if any metadata or external sync job is currently running.
     */
    fun isAnySyncRunning(): Boolean {
        return JikanSyncManager.syncState.value.isRunning ||
               TmdbSyncManager.syncState.value.isRunning ||
               BookSyncManager.syncState.value.isRunning ||
               TachiyomiResolverManager.resolveState.value.isRunning ||
               MalSyncManager.syncState.value.isRunning
    }

    /**
     * Starts background CPU wake lock for syncing.
     */
    fun start(context: Context) {
        acquireWakeLock(context)
    }

    /**
     * Explicitly stops/releases the wake lock.
     */
    fun stop(context: Context? = null) {
        releaseWakeLock()
    }

    /**
     * Starts wake lock only if at least one sync job is active.
     */
    fun checkAndStart(context: Context) {
        if (isAnySyncRunning()) {
            acquireWakeLock(context)
        }
    }

    /**
     * Releases wake lock if no sync jobs are running.
     */
    fun checkAndStop(context: Context? = null) {
        if (!isAnySyncRunning()) {
            releaseWakeLock()
        }
    }

    private fun acquireWakeLock(context: Context) {
        try {
            if (wakeLock == null) {
                val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LXTrackList:SyncWakeLock")?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(15 * 60 * 1000L) // 15-minute safety timeout
                Log.d(TAG, "Sync WakeLock acquired")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire sync wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "Sync WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release sync wake lock", e)
        }
    }

    fun cancelAllSyncs(context: Context? = null) {
        try {
            JikanSyncManager.cancelSync(context)
            TmdbSyncManager.cancelSync(context)
            BookSyncManager.cancelSync(context)
            TachiyomiResolverManager.cancelResolution(context)
            MalSyncManager.cancelSync()
            releaseWakeLock()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling all syncs", e)
        }
    }
}
