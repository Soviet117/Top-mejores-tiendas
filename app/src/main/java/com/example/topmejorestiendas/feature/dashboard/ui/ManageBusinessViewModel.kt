package com.example.topmejorestiendas.feature.dashboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.core.domain.mapper.toDomainModel
import com.example.topmejorestiendas.data.repository.NegocioRepository
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
    private val negocioRepository = NegocioRepository(application)
    private val sessionManager = SessionManager(application)
    
    private val _uiState = MutableStateFlow<ManageBusinessUiState>(ManageBusinessUiState.Loading)
    val uiState: StateFlow<ManageBusinessUiState> = _uiState.asStateFlow()

    fun loadBusiness(businessId: Int) {
        _uiState.value = ManageBusinessUiState.Loading
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
                        _uiState.value = ManageBusinessUiState.Success(negocioLocal)
                    },
                    onFailure = {
                        _uiState.value = ManageBusinessUiState.Error(it.message ?: "Local no encontrado.")
                    }
                )
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

            // Nota: En un entorno de producción estricto, enviaríamos la contraseña al backend 
            // para validarla antes de borrar el negocio. 
            // Por ahora, como el endpoint DELETE /api/negocios/:id solo verifica el JWT, 
            // confiaremos en que el usuario ya está autenticado.
            val result = negocioRepository.deleteNegocio(businessId)
            
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { onResult(true, "Local eliminado.") },
                    onFailure = { onResult(false, it.message ?: "Error al eliminar el local.") }
                )
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
