package com.taleson2wheels.app.ui.admin

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.ActivityLogEntry
import com.taleson2wheels.app.data.repository.AdminRepository
import kotlinx.coroutines.launch

@Immutable
data class AdminActivityLogUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<ActivityLogEntry> = emptyList(),
    val nextCursor: String? = null,
    val error: String? = null,
    val loadMoreError: String? = null,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoading
}

/** The admin Activity-log viewer: a read-only, cursor-paginated audit trail (newest first). */
class AdminActivityLogViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    var uiState by mutableStateOf(AdminActivityLogUiState(isLoading = true))
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null, loadMoreError = null)
            when (val r = adminRepository.activityLog(limit = PAGE_SIZE)) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoading = false, items = r.data.items, nextCursor = r.data.nextCursor,
                )
                is ApiResult.Failure -> uiState = uiState.copy(isLoading = false, error = r.error.userMessage)
            }
        }
    }

    fun loadMore() {
        val cursor = uiState.nextCursor ?: return
        if (uiState.isLoadingMore) return
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingMore = true, loadMoreError = null)
            when (val r = adminRepository.activityLog(cursor = cursor, limit = PAGE_SIZE)) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoadingMore = false, items = uiState.items + r.data.items, nextCursor = r.data.nextCursor,
                )
                is ApiResult.Failure -> uiState = uiState.copy(isLoadingMore = false, loadMoreError = r.error.userMessage)
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
