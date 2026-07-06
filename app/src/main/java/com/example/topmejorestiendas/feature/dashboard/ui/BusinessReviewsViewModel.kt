package com.example.topmejorestiendas.feature.dashboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.data.repository.ResenaRepository
import com.example.topmejorestiendas.model.Resena
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

data class BusinessReviewsUiState(
    val isLoading: Boolean = true,
    val reviews: List<Resena> = emptyList(),
    val error: String? = null
)

class BusinessReviewsViewModel(application: Application) : AndroidViewModel(application) {
    private val resenaRepository = ResenaRepository(application)
    
    private val _uiState = MutableStateFlow(BusinessReviewsUiState())
    val uiState: StateFlow<BusinessReviewsUiState> = _uiState.asStateFlow()

    fun loadReviews(businessId: Int) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            val result = resenaRepository.getResenas(businessId)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { dtoReviews ->
                        val localReviews = dtoReviews.map { dto ->
                            Resena(
                                dto.idUsuario,
                                dto.idNegocio,
                                dto.calificacion,
                                dto.calidadAtencion,
                                dto.calidadProductos,
                                dto.costos,
                                dto.comentario,
                                parseFecha(dto.fecha)
                            ).apply {
                                id = dto.id
                                respuestaDuenio = dto.respuestaDuenio
                            }
                        }
                        _uiState.value = _uiState.value.copy(isLoading = false, reviews = localReviews)
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
                    }
                )
            }
        }
    }

    fun submitResponse(review: Resena, responseText: String) {
        if (responseText.isBlank()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            val result = resenaRepository.responderResena(review.id, responseText)
            
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    loadReviews(review.idNegocio)
                } else {
                    _uiState.value = _uiState.value.copy(error = "Error al guardar respuesta: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }
}

private val dateFormats = arrayOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
    "yyyy-MM-dd'T'HH:mm:ss",
    "yyyy-MM-dd"
)

private fun parseFecha(fechaStr: String?): Long {
    if (fechaStr.isNullOrBlank()) return 0L
    for (format in dateFormats) {
        try {
            return SimpleDateFormat(format, Locale.US).parse(fechaStr)?.time ?: 0L
        } catch (_: Exception) { }
    }
    return 0L
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
