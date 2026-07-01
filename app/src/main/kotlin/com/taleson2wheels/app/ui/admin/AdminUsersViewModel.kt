package com.taleson2wheels.app.ui.admin

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.AdminUser
import com.taleson2wheels.app.data.repository.AdminRepository
import kotlinx.coroutines.launch

@Immutable
data class AdminUsersUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<AdminUser> = emptyList(),
    val nextCursor: String? = null,
    /** "pending" | "active" | null (all). Super admins only; core members are pinned to pending server-side. */
    val status: String? = "pending",
    val error: String? = null,
    val loadMoreError: String? = null,
    /** User ids with an action in flight — their row shows a spinner / disables buttons. */
    val pendingActionIds: Set<String> = emptySet(),
    val actionError: String? = null,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoading
}

/**
 * The admin Users screen: a cursor-paginated, status-filtered account list with
 * per-row actions (approve / reject / block / unblock / change role). Actions
 * update (or remove) the affected row on success; every gate is enforced
 * server-side, so a forbidden action just surfaces its 403 in [actionError].
 */
class AdminUsersViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    var uiState by mutableStateOf(AdminUsersUiState(isLoading = true))
        private set

    init {
        refresh()
    }

    fun setStatus(status: String?) {
        if (status == uiState.status) return
        uiState = uiState.copy(status = status)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null, loadMoreError = null)
            when (val r = adminRepository.users(status = uiState.status, limit = PAGE_SIZE)) {
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
            when (val r = adminRepository.users(status = uiState.status, cursor = cursor, limit = PAGE_SIZE)) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoadingMore = false, items = uiState.items + r.data.items, nextCursor = r.data.nextCursor,
                )
                is ApiResult.Failure -> uiState = uiState.copy(isLoadingMore = false, loadMoreError = r.error.userMessage)
            }
        }
    }

    fun approve(id: String) = act(id, { adminRepository.approveUser(id) }) { user ->
        // In the pending view an approved user leaves the queue; otherwise update in place.
        uiState = if (uiState.status == "pending") removeRow(id) else replaceRow(user)
    }

    fun reject(id: String) = act(id, { adminRepository.rejectUser(id) }) {
        uiState = removeRow(id)
    }

    fun setBlocked(id: String, blocked: Boolean) =
        act(id, { adminRepository.setUserBlocked(id, blocked) }) { user -> uiState = replaceRow(user) }

    fun setRole(id: String, role: String) =
        act(id, { adminRepository.setUserRole(id, role) }) { user -> uiState = replaceRow(user) }

    fun clearActionError() {
        if (uiState.actionError != null) uiState = uiState.copy(actionError = null)
    }

    /** Shared action bookkeeping: mark the row in-flight, run [block], surface errors. */
    private fun act(id: String, block: suspend () -> ApiResult<*>, onSuccess: (AdminUser) -> Unit) {
        if (id in uiState.pendingActionIds) return
        uiState = uiState.copy(pendingActionIds = uiState.pendingActionIds + id)
        viewModelScope.launch {
            val result = block()
            uiState = uiState.copy(pendingActionIds = uiState.pendingActionIds - id)
            when (result) {
                is ApiResult.Success<*> -> {
                    val data = result.data
                    // reject returns a String id; the rest return an AdminUser. The
                    // String case is handled by the caller ignoring its arg.
                    onSuccess(data as? AdminUser ?: uiState.items.first { it.id == id })
                }
                is ApiResult.Failure -> uiState = uiState.copy(actionError = result.error.userMessage)
            }
        }
    }

    private fun replaceRow(user: AdminUser): AdminUsersUiState =
        uiState.copy(items = uiState.items.map { if (it.id == user.id) user else it })

    private fun removeRow(id: String): AdminUsersUiState =
        uiState.copy(items = uiState.items.filterNot { it.id == id })

    private companion object {
        const val PAGE_SIZE = 20
    }
}
