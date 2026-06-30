package com.taleson2wheels.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * One rider's standing in the current achievement period
 * (`GET /api/v1/achievements`). The endpoint returns an open-ended object; the
 * fields below are the stable subset the app renders (`ignoreUnknownKeys`
 * tolerates the rest).
 */
@Serializable
data class AchievementRider(
    val id: String,
    val name: String = "",
    val avatarUrl: String? = null,
    val userRole: String? = null,
    val ridesCompletedInPeriod: Int = 0,
    val ridesOrganizedInPeriod: Int = 0,
    val sweepsDoneInPeriod: Int = 0,
    val totalPts: Double = 0.0,
    val percentageAchieved: Double = 0.0,
    val highlighted: Boolean = false,
)

/** `GET /api/v1/achievements` — the period "arena" standings. */
@Serializable
data class AchievementsResponse(
    val configured: Boolean = false,
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val thresholdPercent: Double = 0.0,
    val totalRidesInPeriod: Int = 0,
    val riders: List<AchievementRider> = emptyList(),
)
