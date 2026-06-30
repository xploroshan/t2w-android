package com.taleson2wheels.app.ui.home

import androidx.compose.runtime.Immutable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.NotificationDto
import com.taleson2wheels.app.data.remote.dto.RideCard
import com.taleson2wheels.app.data.remote.dto.StatsDto
import com.taleson2wheels.app.data.repository.AuthRepository
import com.taleson2wheels.app.data.repository.CatalogRepository
import com.taleson2wheels.app.data.repository.RidesRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String? = null,
    val stats: StatsDto? = null,
    val upcomingRides: List<RideCard> = emptyList(),
    val notifications: List<NotificationDto> = emptyList(),
    val error: String? = null,
)

/** Loads the home landing: greeting (`/auth/me`), `/stats`, upcoming `/rides`, and `/notifications`. */
class HomeViewModel(
    private val catalogRepository: CatalogRepository,
    private val authRepository: AuthRepository,
    private val ridesRepository: RidesRepository,
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        refresh()
        // Keep the dashboard's upcoming-rides section in sync after the user
        // registers for a ride elsewhere in the app.
        viewModelScope.launch {
            ridesRepository.registrations.collect { refresh() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            // Fetch in parallel; the dashboard degrades gracefully if a piece fails.
            val meDeferred = async { authRepository.currentUser() }
            val statsDeferred = async { catalogRepository.stats() }
            val notifsDeferred = async { catalogRepository.notifications() }
            val ridesDeferred = async { ridesRepository.rides(limit = 5, status = "upcoming") }

            val meResult = meDeferred.await()
            val statsResult = statsDeferred.await()
            val notifsResult = notifsDeferred.await()
            val ridesResult = ridesDeferred.await()
            val name = (meResult as? ApiResult.Success)?.data?.name
            val stats = (statsResult as? ApiResult.Success)?.data
            val notifs = (notifsResult as? ApiResult.Success)?.data.orEmpty()
            val upcoming = (ridesResult as? ApiResult.Success)?.data?.items.orEmpty()

            uiState = HomeUiState(
                isLoading = false,
                userName = name,
                stats = stats,
                upcomingRides = upcoming,
                notifications = notifs.take(5),
                // Surface an error only when the dashboard has nothing to show
                // (no user, no stats, no notifications) AND at least one call
                // actually failed — an empty-but-successful feed is a normal
                // state. Carry whichever failure message is available, rather
                // than keying solely on the notifications result being a Failure
                // (which left auth/stats failures invisible when notifs returned
                // an empty success).
                error = if (name == null && stats == null && notifs.isEmpty()) {
                    listOf(meResult, statsResult, notifsResult)
                        .filterIsInstance<ApiResult.Failure>()
                        .firstOrNull()?.error?.userMessage
                } else {
                    null
                },
            )
        }
    }
}
