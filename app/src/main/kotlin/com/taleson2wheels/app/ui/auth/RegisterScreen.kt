package com.taleson2wheels.app.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.ui.AppViewModelFactory

@Composable
fun RegisterScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    AuthScaffold(title = "Create account", onBack = onBack) { fieldModifier ->
        when (state.step) {
            RegisterStep.EMAIL -> {
                Text("Enter your email to get a verification code.", style = MaterialTheme.typography.bodyLarge)
                AuthField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Email",
                    enabled = !state.isSubmitting,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    onImeAction = viewModel::submitEmail,
                    modifier = fieldModifier,
                )
                AuthErrorText(state.error, fieldModifier)
                AuthPrimaryButton("Send code", state.canSubmitEmail, state.isSubmitting, viewModel::submitEmail, fieldModifier)
            }

            RegisterStep.CODE -> {
                Text("We emailed a code to ${state.email}.", style = MaterialTheme.typography.bodyLarge)
                AuthField(
                    value = state.code,
                    onValueChange = viewModel::onCodeChange,
                    label = "Verification code",
                    enabled = !state.isSubmitting,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    onImeAction = viewModel::submitCode,
                    modifier = fieldModifier,
                )
                AuthErrorText(state.error, fieldModifier)
                AuthPrimaryButton("Verify", state.canSubmitCode, state.isSubmitting, viewModel::submitCode, fieldModifier)
                Text(
                    "Wrong email? Go back",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().clickableText(viewModel::back),
                )
            }

            RegisterStep.DETAILS -> {
                Text("Email verified. Finish your profile.", style = MaterialTheme.typography.bodyLarge)
                AuthField(state.name, viewModel::onNameChange, "Full name", fieldModifier, !state.isSubmitting)
                AuthField(
                    state.password, viewModel::onPasswordChange, "Password (min 12 characters)",
                    fieldModifier, !state.isSubmitting, isPassword = true, keyboardType = KeyboardType.Password,
                )
                AuthField(state.phone, viewModel::onPhoneChange, "Phone (optional)", fieldModifier, !state.isSubmitting, keyboardType = KeyboardType.Phone)
                AuthField(
                    state.city, viewModel::onCityChange, "City (optional)",
                    fieldModifier, !state.isSubmitting, imeAction = ImeAction.Done, onImeAction = viewModel::submitDetails,
                )
                AuthErrorText(state.error, fieldModifier)
                AuthPrimaryButton("Create account", state.canSubmitDetails, state.isSubmitting, viewModel::submitDetails, fieldModifier)
            }
        }
    }
}
