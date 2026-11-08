package com.autotrack.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.autotrack.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class ExportFormat(val extension: String, val label: String) {
    CSV("csv", "CSV"),
    PDF("pdf", "PDF (Searchable)")
}

@Composable
fun ExportLedgerDialog(
    isOpen: Boolean,
    transactionCount: Int,
    onDismiss: () -> Unit,
    onConfirmExport: (fileName: String, format: ExportFormat) -> Unit
) {
    if (!isOpen) return

    var baseName by remember { mutableStateOf("AutoTrack_Ledger") }
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }

    // Today's date in IST (yyyy-MM-dd)
    val todayDate = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        sdf.format(Date())
    }

    val cleanName = remember(baseName) {
        val sanitized = baseName.trim().replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
        if (sanitized.isBlank()) "AutoTrack_Ledger" else sanitized
    }

    val previewFullName = "$cleanName" + "_" + "$todayDate.${selectedFormat.extension}"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardBg,
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(EmeraldPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .border(1.dp, EmeraldPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                "Export Ledger",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Exporting $transactionCount transactions",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = BorderColor, thickness = 1.dp)

                // 1. File Name Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "1. File Name",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = baseName,
                        onValueChange = { baseName = it },
                        placeholder = { Text("e.g. AutoTrack_Ledger", fontSize = 13.sp, color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        )
                    )

                    // Live Preview of appended date & extension
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Will be saved as:",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        Text(
                            previewFullName,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }

                // 2. File Format Selection (CSV or Searchable PDF)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "2. Select File Format",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // CSV Option
                        val isCsvSelected = selectedFormat == ExportFormat.CSV
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isCsvSelected) EmeraldPrimary.copy(alpha = 0.12f) else DarkBg,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isCsvSelected) EmeraldPrimary else BorderColor,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedFormat = ExportFormat.CSV }
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.TableChart,
                                            contentDescription = null,
                                            tint = if (isCsvSelected) EmeraldPrimary else TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "CSV",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(
                                                if (isCsvSelected) EmeraldPrimary else Color.Transparent,
                                                CircleShape
                                            )
                                            .border(1.dp, if (isCsvSelected) EmeraldPrimary else TextMuted, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCsvSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(Color.White, CircleShape)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "Spreadsheet data (.csv)",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    lineHeight = 12.sp
                                )
                            }
                        }

                        // Searchable PDF Option
                        val isPdfSelected = selectedFormat == ExportFormat.PDF
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isPdfSelected) EmeraldPrimary.copy(alpha = 0.12f) else DarkBg,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isPdfSelected) EmeraldPrimary else BorderColor,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedFormat = ExportFormat.PDF }
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = if (isPdfSelected) EmeraldPrimary else TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "PDF",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(
                                                if (isPdfSelected) EmeraldPrimary else Color.Transparent,
                                                CircleShape
                                            )
                                            .border(1.dp, if (isPdfSelected) EmeraldPrimary else TextMuted, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isPdfSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(Color.White, CircleShape)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "Searchable PDF (.pdf)",
                                    fontSize = 10.sp,
                                    color = EmeraldPrimary,
                                    lineHeight = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onConfirmExport(cleanName, selectedFormat)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Download ${selectedFormat.extension.uppercase()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
