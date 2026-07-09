package com.example.topmejorestiendas.feature.business.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.topmejorestiendas.feature.common.ui.OsmMap
import com.example.topmejorestiendas.model.Resena
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch


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
    val context = LocalContext.current

    // Scanner launcher
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        if (result.contents != null) {
            viewModel.verifyQrToken(result.contents)
        }
    }

    // Permission launcher for camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val options = ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Escanea el QR del local")
                .setCameraId(0)
                .setBeepEnabled(false)
                .setOrientationLocked(true)
            scannerLauncher.launch(options)
        } else {
            Toast.makeText(context, "Permiso de cámara necesario para escanear QR", Toast.LENGTH_SHORT).show()
        }
    }

    // Show QR access messages
    LaunchedEffect(uiState.qrAccessMessage) {
        uiState.qrAccessMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearQrMessage()
        }
    }

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

    var showReservationDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isGuest) {
                Column(horizontalAlignment = Alignment.End) {
                    ExtendedFloatingActionButton(
                        onClick = { showReservationDialog = true },
                        icon = { Icon(Icons.Filled.Event, contentDescription = "Reservar") },
                        text = { Text("Reservar Horario") },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    if (!uiState.hasReviewAccess) {
                        // Show QR scanner button when no access
                        ExtendedFloatingActionButton(
                            onClick = {
                                val hasCameraPermission = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasCameraPermission) {
                                    val options = ScanOptions()
                                        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                        .setPrompt("Escanea el QR del local")
                                        .setCameraId(0)
                                        .setBeepEnabled(false)
                                        .setOrientationLocked(true)
                                    scannerLauncher.launch(options)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear QR") },
                            text = { Text("Escanear QR para Reseñar") },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else {
                        // Show review button when access is granted
                        ExtendedFloatingActionButton(
                            onClick = { showRatingDialog = true },
                            icon = { Icon(Icons.Filled.Star, contentDescription = if (isEditing) "Editar Reseña" else "Calificar") },
                            text = { Text(if (isEditing) "Editar Reseña" else "Escribir Reseña") }
                        )
                    }
                }
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
                            Button(
                                onClick = {
                                    val gmmIntentUri = Uri.parse("google.navigation:q=${business.latitude},${business.longitude}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    } else {
                                        val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${business.latitude},${business.longitude}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Cómo llegar", style = MaterialTheme.typography.labelMedium)
                            }
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

        if (showReservationDialog) {
            ReservationDialog(
                onDismiss = { showReservationDialog = false },
                onSubmit = { fecha, horaInicio, horaFin, personas ->
                    viewModel.createReservation(fecha, horaInicio, horaFin, personas) { success, message ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = message,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                    showReservationDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Int) -> Unit
) {
    // ── Estado interno ────────────────────────────────────────────
    val calendar = remember { Calendar.getInstance() }

    // DatePicker state — default a hoy
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis
    )
    var showDatePicker by remember { mutableStateOf(false) }

    // TimePicker estados — inicio y fin
    val timePickerStateInicio = rememberTimePickerState(
        initialHour = 8,
        initialMinute = 0,
        is24Hour = true
    )
    val timePickerStateFin = rememberTimePickerState(
        initialHour = 9,
        initialMinute = 0,
        is24Hour = true
    )
    var showTimePickerInicio by remember { mutableStateOf(false) }
    var showTimePickerFin by remember { mutableStateOf(false) }

    // Cantidad de personas
    var personas by remember { mutableStateOf("1") }

    // ── Formateo de valores legibles ──────────────────────────────
    val fechaDisplay = remember(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
        } ?: "Seleccionar fecha"
    }
    val fechaApi = remember(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            // Ajustar a medianoche UTC para evitar desfase por zona horaria
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.timeInMillis = it
            String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        } ?: ""
    }
    val horaInicioDisplay = remember(timePickerStateInicio.hour, timePickerStateInicio.minute) {
        String.format("%02d:%02d", timePickerStateInicio.hour, timePickerStateInicio.minute)
    }
    val horaFinDisplay = remember(timePickerStateFin.hour, timePickerStateFin.minute) {
        String.format("%02d:%02d", timePickerStateFin.hour, timePickerStateFin.minute)
    }

    val isValid = fechaApi.isNotBlank() &&
            (timePickerStateFin.hour > timePickerStateInicio.hour ||
                    (timePickerStateFin.hour == timePickerStateInicio.hour && timePickerStateFin.minute > timePickerStateInicio.minute))

    // ── Diálogos nativos ──────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePickerInicio) {
        TimePickerDialog(
            onDismiss = { showTimePickerInicio = false },
            onConfirm = { showTimePickerInicio = false },
            title = "Hora de inicio"
        ) {
            TimePicker(state = timePickerStateInicio)
        }
    }

    if (showTimePickerFin) {
        TimePickerDialog(
            onDismiss = { showTimePickerFin = false },
            onConfirm = { showTimePickerFin = false },
            title = "Hora de fin"
        ) {
            TimePicker(state = timePickerStateFin)
        }
    }

    // ── Diálogo principal ─────────────────────────────────────────
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reservar Horario", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Selecciona la fecha y el horario que deseas reservar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Selector de Fecha
                OutlinedTextField(
                    value = fechaDisplay,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha") },
                    leadingIcon = {
                        Icon(Icons.Filled.Event, contentDescription = "Fecha", tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                    )
                )

                // Selectors de Hora en fila
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = horaInicioDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Inicio") },
                        leadingIcon = {
                            Icon(Icons.Filled.Schedule, contentDescription = "Hora inicio", tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTimePickerInicio = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                    OutlinedTextField(
                        value = horaFinDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fin") },
                        leadingIcon = {
                            Icon(Icons.Filled.Schedule, contentDescription = "Hora fin", tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTimePickerFin = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                }

                // Cantidad de personas
                OutlinedTextField(
                    value = personas,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) personas = it },
                    label = { Text("Reserva para") },
                    leadingIcon = {
                        Icon(Icons.Filled.Person, contentDescription = "Personas", tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                if (!isValid && fechaApi.isNotBlank()) {
                    Text(
                        "La hora de fin debe ser posterior a la hora de inicio.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(fechaApi, horaInicioDisplay, horaFinDisplay, personas.toIntOrNull() ?: 1) },
                enabled = isValid && (personas.toIntOrNull() ?: 0) > 0
            ) {
                Text("Solicitar Reserva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Helper: envoltorio de diálogo para el TimePicker ─────────────
@Composable
fun TimePickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title)
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                content()
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
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
                    Text(text = review.nombreUsuario ?: "Usuario ${review.idUsuario}", fontWeight = FontWeight.Bold)
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
