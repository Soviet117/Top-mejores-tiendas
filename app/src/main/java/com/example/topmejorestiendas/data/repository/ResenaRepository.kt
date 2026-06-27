package com.example.topmejorestiendas.data.repository

import android.content.Context
import com.example.topmejorestiendas.data.remote.RetrofitClient
import com.example.topmejorestiendas.data.remote.dto.CreateResenaRequest
import com.example.topmejorestiendas.data.remote.dto.ResenaDto
import com.example.topmejorestiendas.utils.SessionManager

class ResenaRepository(context: Context) {

    private val api = RetrofitClient.apiService
    private val sessionManager = SessionManager(context)
    private val token get() = RetrofitClient.bearerToken(sessionManager.authToken ?: "")

    suspend fun getResenas(negocioId: Int): Result<List<ResenaDto>> {
        return try {
            val response = api.getResenas(negocioId = negocioId)
            if (response.isSuccessful) {
                Result.success(response.body()!!.resenas)
            } else {
                Result.failure(Exception("Error al obtener reseñas (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    suspend fun createResena(request: CreateResenaRequest): Result<Unit> {
        return try {
            val response = api.createResena(token, request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = when (response.code()) {
                    409 -> "Ya dejaste una reseña para este negocio"
                    else -> "Error al crear la reseña (${response.code()})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    suspend fun responderResena(resenaId: Int, respuesta: String): Result<Unit> {
        return try {
            val response = api.responderResena(token, resenaId, mapOf("respuestaDuenio" to respuesta))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al guardar la respuesta (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    suspend fun deleteResena(resenaId: Int): Result<Unit> {
        return try {
            val response = api.deleteResena(token, resenaId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al eliminar la reseña (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }
}
