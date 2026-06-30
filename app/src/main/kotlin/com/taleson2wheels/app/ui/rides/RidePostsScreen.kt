package com.taleson2wheels.app.ui.rides

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.taleson2wheels.app.ui.common.ZoomableAsyncImage
import com.taleson2wheels.app.data.remote.dto.RidePost
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.auth.AuthErrorText
import com.taleson2wheels.app.ui.auth.AuthPrimaryButton
import com.taleson2wheels.app.ui.common.Avatar
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.common.readPickedImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RidePostsScreen(
    rideId: String,
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RidePostsViewModel = viewModel(factory = factory),
) {
    LaunchedEffect(rideId) { viewModel.load(rideId) }
    val state = viewModel.uiState
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.submittedMessage) {
        state.submittedMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissSubmittedMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Ride tales") },
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
            ExtendedFloatingActionButton(
                onClick = viewModel::openComposer,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add tale") },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingView(Modifier.padding(innerPadding))
            state.error != null && state.posts.isEmpty() ->
                ErrorView(state.error, { viewModel.load(rideId) }, Modifier.padding(innerPadding))
            state.posts.isEmpty() ->
                ErrorView("No tales yet. Be the first to share one.", { viewModel.load(rideId) }, Modifier.padding(innerPadding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.posts, key = { it.id }) { post -> RidePostCard(post) }
                if (state.canLoadMore || state.loadMoreError != null) {
                    item(key = "load-more") {
                        if (state.loadMoreError != null) {
                            Text(
                                text = "Couldn't load more — tap to retry",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.loadMore(rideId) }
                                    .padding(16.dp),
                            )
                        } else {
                            LaunchedEffect(state.nextCursor) { viewModel.loadMore(rideId) }
                            LoadingView(Modifier.fillMaxWidth().padding(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (state.composerOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = viewModel::closeComposer, sheetState = sheetState) {
            Composer(state = state, viewModel = viewModel, rideId = rideId)
        }
    }
}

@Composable
private fun RidePostCard(post: RidePost) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(url = post.authorAvatar, name = post.authorName, size = 32.dp)
                Text(
                    post.authorName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (post.content.isNotBlank()) {
                Text(post.content, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            if (post.images.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(post.images, key = { it }) { url ->
                        ZoomableAsyncImage(
                            url = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(160.dp).clip(RoundedCornerShape(12.dp)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Composer(state: RidePostsUiState, viewModel: RidePostsViewModel, rideId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val full = Modifier.fillMaxWidth()

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val img = context.readPickedImage(uri) ?: return@launch
            viewModel.uploadImage(img.bytes, img.fileName("tale"), img.mime)
        }
    }

    Column(
        modifier = full.padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Share a tale",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = state.draft,
            onValueChange = viewModel::onDraftChange,
            label = { Text("What happened on this ride?") },
            enabled = !state.isSubmitting,
            minLines = 3,
            modifier = full,
        )
        if (state.draftImages.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.draftImages, key = { it }) { url ->
                    androidx.compose.foundation.layout.Box {
                        SubcomposeAsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)),
                            loading = {},
                            error = {},
                        )
                        IconButton(onClick = { viewModel.removeDraftImage(url) }, modifier = Modifier.align(Alignment.TopEnd)) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = { pickImage.launch("image/*") }, enabled = !state.isUploading, modifier = full) {
            Text(if (state.isUploading) "Uploading…" else "Add photo")
        }
        AuthErrorText(state.composerError, full)
        AuthPrimaryButton(
            text = "Post tale",
            enabled = state.canSubmit,
            loading = state.isSubmitting,
            onClick = { viewModel.submit(rideId) },
            modifier = full,
        )
    }
}
