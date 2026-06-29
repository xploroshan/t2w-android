package com.taleson2wheels.app.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val isSubmitting: Boolean = false,
    val done: Boolean = false,
    val error: String? = null,
) {
    val canSubmit get() = currentPassword.isNotBlank() && newPassword.length >= 12 && !isSubmitting
}

/** Authenticated password change. On success the repo persists the fresh token pair. */
class ChangePasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var uiState by mutableStateOf(ChangePasswordUiState())
        private set

    fun onCurrentChange(v: String) { uiState = uiState.copy(currentPassword = v, error = null) }
    fun onNewChange(v: String) { uiState = uiState.copy(newPassword = v, error = null) }

    fun submit() {
        if (!uiState.canSubmit) return
        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true, error = null)
            when (val r = authRepository.changePassword(uiState.currentPassword, uiState.newPassword)) {
                is ApiResult.Success -> uiState = uiState.copy(isSubmitting = false, done = true)
                is ApiResult.Failure -> uiState = uiState.copy(isSubmitting = false, error = r.error.userMessage)
            }
        }
    }
}
