
package com.example.absensiapps.ui.employee

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.absensiapps.data.remote.SupabaseClientProvider
import com.example.absensiapps.databinding.ActivityBiometricConsentBinding
import com.example.absensiapps.utils.applySystemBarInsets
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BiometricConsentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBiometricConsentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityBiometricConsentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applySystemBarInsets()

        setupConsent()
    }

    private fun setupConsent() {

        binding.btnContinue.isEnabled = false

        binding.cbConsent.setOnCheckedChangeListener { _, isChecked ->
            binding.btnContinue.isEnabled = isChecked
        }

        binding.btnContinue.setOnClickListener {

            if (!binding.cbConsent.isChecked) return@setOnClickListener

            saveConsent()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun saveConsent() {

        binding.btnContinue.isEnabled = false
        binding.btnCancel.isEnabled = false
        binding.cbConsent.isEnabled = false
        binding.btnContinue.text = "Menyimpan persetujuan..."

        lifecycleScope.launch {

            try {

                val response = SupabaseClientProvider.client.functions.invoke(
                    function = "save-biometric-consent",
                    body = buildJsonObject {
                        put("accepted", true)
                    }
                )

                if (!response.status.isSuccess()) {
                    throw Exception("Gagal menyimpan persetujuan.")
                }

                Toast.makeText(
                    this@BiometricConsentActivity,
                    "Persetujuan berhasil disimpan",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(
                    this@BiometricConsentActivity,
                    FaceDetectionActivity::class.java
                )

                startActivity(intent)
                finish()

            } catch (e: Exception) {

                binding.btnContinue.isEnabled = binding.cbConsent.isChecked
                binding.btnCancel.isEnabled = true
                binding.cbConsent.isEnabled = true
                binding.btnContinue.text = "Setuju dan Lanjutkan"

                Toast.makeText(
                    this@BiometricConsentActivity,
                    "Gagal menyimpan persetujuan: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
