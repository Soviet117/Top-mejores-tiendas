package com.example.topmejorestiendas.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Servicio de envío de correos electrónicos de verificación vía SMTP (JavaMail).
 *
 * Genera un código OTP de 6 dígitos y lo envía al correo del usuario
 * con un template HTML profesional. Opera en Dispatchers.IO para no
 * bloquear el hilo principal.
 *
 * Uso:
 * ```
 * val result = EmailService.sendVerificationCode("usuario@gmail.com")
 * result.onSuccess { otpCode -> /* guardar código */ }
 * result.onFailure { error -> /* mostrar error */ }
 * ```
 */
object EmailService {

    private val secureRandom = SecureRandom()

    /**
     * Genera un código OTP numérico seguro de [EmailConfig.OTP_LENGTH] dígitos.
     * Usa SecureRandom para criptografía segura.
     */
    fun generateOtpCode(): String {
        val max = Math.pow(10.0, EmailConfig.OTP_LENGTH.toDouble()).toInt()
        val min = Math.pow(10.0, (EmailConfig.OTP_LENGTH - 1).toDouble()).toInt()
        val code = min + secureRandom.nextInt(max - min)
        return code.toString()
    }

    /**
     * Envía un código de verificación al correo especificado.
     *
     * @param recipientEmail Dirección de correo del destinatario
     * @return Result con el código OTP generado (Success) o excepción (Failure)
     */
    suspend fun sendVerificationCode(recipientEmail: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val otpCode = generateOtpCode()

                val properties = Properties().apply {
                    put("mail.smtp.host", EmailConfig.SMTP_HOST)
                    put("mail.smtp.port", EmailConfig.SMTP_PORT)
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.socketFactory.port", EmailConfig.SMTP_PORT)
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.ssl.enable", "true")
                    put("mail.smtp.connectiontimeout", "10000")
                    put("mail.smtp.timeout", "10000")
                    put("mail.smtp.writetimeout", "10000")
                }

                val session = Session.getInstance(properties, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(
                            EmailConfig.SENDER_EMAIL,
                            EmailConfig.SENDER_PASSWORD
                        )
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(EmailConfig.SENDER_EMAIL, EmailConfig.SENDER_DISPLAY_NAME))
                    setRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                    subject = EmailConfig.EMAIL_SUBJECT
                    setContent(buildHtmlTemplate(otpCode, recipientEmail), "text/html; charset=utf-8")
                }

                Transport.send(message)

                Result.success(otpCode)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Genera el template HTML profesional del correo de verificación.
     */
    private fun buildHtmlTemplate(otpCode: String, email: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="margin:0;padding:0;background-color:#f4f4f7;font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;">
            <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f7;padding:40px 0;">
                <tr>
                    <td align="center">
                        <table width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:12px;box-shadow:0 4px 24px rgba(0,0,0,0.08);overflow:hidden;">
                            <!-- Header -->
                            <tr>
                                <td style="background:linear-gradient(135deg,#1a237e 0%,#5c6bc0 100%);padding:32px 40px;text-align:center;">
                                    <h1 style="color:#ffffff;margin:0;font-size:22px;font-weight:700;letter-spacing:0.5px;">
                                        🏆 Top Mejores Tiendas
                                    </h1>
                                    <p style="color:#c5cae9;margin:8px 0 0;font-size:13px;">
                                        Verificación de Correo Electrónico
                                    </p>
                                </td>
                            </tr>
                            <!-- Body -->
                            <tr>
                                <td style="padding:40px;">
                                    <p style="color:#333;font-size:15px;margin:0 0 16px;line-height:1.6;">
                                        ¡Hola! 👋
                                    </p>
                                    <p style="color:#555;font-size:14px;margin:0 0 24px;line-height:1.6;">
                                        Recibimos una solicitud para verificar tu correo electrónico 
                                        <strong style="color:#1a237e;">$email</strong> en la aplicación 
                                        <strong>Top Mejores Tiendas</strong>. Usa el siguiente código:
                                    </p>
                                    <!-- OTP Code Box -->
                                    <table width="100%" cellpadding="0" cellspacing="0">
                                        <tr>
                                            <td align="center" style="padding:16px 0;">
                                                <div style="background:linear-gradient(135deg,#e8eaf6 0%,#f3e5f5 100%);border:2px dashed #5c6bc0;border-radius:12px;padding:20px 40px;display:inline-block;">
                                                    <span style="font-size:36px;font-weight:800;letter-spacing:12px;color:#1a237e;font-family:'Courier New',monospace;">
                                                        $otpCode
                                                    </span>
                                                </div>
                                            </td>
                                        </tr>
                                    </table>
                                    <p style="color:#888;font-size:12px;margin:24px 0 0;line-height:1.5;text-align:center;">
                                        ⏰ Este código es válido por <strong>10 minutos</strong>.<br>
                                        Si no solicitaste este código, puedes ignorar este mensaje.
                                    </p>
                                </td>
                            </tr>
                            <!-- Footer -->
                            <tr>
                                <td style="background-color:#f8f9fa;padding:20px 40px;text-align:center;border-top:1px solid #e8e8e8;">
                                    <p style="color:#aaa;font-size:11px;margin:0;line-height:1.5;">
                                        © 2026 Top Mejores Tiendas — Perú<br>
                                        Este es un correo automático, por favor no respondas.
                                    </p>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.trimIndent()
    }
}
