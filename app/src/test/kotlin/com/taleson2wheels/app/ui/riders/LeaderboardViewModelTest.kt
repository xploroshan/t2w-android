package com.taleson2wheels.app.ui.riders

import com.taleson2wheels.app.data.remote.api.RidersApi
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RiderDto
import com.taleson2wheels.app.data.remote.dto.RiderResponse
import com.taleson2wheels.app.data.repository.RidersRepository
import com.taleson2wheels.app.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Drives [LeaderboardViewModel] against a hand-controlled fake API so the
 * cursor-pagination + stale-response-race behaviour can be verified
 * deterministically (each call suspends on a deferred the test resolves).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    /** A RidersApi whose every leaderboard() call parks on a deferred the test resolves. */
    private class FakeRidersApi : RidersApi {
        data class Call(
            val cursor: String?,
            val search: String?,
            val deferred: CompletableDeferred<Page<RiderDto>> = CompletableDeferred(),
        )
        val calls = mutableListOf<Call>()

        override suspend fun leaderboard(
            cursor: String?, limit: Int, search: String?, period: String, includeMerged: Boolean,
        ): Page<RiderDto> {
            val call = Call(cursor, search)
            calls += call
            return call.deferred.await()
        }

        override suspend fun profile(id: String): RiderResponse = error("not used in this test")
    }

    private val fake = FakeRidersApi()
    private val vm get() = LeaderboardViewModel(RidersRepository(fake, Json { ignoreUnknownKeys = true }))

    private fun riders(vararg ids: String) = ids.map { RiderDto(id = it, name = it) }
    private fun page(items: List<RiderDto>, next: String?) = Page(items = items, nextCursor = next)
    private fun lastWith(search: String?) = fake.calls.last { it.search == search }

    @Test
    fun loadMore_failure_sets_loadMoreError_not_error_and_retry_recovers() = runTest(mainDispatcher.dispatcher) {
        val model = vm
        advanceUntilIdle() // init -> refresh parks on the first-page call
        fake.calls.last().deferred.complete(page(riders("a"), next = "c1"))
        advanceUntilIdle()
        assertEquals(listOf("a"), model.uiState.riders.map { it.id })
        assertTrue(model.uiState.canLoadMore)

        // Page 2 fails.
        model.loadMore(); advanceUntilIdle()
        fake.calls.last().deferred.completeExceptionally(IOException("boom"))
        advanceUntilIdle()
        assertTrue("a failed page must set loadMoreError", model.uiState.loadMoreError != null)
        assertNull("the primary error must not be clobbered by a pagination failure", model.uiState.error)
        assertTrue("the page is still retryable", model.uiState.canLoadMore)

        // Retrying succeeds and clears the error.
        model.loadMore(); advanceUntilIdle()
        fake.calls.last().deferred.complete(page(riders("b"), next = null))
        advanceUntilIdle()
        assertNull(model.uiState.loadMoreError)
        assertEquals(listOf("a", "b"), model.uiState.riders.map { it.id })
        assertTrue("a null next cursor ends pagination", !model.uiState.canLoadMore)
    }

    @Test
    fun stale_loadMore_response_after_query_change_is_discarded() = runTest(mainDispatcher.dispatcher) {
        val model = vm
        advanceUntilIdle()
        lastWith(null).deferred.complete(page(riders("A1", "A2"), next = "ca"))
        advanceUntilIdle()
        assertEquals(listOf("A1", "A2"), model.uiState.riders.map { it.id })

        // Start loading page 2 for the empty query, but DON'T resolve it yet.
        model.loadMore(); advanceUntilIdle()
        val staleCall = fake.calls.last()

        // User searches "B": after the debounce, refresh() runs and must cancel the
        // in-flight load-more so its result can't land on the new query's list.
        model.onQueryChange("B"); advanceUntilIdle()
        lastWith("B").deferred.complete(page(riders("B1", "B2"), next = "cb"))
        advanceUntilIdle()
        assertEquals(listOf("B1", "B2"), model.uiState.riders.map { it.id })

        // The stale page-2 response now arrives — it must be ignored.
        staleCall.deferred.complete(page(riders("A3", "A4"), next = "ca2"))
        advanceUntilIdle()
        assertEquals("stale riders must not be appended", listOf("B1", "B2"), model.uiState.riders.map { it.id })
        assertEquals("the stale cursor must not be resurrected", "cb", model.uiState.nextCursor)
    }
}
