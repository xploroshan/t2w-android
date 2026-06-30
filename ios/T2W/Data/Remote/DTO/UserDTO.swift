import Foundation

/// Sanitized user (no password hash). Mirrors Android `UserDto`. Optionals use
/// Swift's native `?` in place of Kotlin nullables; unmodeled server fields are
/// simply ignored by `Codable` (the analogue of `ignoreUnknownKeys = true`).
struct UserDTO: Decodable, Identifiable {
    let id: String
    let name: String
    let email: String
    let phone: String?
    let avatar: String?
    let role: String
    let isApproved: Bool
    let city: String?
    let totalKm: Double
    let ridesCompleted: Int

    enum CodingKeys: String, CodingKey {
        case id, name, email, phone, avatar, role, isApproved, city, totalKm, ridesCompleted
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(String.self, forKey: .id)
        name = try c.decode(String.self, forKey: .name)
        email = try c.decode(String.self, forKey: .email)
        phone = try c.decodeIfPresent(String.self, forKey: .phone)
        avatar = try c.decodeIfPresent(String.self, forKey: .avatar)
        role = try c.decode(String.self, forKey: .role)
        // Defaulted fields tolerate omission, matching the Kotlin defaults.
        isApproved = try c.decodeIfPresent(Bool.self, forKey: .isApproved) ?? false
        city = try c.decodeIfPresent(String.self, forKey: .city)
        totalKm = try c.decodeIfPresent(Double.self, forKey: .totalKm) ?? 0
        ridesCompleted = try c.decodeIfPresent(Int.self, forKey: .ridesCompleted) ?? 0
    }
}
