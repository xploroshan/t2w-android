package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

// ── Garage ───────────────────────────────────────────────────────────────────

/** `POST /api/v1/upload` → `{ "url": ... }` (Vercel Blob public URL). */
@Serializable
data class UploadResponse(val url: String)

@Serializable
data class MotorcycleInput(
    val make: String,
    val model: String,
    val year: Int = 0,
    val cc: Int = 0,
    val color: String? = null,
    val nickname: String? = null,
    val imageUrl: String? = null,
)

/** Garage create/update wrap the bike in `{ "motorcycle": ... }`. */
@Serializable
data class MotorcycleResponse(val motorcycle: MotorcycleDto)

@Serializable
data class MotorcycleDto(
    val id: String,
    val make: String,
    val model: String,
    val year: Int = 0,
    val cc: Int = 0,
    val color: String? = null,
    val nickname: String? = null,
    val imageUrl: String? = null,
)

/** `GET /motorcycles` → `{ "motorcycles": [...] }`. */
@Serializable
data class MotorcyclesResponse(val motorcycles: List<MotorcycleDto> = emptyList())

// ── Badges ───────────────────────────────────────────────────────────────────

@Serializable
data class BadgeDto(
    val id: String,
    val tier: String? = null,
    val kind: String? = null,
    val name: String? = null,
    val description: String? = null,
    val minKm: Double = 0.0,
    val icon: String? = null,
    val color: String? = null,
)

/** A badge a user has earned (`User.earnedBadges`). */
@Serializable
data class EarnedBadgeDto(
    val id: String,
    val earnedDate: String? = null,
    val badge: BadgeDto? = null,
)

/** `GET /badges` → `{ "badges": [...] }`. */
@Serializable
data class BadgesResponse(val badges: List<BadgeDto> = emptyList())

// ── Content ──────────────────────────────────────────────────────────────────

@Serializable
data class GuidelineDto(
    val id: String,
    val title: String,
    val content: String,
    val category: String? = null,
    val icon: String? = null,
)

/** `GET /guidelines` → `{ "guidelines": [...] }`. */
@Serializable
data class GuidelinesResponse(val guidelines: List<GuidelineDto> = emptyList())

@Serializable
data class CrewMemberDto(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val role: String? = null,
)

/** `GET /crew` → `{ "crew": [...] }`. */
@Serializable
data class CrewResponse(val crew: List<CrewMemberDto> = emptyList())

@Serializable
data class NotificationDto(
    val id: String,
    val title: String,
    val message: String,
    val type: String? = null,
    val date: String? = null,
    val isRead: Boolean = false,
)

/** `GET /notifications` → `{ "notifications": [...] }`. */
@Serializable
data class NotificationsResponse(val notifications: List<NotificationDto> = emptyList())

/**
 * Body for `PUT /notifications` (mark-as-read). Omit [ids] (null) to mark ALL of
 * the caller's notifications read; otherwise only the listed ids are updated.
 */
@Serializable
data class MarkReadRequest(val ids: List<String>? = null)

/** `PUT /notifications` → `{ "success": true, "updated": n }`. */
@Serializable
data class MarkReadResponse(val success: Boolean = false, val updated: Int = 0)

// ── System ───────────────────────────────────────────────────────────────────

@Serializable
data class StatsDto(
    val activeRiders: Int = 0,
    val ridesCompleted: Int = 0,
    val kmsCovered: Long = 0,
    val countriesRidden: Int = 0,
)

@Serializable
data class HealthDto(
    val status: String,
    val timestamp: String? = null,
    val database: String? = null,
)
