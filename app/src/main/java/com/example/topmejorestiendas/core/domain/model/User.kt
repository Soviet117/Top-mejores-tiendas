package com.example.topmejorestiendas.core.domain.model

data class User(
    val id: Int,
    val fullName: String,
    val email: String,
    val phone: String,
    val profilePhotoUrl: String,
    val isOwner: Boolean,
    val ruc: String? = null
)
