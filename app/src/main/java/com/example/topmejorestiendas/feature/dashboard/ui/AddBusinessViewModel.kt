package com.example.topmejorestiendas.feature.dashboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.data.remote.dto.AmbienteRequestDto
import com.example.topmejorestiendas.data.remote.dto.CategoriaDto
import com.example.topmejorestiendas.data.remote.dto.CreateNegocioRequest
import com.example.topmejorestiendas.data.repository.NegocioRepository
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
    private val negocioRepository = NegocioRepository(application)
    private val sessionManager = SessionManager(application)
    
    private val _uiState = MutableStateFlow(AddBusinessState())
    val uiState: StateFlow<AddBusinessState> = _uiState.asStateFlow()

    private val _categorias = MutableStateFlow<List<CategoriaDto>>(emptyList())
    val categorias: StateFlow<List<CategoriaDto>> = _categorias.asStateFlow()

    fun loadCategorias() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = negocioRepository.getCategorias()
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { _categorias.value = it },
                    onFailure = { /* mantener lista vacía */ }
                )
            }
        }
    }

    fun createCategoria(nombre: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = negocioRepository.createCategoria(nombre)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = {
                        loadCategorias()
                        onResult(true)
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(error = it.message ?: "Error al crear categoría")
                        onResult(false)
                    }
                )
            }
        }
    }

    fun registerBusiness(
        name: String, 
        category: String, 
        address: String, 
        schedule: String, 
        description: String,
        photoUri: String,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        prices: String = "",
        ambientesStr: String = ""
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

        val ambientesList = if (ambientesStr.isNotBlank()) {
            ambientesStr.split(", ").mapNotNull { entry ->
                val parts = entry.split(": ")
                if (parts.size == 2) {
                    val nums = parts[1].split("_")
                    if (nums.size == 2) {
                        AmbienteRequestDto(
                            nombre = parts[0],
                            cantidad = nums[0].toIntOrNull() ?: 1,
                            capacidad = nums[1].toIntOrNull() ?: 1
                        )
                    } else null
                } else null
            }.takeIf { it.isNotEmpty() }
        } else null

        viewModelScope.launch(Dispatchers.IO) {
            val request = CreateNegocioRequest(
                nombreNegocio = name,
                rubro = category,
                direccion = address,
                horario = schedule.ifBlank { null },
                latitud = latitude.takeIf { it != 0.0 },
                longitud = longitude.takeIf { it != 0.0 },
                descripcion = description.ifBlank { null },
                precios = prices.ifBlank { null },
                fotoNegocioBase64 = com.example.topmejorestiendas.utils.ImageUtils.uriToBase64(getApplication(), photoUri),
                ambientes = ambientesList
            )
            
            val result = negocioRepository.createNegocio(request)
            
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = it.message ?: "Error al guardar el local")
                    }
                )
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
