package com.lx.tracklist.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.lx.tracklist.data.remote.ApiKeyState
import com.lx.tracklist.data.remote.GameApiConfig
import com.lx.tracklist.data.remote.GoogleBooksConfig
import com.lx.tracklist.data.remote.IgdbApiClient
import com.lx.tracklist.data.remote.RawgApiClient
import com.lx.tracklist.data.remote.SteamGridDbApiClient
import com.lx.tracklist.data.remote.TmdbConfig
import com.lx.tracklist.ui.components.LXBadge
import com.lx.tracklist.ui.components.LXCard
import com.lx.tracklist.ui.theme.LocalLXColors
import kotlinx.coroutines.launch

/**
 * Renders the **API Keys** sub-section under Settings -> External Sync Jobs for entering optional
 * device-local overrides when building without bundled `.env` credentials.
 */
@Composable
fun SyncJobsApiKeysSection(
    onApiKeysChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalLXColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "API KEYS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 11.sp
            ),
            color = colors.primary,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
        )

        MediaAndBooksApiKeysCard(onApiKeysChanged = onApiKeysChanged)

        GameSettingsCard(onApiKeysChanged = onApiKeysChanged)
    }
}

@Composable
private fun MediaAndBooksApiKeysCard(
    onApiKeysChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalLXColors.current

    var revision by remember { mutableIntStateOf(0) }
    var tmdbDraft by remember { mutableStateOf(TmdbConfig.getCustomAccessToken(context)) }
    var booksDraft by remember { mutableStateOf(GoogleBooksConfig.getCustomApiKey(context)) }

    var tmdbFeedback by remember { mutableStateOf<String?>(null) }
    var booksFeedback by remember { mutableStateOf<String?>(null) }

    val tmdbState = remember(revision) { TmdbConfig.getAccessTokenState(context) }
    val booksState = remember(revision) { GoogleBooksConfig.getApiKeyState(context) }

    LXCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Movies, TV & Books API Keys",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                LXBadge(
                    text = if (tmdbState.isConfigured) "TMDB READY" else "TMDB MISSING",
                    color = if (tmdbState.isConfigured) colors.primary else colors.textSecondary
                )
            }

            Text(
                text = "Configure your TMDB Read Access Token (required for Movies & TV search/sync) and an optional Google Books API key for higher book lookup quota.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            // Attribution Notice matching job card size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = TmdbConfig.ATTRIBUTION_TEXT,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textSecondary
                )
            }

            // TMDB Read Access Token
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ApiKeyInputField(
                    label = "TMDB Read Access Token",
                    state = tmdbState,
                    value = tmdbDraft,
                    onValueChange = {
                        tmdbDraft = it
                        tmdbFeedback = null
                    },
                    isSecret = true,
                    accentColor = colors.primary
                )

                KeySaveClearRow(
                    hasCustomOverride = tmdbState == ApiKeyState.CUSTOM,
                    feedback = tmdbFeedback,
                    onSave = {
                        TmdbConfig.setCustomAccessToken(context, tmdbDraft)
                        tmdbDraft = TmdbConfig.getCustomAccessToken(context)
                        revision++
                        onApiKeysChanged()
                        tmdbFeedback = if (tmdbDraft.isBlank()) "Cleared custom TMDB token" else "Saved TMDB token"
                    },
                    onClear = {
                        TmdbConfig.setCustomAccessToken(context, null)
                        tmdbDraft = ""
                        revision++
                        onApiKeysChanged()
                        tmdbFeedback = "Reverted custom TMDB token"
                    }
                )
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.6f))

            // Google Books API Key
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ApiKeyInputField(
                    label = "Google Books API Key (Optional)",
                    state = booksState,
                    value = booksDraft,
                    onValueChange = {
                        booksDraft = it
                        booksFeedback = null
                    },
                    isSecret = true,
                    accentColor = colors.info
                )

                KeySaveClearRow(
                    hasCustomOverride = booksState == ApiKeyState.CUSTOM,
                    feedback = booksFeedback,
                    onSave = {
                        GoogleBooksConfig.setCustomApiKey(context, booksDraft)
                        booksDraft = GoogleBooksConfig.getCustomApiKey(context)
                        revision++
                        onApiKeysChanged()
                        booksFeedback = if (booksDraft.isBlank()) "Cleared custom Google Books key" else "Saved Google Books key"
                    },
                    onClear = {
                        GoogleBooksConfig.setCustomApiKey(context, null)
                        booksDraft = ""
                        revision++
                        onApiKeysChanged()
                        booksFeedback = "Reverted custom Google Books key"
                    }
                )
            }
        }
    }
}

@Composable
fun GameSettingsCard(
    onApiKeysChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = LocalLXColors.current

    var revision by remember { mutableIntStateOf(0) }
    var igdbClientId by remember { mutableStateOf(GameApiConfig.getCustomIgdbClientId(context)) }
    var igdbClientSecret by remember { mutableStateOf(GameApiConfig.getCustomIgdbClientSecret(context)) }
    var rawgApiKey by remember { mutableStateOf(GameApiConfig.getCustomRawgApiKey(context)) }
    var steamGridDbApiKey by remember { mutableStateOf(GameApiConfig.getCustomSteamGridDbApiKey(context)) }

    // Test connection & feedback states
    var isTestingIgdb by remember { mutableStateOf(false) }
    var igdbTestResult by remember { mutableStateOf<String?>(null) }
    var isIgdbTestSuccess by remember { mutableStateOf(false) }

    var isTestingRawg by remember { mutableStateOf(false) }
    var rawgTestResult by remember { mutableStateOf<String?>(null) }
    var isRawgTestSuccess by remember { mutableStateOf(false) }

    var isTestingSteamGrid by remember { mutableStateOf(false) }
    var steamGridTestResult by remember { mutableStateOf<String?>(null) }
    var isSteamGridTestSuccess by remember { mutableStateOf(false) }

    val igdbClientIdState = remember(revision) { GameApiConfig.getIgdbClientIdState(context) }
    val igdbClientSecretState = remember(revision) { GameApiConfig.getIgdbClientSecretState(context) }
    val rawgKeyState = remember(revision) { GameApiConfig.getRawgApiKeyState(context) }
    val steamGridKeyState = remember(revision) { GameApiConfig.getSteamGridDbApiKeyState(context) }

    val isIgdbConfigured = remember(revision) { GameApiConfig.isIgdbConfigured(context) }
    val isRawgConfigured = remember(revision) { GameApiConfig.isRawgConfigured(context) }

    fun saveIgdbDrafts() {
        GameApiConfig.setCustomIgdbCredentials(context, igdbClientId, igdbClientSecret)
        igdbClientId = GameApiConfig.getCustomIgdbClientId(context)
        igdbClientSecret = GameApiConfig.getCustomIgdbClientSecret(context)
        revision++
        onApiKeysChanged()
    }

    fun saveRawgDraft() {
        GameApiConfig.setCustomRawgApiKey(context, rawgApiKey)
        rawgApiKey = GameApiConfig.getCustomRawgApiKey(context)
        revision++
        onApiKeysChanged()
    }

    fun saveSteamGridDraft() {
        GameApiConfig.setCustomSteamGridDbApiKey(context, steamGridDbApiKey)
        steamGridDbApiKey = GameApiConfig.getCustomSteamGridDbApiKey(context)
        revision++
        onApiKeysChanged()
    }

    LXCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row styled identically to job cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Games & Posters API Keys",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                when {
                    isIgdbConfigured -> LXBadge(text = "IGDB CONNECTED", color = colors.primary)
                    isRawgConfigured -> LXBadge(text = "RAWG CONNECTED", color = colors.info)
                    else -> LXBadge(text = "MISSING", color = colors.textSecondary)
                }
            }

            Text(
                text = "Credentials used for Video Games metadata & search (IGDB via Twitch OAuth and RAWG) plus custom 600×900 vertical posters (SteamGridDB).",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            // Attributions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${GameApiConfig.IGDB_ATTRIBUTION_TEXT} • ${GameApiConfig.RAWG_ATTRIBUTION_TEXT}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = colors.textSecondary
                )
            }

            // Section 1: IGDB Configuration
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "IGDB v4 Credentials (Twitch Developer API)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primary
                )

                ApiKeyInputField(
                    label = "IGDB Client ID",
                    state = igdbClientIdState,
                    value = igdbClientId,
                    onValueChange = {
                        igdbClientId = it
                        igdbTestResult = null
                    },
                    isSecret = false,
                    accentColor = colors.primary
                )

                ApiKeyInputField(
                    label = "IGDB Client Secret",
                    state = igdbClientSecretState,
                    value = igdbClientSecret,
                    onValueChange = {
                        igdbClientSecret = it
                        igdbTestResult = null
                    },
                    isSecret = true,
                    accentColor = colors.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            saveIgdbDrafts()
                            isIgdbTestSuccess = true
                            igdbTestResult = if (igdbClientId.isBlank() && igdbClientSecret.isBlank()) {
                                "Cleared custom IGDB credentials"
                            } else {
                                "Saved IGDB credentials"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onAccent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (igdbClientIdState == ApiKeyState.CUSTOM || igdbClientSecretState == ApiKeyState.CUSTOM) {
                        OutlinedButton(
                            onClick = {
                                igdbClientId = ""
                                igdbClientSecret = ""
                                saveIgdbDrafts()
                                isIgdbTestSuccess = true
                                igdbTestResult = "Reverted custom IGDB credentials"
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                            border = BorderStroke(1.dp, colors.error.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (igdbClientId.isNotBlank() || igdbClientSecret.isNotBlank()) {
                                saveIgdbDrafts()
                            }
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
                        enabled = !isTestingIgdb &&
                            (GameApiConfig.isIgdbConfigured(context) || (igdbClientId.isNotBlank() && igdbClientSecret.isNotBlank())),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isTestingIgdb) {
                            CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testing...", fontSize = 12.sp)
                        } else {
                            Text("Test", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (igdbTestResult != null) {
                    Text(
                        text = igdbTestResult.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isIgdbTestSuccess) colors.success else colors.error
                    )
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.6f))

            // Section 2: RAWG Configuration
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RAWG API Key (Fallback)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.info
                )

                ApiKeyInputField(
                    label = "RAWG API Key",
                    state = rawgKeyState,
                    value = rawgApiKey,
                    onValueChange = {
                        rawgApiKey = it
                        rawgTestResult = null
                    },
                    isSecret = true,
                    accentColor = colors.info
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            saveRawgDraft()
                            isRawgTestSuccess = true
                            rawgTestResult = if (rawgApiKey.isBlank()) "Cleared custom RAWG key" else "Saved RAWG key"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onAccent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (rawgKeyState == ApiKeyState.CUSTOM) {
                        OutlinedButton(
                            onClick = {
                                rawgApiKey = ""
                                saveRawgDraft()
                                isRawgTestSuccess = true
                                rawgTestResult = "Reverted custom RAWG key"
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                            border = BorderStroke(1.dp, colors.error.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (rawgApiKey.isNotBlank()) saveRawgDraft()
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
                        enabled = !isTestingRawg && (rawgKeyState.isConfigured || rawgApiKey.isNotBlank()),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.info),
                        border = BorderStroke(1.dp, colors.info.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isTestingRawg) {
                            CircularProgressIndicator(color = colors.info, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testing...", fontSize = 12.sp)
                        } else {
                            Text("Test", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (rawgTestResult != null) {
                    Text(
                        text = rawgTestResult.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isRawgTestSuccess) colors.success else colors.error
                    )
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.6f))

            // Section 3: SteamGridDB Configuration
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SteamGridDB API Key (Optional Posters)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.scoreGold
                )

                ApiKeyInputField(
                    label = "SteamGridDB API Key",
                    state = steamGridKeyState,
                    value = steamGridDbApiKey,
                    onValueChange = {
                        steamGridDbApiKey = it
                        steamGridTestResult = null
                    },
                    isSecret = true,
                    accentColor = colors.scoreGold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            saveSteamGridDraft()
                            isSteamGridTestSuccess = true
                            steamGridTestResult = if (steamGridDbApiKey.isBlank()) {
                                "Cleared custom SteamGridDB key"
                            } else {
                                "Saved SteamGridDB key"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onAccent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (steamGridKeyState == ApiKeyState.CUSTOM) {
                        OutlinedButton(
                            onClick = {
                                steamGridDbApiKey = ""
                                saveSteamGridDraft()
                                isSteamGridTestSuccess = true
                                steamGridTestResult = "Reverted custom SteamGridDB key"
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                            border = BorderStroke(1.dp, colors.error.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (steamGridDbApiKey.isNotBlank()) saveSteamGridDraft()
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
                        enabled = !isTestingSteamGrid && (steamGridKeyState.isConfigured || steamGridDbApiKey.isNotBlank()),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.scoreGold),
                        border = BorderStroke(1.dp, colors.scoreGold.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isTestingSteamGrid) {
                            CircularProgressIndicator(color = colors.scoreGold, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testing...", fontSize = 12.sp)
                        } else {
                            Text("Test", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (steamGridTestResult != null) {
                    Text(
                        text = steamGridTestResult.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (isSteamGridTestSuccess) colors.success else colors.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeyStateBadge(state: ApiKeyState) {
    val colors = LocalLXColors.current
    val badgeColor = when (state) {
        ApiKeyState.CUSTOM -> colors.primary
        ApiKeyState.BUILT_IN -> colors.info
        ApiKeyState.MISSING -> colors.textSecondary
    }
    LXBadge(text = state.label.uppercase(), color = badgeColor)
}

@Composable
private fun ApiKeyInputField(
    label: String,
    state: ApiKeyState,
    value: String,
    onValueChange: (String) -> Unit,
    isSecret: Boolean,
    accentColor: Color
) {
    val colors = LocalLXColors.current
    var isSecretVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ApiKeyStateBadge(state)
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = if (state == ApiKeyState.BUILT_IN) {
                        "Using built-in key — enter a custom key to override"
                    } else {
                        "Enter $label"
                    },
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true,
            visualTransformation = if (isSecret && !isSecretVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = if (isSecret) {
                KeyboardOptions(keyboardType = KeyboardType.Password)
            } else {
                KeyboardOptions.Default
            },
            trailingIcon = if (isSecret) {
                {
                    IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                        Icon(
                            imageVector = if (isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isSecretVisible) "Hide secret" else "Show secret",
                            tint = colors.textSecondary
                        )
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun KeySaveClearRow(
    hasCustomOverride: Boolean,
    feedback: String?,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    val colors = LocalLXColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onAccent
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (hasCustomOverride) {
            OutlinedButton(
                onClick = onClear,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Clear", fontSize = 12.sp)
            }
        }

        if (feedback != null) {
            Text(
                text = feedback,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = colors.success,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
