package com.example.topmejorestiendas.core.domain.model

data class Business(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val rating: Double,
    val reviewCount: Int,
    val category: String,
    val isVerified: Boolean = false,
    val isOpen: Boolean = true,
    val distanceText: String = "Cerca de ti"
)
