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
import kotlinx.coroutines.launch

@Immutable
data class BlogDetailUiState(
    val isLoading: Boolean = true,
    val blog: BlogCard? = null,
    val error: String? = null,
    /** Whether a session exists; the like affordance is enabled only when signed in. */
    val isSignedIn: Boolean = false,
    /** Transient message shown (and cleared) when a like couldn't be saved. */
    val likeError: String? = null,
)

/** Loads a single story (`/api/v1/blogs/{id}`) and toggles its like. */
class BlogDetailViewModel(
    private val blogsRepository: BlogsRepository,
    private val session: AuthSession,
) : ViewModel() {

    var uiState by mutableStateOf(BlogDetailUiState())
        private set

    // True while a like toggle is in flight, so a double-tap can't race two writes.
    private var likeInFlight = false

    init {
        observeSession()
    }

    fun load(id: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (val r = blogsRepository.blog(id)) {
                is ApiResult.Success -> uiState = uiState.copy(isLoading = false, blog = r.data, error = null)
                is ApiResult.Failure -> uiState = uiState.copy(isLoading = false, blog = null, error = r.error.userMessage)
            }
        }
    }

    /** Toggle the like optimistically, reconciling with the server (or reverting on failure). */
    fun toggleLike() {
        val before = uiState.blog ?: return
        if (!uiState.isSignedIn) {
            uiState = uiState.copy(likeError = "Sign in to like stories.")
            return
        }
        if (likeInFlight) return
        likeInFlight = true
        val liked = !before.likedByMe
        uiState = uiState.copy(
            blog = before.copy(likedByMe = liked, likes = (before.likes + if (liked) 1 else -1).coerceAtLeast(0)),
        )
        viewModelScope.launch {
            val result = blogsRepository.setLike(before.id, liked)
            likeInFlight = false
            when (result) {
                is ApiResult.Success -> uiState = uiState.copy(
                    blog = uiState.blog?.copy(likedByMe = result.data.likedByMe, likes = result.data.likes),
                )
                is ApiResult.Failure -> uiState = uiState.copy(
                    blog = uiState.blog?.copy(likedByMe = before.likedByMe, likes = before.likes),
                    likeError = result.error.userMessage,
                )
            }
        }
    }

    fun clearLikeError() {
        if (uiState.likeError != null) uiState = uiState.copy(likeError = null)
    }

    private fun observeSession() {
        viewModelScope.launch {
            session.tokens.collect { tokens ->
                uiState = uiState.copy(isSignedIn = tokens != null)
            }
        }
    }
}
