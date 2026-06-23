package com.example.topmejorestiendas.feature.dashboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.topmejorestiendas.core.domain.model.Business
import com.example.topmejorestiendas.core.designsystem.components.BusinessCard

import androidx.compose.material.icons.filled.Email

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardScreen(
    viewModel: OwnerDashboardViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToAddBusiness: () -> Unit,
    onNavigateToBusinessDetail: (String) -> Unit,
    onNavigateToInbox: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Recargar locales cuando la pantalla gane foco
    LaunchedEffect(Unit) {
        viewModel.loadBusinesses()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Negocios") },
                actions = {
                    IconButton(onClick = onNavigateToInbox) {
                        Icon(Icons.Default.Email, contentDescription = "Bandeja de Reservas")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Mi Perfil")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddBusiness) {
                Icon(Icons.Default.Add, contentDescription = "Registrar Local")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.businesses.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Aún no has registrado ningún local",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Pulsa el botón '+' para añadir tu primer negocio a la plataforma.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.businesses) { negocio ->
                        // Map Room Entity to Domain Model for BusinessCard
                        val businessDomain = Business(
                            id = negocio.id.toString(),
                            name = negocio.nombreNegocio ?: "Sin Nombre",
                            description = negocio.descripcion ?: "",
                            imageUrl = negocio.fotoNegocio ?: "",
                            rating = negocio.calificacionPromedio.toDouble(),
                            reviewCount = 0, // Mocked for now
                            category = negocio.rubro ?: "General",
                            isVerified = true,
                            isOpen = true, // We can calculate this based on schedule if needed
                            distanceText = negocio.direccion ?: ""
                        )
                        BusinessCard(
                            business = businessDomain,
                            onClick = { onNavigateToBusinessDetail(negocio.id.toString()) }
                        )
                    }
                }
            }
        }
    }
}
