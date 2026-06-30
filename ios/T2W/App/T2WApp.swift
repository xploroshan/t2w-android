import SwiftUI

/// App entry + auth gate — the iOS analogue of the Android `T2WApp` composable.
/// It observes the session: while not ready it shows a spinner; `tokens == nil`
/// shows Login; otherwise the signed-in shell. Clearing the session anywhere
/// (logout, refresh failure) returns the user to Login automatically.
@main
struct T2WApp: App {
    @StateObject private var container = AppContainer()

    var body: some Scene {
        WindowGroup {
            RootView(container: container)
                .environmentObject(container.session)
        }
    }
}

private struct RootView: View {
    let container: AppContainer
    @EnvironmentObject private var session: SessionStore

    var body: some View {
        Group {
            if !session.isReady {
                ProgressView()
            } else if session.tokens == nil {
                // Build the login VM from the *real* container so a successful
                // login persists to the shared session and flips this gate.
                LoginView(viewModel: LoginViewModel(repository: container.authRepository))
            } else {
                MainTabsView(container: container)
            }
        }
    }
}

/// Signed-in shell. Mirrors the Android `MainScreen` bottom-nav with the same
/// four destinations (Home / Rides / Riders / Profile). This scaffold ships the
/// Rides tab wired to the client; the others are placeholders to fill in.
struct MainTabsView: View {
    let container: AppContainer

    var body: some View {
        TabView {
            Text("Tales on 2 Wheels")
                .tabItem { Label("Home", systemImage: "house") }

            RidesListView(repository: container.ridesRepository)
                .tabItem { Label("Rides", systemImage: "bicycle") }

            Text("Leaderboard")
                .tabItem { Label("Riders", systemImage: "trophy") }

            Text("Profile")
                .tabItem { Label("Profile", systemImage: "person") }
        }
    }
}
