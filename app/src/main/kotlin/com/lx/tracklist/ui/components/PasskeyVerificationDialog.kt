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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    title: String = "Enter Passkey",
    subtitle: String = "Enter 4-digit passkey for \"$profileName\"",
    dismissButtonText: String = "Cancel",
    onSecondaryAction: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var enteredDigits by remember { mutableStateOf("") }
    var failedAttempts by remember { mutableIntStateOf(0) }
    var cooldownSecondsRemaining by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isCooldownActive = cooldownSecondsRemaining > 0

    // 10s cooldown countdown timer
    LaunchedEffect(isCooldownActive) {
        if (isCooldownActive) {
            while (cooldownSecondsRemaining > 0) {
                delay(1000L)
                cooldownSecondsRemaining--
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
                onSuccess()
            } else {
                val newFailed = failedAttempts + 1
                failedAttempts = newFailed
                enteredDigits = ""

                if (newFailed % 3 == 0) {
                    cooldownSecondsRemaining = 10
                    errorMessage = "Incorrect 3 times. Locked for 10s."
                } else {
                    val remainingTries = 3 - (newFailed % 3)
                    errorMessage = "Incorrect passkey ($remainingTries tries left before 10s cooldown)"
                }
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LXTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 4 PIN Dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredDigits.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
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

                Spacer(modifier = Modifier.height(14.dp))

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
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom 3x4 Number Keypad
                val keypadRows = listOf(
                    listOf('1', '2', '3'),
                    listOf('4', '5', '6'),
                    listOf('7', '8', '9'),
                    listOf('C', '0', 'B')
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    keypadRows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            row.forEach { key ->
                                when (key) {
                                    'C' -> {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
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
                                                .size(54.dp)
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
                                                .size(54.dp)
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
                                                fontSize = 20.sp,
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

