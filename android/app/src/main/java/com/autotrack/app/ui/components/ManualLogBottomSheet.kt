package com.autotrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autotrack.app.data.Category
import com.autotrack.app.ui.theme.*

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLogBottomSheet(
    onDismissRequest: () -> Unit,
    categories: List<Category>,
    onSaveTransaction: (amount: Double, type: String, vendor: String?, categoryId: String?, paymentMethod: String, note: String?) -> Unit,
    onCreateCategory: (name: String, icon: String, cap: Double?) -> Unit
) {
    // Level 1 or 2 State
    var level by remember { mutableStateOf(1) }
    var type by remember { mutableStateOf("expense") } // "expense" = Outcome, "income" = Income
    var receiverVendor by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var level1Error by remember { mutableStateOf<String?>(null) }
    var errorField by remember { mutableStateOf<String?>(null) } // "toFrom" or "amount"

    // Level 2 State
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var paymentMethod by remember { mutableStateOf("UPI") } // "UPI" or "Cash"
    var note by remember { mutableStateOf("") }
    var showCreateCatDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .statusBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Semi-transparent backdrop (tapping outside closes dialog)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.70f))
                    .clickable(onClick = onDismissRequest)
            )

            // Top Floating Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .padding(top = 36.dp, bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = CardDefaults.outlinedCardBorder(enabled = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
            if (level == 1) {
                /* ================= LEVEL 1 VIEW ================= */
                Text(
                    text = "Log Transaction",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Row 1: Outcome (-) / Income (+) Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            type = "expense"
                            level1Error = null
                            errorField = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "expense") RoseExpense else Color.Transparent,
                            contentColor = if (type == "expense") Color.White else TextMuted
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Outcome (-)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            type = "income"
                            level1Error = null
                            errorField = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == "income") EmeraldPrimary else Color.Transparent,
                            contentColor = if (type == "income") Color.White else TextMuted
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Income (+)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Row 2: TO / FROM Field
                Text(
                    text = if (type == "expense") "TO (Receiver / Vendor)" else "FROM (Sender / Source)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = receiverVendor,
                    onValueChange = {
                        receiverVendor = it
                        if (errorField == "toFrom") {
                            errorField = null
                            level1Error = null
                        }
                    },
                    placeholder = {
                        Text(
                            text = if (type == "expense") "e.g. Tea Stall, Zomato, Mother..." else "e.g. Salary, Mother, Friend...",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    },
                    isError = errorField == "toFrom",
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = BorderColor,
                        errorBorderColor = RoseExpense,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Row 3: Amount Field & Continue Button
                Text(
                    text = "Amount",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = {
                            amount = it
                            if (errorField == "amount") {
                                errorField = null
                                level1Error = null
                            }
                        },
                        placeholder = { Text("0.00", fontSize = 14.sp, color = TextMuted) },
                        prefix = { Text("₹ ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = errorField == "amount",
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg,
                            focusedBorderColor = if (type == "expense") RoseExpense else EmeraldPrimary,
                            unfocusedBorderColor = BorderColor,
                            errorBorderColor = RoseExpense,
                            focusedTextColor = if (type == "expense") RoseExpense else EmeraldPrimary,
                            unfocusedTextColor = if (type == "expense") RoseExpense else EmeraldPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            if (receiverVendor.trim().isEmpty()) {
                                errorField = "toFrom"
                                level1Error = if (type == "expense") "Please enter TO (Receiver / Vendor) name" else "Please enter FROM (Sender / Source) name"
                                return@Button
                            }
                            val num = amount.toDoubleOrNull()
                            if (num == null || num <= 0) {
                                errorField = "amount"
                                level1Error = "Please enter amount"
                                return@Button
                            }
                            errorField = null
                            level1Error = null
                            level = 2
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Continue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                if (level1Error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• ${level1Error!!}",
                        fontSize = 11.sp,
                        color = RoseExpense,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                /* ================= LEVEL 2 VIEW ================= */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (type == "expense") "- Outcome" else "+ Income"}: ₹${amount.toDoubleOrNull() ?: 0.0}",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (type == "expense") RoseExpense else EmeraldPrimary
                    )

                    OutlinedButton(
                        onClick = { level = 1 },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Back", fontSize = 11.sp, color = Color.White)
                    }
                }

                HorizontalDivider(color = BorderColor, modifier = Modifier.padding(bottom = 16.dp))

                // Category Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Category", fontSize = 11.sp, color = Color.LightGray)
                    Text(
                        text = "+ New Category",
                        fontSize = 11.sp,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showCreateCatDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                var catDropdownExpanded by remember { mutableStateOf(false) }
                val activeCatName = categories.find { it.id == selectedCategoryId }?.let { "${it.icon} ${it.name}" } ?: "📦 Uncategorized"

                Box {
                    OutlinedButton(
                        onClick = { catDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(activeCatName, color = Color.White, fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                        }
                    }

                    DropdownMenu(
                        expanded = catDropdownExpanded,
                        onDismissRequest = { catDropdownExpanded = false },
                        modifier = Modifier.background(CardBg).border(1.dp, BorderColor)
                    ) {
                        DropdownMenuItem(
                            text = { Text("📦 Uncategorized", color = Color.White, fontSize = 12.sp) },
                            onClick = {
                                selectedCategoryId = null
                                catDropdownExpanded = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.icon} ${cat.name}", color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    catDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Method (UPI vs Cash)
                Text("Payment Method", fontSize = 11.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { paymentMethod = "UPI" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (paymentMethod == "UPI") EmeraldPrimary else Color.Transparent,
                            contentColor = if (paymentMethod == "UPI") Color.White else TextMuted
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("UPI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { paymentMethod = "Cash" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (paymentMethod == "Cash") AmberWarning else Color.Transparent,
                            contentColor = if (paymentMethod == "Cash") Color.White else TextMuted
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cash", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notes (Optional)
                Text("Notes (Optional)", fontSize = 11.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("e.g. Tea with friends...", fontSize = 12.sp, color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Row: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextMuted, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val num = amount.toDoubleOrNull() ?: 0.0
                            onSaveTransaction(
                                num,
                                type,
                                receiverVendor.ifBlank { null },
                                selectedCategoryId,
                                paymentMethod,
                                note.ifBlank { null }
                            )
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(2f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Transaction", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                }
            }
        }
    }

    // Inline Create Category Dialog
    if (showCreateCatDialog) {
        var newCatName by remember { mutableStateOf("") }
        var newCatIcon by remember { mutableStateOf("🏷️") }
        var newCatCap by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateCatDialog = false },
            title = { Text("Create New Category", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCatIcon,
                        onValueChange = { newCatIcon = it },
                        label = { Text("Icon Emoji (e.g. 🍕)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCatCap,
                        onValueChange = { newCatCap = it },
                        label = { Text("Opening Balance (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            val cap = newCatCap.toDoubleOrNull()
                            onCreateCategory(newCatName.trim(), newCatIcon.ifBlank { "🏷️" }, cap)
                            showCreateCatDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCatDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = CardBg
        )
    }
}
}

