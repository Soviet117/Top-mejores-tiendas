package com.example.topmejorestiendas.feature.dashboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.core.domain.mapper.toDomainModel
import com.example.topmejorestiendas.data.remote.dto.CreateNegocioRequest
import com.example.topmejorestiendas.data.repository.NegocioRepository
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
    private val negocioRepository = NegocioRepository(application)
    
    private val _uiState = MutableStateFlow<EditBusinessUiState>(EditBusinessUiState.Loading)
    val uiState: StateFlow<EditBusinessUiState> = _uiState.asStateFlow()

    fun loadBusiness(businessId: Int) {
        _uiState.value = EditBusinessUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val result = negocioRepository.getNegocioById(businessId)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { dto ->
                        val negocioLocal = Negocio(
                            dto.nombreNegocio,
                            dto.rubro,
                            dto.direccion,
                            dto.horario ?: "",
                            dto.fotoNegocio ?: "",
                            dto.descripcion ?: "",
                            dto.idDuenio
                        ).apply {
                            id = dto.id
                            latitud = dto.latitud ?: 0.0
                            longitud = dto.longitud ?: 0.0
                            precios = dto.precios
                            calificacionPromedio = dto.calificacionPromedio
                        }
                        _uiState.value = EditBusinessUiState.Loaded(negocioLocal)
                    },
                    onFailure = {
                        _uiState.value = EditBusinessUiState.Error(it.message ?: "Error al cargar el local")
                    }
                )
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
        longitude: Double,
        prices: String = ""
    ) {
        if (name.isBlank() || category.isBlank() || address.isBlank()) {
            _uiState.value = EditBusinessUiState.Error("Campos obligatorios vacíos.")
            return
        }

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
                fotoNegocioBase64 = com.example.topmejorestiendas.utils.ImageUtils.uriToBase64(getApplication(), photoUri)
            )
            
            val result = negocioRepository.updateNegocio(businessId, request)
            
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = {
                        _uiState.value = EditBusinessUiState.Success
                    },
                    onFailure = {
                        _uiState.value = EditBusinessUiState.Error(it.message ?: "Error al actualizar")
                    }
                )
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
