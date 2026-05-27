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

data class OwnerDashboardState(
    val isLoading: Boolean = true,
    val businesses: List<Negocio> = emptyList()
)

class OwnerDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val sessionManager = SessionManager(application)
    
    private val _uiState = MutableStateFlow(OwnerDashboardState())
    val uiState: StateFlow<OwnerDashboardState> = _uiState.asStateFlow()

    init {
        loadBusinesses()
    }

    fun loadBusinesses() {
        val userId = sessionManager.userId
        if (userId != -1) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            viewModelScope.launch(Dispatchers.IO) {
                val negocios = db.negocioDao().obtenerPorDuenio(userId)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        businesses = negocios
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false)
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
