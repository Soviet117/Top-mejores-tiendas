package com.example.topmejorestiendas.feature.dashboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.database.AppDatabase
import com.example.topmejorestiendas.model.Negocio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class EditBusinessUiState {
    object Loading : EditBusinessUiState()
    data class Loaded(val negocio: Negocio) : EditBusinessUiState()
    data class Error(val message: String) : EditBusinessUiState()
    object Success : EditBusinessUiState()
}

class EditBusinessViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    
    private val _uiState = MutableStateFlow<EditBusinessUiState>(EditBusinessUiState.Loading)
    val uiState: StateFlow<EditBusinessUiState> = _uiState.asStateFlow()

    fun loadBusiness(businessId: Int) {
        _uiState.value = EditBusinessUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val negocio = db.negocioDao().obtenerPorId(businessId)
                withContext(Dispatchers.Main) {
                    if (negocio != null) {
                        _uiState.value = EditBusinessUiState.Loaded(negocio)
                    } else {
                        _uiState.value = EditBusinessUiState.Error("Local no encontrado.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = EditBusinessUiState.Error("Error: ${e.message}")
                }
            }
        }
    }

    fun updateBusiness(
        businessId: Int,
        name: String,
        category: String,
        address: String,
        schedule: String,
        description: String,
        photoUri: String,
        latitude: Double,
        longitude: Double
    ) {
        if (name.isBlank() || category.isBlank() || address.isBlank()) {
            _uiState.value = EditBusinessUiState.Error("Campos obligatorios vacíos.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val negocio = db.negocioDao().obtenerPorId(businessId)
                if (negocio != null) {
                    negocio.nombreNegocio = name
                    negocio.rubro = category
                    negocio.direccion = address
                    negocio.horario = schedule
                    negocio.descripcion = description
                    negocio.fotoNegocio = photoUri
                    negocio.latitud = latitude
                    negocio.longitud = longitude
                    
                    db.negocioDao().actualizar(negocio)
                    
                    withContext(Dispatchers.Main) {
                        _uiState.value = EditBusinessUiState.Success
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = EditBusinessUiState.Error("Error al actualizar: ${e.message}")
                }
            }
        }
    }
}

class EditBusinessViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditBusinessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditBusinessViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
