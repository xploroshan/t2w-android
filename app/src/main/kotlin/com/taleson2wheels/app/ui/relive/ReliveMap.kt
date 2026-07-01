package com.taleson2wheels.app.ui.relive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.extension.style.terrain.generated.terrain
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.taleson2wheels.app.data.remote.dto.LivePathPoint

/**
 * Holds the per-MapView imperative handles that must survive recomposition: the
 * moving marker's annotation manager + the marker itself, and the identity of the
 * geometry last drawn into the style (so the style is reloaded — an expensive op —
 * only when the track actually changes, not on every playback frame).
 */
private class ReliveMapHolder {
    var markerManager: CircleAnnotationManager? = null
    var marker: CircleAnnotation? = null
    var drawnRecorded: List<Point>? = null
    var drawnPlanned: List<Point>? = null
}

/**
 * The Mapbox 3D-terrain flyover surface. Renders the selected rider's recorded
 * track (accent line) over the planned route (gold) on a satellite + DEM-terrain
 * style, with a moving marker and a chase camera driven by [sample]. All Mapbox
 * API surface is confined to this file; everything above it ([ReliveViewModel],
 * [RelivePlayback]) is pure and unit-tested.
 *
 * Precondition: [token] is a non-blank Mapbox public (`pk.`) token — the caller
 * shows a placeholder instead when it's blank, so tiles never fail to load here.
 */
@Composable
fun ReliveMap(
    token: String,
    recorded: List<ReliveTrackPoint>,
    planned: List<LivePathPoint>,
    sample: ReliveSample?,
    modifier: Modifier = Modifier,
) {
    // Map domain points → Mapbox points once per track (stable reference across
    // playback frames), so the update block can cheaply detect a real track change
    // by reference instead of rebuilding/rehashing the list every frame.
    val recordedPts = remember(recorded) { recorded.map { Point.fromLngLat(it.lng, it.lat) } }
    val plannedPts = remember(planned) { planned.map { Point.fromLngLat(it.lng, it.lat) } }
    val holder = remember { ReliveMapHolder() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // The global token must be set before the first MapView is created.
            MapboxOptions.accessToken = token
            MapView(ctx).apply {
                // v11 MapView is lifecycle-aware via the view tree's LifecycleOwner
                // (AndroidView provides it), so start/stop/destroy are automatic.
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(recordedPts.firstOrNull() ?: DEFAULT_CENTER)
                        .zoom(if (recordedPts.isNotEmpty()) FLYOVER_ZOOM else OVERVIEW_ZOOM)
                        .pitch(FLYOVER_PITCH)
                        .build(),
                )
            }
        },
        update = { mapView ->
            // Reload the style only when the drawn geometry actually changed (rider
            // switch / first track), never on a mere playback tick. The recorded +
            // planned lines are style layers; the annotation plugin re-adds the
            // marker on top of each newly loaded style, so the manager persists.
            if (holder.drawnRecorded !== recordedPts || holder.drawnPlanned !== plannedPts) {
                holder.drawnRecorded = recordedPts
                holder.drawnPlanned = plannedPts
                mapView.mapboxMap.loadStyle(
                    style(Style.STANDARD_SATELLITE) {
                        +rasterDemSource(DEM_SOURCE) {
                            url(TERRAIN_DEM_URL)
                            // 514 = padded DEM tile; better perf than 512.
                            tileSize(514)
                        }
                        +terrain(DEM_SOURCE) {
                            exaggeration(TERRAIN_EXAGGERATION)
                        }
                        if (plannedPts.size >= 2) {
                            +geoJsonSource(PLANNED_SOURCE) {
                                geometry(LineString.fromLngLats(plannedPts))
                            }
                            +lineLayer(PLANNED_LAYER, PLANNED_SOURCE) {
                                lineColor(GOLD)
                                lineWidth(3.0)
                            }
                        }
                        if (recordedPts.size >= 2) {
                            +geoJsonSource(TRACK_SOURCE) {
                                geometry(LineString.fromLngLats(recordedPts))
                            }
                            +lineLayer(TRACK_LAYER, TRACK_SOURCE) {
                                lineColor(ACCENT)
                                lineWidth(5.0)
                            }
                        }
                    },
                )
            }

            // Create the marker manager once; it re-attaches to each loaded style
            // internally, so it need not be recreated on a style reload.
            val mgr = holder.markerManager
                ?: mapView.annotations.createCircleAnnotationManager().also { holder.markerManager = it }

            // Every frame: move the marker + chase camera to the interpolated sample.
            sample?.let { s ->
                val here = Point.fromLngLat(s.lng, s.lat)
                val existing = holder.marker
                if (existing == null) {
                    holder.marker = mgr.create(
                        CircleAnnotationOptions()
                            .withPoint(here)
                            .withCircleRadius(7.0)
                            .withCircleColor(ACCENT)
                            .withCircleStrokeColor(WHITE)
                            .withCircleStrokeWidth(2.0),
                    )
                } else {
                    existing.point = here
                    mgr.update(existing)
                }
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(here)
                        .zoom(FLYOVER_ZOOM)
                        .pitch(FLYOVER_PITCH)
                        .bearing(s.bearing)
                        .build(),
                )
            }
        },
    )
}

private val DEFAULT_CENTER: Point = Point.fromLngLat(77.59, 12.97) // Bengaluru
private const val FLYOVER_ZOOM = 15.5
private const val OVERVIEW_ZOOM = 6.0
private const val FLYOVER_PITCH = 62.0
private const val TERRAIN_EXAGGERATION = 1.3
private const val DEM_SOURCE = "relive-dem"
private const val TERRAIN_DEM_URL = "mapbox://mapbox.mapbox-terrain-dem-v1"
private const val TRACK_SOURCE = "relive-track-src"
private const val TRACK_LAYER = "relive-track-layer"
private const val PLANNED_SOURCE = "relive-planned-src"
private const val PLANNED_LAYER = "relive-planned-layer"
private const val ACCENT = "#E94560"
private const val GOLD = "#F5A623"
private const val WHITE = "#FFFFFF"
