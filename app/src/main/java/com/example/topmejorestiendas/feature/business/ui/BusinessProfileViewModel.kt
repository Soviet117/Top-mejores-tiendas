package com.example.topmejorestiendas.feature.business.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.core.domain.mapper.toDomainModel
import com.example.topmejorestiendas.core.domain.model.Business
import com.example.topmejorestiendas.database.AppDatabase
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
    val error: String? = null
)

class BusinessProfileViewModel(
    application: Application,
    private val businessId: String
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val negocioDao = db.negocioDao()
    private val resenaDao = db.resenaDao()
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
                
                val negocio = negocioDao.obtenerPorId(bId)
                if (negocio != null) {
                    val resenas = resenaDao.obtenerPorNegocio(bId)
                    val mappedBusiness = negocio.toDomainModel(resenas)
                    
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            business = mappedBusiness,
                            reviews = resenas
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Negocio no encontrado"
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

    fun submitReview(ratingAtencion: Int, ratingProducto: Int, ratingCosto: Int, comment: String) {
        val bId = businessId.toIntOrNull() ?: return
        val currentUserId = sessionManager.userId
        
        if (currentUserId == -1) {
            // Manejar error de sesión no iniciada si es necesario
            return
        }

        val calificacionGlobal = (ratingAtencion + ratingProducto + ratingCosto) / 3

        viewModelScope.launch(Dispatchers.IO) {
            val newReview = Resena(
                currentUserId,
                bId,
                calificacionGlobal,
                ratingAtencion,
                ratingProducto,
                ratingCosto,
                comment,
                System.currentTimeMillis()
            )
            resenaDao.insertar(newReview)
            
            // Actualizar el promedio en el negocio
            val newAverage = resenaDao.obtenerPromedio(bId)
            val negocio = negocioDao.obtenerPorId(bId)
            if (negocio != null) {
                negocio.calificacionPromedio = newAverage
                negocioDao.actualizar(negocio)
            }

            // Recargar datos para refrescar la UI
            loadBusinessData()
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
