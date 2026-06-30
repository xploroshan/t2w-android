package com.taleson2wheels.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.data.remote.dto.EarnedBadgeDto
import com.taleson2wheels.app.data.remote.dto.MotorcycleDto
import com.taleson2wheels.app.data.remote.dto.UserDto
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.Avatar
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    factory: AppViewModelFactory,
    onOpenGuidelines: () -> Unit,
    onOpenCrew: () -> Unit,
    onOpenGarage: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    // Re-fetch when the screen re-enters composition (e.g. returning from edit).
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.refresh() }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    IconButton(onClick = viewModel::logout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingView(Modifier.padding(innerPadding))
            state.error != null && state.user == null ->
                ErrorView(state.error, viewModel::refresh, Modifier.padding(innerPadding))
            state.user != null -> ProfileBody(
                user = state.user,
                onOpenGuidelines = onOpenGuidelines,
                onOpenCrew = onOpenCrew,
                onOpenGarage = onOpenGarage,
                onEditProfile = onEditProfile,
                onChangePassword = onChangePassword,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ProfileBody(
    user: UserDto,
    onOpenGuidelines: () -> Unit,
    onOpenCrew: () -> Unit,
    onOpenGarage: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Avatar(url = user.avatar, name = user.name, size = 72.dp)
                Column {
                    Text(user.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    Text(user.email, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text(user.role, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MiniStat("${user.ridesCompleted}", "rides", Modifier.weight(1f))
                MiniStat("${user.totalKm.toInt()}", "km", Modifier.weight(1f))
                MiniStat("${user.earnedBadges.size}", "badges", Modifier.weight(1f))
            }
        }

        item { SectionHeader("Garage") }
        items(user.motorcycles, key = { it.id }) { MotorcycleCard(it) }
        item { LinkRow("Manage garage", onOpenGarage) }

        if (user.earnedBadges.isNotEmpty()) {
            item { SectionHeader("Badges") }
            items(user.earnedBadges, key = { it.id }) { BadgeRow(it) }
        }

        item { SectionHeader("More") }
        item { LinkRow("Edit profile", onEditProfile) }
        item { LinkRow("Ride guidelines", onOpenGuidelines) }
        item { LinkRow("Crew", onOpenCrew) }
        item { LinkRow("Change password", onChangePassword) }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun MotorcycleCard(bike: MotorcycleDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = listOfNotNull(bike.nickname, "${bike.make} ${bike.model}").first(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${bike.make} ${bike.model} · ${bike.year} · ${bike.cc}cc",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun BadgeRow(earned: EarnedBadgeDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = earned.badge?.name ?: earned.badge?.tier ?: "Badge",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            earned.badge?.description?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
