package com.taleson2wheels.app.ui.live

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.taleson2wheels.app.ui.components.BrandCard
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.taleson2wheels.app.data.remote.dto.LivePathPoint
import com.taleson2wheels.app.data.remote.dto.LiveRiderPosition

/**
 * Live map: a marker per rider (lead/sweep/deviated tinted) and the lead rider's
 * traced path. Renders only when a Google Maps API key is configured (see
 * docs/HARDENING.md); without one the map area stays blank but the build and the
 * rest of the live screen are unaffected.
 */
@Composable
fun LiveMapCard(
    riders: List<LiveRiderPosition>,
    leadPath: List<LivePathPoint>,
    modifier: Modifier = Modifier,
) {
    val center = riders.firstOrNull()?.let { LatLng(it.lat, it.lng) } ?: return
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 13f)
    }

    BrandCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        GoogleMap(
            modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(16.dp)),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = com.google.maps.android.compose.MapType.NORMAL),
        ) {
            if (leadPath.size >= 2) {
                Polyline(points = leadPath.map { LatLng(it.lat, it.lng) })
            }
            riders.forEach { rider ->
                val hue = when {
                    rider.isLead -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                    rider.isSweep -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
                    rider.isDeviated -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                    else -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE
                }
                Marker(
                    state = rememberMarkerState(key = rider.userId, position = LatLng(rider.lat, rider.lng)),
                    title = rider.userName.ifBlank { "Rider" },
                    snippet = buildString {
                        when {
                            rider.isLead -> append("Lead")
                            rider.isSweep -> append("Sweep")
                            rider.isDeviated -> append("Off-route")
                        }
                        rider.speed?.let {
                            if (isNotEmpty()) append(" · ")
                            append("${(it * 3.6).toInt()} km/h")
                        }
                    }.ifBlank { null },
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hue),
                )
            }
        }
    }
}
