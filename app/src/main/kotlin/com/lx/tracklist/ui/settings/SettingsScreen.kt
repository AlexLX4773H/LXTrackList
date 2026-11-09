package com.lx.tracklist.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.lx.tracklist.data.remote.AppUpdateChecker
import com.lx.tracklist.data.remote.AppUpdateStatus
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import coil.compose.AsyncImage
import com.lx.tracklist.R
import com.lx.tracklist.data.local.StorageFileInfo
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.SubCategory
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.model.UserProfile
import com.lx.tracklist.data.remote.JikanSyncProgress
import com.lx.tracklist.data.remote.TmdbConfig
import com.lx.tracklist.data.remote.TmdbSyncProgress
import com.lx.tracklist.data.repository.ConflictStrategy
import com.lx.tracklist.ui.components.PasskeyVerificationDialog
import com.lx.tracklist.ui.components.UserAvatarButton
import com.lx.tracklist.ui.components.getCategoryIcon
import com.lx.tracklist.ui.settings.ExportDestination
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXErrorRed
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXScoreGold
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXSurfaceVariant
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary

enum class SettingsSubMenu(val title: String) {
    CATEGORIES("Custom Categories"),
    SYNC_JOBS("External Sync Jobs"),
    STORAGE("Backup & Storage"),
    CONFLICTS("Conflict Resolution"),
    EXPORT_GUIDES("Export Guides & Help"),
    STARTUP_VIEW("Default Startup View"),
    PREFERENCES("Gestures & Navigation"),
    SECURITY("Security & Passkey"),
    ABOUT("About")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onUserAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val jikanSyncState by viewModel.jikanSyncState.collectAsState()
    val tmdbSyncState by viewModel.tmdbSyncState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    var activeSubMenu by rememberSaveable { mutableStateOf<SettingsSubMenu?>(null) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CustomCategory?>(null) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryMediaType by remember { mutableStateOf<MediaType?>(MediaType.MANGA) }
    var newCategoryIsWatch by remember { mutableStateOf(false) }
    var newCategoryTrackingUnit by remember { mutableStateOf(TrackingUnit.CHAPTERS) }
    val colorOptions = listOf("#A7EF60", "#4DA8DA", "#BB86FC", "#FF7597", "#FFD700", "#00E5FF", "#FF8A65")
    var selectedColor by remember { mutableStateOf(colorOptions.first()) }

    LaunchedEffect(Unit) {
        viewModel.checkStorageStatus(context)
    }

    // Auto-dismiss status notification after 6 seconds
    LaunchedEffect(state.statusMessage) {
        if (state.statusMessage != null) {
            delay(6000L)
            viewModel.clearStatusMessage()
        }
    }

    // Clear notification when leaving the Settings screen so it does not persist across tabs
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearStatusMessage()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val ensureNotificationPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importFromUri(context, it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportPendingToCustomUri(context, it) }
    }

    // Add Custom Category Dialog
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddCategoryDialog = false
                newCategoryName = ""
            },
            title = {
                Text("New Custom Category", color = LXTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Category Name", style = MaterialTheme.typography.labelMedium, color = LXTextSecondary)
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("e.g. Donghua, Web Novels, Audiobooks...", color = LXTextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LXPrimaryGreen,
                            unfocusedBorderColor = LXBorder,
                            focusedTextColor = LXTextPrimary,
                            unfocusedTextColor = LXTextPrimary,
                            cursorColor = LXPrimaryGreen
                        )
                    )

                    Text("Tracking Mode", style = MaterialTheme.typography.labelMedium, color = LXTextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !newCategoryIsWatch,
                            onClick = { newCategoryIsWatch = false },
                            label = { Text("Reading (Chapters/Vol)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LXPrimaryGreen,
                                selectedLabelColor = LXBackgroundBlack,
                                containerColor = LXSurfaceElevated,
                                labelColor = LXTextSecondary
                            )
                        )
                        FilterChip(
                            selected = newCategoryIsWatch,
                            onClick = { newCategoryIsWatch = true },
                            label = { Text("Watching (Episodes)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LXPrimaryGreen,
                                selectedLabelColor = LXBackgroundBlack,
                                containerColor = LXSurfaceElevated,
                                labelColor = LXTextSecondary
                            )
                        )
                    }

                    if (!newCategoryIsWatch) {
                        Text("Reading Tracking Unit", style = MaterialTheme.typography.labelMedium, color = LXTextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TrackingUnit.entries.forEach { unit ->
                                FilterChip(
                                    selected = newCategoryTrackingUnit == unit,
                                    onClick = { newCategoryTrackingUnit = unit },
                                    label = { Text(unit.displayName, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LXPrimaryGreen,
                                        selectedLabelColor = LXBackgroundBlack,
                                        containerColor = LXSurfaceElevated,
                                        labelColor = LXTextSecondary
                                    )
                                )
                            }
                        }
                    }

                    Text("Badge Color", style = MaterialTheme.typography.labelMedium, color = LXTextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(colorOptions) { hex ->
                            val color = parseHexColor(hex)
                            val isChosen = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isChosen) 3.dp else 1.dp,
                                        color = if (isChosen) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChosen) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCustomCategory(
                                name = newCategoryName,
                                isWatchType = newCategoryIsWatch,
                                trackingUnit = newCategoryTrackingUnit,
                                colorHex = selectedColor
                            )
                            newCategoryName = ""
                            showAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Create Category", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddCategoryDialog = false
                    newCategoryName = ""
                }) {
                    Text("Cancel", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceElevated
        )
    }

    // Delete Custom Category Confirmation Dialog
    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category?", color = LXTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete custom category \"${categoryToDelete?.name}\"?",
                    color = LXTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        categoryToDelete?.let { viewModel.deleteCustomCategory(it) }
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LXErrorRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceElevated
        )
    }

    // Analyzing File Progress Dialog
    if (state.isPreparingImport) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = {
                Text(
                    text = "Analyzing File...",
                    color = LXTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = LXPrimaryGreen,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp
                    )
                    Text(
                        text = "Reading entries and detecting categories...",
                        color = LXTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            containerColor = LXSurfaceElevated
        )
    }

    // Import Preview and Confirmation Dialog
    val preview = state.importPreview
    if (preview != null) {
        AlertDialog(
            onDismissRequest = {
                if (!preview.isImporting) {
                    viewModel.cancelImportPreview()
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Confirm Import",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                        Text(
                            text = preview.sourceName,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LXTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total in file: ${preview.totalCount} items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LXTextSecondary
                                )
                                Text(
                                    text = "${preview.selectedCount} selected to import",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (preview.selectedCount > 0) LXPrimaryGreen else LXErrorRed
                                )
                            }

                            TextButton(
                                onClick = {
                                    if (preview.selectedCategoryKeys.size == preview.categoryGroups.size) {
                                        viewModel.deselectAllCategories()
                                    } else {
                                        viewModel.selectAllCategories()
                                    }
                                },
                                enabled = !preview.isImporting
                            ) {
                                Text(
                                    text = if (preview.selectedCategoryKeys.size == preview.categoryGroups.size) "Deselect All" else "Select All",
                                    color = LXPrimaryGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Tachiyomi Favorites Toggle Option
                    if (preview.isTachiyomiSource) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !preview.isImporting) {
                                        viewModel.setImportFavorites(!preview.importFavorites)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (preview.importFavorites) LXErrorRed.copy(alpha = 0.15f)
                                                else LXSurfaceElevated
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (preview.importFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            tint = if (preview.importFavorites) LXErrorRed else LXTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = "Import as Favorites",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = LXTextPrimary
                                        )
                                        Text(
                                            text = if (preview.importFavorites)
                                                "Entries will be starred as favorites"
                                            else
                                                "Import as normal items (not favorites)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = if (preview.importFavorites) LXPrimaryGreen else LXTextSecondary
                                        )
                                    }
                                }

                                Switch(
                                    checked = preview.importFavorites,
                                    onCheckedChange = { viewModel.setImportFavorites(it) },
                                    enabled = !preview.isImporting,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = LXBackgroundBlack,
                                        checkedTrackColor = LXPrimaryGreen,
                                        uncheckedThumbColor = LXTextSecondary,
                                        uncheckedTrackColor = LXSurfaceElevated
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        text = "Choose types / categories to import:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = LXTextSecondary
                    )

                    // Categories list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        preview.categoryGroups.forEach { group ->
                            val isChecked = preview.selectedCategoryKeys.contains(group.key)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChecked) LXPrimaryGreen.copy(alpha = 0.12f) else LXSurfaceElevated)
                                    .border(
                                        width = 1.dp,
                                        color = if (isChecked) LXPrimaryGreen.copy(alpha = 0.6f) else LXBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !preview.isImporting) {
                                        viewModel.toggleCategorySelection(group.key)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { viewModel.toggleCategorySelection(group.key) },
                                        enabled = !preview.isImporting,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = LXPrimaryGreen,
                                            checkmarkColor = LXBackgroundBlack,
                                            uncheckedColor = LXTextSecondary
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Icon(
                                        imageVector = when {
                                            group.isCustomCategory -> Icons.Default.Bookmark
                                            group.mediaType == MediaType.ANIME -> Icons.Default.LiveTv
                                            group.mediaType == MediaType.MANGA -> Icons.AutoMirrored.Filled.MenuBook
                                            group.mediaType == MediaType.MANHWA -> Icons.Default.PhoneAndroid
                                            group.mediaType == MediaType.MOVIE -> Icons.Default.Movie
                                            group.mediaType == MediaType.SHOW -> Icons.Default.Tv
                                            group.mediaType == MediaType.BOOK -> Icons.Default.Book
                                            else -> Icons.Default.Bookmark
                                        },
                                        contentDescription = null,
                                        tint = if (isChecked) LXPrimaryGreen else LXTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = group.displayName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = LXTextPrimary
                                            )

                                            if (group.isSubCategory) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(LXPrimaryGreen.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Sub Category",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                        color = LXPrimaryGreen
                                                    )
                                                }
                                            } else if (group.isCustomCategory) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(LXPrimaryGreen.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Custom Category",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                        color = LXPrimaryGreen
                                                    )
                                                }
                                            }
                                        }

                                        if (!group.parentCategoryName.isNullOrBlank()) {
                                            Text(
                                                text = "in ${group.parentCategoryName}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = LXTextSecondary
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LXSurfaceBlack)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${group.count} ${if (group.count == 1) "item" else "items"}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                        color = LXTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ensureNotificationPermission()
                        viewModel.confirmImport()
                    },
                    enabled = preview.selectedCount > 0 && !preview.isImporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LXPrimaryGreen,
                        contentColor = LXBackgroundBlack,
                        disabledContainerColor = LXSurfaceElevated,
                        disabledContentColor = LXTextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (preview.isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = LXBackgroundBlack,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importing...", fontWeight = FontWeight.Bold)
                    } else {
                        Text(
                            text = "Import (${preview.selectedCount})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelImportPreview() },
                    enabled = !preview.isImporting
                ) {
                    Text("Cancel", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceElevated
        )
    }

    // Preparing Export Progress Dialog
    if (state.isPreparingExport) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = {
                Text(
                    text = "Preparing Export...",
                    color = LXTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = LXPrimaryGreen,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp
                    )
                    Text(
                        text = "Loading library entries and categories...",
                        color = LXTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            containerColor = LXSurfaceElevated
        )
    }

    // Export Preview and Confirmation Dialog
    val exportPreview = state.exportPreview
    if (exportPreview != null) {
        AlertDialog(
            onDismissRequest = {
                if (!exportPreview.isExporting) {
                    viewModel.cancelExportPreview()
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Confirm Export",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                        Text(
                            text = "Choose types and categories to include in backup",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LXTextSecondary
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Export destination selection
                    Text(
                        text = "Export Destination:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = LXTextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = exportPreview.destination == ExportDestination.HOME_OUTPUT_FOLDER,
                            onClick = { viewModel.setExportDestination(ExportDestination.HOME_OUTPUT_FOLDER) },
                            enabled = state.hasStoragePermission && !exportPreview.isExporting,
                            label = { Text("Home /output", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LXPrimaryGreen,
                                selectedLabelColor = LXBackgroundBlack,
                                containerColor = LXSurfaceElevated,
                                labelColor = LXTextSecondary
                            )
                        )

                        FilterChip(
                            selected = exportPreview.destination == ExportDestination.CUSTOM_FILE,
                            onClick = { viewModel.setExportDestination(ExportDestination.CUSTOM_FILE) },
                            enabled = !exportPreview.isExporting,
                            label = { Text("Custom File (Save As...)", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LXPrimaryGreen,
                                selectedLabelColor = LXBackgroundBlack,
                                containerColor = LXSurfaceElevated,
                                labelColor = LXTextSecondary
                            )
                        )
                    }

                    // Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total in library: ${exportPreview.totalCount} items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LXTextSecondary
                                )
                                Text(
                                    text = "${exportPreview.selectedCount} selected to export",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (exportPreview.selectedCount > 0) LXPrimaryGreen else LXErrorRed
                                )
                            }

                            TextButton(
                                onClick = {
                                    if (exportPreview.selectedCategoryKeys.size == exportPreview.categoryGroups.size) {
                                        viewModel.deselectAllExportCategories()
                                    } else {
                                        viewModel.selectAllExportCategories()
                                    }
                                },
                                enabled = !exportPreview.isExporting
                            ) {
                                Text(
                                    text = if (exportPreview.selectedCategoryKeys.size == exportPreview.categoryGroups.size) "Deselect All" else "Select All",
                                    color = LXPrimaryGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "Categories to export:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = LXTextSecondary
                    )

                    // Categories checklist
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        exportPreview.categoryGroups.forEach { group ->
                            val isChecked = exportPreview.selectedCategoryKeys.contains(group.key)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChecked) LXPrimaryGreen.copy(alpha = 0.12f) else LXSurfaceElevated)
                                    .border(
                                        width = 1.dp,
                                        color = if (isChecked) LXPrimaryGreen.copy(alpha = 0.6f) else LXBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !exportPreview.isExporting) {
                                        viewModel.toggleExportCategorySelection(group.key)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { viewModel.toggleExportCategorySelection(group.key) },
                                        enabled = !exportPreview.isExporting,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = LXPrimaryGreen,
                                            checkmarkColor = LXBackgroundBlack,
                                            uncheckedColor = LXTextSecondary
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Icon(
                                        imageVector = when {
                                            group.isCustomCategory -> Icons.Default.Bookmark
                                            group.mediaType == MediaType.ANIME -> Icons.Default.LiveTv
                                            group.mediaType == MediaType.MANGA -> Icons.AutoMirrored.Filled.MenuBook
                                            group.mediaType == MediaType.MANHWA -> Icons.Default.PhoneAndroid
                                            group.mediaType == MediaType.MOVIE -> Icons.Default.Movie
                                            group.mediaType == MediaType.SHOW -> Icons.Default.Tv
                                            group.mediaType == MediaType.BOOK -> Icons.Default.Book
                                            else -> Icons.Default.Bookmark
                                        },
                                        contentDescription = null,
                                        tint = if (isChecked) LXPrimaryGreen else LXTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = group.displayName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = LXTextPrimary
                                            )

                                            if (group.isSubCategory) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(LXPrimaryGreen.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Sub Category",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                        color = LXPrimaryGreen
                                                    )
                                                }
                                            } else if (group.isCustomCategory) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(LXPrimaryGreen.copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Custom Category",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                        color = LXPrimaryGreen
                                                    )
                                                }
                                            }
                                        }

                                        if (!group.parentCategoryName.isNullOrBlank()) {
                                            Text(
                                                text = "in ${group.parentCategoryName}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = LXTextSecondary
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LXSurfaceBlack)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${group.count} ${if (group.count == 1) "item" else "items"}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                        color = LXTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exportPreview.destination == ExportDestination.HOME_OUTPUT_FOLDER) {
                            viewModel.confirmExportToHomeOutput(context)
                        } else {
                            viewModel.startCustomExport {
                                exportLauncher.launch("LX_Track_List_Backup_${System.currentTimeMillis()}.json")
                            }
                        }
                    },
                    enabled = exportPreview.selectedCount > 0 && !exportPreview.isExporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LXPrimaryGreen,
                        contentColor = LXBackgroundBlack,
                        disabledContainerColor = LXSurfaceElevated,
                        disabledContentColor = LXTextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (exportPreview.isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = LXBackgroundBlack,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporting...", fontWeight = FontWeight.Bold)
                    } else {
                        val actionLabel = if (exportPreview.destination == ExportDestination.HOME_OUTPUT_FOLDER) "Export" else "Save As..."
                        Text(
                            text = "$actionLabel (${exportPreview.selectedCount})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelExportPreview() },
                    enabled = !exportPreview.isExporting
                ) {
                    Text("Cancel", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceElevated
        )
    }

    BackHandler(enabled = !isTablet && activeSubMenu != null) {
        activeSubMenu = null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LXBackgroundBlack,
        contentWindowInsets = if (isTablet) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (!isTablet && activeSubMenu != null) activeSubMenu!!.title else "Settings & Storage",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                },
                navigationIcon = {
                    if (!isTablet && activeSubMenu != null) {
                        IconButton(onClick = { activeSubMenu = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = LXPrimaryGreen
                            )
                        }
                    } else {
                        UserAvatarButton(
                            activeUser = state.activeUser,
                            onClick = onUserAvatarClick
                        )
                    }
                },
                actions = {
                    if (!isTablet && activeSubMenu == SettingsSubMenu.CATEGORIES) {
                        IconButton(onClick = { showAddCategoryDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Category", tint = LXPrimaryGreen)
                        }
                    } else if (!isTablet && activeSubMenu == SettingsSubMenu.STORAGE) {
                        IconButton(onClick = {
                            viewModel.clearStatusMessage()
                            viewModel.checkStorageStatus(context)
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = LXPrimaryGreen)
                        }
                    } else if (activeSubMenu == null || isTablet) {
                        IconButton(onClick = {
                            viewModel.clearStatusMessage()
                            viewModel.checkStorageStatus(context)
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = LXPrimaryGreen)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LXBackgroundBlack)
            )
        }
    ) { paddingValues ->
        if (isTablet) {
            // Adaptive Two-Pane Layout for Tablets
            val currentSelection = activeSubMenu ?: SettingsSubMenu.CATEGORIES
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Master Pane: Settings Navigation Menu
                Card(
                    modifier = Modifier
                        .width(290.dp)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    SettingsMainMenu(
                        state = state,
                        jikanSyncState = jikanSyncState,
                        tmdbSyncState = tmdbSyncState,
                        selectedSubMenu = currentSelection,
                        onSelectSubMenu = { activeSubMenu = it },
                        onDismissStatusMessage = { viewModel.clearStatusMessage() }
                    )
                }

                // Right Detail Pane: Active Sub-Menu Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (currentSelection) {
                        SettingsSubMenu.CATEGORIES -> CustomCategoriesContent(
                            customCategories = state.customCategories,
                            onAddCategoryClick = { showAddCategoryDialog = true },
                            onDeleteCategoryClick = { categoryToDelete = it }
                        )
                        SettingsSubMenu.SYNC_JOBS -> SyncJobsContent(
                            jikanSyncState = jikanSyncState,
                            tmdbSyncState = tmdbSyncState,
                            onStartJikanSync = { force ->
                                ensureNotificationPermission()
                                viewModel.startJikanSync(context, force)
                            },
                            onCancelJikanSync = { viewModel.cancelJikanSync(context) },
                            onStartTmdbSync = { force ->
                                ensureNotificationPermission()
                                viewModel.startTmdbSync(context, force)
                            },
                            onCancelTmdbSync = { viewModel.cancelTmdbSync(context) }
                        )
                        SettingsSubMenu.STORAGE -> BackupStorageContent(
                            state = state,
                            onCheckStorageStatus = { viewModel.checkStorageStatus(context) },
                            onRequestStoragePermission = { viewModel.requestStoragePermission(context) },
                            onExportToHomeOutput = { viewModel.prepareExport(ExportDestination.HOME_OUTPUT_FOLDER) },
                            onImportAllLocalFiles = {
                                ensureNotificationPermission()
                                viewModel.importAllLocalFiles(context)
                            },
                            onImportLocalFile = {
                                ensureNotificationPermission()
                                viewModel.importLocalFile(context, it)
                            },
                            onPickFile = {
                                ensureNotificationPermission()
                                importLauncher.launch("*/*")
                            },
                            onSaveAs = { viewModel.prepareExport(ExportDestination.CUSTOM_FILE) },
                            onSetTachiyomiImportAsFavorites = { viewModel.setTachiyomiImportAsFavorites(it) },
                            onReindexCovers = { viewModel.reindexCoverImages(context) },
                            onClearCoverCache = { viewModel.clearCoverCache(context) },
                            onClearUnlinkedCoverCache = { viewModel.clearUnlinkedCoverCache(context) },
                            onNavigateToExportGuides = { activeSubMenu = SettingsSubMenu.EXPORT_GUIDES }
                        )
                        SettingsSubMenu.CONFLICTS -> ConflictResolutionContent(
                            selectedStrategy = state.selectedConflictStrategy,
                            onSelectStrategy = { viewModel.setConflictStrategy(it) }
                        )
                        SettingsSubMenu.EXPORT_GUIDES -> ExportGuidesContent(
                            onNavigateToStorage = { activeSubMenu = SettingsSubMenu.STORAGE }
                        )
                        SettingsSubMenu.STARTUP_VIEW -> StartupViewContent(
                            state = state,
                            onSelectProfile = { viewModel.selectProfileForDefaults(it) },
                            onUpdateStartupDefaults = { profileId, mediaType, customCat, subCat, status ->
                                viewModel.updateStartupDefaults(profileId, mediaType, customCat, subCat, status)
                            },
                            onResetStartupDefaults = { viewModel.resetStartupDefaults(it) }
                        )
                        SettingsSubMenu.PREFERENCES -> PreferencesContent(
                            isSwipeToSwitchTabsEnabled = state.isSwipeToSwitchTabsEnabled,
                            onSetSwipeToSwitchTabsEnabled = { viewModel.setSwipeToSwitchTabsEnabled(it) },
                            onNavigateToStartupView = { activeSubMenu = SettingsSubMenu.STARTUP_VIEW }
                        )
                        SettingsSubMenu.SECURITY -> SecurityContent(
                            activeUser = state.activeUser,
                            onUpdatePassKey = { viewModel.updatePassKey(it) }
                        )
                        SettingsSubMenu.ABOUT -> AboutContent(
                            state = state,
                            onCheckForUpdates = { viewModel.checkForUpdates() }
                        )
                    }
                }
            }
        } else {
            // Phone Single-Pane Layout with Sub-menus
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                when (val subMenu = activeSubMenu) {
                    null -> {
                        SettingsMainMenu(
                            state = state,
                            jikanSyncState = jikanSyncState,
                            tmdbSyncState = tmdbSyncState,
                            selectedSubMenu = null,
                            onSelectSubMenu = { activeSubMenu = it },
                            onDismissStatusMessage = { viewModel.clearStatusMessage() },
                            modifier = Modifier.widthIn(max = 840.dp)
                        )
                    }
                    SettingsSubMenu.CATEGORIES -> {
                        CustomCategoriesContent(
                            customCategories = state.customCategories,
                            onAddCategoryClick = { showAddCategoryDialog = true },
                            onDeleteCategoryClick = { categoryToDelete = it },
                            modifier = Modifier.widthIn(max = 840.dp)
                        )
                    }
                    SettingsSubMenu.SYNC_JOBS -> {
                        SyncJobsContent(
                            jikanSyncState = jikanSyncState,
                            tmdbSyncState = tmdbSyncState,
                            onStartJikanSync = { force ->
                                ensureNotificationPermission()
                                viewModel.startJikanSync(context, force)
                            },
                            onCancelJikanSync = { viewModel.cancelJikanSync(context) },
                            onStartTmdbSync = { force ->
                                ensureNotificationPermission()
                                viewModel.startTmdbSync(context, force)
                            },
                            onCancelTmdbSync = { viewModel.cancelTmdbSync(context) },
                            modifier = Modifier.widthIn(max = 840.dp)
                        )
                    }
                    SettingsSubMenu.STORAGE -> {
                        BackupStorageContent(
                            state = state,
                            onCheckStorageStatus = { viewModel.checkStorageStatus(context) },
                            onRequestStoragePermission = { viewModel.requestStoragePermission(context) },
                            onExportToHomeOutput = { viewModel.prepareExport(ExportDestination.HOME_OUTPUT_FOLDER) },
                            onImportAllLocalFiles = {
                                ensureNotificationPermission()
                                viewModel.importAllLocalFiles(context)
                            },
                            onImportLocalFile = {
                                ensureNotificationPermission()
                                viewModel.importLocalFile(context, it)
                            },
                            onPickFile = {
                                ensureNotificationPermission()
                                importLauncher.launch("*/*")
                            },
                            onSaveAs = { viewModel.prepareExport(ExportDestination.CUSTOM_FILE) },
                            onSetTachiyomiImportAsFavorites = { viewModel.setTachiyomiImportAsFavorites(it) },
                            onReindexCovers = { viewModel.reindexCoverImages(context) },
                            onClearCoverCache = { viewModel.clearCoverCache(context) },
                            onClearUnlinkedCoverCache = { viewModel.clearUnlinkedCoverCache(context) },
                            onNavigateToExportGuides = { activeSubMenu = SettingsSubMenu.EXPORT_GUIDES },
                            modifier = Modifier.widthIn(max = 840.dp)
                        )
                    }
                    SettingsSubMenu.CONFLICTS -> {
                        ConflictResolutionContent(
                            selectedStrategy = state.selectedConflictStrategy,
                            onSelectStrategy = { viewModel.setConflictStrategy(it) },
                            modifier = Modifier.widthIn(max = 840.dp)
                        )
                    }
                    SettingsSubMenu.EXPORT_GUIDES -> {
                        ExportGuidesContent(
                            onNavigateToStorage = { activeSubMenu = SettingsSubMenu.STORAGE },
                            modifier = Modifier.widthIn(max = 840.dp)
                        )
                    }
                    SettingsSubMenu.STARTUP_VIEW -> {
                        StartupViewContent(
                            state = state,
                            onSelectProfile = { viewModel.selectProfileForDefaults(it) },
                            onUpdateStartupDefaults = { profileId, mediaType, customCat, subCat, status ->
                                viewModel.updateStartupDefaults(profileId, mediaType, customCat, subCat, status)
                            },
                            onResetStartupDefaults = { viewModel.resetStartupDefaults(it) },
                            modifier = Modifier.widthIn(max = 840.dp)
                        )
                    }
                    SettingsSubMenu.PREFERENCES -> {
                        PreferencesContent(
                            isSwipeToSwitchTabsEnabled = state.isSwipeToSwitchTabsEnabled,
                            onSetSwipeToSwitchTabsEnabled = { viewModel.setSwipeToSwitchTabsEnabled(it) },
                            onNavigateToStartupView = { activeSubMenu = SettingsSubMenu.STARTUP_VIEW },
                            modifier = Modifier.widthIn(max = 840.dp)
                        )
                    }
                    SettingsSubMenu.SECURITY -> {
                        SecurityContent(
                            activeUser = state.activeUser,
                            onUpdatePassKey = { viewModel.updatePassKey(it) },
                            modifier = Modifier.widthIn(max = 840.dp)
                        )
                    }
                    SettingsSubMenu.ABOUT -> {
                        AboutContent(
                            state = state,
                            onCheckForUpdates = { viewModel.checkForUpdates() },
                            modifier = Modifier.widthIn(max = 840.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsMainMenu(
    state: SettingsUiState,
    jikanSyncState: JikanSyncProgress,
    tmdbSyncState: TmdbSyncProgress,
    selectedSubMenu: SettingsSubMenu?,
    onSelectSubMenu: (SettingsSubMenu) -> Unit,
    onDismissStatusMessage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Notification Banner (if any)
        if (state.statusMessage != null) {
            StatusMessageBanner(
                message = state.statusMessage,
                isError = state.isErrorMessage,
                onDismiss = onDismissStatusMessage
            )
        }

        // Section: Sync & External Services
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionHeader("Sync & External Services")
            val isSyncing = jikanSyncState.isRunning || tmdbSyncState.isRunning
            SettingsMenuItem(
                icon = Icons.Default.Sync,
                iconBackgroundColor = LXPrimaryGreen.copy(alpha = 0.15f),
                iconTintColor = LXPrimaryGreen,
                title = "External Sync Jobs",
                subtitle = "Automated metadata, poster & episode sync with online databases",
                badgeText = if (isSyncing) "SYNCING" else "ONLINE",
                badgeColor = LXPrimaryGreen,
                isSelected = selectedSubMenu == SettingsSubMenu.SYNC_JOBS,
                onClick = { onSelectSubMenu(SettingsSubMenu.SYNC_JOBS) }
            )
        }

        // Section: Content & Organization
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionHeader("Content & Organization")
            SettingsMenuItem(
                icon = Icons.Default.Bookmark,
                iconBackgroundColor = LXPrimaryGreen.copy(alpha = 0.15f),
                iconTintColor = LXPrimaryGreen,
                title = "Custom Categories",
                subtitle = if (state.customCategories.isEmpty()) "No custom categories created"
                else "${state.customCategories.size} custom categories created",
                badgeText = if (state.customCategories.isNotEmpty()) "${state.customCategories.size}" else null,
                badgeColor = LXPrimaryGreen,
                isSelected = selectedSubMenu == SettingsSubMenu.CATEGORIES,
                onClick = { onSelectSubMenu(SettingsSubMenu.CATEGORIES) }
            )
        }

        // Section: Data & Storage
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionHeader("Data & Storage")
            SettingsMenuItem(
                icon = Icons.Default.Folder,
                iconBackgroundColor = Color(0xFF4DA8DA).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF4DA8DA),
                title = "Backup & Storage",
                subtitle = if (state.hasStoragePermission) "Connected • ${state.inputFiles.size} input files • ${state.coverCacheSizeFormatted} covers"
                else "Storage access required • ${state.coverCacheSizeFormatted} covers",
                badgeText = if (state.hasStoragePermission) "CONNECTED" else "ACCESS NEEDED",
                badgeColor = if (state.hasStoragePermission) LXPrimaryGreen else LXScoreGold,
                isSelected = selectedSubMenu == SettingsSubMenu.STORAGE,
                onClick = { onSelectSubMenu(SettingsSubMenu.STORAGE) }
            )
            SettingsMenuItem(
                icon = Icons.Default.Sync,
                iconBackgroundColor = LXScoreGold.copy(alpha = 0.15f),
                iconTintColor = LXScoreGold,
                title = "Conflict Resolution",
                subtitle = when (state.selectedConflictStrategy) {
                    ConflictStrategy.SKIP -> "Skip: Keep existing entries"
                    ConflictStrategy.OVERWRITE -> "Overwrite: Replace with imported"
                    ConflictStrategy.MERGE -> "Merge: Smartly merge entries"
                },
                badgeText = state.selectedConflictStrategy.name,
                badgeColor = LXScoreGold,
                isSelected = selectedSubMenu == SettingsSubMenu.CONFLICTS,
                onClick = { onSelectSubMenu(SettingsSubMenu.CONFLICTS) }
            )
            SettingsMenuItem(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                iconBackgroundColor = LXPrimaryGreen.copy(alpha = 0.15f),
                iconTintColor = LXPrimaryGreen,
                title = "Export Guides & Help",
                subtitle = "How to export from MyAnimeList, AniList & Tachiyomi",
                badgeText = "GUIDES",
                badgeColor = LXPrimaryGreen,
                isSelected = selectedSubMenu == SettingsSubMenu.EXPORT_GUIDES,
                onClick = { onSelectSubMenu(SettingsSubMenu.EXPORT_GUIDES) }
            )
        }

        // Section: Preferences
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionHeader("Preferences")

            val currentProfileId = state.selectedProfileIdForDefaults ?: state.activeUser.id
            val targetUser = state.allUsers.firstOrNull { it.id == currentProfileId } ?: state.activeUser
            val defaultCatName = targetUser.defaultCustomCategory ?: (targetUser.defaultMediaType ?: MediaType.ANIME).displayName
            val defaultTabName = targetUser.defaultSubCategory ?: (targetUser.defaultStatus?.getDisplayLabel(targetUser.defaultMediaType ?: MediaType.ANIME) ?: "All")

            SettingsMenuItem(
                icon = Icons.Default.Bookmark,
                iconBackgroundColor = LXPrimaryGreen.copy(alpha = 0.15f),
                iconTintColor = LXPrimaryGreen,
                title = "Default Startup View",
                subtitle = "${targetUser.name}: $defaultCatName • $defaultTabName",
                badgeText = "PER PROFILE",
                badgeColor = LXPrimaryGreen,
                isSelected = selectedSubMenu == SettingsSubMenu.STARTUP_VIEW,
                onClick = { onSelectSubMenu(SettingsSubMenu.STARTUP_VIEW) }
            )

            SettingsMenuItem(
                icon = Icons.Default.TouchApp,
                iconBackgroundColor = Color(0xFFBB86FC).copy(alpha = 0.15f),
                iconTintColor = Color(0xFFBB86FC),
                title = "Gestures & Navigation",
                subtitle = if (state.isSwipeToSwitchTabsEnabled) "Swipe between tabs enabled"
                else "Swipe between tabs disabled",
                badgeText = if (state.isSwipeToSwitchTabsEnabled) "ON" else "OFF",
                badgeColor = if (state.isSwipeToSwitchTabsEnabled) LXPrimaryGreen else LXTextSecondary,
                isSelected = selectedSubMenu == SettingsSubMenu.PREFERENCES,
                onClick = { onSelectSubMenu(SettingsSubMenu.PREFERENCES) }
            )

            SettingsMenuItem(
                icon = Icons.Default.Lock,
                iconBackgroundColor = LXPrimaryGreen.copy(alpha = 0.15f),
                iconTintColor = LXPrimaryGreen,
                title = "Security & Passkey",
                subtitle = if (state.activeUser.passKey != null) "Passkey protection active for \"${state.activeUser.name}\""
                else "Lock this profile with a 4-digit passkey",
                badgeText = if (state.activeUser.passKey != null) "PROTECTED" else "OFF",
                badgeColor = if (state.activeUser.passKey != null) LXPrimaryGreen else LXTextSecondary,
                isSelected = selectedSubMenu == SettingsSubMenu.SECURITY,
                onClick = { onSelectSubMenu(SettingsSubMenu.SECURITY) }
            )
        }

        // Section: System & About
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionHeader("System & About")
            SettingsMenuItem(
                icon = Icons.Default.Info,
                iconBackgroundColor = Color(0xFF00E5FF).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF00E5FF),
                title = "About LX Track List",
                subtitle = "Version ${com.lx.tracklist.BuildConfig.VERSION_NAME} • Device & storage details",
                badgeText = "v${com.lx.tracklist.BuildConfig.VERSION_NAME}",
                badgeColor = Color(0xFF00E5FF),
                isSelected = selectedSubMenu == SettingsSubMenu.ABOUT,
                onClick = { onSelectSubMenu(SettingsSubMenu.ABOUT) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 11.sp
        ),
        color = LXPrimaryGreen,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTintColor: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color = LXPrimaryGreen,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) LXPrimaryGreen.copy(alpha = 0.12f) else LXSurfaceBlack)
            .border(
                width = 1.dp,
                color = if (isSelected) LXPrimaryGreen else LXBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = LXTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = LXTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = if (isSelected) LXPrimaryGreen else LXTextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun StatusMessageBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) LXErrorRed.copy(alpha = 0.15f) else LXPrimaryGreen.copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isError) LXErrorRed else LXPrimaryGreen
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isError) LXErrorRed else LXPrimaryGreen
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = message,
                    color = LXTextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = LXTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomCategoriesContent(
    customCategories: List<CustomCategory>,
    onAddCategoryClick: () -> Unit,
    onDeleteCategoryClick: (CustomCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
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
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = LXPrimaryGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Custom Categories (${customCategories.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }

                    Button(
                        onClick = onAddCategoryClick,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LXPrimaryGreen.copy(alpha = 0.15f),
                            contentColor = LXPrimaryGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Text(
                    text = "Add personalized categories outside default ones (e.g. Donghua, Web Novels, Audiobooks, Visual Novels, Games) and track them with custom units.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary
                )

                if (customCategories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LXSurfaceElevated)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = LXTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No custom categories created yet.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = LXTextPrimary
                            )
                            Text(
                                text = "Create custom categories to organize and track media with custom units.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onAddCategoryClick,
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LXPrimaryGreen.copy(alpha = 0.15f),
                                    contentColor = LXPrimaryGreen
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Category", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        customCategories.forEach { cat ->
                            val catColor = parseHexColor(cat.colorHex)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LXSurfaceElevated)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(catColor.copy(alpha = 0.2f))
                                            .border(1.dp, catColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (cat.isWatchType) Icons.Default.LiveTv else Icons.Default.Bookmark,
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = LXTextPrimary
                                        )
                                        Text(
                                            text = if (cat.isWatchType) "Watch • Episodes" else "Read • ${cat.trackingUnit.displayName}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = LXTextSecondary
                                        )
                                    }
                                }

                                IconButton(onClick = { onDeleteCategoryClick(cat) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Category",
                                        tint = LXTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BackupStorageContent(
    state: SettingsUiState,
    onCheckStorageStatus: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onExportToHomeOutput: () -> Unit,
    onImportAllLocalFiles: () -> Unit,
    onImportLocalFile: (StorageFileInfo) -> Unit,
    onPickFile: () -> Unit,
    onSaveAs: () -> Unit,
    onSetTachiyomiImportAsFavorites: (Boolean) -> Unit,
    onReindexCovers: () -> Unit,
    onClearCoverCache: () -> Unit,
    onClearUnlinkedCoverCache: () -> Unit,
    onNavigateToExportGuides: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showReindexConfirmDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirmDialog by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dedicated Home Folder Storage Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
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
                        Icon(Icons.Default.Folder, contentDescription = null, tint = LXPrimaryGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Home Storage Folder",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (state.hasStoragePermission) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(LXPrimaryGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "CONNECTED",
                                color = LXPrimaryGreen,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Root: ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = LXPrimaryGreen
                        )
                        Text(
                            text = state.homeFolderPath.ifBlank { "/storage/emulated/0/LXTrackList" },
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            color = LXTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Input: ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = LXTextSecondary
                        )
                        Text(
                            text = "${state.homeFolderPath.ifBlank { "/storage/emulated/0/LXTrackList" }}/input",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            color = LXTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Output: ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = LXTextSecondary
                        )
                        Text(
                            text = "${state.homeFolderPath.ifBlank { "/storage/emulated/0/LXTrackList" }}/output",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            color = LXTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!state.hasStoragePermission) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LXSurfaceElevated),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = LXScoreGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Storage Access Required", color = LXScoreGold, style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Grant access so LX Track List can read from the input folder and export directly to output.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onRequestStoragePermission,
                                colors = ButtonDefaults.buttonColors(containerColor = LXScoreGold, contentColor = LXBackgroundBlack),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Grant Storage Access", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Direct Export Button
                    Button(
                        onClick = onExportToHomeOutput,
                        enabled = !state.isExporting && !state.isPreparingExport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 44.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isExporting) {
                            CircularProgressIndicator(color = LXBackgroundBlack, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exporting to output folder...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Export Backup to /output Folder",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(color = LXBorder, thickness = 1.dp)

                    // Files in input folder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Files in /input folder (${state.inputFiles.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )

                        if (state.inputFiles.isNotEmpty()) {
                            TextButton(
                                onClick = onImportAllLocalFiles,
                                enabled = !state.isImporting && !state.isPreparingImport
                            ) {
                                Text("Import All", color = LXPrimaryGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (state.inputFiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LXSurfaceElevated)
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No files found in ${state.homeFolderPath}/input\nCopy your MAL XML/GZ, Tachiyomi (.tachibk), CSV, or JSON files there to import instantly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.inputFiles.forEach { fileInfo ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(LXSurfaceElevated)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fileInfo.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = LXTextPrimary
                                        )
                                        Text(
                                            text = "${fileInfo.fileType} • ${fileInfo.sizeFormatted} • ${fileInfo.lastModifiedFormatted}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = LXTextSecondary
                                        )
                                    }

                                    Button(
                                        onClick = { onImportLocalFile(fileInfo) },
                                        enabled = !state.isImporting && !state.isPreparingImport,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = LXPrimaryGreen.copy(alpha = 0.2f),
                                            contentColor = LXPrimaryGreen
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Import", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Help on Exporting from Sources Banner
        if (onNavigateToExportGuides != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigateToExportGuides() },
                colors = CardDefaults.cardColors(containerColor = LXPrimaryGreen.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LXPrimaryGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = LXPrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "How to Export from Other Apps",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = LXPrimaryGreen
                        )
                        Text(
                            text = "Guides for MyAnimeList, AniList, Tachiyomi & CSV.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LXTextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "View Guides",
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Standard File Picker Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Manual File Import & Export",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )

                Text(
                    text = "Select backup files anywhere on your device or cloud storage to import or export.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickFile,
                        enabled = !state.isImporting && !state.isPreparingImport,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LXTextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pick File...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onSaveAs,
                        enabled = !state.isExporting && !state.isPreparingExport,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LXTextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save As...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Tachiyomi Import Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tachiyomi Import Options",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                }

                Text(
                    text = "Tachiyomi and Mihon mark all library items as favorites by default. Choose how you want them imported into LX Track List.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .clickable { onSetTachiyomiImportAsFavorites(!state.isTachiyomiImportAsFavorites) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Import as Favorites by Default",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = LXTextPrimary
                        )
                        Text(
                            text = if (state.isTachiyomiImportAsFavorites)
                                "Tachiyomi entries will be starred as favorites"
                            else
                                "Tachiyomi entries will be imported as normal library items",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = if (state.isTachiyomiImportAsFavorites) LXPrimaryGreen else LXTextSecondary
                        )
                    }

                    Switch(
                        checked = state.isTachiyomiImportAsFavorites,
                        onCheckedChange = onSetTachiyomiImportAsFavorites,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LXBackgroundBlack,
                            checkedTrackColor = LXPrimaryGreen,
                            uncheckedThumbColor = LXTextSecondary,
                            uncheckedTrackColor = LXSurfaceElevated
                        )
                    )
                }
            }
        }

        // Cover Image Cache & Offline Storage Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cover Image Cache",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                }

                Text(
                    text = "Images with working URLs are stored in persistent local cache so they remain available offline and won't be cleared by the OS. Use 'Reindex Images' to refresh the cache and re-download fresh covers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary
                )

                // Cache Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Local Cache Size",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = LXTextSecondary
                        )
                        Text(
                            text = state.coverCacheSizeFormatted,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXPrimaryGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Cached Covers",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = LXTextSecondary
                        )
                        Text(
                            text = "${state.coverCacheCount} files",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }
                }

                if (state.isReindexingCovers) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.reindexStatusText ?: "Reindexing covers...",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = LXPrimaryGreen,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(state.reindexProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = LXTextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(LXSurfaceElevated)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(state.reindexProgress.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(LXPrimaryGreen)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showReindexConfirmDialog = true },
                            enabled = !state.isReindexingCovers && !state.isImporting && !state.isExporting,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 44.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LXPrimaryGreen,
                                contentColor = LXBackgroundBlack
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reindex Images",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { showClearCacheConfirmDialog = true },
                            enabled = !state.isReindexingCovers && state.coverCacheCount > 0,
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LXErrorRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LXErrorRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = LXErrorRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear", color = LXErrorRed, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (showReindexConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showReindexConfirmDialog = false },
                title = { Text("Reindex Cover Images", color = LXTextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "This will clear your local cover image cache and re-download fresh images for all entries with valid URLs.\n\nEntries whose URLs are broken or unreachable will simply remain uncached until re-checked.",
                        color = LXTextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showReindexConfirmDialog = false
                            onReindexCovers()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack)
                    ) {
                        Text("Reindex Now", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReindexConfirmDialog = false }) {
                        Text("Cancel", color = LXTextSecondary)
                    }
                },
                containerColor = LXSurfaceBlack,
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (showClearCacheConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheConfirmDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = LXPrimaryGreen)
                        Text("Clear Cover Cache", color = LXTextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Choose which cached cover images you want to clear from local storage:",
                            color = LXTextSecondary,
                            fontSize = 13.sp
                        )

                        // Option 1: Clear Unlinked Only (Recommended)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showClearCacheConfirmDialog = false
                                    onClearUnlinkedCoverCache()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LXSurfaceElevated),
                            border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(LXPrimaryGreen.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = LXPrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "Clear Unlinked Only",
                                        color = LXPrimaryGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = LXPrimaryGreen, fontWeight = FontWeight.SemiBold)) {
                                            append("Recommended: ")
                                        }
                                        append("Preserves all covers of actively tracked entries in your library. Only removes unlinked search thumbnails and orphan files.")
                                    },
                                    color = LXTextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        // Option 2: Clear Entire Cache
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showClearCacheConfirmDialog = false
                                    onClearCoverCache()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = LXSurfaceElevated),
                            border = BorderStroke(1.dp, LXBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(LXErrorRed.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = LXErrorRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "Clear Entire Image Cache",
                                        color = LXErrorRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    text = "Deletes all cached images. Actively tracked covers will re-download when viewed online.",
                                    color = LXTextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showClearCacheConfirmDialog = false }) {
                        Text("Cancel", color = LXTextSecondary)
                    }
                },
                containerColor = LXSurfaceBlack,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SyncJobsContent(
    jikanSyncState: JikanSyncProgress,
    tmdbSyncState: TmdbSyncProgress,
    onStartJikanSync: (Boolean) -> Unit,
    onCancelJikanSync: () -> Unit,
    onStartTmdbSync: (Boolean) -> Unit,
    onCancelTmdbSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "External Sync Services",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                }
                Text(
                    text = "Automate fetching official high-resolution posters, runtimes, broadcast dates, season & episode counts, tags, and ratings from MyAnimeList (Jikan), MangaDex, AniList, and The Movie Database (TMDB).",
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary
                )
            }
        }

        // Anime / Manga Sync Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
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
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = LXPrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Anime / Manga Sync",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (jikanSyncState.isRunning) {
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
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Text(
                    text = "Fetches official posters, runtimes, broadcast dates, total episodes/chapters, alternate titles, tags, and synopses from MyAnimeList (Jikan) with automatic MangaDex, AniList, and Kitsu fallbacks for Manga and Manhwa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary
                )

                if (jikanSyncState.isRunning) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LXSurfaceElevated)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progress (${jikanSyncState.completedCount}/${jikanSyncState.totalCount}) • ${jikanSyncState.updatedCount} updated",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = LXTextPrimary
                            )
                            Text(
                                text = "${(jikanSyncState.progressFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = LXPrimaryGreen
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(LXBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(jikanSyncState.progressFraction)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(LXPrimaryGreen)
                            )
                        }

                        if (jikanSyncState.currentTitle.isNotBlank()) {
                            Text(
                                text = "Current: ${jikanSyncState.currentTitle}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = LXTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "Pending: ${jikanSyncState.pendingCount} remaining${if (jikanSyncState.failedCount > 0) " • ${jikanSyncState.failedCount} failed" else ""}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LXTextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = onCancelJikanSync,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LXErrorRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LXErrorRed.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel Sync", fontSize = 12.sp, color = LXErrorRed)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onStartJikanSync(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LXPrimaryGreen,
                                contentColor = LXBackgroundBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Fetch Missing",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { onStartJikanSync(true) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LXPrimaryGreen),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Re-fetch All",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Movies / TV Sync Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
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
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = LXPrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Movies / TV Sync",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val isConfigured = TmdbConfig.isConfigured(LocalContext.current)
                    if (tmdbSyncState.isRunning) {
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
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else if (isConfigured) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(LXPrimaryGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "CONNECTED",
                                color = LXPrimaryGreen,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Text(
                    text = "Fetches official posters, runtimes, broadcast dates, season & episode counts, tags, and ratings for Movies and TV Shows from The Movie Database (TMDB).",
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary
                )

                // TMDB Logo & Official Attribution Notice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_tmdb_logo),
                        contentDescription = "TMDB",
                        modifier = Modifier.height(18.dp).width(26.dp)
                    )
                    Text(
                        text = TmdbConfig.ATTRIBUTION_TEXT,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = LXTextSecondary.copy(alpha = 0.8f)
                    )
                }

                if (tmdbSyncState.isRunning) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LXSurfaceElevated)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progress (${tmdbSyncState.completedCount}/${tmdbSyncState.totalCount}) • ${tmdbSyncState.updatedCount} updated",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = LXTextPrimary
                            )
                            Text(
                                text = "${(tmdbSyncState.progressFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = LXPrimaryGreen
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(LXBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(tmdbSyncState.progressFraction)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(LXPrimaryGreen)
                            )
                        }

                        if (tmdbSyncState.currentTitle.isNotBlank()) {
                            Text(
                                text = "Current: ${tmdbSyncState.currentTitle}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = LXTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "Pending: ${tmdbSyncState.pendingCount} remaining${if (tmdbSyncState.failedCount > 0) " • ${tmdbSyncState.failedCount} failed" else ""}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LXTextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = onCancelTmdbSync,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LXErrorRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LXErrorRed.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel Sync", fontSize = 12.sp, color = LXErrorRed)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onStartTmdbSync(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LXPrimaryGreen,
                                contentColor = LXBackgroundBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Fetch Missing",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { onStartTmdbSync(true) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LXPrimaryGreen),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Re-fetch All",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ConflictResolutionContent(
    selectedStrategy: ConflictStrategy,
    onSelectStrategy: (ConflictStrategy) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = LXScoreGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Conflict Resolution Strategy",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                }

                Text(
                    text = "Determines how to handle existing entries when importing from backup files, MAL XML, or Tachiyomi backups:",
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary
                )

                ConflictStrategy.entries.forEach { strategy ->
                    val isSelected = selectedStrategy == strategy
                    val desc = when (strategy) {
                        ConflictStrategy.SKIP -> "Keep existing entry, ignore imported version"
                        ConflictStrategy.OVERWRITE -> "Replace existing entry completely with imported data"
                        ConflictStrategy.MERGE -> "Keep higher progress, newer dates, and merge subcategories/tags"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) LXPrimaryGreen.copy(alpha = 0.12f) else LXSurfaceElevated)
                            .border(1.dp, if (isSelected) LXPrimaryGreen else LXBorder, RoundedCornerShape(8.dp))
                            .clickable { onSelectStrategy(strategy) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) LXPrimaryGreen else LXBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = LXBackgroundBlack, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = strategy.name,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                                )
                                if (strategy == ConflictStrategy.MERGE) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(LXPrimaryGreen.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "RECOMMENDED",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = LXPrimaryGreen
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = LXTextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PreferencesContent(
    isSwipeToSwitchTabsEnabled: Boolean,
    onSetSwipeToSwitchTabsEnabled: (Boolean) -> Unit,
    onNavigateToStartupView: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onNavigateToStartupView != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToStartupView() },
                colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                border = androidx.compose.foundation.BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(LXPrimaryGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = LXPrimaryGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Default Startup View",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = LXTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Configure default Category and Status or SubCategory when app boots up (per Profile)",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open",
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Swipe to Switch Tabs",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Swipe left or right on Home screen to switch between status and subcategory tabs",
                            style = MaterialTheme.typography.bodySmall,
                            color = LXTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = isSwipeToSwitchTabsEnabled,
                        onCheckedChange = onSetSwipeToSwitchTabsEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LXBackgroundBlack,
                            checkedTrackColor = LXPrimaryGreen,
                            uncheckedThumbColor = LXTextSecondary,
                            uncheckedTrackColor = LXSurfaceElevated
                        )
                    )
                }

                HorizontalDivider(color = LXBorder, thickness = 1.dp)

                Text(
                    text = "Tip: When enabled, swiping across the Home screen smoothly navigates through All, Watching, Completed, On Hold, and custom subcategories.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = LXTextSecondary.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StartupViewContent(
    state: SettingsUiState,
    onSelectProfile: (Long) -> Unit,
    onUpdateStartupDefaults: (Long, MediaType?, String?, String?, MediaStatus?) -> Unit,
    onResetStartupDefaults: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentProfileId = state.selectedProfileIdForDefaults ?: state.activeUser.id
    val targetProfile = state.allUsers.firstOrNull { it.id == currentProfileId } ?: state.activeUser

    val currentMediaType = targetProfile.defaultMediaType ?: MediaType.ANIME
    val currentCustomCat = targetProfile.defaultCustomCategory
    val currentSubCat = targetProfile.defaultSubCategory
    val currentStatus = targetProfile.defaultStatus

    val categoryDisplayName = currentCustomCat ?: currentMediaType.displayName
    val tabDisplayName = currentSubCat ?: (currentStatus?.getDisplayLabel(currentMediaType) ?: "All")

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Selector Header
        if (state.allUsers.size > 1) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
                border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Profile",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(LXPrimaryGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Per-Profile Setting",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = LXPrimaryGreen
                            )
                        }
                    }
                    Text(
                        text = "Select which profile's startup default view you want to configure:",
                        style = MaterialTheme.typography.bodySmall,
                        color = LXTextSecondary
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.allUsers) { user ->
                            val isSelected = user.id == targetProfile.id
                            val isActive = user.id == state.activeUser.id
                            val color = parseHexColor(user.avatarColorHex)

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) LXPrimaryGreen.copy(alpha = 0.15f) else LXSurfaceElevated)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) LXPrimaryGreen else LXBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectProfile(user.id) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!user.avatarImageUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = user.avatarImageUri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Text(
                                            text = user.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color.Black
                                        )
                                    }
                                }
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                                )
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(LXPrimaryGreen.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Active",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = LXPrimaryGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Boot Target Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(LXPrimaryGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = LXPrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Current Boot Target",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = LXPrimaryGreen
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LXSurfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = targetProfile.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }
                }

                Text(
                    text = "$categoryDisplayName • $tabDisplayName",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )

                Text(
                    text = "When LX Track List launches into \"${targetProfile.name}\", it will open directly to $categoryDisplayName with the $tabDisplayName tab active.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary
                )
            }
        }

        // Section 1: Default Category
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "1. Default Category",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )
                Text(
                    text = "Select which category tab appears when opening the app:",
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary
                )

                Text(
                    text = "BUILT-IN CATEGORIES",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = LXTextSecondary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MediaType.entries.forEach { mediaType ->
                        val isSelected = currentCustomCat == null && currentMediaType == mediaType
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) LXPrimaryGreen.copy(alpha = 0.15f) else LXSurfaceElevated)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) LXPrimaryGreen else LXBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    val newSubCat = if (currentSubCat != null && state.allSubCategories.any {
                                            it.categoryKey.equals(mediaType.name, ignoreCase = true) &&
                                                    it.name.equals(currentSubCat, ignoreCase = true)
                                        }) currentSubCat else null
                                    onUpdateStartupDefaults(targetProfile.id, mediaType, null, newSubCat, currentStatus)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(mediaType),
                                contentDescription = null,
                                tint = if (isSelected) LXPrimaryGreen else LXTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = mediaType.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (state.customCategories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "CUSTOM CATEGORIES",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = LXTextSecondary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        state.customCategories.forEach { customCat ->
                            val isSelected = currentCustomCat.equals(customCat.name, ignoreCase = true)
                            val accentColor = parseHexColor(customCat.colorHex)

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) accentColor.copy(alpha = 0.15f) else LXSurfaceElevated)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) accentColor else LXBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        val newSubCat = if (currentSubCat != null && state.allSubCategories.any {
                                                it.categoryKey.equals(customCat.name, ignoreCase = true) &&
                                                        it.name.equals(currentSubCat, ignoreCase = true)
                                            }) currentSubCat else null
                                        onUpdateStartupDefaults(
                                            targetProfile.id,
                                            customCat.mediaType ?: MediaType.MANGA,
                                            customCat.name,
                                            newSubCat,
                                            currentStatus
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                                Text(
                                    text = customCat.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) accentColor else LXTextPrimary
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Default Tab (Status or SubCategory)
        val activeKey = currentCustomCat ?: currentMediaType.name
        val subCatsForActive = state.allSubCategories.filter { it.categoryKey.equals(activeKey, ignoreCase = true) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "2. Default Tab (Status / SubCategory)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )
                Text(
                    text = "Choose which tab is active for $categoryDisplayName on launch. SubCategory and Status are mutually exclusive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary
                )

                Text(
                    text = "STATUS TABS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = LXTextSecondary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // ALL tab
                    val isAllSelected = currentSubCat == null && currentStatus == null
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isAllSelected) LXPrimaryGreen.copy(alpha = 0.15f) else LXSurfaceElevated)
                            .border(
                                width = if (isAllSelected) 1.5.dp else 1.dp,
                                color = if (isAllSelected) LXPrimaryGreen else LXBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onUpdateStartupDefaults(targetProfile.id, currentMediaType, currentCustomCat, null, null)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ALL (Default)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isAllSelected) LXPrimaryGreen else LXTextPrimary
                        )
                        if (isAllSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = LXPrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Status entries
                    val statuses = listOf(
                        MediaStatus.WATCHING,
                        MediaStatus.PLAN_TO_WATCH,
                        MediaStatus.COMPLETED,
                        MediaStatus.ON_HOLD,
                        MediaStatus.DROPPED
                    )

                    statuses.forEach { st ->
                        val isStatusSelected = currentSubCat == null && currentStatus == st
                        val label = st.getDisplayLabel(currentMediaType)

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isStatusSelected) LXPrimaryGreen.copy(alpha = 0.15f) else LXSurfaceElevated)
                                .border(
                                    width = if (isStatusSelected) 1.5.dp else 1.dp,
                                    color = if (isStatusSelected) LXPrimaryGreen else LXBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onUpdateStartupDefaults(targetProfile.id, currentMediaType, currentCustomCat, null, st)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isStatusSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isStatusSelected) LXPrimaryGreen else LXTextPrimary
                            )
                            if (isStatusSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SUBCATEGORIES FOR $categoryDisplayName",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = LXTextSecondary
                )

                if (subCatsForActive.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        subCatsForActive.forEach { subCat ->
                            val isSubSelected = currentSubCat.equals(subCat.name, ignoreCase = true)
                            val color = parseHexColor(subCat.colorHex)

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSubSelected) color.copy(alpha = 0.15f) else LXSurfaceElevated)
                                    .border(
                                        width = if (isSubSelected) 1.5.dp else 1.dp,
                                        color = if (isSubSelected) color else LXBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        onUpdateStartupDefaults(
                                            targetProfile.id,
                                            currentMediaType,
                                            currentCustomCat,
                                            subCat.name,
                                            null
                                        )
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Text(
                                    text = subCat.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSubSelected) color else LXTextPrimary
                                )
                                if (isSubSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LXSurfaceElevated.copy(alpha = 0.5f))
                            .border(1.dp, LXBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "No subcategories found for $categoryDisplayName. You can create subcategories directly from the Home screen tab bar (+) to use them as your startup view.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LXTextSecondary
                        )
                    }
                }
            }
        }

        // Section 3: Reset
        OutlinedButton(
            onClick = { onResetStartupDefaults(targetProfile.id) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LXTextSecondary),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = LXTextSecondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Reset ${targetProfile.name} to App Default (Anime • All)",
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AboutContent(
    state: SettingsUiState,
    onCheckForUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val openUrl: (String) -> Unit = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Branding Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.lx_logo),
                    contentDescription = "LX Logo",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "LX Track List",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXPrimaryGreen
                    )
                    Text(
                        text = "Version ${com.lx.tracklist.BuildConfig.VERSION_NAME} (Optimized for Phone & Tablet)",
                        style = MaterialTheme.typography.bodySmall,
                        color = LXTextSecondary
                    )
                    Text(
                        text = "Xiaomi 11T Pro, OnePlus 12R & OnePlus Pad Ready",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = LXTextSecondary.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // App Updates Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = BorderStroke(
                1.dp,
                if (state.updateStatus is AppUpdateStatus.UpdateAvailable) LXPrimaryGreen else LXBorder
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (state.updateStatus is AppUpdateStatus.UpdateAvailable)
                                        LXPrimaryGreen.copy(alpha = 0.15f)
                                    else
                                        LXSurfaceElevated
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (state.updateStatus) {
                                    is AppUpdateStatus.UpdateAvailable -> Icons.Default.NewReleases
                                    is AppUpdateStatus.UpToDate -> Icons.Default.CheckCircle
                                    is AppUpdateStatus.Error -> Icons.Default.ErrorOutline
                                    else -> Icons.Default.SystemUpdate
                                },
                                contentDescription = "App Updates",
                                tint = when (state.updateStatus) {
                                    is AppUpdateStatus.UpdateAvailable -> LXPrimaryGreen
                                    is AppUpdateStatus.UpToDate -> LXPrimaryGreen
                                    is AppUpdateStatus.Error -> Color(0xFFEF4444)
                                    else -> LXTextSecondary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Check for Updates",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = LXTextPrimary
                            )
                            Text(
                                text = "Current version: v${com.lx.tracklist.BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextSecondary
                            )
                        }
                    }

                    // Status Badge
                    when (state.updateStatus) {
                        is AppUpdateStatus.UpdateAvailable -> {
                            Surface(
                                color = LXPrimaryGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Update Available",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = LXPrimaryGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        is AppUpdateStatus.UpToDate -> {
                            Surface(
                                color = LXSurfaceElevated,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, LXBorder)
                            ) {
                                Text(
                                    text = "Latest Version",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = LXTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        else -> Unit
                    }
                }

                HorizontalDivider(color = LXBorder, thickness = 1.dp)

                when (val status = state.updateStatus) {
                    AppUpdateStatus.Idle -> {
                        Text(
                            text = "Check GitHub Releases to see if a newer version is available. On click, the release page will open in your browser.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LXTextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onCheckForUpdates,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LXPrimaryGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Check for Updates", fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { openUrl(AppUpdateChecker.RELEASES_PAGE_URL) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, LXBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LXTextSecondary)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Releases")
                            }
                        }
                    }

                    AppUpdateStatus.Checking -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LXSurfaceElevated)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = LXPrimaryGreen,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Checking GitHub Releases for updates...",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextPrimary
                            )
                        }
                    }

                    is AppUpdateStatus.UpdateAvailable -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LXPrimaryGreen.copy(alpha = 0.08f))
                                .border(1.dp, LXPrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { openUrl(status.updateInfo.releaseUrl) }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = status.updateInfo.releaseName ?: "New Release ${status.updateInfo.latestVersion}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LXPrimaryGreen
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open Release Page",
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = "A new version (${status.updateInfo.latestVersion}) is available on GitHub! Click anywhere on this banner or below to open the release page and download the APK.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { openUrl(status.updateInfo.releaseUrl) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LXPrimaryGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open GitHub Release (${status.updateInfo.latestVersion})", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onCheckForUpdates,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, LXBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LXTextSecondary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    is AppUpdateStatus.UpToDate -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LXSurfaceElevated)
                                .clickable { openUrl(status.updateInfo.releaseUrl) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = LXPrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "You are up to date!",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = LXTextPrimary
                                )
                                Text(
                                    text = "Version ${status.updateInfo.currentVersion} is the latest available release.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LXTextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open Releases",
                                tint = LXTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCheckForUpdates,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, LXBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LXTextPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Check Again")
                            }

                            OutlinedButton(
                                onClick = { openUrl(status.updateInfo.releaseUrl) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, LXBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LXTextSecondary)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View on GitHub")
                            }
                        }
                    }

                    is AppUpdateStatus.Error -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { openUrl(status.fallbackUrl) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Update check failed",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFEF4444)
                                )
                                Text(
                                    text = status.message,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = LXTextSecondary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onCheckForUpdates,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LXSurfaceElevated,
                                    contentColor = LXTextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }

                            OutlinedButton(
                                onClick = { openUrl(status.fallbackUrl) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, LXBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LXTextSecondary)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open GitHub")
                            }
                        }
                    }
                }
            }
        }

        // Details Specifications Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Storage Locations",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )

                StorageLocationItem(
                    label = "Root Storage Folder",
                    path = state.homeFolderPath.ifBlank { "/storage/emulated/0/LXTrackList" }
                )
                StorageLocationItem(
                    label = "Input Folder",
                    path = "${state.homeFolderPath.ifBlank { "/storage/emulated/0/LXTrackList" }}/input"
                )
                StorageLocationItem(
                    label = "Output Folder",
                    path = "${state.homeFolderPath.ifBlank { "/storage/emulated/0/LXTrackList" }}/output"
                )

                HorizontalDivider(color = LXBorder, thickness = 1.dp)

                Text(
                    text = "App Configuration",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )

                InfoRow(label = "Custom Categories", value = "${state.customCategories.size} created")
                InfoRow(label = "Import Strategy", value = state.selectedConflictStrategy.name)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StorageLocationItem(label: String, path: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LXSurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = LXTextSecondary
        )
        Text(
            text = path,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = LXPrimaryGreen,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = LXTextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = LXTextPrimary)
    }
}


private fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        LXPrimaryGreen
    }
}

@Composable
private fun ExportGuidesContent(
    onNavigateToStorage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    var copiedUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(copiedUrl) {
        if (copiedUrl != null) {
            kotlinx.coroutines.delay(2000)
            copiedUrl = null
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(LXPrimaryGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Importing & Exporting Help",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXPrimaryGreen
                    )
                    Text(
                        text = "Follow these step-by-step instructions to export your lists from other tracking platforms and import them into LX Track List.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LXTextSecondary
                    )
                }
            }
        }

        // Section 1: MyAnimeList (MAL) Guide (Featured)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
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
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E51A2).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = Color(0xFF4DA8DA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "MyAnimeList (MAL)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LXPrimaryGreen.copy(alpha = 0.15f))
                            .border(1.dp, LXPrimaryGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "XML / GZ",
                            color = LXPrimaryGreen,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Text(
                    text = "MyAnimeList provides a dedicated export panel where you can download your complete Anime and Manga watch lists in a compressed XML (.xml.gz) format.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LXTextSecondary
                )

                // Interactive URL Box with Open and Copy actions
                val malExportUrl = "https://myanimelist.net/panel.php?go=export"
                val isMalCopied = copiedUrl == malExportUrl

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .border(1.dp, LXBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Official Export Panel URL:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = LXTextSecondary
                    )

                    Text(
                        text = malExportUrl,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = LXPrimaryGreen
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri(malExportUrl)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LXPrimaryGreen,
                                contentColor = LXBackgroundBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open Link",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(malExportUrl))
                                copiedUrl = malExportUrl
                                Toast.makeText(context, "MyAnimeList export URL copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isMalCopied) LXPrimaryGreen else LXTextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isMalCopied) LXPrimaryGreen else LXBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isMalCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isMalCopied) "Copied!" else "Copy URL",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Text(
                    text = "Step-by-step instructions:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )

                GuideStepItem(
                    number = "1",
                    title = "Log in & Open Export Page",
                    description = "Tap 'Open Link' above or copy the URL into your browser (log in to MyAnimeList if prompted)."
                )
                GuideStepItem(
                    number = "2",
                    title = "Export List",
                    description = "Select 'Anime List' or 'Manga List', then tap the 'Export My List' button."
                )
                GuideStepItem(
                    number = "3",
                    title = "Download Archive",
                    description = "Wait a few seconds for MAL to generate your file, then download the .xml.gz archive."
                )
                GuideStepItem(
                    number = "4",
                    title = "Import into LX Track List",
                    description = "Go to Backup & Storage in this app, tap 'Pick & Import File' (or drop the file into /LXTrackList/input), and your entries will be imported automatically."
                )
            }
        }

        // Section 2: AniList Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
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
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF02A9FF).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = Color(0xFF02A9FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AniList",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF02A9FF).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF02A9FF).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "JSON",
                            color = Color(0xFF02A9FF),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Text(
                    text = "AniList allows exporting your full anime and manga tracking history via their Data Exchange settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LXTextSecondary
                )

                val anilistUrl = "https://anilist.co/settings/data-exchange"
                val isAnilistCopied = copiedUrl == anilistUrl

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXSurfaceElevated)
                        .border(1.dp, LXBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "AniList Data Exchange URL:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = LXTextSecondary
                    )

                    Text(
                        text = anilistUrl,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF02A9FF)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    uriHandler.openUri(anilistUrl)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF02A9FF),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open Link",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(anilistUrl))
                                copiedUrl = anilistUrl
                                Toast.makeText(context, "AniList URL copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isAnilistCopied) Color(0xFF02A9FF) else LXTextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isAnilistCopied) Color(0xFF02A9FF) else LXBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isAnilistCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAnilistCopied) "Copied!" else "Copy URL",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Text(
                    text = "Step-by-step instructions:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )

                GuideStepItem(number = "1", title = "Log in to AniList", description = "Navigate to Settings > Data Exchange.")
                GuideStepItem(number = "2", title = "Request Data Export", description = "Click the 'Export' button to generate your data archive.")
                GuideStepItem(number = "3", title = "Download & Import", description = "Download the .json export file and select it in LX Track List under Backup & Storage.")
            }
        }

        // Section 3: Tachiyomi / Mihon / Aniyomi Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
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
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(LXScoreGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = LXScoreGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tachiyomi / Mihon / Aniyomi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LXScoreGold.copy(alpha = 0.15f))
                            .border(1.dp, LXScoreGold.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = ".tachibk",
                            color = LXScoreGold,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Text(
                    text = "Export your entire manga and comic reading library directly from the Tachiyomi, Mihon, or Aniyomi app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LXTextSecondary
                )

                Text(
                    text = "Step-by-step instructions:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )

                GuideStepItem(number = "1", title = "Open App Settings", description = "In Tachiyomi or Mihon, go to More > Settings > Data and storage.")
                GuideStepItem(number = "2", title = "Create Backup", description = "Tap 'Create backup' and save the .tachibk backup file to your device.")
                GuideStepItem(number = "3", title = "Import into LX Track List", description = "In Backup & Storage, tap 'Pick & Import File' and choose your .tachibk file. Your manga titles, read chapters, and custom categories will be seamlessly imported.")
            }
        }

        // Section 4: CSV / JSON Generic
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
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
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFBA68C8).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color(0xFFBA68C8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Custom Spreadsheets",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFBA68C8).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFBA68C8).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "CSV / JSON",
                            color = Color(0xFFBA68C8),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Text(
                    text = "You can also import custom CSV files or previously exported LX Track List JSON backups. Supported columns in CSV include: title, mediaType, status, score, currentEpisodes, totalEpisodes, notes, tags, and startDate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LXTextSecondary
                )
            }
        }

        // Ready to Import Call-to-action
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onNavigateToStorage() },
            colors = CardDefaults.cardColors(containerColor = LXPrimaryGreen.copy(alpha = 0.12f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = LXPrimaryGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ready to import?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LXPrimaryGreen
                    )
                    Text(
                        text = "Go to Backup & Storage to pick or auto-import your files.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LXTextSecondary
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = LXPrimaryGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun GuideStepItem(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(LXPrimaryGreen.copy(alpha = 0.15f))
                .border(1.dp, LXPrimaryGreen.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = LXPrimaryGreen,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = LXTextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = LXTextSecondary
            )
        }
    }
}

@Composable
private fun SecurityContent(
    activeUser: UserProfile,
    onUpdatePassKey: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSetPasskeyDialog by remember { mutableStateOf(false) }
    var showVerifyForChangeDialog by remember { mutableStateOf(false) }
    var showVerifyForRemoveDialog by remember { mutableStateOf(false) }

    val hasPasskey = !activeUser.passKey.isNullOrBlank()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = LXSurfaceElevated),
            border = BorderStroke(1.dp, if (hasPasskey) LXPrimaryGreen.copy(alpha = 0.5f) else LXBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (hasPasskey) LXPrimaryGreen.copy(alpha = 0.15f) else LXSurfaceBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (hasPasskey) LXPrimaryGreen else LXTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeUser.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                        Text(
                            text = if (hasPasskey) "Passkey Protection Enabled (4 digits)" else "No Passkey Protection",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasPasskey) LXPrimaryGreen else LXTextSecondary
                        )
                    }
                }

                HorizontalDivider(color = LXBorder.copy(alpha = 0.5f))

                Text(
                    text = if (hasPasskey) {
                        "When switching to this profile from the profile switcher, you will be required to enter the 4-digit passkey. If entered incorrectly 3 times, a 10-second cooldown is enforced."
                    } else {
                        "Protect \"${activeUser.name}\" with a 4-digit passkey. Switching to this profile will require this passkey. If entered incorrectly 3 times, a 10-second cooldown is enforced."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        // Action Buttons
        if (!hasPasskey) {
            Button(
                onClick = { showSetPasskeyDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set 4-Digit Passkey", fontWeight = FontWeight.Bold)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showVerifyForChangeDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Change Passkey", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { showVerifyForRemoveDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LXErrorRed),
                    border = BorderStroke(1.dp, LXErrorRed.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Remove Passkey", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }
        }

        // Verification dialog before changing passkey (with 3-attempt 10s cooldown)
        if (showVerifyForChangeDialog) {
            PasskeyVerificationDialog(
                profileName = activeUser.name,
                expectedPasskey = activeUser.passKey ?: "",
                title = "Verify Current Passkey",
                subtitle = "Enter current 4-digit passkey to continue",
                onSuccess = {
                    showVerifyForChangeDialog = false
                    showSetPasskeyDialog = true
                },
                onDismiss = { showVerifyForChangeDialog = false }
            )
        }

        // Verification dialog before removing passkey (with 3-attempt 10s cooldown)
        if (showVerifyForRemoveDialog) {
            PasskeyVerificationDialog(
                profileName = activeUser.name,
                expectedPasskey = activeUser.passKey ?: "",
                title = "Remove Passkey",
                subtitle = "Enter current 4-digit passkey to remove protection",
                onSuccess = {
                    showVerifyForRemoveDialog = false
                    onUpdatePassKey(null)
                },
                onDismiss = { showVerifyForRemoveDialog = false }
            )
        }

        // Set or Change Passkey Dialog
        if (showSetPasskeyDialog) {
            SetPasskeyDialog(
                profileName = activeUser.name,
                onSave = { newPin ->
                    onUpdatePassKey(newPin)
                    showSetPasskeyDialog = false
                },
                onDismiss = { showSetPasskeyDialog = false }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SetPasskeyDialog(
    profileName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = LXPrimaryGreen)
                Text("Set 4-Digit Passkey", color = LXTextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Choose a 4-digit PIN for profile \"$profileName\".",
                    color = LXTextSecondary,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(4)
                        pin = digits
                        error = null
                    },
                    label = { Text("4-Digit PIN") },
                    placeholder = { Text("e.g. 1234") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LXPrimaryGreen,
                        unfocusedBorderColor = LXBorder,
                        focusedTextColor = LXTextPrimary,
                        unfocusedTextColor = LXTextPrimary,
                        cursorColor = LXPrimaryGreen
                    )
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(4)
                        confirmPin = digits
                        error = null
                    },
                    label = { Text("Confirm 4-Digit PIN") },
                    placeholder = { Text("Re-enter PIN") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LXPrimaryGreen,
                        unfocusedBorderColor = LXBorder,
                        focusedTextColor = LXTextPrimary,
                        unfocusedTextColor = LXTextPrimary,
                        cursorColor = LXPrimaryGreen
                    )
                )

                if (error != null) {
                    Text(text = error ?: "", color = LXErrorRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length != 4) {
                        error = "PIN must be exactly 4 digits"
                    } else if (pin != confirmPin) {
                        error = "PINs do not match"
                    } else {
                        onSave(pin)
                    }
                },
                enabled = pin.length == 4 && confirmPin.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack)
            ) {
                Text("Save Passkey", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LXTextSecondary)
            }
        },
        containerColor = LXSurfaceBlack,
        shape = RoundedCornerShape(12.dp)
    )
}
