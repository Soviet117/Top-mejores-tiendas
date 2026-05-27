package com.example.topmejorestiendas.feature.dashboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.database.AppDatabase
import com.example.topmejorestiendas.model.Resena
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BusinessReviewsUiState(
    val isLoading: Boolean = true,
    val reviews: List<Resena> = emptyList(),
    val error: String? = null
)

class BusinessReviewsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    
    private val _uiState = MutableStateFlow(BusinessReviewsUiState())
    val uiState: StateFlow<BusinessReviewsUiState> = _uiState.asStateFlow()

    fun loadReviews(businessId: Int) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resenas = db.resenaDao().obtenerPorNegocio(businessId)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isLoading = false, reviews = resenas)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun submitResponse(review: Resena, responseText: String) {
        if (responseText.isBlank()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Modificar el campo recién añadido en la BD
                review.respuestaDuenio = responseText
                // Nota: ResenaDao no tiene método actualizar. Usaremos un query custom o insert si room lo soporta, 
                // Pero como es un RoomDao sin @Update, deberíamos usar una forma de actualizar.
                // Sin embargo, si ResenaDao no tiene @Update, necesitamos agregarlo a ResenaDao.java
                // Para simplificar, lo haremos como paso extra (modificar ResenaDao.java)
                
                db.resenaDao().actualizar(review)
                
                // Recargar reseñas
                val resenas = db.resenaDao().obtenerPorNegocio(review.idNegocio)
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(reviews = resenas)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(error = "Error al guardar respuesta: ${e.message}")
                }
            }
        }
    }
}

class BusinessReviewsViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BusinessReviewsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BusinessReviewsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
