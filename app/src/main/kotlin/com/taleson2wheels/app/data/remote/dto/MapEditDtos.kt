package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ── Map-edit (post-ride) — /api/v1/rides/{id}/live/map-edit/* ────────────────
// The editor operates on a PAUSED/ENDED session (never a live one) and is gated
// server-side by canEditRideMap. See docs on the backend map-edit surface.

// --- Breaks ---

/** Body for POST .../map-edit/breaks. `startedAt`/`endedAt` are ISO-8601. */
@Serializable
data class MapAddBreakRequest(
    val startedAt: String,
    val endedAt: String? = null,
    val reason: String? = null,
)

/** Response wrapper for add/edit break — reuses the shared LiveBreak shape. */
@Serializable
data class MapBreakResponse(
    @SerialName("break") val breakInfo: LiveBreak? = null,
)

/** Generic `{ success, id }` returned by the break delete. */
@Serializable
data class MapDeleteResponse(
    val success: Boolean = false,
    val id: String? = null,
)

// --- Smooth & fill ---

@Serializable
data class MapSmoothRequest(val userId: String)

/** Stats returned by the smooth pipeline (preview or persist). */
@Serializable
data class SmoothStats(
    val rawCount: Int = 0,
    val snappedCount: Int = 0,
    val interpolatedCount: Int = 0,
    val gapsFilled: Int = 0,
    val gapsSkipped: Int = 0,
    val gapsTotalSeconds: Double = 0.0,
    val movedPercent: Int = 0,
)

/**
 * Response for POST .../smooth-track. `points` is an INTEGER count on persist and
 * an ARRAY of proposed points on preview — kept as a raw JsonElement so the VM can
 * derive a count from either shape.
 */
@Serializable
data class MapSmoothResponse(
    val stats: SmoothStats? = null,
    val points: JsonElement? = null,
    val preview: Boolean = false,
)

/** Body for DELETE .../smooth-track (revert to raw). */
@Serializable
data class MapRevertRequest(val userId: String)

/** `{ deleted }` returned by revert + track-point deletes. */
@Serializable
data class MapDeletedResponse(val deleted: Int = 0)

// --- Stats overrides ---
// The request body is built as a JsonObject in the repository (so a cleared
// field can send an explicit null, which the app's explicitNulls=false Json
// would otherwise omit). The response just echoes the session's override columns.

@Serializable
data class MapStatsSession(
    val id: String = "",
    val distanceKmOverride: Double? = null,
    val avgSpeedKmhOverride: Double? = null,
    val maxSpeedKmhOverride: Double? = null,
    val movingMinutesOverride: Int? = null,
    val elevationGainM: Int? = null,
    val elevationLossM: Int? = null,
)

@Serializable
data class MapStatsResponse(val session: MapStatsSession? = null)

// --- Audit ---

@Serializable
data class MapAuditEntry(
    val id: String,
    val editedBy: String = "",
    val editedByName: String = "",
    val action: String = "",
    val details: JsonElement? = null,
    val createdAt: String = "",
)

@Serializable
data class MapAuditResponse(val edits: List<MapAuditEntry> = emptyList())

// --- GPX import (multipart) ---

/** Response for POST .../track-from-gpx (recorded-track replacement). */
@Serializable
data class MapGpxTrackResponse(
    val inserted: Int = 0,
    val attachmentId: String? = null,
    val distanceKm: Double = 0.0,
)

/** Response for POST .../planned-route/from-gpx. */
@Serializable
data class MapGpxPlannedResponse(
    val waypointCount: Int = 0,
    val distanceKm: Double = 0.0,
    val attachmentId: String? = null,
    val plannedRouteEditedAt: String? = null,
)
