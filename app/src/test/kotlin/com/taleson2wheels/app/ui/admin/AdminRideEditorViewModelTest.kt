package com.taleson2wheels.app.ui.admin

import com.taleson2wheels.app.data.local.CacheStore
import com.taleson2wheels.app.data.local.ResponseCache
import com.taleson2wheels.app.data.remote.api.AdminApi
import com.taleson2wheels.app.data.remote.api.RidesApi
import com.taleson2wheels.app.data.remote.dto.AdminUser
import com.taleson2wheels.app.data.remote.dto.BlockBody
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.remote.dto.DropOutBody
import com.taleson2wheels.app.data.remote.dto.ModerationAction
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RegisterRideRequest
import com.taleson2wheels.app.data.remote.dto.RideCard
import com.taleson2wheels.app.data.remote.dto.RideDeleteResponse
import com.taleson2wheels.app.data.remote.dto.RideDetail
import com.taleson2wheels.app.data.remote.dto.RideDetailResponse
import com.taleson2wheels.app.data.remote.dto.RideInput
import com.taleson2wheels.app.data.remote.dto.RidePost
import com.taleson2wheels.app.data.remote.dto.RidePostInput
import com.taleson2wheels.app.data.remote.dto.RidePostResponse
import com.taleson2wheels.app.data.remote.dto.RideRegistrationResponse
import com.taleson2wheels.app.data.remote.dto.RoleBody
import com.taleson2wheels.app.data.remote.dto.SetParticipationBody
import com.taleson2wheels.app.data.repository.AdminRepository
import com.taleson2wheels.app.data.repository.RidesRepository
import com.taleson2wheels.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminRideEditorViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeStore : CacheStore {
        val map = mutableMapOf<String, String>()
        override suspend fun read(key: String): String? = map[key]
        override suspend fun write(key: String, json: String) { map[key] = json }
        override suspend fun delete(key: String) { map.remove(key) }
        override suspend fun clear() { map.clear() }
    }

    private class FakeRidesApi(private val detail: RideDetail?) : RidesApi {
        override suspend fun detail(id: String): RideDetailResponse =
            RideDetailResponse(detail ?: error("no ride"))
        override suspend fun list(cursor: String?, limit: Int, status: String?): Page<RideCard> = error("unused")
        override suspend fun register(id: String, body: RegisterRideRequest): RideRegistrationResponse = error("unused")
        override suspend fun posts(id: String, cursor: String?, limit: Int): Page<RidePost> = error("unused")
        override suspend fun createPost(id: String, body: RidePostInput): RidePostResponse = error("unused")
    }

    private class FakeAdminApi : AdminApi {
        var lastCreate: RideInput? = null
        var lastUpdate: Pair<String, RideInput>? = null

        override suspend fun createRide(body: RideInput): RideDetailResponse {
            lastCreate = body
            return RideDetailResponse(RideDetail(id = "new-id", title = body.title ?: ""))
        }

        override suspend fun updateRide(id: String, body: RideInput): RideDetailResponse {
            lastUpdate = id to body
            return RideDetailResponse(RideDetail(id = id, title = body.title ?: ""))
        }

        // Unused in this suite.
        override suspend fun deleteRide(id: String) = RideDeleteResponse(id = id, success = true)
        override suspend fun registrations(status: String?, rideId: String?, cursor: String?, limit: Int) =
            Page<com.taleson2wheels.app.data.remote.dto.RegistrationModeration>(items = emptyList(), nextCursor = null)
        override suspend fun moderateRegistration(id: String, body: ModerationAction) = throw UnsupportedOperationException()
        override suspend fun moderationBlogs(status: String?, cursor: String?, limit: Int) =
            Page<BlogCard>(items = emptyList(), nextCursor = null)
        override suspend fun moderateBlog(id: String, body: ModerationAction) = throw UnsupportedOperationException()
        override suspend fun moderationRidePosts(status: String?, rideId: String?, cursor: String?, limit: Int) =
            Page<RidePost>(items = emptyList(), nextCursor = null)
        override suspend fun moderateRidePost(id: String, body: ModerationAction) = throw UnsupportedOperationException()
        override suspend fun users(status: String?, cursor: String?, limit: Int) =
            Page<AdminUser>(items = emptyList(), nextCursor = null)
        override suspend fun approveUser(id: String) = throw UnsupportedOperationException()
        override suspend fun rejectUser(id: String) = throw UnsupportedOperationException()
        override suspend fun blockUser(id: String, body: BlockBody) = throw UnsupportedOperationException()
        override suspend fun setUserRole(id: String, body: RoleBody) = throw UnsupportedOperationException()
        override suspend fun participation(id: String) = throw UnsupportedOperationException()
        override suspend fun setParticipation(id: String, body: SetParticipationBody) = throw UnsupportedOperationException()
        override suspend fun setParticipationDroppedOut(id: String, body: DropOutBody) = throw UnsupportedOperationException()
    }

    private val admin = FakeAdminApi()
    private fun vm(detail: RideDetail? = null): AdminRideEditorViewModel {
        val ridesApi = FakeRidesApi(detail)
        return AdminRideEditorViewModel(
            RidesRepository(ridesApi, json, ResponseCache(FakeStore(), json)),
            AdminRepository(admin, json),
        )
    }

    @Test
    fun validation_blocks_create_when_required_fields_are_missing() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load(null); advanceUntilIdle()

        m.save(); advanceUntilIdle()

        assertNotNull(m.uiState.validationError)
        assertNull("no API call on an invalid form", admin.lastCreate)
    }

    @Test
    fun create_sends_the_input_and_marks_saved() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load(null); advanceUntilIdle()
        m.onForm {
            copy(
                title = "Coorg Run",
                startDateMillis = 1_700_000_000_000L,
                endDateMillis = 1_700_100_000_000L,
                startLocation = "Bangalore",
                endLocation = "Coorg",
                distanceKm = "260",
                maxRiders = "30",
            )
        }

        m.save(); advanceUntilIdle()

        val sent = admin.lastCreate
        assertNotNull(sent)
        assertEquals("Coorg Run", sent!!.title)
        assertEquals(260.0, sent.distanceKm!!, 0.0)
        assertEquals(30, sent.maxRiders)
        assertNotNull(sent.startDate)
        assertNotNull(sent.endDate)
        assertTrue(m.uiState.saved)
    }

    @Test
    fun edit_mode_prefills_from_ride_detail() = runTest(mainDispatcher.dispatcher) {
        val detail = RideDetail(
            id = "r1", title = "Existing", rideNumber = "#007", type = "weekend", status = "upcoming",
            startDate = "2026-08-01T00:00:00Z", endDate = "2026-08-02T00:00:00Z",
            startLocation = "A", endLocation = "B", distanceKm = 120.0, difficulty = "easy",
        )
        val m = vm(detail); m.load("r1"); advanceUntilIdle()

        assertEquals("Existing", m.uiState.form.title)
        assertEquals("weekend", m.uiState.form.type)
        assertEquals("A", m.uiState.form.startLocation)
        assertEquals("120", m.uiState.form.distanceKm)
        assertNotNull(m.uiState.form.startDateMillis)
        assertEquals("#007", m.uiState.rideNumber)
        assertTrue(m.uiState.isEdit)
    }

    @Test
    fun edit_sends_update_and_marks_saved() = runTest(mainDispatcher.dispatcher) {
        val detail = RideDetail(
            id = "r1", title = "Existing", status = "upcoming",
            startDate = "2026-08-01T00:00:00Z", endDate = "2026-08-02T00:00:00Z",
            startLocation = "A", endLocation = "B",
        )
        val m = vm(detail); m.load("r1"); advanceUntilIdle()
        m.onForm { copy(title = "Renamed") }

        m.save(); advanceUntilIdle()

        assertEquals("r1", admin.lastUpdate?.first)
        assertEquals("Renamed", admin.lastUpdate?.second?.title)
        assertTrue(m.uiState.saved)
    }
}
