package com.ammu.player.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ammu.player.data.local.entity.TrackEntity
import com.ammu.player.ui.theme.*

@Composable
fun QueueTwoZoneList(
    playingQueue: List<TrackEntity>,
    playNextQueue: List<TrackEntity>,
    currentTrack: TrackEntity?,
    isPlaying: Boolean,
    onTrackSelected: (TrackEntity) -> Unit,
    onRemoveFromPlayNext: (Int) -> Unit,
    onReorderQueue: (from: Int, to: Int) -> Unit,
    onShiftQueueItem: (index: Int, delta: Int) -> Unit,
    onRemoveQueueItem: (index: Int) -> Unit,
    onViewAuthor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Drag-and-drop state for Zone A
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dropTargetIndex by remember { mutableStateOf<Int?>(null) }

    // Queue Duration calculation
    val totalCount = playingQueue.size + playNextQueue.size
    val totalSeconds = totalCount * 210L // Average 3.5 mins
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    val durationFormatted = if (hrs > 0) {
        "%d:%02d:%02d".format(hrs, mins, secs)
    } else {
        "%02d:%02d".format(mins, secs)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Queue Header
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Column {
                Text(
                    text = "Active Queue",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$totalCount song${if (totalCount == 1) "" else "s"} • ~$durationFormatted",
                    color = AccentGreenLight,
                    fontSize = 11.sp
                )
            }
            OutlinedButton(
                onClick = onViewAuthor,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("👤 Author", fontSize = 11.sp, color = AccentGreenLight)
            }
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
        ) {
            // Play Next Queued Tracks
            itemsIndexed(playNextQueue) { qIdx, qTrack ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D2117), RoundedCornerShape(10.dp))
                        .border(1.dp, AccentGreenLight, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTrackSelected(qTrack) }
                    ) {
                        Text(
                            text = "[Up Next] ",
                            color = AccentGreenLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = qTrack.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { onRemoveFromPlayNext(qIdx) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("✕", color = DangerRed, fontSize = 12.sp)
                    }
                }
            }

            // Playing Queue Items
            itemsIndexed(playingQueue) { index, track ->
                val isCurrent = currentTrack?.name == track.name
                val isDragging = draggingIndex == index
                val isDropTarget = dropTargetIndex == index

                Column {
                    // Red Inline Drop Target Indicator Line
                    if (isDropTarget) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(DangerRed, RoundedCornerShape(2.dp))
                                .shadow(6.dp, spotColor = DangerRed)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                when {
                                    isCurrent -> Color(0xFF0E1D16)
                                    isDragging -> Color(0xFF1E2A3C)
                                    else -> Color(0xFF0C1017)
                                },
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = if (isCurrent) 1.5.dp else 1.dp,
                                color = if (isCurrent) AccentGreen else DarkBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        // ZONE A: Title & Info (Long-press & Drag Reorder Zone)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .pointerInput(index) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingIndex = index
                                            dropTargetIndex = index
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val estimatedRowHeight = 44.dp.toPx()
                                            val rowsDelta = (dragAmount.y / estimatedRowHeight).toInt()
                                            val target = (index + rowsDelta).coerceIn(0, playingQueue.size - 1)
                                            dropTargetIndex = target
                                        },
                                        onDragEnd = {
                                            if (draggingIndex != null && dropTargetIndex != null && draggingIndex != dropTargetIndex) {
                                                onReorderQueue(draggingIndex!!, dropTargetIndex!!)
                                            }
                                            draggingIndex = null
                                            dropTargetIndex = null
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                            dropTargetIndex = null
                                        }
                                    )
                                }
                                .clickable { onTrackSelected(track) }
                        ) {
                            Text(
                                text = "${index + 1}. ",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = track.name,
                                color = if (isCurrent) AccentGreenLight else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (track.isMissing) {
                                Text(
                                    text = "Missing",
                                    color = DangerRed,
                                    fontSize = 9.sp,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .background(Color(0x22F85149), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }

                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                MiniEqualizerBars(isPlaying = isPlaying)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // ZONE B: Shift Buttons & Delete (Scroll-Only Zone, No Drag Interference)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { onShiftQueueItem(index, -1) },
                                enabled = index > 0,
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(DarkSurface, RoundedCornerShape(6.dp))
                            ) {
                                Text("▲", fontSize = 10.sp, color = TextPrimary)
                            }
                            IconButton(
                                onClick = { onShiftQueueItem(index, 1) },
                                enabled = index < playingQueue.size - 1,
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(DarkSurface, RoundedCornerShape(6.dp))
                            ) {
                                Text("▼", fontSize = 10.sp, color = TextPrimary)
                            }
                            IconButton(
                                onClick = { onRemoveQueueItem(index) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(DarkSurface, RoundedCornerShape(6.dp))
                            ) {
                                Text("✕", fontSize = 10.sp, color = DangerRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "MiniEq")

    val h1 by transition.animateFloat(
        initialValue = 3f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "Bar1"
    )
    val h2 by transition.animateFloat(
        initialValue = 10f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "Bar2"
    )
    val h3 by transition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "Bar3"
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.height(12.dp)
    ) {
        val barColor = Color.White
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(if (isPlaying) h1.dp else 4.dp)
                .background(barColor, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(if (isPlaying) h2.dp else 4.dp)
                .background(barColor, RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(if (isPlaying) h3.dp else 4.dp)
                .background(barColor, RoundedCornerShape(1.dp))
        )
    }
}
