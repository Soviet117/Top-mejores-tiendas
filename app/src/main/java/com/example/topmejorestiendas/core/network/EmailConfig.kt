package com.example.topmejorestiendas.core.network

import com.example.topmejorestiendas.BuildConfig

object EmailConfig {

    val SENDER_EMAIL = BuildConfig.SENDER_EMAIL

    val SENDER_PASSWORD = BuildConfig.SENDER_PASSWORD

    const val SMTP_HOST = "smtp.gmail.com"
    const val SMTP_PORT = "465"

    const val SENDER_DISPLAY_NAME = "Top Mejores Tiendas"

    const val OTP_LENGTH = 6

    const val EMAIL_SUBJECT = "C\u00f3digo de Verificaci\u00f3n - Top Mejores Tiendas"
}
