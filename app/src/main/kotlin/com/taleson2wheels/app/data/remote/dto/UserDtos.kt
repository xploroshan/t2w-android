package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Sanitized user (no password hash). The backend returns the full row plus
 * `linkedRiderId`; these are the stable fields a client should rely on
 * (`ignoreUnknownKeys` tolerates the rest).
 */
@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val avatar: String? = null,
    val role: String,
    val isApproved: Boolean = false,
    val city: String? = null,
    val ridingExperience: String? = null,
    val totalKm: Double = 0.0,
    val ridesCompleted: Int = 0,
    val linkedRiderId: String? = null,
    val motorcycles: List<MotorcycleDto> = emptyList(),
    val earnedBadges: List<EarnedBadgeDto> = emptyList(),
)

/** Leaderboard / crew entry (`/riders`, `/riders/{id}`). */
@Serializable
data class RiderDto(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val userRole: String? = null,
    val ridesCompleted: Int = 0,
    val totalKm: Double = 0.0,
    val totalPoints: Double = 0.0,
    val ridesOrganized: Int = 0,
    val sweepsDone: Int = 0,
    val pilotsDone: Int = 0,
    /** PII — present only for privileged/authenticated viewers. */
    val email: String? = null,
)

/** `/riders/{id}` wraps the rider in `{ "rider": ... }`. */
@Serializable
data class RiderResponse(val rider: RiderDto)
