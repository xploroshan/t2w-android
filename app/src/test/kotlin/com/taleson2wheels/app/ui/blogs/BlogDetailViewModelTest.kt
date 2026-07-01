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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

/** Verifies the optimistic like behaviour on the story detail screen. */
@OptIn(ExperimentalCoroutinesApi::class)
class BlogDetailViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private class FakeBlogsApi(private var detailResult: BlogCard) : BlogsApi {
        var likeResult: LikeState = LikeState(likes = 0, likedByMe = false)
        var likeThrows: Throwable? = null
        var likeCalls = 0

        override suspend fun list(cursor: String?, limit: Int, type: String?, isVlog: Boolean?): Page<BlogCard> =
            error("unused")

        override suspend fun detail(id: String): BlogResponse = BlogResponse(detailResult)
        override suspend fun create(body: BlogInput): BlogResponse = error("unused")

        private fun toggle(): LikeState {
            likeCalls++
            likeThrows?.let { throw it }
            return likeResult
        }

        override suspend fun like(id: String): LikeState = toggle()
        override suspend fun unlike(id: String): LikeState = toggle()
    }

    private class FakeSession(signedIn: Boolean) : AuthSession {
        override val tokens: StateFlow<Tokens?> =
            MutableStateFlow(if (signedIn) Tokens("access", "refresh") else null)

        override suspend fun currentUser(): ApiResult<UserDto> =
            ApiResult.Failure(ApiError.Network(IOException("unused")))
    }

    private val json = Json { ignoreUnknownKeys = true }
    private fun blog(likes: Int, liked: Boolean) =
        BlogCard(id = "b1", title = "Story", likes = likes, likedByMe = liked)

    private fun vm(detail: BlogCard, signedIn: Boolean = true): Pair<BlogDetailViewModel, FakeBlogsApi> {
        val fake = FakeBlogsApi(detail)
        return BlogDetailViewModel(BlogsRepository(fake, json), FakeSession(signedIn)) to fake
    }

    @Test
    fun like_updates_optimistically_then_reconciles() = runTest(mainDispatcher.dispatcher) {
        val (model, fake) = vm(blog(likes = 2, liked = false))
        fake.likeResult = LikeState(likes = 7, likedByMe = true)
        model.load("b1"); advanceUntilIdle()

        model.toggleLike(); advanceUntilIdle()
        assertEquals(7, model.uiState.blog?.likes)
        assertTrue(model.uiState.blog?.likedByMe == true)
        assertEquals(1, fake.likeCalls)
    }

    @Test
    fun like_failure_reverts_and_surfaces_error() = runTest(mainDispatcher.dispatcher) {
        val (model, fake) = vm(blog(likes = 2, liked = false))
        fake.likeThrows = IOException("boom")
        model.load("b1"); advanceUntilIdle()

        model.toggleLike(); advanceUntilIdle()
        assertEquals(2, model.uiState.blog?.likes)
        assertFalse(model.uiState.blog?.likedByMe == true)
        assertNotNull(model.uiState.likeError)
    }

    @Test
    fun anonymous_caller_cannot_like() = runTest(mainDispatcher.dispatcher) {
        val (model, fake) = vm(blog(likes = 2, liked = false), signedIn = false)
        model.load("b1"); advanceUntilIdle()

        model.toggleLike(); advanceUntilIdle()
        assertEquals(0, fake.likeCalls)
        assertEquals(2, model.uiState.blog?.likes)
        assertNotNull(model.uiState.likeError)
    }
}
