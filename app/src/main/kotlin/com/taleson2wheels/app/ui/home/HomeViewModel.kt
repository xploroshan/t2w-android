package com.taleson2wheels.app.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.NotificationDto
import com.taleson2wheels.app.data.remote.dto.StatsDto
import com.taleson2wheels.app.data.repository.AuthRepository
import com.taleson2wheels.app.data.repository.CatalogRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String? = null,
    val stats: StatsDto? = null,
    val notifications: List<NotificationDto> = emptyList(),
    val error: String? = null,
)

/** Loads the home dashboard: greeting (`/auth/me`), `/stats`, and `/notifications`. */
class HomeViewModel(
    private val catalogRepository: CatalogRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            // Fetch in parallel; the dashboard degrades gracefully if a piece fails.
            val meDeferred = async { authRepository.currentUser() }
            val statsDeferred = async { catalogRepository.stats() }
            val notifsDeferred = async { catalogRepository.notifications() }

            val name = (meDeferred.await() as? ApiResult.Success)?.data?.name
            val stats = (statsDeferred.await() as? ApiResult.Success)?.data
            val notifsResult = notifsDeferred.await()
            val notifs = (notifsResult as? ApiResult.Success)?.data.orEmpty()

            uiState = HomeUiState(
                isLoading = false,
                userName = name,
                stats = stats,
                notifications = notifs.take(5),
                // Only surface an error when the whole dashboard came back empty.
                error = if (name == null && stats == null && notifsResult is ApiResult.Failure) {
                    notifsResult.error.userMessage
                } else {
                    null
                },
            )
        }
    }
}
