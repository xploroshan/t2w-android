package com.taleson2wheels.app.ui.admin

import com.taleson2wheels.app.data.remote.api.AdminApi
import com.taleson2wheels.app.data.remote.api.ContentApi
import com.taleson2wheels.app.data.remote.dto.AchievementsResponse
import com.taleson2wheels.app.data.remote.dto.ActivityLogEntry
import com.taleson2wheels.app.data.remote.dto.AdminUser
import com.taleson2wheels.app.data.remote.dto.BadgeDto
import com.taleson2wheels.app.data.remote.dto.BadgeInput
import com.taleson2wheels.app.data.remote.dto.BadgeDeleteResponse
import com.taleson2wheels.app.data.remote.dto.BadgesResponse
import com.taleson2wheels.app.data.remote.dto.BlockBody
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.remote.dto.ContactRequest
import com.taleson2wheels.app.data.remote.dto.ContactResponse
import com.taleson2wheels.app.data.remote.dto.CrewResponse
import com.taleson2wheels.app.data.remote.dto.DropOutBody
import com.taleson2wheels.app.data.remote.dto.GuidelinesResponse
import com.taleson2wheels.app.data.remote.dto.HealthDto
import com.taleson2wheels.app.data.remote.dto.MarkReadRequest
import com.taleson2wheels.app.data.remote.dto.MarkReadResponse
import com.taleson2wheels.app.data.remote.dto.ModerationAction
import com.taleson2wheels.app.data.remote.dto.NotificationsResponse
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RideInput
import com.taleson2wheels.app.data.remote.dto.RidePost
import com.taleson2wheels.app.data.remote.dto.RoleBody
import com.taleson2wheels.app.data.remote.dto.SetParticipationBody
import com.taleson2wheels.app.data.remote.dto.StatsDto
import com.taleson2wheels.app.data.repository.AdminRepository
import com.taleson2wheels.app.data.repository.CatalogRepository
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
class AdminBadgesViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeContentApi : ContentApi {
        var badges: List<BadgeDto> = emptyList()
        override suspend fun badges(): BadgesResponse = BadgesResponse(badges)
        override suspend fun health(): HealthDto = error("unused")
        override suspend fun stats(): StatsDto = error("unused")
        override suspend fun guidelines(): GuidelinesResponse = error("unused")
        override suspend fun crew(): CrewResponse = error("unused")
        override suspend fun achievements(): AchievementsResponse = error("unused")
        override suspend fun notifications(): NotificationsResponse = error("unused")
        override suspend fun markNotificationsRead(body: MarkReadRequest): MarkReadResponse = error("unused")
        override suspend fun contact(body: ContactRequest): ContactResponse = error("unused")
    }

    private class FakeAdminApi : AdminApi {
        val deleteCalls = mutableListOf<String>()
        var deleteThrows: Throwable? = null

        override suspend fun deleteBadge(id: String): BadgeDeleteResponse {
            deleteCalls += id
            deleteThrows?.let { throw it }
            return BadgeDeleteResponse(id = id, success = true)
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
        override suspend fun deleteRide(id: String) = throw UnsupportedOperationException()
        override suspend fun participation(id: String) = throw UnsupportedOperationException()
        override suspend fun setParticipation(id: String, body: SetParticipationBody) = throw UnsupportedOperationException()
        override suspend fun setParticipationDroppedOut(id: String, body: DropOutBody) = throw UnsupportedOperationException()
        override suspend fun createBadge(body: BadgeInput) = throw UnsupportedOperationException()
        override suspend fun updateBadge(id: String, body: BadgeInput) = throw UnsupportedOperationException()
        override suspend fun activityLog(cursor: String?, limit: Int) =
            Page<ActivityLogEntry>(items = emptyList(), nextCursor = null)
    }

    private val content = FakeContentApi()
    private val admin = FakeAdminApi()
    private fun vm() = AdminBadgesViewModel(CatalogRepository(content, json), AdminRepository(admin, json))

    private fun badge(id: String) = BadgeDto(id = id, tier = "tier-$id", name = "Badge $id", minKm = 100.0)
    private fun idsOf(m: AdminBadgesViewModel) = m.uiState.items.map { it.id }

    @Test
    fun loads_badges_on_init() = runTest(mainDispatcher.dispatcher) {
        content.badges = listOf(badge("a"), badge("b"))
        val m = vm(); advanceUntilIdle()
        assertFalse(m.uiState.isLoading)
        assertEquals(listOf("a", "b"), idsOf(m))
    }

    @Test
    fun deleting_a_badge_removes_the_row() = runTest(mainDispatcher.dispatcher) {
        content.badges = listOf(badge("a"), badge("b"))
        val m = vm(); advanceUntilIdle()

        m.deleteBadge("a"); advanceUntilIdle()

        assertEquals(listOf("b"), idsOf(m))
        assertEquals(listOf("a"), admin.deleteCalls)
    }

    @Test
    fun a_failed_delete_keeps_the_row_and_surfaces_an_error() = runTest(mainDispatcher.dispatcher) {
        content.badges = listOf(badge("a"))
        val m = vm(); advanceUntilIdle()
        admin.deleteThrows = IOException("boom")

        m.deleteBadge("a"); advanceUntilIdle()

        assertTrue(idsOf(m).contains("a"))
        assertNotNull(m.uiState.actionError)
    }
}
