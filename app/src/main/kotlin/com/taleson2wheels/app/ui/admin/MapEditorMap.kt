package com.taleson2wheels.app.ui.admin

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.taleson2wheels.app.data.remote.dto.LivePathPoint
import com.taleson2wheels.app.ui.components.BrandCard

/**
 * Post-ride editor map: the selected rider's recorded track (red) over the
 * planned-route overlay (gold), with start/end pins. Renders only when a Google
 * Maps key is configured (docs/HARDENING.md); without one it stays blank but the
 * editor controls below are unaffected.
 */
@Composable
fun MapEditorMapCard(
    recordedPath: List<LivePathPoint>,
    plannedRoute: List<LivePathPoint>,
    modifier: Modifier = Modifier,
) {
    val recorded = recordedPath.map { LatLng(it.lat, it.lng) }
    val planned = plannedRoute.map { LatLng(it.lat, it.lng) }
    val center = recorded.firstOrNull() ?: planned.firstOrNull() ?: LatLng(12.97, 77.59)
    val hasTrack = recorded.size >= 2 || planned.size >= 2
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, if (hasTrack) 11f else 6f)
    }

    BrandCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        GoogleMap(
            modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(16.dp)),
            cameraPositionState = cameraPositionState,
        ) {
            if (planned.size >= 2) {
                Polyline(points = planned, color = Color(0xFFF5A623), width = 6f)
            }
            if (recorded.size >= 2) {
                Polyline(points = recorded, color = Color(0xFFFF4757), width = 8f)
                Marker(
                    state = MarkerState(position = recorded.first()),
                    title = "Start",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                )
                Marker(
                    state = MarkerState(position = recorded.last()),
                    title = "End",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                )
            }
        }
    }
}
