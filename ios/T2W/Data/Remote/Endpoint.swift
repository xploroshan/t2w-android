import Foundation

/// Base URL for the T2W `/api/v1` namespace.
///
/// Mirrors the Android client's `BuildConfig.API_BASE_URL` (host root; the
/// `api/v1/` prefix is carried in each path). Override per-build via the
/// `T2W_API_BASE_URL` value in an xcconfig / Info.plist rather than hard-coding.
enum APIConfig {
    /// Host root, e.g. `https://taleson2wheels.com`. Paths add `/api/v1/...`.
    static let baseURL: URL = {
        if let raw = Bundle.main.object(forInfoDictionaryKey: "T2W_API_BASE_URL") as? String,
           let url = URL(string: raw) {
            return url
        }
        return URL(string: "https://taleson2wheels.com")!
    }()
}

/// The HTTP verbs the client uses.
enum HTTPMethod: String {
    case get = "GET"
    case post = "POST"
}

/// A typed description of one `/api/v1` request.
///
/// `requiresAuth` decides whether `APIClient` attaches the bearer token and
/// retries on a 401 — the Swift analogue of the Android `AuthInterceptor` +
/// `TokenAuthenticator` split (login/register/refresh are unauthenticated).
struct Endpoint {
    let method: HTTPMethod
    /// Path under the host root, e.g. `api/v1/auth/login`.
    let path: String
    let queryItems: [URLQueryItem]
    let body: Encodable?
    let requiresAuth: Bool

    init(
        method: HTTPMethod,
        path: String,
        queryItems: [URLQueryItem] = [],
        body: Encodable? = nil,
        requiresAuth: Bool = true
    ) {
        self.method = method
        self.path = path
        self.queryItems = queryItems
        self.body = body
        self.requiresAuth = requiresAuth
    }
}

// MARK: - Endpoint catalogue (mirrors the Android Retrofit interfaces)

extension Endpoint {
    // Auth — unauthenticated
    static func login(_ body: LoginRequest) -> Endpoint {
        Endpoint(method: .post, path: "api/v1/auth/login", body: body, requiresAuth: false)
    }

    static func refresh(_ body: RefreshRequest) -> Endpoint {
        Endpoint(method: .post, path: "api/v1/auth/refresh", body: body, requiresAuth: false)
    }

    // Auth — bearer
    static var me: Endpoint {
        Endpoint(method: .get, path: "api/v1/auth/me")
    }

    // Rides — bearer, cursor-paginated
    static func rides(cursor: String? = nil, limit: Int = 20, status: String? = nil) -> Endpoint {
        var items = [URLQueryItem(name: "limit", value: String(limit))]
        if let cursor { items.append(URLQueryItem(name: "cursor", value: cursor)) }
        if let status { items.append(URLQueryItem(name: "status", value: status)) }
        return Endpoint(method: .get, path: "api/v1/rides", queryItems: items)
    }

    static func ride(id: String) -> Endpoint {
        Endpoint(method: .get, path: "api/v1/rides/\(id)")
    }
}
