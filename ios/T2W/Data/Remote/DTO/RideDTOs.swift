import Foundation

/// List-view ride (`GET /rides` → `Page<RideCard>`). `status` is computed
/// server-side; `myRegistrationStatus` is nil unless the bearer viewer is
/// registered. Mirrors Android `RideCard`.
struct RideCard: Decodable, Identifiable {
    let id: String
    let title: String
    let rideNumber: String?
    let type: String?
    let status: String?
    let startDate: String?
    let endDate: String?
    let distanceKm: Double
    let difficulty: String?
    let fee: Double
    let posterUrl: String?
    let registeredRiders: Int
    let myRegistrationStatus: String?

    enum CodingKeys: String, CodingKey {
        case id, title, rideNumber, type, status, startDate, endDate
        case distanceKm, difficulty, fee, posterUrl, registeredRiders, myRegistrationStatus
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(String.self, forKey: .id)
        title = try c.decode(String.self, forKey: .title)
        rideNumber = try c.decodeIfPresent(String.self, forKey: .rideNumber)
        type = try c.decodeIfPresent(String.self, forKey: .type)
        status = try c.decodeIfPresent(String.self, forKey: .status)
        startDate = try c.decodeIfPresent(String.self, forKey: .startDate)
        endDate = try c.decodeIfPresent(String.self, forKey: .endDate)
        distanceKm = try c.decodeIfPresent(Double.self, forKey: .distanceKm) ?? 0
        difficulty = try c.decodeIfPresent(String.self, forKey: .difficulty)
        fee = try c.decodeIfPresent(Double.self, forKey: .fee) ?? 0
        posterUrl = try c.decodeIfPresent(String.self, forKey: .posterUrl)
        registeredRiders = try c.decodeIfPresent(Int.self, forKey: .registeredRiders) ?? 0
        myRegistrationStatus = try c.decodeIfPresent(String.self, forKey: .myRegistrationStatus)
    }
}

/// `/rides/{id}` wraps the ride in `{ "ride": ... }`. Mirrors Android
/// `RideDetailResponse`.
struct RideDetailResponse: Decodable {
    let ride: RideCard
}
