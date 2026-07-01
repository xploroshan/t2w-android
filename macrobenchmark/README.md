# :macrobenchmark

Device-backed performance measurements for **Tales on 2 Wheels**
(`com.taleson2wheels.app`), run against the release-like `benchmark` build type
(minified like release, debuggable so the harness can attach). These need a
device or emulator — they run on the perf CI, not in the JVM unit-test run.

## What's here

| Class | Measures |
| --- | --- |
| `StartupBenchmark` | Cold + warm startup time (`StartupTimingMetric`). |
| `BaselineProfileGenerator` | Records the hot startup / first-frame code paths into a **Baseline Profile**. |

## Startup benchmark

```bash
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
```

Cold = process freshly created; warm = process alive, activity recreated. The
emulator CI runs this (numbers off an emulator are informational, not
release-grade — measure on a physical device for real figures).

## Baseline Profile

A Baseline Profile lists the classes/methods ART should AOT-compile at install
time, cutting cold-start jank on the critical journey. The wiring:

- **Producer** — this module applies `androidx.baselineprofile` and exposes
  `BaselineProfileGenerator`, which exercises cold launch to the first screen.
- **Consumer** — `:app` applies the plugin, `baselineProfile(project(":macrobenchmark"))`,
  and bundles `androidx.profileinstaller` so the profile installs at first run.

Generate + embed the profile (needs a connected device or a Gradle Managed
Device — the reactivecircus emulator on CI qualifies):

```bash
./gradlew :app:generateBaselineProfile
```

That runs the generator on-device and writes the profile into
`app/src/…/generated/baselineProfiles/`. **Commit the generated file** so release
builds ship it. Until then the build is unaffected — `assembleRelease` /
`assembleBenchmark` merge whatever profile exists (or none) and still succeed;
this was validated token-free (R8 minify + the profile-merge tasks run clean).

The generator only covers the signed-out startup path today (no backend needed);
extend it with post-login scrolls once the perf runner has a seeded backend.
