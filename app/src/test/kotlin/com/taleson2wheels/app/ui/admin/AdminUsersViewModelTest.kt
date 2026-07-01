package com.taleson2wheels.app.ui.admin

import com.taleson2wheels.app.data.remote.api.AdminApi
import com.taleson2wheels.app.data.remote.dto.AdminUser
import com.taleson2wheels.app.data.remote.dto.AdminUserResponse
import com.taleson2wheels.app.data.remote.dto.BlockBody
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.remote.dto.BlogResponse
import com.taleson2wheels.app.data.remote.dto.IdResponse
import com.taleson2wheels.app.data.remote.dto.ModerationAction
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RegistrationModeration
import com.taleson2wheels.app.data.remote.dto.RegistrationModerationResponse
import com.taleson2wheels.app.data.remote.dto.RidePost
import com.taleson2wheels.app.data.remote.dto.RidePostResponse
import com.taleson2wheels.app.data.remote.dto.RoleBody
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
class AdminUsersViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeAdminApi : AdminApi {
        var page: Page<AdminUser> = Page(items = emptyList(), nextCursor = null)
        var lastStatus: String? = "unset"
        val approveCalls = mutableListOf<String>()
        val rejectCalls = mutableListOf<String>()
        val blockCalls = mutableListOf<Pair<String, Boolean>>()
        val roleCalls = mutableListOf<Pair<String, String>>()
        var actionThrows: Throwable? = null

        override suspend fun users(status: String?, cursor: String?, limit: Int): Page<AdminUser> {
            lastStatus = status
            return page
        }

        override suspend fun approveUser(id: String): AdminUserResponse {
            approveCalls += id
            actionThrows?.let { throw it }
            return AdminUserResponse(user(id).copy(isApproved = true))
        }

        override suspend fun rejectUser(id: String): IdResponse {
            rejectCalls += id
            actionThrows?.let { throw it }
            return IdResponse(id)
        }

        override suspend fun blockUser(id: String, body: BlockBody): AdminUserResponse {
            blockCalls += id to body.blocked
            actionThrows?.let { throw it }
            return AdminUserResponse(user(id).copy(blocked = body.blocked))
        }

        override suspend fun setUserRole(id: String, body: RoleBody): AdminUserResponse {
            roleCalls += id to body.role
            actionThrows?.let { throw it }
            return AdminUserResponse(user(id).copy(role = body.role))
        }

        // Moderation endpoints unused here.
        override suspend fun registrations(status: String?, rideId: String?, cursor: String?, limit: Int) =
            Page<RegistrationModeration>(items = emptyList(), nextCursor = null)
        override suspend fun moderateRegistration(id: String, body: ModerationAction): RegistrationModerationResponse =
            throw UnsupportedOperationException()
        override suspend fun moderationBlogs(status: String?, cursor: String?, limit: Int) =
            Page<BlogCard>(items = emptyList(), nextCursor = null)
        override suspend fun moderateBlog(id: String, body: ModerationAction): BlogResponse = throw UnsupportedOperationException()
        override suspend fun moderationRidePosts(status: String?, rideId: String?, cursor: String?, limit: Int) =
            Page<RidePost>(items = emptyList(), nextCursor = null)
        override suspend fun moderateRidePost(id: String, body: ModerationAction): RidePostResponse = throw UnsupportedOperationException()

        // Ride CRUD + participation unused here.
        override suspend fun createRide(body: com.taleson2wheels.app.data.remote.dto.RideInput) = throw UnsupportedOperationException()
        override suspend fun updateRide(id: String, body: com.taleson2wheels.app.data.remote.dto.RideInput) = throw UnsupportedOperationException()
        override suspend fun deleteRide(id: String) = throw UnsupportedOperationException()
        override suspend fun participation(id: String) = throw UnsupportedOperationException()
        override suspend fun setParticipation(id: String, body: com.taleson2wheels.app.data.remote.dto.SetParticipationBody) = throw UnsupportedOperationException()
        override suspend fun setParticipationDroppedOut(id: String, body: com.taleson2wheels.app.data.remote.dto.DropOutBody) = throw UnsupportedOperationException()
        override suspend fun createBadge(body: com.taleson2wheels.app.data.remote.dto.BadgeInput) = throw UnsupportedOperationException()
        override suspend fun updateBadge(id: String, body: com.taleson2wheels.app.data.remote.dto.BadgeInput) = throw UnsupportedOperationException()
        override suspend fun deleteBadge(id: String) = throw UnsupportedOperationException()
        override suspend fun activityLog(cursor: String?, limit: Int) = throw UnsupportedOperationException()
    }

    private val fake = FakeAdminApi()
    private fun vm() = AdminUsersViewModel(AdminRepository(fake, json))

    companion object {
        fun user(id: String, isApproved: Boolean = false, blocked: Boolean = false, role: String = "rider") =
            AdminUser(id = id, name = "User $id", email = "$id@t2w.com", role = role, isApproved = isApproved, blocked = blocked)
    }

    private fun idsOf(m: AdminUsersViewModel) = m.uiState.items.map { it.id }

    @Test
    fun loads_the_pending_queue_on_init() = runTest(mainDispatcher.dispatcher) {
        fake.page = Page(items = listOf(user("a"), user("b")), nextCursor = null)
        val m = vm(); advanceUntilIdle()
        assertFalse(m.uiState.isLoading)
        assertEquals(listOf("a", "b"), idsOf(m))
        assertEquals("pending", fake.lastStatus)
    }

    @Test
    fun approving_in_the_pending_view_removes_the_row() = runTest(mainDispatcher.dispatcher) {
        fake.page = Page(items = listOf(user("a"), user("b")), nextCursor = null)
        val m = vm(); advanceUntilIdle()

        m.approve("a"); advanceUntilIdle()

        assertEquals(listOf("b"), idsOf(m))
        assertEquals(listOf("a"), fake.approveCalls)
    }

    @Test
    fun rejecting_removes_the_row() = runTest(mainDispatcher.dispatcher) {
        fake.page = Page(items = listOf(user("a")), nextCursor = null)
        val m = vm(); advanceUntilIdle()

        m.reject("a"); advanceUntilIdle()

        assertTrue(idsOf(m).isEmpty())
        assertEquals(listOf("a"), fake.rejectCalls)
    }

    @Test
    fun blocking_updates_the_row_in_place() = runTest(mainDispatcher.dispatcher) {
        fake.page = Page(items = listOf(user("a", isApproved = true)), nextCursor = null)
        val m = vm(); advanceUntilIdle()

        m.setBlocked("a", true); advanceUntilIdle()

        assertEquals(listOf("a" to true), fake.blockCalls)
        assertTrue(m.uiState.items.single().blocked)
    }

    @Test
    fun changing_role_updates_the_row() = runTest(mainDispatcher.dispatcher) {
        fake.page = Page(items = listOf(user("a", isApproved = true)), nextCursor = null)
        val m = vm(); advanceUntilIdle()

        m.setRole("a", "t2w_rider"); advanceUntilIdle()

        assertEquals(listOf("a" to "t2w_rider"), fake.roleCalls)
        assertEquals("t2w_rider", m.uiState.items.single().role)
    }

    @Test
    fun a_failed_action_surfaces_an_error_and_keeps_the_row() = runTest(mainDispatcher.dispatcher) {
        fake.page = Page(items = listOf(user("a", isApproved = true)), nextCursor = null)
        val m = vm(); advanceUntilIdle()
        fake.actionThrows = IOException("boom")

        m.setBlocked("a", true); advanceUntilIdle()

        assertNotNull(m.uiState.actionError)
        assertFalse("row must not have been mutated on failure", m.uiState.items.single().blocked)
    }

    @Test
    fun switching_the_status_filter_refetches() = runTest(mainDispatcher.dispatcher) {
        fake.page = Page(items = listOf(user("a")), nextCursor = null)
        val m = vm(); advanceUntilIdle()

        m.setStatus("active"); advanceUntilIdle()

        assertEquals("active", fake.lastStatus)
        assertEquals("active", m.uiState.status)
    }
}
