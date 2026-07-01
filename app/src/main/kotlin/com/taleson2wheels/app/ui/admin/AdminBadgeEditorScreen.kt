package com.taleson2wheels.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.components.BrandBackground

private val BADGE_KINDS = listOf("lifetime_km", "per_ride")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBadgeEditorScreen(
    badgeId: String?,
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminBadgeEditorViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    LaunchedEffect(badgeId) { viewModel.load(badgeId) }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Edit badge" else "New badge") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        BrandBackground(Modifier.padding(innerPadding)) {
            when {
                state.isLoading -> LoadingView()
                state.loadError != null -> ErrorView(state.loadError, onRetry = { viewModel.load(badgeId) })
                else -> BadgeFormBody(state = state, viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgeFormBody(state: AdminBadgeEditorUiState, viewModel: AdminBadgeEditorViewModel) {
    val form = state.form
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OutlinedTextField(
            value = form.tier,
            onValueChange = { v -> viewModel.onForm { copy(tier = v) } },
            label = { Text("Tier") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        KindDropdown(form.kind) { v -> viewModel.onForm { copy(kind = v) } }
        OutlinedTextField(
            value = form.name,
            onValueChange = { v -> viewModel.onForm { copy(name = v) } },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.description,
            onValueChange = { v -> viewModel.onForm { copy(description = v) } },
            label = { Text("Description") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.minKm,
            onValueChange = { v -> viewModel.onForm { copy(minKm = v) } },
            label = { Text("Minimum km") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.icon,
            onValueChange = { v -> viewModel.onForm { copy(icon = v) } },
            label = { Text("Icon (emoji or name)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.color,
            onValueChange = { v -> viewModel.onForm { copy(color = v) } },
            label = { Text("Colour (hex)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        (state.validationError ?: state.saveError)?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = viewModel::save,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            }
            Text(if (state.isEdit) "Save changes" else "Create badge")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KindDropdown(value: String, onPick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value.replace('_', ' ').replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Kind") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BADGE_KINDS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                    onClick = { onPick(option); expanded = false },
                )
            }
        }
    }
}
