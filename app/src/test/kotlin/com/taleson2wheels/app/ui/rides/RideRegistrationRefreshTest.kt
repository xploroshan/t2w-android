package com.taleson2wheels.app.ui.rides

import com.taleson2wheels.app.data.local.CacheStore
import com.taleson2wheels.app.data.local.ResponseCache
import com.taleson2wheels.app.data.remote.api.RidesApi
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RegisterRideRequest
import com.taleson2wheels.app.data.remote.dto.RideCard
import com.taleson2wheels.app.data.remote.dto.RideDetail
import com.taleson2wheels.app.data.remote.dto.RideDetailResponse
import com.taleson2wheels.app.data.remote.dto.RidePost
import com.taleson2wheels.app.data.remote.dto.RidePostInput
import com.taleson2wheels.app.data.remote.dto.RidePostResponse
import com.taleson2wheels.app.data.remote.dto.RideRegistrationDto
import com.taleson2wheels.app.data.remote.dto.RideRegistrationResponse
import com.taleson2wheels.app.data.repository.RidesRepository
import com.taleson2wheels.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Verifies the registration → refresh contract: a successful `register()` emits
 * on [RidesRepository.registrations], invalidates the stale ride/list caches, and
 * (via that signal) makes [RideDetailViewModel] re-fetch so the detail screen
 * stops offering "Register" and shows the updated rider count.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RideRegistrationRefreshTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    /** In-memory [CacheStore] so cache invalidation is observable without Room. */
    private class FakeStore : CacheStore {
        val map = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = map[key]
        override suspend fun write(key: String, json: String) { map[key] = json }
        override suspend fun delete(key: String) { map.remove(key) }
        override suspend fun clear() { map.clear() }
    }

    /** Backend that flips the viewer's registration state when register() is called. */
    private class FakeRidesApi : RidesApi {
        var registered = false
        var riderCount = 10
        var listCalls = 0

        override suspend fun list(cursor: String?, limit: Int, status: String?): Page<RideCard> {
            listCalls++
            return Page(
                items = listOf(
                    RideCard(
                        id = "r1", title = "Coorg Monsoon Run",
                        registeredRiders = riderCount,
                        myRegistrationStatus = if (registered) "pending" else null,
                    ),
                ),
                nextCursor = null,
            )
        }

        override suspend fun detail(id: String): RideDetailResponse =
            RideDetailResponse(
                RideDetail(
                    id = id, title = "Coorg Monsoon Run", status = "upcoming",
                    registeredRiders = riderCount, currentUserRegistered = registered,
                ),
            )

        override suspend fun register(id: String, body: RegisterRideRequest): RideRegistrationResponse {
            registered = true
            riderCount += 1
            return RideRegistrationResponse(
                RideRegistrationDto(id = "reg1", rideId = id, approvalStatus = "pending", confirmationCode = "ABC123"),
            )
        }

        override suspend fun posts(id: String, cursor: String?, limit: Int): Page<RidePost> = error("unused")
        override suspend fun createPost(id: String, body: RidePostInput): RidePostResponse = error("unused")
    }

    private fun repo(api: FakeRidesApi, store: FakeStore) =
        RidesRepository(api, json, ResponseCache(store, json))

    private val sampleBody = RegisterRideRequest(
        phone = "9999999999", agreedCancellationTerms = true, agreedIndemnity = true,
    )

    @Test
    fun ride_detail_reloads_and_flips_to_registered_after_registering() =
        runTest(mainDispatcher.dispatcher) {
            val api = FakeRidesApi()
            val repo = repo(api, FakeStore())
            val detail = RideDetailViewModel(repo)

            detail.load("r1")
            advanceUntilIdle()
            assertFalse("starts un-registered", detail.uiState.ride!!.currentUserRegistered)
            assertEquals(10, detail.uiState.ride!!.registeredRiders)

            // Register exactly as RegistrationViewModel would.
            repo.register("r1", sampleBody)
            advanceUntilIdle()

            assertTrue(
                "detail must re-fetch and reflect the registration on return",
                detail.uiState.ride!!.currentUserRegistered,
            )
            assertEquals("rider count must update", 11, detail.uiState.ride!!.registeredRiders)
        }

    @Test
    fun register_invalidates_the_stale_ride_and_list_caches() = runTest(mainDispatcher.dispatcher) {
        val api = FakeRidesApi()
        val store = FakeStore()
        val repo = repo(api, store)

        // Seed the caches the way the screens would have.
        repo.rides()
        repo.ride("r1")
        advanceUntilIdle()
        assertTrue("ride detail cached", store.map.containsKey("ride:r1"))
        assertTrue("first list page cached", store.map.containsKey("rides:first"))

        repo.register("r1", sampleBody)
        advanceUntilIdle()

        // Both now describe the pre-registration state, so they must be dropped —
        // otherwise an offline reload would re-serve the stale snapshot.
        assertNull("stale ride detail dropped", store.map["ride:r1"])
        assertNull("stale first list page dropped", store.map["rides:first"])
    }

    @Test
    fun only_the_default_page_size_is_cached_under_rides_first() = runTest(mainDispatcher.dispatcher) {
        val api = FakeRidesApi()
        val store = FakeStore()
        val repo = repo(api, store)

        // A non-default limit must bypass the cache so it can't alias / overwrite
        // the single "rides:first" snapshot (which is always a default-sized page).
        repo.rides(limit = 50)
        advanceUntilIdle()
        assertFalse("non-default page size must not touch the shared key", store.map.containsKey("rides:first"))

        // The default first page still caches.
        repo.rides()
        advanceUntilIdle()
        assertTrue("default first page is cached", store.map.containsKey("rides:first"))
    }

    @Test
    fun detail_does_not_reload_for_a_different_ride() = runTest(mainDispatcher.dispatcher) {
        val api = FakeRidesApi()
        val repo = repo(api, FakeStore())
        val detail = RideDetailViewModel(repo)

        detail.load("r1")
        advanceUntilIdle()
        val before = detail.uiState.ride

        // A registration for a DIFFERENT ride must not disturb this screen.
        repo.register("other-ride", sampleBody)
        advanceUntilIdle()

        assertEquals("unrelated registration must not reload this ride", before, detail.uiState.ride)
    }
}
