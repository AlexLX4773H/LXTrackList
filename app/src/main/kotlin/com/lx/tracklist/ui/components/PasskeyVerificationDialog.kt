package com.lx.tracklist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXErrorRed
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import kotlinx.coroutines.delay

@Composable
fun PasskeyVerificationDialog(
    profileName: String,
    expectedPasskey: String,
    profileId: Long = 0L,
    title: String = "Enter Passkey",
    subtitle: String = "Enter 4-digit passkey for \"$profileName\"",
    dismissButtonText: String = "Cancel",
    onSecondaryAction: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val lockoutKey = remember(profileId, profileName) {
        PasskeyLockoutStore.keyFor(profileId, profileName)
    }
    var enteredDigits by remember(lockoutKey) { mutableStateOf("") }
    var failedAttempts by remember(lockoutKey) {
        mutableIntStateOf(PasskeyLockoutStore.tracker.stateFor(lockoutKey).failedAttempts)
    }
    var cooldownSecondsRemaining by remember(lockoutKey) {
        mutableIntStateOf(PasskeyLockoutStore.tracker.remainingCooldownSeconds(lockoutKey))
    }
    var errorMessage by remember(lockoutKey) {
        val state = PasskeyLockoutStore.tracker.stateFor(lockoutKey)
        val remaining = PasskeyLockoutStore.tracker.remainingCooldownSeconds(lockoutKey)
        mutableStateOf(
            when {
                remaining > 0 -> "Incorrect 3 times. Locked for 10s."
                state.failedAttempts > 0 -> {
                    val left = 3 - (state.failedAttempts % 3)
                    "Incorrect passkey (${com.lx.tracklist.util.plural(left, "try", "tries")} left before 10s cooldown)"
                }
                else -> null
            }
        )
    }

    val isCooldownActive = cooldownSecondsRemaining > 0

    // 10s cooldown countdown timer synchronized with persisted end timestamp
    LaunchedEffect(lockoutKey, isCooldownActive) {
        if (isCooldownActive) {
            while (true) {
                val remaining = PasskeyLockoutStore.tracker.remainingCooldownSeconds(lockoutKey)
                cooldownSecondsRemaining = remaining
                if (remaining <= 0) break
                delay(250L)
            }
            errorMessage = null
        }
    }

    fun handleDigitPress(digit: Char) {
        if (isCooldownActive || enteredDigits.length >= 4) return
        val next = enteredDigits + digit
        enteredDigits = next
        errorMessage = null

        if (next.length == 4) {
            if (next == expectedPasskey) {
                PasskeyLockoutStore.tracker.recordSuccess(lockoutKey)
                onSuccess()
            } else {
                val failure = PasskeyLockoutStore.tracker.recordFailure(lockoutKey)
                failedAttempts = failure.failedAttempts
                cooldownSecondsRemaining = failure.cooldownSecondsRemaining
                enteredDigits = ""
                errorMessage = failure.errorMessage
            }
        }
    }

    fun handleBackspace() {
        if (isCooldownActive || enteredDigits.isEmpty()) return
        enteredDigits = enteredDigits.dropLast(1)
        errorMessage = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isCooldownActive) LXErrorRed else LXPrimaryGreen
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )
            }
        },
        text = {
            val isCompactHeight = LocalConfiguration.current.screenHeightDp <= 520
            val keySize = if (isCompactHeight) 44.dp else 54.dp
            val keypadRowSpacing = if (isCompactHeight) 6.dp else 10.dp
            val keypadColSpacing = if (isCompactHeight) 12.dp else 16.dp
            val topGap = if (isCompactHeight) 10.dp else 18.dp
            val midGap = if (isCompactHeight) 8.dp else 14.dp
            val digitTextSize = if (isCompactHeight) 18.sp else 20.sp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LXTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(topGap))

                // 4 PIN Dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredDigits.length
                        Box(
                            modifier = Modifier
                                .size(if (isCompactHeight) 14.dp else 18.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCooldownActive -> LXErrorRed.copy(alpha = 0.5f)
                                        isFilled -> LXPrimaryGreen
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = 2.dp,
                                    color = when {
                                        isCooldownActive -> LXErrorRed
                                        isFilled -> LXPrimaryGreen
                                        else -> LXBorder
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(midGap))

                // Error or Cooldown Message
                if (isCooldownActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = LXErrorRed, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Cooldown active: try again in ${cooldownSecondsRemaining}s",
                            color = LXErrorRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = LXErrorRed,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                } else if (!isCompactHeight) {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(midGap))

                // Custom 3x4 Number Keypad
                val keypadRows = listOf(
                    listOf('1', '2', '3'),
                    listOf('4', '5', '6'),
                    listOf('7', '8', '9'),
                    listOf('C', '0', 'B')
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(keypadRowSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    keypadRows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(keypadColSpacing)) {
                            row.forEach { key ->
                                when (key) {
                                    'C' -> {
                                        Box(
                                            modifier = Modifier
                                                .size(keySize)
                                                .clip(CircleShape)
                                                .clickable(enabled = !isCooldownActive && enteredDigits.isNotEmpty()) {
                                                    enteredDigits = ""
                                                    errorMessage = null
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "CLEAR",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (!isCooldownActive && enteredDigits.isNotEmpty()) LXTextSecondary else LXTextSecondary.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                    'B' -> {
                                        Box(
                                            modifier = Modifier
                                                .size(keySize)
                                                .clip(CircleShape)
                                                .clickable(enabled = !isCooldownActive && enteredDigits.isNotEmpty()) {
                                                    handleBackspace()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Backspace",
                                                tint = if (!isCooldownActive && enteredDigits.isNotEmpty()) LXPrimaryGreen else LXTextSecondary.copy(alpha = 0.3f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        Box(
                                            modifier = Modifier
                                                .size(keySize)
                                                .clip(CircleShape)
                                                .background(if (isCooldownActive) LXBackgroundBlack.copy(alpha = 0.4f) else LXSurfaceElevated)
                                                .border(1.dp, if (isCooldownActive) LXBorder.copy(alpha = 0.3f) else LXBorder, CircleShape)
                                                .clickable(enabled = !isCooldownActive) {
                                                    handleDigitPress(key)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = key.toString(),
                                                fontSize = digitTextSize,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCooldownActive) LXTextSecondary.copy(alpha = 0.4f) else LXTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (onSecondaryAction != null && secondaryActionText != null) {
                TextButton(onClick = onSecondaryAction) {
                    Text(secondaryActionText, color = LXPrimaryGreen, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissButtonText, color = LXTextSecondary)
            }
        },
        containerColor = LXBackgroundBlack,
        shape = RoundedCornerShape(16.dp)
    )
}

