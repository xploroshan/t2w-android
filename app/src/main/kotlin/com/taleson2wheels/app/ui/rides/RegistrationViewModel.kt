package com.taleson2wheels.app.ui.rides

import androidx.compose.runtime.Immutable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.RegField
import com.taleson2wheels.app.data.remote.dto.RegPaymentConfig
import com.taleson2wheels.app.data.remote.dto.RegisterRideRequest
import com.taleson2wheels.app.data.remote.dto.RegistrationConfig
import com.taleson2wheels.app.data.repository.RidesRepository
import com.taleson2wheels.app.data.repository.UploadRepository
import kotlinx.coroutines.launch

@Immutable
data class RegistrationUiState(
    val isLoadingConfig: Boolean = true,
    val configError: String? = null,
    val config: RegistrationConfig? = null,
    /** Field values keyed by [RegField.key]. */
    val values: Map<String, String> = emptyMap(),
    val agreedCancellationTerms: Boolean = false,
    val agreedIndemnity: Boolean = false,
    val upiTransactionId: String = "",
    val paymentScreenshotUrl: String? = null,
    val isUploading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val confirmationCode: String? = null,
) {
    val done: Boolean get() = confirmationCode != null

    val canSubmit: Boolean
        get() {
            val cfg = config ?: return false
            if (isSubmitting || isUploading) return false
            val allRequiredFilled = cfg.fields
                .filter { it.required }
                .all { values[it.key].orEmpty().isNotBlank() }
            if (!allRequiredFilled) return false
            if (cfg.requireCancellationAgreement && !agreedCancellationTerms) return false
            if (cfg.requireIndemnity && !agreedIndemnity) return false
            return true
        }
}

/**
 * Drives the ride-registration form. The form is DYNAMIC: it loads the ride's
 * [RegistrationConfig] (which fields to collect, their option lists, the required
 * agreements, and the payment mode) and the screen renders itself from that.
 */
class RegistrationViewModel(
    private val ridesRepository: RidesRepository,
    private val uploadRepository: UploadRepository,
) : ViewModel() {

    var uiState by mutableStateOf(RegistrationUiState())
        private set

    fun load(rideId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingConfig = true, configError = null)
            when (val r = ridesRepository.ride(rideId)) {
                is ApiResult.Success ->
                    uiState = uiState.copy(
                        isLoadingConfig = false,
                        // A backend that predates registrationConfig (or a config-less
                        // ride) falls back to a sensible default so the form still works.
                        config = r.data.registrationConfig ?: fallbackConfig(r.data.fee),
                    )
                is ApiResult.Failure ->
                    uiState = uiState.copy(isLoadingConfig = false, configError = r.error.userMessage)
            }
        }
    }

    fun onFieldChange(key: String, value: String) {
        uiState = uiState.copy(values = uiState.values + (key to value), error = null)
    }

    fun onUpiTransactionIdChange(v: String) { uiState = uiState.copy(upiTransactionId = v, error = null) }
    fun onAgreeCancellation(v: Boolean) { uiState = uiState.copy(agreedCancellationTerms = v, error = null) }
    fun onAgreeIndemnity(v: Boolean) { uiState = uiState.copy(agreedIndemnity = v, error = null) }

    fun uploadPaymentScreenshot(bytes: ByteArray, filename: String, mimeType: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isUploading = true, error = null)
            when (val r = uploadRepository.uploadImage(bytes, filename, mimeType, type = "payment")) {
                is ApiResult.Success -> uiState = uiState.copy(isUploading = false, paymentScreenshotUrl = r.data)
                is ApiResult.Failure -> uiState = uiState.copy(isUploading = false, error = r.error.userMessage)
            }
        }
    }

    fun submit(rideId: String) {
        if (!uiState.canSubmit) return
        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true, error = null)
            val s = uiState
            val v = s.values
            fun field(key: String) = v[key]?.trim()?.ifBlank { null }
            val body = RegisterRideRequest(
                riderName = field("riderName"),
                email = field("email"),
                phone = v["phone"].orEmpty().trim(),
                address = field("address"),
                emergencyContactName = field("emergencyContactName"),
                emergencyContactPhone = field("emergencyContactPhone"),
                bloodGroup = field("bloodGroup"),
                foodPreference = field("foodPreference"),
                ridingType = field("ridingType"),
                referredBy = field("referredBy"),
                vehicleModel = field("vehicleModel"),
                vehicleRegNumber = field("vehicleRegNumber"),
                tshirtSize = field("tshirtSize"),
                // accommodationType is assigned server-side; never sent from here.
                upiTransactionId = s.upiTransactionId.ifBlank { null },
                paymentScreenshot = s.paymentScreenshotUrl,
                agreedCancellationTerms = s.agreedCancellationTerms,
                agreedIndemnity = s.agreedIndemnity,
            )
            when (val r = ridesRepository.register(rideId, body)) {
                is ApiResult.Success ->
                    uiState = uiState.copy(isSubmitting = false, confirmationCode = r.data.confirmationCode ?: "PENDING")
                is ApiResult.Failure ->
                    uiState = uiState.copy(isSubmitting = false, error = r.error.userMessage)
            }
        }
    }

    private companion object {
        // Used only when the ride detail carries no registrationConfig (older
        // backend). Intentionally minimal — the server config is the real source.
        fun fallbackConfig(fee: Double) = RegistrationConfig(
            fields = listOf(
                RegField("riderName", "Name (optional — defaults to your profile)", "text"),
                RegField("phone", "Phone", "tel", required = true),
                RegField("email", "Email", "email"),
                RegField("emergencyContactName", "Emergency contact name", "text"),
                RegField("emergencyContactPhone", "Emergency contact phone", "tel"),
            ),
            requireCancellationAgreement = true,
            requireIndemnity = true,
            payment = RegPaymentConfig(mode = "screenshot", fee = fee),
        )
    }
}
