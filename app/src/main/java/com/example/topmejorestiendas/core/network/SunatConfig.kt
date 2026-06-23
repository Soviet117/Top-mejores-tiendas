package com.example.topmejorestiendas.core.network

import com.example.topmejorestiendas.BuildConfig

/**
 * Configuración de la API de consulta de RUC SUNAT.
 *
 * INSTRUCCIONES:
 * 1. Regístrate en https://apis.net.pe
 * 2. Obtén tu Bearer Token gratuito desde el panel de control
 * 3. Agrega tu token en el archivo local.properties como SUNAT_BEARER_TOKEN
 *
 * NOTA: El plan gratuito permite ~100 consultas diarias.
 */
object SunatConfig {

    /** URL base de la API de consulta RUC (v1) */
    const val BASE_URL = "https://api.apis.net.pe/v1/ruc"

    /** Token de autenticación — Obtenido de local.properties de forma segura */
    val BEARER_TOKEN = BuildConfig.SUNAT_BEARER_TOKEN

    /** Referer requerido por la API */
    const val REFERER = "https://apis.net.pe/api-consulta-ruc"

    /** Longitud esperada de un RUC válido peruano */
    const val RUC_LENGTH = 11
}
