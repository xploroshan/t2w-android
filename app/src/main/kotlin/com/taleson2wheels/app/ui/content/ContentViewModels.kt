package com.taleson2wheels.app.ui.content

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.CrewMemberDto
import com.taleson2wheels.app.data.remote.dto.GuidelineDto
import com.taleson2wheels.app.data.repository.CatalogRepository
import kotlinx.coroutines.launch

data class GuidelinesUiState(
    val isLoading: Boolean = true,
    val guidelines: List<GuidelineDto> = emptyList(),
    val error: String? = null,
)

class GuidelinesViewModel(private val catalogRepository: CatalogRepository) : ViewModel() {

    var uiState by mutableStateOf(GuidelinesUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (val result = catalogRepository.guidelines()) {
                is ApiResult.Success -> uiState = GuidelinesUiState(isLoading = false, guidelines = result.data)
                is ApiResult.Failure ->
                    uiState = GuidelinesUiState(isLoading = false, error = result.error.userMessage)
            }
        }
    }
}

data class CrewUiState(
    val isLoading: Boolean = true,
    val crew: List<CrewMemberDto> = emptyList(),
    val error: String? = null,
)

class CrewViewModel(private val catalogRepository: CatalogRepository) : ViewModel() {

    var uiState by mutableStateOf(CrewUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (val result = catalogRepository.crew()) {
                is ApiResult.Success -> uiState = CrewUiState(isLoading = false, crew = result.data)
                is ApiResult.Failure ->
                    uiState = CrewUiState(isLoading = false, error = result.error.userMessage)
            }
        }
    }
}
