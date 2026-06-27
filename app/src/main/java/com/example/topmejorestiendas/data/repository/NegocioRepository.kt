package com.example.topmejorestiendas.data.repository

import android.content.Context
import com.example.topmejorestiendas.data.remote.RetrofitClient
import com.example.topmejorestiendas.data.remote.dto.CreateNegocioRequest
import com.example.topmejorestiendas.data.remote.dto.NegocioDto
import com.example.topmejorestiendas.utils.SessionManager

class NegocioRepository(context: Context) {

    private val api = RetrofitClient.apiService
    private val sessionManager = SessionManager(context)
    private val token get() = RetrofitClient.bearerToken(sessionManager.authToken ?: "")

    suspend fun getNegocios(rubro: String? = null): Result<List<NegocioDto>> {
        return try {
            val response = api.getNegocios(rubro = rubro)
            if (response.isSuccessful) {
                Result.success(response.body()!!.negocios)
            } else {
                Result.failure(Exception("Error al obtener negocios (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    suspend fun getMisNegocios(): Result<List<NegocioDto>> {
        return try {
            val response = api.getMisNegocios(token)
            if (response.isSuccessful) {
                Result.success(response.body()!!.negocios)
            } else {
                Result.failure(Exception("Error al obtener tus negocios (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    suspend fun getNegocioById(id: Int): Result<NegocioDto> {
        return try {
            val response = api.getNegocioById(id)
            if (response.isSuccessful) {
                Result.success(response.body()!!.negocio)
            } else {
                Result.failure(Exception("Negocio no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    suspend fun createNegocio(request: CreateNegocioRequest): Result<NegocioDto> {
        return try {
            val response = api.createNegocio(token, request)
            if (response.isSuccessful) {
                Result.success(response.body()!!.negocio)
            } else {
                Result.failure(Exception("Error al crear el negocio (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    suspend fun updateNegocio(id: Int, request: CreateNegocioRequest): Result<NegocioDto> {
        return try {
            val response = api.updateNegocio(token, id, request)
            if (response.isSuccessful) {
                Result.success(response.body()!!.negocio)
            } else {
                Result.failure(Exception("Error al actualizar el negocio (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    suspend fun deleteNegocio(id: Int): Result<Unit> {
        return try {
            val response = api.deleteNegocio(token, id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al eliminar el negocio (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }
}
