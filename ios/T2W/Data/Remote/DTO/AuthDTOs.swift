import Foundation

// Shapes mirror the Android `AuthDtos.kt` / T2W `docs/openapi-v1.yaml`.

// MARK: - Requests

struct LoginRequest: Encodable {
    let email: String
    let password: String
    var deviceId: String? = nil
    var platform: String = "ios"
}

struct RefreshRequest: Encodable {
    let refreshToken: String
    var deviceId: String? = nil
    var platform: String = "ios"
}

// MARK: - Responses

/// `/auth/login` and `/auth/register` → token pair + the signed-in user.
struct AuthSuccess: Decodable {
    let accessToken: String
    let refreshToken: String
    /// ISO-8601 instant; the opaque refresh token rotates (~60 days).
    let refreshTokenExpiresAt: String
    let user: UserDTO
}

/// `/auth/refresh` → a fresh token pair (no user payload).
struct RefreshSuccess: Decodable {
    let accessToken: String
    let refreshToken: String
    let refreshTokenExpiresAt: String
}

/// `/auth/me` wraps the user in `{ "user": ... }`.
struct MeResponse: Decodable {
    let user: UserDTO
}
