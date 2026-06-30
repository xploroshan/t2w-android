package com.taleson2wheels.app.ui.blogs

import androidx.compose.runtime.Immutable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.repository.BlogsRepository
import kotlinx.coroutines.launch

@Immutable
data class BlogDetailUiState(
    val isLoading: Boolean = true,
    val blog: BlogCard? = null,
    val error: String? = null,
)

/** Loads a single story (`/api/v1/blogs/{id}`). */
class BlogDetailViewModel(
    private val blogsRepository: BlogsRepository,
) : ViewModel() {

    var uiState by mutableStateOf(BlogDetailUiState())
        private set

    fun load(id: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (val r = blogsRepository.blog(id)) {
                is ApiResult.Success -> uiState = BlogDetailUiState(isLoading = false, blog = r.data)
                is ApiResult.Failure -> uiState = BlogDetailUiState(isLoading = false, error = r.error.userMessage)
            }
        }
    }
}
