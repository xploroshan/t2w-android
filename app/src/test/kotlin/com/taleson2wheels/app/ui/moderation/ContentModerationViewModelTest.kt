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
class ContentModerationViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }

    /** A fake that records moderate calls and can be told to fail the next one. */
    private class FakeAdminApi : AdminApi {
        var blogPage: Page<BlogCard> = Page(items = emptyList(), nextCursor = null)
        var ridePostPage: Page<RidePost> = Page(items = emptyList(), nextCursor = null)
        val moderateBlogCalls = mutableListOf<Pair<String, String>>()
        val moderateRidePostCalls = mutableListOf<Pair<String, String>>()
        val blogListStatuses = mutableListOf<String?>()
        var moderateThrows: Throwable? = null

        override suspend fun registrations(status: String?, rideId: String?, cursor: String?, limit: Int) =
            Page<RegistrationModeration>(items = emptyList(), nextCursor = null)

        override suspend fun moderateRegistration(id: String, body: ModerationAction): RegistrationModerationResponse =
            throw UnsupportedOperationException("not used")

        override suspend fun moderationBlogs(status: String?, cursor: String?, limit: Int): Page<BlogCard> {
            blogListStatuses += status
            return blogPage
        }

        override suspend fun moderateBlog(id: String, body: ModerationAction): BlogResponse {
            moderateBlogCalls += id to body.action
            moderateThrows?.let { throw it }
            return BlogResponse(blog(id).copy(approvalStatus = if (body.action == "approve") "approved" else "rejected"))
        }

        override suspend fun moderationRidePosts(status: String?, rideId: String?, cursor: String?, limit: Int): Page<RidePost> =
            ridePostPage

        override suspend fun moderateRidePost(id: String, body: ModerationAction): RidePostResponse {
            moderateRidePostCalls += id to body.action
            moderateThrows?.let { throw it }
            return RidePostResponse(ridePost(id).copy(approvalStatus = if (body.action == "approve") "approved" else "rejected"))
        }

        // User-management endpoints unused in this suite.
        override suspend fun users(status: String?, cursor: String?, limit: Int) =
            Page<com.taleson2wheels.app.data.remote.dto.AdminUser>(items = emptyList(), nextCursor = null)
        override suspend fun approveUser(id: String) = throw UnsupportedOperationException("not used")
        override suspend fun rejectUser(id: String) = throw UnsupportedOperationException("not used")
        override suspend fun blockUser(id: String, body: com.taleson2wheels.app.data.remote.dto.BlockBody) =
            throw UnsupportedOperationException("not used")
        override suspend fun setUserRole(id: String, body: com.taleson2wheels.app.data.remote.dto.RoleBody) =
            throw UnsupportedOperationException("not used")
    }

    private val fake = FakeAdminApi()
    private fun repo() = AdminRepository(fake, json)

    companion object {
        fun blog(id: String) = BlogCard(id = id, title = "Blog $id", approvalStatus = "pending")
        fun ridePost(id: String) = RidePost(id = id, rideId = "r1", authorName = "Rider $id", approvalStatus = "pending")
    }

    // ── Blogs ────────────────────────────────────────────────────────────────

    @Test
    fun blog_queue_loads_pending_posts_on_init() = runTest(mainDispatcher.dispatcher) {
        fake.blogPage = Page(items = listOf(blog("a"), blog("b")), nextCursor = "cur")
        val vm = BlogModerationViewModel(repo()); advanceUntilIdle()

        assertFalse(vm.uiState.isLoading)
        assertEquals(listOf("a", "b"), vm.uiState.items.map { it.id })
        assertEquals("cur", vm.uiState.nextCursor)
        // The queue only asks for the pending page.
        assertEquals(listOf<String?>("pending"), fake.blogListStatuses)
    }

    @Test
    fun approving_a_blog_removes_it_optimistically_and_calls_approve() = runTest(mainDispatcher.dispatcher) {
        fake.blogPage = Page(items = listOf(blog("a"), blog("b")), nextCursor = null)
        val vm = BlogModerationViewModel(repo()); advanceUntilIdle()

        vm.moderate("a", approve = true); advanceUntilIdle()

        assertEquals(listOf("b"), vm.uiState.items.map { it.id })
        assertEquals(listOf("a" to "approve"), fake.moderateBlogCalls)
    }

    @Test
    fun a_failed_blog_action_restores_the_row_and_surfaces_an_error() = runTest(mainDispatcher.dispatcher) {
        fake.blogPage = Page(items = listOf(blog("a")), nextCursor = null)
        val vm = BlogModerationViewModel(repo()); advanceUntilIdle()
        fake.moderateThrows = IOException("boom")

        vm.moderate("a", approve = false); advanceUntilIdle()

        assertTrue("the row must come back on failure", vm.uiState.items.any { it.id == "a" })
        assertNotNull(vm.uiState.actionError)
        assertEquals("reject", fake.moderateBlogCalls.single().second)
    }

    // ── Ride posts ─────────────────────────────────────────────────────────

    @Test
    fun ride_post_queue_loads_and_rejects() = runTest(mainDispatcher.dispatcher) {
        fake.ridePostPage = Page(items = listOf(ridePost("p1"), ridePost("p2")), nextCursor = null)
        val vm = RidePostModerationViewModel(repo()); advanceUntilIdle()
        assertEquals(listOf("p1", "p2"), vm.uiState.items.map { it.id })

        vm.moderate("p1", approve = false); advanceUntilIdle()

        assertEquals(listOf("p2"), vm.uiState.items.map { it.id })
        assertEquals(listOf("p1" to "reject"), fake.moderateRidePostCalls)
    }
}
