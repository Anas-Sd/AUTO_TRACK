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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.autotrack.app.data.Category
import com.autotrack.app.ui.theme.*

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
    var amount by remember { mutableStateOf("") }
    var receiverVendor by remember { mutableStateOf("") } // Notes / Vendor
    var level1Error by remember { mutableStateOf<String?>(null) }
    var errorField by remember { mutableStateOf<String?>(null) }

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
            // Backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.70f))
                    .clickable(onClick = onDismissRequest)
            )

            // Sleek Floating Card (Reduced by ~25%)
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(top = 32.dp, bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = CardDefaults.outlinedCardBorder(enabled = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    if (level == 1) {
                        /* ================= LEVEL 1 VIEW ================= */
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Log Transaction",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            )
                            Text(
                                text = "Category Options →",
                                fontSize = 11.sp,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { level = 2 }
                            )
                        }

                        // Row 1: Outcome (-) / Income (+) Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBg, RoundedCornerShape(10.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Outcome (-)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Income (+)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Row 2: Amount Field (Replaces TO/FROM)
                        Text(
                            text = "Amount",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        OutlinedTextField(
                            value = amount,
                            onValueChange = {
                                amount = it
                                if (errorField == "amount") {
                                    errorField = null
                                    level1Error = null
                                }
                            },
                            placeholder = { Text("0.00", fontSize = 13.sp, color = TextMuted) },
                            prefix = { Text("₹ ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMuted) },
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
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Row 3: Notes (Optional) Field
                        Text(
                            text = "Notes (Optional)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        OutlinedTextField(
                            value = receiverVendor,
                            onValueChange = {
                                receiverVendor = it
                                if (errorField == "notes") {
                                    errorField = null
                                    level1Error = null
                                }
                            },
                            placeholder = {
                                Text(
                                    text = if (type == "expense") "e.g. Tea Stall, Zomato..." else "e.g. Salary, Friend...",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            },
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

                        if (level1Error != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• ${level1Error!!}",
                                fontSize = 10.sp,
                                color = RoseExpense,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Single Continue Button in Level 1 (Advances to Level 2)
                        Button(
                            onClick = {
                                val num = amount.toDoubleOrNull()
                                if (num == null || num <= 0) {
                                    errorField = "amount"
                                    level1Error = "Please enter a valid amount"
                                    return@Button
                                }
                                level = 2
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("Continue →", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        /* ================= LEVEL 2 VIEW ================= */
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${if (type == "expense") "- Outcome" else "+ Income"}: ₹${amount.toDoubleOrNull() ?: 0.0}",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (type == "expense") RoseExpense else EmeraldPrimary
                            )

                            OutlinedButton(
                                onClick = { level = 1 },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(13.dp), tint = EmeraldPrimary)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Back", fontSize = 10.sp, color = Color.White)
                            }
                        }

                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(bottom = 12.dp))

                        // Category Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Category", fontSize = 10.sp, color = Color.LightGray)
                            Text(
                                text = "+ New Category",
                                fontSize = 10.sp,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { showCreateCatDialog = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        var catDropdownExpanded by remember { mutableStateOf(false) }
                        val activeCatName = categories.find { it.id == selectedCategoryId }?.let { "${it.icon} ${it.name}" } ?: "📦 Uncategorized"

                        Box {
                            OutlinedButton(
                                onClick = { catDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkBg),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(activeCatName, color = Color.White, fontSize = 11.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                                }
                            }

                            DropdownMenu(
                                expanded = catDropdownExpanded,
                                onDismissRequest = { catDropdownExpanded = false },
                                modifier = Modifier.background(CardBg).border(1.dp, BorderColor)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📦 Uncategorized", color = Color.White, fontSize = 11.sp) },
                                    onClick = {
                                        selectedCategoryId = null
                                        catDropdownExpanded = false
                                    }
                                )
                                categories.forEach { cat ->
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Payment Method (UPI vs Cash)
                        Text("Payment Method", fontSize = 10.sp, color = Color.LightGray)
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3 Buttons in Level 2 (Cancel/Back, Continue+, Save)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1. Cancel / Back Button
                            OutlinedButton(
                                onClick = { level = 1 },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("Cancel", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            }

                            // 2. Continue+ Button (Saves & resets to Level 1 with clean fields for consecutive logging)
                            Button(
                                onClick = {
                                    val num = amount.toDoubleOrNull() ?: 0.0
                                    onSaveTransaction(
                                        num,
                                        type,
                                        receiverVendor.ifBlank { null },
                                        selectedCategoryId,
                                        paymentMethod,
                                        receiverVendor.ifBlank { null }
                                    )
                                    // Reset to Level 1 & clear fields
                                    level = 1
                                    amount = ""
                                    receiverVendor = ""
                                    selectedCategoryId = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("Continue +", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // 3. Save Button (Saves & closes popup)
                            Button(
                                onClick = {
                                    val num = amount.toDoubleOrNull() ?: 0.0
                                    onSaveTransaction(
                                        num,
                                        type,
                                        receiverVendor.ifBlank { null },
                                        selectedCategoryId,
                                        paymentMethod,
                                        receiverVendor.ifBlank { null }
                                    )
                                    onDismissRequest()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("Save ✔", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Inline Create Category Dialog
    if (showCreateCatDialog) {
        var newCatName by remember { mutableStateOf("") }
        var newCatIcon by remember { mutableStateOf("🍕") }
        var newCatCap by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateCatDialog = false },
            title = { Text("Create New Category", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category Name", fontSize = 11.sp) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCatIcon,
                        onValueChange = { newCatIcon = it },
                        label = { Text("Icon Emoji (e.g. 🍕)", fontSize = 11.sp) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCatCap,
                        onValueChange = { newCatCap = it },
                        label = { Text("Opening Balance (Optional)", fontSize = 11.sp) },
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
                    Text("Create", fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCatDialog = false }) {
                    Text("Cancel", color = TextMuted, fontSize = 11.sp)
                }
            },
            containerColor = CardBg
        )
    }
}
