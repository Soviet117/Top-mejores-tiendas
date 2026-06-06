package com.example.topmejorestiendas.feature.common.ui

import android.content.Context
import android.view.MotionEvent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

@Composable
fun OsmMap(
    modifier: Modifier = Modifier,
    latitude: Double,
    longitude: Double,
    onLocationChanged: ((Double, Double) -> Unit)? = null,
    isEditMode: Boolean = false
) {
    val context = LocalContext.current

    // OSMDroid requires configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osm_prefs", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val geoPoint = remember(latitude, longitude) {
        if (latitude == 0.0 && longitude == 0.0) {
            // Default coordinates (e.g. city center)
            GeoPoint(-16.5000, -68.1500) // La Paz, Bolivia as fallback
        } else {
            GeoPoint(latitude, longitude)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            object : MapView(ctx) {
                override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                    parent.requestDisallowInterceptTouchEvent(true)
                    return super.dispatchTouchEvent(ev)
                }
            }.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(16.0)
                controller.setCenter(geoPoint)

                val marker = Marker(this)
                marker.position = geoPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = if (isEditMode) "Tu Local" else "Ubicación"
                overlays.add(marker)

                if (isEditMode) {
                    val touchOverlay = object : Overlay() {
                        override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                            val projection = mapView.projection
                            val gPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                            
                            marker.position = gPoint
                            mapView.invalidate()
                            
                            onLocationChanged?.invoke(gPoint.latitude, gPoint.longitude)
                            return true
                        }
                    }
                    overlays.add(touchOverlay)
                }
            }
        },
        update = { mapView ->
            if (!isEditMode && (latitude != 0.0 || longitude != 0.0)) {
                mapView.controller.setCenter(geoPoint)
                val marker = mapView.overlays.filterIsInstance<Marker>().firstOrNull()
                marker?.position = geoPoint
                mapView.invalidate()
            }
        }
    )
}
