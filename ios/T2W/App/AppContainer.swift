import Foundation

/// Hand-rolled dependency container — the iOS analogue of the Android
/// `di/AppContainer`. One instance lives at the app root and owns every
/// long-lived singleton: the session store, the `/api/v1` client, and the
/// repositories. No third-party DI framework, matching the Android scaffold's
/// deliberately annotation-processor-free approach.
@MainActor
final class AppContainer {
    let session: SessionStore
    let client: APIClient
    let authRepository: AuthRepository
    let ridesRepository: RidesRepository

    init() {
        let session = SessionStore()
        let client = APIClient(store: session)
        self.session = session
        self.client = client
        self.authRepository = AuthRepository(client: client, store: session)
        self.ridesRepository = RidesRepository(client: client)
    }
}
