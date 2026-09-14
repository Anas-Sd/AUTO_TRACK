package com.autotrack.app

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    // Target deployed URL / fallback Supabase endpoint
    var WEB_BASE_URL = "https://kdiefrqgmoahpfcstbzc.supabase.co"
    const val SUPABASE_URL = "https://kdiefrqgmoahpfcstbzc.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtkaWVmcnFnbW9haHBmY3N0YnpjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg1ODE0OTEsImV4cCI6MjEwNDE1NzQ5MX0.chEhUh4KKaTQGL4beE6ZSf6V65aiWBHvW3cLXMmmQhE"

    private const val PREFS_FILE = "autotrack_secure_prefs"
    private const val KEY_VAULT_CODE = "vault_code"
    private const val KEY_SESSION_TOKEN = "session_token"
    private const val KEY_ALLOWED_PACKAGES = "allowed_packages"
    private const val KEY_CACHED_CATEGORIES = "cached_categories"
    private lateinit var prefs: SharedPreferences
    private lateinit var dbHelper: OfflineQueueDbHelper
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun init(context: Context) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            PREFS_FILE,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        dbHelper = OfflineQueueDbHelper(context)
    }

    fun getVaultCode(): String? = prefs.getString(KEY_VAULT_CODE, null)

    fun saveVaultCode(code: String) {
        prefs.edit().putString(KEY_VAULT_CODE, code.trim().toUpperCase(Locale.ROOT)).apply()
    }

    fun getSessionToken(): String? = prefs.getString(KEY_SESSION_TOKEN, null)

    fun saveSessionToken(token: String) {
        prefs.edit().putString(KEY_SESSION_TOKEN, token).apply()
    }

    fun clearVault() {
        prefs.edit().remove(KEY_VAULT_CODE).remove(KEY_SESSION_TOKEN).apply()
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
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
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
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .addHeader("Content-Type", "application/json")
                .post(array.toString().toRequestBody(jsonMedia))
                .build()

            client.newCall(req).execute()
        } catch (e: Exception) {
            // ignore
        }
    }

    suspend fun issueVaultSession(code: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$SUPABASE_URL/functions/v1/issue-vault-session"
            val payload = JSONObject().apply { put("code", code.trim().toUpperCase(Locale.ROOT)) }
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()

            val response = client.newCall(req).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val token = json.optString("token", "")
                if (token.isNotEmpty()) {
                    saveSessionToken(token)
                    fetchCategories()
                    return@withContext token
                }
            }
        } catch (e: Exception) {
            // log error
        }
        null
    }

    suspend fun fetchCategories(): Unit = withContext(Dispatchers.IO) {
        val vault = getVaultCode() ?: return@withContext
        val token = getSessionToken()
        val url = "$SUPABASE_URL/rest/v1/categories?vault_code=eq.$vault&select=id,name,icon,color,monthly_cap"
        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .get()

        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

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

    suspend fun saveTransaction(
        context: Context,
        amount: Double,
        type: String,
        vendor: String?,
        categoryId: String?,
        sourceApp: String,
        note: String?,
        rawNotification: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val vault = getVaultCode() ?: return@withContext false

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
            return@withContext true
        }

        val url = "$SUPABASE_URL/rest/v1/transactions"
        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(payload.toString().toRequestBody(jsonMedia))

        try {
            val res = client.newCall(reqBuilder.build()).execute()
            if (res.isSuccessful) {
                // Also trigger flush if there are pending offline transactions
                flushOfflineQueue(context)
                return@withContext true
            } else {
                // If rejected by network/auth, queue for retry
                dbHelper.enqueueTransaction(payload)
                return@withContext true
            }
        } catch (e: Exception) {
            dbHelper.enqueueTransaction(payload)
            return@withContext true
        }
    }

    fun flushOfflineQueue(context: Context) {
        if (!isOnline(context)) return

        CoroutineScope(Dispatchers.IO).launch {
            val pending = dbHelper.getPendingTransactions()
            for ((id, payload) in pending) {
                try {
                    val url = "$SUPABASE_URL/rest/v1/transactions"
                    val reqBuilder = Request.Builder()
                        .url(url)
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=minimal")
                        .post(payload.toString().toRequestBody(jsonMedia))

                    val res = client.newCall(reqBuilder.build()).execute()
                    if (res.isSuccessful) {
                        dbHelper.removeTransaction(id)
                    }
                } catch (e: Exception) {
                    break // Stop on connection drop
                }
            }
        }
    }

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun rotateVaultCode(): String? = withContext(Dispatchers.IO) {
        val vault = getVaultCode() ?: return@withContext null
        val token = getSessionToken()
        val url = "$SUPABASE_URL/functions/v1/vault/rotate"
        val reqBuilder = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .post("{}".toRequestBody(jsonMedia))
        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }
        try {
            val res = client.newCall(reqBuilder.build()).execute()
            if (res.isSuccessful) {
                val json = JSONObject(res.body?.string() ?: "{}")
                val newCode = json.optString("new_code", "")
                if (newCode.isNotEmpty()) {
                    saveVaultCode(newCode)
                    return@withContext newCode
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        null
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
