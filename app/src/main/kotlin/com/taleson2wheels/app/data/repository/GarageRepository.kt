package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.GarageApi
import com.taleson2wheels.app.data.remote.dto.MotorcycleDto
import com.taleson2wheels.app.data.remote.dto.MotorcycleInput
import com.taleson2wheels.app.data.remote.safeApiCall
import kotlinx.serialization.json.Json

/** CRUD over the signed-in rider's garage (`/api/v1/motorcycles`). */
class GarageRepository(
    private val garageApi: GarageApi,
    private val json: Json,
) {
    suspend fun list(): ApiResult<List<MotorcycleDto>> =
        safeApiCall(json) { garageApi.list().motorcycles }

    suspend fun create(input: MotorcycleInput): ApiResult<MotorcycleDto> =
        safeApiCall(json) { garageApi.create(input).motorcycle }

    suspend fun update(id: String, input: MotorcycleInput): ApiResult<MotorcycleDto> =
        safeApiCall(json) { garageApi.update(id, input).motorcycle }

    suspend fun delete(id: String): ApiResult<Unit> =
        safeApiCall(json) { garageApi.delete(id) }
}
