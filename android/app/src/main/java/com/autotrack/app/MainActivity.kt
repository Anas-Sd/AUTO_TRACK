package com.autotrack.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.autotrack.app.ui.MainScreen
import com.autotrack.app.ui.theme.AutoTrackTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataSyncManager.init(this)

        // 1. Check if vault code exists. If first launch, show Onboarding
        val vaultCode = DataSyncManager.getVaultCode()
        if (vaultCode.isNullOrEmpty()) {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // 2. Render Native Jetpack Compose UI
        setContent {
            AutoTrackTheme {
                MainScreen(
                    onLogoutRequest = {
                        val intent = Intent(this@MainActivity, OnboardingActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }

        if (DataSyncManager.isOnline(this)) {
            DataSyncManager.flushOfflineQueue(this)
        }
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            Toast.makeText(this, "Shortcut triggered via Volume Key: Ready to log transaction!", Toast.LENGTH_SHORT).show()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // Native Javascript Bridge for compatibility
    inner class AndroidBridge(private val context: Context) {

        @JavascriptInterface
        fun isAndroidApp(): Boolean = true

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
        fun copyToClipboard(text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Vault Code", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun onLogout() {
            DataSyncManager.clearVault()
            val intent = Intent(context, OnboardingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
    }
}
