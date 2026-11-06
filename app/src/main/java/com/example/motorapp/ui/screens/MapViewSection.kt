package com.example.motorapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.motorapp.domain.model.NavigationState
import com.example.motorapp.domain.model.PeerRider
import com.example.motorapp.domain.model.TurnManeuver
import com.example.motorapp.util.formatDecimal
import com.example.motorapp.util.formatDistance
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker as OsmMarker
import org.osmdroid.views.overlay.Polyline as OsmPolyline

@Composable
fun MapViewSection(
    currentLocation: Location?,
    connectedPeers: Map<String, PeerRider>,
    navigationState: NavigationState,
    isP2pSharing: Boolean,
    onToggleP2pSharing: () -> Unit,
    onSetDestination: (Double, Double) -> Unit,
    onClearDestination: () -> Unit
) {
    val context = LocalContext.current
    var useOpenStreetMap by remember { mutableStateOf(true) }

    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    Box(modifier = Modifier.fillMaxSize()) {
        if (useOpenStreetMap) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    Configuration.getInstance().userAgentValue = "MotorAppTelemetry/1.0 (https://github.com/motorapp/motorapp)"
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)

                        val startPoint = GeoPoint(
                            currentLocation?.latitude ?: 47.4979,
                            currentLocation?.longitude ?: 19.0402
                        )
                        controller.setCenter(startPoint)

                        overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                p?.let { onSetDestination(it.latitude, it.longitude) }
                                return true
                            }

                            override fun longPressHelper(p: GeoPoint?): Boolean {
                                p?.let { onSetDestination(it.latitude, it.longitude) }
                                return true
                            }
                        }))
                    }
                },
                update = { mapView ->
                    mapView.overlays.removeAll { it is OsmMarker || it is OsmPolyline }

                    currentLocation?.let { loc ->
                        val myMarker = OsmMarker(mapView).apply {
                            position = GeoPoint(loc.latitude, loc.longitude)
                            title = "My Bike"
                            setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                        }
                        mapView.overlays.add(myMarker)
                    }

                    connectedPeers.values.forEach { peer ->
                        val peerMarker = OsmMarker(mapView).apply {
                            position = GeoPoint(peer.latitude, peer.longitude)
                            title = "${peer.name} (${peer.speedKmH.formatDecimal(0)} km/h)"
                            setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                        }
                        mapView.overlays.add(peerMarker)
                    }

                    if (navigationState.hasActiveDestination) {
                        val destGeo = GeoPoint(navigationState.destinationLatitude, navigationState.destinationLongitude)
                        val destMarker = OsmMarker(mapView).apply {
                            position = destGeo
                            title = "Destination"
                            setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                        }
                        mapView.overlays.add(destMarker)

                        val line = OsmPolyline().apply {
                            if (navigationState.routePoints.isNotEmpty()) {
                                navigationState.routePoints.forEach { pt ->
                                    addPoint(GeoPoint(pt.first, pt.second))
                                }
                            } else {
                                currentLocation?.let { loc ->
                                    addPoint(GeoPoint(loc.latitude, loc.longitude))
                                    addPoint(destGeo)
                                }
                            }
                            outlinePaint.color = android.graphics.Color.parseColor("#0288D1")
                            outlinePaint.strokeWidth = 14f
                        }
                        mapView.overlays.add(line)
                    }

                    mapView.invalidate()
                }
            )
        } else {
            val defaultLatLng = LatLng(
                currentLocation?.latitude ?: 47.4979,
                currentLocation?.longitude ?: 19.0402
            )

            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(defaultLatLng, 15f)
            }

            LaunchedEffect(currentLocation) {
                currentLocation?.let {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                        LatLng(it.latitude, it.longitude),
                        16f
                    )
                }
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = hasLocationPermission),
                onMapClick = { latLng ->
                    onSetDestination(latLng.latitude, latLng.longitude)
                }
            ) {
                connectedPeers.values.forEach { peer ->
                    Marker(
                        state = MarkerState(position = LatLng(peer.latitude, peer.longitude)),
                        title = peer.name,
                        snippet = "Speed: ${peer.speedKmH.formatDecimal(0)} km/h | Lean: ${peer.leanAngle.formatDecimal(1)}°"
                    )
                }

                if (navigationState.hasActiveDestination) {
                    val destLatLng = LatLng(navigationState.destinationLatitude, navigationState.destinationLongitude)
                    Marker(
                        state = MarkerState(position = destLatLng),
                        title = "Destination",
                        snippet = "Turn: ${navigationState.nextManeuver} (${navigationState.distanceToNextTurnMeters.formatDistance()})"
                    )

                    val polylinePoints = if (navigationState.routePoints.isNotEmpty()) {
                        navigationState.routePoints.map { LatLng(it.first, it.second) }
                    } else {
                        listOfNotNull(
                            currentLocation?.let { LatLng(it.latitude, it.longitude) },
                            destLatLng
                        )
                    }

                    if (polylinePoints.size >= 2) {
                        Polyline(
                            points = polylinePoints,
                            color = Color(0xFF0288D1),
                            width = 14f
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            if (navigationState.hasActiveDestination) {
                val (symbol, turnText) = when (navigationState.nextManeuver) {
                    TurnManeuver.LEFT -> "←" to "TURN LEFT"
                    TurnManeuver.SLIGHT_LEFT -> "↖" to "BEAR LEFT"
                    TurnManeuver.SHARP_LEFT -> "⮌" to "SHARP LEFT"
                    TurnManeuver.RIGHT -> "→" to "TURN RIGHT"
                    TurnManeuver.SLIGHT_RIGHT -> "↗" to "BEAR RIGHT"
                    TurnManeuver.SHARP_RIGHT -> "⮱" to "SHARP RIGHT"
                    TurnManeuver.STRAIGHT -> "↑" to "CONTINUE STRAIGHT"
                    TurnManeuver.U_TURN -> "↶" to "U-TURN"
                    TurnManeuver.ARRIVED -> "🏁" to "ARRIVED"
                    TurnManeuver.NONE -> "" to ""
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (navigationState.isLoadingRoute) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .align(Alignment.CenterVertically),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (navigationState.isLoadingRoute) "Calculating road route..." else "$symbol $turnText in ${navigationState.distanceToNextTurnMeters.formatDistance()}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Destination: ${navigationState.totalDistanceRemainingMeters.formatDistance()} remaining",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        IconButton(onClick = onClearDestination) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Route")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                ) {
                    Text(
                        text = "Tap anywhere on map to plan turn-by-turn road route",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { useOpenStreetMap = !useOpenStreetMap },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (useOpenStreetMap) Color(0xFF1565C0) else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = if (useOpenStreetMap) "OSM MAP" else "GOOGLE MAP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = onToggleP2pSharing,
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isP2pSharing) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isP2pSharing) Icons.Default.Group else Icons.Default.GroupOff,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = if (isP2pSharing) "P2P ACTIVE (${connectedPeers.size})" else "START P2P",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
