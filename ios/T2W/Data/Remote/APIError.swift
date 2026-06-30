import Foundation

/// Normalized client-side error surface, mirroring the Android `ApiError` /
/// `ApiResult` split. UI layers switch on these instead of raw `URLError`s.
enum APIError: Error, Equatable {
    /// Couldn't build the request URL.
    case invalidRequest
    /// Transport failure (no connectivity, timeout, TLS, …).
    case transport(String)
    /// 401 that survived a refresh attempt — the session is gone; show login.
    case unauthorized
    /// Any non-2xx HTTP status with the decoded server `{ error, message }`.
    case http(status: Int, message: String?)
    /// 2xx body that didn't match the expected `Codable` shape.
    case decoding(String)

    var userMessage: String {
        switch self {
        case .invalidRequest:
            return "Something went wrong building the request."
        case .transport(let m):
            return "Network error: \(m)"
        case .unauthorized:
            return "Your session expired. Please sign in again."
        case .http(_, let message):
            return message ?? "The server returned an error."
        case .decoding:
            return "Unexpected response from the server."
        }
    }
}

/// Shape of the backend's error envelope (`{ "error": "...", "message": "..." }`).
struct APIErrorBody: Decodable {
    let error: String?
    let message: String?
}
