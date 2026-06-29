package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.ItemList
import com.taleson2wheels.app.data.remote.dto.MotorcycleDto
import com.taleson2wheels.app.data.remote.dto.MotorcycleInput
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/** `/api/v1/motorcycles/…` — the signed-in rider's garage. */
interface GarageApi {

    @GET("motorcycles")
    suspend fun list(): ItemList<MotorcycleDto>

    @POST("motorcycles")
    suspend fun create(@Body body: MotorcycleInput): MotorcycleDto

    @PATCH("motorcycles/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body body: MotorcycleInput,
    ): MotorcycleDto

    @DELETE("motorcycles/{id}")
    suspend fun delete(@Path("id") id: String)
}
