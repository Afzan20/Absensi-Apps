package com.example.absensiapps.data.repository

import com.example.absensiapps.data.model.AttendanceDirectory
import com.example.absensiapps.data.remote.SupabaseClientProvider

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class AttendanceRepository(
    private val supabase: SupabaseClient =
        SupabaseClientProvider.client
) {

    // =====================================
    // GET ATTENDANCE BY DATE
    // =====================================

    suspend fun getAttendanceByDate(
        date: String
    ): List<AttendanceDirectory> {

        return supabase
            .from("attendance_directory")
            .select {
                filter {
                    eq("attendance_date", date)
                }

                order(
                    column = "check_in_at",
                    order = Order.DESCENDING
                )
            }
            .decodeList<AttendanceDirectory>()
    }

}
