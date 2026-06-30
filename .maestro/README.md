# Maestro E2E flows

End-to-end UI flows for the **Tales on 2 Wheels** Android app
(`com.taleson2wheels.app`), written for [Maestro](https://maestro.mobile.dev).

Selectors are **text-based** and target the real on-screen copy
(`res/values/strings.xml` + composable literals), so they survive layout and
restyle changes. They intentionally avoid brittle view-ID / index selectors.

## Flows

| File | What it covers |
| --- | --- |
| `01_launch_to_login.yaml` | Cold launch on a signed-out install lands on the login screen; asserts title, subtitle, field labels, button, and the forgot/register links. |
| `02_login_form_validation.yaml` | The login form keeps "Sign in" inert on blank input, accepts a well-formed email + password, and dispatches the request without crashing. |
| `03_tab_navigation.yaml` | Signs in, then walks the bottom tabs Home → Rides → Riders → Profile, asserting each destination's app-bar text. |

## Prerequisites

- Maestro CLI installed: `curl -Ls "https://get.maestro.mobile.dev" | bash`
- A connected Android emulator or device with the **debug** APK installed
  (see [`../docs/SIDELOAD.md`](../docs/SIDELOAD.md) to build and `adb install`).
- For `03_tab_navigation.yaml`: a backend reachable at the app's
  `API_BASE_URL` (debug default `http://10.0.2.2:3000/`, i.e. the host's
  `localhost:3000` from the emulator) with a **seeded login account**.

## Running

Run the whole suite (Maestro picks up every `*.yaml` in the folder):

```bash
maestro test .maestro
```

Run a single flow:

```bash
maestro test .maestro/01_launch_to_login.yaml
```

Pass seeded credentials to the tab-navigation flow without committing secrets:

```bash
maestro test \
  -e EMAIL=you@example.com \
  -e PASSWORD=your-password \
  .maestro/03_tab_navigation.yaml
```

Interactive selector exploration (useful when adding flows):

```bash
maestro studio
```

## Notes

- `clearState` at the top of each flow wipes the DataStore session so runs start
  signed-out and deterministic.
- Flows 01 and 02 need **no backend** — they exercise the auth gate and
  client-side form behaviour only. Flow 02's post-submit assertion is
  deliberately lenient (the login title remaining proves the form round-tripped);
  point it at a seeded backend and swap in a home-screen assertion for a true
  happy-path login.
- CI: Maestro Cloud or a self-hosted emulator runner can execute `maestro test
  .maestro`. Keep credentials in CI secrets and inject them with `-e`.
