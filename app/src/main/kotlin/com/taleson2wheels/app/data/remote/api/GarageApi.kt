package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.MotorcycleInput
import com.taleson2wheels.app.data.remote.dto.MotorcycleResponse
import com.taleson2wheels.app.data.remote.dto.MotorcyclesResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * `/api/v1/motorcycles` — the signed-in rider's garage. Full CRUD: list, add,
 * edit, and remove. Ownership is enforced server-side; a bike the caller does
 * not own is reported as 404.
 */
interface GarageApi {

    @GET("api/v1/motorcycles")
    suspend fun list(): MotorcyclesResponse

    @POST("api/v1/motorcycles")
    suspend fun create(@Body body: MotorcycleInput): MotorcycleResponse

    @PATCH("api/v1/motorcycles/{id}")
    suspend fun update(@Path("id") id: String, @Body body: MotorcycleInput): MotorcycleResponse

    @DELETE("api/v1/motorcycles/{id}")
    suspend fun delete(@Path("id") id: String)
}
