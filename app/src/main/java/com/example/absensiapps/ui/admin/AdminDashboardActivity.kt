package com.example.absensiapps.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.absensiapps.MainActivity
import com.example.absensiapps.data.repository.AuthRepository
import com.example.absensiapps.databinding.ActivityAdminDashboardBinding
import com.example.absensiapps.utils.applySystemBarInsets
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applySystemBarInsets()

        setupDashboard()
        setupMenu()
        setupLogout()
        setupBackPress()
    }

    // =====================================
    // DASHBOARD
    // =====================================

    private fun setupDashboard() {

        val fullName = intent.getStringExtra("full_name") ?: "Admin"

        binding.tvWelcome.text = "Halo, $fullName!"

        val dateFormat = SimpleDateFormat(
            "EEEE, dd MMMM yyyy",
            Locale("id", "ID")
        )

        binding.tvCurrentDate.text = dateFormat.format(Date())
    }

    // =====================================
    // NAVIGATION MENU
    // =====================================
    private fun setupMenu() {

        // Menu Manajemen Pegawai
        binding.cardEmployees.setOnClickListener {

            val intent = Intent(
                this,
                EmployeeManagementActivity::class.java
            )

            startActivity(intent)
        }

        // Menu Data Kehadiran
        binding.cardAttendance.setOnClickListener {

            val intent = Intent(
                this,
                AttendanceManagementActivity::class.java
            )

            startActivity(intent)
        }
    }

    // =====================================
    // LOGOUT
    // =====================================

    private fun setupLogout() {

        binding.btnLogout.setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle("Keluar dari Akun?")
                .setMessage(
                    "Apakah Anda yakin ingin mengakhiri sesi login?"
                )
                .setNegativeButton("Batal", null)
                .setPositiveButton("Logout") { _, _ ->

                    processLogout()

                }
                .show()
        }
    }

    private fun processLogout() {

        binding.btnLogout.isEnabled = false
        binding.btnLogout.text = "Keluar..."

        lifecycleScope.launch {

            try {

                authRepository.logout()

                val intent = Intent(
                    this@AdminDashboardActivity,
                    MainActivity::class.java
                ).apply {

                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                }

                startActivity(intent)

            } catch (e: Exception) {

                Toast.makeText(
                    this@AdminDashboardActivity,
                    "Logout gagal: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                binding.btnLogout.isEnabled = true
                binding.btnLogout.text = "Keluar dari Akun"
            }
        }
    }

    // =====================================
    // BACK BUTTON
    // =====================================

    private fun setupBackPress() {

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    AlertDialog.Builder(this@AdminDashboardActivity)
                        .setTitle("Keluar Aplikasi?")
                        .setMessage(
                            "Apakah Anda yakin ingin keluar dari Absensi Apps?"
                        )
                        .setNegativeButton("Batal", null)
                        .setPositiveButton("Keluar") { _, _ ->

                            finish()
                        }
                        .show()
                }
            }
        )
    }
}
