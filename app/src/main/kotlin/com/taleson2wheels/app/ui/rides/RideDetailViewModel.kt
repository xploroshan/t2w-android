package com.taleson2wheels.app.ui.rides

import androidx.compose.runtime.Immutable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.RideDetail
import com.taleson2wheels.app.data.repository.RidesRepository
import kotlinx.coroutines.launch

@Immutable
data class RideDetailUiState(
    val isLoading: Boolean = true,
    val ride: RideDetail? = null,
    val error: String? = null,
)

/** Loads a single ride by id for the detail screen. */
class RideDetailViewModel(private val ridesRepository: RidesRepository) : ViewModel() {

    var uiState by mutableStateOf(RideDetailUiState())
        private set

    private var loadedId: String? = null

    init {
        // If the user registers for the ride currently shown, its cached detail is
        // now stale (still "Register" CTA + old rider count). Re-fetch so the
        // screen reflects the registration the moment they return to it, instead
        // of letting them register again. The screen's LaunchedEffect(rideId)
        // alone can't do this: the id is unchanged on return, so load() no-ops.
        viewModelScope.launch {
            ridesRepository.registrations.collect { id -> if (id == loadedId) fetch(id) }
        }
    }

    /** Idempotent: safe to call from a composable LaunchedEffect on recomposition. */
    fun load(id: String) {
        if (loadedId == id && uiState.error == null) return
        fetch(id)
    }

    private fun fetch(id: String) {
        loadedId = id
        viewModelScope.launch {
            uiState = RideDetailUiState(isLoading = true)
            when (val result = ridesRepository.ride(id)) {
                is ApiResult.Success -> uiState = RideDetailUiState(isLoading = false, ride = result.data)
                is ApiResult.Failure ->
                    uiState = RideDetailUiState(isLoading = false, error = result.error.userMessage)
            }
        }
    }
}
