package com.lx.tracklist.ui.components

import com.lx.tracklist.util.plural
import kotlin.math.ceil

/**
 * Snapshot of passkey verification attempts and active cooldown for a single profile.
 */
data class PasskeyLockoutState(
    val failedAttempts: Int = 0,
    val cooldownEndMillis: Long = 0L
)

/**
 * Result of recording a failed passkey attempt.
 */
data class PasskeyFailureResult(
    val failedAttempts: Int,
    val cooldownSecondsRemaining: Int,
    val errorMessage: String
)

/**
 * Pure, testable tracker for per-profile passkey attempts and 10-second cooldown lockouts.
 */
class PasskeyAttemptTracker(
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val maxAttemptsBeforeCooldown: Int = 3,
    private val cooldownDurationMillis: Long = 10_000L
) {
    private val states = mutableMapOf<String, PasskeyLockoutState>()

    /** Returns the current lockout state for [profileKey]. */
    @Synchronized
    fun stateFor(profileKey: String): PasskeyLockoutState {
        return states[profileKey] ?: PasskeyLockoutState()
    }

    /** Returns remaining cooldown seconds (0 if not currently locked out) for [profileKey]. */
    @Synchronized
    fun remainingCooldownSeconds(profileKey: String): Int {
        val endMillis = states[profileKey]?.cooldownEndMillis ?: 0L
        val remainingMillis = endMillis - nowProvider()
        if (remainingMillis <= 0L) return 0
        return ceil(remainingMillis / 1000.0).toInt().coerceAtLeast(1)
    }

    /** Clears failed attempts and cooldown state after a valid passkey entry. */
    @Synchronized
    fun recordSuccess(profileKey: String) {
        states.remove(profileKey)
    }

    /**
     * Records a failed passkey attempt for [profileKey] and triggers a cooldown every
     * [maxAttemptsBeforeCooldown] consecutive failures.
     */
    @Synchronized
    fun recordFailure(profileKey: String): PasskeyFailureResult {
        val current = states[profileKey] ?: PasskeyLockoutState()
        val activeCooldown = remainingCooldownSeconds(profileKey)
        if (activeCooldown > 0) {
            return PasskeyFailureResult(
                failedAttempts = current.failedAttempts,
                cooldownSecondsRemaining = activeCooldown,
                errorMessage = "Incorrect $maxAttemptsBeforeCooldown times. Locked for ${cooldownDurationMillis / 1000}s."
            )
        }

        val newFailed = current.failedAttempts + 1
        return if (newFailed % maxAttemptsBeforeCooldown == 0) {
            val cooldownEnd = nowProvider() + cooldownDurationMillis
            states[profileKey] = PasskeyLockoutState(failedAttempts = newFailed, cooldownEndMillis = cooldownEnd)
            val cooldownSecs = (cooldownDurationMillis / 1000L).toInt()
            PasskeyFailureResult(
                failedAttempts = newFailed,
                cooldownSecondsRemaining = cooldownSecs,
                errorMessage = "Incorrect $maxAttemptsBeforeCooldown times. Locked for ${cooldownSecs}s."
            )
        } else {
            states[profileKey] = PasskeyLockoutState(failedAttempts = newFailed, cooldownEndMillis = 0L)
            val remainingTries = maxAttemptsBeforeCooldown - (newFailed % maxAttemptsBeforeCooldown)
            PasskeyFailureResult(
                failedAttempts = newFailed,
                cooldownSecondsRemaining = 0,
                errorMessage = "Incorrect passkey (${plural(remainingTries, "try", "tries")} left before ${cooldownDurationMillis / 1000}s cooldown)"
            )
        }
    }
}

/** Process-wide store so passkey attempt counters and cooldowns persist across dialog reopenings. */
object PasskeyLockoutStore {
    val tracker = PasskeyAttemptTracker()

    fun keyFor(profileId: Long, profileName: String): String =
        if (profileId > 0L) "id:$profileId" else "name:$profileName"
}

/**
 * Determines whether tapping a profile row requires PIN verification.
 * When [requirePinForActive] is true (e.g. from the lock screen), any profile with a non-blank
 * passkey requires verification, including the currently active profile.
 */
fun shouldRequirePinForProfileSelection(
    passKey: String?,
    isActive: Boolean,
    requirePinForActive: Boolean
): Boolean {
    if (passKey.isNullOrBlank()) return false
    return requirePinForActive || !isActive
}
