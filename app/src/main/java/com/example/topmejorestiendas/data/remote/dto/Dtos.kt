package com.example.topmejorestiendas.data.remote.dto

import com.google.gson.annotations.SerializedName

// ─── Auth ─────────────────────────────────────────────────

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("contrasena") val contrasena: String
)

data class RegisterRequest(
    @SerializedName("nombreCompleto") val nombreCompleto: String,
    @SerializedName("email") val email: String,
    @SerializedName("contrasena") val contrasena: String,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("esDuenio") val esDuenio: Boolean = false,
    @SerializedName("ruc") val ruc: String? = null,
    @SerializedName("razonSocial") val razonSocial: String? = null,
    @SerializedName("fotoPerfil") val fotoPerfil: String? = null
)

data class AuthResponse(
    @SerializedName("message") val message: String,
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UsuarioDto
)

data class UpdateProfileRequest(
    @SerializedName("nombreCompleto") val nombreCompleto: String?,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("fotoPerfil") val fotoPerfil: String?,
    @SerializedName("ruc") val ruc: String?
)

data class UpdatePasswordRequest(
    @SerializedName("currentPassword") val currentPassword: String,
    @SerializedName("newPassword") val newPassword: String
)

data class DeleteAccountRequest(
    @SerializedName("password") val password: String
)


// ─── Usuario ───────────────────────────────────────────────

data class UsuarioDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nombreCompleto") val nombreCompleto: String,
    @SerializedName("email") val email: String,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("fotoPerfil") val fotoPerfil: String?,
    @SerializedName("esDuenio") val esDuenio: Boolean,
    @SerializedName("ruc") val ruc: String?,
    @SerializedName("razonSocial") val razonSocial: String?,
    @SerializedName("emailVerificado") val emailVerificado: Boolean
)

data class MeResponse(
    @SerializedName("user") val user: UsuarioDto
)

// ─── Negocios ──────────────────────────────────────────────

data class NegocioDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nombreNegocio") val nombreNegocio: String,
    @SerializedName("rubro") val rubro: String,
    @SerializedName("direccion") val direccion: String,
    @SerializedName("horario") val horario: String?,
    @SerializedName("calificacionPromedio") val calificacionPromedio: Float,
    @SerializedName("latitud") val latitud: Double?,
    @SerializedName("longitud") val longitud: Double?,
    @SerializedName("fotoNegocio") val fotoNegocio: String?,
    @SerializedName("descripcion") val descripcion: String?,
    @SerializedName("precios") val precios: String?,
    @SerializedName("idDuenio") val idDuenio: Int,
    @SerializedName("resenas") val resenas: List<ResenaSimpleDto>? = null,
    @SerializedName("duenio") val duenio: DuenioSimpleDto? = null
)

data class DuenioSimpleDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nombreCompleto") val nombreCompleto: String,
    @SerializedName("telefono") val telefono: String?
)

data class NegociosListResponse(
    @SerializedName("negocios") val negocios: List<NegocioDto>
)

data class NegocioResponse(
    @SerializedName("negocio") val negocio: NegocioDto
)

data class CreateNegocioRequest(
    @SerializedName("nombreNegocio") val nombreNegocio: String,
    @SerializedName("rubro") val rubro: String,
    @SerializedName("direccion") val direccion: String,
    @SerializedName("horario") val horario: String? = null,
    @SerializedName("latitud") val latitud: Double? = null,
    @SerializedName("longitud") val longitud: Double? = null,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("precios") val precios: String? = null,
    @SerializedName("fotoNegocioBase64") val fotoNegocioBase64: String? = null
)

// ─── Reseñas ───────────────────────────────────────────────

data class ResenaDto(
    @SerializedName("id") val id: Int,
    @SerializedName("idUsuario") val idUsuario: Int,
    @SerializedName("idNegocio") val idNegocio: Int,
    @SerializedName("calificacion") val calificacion: Int,
    @SerializedName("calidadAtencion") val calidadAtencion: Int,
    @SerializedName("calidadProductos") val calidadProductos: Int,
    @SerializedName("costos") val costos: Int,
    @SerializedName("comentario") val comentario: String?,
    @SerializedName("respuestaDuenio") val respuestaDuenio: String?,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("usuario") val usuario: AutorDto?,
    @SerializedName("negocio") val negocio: NegocioSimpleDto? = null
)

data class ResenaSimpleDto(
    @SerializedName("calificacion") val calificacion: Int,
    @SerializedName("calidadAtencion") val calidadAtencion: Int,
    @SerializedName("calidadProductos") val calidadProductos: Int,
    @SerializedName("costos") val costos: Int
)

data class AutorDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nombreCompleto") val nombreCompleto: String,
    @SerializedName("fotoPerfil") val fotoPerfil: String?
)

data class ResenasListResponse(
    @SerializedName("resenas") val resenas: List<ResenaDto>
)

data class CreateResenaRequest(
    @SerializedName("idNegocio") val idNegocio: Int,
    @SerializedName("calificacion") val calificacion: Int,
    @SerializedName("calidadAtencion") val calidadAtencion: Int,
    @SerializedName("calidadProductos") val calidadProductos: Int,
    @SerializedName("costos") val costos: Int,
    @SerializedName("comentario") val comentario: String? = null
)

// ─── Reservas ──────────────────────────────────────────────

data class ReservaDto(
    @SerializedName("id") val id: Int,
    @SerializedName("idNegocio") val idNegocio: Int,
    @SerializedName("idUsuario") val idUsuario: Int,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("horaInicio") val horaInicio: String,
    @SerializedName("horaFin") val horaFin: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("fechaCreacion") val fechaCreacion: String,
    @SerializedName("negocio") val negocio: NegocioSimpleDto?,
    @SerializedName("usuario") val usuario: ClienteSimpleDto?
)

data class NegocioSimpleDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nombreNegocio") val nombreNegocio: String,
    @SerializedName("rubro") val rubro: String,
    @SerializedName("fotoNegocio") val fotoNegocio: String? = null,
    @SerializedName("direccion") val direccion: String? = null
)

data class ClienteSimpleDto(
    @SerializedName("id") val id: Int,
    @SerializedName("nombreCompleto") val nombreCompleto: String,
    @SerializedName("email") val email: String,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("fotoPerfil") val fotoPerfil: String?
)

data class ReservasListResponse(
    @SerializedName("reservas") val reservas: List<ReservaDto>
)

data class CreateReservaRequest(
    @SerializedName("idNegocio") val idNegocio: Int,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("horaInicio") val horaInicio: String,
    @SerializedName("horaFin") val horaFin: String
)

data class UpdateEstadoRequest(
    @SerializedName("estado") val estado: String
)

// ─── Generic ───────────────────────────────────────────────

data class MessageResponse(
    @SerializedName("message") val message: String
)

data class ApiError(
    @SerializedName("error") val error: String,
    @SerializedName("details") val details: List<Any>? = null
)
