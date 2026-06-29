# t2w-android

Native **Kotlin** Android app for **Tales on 2 Wheels**, wired to the backend's
versioned mobile API namespace **`/api/v1`**.

The API contract is owned by the backend repo at
[`T2W/docs/openapi-v1.yaml`](../T2W/docs/openapi-v1.yaml) (OpenAPI 3.1). The
Retrofit DTOs and service interfaces here are kept in sync with that document —
it is the source of truth. See `T2W/docs/mobile-apps-plan.md` for the wider
mobile strategy.

> Status: **scaffold**. Auth (login) + the rides list/detail flow are wired
> end-to-end through `/api/v1`; the remaining endpoints in the spec have typed
> service methods ready to consume. See [Roadmap](#roadmap).

---

## Tech stack

| Concern | Choice |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Navigation | `androidx.navigation:navigation-compose` |
| Architecture | MVVM (`ViewModel` + `StateFlow`/Compose state) |
| Networking | Retrofit 2.11 + OkHttp 4.12 + `kotlinx.serialization` |
| Async | Kotlin Coroutines |
| Auth storage | DataStore Preferences (token pair) |
| Images | Coil |
| DI | Hand-rolled `AppContainer` (no Hilt/KSP — keeps the build lean) |
| Build | Gradle 8.11 wrapper, AGP 8.7, version catalog (`gradle/libs.versions.toml`) |
| Min / target SDK | 26 / 35 |

---

## How it's wired to `/api/v1`

```
ui (Compose screens)
  └─ ViewModel (StateFlow)
       └─ Repository  ── returns ApiResult<T> (typed success/failure)
            └─ Retrofit service (AuthApi / RidesApi / …)  ──HTTPS──▶  /api/v1/*
                 ├─ AuthInterceptor        adds  Authorization: Bearer <accessToken>
                 └─ TokenAuthenticator     on 401 → POST /api/v1/auth/refresh, retry once
            SessionStore (DataStore) holds the access + refresh token pair
```

Key pieces:

- **Base URL** comes from `BuildConfig.API_BASE_URL`.
  - `debug` → `http://10.0.2.2:3000/api/v1/` (local Next.js as seen from the
    Android emulator).
  - `release` → `https://taleson2wheels.com/api/v1/`.
  - Override either without editing source by creating a git-ignored
    `secrets.properties` at the repo root:
    ```properties
    T2W_API_BASE_URL_DEBUG=http://192.168.1.20:3000/api/v1/
    T2W_API_BASE_URL=https://staging.taleson2wheels.com/api/v1/
    ```
- **Auth model** (mirrors `mobile-apps-plan.md` §6): a short-lived JWT **access
  token** sent as `Authorization: Bearer …`, plus a rotating **refresh token**.
  `TokenAuthenticator` transparently refreshes on a `401` and retries the request
  once; a failed refresh clears the session and the UI falls back to login.
  The refresh call runs on a **separate, un-intercepted** OkHttp client so it
  can never recurse.
- **Error envelope**: every non-2xx body is decoded as
  `{ "error": { "code", "message", "details" } }` into the typed
  `ApiError.Http(status, code, serverMessage)`; network failures become
  `ApiError.Network`. Repositories return `ApiResult<T>` so the UI never touches
  `try/catch`.
- **Cursor pagination**: list endpoints return `Page<T>(items, nextCursor)`;
  `RidesViewModel` appends pages until `nextCursor == null`.

### Spec → code map

| `openapi-v1.yaml` | Kotlin |
|---|---|
| `components.schemas.*` | `data/remote/dto/*.kt` |
| `ErrorResponse` | `data/remote/ApiError.kt` |
| Cursor page envelope | `data/remote/dto/Page.kt` |
| `/auth/...` | `data/remote/api/AuthApi.kt` |
| `/rides/...`, `/rides/{id}/live/...` | `data/remote/api/RidesApi.kt` |
| `/riders/...` | `data/remote/api/RidersApi.kt` |
| `/motorcycles/...` | `data/remote/api/GarageApi.kt` |
| `/health` `/stats` `/guidelines` `/badges` `/blogs` `/ride-posts` `/notifications` `/devices` | `data/remote/api/ContentApi.kt` |

If you prefer code generation, the same spec can drive
`openapi-generator-cli generate -g kotlin -i ../T2W/docs/openapi-v1.yaml`; the
hand-written client here is intentionally small and dependency-light.

---

## Project layout

```
app/src/main/kotlin/com/taleson2wheels/app/
├── T2WApplication.kt        # owns AppContainer, hydrates the session
├── MainActivity.kt          # edge-to-edge Compose host
├── di/AppContainer.kt       # manual DI: Json, OkHttp, Retrofit, services, repos
├── data/
│   ├── remote/
│   │   ├── ApiError.kt ApiResult.kt
│   │   ├── AuthInterceptor.kt TokenAuthenticator.kt
│   │   ├── api/             # Retrofit service interfaces
│   │   └── dto/             # @Serializable DTOs matching the spec
│   ├── session/SessionStore.kt   # DataStore-backed token store
│   └── repository/          # AuthRepository, RidesRepository
└── ui/
    ├── T2WApp.kt            # auth gate + NavHost
    ├── AppViewModelFactory.kt
    ├── theme/               # Material 3 theme (T2W brand)
    ├── auth/                # LoginScreen + LoginViewModel
    ├── rides/               # Rides list + detail screens & view models
    └── common/              # Loading / error views
```

---

## Build & run

Requires the Android SDK (platform 35, build-tools 35) and JDK 17+.

```bash
# Unit tests (pure JVM — contract parsing)
./gradlew testDebugUnitTest

# Debug APK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Minified release APK (validates R8 + ProGuard rules)
./gradlew assembleRelease
```

To run against a local backend, start the Next.js app (`npm run dev` in the
`T2W` repo) and launch the app on an emulator — `10.0.2.2` maps to the host's
`localhost`. Cleartext is permitted only for `10.0.2.2`/`localhost` via
`res/xml/network_security_config.xml`; all other traffic is HTTPS-only.

---

## Roadmap

Tracks the phases in `T2W/docs/mobile-apps-plan.md`:

- **Done (scaffold):** project + build setup, networking layer wired to
  `/api/v1`, bearer/refresh auth, login flow, rides list + detail, unit tests.
- **Phase 1 (next):** register/OTP/reset screens, home tabs, ride registration
  form, profile/garage, leaderboard, badges, guidelines, push registration
  (`/devices`), and **background GPS** live tracking via a foreground service
  posting batches to `/rides/{id}/live/location`.
- **Phase 2:** blogs, ride posts, notifications center, deep links.
- **Phase 3:** admin parity.

### Hardening notes

- Refresh tokens are stored in the app's private DataStore and excluded from
  backups (`res/xml/backup_rules.xml`). Moving them behind the Android Keystore
  (`EncryptedSharedPreferences` / `androidx.security`) is a Phase 1 follow-up.
- DI is intentionally manual; adopt Hilt if the graph grows.
