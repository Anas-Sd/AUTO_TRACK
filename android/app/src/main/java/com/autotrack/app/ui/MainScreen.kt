package com.autotrack.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autotrack.app.DataSyncManager
import com.autotrack.app.data.Category
import com.autotrack.app.data.TransactionItem
import com.autotrack.app.ui.components.ManualLogBottomSheet
import com.autotrack.app.ui.screens.*
import com.autotrack.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogoutRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf("overview") } // "overview", "ledger", "categories", "settings"
    var isBottomSheetOpen by remember { mutableStateOf(false) }
    var selectedLedgerCategoryFilter by remember { mutableStateOf<String?>(null) }

    var transactions by remember { mutableStateOf<List<TransactionItem>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var vaultCode by remember { mutableStateOf(DataSyncManager.getVaultCode() ?: "") }
    var profileName by remember { mutableStateOf(DataSyncManager.getProfileName() ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Fetch transactions & categories from Supabase
    fun refreshData(isManualSwipe: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            if (isManualSwipe) {
                withContext(Dispatchers.Main) { isRefreshing = true }
            }

            if (DataSyncManager.isOnline(context)) {
                val flushed = DataSyncManager.flushOfflineQueueSync(context)
                if (flushed > 0) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Synced $flushed queued offline transaction(s)!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            val vault = DataSyncManager.getVaultCode() ?: return@launch
            val client = OkHttpClient()

            // Fetch Categories
            val catUrl = "${DataSyncManager.SUPABASE_URL}/rest/v1/categories?vault_code=eq.$vault&select=id,name,icon,color,monthly_cap"
            val catReq = Request.Builder()
                .url(catUrl)
                .addHeader("apikey", DataSyncManager.SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer ${DataSyncManager.SUPABASE_SERVICE_ROLE_KEY}")
                .get()
                .build()

            val newCats = mutableListOf<Category>()
            try {
                val res = client.newCall(catReq).execute()
                if (res.isSuccessful) {
                    val body = res.body?.string() ?: "[]"
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        newCats.add(
                            Category(
                                id = obj.optString("id", ""),
                                name = obj.optString("name", "Category"),
                                icon = obj.optString("icon", "🏷️"),
                                color = obj.optString("color", "#10B981"),
                                monthlyCap = if (obj.has("monthly_cap") && !obj.isNull("monthly_cap")) obj.optDouble("monthly_cap") else null
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // handle offline
            }

            // Fetch Transactions
            val txUrl = "${DataSyncManager.SUPABASE_URL}/rest/v1/transactions?vault_code=eq.$vault&select=id,vault_code,amount,type,receiver_vendor,category_id,source_app,note,occurred_at&order=occurred_at.desc"
            val txReq = Request.Builder()
                .url(txUrl)
                .addHeader("apikey", DataSyncManager.SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer ${DataSyncManager.SUPABASE_SERVICE_ROLE_KEY}")
                .get()
                .build()

            val newTxs = mutableListOf<TransactionItem>()
            try {
                val res = client.newCall(txReq).execute()
                if (res.isSuccessful) {
                    val body = res.body?.string() ?: "[]"
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        newTxs.add(
                            TransactionItem(
                                id = obj.optString("id", ""),
                                vaultCode = obj.optString("vault_code", ""),
                                amount = obj.optDouble("amount", 0.0),
                                type = obj.optString("type", "expense"),
                                receiverVendor = if (obj.has("receiver_vendor") && !obj.isNull("receiver_vendor")) obj.optString("receiver_vendor") else null,
                                categoryId = if (obj.has("category_id") && !obj.isNull("category_id")) obj.optString("category_id") else null,
                                sourceApp = obj.optString("source_app", "UPI"),
                                note = if (obj.has("note") && !obj.isNull("note")) obj.optString("note") else null,
                                occurredAt = obj.optString("occurred_at", "")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // handle offline
            }

            withContext(Dispatchers.Main) {
                categories = newCats
                transactions = newTxs
                isRefreshing = false
                if (isManualSwipe) {
                    Toast.makeText(context, "Data refreshed!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Scaffold(
        containerColor = DarkBg,
        bottomBar = {
            NavigationBar(
                containerColor = CardBg,
                contentColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.border(1.dp, BorderColor)
            ) {
                NavigationBarItem(
                    selected = activeTab == "overview",
                    onClick = { activeTab = "overview" },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Overview") },
                    label = { Text("Overview", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = DarkBg,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )

                NavigationBarItem(
                    selected = activeTab == "ledger",
                    onClick = {
                        activeTab = "ledger"
                        selectedLedgerCategoryFilter = null
                    },
                    icon = { Icon(Icons.Default.Receipt, contentDescription = "Ledger") },
                    label = { Text("Ledger", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = DarkBg,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )

                NavigationBarItem(
                    selected = activeTab == "categories",
                    onClick = { activeTab = "categories" },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Categories") },
                    label = { Text("Categories", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = DarkBg,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )

                NavigationBarItem(
                    selected = activeTab == "settings",
                    onClick = { activeTab = "settings" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = DarkBg,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isBottomSheetOpen = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Transaction", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        var pullOffset by remember { mutableStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "overview" -> OverviewScreen(transactions = transactions, categories = categories, userName = profileName)
                "ledger" -> LedgerScreen(
                    transactions = transactions,
                    categories = categories,
                    initialCategoryFilter = selectedLedgerCategoryFilter,
                    onDeleteTransaction = { txId ->
                        scope.launch {
                            DataSyncManager.deleteTransaction(txId)
                            refreshData()
                        }
                    },
                    onEditTransaction = { id, amount, type, vendor, categoryId, sourceApp, note ->
                        scope.launch {
                            val ok = DataSyncManager.updateTransaction(id, amount, type, vendor, categoryId, sourceApp, note)
                            if (ok) {
                                Toast.makeText(context, "Transaction updated!", Toast.LENGTH_SHORT).show()
                                refreshData()
                            }
                        }
                    }
                )
                "categories" -> CategoriesScreen(
                    categories = categories,
                    transactions = transactions,
                    onSelectCategory = { catId ->
                        selectedLedgerCategoryFilter = catId
                        activeTab = "ledger"
                    },
                    onCreateCategory = { name, icon, cap ->
                        scope.launch {
                            val ok = DataSyncManager.createCategory(name, icon, "#10B981", cap)
                            if (ok) {
                                Toast.makeText(context, "Category created!", Toast.LENGTH_SHORT).show()
                                refreshData()
                            }
                        }
                    },
                    onUpdateCategory = { id, name, icon, cap ->
                        scope.launch {
                            val ok = DataSyncManager.updateCategory(id, name, icon, "#10B981", cap)
                            if (ok) {
                                Toast.makeText(context, "Category updated!", Toast.LENGTH_SHORT).show()
                                refreshData()
                            }
                        }
                    },
                    onDeleteCategory = { id ->
                        scope.launch {
                            val ok = DataSyncManager.deleteCategory(id)
                            if (ok) {
                                Toast.makeText(context, "Category deleted!", Toast.LENGTH_SHORT).show()
                                refreshData()
                            }
                        }
                    }
                )
                "settings" -> SettingsScreen(
                    vaultCode = vaultCode,
                    profileName = profileName,
                    onSaveProfileName = { newName ->
                        DataSyncManager.saveProfileName(newName)
                        profileName = newName
                    },
                    onRotateCode = { targetCode ->
                        scope.launch {
                            val newCode = DataSyncManager.rotateVaultCode(targetCode)
                            if (newCode != null) {
                                vaultCode = newCode
                                Toast.makeText(context, "Vault Code rotated to: $newCode. Please log in again.", Toast.LENGTH_LONG).show()
                                DataSyncManager.clearVault()
                                onLogoutRequest()
                            } else {
                                Toast.makeText(context, "Failed to rotate Vault Code", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onWipeData = {
                        scope.launch {
                            DataSyncManager.wipeVaultData()
                            refreshData()
                        }
                    },
                    onLogout = {
                        DataSyncManager.clearVault()
                        onLogoutRequest()
                    },
                    onSyncQueue = {
                        scope.launch {
                            val synced = DataSyncManager.flushOfflineQueueSync(context)
                            if (synced > 0) {
                                Toast.makeText(context, "Synced $synced offline transaction(s)!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No offline items to sync or currently offline", Toast.LENGTH_SHORT).show()
                            }
                            refreshData()
                        }
                    }
                )
            }

            if (isRefreshing || pullOffset > 20f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = CardBg,
                        shape = CircleShape,
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, EmeraldPrimary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = EmeraldPrimary,
                                strokeWidth = 2.dp
                            )
                            Text("Refreshing...", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Native 2-Level Manual Log Dialog (Top-Anchored Modal Popup)
    if (isBottomSheetOpen) {
        ManualLogBottomSheet(
            onDismissRequest = { isBottomSheetOpen = false },
            categories = categories,
            onSaveTransaction = { amount, type, vendor, categoryId, method, note ->
                scope.launch {
                    val saveRes = DataSyncManager.saveTransactionWithStatus(
                        context = context,
                        amount = amount,
                        type = type,
                        vendor = vendor,
                        categoryId = categoryId,
                        sourceApp = method,
                        note = note,
                        rawNotification = null
                    )
                    if (saveRes == DataSyncManager.SaveResult.SAVED_ONLINE) {
                        Toast.makeText(context, "Transaction saved!", Toast.LENGTH_SHORT).show()
                    } else if (saveRes == DataSyncManager.SaveResult.QUEUED_OFFLINE) {
                        Toast.makeText(context, "Transaction queued (offline)", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to save transaction", Toast.LENGTH_SHORT).show()
                    }
                    refreshData()
                }
            },
            onCreateCategory = { name, icon, cap ->
                scope.launch {
                    DataSyncManager.createCategory(name, icon, "#10B981", cap)
                    refreshData()
                }
            }
        )
    }
}
