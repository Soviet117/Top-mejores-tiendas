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

data class AddBusinessState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class AddBusinessViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val sessionManager = SessionManager(application)
    
    private val _uiState = MutableStateFlow(AddBusinessState())
    val uiState: StateFlow<AddBusinessState> = _uiState.asStateFlow()

    fun registerBusiness(
        name: String, 
        category: String, 
        address: String, 
        schedule: String, 
        description: String,
        photoUri: String
    ) {
        if (name.isBlank() || category.isBlank() || address.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Nombre, Categoría y Dirección son obligatorios.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        val ownerId = sessionManager.userId
        if (ownerId == -1) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Sesión inválida")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newBusiness = Negocio(
                    name,
                    category,
                    address,
                    schedule,
                    photoUri,
                    description,
                    ownerId
                )
                
                val result = db.negocioDao().insertar(newBusiness)
                
                withContext(Dispatchers.Main) {
                    if (result > 0) {
                        _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "Error al guardar el local en la base de datos.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Error inesperado: ${e.message}")
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

class AddBusinessViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddBusinessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddBusinessViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
