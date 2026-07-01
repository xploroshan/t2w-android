package com.taleson2wheels.app.ui.rides

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.data.remote.dto.RegField
import com.taleson2wheels.app.data.remote.dto.RegPaymentConfig
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
    androidx.compose.runtime.LaunchedEffect(rideId) { viewModel.load(rideId) }
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
        when {
            state.done -> {
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
            }

            state.isLoadingConfig -> {
                Text("Loading registration form…", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            state.config == null -> {
                AuthErrorText(state.configError ?: "Couldn't load the registration form.", m)
                AuthPrimaryButton("Retry", enabled = true, loading = false, onClick = { viewModel.load(rideId) }, modifier = m)
            }

            else -> {
                val config = state.config
                Text(rideTitle, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)

                config.fields.forEachIndexed { index, field ->
                    RegistrationField(
                        field = field,
                        value = state.values[field.key].orEmpty(),
                        enabled = !state.isSubmitting,
                        isLast = index == config.fields.lastIndex,
                        onValueChange = { viewModel.onFieldChange(field.key, it) },
                        modifier = m,
                    )
                }

                PaymentSection(
                    payment = config.payment,
                    isUploading = state.isUploading,
                    hasScreenshot = state.paymentScreenshotUrl != null,
                    upiTransactionId = state.upiTransactionId,
                    onPickScreenshot = { pickImage.launch("image/*") },
                    onUpiTransactionIdChange = viewModel::onUpiTransactionIdChange,
                    enabled = !state.isSubmitting,
                    modifier = m,
                )

                if (config.requireCancellationAgreement) {
                    if (config.cancellationText.isNotBlank()) {
                        Text(
                            config.cancellationText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    CheckRow("I agree to the cancellation terms", state.agreedCancellationTerms, viewModel::onAgreeCancellation, !state.isSubmitting)
                }
                if (config.requireIndemnity) {
                    CheckRow("I accept the indemnity / liability waiver", state.agreedIndemnity, viewModel::onAgreeIndemnity, !state.isSubmitting)
                }

                AuthErrorText(state.error, m)
                AuthPrimaryButton("Submit registration", state.canSubmit, state.isSubmitting, { viewModel.submit(rideId) }, m)
            }
        }
    }
}

@Composable
private fun RegistrationField(
    field: RegField,
    value: String,
    enabled: Boolean,
    isLast: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
    val label = if (field.required) "${field.label} *" else field.label
    if (field.type == "select") {
        SelectField(label = label, value = value, options = field.options, enabled = enabled, onValueChange = onValueChange, modifier = modifier)
    } else {
        AuthField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = modifier,
            enabled = enabled,
            keyboardType = when (field.type) {
                "tel" -> KeyboardType.Phone
                "email" -> KeyboardType.Email
                else -> KeyboardType.Text
            },
            imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
        )
    }
}

/** A dropdown-style select mirroring the auth text-field look. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectField(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PaymentSection(
    payment: RegPaymentConfig,
    isUploading: Boolean,
    hasScreenshot: Boolean,
    upiTransactionId: String,
    onPickScreenshot: () -> Unit,
    onUpiTransactionIdChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    if (payment.mode == "none") return
    if (payment.fee > 0) {
        Text(
            "Fee: ₹%,.0f".format(payment.fee),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
    payment.upiIds.forEach { upi ->
        Text("UPI · ${upi.label}: ${upi.id}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    payment.bankAccounts.forEach { bank ->
        Text("Bank · ${bank.label}: ${bank.details}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (payment.mode == "screenshot" || payment.mode == "both") {
        OutlinedButton(onClick = onPickScreenshot, enabled = enabled && !isUploading, modifier = modifier) {
            Text(
                when {
                    isUploading -> "Uploading…"
                    hasScreenshot -> "✓ Payment screenshot attached"
                    else -> "Attach payment screenshot"
                },
            )
        }
    }
    if (payment.mode == "transaction_id" || payment.mode == "both") {
        AuthField(
            value = upiTransactionId,
            onValueChange = onUpiTransactionIdChange,
            label = "UPI transaction ID",
            modifier = modifier,
            enabled = enabled,
            imeAction = ImeAction.Done,
        )
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
