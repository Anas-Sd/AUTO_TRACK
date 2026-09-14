package com.autotrack.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        binding.btnEnterVault.setOnClickListener {
            val code = binding.etVaultCode.text.toString().trim().uppercase()
            if (code.length < 4) {
                binding.tvLoginError.text = "Please enter a valid vault code"
                binding.tvLoginError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            binding.tvLoginError.visibility = View.GONE
            binding.btnEnterVault.isEnabled = false
            binding.btnEnterVault.text = "Unlocking Vault..."

            CoroutineScope(Dispatchers.IO).launch {
                val token = DataSyncManager.issueVaultSession(code)
                withContext(Dispatchers.Main) {
                    binding.btnEnterVault.isEnabled = true
                    binding.btnEnterVault.text = "Unlock Dashboard →"

                    if (!token.isNullOrEmpty()) {
                        DataSyncManager.saveVaultCode(code)
                        val intent = Intent(this@OnboardingActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        binding.tvLoginError.text = "Invalid Vault Code. Please check and try again."
                        binding.tvLoginError.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}
