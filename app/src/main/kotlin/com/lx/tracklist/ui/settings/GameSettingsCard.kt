package com.lx.tracklist.ui.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.data.remote.GameApiConfig
import com.lx.tracklist.data.remote.GameSyncProgress
import com.lx.tracklist.data.remote.IgdbApiClient
import com.lx.tracklist.data.remote.RawgApiClient
import com.lx.tracklist.data.remote.SteamGridDbApiClient
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXErrorRed
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXScoreGold
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import kotlinx.coroutines.launch

@Composable
fun GameSettingsCard(
    gameSyncState: GameSyncProgress,
    onStartSync: (forceRefresh: Boolean) -> Unit,
    onCancelSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var igdbClientId by remember { mutableStateOf(GameApiConfig.getIgdbClientId(context)) }
    var igdbClientSecret by remember { mutableStateOf(GameApiConfig.getIgdbClientSecret(context)) }
    var rawgApiKey by remember { mutableStateOf(GameApiConfig.getRawgApiKey(context)) }
    var steamGridDbApiKey by remember { mutableStateOf(GameApiConfig.getSteamGridDbApiKey(context)) }

    var isSecretVisible by remember { mutableStateOf(false) }

    // Test connection states
    var isTestingIgdb by remember { mutableStateOf(false) }
    var igdbTestResult by remember { mutableStateOf<String?>(null) }
    var isIgdbTestSuccess by remember { mutableStateOf(false) }

    var isTestingRawg by remember { mutableStateOf(false) }
    var rawgTestResult by remember { mutableStateOf<String?>(null) }
    var isRawgTestSuccess by remember { mutableStateOf(false) }

    var isTestingSteamGrid by remember { mutableStateOf(false) }
    var steamGridTestResult by remember { mutableStateOf<String?>(null) }
    var isSteamGridTestSuccess by remember { mutableStateOf(false) }

    val isIgdbConfigured = GameApiConfig.isIgdbConfigured(context)
    val isRawgConfigured = GameApiConfig.isRawgConfigured(context)
    val isSteamGridConfigured = GameApiConfig.isSteamGridDbConfigured(context)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LXSurfaceElevated)
            .border(1.dp, LXBorder, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = LXPrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Video Games Metadata & Posters",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (gameSyncState.isRunning) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(LXPrimaryGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SYNCING",
                        color = LXPrimaryGreen,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            } else if (isIgdbConfigured) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(LXPrimaryGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "IGDB CONNECTED",
                        color = LXPrimaryGreen,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            } else if (isRawgConfigured) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "RAWG CONNECTED",
                        color = Color(0xFF00E5FF),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(LXBorder.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "NOT CONFIGURED",
                        color = LXTextSecondary,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }
        }

        Text(
            text = "Fetches game metadata, covers, release dates, platforms, and franchise series from IGDB (Twitch OAuth) and RAWG. Custom 600×900 vertical posters powered by SteamGridDB.",
            style = MaterialTheme.typography.bodySmall,
            color = LXTextSecondary
        )

        // Attributions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(LXBackgroundBlack.copy(alpha = 0.5f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${GameApiConfig.IGDB_ATTRIBUTION_TEXT} • ${GameApiConfig.RAWG_ATTRIBUTION_TEXT}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = LXTextSecondary.copy(alpha = 0.8f)
            )
        }

        // Section 1: IGDB Configuration
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "IGDB v4 Credentials (Twitch Developer API)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = LXPrimaryGreen
            )

            OutlinedTextField(
                value = igdbClientId,
                onValueChange = {
                    igdbClientId = it
                    GameApiConfig.setCustomIgdbCredentials(context, it, igdbClientSecret)
                },
                label = { Text("IGDB Client ID", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LXPrimaryGreen,
                    unfocusedBorderColor = LXBorder,
                    focusedTextColor = LXTextPrimary,
                    unfocusedTextColor = LXTextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = igdbClientSecret,
                onValueChange = {
                    igdbClientSecret = it
                    GameApiConfig.setCustomIgdbCredentials(context, igdbClientId, it)
                },
                label = { Text("IGDB Client Secret", fontSize = 12.sp) },
                singleLine = true,
                visualTransformation = if (isSecretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                        Icon(
                            imageVector = if (isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isSecretVisible) "Hide secret" else "Show secret",
                            tint = LXTextSecondary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LXPrimaryGreen,
                    unfocusedBorderColor = LXBorder,
                    focusedTextColor = LXTextPrimary,
                    unfocusedTextColor = LXTextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        isTestingIgdb = true
                        igdbTestResult = null
                        coroutineScope.launch {
                            val res = IgdbApiClient.ensureValidToken(context, forceRefresh = true)
                            isTestingIgdb = false
                            if (res.isSuccess) {
                                isIgdbTestSuccess = true
                                igdbTestResult = "Connected! Twitch OAuth token active."
                            } else {
                                isIgdbTestSuccess = false
                                igdbTestResult = "Failed: ${res.exceptionOrNull()?.message ?: "Check credentials"}"
                            }
                        }
                    },
                    enabled = !isTestingIgdb && igdbClientId.isNotBlank() && igdbClientSecret.isNotBlank(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LXPrimaryGreen),
                    border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isTestingIgdb) {
                        CircularProgressIndicator(color = LXPrimaryGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Testing...", fontSize = 12.sp)
                    } else {
                        Text("Test IGDB Connection", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (igdbTestResult != null) {
                    Text(
                        text = igdbTestResult ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isIgdbTestSuccess) LXPrimaryGreen else LXErrorRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Section 2: RAWG Configuration
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "RAWG API Key (Optional Fallback)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF00E5FF)
            )

            OutlinedTextField(
                value = rawgApiKey,
                onValueChange = {
                    rawgApiKey = it
                    GameApiConfig.setCustomRawgApiKey(context, it)
                },
                label = { Text("RAWG API Key", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = LXBorder,
                    focusedTextColor = LXTextPrimary,
                    unfocusedTextColor = LXTextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        isTestingRawg = true
                        rawgTestResult = null
                        coroutineScope.launch {
                            val res = RawgApiClient.searchGames(context, "Zelda", limit = 1)
                            isTestingRawg = false
                            if (res.isSuccess) {
                                isRawgTestSuccess = true
                                rawgTestResult = "Connected! RAWG responding."
                            } else {
                                isRawgTestSuccess = false
                                rawgTestResult = "Failed: ${res.exceptionOrNull()?.message ?: "Check key"}"
                            }
                        }
                    },
                    enabled = !isTestingRawg && rawgApiKey.isNotBlank(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isTestingRawg) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Testing...", fontSize = 12.sp)
                    } else {
                        Text("Test RAWG Connection", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (rawgTestResult != null) {
                    Text(
                        text = rawgTestResult ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isRawgTestSuccess) LXPrimaryGreen else LXErrorRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Section 3: SteamGridDB Configuration
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "SteamGridDB API Key (Optional Posters)",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = LXScoreGold
            )

            OutlinedTextField(
                value = steamGridDbApiKey,
                onValueChange = {
                    steamGridDbApiKey = it
                    GameApiConfig.setCustomSteamGridDbApiKey(context, it)
                },
                label = { Text("SteamGridDB API Key", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LXScoreGold,
                    unfocusedBorderColor = LXBorder,
                    focusedTextColor = LXTextPrimary,
                    unfocusedTextColor = LXTextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        isTestingSteamGrid = true
                        steamGridTestResult = null
                        coroutineScope.launch {
                            val res = SteamGridDbApiClient.searchByTitle(context, "Portal")
                            isTestingSteamGrid = false
                            if (res.isSuccess) {
                                isSteamGridTestSuccess = true
                                steamGridTestResult = "Connected! SteamGridDB responding."
                            } else {
                                isSteamGridTestSuccess = false
                                steamGridTestResult = "Failed: ${res.exceptionOrNull()?.message ?: "Check key"}"
                            }
                        }
                    },
                    enabled = !isTestingSteamGrid && steamGridDbApiKey.isNotBlank(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LXScoreGold),
                    border = BorderStroke(1.dp, LXScoreGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isTestingSteamGrid) {
                        CircularProgressIndicator(color = LXScoreGold, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Testing...", fontSize = 12.sp)
                    } else {
                        Text("Test SteamGridDB Connection", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (steamGridTestResult != null) {
                    Text(
                        text = steamGridTestResult ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isSteamGridTestSuccess) LXPrimaryGreen else LXErrorRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Section 4: Sync Controls
        if (gameSyncState.isRunning) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LXBackgroundBlack.copy(alpha = 0.5f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (gameSyncState.totalCount > 0) {
                            "Progress (${gameSyncState.completedCount}/${gameSyncState.totalCount}) • ${gameSyncState.updatedCount} updated"
                        } else {
                            gameSyncState.statusMessage ?: "Preparing game sync..."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = LXTextPrimary
                    )
                    if (gameSyncState.totalCount > 0) {
                        Text(
                            text = "${(gameSyncState.progressFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = LXPrimaryGreen
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(LXBorder)
                ) {
                    if (gameSyncState.totalCount > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(gameSyncState.progressFraction)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(LXPrimaryGreen)
                        )
                    }
                }

                if (gameSyncState.currentTitle.isNotBlank()) {
                    Text(
                        text = "Current: ${gameSyncState.currentTitle}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = LXTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onCancelSync,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LXErrorRed),
                        border = BorderStroke(1.dp, LXErrorRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancel Sync", fontSize = 12.sp)
                    }
                }
            }
        } else {
            if (!gameSyncState.statusMessage.isNullOrBlank()) {
                Text(
                    text = gameSyncState.statusMessage ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = LXTextSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onStartSync(false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LXPrimaryGreen,
                        contentColor = LXBackgroundBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Games", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onStartSync(true) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LXPrimaryGreen),
                    border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Force Refresh", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
