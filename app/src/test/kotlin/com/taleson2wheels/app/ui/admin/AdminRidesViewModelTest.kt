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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AdminRidesViewModelTest {

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

    private class FakeRidesApi : RidesApi {
        var page: Page<RideCard> = Page(items = emptyList(), nextCursor = null)
        override suspend fun list(cursor: String?, limit: Int, status: String?): Page<RideCard> = page
        override suspend fun detail(id: String): RideDetailResponse = error("unused")
        override suspend fun register(id: String, body: RegisterRideRequest): RideRegistrationResponse = error("unused")
        override suspend fun posts(id: String, cursor: String?, limit: Int): Page<RidePost> = error("unused")
        override suspend fun createPost(id: String, body: RidePostInput): RidePostResponse = error("unused")
    }

    private class FakeAdminApi : AdminApi {
        val deleteCalls = mutableListOf<String>()
        var deleteThrows: Throwable? = null

        override suspend fun deleteRide(id: String): RideDeleteResponse {
            deleteCalls += id
            deleteThrows?.let { throw it }
            return RideDeleteResponse(id = id, success = true)
        }

        // Everything else is unused in this suite.
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
        override suspend fun createRide(body: RideInput) = throw UnsupportedOperationException()
        override suspend fun updateRide(id: String, body: RideInput) = throw UnsupportedOperationException()
        override suspend fun participation(id: String) = throw UnsupportedOperationException()
        override suspend fun setParticipation(id: String, body: SetParticipationBody) = throw UnsupportedOperationException()
        override suspend fun setParticipationDroppedOut(id: String, body: DropOutBody) = throw UnsupportedOperationException()
    }

    private val rides = FakeRidesApi()
    private val admin = FakeAdminApi()
    private fun vm() = AdminRidesViewModel(
        RidesRepository(rides, json, ResponseCache(FakeStore(), json)),
        AdminRepository(admin, json),
    )

    private fun ride(id: String) = RideCard(id = id, title = "Ride $id", status = "upcoming")
    private fun idsOf(m: AdminRidesViewModel) = m.uiState.items.map { it.id }

    @Test
    fun loads_all_rides_on_init() = runTest(mainDispatcher.dispatcher) {
        rides.page = Page(items = listOf(ride("a"), ride("b")), nextCursor = "cur")
        val m = vm(); advanceUntilIdle()
        assertFalse(m.uiState.isLoading)
        assertEquals(listOf("a", "b"), idsOf(m))
        assertEquals("cur", m.uiState.nextCursor)
    }

    @Test
    fun deleting_a_ride_removes_the_row() = runTest(mainDispatcher.dispatcher) {
        rides.page = Page(items = listOf(ride("a"), ride("b")), nextCursor = null)
        val m = vm(); advanceUntilIdle()

        m.deleteRide("a"); advanceUntilIdle()

        assertEquals(listOf("b"), idsOf(m))
        assertEquals(listOf("a"), admin.deleteCalls)
    }

    @Test
    fun a_failed_delete_keeps_the_row_and_surfaces_an_error() = runTest(mainDispatcher.dispatcher) {
        rides.page = Page(items = listOf(ride("a")), nextCursor = null)
        val m = vm(); advanceUntilIdle()
        admin.deleteThrows = IOException("boom")

        m.deleteRide("a"); advanceUntilIdle()

        assertTrue("the row must stay on failure", idsOf(m).contains("a"))
        assertNotNull(m.uiState.actionError)
    }
}
