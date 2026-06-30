# Configuring secrets (a beginner's walkthrough)

This guide is for setting up the four pieces of external configuration the app
needs to ship a real release. **You do not need any of these to build and run
the app** — the project is designed to compile and run with all of them missing
(maps show blank, push is silent, crashes aren't reported, and the release APK
is debug‑signed). Add them when you're ready to go to production.

Each secret lives in a **git‑ignored** file at the repo root or in `app/`, so
nothing here ever gets committed. The files already listed in `.gitignore` are:

```
secrets.properties      # API URL, Maps key  (key=value text)
keystore.properties     # release signing    (key=value text)
*.jks  *.keystore        # the signing keystore itself
google-services.json     # Firebase / FCM     (downloaded JSON, goes in app/)
```

> **Golden rule:** never commit these files or paste their contents into chat,
> a PR, an issue, or a screenshot. If a key leaks, rotate it in its console.

There's a ready‑made template — copy it once and fill in the blanks:

```bash
cp secrets.properties.example secrets.properties
```

---

## 0. The one file most setups need: `secrets.properties`

Create a plain‑text file named `secrets.properties` in the **repo root** (the
folder that has `settings.gradle.kts`). It's a list of `key=value` lines. The
build reads it automatically; absent keys fall back to safe defaults.

```properties
# Where the app talks to your backend. Must end with a trailing slash.
# Debug builds (emulator) reach your laptop's localhost at 10.0.2.2:
T2W_API_BASE_URL_DEBUG=http://10.0.2.2:3000/
# Release builds point at production:
T2W_API_BASE_URL=https://taleson2wheels.com/

# Google Maps (see section 1). Leave blank until you have a key.
MAPS_API_KEY=
```

After editing any `secrets.properties` value, do a full rebuild so Gradle picks
it up:

```bash
./gradlew clean :app:assembleDebug
```

---

## 1. Google Maps API key (live‑ride map)

**What it's for:** the live‑ride screen renders rider positions on a Google map.
Without a key the map tiles render as a blank grey grid; everything else works.

### Get the key

1. Go to <https://console.cloud.google.com/> and sign in.
2. Top bar → **project dropdown** → **New Project** → name it `taleson2wheels`
   → **Create**. Make sure it's selected afterwards.
3. Left menu → **APIs & Services → Library**. Search **"Maps SDK for Android"**,
   open it, click **Enable**.
4. Left menu → **APIs & Services → Credentials** → **+ Create credentials** →
   **API key**. A key like `AIzaSy...` appears. Click **Copy**.
5. **Restrict the key** (strongly recommended) → on the key's edit page:
   - *Application restrictions* → **Android apps** → **Add an item**:
     - Package name: `com.taleson2wheels.app`
     - SHA‑1 fingerprint: get it with
       `./gradlew :app:signingReport` and copy the **SHA1** under
       `Variant: debug` (and again for your release keystore once you have one).
   - *API restrictions* → **Restrict key** → tick **Maps SDK for Android** →
     **Save**.

### Put it in the app

Add it to `secrets.properties`:

```properties
MAPS_API_KEY=AIzaSyYOUR_REAL_KEY_HERE
```

The build injects it into the manifest as the `com.google.android.geo.API_KEY`
meta‑data, so you never paste the key into source. Rebuild, open a live ride,
and the map should render.

> Billing: Google requires a billing account on the project, but Maps SDK for
> Android has a large free tier. Set a budget alert under **Billing → Budgets**.

---

## 2. Firebase Cloud Messaging — `google-services.json` (push notifications)

**What it's for:** server‑sent push notifications (ride reminders, approvals).
Without it, the app still registers a *null* token (a harmless no‑op) and simply
receives no pushes.

### Create the Firebase project + app

1. Go to <https://console.firebase.google.com/> → **Add project**. You can reuse
   the same Google Cloud project from section 1 (pick it from the list) or make
   a new one. Accept the defaults.
2. On the project overview, click the **Android** icon ("Add app").
3. **Android package name:** `com.taleson2wheels.app` (must match exactly).
4. Nickname / debug SHA‑1 are optional for FCM — you can skip them. Click
   **Register app**.
5. Click **Download google-services.json**.

### Put it in the app

Move the downloaded file to the **`app/`** folder (next to `build.gradle.kts`):

```
t2w-android/app/google-services.json
```

It's already git‑ignored. That's all the *config* you supply. The remaining
work is one‑time **code** wiring (a developer task, not a secret):

1. Add the Google Services Gradle plugin and `firebase-messaging` dependency.
2. Replace `NoOpPushTokenProvider` with an `FcmPushTokenProvider` that returns
   `FirebaseMessaging.getInstance().token.await()` in `AppContainer`.
3. Add a `FirebaseMessagingService` for `onNewToken` (re‑register the device)
   and to display incoming messages.

The backend dispatch half (sending pushes) is already implemented server‑side.

---

## 3. Sentry DSN (crash reporting)

**What it's for:** automatic crash/error reports. Without a DSN, reporting is
simply disabled — the app behaves identically.

### Get the DSN

1. Go to <https://sentry.io/> and sign up / sign in (free tier is fine).
2. **Projects → Create project** → platform **Android** → name it
   `t2w-android` → **Create**.
3. After creation, open **Settings → Projects → t2w-android → Client Keys
   (DSN)**. Copy the **DSN** — it looks like
   `https://abc123@o45678.ingest.sentry.io/123456`.

### Put it in the app

Add it to `secrets.properties`:

```properties
SENTRY_DSN=https://abc123@o45678.ingest.sentry.io/123456
```

Then the one‑time **code** wiring (developer task): add the
`io.sentry:sentry-android` dependency, expose the DSN as a `BuildConfig` field
via `secretOr("SENTRY_DSN", "")`, and in `T2WApplication.onCreate` initialize
Sentry **only when the DSN is non‑blank** so unconfigured/debug builds stay
silent.

---

## 4. Upload keystore (signing the release build)

**What it's for:** Google Play will only accept an APK/AAB signed with *your*
private release key. Until you configure one, release builds fall back to the
debug signature (fine for local testing, **rejected by Play**).

> Keep the keystore file and its passwords safe and backed up. If you lose them
> you cannot publish updates to the same app listing — ever.

### Generate the keystore (one time)

`keytool` ships with the JDK. From anywhere:

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

It will ask for:
- a **keystore password** (remember it),
- your name/org (any sensible values),
- a **key password** (you can press Enter to reuse the keystore password).

Move `upload-keystore.jks` somewhere safe **outside the repo** (so it can't be
committed), e.g. `~/keys/upload-keystore.jks`.

### Point the build at it

Create `keystore.properties` in the **repo root** (git‑ignored):

```properties
storeFile=/Users/you/keys/upload-keystore.jks
storePassword=your-keystore-password
keyAlias=upload
keyPassword=your-key-password
```

Now build a real signed release:

```bash
./gradlew :app:assembleRelease     # signed APK  → app/build/outputs/apk/release/
./gradlew :app:bundleRelease       # signed AAB  → app/build/outputs/bundle/release/  (upload this to Play)
```

If `keystore.properties` is missing, the release still assembles (debug‑signed)
so CI and fresh clones never break.

---

## Quick checklist

| Secret | File | Where to get it | App works without it? |
|---|---|---|---|
| API base URL | `secrets.properties` → `T2W_API_BASE_URL*` | your backend host | Yes (defaults to prod/emulator) |
| Maps key | `secrets.properties` → `MAPS_API_KEY` | Google Cloud Console | Yes (blank map) |
| FCM | `app/google-services.json` | Firebase Console | Yes (no push) |
| Sentry DSN | `secrets.properties` → `SENTRY_DSN` | sentry.io | Yes (no crash reports) |
| Release signing | `keystore.properties` + `*.jks` | `keytool` (you generate it) | Yes (debug‑signed) |

See `docs/HARDENING.md` for the security posture behind these, and
`docs/SIDELOAD.md` for distributing a build without the Play Store.
