package com.taleson2wheels.app.data.repository

import com.taleson2wheels.app.data.local.ResponseCache
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.RidesApi
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RegisterRideRequest
import com.taleson2wheels.app.data.remote.dto.RideCard
import com.taleson2wheels.app.data.remote.dto.RideDetail
import com.taleson2wheels.app.data.remote.dto.RidePost
import com.taleson2wheels.app.data.remote.dto.RidePostInput
import com.taleson2wheels.app.data.remote.dto.RideRegistrationDto
import com.taleson2wheels.app.data.remote.safeApiCall
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json

/**
 * Reads the cursor-paginated ride list and ride detail from `/api/v1/rides`.
 *
 * The default ride feed (first page) and each ride's detail are cached via
 * [ResponseCache], so a returning user sees last-known content instantly and the
 * app still renders those screens when the device is offline.
 */
class RidesRepository(
    private val ridesApi: RidesApi,
    private val json: Json,
    private val cache: ResponseCache,
) {
    // Emits the rideId whenever the current user successfully registers for a
    // ride. The ride detail / rides list / home dashboard observe this so they can
    // refresh their now-stale, viewer-specific state (the "Register" CTA, the
    // registeredRiders count, myRegistrationStatus) instead of showing the
    // pre-registration snapshot until the next manual pull-to-refresh.
    private val _registrations = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val registrations: SharedFlow<String> = _registrations.asSharedFlow()

    suspend fun rides(
        cursor: String? = null,
        limit: Int = 20,
        status: String? = null,
    ): ApiResult<Page<RideCard>> {
        val call: suspend () -> ApiResult<Page<RideCard>> = {
            safeApiCall(json) { ridesApi.list(cursor = cursor, limit = limit, status = status) }
        }
        // Only the default first page is cacheable — cursors and status filters
        // are request-specific and shouldn't shadow the offline snapshot.
        return if (cursor == null && status == null) {
            cache.networkWithFallback(
                key = CACHE_RIDES_FIRST,
                serializer = Page.serializer(RideCard.serializer()),
                networkCall = call,
            )
        } else {
            call()
        }
    }

    suspend fun ride(id: String): ApiResult<RideDetail> =
        cache.networkWithFallback(
            key = "ride:$id",
            serializer = RideDetail.serializer(),
        ) {
            safeApiCall(json) { ridesApi.detail(id).ride }
        }

    suspend fun register(rideId: String, body: RegisterRideRequest): ApiResult<RideRegistrationDto> {
        val result = safeApiCall(json) { ridesApi.register(rideId, body).registration }
        if (result is ApiResult.Success) {
            // The cached ride detail + first list page still describe the
            // pre-registration state (currentUserRegistered=false, old counts).
            // Drop them so the next read re-fetches, then notify observers.
            cache.invalidate("ride:$rideId")
            cache.invalidate(CACHE_RIDES_FIRST)
            _registrations.tryEmit(rideId)
        }
        return result
    }

    suspend fun posts(rideId: String, cursor: String? = null, limit: Int = 20): ApiResult<Page<RidePost>> =
        safeApiCall(json) { ridesApi.posts(rideId, cursor = cursor, limit = limit) }

    suspend fun createPost(rideId: String, input: RidePostInput): ApiResult<RidePost> =
        safeApiCall(json) { ridesApi.createPost(rideId, input).post }

    private companion object {
        const val CACHE_RIDES_FIRST = "rides:first"
    }
}
