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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogoutRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf("overview") } // "overview", "ledger", "categories", "settings"
    var isBottomSheetOpen by remember { mutableStateOf(false) }

    var transactions by remember { mutableStateOf<List<TransactionItem>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var vaultCode by remember { mutableStateOf(DataSyncManager.getVaultCode() ?: "") }
    var isLoading by remember { mutableStateOf(false) }

    // Fetch transactions & categories from Supabase
    fun refreshData() {
        scope.launch(Dispatchers.IO) {
            val vault = DataSyncManager.getVaultCode() ?: return@launch
            val token = DataSyncManager.getSessionToken()
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
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
        // Auto-open manual log bottom sheet on Android launch
        isBottomSheetOpen = true
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
                    onClick = { activeTab = "ledger" },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "overview" -> OverviewScreen(transactions = transactions, categories = categories)
                "ledger" -> LedgerScreen(
                    transactions = transactions,
                    categories = categories,
                    onDeleteTransaction = { txId ->
                        scope.launch {
                            DataSyncManager.deleteTransaction(txId)
                            refreshData()
                        }
                    }
                )
                "categories" -> CategoriesScreen(categories = categories, transactions = transactions)
                "settings" -> SettingsScreen(
                    vaultCode = vaultCode,
                    onRotateCode = {
                        scope.launch {
                            val newCode = DataSyncManager.rotateVaultCode()
                            if (newCode != null) {
                                vaultCode = newCode
                                refreshData()
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
                    }
                )
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
                    val ok = DataSyncManager.saveTransaction(
                        context = context,
                        amount = amount,
                        type = type,
                        vendor = vendor,
                        categoryId = categoryId,
                        sourceApp = method,
                        note = note,
                        rawNotification = null
                    )
                    if (ok) {
                        Toast.makeText(context, "Transaction saved!", Toast.LENGTH_SHORT).show()
                        refreshData()
                    }
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
