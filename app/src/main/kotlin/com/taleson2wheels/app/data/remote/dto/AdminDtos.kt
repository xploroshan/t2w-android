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

// ── Admin ride CRUD (POST/PATCH/DELETE /api/v1/admin/rides*) ──────────────────

/**
 * Ride create/update payload (`RideInput` in `docs/openapi-v1.yaml`). Every field
 * is nullable so the app's Json (`explicitNulls = false`) omits the ones the
 * mobile editor doesn't manage — a partial PATCH then leaves the untouched
 * columns (route, highlights, crew, regFormSettings, reg windows, poster) intact
 * instead of wiping them. On create the backend applies its own defaults for the
 * omitted fields.
 *
 * The mobile editor deliberately covers only the round-trippable core fields;
 * richer editing (route/highlights/registration form/crew) stays on the web admin.
 */
@Serializable
data class RideInput(
    val title: String? = null,
    val type: String? = null,
    val status: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val startLocation: String? = null,
    val endLocation: String? = null,
    val distanceKm: Double? = null,
    val maxRiders: Int? = null,
    val difficulty: String? = null,
    val description: String? = null,
    val fee: Double? = null,
)

/** `{ "success": ..., "id": ... }` returned by the delete-ride endpoint. */
@Serializable
data class RideDeleteResponse(val id: String, val success: Boolean = false)

// ── Admin participation matrix (GET/PUT/PATCH .../rides/{id}/participation) ────

/** One participation row for a ride (`ParticipationRow`): rider + points + drop-out. */
@Serializable
data class ParticipationRow(
    val riderProfileId: String,
    val riderName: String,
    val avatarUrl: String? = null,
    val points: Double,
    val droppedOut: Boolean,
)

/** `GET .../participation` → `{ "items": [...] }`. */
@Serializable
data class ParticipationListResponse(val items: List<ParticipationRow> = emptyList())

/**
 * Body for `PUT .../participation`. An explicit non-positive [points] removes the
 * rider's participation; an omitted [points] (null → dropped by `explicitNulls`)
 * auto-awards by ride type.
 */
@Serializable
data class SetParticipationBody(val riderProfileId: String, val points: Double? = null)

/** `PUT .../participation` → `{ "action": "set" | "removed", "riderProfileId", "points"? }`. */
@Serializable
data class SetParticipationResult(
    val action: String? = null,
    val riderProfileId: String,
    val points: Double? = null,
)

/** Body for `PATCH .../participation` (super-admin only). */
@Serializable
data class DropOutBody(val riderProfileId: String, val droppedOut: Boolean)

/** `PATCH .../participation` → `{ "riderProfileId", "droppedOut" }`. */
@Serializable
data class DropOutResult(val riderProfileId: String, val droppedOut: Boolean)

// ── Admin badge CRUD (POST/PATCH/DELETE /api/v1/admin/badges*) ────────────────

/**
 * Badge create/update payload (`BadgeInput`). Create requires tier / name /
 * minKm; kind defaults to `lifetime_km` server-side. Every field is nullable so
 * the app's Json (`explicitNulls = false`) omits the untouched ones on a partial
 * PATCH. The badge *listing* stays on the public `GET /api/v1/badges`.
 */
@Serializable
data class BadgeInput(
    val tier: String? = null,
    val kind: String? = null,
    val name: String? = null,
    val description: String? = null,
    val minKm: Double? = null,
    val icon: String? = null,
    val color: String? = null,
)

/** create / update badge endpoints wrap the row in `{ "badge": ... }`. */
@Serializable
data class BadgeResponse(val badge: BadgeDto)

/** `{ "success": ..., "id": ... }` returned by the delete-badge endpoint. */
@Serializable
data class BadgeDeleteResponse(val id: String, val success: Boolean = false)

// ── Admin activity log (GET /api/v1/admin/activity-log) ───────────────────────

/**
 * One audit-trail entry (`ActivityLogEntry`), newest first. `rollbackData` (an
 * arbitrary parsed-JSON blob) is intentionally not modelled — the mobile viewer
 * is read-only and `ignoreUnknownKeys` drops it.
 */
@Serializable
data class ActivityLogEntry(
    val id: String,
    val action: String,
    val performedBy: String,
    val performedByName: String,
    val targetId: String,
    val targetName: String,
    val details: String? = null,
    val timestamp: String,
)
