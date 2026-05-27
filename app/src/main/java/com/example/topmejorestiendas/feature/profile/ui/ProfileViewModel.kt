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

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val sessionManager = SessionManager(application)

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val userId = sessionManager.userId
        if (userId != -1) {
            viewModelScope.launch(Dispatchers.IO) {
                val usuarioEntity = db.usuarioDao().obtenerPorId(userId)
                withContext(Dispatchers.Main) {
                    if (usuarioEntity != null) {
                        _userState.value = User(
                            id = usuarioEntity.id,
                            fullName = usuarioEntity.nombreCompleto ?: "",
                            email = usuarioEntity.email ?: "",
                            phone = usuarioEntity.telefono ?: "",
                            profilePhotoUrl = usuarioEntity.fotoPerfil ?: "",
                            isOwner = usuarioEntity.esDuenio
                        )
                    }
                }
            }
        }
    }

    fun updateUser(fullName: String, phone: String, photoUrl: String, onComplete: () -> Unit) {
        val currentUser = _userState.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val usuarioEntity = db.usuarioDao().obtenerPorId(currentUser.id)
            if (usuarioEntity != null) {
                usuarioEntity.nombreCompleto = fullName
                usuarioEntity.telefono = phone
                usuarioEntity.fotoPerfil = photoUrl
                db.usuarioDao().actualizar(usuarioEntity)
                
                withContext(Dispatchers.Main) {
                    _userState.value = currentUser.copy(
                        fullName = fullName,
                        phone = phone,
                        profilePhotoUrl = photoUrl
                    )
                    onComplete()
                }
            }
        }
    }

    fun logout() {
        sessionManager.logout()
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
