package com.example.topmejorestiendas.core.network

import com.example.topmejorestiendas.data.remote.RetrofitClient
import com.example.topmejorestiendas.data.remote.dto.SendVerificationCodeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EmailService {

    private val api = RetrofitClient.apiService

    suspend fun sendVerificationCode(recipientEmail: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.sendVerificationCode(
                    SendVerificationCodeRequest(email = recipientEmail)
                )

                if (response.isSuccessful) {
                    val body = response.body()!!
                    Result.success(body.otpCode)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error ${response.code()}"
                    Result.failure(Exception(errorBody))
                }
            } catch (e: Exception) {
                android.util.Log.e("EmailService", "Error al enviar código: ${e.message}", e)
                Result.failure(Exception("Sin conexi\u00f3n al servidor. Verifica tu internet."))
            }
        }
    }
}
