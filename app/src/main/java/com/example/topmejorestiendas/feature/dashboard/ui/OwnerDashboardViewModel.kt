package com.example.topmejorestiendas.feature.dashboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.core.domain.mapper.toDomainModel
import com.example.topmejorestiendas.data.repository.NegocioRepository
import com.example.topmejorestiendas.data.repository.ReservaRepository
import com.example.topmejorestiendas.model.Negocio
import com.example.topmejorestiendas.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OwnerDashboardState(
    val isLoading: Boolean = true,
    val businesses: List<Negocio> = emptyList(),
    val error: String? = null,
    val pendingReservasCount: Int = 0,
    val profilePhotoUrl: String = ""
)

class OwnerDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val negocioRepository = NegocioRepository(application)
    private val reservaRepository = ReservaRepository(application)
    private val sessionManager = SessionManager(application)
    
    private val _uiState = MutableStateFlow(OwnerDashboardState())
    val uiState: StateFlow<OwnerDashboardState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            profilePhotoUrl = sessionManager.getProfilePhoto()
        )
        loadBusinesses()
        loadPendingReservasCount()
    }

    private fun loadPendingReservasCount() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = reservaRepository.getPendingReservasCount()
            withContext(Dispatchers.Main) {
                result.onSuccess { count ->
                    _uiState.value = _uiState.value.copy(pendingReservasCount = count)
                }
            }
        }
    }

    fun loadBusinesses() {
        val userId = sessionManager.userId
        if (userId != -1) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            viewModelScope.launch(Dispatchers.IO) {
                val result = negocioRepository.getMisNegocios()
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { negociosDto ->
                            val negociosUi = negociosDto.map { dto ->
                                Negocio(
                                    dto.nombreNegocio,
                                    dto.rubro,
                                    dto.direccion,
                                    dto.horario ?: "",
                                    dto.fotoNegocio ?: "",
                                    dto.descripcion ?: "",
                                    userId
                                ).apply {
                                    id = dto.id
                                    calificacionPromedio = dto.calificacionPromedio
                                    latitud = dto.latitud ?: 0.0
                                    longitud = dto.longitud ?: 0.0
                                    precios = dto.precios
                                }
                            }
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                businesses = negociosUi
                            )
                        },
                        onFailure = {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = it.message ?: "Error al cargar negocios"
                            )
                        }
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Sesión inválida")
        }
    }
}

class OwnerDashboardViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OwnerDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OwnerDashboardViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
