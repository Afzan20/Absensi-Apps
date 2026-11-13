package com.example.absensiapps.ui.admin

import android.app.DatePickerDialog
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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.absensiapps.R
import com.example.absensiapps.data.model.AttendanceDirectory
import com.example.absensiapps.data.repository.AttendanceRepository
import com.example.absensiapps.databinding.ActivityAttendanceManagementBinding
import com.example.absensiapps.utils.applySystemBarInsets
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.OffsetDateTime
import java.util.Locale

class AttendanceManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceManagementBinding

    private val attendanceRepository = AttendanceRepository()

    private val jakartaZone = ZoneId.of("Asia/Jakarta")

    private var selectedDate: LocalDate =
        LocalDate.now(ZoneId.of("Asia/Jakarta"))

    private var attendanceList: List<AttendanceDirectory> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityAttendanceManagementBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.root.applySystemBarInsets()

        setupToolbar()
        setupDateFilter()
        setupSearch()

        loadAttendance()
    }

    // =====================================
    // TOOLBAR
    // =====================================

    private fun setupToolbar() {

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRefresh.setOnClickListener {
            loadAttendance()
        }
    }

    // =====================================
    // DATE FILTER
    // =====================================

    private fun setupDateFilter() {

        updateDateLabel()

        binding.tvSelectedDate.setOnClickListener {

            DatePickerDialog(
                this,

                { _, year, month, day ->

                    selectedDate = LocalDate.of(
                        year,
                        month + 1,
                        day
                    )

                    updateDateLabel()

                    loadAttendance()
                },

                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth

            ).show()
        }
    }

    private fun updateDateLabel() {

        val formatter = DateTimeFormatter.ofPattern(
            "EEEE, dd MMMM yyyy",
            Locale("id", "ID")
        )

        binding.tvSelectedDate.text =
            selectedDate.format(formatter)
    }

    // =====================================
    // SEARCH
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

                showAttendance(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}

        })
    }

    // =====================================
    // LOAD ATTENDANCE
    // =====================================

    private fun loadAttendance() {

        binding.progressBar.visibility = View.VISIBLE

        binding.tvMessage.visibility = View.GONE

        binding.tvEmpty.visibility = View.GONE

        binding.btnRefresh.isEnabled = false

        lifecycleScope.launch {

            try {

                attendanceList =
                    attendanceRepository.getAttendanceByDate(
                        selectedDate.toString()
                    )

                updateStatistics()

                showAttendance(
                    binding.etSearch.text.toString()
                )

            } catch (e: Exception) {

                attendanceList = emptyList()

                binding.attendanceContainer.removeAllViews()

                binding.tvTotalRecords.text = "--"
                binding.tvPresentCount.text = "--"
                binding.tvLateCount.text = "--"

                binding.tvMessage.text =
                    "Gagal mengambil data: ${e.message ?: "Kesalahan koneksi"}"

                binding.tvMessage.visibility = View.VISIBLE

            } finally {

                binding.progressBar.visibility = View.GONE

                binding.btnRefresh.isEnabled = true
            }
        }
    }

    // =====================================
    // STATISTICS
    // =====================================

    private fun updateStatistics() {

        val total = attendanceList.size

        val present = attendanceList.count {
            it.attendanceStatus == "present"
        }

        val late = attendanceList.count {
            it.attendanceStatus == "late"
        }

        binding.tvTotalRecords.text = total.toString()

        binding.tvPresentCount.text = present.toString()

        binding.tvLateCount.text = late.toString()
    }

    // =====================================
    // SHOW ATTENDANCE
    // =====================================

    private fun showAttendance(keyword: String = "") {

        binding.attendanceContainer.removeAllViews()

        val filtered = attendanceList.filter {

            it.fullName.contains(keyword, ignoreCase = true) ||
                    it.employeeNumber.contains(keyword, ignoreCase = true) ||
                    (it.department ?: "").contains(keyword, ignoreCase = true)

        }

        if (filtered.isEmpty()) {

            binding.tvEmpty.text = if (keyword.isNotBlank()) {
                "Pegawai tidak ditemukan."
            } else {
                "Belum ada data kehadiran pada tanggal ini."
            }

            binding.tvEmpty.visibility = View.VISIBLE

            return
        }

        binding.tvEmpty.visibility = View.GONE

        filtered.forEach { attendance ->

            binding.attendanceContainer.addView(
                createAttendanceCard(attendance)
            )
        }
    }

    // =====================================
    // CREATE ATTENDANCE CARD
    // =====================================

    private fun createAttendanceCard(
        attendance: AttendanceDirectory
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
                this@AttendanceManagementActivity,
                R.drawable.bg_login_card
            )

            elevation = dp(2).toFloat()
        }

        val cardParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        cardParams.bottomMargin = dp(13)

        card.layoutParams = cardParams

        // NAME AND STATUS

        val header = LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL
        }

        val name = createText(
            attendance.fullName,
            15f,
            Color.rgb(36, 85, 58),
            true
        )

        name.layoutParams = LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        )

        val isLate = attendance.attendanceStatus == "late"

        val statusBadge = TextView(this).apply {

            text = if (isLate) "TERLAMBAT" else "HADIR"

            textSize = 10f

            setTypeface(null, Typeface.BOLD)

            setTextColor(
                if (isLate) {
                    Color.rgb(177, 123, 25)
                } else {
                    Color.rgb(36, 130, 75)
                }
            )

            background = ContextCompat.getDrawable(
                this@AttendanceManagementActivity,
                if (isLate) {
                    R.drawable.bg_status_pending
                } else {
                    R.drawable.bg_status_success
                }
            )

            setPadding(
                dp(10),
                dp(7),
                dp(10),
                dp(7)
            )
        }

        header.addView(name)
        header.addView(statusBadge)

        card.addView(header)

        // EMPLOYEE INFORMATION

        addCardInfo(
            card,
            "NIP: ${attendance.employeeNumber}"
        )

        addCardInfo(
            card,
            "Departemen: ${attendance.department ?: "-"}"
        )

        // DIVIDER

        val divider = View(this).apply {
            setBackgroundColor(Color.rgb(232, 239, 234))
        }

        val dividerParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(1)
        )

        dividerParams.topMargin = dp(14)
        dividerParams.bottomMargin = dp(13)

        card.addView(divider, dividerParams)

        // ATTENDANCE TIME

        addCardInfo(
            card,
            "Jam Masuk: ${formatTime(attendance.checkInAt)}",
            true
        )

        addCardInfo(
            card,
            "Jam Pulang: ${formatTime(attendance.checkOutAt)}",
            true
        )

        // VERIFICATION STATUS

        val faceStatus = if (attendance.faceVerified) {
            "Terverifikasi"
        } else {
            "Belum terverifikasi"
        }

        val locationStatus = if (attendance.locationVerified) {
            "Terverifikasi"
        } else {
            "Belum terverifikasi"
        }

        addCardInfo(
            card,
            "Verifikasi Wajah: $faceStatus"
        )

        addCardInfo(
            card,
            "Verifikasi Lokasi: $locationStatus"
        )

        if (attendance.checkInDistanceM != null) {

            addCardInfo(
                card,
                "Jarak dari Kantor: ${"%.1f".format(Locale.US, attendance.checkInDistanceM)} meter"
            )
        }

        return card
    }

    // =====================================
    // FORMAT TIMESTAMP
    // =====================================

    private fun formatTime(value: String?): String {

        if (value.isNullOrBlank()) {
            return "--:--"
        }

        return try {

            OffsetDateTime.parse(value)
                .atZoneSameInstant(jakartaZone)
                .format(
                    DateTimeFormatter.ofPattern("HH:mm")
                )

        } catch (_: Exception) {

            try {

                Instant.parse(value)
                    .atZone(jakartaZone)
                    .format(
                        DateTimeFormatter.ofPattern("HH:mm")
                    )

            } catch (_: Exception) {

                value.substringAfter("T", value).take(5)
            }
        }
    }

    // =====================================
    // CARD INFORMATION
    // =====================================

    private fun addCardInfo(
        parent: LinearLayout,
        text: String,
        bold: Boolean = false
    ) {

        val textView = createText(
            text,
            12f,
            Color.rgb(130, 144, 135),
            bold
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
    // CLEAR CONTAINER
    // =====================================

    private fun android.widget.LinearLayout.employeeContainerClear() {

        binding.attendanceContainer.removeAllViews()

    }

    // =====================================
    // DP CONVERTER
    // =====================================

    private fun dp(value: Int): Int {

        return (
                value * resources.displayMetrics.density
                ).toInt()
    }
}
