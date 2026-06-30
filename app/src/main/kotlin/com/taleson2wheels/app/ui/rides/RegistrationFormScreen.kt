package com.taleson2wheels.app.ui.rides

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.auth.AuthErrorText
import com.taleson2wheels.app.ui.auth.AuthField
import com.taleson2wheels.app.ui.auth.AuthPrimaryButton
import com.taleson2wheels.app.ui.auth.AuthScaffold
import com.taleson2wheels.app.ui.common.readPickedImage
import kotlinx.coroutines.launch

@Composable
fun RegistrationFormScreen(
    rideId: String,
    rideTitle: String,
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    viewModel: RegistrationViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val img = context.readPickedImage(uri) ?: return@launch
            viewModel.uploadPaymentScreenshot(img.bytes, img.fileName("payment"), img.mime)
        }
    }

    AuthScaffold(title = "Register", onBack = onBack) { m ->
        if (state.done) {
            Text("You're registered for", style = MaterialTheme.typography.bodyLarge)
            Text(rideTitle, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "Confirmation: ${state.confirmationCode}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Your registration is pending confirmation by the T2W team.",
                style = MaterialTheme.typography.bodyLarge,
            )
            AuthPrimaryButton("Done", enabled = true, loading = false, onClick = onBack, modifier = m)
            return@AuthScaffold
        }

        Text(rideTitle, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)

        AuthField(state.riderName, viewModel::onRiderNameChange, "Name (optional — defaults to your profile)", m, !state.isSubmitting)
        AuthField(state.email, viewModel::onEmailChange, "Email (optional)", m, !state.isSubmitting, keyboardType = KeyboardType.Email)
        AuthField(state.phone, viewModel::onPhoneChange, "Phone", m, !state.isSubmitting, keyboardType = KeyboardType.Phone)
        AuthField(state.emergencyContactName, viewModel::onEmergencyNameChange, "Emergency contact name", m, !state.isSubmitting)
        AuthField(state.emergencyContactPhone, viewModel::onEmergencyPhoneChange, "Emergency contact phone", m, !state.isSubmitting, keyboardType = KeyboardType.Phone)
        AuthField(state.bloodGroup, viewModel::onBloodGroupChange, "Blood group", m, !state.isSubmitting)
        AuthField(state.vehicleModel, viewModel::onVehicleModelChange, "Vehicle model", m, !state.isSubmitting)
        AuthField(state.vehicleRegNumber, viewModel::onVehicleRegChange, "Vehicle reg. number", m, !state.isSubmitting)
        AuthField(state.tshirtSize, viewModel::onTshirtChange, "T-shirt size", m, !state.isSubmitting)
        AuthField(state.upiTransactionId, viewModel::onUpiChange, "UPI transaction ID", m, !state.isSubmitting, imeAction = ImeAction.Done)

        OutlinedButton(onClick = { pickImage.launch("image/*") }, enabled = !state.isUploading, modifier = m) {
            Text(
                when {
                    state.isUploading -> "Uploading…"
                    state.paymentScreenshotUrl != null -> "✓ Payment screenshot attached"
                    else -> "Attach payment screenshot"
                },
            )
        }

        CheckRow("I agree to the cancellation terms", state.agreedCancellationTerms, viewModel::onAgreeCancellation, !state.isSubmitting)
        CheckRow("I accept the indemnity / liability waiver", state.agreedIndemnity, viewModel::onAgreeIndemnity, !state.isSubmitting)

        AuthErrorText(state.error, m)
        AuthPrimaryButton("Submit registration", state.canSubmit, state.isSubmitting, { viewModel.submit(rideId) }, m)
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}
