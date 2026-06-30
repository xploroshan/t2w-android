# Sideloading & offline GPS field testing

How to build, install, and field-test the **Tales on 2 Wheels** Android client
(`com.taleson2wheels.app`) on a real device without going through the Play
Store. Covers debug + release APKs, `adb` install / sideload, and the
offline-GPS live-tracking field test.

---

## 1. Prerequisites

- **Android SDK platform-tools** (`adb`) on your `PATH`. With Android Studio:
  `~/Android/Sdk/platform-tools`.
- A device with **USB debugging** enabled (Settings → Developer options → USB
  debugging), or an emulator (`emulator -avd <name>`).
- JDK 17 (the project targets `JavaVersion.VERSION_17`).

Confirm the device is visible:

```bash
adb devices
# List of devices attached
# 1A2B3C4D5E    device
```

---

## 2. Build a debug APK

The debug build is unsigned-for-distribution (signed with the auto-generated
debug keystore) and points at the debug `API_BASE_URL`
(`http://10.0.2.2:3000/` by default — the host's `localhost:3000` as seen from
an emulator).

```bash
./gradlew :app:assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

Override the API host per-machine without editing `build.gradle.kts` by adding
a git-ignored `secrets.properties` at the repo root:

```properties
# secrets.properties (git-ignored)
T2W_API_BASE_URL_DEBUG=http://192.168.1.50:3000/
```

For a **physical device** on the same Wi-Fi, point it at your dev machine's LAN
IP (not `10.0.2.2`, which only resolves inside the emulator), e.g.
`http://192.168.1.50:3000/`.

---

## 3. Build a release APK

```bash
./gradlew :app:assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk  (or -unsigned.apk)
```

The release build type enables R8 (`isMinifyEnabled = true`) and resource
shrinking, and uses the production `API_BASE_URL`
(`https://taleson2wheels.com/`, overridable via `T2W_API_BASE_URL`).

### Release signing

> **Status:** the current `app/build.gradle.kts` does **not** declare a
> `signingConfigs` block, so `assembleRelease` produces an
> **unsigned** APK. You must sign it before a device will install it.

Two options:

**A. Sign manually (quickest for a one-off field build):**

```bash
# 1. Create a keystore once (keep keystore + passwords OUT of git):
keytool -genkeypair -v -keystore release.keystore \
  -alias t2w -keyalg RSA -keysize 2048 -validity 10000

# 2. Zipalign, then sign with apksigner (from build-tools):
zipalign -v -p 4 app-release-unsigned.apk app-release-aligned.apk
apksigner sign --ks release.keystore --out app-release.apk app-release-aligned.apk

# 3. Verify:
apksigner verify --print-certs app-release.apk
```

**B. Wire a `keystore.properties` into Gradle (recommended for repeat builds).**
Add a git-ignored `keystore.properties` at the repo root:

```properties
# keystore.properties (git-ignored)
storeFile=release.keystore
storePassword=…
keyAlias=t2w
keyPassword=…
```

…then add a `signingConfigs` block to `app/build.gradle.kts` that loads it
(mirroring the existing `secrets.properties` pattern) and reference it from the
`release` build type:

```kotlin
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
            // …existing minify / API_BASE_URL config…
        }
    }
}
```

Make sure `keystore.properties`, `*.keystore`, and `*.jks` are in `.gitignore`.

---

## 4. Install / sideload via adb

Install (or reinstall, keeping data) onto the connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Useful variants:

```bash
adb install -r -d app-debug.apk          # allow version downgrade
adb -s 1A2B3C4D5E install -r app.apk     # target a specific device by serial
adb uninstall com.taleson2wheels.app     # remove a previous install first
```

If the device blocks the install with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
(signature mismatch between debug and release builds), uninstall first.

### Sideload without a dev machine

Copy the APK to the phone (USB, Drive, etc.), then in a file manager tap it and
allow **"Install unknown apps"** for that source. This is the no-`adb` path for
handing a build to a rider for a field test.

---

## 5. Offline-GPS live-tracking field test

The whole reason the native app exists is reliable **live GPS sharing** during a
ride — something browsers can't do dependably. The live-ride screen
(`ui/live/LiveRideScreen.kt`) exposes a **"Share my location"** toggle that
starts streaming the rider's position to the backend for the ride's live map.

> **Foreground-only, for now.** Location sharing currently runs **only while the
> app is in the foreground** — there is no background/foreground-service GPS yet
> (see [`live-tracking-plan.md`](./live-tracking-plan.md) for the planned
> foreground-service work). Keep the app open and the screen on for the duration
> of the test.

### Goal

Verify that GPS sampling and the upload pipeline behave sensibly when
connectivity is intermittent — the realistic case on a ride through canyons,
tunnels, and dead zones.

### Procedure

1. **Build & install** a debug APK pointed at a reachable backend (§2/§4). For a
   real outdoor test, use a LAN/public host, not `10.0.2.2`.
2. **Grant location.** First launch of the live screen requests
   `ACCESS_FINE_LOCATION` at runtime; grant **"While using the app"**. Confirm
   GPS/Location is enabled on the device.
3. **Join a live ride** and toggle **"Share my location"** on. Confirm your
   position appears on the ride's live map.
4. **Go offline mid-stream** to simulate a dead zone:
   ```bash
   adb shell svc data disable    # drop mobile data
   adb shell svc wifi disable    # drop Wi-Fi
   ```
   (or just toggle Airplane mode on the device). Keep moving so new GPS fixes
   are produced while there's no network.
5. **Observe** that the app keeps acquiring fixes, doesn't crash or ANR, and the
   toggle stays "on" while offline.
6. **Come back online**:
   ```bash
   adb shell svc data enable
   adb shell svc wifi enable
   ```
   Confirm the stream resumes and the live map catches up (position jumps to the
   latest fix; whether buffered points are backfilled depends on the
   live-tracking implementation — note what you observe).
7. **Toggle "Share my location" off** and confirm the app stops sampling GPS
   (battery/location indicator clears).

### What to watch

- **Battery drain** over a representative ride duration with the screen on.
- **Behaviour on `onStop`** — since tracking is foreground-only, backgrounding
  the app (home button, screen lock) is expected to pause sharing. Document the
  exact behaviour so testers aren't surprised.
- **GPS accuracy** in marginal conditions (tree cover, tunnels).

### Capturing diagnostics

```bash
# Live logs filtered to the app's tracking tags:
adb logcat | grep -i -E "taleson2wheels|location|live"

# Pull a logcat snapshot for a bug report:
adb logcat -d > t2w-fieldtest-$(date +%Y%m%d-%H%M).log
```

---

## 6. Quick reference

```bash
./gradlew :app:assembleDebug                              # build debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk  # install
adb shell am start -n com.taleson2wheels.app/.MainActivity  # launch
adb shell svc data disable && adb shell svc wifi disable    # go offline
adb shell svc data enable  && adb shell svc wifi enable     # back online
adb uninstall com.taleson2wheels.app                        # remove
```
