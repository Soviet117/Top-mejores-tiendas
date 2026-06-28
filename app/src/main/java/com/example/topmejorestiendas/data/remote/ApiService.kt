package com.example.topmejorestiendas.data.remote

import com.example.topmejorestiendas.data.remote.dto.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz Retrofit que mapea todos los endpoints del backend UbiTop.
 * Las funciones suspend son para Kotlin/coroutines.
 * Las funciones Call<> son para interop con Java.
 */
interface ApiService {

    // ─── AUTH ──────────────────────────────────────────────────
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    /** Versión Call<> para uso desde Java (RegistroUsuarioActivity) */
    @POST("api/auth/register")
    fun registerCall(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<MeResponse>

    // ─── NEGOCIOS ──────────────────────────────────────────────
    @GET("api/negocios")
    suspend fun getNegocios(
        @Query("rubro") rubro: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<NegociosListResponse>

    @GET("api/negocios/mios")
    suspend fun getMisNegocios(
        @Header("Authorization") token: String
    ): Response<NegociosListResponse>

    @GET("api/negocios/{id}")
    suspend fun getNegocioById(@Path("id") id: Int): Response<NegocioResponse>

    @POST("api/negocios")
    suspend fun createNegocio(
        @Header("Authorization") token: String,
        @Body request: CreateNegocioRequest
    ): Response<NegocioResponse>

    @PUT("api/negocios/{id}")
    suspend fun updateNegocio(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: CreateNegocioRequest
    ): Response<NegocioResponse>

    @DELETE("api/negocios/{id}")
    suspend fun deleteNegocio(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MessageResponse>

    // ─── RESEÑAS ───────────────────────────────────────────────
    @GET("api/resenas")
    suspend fun getResenas(
        @Query("negocioId") negocioId: Int? = null
    ): Response<ResenasListResponse>

    @POST("api/resenas")
    suspend fun createResena(
        @Header("Authorization") token: String,
        @Body request: CreateResenaRequest
    ): Response<MessageResponse>

    @PATCH("api/resenas/{id}/respuesta")
    suspend fun responderResena(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Response<MessageResponse>

    @DELETE("api/resenas/{id}")
    suspend fun deleteResena(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MessageResponse>

    // ─── RESERVAS ──────────────────────────────────────────────
    @GET("api/reservas/cliente")
    suspend fun getReservasCliente(
        @Header("Authorization") token: String
    ): Response<ReservasListResponse>

    @GET("api/reservas/inbox")
    suspend fun getReservasInbox(
        @Header("Authorization") token: String
    ): Response<ReservasListResponse>

    @POST("api/reservas")
    suspend fun createReserva(
        @Header("Authorization") token: String,
        @Body request: CreateReservaRequest
    ): Response<MessageResponse>

    @PATCH("api/reservas/{id}/estado")
    suspend fun updateEstadoReserva(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: UpdateEstadoRequest
    ): Response<MessageResponse>

    @DELETE("api/reservas/{id}")
    suspend fun cancelarReserva(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MessageResponse>
}
