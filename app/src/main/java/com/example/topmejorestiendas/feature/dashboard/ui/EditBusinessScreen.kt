package com.example.topmejorestiendas.feature.dashboard.ui

import android.net.Uri
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.topmejorestiendas.feature.common.ui.OsmMap
import androidx.compose.material.icons.filled.Add

private fun parseScheduleString(scheduleStr: String?): List<ScheduleDayState> {
    val defaultDays = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    if (scheduleStr.isNullOrBlank()) {
        return defaultDays.map { day ->
            ScheduleDayState(
                name = day,
                isOpen = mutableStateOf(day != "Domingo"),
                openTime = mutableStateOf("08:00"),
                closeTime = mutableStateOf("18:00")
            )
        }
    }

    val parsedMap = scheduleStr.split(",").associate { chunk ->
        val parts = chunk.split(":")
        val dayKey = parts.getOrNull(0)?.trim() ?: ""
        val timeValue = parts.drop(1).joinToString(":").trim()
        dayKey to timeValue
    }

    return defaultDays.map { day ->
        val shortDay = day.take(3)
        val timeStr = parsedMap[shortDay]

        val isOpen = timeStr != null && timeStr != "Cerrado"
        var openT = "08:00"
        var closeT = "18:00"

        if (isOpen && timeStr != null && timeStr.contains("-")) {
            val tParts = timeStr.split("-")
            openT = tParts.getOrNull(0)?.trim() ?: "08:00"
            closeT = tParts.getOrNull(1)?.trim() ?: "18:00"
        }

        ScheduleDayState(
            name = day,
            isOpen = mutableStateOf(isOpen),
            openTime = mutableStateOf(openT),
            closeTime = mutableStateOf(closeT)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBusinessScreen(
    businessId: String,
    viewModel: EditBusinessViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(businessId) {
        businessId.toIntOrNull()?.let {
            viewModel.loadBusiness(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Información") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is EditBusinessUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is EditBusinessUiState.Error -> {
                    Text(
                        text = (uiState as EditBusinessUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is EditBusinessUiState.Success -> {
                    LaunchedEffect(Unit) { onNavigateBack() }
                }
                is EditBusinessUiState.Loaded -> {
                    val negocio = (uiState as EditBusinessUiState.Loaded).negocio
                    
                    var name by remember { mutableStateOf(negocio.nombreNegocio ?: "") }
                    var category by remember { mutableStateOf(negocio.rubro ?: "") }
                    var address by remember { mutableStateOf(negocio.direccion ?: "") }
                    var latitude by remember { mutableStateOf(negocio.latitud) }
                    var longitude by remember { mutableStateOf(negocio.longitud) }
                    
                    val scheduleState = remember(negocio.horario) { parseScheduleString(negocio.horario) }
                    
                    var description by remember { mutableStateOf(negocio.descripcion ?: "") }
                    var photoUri by remember { mutableStateOf<Uri?>(if (!negocio.fotoNegocio.isNullOrEmpty()) Uri.parse(negocio.fotoNegocio) else null) }

                    val context = LocalContext.current

                    val showPricing = category == "Piscinas" || category == "Canchas Sintéticas"
                    val priceEntries = remember { mutableStateListOf<PriceEntry>() }
                    LaunchedEffect(negocio.id, category) {
                        priceEntries.clear()
                        if (showPricing && !negocio.precios.isNullOrBlank()) {
                            negocio.precios!!.split(",").forEach { entry ->
                                val parts = entry.trim().split(":")
                                if (parts.size == 2) {
                                    priceEntries.add(PriceEntry(parts[0].trim(), parts[1].trim()))
                                }
                            }
                        }
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
                                    aspectRatioX = 16,
                                    aspectRatioY = 9,
                                    fixAspectRatio = true
                                ))
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
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
                                    Text("Toca para cambiar la portada", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
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
                            onValueChange = { address = it },
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
                            onValueChange = { description = it },
                            label = { Text("Descripción") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )

                        if (showPricing) {
                            Spacer(modifier = Modifier.height(24.dp))
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

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                businessId.toIntOrNull()?.let { id ->
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
                                        
                                    val finalPhotoUri = if (photoUri?.toString() != negocio.fotoNegocio) {
                                        photoUri?.toString() ?: ""
                                    } else {
                                        ""
                                    }
                                    viewModel.updateBusiness(id, name, category, address, finalSchedule, description, finalPhotoUri, latitude, longitude, finalPrices)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Guardar Cambios")
                        }
                    }
                }
            }
        }
    }
}
