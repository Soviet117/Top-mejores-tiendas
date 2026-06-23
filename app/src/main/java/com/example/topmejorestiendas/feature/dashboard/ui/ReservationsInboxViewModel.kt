package com.example.topmejorestiendas.feature.dashboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.database.AppDatabase
import com.example.topmejorestiendas.model.ReservaConDetalle
import com.example.topmejorestiendas.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReservationsInboxState(
    val isLoading: Boolean = true,
    val reservations: List<ReservaConDetalle> = emptyList()
)

class ReservationsInboxViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val sessionManager = SessionManager(application)
    
    private val _uiState = MutableStateFlow(ReservationsInboxState())
    val uiState: StateFlow<ReservationsInboxState> = _uiState.asStateFlow()

    init {
        loadReservations()
    }

    fun loadReservations() {
        val userId = sessionManager.userId
        if (userId != -1) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            viewModelScope.launch(Dispatchers.IO) {
                val reservas = db.reservaDao().getReservasPorDuenio(userId)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        reservations = reservas
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun updateReservationStatus(reservaId: Int, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.reservaDao().actualizarEstado(reservaId, newStatus)
            loadReservations()
        }
    }
}

class ReservationsInboxViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReservationsInboxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReservationsInboxViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
