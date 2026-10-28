package com.lx.tracklist.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.R
import com.lx.tracklist.data.model.CustomCategory
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.repository.ConflictStrategy
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CustomCategory?>(null) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIsWatch by remember { mutableStateOf(false) }
    var newCategoryTrackingUnit by remember { mutableStateOf(TrackingUnit.CHAPTERS) }
    val colorOptions = listOf("#A7EF60", "#4DA8DA", "#BB86FC", "#FF7597", "#FFD700", "#00E5FF", "#FF8A65")
    var selectedColor by remember { mutableStateOf(colorOptions.first()) }

    LaunchedEffect(Unit) {
        viewModel.checkStorageStatus(context)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importFromUri(context, it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportToCustomUri(context, it) }
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
                        placeholder = { Text("e.g. Donghua, Web Novels, Games...", color = LXTextSecondary) },
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LXBackgroundBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Storage",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.checkStorageStatus(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = LXPrimaryGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LXBackgroundBlack)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Notification Banner
                if (state.statusMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.isErrorMessage) LXErrorRed.copy(alpha = 0.15f) else LXPrimaryGreen.copy(alpha = 0.15f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (state.isErrorMessage) LXErrorRed else LXPrimaryGreen
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (state.isErrorMessage) LXErrorRed else LXPrimaryGreen
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.statusMessage ?: "",
                                color = LXTextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Custom Categories Management Card
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = LXPrimaryGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Custom Categories (${state.customCategories.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LXTextPrimary
                                )
                            }

                            Button(
                                onClick = { showAddCategoryDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LXPrimaryGreen.copy(alpha = 0.15f),
                                    contentColor = LXPrimaryGreen
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Add Category", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "Add personalized categories outside default ones (e.g. Donghua, Web Novels, Audiobooks, Visual Novels, Games) and track them with custom units.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LXTextSecondary
                        )

                        if (state.customCategories.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LXSurfaceElevated)
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No custom categories created yet. Tap \"+ Add Category\" above to create one.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LXTextSecondary
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.customCategories.forEach { cat ->
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
                                                    .size(32.dp)
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

                                        IconButton(onClick = { categoryToDelete = cat }) {
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = LXPrimaryGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Home Storage Folder",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = LXTextPrimary
                                )
                            }

                            if (state.hasStoragePermission) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LXPrimaryGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("CONNECTED", color = LXPrimaryGreen, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        Text(
                            text = "Root: ${state.homeFolderPath}\n• Input: ${state.homeFolderPath}/input\n• Output: ${state.homeFolderPath}/output",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LXTextSecondary
                        )

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
                                        "Grant access so LX Track List can read from the input folder and export directly to output.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LXTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { viewModel.requestStoragePermission(context) },
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
                                onClick = { viewModel.exportToHomeOutput(context) },
                                enabled = !state.isExporting,
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (state.isExporting) {
                                    CircularProgressIndicator(color = LXBackgroundBlack, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Exporting to output folder...")
                                } else {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Export Backup to /output Folder", fontWeight = FontWeight.Bold)
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
                                        onClick = { viewModel.importAllLocalFiles(context) },
                                        enabled = !state.isImporting
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
                                                onClick = { viewModel.importLocalFile(context, fileInfo) },
                                                enabled = !state.isImporting,
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

                // Conflict Resolution Strategy Card
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
                        Text(
                            text = "Conflict Resolution Strategy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXPrimaryGreen
                        )
                        Text(
                            text = "Determines how to handle existing entries when importing:",
                            style = MaterialTheme.typography.bodySmall,
                            color = LXTextSecondary
                        )

                        ConflictStrategy.entries.forEach { strategy ->
                            val isSelected = state.selectedConflictStrategy == strategy
                            val desc = when (strategy) {
                                ConflictStrategy.SKIP -> "Keep existing entry, ignore imported"
                                ConflictStrategy.OVERWRITE -> "Replace existing with imported data"
                                ConflictStrategy.MERGE -> "Keep higher progress, newer dates, merge tags"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) LXPrimaryGreen.copy(alpha = 0.12f) else LXSurfaceElevated)
                                    .border(1.dp, if (isSelected) LXPrimaryGreen else LXBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setConflictStrategy(strategy) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) LXPrimaryGreen else LXBorder),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = LXBackgroundBlack, modifier = Modifier.size(12.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = strategy.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) LXPrimaryGreen else LXTextPrimary
                                    )
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
                            text = "Other Storage Locations",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { importLauncher.launch("*/*") },
                                modifier = Modifier.weight(1f).height(42.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LXTextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick File...", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { exportLauncher.launch("LX_Track_List_Backup_${System.currentTimeMillis()}.json") },
                                modifier = Modifier.weight(1f).height(42.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LXTextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save As...", fontSize = 12.sp)
                            }
                        }
                    }
                }

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
                                text = "Version 1.0.0 (Optimized for Phone & Tablet)",
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextSecondary
                            )
                            Text(
                                text = "OnePlus 12R (2780x1264) & OnePlus Pad (2800x2000) Ready",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = LXTextSecondary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
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
