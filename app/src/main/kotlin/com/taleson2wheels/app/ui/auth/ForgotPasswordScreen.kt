package com.taleson2wheels.app.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.ui.AppViewModelFactory

@Composable
fun ForgotPasswordScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    AuthScaffold(title = "Reset password", onBack = onBack) { m ->
        when (state.step) {
            ResetStep.EMAIL -> {
                Text("Enter your account email — we'll send a reset code.", style = MaterialTheme.typography.bodyLarge)
                AuthField(state.email, viewModel::onEmailChange, "Email", m, !state.isSubmitting, keyboardType = KeyboardType.Email, imeAction = ImeAction.Done, onImeAction = viewModel::submitEmail)
                AuthErrorText(state.error, m)
                AuthPrimaryButton("Send code", state.canSubmitEmail, state.isSubmitting, viewModel::submitEmail, m)
            }

            ResetStep.CODE -> {
                Text("Enter the code we emailed to ${state.email}.", style = MaterialTheme.typography.bodyLarge)
                AuthField(state.code, viewModel::onCodeChange, "Reset code", m, !state.isSubmitting, keyboardType = KeyboardType.Number, imeAction = ImeAction.Done, onImeAction = viewModel::submitCode)
                AuthErrorText(state.error, m)
                AuthPrimaryButton("Verify", state.canSubmitCode, state.isSubmitting, viewModel::submitCode, m)
            }

            ResetStep.PASSWORD -> {
                Text("Choose a new password.", style = MaterialTheme.typography.bodyLarge)
                AuthField(state.newPassword, viewModel::onNewPasswordChange, "New password (min 12 characters)", m, !state.isSubmitting, isPassword = true, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, onImeAction = viewModel::submitNewPassword)
                AuthErrorText(state.error, m)
                AuthPrimaryButton("Reset password", state.canSubmitPassword, state.isSubmitting, viewModel::submitNewPassword, m)
            }

            ResetStep.DONE -> {
                Text(
                    "Your password was reset. You can now sign in with your new password.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                AuthPrimaryButton("Back to sign in", enabled = true, loading = false, onClick = onBack, modifier = m)
            }
        }
    }
}
