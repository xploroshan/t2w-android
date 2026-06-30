package com.taleson2wheels.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.R
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.components.BrandBackground
import com.taleson2wheels.app.ui.components.BrandWordmark

@Composable
fun LoginScreen(
    factory: AppViewModelFactory,
    onRegister: () -> Unit = {},
    onForgot: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    BrandBackground(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Brand wordmark as the hero moment, matching the website's dark login.
            BrandWordmark(style = MaterialTheme.typography.displaySmall)
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            )

            AuthField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = stringResource(R.string.login_email),
                enabled = !state.isSubmitting,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth(),
            )
            AuthField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = stringResource(R.string.login_password),
                enabled = !state.isSubmitting,
                isPassword = true,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onImeAction = viewModel::submit,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            AuthErrorText(
                message = state.error,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )

            AuthPrimaryButton(
                text = stringResource(R.string.login_button),
                enabled = state.canSubmit,
                loading = state.isSubmitting,
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )

            Text(
                text = "Forgot password?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp).clickableText(onForgot),
            )
            Text(
                text = "New rider? Create an account",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp).clickableText(onRegister),
            )
        }
    }
}
