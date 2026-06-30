package com.taleson2wheels.app.ui.riders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.RiderDto
import com.taleson2wheels.app.data.repository.RidersRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Leaderboard scoring window. */
enum class LeaderboardPeriod(val apiValue: String, val label: String) {
    SIX_MONTHS("6m", "6 months"),
    ONE_YEAR("1y", "1 year"),
    ALL("all", "All time"),
}

data class LeaderboardUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val period: LeaderboardPeriod = LeaderboardPeriod.ALL,
    val query: String = "",
    val riders: List<RiderDto> = emptyList(),
    val nextCursor: String? = null,
    val error: String? = null,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoading
}

/** Cursor-paginated leaderboard with a period filter + debounced search (`/riders`). */
class LeaderboardViewModel(
    private val ridersRepository: RidersRepository,
) : ViewModel() {

    var uiState by mutableStateOf(LeaderboardUiState(isLoading = true))
        private set

    private var searchJob: Job? = null
    // Tracks the in-flight first-page load so a newer query/period/refresh can
    // cancel a superseded request — otherwise an out-of-order response could
    // overwrite the screen with results for a stale query.
    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun setPeriod(period: LeaderboardPeriod) {
        if (period == uiState.period) return
        uiState = uiState.copy(period = period)
        refresh()
    }

    /** Update the search box and refresh after a short debounce. */
    fun onQueryChange(query: String) {
        uiState = uiState.copy(query = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            refresh()
        }
    }

    fun clearQuery() {
        searchJob?.cancel()
        if (uiState.query.isBlank()) return
        uiState = uiState.copy(query = "")
        refresh()
    }

    fun refresh() {
        // Cancel any in-flight first-page load so its (possibly out-of-order)
        // result can't land after this newer one.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (
                val result = ridersRepository.leaderboard(
                    limit = PAGE_SIZE,
                    period = uiState.period.apiValue,
                    search = uiState.query.ifBlank { null },
                )
            ) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoading = false,
                    riders = result.data.items,
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
            uiState = uiState.copy(isLoadingMore = true)
            when (
                val result = ridersRepository.leaderboard(
                    cursor = cursor,
                    limit = PAGE_SIZE,
                    period = uiState.period.apiValue,
                    search = uiState.query.ifBlank { null },
                )
            ) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoadingMore = false,
                    riders = uiState.riders + result.data.items,
                    nextCursor = result.data.nextCursor,
                )
                is ApiResult.Failure -> uiState = uiState.copy(
                    isLoadingMore = false,
                    error = result.error.userMessage,
                )
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
