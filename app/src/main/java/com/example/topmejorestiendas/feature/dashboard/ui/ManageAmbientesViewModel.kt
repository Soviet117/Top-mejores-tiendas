package com.example.topmejorestiendas.feature.dashboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.data.remote.dto.AmbienteEntry
import com.example.topmejorestiendas.data.repository.ReservaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ManageAmbientesState(
    val isLoading: Boolean = false,
    val ambientes: List<AmbienteEntry> = emptyList(),
    val showEditDialog: Boolean = false,
    val editingAmbiente: AmbienteEntry? = null,
    val error: String? = null
)

class ManageAmbientesViewModel(
    application: Application,
    private val idNegocio: Int
) : AndroidViewModel(application) {
    private val repository = ReservaRepository(application)

    private val _uiState = MutableStateFlow(ManageAmbientesState())
    val uiState: StateFlow<ManageAmbientesState> = _uiState.asStateFlow()

    fun loadAmbientes() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getAmbientesByNegocio(idNegocio)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { ambientes ->
                        _uiState.value = _uiState.value.copy(isLoading = false, ambientes = ambientes)
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
                    }
                )
            }
        }
    }

    fun startEditing(ambiente: AmbienteEntry) {
        _uiState.value = _uiState.value.copy(showEditDialog = true, editingAmbiente = ambiente)
    }

    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(showEditDialog = false, editingAmbiente = null)
    }

    fun saveAmbiente(id: Int, nombre: String, cantidad: Int, capacidad: Int) {
        _uiState.value = _uiState.value.copy(isLoading = true, showEditDialog = false)
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.updateAmbiente(id, nombre, cantidad, capacidad)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(editingAmbiente = null)
                        loadAmbientes()
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = it.message, showEditDialog = true)
                    }
                )
            }
        }
    }

    fun toggleAmbiente(id: Int, estabaActivo: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.toggleAmbiente(id)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { loadAmbientes() },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
                    }
                )
            }
        }
    }
}

class ManageAmbientesViewModelFactory(
    private val application: Application,
    private val idNegocio: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageAmbientesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManageAmbientesViewModel(application, idNegocio) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
