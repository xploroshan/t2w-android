package com.taleson2wheels.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.auth.AuthErrorText
import com.taleson2wheels.app.ui.auth.AuthField
import com.taleson2wheels.app.ui.auth.AuthPrimaryButton
import com.taleson2wheels.app.ui.auth.AuthScaffold
import com.taleson2wheels.app.ui.common.Avatar
import com.taleson2wheels.app.ui.common.LoadingView

@Composable
fun ProfileEditScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    viewModel: ProfileEditViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    val context = LocalContext.current

    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                if (bytes != null) viewModel.uploadAvatar(bytes, "avatar.${mime.substringAfter('/')}", mime)
            }
        }
    }

    AuthScaffold(title = "Edit profile", onBack = onBack) { m ->
        if (state.isLoading) {
            LoadingView()
            return@AuthScaffold
        }

        Box(modifier = m, contentAlignment = Alignment.Center) {
            Avatar(url = state.avatarUrl, name = state.name.ifBlank { "?" }, size = 88.dp)
        }
        OutlinedButton(onClick = { pickImage.launch("image/*") }, enabled = !state.isUploading, modifier = m) {
            Text(if (state.isUploading) "Uploading…" else "Change photo")
        }

        AuthField(state.name, viewModel::onNameChange, "Name", m, !state.isSaving)
        AuthField(state.phone, viewModel::onPhoneChange, "Phone", m, !state.isSaving, keyboardType = KeyboardType.Phone)
        AuthField(state.city, viewModel::onCityChange, "City", m, !state.isSaving)
        AuthField(state.ridingExperience, viewModel::onExperienceChange, "Riding experience", m, !state.isSaving, imeAction = ImeAction.Done)

        AuthErrorText(state.error, m)
        AuthPrimaryButton("Save", state.canSave, state.isSaving, viewModel::save, m)

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Name, phone, city, riding experience and photo only — your email and role can't be changed here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
