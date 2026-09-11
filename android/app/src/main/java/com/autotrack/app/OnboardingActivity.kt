package com.autotrack.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.autotrack.app.databinding.ActivityOnboardingBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val existingVault = DataSyncManager.getVaultCode()
        if (existingVault.isNullOrEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val code = DataSyncManager.createVault()
                withContext(Dispatchers.Main) {
                    binding.tvVaultCode.text = code ?: "ERROR-RETRY"
                }
            }
        } else {
            binding.tvVaultCode.text = existingVault
        }

        binding.btnCopyCode.setOnClickListener {
            val code = binding.tvVaultCode.text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("AutoTrack Vault Code", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Vault code copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.btnGrantNotif.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        binding.btnGrantOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }

        binding.btnGrantBattery.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        }

        binding.btnEnterApp.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatuses()
    }

    private fun updatePermissionStatuses() {
        // 1. Notification Listener Status
        val notifGranted = isNotificationListenerGranted()
        binding.btnGrantNotif.text = if (notifGranted) "Granted ✓" else "Grant"
        binding.btnGrantNotif.isEnabled = !notifGranted

        // 2. Overlay Status
        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
        binding.btnGrantOverlay.text = if (overlayGranted) "Granted ✓" else "Grant"
        binding.btnGrantOverlay.isEnabled = !overlayGranted

        // 3. Battery Optimization Status
        val batteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
        binding.btnGrantBattery.text = if (batteryIgnored) "Disabled ✓" else "Disable"
        binding.btnGrantBattery.isEnabled = !batteryIgnored
    }

    private fun isNotificationListenerGranted(): Boolean {
        val packages = NotificationManagerCompat.getEnabledListenerPackages(this)
        return packages.contains(packageName)
    }
}
