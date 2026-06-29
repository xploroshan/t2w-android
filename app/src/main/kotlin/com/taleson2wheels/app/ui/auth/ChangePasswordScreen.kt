package com.taleson2wheels.app.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.ui.AppViewModelFactory

@Composable
fun ChangePasswordScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    viewModel: ChangePasswordViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState

    // Pop back once the change succeeds (the repo already saved the fresh tokens).
    LaunchedEffect(state.done) { if (state.done) onBack() }

    AuthScaffold(title = "Change password", onBack = onBack) { m ->
        Text("Enter your current password and a new one.", style = MaterialTheme.typography.bodyLarge)
        AuthField(state.currentPassword, viewModel::onCurrentChange, "Current password", m, !state.isSubmitting, isPassword = true, keyboardType = KeyboardType.Password)
        AuthField(state.newPassword, viewModel::onNewChange, "New password (min 12 characters)", m, !state.isSubmitting, isPassword = true, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, onImeAction = viewModel::submit)
        AuthErrorText(state.error, m)
        AuthPrimaryButton("Update password", state.canSubmit, state.isSubmitting, viewModel::submit, m)
    }
}
