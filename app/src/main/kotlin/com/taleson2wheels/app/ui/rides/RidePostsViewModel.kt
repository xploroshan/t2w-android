package com.taleson2wheels.app.ui.rides

import androidx.compose.runtime.Immutable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.RidePost
import com.taleson2wheels.app.data.remote.dto.RidePostInput
import com.taleson2wheels.app.data.repository.RidesRepository
import com.taleson2wheels.app.data.repository.UploadRepository
import kotlinx.coroutines.launch

@Immutable
data class RidePostsUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val posts: List<RidePost> = emptyList(),
    val nextCursor: String? = null,
    val error: String? = null,
    /** Set when fetching the NEXT page fails — shown as a tap-to-retry footer. */
    val loadMoreError: String? = null,
    // Composer
    val composerOpen: Boolean = false,
    val draft: String = "",
    val draftImages: List<String> = emptyList(),
    val isUploading: Boolean = false,
    val isSubmitting: Boolean = false,
    val composerError: String? = null,
    val submittedMessage: String? = null,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoading
    val canSubmit: Boolean get() = draft.isNotBlank() && !isSubmitting && !isUploading
}

/** Loads a ride's "tales" feed and drives the create-post composer. */
class RidePostsViewModel(
    private val ridesRepository: RidesRepository,
    private val uploadRepository: UploadRepository,
) : ViewModel() {

    var uiState by mutableStateOf(RidePostsUiState())
        private set

    fun load(rideId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (val r = ridesRepository.posts(rideId, limit = PAGE_SIZE)) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoading = false,
                    posts = r.data.items,
                    nextCursor = r.data.nextCursor,
                )
                is ApiResult.Failure -> uiState = uiState.copy(isLoading = false, error = r.error.userMessage)
            }
        }
    }

    fun loadMore(rideId: String) {
        val cursor = uiState.nextCursor
        if (cursor == null || uiState.isLoadingMore) return
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingMore = true, loadMoreError = null)
            when (val r = ridesRepository.posts(rideId, cursor = cursor, limit = PAGE_SIZE)) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoadingMore = false,
                    posts = uiState.posts + r.data.items,
                    nextCursor = r.data.nextCursor,
                )
                is ApiResult.Failure -> uiState = uiState.copy(isLoadingMore = false, loadMoreError = r.error.userMessage)
            }
        }
    }

    // ── Composer ──────────────────────────────────────────────────────────────
    fun openComposer() { uiState = uiState.copy(composerOpen = true, composerError = null) }
    fun closeComposer() {
        uiState = uiState.copy(composerOpen = false, draft = "", draftImages = emptyList(), composerError = null)
    }
    fun onDraftChange(v: String) { uiState = uiState.copy(draft = v, composerError = null) }
    fun removeDraftImage(url: String) { uiState = uiState.copy(draftImages = uiState.draftImages - url) }
    fun dismissSubmittedMessage() { uiState = uiState.copy(submittedMessage = null) }

    fun uploadImage(bytes: ByteArray, filename: String, mimeType: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isUploading = true, composerError = null)
            when (val r = uploadRepository.uploadImage(bytes, filename, mimeType, type = "ridepost")) {
                is ApiResult.Success -> uiState = uiState.copy(isUploading = false, draftImages = uiState.draftImages + r.data)
                is ApiResult.Failure -> uiState = uiState.copy(isUploading = false, composerError = r.error.userMessage)
            }
        }
    }

    fun submit(rideId: String) {
        if (!uiState.canSubmit) return
        val input = RidePostInput(content = uiState.draft.trim(), images = uiState.draftImages)
        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true, composerError = null)
            when (val r = ridesRepository.createPost(rideId, input)) {
                is ApiResult.Success -> {
                    // Approved posts (admins) appear immediately; others await moderation.
                    val approved = r.data.approvalStatus == "approved"
                    uiState = uiState.copy(
                        isSubmitting = false,
                        composerOpen = false,
                        draft = "",
                        draftImages = emptyList(),
                        posts = if (approved) listOf(r.data) + uiState.posts else uiState.posts,
                        submittedMessage = if (approved) "Tale posted" else "Your tale was submitted for approval",
                    )
                }
                is ApiResult.Failure ->
                    uiState = uiState.copy(isSubmitting = false, composerError = r.error.userMessage)
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
