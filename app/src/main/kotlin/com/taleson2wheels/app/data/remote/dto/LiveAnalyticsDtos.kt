package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Post-ride analytics (`GET /rides/{id}/live/analytics`). The server returns a
 * rich, open-ended object (`splits`, `climb`, `elevation`, `cohesion`,
 * `leaderboard`); we model the well-structured `leaderboard` and tolerate the
 * rest via `ignoreUnknownKeys`. Headline elevation/distance come from the
 * separate, strongly-typed metrics endpoint.
 */
@Serializable
data class LiveAnalytics(
    val viewUserId: String? = null,
    val leaderboard: List<AnalyticsRider> = emptyList(),
)

/** One rider's post-ride standing, sorted by distance server-side. */
@Serializable
data class AnalyticsRider(
    val userId: String = "",
    val name: String = "",
    val avatar: String? = null,
    val distanceKm: Double = 0.0,
    val movingMinutes: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
)
