package com.example.topmejorestiendas.feature.home.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import com.example.topmejorestiendas.core.designsystem.components.BusinessCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToBusiness: (String) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Top Tiendas") },
                    actions = {
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Mi Perfil")
                        }
                        IconButton(onClick = { /* TODO: Toggle Map View */ }) {
                            Icon(Icons.Default.Map, contentDescription = "Ver Mapa")
                        }
                    }
                )
                
                // Nuevo SearchBar implementado para Material 3
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Buscar tiendas, cafés, etc.") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Crossfade(targetState = uiState.isLoading, label = "loading_crossfade") { isLoading ->
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.selectedCategory == "Todo" && uiState.searchQuery.isBlank()) {
                    // Vista Principal: Top 3 + Categorías
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Top 3 Section
                        item {
                            Text(
                                text = "Top 3 de la semana",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            val top3 = uiState.businesses.take(3)
                            if (top3.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(end = 16.dp)
                                ) {
                                    items(top3) { business ->
                                        Box(modifier = Modifier.width(280.dp)) {
                                            BusinessCard(
                                                business = business,
                                                onClick = { onNavigateToBusiness(business.id) },
                                                onToggleFavorite = { viewModel.toggleFavorite(business.id) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text("No hay negocios top por ahora", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Explorar por Categorías
                        item {
                            Text(
                                text = "Explorar por categorías",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            val displayCategories = uiState.categories.filter { it != "Todo" }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.heightIn(max = 600.dp) // Para permitir el scroll en la columna padre sin conflictos graves
                            ) {
                                items(displayCategories) { category ->
                                    CategoryGridCard(
                                        categoryName = category,
                                        onClick = { viewModel.onCategorySelected(category) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Vista de Resultados (Lista con botón para volver)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            if (uiState.selectedCategory != "Todo") {
                                FilterChip(
                                    selected = true,
                                    onClick = { viewModel.onCategorySelected("Todo") },
                                    label = { Text("${uiState.selectedCategory} ✕") }
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("${uiState.businesses.size} resultados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (uiState.selectedCategory != "Todo") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Rankear por:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    item {
                                        FilterChip(
                                            selected = uiState.selectedSortOption == "Destacados",
                                            onClick = { viewModel.onSortOptionSelected("Destacados") },
                                            label = { Text("Destacados") }
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = uiState.selectedSortOption == "Costo",
                                            onClick = { viewModel.onSortOptionSelected("Costo") },
                                            label = { Text("Costo") }
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = uiState.selectedSortOption == "Atención",
                                            onClick = { viewModel.onSortOptionSelected("Atención") },
                                            label = { Text("Atención") }
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = uiState.selectedSortOption == "Producto",
                                            onClick = { viewModel.onSortOptionSelected("Producto") },
                                            label = { Text("Producto") }
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.businesses.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No se encontraron negocios",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.businesses) { business ->
                                    BusinessCard(
                                        business = business,
                                        onClick = { onNavigateToBusiness(business.id) },
                                        onToggleFavorite = { viewModel.toggleFavorite(business.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryGridCard(categoryName: String, onClick: () -> Unit) {
    val icon = when (categoryName) {
        "Restaurantes" -> Icons.Default.Fastfood
        "Canchas Sintéticas" -> Icons.Default.SportsSoccer
        "Piscinas" -> Icons.Default.Pool
        "Cafeterías" -> Icons.Default.Coffee
        "Gimnasios" -> Icons.Default.FitnessCenter
        "Tiendas de Ropa" -> Icons.Default.ShoppingBag
        "Farmacias" -> Icons.Default.LocalPharmacy
        "Supermercados" -> Icons.Default.LocalGroceryStore
        else -> Icons.Default.Category
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().aspectRatio(1.5f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
