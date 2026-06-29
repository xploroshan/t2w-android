package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.ContentApi
import com.taleson2wheels.app.data.remote.dto.BadgeDto
import com.taleson2wheels.app.data.remote.dto.CrewMemberDto
import com.taleson2wheels.app.data.remote.dto.GuidelineDto
import com.taleson2wheels.app.data.remote.dto.NotificationDto
import com.taleson2wheels.app.data.remote.dto.StatsDto
import com.taleson2wheels.app.data.remote.safeApiCall
import kotlinx.serialization.json.Json

/** Reads the read-only catalog endpoints (stats, guidelines, crew, badges, notifications). */
class CatalogRepository(
    private val contentApi: ContentApi,
    private val json: Json,
) {
    suspend fun stats(): ApiResult<StatsDto> =
        safeApiCall(json) { contentApi.stats() }

    suspend fun guidelines(): ApiResult<List<GuidelineDto>> =
        safeApiCall(json) { contentApi.guidelines().guidelines }

    suspend fun crew(): ApiResult<List<CrewMemberDto>> =
        safeApiCall(json) { contentApi.crew().crew }

    suspend fun badges(): ApiResult<List<BadgeDto>> =
        safeApiCall(json) { contentApi.badges().badges }

    suspend fun notifications(): ApiResult<List<NotificationDto>> =
        safeApiCall(json) { contentApi.notifications().notifications }
}
