package com.example.topmejorestiendas.feature.auth.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.core.domain.model.User
import com.example.topmejorestiendas.core.network.EmailService
import com.example.topmejorestiendas.core.network.EmailVerificationState
import com.example.topmejorestiendas.core.network.RucVerificationState
import com.example.topmejorestiendas.core.network.SunatService
import com.example.topmejorestiendas.data.repository.AuthRepository
import com.example.topmejorestiendas.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: User? = null,
    val isLoggedIn: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    // ─── Repositorios (backend API) — Room eliminado ─────────────────────
    private val authRepository = AuthRepository(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(AuthState())
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    // ── Estado de verificación de email ──────────────────────────────────
    private val _emailVerificationState = MutableStateFlow<EmailVerificationState>(EmailVerificationState.Idle)
    val emailVerificationState: StateFlow<EmailVerificationState> = _emailVerificationState.asStateFlow()

    /** Código OTP actual generado — se almacena en memoria, nunca persistido */
    private var currentOtpCode: String? = null

    /** Email que fue verificado — para evitar cambios post-verificación */
    private var verifiedEmail: String? = null

    // ── Estado de verificación de RUC SUNAT ──────────────────────────────
    private val _rucVerificationState = MutableStateFlow<RucVerificationState>(RucVerificationState.Idle)
    val rucVerificationState: StateFlow<RucVerificationState> = _rucVerificationState.asStateFlow()

    /** RUC que fue verificado — para evitar cambios post-verificación */
    private var verifiedRuc: String? = null

    init {
        checkSession()
    }

    private fun checkSession() {
        // La sesión ahora se valida por presencia del token JWT
        if (authRepository.isLoggedIn()) {
            _uiState.value = _uiState.value.copy(isLoggedIn = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOGIN → Ahora usa la API del backend
    // ═══════════════════════════════════════════════════════════════════════

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Completa todos los campos")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = authRepository.login(email, pass)
            result.onSuccess { response ->
                val u = response.user
                val domainUser = User(
                    id = u.id,
                    fullName = u.nombreCompleto,
                    email = u.email,
                    phone = u.telefono ?: "",
                    profilePhotoUrl = u.fotoPerfil ?: "",
                    isOwner = u.esDuenio,
                    ruc = u.ruc,
                    emailVerified = u.emailVerificado,
                    razonSocial = u.razonSocial
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = domainUser,
                    isLoggedIn = true
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Credenciales incorrectas"
                )
            }
        }
    }

    fun loginAsGuest() {
        sessionManager.createGuestSession()
        val guestUser = User(
            id = -2,
            fullName = "Invitado",
            email = "",
            phone = "",
            profilePhotoUrl = "",
            isOwner = false,
            ruc = null
        )
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            user = guestUser,
            isLoggedIn = true,
            error = null
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VERIFICACIÓN DE EMAIL (OTP) — Sin cambios, lógica en memoria
    // ═══════════════════════════════════════════════════════════════════════

    fun sendVerificationEmail(email: String) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailVerificationState.value = EmailVerificationState.Error("Ingresa un correo electrónico válido")
            return
        }

        _emailVerificationState.value = EmailVerificationState.Sending

        viewModelScope.launch {
            val result = EmailService.sendVerificationCode(email)

            result.onSuccess { otpCode ->
                currentOtpCode = otpCode
                _emailVerificationState.value = EmailVerificationState.CodeSent(
                    "Código enviado a $email"
                )
            }.onFailure { error ->
                val msg = error.message ?: "Error desconocido"
                val userMessage = if (msg.contains("datos móviles", ignoreCase = true) ||
                    msg.contains("WiFi universitaria", ignoreCase = true)) {
                    msg + "\n\n💡 Sugerencia: Desactiva el WiFi y usa tus datos móviles para verificar tu correo."
                } else {
                    "No se pudo enviar el código: $msg"
                }
                _emailVerificationState.value = EmailVerificationState.Error(userMessage)
            }
        }
    }

    fun verifyEmailCode(inputCode: String, email: String) {
        if (inputCode.isBlank()) {
            _emailVerificationState.value = EmailVerificationState.Error("Ingresa el código de verificación")
            return
        }

        if (currentOtpCode == null) {
            _emailVerificationState.value = EmailVerificationState.Error("Primero envía un código de verificación")
            return
        }

        if (inputCode.trim() == currentOtpCode) {
            verifiedEmail = email
            _emailVerificationState.value = EmailVerificationState.Verified
            currentOtpCode = null
        } else {
            _emailVerificationState.value = EmailVerificationState.Error("Código incorrecto. Intenta nuevamente.")
        }
    }

    fun isEmailVerified(currentEmail: String): Boolean {
        return _emailVerificationState.value is EmailVerificationState.Verified &&
                verifiedEmail == currentEmail
    }

    fun resetEmailVerification() {
        _emailVerificationState.value = EmailVerificationState.Idle
        currentOtpCode = null
        verifiedEmail = null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VERIFICACIÓN DE RUC (SUNAT) — Sin cambios
    // ═══════════════════════════════════════════════════════════════════════

    fun verifyRuc(ruc: String) {
        if (ruc.length != 11) {
            _rucVerificationState.value = RucVerificationState.Idle
            verifiedRuc = null
            return
        }

        if (!SunatService.isValidRucFormat(ruc)) {
            _rucVerificationState.value = RucVerificationState.Invalid(
                "RUC inválido. Debe empezar con 10 (persona natural) o 20 (persona jurídica)."
            )
            return
        }

        _rucVerificationState.value = RucVerificationState.Verifying

        viewModelScope.launch {
            val result = SunatService.consultarRuc(ruc)

            result.onSuccess { response ->
                if (response.isValid) {
                    verifiedRuc = ruc
                    _rucVerificationState.value = RucVerificationState.Verified(response)
                } else {
                    verifiedRuc = null
                    val reason = buildString {
                        append("RUC encontrado pero no válido: ")
                        append("Estado: ${response.estado}, Condición: ${response.condicion}. ")
                        if (response.estado.uppercase() != "ACTIVO") {
                            append("El contribuyente no está ACTIVO en SUNAT.")
                        }
                        if (response.condicion.uppercase() != "HABIDO") {
                            append("El contribuyente no tiene condición de HABIDO.")
                        }
                    }
                    _rucVerificationState.value = RucVerificationState.Invalid(reason, response)
                }
            }.onFailure { error ->
                verifiedRuc = null
                _rucVerificationState.value = RucVerificationState.Error(
                    error.message ?: "Error al consultar SUNAT"
                )
            }
        }
    }

    fun isRucVerified(currentRuc: String): Boolean {
        return _rucVerificationState.value is RucVerificationState.Verified &&
                verifiedRuc == currentRuc
    }

    fun resetRucVerification() {
        _rucVerificationState.value = RucVerificationState.Idle
        verifiedRuc = null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REGISTRO → Ahora usa la API del backend
    // ═══════════════════════════════════════════════════════════════════════

    fun register(name: String, email: String, phone: String, pass: String, isOwner: Boolean, ruc: String) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Completa los campos obligatorios")
            return
        }

        if (!isEmailVerified(email)) {
            _uiState.value = _uiState.value.copy(error = "Debes verificar tu correo electrónico antes de registrarte")
            return
        }

        if (isOwner) {
            if (ruc.isBlank()) {
                _uiState.value = _uiState.value.copy(error = "El RUC es obligatorio para dueños de negocio")
                return
            }
            if (!isRucVerified(ruc)) {
                _uiState.value = _uiState.value.copy(error = "Debes verificar tu RUC en SUNAT antes de registrarte")
                return
            }
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        val razonSocial = if (isOwner) {
            val rucState = _rucVerificationState.value
            if (rucState is RucVerificationState.Verified) rucState.response.razonSocial else null
        } else null

        viewModelScope.launch {
            val result = authRepository.register(
                nombreCompleto = name,
                email = email,
                password = pass,
                telefono = phone.ifBlank { null },
                esDuenio = isOwner,
                ruc = if (isOwner) ruc else null,
                razonSocial = razonSocial
            )

            result.onSuccess { response ->
                val u = response.user
                val domainUser = User(
                    id = u.id,
                    fullName = u.nombreCompleto,
                    email = u.email,
                    phone = u.telefono ?: "",
                    profilePhotoUrl = u.fotoPerfil ?: "",
                    isOwner = u.esDuenio,
                    ruc = u.ruc,
                    emailVerified = true,
                    razonSocial = u.razonSocial
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = domainUser,
                    isLoggedIn = true
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "No se pudo registrar el usuario"
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun logout() {
        authRepository.logout()
        resetEmailVerification()
        resetRucVerification()
        _uiState.value = AuthState()
    }
}

class AuthViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
