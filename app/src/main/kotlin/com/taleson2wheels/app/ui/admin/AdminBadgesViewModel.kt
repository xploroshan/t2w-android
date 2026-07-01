package com.taleson2wheels.app.ui.admin

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.BadgeDto
import com.taleson2wheels.app.data.repository.AdminRepository
import com.taleson2wheels.app.data.repository.CatalogRepository
import kotlinx.coroutines.launch

@Immutable
data class AdminBadgesUiState(
    val isLoading: Boolean = false,
    val items: List<BadgeDto> = emptyList(),
    val error: String? = null,
    /** Badge ids with an action (delete) in flight. */
    val pendingActionIds: Set<String> = emptySet(),
    val actionError: String? = null,
)

/**
 * The admin Badges screen: lists the badge catalogue (from the public
 * `GET /api/v1/badges`) with per-row delete (canManageBadges, server-side).
 * Create / edit happen on [AdminBadgeEditorViewModel]; this list re-fetches when
 * the screen re-enters composition so a create/edit shows up on return.
 */
class AdminBadgesViewModel(
    private val catalogRepository: CatalogRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {

    var uiState by mutableStateOf(AdminBadgesUiState(isLoading = true))
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (val r = catalogRepository.badges()) {
                is ApiResult.Success -> uiState = uiState.copy(isLoading = false, items = r.data)
                is ApiResult.Failure -> uiState = uiState.copy(isLoading = false, error = r.error.userMessage)
            }
        }
    }

    fun deleteBadge(id: String) {
        if (id in uiState.pendingActionIds) return
        uiState = uiState.copy(pendingActionIds = uiState.pendingActionIds + id)
        viewModelScope.launch {
            val result = adminRepository.deleteBadge(id)
            uiState = uiState.copy(pendingActionIds = uiState.pendingActionIds - id)
            when (result) {
                is ApiResult.Success -> uiState = uiState.copy(items = uiState.items.filterNot { it.id == id })
                is ApiResult.Failure -> uiState = uiState.copy(actionError = result.error.userMessage)
            }
        }
    }

    fun clearActionError() {
        if (uiState.actionError != null) uiState = uiState.copy(actionError = null)
    }
}
