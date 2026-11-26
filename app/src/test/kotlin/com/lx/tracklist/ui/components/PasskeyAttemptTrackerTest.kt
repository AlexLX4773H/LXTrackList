package com.lx.tracklist.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasskeyAttemptTrackerTest {

    @Test
    fun `shouldRequirePinForProfileSelection blocks active bypass on lock screen`() {
        // Lock screen (requirePinForActive = true) requires PIN for active protected profile
        assertTrue(shouldRequirePinForProfileSelection("1234", isActive = true, requirePinForActive = true))
        assertTrue(shouldRequirePinForProfileSelection("1234", isActive = false, requirePinForActive = true))
        assertFalse(shouldRequirePinForProfileSelection(null, isActive = true, requirePinForActive = true))
        assertFalse(shouldRequirePinForProfileSelection("   ", isActive = true, requirePinForActive = true))

        // Unlocked switcher only prompts for non-active protected profiles
        assertFalse(shouldRequirePinForProfileSelection("1234", isActive = true, requirePinForActive = false))
        assertTrue(shouldRequirePinForProfileSelection("1234", isActive = false, requirePinForActive = false))
    }

    @Test
    fun `attempt tracker preserves failed count and cooldown across reopen`() {
        var now = 1_000_000L
        val tracker = PasskeyAttemptTracker(nowProvider = { now })
        val key = PasskeyLockoutStore.keyFor(profileId = 7L, profileName = "Locked")

        val fail1 = tracker.recordFailure(key)
        assertEquals(1, fail1.failedAttempts)
        assertEquals(0, fail1.cooldownSecondsRemaining)
        assertTrue(fail1.errorMessage.contains("2 tries left"))

        // Simulate closing and reopening dialog 2 seconds later
        now += 2_000L
        assertEquals(1, tracker.stateFor(key).failedAttempts)
        assertEquals(0, tracker.remainingCooldownSeconds(key))

        val fail2 = tracker.recordFailure(key)
        assertEquals(2, fail2.failedAttempts)
        assertTrue(fail2.errorMessage.contains("1 try left"))

        // 3rd failure triggers 10s cooldown
        val fail3 = tracker.recordFailure(key)
        assertEquals(3, fail3.failedAttempts)
        assertEquals(10, fail3.cooldownSecondsRemaining)

        // Reopen 4 seconds later -> 6 seconds cooldown still enforced
        now += 4_000L
        assertEquals(6, tracker.remainingCooldownSeconds(key))

        // After cooldown elapses, failedAttempts stays at 3 until success
        now += 7_000L
        assertEquals(0, tracker.remainingCooldownSeconds(key))
        assertEquals(3, tracker.stateFor(key).failedAttempts)

        // Different profile keeps independent state
        assertEquals(0, tracker.stateFor("id:99").failedAttempts)

        // Correct passkey resets state
        tracker.recordSuccess(key)
        assertEquals(0, tracker.stateFor(key).failedAttempts)
        assertEquals(0, tracker.remainingCooldownSeconds(key))
    }
}
