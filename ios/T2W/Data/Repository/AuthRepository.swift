import Foundation

/// Auth flows over `/api/v1/auth/…`. Mirrors Android `AuthRepository`: it owns
/// login + the session side effects (persisting the token pair on success).
struct AuthRepository {
    let client: APIClient
    let store: SessionStore

    /// Logs in and persists the returned token pair. The signed-in user is
    /// returned for the caller to show immediately.
    @discardableResult
    func login(email: String, password: String) async throws -> UserDTO {
        let body = LoginRequest(email: email, password: password)
        let success: AuthSuccess = try await client.send(.login(body))
        await store.save(Tokens(accessToken: success.accessToken,
                                refreshToken: success.refreshToken))
        return success.user
    }

    /// Fetches the current user (bearer). Triggers refresh-on-401 transparently.
    func me() async throws -> UserDTO {
        let response: MeResponse = try await client.send(.me)
        return response.user
    }

    func logout() async {
        await store.clear()
    }
}
