package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.MotorcyclesResponse
import retrofit2.http.GET

/**
 * `/api/v1/motorcycles` — the signed-in rider's garage (read-only for now;
 * create/update/delete are not yet in the backend contract).
 */
interface GarageApi {

    @GET("api/v1/motorcycles")
    suspend fun list(): MotorcyclesResponse
}
