package com.example.absensiapps.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.functions.Functions

object SupabaseClientProvider {

    private const val SUPABASE_URL =
        "https://irymcabrwwsasvfzwjoo.supabase.co"

    private const val SUPABASE_PUBLISHABLE_KEY =
        "sb_publishable_myichBs9V1qctAYYdtSlGg_-grSQJZA"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_PUBLISHABLE_KEY
    ) {

        install(Auth)

        install(Postgrest)

        // Edge Functions
        install(Functions)
    }
}
