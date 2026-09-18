package com.ammu.player.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ammu.player.data.local.entity.TrackEntity
import com.ammu.player.ui.theme.*

@Composable
fun AudioTrimmerDialog(
    track: TrackEntity,
    durationMs: Long,
    isTrimming: Boolean,
    onDismiss: () -> Unit,
    onConfirmTrim: (startSec: Float, endSec: Float) -> Unit
) {
    val totalSec = (durationMs / 1000f).coerceAtLeast(10f)

    var startSec by remember { mutableFloatStateOf(0f) }
    var endSec by remember { mutableFloatStateOf((30f).coerceAtMost(totalSec)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "✂️ Audio Trimmer (.mp3 Compressed)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = track.name,
                    fontSize = 12.sp,
                    color = AccentGreenLight,
                    fontWeight = FontWeight.SemiBold
                )

                // Start Time Slider
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Time", fontSize = 11.sp, color = TextMuted)
                        Text("%.1fs".format(startSec), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGreenLight)
                    }
                    Slider(
                        value = startSec,
                        onValueChange = {
                            startSec = it.coerceAtMost(endSec - 1f)
                        },
                        valueRange = 0f..totalSec,
                        colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen)
                    )
                }

                // End Time Slider
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("End Time", fontSize = 11.sp, color = TextMuted)
                        Text("%.1fs".format(endSec), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGreenLight)
                    }
                    Slider(
                        value = endSec,
                        onValueChange = {
                            endSec = it.coerceAtLeast(startSec + 1f)
                        },
                        valueRange = 0f..totalSec,
                        colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen)
                    )
                }

                Text(
                    text = "Clip Duration: %.1fs".format(endSec - startSec),
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirmTrim(startSec, endSec) },
                        enabled = !isTrimming,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        if (isTrimming) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Slicing...")
                        } else {
                            Text("✂️ Cut & Save MP3", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
