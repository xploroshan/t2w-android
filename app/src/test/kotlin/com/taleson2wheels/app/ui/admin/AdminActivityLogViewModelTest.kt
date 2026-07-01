package com.taleson2wheels.app.ui.admin

import com.taleson2wheels.app.data.remote.api.AdminApi
import com.taleson2wheels.app.data.remote.dto.ActivityLogEntry
import com.taleson2wheels.app.data.remote.dto.AdminUser
import com.taleson2wheels.app.data.remote.dto.BadgeInput
import com.taleson2wheels.app.data.remote.dto.BlockBody
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.remote.dto.DropOutBody
import com.taleson2wheels.app.data.remote.dto.ModerationAction
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RideInput
import com.taleson2wheels.app.data.remote.dto.RidePost
import com.taleson2wheels.app.data.remote.dto.RoleBody
import com.taleson2wheels.app.data.remote.dto.SetParticipationBody
import com.taleson2wheels.app.data.repository.AdminRepository
import com.taleson2wheels.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminActivityLogViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeAdminApi : AdminApi {
        val pages = ArrayDeque<Page<ActivityLogEntry>>()
        val cursors = mutableListOf<String?>()

        override suspend fun activityLog(cursor: String?, limit: Int): Page<ActivityLogEntry> {
            cursors += cursor
            return pages.removeFirstOrNull() ?: Page(items = emptyList(), nextCursor = null)
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
        override suspend fun deleteRide(id: String) = throw UnsupportedOperationException()
        override suspend fun participation(id: String) = throw UnsupportedOperationException()
        override suspend fun setParticipation(id: String, body: SetParticipationBody) = throw UnsupportedOperationException()
        override suspend fun setParticipationDroppedOut(id: String, body: DropOutBody) = throw UnsupportedOperationException()
        override suspend fun createBadge(body: BadgeInput) = throw UnsupportedOperationException()
        override suspend fun updateBadge(id: String, body: BadgeInput) = throw UnsupportedOperationException()
        override suspend fun deleteBadge(id: String) = throw UnsupportedOperationException()
    }

    private val fake = FakeAdminApi()
    private fun vm() = AdminActivityLogViewModel(AdminRepository(fake, json))

    private fun entry(id: String) = ActivityLogEntry(
        id = id, action = "user.block", performedBy = "u1", performedByName = "Admin",
        targetId = "t-$id", targetName = "Target $id", timestamp = "2026-07-01T10:00:00Z",
    )

    private fun idsOf(m: AdminActivityLogViewModel) = m.uiState.items.map { it.id }

    @Test
    fun loads_the_first_page_on_init() = runTest(mainDispatcher.dispatcher) {
        fake.pages.addLast(Page(items = listOf(entry("a"), entry("b")), nextCursor = "cur"))
        val m = vm(); advanceUntilIdle()
        assertFalse(m.uiState.isLoading)
        assertEquals(listOf("a", "b"), idsOf(m))
        assertEquals("cur", m.uiState.nextCursor)
        assertEquals(listOf<String?>(null), fake.cursors)
    }

    @Test
    fun load_more_appends_the_next_page() = runTest(mainDispatcher.dispatcher) {
        fake.pages.addLast(Page(items = listOf(entry("a")), nextCursor = "cur"))
        fake.pages.addLast(Page(items = listOf(entry("b")), nextCursor = null))
        val m = vm(); advanceUntilIdle()

        m.loadMore(); advanceUntilIdle()

        assertEquals(listOf("a", "b"), idsOf(m))
        assertNull(m.uiState.nextCursor)
        assertEquals(listOf(null, "cur"), fake.cursors)
    }
}
