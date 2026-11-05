package com.lx.tracklist.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lx.tracklist.MainActivity
import com.lx.tracklist.R

object NotificationHelper {

    const val CHANNEL_IMPORT_EXPORT = "channel_import_export"
    const val CHANNEL_JIKAN_SYNC = "jikan_sync_channel"

    const val NOTIFICATION_ID_IMPORT = 47010
    const val NOTIFICATION_ID_JIKAN = 47007

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val importChannel = NotificationChannel(
                CHANNEL_IMPORT_EXPORT,
                "Import & Export",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows progress and completion for backup file imports and exports"
            }

            val jikanChannel = NotificationChannel(
                CHANNEL_JIKAN_SYNC,
                "MyAnimeList (Jikan) Sync",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows real-time progress and completion when syncing posters and metadata from MyAnimeList"
            }

            manager.createNotificationChannel(importChannel)
            manager.createNotificationChannel(jikanChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun getMainActivityPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    private fun getLargeIcon(context: Context) = try {
        BitmapFactory.decodeResource(context.resources, R.drawable.lx_logo)
    } catch (_: Exception) {
        null
    }

    fun cleanSourceName(sourceName: String): String {
        val trimmed = sourceName.trim()
        val afterSlash = trimmed.substringAfterLast('/')
        return afterSlash.substringAfterLast(':').ifBlank { trimmed }
    }

    fun showImportProgress(context: Context, sourceName: String) {
        if (!hasNotificationPermission(context)) return

        try {
            val shortName = cleanSourceName(sourceName)
            val pendingIntent = getMainActivityPendingIntent(context)
            val builder = NotificationCompat.Builder(context, CHANNEL_IMPORT_EXPORT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Importing $shortName")
                .setContentText("Reading and importing entries into library...")
                .setProgress(0, 0, true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            getLargeIcon(context)?.let { builder.setLargeIcon(it) }

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_IMPORT, builder.build())
        } catch (_: Exception) {}
    }

    fun showImportComplete(
        context: Context,
        sourceName: String,
        totalCount: Int,
        addedCount: Int,
        updatedCount: Int
    ) {
        if (!hasNotificationPermission(context)) return

        try {
            val shortName = cleanSourceName(sourceName)
            val pendingIntent = getMainActivityPendingIntent(context)
            val summaryText = "Imported $totalCount entries ($addedCount added, $updatedCount updated)"
            val expandedText = "Import completed successfully from $shortName\n• Total: $totalCount entries\n• Added: $addedCount new\n• Updated: $updatedCount existing"

            val builder = NotificationCompat.Builder(context, CHANNEL_IMPORT_EXPORT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Import Completed")
                .setContentText(summaryText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            getLargeIcon(context)?.let { builder.setLargeIcon(it) }

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_IMPORT, builder.build())
        } catch (_: Exception) {}
    }

    fun showImportFailed(context: Context, sourceName: String, errorMessage: String) {
        if (!hasNotificationPermission(context)) return

        try {
            val shortName = cleanSourceName(sourceName)
            val pendingIntent = getMainActivityPendingIntent(context)
            val builder = NotificationCompat.Builder(context, CHANNEL_IMPORT_EXPORT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Import Failed")
                .setContentText("Failed to import $shortName: $errorMessage")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Failed to import $shortName\n$errorMessage"))
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            getLargeIcon(context)?.let { builder.setLargeIcon(it) }

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_IMPORT, builder.build())
        } catch (_: Exception) {}
    }

    fun showJikanSyncProgress(
        context: Context,
        completed: Int,
        total: Int,
        currentTitle: String
    ) {
        if (!hasNotificationPermission(context)) return

        try {
            val pendingIntent = getMainActivityPendingIntent(context)
            val text = if (currentTitle.isNotBlank()) "Fetching: $currentTitle" else "Updating MAL entries..."

            val builder = NotificationCompat.Builder(context, CHANNEL_JIKAN_SYNC)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Syncing MAL Metadata ($completed/$total)")
                .setContentText(text)
                .setProgress(total, completed, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            getLargeIcon(context)?.let { builder.setLargeIcon(it) }

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_JIKAN, builder.build())
        } catch (_: Exception) {}
    }

    fun showJikanSyncComplete(
        context: Context,
        completed: Int,
        total: Int,
        updated: Int,
        failed: Int,
        lastError: String?,
        isCancelled: Boolean
    ) {
        if (!hasNotificationPermission(context)) return

        try {
            val pendingIntent = getMainActivityPendingIntent(context)
            val title: String
            val content: String
            val expanded: String

            if (isCancelled) {
                title = "MAL Sync Cancelled"
                content = "$completed / $total completed ($updated updated)"
                expanded = "Metadata sync was cancelled by user.\n• Processed: $completed / $total\n• Updated: $updated entries"
            } else if (total == 0) {
                title = "MAL Sync"
                content = if (lastError != null) lastError else "All MAL entries are already up to date"
                expanded = if (lastError != null) lastError else "All entries with MyAnimeList source already have posters and details."
            } else if (failed == 0) {
                title = "MAL Sync Completed"
                content = "Updated $updated MAL entries with posters & details"
                expanded = "Successfully synced $total entries from MyAnimeList!\n• Updated: $updated entries with high-res posters & runtimes"
            } else {
                title = "MAL Sync Finished"
                content = "$updated updated, $failed failed"
                expanded = "Sync finished with issues:\n• Updated: $updated entries\n• Failed: $failed entries${if (!lastError.isNullOrBlank()) "\n• Note: $lastError" else ""}"
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_JIKAN_SYNC)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(expanded))
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            getLargeIcon(context)?.let { builder.setLargeIcon(it) }

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_JIKAN, builder.build())
        } catch (_: Exception) {}
    }
}
