package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Body for `POST /api/v1/devices` — upserts on (user, deviceId). */
@Serializable
data class DeviceRegistration(
    val token: String,
    val platform: String = "android",
    val deviceId: String,
    val appBuild: String? = null,
)

/** Sanitized device row (`{ device }`) — no userId/token echoed back. */
@Serializable
data class Device(
    val id: String,
    val platform: String = "",
    val deviceId: String = "",
    val appBuild: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class DeviceResponse(val device: Device)
