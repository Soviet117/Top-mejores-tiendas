package com.example.topmejorestiendas.core.network

import com.example.topmejorestiendas.BuildConfig

/**
 * Configuración centralizada del servicio SMTP para envío de correos de verificación.
 *
 * INSTRUCCIONES:
 * 1. Crea una cuenta Gmail dedicada (ej: topmejorestiendas.app@gmail.com)
 * 2. Activa la Verificación en 2 Pasos en esa cuenta de Google
 * 3. Ve a: Cuenta de Google > Seguridad > Verificación en 2 pasos > Contraseñas de aplicaciones
 * 4. Genera una contraseña de aplicación y pégala en local.properties como SENDER_PASSWORD
 * 5. Agrega SENDER_EMAIL en local.properties con tu correo
 */
object EmailConfig {

    /** Correo remitente — Obtenido de local.properties de forma segura */
    val SENDER_EMAIL = BuildConfig.SENDER_EMAIL

    /** App Password generado desde la cuenta de Google — Obtenido de local.properties de forma segura */
    val SENDER_PASSWORD = BuildConfig.SENDER_PASSWORD

    /** Configuración del servidor SMTP de Gmail */
    const val SMTP_HOST = "smtp.gmail.com"
    const val SMTP_PORT = "465"

    /** Nombre que aparece como remitente en el correo */
    const val SENDER_DISPLAY_NAME = "Top Mejores Tiendas"

    /** Longitud del código OTP */
    const val OTP_LENGTH = 6

    /** Asunto del correo de verificación */
    const val EMAIL_SUBJECT = "Código de Verificación - Top Mejores Tiendas"
}
