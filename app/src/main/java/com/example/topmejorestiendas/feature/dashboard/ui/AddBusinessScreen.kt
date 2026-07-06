package com.example.topmejorestiendas.feature.dashboard.ui

import android.net.Uri
import android.Manifest
import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.topmejorestiendas.feature.common.ui.OsmMap
import com.example.topmejorestiendas.feature.common.ui.OsmMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBusinessScreen(
    viewModel: AddBusinessViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    
    val scheduleState = remember {
        listOf(
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
        ).map { day ->
            ScheduleDayState(
                name = day,
                isOpen = mutableStateOf(day != "Domingo"), // Domingo cerrado por defecto
                openTime = mutableStateOf("08:00"),
                closeTime = mutableStateOf("18:00")
            )
        }
    }

    var description by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val hasDefaultPricing = category == "Piscinas" || category == "Canchas Sintéticas"
    var enablePricing by remember { mutableStateOf(false) }
    LaunchedEffect(category) { enablePricing = false }
    val showPricing = hasDefaultPricing || (!hasDefaultPricing && enablePricing)
    val priceEntries = remember { mutableStateListOf<PriceEntry>() }
    LaunchedEffect(showPricing) {
        if (showPricing && priceEntries.isEmpty()) {
            when (category) {
                "Piscinas" -> {
                    priceEntries.add(PriceEntry("Adulto", ""))
                    priceEntries.add(PriceEntry("Niños", ""))
                }
                "Canchas Sintéticas" -> {
                    priceEntries.add(PriceEntry("Hora", ""))
                    priceEntries.add(PriceEntry("Media hora", ""))
                }
            }
        } else if (!showPricing) {
            priceEntries.clear()
        }
    }

    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            photoUri = result.uriContent
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            cropImageLauncher.launch(
                CropImageContractOptions(it, CropImageOptions(
                    aspectRatioX = 1,
                    aspectRatioY = 1,
                    fixAspectRatio = true
                ))
            )
        }
    }

    val context = LocalContext.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Local") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Selector de Imagen del Local
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Foto del local",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Añadir foto principal",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Toca para añadir una portada", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; viewModel.clearError() },
                label = { Text("Nombre del Local") },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            val dbCategories by viewModel.categorias.collectAsState()
            var expandedCategory by remember { mutableStateOf(false) }
            var showNewCategoryDialog by remember { mutableStateOf(false) }
            var newCategoryName by remember { mutableStateOf("") }

            LaunchedEffect(Unit) { viewModel.loadCategorias() }

            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    dbCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.nombre) },
                            onClick = {
                                category = cat.nombre
                                viewModel.clearError()
                                expandedCategory = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("➕ Agregar nueva categoría...") },
                        onClick = {
                            expandedCategory = false
                            showNewCategoryDialog = true
                        }
                    )
                }
            }

            if (showNewCategoryDialog) {
                AlertDialog(
                    onDismissRequest = { showNewCategoryDialog = false },
                    title = { Text("Nueva Categoría") },
                    text = {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("Nombre de la categoría") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val name = newCategoryName.trim()
                            if (name.isNotBlank()) {
                                viewModel.createCategoria(name) { success ->
                                    if (success) {
                                        category = name
                                        newCategoryName = ""
                                        showNewCategoryDialog = false
                                    }
                                }
                            }
                        }) { Text("Crear") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewCategoryDialog = false; newCategoryName = "" }) { Text("Cancelar") }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it; viewModel.clearError() },
                label = { Text("Dirección") },
                leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Ubicación en el Mapa",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (latitude != 0.0 || longitude != 0.0) {
                    IconButton(onClick = { 
                        latitude = 0.0
                        longitude = 0.0
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Borrar ubicación",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                OsmMap(
                    modifier = Modifier.fillMaxSize(),
                    latitude = latitude,
                    longitude = longitude,
                    isEditMode = true,
                    onLocationChanged = { lat, lon ->
                        latitude = lat
                        longitude = lon
                        try {
                            val geocoder = Geocoder(context, java.util.Locale.getDefault())
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(lat, lon, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val addr = addresses[0]
                                val parts = mutableListOf<String>()
                                if (!addr.thoroughfare.isNullOrBlank()) parts.add(addr.thoroughfare)
                                if (!addr.subThoroughfare.isNullOrBlank()) parts.add(addr.subThoroughfare)
                                if (!addr.locality.isNullOrBlank()) parts.add(addr.locality)
                                if (!addr.subAdminArea.isNullOrBlank()) parts.add(addr.subAdminArea)
                                if (parts.isNotEmpty()) {
                                    address = parts.joinToString(", ")
                                }
                            }
                        } catch (_: Exception) { }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Horario de Atención",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    scheduleState.forEach { dayState ->
                        ScheduleRow(dayState)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it; viewModel.clearError() },
                label = { Text("Descripción del Negocio") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            if (!hasDefaultPricing && category.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Agregar tarifario",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = enablePricing,
                        onCheckedChange = { enablePricing = it }
                    )
                }
            }

            if (showPricing) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Precios",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        priceEntries.forEachIndexed { index, entry ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                OutlinedTextField(
                                    value = entry.concept,
                                    onValueChange = { priceEntries[index] = entry.copy(concept = it) },
                                    label = { Text("Concepto") },
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = entry.price,
                                    onValueChange = { priceEntries[index] = entry.copy(price = it) },
                                    label = { Text("Precio (S/)") },
                                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                                    singleLine = true
                                )
                                if (priceEntries.size > 1) {
                                    IconButton(onClick = { priceEntries.removeAt(index) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { priceEntries.add(PriceEntry("", "")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Agregar otra tarifa")
                        }
                    }
                }
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val finalSchedule = scheduleState.joinToString(", ") { state ->
                        if (state.isOpen.value) {
                            "${state.name.take(3)}: ${state.openTime.value}-${state.closeTime.value}"
                        } else {
                            "${state.name.take(3)}: Cerrado"
                        }
                    }
                    val finalPrices = priceEntries
                        .filter { it.concept.isNotBlank() && it.price.isNotBlank() }
                        .joinToString(", ") { "${it.concept}: ${it.price}" }
                    viewModel.registerBusiness(
                        name, category, address, finalSchedule, description, photoUri?.toString() ?: "", latitude, longitude, finalPrices
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Guardar Local")
                }
            }
        }
    }
}

data class ScheduleDayState(
    val name: String,
    val isOpen: MutableState<Boolean>,
    val openTime: MutableState<String>,
    val closeTime: MutableState<String>
)

data class PriceEntry(
    val concept: String,
    val price: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleRow(state: ScheduleDayState) {
    var showOpenPicker by remember { mutableStateOf(false) }
    var showClosePicker by remember { mutableStateOf(false) }

    if (showOpenPicker) {
        val timeState = rememberTimePickerState(
            initialHour = state.openTime.value.substringBefore(":").toIntOrNull() ?: 8,
            initialMinute = state.openTime.value.substringAfter(":").toIntOrNull() ?: 0,
            is24Hour = true
        )
        TimePickerDialog(
            onDismiss = { showOpenPicker = false },
            onConfirm = {
                state.openTime.value = String.format("%02d:%02d", timeState.hour, timeState.minute)
                showOpenPicker = false
            },
            title = "Hora de apertura"
        ) {
            TimePicker(state = timeState)
        }
    }

    if (showClosePicker) {
        val timeState = rememberTimePickerState(
            initialHour = state.closeTime.value.substringBefore(":").toIntOrNull() ?: 18,
            initialMinute = state.closeTime.value.substringAfter(":").toIntOrNull() ?: 0,
            is24Hour = true
        )
        TimePickerDialog(
            onDismiss = { showClosePicker = false },
            onConfirm = {
                state.closeTime.value = String.format("%02d:%02d", timeState.hour, timeState.minute)
                showClosePicker = false
            },
            title = "Hora de cierre"
        ) {
            TimePicker(state = timeState)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Switch(
            checked = state.isOpen.value,
            onCheckedChange = { state.isOpen.value = it }
        )
        Text(
            text = state.name.take(3), // Lun, Mar, Mie...
            modifier = Modifier.width(48.dp).padding(start = 8.dp),
            fontWeight = FontWeight.Bold
        )
        
        if (state.isOpen.value) {
            OutlinedTextField(
                value = state.openTime.value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .clickable { showOpenPicker = true },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Text("-")
            OutlinedTextField(
                value = state.closeTime.value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .clickable { showClosePicker = true },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        } else {
            Text(
                text = "Cerrado",
                modifier = Modifier.weight(2f).padding(start = 16.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
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
