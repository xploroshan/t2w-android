package com.taleson2wheels.app.ui.rides

import androidx.compose.runtime.Immutable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.RegisterRideRequest
import com.taleson2wheels.app.data.repository.RidesRepository
import com.taleson2wheels.app.data.repository.UploadRepository
import kotlinx.coroutines.launch

@Immutable
data class RegistrationUiState(
    val riderName: String = "",
    val email: String = "",
    val phone: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val bloodGroup: String = "",
    val vehicleModel: String = "",
    val vehicleRegNumber: String = "",
    val tshirtSize: String = "",
    val upiTransactionId: String = "",
    val accommodationType: String = "bed",
    val paymentScreenshotUrl: String? = null,
    val agreedCancellationTerms: Boolean = false,
    val agreedIndemnity: Boolean = false,
    val isUploading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val confirmationCode: String? = null,
) {
    val done: Boolean get() = confirmationCode != null
    val canSubmit: Boolean
        get() = phone.isNotBlank() && agreedCancellationTerms && agreedIndemnity &&
            !isSubmitting && !isUploading
}

/** Drives the ride-registration form, including payment-screenshot upload. */
class RegistrationViewModel(
    private val ridesRepository: RidesRepository,
    private val uploadRepository: UploadRepository,
) : ViewModel() {

    var uiState by mutableStateOf(RegistrationUiState())
        private set

    fun onRiderNameChange(v: String) { uiState = uiState.copy(riderName = v, error = null) }
    fun onEmailChange(v: String) { uiState = uiState.copy(email = v, error = null) }
    fun onPhoneChange(v: String) { uiState = uiState.copy(phone = v, error = null) }
    fun onEmergencyNameChange(v: String) { uiState = uiState.copy(emergencyContactName = v, error = null) }
    fun onEmergencyPhoneChange(v: String) { uiState = uiState.copy(emergencyContactPhone = v, error = null) }
    fun onBloodGroupChange(v: String) { uiState = uiState.copy(bloodGroup = v, error = null) }
    fun onVehicleModelChange(v: String) { uiState = uiState.copy(vehicleModel = v, error = null) }
    fun onVehicleRegChange(v: String) { uiState = uiState.copy(vehicleRegNumber = v, error = null) }
    fun onTshirtChange(v: String) { uiState = uiState.copy(tshirtSize = v, error = null) }
    fun onUpiChange(v: String) { uiState = uiState.copy(upiTransactionId = v, error = null) }
    fun onAccommodationChange(v: String) { uiState = uiState.copy(accommodationType = v, error = null) }
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
            val body = RegisterRideRequest(
                riderName = s.riderName.ifBlank { null },
                email = s.email.ifBlank { null },
                phone = s.phone,
                emergencyContactName = s.emergencyContactName.ifBlank { null },
                emergencyContactPhone = s.emergencyContactPhone.ifBlank { null },
                bloodGroup = s.bloodGroup.ifBlank { null },
                vehicleModel = s.vehicleModel.ifBlank { null },
                vehicleRegNumber = s.vehicleRegNumber.ifBlank { null },
                tshirtSize = s.tshirtSize.ifBlank { null },
                accommodationType = s.accommodationType,
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
}
