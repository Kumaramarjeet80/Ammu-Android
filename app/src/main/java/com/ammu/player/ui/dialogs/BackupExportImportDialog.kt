package com.ammu.player.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ammu.player.crypto.ExportOptions
import com.ammu.player.crypto.ImportPermissions
import com.ammu.player.crypto.ImportVerificationResult
import com.ammu.player.data.local.entity.PlaylistEntity
import com.ammu.player.data.local.entity.TrackEntity
import com.ammu.player.ui.theme.*

@Composable
fun BackupExportDialog(
    playlists: List<PlaylistEntity>,
    tracks: List<TrackEntity>,
    onDismiss: () -> Unit,
    onConfirmExport: (ExportOptions) -> Unit
) {
    var masterKey by remember { mutableStateOf("") }
    var encryptionKey by remember { mutableStateOf("") }
    var creatorPasskey by remember { mutableStateOf("") }
    var downloadKey by remember { mutableStateOf("") }

    var selectedPlaylists by remember { mutableStateOf(playlists.map { it.id }.toSet()) }
    var allowedDownloadTracks by remember { mutableStateOf(tracks.map { it.name }.toSet()) }

    var includeAudio by remember { mutableStateOf(true) }
    var includeClips by remember { mutableStateOf(true) }
    var includeTimestamps by remember { mutableStateOf(true) }
    var includeImages by remember { mutableStateOf(true) }

    var trackSearchQuery by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "🔒 4-Key Security Suite & Export",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Protect intellectual property with granular encryption rules.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                // 4 Keys Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Security Keys (Optional):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGreenLight)

                        OutlinedTextField(
                            value = masterKey,
                            onValueChange = { masterKey = it },
                            label = { Text("👑 Master Key (Unlocks all features)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = encryptionKey,
                            onValueChange = { encryptionKey = it },
                            label = { Text("🔐 Encryption Key (Whole AES-GCM JSON)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = creatorPasskey,
                            onValueChange = { creatorPasskey = it },
                            label = { Text("🔑 Creator Passkey (Locks metadata & author)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = downloadKey,
                            onValueChange = { downloadKey = it },
                            label = { Text("🎵 Download Key (Unlocks restricted tracks)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Playlists to include
                item {
                    Text("Select Playlists to Include:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        playlists.forEach { pl ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedPlaylists.contains(pl.id),
                                    onCheckedChange = { checked ->
                                        selectedPlaylists = if (checked) selectedPlaylists + pl.id else selectedPlaylists - pl.id
                                    }
                                )
                                Text(pl.name, fontSize = 12.sp, color = TextPrimary)
                            }
                        }
                    }
                }

                // Granular Track Download Permissions
                item {
                    Text("Track Download Policy (Scoped):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = trackSearchQuery,
                        onValueChange = { trackSearchQuery = it },
                        placeholder = { Text("🔍 Filter tracks...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Button(
                            onClick = { allowedDownloadTracks = tracks.map { it.name }.toSet() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Allow All", fontSize = 10.sp)
                        }
                        Button(
                            onClick = { allowedDownloadTracks = emptySet() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Restrict All", fontSize = 10.sp)
                        }
                    }

                    val filteredTracks = tracks.filter { it.name.contains(trackSearchQuery, ignoreCase = true) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .background(Color(0xFF090D15), RoundedCornerShape(6.dp))
                            .padding(4.dp)
                    ) {
                        filteredTracks.forEach { trk ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = allowedDownloadTracks.contains(trk.name),
                                    onCheckedChange = { checked ->
                                        allowedDownloadTracks = if (checked) allowedDownloadTracks + trk.name else allowedDownloadTracks - trk.name
                                    }
                                )
                                Text("Allow: ${trk.name}", fontSize = 11.sp, color = TextPrimary, maxLines = 1)
                            }
                        }
                    }
                }

                // Content Toggles
                item {
                    Column {
                        Text("Export Content:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = includeAudio, onCheckedChange = { includeAudio = it })
                            Text("Include Song Audio Files (.mp3)", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = includeClips, onCheckedChange = { includeClips = it })
                            Text("Include Trimmed Clips", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = includeTimestamps, onCheckedChange = { includeTimestamps = it })
                            Text("Include Saved Timestamps / Markers", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = includeImages, onCheckedChange = { includeImages = it })
                            Text("Include Covers & App Logos", fontSize = 12.sp)
                        }
                    }
                }

                // Actions
                item {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onConfirmExport(
                                    ExportOptions(
                                        masterKey = masterKey.trim(),
                                        encryptionKey = encryptionKey.trim(),
                                        creatorPasskey = creatorPasskey.trim(),
                                        downloadKey = downloadKey.trim(),
                                        selectedPlaylistIds = selectedPlaylists,
                                        allowedDownloadTrackNames = allowedDownloadTracks,
                                        includeAudio = includeAudio,
                                        includeTrimmedClips = includeClips,
                                        includeTimestamps = includeTimestamps,
                                        includeImages = includeImages
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                        ) {
                            Text("Export Backup", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StorageMatcherVerificationDialog(
    verificationResult: ImportVerificationResult,
    onDismiss: () -> Unit,
    onConfirmImport: (selectedPlaylists: Set<String>, importMarkers: Boolean, importLyrics: Boolean, importClips: Boolean) -> Unit
) {
    var selectedPlaylists by remember { mutableStateOf(verificationResult.playlists.map { it.id }.toSet()) }
    var importMarkers by remember { mutableStateOf(true) }
    var importLyrics by remember { mutableStateOf(true) }
    var importClips by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "🔍 Storage Matcher & Verification",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Curated by: ${verificationResult.authorName}",
                    fontSize = 12.sp,
                    color = AccentGreenLight,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Green / Red Verification Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0x222EA043), RoundedCornerShape(8.dp))
                            .border(1.dp, AccentGreen, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ ${verificationResult.availableTracksCount} Available",
                            color = AccentGreenLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0x22F85149), RoundedCornerShape(8.dp))
                            .border(1.dp, DangerRed, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕ ${verificationResult.missingTracksCount} Not in Storage",
                            color = DangerRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Track Matcher Details List
                Text("Storage Matching Results:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF090D15), RoundedCornerShape(6.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(verificationResult.trackMatchList) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${if (item.isAvailable) "✓ [Available]" else "✕ [Missing]"} ${item.name}",
                                color = if (item.isAvailable) AccentGreenLight else DangerRed,
                                fontSize = 11.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Select Playlists to Import
                Text("Select Playlists to Import:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column {
                    verificationResult.playlists.forEach { pl ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedPlaylists.contains(pl.id),
                                onCheckedChange = { checked ->
                                    selectedPlaylists = if (checked) selectedPlaylists + pl.id else selectedPlaylists - pl.id
                                }
                            )
                            Text(pl.name, fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirmImport(selectedPlaylists, importMarkers, importLyrics, importClips)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Text("Confirm & Import", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
