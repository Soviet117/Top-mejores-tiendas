package com.example.topmejorestiendas.core.network

import com.example.topmejorestiendas.data.remote.RetrofitClient
import com.example.topmejorestiendas.data.remote.dto.SendVerificationCodeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object EmailService {

    private val api = RetrofitClient.apiService
    private const val MAX_RETRIES = 1
    private const val RETRY_DELAY_MS = 3000L

    suspend fun sendVerificationCode(recipientEmail: String): Result<String> {
        return withContext(Dispatchers.IO) {
            var lastError: Exception? = null

            for (attempt in 0..MAX_RETRIES) {
                if (attempt > 0) {
                    android.util.Log.i("EmailService", "Reintentando envío ($attempt/$MAX_RETRIES)...")
                    delay(RETRY_DELAY_MS)
                }

                try {
                    val response = api.sendVerificationCode(
                        SendVerificationCodeRequest(email = recipientEmail)
                    )

                    if (response.isSuccessful) {
                        val body = response.body()!!
                        return@withContext Result.success(body.otpCode)
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Error ${response.code()}"
                        lastError = Exception(errorBody)
                    }
                } catch (e: SocketTimeoutException) {
                    android.util.Log.w("EmailService", "Timeout (intento ${attempt + 1}): ${e.message}")
                    lastError = e
                } catch (e: ConnectException) {
                    android.util.Log.w("EmailService", "Conexión rechazada (intento ${attempt + 1}): ${e.message}")
                    lastError = e
                } catch (e: UnknownHostException) {
                    android.util.Log.w("EmailService", "DNS sin resolución (intento ${attempt + 1}): ${e.message}")
                    lastError = e
                } catch (e: Exception) {
                    android.util.Log.e("EmailService", "Error inesperado: ${e.message}", e)
                    lastError = e
                }
            }

            val message = when (lastError) {
                is SocketTimeoutException ->
                    "El servidor está tardando mucho en responder. Reintenta en unos momentos."
                is ConnectException ->
                    "No se pudo conectar al servidor. Si usas WiFi universitaria, prueba con datos móviles."
                is UnknownHostException ->
                    "No se pudo resolver el servidor. Verifica tu conexión a internet."
                else -> lastError?.message ?: "Sin conexión al servidor. Verifica tu internet."
            }

            android.util.Log.e("EmailService", "Error final tras ${MAX_RETRIES + 1} intentos: $message")
            Result.failure(Exception(message))
        }
    }
}
