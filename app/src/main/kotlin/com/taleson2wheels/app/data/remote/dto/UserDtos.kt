package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

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
    val joinDate: String? = null,
)

@Serializable
data class RiderDto(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val role: String? = null,
    val points: Double = 0.0,
    val ridesParticipated: Int = 0,
    val ridesOrganized: Int = 0,
    val sweepsDone: Int = 0,
    val pilotsDone: Int = 0,
    val joinDate: String? = null,
)
