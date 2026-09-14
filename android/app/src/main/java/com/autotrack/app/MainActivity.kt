package com.autotrack.app

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.autotrack.app.databinding.ActivityMainBinding
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Deployed web app URL or local development server
    private var webAppUrl = "https://autotrackx.vercel.app"

    private var isPendingLogCash = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Check if vault code exists. If first launch, show Onboarding
        val vaultCode = DataSyncManager.getVaultCode()
        if (vaultCode.isNullOrEmpty()) {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // 2. Start Foreground Watcher Service
        ForegroundWatchService.start(this)

        // 3. Setup WebView & Android Javascript Bridge
        setupWebView(vaultCode)

        binding.btnRetry.setOnClickListener {
            if (DataSyncManager.isOnline(this)) {
                binding.errorView.visibility = View.GONE
                DataSyncManager.flushOfflineQueue(this)
                binding.webView.reload()
            } else {
                Toast.makeText(this, "Still offline. Auto-tracking is running locally.", Toast.LENGTH_SHORT).show()
            }
        }

        handleCashLogIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCashLogIntent(intent)
    }

    private fun handleCashLogIntent(intent: Intent?) {
        if (intent?.action == "ACTION_LOG_CASH") {
            isPendingLogCash = true
            triggerLogCashModal()
        }
    }

    private fun triggerLogCashModal() {
        if (isPendingLogCash) {
            binding.webView.evaluateJavascript(
                "if (window.openManualLogModal) { window.openManualLogModal('Cash'); }",
                null
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(vaultCode: String) {
        val webView = binding.webView
        val settings = webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.cacheMode = if (DataSyncManager.isOnline(this)) {
            WebSettings.LOAD_DEFAULT
        } else {
            WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
        settings.allowFileAccess = false
        settings.allowContentAccess = false

        // Attach native Javascript Bridge
        webView.addJavascriptInterface(AndroidBridge(this), "AndroidBridge")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.progress = newProgress
                } else {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.errorView.visibility = View.GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                triggerLogCashModal()
                if (DataSyncManager.isOnline(this@MainActivity)) {
                    DataSyncManager.flushOfflineQueue(this@MainActivity)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true && !DataSyncManager.isOnline(this@MainActivity)) {
                    binding.errorView.visibility = View.VISIBLE
                }
            }
        }

        // Auto-login URL automatically signs in the WebView with httpOnly session cookie
        val targetUrl = "$webAppUrl/auto-login?code=$vaultCode"
        webView.loadUrl(targetUrl)
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    // Javascript Interface exposed to Web App Settings Tab
    inner class AndroidBridge(private val context: Context) {

        @JavascriptInterface
        fun isAndroidApp(): Boolean = true

        @JavascriptInterface
        fun getServiceEnabled(): Boolean {
            return DataSyncManager.isServiceEnabled()
        }

        @JavascriptInterface
        fun setServiceEnabled(enabled: Boolean) {
            DataSyncManager.setServiceEnabled(enabled)
        }

        @JavascriptInterface
        fun getCashReminderEnabled(): Boolean {
            return DataSyncManager.getCashReminderEnabled()
        }

        @JavascriptInterface
        fun setCashReminderEnabled(enabled: Boolean) {
            DataSyncManager.setCashReminderEnabled(context, enabled)
        }

        @JavascriptInterface
        fun getCashReminderTime(): String {
            return DataSyncManager.getCashReminderTime()
        }

        @JavascriptInterface
        fun setCashReminderTime(timeStr: String) {
            DataSyncManager.setCashReminderTime(context, timeStr)
        }

        @JavascriptInterface
        fun getVaultCode(): String {
            return DataSyncManager.getVaultCode() ?: ""
        }

        @JavascriptInterface
        fun setVaultCode(code: String) {
            DataSyncManager.saveVaultCode(code)
        }

        @JavascriptInterface
        fun openAppSettings() {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        @JavascriptInterface
        fun getNotificationPermission(): Boolean {
            val packages = NotificationManagerCompat.getEnabledListenerPackages(context)
            return packages.contains(context.packageName)
        }

        @JavascriptInterface
        fun requestNotificationPermission() {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        @JavascriptInterface
        fun getOverlayPermission(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        @JavascriptInterface
        fun requestOverlayPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }

        @JavascriptInterface
        fun getBatteryOptimizationStatus(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                pm.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }
        }

        @JavascriptInterface
        fun requestBatteryOptimization() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                }
            }
        }

        @JavascriptInterface
        fun getAllowedPackages(): String {
            val pkgs = DataSyncManager.getAllowedPackages()
            return JSONArray(pkgs).toString()
        }

        @JavascriptInterface
        fun setAllowedPackages(jsonStr: String) {
            try {
                val array = JSONArray(jsonStr)
                val set = mutableSetOf<String>()
                for (i in 0 until array.length()) {
                    set.add(array.getString(i))
                }
                DataSyncManager.setAllowedPackages(set)
            } catch (e: Exception) {
                // ignore
            }
        }

        @JavascriptInterface
        fun copyToClipboard(text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Auto Track", text)
            clipboard.setPrimaryClip(clip)
            runOnUiThread {
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun onLogout() {
            DataSyncManager.clearVault()
            val intent = Intent(context, OnboardingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
            finish()
        }
    }
}
