package com.taleson2wheels.app.ui.rides

import androidx.compose.runtime.Immutable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.RideCard
import com.taleson2wheels.app.data.repository.AuthRepository
import com.taleson2wheels.app.data.repository.RidesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Immutable
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

    // Cancel an in-flight first-page / next-page load when a refresh starts, and
    // stamp each refresh with a generation token so a still-in-flight loadMore
    // can't append its stale page onto (and corrupt the cursor of) a list the
    // refresh has since replaced.
    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var generation = 0

    init {
        refresh()
        // Reflect a registration made deeper in the flow (ride detail → form) when
        // the user returns to the list, so the card's rider count / "you:" status
        // updates without a manual pull-to-refresh.
        viewModelScope.launch {
            ridesRepository.registrations.collect { refresh() }
        }
    }

    fun refresh() {
        loadJob?.cancel()
        loadMoreJob?.cancel()
        val gen = ++generation
        loadJob = viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, isLoadingMore = false, error = null)
            // No explicit limit → the repository's DEFAULT_LIMIT, which is exactly
            // the size its cacheable "rides:first" snapshot keys on (one source of
            // truth; a local page-size constant could silently drift off it).
            when (val result = ridesRepository.rides()) {
                is ApiResult.Success -> if (gen == generation) uiState = uiState.copy(
                    isLoading = false,
                    rides = result.data.items,
                    nextCursor = result.data.nextCursor,
                )
                is ApiResult.Failure -> if (gen == generation) uiState = uiState.copy(
                    isLoading = false,
                    error = result.error.userMessage,
                )
            }
        }
    }

    fun loadMore() {
        val cursor = uiState.nextCursor
        if (cursor == null || uiState.isLoadingMore) return
        // The generation this page belongs to — if a refresh supersedes it before
        // the response lands, discard the result instead of appending a stale page.
        val gen = generation
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            uiState = uiState.copy(isLoadingMore = true, loadMoreError = null)
            when (val result = ridesRepository.rides(cursor = cursor)) {
                is ApiResult.Success -> if (gen == generation) uiState = uiState.copy(
                    isLoadingMore = false,
                    rides = uiState.rides + result.data.items,
                    nextCursor = result.data.nextCursor,
                )
                is ApiResult.Failure -> if (gen == generation) uiState = uiState.copy(
                    isLoadingMore = false,
                    loadMoreError = result.error.userMessage,
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
