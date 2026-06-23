package com.example.topmejorestiendas.core.network

/**
 * Configuración de la API de consulta de RUC SUNAT.
 *
 * INSTRUCCIONES:
 * 1. Regístrate en https://apis.net.pe
 * 2. Obtén tu Bearer Token gratuito desde el panel de control
 * 3. Reemplaza BEARER_TOKEN con tu token
 *
 * NOTA: El plan gratuito permite ~100 consultas diarias.
 */
object SunatConfig {

    /** URL base de la API de consulta RUC */
    const val BASE_URL = "https://api.apis.net.pe/v2/sunat/ruc"

    /** Token de autenticación — Reemplazar con tu token de apis.net.pe */
    const val BEARER_TOKEN = "TU_BEARER_TOKEN_AQUI"

    /** Referer requerido por la API */
    const val REFERER = "https://apis.net.pe/api-consulta-ruc"

    /** Longitud esperada de un RUC válido peruano */
    const val RUC_LENGTH = 11
}
