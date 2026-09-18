package com.ammu.player.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ammu.player.data.local.entity.PlaylistEntity
import com.ammu.player.ui.theme.*

@Composable
fun EditPlaylistDialog(
    playlist: PlaylistEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, author: String) -> Unit
) {
    var name by remember { mutableStateOf(playlist.name) }
    var author by remember { mutableStateOf(playlist.authorName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "✏️ Edit Playlist Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Title") },
                    enabled = playlist.id != "favorites",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (playlist.id == "favorites") {
                    Text("Favorites playlist name cannot be modified.", fontSize = 10.sp, color = WarningAmber)
                }

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Playlist Author Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name.trim(), author.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Text("Update Details", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
