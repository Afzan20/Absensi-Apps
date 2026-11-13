package com.example.absensiapps.ui.admin

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.absensiapps.R
import com.example.absensiapps.data.model.EmployeeDirectory
import com.example.absensiapps.data.repository.EmployeeRepository
import com.example.absensiapps.databinding.ActivityEmployeeManagementBinding
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge
import com.example.absensiapps.utils.applySystemBarInsets
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog

class EmployeeManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmployeeManagementBinding

    private val employeeRepository = EmployeeRepository()

    private val editEmployeeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == RESULT_OK) {
            loadEmployees()
        }
    }

    private var employeeList: List<EmployeeDirectory> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityEmployeeManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mengatur jarak dari status bar dan navigation bar
        binding.root.applySystemBarInsets()

        setupToolbar()
        setupSearch()

        loadEmployees()
    }

    // =====================================
    // TOOLBAR
    // =====================================

    private fun setupToolbar() {

        // Tombol kembali
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Tombol refresh
        binding.btnRefresh.setOnClickListener {
            loadEmployees()
        }

        // Tombol tambah pegawai
        binding.btnAddEmployee.setOnClickListener {

            val intent = Intent(
                this,
                AddEmployeeActivity::class.java
            )

            addEmployeeLauncher.launch(intent)
        }
    }

    // =====================================
    // SEARCH EMPLOYEE
    // =====================================

    private fun setupSearch() {

        binding.etSearch.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                showEmployees(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}

        })
    }

    // =====================================
    // LOAD EMPLOYEES FROM SUPABASE
    // =====================================

    private fun loadEmployees() {

        binding.progressBar.visibility = View.VISIBLE
        binding.tvMessage.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE

        binding.btnRefresh.isEnabled = false

        lifecycleScope.launch {

            try {

                employeeList = employeeRepository.getEmployees()

                showEmployees(binding.etSearch.text.toString())

            } catch (e: Exception) {

                binding.tvMessage.text =
                    "Gagal mengambil data: ${e.message ?: "Kesalahan koneksi"}"

                binding.tvMessage.visibility = View.VISIBLE

                binding.employeeContainer.removeAllViews()

                binding.tvCount.text = "Data tidak tersedia"

            } finally {

                binding.progressBar.visibility = View.GONE
                binding.btnRefresh.isEnabled = true

            }
        }
    }

    // =====================================
    // DISPLAY EMPLOYEES
    // =====================================

    private fun showEmployees(keyword: String = "") {

        binding.employeeContainer.removeAllViews()

        val filteredEmployees = employeeList.filter {

            it.fullName.contains(keyword, ignoreCase = true) ||
                    it.employeeNumber.contains(keyword, ignoreCase = true) ||
                    (it.department ?: "").contains(keyword, ignoreCase = true)

        }

        binding.tvCount.text =
            "${filteredEmployees.size} Pegawai"

        if (filteredEmployees.isEmpty()) {

            binding.tvEmpty.text = if (keyword.isEmpty()) {
                "Belum ada data pegawai.\nData akan muncul setelah pegawai didaftarkan."
            } else {
                "Pegawai tidak ditemukan."
            }

            binding.tvEmpty.visibility = View.VISIBLE

            return
        }

        binding.tvEmpty.visibility = View.GONE

        filteredEmployees.forEach { employee ->

            val card = createEmployeeCard(employee)

            binding.employeeContainer.addView(card)

        }
    }

    // =====================================
    // CREATE EMPLOYEE CARD
    // =====================================

    private fun createEmployeeCard(
        employee: EmployeeDirectory
    ): LinearLayout {

        val card = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(17),
                dp(16),
                dp(17),
                dp(16)
            )

            background = ContextCompat.getDrawable(
                this@EmployeeManagementActivity,
                R.drawable.bg_login_card
            )

            elevation = dp(2).toFloat()

        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        params.bottomMargin = dp(13)

        card.layoutParams = params

        // Header pegawai
        val header = LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

        }

        val name = createText(
            employee.fullName,
            16f,
            Color.rgb(36, 85, 58),
            true
        )

        name.layoutParams = LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        )

        val status = TextView(this).apply {

            text = if (employee.isActive) "Aktif" else "Nonaktif"

            textSize = 11f

            setTypeface(null, Typeface.BOLD)

            setTextColor(
                if (employee.isActive) {
                    Color.rgb(49, 169, 104)
                } else {
                    Color.rgb(190, 75, 75)
                }
            )

        }

        header.addView(name)
        header.addView(status)

        card.addView(header)

        // Informasi pegawai
        addCardInfo(
            card,
            "NIP: ${employee.employeeNumber}"
        )

        addCardInfo(
            card,
            "Departemen: ${employee.department ?: "-"}"
        )

        addCardInfo(
            card,
            "Jabatan: ${employee.position ?: "-"}"
        )

        addCardInfo(
            card,
            "Nomor Telepon: ${employee.phone ?: "-"}"
        )

        addCardInfo(
            card,
            "Tanggal Bergabung: ${employee.hireDate ?: "-"}"
        )


        // =====================================
        // BUTTON ACTIVATE / DEACTIVATE
        // =====================================

        val toggleButton = TextView(this).apply {

            text = if (employee.isActive) {
                "Nonaktifkan Pegawai"
            } else {
                "Aktifkan Kembali"
            }

            textSize = 12f

            setTypeface(null, Typeface.BOLD)

            setTextColor(
                if (employee.isActive) {
                    Color.rgb(190, 65, 65)
                } else {
                    Color.rgb(36, 130, 75)
                }
            )

            background = ContextCompat.getDrawable(
                this@EmployeeManagementActivity,
                if (employee.isActive) {
                    R.drawable.bg_status_danger
                } else {
                    R.drawable.bg_status_success
                }
            )

            gravity = Gravity.CENTER

            setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
            )

            isClickable = true
            isFocusable = true
        }

        val toggleParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(15)
        }

        card.addView(toggleButton, toggleParams)

        toggleButton.setOnClickListener {
            showStatusConfirmation(employee, toggleButton)
        }

        card.isClickable = true
        card.isFocusable = true

        card.setOnClickListener {

            val intent = Intent(
                this,
                EditEmployeeActivity::class.java
            ).apply {

                putExtra("employee_id", employee.id)

                putExtra("full_name", employee.fullName)

                putExtra(
                    "employee_number",
                    employee.employeeNumber
                )

                putExtra(
                    "department",
                    employee.department ?: ""
                )

                putExtra(
                    "position",
                    employee.position ?: ""
                )

                putExtra(
                    "phone",
                    employee.phone ?: ""
                )
            }

            editEmployeeLauncher.launch(intent)
        }

        return card
    }

    // =====================================
    // CARD INFORMATION
    // =====================================

    private fun addCardInfo(
        parent: LinearLayout,
        text: String
    ) {

        val textView = createText(
            text,
            12f,
            Color.rgb(130, 144, 135),
            false
        )

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        params.topMargin = dp(8)

        textView.layoutParams = params

        parent.addView(textView)
    }

    // =====================================
    // TEXT VIEW HELPER
    // =====================================

    private fun createText(
        text: String,
        size: Float,
        color: Int,
        bold: Boolean
    ): TextView {

        return TextView(this).apply {

            this.text = text

            textSize = size

            setTextColor(color)

            if (bold) {
                setTypeface(null, Typeface.BOLD)
            }

        }
    }

    // =====================================
    // DP CONVERTER
    // =====================================

    private fun dp(value: Int): Int {

        return (
                value * resources.displayMetrics.density
                ).toInt()

    }

    private val addEmployeeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == RESULT_OK) {
            // Memuat ulang data setelah pegawai ditambahkan
            loadEmployees()
        }
    }


// =====================================
// CONFIRM STATUS CHANGE
// =====================================

    private fun showStatusConfirmation(
        employee: EmployeeDirectory,
        button: TextView
    ) {

        val newStatus = !employee.isActive

        val title = if (newStatus) {
            "Aktifkan Pegawai?"
        } else {
            "Nonaktifkan Pegawai?"
        }

        val message = if (newStatus) {

            "Pegawai ${employee.fullName} akan diaktifkan kembali dan dapat login ke aplikasi."

        } else {

            "Pegawai ${employee.fullName} tidak akan dapat login lagi. Seluruh riwayat absensi dan pengajuan izinnya tetap tersimpan."

        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Batal", null)
            .setPositiveButton(
                if (newStatus) "Aktifkan" else "Nonaktifkan"
            ) { _, _ ->

                updateEmployeeStatus(
                    employee,
                    newStatus,
                    button
                )

            }
            .show()
    }


// =====================================
// UPDATE STATUS EMPLOYEE
// =====================================

    private fun updateEmployeeStatus(
        employee: EmployeeDirectory,
        newStatus: Boolean,
        button: TextView
    ) {

        button.isEnabled = false
        button.text = "Memproses..."

        lifecycleScope.launch {

            try {

                employeeRepository.setEmployeeStatus(
                    employeeId = employee.id,
                    isActive = newStatus
                )

                Toast.makeText(
                    this@EmployeeManagementActivity,
                    if (newStatus) {
                        "Pegawai berhasil diaktifkan"
                    } else {
                        "Pegawai berhasil dinonaktifkan"
                    },
                    Toast.LENGTH_SHORT
                ).show()

                // Memuat ulang data pegawai
                loadEmployees()

            } catch (e: Exception) {

                Toast.makeText(
                    this@EmployeeManagementActivity,
                    "Gagal mengubah status: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                button.isEnabled = true

                button.text = if (employee.isActive) {
                    "Nonaktifkan Pegawai"
                } else {
                    "Aktifkan Kembali"
                }
            }
        }
    }

}
