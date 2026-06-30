package com.taleson2wheels.app.ui.live

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.LiveAnalytics
import com.taleson2wheels.app.data.remote.dto.LiveMetrics
import com.taleson2wheels.app.data.repository.LiveRepository
import kotlinx.coroutines.launch

data class LiveInsightsUiState(
    val isLoading: Boolean = true,
    val analytics: LiveAnalytics? = null,
    val metrics: LiveMetrics? = null,
    val elevation: List<Float> = emptyList(),
    val error: String? = null,
) {
    val isEmpty: Boolean
        get() = (analytics?.leaderboard.isNullOrEmpty()) && metrics == null
}

/** Loads post-ride insights: analytics leaderboard, headline metrics, lead elevation. */
class LiveInsightsViewModel(
    private val liveRepository: LiveRepository,
) : ViewModel() {

    var uiState by mutableStateOf(LiveInsightsUiState())
        private set

    fun load(rideId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val analytics = (liveRepository.analytics(rideId) as? ApiResult.Success)?.data
            val metrics = (liveRepository.metrics(rideId) as? ApiResult.Success)?.data

            uiState = uiState.copy(
                isLoading = false,
                analytics = analytics,
                metrics = metrics,
                error = if (analytics == null && metrics == null) "No insights available for this ride yet." else null,
            )

            // Elevation is cached only for the lead rider; fetch it best-effort.
            val leadId = (liveRepository.state(rideId) as? ApiResult.Success)?.data?.session?.leadRiderId
            if (leadId != null) {
                (liveRepository.elevationAltitudes(rideId, leadId) as? ApiResult.Success)?.let {
                    uiState = uiState.copy(elevation = it.data)
                }
            }
        }
    }
}
