package com.example.topmejorestiendas.feature.home.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.ImageLoader
import coil.request.ImageRequest
import coil.compose.AsyncImage
import com.example.topmejorestiendas.core.domain.model.Business
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreMapScreen(
    viewModel: StoreMapViewModel,
    onNavigateToBusiness: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedBusiness by remember { mutableStateOf<Business?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tiendas cercanas") },
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
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error!!,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    StoreMapView(
                        businesses = uiState.businesses,
                        userLat = uiState.userLat,
                        userLng = uiState.userLng,
                        profilePhotoUrl = uiState.profilePhotoUrl,
                        onBusinessClick = { business ->
                            selectedBusiness = business
                            showBottomSheet = true
                        }
                    )
                }
            }
        }
    }

    if (showBottomSheet && selectedBusiness != null) {
        val business = selectedBusiness!!
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            BusinessBottomSheetContent(
                business = business,
                onViewBusiness = {
                    showBottomSheet = false
                    onNavigateToBusiness(business.id)
                }
            )
        }
    }
}

@Composable
private fun StoreMapView(
    businesses: List<Business>,
    userLat: Double,
    userLng: Double,
    profilePhotoUrl: String,
    onBusinessClick: (Business) -> Unit
) {
    val context = LocalContext.current

    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var zoomApplied by remember { mutableStateOf(false) }
    val userPoint = remember(userLat, userLng) {
        if (userLat != 0.0 || userLng != 0.0) GeoPoint(userLat, userLng) else null
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osm_prefs", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    LaunchedEffect(profilePhotoUrl) {
        if (profilePhotoUrl.isNotEmpty()) {
            try {
                val loader = ImageLoader(context)
                val result = loader.execute(
                    ImageRequest.Builder(context).data(profilePhotoUrl).size(80).build()
                )
                profileBitmap = (result.drawable as? BitmapDrawable)?.bitmap
            } catch (_: Exception) { }
        }
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapViewRef?.onDetach()
        }
    }

    LaunchedEffect(mapViewRef, businesses, userPoint, profileBitmap) {
        val mv = mapViewRef ?: return@LaunchedEffect
        if (businesses.isEmpty()) return@LaunchedEffect

        val validBusinesses = businesses.filter { it.latitude != 0.0 || it.longitude != 0.0 }
        mv.overlays.clear()

        if (userPoint != null) {
            val icon = BitmapDrawable(
                mv.resources,
                if (profileBitmap != null) createPhotoCircleBitmap(profileBitmap!!, mv.context)
                else createTextCircleBitmap(48, "#1976D2", "T", mv.context)
            )
            Marker(mv).apply {
                position = userPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Tu ubicación"
                this.icon = icon
                mv.overlays.add(this)
            }
        }

        val photoBitmaps = mutableMapOf<String, Bitmap?>()
        for (business in validBusinesses) {
            if (business.imageUrl.isNotEmpty()) {
                val bitmap = try {
                    val base64 = business.imageUrl.substringAfter("base64,").trim()
                    val imageBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } catch (_: Exception) { null }
                photoBitmaps[business.id] = bitmap
            }
        }

        for (business in validBusinesses) {
            val rawPhoto = photoBitmaps[business.id]
            val swPhoto = rawPhoto?.let {
                if (it.config != Bitmap.Config.ARGB_8888) it.copy(Bitmap.Config.ARGB_8888, false)
                else it
            }
            val bitmap = createBusinessMarkerBitmap(swPhoto, business.category, mv.context)
            Marker(mv).apply {
                position = GeoPoint(business.latitude, business.longitude)
                setAnchor(Marker.ANCHOR_CENTER, 1f)
                title = business.name
                snippet = business.distanceText
                relatedObject = business
                icon = BitmapDrawable(mv.resources, bitmap)
                setOnMarkerClickListener { m, _ ->
                    val biz = m.relatedObject as? Business
                    if (biz != null) onBusinessClick(biz)
                    true
                }
                mv.overlays.add(this)
            }
        }

        if (!zoomApplied) {
            zoomApplied = true
            val allPoints = mutableListOf<GeoPoint>()
            userPoint?.let { allPoints.add(it) }
            allPoints.addAll(validBusinesses.map { GeoPoint(it.latitude, it.longitude) })

            if (allPoints.size >= 2) {
                val north = allPoints.maxOf { it.latitude }
                val south = allPoints.minOf { it.latitude }
                val east = allPoints.maxOf { it.longitude }
                val west = allPoints.minOf { it.longitude }
                mv.zoomToBoundingBox(
                    BoundingBox(north + 0.008, east + 0.008, south - 0.008, west - 0.008),
                    true
                )
            } else if (allPoints.size == 1) {
                mv.controller.animateTo(allPoints[0])
                mv.controller.setZoom(15.0)
            }
        }

        mv.invalidate()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).also { mv ->
                mv.setTileSource(TileSourceFactory.MAPNIK)
                mv.setMultiTouchControls(true)
                mv.controller.setZoom(14.0)
                mv.controller.setCenter(userPoint ?: GeoPoint(-5.9327, -77.3082))
                mapViewRef = mv
            }
        },
        update = { }
    )
}

private fun createTextCircleBitmap(size: Int, colorHex: String, text: String, ctx: Context): Bitmap {
    val density = ctx.resources.displayMetrics.density
    val pxSize = (size * density).toInt()
    val bitmap = Bitmap.createBitmap(pxSize, pxSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle((pxSize / 2).toFloat(), (pxSize / 2).toFloat(), (pxSize / 2).toFloat(), paint)

    paint.color = android.graphics.Color.parseColor(colorHex)
    canvas.drawCircle((pxSize / 2).toFloat(), (pxSize / 2).toFloat(), (pxSize / 2 - 4).toFloat(), paint)

    paint.color = android.graphics.Color.WHITE
    paint.textSize = pxSize * 0.55f
    paint.textAlign = Paint.Align.CENTER
    val yPos = (pxSize / 2).toFloat() - ((paint.descent() + paint.ascent()) / 2)
    canvas.drawText(text.take(1).uppercase(), (pxSize / 2).toFloat(), yPos, paint)

    return bitmap
}

private fun createPhotoCircleBitmap(photo: Bitmap, ctx: Context): Bitmap {
    val swPhoto = if (photo.config != Bitmap.Config.ARGB_8888) {
        photo.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        photo
    }
    val density = ctx.resources.displayMetrics.density
    val pxSize = (48 * density).toInt()
    val bitmap = Bitmap.createBitmap(pxSize, pxSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle((pxSize / 2).toFloat(), (pxSize / 2).toFloat(), (pxSize / 2).toFloat(), paint)

    val path = Path().apply {
        addCircle((pxSize / 2).toFloat(), (pxSize / 2).toFloat(), (pxSize / 2 - 3).toFloat(), Path.Direction.CW)
    }
    canvas.save()
    canvas.clipPath(path)
    canvas.drawBitmap(swPhoto, Rect(0, 0, swPhoto.width, swPhoto.height), Rect(3, 3, pxSize - 3, pxSize - 3), null)
    canvas.restore()

    return bitmap
}

private fun getCategoryColor(category: String, colorHex: String? = null): Int {
    if (colorHex != null) {
        try {
            return android.graphics.Color.parseColor(colorHex)
        } catch (_: Exception) { }
    }
    return when (category.lowercase().trim()) {
        "restaurante" -> 0xFFE53935.toInt()
        "cafetería", "cafeteria" -> 0xFF795548.toInt()
        "cancha sintética", "cancha sintetico", "cancha" -> 0xFF43A047.toInt()
        "bar" -> 0xFF5C6BC0.toInt()
        "pizzería", "pizzeria" -> 0xFFFF9800.toInt()
        "heladería", "heladeria" -> 0xFFEC407A.toInt()
        "panadería", "panaderia" -> 0xFFFFA726.toInt()
        "gimnasio" -> 0xFF7B1FA2.toInt()
        "salón de belleza", "salon de belleza", "peluquería", "peluqueria" -> 0xFFE91E63.toInt()
        "licorería", "licoreria" -> 0xFF607D8B.toInt()
        "farmacia" -> 0xFF00BCD4.toInt()
        "supermercado" -> 0xFF4CAF50.toInt()
        "librería", "libreria" -> 0xFFFF5722.toInt()
        else -> 0xFF757575.toInt()
    }
}

private fun createBusinessMarkerBitmap(photo: Bitmap?, category: String, ctx: Context, colorHex: String? = null): Bitmap {
    val density = ctx.resources.displayMetrics.density
    val photoWidthPx = (64 * density).toInt()
    val barHeightPx = (18 * density).toInt()
    val totalHeightPx = photoWidthPx + barHeightPx
    val radiusPx = (8 * density).toInt()
    val categoryColor = getCategoryColor(category)
    val shortCategory = category.split(" ").first().take(8)

    val bitmap = Bitmap.createBitmap(photoWidthPx, totalHeightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val roundRect = RectF(0f, 0f, photoWidthPx.toFloat(), totalHeightPx.toFloat())
    val clipPath = Path().apply {
        addRoundRect(roundRect, radiusPx.toFloat(), radiusPx.toFloat(), Path.Direction.CW)
    }
    canvas.clipPath(clipPath)

    if (photo != null) {
        val srcRect: Rect
        val dstRect = Rect(0, 0, photoWidthPx, photoWidthPx)
        val photoAspect = photo.width.toFloat() / photo.height.toFloat()
        if (photoAspect > 1f) {
            val cropWidth = photo.height
            val cropLeft = (photo.width - cropWidth) / 2
            srcRect = Rect(cropLeft, 0, cropLeft + cropWidth, photo.height)
        } else {
            val cropHeight = photo.width
            val cropTop = (photo.height - cropHeight) / 2
            srcRect = Rect(0, cropTop, photo.width, cropTop + cropHeight)
        }
        canvas.drawBitmap(photo, srcRect, dstRect, null)
    } else {
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = categoryColor }
        canvas.drawCircle(photoWidthPx / 2f, photoWidthPx / 2f, photoWidthPx / 2f, circlePaint)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = photoWidthPx * 0.5f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val yPos = photoWidthPx / 2f - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(shortCategory.first().uppercase(), photoWidthPx / 2f, yPos, textPaint)
    }

    val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = categoryColor }
    canvas.drawRect(0f, photoWidthPx.toFloat(), photoWidthPx.toFloat(), totalHeightPx.toFloat(), barPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = (11 * density).toFloat()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val textY = photoWidthPx + (barHeightPx / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText(shortCategory, photoWidthPx / 2f, textY, textPaint)

    return bitmap
}

@Composable
private fun BusinessBottomSheetContent(
    business: Business,
    onViewBusiness: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = business.imageUrl,
                contentDescription = business.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = business.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format(Locale.US, "%.1f", business.rating),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB300)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < business.rating.toInt()) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (index < business.rating.toInt()) Color(0xFFFFB300) else Color(0xFFE0E0E0),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = business.distanceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onViewBusiness,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Ver negocio", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
