package com.lx.tracklist

import com.lx.tracklist.data.remote.AppUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun testCleanVersion() {
        assertEquals("0.1.6", AppUpdateChecker.cleanVersion("0.1.6"))
        assertEquals("0.1.6", AppUpdateChecker.cleanVersion("v0.1.6"))
        assertEquals("0.1.6", AppUpdateChecker.cleanVersion("V0.1.6"))
        assertEquals("0.1.6", AppUpdateChecker.cleanVersion("  v0.1.6  "))
    }

    @Test
    fun testIsVersionNewer() {
        // Newer patch version
        assertTrue(AppUpdateChecker.isVersionNewer(latest = "0.1.7", current = "0.1.6"))
        assertTrue(AppUpdateChecker.isVersionNewer(latest = "v0.1.7", current = "0.1.6"))
        assertTrue(AppUpdateChecker.isVersionNewer(latest = "v0.1.7", current = "v0.1.6"))

        // Newer minor/major version
        assertTrue(AppUpdateChecker.isVersionNewer(latest = "0.2.0", current = "0.1.6"))
        assertTrue(AppUpdateChecker.isVersionNewer(latest = "1.0.0", current = "0.1.6"))

        // Same version
        assertFalse(AppUpdateChecker.isVersionNewer(latest = "0.1.6", current = "0.1.6"))
        assertFalse(AppUpdateChecker.isVersionNewer(latest = "v0.1.6", current = "0.1.6"))
        assertFalse(AppUpdateChecker.isVersionNewer(latest = "0.1.6", current = "v0.1.6"))

        // Older version
        assertFalse(AppUpdateChecker.isVersionNewer(latest = "0.1.5", current = "0.1.6"))
        assertFalse(AppUpdateChecker.isVersionNewer(latest = "0.1.0", current = "0.1.6"))
        assertFalse(AppUpdateChecker.isVersionNewer(latest = "v0.0.9", current = "0.1.6"))

        // Four-part version comparison
        assertTrue(AppUpdateChecker.isVersionNewer(latest = "0.1.6.1", current = "0.1.6"))
        assertFalse(AppUpdateChecker.isVersionNewer(latest = "0.1.6", current = "0.1.6.1"))

        // Pre-release tag handling
        assertTrue(AppUpdateChecker.isVersionNewer(latest = "0.1.7-beta1", current = "0.1.6"))
    }
}

