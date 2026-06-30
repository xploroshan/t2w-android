package com.taleson2wheels.app.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.ProfileUpdateRequest
import com.taleson2wheels.app.data.repository.AuthRepository
import com.taleson2wheels.app.data.repository.UploadRepository
import kotlinx.coroutines.launch

data class ProfileEditUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val phone: String = "",
    val city: String = "",
    val ridingExperience: String = "",
    val avatarUrl: String? = null,
    val isUploading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank() && !isSaving && !isUploading
}

/** Loads the current profile, drives the edit form, and saves via PATCH /auth/me. */
class ProfileEditViewModel(
    private val authRepository: AuthRepository,
    private val uploadRepository: UploadRepository,
) : ViewModel() {

    var uiState by mutableStateOf(ProfileEditUiState())
        private set

    init { load() }

    private fun load() {
        viewModelScope.launch {
            when (val r = authRepository.currentUser()) {
                is ApiResult.Success -> uiState = ProfileEditUiState(
                    isLoading = false,
                    name = r.data.name,
                    phone = r.data.phone.orEmpty(),
                    city = r.data.city.orEmpty(),
                    ridingExperience = r.data.ridingExperience.orEmpty(),
                    avatarUrl = r.data.avatar,
                )
                is ApiResult.Failure -> uiState = uiState.copy(isLoading = false, error = r.error.userMessage)
            }
        }
    }

    fun onNameChange(v: String) { uiState = uiState.copy(name = v, error = null) }
    fun onPhoneChange(v: String) { uiState = uiState.copy(phone = v, error = null) }
    fun onCityChange(v: String) { uiState = uiState.copy(city = v, error = null) }
    fun onExperienceChange(v: String) { uiState = uiState.copy(ridingExperience = v, error = null) }

    fun uploadAvatar(bytes: ByteArray, filename: String, mimeType: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isUploading = true, error = null)
            when (val r = uploadRepository.uploadImage(bytes, filename, mimeType, type = "avatar")) {
                is ApiResult.Success -> uiState = uiState.copy(isUploading = false, avatarUrl = r.data)
                is ApiResult.Failure -> uiState = uiState.copy(isUploading = false, error = r.error.userMessage)
            }
        }
    }

    fun save() {
        if (!uiState.canSave) return
        val s = uiState
        viewModelScope.launch {
            uiState = uiState.copy(isSaving = true, error = null)
            val req = ProfileUpdateRequest(
                name = s.name.trim(),
                phone = s.phone.trim(),
                city = s.city.trim(),
                ridingExperience = s.ridingExperience.trim(),
                avatar = s.avatarUrl,
            )
            when (val r = authRepository.updateProfile(req)) {
                is ApiResult.Success -> uiState = uiState.copy(isSaving = false, saved = true)
                is ApiResult.Failure -> uiState = uiState.copy(isSaving = false, error = r.error.userMessage)
            }
        }
    }
}
