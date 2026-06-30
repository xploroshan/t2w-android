package com.taleson2wheels.app.ui.achievements

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.AchievementsResponse
import com.taleson2wheels.app.data.repository.CatalogRepository
import kotlinx.coroutines.launch

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val data: AchievementsResponse? = null,
    val error: String? = null,
)

/** Loads the period "arena" achievement standings (`/api/v1/achievements`). */
class AchievementsViewModel(
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    var uiState by mutableStateOf(AchievementsUiState())
        private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (val r = catalogRepository.achievements()) {
                is ApiResult.Success -> uiState = AchievementsUiState(isLoading = false, data = r.data)
                is ApiResult.Failure -> uiState = AchievementsUiState(isLoading = false, error = r.error.userMessage)
            }
        }
    }
}
