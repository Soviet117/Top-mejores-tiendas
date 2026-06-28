package com.example.topmejorestiendas.data.repository

import android.content.Context
import com.example.topmejorestiendas.data.remote.RetrofitClient
import com.example.topmejorestiendas.data.remote.dto.AuthResponse
import com.example.topmejorestiendas.data.remote.dto.LoginRequest
import com.example.topmejorestiendas.data.remote.dto.RegisterRequest
import com.example.topmejorestiendas.data.remote.dto.UpdateProfileRequest
import com.example.topmejorestiendas.data.remote.dto.UpdatePasswordRequest
import com.example.topmejorestiendas.data.remote.dto.DeleteAccountRequest
import com.example.topmejorestiendas.data.remote.dto.UsuarioDto
import com.example.topmejorestiendas.utils.SessionManager

/**
 * Repositorio de autenticación.
 * Centraliza login, registro y gestión de sesión (token JWT en SessionManager).
 */
class AuthRepository(context: Context) {

    private val api = RetrofitClient.apiService
    private val sessionManager = SessionManager(context)

    /**
     * Inicia sesión y guarda el token + datos del usuario en SessionManager.
     * @return Result con el AuthResponse o un error descriptivo.
     */
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(email = email, contrasena = password))
            if (response.isSuccessful) {
                val body = response.body()!!
                sessionManager.saveSession(
                    body.token,
                    body.user.id,
                    body.user.esDuenio,
                    body.user.nombreCompleto,
                    body.user.email,
                    body.user.fotoPerfil ?: ""
                )
                Result.success(body)
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "Credenciales incorrectas"
                    else -> "Error del servidor (${response.code()})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión al servidor. Verifica tu internet."))
        }
    }

    /**
     * Registra un nuevo usuario.
     * @return Result con el AuthResponse o un error descriptivo.
     */
    suspend fun register(
        nombreCompleto: String,
        email: String,
        password: String,
        telefono: String? = null,
        esDuenio: Boolean = false,
        ruc: String? = null,
        razonSocial: String? = null
    ): Result<AuthResponse> {
        return try {
            val response = api.register(
                RegisterRequest(
                    nombreCompleto = nombreCompleto,
                    email = email,
                    contrasena = password,
                    telefono = telefono,
                    esDuenio = esDuenio,
                    ruc = ruc,
                    razonSocial = razonSocial
                )
            )
            if (response.isSuccessful) {
                val body = response.body()!!
                sessionManager.saveSession(
                    body.token,
                    body.user.id,
                    body.user.esDuenio,
                    body.user.nombreCompleto,
                    body.user.email,
                    body.user.fotoPerfil ?: ""
                )
                Result.success(body)
            } else {
                val errorMsg = when (response.code()) {
                    409 -> "El email ya está registrado"
                    400 -> "Datos inválidos. Verifica los campos."
                    else -> "Error del servidor (${response.code()})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión al servidor. Verifica tu internet."))
        }
    }

    /**
     * Obtiene el perfil del usuario autenticado desde el servidor.
     */
    suspend fun getProfile(): Result<UsuarioDto> {
        return try {
            val token = sessionManager.authToken ?: return Result.failure(Exception("No autenticado"))
            val response = api.getMe(RetrofitClient.bearerToken(token))
            if (response.isSuccessful) {
                Result.success(response.body()!!.user)
            } else {
                Result.failure(Exception("Error al obtener el perfil"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    suspend fun updateProfile(
        nombreCompleto: String?,
        telefono: String?,
        fotoPerfil: String?,
        ruc: String?
    ): Result<UsuarioDto> {
        return try {
            val token = sessionManager.authToken ?: return Result.failure(Exception("No autenticado"))
            val request = UpdateProfileRequest(nombreCompleto, telefono, fotoPerfil, ruc)
            val response = api.updateProfile(RetrofitClient.bearerToken(token), request)
            if (response.isSuccessful) {
                val user = response.body()!!.user
                user.fotoPerfil?.let { sessionManager.setProfilePhoto(it) }
                Result.success(user)
            } else {
                Result.failure(Exception("Error al actualizar perfil"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    suspend fun updatePassword(currentPass: String, newPass: String): Result<Unit> {
        return try {
            val token = sessionManager.authToken ?: return Result.failure(Exception("No autenticado"))
            val request = UpdatePasswordRequest(currentPass, newPass)
            val response = api.updatePassword(RetrofitClient.bearerToken(token), request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val msg = if (response.code() == 401) "Contraseña actual incorrecta" else "Error al actualizar contraseña"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    suspend fun deleteAccount(password: String): Result<Unit> {
        return try {
            val token = sessionManager.authToken ?: return Result.failure(Exception("No autenticado"))
            val request = DeleteAccountRequest(password)
            val response = api.deleteAccount(RetrofitClient.bearerToken(token), request)
            if (response.isSuccessful) {
                sessionManager.clearSession()
                Result.success(Unit)
            } else {
                val msg = if (response.code() == 401) "Contraseña incorrecta" else "Error al eliminar cuenta"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sin conexión. Verifica tu internet."))
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }

    fun isLoggedIn() = sessionManager.authToken != null
}
