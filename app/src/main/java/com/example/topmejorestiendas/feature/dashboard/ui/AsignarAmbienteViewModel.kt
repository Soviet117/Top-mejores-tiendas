package com.example.topmejorestiendas.feature.dashboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.data.remote.dto.AmbienteDisponibleDto
import com.example.topmejorestiendas.data.repository.ReservaRepository
import com.example.topmejorestiendas.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AsignarAmbienteState(
    val isLoading: Boolean = false,
    val ambientes: List<AmbienteDisponibleDto> = emptyList(),
    val idAmbienteAsignado: Int? = null,
    val error: String? = null,
    val success: Boolean = false
)

class AsignarAmbienteViewModel(
    application: Application,
    private val idNegocio: Int,
    private val idReserva: Int
) : AndroidViewModel(application) {
    private val reservaRepository = ReservaRepository(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(AsignarAmbienteState())
    val uiState: StateFlow<AsignarAmbienteState> = _uiState.asStateFlow()

    fun loadAmbientes() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            val result = reservaRepository.getAmbientesDisponibles(idNegocio)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { ambientes ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            ambientes = ambientes
                        )
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
                    }
                )
            }
        }
    }

    fun asignarAmbiente(idAmbiente: Int) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            val result = reservaRepository.asignarAmbiente(idReserva, idAmbiente, 1)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            idAmbienteAsignado = idAmbiente,
                            success = true
                        )
                        loadAmbientes()
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
                    }
                )
            }
        }
    }
}

class AsignarAmbienteViewModelFactory(
    private val application: Application,
    private val idNegocio: Int,
    private val idReserva: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AsignarAmbienteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AsignarAmbienteViewModel(application, idNegocio, idReserva) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
