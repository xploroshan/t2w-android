package com.taleson2wheels.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.taleson2wheels.app.ui.components.BrandBackground
import com.taleson2wheels.app.ui.components.BrandCard
import com.taleson2wheels.app.ui.components.SectionHeader
import com.taleson2wheels.app.ui.theme.badgeTierColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    factory: AppViewModelFactory,
    onOpenGuidelines: () -> Unit,
    onOpenCrew: () -> Unit,
    onOpenGarage: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenContact: () -> Unit,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = factory),
) {
    val state = viewModel.uiState
    // Re-fetch when the screen re-enters composition (e.g. returning from edit).
    LaunchedEffect(Unit) { viewModel.refresh() }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
                    IconButton(onClick = viewModel::logout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                },
            )
        },
    ) { innerPadding ->
        BrandBackground(Modifier.padding(innerPadding)) {
            when {
                state.isLoading -> LoadingView()
                state.error != null && state.user == null ->
                    ErrorView(state.error, viewModel::refresh)
                state.user != null -> ProfileBody(
                    user = state.user,
                    onOpenGuidelines = onOpenGuidelines,
                    onOpenCrew = onOpenCrew,
                    onOpenGarage = onOpenGarage,
                    onEditProfile = onEditProfile,
                    onChangePassword = onChangePassword,
                    onOpenAbout = onOpenAbout,
                    onOpenContact = onOpenContact,
                    onOpenAdmin = onOpenAdmin,
                )
            }
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
    onOpenAbout: () -> Unit,
    onOpenContact: () -> Unit,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Avatar(url = user.avatar, name = user.name, size = 72.dp)
                Column {
                    Text(user.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = user.role.replace('_', ' ').replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
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

        // Role-gated admin console entry. Each destination's per-toggle
        // permissions (canApproveUsers, canManageRegistrations, canApproveContent,
        // …) are enforced server-side; core members / superadmins see the entry
        // and each screen surfaces a 403 if its toggle is off.
        if (user.role == "core_member" || user.role == "superadmin") {
            item { SectionHeader("Admin") }
            item { LinkRow("Admin console", onOpenAdmin) }
        }

        item { SectionHeader("More") }
        item { LinkRow("Edit profile", onEditProfile) }
        item { LinkRow("Ride guidelines", onOpenGuidelines) }
        item { LinkRow("Crew", onOpenCrew) }
        item { LinkRow("About & connect", onOpenAbout) }
        item { LinkRow("Contact us", onOpenContact) }
        item { LinkRow("Change password", onChangePassword) }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    BrandCard(modifier = modifier, contentPadding = PaddingValues(vertical = 14.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun MotorcycleCard(bike: MotorcycleDto) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            // A non-null but blank nickname must not render an empty title —
            // fall back to make + model.
            text = bike.nickname?.takeIf { it.isNotBlank() } ?: "${bike.make} ${bike.model}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "${bike.make} ${bike.model} · ${bike.year} · ${bike.cc}cc",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BadgeRow(earned: EarnedBadgeDto) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = earned.badge?.name ?: earned.badge?.tier ?: "Badge",
            style = MaterialTheme.typography.titleLarge,
            color = badgeTierColor(earned.badge?.tier),
            fontWeight = FontWeight.SemiBold,
        )
        earned.badge?.description?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    BrandCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
