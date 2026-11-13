package com.example.absensiapps.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttendanceDirectory(

    val id: String,

    @SerialName("employee_id")
    val employeeId: String,

    @SerialName("full_name")
    val fullName: String,

    @SerialName("employee_number")
    val employeeNumber: String,

    val department: String? = null,

    val position: String? = null,

    @SerialName("attendance_date")
    val attendanceDate: String,

    @SerialName("check_in_at")
    val checkInAt: String,

    @SerialName("check_out_at")
    val checkOutAt: String? = null,

    @SerialName("attendance_status")
    val attendanceStatus: String,

    @SerialName("face_verified")
    val faceVerified: Boolean,

    @SerialName("location_verified")
    val locationVerified: Boolean,

    @SerialName("check_in_distance_m")
    val checkInDistanceM: Double? = null

)
