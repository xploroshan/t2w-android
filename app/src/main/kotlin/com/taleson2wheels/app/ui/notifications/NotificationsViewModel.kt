package com.taleson2wheels.app.ui.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.NotificationDto
import com.taleson2wheels.app.data.repository.CatalogRepository
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<NotificationDto> = emptyList(),
    val error: String? = null,
    val isMarking: Boolean = false,
) {
    val unreadCount: Int get() = notifications.count { !it.isRead }
    val hasUnread: Boolean get() = unreadCount > 0
}

/** Lists the caller's notifications and marks them read (one or all). */
class NotificationsViewModel(
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    var uiState by mutableStateOf(NotificationsUiState())
        private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (val r = catalogRepository.notifications()) {
                is ApiResult.Success -> uiState = uiState.copy(isLoading = false, notifications = r.data)
                is ApiResult.Failure -> uiState = uiState.copy(isLoading = false, error = r.error.userMessage)
            }
        }
    }

    /** Mark a single notification read, flipping it locally on success. */
    fun markRead(id: String) {
        val target = uiState.notifications.firstOrNull { it.id == id } ?: return
        if (target.isRead) return
        viewModelScope.launch {
            when (catalogRepository.markNotificationsRead(listOf(id))) {
                is ApiResult.Success -> uiState = uiState.copy(notifications = flip(uiState.notifications) { it.id == id })
                is ApiResult.Failure -> Unit // keep silent; a transient mark-read failure isn't worth a banner
            }
        }
    }

    fun markAllRead() {
        if (!uiState.hasUnread || uiState.isMarking) return
        viewModelScope.launch {
            uiState = uiState.copy(isMarking = true)
            when (val r = catalogRepository.markNotificationsRead(null)) {
                is ApiResult.Success ->
                    uiState = uiState.copy(isMarking = false, notifications = flip(uiState.notifications) { true })
                is ApiResult.Failure ->
                    uiState = uiState.copy(isMarking = false, error = r.error.userMessage)
            }
        }
    }

    private fun flip(list: List<NotificationDto>, pred: (NotificationDto) -> Boolean): List<NotificationDto> =
        list.map { if (pred(it)) it.copy(isRead = true) else it }
}
