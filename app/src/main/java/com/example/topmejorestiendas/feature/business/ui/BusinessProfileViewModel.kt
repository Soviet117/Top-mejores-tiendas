package com.example.topmejorestiendas.feature.business.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.core.domain.mapper.toDomainModel
import com.example.topmejorestiendas.core.domain.model.Business
import com.example.topmejorestiendas.data.remote.dto.CreateResenaRequest
import com.example.topmejorestiendas.data.repository.NegocioRepository
import com.example.topmejorestiendas.data.repository.ResenaRepository
import com.example.topmejorestiendas.data.repository.ReservaRepository
import com.example.topmejorestiendas.model.Resena
import com.example.topmejorestiendas.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BusinessProfileUiState(
    val isLoading: Boolean = true,
    val business: Business? = null,
    val reviews: List<Resena> = emptyList(),
    val error: String? = null,
    val isGuest: Boolean = false,
    val userReview: Resena? = null,
    val hasReviewAccess: Boolean = false,
    val qrAccessMessage: String? = null,
    val showQrScanner: Boolean = false
)

class BusinessProfileViewModel(
    application: Application,
    private val businessId: String
) : AndroidViewModel(application) {

    private val negocioRepository = NegocioRepository(application)
    private val resenaRepository = ResenaRepository(application)
    private val reservaRepository = ReservaRepository(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(BusinessProfileUiState())
    val uiState: StateFlow<BusinessProfileUiState> = _uiState.asStateFlow()

    init {
        loadBusinessData()
    }

    private fun loadBusinessData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bId = businessId.toIntOrNull() ?: return@launch
                
                val negocioResult = negocioRepository.getNegocioById(bId)
                if (negocioResult.isSuccess) {
                    val negocioDto = negocioResult.getOrNull()!!
                    val mappedBusiness = negocioDto.toDomainModel()
                    
                    // Convert DTO reviews to UI Resena model
                    val resenasDto = negocioDto.resenas ?: emptyList()
                    val resenas = resenasDto.map { dto ->
                        Resena(
                            0, // idUsuario
                            bId, // idNegocio
                            dto.calificacion,
                            dto.calidadAtencion,
                            dto.calidadProductos,
                            dto.costos,
                            "", // comentario
                            0L // fecha
                        )
                    }

                    // For the userReview, we might need a separate call or check if they left one
                    // To keep it simple, we will fetch full reviews if needed or just use what we have
                    val allResenasResult = resenaRepository.getResenas(bId)
                    val fullReviews = if (allResenasResult.isSuccess) {
                        allResenasResult.getOrNull()!!.map { dto ->
                            Resena(
                                dto.idUsuario,
                                dto.idNegocio,
                                dto.calificacion,
                                dto.calidadAtencion,
                                dto.calidadProductos,
                                dto.costos,
                                dto.comentario,
                                0L // fecha
                            ).apply { 
                                id = dto.id
                                respuestaDuenio = dto.respuestaDuenio
                            }
                        }
                    } else emptyList()

                    val currentUserId = sessionManager.userId
                    val userReview = if (currentUserId > 0) {
                        fullReviews.find { it.idUsuario == currentUserId }
                    } else null
                    
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            business = mappedBusiness,
                            reviews = fullReviews,
                            isGuest = sessionManager.userId == -2 || sessionManager.userId == -1,
                            userReview = userReview
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = negocioResult.exceptionOrNull()?.message ?: "Negocio no encontrado"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error al cargar datos"
                    )
                }
            }
        }
    }

    fun toggleQrScanner() {
        _uiState.value = _uiState.value.copy(
            showQrScanner = !_uiState.value.showQrScanner
        )
    }

    fun verifyQrToken(qrToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = resenaRepository.verifyQrToken(qrToken)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { response ->
                        if (response.autorizado) {
                            _uiState.value = _uiState.value.copy(
                                hasReviewAccess = true,
                                qrAccessMessage = response.mensaje,
                                showQrScanner = false
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                qrAccessMessage = "Acceso denegado"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            qrAccessMessage = error.message ?: "Error al verificar QR"
                        )
                    }
                )
            }
        }
    }

    fun clearQrMessage() {
        _uiState.value = _uiState.value.copy(qrAccessMessage = null)
    }

    fun submitReview(ratingAtencion: Int, ratingProducto: Int, ratingCosto: Int, comment: String) {
        val bId = businessId.toIntOrNull() ?: return
        val currentUserId = sessionManager.userId
        
        if (currentUserId == -1) {
            return
        }

        if (!_uiState.value.hasReviewAccess) {
            _uiState.value = _uiState.value.copy(
                qrAccessMessage = "Debes escanear el QR del local primero"
            )
            return
        }

        val calificacionGlobal = (ratingAtencion + ratingProducto + ratingCosto) / 3

        viewModelScope.launch(Dispatchers.IO) {
            // Obtener el qrToken del negocio para enviarlo en la reseña
            val qrTokenResult = negocioRepository.getQrToken(bId)
            val qrToken = qrTokenResult.getOrNull() ?: ""

            val request = CreateResenaRequest(
                idNegocio = bId,
                calificacion = calificacionGlobal,
                calidadAtencion = ratingAtencion,
                calidadProductos = ratingProducto,
                costos = ratingCosto,
                comentario = comment.ifBlank { null },
                qrToken = qrToken
            )
            
            val result = resenaRepository.createResena(request)
            
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(hasReviewAccess = false)
                loadBusinessData()
            } else {
                _uiState.value = _uiState.value.copy(
                    qrAccessMessage = result.exceptionOrNull()?.message ?: "Error al crear reseña"
                )
                loadBusinessData()
            }
        }
    }

    fun submitReport(motivo: String) {
        val bId = businessId.toIntOrNull() ?: return
        val currentUserId = sessionManager.userId
        
        if (currentUserId == -1 || currentUserId == -2) {
            // Invitados no pueden reportar
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Reportes no están implementados en el backend todavía.
            // Se puede omitir por ahora o dejar un TODO.
        }
    }

    fun createReservation(fecha: String, horaInicio: String, horaFin: String, onResult: (Boolean, String) -> Unit) {
        val bId = businessId.toIntOrNull()
        if (bId == null) {
            onResult(false, "ID de negocio inválido")
            return
        }
        val currentUserId = sessionManager.userId
        if (currentUserId == -1 || currentUserId == -2) {
            onResult(false, "Debes iniciar sesión para reservar")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = reservaRepository.createReserva(
                idNegocio = bId,
                fecha = fecha,
                horaInicio = horaInicio,
                horaFin = horaFin
            )
            
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { onResult(true, "Reserva solicitada correctamente") },
                    onFailure = { onResult(false, it.message ?: "Error al crear la reserva") }
                )
            }
        }
    }
}

class BusinessProfileViewModelFactory(
    private val application: Application,
    private val businessId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BusinessProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BusinessProfileViewModel(application, businessId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
