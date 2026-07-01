package com.taleson2wheels.app.ui.contact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.auth.AuthScaffold
import com.taleson2wheels.app.ui.components.BrandCard
import com.taleson2wheels.app.ui.components.GradientButton
import com.taleson2wheels.app.ui.components.InputField

@Composable
fun ContactScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    viewModel: ContactViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    AuthScaffold(title = "Contact us", onBack = onBack) { m ->
        if (state.sent) {
            BrandCard(modifier = m) {
                Text(
                    "Thanks for reaching out!",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "We've got your message and will get back to you soon.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            GradientButton(text = "Done", onClick = onBack, modifier = m.padding(top = 4.dp))
            return@AuthScaffold
        }

        Text(
            "Questions about a ride, joining the crew, or anything else? Send us a note.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        InputField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = "Your name",
            enabled = !state.isSubmitting,
            modifier = m,
        )
        InputField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email",
            keyboardType = KeyboardType.Email,
            enabled = !state.isSubmitting,
            modifier = m,
        )
        InputField(
            value = state.subject,
            onValueChange = viewModel::onSubjectChange,
            label = "Subject",
            enabled = !state.isSubmitting,
            modifier = m,
        )
        InputField(
            value = state.message,
            onValueChange = viewModel::onMessageChange,
            label = "Message",
            singleLine = false,
            enabled = !state.isSubmitting,
            error = state.error,
            modifier = m.heightIn(min = 120.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = m) {
            GradientButton(
                text = "Send message",
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                loading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
