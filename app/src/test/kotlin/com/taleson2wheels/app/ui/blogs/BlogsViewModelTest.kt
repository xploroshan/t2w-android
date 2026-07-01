package com.taleson2wheels.app.ui.blogs

import com.taleson2wheels.app.data.remote.ApiError
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.api.BlogsApi
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.remote.dto.BlogInput
import com.taleson2wheels.app.data.remote.dto.BlogResponse
import com.taleson2wheels.app.data.remote.dto.LikeState
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.UserDto
import com.taleson2wheels.app.data.repository.AuthSession
import com.taleson2wheels.app.data.repository.BlogsRepository
import com.taleson2wheels.app.data.session.Tokens
import com.taleson2wheels.app.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Drives [BlogsViewModel] against fakes to verify the two PR4c behaviours:
 * the filter → query-param mapping, and the optimistic like (reconcile on
 * success, revert on failure, gated on sign-in, and single-flight per post).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BlogsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private class FakeBlogsApi : BlogsApi {
        data class ListCall(val cursor: String?, val type: String?, val isVlog: Boolean?)
        data class LikeCall(val id: String, val liked: Boolean)

        val listCalls = mutableListOf<ListCall>()
        var listResult: Page<BlogCard> = Page(items = emptyList(), nextCursor = null)

        val likeCalls = mutableListOf<LikeCall>()
        var likeResult: LikeState = LikeState(likes = 0, likedByMe = false)
        var likeThrows: Throwable? = null
        /** When set, like/unlike park here until the test completes it (single-flight test). */
        var likeGate: CompletableDeferred<Unit>? = null

        override suspend fun list(cursor: String?, limit: Int, type: String?, isVlog: Boolean?): Page<BlogCard> {
            listCalls += ListCall(cursor, type, isVlog)
            return listResult
        }

        override suspend fun detail(id: String): BlogResponse = error("unused")
        override suspend fun create(body: BlogInput): BlogResponse = error("unused")

        private suspend fun toggle(id: String, liked: Boolean): LikeState {
            likeCalls += LikeCall(id, liked)
            likeGate?.await()
            likeThrows?.let { throw it }
            return likeResult
        }

        override suspend fun like(id: String): LikeState = toggle(id, true)
        override suspend fun unlike(id: String): LikeState = toggle(id, false)
    }

    private class FakeSession(signedIn: Boolean, private val role: String? = "rider") : AuthSession {
        override val tokens: StateFlow<Tokens?> =
            MutableStateFlow(if (signedIn) Tokens("access", "refresh") else null)

        override suspend fun currentUser(): ApiResult<UserDto> =
            role?.let { ApiResult.Success(UserDto(id = "u1", name = "U", email = "u@t2w.app", role = it)) }
                ?: ApiResult.Failure(ApiError.Network(IOException("anon")))
    }

    private val fake = FakeBlogsApi()
    private val json = Json { ignoreUnknownKeys = true }
    private fun vm(signedIn: Boolean = true) =
        BlogsViewModel(BlogsRepository(fake, json), FakeSession(signedIn))

    private fun blog(id: String, likes: Int, liked: Boolean) =
        BlogCard(id = id, title = "Story $id", likes = likes, likedByMe = liked)

    @Test
    fun filter_selection_maps_to_type_and_isVlog_query_params() = runTest(mainDispatcher.dispatcher) {
        val model = vm(); advanceUntilIdle() // init refresh → ALL (no filters)
        assertEquals(FakeBlogsApi.ListCall(null, null, null), fake.listCalls.last())

        model.setFilter(BlogFilter.OFFICIAL); advanceUntilIdle()
        assertEquals("official", fake.listCalls.last().type)
        assertNull(fake.listCalls.last().isVlog)

        model.setFilter(BlogFilter.VLOGS); advanceUntilIdle()
        assertNull(fake.listCalls.last().type)
        assertEquals(true, fake.listCalls.last().isVlog)

        // Re-selecting the current filter is a no-op (no extra request).
        val before = fake.listCalls.size
        model.setFilter(BlogFilter.VLOGS); advanceUntilIdle()
        assertEquals(before, fake.listCalls.size)
    }

    @Test
    fun like_updates_optimistically_then_reconciles_to_the_server_count() = runTest(mainDispatcher.dispatcher) {
        fake.listResult = Page(items = listOf(blog("b1", likes = 3, liked = false)), nextCursor = null)
        fake.likeResult = LikeState(likes = 10, likedByMe = true) // server truth differs from the +1 guess
        fake.likeGate = CompletableDeferred()
        val model = vm(); advanceUntilIdle()

        model.toggleLike("b1"); advanceUntilIdle() // optimistic edit, then parks on the gate
        val optimistic = model.uiState.blogs.first { it.id == "b1" }
        assertEquals("optimistic +1", 4, optimistic.likes)
        assertTrue(optimistic.likedByMe)

        fake.likeGate!!.complete(Unit); advanceUntilIdle()
        val reconciled = model.uiState.blogs.first { it.id == "b1" }
        assertEquals("uses the server count, not the optimistic guess", 10, reconciled.likes)
        assertTrue(reconciled.likedByMe)
        assertEquals(listOf(FakeBlogsApi.LikeCall("b1", true)), fake.likeCalls)
    }

    @Test
    fun like_failure_reverts_the_optimistic_edit_and_surfaces_an_error() = runTest(mainDispatcher.dispatcher) {
        fake.listResult = Page(items = listOf(blog("b1", likes = 3, liked = false)), nextCursor = null)
        fake.likeThrows = IOException("boom")
        val model = vm(); advanceUntilIdle()

        model.toggleLike("b1"); advanceUntilIdle()
        val reverted = model.uiState.blogs.first { it.id == "b1" }
        assertEquals(3, reverted.likes)
        assertFalse(reverted.likedByMe)
        assertNotNull(model.uiState.likeError)
    }

    @Test
    fun anonymous_caller_cannot_like_and_no_request_is_sent() = runTest(mainDispatcher.dispatcher) {
        fake.listResult = Page(items = listOf(blog("b1", likes = 3, liked = false)), nextCursor = null)
        val model = vm(signedIn = false); advanceUntilIdle()

        model.toggleLike("b1"); advanceUntilIdle()
        assertTrue("no like request for an anonymous caller", fake.likeCalls.isEmpty())
        val unchanged = model.uiState.blogs.first { it.id == "b1" }
        assertEquals(3, unchanged.likes)
        assertFalse(unchanged.likedByMe)
        assertNotNull(model.uiState.likeError)
    }

    @Test
    fun a_second_tap_while_a_like_is_in_flight_is_ignored() = runTest(mainDispatcher.dispatcher) {
        fake.listResult = Page(items = listOf(blog("b1", likes = 3, liked = false)), nextCursor = null)
        fake.likeResult = LikeState(likes = 4, likedByMe = true)
        fake.likeGate = CompletableDeferred()
        val model = vm(); advanceUntilIdle()

        model.toggleLike("b1"); advanceUntilIdle() // in flight, parked on the gate
        model.toggleLike("b1"); advanceUntilIdle() // must be dropped, not queued as an un-like
        assertEquals("single-flight per post", 1, fake.likeCalls.size)

        fake.likeGate!!.complete(Unit); advanceUntilIdle()
        assertEquals(1, fake.likeCalls.size)
        val finalState = model.uiState.blogs.first { it.id == "b1" }
        assertEquals(4, finalState.likes)
        assertTrue(finalState.likedByMe)
    }
}
