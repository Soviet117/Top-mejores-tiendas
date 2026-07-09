package com.example.topmejorestiendas.feature.profile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.data.repository.ReservaRepository
import com.example.topmejorestiendas.model.Reserva
import com.example.topmejorestiendas.model.ReservaConDetalle
import com.example.topmejorestiendas.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ClientReservationsState(
    val isLoading: Boolean = true,
    val reservations: List<ReservaConDetalle> = emptyList(),
    val error: String? = null
)

class ClientReservationsViewModel(application: Application) : AndroidViewModel(application) {
    private val reservaRepository = ReservaRepository(application)
    private val sessionManager = SessionManager(application)
    
    private val _uiState = MutableStateFlow(ClientReservationsState())
    val uiState: StateFlow<ClientReservationsState> = _uiState.asStateFlow()

    init {
        loadReservations()
    }

    private fun loadReservations() {
        val userId = sessionManager.userId
        if (userId != -1) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            viewModelScope.launch(Dispatchers.IO) {
                val result = reservaRepository.getReservasCliente()
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { dtoReservas ->
                            val reservasConDetalle = dtoReservas.map { dto ->
                            val localReserva = Reserva(
                                dto.idNegocio,
                                dto.idUsuario,
                                dto.fecha,
                                dto.horaInicio,
                                dto.horaFin,
                                dto.personas,
                                dto.estado,
                                0L
                            ).apply { id = dto.id }
                            
                            ReservaConDetalle().apply {
                                this.reserva = localReserva
                                this.nombreCliente = dto.usuario?.nombreCompleto ?: "Tú"
                                this.telefonoCliente = dto.usuario?.telefono
                                this.nombreNegocio = dto.negocio?.nombreNegocio ?: "Local Desconocido"
                            }
                            }
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                reservations = reservasConDetalle
                            )
                        },
                        onFailure = {
                            _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
                        }
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Sesión inválida")
        }
    }
}

class ClientReservationsViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientReservationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClientReservationsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
