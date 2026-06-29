package com.taleson2wheels.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taleson2wheels.app.di.AppContainer
import com.taleson2wheels.app.ui.auth.LoginScreen
import com.taleson2wheels.app.ui.common.LoadingView
import com.taleson2wheels.app.ui.rides.RideDetailScreen
import com.taleson2wheels.app.ui.rides.RidesScreen
import com.taleson2wheels.app.ui.theme.T2WTheme

private object Routes {
    const val RIDES = "rides"
    const val RIDE_DETAIL = "rides/{rideId}"
    fun rideDetail(id: String) = "rides/$id"
}

/**
 * Root composable + auth gate. The session lives in [AppContainer.session]; this
 * observes it to switch between the login screen and the signed-in app shell.
 * Clearing the session anywhere (logout, refresh failure) automatically returns
 * the user to login — no manual navigation required.
 */
@Composable
fun T2WApp(container: AppContainer) {
    T2WTheme {
        val factory = remember(container) { AppViewModelFactory(container) }
        val ready by container.session.ready.collectAsStateWithLifecycle()
        val tokens by container.session.tokens.collectAsStateWithLifecycle()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when {
                !ready -> LoadingView()
                tokens == null -> LoginScreen(factory = factory)
                else -> SignedInNavHost(factory = factory)
            }
        }
    }
}

@Composable
private fun SignedInNavHost(factory: AppViewModelFactory) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.RIDES) {
        composable(Routes.RIDES) {
            RidesScreen(
                factory = factory,
                onRideClick = { id -> navController.navigate(Routes.rideDetail(id)) },
            )
        }
        composable(
            route = Routes.RIDE_DETAIL,
            arguments = listOf(navArgument("rideId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val rideId = backStackEntry.arguments?.getString("rideId").orEmpty()
            RideDetailScreen(
                rideId = rideId,
                factory = factory,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
