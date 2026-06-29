package com.taleson2wheels.app.ui.auth

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.taleson2wheels.app.ui.AppViewModelFactory

/**
 * Pre-authentication navigation: Login ↔ Register ↔ Reset password. A successful
 * login/register persists the session, which the app's auth gate ([T2WApp])
 * observes to swap this whole flow out for the signed-in shell.
 */
@Composable
fun AuthFlow(factory: AppViewModelFactory) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "login") {
        composable("login") {
            LoginScreen(
                factory = factory,
                onRegister = { nav.navigate("register") },
                onForgot = { nav.navigate("forgot") },
            )
        }
        composable("register") {
            RegisterScreen(factory = factory, onBack = { nav.popBackStack() })
        }
        composable("forgot") {
            ForgotPasswordScreen(factory = factory, onBack = { nav.popBackStack() })
        }
    }
}
