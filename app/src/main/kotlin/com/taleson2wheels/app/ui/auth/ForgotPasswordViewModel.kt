package com.taleson2wheels.app.ui.auth

import androidx.compose.runtime.Immutable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

enum class ResetStep { EMAIL, CODE, PASSWORD, DONE }

@Immutable
data class ForgotPasswordUiState(
    val step: ResetStep = ResetStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
) {
    val canSubmitEmail get() = email.contains("@") && !isSubmitting
    val canSubmitCode get() = code.trim().length >= 4 && !isSubmitting
    val canSubmitPassword get() = newPassword.length >= 12 && !isSubmitting
}

/** Email → reset code → new password. Enumeration-safe: the email step always advances. */
class ForgotPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var uiState by mutableStateOf(ForgotPasswordUiState())
        private set

    fun onEmailChange(v: String) { uiState = uiState.copy(email = v, error = null) }
    fun onCodeChange(v: String) { uiState = uiState.copy(code = v, error = null) }
    fun onNewPasswordChange(v: String) { uiState = uiState.copy(newPassword = v, error = null) }

    fun submitEmail() {
        if (!uiState.canSubmitEmail) return
        step({ authRepository.sendResetOtp(uiState.email) }) { copy(step = ResetStep.CODE) }
    }

    fun submitCode() {
        if (!uiState.canSubmitCode) return
        step({ authRepository.verifyResetOtp(uiState.email, uiState.code) }) { copy(step = ResetStep.PASSWORD) }
    }

    fun submitNewPassword() {
        if (!uiState.canSubmitPassword) return
        step({ authRepository.resetPassword(uiState.email, uiState.newPassword) }) { copy(step = ResetStep.DONE) }
    }

    private fun step(
        call: suspend () -> ApiResult<Unit>,
        advance: ForgotPasswordUiState.() -> ForgotPasswordUiState,
    ) {
        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true, error = null)
            when (val r = call()) {
                is ApiResult.Success -> uiState = uiState.advance().copy(isSubmitting = false)
                is ApiResult.Failure -> uiState = uiState.copy(isSubmitting = false, error = r.error.userMessage)
            }
        }
    }
}
