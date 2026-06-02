package com.example.topmejorestiendas.feature.home.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.topmejorestiendas.core.domain.mapper.toDomainModel
import com.example.topmejorestiendas.core.domain.model.Business
import com.example.topmejorestiendas.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val isLoading: Boolean = true,
    val businesses: List<Business> = emptyList(),
    val categories: List<String> = listOf("Todo", "Cafetería", "Restaurante", "Ofertas", "Ropa"),
    val selectedCategory: String = "Todo",
    val searchQuery: String = "",
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val negocioDao = db.negocioDao()
    private val resenaDao = db.resenaDao()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        fetchBusinesses()
    }

    fun onCategorySelected(category: String) {
        if (_uiState.value.selectedCategory != category) {
            _uiState.value = _uiState.value.copy(selectedCategory = category, isLoading = true)
            fetchBusinesses()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        // Podríamos filtrar localmente aquí para que sea más rápido
        filterLocalList()
    }

    // Guardamos la lista original (sin filtro de búsqueda por nombre)
    private var originalList: List<Business> = emptyList()

    private fun fetchBusinesses() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val category = _uiState.value.selectedCategory
                val dbNegocios = if (category == "Todo" || category == "Ofertas") { // "Ofertas" no es un rubro real en la DB por ahora
                    negocioDao.obtenerTop(100)
                } else {
                    negocioDao.obtenerPorRubro(category)
                }

                // Mapear al modelo de UI
                val mappedBusinesses = dbNegocios.map { negocio ->
                    val reviewsCount = resenaDao.obtenerPorNegocio(negocio.id).size
                    negocio.toDomainModel(reviewsCount)
                }

                originalList = mappedBusinesses

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        businesses = originalList,
                        error = null
                    )
                    filterLocalList() // Aplicar búsqueda de texto si la hay
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error al cargar los negocios: ${e.message}"
                    )
                }
            }
        }
    }

    private fun filterLocalList() {
        val query = _uiState.value.searchQuery
        val filtered = if (query.isBlank()) {
            originalList
        } else {
            originalList.filter { it.name.contains(query, ignoreCase = true) }
        }
        _uiState.value = _uiState.value.copy(businesses = filtered)
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
