package com.autotrack.app

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object DataSyncManager {

    private val _dataUpdateFlow = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val dataUpdateFlow: SharedFlow<Unit> = _dataUpdateFlow.asSharedFlow()

    fun notifyDataChanged() {
        _dataUpdateFlow.tryEmit(Unit)
    }

    // Target deployed URL / fallback Supabase endpoint
    var WEB_BASE_URL = "https://kdiefrqgmoahpfcstbzc.supabase.co"
    const val SUPABASE_URL = "https://kdiefrqgmoahpfcstbzc.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtkaWVmcnFnbW9haHBmY3N0YnpjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg1ODE0OTEsImV4cCI6MjEwNDE1NzQ5MX0.chEhUh4KKaTQGL4beE6ZSf6V65aiWBHvW3cLXMmmQhE"
    const val SUPABASE_SERVICE_ROLE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtkaWVmcnFnbW9haHBmY3N0YnpjIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4ODU4MTQ5MSwiZXhwIjoyMTA0MTU3NDkxfQ.jKXBvbUv9MwHGetQ4TU1AfRTJzJACCiccJiATJZgVRI"

    private const val PREFS_FILE = "autotrack_secure_prefs"
    private const val KEY_VAULT_CODE = "vault_code"
    private const val KEY_SESSION_TOKEN = "session_token"
    private const val KEY_ALLOWED_PACKAGES = "allowed_packages"
    private const val KEY_CACHED_CATEGORIES = "cached_categories"
    private const val KEY_PROFILE_NAME = "profile_name"
    private lateinit var prefs: SharedPreferences
    private lateinit var dbHelper: OfflineQueueDbHelper
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun init(context: Context) {
        if (::prefs.isInitialized && ::dbHelper.isInitialized) return
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            prefs = EncryptedSharedPreferences.create(
                PREFS_FILE,
                masterKeyAlias,
                context.applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            try {
                context.applicationContext.deleteSharedPreferences(PREFS_FILE)
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                prefs = EncryptedSharedPreferences.create(
                    PREFS_FILE,
                    masterKeyAlias,
                    context.applicationContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e2: Exception) {
                prefs = context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            }
        }
        try {
            dbHelper = OfflineQueueDbHelper(context.applicationContext)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun ensureInit() {
        if (!::prefs.isInitialized || !::dbHelper.isInitialized) {
            try {
                init(AutoTrackApp.instance)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun getProfileName(): String {
        ensureInit()
        return if (::prefs.isInitialized) prefs.getString(KEY_PROFILE_NAME, "User") ?: "User" else "User"
    }

    fun saveProfileName(name: String) {
        ensureInit()
        if (::prefs.isInitialized) prefs.edit().putString(KEY_PROFILE_NAME, name.trim()).apply()
    }

    fun getVaultCode(): String? {
        ensureInit()
        return if (::prefs.isInitialized) prefs.getString(KEY_VAULT_CODE, null) else null
    }

    fun saveVaultCode(code: String) {
        ensureInit()
        if (::prefs.isInitialized) prefs.edit().putString(KEY_VAULT_CODE, code.trim().uppercase(Locale.ROOT)).apply()
    }

    fun getSessionToken(): String? {
        ensureInit()
        return if (::prefs.isInitialized) prefs.getString(KEY_SESSION_TOKEN, null) else null
    }

    fun saveSessionToken(token: String) {
        ensureInit()
        if (::prefs.isInitialized) prefs.edit().putString(KEY_SESSION_TOKEN, token).apply()
    }

    fun clearVault() {
        ensureInit()
        if (::prefs.isInitialized) prefs.edit().remove(KEY_VAULT_CODE).remove(KEY_SESSION_TOKEN).apply()
    }

    fun getAllowedPackages(): Set<String> {
        return prefs.getStringSet(KEY_ALLOWED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun setAllowedPackages(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_ALLOWED_PACKAGES, packages).apply()
    }

    fun getCachedCategories(): List<Pair<String, String>> {
        val raw = prefs.getString(KEY_CACHED_CATEGORIES, null) ?: return listOf(
            Pair("", "📦 Uncategorized"),
            Pair("food", "🍔 Food & Dining"),
            Pair("shopping", "🛍️ Shopping"),
            Pair("bills", "⚡ Bills & Utilities"),
            Pair("transport", "🚗 Transportation"),
            Pair("entertainment", "🎬 Entertainment")
        )
        val list = mutableListOf<Pair<String, String>>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", "")
                val name = obj.optString("name", "Category")
                val icon = obj.optString("icon", "🏷️")
                list.add(Pair(id, "$icon $name"))
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }

    fun setCachedCategories(categoriesJson: String) {
        prefs.edit().putString(KEY_CACHED_CATEGORIES, categoriesJson).apply()
    }

    suspend fun createVault(label: String = "My Vault"): String? = withContext(Dispatchers.IO) {
        try {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val sb = StringBuilder()
            for (i in 0 until 8) {
                sb.append(chars[(Math.random() * chars.length).toInt()])
            }
            val code = sb.toString()
            val vaultLabel = if (label.trim().isNotEmpty()) label.trim() else "My Vault"

            val url = "$SUPABASE_URL/rest/v1/vault_codes"
            val payload = JSONObject().apply {
                put("code", code)
                put("label", vaultLabel)
            }
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()

            client.newCall(req).execute()

            seedDefaultCategories(code)

            saveVaultCode(code)
            issueVaultSession(code)
            code
        } catch (e: Exception) {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val sb = StringBuilder()
            for (i in 0 until 8) sb.append(chars[(Math.random() * chars.length).toInt()])
            val fallbackCode = sb.toString()
            saveVaultCode(fallbackCode)
            issueVaultSession(fallbackCode)
            fallbackCode
        }
    }

    private fun seedDefaultCategories(vaultCode: String) {
        try {
            val defaultCats = listOf(
                JSONObject().apply { put("vault_code", vaultCode); put("name", "Food & Dining"); put("icon", "🍔"); put("color", "#F59E0B"); put("monthly_cap", 10000) },
                JSONObject().apply { put("vault_code", vaultCode); put("name", "Shopping"); put("icon", "🛍️"); put("color", "#EC4899"); put("monthly_cap", 8000) },
                JSONObject().apply { put("vault_code", vaultCode); put("name", "Bills & Utilities"); put("icon", "⚡"); put("color", "#3B82F6"); put("monthly_cap", 5000) },
                JSONObject().apply { put("vault_code", vaultCode); put("name", "Transportation"); put("icon", "🚗"); put("color", "#10B981"); put("monthly_cap", 4000) },
                JSONObject().apply { put("vault_code", vaultCode); put("name", "Entertainment"); put("icon", "🎬"); put("color", "#8B5CF6"); put("monthly_cap", 3000) },
                JSONObject().apply { put("vault_code", vaultCode); put("name", "Health & Care"); put("icon", "💊"); put("color", "#EF4444"); put("monthly_cap", 5000) },
                JSONObject().apply { put("vault_code", vaultCode); put("name", "Salary & Income"); put("icon", "💰"); put("color", "#10B981") },
                JSONObject().apply { put("vault_code", vaultCode); put("name", "Investments"); put("icon", "📈"); put("color", "#06B6D4") }
            )

            val array = JSONArray()
            defaultCats.forEach { array.put(it) }

            val url = "$SUPABASE_URL/rest/v1/categories"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                .addHeader("Content-Type", "application/json")
                .post(array.toString().toRequestBody(jsonMedia))
                .build()

            client.newCall(req).execute()
        } catch (e: Exception) {
            // ignore
        }
    }

    suspend fun issueVaultSession(code: String): String? = withContext(Dispatchers.IO) {
        val cleanCode = code.trim().uppercase(Locale.ROOT)
        if (cleanCode.isEmpty()) return@withContext null
        try {
            val url = "$SUPABASE_URL/rest/v1/vault_codes?code=eq.$cleanCode&select=code,label"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                .get()
                .build()

            val response = client.newCall(req).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: "[]"
                val array = JSONArray(bodyStr)
                if (array.length() > 0) {
                    updateLastAccessed(cleanCode)
                    saveVaultCode(cleanCode)
                    saveSessionToken("vault_token_$cleanCode")
                    fetchCategories()
                    return@withContext cleanCode
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    private fun updateLastAccessed(code: String) {
        try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val url = "$SUPABASE_URL/rest/v1/vault_codes?code=eq.$code"
            val payload = JSONObject().apply {
                put("last_accessed", isoFormat.format(Date()))
            }
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                .addHeader("Content-Type", "application/json")
                .patch(payload.toString().toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute()
        } catch (e: Exception) {
            // ignore
        }
    }

    suspend fun fetchCategories(): Unit = withContext(Dispatchers.IO) {
        val vault = getVaultCode() ?: return@withContext
        val url = "$SUPABASE_URL/rest/v1/categories?vault_code=eq.$vault&select=id,name,icon,color,monthly_cap"
        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
            .get()

        try {
            val res = client.newCall(reqBuilder.build()).execute()
            if (res.isSuccessful) {
                val body = res.body?.string() ?: "[]"
                setCachedCategories(body)
            }
        } catch (e: Exception) {
            // ignore network failure
        }
    }

    enum class SaveResult {
        SAVED_ONLINE,
        QUEUED_OFFLINE,
        FAILED
    }

    fun getPendingOfflineQueue(): List<Pair<Long, JSONObject>> {
        return if (::dbHelper.isInitialized) dbHelper.getPendingTransactions() else emptyList()
    }

    suspend fun flushOfflineQueueSync(context: Context): Int = withContext(Dispatchers.IO) {
        if (!isOnline(context)) return@withContext 0
        if (!::dbHelper.isInitialized) return@withContext 0
        val pending = dbHelper.getPendingTransactions()
        var syncedCount = 0
        for ((id, payload) in pending) {
            try {
                val url = "$SUPABASE_URL/rest/v1/transactions"
                val reqBuilder = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(payload.toString().toRequestBody(jsonMedia))

                val res = client.newCall(reqBuilder.build()).execute()
                if (res.isSuccessful) {
                    dbHelper.removeTransaction(id)
                    syncedCount++
                }
            } catch (e: Exception) {
                break
            }
        }
        syncedCount
    }

    suspend fun saveTransactionWithStatus(
        context: Context,
        amount: Double,
        type: String,
        vendor: String?,
        categoryId: String?,
        sourceApp: String,
        note: String?,
        rawNotification: String?
    ): SaveResult = withContext(Dispatchers.IO) {
        val vault = getVaultCode() ?: return@withContext SaveResult.FAILED

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val occurredAt = isoFormat.format(Date())

        val payload = JSONObject().apply {
            put("vault_code", vault)
            put("amount", amount)
            put("type", type)
            if (!categoryId.isNullOrBlank()) put("category_id", categoryId)
            if (!vendor.isNullOrBlank()) put("receiver_vendor", vendor)
            put("source_app", sourceApp)
            if (!note.isNullOrBlank()) put("note", note)
            put("occurred_at", occurredAt)
        }

        // If offline, enqueue immediately
        if (!isOnline(context)) {
            dbHelper.enqueueTransaction(payload)
            return@withContext SaveResult.QUEUED_OFFLINE
        }

        val url = "$SUPABASE_URL/rest/v1/transactions"
        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(payload.toString().toRequestBody(jsonMedia))

        try {
            val res = client.newCall(reqBuilder.build()).execute()
            if (res.isSuccessful) {
                flushOfflineQueue(context)
                return@withContext SaveResult.SAVED_ONLINE
            } else {
                dbHelper.enqueueTransaction(payload)
                return@withContext SaveResult.QUEUED_OFFLINE
            }
        } catch (e: Exception) {
            dbHelper.enqueueTransaction(payload)
            return@withContext SaveResult.QUEUED_OFFLINE
        }
    }

    suspend fun saveTransaction(
        context: Context,
        amount: Double,
        type: String,
        vendor: String?,
        categoryId: String?,
        sourceApp: String,
        note: String?,
        rawNotification: String?
    ): Boolean {
        val res = saveTransactionWithStatus(context, amount, type, vendor, categoryId, sourceApp, note, rawNotification)
        if (res != SaveResult.FAILED) {
            notifyDataChanged()
        }
        return res != SaveResult.FAILED
    }

    suspend fun fetchLatestTransaction(): com.autotrack.app.data.TransactionItem? = withContext(Dispatchers.IO) {
        val vault = getVaultCode() ?: return@withContext null
        try {
            val url = "$SUPABASE_URL/rest/v1/transactions?vault_code=eq.$vault&order=occurred_at.desc&limit=1"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                .get()
                .build()

            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                val body = res.body?.string() ?: "[]"
                val array = JSONArray(body)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    return@withContext com.autotrack.app.data.TransactionItem(
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
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun deleteTransaction(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/transactions?id=eq.$id"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                .delete()
                .build()

            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                notifyDataChanged()
            }
            return@withContext res.isSuccessful
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun updateTransaction(
        id: String,
        amount: Double,
        type: String,
        vendor: String?,
        categoryId: String?,
        sourceApp: String,
        note: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/rest/v1/transactions?id=eq.$id"
            val payload = JSONObject().apply {
                put("amount", amount)
                put("type", type)
                put("category_id", categoryId ?: JSONObject.NULL)
                put("receiver_vendor", vendor ?: JSONObject.NULL)
                put("source_app", sourceApp)
                put("note", note ?: JSONObject.NULL)
            }
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(payload.toString().toRequestBody(jsonMedia))
                .build()

            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                notifyDataChanged()
            }
            return@withContext res.isSuccessful
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun createCategory(name: String, icon: String, color: String = "#10B981", monthlyCap: Double? = null): Boolean = withContext(Dispatchers.IO) {
        val vault = getVaultCode() ?: return@withContext false
        try {
            val url = "$SUPABASE_URL/rest/v1/categories"
            val payload = JSONObject().apply {
                put("vault_code", vault)
                put("name", name)
                put("icon", icon)
                put("color", color)
                if (monthlyCap != null) put("monthly_cap", monthlyCap)
            }
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()

            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                notifyDataChanged()
            }
            return@withContext res.isSuccessful
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun updateCategory(
        id: String,
        name: String,
        icon: String,
        color: String = "#10B981",
        monthlyCap: Double? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val vault = getVaultCode() ?: return@withContext false
        try {
            val encodedId = java.net.URLEncoder.encode(id, "UTF-8")
            val url = "$SUPABASE_URL/rest/v1/categories?vault_code=eq.$vault&id=eq.$encodedId"
            val payload = JSONObject().apply {
                put("name", name)
                put("icon", icon)
                put("color", color)
                put("monthly_cap", monthlyCap ?: JSONObject.NULL)
            }
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(payload.toString().toRequestBody(jsonMedia))
                .build()

            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                fetchCategories()
                notifyDataChanged()
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            fetchCategories()
            notifyDataChanged()
        } catch (e: Exception) { }
        return@withContext true
    }

    suspend fun deleteCategory(id: String): Boolean = withContext(Dispatchers.IO) {
        val vault = getVaultCode() ?: return@withContext false
        val encodedId = try { java.net.URLEncoder.encode(id, "UTF-8") } catch (e: Exception) { id }
        try {
            // Step 1: Data consistency — update all transactions belonging to this category to category_id = NULL (Uncategorized)
            val patchTxUrl = "$SUPABASE_URL/rest/v1/transactions?vault_code=eq.$vault&category_id=eq.$encodedId"
            val patchTxPayload = JSONObject().apply {
                put("category_id", JSONObject.NULL)
            }
            val patchTxReq = Request.Builder()
                .url(patchTxUrl)
                .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(patchTxPayload.toString().toRequestBody(jsonMedia))
                .build()

            client.newCall(patchTxReq).execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            // Step 2: Delete category from categories table
            val url = "$SUPABASE_URL/rest/v1/categories?vault_code=eq.$vault&id=eq.$encodedId"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                .delete()
                .build()

            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                fetchCategories()
                notifyDataChanged()
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            fetchCategories()
            notifyDataChanged()
        } catch (e: Exception) { }
        return@withContext true
    }

    fun flushOfflineQueue(context: Context) {
        if (!isOnline(context)) return

        CoroutineScope(Dispatchers.IO).launch {
            flushOfflineQueueSync(context)
        }
    }

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun rotateVaultCode(targetCode: String? = null): String? = withContext(Dispatchers.IO) {
        val oldVault = getVaultCode() ?: return@withContext null

        val newVault = if (!targetCode.isNullOrBlank()) {
            targetCode.trim().uppercase(Locale.ROOT)
        } else {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val sb = StringBuilder()
            for (i in 0 until 8) {
                sb.append(chars[(Math.random() * chars.length).toInt()])
            }
            sb.toString()
        }

        try {
            // 1. Create new vault code entry in Supabase database
            val vaultUrl = "$SUPABASE_URL/rest/v1/vault_codes"
            val vaultPayload = JSONObject().apply {
                put("code", newVault)
                put("label", "My Vault")
            }
            val vaultReq = Request.Builder()
                .url(vaultUrl)
                .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(vaultPayload.toString().toRequestBody(jsonMedia))
                .build()

            val vaultRes = client.newCall(vaultReq).execute()
            if (!vaultRes.isSuccessful && vaultRes.code != 409) {
                return@withContext null
            }

            // 2. Re-link all transactions in database from oldVault -> newVault
            val txUrl = "$SUPABASE_URL/rest/v1/transactions?vault_code=eq.$oldVault"
            val txPayload = JSONObject().apply {
                put("vault_code", newVault)
            }
            val txReq = Request.Builder()
                .url(txUrl)
                .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(txPayload.toString().toRequestBody(jsonMedia))
                .build()

            client.newCall(txReq).execute()

            // 3. Re-link all categories in database from oldVault -> newVault
            val catUrl = "$SUPABASE_URL/rest/v1/categories?vault_code=eq.$oldVault"
            val catPayload = JSONObject().apply {
                put("vault_code", newVault)
            }
            val catReq = Request.Builder()
                .url(catUrl)
                .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(catPayload.toString().toRequestBody(jsonMedia))
                .build()

            client.newCall(catReq).execute()

            // 4. Remove old vault code entry
            val delUrl = "$SUPABASE_URL/rest/v1/vault_codes?code=eq.$oldVault"
            val delReq = Request.Builder()
                .url(delUrl)
                .addHeader("apikey", SUPABASE_SERVICE_ROLE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_SERVICE_ROLE_KEY")
                .delete()
                .build()

            client.newCall(delReq).execute()

            // 5. Update local state & session
            saveVaultCode(newVault)
            issueVaultSession(newVault)
            newVault
        } catch (e: Exception) {
            null
        }
    }

    suspend fun wipeVaultData(): Boolean = withContext(Dispatchers.IO) {
        val vault = getVaultCode() ?: return@withContext false
        val token = getSessionToken()
        val url = "$SUPABASE_URL/rest/v1/transactions?vault_code=eq.$vault"
        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .delete()
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }
        try {
            val res = client.newCall(reqBuilder.build()).execute()
            return@withContext res.isSuccessful
        } catch (e: Exception) {
            return@withContext false
        }
    }
}
