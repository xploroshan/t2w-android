package com.taleson2wheels.app.ui.rides

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terrain
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.taleson2wheels.app.R
import com.taleson2wheels.app.data.remote.dto.RideDetail
import com.taleson2wheels.app.ui.AppViewModelFactory
import com.taleson2wheels.app.ui.common.ErrorView
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.components.BrandBackground
import com.taleson2wheels.app.ui.components.BrandCard
import com.taleson2wheels.app.ui.components.GradientButton
import com.taleson2wheels.app.ui.components.GradientText
import com.taleson2wheels.app.ui.components.SecondaryButton
import com.taleson2wheels.app.ui.components.TagChip
import com.taleson2wheels.app.ui.theme.HeroGradient
import com.taleson2wheels.app.ui.theme.T2WGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    rideId: String,
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    onRegister: (rideId: String, rideTitle: String) -> Unit,
    onOpenPosts: (String) -> Unit,
    onOpenLive: (String) -> Unit,
    onOpenRelive: (String) -> Unit,
    onOpenInsights: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RideDetailViewModel = viewModel(factory = factory),
) {
    LaunchedEffect(rideId) { viewModel.load(rideId) }
    val state = viewModel.uiState
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(state.ride?.title ?: stringResource(R.string.rides_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.ride?.let { ride ->
                        IconButton(onClick = { shareRide(context, ride) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share ride")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingView(Modifier.padding(innerPadding))
            state.error != null -> ErrorView(
                message = state.error,
                onRetry = { viewModel.load(rideId) },
                modifier = Modifier.padding(innerPadding),
            )
            state.ride != null -> BrandBackground(Modifier.padding(innerPadding)) {
                RideDetailBody(
                    ride = state.ride,
                    onRegister = { onRegister(state.ride.id, state.ride.title) },
                    onOpenPosts = { onOpenPosts(state.ride.id) },
                    onOpenLive = { onOpenLive(state.ride.id) },
                    onOpenRelive = { onOpenRelive(state.ride.id) },
                    onOpenInsights = { onOpenInsights(state.ride.id) },
                    onAddToCalendar = { addRideToCalendar(context, state.ride) },
                )
            }
        }
    }
}

@Composable
internal fun RideDetailBody(
    ride: RideDetail,
    onRegister: () -> Unit,
    onOpenPosts: () -> Unit,
    onOpenLive: () -> Unit,
    onOpenRelive: () -> Unit,
    onOpenInsights: () -> Unit,
    onAddToCalendar: () -> Unit,
) {
    val isUpcoming = ride.status == "upcoming"
    val isLive = ride.status == "ongoing" || ride.status == "active" || ride.status == "live"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PosterHeader(ride)

        // Headline facts as compact cards.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            FactCard("Distance", "${ride.distanceKm.toInt()} km", Modifier.weight(1f))
            FactCard("Difficulty", ride.difficulty ?: "—", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            FactCard("Fee", if (ride.fee > 0) "₹${ride.fee.toInt()}" else "Free", Modifier.weight(1f))
            FactCard(
                "Riders",
                "${ride.registeredRiders}${if (ride.maxRiders > 0) "/${ride.maxRiders}" else ""}",
                Modifier.weight(1f),
            )
        }

        // The rest of the structured detail.
        BrandCard(modifier = Modifier.fillMaxWidth()) {
            formatRideDate(ride.startDate)?.let { DetailRow("When", it) }
            ride.type?.let { DetailRow("Type", it.replaceFirstChar { c -> c.uppercase() }) }
            ride.startLocation?.let { DetailRow("Start", it) }
            ride.endLocation?.let { DetailRow("End", it) }
            ride.currentUserApprovalStatus?.let { DetailRow("Your status", it.replaceFirstChar { c -> c.uppercase() }) }
        }

        if (!ride.description.isNullOrBlank()) {
            Text(
                text = ride.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (ride.currentUserRegistered) {
            Text(
                text = "✓ You're registered for this ride.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        } else if (isUpcoming) {
            GradientButton(text = "Register for this ride", onClick = onRegister, modifier = Modifier.fillMaxWidth())
        }

        if (isLive) {
            GradientButton(text = "Live tracking", onClick = onOpenLive, modifier = Modifier.fillMaxWidth())
        } else {
            SecondaryButton(text = "Live tracking", onClick = onOpenLive, modifier = Modifier.fillMaxWidth())
        }
        SecondaryButton(text = "Ride tales", onClick = onOpenPosts, modifier = Modifier.fillMaxWidth())
        if (!isUpcoming) {
            SecondaryButton(
                text = "Relive in 3D",
                onClick = onOpenRelive,
                leadingIcon = Icons.Filled.Terrain,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(text = "Ride insights", onClick = onOpenInsights, modifier = Modifier.fillMaxWidth())
        }
        if (isUpcoming && ride.startDate != null) {
            SecondaryButton(
                text = "Add to calendar",
                onClick = onAddToCalendar,
                leadingIcon = Icons.Filled.CalendarMonth,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PosterHeader(ride: RideDetail) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(20.dp))
            .background(HeroGradient),
    ) {
        if (!ride.posterUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ride.posterUrl,
                contentDescription = ride.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {},
                error = {},
            )
        }
        // Bottom scrim so the title stays legible over any poster.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)))),
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ride.rideNumber?.let {
                    Text("#$it", style = MaterialTheme.typography.titleMedium, color = T2WGold, fontWeight = FontWeight.Bold)
                }
                ride.status?.let { TagChip(it.replaceFirstChar { c -> c.uppercase() }) }
            }
            GradientText(ride.title, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun FactCard(label: String, value: String, modifier: Modifier = Modifier) {
    BrandCard(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = "$label",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ── Intents ─────────────────────────────────────────────────────────────────

private fun shareRide(context: Context, ride: RideDetail) {
    val text = buildString {
        append(ride.title)
        formatRideDate(ride.startDate)?.let { append("\n").append(it) }
        ride.startLocation?.let { append("\n").append(it) }
        append("\n\nRiding with Tales on 2 Wheels.")
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, ride.title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(send, "Share ride")) }
}

private fun addRideToCalendar(context: Context, ride: RideDetail) {
    val begin = rideEpochMillis(ride.startDate)
    val end = rideEpochMillis(ride.endDate) ?: begin?.plus(2 * 60 * 60 * 1000L)
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, ride.title)
        ride.startLocation?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
        ride.description?.let { putExtra(CalendarContract.Events.DESCRIPTION, it) }
        begin?.let { putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it) }
        end?.let { putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it) }
    }
    // No calendar app is possible — fail soft rather than crash.
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Intentionally ignored: device has no calendar app.
    }
}

/** Parse an ISO date/datetime to epoch millis, tolerating date-only values. */
private fun rideEpochMillis(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return runCatching { java.time.Instant.parse(iso).toEpochMilli() }
        .recoverCatching { java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli() }
        .recoverCatching {
            java.time.LocalDate.parse(iso).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        .getOrNull()
}

private fun formatRideDate(iso: String?): String? {
    val ms = rideEpochMillis(iso) ?: return null
    return java.time.Instant.ofEpochMilli(ms)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
}
