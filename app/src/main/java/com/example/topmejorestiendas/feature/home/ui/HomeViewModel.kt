package com.example.topmejorestiendas.feature.home.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.core.domain.mapper.toDomainModel
import com.example.topmejorestiendas.core.domain.model.Business
import com.example.topmejorestiendas.data.repository.NegocioRepository
import com.example.topmejorestiendas.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val isLoading: Boolean = true,
    val businesses: List<Business> = emptyList(),
    val categories: List<String> = listOf("Todo", "Restaurantes", "Canchas Sintéticas", "Piscinas", "Cafeterías", "Gimnasios", "Tiendas de Ropa", "Farmacias", "Supermercados", "Otros"),
    val selectedCategory: String = "Todo",
    val searchQuery: String = "",
    val selectedSortOption: String = "Destacados",
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val negocioRepository = NegocioRepository(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        fetchBusinesses()
    }

    fun onCategorySelected(category: String) {
        if (_uiState.value.selectedCategory != category) {
            _uiState.value = _uiState.value.copy(
                selectedCategory = category, 
                selectedSortOption = "Destacados",
                isLoading = true
            )
            fetchBusinesses()
        }
    }

    fun onSortOptionSelected(option: String) {
        if (_uiState.value.selectedSortOption != option) {
            _uiState.value = _uiState.value.copy(selectedSortOption = option)
            applySortingAndFiltering()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applySortingAndFiltering()
    }

    fun toggleFavorite(businessId: String) {
        sessionManager.toggleFavorite(businessId)
        fetchBusinesses() // Recargar para aplicar orden y estado
    }

    // Guardamos la lista original (sin filtro de búsqueda por nombre)
    private var originalList: List<Business> = emptyList()

    private fun fetchBusinesses() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val category = _uiState.value.selectedCategory
                
                // Mapear la categoría para la API
                val apiCategory = if (category == "Todo" || category == "Ofertas") null else category
                
                // Obtener negocios desde el backend
                val result = negocioRepository.getNegocios(rubro = apiCategory)

                if (result.isSuccess) {
                    val dtos = result.getOrNull() ?: emptyList()
                    val favorites = sessionManager.favorites

                    // Mapear al modelo de UI e inyectar favoritos
                    val mappedBusinesses = dtos.map { dto ->
                        val business = dto.toDomainModel()
                        business.copy(isFavorite = favorites.contains(business.id))
                    }.sortedByDescending { it.isFavorite }

                    originalList = mappedBusinesses

                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            businesses = originalList,
                            error = null
                        )
                        applySortingAndFiltering() // Aplicar búsqueda de texto y ordenación
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Error al cargar negocios"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.localizedMessage
                    )
                }
            }
        }
    }

    private fun applySortingAndFiltering() {
        val query = _uiState.value.searchQuery
        val sortOption = _uiState.value.selectedSortOption

        var list = if (query.isBlank()) {
            originalList
        } else {
            originalList.filter { it.name.contains(query, ignoreCase = true) }
        }

        list = when (sortOption) {
            "Costo" -> list.sortedByDescending { it.ratingCosto }
            "Atención" -> list.sortedByDescending { it.ratingAtencion }
            "Producto" -> list.sortedByDescending { it.ratingProducto }
            else -> list.sortedByDescending { it.isFavorite }
        }

        _uiState.value = _uiState.value.copy(businesses = list)
    }
}

class HomeViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
