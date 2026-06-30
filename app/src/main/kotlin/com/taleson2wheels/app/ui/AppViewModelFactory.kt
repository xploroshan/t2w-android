package com.taleson2wheels.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.taleson2wheels.app.di.AppContainer
import com.taleson2wheels.app.ui.auth.ChangePasswordViewModel
import com.taleson2wheels.app.ui.auth.ForgotPasswordViewModel
import com.taleson2wheels.app.ui.auth.LoginViewModel
import com.taleson2wheels.app.ui.achievements.AchievementsViewModel
import com.taleson2wheels.app.ui.auth.RegisterViewModel
import com.taleson2wheels.app.ui.blogs.BlogComposerViewModel
import com.taleson2wheels.app.ui.blogs.BlogDetailViewModel
import com.taleson2wheels.app.ui.blogs.BlogsViewModel
import com.taleson2wheels.app.ui.content.CrewViewModel
import com.taleson2wheels.app.ui.content.GuidelinesViewModel
import com.taleson2wheels.app.ui.garage.GarageViewModel
import com.taleson2wheels.app.ui.home.HomeViewModel
import com.taleson2wheels.app.ui.live.LiveInsightsViewModel
import com.taleson2wheels.app.ui.live.LiveRideViewModel
import com.taleson2wheels.app.ui.notifications.NotificationsViewModel
import com.taleson2wheels.app.ui.profile.ProfileEditViewModel
import com.taleson2wheels.app.ui.profile.ProfileViewModel
import com.taleson2wheels.app.ui.riders.LeaderboardViewModel
import com.taleson2wheels.app.ui.riders.RiderProfileViewModel
import com.taleson2wheels.app.ui.rides.RegistrationViewModel
import com.taleson2wheels.app.ui.rides.RideDetailViewModel
import com.taleson2wheels.app.ui.rides.RidePostsViewModel
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

        modelClass.isAssignableFrom(RegistrationViewModel::class.java) ->
            RegistrationViewModel(container.ridesRepository, container.uploadRepository) as T

        modelClass.isAssignableFrom(RidePostsViewModel::class.java) ->
            RidePostsViewModel(container.ridesRepository, container.uploadRepository) as T

        modelClass.isAssignableFrom(LiveRideViewModel::class.java) ->
            LiveRideViewModel(container.liveRepository) as T

        modelClass.isAssignableFrom(LiveInsightsViewModel::class.java) ->
            LiveInsightsViewModel(container.liveRepository) as T

        modelClass.isAssignableFrom(LeaderboardViewModel::class.java) ->
            LeaderboardViewModel(container.ridersRepository) as T

        modelClass.isAssignableFrom(AchievementsViewModel::class.java) ->
            AchievementsViewModel(container.catalogRepository) as T

        modelClass.isAssignableFrom(RiderProfileViewModel::class.java) ->
            RiderProfileViewModel(container.ridersRepository) as T

        modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
            ProfileViewModel(container.authRepository) as T

        modelClass.isAssignableFrom(ProfileEditViewModel::class.java) ->
            ProfileEditViewModel(container.authRepository, container.uploadRepository) as T

        modelClass.isAssignableFrom(GarageViewModel::class.java) ->
            GarageViewModel(container.garageRepository, container.uploadRepository) as T

        modelClass.isAssignableFrom(NotificationsViewModel::class.java) ->
            NotificationsViewModel(container.catalogRepository) as T

        modelClass.isAssignableFrom(GuidelinesViewModel::class.java) ->
            GuidelinesViewModel(container.catalogRepository) as T

        modelClass.isAssignableFrom(CrewViewModel::class.java) ->
            CrewViewModel(container.catalogRepository) as T

        modelClass.isAssignableFrom(BlogsViewModel::class.java) ->
            BlogsViewModel(container.blogsRepository, container.authRepository) as T

        modelClass.isAssignableFrom(BlogComposerViewModel::class.java) ->
            BlogComposerViewModel(container.blogsRepository, container.uploadRepository) as T

        modelClass.isAssignableFrom(BlogDetailViewModel::class.java) ->
            BlogDetailViewModel(container.blogsRepository) as T

        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
