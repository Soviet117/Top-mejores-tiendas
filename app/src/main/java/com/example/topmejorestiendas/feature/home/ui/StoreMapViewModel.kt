package com.example.topmejorestiendas.feature.home.ui

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.core.domain.mapper.toDomainModel
import com.example.topmejorestiendas.core.domain.model.Business
import com.example.topmejorestiendas.data.repository.NegocioRepository
import com.example.topmejorestiendas.utils.SessionManager
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StoreMapUiState(
    val isLoading: Boolean = true,
    val businesses: List<Business> = emptyList(),
    val userLat: Double = 0.0,
    val userLng: Double = 0.0,
    val error: String? = null,
    val profilePhotoUrl: String = ""
)

class StoreMapViewModel(application: Application) : AndroidViewModel(application) {
    private val negocioRepository = NegocioRepository(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(StoreMapUiState())
    val uiState: StateFlow<StoreMapUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            profilePhotoUrl = sessionManager.getProfilePhoto()
        )
        loadBusinesses()
        getUserLocation()
    }

    private fun loadBusinesses() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = negocioRepository.getNegocios()
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { dtos ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            businesses = dtos.map { it.toDomainModel() }
                        )
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
                    }
                )
            }
        }
    }

    private fun getUserLocation() {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                _uiState.value = _uiState.value.copy(
                    userLat = location.latitude,
                    userLng = location.longitude
                )
            }
        }
    }
}

class StoreMapViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoreMapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoreMapViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
