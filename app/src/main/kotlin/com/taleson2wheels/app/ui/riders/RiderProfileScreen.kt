package com.taleson2wheels.app.ui.riders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taleson2wheels.app.data.remote.dto.RiderDto
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.Avatar
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderProfileScreen(
    riderId: String,
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RiderProfileViewModel = viewModel(factory = factory),
) {
    LaunchedEffect(riderId) { viewModel.load(riderId) }
    val state = viewModel.uiState

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.rider?.name ?: "Rider") },
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
        when {
            state.isLoading -> LoadingView(Modifier.padding(innerPadding))
            state.error != null -> ErrorView(state.error, { viewModel.load(riderId) }, Modifier.padding(innerPadding))
            state.rider != null -> RiderProfileBody(state.rider, Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun RiderProfileBody(rider: RiderDto, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Avatar(url = rider.avatarUrl, name = rider.name, size = 96.dp)
        Text(
            text = rider.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
        rider.userRole?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StatLine("Points", rider.totalPoints.toInt().toString())
            StatLine("Rides completed", rider.ridesCompleted.toString())
            StatLine("Distance", "${rider.totalKm.toInt()} km")
            StatLine("Rides organized", rider.ridesOrganized.toString())
            StatLine("Sweeps", rider.sweepsDone.toString())
            StatLine("Pilots", rider.pilotsDone.toString())
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
