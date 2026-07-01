package com.taleson2wheels.app.ui.moderation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.Page
import kotlinx.coroutines.launch

/**
 * State for one cursor-paginated moderation queue. Shared by the registration,
 * blog and ride-post queues (only the item type [T] differs).
 */
@Immutable
data class ModerationQueueState<T>(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<T> = emptyList(),
    val nextCursor: String? = null,
    val error: String? = null,
    val loadMoreError: String? = null,
    /** Item ids with an approve/reject in flight — their row shows a spinner. */
    val pendingActionIds: Set<String> = emptySet(),
    /** Transient message shown (and cleared) when an approve/reject fails. */
    val actionError: String? = null,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoading
}

/**
 * The shared state machine for a core-member moderation queue: a cursor-paginated
 * list of pending items with **optimistic** approve/reject — the row leaves the
 * list immediately and is restored (with an error) if the request fails.
 *
 * Subclasses bind it to a concrete endpoint by implementing [fetchPage], [idOf]
 * and [applyModeration], and must call [refresh] from their own `init` block
 * (the base cannot, since its abstract members aren't wired until the subclass
 * constructor runs).
 */
abstract class ModerationQueueViewModel<T> : ViewModel() {

    var uiState by mutableStateOf(ModerationQueueState<T>(isLoading = true))
        private set

    /** Fetch a page; [cursor] is null for the first page. */
    protected abstract suspend fun fetchPage(cursor: String?): ApiResult<Page<T>>

    /** Stable identity for an item (used for optimistic removal / restore + list keys). */
    protected abstract fun idOf(item: T): String

    /** Apply an approve/reject to [id]. The returned payload is ignored on success. */
    protected abstract suspend fun applyModeration(id: String, approve: Boolean): ApiResult<*>

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null, loadMoreError = null)
            when (val r = fetchPage(null)) {
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
        val cursor = uiState.nextCursor ?: return
        if (uiState.isLoadingMore) return
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingMore = true, loadMoreError = null)
            when (val r = fetchPage(cursor)) {
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
        val index = uiState.items.indexOfFirst { idOf(it) == id }
        if (index < 0) return
        val removed = uiState.items[index]
        uiState = uiState.copy(
            items = uiState.items.filterNot { idOf(it) == id },
            pendingActionIds = uiState.pendingActionIds + id,
        )
        viewModelScope.launch {
            val result = applyModeration(id, approve)
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
}
