package com.example.topmejorestiendas.feature.home.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.topmejorestiendas.core.designsystem.components.BusinessCard
import com.example.topmejorestiendas.core.domain.model.Business

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBusiness: (String) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Mock Data
    val businesses = listOf(
        Business(
            id = "1",
            name = "Café de Especialidad 'El Grano'",
            description = "El mejor café de la ciudad, tostado artesanalmente.",
            imageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?q=80&w=600&auto=format&fit=crop",
            rating = 4.8,
            reviewCount = 124,
            category = "Cafetería",
            isVerified = true,
            isOpen = true,
            distanceText = "A 200m de ti"
        ),
        Business(
            id = "2",
            name = "Hamburguesas 'La Vaca Lola'",
            description = "Hamburguesas smash con ingredientes locales.",
            imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?q=80&w=600&auto=format&fit=crop",
            rating = 4.5,
            reviewCount = 89,
            category = "Restaurante",
            isVerified = false,
            isOpen = false,
            distanceText = "A 1.2km de ti"
        )
    )

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
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { /* TODO */ },
                    active = false,
                    onActiveChange = { },
                    placeholder = { Text("Buscar tiendas, cafés, etc.") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Contenido del buscador desplegado
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Categorías
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { CategoryChip("Ofertas", Icons.Default.LocalOffer) }
                item { CategoryChip("Restaurantes", Icons.Default.Fastfood) }
                item { CategoryChip("Cafeterías", Icons.Default.Coffee) }
                item { CategoryChip("Ropa", Icons.Default.ShoppingBag) }
            }

            // Feed de Tiendas
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(businesses) { business ->
                    BusinessCard(
                        business = business,
                        onClick = { onNavigateToBusiness(business.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FilterChip(
        selected = false,
        onClick = { },
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        }
    )
}
