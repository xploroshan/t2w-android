package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Live tracking (/api/v1/rides/{id}/live/*) ────────────────────────────────

@Serializable
data class LiveBreak(
    val id: String,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val reason: String? = null,
)

/** A live tracking session for a ride. `status` ∈ waiting|live|paused|ended. */
@Serializable
data class LiveSession(
    val id: String,
    val rideId: String,
    val status: String,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val leadRiderId: String? = null,
    val sweepRiderId: String? = null,
    val plannedRoute: String? = null,
    val breaks: List<LiveBreak> = emptyList(),
)

/** A rider's latest position in the live session. */
@Serializable
data class LiveRiderPosition(
    val userId: String,
    val userName: String = "",
    val userAvatar: String? = null,
    val lat: Double,
    val lng: Double,
    val speed: Double? = null,
    val heading: Double? = null,
    val isDeviated: Boolean = false,
    val isLead: Boolean = false,
    val isSweep: Boolean = false,
    val recordedAt: String? = null,
)

@Serializable
data class LivePathPoint(
    val lat: Double,
    val lng: Double,
    val recordedAt: String? = null,
    val speed: Double? = null,
    val accuracy: Double? = null,
)

/** `GET /rides/{id}/live` — session state + rider positions + paths. */
@Serializable
data class LiveState(
    val session: LiveSession? = null,
    val riders: List<LiveRiderPosition> = emptyList(),
    val leadPath: List<LivePathPoint> = emptyList(),
    val myPath: List<LivePathPoint> = emptyList(),
    val viewPath: List<LivePathPoint> = emptyList(),
)

/** One GPS fix queued for upload. `recordedAt` is the device fix time (ISO-8601). */
@Serializable
data class LocationPoint(
    val lat: Double,
    val lng: Double,
    val speed: Double? = null,
    val heading: Double? = null,
    val accuracy: Double? = null,
    val recordedAt: String,
)

/** Body for `POST /rides/{id}/live/location` — a batch of queued fixes. */
@Serializable
data class LocationBatch(val points: List<LocationPoint>)

@Serializable
data class LocationUploadResponse(
    val success: Boolean = false,
    val received: Int = 0,
    val accepted: Int = 0,
    val deviatedCount: Int = 0,
)

/** Headline live metrics; the server may add more fields (tolerated). */
@Serializable
data class LiveMetrics(
    val elapsedMinutes: Double = 0.0,
    val movingMinutes: Double = 0.0,
    val distanceKm: Double = 0.0,
    val distanceSource: String? = null,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val breakCount: Int = 0,
    val breakMinutes: Double = 0.0,
    val riderCount: Int = 0,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val elevationGainM: Double? = null,
    val elevationLossM: Double? = null,
)

// ── Request / response wrappers ──────────────────────────────────────────────

/** Body for `POST /rides/{id}/live` — action ∈ start|pause|resume|end. */
@Serializable
data class LiveControlRequest(val action: String)

@Serializable
data class LiveControlResponse(
    val session: LiveSession? = null,
    val action: String? = null,
)

@Serializable
data class LiveJoinResponse(
    val success: Boolean = false,
    val session: LiveSession? = null,
    val isLead: Boolean = false,
    val isSweep: Boolean = false,
)

/** Body for `POST /rides/{id}/live/break` — action ∈ start|end. */
@Serializable
data class LiveBreakRequest(val action: String, val reason: String? = null)

@Serializable
data class LiveBreakResponse(
    val success: Boolean = false,
    @SerialName("break") val breakInfo: LiveBreak? = null,
)
