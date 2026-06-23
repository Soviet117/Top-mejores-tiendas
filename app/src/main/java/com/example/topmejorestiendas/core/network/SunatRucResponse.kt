package com.example.topmejorestiendas.core.network

/**
 * Modelo de respuesta de la API de consulta de RUC de SUNAT.
 * Mapea los campos principales de la respuesta JSON de apis.net.pe.
 *
 * Campos SUNAT:
 * - estado: "ACTIVO" o "BAJA DE OFICIO", "BAJA PROVISIONAL", etc.
 * - condicion: "HABIDO" o "NO HABIDO"
 */
data class SunatRucResponse(
    val ruc: String,
    val razonSocial: String,
    val estado: String,
    val condicion: String,
    val direccion: String,
    val departamento: String,
    val provincia: String,
    val distrito: String
) {
    /** Verifica que el contribuyente esté ACTIVO y HABIDO */
    val isValid: Boolean
        get() = estado.uppercase() == "ACTIVO" && condicion.uppercase() == "HABIDO"

    /** Dirección completa formateada */
    val direccionCompleta: String
        get() = buildString {
            if (direccion.isNotBlank()) append(direccion)
            val ubicacion = listOfNotNull(
                distrito.takeIf { it.isNotBlank() },
                provincia.takeIf { it.isNotBlank() },
                departamento.takeIf { it.isNotBlank() }
            ).joinToString(", ")
            if (ubicacion.isNotBlank()) {
                if (isNotEmpty()) append(" - ")
                append(ubicacion)
            }
        }
}
