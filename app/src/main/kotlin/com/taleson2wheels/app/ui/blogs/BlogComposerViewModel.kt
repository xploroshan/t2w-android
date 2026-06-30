package com.taleson2wheels.app.ui.blogs

import androidx.compose.runtime.Immutable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.BlogInput
import com.taleson2wheels.app.data.repository.BlogsRepository
import com.taleson2wheels.app.data.repository.UploadRepository
import kotlinx.coroutines.launch

@Immutable
data class BlogComposerUiState(
    val title: String = "",
    val excerpt: String = "",
    val content: String = "",
    val tagsText: String = "",
    val coverImage: String? = null,
    val isVlog: Boolean = false,
    val videoUrl: String = "",
    val isUploading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    /** Set once the post is created; carries the moderation note for the caller. */
    val doneMessage: String? = null,
) {
    val canSubmit: Boolean
        get() = title.isNotBlank() && content.isNotBlank() && !isSubmitting && !isUploading
}

/** Drives the "write a story" composer — cover upload + `POST /api/v1/blogs`. */
class BlogComposerViewModel(
    private val blogsRepository: BlogsRepository,
    private val uploadRepository: UploadRepository,
) : ViewModel() {

    var uiState by mutableStateOf(BlogComposerUiState())
        private set

    fun onTitleChange(v: String) { uiState = uiState.copy(title = v, error = null) }
    fun onExcerptChange(v: String) { uiState = uiState.copy(excerpt = v, error = null) }
    fun onContentChange(v: String) { uiState = uiState.copy(content = v, error = null) }
    fun onTagsChange(v: String) { uiState = uiState.copy(tagsText = v, error = null) }
    fun onVideoUrlChange(v: String) { uiState = uiState.copy(videoUrl = v, error = null) }
    fun toggleVlog(on: Boolean) { uiState = uiState.copy(isVlog = on, error = null) }
    fun removeCover() { uiState = uiState.copy(coverImage = null) }

    fun uploadCover(bytes: ByteArray, filename: String, mimeType: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isUploading = true, error = null)
            when (val r = uploadRepository.uploadImage(bytes, filename, mimeType, type = "blog")) {
                is ApiResult.Success -> uiState = uiState.copy(isUploading = false, coverImage = r.data)
                is ApiResult.Failure -> uiState = uiState.copy(isUploading = false, error = r.error.userMessage)
            }
        }
    }

    fun submit() {
        if (!uiState.canSubmit) return
        val s = uiState
        val tags = s.tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val input = BlogInput(
            title = s.title.trim(),
            excerpt = s.excerpt.trim().ifBlank { null },
            content = s.content.trim(),
            coverImage = s.coverImage,
            tags = tags,
            type = "personal",
            isVlog = s.isVlog,
            videoUrl = if (s.isVlog) s.videoUrl.trim().ifBlank { null } else null,
        )
        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true, error = null)
            when (val r = blogsRepository.createBlog(input)) {
                is ApiResult.Success -> {
                    // Created posts default to "pending"; admins may be auto-approved.
                    val approved = r.data.approvalStatus == "approved"
                    uiState = uiState.copy(
                        isSubmitting = false,
                        doneMessage = if (approved) "Story published" else "Story submitted for approval",
                    )
                }
                is ApiResult.Failure -> uiState = uiState.copy(isSubmitting = false, error = r.error.userMessage)
            }
        }
    }
}
