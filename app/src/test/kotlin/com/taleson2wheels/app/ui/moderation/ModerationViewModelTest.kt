package com.taleson2wheels.app.ui.moderation

import com.taleson2wheels.app.data.remote.api.AdminApi
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.remote.dto.BlogResponse
import com.taleson2wheels.app.data.remote.dto.ModerationAction
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RegistrationModeration
import com.taleson2wheels.app.data.remote.dto.RegistrationModerationResponse
import com.taleson2wheels.app.data.remote.dto.RidePost
import com.taleson2wheels.app.data.remote.dto.RidePostResponse
import com.taleson2wheels.app.data.repository.AdminRepository
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
class ModerationViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeAdminApi : AdminApi {
        var page: Page<RegistrationModeration> = Page(items = emptyList(), nextCursor = null)
        val moderateCalls = mutableListOf<Pair<String, String>>()
        var moderateThrows: Throwable? = null

        override suspend fun registrations(status: String?, rideId: String?, cursor: String?, limit: Int) = page

        override suspend fun moderateRegistration(id: String, body: ModerationAction): RegistrationModerationResponse {
            moderateCalls += id to body.action
            moderateThrows?.let { throw it }
            val newStatus = if (body.action == "approve") "confirmed" else "rejected"
            return RegistrationModerationResponse(reg(id).copy(approvalStatus = newStatus))
        }

        // Content queues are unused in this suite (covered by ContentModerationViewModelTest).
        override suspend fun moderationBlogs(status: String?, cursor: String?, limit: Int): Page<BlogCard> =
            Page(items = emptyList(), nextCursor = null)

        override suspend fun moderateBlog(id: String, body: ModerationAction): BlogResponse =
            throw UnsupportedOperationException("not used")

        override suspend fun moderationRidePosts(status: String?, rideId: String?, cursor: String?, limit: Int): Page<RidePost> =
            Page(items = emptyList(), nextCursor = null)

        override suspend fun moderateRidePost(id: String, body: ModerationAction): RidePostResponse =
            throw UnsupportedOperationException("not used")

        // User-management endpoints unused in this suite.
        override suspend fun users(status: String?, cursor: String?, limit: Int) =
            Page<com.taleson2wheels.app.data.remote.dto.AdminUser>(items = emptyList(), nextCursor = null)
        override suspend fun approveUser(id: String) = throw UnsupportedOperationException("not used")
        override suspend fun rejectUser(id: String) = throw UnsupportedOperationException("not used")
        override suspend fun blockUser(id: String, body: com.taleson2wheels.app.data.remote.dto.BlockBody) =
            throw UnsupportedOperationException("not used")
        override suspend fun setUserRole(id: String, body: com.taleson2wheels.app.data.remote.dto.RoleBody) =
            throw UnsupportedOperationException("not used")

        // Ride CRUD + participation unused in this suite.
        override suspend fun createRide(body: com.taleson2wheels.app.data.remote.dto.RideInput) =
            throw UnsupportedOperationException("not used")
        override suspend fun updateRide(id: String, body: com.taleson2wheels.app.data.remote.dto.RideInput) =
            throw UnsupportedOperationException("not used")
        override suspend fun deleteRide(id: String) = throw UnsupportedOperationException("not used")
        override suspend fun participation(id: String) = throw UnsupportedOperationException("not used")
        override suspend fun setParticipation(id: String, body: com.taleson2wheels.app.data.remote.dto.SetParticipationBody) =
            throw UnsupportedOperationException("not used")
        override suspend fun setParticipationDroppedOut(id: String, body: com.taleson2wheels.app.data.remote.dto.DropOutBody) =
            throw UnsupportedOperationException("not used")
    }

    private val fake = FakeAdminApi()
    private fun vm() = ModerationViewModel(AdminRepository(fake, json))

    companion object {
        fun reg(id: String) = RegistrationModeration(
            id = id, userId = "u-$id", rideId = "r1", riderName = "Rider $id", approvalStatus = "pending",
        )
    }

    private fun idsOf(model: ModerationViewModel) = model.uiState.items.map { it.id }

    @Test
    fun loads_pending_registrations_on_init() = runTest(mainDispatcher.dispatcher) {
        fake.page = Page(items = listOf(reg("a"), reg("b")), nextCursor = null)
        val model = vm(); advanceUntilIdle()
        assertFalse(model.uiState.isLoading)
        assertEquals(listOf("a", "b"), idsOf(model))
    }

    @Test
    fun approve_removes_the_row_optimistically_and_calls_the_api_with_approve() = runTest(mainDispatcher.dispatcher) {
        fake.page = Page(items = listOf(reg("a"), reg("b")), nextCursor = null)
        val model = vm(); advanceUntilIdle()

        model.moderate("a", approve = true); advanceUntilIdle()

        assertEquals(listOf("b"), idsOf(model))
        assertEquals(listOf("a" to "approve"), fake.moderateCalls)
    }

    @Test
    fun reject_sends_the_reject_action() = runTest(mainDispatcher.dispatcher) {
        fake.page = Page(items = listOf(reg("a")), nextCursor = null)
        val model = vm(); advanceUntilIdle()

        model.moderate("a", approve = false); advanceUntilIdle()

        assertEquals("reject", fake.moderateCalls.single().second)
        assertTrue(idsOf(model).isEmpty())
    }

    @Test
    fun a_failed_action_restores_the_row_and_surfaces_an_error() = runTest(mainDispatcher.dispatcher) {
        fake.page = Page(items = listOf(reg("a"), reg("b")), nextCursor = null)
        val model = vm(); advanceUntilIdle()
        fake.moderateThrows = IOException("boom")

        model.moderate("a", approve = true); advanceUntilIdle()

        assertTrue("the row must come back on failure", idsOf(model).contains("a"))
        assertNotNull(model.uiState.actionError)
    }
}
