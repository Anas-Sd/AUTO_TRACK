package com.autotrack.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.autotrack.app.data.Category
import com.autotrack.app.data.TransactionItem
import com.autotrack.app.ui.theme.*

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.AccessTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun parseIsoToIstPair(isoStr: String): Pair<String, String> {
    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
    val sdfTime = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
    if (isoStr.isBlank()) {
        val now = Date()
        return Pair(sdfDate.format(now), sdfTime.format(now))
    }
    val parsers = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") },
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
    )
    for (parser in parsers) {
        try {
            val date = parser.parse(isoStr)
            if (date != null) return Pair(sdfDate.format(date), sdfTime.format(date))
        } catch (_: Exception) {}
    }
    val now = Date()
    return Pair(sdfDate.format(now), sdfTime.format(now))
}

private fun formatIstPairToIso(dateStr: String, timeStr: String): String {
    try {
        val combined = "$dateStr $timeStr"
        val sdfInput = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        val date = sdfInput.parse(combined)
        val sdfOutput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        return sdfOutput.format(date ?: Date())
    } catch (e: Exception) {
        val sdfOutput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        return sdfOutput.format(Date())
    }
}

@Composable
fun EditTransactionDialog(
    transaction: TransactionItem,
    categories: List<Category>,
    onDismissRequest: () -> Unit,
    onSaveTransaction: (
        id: String,
        amount: Double,
        type: String,
        vendor: String?,
        categoryId: String?,
        sourceApp: String,
        note: String?,
        occurredAt: String?
    ) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(transaction.type) }
    var amountText by remember { mutableStateOf(transaction.amount.toInt().toString()) }
    var noteText by remember { mutableStateOf(transaction.note ?: "") }
    var vendorText by remember { mutableStateOf(transaction.receiverVendor ?: "") }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var paymentMethod by remember { mutableStateOf(if (transaction.sourceApp.equals("Cash", ignoreCase = true)) "Cash" else "UPI") }

    val initialPair = remember(transaction.occurredAt) { parseIsoToIstPair(transaction.occurredAt) }
    var selectedDate by remember { mutableStateOf(initialPair.first) }
    var selectedTime by remember { mutableStateOf(initialPair.second) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var catDropdownExpanded by remember { mutableStateOf(false) }

    fun showDatePicker() {
        val parts = selectedDate.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
        val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
        DatePickerDialog(context, { _, y, m, d ->
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
        }, year, month, day).show()
    }

    fun showTimePicker() {
        val parts = selectedTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(context, { _, h, m ->
            selectedTime = String.format(Locale.US, "%02d:%02d", h, m)
        }, hour, minute, true).show()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(onClick = onDismissRequest)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = CardDefaults.outlinedCardBorder(enabled = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            Text("Edit Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        IconButton(onClick = onDismissRequest, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    // 1. Income / Outcome Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { type = "expense" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "expense") RoseExpense else Color.Transparent,
                                contentColor = if (type == "expense") Color.White else TextMuted
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Outcome (-)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { type = "income" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "income") EmeraldPrimary else Color.Transparent,
                                contentColor = if (type == "income") Color.White else TextMuted
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Income (+)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 2. Amount Field
                    Column {
                        Text("Amount (₹)", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = {
                                amountText = it
                                errorMessage = null
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 3. Note Field
                    Column {
                        Text("* Notes / Payee (Mandatory)", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = {
                                noteText = it
                                if (errorMessage != null) errorMessage = null
                            },
                            placeholder = { Text("Add transaction note...", fontSize = 11.sp, color = TextMuted) },
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 4. Category Dropdown Selector
                    Column {
                        Text("Category", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))

                        val sortedCategories = remember(categories, transaction) {
                            com.autotrack.app.DataSyncManager.getSortedCategoriesByRecency(categories, listOf(transaction))
                        }
                        val activeCatName = sortedCategories.find { it.id == selectedCategoryId }?.let { "${it.icon} ${it.name}" } ?: "📦 Uncategorized"

                        Box {
                            OutlinedButton(
                                onClick = { catDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkBg),
                                border = BorderStroke(1.dp, BorderColor),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(activeCatName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                                }
                            }

                            DropdownMenu(
                                expanded = catDropdownExpanded,
                                onDismissRequest = { catDropdownExpanded = false },
                                modifier = Modifier.heightIn(max = 210.dp).background(CardBg).border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📦 Uncategorized", color = Color.White, fontSize = 11.sp) },
                                    onClick = {
                                        selectedCategoryId = null
                                        catDropdownExpanded = false
                                    }
                                )
                                sortedCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text("${cat.icon} ${cat.name}", color = Color.White, fontSize = 11.sp) },
                                        onClick = {
                                            selectedCategoryId = cat.id
                                            catDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 5. Payment Method (UPI vs Cash)
                    Column {
                        Text("Payment Method", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBg, RoundedCornerShape(10.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { paymentMethod = "UPI" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (paymentMethod == "UPI") EmeraldPrimary else Color.Transparent,
                                    contentColor = if (paymentMethod == "UPI") Color.White else TextMuted
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("UPI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { paymentMethod = "Cash" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (paymentMethod == "Cash") AmberWarning else Color.Transparent,
                                    contentColor = if (paymentMethod == "Cash") Color.White else TextMuted
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cash", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 6. Date & Time Selection (Editable)
                    Column {
                        Text("Date & Time (IST)", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDatePicker() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkBg),
                                border = BorderStroke(1.dp, BorderColor),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                    Text(selectedDate, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            OutlinedButton(
                                onClick = { showTimePicker() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkBg),
                                border = BorderStroke(1.dp, BorderColor),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                    Text(selectedTime, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(errorMessage!!, fontSize = 11.sp, color = RoseExpense, fontWeight = FontWeight.Bold)
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Text("Cancel", color = TextMuted, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val parsedAmount = amountText.toDoubleOrNull()
                                if (parsedAmount == null || parsedAmount <= 0) {
                                    errorMessage = "Enter valid amount"
                                    return@Button
                                }
                                if (noteText.isBlank() && vendorText.isBlank()) {
                                    errorMessage = "Notes / Payee is required"
                                    return@Button
                                }
                                val formattedIso = formatIstPairToIso(selectedDate, selectedTime)
                                onSaveTransaction(
                                    transaction.id,
                                    parsedAmount,
                                    type,
                                    vendorText.ifBlank { null },
                                    selectedCategoryId,
                                    paymentMethod,
                                    noteText.ifBlank { null },
                                    formattedIso
                                )
                                onDismissRequest()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Save Changes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
