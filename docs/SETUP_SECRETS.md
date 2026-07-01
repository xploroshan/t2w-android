# Configuring secrets (a beginner's walkthrough)

This guide is for setting up the pieces of external configuration the app needs
to ship a real release. **You do not need any of these to build and run the
app** — the project is designed to compile and run with all of them missing
(maps show blank, the 3D flyover shows a placeholder, push is silent, crashes
aren't reported, and the release APK is debug‑signed). Add them when you're
ready to go to production.

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

## 5. Mapbox tokens (Relive 3D flyover)

**What it's for:** the **Relive** screen replays a finished ride as an animated
3D‑terrain flyover, which needs Mapbox (Google Maps can't render true 3D
terrain). Without it the rest of the app is unaffected — the Relive screen just
shows a "set your Mapbox token" hint.

Mapbox is unusual: it needs **two different tokens**, and they are NOT
interchangeable. Read this bit slowly the first time.

| # | Token | Starts with | Secret? | Used | Goes in |
|---|---|---|---|---|---|
| A | **Public / runtime** | `pk.` | No (it's embedded in the app) | at run time, to load the map | `secrets.properties` → `MAPBOX_ACCESS_TOKEN` |
| B | **Secret / downloads** | `sk.` | **Yes — never commit** | at build time, to download the Mapbox SDK | `~/.gradle/gradle.properties` → `MAPBOX_DOWNLOADS_TOKEN` |

> **Can I reuse the website's Mapbox token?** Only token **A** (the `pk.`), and
> only if the website's token has **no URL restrictions** — a web token locked to
> your website's domain will silently fail in the app (native requests carry no
> browser origin for Mapbox to match). Recommended: make a **separate app token**
> so the web token can stay URL‑restricted. Token **B** is Android‑only; the
> website has no equivalent, so you must create it fresh regardless.

### Step 1 — create a Mapbox account

1. Go to <https://account.mapbox.com/auth/signup/> and sign up (the free tier is
   generous — 25k+ map loads/month). Verify your email.
2. You'll land on the **Account** page at <https://account.mapbox.com/>.

### Step 2 — get token A (public `pk.`)

1. On the Account page scroll to **Access tokens**. There's a **Default public
   token** that already starts with `pk.` — you can use it, but better:
2. Click **Create a token**. Name it `t2w-android-app`.
3. Under **Scopes**, leave the default *public* scopes ticked (Styles:Read,
   Styles:Tiles, Fonts:Read, Datasets:Read, Vision:Read) — these are all a map
   needs. Do **not** tick any secret scopes here.
4. Leave **URL restrictions empty** (native apps can't use them). Click
   **Create token** and **Copy** the `pk....` value.

### Step 3 — get token B (secret `sk.` with Downloads:Read)

1. Back on **Access tokens** → **Create a token**. Name it
   `t2w-android-downloads`.
2. Under **Scopes**, scroll to the **Secret scopes** section and tick
   **`Downloads:Read`**. (Ticking any secret scope makes the whole token secret —
   it will start with `sk.`.)
3. Click **Create token**. Mapbox shows the `sk....` value **once** — copy it
   now and keep it somewhere safe. If you lose it, delete it and make a new one.

### Step 4 — put token A in the app

Add it to `secrets.properties` (repo root, git‑ignored):

```properties
MAPBOX_ACCESS_TOKEN=pk.eyJ1Ijoi...your-public-token
```

The build exposes it as `BuildConfig.MAPBOX_ACCESS_TOKEN`; the Relive screen sets
it on `MapboxOptions.accessToken` at runtime — you never paste it into source.

### Step 5 — put token B in your global Gradle config

Token B authenticates the download of the Mapbox SDK, so it must be readable by
Gradle **for every build on your machine** — it lives **outside the repo**, in
your user‑level Gradle properties (create the file if it doesn't exist):

- **macOS / Linux:** `~/.gradle/gradle.properties`
- **Windows:** `C:\Users\<you>\.gradle\gradle.properties`

Add one line:

```properties
MAPBOX_DOWNLOADS_TOKEN=sk.eyJ1Ijoi...your-secret-downloads-token
```

> Why here and not in the repo? Because it's a **secret** and it's needed before
> the project's own files are read (to fetch the SDK). Putting it in your global
> Gradle config keeps it off every commit automatically. (An env var
> `MAPBOX_DOWNLOADS_TOKEN=...` also works, e.g. in CI — see below.)

### Step 6 — build the real terrain map with `-Pt2wMapbox=true`

**The Mapbox SDK is opt-in.** By default the app builds **token-free**: the
Relive screen compiles a lightweight placeholder map (`src/noMapbox`) and needs
neither token. That's why a fresh clone builds with no Mapbox setup at all.

To compile the real Mapbox-backed terrain flyover, pass the flag (token B must be
configured per Step 5):

```bash
./gradlew clean :app:assembleDebug -Pt2wMapbox=true
```

If token B is wrong/missing on a `-Pt2wMapbox=true` build you'll see a
`401 Unauthorized` from `api.mapbox.com` when Gradle tries to fetch
`com.mapbox.maps:android` — fix `MAPBOX_DOWNLOADS_TOKEN`. If token A (the public
`pk.`) is missing the build still succeeds; the Relive map just shows its "set a
token" placeholder at runtime. Release builds should pass `-Pt2wMapbox=true` to
ship the real map (a release without it warns loudly).

### CI

The default PR gate (`android-ci.yml`: unit tests + Paparazzi + `assembleDebug`)
and the emulator suite both run **token-free** (`-Pt2wMapbox=false`), so they need
no Mapbox secret — CI proves a fresh clone builds. A dedicated guarded step then
compiles the **real** Mapbox variant (`-Pt2wMapbox=true`) **only when** the
`MAPBOX_DOWNLOADS_TOKEN` repo secret is present, and skips with a notice otherwise:

```yaml
- name: Compile Mapbox Relive variant (if token available)
  env:
    MAPBOX_DOWNLOADS_TOKEN: ${{ secrets.MAPBOX_DOWNLOADS_TOKEN }}
  run: |
    if [ -n "$MAPBOX_DOWNLOADS_TOKEN" ]; then
      ./gradlew :app:compileDebugKotlin -Pt2wMapbox=true --stacktrace
    else
      echo "::notice::MAPBOX_DOWNLOADS_TOKEN not set — skipping the real Mapbox compile check"
    fi
```

So: add **`MAPBOX_DOWNLOADS_TOKEN`** (the `sk.` with `Downloads:Read`) as a
**repository secret** (Settings → Secrets and variables → Actions) to have CI
validate the real terrain map; without it CI still passes on the token-free build.

> **Golden rule (again):** the `sk.` downloads token is a real secret — never
> commit it, never paste it in chat/PRs. If it leaks, delete it at
> <https://account.mapbox.com/access-tokens/> and make a new one.

---

## Quick checklist

| Secret | File | Where to get it | App works without it? |
|---|---|---|---|
| API base URL | `secrets.properties` → `T2W_API_BASE_URL*` | your backend host | Yes (defaults to prod/emulator) |
| Maps key | `secrets.properties` → `MAPS_API_KEY` | Google Cloud Console | Yes (blank map) |
| Mapbox runtime (`pk.`) | `secrets.properties` → `MAPBOX_ACCESS_TOKEN` | account.mapbox.com → Tokens | Yes (Relive placeholder) |
| Mapbox downloads (`sk.`) | `~/.gradle/gradle.properties` (local) · repo secret (CI) → `MAPBOX_DOWNLOADS_TOKEN` | account.mapbox.com → Tokens (Downloads:Read) | Yes by default (token-free build) — only needed with `-Pt2wMapbox=true` for the real terrain map |
| FCM | `app/google-services.json` | Firebase Console | Yes (no push) |
| Sentry DSN | `secrets.properties` → `SENTRY_DSN` | sentry.io | Yes (no crash reports) |
| Release signing | `keystore.properties` + `*.jks` | `keytool` (you generate it) | Yes (debug‑signed) |

See `docs/HARDENING.md` for the security posture behind these, and
`docs/SIDELOAD.md` for distributing a build without the Play Store.
