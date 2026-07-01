package com.taleson2wheels.app.ui.admin

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.BadgeDto
import com.taleson2wheels.app.data.remote.dto.BadgeInput
import com.taleson2wheels.app.data.repository.AdminRepository
import com.taleson2wheels.app.data.repository.CatalogRepository
import kotlinx.coroutines.launch

@Immutable
data class BadgeForm(
    val tier: String = "",
    val kind: String = "lifetime_km",
    val name: String = "",
    val description: String = "",
    val minKm: String = "",
    val icon: String = "",
    val color: String = "",
)

@Immutable
data class AdminBadgeEditorUiState(
    /** null in create mode; the badge id in edit mode. */
    val badgeId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val loadError: String? = null,
    val saveError: String? = null,
    val validationError: String? = null,
    val form: BadgeForm = BadgeForm(),
    val saved: Boolean = false,
) {
    val isEdit: Boolean get() = badgeId != null
}

/**
 * Backs the create / edit badge form. In edit mode it prefills from the badge
 * catalogue (the public list, found by id — there's no single-badge GET). tier /
 * name / minKm are required on create; the server remains the source of truth.
 */
class AdminBadgeEditorViewModel(
    private val catalogRepository: CatalogRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {

    var uiState by mutableStateOf(AdminBadgeEditorUiState())
        private set

    private var initialized = false

    fun load(badgeId: String?) {
        if (initialized) return
        initialized = true
        if (badgeId == null) {
            uiState = AdminBadgeEditorUiState(badgeId = null)
            return
        }
        uiState = uiState.copy(badgeId = badgeId, isLoading = true, loadError = null)
        viewModelScope.launch {
            when (val r = catalogRepository.badges()) {
                is ApiResult.Success -> {
                    val badge = r.data.firstOrNull { it.id == badgeId }
                    uiState = if (badge != null) {
                        uiState.copy(isLoading = false, form = badge.toForm())
                    } else {
                        uiState.copy(isLoading = false, loadError = "Badge not found")
                    }
                }
                is ApiResult.Failure -> uiState = uiState.copy(isLoading = false, loadError = r.error.userMessage)
            }
        }
    }

    fun onForm(update: BadgeForm.() -> BadgeForm) {
        uiState = uiState.copy(form = uiState.form.update(), validationError = null)
    }

    fun save() {
        if (uiState.isSaving) return
        val form = uiState.form
        validate(form)?.let { uiState = uiState.copy(validationError = it); return }

        val input = form.toInput()
        uiState = uiState.copy(isSaving = true, saveError = null)
        viewModelScope.launch {
            val badgeId = uiState.badgeId
            val result = if (badgeId == null) {
                adminRepository.createBadge(input)
            } else {
                adminRepository.updateBadge(badgeId, input)
            }
            when (result) {
                is ApiResult.Success -> uiState = uiState.copy(isSaving = false, saved = true)
                is ApiResult.Failure -> uiState = uiState.copy(isSaving = false, saveError = result.error.userMessage)
            }
        }
    }

    fun clearSaveError() {
        if (uiState.saveError != null) uiState = uiState.copy(saveError = null)
    }

    private fun validate(form: BadgeForm): String? = when {
        form.tier.isBlank() -> "Tier is required"
        form.name.isBlank() -> "Name is required"
        form.minKm.toDoubleOrNull() == null -> "Minimum km must be a number"
        else -> null
    }

    private fun BadgeForm.toInput() = BadgeInput(
        tier = tier.trim(),
        kind = kind,
        name = name.trim(),
        description = description,
        minKm = minKm.toDoubleOrNull(),
        icon = icon.trim(),
        color = color.trim(),
    )

    private fun BadgeDto.toForm() = BadgeForm(
        tier = tier.orEmpty(),
        kind = kind ?: "lifetime_km",
        name = name.orEmpty(),
        description = description.orEmpty(),
        minKm = if (minKm == minKm.toLong().toDouble()) minKm.toLong().toString() else minKm.toString(),
        icon = icon.orEmpty(),
        color = color.orEmpty(),
    )
}
