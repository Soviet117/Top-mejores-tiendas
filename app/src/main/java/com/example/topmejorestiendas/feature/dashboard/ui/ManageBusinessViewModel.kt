package com.example.topmejorestiendas.feature.dashboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.database.AppDatabase
import com.example.topmejorestiendas.model.Negocio
import com.example.topmejorestiendas.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ManageBusinessUiState {
    object Loading : ManageBusinessUiState()
    data class Success(val negocio: Negocio) : ManageBusinessUiState()
    data class Error(val message: String) : ManageBusinessUiState()
}

class ManageBusinessViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val sessionManager = SessionManager(application)
    
    private val _uiState = MutableStateFlow<ManageBusinessUiState>(ManageBusinessUiState.Loading)
    val uiState: StateFlow<ManageBusinessUiState> = _uiState.asStateFlow()

    fun loadBusiness(businessId: Int) {
        _uiState.value = ManageBusinessUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val negocio = db.negocioDao().obtenerPorId(businessId)
                withContext(Dispatchers.Main) {
                    if (negocio != null) {
                        _uiState.value = ManageBusinessUiState.Success(negocio)
                    } else {
                        _uiState.value = ManageBusinessUiState.Error("Local no encontrado.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = ManageBusinessUiState.Error("Error: ${e.message}")
                }
            }
        }
    }

    fun deleteBusiness(businessId: Int, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = sessionManager.userId
            if (userId == -1) {
                withContext(Dispatchers.Main) { onResult(false, "Sesión inválida") }
                return@launch
            }

            val usuarioEntity = db.usuarioDao().obtenerPorId(userId)
            if (usuarioEntity != null) {
                if (usuarioEntity.contrasena == password) {
                    db.negocioDao().eliminarPorId(businessId)
                    withContext(Dispatchers.Main) { onResult(true, "Local eliminado.") }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "Contraseña incorrecta.") }
                }
            } else {
                withContext(Dispatchers.Main) { onResult(false, "Error de autenticación.") }
            }
        }
    }
}

class ManageBusinessViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageBusinessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManageBusinessViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
