# Tales on 2 Wheels — iOS (SwiftUI scaffold)

> **Status: non-built scaffold.** There is no Xcode/Swift toolchain in this
> repo, so **none of this has been compiled**. The code is idiomatic, internally
> consistent SwiftUI that a developer can drop into a fresh Xcode project (or a
> Swift package) and build on. It deliberately **mirrors the Android client's
> architecture** so the two stay conceptually in sync. Treat compile errors on
> first import as expected wiring (target membership, Info.plist keys), not bugs.

GitHub org policy scopes us to `xploroshan/t2w-android` and `xploroshan/t2w`, so
this iOS scaffold lives **inside the Android repo** under `ios/` rather than in a
separate repository.

## What's here

A minimal end-to-end slice: app entry + auth gate → networking against
`/api/v1` (bearer + refresh) → Codable DTOs → repositories → two SwiftUI screens
(Login, Rides list) with their view models.

```
ios/
  README.md
  T2W/
    App/
      T2WApp.swift           # @main entry, auth gate, MainTabsView, env injection
      AppContainer.swift     # hand-rolled DI container (mirrors Android di/AppContainer)
    Data/
      Remote/
        APIClient.swift      # async/await client: bearer inject + refresh-on-401 + retry
        APIError.swift       # normalized error surface (mirrors ApiError/ApiResult)
        Endpoint.swift       # typed endpoint catalogue + base URL config
        DTO/
          AuthDTOs.swift     # LoginRequest, RefreshRequest, AuthSuccess, RefreshSuccess, MeResponse
          UserDTO.swift      # sanitized user
          RideDTOs.swift     # RideCard, RideDetailResponse
          Page.swift         # generic cursor-paginated envelope
      Session/
        SessionStore.swift   # token persistence + auth state + single-flight refresh
      Repository/
        AuthRepository.swift # login / me / logout
        RidesRepository.swift# rides list + detail
    Features/
      Auth/
        LoginView.swift
        LoginViewModel.swift
      Rides/
        RidesListView.swift
        RidesListViewModel.swift
```

## Architecture overview (and the Android mapping)

The layering is intentionally the same as the Kotlin/Compose client:

| Concern | iOS (this scaffold) | Android client |
| --- | --- | --- |
| DI | `AppContainer` (plain object) | `di/AppContainer` (hand-rolled, no Hilt) |
| HTTP | `APIClient` (`URLSession` + `async/await`) | Retrofit + OkHttp |
| Bearer injection | `APIClient.makeRequest` on `requiresAuth` | `AuthInterceptor` |
| Refresh-on-401 + retry | `APIClient.sendRaw` + `SessionStore.refresh` | `TokenAuthenticator` |
| Non-recursive refresh | `performRefresh` (bare request, no retry) | `plainClient` / `refreshApi` |
| Session + auth gate | `SessionStore` (`@Published tokens`) + `RootView` | `SessionStore` (DataStore) + `T2WApp` |
| DTOs | `Codable` structs | `@Serializable` data classes |
| Tolerant decode | `decodeIfPresent` + ignored extra keys | `ignoreUnknownKeys`, field defaults |
| Pagination | `Page<Item>` (`items` + `nextCursor`) | `Page<T>` |
| Repositories | `AuthRepository`, `RidesRepository` | same names |
| Screens | SwiftUI `View` + `ObservableObject` VM | Composable + `ViewModel` |

### Auth & token refresh

- Login/register/refresh are **unauthenticated** (`Endpoint.requiresAuth ==
  false`); every other call carries `Authorization: Bearer <accessToken>`.
- On a `401` for an authenticated call, `APIClient` asks `SessionStore` to
  refresh **once** (single-flight: concurrent 401s await the same refresh `Task`,
  the analogue of the Android authenticator's `synchronized` lock), then retries
  the original request a single time.
- The refresh request is built **without** the bearer/retry path so a failing
  refresh can't recurse. A failed refresh **clears the session**, which flips the
  `RootView` gate back to `LoginView` — no manual navigation.

### Configuration

`APIConfig.baseURL` reads `T2W_API_BASE_URL` from the Info.plist (wire it from an
`.xcconfig` per build configuration), defaulting to `https://taleson2wheels.com`.
Paths carry the `api/v1/...` prefix, matching the OpenAPI servers and the Android
`BuildConfig.API_BASE_URL` convention.

> **Security TODO:** `SessionStore` currently sketches persistence with
> `UserDefaults`. Before shipping, move tokens to the **Keychain**
> (`kSecClassGenericPassword`). The spot is marked in the source.

## Creating the Xcode project

This folder is plain `.swift` sources — there's no `.xcodeproj`/`.xcworkspace`
checked in. Two ways to get it building:

### Option A — app target in a new Xcode project (recommended)

1. **Xcode → File → New → Project → iOS → App.**
   - Product name `T2W`, Interface **SwiftUI**, Language **Swift**, iOS 16+.
2. Delete the template's generated `ContentView.swift` and the `@main` `App`
   struct (this scaffold provides its own `@main` in `App/T2WApp.swift`).
3. **Drag the `ios/T2W/` folder into the project navigator** with *"Create
   groups"* and **add to the app target**. Keep the folder structure.
4. Add `T2W_API_BASE_URL` to the target's **Info.plist** (or an `.xcconfig`),
   e.g. `https://taleson2wheels.com` (or your dev host).
5. Build & run on the simulator. Fill in the placeholder Home/Riders/Profile
   tabs and the forgot-password / registration flows next.

### Option B — Swift package (for unit-testing the data layer headlessly)

`Data/` has no UIKit/SwiftUI dependencies, so you can lift `App` (minus
`@main`/views) + `Data/` into a `T2WCore` SwiftPM library target and unit-test
`APIClient`, `SessionStore`, and the DTO decoders with `URLProtocol` stubs.
`Features/` (SwiftUI views) stays in the app target.

## Not included (intentionally)

To keep the scaffold a coherent slice rather than a half-built app, these are
left as clearly-marked TODOs: registration / forgot-password flows, the
Home / Riders / Profile tab contents, ride detail, image loading, live GPS
tracking, and a real Keychain-backed token store. The Android client is the
reference for all of them.
