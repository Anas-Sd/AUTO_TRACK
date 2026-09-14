package com.autotrack.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autotrack.app.ui.theme.*

@Composable
fun SettingsScreen(
    vaultCode: String,
    onRotateCode: () -> Unit,
    onWipeData: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var showCode by remember { mutableStateOf(false) }
    var showRotateDialog by remember { mutableStateOf(false) }
    var showWipeDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
    ) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = EmeraldPrimary)
                    Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("Manage vault profile, vault code security, and diagnostics", fontSize = 11.sp, color = TextMuted)
            }
        }

        // Vault Access Code Card (Displayed in Android App)
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

        // System Diagnostics Card
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

        // Danger Zone Card
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

                    Divider(color = BorderColor)

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

    if (showRotateDialog) {
        AlertDialog(
            onDismissRequest = { showRotateDialog = false },
            title = { Text("Rotate Vault Code", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to generate a new Vault Code?", fontSize = 12.sp, color = TextMuted) },
            confirmButton = {
                Button(
                    onClick = {
                        onRotateCode()
                        showRotateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Rotate Code")
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
