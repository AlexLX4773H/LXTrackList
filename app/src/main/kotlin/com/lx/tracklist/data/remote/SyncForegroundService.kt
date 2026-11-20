package com.lx.tracklist.data.remote

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.lx.tracklist.LXApplication
import com.lx.tracklist.data.remote.mal.MalSyncManager
import com.lx.tracklist.data.remote.mal.MalSyncProgress
import com.lx.tracklist.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Foreground Service that keeps external sync operations (Jikan/MAL, TMDB, Books, Tachiyomi resolution, MAL account)
 * active when the app is in the background or when the screen is locked, preventing Android OS and OEM battery killers
 * from suspending network connections or killing the background process.
 */
class SyncForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "SyncForegroundService"

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
                   GameSyncManager.syncState.value.isRunning ||
                   TachiyomiResolverManager.resolveState.value.isRunning ||
                   MalSyncManager.syncState.value.isRunning
        }

        /**
         * Starts the foreground service if not already active.
         */
        fun start(context: Context) {
            try {
                val intent = Intent(context, SyncForegroundService::class.java).apply {
                    action = ACTION_START
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SyncForegroundService", e)
            }
        }

        /**
         * Explicitly stops the foreground service.
         */
        fun stop(context: Context? = null) {
            try {
                val ctx = context ?: LXApplication.instance
                val intent = Intent(ctx, SyncForegroundService::class.java).apply {
                    action = ACTION_STOP
                }
                ctx.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop SyncForegroundService", e)
            }
        }

        /**
         * Starts the foreground service only if at least one sync job is active.
         */
        fun checkAndStart(context: Context) {
            if (isAnySyncRunning()) {
                start(context)
            }
        }

        /**
         * Checks whether any sync is still active; if none are, stops the foreground service.
         */
        fun checkAndStop(context: Context? = null) {
            if (!isAnySyncRunning()) {
                stop(context)
            }
        }

        /**
         * Cancels all active syncs across all sync managers.
         */
        fun cancelAllSyncs(context: Context? = null) {
            try {
                val ctx = context ?: LXApplication.instance
                JikanSyncManager.cancelSync(ctx)
                TmdbSyncManager.cancelSync(ctx)
                BookSyncManager.cancelSync(ctx)
                GameSyncManager.cancelSync(ctx)
                TachiyomiResolverManager.cancelResolution(ctx)
                MalSyncManager.cancelSync()
                stop(ctx)
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling all syncs", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SyncForegroundService created")
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_CANCEL_ALL) {
            Log.d(TAG, "Cancelling all active sync operations via foreground notification action")
            cancelAllSyncs(this)
            stopForegroundAndFinish()
            return START_NOT_STICKY
        }

        if (action == ACTION_STOP) {
            Log.d(TAG, "Stopping SyncForegroundService via action")
            stopForegroundAndFinish()
            return START_NOT_STICKY
        }

        // Show initial foreground notification immediately to satisfy Android's 5-second requirement
        startForegroundNotification()

        // Start observing all sync state flows
        startObservingSyncStates()

        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        NotificationHelper.initNotificationChannels(this)
        val initialNotification = NotificationHelper.buildSyncServiceNotification(
            context = this,
            title = "Syncing Library Data in Background",
            content = "Updating media entries and posters...",
            indeterminate = true
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NotificationHelper.NOTIFICATION_ID_SYNC_SERVICE,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID_SYNC_SERVICE, initialNotification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service notification", e)
        }
    }

    private fun startObservingSyncStates() {
        observerJob?.cancel()
        observerJob = serviceScope.launch {
            combine(
                combine(JikanSyncManager.syncState, TmdbSyncManager.syncState, BookSyncManager.syncState) { j, t, b -> Triple(j, t, b) },
                combine(GameSyncManager.syncState, TachiyomiResolverManager.resolveState, MalSyncManager.syncState) { g, r, m -> Triple(g, r, m) }
            ) { (jikan, tmdb, book), (game, tachi, mal) ->
                SyncCombinedState(jikan, tmdb, book, game, tachi, mal)
            }.collect { combined ->
                if (!combined.isAnyRunning) {
                    // Small grace delay to avoid flickering between sequential jobs
                    delay(1200L)
                    if (!isAnySyncRunning()) {
                        Log.d(TAG, "All sync operations completed. Stopping foreground service.")
                        stopForegroundAndFinish()
                    }
                } else {
                    updateNotification(combined)
                }
            }
        }
    }

    private fun updateNotification(combined: SyncCombinedState) {
        val runningList = mutableListOf<String>()
        if (combined.jikan.isRunning) runningList.add("Anime")
        if (combined.tmdb.isRunning) runningList.add("Movies & TV")
        if (combined.book.isRunning) runningList.add("Books")
        if (combined.game.isRunning) runningList.add("Games")
        if (combined.tachi.isRunning) runningList.add("Tachiyomi IDs")
        if (combined.mal.isRunning) runningList.add("MAL Account")

        val title: String
        val content: String
        val progress: Int
        val maxProgress: Int
        val indeterminate: Boolean

        if (runningList.size == 1) {
            when {
                combined.jikan.isRunning -> {
                    val j = combined.jikan
                    title = "Syncing Anime Metadata (${j.completedCount}/${j.totalCount})"
                    content = if (j.currentTitle.isNotBlank()) "Fetching: ${j.currentTitle}" else j.statusMessage ?: "Updating MAL entries..."
                    progress = j.completedCount
                    maxProgress = j.totalCount
                    indeterminate = j.totalCount == 0
                }
                combined.tmdb.isRunning -> {
                    val t = combined.tmdb
                    title = "Syncing Movies & TV Shows (${t.completedCount}/${t.totalCount})"
                    content = if (t.currentTitle.isNotBlank()) "Fetching: ${t.currentTitle}" else t.statusMessage ?: "Updating TMDB entries..."
                    progress = t.completedCount
                    maxProgress = t.totalCount
                    indeterminate = t.totalCount == 0
                }
                combined.book.isRunning -> {
                    val b = combined.book
                    title = "Syncing Books & Novels (${b.completedCount}/${b.totalCount})"
                    content = if (b.currentTitle.isNotBlank()) "Fetching: ${b.currentTitle}" else b.statusMessage ?: "Updating book entries..."
                    progress = b.completedCount
                    maxProgress = b.totalCount
                    indeterminate = b.totalCount == 0
                }
                combined.game.isRunning -> {
                    val g = combined.game
                    title = "Syncing Video Games (${g.completedCount}/${g.totalCount})"
                    content = if (g.currentTitle.isNotBlank()) "Fetching: ${g.currentTitle}" else g.statusMessage ?: "Updating game entries..."
                    progress = g.completedCount
                    maxProgress = g.totalCount
                    indeterminate = g.totalCount == 0
                }
                combined.tachi.isRunning -> {
                    val r = combined.tachi
                    title = "Resolving IDs (${r.completedCount}/${r.totalCount})"
                    content = if (r.currentTitle.isNotBlank()) "Resolving: ${r.currentTitle}" else "Matching IDs across databases..."
                    progress = r.completedCount
                    maxProgress = r.totalCount
                    indeterminate = r.totalCount == 0
                }
                else -> {
                    val m = combined.mal
                    title = "Syncing External Tracking (${m.completedCount}/${m.totalCount})"
                    content = "Updating MyAnimeList account..."
                    progress = m.completedCount
                    maxProgress = m.totalCount
                    indeterminate = m.totalCount == 0
                }
            }
        } else {
            title = "Syncing ${runningList.size} Jobs in Background"
            val parts = mutableListOf<String>()
            var totalComp = 0
            var totalMax = 0
            if (combined.jikan.isRunning) {
                parts.add("MAL: ${combined.jikan.completedCount}/${combined.jikan.totalCount}")
                totalComp += combined.jikan.completedCount
                totalMax += combined.jikan.totalCount
            }
            if (combined.tmdb.isRunning) {
                parts.add("TMDB: ${combined.tmdb.completedCount}/${combined.tmdb.totalCount}")
                totalComp += combined.tmdb.completedCount
                totalMax += combined.tmdb.totalCount
            }
            if (combined.book.isRunning) {
                parts.add("Books: ${combined.book.completedCount}/${combined.book.totalCount}")
                totalComp += combined.book.completedCount
                totalMax += combined.book.totalCount
            }
            if (combined.game.isRunning) {
                parts.add("Games: ${combined.game.completedCount}/${combined.game.totalCount}")
                totalComp += combined.game.completedCount
                totalMax += combined.game.totalCount
            }
            if (combined.tachi.isRunning) {
                parts.add("IDs: ${combined.tachi.completedCount}/${combined.tachi.totalCount}")
                totalComp += combined.tachi.completedCount
                totalMax += combined.tachi.totalCount
            }
            if (combined.mal.isRunning) {
                parts.add("Account: ${combined.mal.completedCount}/${combined.mal.totalCount}")
                totalComp += combined.mal.completedCount
                totalMax += combined.mal.totalCount
            }
            content = parts.joinToString(" • ")
            progress = totalComp
            maxProgress = totalMax
            indeterminate = totalMax == 0
        }

        try {
            val updatedNotification = NotificationHelper.buildSyncServiceNotification(
                context = this,
                title = title,
                content = content,
                progress = progress,
                maxProgress = maxProgress,
                indeterminate = indeterminate
            )
            NotificationManagerCompat.from(this).notify(
                NotificationHelper.NOTIFICATION_ID_SYNC_SERVICE,
                updatedNotification
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating foreground sync notification", e)
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LXTrackList:SyncForegroundWakeLock")?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(30 * 60 * 1000L) // 30-minute safety limit
                Log.d(TAG, "Foreground service WakeLock acquired")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock in foreground service", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "Foreground service WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock in foreground service", e)
        }
    }

    private fun stopForegroundAndFinish() {
        try {
            releaseWakeLock()
            observerJob?.cancel()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping foreground service", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        serviceScope.cancel()
        Log.d(TAG, "SyncForegroundService destroyed")
    }

    private data class SyncCombinedState(
        val jikan: JikanSyncProgress,
        val tmdb: TmdbSyncProgress,
        val book: BookSyncProgress,
        val game: GameSyncProgress,
        val tachi: TachiyomiResolveProgress,
        val mal: MalSyncProgress
    ) {
        val isAnyRunning: Boolean
            get() = jikan.isRunning || tmdb.isRunning || book.isRunning || game.isRunning || tachi.isRunning || mal.isRunning
    }
}
