package com.taleson2wheels.app.data.location

import com.taleson2wheels.app.data.remote.dto.LocationPoint
import kotlinx.serialization.Serializable

/**
 * The unsent GPS tail persisted to disk when the live-location service is torn
 * down, so it survives process death and a failed final upload. Recovered and
 * re-uploaded (to its original [rideId]) on the next live-ride start.
 */
@Serializable
data class PendingFixes(val rideId: String, val points: List<LocationPoint>)
