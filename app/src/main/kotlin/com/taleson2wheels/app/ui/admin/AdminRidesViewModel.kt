package com.taleson2wheels.app.ui.admin

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.RideCard
import com.taleson2wheels.app.data.repository.AdminRepository
import com.taleson2wheels.app.data.repository.RidesRepository
import kotlinx.coroutines.launch

@Immutable
data class AdminRidesUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<RideCard> = emptyList(),
    val nextCursor: String? = null,
    val error: String? = null,
    val loadMoreError: String? = null,
    /** Ride ids with an action (delete) in flight. */
    val pendingActionIds: Set<String> = emptySet(),
    val actionError: String? = null,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoading
}

/**
 * The admin Rides screen: an uncached, all-status list of rides with per-row
 * delete (super-admin only, enforced server-side). Create / edit happen on the
 * separate [AdminRideEditorViewModel]; this list re-fetches when the screen
 * re-enters composition so a create/edit is reflected on return.
 */
class AdminRidesViewModel(
    private val ridesRepository: RidesRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {

    var uiState by mutableStateOf(AdminRidesUiState(isLoading = true))
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null, loadMoreError = null)
            when (val r = ridesRepository.ridesFresh(limit = PAGE_SIZE)) {
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
            when (val r = ridesRepository.ridesFresh(cursor = cursor, limit = PAGE_SIZE)) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoadingMore = false, items = uiState.items + r.data.items, nextCursor = r.data.nextCursor,
                )
                is ApiResult.Failure -> uiState = uiState.copy(isLoadingMore = false, loadMoreError = r.error.userMessage)
            }
        }
    }

    fun deleteRide(id: String) {
        if (id in uiState.pendingActionIds) return
        uiState = uiState.copy(pendingActionIds = uiState.pendingActionIds + id)
        viewModelScope.launch {
            val result = adminRepository.deleteRide(id)
            uiState = uiState.copy(pendingActionIds = uiState.pendingActionIds - id)
            when (result) {
                is ApiResult.Success -> uiState = uiState.copy(items = uiState.items.filterNot { it.id == id })
                is ApiResult.Failure -> uiState = uiState.copy(actionError = result.error.userMessage)
            }
        }
    }

    fun clearActionError() {
        if (uiState.actionError != null) uiState = uiState.copy(actionError = null)
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
