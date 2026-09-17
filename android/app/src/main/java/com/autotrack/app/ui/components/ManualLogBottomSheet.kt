package com.autotrack.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.autotrack.app.data.Category
import com.autotrack.app.data.TransactionItem
import com.autotrack.app.ui.theme.*

private data class InstalledPaymentApp(
    val name: String,
    val packageName: String,
    val iconEmoji: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLogBottomSheet(
    onDismissRequest: () -> Unit,
    categories: List<Category>,
    onSaveTransaction: (amount: Double, type: String, vendor: String?, categoryId: String?, paymentMethod: String, note: String?) -> Unit,
    onCreateCategory: (name: String, icon: String, cap: Double?) -> Unit,
    latestTransaction: TransactionItem? = null,
    onDeleteTransaction: ((id: String) -> Unit)? = null
) {
    val context = LocalContext.current

    // Level State:
    // Level 1 = Main Action Selection (Pic 1 - Level 1)
    // Level 2 = Form Details (Pic 1 Level 2 for Log, Pic 2 Level 2 for Pay & Log, Pic 3 Level 2 for Undo)
    // Level 3 = Extra options (Pic 1 Level 3 for Log, Pic 2 Level 3 App Launcher for Pay & Log)
    var level by remember { mutableIntStateOf(1) }

    // Mode Selection: "log", "payment", "undo"
    var selectedMode by remember { mutableStateOf("log") }

    // Form State
    var type by remember { mutableStateOf("expense") } // "expense" = Outcome, "income" = Income
    var amount by remember { mutableStateOf("") }
    var receiverVendor by remember { mutableStateOf("") } // Notes / Vendor
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var errorField by remember { mutableStateOf<String?>(null) }

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var paymentMethod by remember { mutableStateOf("UPI") }
    var showCreateCatDialog by remember { mutableStateOf(false) }

    // Auto-discover installed payment apps dynamically
    val installedPaymentApps = remember(context) {
        val pm = context.packageManager

        val targetAppSpecs = listOf(
            Pair("GPay", listOf("com.google.android.apps.nbu.paisa.user") to Pair("🔵", Color(0xFF4285F4))),
            Pair("PhonePe", listOf("com.phonepe.app", "com.phonepe.android", "com.phonepe.simulator") to Pair("🟣", Color(0xFF5F259F))),
            Pair("Paytm", listOf("net.one97.paytm") to Pair("🔷", Color(0xFF00BAF2))),
            Pair("Navi", listOf("com.navi.android", "com.naviapp", "com.navi.finance", "com.navi.mutualfund") to Pair("🟢", Color(0xFF00D09C))),
            Pair("Super.money", listOf("tech.super.money", "com.supermoney.app", "money.super.app", "com.flipkart.supermoney") to Pair("⚡", Color(0xFFEAB308))),
            Pair("BHIM", listOf("in.org.npci.upiapp") to Pair("🟠", Color(0xFFEA580C))),
            Pair("CRED", listOf("com.dreamplug.androidapp") to Pair("🖤", Color(0xFF374151)))
        )

        val list = mutableListOf<InstalledPaymentApp>()

        // 1. Scan configured app package aliases
        for ((name, pair) in targetAppSpecs) {
            val (packages, meta) = pair
            val (icon, color) = meta
            var installedPkg = ""
            for (pkg in packages) {
                val intent = pm.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    installedPkg = pkg
                    break
                }
            }
            if (installedPkg.isNotEmpty()) {
                list.add(InstalledPaymentApp(name, installedPkg, icon, color))
            }
        }

        // 2. Query system-wide UPI handlers so no installed payment app is missed
        val upiIntent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
        val resolveInfos = try { pm.queryIntentActivities(upiIntent, 0) } catch (e: Exception) { emptyList() }
        for (ri in resolveInfos) {
            val pkg = ri.activityInfo.packageName
            val label = ri.loadLabel(pm).toString()
            if (list.none { it.packageName == pkg }) {
                list.add(InstalledPaymentApp(label.take(12), pkg, "📲", EmeraldPrimary))
            }
        }

        if (list.isEmpty()) {
            list.add(InstalledPaymentApp("UPI Apps", "", "📲", EmeraldPrimary))
        }
        list
    }

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
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(onClick = onDismissRequest)
            )

            // Main Popup Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .padding(top = 28.dp, bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = CardDefaults.outlinedCardBorder(enabled = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    /* ========================================================================= */
                    /* LEVEL 1: STARTING SELECTION SCREEN (Pic 1 - Level 1)                      */
                    /* ========================================================================= */
                    if (level == 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("AutoTrack Quick Action", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            IconButton(onClick = onDismissRequest, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                            }
                        }

                        HorizontalDivider(color = BorderColor)

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Option 1: LOG Transaction
                            Button(
                                onClick = {
                                    selectedMode = "log"
                                    level = 2
                                    errorMsg = null
                                    errorField = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("LOG Transaction", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // Option 2: Pay & Log
                            Button(
                                onClick = {
                                    selectedMode = "payment"
                                    type = "expense" // Pay is 100% Outcome / Expense
                                    paymentMethod = "UPI" // Pay is 100% UPI
                                    level = 2
                                    errorMsg = null
                                    errorField = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pay & Log", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // Option 3: UNDO
                            Button(
                                onClick = {
                                    selectedMode = "undo"
                                    level = 2
                                    errorMsg = null
                                    errorField = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RoseExpense),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("UNDO", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    /* ========================================================================= */
                    /* MODE 1: LOG TRANSACTION (Pic 1 - Level 2 & Level 3)                      */
                    /* ========================================================================= */
                    else if (selectedMode == "log") {
                        if (level == 2) {
                            // Pic 1 - Level 2 (Log Transaction Level 2)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { level = 1 },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, BorderColor),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(13.dp), tint = EmeraldPrimary)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Menu", fontSize = 10.sp, color = Color.White)
                                }

                                Text("📝 Log Transaction", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)

                                IconButton(onClick = onDismissRequest, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                                }
                            }

                            HorizontalDivider(color = BorderColor)

                            // Toggle Row: Outcome vs Income
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
                                        errorMsg = null
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
                                    Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Outcome (-)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        type = "income"
                                        errorMsg = null
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
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Income (+)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Field 1: Amount (Mandatory *)
                            Column {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("* Amount (Mandatory)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                    if (errorField == "amount") {
                                        Text("Required", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoseExpense)
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                OutlinedTextField(
                                    value = amount,
                                    onValueChange = {
                                        amount = it
                                        if (errorField == "amount") {
                                            errorField = null
                                            errorMsg = null
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
                            }

                            // Field 2: Notes / Payee (*)
                            Column {
                                Text("* Notes / Payee", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                Spacer(modifier = Modifier.height(3.dp))
                                OutlinedTextField(
                                    value = receiverVendor,
                                    onValueChange = { receiverVendor = it },
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
                            }

                            if (errorMsg != null) {
                                Text("• $errorMsg", fontSize = 10.sp, color = RoseExpense, fontWeight = FontWeight.Bold)
                            }

                            // Level 2 Action Buttons (Cancel | Continue)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismissRequest,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BorderColor)
                                ) {
                                    Text("Cancel", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val num = amount.toDoubleOrNull()
                                        if (num == null || num <= 0) {
                                            errorField = "amount"
                                            errorMsg = "Please enter a valid amount"
                                            return@Button
                                        }
                                        level = 3
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Continue →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        } else if (level == 3) {
                            // Pic 1 - Level 3 (Log Transaction Level 3)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Transaction - ₹${amount.toDoubleOrNull() ?: 0.0}",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (type == "expense") RoseExpense else EmeraldPrimary
                                )

                                OutlinedButton(
                                    onClick = { level = 2 },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, BorderColor),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(13.dp), tint = EmeraldPrimary)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Back", fontSize = 10.sp, color = Color.White)
                                }
                            }

                            HorizontalDivider(color = BorderColor)

                            // Category Selector with [+] button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Category", fontSize = 10.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showCreateCatDialog = true }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Category", tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Category", fontSize = 10.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                }
                            }

                            var catDropdownExpanded by remember { mutableStateOf(false) }
                            val activeCatName = categories.find { it.id == selectedCategoryId }?.let { "${it.icon} ${it.name}" } ?: "📦 Uncategorized"

                            Box {
                                OutlinedButton(
                                    onClick = { catDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkBg),
                                    border = BorderStroke(1.dp, BorderColor)
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

                            // Payment Method Selection (UPI vs Cash)
                            Text("Payment Method", fontSize = 10.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
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

                            // Pic 1 - Level 3 Action Buttons (Cancel | Continue | Save)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismissRequest,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
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
                                            receiverVendor.ifBlank { null }
                                        )
                                        level = 2
                                        amount = ""
                                        receiverVendor = ""
                                        selectedCategoryId = null
                                        Toast.makeText(context, "Logged! Add next...", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.1f)
                                ) {
                                    Text("Continue +", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                            receiverVendor.ifBlank { null }
                                        )
                                        onDismissRequest()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Save ✔", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    /* ========================================================================= */
                    /* MODE 2: PAY & LOG (Pic 2 - Level 2 & Level 3)                            */
                    /* ========================================================================= */
                    else if (selectedMode == "payment") {
                        if (level == 2) {
                            // Pic 2 - Level 2 (Pay & Log Level 2)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { level = 1 },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, BorderColor),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(13.dp), tint = EmeraldPrimary)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Menu", fontSize = 10.sp, color = Color.White)
                                }

                                Text("⚡ Pay & Log", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)

                                IconButton(onClick = onDismissRequest, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                                }
                            }

                            HorizontalDivider(color = BorderColor)

                            // Field 1: Amount (Mandatory *)
                            Column {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("* Amount (Mandatory)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                    if (errorField == "amount") {
                                        Text("Required", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoseExpense)
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                OutlinedTextField(
                                    value = amount,
                                    onValueChange = {
                                        amount = it
                                        if (errorField == "amount") {
                                            errorField = null
                                            errorMsg = null
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
                                        focusedBorderColor = Color(0xFF0284C7),
                                        unfocusedBorderColor = BorderColor,
                                        errorBorderColor = RoseExpense,
                                        focusedTextColor = Color(0xFF0284C7),
                                        unfocusedTextColor = Color(0xFF0284C7)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Field 2: Notes / Payee
                            Column {
                                Text("Notes / Payee", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                Spacer(modifier = Modifier.height(3.dp))
                                OutlinedTextField(
                                    value = receiverVendor,
                                    onValueChange = { receiverVendor = it },
                                    placeholder = { Text("e.g. Shop, Friend...", fontSize = 11.sp, color = TextMuted) },
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

                            // Field 3: Category Selector with [+] button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Category", fontSize = 10.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showCreateCatDialog = true }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Category", tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Category", fontSize = 10.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                }
                            }

                            var catDropdownExpanded by remember { mutableStateOf(false) }
                            val activeCatName = categories.find { it.id == selectedCategoryId }?.let { "${it.icon} ${it.name}" } ?: "📦 Uncategorized"

                            Box {
                                OutlinedButton(
                                    onClick = { catDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkBg),
                                    border = BorderStroke(1.dp, BorderColor)
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

                            if (errorMsg != null) {
                                Text("• $errorMsg", fontSize = 10.sp, color = RoseExpense, fontWeight = FontWeight.Bold)
                            }

                            // Pic 2 - Level 2 Action Buttons (Cancel | Continue)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismissRequest,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BorderColor)
                                ) {
                                    Text("Cancel", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val num = amount.toDoubleOrNull()
                                        if (num == null || num <= 0) {
                                            errorField = "amount"
                                            errorMsg = "Please enter a valid amount"
                                            return@Button
                                        }

                                        // Save transaction immediately as outcome & UPI
                                        onSaveTransaction(
                                            num,
                                            "expense",
                                            receiverVendor.ifBlank { null },
                                            selectedCategoryId,
                                            "UPI",
                                            receiverVendor.ifBlank { null }
                                        )

                                        // Copy amount to clipboard for smooth payment paste
                                        try {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("AutoTrack Payment Amount", amount)
                                            clipboard.setPrimaryClip(clip)
                                        } catch (e: Exception) { }

                                        level = 3
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Text("Continue →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        } else if (level == 3) {
                            // Pic 2 - Level 3 (Pay & Log App Launcher Grid)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⚡ Select App to Complete Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                                    }
                                }

                                HorizontalDivider(color = BorderColor)

                                // Summary Card: Amount -> Notes -> Category
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                                    border = BorderStroke(1.dp, BorderColor),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Amount ->", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                            Text("₹$amount", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = RoseExpense)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Notes ->", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                            Text(receiverVendor.ifBlank { "N/A" }, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Category ->", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                            val catObj = categories.find { it.id == selectedCategoryId }
                                            Text(if (catObj != null) "${catObj.icon} ${catObj.name}" else "📦 Uncategorized", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Text(
                                    text = "Log saved! Amount copied to clipboard. Tap an app to launch:",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(160.dp)
                                ) {
                                    items(installedPaymentApps) { app ->
                                        Surface(
                                            onClick = {
                                                val pm = context.packageManager
                                                var launched = false

                                                // Attempt 1: Main Launcher Intent (Guaranteed for PhonePe, GPay, Navi, SuperMoney, etc.)
                                                if (app.packageName.isNotEmpty()) {
                                                    val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                                                    if (launchIntent != null) {
                                                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        try {
                                                            context.startActivity(launchIntent)
                                                            launched = true
                                                        } catch (e: Exception) { }
                                                    }
                                                }

                                                // Attempt 2: Direct upi://pay intent with package
                                                if (!launched && app.packageName.isNotEmpty()) {
                                                    try {
                                                        val upiUri = Uri.parse("upi://pay?am=$amount&tn=${Uri.encode(receiverVendor.ifBlank { "Payment" })}&cu=INR")
                                                        val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                                        intent.setPackage(app.packageName)
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        context.startActivity(intent)
                                                        launched = true
                                                    } catch (e: Exception) { }
                                                }

                                                // Attempt 3: Generic system UPI intent fallback
                                                if (!launched) {
                                                    try {
                                                        val upiUri = Uri.parse("upi://pay?am=$amount&tn=${Uri.encode(receiverVendor.ifBlank { "Payment" })}&cu=INR")
                                                        val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Could not open ${app.name}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                onDismissRequest()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = DarkBg,
                                            border = BorderStroke(1.dp, app.color.copy(alpha = 0.5f))
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(10.dp)
                                            ) {
                                                Text(app.iconEmoji, fontSize = 22.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = app.name,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    /* ========================================================================= */
                    /* MODE 3: UNDO RECENT TRANSACTION (Pic 3 - Level 2)                        */
                    /* ========================================================================= */
                    else if (selectedMode == "undo") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { level = 1 },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderColor),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(13.dp), tint = EmeraldPrimary)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Menu", fontSize = 10.sp, color = Color.White)
                            }

                            Text("↺ Undo Recent Transaction", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)

                            IconButton(onClick = onDismissRequest, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                            }
                        }

                        HorizontalDivider(color = BorderColor)

                        if (latestTransaction == null) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No recent transaction found to undo.", fontSize = 12.sp, color = TextMuted)
                            }
                        } else {
                            val tx = latestTransaction
                            val cat = categories.find { it.id == tx.categoryId }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${cat?.icon ?: "🏷️"} ${cat?.name ?: "Uncategorized"}",
                                                fontSize = 11.sp,
                                                color = TextMuted
                                            )
                                            Text(
                                                text = if (tx.type == "income") "+₹${tx.amount.toInt()}" else "-₹${tx.amount.toInt()}",
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (tx.type == "income") EmeraldPrimary else RoseExpense
                                            )
                                        }
                                        Text(
                                            text = tx.receiverVendor ?: tx.note ?: "Transaction #${tx.id.take(8)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Text(
                                    text = "Are you sure you want to delete and undo this transaction? This will permanently remove it from database and app totals.",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onDismissRequest,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, BorderColor)
                                    ) {
                                        Text("Cancel", fontSize = 11.sp, color = TextMuted)
                                    }

                                    Button(
                                        onClick = {
                                            if (onDeleteTransaction != null) {
                                                onDeleteTransaction(tx.id)
                                                Toast.makeText(context, "Transaction undone & deleted!", Toast.LENGTH_SHORT).show()
                                            }
                                            onDismissRequest()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RoseExpense),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Text("Delete 🗑️", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
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
