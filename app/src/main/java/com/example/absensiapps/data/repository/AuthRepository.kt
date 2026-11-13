package com.example.absensiapps.data.repository

import com.example.absensiapps.data.model.UserProfile
import com.example.absensiapps.data.remote.SupabaseClientProvider

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class AuthRepository(
    private val supabase: SupabaseClient =
        SupabaseClientProvider.client
) {

    // =====================================
    // LOGIN
    // =====================================

    suspend fun login(
        email: String,
        password: String
    ): UserProfile {

        // 1. Login menggunakan Supabase Auth
        supabase.auth.signInWith(Email) {

            this.email = email
            this.password = password

        }

        // 2. Mengambil ID pengguna yang login
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw Exception("Sesi login tidak ditemukan.")

        // 3. Mengambil profil berdasarkan ID pengguna
        val profile = supabase.from("profiles")
            .select(
                columns = Columns.list(
                    "id",
                    "full_name",
                    "role"
                )
            ) {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingle<UserProfile>()

        // 4. Memeriksa role pengguna
        if (profile.role != "admin" &&
            profile.role != "employee"
        ) {
            supabase.auth.signOut()

            throw Exception("Role pengguna tidak dikenali.")
        }

        // 5. Mengembalikan profil pengguna
        return profile
    }


    // =====================================
    // LOGOUT
    // =====================================

    suspend fun logout() {

        supabase.auth.signOut()

    }
}