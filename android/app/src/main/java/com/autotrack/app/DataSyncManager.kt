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
    private const val KEY_SERVICE_ENABLED = "service_enabled"
    private const val KEY_CASH_REMINDER_ENABLED = "cash_reminder_enabled"
    private const val KEY_CASH_REMINDER_TIME = "cash_reminder_time"

    private lateinit var prefs: SharedPreferences

    fun isServiceEnabled(): Boolean = prefs.getBoolean(KEY_SERVICE_ENABLED, true)

    fun setServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
    }

    fun getCashReminderEnabled(): Boolean = prefs.getBoolean(KEY_CASH_REMINDER_ENABLED, false)

    fun setCashReminderEnabled(context: Context, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CASH_REMINDER_ENABLED, enabled).apply()
        if (enabled) {
            val timeParts = getCashReminderTime().split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 21
            val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            DailyCashReminderReceiver.schedule(context, hour, minute)
        } else {
            DailyCashReminderReceiver.cancel(context)
        }
    }

    fun getCashReminderTime(): String = prefs.getString(KEY_CASH_REMINDER_TIME, "21:00") ?: "21:00"

    fun setCashReminderTime(context: Context, timeStr: String) {
        prefs.edit().putString(KEY_CASH_REMINDER_TIME, timeStr).apply()
        if (getCashReminderEnabled()) {
            val timeParts = timeStr.split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 21
            val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            DailyCashReminderReceiver.schedule(context, hour, minute)
        }
    }
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

        // Seed default allowed packages if empty
        if (!prefs.contains(KEY_ALLOWED_PACKAGES)) {
            val defaults = setOf(
                "com.google.android.apps.nbu.paisa.user", // Google Pay
                "com.phonepe.app",                       // PhonePe
                "net.one97.paytm",                       // Paytm
                "in.org.npci.upiapp",                    // BHIM
                "com.hdfcbank.payzapp",                  // HDFC
                "com.csam.icici.bank.imobile",          // ICICI
                "com.sbi.lotusintouch",                  // SBI
                "com.axis.mobile"                        // Axis
            )
            setAllowedPackages(defaults)
        }
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

    suspend fun createVault(): String? = withContext(Dispatchers.IO) {
        try {
            // Generate random 8-character uppercase code
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val sb = StringBuilder()
            for (i in 0 until 8) {
                sb.append(chars[(Math.random() * chars.length).toInt()])
            }
            val code = sb.toString()

            // Edge function URL or Supabase REST insert using anon/service key
            val url = "$SUPABASE_URL/functions/v1/create-vault"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .post("{}".toRequestBody(jsonMedia))
                .build()

            var generatedCode = code
            val response = client.newCall(req).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrEmpty()) {
                    val json = JSONObject(bodyStr)
                    generatedCode = json.optString("code", code)
                }
            }

            saveVaultCode(generatedCode)
            issueVaultSession(generatedCode)
            generatedCode
        } catch (e: Exception) {
            // Fallback: store locally generated code
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val sb = StringBuilder()
            for (i in 0 until 8) sb.append(chars[(Math.random() * chars.length).toInt()])
            val fallbackCode = sb.toString()
            saveVaultCode(fallbackCode)
            issueVaultSession(fallbackCode)
            fallbackCode
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
        val token = getSessionToken()

        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val occurredAt = isoFormat.format(Date())

        val payload = JSONObject().apply {
            put("vault_code", vault)
            put("title", if (vendor.isNullOrBlank()) (if (type == "income") "Income" else "Payment") else vendor)
            put("amount", amount)
            put("type", type)
            if (!categoryId.isNullOrBlank()) put("category_id", categoryId)
            if (!vendor.isNullOrBlank()) put("receiver_vendor", vendor)
            put("source_app", sourceApp)
            if (!note.isNullOrBlank()) put("note", note)
            if (!rawNotification.isNullOrBlank()) put("raw_notification", rawNotification)
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
            .addHeader("Prefer", "return=minimal")
            .post(payload.toString().toRequestBody(jsonMedia))

        if (!token.isNullOrEmpty()) {
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }

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
        val token = getSessionToken()

        CoroutineScope(Dispatchers.IO).launch {
            val pending = dbHelper.getPendingTransactions()
            for ((id, payload) in pending) {
                try {
                    val url = "$SUPABASE_URL/rest/v1/transactions"
                    val reqBuilder = Request.Builder()
                        .url(url)
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .post(payload.toString().toRequestBody(jsonMedia))

                    if (!token.isNullOrEmpty()) {
                        reqBuilder.addHeader("Authorization", "Bearer $token")
                    }

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
}
