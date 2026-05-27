package com.example.topmejorestiendas.feature.business.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
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
import com.example.topmejorestiendas.core.domain.model.Business
import java.text.SimpleDateFormat
import java.util.*

// Modelo de datos temporal para reseñas
data class MockReview(
    val id: String = UUID.randomUUID().toString(),
    val userName: String,
    val rating: Int,
    val comment: String,
    val date: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    businessId: String?,
    onNavigateBack: () -> Unit
) {
    // Obtener datos mockeados basados en el ID
    val business = getMockBusiness(businessId)

    if (business == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Negocio no encontrado", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onNavigateBack, modifier = Modifier.padding(top = 16.dp)) {
                Text("Regresar")
            }
        }
        return
    }

    // Estado local para las reseñas
    var reviews by remember { mutableStateOf(getMockReviews(businessId)) }
    var showRatingDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showRatingDialog = true },
                icon = { Icon(Icons.Filled.Star, contentDescription = "Calificar") },
                text = { Text("Escribir Reseña") }
            )
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
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Calculate dynamic rating
                        val currentRating = if (reviews.isEmpty()) business.rating else reviews.map { it.rating }.average()
                        
                        Text(
                            text = String.format(Locale.US, "%.1f", currentRating),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " (${reviews.size} reseñas)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "• ${business.category}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = business.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Ubicación: ${business.distanceText}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

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
            if (reviews.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Aún no hay reseñas. ¡Sé el primero!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(reviews) { review ->
                    ReviewCard(review = review)
                }
            }
            
            // Spacer for FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        if (showRatingDialog) {
            RatingDialog(
                onDismiss = { showRatingDialog = false },
                onSubmit = { rating, comment ->
                    val newReview = MockReview(
                        userName = "Usuario Nuevo",
                        rating = rating,
                        comment = comment
                    )
                    reviews = listOf(newReview) + reviews
                    showRatingDialog = false
                }
            )
        }
    }
}

@Composable
fun ReviewCard(review: MockReview) {
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
                        text = review.userName.first().toString().uppercase(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = review.userName, fontWeight = FontWeight.Bold)
                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    Text(
                        text = dateFormat.format(Date(review.date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < review.rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = review.comment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun RatingDialog(
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String) -> Unit
) {
    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calificar negocio") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    repeat(5) { index ->
                        IconButton(onClick = { rating = index + 1 }) {
                            Icon(
                                imageVector = if (index < rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Estrella ${index + 1}",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
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
                onClick = { onSubmit(rating, comment) },
                enabled = rating > 0
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

// Datos Mock
fun getMockBusiness(id: String?): Business? {
    val businesses = listOf(
        Business(
            id = "1",
            name = "Café de Especialidad 'El Grano'",
            description = "El mejor café de la ciudad, tostado artesanalmente. Disfruta de un ambiente acogedor, WiFi rápido y la mejor pastelería de la región.",
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
            description = "Hamburguesas smash con ingredientes locales. Preparadas al momento con pan artesanal y nuestras salsas secretas.",
            imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?q=80&w=600&auto=format&fit=crop",
            rating = 4.5,
            reviewCount = 89,
            category = "Restaurante",
            isVerified = false,
            isOpen = false,
            distanceText = "A 1.2km de ti"
        )
    )
    return businesses.find { it.id == id }
}

fun getMockReviews(businessId: String?): List<MockReview> {
    if (businessId == "1") {
        return listOf(
            MockReview(userName = "Ana García", rating = 5, comment = "Excelente café y muy buen ambiente para trabajar."),
            MockReview(userName = "Carlos Ruiz", rating = 4, comment = "Muy rico todo, aunque un poco ruidoso en las tardes.")
        )
    }
    if (businessId == "2") {
        return listOf(
            MockReview(userName = "Luis Fernando", rating = 5, comment = "Las mejores hamburguesas de la ciudad, punto.")
        )
    }
    return emptyList()
}
