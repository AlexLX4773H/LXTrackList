package com.lx.tracklist.ui.detail

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.lx.tracklist.R
import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import com.lx.tracklist.data.model.RelatedMediaItem
import com.lx.tracklist.data.model.TrackingUnit
import com.lx.tracklist.data.model.UserProfile
import com.lx.tracklist.data.remote.JikanApiClient
import com.lx.tracklist.data.remote.JikanSyncManager
import com.lx.tracklist.data.remote.TachiyomiResolverManager
import com.lx.tracklist.data.remote.TmdbApiClient
import com.lx.tracklist.data.remote.mal.MalSyncManager
import com.lx.tracklist.data.remote.mal.MalTokenStore
import com.lx.tracklist.ui.components.AddRelatedToLibraryDialog
import com.lx.tracklist.ui.components.UserAvatarButton
import com.lx.tracklist.ui.theme.LXAiringBlue
import com.lx.tracklist.ui.theme.LXBackgroundBlack
import com.lx.tracklist.ui.theme.LXBorder
import com.lx.tracklist.ui.theme.LXErrorRed
import com.lx.tracklist.ui.theme.LXPrimaryGreen
import com.lx.tracklist.ui.theme.LXProgressTrack
import com.lx.tracklist.ui.theme.LXScoreGold
import com.lx.tracklist.ui.theme.LXSurfaceBlack
import com.lx.tracklist.ui.theme.LXSurfaceElevated
import com.lx.tracklist.ui.theme.LXSurfaceVariant
import com.lx.tracklist.ui.theme.LXTextPrimary
import com.lx.tracklist.ui.theme.LXTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onNavigateToDetail: ((Long) -> Unit)? = null,
    activeUser: UserProfile? = null,
    onUserAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val entry by viewModel.entry.collectAsState()
    val isDeleted by viewModel.isDeleted.collectAsState()
    val relatedEntries by viewModel.relatedEntries.collectAsState()
    val isLoadingRelated by viewModel.isLoadingRelated.collectAsState()
    val relatedErrorMessage by viewModel.relatedErrorMessage.collectAsState()
    val isRefreshingMetadata by viewModel.isRefreshingMetadata.collectAsState()
    val metadataRefreshMessage by viewModel.metadataRefreshMessage.collectAsState()
    val isPushingToExternalTracker by viewModel.isPushingToExternalTracker.collectAsState()
    val externalSyncMessage by viewModel.externalSyncMessage.collectAsState()
    val vmActiveUser by viewModel.activeUser.collectAsState()
    val effectiveUser = activeUser ?: vmActiveUser
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStartRewatchDialog by remember { mutableStateOf(false) }
    var showRefreshConfirmDialog by remember { mutableStateOf(false) }
    var showChangeStatusOnIncrementDialog by remember { mutableStateOf(false) }
    var relatedItemToAdd by remember { mutableStateOf<RelatedMediaItem?>(null) }
    var previewRelatedItem by remember { mutableStateOf<RelatedMediaItem?>(null) }
    var isAddingRelated by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 720

    LaunchedEffect(entry?.id) {
        entry?.let { viewModel.loadRelatedEntries(it, context) }
    }

    LaunchedEffect(metadataRefreshMessage) {
        metadataRefreshMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearRefreshMessage()
        }
    }

    LaunchedEffect(externalSyncMessage) {
        externalSyncMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearExternalSyncMessage()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.deletedEvent.collect {
            onNavigateBack()
        }
    }

    LaunchedEffect(isDeleted) {
        if (isDeleted) {
            viewModel.resetDeletedState()
            onNavigateBack()
        }
    }

    BackHandler(onBack = onNavigateBack)

    if (showRefreshConfirmDialog && entry != null) {
        val current = entry!!
        val isTachiyomi = JikanSyncManager.isTachiyomiSource(current)
        val isMovieOrShow = current.mediaType == MediaType.MOVIE || current.mediaType == MediaType.SHOW
        val isBook = current.mediaType == MediaType.BOOK
        val hasValidId = JikanSyncManager.hasValidExternalId(current)
        val isMal = JikanSyncManager.isMalSource(current) || current.mediaType == MediaType.ANIME || current.mediaType == MediaType.MANGA || current.mediaType == MediaType.MANHWA
        val sourceName = when {
            isMovieOrShow -> "The Movie Database (TMDB)"
            isBook -> current.externalSource?.takeIf { it.isNotBlank() } ?: "Google Books / Open Library"
            isTachiyomi -> "Jikan / MangaDex / AniList / Kitsu"
            isMal -> "MyAnimeList (MAL)"
            else -> current.externalSource?.takeIf { it.isNotBlank() } ?: "External Database"
        }
        val isFindingAndLinking = isTachiyomi || !hasValidId || TachiyomiResolverManager.isEligibleForResolution(current)
        val extId = current.externalId

        AlertDialog(
            onDismissRequest = { showRefreshConfirmDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isFindingAndLinking) Icons.Default.Search else Icons.Default.Refresh,
                        contentDescription = null,
                        tint = LXPrimaryGreen
                    )
                    Text(
                        text = if (isFindingAndLinking) {
                            when {
                                isMovieOrShow -> "Find & Link TMDB ID?"
                                isBook -> "Find & Link Book ID?"
                                else -> "Find & Link External ID?"
                            }
                        } else "Refresh Metadata?",
                        color = LXTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isFindingAndLinking) {
                            "Search $sourceName for \"${current.title}\" to resolve and link a valid external ID?"
                        } else {
                            "Refresh official metadata for \"${current.title}\" from $sourceName${if (!extId.isNullOrBlank()) " using ID (#$extId)" else " by searching title"}?"
                        },
                        color = LXTextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isFindingAndLinking) {
                            when {
                                isMovieOrShow -> "This will search TMDB to link a valid ID and update poster, release dates, runtime, genres, scores, and synopsis."
                                isBook -> "This will search Google Books and Open Library to link a valid book ID and update cover, pages, publication details, and synopsis."
                                else -> "This will search databases to link a valid external ID (MAL, MangaDex, AniList, Kitsu) and update cover, chapters/episodes, and synopsis. Once linked, external tracking can be enabled."
                            }
                        } else {
                            if (isBook) {
                                "This will update cover, page counts, publisher, published dates, ratings, and description to the latest information from $sourceName."
                            } else {
                                "This will update posters, episode/chapter counts, runtime, broadcast/air dates, scores, and synopsis to the latest information from the external database."
                            }
                        },
                        color = LXTextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRefreshConfirmDialog = false
                        viewModel.refreshMetadataFromExternalSource(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LXPrimaryGreen, contentColor = LXBackgroundBlack)
                ) {
                    Text(if (isFindingAndLinking) "Find & Link" else "Refresh", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefreshConfirmDialog = false }) {
                    Text("Cancel", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceElevated,
            shape = RoundedCornerShape(12.dp)
        )
    }

    if (showChangeStatusOnIncrementDialog && entry != null) {
        val current = entry!!
        val statusLabel = if (current.mediaType.isReadType) "Reading" else "Watching"
        val planLabel = if (current.mediaType.isReadType) "Plan to Read" else "Plan to Watch"

        AlertDialog(
            onDismissRequest = { showChangeStatusOnIncrementDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = LXPrimaryGreen
                    )
                    Text(
                        text = "Change Status to $statusLabel?",
                        color = LXTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "\"${current.title}\" is currently in $planLabel. Would you like to change its status to $statusLabel?",
                        color = LXTextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Adding progress indicates you have started this title.",
                        color = LXTextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showChangeStatusOnIncrementDialog = false
                        viewModel.increment(andChangeStatusTo = MediaStatus.WATCHING)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LXPrimaryGreen,
                        contentColor = LXBackgroundBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Change to $statusLabel", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showChangeStatusOnIncrementDialog = false }) {
                        Text("Cancel", color = LXTextSecondary)
                    }
                    TextButton(onClick = {
                        showChangeStatusOnIncrementDialog = false
                        viewModel.increment()
                    }) {
                        Text("Keep as $planLabel", color = LXTextPrimary)
                    }
                }
            },
            containerColor = LXSurfaceElevated,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LXBackgroundBlack,
        topBar = {
            TopAppBar(
                title = { Text(if (isTablet) (entry?.title ?: "Details") else "Details", color = LXTextPrimary, maxLines = 1) },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = LXPrimaryGreen
                            )
                        }
                        UserAvatarButton(
                            activeUser = activeUser,
                            onClick = onUserAvatarClick,
                            size = 32.dp,
                            paddingStart = 0.dp,
                            paddingEnd = 4.dp
                        )
                    }
                },
                actions = {
                    entry?.let { e ->
                        // Refresh metadata button from external source
                        IconButton(
                            onClick = { showRefreshConfirmDialog = true },
                            enabled = !isRefreshingMetadata
                        ) {
                            if (isRefreshingMetadata) {
                                CircularProgressIndicator(
                                    color = LXPrimaryGreen,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Metadata",
                                    tint = LXPrimaryGreen
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (e.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (e.isFavorite) LXPrimaryGreen else LXTextSecondary
                            )
                        }
                        IconButton(onClick = { onEdit(e.id) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = LXPrimaryGreen
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = LXErrorRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LXBackgroundBlack)
            )
        }
    ) { paddingValues ->
        val currentEntry = entry
        if (currentEntry == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", color = LXTextSecondary)
            }
        } else {
            if (isTablet) {
                // Tablet Two-Pane Layout (e.g. OnePlus Pad)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Column (Cover art & Overview)
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(LXSurfaceBlack)
                                .border(1.dp, LXBorder, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentEntry.coverImageUri.isNullOrBlank()) {
                                SubcomposeAsyncImage(
                                    model = currentEntry.coverImageUri,
                                    contentDescription = currentEntry.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    success = {
                                        SubcomposeAsyncImageContent()
                                    },
                                    loading = {
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(R.drawable.lx_logo),
                                            contentDescription = null,
                                            modifier = Modifier.size(110.dp).clip(CircleShape),
                                            contentScale = ContentScale.Fit
                                        )
                                    },
                                    error = {
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(R.drawable.lx_logo),
                                            contentDescription = null,
                                            modifier = Modifier.size(110.dp).clip(CircleShape),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                )
                            } else {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(R.drawable.lx_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(110.dp).clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        CopyableEntityTitle(title = currentEntry.title)

                        val altTitles = currentEntry.getAlternateTitles()
                        if (altTitles.isNotEmpty()) {
                            altTitles.forEach { alt ->
                                Text(
                                    text = alt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LXTextSecondary
                                )
                            }
                        }

                        // Badges
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetaChip(label = currentEntry.mediaType.displayName, color = LXPrimaryGreen)
                            if (!currentEntry.customCategory.isNullOrBlank()) {
                                MetaChip(label = currentEntry.customCategory, color = LXScoreGold)
                            }
                            if (!currentEntry.subCategory.isNullOrBlank()) {
                                MetaChip(label = currentEntry.subCategory, color = LXScoreGold)
                            }
                            StatusDropdownChip(
                                status = currentEntry.status,
                                mediaType = currentEntry.mediaType,
                                onStatusSelected = { viewModel.updateStatus(it) }
                            )
                            ScoreDropdownChip(
                                score = currentEntry.score,
                                onScoreSelected = { viewModel.updateScore(it) }
                            )
                            if (currentEntry.externalScore != null && currentEntry.externalScore > 0f) {
                                ExternalScoreChip(
                                    score = currentEntry.externalScore,
                                    source = currentEntry.externalScoreSource
                                )
                            }
                            if (currentEntry.mediaType == MediaType.MOVIE && currentEntry.durationMinutes != null && currentEntry.durationMinutes > 0) {
                                val h = currentEntry.durationMinutes / 60
                                val m = currentEntry.durationMinutes % 60
                                val durStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                                MetaChip(label = "⏱️ $durStr", color = LXPrimaryGreen)
                            } else if (currentEntry.episodeLengthMinutes != null && currentEntry.episodeLengthMinutes > 0) {
                                MetaChip(label = "⏱️ ${currentEntry.episodeLengthMinutes} min/ep", color = LXPrimaryGreen)
                            }
                            if (currentEntry.isRewatching) {
                                MetaChip(label = "REWATCHING", color = LXPrimaryGreen)
                            }
                            if (currentEntry.rewatchCount > 0) {
                                MetaChip(label = "Rewatched ${currentEntry.rewatchCount}x", color = LXScoreGold)
                            }
                        }
                    }

                    // Right Column (Progress, Dates, Description, User Comments, Tags)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DetailProgressCard(
                            entry = currentEntry,
                            onIncrement = {
                                if (currentEntry.status == MediaStatus.PLAN_TO_WATCH) {
                                    showChangeStatusOnIncrementDialog = true
                                } else {
                                    viewModel.increment()
                                }
                            },
                            onDecrement = { viewModel.decrement() },
                            onSetProgress = { viewModel.setProgress(it) },
                            onSetShowProgress = { season, ep -> viewModel.setShowProgress(season, ep) },
                            onStartRewatch = { showStartRewatchDialog = true },
                            onFinishRewatch = { viewModel.finishRewatch() },
                            onCancelRewatch = { viewModel.cancelRewatch() }
                        )
                        DetailDatesCard(currentEntry)
                        DetailRelatedCard(
                            relatedEntries = relatedEntries,
                            isLoading = isLoadingRelated,
                            errorMessage = relatedErrorMessage,
                            onRetry = { viewModel.retryLoadRelatedEntries(context) },
                            onItemClick = { item -> previewRelatedItem = item },
                            onAddClick = { item -> relatedItemToAdd = item },
                            onViewInLibraryClick = { entryId -> onNavigateToDetail?.invoke(entryId) }
                        )
                        DetailDescriptionCard(currentEntry)
                        DetailUserCommentsCard(currentEntry)
                        DetailExternalTrackingCard(
                            entry = currentEntry,
                            activeUser = effectiveUser,
                            isPushing = isPushingToExternalTracker,
                            onToggleTracking = { viewModel.setExternalTracking(it) },
                            onPushToExternalTracker = { viewModel.pushToExternalTracker(context) },
                            onRequestLinkExternalId = { showRefreshConfirmDialog = true }
                        )
                        DetailTagsCard(currentEntry)
                        DetailSourceText(currentEntry)
                    }
                }
            } else {
                // Phone Single-Column Layout (e.g. OnePlus 12R)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(LXSurfaceBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentEntry.coverImageUri.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = currentEntry.coverImageUri,
                                contentDescription = currentEntry.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                success = {
                                    SubcomposeAsyncImageContent()
                                },
                                loading = {
                                    androidx.compose.foundation.Image(
                                        painter = painterResource(R.drawable.lx_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(110.dp).clip(CircleShape),
                                        contentScale = ContentScale.Fit
                                    )
                                },
                                error = {
                                    androidx.compose.foundation.Image(
                                        painter = painterResource(R.drawable.lx_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(110.dp).clip(CircleShape),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            )
                        } else {
                            androidx.compose.foundation.Image(
                                painter = painterResource(R.drawable.lx_logo),
                                contentDescription = null,
                                modifier = Modifier.size(110.dp).clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, LXBackgroundBlack),
                                        startY = 100f
                                    )
                                )
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CopyableEntityTitle(title = currentEntry.title)

                        val altTitles = currentEntry.getAlternateTitles()
                        if (altTitles.isNotEmpty()) {
                            altTitles.forEach { alt ->
                                Text(
                                    text = alt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LXTextSecondary
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetaChip(label = currentEntry.mediaType.displayName, color = LXPrimaryGreen)
                            if (!currentEntry.customCategory.isNullOrBlank()) {
                                MetaChip(label = currentEntry.customCategory, color = LXScoreGold)
                            }
                            if (!currentEntry.subCategory.isNullOrBlank()) {
                                MetaChip(label = currentEntry.subCategory, color = LXScoreGold)
                            }
                            StatusDropdownChip(
                                status = currentEntry.status,
                                mediaType = currentEntry.mediaType,
                                onStatusSelected = { viewModel.updateStatus(it) }
                            )
                            ScoreDropdownChip(
                                score = currentEntry.score,
                                onScoreSelected = { viewModel.updateScore(it) }
                            )
                            if (currentEntry.externalScore != null && currentEntry.externalScore > 0f) {
                                ExternalScoreChip(
                                    score = currentEntry.externalScore,
                                    source = currentEntry.externalScoreSource
                                )
                            }
                            if (currentEntry.mediaType == MediaType.MOVIE && currentEntry.durationMinutes != null && currentEntry.durationMinutes > 0) {
                                val h = currentEntry.durationMinutes / 60
                                val m = currentEntry.durationMinutes % 60
                                val durStr = if (h > 0) "${h}h ${m}m" else "${m}m"
                                MetaChip(label = "⏱️ $durStr", color = LXPrimaryGreen)
                            } else if (currentEntry.episodeLengthMinutes != null && currentEntry.episodeLengthMinutes > 0) {
                                MetaChip(label = "⏱️ ${currentEntry.episodeLengthMinutes} min/ep", color = LXPrimaryGreen)
                            }
                            if (currentEntry.isRewatching) {
                                MetaChip(label = "REWATCHING", color = LXPrimaryGreen)
                            }
                            if (currentEntry.rewatchCount > 0) {
                                MetaChip(label = "Rewatched ${currentEntry.rewatchCount}x", color = LXScoreGold)
                            }
                            if (currentEntry.isAiring) {
                                val airingLabel = if (currentEntry.mediaType.isWatchType) "AIRING" else "PUBLISHING"
                                MetaChip(label = airingLabel, color = LXAiringBlue)
                            }
                        }

                        DetailProgressCard(
                            entry = currentEntry,
                            onIncrement = {
                                if (currentEntry.status == MediaStatus.PLAN_TO_WATCH) {
                                    showChangeStatusOnIncrementDialog = true
                                } else {
                                    viewModel.increment()
                                }
                            },
                            onDecrement = { viewModel.decrement() },
                            onSetProgress = { viewModel.setProgress(it) },
                            onSetShowProgress = { season, ep -> viewModel.setShowProgress(season, ep) },
                            onStartRewatch = { showStartRewatchDialog = true },
                            onFinishRewatch = { viewModel.finishRewatch() },
                            onCancelRewatch = { viewModel.cancelRewatch() }
                        )
                        DetailDatesCard(currentEntry)
                        DetailRelatedCard(
                            relatedEntries = relatedEntries,
                            isLoading = isLoadingRelated,
                            errorMessage = relatedErrorMessage,
                            onRetry = { viewModel.retryLoadRelatedEntries(context) },
                            onItemClick = { item -> previewRelatedItem = item },
                            onAddClick = { item -> relatedItemToAdd = item },
                            onViewInLibraryClick = { entryId -> onNavigateToDetail?.invoke(entryId) }
                        )
                        DetailDescriptionCard(currentEntry)
                        DetailUserCommentsCard(currentEntry)
                        DetailExternalTrackingCard(
                            entry = currentEntry,
                            activeUser = effectiveUser,
                            isPushing = isPushingToExternalTracker,
                            onToggleTracking = { viewModel.setExternalTracking(it) },
                            onPushToExternalTracker = { viewModel.pushToExternalTracker(context) },
                            onRequestLinkExternalId = { showRefreshConfirmDialog = true }
                        )
                        DetailTagsCard(currentEntry)
                        DetailSourceText(currentEntry)

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Entry?", color = LXTextPrimary) },
            text = { Text("Are you sure you want to delete this entry? This action cannot be undone.", color = LXTextSecondary) },
            containerColor = LXSurfaceBlack,
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteEntry()
                }) {
                    Text("Delete", color = LXErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = LXTextPrimary)
                }
            }
        )
    }

    val currentEntry = entry
    if (showStartRewatchDialog && currentEntry != null) {
        val isReading = currentEntry.mediaType == MediaType.MANGA ||
                        currentEntry.mediaType == MediaType.MANHWA ||
                        currentEntry.mediaType == MediaType.BOOK
        val actionName = if (isReading) "Re-read" else "Rewatch"
        val actionVerb = if (isReading) "re-reading" else "rewatching"

        AlertDialog(
            onDismissRequest = { showStartRewatchDialog = false },
            title = {
                Text(
                    text = "Start $actionName?",
                    color = LXTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to start $actionVerb \"${currentEntry.title}\"? Your progress will be reset so you can track your new session.",
                    color = LXTextSecondary
                )
            },
            containerColor = LXSurfaceBlack,
            shape = RoundedCornerShape(14.dp),
            confirmButton = {
                Button(
                    onClick = {
                        showStartRewatchDialog = false
                        viewModel.startRewatch()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LXPrimaryGreen,
                        contentColor = LXBackgroundBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Start $actionName",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStartRewatchDialog = false }
                ) {
                    Text("Cancel", color = LXTextSecondary)
                }
            }
        )
    }

    if (previewRelatedItem != null) {
        RelatedItemDetailSheet(
            item = previewRelatedItem!!,
            context = context,
            onDismiss = { previewRelatedItem = null },
            onAddClick = {
                val toAdd = previewRelatedItem
                previewRelatedItem = null
                relatedItemToAdd = toAdd
            },
            onViewInLibrary = { entryId ->
                previewRelatedItem = null
                onNavigateToDetail?.invoke(entryId)
            }
        )
    }

    if (relatedItemToAdd != null) {
        relatedItemToAdd?.let { item ->
            AddRelatedToLibraryDialog(
                item = item,
                isAdding = isAddingRelated,
                onConfirm = { status ->
                    if (!isAddingRelated) {
                        isAddingRelated = true
                        coroutineScope.launch {
                            try {
                                val newId = viewModel.addRelatedToLibrary(item, status, context)
                                relatedItemToAdd = null
                                if (newId > 0) {
                                    onNavigateToDetail?.invoke(newId)
                                }
                            } finally {
                                isAddingRelated = false
                            }
                        }
                    }
                },
                onDismiss = {
                    if (!isAddingRelated) {
                        relatedItemToAdd = null
                    }
                }
            )
        }
    }
}

private data class ProgressInfo(
    val current: Int,
    val total: Int,
    val totalStr: String,
    val unit: String,
    val description: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CopyableEntityTitle(title: String) {
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
        color = LXTextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    clipboardManager.setText(AnnotatedString(title))
                    Toast.makeText(context, "Copied title to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailProgressCard(
    entry: MediaEntry,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSetProgress: (Int) -> Unit = {},
    onSetShowProgress: (Int, Int) -> Unit = { _, _ -> },
    onStartRewatch: () -> Unit,
    onFinishRewatch: () -> Unit,
    onCancelRewatch: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editSeasonInput by remember { mutableStateOf("${entry.currentSeasons}") }
    var editInput by remember { mutableStateOf("") }

    val isVideo = entry.mediaType == MediaType.ANIME || entry.mediaType == MediaType.SHOW
    val isMangaOrBook = entry.mediaType == MediaType.MANGA || entry.mediaType == MediaType.MANHWA || entry.mediaType == MediaType.BOOK
    val isMovie = entry.mediaType == MediaType.MOVIE
    val hasSeasonTracking = entry.hasSeasonEpisodeTracking()
    val seasonCounts = if (hasSeasonTracking) entry.getSeasonEpisodes() else emptyList()

    val info = when (entry.mediaType) {
        MediaType.ANIME -> {
            val total = entry.totalEpisodes
            val totalStr = if (total > 0) "$total" else "?"
            ProgressInfo(entry.currentEpisodes, total, totalStr, "Eps", "Episodes watched")
        }
        MediaType.SHOW -> {
            if (hasSeasonTracking) {
                val seasonTotal = entry.getTotalEpisodesForSeason(entry.currentSeasons)
                val seasonTotalStr = if (seasonTotal > 0) "$seasonTotal" else "?"
                val overallTotal = entry.getEffectiveTotalEpisodes()
                val overallTotalStr = if (overallTotal > 0) "$overallTotal" else "?"
                val watchedTotal = entry.getTotalWatchedEpisodes()
                ProgressInfo(
                    current = entry.currentEpisodes,
                    total = seasonTotal,
                    totalStr = seasonTotalStr,
                    unit = "Eps",
                    description = "S${entry.currentSeasons}/${entry.getEffectiveTotalSeasons()} • Total $watchedTotal/$overallTotalStr eps"
                )
            } else {
                val total = entry.totalEpisodes
                val totalStr = if (total > 0) "$total" else "?"
                val seasonStr = if (entry.currentSeasons > 0) "S${entry.currentSeasons} • " else ""
                ProgressInfo(entry.currentEpisodes, total, totalStr, "Eps", "${seasonStr}Episodes watched")
            }
        }
        MediaType.MANGA, MediaType.MANHWA -> {
            when (entry.trackingUnit) {
                TrackingUnit.VOLUMES -> {
                    val total = entry.totalVolumes
                    val totalStr = if (total > 0) "$total" else "?"
                    ProgressInfo(entry.currentVolumes, total, totalStr, "Vol", "Volumes read")
                }
                TrackingUnit.PAGES -> {
                    val total = entry.totalPages
                    val totalStr = if (total > 0) "$total" else "?"
                    ProgressInfo(entry.currentPages, total, totalStr, "Pages", "Pages read")
                }
                else -> {
                    val total = entry.totalChapters
                    val totalStr = if (total > 0) "$total" else "?"
                    val sub = if (entry.currentVolumes > 0 || entry.totalVolumes > 0) {
                        val vTot = if (entry.totalVolumes > 0) "${entry.totalVolumes}" else "?"
                        "Vol ${entry.currentVolumes}/$vTot • Chapters read"
                    } else {
                        "Chapters read"
                    }
                    ProgressInfo(entry.currentChapters, total, totalStr, "Ch", sub)
                }
            }
        }
        MediaType.BOOK -> {
            when (entry.trackingUnit) {
                TrackingUnit.PAGES -> {
                    val total = entry.totalPages
                    val totalStr = if (total > 0) "$total" else "?"
                    ProgressInfo(entry.currentPages, total, totalStr, "Pages", "Pages read")
                }
                TrackingUnit.CHAPTERS -> {
                    val total = entry.totalChapters
                    val totalStr = if (total > 0) "$total" else "?"
                    ProgressInfo(entry.currentChapters, total, totalStr, "Ch", "Chapters read")
                }
                else -> {
                    val total = entry.totalVolumes
                    val totalStr = if (total > 0) "$total" else "?"
                    ProgressInfo(entry.currentVolumes, total, totalStr, "Vol", "Volumes read")
                }
            }
        }
        MediaType.MOVIE -> {
            val isDone = entry.status == MediaStatus.COMPLETED
            ProgressInfo(if (isDone) 1 else 0, 1, "1", "Film", if (isDone) "Watched" else "Plan to watch")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
        border = BorderStroke(1.dp, LXBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Title on left, Status / Completion badge on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Progress",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )

                if (entry.status == MediaStatus.COMPLETED) {
                    Surface(
                        color = LXPrimaryGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Completed",
                            color = LXPrimaryGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (info.total > 0 || (hasSeasonTracking && entry.getEffectiveTotalEpisodes() > 0)) {
                    val pct = (entry.progressFraction * 100).toInt()
                    Surface(
                        color = LXSurfaceElevated,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, LXBorder)
                    ) {
                        Text(
                            text = "$pct%",
                            color = LXPrimaryGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (entry.isAiring) {
                    val airingLabel = if (entry.mediaType.isWatchType) "Airing" else "Publishing"
                    Surface(
                        color = LXAiringBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, LXAiringBlue.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = airingLabel,
                            color = LXAiringBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Rewatching banner & controls
            if (entry.isRewatching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LXPrimaryGreen.copy(alpha = 0.12f))
                        .border(1.dp, LXPrimaryGreen.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = LXPrimaryGreen, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Rewatching${if (entry.rewatchCount > 0) " (#${entry.rewatchCount + 1})" else ""}",
                            color = LXPrimaryGreen,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onCancelRewatch,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Cancel", color = LXTextSecondary, fontSize = 11.sp)
                        }
                        Button(
                            onClick = onFinishRewatch,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LXPrimaryGreen,
                                contentColor = LXBackgroundBlack
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Finish Rewatch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Hero Progress Row: Number on left, steppers on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            editSeasonInput = "${entry.currentSeasons}"
                            editInput = "${info.current}"
                            showEditDialog = true
                        }
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${info.current}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            ),
                            color = LXPrimaryGreen
                        )
                        Text(
                            text = "/ ${info.totalStr} ${info.unit}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = LXTextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Progress",
                            tint = LXTextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier
                                .size(14.dp)
                                .padding(bottom = 5.dp)
                        )
                    }
                    Text(
                        text = info.description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = LXTextSecondary.copy(alpha = 0.7f)
                    )
                    if ((isVideo || isMovie) && entry.currentTimeMinutes != null && entry.currentTimeMinutes > 0) {
                        Spacer(modifier = Modifier.height(3.dp))
                        val totalRuntime = entry.getEffectiveRuntimeMinutes()
                        val timeStr = if (totalRuntime != null && totalRuntime > 0) {
                            "${entry.formatPlaybackTime() ?: "${entry.currentTimeMinutes}m"} / ${totalRuntime}m (${entry.getPlaybackProgressPercent()}%)"
                        } else {
                            entry.formatPlaybackTime() ?: "${entry.currentTimeMinutes}m"
                        }
                        Text(
                            text = "⏱️ Watched $timeStr",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = LXPrimaryGreen
                        )
                    }
                }

                if (!isMovie) {
                    val canDecrement = info.current > 0 || (hasSeasonTracking && entry.currentSeasons > 1)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onDecrement,
                            enabled = canDecrement,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (canDecrement) LXSurfaceElevated else LXSurfaceElevated.copy(alpha = 0.4f))
                                .border(1.dp, LXBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrement",
                                tint = if (canDecrement) LXTextPrimary else LXTextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Button(
                            onClick = onIncrement,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LXPrimaryGreen,
                                contentColor = LXBackgroundBlack
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "1",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Animated Progress Bar (only rendered when total is known)
            if (info.total > 0 || (hasSeasonTracking && entry.getEffectiveTotalEpisodes() > 0)) {
                val progressAnimated by animateFloatAsState(
                    targetValue = entry.progressFraction,
                    label = "progress_bar_anim"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(LXProgressTrack)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnimated)
                            .clip(RoundedCornerShape(3.dp))
                            .background(LXPrimaryGreen)
                    )
                }
            }

            // Season-by-season breakdown (SHOW only)
            if (entry.mediaType == MediaType.SHOW && hasSeasonTracking) {
                var showSeasonsList by remember(entry.id) { mutableStateOf(seasonCounts.size <= 6) }
                val totalSeasonsCount = entry.getEffectiveTotalSeasons()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LXSurfaceElevated)
                        .border(1.dp, LXBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSeasonsList = !showSeasonsList },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Seasons (${seasonCounts.size}) • S${entry.currentSeasons}: ${entry.currentEpisodes}/${if (info.total > 0) info.total else "?"}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = LXTextPrimary
                        )
                        Icon(
                            imageVector = if (showSeasonsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showSeasonsList) "Hide seasons" else "Show seasons",
                            tint = LXTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = showSeasonsList) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (1..totalSeasonsCount).forEach { sNum ->
                                val sTotal = entry.getTotalEpisodesForSeason(sNum)
                                val isCurrent = sNum == entry.currentSeasons
                                val isCompletedSeason = sNum < entry.currentSeasons ||
                                    (isCurrent && sTotal > 0 && entry.currentEpisodes >= sTotal)
                                val pillBg = when {
                                    isCurrent -> LXPrimaryGreen.copy(alpha = 0.18f)
                                    isCompletedSeason -> LXPrimaryGreen.copy(alpha = 0.08f)
                                    else -> LXSurfaceBlack
                                }
                                val pillBorder = when {
                                    isCurrent -> LXPrimaryGreen
                                    isCompletedSeason -> LXPrimaryGreen.copy(alpha = 0.4f)
                                    else -> LXBorder.copy(alpha = 0.6f)
                                }
                                val labelText = buildString {
                                    append("S$sNum")
                                    if (isCurrent) {
                                        append(" ${entry.currentEpisodes}/${if (sTotal > 0) sTotal else "?"}")
                                    } else if (sTotal > 0) {
                                        append(" • ${sTotal}ep")
                                    }
                                }
                                Surface(
                                    color = pillBg,
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, pillBorder),
                                    modifier = Modifier.clickable {
                                        editSeasonInput = "$sNum"
                                        editInput = if (isCurrent) "${entry.currentEpisodes}" else "0"
                                        showEditDialog = true
                                    }
                                ) {
                                    Text(
                                        text = labelText,
                                        color = if (isCurrent) LXPrimaryGreen else if (isCompletedSeason) LXPrimaryGreen.copy(alpha = 0.85f) else LXTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Airing / Release Status Panel
            if (entry.isAiring && (isVideo || isMangaOrBook)) {
                val (readWatched, airedReleased, total) = if (isVideo) {
                    Triple(entry.getTotalWatchedEpisodes(), entry.airedEpisodes, entry.getEffectiveTotalEpisodes())
                } else {
                    Triple(entry.currentChapters, entry.airedChapters, entry.totalChapters)
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(LXSurfaceElevated)
                            .border(1.dp, LXBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isVideo) "Watched" else "Read",
                                color = LXTextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$readWatched",
                                color = LXPrimaryGreen,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(26.dp)
                                .width(1.dp)
                                .background(LXBorder.copy(alpha = 0.6f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isVideo) "Aired" else "Released",
                                color = LXTextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (airedReleased > 0) "$airedReleased" else "-",
                                color = if (airedReleased > 0) LXAiringBlue else LXTextSecondary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(26.dp)
                                .width(1.dp)
                                .background(LXBorder.copy(alpha = 0.6f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total",
                                color = LXTextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (total > 0) "$total" else "?",
                                color = LXTextPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    if (airedReleased > 0) {
                        val diff = airedReleased - readWatched
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    diff > 0 -> "$diff ${if (isVideo) "episodes" else "chapters"} behind schedule"
                                    diff == 0 -> "Up to date with latest release"
                                    else -> "Ahead of latest release"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (diff > 0) LXAiringBlue else LXPrimaryGreen
                            )
                        }
                    }
                }
            }

            // Start Rewatch button (if completed and not currently rewatching)
            if (entry.status == MediaStatus.COMPLETED && !entry.isRewatching) {
                OutlinedButton(
                    onClick = onStartRewatch,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LXPrimaryGreen),
                    border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = LXPrimaryGreen, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Rewatch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Quick Edit Progress Dialog
    if (showEditDialog) {
        val isShowWithSeasons = entry.mediaType == MediaType.SHOW && (hasSeasonTracking || entry.totalSeasons > 1)
        val previewSeason = editSeasonInput.toIntOrNull()?.coerceIn(1, entry.getEffectiveTotalSeasons()) ?: entry.currentSeasons
        val previewSeasonTotal = entry.getTotalEpisodesForSeason(previewSeason)
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = if (isShowWithSeasons) "Set Season & Episode" else "Set Progress",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LXTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isShowWithSeasons) {
                            val totalSuffix = if (previewSeasonTotal > 0) " (S$previewSeason has $previewSeasonTotal eps)" else ""
                            "Enter current season and episode$totalSuffix:"
                        } else {
                            "Enter current ${info.description.lowercase()}:"
                        },
                        color = LXTextSecondary,
                        fontSize = 13.sp
                    )
                    if (isShowWithSeasons) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editSeasonInput,
                                onValueChange = { if (it.all { c -> c.isDigit() }) editSeasonInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                label = { Text("Season") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LXPrimaryGreen,
                                    unfocusedBorderColor = LXBorder,
                                    focusedLabelColor = LXPrimaryGreen,
                                    focusedTextColor = LXTextPrimary,
                                    unfocusedTextColor = LXTextPrimary,
                                    cursorColor = LXPrimaryGreen
                                )
                            )
                            OutlinedTextField(
                                value = editInput,
                                onValueChange = { if (it.all { c -> c.isDigit() }) editInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                label = { Text(if (previewSeasonTotal > 0) "Ep / $previewSeasonTotal" else "Ep") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LXPrimaryGreen,
                                    unfocusedBorderColor = LXBorder,
                                    focusedLabelColor = LXPrimaryGreen,
                                    focusedTextColor = LXTextPrimary,
                                    unfocusedTextColor = LXTextPrimary,
                                    cursorColor = LXPrimaryGreen
                                )
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = editInput,
                            onValueChange = { if (it.all { c -> c.isDigit() }) editInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            label = { Text(info.unit) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LXPrimaryGreen,
                                unfocusedBorderColor = LXBorder,
                                focusedLabelColor = LXPrimaryGreen,
                                focusedTextColor = LXTextPrimary,
                                unfocusedTextColor = LXTextPrimary,
                                cursorColor = LXPrimaryGreen
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedEp = editInput.toIntOrNull()
                        if (isShowWithSeasons) {
                            val parsedSeason = editSeasonInput.toIntOrNull() ?: entry.currentSeasons
                            if (parsedEp != null) {
                                onSetShowProgress(parsedSeason, parsedEp)
                            }
                        } else if (parsedEp != null) {
                            onSetProgress(parsedEp)
                        }
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LXPrimaryGreen,
                        contentColor = LXBackgroundBlack
                    )
                ) {
                    Text("Set", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceBlack,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun DetailDatesCard(entry: MediaEntry) {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val userStartStr = entry.startDate?.let { sdf.format(Date(it)) }
    val userFinishStr = entry.finishDate?.let { sdf.format(Date(it)) }

    val airedStartStr = entry.airedStartDate?.let { sdf.format(Date(it)) }
    val airedEndStr = if (entry.isAiring && entry.airedEndDate == null) "Present" else entry.airedEndDate?.let { sdf.format(Date(it)) }

    val hasUserData = userStartStr != null || userFinishStr != null
    val hasAiredData = airedStartStr != null || airedEndStr != null

    if (!hasUserData && !hasAiredData) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
        border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasAiredData) {
                if (entry.mediaType == MediaType.MOVIE) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Release Date", color = LXPrimaryGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = airedStartStr ?: airedEndStr ?: "-",
                            color = LXTextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (entry.mediaType == MediaType.BOOK && (airedStartStr == airedEndStr || airedEndStr == null || airedStartStr == null)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Published Date", color = LXPrimaryGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = airedStartStr ?: airedEndStr ?: "-",
                            color = LXTextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val airedTitle = when (entry.mediaType) {
                        MediaType.ANIME, MediaType.SHOW -> "Aired / Broadcast Dates"
                        else -> "Publishing Period"
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(airedTitle, color = LXPrimaryGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("From", color = LXTextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text(airedStartStr ?: "-", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("To", color = LXTextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text(airedEndStr ?: "-", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (hasUserData && hasAiredData) {
                HorizontalDivider(color = LXBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
            }

            if (hasUserData) {
                val userTitle = when (entry.mediaType) {
                    MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK -> "My Reading Dates"
                    else -> "My Watch Dates"
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(userTitle, color = LXPrimaryGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Started", color = LXTextSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(userStartStr ?: "-", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Completed", color = LXTextSecondary, style = MaterialTheme.typography.bodySmall)
                            Text(userFinishStr ?: "-", color = LXTextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailExternalTrackingCard(
    entry: MediaEntry,
    activeUser: UserProfile,
    isPushing: Boolean,
    onToggleTracking: (Boolean) -> Unit,
    onPushToExternalTracker: () -> Unit,
    onRequestLinkExternalId: () -> Unit = {}
) {
    val context = LocalContext.current
    val canTrack = MalSyncManager.canBeExternallyTracked(entry)
    val malTokenStore = remember(context, activeUser.id) { MalTokenStore(context) }
    val isMalLoggedIn = remember(context, activeUser.id) { malTokenStore.isLoggedIn(activeUser.id) }
    val malUsername: String? = remember(context, activeUser.id) { malTokenStore.getMalUsername(activeUser.id) }
    var isExpanded by remember { mutableStateOf(false) }
    var pendingTrackingChange by remember { mutableStateOf<Boolean?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
        border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "External Tracking",
                        tint = if (entry.isExternallyTracked) LXPrimaryGreen else LXTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "External Tracking",
                        color = LXTextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Switch(
                    checked = entry.isExternallyTracked,
                    onCheckedChange = { pendingTrackingChange = it },
                    enabled = canTrack,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LXBackgroundBlack,
                        checkedTrackColor = LXPrimaryGreen,
                        uncheckedThumbColor = LXTextSecondary,
                        uncheckedTrackColor = LXSurfaceElevated
                    )
                )
            }

            if (entry.isExternallyTracked) {
                // Clickable Status Pill that expands downwards on click
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isExpanded = !isExpanded },
                    color = LXPrimaryGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LXPrimaryGreen)
                            )
                            Text(
                                text = if (!entry.externalId.isNullOrBlank()) {
                                    "Sync Active for ${activeUser.name} (#${entry.externalId})"
                                } else {
                                    "Sync Active for ${activeUser.name}"
                                },
                                color = LXPrimaryGreen,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = if (isExpanded) "Collapse details" else "Expand details",
                            tint = LXPrimaryGreen,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (isExpanded) 180f else 0f)
                        )
                    }
                }

                // Downward expansion showing account, ID, and manual send button
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LXSurfaceElevated)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Service",
                                color = LXTextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "MyAnimeList",
                                color = LXTextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MyAnimeList ID",
                                color = LXTextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "#${entry.externalId}",
                                color = LXTextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Syncing To Account",
                                color = LXTextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = if (isMalLoggedIn) "@$malUsername" else "Not Connected",
                                color = if (isMalLoggedIn) LXPrimaryGreen else LXErrorRed,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Manual Send Button
                        Button(
                            onClick = onPushToExternalTracker,
                            enabled = !isPushing && canTrack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LXPrimaryGreen,
                                contentColor = LXBackgroundBlack,
                                disabledContainerColor = LXSurfaceVariant,
                                disabledContentColor = LXTextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isPushing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = LXBackgroundBlack
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sending to MyAnimeList...",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Send Data to External Tracker",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                val isTachiyomi = JikanSyncManager.isTachiyomiSource(entry)
                val hasValidId = JikanSyncManager.hasValidExternalId(entry)
                val isMovieOrShow = entry.mediaType == MediaType.MOVIE || entry.mediaType == MediaType.SHOW
                val isBook = entry.mediaType == MediaType.BOOK
                val isMal = JikanSyncManager.isMalSource(entry) || entry.mediaType == MediaType.ANIME

                if (hasValidId) {
                    val sourceLabel = when {
                        isMovieOrShow || entry.externalSource.equals("TMDB", ignoreCase = true) -> "TMDB"
                        entry.externalSource.equals("MAL", ignoreCase = true) || entry.externalSource.equals("MyAnimeList", ignoreCase = true) -> "MAL"
                        !entry.externalSource.isNullOrBlank() -> entry.externalSource
                        isBook -> "Book"
                        isMal -> "MAL"
                        else -> "External"
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRequestLinkExternalId() },
                        color = LXSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(LXPrimaryGreen)
                                )
                                Text(
                                    text = "$sourceLabel ID Linked (#${entry.externalId})",
                                    color = LXTextPrimary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Refresh",
                                    color = LXPrimaryGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                } else {
                    val unlinkedLabel = when {
                        isTachiyomi -> "Tachiyomi ID (Not Linked)"
                        isMovieOrShow -> "TMDB ID (Not Linked)"
                        isBook -> "Book ID (Not Linked)"
                        isMal -> "MAL ID (Not Linked)"
                        else -> "No External ID Linked"
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRequestLinkExternalId() },
                        color = LXSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(LXScoreGold)
                                )
                                Text(
                                    text = unlinkedLabel,
                                    color = LXTextPrimary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Find & Link ID",
                                    color = LXPrimaryGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingTrackingChange != null) {
        val enabling = pendingTrackingChange == true
        AlertDialog(
            onDismissRequest = { pendingTrackingChange = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = if (enabling) LXPrimaryGreen else LXErrorRed
                    )
                    Text(
                        text = if (enabling) "Add to External Tracking" else "Remove from External Tracking",
                        color = LXTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (enabling) {
                            "Enable external tracking for \"${entry.title}\"?"
                        } else {
                            "Remove \"${entry.title}\" from external tracking?"
                        },
                        color = LXTextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (enabling) {
                            "This entry will sync with MyAnimeList for profile \"${activeUser.name}\". Future progress, score, and status changes will be updated automatically."
                        } else {
                            "Changes will no longer sync to MyAnimeList for profile \"${activeUser.name}\". Your local tracking progress and data will remain intact."
                        },
                        color = LXTextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = pendingTrackingChange!!
                        pendingTrackingChange = null
                        onToggleTracking(target)
                        if (!target) {
                            isExpanded = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (enabling) LXPrimaryGreen else LXErrorRed,
                        contentColor = if (enabling) LXBackgroundBlack else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (enabling) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (enabling) "Add to External Tracking" else "Remove from External Tracking",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTrackingChange = null }) {
                    Text("Cancel", color = LXTextSecondary)
                }
            },
            containerColor = LXSurfaceElevated
        )
    }
}

@Composable
private fun DetailDescriptionCard(entry: MediaEntry) {
    val desc = entry.description ?: entry.notes
    if (!desc.isNullOrBlank()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Description",
                    color = LXPrimaryGreen,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = desc,
                    color = LXTextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DetailUserCommentsCard(entry: MediaEntry) {
    if (!entry.userComments.isNullOrBlank()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, LXBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = null,
                        tint = LXPrimaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "User Comments",
                        color = LXPrimaryGreen,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.userComments,
                    color = LXTextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailTagsCard(entry: MediaEntry) {
    if (!entry.tags.isNullOrBlank()) {
        Column {
            Text("Tags", color = LXTextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                entry.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LXSurfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("#$tag", color = LXPrimaryGreen, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSourceText(entry: MediaEntry) {
    val srcParts = mutableListOf<String>()
    val isTmdb = entry.externalSource?.equals("TMDB", ignoreCase = true) == true || entry.externalScoreSource?.equals("TMDB", ignoreCase = true) == true
    if (!entry.externalSource.isNullOrBlank()) {
        val srcName = if (entry.externalSource.equals("TACHIYOMI", ignoreCase = true)) "Tachiyomi" else entry.externalSource
        srcParts.add("Imported from $srcName" + if (entry.externalId != null) " (ID: ${entry.externalId})" else "")
    }
    if (isTmdb) {
        srcParts.add("Data from TMDB")
    }
    if (entry.externalScore != null && entry.externalScore > 0f) {
        val src = entry.externalScoreSource?.takeIf { it.isNotBlank() } ?: "MAL"
        val scoreStr = if (entry.externalScore % 1.0f == 0.0f) {
            entry.externalScore.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", entry.externalScore).trimEnd('0').trimEnd('.')
        }
        srcParts.add("$src Rating: ★ $scoreStr")
    }
    if (srcParts.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isTmdb) {
                    Image(
                        painter = painterResource(R.drawable.ic_tmdb_logo),
                        contentDescription = "TMDB",
                        modifier = Modifier.height(14.dp).width(20.dp)
                    )
                }
                Text(
                    text = srcParts.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = LXTextSecondary.copy(alpha = 0.7f)
                )
            }
            if (isTmdb) {
                Text(
                    text = "This product uses the TMDB API but is not endorsed or certified by TMDB.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = LXTextSecondary.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun detailPillHeight(): Dp {
    val lineHeightDp = with(LocalDensity.current) {
        MaterialTheme.typography.labelMedium.lineHeight.toDp()
    }
    return maxOf(26.dp, lineHeightDp + 10.dp)
}

@Composable
private fun DetailPill(
    backgroundColor: Color,
    borderColor: Color,
    horizontalPadding: PaddingValues = PaddingValues(horizontal = 8.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    val baseModifier = Modifier
        .height(detailPillHeight())
        .clip(shape)
        .background(backgroundColor)
        .border(width = 1.dp, color = borderColor, shape = shape)
    val clickableModifier = if (onClick != null) {
        baseModifier.clickable(onClick = onClick)
    } else {
        baseModifier
    }
    Row(
        modifier = clickableModifier.padding(horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun ExternalScoreChip(
    score: Float,
    source: String?
) {
    val src = source?.takeIf { it.isNotBlank() } ?: "MAL"
    val scoreStr = if (score % 1.0f == 0.0f) {
        score.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", score)
    }
    DetailPill(
        backgroundColor = LXScoreGold.copy(alpha = 0.12f),
        borderColor = LXScoreGold.copy(alpha = 0.35f)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = LXScoreGold,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "$scoreStr ($src)",
            color = LXScoreGold,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MetaChip(label: String, color: Color) {
    DetailPill(
        backgroundColor = color.copy(alpha = 0.15f),
        borderColor = color.copy(alpha = 0.4f)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusDropdownChip(
    status: MediaStatus,
    mediaType: MediaType,
    onStatusSelected: (MediaStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (status) {
        MediaStatus.COMPLETED -> LXPrimaryGreen
        MediaStatus.WATCHING -> LXPrimaryGreen
        MediaStatus.ON_HOLD -> LXScoreGold
        MediaStatus.DROPPED -> LXErrorRed.copy(alpha = 0.85f)
        MediaStatus.PLAN_TO_WATCH -> LXTextSecondary
    }

    Box {
        DetailPill(
            backgroundColor = statusColor.copy(alpha = 0.15f),
            borderColor = statusColor.copy(alpha = 0.45f),
            horizontalPadding = PaddingValues(start = 8.dp, end = 4.dp),
            onClick = { expanded = true }
        ) {
            Text(
                text = status.getDisplayLabel(mediaType),
                color = statusColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Change Status",
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(LXSurfaceElevated)
        ) {
            MediaStatus.entries.forEach { item ->
                val isSelected = item == status
                val itemLabel = item.getDisplayLabel(mediaType)
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = LXPrimaryGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = itemLabel,
                                color = if (isSelected) LXPrimaryGreen else LXTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onStatusSelected(item)
                    }
                )
            }
        }
    }
}

@Composable
private fun ScoreDropdownChip(
    score: Int?,
    onScoreSelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val hasScore = score != null && score > 0

    Box {
        DetailPill(
            backgroundColor = if (hasScore) LXScoreGold.copy(alpha = 0.15f) else LXSurfaceElevated,
            borderColor = if (hasScore) LXScoreGold.copy(alpha = 0.45f) else LXBorder,
            horizontalPadding = PaddingValues(start = 8.dp, end = 4.dp),
            onClick = { expanded = true }
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (hasScore) LXScoreGold else LXTextSecondary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (hasScore) "$score/10" else "Rate",
                color = if (hasScore) LXScoreGold else LXTextSecondary,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Score",
                tint = if (hasScore) LXScoreGold else LXTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(LXSurfaceElevated)
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "— Clear / Unrated",
                        color = if (score == null) LXPrimaryGreen else LXTextSecondary,
                        fontWeight = if (score == null) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = {
                    expanded = false
                    onScoreSelected(null)
                }
            )
            (10 downTo 1).forEach { s ->
                val isSelected = score == s
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isSelected) LXScoreGold else LXTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "$s / 10",
                                color = if (isSelected) LXScoreGold else LXTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onScoreSelected(s)
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailRelatedCard(
    relatedEntries: List<RelatedMediaItem>,
    isLoading: Boolean,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    onItemClick: (RelatedMediaItem) -> Unit,
    onAddClick: (RelatedMediaItem) -> Unit,
    onViewInLibraryClick: (Long) -> Unit
) {
    val trackingCount = remember(relatedEntries) { relatedEntries.count { it.inLibraryEntryId != null } }
    val untrackedCount = remember(relatedEntries) { relatedEntries.size - trackingCount }
    var filterTab by remember { mutableStateOf("ALL") } // "ALL", "TRACKING", "UNTRACKED"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LXSurfaceBlack),
        border = BorderStroke(1.dp, LXBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Related",
                        color = LXPrimaryGreen,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    if (relatedEntries.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(LXSurfaceElevated)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = relatedEntries.size.toString(),
                                color = LXTextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = LXPrimaryGreen
                    )
                }
            }

            if (relatedEntries.isEmpty()) {
                if (isLoading) {
                    Text(
                        text = "Checking for related entries...",
                        color = LXTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = LXTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    if (onRetry != null) {
                        OutlinedButton(
                            onClick = onRetry,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LXPrimaryGreen),
                            border = BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = LXPrimaryGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "There are no related entries.",
                            color = LXTextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        if (onRetry != null) {
                            TextButton(
                                onClick = onRetry,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Check for related entries", tint = LXPrimaryGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Check Again", color = LXPrimaryGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                // Tracking vs Untracked Stats Pills & Filter Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // All Pill Tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (filterTab == "ALL") LXPrimaryGreen.copy(alpha = 0.2f) else LXSurfaceElevated)
                            .border(1.dp, if (filterTab == "ALL") LXPrimaryGreen else LXBorder, RoundedCornerShape(6.dp))
                            .clickable { filterTab = "ALL" }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "All (${relatedEntries.size})",
                            color = if (filterTab == "ALL") LXPrimaryGreen else LXTextSecondary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Tracking Pill Tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (filterTab == "TRACKING") LXPrimaryGreen.copy(alpha = 0.25f) else LXSurfaceElevated)
                            .border(1.dp, if (filterTab == "TRACKING") LXPrimaryGreen else LXBorder, RoundedCornerShape(6.dp))
                            .clickable { filterTab = "TRACKING" }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Tracking ($trackingCount)",
                            color = if (filterTab == "TRACKING") LXPrimaryGreen else LXPrimaryGreen.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Not Tracking Pill Tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (filterTab == "UNTRACKED") Color(0xFFF59E0B).copy(alpha = 0.25f) else LXSurfaceElevated)
                            .border(1.dp, if (filterTab == "UNTRACKED") Color(0xFFF59E0B) else LXBorder, RoundedCornerShape(6.dp))
                            .clickable { filterTab = "UNTRACKED" }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Not Tracking ($untrackedCount)",
                            color = if (filterTab == "UNTRACKED") Color(0xFFF59E0B) else LXTextSecondary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                val currentFiltered = when (filterTab) {
                    "TRACKING" -> relatedEntries.filter { it.inLibraryEntryId != null }
                    "UNTRACKED" -> relatedEntries.filter { it.inLibraryEntryId == null }
                    else -> relatedEntries
                }

                if (currentFiltered.isEmpty()) {
                    Text(
                        text = if (filterTab == "TRACKING") "No related entries currently in your library" else "All related entries are already in your library!",
                        color = LXTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    val displayList = if (expanded || currentFiltered.size <= 4) currentFiltered else currentFiltered.take(4)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayList.forEach { item ->
                            RelatedItemRow(
                                item = item,
                                onClick = { onItemClick(item) },
                                onAddClick = { onAddClick(item) },
                                onViewInLibraryClick = { onViewInLibraryClick(it) }
                            )
                        }

                        if (currentFiltered.size > 4) {
                            TextButton(
                                onClick = { expanded = !expanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (expanded) "Show Less" else "Show All (${currentFiltered.size})",
                                    color = LXPrimaryGreen,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedItemRow(
    item: RelatedMediaItem,
    onClick: () -> Unit,
    onAddClick: () -> Unit,
    onViewInLibraryClick: (Long) -> Unit
) {
    val isInLibrary = item.inLibraryEntryId != null
    val relColor = when {
        item.relationship.contains("Sequel", ignoreCase = true) -> LXPrimaryGreen
        item.relationship.contains("Prequel", ignoreCase = true) -> Color(0xFF38BDF8)
        item.relationship.contains("Collection", ignoreCase = true) || item.relationship.contains("Part", ignoreCase = true) -> LXScoreGold
        item.relationship.contains("Spin", ignoreCase = true) || item.relationship.contains("Side", ignoreCase = true) -> Color(0xFFA78BFA)
        else -> LXTextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LXSurfaceElevated.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail poster
        Box(
            modifier = Modifier
                .size(40.dp, 56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(LXSurfaceBlack),
            contentAlignment = Alignment.Center
        ) {
            if (!item.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = when {
                        item.mediaType == MediaType.BOOK -> Icons.Default.Book
                        item.mediaType == MediaType.MOVIE -> Icons.Default.Movie
                        else -> Icons.Default.Tv
                    },
                    contentDescription = null,
                    tint = LXTextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Relationship tag
                val displayRel = when {
                    item.relationship.equals("Alternative Version", ignoreCase = true) || item.relationship.equals("Alternative", ignoreCase = true) -> "ALT VERSION"
                    item.relationship.equals("Part of Series", ignoreCase = true) -> "SERIES"
                    item.relationship.equals("Parent Story", ignoreCase = true) -> "PARENT"
                    else -> item.relationship.uppercase()
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(relColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = displayRel,
                        color = relColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        fontSize = 9.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (!item.releaseYear.isNullOrBlank()) {
                    Text(
                        text = item.releaseYear,
                        color = LXTextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (item.score != null && item.score > 0f) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = LXScoreGold,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", item.score),
                            color = LXScoreGold,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Text(
                text = item.title,
                color = LXTextPrimary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Action / Status Pill
        if (isInLibrary) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(LXPrimaryGreen.copy(alpha = 0.15f))
                    .clickable { onViewInLibraryClick(item.inLibraryEntryId) }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = LXPrimaryGreen,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = item.inLibraryStatus?.getDisplayLabel(item.mediaType) ?: "In Library",
                    color = LXPrimaryGreen,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "View",
                    tint = LXPrimaryGreen,
                    modifier = Modifier.size(11.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(LXPrimaryGreen.copy(alpha = 0.15f))
                    .border(BorderStroke(1.dp, LXPrimaryGreen.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
                    .clickable { onAddClick() }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = LXPrimaryGreen,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Add",
                    color = LXPrimaryGreen,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

private data class RelatedDetailedInfo(
    val title: String? = null,
    val alternateTitle: String? = null,
    val synopsis: String? = null,
    val tags: String? = null,
    val posterUrl: String? = null,
    val score: Float? = null,
    val scoreSource: String? = null,
    val releaseYear: String? = null,
    val durationMinutes: Int? = null,
    val totalUnits: Int? = null,
    val totalVolumes: Int? = null,
    val statusText: String? = null,
    val isAiring: Boolean? = null,
    val externalId: String? = null,
    val externalSource: String? = null,
    val webUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RelatedItemDetailSheet(
    item: RelatedMediaItem,
    context: Context,
    onDismiss: () -> Unit,
    onAddClick: () -> Unit,
    onViewInLibrary: (Long) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
            override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero
        }
    }

    var detailedInfo by remember(item.externalId, item.externalSource) { mutableStateOf<RelatedDetailedInfo?>(null) }
    var isLoadingDetails by remember(item.externalId, item.externalSource) { mutableStateOf(true) }

    LaunchedEffect(item.externalId, item.externalSource) {
        isLoadingDetails = true
        withContext(Dispatchers.IO) {
            try {
                if (item.externalSource.equals("TMDB", ignoreCase = true)) {
                    val tmdbId = item.externalId.toLongOrNull() ?: 0L
                    if (item.mediaType == MediaType.MOVIE) {
                        val full = TmdbApiClient.fetchMovieDetails(tmdbId, context).getOrNull()
                        val yr = full?.airedStartDate?.let {
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = it }
                            cal.get(Calendar.YEAR).toString()
                        }
                        val resolvedExtId = if (tmdbId > 0) tmdbId.toString() else item.externalId.takeIf { it.isNotBlank() && it != "0" }
                        val resolvedUrl = if (tmdbId > 0) "https://www.themoviedb.org/movie/$tmdbId" else null
                        detailedInfo = RelatedDetailedInfo(
                            title = full?.title,
                            alternateTitle = full?.alternateTitle,
                            synopsis = full?.synopsis,
                            tags = full?.tags,
                            posterUrl = full?.posterUrl,
                            score = full?.externalScore,
                            scoreSource = "TMDB",
                            releaseYear = yr,
                            durationMinutes = full?.durationMinutes,
                            statusText = "Movie",
                            externalId = resolvedExtId,
                            externalSource = "TMDB",
                            webUrl = resolvedUrl
                        )
                    } else {
                        val full = TmdbApiClient.fetchTvDetails(tmdbId, context).getOrNull()
                        val yr = full?.airedStartDate?.let {
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = it }
                            cal.get(Calendar.YEAR).toString()
                        }
                        val resolvedExtId = if (tmdbId > 0) tmdbId.toString() else item.externalId.takeIf { it.isNotBlank() && it != "0" }
                        val resolvedUrl = if (tmdbId > 0) "https://www.themoviedb.org/tv/$tmdbId" else null
                        detailedInfo = RelatedDetailedInfo(
                            title = full?.title,
                            alternateTitle = full?.alternateTitle,
                            synopsis = full?.synopsis,
                            tags = full?.tags,
                            posterUrl = full?.posterUrl,
                            score = full?.externalScore,
                            scoreSource = "TMDB",
                            releaseYear = yr,
                            durationMinutes = full?.durationMinutes,
                            totalUnits = full?.totalEpisodes,
                            isAiring = full?.isAiring,
                            statusText = if (full?.isAiring == true) "Airing" else "Finished",
                            externalId = resolvedExtId,
                            externalSource = "TMDB",
                            webUrl = resolvedUrl
                        )
                    }
                } else if (item.externalSource.equals("Google Books", ignoreCase = true)) {
                    val full = com.lx.tracklist.data.remote.GoogleBooksApiClient.fetchVolumeDetails(item.externalId, context).getOrNull()
                        ?: com.lx.tracklist.data.remote.BookMetadataManager.searchBooks(item.title, context, limit = 1).getOrNull()?.firstOrNull()
                    val yr = full?.publishedDate?.take(4) ?: item.releaseYear
                    detailedInfo = RelatedDetailedInfo(
                        title = full?.title ?: item.title,
                        alternateTitle = full?.subtitle,
                        synopsis = full?.description,
                        tags = full?.categories?.joinToString(", "),
                        posterUrl = full?.coverUrl ?: item.posterUrl,
                        score = full?.averageScore ?: item.score,
                        scoreSource = "Google Books",
                        releaseYear = yr,
                        durationMinutes = null,
                        totalUnits = full?.pageCount,
                        statusText = "Book",
                        externalId = item.externalId,
                        externalSource = "Google Books",
                        webUrl = full?.infoUrl ?: "https://books.google.com/books?id=${item.externalId}"
                    )
                } else if (item.externalSource.equals("Open Library", ignoreCase = true)) {
                    val workKey = item.externalId.removePrefix("/works/").removePrefix("/").trim()
                    val full = com.lx.tracklist.data.remote.OpenLibraryApiClient.fetchWorkDetails(workKey).getOrNull()
                        ?: com.lx.tracklist.data.remote.BookMetadataManager.searchBooks(item.title, context, limit = 1).getOrNull()?.firstOrNull()
                    val yr = full?.publishedDate?.take(4) ?: item.releaseYear
                    detailedInfo = RelatedDetailedInfo(
                        title = full?.title ?: item.title,
                        alternateTitle = full?.subtitle,
                        synopsis = full?.description,
                        tags = full?.categories?.joinToString(", "),
                        posterUrl = full?.coverUrl ?: item.posterUrl,
                        score = full?.averageScore ?: item.score,
                        scoreSource = "Open Library",
                        releaseYear = yr,
                        durationMinutes = null,
                        totalUnits = full?.pageCount,
                        statusText = "Book",
                        externalId = item.externalId,
                        externalSource = "Open Library",
                        webUrl = full?.infoUrl ?: "https://openlibrary.org/works/$workKey"
                    )
                } else {
                    val malId = item.externalId.toLongOrNull() ?: 0L
                    val isManga = item.mediaType == MediaType.MANGA || item.mediaType == MediaType.MANHWA || item.mediaType == MediaType.BOOK
                    var full = if (malId > 0) JikanApiClient.fetchMediaDetails(malId, isManga).getOrNull() else null
                    if (full == null && item.title.isNotBlank()) {
                        full = JikanApiClient.queryAniListByTitle(item.title, isManga).getOrNull()
                    }
                    val yr = full?.airedStartDate?.let {
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = it }
                        cal.get(Calendar.YEAR).toString()
                    }
                    val resolvedMalId = full?.malId?.takeIf { it > 0 } ?: malId.takeIf { it > 0 }
                    val resolvedExtId = resolvedMalId?.toString() ?: item.externalId.takeIf { it.isNotBlank() && it != "0" }
                    val resolvedSource = if (resolvedMalId != null) {
                        "MAL"
                    } else {
                        full?.externalScoreSource ?: item.externalSource.ifBlank { "MAL" }
                    }
                    val typePath = if (isManga) "manga" else "anime"
                    val resolvedWebUrl = full?.webUrl ?: run {
                        if (resolvedSource.equals("AniList", ignoreCase = true) && resolvedExtId != null) {
                            "https://anilist.co/$typePath/$resolvedExtId"
                        } else if (resolvedMalId != null) {
                            "https://myanimelist.net/$typePath/$resolvedMalId"
                        } else null
                    }
                    detailedInfo = RelatedDetailedInfo(
                        title = full?.title,
                        alternateTitle = full?.alternateTitle,
                        synopsis = full?.synopsis,
                        tags = full?.tags,
                        posterUrl = full?.posterUrl,
                        score = full?.externalScore,
                        scoreSource = full?.externalScoreSource ?: "MAL",
                        releaseYear = yr,
                        durationMinutes = full?.durationMinutes,
                        totalUnits = if (isManga) full?.totalChapters else full?.totalEpisodes,
                        totalVolumes = full?.totalVolumes,
                        isAiring = full?.isAiring,
                        statusText = full?.type ?: item.mediaType.displayName,
                        externalId = resolvedExtId,
                        externalSource = resolvedSource,
                        webUrl = resolvedWebUrl
                    )
                }
            } catch (_: Exception) {}
            isLoadingDetails = false
        }
    }

    val displayTitle = detailedInfo?.title ?: item.title
    val displayPoster = detailedInfo?.posterUrl ?: item.posterUrl
    val displayScore = detailedInfo?.score ?: item.score
    val displayScoreSource = detailedInfo?.scoreSource ?: item.externalSource
    val displayYear = detailedInfo?.releaseYear ?: item.releaseYear
    val displayAlternate = detailedInfo?.alternateTitle
    val displaySynopsis = detailedInfo?.synopsis
    val displayTags = detailedInfo?.tags
    val displayExternalId = detailedInfo?.externalId ?: item.externalId.takeIf { it.isNotBlank() && it != "0" }
    val displayExternalSource = detailedInfo?.externalSource ?: if (item.externalSource.equals("TMDB", ignoreCase = true)) "TMDB" else "MAL"
    val displayWebUrl = detailedInfo?.webUrl ?: run {
        if (displayExternalId != null) {
            if (displayExternalSource.equals("TMDB", ignoreCase = true)) {
                val typeStr = if (item.mediaType == MediaType.MOVIE) "movie" else "tv"
                "https://www.themoviedb.org/$typeStr/$displayExternalId"
            } else if (displayExternalSource.equals("MangaDex", ignoreCase = true)) {
                "https://mangadex.org/title/$displayExternalId"
            } else if (displayExternalSource.equals("Kitsu", ignoreCase = true)) {
                "https://kitsu.app/manga/$displayExternalId"
            } else {
                val isManga = item.mediaType == MediaType.MANGA || item.mediaType == MediaType.MANHWA || item.mediaType == MediaType.BOOK
                val typeStr = if (isManga) "manga" else "anime"
                if (displayExternalSource.equals("AniList", ignoreCase = true)) {
                    "https://anilist.co/$typeStr/$displayExternalId"
                } else {
                    "https://myanimelist.net/$typeStr/$displayExternalId"
                }
            }
        } else null
    }

    val relColor = when {
        item.relationship.contains("Sequel", ignoreCase = true) -> LXPrimaryGreen
        item.relationship.contains("Prequel", ignoreCase = true) -> Color(0xFF38BDF8)
        item.relationship.contains("Collection", ignoreCase = true) || item.relationship.contains("Part", ignoreCase = true) -> LXScoreGold
        item.relationship.contains("Spin", ignoreCase = true) || item.relationship.contains("Side", ignoreCase = true) -> Color(0xFFA78BFA)
        else -> LXTextSecondary
    }

    val unitsInfo = when (item.mediaType) {
        MediaType.ANIME, MediaType.SHOW -> {
            val eps = detailedInfo?.totalUnits?.takeIf { it > 0 }?.let { "$it Episodes" }
            val dur = detailedInfo?.durationMinutes?.takeIf { it > 0 }?.let { "${it}m/ep" }
            listOfNotNull(eps, dur).joinToString(" • ").takeIf { it.isNotBlank() }
        }
        MediaType.MOVIE -> {
            detailedInfo?.durationMinutes?.takeIf { it > 0 }?.let { "${it}m runtime" }
        }
        MediaType.MANGA, MediaType.MANHWA, MediaType.BOOK -> {
            val chs = detailedInfo?.totalUnits?.takeIf { it > 0 }?.let { "$it Chapters" }
            val vols = detailedInfo?.totalVolumes?.takeIf { it > 0 }?.let { "$it Volumes" }
            listOfNotNull(chs, vols).joinToString(" • ").takeIf { it.isNotBlank() }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LXSurfaceBlack,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(LXBorder)
            )
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(nestedScrollConnection)
        ) {
            val maxSheetHeight = maxHeight * 0.90f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Poster + Title + Metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Poster
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(148.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(LXSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!displayPoster.isNullOrBlank()) {
                            AsyncImage(
                                model = displayPoster,
                                contentDescription = displayTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = if (item.mediaType == MediaType.MOVIE) Icons.Default.Movie else Icons.Default.Tv,
                                contentDescription = null,
                                tint = LXTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Info Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = LXTextPrimary
                        )

                        if (!displayAlternate.isNullOrBlank() && !displayAlternate.equals(displayTitle, ignoreCase = true)) {
                            Text(
                                text = displayAlternate.replace("\n", " • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextSecondary.copy(alpha = 0.8f)
                            )
                        }

                        // Chips row (Relationship, Format, Year, Score)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = relColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = item.relationship.uppercase(),
                                    color = relColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = LXPrimaryGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = detailedInfo?.statusText ?: item.mediaType.displayName,
                                    color = LXPrimaryGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (!displayYear.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = LXSurfaceElevated
                                ) {
                                    Text(
                                        text = displayYear,
                                        color = LXTextSecondary,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (displayScore != null && displayScore > 0f) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = LXScoreGold.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = LXScoreGold,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", displayScore)} ($displayScoreSource)",
                                            color = LXScoreGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (!displayExternalId.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = LXSurfaceElevated,
                                    border = BorderStroke(1.dp, if (!displayWebUrl.isNullOrBlank()) LXPrimaryGreen.copy(alpha = 0.4f) else LXBorder),
                                    modifier = Modifier.clickable {
                                        if (!displayWebUrl.isNullOrBlank()) {
                                            try {
                                                uriHandler.openUri(displayWebUrl)
                                            } catch (_: Exception) {
                                                clipboardManager.setText(AnnotatedString(displayExternalId))
                                                Toast.makeText(context, "Copied ID $displayExternalId to clipboard", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            clipboardManager.setText(AnnotatedString(displayExternalId))
                                            Toast.makeText(context, "Copied ID $displayExternalId to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$displayExternalSource ID: $displayExternalId",
                                            color = LXTextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (!displayWebUrl.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = "Open in $displayExternalSource",
                                                tint = LXPrimaryGreen,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!unitsInfo.isNullOrBlank()) {
                            Text(
                                text = unitsInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = LXTextSecondary
                            )
                        }
                    }
                }

                // Tags / Genres
                if (!displayTags.isNullOrBlank()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        displayTags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = LXSurfaceElevated,
                                border = BorderStroke(0.5.dp, LXBorder)
                            ) {
                                Text(
                                    text = "#$tag",
                                    color = LXTextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Synopsis Card
                if (isLoadingDetails) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = LXPrimaryGreen,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Loading overview & details...",
                            color = LXTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else if (!displaySynopsis.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LXSurfaceElevated),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, LXBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Synopsis / Overview",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = LXPrimaryGreen
                            )
                            Text(
                                text = displaySynopsis,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = LXTextPrimary
                            )
                        }
                    }
                }

                // TMDB Attribution Notice
                if (item.externalSource == "TMDB") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LXSurfaceElevated)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_tmdb_logo),
                            contentDescription = "TMDB",
                            modifier = Modifier.height(14.dp).width(20.dp)
                        )
                        Text(
                            text = "This product uses the TMDB API but is not endorsed or certified by TMDB.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = LXTextSecondary.copy(alpha = 0.6f)
                        )
                    }
                }

                // Bottom Actions
                if (!displayWebUrl.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            try {
                                uriHandler.openUri(displayWebUrl)
                            } catch (_: Exception) {
                                clipboardManager.setText(AnnotatedString(displayExternalId ?: ""))
                                Toast.makeText(context, "Copied ID $displayExternalId", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = LXTextPrimary
                        ),
                        border = BorderStroke(1.dp, LXBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = LXPrimaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open on $displayExternalSource" + (displayExternalId?.let { " (ID: $it)" } ?: ""),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (item.inLibraryEntryId != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onViewInLibrary(item.inLibraryEntryId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LXPrimaryGreen,
                            contentColor = LXBackgroundBlack
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View in Library (${item.inLibraryStatus?.getDisplayLabel(item.mediaType) ?: "In Library"})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            onDismiss()
                            onAddClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LXPrimaryGreen,
                            contentColor = LXBackgroundBlack
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add to Library",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
