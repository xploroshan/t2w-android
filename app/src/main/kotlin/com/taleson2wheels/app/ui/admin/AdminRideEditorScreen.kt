package com.taleson2wheels.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.components.BrandBackground
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val RIDE_TYPES = listOf("day", "weekend", "multi-day", "expedition")
private val RIDE_STATUSES = listOf("upcoming", "ongoing", "completed", "cancelled")
private val DIFFICULTIES = listOf("easy", "moderate", "challenging", "extreme")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRideEditorScreen(
    rideId: String?,
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminRideEditorViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    LaunchedEffect(rideId) { viewModel.load(rideId) }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Edit ride" else "New ride") },
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
                state.loadError != null -> ErrorView(state.loadError, onRetry = { viewModel.load(rideId) })
                else -> RideForm(state = state, viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RideForm(state: AdminRideEditorUiState, viewModel: AdminRideEditorViewModel) {
    val form = state.form
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        state.rideNumber?.takeIf { it.isNotBlank() }?.let {
            Text("Ride $it", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }

        OutlinedTextField(
            value = form.title,
            onValueChange = { v -> viewModel.onForm { copy(title = v) } },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        EnumDropdown("Type", form.type, RIDE_TYPES) { v -> viewModel.onForm { copy(type = v) } }
        EnumDropdown("Status", form.status, RIDE_STATUSES) { v -> viewModel.onForm { copy(status = v) } }
        EnumDropdown("Difficulty", form.difficulty, DIFFICULTIES) { v -> viewModel.onForm { copy(difficulty = v) } }

        DateField("Start date", form.startDateMillis) { showStartPicker = true }
        DateField("End date", form.endDateMillis) { showEndPicker = true }

        OutlinedTextField(
            value = form.startLocation,
            onValueChange = { v -> viewModel.onForm { copy(startLocation = v) } },
            label = { Text("Start location") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.endLocation,
            onValueChange = { v -> viewModel.onForm { copy(endLocation = v) } },
            label = { Text("End location") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = form.distanceKm,
                onValueChange = { v -> viewModel.onForm { copy(distanceKm = v) } },
                label = { Text("Distance (km)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = form.maxRiders,
                onValueChange = { v -> viewModel.onForm { copy(maxRiders = v) } },
                label = { Text("Max riders") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = form.fee,
            onValueChange = { v -> viewModel.onForm { copy(fee = v) } },
            label = { Text("Fee (₹)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.description,
            onValueChange = { v -> viewModel.onForm { copy(description = v) } },
            label = { Text("Description") },
            minLines = 3,
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
            Text(if (state.isEdit) "Save changes" else "Create ride")
        }
    }

    if (showStartPicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = form.startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onForm { copy(startDateMillis = picker.selectedDateMillis) }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = picker) }
    }
    if (showEndPicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = form.endDateMillis ?: form.startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onForm { copy(endDateMillis = picker.selectedDateMillis) }
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = picker) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnumDropdown(label: String, value: String, options: List<String>, onPick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value.replace('-', ' ').replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replace('-', ' ').replaceFirstChar { it.uppercase() }) },
                    onClick = { onPick(option); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun DateField(label: String, millis: Long?, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(millis?.let(::formatPickedDate) ?: "Select", modifier = Modifier.weight(1f))
            }
        }
    }
}

private val pickedDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun formatPickedDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).format(pickedDateFormatter)
