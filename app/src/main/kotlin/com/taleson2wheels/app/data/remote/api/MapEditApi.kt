package com.taleson2wheels.app.data.remote.api

import com.taleson2wheels.app.data.remote.dto.MapAddBreakRequest
import com.taleson2wheels.app.data.remote.dto.MapAuditResponse
import com.taleson2wheels.app.data.remote.dto.MapBreakResponse
import com.taleson2wheels.app.data.remote.dto.MapDeleteResponse
import com.taleson2wheels.app.data.remote.dto.MapDeletedResponse
import com.taleson2wheels.app.data.remote.dto.MapRevertRequest
import com.taleson2wheels.app.data.remote.dto.MapSmoothRequest
import com.taleson2wheels.app.data.remote.dto.MapSmoothResponse
import com.taleson2wheels.app.data.remote.dto.MapStatsResponse
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * `/api/v1/rides/{id}/live/map-edit/…` — the post-ride map editor (bearer +
 * canEditRideMap; the session must be paused/ended, never live).
 */
interface MapEditApi {

    // --- Breaks ---
    @POST("api/v1/rides/{id}/live/map-edit/breaks")
    suspend fun addBreak(@Path("id") id: String, @Body body: MapAddBreakRequest): MapBreakResponse

    @DELETE("api/v1/rides/{id}/live/map-edit/breaks/{breakId}")
    suspend fun deleteBreak(@Path("id") id: String, @Path("breakId") breakId: String): MapDeleteResponse

    // --- Stats overrides (body is a JsonObject so a cleared field can send null) ---
    @PATCH("api/v1/rides/{id}/live/map-edit/stats")
    suspend fun updateStats(@Path("id") id: String, @Body body: JsonObject): MapStatsResponse

    // --- Smooth & fill ---
    @POST("api/v1/rides/{id}/live/map-edit/smooth-track")
    suspend fun smoothTrack(
        @Path("id") id: String,
        @Body body: MapSmoothRequest,
        // "1" → preview (proposed track, not persisted); null → persist.
        @Query("preview") preview: String? = null,
    ): MapSmoothResponse

    // DELETE with a body needs @HTTP(hasBody = true) — @DELETE forbids @Body.
    @HTTP(method = "DELETE", path = "api/v1/rides/{id}/live/map-edit/smooth-track", hasBody = true)
    suspend fun revertSmooth(@Path("id") id: String, @Body body: MapRevertRequest): MapDeletedResponse

    // --- Audit ---
    @GET("api/v1/rides/{id}/live/map-edit/audit")
    suspend fun audit(@Path("id") id: String): MapAuditResponse
}
