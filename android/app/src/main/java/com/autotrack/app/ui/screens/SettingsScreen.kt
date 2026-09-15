package com.autotrack.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autotrack.app.DataSyncManager
import com.autotrack.app.ui.theme.*

@Composable
fun SettingsScreen(
    vaultCode: String,
    profileName: String,
    onSaveProfileName: (String) -> Unit,
    onRotateCode: (customCode: String?) -> Unit,
    onWipeData: () -> Unit,
    onLogout: () -> Unit,
    onSyncQueue: () -> Unit = {}
) {
    val context = LocalContext.current
    var showCode by remember { mutableStateOf(false) }
    var showRotateDialog by remember { mutableStateOf(false) }
    var showWipeDialog by remember { mutableStateOf(false) }

    var nameInput by remember { mutableStateOf(profileName) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        // 1. Header Title
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = EmeraldPrimary)
                    Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("Manage profile name, vault code security, and diagnostics", fontSize = 11.sp, color = TextMuted)
            }
        }

        // 2. Profile Name Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(enabled = true),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                        Text("Profile Name", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Text("Displayed on top of the Financial Overview screen", fontSize = 10.sp, color = TextMuted)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            placeholder = { Text("e.g. Anas", fontSize = 12.sp, color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                if (nameInput.isNotBlank()) {
                                    onSaveProfileName(nameInput.trim())
                                    Toast.makeText(context, "Profile name updated!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Vault Access Code Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(enabled = true),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Text("Vault Access Code", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text("Secret Key", fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (showCode) vaultCode else "••••••••",
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    IconButton(
                                        onClick = { showCode = !showCode },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            if (showCode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Vault Code", vaultCode)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Vault Code copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy", fontSize = 11.sp, color = Color.White)
                                    }

                                    Button(
                                        onClick = { showRotateDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = EmeraldPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Rotate", fontSize = 11.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text(
                                "Use this Vault Code to log into your account across devices. Keep it secret.",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        // 4. Offline Transaction Queue Card
        item {
            val pendingQueue = remember { DataSyncManager.getPendingOfflineQueue() }
            val count = pendingQueue.size

            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(enabled = true),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Text("Offline Transaction Queue", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Surface(
                            color = if (count > 0) AmberWarning.copy(alpha = 0.2f) else EmeraldPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (count > 0) "$count Pending" else "100% Synced",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (count > 0) AmberWarning else EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = if (count > 0) "Transactions logged while offline are queued locally in SQLite and will sync automatically when internet is connected." else "No pending offline transactions. All data is fully synced to Supabase.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    if (count > 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            pendingQueue.take(5).forEach { (_, json) ->
                                val amt = json.optDouble("amount", 0.0)
                                val type = json.optString("type", "expense")
                                val note = json.optString("note", json.optString("receiver_vendor", "Offline Log"))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkBg, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (note.isBlank()) "Offline Log" else note, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${if (type == "income") "+" else "-"}₹${amt.toInt()}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (type == "income") EmeraldPrimary else RoseExpense,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onSyncQueue,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Queue Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. System Diagnostics Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(enabled = true),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("System Diagnostics", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(DarkBg, RoundedCornerShape(12.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("DATABASE SYNC", fontSize = 8.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                Text("Supabase PostgreSQL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Live Sync Active", fontSize = 9.sp, color = EmeraldPrimary)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(DarkBg, RoundedCornerShape(12.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text("OFFLINE QUEUE", fontSize = 8.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                Text("SQLite Local Storage", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Offline-first ready", fontSize = 9.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }
        }

        // 5. Danger Zone Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(enabled = true),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clear All Data", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RoseExpense)
                            Text("Permanently delete all transactions & categories in vault", fontSize = 10.sp, color = TextMuted)
                        }
                        Button(
                            onClick = { showWipeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RoseExpense.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = RoseExpense, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wipe Data", fontSize = 11.sp, color = RoseExpense, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = BorderColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lock & Log Out", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Clears active session token", fontSize = 10.sp, color = TextMuted)
                        }
                        OutlinedButton(
                            onClick = onLogout,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Out", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Rotate Vault Code Modal Dialog
    if (showRotateDialog) {
        var customCodeInput by remember { mutableStateOf("") }
        var rotateError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showRotateDialog = false },
            title = { Text("Rotate / Change Vault Code", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter your custom Vault Code or generate a random one. All database records will be migrated automatically, and you will be logged out.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    OutlinedTextField(
                        value = customCodeInput,
                        onValueChange = {
                            customCodeInput = it
                            rotateError = null
                        },
                        placeholder = { Text("Custom code (or leave empty for random)", fontSize = 11.sp, color = TextMuted) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (rotateError != null) {
                        Text(rotateError!!, fontSize = 11.sp, color = RoseExpense, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val codeToUse = customCodeInput.trim()
                        if (codeToUse.isNotBlank() && codeToUse.length < 4) {
                            rotateError = "Vault code must be at least 4 characters"
                            return@Button
                        }
                        onRotateCode(if (codeToUse.isBlank()) null else codeToUse)
                        showRotateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Rotate & Migrate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRotateDialog = false }) { Text("Cancel", color = TextMuted) }
            },
            containerColor = CardBg
        )
    }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = { Text("Wipe All Vault Data", color = RoseExpense, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete ALL transactions and categories? This action cannot be undone.", fontSize = 12.sp, color = TextMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        onWipeData()
                        showWipeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseExpense)
                ) {
                    Text("Wipe Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) { Text("Cancel", color = TextMuted) }
            },
            containerColor = CardBg
        )
    }
}
