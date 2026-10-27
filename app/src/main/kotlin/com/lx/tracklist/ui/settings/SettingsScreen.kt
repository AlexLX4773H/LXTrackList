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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lx.tracklist.R
import com.lx.tracklist.data.repository.ConflictStrategy
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXErrorRed
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
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

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importFile(context, it) }
    }

    // Document creator for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportToJson(context, it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LXBackgroundBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Data",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LXTextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LXBackgroundBlack)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Banner if any
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

            // Data Import Card
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
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = LXPrimaryGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Import Data",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }

                    Text(
                        text = "Import anime/manga lists from MyAnimeList (.xml, .xml.gz) or generic CSV/JSON files. Fully local, no login required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LXTextSecondary
                    )

                    // Conflict Strategy Selection
                    Text(
                        text = "Conflict Resolution Strategy",
                        style = MaterialTheme.typography.labelLarge,
                        color = LXPrimaryGreen
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    .border(
                                        1.dp,
                                        if (isSelected) LXPrimaryGreen else LXBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setConflictStrategy(strategy) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = LXBackgroundBlack,
                                            modifier = Modifier.size(14.dp)
                                        )
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

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { importLauncher.launch("*/*") },
                        enabled = !state.isImporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LXPrimaryGreen,
                            contentColor = LXBackgroundBlack
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isImporting) {
                            CircularProgressIndicator(
                                color = LXBackgroundBlack,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importing entries...")
                        } else {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select File to Import", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Data Export Card
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
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = LXPrimaryGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Export Backup",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )
                    }

                    Text(
                        text = "Export your entire database to a JSON backup file on your device. You can restore this file anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LXTextSecondary
                    )

                    OutlinedButton(
                        onClick = { exportLauncher.launch("LX_Track_List_Backup_${System.currentTimeMillis()}.json") },
                        enabled = !state.isExporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LXPrimaryGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LXPrimaryGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isExporting) {
                            CircularProgressIndicator(
                                color = LXPrimaryGreen,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exporting backup...")
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export JSON Backup", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // App & Logo Information Card
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
                            text = "Version 1.0.0 (MVP)",
                            style = MaterialTheme.typography.bodySmall,
                            color = LXTextSecondary
                        )
                        Text(
                            text = "Personal Media Tracker for Anime, Manga, Manhwa, Movies, Shows & Books",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LXTextSecondary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
