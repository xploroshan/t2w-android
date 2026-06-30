import Foundation

/// `async/await` HTTP client for the T2W `/api/v1` surface.
///
/// Mirrors the Android networking stack:
///   - **Bearer injection** for `requiresAuth` endpoints  → Android `AuthInterceptor`
///   - **Refresh-on-401 + single retry**                  → Android `TokenAuthenticator`
///   - **Typed decode + normalized errors**               → Android Retrofit + `ApiResult`
///
/// The refresh call deliberately goes out **without** the bearer/refresh retry
/// path (it builds its own request) so a failing refresh can't recurse — the
/// same invariant the Android `plainClient` enforces.
actor APIClient {
    private let baseURL: URL
    private let session: URLSession
    private let store: SessionStore

    private let decoder: JSONDecoder = {
        let d = JSONDecoder()
        // DTOs use explicit CodingKeys, so no global key strategy is needed.
        return d
    }()
    private let encoder: JSONEncoder = {
        let e = JSONEncoder()
        return e
    }()

    init(
        baseURL: URL = APIConfig.baseURL,
        session: URLSession = .shared,
        store: SessionStore
    ) {
        self.baseURL = baseURL
        self.session = session
        self.store = store
    }

    // MARK: - Public API

    /// Sends `endpoint` and decodes the 2xx body as `Response`.
    func send<Response: Decodable>(_ endpoint: Endpoint, as: Response.Type = Response.self) async throws -> Response {
        let data = try await sendRaw(endpoint, isRetry: false)
        do {
            return try decoder.decode(Response.self, from: data)
        } catch {
            throw APIError.decoding(String(describing: error))
        }
    }

    /// Sends an endpoint whose 2xx body is ignored (e.g. logout).
    @discardableResult
    func send(_ endpoint: Endpoint) async throws -> Data {
        try await sendRaw(endpoint, isRetry: false)
    }

    // MARK: - Core request pipeline

    private func sendRaw(_ endpoint: Endpoint, isRetry: Bool) async throws -> Data {
        let accessToken = endpoint.requiresAuth ? await store.accessToken : nil
        let request = try makeRequest(endpoint, accessToken: accessToken)

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            throw APIError.transport(error.localizedDescription)
        }

        guard let http = response as? HTTPURLResponse else {
            throw APIError.transport("Non-HTTP response")
        }

        // 401 on an authenticated call → refresh once, then retry once.
        if http.statusCode == 401, endpoint.requiresAuth, !isRetry, accessToken != nil {
            _ = try await store.refresh(using: { [weak self] refreshToken in
                guard let self else { throw APIError.unauthorized }
                return try await self.performRefresh(refreshToken)
            })
            return try await sendRaw(endpoint, isRetry: true)
        }

        guard (200..<300).contains(http.statusCode) else {
            if http.statusCode == 401 { throw APIError.unauthorized }
            let body = try? decoder.decode(APIErrorBody.self, from: data)
            throw APIError.http(status: http.statusCode, message: body?.message ?? body?.error)
        }

        return data
    }

    /// Token refresh on a bare request — no bearer, no retry — so it can't recurse.
    /// Direct analogue of the Android `refreshApi` built on `plainClient`.
    private func performRefresh(_ refreshToken: String) async throws -> Tokens {
        let endpoint = Endpoint.refresh(RefreshRequest(refreshToken: refreshToken))
        let request = try makeRequest(endpoint, accessToken: nil)
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw APIError.unauthorized
        }
        let fresh = try decoder.decode(RefreshSuccess.self, from: data)
        return Tokens(accessToken: fresh.accessToken, refreshToken: fresh.refreshToken)
    }

    // MARK: - Request building

    private func makeRequest(_ endpoint: Endpoint, accessToken: String?) throws -> URLRequest {
        guard var components = URLComponents(
            url: baseURL.appendingPathComponent(endpoint.path),
            resolvingAgainstBaseURL: false
        ) else {
            throw APIError.invalidRequest
        }
        if !endpoint.queryItems.isEmpty {
            components.queryItems = endpoint.queryItems
        }
        guard let url = components.url else { throw APIError.invalidRequest }

        var request = URLRequest(url: url)
        request.httpMethod = endpoint.method.rawValue
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        if let accessToken {
            request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        }
        if let body = endpoint.body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try encoder.encode(AnyEncodable(body))
        }
        return request
    }
}

/// Type-erasing wrapper so `Endpoint.body` can be any `Encodable`.
private struct AnyEncodable: Encodable {
    private let encodeFn: (Encoder) throws -> Void
    init(_ wrapped: Encodable) { encodeFn = wrapped.encode }
    func encode(to encoder: Encoder) throws { try encodeFn(encoder) }
}
