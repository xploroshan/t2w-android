import Foundation

/// Read surface for `/api/v1/rides/…`. Mirrors Android `RidesRepository`:
/// cursor-paginated list + detail.
struct RidesRepository {
    let client: APIClient

    /// One page of rides. Pass the previous page's `nextCursor` to page forward.
    func list(cursor: String? = nil, limit: Int = 20, status: String? = nil) async throws -> Page<RideCard> {
        try await client.send(.rides(cursor: cursor, limit: limit, status: status))
    }

    func detail(id: String) async throws -> RideCard {
        let response: RideDetailResponse = try await client.send(.ride(id: id))
        return response.ride
    }
}
