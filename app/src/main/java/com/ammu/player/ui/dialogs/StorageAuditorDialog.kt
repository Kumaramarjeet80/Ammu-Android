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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ammu.player.data.repository.DuplicateTrackItem
import com.ammu.player.data.repository.StorageAuditSummary
import com.ammu.player.ui.theme.*

@Composable
fun StorageAuditorDialog(
    summary: StorageAuditSummary?,
    isScanning: Boolean,
    onScan: () -> Unit,
    onPurgeDuplicates: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔍 Storage Auditor & Duplicate Cleaner",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text("✕", color = TextMuted)
                    }
                }

                Text(
                    text = "100% Offline scanner. Detects identical audio files to reclaim storage.",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                // Summary Cards
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val sizeMb = (summary?.totalSizeBytes ?: 0L) / (1024f * 1024f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("%.1f MB".format(sizeMb), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentGreenLight)
                            Text("Total Audio Used", fontSize = 10.sp, color = TextMuted)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${summary?.duplicateCount ?: 0}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                            Text("Duplicate Tracks", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }

                Button(
                    onClick = onScan,
                    enabled = !isScanning,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scanning...")
                    } else {
                        Text("⚡ Scan Device Storage", fontWeight = FontWeight.Bold)
                    }
                }

                Text("Detected Duplicates:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                val duplicates = summary?.duplicateTracks ?: emptyList()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF090D15), RoundedCornerShape(6.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (duplicates.isEmpty()) {
                        item {
                            Text(
                                text = if (summary == null) "Run scan to analyze local audio files." else "✓ No duplicate tracks detected. Storage is clean!",
                                color = if (summary == null) TextMuted else AccentGreenLight,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        items(duplicates) { dup ->
                            val mb = dup.sizeBytes / (1024f * 1024f)
                            Text(
                                text = "⚠️ ${dup.name} (%.1f MB)".format(mb),
                                color = TextPrimary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (duplicates.isNotEmpty()) {
                    Button(
                        onClick = {
                            onPurgeDuplicates(duplicates.map { it.trackId }.toSet())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🗑 Purge ${duplicates.size} Duplicates", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
