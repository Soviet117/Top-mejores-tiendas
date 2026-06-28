package com.example.topmejorestiendas.feature.profile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.core.domain.model.User
import com.example.topmejorestiendas.data.repository.AuthRepository
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

    private val authRepository = AuthRepository(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        if (sessionManager.userId != -1) {
            viewModelScope.launch(Dispatchers.IO) {
                val result = authRepository.getProfile()
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { usuarioDto ->
                            _uiState.value = ProfileUiState.Success(
                                User(
                                    id = usuarioDto.id,
                                    fullName = usuarioDto.nombreCompleto,
                                    email = usuarioDto.email,
                                    phone = usuarioDto.telefono ?: "",
                                    profilePhotoUrl = usuarioDto.fotoPerfil ?: "",
                                    isOwner = usuarioDto.esDuenio,
                                    ruc = usuarioDto.ruc
                                )
                            )
                        },
                        onFailure = {
                            _uiState.value = ProfileUiState.Error(it.message ?: "Usuario no encontrado.")
                        }
                    )
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
            val result = authRepository.updateProfile(
                nombreCompleto = fullName.ifBlank { null },
                telefono = phone.ifBlank { null },
                fotoPerfil = photoUrl.ifBlank { null },
                ruc = ruc.ifBlank { null }
            )
            
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { usuarioDto ->
                        _uiState.value = ProfileUiState.Success(
                            currentUser.copy(
                                fullName = usuarioDto.nombreCompleto,
                                phone = usuarioDto.telefono ?: "",
                                profilePhotoUrl = usuarioDto.fotoPerfil ?: "",
                                ruc = usuarioDto.ruc
                            )
                        )
                        onComplete()
                    },
                    onFailure = {
                        _uiState.value = ProfileUiState.Error(it.message ?: "No se pudo actualizar el perfil.")
                    }
                )
            }
        }
    }

    fun updatePassword(currentPass: String, newPass: String, onResult: (Boolean, String) -> Unit) {
        val currentState = _uiState.value
        if (currentState !is ProfileUiState.Success) return

        viewModelScope.launch(Dispatchers.IO) {
            val result = authRepository.updatePassword(currentPass, newPass)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { onResult(true, "Contraseña actualizada con éxito.") },
                    onFailure = { onResult(false, it.message ?: "Error al actualizar contraseña.") }
                )
            }
        }
    }

    fun deleteAccount(password: String, onResult: (Boolean, String) -> Unit) {
        val currentState = _uiState.value
        if (currentState !is ProfileUiState.Success) return

        viewModelScope.launch(Dispatchers.IO) {
            val result = authRepository.deleteAccount(password)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { onResult(true, "Cuenta eliminada permanentemente.") },
                    onFailure = { onResult(false, it.message ?: "Error al eliminar la cuenta.") }
                )
            }
        }
    }

    fun logout() {
        authRepository.logout()
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
