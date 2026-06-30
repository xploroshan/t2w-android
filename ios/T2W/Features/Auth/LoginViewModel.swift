import Foundation

/// Drives `LoginView`. Mirrors the Android `LoginViewModel`: holds the form
/// fields, a `canSubmit` gate (both fields non-blank, not already submitting),
/// and runs the login call, surfacing an inline error on failure. On success it
/// persists the session via `AuthRepository`, which flips the app's auth gate.
@MainActor
final class LoginViewModel: ObservableObject {
    @Published var email: String = ""
    @Published var password: String = ""
    @Published private(set) var isSubmitting: Bool = false
    @Published private(set) var error: String?

    private let repository: AuthRepository

    init(repository: AuthRepository) {
        self.repository = repository
    }

    var canSubmit: Bool {
        !isSubmitting
            && !email.trimmingCharacters(in: .whitespaces).isEmpty
            && !password.isEmpty
    }

    func submit() async {
        guard canSubmit else { return }
        isSubmitting = true
        error = nil
        do {
            _ = try await repository.login(
                email: email.trimmingCharacters(in: .whitespaces),
                password: password
            )
            // Session now holds tokens; RootView swaps to the signed-in shell.
        } catch let apiError as APIError {
            error = apiError.userMessage
        } catch {
            error = "Something went wrong. Please try again."
        }
        isSubmitting = false
    }
}
