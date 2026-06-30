# Screenshot tests (Paparazzi)

[Paparazzi](https://github.com/cashapp/paparazzi) renders Jetpack Compose UI to
PNG images **on the JVM** — no emulator, device, or backend. We use it for two
things: a visual gallery of the app's screens, and screenshot regression tests.

The golden images live in [`app/src/test/snapshots/images/`](../app/src/test/snapshots/images)
and the render code in [`app/src/test/kotlin/com/taleson2wheels/app/paparazzi/`](../app/src/test/kotlin/com/taleson2wheels/app/paparazzi).

## Commands

```bash
# (Re)generate the golden PNGs — run this after intentional UI changes:
./gradlew :app:recordPaparazziDebug

# Verify the current UI still matches the goldens (diffs land in build/reports/paparazzi):
./gradlew :app:verifyPaparazziDebug
```

After `record`, open `app/src/test/snapshots/images/*.png` to see each screen.

## How it's wired

- The screenshot tests live in `src/test` but are **excluded from the normal
  `testDebugUnitTest`** task (see the `isPaparazziRun` guard in
  `app/build.gradle.kts`). They run **only** via `record/verifyPaparazziDebug`.
  This keeps `testDebugUnitTest` — which CI and Android Studio run — free of
  pixel-exact assertions, so it never fails just because a host renders fonts
  slightly differently.
- **Goldens are OS-specific.** The committed PNGs were recorded on Linux (the CI
  platform). If you `verifyPaparazziDebug` on macOS/Windows you'll see diffs from
  sub-pixel font rendering — that's expected; `recordPaparazziDebug` locally to
  regenerate for your machine, or rely on a Linux runner for verification.
- Paparazzi `2.0.0-alpha01` is pinned deliberately: it's the build compiled
  against Kotlin 2.0.21 (the project's version). Newer alphas require Kotlin 2.1+.

## Adding a screen

The tests render **real app composables** with sample data. To snapshot a new
screen, expose its presentational composable as `internal` (most list items
already are) and add a `@Test` to `ScreenshotTest.kt`:

```kotlin
@Test
fun my_screen() = snapshot {
    Column(Modifier.fillMaxSize()) {
        AppTopBar("My screen")
        MyCardItem(sampleData)
    }
}
```

Then run `./gradlew :app:recordPaparazziDebug` to write its golden.

> Stateful screens (those that take `AppViewModelFactory`) are snapshotted via
> their stateless leaf composables + sample DTOs, rather than the full
> `XScreen(factory)` entry point, to avoid spinning up ViewModels/coroutines/network.
