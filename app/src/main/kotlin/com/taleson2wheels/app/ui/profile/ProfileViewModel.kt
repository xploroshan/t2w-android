package com.taleson2wheels.app.ui.profile

import androidx.compose.runtime.Immutable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.UserDto
import com.taleson2wheels.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

@Immutable
data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: UserDto? = null,
    val error: String? = null,
)

/** Loads the signed-in user (`/auth/me`), including garage + earned badges. */
class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var uiState by mutableStateOf(ProfileUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (val result = authRepository.currentUser()) {
                is ApiResult.Success -> uiState = ProfileUiState(isLoading = false, user = result.data)
                is ApiResult.Failure ->
                    uiState = ProfileUiState(isLoading = false, error = result.error.userMessage)
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
