package com.example.topmejorestiendas.feature.profile.ui

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

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: User) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val userId = sessionManager.userId
        if (userId != -1) {
            viewModelScope.launch(Dispatchers.IO) {
                val usuarioEntity = db.usuarioDao().obtenerPorId(userId)
                withContext(Dispatchers.Main) {
                    if (usuarioEntity != null) {
                        _uiState.value = ProfileUiState.Success(
                            User(
                                id = usuarioEntity.id,
                                fullName = usuarioEntity.nombreCompleto ?: "",
                                email = usuarioEntity.email ?: "",
                                phone = usuarioEntity.telefono ?: "",
                                profilePhotoUrl = usuarioEntity.fotoPerfil ?: "",
                                isOwner = usuarioEntity.esDuenio,
                                ruc = usuarioEntity.ruc
                            )
                        )
                    } else {
                        _uiState.value = ProfileUiState.Error("Usuario no encontrado en la base de datos.")
                    }
                }
            }
        } else {
            _uiState.value = ProfileUiState.Error("Sesión inválida o expirada.")
        }
    }

    fun updateUser(fullName: String, phone: String, photoUrl: String, ruc: String, onComplete: () -> Unit) {
        val currentState = _uiState.value
        if (currentState !is ProfileUiState.Success) return
        
        val currentUser = currentState.user
        _uiState.value = ProfileUiState.Loading
        
        viewModelScope.launch(Dispatchers.IO) {
            val usuarioEntity = db.usuarioDao().obtenerPorId(currentUser.id)
            if (usuarioEntity != null) {
                usuarioEntity.nombreCompleto = fullName
                usuarioEntity.telefono = phone
                usuarioEntity.fotoPerfil = photoUrl
                usuarioEntity.ruc = ruc
                db.usuarioDao().actualizar(usuarioEntity)
                
                withContext(Dispatchers.Main) {
                    _uiState.value = ProfileUiState.Success(
                        currentUser.copy(
                            fullName = fullName,
                            phone = phone,
                            profilePhotoUrl = photoUrl,
                            ruc = ruc
                        )
                    )
                    onComplete()
                }
            } else {
                withContext(Dispatchers.Main) {
                    _uiState.value = ProfileUiState.Error("No se pudo actualizar el perfil.")
                }
            }
        }
    }

    fun updatePassword(currentPass: String, newPass: String, onResult: (Boolean, String) -> Unit) {
        val currentState = _uiState.value
        if (currentState !is ProfileUiState.Success) return
        val currentUser = currentState.user

        viewModelScope.launch(Dispatchers.IO) {
            val usuarioEntity = db.usuarioDao().obtenerPorId(currentUser.id)
            if (usuarioEntity != null) {
                if (usuarioEntity.contrasena == currentPass) {
                    usuarioEntity.contrasena = newPass
                    db.usuarioDao().actualizar(usuarioEntity)
                    withContext(Dispatchers.Main) { onResult(true, "Contraseña actualizada con éxito.") }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "La contraseña actual es incorrecta.") }
                }
            } else {
                withContext(Dispatchers.Main) { onResult(false, "Usuario no encontrado.") }
            }
        }
    }

    fun deleteAccount(password: String, onResult: (Boolean, String) -> Unit) {
        val currentState = _uiState.value
        if (currentState !is ProfileUiState.Success) return
        val currentUser = currentState.user

        viewModelScope.launch(Dispatchers.IO) {
            val usuarioEntity = db.usuarioDao().obtenerPorId(currentUser.id)
            if (usuarioEntity != null) {
                if (usuarioEntity.contrasena == password) {
                    // Eliminación en cascada
                    db.negocioDao().eliminarPorDuenio(currentUser.id)
                    db.usuarioDao().eliminarPorId(currentUser.id)
                    withContext(Dispatchers.Main) { 
                        sessionManager.logout()
                        onResult(true, "Cuenta eliminada permanentemente.") 
                    }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "Contraseña incorrecta.") }
                }
            } else {
                withContext(Dispatchers.Main) { onResult(false, "Usuario no encontrado.") }
            }
        }
    }

    fun logout() {
        sessionManager.logout()
        _uiState.value = ProfileUiState.Loading
    }
}

class ProfileViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
