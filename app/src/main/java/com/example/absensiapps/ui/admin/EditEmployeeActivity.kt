package com.example.absensiapps.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.absensiapps.data.repository.EmployeeRepository
import com.example.absensiapps.databinding.ActivityEditEmployeeBinding
import com.example.absensiapps.utils.applySystemBarInsets
import kotlinx.coroutines.launch

class EditEmployeeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditEmployeeBinding

    private val employeeRepository = EmployeeRepository()

    private var employeeId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityEditEmployeeBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.root.applySystemBarInsets()

        employeeId = intent.getStringExtra("employee_id") ?: ""

        if (employeeId.isBlank()) {

            Toast.makeText(
                this,
                "ID pegawai tidak ditemukan",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        loadEmployeeData()

        setupActions()
    }

    // =====================================
    // LOAD EMPLOYEE DATA
    // =====================================

    private fun loadEmployeeData() {

        binding.etFullName.setText(
            intent.getStringExtra("full_name") ?: ""
        )

        binding.etEmployeeNumber.setText(
            intent.getStringExtra("employee_number") ?: ""
        )

        binding.etDepartment.setText(
            intent.getStringExtra("department") ?: ""
        )

        binding.etPosition.setText(
            intent.getStringExtra("position") ?: ""
        )

        binding.etPhone.setText(
            intent.getStringExtra("phone") ?: ""
        )
    }

    // =====================================
    // BUTTON ACTIONS
    // =====================================

    private fun setupActions() {

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSaveEmployee.setOnClickListener {

            val fullName =
                binding.etFullName.text.toString().trim()

            val employeeNumber =
                binding.etEmployeeNumber.text.toString().trim()

            val department =
                binding.etDepartment.text.toString().trim()

            val position =
                binding.etPosition.text.toString().trim()

            val phone =
                binding.etPhone.text.toString().trim()

            if (fullName.isBlank()) {
                binding.etFullName.error = "Nama wajib diisi"
                binding.etFullName.requestFocus()
                return@setOnClickListener
            }

            if (employeeNumber.isBlank()) {
                binding.etEmployeeNumber.error = "NIP wajib diisi"
                binding.etEmployeeNumber.requestFocus()
                return@setOnClickListener
            }

            updateEmployee(
                fullName,
                employeeNumber,
                department,
                position,
                phone
            )
        }
    }

    // =====================================
    // UPDATE EMPLOYEE
    // =====================================

    private fun updateEmployee(
        fullName: String,
        employeeNumber: String,
        department: String,
        position: String,
        phone: String
    ) {

        binding.btnSaveEmployee.isEnabled = false

        binding.btnSaveEmployee.text = "Menyimpan..."

        lifecycleScope.launch {

            try {

                employeeRepository.updateEmployee(
                    employeeId = employeeId,
                    fullName = fullName,
                    employeeNumber = employeeNumber,
                    department = department,
                    position = position,
                    phone = phone
                )

                Toast.makeText(
                    this@EditEmployeeActivity,
                    "Data pegawai berhasil diperbarui",
                    Toast.LENGTH_SHORT
                ).show()

                setResult(RESULT_OK)

                finish()

            } catch (e: Exception) {

                Toast.makeText(
                    this@EditEmployeeActivity,
                    "Gagal memperbarui: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                binding.btnSaveEmployee.isEnabled = true

                binding.btnSaveEmployee.text = "Simpan Perubahan"
            }
        }
    }
}
