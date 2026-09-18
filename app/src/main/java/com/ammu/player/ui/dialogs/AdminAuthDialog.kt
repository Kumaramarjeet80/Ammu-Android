package com.ammu.player.ui.dialogs

import androidx.compose.foundation.layout.*
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
import com.ammu.player.ui.theme.*

@Composable
fun AdminAuthDialog(
    hasRegisteredAdminKey: Boolean,
    onDismiss: () -> Unit,
    onAuthorize: (String) -> Unit
) {
    var adminKeyInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🛡️", fontSize = 40.sp)

                Text(
                    text = "Admin Super-Key Access",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (!hasRegisteredAdminKey) {
                    Text(
                        text = "There is no super access. You have to put keys to get access.\nNo super access allowed.",
                        fontSize = 12.sp,
                        color = DangerRed,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)) {
                        Text("Dismiss")
                    }
                } else {
                    Text(
                        text = "Enter your account Admin Key to bypass all protections and authorize master rights.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    OutlinedTextField(
                        value = adminKeyInput,
                        onValueChange = {
                            adminKeyInput = it
                            errorMessage = null
                        },
                        label = { Text("Admin Super-Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = DangerRed, fontSize = 11.sp)
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (adminKeyInput.isBlank()) {
                                    errorMessage = "Please enter Admin Key"
                                } else {
                                    onAuthorize(adminKeyInput.trim())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                        ) {
                            Text("Authorize", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
