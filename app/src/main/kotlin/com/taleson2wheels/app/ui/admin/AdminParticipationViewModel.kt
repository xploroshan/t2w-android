package com.taleson2wheels.app.ui.admin

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.ParticipationRow
import com.taleson2wheels.app.data.repository.AdminRepository
import kotlinx.coroutines.launch

@Immutable
data class AdminParticipationUiState(
    val rideId: String = "",
    val isLoading: Boolean = false,
    val items: List<ParticipationRow> = emptyList(),
    val error: String? = null,
    /** riderProfileIds with an action in flight. */
    val pendingActionIds: Set<String> = emptySet(),
    val actionError: String? = null,
)

/**
 * The per-ride participation editor: lists each rider's points + drop-out state
 * and edits them in place. Setting points to 0 removes the rider; the drop-out
 * toggle is super-admin only (enforced server-side). Adding brand-new
 * participants stays on the web admin — this surface edits existing rows.
 */
class AdminParticipationViewModel(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    var uiState by mutableStateOf(AdminParticipationUiState(isLoading = true))
        private set

    private var initialized = false

    fun load(rideId: String) {
        if (initialized) return
        initialized = true
        uiState = uiState.copy(rideId = rideId)
        refresh()
    }

    fun refresh() {
        val rideId = uiState.rideId
        if (rideId.isBlank()) return
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            when (val r = adminRepository.participation(rideId)) {
                is ApiResult.Success -> uiState = uiState.copy(isLoading = false, items = r.data)
                is ApiResult.Failure -> uiState = uiState.copy(isLoading = false, error = r.error.userMessage)
            }
        }
    }

    /** Set a rider's points. A non-positive value removes the rider from the ride. */
    fun setPoints(riderProfileId: String, points: Double) =
        act(riderProfileId) {
            when (val r = adminRepository.setParticipationPoints(uiState.rideId, riderProfileId, points)) {
                is ApiResult.Success -> {
                    uiState = if (r.data.action == "removed") {
                        removeRow(riderProfileId)
                    } else {
                        mapRow(riderProfileId) { it.copy(points = r.data.points ?: points) }
                    }
                    null
                }
                is ApiResult.Failure -> r.error.userMessage
            }
        }

    fun remove(riderProfileId: String) = setPoints(riderProfileId, 0.0)

    fun setDroppedOut(riderProfileId: String, droppedOut: Boolean) =
        act(riderProfileId) {
            when (val r = adminRepository.setParticipationDroppedOut(uiState.rideId, riderProfileId, droppedOut)) {
                is ApiResult.Success -> {
                    uiState = mapRow(riderProfileId) { it.copy(droppedOut = r.data.droppedOut) }
                    null
                }
                is ApiResult.Failure -> r.error.userMessage
            }
        }

    fun clearActionError() {
        if (uiState.actionError != null) uiState = uiState.copy(actionError = null)
    }

    /** Shared bookkeeping: mark [riderProfileId] in-flight, run [block], surface its error (or null). */
    private fun act(riderProfileId: String, block: suspend () -> String?) {
        if (riderProfileId in uiState.pendingActionIds) return
        uiState = uiState.copy(pendingActionIds = uiState.pendingActionIds + riderProfileId)
        viewModelScope.launch {
            val error = block()
            uiState = uiState.copy(
                pendingActionIds = uiState.pendingActionIds - riderProfileId,
                actionError = error ?: uiState.actionError,
            )
        }
    }

    private fun removeRow(riderProfileId: String): AdminParticipationUiState =
        uiState.copy(items = uiState.items.filterNot { it.riderProfileId == riderProfileId })

    private fun mapRow(riderProfileId: String, transform: (ParticipationRow) -> ParticipationRow): AdminParticipationUiState =
        uiState.copy(items = uiState.items.map { if (it.riderProfileId == riderProfileId) transform(it) else it })
}
