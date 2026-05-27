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
                                isOwner = usuarioEntity.esDuenio
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

    fun updateUser(fullName: String, phone: String, photoUrl: String, onComplete: () -> Unit) {
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
                db.usuarioDao().actualizar(usuarioEntity)
                
                withContext(Dispatchers.Main) {
                    _uiState.value = ProfileUiState.Success(
                        currentUser.copy(
                            fullName = fullName,
                            phone = phone,
                            profilePhotoUrl = photoUrl
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
