package com.taleson2wheels.app.ui.admin

import com.taleson2wheels.app.data.remote.api.AdminApi
import com.taleson2wheels.app.data.remote.api.ContentApi
import com.taleson2wheels.app.data.remote.dto.AchievementsResponse
import com.taleson2wheels.app.data.remote.dto.ActivityLogEntry
import com.taleson2wheels.app.data.remote.dto.AdminUser
import com.taleson2wheels.app.data.remote.dto.BadgeDto
import com.taleson2wheels.app.data.remote.dto.BadgeInput
import com.taleson2wheels.app.data.remote.dto.BadgeResponse
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminBadgeEditorViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeContentApi(val badges: List<BadgeDto>) : ContentApi {
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
        var lastCreate: BadgeInput? = null
        var lastUpdate: Pair<String, BadgeInput>? = null

        override suspend fun createBadge(body: BadgeInput): BadgeResponse {
            lastCreate = body
            return BadgeResponse(BadgeDto(id = "new-id", tier = body.tier, name = body.name))
        }

        override suspend fun updateBadge(id: String, body: BadgeInput): BadgeResponse {
            lastUpdate = id to body
            return BadgeResponse(BadgeDto(id = id, tier = body.tier, name = body.name))
        }

        // Unused in this suite.
        override suspend fun deleteBadge(id: String) = throw UnsupportedOperationException()
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
        override suspend fun activityLog(cursor: String?, limit: Int) =
            Page<ActivityLogEntry>(items = emptyList(), nextCursor = null)
    }

    private val admin = FakeAdminApi()
    private fun vm(badges: List<BadgeDto> = emptyList()) =
        AdminBadgeEditorViewModel(CatalogRepository(FakeContentApi(badges), json), AdminRepository(admin, json))

    @Test
    fun validation_blocks_create_when_required_fields_missing() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load(null); advanceUntilIdle()

        m.save(); advanceUntilIdle()

        assertNotNull(m.uiState.validationError)
        assertNull(admin.lastCreate)
    }

    @Test
    fun create_sends_the_input_and_marks_saved() = runTest(mainDispatcher.dispatcher) {
        val m = vm(); m.load(null); advanceUntilIdle()
        m.onForm { copy(tier = "gold", name = "Gold", minKm = "5000") }

        m.save(); advanceUntilIdle()

        val sent = admin.lastCreate
        assertNotNull(sent)
        assertEquals("gold", sent!!.tier)
        assertEquals("Gold", sent.name)
        assertEquals(5000.0, sent.minKm!!, 0.0)
        assertTrue(m.uiState.saved)
    }

    @Test
    fun edit_mode_prefills_from_the_badge_catalogue() = runTest(mainDispatcher.dispatcher) {
        val badge = BadgeDto(id = "b1", tier = "silver", kind = "lifetime_km", name = "Silver", minKm = 2500.0)
        val m = vm(badges = listOf(badge)); m.load("b1"); advanceUntilIdle()

        assertEquals("silver", m.uiState.form.tier)
        assertEquals("Silver", m.uiState.form.name)
        assertEquals("2500", m.uiState.form.minKm)
        assertTrue(m.uiState.isEdit)
    }

    @Test
    fun edit_sends_update_and_marks_saved() = runTest(mainDispatcher.dispatcher) {
        val badge = BadgeDto(id = "b1", tier = "silver", name = "Silver", minKm = 2500.0)
        val m = vm(badges = listOf(badge)); m.load("b1"); advanceUntilIdle()
        m.onForm { copy(name = "Silver Elite") }

        m.save(); advanceUntilIdle()

        assertEquals("b1", admin.lastUpdate?.first)
        assertEquals("Silver Elite", admin.lastUpdate?.second?.name)
        assertTrue(m.uiState.saved)
    }
}
