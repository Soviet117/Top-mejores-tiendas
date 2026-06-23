package com.example.topmejorestiendas.core.network

/**
 * Estados posibles del flujo de verificación de email.
 * Sigue el patrón sealed class para manejo exhaustivo en la UI.
 */
sealed class EmailVerificationState {
    /** Estado inicial: no se ha iniciado ninguna verificación */
    data object Idle : EmailVerificationState()

    /** Enviando el correo con el código OTP */
    data object Sending : EmailVerificationState()

    /** Código enviado exitosamente, esperando que el usuario lo ingrese */
    data class CodeSent(val message: String = "Código enviado a tu correo") : EmailVerificationState()

    /** El usuario ingresó el código correcto — Email verificado */
    data object Verified : EmailVerificationState()

    /** Error durante el proceso (red, SMTP, etc.) */
    data class Error(val message: String) : EmailVerificationState()
}
