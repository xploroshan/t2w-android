package com.taleson2wheels.app.ui.garage

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.taleson2wheels.app.data.remote.dto.MotorcycleDto
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.auth.AuthErrorText
import com.taleson2wheels.app.ui.auth.AuthField
import com.taleson2wheels.app.ui.auth.AuthPrimaryButton
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GarageViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    var pendingDelete by remember { mutableStateOf<MotorcycleDto?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("My Garage") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::startAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add motorcycle")
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingView(Modifier.padding(innerPadding))
            state.error != null && state.bikes.isEmpty() ->
                ErrorView(state.error, viewModel::load, Modifier.padding(innerPadding))
            else -> GarageList(
                state = state,
                onEdit = viewModel::startEdit,
                onDelete = { pendingDelete = it },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    state.editor?.let { editor ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = viewModel::dismissEditor, sheetState = sheetState) {
            GarageEditor(editor = editor, viewModel = viewModel)
        }
    }

    pendingDelete?.let { bike ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove motorcycle?") },
            text = { Text("Remove ${bikeTitle(bike)} from your garage? This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(bike.id)
                    pendingDelete = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun GarageList(
    state: GarageUiState,
    onEdit: (MotorcycleDto) -> Unit,
    onDelete: (MotorcycleDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.bikes.isEmpty()) {
            item {
                Text(
                    "No motorcycles yet. Tap + to add your first bike.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        items(state.bikes, key = { it.id }) { bike ->
            MotorcycleCard(
                bike = bike,
                isDeleting = state.deletingId == bike.id,
                onEdit = { onEdit(bike) },
                onDelete = { onDelete(bike) },
            )
        }
    }
}

@Composable
private fun MotorcycleCard(
    bike: MotorcycleDto,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            if (!bike.imageUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = bike.imageUrl,
                    contentDescription = bikeTitle(bike),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    loading = {},
                    error = {},
                )
            }
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = bikeTitle(bike),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = listOfNotNull(
                        "${bike.make} ${bike.model}",
                        bike.year.takeIf { it > 0 }?.toString(),
                        bike.cc.takeIf { it > 0 }?.let { "${it}cc" },
                        bike.color?.ifBlank { null },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onEdit, enabled = !isDeleting) { Text("Edit") }
                    TextButton(onClick = onDelete, enabled = !isDeleting) {
                        Text(if (isDeleting) "Removing…" else "Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun GarageEditor(editor: GarageEditorState, viewModel: GarageViewModel) {
    val context = LocalContext.current
    val full = Modifier.fillMaxWidth()

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                if (bytes != null) viewModel.uploadImage(bytes, "bike.${mime.substringAfter('/')}", mime)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (editor.isEditing) "Edit motorcycle" else "Add motorcycle",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )

        if (!editor.imageUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = editor.imageUrl,
                contentDescription = "Motorcycle photo",
                contentScale = ContentScale.Crop,
                modifier = full.aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)),
                loading = {},
                error = {},
            )
        }

        AuthField(editor.make, viewModel::onMakeChange, "Make", full, !editor.isSaving)
        AuthField(editor.model, viewModel::onModelChange, "Model", full, !editor.isSaving)
        AuthField(editor.year, viewModel::onYearChange, "Year", full, !editor.isSaving, keyboardType = KeyboardType.Number)
        AuthField(editor.cc, viewModel::onCcChange, "Engine (cc)", full, !editor.isSaving, keyboardType = KeyboardType.Number)
        AuthField(editor.color, viewModel::onColorChange, "Color", full, !editor.isSaving)
        AuthField(editor.nickname, viewModel::onNicknameChange, "Nickname (optional)", full, !editor.isSaving, imeAction = ImeAction.Done)

        OutlinedButton(onClick = { pickImage.launch("image/*") }, enabled = !editor.isUploading, modifier = full) {
            Text(
                when {
                    editor.isUploading -> "Uploading…"
                    editor.imageUrl != null -> "Change photo"
                    else -> "Add photo"
                },
            )
        }

        AuthErrorText(editor.error, full)
        AuthPrimaryButton(
            text = if (editor.isEditing) "Save changes" else "Add to garage",
            enabled = editor.canSave,
            loading = editor.isSaving,
            onClick = viewModel::save,
            modifier = full,
        )
    }
}

private fun bikeTitle(bike: MotorcycleDto): String =
    bike.nickname?.ifBlank { null } ?: "${bike.make} ${bike.model}"
