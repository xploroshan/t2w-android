package com.taleson2wheels.app.ui.moderation

import androidx.compose.runtime.Immutable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.RegistrationModeration
import com.taleson2wheels.app.data.repository.AdminRepository
import kotlinx.coroutines.launch

@Immutable
data class ModerationUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<RegistrationModeration> = emptyList(),
    val nextCursor: String? = null,
    val error: String? = null,
    val loadMoreError: String? = null,
    /** Registration ids with an approve/reject in flight — their row shows a spinner. */
    val pendingActionIds: Set<String> = emptySet(),
    /** Transient message shown (and cleared) when an approve/reject fails. */
    val actionError: String? = null,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoading
}

/**
 * The core-member moderation queue: pending ride registrations to approve/reject
 * (`GET`/`POST /api/v1/admin/registrations`). Actions apply optimistically — the
 * row leaves the list immediately and is restored if the request fails.
 */
class ModerationViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    var uiState by mutableStateOf(ModerationUiState(isLoading = true))
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null, loadMoreError = null)
            when (val r = adminRepository.registrations(status = STATUS, limit = PAGE_SIZE)) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoading = false,
                    items = r.data.items,
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
            uiState = uiState.copy(isLoadingMore = true, loadMoreError = null)
            when (val r = adminRepository.registrations(status = STATUS, cursor = cursor, limit = PAGE_SIZE)) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoadingMore = false,
                    items = uiState.items + r.data.items,
                    nextCursor = r.data.nextCursor,
                )
                is ApiResult.Failure -> uiState = uiState.copy(isLoadingMore = false, loadMoreError = r.error.userMessage)
            }
        }
    }

    /** Approve or reject [id], removing it from the queue optimistically. */
    fun moderate(id: String, approve: Boolean) {
        if (id in uiState.pendingActionIds) return
        val index = uiState.items.indexOfFirst { it.id == id }
        if (index < 0) return
        val removed = uiState.items[index]
        uiState = uiState.copy(
            items = uiState.items.filterNot { it.id == id },
            pendingActionIds = uiState.pendingActionIds + id,
        )
        viewModelScope.launch {
            val result = adminRepository.moderateRegistration(id, approve)
            uiState = uiState.copy(pendingActionIds = uiState.pendingActionIds - id)
            if (result is ApiResult.Failure) {
                // Restore the row at (or near) its original position and surface the error.
                val restoreAt = index.coerceAtMost(uiState.items.size)
                uiState = uiState.copy(
                    items = uiState.items.toMutableList().apply { add(restoreAt, removed) },
                    actionError = result.error.userMessage,
                )
            }
        }
    }

    fun clearActionError() {
        if (uiState.actionError != null) uiState = uiState.copy(actionError = null)
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val STATUS = "pending"
    }
}
