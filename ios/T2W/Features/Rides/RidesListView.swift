import SwiftUI

/// Rides list — mirrors the Android `RidesScreen`: a title, loading/error/empty
/// states, and a list of ride cards that pages forward on scroll.
struct RidesListView: View {
    @StateObject private var viewModel: RidesListViewModel

    init(repository: RidesRepository) {
        _viewModel = StateObject(wrappedValue: RidesListViewModel(repository: repository))
    }

    var body: some View {
        NavigationStack {
            content
                .navigationTitle("Rides")
                .task { await viewModel.loadInitialIfNeeded() }
                .refreshable { await viewModel.refresh() }
        }
    }

    @ViewBuilder
    private var content: some View {
        if viewModel.isLoading && viewModel.rides.isEmpty {
            ProgressView()
        } else if let error = viewModel.error, viewModel.rides.isEmpty {
            VStack(spacing: 12) {
                Text(error).multilineTextAlignment(.center)
                Button("Retry") { Task { await viewModel.refresh() } }
            }
            .padding()
        } else if viewModel.rides.isEmpty {
            Text("No rides yet. Pull to refresh.")
                .foregroundStyle(.secondary)
        } else {
            List {
                ForEach(viewModel.rides) { ride in
                    RideRow(ride: ride)
                }
                if viewModel.canLoadMore {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .task { await viewModel.loadMore() }
                }
            }
            .listStyle(.plain)
        }
    }
}

private struct RideRow: View {
    let ride: RideCard

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(ride.title).font(.headline)
            HStack(spacing: 8) {
                if let status = ride.status {
                    Text(status.capitalized).font(.caption).foregroundStyle(.tint)
                }
                Text("\(Int(ride.distanceKm)) km")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text("\(ride.registeredRiders) riders")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}
