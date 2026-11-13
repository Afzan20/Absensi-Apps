
package com.example.absensiapps.ui.admin

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.absensiapps.data.repository.EmployeeRepository
import com.example.absensiapps.databinding.ActivityAddEmployeeBinding
import com.example.absensiapps.utils.applySystemBarInsets
import kotlinx.coroutines.launch

class AddEmployeeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEmployeeBinding

    private val employeeRepository = EmployeeRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityAddEmployeeBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.root.applySystemBarInsets()

        setupToolbar()
        setupSaveButton()
    }

    // =====================================
    // TOOLBAR
    // =====================================

    private fun setupToolbar() {

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    // =====================================
    // SAVE BUTTON
    // =====================================

    private fun setupSaveButton() {

        binding.btnSaveEmployee.setOnClickListener {

            val fullName =
                binding.etFullName.text.toString().trim()

            val employeeNumber =
                binding.etEmployeeNumber.text.toString().trim()

            val email =
                binding.etEmail.text.toString().trim()

            val password =
                binding.etPassword.text.toString()

            val department =
                binding.etDepartment.text.toString().trim()

            val position =
                binding.etPosition.text.toString().trim()

            val phone =
                binding.etPhone.text.toString().trim()

            // Validasi nama
            if (fullName.isEmpty()) {
                binding.etFullName.error = "Nama wajib diisi"
                binding.etFullName.requestFocus()
                return@setOnClickListener
            }

            // Validasi NIP
            if (employeeNumber.isEmpty()) {
                binding.etEmployeeNumber.error = "NIP wajib diisi"
                binding.etEmployeeNumber.requestFocus()
                return@setOnClickListener
            }

            // Validasi email
            if (
                email.isEmpty() ||
                !Patterns.EMAIL_ADDRESS.matcher(email).matches()
            ) {
                binding.etEmail.error = "Masukkan email yang valid"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            // Validasi password
            if (password.length < 8) {
                binding.etPassword.error =
                    "Password minimal 8 karakter"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            saveEmployee(
                fullName,
                employeeNumber,
                email,
                password,
                department,
                position,
                phone
            )
        }
    }

    // =====================================
    // SAVE EMPLOYEE TO SUPABASE
    // =====================================

    private fun saveEmployee(
        fullName: String,
        employeeNumber: String,
        email: String,
        password: String,
        department: String,
        position: String,
        phone: String
    ) {

        binding.btnSaveEmployee.isEnabled = false
        binding.btnSaveEmployee.text = "Menyimpan..."

        lifecycleScope.launch {

            try {

                employeeRepository.createEmployee(
                    fullName = fullName,
                    employeeNumber = employeeNumber,
                    email = email,
                    password = password,
                    department = department,
                    position = position,
                    phone = phone
                )

                Toast.makeText(
                    this@AddEmployeeActivity,
                    "Pegawai berhasil ditambahkan!",
                    Toast.LENGTH_LONG
                ).show()

                // Mengirim hasil ke halaman sebelumnya
                setResult(RESULT_OK)

                finish()

            } catch (e: Exception) {

                Toast.makeText(
                    this@AddEmployeeActivity,
                    "Gagal menyimpan: ${e.message ?: "Kesalahan koneksi"}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                binding.btnSaveEmployee.isEnabled = true
                binding.btnSaveEmployee.text = "Simpan Pegawai"

            }
        }
    }
}
