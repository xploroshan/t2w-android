package com.taleson2wheels.app.ui.contact

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.repository.AuthRepository
import com.taleson2wheels.app.data.repository.CatalogRepository
import kotlinx.coroutines.launch

@Immutable
data class ContactUiState(
    val name: String = "",
    val email: String = "",
    val subject: String = "",
    val message: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val sent: Boolean = false,
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() && email.isNotBlank() && subject.isNotBlank() &&
            message.isNotBlank() && !isSubmitting
}

/** Drives the contact form; prefills name/email from the signed-in user. */
class ContactViewModel(
    private val catalogRepository: CatalogRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var uiState by mutableStateOf(ContactUiState())
        private set

    init {
        viewModelScope.launch {
            (authRepository.currentUser() as? ApiResult.Success)?.data?.let { user ->
                // Don't clobber anything the user has already typed.
                uiState = uiState.copy(
                    name = uiState.name.ifBlank { user.name },
                    email = uiState.email.ifBlank { user.email },
                )
            }
        }
    }

    fun onNameChange(v: String) { uiState = uiState.copy(name = v, error = null) }
    fun onEmailChange(v: String) { uiState = uiState.copy(email = v, error = null) }
    fun onSubjectChange(v: String) { uiState = uiState.copy(subject = v, error = null) }
    fun onMessageChange(v: String) { uiState = uiState.copy(message = v, error = null) }

    fun submit() {
        if (!uiState.canSubmit) return
        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true, error = null)
            val s = uiState
            when (val r = catalogRepository.submitContact(s.name.trim(), s.email.trim(), s.subject.trim(), s.message.trim())) {
                is ApiResult.Success ->
                    uiState = if (r.data) {
                        uiState.copy(isSubmitting = false, sent = true)
                    } else {
                        uiState.copy(isSubmitting = false, error = "Couldn't send your message. Please try again.")
                    }
                is ApiResult.Failure ->
                    uiState = uiState.copy(isSubmitting = false, error = r.error.userMessage)
            }
        }
    }
}
