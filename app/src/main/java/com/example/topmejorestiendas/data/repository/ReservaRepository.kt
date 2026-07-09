package com.example.topmejorestiendas.data.repository

import android.content.Context
import com.example.topmejorestiendas.data.remote.RetrofitClient
import com.example.topmejorestiendas.data.remote.dto.AmbienteDisponibleDto
import com.example.topmejorestiendas.data.remote.dto.AsignarAmbienteRequest
import com.example.topmejorestiendas.data.remote.dto.CreateReservaRequest
import com.example.topmejorestiendas.data.remote.dto.ReservaDto
import com.example.topmejorestiendas.data.remote.dto.UpdateEstadoRequest
import com.example.topmejorestiendas.utils.SessionManager

class ReservaRepository(context: Context) {

    private val api = RetrofitClient.apiService
    private val sessionManager = SessionManager(context)
    private val token get() = RetrofitClient.bearerToken(sessionManager.authToken ?: "")

    /** Conteo de reservas PENDIENTE para el badge del inbox */
    suspend fun getPendingReservasCount(): Result<Int> {
        return try {
            val response = api.getPendingReservasCount(token)
            if (response.isSuccessful) {
                Result.success(response.body()!!.count)
            } else {
                Result.failure(Exception("Error al obtener conteo (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    /** Historial del cliente autenticado */
    suspend fun getReservasCliente(): Result<List<ReservaDto>> {
        return try {
            val response = api.getReservasCliente(token)
            if (response.isSuccessful) {
                Result.success(response.body()!!.reservas)
            } else {
                Result.failure(Exception("Error al obtener tus reservas (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    /** Inbox del dueño con datos de los clientes */
    suspend fun getReservasInbox(): Result<List<ReservaDto>> {
        return try {
            val response = api.getReservasInbox(token)
            if (response.isSuccessful) {
                Result.success(response.body()!!.reservas)
            } else {
                Result.failure(Exception("Error al obtener el inbox (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    /** Solicitar una nueva reserva */
    suspend fun createReserva(
        idNegocio: Int,
        fecha: String,
        horaInicio: String,
        horaFin: String,
        personas: Int = 1
    ): Result<Unit> {
        return try {
            val response = api.createReserva(
                token,
                CreateReservaRequest(
                    idNegocio = idNegocio,
                    fecha = fecha,
                    horaInicio = horaInicio,
                    horaFin = horaFin,
                    personas = personas
                )
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = when (response.code()) {
                    409 -> "El horario solicitado ya está ocupado"
                    400 -> "Datos inválidos. Verifica la fecha y hora."
                    else -> "Error al crear la reserva (${response.code()})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    /** Confirmar o rechazar una reserva (solo dueño) */
    suspend fun updateEstado(reservaId: Int, nuevoEstado: String): Result<Unit> {
        return try {
            val response = api.updateEstadoReserva(token, reservaId, UpdateEstadoRequest(nuevoEstado))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al actualizar la reserva (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    /** Cancelar una reserva propia (cliente) */
    suspend fun cancelarReserva(reservaId: Int): Result<Unit> {
        return try {
            val response = api.cancelarReserva(token, reservaId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al cancelar la reserva (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    /** Obtener ambientes con cuentas de unidades libres para un negocio */
    suspend fun getAmbientesDisponibles(idNegocio: Int): Result<List<AmbienteDisponibleDto>> {
        return try {
            val response = api.getAmbientesDisponibles(token, idNegocio)
            if (response.isSuccessful) {
                Result.success(response.body()!!.ambientes)
            } else {
                Result.failure(Exception("Error al obtener ambientes (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    /** Asignar un ambiente a una reserva */
    suspend fun asignarAmbiente(reservaId: Int, idAmbiente: Int, unidadNumero: Int): Result<Unit> {
        return try {
            val response = api.asignarAmbiente(token, reservaId, AsignarAmbienteRequest(idAmbiente, unidadNumero))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Error al asignar ambiente"
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    /** Quitar ambiente asignado a una reserva */
    suspend fun quitarAmbiente(reservaId: Int): Result<Unit> {
        return try {
            val response = api.quitarAmbiente(token, reservaId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al quitar ambiente (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }
}
