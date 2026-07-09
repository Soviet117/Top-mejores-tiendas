package com.example.topmejorestiendas.feature.dashboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAmbientesScreen(
    viewModel: ManageAmbientesViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAmbientes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Ambientes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.ambientes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(
                    text = "No hay ambientes registrados.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.ambientes) { ambiente ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ambiente.activo == false)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = ambiente.nombre,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ambiente.activo == false) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (ambiente.activo == false) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "En reposo",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Cantidad: ${ambiente.cantidad}  |  Capacidad: ${ambiente.capacidad} pers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { ambiente.id?.let { viewModel.toggleAmbiente(it, ambiente.activo ?: true) } },
                                enabled = ambiente.activo != false && ambiente.id != null
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Editar",
                                    tint = if (ambiente.activo != false) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                            IconButton(onClick = { ambiente.id?.let { viewModel.toggleAmbiente(it, ambiente.activo ?: true) } }) {
                                Icon(
                                    if (ambiente.activo != false) Icons.Filled.Remove else Icons.Filled.Check,
                                    contentDescription = if (ambiente.activo != false) "Desactivar" else "Activar",
                                    tint = if (ambiente.activo != false) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.showEditDialog && uiState.editingAmbiente != null) {
            val edit = uiState.editingAmbiente!!
            var editNombre by remember { mutableStateOf(edit.nombre) }
            var editCantidad by remember { mutableStateOf(edit.cantidad.toString()) }
            var editCapacidad by remember { mutableStateOf(edit.capacidad.toString()) }

            AlertDialog(
                onDismissRequest = { viewModel.cancelEditing() },
                title = { Text("Editar Ambiente") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editNombre,
                            onValueChange = { editNombre = it },
                            label = { Text("Nombre") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editCantidad,
                            onValueChange = { editCantidad = it },
                            label = { Text("Cantidad") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editCapacidad,
                            onValueChange = { editCapacidad = it },
                            label = { Text("Capacidad por unidad") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        edit.id?.let { id ->
                            viewModel.saveAmbiente(id, editNombre, editCantidad.toIntOrNull() ?: edit.cantidad, editCapacidad.toIntOrNull() ?: edit.capacidad)
                        }
                    }) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelEditing() }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
