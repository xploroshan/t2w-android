package com.taleson2wheels.app.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isSubmitting
}

/**
 * Drives the login form. On success the [AuthRepository] persists the session,
 * which the app-level auth gate observes to swap in the app shell — so this
 * view model only needs to surface errors, not navigate.
 */
class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun onEmailChange(value: String) {
        uiState = uiState.copy(email = value, error = null)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(password = value, error = null)
    }

    fun submit() {
        if (!uiState.canSubmit) return
        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true, error = null)
            when (val result = authRepository.login(uiState.email, uiState.password)) {
                is ApiResult.Success -> Unit // session flow drives navigation
                is ApiResult.Failure ->
                    uiState = uiState.copy(isSubmitting = false, error = result.error.userMessage)
            }
        }
    }
}
