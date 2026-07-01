package com.taleson2wheels.app.ui.admin

import com.taleson2wheels.app.data.remote.api.AdminApi
import com.taleson2wheels.app.data.remote.dto.AdminUser
import com.taleson2wheels.app.data.remote.dto.BlockBody
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.remote.dto.DropOutBody
import com.taleson2wheels.app.data.remote.dto.DropOutResult
import com.taleson2wheels.app.data.remote.dto.ModerationAction
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.ParticipationListResponse
import com.taleson2wheels.app.data.remote.dto.ParticipationRow
import com.taleson2wheels.app.data.remote.dto.RideDeleteResponse
import com.taleson2wheels.app.data.remote.dto.RideInput
import com.taleson2wheels.app.data.remote.dto.RidePost
import com.taleson2wheels.app.data.remote.dto.RoleBody
import com.taleson2wheels.app.data.remote.dto.SetParticipationBody
import com.taleson2wheels.app.data.remote.dto.SetParticipationResult
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
class AdminParticipationViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeAdminApi : AdminApi {
        var rows: List<ParticipationRow> = emptyList()
        val setCalls = mutableListOf<Pair<String, Double?>>()
        val dropCalls = mutableListOf<Pair<String, Boolean>>()
        var actionThrows: Throwable? = null

        override suspend fun participation(id: String): ParticipationListResponse =
            ParticipationListResponse(rows)

        override suspend fun setParticipation(id: String, body: SetParticipationBody): SetParticipationResult {
            setCalls += body.riderProfileId to body.points
            actionThrows?.let { throw it }
            val removed = body.points != null && body.points <= 0.0
            return SetParticipationResult(
                action = if (removed) "removed" else "set",
                riderProfileId = body.riderProfileId,
                points = if (removed) null else body.points,
            )
        }

        override suspend fun setParticipationDroppedOut(id: String, body: DropOutBody): DropOutResult {
            dropCalls += body.riderProfileId to body.droppedOut
            actionThrows?.let { throw it }
            return DropOutResult(body.riderProfileId, body.droppedOut)
        }

        // Unused in this suite.
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
        override suspend fun deleteRide(id: String) = RideDeleteResponse(id = id, success = true)
    }

    private val fake = FakeAdminApi()
    private fun vm(): AdminParticipationViewModel {
        val m = AdminParticipationViewModel(AdminRepository(fake, json))
        m.load("r1")
        return m
    }

    private fun row(id: String, points: Double = 10.0, droppedOut: Boolean = false) =
        ParticipationRow(riderProfileId = id, riderName = "Rider $id", points = points, droppedOut = droppedOut)

    private fun idsOf(m: AdminParticipationViewModel) = m.uiState.items.map { it.riderProfileId }

    @Test
    fun loads_participation_on_init() = runTest(mainDispatcher.dispatcher) {
        fake.rows = listOf(row("a"), row("b"))
        val m = vm(); advanceUntilIdle()
        assertFalse(m.uiState.isLoading)
        assertEquals(listOf("a", "b"), idsOf(m))
    }

    @Test
    fun setting_points_updates_the_row_in_place() = runTest(mainDispatcher.dispatcher) {
        fake.rows = listOf(row("a", points = 10.0))
        val m = vm(); advanceUntilIdle()

        m.setPoints("a", 25.0); advanceUntilIdle()

        assertEquals(listOf("a" to 25.0), fake.setCalls)
        assertEquals(25.0, m.uiState.items.single().points, 0.0)
    }

    @Test
    fun setting_points_to_zero_removes_the_row() = runTest(mainDispatcher.dispatcher) {
        fake.rows = listOf(row("a"), row("b"))
        val m = vm(); advanceUntilIdle()

        m.remove("a"); advanceUntilIdle()

        assertEquals(listOf("b"), idsOf(m))
        assertEquals(0.0, fake.setCalls.single().second!!, 0.0)
    }

    @Test
    fun toggling_dropped_out_updates_the_row() = runTest(mainDispatcher.dispatcher) {
        fake.rows = listOf(row("a", droppedOut = false))
        val m = vm(); advanceUntilIdle()

        m.setDroppedOut("a", true); advanceUntilIdle()

        assertEquals(listOf("a" to true), fake.dropCalls)
        assertTrue(m.uiState.items.single().droppedOut)
    }

    @Test
    fun a_failed_action_keeps_the_row_and_surfaces_an_error() = runTest(mainDispatcher.dispatcher) {
        fake.rows = listOf(row("a", points = 10.0))
        val m = vm(); advanceUntilIdle()
        fake.actionThrows = IOException("boom")

        m.setPoints("a", 50.0); advanceUntilIdle()

        assertNotNull(m.uiState.actionError)
        assertEquals("row must be unchanged on failure", 10.0, m.uiState.items.single().points, 0.0)
    }
}
