package com.example.absensiapps.data.repository

import com.example.absensiapps.data.model.EmployeeDirectory
import com.example.absensiapps.data.remote.SupabaseClientProvider

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class EmployeeRepository(
    private val supabase: SupabaseClient =
        SupabaseClientProvider.client
) {

    // =====================================
    // GET EMPLOYEES
    // =====================================

    suspend fun getEmployees(): List<EmployeeDirectory> {

        return supabase
            .from("employee_directory")
            .select {
                order(
                    column = "full_name",
                    order = Order.ASCENDING
                )
            }
            .decodeList<EmployeeDirectory>()
    }


    // =====================================
    // CREATE EMPLOYEE
    // =====================================

    suspend fun createEmployee(
        fullName: String,
        employeeNumber: String,
        email: String,
        password: String,
        department: String,
        position: String,
        phone: String
    ) {

        val payload = buildJsonObject {

            put("full_name", fullName)

            put("employee_number", employeeNumber)

            put("email", email)

            put("password", password)

            put("department", department)

            put("position", position)

            put("phone", phone)

        }

        // Memanggil Supabase Edge Function
        supabase.functions.invoke(
            function = "create-employee",
            body = payload
        )
    }

    suspend fun updateEmployee(
        employeeId: String,
        fullName: String,
        employeeNumber: String,
        department: String,
        position: String,
        phone: String
    ) {

        val payload = buildJsonObject {

            put("employee_id", employeeId)

            put("full_name", fullName)

            put("employee_number", employeeNumber)

            put("department", department)

            put("position", position)

            put("phone", phone)

        }

        supabase.functions.invoke(
            function = "update-employee",
            body = payload
        )
    }

    // =====================================
    // ACTIVATE / DEACTIVATE EMPLOYEE
    // =====================================

    suspend fun setEmployeeStatus(
        employeeId: String,
        isActive: Boolean
    ) {

        val payload = buildJsonObject {

            put("employee_id", employeeId)

            put("is_active", isActive)

        }

        supabase.functions.invoke(
            function = "set-employee-status",
            body = payload
        )
    }
}
