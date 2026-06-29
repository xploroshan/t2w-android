package com.taleson2wheels.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.taleson2wheels.app.di.AppContainer
import com.taleson2wheels.app.ui.auth.ChangePasswordViewModel
import com.taleson2wheels.app.ui.auth.ForgotPasswordViewModel
import com.taleson2wheels.app.ui.auth.LoginViewModel
import com.taleson2wheels.app.ui.auth.RegisterViewModel
import com.taleson2wheels.app.ui.content.CrewViewModel
import com.taleson2wheels.app.ui.content.GuidelinesViewModel
import com.taleson2wheels.app.ui.home.HomeViewModel
import com.taleson2wheels.app.ui.profile.ProfileViewModel
import com.taleson2wheels.app.ui.riders.LeaderboardViewModel
import com.taleson2wheels.app.ui.riders.RiderProfileViewModel
import com.taleson2wheels.app.ui.rides.RideDetailViewModel
import com.taleson2wheels.app.ui.rides.RidesViewModel

/**
 * Minimal manual ViewModel factory — maps view-model types to the repositories
 * held by the [AppContainer]. Replace with Hilt's `@HiltViewModel` if/when the
 * project adopts Hilt.
 */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(LoginViewModel::class.java) ->
            LoginViewModel(container.authRepository) as T

        modelClass.isAssignableFrom(RegisterViewModel::class.java) ->
            RegisterViewModel(container.authRepository) as T

        modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java) ->
            ForgotPasswordViewModel(container.authRepository) as T

        modelClass.isAssignableFrom(ChangePasswordViewModel::class.java) ->
            ChangePasswordViewModel(container.authRepository) as T

        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(container.catalogRepository, container.authRepository) as T

        modelClass.isAssignableFrom(RidesViewModel::class.java) ->
            RidesViewModel(container.ridesRepository, container.authRepository) as T

        modelClass.isAssignableFrom(RideDetailViewModel::class.java) ->
            RideDetailViewModel(container.ridesRepository) as T

        modelClass.isAssignableFrom(LeaderboardViewModel::class.java) ->
            LeaderboardViewModel(container.ridersRepository) as T

        modelClass.isAssignableFrom(RiderProfileViewModel::class.java) ->
            RiderProfileViewModel(container.ridersRepository) as T

        modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
            ProfileViewModel(container.authRepository) as T

        modelClass.isAssignableFrom(GuidelinesViewModel::class.java) ->
            GuidelinesViewModel(container.catalogRepository) as T

        modelClass.isAssignableFrom(CrewViewModel::class.java) ->
            CrewViewModel(container.catalogRepository) as T

        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
