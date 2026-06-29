package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RideDto(
    val id: String,
    val title: String,
    val rideNumber: String,
    val type: String,
    val status: String,
    val startDate: String,
    val endDate: String,
    val startLocation: String? = null,
    val startLocationUrl: String? = null,
    val endLocation: String? = null,
    val endLocationUrl: String? = null,
    val route: List<String> = emptyList(),
    val distanceKm: Double = 0.0,
    val maxRiders: Int = 0,
    val difficulty: String? = null,
    val description: String? = null,
    val highlights: List<String> = emptyList(),
    val posterUrl: String? = null,
    val fee: Double = 0.0,
    val extraBedSlots: Int = 0,
    val extraBedFee: Double = 0.0,
    val leadRider: String? = null,
    val sweepRider: String? = null,
    val meetupTime: String? = null,
    val rideStartTime: String? = null,
    val detailsVisible: Boolean = false,
    val registrationCount: Int = 0,
    val isRegistered: Boolean = false,
)

@Serializable
data class RideRegistrationRequest(
    val riderName: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val bloodGroup: String? = null,
    val foodPreference: String? = null,
    val vehicleModel: String? = null,
    val vehicleRegNumber: String? = null,
    val tshirtSize: String? = null,
    val accommodationType: String = "bed",
    val upiTransactionId: String? = null,
    val paymentScreenshot: String? = null,
    val agreedCancellationTerms: Boolean = false,
    val agreedIndemnity: Boolean = false,
)

@Serializable
data class RideRegistrationDto(
    val id: String,
    val rideId: String,
    val riderName: String? = null,
    val approvalStatus: String,
    val accommodationType: String? = null,
    val confirmationCode: String? = null,
    val registeredAt: String? = null,
)

// ── Live tracking ───────────────────────────────────────────────────────────

@Serializable
data class LocationPoint(
    val lat: Double,
    val lng: Double,
    val speed: Double? = null,
    val heading: Double? = null,
    val accuracy: Double? = null,
    val ts: String,
)

@Serializable
data class LocationBatch(val points: List<LocationPoint>)

@Serializable
data class LocationBatchAck(val accepted: Int)

@Serializable
data class LiveMetricsDto(
    val status: String,
    val distanceKm: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val movingMinutes: Int = 0,
    val riderCount: Int = 0,
    val startedAt: String? = null,
    val endedAt: String? = null,
)
