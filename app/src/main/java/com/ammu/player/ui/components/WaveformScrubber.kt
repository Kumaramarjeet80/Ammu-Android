package com.ammu.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ammu.player.data.local.entity.TimestampEntity
import com.ammu.player.ui.theme.AccentGreen
import com.ammu.player.ui.theme.AccentGreenLight
import com.ammu.player.ui.theme.SeekTickYellow
import com.ammu.player.ui.theme.TextMuted
import kotlin.math.sin

@Composable
fun WaveformScrubber(
    currentPositionMs: Long,
    durationMs: Long,
    timestamps: List<TimestampEntity>,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    // Find active timestamp marker if current position is past marker
    val activeMarker = timestamps.lastOrNull { currentPositionMs >= it.timeMs }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        // Active Marker Pill Badge
        if (activeMarker != null) {
            Box(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .background(Color(0x332EA043), RoundedCornerShape(12.dp))
                    .border(1.dp, AccentGreen, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "📍 ${activeMarker.name} (${formatTime(activeMarker.timeMs)})",
                    color = AccentGreenLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Waveform Bars Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(Color(0x22000000), RoundedCornerShape(6.dp))
        ) {
            val barCount = 70
            val barWidth = size.width / barCount
            val midY = size.height / 2

            for (i in 0 until barCount) {
                val barProgress = i.toFloat() / barCount
                val heightMultiplier = (sin(i * 0.25) * 0.4 + 0.6).toFloat()
                val barHeight = (size.height * 0.75f * heightMultiplier).coerceAtLeast(4f)

                val color = if (barProgress <= progress) AccentGreen else Color(0x33FFFFFF)

                drawRect(
                    color = color,
                    topLeft = Offset(i * barWidth + 1.5f, midY - barHeight / 2),
                    size = Size(barWidth - 2.5f, barHeight)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Scrubber Track with Seek Ticks
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Seek Ticks Layer
            if (durationMs > 0 && timestamps.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .padding(horizontal = 6.dp)
                ) {
                    timestamps.forEach { ts ->
                        val tickProgress = (ts.timeMs.toFloat() / durationMs).coerceIn(0f, 1f)
                        val tickX = size.width * tickProgress

                        drawRoundRect(
                            color = SeekTickYellow,
                            topLeft = Offset(tickX - 2.dp.toPx(), 0f),
                            size = Size(4.dp.toPx(), size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }

            Slider(
                value = progress,
                onValueChange = { newProgress ->
                    onSeek((newProgress * durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = AccentGreenLight,
                    activeTrackColor = AccentGreen,
                    inactiveTrackColor = Color(0x33FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Time labels
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = formatTime(currentPositionMs),
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTime(durationMs),
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
