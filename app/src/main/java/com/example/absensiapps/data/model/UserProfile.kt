package com.example.absensiapps.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(

    val id: String,

    @SerialName("full_name")
    val fullName: String,

    val role: String
)