package com.taleson2wheels.app.ui.blogs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.repository.AuthRepository
import com.taleson2wheels.app.data.repository.BlogsRepository
import kotlinx.coroutines.launch

data class BlogsUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val blogs: List<BlogCard> = emptyList(),
    val nextCursor: String? = null,
    val error: String? = null,
    /** Whether to show the "write a story" affordance — gated on the signed-in role. */
    val canCompose: Boolean = false,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoading
}

/** Loads the cursor-paginated story feed (`/api/v1/blogs`). */
class BlogsViewModel(
    private val blogsRepository: BlogsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var uiState by mutableStateOf(BlogsUiState(isLoading = true))
        private set

    init {
        refresh()
        loadComposePermission()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (val r = blogsRepository.blogs(limit = PAGE_SIZE)) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoading = false,
                    blogs = r.data.items,
                    nextCursor = r.data.nextCursor,
                )
                is ApiResult.Failure -> uiState = uiState.copy(isLoading = false, error = r.error.userMessage)
            }
        }
    }

    // Roles that may author a story. t2w_rider is gated by a server-side
    // `canPostBlog` toggle we can't read here, so the compose attempt may still
    // 403 — the composer surfaces that message cleanly when it happens.
    private fun loadComposePermission() {
        viewModelScope.launch {
            val role = (authRepository.currentUser() as? ApiResult.Success)?.data?.role
            uiState = uiState.copy(canCompose = role in COMPOSE_ROLES)
        }
    }

    fun loadMore() {
        val cursor = uiState.nextCursor
        if (cursor == null || uiState.isLoadingMore) return
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingMore = true)
            when (val r = blogsRepository.blogs(cursor = cursor, limit = PAGE_SIZE)) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoadingMore = false,
                    blogs = uiState.blogs + r.data.items,
                    nextCursor = r.data.nextCursor,
                )
                is ApiResult.Failure -> uiState = uiState.copy(isLoadingMore = false, error = r.error.userMessage)
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
        val COMPOSE_ROLES = setOf("superadmin", "core_member", "t2w_rider")
    }
}
