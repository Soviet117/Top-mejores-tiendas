package com.example.topmejorestiendas.feature.profile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.data.repository.ResenaRepository
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
    private val resenaRepository = ResenaRepository(application)
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
            val result = resenaRepository.getMisResenas()
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { dtoReviews ->
                        val reviewsWithBusiness = dtoReviews.map { dto ->
                            val localReview = Resena(
                                dto.idUsuario,
                                dto.idNegocio,
                                dto.calificacion,
                                dto.calidadAtencion,
                                dto.calidadProductos,
                                dto.costos,
                                dto.comentario,
                                0L
                            ).apply {
                                id = dto.id
                                respuestaDuenio = dto.respuestaDuenio
                            }

                            ReviewWithBusiness(
                                review = localReview,
                                businessName = dto.negocio?.nombreNegocio ?: "Local eliminado"
                            )
                        }
                        _uiState.value = ReviewHistoryUiState(
                            isLoading = false,
                            reviews = reviewsWithBusiness
                        )
                    },
                    onFailure = {
                        _uiState.value = ReviewHistoryUiState(
                            isLoading = false,
                            error = it.message ?: "Error al cargar historial"
                        )
                    }
                )
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
