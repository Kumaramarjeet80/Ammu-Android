package com.ammu.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ammu.player.audio.DspEngine
import com.ammu.player.audio.EqEngineMode
import com.ammu.player.data.local.entity.CustomEqPresetEntity
import com.ammu.player.ui.theme.*

@Composable
fun DualEqualizerView(
    dspEngine: DspEngine,
    customPresets: List<CustomEqPresetEntity>,
    onVolumeAdjust: (Float) -> Unit,
    onSaveCustomPreset: (String, FloatArray) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onExportEqProfile: () -> Unit,
    onImportEqProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEnabled by remember { mutableStateOf(dspEngine.isEnabled) }
    var activeMode by remember { mutableStateOf(dspEngine.activeMode) }
    var activeVivoKey by remember { mutableStateOf(dspEngine.activeVivoPresetKey) }
    var bassBoostDb by remember { mutableFloatStateOf(dspEngine.bassBoostDb) }
    var preampDb by remember { mutableFloatStateOf(dspEngine.preampDb) }

    // Gain buffers to trigger UI re-renders
    var vlcGains by remember { mutableStateOf(dspEngine.currentVlcGains.clone()) }
    var vivoGains by remember { mutableStateOf(dspEngine.currentVivoGains.clone()) }

    var showSavePresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Engine Selector Tabs (VLC vs Vivo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF090D15), RoundedCornerShape(8.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                .padding(2.dp)
        ) {
            Button(
                onClick = {
                    activeMode = EqEngineMode.VLC
                    dspEngine.setEngineMode(EqEngineMode.VLC)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeMode == EqEngineMode.VLC) AccentGreen else Color.Transparent
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "🎛️ VLC EQ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeMode == EqEngineMode.VLC) Color.White else TextMuted
                )
            }

            Button(
                onClick = {
                    activeMode = EqEngineMode.VIVO
                    dspEngine.setEngineMode(EqEngineMode.VIVO)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeMode == EqEngineMode.VIVO) AccentGreen else Color.Transparent
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "📱 Vivo Studio EQ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeMode == EqEngineMode.VIVO) Color.White else TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Master EQ Toggle & Actions Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        isEnabled = checked
                        dspEngine.setDspEnabled(checked, onVolumeAdjust)
                    },
                    colors = CheckboxDefaults.colors(checkedColor = AccentGreen)
                )
                Text(
                    text = "EQ Active (Auto Vol 20%)",
                    color = AccentGreenLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = onExportEqProfile,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("💾 Export", fontSize = 10.sp, color = TextPrimary)
                }
                OutlinedButton(
                    onClick = onImportEqProfile,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("📂 Import", fontSize = 10.sp, color = TextPrimary)
                }
                OutlinedButton(
                    onClick = {
                        dspEngine.resetToDefaults(onVolumeAdjust)
                        isEnabled = dspEngine.isEnabled
                        vlcGains = dspEngine.currentVlcGains.clone()
                        vivoGains = dspEngine.currentVivoGains.clone()
                        bassBoostDb = dspEngine.bassBoostDb
                        preampDb = dspEngine.preampDb
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Reset", fontSize = 10.sp, color = DangerRed)
                }
            }
        }

        // Quick Preset Chips Strip
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp)
        ) {
            val quickPresets = listOf(
                "Flat" to "close",
                "Vocal Clarity" to "pop",
                "Bass Heavy" to "electro",
                "Treble Boost" to "rock",
                "Acoustic" to "classical"
            )
            quickPresets.forEach { (label, presetKey) ->
                SuggestionChip(
                    onClick = {
                        dspEngine.applyVivoPreset(presetKey)
                        activeVivoKey = presetKey
                        vivoGains = dspEngine.currentVivoGains.clone()
                        if (presetKey == "electro") {
                            dspEngine.setBassBoost(8f)
                            bassBoostDb = 8f
                        }
                    },
                    label = { Text(label, fontSize = 11.sp, color = TextPrimary) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (activeMode == EqEngineMode.VLC) {
            // VLC 10-Band EQ View
            Column {
                // Bass Boost Strip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🔊 Bass Boost: %.1fdB".format(bassBoostDb),
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Slider(
                        value = bassBoostDb,
                        onValueChange = {
                            bassBoostDb = it
                            dspEngine.setBassBoost(it)
                        },
                        valueRange = 0f..15f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentGreenLight,
                            activeTrackColor = AccentGreen
                        ),
                        modifier = Modifier.width(160.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 10 Band VLC Sliders (Scrollable row)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    // Preamp
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(42.dp)
                    ) {
                        Text("%.1fdB".format(preampDb), fontSize = 9.sp, color = AccentGreenLight)
                        Slider(
                            value = preampDb,
                            onValueChange = {
                                preampDb = it
                                dspEngine.setPreamp(it)
                            },
                            valueRange = -20f..20f,
                            modifier = Modifier.height(110.dp)
                        )
                        Text("Pre", fontSize = 10.sp, color = TextMuted)
                    }

                    // 10 VLC Frequency Bands
                    DspEngine.VLC_BANDS.forEachIndexed { index, freq ->
                        val freqLabel = if (freq >= 1000) "${freq / 1000}k" else "$freq"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(42.dp)
                        ) {
                            Text("%.1fdB".format(vlcGains[index]), fontSize = 9.sp, color = AccentGreenLight)
                            Slider(
                                value = vlcGains[index],
                                onValueChange = {
                                    val newGains = vlcGains.clone()
                                    newGains[index] = it
                                    vlcGains = newGains
                                    dspEngine.updateVlcBand(index, it)
                                },
                                valueRange = -20f..20f,
                                modifier = Modifier.height(110.dp)
                            )
                            Text(freqLabel, fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }
            }
        } else {
            // Vivo Studio 10-Band EQ View
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Preset: ${activeVivoKey.replaceFirstChar { it.uppercase() }}",
                        color = VivoPink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showSavePresetDialog = true }) {
                        Text("➕ Save Preset", fontSize = 11.sp, color = AccentGreenLight)
                    }
                }

                // 10 Vivo Sliders
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    DspEngine.VIVO_BANDS.forEachIndexed { index, freq ->
                        val freqLabel = if (freq >= 1000) "${freq / 1000}k" else "$freq"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(36.dp)
                        ) {
                            Text("%.1f".format(vivoGains[index]), fontSize = 9.sp, color = VivoPink)
                            Slider(
                                value = vivoGains[index],
                                onValueChange = {
                                    val newGains = vivoGains.clone()
                                    newGains[index] = it
                                    vivoGains = newGains
                                    activeVivoKey = "custom"
                                    dspEngine.updateVivoBand(index, it)
                                },
                                valueRange = -12f..12f,
                                colors = SliderDefaults.colors(
                                    thumbColor = VivoPink,
                                    activeTrackColor = VivoPink
                                ),
                                modifier = Modifier.height(120.dp)
                            )
                            Text(freqLabel, fontSize = 9.sp, color = TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Dynamic Bezier Spline Curve Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(0x33000000), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val midY = h / 2

                        // Zero reference line
                        drawLine(
                            color = Color(0x26FFFFFF),
                            start = Offset(0f, midY),
                            end = Offset(w, midY),
                            strokeWidth = 1f
                        )

                        // Compute 10 points
                        val stepX = w / (vivoGains.size - 1)
                        val points = vivoGains.mapIndexed { i, gain ->
                            val x = i * stepX
                            val y = midY - (gain / 12f) * (midY - 8f)
                            Offset(x, y)
                        }

                        // Draw smooth cubic Bezier spline
                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p0 = if (i == 0) points[0] else points[i - 1]
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val p3 = if (i + 2 < points.size) points[i + 2] else points[i + 1]

                                val cp1x = p1.x + (p2.x - p0.x) / 6f
                                val cp1y = p1.y + (p2.y - p0.y) / 6f
                                val cp2x = p2.x - (p3.x - p1.x) / 6f
                                val cp2y = p2.y - (p3.y - p1.y) / 6f

                                cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = VivoPink,
                            style = Stroke(width = 2.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Horizontal Preset Carousel
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    DspEngine.VIVO_OFFICIAL_PRESETS.keys.forEach { presetKey ->
                        val isSelected = activeVivoKey == presetKey
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                activeVivoKey = presetKey
                                dspEngine.applyVivoPreset(presetKey)
                                vivoGains = dspEngine.currentVivoGains.clone()
                            },
                            label = {
                                Text(
                                    text = presetKey.replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    color = if (isSelected) VivoPink else TextMuted
                                )
                            }
                        )
                    }

                    // User Custom Presets
                    customPresets.forEach { preset ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = activeVivoKey == preset.name,
                                onClick = {
                                    activeVivoKey = preset.name
                                    // Parse gains array
                                    val gains = preset.gainsJson
                                        .replace("[", "").replace("]", "")
                                        .split(",").mapNotNull { it.trim().toFloatOrNull() }
                                        .toFloatArray()
                                    dspEngine.applyVivoPreset(preset.name, gains)
                                    vivoGains = gains.clone()
                                },
                                label = { Text(preset.name, fontSize = 11.sp) }
                            )
                            IconButton(
                                onClick = { onDeleteCustomPreset(preset.name) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("✕", fontSize = 10.sp, color = DangerRed)
                            }
                        }
                    }
                }
            }
        }

        // Developer attribution
        Text(
            text = "Made by & for Amarjeet kumar",
            color = TextMuted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )
    }

    // Save Preset Dialog
    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Custom Preset") },
            text = {
                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = { newPresetName = it },
                    label = { Text("Preset Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            onSaveCustomPreset(newPresetName.trim(), vivoGains)
                            showSavePresetDialog = false
                            newPresetName = ""
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
