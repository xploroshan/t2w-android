package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * List-view ride (`GET /rides` → `Page<RideCard>`). `status` is computed
 * server-side; `myRegistrationStatus` is null unless the bearer viewer is
 * registered.
 */
@Serializable
data class RideCard(
    val id: String,
    val title: String,
    val rideNumber: String? = null,
    val type: String? = null,
    val status: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val distanceKm: Double = 0.0,
    val difficulty: String? = null,
    val fee: Double = 0.0,
    val posterUrl: String? = null,
    val registeredRiders: Int = 0,
    val activeRegistrations: Int = 0,
    val myRegistrationStatus: String? = null,
)

/**
 * Detail-view ride (`GET /rides/{id}` → `{ "ride": RideDetail }`). A superset of
 * [RideCard] with crew, registrations and the viewer's status; the backend marks
 * it `additionalProperties: true`, so unmodeled fields are tolerated.
 */
@Serializable
data class RideDetail(
    val id: String,
    val title: String,
    val rideNumber: String? = null,
    val type: String? = null,
    val status: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val startLocation: String? = null,
    val endLocation: String? = null,
    val distanceKm: Double = 0.0,
    val difficulty: String? = null,
    val description: String? = null,
    val fee: Double = 0.0,
    val maxRiders: Int = 0,
    val posterUrl: String? = null,
    val registeredRiders: Int = 0,
    val activeRegistrations: Int = 0,
    val currentUserRegistered: Boolean = false,
    val currentUserApprovalStatus: String? = null,
)

/** `/rides/{id}` wraps the ride in `{ "ride": ... }`. */
@Serializable
data class RideDetailResponse(val ride: RideDetail)
