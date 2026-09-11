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
import android.text.Editable
import android.text.TextWatcher
import android.view.View
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
    private var generatedCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. TextWatcher for Name input
        binding.etUserName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkPermissionAndFormStatus()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 2. Permission Buttons
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

        // 3. Step 1 -> Step 2 transition
        binding.btnContinueToStep2.setOnClickListener {
            val userName = binding.etUserName.text.toString().trim()
            if (userName.isEmpty()) {
                Toast.makeText(this, "Please enter your name to continue", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.layoutStep1.visibility = View.GONE
            binding.layoutStep2.visibility = View.VISIBLE
            binding.tvStepIndicator.text = "STEP 2 / 2"
            binding.tvWelcomeUser.text = "Hello $userName! Ready to create your Vault Code."
        }

        // 4. Step 2: Generate & Register Vault Code in Supabase
        binding.btnGenerateVault.setOnClickListener {
            val userName = binding.etUserName.text.toString().trim()
            binding.btnGenerateVault.isEnabled = false
            binding.btnGenerateVault.text = "Creating Vault..."

            CoroutineScope(Dispatchers.IO).launch {
                val code = DataSyncManager.createVault(userName)
                withContext(Dispatchers.Main) {
                    generatedCode = code
                    binding.btnGenerateVault.visibility = View.GONE
                    binding.cardVaultResult.visibility = View.VISIBLE
                    binding.tvVaultCode.text = code ?: "ERROR-RETRY"
                    binding.btnEnterApp.isEnabled = true
                }
            }
        }

        // 5. Copy Vault Code
        binding.btnCopyCode.setOnClickListener {
            val code = binding.tvVaultCode.text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("AutoTrack Vault Code", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Vault code copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // 6. Confirm & Enter Dashboard
        binding.btnEnterApp.setOnClickListener {
            val code = generatedCode ?: binding.tvVaultCode.text.toString()
            if (!code.isNullOrEmpty() && code != "-------") {
                DataSyncManager.saveVaultCode(code)
            }
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndFormStatus()
    }

    private fun checkPermissionAndFormStatus() {
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

        // Enable Step 1 -> Step 2 button ONLY when permissions are accepted AND name is non-empty
        val userName = binding.etUserName.text.toString().trim()
        val canProceed = notifGranted && overlayGranted && userName.isNotEmpty()

        binding.btnContinueToStep2.isEnabled = canProceed
        if (canProceed) {
            binding.btnContinueToStep2.alpha = 1.0f
        } else {
            binding.btnContinueToStep2.alpha = 0.5f
        }
    }

    private fun isNotificationListenerGranted(): Boolean {
        val packages = NotificationManagerCompat.getEnabledListenerPackages(this)
        return packages.contains(packageName)
    }
}
