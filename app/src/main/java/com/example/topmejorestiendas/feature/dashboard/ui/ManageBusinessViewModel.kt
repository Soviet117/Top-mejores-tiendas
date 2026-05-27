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

sealed class ManageBusinessUiState {
    object Loading : ManageBusinessUiState()
    data class Success(val negocio: Negocio) : ManageBusinessUiState()
    data class Error(val message: String) : ManageBusinessUiState()
}

class ManageBusinessViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    
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
