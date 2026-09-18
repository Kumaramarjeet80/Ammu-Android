package com.ammu.player.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ammu.player.audio.PlayerRepeatMode
import com.ammu.player.data.local.entity.TimestampEntity
import com.ammu.player.data.local.entity.TrimmedClipEntity
import com.ammu.player.ui.components.*
import com.ammu.player.ui.theme.*
import com.ammu.player.ui.viewmodel.AmmuMainViewModel
import com.ammu.player.ui.viewmodel.AppDialog
import com.ammu.player.ui.viewmodel.PlayerDrawer
import kotlin.math.roundToInt

@Composable
fun FullscreenPlayerSheet(
    viewModel: AmmuMainViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackManager = viewModel.playbackManager
    val currentTrack by playbackManager.currentTrack.collectAsState()
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val currentPositionMs by playbackManager.currentPositionMs.collectAsState()
    val durationMs by playbackManager.durationMs.collectAsState()
    val playbackSpeed by playbackManager.playbackSpeed.collectAsState()
    val volume by playbackManager.volume.collectAsState()
    val isShuffle by playbackManager.isShuffle.collectAsState()
    val repeatMode by playbackManager.repeatMode.collectAsState()
    val sleepTimerSec by playbackManager.sleepTimerSec.collectAsState()
    val playingQueue by playbackManager.playingQueue.collectAsState()
    val playNextQueue by playbackManager.playNextQueue.collectAsState()

    val isVinylMode by viewModel.isVinylMode.collectAsState()
    val activeDrawer by viewModel.activeDrawer.collectAsState()
    val customPresets by viewModel.customPresets.collectAsState()

    // Smooth drag-down pull-to-dismiss handle spring offset
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "DismissSpring"
    )

    // Sub-drawer states
    var loopPointA by remember { mutableStateOf<Long?>(null) }
    var loopPointB by remember { mutableStateOf<Long?>(null) }
    var lyricsInput by remember { mutableStateOf("") }
    var newMarkerName by remember { mutableStateOf("") }
    var showAddMarkerDialog by remember { mutableStateOf(false) }

    val timestamps = remember { mutableStateListOf<TimestampEntity>() }
    val clips = remember { mutableStateListOf<TrimmedClipEntity>() }

    if (currentTrack == null) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Drag-down pull-to-dismiss handle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(DarkCard)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount.y > 0 || dragOffsetY > 0) {
                                    dragOffsetY = (dragOffsetY + dragAmount.y).coerceAtLeast(0f)
                                }
                            },
                            onDragEnd = {
                                if (dragOffsetY > 160f) {
                                    onDismiss()
                                } else {
                                    dragOffsetY = 0f
                                }
                            },
                            onDragCancel = { dragOffsetY = 0f }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .background(DarkBorder, RoundedCornerShape(2.dp))
                )
            }

            // Top Navigation Bar
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                ) {
                    Text("▼ Minimize", fontSize = 12.sp, color = TextPrimary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Playback Speed Toggle
                    Button(
                        onClick = { playbackManager.cycleSpeed() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                    ) {
                        Text("${playbackSpeed}x", fontSize = 11.sp, color = AccentGreenLight, fontWeight = FontWeight.Bold)
                    }

                    // Sleep Timer Toggle
                    Button(
                        onClick = {
                            val nextMinutes = when (sleepTimerSec) {
                                null -> 15
                                in 1..900 -> 30
                                in 901..1800 -> 45
                                in 1801..2700 -> 60
                                else -> 0
                            }
                            playbackManager.setSleepTimer(nextMinutes)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                    ) {
                        val label = if (sleepTimerSec != null) "⏱️ ${(sleepTimerSec!! / 60) + 1}m" else "⏱️ Off"
                        Text(label, fontSize = 11.sp, color = AccentGreenLight, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(
                    onClick = { viewModel.toggleFavorite(currentTrack!!) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("❤️", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Collapsible Hero Section: Vinyl / Artwork Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                VinylDiscView(
                    artworkUri = currentTrack?.filePath,
                    isPlaying = isPlaying,
                    isVinylMode = isVinylMode,
                    onToggleVinylMode = { viewModel.toggleVinylMode() },
                    onSeekRelative = { offset -> playbackManager.seekRelative(offset) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Song Title & Playlist Info
                Text(
                    text = currentTrack?.name ?: "No Track",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Playlist: Active",
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Waveform Scrubber & Ticks
                WaveformScrubber(
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    timestamps = timestamps,
                    onSeek = { targetMs -> playbackManager.seekTo(targetMs) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Jump Row (-10s, Shuffle, Repeat, +10s)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                ) {
                    OutlinedButton(
                        onClick = { playbackManager.seekRelative(-10_000L) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("⏪ -10s", fontSize = 11.sp, color = TextPrimary)
                    }

                    OutlinedButton(
                        onClick = { playbackManager.toggleShuffle() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isShuffle) AccentGreen else Color.Transparent
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(if (isShuffle) "🔀 On" else "🔀 Off", fontSize = 11.sp, color = if (isShuffle) Color.White else TextPrimary)
                    }

                    OutlinedButton(
                        onClick = { playbackManager.toggleRepeat() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (repeatMode == PlayerRepeatMode.ONE) AccentGreen else Color.Transparent
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(if (repeatMode == PlayerRepeatMode.ONE) "🔂 One" else "🔁 All", fontSize = 11.sp, color = TextPrimary)
                    }

                    OutlinedButton(
                        onClick = { playbackManager.seekRelative(10_000L) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("⏩ +10s", fontSize = 11.sp, color = TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sticky Controls Anchor: Previous, Huge Play/Pause, Next
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .border(1.dp, DarkBorder)
                    .padding(vertical = 10.dp, horizontal = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    IconButton(
                        onClick = { playbackManager.playPrevious() },
                        modifier = Modifier
                            .size(46.dp)
                            .background(DarkSurface, CircleShape)
                            .border(1.dp, DarkBorder, CircleShape)
                    ) {
                        Text("⏮", fontSize = 18.sp, color = Color.White)
                    }

                    IconButton(
                        onClick = { playbackManager.togglePlayPause() },
                        modifier = Modifier
                            .size(62.dp)
                            .background(AccentGreen, CircleShape)
                            .shadow(12.dp, CircleShape)
                    ) {
                        Text(
                            text = if (isPlaying) "⏸" else "▶",
                            fontSize = 24.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { playbackManager.playNext() },
                        modifier = Modifier
                            .size(46.dp)
                            .background(DarkSurface, CircleShape)
                            .border(1.dp, DarkBorder, CircleShape)
                    ) {
                        Text("⏭", fontSize = 18.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Independent Tool Drawers Navigation Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    val drawers = listOf(
                        Pair(PlayerDrawer.QUEUE, "≡ Playlist"),
                        Pair(PlayerDrawer.VOLUME, "🔊 Volume"),
                        Pair(PlayerDrawer.EQ, "🎛 EQ & Bass"),
                        Pair(PlayerDrawer.TIMESTAMPS, "⏱️ Markers"),
                        Pair(PlayerDrawer.LOOPER, "🔄 A-B Loop"),
                        Pair(PlayerDrawer.CLIPS, "✂️ Trim"),
                        Pair(PlayerDrawer.LYRICS, "📝 Lyrics")
                    )

                    drawers.forEach { pair ->
                        val drawer = pair.first
                        val label = pair.second
                        val isSelected = activeDrawer == drawer
                        Button(
                            onClick = { viewModel.toggleDrawer(drawer) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) AccentGreen else DarkSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(label, fontSize = 11.sp, color = if (isSelected) Color.White else TextPrimary)
                        }
                    }
                }
            }

            // Sub-Drawers Area
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                when (activeDrawer) {
                    PlayerDrawer.VOLUME -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkCard, RoundedCornerShape(12.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Master Volume (Software Gain)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${(volume * 100).toInt()}%", fontSize = 12.sp, color = AccentGreenLight, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = volume,
                                onValueChange = { playbackManager.setMasterVolume(it) },
                                colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { playbackManager.setMasterVolume(0.20f) }, modifier = Modifier.weight(1f)) {
                                    Text("20% (DSP)", fontSize = 10.sp)
                                }
                                Button(onClick = { playbackManager.setMasterVolume(0.50f) }, modifier = Modifier.weight(1f)) {
                                    Text("50%", fontSize = 10.sp)
                                }
                                Button(onClick = { playbackManager.setMasterVolume(0.80f) }, modifier = Modifier.weight(1f)) {
                                    Text("80%", fontSize = 10.sp)
                                }
                                Button(onClick = { playbackManager.setMasterVolume(1.0f) }, modifier = Modifier.weight(1f)) {
                                    Text("100%", fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    PlayerDrawer.EQ -> {
                        DualEqualizerView(
                            dspEngine = viewModel.dspEngine,
                            customPresets = customPresets,
                            onVolumeAdjust = { playbackManager.setMasterVolume(it) },
                            onSaveCustomPreset = { name, gains -> viewModel.saveCustomEqPreset(name, gains) },
                            onDeleteCustomPreset = { name -> viewModel.deleteCustomEqPreset(name) },
                            onExportEqProfile = { viewModel.openDialog(AppDialog.Export) },
                            onImportEqProfile = { }
                        )
                    }

                    PlayerDrawer.QUEUE -> {
                        QueueTwoZoneList(
                            playingQueue = playingQueue,
                            playNextQueue = playNextQueue,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            onTrackSelected = { track -> playbackManager.playTrack(track) },
                            onRemoveFromPlayNext = { idx -> playbackManager.removeTrackFromPlayNext(idx) },
                            onReorderQueue = { from, to -> playbackManager.reorderQueue(from, to) },
                            onShiftQueueItem = { idx, delta -> playbackManager.shiftQueueItem(idx, delta) },
                            onRemoveQueueItem = { idx -> playbackManager.removeQueueItem(idx) },
                            onViewAuthor = { viewModel.openDialog(AppDialog.Insights) }
                        )
                    }

                    PlayerDrawer.TIMESTAMPS -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkCard, RoundedCornerShape(12.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("⏱️ Track Markers", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { showAddMarkerDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                                ) {
                                    Text("➕ Add Current Time", fontSize = 11.sp, color = AccentGreenLight)
                                }
                            }

                            if (timestamps.isEmpty()) {
                                Text("No timestamps saved yet.", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(vertical = 8.dp))
                            } else {
                                timestamps.forEach { ts ->
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Text("${formatTime(ts.timeMs)} • ${ts.name}", fontSize = 12.sp, color = TextPrimary)
                                        IconButton(onClick = { timestamps.remove(ts) }, modifier = Modifier.size(24.dp)) {
                                            Text("✕", color = DangerRed)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PlayerDrawer.LOOPER -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkCard, RoundedCornerShape(12.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("Continuous A-B Section Repeater", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { loopPointA = currentPositionMs },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Set A: ${loopPointA?.let { formatTime(it) } ?: "--:--"}", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { loopPointB = currentPositionMs },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Set B: ${loopPointB?.let { formatTime(it) } ?: "--:--"}", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        loopPointA = null
                                        loopPointB = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                                ) {
                                    Text("Clear", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    PlayerDrawer.LYRICS -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkCard, RoundedCornerShape(12.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("Offline Lyrics & Notes", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = lyricsInput,
                                onValueChange = { lyricsInput = it },
                                placeholder = { Text("Paste song lyrics or notes here...") },
                                modifier = Modifier.fillMaxWidth().height(120.dp)
                            )
                        }
                    }

                    PlayerDrawer.CLIPS -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkCard, RoundedCornerShape(12.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("✂️ Saved MP3 Clips", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = {
                                        currentTrack?.let { viewModel.openDialog(AppDialog.Trimmer(it)) }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                                ) {
                                    Text("➕ New Trim", fontSize = 11.sp, color = AccentGreenLight)
                                }
                            }
                            if (clips.isEmpty()) {
                                Text("No clips saved for this track yet.", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }

                    null -> {}
                }
            }

            // Bottom Developer Attribution
            Text(
                text = "Made by & for Amarjeet kumar",
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp)
            )
        }

        // Add Marker Dialog
        if (showAddMarkerDialog) {
            AlertDialog(
                onDismissRequest = { showAddMarkerDialog = false },
                title = { Text("Add Bookmark Marker") },
                text = {
                    OutlinedTextField(
                        value = newMarkerName,
                        onValueChange = { newMarkerName = it },
                        label = { Text("Marker Name") },
                        placeholder = { Text("Chorus / Best Part") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val name = if (newMarkerName.isNotBlank()) newMarkerName else "Marker at ${formatTime(currentPositionMs)}"
                            timestamps.add(TimestampEntity(id = "ts_${System.currentTimeMillis()}", songKey = currentTrack!!.name, timeMs = currentPositionMs, name = name))
                            showAddMarkerDialog = false
                            newMarkerName = ""
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddMarkerDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
