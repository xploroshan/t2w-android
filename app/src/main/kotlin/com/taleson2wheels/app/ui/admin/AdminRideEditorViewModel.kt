package com.taleson2wheels.app.ui.admin

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.RideDetail
import com.taleson2wheels.app.data.remote.dto.RideInput
import com.taleson2wheels.app.data.repository.AdminRepository
import com.taleson2wheels.app.data.repository.RidesRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/** The editable ride fields the mobile admin surface manages (a safe subset). */
@Immutable
data class RideForm(
    val title: String = "",
    val type: String = "day",
    val status: String = "upcoming",
    val difficulty: String = "moderate",
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val startLocation: String = "",
    val endLocation: String = "",
    val distanceKm: String = "",
    val maxRiders: String = "",
    val fee: String = "",
    val description: String = "",
)

@Immutable
data class AdminRideEditorUiState(
    /** null in create mode; the ride id in edit mode. */
    val rideId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val loadError: String? = null,
    val saveError: String? = null,
    /** Client-side pre-flight validation message (server remains the source of truth). */
    val validationError: String? = null,
    val form: RideForm = RideForm(),
    /** Read-only, shown in edit mode only. */
    val rideNumber: String? = null,
    /** Flips true once a create/update succeeds so the screen can pop back. */
    val saved: Boolean = false,
) {
    val isEdit: Boolean get() = rideId != null
}

/**
 * Backs the create / edit ride form. [load] is called once by the screen with the
 * target ride id (or null to create). In edit mode it prefills from the ride
 * detail; only the round-trippable fields are editable, so a PATCH can't wipe the
 * web-only columns (route, highlights, crew, registration form) it never sends.
 */
class AdminRideEditorViewModel(
    private val ridesRepository: RidesRepository,
    private val adminRepository: AdminRepository,
) : ViewModel() {

    var uiState by mutableStateOf(AdminRideEditorUiState())
        private set

    private var initialized = false

    /** Idempotent: the screen calls this from a LaunchedEffect; VM survives config changes. */
    fun load(rideId: String?) {
        if (initialized) return
        initialized = true
        if (rideId == null) {
            uiState = AdminRideEditorUiState(rideId = null)
            return
        }
        uiState = uiState.copy(rideId = rideId, isLoading = true, loadError = null)
        viewModelScope.launch {
            when (val r = ridesRepository.ride(rideId)) {
                is ApiResult.Success -> uiState = uiState.copy(
                    isLoading = false,
                    rideNumber = r.data.rideNumber,
                    form = r.data.toForm(),
                )
                is ApiResult.Failure -> uiState = uiState.copy(isLoading = false, loadError = r.error.userMessage)
            }
        }
    }

    fun onForm(update: RideForm.() -> RideForm) {
        uiState = uiState.copy(form = uiState.form.update(), validationError = null)
    }

    fun save() {
        if (uiState.isSaving) return
        val form = uiState.form
        validate(form)?.let { uiState = uiState.copy(validationError = it); return }

        val input = form.toInput()
        uiState = uiState.copy(isSaving = true, saveError = null)
        viewModelScope.launch {
            val rideId = uiState.rideId
            val result = if (rideId == null) {
                adminRepository.createRide(input)
            } else {
                adminRepository.updateRide(rideId, input)
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

    private fun validate(form: RideForm): String? = when {
        form.title.isBlank() -> "Title is required"
        form.startDateMillis == null -> "Pick a start date"
        form.endDateMillis == null -> "Pick an end date"
        form.endDateMillis < form.startDateMillis -> "End date can't be before the start date"
        form.startLocation.isBlank() -> "Start location is required"
        form.endLocation.isBlank() -> "End location is required"
        else -> null
    }

    private fun RideForm.toInput() = RideInput(
        title = title.trim(),
        type = type,
        status = status,
        difficulty = difficulty,
        startDate = startDateMillis?.let { Instant.ofEpochMilli(it).toString() },
        endDate = endDateMillis?.let { Instant.ofEpochMilli(it).toString() },
        startLocation = startLocation.trim(),
        endLocation = endLocation.trim(),
        distanceKm = distanceKm.toDoubleOrNull(),
        maxRiders = maxRiders.toIntOrNull(),
        fee = fee.toDoubleOrNull(),
        description = description,
    )

    private fun RideDetail.toForm() = RideForm(
        title = title,
        type = type ?: "day",
        status = status ?: "upcoming",
        difficulty = difficulty ?: "moderate",
        startDateMillis = parseIsoToMillis(startDate),
        endDateMillis = parseIsoToMillis(endDate),
        startLocation = startLocation.orEmpty(),
        endLocation = endLocation.orEmpty(),
        distanceKm = if (distanceKm > 0) trimNumber(distanceKm) else "",
        maxRiders = if (maxRiders > 0) maxRiders.toString() else "",
        fee = if (fee > 0) trimNumber(fee) else "",
        description = description.orEmpty(),
    )
}

/** "12.0" → "12", "12.5" → "12.5" — drops a trailing ".0" for cleaner text fields. */
private fun trimNumber(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

/** Parse an ISO date/date-time to epoch millis, tolerating the shapes the API returns. */
internal fun parseIsoToMillis(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return runCatching { Instant.parse(iso).toEpochMilli() }
        .recoverCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }
        .recoverCatching { LocalDate.parse(iso).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() }
        .getOrNull()
}
