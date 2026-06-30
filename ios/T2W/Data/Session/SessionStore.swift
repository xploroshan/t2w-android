import Foundation
import Combine

/// The access + refresh token pair (mirrors Android `data.session.Tokens`).
struct Tokens: Codable, Equatable {
    let accessToken: String
    let refreshToken: String
}

/// Persists the session and publishes auth state, mirroring the Android
/// `SessionStore` (DataStore) + the `T2WApp` auth gate. The root view observes
/// `tokens`: `nil` shows Login, non-`nil` shows the signed-in shell. Clearing
/// the session anywhere (logout, refresh failure) returns the user to Login.
///
/// Tokens belong in the **Keychain** in a real build; this scaffold sketches the
/// surface with `UserDefaults` and marks the spot. `@MainActor` keeps the
/// `@Published` mutations on the main thread for SwiftUI.
@MainActor
final class SessionStore: ObservableObject {
    @Published private(set) var tokens: Tokens?
    /// False until the persisted session has been loaded once (matches Android's
    /// `ready` flag used to show a splash/loading view before deciding a screen).
    @Published private(set) var isReady: Bool = false

    private let defaults: UserDefaults
    private let storageKey = "t2w.session.tokens"

    /// Serializes refreshes so concurrent 401s don't each fire a refresh — the
    /// analogue of `TokenAuthenticator`'s `synchronized(lock)`.
    private var inFlightRefresh: Task<Tokens, Error>?

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        load()
    }

    private func load() {
        if let data = defaults.data(forKey: storageKey),
           let saved = try? JSONDecoder().decode(Tokens.self, from: data) {
            tokens = saved
        }
        isReady = true
    }

    func save(_ tokens: Tokens) {
        self.tokens = tokens
        if let data = try? JSONEncoder().encode(tokens) {
            // TODO(prod): store in Keychain (kSecClassGenericPassword), not UserDefaults.
            defaults.set(data, forKey: storageKey)
        }
    }

    func clear() {
        tokens = nil
        defaults.removeObject(forKey: storageKey)
    }

    var accessToken: String? { tokens?.accessToken }
    var refreshToken: String? { tokens?.refreshToken }

    /// Single-flight refresh. If a refresh is already running, await it instead
    /// of starting another; on failure the session is cleared (→ back to login).
    /// `perform` does the actual `/auth/refresh` call (injected by `APIClient`
    /// to avoid a retain cycle and to keep `SessionStore` transport-agnostic).
    func refresh(using perform: @escaping (String) async throws -> Tokens) async throws -> Tokens {
        if let existing = inFlightRefresh {
            return try await existing.value
        }
        guard let refreshToken else {
            clear()
            throw APIError.unauthorized
        }
        let task = Task<Tokens, Error> {
            defer { inFlightRefresh = nil }
            do {
                let fresh = try await perform(refreshToken)
                save(fresh)
                return fresh
            } catch {
                clear()
                throw APIError.unauthorized
            }
        }
        inFlightRefresh = task
        return try await task.value
    }
}
