package com.taleson2wheels.app.ui.blogs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.auth.AuthErrorText
import com.taleson2wheels.app.ui.auth.AuthPrimaryButton
import com.taleson2wheels.app.ui.common.readPickedImage
import kotlinx.coroutines.launch

/**
 * Full-screen "write a story" composer. On success it shows the moderation note
 * (e.g. "submitted for approval") as a snackbar, then pops via [onBack].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogComposerScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BlogComposerViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val full = Modifier.fillMaxWidth()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.doneMessage) {
        state.doneMessage?.let {
            snackbar.showSnackbar(it)
            onBack()
        }
    }

    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val img = context.readPickedImage(uri) ?: return@launch
            viewModel.uploadCover(img.bytes, img.fileName("cover"), img.mime)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Write a story") },
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.coverImage != null) {
                Box {
                    SubcomposeAsyncImage(
                        model = state.coverImage,
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = full.aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)),
                        loading = {},
                        error = {},
                    )
                    IconButton(
                        onClick = viewModel::removeCover,
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove cover", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            OutlinedButton(
                onClick = { pickCover.launch("image/*") },
                enabled = !state.isUploading,
                modifier = full,
            ) {
                Text(
                    when {
                        state.isUploading -> "Uploading…"
                        state.coverImage != null -> "Replace cover photo"
                        else -> "Add cover photo"
                    },
                )
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title") },
                singleLine = true,
                enabled = !state.isSubmitting,
                modifier = full,
            )
            OutlinedTextField(
                value = state.excerpt,
                onValueChange = viewModel::onExcerptChange,
                label = { Text("Short summary (optional)") },
                enabled = !state.isSubmitting,
                minLines = 2,
                modifier = full,
            )
            OutlinedTextField(
                value = state.content,
                onValueChange = viewModel::onContentChange,
                label = { Text("Your story") },
                enabled = !state.isSubmitting,
                minLines = 6,
                modifier = full,
            )
            OutlinedTextField(
                value = state.tagsText,
                onValueChange = viewModel::onTagsChange,
                label = { Text("Tags (comma-separated)") },
                enabled = !state.isSubmitting,
                singleLine = true,
                modifier = full,
            )

            androidx.compose.foundation.layout.Row(
                modifier = full,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "This is a vlog",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Switch(checked = state.isVlog, onCheckedChange = viewModel::toggleVlog, enabled = !state.isSubmitting)
            }
            if (state.isVlog) {
                OutlinedTextField(
                    value = state.videoUrl,
                    onValueChange = viewModel::onVideoUrlChange,
                    label = { Text("Video URL") },
                    enabled = !state.isSubmitting,
                    singleLine = true,
                    modifier = full,
                )
            }

            AuthErrorText(state.error, full)
            AuthPrimaryButton(
                text = "Publish story",
                enabled = state.canSubmit,
                loading = state.isSubmitting,
                onClick = viewModel::submit,
                modifier = full,
            )
        }
    }
}
