package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * A registration in the moderation queue
 * (`GET /api/v1/admin/registrations` → `Page<RegistrationModeration>`, and the
 * item returned by the approve/reject endpoint). Mirrors the `RegistrationModeration`
 * schema in `docs/openapi-v1.yaml`.
 */
@Serializable
data class RegistrationModeration(
    val id: String,
    val userId: String,
    val rideId: String,
    val riderName: String,
    val email: String? = null,
    val phone: String? = null,
    val approvalStatus: String,
    val accommodationType: String? = null,
    val confirmationCode: String? = null,
    val registeredAt: String? = null,
)

/** `POST /api/v1/admin/registrations/{id}` wraps the updated row in `{ "registration": ... }`. */
@Serializable
data class RegistrationModerationResponse(val registration: RegistrationModeration)

/**
 * Body for the moderation endpoints (registrations, blogs, ride-posts):
 * `approve` confirms/approves, `reject` rejects. Mirrors `ModerationAction`.
 */
@Serializable
data class ModerationAction(val action: String)
