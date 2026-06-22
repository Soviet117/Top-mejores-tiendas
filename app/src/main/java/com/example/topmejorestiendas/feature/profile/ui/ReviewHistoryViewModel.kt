package com.example.topmejorestiendas.feature.profile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.database.AppDatabase
import com.example.topmejorestiendas.model.Resena
import com.example.topmejorestiendas.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReviewWithBusiness(
    val review: Resena,
    val businessName: String
)

data class ReviewHistoryUiState(
    val isLoading: Boolean = true,
    val reviews: List<ReviewWithBusiness> = emptyList(),
    val error: String? = null
)

class ReviewHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val resenaDao = db.resenaDao()
    private val negocioDao = db.negocioDao()
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(ReviewHistoryUiState())
    val uiState: StateFlow<ReviewHistoryUiState> = _uiState.asStateFlow()

    init {
        loadReviewHistory()
    }

    private fun loadReviewHistory() {
        val userId = sessionManager.userId
        if (userId <= 0) {
            _uiState.value = ReviewHistoryUiState(isLoading = false, error = "Sesión no válida")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reviews = resenaDao.obtenerPorUsuario(userId)
                val reviewsWithBusiness = reviews.map { review ->
                    val negocio = negocioDao.obtenerPorId(review.idNegocio)
                    ReviewWithBusiness(
                        review = review,
                        businessName = negocio?.nombreNegocio ?: "Negocio eliminado"
                    )
                }
                withContext(Dispatchers.Main) {
                    _uiState.value = ReviewHistoryUiState(
                        isLoading = false,
                        reviews = reviewsWithBusiness
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = ReviewHistoryUiState(
                        isLoading = false,
                        error = "Error al cargar historial"
                    )
                }
            }
        }
    }
}

class ReviewHistoryViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReviewHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReviewHistoryViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
