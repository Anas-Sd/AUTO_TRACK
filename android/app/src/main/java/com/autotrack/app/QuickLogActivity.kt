package com.autotrack.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.autotrack.app.data.Category
import com.autotrack.app.ui.components.ManualLogBottomSheet
import com.autotrack.app.ui.theme.AutoTrackTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class QuickLogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataSyncManager.init(this)

        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContent {
            AutoTrackTheme {
                var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
                var latestTx by remember { mutableStateOf<com.autotrack.app.data.TransactionItem?>(null) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    val vault = DataSyncManager.getVaultCode()
                    if (vault.isNullOrEmpty()) {
                        Toast.makeText(this@QuickLogActivity, "Please set up Vault Code in AutoTrack first", Toast.LENGTH_LONG).show()
                        finish()
                        return@LaunchedEffect
                    }

                    scope.launch(Dispatchers.IO) {
                        val client = OkHttpClient()
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
                            // offline
                        }

                        val fetchedLatest = DataSyncManager.fetchLatestTransaction()

                        withContext(Dispatchers.Main) {
                            categories = newCats
                            latestTx = fetchedLatest
                        }
                    }
                }

                ManualLogBottomSheet(
                    onDismissRequest = { finish() },
                    categories = categories,
                    latestTransaction = latestTx,
                    onSaveTransaction = { amount, type, vendor, categoryId, method, note ->
                        scope.launch {
                            val ok = DataSyncManager.saveTransaction(
                                context = this@QuickLogActivity,
                                amount = amount,
                                type = type,
                                vendor = vendor,
                                categoryId = categoryId,
                                sourceApp = method,
                                note = note,
                                rawNotification = null
                            )
                            if (ok) {
                                Toast.makeText(this@QuickLogActivity, "Transaction saved!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onCreateCategory = { name, icon, cap ->
                        scope.launch {
                            DataSyncManager.createCategory(name, icon, "#10B981", cap)
                        }
                    },
                    onDeleteTransaction = { txId ->
                        scope.launch {
                            DataSyncManager.deleteTransaction(txId)
                            finish()
                        }
                    }
                )
            }
        }
    }
}
