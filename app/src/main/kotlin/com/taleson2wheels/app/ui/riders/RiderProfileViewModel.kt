package com.taleson2wheels.app.ui.riders

import androidx.compose.runtime.Immutable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.RiderDto
import com.taleson2wheels.app.data.repository.RidersRepository
import kotlinx.coroutines.launch

@Immutable
data class RiderProfileUiState(
    val isLoading: Boolean = true,
    val rider: RiderDto? = null,
    val error: String? = null,
)

/** Loads a single rider profile (`/riders/{id}`). */
class RiderProfileViewModel(private val ridersRepository: RidersRepository) : ViewModel() {

    var uiState by mutableStateOf(RiderProfileUiState())
        private set

    private var loadedId: String? = null

    fun load(id: String) {
        if (loadedId == id && uiState.error == null) return
        loadedId = id
        viewModelScope.launch {
            uiState = RiderProfileUiState(isLoading = true)
            when (val result = ridersRepository.profile(id)) {
                is ApiResult.Success -> uiState = RiderProfileUiState(isLoading = false, rider = result.data)
                is ApiResult.Failure ->
                    uiState = RiderProfileUiState(isLoading = false, error = result.error.userMessage)
            }
        }
    }
}
