package com.taleson2wheels.app.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

/** Three-step signup: verify the email, then set the password + name. */
enum class RegisterStep { EMAIL, CODE, DETAILS }

data class RegisterUiState(
    val step: RegisterStep = RegisterStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val name: String = "",
    val password: String = "",
    val phone: String = "",
    val city: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
) {
    val canSubmitEmail get() = email.contains("@") && !isSubmitting
    val canSubmitCode get() = code.trim().length >= 4 && !isSubmitting
    val canSubmitDetails get() = name.isNotBlank() && password.length >= 12 && !isSubmitting
}

/**
 * Drives signup. On the final `register()` the [AuthRepository] persists the
 * session, which the app's auth gate observes to swap in the app shell — so a
 * successful registration needs no explicit navigation here.
 */
class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    var uiState by mutableStateOf(RegisterUiState())
        private set

    fun onEmailChange(v: String) { uiState = uiState.copy(email = v, error = null) }
    fun onCodeChange(v: String) { uiState = uiState.copy(code = v, error = null) }
    fun onNameChange(v: String) { uiState = uiState.copy(name = v, error = null) }
    fun onPasswordChange(v: String) { uiState = uiState.copy(password = v, error = null) }
    fun onPhoneChange(v: String) { uiState = uiState.copy(phone = v, error = null) }
    fun onCityChange(v: String) { uiState = uiState.copy(city = v, error = null) }

    fun back() {
        uiState = when (uiState.step) {
            RegisterStep.CODE -> uiState.copy(step = RegisterStep.EMAIL, error = null)
            RegisterStep.DETAILS -> uiState.copy(step = RegisterStep.CODE, error = null)
            RegisterStep.EMAIL -> uiState
        }
    }

    fun submitEmail() {
        if (!uiState.canSubmitEmail) return
        step({ authRepository.sendSignupOtp(uiState.email) }) { copy(step = RegisterStep.CODE) }
    }

    fun submitCode() {
        if (!uiState.canSubmitCode) return
        step({ authRepository.verifySignupOtp(uiState.email, uiState.code) }) { copy(step = RegisterStep.DETAILS) }
    }

    fun submitDetails() {
        if (!uiState.canSubmitDetails) return
        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true, error = null)
            when (
                val r = authRepository.register(
                    name = uiState.name,
                    email = uiState.email,
                    password = uiState.password,
                    phone = uiState.phone.ifBlank { null },
                    city = uiState.city.ifBlank { null },
                )
            ) {
                is ApiResult.Success -> Unit // session flow drives navigation
                is ApiResult.Failure -> uiState = uiState.copy(isSubmitting = false, error = r.error.userMessage)
            }
        }
    }

    // Run a step's network call; advance the state machine on success.
    private fun step(
        call: suspend () -> ApiResult<Unit>,
        advance: RegisterUiState.() -> RegisterUiState,
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
