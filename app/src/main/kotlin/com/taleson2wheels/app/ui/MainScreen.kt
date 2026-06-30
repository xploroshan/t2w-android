package com.taleson2wheels.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taleson2wheels.app.ui.content.CrewScreen
import com.taleson2wheels.app.ui.content.GuidelinesScreen
import com.taleson2wheels.app.ui.garage.GarageScreen
import com.taleson2wheels.app.ui.home.HomeScreen
import com.taleson2wheels.app.ui.live.LiveRideScreen
import com.taleson2wheels.app.ui.profile.ProfileScreen
import com.taleson2wheels.app.ui.riders.LeaderboardScreen
import com.taleson2wheels.app.ui.riders.RiderProfileScreen
import com.taleson2wheels.app.ui.rides.RegistrationFormScreen
import com.taleson2wheels.app.ui.rides.RideDetailScreen
import com.taleson2wheels.app.ui.rides.RidePostsScreen
import com.taleson2wheels.app.ui.rides.RidesScreen

object Routes {
    const val HOME = "home"
    const val RIDES = "rides"
    const val RIDERS = "riders"
    const val PROFILE = "profile"
    const val RIDE_DETAIL = "rides/{rideId}"
    const val RIDE_REGISTER = "rides/{rideId}/register?title={title}"
    const val RIDE_POSTS = "rides/{rideId}/posts"
    const val RIDE_LIVE = "rides/{rideId}/live"
    const val RIDER_PROFILE = "riders/{riderId}"
    const val GUIDELINES = "guidelines"
    const val CREW = "crew"
    const val GARAGE = "garage"
    const val CHANGE_PASSWORD = "change-password"
    fun rideDetail(id: String) = "rides/$id"
    fun rideRegister(id: String, title: String) = "rides/$id/register?title=${Uri.encode(title)}"
    fun ridePosts(id: String) = "rides/$id/posts"
    fun rideLive(id: String) = "rides/$id/live"
    fun riderProfile(id: String) = "riders/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.RIDES, "Rides", Icons.Filled.TwoWheeler),
    Tab(Routes.RIDERS, "Riders", Icons.Filled.EmojiEvents),
    Tab(Routes.PROFILE, "Profile", Icons.Filled.Person),
)

/** Signed-in app shell: a bottom-nav scaffold hosting the tab + detail graph. */
@Composable
fun MainScreen(factory: AppViewModelFactory) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onTab = tabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (onTab) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) { HomeScreen(factory = factory) }

            composable(Routes.RIDES) {
                RidesScreen(
                    factory = factory,
                    onRideClick = { id -> navController.navigate(Routes.rideDetail(id)) },
                )
            }
            composable(
                route = Routes.RIDE_DETAIL,
                arguments = listOf(navArgument("rideId") { type = NavType.StringType }),
            ) { entry ->
                RideDetailScreen(
                    rideId = entry.arguments?.getString("rideId").orEmpty(),
                    factory = factory,
                    onBack = { navController.popBackStack() },
                    onRegister = { id, title -> navController.navigate(Routes.rideRegister(id, title)) },
                    onOpenPosts = { id -> navController.navigate(Routes.ridePosts(id)) },
                    onOpenLive = { id -> navController.navigate(Routes.rideLive(id)) },
                )
            }
            composable(
                route = Routes.RIDE_LIVE,
                arguments = listOf(navArgument("rideId") { type = NavType.StringType }),
            ) { entry ->
                LiveRideScreen(
                    rideId = entry.arguments?.getString("rideId").orEmpty(),
                    factory = factory,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.RIDE_POSTS,
                arguments = listOf(navArgument("rideId") { type = NavType.StringType }),
            ) { entry ->
                RidePostsScreen(
                    rideId = entry.arguments?.getString("rideId").orEmpty(),
                    factory = factory,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.RIDE_REGISTER,
                arguments = listOf(
                    navArgument("rideId") { type = NavType.StringType },
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                RegistrationFormScreen(
                    rideId = entry.arguments?.getString("rideId").orEmpty(),
                    rideTitle = entry.arguments?.getString("title").orEmpty(),
                    factory = factory,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.RIDERS) {
                LeaderboardScreen(
                    factory = factory,
                    onRiderClick = { id -> navController.navigate(Routes.riderProfile(id)) },
                )
            }
            composable(
                route = Routes.RIDER_PROFILE,
                arguments = listOf(navArgument("riderId") { type = NavType.StringType }),
            ) { entry ->
                RiderProfileScreen(
                    riderId = entry.arguments?.getString("riderId").orEmpty(),
                    factory = factory,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    factory = factory,
                    onOpenGuidelines = { navController.navigate(Routes.GUIDELINES) },
                    onOpenCrew = { navController.navigate(Routes.CREW) },
                    onOpenGarage = { navController.navigate(Routes.GARAGE) },
                    onChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                )
            }
            composable(Routes.GARAGE) {
                GarageScreen(factory = factory, onBack = { navController.popBackStack() })
            }
            composable(Routes.GUIDELINES) {
                GuidelinesScreen(factory = factory, onBack = { navController.popBackStack() })
            }
            composable(Routes.CREW) {
                CrewScreen(factory = factory, onBack = { navController.popBackStack() })
            }
            composable(Routes.CHANGE_PASSWORD) {
                com.taleson2wheels.app.ui.auth.ChangePasswordScreen(
                    factory = factory,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
