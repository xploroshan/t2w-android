package com.taleson2wheels.app.paparazzi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.taleson2wheels.app.data.remote.dto.BlogCard
import com.taleson2wheels.app.data.remote.dto.LiveMetrics
import com.taleson2wheels.app.data.remote.dto.LiveRiderPosition
import com.taleson2wheels.app.data.remote.dto.MotorcycleDto
import com.taleson2wheels.app.data.remote.dto.NotificationDto
import com.taleson2wheels.app.data.remote.dto.RideCard
import com.taleson2wheels.app.data.remote.dto.RiderDto
import com.taleson2wheels.app.data.remote.dto.StatsDto
import com.taleson2wheels.app.ui.auth.AuthErrorText
import com.taleson2wheels.app.ui.auth.AuthField
import com.taleson2wheels.app.ui.auth.AuthPrimaryButton
import com.taleson2wheels.app.ui.blogs.BlogCardItem
import com.taleson2wheels.app.ui.components.SectionHeader
import com.taleson2wheels.app.ui.home.Hero
import com.taleson2wheels.app.ui.home.NotificationCard
import com.taleson2wheels.app.ui.home.StatsGrid
import com.taleson2wheels.app.ui.live.MetricsCard
import com.taleson2wheels.app.ui.live.RiderRow
import com.taleson2wheels.app.ui.profile.MotorcycleCard
import com.taleson2wheels.app.ui.riders.Podium
import com.taleson2wheels.app.ui.riders.RiderRow as LeaderboardRiderRow
import com.taleson2wheels.app.ui.rides.RideCardItem
import com.taleson2wheels.app.ui.theme.T2WTheme
import org.junit.Rule
import org.junit.Test

/**
 * JVM screenshot tests (Paparazzi). Each renders REAL app composables with
 * sample data on the actual T2W theme — no emulator, device, or backend.
 *
 *   ./gradlew :app:recordPaparazziDebug   # write/update the golden PNGs
 *   ./gradlew :app:verifyPaparazziDebug   # fail on any visual diff (CI)
 *
 * Goldens live in app/src/test/snapshots/images/. To add a screen, expose its
 * presentational composable as `internal` and snapshot it here with sample data.
 */
@OptIn(ExperimentalMaterial3Api::class)
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    /** Theme + background, matching how every screen is hosted. */
    private fun snapshot(content: @Composable () -> Unit) = paparazzi.snapshot {
        T2WTheme {
            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }

    /** The app's branded top bar (primary container), used across screens. */
    @Composable
    private fun AppTopBar(title: String) {
        TopAppBar(
            title = { Text(title) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
    }

    @Test
    fun login() = snapshot {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Tales on 2 Wheels",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Sign in to ride with the crew",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            AuthField(value = "rider@taleson2wheels.com", onValueChange = {}, label = "Email", modifier = Modifier.fillMaxWidth())
            AuthField(value = "••••••••••", onValueChange = {}, label = "Password", isPassword = true, modifier = Modifier.fillMaxWidth())
            AuthErrorText("Invalid email or password", Modifier.fillMaxWidth())
            AuthPrimaryButton(text = "Sign in", enabled = true, loading = false, onClick = {}, modifier = Modifier.fillMaxWidth())
            Text("Forgot password?", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Text("New rider? Create an account", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }
    }

    @Test
    fun home_landing() = snapshot {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Hero(userName = "Aditi")
            StatsGrid(StatsDto(activeRiders = 128, ridesCompleted = 342, kmsCovered = 96450, countriesRidden = 7))
            SectionHeader("Upcoming rides")
            RideCardItem(
                RideCard(
                    id = "r1", title = "Coorg Monsoon Run", rideNumber = "42", status = "upcoming",
                    distanceKm = 320.0, difficulty = "Moderate", registeredRiders = 18,
                ),
            ) {}
            SectionHeader("What's new")
            NotificationCard(
                NotificationDto(id = "n1", title = "Spiti Expedition — registrations open", message = "10 seats · closes Friday"),
            )
        }
    }

    @Test
    fun leaderboard() = snapshot {
        val riders = listOf(
            RiderDto(id = "u1", name = "Aditi R", ridesCompleted = 42, totalKm = 9800.0, totalPoints = 1860.0),
            RiderDto(id = "u2", name = "Vikram S", ridesCompleted = 38, totalKm = 8600.0, totalPoints = 1640.0),
            RiderDto(id = "u3", name = "Neha K", ridesCompleted = 31, totalKm = 7100.0, totalPoints = 1390.0),
            RiderDto(id = "u4", name = "Rahul M", ridesCompleted = 27, totalKm = 6200.0, totalPoints = 1120.0),
            RiderDto(id = "u5", name = "Sana P", ridesCompleted = 22, totalKm = 5400.0, totalPoints = 980.0),
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Podium(riders.take(3)) {}
            riders.drop(3).forEachIndexed { i, r -> LeaderboardRiderRow(rank = i + 4, rider = r) {} }
        }
    }

    @Test
    fun rides_list() = snapshot {
        Column(Modifier.fillMaxSize()) {
            AppTopBar("Rides")
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RideCardItem(
                    RideCard(
                        id = "r1", title = "Coorg Monsoon Run", rideNumber = "42", status = "upcoming",
                        distanceKm = 320.0, difficulty = "Moderate", registeredRiders = 18, myRegistrationStatus = "confirmed",
                    ),
                ) {}
                RideCardItem(
                    RideCard(
                        id = "r2", title = "Sakleshpur Ghats Loop", rideNumber = "41", status = "completed",
                        distanceKm = 210.0, difficulty = "Easy", registeredRiders = 24,
                    ),
                ) {}
                RideCardItem(
                    RideCard(
                        id = "r3", title = "Spiti Expedition", rideNumber = "40", status = "upcoming",
                        distanceKm = 1450.0, difficulty = "Hard", registeredRiders = 9,
                    ),
                ) {}
            }
        }
    }

    @Test
    fun stories() = snapshot {
        Column(Modifier.fillMaxSize()) {
            AppTopBar("Stories")
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BlogCardItem(
                    BlogCard(
                        id = "b1", title = "Chasing the Western Ghats",
                        excerpt = "Three days, two mountain passes, and one unforgettable monsoon ride through Karnataka.",
                        authorName = "Aditi R", readTime = 6,
                    ),
                ) {}
                BlogCardItem(
                    BlogCard(
                        id = "b2", title = "Building a long-distance touring setup",
                        excerpt = "What actually mattered after 12,000 km on the saddle.",
                        authorName = "Vikram S", readTime = 9, isVlog = true,
                    ),
                ) {}
            }
        }
    }

    @Test
    fun live_ride() = snapshot {
        Column(Modifier.fillMaxSize()) {
            AppTopBar("Live ride")
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("LIVE", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                MetricsCard(
                    LiveMetrics(
                        elapsedMinutes = 184.0, movingMinutes = 151.0, distanceKm = 124.5,
                        avgSpeedKmh = 46.4, maxSpeedKmh = 92.0, breakCount = 2, breakMinutes = 33.0, riderCount = 12,
                    ),
                )
                RiderRow(LiveRiderPosition(userId = "u1", userName = "Aditi R", lat = 0.0, lng = 0.0, speed = 13.4, isLead = true))
                RiderRow(LiveRiderPosition(userId = "u2", userName = "Vikram S", lat = 0.0, lng = 0.0, speed = 11.1, isSweep = true))
                RiderRow(LiveRiderPosition(userId = "u3", userName = "Neha K", lat = 0.0, lng = 0.0, speed = 8.0, isDeviated = true))
            }
        }
    }

    @Test
    fun profile_garage() = snapshot {
        Column(Modifier.fillMaxSize()) {
            AppTopBar("Profile")
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Garage", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                MotorcycleCard(MotorcycleDto(id = "m1", make = "Royal Enfield", model = "Himalayan", year = 2023, cc = 411, nickname = "Yeti"))
                MotorcycleCard(MotorcycleDto(id = "m2", make = "KTM", model = "390 Adventure", year = 2022, cc = 373))
            }
        }
    }
}
