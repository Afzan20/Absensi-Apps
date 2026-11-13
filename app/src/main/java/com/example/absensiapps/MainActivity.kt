package com.example.absensiapps

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.absensiapps.databinding.ActivityMainBinding
import com.example.absensiapps.data.repository.AuthRepository
import com.example.absensiapps.ui.admin.AdminDashboardActivity
import com.example.absensiapps.ui.employee.EmployeeDashboardActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->

            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        setupLogin()
    }

    private fun setupLogin() {

        binding.btnLogin.setOnClickListener {

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            when {

                email.isEmpty() -> {
                    binding.etEmail.error = "Email wajib diisi"
                    binding.etEmail.requestFocus()
                }

                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    binding.etEmail.error = "Email tidak valid"
                    binding.etEmail.requestFocus()
                }

                password.isEmpty() -> {
                    binding.etPassword.error = "Password wajib diisi"
                    binding.etPassword.requestFocus()
                }

                else -> {
                    processLogin(email, password)
                }
            }
        }
    }

    private fun processLogin(email: String, password: String) {

        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Memproses..."

        lifecycleScope.launch {

            try {

                // Login dan mengambil profil pengguna
                val profile = authRepository.login(
                    email,
                    password
                )

                // Menentukan halaman berdasarkan role
                val destination = when (profile.role) {

                    "admin" -> AdminDashboardActivity::class.java

                    "employee" -> EmployeeDashboardActivity::class.java

                    else -> throw Exception("Role tidak dikenali")
                }

                // Membuka dashboard sesuai role
                val intent = Intent(
                    this@MainActivity,
                    destination
                )

                intent.putExtra(
                    "full_name",
                    profile.fullName
                )

                startActivity(intent)

                finish()

            } catch (e: Exception) {

                Toast.makeText(
                    this@MainActivity,
                    "Login gagal: ${e.message ?: "Terjadi kesalahan"}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Masuk"
            }
        }
    }
}
