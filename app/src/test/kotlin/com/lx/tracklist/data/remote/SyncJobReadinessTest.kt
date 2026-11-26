package com.lx.tracklist.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncJobReadinessTest {

    @Test
    fun `JOB-02 game and TMDB badges report KEY NEEDED when keys are missing and READY when present`() {
        assertEquals(SyncJobBadgeState.KEY_NEEDED, SyncJobReadiness.gameBadge(isRunning = false, isIgdbConfigured = false, isRawgConfigured = false))
        assertEquals(SyncJobBadgeState.READY, SyncJobReadiness.gameBadge(isRunning = false, isIgdbConfigured = true, isRawgConfigured = false))
        assertEquals(SyncJobBadgeState.READY, SyncJobReadiness.gameBadge(isRunning = false, isIgdbConfigured = false, isRawgConfigured = true))
        assertEquals(SyncJobBadgeState.SYNCING, SyncJobReadiness.gameBadge(isRunning = true, isIgdbConfigured = false, isRawgConfigured = false))

        assertEquals(SyncJobBadgeState.KEY_NEEDED, SyncJobReadiness.tmdbBadge(isRunning = false, isTmdbConfigured = false))
        assertEquals(SyncJobBadgeState.READY, SyncJobReadiness.tmdbBadge(isRunning = false, isTmdbConfigured = true))
        assertEquals(SyncJobBadgeState.SYNCING, SyncJobReadiness.tmdbBadge(isRunning = true, isTmdbConfigured = true))
    }

    @Test
    fun `JOB-02 keyless jobs always report READY when idle and main menu includes Resolve IDs and MAL pull`() {
        assertEquals(SyncJobBadgeState.READY, SyncJobReadiness.jikanBadge(isRunning = false))
        assertEquals(SyncJobBadgeState.SYNCING, SyncJobReadiness.jikanBadge(isRunning = true))
        assertEquals(SyncJobBadgeState.READY, SyncJobReadiness.bookBadge(isRunning = false))
        assertEquals(SyncJobBadgeState.READY, SyncJobReadiness.musicBadge(isRunning = false))
        assertEquals(SyncJobBadgeState.READY, SyncJobReadiness.resolveIdsBadge(isRunning = false))
        assertEquals(SyncJobBadgeState.RESOLVING, SyncJobReadiness.resolveIdsBadge(isRunning = true))

        val idle = SyncJobReadiness.mainMenuSummary(
            isJikanRunning = false,
            isTmdbRunning = false,
            isBookRunning = false,
            isGameRunning = false,
            isMusicRunning = false,
            isResolveIdsRunning = false,
            isMalPullRunning = false
        )
        assertEquals("READY", idle.badgeText)
        assertFalse(idle.isActive)

        val resolving = SyncJobReadiness.mainMenuSummary(
            isJikanRunning = false,
            isTmdbRunning = false,
            isBookRunning = false,
            isGameRunning = false,
            isMusicRunning = false,
            isResolveIdsRunning = true,
            isMalPullRunning = false
        )
        assertEquals("RESOLVING", resolving.badgeText)
        assertTrue(resolving.isActive)

        val malPulling = SyncJobReadiness.mainMenuSummary(
            isJikanRunning = false,
            isTmdbRunning = false,
            isBookRunning = false,
            isGameRunning = false,
            isMusicRunning = false,
            isResolveIdsRunning = false,
            isMalPullRunning = true
        )
        assertEquals("SYNCING", malPulling.badgeText)
        assertTrue(malPulling.isActive)
    }
}
