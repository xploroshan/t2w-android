package com.taleson2wheels.app.ui.blogs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.repository.BlogsRepository
import kotlinx.coroutines.launch

data class BlogsUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val blogs: List<BlogCard> = emptyList(),
    val nextCursor: String? = null,
    val error: String? = null,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoading
}

/** Loads the cursor-paginated story feed (`/api/v1/blogs`). */
class BlogsViewModel(
    private val blogsRepository: BlogsRepository,
) : ViewModel() {

    var uiState by mutableStateOf(BlogsUiState(isLoading = true))
        private set

    init { refresh() }

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
    }
}
