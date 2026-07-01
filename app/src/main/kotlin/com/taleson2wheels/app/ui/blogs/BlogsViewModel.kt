package com.taleson2wheels.app.ui.blogs

import androidx.compose.runtime.Immutable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.repository.AuthSession
import com.taleson2wheels.app.data.repository.BlogsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Story feed filter. Maps 1:1 to the server-side `type`/`isVlog` query params. */
enum class BlogFilter(val label: String, val type: String?, val isVlog: Boolean?) {
    ALL("All", null, null),
    OFFICIAL("Official", "official", null),
    PERSONAL("Personal", "personal", null),
    VLOGS("Vlogs", null, true),
}

@Immutable
data class BlogsUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val filter: BlogFilter = BlogFilter.ALL,
    val blogs: List<BlogCard> = emptyList(),
    val nextCursor: String? = null,
    val error: String? = null,
    /** Set when fetching the NEXT page fails — shown as a tap-to-retry footer so
     *  pagination errors aren't silently swallowed under an existing list. */
    val loadMoreError: String? = null,
    /** Whether to show the "write a story" affordance — gated on the signed-in role. */
    val canCompose: Boolean = false,
    /** Whether a session exists; the like affordance is enabled only when signed in. */
    val isSignedIn: Boolean = false,
    /** Transient message shown (and cleared) when a like couldn't be saved. */
    val likeError: String? = null,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoading
}

/** Loads the cursor-paginated story feed (`/api/v1/blogs`) with a type/vlog filter and per-post likes. */
class BlogsViewModel(
    private val blogsRepository: BlogsRepository,
    private val session: AuthSession,
) : ViewModel() {

    var uiState by mutableStateOf(BlogsUiState(isLoading = true))
        private set

    // Cancel an in-flight first-page / next-page load when the filter changes or a
    // refresh starts, so an out-of-order response can't overwrite a newer filter.
    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    // Blog ids with an in-flight like toggle — guards against a double-tap racing
    // two writes (and two conflicting optimistic edits) against the same post.
    private val pendingLikes = mutableSetOf<String>()

    init {
        refresh()
        loadComposePermission()
        observeSession()
    }

    fun setFilter(filter: BlogFilter) {
        if (filter == uiState.filter) return
        uiState = uiState.copy(filter = filter)
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadMoreJob?.cancel()
        val filter = uiState.filter
        loadJob = viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, isLoadingMore = false, error = null, loadMoreError = null)
            when (
                val r = blogsRepository.blogs(limit = PAGE_SIZE, type = filter.type, isVlog = filter.isVlog)
            ) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoading = false,
                    blogs = r.data.items,
                    nextCursor = r.data.nextCursor,
                )
                is ApiResult.Failure -> uiState = uiState.copy(isLoading = false, error = r.error.userMessage)
            }
        }
    }

    fun loadMore() {
        val cursor = uiState.nextCursor
        if (cursor == null || uiState.isLoadingMore) return
        // Capture the filter this page belongs to so a response that lands after the
        // user switched filters is discarded rather than appended to the new list.
        val reqFilter = uiState.filter
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            uiState = uiState.copy(isLoadingMore = true, loadMoreError = null)
            when (
                val r = blogsRepository.blogs(
                    cursor = cursor, limit = PAGE_SIZE, type = reqFilter.type, isVlog = reqFilter.isVlog,
                )
            ) {
                is ApiResult.Success ->
                    if (uiState.filter == reqFilter) uiState = uiState.copy(
                        isLoadingMore = false,
                        blogs = uiState.blogs + r.data.items,
                        nextCursor = r.data.nextCursor,
                    )
                is ApiResult.Failure ->
                    if (uiState.filter == reqFilter) uiState = uiState.copy(
                        isLoadingMore = false,
                        loadMoreError = r.error.userMessage,
                    )
            }
        }
    }

    /**
     * Toggle the caller's like on [blogId], updating the count optimistically and
     * reconciling with the server's authoritative value (or reverting on failure).
     */
    fun toggleLike(blogId: String) {
        val before = uiState.blogs.find { it.id == blogId } ?: return
        if (!uiState.isSignedIn) {
            uiState = uiState.copy(likeError = "Sign in to like stories.")
            return
        }
        if (!pendingLikes.add(blogId)) return // a toggle for this post is already in flight
        val liked = !before.likedByMe
        uiState = uiState.copy(blogs = uiState.blogs.map { b ->
            if (b.id == blogId) b.copy(likedByMe = liked, likes = (b.likes + if (liked) 1 else -1).coerceAtLeast(0)) else b
        })
        viewModelScope.launch {
            val result = blogsRepository.setLike(blogId, liked)
            pendingLikes.remove(blogId)
            when (result) {
                is ApiResult.Success -> uiState = uiState.copy(blogs = uiState.blogs.map { b ->
                    if (b.id == blogId) b.copy(likedByMe = result.data.likedByMe, likes = result.data.likes) else b
                })
                is ApiResult.Failure -> uiState = uiState.copy(
                    // Revert only this post to its pre-toggle values; leave newer edits alone.
                    blogs = uiState.blogs.map { b ->
                        if (b.id == blogId) b.copy(likedByMe = before.likedByMe, likes = before.likes) else b
                    },
                    likeError = result.error.userMessage,
                )
            }
        }
    }

    fun clearLikeError() {
        if (uiState.likeError != null) uiState = uiState.copy(likeError = null)
    }

    // Roles that may author a story. t2w_rider is gated by a server-side
    // `canPostBlog` toggle we can't read here, so the compose attempt may still
    // 403 — the composer surfaces that message cleanly when it happens.
    private fun loadComposePermission() {
        viewModelScope.launch {
            val role = (session.currentUser() as? ApiResult.Success)?.data?.role
            uiState = uiState.copy(canCompose = role in COMPOSE_ROLES)
        }
    }

    // Keep the signed-in flag reactive to login/logout while the screen is open.
    private fun observeSession() {
        viewModelScope.launch {
            session.tokens.collect { tokens ->
                uiState = uiState.copy(isSignedIn = tokens != null)
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
        val COMPOSE_ROLES = setOf("superadmin", "core_member", "t2w_rider")
    }
}
