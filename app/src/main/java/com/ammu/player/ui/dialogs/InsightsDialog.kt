package com.ammu.player.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ammu.player.data.local.entity.DailyListeningTimeEntity
import com.ammu.player.data.local.entity.PlaybackHistoryEntity
import com.ammu.player.data.local.entity.StatsEntity
import com.ammu.player.ui.theme.*

@Composable
fun InsightsDialog(
    dailyTimes: List<DailyListeningTimeEntity>,
    topStats: List<StatsEntity>,
    history: List<PlaybackHistoryEntity>,
    onPlaySongName: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val totalAllTimeSec = dailyTimes.sumOf { it.totalSeconds }
    val todaySec = dailyTimes.firstOrNull()?.totalSeconds ?: 0L
    val weekSec = dailyTimes.take(7).sumOf { it.totalSeconds }
    val monthSec = dailyTimes.take(30).sumOf { it.totalSeconds }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(vertical = 10.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📊 Listening Insights & Analytics",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Text("✕", color = TextMuted)
                        }
                    }
                }

                // Time grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatCard(title = "Played Today", value = formatHoursMins(todaySec), modifier = Modifier.weight(1f))
                            StatCard(title = "This Week", value = formatHoursMins(weekSec), modifier = Modifier.weight(1f))
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatCard(title = "This Month", value = formatHoursMins(monthSec), modifier = Modifier.weight(1f))
                            StatCard(title = "All-Time Play", value = formatHoursMins(totalAllTimeSec), modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Milestones Badges
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text("🏆 Milestones Unlocked", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGreenLight)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MilestoneChip(title = "🎧 First Listen", unlocked = totalAllTimeSec > 60)
                            MilestoneChip(title = "⏳ 1 Hour Club", unlocked = totalAllTimeSec >= 3600)
                            MilestoneChip(title = "🏃 Marathoner", unlocked = totalAllTimeSec >= 18000)
                        }
                    }
                }

                // Top 5 Songs
                item {
                    Text("🔥 Top 5 Most Replayed Songs (Tap to Play):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF090D15), RoundedCornerShape(6.dp))
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (topStats.isEmpty()) {
                            Text("No track replays logged yet.", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(6.dp))
                        } else {
                            topStats.take(5).forEachIndexed { idx, stat ->
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPlaySongName(stat.songKey) }
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = "#${idx + 1} ${stat.songKey}",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("${stat.playCount} plays", color = AccentGreenLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Playback Timeline History
                item {
                    Text("🕒 Recent Playback History (Tap to Play):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF090D15), RoundedCornerShape(6.dp))
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (history.isEmpty()) {
                            Text("No recent playback history.", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(6.dp))
                        } else {
                            history.take(15).forEach { h ->
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPlaySongName(h.trackName) }
                                        .padding(6.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(h.trackName, color = TextPrimary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(h.playlistName, color = TextMuted, fontSize = 9.sp)
                                    }
                                    Text(h.dateIst, color = TextMuted, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(DarkSurface, RoundedCornerShape(8.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentGreenLight)
            Text(title, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun MilestoneChip(title: String, unlocked: Boolean) {
    Box(
        modifier = Modifier
            .background(if (unlocked) Color(0x222EA043) else Color(0x11FFFFFF), RoundedCornerShape(12.dp))
            .border(1.dp, if (unlocked) AccentGreen else DarkBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$title ${if (unlocked) "✓" else "🔒"}",
            fontSize = 9.sp,
            color = if (unlocked) AccentGreenLight else TextMuted
        )
    }
}

private fun formatHoursMins(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
