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
import com.example.topmejorestiendas.database.AppDatabase
import com.example.topmejorestiendas.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: User? = null,
    val isLoggedIn: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
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
        if (sessionManager.isLoggedIn) {
            val userId = sessionManager.userId
            if (userId != -1) {
                _uiState.value = _uiState.value.copy(isLoggedIn = true)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOGIN
    // ═══════════════════════════════════════════════════════════════════════

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Completa todos los campos")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val usuarioEntity = db.usuarioDao().login(email, pass)
                withContext(Dispatchers.Main) {
                    if (usuarioEntity != null) {
                        sessionManager.createLoginSession(usuarioEntity.id)
                        val domainUser = User(
                            id = usuarioEntity.id,
                            fullName = usuarioEntity.nombreCompleto ?: "",
                            email = usuarioEntity.email ?: "",
                            phone = usuarioEntity.telefono ?: "",
                            profilePhotoUrl = usuarioEntity.fotoPerfil ?: "",
                            isOwner = usuarioEntity.esDuenio,
                            ruc = usuarioEntity.ruc,
                            emailVerified = usuarioEntity.emailVerificado,
                            razonSocial = usuarioEntity.razonSocial
                        )
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            user = domainUser,
                            isLoggedIn = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Correo o contraseña incorrectos"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error inesperado: ${e.message}"
                    )
                }
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
    // VERIFICACIÓN DE EMAIL (OTP)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Envía un código de verificación OTP al correo especificado.
     * Valida el formato del email antes de enviar.
     */
    fun sendVerificationEmail(email: String) {
        // Validar formato de email
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
                _emailVerificationState.value = EmailVerificationState.Error(
                    "No se pudo enviar el código: ${error.message}"
                )
            }
        }
    }

    /**
     * Verifica el código OTP ingresado por el usuario.
     * Si coincide con el código enviado, marca el email como verificado.
     */
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
            currentOtpCode = null // Invalidar código usado
        } else {
            _emailVerificationState.value = EmailVerificationState.Error("Código incorrecto. Intenta nuevamente.")
        }
    }

    /**
     * Verifica si el email proporcionado sigue siendo el verificado.
     * Si el usuario cambia el email después de verificar, se invalida.
     */
    fun isEmailVerified(currentEmail: String): Boolean {
        return _emailVerificationState.value is EmailVerificationState.Verified &&
                verifiedEmail == currentEmail
    }

    /**
     * Resetea el estado de verificación de email (para cuando el usuario cambia el email).
     */
    fun resetEmailVerification() {
        _emailVerificationState.value = EmailVerificationState.Idle
        currentOtpCode = null
        verifiedEmail = null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VERIFICACIÓN DE RUC (SUNAT)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Consulta un RUC en la base de datos de SUNAT.
     * Valida el formato y verifica que el contribuyente esté ACTIVO y HABIDO.
     */
    fun verifyRuc(ruc: String) {
        // Validación de formato rápida
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

    /**
     * Verifica si el RUC proporcionado sigue siendo el verificado.
     */
    fun isRucVerified(currentRuc: String): Boolean {
        return _rucVerificationState.value is RucVerificationState.Verified &&
                verifiedRuc == currentRuc
    }

    /**
     * Resetea el estado de verificación de RUC.
     */
    fun resetRucVerification() {
        _rucVerificationState.value = RucVerificationState.Idle
        verifiedRuc = null
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REGISTRO
    // ═══════════════════════════════════════════════════════════════════════

    fun register(name: String, email: String, phone: String, pass: String, isOwner: Boolean, ruc: String) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Completa los campos obligatorios")
            return
        }

        // ── Verificar que el email fue validado ──
        if (!isEmailVerified(email)) {
            _uiState.value = _uiState.value.copy(error = "Debes verificar tu correo electrónico antes de registrarte")
            return
        }

        // ── Si es dueño, verificar RUC SUNAT ──
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

        // Obtener la razón social del RUC verificado
        val razonSocial = if (isOwner) {
            val rucState = _rucVerificationState.value
            if (rucState is RucVerificationState.Verified) rucState.response.razonSocial else null
        } else null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Verificar si ya existe el usuario
                val existingUser = db.usuarioDao().obtenerPorEmail(email)
                if (existingUser != null) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "El correo ya está registrado"
                        )
                    }
                    return@launch
                }

                val newUser = com.example.topmejorestiendas.model.Usuario(
                    name,
                    email,
                    pass,
                    phone,
                    "",
                    isOwner,
                    if (isOwner) ruc else null
                )
                // Marcar email como verificado y guardar razón social
                newUser.emailVerificado = true
                newUser.razonSocial = razonSocial

                val newId = db.usuarioDao().registrar(newUser)
                
                withContext(Dispatchers.Main) {
                    if (newId > 0) {
                        sessionManager.createLoginSession(newId.toInt())
                        val domainUser = User(
                            id = newId.toInt(),
                            fullName = name,
                            email = email,
                            phone = phone,
                            profilePhotoUrl = "",
                            isOwner = isOwner,
                            ruc = if (isOwner) ruc else null,
                            emailVerified = true,
                            razonSocial = razonSocial
                        )
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            user = domainUser,
                            isLoggedIn = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "No se pudo registrar el usuario"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error inesperado: ${e.message}"
                    )
                }
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
        sessionManager.logout()
        // Resetear todos los estados de verificación
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
