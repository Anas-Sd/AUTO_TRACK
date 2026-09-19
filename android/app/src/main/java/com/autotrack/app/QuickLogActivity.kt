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

class QuickLogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataSyncManager.init(this)

        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContent {
            AutoTrackTheme {
                var categories by remember { mutableStateOf<List<Category>>(DataSyncManager.getCachedCategoryObjects()) }
                var latestTx by remember { mutableStateOf<com.autotrack.app.data.TransactionItem?>(null) }
                val scope = rememberCoroutineScope()

                suspend fun refreshShortcutCategoriesAndData() {
                    val catList = DataSyncManager.fetchCategoryObjects()
                    val fetchedLatest = DataSyncManager.fetchLatestTransaction()

                    withContext(Dispatchers.Main) {
                        categories = catList
                        latestTx = fetchedLatest
                    }
                }

                LaunchedEffect(Unit) {
                    val vault = DataSyncManager.getVaultCode()
                    if (vault.isNullOrEmpty()) {
                        Toast.makeText(this@QuickLogActivity, "Please set up Vault Code in AutoTrack first", Toast.LENGTH_LONG).show()
                        finish()
                        return@LaunchedEffect
                    }

                    refreshShortcutCategoriesAndData()

                    DataSyncManager.dataUpdateFlow.collect {
                        val updatedCats = DataSyncManager.getCachedCategoryObjects()
                        val fetchedLatest = DataSyncManager.fetchLatestTransaction()
                        withContext(Dispatchers.Main) {
                            if (updatedCats.isNotEmpty()) {
                                categories = updatedCats
                            }
                            latestTx = fetchedLatest
                        }
                    }
                }

                ManualLogBottomSheet(
                    onDismissRequest = { finish() },
                    categories = categories,
                    latestTransaction = latestTx,
                    onSaveTransaction = { amount, type, vendor, categoryId, method, note, occurredAt ->
                        scope.launch {
                            val saveRes = DataSyncManager.saveTransactionWithStatus(
                                context = applicationContext,
                                amount = amount,
                                type = type,
                                vendor = vendor,
                                categoryId = categoryId,
                                sourceApp = method,
                                note = note,
                                rawNotification = null,
                                customOccurredAt = occurredAt
                            )
                            withContext(Dispatchers.Main) {
                                if (saveRes == DataSyncManager.SaveResult.SAVED_ONLINE) {
                                    Toast.makeText(applicationContext, "Transaction saved!", Toast.LENGTH_SHORT).show()
                                } else if (saveRes == DataSyncManager.SaveResult.QUEUED_OFFLINE) {
                                    Toast.makeText(applicationContext, "Transaction queued (offline)", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(applicationContext, "Failed to save transaction", Toast.LENGTH_SHORT).show()
                                }
                            }
                            DataSyncManager.notifyDataChanged()
                        }
                    },
                    onCreateCategory = { name, icon, cap ->
                        scope.launch {
                            val newId = DataSyncManager.createCategoryAndGetId(name, icon, "#10B981", cap)
                            withContext(Dispatchers.Main) {
                                if (newId != null) {
                                    Toast.makeText(applicationContext, "Category created successfully!", Toast.LENGTH_SHORT).show()
                                }
                                categories = DataSyncManager.getCachedCategoryObjects()
                            }
                        }
                    },
                    onDeleteTransaction = { txId ->
                        scope.launch {
                            val ok = DataSyncManager.deleteTransaction(txId)
                            withContext(Dispatchers.Main) {
                                if (ok) {
                                    Toast.makeText(applicationContext, "Transaction undone & deleted!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            DataSyncManager.notifyDataChanged()
                            finish()
                        }
                    }
                )
            }
        }
    }
}
