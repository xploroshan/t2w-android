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

// ── Admin user management (GET/POST /api/v1/admin/users*) ────────────────────

/**
 * An account-holder in the admin users list. Mirrors the `AdminUser` schema.
 * `lastLoginAt` is present only when the requester is a super admin.
 */
@Serializable
data class AdminUser(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val isApproved: Boolean,
    val blocked: Boolean,
    val blockedAt: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val linkedRiderId: String? = null,
    val totalKm: Double = 0.0,
    val ridesCompleted: Int = 0,
    val createdAt: String? = null,
    val lastLoginAt: String? = null,
)

/** approve / block / role endpoints wrap the updated row in `{ "user": ... }`. */
@Serializable
data class AdminUserResponse(val user: AdminUser)

/** Body for the block endpoint. */
@Serializable
data class BlockBody(val blocked: Boolean)

/** Body for the set-role endpoint. */
@Serializable
data class RoleBody(val role: String)

/** `{ "id": ... }` returned by the reject endpoint. */
@Serializable
data class IdResponse(val id: String)
