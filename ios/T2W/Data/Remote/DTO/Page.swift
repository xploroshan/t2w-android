import Foundation

/// Generic cursor-paginated envelope used by every list endpoint:
/// `{ "items": [...], "nextCursor": "…" | null }`. `nextCursor == nil` means the
/// last page has been reached. Mirrors Android `Page<T>`.
struct Page<Item: Decodable>: Decodable {
    let items: [Item]
    let nextCursor: String?

    enum CodingKeys: String, CodingKey {
        case items, nextCursor
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        items = try c.decodeIfPresent([Item].self, forKey: .items) ?? []
        nextCursor = try c.decodeIfPresent(String.self, forKey: .nextCursor)
    }
}
