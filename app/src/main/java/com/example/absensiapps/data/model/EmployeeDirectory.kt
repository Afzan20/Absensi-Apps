package com.example.absensiapps.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmployeeDirectory(

    val id: String,

    @SerialName("full_name")
    val fullName: String,

    @SerialName("employee_number")
    val employeeNumber: String,

    val department: String? = null,

    val position: String? = null,

    val phone: String? = null,

    @SerialName("hire_date")
    val hireDate: String? = null,

    @SerialName("is_active")
    val isActive: Boolean = true
)
