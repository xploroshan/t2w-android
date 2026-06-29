package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MotorcycleDto(
    val id: String,
    val make: String,
    val model: String,
    val year: Int,
    val cc: Int,
    val color: String,
    val nickname: String? = null,
    val imageUrl: String? = null,
)

@Serializable
data class MotorcycleInput(
    val make: String,
    val model: String,
    val year: Int,
    val cc: Int,
    val color: String,
    val nickname: String? = null,
    val imageUrl: String? = null,
)

@Serializable
data class BadgeDto(
    val id: String,
    val tier: String,
    val kind: String? = null,
    val name: String,
    val description: String,
    val minKm: Double = 0.0,
    val icon: String,
    val color: String,
    val earned: Boolean = false,
    val earnedDate: String? = null,
)

@Serializable
data class BlogPostDto(
    val id: String,
    val title: String,
    val excerpt: String,
    val content: String? = null,
    val authorName: String,
    val authorAvatar: String? = null,
    val coverImage: String? = null,
    val tags: List<String> = emptyList(),
    val type: String,
    val isVlog: Boolean = false,
    val videoUrl: String? = null,
    val readTime: Int = 0,
    val likes: Int = 0,
    val publishDate: String,
)

@Serializable
data class RidePostDto(
    val id: String,
    val rideId: String,
    val authorName: String,
    val content: String,
    val images: List<String> = emptyList(),
    val approvalStatus: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class NotificationDto(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val date: String,
    val isRead: Boolean = false,
)

@Serializable
data class GuidelineDto(
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val icon: String? = null,
)

@Serializable
data class StatsDto(
    val activeRiders: Int = 0,
    val completedRides: Int = 0,
    val totalKm: Double = 0.0,
    val countries: Int = 0,
)

@Serializable
data class HealthDto(
    val status: String,
    val version: String,
    val time: String? = null,
)

// ── Devices (push registration) ──────────────────────────────────────────────

@Serializable
data class DeviceRegistration(
    val token: String,
    val platform: String = "android",
    val deviceId: String,
    val appBuild: String? = null,
)

@Serializable
data class DeviceDto(
    val id: String,
    val platform: String,
    val deviceId: String,
    val appBuild: String? = null,
    val createdAt: String? = null,
)

// ── Simple list envelopes (non-paginated `{ "items": [...] }`) ────────────────

@Serializable
data class ItemList<T>(val items: List<T> = emptyList())
