package com.lx.tracklist.data.remote

/**
 * Standardized status/readiness badge states rendered on External Sync Job cards.
 */
enum class SyncJobBadgeState(val label: String) {
    SYNCING("SYNCING"),
    RESOLVING("RESOLVING"),
    READY("READY"),
    KEY_NEEDED("KEY NEEDED")
}

/**
 * Aggregated badge state for the Settings main-menu "External Sync Jobs" row.
 */
data class ExternalSyncJobsSummary(
    val badgeText: String,
    val isActive: Boolean
)

/**
 * Pure readiness helpers mapping sync manager running flags and API key configuration
 * onto the canonical badge states shown in Settings -> External Sync Jobs.
 */
object SyncJobReadiness {

    const val MISSING_KEY_HINT = "Add an API key in Settings → API Keys"

    fun jikanBadge(isRunning: Boolean): SyncJobBadgeState =
        if (isRunning) SyncJobBadgeState.SYNCING else SyncJobBadgeState.READY

    fun tmdbBadge(isRunning: Boolean, isTmdbConfigured: Boolean): SyncJobBadgeState = when {
        isRunning -> SyncJobBadgeState.SYNCING
        isTmdbConfigured -> SyncJobBadgeState.READY
        else -> SyncJobBadgeState.KEY_NEEDED
    }

    fun bookBadge(isRunning: Boolean): SyncJobBadgeState =
        if (isRunning) SyncJobBadgeState.SYNCING else SyncJobBadgeState.READY

    fun gameBadge(
        isRunning: Boolean,
        isIgdbConfigured: Boolean,
        isRawgConfigured: Boolean
    ): SyncJobBadgeState = when {
        isRunning -> SyncJobBadgeState.SYNCING
        isIgdbConfigured || isRawgConfigured -> SyncJobBadgeState.READY
        else -> SyncJobBadgeState.KEY_NEEDED
    }

    fun musicBadge(isRunning: Boolean): SyncJobBadgeState =
        if (isRunning) SyncJobBadgeState.SYNCING else SyncJobBadgeState.READY

    fun resolveIdsBadge(isRunning: Boolean): SyncJobBadgeState =
        if (isRunning) SyncJobBadgeState.RESOLVING else SyncJobBadgeState.READY

    fun mainMenuSummary(
        isJikanRunning: Boolean,
        isTmdbRunning: Boolean,
        isBookRunning: Boolean,
        isGameRunning: Boolean,
        isMusicRunning: Boolean,
        isResolveIdsRunning: Boolean,
        isMalPullRunning: Boolean
    ): ExternalSyncJobsSummary {
        val anySyncRunning = isJikanRunning || isTmdbRunning || isBookRunning ||
            isGameRunning || isMusicRunning || isMalPullRunning
        return when {
            anySyncRunning -> ExternalSyncJobsSummary(badgeText = "SYNCING", isActive = true)
            isResolveIdsRunning -> ExternalSyncJobsSummary(badgeText = "RESOLVING", isActive = true)
            else -> ExternalSyncJobsSummary(badgeText = "READY", isActive = false)
        }
    }
}
