package com.ammu.player.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ammu.player.crypto.ImportPermissions
import com.ammu.player.crypto.SecuritySuite
import com.ammu.player.data.local.entity.PlaylistEntity
import com.ammu.player.data.local.entity.TrackEntity
import com.ammu.player.ui.components.*
import com.ammu.player.ui.dialogs.*
import com.ammu.player.ui.theme.*
import com.ammu.player.ui.viewmodel.AmmuMainViewModel
import com.ammu.player.ui.viewmodel.AppDialog
import com.ammu.player.ui.viewmodel.SortMode

@Composable
fun AmmuMainScreen(
    viewModel: AmmuMainViewModel,
    onOpenFullscreenPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val playlists by viewModel.playlists.collectAsState()
    val activePlaylistId by viewModel.activePlaylistId.collectAsState()
    val displayedTracks by viewModel.displayedTracks.collectAsState()
    val rawTracks by viewModel.rawTracks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val multiSelectMode by viewModel.multiSelectMode.collectAsState()
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsState()
    val undoAction by viewModel.undoAction.collectAsState()

    val currentTrack by viewModel.playbackManager.currentTrack.collectAsState()
    val isPlaying by viewModel.playbackManager.isPlaying.collectAsState()

    val heartBurstKey by viewModel.heartBurstKey.collectAsState()
    val isLastActionLiked by viewModel.isLastActionLiked.collectAsState()
    val emotionalToastKey by viewModel.emotionalToastKey.collectAsState()

    val dialogState by viewModel.dialogState.collectAsState()
    val storageAuditSummary by viewModel.storageAuditSummary.collectAsState()
    val isScanningStorage by viewModel.isScanningStorage.collectAsState()

    // File picker launcher for adding audio files
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importAudioFiles(uris)
            Toast.makeText(context, "Added ${uris.size} audio file(s)", Toast.LENGTH_SHORT).show()
        }
    }

    // JSON backup file picker launcher
    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val jsonString = context.contentResolver.openInputStream(it)?.bufferedReader().use { r -> r?.readText() }
                if (jsonString != null) {
                    viewModel.verifyBackupPayload(jsonString) { verificationResult ->
                        viewModel.openDialog(AppDialog.Verification(verificationResult))
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading backup file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showSortMenu by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var trackContextMenuTarget by remember { mutableStateOf<TrackEntity?>(null) }
    var playlistMenuTarget by remember { mutableStateOf<PlaylistEntity?>(null) }

    val activePlaylist = playlists.find { it.id == activePlaylistId } ?: playlists.firstOrNull()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            if (currentTrack != null) {
                MiniPlayerBar(
                    currentTrack = currentTrack,
                    playlistName = activePlaylist?.name ?: "All",
                    isPlaying = isPlaying,
                    isFavorite = isLastActionLiked,
                    onPlayPause = { viewModel.playbackManager.togglePlayPause() },
                    onNext = { viewModel.playbackManager.playNext() },
                    onPrevious = { viewModel.playbackManager.playPrevious() },
                    onFavoriteToggle = { currentTrack?.let { viewModel.toggleFavorite(it) } },
                    onExpandToFullscreen = onOpenFullscreenPlayer
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // App Top Bar
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentGreen)
                                .border(1.dp, DarkBorder, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎧", fontSize = 14.sp)
                        }

                        Column {
                            Text(
                                text = "Ammu",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .background(AccentGreen, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "PURE AUDIO",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { viewModel.openDialog(AppDialog.Insights) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("📊", fontSize = 16.sp)
                        }

                        IconButton(
                            onClick = { viewModel.openDialog(AppDialog.Settings) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("⚙️", fontSize = 16.sp)
                        }
                    }
                }

                // Horizontal Playlist Chips Bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D121C))
                        .border(1.dp, DarkBorder)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // + New Playlist Button
                    Button(
                        onClick = { showCreatePlaylistDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("➕ New", fontSize = 11.sp, color = AccentGreenLight, fontWeight = FontWeight.Bold)
                    }

                    playlists.forEach { playlist ->
                        val isSelected = playlist.id == activePlaylistId
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) AccentGreen else DarkSurface)
                                .border(
                                    width = 1.dp,
                                    color = if (playlist.isSmart) Color(0x33FFFFFF) else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.selectPlaylist(playlist.id) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = playlist.name,
                                color = if (isSelected) Color.White else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )

                            if (playlist.id != "all" && !playlist.isSmart) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "⋮",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    modifier = Modifier.clickable { playlistMenuTarget = playlist }
                                )
                            }
                        }
                    }
                }

                // Search & Sort Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111722))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("🔍 Search songs...", fontSize = 12.sp) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Text("✕", fontSize = 12.sp, color = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )

                    // Sort Button
                    Box {
                        OutlinedButton(
                            onClick = { showSortMenu = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("↕️ Sort", fontSize = 11.sp, color = TextPrimary)
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("A to Z") }, onClick = { viewModel.setSortMode(SortMode.AZ); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Z to A") }, onClick = { viewModel.setSortMode(SortMode.ZA); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Size: Large to Small") }, onClick = { viewModel.setSortMode(SortMode.SIZE_DESC); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Size: Small to Large") }, onClick = { viewModel.setSortMode(SortMode.SIZE_ASC); showSortMenu = false })
                            DropdownMenuItem(text = { Text("Default") }, onClick = { viewModel.setSortMode(SortMode.DEFAULT); showSortMenu = false })
                        }
                    }

                    // Clean Titles Button
                    OutlinedButton(
                        onClick = {
                            viewModel.cleanSongTitles { cleanedCount ->
                                Toast.makeText(context, "Cleaned $cleanedCount title(s)!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("✨ Clean", fontSize = 11.sp, color = TextPrimary)
                    }
                }

                // Multi-Select Batch Action Bar
                if (multiSelectMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B2638))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { viewModel.selectAllTracks() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Select All", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${selectedTrackIds.size} Selected", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { viewModel.deleteSelectedTracksWithUndo() },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("🗑 Delete", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Tracks List Header
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = activePlaylist?.name ?: "Tracks",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${displayedTracks.size} songs",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Button(
                        onClick = {
                            audioPickerLauncher.launch(arrayOf("audio/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("➕ Add Songs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Song Rows List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (displayedTracks.isEmpty()) {
                        item {
                            Text(
                                text = "No songs found in this playlist.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        itemsIndexed(displayedTracks) { index, track ->
                            val isCurrentlyPlaying = currentTrack?.name == track.name
                            var dragDistanceX by remember { mutableFloatStateOf(0f) }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrentlyPlaying) Color(0xFF14231B) else DarkCard)
                                    .border(
                                        width = if (isCurrentlyPlaying) 1.5.dp else 1.dp,
                                        color = if (isCurrentlyPlaying) AccentGreen else DarkBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (dragDistanceX > 80f) {
                                                    // Swipe Right: Queue Play Next
                                                    viewModel.playbackManager.queuePlayNext(track)
                                                    Toast.makeText(context, "Queued to play next!", Toast.LENGTH_SHORT).show()
                                                } else if (dragDistanceX < -80f) {
                                                    // Swipe Left: Delete with Undo
                                                    viewModel.deleteTrackWithUndo(track)
                                                }
                                                dragDistanceX = 0f
                                            },
                                            onDragCancel = { dragDistanceX = 0f },
                                            onHorizontalDrag = { _, dragAmount ->
                                                dragDistanceX += dragAmount
                                            }
                                        )
                                    }
                                    .clickable {
                                        if (multiSelectMode) {
                                            viewModel.toggleSelectTrack(track.id)
                                        } else {
                                            viewModel.playTrack(track)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                if (multiSelectMode) {
                                    Checkbox(
                                        checked = selectedTrackIds.contains(track.id),
                                        onCheckedChange = { viewModel.toggleSelectTrack(track.id) },
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${index + 1}. ${track.name}",
                                            color = if (isCurrentlyPlaying) AccentGreenLight else Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (track.isMissing) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "⚠️ Not in storage",
                                                color = DangerRed,
                                                fontSize = 9.sp,
                                                modifier = Modifier
                                                    .background(Color(0x22F85149), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }

                                        if (isCurrentlyPlaying) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            MiniEqualizerBars(isPlaying = isPlaying)
                                        }
                                    }

                                    val sizeMb = track.fileSize / (1024f * 1024f)
                                    Text(
                                        text = "%.1f MB".format(sizeMb),
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(track) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("❤️", fontSize = 14.sp)
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteTrackWithUndo(track) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("🗑", fontSize = 14.sp, color = DangerRed)
                                    }

                                    IconButton(
                                        onClick = { trackContextMenuTarget = track },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("⋮", fontSize = 18.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5-Second Undo Toast Bar
            AnimatedVisibility(
                visible = undoAction != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
            ) {
                undoAction?.let { action ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(action.message, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Button(
                                onClick = { viewModel.triggerUndo() },
                                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                            ) {
                                Text("↩️ Undo (5s)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Heart Burst Overlay
            HeartBurstOverlay(
                isFavorited = isLastActionLiked,
                triggerKey = heartBurstKey,
                modifier = Modifier.align(Alignment.Center)
            )

            // Emotional Toast Reaction
            EmotionalToast(
                isLiked = isLastActionLiked,
                triggerKey = emotionalToastKey,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    // App Dialogs
    when (val dialog = dialogState) {
        is AppDialog.Export -> {
            BackupExportDialog(
                playlists = playlists,
                tracks = rawTracks,
                onDismiss = { viewModel.dismissDialog() },
                onConfirmExport = { options ->
                    viewModel.executeExport(options) { path ->
                        Toast.makeText(context, "Exported to: $path", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        is AppDialog.Verification -> {
            StorageMatcherVerificationDialog(
                verificationResult = dialog.result,
                onDismiss = { viewModel.dismissDialog() },
                onConfirmImport = { selectedPls, markers, lyrics, clips ->
                    viewModel.executeImport(
                        payload = dialog.result.parsedPayload,
                        selectedPlaylists = selectedPls,
                        permissions = ImportPermissions(isMasterUnlocked = true, isDownloadUnlocked = true, isAuthorUnlocked = true, isPermanentAdmin = false),
                        importMarkers = markers,
                        importLyrics = lyrics,
                        importClips = clips
                    )
                    Toast.makeText(context, "Import successful!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        is AppDialog.Auditor -> {
            StorageAuditorDialog(
                summary = storageAuditSummary,
                isScanning = isScanningStorage,
                onScan = { viewModel.runStorageAudit() },
                onPurgeDuplicates = { ids ->
                    viewModel.purgeStorageDuplicates(ids)
                    Toast.makeText(context, "Purged ${ids.size} duplicate tracks!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is AppDialog.Trimmer -> {
            AudioTrimmerDialog(
                track = dialog.track,
                durationMs = viewModel.playbackManager.durationMs.value,
                isTrimming = viewModel.isTrimming.value,
                onDismiss = { viewModel.dismissDialog() },
                onConfirmTrim = { start, end ->
                    viewModel.trimAudio(dialog.track, start, end) { success ->
                        Toast.makeText(context, if (success) "Trim saved!" else "Trimming failed", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        is AppDialog.Insights -> {
            InsightsDialog(
                dailyTimes = emptyList(),
                topStats = emptyList(),
                history = emptyList(),
                onPlaySongName = { songName ->
                    val found = rawTracks.find { it.name.equals(songName, ignoreCase = true) }
                    if (found != null) {
                        viewModel.playTrack(found)
                        viewModel.dismissDialog()
                    }
                },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is AppDialog.AdminAuth -> {
            AdminAuthDialog(
                hasRegisteredAdminKey = SecuritySuite.getAdminKeyHash(context) != null,
                onDismiss = { viewModel.dismissDialog() },
                onAuthorize = { key ->
                    if (SecuritySuite.verifyAdminKey(context, key)) {
                        Toast.makeText(context, "Admin Super-Key Authorized!", Toast.LENGTH_SHORT).show()
                        viewModel.dismissDialog()
                    } else {
                        Toast.makeText(context, "Invalid Admin Key", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        is AppDialog.Settings -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text("⚙️ Studio Preferences & Hub") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.openDialog(AppDialog.Export) }, modifier = Modifier.fillMaxWidth()) {
                            Text("📦 4-Key Selective Export")
                        }
                        Button(onClick = { jsonPickerLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.fillMaxWidth()) {
                            Text("📥 Restore / Import Backup (.json)")
                        }
                        Button(onClick = { viewModel.openDialog(AppDialog.Auditor) }, modifier = Modifier.fillMaxWidth()) {
                            Text("🔍 Storage Auditor & Cleaner")
                        }
                        Button(onClick = { viewModel.openDialog(AppDialog.AdminAuth) }, modifier = Modifier.fillMaxWidth()) {
                            Text("🛡️ Admin Super-Key Access")
                        }
                        Button(onClick = { viewModel.toggleAmoledMode() }, modifier = Modifier.fillMaxWidth()) {
                            Text("🌓 Toggle AMOLED Midnight Black")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) { Text("Close") }
                }
            )
        }

        is AppDialog.EditPlaylist -> {
            EditPlaylistDialog(
                playlist = dialog.playlist,
                onDismiss = { viewModel.dismissDialog() },
                onSave = { name, author ->
                    viewModel.updatePlaylist(dialog.playlist, name, author)
                    viewModel.dismissDialog()
                }
            )
        }

        null -> {}
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName.trim(), "Amarjeet Kumar")
                            showCreatePlaylistDialog = false
                            newPlaylistName = ""
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Track 3-dots Context Menu
    trackContextMenuTarget?.let { track ->
        AlertDialog(
            onDismissRequest = { trackContextMenuTarget = null },
            title = { Text(track.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.playbackManager.queuePlayNext(track)
                            trackContextMenuTarget = null
                            Toast.makeText(context, "Queued to play next!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⏭️ Play Next", color = TextPrimary)
                    }

                    TextButton(
                        onClick = {
                            viewModel.startMultiSelect(track.id)
                            trackContextMenuTarget = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("☑️ Select Track", color = TextPrimary)
                    }

                    TextButton(
                        onClick = {
                            viewModel.openDialog(AppDialog.Trimmer(track))
                            trackContextMenuTarget = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("✂️ Trim Audio Clip", color = TextPrimary)
                    }

                    TextButton(
                        onClick = {
                            viewModel.deleteTrackWithUndo(track)
                            trackContextMenuTarget = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🗑️ Delete Track", color = DangerRed)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { trackContextMenuTarget = null }) { Text("Close") }
            }
        )
    }

    // Playlist 3-dots Menu
    playlistMenuTarget?.let { pl ->
        AlertDialog(
            onDismissRequest = { playlistMenuTarget = null },
            title = { Text(pl.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.openDialog(AppDialog.EditPlaylist(pl))
                            playlistMenuTarget = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("✏️ Edit Details", color = TextPrimary)
                    }

                    TextButton(
                        onClick = {
                            viewModel.deletePlaylist(pl)
                            playlistMenuTarget = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🗑️ Delete Playlist", color = DangerRed)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { playlistMenuTarget = null }) { Text("Close") }
            }
        )
    }
}
