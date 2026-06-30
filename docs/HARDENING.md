# Hardening

What's in place in the app, and the remaining steps that need external config
(secrets / Firebase / Sentry) to finish.

## Done

### Token storage at rest
Session tokens are encrypted with an **AES-256-GCM** key held in the
hardware-backed **Android Keystore** (`TokenCipher`), then stored as Base64 in
the private DataStore. The on-disk store never holds a usable bearer/refresh
token. A blob that fails to decrypt (Keystore key invalidated, corruption) is
discarded and treated as "no session", so the user simply re-authenticates.
Tokens remain excluded from backups (`res/xml/backup_rules.xml`).

### Release signing
`app/build.gradle.kts` reads a **git-ignored** `keystore.properties` at the repo
root and configures a `release` signing config from it. When the file is absent
(CI without secrets, fresh clone) the release build falls back to debug signing
so it still assembles.

Create `keystore.properties` (never commit it):

```properties
storeFile=/absolute/path/to/upload-keystore.jks
storePassword=••••
keyAlias=upload
keyPassword=••••
```

`*.jks`, `*.keystore`, `keystore.properties`, and `google-services.json` are
git-ignored.

### Push device registration plumbing
`DevicesApi` / `DevicesRepository` wire `POST /api/v1/devices` and
`DELETE /api/v1/devices/{token}`. `AuthRepository` best-effort **registers** the
device on login/register and **deregisters** on logout. The push token comes
from a `PushTokenProvider` — currently `NoOpPushTokenProvider` (returns null), so
registration is a latent no-op until Firebase is wired below.

## Remaining (need external config)

### Firebase Cloud Messaging (push)
1. Add the `com.google.gms.google-services` Gradle plugin and
   `com.google.firebase:firebase-messaging` to the app.
2. Drop the project's `google-services.json` into `app/` (git-ignored).
3. Implement an `FcmPushTokenProvider : PushTokenProvider` returning
   `FirebaseMessaging.getInstance().token.await()` and swap it in for
   `NoOpPushTokenProvider` in `AppContainer`.
4. Add a `FirebaseMessagingService` to receive `onNewToken` (re-register) and
   display notifications. The backend dispatch side is handled server-side
   (`src/lib/push/fcm.ts` in the API repo).

### Sentry (crash reporting)
Add `io.sentry:sentry-android`, initialize it in `T2WApplication` with a DSN from
a `BuildConfig` field (empty default → disabled), and gate init on a non-blank
DSN so debug/unconfigured builds stay silent. (The API backend already has
Sentry configured.)

### Google Maps key (live-ride map)
The live-ride screen renders a Google Map (rider markers + the lead path) once a
**Maps SDK for Android** key is supplied; without one the map area stays blank
and the rest of the screen is unaffected.

1. In Google Cloud Console, enable **Maps SDK for Android** and create an API key
   (restrict it to the app's package name + signing SHA-1).
2. Add it to the git-ignored `secrets.properties` at the repo root:
   ```properties
   MAPS_API_KEY=AIza…
   ```
   (or pass `-PMAPS_API_KEY=AIza…`). It's injected into the manifest via the
   `MAPS_API_KEY` placeholder; empty by default so the build works without it.
