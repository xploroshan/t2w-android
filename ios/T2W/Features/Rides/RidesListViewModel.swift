import Foundation

/// Drives `RidesListView`. Mirrors the Android `RidesViewModel`: loads the first
/// page of `/rides`, exposes loading/error/empty states, and pages forward via
/// the `nextCursor` cursor envelope.
@MainActor
final class RidesListViewModel: ObservableObject {
    @Published private(set) var rides: [RideCard] = []
    @Published private(set) var isLoading: Bool = false
    @Published private(set) var error: String?

    private let repository: RidesRepository
    private var nextCursor: String?
    private var hasLoadedOnce = false

    var canLoadMore: Bool { nextCursor != nil }

    init(repository: RidesRepository) {
        self.repository = repository
    }

    /// First load (idempotent — safe to call from `.task`).
    func loadInitialIfNeeded() async {
        guard !hasLoadedOnce else { return }
        hasLoadedOnce = true
        await refresh()
    }

    func refresh() async {
        isLoading = true
        error = nil
        nextCursor = nil
        do {
            let page = try await repository.list()
            rides = page.items
            nextCursor = page.nextCursor
        } catch let apiError as APIError {
            error = apiError.userMessage
        } catch {
            error = "Couldn't load rides."
        }
        isLoading = false
    }

    func loadMore() async {
        guard let cursor = nextCursor, !isLoading else { return }
        isLoading = true
        do {
            let page = try await repository.list(cursor: cursor)
            rides.append(contentsOf: page.items)
            nextCursor = page.nextCursor
        } catch {
            // Keep the already-loaded rides; surface a transient error.
            self.error = (error as? APIError)?.userMessage ?? "Couldn't load more rides."
        }
        isLoading = false
    }
}
