package com.example.topmejorestiendas.core.domain.mapper

import com.example.topmejorestiendas.core.domain.model.Business
import com.example.topmejorestiendas.model.Negocio
import java.util.Calendar

fun Negocio.toDomainModel(reviewCount: Int = 0): Business {
    // Lógica simple para simular si está abierto basado en la hora actual
    // En el futuro, esto debería parsear el string 'horario' (ej: "08:00 - 18:00")
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val isMockOpen = currentHour in 8..22 // Mock: Asumimos que abren de 8am a 10pm por defecto
    
    // Mock de verificación basado en si tiene foto válida
    val isMockVerified = !this.fotoNegocio.isNullOrBlank()

    return Business(
        id = this.id.toString(),
        name = this.nombreNegocio ?: "Sin Nombre",
        description = this.descripcion ?: "",
        imageUrl = this.fotoNegocio ?: "https://images.unsplash.com/photo-1554118811-1e0d58224f24?q=80&w=600&auto=format&fit=crop", // Imagen por defecto si no hay
        rating = this.calificacionPromedio.toDouble(),
        reviewCount = reviewCount,
        category = this.rubro ?: "General",
        isVerified = isMockVerified,
        isOpen = isMockOpen,
        distanceText = "A ${String.format("%.1f", Math.random() * 5 + 0.1)} km de ti" // Mockeando la distancia por ahora
    )
}
