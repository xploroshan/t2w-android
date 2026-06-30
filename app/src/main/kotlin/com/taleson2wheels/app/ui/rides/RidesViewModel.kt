package com.taleson2wheels.app.ui.rides

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.RideCard
import com.taleson2wheels.app.data.repository.AuthRepository
import com.taleson2wheels.app.data.repository.RidesRepository
import kotlinx.coroutines.launch

data class RidesUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val rides: List<RideCard> = emptyList(),
    val nextCursor: String? = null,
    val error: String? = null,
    /** Set when fetching the NEXT page fails — shown as a tap-to-retry footer so
     *  pagination errors aren't silently swallowed under an existing list. */
    val loadMoreError: String? = null,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoading
}

/** Loads the cursor-paginated ride list and owns the sign-out action. */
class RidesViewModel(
    private val ridesRepository: RidesRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var uiState by mutableStateOf(RidesUiState(isLoading = true))
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (val result = ridesRepository.rides(limit = PAGE_SIZE)) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoading = false,
                    rides = result.data.items,
                    nextCursor = result.data.nextCursor,
                )
                is ApiResult.Failure -> uiState = uiState.copy(
                    isLoading = false,
                    error = result.error.userMessage,
                )
            }
        }
    }

    fun loadMore() {
        val cursor = uiState.nextCursor
        if (cursor == null || uiState.isLoadingMore) return
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingMore = true, loadMoreError = null)
            when (val result = ridesRepository.rides(cursor = cursor, limit = PAGE_SIZE)) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoadingMore = false,
                    rides = uiState.rides + result.data.items,
                    nextCursor = result.data.nextCursor,
                )
                is ApiResult.Failure -> uiState = uiState.copy(
                    isLoadingMore = false,
                    loadMoreError = result.error.userMessage,
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
