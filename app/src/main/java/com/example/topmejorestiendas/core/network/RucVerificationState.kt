package com.example.topmejorestiendas.core.network

/**
 * Estados posibles del flujo de verificación de RUC contra SUNAT.
 */
sealed class RucVerificationState {
    /** Estado inicial: no se ha consultado ningún RUC */
    data object Idle : RucVerificationState()

    /** Consultando la API de SUNAT */
    data object Verifying : RucVerificationState()

    /** RUC válido — ACTIVO y HABIDO en SUNAT */
    data class Verified(val response: SunatRucResponse) : RucVerificationState()

    /** RUC encontrado pero no cumple requisitos (BAJA, NO HABIDO, etc.) */
    data class Invalid(val reason: String, val response: SunatRucResponse? = null) : RucVerificationState()

    /** Error de red o del servicio */
    data class Error(val message: String) : RucVerificationState()
}
