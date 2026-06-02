package com.example.topmejorestiendas.feature.auth.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.core.domain.model.User
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

    init {
        checkSession()
    }

    private fun checkSession() {
        if (sessionManager.isLoggedIn) {
            val userId = sessionManager.userId
            if (userId != -1) {
                // Fetch user logic can be added here if needed to populate data
                _uiState.value = _uiState.value.copy(isLoggedIn = true)
                // For a proper implementation, we'd query the user from DB and set the 'user' field,
                // but let's do that cleanly if we need the role immediately upon cold boot.
            }
        }
    }

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
                            ruc = usuarioEntity.ruc
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

    fun register(name: String, email: String, phone: String, pass: String, isOwner: Boolean, ruc: String) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Completa los campos obligatorios")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

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
                            ruc = if (isOwner) ruc else null
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun logout() {
        sessionManager.logout()
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
