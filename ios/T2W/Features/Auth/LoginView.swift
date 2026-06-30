import SwiftUI

/// Login screen — mirrors the Android `LoginScreen` copy and behaviour:
/// title + subtitle, email + secure password fields, a submit button gated on
/// `canSubmit`, and an inline error. Strings match `res/values/strings.xml`.
///
/// The view model is injected (built from the app's `AppContainer` by the
/// caller) so login persists to the *real* `SessionStore` and flips the auth
/// gate. Previews/tests pass a stub VM.
struct LoginView: View {
    @StateObject private var viewModel: LoginViewModel

    init(viewModel: LoginViewModel) {
        _viewModel = StateObject(wrappedValue: viewModel)
    }

    var body: some View {
        VStack(spacing: 16) {
            Text("Tales on 2 Wheels")
                .font(.title)
                .foregroundStyle(.tint)

            Text("Sign in to ride with the crew")
                .font(.body)
                .padding(.bottom, 16)

            TextField("Email", text: $viewModel.email)
                .textContentType(.emailAddress)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .textFieldStyle(.roundedBorder)
                .disabled(viewModel.isSubmitting)

            SecureField("Password", text: $viewModel.password)
                .textContentType(.password)
                .textFieldStyle(.roundedBorder)
                .disabled(viewModel.isSubmitting)

            if let error = viewModel.error {
                Text(error)
                    .font(.body)
                    .foregroundStyle(.red)
                    .multilineTextAlignment(.center)
            }

            Button {
                Task { await viewModel.submit() }
            } label: {
                if viewModel.isSubmitting {
                    ProgressView()
                } else {
                    Text("Sign in").frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .disabled(!viewModel.canSubmit)

            Button("Forgot password?") { /* TODO: forgot-password flow */ }
                .font(.body)
                .padding(.top, 8)
            Button("New rider? Create an account") { /* TODO: registration flow */ }
                .font(.body)
        }
        .padding(.horizontal, 24)
        .frame(maxHeight: .infinity)
    }
}

#if DEBUG
#Preview {
    let session = SessionStore()
    return LoginView(viewModel: LoginViewModel(
        repository: AuthRepository(
            client: APIClient(store: session),
            store: session
        )
    ))
}
#endif
