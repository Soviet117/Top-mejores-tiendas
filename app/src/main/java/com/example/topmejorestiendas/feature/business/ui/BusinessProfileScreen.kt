package com.example.topmejorestiendas.feature.business.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.topmejorestiendas.feature.common.ui.OsmMap
import com.example.topmejorestiendas.model.Resena
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    viewModel: BusinessProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRatingDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    val isEditing = uiState.userReview != null

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val business = uiState.business

    if (business == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(uiState.error ?: "Negocio no encontrado", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onNavigateBack, modifier = Modifier.padding(top = 16.dp)) {
                Text("Regresar")
            }
        }
        return
    }

    Scaffold(
        floatingActionButton = {
            if (!uiState.isGuest) {
                ExtendedFloatingActionButton(
                    onClick = { showRatingDialog = true },
                    icon = { Icon(Icons.Filled.Star, contentDescription = if (isEditing) "Editar Reseña" else "Calificar") },
                    text = { Text(if (isEditing) "Editar Reseña" else "Escribir Reseña") }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Image
            item {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)) {
                    AsyncImage(
                        model = business.imageUrl,
                        contentDescription = business.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                )
                            )
                    )

                    // Back Button
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            .align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = Color.White
                        )
                    }

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (business.isOpen) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (business.isOpen) "ABIERTO" else "CERRADO",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Info Section
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = business.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (business.isVerified) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verificado",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        if (!uiState.isGuest) {
                            IconButton(onClick = { showReportDialog = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Reportar",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Global Rating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(Locale.US, "%.1f", business.rating),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row {
                                repeat(5) { index ->
                                    Icon(
                                        imageVector = if (index < business.rating.toInt()) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${business.reviewCount} reseñas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detailed Ratings
                    if (business.reviewCount > 0) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            RatingProgressBar("Atención", business.ratingAtencion)
                            RatingProgressBar("Producto", business.ratingProducto)
                            RatingProgressBar("Costos", business.ratingCosto)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = business.description,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    if (business.prices.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        PricesTable(pricesString = business.prices)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Ubicación: ${business.distanceText}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (business.latitude != 0.0 || business.longitude != 0.0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            OsmMap(
                                modifier = Modifier.fillMaxSize(),
                                latitude = business.latitude,
                                longitude = business.longitude,
                                isEditMode = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Reseñas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Reviews List
            if (uiState.reviews.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Aún no hay reseñas. ¡Sé el primero!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(uiState.reviews) { review ->
                    ReviewCard(review = review)
                }
            }
            
            // Spacer for FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        if (showRatingDialog) {
            val existing = uiState.userReview
            RatingDialog(
                onDismiss = { showRatingDialog = false },
                initialAtencion = existing?.calidadAtencion ?: 0,
                initialProducto = existing?.calidadProductos ?: 0,
                initialCosto = existing?.costos ?: 0,
                initialComment = existing?.comentario ?: "",
                onSubmit = { ratingAtencion, ratingProducto, ratingCosto, comment ->
                    viewModel.submitReview(ratingAtencion, ratingProducto, ratingCosto, comment)
                    showRatingDialog = false
                }
            )
        }

        if (showReportDialog) {
            ReportDialog(
                onDismiss = { showReportDialog = false },
                onSubmit = { reason ->
                    viewModel.submitReport(reason)
                    showReportDialog = false
                }
            )
        }
    }
}

@Composable
fun ReviewCard(review: Resena) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "U", // Podríamos cargar el nombre del usuario real si unimos la tabla
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Usuario ${review.idUsuario}", fontWeight = FontWeight.Bold)
                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    Text(
                        text = dateFormat.format(Date(review.fecha)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < review.calificacion) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = review.comentario, style = MaterialTheme.typography.bodyMedium)

            if (!review.respuestaDuenio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Respuesta del dueño:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = review.respuestaDuenio!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingDialog(
    onDismiss: () -> Unit,
    initialAtencion: Int = 0,
    initialProducto: Int = 0,
    initialCosto: Int = 0,
    initialComment: String = "",
    onSubmit: (ratingAtencion: Int, ratingProducto: Int, ratingCosto: Int, comment: String) -> Unit
) {
    var ratingAtencion by remember { mutableIntStateOf(initialAtencion) }
    var ratingProducto by remember { mutableIntStateOf(initialProducto) }
    var ratingCosto by remember { mutableIntStateOf(initialCosto) }
    var comment by remember { mutableStateOf(initialComment) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialAtencion > 0 || initialProducto > 0 || initialCosto > 0) "Editar Reseña" else "Calificar negocio") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RatingRow(label = "Atención", currentRating = ratingAtencion, onRatingChange = { ratingAtencion = it })
                RatingRow(label = "Producto", currentRating = ratingProducto, onRatingChange = { ratingProducto = it })
                RatingRow(label = "Costos", currentRating = ratingCosto, onRatingChange = { ratingCosto = it })

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Escribe tu opinión") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(ratingAtencion, ratingProducto, ratingCosto, comment) },
                enabled = ratingAtencion > 0 && ratingProducto > 0 && ratingCosto > 0
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun RatingRow(label: String, currentRating: Int, onRatingChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Row {
            repeat(5) { index ->
                IconButton(
                    onClick = { onRatingChange(index + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (index < currentRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Estrella ${index + 1}",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RatingProgressBar(label: String, rating: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.3f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        LinearProgressIndicator(
            progress = { (rating / 5.0).toFloat() },
            modifier = Modifier
                .weight(0.6f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(0xFFFFB300),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = String.format(Locale.US, "%.1f", rating),
            modifier = Modifier.weight(0.1f).padding(start = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PricesTable(pricesString: String) {
    val priceEntries = pricesString.split(",").mapNotNull { entry ->
        val parts = entry.trim().split(":")
        if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
    }
    if (priceEntries.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Precios",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Concepto",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Precio",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
                priceEntries.forEach { (concept, price) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = concept,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "S/ $price",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("") }
    val options = listOf("El local ya no existe", "Información falsa o engañosa", "Contenido inapropiado", "Otro")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reportar Local", color = MaterialTheme.colorScheme.error) },
        text = {
            Column {
                Text("Por favor, selecciona el motivo de tu reporte:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { selectedReason = option }
                    ) {
                        RadioButton(
                            selected = (option == selectedReason),
                            onClick = { selectedReason = option }
                        )
                        Text(text = option, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason) },
                enabled = selectedReason.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Enviar Reporte")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
